package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.jira.Developer;

public interface DeveloperDao extends JpaRepository<Developer, String> {

    Developer findByKey(String key);

    
    
}
