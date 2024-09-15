package it.torkin.dataminer.entities.jira;


import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Developer{
    private String accountType;
    private boolean active;
    @Embedded
    private AvatarUrls avatarUrls;
    private String displayName;
    @Id
    private String key;
    private String name;
    private String self;
    private String timeZone;
}
