package it.torkin.dataminer.entities.jira.project;

import java.sql.Timestamp;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ProjectInsight{
    private Timestamp lastIssueUpTimestampTime;
    private int totalIssueCount;
}
