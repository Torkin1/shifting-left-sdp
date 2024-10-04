package it.torkin.dataminer.dao.local;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.torkin.dataminer.entities.jira.project.Project;

@Repository
public interface ProjectDao extends JpaRepository<Project, String>{

    @Query("SELECT DISTINCT i.details.fields.project FROM Issue i JOIN i.commits c WHERE c.dataset.name = :datasetName")
    Set<Project> findAllByDataset(String datasetName);

}
