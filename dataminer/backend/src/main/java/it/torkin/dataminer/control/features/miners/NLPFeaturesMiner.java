package it.torkin.dataminer.control.features.miners;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.stream.JsonWriter;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import it.torkin.dataminer.config.features.NLPFeaturesConfig;
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
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc.BuggyTicketsSimilarityMiningBlockingStub;
import it.torkin.dataminer.nlp.Request.NlpIssueBean;
import it.torkin.dataminer.nlp.Request.NlpIssueRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityScores;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsResponse;
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

    private BuggyTicketsSimilarityMiningBlockingStub buggySimilarityStub;

    private Set<String> featureNames = new HashSet<>();
    
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
            .setProject(fields.getProject().getKey())
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
                    try(Stream<Issue> issues = processedIssuesBean.getProcessedIssues()){
                        issues.forEach((issue) -> {
                            try {
                                NlpIssueBean bean = prepareBean(issue, dataset, measurementDate);
                                serializeBean(writer, bean);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
            writer.endArray();
        }

    }
    
    @Override
    @Transactional
    public void init() throws UnableToInitNLPFeaturesMinerException {
        
        // disable miner if remote is not available
        if (config.getBuggySimilarityGrpcTarget() == null){
            log.warn("no remote nlp target set, the following features will not be mined: {}", this.getFeatureNames());
            return;
        }
        
        try{
            File serializedIssueSummariesFile = new File(config.getNlpIssueBeans());
        
            if (!serializedIssueSummariesFile.exists()){
                serializeIssueBeans(serializedIssueSummariesFile);
        }

        Channel channel = ManagedChannelBuilder.forTarget(config.getBuggySimilarityGrpcTarget()).usePlaintext().build();
        buggySimilarityStub = BuggyTicketsSimilarityMiningGrpc.newBlockingStub(channel);

        NlpIssueSimilarityVariantsResponse variantsResponse = 
            buggySimilarityStub.getSimilarityVariants(NlpIssueSimilarityVariantsRequest.newBuilder().build());
        featureNames.clear();
        variantsResponse.getVariantList().forEach(
            (variant) -> featureNames.add(IssueFeature.BUGGY_SIMILARITY.getName() + "_" + variant));
         
        // TODO: read beans from JSON and send them to NLP remote miners
        
        } catch (Exception e) {
            throw new UnableToInitNLPFeaturesMinerException(e);
        }
    }

    @Override
    public void mine(FeatureMinerBean bean) {
        // mine features from NLP remote miners

        if (config.getBuggySimilarityGrpcTarget() == null) return;

        NlpIssueRequest request = NlpIssueRequest.newBuilder()
            .setDataset(bean.getDataset())
            /**
             * TODO: already generated json uses project names instead of keys.
             *       As soon as the json sending to the remote miner
             *       is implemented, this should be changed to use project key since
             *       names are not guaranteed to be unique.
             */
            .setProject(bean.getIssue().getDetails().getFields().getProject().getName())
            .setKey(bean.getIssue().getKey())
            .setMeasurementDateName(bean.getMeasurementDate().getName())
            .build();
        try{
            NlpIssueSimilarityScores buggySimilarityScores = buggySimilarityStub.getSimilarityScores(request);

            buggySimilarityScores.getScoreByNameMap().forEach((k, v) -> {
                Feature<?> feature = new DoubleFeature(k, v);
                bean.getMeasurement().getFeatures().add(feature);
            });
        } catch (Exception e){
            log.error("cannot mine buggy similarity for issue {}", bean.getIssue().getKey(), e);
        }


    }

    @Override
    protected Set<String> getFeatureNames() {
        return featureNames;
    }

}
