package it.torkin.dataminer.toolbox.string;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringTools {

    public boolean isBlank(String string){
        return string == null || string.trim().isEmpty();
    }

    public String stripFilenameExtension(String filename){
        return filename.split(Pattern.quote("."))[0];
    }
    
}
