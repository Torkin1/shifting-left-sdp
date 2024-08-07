package it.torkin.dataminer.entities.apachejit;

import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;

@Entity
@Data
public class Issue {
    
    @Id
    private String key; // "PROJ-123"
    
    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<Commit> commits = new ArrayList<>();

    /**
     * Issue details mapped to the attributes obtainable from the
     * Jira REST API.
     */
    @Embedded
    private IssueDetails details;

}
