package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;

import org.springframework.stereotype.Service;

import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{
    
    @Override
    public boolean isBuggy(IssueBean bean){

        return bean.getIssue().getCommits().stream().anyMatch(commit -> {
            
            Timestamp measurementDate = bean.getMeasurementDate();
            if (measurementDate == null){
                // we assume that the measurement date is now,
                // to include all the related commits in the dataset
                // (all data in dataset should be in the past with respect to now)
                measurementDate = TimeTools.now();
            }
                
            return commit.isBuggy()
             && commit.getDataset().getName().equals(bean.getDataset())
             && !commit.getTimestamp().after(measurementDate);
        });

    }

    @Override
    public String getDescription(IssueBean bean) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
    }

}
