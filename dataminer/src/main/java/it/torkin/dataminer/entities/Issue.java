package it.torkin.dataminer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"key"}),
})
@Data
public class Issue {

    @Id
    private Long id;

    private String key;
    private String title;
    private String projectName;
    private String description;

    private long commitTimestamp;

    private boolean isBuggy;

    
}
