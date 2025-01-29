package it.torkin.dataminer.control.features.miners;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.dataset.features.FeatureAggregation;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;

@Component
public class JITAggregatedMiner extends FeatureMiner {
    
    @Autowired private IIssueController issueController;
    
    private String buildAggregateJITFearureName(Feature<?> aggregatedFeature, String featureName, FeatureAggregation aggregation){
        return featureName + "-" + ((aggregation != null) ? aggregation.toString() : "");
    }
    
    private static <T> Feature<?> aggregate(List<Feature<?>> features, T prototypeValue, FeatureAggregation aggregation){
    
        Collector<Feature<?>, ?, Feature<?>> collector = (Collector<Feature<?>, ?, Feature<?>>) FeatureAggregation.getCollector(aggregation);
        return features.stream().collect(collector);
    
    }
    
    private Set<Feature<?>> aggregateCommitFeatures(List<Commit> commits){
        Set<Feature<?>> aggregatedFeatures = new HashSet<>();
        
        /**
         * We have a matrix in which each row is a commit and each column is a feature.
         * 
         *         | f1   | f2   | f3   | f4   | f5   |
         *      c1 | c1f1 | c1f2 | c1f3 | c1f4 | c1f5 |
         *      c2 | c2f1 | c2f2 | c2f3 | c2f4 | c2f5 |
         *      c3 | c3f1 | c3f2 | c3f3 | c3f4 | c3f5 |
         *      c4 | c4f1 | c4f2 | c4f3 | c4f4 | c4f5 |
         *      c5 | c5f1 | c5f2 | c5f3 | c5f4 | c5f5 |
         * 
         * If we transpose this matrix we get all values of a feature for all commits in any row.
         * 
         *         | c1   | c2   | c3   | c4   | c5   |
         *      f1 | c1f1 | c2f1 | c3f1 | c4f1 | c5f1 |
         *      f2 | c1f2 | c2f2 | c3f2 | c4f2 | c5f2 |
         *      f3 | c1f3 | c2f3 | c3f3 | c4f3 | c5f3 |
         *      f4 | c1f4 | c2f4 | c3f4 | c4f4 | c5f4 |
         *      f5 | c1f5 | c2f5 | c3f5 | c4f5 | c5f5 |
         * 
         * Now we can apply a reduction on each row to get the aggregate value of each feature.
         * 
         *         | Aggregated Value                     |
         *      f1 | f1_red(c1f1, c2f1, c3f1, c4f1, c5f1) |
         *      f2 | f2_red(c1f2, c2f2, c3f2, c4f2, c5f2) |
         *      f3 | f3_red(c1f3, c2f3, c3f3, c4f3, c5f3) |
         *      f4 | f4_red(c1f4, c2f4, c3f4, c4f4, c5f4) |
         *      f5 | f5_red(c1f5, c2f5, c3f5, c4f5, c5f5) |
         * 
         * We assume that each commit has the same set of features.
         */
            
        Map<String, List<Feature<?>>> featuresCommitMatrix = new HashMap<>();

        for (Commit commit : commits){
            for (Feature<?> feature : commit.getMeasurement().getFeatures()){
                
                // skip features with no aggregation method specified
                if (feature.getAggregation() != null) {

                    // transpose matrix
                    String featureName = feature.getName();
                    featuresCommitMatrix.putIfAbsent(featureName, new ArrayList<Feature<?>>());
                    List<Feature<?>> featureRow = (List<Feature<?>>) featuresCommitMatrix.get(featureName);
                    featureRow.add(feature);
                }
                
            }
        }
        
        // apply reduction to each row
        for (String featureName : featuresCommitMatrix.keySet()){
            List<Feature<?>> featureRow = featuresCommitMatrix.get(featureName);
            if (!featureRow.isEmpty()){
                Feature<?> prototype = featureRow.get(0);
                FeatureAggregation aggregation = prototype.getAggregation();
                Feature<?> aggregatedFeature = aggregate(featureRow, prototype.getValue(), aggregation);
                aggregatedFeature.setName(buildAggregateJITFearureName(aggregatedFeature, featureName, aggregation));
                aggregatedFeatures.add(aggregatedFeature);
            }

        }

        return aggregatedFeatures;
    }

    @Override
    public void mine(FeatureMinerBean bean) {
        Issue issue = bean.getIssue();
        String Dataset = bean.getDataset();
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();

        List<Commit> issueCommits = issueController.getCommits(new IssueCommitBean(issue, Dataset, measurementDate));
        Set<Feature<?>> aggregatedFeatures = aggregateCommitFeatures(issueCommits); 

        bean.getMeasurement().getFeatures().addAll(aggregatedFeatures);
        bean.getMeasurement().getFeatures().add(new IntegerFeature(IssueFeature.NUM_COMMITS.getFullName(), issueCommits.size()));

    }

    @Override
    protected Set<String> getFeatureNames() {
        /**
         * We check only for the num of commits feature since it is the only one shared
         * among all datasets
         */
        return Set.of(IssueFeature.NUM_COMMITS.getFullName());
    }
    
}
