package it.torkin.dataminer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.entities.dataset.Dataset;
import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import lombok.Data;

import java.io.File;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.fork",
    ignoreUnknownFields = false)
@Data
public class ForkConfig {

    @Autowired private DataConfig dataConfig;
    @Autowired private WorkersConfig workersConfig;
    
    @PostConstruct
    public void init(){
        if (StringTools.isBlank(dir)){
            dir = dataConfig.getDir();
        }
        
    }
    
    private String dir;

    private Integer index;

    public boolean isChild(){
        return index != null;
    }


    public String getForkDir(Integer i){
        File forkDir = new File(dir + "/forks/" + i);
        forkDir.mkdirs();
        return forkDir.getAbsolutePath();
    }


    public int getForkCount(){
        return workersConfig.getParallelismLevel();
    }

    public String getForkInputFile(Integer i, Dataset dataset, MeasurementDate measurementDate) {
        return getForkDir(i) + "/" + dataset.getName() + "_" + measurementDate.getName().toString() + ".issues";
    }
    

}
