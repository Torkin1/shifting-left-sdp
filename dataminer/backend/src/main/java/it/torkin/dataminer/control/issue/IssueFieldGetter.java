package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssueHistoryItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gets the value of an issue field according to a given measurement date
 */
@RequiredArgsConstructor
@Slf4j
class IssueFieldGetter<F> implements Function<IssueFieldGetterBean, F> {
    
    /**
     * The caller knows which issue details attribute corresponds
     * to the wanted field, so it provides a way to get it
     */
    private final Function<IssueFields, F> getValueFromDetails;
    /**
     * from|to field values are stored in the changelogs as strings,
     * so the caller needs to provide a way to parse them
     */
    private final Function<String, F> mapStringToFieldValue;
    
    @Override
    public F apply(IssueFieldGetterBean bean) {
        if (!checkMeasurementDate(bean.getIssueBean(), bean.getIssueField())) return null;

        IssueHistory history = findLastHistory(bean.getIssueBean(), bean.getIssueField());
        if (history == null) {
            // no changes applied to description since the opening of the issue
            return getValueFromDetails.apply(bean.getIssueBean().getIssue().getDetails().getFields());
        } else {
            IssueHistoryItem item = getLatestHistoryItem(history, bean.getIssueField());
            return mapStringToFieldValue.apply(extractValueFromHistoryItem(item, history.getCreated(), bean.getIssueBean().getMeasurementDate()));
        }
    }
    
    private List<IssueHistory> getHistories(IssueBean bean, IssueField field){
        return bean.getIssue().getDetails().getChangelog().getHistories().stream()
            // makes sure the histories are ordered by creation date
            .sorted((h1, h2) -> h1.getCreated().compareTo(h2.getCreated()))
            // accept only histories that are related to the given field
            .filter(history -> changesField(history, field))
            .toList();
    }

    private boolean changesField(IssueHistory history, IssueField field){
        return history.getItems().stream().anyMatch(item -> item.getField().equals(field.getName()));
    }
    
    private IssueHistory findLastHistory(IssueBean bean, IssueField field){

        List<IssueHistory> histories = getHistories(bean, field);
        
        /**
         * At this point we have a list of histories that have at least one item
         * related to the given field, ordered by creation date.
         * 
         * We can be in one among the following situations:
         * 
         * 1. The list is empty: there is no history related to the given field
         *      => the field never changed since the opening of the issue
         *      => we return null, the wanted value is not present in the changelog
         * 
         * 2. The list is not empty, but the first history is after the measurement date
         *     => the field did change, but after the measurement date.
         *     => we return the first history, the wanted value is stored in it
         *        and can be accessed using the from|fromString IssueHistoryItem fields
         * 
         * 3. The list is not empty, and the first history is before the measurement date
         *    => the field did change in a time following the measurement date
         *    => we return the last history that is not after the measurement date
         *       since it contains the wanted value in the to|toString IssueHistoryItem fields
         */

        if (histories.isEmpty()) return null;   // case 1
        else {
            return histories.stream()
                // accept only histories that are not after the measurement date
                .filter(history -> !history.getCreated().after(bean.getMeasurementDate()))
                // if some history survives we are in case 3, otherwise in case 2
                .reduce((h1, h2) -> h2)
                .orElse(histories.get(0));
        }
        
    }

    private IssueHistoryItem getLatestHistoryItem(IssueHistory history, IssueField field){
                
        // Since the history items are not ordered by creation date, we search for the last one
        // that is related to the given field 
        ListIterator<IssueHistoryItem> reversedItems = history.getItems().listIterator(history.getItems().size());
        while (reversedItems.hasPrevious()){
            IssueHistoryItem item = reversedItems.previous();
            if (item.getField().equals(field.getName())){
                return item;
            }
        }
        throw new RuntimeException("Cannot find an item related to " + field.getName() + " in history " + history);
    }

    private String extractValueFromHistoryItem(IssueHistoryItem item, Timestamp historyCreatedDate, Timestamp measurementDate){
        if (measurementDate.before(historyCreatedDate)){
            // see case 2 in findFirstHistory()
            return item.getFromString();
        } else {
            // see case 3 in findFirstHistory()
            return item.getToString();
        }
    }

    private boolean checkMeasurementDate(IssueBean bean, IssueField field){
        if (bean.getMeasurementDate() == null){
            throw new NullPointerException(String.format("Cannot get %s of Issue %s without a measurement date. If you want the latest version of data, use TimeTools.now() return value as measurement date.", field.getName(), bean.getIssue().getKey()));
        }

        if (bean.getMeasurementDate().before(bean.getIssue().getDetails().getFields().getCreated())){
            log.warn(String.format("The measurement date %s is after the creation date of issue %s. The %s field will be null.", bean.getMeasurementDate(), bean.getIssue().getKey(), field.getName()));
            return false;
        }

        return true;
    }
}
