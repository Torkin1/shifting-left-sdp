package it.torkin.dataminer.toolbox.string;

import lombok.Data;

/**
 * Reads a string and interprets it as a boolean value according
 * to the mapping given by user
 */
@Data
public class BooleanReader {

    private final String trueString;
    private final String falseString;

    /**
     * If value is not equal to neither trueString nor falseString,
     * the method returns null 
     * @return
     */
    public Boolean read(String booleanString){
        if (booleanString.equals(trueString)){
            return true;
        } else if (booleanString.equals(falseString)){
            return false;
        } else {
            return null;
        }
    }

    /**
     * Converts a boolean string into a standard format
     * @param booleanString
     * @return
     */
    public String toString(String booleanString){
        return Boolean.toString(read(booleanString));
    }

}
