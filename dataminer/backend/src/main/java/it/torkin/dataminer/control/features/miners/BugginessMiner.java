package it.torkin.dataminer.control.features.miners;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.entities.dataset.features.BooleanFeature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;

@Component
public class BugginessMiner extends FeatureMiner{

    @Autowired private IIssueController issueController;

    @Override
    public void mine(FeatureMinerBean bean) {
        Boolean isBuggy = issueController.isBuggy(new IssueCommitBean(bean.getIssue(), bean.getDataset()));
        bean.getMeasurement().getFeatures().add(new BooleanFeature(IssueFeature.BUGGINESS.getFullName(), isBuggy));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(IssueFeature.BUGGINESS.getFullName());
    }
}
