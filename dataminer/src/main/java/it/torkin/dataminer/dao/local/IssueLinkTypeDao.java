package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.jira.issue.IssueLinkType;

public interface IssueLinkTypeDao extends JpaRepository<IssueLinkType, String> {

    
}
