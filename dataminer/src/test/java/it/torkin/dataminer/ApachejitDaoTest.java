package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.ApachejitConfig;
import it.torkin.dataminer.dao.apachejit.ApachejitDao;
import it.torkin.dataminer.dao.apachejit.CommitRecord;
import it.torkin.dataminer.dao.apachejit.UnableToGetCommitsException;
import jakarta.transaction.Transactional;

@SpringBootTest()
@Profile("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ApachejitDaoTest extends AbstractTransactionalJUnit4SpringContextTests{

    @Autowired private ApachejitConfig apachejitConfig;
    
    @Test
    @Transactional
    public void testLoadCommit() throws UnableToGetCommitsException{

        ApachejitDao apachejitDao = new ApachejitDao(apachejitConfig);
        
        String expectedCommitId = "7b8480744ea6e6fb41efd4329bb470c8f3c763db";
        String actualCommitId;

        CommitRecord commit;
                
        commit = apachejitDao.getAllCommits().next();
        actualCommitId = commit.getCommit_id();

        assertEquals(expectedCommitId, actualCommitId);

    }
    
}
