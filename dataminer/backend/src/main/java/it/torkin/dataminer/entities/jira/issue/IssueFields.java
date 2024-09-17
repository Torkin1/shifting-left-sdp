package it.torkin.dataminer.entities.jira.issue; 
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.entities.jira.Component;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.project.Project;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Embeddable
@Data
public class IssueFields{
    
    @Column(columnDefinition = "text")
    private String description;
    private Timestamp updated;
    private Timestamp created;
    private Timestamp resolutiondate;
    private Timestamp duedate;
    private String summary; // issue title
    private int workratio;

    @AttributeOverrides({
        @AttributeOverride(name="self", column=@Column(name="issue_watchers_self")),
    })
    @Embedded private IssueWatcher watcher;
    @Embedded private IssueCommentLog comment;  // make sure to iterate over all pages
    @Embedded private IssueWorkLog worklog; // make sure to iterate over all pages
    @Embedded private IssueTimeTracking timetracking;
    @AttributeOverrides({
        @AttributeOverride(name = "progress", column = @Column(name="issue_progress_aggregate_progress")),
        @AttributeOverride(name = "total", column = @Column(name="issue_progress_aggregate_total"))    
    })
    @Embedded private IssueProgress aggregateprogress;
    @Embedded private IssueProgress progress;
    @AttributeOverrides({
        @AttributeOverride(name="self", column=@Column(name="issue_votes_self")),
    })
    @Embedded private IssueVotes votes;

    @SerializedName("attachment")
    @ElementCollection
    private List<IssueAttachment> attachments = new ArrayList<>();
    
    @ElementCollection
    private List<IssueLink> issuelinks = new ArrayList<>();
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private List<IssuePointer> subtasks = new ArrayList<>();


    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private IssueStatus status;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private IssuePriority priority;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private IssueType issuetype;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private IssueResolution resolution;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer assignee;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer creator;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Developer reporter;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Project project;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Component> components = new ArrayList<>();

}

// NOTE: Removing the issue should remove all objects closely related to it
// such as comments, worklogs, watchers, votes, etc. but NOT
// the project, developer, issue type, issue status, issue priority, etc.