package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.dataset.Repository;

public interface RepositoryDao extends JpaRepository<Repository, String> {
    
}
