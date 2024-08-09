package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.apachejit",
    ignoreUnknownFields = false)
@Data
public class ApachejitConfig{

    /** Path to csv containing commits */
    @NotBlank
    private String commitsPath;
    /** True if dataset should be loaded even if it is already present in db
     * (Even if set to false, csv file will be still parsed 
     *  and new records will be stored)
     */
    @NotNull
    private Boolean refresh;
    /** If true, skips the parsing of csv file,
     *  effectively denying the dataset load*/
    @NotNull
    private Boolean skipLoad;
}