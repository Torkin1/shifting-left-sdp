package it.torkin.dataminer.control.dataset.raw;

public class UnableToFindDatasourceImplementationException extends Exception{

    public UnableToFindDatasourceImplementationException(String datasource, Exception e) {
        super(String.format("unable to prepare implementation for datasource %s: %s", datasource, e.getMessage()), e);
    }

}
