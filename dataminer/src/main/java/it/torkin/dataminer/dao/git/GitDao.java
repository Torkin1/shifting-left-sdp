package it.torkin.dataminer.dao.git;

public class GitDao {

    /** TODO: stub 
     * @throws IssueNotFoundException */
    public String getLinkedIssueByCommit(String commitHash) throws IssueNotFoundException {
        
        String issuekey = "PROJ-123";

        if (issuekey == null) throw new IssueNotFoundException(commitHash);

        return issuekey;

    }
    
}
