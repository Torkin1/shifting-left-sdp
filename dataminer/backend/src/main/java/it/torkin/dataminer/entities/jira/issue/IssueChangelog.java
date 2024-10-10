package it.torkin.dataminer.entities.jira.issue;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Data;

@Data
@Embeddable
public class IssueChangelog {
    
    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("created ASC")
    private List<IssueHistory> histories = new ArrayList<>();
}
