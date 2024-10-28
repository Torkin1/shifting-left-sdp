package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.entities.dataset.Issue;

@Component
public class MeasurementAfterOpeningDateFilter extends IssueFilter{

    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {

        Issue issue = bean.getIssue();
        Timestamp openingDate = issue.getDetails().getFields().getCreated();
        Timestamp measurementDate = bean.getMeasurementDate();

        return !measurementDate.before(openingDate);

    }

}
