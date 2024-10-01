package it.torkin.dataminer.control.measurementdate;

import java.sql.Timestamp;
import java.util.function.Function;

public interface MeasurementDate extends Function<MeasurementDateBean, Timestamp> {}
