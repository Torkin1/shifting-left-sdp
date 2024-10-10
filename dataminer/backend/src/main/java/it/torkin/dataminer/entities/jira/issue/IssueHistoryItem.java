package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueHistoryItem {

    private String field;
    private String fieldtype;

    private String from;
    @Column(columnDefinition = "text")
    private String fromString;

    private String to;
    @Column(columnDefinition = "text")
    private String toString;

}
