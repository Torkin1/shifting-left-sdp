package it.torkin.dataminer.config;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.data",
    ignoreUnknownFields = false)
@Data
public class DataConfig {

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(outputDir)){
            outputDir = dir + "/output";
        }
        new File(dir).mkdirs();
        new File(outputDir).mkdirs();
    }
    
    @NotBlank
    private String dir;

    private String outputDir;
    
}
