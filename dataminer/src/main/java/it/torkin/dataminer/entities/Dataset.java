package it.torkin.dataminer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Beware that the measurements are to be considered as screenshots of the dataset,
 * thus they could not reflect changes of the entities loaded in db until a new
 * screenshot is taken.
 */
@Entity
@Data
public class Dataset {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique=true, nullable = false)
    private String name;
    
}
