package it.torkin.dataminer.control.dataset;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.raw.UnableToFindDatasourceImplementationException;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.control.dataset.raw.UnableToPrepareDatasourceException;
import it.torkin.dataminer.control.dataset.raw.datasources.Datasource;
import it.torkin.dataminer.dao.local.CommitCount;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
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
}
