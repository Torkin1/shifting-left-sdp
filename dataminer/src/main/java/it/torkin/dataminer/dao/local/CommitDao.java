package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.dataset.Commit;

public interface CommitDao extends JpaRepository<Commit, Long>{

    public int countByDatasetName(String datasetName);
    
}
