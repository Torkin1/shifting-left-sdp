package it.torkin.dataminer.control.dataset.processed.filters;

import java.util.function.Function;

import it.torkin.dataminer.entities.dataset.Issue;

/**
 * Stateless filter used to process a dataset.
 */
public interface IssueFilter extends Function<Issue, Boolean>{
    
}
