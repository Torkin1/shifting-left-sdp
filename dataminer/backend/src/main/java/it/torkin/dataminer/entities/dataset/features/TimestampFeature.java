package it.torkin.dataminer.entities.dataset.features;

import java.sql.Timestamp;

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
public class TimestampFeature extends Feature<Timestamp> {

    private Timestamp value;
    
    public TimestampFeature(String name, Timestamp value) {
        super(name);
        this.value = value;
    }

    public TimestampFeature(String name, Timestamp value, FeatureAggregation aggregation) {
        super(name, aggregation);
        this.value = value;
    }

    @Override
    public Feature<Timestamp> clone() {
        return new TimestampFeature(name, (Timestamp) value.clone(), aggregation);
    }
}
