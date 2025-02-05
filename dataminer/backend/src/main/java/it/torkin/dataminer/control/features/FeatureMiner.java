package it.torkin.dataminer.control.features;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import it.torkin.dataminer.entities.dataset.Measurement;
import lombok.extern.slf4j.Slf4j;

/**
 * Mines a set of features from a given issue using the provided measurement
 * date.
 * 
 * Features are stored in the Measurement object corresponding to the
 * provided measurement date.
 * 
 * Feature is mined only if it has not been already mined for the
 * related measurement date.
 */
@Slf4j
public abstract class FeatureMiner implements Consumer<FeatureMinerBean>{

    public void init() throws Exception{}

    public abstract void mine(FeatureMinerBean bean);

    /**
     * Lists the names of the features that will be mined by this miner.
     * The feature name must be different for each variant of the feature.
     * @return
     */
    protected abstract Set<String> getFeatureNames();

    @Override
    public final void accept(FeatureMinerBean bean) {
        
            
        Measurement measurement = bean.getMeasurement();
        Set<String> featureNames = this.getFeatureNames();
        Set<String> measurementFeatureNames = new HashSet<>();

        // if the measurement already contains the features, skip mining.
        // One missing feature provided by the miner is enough to trigger mining
        // of all the features it provides.
        measurement.getFeatures().forEach(f -> measurementFeatureNames.add(f.getName()));
        if (!measurementFeatureNames.containsAll(featureNames)){
            measurement.getFeatures().removeIf(f -> featureNames.contains(f.getName()));
            // log.info("{} on issue {}", getClass().getSimpleName(), bean.getIssue().getKey());
            this.mine(bean);
        }
    }
                 

}
