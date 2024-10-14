package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

    @Override
    public boolean isBuggy(IssueBean bean){
        
        if (bean.getDataset() == null){
            throw new NullPointerException(String.format("Cannot get bugginess of Issue %s without setting from which dataset we can search for commits (datasets can disagree about the bugginess of an issue)", bean.getIssue().getKey()));
        }
        
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
        
        return new IssueFieldGetter<String>(
            fields -> fields.getDescription(),
            Function.identity()
        ).apply(new IssueFieldGetterBean(bean, IssueField.DESCRIPTION));        
    }

}
