package it.torkin.dataminer;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.UnableToCreateRawDatasetException;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.entities.Dataset;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class DatasetControllerTest {

    @Autowired private IDatasetController datasetController;

    @Autowired private CommitDao commitDao;

    @Test
    @Transactional
    public void testLoadDataset() throws UnableToCreateRawDatasetException{
      
        datasetController.createRawDataset();
        log.info("loaded {} commits from apachejit", commitDao.countByDatasetName("apachejit"));

        assertNotEquals(0, commitDao.countByDatasetName("apachejit"));
    }
}
