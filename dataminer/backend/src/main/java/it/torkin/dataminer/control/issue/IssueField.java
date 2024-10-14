package it.torkin.dataminer.control.issue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
enum IssueField {
    DESCRIPTION("description");

    private final String name;
}

