package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class TemporalSpan {
    private Timestamp start;
    private Timestamp end;
}
