package it.torkin.dataminer.control.dataset;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.ApachejitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.dao.apachejit.ApachejitDao;
import it.torkin.dataminer.dao.apachejit.Resultset;
import it.torkin.dataminer.dao.apachejit.CommitRecord;
import it.torkin.dataminer.dao.apachejit.IssueRecord;
import it.torkin.dataminer.dao.apachejit.UnableToGetCommitsException;
import it.torkin.dataminer.dao.apachejit.UnableToGetIssuesException;
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
    
    private void loadIssues(ApachejitDao apachejitDao, JiraDao jiraDao) throws UnableToLoadIssuesException {
        /**
         * - get the path of the issues folder from the configuration
         * - list all csvs in the folder
         * - for each csv
         *  - load each issue record from csv
         *  - for each record
         *   - transform record into an Issue object
         *   - fetch issue details from Jira API
         *   - store IssueDetails reference in Issue
         *   - load Commit matching the hash commit in Issue
         *   - store Commit reference in Issue
         *   - save Issue in the database
        */
        
        List<Resultset<IssueRecord>> issues;        
        Issue issue;
        IssueRecord record;
        int skipped;

        try {
            issues = apachejitDao.getAllIssues(apachejitConfig.getIssuesPath());

            for (int i = 0; i < issues.size(); i++) {
                try (Resultset<IssueRecord> projectIssues = issues.get(i)) {
                    while (projectIssues.hasNext()) {
                        
                        record = projectIssues.next();
                        dataset.setNrIssueRecords(dataset.getNrIssueRecords() + 1);
                        // skip commit if it is already in db, but only if
                         // we do not have to refresh the db
                        if (!issueDao.existsByKey(record.getIssue_key())
                            || apachejitConfig.isRefresh()) {
                            try {
                                issue = new Issue();
                                issue.setKey(record.getIssue_key());
                                linkIssueDetails(issue, jiraDao);
                                linkCommit(issue, record.getCommit_id());
                                mergeObjectsEntity(issue);
                                issueDao.save(issue);
                            } catch (UnableToLinkIssueDetailsException | CommitNotFoundException e) {
                                log.warn(String.format("Skipping issue %s: %s", record.getIssue_key(), e.getMessage()));
                                dataset.getSkippedIssuesKeys().add(record.getIssue_key());
                            }
                        } 
                    }    
                    
                } 
            }
            skipped = dataset.getSkippedIssuesKeys().size();
            if (skipped > 0) {
                log.warn(String.format("Skipped %d issues", skipped));
            }
        } catch (UnableToGetIssuesException | IOException e) {
        
            throw new UnableToLoadIssuesException(e);
        }
    }

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
            
        } catch (UnableToGetIssueException e) {
            throw new UnableToLinkIssueDetailsException(e);
        }
    }

    private void linkCommit(Issue issue, String commitHash) throws CommitNotFoundException {

        Commit commit = commitDao.findByHash(commitHash);
        if (commit == null){
            throw new CommitNotFoundException(issue, commitHash);
        }
        issue.getCommits().add(commit);
    }

    private void loadCommits(ApachejitDao apachejitDao) throws UnableToLoadCommitsException {
        /**
         * - get the path of the commits file from the configuration
         * - for each commit
         *  - load commit record from csv
         *  - transform it into a Commit object
         *  - save it in the database
         */

        CommitRecord record;
        Commit commit;

        try (Resultset<CommitRecord> commits = apachejitDao.getAllCommits(apachejitConfig.getCommitsPath())) {

            while (commits.hasNext()) {

                record = commits.next();
                // skip commit if it is already in db, but only if
                // we do not have to refresh the db
                if(!commitDao.existsByHash(record.getCommit_id())
                 || apachejitConfig.isRefresh()){
                    commit = new Commit(record);
                    commitDao.save(commit);
                    dataset.setNrCommits(dataset.getNrCommits() + 1);
                 }
            }

        } catch (UnableToGetCommitsException | IOException e) {
            throw new UnableToLoadCommitsException(e);
        }
        
    }

    /*
     * Loads apachejit dataset from the filesystem into the local db,
     * fetching issue details from Jira API
     * 
     * @return a @Code Dataset entity with some stats about the dataset 
     */
    @Override
    public void loadDataset() throws UnableToLoadDatasetException {
                
        ApachejitDao apachejitDao;
        JiraDao jiraDao;

        if(apachejitConfig.isSkipLoad()) return;

        try {
                
            if(dataset == null){
                dataset = new Dataset();
                dataset.setName("apachejit");
            }

            apachejitDao = new ApachejitDao();
            jiraDao = new JiraDao(
                jiraConfig.getHostname(),
                jiraConfig.getApiVersion());
            
            loadCommits(apachejitDao);
            loadIssues(apachejitDao, jiraDao);
            datasetDao.save(dataset);

        } catch (UnableToLoadCommitsException | UnableToLoadIssuesException e) {
            dataset = null;
            throw new UnableToLoadDatasetException(e);
        }
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }
    
}
