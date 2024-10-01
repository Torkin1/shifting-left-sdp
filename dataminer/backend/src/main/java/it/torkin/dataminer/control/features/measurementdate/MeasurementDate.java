package it.torkin.dataminer.control.features.measurementdate;

import java.sql.Timestamp;
import java.util.function.Function;

import it.torkin.dataminer.entities.dataset.Issue;

public interface MeasurementDate extends Function<Issue, Timestamp> {}
