package it.torkin.dataminer.control.dataset;

public class UnableToPrepareDatasourceException extends Exception{

    public UnableToPrepareDatasourceException(String name, Exception e) {
        super(String.format("Unable to prepare datasource %s", name), e);
    }

}
