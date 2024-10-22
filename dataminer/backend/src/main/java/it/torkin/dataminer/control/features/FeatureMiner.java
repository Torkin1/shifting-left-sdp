package it.torkin.dataminer.control.features;

import java.util.function.Consumer;
import java.util.Set;
import java.util.HashSet;

import it.torkin.dataminer.entities.dataset.Feature;
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
        
        if (bean.getIssue().getMeasurementByMeasurementDateName(bean.getMeasurement().getMeasurementDateName()) == null){
            Measurement measurement = bean.getMeasurement();
        
            Set<String> featureNames = this.getFeatureNames();
            Set<Feature> features = new HashSet<>();
            featureNames.forEach(fn -> features.add(new Feature(fn)));
            if (!measurement.getFeatures().containsAll(features)){
                measurement.getFeatures().removeAll(features);
                this.mine(bean);
            }
        }
                 
    }

}
