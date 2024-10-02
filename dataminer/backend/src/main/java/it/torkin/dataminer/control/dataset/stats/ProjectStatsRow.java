package it.torkin.dataminer.control.dataset.stats;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"dataset", "project", "measurementDate", "usableTickets", "excludedTickets"})
public class ProjectStatsRow {
    private String dataset;
    private String project;
    private String measurementDate;
    private int usableTickets;
    private int excludedTickets;

}
