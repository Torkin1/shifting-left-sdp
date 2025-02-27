package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.control.measurementdate.MeasurementDate;
import it.torkin.dataminer.control.measurementdate.MeasurementDateBean;
import it.torkin.dataminer.dao.local.IssuePriorityDao;
import it.torkin.dataminer.dao.local.IssueStatusDao;
import it.torkin.dataminer.dao.local.IssueTypeDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.issue.IssueAttachment;
import it.torkin.dataminer.entities.jira.issue.IssueComment;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssueHistoryItem;
import it.torkin.dataminer.entities.jira.issue.IssuePriority;
import it.torkin.dataminer.entities.jira.issue.IssueStatus;
import it.torkin.dataminer.entities.jira.issue.IssueType;
import it.torkin.dataminer.entities.jira.issue.IssueWorkItem;
import it.torkin.dataminer.rest.UnableToGetResourceException;
import it.torkin.dataminer.toolbox.string.StringTools;
import it.torkin.dataminer.toolbox.time.TimeTools;
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
    private class IssueFieldGetter<F> implements Function<IssueFieldBean, F> {
        
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
        public F apply(IssueFieldBean bean) {
            
            if (!checkMeasurementDate(bean.getIssueBean(), bean.getIssueField())) return null;
    
            List<IssueHistory> histories = findHistories(bean.getIssueBean(), bean.getIssueField());
            if (histories.isEmpty()) {
                // no changes applied to wanted field since the opening of the issue
                return getValueFromDetailsFields.apply(bean.getIssueBean().getIssue().getDetails().getFields());
            } else {
                IssueHistory history = histories.get(histories.size() - 1);
                List<IssueHistoryItem> items = getHistoryItems(history, bean.getIssueField());
                return mapEntryToFieldValue.apply(extractValueFromHistoryItems(items, history.getCreated(), bean.getIssueBean().getMeasurementDate()));
            }
        }
                
    }

    /**
     * Searches all value a field had until the measurement date.
     * See {@link IssueFieldGetter} for more details.
     */
    @RequiredArgsConstructor
    private class IssueFieldChangesGetter<F> implements Function<IssueFieldBean, Set<F>> {
        private final Function<IssueFields, F> getValueFromDetailsFields;
        private final Function<List<HistoryEntry>, F> mapEntryToFieldValue;
        
        @Override
        public Set<F> apply(IssueFieldBean bean) {
            List<IssueHistory> histories = findHistories(bean.getIssueBean(), bean.getIssueField());
            Set<F> changeset = new HashSet<>();

            for (IssueHistory history : histories){
                List<IssueHistoryItem> items = getHistoryItems(history, bean.getIssueField());
                List<HistoryEntry> entries = extractValueFromHistoryItems(items, history.getCreated(), bean.getIssueBean().getMeasurementDate());
                changeset.add(mapEntryToFieldValue.apply(entries));
            }

            F valueFromDetails = getValueFromDetailsFields.apply(bean.getIssueBean().getIssue().getDetails().getFields());
            changeset.add(valueFromDetails);
            return changeset;
        }
    }

        
    @Autowired private IssueStatusDao issueStatusDao;
    @Autowired private IssuePriorityDao issuePriorityDao;
    @Autowired private IssueTypeDao issueTypeDao;
    
    private Set<String> inProgressIssueStatuses = new HashSet<>();
    
    @Override
    public Boolean isBuggy(IssueCommitBean bean){
                
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();

        Timestamp measurementDate = bean.getMeasurementDate() != null? bean.getMeasurementDate() : TimeTools.now();
        
        return issue.getCommits().stream().anyMatch(commit -> {          
            return commit.isBuggy()
             && !commit.getTimestamp().after(measurementDate)
             && commit.getDataset().getName().equals(dataset);
        });
    }

    private Timestamp applyMeasurementDate(Issue issue, IssueMeasurementDateBean bean){
        
        String datasetName = bean.getDataset();
        MeasurementDate measurementDate = bean.getMeasurementDate();
        MeasurementDate fallbackDate = bean.getFallbackDate();
        
        Optional<Timestamp> measurementDateOptional = measurementDate.apply(new MeasurementDateBean(datasetName, issue));
        if (fallbackDate == null){
            // fallback date disabled
            return measurementDateOptional.orElseThrow(() -> new MissingMeasurementDateException(datasetName, issue, measurementDate));
        } else {
            // fallback date enabled
            return measurementDateOptional.orElseGet(() -> {
                bean.getFellBackIssueKeys().add(issue.getKey());
                return fallbackDate.apply(new MeasurementDateBean(datasetName, issue)).get();
            });
        }
    }
    
    @Override
    public int compareMeasurementDate(IssueMeasurementDateBean bean){
        Issue i1 = bean.getI1();
        Issue i2 = bean.getI2();

        Timestamp date1 = applyMeasurementDate(i1, bean);
        Timestamp date2 = applyMeasurementDate(i2, bean);

        return date1.compareTo(date2);
    }

    @Override
    public boolean isBefore(IssueMeasurementDateBean bean){
        return compareMeasurementDate(bean) < 0;
    }

    @Override
    public boolean isAfter(IssueMeasurementDateBean bean){
        return compareMeasurementDate(bean) > 0;
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
        ).apply(new IssueFieldBean(bean, IssueField.DESCRIPTION));        
    }

    @Override
    public String getTitle(IssueBean bean){
        return new IssueFieldGetter<String>(
            fields -> fields.getSummary() == null? "" : fields.getSummary(),
            entries -> entries.get(0).getValueString()
        ).apply(new IssueFieldBean(bean, IssueField.SUMMARY));
    }

    @Override
    public String getAssigneeKey(IssueBean bean) {
        return new IssueFieldGetter<String>(
            fields -> fields.getAssignee() == null? "" : fields.getAssignee().getKey(),
            entries -> entries.get(0).getValue()
        ).apply(new IssueFieldBean(bean, IssueField.ASSIGNEE));
    }

    private boolean changelogContains(Issue issue, HistoryEntry example, Timestamp measurementDate, IssueField issueField, boolean and){
        return issue.getDetails().getChangelog().getHistories().stream()
            .filter(h -> !h.getCreated().after(measurementDate))
            .filter(h -> h.getItems().stream().anyMatch(i -> i.getField().equals(issueField.getName())))
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

        Developer assignee = bean.getIssue().getDetails().getFields().getAssignee();
        if (assignee == null) return false;
        else if (assignee.getKey().equals(bean.getAssigneeKey())) return true;
        else return changelogContains(
            bean.getIssue(),
            new HistoryEntry(bean.getAssigneeKey(), null), 
            bean.getMeasurementDate(),
            IssueField.ASSIGNEE, 
            false);
        
                
    }

    private void initInProgressIssueStatusCategoryIds() throws UnableToGetResourceException{
        List<IssueStatus> issueStatuses = issueStatusDao.findAll();
        final String inProgress = "In Progress";

        // All statuses with a status category named "In Progress" are considered in progress
        for (IssueStatus status : issueStatuses){
            if (status.getStatusCategory().getName().equals(inProgress)){
                inProgressIssueStatuses.add(status.getJiraId());
            }
        }

    }

    /**
     * A smarter version of getHistories() that searches for changes in the given field
     * (see method comments for more details)
     */
    private List<IssueHistory> findHistories(IssueBean bean, IssueField field){
        
        // get list of histories until now that have at least one item related to the given field
        // ordered by creation date
        List<IssueHistory> histories = getHistories(new IssueBean(bean.getIssue(), TimeTools.now()), field);

        /**
         * We can be in one among the following situations:
         * 
         * 1. The list is empty: there is no history related to the given field
         *      => the field never changed since the opening of the issue
         *      => we return an empty list, the wanted value is not present
         *         in the changelog
         * 
         * 2. The list is not empty, but the first history is after the measurement date
         *     => the field did change, but after the measurement date.
         *     => we return the first history, the wanted value is stored in it
         *        and can be accessed using the from|fromString IssueHistoryItem fields
         * 
         * 3. The list is not empty, and the first history is before the measurement date
         *    => the field did change in a time before the measurement date
         *    => we return all histories that are not after the measurement date
         *       since it contains the wanted value in the to|toString IssueHistoryItem fields
         */

        List<IssueHistory> result = new ArrayList<>();
        if (!histories.isEmpty()){
            result.addAll(histories.stream()
            // accept only histories that are not after the measurement date
            .filter(history -> !history.getCreated().after(bean.getMeasurementDate()))
            .toList());
            // if some history survives we are in case 3, otherwise in case 2
            if (result.isEmpty()){
                result.add(histories.get(0));
            }
        }
        return result;
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
    @Override
    public List<IssueHistory> getHistories(IssueBean bean, IssueField field){
        Stream<IssueHistory> histories = getItemsBeforeDate(
            bean.getIssue().getDetails().getChangelog().getHistories(),
            bean.getMeasurementDate(),
            history -> history.getCreated());
        // accept only histories with at least one item related to the given field
        // (if specified)
        if (field != null)
            histories = histories.filter(history -> changesField(history, field)); 
        return histories.toList();
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
        Timespan span = null;
        for (IssueHistory history : histories){
            for (IssueHistoryItem item : history.getItems()){
                
                if (inProgressIssueStatuses.contains(item.getTo()) && !inProgressIssueStatuses.contains(item.getFrom())){
                    span = new Timespan();
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
        // if last in progress transition never ended in this changelog view, we can assume it is still in progress
        // at the measurement date
        // NOTE: Issue could be transitioned from in progress to another state after the measurement date actually
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

    @Override
    public Set<String> getReporterChangeset(IssueBean bean){

        return new IssueFieldChangesGetter<String>(
            fields -> fields.getReporter() == null? "" : fields.getReporter().getKey(),
            entries -> entries.get(0).getValue()
        ).apply(new IssueFieldBean(bean, IssueField.REPORTER));
    }

    @Override
    public Set<String> getAssigneeChangeset(IssueBean bean){

        return new IssueFieldChangesGetter<String>(
            fields -> fields.getAssignee() == null? "" : fields.getAssignee().getKey(),
            entries -> entries.get(0).getValue()
        ).apply(new IssueFieldBean(bean, IssueField.ASSIGNEE));
    }
    
    private <I> Stream<I> getItemsBeforeDate(List<I> items, Timestamp date, Function<I, Timestamp> getItemDate){
        return items.stream()
            .sorted((i1, i2) -> getItemDate.apply(i1).compareTo(getItemDate.apply(i2)))
            .takeWhile(item -> !getItemDate.apply(item).after(date));
    }
    
    @Override
    public List<IssueComment> getComments(IssueBean bean) {
        return getItemsBeforeDate(
            bean.getIssue().getDetails().getFields().getComment().getComments(),
            bean.getMeasurementDate(),
            comment -> comment.getCreated()).toList();
    }

    @Override
    public List<IssueWorkItem> getWorkItems(IssueBean bean) {
        return getItemsBeforeDate(
            bean.getIssue().getDetails().getFields().getWorklog().getWorklogs(),
            bean.getMeasurementDate(),
            worklog -> worklog.getCreated()).toList();
    }

    @Override
    public List<IssueAttachment> getAttachments(IssueBean bean) {
        return getItemsBeforeDate(
            bean.getIssue().getDetails().getFields().getAttachments(),
            bean.getMeasurementDate(),
            attachment -> attachment.getCreated()).toList();
    }

    @Override
    public Set<String> getHistoryAuthors(IssueBean bean) {
        List<IssueHistory> histories = getHistories(bean);
        Set<String> authors = histories.stream()
                .filter(history -> history.getAuthor() != null)
        .map(history -> history.getAuthor().getKey())
        .collect(Collectors.toSet());
        return authors;
    }

    @Override
    public Optional<IssuePriority> getPriority(IssueBean bean) {
        return new IssueFieldGetter<Optional<IssuePriority>>(
            fields -> Optional.ofNullable(fields.getPriority()),
            entries -> issuePriorityDao.findById(entries.get(0).getValue())
        ).apply(new IssueFieldBean(bean, IssueField.PRIORITY));
    }

    @Override
    public Optional<IssueType> getType(IssueBean bean) {
        return new IssueFieldGetter<Optional<IssueType>>(
            fields -> Optional.ofNullable(fields.getIssuetype()),
            entries -> issueTypeDao.findById(entries.get(0).getValue())
        ).apply(new IssueFieldBean(bean, IssueField.TYPE));
    }

    @Override
    public List<IssueHistory> getHistories(IssueBean bean) {
        return getHistories(bean, null);
    }

    @Override
    public List<Commit> getCommits(IssueCommitBean bean) {
        return bean.getIssue().getCommits().stream()
            .filter(commit -> commit.getDataset().getName().equals(bean.getDataset()))
            .filter(commit -> !commit.getTimestamp().after(bean.getMeasurementDate()))
            .toList();
    }

    @Override
    public Boolean fromDataset(IssueCommitBean bean) {
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();

        Timestamp measurementDate = bean.getMeasurementDate() != null? bean.getMeasurementDate() : TimeTools.now();
        
        return issue.getCommits().stream().anyMatch(commit -> {          
            return !commit.getTimestamp().after(measurementDate)
             && commit.getDataset().getName().equals(dataset);
        });
    }
}
