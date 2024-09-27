package it.torkin.dataminer.entities.jira.issue; 
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import lombok.Data; 

@Data
@Embeddable
public class IssueContent{
    private String type;
    @ElementCollection
    private List<IssueContent> content;
    @Column(columnDefinition = "text")
    private String text;
}
