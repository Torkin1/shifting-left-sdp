package it.torkin.dataminer.control.dataset.stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import it.torkin.dataminer.config.StatsConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.toolbox.math.SafeMath;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StatsController implements IStatsController{

    @Autowired private ILinkageController linkageController;
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private IIssueController issueController;
    @Autowired private DatasetDao datasetDao;
    @Autowired private StatsConfig statsConfig;
    
    @Override
    @Transactional
    public void printStatsToCSV() throws IOException {
        
        List<Dataset> datasets = datasetDao.findAll();
        CsvMapper mapper = new CsvMapper();
        
        /**
         * Commit stats are calculated using repositories (registered in Git),
         * while tickets stats are calculated using projects (registered in Jira)
         */
        
        CsvSchema repositoriesSchema = mapper.schemaFor(RepositoriesStatsRow.class).withUseHeader(true);
        File repositoriesOutput = new File(statsConfig.getRepositoriesStats());
        ObjectWriter repositoriesWriter = mapper.writer(repositoriesSchema);
        printRepositoriesStats(datasets, repositoriesWriter, repositoriesOutput);

        CsvSchema projectsSchema = mapper.schemaFor(ProjectStatsRow.class).withUseHeader(true);
        File projectsOutput = new File(statsConfig.getProjectsStats());
        ObjectWriter projectsWriter = mapper.writer(projectsSchema);
        printProjectsStats(datasets, projectsWriter, projectsOutput);

    }

    private void printRepositoriesStats(List<Dataset> datasets, ObjectWriter writer,
        File output) throws IOException {

        try (SequenceWriter sequenceWriter = writer.writeValues(output)) {
            for (Dataset dataset : datasets) {

                LinkageBean linkageBean = new LinkageBean(dataset.getName());
                LinkageBean buggyLinkageBean = new LinkageBean(dataset.getName());

                linkageController.calcTicketLinkage(linkageBean);
                linkageController.calcBuggyTicketLinkage(buggyLinkageBean);

                linkageBean.getLinkageByRepository().forEach((repository, linkage) -> {

                    if (!repository.equals(LinkageBean.ALL_REPOSITORIES)) {
                        RepositoriesStatsRow row = new RepositoriesStatsRow();
                        row.setDataset(dataset.getName());
                        row.setRepository(repository);
                        row.setLinkage(linkage);
                        row.setBuggyLinkage(
                                buggyLinkageBean.getLinkageByRepository().getOrDefault(repository, 0.0)
                        );
                        try {
                            sequenceWriter.write(row);

                        } catch (IOException e) {
                            log.error("Cannot write row to CSV at {}, row is {}", output, row, e);
                            throw new RuntimeException(e);
                        }
                    }
                });
                log.info("repository stats dumped for dataset {}", dataset.getName());
            }
        }
    }

    private void printProjectsStats(List<Dataset> datasets, ObjectWriter writer,
    File output) throws IOException{

        try (SequenceWriter sequenceWriter = writer.writeValues(output)){
            for (Dataset dataset : datasets){
            
            Map<String, Integer> issuesByProject = new HashMap<>();
            Map<String, Integer> buggyIssuesByProject = new HashMap<>();
            // For stats purpose only, the date of first commit is enough
            ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), new FirstCommitDate());
            processedDatasetController.getFilteredIssues(processedIssuesBean);
            // we must first count each project's issues in order to trigger filters
            processedIssuesBean.getProcessedIssues().forEach((issue) -> {
                String project = issue.getDetails().getFields().getProject().getName();
                if(issueController.isBuggy(new IssueBean(issue, dataset.getName()))){
                    buggyIssuesByProject.compute(project, (k, v) -> v == null ? 1 : v + 1);
                }
                issuesByProject.compute(project, (k, v) -> v == null ? 1 : v + 1);
            });
            // now we have filtered out issue counts by project and can proceed
            // to write stats to csv
                issuesByProject.forEach((project, count) -> {
                    
                    ProjectStatsRow row = new ProjectStatsRow();
                    String measurementDate = processedIssuesBean.getMeasurementDate().getClass().getSimpleName();
                    long excludedTickets = processedIssuesBean.getExcludedByProject().getOrDefault(project, 0);
                    long usableBuggyTickets = buggyIssuesByProject.getOrDefault(project, 0);
                    long usableTickets = count;
                    long tickets = usableTickets + excludedTickets;

                    row.setDataset(dataset.getName());
                    row.setProject(project);
                    row.setMeasurementDate(measurementDate);
                    row.setTickets(tickets);
                    row.setUsableTickets(count);
                    row.setExcludedTickets(excludedTickets);
                    row.setUsableBuggyTicketsPercentage(SafeMath.calcPercentage(usableBuggyTickets, usableTickets));
                    processedIssuesBean.getFilteredByProjectGroupedByFilter().forEach((filter, filteredByProject) -> {
                        long filteredTickets = filteredByProject.getOrDefault(project, 0L);
                        if (filteredTickets > 0){
                            row.getFilteredTicketsByFilterMap().put(filter, filteredTickets);
                        }
                    });
                    
                    try {
                        sequenceWriter.write(row);
                    } catch (IOException e) {
                        log.error("Cannot write row to CSV at {}, row is {}", output, row, e);
                        throw new RuntimeException(e);
                    }
                });
                log.info("project stats dumped for dataset {}", dataset.getName());
            }
        }
    }

}
