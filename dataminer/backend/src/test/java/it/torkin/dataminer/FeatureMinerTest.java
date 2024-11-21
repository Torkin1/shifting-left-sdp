package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.features.NLPFeaturesConfig;
import it.torkin.dataminer.control.dataset.DatasetController;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IFeatureController;
import it.torkin.dataminer.control.features.miners.AssigneeANFICMiner;
import it.torkin.dataminer.control.features.miners.NLPFeaturesMiner;
import it.torkin.dataminer.control.features.miners.ProjectCodeQualityMiner;
import it.torkin.dataminer.control.features.miners.TemporalLocalityMiner;
import it.torkin.dataminer.control.features.miners.UnableToInitNLPFeaturesMinerException;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.control.measurementdate.impl.OneSecondBeforeFirstCommitDate;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.time.TimeTools;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class FeatureMinerTest {

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

    @Autowired private CommitDao commitDao;
    @Autowired private AssigneeANFICMiner assigneeANFICMiner;
    @Autowired private DatasetController datasetController;
    @Autowired private DatasetDao datasetDao;

    @Test
    @Transactional
    public void testMineFeatures() throws Exception{

        datasetController.createRawDataset();
        
        featureController.initMiners();
        featureController.mineFeatures();

        ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean("jitsdp", new OneSecondBeforeFirstCommitDate());
        processedDatasetController.getFilteredIssues(processedIssuesBean);
        
        Iterator<Issue> issues = processedIssuesBean.getProcessedIssues().iterator();
        while(issues.hasNext()){
            Issue currentIssue = issues.next();

            log.debug("measurement for issue {}: {}", currentIssue.getKey(), currentIssue.getMeasurements().iterator().next());
        }

    }

    /**
     * NOTE: deactivate issue filters before firing this method
     */
    @Transactional
    @Test
    public void testMineANFIC(){

        Issue buggyPastIssue = new Issue();
        Issue cleanPastIssue = new Issue();
        Issue issue = new Issue();

        Project project = new Project();
        project.setKey("myProject");
        project.setJiraId("myProject");

        Developer dev = new Developer();
        dev.setKey("myDev");

        Dataset dataset = new Dataset();
        dataset.setName("jitsdp");
        dataset = datasetDao.save(dataset);

        Commit buggyCommit = new Commit();
        buggyCommit.setHash("buggyCommit");
        buggyCommit.setTimestamp(TimeTools.now());
        buggyCommit.setBuggy(true);
        buggyCommit.setDataset(dataset);

        Commit cleanCommit = new Commit();
        cleanCommit.setHash("cleanCommit");
        cleanCommit.setTimestamp(TimeTools.now());
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);

        buggyPastIssue.setKey("issue1");
        buggyPastIssue.setDetails(new IssueDetails());
        buggyPastIssue.getDetails().setFields(new IssueFields());
        buggyPastIssue.getDetails().getFields().setAssignee(dev);
        buggyPastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        buggyPastIssue.getDetails().getFields().setProject(project);

        buggyCommit.getIssues().add(buggyPastIssue);
        buggyPastIssue.getCommits().add(buggyCommit);

        cleanCommit.getIssues().add(cleanPastIssue);
        cleanCommit.getIssues().add(issue);
        cleanPastIssue.getCommits().add(cleanCommit);
        issue.getCommits().add(cleanCommit);

        cleanPastIssue.setKey("issue2");
        cleanPastIssue.setDetails(new IssueDetails());
        cleanPastIssue.getDetails().setFields(new IssueFields());
        cleanPastIssue.getDetails().getFields().setAssignee(dev);
        cleanPastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        cleanPastIssue.getDetails().getFields().setProject(project);

        issue.setKey("issue3");
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());
        issue.getDetails().getFields().setAssignee(dev);
        issue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        issue.getDetails().getFields().setProject(project);

        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);

        buggyPastIssue = issueDao.save(buggyPastIssue);
        cleanPastIssue = issueDao.save(cleanPastIssue);
        issue = issueDao.save(issue);

        Measurement measurement = new Measurement();
        MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
        measurement.setMeasurementDate(measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue)));
        measurement.setMeasurementDateName(measurementDate.getName());
        measurement.setIssue(issue);
        issue.getMeasurements().add(measurement);

        assigneeANFICMiner.accept(new FeatureMinerBean(
            dataset.getName(), issue, measurement, measurementDate));

        assertEquals(0.3333333333333333, measurement.getFeatureByName(IssueFeature.ANFIC.getName()).getValue());
    }

    @Autowired private TemporalLocalityMiner temporalLocalityMiner;

    /**
     * NOTE: deactivate issue filters before firing this test
     */
    @Transactional
    @Test
    public void testTemporalLocality() throws Exception{
        Issue buggyPastIssue = new Issue();
        Issue buggyPastIssue2 = new Issue();
        Issue issue = new Issue();

        Project project = new Project();
        project.setKey("myProject");
        project.setJiraId("myProject");

        Developer dev = new Developer();
        dev.setKey("myDev");

        Dataset dataset = new Dataset();
        dataset.setName("jitsdp");
        dataset = datasetDao.save(dataset);

        Commit buggyCommit = new Commit();
        buggyCommit.setHash("buggyCommit");
        buggyCommit.setTimestamp(TimeTools.now());
        buggyCommit.setBuggy(true);
        buggyCommit.setDataset(dataset);

        Commit cleanCommit = new Commit();
        cleanCommit.setHash("cleanCommit");
        cleanCommit.setTimestamp(TimeTools.now());
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);

        buggyPastIssue.setKey("issue1");
        buggyPastIssue.setDetails(new IssueDetails());
        buggyPastIssue.getDetails().setFields(new IssueFields());
        buggyPastIssue.getDetails().getFields().setAssignee(dev);
        buggyPastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        buggyPastIssue.getDetails().getFields().setProject(project);

        buggyCommit.getIssues().add(buggyPastIssue);
        buggyPastIssue.getCommits().add(buggyCommit);

        buggyCommit.getIssues().add(buggyPastIssue2);
        cleanCommit.getIssues().add(issue);
        buggyPastIssue2.getCommits().add(buggyCommit);
        issue.getCommits().add(cleanCommit);

        buggyPastIssue2.setKey("issue2");
        buggyPastIssue2.setDetails(new IssueDetails());
        buggyPastIssue2.getDetails().setFields(new IssueFields());
        buggyPastIssue2.getDetails().getFields().setAssignee(dev);
        buggyPastIssue2.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        buggyPastIssue2.getDetails().getFields().setProject(project);

        issue.setKey("issue3");
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());
        issue.getDetails().getFields().setAssignee(dev);
        issue.getDetails().getFields().setCreated(TimeTools.now());
        issue.getDetails().getFields().setProject(project);

        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);

        buggyPastIssue = issueDao.save(buggyPastIssue);
        buggyPastIssue2 = issueDao.save(buggyPastIssue2);
        issue = issueDao.save(issue);

        Measurement measurement = new Measurement();
        MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
        measurement.setMeasurementDate(measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue)));
        measurement.setMeasurementDateName(measurementDate.getName());
        measurement.setIssue(issue);
        issue.getMeasurements().add(measurement);

        temporalLocalityMiner.init();
        temporalLocalityMiner.accept(new FeatureMinerBean(
            dataset.getName(), issue, measurement, measurementDate));
        
        assertEquals(100.0, measurement.getFeatureByName(IssueFeature.TEMPORAL_LOCALITY.getName()).getValue());
    }

    @Autowired private ProjectCodeQualityMiner projectCodeQualityMiner;
    
    @Transactional
    @Test
    public void testCodeSmells() throws Exception{
        datasetController.createRawDataset();
        projectCodeQualityMiner.init();

        Dataset dataset = datasetDao.findAll().get(0);
        Issue issue = issueDao.findAllByDataset(dataset.getName()).findFirst().get();

        MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
        Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));
        Measurement measurement = new Measurement();
        measurement.setMeasurementDate(measurementDateValue);
        measurement.setMeasurementDateName(measurementDate.getName());
        measurement.setIssue(issue);
        
        projectCodeQualityMiner.accept(new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate));

    }
}
