package it.torkin.dataminer.dao.jira;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum QueryFormat {
    GET_ISSUE_BY_KEY("https://%s/jira/rest/api/%d/issue/%s?expand=changelog"),
    GET_ISSUE_STATUSES("https://%s/jira/rest/api/%d/status/"),
    GET_ISSUE_WORKLOG("https://%s/jira/rest/api/%d/issue/%s/worklog"),
    ;

    private final String queryString;

    public String toString() {
        return queryString;
    }
}
