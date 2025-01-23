package it.torkin.dataminer.control.measurementdate;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.function.Function;

public interface MeasurementDate extends Function<MeasurementDateBean, Optional<Timestamp>> {

    public default String getName(){
        return this.getClass().getSimpleName().split("\\$\\$")[0];
    }
}
