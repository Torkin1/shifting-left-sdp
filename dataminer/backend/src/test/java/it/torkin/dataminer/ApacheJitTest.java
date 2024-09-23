package it.torkin.dataminer;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.dao.datasources.Apachejit;
import it.torkin.dataminer.dao.datasources.Datasource;
import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.toolbox.time.TimeTools;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class ApacheJitTest {

    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;

    @Test
    public void testCommitLoading() throws Exception{

        try(Datasource apachejit = new Apachejit();) {
            
            Dataset dataset = new Dataset();
            dataset.setName("apachejit");
            dataset = datasetDao.save(dataset);
            
            DatasourceConfig datasourceConfig = new DatasourceConfig();
            datasourceConfig.setExpectedSize(2000);
            datasourceConfig.setName("apachejit");
            datasourceConfig.setPath("./src/test/resources/dataset_example/");
    
            apachejit.init(datasourceConfig);
            Commit commit = apachejit.next();
            commit.setDataset(dataset);

            commit.getMeasurement().setPredictionDate(TimeTools.now());
            commit.getMeasurement().setCommit(commit);
            commit = commitDao.save(commit);
            log.debug(commit.toString());

            assertNotEquals(0, commit.getMeasurement().getFeatures().size());

        }
    }

}
