package it.torkin.dataminer.config.features;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.torkin.dataminer.config.DataConfig;
import it.torkin.dataminer.toolbox.string.StringTools;
import jakarta.annotation.PostConstruct;
import lombok.Data;

@Configuration
@ConfigurationProperties(
    prefix = "dataminer.features.nlp",
    ignoreUnknownFields = false)
@Data
public class NLPFeaturesConfig {

    @Autowired private DataConfig dataConfig;

    @PostConstruct
    public void init(){
        if (StringTools.isBlank(dir)){
            dir = dataConfig.getOutputDir() + "/nlp";
        }
        if (StringTools.isBlank(nlpIssueBeans)){
            nlpIssueBeans = dir + "/issue-beans.json";
        }
        new File(dir).mkdirs();
    }
    
    private String dir;

    /**
     * Path to the file containing the serialized issue summaries to be crunched
     * by the NLP remote miners.
     */
    private String nlpIssueBeans;
    
}
