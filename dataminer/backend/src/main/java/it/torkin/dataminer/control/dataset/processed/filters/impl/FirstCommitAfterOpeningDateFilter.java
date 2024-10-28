package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;

@Component
public class FirstCommitAfterOpeningDateFilter extends IssueFilter{

    @Autowired
    private IIssueController issueController;
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {

        Issue issue = bean.getIssue();
        Timestamp openingDate = issue.getDetails().getFields().getCreated();

        Commit firstCommit = issueController.getFirstCommit(new IssueCommitBean(issue, bean.getDatasetName()));
        
        return firstCommit != null && !firstCommit.getTimestamp().before(openingDate);
        
    }

}
