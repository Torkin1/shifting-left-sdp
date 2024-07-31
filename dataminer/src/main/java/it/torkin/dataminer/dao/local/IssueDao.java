package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.jira.issue.Issue;


public interface IssueDao extends JpaRepository<Issue, String> {
    
    Issue findByKey(String key);
    Issue findByJiraId(String jiraId);
}
