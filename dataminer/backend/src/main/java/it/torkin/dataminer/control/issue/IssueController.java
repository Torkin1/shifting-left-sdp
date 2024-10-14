package it.torkin.dataminer.control.issue;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import it.torkin.dataminer.entities.dataset.Issue;
import it.torkin.dataminer.entities.dataset.IssueBean;
import it.torkin.dataminer.entities.dataset.IssueBugginessBean;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IssueController implements IIssueController{

    @Override
    public boolean isBuggy(IssueBugginessBean bean){
                
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

}
