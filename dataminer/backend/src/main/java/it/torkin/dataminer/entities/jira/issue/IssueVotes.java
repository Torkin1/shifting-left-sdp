package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueVotes {
    private String self;
    private int votes;
    private boolean hasVoted;
}
