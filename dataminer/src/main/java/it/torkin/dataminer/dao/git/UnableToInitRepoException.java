package it.torkin.dataminer.dao.git;

import java.io.IOException;

public class UnableToInitRepoException extends Exception{

    public UnableToInitRepoException(Exception e) {
        super(e);
    }

}
