package it.torkin.dataminer.dao.apachejit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommitRecord {
    private String commit_id; // commit hash
    private String project;   // project key
    private boolean buggy;    // true if the commit induced a bug
}
