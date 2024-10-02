package it.torkin.dataminer.toolbox.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CsvTools {

    public CsvSchema getSchema(CsvMapper mapper, Class<?> recipientClass){
        return mapper.schemaFor(recipientClass).withUseHeader(true);
    }

}
