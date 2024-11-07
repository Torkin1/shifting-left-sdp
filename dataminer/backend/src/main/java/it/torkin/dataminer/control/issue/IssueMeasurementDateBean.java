package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class IssueMeasurementDateBean {

    private final String dataset;
    private final Issue i1;
    private final Issue i2;
    private final MeasurementDate measurementDate;

}
