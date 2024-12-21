package it.torkin.dataminer.control.features.miners;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.control.measurementdate.impl.OneSecondBeforeFirstCommitDate;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.extern.slf4j.Slf4j;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
import it.torkin.dataminer.entities.dataset.features.LongFeature;

@Component
@Slf4j
public class LatestCommitMiner extends FeatureMiner{

    private static final String CHURN = IssueFeature.LATEST_COMMIT.getFullName() + ": Churn";
    private static final String NUM_OF_FILES = IssueFeature.LATEST_COMMIT.getFullName() + ": Number of files"; 

    @Autowired private GitConfig gitConfig;
    @Autowired private DatasetDao datasetDao;
    
    private Map<String, Map<String, String>> repoByProjectByDataset = new HashMap<>();

    @Override
    public void init(){
        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets){
            Map<String, String> repoByProject = new HashMap<>();
            repoByProject.putAll(dataset.getGuessedRepoByProjects());
            repoByProjectByDataset.put(dataset.getName(), repoByProject);
        }
    }
    
    @Override
    public void mine(FeatureMinerBean bean) {
        
        long churn = 0;
        int numOfFiles = 0;
        
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();
        String project = issue.getDetails().getFields().getProject().getKey();
        String repository = repoByProjectByDataset.get(dataset).get(project);
        Timestamp firstCommitDate = new OneSecondBeforeFirstCommitDate().apply(new MeasurementDateBean(dataset, issue));

        GitConfig threadGitConfig = gitConfig.forThread(bean.getThreadIndex());

        try (GitDao gitDao = new GitDao(threadGitConfig, repository)){

            gitDao.checkout();
            String latestCommitHash = gitDao.getLatestCommitHash(firstCommitDate);
            if (latestCommitHash == null){
                log.warn("first commit date {} of issue {} precedes repository {} creation date", firstCommitDate, issue.getKey(), repository);
                churn = -1;
                numOfFiles = -1;
            }
            else {
                churn = gitDao.getChurn(latestCommitHash);
                numOfFiles = gitDao.getCommitChangeset(latestCommitHash).size();
            }



        } catch (Exception e){
            log.error("Error while mining latest project commit for issue {}", issue.getKey(), e);
            churn = -1;
            numOfFiles = -1;
        }

        bean.getMeasurement().getFeatures().add(new LongFeature(CHURN, churn));
        bean.getMeasurement().getFeatures().add(new IntegerFeature(NUM_OF_FILES, numOfFiles));
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(CHURN, NUM_OF_FILES);
    }
    
}
