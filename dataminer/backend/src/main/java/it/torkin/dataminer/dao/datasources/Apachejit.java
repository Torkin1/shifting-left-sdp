package it.torkin.dataminer.dao.datasources;

import java.time.Year;
import java.util.Map;

import org.joda.time.Instant;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Feature;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.csv.UnableToGetResultsetException;
import it.torkin.dataminer.toolbox.string.BooleanReader;
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
        Measurement measurement;
        BooleanReader booleanReader = new BooleanReader("True", "False");
        
        record = records.next();
        commit = new Commit();
        measurement = new Measurement();

        record.forEach((k,v) -> {
                                    
            switch(k){
                case "commit_id":
                    commit.setHash(v);
                    break;
                case "buggy":
                    commit.setBuggy(booleanReader.read(v));
                    break;
                case "project":
                    commit.setProject(v);
                    break;
                case "fix":
                    measurement.getFeatures().add(new Feature(k, booleanReader.toString(v), Boolean.class));
                    break;
                case "year":
                    measurement.getFeatures().add(new Feature(k, v, Year.class));
                    break;
                case "author_date":
                    measurement.getFeatures().add(new Feature(k, v, Instant.class));
                    break;
                case "la":
                case "ld":
                case "nf":
                case "nd":
                case "ns":
                    measurement.getFeatures().add(new Feature(k, v, Integer.class));
                    break;
                case "ent":
                case "ndev":
                case "age":
                case "nuc":
                case "aexp":
                case "arexp":
                case "asexp":
                    measurement.getFeatures().add(new Feature(k, v, Double.class));
                    break;
                default:
                    log.debug("Unknown feature from apachejit: {}={}", k, v);
                    break;
            }
        });

        commit.setMeasurement(measurement);
        measurement.setCommit(commit);
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
