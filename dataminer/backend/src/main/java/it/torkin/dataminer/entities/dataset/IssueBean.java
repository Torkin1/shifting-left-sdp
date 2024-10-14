package it.torkin.dataminer.entities.dataset;

import java.sql.Timestamp;

import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class IssueBean {
    private String dataset;
    private Timestamp measurementDate;
    @NonNull private Issue issue;

    public IssueBean(Issue issue, String dataset) {
        this.dataset = dataset;
        this.issue = issue;
    }
    
    public IssueBean(Issue issue, Timestamp measurementDate) {
        this.issue = issue;
        this.measurementDate = measurementDate;
    }

}
