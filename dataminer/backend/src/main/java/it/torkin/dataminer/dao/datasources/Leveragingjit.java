package it.torkin.dataminer.dao.datasources;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Feature;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.toolbox.csv.CsvSchemaBean;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.string.BooleanReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Leveragingjit implements Datasource{
    
    private Queue<Resultset<Map<String, String>>> records = new LinkedList<>();
    
    private static String toRepo(String project){
        StringBuilder builder = new StringBuilder();
        String repoName = null;

        builder.append("apache/");

        switch (project) {
            case "ARTEMIS":
                repoName = "activemq-artemis";
                break;
            case "DIRSERVER":
                repoName = "directory-server";
                break;
            case "GROOVY":
                repoName = "groovy";
                break;
            case "MNG":
                repoName = "maven";
                break;
            case "NUTCH":
                repoName = "nutch";
                break;
            case "OPENJPA":
                repoName = "openjpa";
                break;
            case "QPID":
                repoName = "qpid";
                break;
            case "TIKA":
                repoName = "tika";
                break;
            case "ZOOKEEPER":
                repoName = "zookeeper";     
                break;
            default:
                log.error("No registered remote repository for project {} of datasource LeveragingJIT", project);
                break;
        }
        builder.append(repoName);
        return builder.toString();
    }

    private List<File> getDatafiles(DatasourceConfig config){

        List<File> datafiles = new LinkedList<>();
        
        Stream.of(new File(config.getPath()).listFiles())
         .filter(file -> file.getName().endsWith(".csv"))
         .forEach(file -> datafiles.add(file));

        return datafiles;
    }
    
    @Override
    public boolean hasNext() {

        if (!records.isEmpty()) {
            return records.peek().hasNext();
        }
        return false;
    }

    @Override
    public Commit next() {

        Map<String, String> record;
        Commit commit = new Commit();
        Measurement measurement = new Measurement();
        BooleanReader booleanReader = new BooleanReader("YES", "NO");

        record = records.peek().next();
        record.forEach((k, v) -> {

            switch (k) {
                case "Project":
                    commit.setRepository(toRepo(v));
                    break;
                case "Commit":
                    commit.setHash(v);
                    break;
                case "Actual_Defective":
                    commit.setBuggy(booleanReader.read(v));
                    break;
                case "Release":
                    /**
                     * Storing release as a string to allow Ordinal scale operations
                     * only.
                     */
                    measurement.getFeatures().add(new Feature(k, v, String.class));
                    break;
                case "NS":
                case "ND":
                case "NF":
                case "Size":
                case "LA":
                case "LD":
                case "LT":
                case "NDEV":
                case "AGE":
                case "NUC":
                case "EXP":
                case "SEXP":
                    measurement.getFeatures().add(new Feature(k, v, Integer.class));
                    break;
                case "Entropy":
                case "REXP":
                    measurement.getFeatures().add(new Feature(k, v, Double.class));
                    break;
                case "FIX":
                    measurement.getFeatures().add(new Feature(k, booleanReader.toString(v), Boolean.class));
                    break;
                default:
                    break;
            }
        });

        /**
         * Change data file if the current one has been fully read
         */
        if (!records.peek().hasNext()){
            try{
                records.poll().close();
            } catch (Exception e){
                log.error("Unable to close resultset", e);
            }
        }

        commit.setMeasurement(measurement);
        measurement.setCommit(commit);
        return commit;
        
    }

    @Override
    public void close() throws Exception {
        for (Resultset<Map<String, String>> set : records) {
            set.close();
        }
    } 

    @Override
    public void init(DatasourceConfig config) throws UnableToInitDatasourceException {

        CsvSchemaBean bean = new CsvSchemaBean();
        bean.setColumnSeparator(';');
        
        for (File datafile : getDatafiles(config)) {
            try {
                records.add(new Resultset<>(datafile, Map.class, bean));
            } catch (Exception e) {
                throw new UnableToInitDatasourceException(e);
            }
        }

    }

}
