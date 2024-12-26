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
    BUGGY_SIMILARITY("NLP:buggy_similarity", IssueFeatureFamily.R2R),
    /**
     * Metrics describing the developer perfomances.
     */
    ASSIGNEE("assignee", IssueFeatureFamily.DEVELOPER),
    /**
     * buggy tickets introduced in the last x% of tickets.
     */
    TEMPORAL_LOCALITY("temporal_locality", IssueFeatureFamily.ENVIRONMENTAL_TEMPERATURE),
    /**
     * If the issue has at least one buggy commit linked to it. Target label to predict
     */
    BUGGINESS("bugginess", null),
    /**
     * Technical debt accumulated in the estimated set of classes impacted by the ticket (worst case: the entire project)
     */
    CODE_QUALITY("code_quality", IssueFeatureFamily.CODE),
    /**
     * Changes to the codebase introduced while the ticket was in progress
     */
    COMMITS_WHILE_IN_PROGRESS("commits_while_in_progress", IssueFeatureFamily.ENVIRONMENTAL_TEMPERATURE),
    /**
     * The components the issue is related to.
     * In jira, components are a way to group issues within a project.
     */
    COMPONENTS("components", IssueFeatureFamily.INTRINSIC),
    /**
     * Features that analyze the issue's description and summary using nlp techniques.
     */
    NLP_DESCRIPTION("nlp4re_description", IssueFeatureFamily.INTRINSIC),
    /**
     * Features that analyze the issue's sentiment using nlp techniques. 
     */
    NLP_SENTIMENT("nlp4re_sentiment", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    /**
     * Features that analyze the issue's distinct people involved in issue's lifecycle,
     * such as assignees, reporters, editors, ...
     */
    ISSUE_PARTICIPANTS("issue_Participants", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    /**
     * Features that measure the size of the codebase impacted by the issue.
     */
    CODE_SIZE("code_size", IssueFeatureFamily.CODE),
    /**
     * How important the issue is to the project (i.e: critical, major, minor, trivial) 
     */
    PRIORITY("priority", IssueFeatureFamily.INTRINSIC),
    /**
     * The purpose of the issue (i.e: bug, improvement, new feature, ...)
     */
    TYPE("type", IssueFeatureFamily.INTRINSIC),
    /**
     * The activities the issue has been involved in. Activities
     * can be about issue state change, discussion, implementation efforts, ...
     */
    ACTIVITIES("activities", IssueFeatureFamily.INTERNAL_TEMPERATURE),
    /**
     * Latest commit submitted to project codebase before the issue measurement date.
     * If such commit changed the project a lot, the integration of the issue implementation
     * could bring new defects.
     */
    LATEST_COMMIT("latest_commit", IssueFeatureFamily.ENVIRONMENTAL_TEMPERATURE),
    ;

    private final String name;
    private final IssueFeatureFamily family;

    @Override
    public String toString(){
        return this.getFullName();
    }

    public String getFullName(){
        
        return this.family == null? this.name : this.family.getName() + ":" + this.name;
    }

    public String getFullName(String variant){
        return this.getFullName() + "-" + variant; 
    }
}
