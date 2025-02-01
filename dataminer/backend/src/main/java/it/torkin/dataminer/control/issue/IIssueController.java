package it.torkin.dataminer.control.issue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.jira.issue.IssueAttachment;
import it.torkin.dataminer.entities.jira.issue.IssueComment;
import it.torkin.dataminer.entities.jira.issue.IssueHistory;
import it.torkin.dataminer.entities.jira.issue.IssuePriority;
import it.torkin.dataminer.entities.jira.issue.IssueType;
import it.torkin.dataminer.entities.jira.issue.IssueWorkItem;

/**
 * Expose methods to get information about issues
 * taking in consideration dataset informations, measurement date,
 * related commits, etc.
 */
public interface IIssueController {

    /**
     * Returns if the issue has at least one buggy commit belonging
     * to the given dataset.
     * A measurement date can be specified to consider only the commits
     * strictly before that date.
     * @param dataset non null
     * @param issue non null
     */
    public Boolean isBuggy(IssueCommitBean bean);

    /**
     * Return true if issue has at least one commit belonging to the
     * given dataset.
     * A measurement date can be specified to consider only the commits
     * strictly before that date.
     */
    public Boolean fromDataset(IssueCommitBean bean);

    /**
     * Returns the first commit of the issue according to the given
     * dataset, or null if no commits of such dataset are found on this issue.
     * @param bean
     * @return
     */
    public Commit getFirstCommit(IssueCommitBean bean);

    /**
     * Returns the list of commits of the issue prior to the given measurement date
     */
    public List<Commit> getCommits(IssueCommitBean bean);

    /**
     * Returns the description of the issue according to the given
     * measurement date.
     * @param issue non null
     * @param measurementDate non null
     * @return
     */
    public String getDescription(IssueBean bean);

    /**
     * Returns the title of the issue at the given measurement date.
     * If no title is found, an empty string is returned.
     * @param bean
     * @return
     */
    public String getTitle(IssueBean bean);

    /**
     * Returns the assignee key of the issue at the given measurement date,
     * or null if no assignee has ever been set for this issue.
     * @param bean
     * @return
     */
    public String getAssigneeKey(IssueBean bean);

    /**
     * Returns true if the given developer has ever been assigned to the issue.
     */
    public boolean hasBeenAssigned(HasBeenAssignedBean bean);

    /**
     * Returns true if issue1 is strictly before issue2 according to
     * the provided measurement date and dataset.
     * @param bean
     * @return
     */
    public boolean isBefore(IssueMeasurementDateBean bean);
    
    /**
     * Returns true if issue1 is strictly after issue2 according to
     * the provided measurement date and dataset.
     * @param bean
     * @return
     */
    public boolean isAfter(IssueMeasurementDateBean bean);

    public int compareMeasurementDate(IssueMeasurementDateBean bean);

    /**
     * Returns a list of temporal spans in which the issue was in progress,
     * sorted from the earliest to the latest.
     */
    public void getInProgressTemporalSpans(IssueTemporalSpanBean bean);

    /**
     * Gets list of issue components ids at measurement date 
     */
    public Set<String> getComponentsIds(IssueBean bean);

    /**
     * Gets set of values the reporter field has taken during the issue lifecycle
     * until the specified measurement date.
     * 
     * @param bean
     * @return
     */
    public Set<String> getReporterChangeset(IssueBean bean);

        /**
     * Gets set of values the assignee field has taken during the issue lifecycle
     * until the specified measurement date.
     * @param bean
     * @return
     */
    public Set<String> getAssigneeChangeset(IssueBean bean);

    /**
     * Gets comments before the specified measurement date
     */
    public List<IssueComment> getComments(IssueBean bean);

    /**
     * Gets worklog items before the specified measurement date
     */
    public List<IssueWorkItem> getWorkItems(IssueBean bean);

    /**
     * Gets attachments before the specified measurement date
     */
    public List<IssueAttachment> getAttachments(IssueBean bean);

    /**
     * Gets sets of developer ids that have authored changes on the issue
     * before the specified measurement date.
     */
    public Set<String> getHistoryAuthors(IssueBean bean);

    /**
     * Gets priority of ticket at measurement date
     */
    public Optional<IssuePriority> getPriority(IssueBean bean);

    /**
     * Gets issue type at measurement date
     */
    public Optional<IssueType> getType(IssueBean bean);

    /**
     * Gets historical changes applied to the issue until the specified measurement date
     */
    public List<IssueHistory> getHistories(IssueBean bean);

    /**
     * Gets historical changes applied to the issue until the specified measurement date
     * for the specified field
     */
    public List<IssueHistory> getHistories(IssueBean bean, IssueField field);
}
