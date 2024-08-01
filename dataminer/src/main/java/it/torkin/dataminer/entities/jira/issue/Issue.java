package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.rest.parsing.Hide;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
                                                                            
import lombok.Data;

@Entity
@Data
public class Issue {

    @SerializedName("id")
    @Id private String jiraId;
    
    @Column(unique = true) private String key;  // "PROJ-123"
    @Column(unique = true) private String self; // link to issue in Jira

    @Embedded private IssueFields fields;
    
    @Hide private Timestamp commitTimestamp;
    @Hide private boolean isBuggy;

    
}
