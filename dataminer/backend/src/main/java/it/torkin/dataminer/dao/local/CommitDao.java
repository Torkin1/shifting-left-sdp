package it.torkin.dataminer.dao.local;

import java.util.Set;

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
}
