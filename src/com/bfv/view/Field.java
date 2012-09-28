package com.bfv.view;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/09/12
 * Time: 3:42 PM
 */
public class Field {
    private int id;
    private String fieldName;
    private String defaultDecimalFormat;
    private String defaultLabel;

    private ArrayList<FieldUnits> units;
    private int defaultMultiplierIndex;


    public Field(int id, String fieldName, String defaultDecimalFormat, String defaultLabel, String defaultUnits) {
        this.id = id;
        this.fieldName = fieldName;
        this.defaultDecimalFormat = defaultDecimalFormat;
        this.defaultLabel = defaultLabel;
        units = new ArrayList<FieldUnits>();
        units.add(new FieldUnits(defaultUnits, 1.0));
        defaultMultiplierIndex = 0;

    }

    public int getId() {
        return id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDefaultDecimalFormat() {
        return defaultDecimalFormat;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public ArrayList<FieldUnits> getUnits() {
        return units;
    }

    public String[] getUnitNames() {
        String[] ret = new String[units.size()];
        for (int i = 0; i < units.size(); i++) {
            FieldUnits fieldUnits = units.get(i);
            ret[i] = fieldUnits.getName();
        }
        return ret;
    }

    public void addUnits(String unitName, double multiplier) {
        units.add(new FieldUnits(unitName, multiplier));
    }

    public double getUnitMultiplier(String unitName) {
        for (int i = 0; i < units.size(); i++) {
            FieldUnits fieldUnits = units.get(i);
            if (fieldUnits.getName().equals(unitName)) {
                return fieldUnits.getMultiplier();
            }
        }

        return 1.0;

    }

    public double getUnitMultiplier(int index) {
        return units.get(index).getMultiplier();
    }

    //    public double getUnitMultiplier(){
//            return units.get(defaultMultiplierIndex).getMultiplier();
//    }
    public String getUnitMultiplierName(int multiplierIndex) {
        return units.get(multiplierIndex).getName();
    }

    public void setDefaultMultiplierIndex(int defaultMultiplierIndex) {
        this.defaultMultiplierIndex = defaultMultiplierIndex;
        if (this.defaultMultiplierIndex > units.size() - 1) {
            this.defaultMultiplierIndex = 0;
        }

    }

    public int getDefaultMultiplierIndex() {
        return defaultMultiplierIndex;
    }
}
