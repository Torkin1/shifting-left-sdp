package it.torkin.dataminer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.Issue;
import jakarta.transaction.Transactional;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Profile("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class LocalDBTest extends AbstractTransactionalJUnit4SpringContextTests{

    @Autowired
    private IssueDao issueDao;

    private Logger logger = Logger.getLogger(LocalDBTest.class.getName()); 

    private final String key = "KEY-1";
    
    @Test
    @Transactional
    public void testStoreAndLoad(){
        
        // let's test the db

        Issue expected;
        Issue actual;

        expected = new Issue();
        expected.setId(1L);
        expected.setKey(key);
        expected.setBuggy(true);

        issueDao.saveAndFlush(expected);
        logger.fine("issue saved successfully");

        actual = issueDao.findByKey(key);
        logger.fine("issue loaded successfully");

        assertEquals(expected.isBuggy(), actual.isBuggy());

    }

}
