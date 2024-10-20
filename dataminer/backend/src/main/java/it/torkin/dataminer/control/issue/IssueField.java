package it.torkin.dataminer.control.issue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
enum IssueField {
    DESCRIPTION("description"),
    SUMMARY("summary"),;

    private final String name;
}

