package it.torkin.dataminer.control.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.stream.JsonWriter;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.config.features.NLPFeaturesConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.raw.UnableToFindDatasourceImplementationException;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.control.dataset.raw.UnableToPrepareDatasourceException;
import it.torkin.dataminer.control.dataset.raw.datasources.Datasource;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.measurementdate.IMeasurementDateController;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.CommitCount;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.nlp.Request.NlpIssueBean;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

@Service
@Slf4j
public class DatasetController implements IDatasetController {

    @Autowired private IRawDatasetController rawDatasetController;
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private DatasetDao datasetDao;
    @Autowired private CommitDao commitDao;
    @Autowired private DatasourceGlobalConfig datasourceGlobalConfig;
    @Autowired private NLPFeaturesConfig nlpFeaturesConfig;
    @Autowired private IIssueController issueController;
    @Autowired private IMeasurementDateController measurementDateController;

    private List<Datasource> datasources = new ArrayList<>();
    
    private void loadDatasources() throws Exception {
        Datasource datasource;
        DatasourceConfig config;

        prepareDatasources();

        try (ProgressBar progress = new ProgressBar("loading datasources", datasources.size())) {
            
            for (int i = 0; i < datasources.size(); i++) {
                
                datasource = datasources.get(i);
                config = datasourceGlobalConfig.getSources().get(i);
                progress.setExtraMessage(config.getName());

                if(!datasetDao.existsByName(config.getName())){
                    log.info("loading datasource {}", config.getName());
                    rawDatasetController.loadDatasource(datasource, config);
                }
                else {
                    log.warn("Datasource {} already exists in the database. Skipping.", config.getName());
                }

                datasource.close();
                progress.step();

            }
        }
    }

    /**
     * For each project, selects the corresponding repositories with the most number
     * of commits linked to issues belonging to that project. The mapping is stored
     * in the dataset object.
     */
    private void guessProjectRepositories(List<Dataset> datasets, List<CommitCount> commitCounts){

        Map<String, Map<String, Map<String, Long>>> countByRepoByProjectByDataset = new HashMap<>();
        for(CommitCount commitCount : commitCounts){
            countByRepoByProjectByDataset.putIfAbsent(commitCount.getDataset(), new HashMap<>());
            countByRepoByProjectByDataset.get(commitCount.getDataset()).putIfAbsent(commitCount.getProject(), new HashMap<>());
            countByRepoByProjectByDataset.get(commitCount.getDataset()).get(commitCount.getProject()).put(commitCount.getRepository(), commitCount.getTotal());
        }

        for(Dataset dataset : datasets){

            Map<String, Map<String, Long>> countByRepoByProject = countByRepoByProjectByDataset.get(dataset.getName());
            countByRepoByProject.forEach((project, countByRepo) -> {
                Entry<String, Long> maxEntry = countByRepo.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get();
                dataset.getGuessedRepoByProjects().put(project, maxEntry.getKey());
            });

        }

    }

    /**
     * Commits from a repository could be linked to issues belonging to different projects.
     * This means that projects with very few loaded issues could have repository features
     * that are not necessarily representative of the project. To avoid this, we
     * approximate the relation among repositories and projects by treating it as a 1-1,
     * retaining only the project with the most issues for each repository value.
     */
    private void retainOnlyProjectWithMostIssuesForSameRepository(List<Dataset> datasets, List<CommitCount> commitCounts){
    
        Map<String, Map<String, Map<String, Long>>> countByProjectByRepoByDataset = new HashMap<>();
        for (CommitCount commitCount : commitCounts){
            countByProjectByRepoByDataset.putIfAbsent(commitCount.getDataset(), new HashMap<>());
            countByProjectByRepoByDataset.get(commitCount.getDataset()).putIfAbsent(commitCount.getRepository(), new HashMap<>());
            countByProjectByRepoByDataset.get(commitCount.getDataset()).get(commitCount.getRepository()).put(commitCount.getProject(), commitCount.getTotal());
        }
        

        for (Dataset dataset : datasets){
            
            Map<String, Map<String, Long>> countByProjectByRepo = countByProjectByRepoByDataset.get(dataset.getName());
            Set<String> repositories = countByProjectByRepo.keySet();
            for (String repository : repositories){
                Map<String, Long> countByProject = countByProjectByRepo.get(repository);
                Entry<String, Long> maxEntry = countByProject.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get();
                dataset.getGuessedRepoByProjects().entrySet().removeIf(entry -> {
                    String project = entry.getKey();
                    String repo = entry.getValue();
                    return repo.equals(repository) && !project.equals(maxEntry.getKey());
                });
            }
        }
    }
    
    /**
     * implements #129
     */
    private void mapRepositoriesToProjects(){

        List<Dataset> datasets = datasetDao.findAll();
        datasets.removeIf(dataset -> !dataset.getGuessedRepoByProjects().isEmpty());

        if(!datasets.isEmpty()){
            List<CommitCount> commitCounts = commitDao.countByDatasetAndRepositoryAndProject();
            guessProjectRepositories(datasets, commitCounts);
            retainOnlyProjectWithMostIssuesForSameRepository(datasets, commitCounts);
            datasetDao.saveAll(datasets);
        }
    }
    
    @Override
    @Transactional
    public void createRawDataset() throws UnableToCreateRawDatasetException {

        try {
            
            loadDatasources();
            mapRepositoriesToProjects();
            
        } catch (UnableToPrepareDatasourceException | UnableToLoadCommitsException e) {
            throw new UnableToCreateRawDatasetException(e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            if (e instanceof RuntimeException)
                throw new UnableToCreateRawDatasetException(e);
        }
    }

    /**
     * Assures that
     * all specified datasources are accessible and ready to be mined.
     * @throws UnableToPrepareDatasourceException 
     */
    private void prepareDatasources() throws UnableToPrepareDatasourceException{

        Datasource datasource;
        
        for (DatasourceConfig config : datasourceGlobalConfig.getSources()) {

            
            try {
                datasource = findDatasourceImpl(config);
                datasource.init(config);
                datasources.add(datasource);
            } catch (UnableToFindDatasourceImplementationException | UnableToInitDatasourceException e) {
                throw new UnableToPrepareDatasourceException(config.getName(), e);
            }

        }

    }

    /**
     * Gets the implementation of the datasource specified in the config using
     * reflection.
     * @param config
     * @return
     * @throws UnableToFindDatasourceImplementationException
     */
    private Datasource findDatasourceImpl(DatasourceConfig config) throws UnableToFindDatasourceImplementationException {

        String implName;
        Datasource datasource;

            try {
                implName = implNameFromConfig(config);
                datasource = (Datasource) Class.forName(implName).getDeclaredConstructor().newInstance();
                return datasource;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException e) {
                throw new UnableToFindDatasourceImplementationException(config.getName(), e);
            }

    }

    private String implNameFromConfig(DatasourceConfig config) {
        return String.format("%s.%s%s", datasourceGlobalConfig.getImplPackage(),
         config.getName().substring(0, 1).toUpperCase(), config.getName().substring(1));
    }

    @Override
    @Transactional
    public void getProcessedIssues(ProcessedIssuesBean bean) {
        processedDatasetController.getFilteredIssues(bean);
    }

    @Override
    @Transactional
    public void printNLPIssueBeans() throws IOException {
        File serializedIssueSummariesFile = new File(nlpFeaturesConfig.getNlpIssueBeans());

        if (!serializedIssueSummariesFile.exists()){
            serializeIssueBeans(serializedIssueSummariesFile);
        }
    }

    private void serializeBean(JsonWriter writer, NlpIssueBean bean) throws IOException{

        writer.beginObject();

        writer.name("dataset").value(bean.getDataset());
        writer.name("Project name").value(bean.getProject());
        writer.name("measurementDateName").value(bean.getMeasurementDateName());
        writer.name("Requirement ID").value(bean.getKey());
        writer.name("Requirement text").value(bean.hasDescription()? bean.getDescription() : null);
        writer.name("Requirement title").value(bean.hasTitle()? bean.getTitle() : null);
        writer.name("buggy").value(bean.getBuggy());
        writer.name("date").value(
            new SimpleDateFormat("yyyy-MM-dd").format(Date.from(
                Instant.ofEpochSecond(bean.getMeasurementDate().getSeconds(),
                 bean.getMeasurementDate().getNanos()))));
        
        // comments
        writer.name("comments");
        writer.beginArray();
        for (String comment : bean.getCommentsList()){
            writer.value(comment);
        }
        writer.endArray();

        writer.endObject();
    }

    private NlpIssueBean prepareBean(Issue issue, Dataset dataset, MeasurementDate measurementDate){

        IssueFields fields = issue.getDetails().getFields();
        Timestamp measurementDateValue = measurementDate.apply(new MeasurementDateBean(dataset.getName(), issue));

        String description = issueController.getDescription(new IssueBean(issue, measurementDateValue));
        String title = issueController.getTitle(new IssueBean(issue, measurementDateValue));

        NlpIssueBean.Builder beanBuilder = NlpIssueBean.newBuilder()
            .setDataset(dataset.getName())
            .setProject(fields.getProject().getKey())
            .setMeasurementDateName(measurementDate.getName())
            .setKey(issue.getKey())
            .setBuggy(issueController.isBuggy(new IssueCommitBean(issue, dataset.getName())))
            .setMeasurementDate(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(measurementDateValue.getTime() / 1000)
                .setNanos(measurementDateValue.getNanos()).build())
            .addAllComments(
                issueController.getComments(new IssueBean(issue, measurementDateValue))
                .stream()
                .map(comment -> comment.getBody())
                .toList());
            

        if (description != null){
            beanBuilder.setDescription(description);
        }
        if (title != null){
            beanBuilder.setTitle(title);
        }

        return beanBuilder.build();

    }

    /**
     * Dumps issue beans to a JSON file
     */
    private void serializeIssueBeans(File outputFile) throws IOException{

        List<Dataset> datasets;
        ProcessedIssuesBean processedIssuesBean;
        List<MeasurementDate> measurementDates = measurementDateController.getMeasurementDates();

        try (JsonWriter writer = new JsonWriter(new FileWriter(outputFile))) {

            writer.beginArray();

            datasets = datasetDao.findAll();
            for (Dataset dataset : datasets) {
                for (MeasurementDate measurementDate : measurementDates) {

                    // query db for processed issues
                    processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), measurementDate);
                    processedDatasetController.getFilteredIssues(processedIssuesBean);

                    // write summaries to JSON
                    try(Stream<Issue> issues = processedIssuesBean.getProcessedIssues()){
                        issues.forEach((issue) -> {
                            try {
                                NlpIssueBean bean = prepareBean(issue, dataset, measurementDate);
                                serializeBean(writer, bean);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
            writer.endArray();
        }

    }
}
