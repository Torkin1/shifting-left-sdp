package it.torkin.dataminer.control.dataset.processed;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class ProcessedIssuesBean {

    // TODO: #63: move both this fields in a ProcessedIssues class (package entities.transient)
    private final String datasetName;
    private Stream<Issue> processedIssues;

    /**
     * NOTE: Values in the following maps are updated each time the filters are applied.
     * Since the issues are returned as a Stream object, the total number of
     * filtered out issues is not known until the stream is fully consumed.
     */

    /**
     * Number of issues excluded by at least one filter.
     * This number, summed with the number of processedIssues, gives the total number of issues
     * stored in the dataset.
     */
    private Map<String, Integer> excludedByProject = new HashMap<>();

    /**
     * Number of issues flagged as to exclude for each project, grouped by filter.
     * An Issue can be excluded by multiple filters, so the sum of the values for each project
     * can be greater than the corresponding value in the excludedByProject map.
     */
    private Map<String, Map<String, Integer>> filteredByProjectGroupedByFilter = new HashMap<>();

    private final MeasurementDate measurementDate;
    
}
