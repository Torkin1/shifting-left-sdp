package it.torkin.dataminer.entities.dataset;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.toolbox.time.TimeTools;
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
    
    @ManyToMany(mappedBy = "issues", fetch = FetchType.EAGER)
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

    /**
     * Returns if this issue has at least one buggy commit belonging
     * to the given dataset.
     * A measurement date can be specified to consider only the commits
     * strictly before that date.
     */
    public boolean isBuggy(IssueBugginessBean bean){

        return commits.stream().anyMatch(commit -> {
            
            Timestamp measurementDate = bean.getMeasurementDate();
            if (measurementDate == null){
                // we assume that the measurement date is now,
                // to include all the related commits in the dataset
                // (all data in dataset should be in the past with respect to now)
                measurementDate = TimeTools.now();
            }
                
            return commit.isBuggy()
             && commit.getDataset().getName().equals(bean.getDataset())
             && !commit.getTimestamp().after(measurementDate);
        });

    }

}
