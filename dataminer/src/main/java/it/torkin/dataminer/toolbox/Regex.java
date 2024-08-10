package it.torkin.dataminer.toolbox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Regex {

    /**
     * Find the first occurrence of a regular expression in a string
     * @param regexp
     * @param haystack
     * @return
     * @throws NoMatchFoundException 
     */
    public String findFirst(String regexp, String haystack) throws NoMatchFoundException{
        Pattern pattern;
        Matcher matcher;
        String needle;

        pattern = Pattern.compile(regexp);
        matcher = pattern.matcher(haystack);

        if (matcher.find()){
            needle = matcher.group();
            return needle;
        }

        throw new NoMatchFoundException(regexp, haystack);


    }
    
}
