package it.torkin.codesmells.git;

import java.io.File;

public class UnableToCloneRepoException extends Exception{

    public UnableToCloneRepoException(Exception e, String remote, File local) {
        super(String.format("unable to clone repo from %s to %s", local.getAbsolutePath(), remote), e);
    }

}
