package it.torkin.dataminer.control.dataset.raw;

public class UnableToFetchIssueException extends Exception{

    public UnableToFetchIssueException(String commit, Exception e) {
        super(String.format("unable to fetch issue for commit %s: %s", commit, e.getMessage()), e);
    }

    public UnableToFetchIssueException(String commit, String cause) {
        super(String.format("unable to fetch issue for commit %s: %s", commit, cause));
    }

}
