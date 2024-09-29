package it.torkin.dataminer.control.dataset.processed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.IssueDao;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import it.torkin.dataminer.entities.dataset.Issue;

@Service
@Slf4j
public class ProcessedDatasetController implements IProcessedDatasetController {
    
    @Autowired private IssueDao issueDao;
    
    @Autowired(required = false)
    private List<IssueFilter> issueFilters = new ArrayList<>();
        
    /**
     * We want to return issues only if they pass the filters.
     */
    private boolean passesFilters(IssueFilterBean bean, Map<String, Map<String, Integer>> filteredByFilterPerProject) {
        for (Function<IssueFilterBean, Boolean> filter : issueFilters){
            if (!filter.apply(bean)) {
                // we update filtered issues count per project for this filter
                String project = bean.getIssue().getDetails().getFields().getProject().getName();
                String filterName = filter.getClass().getSimpleName();
                filteredByFilterPerProject.get(filterName).compute(project, (k, v) -> v == null ? 1 : v + 1);
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void getFilteredIssues(ProcessedIssuesBean bean) {

        issueFilters.forEach((filter) -> bean.getFilteredByFilterPerProjecy().put(filter.getClass().getSimpleName(), new HashMap<>()));
        bean.setProcessedIssues(issueDao.findAllByDatasetName(bean.getDatasetName())
        .map(new Function<Issue, Issue>() {

            long traversed = 0;
            
            @Override
            public Issue apply(Issue issue) {
                
                traversed++;
                if (traversed % 1000 == 0) {
                    log.info("traversed {} issues of dataset {}", traversed, bean.getDatasetName());
                }
                return issue;
            }
            
         })
         .filter((issue) -> passesFilters(new IssueFilterBean(issue, bean.getDatasetName()), bean.getFilteredByFilterPerProjecy())));  
    }
    

}
