package it.torkin.dataminer.dao.git;

import java.io.IOException;

public class UnableToDoDiffException extends Exception{

    public UnableToDoDiffException(IOException e) {
        super(e);
    }

}
