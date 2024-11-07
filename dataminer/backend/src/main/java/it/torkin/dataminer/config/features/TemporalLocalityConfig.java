package it.torkin.dataminer.config.features;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.features.temporal-locality",
    ignoreUnknownFields = false)
@Data
public class TemporalLocalityConfig {

    @PostConstruct
    public void init(){
        if (windowSize < 0.0 || windowSize > 100.0){
            throw new IllegalArgumentException("temporal locality window size must be between 0 and 100, but is " + windowSize);  
        }
    }

    /**
     * Percentage of issues to include in the temporal locality window.
     */
    @NotNull
    private Double windowSize;
    
}
