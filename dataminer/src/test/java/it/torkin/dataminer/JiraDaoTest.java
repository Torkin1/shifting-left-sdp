package it.torkin.dataminer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.dao.jira.UnableToGetIssueException;
import it.torkin.dataminer.entities.jira.issue.Issue;
import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;
import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Profile("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class JiraDaoTest extends AbstractTransactionalJUnit4SpringContextTests{


    private Logger logger = Logger.getLogger(LocalDBTest.class.getName()); 
    
    private static final String ISSUE_EXAMPLES_DIR = "./src/test/resources/issue_examples/";
    
    @Test
    @Transactional
    public void testQueryFromJira() throws FileNotFoundException, UnableToGetIssueException{
        
        // let's test the db

        File[] issue_samples = new File(ISSUE_EXAMPLES_DIR).listFiles();

        Issue expected;
        Issue actual;

        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

        JiraDao jiraDao = new JiraDao("issues.apache.org", 2); 
        
        for (File issue_sample : issue_samples){
            expected = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), Issue.class);
            actual = jiraDao.queryIssue(expected.getKey());
            assertEquals(expected.getJiraId(), actual.getJiraId());
        }
        
    }
}
