package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.util.List;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Commit;

public class FirstCommitDate implements MeasurementDate{

    @Override
    public Timestamp apply(MeasurementDateBean bean) {
        
        List<Commit> commits = bean.getIssue().getCommits();
        Commit earliest = commits.get(0);
        for (Commit commit : commits){
            if (commit.getDataset().getName().equals(bean.getDatasetName()) 
             && commit.getTimestamp().before(earliest.getTimestamp())){
                earliest = commit;
            }
        }
        return earliest.getTimestamp();
    }

}
