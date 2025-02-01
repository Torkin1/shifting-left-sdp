package it.torkin.dataminer.config.filters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.filters.selected-projects",
    ignoreUnknownFields = false)
@Data
public class SelectedProjectFilterConfig {

    /**
     * If set, only selected projects keys will be processed
     */
    private String[] keys;

}
