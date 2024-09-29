package it.torkin.dataminer;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.dao.datasources.Apachejit;
import it.torkin.dataminer.dao.datasources.Datasource;
import it.torkin.dataminer.entities.dataset.Issue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class ProcessedDatasetTest {

    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IRawDatasetController rawDatasetController;
    
    @Test
    @Transactional
    public void getFilteredIssuesTest() throws UnableToLoadCommitsException, UnableToInitDatasourceException {

        Datasource datasource = new Apachejit();
        DatasourceConfig config = new DatasourceConfig();
        config.setName("apachejit");
        config.setPath("./src/test/resources/dataset_example/");
        config.setExpectedSize(2000);
        
        datasource.init(config);
        rawDatasetController.loadDatasource(datasource, config);

        ProcessedIssuesBean bean = new ProcessedIssuesBean();
        bean.setDatasetName("apachejit");
        processedDatasetController.getFilteredIssues(bean);
        
        log.info(bean.toString());
        log.info("Processed issues count: " + bean.getProcessedIssues().count());
        log.info(bean.toString());
    }
    
}
