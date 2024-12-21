package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

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
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IFeatureController;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.features.miners.ActivitiesMiner;
import it.torkin.dataminer.control.features.miners.AssigneeMiner;
import it.torkin.dataminer.control.features.miners.BuggySimilarityMiner;
import it.torkin.dataminer.control.features.miners.CommitsWhileInProgressMiner;
import it.torkin.dataminer.control.features.miners.LatestCommitMiner;
import it.torkin.dataminer.control.features.miners.NLP4REMiner;
import it.torkin.dataminer.control.features.miners.PriorityMiner;
import it.torkin.dataminer.control.features.miners.CodeQualityMiner;
import it.torkin.dataminer.control.features.miners.CodeSizeMiner;
import it.torkin.dataminer.control.features.miners.TemporalLocalityMiner;
import it.torkin.dataminer.control.features.miners.TypeMiner;
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
import it.torkin.dataminer.entities.dataset.Repository;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.issue.IssueChangelog;
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

    @Autowired private BuggySimilarityMiner nlpFeaturesMiner;
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
    @Autowired private AssigneeMiner assigneeANFICMiner;
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
        Issue pastIssue = new Issue();

        Repository repository = new Repository();
        repository.setId("myRepo");

        Project project = new Project();
        project.setKey("myProject");
        project.setJiraId("myProject");

        Developer dev = new Developer();
        dev.setKey("myDev");

        Dataset dataset = new Dataset();
        dataset.setName("jitsdp");
        dataset = datasetDao.save(dataset);

        Timestamp now = TimeTools.now();
        Commit buggyCommit = new Commit();
        buggyCommit.setHash("buggyCommit");
        buggyCommit.setTimestamp(now);
        buggyCommit.setBuggy(true);
        buggyCommit.setDataset(dataset);
        buggyCommit.setRepository(repository);

        Commit cleanCommit = new Commit();
        cleanCommit.setHash("cleanCommit");
        cleanCommit.setTimestamp(now);
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);
        cleanCommit.setRepository(repository);

        Commit pastCommit = new Commit();
        pastCommit.setHash("pastCommit");
        pastCommit.setTimestamp(Timestamp.from(now.toInstant().minus(1, ChronoUnit.SECONDS)));
        pastCommit.setBuggy(false);
        pastCommit.setDataset(dataset);
        pastCommit.setRepository(repository);

        buggyPastIssue.setKey("buggyPastIssue");
        buggyPastIssue.setDetails(new IssueDetails());
        buggyPastIssue.getDetails().setFields(new IssueFields());
        buggyPastIssue.getDetails().getFields().setAssignee(dev);
        buggyPastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        buggyPastIssue.getDetails().getFields().setProject(project);
        buggyPastIssue.getDetails().setChangelog(new IssueChangelog());

        buggyCommit.getIssues().add(buggyPastIssue);
        buggyPastIssue.getCommits().add(buggyCommit);

        cleanCommit.getIssues().add(cleanPastIssue);
        cleanCommit.getIssues().add(issue);
        cleanPastIssue.getCommits().add(cleanCommit);
        issue.getCommits().add(cleanCommit);
        pastIssue.getCommits().add(pastCommit);
        pastCommit.getIssues().add(pastIssue);

        cleanPastIssue.setKey("cleanPastIssue");
        cleanPastIssue.setDetails(new IssueDetails());
        cleanPastIssue.getDetails().setFields(new IssueFields());
        cleanPastIssue.getDetails().getFields().setAssignee(dev);
        cleanPastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        cleanPastIssue.getDetails().getFields().setProject(project);
        cleanPastIssue.getDetails().setChangelog(new IssueChangelog());

        issue.setKey("issue");
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());
        issue.getDetails().getFields().setAssignee(dev);
        issue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        issue.getDetails().getFields().setProject(project);
        issue.getDetails().setChangelog(new IssueChangelog());

        pastIssue.setKey("pastIssue");
        pastIssue.setDetails(new IssueDetails());
        pastIssue.getDetails().setFields(new IssueFields());
        pastIssue.getDetails().getFields().setCreated(Timestamp.from(TimeTools.now().toInstant().minus(2, ChronoUnit.DAYS)));
        pastIssue.getDetails().getFields().setProject(project);
        pastIssue.getDetails().setChangelog(new IssueChangelog());

        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);
        commitDao.save(pastCommit);

        buggyPastIssue = issueDao.save(buggyPastIssue);
        cleanPastIssue = issueDao.save(cleanPastIssue);
        issue = issueDao.save(issue);
        pastIssue = issueDao.save(pastIssue);

        Measurement measurement = new Measurement();
        MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
        measurement.setMeasurementDate(measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue)));
        measurement.setMeasurementDateName(measurementDate.getName());
        measurement.setIssue(issue);
        issue.getMeasurements().add(measurement);

        assigneeANFICMiner.accept(new FeatureMinerBean(
            dataset.getName(), issue, measurement, measurementDate, 0));

        assertEquals(0.5, measurement.getFeatureByName(IssueFeature.ASSIGNEE.getFullName() + ": ANFIC").getValue());
        assertEquals(0.6666666666666666, measurement.getFeatureByName(IssueFeature.ASSIGNEE.getFullName() + ": Familiarity").getValue());
    }

    @Autowired private TemporalLocalityMiner temporalLocalityMiner;

    @Transactional
    @Test
    public void testTemporalLocality() throws Exception{

        testMiner(temporalLocalityMiner);
    }

    @Autowired private CodeQualityMiner projectCodeQualityMiner;
    
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
        
        projectCodeQualityMiner.accept(new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate, 0));
        log.debug("measurement: {}", measurement);

    }

    @Autowired private CodeSizeMiner projectCodeSizeMiner;

    @Transactional
    @Test
    public void testCodeSize() throws Exception{
        testMiner(projectCodeSizeMiner);
    }

    @Autowired private PriorityMiner priorityMiner;

    @Transactional
    @Test
    public void testPriority() throws Exception{
        datasetController.createRawDataset();
        priorityMiner.init();

        Dataset dataset = datasetDao.findAll().get(0);
        
        Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName());

        issues.forEach(issue -> {
            MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
            Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));
            Measurement measurement = new Measurement();
            measurement.setMeasurementDate(measurementDateValue);
            measurement.setMeasurementDateName(measurementDate.getName());
            measurement.setIssue(issue);
            
            priorityMiner.accept(new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate, 0));
            log.debug("measurement: {}", measurement);
        });
        
    }

    @Autowired private TypeMiner typeMiner;

    private void testMiner(FeatureMiner miner) throws Exception{
        datasetController.createRawDataset();
        miner.init();

        Dataset dataset = datasetDao.findAll().get(0);
        
        Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName());

        issues.forEach(issue -> {
            MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
            Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));
            Measurement measurement = new Measurement();
            measurement.setMeasurementDate(measurementDateValue);
            measurement.setMeasurementDateName(measurementDate.getName());
            measurement.setIssue(issue);
            
            miner.accept(new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate, 0));
            log.debug("measurement: {}", measurement);
        });
    }
    
    @Transactional
    @Test
    public void testType() throws Exception{
        datasetController.createRawDataset();
        typeMiner.init();

        Dataset dataset = datasetDao.findAll().get(0);
        
        Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName());

        issues.forEach(issue -> {
            MeasurementDate measurementDate = new OneSecondBeforeFirstCommitDate();
            Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));
            Measurement measurement = new Measurement();
            measurement.setMeasurementDate(measurementDateValue);
            measurement.setMeasurementDateName(measurementDate.getName());
            measurement.setIssue(issue);
            
            typeMiner.accept(new FeatureMinerBean(dataset.getName(), issue, measurement, measurementDate, 0));
            log.debug("measurement: {}", measurement);
        });

    }

    @Autowired private ActivitiesMiner activitiesMiner;

    @Transactional
    @Test
    public void testActivities() throws Exception{
        testMiner(activitiesMiner);
    }

    @Autowired private LatestCommitMiner latestProjectCommitMiner;

    @Transactional
    @Test
    public void testLatestProjectCommit() throws Exception{
        testMiner(latestProjectCommitMiner);
    }
    
    @Autowired private CommitsWhileInProgressMiner commitsWhileInProgressMiner;

    @Transactional
    @Test
    public void testCommitsWhileInProgress() throws Exception{
        testMiner(commitsWhileInProgressMiner);
    }

    @Autowired private AssigneeMiner assigneeMiner;

    @Transactional
    @Test
    public void testAssignee() throws Exception{
        testMiner(assigneeMiner);
    }

    @Autowired private NLP4REMiner nlp4reMiner;
    
    @Transactional
    @Test
    public void testNLP4REMiner() throws Exception{
        testMiner(nlp4reMiner);
    }
}
