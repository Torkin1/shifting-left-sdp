package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.Issue;


public interface IssueDao extends JpaRepository<Issue, Long> {
    
    Issue findByKey(String key);
}
