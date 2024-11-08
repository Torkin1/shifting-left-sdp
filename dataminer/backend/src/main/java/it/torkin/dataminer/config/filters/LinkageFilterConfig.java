package it.torkin.dataminer.config.filters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.filters.linkage",
    ignoreUnknownFields = false)
@Data
public class LinkageFilterConfig {
    /**
     * Only top N project-dataset pairs ranked by buggy linkage
     * are selected for further processing
     */
    @Min(0)
    private Integer topNBuggyLinkage;
}
