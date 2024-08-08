package it.torkin.dataminer.dao.git;

public class IssueNotFoundException extends Exception{

    public IssueNotFoundException(String commitHash) {
        super(String.format("Unable to find issue for commit %s: ", commitHash));
    }

}
