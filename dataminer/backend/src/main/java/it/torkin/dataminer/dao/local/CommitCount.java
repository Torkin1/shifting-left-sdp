package it.torkin.dataminer.dao.local;

public interface CommitCount {

    public String getDataset();
    public String getRepository();
    public String getProject();
    public Long getTotal(); 

}
