package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.entities.dataset.features.Feature;
import it.torkin.dataminer.entities.dataset.features.StringFeature;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class MeasurementTest {

    @Autowired private IssueDao issueDao;
    
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
}
