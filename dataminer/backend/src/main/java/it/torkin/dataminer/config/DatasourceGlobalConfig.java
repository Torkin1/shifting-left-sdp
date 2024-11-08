package it.torkin.dataminer.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.datasources",
    ignoreUnknownFields = false)
@Data
public class DatasourceGlobalConfig {

    @PostConstruct
    private void init() {
        sources.forEach((source) -> {
            source.init(this);
            sourcesMap.put(source.getName(), source);
        });
    }

    /**
     * Path to dir containing datasources
     */
    @NotBlank
    private String dir;
    
    /**
     * Each desired datasource must come with a config object
     * listed here
     */
    @NotNull
    private List<DatasourceConfig> sources = new ArrayList<>();

    /**
     * Mirror of sources list, but indexed by name.
     */
    private Map<String, DatasourceConfig> sourcesMap = new HashMap<>();

    /**
     * Package containing datasources implementations
     */
    @NotBlank
    private String implPackage;
}
