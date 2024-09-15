package it.torkin.dataminer.dao.jira;

public class UnableToGetIssueException extends Exception {

    public UnableToGetIssueException(Throwable cause, String hostname, String key) {
        super(String.format("unable to get issue %s from %s", key, hostname ), cause);
    }

}
