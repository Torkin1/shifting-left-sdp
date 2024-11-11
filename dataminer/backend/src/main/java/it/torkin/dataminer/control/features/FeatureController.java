package it.torkin.dataminer.control.features;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.MeasurementDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import jakarta.transaction.Transactional;

@Service
public class FeatureController implements IFeatureController{

    @Autowired private List<FeatureMiner> miners;

    @Autowired private DatasetDao datasetDao;
    @Autowired private IssueDao issueDao;
    @Autowired private MeasurementDao measurementDao;
    
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IMeasurementDateController measurementDateController;

    @Override
    @Transactional
    public void initMiners() throws Exception{
        miners.forEach(miner -> {
            try {
                miner.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void doMeasurements(FeatureMinerBean bean){
        miners.forEach(miner -> miner.accept(bean));
    }

    private void saveMeasurement(FeatureMinerBean bean){
        Measurement measurement = measurementDao.save(bean.getMeasurement());
        bean.getIssue().getMeasurements().removeIf(m -> m.getMeasurementDateName().equals(measurement.getMeasurementDateName()));
        bean.getIssue().getMeasurements().add(measurement);
        issueDao.save(bean.getIssue());
    }
    
    @Override
    @Transactional
    public void mineFeatures(){
        
        List<Dataset> datasets = datasetDao.findAll();
        ProcessedIssuesBean processedIssuesBean;
        Iterator<Issue> issues;
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        for (Dataset dataset : datasets) {
            for (MeasurementDate measurementDate : measurementDates) {
                
                // collect processed issue
                processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                processedDatasetController.getFilteredIssues(processedIssuesBean);
                issues = processedIssuesBean.getProcessedIssues().iterator();

                while (issues.hasNext()) {
                    Issue issue = issues.next();
                    Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));

                    
                    Measurement measurement = issue.getMeasurementByMeasurementDateName(measurementDate.getName());
                    if (measurement == null){
                        measurement = new Measurement();
                        measurement.setMeasurementDate(measurementDateValue);
                        measurement.setMeasurementDateName(measurementDate.getName());
                        measurement.setIssue(issue);
                        measurement.setDataset(dataset);
                    }

                    FeatureMinerBean bean = new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate);
                    doMeasurements(bean);
                    saveMeasurement(bean);
 
                }
            }
        }
        


    }
}
