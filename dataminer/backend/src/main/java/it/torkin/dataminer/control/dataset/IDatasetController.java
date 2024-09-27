package it.torkin.dataminer.control.dataset;

import java.util.stream.Stream;

import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.entities.dataset.Issue;

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
    Stream<Issue> getProcessedIssues(ProcessedIssuesBean bean);
    
}
