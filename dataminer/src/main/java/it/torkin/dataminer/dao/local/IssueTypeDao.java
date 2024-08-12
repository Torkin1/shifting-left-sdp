package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.jira.issue.IssueType;

@Repository
public interface IssueTypeDao extends JpaRepository<IssueType, String>{

}
