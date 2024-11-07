package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.entities.dataset.Commit;

public interface IIssueController {

    /**
     * Returns if the issue has at least one buggy commit belonging
     * to the given dataset.
     * A measurement date can be specified to consider only the commits
     * strictly before that date.
     * @param dataset non null
     * @param issue non null
     */
    public boolean isBuggy(IssueCommitBean bean);

    /**
     * Returns the first commit of the issue according to the given
     * dataset, or null if no commits of such dataset are found on this issue.
     * @param bean
     * @return
     */
    public Commit getFirstCommit(IssueCommitBean bean);

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
}
