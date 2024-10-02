package it.torkin.dataminer.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.data.output.stats",
    ignoreUnknownFields = false)
@Data
public class StatsConfig {
    
    @Autowired private DataConfig dataConfig;

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(dir)){
            dir = dataConfig.getOutputDir() + "/stats";
        }
        if (StringTools.isBlank(repositoriesStats)){
            repositoriesStats = dir + "/repositories.csv";
        }
        if (StringTools.isBlank(projectsStats)){
            projectsStats = dir + "/projects.csv";
        }
        new File(dir).mkdirs();
    }
    
    private String dir;

    private String repositoriesStats;
    private String projectsStats;
}
