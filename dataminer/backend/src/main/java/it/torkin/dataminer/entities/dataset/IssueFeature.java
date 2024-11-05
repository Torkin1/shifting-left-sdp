package it.torkin.dataminer.entities.dataset;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueFeature {
    
    BUGGY_SIMILARITY("Buggy similarity"),
    ANFIC("Assignee ANFIC")
    ;

    private final String name;

    @Override
    public String toString(){
        return this.name;
    }
}
