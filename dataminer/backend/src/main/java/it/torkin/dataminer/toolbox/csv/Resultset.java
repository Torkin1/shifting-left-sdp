package it.torkin.dataminer.toolbox.csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class Resultset<T> implements Iterator<T>, AutoCloseable {
    private final FileReader in;
    private final MappingIterator<T> records;

    public Resultset(String datafile_path, Class<?> recipientClass) throws UnableToGetResultsetException{

        this(new File(datafile_path), recipientClass);
        
    }

    public Resultset(File datafile, Class<?> recipientClass) throws UnableToGetResultsetException{
        CsvSchema schema;
        try {
            in = new FileReader(datafile);
            schema = CsvSchema.emptySchema().withHeader();
            this.records = new CsvMapper()
             .readerFor(recipientClass)
             .with(schema)
             .readValues(in);
            
        } catch (IOException e) {
            throw new UnableToGetResultsetException(datafile, e);
        }
    }

    @Override
    public void close() throws IOException{
        in.close();
    }

    @Override
    public boolean hasNext() {
        return records.hasNext();
    }

    @Override
    public T next() {
        return records.next();
    }

    public void reset() throws IOException{
        in.reset();
    }
}
