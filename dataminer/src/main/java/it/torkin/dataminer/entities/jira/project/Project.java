package it.torkin.dataminer.entities.jira.project;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.AvatarUrls;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Project{
    @Embedded
    private AvatarUrls avatarUrls;
    @Id
    @SerializedName("id")
    private String jiraId;
    @Embedded
    private ProjectInsight insight;
    private String key;
    private String name;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private ProjectCategory projectCategory;
    private String self;
    private boolean simplified;
    private String style;
}
