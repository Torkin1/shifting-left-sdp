package it.torkin.dataminer.control.dataset.raw;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.dao.local.ComponentDao;
import it.torkin.dataminer.dao.local.DeveloperDao;
import it.torkin.dataminer.dao.local.IssueLinkTypeDao;
import it.torkin.dataminer.dao.local.IssuePointerDao;
import it.torkin.dataminer.dao.local.IssuePriorityDao;
import it.torkin.dataminer.dao.local.IssueResolutionDao;
import it.torkin.dataminer.dao.local.IssueStatusDao;
import it.torkin.dataminer.dao.local.IssueTypeDao;
import it.torkin.dataminer.dao.local.ProjectDao;
import it.torkin.dataminer.entities.jira.Component;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.entities.jira.issue.IssueAttachment;
import it.torkin.dataminer.entities.jira.issue.IssueComment;
import it.torkin.dataminer.entities.jira.issue.IssueDetails;
import it.torkin.dataminer.entities.jira.issue.IssueFields;
import it.torkin.dataminer.entities.jira.issue.IssueLink;
import it.torkin.dataminer.entities.jira.issue.IssueLinkType;
import it.torkin.dataminer.entities.jira.issue.IssuePointer;
import it.torkin.dataminer.entities.jira.issue.IssuePriority;
import it.torkin.dataminer.entities.jira.issue.IssueResolution;
import it.torkin.dataminer.entities.jira.issue.IssueStatus;
import it.torkin.dataminer.entities.jira.issue.IssueType;
import it.torkin.dataminer.entities.jira.issue.IssueWorkItem;
import it.torkin.dataminer.entities.jira.project.Project;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EntityMerger implements IEntityMerger{

    @Autowired private IssueStatusDao issueStatusDao;
    @Autowired private IssueTypeDao issueTypeDao;
    @Autowired private IssuePriorityDao issuePriorityDao;
    @Autowired private DeveloperDao developerDao;
    @Autowired private ProjectDao projectDao;
    @Autowired private ComponentDao componentDao;
    @Autowired private IssuePointerDao issuePointerDao;
    @Autowired private IssueLinkTypeDao issueLinkTypeDao;
    @Autowired private IssueResolutionDao issueResolutionDao;

    private class EntityFinder<T>{

        /**
         * Merges the given object with the corresponding entity in the local db.
         * https://stackoverflow.com/questions/16246675/hibernate-error-a-different-object-with-the-same-identifier-value-was-already-a/16246858#16246858
         */
        public T find(T object, String key){

            T entity = null;
            JpaRepository<T, String> dao;
            
            /**
             * Unholy if-else chain to determine the correct dao to use
             */
            if (object instanceof Developer){
                dao = (JpaRepository<T, String>) developerDao;
            }
            else if(object instanceof IssueStatus){
                dao = (JpaRepository<T, String>) issueStatusDao;
            }
            else if (object instanceof IssueType){
                dao = (JpaRepository<T, String>) issueTypeDao;
            }
            else if (object instanceof IssuePriority){
                dao = (JpaRepository<T, String>) issuePriorityDao;
            }
            else if (object instanceof Project){
                dao = (JpaRepository<T, String>) projectDao;
            }
            else if (object instanceof Component){
                dao = (JpaRepository<T, String>) componentDao;
            }
            else if (object instanceof IssuePointer){
                dao = (JpaRepository<T, String>) issuePointerDao;
            }
            else if (object instanceof IssueLinkType){
                dao = (JpaRepository<T, String>) issueLinkTypeDao;
            }
            else if (object instanceof IssueResolution){
                dao = (JpaRepository<T, String>) issueResolutionDao;
            }
            else{
                throw new IllegalArgumentException(String.format("Unsupported entity type: %s", object.getClass().getName()));
            }
            
            entity = dao.save(object);
            return entity;
            
        }
    }

    @Override
    public void mergeIssueDetails(IssueDetails issueDetails){

        IssueFields fields = issueDetails.getFields();
        log.debug(String.format("merging details of issue %s", issueDetails.getJiraKey()));
        mergeIssueFields(fields);

    }

    private void mergeIssueFields(IssueFields fields) {
        mergeDevelopers(fields);
        mergeIssuePointers(fields);
        mergeIssueStatus(fields);
        mergeIssueType(fields);
        mergeIssuePriority(fields);
        mergeIssueResolution(fields);
        mergeProject(fields);
        mergeComponents(fields);
    }

    private void mergeIssueResolution(IssueFields fields) {
        if (fields.getResolution() == null) return;
        fields.setResolution(new EntityFinder<IssueResolution>().find(fields.getResolution(), fields.getResolution().getJiraId()));
    }

    private void mergeIssuePointers(IssueFields fields) {

        for(IssuePointer subtask : fields.getSubtasks()){
            mergeIssueFields(subtask.getFields());
        }
        fields.getSubtasks().replaceAll((subtask) -> {
            return new EntityFinder<IssuePointer>().find(subtask, subtask.getJiraId());
        });
        
        for (IssueLink link : fields.getIssuelinks()) {
            link.setType(new EntityFinder<IssueLinkType>().find(link.getType(), link.getType().getJiraId()));
            link.setInwardIssue(findIssuePointer(link.getInwardIssue()));
            link.setOutwardIssue(findIssuePointer(link.getOutwardIssue()));
        }
    }

    private void mergeComponents(IssueFields fields) {
        fields.getComponents().replaceAll((component) -> {
            return new EntityFinder<Component>().find(component, component.getJiraId());
        });
    }

    private void mergeProject(IssueFields fields) {
        if (fields.getProject() == null) return;
        fields.setProject(new EntityFinder<Project>().find(fields.getProject(), fields.getProject().getJiraId()));
    }

    private void mergeIssuePriority(IssueFields fields) {
        if (fields.getPriority() == null) return;
        fields.setPriority(new EntityFinder<IssuePriority>().find(fields.getPriority(), fields.getPriority().getJiraId()));
    }

    private void mergeIssueType(IssueFields fields) {
        if (fields.getIssuetype() == null) return;
        fields.setIssuetype(new EntityFinder<IssueType>().find(fields.getIssuetype(), fields.getIssuetype().getJiraId()));
    }

    private void mergeIssueStatus(IssueFields fields) {
        if (fields.getStatus() == null) return;
        fields.setStatus(new EntityFinder<IssueStatus>().find(fields.getStatus(), fields.getStatus().getJiraId()));
    }

    private Developer findDeveloper(Developer developer){
        if (developer == null) return null;
        return new EntityFinder<Developer>().find(developer, developer.getKey());
    }

    private IssuePointer findIssuePointer(IssuePointer pointer) {

        if (pointer == null) return null;
        mergeIssueFields(pointer.getFields());
        return new EntityFinder<IssuePointer>().find(pointer, pointer.getJiraId());

    }
    
    /**
     * CAUTION: must be called before all other merge methods
     */
    private void mergeDevelopers(IssueFields fields) {
                
        fields.setAssignee(findDeveloper(fields.getAssignee()));
        fields.setCreator(findDeveloper(fields.getCreator()));
        fields.setReporter(findDeveloper(fields.getReporter()));

        if (fields.getComment() != null){
            for (IssueComment comment : fields.getComment().getComments()) {
            
                comment.setAuthor(findDeveloper(comment.getAuthor()));
                comment.setUpdateAuthor(findDeveloper(comment.getUpdateAuthor())); 
            }
        }
        
        if (fields.getWorklog() != null){
            for (IssueWorkItem workItem : fields.getWorklog().getWorklogs()) {
                workItem.setAuthor(findDeveloper(workItem.getAuthor()));
                workItem.setUpdateAuthor(findDeveloper(workItem.getUpdateAuthor()));
            }
        }

        for (IssueAttachment attachment : fields.getAttachments()){
            attachment.setAuthor(findDeveloper(attachment.getAuthor()));
        }
    }

    
}
