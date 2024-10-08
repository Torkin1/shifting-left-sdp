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
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.datasources",
    ignoreUnknownFields = false)
@Data
@Slf4j
public class DatasourceGlobalConfig {

    /**
     * we use half of available cores to account for hyperthreading and to leave
     * some room for other processes
     */
    private static final int DEFAULT_MAX_WORKERS = Runtime.getRuntime().availableProcessors() / 2;

    @PostConstruct
    private void init() {
        if (parallelismLevel == null || parallelismLevel > DEFAULT_MAX_WORKERS) {
            log.warn("Max workers not set or set too high, using default value {}", DEFAULT_MAX_WORKERS);
            parallelismLevel = DEFAULT_MAX_WORKERS;
        }
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

    /**
     * How many commits we can load from datasources before storing them to db
     */
    @NotNull
    private Integer commitBatchSize;

    @Min(0)
    /** How many workers can be active at max to process commits.
     * A value of 0 means that loading of commits is done in the main thread.
     */
    private Integer parallelismLevel;

    /**
     * How many milliseconds the main thread waits before retrying to submit a processing
     * commit task if this one has been rejected by the workers
     */
    @Min(0)
    private Long taskSubmitRetryTimeout;

    /**
     * How many retries the main thread will do before giving up on submitting a processing
     * commit task
     */
    @Min(0)
    private Integer taskSubmitMaxRetries = 3;
}
