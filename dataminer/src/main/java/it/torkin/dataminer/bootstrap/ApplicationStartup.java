package it.torkin.dataminer.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.ApachejitConfig;
import it.torkin.dataminer.config.GitConfig;
import it.torkin.dataminer.config.JiraConfig;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private JiraConfig jiraConfig;
    @Autowired private ApachejitConfig apachejitConfig;
    @Autowired private GitConfig gitConfig;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        init();
        greet();

    }

    private void init() {

        
    }

    private void printConfigs() {

        log.debug(String.format("dataset config: %s", apachejitConfig));
        log.debug(String.format("jira config: %s", jiraConfig));
        log.debug(String.format("git config: %s", gitConfig));
        
    }
    
    private void greet(){
        log.info("Application started");
        printConfigs();  
    }
}
