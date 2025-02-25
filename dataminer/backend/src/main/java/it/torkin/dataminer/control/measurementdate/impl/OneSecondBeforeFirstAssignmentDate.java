package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueField;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.jira.Developer;
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
            // If there is an assignee in ticket details, we return the opening date
            Developer assignee = bean.getIssue().getDetails().getFields().getAssignee();
            date = assignee != null? new OpeningDate().apply(bean) : Optional.empty();

        } else {
            IssueHistory firstHistory = assigneeHistories.get(0);
            IssueHistoryItem firstItem = firstHistory.getItems().stream()
                .filter(item -> IssueField.ASSIGNEE.getName().equals(item.getField()))
                .findFirst()
                .get(); // should never be empty at this point
                
            // if the `from` field of the item is set, we return the opening date, else we return the
            // history creation date
            date = StringTools.isBlank(firstItem.getFrom())? Optional.of(firstHistory.getCreated()) : new OpeningDate().apply(bean);
        }
        if (!date.isPresent()) return date;
        return Optional.of(Timestamp.from(date.get().toInstant().minus(1, ChronoUnit.SECONDS)));
    }
    
}
