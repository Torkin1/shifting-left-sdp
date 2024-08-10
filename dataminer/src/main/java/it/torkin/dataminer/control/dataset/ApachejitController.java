package it.torkin.dataminer.control.dataset;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.ApachejitConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.dao.apachejit.ApachejitDao;
import it.torkin.dataminer.dao.apachejit.CommitRecord;
import it.torkin.dataminer.dao.apachejit.Resultset;
import it.torkin.dataminer.dao.apachejit.UnableToGetCommitsException;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.git.UnableToGetLinkedIssueKeyException;
import it.torkin.dataminer.dao.git.UnableToInitRepoException;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.dao.jira.UnableToGetIssueException;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.DeveloperDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.Dataset;
import it.torkin.dataminer.entities.apachejit.Commit;
import it.torkin.dataminer.entities.apachejit.Issue;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.issue.IssueAttachment;
import it.torkin.dataminer.entities.jira.issue.IssueComment;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.issue.IssueWorkItem;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApachejitController implements IDatasetController{

    @Autowired private ApachejitConfig apachejitConfig;
    @Autowired private JiraConfig jiraConfig;
    @Autowired private GitConfig gitConfig;

    @Autowired private CommitDao commitDao;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private DeveloperDao developerDao;
    
    private Dataset dataset;
    private Map<String, GitDao> gitdaoByProject = new HashMap<>();
    
    /**
     * Checks if the developer object refers to an existing entity in the local db.
     * If that's the case, the existing entity is returned, otherwise the developer
     * object is saved in the db and returned.
     * @param developer
     * @return
     */
    private Developer mergeDeveloper(Developer developer){
        Developer merged = developerDao.findByKey(developer.getKey());
        if(merged == null){
            merged = developerDao.save(developer);
        }
        return merged;

    }
        
    
    /** https://stackoverflow.com/questions/78844495/org-springframework-dao-duplicatekeyexception-a-different-object-with-the-same */
    private void mergeEntities(Issue issue){

        mergeDevelopers(issue);

    }
    
    private void mergeDevelopers(Issue issue){

        IssueFields fields = issue.getDetails().getFields();
        
        fields.setAssignee(mergeDeveloper(fields.getAssignee()));
        fields.setCreator(fields.getCreator());
        fields.setReporter(fields.getReporter());

        for (IssueComment comment : fields.getComment().getComments()) {
            
            comment.setAuthor(mergeDeveloper(comment.getAuthor()));
            comment.setUpdateAuthor(mergeDeveloper(comment.getUpdateAuthor())); 
        }
        for (IssueWorkItem workItem : fields.getWorklog().getWorklogs()) {
            workItem.setAuthor(mergeDeveloper(workItem.getAuthor()));
            workItem.setUpdateAuthor(mergeDeveloper(workItem.getUpdateAuthor()));
        }
        for(IssueAttachment attachment : fields.getAttachments()){
            attachment.setAuthor(mergeDeveloper(attachment.getAuthor()));
        }
    }

    private void linkIssueDetails(Issue issue, JiraDao jiraDao) throws UnableToLinkIssueDetailsException {
        try {
            IssueDetails details = jiraDao.queryIssueDetails(issue.getKey());
            issue.setDetails(details);
            mergeEntities(issue);
            
        } catch (UnableToGetIssueException e) {
            throw new UnableToLinkIssueDetailsException(e);
        }
    }

    private void loadCommits() throws UnableToLoadCommitsException {
// for each commit record in apachejit:
//         - create commit entity by commit record id if not exists
//         - mine project name among other features
//         - resolve project repo
//         - if repo not available locally:
//           - download repo in resources
//         - get issue key from repo
//         - get commit timestamp from repo
//         - create issue entity if not exists
//         - link issue details
//         - link commit
//         - store issue
        CommitRecord record;
        Commit commit;
        ApachejitDao apachejitDao;
        JiraDao jiraDao;
        Issue issue;

        apachejitDao = new ApachejitDao(apachejitConfig);
        jiraDao = new JiraDao(jiraConfig);

        try (Resultset<CommitRecord> commits = apachejitDao.getAllCommits()) {

            while (commits.hasNext()) {
                record = commits.next();
                issue = null;
                try {
                    
                    commit = loadCommit(record);
                    issue = new Issue();
                    linkIssueCommit(issue, commit);
                    linkIssueDetails(issue, jiraDao);
                    issueDao.save(issue);
                
                } catch (UnableToLinkIssueDetailsException
                 | CommitAlreadyLoadedException | UnableToLinkIssueException e) {
                    log.warn(String.format("Skipping commit %s: %s", record.getCommit_id(), e.toString()));
                    if(e.getClass() != CommitAlreadyLoadedException.class)
                        dataset.getSkipped().put(record.getCommit_id(), (issue!=null)? issue.getKey() : null);
                }
            }
            

        } catch (UnableToGetCommitsException | IOException e) {
            throw new UnableToLoadCommitsException(e);
        }
        
    }

    private GitDao getGitdaoByProject(String project) throws UnableToInitRepoException{
        if(!gitdaoByProject.containsKey(project))
            gitdaoByProject.put(project, new GitDao(gitConfig, project));
        return gitdaoByProject.get(project);
    }
    
    /**
     * Loads in Issue object the key of the issue linked to the commit
     * and registers the commit in the issue object
     * @param issue
     * @param commit
     * @throws UnableToLinkIssueException
     */
    private void linkIssueCommit(Issue issue, Commit commit) throws UnableToLinkIssueException{
        
        String project;
        String issuekey;
        GitDao gitDao;

        try {
            
            // Gets key of issue linked to commit
            project = commit.getRepoName();            
            gitDao = getGitdaoByProject(project);
            issuekey = gitDao.getLinkedIssueKeyByCommit(commit.getHash());
            issue.setKey(issuekey);

            // adds commit to issue
            issue.getCommits().add(commit);

            dataset.setNrLinkedCommits(dataset.getNrLinkedCommits() + 1);
            if (issue.getCommits().size() > 1) 
                dataset.getIssuesWithMultipleCommits().add(issuekey);
            

        } catch (UnableToInitRepoException | UnableToGetLinkedIssueKeyException e) {
            throw new UnableToLinkIssueException(e);
        }
        
    }

    /**
     * Loads a commit record from apachejit
     */
    private Commit loadCommit(CommitRecord record) throws CommitAlreadyLoadedException {
        
        Commit commit;

        if(commitDao.existsByHash(record.getCommit_id())
        && !apachejitConfig.getRefresh()){
            throw new CommitAlreadyLoadedException(record.getCommit_id(), apachejitConfig.getRefresh());
        }

        commit = new Commit(record);
        dataset.setNrCommits(dataset.getNrCommits() + 1);

        return commit;
        
    }

    
    /*
     * Loads apachejit dataset from the filesystem into the local db,
     * fetching issue details from Jira API
     * 
     * @return a @Code Dataset entity with some stats about the dataset 
     */
    @Override
    public void loadDataset() throws UnableToLoadDatasetException {
                

        if(apachejitConfig.getSkipLoad()) return;

        try {
                
            init();
            loadCommits();
            datasetDao.save(dataset);

        } catch (UnableToLoadCommitsException e) {
            dataset = null;
            throw new UnableToLoadDatasetException(e);
        } finally {
            exit();
        }
    }

    private void init(){
        dataset = new Dataset();
        dataset.setName("apachejit");
    }

    private void exit() {
        
        gitdaoByProject.forEach((project, gitDao) -> {
            try(gitDao) {
            } catch (Exception e) {
                log.warn(String.format("Unable to close git dao of project %s: %s", project, e.getMessage()));
            }
        });
        gitdaoByProject.clear();
    }


    @Override
    public Dataset getDataset() {
        return dataset;
    }
    
}
