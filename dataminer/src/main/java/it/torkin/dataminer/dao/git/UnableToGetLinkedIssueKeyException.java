package it.torkin.dataminer.dao.git;

import java.io.File;

public class UnableToGetLinkedIssueKeyException extends Exception{

    public UnableToGetLinkedIssueKeyException(String hash, File localDir, Exception e) {
        super(String.format("Unable to get linked issue key from commit %s in repository %s", hash, localDir.getName()) ,e);
    }

}
