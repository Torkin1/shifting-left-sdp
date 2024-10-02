package it.torkin.dataminer.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
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

    /**Used to extract linked issue key in commit message
     */
    @NotBlank
    private String linkedIssueKeyRegexp;
    
}
