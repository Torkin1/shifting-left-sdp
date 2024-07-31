package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueProgress {
    private int progress;
    private int total;
}
