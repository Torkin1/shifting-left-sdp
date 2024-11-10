package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import lombok.Data;

/**
 * Accepts only issues belonging to a project which we can guess what is its
 * main repository.
 */
@Component
public class HasGuessedRepositoryFilter extends IssueFilter{

    @Data
    private class State{
        private Map<String, Dataset> datasets = new HashMap<>();
    }
    
    @Autowired private DatasetDao datasetDao;

    /**
     * Read-only state, do not modify it unless we are initializing the filter.
     */
    private HasGuessedRepositoryFilter.State state = new HasGuessedRepositoryFilter.State();
        
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        
        if (state.getDatasets().isEmpty()) {
            datasetDao.findAll().forEach(dataset -> state.getDatasets().put(dataset.getName(), dataset));        
        }
        
        Map<String, Dataset> datasets = state.getDatasets();

        Dataset dataset = datasets.get(bean.getDatasetName());
        return dataset.getGuessedRepoByProjects().containsKey(bean.getIssue().getDetails().getFields().getProject().getKey());
    }
    
}
