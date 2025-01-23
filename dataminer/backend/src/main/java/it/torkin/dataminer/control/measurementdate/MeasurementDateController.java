package it.torkin.dataminer.control.measurementdate;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.MeasurementConfig;

@Service
public class MeasurementDateController  implements IMeasurementDateController{

    @Autowired private MeasurementConfig config;
    @Autowired private List<MeasurementDate> dates;
    
    @Override
    public List<MeasurementDate> getMeasurementDates() {
        Set<String> dateNames = Set.of(config.getDates());
        return dates.stream().filter(d -> dateNames.contains(d.getName())).toList();
    }
    
}
