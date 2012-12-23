/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2012 Alistair Dickie

 BlueFlyVario is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 BlueFlyVario is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with BlueFlyVario.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bfv.view;

import java.util.ArrayList;

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
