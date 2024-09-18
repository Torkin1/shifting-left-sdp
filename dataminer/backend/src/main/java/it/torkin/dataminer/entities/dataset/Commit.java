package it.torkin.dataminer.entities.dataset;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.Dataset;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"hash", "project", "dataset_id"})
})
public class Commit {
    
    @Id
    @GeneratedValue
    private long id;

    private String hash;
    
    private boolean buggy;
    private Timestamp timestamp;

    /**
     * Must match the name given to the repo (i.e apache/myproject)
     */
    private String project;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Issue> issues = new ArrayList<>();

    @ManyToOne(optional = false)
    private Dataset dataset;
}
