package it.torkin.dataminer.entities.dataset;

import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Issue {
    
    public Issue(String issueKey) {
        this.key = issueKey;
    }

    @Id
    private String key; // "PROJ-123"
    
    @ManyToMany(mappedBy = "issues")
    @OrderBy("timestamp ASC")
    private List<Commit> commits = new ArrayList<>();

    /**
     * Issue details mapped to the attributes obtainable from the
     * Jira REST API.
     */
    @Embedded
    private IssueDetails details;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "issue")
    @OrderBy("measurementDate ASC")
    private List<Measurement> measurements = new ArrayList<>();

    public Measurement getMeasurementByMeasurementDateName(String measurementDateName){
        return measurements.stream()
            .filter(m -> m.getMeasurementDateName().equals(measurementDateName))
            .findFirst()
            .orElse(null);
    }

}
