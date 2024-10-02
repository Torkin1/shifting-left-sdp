package it.torkin.dataminer.control.measurementdate;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;

@Data
public class MeasurementDateBean {
    private final String datasetName;
    private final Issue issue;
}
