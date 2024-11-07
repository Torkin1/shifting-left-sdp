package it.torkin.dataminer.entities.ephemereal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueFeature {
    
    BUGGY_SIMILARITY("Buggy similarity"),
    ANFIC("Assignee ANFIC"),
    TEMPORAL_LOCALITY("Temporal locality")
    ;

    private final String name;

    @Override
    public String toString(){
        return this.name;
    }
}
