package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.git",
    ignoreUnknownFields = false)
@Data
public class GitConfig {

    /** remote hostname where repos are downloaded if not 
     * available locally
     */
    @NotBlank
    private String hostname;

    /** Dir where repos will be stored */
    @NotBlank
    private String reposDir;

    /**Used to extract linked issue key in commit message
     */
    @NotBlank
    private String linkedIssueKeyRegexp;
    
}
