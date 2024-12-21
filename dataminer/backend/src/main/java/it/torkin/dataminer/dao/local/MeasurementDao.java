package it.torkin.dataminer.dao.local;

import java.util.stream.Stream;

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

    @Query("SELECT m FROM Measurement m WHERE m.commit IS NOT NULL AND m.commit.dataset.name = :dataset ORDER BY m.measurementDate ASC")
    Stream<Measurement> findAllWithCommitByDataset(String dataset);
}
