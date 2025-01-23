package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;

@Component
public class OneSecondBeforeFirstCommitDate implements MeasurementDate{

    private FirstCommitDate firstCommitDate = new FirstCommitDate();
    
    @Override
    public Optional<Timestamp> apply(MeasurementDateBean arg0) {
        return Optional.of(Timestamp.from(firstCommitDate.apply(arg0).get().toInstant().minus(1, ChronoUnit.SECONDS)));
    }

}
