package it.torkin.dataminer.control.workers;

import java.util.concurrent.Callable;

import org.springframework.util.function.ThrowingConsumer;

import lombok.Data;

@Data
public class Task<T> implements Callable<Task<T>>{

    private final ThrowingConsumer<T> task;
    private final T taskBean;

    /**
     * Set if the task execution threw an exception
     */
    private Exception exception;
    
    @Override
    public final Task<T> call() throws Exception {

        try {
            task.acceptWithException(taskBean);
        } catch (Exception e) {
            exception = e;
        }

        return this;
    }


    
}
