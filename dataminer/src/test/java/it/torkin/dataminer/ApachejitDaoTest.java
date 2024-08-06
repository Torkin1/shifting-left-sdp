package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.apachejit.ApachejitDao;
import it.torkin.dataminer.dao.apachejit.CommitRecord;
import it.torkin.dataminer.dao.apachejit.UnableToGetCommitsException;
import it.torkin.dataminer.dao.apachejit.UnableToGetIssuesException;
import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Profile("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class ApachejitDaoTest extends AbstractTransactionalJUnit4SpringContextTests{

    private ApachejitDao apachejitDao = new ApachejitDao();

    private final String commitsPath = "./src/main/resources/apachejit/dataset/apachejit_total.csv";
    private final String issuesPath = "./src/main/resources/apachejit/data";
    
    @Test
    @Transactional
    public void testLoadCommit() throws UnableToGetCommitsException{

        String expectedCommitId = "7b8480744ea6e6fb41efd4329bb470c8f3c763db";
        String actualCommitId;

        CommitRecord commit;
                
        commit = apachejitDao.getAllCommits(commitsPath).next();
        actualCommitId = commit.getCommit_id();

        assertEquals(expectedCommitId, actualCommitId);

    }

    @Test
    @Transactional
    public void testLoadIssues() throws UnableToGetIssuesException{

        assertNotEquals(0, apachejitDao.getAllIssues(issuesPath).size());

    }
    
}
