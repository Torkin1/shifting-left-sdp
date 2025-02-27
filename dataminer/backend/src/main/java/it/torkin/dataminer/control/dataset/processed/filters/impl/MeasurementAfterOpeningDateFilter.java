package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateController;
import it.torkin.dataminer.entities.dataset.Issue;

@Component
public class MeasurementAfterOpeningDateFilter extends IssueFilter{

    @Autowired private MeasurementDateController measurementDateController;
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {

        Issue issue = bean.getIssue();
        Timestamp openingDate = issue.getDetails().getFields().getCreated();
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        for (MeasurementDate measurementDate : measurementDates) {
            Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(bean.getDatasetName(), issue)).get();
            if (measurementDateValue.before(openingDate)){
                return false;
            }
        }
        return true;
    }
}
