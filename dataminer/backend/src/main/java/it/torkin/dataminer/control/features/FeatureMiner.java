package it.torkin.dataminer.control.features;

import java.util.function.Consumer;

public interface FeatureMiner extends Consumer<FeatureMinerBean>{

    public default void init() throws Exception{}

}
