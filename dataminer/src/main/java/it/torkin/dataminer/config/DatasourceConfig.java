package it.torkin.dataminer.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasourceConfig {


    /**Specify this if expected dataset size is known  */
    private Integer expectedSize;

    /**name of datasource. Must match the name of the folder
     * containing the datasource.
     */
    @NotBlank
    private String name;

    /** Path to folder containing datasource */
    @NotBlank
    private String path;    
    
}
