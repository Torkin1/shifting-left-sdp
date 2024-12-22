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
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueTemporalSpanBean;
import it.torkin.dataminer.control.issue.TemporalSpan;
import it.torkin.dataminer.dao.git.GitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.LongFeature;
import lombok.extern.slf4j.Slf4j;

/**
 * #195
 */
@Component
@Slf4j
public class CommitsWhileInProgressMiner extends FeatureMiner{

    private static final String COUNT = IssueFeature.COMMITS_WHILE_IN_PROGRESS.getFullName("Count");
    private static final String CHURN = IssueFeature.COMMITS_WHILE_IN_PROGRESS.getFullName("Churn");

    @Autowired private IIssueController issueController;
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

        Timestamp measurementDate = bean.getMeasurement().getMeasurementDate();
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();
        String project = issue.getDetails().getFields().getProject().getKey();
        String repository = repoByProjectByDataset.get(dataset).get(project);

        long count = 0;
        long churn = 0;

        GitConfig threadGitConfig = gitConfig.forThread(bean.getThreadIndex());
        
        try(GitDao gitDao = new GitDao(threadGitConfig, repository)){
            gitDao.checkout();
            // gets temporal spans in which the issue was in progress
            IssueTemporalSpanBean issueTemporalSpanBean = new IssueTemporalSpanBean(issue, measurementDate);
            issueController.getInProgressTemporalSpans(issueTemporalSpanBean);

            // for each temporal span, get the commits submitted in it
            for (TemporalSpan span : issueTemporalSpanBean.getTemporalSpans()){
                count += gitDao.getCommitCount(span.getStart(), span.getEnd());
                churn += gitDao.getChurn(span.getStart(), span.getEnd());
            }
        } catch (Exception e) {
            log.error("Error while mining commits while in progress for issue " + issue.getKey(), e);
            count = -1;
            churn = -1;
        }

        bean.getMeasurement().getFeatures().add(new LongFeature(COUNT, count));
        bean.getMeasurement().getFeatures().add(new LongFeature(CHURN, churn));
        
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(COUNT, CHURN); 
    }
    
}
