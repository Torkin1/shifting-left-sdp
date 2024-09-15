package it.torkin.dataminer.dao.datasources;

import java.util.Map;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;

public class Apachejit implements Datasource{
    
    private Resultset<Map<String, String>> records;
    private String commitsPath;
    
    @Override
    public boolean hasNext() {
        return records.hasNext();
    }

    @Override
    public Commit next() {
        Map<String, String> record;
        Commit commit;
        
        record = records.next();
        commit = new Commit();

        commit.setHash(record.get("commit_id"));
        commit.setBuggy(record.get("buggy").equals("True"));
        commit.setProject(record.get("project"));

        return commit;
        
    }

    @Override
    public void close() throws Exception {
        records.close();
    }

    @Override
    public void init(DatasourceConfig config) throws UnableToInitDatasourceException {

        commitsPath = String.format("%s/%s", config.getPath(), "dataset/apachejit_total.csv");
        
        try {
            records = new Resultset<>(commitsPath, Map.class);
        } catch (UnableToGetResultsetException e) {
            throw new UnableToInitDatasourceException(e);
        }           
    }


    
}
