package it.torkin.dataminer.entities.jira.issue;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Embeddable
public class IssueWorklog{
    @Transient
    private int start;
    @Transient
    private int maxResults;
    @Transient
    private int total;
    @ElementCollection
    @OrderBy("created ASC")
    private List<IssueWorkItem> worklogs = new ArrayList<>();
}
