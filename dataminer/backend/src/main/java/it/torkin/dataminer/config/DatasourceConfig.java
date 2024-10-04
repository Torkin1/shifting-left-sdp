package it.torkin.dataminer.config;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasourceConfig {


    @Autowired private DatasourceGlobalConfig config;

    @PostConstruct
    private void init() {
        path = config.getDir() + path;
    
        if (snoringPercentage < 0.0 || snoringPercentage > 100.0){
            throw new IllegalArgumentException("snoringPercentage must be between 0 and 100, but is " + snoringPercentage);  
        }
    }   
    
    /**Specify this if expected dataset size is known  */
    private Integer expectedSize;

    /**
     * name of datasource.
     */
    @NotBlank
    private String name;

    /** Path to folder containing datasource, starting from the
     * root of the datasources directory.
     */
    @NotBlank
    private String path;
    
    /** What percentage of most recent issues per project we want
     * to filter out from the processed dataset resulted from the
     * loading of this datasource.
     */
    @NotNull
    private Double snoringPercentage;
    
}
