package it.torkin.dataminer.entities.apachejit;


import java.sql.Timestamp;

import it.torkin.dataminer.dao.apachejit.CommitRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Commit {
    
    @Id
    @GeneratedValue
    private long id;

    @Column(unique = true) private String hash;
    
    private boolean isBuggy;
    private Timestamp timestamp;

    public Commit(CommitRecord record){
        this.hash = record.getCommit_id();
        this.isBuggy = record.isBuggy();
        this.timestamp = new Timestamp(record.getAuthor_date());
    }


}
