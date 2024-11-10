package it.torkin.dataminer.control.dataset.processed;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.filters.IssueFilterConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.IssueDao;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessedDatasetController implements IProcessedDatasetController {
    
    @Autowired private IssueDao issueDao;
    @Autowired private IssueFilterConfig filterConfig;
    
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
                String project = issueFilterBean.getIssue().getDetails().getFields().getProject().getKey();
                // we track issues filtered by this filter for the project
                if (filterConfig.getApplyAnyway() || (!filterConfig.getApplyAnyway() && !issueFilterBean.isFiltered())) {
                    processedIssuesBean.getFilteredByProjectGroupedByFilter().get(filterName)
                        .compute(project, (k, v) -> v == null ? 1 : v + 1);
                }
                if (!issueFilterBean.isFiltered()) {
                    // we update excluded issues count per project
                    processedIssuesBean.getExcludedByProject()
                        .compute(project, (k, v) -> v == null ? 1 : v + 1);
                    issueFilterBean.setFiltered(true);
                }
                
            }
        }
        return !issueFilterBean.isFiltered();
    }

    @Override
    @Transactional
    public void getFilteredIssues(ProcessedIssuesBean bean) {
        
        // bean object is shared among filter invocations on all issues
        // for this query
        IssueFilterBean issueFilterBean = new IssueFilterBean();
        
        bean.setProcessedIssues(issueDao.findAllByDataset(bean.getDatasetName())
            // we order the issues by the MeasurementDate from least to most recent
            .sorted((issue1, issue2) -> bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue1))
                .compareTo(bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue2))))
            // we filter out issues that do not pass the filters
            .filter((issue) -> {
                Timestamp measurementDate = bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue));
                issueFilterBean.setIssue(issue);
                issueFilterBean.setDatasetName(bean.getDatasetName());
                issueFilterBean.setMeasurementDate(measurementDate);
                issueFilterBean.setApplyAnyway(filterConfig.getApplyAnyway());
                issueFilterBean.setFiltered(false);
                issueFilterBean.setMeasurementDateName(bean.getMeasurementDate().getName());
                return passesFilters(issueFilterBean, bean);
            }));  
    }
}
