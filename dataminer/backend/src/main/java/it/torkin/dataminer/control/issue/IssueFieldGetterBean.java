package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.entities.dataset.IssueBean;
import lombok.Data;

@Data
class IssueFieldGetterBean {

    private final IssueBean issueBean;
    private final IssueField issueField;
    
}