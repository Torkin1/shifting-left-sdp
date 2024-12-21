package it.torkin.dataminer.entities.dataset.features;

import jakarta.persistence.Entity;
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
public abstract class Feature<T> {
    
    @Id
    @GeneratedValue
    protected Long id;
    
    /**
     * Name of feature
     */
    private String name;

    protected Feature(String name) {
        this.name = name;
    }

    public abstract T getValue();
}
