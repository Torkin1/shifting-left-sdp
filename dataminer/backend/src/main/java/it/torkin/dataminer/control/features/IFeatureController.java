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
     */
    public void mineFeatures();
    
    
}
