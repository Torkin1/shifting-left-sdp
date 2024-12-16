package it.torkin.dataminer.control.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Issue features are metrics that can be informative about the issue's bugginess.
 * Each feature is responsibility of a FeatureMiner, which can measure several variants 
 * of the feature.
 */
@Getter
@RequiredArgsConstructor
public enum IssueFeature {
    
    /**
     * similarity score of this ticket among other tickets known as buggy before the measurement date
     */
    BUGGY_SIMILARITY("Buggy similarity", IssueFeatureFamily.R2R),
    /**
     * Buggy tickets each dev has been historically assigned.
     */
    ANFIC("Assignee ANFIC", IssueFeatureFamily.DEVELOPER),
    /**
     * buggy tickets introduced in the last x% of tickets.
     */
    TEMPORAL_LOCALITY("Temporal locality", IssueFeatureFamily.ENVIRONMENTAL_TEMPERATURE),
    /**
     * If the issue has at least one buggy commit linked to it. Target label to predict
     */
    BUGGINESS("Bugginess", null),
    /**
     * Technical debt accumulated in the estimated set of classes impacted by the ticket (worst case: the entire project)
     */
    PROJECT_CODE_QUALITY("Project code quality", IssueFeatureFamily.CODE),
    /**
     * Changes to the codebase introduced while the ticket was in progress
     */
    COMMITS_WHILE_IN_PROGRESS("Commits while in progress", IssueFeatureFamily.ENVIRONMENTAL_TEMPERATURE),
    /**
     * The components the issue is related to.
     * In jira, components are a way to group issues within a project.
     */
    COMPONENTS("Components", IssueFeatureFamily.INTRINSIC),
    /**
     * Features that analyze the issue's description and summary using nlp techniques.
     */
    NLP_DESCRIPTION("NLP Description", IssueFeatureFamily.INTRINSIC),
    /**
     * Features that analyze the issue's sentiment using nlp techniques. 
     */
    NLP_SENTIMENT("NLP Sentiment", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    /**
     * Features that analyze the issue's distinct people involved in issue's lifecycle,
     * such as assignees, reporters, editors, ...
     */
    ISSUE_PARTICIPANTS("Issue Participants", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    /**
     * Features that measure the size of the codebase impacted by the issue.
     */
    PROJECT_CODE_SIZE("Project code size", IssueFeatureFamily.CODE),
    /**
     * How important the issue is to the project (i.e: critical, major, minor, trivial) 
     */
    PRIORITY("Priority", IssueFeatureFamily.INTRINSIC),
    /**
     * The purpose of the issue (i.e: bug, improvement, new feature, ...)
     */
    TYPE("Type", IssueFeatureFamily.INTRINSIC),
    /**
     * The activities the issue has been involved in. Activities
     * can be about issue state change, discussion, implementation efforts, ...
     */
    ACTIVITIES("Activities", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    ;

    private final String name;
    private final IssueFeatureFamily family;

    @Override
    public String toString(){
        return this.getFullName();
    }

    public String getFullName(){
        
        return this.family == null? this.name : this.family.getName() + "-" + this.name;
    }
}
