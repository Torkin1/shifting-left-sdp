package it.torkin.codesmells.git;

public class UnableToGetCommitException extends Exception{

    public UnableToGetCommitException(String hash, Exception e) {
        super(String.format("unable to get commit %s", hash),e);
    }

}
