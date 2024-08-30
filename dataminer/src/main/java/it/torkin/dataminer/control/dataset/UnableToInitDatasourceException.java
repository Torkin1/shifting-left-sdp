package it.torkin.dataminer.control.dataset;

import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;

public class UnableToInitDatasourceException extends Exception{

    public UnableToInitDatasourceException(UnableToGetResultsetException e) {
        super(e);
    }

}
