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
import it.torkin.dataminer.dao.git.UnableToGetCommitDetailsException;
import it.torkin.dataminer.dao.git.UnableToGetLinkedIssueKeyException;
import it.torkin.dataminer.dao.git.UnableToInitRepoException;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.dao.jira.UnableToGetIssueException;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.entities.Dataset;
import it.torkin.dataminer.entities.apachejit.Commit;
import it.torkin.dataminer.entities.apachejit.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Service
@Slf4j
public class ApachejitController implements IDatasetController{

    @Autowired private ApachejitConfig apachejitConfig;
    @Autowired private JiraConfig jiraConfig;
    @Autowired private GitConfig gitConfig;

    @Autowired private CommitDao commitDao;
    @Autowired private IssueDao issueDao;
    @Autowired private DatasetDao datasetDao;

    @Autowired private EntityMerger entityMerger;
    
    private Dataset dataset;
    private Map<String, GitDao> gitdaoByProject = new HashMap<>();        
    
    
    private void linkIssueDetails(Issue issue, JiraDao jiraDao) throws UnableToLinkIssueDetailsException {
        try {
            IssueDetails details = jiraDao.queryIssueDetails(issue.getKey());
            entityMerger.mergeIssueDetails(details);
            issue.setDetails(details);
            
        } catch (UnableToGetIssueException e) {
            throw new UnableToLinkIssueDetailsException(e);
        }
    }

    /**
     * Gets issue entity linked to commit from DB if exists, elses creates a new one.
     * @param commit
     * @param progress
     * @return
     * @throws UnableToLinkIssueException
     */
    private Issue fetchIssue(Commit commit, JiraDao jiraDao, ProgressBar progress) throws UnableToLinkIssueException{
        
        Issue issue;
        GitDao gitDao;
        String issueKey;

        try {
            gitDao = getGitdaoByProject(commit.getRepoName());
            gitDao.getCommitDetails(commit);
            issueKey = gitDao.getLinkedIssueKeyByCommit(commit.getHash());
            issue = issueDao.findByKey(issueKey);
            if (issue == null){
                issue = new Issue(issueKey);
                progress.setExtraMessage(String.format("fetching issue details for issue %s", issue.getKey()));
                linkIssueDetails(issue, jiraDao);
            }
            return issue;
        } catch (UnableToInitRepoException | UnableToGetCommitDetailsException | UnableToGetLinkedIssueKeyException | UnableToLinkIssueDetailsException e) {
            throw new UnableToLinkIssueException(e);
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
        CommitRecord record = null;
        Commit commit;
        ApachejitDao apachejitDao;
        Issue issue = null;
        JiraDao jiraDao;

        apachejitDao = new ApachejitDao(apachejitConfig);
        jiraDao = new JiraDao(jiraConfig);

        try (Resultset<CommitRecord> records = apachejitDao.getAllCommits();
                ProgressBar progress = new ProgressBar("loading commits", apachejitConfig.getExpectedSize() == null? -1 : apachejitConfig.getExpectedSize())) {

            while (records.hasNext()) {
                
                try {
                    record = records.next();
                    commit = loadCommit(record);
                    issue = fetchIssue(commit, jiraDao, progress);
                    linkIssueCommit(issue, commit);                 
                    issueDao.save(issue);
                
                } catch (CommitAlreadyLoadedException | UnableToLinkIssueException e) {
                    log.warn(String.format("Skipping commit %s: %s", record.getCommit_id(), e.toString()));
                    if(e.getClass() != CommitAlreadyLoadedException.class)
                        dataset.getSkipped().put(record.getCommit_id(), (issue!=null)? issue.getKey() : null);
                }
                finally {
                    progress.step();
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
     * registers the commit in the issue object
     * @param issue
     * @param commit
     * @throws UnableToLinkIssueException
     */
    private void linkIssueCommit(Issue issue, Commit commit) throws UnableToLinkIssueException{
            
        // adds commit to issue
        issue.getCommits().add(commit);

        dataset.setNrLinkedCommits(dataset.getNrLinkedCommits() + 1);
        if (issue.getCommits().size() > 1) 
            dataset.getIssuesWithMultipleCommits().add(issue.getKey());
        
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
