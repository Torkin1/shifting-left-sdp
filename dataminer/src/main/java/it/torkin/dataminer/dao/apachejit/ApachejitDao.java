package it.torkin.dataminer.dao.apachejit;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Interfaces with ApacheJIT dataset as it was a read-only db.
 * Go see ApacheJIT documentation for more info about the dataset structure.
 */
public class ApachejitDao {

    /**
     * Gets all commits (clean and buggy ones)
     * @param commitsPath
     * @return
     * @throws UnableToGetCommitsException
     */
    public Resultset<CommitRecord> getAllCommits(String commitsPath) throws UnableToGetCommitsException{
        
        try {
            return new Resultset<>(commitsPath, CommitRecord.class);
        } catch (UnableToGetResultsetException e) {
            throw new UnableToGetCommitsException(commitsPath, e);
        }           
    }

    /**
     * Return list of .csv containing issue data
     * @param issuesFolderPath
     * @return
     */
    private File[] listIssueDataFiles(String issuesFolderPath){

        File issuesFolder = new File(issuesFolderPath);
        File[] data_files = issuesFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");
            }
        });

        return data_files;
    }
    
    /**
     * Gets all issues for each project
     * @param issuesFolderPath
     * @return
     * @throws UnableToGetIssuesException
     */
    public List<Resultset<IssueRecord>> getAllIssues(String issuesFolderPath) throws UnableToGetIssuesException{
        
        List<Resultset<IssueRecord>> issues = new ArrayList<>();
        File[] data_files = listIssueDataFiles(issuesFolderPath);

        for (File data_file : data_files) {
            try {
                issues.add(new Resultset<>(data_file, IssueRecord.class));
            } catch (UnableToGetResultsetException e) {
                throw new UnableToGetIssuesException(data_file, e);
            }
        }

        return issues;

    }
    
}
