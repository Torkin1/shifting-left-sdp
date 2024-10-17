package it.torkin.dataminer.entities.dataset;

import jakarta.persistence.FetchType;

import java.sql.Timestamp;
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
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, Integer> unlinkedByRepository = new HashMap<>();

    /**
     * Number of buggy commits that couldn't be linked to an issue,
     * divided by project name.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, Integer> buggyUnlinkedByRepository = new HashMap<>();

    private Timestamp lastUpdatedTime;

    /**
     * #128: Use this map to lookup the guessed main repository for a project
     * according to this dataset.
     * 
     * If the project is not in the map, no guess can be taken from the repositories
     * loaded from this dataset.
     */
    @ElementCollection
    private Map<String, String> guessedRepoByProjects = new HashMap<>();
    
}
