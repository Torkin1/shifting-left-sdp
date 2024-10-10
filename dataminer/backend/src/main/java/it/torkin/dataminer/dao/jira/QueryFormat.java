package it.torkin.dataminer.dao.jira;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum QueryFormat {
    GET_ISSUE_BY_KEY("https://%s/jira/rest/api/%d/issue/%s?expand=changelog")
    ;

    private final String queryString;

    public String toString() {
        return queryString;
    }
}
