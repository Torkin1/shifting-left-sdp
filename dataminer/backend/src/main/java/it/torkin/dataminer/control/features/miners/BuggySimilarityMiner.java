package it.torkin.dataminer.control.features.miners;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import it.torkin.dataminer.config.features.NLPFeaturesConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc;
import it.torkin.dataminer.nlp.BuggyTicketsSimilarityMiningGrpc.BuggyTicketsSimilarityMiningBlockingStub;
import it.torkin.dataminer.nlp.Request.NlpIssueRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityScores;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityVariantsResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * #46 and its subtasks
 */
@Component
@Slf4j
public class BuggySimilarityMiner extends FeatureMiner{

    @Autowired private NLPFeaturesConfig config;

    private BuggyTicketsSimilarityMiningBlockingStub buggySimilarityStub;

    private Set<String> featureNames = new HashSet<>();


    @Override
    @Transactional
    public void init() throws UnableToInitNLPFeaturesMinerException {

        // disable miner if remote is not available
        if (config.getBuggySimilarityGrpcTarget() == null){
            log.warn("no remote nlp target set, the following features will not be mined: {}", this.getFeatureNames());
            return;
        }

        try{

            Channel channel = ManagedChannelBuilder.forTarget(config.getBuggySimilarityGrpcTarget()).usePlaintext().build();
            buggySimilarityStub = BuggyTicketsSimilarityMiningGrpc.newBlockingStub(channel);

            NlpIssueSimilarityVariantsResponse variantsResponse = 
                buggySimilarityStub.getSimilarityVariants(NlpIssueSimilarityVariantsRequest.newBuilder().build());
            featureNames.clear();
            variantsResponse.getVariantList().forEach(
                (variant) -> featureNames.add(IssueFeature.BUGGY_SIMILARITY.getFullName() + ": " + variant));
            
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
