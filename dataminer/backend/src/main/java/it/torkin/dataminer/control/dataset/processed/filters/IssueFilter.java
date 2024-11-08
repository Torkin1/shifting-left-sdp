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
    
    /**
     * If filter needs to maintain a state, it must override this method to create it.
     * This is necessary to avoid state sharing among different streams
     * since the filter instances are singleton.
     * @param bean
     * @return
     */
    protected Object createState(IssueFilterBean bean){
        return null;
    } 
    
    @Override
    @Transactional
    public final Boolean apply(IssueFilterBean bean){
        if (bean.getFilterStates().get(this.getName()) == null){
            bean.getFilterStates().put(this.getName(), createState(bean));
        }
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
     * Implementations must override this method to apply the filter.
     * Filter is applied only if issue has not been already filtered out,
     * or if applyAnyway flag has been set.
     */
    protected abstract Boolean applyFilter(IssueFilterBean bean);

    public final String getName(){
        return this.getClass().getSimpleName().split("\\$\\$")[0];
    }
    
}
