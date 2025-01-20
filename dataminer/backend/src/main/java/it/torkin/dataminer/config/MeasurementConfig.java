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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Configuration
@ConfigurationProperties(
    prefix="dataminer.measurement",
    ignoreUnknownFields=false)
@Data
public class MeasurementConfig {

    @RequiredArgsConstructor
    @Getter
    public static enum Treatments{
        COMMIT("JIT"),
        ISSUE_COMMIT("TJIT"),
        NOT_YET_IMPLEMENTED("NYI"),
        NOT_YET_ASSIGNED("NYA"),
        JUST_COMPLETED("JC"),
        ;

        private final String code;

        @Override
        public String toString(){
            return code;
        }
    }
    
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

    public String getOutputFileName(String dataset, String project, String measurementDate, Treatments treatment){
        return dir + "/" + dataset + "_" + project + "_" + measurementDate + "_" + treatment + ".csv";
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

    /**
     * Optional Lower bound for normalized values.
     */
    private Double printLowerBound = Double.NEGATIVE_INFINITY;

    /**
     * Optional Upper bound for normalized values.
     */
    private Double printUpperBound = Double.POSITIVE_INFINITY;
    
}
