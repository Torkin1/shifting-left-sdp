package it.torkin.dataminer.rest;

public class UnableToPostResourceException extends Exception {

    public UnableToPostResourceException(Exception e, String resourceUrl) {
        super(String.format("Unable to post resource at %s", resourceUrl), e);
    }

}
