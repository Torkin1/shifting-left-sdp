package it.torkin.dataminer.config;

import java.io.File;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix="dataminer.measurement",
    ignoreUnknownFields=false)
@Data
public class MeasurementConfig {

    /**
     * List of measurement dates implementation that can be used
     */
    @NotEmpty
    private Set<String> dates;

    @Autowired private DataConfig dataConfig;

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(dir)){
            dir = dataConfig.getOutputDir() + "/measurements";
        }
        new File(dir).mkdirs();
    }
    
    private String dir;

    public String getOutputFileName(String dataset, String project, String measurementDate){
        return dir + "/" + dataset + "_" + project + "_" + measurementDate + ".csv";
    }

    
}
