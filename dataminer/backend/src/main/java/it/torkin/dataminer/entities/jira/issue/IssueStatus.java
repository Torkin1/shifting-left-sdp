package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class IssueStatus{
    private String iconUrl;
    @Id
    @SerializedName("id")
    private String jiraId;
    
    private String name;
    private String self;
    @Column(columnDefinition = "text")
    private String description;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private IssueStatusCategory statusCategory;
}
