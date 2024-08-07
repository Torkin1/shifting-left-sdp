package it.torkin.dataminer.dao.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.ClientResourceRequest;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import lombok.AllArgsConstructor;

/**
 * Client for Jira REST API
 */
@AllArgsConstructor
public class JiraDao {

    private String hostname;
    private int apiVersion;
        
    public String forgeQuery(QueryTemplate queryTemplate, Object... args){
        List<Object> argsList = new ArrayList<>();
        argsList.add(hostname);
        argsList.add(apiVersion);
        argsList.addAll(Arrays.asList(args));
        return String.format(queryTemplate.toString(), argsList.toArray());
    }

    public IssueDetails queryIssueDetails(String key) throws UnableToGetIssueException{

        String query;
        IssueDetails issue;

        try {
            query = forgeQuery(QueryTemplate.GET_ISSUE_BY_KEY, key);
            issue = new ClientResourceRequest<>(IssueDetails.class, query).getResource();
            /**
             * Retrieved issue could be different from the requested one
             * (see comments in the key field of IssueDetails class)
             */
            if (!issue.getJiraKey().equals(key)) throw new IssueKeyMismatchException(key, issue.getJiraKey());
            return issue;
        } catch (UnableToGetResourceException | IssueKeyMismatchException e) {
            throw new UnableToGetIssueException(e, hostname, key);
        }
        
    }

}
