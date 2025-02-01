package it.torkin.dataminer.control.dataset.processed.filters.impl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.config.filters.SelectedProjectFilterConfig;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilter;
import it.torkin.dataminer.control.dataset.processed.filters.IssueFilterBean;
import it.torkin.dataminer.entities.jira.project.Project;

@Component
public class SelectedProjectsFilter extends IssueFilter{

    @Autowired private SelectedProjectFilterConfig config;
    
    private Set<String> selectedProjects = null;
    
    private void init(){
        selectedProjects = new HashSet<>();
        if (config.getSelectedProjects() != null && config.getSelectedProjects().length > 0){
            for (String project : config.getSelectedProjects()){
                selectedProjects.add(project);
            }
        }
    }
    
    @Override
    protected Boolean applyFilter(IssueFilterBean bean) {
        if (selectedProjects == null) init();

        Project project = bean.getIssue().getDetails().getFields().getProject();
        return selectedProjects.contains(project.getKey());
    }
    
}
