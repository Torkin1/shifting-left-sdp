package it.torkin.dataminer.control.dataset.raw;

import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import lombok.Data;

@Data
public class ProcessCommitBean {
    
    private final Commit commit;
    private final Dataset dataset;
}
