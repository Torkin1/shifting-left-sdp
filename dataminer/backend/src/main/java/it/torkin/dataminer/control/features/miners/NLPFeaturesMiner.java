package it.torkin.dataminer.control.features.miners;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.stream.JsonWriter;

import it.torkin.dataminer.config.NLPFeaturesConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Feature;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.IssueFeature;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.nlp.Request.NlpIssueBean;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NLPFeaturesMiner extends FeatureMiner{

    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IIssueController issueController;
    @Autowired private NLPFeaturesConfig config;
    @Autowired private DatasetDao datasetDao;
    @Autowired private List<MeasurementDate> measurementDates;
    
    private void serializeBean(JsonWriter writer, NlpIssueBean bean) throws IOException{

        writer.beginObject();

        writer.name("dataset").value(bean.getDataset());
        writer.name("Project name").value(bean.getProject());
        writer.name("measurementDateName").value(bean.getMeasurementDateName());
        writer.name("Requirement ID").value(bean.getKey());
        writer.name("Requirement text").value(bean.hasDescription()? bean.getDescription() : null);
        writer.name("Requirement title").value(bean.hasTitle()? bean.getTitle() : null);
        writer.name("buggy").value(bean.getBuggy());
        writer.name("date").value(
            new SimpleDateFormat("yyyy-MM-dd").format(Date.from(
                Instant.ofEpochSecond(bean.getMeasurementDate().getSeconds(),
                 bean.getMeasurementDate().getNanos()))));        

        writer.endObject();
    }

    private NlpIssueBean prepareBean(Issue issue, Dataset dataset, MeasurementDate measurementDate){
        
        IssueFields fields = issue.getDetails().getFields();
        Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));
        
        String description = issueController.getDescription(new IssueBean(issue, measurementDateValue));
        String title = issueController.getTitle(new IssueBean(issue, measurementDateValue));
        
        NlpIssueBean.Builder beanBuilder = NlpIssueBean.newBuilder()
            .setDataset(dataset.getName())
            .setProject(fields.getProject().getName())
            .setMeasurementDateName(measurementDate.getName())
            .setKey(issue.getKey())
            .setBuggy(issueController.isBuggy(new IssueCommitBean(issue, dataset.getName())))
            .setMeasurementDate(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(measurementDateValue.getTime() / 1000)
                .setNanos(measurementDateValue.getNanos()).build()
            );

        if (description != null){
            beanBuilder.setDescription(description);
        }
        if (title != null){
            beanBuilder.setTitle(title);
        }
        
        return beanBuilder.build();

    }
    
    /**
     * Dumps issue beans to a JSON file
     */
    private void serializeIssueBeans(File outputFile) throws IOException{

        List<Dataset> datasets;
        ProcessedIssuesBean processedIssuesBean;

        try (JsonWriter writer = new JsonWriter(new FileWriter(outputFile))) {
            
            writer.beginArray();

            datasets = datasetDao.findAll();
            for (Dataset dataset : datasets) {
                for (MeasurementDate measurementDate : measurementDates) {
                    
                    // query db for processed issues
                    processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                    processedDatasetController.getFilteredIssues(processedIssuesBean);

                    // write summaries to JSON
                    processedIssuesBean.getProcessedIssues().forEach((issue) -> {
                        try {
                            NlpIssueBean bean = prepareBean(issue, dataset, measurementDate);
                            serializeBean(writer, bean);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            writer.endArray();
        }

    }
    
    @Override
    @Transactional
    public void init() throws UnableToInitNLPFeaturesMinerException {
        
        try{
            File serializedIssueSummariesFile = new File(config.getNlpIssueBeans());
        
            if (!serializedIssueSummariesFile.exists()){
                serializeIssueBeans(serializedIssueSummariesFile);
        }
         
        // TODO: read beans from JSON and send them to NLP remote miners
        
        } catch (Exception e) {
            throw new UnableToInitNLPFeaturesMinerException(e);
        }
    }

    @Override
    public void mine(FeatureMinerBean bean) {
        // TODO: stub
        // mine features from NLP remote miners

        Feature buggySimilarity = new Feature(IssueFeature.BUGGY_SIMILARITY.getName(), "stub", null);

        bean.getMeasurement().getFeatures().add(buggySimilarity);

    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(IssueFeature.BUGGY_SIMILARITY.getName());
    }

}
