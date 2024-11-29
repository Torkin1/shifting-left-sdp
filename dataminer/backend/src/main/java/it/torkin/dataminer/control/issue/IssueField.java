package it.torkin.dataminer.control.issue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IssueField {
    DESCRIPTION("description"),
    SUMMARY("summary"),
    ASSIGNEE("assignee"),
    STATUS("status"),
    COMPONENT("Component"),
    ;

    private final String name;
}

