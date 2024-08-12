package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.jira.Component;

@Repository
public interface ComponentDao extends JpaRepository<Component, String>{
    
}
