package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.dao.jira.UnableToGetIssueException;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;
import jakarta.transaction.Transactional;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class JiraDaoTest extends AbstractTransactionalJUnit4SpringContextTests{

    
    private static final String ISSUE_EXAMPLES_DIR = "./src/test/resources/issue_examples/";
    @Autowired private JiraConfig jiraConfig;
    
    @Test
    @Transactional
    public void testQueryFromJira() throws FileNotFoundException, UnableToGetIssueException{
        
        File[] issue_samples = new File(ISSUE_EXAMPLES_DIR).listFiles();

        IssueDetails expected;
        IssueDetails actual;

        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

        JiraDao jiraDao = new JiraDao(jiraConfig); 
        
        for (File issue_sample : issue_samples){
            expected = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
            actual = jiraDao.queryIssueDetails(expected.getJiraKey());
            assertEquals(expected.getJiraId(), actual.getJiraId());
        }
        
    }
}
