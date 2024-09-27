package it.torkin.dataminer.control.dataset.processed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessedDatasetController implements IProcessedDatasetController {
    
    @Autowired private IssueDao issueDao;
    
    @Autowired(required = false)
    private List<IssueFilter> issueFilters = new ArrayList<>();
        
    /**
     * We want to return issues only if they pass the filters.
     */
    private boolean passesFilters(IssueFilterBean bean, Map<String, Integer> filteredByFilter) {
        for (Function<IssueFilterBean, Boolean> filter : issueFilters){
            if (!filter.apply(bean)) {
                filteredByFilter.compute(filter.getClass().getName(), (k, v) -> v == null ? 1 : v + 1);
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public Stream<Issue> getFilteredIssues(ProcessedIssuesBean bean) {

        return issueDao.findAllByDatasetName(bean.getDatasetName())
                                .filter((issue) -> passesFilters(new IssueFilterBean(issue, bean.getDatasetName()), bean.getFilteredByFilter()));  
    }
    

}
