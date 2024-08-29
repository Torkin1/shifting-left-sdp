package it.torkin.dataminer.entities.dataset;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import it.torkin.dataminer.dao.apachejit.CommitRecord;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"hash", "repoName"})
})
public class Commit {
    
    @Id
    @GeneratedValue
    private long id;

    private String hash;
    
    private boolean isBuggy;
    private Timestamp timestamp;
    private String repoName;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Issue> issues = new ArrayList<>();

    public Commit(CommitRecord record){
        this.hash = record.getCommit_id();
        this.isBuggy = record.isBuggy();
        this.repoName = record.getProject();
    }


}
