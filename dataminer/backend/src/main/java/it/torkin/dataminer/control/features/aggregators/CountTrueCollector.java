package it.torkin.dataminer.control.features.aggregators;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import it.torkin.dataminer.entities.dataset.features.BooleanFeature;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
import it.torkin.dataminer.toolbox.Holder;

public class CountTrueCollector implements Collector<BooleanFeature, Holder<Integer>, IntegerFeature> {

    @Override
    public BiConsumer<Holder<Integer>, BooleanFeature> accumulator() {
        return (holder, feature) -> holder.setValue(feature.getValue()? holder.getValue() + 1 : holder.getValue());
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }

    @Override
    public BinaryOperator<Holder<Integer>> combiner() {
        return (holder1, holder2) -> new Holder<>(holder1.getValue() + holder2.getValue());
    }

    @Override
    public Function<Holder<Integer>, IntegerFeature> finisher() {
        return holder -> new IntegerFeature(null, holder.getValue());
    }

    @Override
    public Supplier<Holder<Integer>> supplier() {
        return () -> new Holder<>(0);
    }
    
}
