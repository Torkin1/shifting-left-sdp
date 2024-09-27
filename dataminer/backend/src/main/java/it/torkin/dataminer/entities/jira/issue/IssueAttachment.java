package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Embeddable
@Table(name="issue_attachment")
public class IssueAttachment{
    
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer author;
    @Column(columnDefinition = "text")
    private String content;
    private Timestamp created;
    private String filename;
    @Column(unique = true)
    @SerializedName("id")
    private String jiraId;
    private String mimeType;
    private String self;
    private int size;
    private String thumbnail;
}
