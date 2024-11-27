package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssueHistoryItem;
import it.torkin.dataminer.entities.jira.issue.IssueStatus;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

    @Autowired private JiraConfig jiraConfig;
    
    private Set<String> inProgressIssueStatuses = new HashSet<>();
    private JiraDao jiraDao;

    @PostConstruct
    public void init(){
        jiraDao = new JiraDao(jiraConfig);
    }
    
    @Override
    public Boolean isBuggy(IssueCommitBean bean){
                
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();
        
        return issue.getCommits().stream().anyMatch(commit -> {          
            return commit.isBuggy()
             && commit.getDataset().getName().equals(dataset);
        });
    }

    private int compareMeasurementDate(String datasetName, Issue i1, Issue i2, MeasurementDate measurementDate){
        return measurementDate.apply(new MeasurementDateBean(datasetName, i1))
            .compareTo(measurementDate.apply(new MeasurementDateBean(datasetName, i2)));
    }

    @Override
    public int compareMeasurementDate(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate());
    }

    @Override
    public boolean isBefore(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate()) < 0;
    }

    @Override
    public boolean isAfter(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate()) > 0;
    }

    @Override
    public Commit getFirstCommit(IssueCommitBean bean) {
        return bean.getIssue().getCommits().stream()
            .filter(commit -> commit.getDataset().getName().equals(bean.getDataset()))
            .sorted((c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getDescription(IssueBean bean) {
        
        return new IssueFieldGetter<String>(
            fields -> fields.getDescription() == null? "" : fields.getDescription(),
            entry -> entry.getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.DESCRIPTION));        
    }

    @Override
    public String getTitle(IssueBean bean){
        return new IssueFieldGetter<String>(
            fields -> fields.getSummary() == null? "" : fields.getSummary(),
            entry -> entry.getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.SUMMARY));
    }

    @Override
    public String getAssigneeKey(IssueBean bean) {
        return new IssueFieldGetter<String>(
            fields -> fields.getAssignee() == null? "" : fields.getAssignee().getKey(),
            entry -> entry.getValue()
        ).apply(new IssueFieldGetterBean(bean, IssueField.ASSIGNEE));
    }

    private boolean changelogContains(Issue issue, HistoryEntry example, Timestamp measurementDate, boolean and){
        return issue.getDetails().getChangelog().getHistories().stream()
            .filter(h -> !h.getCreated().after(measurementDate))
            .anyMatch(h -> h.getItems().stream().anyMatch(i -> {
                if (and){
                    return i.getFrom() == example.getValue() && i.getTo() == example.getValue();
                }
                else {
                    return i.getFrom() == example.getValue() || i.getTo() == example.getValue();
                }
            }));
    }
    
    @Override
    public boolean hasBeenAssigned(HasBeenAssignedBean bean) {

        String assigneeKey = getAssigneeKey(new IssueBean(bean.getIssue(), bean.getMeasurementDate()));
        if (assigneeKey == null) return false;
        if (assigneeKey.equals(bean.getAssigneeKey())) return true;
        else return changelogContains(
            bean.getIssue(),
            new HistoryEntry(bean.getAssigneeKey(), null), 
            bean.getMeasurementDate(), 
            false);
        
                
    }

    private void initInProgressIssueStatusCategoryIds() throws UnableToGetResourceException{
        IssueStatus[] issueStatuses = jiraDao.queryIssueStatuses();
        final String inProgress = "In Progress";

        // All statuses with a status category named "In Progress" are considered in progress
        for (IssueStatus status : issueStatuses){
            if (status.getStatusCategory().getName().equals(inProgress)){
                inProgressIssueStatuses.add(status.getJiraId());
            }
        }

    }

    @Override
    public void getInProgressTemporalSpans(IssueTemporalSpanBean bean) {
        
        // cache in progress issue statuses
        if (inProgressIssueStatuses.isEmpty()){
            try {
                initInProgressIssueStatusCategoryIds();
            } catch (UnableToGetResourceException e) {
                log.error("Unable to get issue statuses", e);
                return;
            }
        }

        // traverse changelog to find in progress timespans boundaries
        // NOTE: if issue is already in progress at the opening date, we do not count it.
        List<IssueHistory> histories = bean
            .getIssue()
            .getDetails()
            .getChangelog()
            .getHistories()
            .stream()
            .filter(h -> !h.getCreated().after(bean.getMeasurementDate()))
            .sorted((h1, h2) -> h1.getCreated().compareTo(h2.getCreated()))
            .toList();
        TemporalSpan span = null;
        for (IssueHistory history : histories){
            for (IssueHistoryItem item : history.getItems()){

                if (!item.getField().equals(IssueField.STATUS.getName())) continue;
                
                if (item.getField() != null){
                    if (inProgressIssueStatuses.contains(item.getTo()) && !inProgressIssueStatuses.contains(item.getFrom())){
                        span = new TemporalSpan();
                        span.setStart(history.getCreated());
                    }
                    else if (inProgressIssueStatuses.contains(item.getFrom()) && !inProgressIssueStatuses.contains(item.getTo())
                     && span != null){
                        span.setEnd(history.getCreated());
                        bean.getTemporalSpans().add(span);
                        span = null;
                    }
                }
            }
        }
        // if last in progress transition never ended in changelog, we can assume it is still in progress
        // at the measurement date
        // NOTE: Issue could be transitioned from in progress to another state before the measurement date actually
        if (span != null){
            span.setEnd(bean.getMeasurementDate());
            bean.getTemporalSpans().add(span);
        }


    }
}
