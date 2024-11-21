package it.torkin.codesmells.git;

public class UnableToInitRepoException extends Exception{

    public UnableToInitRepoException(Exception e) {
        super(e);
    }

}
