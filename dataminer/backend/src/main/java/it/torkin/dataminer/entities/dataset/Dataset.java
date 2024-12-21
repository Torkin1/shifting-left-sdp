package it.torkin.dataminer.entities.dataset;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

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
     * #128: Use this map to lookup the guessed main repository for a project key
     * according to this dataset.
     * 
     * If the project is not in the map, no guess can be taken from the repositories
     * loaded from this dataset.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> guessedRepoByProjects = new HashMap<>();

    /**
     * Gets the project key by the guessed repository, if the repository is
     * actually linked to a project, else null
     * @param repo
     * @return
     */
    public Optional<String> getProjectByGuessedRepo(String repo){
        for (Entry<String, String> entry : guessedRepoByProjects.entrySet()){
            if (entry.getValue().equals(repo)){
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
    
}
