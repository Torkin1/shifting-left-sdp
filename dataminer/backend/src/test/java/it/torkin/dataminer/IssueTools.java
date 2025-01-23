package it.torkin.dataminer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.parsing.AnnotationExclusionStrategy;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IssueTools {
    
    private final String ISSUE_EXAMPLES_DIR = "./src/test/resources/issue_examples/";
    
    public Issue getIssueExample(String issueKey) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
                Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        File issue_sample = new File(ISSUE_EXAMPLES_DIR + issueKey + ".json"); 
        
        IssueDetails issueDetails = gson.fromJson(new JsonReader(new FileReader(issue_sample.getAbsolutePath())), IssueDetails.class);
        Issue issue = new Issue();
        issue.setDetails(issueDetails);
        return issue;
    }
}
