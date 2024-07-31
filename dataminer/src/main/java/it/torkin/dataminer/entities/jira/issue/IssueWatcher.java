package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueWatcher{
    private boolean isWatching;
    private String self;
    private int watchCount;
}
