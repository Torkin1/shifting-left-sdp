package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SnoringIssueEntry that = (SnoringIssueEntry) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode(){
            return key.hashCode();
        }

    }
    
    @Autowired private MeasurementDateController measurementDateController;

    @Autowired private DatasourceGlobalConfig config;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ProjectDao projectDao;

    /**
     * The following attributes are the shared state of the filter.
     * They must be initialized before the filter is applied and
     * stay read-only afterwards.
     */
    private Map<String, Map<String, Map<String, List<SnoringIssueEntry>>>> snoringIssuesByMeasurementDateByProjectByDataset = new HashMap<>();
    private Map<String, Map<String, Long>> snoringIssuesCountByProjectByDataset = new HashMap<>();

    private Long calcSnoringIssuesCount(Dataset dataset, Project project, DatasourceGlobalConfig config) {
        double percentage = config.getSourcesMap().get(dataset.getName()).getSnoringPercentage();
        long issueCount = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
        return SafeMath.ceiledInversePercentage(percentage, issueCount);
    }

    private boolean addToSnoringIssues(List<SnoringIssueEntry> snoringIssues, SnoringIssueEntry snoringIssue, long snoringIssuesCount){
        boolean added = false;
        if (snoringIssues.size() < snoringIssuesCount){
            snoringIssues.add(snoringIssue);
            added = true;
        } 
        if (!snoringIssue.getTimestamp().after(snoringIssues.get(0).getTimestamp())) {
            snoringIssues.set(0, snoringIssue);
            added = true;
        };
        if (added == true){
            snoringIssues.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        }
        return added;
    }

    private boolean isSnoring(Issue issue, List<SnoringIssueEntry> snoringIssues){
        String issuekey = issue.getKey();
        return snoringIssues.stream().anyMatch(sie -> sie.getKey().equals(issuekey));
    }
    
    @Override
    protected void beforeApply(IssueFilterBean bean) {
        
        // we cache set of snoring issues to avoid multiple queries        
        if (snoringIssuesByMeasurementDateByProjectByDataset.isEmpty()){
            
            List<Dataset> datasets = datasetDao.findAll();
            Set<Project> projects;
            List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();
            
            for (Dataset dataset : datasets){

                snoringIssuesByMeasurementDateByProjectByDataset.put(dataset.getName(), new HashMap<>());
                snoringIssuesCountByProjectByDataset.put(dataset.getName(), new HashMap<>());                
                projects = projectDao.findAllByDataset(dataset.getName());

                for (Project project : projects){
                    
                    long snoringIssuesCount = calcSnoringIssuesCount(dataset, project, config);
                    snoringIssuesCountByProjectByDataset.get(dataset.getName()).put(project.getKey(), snoringIssuesCount);
                    
                    Map<String, Map<String, List<SnoringIssueEntry>>> snoringIssuesByMeasurementDateByProject = 
                        snoringIssuesByMeasurementDateByProjectByDataset.get(dataset.getName());
                    snoringIssuesByMeasurementDateByProject.put(project.getKey(), new HashMap<>());

                    for (MeasurementDate measurementDate : measurementDates){
                        Map<String, List<SnoringIssueEntry>> snoringIssuesByMeasurementDate = 
                            snoringIssuesByMeasurementDateByProject.get(project.getKey());
                        snoringIssuesByMeasurementDate.put(
                            measurementDate.getName(),
                            new ArrayList<>());
                    }
                             
                }

                Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName());
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
                            Timestamp issueMeasurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), i)).get();
                            addToSnoringIssues(
                                snoringIssues,
                                new SnoringIssueEntry(issueKey, issueMeasurementDateValue),
                                snoringIssuesCountByProjectByDataset.get(dataset.getName()).get(project.getKey()));
                        }
                    });
                }

            }

        }
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
                
        List<SnoringIssueEntry> snoringIssues = snoringIssuesByMeasurementDateByProjectByDataset.get(bean.getDatasetName())
            .get(bean.getIssue().getDetails().getFields().getProject().getKey())
            .get(bean.getMeasurementDateName());
        
        boolean accepted = !isSnoring(bean.getIssue(), snoringIssues);
        return accepted;
    }

}
