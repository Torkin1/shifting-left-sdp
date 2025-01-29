package it.torkin.dataminer.control.features.aggregators;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.toolbox.Holder;

public class SumCollector implements Collector<Feature<? extends Number>, Holder<Double>, DoubleFeature>{

    @Override
    public BiConsumer<Holder<Double>, Feature<? extends Number>> accumulator() {
        return (holder, feature) -> holder.setValue(holder.getValue() + feature.getValue().doubleValue());
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Collector.Characteristics.UNORDERED);
    }

    @Override
    public BinaryOperator<Holder<Double>> combiner() {
        return (holder1, holder2) -> new Holder<>(holder1.getValue() + holder2.getValue());
    }

    @Override
    public Function<Holder<Double>, DoubleFeature> finisher() {
        return (holder) -> new DoubleFeature(null, holder.getValue());
    }

    @Override
    public Supplier<Holder<Double>> supplier() {
        return () -> new Holder<Double>(0.0);
    }
    
}
