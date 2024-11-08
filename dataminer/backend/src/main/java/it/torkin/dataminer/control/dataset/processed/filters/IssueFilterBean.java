package it.torkin.dataminer.control.dataset.processed.filters;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IssueFilterBean {
    public IssueFilterBean(Issue issue, String datasetName,
     Timestamp measurementDate, boolean applyAnyway) {
        this.issue = issue;
        this.datasetName = datasetName;
        this.measurementDate = measurementDate;
        this.applyAnyway = applyAnyway;
    }

    private Issue issue;
    private String datasetName;
    private Timestamp measurementDate;

    /**
     * NOTE: filters must access this field in read-only mode.
     */
    private boolean filtered = false;

    /**
     * If true, filter will be applied even if issue has been already filtered out.
     */
    private boolean applyAnyway;

    /**
     * Filters must be stateless, so if they need state they
     * must store it here
     */
    private final Map<String, Object> filterStates = new HashMap<>();

    

}
