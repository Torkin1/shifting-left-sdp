package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.jira.issue.IssueResolution;

public interface IssueResolutionDao extends JpaRepository<IssueResolution, String>{
    
}
