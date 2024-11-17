package it.torkin.dataminer.control.dataset.stats;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.dao.local.CommitDao;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.toolbox.math.SafeMath;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class LinkageController implements ILinkageController {

    @Autowired private CommitDao commitDao;
    @Autowired private DatasetDao datasetDao;
    
    @Override
    public void calcBuggyTicketLinkage(LinkageBean linkageBean) {

        calcTicketLinkage(linkageBean, true);        
    }
    
    private void calcTicketLinkage(LinkageBean linkageBean, boolean buggyOnly){
        double linkedCount = 0;
        double count = 0;
        double totalLinkedCount = 0;
        double totalCount = 0;
        double linkage;

        Set<String> projects;
        Dataset dataset;
        
        dataset = datasetDao.findByName(linkageBean.getDatasetName());
        projects = commitDao.findDistinctRepositoriesByDatasetName(dataset.getName());

        // calcs linkage for each project
        for (String project : projects) {

            if(buggyOnly){
                linkedCount = SafeMath.nullAsZero(commitDao.countByDatasetNameAndRepositoryIdAndBuggy(dataset.getName(), project, true));
                count = SafeMath.nullAsZero(dataset.getBuggyUnlinkedByRepository().get(project));
            } else {
                linkedCount =  SafeMath.nullAsZero(commitDao.countByDatasetNameAndRepositoryId(dataset.getName(), project));
                count =  SafeMath.nullAsZero(dataset.getUnlinkedByRepository().get(project));
            }
            count += linkedCount;

            linkage = SafeMath.calcPercentage(linkedCount, count);
            linkageBean.getLinkageByRepository().put(project, linkage);
            
            totalLinkedCount += linkedCount;
            totalCount += count;
        }

        // calcs total linkage
        linkage = SafeMath.calcPercentage(totalLinkedCount, totalCount);
        linkageBean.getLinkageByRepository().put(LinkageBean.ALL_REPOSITORIES, linkage);
    }

    @Override
    public void calcTicketLinkage(LinkageBean linkageBean) {
        calcTicketLinkage(linkageBean, false);
    }
}
