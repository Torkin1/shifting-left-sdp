package it.torkin.dataminer.dao.local;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.torkin.dataminer.entities.dataset.Measurement;

public interface MeasurementDao extends JpaRepository<Measurement, Long> {

    @Query("SELECT COUNT(m) FROM Measurement m WHERE m.issue.details.fields.project.key = :projectKey AND m.measurementDateName = :measurementDateName AND m.dataset.name = :datasetName")
    long countByProjectAndDatasetAndMeasurementDateName(String projectKey, String datasetName, String measurementDateName);
    
    @Query("SELECT m FROM Measurement m WHERE m.issue.details.fields.project.key = :projectKey AND m.measurementDateName = :measurementDateName AND m.dataset.name = :datasetName ORDER BY m.measurementDate ASC")
    Stream<Measurement> findAllByProjectAndDatasetAndMeasurementDateName(String projectKey, String datasetName, String measurementDateName);
    
    @Query("SELECT m FROM Measurement m")
    Stream<Measurement> findAllStreaming();

    @Query("SELECT m FROM Measurement m WHERE m.issue IS NOT NULL ORDER BY m.measurementDate ASC")
    Stream<Measurement> findAllWithIssue();

    @Query("SELECT m FROM Measurement m INNER JOIN m.commit.issues i JOIN i.measurements im WHERE m.commit.dataset.name = :dataset AND i.details.fields.project.key = :project AND im.measurementDateName = :measurementDateName ORDER BY m.measurementDate ASC")
    Stream<Measurement> findAllWithCommitByDatasetAndProjectAndMeasurementDate(String dataset, String project, String measurementDateName);

    @Query("SELECT m FROM Measurement m WHERE m.commit.dataset.name = :dataset")
    List<Measurement> findWithCommitByDatasetLimited(String dataset, Pageable pageable);

}
