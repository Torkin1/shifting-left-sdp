package it.torkin.dataminer.control.features.miners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.Smells.CodeSmellsCountRequest;
import it.torkin.dataminer.Smells.CodeSmellsCountResponse;
import it.torkin.dataminer.Smells.RepoCoordinates;
import it.torkin.dataminer.config.DataConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * #193: Project Code Quality Miner.
 * We resort to a remote miner since PMD library has incompatible dependencies
 * with hibernate.
 */
@Slf4j
@Component
public class ProjectCodeQualityMiner extends FeatureMiner{

    private static final String[] featureSubNames = {
        IssueFeature.PROJECT_CODE_QUALITY + ": " + "Smells count",
    };

    @Autowired private DatasetDao datasetDao;

    private Map<String, Map<String, String>> repoByProjectByDataset = new HashMap<>();

    @Autowired private GitConfig gitConfig;
    @Autowired private DataConfig dataConfig;
    
    @Override
    @Transactional
    public void init() throws Exception {
        
        // caches all project-repo mappings for every dataset
        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets) {
            repoByProjectByDataset.put(dataset.getName(), new HashMap<>());
            Map<String, String> repoByProject = repoByProjectByDataset.get(dataset.getName());

            repoByProject.putAll(dataset.getGuessedRepoByProjects());
        }

    }

    @Override
    @Transactional
    public void mine(FeatureMinerBean bean) {
        
        Integer smellsCount;
        
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        String dataset = bean.getDataset();
        String repository = repoByProjectByDataset.get(dataset).get(project);

        // fetch smells count from remote miner
        CodeSmellsCountRequest request = CodeSmellsCountRequest.newBuilder()
            .setMeasurementDate(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(measurementDate.getTime() / 1000)
                .setNanos(measurementDate.getNanos())
                .build())
            .setRepoCoordinates(RepoCoordinates.newBuilder()
                .setName(repository)
                .build())
            .build();
        try{
            CodeSmellsCountResponse response = processRequest(request);
            smellsCount = response.getSmellsCount();
        } catch (Exception e){
            Log.error("unable to mine smells for repo {} using issue {} at {}", repository, bean.getIssue().getKey(), bean.getMeasurement().getMeasurementDateName(), e);
            smellsCount = -1;
        }
        
        // store results in the measurement object
        bean.getMeasurement().getFeatures().add(new IntegerFeature(featureSubNames[0], smellsCount));
        
    }

    /**
     * Once a remote miner, now refactored to be launched from parallel forks
     * @param request
     * @return
     */
    private CodeSmellsCountResponse processRequest(CodeSmellsCountRequest request) {
        
        Integer smellsCount;
        String dataDirName = dataConfig.getDir();
        
        try (GitDao gitDao = new GitDao(gitConfig, request.getRepoCoordinates().getName())){

            // checkout corresponding repo at measurement date
            Date measurementDate = Date.from(Instant.ofEpochSecond(
                request.getMeasurementDate().getSeconds(),
                request.getMeasurementDate().getNanos()));
            System.out.println("requested to measure code quality of repo "+request.getRepoCoordinates().getName()+ " at " +measurementDate);
            gitDao.checkout(measurementDate);

            File repository = new File(gitConfig.getReposDir() + "/" + request.getRepoCoordinates().getName());
            File violationsFile = new File(dataDirName+"/violations.csv");
            (new ProcessBuilder("/pmd/bin/pmd", "check", "-t", "0", "-d", ".", "-R", "rulesets/java/quickstart.xml", "-f", "csv", "-r", violationsFile.getAbsolutePath()))
                    .directory(repository)
                    .inheritIO()
                    .start();


            try (BufferedReader reader = new BufferedReader(new FileReader(violationsFile))) {
                smellsCount = 0;
                reader.skip(1);
                while (reader.readLine() != null) smellsCount ++;
            }


        } catch (Exception e) {
            Log.error("unable to mine smells for repo {} ", request.getRepoCoordinates().getName(), e);
            smellsCount = -1;
        }

        System.out.println("smellscount: "+smellsCount);
        return CodeSmellsCountResponse.newBuilder()
            .setSmellsCount(smellsCount)
            .build();
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(featureSubNames);
    }

    
}
