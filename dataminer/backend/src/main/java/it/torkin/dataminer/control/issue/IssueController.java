package it.torkin.dataminer.control.issue;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import it.torkin.dataminer.entities.dataset.Commit;
import it.torkin.dataminer.entities.dataset.Issue;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

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
    public String getDescription(IssueBean bean) {
        
        return new IssueFieldGetter<String>(
            fields -> fields.getDescription() == null? "" : fields.getDescription(),
            Function.identity()
        ).apply(new IssueFieldGetterBean(bean, IssueField.DESCRIPTION));        
    }

    @Override
    public String getTitle(IssueBean bean){
        return new IssueFieldGetter<String>(
            fields -> fields.getSummary() == null? "" : fields.getSummary(),
            Function.identity()
        ).apply(new IssueFieldGetterBean(bean, IssueField.SUMMARY));
    }

    @Override
    public Commit getFirstCommit(IssueCommitBean bean) {
        return bean.getIssue().getCommits().stream()
            .filter(commit -> commit.getDataset().getName().equals(bean.getDataset()))
            .sorted((c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()))
            .findFirst()
            .orElse(null);
    }
}
