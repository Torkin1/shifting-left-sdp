package it.torkin.dataminer.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    
}
