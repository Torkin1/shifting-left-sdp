package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class IssuePriority {
    private String iconUrl;
    @Id
    @SerializedName("id")
    private String jiraId;
    private String name;
    private String self;
}
