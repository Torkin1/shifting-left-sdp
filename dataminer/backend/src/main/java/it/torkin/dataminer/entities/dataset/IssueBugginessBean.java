package it.torkin.dataminer.entities.dataset;

import lombok.Data;

/**
 * Bean to use when we need to read information regarding an issue
 * and its commits.
 */
@Data
public class IssueBugginessBean {
    private final Issue issue;
    private final String dataset;
}
