package it.torkin.dataminer.control.dataset.raw.datasources;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Feature;
import it.torkin.dataminer.entities.dataset.Measurement;
import it.torkin.dataminer.toolbox.csv.CsvSchemaBean;
import it.torkin.dataminer.toolbox.csv.Resultset;
import it.torkin.dataminer.toolbox.string.BooleanReader;
import it.torkin.dataminer.toolbox.string.StringTools;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Jitsdp implements Datasource{

    private final Set<String> USABLE_PROJECTS = Set.of(
        "camel"
    );

    private static String toRepo(String project){
        StringBuilder builder = new StringBuilder();
        String repoName = null;

        builder.append("apache/");

        switch (project) {
            case "camel":
                repoName = "camel";
                break;
            default:
                log.error("No registered remote repository for project {} of datasource LeveragingJIT", project);
                break;
        }
        builder.append(repoName);
        return builder.toString();
    }

    private Queue<DatasourceFile> datasourceFiles = new LinkedList<>();
    
        private List<File> getDatafiles(DatasourceConfig config){

        List<File> datafiles = new LinkedList<>();
        
        Stream.of(new File(config.getPath()).listFiles())
         .filter(file -> file.getName().endsWith(".csv"))
         .filter(file -> USABLE_PROJECTS.contains(StringTools.stripFilenameExtension(file.getName())))
         .forEach(file -> datafiles.add(file));

        return datafiles;
    }
    
    @Override
    public boolean hasNext() {
        if (!datasourceFiles.isEmpty()) {
            return datasourceFiles.peek().getRecords().hasNext();
        }
        return false;
    }

    @Override
    public Commit next() {
        Map<String, String> record;
        Commit commit = new Commit();
        Measurement measurement = new Measurement();
        BooleanReader booleanReader = new BooleanReader("True", "False");
        
        commit.setRepository(toRepo(StringTools.stripFilenameExtension(datasourceFiles.peek().getFile().getName())));

        record = datasourceFiles.peek().getRecords().next();
        record.forEach((k, v) -> {

            switch (k) {
                case "commit_hash":
                    commit.setHash(v);
                    break;
                case "contains_bug":
                    commit.setBuggy(booleanReader.read(v));
                    break;
                case "classification":
                    measurement.getFeatures().add(new Feature(k, v, String.class));
                    break;
                case "entrophy":
                case "ns":
                case "nd":
                case "nf":
                case "la":
                case "ld":
                case "lt":
                case "ndev":
                case "age":
                case "nuc":
                case "exp":
                case "rexp":
                case "sexp":
                case "glm_probability":
                    measurement.getFeatures().add(new Feature(k, v, Double.class));
                    break;
                case "fix":
                case "linked":
                    measurement.getFeatures().add(new Feature(k, booleanReader.toString(v), Boolean.class));
                    break;
                default:
                    break;
            }
        });

        /**
         * Change data file if the current one has been fully read
         */
        if (!datasourceFiles.peek().getRecords().hasNext()){
            try{
                datasourceFiles.poll().getRecords().close();
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
        for (DatasourceFile datasourceFile : datasourceFiles) {
            datasourceFile.getRecords().close();
        }
    }

    @Override
    public void init(DatasourceConfig config) throws UnableToInitDatasourceException {
        CsvSchemaBean bean = new CsvSchemaBean();
        bean.setColumnSeparator(',');
        List<File> datafiles = getDatafiles(config);
        try {
            for (File datafile : datafiles) {
                datasourceFiles.add(new DatasourceFile(datafile, new Resultset<>(datafile, Map.class, bean)));
            }
        }catch (Exception e) {
            throw new UnableToInitDatasourceException(e);
        }
    }
    
}
