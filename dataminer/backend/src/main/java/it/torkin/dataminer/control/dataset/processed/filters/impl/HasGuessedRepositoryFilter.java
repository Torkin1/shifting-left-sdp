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
    
    @Override
    protected Object createState(IssueFilterBean bean){
        HasGuessedRepositoryFilter.State state = new HasGuessedRepositoryFilter.State();
        datasetDao.findAll().forEach(dataset -> state.getDatasets().put(dataset.getName(), dataset));
        return state;
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        
        String name = this.getName();
        HasGuessedRepositoryFilter.State state = (HasGuessedRepositoryFilter.State)bean.getFilterStates().get(name);
        Map<String, Dataset> datasets = state.getDatasets();
        // Map<String, Dataset> datasets = ((HasGuessedRepositoryFilter.State)bean.getFilterStates().get(this.getName())).getDatasets();

        Dataset dataset = datasets.get(bean.getDatasetName());
        return dataset.getGuessedRepoByProjects().containsKey(bean.getIssue().getDetails().getFields().getProject().getKey());
    }
    
}
