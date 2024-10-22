package it.torkin.dataminer.control.workers;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.WorkersConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class WorkersController implements IWorkersController{

    private ExecutorService workers;
    private Queue<Future<?>> batch = new LinkedList<>();

    @Autowired private WorkersConfig workersConfig;
    
    @PostConstruct
    public void init(){
        workers = Executors.newWorkStealingPool(workersConfig.getParallelismLevel());
    }

    @PreDestroy
    public void cleanup(){
        workers.shutdown();
    }

    @Override
    public void submit(Task<?> task) {
        if (isBatchFull())
            throw new RejectedExecutionException("there are already " + batch.size() + " tasks in the batch and the max batch size is " + workersConfig.getTaskBatchSize()); 
        batch.add(workers.submit(task));
    }

    @Override
    public boolean isBatchFull(){
        return batch.size() == workersConfig.getTaskBatchSize();
    }

    @Override
    public boolean isBatchEmpty(){
        return batch.isEmpty();
    }

    @Override
    public Task<?> collect() throws InterruptedException, ExecutionException{
        if (batch.isEmpty()) return null;
        return (Task<?>) batch.poll().get();
    }

    
}
