package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.apachejit.Issue;
import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class LocalDBTest extends AbstractTransactionalJUnit4SpringContextTests{

    @Autowired private IssueDao issueDao;
        
    @Test
    @Transactional
    public void testStoreAndLoad() throws FileNotFoundException{
        
        String expected = "PROJ-123";
        String actual;
        
        // let's test the db
        Issue issue = new Issue();

        issue.setKey(expected);
        issueDao.saveAndFlush(issue);

        issue = issueDao.findByKey(expected);
        actual = issue.getKey();

        assertEquals(expected, actual);

        
    }
}
