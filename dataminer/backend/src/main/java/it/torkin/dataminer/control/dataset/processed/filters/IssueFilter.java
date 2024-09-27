package it.torkin.dataminer.control.dataset.processed.filters;

import java.util.function.Function;


/**
 * Stateless filter used to process a dataset.
 */
public interface IssueFilter extends Function<IssueFilterBean, Boolean>{
    
}
