package it.torkin.dataminer.toolbox.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SafeMath {

    public double calcPercentage(double part, double total) {
        return total == 0 ? 0 : part / total * 100;
    }

}
