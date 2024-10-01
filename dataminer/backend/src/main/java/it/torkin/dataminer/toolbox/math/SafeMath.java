package it.torkin.dataminer.toolbox.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SafeMath {

    public double calcPercentage(double part, double total) {
        return total == 0 ? 0 : part / total * 100;
    }

    public double inversePercentage(double percentage, double total){
        return (total * percentage) / 100;
    }

    public long ceiledInversePercentage(double percentage, double total){
        return (long) Math.ceil(inversePercentage(percentage, total));
    }

    public int nullAsZero(Integer value) {
        return value == null ? 0 : value;
    }

    public double nullAsZero(Double value) {
        return value == null ? 0 : value;
    }

    public long nullAsZero(Long value) {
        return value == null ? 0 : value;
    }
}
