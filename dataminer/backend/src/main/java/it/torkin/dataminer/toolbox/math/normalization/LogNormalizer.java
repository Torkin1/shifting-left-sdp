package it.torkin.dataminer.toolbox.math.normalization;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class LogNormalizer extends Normalizer{

    @NonNull
    private final Double base;
    
    @Override
    public Double normalize(Double arg0) {
        
        if (base <= 0) throw new IllegalStateException("Logarithm base should be > 0, but is " + base);
        
        if (base == Math.E) return Math.log(arg0);
        else if (base == 10.0) return Math.log10(arg0);
        else return Math.log(arg0) / Math.log(base);
    }

    
    
}
