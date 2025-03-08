package it.torkin.dataminer.control.features.miners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ProcessBuilder.Redirect;
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
import it.torkin.dataminer.config.features.ProjectCodeQualityConfig;
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
 * We resort to an external binary since PMD library has incompatible dependencies
 * with hibernate.
 */
@Slf4j
@Component
public class CodeQualityMiner extends FeatureMiner{

    private static final String SMELLS = IssueFeature.CODE_QUALITY.getFullName("Smells_count");

    @Autowired private DatasetDao datasetDao;

    private Map<String, Map<String, String>> repoByProjectByDataset = new HashMap<>();

    @Autowired private GitConfig gitConfig;
    @Autowired private DataConfig dataConfig;
    @Autowired private ProjectCodeQualityConfig projectCodeQualityConfig;
    
    @Override
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
    public void mine(FeatureMinerBean bean) {
        
        Integer smellsCount;
        
        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();
        String project = bean.getIssue().getDetails().getFields().getProject().getKey();
        String dataset = bean.getDataset();
        String repository = repoByProjectByDataset.get(dataset).get(project);

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
            CodeSmellsCountResponse response = processRequest(request, bean.getThreadIndex());
            smellsCount = response.getSmellsCount();
        } catch (Exception e){
            Log.error("unable to mine smells for repo {} using issue {} at {}", repository, bean.getIssue().getKey(), bean.getMeasurement().getMeasurementDateName(), e);
            smellsCount = -1;
        }
        
        // store results in the measurement object
        bean.getMeasurement().getFeatures().add(new IntegerFeature(SMELLS, smellsCount));
        
    }

    /**
     * Once a remote miner, now refactored to be launched from parallel forks
     * @param request
     * @return
     */
    private CodeSmellsCountResponse processRequest(CodeSmellsCountRequest request, Integer threadIndex) {
        
        Integer smellsCount;
        String dataDirName = dataConfig.getDir();
        
        GitConfig threadGitConfig = gitConfig.forThread(threadIndex);
        try (GitDao gitDao = new GitDao(threadGitConfig, request.getRepoCoordinates().getName())){

            // checkout corresponding repo at measurement date
            Date measurementDate = Date.from(Instant.ofEpochSecond(
                request.getMeasurementDate().getSeconds(),
                request.getMeasurementDate().getNanos()));
            gitDao.checkout(measurementDate);

            File repository = new File(threadGitConfig.getReposDir() + "/" + request.getRepoCoordinates().getName());
            File root = new File(dataDirName+"/codequality");
            root.mkdirs();
            File violationsFile = new File(root.getAbsolutePath()+"/violations"+threadIndex+".csv");
            File cacheFile = new File(root.getAbsolutePath()+"/cache"+threadIndex+".pmd");
            ProcessBuilder pmdProcess = (new ProcessBuilder(projectCodeQualityConfig.getPmdPath(), "check", "--cache", cacheFile.getAbsolutePath(), "-t", "0", "-d", ".", "-R", "rulesets/java/quickstart.xml", "-f", "csv", "-r", violationsFile.getAbsolutePath()))
                    .directory(repository)
                    .redirectOutput(Redirect.DISCARD)
                    .redirectError(Redirect.INHERIT);
            pmdProcess.environment().remove("JAVA_OPTS");
            pmdProcess.start().waitFor();


            try (BufferedReader reader = new BufferedReader(new FileReader(violationsFile))) {
                smellsCount = 0;
                reader.skip(1);
                while (reader.readLine() != null) smellsCount ++;
            }


        } catch (Exception e) {
            Log.error("unable to mine smells for repo {} ", request.getRepoCoordinates().getName(), e);
            smellsCount = -1;
        }

        return CodeSmellsCountResponse.newBuilder()
            .setSmellsCount(smellsCount)
            .build();
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(SMELLS);
    }

    
}
