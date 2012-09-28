package com.bfv.view;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/09/12
 * Time: 5:31 PM
 */
public class FieldUnits {
    public String name;
    public double multiplier;

    public FieldUnits(String name, double multiplier) {
        this.name = name;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
