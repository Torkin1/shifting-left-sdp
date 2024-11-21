package it.torkin.codesmells.git;

public class UnableToGetLinkedIssueKeyException extends Exception{

    public UnableToGetLinkedIssueKeyException(String hash, String repoName, Exception e) {
        super(String.format("Unable to get linked issue key from commit %s in repository %s: %s", hash, repoName, e.toString()) ,e);
    }

}
