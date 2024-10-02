package it.torkin.dataminer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.StatsConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.IStatsController;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class StatsControllerTest {
    
    @Autowired private IDatasetController datasetController;
    @Autowired private IStatsController statsController;
    @Autowired private StatsConfig statsConfig;
    
    @Transactional
    @Test
    public void testPrintStatsToCSV() throws UnableToCreateRawDatasetException, IOException {

        datasetController.createRawDataset();
        statsController.printStatsToCSV();

        assertTrue(new File(statsConfig.getRepositoriesStats()).exists());
        assertTrue(new File(statsConfig.getProjectsStats()).exists());
    }

}
