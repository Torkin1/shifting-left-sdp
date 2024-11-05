package it.torkin.dataminer.control.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueFeatures {
    
    BUGGY_SIMILARITY("Buggy similarity"),
    ANFIC("Assignee ANFIC")
    ;

    private final String name;

    @Override
    public String toString(){
        return this.name;
    }
}
