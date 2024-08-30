package it.torkin.dataminer.control.dataset;

public interface IDatasetController {

    /**
     * Creates raw dataset from datasources
     * @return 
     * @throws UnableToCreateRawDatasetException
     */
    void createRawDataset() throws UnableToCreateRawDatasetException;
    
}
