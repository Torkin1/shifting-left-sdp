package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.config.filters.LinkageFilterConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.dataset.processed.filters.impl.ExclusiveBuggyCommitsOnlyFilter;
import it.torkin.dataminer.control.dataset.processed.filters.impl.FirstCommitAfterOpeningDateFilter;
import it.torkin.dataminer.control.dataset.processed.filters.impl.LinkageFilter;
import it.torkin.dataminer.control.dataset.processed.filters.impl.MeasurementAfterOpeningDateFilter;
import it.torkin.dataminer.control.dataset.processed.filters.impl.NotMostRecentFilter;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.ILinkageController;
import it.torkin.dataminer.control.dataset.stats.LinkageBean;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.project.Project;
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
    @Autowired private ProjectDao projectDao;
    
    @Autowired private LinkageFilter linkageFilter;
    @Autowired private NotMostRecentFilter filter;

    @Autowired private ILinkageController linkageController;
    @Autowired private IDatasetController datasetController; 

    @Autowired private DatasourceGlobalConfig datasourceGlobalConfig;
    @Autowired private LinkageFilterConfig linkageFilterConfig;

    @Test
    @Transactional
    public void testNotMostRecentFilter() throws UnableToCreateRawDatasetException{
        

        datasetController.createRawDataset();

        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets){
            Map<String, Long> actualCountByProject = new HashMap<>();
            Map<String, Long> expectedCountByProject = new HashMap<>();

            Set<Project> projects = projectDao.findAllByDataset(dataset.getName());
            for (Project project : projects){
                long totalCount = issueDao.countByDatasetAndProject(dataset.getName(), project.getKey());
                double percentage = datasourceGlobalConfig.getSourcesMap().get(dataset.getName()).getSnoringPercentage();
                long expectedFilteredCount = SafeMath.ceiledInversePercentage(percentage, totalCount);
                long expectedCount = totalCount - expectedFilteredCount;
                log.info("expected count for project {}: {} - {} = {}", project.getKey(), totalCount, expectedFilteredCount, expectedCount);
                expectedCountByProject.put(project.getKey(), expectedCount);
            }
            IssueFilterBean issueFilterBean = new IssueFilterBean();
            
            Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName())
                .filter((issue) -> {
                    issueFilterBean.setIssue(issue);
                    issueFilterBean.setDatasetName(dataset.getName());
                    issueFilterBean.setApplyAnyway(false);
                    return filter.apply(issueFilterBean);
                });
            issues.forEach((issue) -> {
                Project project = issue.getDetails().getFields().getProject();
                actualCountByProject.compute(project.getKey(), (p, count) -> count == null ? 1 : count + 1);
            });
    
            log.info("Expected count by project: {}", expectedCountByProject);
            log.info("Actual count by project: {}", actualCountByProject);
            expectedCountByProject.forEach((project, expectedCount) -> {
                assertEquals(expectedCount, actualCountByProject.getOrDefault(project, 0L));
            });
        }
        
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
        assertTrue(filter.apply(new IssueFilterBean(issue1, dataset.getName(), null, false)));
        assertFalse(filter.apply(new IssueFilterBean(issue2, dataset.getName(), null, false)));
        assertTrue(filter.apply(new IssueFilterBean(issue3, dataset.getName(), null, false)));

    }

    @Test
    @Transactional
    public void testLinkageFilter() throws UnableToCreateRawDatasetException{

        /**
         * We expect that repositories of processed issues have a
         * linkage above the top N repositories fetched from 
         * the datasets.
        */
        datasetController.createRawDataset();

        List<Dataset> datasets = datasetDao.findAll();
        List<Double> buggyLinkages = new ArrayList<>();
        Map<String, LinkageBean> buggyLinkagesByDataset = new HashMap<>();

        for (Dataset dataset : datasets) {
            LinkageBean buggyLinkageBean = new LinkageBean(dataset.getName());
            linkageController.calcBuggyTicketLinkage(buggyLinkageBean);
            buggyLinkagesByDataset.put(dataset.getName(), buggyLinkageBean);
            buggyLinkageBean.getLinkageByRepository().forEach((repository, linkage) -> {
                if (repository != LinkageBean.ALL_REPOSITORIES){
                    buggyLinkages.add(linkage);
                }
            });
        }
        buggyLinkages.sort(Double::compare);
        log.info("buggyLinkages: {}", buggyLinkages);
        int selectedIndex = buggyLinkages.size() >= linkageFilterConfig.getTopNBuggyLinkage() ? buggyLinkages.size() - linkageFilterConfig.getTopNBuggyLinkage() : 0;
        log.info("threshold: {}", buggyLinkages.get(selectedIndex));

        IssueFilterBean bean = new IssueFilterBean();
        for (Dataset dataset : datasets){
            Stream<Issue> issues = issueDao.findAllByDataset(dataset.getName());
            issues
             .filter(issue -> {
                bean.setIssue(issue);
                bean.setDatasetName(dataset.getName());
                bean.setApplyAnyway(true);
                return linkageFilter.apply(bean);
             })
             .forEach(issue -> {
                for(Commit commit : issue.getCommits()){
                    if (commit.getDataset().getName().equals(dataset.getName())){
                        assertTrue(buggyLinkagesByDataset.get(dataset.getName()).getLinkageByRepository().get(commit.getRepository()) >= buggyLinkages.get(selectedIndex));
                    }
                } 
             });
        }

    }

    @Autowired private FirstCommitAfterOpeningDateFilter firstCommitAfterOpeningDate;

    @Test
    @Transactional
    public void testFirstCommitAfterOpeningDate(){

        Issue issue = new Issue();
        Commit commit = new Commit();
        Dataset dataset = new Dataset();

        Instant openingDate = Instant.now();
        // opening date is 1 second before the first commit
        Instant firstCommitDate = openingDate.minus(1, ChronoUnit.SECONDS); 

        dataset.setName("dataset_test");
        commit.setDataset(dataset);
        commit.setTimestamp(Timestamp.from(firstCommitDate));
        issue.getCommits().add(commit);
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());
        issue.getDetails().getFields().setCreated(Timestamp.from(openingDate));

        assertFalse(firstCommitAfterOpeningDate.apply(new IssueFilterBean(issue, dataset.getName(), null, false)));

    }

    @Autowired private MeasurementAfterOpeningDateFilter measurementAfterOpeningDateFilter;

    @Test
    @Transactional
    public void testMeasurementAfterOpeningDate(){
        Issue issue = new Issue();

        Instant openingDate = Instant.now();
        Instant measurementDate = openingDate.minus(1, ChronoUnit.SECONDS); 
        
        issue.setDetails(new IssueDetails());
        issue.getDetails().setFields(new IssueFields());
        issue.getDetails().getFields().setCreated(Timestamp.from(openingDate));
        
        assertFalse(measurementAfterOpeningDateFilter.apply(new IssueFilterBean(issue, "", Timestamp.from(measurementDate), false)));
    }
    
}
