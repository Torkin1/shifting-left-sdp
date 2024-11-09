package it.torkin.dataminer.control.features.miners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.features.TemporalLocalityConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.issue.IssueMeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.math.SafeMath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
    * #147
    * We can imagine the meaning of this feature as a temperature of the project:
    * An higher value means that the project is very sick and thus subject
    * to bug injection.
 */
@Component
@Slf4j
public class TemporalLocalityMiner extends FeatureMiner{

    private Map<String, Map<String, Long>> issuesInWindowByProjectByDataset = new HashMap<>();

    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;
    @Autowired private IssueDao issueDao;

    @Autowired private TemporalLocalityConfig config;

    @Autowired private IDatasetController datasetController;
    @Autowired private IIssueController issueController;
    
    @Data
    private class IssueCount{
        private double count;

        public void add(double count){
            this.count += count;
        }

    }
    
    private long calcIssuesToSkip(long issuesCount, long issuesInWindow){
        return issuesCount - issuesInWindow;
    }
    
    @Override
    public void mine(FeatureMinerBean bean) {
        
        double temperature = 0.0;

        String dataset = bean.getDataset();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        
        // calc num of issues to skip before reaching the ones in the window
        long issuesCount = issueDao.findAllByDatasetAndProject(dataset, project)
            // accept issues prior to the issue to be measured
            .filter(i -> !issueController.isAfter(new IssueMeasurementDateBean(dataset, i, bean.getIssue(), bean.getMeasurementDate())))
            // exclude the issue to be measured
            .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
            .count();
        long issuesInWindow = issuesInWindowByProjectByDataset.get(dataset).get(project);
        
        // if we don't have enough issues to fill the window, we can't be sure that the temperature value
        // is reliable, so we set it to Nan
        if (issuesCount < issuesInWindow){
            temperature = Double.NaN;
        }
        else {
            long issuesToSkip = calcIssuesToSkip(issuesCount, issuesInWindow);
            IssueCount buggyCount = new IssueCount();
    
            ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(bean.getDataset(), bean.getMeasurementDate());
            datasetController.getProcessedIssues(processedIssuesBean);

            processedIssuesBean.getProcessedIssues()
                // retain only issues of same project
                .filter(i -> i.getDetails().getFields().getProject().getKey().equals(bean.getIssue().getDetails().getFields().getProject().getKey()))
                // retain only issues prior to the issue to be measured
                .filter(i -> !issueController.isAfter(new IssueMeasurementDateBean(dataset, i, bean.getIssue(), bean.getMeasurementDate())))
                // exclude issue to be measured
                .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
                // skip issues until we reach the temporal locality window
                .skip(issuesToSkip)
                // count buggy issues in window
                .forEach(i -> {
                    // skip issue to be measured
                    if ( issueController.isBuggy(new IssueCommitBean(i, bean.getDataset())))
                        buggyCount.add(1);
                });
            
            // calculate temperature as the percentage of buggy issues in window
            temperature = SafeMath.calcPercentage(buggyCount.getCount(), issuesInWindow);
        }
        
        // store temperature in measurement
        bean.getMeasurement().getFeatures().add(new DoubleFeature(
            IssueFeature.TEMPORAL_LOCALITY.getName(),
            temperature));

    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(IssueFeature.TEMPORAL_LOCALITY.getName());
    }

    @Override
    public void init() throws Exception{

        issuesInWindowByProjectByDataset.clear();
        
        // calc num of issues in the window for each dataset and project
        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets){
            
            issuesInWindowByProjectByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            
            Map<String, Long> issuesInWindowByProject = issuesInWindowByProjectByDataset.get(dataset.getName());
            Set<Project> projects = projectDao.findAllByDataset(dataset.getName());

            for (Project project : projects){
                Long projectIssues = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
                Long issuesInWindow = SafeMath.ceiledInversePercentage(config.getWindowSize(), projectIssues);
                issuesInWindowByProject.put(project.getKey(), issuesInWindow);
            }

        }
    }
    
    
}
