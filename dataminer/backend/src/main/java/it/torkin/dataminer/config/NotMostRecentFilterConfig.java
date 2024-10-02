package it.torkin.dataminer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix="dataminer.issuefilter.notmostrecent",
    ignoreUnknownFields=false)
@Data
public class NotMostRecentFilterConfig {

    /**
     * Percentage of Issues to discard among the most recent ones according to the
     * measurement date in use.
     */
    @NotNull
    @Min(0)
    @Max(100)
    private Double percentage;

}
