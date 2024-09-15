package it.torkin.dataminer.control.dataset.raw;

public class CommitAlreadyLoadedException extends Exception{

    public CommitAlreadyLoadedException(String commit_id, boolean refresh) {
        super(String.format("Commit %s already loaded in dataset and refresh=%s", commit_id, refresh));
    }

}
