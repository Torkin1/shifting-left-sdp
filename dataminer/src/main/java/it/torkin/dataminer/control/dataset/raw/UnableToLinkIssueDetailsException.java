package it.torkin.dataminer.control.dataset.raw;

import it.torkin.dataminer.dao.jira.UnableToGetIssueException;

public class UnableToLinkIssueDetailsException extends Exception {

    public UnableToLinkIssueDetailsException(UnableToGetIssueException e) {
        super(e);
    }

}
