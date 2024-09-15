package it.torkin.dataminer.entities.jira.project;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ProjectCategory{
    private String description;
    @Id
    @SerializedName("id")
    private String jiraId;
    private String name;
    private String self;
}
