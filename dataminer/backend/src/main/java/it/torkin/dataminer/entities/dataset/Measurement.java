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
        @UniqueConstraint(columnNames = {"issue_key", "prediction_date"}),
        @UniqueConstraint(columnNames = {"commit_id", "prediction_date"})
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
     * | -----------------|???????????????????????????> Values of d over time
     *    past      predictionDate           future...
     *                    ^
     *                    here data values are usable and most fresh!
     * </pre>
     * 
     * Since we want our model to have access
     * to the most recent data in the hope that it can better grasp the relationsips
     * among the features, feature values of a Measurement must be calculated
     * using data values at the time represented by the prediciton date.
     */
    @Column(nullable = false)
    private Timestamp predictionDate;

    @ElementCollection
    private Set<Feature> features = new HashSet<>();

    @Override
    public String toString() {
        return "Measurement [id=" + id + ", predictionDate=" + predictionDate
                + ", features=" + features + "]";
    }

    

}
