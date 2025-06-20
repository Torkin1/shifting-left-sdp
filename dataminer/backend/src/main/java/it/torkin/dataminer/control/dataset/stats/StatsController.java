package it.torkin.dataminer.control.dataset.stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
import it.torkin.dataminer.control.issue.IssueCommitBean;
import it.torkin.dataminer.control.measurementdate.impl.FirstCommitDate;
import it.torkin.dataminer.dao.local.DatasetDao;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.project.Project;
import it.torkin.dataminer.toolbox.math.SafeMath;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

        File repositoriesOutput = new File(statsConfig.getRepositoriesStats());
        if (!repositoriesOutput.exists()) {
            CsvSchema repositoriesSchema = mapper.schemaFor(RepositoriesStatsRow.class).withUseHeader(true);
            ObjectWriter repositoriesWriter = mapper.writer(repositoriesSchema);
            printRepositoriesStats(datasets, repositoriesWriter, repositoriesOutput);
        } else {
          log.info("Repositories stats already exists at {}", repositoriesOutput.getAbsolutePath());
        }

        File projectsOutput = new File(statsConfig.getProjectsStats());
        if (!projectsOutput.exists()) {
            CsvSchema projectsSchema = mapper.schemaFor(ProjectStatsRow.class).withUseHeader(true);
            ObjectWriter projectsWriter = mapper.writer(projectsSchema);
            printProjectsStats(datasets, projectsWriter, projectsOutput);
        } else {
            log.info("Projects stats already exists at {}", projectsOutput.getAbsolutePath());
        }
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
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private class IssueCount {
        private long count = 0;
        private long buggyCount = 0;

        public void add(Issue issue, Dataset dataset){
            count++;
            if(issueController.isBuggy(new IssueCommitBean(issue, dataset.getName()))){
                buggyCount++;
            }
        }

        public void add(IssueCount other){
            count += other.count;
            buggyCount += other.buggyCount;
        }
    }

    @Data
    private class CountIssuesBean{
        // input
        private final Dataset dataset;
        private final Issue issue;

        // output
        private Project project;
        private IssueCount issueCount;
    }
    
    private void printProjectsStats(List<Dataset> datasets, ObjectWriter writer,
    File output) throws IOException{

        try (SequenceWriter sequenceWriter = writer.writeValues(output)){
            for (Dataset dataset : datasets){
            
                LinkageBean linkageBean = new LinkageBean(dataset.getName());
                LinkageBean buggyLinkageBean = new LinkageBean(dataset.getName());

                linkageController.calcTicketLinkage(linkageBean);
                linkageController.calcBuggyTicketLinkage(buggyLinkageBean);

                Map<String, IssueCount> countByProject = new HashMap<>();               
                // For stats purpose only, the date of first commit is enough
                ProcessedIssuesBean processedIssuesBean = new ProcessedIssuesBean(dataset.getName(), new FirstCommitDate());
                processedDatasetController.getFilteredIssues(processedIssuesBean);
                // we must first count each project's issues in order to trigger filters
                try(Stream<Issue> issues = processedIssuesBean.getProcessedIssues()){

                    Iterator<Issue> issueIterator = issues.iterator();
                    while(issueIterator.hasNext()){
                        Issue issue = issueIterator.next();
                        Project project = issue.getDetails().getFields().getProject();
                        countByProject.putIfAbsent(project.getKey(), new IssueCount());
                        countByProject.get(project.getKey()).add(issue, dataset);
                    }
                }
                // now we have filtered out issue counts by project and can proceed
                // to write stats to csv
                countByProject.forEach((project, issuecount) -> {
                    
                    ProjectStatsRow row = new ProjectStatsRow();
                    String measurementDate = processedIssuesBean.getMeasurementDate().getClass().getSimpleName();
                    long excludedTickets = processedIssuesBean.getExcludedByProject().getOrDefault(project, 0);
                    long usableBuggyTickets = issuecount.getBuggyCount();
                    long usableTickets = issuecount.getCount();
                    long tickets = usableTickets + excludedTickets;
                    String guessedRepository = dataset.getGuessedRepoByProjects().get(project);
                    Map<String, Long> filteredTicketsByFilter = new HashMap<>();

                    row.setDataset(dataset.getName());
                    row.setProject(project);
                    row.setGuessedRepository(guessedRepository);
                    row.setLinkage(linkageBean.getLinkageByRepository().getOrDefault(guessedRepository, 0.0));
                    row.setBuggyLinkage(buggyLinkageBean.getLinkageByRepository().getOrDefault(guessedRepository, 0.0));
                    row.setMeasurementDate(measurementDate);
                    row.setTickets(tickets);
                    row.setUsableTickets(usableTickets);
                    row.setExcludedTickets(excludedTickets);
                    row.setUsableBuggyTicketsPercentage(SafeMath.calcPercentage(usableBuggyTickets, usableTickets));
                    processedIssuesBean.getFilteredByProjectGroupedByFilter().forEach((filter, filteredByProject) -> {
                        long filteredTickets = filteredByProject.getOrDefault(project, 0L);
                        if (filteredTickets > 0){
                            filteredTicketsByFilter.put(filter, filteredTickets);
                        }
                    });
                    row.setFilteredTicketsByFilterMap(filteredTicketsByFilter);
                    
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
