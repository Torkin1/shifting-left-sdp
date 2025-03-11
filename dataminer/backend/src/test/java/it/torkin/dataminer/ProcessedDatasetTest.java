package it.torkin.dataminer;

import static org.junit.Assert.assertFalse;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.control.measurementdate.impl.OneSecondBeforeFirstCommitDate;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class ProcessedDatasetTest {

    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IDatasetController datasetController;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private IssueDao issueDao;
    @Test
    // @Transactional
    public void getFilteredIssuesTest() throws UnableToLoadCommitsException, UnableToInitDatasourceException, UnableToCreateRawDatasetException {

        datasetController.createRawDataset();
        
        ProcessedIssuesBean bean = new ProcessedIssuesBean("apachejit", new OneSecondBeforeFirstCommitDate());
        long unfilteredCount = issueDao.countByDataset("apachejit");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            processedDatasetController.initFilters();
            processedDatasetController.getFilteredIssues(bean);
        
            log.info(bean.toString());
            log.info("Processed issues count: " + bean.getProcessedIssues().count());
            log.info("Unfiltered issues count: " + unfilteredCount);
            log.info(bean.toString());
    
        });
    }

    @Test
    @Transactional
    public void testIssuesOrderedByMeasurementDate() throws UnableToCreateRawDatasetException{
        final String datasetName = "leveragingjit";
        ProcessedIssuesBean bean = new ProcessedIssuesBean("leveragingjit", new FirstCommitDate());
        MeasurementDate measurementDate = new FirstCommitDate();

        datasetController.createRawDataset();
        processedDatasetController.getFilteredIssues(bean);

        bean.getProcessedIssues().forEach(new Consumer<Issue>() {

            private Issue lastIssue = null;
            
            @Override
            public void accept(Issue issue) {
                if (lastIssue != null){
                    assertFalse(measurementDate.apply(new MeasurementDateBean(datasetName, lastIssue)).get()
                        .after(measurementDate.apply(new MeasurementDateBean(datasetName, issue)).get()));
                }
                lastIssue = issue;
            }
        });

    }
    
}
