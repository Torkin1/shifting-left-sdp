package it.torkin.dataminer.dao.apachejit;

import it.torkin.dataminer.config.ApachejitConfig;

/**
 * Interfaces with ApacheJIT dataset as it was a read-only db.
 * Go see ApacheJIT documentation for more info about the dataset structure.
 */
public class ApachejitDao {

    private String commitsPath;

    public ApachejitDao(ApachejitConfig config){
        this.commitsPath = config.getCommitsPath();
    }
    
    
    /**
     * Gets all commits (clean and buggy ones)
     * @param commitsPath
     * @return
     * @throws UnableToGetCommitsException
     */
    public Resultset<CommitRecord> getAllCommits() throws UnableToGetCommitsException{
        
        try {
            return new Resultset<>(commitsPath, CommitRecord.class);
        } catch (UnableToGetResultsetException e) {
            throw new UnableToGetCommitsException(commitsPath, e);
        }           
    }   
}
