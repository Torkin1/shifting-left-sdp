package it.torkin.dataminer.control.dataset;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.DatasourceConfig;
import it.torkin.dataminer.config.DatasourceGlobalConfig;
import it.torkin.dataminer.control.dataset.processed.IProcessedDatasetController;
import it.torkin.dataminer.control.dataset.processed.ProcessedIssuesBean;
import it.torkin.dataminer.control.dataset.raw.IRawDatasetController;
import it.torkin.dataminer.control.dataset.raw.UnableToCreateRawDatasetException;
import it.torkin.dataminer.control.dataset.raw.UnableToFindDatasourceImplementationException;
import it.torkin.dataminer.control.dataset.raw.UnableToInitDatasourceException;
import it.torkin.dataminer.control.dataset.raw.UnableToLoadCommitsException;
import it.torkin.dataminer.control.dataset.raw.UnableToPrepareDatasourceException;
import it.torkin.dataminer.dao.datasources.Datasource;
import it.torkin.dataminer.dao.local.DatasetDao;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

import jakarta.transaction.Transactional;
import it.torkin.dataminer.entities.dataset.Issue;
import java.util.stream.Stream;

@Service
@Slf4j
public class DatasetController implements IDatasetController {

    @Autowired private IRawDatasetController rawDatasetController;
    @Autowired private IProcessedDatasetController processedDatasetController;
    @Autowired private DatasetDao datasetDao;
    @Autowired private DatasourceGlobalConfig datasourceGlobalConfig;

    private List<Datasource> datasources = new ArrayList<>();
    
    @Override
    public void createRawDataset() throws UnableToCreateRawDatasetException {

        Datasource datasource;
        DatasourceConfig config;

        try (ProgressBar progress = new ProgressBar("loading datasources", datasources.size())) {
            
            prepareDatasources();
            
            for (int i = 0; i < datasources.size(); i++) {
                
                datasource = datasources.get(i);
                config = datasourceGlobalConfig.getSources().get(i);
                progress.setExtraMessage(config.getName());

                if(!datasetDao.existsByName(config.getName())){
                    log.info("loading datasource {}", config.getName());
                    rawDatasetController.loadDatasource(datasource, config);
                }
                else {
                    log.warn("Datasource {} already exists in the database. Skipping.", config.getName());
                }

                datasource.close();
                progress.step();

            }
        } catch (UnableToPrepareDatasourceException | UnableToLoadCommitsException e) {
            throw new UnableToCreateRawDatasetException(e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            if (e instanceof RuntimeException)
                throw new UnableToCreateRawDatasetException(e);
        }
    }

    /**
     * Assures that
     * all specified datasources are accessible and ready to be mined.
     * @throws UnableToPrepareDatasourceException 
     */
    private void prepareDatasources() throws UnableToPrepareDatasourceException{

        Datasource datasource;
        
        for (DatasourceConfig config : datasourceGlobalConfig.getSources()) {

            
            try {
                datasource = findDatasourceImpl(config);
                datasource.init(config);
                datasources.add(datasource);
            } catch (UnableToFindDatasourceImplementationException | UnableToInitDatasourceException e) {
                throw new UnableToPrepareDatasourceException(config.getName(), e);
            }

        }

    }

    /**
     * Gets the implementation of the datasource specified in the config using
     * reflection.
     * @param config
     * @return
     * @throws UnableToFindDatasourceImplementationException
     */
    private Datasource findDatasourceImpl(DatasourceConfig config) throws UnableToFindDatasourceImplementationException {

        String implName;
        Datasource datasource;

            try {
                implName = implNameFromConfig(config);
                datasource = (Datasource) Class.forName(implName).getDeclaredConstructor().newInstance();
                return datasource;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ClassNotFoundException e) {
                throw new UnableToFindDatasourceImplementationException(config.getName(), e);
            }

    }

    private String implNameFromConfig(DatasourceConfig config) {
        return String.format("%s.%s%s", datasourceGlobalConfig.getImplPackage(),
         config.getName().substring(0, 1).toUpperCase(), config.getName().substring(1));
    }

    @Override
    @Transactional
    public void getProcessedIssues(ProcessedIssuesBean bean) {
        processedDatasetController.getFilteredIssues(bean);
        try (Stream<Issue> processedIssues = bean.getProcessedIssues()){
            log.info("Processed issues count: " + bean.getProcessedIssues().count());
        }
        log.info(bean.toString());
    }
}
