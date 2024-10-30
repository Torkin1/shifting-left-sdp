package it.torkin.dataminer.control.issue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.dao.local.DeveloperDao;
import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.jira.Developer;
import it.torkin.dataminer.toolbox.string.StringTools;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

    @Autowired private DeveloperDao developerDao;
    
    @Override
    public boolean isBuggy(IssueCommitBean bean){
                
        Issue issue = bean.getIssue();
        String dataset = bean.getDataset();
        
        return issue.getCommits().stream().anyMatch(commit -> {          
            return commit.isBuggy()
             && commit.getDataset().getName().equals(dataset);
        });
    }

    @Override
    public Commit getFirstCommit(IssueCommitBean bean) {
        return bean.getIssue().getCommits().stream()
            .filter(commit -> commit.getDataset().getName().equals(bean.getDataset()))
            .sorted((c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getDescription(IssueBean bean) {
        
        return new IssueFieldGetter<String>(
            fields -> fields.getDescription() == null? "" : fields.getDescription(),
            entry -> entry.getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.DESCRIPTION));        
    }

    @Override
    public String getTitle(IssueBean bean){
        return new IssueFieldGetter<String>(
            fields -> fields.getSummary() == null? "" : fields.getSummary(),
            entry -> entry.getValueString()
        ).apply(new IssueFieldGetterBean(bean, IssueField.SUMMARY));
    }

    @Override
    public Developer getAssignee(IssueBean bean) {
        return new IssueFieldGetter<Developer>(
            fields -> fields.getAssignee(),
            entry -> {
                String assigneeKey = entry.getValue();
                String assigneeName = entry.getValueString();
                if (StringTools.isBlank(assigneeKey)) return null;
                Developer developer = developerDao.findByKey(assigneeKey);
                if (developer == null){
                    developer = new Developer();
                    developer.setKey(assigneeKey);
                    developer.setName(assigneeName);
                }
                return developer;
            }
        ).apply(new IssueFieldGetterBean(bean, IssueField.ASSIGNEE));
    }
}
