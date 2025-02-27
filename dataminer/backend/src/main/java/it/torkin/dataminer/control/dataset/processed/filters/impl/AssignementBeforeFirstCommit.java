package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.control.measurementdate.impl.OneSecondBeforeFirstAssignmentDate;

/**
 * Filters away issues that have their first assignment not before the first commit.
 */
@Component
public class AssignementBeforeFirstCommit extends IssueFilter{

    @Autowired private FirstCommitDate firstCommitDate;
    @Autowired private OneSecondBeforeFirstAssignmentDate oneSecondBeforeFirstAssignmentDate;
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        
        MeasurementDateBean measurementDateBean = new MeasurementDateBean(bean.getDatasetName(), bean.getIssue());
        Timestamp firstCommitDateValue = firstCommitDate.apply(measurementDateBean).get();
        Timestamp firstAssignmentDateValue = oneSecondBeforeFirstAssignmentDate.apply(measurementDateBean).get();

        return firstAssignmentDateValue.before(firstCommitDateValue);
    }
    
}
