package it.torkin.dataminer.control.dataset.raw;

import it.torkin.dataminer.entities.dataset.Issue;

public class CommitNotFoundException extends Exception{

    public CommitNotFoundException(Issue issue, String commitHash) {
        super(String.format("Unable to find commit %s referenced by issue %s", commitHash, issue.getKey()));
    }

}
