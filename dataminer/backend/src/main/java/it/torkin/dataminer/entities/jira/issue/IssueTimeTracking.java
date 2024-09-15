package it.torkin.dataminer.entities.jira.issue;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class IssueTimeTracking{
    private String originalEstimate;
    private int originalEstimateSeconds;
    private String remainingEstimate;
    private int remainingEstimateSeconds;
    private String timeSpent;
    private int timeSpentSeconds;
}
