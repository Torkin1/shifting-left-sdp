package it.torkin.dataminer.toolbox;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Use this to pass a value to update from lambdas
 */
@Data
@AllArgsConstructor
public class Holder<T> {

    private T value;
    
}
