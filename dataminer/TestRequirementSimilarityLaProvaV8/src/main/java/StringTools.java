
import java.util.regex.Pattern;

public class StringTools {

    public static boolean isBlank(String string){
        return string == null || string.trim().isEmpty();
    }

    public static String stripFilenameExtension(String filename){
        return filename.split(Pattern.quote("."))[0];
    }
    
}
