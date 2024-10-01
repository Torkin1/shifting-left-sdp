package it.torkin.dataminer.control.features.measurementdate.impl;

import java.sql.Timestamp;

import it.torkin.dataminer.control.features.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Issue;

public class CommitDate extends MeasurementDate{

    @Override
    protected Timestamp getMeasurementDate(Issue issue) {
        
        return issue.getCommits().stream().findFirst().get().getTimestamp();
    }

}
