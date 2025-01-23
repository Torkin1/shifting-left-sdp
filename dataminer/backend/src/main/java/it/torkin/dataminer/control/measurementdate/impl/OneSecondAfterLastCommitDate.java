package it.torkin.dataminer.control.measurementdate.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.entities.dataset.Commit;

@Component
public class OneSecondAfterLastCommitDate implements MeasurementDate{

    @Override
    public Optional<Timestamp> apply(MeasurementDateBean bean) {
        List<Commit> commits = bean.getIssue().getCommits();
        Commit latest = commits.get(0);
        for (Commit commit : commits){
            if (commit.getDataset().getName().equals(bean.getDatasetName()) 
             && commit.getTimestamp().after(latest.getTimestamp())){
                latest = commit;
            }
        }
        return Optional.of(Timestamp.from(latest.getTimestamp().toInstant().plus(1, ChronoUnit.SECONDS)));
    }

  
}