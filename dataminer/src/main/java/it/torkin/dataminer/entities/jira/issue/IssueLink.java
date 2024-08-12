package it.torkin.dataminer.entities.jira.issue;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name="issue_link")
@Embeddable
public class IssueLink{
    
    @SerializedName("id")
    @Column(unique = true)
    private String jiraId;
    
    @ManyToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssueLinkType type;
    @OneToOne(
        optional = true,
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssuePointer outwardIssue;
    @OneToOne(
        optional = true,
        cascade = { CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY)
    private IssuePointer inwardIssue;

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
