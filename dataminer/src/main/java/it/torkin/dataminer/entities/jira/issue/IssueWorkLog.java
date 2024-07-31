package it.torkin.dataminer.entities.jira.issue;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Embeddable
public class IssueWorkLog{
    @Transient
    private int start;
    @Transient
    private int maxResults;
    @Transient
    private int total;
    @OneToMany(cascade = { CascadeType.ALL}, fetch = FetchType.LAZY)
    private List<IssueWorkItem> worklogs;
}
