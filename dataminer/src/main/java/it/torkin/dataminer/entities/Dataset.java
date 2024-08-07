package it.torkin.dataminer.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Summary of statistics and measurement on a dataset.
 * Beware that the measurements are to be considered as screenshots of the dataset,
 * thus they could not reflect changes of the entities loaded in db.
 */
@Entity
@Data
public class Dataset {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique=true, nullable = false)
    private String name;
    
    /**
     * How many issues are in the dataset
     */
    @Column(nullable = false)
    private int nrIssueRecords;
    @Column(nullable = false)
    private int nrCommits;

    /**
     * Issues that could not be loaded from the dataset
     * (they could be outliers, malformed,
     *  inconsistent with the rest of the dataset, etc...)
     */
    @ElementCollection
    private List<String> skippedIssuesKeys = new ArrayList<>();

    
}
