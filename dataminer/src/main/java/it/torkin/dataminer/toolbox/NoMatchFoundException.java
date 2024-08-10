package it.torkin.dataminer.toolbox;

public class NoMatchFoundException extends Exception{

    public NoMatchFoundException(String regexp, String haystack) {
        super(String.format("No match found using regexp %s in string %s", regexp, haystack));
    }

}
