package it.torkin.dataminer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.UnableToLoadDatasetException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Slf4j
public class DatasetControllerTest {

    @Autowired private IDatasetController datasetController;

    @Test
    @Transactional
    public void testLoadDataset() throws UnableToLoadDatasetException{
      
        datasetController.loadDataset();
        log.info(datasetController.getDataset().toString());

    }
}
