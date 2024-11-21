package it.torkin.codesmells.git;

import java.io.File;

public class CloneInNonEmptyDirException extends Exception{

    public CloneInNonEmptyDirException(File localDir) {
        super(String.format("attempting to clone repo in non empty dir at %s", localDir.getAbsolutePath()));
    }

}
