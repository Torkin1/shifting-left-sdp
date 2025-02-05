package it.torkin.dataminer.control.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A Feature family represents an aspect of the issue that can be informative
 * about the issue's bugginess. By refining the family, one can find a specific
 * metric (namely, an IssueFeature) that can help the model predict the issue's bugginess.
 */
@RequiredArgsConstructor
@Getter
public enum IssueFeatureFamily {
    /**
     * Features that take into account differences and similarities
     * among (buggy) issues.
     */
    R2R("R2R"),
    /**
     * Features that take into account how often an issue is touched
     * during its lifecycle, or how often it is involved in debates 
     * and decisions. 
     */
    INTERNAL_TEMPERATURE("Internal_Temperature"),
    /**
     * How often the environment changes during an issue lifecycle.
     */
    ENVIRONMENTAL_TEMPERATURE("External_Temperature"),
    /**
     * The human factor. Features that take into account how a developer's
     * effort can influence the issue bugginess.
     */
    DEVELOPER("Developer"),
    /**
     * The state and quality of the codebase. 
     */
    CODE("Code"),
    /**
     * Some issues can be more difficult, require more effort, or
     * relate to more complex and buggy prone parts of the codebase than
     * others.
     */
    INTRINSIC("Intrinsic"),
    /**
     * Features of the commits related to the issue.
     */
    JIT("JIT"),
    ;

    private final String name;
}
