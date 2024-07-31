package it.torkin.dataminer.entities.jira.issue; 
import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data; 

@Data
@Entity
public class IssueComment{
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
    private Developer author;
    /**
     * TODO: JIRA API example defines an object for this field,
     * but apache issues use a string as a value to it.
     * Check what issues could cause a mismatch, and see what is inside
     * the body field.
     */
    @Column(columnDefinition = "text")
    private String body;
    private Timestamp created;
    
    @Id    
    @SerializedName("id")
    private String jiraId;
    private String self;
    
    /**
     * NOTE: Maybe the upTimestamp fields refer to the last upTimestamp?
     */

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
    private Developer upTimestampAuthor;
    private Timestamp upTimestampd;

    @Embedded
    private IssueCommentVisibility visibility;
}
