package it.torkin.dataminer.entities.dataset.features;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@ToString(callSuper = true)
public class IntegerFeature extends Feature<Integer> {

    private Integer value;
    
    public IntegerFeature(String name, Integer value) {
        super(name);
        this.value = value;
    }

    public IntegerFeature(String name, Integer value, FeatureAggregation aggregation) {
        super(name, aggregation);
        this.value = value;
    }
    
    @Override
    public Feature<Integer> clone() {
        return new IntegerFeature(name, value, aggregation);
    }
}
