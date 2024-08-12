package it.torkin.dataminer.entities.jira.issue;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Embeddable
public class IssueCommentLog {
    @ElementCollection
    private List<IssueComment> comments = new ArrayList<>();

    @Transient
    private int maxResults;
    @Transient
    private int total;
    @Transient
    private int startAt;
}
