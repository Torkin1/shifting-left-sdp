package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;

public class OneSecondAfterOpeningDate implements MeasurementDate{

    private MeasurementDate openingDate = new OpeningDate();
    
    @Override
    public Optional<Timestamp> apply(MeasurementDateBean arg0) {
        return Optional.of(Timestamp.from(openingDate.apply(arg0).get().toInstant().minus(1, ChronoUnit.SECONDS)));
    }
    
}
