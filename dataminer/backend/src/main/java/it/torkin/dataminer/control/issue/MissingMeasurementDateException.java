package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Issue;

public class MissingMeasurementDateException extends RuntimeException{
    
    private static final long serialVersionUID = 1L;

    public MissingMeasurementDateException(String datasetName, Issue issue, MeasurementDate measurementDate) {
        super("Missing measurement date for issue " + issue.getKey() + " in dataset " + datasetName + " using " + measurementDate.getName());
    }
}
    

