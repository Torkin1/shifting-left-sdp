package it.torkin.dataminer.dao.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.entities.jira.issue.IssueStatus;
import it.torkin.dataminer.entities.jira.issue.IssueWorklog;
import it.torkin.dataminer.rest.ClientResourceRequest;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Client for Jira REST API
 */
@Slf4j
public class JiraDao {

    private final String hostname;
    private final int apiVersion;

    /**
     * If there are lots of false positive issue keys, you can set the jira dao to
     * use a cache to avoid querying the same faulty key multiple times.
     */
    private Set<String> failedKeys = new HashSet<>();

    @Getter
    @Setter
    private boolean useCache = true;

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

    /**
     * Gets details of issue from jira matching the given key
     */
    public IssueDetails queryIssueDetails(String key) throws UnableToGetIssueException{

        String query;
        IssueDetails issueDetails;

        try {
            if (useCache && failedKeys.contains(key))
                throw new UnableToGetIssueException("key has already showed to be not owned by an available issue", hostname, key);
            query = forgeQuery(QueryFormat.GET_ISSUE_BY_KEY, key);
            issueDetails = new ClientResourceRequest<>(IssueDetails.class, query).getResource();
            /**
             * Retrieved issue could be different from the requested one
             * (see comments in the key field of IssueDetails class)
             */
            if (!issueDetails.getJiraKey().equals(key)) throw new IssueKeyMismatchException(key, issueDetails.getJiraKey());

            // we check if worklog has more entries to fetch than the ones already provided
            // in the IssueFields object
            expandWorklog(issueDetails);

            return issueDetails;
        } catch (UnableToGetResourceException | IssueKeyMismatchException e) {
            if (useCache) failedKeys.add(key);
            throw new UnableToGetIssueException(e, hostname, key);
        }
        
    }

    private void expandWorklog(IssueDetails issueDetails){
        IssueWorklog worklog = issueDetails.getFields().getWorklog();
        if (worklog.getMaxResults() < worklog.getTotal()){
            // there are more worklog entries to fetch
            try {
                IssueWorklog fullWorklog = getIssueWorklog(issueDetails.getJiraKey());
                issueDetails.getFields().setWorklog(fullWorklog);
            } catch (UnableToGetIssueException e) {
                // worklog is not a fundamental detail, we can use the partial one already provided
                // in the issue details
                log.warn("unable to get full worklog for issue {}", issueDetails.getJiraKey(), e);
            }
        }
    }

    /**
     * Gets set of issue lifecycle statuses
     * @return
     * @throws UnableToGetResourceException
     */
    public IssueStatus[] queryIssueStatuses() throws UnableToGetResourceException{
        String query = forgeQuery(QueryFormat.GET_ISSUE_STATUSES);
        IssueStatus[] statuses = new ClientResourceRequest<>(IssueStatus[].class, query).getResource();

        return statuses;
        
    }

    /**
     * Gets entire worklog of jira issue.
     */
    public IssueWorklog getIssueWorklog(String key) throws UnableToGetIssueException{
        String query;
        IssueWorklog worklog;

        try {
            query = forgeQuery(QueryFormat.GET_ISSUE_WORKLOG, key);
            worklog = new ClientResourceRequest<>(IssueWorklog.class, query).getResource();
            return worklog;
        } catch (UnableToGetResourceException e) {
            throw new UnableToGetIssueException(e, hostname, key);
        }
    }

}
