package it.torkin.dataminer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.IssueBugginessBean;
import it.torkin.dataminer.toolbox.time.TimeTools;
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

    @Test
    public void testIsBuggy(){
        
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

        Commit cleanCommit = new Commit();
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);
        cleanCommit.setHash("cleanCommit");
        cleanCommit.setTimestamp(TimeTools.now());

        Commit buggyCommitWrongDataset = new Commit();
        buggyCommitWrongDataset.setBuggy(true);
        buggyCommitWrongDataset.setDataset(wrongDataset);
        buggyCommitWrongDataset.setHash("buggyCommitWrongDataset");
        buggyCommitWrongDataset.setTimestamp(TimeTools.now());

        issue1.getCommits().add(buggyCommit);
        buggyCommit.getIssues().add(issue1);

        issue2.getCommits().add(cleanCommit);
        cleanCommit.getIssues().add(issue2);

        issue3.getCommits().add(buggyCommitWrongDataset);
        buggyCommitWrongDataset.getIssues().add(issue3);

        issueDao.save(issue1);
        issueDao.save(issue2);
        issueDao.save(issue3);

        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);
        commitDao.save(buggyCommitWrongDataset);

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
        assertTrue(issue1.isBuggy(new IssueBugginessBean("dataset_test", TimeTools.now())));
        assertFalse(issue2.isBuggy(new IssueBugginessBean("dataset_test", TimeTools.now())));
        assertFalse(issue3.isBuggy(new IssueBugginessBean("dataset_test", TimeTools.now())));
        assertTrue(issue3.isBuggy(new IssueBugginessBean("dataset_test2", TimeTools.now())));
        

    }
    
}
