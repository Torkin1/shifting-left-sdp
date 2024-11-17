package it.torkin.dataminer.control.features;

/**
 * Facade for the feature extraction process
 */
public interface IFeatureController {

    /**
     * Prepares the miner so that they can be later used to extract features.
     */
    public void initMiners() throws Exception;

    /**
     * Extracts features from the data and stores them in DB.
     * Then, Prints the measurements into csv files in output folder.
     * Each file contains the measurements of all issue features of a same
     * project, from a dataset according to a specific measurement date.
     * Each line of the file contains the measurements of a single issue.
     */
    public void mineFeatures();
        
}
