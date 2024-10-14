package it.torkin.dataminer.control.features;

import java.util.function.Consumer;

import it.torkin.dataminer.control.features.miners.UnableToInitNLPFeaturesMinerException;

public interface FeatureMiner extends Consumer<FeatureMinerBean>{

    public default void init() throws UnableToInitNLPFeaturesMinerException{}

}
