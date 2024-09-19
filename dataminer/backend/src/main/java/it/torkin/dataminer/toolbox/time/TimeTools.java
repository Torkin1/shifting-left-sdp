package it.torkin.dataminer.toolbox.time;

import java.sql.Timestamp;
import java.util.Calendar;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeTools {

    public Timestamp now(){
        return new Timestamp(Calendar.getInstance().getTimeInMillis());
    }

}
