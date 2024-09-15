package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class IssueStatusCategory {
    private String self;
    @Id
    @SerializedName("id")
    private int jiraId;
    private String key;
    private String colorName;
    private String name;
}
