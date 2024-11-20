package it.torkin.dataminer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.git.UnableToInitRepoException;
import it.torkin.dataminer.entities.dataset.Commit;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class GitDaoTest {

    @Autowired private GitConfig gitConfig;

    private final String projectName = "Torkin1/test";
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
        commit.setHash("d06ae18329eeef15c2e982aa0905832a09bd2508");

        try (GitDao gitDao = new GitDao(gitConfig, projectName)){
            
            File reposdir = new File(gitConfig.getReposDir()+"/"+projectName);
            File[] repos;

            gitDao.checkout();
            repos = reposdir.listFiles();
            // when counting files, don't forget the .git folder
            assertEquals(3, repos.length);
            
            gitDao.checkout(commit);
            repos = reposdir.listFiles();
            assertEquals(2, repos.length);
            
            Date date = Date.from(Instant.now());
            gitDao.checkout(date);
            repos = reposdir.listFiles();
            assertEquals(3, repos.length);
            
        }
    }
}
