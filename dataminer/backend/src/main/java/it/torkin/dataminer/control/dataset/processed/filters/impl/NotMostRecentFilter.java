package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateController;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.math.SafeMath;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * #87: Discard % of most recent issues to account for Snoring
 * effects.
 */
@Component
public class NotMostRecentFilter extends IssueFilter{
        
    @RequiredArgsConstructor
    @Getter
    private static class SnoringIssueEntry{
        
        private final String key;
        private final Timestamp timestamp;

    }
    
    @Autowired private MeasurementDateController measurementDateController;

    @Autowired private DatasourceGlobalConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;

    @Autowired private SelectedProjectsFilter selectedProjectsFilter;

    /**
     * The following attributes are the shared state of the filter.
     * They must be initialized before the filter is applied and
     * stay read-only afterwards.
     */
    private Map<String, Map<String, Map<String, Timestamp>>> snoringTimestampByMeasurementDateByProjectByDataset = new HashMap<>();
    
    private Map<String, Map<String, Long>> snoringIssuesCountByProjectByDataset = new HashMap<>();

    private Long calcSnoringIssuesCount(Dataset dataset, Project project, DatasourceGlobalConfig config) {
        double percentage = config.getSourcesMap().get(dataset.getName()).getSnoringPercentage();
        long issueCount = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
        return SafeMath.ceiledInversePercentage(percentage, issueCount);
    }

    private boolean addToSnoringIssues(List<SnoringIssueEntry> snoringIssues, SnoringIssueEntry snoringIssue, long snoringIssuesCount){
        boolean added = false;
        if (snoringIssuesCount == 0) return false;
        if (snoringIssues.size() < snoringIssuesCount){
            snoringIssues.add(snoringIssue);
            added = true;
        } 
        else if (snoringIssue.getTimestamp().after(snoringIssues.get(0).getTimestamp())) {
            snoringIssues.set(0, snoringIssue);
            added = true;
        };
        if (added){
            snoringIssues.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        }
        return added;
    }

    private boolean isSnoring(Timestamp measurementDate, Timestamp snoringTimestamp){
        return !measurementDate.before(snoringTimestamp);
    }
    
    @Override
    protected void _init() {
        
        // we cache set of snoring issues to avoid multiple queries        
        if (snoringTimestampByMeasurementDateByProjectByDataset.isEmpty()){
            
            List<Dataset> datasets = datasetDao.findAll();
            Set<Project> projects;
            List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

            Map<String, Map<String, Map<String, List<SnoringIssueEntry>>>> snoringIssuesByMeasurementDateByProjectByDataset = new HashMap<>();
            
            for (Dataset dataset : datasets){

                snoringIssuesByMeasurementDateByProjectByDataset.put(dataset.getName(), new HashMap<>());
                snoringTimestampByMeasurementDateByProjectByDataset.put(dataset.getName(), new HashMap<>());
                snoringIssuesCountByProjectByDataset.put(dataset.getName(), new HashMap<>());                
                projects = projectDao.findAllByDataset(dataset.getName());

                for (Project project : projects){
                    
                    long snoringIssuesCount = calcSnoringIssuesCount(dataset, project, config);
                    snoringIssuesCountByProjectByDataset.get(dataset.getName()).put(project.getKey(), snoringIssuesCount);
                    
                    Map<String, Map<String, List<SnoringIssueEntry>>> snoringIssuesByMeasurementDateByProject = 
                        snoringIssuesByMeasurementDateByProjectByDataset.get(dataset.getName());
                    snoringIssuesByMeasurementDateByProject.put(project.getKey(), new HashMap<>());
                    Map<String, Map<String, Timestamp>> snoringTimestampByMeasurementDateByProject = 
                        snoringTimestampByMeasurementDateByProjectByDataset.get(dataset.getName());
                        snoringTimestampByMeasurementDateByProject.put(project.getKey(), new HashMap<>());

                    for (MeasurementDate measurementDate : measurementDates){
                        Map<String, List<SnoringIssueEntry>> snoringIssuesByMeasurementDate = 
                            snoringIssuesByMeasurementDateByProject.get(project.getKey());
                        snoringIssuesByMeasurementDate.put(
                            measurementDate.getName(),
                            new ArrayList<>());
                    }
                             
                }

                // applies measurement date to all issues
                selectedProjectsFilter._init();
                Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName())
                        .filter(i -> selectedProjectsFilter.applyFilter(new IssueFilterBean(i, dataset.getName(), null, true)));
                try(issues){
                    Map<String, Map<String, List<SnoringIssueEntry>>> snoringIssuesByMeasurementDateByProject = 
                        snoringIssuesByMeasurementDateByProjectByDataset.get(dataset.getName());
                    issues.forEach(i -> {
                        Project project = i.getDetails().getFields().getProject();
                        Map<String, List<SnoringIssueEntry>> snoringIssuesByMeasurementDate = 
                            snoringIssuesByMeasurementDateByProject.get(project.getKey());
                        for (MeasurementDate measurementDate : measurementDates){
                            List<SnoringIssueEntry> snoringIssues = snoringIssuesByMeasurementDate.get(measurementDate.getName());
                            String issueKey = i.getKey();
                            Optional<Timestamp> issueMeasurementDateValueOptional = measurementDate.apply(new MeasurementDateBean(dataset.getName(), i));
                            Timestamp issueMeasurementDateValue = issueMeasurementDateValueOptional.get();
                            addToSnoringIssues(
                                snoringIssues,
                                new SnoringIssueEntry(issueKey, issueMeasurementDateValue),
                                snoringIssuesCountByProjectByDataset.get(dataset.getName()).get(project.getKey()));
                        }
                    });
                }

                // calcs snoring timestamps
                for (Project project : projects){
                    Map<String, Timestamp> snoringTimestampByMeasurementDate = snoringTimestampByMeasurementDateByProjectByDataset.get(dataset.getName()).get(project.getKey());
                    Map<String, List<SnoringIssueEntry>> snoringIssuesByMeasurementDate = snoringIssuesByMeasurementDateByProjectByDataset.get(dataset.getName()).get(project.getKey());
                    for (MeasurementDate measurementDate : measurementDates){
                        Timestamp snoringTimestamp = calculateSnoringTimestamp(snoringIssuesByMeasurementDate.get(measurementDate.getName()));
                        snoringTimestampByMeasurementDate.put(measurementDate.getName(), snoringTimestamp);
                    }
                }

            }

        }
    }

    private Timestamp calculateSnoringTimestamp(List<SnoringIssueEntry> snoringIssues){
        Timestamp earliest = TimeTools.now();
        for (SnoringIssueEntry snoringIssue : snoringIssues){
            if (snoringIssue.getTimestamp().before(earliest)){
                earliest = snoringIssue.getTimestamp();
            }
        }
        return earliest;
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
                
        Map<String, Timestamp> snoringTimestampByMeasurementDate = snoringTimestampByMeasurementDateByProjectByDataset.get(bean.getDatasetName())
            .get(bean.getIssue().getDetails().getFields().getProject().getKey());
        
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
        for (MeasurementDate measurementDate : measurementDates){
            Timestamp snoringDate = snoringTimestampByMeasurementDate.get(measurementDate.getName());
            if (isSnoring(bean.getMeasurementDate(), snoringDate)){
                return false;
            }
        }
        return true;
    }
}
