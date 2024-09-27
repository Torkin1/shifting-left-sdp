package it.torkin.dataminer;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.dao.datasources.Apachejit;
import it.torkin.dataminer.dao.datasources.Datasource;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class DatasetControllerTest {

    @Autowired private IDatasetController datasetController;
    @Autowired private IRawDatasetController rawDatasetController;

    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;

    @Test
    @Transactional
    public void testLoadDataset() throws UnableToCreateRawDatasetException{
      
        
        datasetController.createRawDataset();
        // log.info("loaded {} commits from apachejit", commitDao.countByDatasetName("apachejit"));
        List<Dataset> datasets = datasetDao.findAll();
        log.info("Datasets : {}", datasets);

        assertNotEquals(0, datasets.size());
    }

    @Test
    public void testTransactionalLoadDataset() throws Exception{

        DatasourceConfig config;

        config = new DatasourceConfig();
        config.setName("apachejit");
        config.setPath("./src/test/resources/dataset_example_failure/");
        config.setExpectedSize(2);

        try (Datasource datasource = new Apachejit()) {
            datasource.init(config);
            // modify loadDatasource to throw an exception, or 
            // put in datasource commits with a linked issue that
            // can trigger an exception(for example, with duplicated primary keys)
            rawDatasetController.loadDatasource(datasource, config);
        } catch (UnableToLoadCommitsException | RuntimeException e) {
            
            log.debug("caught exception", e);
            assertNull(datasetDao.findByName("apachejit"));
            return;
        }

        fail("Expected exception");

    }
}
