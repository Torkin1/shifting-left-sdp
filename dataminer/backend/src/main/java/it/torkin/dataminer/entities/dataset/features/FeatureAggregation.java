package it.torkin.dataminer.entities.dataset.features;

import java.util.stream.Collector;

import it.torkin.dataminer.control.features.aggregators.CountTrueCollector;
import it.torkin.dataminer.control.features.aggregators.DurationCollector;
import it.torkin.dataminer.control.features.aggregators.MaxCollector;
import it.torkin.dataminer.control.features.aggregators.MinCollector;
import it.torkin.dataminer.control.features.aggregators.SumCollector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FeatureAggregation {
    
    /**
     * Given a set of double values, it returns the maximum value.
     */
    MAX,

    /**
     * Given a set of double values, it returns the minimum value.
     */
    MIN,

    /**
     * Given a set of double values, it returns the summed value.
     */
    SUM,

    /**
     * Given a set of booleans, it returns the count of true instances.
     */
    COUNT_TRUE,

    /**
     * Given a set of dates, it returns the timespan duration between the oldest and the latest dates.
     */
    DURATION,
    ;

    public static Collector<? extends Feature<?>, ?, ? extends Feature<?>> getCollector(FeatureAggregation aggregation){
        switch (aggregation) {
            case MAX:
                return new MaxCollector();
            case MIN:
                return new MinCollector();
            case SUM:
                return new SumCollector();
            case COUNT_TRUE:
                return new CountTrueCollector();
            case DURATION:
                return new DurationCollector();
            default:
                throw new IllegalArgumentException("No collector specified for feature aggregation " + aggregation);
        }
    }
}
