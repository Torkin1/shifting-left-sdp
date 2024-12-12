package it.torkin.dataminer.toolbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Use this to pass a value to update from lambdas
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Holder<T> {

    private T value = null;
    
}
