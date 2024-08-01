package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Embeddable
public class UntrustedIssueFields {
    
    private String summary;

    @ManyToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssueStatus status;
    @ManyToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE}, 
        fetch = FetchType.LAZY)
    private IssuePriority priority;
    @ManyToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssueType issuetype;

}
