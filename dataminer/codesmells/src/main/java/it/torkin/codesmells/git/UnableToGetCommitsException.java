package it.torkin.codesmells.git;

public class UnableToGetCommitsException extends Exception{

    public UnableToGetCommitsException(Exception e){
        super(e);
    }

}
