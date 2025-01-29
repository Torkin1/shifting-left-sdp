package it.torkin.dataminer.entities.dataset.features;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * named quantity used by a prediction model to learn relationships among data.
 */
@Data
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@Entity
public abstract class Feature<T> implements Cloneable{
    
    @Id
    @GeneratedValue
    protected Long id;
    
    /**
     * Name of feature
     */
    protected String name;

    /**
     * If the feature is aggregable, this field contains the aggregation strategy to apply.
     */
    @Enumerated(EnumType.STRING)
    protected FeatureAggregation aggregation;

    protected Feature(String name) {
        this.name = name;
    }

    protected Feature(String name, FeatureAggregation aggregation) {
        this.name = name;
        this.aggregation = aggregation;
    }


    public abstract T getValue();

    @Override
    public abstract Feature<T> clone();

}
