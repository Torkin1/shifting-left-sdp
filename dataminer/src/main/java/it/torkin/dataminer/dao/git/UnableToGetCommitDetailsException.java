package it.torkin.dataminer.dao.git;

public class UnableToGetCommitDetailsException extends Exception{

    public UnableToGetCommitDetailsException(String hash, UnableToGetCommitException e) {
        super(String.format("unable to get commit details for commit %s: %s", hash, e.getMessage()),e);
    }

}
