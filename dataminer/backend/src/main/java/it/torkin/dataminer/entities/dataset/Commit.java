package it.torkin.dataminer.entities.dataset;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"hash", "repository_id", "dataset_id"})
})
public class Commit {
    
    @Id
    @GeneratedValue
    private long id;

    private String hash;
    
    private boolean buggy;
    private Timestamp timestamp;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Issue> issues = new ArrayList<>();

    @ManyToOne(optional = false)
    private Dataset dataset;

    /**
     * NOTE: commits have only one prediction date, namely
     * the date of commit.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "commit")
    private Measurement measurement;

    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Repository repository;

    @Override
    public String toString() {
        return "Commit [id=" + id + ", hash=" + hash + ", buggy=" + buggy + ", timestamp=" + timestamp + "repository="+repository+ "]";
    }

}
