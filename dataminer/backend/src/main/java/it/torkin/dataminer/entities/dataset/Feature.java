package it.torkin.dataminer.entities.dataset;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * named quantity used by a prediction model to learn relationships among data.
 */
@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class Feature {
    
    private String name;
    /**
     * Value is stored as a string to make it digestible
     * by the database, so we need to store the type as well
     * if we want to manipulate the value.
     */
    private String value;
    private Class<?> type;
}
