package it.torkin.dataminer.dao.local;

import org.springframework.data.jpa.repository.JpaRepository;

import it.torkin.dataminer.entities.dataset.Measurement;

public interface MeasurementDao extends JpaRepository<Measurement, Long> {
    
}
