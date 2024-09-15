package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class IssueType {
    @Id
    @SerializedName("id")
    private String jiraId;
    private String self;
    private String name;
    private String description;
    private String iconUrl;
    private boolean subtask;
    private int avatarId;
}
