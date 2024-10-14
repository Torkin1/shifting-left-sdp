package it.torkin.dataminer.control.features;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class FeatureController implements IFeatureController{

    @Autowired private List<FeatureMiner> miners;
    
    @Override
    public void initMiners() throws Exception{
        miners.forEach(miner -> {
            try {
                miner.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
