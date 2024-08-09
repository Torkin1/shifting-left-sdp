package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix="dataminer.jira",
    ignoreUnknownFields=false)
@Data
public class JiraConfig {
    
    /** Name of the Jira API remote host */
    @NotBlank
    private String hostname;
    /** Api version */
    @NotBlank
    private int apiVersion;
}
