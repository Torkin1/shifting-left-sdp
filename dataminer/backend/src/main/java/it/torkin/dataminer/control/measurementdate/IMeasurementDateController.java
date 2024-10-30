package it.torkin.dataminer.control.measurementdate;

import java.util.List;

public interface IMeasurementDateController {
    
    /**
     * Returns a list of measurement date implementations.
     * Which implementations are returned is defined in the
     * application.properties file.
     */
    public List<MeasurementDate> getMeasurementDates();
}
