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

        if (printLogBase != null && (printLogBase <= 0 || printLogBase == 1)){
            throw new IllegalArgumentException("Log base cannot be " + printLogBase);
        }
            
    }
    
    private String dir;

    public String getOutputFileName(String dataset, String project, String measurementDate){
        return dir + "/" + dataset + "_" + project + "_" + measurementDate + ".csv";
    }

    /**
     * If NaN values are a problem, they can be replaced with this value
     * if one has been specified.
     */
    private String printNanReplacement;

    /**
     * Numeric feature values are log normalized before being written to the output file.
     * Set this value to specify the base of the logarithm.
     * If not set, normalization does not take place
     */
    private Double printLogBase;
    
}
