package it.torkin.dataminer.control.dataset.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.dataset.IDatasetController;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;

@Service
public class DatasetController implements IDatasetController {

    @Autowired private IRawDatasetController rawDatasetController;
    
    @Override
    public void createRawDataset() throws UnableToCreateRawDatasetException {

        rawDatasetController.createRawDataset();
    }
    
}
