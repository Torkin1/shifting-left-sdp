package it.torkin.dataminer.entities.dataset;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * named feature.
 */
@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class Feature {
    
    private String name;
    /**
     * Value is stored as a string to make it digestible
     * by the database, so we need to store the type as well.
     */
    private String value;
    private Class<?> type;
}
