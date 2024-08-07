package it.torkin.dataminer.control.dataset;

import it.torkin.dataminer.entities.Dataset;

public interface IDatasetController {

    /**
     * Loads dataset to make it available to the application
     * @return 
     * @throws UnableToLoadDatasetException
     */
    void loadDataset() throws UnableToLoadDatasetException;

    /**
     * Gets last loaded dataset summary, or null if no dataset has been loaded
     * @return
     */
    Dataset getDataset();
    
}
