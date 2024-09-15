package it.torkin.dataminer.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.datasources",
    ignoreUnknownFields = false)
@Data
public class DatasourceGlobalConfig {


    /**Each desired datasource must come with a config object
     * listed here
     */
    @NotNull
    private List<DatasourceConfig> sources = new ArrayList<>();

    /**Package containing datasources implementations */
    @NotBlank
    private String implPackage;
}
