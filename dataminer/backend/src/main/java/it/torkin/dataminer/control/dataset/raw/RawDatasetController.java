package it.torkin.dataminer.control.dataset.raw;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.dao.datasources.Datasource;
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
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Slf4j
@Service
public class RawDatasetController implements IRawDatasetController{
    
    @Autowired private DatasourceGlobalConfig datasourceGlobalConfig;
    @Autowired private JiraConfig jiraConfig;
    @Autowired private GitConfig gitConfig;

    @Autowired private DatasetDao datasetDao;
    @Autowired private CommitDao commitDao;
    @Autowired private IssueDao issueDao;

    @Autowired private EntityMerger entityMerger;

    private JiraDao jiraDao;

    private List<Datasource> datasources = new ArrayList<>();
    private Map<String, GitDao> gitdaoByProject = new HashMap<>();        
        
    public void createRawDataset() throws UnableToCreateRawDatasetException {
               
        Datasource datasource;
        DatasourceConfig config;

        jiraDao = new JiraDao(jiraConfig);

        try (ProgressBar progress = new ProgressBar("loading datasources", datasources.size())) {
            
            prepareDatasources();
            
            for (int i = 0; i < datasources.size(); i++) {
                
                datasource = datasources.get(i);
                config = datasourceGlobalConfig.getSources().get(i);
                progress.setExtraMessage(config.getName());

                if(!datasetDao.existsByName(config.getName())){
                    log.info("loading datasource {}", config.getName());
                    loadDatasource(datasource, config);
                }
                else {
                    log.warn("Datasource {} already exists in the database. Skipping.", config.getName());
                }

                datasource.close();
                progress.step();

            }
        } catch (UnableToPrepareDatasourceException | UnableToLoadCommitsException e) {
            throw new UnableToCreateRawDatasetException(e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            if (e instanceof RuntimeException)
                throw new UnableToCreateRawDatasetException(e);
        }


    }

    @Transactional
    private void loadDatasource(Datasource datasource, DatasourceConfig config) throws UnableToLoadCommitsException{
        
        Dataset dataset;
        
        dataset = new Dataset();
        dataset.setName(config.getName());
        datasetDao.save(dataset);    
        loadCommits(datasource, dataset, config);
}

    private void loadCommits(Datasource datasource, Dataset dataset, DatasourceConfig config) throws UnableToLoadCommitsException {
        
        Commit commit;
        try (ProgressBar progress = new ProgressBar("loading commits", config.getExpectedSize())){
            while(datasource.hasNext()){
                commit = datasource.next();
                try {
                    fillCommitDetails(commit);
                    linkCommitIssues(commit);
                    commit.setDataset(dataset);
                    commit = commitDao.save(commit);
                } catch (UnableToInitRepoException e) {
                    throw new UnableToLoadCommitsException(e);
                } catch (UnableToFetchIssueException | UnableToGetCommitDetailsException e) {
                    handleSkippedCommit(commit, dataset, e);
                }
                progress.step();
                
            }       
        }
    }

    private void handleSkippedCommit(Commit commit, Dataset dataset, Exception cause){
                
        log.warn("Skipping commit {} of project {} from dataset {}: {}",
        commit.getHash(), commit.getProject(), dataset.getName(), cause.toString());
        dataset.setSkipped(dataset.getSkipped() + 1);

        if (cause instanceof UnableToFetchIssueException)
            dataset.getUnlinkedByProject().compute(commit.getProject(),
             (project, count) -> count == null ? 1 : count + 1);
    }

    private void fillCommitDetails(Commit commit) throws UnableToInitRepoException, UnableToGetCommitDetailsException {

        GitDao gitDao;

        gitDao = getGitdaoByProject(commit.getProject());
        gitDao.getCommitDetails(commit);
    }

    private void linkCommitIssues(Commit commit) throws UnableToFetchIssueException {
        List<Issue> issues;
        
        issues = fetchLinkedIssues(commit);

        for (Issue issue : issues){
            issue.getCommits().add(commit);
            commit.getIssues().add(issue);
        }

    }

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
    private List<Issue> fetchLinkedIssues(Commit commit) throws UnableToFetchIssueException {
        List<Issue> issues = new ArrayList<>();
        Issue issue;
        GitDao gitDao;
        List<String> issueKeys;

        try {
            gitDao = getGitdaoByProject(commit.getProject());
            issueKeys = gitDao.getLinkedIssueKeysByCommit(commit.getHash());
            for (String issueKey : issueKeys){
                issue = issueDao.findByKey(issueKey);
                if (issue == null){
                    issue = new Issue(issueKey);
                    linkIssueDetails(issue, jiraDao);
                    issue = issueDao.save(issue);
                }
                issues.add(issue);
            }
            return issues;
        } catch (UnableToInitRepoException | UnableToGetLinkedIssueKeyException | UnableToLinkIssueDetailsException e) {
            throw new UnableToFetchIssueException(commit.getHash(), e);
        }
    }

    private GitDao getGitdaoByProject(String project) throws UnableToInitRepoException{
        if(!gitdaoByProject.containsKey(project))
            gitdaoByProject.put(project, new GitDao(gitConfig, project));
        return gitdaoByProject.get(project);
    }

    /**
     * Assures that
     * all specified datasources are accessible and ready to be mined.
     * @throws UnableToPrepareDatasourceException 
     */
    private void prepareDatasources() throws UnableToPrepareDatasourceException{

        Datasource datasource;
        
        for (DatasourceConfig config : datasourceGlobalConfig.getSources()) {

            
            try {
                datasource = findDatasourceImpl(config);
                datasource.init(config);
                datasources.add(datasource);
            } catch (UnableToFindDatasourceImplementationException | UnableToInitDatasourceException e) {
                throw new UnableToPrepareDatasourceException(config.getName(), e);
            }

        }

    }
    
    /**
     * Gets the implementation of the datasource specified in the config using
     * reflection.
     * @param config
     * @return
     * @throws UnableToFindDatasourceImplementationException
     */
    private Datasource findDatasourceImpl(DatasourceConfig config) throws UnableToFindDatasourceImplementationException {

        String implName;
        Datasource datasource;

            try {
                implName = implNameFromConfig(config);
                datasource = (Datasource) Class.forName(implName).getDeclaredConstructor().newInstance();
                return datasource;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException e) {
                throw new UnableToFindDatasourceImplementationException(config.getName(), e);
            }

    }

    private String implNameFromConfig(DatasourceConfig config) {
        return String.format("%s.%s%s", datasourceGlobalConfig.getImplPackage(),
         config.getName().substring(0, 1).toUpperCase(), config.getName().substring(1));
    }
}
