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
 * 
 * We cannot trust issues pointed by subtasks and issuelinks
 * since they may not be present in the apacheJIT dataset.
 * We store the short versions since they are provided by default
 * by the Jira API. 
 * 
 * If the extended version of the issue is needed,
 * it must be fetched independently from the jira api if not present
 * in the local database, and quality checked to align with the
 * data in the apacheJIT dataset before storing it in the local db.
 */
@Data
@Entity
public class IssuePointer {
    @Id
    @SerializedName("id")
    private String jiraId;

    private String key;
    private String self;

    @Embedded private IssueFields fields;

}
