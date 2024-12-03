package it.torkin.dataminer.rest;

public class UnableToGetResourceException extends Exception {
    public UnableToGetResourceException(Exception cause){
        super(cause);
    }

    public UnableToGetResourceException(Exception e, String resourceUrl, String resourceTypeName) {
        super(String.format("Unable to get resource of type %s at %s", resourceTypeName, resourceUrl), e);
    }

    public UnableToGetResourceException(String resourceUrl, String resourceTypeName) {
        super(String.format("Unable to get resource of type %s at %s", resourceTypeName, resourceUrl));
    }
}
