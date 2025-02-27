package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueField;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssueHistoryItem;
import it.torkin.dataminer.toolbox.string.StringTools;
import it.torkin.dataminer.toolbox.time.TimeTools;

/**
 * Returns one second after the first assignment date of the issue, with
 * the left bound being one second after the opening date of the issue.
 */
@Component
public class OneSecondBeforeFirstAssignmentDate implements MeasurementDate{

    @Autowired private IIssueController issueController;
    
    @Override
    public Optional<Timestamp> apply(MeasurementDateBean bean) {

        Optional<Timestamp> date;
        
        // search in the changelog for histories related to the assignee field
        List<IssueHistory> assigneeHistories = issueController.getHistories(
            new IssueBean(bean.getIssue(), TimeTools.now()),
            IssueField.ASSIGNEE
        );
        if (assigneeHistories.isEmpty()) {

            // Changelog of assignee field is empty. 
            // We return one second after opening date in both cases when the assignee is set or not
            date = new OpeningDate().apply(bean);
            date = Optional.of(TimeTools.plusOneSecond(date.get()));

        } else {
            IssueHistory firstHistory = assigneeHistories.get(0);
            IssueHistoryItem firstItem = firstHistory.getItems().stream()
                .filter(item -> IssueField.ASSIGNEE.getName().equals(item.getField()))
                .findFirst()
                .get(); // should never be empty at this point
                
            // if the `from` field of the item is set, we return the opening date, else we return the
            // history creation date
            date = StringTools.isBlank(firstItem.getFrom())? Optional.of(TimeTools.minusOneSecond(firstHistory.getCreated())) : Optional.of(TimeTools.plusOneSecond(new OpeningDate().apply(bean).get()));
        }
        return date;
    }
    
}
