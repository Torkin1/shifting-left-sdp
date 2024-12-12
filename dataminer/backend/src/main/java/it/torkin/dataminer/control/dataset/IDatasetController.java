package it.torkin.dataminer.control.dataset;

import java.io.IOException;

import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
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

    /**
     * Gets processed issues from a dataset 
     */
    void getProcessedIssues(ProcessedIssuesBean bean);

    /**
     * Prints nlp issue beans in a json file
     * @throws IOException 
     */
    void printNLPIssueBeans() throws IOException;
    
}
