package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class IssueCommentVisibility{
    private String identifier;
    private String type;
    private String value;
}
