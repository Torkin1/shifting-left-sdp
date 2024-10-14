package it.torkin.dataminer;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.config.NLPFeaturesConfig;
import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.features.miners.NLPFeaturesMiner;
import it.torkin.dataminer.control.features.miners.UnableToInitNLPFeaturesMinerException;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class FeatureMinerTest {

    @Autowired private IDatasetController datasetController;
    @Autowired private NLPFeaturesMiner nlpFeaturesMiner;
    @Autowired private NLPFeaturesConfig config;

    @Test
    public void testNlpFeaturesMiner() throws UnableToCreateRawDatasetException, UnableToInitNLPFeaturesMinerException {


        datasetController.createRawDataset();
        nlpFeaturesMiner.init();

        assertTrue(new File(config.getNlpIssueBeans()).exists());

    }

}
