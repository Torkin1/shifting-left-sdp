package it.torkin.dataminer.control.features.miners;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.issue.IssueMeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
/**
 * #207
 */
@Component
public class ComponentsMiner extends FeatureMiner{

    private static final String COUNT = IssueFeature.COMPONENTS.getFullName("Count");
    private static final String BUGGINESS_MAX = IssueFeature.COMPONENTS.getFullName("Max Bugginess");

    @Autowired private IIssueController issueController;
    @Autowired private IProcessedDatasetController processedDatasetController;
    
    @Override
    public void mine(FeatureMinerBean bean) {

        int count = 0;
        Map<String, Integer> issuesByComponentId = new HashMap<>();
        Map<String, Integer> buggyByComponentId = new HashMap<>();
        Double maxBugginess = 0.0;
        
        /**
         *  get issue Components at measurement date;
            store components count as a feature ;
            for each component:

                calc component bugginess at measurement date;
                get max and store it as feature.

            Component bugginess = #componentIssues/#componentBuggyIssues

         */
        
        Issue issue = bean.getIssue();
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();

        // gets components at measurement date and count them
        Set<String> componentsIds
         = issueController.getComponentsIds(new IssueBean(issue, measurementDate));
        count = componentsIds.size();
        for (String component : componentsIds) {
            issuesByComponentId.put(component, 0);
            buggyByComponentId.put(component, 0);
        }

        ProcessedIssuesBean processedIssuesBean
         = new ProcessedIssuesBean(bean.getDataset(), bean.getMeasurementDate());
        processedDatasetController.getFilteredIssues(processedIssuesBean);
        try(Stream<Issue> issues = processedIssuesBean.getProcessedIssues()) {
            issues
            // only pick issues with measurement date before or equal the measurement date of this issue
            .filter(i -> !issueController.isAfter(new IssueMeasurementDateBean(bean.getDataset(), i, issue, bean.getMeasurementDate())))
            // only pick issues of same dataset and project
            .filter(i -> i.getDetails().getFields().getProject().getKey().equals(bean.getIssue().getDetails().getFields().getProject().getKey()))
            // exclude the issue to be measured
            .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
            // count occurrences of component in issues
            .forEach(i -> {
                // calculate intersection of current and past issue components sets
                Set<String> iComponentsIds = issueController.getComponentsIds(new IssueBean(i, measurementDate));
                iComponentsIds.retainAll(componentsIds);
                for (String componentId : iComponentsIds){
                    // count issues and buggy issues for each component
                    issuesByComponentId.put(componentId, issuesByComponentId.get(componentId) + 1);
                    if (issueController.isBuggy(new IssueCommitBean(i, bean.getDataset()))){
                        buggyByComponentId.put(componentId, buggyByComponentId.get(componentId) + 1);
                    }
                } 
            });
        }
        
        // get max bugginess
        for (String componentId : componentsIds) {
            double issueCount = issuesByComponentId.get(componentId);
            double buggyCount = buggyByComponentId.get(componentId);
            double bugginess = issueCount > 0? buggyCount / issueCount : 0;
            if (Double.isFinite(count) && bugginess > maxBugginess) {
                maxBugginess = bugginess;
            }
        }

        bean.getMeasurement().getFeatures().add(new IntegerFeature(COUNT, count));
        bean.getMeasurement().getFeatures().add(new DoubleFeature(BUGGINESS_MAX, maxBugginess));

    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(COUNT, BUGGINESS_MAX);
    }
    
}
