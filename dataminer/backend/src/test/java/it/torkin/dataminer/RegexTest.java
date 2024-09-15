package it.torkin.dataminer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.toolbox.regex.NoMatchFoundException;
import it.torkin.dataminer.toolbox.regex.Regex;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class RegexTest {
    
    @Autowired private GitConfig gitConfig;

    @Test
    public void testIssueKeyRegexp() throws NoMatchFoundException{

        String expected = "PROJ-123";
        String commitMessage = String.format("%s: test commit message", expected);
        String actual;

        Regex keys = new Regex(gitConfig.getLinkedIssueKeyRegexp(), commitMessage);

        actual = keys.iterator().next();
        assertEquals(expected, actual);
    }
}
