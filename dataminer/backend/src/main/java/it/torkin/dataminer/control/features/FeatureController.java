package it.torkin.dataminer.control.features;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
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

import it.torkin.dataminer.config.MeasurementConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.MeasurementDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.jira.project.Project;
import jakarta.transaction.Transactional;

@Service
public class FeatureController implements IFeatureController{

    @Autowired private List<FeatureMiner> miners;

    @Autowired private DatasetDao datasetDao;
    @Autowired private IssueDao issueDao;
    @Autowired private MeasurementDao measurementDao;
    @Autowired private ProjectDao projectDao;
    
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
        bean.getIssue().getMeasurements().add(bean.getMeasurement());
        // Measurement measurement = measurementDao.save(bean.getMeasurement());
        measurementDao.save(bean.getMeasurement());
        // bean.getIssue().getMeasurements().removeIf(m -> m.getMeasurementDateName().equals(measurement.getMeasurementDateName()));
        issueDao.save(bean.getIssue());
    }
    
    @Override
    @Transactional
    public void mineFeatures(){
        
        if (measurementPrintExists()) return;
        
        List<Dataset> datasets = datasetDao.findAll();
        ProcessedIssuesBean processedIssuesBean;
        Iterator<Issue> issues;
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        for (Dataset dataset : datasets) {
            for (MeasurementDate measurementDate : measurementDates) {
                
                // collect processed issue
                processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                processedDatasetController.getFilteredIssues(processedIssuesBean);
                issues = processedIssuesBean.getProcessedIssues().iterator();

                while (issues.hasNext()) {
                    Issue issue = issues.next();
                    Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));

                    
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
 
                }
            }
        }         
    }

    private boolean measurementPrintExists(){
        return new File(measurementConfig.getDir()).list().length > 0;
    }
    
    @Override
    @Transactional
    public void printMeasurements() throws IOException{
        Set<String> featureNames = getFeatureNames();
        List<Dataset> datasets = datasetDao.findAll();
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
        for (Dataset dataset : datasets) {
            for (MeasurementDate measurementDate : measurementDates) {
                Set<Project> projects = projectDao.findAllByDataset(dataset.getName());
                for (Project project : projects) {
                    File outputFile = new File(measurementConfig.getOutputFileName(dataset.getName(), project.getKey(), measurementDate.getName()));
                    Stream<Measurement> measurements = measurementDao
                        .findAllByProjectAndDatasetAndMeasurementDateName(project.getKey(), dataset.getName(), measurementDate.getName());
                    CsvSchema schema = createCsvSchema(featureNames);
                    CsvMapper mapper = new CsvMapper();
                    ObjectWriter writer = mapper.writer(schema);
                    try (SequenceWriter sequenceWriter = writer.writeValues(outputFile)){
                        measurements.forEach(measurement -> {
                            // TODO: if feature is numeric, normalize it
                            Map<String, Object> features = new LinkedHashMap<>();
                            measurement.getFeatures().forEach(f -> {
                                features.put(f.getName(), f.getValue());
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
    }
    
    private Set<String> getFeatureNames(){
        Set<String> featureNames = new HashSet<>();
        miners.forEach(miner -> featureNames.addAll(miner.getFeatureNames()));
        return featureNames;
    }

    private CsvSchema createCsvSchema(Set<String> featureNames){
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder();
        for (String featureName : featureNames){
            schemaBuilder = schemaBuilder.addColumn(featureName);
        }
        schemaBuilder = schemaBuilder.setUseHeader(true)
            .setColumnSeparator(',')
            ;
        return schemaBuilder.build();
    }
}
