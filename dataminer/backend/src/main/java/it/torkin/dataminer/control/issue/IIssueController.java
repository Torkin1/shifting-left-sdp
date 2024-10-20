package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.entities.dataset.IssueBugginessBean;

public interface IIssueController {

    /**
     * Returns if the issue has at least one buggy commit belonging
     * to the given dataset.
     * A measurement date can be specified to consider only the commits
     * strictly before that date.
     * @param dataset non null
     * @param issue non null
     * @param measurementDate nullable (fallback to now)
     */
    public boolean isBuggy(IssueBugginessBean bean);

    /**
     * Returns the description of the issue according to the given
     * measurement date.
     * @param dataset nullable (ignored)
     * @param issue non null
     * @param measurementDate non null
     * @return
     */
    public String getDescription(IssueBean bean);

    String getTitle(IssueBean bean);
}
