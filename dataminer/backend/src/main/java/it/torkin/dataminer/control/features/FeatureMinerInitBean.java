package it.torkin.dataminer.control.features;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import lombok.Data;

@Data
public class FeatureMinerInitBean {

    private final String dataset;
    private final MeasurementDate measurementDate;
    
}
