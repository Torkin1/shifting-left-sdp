package it.torkin.dataminer.dao.apachejit;

public class UnableToGetCommitsException extends Exception{

    public UnableToGetCommitsException(String commitsPath, UnableToGetResultsetException e) {
        super(String.format("Unable to load commits from %s", commitsPath), e);
    }

}
