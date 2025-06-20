package it.torkin.dataminer.control.dataset.raw.datasources;

import java.util.Iterator;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.entities.dataset.Commit;

public interface Datasource extends Iterator<Commit>, AutoCloseable {

    public void init(DatasourceConfig config) throws UnableToInitDatasourceException;   

    public default String featurename(String name){
        return IssueFeature.JIT.getFullName(name);
    }
}
