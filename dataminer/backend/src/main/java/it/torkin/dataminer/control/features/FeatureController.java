package it.torkin.dataminer.control.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Column;

import it.torkin.dataminer.config.ForkConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.MeasurementConfig;
import it.torkin.dataminer.config.MeasurementConfig.PredictionScope;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.MeasurementDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.Holder;
import it.torkin.dataminer.toolbox.math.normalization.LogNormalizer;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;


@Service
@Slf4j
public class FeatureController implements IFeatureController{

    @Getter
    @Autowired private List<FeatureMiner> miners;

    @Autowired private DatasetDao datasetDao;
    @Autowired private IssueDao issueDao;
    @Autowired private MeasurementDao measurementDao;
    @Autowired private ProjectDao projectDao;
    
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IMeasurementDateController measurementDateController;

    @Autowired private MeasurementConfig measurementConfig;
    @Autowired private ForkConfig forkConfig;

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private GitConfig gitConfig;

    @RequiredArgsConstructor
    private class MeasureFeaturesThread extends Thread{
        @Getter
        private final int index;
        private final TransactionTemplate transaction;
        private final ProgressBar progressBar;

        @Getter
        private Exception exception;

        @Override
        public void run() {
                List<Dataset> datasets = datasetDao.findAll();
                List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
                for (Dataset dataset : datasets){
                    for (MeasurementDate measurementDate : measurementDates){
                        File inputIssues = new File(forkConfig.getForkInputFile(index, dataset, measurementDate));
                        try (Stream<String> issuekeys = Files.lines(inputIssues.toPath())) {
                            mineFeatures(issuekeys, dataset, measurementDate, index, progressBar, transaction);
                        } catch (Exception e) {
                            exception = e;
                            return;
                        }
                    } 
                }
        }

        
    }

    @Override
    @Transactional
    public void initMiners() throws Exception{
        miners.forEach(miner -> {
            try {
                miner.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void doMeasurements(FeatureMinerBean bean){
        miners.forEach(miner -> miner.accept(bean));
    }

    private void saveMeasurement(FeatureMinerBean bean){
        bean.getIssue().getMeasurements().add(bean.getMeasurement());
        measurementDao.save(bean.getMeasurement());
        issueDao.save(bean.getIssue());
    }
        
    @Override
    public void mineFeatures(){

        /**
         * Main process divides issues among threads
         */
        List<Dataset> datasets = datasetDao.findAll();
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
        for (int i = 0; i < forkConfig.getForkCount(); i ++){
            new File(forkConfig.getForkDir(i)).mkdirs();
        }

        /**
         * Copies repositories in each thread directory
         */
        for (int i = 0; i < forkConfig.getForkCount(); i ++){
            GitConfig threadGitConfig = gitConfig.forThread(i);
            File repoDir = new File(gitConfig.getReposDir());
            File threadRepoDir = new File(threadGitConfig.getReposDir());
            try {
                
                if (!threadRepoDir.exists()){
                    
                    // exclude forks subdirectory
                    FileUtils.copyDirectory(repoDir, threadRepoDir, pathname -> {
                        String name = pathname.getName();
                        String subDirName = gitConfig.getForksSubDirName();
                        boolean accepted = !subDirName.contains(name);
                        return accepted;
                    });                                     
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        log.info("Repositories copied to thread directories");

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        transaction.executeWithoutResult(status -> {
            ProcessedIssuesBean processedIssuesBean;

            // prepare inputs for forks
            for (Dataset dataset : datasets) {
                for (MeasurementDate measurementDate : measurementDates) {
                    
                    log.info("Measuring issues according to {} at {}", dataset.getName(), measurementDate.getName());
    
                    // collect processed issue
                    processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                    processedDatasetController.getFilteredIssues(processedIssuesBean);
                    Stream<Issue> issues = processedIssuesBean.getProcessedIssues();

                    try(issues){
                                    
                        List<BufferedWriter> writers = new ArrayList<>();
                        for (Integer i = 0; i < forkConfig.getForkCount(); i ++){
                            File minerInputFile = new File(forkConfig.getForkInputFile(i, dataset, measurementDate));
                            BufferedWriter writer = new BufferedWriter(new FileWriter(minerInputFile));
                            writers.add(writer);
                        }
    
                        Holder<Integer> fork = new Holder<>(0);
                        issues.forEach(issue -> {

                            /*
                            * Writes issues assigned to each thread in a corresponding file
                            * */
                            BufferedWriter writer = writers.get(fork.getValue());
                            try {
                                writer.write(issue.getKey());
                                writer.newLine();
                                fork.setValue((fork.getValue() + 1) % forkConfig.getForkCount());
                            } catch (IOException e) {
                                status.setRollbackOnly();
                                throw new RuntimeException("Cannot write issue to miner input file", e);
                            }
                        });
                        for (Writer writer : writers){
                            writer.close();
                        }
                        
                    } catch (IOException e) {
                        status.setRollbackOnly();
                        throw new RuntimeException("Cannot write issue to miner input file", e);
                    }
                }
            }
        });
        log.info("Issues divided among threads");


        /**
         * Each thread processes its issues batch
         */
        List<MeasureFeaturesThread> workers = new ArrayList<>();
        ProgressBar progressBar = new ProgressBar("Measuring issues", -1);
        for (int i = 0; i < forkConfig.getForkCount(); i++){
            workers.add(new MeasureFeaturesThread(i, transaction, progressBar));
            workers.get(i).start();
            
        }
        log.info("Threads started");
        for (int i = 0; i < forkConfig.getForkCount(); i++){
            try {
                MeasureFeaturesThread worker = workers.get(i);
                worker.join();
                if (worker.getException() != null){
                    throw new RuntimeException("Error while measuring features", worker.getException());
                }
            } catch (Exception e) {
                log.error("Error while waiting for fork {}", i, e);
            }
        }

        log.info("measured {} issues", progressBar.getCurrent());
        progressBar.close();
        
    }

    private void mineFeatures(Stream<String> issuekeys, Dataset dataset, MeasurementDate measurementDate, int threadIndex, ProgressBar progressBar, TransactionTemplate transaction){

        TransactionTemplate saveMeasurementTransaction = new TransactionTemplate(transactionManager);
        saveMeasurementTransaction.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

        log.info("Thread {} mining issues of {} at {}", threadIndex, dataset.getName(), measurementDate.getName());
        Iterator<String> iterator = issuekeys.iterator();
        transaction.executeWithoutResult(status -> {

            while (iterator.hasNext()){                


                    String issuekey = iterator.next();
                    Issue issue =  issueDao.findByKey(issuekey);
                
                    // at this point we are measuring issues with an available measurement date
                    Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue)).get();

                    // update already existing measurements instead of replacing it with a new one
                    Measurement measurement = issue.getMeasurementByMeasurementDateName(measurementDate.getName());
                    if (measurement == null){
                        measurement = new Measurement();
                        measurement.setMeasurementDate(measurementDateValue);
                        measurement.setMeasurementDateName(measurementDate.getName());
                        measurement.setIssue(issue);
                        measurement.setDataset(dataset);
                    }

                    FeatureMinerBean bean = new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate, threadIndex);
                    try{
                        doMeasurements(bean);
                        saveMeasurementTransaction.executeWithoutResult(s -> saveMeasurement(bean) );
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot measure features", e);
                    }

                    progressBar.step();
                    progressBar.setExtraMessage(issue.getKey()+" from "+issue.getDetails().getFields().getProject().getKey());


            }
        });
}

    private boolean measurementPrintExists(Dataset dataset, Project project, MeasurementDate measurementDate){
        return new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName(), PredictionScope.ISSUE)).exists();
    }

    @Override
    public void printMeasurements(PrintMeasurementsBean bean){
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        List<Dataset> datasets = datasetDao.findAll();
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
        for (Dataset dataset : datasets){
            Set<Project> projects = transaction.execute(status -> projectDao.findAllByDataset(dataset.getName()) );
            for (Project project : projects){
                for (MeasurementDate measurementDate : measurementDates){
                    transaction.executeWithoutResult(status -> {
                        try {
                            if (measurementPrintExists(dataset, project, measurementDate)){
                                log.info("Measurements already printed for {} {} {}", dataset.getName(), project.getKey(), measurementDate.getName());
                            }
                            else {
                                printIssueMeasurements(dataset, project, measurementDate, bean.getTarget());
                                printCommitMeasurements(dataset, project, measurementDate);
                            }
                        } catch (IOException e) {
                            
                            throw new RuntimeException("Cannot print measurements", e);
                        }
                    });
                }
            }
        }
    }

    private String serializeFeature(Feature<?> f){
        String sValue;
        if (f.getValue() instanceof Number){
            // feature is numeric, we normalize it if a log base has been provided
            Double logBase = measurementConfig.getPrintLogBase();
            Number value = ((Number)f.getValue());
            Double dValue = logBase == null? value.doubleValue() : new LogNormalizer(logBase).apply(value);
            // Truncate value to fit in bounds
            if (!Double.isNaN(dValue) && dValue < measurementConfig.getPrintLowerBound()){
                dValue = measurementConfig.getPrintLowerBound();
            }
            if (!Double.isNaN(dValue) && dValue > measurementConfig.getPrintUpperBound()){
                dValue = measurementConfig.getPrintUpperBound();
            }
            // print NaNs in a weka compatible way
            sValue = Double.isNaN(dValue)? measurementConfig.getPrintNanReplacement() : dValue.toString();
                                            
        } else {
            // feature is not numeric, we print it as is
            sValue = f.getValue() != null? f.getValue().toString() : measurementConfig.getPrintNanReplacement();
        }
        return sValue;
}
    
        private void printIssueMeasurements(Dataset dataset, Project project, MeasurementDate measurementDate, IssueFeature target) throws IOException{
                            
            File outputFile = new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName(), MeasurementConfig.PredictionScope.ISSUE));
            CsvMapper mapper = new CsvMapper();
            Holder<CsvSchema> schema = new Holder<>();
            Holder<ObjectWriter> writer = new Holder<>();
            Holder<SequenceWriter> sequenceWriter = new Holder<>();
            try (Stream<Measurement> measurements = measurementDao.findAllByProjectAndDatasetAndMeasurementDateName(project.getKey(), dataset.getName(), measurementDate.getName())){
                measurements.forEach(measurement -> {

                    try {
                        if (schema.getValue() == null){
                            // Creates csv schema using a measurement as prototype 
                            Set<String> featureNames = getFeatureNames(measurement.getFeatures());
                            schema.setValue(createCsvSchema(featureNames, target));
                            writer.setValue(mapper.writer(schema.getValue()));
                            sequenceWriter.setValue(writer.getValue().writeValues(outputFile));
                        }

                        Map<String, Object> row = new LinkedHashMap<>();
                        measurement.getFeatures().forEach(f -> {
                            String sValue;
                            sValue = serializeFeature(f);
                            row.put(f.getName(), sValue);
                        });
                            sequenceWriter.getValue().write(row);
                    } catch (IOException e) {
                        throw new RuntimeException("Cannot write row to CSV at " + outputFile.getAbsolutePath(), e);
                    }
                });
            } finally {
                if (sequenceWriter.getValue() != null){
                    sequenceWriter.getValue().close();
                }
            }
         
    }
 
    private void printCommitMeasurements(Dataset dataset, Project project, MeasurementDate measurementDate) throws IOException{

        String target = "isBuggy";
        File outputFile = new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName(), MeasurementConfig.PredictionScope.COMMIT));
        CsvMapper mapper = new CsvMapper();
        Holder<CsvSchema> schema = new Holder<>();
        Holder<ObjectWriter> writer = new Holder<>();
        Holder<SequenceWriter> sequenceWriter = new Holder<>();
        Stream<Measurement> measurements = measurementDao.findAllWithCommitByDatasetAndProjectAndMeasurementDate(dataset.getName(), project.getKey(), measurementDate.getName())
        // retain only commits with a single linked issue
        .filter(m -> m.getCommit().getIssues().size() == 1);
        try (measurements){
            measurements.forEach(measurement -> {

                try {
                    if (schema.getValue() == null){
                        // Creates csv schema using a measurement as prototype 
                        Set<String> featureNames = getFeatureNames(measurement.getFeatures());
                        schema.setValue(createCsvSchema(featureNames, target));
                        writer.setValue(mapper.writer(schema.getValue()));
                        sequenceWriter.setValue(writer.getValue().writeValues(outputFile));
                    }

                    Commit commit = measurement.getCommit();

                    Map<String, Object> row = new LinkedHashMap<>();
                    measurement.getFeatures().forEach(f -> {
                        String sValue;
                        sValue = serializeFeature(f);
                        row.put(f.getName(), sValue);
                    });
                    row.put(target, commit.isBuggy());


                    sequenceWriter.getValue().write(row);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot write row to CSV at " + outputFile.getAbsolutePath(), e);
                }
            });
        } finally {
            if (sequenceWriter.getValue() != null){
                sequenceWriter.getValue().close();
            }
        }
                    
     
    }

    private Set<String> getFeatureNames(Set<Feature<?>> prototype){
        Set<String> featureNames = new HashSet<>();
        prototype.forEach(f -> featureNames.add(f.getName()));
        return featureNames;
    }

    private CsvSchema createCsvSchema(Set<String> featureNames, IssueFeature target){
        return createCsvSchema(featureNames, target.getFullName());
    }

    private CsvSchema createCsvSchema(Set<String> featureNames, String targetName){
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder();
        int i = 0;
        for (String featureName : featureNames){
            if (!featureName.equals(targetName)){
                schemaBuilder = schemaBuilder.addColumn(new Column(i, featureName));
                i++;
            }
        }
        schemaBuilder = schemaBuilder.addColumn(new Column(i, targetName));
        schemaBuilder = schemaBuilder.setUseHeader(true)
            .setColumnSeparator(',');
        return schemaBuilder.build().withHeader();
    }
}
