package it.torkin.dataminer.control.features.aggregators;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import it.torkin.dataminer.control.issue.Timespan;
import it.torkin.dataminer.entities.dataset.features.LongFeature;
import it.torkin.dataminer.entities.dataset.features.TimestampFeature;

public class DurationCollector implements Collector<TimestampFeature, Timespan, LongFeature>{

    @Override
    public BiConsumer<Timespan, TimestampFeature> accumulator() {
        // If the timestamp is outside the timespan, we enlarge the timespan to
        // include it.
        return (timespan, feature) -> {
            Timestamp timestamp = feature.getValue();
            if (timespan.getStart() == null || timestamp.before(timespan.getStart())) {
                timespan.setStart(timestamp);
            }
            if (timespan.getEnd() == null || timestamp.after(timespan.getEnd())) {
                timespan.setEnd(timestamp);
            }
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }

    @Override
    public BinaryOperator<Timespan> combiner() {
        return (timespan1, timespan2) -> {
            Timespan result = new Timespan();

            if (timespan1.getStart() == null || timespan2.getStart() == null) {
                result.setStart(timespan1.getStart() == null ? timespan2.getStart() : timespan1.getStart());
            } else {
                result.setStart(timespan2.getStart().before(timespan1.getStart()) ? timespan2.getStart() : timespan1.getStart());
            }
            if (timespan1.getEnd() == null || timespan2.getEnd() == null) {
                result.setEnd(timespan1.getEnd() == null ? timespan2.getEnd() : timespan1.getEnd());
            } else {
                result.setEnd(timespan2.getEnd().after(timespan1.getEnd()) ? timespan2.getEnd() : timespan1.getEnd());
            }

            return result;
        };
    }

    @Override
    public Function<Timespan, LongFeature> finisher() {
        return timespan -> new LongFeature(null, Duration.between(timespan.getStart().toInstant(), timespan.getEnd().toInstant()).toMillis());
    }

    @Override
    public Supplier<Timespan> supplier() {
        return Timespan::new;
    }
    
}
