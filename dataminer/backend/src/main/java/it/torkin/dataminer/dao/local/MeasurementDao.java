package it.torkin.dataminer.dao.local;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import it.torkin.dataminer.entities.dataset.features.Feature;
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

    /*
    * Returns a raw mapping of feature objects containing only name and aggregation */
    @Query(nativeQuery = true,
            value ="select f.aggregation, f.name from feature f join measurement_features mf on mf.features_id = f.id where mf.measurement_id in (SELECT m.id FROM measurement m JOIN dataset d ON m.dataset_id = d.id WHERE m.commit_id is not NULL AND d.name = ?1 limit 1)")
    List<Map<String, String>> findFeaturesPrototypeByDataset(String dataset);

}
