package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.MeasurementDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.dataset.features.StringFeature;
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
public class MeasurementTest {

    @Autowired private IssueDao issueDao;
    @Autowired private MeasurementDao measurementDao;
    @Autowired private DatasetDao datasetDao;
    
    @Test
    public void testMeasurement() {

        Issue issue = new Issue();
        issue.setKey("myIssue");

        Timestamp expected = TimeTools.now();
        Timestamp actual;

        Measurement measurement = new Measurement();
        Feature<String> feature = new StringFeature("myFeature", "myValue");
        measurement.getFeatures().add(feature);
        measurement.setMeasurementDate(expected);
        measurement.setMeasurementDateName("now");

        issue.getMeasurements().add(measurement);
        measurement.setIssue(issue);
        issue = issueDao.save(issue);
        actual = issue.getMeasurementByMeasurementDateName("now").getMeasurementDate();

        assertEquals(expected, actual);
        log.debug("issue: {}", issue);

    }

    @Test
    @Transactional
    public void testFindAllMeasurementByProjectAndDatasetAndMeasurementDateName() {

        Issue issue = new Issue();
        issue.setKey("myIssue");
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());

        Dataset dataset = new Dataset();
        dataset.setName("myDataset");
        dataset = datasetDao.save(dataset);

        Project project = new Project();
        project.setKey("myProject");

        Commit commit = new Commit();
        commit.setDataset(dataset);

        issue.getDetails().getFields().setProject(project);
        issue.getCommits().add(commit);
        commit.getIssues().add(issue);

        Measurement measurement1 = new Measurement();
        measurement1.setMeasurementDateName("now");
        measurement1.setDataset(dataset);
        measurement1.setIssue(issue);
        measurement1.setMeasurementDate(TimeTools.now());
        issue.getMeasurements().add(measurement1);

        Measurement measurement2 = new Measurement();
        measurement2.setMeasurementDateName("notNow");
        measurement2.setDataset(dataset);
        measurement2.setIssue(issue);
        measurement2.setMeasurementDate(TimeTools.now());
        issue.getMeasurements().add(measurement2);
        
        issue = issueDao.save(issue);

        List<Measurement> measurements = measurementDao
            .findAllByProjectAndDatasetAndMeasurementDateName(project.getKey(), dataset.getName(), "now")
            .collect(Collectors.toList());

        assertEquals(1, measurements.size());
        assertEquals(measurement1.getMeasurementDateName(), measurements.get(0).getMeasurementDateName());
        assertEquals(measurement1.getIssue().getDetails().getFields().getProject().getKey(), measurements.get(0).getIssue().getDetails().getFields().getProject().getKey());
        assertEquals(measurement1.getDataset().getName(), measurements.get(0).getDataset().getName());

    }

}
