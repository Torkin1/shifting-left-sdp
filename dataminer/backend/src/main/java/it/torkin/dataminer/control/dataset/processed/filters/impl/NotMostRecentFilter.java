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

/**
 * #87: Discard % of most recent issues to account for Snoring
 * effects.
 * This filter expects the issues to be temporally sorted.
 */
@Component
public class NotMostRecentFilter extends IssueFilter{

    @Autowired private DatasourceGlobalConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;
    
    private Map<String, Map<String, Long>> issuesCountByProjectGroupedByDataset = new HashMap<>();
    private Map<String, Map<String, Long>> remainingsByProjectGroupedByDataset = new HashMap<>();
    private Map<String, Map<String, Long>> thresholdsByProjectGroupedByDataset = new HashMap<>();

    private boolean initialized = false;

    
    private void init() {

        /** Calculates amount of issues corresponding to the desired percentage */
        List<Dataset> datasets = datasetDao.findAll();
        Set<Project> projects;
        for (Dataset dataset : datasets){
            thresholdsByProjectGroupedByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            issuesCountByProjectGroupedByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            remainingsByProjectGroupedByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            double percentage = config.getSourcesMap().get(dataset.getName()).getSnoringPercentage();

            projects = projectDao.findAllByDataset(dataset.getName());
            for (Project project : projects){
                long issueCount = issueDao
                    .countByDatasetAndProject(dataset.getName(), project.getKey());
                issuesCountByProjectGroupedByDataset.get(dataset.getName())
                    .put(project.getKey(), issueCount);
                remainingsByProjectGroupedByDataset.get(dataset.getName())
                    .put(project.getKey(), issueCount);
                thresholdsByProjectGroupedByDataset.get(dataset.getName())
                    .put(project.getKey(), SafeMath.ceiledInversePercentage(percentage, issueCount));
            }
        }
        initialized = true;

    }
    
    @Override
    protected void beforeApply(IssueFilterBean bean) {
        String dataset = bean.getDatasetName();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        remainingsByProjectGroupedByDataset.get(dataset).compute(project, (k, v) -> { return v - 1; } );
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        String dataset = bean.getDatasetName();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        return remainingsByProjectGroupedByDataset.get(dataset).get(project) >= thresholdsByProjectGroupedByDataset.get(dataset).get(project);
    }

    @Override
    public void reset() {
        if (!initialized) init();
        remainingsByProjectGroupedByDataset.forEach((dataset, remainingsByProject) -> {
            remainingsByProject.replaceAll((project, remaining) -> issuesCountByProjectGroupedByDataset.get(dataset).get(project));
        });
    }


}
