package it.torkin.dataminer.dao.git;

import lombok.Data;

@Data
public class Churn {
    private long added;
    private long deleted;

    public void addAdded(long added) {
        this.added += added;
    }

    public void addDeleted(long deleted) {
        this.deleted += deleted;
    }

    public long getTotal()  {
        return added + deleted;
    }
}
