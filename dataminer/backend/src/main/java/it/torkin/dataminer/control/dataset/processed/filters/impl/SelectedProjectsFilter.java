package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.filters.SelectedProjectFilterConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.entities.jira.project.Project;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SelectedProjectsFilter extends IssueFilter{

    @Autowired private SelectedProjectFilterConfig config;
    
    private Set<String> selectedProjects = null;
    private Set<String> selectedJitDatasets = null;
    
    @Override
    protected void _init(){
        selectedProjects = new HashSet<>();
        if (config.getKeys() != null){
            for (String project : config.getKeys()){
                selectedProjects.add(project);
            }
        }
        if (selectedProjects.isEmpty()){
            log.warn("No projects selected for processing, will not exclude any project by key");
        }
        selectedJitDatasets = new HashSet<>();
        if (config.getJitDatasets() != null){
            for (String jitDataset : config.getJitDatasets()){
                selectedJitDatasets.add(jitDataset);
            }
        }
        if (selectedJitDatasets.isEmpty()){
            log.warn("No jit datasets selected for processing, will not exclude any project by jit dataset");
        }

    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {

        Project project = bean.getIssue().getDetails().getFields().getProject();
        String jitDataset = bean.getDatasetName();
        boolean datasetOk = selectedJitDatasets.isEmpty() || selectedJitDatasets.contains(jitDataset);
        boolean projectOk = selectedProjects.isEmpty() || selectedProjects.contains(project.getKey());
        return datasetOk && projectOk; 
    }
    
}
