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
import it.torkin.dataminer.entities.dataset.features.LongFeature;

@Component
public class ActivitiesMiner extends FeatureMiner{

    private static final String ACTIVITIES_COUNT = IssueFeature.ACTIVITIES.getFullName("count");
    private static final String COMMENTS_COUNT = IssueFeature.ACTIVITIES.getFullName("comments count");
    private static final String HISTORIES_COUNT = IssueFeature.ACTIVITIES.getFullName("histories Count");
    private static final String WORK_ITEMS_COUNT = IssueFeature.ACTIVITIES.getFullName("work items Count");

    @Autowired private IIssueController issueController;
    
    @Override
    public void mine(FeatureMinerBean bean) {
        
        long activities = 0;
        long comments = 0;
        long workItems = 0;
        long histories = 0;

        Issue issue = bean.getIssue();
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();

        comments = issueController.getComments(new IssueBean(issue, measurementDate)).size();
        workItems = issueController.getWorkItems(new IssueBean(issue, measurementDate)).size();
        histories = issueController.getHistories(new IssueBean(issue, measurementDate)).size();
        activities = comments + workItems + histories;

        bean.getMeasurement().getFeatures().add(new LongFeature(ACTIVITIES_COUNT, activities));
        bean.getMeasurement().getFeatures().add(new LongFeature(COMMENTS_COUNT, comments));
        bean.getMeasurement().getFeatures().add(new LongFeature(WORK_ITEMS_COUNT, workItems));
        bean.getMeasurement().getFeatures().add(new LongFeature(HISTORIES_COUNT, histories));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(ACTIVITIES_COUNT, COMMENTS_COUNT, HISTORIES_COUNT, WORK_ITEMS_COUNT);
    }
    
}
