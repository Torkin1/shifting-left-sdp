package it.torkin.dataminer.bootstrap;

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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private JiraConfig jiraConfig;
    @Autowired private GitConfig gitConfig;

    @Autowired private IDatasetController datasetController;

    @Autowired private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        init();
        greet();

        if (isTest()) return;
        
        try {
            
            datasetController.createRawDataset();
            log.info("Dataset loaded");


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
