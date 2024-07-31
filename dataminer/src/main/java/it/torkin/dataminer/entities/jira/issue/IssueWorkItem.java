package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class IssueWorkItem {

    private String self;
    @ManyToOne(cascade = { CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private Developer author;
    @ManyToOne(cascade = { CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private Developer upTimestampAuthor;
    @Column(columnDefinition = "text")
    private String comment;
    private Timestamp created;
    private Timestamp upTimestampd;
    private Timestamp started;
    private String timespent;
    private int timeSpentSeconds;
    @Id
    @SerializedName("id")
    private String jiraId;
    private String issueId;

}
