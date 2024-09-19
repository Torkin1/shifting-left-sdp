package it.torkin.dataminer.control.dataset.raw;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.dao.datasources.Datasource;

public interface IRawDatasetController {

    public void loadDatasource(Datasource datasource, DatasourceConfig config) throws UnableToLoadCommitsException;
}
