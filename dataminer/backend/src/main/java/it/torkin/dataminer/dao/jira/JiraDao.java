package it.torkin.dataminer.dao.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.rest.ClientResourceRequest;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Client for Jira REST API
 */
public class JiraDao {

    private final String hostname;
    private final int apiVersion;

    private Set<String> failedKeys = new HashSet<>();

    @Getter
    @Setter
    private boolean cacheFailedKeys = true;

    public JiraDao(JiraConfig config){
        this.hostname = config.getHostname();
        this.apiVersion = config.getApiVersion();
    }
        
    public String forgeQuery(QueryFormat queryTemplate, Object... args){
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
            if (cacheFailedKeys && failedKeys.contains(key))
                throw new UnableToGetIssueException("key has already showed to be not owned by an available issue", hostname, key);
            query = forgeQuery(QueryFormat.GET_ISSUE_BY_KEY, key);
            issue = new ClientResourceRequest<>(IssueDetails.class, query).getResource();
            /**
             * Retrieved issue could be different from the requested one
             * (see comments in the key field of IssueDetails class)
             */
            if (!issue.getJiraKey().equals(key)) throw new IssueKeyMismatchException(key, issue.getJiraKey());
            return issue;
        } catch (UnableToGetResourceException | IssueKeyMismatchException e) {
            if (cacheFailedKeys) failedKeys.add(key);
            throw new UnableToGetIssueException(e, hostname, key);
        }
        
    }

}
