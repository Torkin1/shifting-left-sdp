package it.torkin.dataminer.control.dataset.raw.datasources;

import java.io.File;
import java.util.Map;

import it.torkin.dataminer.toolbox.csv.Resultset;
import lombok.Data;

@Data
public class DatasourceFile {

    private final File file;
    private final Resultset<Map<String, String>> records;
    
}
