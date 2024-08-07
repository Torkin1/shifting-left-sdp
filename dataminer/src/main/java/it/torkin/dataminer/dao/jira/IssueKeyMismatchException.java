package it.torkin.dataminer.dao.jira;

public class IssueKeyMismatchException extends Exception{

    public IssueKeyMismatchException(String expected, String actual) {
        super(String.format("Issue retrieved with key %s does not match requested key %s", actual, expected));
    }

}
