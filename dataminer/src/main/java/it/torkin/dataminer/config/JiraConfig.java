package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix="dataminer.jira",
    ignoreUnknownFields=false)
@Data
public class JiraConfig {
    
    /** Name of the Jira API remote host */
    private String hostname;
    /** Api version */
    private int apiVersion;
}
