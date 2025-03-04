package it.torkin.dataminer.control.features.miners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.features.TemporalLocalityConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.issue.IssueMeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.DoubleFeature;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.Holder;
import it.torkin.dataminer.toolbox.math.SafeMath;
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

    private static final String WEIGHTED = IssueFeature.TEMPORAL_LOCALITY.getFullName("weighted");
    private static final String TEMPERATURE = IssueFeature.TEMPORAL_LOCALITY.getFullName();
    
    private Map<String, Map<String, Long>> issuesInWindowByProjectByDataset = new HashMap<>();
    private Map<String, Map<String, Long>> issuesCountByProjectByDataset = new HashMap<>();

    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;
    @Autowired private IssueDao issueDao;

    @Autowired private TemporalLocalityConfig config;

    @Autowired private IDatasetController datasetController;
    @Autowired private IIssueController issueController;
            
    @Override
    public void mine(FeatureMinerBean bean) {
        
        Holder<Double> temperature = new Holder<>(0.0);
        Holder<Double> weightedTemperature = new Holder<>(0.0);

        String dataset = bean.getDataset();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        
        // calc num of issues to skip before reaching the ones in the window
        long issuesCount = issuesCountByProjectByDataset.get(dataset).get(project);
        long issuesInWindow = issuesInWindowByProjectByDataset.get(dataset).get(project);
        
        // if we don't have enough issues to fill the window, we can't be sure that the temperature value
        // is reliable, so we set it to Nan
        if (issuesCount < issuesInWindow){
            temperature.setValue(Double.NaN);
            weightedTemperature.setValue(Double.NaN);
        }
        else {
    
            ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(bean.getDataset(), bean.getMeasurementDate());
            datasetController.getProcessedIssues(processedIssuesBean);

            try(Stream<Issue> issues = processedIssuesBean.getProcessedIssues()){
                Holder<Integer> j = new Holder<>(0);
                issues
                    // retain only issues of same project
                    .filter(i -> i.getDetails().getFields().getProject().getKey().equals(bean.getIssue().getDetails().getFields().getProject().getKey()))
                    // retain only issues prior to the issue to be measured
                    .filter(i -> !issueController.isAfter(new IssueMeasurementDateBean(dataset, i, bean.getIssue(), bean.getMeasurementDate())))
                    // exclude issue to be measured
                    .filter(i -> !i.getKey().equals(bean.getIssue().getKey()))
                    // sort issues by measurement date from most to least recent
                    .sorted((i1, i2) -> - issueController.compareMeasurementDate(new IssueMeasurementDateBean(bean.getDataset(), i1, i2, bean.getMeasurementDate())))
                    // limit to issues in window
                    .limit(issuesInWindow)
                    .forEach(i -> {
                        
                        if (issueController.isBuggy(new IssueCommitBean(i, dataset))){
                            // count buggy issues in window
                            // temperature is the proportion of buggy issues in the window
                            temperature.setValue(temperature.getValue() + (1.0 / issuesInWindow));

                            // weighted count. Issue weight is its position in the window starting from the most recent issue.
                            // So, oldest issues have less weight.
                            long weight = issuesInWindow - j.getValue();
                            double sumOfweights = SafeMath.sumOfFirst(issuesInWindow);
                            weightedTemperature.setValue(weightedTemperature.getValue() + (1.0 * weight / sumOfweights));
                        }
                        
                        j.setValue(j.getValue() + 1);

                    });

            }
            
        }
        
        // store temperature in measurement
        bean.getMeasurement().getFeatures().add(new DoubleFeature(
            TEMPERATURE,
            temperature.getValue()));
        bean.getMeasurement().getFeatures().add(new DoubleFeature(
            WEIGHTED,
            weightedTemperature.getValue()));

    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(TEMPERATURE, WEIGHTED);
    }

    @Override
    @Transactional
    public void init() throws Exception{

        issuesInWindowByProjectByDataset.clear();
        
        // calc num of issues in the window for each dataset and project
        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets){
            
            issuesInWindowByProjectByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            issuesCountByProjectByDataset.putIfAbsent(dataset.getName(), new HashMap<>());
            
            Map<String, Long> issuesInWindowByProject = issuesInWindowByProjectByDataset.get(dataset.getName());
            Map<String, Long> issuesCountByProject = issuesCountByProjectByDataset.get(dataset.getName());
            Set<Project> projects = projectDao.findAllByDataset(dataset.getName());

            for (Project project : projects){
                Long projectIssues = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
                Long issuesInWindow = SafeMath.ceiledInversePercentage(config.getWindowSize(), projectIssues);
                issuesInWindowByProject.put(project.getKey(), issuesInWindow);
                issuesCountByProject.put(project.getKey(), projectIssues);
            }

        }
    }
    
    
}
