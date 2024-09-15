package it.torkin.dataminer.toolbox.csv;

import java.io.File;

public class UnableToGetResultsetException extends Exception{

    public UnableToGetResultsetException(File datafile, Exception e) {
        super(String.format("can't get result set from file %s", datafile.getAbsolutePath()), e);
    }

}
