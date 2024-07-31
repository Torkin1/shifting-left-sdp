package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="issue_attachment")
public class IssueAttachment{
    
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer author;
    private String content;
    private Timestamp created;
    private String filename;
    @Id
    @SerializedName("id")
    private String jiraId;
    private String mimeType;
    private String self;
    private int size;
    private String thumbnail;
}
