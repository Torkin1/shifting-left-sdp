import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class Resultset<T> implements Iterator<T>, AutoCloseable {
    private FileReader in;
    private MappingIterator<T> records;

    public Resultset(String datafile_path, Class<?> recipientClass) throws UnableToGetResultsetException{

        CsvSchemaBean bean = new CsvSchemaBean();
        bean.setColumnSeparator(',');
        
        init(new File(datafile_path), recipientClass, bean);
        
    }

    public Resultset(File datafile, Class<?> recipientClass, CsvSchemaBean bean) throws UnableToGetResultsetException{
        init(datafile, recipientClass, bean);
    }

    private void init(File datafile, Class<?> recipientClass, CsvSchemaBean bean) throws UnableToGetResultsetException{
        CsvSchema schema;
        try {
            in = new FileReader(datafile);
            schema = CsvSchema.emptySchema()
             .withHeader()
             .withColumnSeparator(bean.getColumnSeparator());
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
