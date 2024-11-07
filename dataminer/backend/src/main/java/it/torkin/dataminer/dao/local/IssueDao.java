package it.torkin.dataminer.dao.local;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.dataset.Issue;

@Repository
public interface IssueDao extends JpaRepository<Issue, String>{

    boolean existsByKey(String issueKey);
    Issue findByKey(String expected);

    @Query("SELECT DISTINCT i FROM Issue i JOIN i.commits c WHERE c.dataset.name = :datasetName")
    Stream<Issue> findAllByDataset(String datasetName);

    @Query("SELECT COUNT(DISTINCT i) FROM Issue i JOIN i.commits c WHERE c.dataset.name = :datasetName")
    long countByDataset(String datasetName);

    @Query("SELECT COUNT(DISTINCT i) FROM Issue i JOIN i.commits c WHERE c.dataset.name = :datasetName AND i.details.fields.project.key = :projectKey")
    long countByDatasetAndProject(String datasetName, String projectKey);

    @Query("SELECT DISTINCT i FROM Issue i JOIN i.commits c WHERE c.dataset.name = :datasetName AND i.details.fields.project.key = :projectKey")
    Stream<Issue> findAllByDatasetAndProject(String datasetName, String projectKey);

    
}
