package it.torkin.dataminer.control.dataset.processed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.ProcessedIssuesConfig;
import it.torkin.dataminer.config.filters.IssueFilterConfig;
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
    @Autowired private IssueFilterConfig filterConfig;

    @Autowired private ProcessedIssuesConfig processedIssuesConfig;

    private boolean filtersInitialized = false;

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

    private File initCache(ProcessedIssuesBean bean){

        Stream<Issue> issues = null;
        File cacheFile = processedIssuesConfig.getCacheFile(bean.getDatasetName(), bean.getMeasurementDate().getName());

        if (!cacheFile.exists()){
            IssueFilterBean issueFilterBean = new IssueFilterBean();
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile));){
                issues = issueDao.findAllByDataset(bean.getDatasetName())
                // we filter out issues that do not pass the filters
                .filter((issue) -> {
                    Optional<Timestamp> measurementDateOptional = bean.getMeasurementDate().apply(new MeasurementDateBean(bean.getDatasetName(), issue));
                    // filter away issues that do not have the measurement date available
                    Timestamp measurementDate = measurementDateOptional.get();
                    issueFilterBean.setIssue(issue);
                    issueFilterBean.setDatasetName(bean.getDatasetName());
                    issueFilterBean.setMeasurementDate(measurementDate);
                    issueFilterBean.setApplyAnyway(filterConfig.getApplyAnyway());
                    issueFilterBean.setFiltered(false);
                    issueFilterBean.setMeasurementDateName(bean.getMeasurementDate().getName());
                    return passesFilters(issueFilterBean, bean);
                });
                issues.forEach(issue -> {
                    try {
                        writer.write(issue.getKey());
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException("unable to write to cache file at " + cacheFile.getAbsolutePath(), e);
                    }
                });
    
            } catch (IOException e) {
                throw new RuntimeException("unable to load cached filtered issues file at " + cacheFile.getAbsolutePath(), e);
            } finally {
                if (issues != null) issues.close();
            }
        }
        
        return cacheFile;
    }
    
    @Override
    @Transactional
    public void getFilteredIssues(ProcessedIssuesBean bean) {
        
        Stream<Issue> issues;
        
        if (!filtersInitialized) {
            initFilters();
            filtersInitialized = true;
        }

        File cacheFile = initCache(bean);
        log.info("loading processed issues from cache: {}", cacheFile);
        try {
            issues = Files.lines(cacheFile.toPath())
                .map(issueDao::findByKey);
        } catch (IOException e) {
            throw new RuntimeException("unable to load cached filtered issues file at " + cacheFile.getAbsolutePath(), e);
        }        
        
        bean.setProcessedIssues(issues);
    }

    @Override
    public void initFilters() {
        issueFilters.forEach(IssueFilter::init);
        log.info("issue filters initialized");
    }
}
