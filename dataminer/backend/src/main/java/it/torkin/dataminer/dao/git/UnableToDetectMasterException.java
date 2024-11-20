package it.torkin.dataminer.dao.git;

public class UnableToDetectMasterException extends Exception {

    public UnableToDetectMasterException(Exception e) {
        super(e);
    }

    public UnableToDetectMasterException(String format) {
        super(format);
    }

}
