package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class IssueResolution {

        private String self;
        @Id
        @SerializedName("id")
        private String jiraId;
        private String description;
        private String name;

}
