package it.torkin.dataminer.control.issue;

import it.torkin.dataminer.entities.dataset.Issue;
import lombok.Data;
import java.sql.Timestamp;

@Data
public class HasBeenAssignedBean {

    private final Issue issue;
    private final String assigneeKey;
    private final Timestamp measurementDate;
}
