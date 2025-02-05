package it.torkin.dataminer.control.features.miners;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.ProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.HasBeenAssignedBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.issue.IssueMeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.toolbox.Holder;

/**
 * Implements feature tracked by #148
 * > The proportion of buggy tickets assigned to the assigned dev.
 * 
 */
@Component
public class AssigneeMiner extends FeatureMiner{

    private static final String ANFIC = IssueFeature.ASSIGNEE.getFullName("ANFIC");
    private static final String FAMILIARITY = IssueFeature.ASSIGNEE.getFullName("Familiarity");

    @Autowired private IIssueController issueController;
    @Autowired private ProcessedDatasetController processedDatasetController;

    @Override
    public void mine(FeatureMinerBean bean) {

        Double anfic;
        Double familiarity;

        Holder<Long> totalIssues = new Holder<>(0L);
        Holder<Long> assigneeTotalIssues = new Holder<>(0L);
        Holder<Long> assigneeBuggyIssues = new Holder<>(0L);

        ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(bean.getDataset(), bean.getMeasurementDate());

        processedDatasetController.getFilteredIssues(processedIssuesBean);
        try(Stream<Issue> issues =  processedIssuesBean.getProcessedIssues()){
            issues
            // only pick issues of same dataset and project
            .filter(i -> i.getDetails().getFields().getProject().getKey().equals(bean.getIssue().getDetails().getFields().getProject().getKey()))
            // exclude the issue to be measured
            .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
            // only pick issues with measurement date before or equal the measurement date of this issue
            .filter(i -> !issueController.isAfter(new IssueMeasurementDateBean(bean.getDataset(), i, bean.getIssue(), bean.getMeasurementDate())))
            .forEach(i -> {
                // count total project issues in dataset prior to measurement date
                totalIssues.setValue(totalIssues.getValue() + 1);
                String assigneeKey = issueController.getAssigneeKey(new IssueBean(bean.getIssue(), bean.getMeasurement().getMeasurementDate()));
                boolean hasBeenAssigned = issueController.hasBeenAssigned(new HasBeenAssignedBean(i, assigneeKey, bean.getMeasurement().getMeasurementDate()));
                if (hasBeenAssigned){
                    // count buggy issues assigned 
                    if (issueController.isBuggy(new IssueCommitBean(i, bean.getDataset()))){
                        assigneeBuggyIssues.setValue(assigneeBuggyIssues.getValue() + 1);
                    }
                    // count issues assigned
                    assigneeTotalIssues.setValue(assigneeTotalIssues.getValue() + 1);
                }
            });
        }
            
        anfic = assigneeBuggyIssues.getValue() / (double) assigneeTotalIssues.getValue();
        familiarity = assigneeTotalIssues.getValue() / (double) totalIssues.getValue();
        
        bean.getMeasurement().getFeatures().add(new DoubleFeature(ANFIC, anfic));
        bean.getMeasurement().getFeatures().add(new DoubleFeature(FAMILIARITY, familiarity));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(ANFIC, FAMILIARITY);
    }
}
