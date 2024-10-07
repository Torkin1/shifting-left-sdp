package it.torkin.dataminer.control.dataset.processed.filters;

import java.util.function.Function;

import jakarta.transaction.Transactional;


/**
 * filter used to process a dataset.
 * All filter implementations are called to filter issues, even if issue ha already been
 * flagged to be filtered out, in order to allow filters to update their internal state
 * (e.g. counters).
 */
public abstract class IssueFilter implements Function<IssueFilterBean, Boolean>{

    @Override
    @Transactional
    public final Boolean apply(IssueFilterBean bean){
        beforeApply(bean);
        if (bean.isFiltered() && !bean.isApplyAnyway()) return false;
        return applyFilter(bean);
    }

    /**
     * Implementations can override this method to perform some operations
     * before applying the filter (e.g. updating internal counters).
     * @param bean
     */
    protected void beforeApply(IssueFilterBean bean) {};

    /**
     * Resets internal state of the filter.
     * Implementations must override this method if they keep a state that
     * must be reset before processing issues coming from a new dataset.
     */
    public void reset() {};

    /**
     * Implementations must override this method to apply the filter.
     * Filter is applied only if issue has not been already filtered out,
     * or if applyAnyway flag has been set.
     */
    protected abstract Boolean applyFilter(IssueFilterBean bean);
    
}
