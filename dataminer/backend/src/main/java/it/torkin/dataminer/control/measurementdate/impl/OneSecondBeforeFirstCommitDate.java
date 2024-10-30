package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;

@Component
public class OneSecondBeforeFirstCommitDate implements MeasurementDate{

    private FirstCommitDate firstCommitDate = new FirstCommitDate();
    
    @Override
    public Timestamp apply(MeasurementDateBean arg0) {
        return Timestamp.from(firstCommitDate.apply(arg0).toInstant().minus(1, ChronoUnit.SECONDS));
    }

}
