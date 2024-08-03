package it.torkin.dataminer.dao.jira;

import java.util.logging.Logger;

import it.torkin.dataminer.entities.jira.issue.Issue;
import it.torkin.dataminer.rest.ClientResourceRequest;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JiraDao {

    private String hostname;
    private int apiVersion;
    
    private static final Logger logger = Logger.getLogger(JiraDao.class.getName());
    
    public String forgeQuery(QueryTemplate queryTemplate, Object... args){
        return String.format(queryTemplate.toString(), args);
    }

    public Issue queryIssue(String key) throws UnableToGetIssueException{

        String query;
        Issue issue;

        try {
            query = forgeQuery(QueryTemplate.GET_ISSUE_BY_KEY, hostname, apiVersion, key);
            issue = new ClientResourceRequest<>(Issue.class, query).getResource();
            return issue;
        } catch (UnableToGetResourceException e) {
            throw new UnableToGetIssueException(e, hostname, key);
        }
        
    }

}
