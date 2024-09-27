package it.torkin.dataminer.control.dataset.processed.filters;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class IssueFilterBean {
    private final  Issue issue;
    private final String datasetName;
}
