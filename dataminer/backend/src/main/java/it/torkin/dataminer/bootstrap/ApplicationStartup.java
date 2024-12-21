package it.torkin.dataminer.bootstrap;

import java.io.IOException;

import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.features.PrintMeasurementsBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.IStatsController;
import it.torkin.dataminer.control.features.IFeatureController;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.features.PrintMeasurementsBean;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private IDatasetController datasetController;
    @Autowired private IStatsController statsController;
    @Autowired private IFeatureController featureController;

    @Autowired private Environment env;
    @Autowired private ApplicationArguments args;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        try {
            init();
            
            if (isTest()) return;
        
            createRawDataset();
            printStats();
            printNLPIssueBeans();
            mineFeatures();


        } catch (Exception e) {
            log.error("fatal", e);
            throw new RuntimeException(e);
        }
    }

    private void init(){
        redirectRestletLogging();
    }
    
    private void redirectRestletLogging() {
        System.getProperties().put("org.restlet.engine.loggerFacadeClass", "org.restlet.ext.slf4j.Slf4jLoggerFacade");
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

        featureController.mineFeatures();
        log.info("Features mined");

        featureController.printMeasurements(new PrintMeasurementsBean(IssueFeature.BUGGINESS));
    }

    private void printNLPIssueBeans() throws IOException {
        datasetController.printNLPIssueBeans();
        log.info("NLP issue beans printed");
    }
}
