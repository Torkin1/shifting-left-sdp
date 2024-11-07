package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.control.dataset.stats.ILinkageController;
import it.torkin.dataminer.control.dataset.stats.LinkageBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.jira.project.Project;
import lombok.extern.slf4j.Slf4j;

/**
 * #124: accept issues only if its main repository has a buggy linkage
 * greater than a threshold. 
 * The threshold is calculated as the minimun value of the top N
 * different buggy linkages of all projects among all datasets.
 * (This means that the corresponding repositories can be more than N since different
 * repositories could share the same linkage).
 * 
 */
@Component
@Slf4j
public class LinkageFilter extends IssueFilter {

    @Autowired private DatasourceGlobalConfig config;
    @Autowired private DatasetDao datasetDao;
    @Autowired private ILinkageController linkageController;

    private double buggyLinkageThreshold;
    private Map<String, LinkageBean> buggyLinkagesByDataset = new HashMap<>();
    private boolean initialized = false;
    private Map<String, Dataset> datasets = new HashMap<>();

    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        
        if (!initialized) throw new IllegalStateException("Filter not initialized");
        
        LinkageBean linkageBean = buggyLinkagesByDataset.get(bean.getDatasetName());
        Double buggyLinkage;
        Project project = bean.getIssue().getDetails().getFields().getProject();
        Dataset dataset = datasets.get(bean.getDatasetName());
        String guessedRepo = dataset.getGuessedRepoByProjects().get(project.getKey());

        buggyLinkage = linkageBean.getLinkageByRepository().getOrDefault(guessedRepo, 0.0);
        return buggyLinkage >= buggyLinkageThreshold;
    }

    private Double selectTopNThreshold(List<Double> values, int n){

        values.sort(Double::compare);
        // if we don't have enough values, threshold will be the lowest
        // so that all values are accepted
        int selectedIndex = values.size() >= n ? values.size() - n : 0;
        if (selectedIndex == 0 && values.size() > 0){
            log.warn("requestsed top {} different buggy linkage values, but only {} are available", n, values.size());
        }
        return values.get(selectedIndex);
    }

    private List<Double> loadLinkages(){
        List<Double> buggyLinkages = new ArrayList<>();

        for (Dataset dataset : datasets.values()) {
            LinkageBean buggyLinkageBean = new LinkageBean(dataset.getName());
            linkageController.calcBuggyTicketLinkage(buggyLinkageBean);
            buggyLinkagesByDataset.put(dataset.getName(), buggyLinkageBean);
            buggyLinkageBean.getLinkageByRepository().forEach((repository, linkage) -> {
                if (repository != LinkageBean.ALL_REPOSITORIES){
                    buggyLinkages.add(linkage);
                }
            });
        }
        return buggyLinkages;
    }

    private void init(){

        buggyLinkagesByDataset.clear();
        datasets.clear();
        datasetDao.findAll().forEach(dataset -> datasets.put(dataset.getName(), dataset));
        List<Double> buggyLinkages = loadLinkages();
        buggyLinkageThreshold = selectTopNThreshold(buggyLinkages, config.getTopNBuggyLinkage());
        initialized = true;
    }

    @Override
    public void reset(){
        if (!initialized) init();
    }
}
