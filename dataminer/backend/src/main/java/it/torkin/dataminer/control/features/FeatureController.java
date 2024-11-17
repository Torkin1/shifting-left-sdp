package it.torkin.dataminer.control.features;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Column;

import it.torkin.dataminer.config.MeasurementConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.MeasurementDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.math.normalization.LogNormalizer;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Service
@Slf4j
public class FeatureController implements IFeatureController{

    @Autowired private List<FeatureMiner> miners;

    @Autowired private DatasetDao datasetDao;
    @Autowired private IssueDao issueDao;
    @Autowired private MeasurementDao measurementDao;
    
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IMeasurementDateController measurementDateController;

    @Autowired private MeasurementConfig measurementConfig;

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
        bean.getIssue().getMeasurements().removeIf(m -> m.getMeasurementDateName().equals(bean.getMeasurement().getMeasurementDateName()));
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

    @Data
    private class LastProjectHolder{
        private Project project = null;
    }
    
    @Override
    @Transactional
    public void mineFeatures(){
                
        if (measurementPrintExists()){
            log.info("Measurement prints already exists, skipping mining");
            return;
        }
        
        List<Dataset> datasets = datasetDao.findAll();
        ProcessedIssuesBean processedIssuesBean;
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        for (Dataset dataset : datasets) {
            for (MeasurementDate measurementDate : measurementDates) {
                
                log.info("Measuring issues according to {} at {}", dataset.getName(), measurementDate.getName());

                // collect processed issue
                processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                processedDatasetController.getFilteredIssues(processedIssuesBean);
                LastProjectHolder lastProjectHolder = new LastProjectHolder();
                try( Stream<Issue> issues = processedIssuesBean.getProcessedIssues();
                    ProgressBar progressBar = new ProgressBar("Measuring issues", -1)){
                    
                    IssueCount issueCount = new IssueCount();
                    issues.forEach( issue -> {
                        
                        progressBar.setExtraMessage(issue.getKey()+" from "+issue.getDetails().getFields().getProject().getKey());

                        // issues are ordered by project, so we can print measurements for a project
                        // when we reach the next project
                        if (lastProjectHolder.getProject() != null && !lastProjectHolder.getProject().getKey().equals(issue.getDetails().getFields().getProject().getKey())){
                            try{
                                printMeasurements(dataset, lastProjectHolder.getProject(), measurementDate);
                            } catch (IOException e){
                                log.error("Cannot print measurements for {} at {}", dataset.getName(), measurementDate.getName(), e);
                            }
                        }
                        lastProjectHolder.setProject(issue.getDetails().getFields().getProject());

                        Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));

                        // update already existing measurements instead of replacing it with a new one
                        Measurement measurement = issue.getMeasurementByMeasurementDateName(measurementDate.getName());
                        if (measurement == null){
                            measurement = new Measurement();
                            measurement.setMeasurementDate(measurementDateValue);
                            measurement.setMeasurementDateName(measurementDate.getName());
                            measurement.setIssue(issue);
                            measurement.setDataset(dataset);
                        }

                        FeatureMinerBean bean = new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate);
                        doMeasurements(bean);
                        saveMeasurement(bean);

                        progressBar.step();
                        issueCount.add();


                    });
                    
                    log.info("measured {} issues", issueCount.getCount());
                }

                // print measurements for the last project
                if (lastProjectHolder.getProject() != null){
                    try{
                        printMeasurements(dataset, lastProjectHolder.getProject(), measurementDate);
                    } catch (IOException e){
                        log.error("Cannot print measurements for {} at {}", dataset.getName(), measurementDate.getName(), e);
                    }
                }
            }

        }         
    }

    private boolean measurementPrintExists(String dataset, String project, String measurementDate){
        return new File(measurementConfig.getOutputFileName(dataset, project, measurementDate)).exists();
    }

    private boolean measurementPrintExists(){
        return new File(measurementConfig.getDir()).listFiles().length > 0;
    }
    
    private void printMeasurements(Dataset dataset, Project project, MeasurementDate measurementDate) throws IOException{
                
        // To create csv schema any issue measurement can be eligible
        // to be a prototype for feature names
        Set<String> featureNames = getFeatureNames(
            measurementDao.findAllWithIssue()
                .findFirst()
                .get()
                .getFeatures()
        );
        Long measurementCount = measurementDao.countByProjectAndDatasetAndMeasurementDateName(project.getKey(), dataset.getName(), measurementDate.getName());
        if (measurementCount > 0){
            File outputFile = new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName()));
            try (Stream<Measurement> measurements = measurementDao.findAllByProjectAndDatasetAndMeasurementDateName(project.getKey(), dataset.getName(), measurementDate.getName())){
                CsvSchema schema = createCsvSchema(featureNames);
                CsvMapper mapper = new CsvMapper();
                ObjectWriter writer = mapper.writer(schema);
                try (SequenceWriter sequenceWriter = writer.writeValues(outputFile)){
                    measurements.forEach(measurement -> {
                        Map<String, Object> features = new LinkedHashMap<>();
                        measurement.getFeatures().forEach(f -> {
                            // if feature is numeric, normalize it
                            if (f.getValue() instanceof Number){
                                features.put(f.getName(), new LogNormalizer(10.0).apply((Number)f.getValue()));
                            } else {
                                features.put(f.getName(), f.getValue());
                            }
                        });
                        try {
                            sequenceWriter.write(features);
                        } catch (IOException e) {
                            throw new RuntimeException("Cannot write row to CSV at " + outputFile.getAbsolutePath(), e);
                        }
                    });
                } 
            }
        }
         
    }
    
    private Set<String> getFeatureNames(Set<Feature<?>> prototype){
        Set<String> featureNames = new HashSet<>();
        prototype.forEach(f -> featureNames.add(f.getName()));
        return featureNames;
    }

    private CsvSchema createCsvSchema(Set<String> featureNames){
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder();
        int i = 0;
        for (String featureName : featureNames){
            if (!featureName.equals(IssueFeature.BUGGINESS.getName())){
                schemaBuilder = schemaBuilder.addColumn(new Column(i, featureName));
                i++;
            }
        }
        schemaBuilder = schemaBuilder.addColumn(new Column(i, IssueFeature.BUGGINESS.getName()));
        schemaBuilder = schemaBuilder.setUseHeader(true)
            .setColumnSeparator(',');
        return schemaBuilder.build().withHeader();
    }
}
