package it.torkin.dataminer.dao.apachejit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueRecord {
    private String issue_key;
    private String commit_id;
}