package it.torkin.dataminer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

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

import it.torkin.dataminer.control.dataset.IEntityMerger;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.apachejit.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;
import jakarta.transaction.Transactional;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EntityMergerTest {

    @Autowired private IEntityMerger entityMerger;
    @Autowired private IssueDao issueDao;
    
    private static final String ISSUE_EXAMPLES_DIR = "./src/test/resources/issue_examples/";

    @Test
    @Transactional
    public void testMergeEntity() throws JsonIOException, JsonSyntaxException, FileNotFoundException{

        File[] issue_samples = new File(ISSUE_EXAMPLES_DIR).listFiles();
        IssueDetails issueDetails;
        Issue expected;
        Issue actual;
        
        Gson gson = new GsonBuilder()
         .setExclusionStrategies(new AnnotationExclusionStrategy()).create();


        
        for (File issue_sample : issue_samples){
            
            expected = new Issue();
            issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
            expected.setKey(issueDetails.getJiraKey());
            entityMerger.mergeIssueDetails(issueDetails);
            issueDao.saveAndFlush(expected);
            
            actual = issueDao.findById(expected.getKey()).get();
            assertEquals(expected.getKey(), actual.getKey());
        }
        

    }
}
