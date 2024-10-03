package it.torkin.dataminer.control.dataset.raw;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;
import lombok.NonNull;

@Data
public class IssueEntry {

    @NonNull private final Issue issue;
    @NonNull private final String issueKey;
    
    public boolean equals(IssueEntry other){
        return issueKey.equals(other.issueKey);
    }

    public int hashCode(){
        return issueKey.hashCode();
    }
    
}
