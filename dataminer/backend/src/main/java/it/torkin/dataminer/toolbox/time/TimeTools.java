package it.torkin.dataminer.toolbox.time;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeTools {

    public Timestamp now(){
        return new Timestamp(Calendar.getInstance().getTimeInMillis());
    }

    public Timestamp dawnOfTime(){
        return new Timestamp(0);
    }

    public Timestamp minusOneSecond(Timestamp timestamp){
        return Timestamp.from(timestamp.toInstant().minus(1, ChronoUnit.SECONDS));
    }

    public Timestamp plusOneSecond(Timestamp timestamp){
        return Timestamp.from(timestamp.toInstant().plus(1, ChronoUnit.SECONDS));
    }

}
