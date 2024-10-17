package it.torkin.dataminer.dao.local;

import java.util.Set;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.torkin.dataminer.entities.dataset.Commit;

public interface CommitDao extends JpaRepository<Commit, Long>{

    public int countByDatasetName(String datasetName);
    public int countByDatasetNameAndRepositoryAndBuggy(String datasetName, String project, boolean buggy);
    public int countByDatasetNameAndBuggy(String datasetName, boolean buggy);
    public int countByDatasetNameAndRepository(String datasetName, String projectName);

    @Query("SELECT c.repository FROM Commit c WHERE c.dataset.name = :datasetName")
    public Set<String> findDistinctRepositoriesByDatasetName(String datasetName);

    /**
     * Finds the number of commits that are linked to a
     * project for each pair of dataset and repository.
     */
    @Query("SELECT c.dataset.name AS dataset, c.repository AS repository, i.details.fields.project.name AS project, COUNT(*) AS total FROM Commit c JOIN c.issues i GROUP BY c.dataset.name, c.repository, i.details.fields.project.name ORDER BY c.dataset.name, i.details.fields.project.name")
    public List<CommitCount> countByDatasetAndRepositoryAndProject();
}
