package it.torkin.dataminer.entities.jira.issue; 
import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;
import lombok.Data; 

@Data
@Embeddable
public class IssueComment{
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
    private Developer author;
    /**
     * TODO: JIRA API example defines a json object for this field,
     * but apache issues use a string as a value to it.
     * Check what issues could cause a mismatch, and see what is inside
     * the body field.
     */
    @Column(columnDefinition = "text")
    private String body;
    private Timestamp created;
        
    @SerializedName("id")
    @Column(unique = true)
    private String jiraId;
    private String self;
    
    /**
     * NOTE: Maybe the update fields refer to the last update?
     */

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
    private Developer updateAuthor;
    private Timestamp updated;

    @Embedded
    private IssueCommentVisibility visibility;
}
