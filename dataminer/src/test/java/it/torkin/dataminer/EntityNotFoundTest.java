package it.torkin.dataminer;

import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.apachejit.Issue;
import jakarta.transaction.Transactional;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EntityNotFoundTest {

    @Autowired private IssueDao issueDao;

    @Test
    @Transactional
    public void testEntityNotFound(){
        Issue issue;
        
        issue = issueDao.findByKey("PROJ-123");

        assertNull(issue);
    }
    
}
