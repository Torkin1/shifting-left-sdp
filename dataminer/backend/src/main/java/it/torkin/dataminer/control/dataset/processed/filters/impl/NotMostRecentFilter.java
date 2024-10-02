package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.NotMostRecentFilterConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.toolbox.math.SafeMath;

/**
 * #87: Discard % of most recent issues to account for Snoring
 * effects.
 * This filter expects the issues to be temporally sorted.
 */
@Component
public class NotMostRecentFilter extends IssueFilter{

    @Autowired private NotMostRecentFilterConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    
    private Map<String, Long> datasetIssuesCounts = new HashMap<>();
    private Map<String, Long> remainings = new HashMap<>();
    private Map<String, Long> thresholds = new HashMap<>();

    private boolean initialized = false;

    private void init() {

        /** Calculates amount of issues corresponding to the desired percentage */
        List<Dataset> datasets = datasetDao.findAll();
        long datasetIssuesCount;
        for (Dataset dataset : datasets){
            datasetIssuesCount = issueDao.countAllByDatasetName(dataset.getName());
            datasetIssuesCounts.put(dataset.getName(), datasetIssuesCount);
            thresholds.put(dataset.getName(), SafeMath.ceiledInversePercentage(config.getPercentage(), datasetIssuesCount));
        }
        initialized = true;

    }
    
    @Override
    protected void beforeApply(IssueFilterBean bean) {
        remainings.compute(bean.getDatasetName(), (k, v) -> { return SafeMath.nullAsZero(v) - 1; } );
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        return remainings.get(bean.getDatasetName()) >= thresholds.get(bean.getDatasetName());
    }

    @Override
    public void reset() {
        if (!initialized) init();
        datasetIssuesCounts.forEach((dataset, count) -> remainings.compute(dataset, (k, remaining) -> count));    
    }


}
