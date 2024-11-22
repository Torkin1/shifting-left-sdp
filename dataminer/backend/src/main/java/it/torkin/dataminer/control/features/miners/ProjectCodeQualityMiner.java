package it.torkin.dataminer.control.features.miners;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import it.torkin.dataminer.CodeSmellsMiningGrpc;
import it.torkin.dataminer.CodeSmellsMiningGrpc.CodeSmellsMiningBlockingStub;
import it.torkin.dataminer.Smells.CodeSmellsCountRequest;
import it.torkin.dataminer.Smells.CodeSmellsCountResponse;
import it.torkin.dataminer.Smells.RepoCoordinates;
import it.torkin.dataminer.config.features.ProjectCodeQualityConfig;
import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
import it.torkin.dataminer.entities.ephemereal.IssueFeature;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    @Autowired private ProjectCodeQualityConfig config;

    private Map<String, Map<String, String>> repoByProjectByDataset = new HashMap<>();
    private CodeSmellsMiningBlockingStub codeSmellsStub;
    
    @Override
    @Transactional
    public void init() throws Exception {
        
        // disable miner if remote is not available
        if (config.getGrpcTarget() == null){
            log.warn("no remote target set, the following features will not be mined: {}", this.getFeatureNames());
            return;
        }
        
        // caches all project-repo mappings for every dataset
        List<Dataset> datasets = datasetDao.findAll();
        for (Dataset dataset : datasets) {
            repoByProjectByDataset.put(dataset.getName(), new HashMap<>());
            Map<String, String> repoByProject = repoByProjectByDataset.get(dataset.getName());

            repoByProject.putAll(dataset.getGuessedRepoByProjects());
        }

        Channel channel = ManagedChannelBuilder.forTarget(config.getGrpcTarget())
            .usePlaintext()
            .build();
        codeSmellsStub = CodeSmellsMiningGrpc.newBlockingStub(channel);
        
    }

    @Override
    @Transactional
    public void mine(FeatureMinerBean bean) {
        
        if (config.getGrpcTarget() == null){
            return;
        }
        
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
            CodeSmellsCountResponse response = codeSmellsStub.countSmells(request);
            smellsCount = response.getSmellsCount();
        } catch (Exception e){
            Log.error("unable to mine smells for repo {} using issue {} at {}", repository, bean.getIssue().getKey(), bean.getMeasurement().getMeasurementDateName(), e);
            smellsCount = -1;
        }
        
        // store results in the measurement object
        bean.getMeasurement().getFeatures().add(new IntegerFeature(featureSubNames[0], smellsCount));
        
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(featureSubNames);
    }

    
}
