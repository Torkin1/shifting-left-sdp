package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.JiraConfig;
import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.jira.JiraDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssueHistoryItem;
import it.torkin.dataminer.entities.jira.issue.IssueStatus;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import it.torkin.dataminer.toolbox.time.TimeTools;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{
    
    /**
     * Gets the value of an issue field according to a given measurement date.
     * The value of the field is first searched in the issue changelog,
     * looking for the last history that is not after the measurement date.
     * If such a history is found, the value is extracted from it as a String,
     * and it is mapped to the wanted type using the provided function
     * {@code mapStringToFieldValue} (Null values are converted to empty strings
     * before they are passed to such function).
     * If no history is found, the value is taken from the issue details using
     * the provided function {@code getValueFromDetails} (Null values must be handled in such function).
     *  
     */
    @RequiredArgsConstructor
    private class IssueFieldGetter<F> implements Function<IssueFieldGetterBean, F> {
        
        /**
         * The caller knows which issue details attribute corresponds
         * to the wanted field, so it provides a way to get it
         */
        private final Function<IssueFields, F> getValueFromDetailsFields;
        /**
         * from|to field values are stored in the changelogs as strings,
         * so the caller needs to provide a way to parse them
         */
        private final Function<List<HistoryEntry>, F> mapEntryToFieldValue;
        
        @Override
        public F apply(IssueFieldGetterBean bean) {
            
            if (!checkMeasurementDate(bean.getIssueBean(), bean.getIssueField())) return null;
    
            IssueHistory history = findLastHistory(bean.getIssueBean(), bean.getIssueField());
            if (history == null) {
                // no changes applied to description since the opening of the issue
                return getValueFromDetailsFields.apply(bean.getIssueBean().getIssue().getDetails().getFields());
            } else {
                List<IssueHistoryItem> items = getHistoryItems(history, bean.getIssueField());
                return mapEntryToFieldValue.apply(extractValueFromHistoryItems(items, history.getCreated(), bean.getIssueBean().getMeasurementDate()));
            }
        }
                
    }
        
    @Autowired private JiraConfig jiraConfig;
    
    private Set<String> inProgressIssueStatuses = new HashSet<>();
    private JiraDao jiraDao;

    @PostConstruct
    public void init(){
        jiraDao = new JiraDao(jiraConfig);
    }
    
    @Override
    public Boolean isBuggy(IssueCommitBean bean){
                
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();
        
        return issue.getCommits().stream().anyMatch(commit -> {          
            return commit.isBuggy()
             && commit.getDataset().getName().equals(dataset);
        });
    }

    private int compareMeasurementDate(String datasetName, Issue i1, Issue i2, MeasurementDate measurementDate){
        return measurementDate.apply(new MeasurementDateBean(datasetName, i1))
            .compareTo(measurementDate.apply(new MeasurementDateBean(datasetName, i2)));
    }

    @Override
    public int compareMeasurementDate(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate());
    }

    @Override
    public boolean isBefore(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate()) < 0;
    }

    @Override
    public boolean isAfter(IssueMeasurementDateBean bean){
        return compareMeasurementDate(
            bean.getDataset(),
            bean.getI1(),
            bean.getI2(),
            bean.getMeasurementDate()) > 0;
    }

    @Override
    public Commit getFirstCommit(IssueCommitBean bean) {
        return bean.getIssue().getCommits().stream()
            .filter(commit -> commit.getDataset().getName().equals(bean.getDataset()))
            .sorted((c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getDescription(IssueBean bean) {
        
        return new IssueFieldGetter<String>(
            fields -> fields.getDescription() == null? "" : fields.getDescription(),
            entries -> entries.get(0).getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.DESCRIPTION));        
    }

    @Override
    public String getTitle(IssueBean bean){
        return new IssueFieldGetter<String>(
            fields -> fields.getSummary() == null? "" : fields.getSummary(),
            entries -> entries.get(0).getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.SUMMARY));
    }

    @Override
    public String getAssigneeKey(IssueBean bean) {
        return new IssueFieldGetter<String>(
            fields -> fields.getAssignee() == null? "" : fields.getAssignee().getKey(),
            entries -> entries.get(0).getValue()
        ).apply(new IssueFieldGetterBean(bean, IssueField.ASSIGNEE));
    }

    private boolean changelogContains(Issue issue, HistoryEntry example, Timestamp measurementDate, boolean and){
        return issue.getDetails().getChangelog().getHistories().stream()
            .filter(h -> !h.getCreated().after(measurementDate))
            .anyMatch(h -> h.getItems().stream().anyMatch(i -> {
                if (and){
                    return i.getFrom() == example.getValue() && i.getTo() == example.getValue();
                }
                else {
                    return i.getFrom() == example.getValue() || i.getTo() == example.getValue();
                }
            }));
    }
    
    @Override
    public boolean hasBeenAssigned(HasBeenAssignedBean bean) {

        String assigneeKey = getAssigneeKey(new IssueBean(bean.getIssue(), bean.getMeasurementDate()));
        if (assigneeKey == null) return false;
        if (assigneeKey.equals(bean.getAssigneeKey())) return true;
        else return changelogContains(
            bean.getIssue(),
            new HistoryEntry(bean.getAssigneeKey(), null), 
            bean.getMeasurementDate(), 
            false);
        
                
    }

    private void initInProgressIssueStatusCategoryIds() throws UnableToGetResourceException{
        IssueStatus[] issueStatuses = jiraDao.queryIssueStatuses();
        final String inProgress = "In Progress";

        // All statuses with a status category named "In Progress" are considered in progress
        for (IssueStatus status : issueStatuses){
            if (status.getStatusCategory().getName().equals(inProgress)){
                inProgressIssueStatuses.add(status.getJiraId());
            }
        }

    }

    private IssueHistory findLastHistory(IssueBean bean, IssueField field){
    
        // we cannot filter away histories after measurement date yet since we need to check
        // in which case we are (see below)
        List<IssueHistory> histories = getHistories(new IssueBean(bean.getIssue(), TimeTools.now()), field);
        
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

    private List<IssueHistoryItem> getHistoryItems(IssueHistory history, IssueField field){
        return history.getItems().stream()
            .filter(item -> item.getField().equals(field.getName()))
            .toList();
    }

    private HistoryEntry extractValueFromHistoryItem(IssueHistoryItem item, Timestamp historyCreatedDate, Timestamp measurementDate){
        
        if (measurementDate.before(historyCreatedDate)){
            // see case 2 in findFirstHistory()
            return new HistoryEntry(
                item.getFrom() == null? "" : item.getFrom(),
                item.getFromString() == null? "" : item.getFromString());
        } else {
            // see case 3 in findFirstHistory()
            return new HistoryEntry(
                item.getTo() == null? "" : item.getTo(),
                item.getToString() == null? "" : item.getToString());
        }
    }

    private List<HistoryEntry> extractValueFromHistoryItems(List<IssueHistoryItem> items, Timestamp historyCreatedDate, Timestamp measurementDate){
        List<HistoryEntry> entries = new ArrayList<>();
        for (IssueHistoryItem item : items){
            entries.add(extractValueFromHistoryItem(item, historyCreatedDate, measurementDate));
        }
        return entries;
    }

    private boolean checkMeasurementDate(IssueBean bean, IssueField field){
        if (bean.getMeasurementDate() == null){
            throw new NullPointerException(String.format("Cannot get %s of Issue %s without a measurement date. If you want the latest version of data, use TimeTools.now() return value as measurement date.", field.getName(), bean.getIssue().getKey()));
        }
        Timestamp created = bean.getIssue().getDetails().getFields().getCreated();
        if (bean.getMeasurementDate().before(created)){
            log.warn(String.format("The measurement date %s is before the creation date of issue %s (%s). The %s field will be null.", bean.getMeasurementDate(), bean.getIssue().getKey(), created, field.getName()));
            return false;
        }

        return true;
    }

    /**
     * Gets changelog histories related to the given field sorted by creation date.
     * Only histories that are created before the measurement date are returned.
     */
    private List<IssueHistory> getHistories(IssueBean bean, IssueField field){
        return bean.getIssue().getDetails().getChangelog().getHistories().stream()
            // makes sure the histories are ordered by creation date
            .sorted((h1, h2) -> h1.getCreated().compareTo(h2.getCreated()))
            // accept only histories that are not after the measurement date
            .filter(history -> !history.getCreated().after(bean.getMeasurementDate()))
            // accept only histories with at least one item related to the given field
            .filter(history -> changesField(history, field))
            .toList();
    }

    private boolean changesField(IssueHistory history, IssueField field){
        return history.getItems().stream().anyMatch(item -> item.getField().equals(field.getName()));
    }
    
    @Override
    public void getInProgressTemporalSpans(IssueTemporalSpanBean bean) {
        
        // cache in progress issue statuses
        if (inProgressIssueStatuses.isEmpty()){
            try {
                initInProgressIssueStatusCategoryIds();
            } catch (UnableToGetResourceException e) {
                log.error("Unable to get issue statuses", e);
                return;
            }
        }

        // traverse changelog to find in progress timespans boundaries
        // NOTE: if issue is already in progress at the opening date, we do not count it.
        List<IssueHistory> histories = getHistories(new IssueBean(bean.getIssue(), bean.getMeasurementDate()), IssueField.STATUS);
        TemporalSpan span = null;
        for (IssueHistory history : histories){
            for (IssueHistoryItem item : history.getItems()){
                
                if (inProgressIssueStatuses.contains(item.getTo()) && !inProgressIssueStatuses.contains(item.getFrom())){
                    span = new TemporalSpan();
                    span.setStart(history.getCreated());
                }
                else if (inProgressIssueStatuses.contains(item.getFrom()) && !inProgressIssueStatuses.contains(item.getTo())
                    && span != null){
                    span.setEnd(history.getCreated());
                    bean.getTemporalSpans().add(span);
                    span = null;
                }
            }
            
        }
        // if last in progress transition never ended in changelog, we can assume it is still in progress
        // at the measurement date
        // NOTE: Issue could be transitioned from in progress to another state before the measurement date actually
        if (span != null){
            span.setEnd(bean.getMeasurementDate());
            bean.getTemporalSpans().add(span);
        }


    }
    
    @Override
    public Set<String> getComponentsIds(IssueBean bean) {

        List<IssueHistory> histories = getHistories(new IssueBean(bean.getIssue(), TimeTools.now()), IssueField.COMPONENT);
        Set<String> components = new HashSet<>();
        
        /**
         * If no changelog entry relates to the components field,
         * we take them from the issue details
         */
        if (histories.isEmpty()) {
            return bean.getIssue().getDetails().getFields().getComponents()
                .stream()
                .map(component -> component.getJiraId())
                .collect(Collectors.toSet());
        }

        /**
         * search through all histories to find addings and removals of components
         * from null to value --> adding
         * from value to null --> removal
         * Search must stop at the measurement date.
         */
        for (IssueHistory history : histories){
            if (history.getCreated().after(bean.getMeasurementDate())) break;
            for (IssueHistoryItem item : history.getItems()){
                if (item.getFrom() == null && item.getTo() != null){
                    components.add(item.getTo());
                }
                else if (item.getFrom() != null && item.getTo() == null){
                    components.remove(item.getFrom());
                }
            }
        }
        return components;
    }
}
