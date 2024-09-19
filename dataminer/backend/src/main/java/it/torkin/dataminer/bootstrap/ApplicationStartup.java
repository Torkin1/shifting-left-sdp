package it.torkin.dataminer.bootstrap;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.ILinkageController;
import it.torkin.dataminer.control.dataset.stats.LinkageBean;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private JiraConfig jiraConfig;
    @Autowired private GitConfig gitConfig;

    @Autowired private IDatasetController datasetController;
    @Autowired private ILinkageController linkageController;
    @Autowired private DatasetDao datasetDao;

    @Autowired private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        List<Dataset> datasets;
        
        init();
        greet();

        if (isTest()) return;
        
        try {
            
            datasetController.createRawDataset();
            log.info("Dataset loaded");

            datasets = datasetDao.findAll();
            for (Dataset dataset : datasets) {
                LinkageBean linkage = new LinkageBean(dataset.getName());
                LinkageBean buggyLinkage = new LinkageBean(dataset.getName());
                linkageController.calcTicketLinkage(linkage);
                linkageController.calcBuggyTicketLinkage(buggyLinkage);
                log.info(String.format("Linkage for dataset %s: %s", dataset.getName(), linkage));
                log.info(String.format("Buggy linkage for dataset %s: %s", dataset.getName(), buggyLinkage));
            }          


        } catch (UnableToCreateRawDatasetException e) {
            log.error("fatal", e);
            throw new RuntimeException(e);
        }
    }

    private boolean isTest() {
        for (String profile : env.getActiveProfiles()) {
            if (profile.equals("test")) return true;
        }
        return false;
    }

    private void init() {

        
    }

    private void printConfigs() {

        log.info(String.format("jira config: %s", jiraConfig));
        log.info(String.format("git config: %s", gitConfig));
        
    }
    
    private void greet(){
        log.info("Application started");
    }
}
