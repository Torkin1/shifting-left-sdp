package it.torkin.dataminer.control.measurementdate;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.torkin.dataminer.config.MeasurementConfig;

@Service
public class MeasurementDateController  implements IMeasurementDateController{

    @Autowired private MeasurementConfig config;
    @Autowired private List<MeasurementDate> dates;
    
    @Override
    public List<MeasurementDate> getMeasurementDates() {
        return dates.stream().filter(d -> config.getDates().contains(d.getName())).toList();
    }
    
}
