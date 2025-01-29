package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Bean to use when we need to read information regarding an issue
 * and its commits.
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class IssueCommitBean {
    @NonNull private final Issue issue;
    @NonNull private final String dataset;
    private Timestamp measurementDate;
}
