package it.torkin.dataminer.control.dataset.processed;

public interface IProcessedDatasetController {

    /**
     * Queries the db for issues returning only the ones
     * that are allowed by the registered filters.
     * @param datasetName From which dataset we want to fetch issues from.
     * An issue belongs to a dataset if it has at least one commit belonging
     * to that dataset.
     * @return Stream of issues
     */
    void getFilteredIssues(ProcessedIssuesBean bean);
   
    /**
     * Call this after the raw dataset has been created
     */
    void initFilters();

}
