package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;

import io.micrometer.common.lang.NonNull;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

/**
 * Bean to use when we need to read information regarding an issue
 * without looking at its commits 
 */
@Data
public class IssueBean {
    @NonNull private final Issue issue; 
    @NonNull private final Timestamp measurementDate;
}
