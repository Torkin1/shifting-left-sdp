package it.torkin.dataminer.dao.datasources;

import java.util.Iterator;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;

public interface Datasource extends Iterator<Commit>, AutoCloseable {

    public void init(DatasourceConfig config) throws UnableToInitDatasourceException;   


}
