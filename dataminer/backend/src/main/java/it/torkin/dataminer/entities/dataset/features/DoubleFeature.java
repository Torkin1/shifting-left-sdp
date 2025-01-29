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
public class DoubleFeature extends Feature<Double> {

    private Double value;
    
    public DoubleFeature(String name, Double value) {
        super(name);
        this.value = value;
    }

    public DoubleFeature(String name, Double value, FeatureAggregation aggregation) {
        super(name, aggregation);
        this.value = value;
    }

    @Override
    public Feature<Double> clone() {
        return new DoubleFeature(name, value, aggregation);
    }
    
}
