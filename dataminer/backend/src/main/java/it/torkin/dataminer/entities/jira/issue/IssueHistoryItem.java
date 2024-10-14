package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueHistoryItem {

    private String field;
    private String fieldtype;

    @Column(name = "from_", columnDefinition = "text")
    private String from;
    @Column(columnDefinition = "text")
    private String fromString;

    @Column(name = "to_", columnDefinition = "text")
    private String to;
    @Column(columnDefinition = "text")
    private String toString;

}
