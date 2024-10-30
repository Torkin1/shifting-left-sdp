package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
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
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class MeasurementDateTest {

    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private IssueDao issueDao;

    @Test
    public void testCommitDate(){

        Dataset dataset = new Dataset();
        dataset.setName("dataset_test");
        dataset = datasetDao.save(dataset);

        Issue issue = new Issue();
        issue.setKey("TEST-123");
        issueDao.save(issue);

        /**
         * TODO: store commits with different dates in issue
         * and test if the measurement date is the date of the
         * oldest commit
         */

        Instant end = Instant.now();
        
        for (int i = 0; i < 3; i ++){
            Commit commit = new Commit();
            commit.setDataset(dataset);
            commit.setTimestamp(Timestamp.from(end.minusSeconds(i)));
            commit.getIssues().add(issue);
            issue.getCommits().add(commit);
            commitDao.save(commit);
        }


        issue = issueDao.findByKey("TEST-123");
        assertEquals(end.truncatedTo(ChronoUnit.SECONDS), issue.getCommits().get(issue.getCommits().size() - 1).getTimestamp().toInstant().truncatedTo(ChronoUnit.SECONDS));
        
    }

    @Test
    public void testMeasurementDateName(){

        MeasurementDate firstCommitDate = new FirstCommitDate();

        assertEquals("FirstCommitDate", firstCommitDate.getName());
    }

    @Autowired private IMeasurementDateController measurementDateController;

    @Test
    public void testMeasurementDateSelection(){

        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        assertEquals(1, measurementDates.size());
        assertEquals("OneSecondBeforeFirstCommitDate", measurementDates.get(0).getName());

    }

    @Test
    public void testOneSecondBeforeFirstCommitDate(){
        MeasurementDate firstCommitDate = new FirstCommitDate();
        MeasurementDate oneSecondBeforeFirstCommitDate = new OneSecondBeforeFirstCommitDate();

        Dataset dataset = new Dataset();
        dataset.setName("dataset_test");
        Issue issue = new Issue();
        Commit commit = new Commit();
        commit.setTimestamp(TimeTools.now());
        commit.setDataset(dataset);
        issue.getCommits().add(commit);

        Timestamp firstCommitDateValue = firstCommitDate.apply(new MeasurementDateBean(dataset.getName(), issue));
        Timestamp oneSecondBeforeFirstCommitDateValue = oneSecondBeforeFirstCommitDate.apply(new MeasurementDateBean(dataset.getName(), issue));

        assertTrue(firstCommitDateValue.after(oneSecondBeforeFirstCommitDateValue));
    }
    
}
