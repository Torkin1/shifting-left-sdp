package it.torkin.dataminer.control.features;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import lombok.Data;

@Data
public class FeatureMinerBean {

    /** Input dataset */
    private final String dataset;
    /** Input issue */
    private final Issue issue;
    
    /**
     * Feature miners will store the measured features here
     */
    private final Measurement measurement;

    private final MeasurementDate measurementDate;
}
