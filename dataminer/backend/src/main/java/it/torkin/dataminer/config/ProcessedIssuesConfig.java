package it.torkin.dataminer.config;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ConfigurationProperties(
        prefix = "dataminer.processed-issues",
        ignoreUnknownFields = false)
@Data
@Slf4j
public class ProcessedIssuesConfig {

    @Autowired
    private DataConfig dataConfig;

    private String cachedir;

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(cachedir)){
            cachedir = dataConfig.getDir() + File.separator + "processed-issues";
            new File(cachedir).mkdirs();
        }

    }

    public File getCacheFile(String dataset, String measurementDate){
        return new File(cachedir + "_" + dataset + "_" + measurementDate);
    }
}
