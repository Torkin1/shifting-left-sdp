package it.torkin.dataminer.control.features.miners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cthing.locc4j.CountUtils;
import org.cthing.locc4j.CountingTreeWalker;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.features.LongFeature;
import it.torkin.dataminer.entities.jira.project.Project;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CodeSizeMiner extends FeatureMiner{
    
    private static final String TOTAL_LOCS = IssueFeature.CODE_SIZE + ": total LOCs";
    private static final String NUM_OF_FILES = IssueFeature.CODE_SIZE + ": number of files";
    private static final String NUM_OF_LANGUAGES = IssueFeature.CODE_SIZE + ": number of languages";

    @Autowired DatasetDao datasetDao;
    @Autowired private GitConfig gitConfig;

    private Map<String, Map<String, String>> repositoriesByProjectByDataset = new HashMap<>();
    
    @Override
    public void init() {
        
        datasetDao.findAll().forEach(dataset -> {
            repositoriesByProjectByDataset.putIfAbsent(dataset.getName(), dataset.getGuessedRepoByProjects());
        });
    }
    
    @Override
    public void mine(FeatureMinerBean bean) {
        
        GitConfig threadGitConfig = gitConfig.forThread(bean.getThreadIndex());
        Project project = bean.getIssue().getDetails().getFields().getProject();
        String dataset = bean.getDataset();
        String repository = repositoriesByProjectByDataset.get(dataset).get(project.getKey());
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();

        long totalLocs = -1;
        long numOfFiles = -1;
        long numOfLanguages = -1;

        try (GitDao gitDao = new GitDao(threadGitConfig, repository)){

            gitDao.checkout(measurementDate);
            
            Path repositoryPath = getRepositoryPath(dataset, project, threadGitConfig);
            Map<Path, Map<Language, Counts>> counts = getCounts(repositoryPath);

            numOfLanguages = getNumOfLanguages(counts);
            numOfFiles = getNumOfFiles(counts);
            totalLocs = getTotalLocs(counts);

        } catch (Exception e) {
            
            log.error("Error while mining project code size", e);

        } finally {
            
            bean.getMeasurement().getFeatures().add(new LongFeature(TOTAL_LOCS, totalLocs));
            bean.getMeasurement().getFeatures().add(new LongFeature(NUM_OF_FILES, numOfFiles));
            bean.getMeasurement().getFeatures().add(new LongFeature(NUM_OF_LANGUAGES, numOfLanguages));
        }

    }

    private long getNumOfLanguages(Map<Path, Map<Language, Counts>> counts) {
        return CountUtils.languages(counts).size();
    }

    private long getNumOfFiles(Map<Path, Map<Language, Counts>> counts) {
        return CountUtils.files(counts).size();
    }

    private long getTotalLocs(Map<Path, Map<Language, Counts>> counts) {
        return CountUtils.total(counts).getCodeLines();
    }

    private Path getRepositoryPath(String dataset, Project project, GitConfig gitConfig) {
        String repositoryName = repositoriesByProjectByDataset.get(dataset).get(project.getKey());
        File repository = new File(gitConfig.getReposDir(), repositoryName);
        return Paths.get(repository.getAbsolutePath());
    }

    private Map<Path, Map<Language, Counts>> getCounts(Path repository) throws IOException{

        CountingTreeWalker walker = new CountingTreeWalker(repository);
        return walker.count();
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(TOTAL_LOCS, NUM_OF_FILES, NUM_OF_LANGUAGES);
    }
    
}
