package it.torkin.dataminer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.fork",
    ignoreUnknownFields = false)
@Data
@Slf4j
public class ForkConfig {

    @Autowired private DataConfig dataConfig;
    @Autowired private WorkersConfig workersConfig;

    // we further limit number of cores usable since some miners can spawn processes and we could
    // incur into an OutOfMemoryError
     private static final int DEFAULT_MAX_FORKS = Runtime.getRuntime().availableProcessors() / 4;

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(dir)){
            dir = dataConfig.getDir();
        }

        if (parallelismLevel == null || parallelismLevel > DEFAULT_MAX_FORKS) {
            log.warn("Max forks not set or set too high, using default value {}", DEFAULT_MAX_FORKS);
            parallelismLevel = DEFAULT_MAX_FORKS;
        }
        
    }
    
    private String dir;

    private Integer index;

    @NotNull
    @Min(1)
    private Integer parallelismLevel;

    public boolean isChild(){
        return index != null;
    }


    public String getForkDir(Integer i){
        File forkDir = new File(dir + "/forks/" + i);
        forkDir.mkdirs();
        return forkDir.getAbsolutePath();
    }


    public int getForkCount(){
        return parallelismLevel;
    }

    public String getForkInputFile(Integer i, Dataset dataset, MeasurementDate measurementDate) {
        return getForkDir(i) + "/" + dataset.getName() + "_" + measurementDate.getName().toString() + ".issues";
    }
    

}
