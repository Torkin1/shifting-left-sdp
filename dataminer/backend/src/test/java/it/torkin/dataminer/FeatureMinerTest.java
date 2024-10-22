package it.torkin.dataminer;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.NLPFeaturesConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.features.IFeatureController;
import it.torkin.dataminer.control.features.miners.NLPFeaturesMiner;
import it.torkin.dataminer.control.features.miners.UnableToInitNLPFeaturesMinerException;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class FeatureMinerTest {

    @Autowired private IDatasetController datasetController;
    @Autowired private NLPFeaturesMiner nlpFeaturesMiner;
    @Autowired private NLPFeaturesConfig config;

    @Autowired private IFeatureController featureController;
    @Autowired private IProcessedDatasetController processedDatasetController;

    @Autowired IssueDao issueDao;

    @Test
    public void testNlpFeaturesMiner() throws UnableToCreateRawDatasetException, UnableToInitNLPFeaturesMinerException {


        datasetController.createRawDataset();
        nlpFeaturesMiner.init();

        assertTrue(new File(config.getNlpIssueBeans()).exists());

    }

    @Test
    @Transactional
    public void testFeatureController() throws Exception{

        datasetController.createRawDataset();
        featureController.initMiners();
        featureController.mineFeatures();

        ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean("apachejit", new FirstCommitDate());
        processedDatasetController.getFilteredIssues(processedIssuesBean);
        Iterator<Issue> issues = processedIssuesBean.getProcessedIssues().iterator();
        
        assertTrue(issues.hasNext());

        while (issues.hasNext()) {
            Issue issue = issues.next();
            issue.getMeasurements().forEach(m -> {
                log.debug("Measurement: {}", m);
                assertNotEquals(0, m.getFeatures().size());
            });
        }        
    }

}
