package it.torkin.dataminer.control.dataset.stats;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"dataset", "repository", "linkage", "buggyLinkage"})
public class RepositoriesStatsRow {

    private String dataset;
    private String repository;
    private double linkage;
    private double buggyLinkage;
}
