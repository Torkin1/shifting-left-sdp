package it.torkin.dataminer.dao.local;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.torkin.dataminer.entities.dataset.Commit;

public interface CommitDao extends JpaRepository<Commit, Long>{

    public int countByDatasetName(String datasetName);
    public int countByDatasetNameAndProjectAndBuggy(String datasetName, String project, boolean buggy);
    public int countByDatasetNameAndBuggy(String datasetName, boolean buggy);
    public int countByDatasetNameAndProject(String datasetName, String projectName);

    @Query("SELECT DISTINCT c.project FROM Commit c JOIN Dataset d ON c.dataset.id = dataset.id WHERE d.name = ?1")
    public Set<String> findDistinctProjectsByDatasetName(String datasetName);
}
