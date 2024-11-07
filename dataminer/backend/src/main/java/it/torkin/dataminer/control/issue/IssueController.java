package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;

import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

    @Override
    public boolean isBuggy(IssueCommitBean bean){
                
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
}
