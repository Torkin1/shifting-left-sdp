package it.torkin.dataminer.dao.jira;

import lombok.AllArgsConstructor;

@AllArgsConstructor
// https://issues.apache.org
public enum QueryTemplate {
    GET_ALL_FIXED_BUGS("https://%s/jira/rest/api/%d/search?jql=project='%s'AND'issueType'='Bug'AND('status'='closed'OR'status'='resolved')AND'resolution'='fixed'ORDER BY'created'ASC&fields=created,fixVersions,versions,resolutiondate,created&startAt=%d"),
    GET_ALL_RELEASES("https://%s/jira/rest/api/%d/project/%s/version?orderBy=releaseDate&status=released"),
    GET_ISSUE_BY_KEY("https://%s/jira/rest/api/%d/issue/%s")
    ;

    private final String queryString;

    public String toString() {
        return queryString;
    }
}
