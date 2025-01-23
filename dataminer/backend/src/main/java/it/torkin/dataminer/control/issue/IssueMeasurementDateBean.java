package it.torkin.dataminer.control.issue;

import java.util.HashSet;
import java.util.Set;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.impl.OpeningDate;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class IssueMeasurementDateBean {

    private final String dataset;
    private final Issue i1;
    private final Issue i2;
    private final MeasurementDate measurementDate;

    /**
     * if measurement date is not available for an issue, we use this date instead.
     * Must use a date which is always available (i.e.: Opening date of ticket)
     * Set it to null if you don't want to use a fallback date and trigger an exception instead.
     */
    private MeasurementDate fallbackDate = new OpeningDate();

    /**
     * If fallback date is available, here are annotated issue keys for which the fall back date was used.
     */
    private Set<String> fellBackIssueKeys = new HashSet<>(); 
}
