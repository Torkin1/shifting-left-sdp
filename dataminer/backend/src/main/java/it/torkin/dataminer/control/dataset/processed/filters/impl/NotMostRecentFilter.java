package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueMeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
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
        
    @Autowired private List<MeasurementDate> measurementDates;

    @Autowired private DatasourceGlobalConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;
    @Autowired private IIssueController issueController;

    /**
     * The following attributes are the shared state of the filter.
     * They must be initialized before the filter is applied and
     * stay read-only afterwards.
     */
    private Map<String, Map<String, Map<String, Set<String>>>> snoringIssuesByMeasurementDateByProjectByDataset = new HashMap<>();

    private Long calcSnoringIssuesCount(Dataset dataset, Project project, DatasourceGlobalConfig config) {
        double percentage = config.getSourcesMap().get(dataset.getName()).getSnoringPercentage();
        long issueCount = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
        return SafeMath.ceiledInversePercentage(percentage, issueCount);
    }
    
    @Override
    protected void beforeApply(IssueFilterBean bean) {
        
        // we cache set of snoring issues to avoid multiple queries        
        if (snoringIssuesByMeasurementDateByProjectByDataset.isEmpty()){
            
            List<Dataset> datasets = datasetDao.findAll();
            Set<Project> projects;
            
            for (Dataset dataset : datasets){
                
                snoringIssuesByMeasurementDateByProjectByDataset.put(dataset.getName(), new HashMap<>());
                projects = projectDao.findAllByDataset(dataset.getName());

                for (Project project : projects){
                    
                    long snoringIssuesCount = calcSnoringIssuesCount(dataset, project, config);
                    
                    Map<String, Map<String, Set<String>>> snoringIssuesByMeasurementDateByProject = 
                        snoringIssuesByMeasurementDateByProjectByDataset.get(dataset.getName());
                    snoringIssuesByMeasurementDateByProject.put(project.getKey(), new HashMap<>());
                    
                    for (MeasurementDate measurementDate : measurementDates){
                        
                        Map<String, Set<String>> snoringIssuesByMeasurementDate = 
                            snoringIssuesByMeasurementDateByProject.get(project.getKey());
                        Set<String> snoringIssueKeys = issueDao.findAllByDatasetAndProject(dataset.getName(), project.getKey())
                            // sort issues coming from this dataset and project by measurement date from most to least recent
                            .sorted((i1, i2) -> - issueController.compareMeasurementDate(
                                new IssueMeasurementDateBean(dataset.getName(), i1, i2, measurementDate)))
                            // limit searching to the snoring percentage
                            .limit(snoringIssuesCount)
                            // collect issue keys in corresponding set
                            .collect(
                                HashSet::new,
                                (s, i) -> s.add(i.getKey()),
                                HashSet::addAll
                            );
                        snoringIssuesByMeasurementDate.put(measurementDate.getName(), snoringIssueKeys);      
                    }
                }
            }
        }
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
                
        Set<String> snoringIssues = snoringIssuesByMeasurementDateByProjectByDataset.get(bean.getDatasetName())
            .get(bean.getIssue().getDetails().getFields().getProject().getKey())
            .get(bean.getMeasurementDateName());
        
        return !snoringIssues.contains(bean.getIssue().getKey());
    }

}
