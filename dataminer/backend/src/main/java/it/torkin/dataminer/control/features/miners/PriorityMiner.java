package it.torkin.dataminer.control.features.miners;

import java.sql.Timestamp;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.StringFeature;
import it.torkin.dataminer.entities.jira.issue.IssuePriority;

@Component
public class PriorityMiner extends FeatureMiner{

    @Autowired private IIssueController issueController;

    
    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(IssueFeature.PRIORITY.name());
    }

    @Override
    public void mine(FeatureMinerBean bean) {

        Issue issue = bean.getIssue();
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();

        String priority = issueController.getPriority(new IssueBean(issue, measurementDate))
            .map(IssuePriority::getName)
            .orElse("");
        
        bean.getMeasurement().getFeatures().add(new StringFeature(IssueFeature.PRIORITY.getName(), priority));

    }
    
}
