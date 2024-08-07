package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.apachejit.Issue;

@Repository
public interface IssueDao extends JpaRepository<Issue, String>{

    boolean existsByKey(String issueKey);

    Issue findByKey(String expected);

    
    
}
