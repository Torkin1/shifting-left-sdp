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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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
import it.torkin.dataminer.dao.git.GitDao;
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
import lombok.Data;
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

        @Override
        public void run() {
            transaction.executeWithoutResult(status -> {
                List<Dataset> datasets = datasetDao.findAll();
                List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
                for (Dataset dataset : datasets){
                    for (MeasurementDate measurementDate : measurementDates){
                        File inputIssues = new File(forkConfig.getForkInputFile(index, dataset, measurementDate));
                        try (Stream<Issue> issues = Files.lines(inputIssues.toPath()).map(line -> issueDao.findByKey(line))) {
                            mineFeatures(issues, dataset, measurementDate, index);
                        } catch (IOException e) {
                            throw new RuntimeException("Cannot read issues from input file "+inputIssues.getAbsolutePath(), e);
                        }
                    } 
                }
            });
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
    
    @Data
    private class IssueCount{
        private Long count = 0L;

        public void add(){
            count++;
        }
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

                    Set<String> projects = new HashSet<>();
                    
                    try(issues){
                                    
                        List<BufferedWriter> writers = new ArrayList<>();
                        for (Integer i = 0; i < forkConfig.getForkCount(); i ++){
                            File minerInputFile = new File(forkConfig.getForkInputFile(i, dataset, measurementDate));
                            BufferedWriter writer = new BufferedWriter(new FileWriter(minerInputFile));
                            writers.add(writer);
                        }
    
                        Holder<Integer> fork = new Holder<>(0);
                        issues.forEach(issue -> {

                            /**
                             * Clones one copy of guessed repository for each thread
                             * */
                            Project project = issue.getDetails().getFields().getProject();
                            boolean repoCloned = !projects.add(project.getKey());
                            if (!repoCloned){
                                for (int i = 0; i < forkConfig.getForkCount(); i ++){
                                    GitConfig threadGitConfig = gitConfig.forThread(i);
                                    String repo = dataset.getGuessedRepoByProjects().get(project.getKey());
                                    try (GitDao gitDao = new GitDao(threadGitConfig, repo)){
                                        // opening the GitDao has the cloning of the repo as side-effect
                                    } catch (Exception e) {
                                        status.setRollbackOnly();
                                        throw new RuntimeException(e);
                                    }
                                }
                            }

                            /*
                            * Writes issues assigned to each thread in a corresponding file
                            * */
                            BufferedWriter writer = writers.get(fork.getValue());
                            try {
                                writer.write(issue.getKey());
                                writer.newLine();
                                fork.setValue((fork.getValue() + 1) % forkConfig.getForkCount());
                            } catch (IOException e) {
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


        /**
         * Each thread processes its issues batch
         */
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < forkConfig.getForkCount(); i++){
            workers.add(new MeasureFeaturesThread(i, transaction));
            workers.get(i).start();
            
        }
        for (int i = 0; i < forkConfig.getForkCount(); i++){
            try {
                workers.get(i).join();
            } catch (InterruptedException e) {
                log.error("Error while waiting for fork {}", i, e);
            }
        }


        
    }

    private void mineFeatures(Stream<Issue> issues, Dataset dataset, MeasurementDate measurementDate, int threadIndex){
        try( issues;
        ProgressBar progressBar = new ProgressBar("Measuring issues", -1)){
        
        IssueCount issueCount = new IssueCount();
        issues.forEach( issue -> {
            
            progressBar.setExtraMessage(issue.getKey()+" from "+issue.getDetails().getFields().getProject().getKey());

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
            doMeasurements(bean);
            saveMeasurement(bean);

            progressBar.step();
            issueCount.add();


        });
        
        log.info("measured {} issues", issueCount.getCount());
    }
}

    private boolean measurementPrintExists(){
        return new File(measurementConfig.getDir()).listFiles().length > 0;
    }

    private boolean measurementPrintExists(Dataset dataset, Project project, MeasurementDate measurementDate){
        return new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName(), PredictionScope.ISSUE)).exists();
    }

    @Override
    @Transactional
    public void printMeasurements(PrintMeasurementsBean bean){
        List<Dataset> datasets = datasetDao.findAll();
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
        for (Dataset dataset : datasets){
            Set<Project> projects = projectDao.findAllByDataset(dataset.getName());
            for (Project project : projects){
                for (MeasurementDate measurementDate : measurementDates){
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
            sValue = f.getValue().toString();
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
