package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.workers",
    ignoreUnknownFields = false)
@Data
@Slf4j
public class WorkersConfig {

    /**
     * we use half of available cores to account for hyperthreading and to leave
     * some room for other processes
     */
    // private static final int DEFAULT_MAX_WORKERS = Runtime.getRuntime().availableProcessors() / 2;
    private static final int DEFAULT_MAX_WORKERS = 1;
    
    @PostConstruct
    private void init() {
        if (parallelismLevel == null || parallelismLevel > DEFAULT_MAX_WORKERS) {
            log.warn("Max workers not set or set too high, using default value {}", DEFAULT_MAX_WORKERS);
            parallelismLevel = DEFAULT_MAX_WORKERS;
        }
    }
    /**
     * How many tasksworkers can accept. To accept more tasks,
     * caller must collect the results of already submitted tasks.
     */
    @NotNull
    private Integer taskBatchSize;

    @Min(1)
    /** How many workers can be active at max to process tasks.
     */
    private Integer parallelismLevel;
    
}
