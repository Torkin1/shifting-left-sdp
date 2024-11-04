package it.torkin.dataminer.control.features;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import it.torkin.dataminer.entities.dataset.Measurement;

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
public abstract class FeatureMiner implements Consumer<FeatureMinerBean>{

    public void init() throws Exception{}

    public abstract void mine(FeatureMinerBean bean);

    protected abstract Set<String> getFeatureNames();

    @Override
    public final void accept(FeatureMinerBean bean) {
        
            
        Measurement measurement = bean.getMeasurement();
        Set<String> featureNames = this.getFeatureNames();
        Set<String> measurementFeatureNames = new HashSet<>();

        measurement.getFeatures().forEach(f -> measurementFeatureNames.add(f.getName()));
        if (!measurementFeatureNames.containsAll(featureNames)){
            measurement.getFeatures().removeIf(f -> featureNames.contains(f.getName()));
            this.mine(bean);
        }
    }
                 

}
