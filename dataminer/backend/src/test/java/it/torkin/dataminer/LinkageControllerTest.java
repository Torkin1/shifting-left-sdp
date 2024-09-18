package it.torkin.dataminer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.stats.ILinkageController;
import it.torkin.dataminer.control.dataset.stats.LinkageBean;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest()
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class LinkageControllerTest {

    @Autowired private ILinkageController linkageController;
    @Autowired private IDatasetController datasetController;
    
    @Test
    @Transactional
    public void testLinkage() throws UnableToCreateRawDatasetException{

        LinkageBean linkageBean;
        LinkageBean buggyLinkageBean;
        
        datasetController.createRawDataset();
        
        linkageBean = new LinkageBean("apachejit");
        buggyLinkageBean = new LinkageBean("apachejit");
        linkageController.calcBuggyTicketLinkage(buggyLinkageBean);
        linkageController.calcTicketLinkage(linkageBean);

        log.info("Linkage summary : {}", linkageBean.toString());
        log.info("Buggy Linkage summary : {}", buggyLinkageBean.toString());
    }
    
}
