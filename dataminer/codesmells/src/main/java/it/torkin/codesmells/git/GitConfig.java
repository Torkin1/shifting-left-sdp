package it.torkin.codesmells.git;

import java.util.List;

import lombok.Data;

@Data
public class GitConfig {
        
    /** remote hostname where repos are downloaded if not 
     * available locally
     */
    private String hostname;

    /** Dir where repos will be stored */
    private String reposDir;

    /**Used to extract linked issue key in commit message
     */
    private String linkedIssueKeyRegexp;

    /**
     * List of names to try to detect the default branch.
     * The candidates are tried in order and the first one
     * that positively matches is used as default branch.
     * If no candidate is suitable for a project an exception is thrown
     * when creating the corresponding GitDao
     */
    private List<String> defaultBranchCandidates;
    
}
