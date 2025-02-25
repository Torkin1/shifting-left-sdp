package it.torkin.dataminer.control.features.miners;

import java.util.Set;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.StringFeature;

@Component
public class IssueKeyMiner extends FeatureMiner{

    private static final String FEATURE_NAME = IssueFeature.KEY.getFullName();

    @Override
    public void mine(FeatureMinerBean bean) {
        
        Issue issue = bean.getIssue();
        bean.getMeasurement().getFeatures().add(new StringFeature(FEATURE_NAME, issue.getKey()));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(FEATURE_NAME);
    }
    
}
