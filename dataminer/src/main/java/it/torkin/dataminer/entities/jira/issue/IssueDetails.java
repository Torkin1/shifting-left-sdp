package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Transient;
import lombok.Data;

@Embeddable
@Data
public class IssueDetails {

    @SerializedName("id")
    @Column(unique = true) private String jiraId;
    
    /**
     * NOTE: quoting the Jira API documentation:
     * 
     * > The issue is identified by its ID or key, however, if the identifier
     * > doesn't match an issue, a case-insensitive search and check for
     * > moved issues is performed. If a matching issue is found its 
     * > details are returned, a 302 or other redirect is not returned.
     * > The issue key returned in the response is the key of the issue found.
     * 
     * This means that we must keep the key to check if it matches with the
     * requested key, but we shouldn't store it in db since it is already
     * provided by the Issue object.
     * 
     * CAUTION: do not use this field to get the key of the issue,
     * use the key field of the Issue object instead.
     * 
     * Source: https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issues/#api-rest-api-2-issue-issueidorkey-get
    **/
    
    @SerializedName("key")
    @Transient private String jiraKey;  
    private String self; // link to issue in Jira

    @Embedded private IssueFields fields;

    
}
