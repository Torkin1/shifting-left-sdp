package it.torkin.dataminer.config.filters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.filters.global",
    ignoreUnknownFields = false)
@Data
public class IssueFilterConfig {

    @NotNull
    private Boolean applyAnyway;
    
}
