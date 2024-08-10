package it.torkin.dataminer.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    
    /** Number of commits in dataset */
    @Column(nullable = false)
    private int nrCommits;

    /** Number of commits linked to an issue in dataset*/
    @Column(nullable = false)
    private int nrLinkedCommits;

    /** Set of issues with multiple commits linked to it */
    @ElementCollection
    private Set<String> issuesWithMultipleCommits = new HashSet<>();

    /**
     * Pairs of commit_id, issue_key that could not be loaded from the dataset
     * (they could be outliers, malformed,
     *  inconsistent with the rest of the dataset, etc...)
     * A null value for the issue_key means that the commit is not linked to any issue.
     * A non null value for the issue key means that we could not retreive details
     * for the issue linked to the commit.
     */
    @ElementCollection
    private Map<String, String> skipped = new HashMap<>();

    public float getLinkage(){
        return (float)nrLinkedCommits/nrCommits;
    }

    public String summary(){

        return String.format("Dataset %s:\n"
            + "Nr of commits: %d\n"
            + "Nr of linked commits: %d\n"
            + "Linkage: %.2f\n"
            + "Nr Issues with multiple commits: %d\n"
            + "Nr Skipped commits: %s\n",
            name, nrCommits, nrLinkedCommits, getLinkage(),
             issuesWithMultipleCommits.size(), skipped.size());

    }
}
