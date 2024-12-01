package it.torkin.dataminer.control.features.miners;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.ProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.issue.HasBeenAssignedBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import it.torkin.dataminer.toolbox.time.TimeTools;
import jakarta.transaction.Transactional;
import lombok.Data;

/**
 * Implements feature tracked by #148
 * > The proportion of buggy tickets assigned to the assigned dev.
 * 
 */
@Component
public class AssigneeANFICMiner extends FeatureMiner{

    @Data
    private class IssueCount {

        private double buggyIssues;
        private double issues;

        public void addBuggyIssue(){
            buggyIssues ++;
        }

        public void addIssue(){
            issues ++;
        }
    }
    
    @Autowired private IIssueController issueController;
    @Autowired private ProcessedDatasetController processedDatasetController;

    @Override
    @Transactional
    public void mine(FeatureMinerBean bean) {
        /**
         * for issue assigned dev:
         *   - count past tickets where assigned == dev (at some point in time) AND ticket is buggy
         *   - do the same for non-buggy tickets
         *   - calc ANFIC(dev) = #BuggyTickets/#Tickets
         *   - store ANFIC as a Feature
        */

        Double anfic;

        IssueCount issueCount = new IssueCount();
        ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(bean.getDataset(), bean.getMeasurementDate());

        processedDatasetController.getFilteredIssues(processedIssuesBean);
        try(Stream<Issue> issues =  processedIssuesBean.getProcessedIssues()){
            issues
            // only pick issues with measurement date before or equal the measurement date of this issue
            .filter(i -> !bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDataset(), i)).after(bean.getMeasurement().getMeasurementDate()))
            // only pick issues of same dataset and project
            .filter(i -> i.getDetails().getFields().getProject().getKey().equals(bean.getIssue().getDetails().getFields().getProject().getKey()))
            // exclude the issue to be measured
            .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
            // only pick issues which were assigned to the dev at some point in the past
            .filter(i -> issueController.hasBeenAssigned(new HasBeenAssignedBean(
                bean.getIssue(),
                issueController.getAssigneeKey(new IssueBean(bean.getIssue(), bean.getMeasurement().getMeasurementDate())), 
                TimeTools.now())))
            // count issues dividing them in buggy and buggy+clean
            .forEach(i -> {
                if (issueController.isBuggy(new IssueCommitBean(i, bean.getDataset()))){
                    issueCount.addBuggyIssue();
                }
                issueCount.addIssue();
            });
        }
            
        anfic = issueCount.buggyIssues / issueCount.issues;
        bean.getMeasurement().getFeatures().add(new DoubleFeature(IssueFeature.ANFIC.getFullName(), anfic));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(IssueFeature.ANFIC.getFullName());
    }
}
