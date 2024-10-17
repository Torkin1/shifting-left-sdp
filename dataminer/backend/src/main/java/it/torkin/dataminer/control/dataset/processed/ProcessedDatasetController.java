package it.torkin.dataminer.control.dataset.processed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessedDatasetController implements IProcessedDatasetController {
    
    @Autowired private IssueDao issueDao;
    
    @Autowired(required = false)
    private List<IssueFilter> issueFilters = new ArrayList<>();

    @PostConstruct
    private void init() {
        log.debug("Issue processing filter list: {}", issueFilters);
    }
        
    /**
     * We want to return issues only if they pass the filters.
     */
    private boolean passesFilters(IssueFilterBean issueFilterBean, ProcessedIssuesBean processedIssuesBean) {
        
        for (IssueFilter filter : issueFilters){
            String filterName = filter.getClass().getSimpleName();
            processedIssuesBean.getFilteredByProjectGroupedByFilter().putIfAbsent(filterName, new HashMap<>());
            if (!filter.apply(issueFilterBean)) {
                String project = issueFilterBean.getIssue().getDetails().getFields().getProject().getName();
                if (!issueFilterBean.isFiltered()) {
                    // we update excluded issues count per project
                    processedIssuesBean.getExcludedByProject()
                     .compute(project, (k, v) -> v == null ? 1 : v + 1);
                    issueFilterBean.setFiltered(true);
                }
                // we track issues filtered by this filter for the project
                processedIssuesBean.getFilteredByProjectGroupedByFilter().get(filterName)
                 .compute(project, (k, v) -> v == null ? 1 : v + 1);
            }
        }
        return !issueFilterBean.isFiltered();
    }

    @Override
    @Transactional
    public void getFilteredIssues(ProcessedIssuesBean bean) {
        
        issueFilters.forEach((filter) -> filter.reset());
        bean.setProcessedIssues(issueDao.findAllByDataset(bean.getDatasetName())
            // we want to log the progress while traversing the issues
            .map(new Function<Issue, Issue>() {

                long traversed = 0;
                
                @Override
                public Issue apply(Issue issue) {
                    
                    traversed++;
                    if (traversed % 10000 == 0) {
                        log.info("traversed {} issues of dataset {}", traversed, bean.getDatasetName());
                    }
                    return issue;
                }   
            })
            // we order the issues by the MeasurementDate from least to most recent
            .sorted((issue1, issue2) -> bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue1))
                .compareTo(bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue2))))
            // we filter out issues that do not pass the filters
            .filter((issue) -> passesFilters(new IssueFilterBean(issue, bean.getDatasetName(), true), bean)));  
    }
}
