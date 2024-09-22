package it.torkin.dataminer.dao.datasources;

import java.time.Year;
import java.util.Map;

import org.joda.time.Instant;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Feature;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        record.forEach((k,v) -> {
                                    
            switch(k){
                case "commit_id":
                    commit.setHash(v);
                    break;
                case "buggy":
                    commit.setBuggy(v.equals("True"));
                    break;
                case "project":
                    commit.setProject(v);
                    break;
                case "fix":
                    // positive value could be different from the "true" string we expect,
                    // so we parse it first and then convert it to a string again to obtain the standard "true" string
                    commit.getFeatures().add(new Feature(k, Boolean.toString(v.equals("True")), Boolean.class));
                    break;
                case "year":
                    commit.getFeatures().add(new Feature(k, v, Year.class));
                    break;
                case "author_date":
                    commit.getFeatures().add(new Feature(k, v, Instant.class));
                    break;
                case "la":
                case "ld":
                case "nf":
                case "nd":
                case "ns":
                    commit.getFeatures().add(new Feature(k, v, Integer.class));
                    break;
                case "ent":
                case "ndev":
                case "age":
                case "nuc":
                case "aexp":
                case "arexp":
                case "asexp":
                    commit.getFeatures().add(new Feature(k, v, Double.class));
                    break;
                default:
                    log.debug("Unknown feature from apachejit: {}={}", k, v);
                    break;
            }
        });

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
