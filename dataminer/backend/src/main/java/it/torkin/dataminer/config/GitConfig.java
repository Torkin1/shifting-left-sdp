package it.torkin.dataminer.config;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.git",
    ignoreUnknownFields = false)
@Data
public class GitConfig {

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(reposDir)){
            reposDir = dataConfig.getDir() + "/repositories";
        }
        if (StringTools.isBlank(forksSubDirName)){
            forksSubDirName = "/forks";
        }
        new File(reposDir).mkdirs();
    }
    
    @Autowired private DataConfig dataConfig;
    
    /** remote hostname where repos are downloaded if not 
     * available locally
     */
    @NotBlank
    private String hostname;

    /** Dir where repos will be stored */
    private String reposDir;

    /** Subdir name where to store repo copies for forks */
    private String forksSubDirName;

    /**Used to extract linked issue key in commit message
     */
    @NotBlank
    private String linkedIssueKeyRegexp;

    /**
     * List of names to try to detect the default branch.
     * The candidates are tried in order and the first one
     * that positively matches is used as default branch.
     * If no candidate is suitable for a project an exception is thrown
     * when creating the corresponding GitDao
     */
    @NotEmpty
    private List<String> defaultBranchCandidates;

    /**
     * gets a copy of this config tailored to be used by
     * a worker thread
     */
    public GitConfig forThread(int threadIndex){
        GitConfig clone = new GitConfig();
        clone.setDataConfig(dataConfig);
        clone.setHostname(hostname);
        clone.setLinkedIssueKeyRegexp(linkedIssueKeyRegexp);
        clone.setDefaultBranchCandidates(defaultBranchCandidates);
        clone.setReposDir(reposDir + forksSubDirName + "/" + threadIndex);
        clone.init();
        return clone;
    }

    
    
}
