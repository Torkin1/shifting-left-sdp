package it.torkin.dataminer.control.dataset.raw;

public interface IRawDatasetController {

    /**
     * Creates raw dataset from datasources
     * @return 
     * @throws UnableToCreateRawDatasetException
     */
    void createRawDataset() throws UnableToCreateRawDatasetException;
    
}
