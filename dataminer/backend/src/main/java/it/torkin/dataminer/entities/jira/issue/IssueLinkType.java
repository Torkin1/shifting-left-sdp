package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class IssueLinkType{
    @Id
    @SerializedName("id")
    private String jiraId;
    private String inward;
    private String name;
    private String outward;
}
