package it.torkin.dataminer.control.dataset.stats;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class LinkageBean {

    
    public static final String ALL_PROJECTS = "all_projects";
    
    private final String datasetName;
    
    /**
     * Linkage can come with a tag specifying from which project it comes from.
     * If the tag is ALL_PROJECTS, then the linkage is calculated using commits
     * from all projects in the dataset.
     * If a project present in the datasource is not present in this map, then
     * either no commits of that project were loaded in the dataset or commits of
     * that project were all filtered away while crating the processsed dataset,
     * thus we can assume that the linkage for that project is 0.
     */
    private final Map<String, Double> linkageByProject = new HashMap<>();

}
