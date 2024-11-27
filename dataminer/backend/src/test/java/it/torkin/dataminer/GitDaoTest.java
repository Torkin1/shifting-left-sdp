package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.git.UnableToInitRepoException;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class GitDaoTest {

    @Autowired private GitConfig gitConfig;

    private String projectName = "Torkin1/test";
    private final String commitHash = "3b12fed371b70bc25c83bdf10ee0508c45e8b474";
    private final String commitMultipleIssuesHash = "e8fb3311e489a67ebfb8c84613e5c65767f4c254";
    
    @Test
    public void testRepoInit() throws Exception{

        try (GitDao gitDao = new GitDao(gitConfig, projectName)){
            assertFalse(isEmpty(new File(gitConfig.getReposDir() + File.pathSeparator + projectName).toPath()));
        }
    }

    private boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }

    
    @Test
    public void testGetLinkedIssueKeyByCommit() throws Exception{
        
        String expected = "PROJ-123";
        String actual;
        
        try (GitDao gitDao = new GitDao(gitConfig, projectName)){

            actual = gitDao.getLinkedIssueKeysByCommit(commitHash).get(0);
            assertEquals(expected, actual);

        }
    }

    @Test
    public void testMultipleLinkedIssues() throws UnableToInitRepoException, Exception{

        int expected = 2;
        int actual;

        try (GitDao gitDao = new GitDao(gitConfig, projectName)){

            actual = gitDao.getLinkedIssueKeysByCommit(commitMultipleIssuesHash).size();
            assertEquals(expected, actual);

        }
    }

    @Test
    public void testCheckout() throws UnableToInitRepoException, Exception{

        Commit commit = new Commit();
        commit.setHash(commitHash);

        try (GitDao gitDao = new GitDao(gitConfig, projectName)){
            
            File reposdir = new File(gitConfig.getReposDir()+"/"+projectName);
            File[] repos;

            gitDao.checkout();
            repos = reposdir.listFiles();
            // when counting files, don't forget the .git folder
            assertEquals(3, repos.length);

            
            Date date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse("Sun Apr 19 14:15:48 UTC 2009");
            gitDao.checkout(date);

            gitDao.checkout();

            gitDao.checkout(commit);
            repos = reposdir.listFiles();
            assertEquals(2, repos.length);

            gitDao.checkout();
            
            date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse("Fri Mar 27 09:22:07 UTC 2009");
            gitDao.checkout(date);
            repos = reposdir.listFiles();
            assertEquals(3, repos.length);
            
        }

    }

    @Autowired private IDatasetController datasetController;
    @Autowired private IssueDao issueDao;
    @Autowired private IIssueController issueController;

    @Test
    public void testCheckoutOnRealRepo() throws UnableToInitRepoException, Exception{
        datasetController.createRawDataset();

        List<Issue> issues = issueDao.findAll();
        
        for(Issue issue : issues){
            Commit firstCommit = issueController.getFirstCommit(new IssueCommitBean(issue, "apachejit"));
            if (firstCommit == null) continue;
            projectName = firstCommit.getRepository().getId();
            try (GitDao gitDao = new GitDao(gitConfig, projectName)){
                gitDao.checkout(issueController.getFirstCommit(new IssueCommitBean(issue, "apachejit")));
            }
        }
    }

    @Test
    public void testCommitCount() throws Exception{
        try(GitDao gitDao = new GitDao(gitConfig, projectName)){
            Timestamp start = TimeTools.dawnOfTime();
            Timestamp end = TimeTools.now();
            long commitCount = gitDao.getCommitCount(start, end);
            assertEquals(4, commitCount);
            long churn = gitDao.getChurn(start, end);
            log.debug("Churn: {}", churn);
            assertNotEquals(0, churn);
        }
    }

}
