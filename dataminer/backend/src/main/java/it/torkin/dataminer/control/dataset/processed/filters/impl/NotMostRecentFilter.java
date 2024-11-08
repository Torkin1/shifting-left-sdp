package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.math.SafeMath;
import lombok.Data;

/**
 * #87: Discard % of most recent issues to account for Snoring
 * effects.
 * This filter expects the issues to be temporally sorted.
 */
@Component
public class NotMostRecentFilter extends IssueFilter{

    @Data
    private class State{
        
        private Map<String, Map<String, Long>> issuesCountByProjectGroupedByDataset = new HashMap<>();
        private Map<String, Map<String, Long>> remainingsByProjectGroupedByDataset = new HashMap<>();
        private Map<String, Map<String, Long>> thresholdsByProjectGroupedByDataset = new HashMap<>();
    
    }
    
    @Autowired private DatasourceGlobalConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;
    
    @Override
    protected Object createState(IssueFilterBean bean){
        NotMostRecentFilter.State state = new NotMostRecentFilter.State();
        init(state);
        return state;
    }
    
    private void init(NotMostRecentFilter.State state) {

        /** Calculates amount of issues corresponding to the desired percentage */
        List<Dataset> datasets = datasetDao.findAll();
        Set<Project> projects;
        for (Dataset dataset : datasets){
            state.getThresholdsByProjectGroupedByDataset().putIfAbsent(dataset.getName(), new HashMap<>());
            state.getIssuesCountByProjectGroupedByDataset().putIfAbsent(dataset.getName(), new HashMap<>());
            state.getRemainingsByProjectGroupedByDataset().putIfAbsent(dataset.getName(), new HashMap<>());
            double percentage = config.getSourcesMap().get(dataset.getName()).getSnoringPercentage();

            projects = projectDao.findAllByDataset(dataset.getName());
            for (Project project : projects){
                long issueCount = issueDao
                    .countByDatasetAndProject(dataset.getName(), project.getKey());
                state.getIssuesCountByProjectGroupedByDataset().get(dataset.getName())
                    .put(project.getKey(), issueCount);
                state.getRemainingsByProjectGroupedByDataset().get(dataset.getName())
                    .put(project.getKey(), issueCount);
                state.getThresholdsByProjectGroupedByDataset().get(dataset.getName())
                    .put(project.getKey(), SafeMath.ceiledInversePercentage(percentage, issueCount));
            }
        }

    }
    
    @Override
    protected void beforeApply(IssueFilterBean bean) {
        
        NotMostRecentFilter.State state = (NotMostRecentFilter.State)bean.getFilterStates().get(this.getName());
        String dataset = bean.getDatasetName();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        state.getRemainingsByProjectGroupedByDataset().get(dataset).compute(project, (k, v) -> { return v - 1; } );
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        
        NotMostRecentFilter.State state = (NotMostRecentFilter.State)bean.getFilterStates().get(this.getName());
        
        String dataset = bean.getDatasetName();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        return state.getRemainingsByProjectGroupedByDataset().get(dataset).get(project) >= state.getThresholdsByProjectGroupedByDataset().get(dataset).get(project);
    }

}
