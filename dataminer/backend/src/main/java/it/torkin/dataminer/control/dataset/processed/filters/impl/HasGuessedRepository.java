package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;

/**
 * Accepts only issues belonging to a project which we can guess what is its
 * main repository.
 */
@Component
public class HasGuessedRepository extends IssueFilter{

    @Autowired private DatasetDao datasetDao;
    private Map<String, Dataset> datasets = new HashMap<>();
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        Dataset dataset = datasets.get(bean.getDatasetName());
        return dataset.getGuessedRepoByProjects().containsKey(bean.getIssue().getDetails().getFields().getProject().getName());
    }

    @Override
    public void reset() {
        datasets.clear();
        datasetDao.findAll().forEach(dataset -> datasets.put(dataset.getName(), dataset));
    }
    
}
