package it.torkin.dataminer.entities.dataset;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class IssueBugginessBean {
    @NonNull private final String dataset;
    private Timestamp measurementDate = null;
}
