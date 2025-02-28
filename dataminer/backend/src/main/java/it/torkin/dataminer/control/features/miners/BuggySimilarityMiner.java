package it.torkin.dataminer.control.features.miners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.grpc.Status;
import it.torkin.dataminer.config.features.NLPFeaturesConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.nlp.Request.NlpIssueRequest;
import it.torkin.dataminer.nlp.Similarity.NlpIssueSimilarityScores;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;
import it.torkin.dataminer.toolbox.string.StringTools;
import lombok.extern.slf4j.Slf4j;

/**
 * #46 and its subtasks
 */
@Component
@Slf4j
public class BuggySimilarityMiner extends FeatureMiner{

    @Autowired private NLPFeaturesConfig config;

    public static final Set<String> METHODS = Set.of("TF-IDF_Cosine", "Jaccard", "EuclideanDistance", "BagOfWords_Cosine", "ExactMatch", "DiceCoefficient", "Hamming", "OverlappingCoefficient", "Levenshtein", "CommonTokenOverlap");
    public static final Set<String> FIELDS = Set.of("Title", "Text");
    public static final Set<String> AGGREGATION = Set.of("MaxSimilarity", "AvgSimilarity");

    private Set<String> featureNames;

    public static String getOutputFileName(String dataset, String project){
        // TODO: account for measurement date
        return "./src/main/resources/buggy-similarity/"+dataset + "_" + project + "_similarity_results.csv";
    }

    public static List<String> getVariantNames(){
        List<String> variantNames = new ArrayList<>(FIELDS.size() * METHODS.size() * AGGREGATION.size());
        for (String field : FIELDS) {
            for (String method : METHODS) {
                for(String aggregation : AGGREGATION){
                    variantNames.add(buildVariantName(method, field, aggregation));
                }
            }
        }
        return variantNames;
    }

    private static String buildVariantName(String method, String field, String aggregation) {
        return aggregation + "_" + method + "_" + field;
    }
  
    @Override
    public void init(){
        featureNames = getVariantNames().stream().map(name -> IssueFeature.BUGGY_SIMILARITY.getFullName(name)).collect(Collectors.toSet());
    }

    @Override
    public void mine(FeatureMinerBean bean) {
        NlpIssueRequest request = NlpIssueRequest.newBuilder()
            .setDataset(bean.getDataset())
            .setProject(bean.getIssue().getDetails().getFields().getProject().getKey())
            .setKey(bean.getIssue().getKey())
            .setMeasurementDateName(bean.getMeasurementDate().getName())
            .build();
        try{
            NlpIssueSimilarityScores buggySimilarityScores = getSimilarityScores(request);

            buggySimilarityScores.getScoreByNameMap().forEach((k, v) -> {
                Feature<?> feature = new DoubleFeature(IssueFeature.BUGGY_SIMILARITY.getFullName(k), v);
                bean.getMeasurement().getFeatures().add(feature);
            });
        } catch (Exception e){
            log.error("cannot mine buggy similarity for issue {}", bean.getIssue().getKey(), e);
            for (String name : featureNames){
                bean.getMeasurement().getFeatures().add(new DoubleFeature(name, Double.NaN));
            }
        }


    }

    private NlpIssueSimilarityScores getSimilarityScores(NlpIssueRequest request){
        // TODO: Open the right file according to the measurement date
        try (Resultset<Map<String, String>> records = new Resultset<>(getOutputFileName(request.getDataset(), request.getProject()), Map.class)) {
            while(records.hasNext()){
                Map<String, String> record = records.next();

                String dataset = record.get("ProjectName").split("_")[0];
                String project = record.get("ProjectName").split("_")[1];
                String key = record.get("RequirementID");

                if (request.getDataset().equals(dataset) && request.getProject().equals(project) && request.getKey().equals(key)){
                    NlpIssueSimilarityScores.Builder similarityScoresBuilder = NlpIssueSimilarityScores.newBuilder()
                        .setRequest(
                            NlpIssueRequest.newBuilder()
                                .setDataset(dataset)
                                .setProject(project)
                                .setKey(key)
                        );
                        record.forEach((k, v) -> {
                            if (!k.equals("ProjectName") && !k.equals("RequirementID") && !k.equals("Buggy")){
                                similarityScoresBuilder.putScoreByName(k, StringTools.isBlank(v) ? Double.NaN : Double.parseDouble(v));
                            }
                        });
                    NlpIssueSimilarityScores similarityScores = similarityScoresBuilder.build(); 
                    return similarityScores;
                }

            }
                
        } catch (UnableToGetResultsetException | IOException e) {
            throw new RuntimeException("error while reading csv scores", e);
        }
        throw Status.NOT_FOUND.withDescription("request has no correspondence in registered nlp beans: " + request.getDataset() + " " + request.getProject() + " " + request.getKey() ).asRuntimeException();
    }


    @Override
    protected Set<String> getFeatureNames() {
        return featureNames;
    }
}
