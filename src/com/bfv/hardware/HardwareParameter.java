/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2013 Alistair Dickie

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

package com.bfv.hardware;

import android.graphics.Color;
import com.bfv.view.ViewComponentParameter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

public class HardwareParameter {

    public static final int TYPE_INT = 0;
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_INTLIST = 5;
    public static final int TYPE_INTOFFSET = 2;

    private String name;
    private String code;
    private double factor;
    private int type;
    private int maxHWVal;
    private int minHWVal;
    private int minHWVersion;


    private int hardwareValue;
    private String value;
    private String message;

    public HardwareParameter(String code, int minHWVersion, int type, String name, double factor, int minHWVal, int maxHWVal) {
        this.name = name;
        this.code = code;
        this.factor = factor;
        this.type = type;
        this.minHWVersion = minHWVersion;
        this.minHWVal = minHWVal;
        this.maxHWVal = maxHWVal;
    }

    //for the lists only
    public String names[];

    public DecimalFormat df;

    public String getName() {
        return name;
    }

    public int getMinHWVersion() {
        return minHWVersion;
    }

    public String getValue() {
        if (type == TYPE_INTLIST) {
            return getIntListName();
        }
        return value;
    }

    public int getHardwareValue() {
        return hardwareValue;
    }

    public void setValueFromDialog(String newValue) {
        // Log.i("BFV", "newValue" + newValue);
        this.value = newValue;
        if (type == TYPE_BOOLEAN) {
            if (!getBooleanValue()) {
                hardwareValue = 0;
            } else {
                hardwareValue = 1;
            }
        } else if (type == TYPE_DOUBLE) {
            hardwareValue = (int) (getDoubleValue() * factor);

        } else if (type == TYPE_INT) {
            hardwareValue = (int) getIntValue();

        } else if (type == TYPE_INTOFFSET) {
            hardwareValue = (int) (getIntValue() - factor);

        } else if (type == TYPE_INTLIST) {
            //todo

        }

        if (hardwareValue < minHWVal) {
            hardwareValue = minHWVal;
        }
        if (hardwareValue > maxHWVal) {
            hardwareValue = maxHWVal;
        }

        value = hardwareValueToStringValue(hardwareValue);

    }

    public void setHardwareValue(String stringHardwareValue) {
        hardwareValue = Short.valueOf(stringHardwareValue);
        value = hardwareValueToStringValue(hardwareValue);
    }

    private String hardwareValueToStringValue(int hwVal) {
        if (type == TYPE_BOOLEAN) {
            if (hwVal == 0) {
                return "false";
            } else {
                return "true";
            }

        } else if (type == TYPE_DOUBLE) {
            return df.format(hwVal / factor);

        } else if (type == TYPE_INT) {
            return hwVal + "";

        } else if (type == TYPE_INTOFFSET) {
            return (int) (hwVal + factor) + "";

        } else if (type == TYPE_INTLIST) {
            //todo

        }
        return hwVal + "";
    }


    public int getIntValue() {
        return Integer.parseInt(value);
    }

    public String getIntListName() {
        int index = Integer.parseInt(value);
        if (index < 0 || index >= names.length) {
            return null;
        }
        return names[index];
    }

    public double getDoubleValue() {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        try {
            return nf.parse(value).doubleValue(); //try to parse the commas in european locales...
        } catch (ParseException e) {
            return Double.parseDouble(value); //fall back to a simple parse..
        }

    }

    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }


    public int getType() {
        return type;
    }

    public String[] getNames() {
        return names;
    }

    public HardwareParameter setDecimalFormat(String format) {
        df = new DecimalFormat(format, DecimalFormatSymbols.getInstance(Locale.US));
        return this;
    }

    public String getCode() {
        return code;
    }

    public String getMessage(boolean includeMinMax) {
        if (message != null) {
            if (includeMinMax) {
                return message + "\n" + "Min=" + getMinValue() + " : Max=" + getMaxValue();
            } else {
                return message;
            }

        } else {
            return this.getName();
        }


    }

    public HardwareParameter setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getMaxValue() {
        return hardwareValueToStringValue(maxHWVal);
    }

    public String getMinValue() {
        return hardwareValueToStringValue(minHWVal);
    }
}
