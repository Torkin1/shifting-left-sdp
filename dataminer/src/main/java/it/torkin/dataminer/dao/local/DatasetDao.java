package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.Dataset;

public interface DatasetDao extends JpaRepository<Dataset, Long> {
    
}
