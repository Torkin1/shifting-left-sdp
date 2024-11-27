package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class IssueTemporalSpanBean {

    private final Issue issue;
    private final List<TemporalSpan> temporalSpans = new ArrayList<>();
    private final Timestamp measurementDate;
}
