package it.torkin.dataminer.dao.git;

public class UnableToGetChangesetException extends Exception{

    public UnableToGetChangesetException(Exception e) {
        super(e);
    }

}
