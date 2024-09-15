package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.jira.issue.IssueStatus;

@Repository
public interface IssueStatusDao extends JpaRepository<IssueStatus, String>{
    
}
