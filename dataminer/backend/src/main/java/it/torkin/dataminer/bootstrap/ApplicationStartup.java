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
import it.torkin.dataminer.control.dataset.stats.IStatsController;
import it.torkin.dataminer.control.features.IFeatureController;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private IDatasetController datasetController;
    @Autowired private IStatsController statsController;
    @Autowired private IFeatureController featureController;

    @Autowired private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        
        if (isTest()) return;
        
        try {
            
            createRawDataset();
            printStats();
            mineFeatures();

        } catch (Exception e) {
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
        log.info("dataset stats printed");
    }

    private void createRawDataset() throws UnableToCreateRawDatasetException {
        datasetController.createRawDataset();
        log.info("Dataset loaded");
    }

    private void mineFeatures() throws Exception{
        featureController.initMiners();
        log.info("Features miners initialized");
        
        // TODO: mine features

    }
}
