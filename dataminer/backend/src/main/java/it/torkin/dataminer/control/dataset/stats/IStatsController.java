package it.torkin.dataminer.control.dataset.stats;

import java.io.IOException;

public interface IStatsController {

    /**
     * Collects all implemented statistics about stored datasets
     * and prints results to CSV files
     * @throws IOException
     */
    public void printStatsToCSV() throws IOException;
    
}
