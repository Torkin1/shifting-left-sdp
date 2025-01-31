package it.torkin.dataminer.control.dataset.raw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.config.WorkersConfig;
import it.torkin.dataminer.control.dataset.raw.datasources.Datasource;
import it.torkin.dataminer.control.workers.IWorkersController;
import it.torkin.dataminer.control.workers.Task;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.git.UnableToCheckoutException;
import it.torkin.dataminer.dao.git.UnableToGetCommitDetailsException;
import it.torkin.dataminer.dao.git.UnableToGetLinkedIssueKeyException;
import it.torkin.dataminer.dao.git.UnableToInitRepoException;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.dao.jira.UnableToGetIssueException;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.dao.local.IssueDao;
import it.torkin.dataminer.dao.local.RepositoryDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.Repository;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Slf4j
@Service
public class RawDatasetController implements IRawDatasetController{
    
    @Autowired private GitConfig gitConfig;
    @Autowired private JiraConfig jiraConfig;
    @Autowired private WorkersConfig workersConfig;

    @Autowired private DatasetDao datasetDao;
    @Autowired private CommitDao commitDao;
    @Autowired private IssueDao issueDao;
    @Autowired private RepositoryDao repositoryDao;

    @Autowired private EntityMerger entityMerger;
    @Autowired private IWorkersController workers;

    @Autowired private PlatformTransactionManager transactionManager;

    private JiraDao jiraDao;

    private Map<String, GitDao> gitdaoByProject = new HashMap<>();

    private List<Commit> commits;
    private Map<String, Set<IssueEntry>> issuesByCommit = new HashMap<>();

    private ProgressBar progress;
    
    private void init(){
        jiraDao = new JiraDao(jiraConfig);
        commits = new ArrayList<>(workersConfig.getTaskBatchSize());
    }
        
    private void cleanup(){

        for(GitDao dao : gitdaoByProject.values()){
            try {
                dao.close();
            } catch (Exception e) {
                log.error("Unable to close gitDao for {}", dao.getProjectName(), e);
            }
        }
        gitdaoByProject.clear();
    }
    
    public void loadDatasource(Datasource datasource, DatasourceConfig config) throws UnableToLoadCommitsException{
        
        Dataset dataset;

        init();
        try {
            dataset = new Dataset();
            dataset.setName(config.getName());
            dataset.setLastUpdatedTime(TimeTools.now());
            dataset = datasetDao.save(dataset);
            loadCommits(datasource, dataset, config);
            dataset = datasetDao.save(dataset);
        } finally { 
            cleanup();
        }       
    }

    /**
     * For each read commit, a task is created and submitted to the workers pool
     * to process commit and linked issue details.
     * @param datasource
     * @param dataset
     * @param config
     * @throws UnableToLoadCommitsException
     */
    private void loadCommits(Datasource datasource, Dataset dataset, DatasourceConfig config) throws UnableToLoadCommitsException {
        
        progress = new ProgressBar("loading commits", config.getExpectedSize());
        Commit commit = null;
        while(datasource.hasNext()){
            try {
                commit = datasource.next();
                commit.setDataset(dataset);
                ProcessCommitBean processCommitBean = processCommit(new ProcessCommitBean(commit, dataset));
                commits.add(processCommitBean.getCommit());
                
            } catch (Exception e) {
                if (commit != null && (e instanceof UnableToFetchIssueException || e instanceof UnableToGetCommitDetailsException)){
                    handleSkippedCommit(commit, dataset, e);
                } else {
                    throw new UnableToLoadCommitsException(e); 
                }
            } 
            
            if (commits.size() == workersConfig.getTaskBatchSize() || !datasource.hasNext()){
                saveCommits();
            }
        }
        progress.close(); 
    }

    private void collectCommitsFromProcessingResults() throws Exception {
        log.debug("Collecting commits from processing results");
        while(!workers.isBatchEmpty()){
            Task<?> task = workers.collect();
            ProcessCommitBean processCommitBean = (ProcessCommitBean) task.getTaskBean();
            if (task.getException() != null){
                if (task.getException() instanceof UnableToFetchIssueException || task.getException() instanceof UnableToGetCommitDetailsException){
                    handleSkippedCommit(processCommitBean.getCommit(), processCommitBean.getDataset(), task.getException());
                } else {
                    throw task.getException();
                }
            } else {
                commits.add(processCommitBean.getCommit());
            }
        }
    }

    private void saveCommits() {

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            log.debug("saving commit batch");
            saveIssues();

            for (Commit commit : commits){
                Repository repository = commit.getRepository();
                commit.setRepository(repositoryDao.save(repository));
            }
            commits = commitDao.saveAll(commits);
            log.debug("Commit batch saved");
            commits.clear();
        });


    }

    private void linkIssueCommit(Issue issue, Commit commit) {
        issue.getCommits().add(commit);
        commit.getIssues().add(issue);
    }

    private void saveIssues() {        
                
        for (Commit commit: commits){
            Set<IssueEntry> commitIssues = issuesByCommit.get(commit.getHash());
            Issue issue;
            for (IssueEntry entry : commitIssues){
                    entityMerger.mergeIssueDetails(entry.getIssue().getDetails());
                    issue = issueDao.save(entry.getIssue());
                linkIssueCommit(issue, commit);
            }  
        }
        issuesByCommit.clear();
    }

    private void handleSkippedCommit(Commit commit, Dataset dataset, Exception cause){
                
        log.debug("Skipping commit {} of project {} from dataset {}: {}",
        commit.getHash(), commit.getRepository(), dataset.getName(), cause.toString());
        dataset.setSkipped(dataset.getSkipped() + 1);

        if (cause instanceof UnableToFetchIssueException){
            try {
                if (log.isDebugEnabled()){
                    log.debug("No linkable issue found in commit comment: {}", getGitdaoByProject(commit.getRepository().getId()).getCommitMessage(commit.getHash()));
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        dataset.getUnlinkedByRepository().compute(commit.getRepository().getId(),
            (project, count) -> count == null ? 1 : count + 1);
        if (commit.isBuggy()){
            dataset.getBuggyUnlinkedByRepository().compute(commit.getRepository().getId(),
                (project, count) -> count == null ? 1 : count + 1);
        }
    }

    private ProcessCommitBean processCommit(ProcessCommitBean task) throws UnableToInitRepoException, UnableToGetCommitDetailsException, UnableToFetchIssueException, UnableToCheckoutException {
        try {
            fillCommitDetails(task.getCommit());
            getCommitIssues(task.getCommit());
        } finally {
            progress.step();
        }
        return task;
    }

    private void fillCommitDetails(Commit commit) throws UnableToInitRepoException, UnableToGetCommitDetailsException, UnableToCheckoutException {

        GitDao gitDao;

        gitDao = getGitdaoByProject(commit.getRepository().getId());
        gitDao.getCommitDetails(commit);
        commit.getMeasurement().setMeasurementDate(commit.getTimestamp());
        commit.getMeasurement().setMeasurementDateName("CommitDate");
        commit.getMeasurement().setDataset(commit.getDataset());
    }

    private void getCommitIssues(Commit commit) throws UnableToFetchIssueException {
        List<Issue> fetched;
        
        fetched = fetchLinkedIssues(commit);
        synchronized (issuesByCommit){
            issuesByCommit.putIfAbsent(commit.getHash(), new HashSet<>());
            for (Issue issue : fetched){
                issuesByCommit.get(commit.getHash()).add(new IssueEntry(issue, issue.getKey()));
            }
        }

    }

    private void linkIssueDetails(Issue issue, JiraDao jiraDao) throws UnableToLinkIssueDetailsException {
        try {
            IssueDetails details = jiraDao.queryIssueDetails(issue.getKey());
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
    private List<Issue> fetchLinkedIssues(Commit commit) throws UnableToFetchIssueException {
        List<Issue> issues = new ArrayList<>();
        Issue issue;
        GitDao gitDao;
        List<String> issueKeys;

        try {
            gitDao = getGitdaoByProject(commit.getRepository().getId());
            issueKeys = gitDao.getLinkedIssueKeysByCommit(commit.getHash());
            for (String issueKey : issueKeys){
                try{
                    issue = new Issue();
                    issue.setKey(issueKey);
                    linkIssueDetails(issue, jiraDao);
                    issues.add(issue);
                } catch (UnableToLinkIssueDetailsException e) {
                    log.debug("Unable to link issue details for issue {}: {}", issueKey, e.toString());
                }
            }
            if (issues.isEmpty()){
                throw new UnableToFetchIssueException(commit.getHash(), "no issues found for given key set: " + issueKeys.toString());
            }
            return issues;
        } catch (UnableToInitRepoException | UnableToGetLinkedIssueKeyException | UnableToCheckoutException e) {
            throw new UnableToFetchIssueException(commit.getHash(), e);
        }
    }

    synchronized private GitDao getGitdaoByProject(String project) throws UnableToInitRepoException, UnableToCheckoutException{
        
        if(!gitdaoByProject.containsKey(project)){
            GitDao gitDao = new GitDao(gitConfig, project);
            gitDao.checkout();
            gitdaoByProject.put(project, gitDao);
        }
        return gitdaoByProject.get(project);
    }

    
}
