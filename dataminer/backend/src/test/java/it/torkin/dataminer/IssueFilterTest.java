package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.NotMostRecentFilterConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.dataset.processed.filters.impl.ExclusiveBuggyCommitsOnlyFilter;
import it.torkin.dataminer.control.dataset.processed.filters.impl.NotMostRecentFilter;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.toolbox.math.SafeMath;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class IssueFilterTest {

    @Autowired private IssueDao issueDao;
    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;

    @Autowired private IDatasetController datasetController; 
    @Autowired private NotMostRecentFilterConfig notMostRecentFilterConfig;
    
    @Autowired private NotMostRecentFilter filter;

    @Test
    @Transactional
    public void testNotMostRecentFilter() throws UnableToCreateRawDatasetException{
        
        datasetController.createRawDataset();
        filter.reset();


        long totalCount = issueDao.countAllByDatasetName("leveragingjit");
        long expectedFilteredCount = SafeMath.ceiledInversePercentage(notMostRecentFilterConfig.getPercentage(), totalCount);
        Stream<Issue> issues = issueDao.findAllByDatasetName("leveragingjit")
            .filter((issue) -> filter.apply(new IssueFilterBean(issue, "leveragingjit")));
        long actualCount = issues.count();
        long expectedCount = totalCount - expectedFilteredCount;

        log.info("Expected count: {}, Actual count: {}", expectedCount, actualCount);
        log.info("Total count: {}, expected filtered count: {}", totalCount, expectedFilteredCount);
        log.info(notMostRecentFilterConfig.toString());        

        assertEquals(expectedCount, actualCount);
    }
    
    @Test
    public void testExclusiveBuggyCommitsOnlyFilters() {

        Dataset dataset = new Dataset();
        dataset.setName("dataset_test");
        dataset = datasetDao.save(dataset);
        
        Issue issue1 = new Issue();
        issue1.setKey("ISSUE-1");
        Issue issue2 = new Issue();
        issue2.setKey("ISSUE-2");
        Issue issue3 = new Issue();
        issue3.setKey("ISSUE-3");

        Commit sharedBuggyCommit = new Commit();
        sharedBuggyCommit.setBuggy(true);
        sharedBuggyCommit.setDataset(dataset);
        sharedBuggyCommit.setHash("sharedBuggyCommit");

        Commit buggyCommit = new Commit();
        buggyCommit.setBuggy(true);
        buggyCommit.setDataset(dataset);
        buggyCommit.setHash("buggyCommit");

        Commit cleanCommit = new Commit();
        cleanCommit.setBuggy(false);
        cleanCommit.setDataset(dataset);
        cleanCommit.setHash("cleanCommit");

        /**
         * Issue 1 has 2 commits, one shared with issue 2
         * and both buggy.
         */
        issue1.getCommits().add(sharedBuggyCommit);
        sharedBuggyCommit.getIssues().add(issue1);
        issue1.getCommits().add(buggyCommit);
        buggyCommit.getIssues().add(issue1);

        /**
         * Issue 2 has only one commit, shared with issue 1
         * and buggy
         */
        sharedBuggyCommit.getIssues().add(issue2);
        issue2.getCommits().add(sharedBuggyCommit);

        /** Issue 3 has only one commit, clean */
        cleanCommit.getIssues().add(issue3);
        issue3.getCommits().add(cleanCommit);

        issueDao.save(issue1);
        issueDao.save(issue2);
        issueDao.save(issue3);
        
        commitDao.save(sharedBuggyCommit);
        commitDao.save(buggyCommit);
        commitDao.save(cleanCommit);

        issue1 = issueDao.findByKey("ISSUE-1");
        issue2 = issueDao.findByKey("ISSUE-2");
        issue3 = issueDao.findByKey("ISSUE-3");
        /**
         * When getting issues through the filter, we expect:
         * - issue 1 to pass the filter since it has at least an exclusive buggy commit
         * - issue 2 to be filtered out since it has only shared buggy commits
         * - issue 3 to pass the filter since it has no buggy commits
         */
        IssueFilter filter = new ExclusiveBuggyCommitsOnlyFilter();
        assertTrue(filter.apply(new IssueFilterBean(issue1, dataset.getName())));
        assertFalse(filter.apply(new IssueFilterBean(issue2, dataset.getName())));
        assertTrue(filter.apply(new IssueFilterBean(issue3, dataset.getName())));

    }
    
}
