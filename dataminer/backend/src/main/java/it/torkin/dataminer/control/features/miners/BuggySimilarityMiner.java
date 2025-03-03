package it.torkin.dataminer.control.features.miners;

import java.io.IOException;
import java.nio.file.Paths;
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
    private Set<String> variants = Set.of(getVariantNames().toArray(new String[0]));
    private Set<String> selectedR2rDistances;

    public String getSimilarityDistancesFilePath(String dataset, String project, String measurementDate){
        return Paths.get(config.getBuggySimilarityR2rDistancesDir(), "issue-beans_" + measurementDate, dataset + "_" + project + "_similarities.csv").toAbsolutePath().toString();
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
        selectedR2rDistances = Set.of(config.getBuggySimilaritySelectedR2rDistances());
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
            for (String name : !selectedR2rDistances.isEmpty()? selectedR2rDistances : variants){
                bean.getMeasurement().getFeatures().add(new DoubleFeature(IssueFeature.BUGGY_SIMILARITY.getFullName(name), Double.NaN));
            }
        }


    }

    private NlpIssueSimilarityScores getSimilarityScores(NlpIssueRequest request){
        String similarityDistancesFilePath = getSimilarityDistancesFilePath(request.getDataset(), request.getProject(), request.getMeasurementDateName());
        try (Resultset<Map<String, String>> records = new Resultset<>(similarityDistancesFilePath, Map.class)) {
            while(records.hasNext()){
                Map<String, String> record = records.next();

                String dataset = request.getDataset();
                String project = request.getProject();
                String key = record.get("RequirementID");

                // if (request.getDataset().equals(dataset) && request.getProject().equals(project) && request.getKey().equals(key)){
                if (request.getKey().equals(key)){
                    NlpIssueSimilarityScores.Builder similarityScoresBuilder = NlpIssueSimilarityScores.newBuilder()
                        .setRequest(
                            NlpIssueRequest.newBuilder()
                                .setDataset(dataset)
                                .setProject(project)
                                .setKey(key)
                        );
                        record.forEach((k, v) -> {
                            if ((!selectedR2rDistances.isEmpty() && selectedR2rDistances.contains(k) 
                            || selectedR2rDistances.isEmpty() && variants.contains(k))){
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
        throw Status.NOT_FOUND.withDescription("request has no correspondence in registered nlp beans: " + request.getDataset() + " " + request.getProject() + " " + request.getKey() + " at " + request.getMeasurementDateName() ).asRuntimeException();
    }


    @Override
    protected Set<String> getFeatureNames() {
        
        return selectedR2rDistances.isEmpty()? featureNames : selectedR2rDistances;
    }
}
