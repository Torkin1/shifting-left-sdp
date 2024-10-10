package it.torkin.dataminer.entities.jira.issue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.rest.parsing.Hide;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class IssueHistory {

    @Id
    @GeneratedValue
    @Hide
    private Long id;
    
    @SerializedName("id")
    private String jiraId;

    @ManyToOne
    private Developer author;
    private Timestamp created;

    @ElementCollection
    private List<IssueHistoryItem> items = new ArrayList<>();

}
