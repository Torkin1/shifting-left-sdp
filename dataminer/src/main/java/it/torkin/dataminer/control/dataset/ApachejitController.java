package it.torkin.dataminer.control.dataset;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.ApachejitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.dao.apachejit.ApachejitDao;
import it.torkin.dataminer.dao.apachejit.CommitRecord;
import it.torkin.dataminer.dao.apachejit.Resultset;
import it.torkin.dataminer.dao.apachejit.UnableToGetCommitsException;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.git.IssueNotFoundException;
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
    @Autowired private CommitDao commitDao;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;
    @Autowired private DeveloperDao developerDao;
    
    private Dataset dataset;
    
    private Developer resolveDeveloper(Developer developer){
        Developer resolved = developerDao.findByKey(developer.getKey());
        if(resolved == null){
            resolved = developerDao.save(developer);
        }
        return resolved;

    }
        
    
    /** https://stackoverflow.com/questions/78844495/org-springframework-dao-duplicatekeyexception-a-different-object-with-the-same */
    private void mergeObjectsEntity(Issue issue){

        mergeDevelopers(issue);

    }
    
    private void mergeDevelopers(Issue issue){

        IssueFields fields = issue.getDetails().getFields();
        
        fields.setAssignee(resolveDeveloper(fields.getAssignee()));
        fields.setCreator(fields.getCreator());
        fields.setReporter(fields.getReporter());

        for (IssueComment comment : fields.getComment().getComments()) {
            
            comment.setAuthor(resolveDeveloper(comment.getAuthor()));
            comment.setUpdateAuthor(resolveDeveloper(comment.getUpdateAuthor())); 
        }
        for (IssueWorkItem workItem : fields.getWorklog().getWorklogs()) {
            workItem.setAuthor(resolveDeveloper(workItem.getAuthor()));
            workItem.setUpdateAuthor(resolveDeveloper(workItem.getUpdateAuthor()));
        }
        for(IssueAttachment attachment : fields.getAttachments()){
            attachment.setAuthor(resolveDeveloper(attachment.getAuthor()));
        }
    }

    private void linkIssueDetails(Issue issue, JiraDao jiraDao) throws UnableToLinkIssueDetailsException {
        try {
            IssueDetails details = jiraDao.queryIssueDetails(issue.getKey());
            issue.setDetails(details);
            mergeObjectsEntity(issue);
            
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
                } catch (IssueNotFoundException
                 | UnableToLinkIssueDetailsException | CommitAlreadyLoadedException e) {
                    log.warn(String.format("Skipping commit %s: %s", record.getCommit_id(), e.getMessage()));
                    if(e.getClass() != CommitAlreadyLoadedException.class)
                        dataset.getSkipped().put(record.getCommit_id(), (issue!=null)? issue.getKey() : null);
                }
            }
            

        } catch (UnableToGetCommitsException | IOException e) {
            throw new UnableToLoadCommitsException(e);
        }
        
    }

    private void linkIssueCommit(Issue issue, Commit commit) throws IssueNotFoundException{
        GitDao gitDao = new GitDao();
        String issuekey;

        issuekey = gitDao.getLinkedIssueByCommit(commit.getHash());
        issue.getCommits().add(commit);
        issue.setKey(issuekey);

        dataset.setNrLinkedCommits(dataset.getNrLinkedCommits() + 1);

    }

    private Commit loadCommit(CommitRecord record) throws CommitAlreadyLoadedException {
        
        Commit commit;

        if(commitDao.existsByHash(record.getCommit_id())
        && !apachejitConfig.isRefresh()){
            throw new CommitAlreadyLoadedException(record.getCommit_id(), apachejitConfig.isRefresh());
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
                

        if(apachejitConfig.isSkipLoad()) return;

        try {
                
            dataset = new Dataset();
            dataset.setName("apachejit");
            loadCommits();
            datasetDao.save(dataset);

        } catch (UnableToLoadCommitsException e) {
            dataset = null;
            throw new UnableToLoadDatasetException(e);
        }
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }
    
}
