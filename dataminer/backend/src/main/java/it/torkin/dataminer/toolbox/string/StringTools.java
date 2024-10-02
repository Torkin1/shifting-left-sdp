package it.torkin.dataminer.toolbox.string;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringTools {

    public boolean isBlank(String string){
        return string == null || string.trim().isEmpty();
    }
    
}
