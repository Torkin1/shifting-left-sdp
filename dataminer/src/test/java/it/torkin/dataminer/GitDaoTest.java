package it.torkin.dataminer;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.dao.git.GitDao;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class GitDaoTest {

    @Autowired private GitConfig gitConfig;

    private final String projectName = "Torkin1/test";
    
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
}
