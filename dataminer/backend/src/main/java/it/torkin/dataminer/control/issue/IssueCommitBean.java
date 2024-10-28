package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;
import lombok.NonNull;

/**
 * Bean to use when we need to read information regarding an issue
 * and its commits.
 */
@Data
public class IssueCommitBean {
    @NonNull private final Issue issue;
    @NonNull private final String dataset;
}
