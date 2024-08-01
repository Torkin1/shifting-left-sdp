package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import it.torkin.dataminer.rest.parsing.Hide;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="issue_link")
public class IssueLink{
    @Id
    @Hide
    @GeneratedValue
    private long id;
    
    @SerializedName("id")
    private String jiraId;
    
    @ManyToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssueLinkType type;
    @OneToOne(
        optional = true,
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private UntrustedIssue outwardIssue;
    @OneToOne(
        optional = true,
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private UntrustedIssue inwardIssue;

    /**
     * NOTE: The direction of the link is defined
     * by which one between outwardIssue and inwardIssue
     * is not null. 
     */

    public boolean isOutward(){
        return outwardIssue != null;
    }

    public boolean isInward(){
        return inwardIssue != null;
    }

}
