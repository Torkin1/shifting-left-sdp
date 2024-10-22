package it.torkin.dataminer.control.workers;

import java.util.concurrent.ExecutionException;

public interface IWorkersController {

    /**
     * Submits a task to the worker pool.
     * 
     * @param workerBean
     */
    public void submit(Task<?> task);

    /**
     * True if there is no more space left for new tasks in batch
     * @return
     */
    public boolean isBatchFull();

    /**
     * True if there are no tasks in the batch
     * @return
     */
    public boolean isBatchEmpty();

    /**
     * Collects the result of a task, or null if batch is empty.
     * @return
     */
    public Task<?> collect() throws InterruptedException, ExecutionException;
}
