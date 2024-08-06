package it.torkin.dataminer.dao.apachejit;

import java.io.File;

public class UnableToGetIssuesException extends Exception {

    public UnableToGetIssuesException(File data_file, Exception e) {
        super(String.format("Unable to load issues from %s", data_file), e);
    }

}
