package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Embeddable
@Data
public class IssueWorkItem {

    private String self;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer author;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer updateAuthor;
    @Column(columnDefinition = "text")
    private String comment;
    private Timestamp created;
    private Timestamp updated;
    private Timestamp started;
    private String timespent;
    private int timeSpentSeconds;
    @Column(unique = true)
    @SerializedName("id")
    private String jiraId;
    private String issueId;

}
