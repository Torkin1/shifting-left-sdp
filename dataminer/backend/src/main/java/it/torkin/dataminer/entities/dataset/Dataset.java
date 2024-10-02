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
    
}
