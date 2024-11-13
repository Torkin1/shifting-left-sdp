package it.torkin.dataminer.toolbox.math.normalization;

import java.util.function.Function;

public abstract class Normalizer implements Function<Number, Double>{

    protected abstract Double normalize(Double arg);
    
    @Override
    public final Double apply(Number arg0) {
        return normalize(arg0.doubleValue());
    }

    
    
}
