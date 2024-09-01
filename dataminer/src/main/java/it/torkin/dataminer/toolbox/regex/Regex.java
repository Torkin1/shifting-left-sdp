package it.torkin.dataminer.toolbox.regex;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex implements Iterable<String> {

    private Pattern pattern;
    private Matcher matcher;
    
    boolean hasNext;

    public Regex(String regex, String haystack) {

        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(haystack);
        hasNext = matcher.find();
    }
    
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return hasNext;
                
            }

            @Override
            public String next() {

                String needle;

                needle = matcher.group();
                hasNext = matcher.find();

                return needle;
            }
        };
    }


}