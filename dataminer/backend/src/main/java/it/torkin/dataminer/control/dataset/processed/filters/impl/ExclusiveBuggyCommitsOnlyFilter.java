package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.transaction.Transactional;

/**
 * #30 If a commit is in common among several tickets and it is buggy,
 * all related tickets must be discarded.
 */
@Component
public class ExclusiveBuggyCommitsOnlyFilter extends IssueFilter{

    @Override
    @Transactional
    public Boolean applyFilter(IssueFilterBean bean) {
        
        int nrSharedBuggyCommit = 0;
        int nrBuggyCommit = 0;
        List<Issue> issuesClosedBySameCommit;

        for (Commit commit : bean.getIssue().getCommits()){
            if (commit.isBuggy() && commit.getDataset().getName().equals(bean.getDatasetName())){
                nrBuggyCommit++;
                issuesClosedBySameCommit = commit.getIssues();
                if (issuesClosedBySameCommit.size() > 1){
                    nrSharedBuggyCommit++;
                }       
            }
        }
        /**
         * We can accept issue only if it is clean OR
         * if it has at least one buggy commit that is not shared
         */
        return nrBuggyCommit == 0 || (nrBuggyCommit - nrSharedBuggyCommit > 0);

    }
    
}
