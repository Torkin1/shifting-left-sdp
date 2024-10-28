package it.torkin.dataminer.control.dataset.processed.filters;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;
import java.sql.Timestamp;

@Data
public class IssueFilterBean {
    private final  Issue issue;
    private final String datasetName;
    private final Timestamp measurementDate;

    /**
     * NOTE: filters must access this field in read-only mode.
     */
    private boolean filtered = false;

    /**
     * If true, filter will be applied even if issue has been already filtered out.
     */
    private final boolean applyAnyway;
}
