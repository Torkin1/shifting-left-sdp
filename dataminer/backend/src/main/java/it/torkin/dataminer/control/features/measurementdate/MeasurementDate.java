package it.torkin.dataminer.control.features.measurementdate;

import java.sql.Timestamp;

import it.torkin.dataminer.entities.dataset.Issue;

public abstract class MeasurementDate {

    protected abstract Timestamp getMeasurementDate(Issue issue);

    /** 
     * True if the measurement date extracted from the issue is
     * stricty before the given date.
     */
    public boolean isBefore(Issue issue, Timestamp date){
        return getMeasurementDate(issue).before(date);
    } 

}
