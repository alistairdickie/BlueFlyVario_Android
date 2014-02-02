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

import android.graphics.Color;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class ViewComponentParameter {

    public static final int TYPE_NOTSET = -1;
    public static final int TYPE_INT = 0;
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_COLOR = 3;
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_INTLIST = 5;


    public String name;
    public String value;
    public int type;

    public int valueType;

    //for the lists only
    public String names[];

    public DecimalFormat df;


    public ViewComponentParameter(String name) {

        this.name = name;
        this.type = TYPE_NOTSET;
    }

    public ViewComponentParameter(String name, String value) {
        this.name = name;
        this.value = value;
        this.type = TYPE_NOTSET;
    }


    public ViewComponentParameter setInt(int val) {
        this.type = TYPE_INT;
        this.value = val + "";
        return this;
    }

    public ViewComponentParameter setIntList(int val, String[] names) {
        this.type = TYPE_INTLIST;

        this.names = names;
        this.value = val + "";
        return this;
    }

    public ViewComponentParameter setDouble(double val) {
        this.type = TYPE_DOUBLE;
        if (df != null) {
            this.value = df.format(val);

        } else {
            this.value = val + "";
        }

        return this;

    }

    public ViewComponentParameter setString(String val) {
        this.type = TYPE_STRING;
        this.value = val + "";
        return this;
    }

    public ViewComponentParameter setColor(int color) {
        this.type = TYPE_COLOR;
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(color));
        while (sb.length() < 8) {
            sb.insert(0, '0'); // pad with leading zero if needed
        }


        this.value = "#" + sb.toString();
        return this;
    }

    public ViewComponentParameter setBoolean(boolean val) {
        this.type = TYPE_BOOLEAN;
        this.value = val + "";
        return this;
    }


    public String getName() {
        return name;
    }

    public String getValue() {
        if (type == TYPE_INTLIST) {
            return getIntListName();
        }
        return value;
    }

    public String getXmlValue() {
        return value;
    }

    public void setValue(String newValue) {//todo check by type ...
        // Log.i("BFV", "newValue" + newValue);
        this.value = newValue;
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

    public int getColorValue() {
        try {
            return Color.parseColor(value);//#RRGGBB #AARRGGBB 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta', 'yellow', 'lightgray', 'darkgray'

        } catch (IllegalArgumentException e) {
            return Color.BLACK;
        }

    }


    public int getType() {
        return type;
    }

    public String[] getNames() {
        return names;
    }

    public ViewComponentParameter setDecimalFormat(String format) {
        df = new DecimalFormat(format, DecimalFormatSymbols.getInstance(Locale.US));
        return this;
    }
}
