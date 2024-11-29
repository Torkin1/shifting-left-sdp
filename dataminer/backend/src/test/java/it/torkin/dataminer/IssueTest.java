package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import it.torkin.dataminer.control.issue.HasBeenAssignedBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.issue.IssueTemporalSpanBean;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.impl.OpeningDate;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.RepositoryDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Repository;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;
import it.torkin.dataminer.toolbox.time.TimeTools;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class IssueTest {

    @Autowired private IssueDao issueDao;
    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private IIssueController issueController;
    @Autowired private RepositoryDao repositoryDao;

    @Test
    @Transactional
    public void testIsBuggy(){
        
        Repository repository = new Repository();
        repository.setId("repository/test");
        repository = repositoryDao.save(repository);
        
        Dataset dataset = new Dataset();
        dataset.setName("dataset_test");
        dataset = datasetDao.save(dataset);

        Dataset wrongDataset = new Dataset();
        wrongDataset.setName("dataset_test2");
        wrongDataset = datasetDao.save(wrongDataset);
        
        Issue issue1 = new Issue();
        issue1.setKey("ISSUE-1");
        Issue issue2 = new Issue();
        issue2.setKey("ISSUE-2");
        Issue issue3 = new Issue();
        issue3.setKey("ISSUE-3");

        Commit buggyCommit = new Commit();
        buggyCommit.setBuggy(true);
        buggyCommit.setDataset(dataset);
        buggyCommit.setHash("buggyCommit");
        buggyCommit.setTimestamp(TimeTools.now());
        buggyCommit.setRepository(repository);

        Commit cleanCommit = new Commit();
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);
        cleanCommit.setHash("cleanCommit");
        cleanCommit.setTimestamp(TimeTools.now());
        cleanCommit.setRepository(repository);

        Commit buggyCommitWrongDataset = new Commit();
        buggyCommitWrongDataset.setBuggy(true);
        buggyCommitWrongDataset.setDataset(wrongDataset);
        buggyCommitWrongDataset.setHash("buggyCommitWrongDataset");
        buggyCommitWrongDataset.setTimestamp(TimeTools.now());
        buggyCommitWrongDataset.setRepository(repository);

        issue1.getCommits().add(buggyCommit);
        buggyCommit.getIssues().add(issue1);

        issue2.getCommits().add(cleanCommit);
        cleanCommit.getIssues().add(issue2);

        issue3.getCommits().add(buggyCommitWrongDataset);
        buggyCommitWrongDataset.getIssues().add(issue3);

        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);
        commitDao.save(buggyCommitWrongDataset);
        
        issueDao.save(issue1);
        issueDao.save(issue2);
        issueDao.save(issue3);

        issue1 = issueDao.findByKey("ISSUE-1");
        issue2 = issueDao.findByKey("ISSUE-2");
        issue3 = issueDao.findByKey("ISSUE-3");

        /**
         * We expect:
         * - issue1 to be buggy
         * - issue2 to be clean
         * - issue3 to be clean according to the right dataset
         * - issue3 to be buggy according to the wrong dataset
         */
        assertTrue(issueController.isBuggy(new IssueCommitBean(issue1, dataset.getName())));
        assertFalse(issueController.isBuggy(new IssueCommitBean(issue2, "dataset_test")));
        assertFalse(issueController.isBuggy(new IssueCommitBean(issue3, "dataset_test")));
        assertTrue(issueController.isBuggy(new IssueCommitBean(issue3, "dataset_test2")));
        

    }

    private static final String ISSUE_EXAMPLES_DIR = "./src/test/resources/issue_examples/";
    @Test
    @Transactional
    public void testGetDescription() throws JsonIOException, JsonSyntaxException, FileNotFoundException{
        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + "AVRO-1582.json"); 
        
        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);
        /**
         * test cases:
         *  - measurement date as now --> equals to the issue's description
         *  - measurement date as 2014-09-08T19:49:45.292+0000 --> equals to toString history item field
         * `Currently serializing a nullable field of type union like:\n"type" : ["null","some type"]\n\nwhen serialized as JSON results in:  \n\n"field":{"some type":"value"}\n\nwhen it could be:\n\n"field":"value"\n\nAlso fields that equal the the default value can be omitted from the serialized data. This is possible because the reader will have the writer's schema and can infer the field values. This reduces the size of the json messages.\n\n\n\n`
         *  - measurement date as 2014-09-08T19:49:45.292+0000 minus 1 second --> equals to fromString history item field
         * 'Currently serializing a nullable field of type union like:\n"type" : ["null","some type"]\n\nwhen serialized as JSON results in:  \n\n"field":{"some type":"value"}\n\nwhen it could be:\n\n"field":"value"\n\n\n'
         *  - measurement date as opening date minus 1 second --> equals to null
         */

        Timestamp now = TimeTools.now();
        Timestamp historyItemTimestamp = Timestamp.from(Instant.parse("2014-09-08T19:49:45.292Z"));
        Timestamp historyItemTimestampMinusOneSecond = Timestamp.from(Instant.parse("2014-09-08T19:49:45.292Z").minus(1, ChronoUnit.SECONDS));
        Timestamp openingDateMinusOneSecond = Timestamp.from(issue.getDetails().getFields().getCreated().toInstant().minus(1, ChronoUnit.SECONDS));

        assertEquals(issue.getDetails().getFields().getDescription(), issueController.getDescription(new IssueBean(issue, now)));
        assertEquals("Currently serializing a nullable field (union) \n" + //
                        "\"type\" : [\"null\",\"some type\"]\n" + //
                        "\n" + //
                        "when serialized as JSON results in:  \n" + //
                        "\n" + //
                        "\"field\":{\"some type\":2}\n" + //
                        "\n" + //
                        "when it could be:\n" + //
                        "\n" + //
                        "\"field\":\"value\"\n" + //
                        "\n" + //
                        "", issueController.getDescription(new IssueBean(issue, historyItemTimestamp)));
        assertEquals("Currently serializing a nullable JSON field (union)\n" + //
                        "\"type\" : [\"null\",\"some type\"]\n" + //
                        "\n" + //
                        "when serialized result in:  \n" + //
                        "\n" + //
                        "\"field\":{\"some type\":2}\n" + //
                        "\n" + //
                        "when it could be:\n" + //
                        "\n" + //
                        "\"field\":\"value\"\n" + //
                        "\n" + //
                        "", issueController.getDescription(new IssueBean(issue, historyItemTimestampMinusOneSecond)));
        assertNull(issueController.getDescription(new IssueBean(issue, openingDateMinusOneSecond)));
    }

    @Test
    @Transactional
    public void testGetAssignee() throws JsonIOException, JsonSyntaxException, FileNotFoundException{
        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + "ZOOKEEPER-107.json"); 
        
        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);
        // issue.setKey("ZOOKEEPER-107");
        // issue = issueDao.save(issue);

        Timestamp historyTimestamp = Timestamp.from(Instant.parse("2009-06-15T22:55:46.519Z"));
        Timestamp now = TimeTools.now();

        assertEquals("phunt", issueController.getAssigneeKey(new IssueBean(issue, historyTimestamp)));
        assertEquals("shralex", issueController.getAssigneeKey(new IssueBean(issue, now)));
    }

    @Test
    @Transactional
    public void testHasAssigned() throws JsonIOException, JsonSyntaxException, FileNotFoundException{

        /**
         * Test the following cases:
         * - current assignee is shralex
         * - assignee at 2009-06-15T22:55:46.519Z is phunt
         * - assignee at 2009-06-15T22:55:46.519Z is not shralex
         * - assignee before 2009-06-15T22:55:46.519Z is neither shralex nor phunt
         */

        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + "ZOOKEEPER-107.json");

        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);

        Timestamp historyTimestamp = Timestamp.from(Instant.parse("2009-06-15T22:55:46.519Z"));
        Timestamp now = TimeTools.now();
        Timestamp openingDate = new OpeningDate().apply(new MeasurementDateBean(null, issue));

        assertTrue(issueController.hasBeenAssigned(new HasBeenAssignedBean(issue, "shralex", now)));
        assertTrue(issueController.hasBeenAssigned(new HasBeenAssignedBean(issue, "phunt", historyTimestamp)));
        assertFalse(issueController.hasBeenAssigned(new HasBeenAssignedBean(issue, "shralex", historyTimestamp)));
        assertFalse(issueController.hasBeenAssigned(new HasBeenAssignedBean(issue, "shralex", openingDate)));
    }


    @Test
    @Transactional
    public void testGetInProgressTimespans() throws JsonIOException, JsonSyntaxException, FileNotFoundException{

        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + "HUDI-8573.json");

        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);

        /**
         * Test the following cases:
         * - measurement date is today --> 1 in progress timespan, starting at 
         * 2024-11-24T14:16:13.824+0000 and ending at 2024-11-25T07:51:20.795+0000
         * - measurement date at 2024-11-24T14:16:14.824+0000 --> 1 in progress timespan,
         * starting at 2024-11-24T14:16:13.824+0000 and ending at measurement date
         * - measurement date at opening date: 0 timespans available.
         */

        Timestamp now = TimeTools.now();
        Timestamp openingDate = new OpeningDate().apply(new MeasurementDateBean(null, issue));
        Timestamp start = Timestamp.from(Instant.parse("2024-11-24T14:16:13.824Z"));
        Timestamp oneSecondAfterStart = Timestamp.from(start.toInstant().plus(1, ChronoUnit.SECONDS));
        Timestamp end = Timestamp.from(Instant.parse("2024-11-25T07:51:20.795Z"));

        IssueTemporalSpanBean bean = new IssueTemporalSpanBean(issue, now);
        issueController.getInProgressTemporalSpans(bean);
        assertEquals(1, bean.getTemporalSpans().size());
        assertEquals(start, bean.getTemporalSpans().get(0).getStart());
        assertEquals(end, bean.getTemporalSpans().get(0).getEnd());

        bean = new IssueTemporalSpanBean(issue, oneSecondAfterStart);
        issueController.getInProgressTemporalSpans(bean);
        assertEquals(1, bean.getTemporalSpans().size());
        assertEquals(start, bean.getTemporalSpans().get(0).getStart());
        assertEquals(oneSecondAfterStart, bean.getTemporalSpans().get(0).getEnd());

        bean = new IssueTemporalSpanBean(issue, openingDate);
        issueController.getInProgressTemporalSpans(bean);
        assertEquals(0, bean.getTemporalSpans().size());

    }

    @Test
    @Transactional
    public void testGetComponents() throws JsonIOException, JsonSyntaxException, FileNotFoundException{
        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + "AVRO-1124.json");

        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);

        /**
         * Test the following cases:
         * - measurement date is today --> 1 component, id:"12312780"
         * - measurement date is opening date --> 0 components
         */

        Timestamp now = TimeTools.now();
        Timestamp openingDate = new OpeningDate().apply(new MeasurementDateBean(null, issue));
        Set<String> components;

        components = issueController.getComponentsIds(new IssueBean(issue, now));
        assertEquals(1, components.size());
        assertEquals("12312780", components.iterator().next());

        components = issueController.getComponentsIds(new IssueBean(issue, openingDate));
        assertEquals(0, components.size());
    }
}
