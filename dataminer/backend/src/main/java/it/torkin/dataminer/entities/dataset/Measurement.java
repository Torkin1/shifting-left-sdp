package it.torkin.dataminer.entities.dataset;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import java.lang.Override;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Stores results of the value calculation of a set of Features according to
 * a given prediction date.
 * Measurement is a weak entity. It is not possibile to store
 * a Measurement without a Commit or Issue attached to it.
 * Besides, there cannot be two measurements with the same prediction date
 * linked to the same Issue or Commit.
 */
@Data
@EqualsAndHashCode(exclude = {"issue", "commit"})
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"issue_key", "measurement_date_name", "dataset_id"}),
        @UniqueConstraint(columnNames = {"commit_id", "measurement_date_name", "dataset_id"})
    }
)
@Check(constraints = "(issue_key IS NOT NULL AND commit_id IS NULL) OR (issue_key IS NULL AND commit_id IS NOT NULL)")
public class Measurement {
    
    @Id
    @GeneratedValue
    private long id;

    @ManyToOne
    private Issue issue;

    @OneToOne
    private Commit commit;
    
    /**
     * 
     * When training a prediciton model, we can only leverage "past" data values
     * to predict "future" ones. This date works as a divide
     * between the two time spans. 
     * 
     * For example, a Datum d needed to compute Feature f 
     * can change its value over time.
     * 
     * <pre>
     * 
     * |------------------|???????????????????????????> Values of d over time
     *    past      predictionDate           future...
     *                    ^
     *                    here data values are usable and most fresh!
     * </pre>
     * 
     * Data values used for measurements have to be selected according to a given
     * measurement date.
     * 
     * <pre>
     * |------------|---------------------|???????????????????>
     * 0            ^                     ^                   
     *         Any value in the past       predictionDate is the divide
     *         can be a                    between past and future values
     *         measurementDate
     * </pre>
     * 
     * We can have a measurementDate strictly before a prediction date
     * (i.e. to cope with snoring issues) or a measurementDate == predictionDate, 
     * but NOT a measurementDate > predictionDate.
     * 
     * 
     */
    @Column(nullable = false)
    private Timestamp measurementDate;

    @Column(nullable = false)
    private String measurementDateName;

    @ManyToOne
    private Dataset dataset;

    @ElementCollection
    private Set<Feature> features = new HashSet<>();

    @Override
    public String toString() {
        return "Measurement [id=" + id + ", measurementDate=" + measurementDate
                + ", features=" + features + "]";
    }

    public Feature getFeatureByName(String name){
        return features.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    

}
