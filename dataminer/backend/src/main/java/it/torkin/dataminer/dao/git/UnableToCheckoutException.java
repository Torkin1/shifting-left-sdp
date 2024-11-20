package it.torkin.dataminer.dao.git;

import org.eclipse.jgit.api.CheckoutResult;

public class UnableToCheckoutException extends Exception{

    public UnableToCheckoutException(CheckoutResult.Status status) {
        super(String.format("checkout status is %s", status.toString()));
    }
    
    public UnableToCheckoutException(Exception e) {
        super(e);
    }

    public UnableToCheckoutException(String format) {
        super(format);
    }

}
