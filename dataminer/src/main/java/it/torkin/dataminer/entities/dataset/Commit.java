package it.torkin.dataminer.entities.dataset;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.entities.Dataset;
import jakarta.persistence.CascadeType;
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
    
    private boolean isBuggy;
    private Timestamp timestamp;
    private String project;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Issue> issues = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    private Dataset dataset;
}
