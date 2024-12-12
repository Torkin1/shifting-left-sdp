package it.torkin.dataminer.config.features;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.features.project-code-quality",
    ignoreUnknownFields = false)
@Data
public class ProjectCodeQualityConfig {

    private String grpcTarget;

    @NotBlank
    private String pmdPath;
    
}
