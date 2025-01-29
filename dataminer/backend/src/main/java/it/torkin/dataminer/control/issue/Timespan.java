package it.torkin.dataminer.control.issue;

import java.sql.Timestamp;
import java.time.Duration;

import lombok.Data;

@Data
public class Timespan {
    private Timestamp start;
    private Timestamp end;

    public Duration getDuration() {
        return Duration.between(start.toInstant(), end.toInstant());
    }
}
