package it.torkin.dataminer.control.dataset.processed;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class ProcessedIssuesBean {

    private String datasetName;
    private Stream<Issue> processedIssues;

    /**
     * NOTE: Values in the map are updated each time the filters are applied.
     * Since the issues are returned as a Stream object, the total number of
     * filtered out issues is not known until the stream is fully consumed.
     */
    private Map<String, Map<String, Integer>> filteredByFilterPerProjecy = new HashMap<>();
    
}
