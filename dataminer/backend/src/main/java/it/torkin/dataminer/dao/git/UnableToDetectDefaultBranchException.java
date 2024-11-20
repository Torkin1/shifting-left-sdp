package it.torkin.dataminer.dao.git;

public class UnableToDetectDefaultBranchException extends Exception {

    public UnableToDetectDefaultBranchException(Exception e) {
        super(e);
    }

    public UnableToDetectDefaultBranchException(String format) {
        super(format);
    }

}
