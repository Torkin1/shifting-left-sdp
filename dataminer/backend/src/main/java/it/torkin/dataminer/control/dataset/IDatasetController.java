package it.torkin.dataminer.control.dataset;

import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;

/**
 * Facade to interact with dataset
 */
public interface IDatasetController {

    /**
     * Creates raw dataset from datasources
     * @return 
     * @throws UnableToCreateRawDatasetException
     */
    void createRawDataset() throws UnableToCreateRawDatasetException;
    
}
