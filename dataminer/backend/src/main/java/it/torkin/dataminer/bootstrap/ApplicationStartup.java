package it.torkin.dataminer.bootstrap;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.ILinkageController;
import it.torkin.dataminer.control.dataset.stats.IStatsController;
import it.torkin.dataminer.control.dataset.stats.LinkageBean;
import it.torkin.dataminer.entities.dataset.Dataset;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private IDatasetController datasetController;
    @Autowired private ILinkageController linkageController;
    @Autowired private IStatsController statsController;

    @Autowired private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        if (isTest()) return;
        
        try {
            
            createRawDataset();
            printStats();         

        } catch (UnableToCreateRawDatasetException | IOException e) {
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

    private void printStats() throws IOException{
        statsController.printStatsToCSV();
    }

    private void createRawDataset() throws UnableToCreateRawDatasetException {
        datasetController.createRawDataset();
        log.info("Dataset loaded");
    }
    
    private void calcLinkage(Dataset dataset){
        LinkageBean linkage = new LinkageBean(dataset.getName());
        LinkageBean buggyLinkage = new LinkageBean(dataset.getName());
        linkageController.calcTicketLinkage(linkage);
        linkageController.calcBuggyTicketLinkage(buggyLinkage);
        log.info(String.format("Linkage for dataset %s: %s", dataset.getName(), linkage));
        log.info(String.format("Buggy linkage for dataset %s: %s", dataset.getName(), buggyLinkage));
    }
}
