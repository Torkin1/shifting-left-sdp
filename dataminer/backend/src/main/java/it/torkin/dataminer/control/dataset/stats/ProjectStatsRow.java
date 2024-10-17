package it.torkin.dataminer.control.dataset.stats;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({
    "dataset",
    "project",
    "guessedRepository",
    "linkage",
    "buggyLinkage", 
    "measurementDate",
    "tickets",
    "usableTickets",
    "usableBuggyTicketsPercentage", 
    "excludedTickets",
    "filteredTicketsByFilter"
})
@JsonIgnoreProperties({
    "filteredTicketsByFilterMap"
})
public class ProjectStatsRow {
    private String dataset;
    private String project;
    private String guessedRepository;
    private double linkage;
    private double buggyLinkage;
    private String measurementDate;
    private long tickets;
    private long usableTickets;
    private double usableBuggyTicketsPercentage;
    private long excludedTickets;

    private Map<String, Long> filteredTicketsByFilterMap = new HashMap<>();

    @JsonProperty("filteredTicketsByFilter")
    public String getFilteredTicketsByFilter(){
        StringBuilder sb = new StringBuilder();
        filteredTicketsByFilterMap.forEach( (filter, count) -> {
            sb.append(filter).append("=").append(count).append(" ");
        });
        return sb.toString();
    }

}
