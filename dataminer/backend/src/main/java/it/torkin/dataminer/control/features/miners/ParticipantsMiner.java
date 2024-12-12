package it.torkin.dataminer.control.features.miners;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.torkin.dataminer.entities.jira.Developer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.torkin.dataminer.control.features.FeatureMiner;
import it.torkin.dataminer.control.features.FeatureMinerBean;
import it.torkin.dataminer.control.features.IssueFeature;
import it.torkin.dataminer.control.issue.IIssueController;
import it.torkin.dataminer.control.issue.IssueBean;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.features.IntegerFeature;
import it.torkin.dataminer.entities.jira.issue.IssueComment;
import it.torkin.dataminer.entities.jira.issue.IssueWorkItem;
import lombok.extern.slf4j.Slf4j;

/**
 * #217
 */
@Component
@Slf4j
public class ParticipantsMiner extends FeatureMiner {

    private static final String ISSUE_PARTICIPANTS_COUNT = IssueFeature.ISSUE_PARTICIPANTS.getName() + ": count";
    
    @Autowired private IIssueController issueController;
    
    @Override
    public void mine(FeatureMinerBean bean) {

        Set<String> participants = getParticipants(bean);

        bean.getMeasurement().getFeatures().add(new IntegerFeature(ISSUE_PARTICIPANTS_COUNT, participants.size()));
        
    }

    private Set<String> getParticipants(FeatureMinerBean bean) {

        Set<String> participants = new HashSet<>();
        Issue issue = bean.getIssue();
        IssueBean issueBean = new IssueBean(issue, bean.getMeasurement().getMeasurementDate());

        // author
        participants.add(getAuthor(issueBean));

        // historical reporters and assignees (including the ones currently set)
        participants.addAll(getHistoricalReporters(issueBean));
        participants.addAll(getHistoricalAssignee(issueBean));

        // comments and worklog authors and updaters
        participants.addAll(getParticipantsInComments(issueBean));
        participants.addAll(getParticipantsInWorklog(issueBean));

        // attachment and changelog authors
        participants.addAll(getParticipantsInAttachments(issueBean));
        participants.addAll(getParticipantsInChangelog(issueBean));

        participants.remove(null);
        participants.remove("");
        return participants;


    }

    private String getAuthor(IssueBean bean) {
        Developer author = bean.getIssue().getDetails().getFields().getCreator();
        if (author != null) return author.getName();
        else return null;
    }

    private Set<String> getHistoricalReporters(IssueBean bean) {
        return issueController.getReporterChangeset(bean);
    }

    private Set<String> getHistoricalAssignee(IssueBean bean) {
        return issueController.getAssigneeChangeset(bean);
    }
    
    private Set<String> getParticipantsInComments(IssueBean bean) {
        List<IssueComment> comments = issueController.getComments(bean);
        Set<String> participants = new HashSet<>();
        for (IssueComment comment : comments) {
            Developer author = comment.getAuthor();
            Developer updateAuthor = comment.getUpdateAuthor();
            if (author != null) participants.add(author.getName());
            if (updateAuthor != null) participants.add(updateAuthor.getName());
        }
        return participants;
        
    }

    private Set<String> getParticipantsInAttachments(IssueBean bean) {
        return issueController.getAttachments(bean).stream()
                .filter(attachment -> attachment.getAuthor() != null)
        .map(a -> a.getAuthor().getKey())
        .collect(Collectors.toSet());
    }

    private Set<String> getParticipantsInWorklog(IssueBean bean) {
        List<IssueWorkItem> workItems = issueController.getWorkItems(bean);
        Set<String> participants = new HashSet<>();
        for (IssueWorkItem workItem : workItems) {
            Developer author = workItem.getAuthor();
            Developer updateAuthor = workItem.getUpdateAuthor();
            if (author != null) participants.add(author.getName());
            if (updateAuthor != null) participants.add(updateAuthor.getName());

        }
        return participants;
    }

    private Set<String> getParticipantsInChangelog(IssueBean bean) {
        return issueController.getHistoryAuthors(bean);
    }

    @Override
    protected Set<String> getFeatureNames() {
        return Set.of(ISSUE_PARTICIPANTS_COUNT);
    }
    
}
