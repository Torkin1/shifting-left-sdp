package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;

@Component
public class OpeningDate implements MeasurementDate {

    @Override
    public Optional<Timestamp> apply(MeasurementDateBean bean) {
        return Optional.of(bean.getIssue().getDetails().getFields().getCreated());
    }

}
