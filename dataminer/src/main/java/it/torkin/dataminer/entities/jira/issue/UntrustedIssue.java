package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Points to an issue in Jira which can be not present in the local database.
 * Use this class to store pointers to issue which we don't know if they
 * are present in the apacheJIT dataset.
 */

@Data
@Entity
public class UntrustedIssue {
    @Id
    @SerializedName("id")
    private String jiraId;

    private String key;
    private String self;

    @Embedded private UntrustedIssueFields fields;

}
