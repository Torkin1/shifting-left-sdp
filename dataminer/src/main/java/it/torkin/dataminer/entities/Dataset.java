package it.torkin.dataminer.entities;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Dataset {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique=true, nullable = false)
    private String name;

    /**
     * Number of commits that couldn't be loaded from the datasource.
     */
    private int skipped;

    /**
     * Number of commits that couldn't be linked to an issue,
     * divided by project name.
     */
    @ElementCollection
    private Map<String, Integer> unlinkedByProject = new HashMap<>();
    
}
