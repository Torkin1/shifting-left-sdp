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
    
    @Override
    protected void _init(){
        selectedProjects = new HashSet<>();
        if (config.getKeys() != null){
            for (String project : config.getKeys()){
                selectedProjects.add(project);
            }
        }
        if (selectedProjects.isEmpty()){
            log.warn("No projects selected for processing, will not exclude any projectb by key");
        }
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {

        if (selectedProjects.isEmpty()) return true;
        Project project = bean.getIssue().getDetails().getFields().getProject();
        return selectedProjects.contains(project.getKey());
    }
    
}
