package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.jira.project.Project;

@Repository
public interface ProjectDao extends JpaRepository<Project, String>{

}
