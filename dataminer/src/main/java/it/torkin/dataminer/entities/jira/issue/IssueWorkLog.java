package it.torkin.dataminer.entities.jira.issue;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 * TODO: iterate over all pages of worklog to fetch all worklog items
 */
@Data
@Embeddable
public class IssueWorkLog{
    @Transient
    private int start;
    @Transient
    private int maxResults;
    @Transient
    private int total;
    @ElementCollection
    private List<IssueWorkItem> worklogs = new ArrayList<>();
}
