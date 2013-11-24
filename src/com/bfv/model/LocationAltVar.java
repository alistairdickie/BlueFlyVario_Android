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

package com.bfv.model;

import android.location.Location;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class LocationAltVar {

    public static int FLIGHT_CSV = 0;

    private Location location;
    private double baroAlt;
    private double vario;
    private double vario2;
    public float x;
    public float y;
    private boolean isMaxVar;
    public double windx;
    public double windy;
    public float driftedX;
    public float driftedY;
    private long creationTime;
    private DecimalFormat latLongFormat = new DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.US));
    private DecimalFormat df3 = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));

    public LocationAltVar() {
        baroAlt = -1000;
        vario = -1000;
        vario2 = -1000;
    }

    public LocationAltVar(Location location, double baroAlt, double vario, double vario2) {
        this.location = location;
        this.baroAlt = baroAlt;
        this.vario = vario;
        this.vario2 = vario2;
        creationTime = System.currentTimeMillis();

    }

    public void setWind(double[] wind) {
        windx = wind[0];
        windy = wind[1];
    }

    public Location getLocation() {
        return location;
    }

    public double getBaroAlt() {
        return baroAlt;
    }

    public double getVario() {
        return vario;
    }

    public double getVario2() {
        return vario2;
    }

    public boolean isMaxVar() {
        return isMaxVar;
    }

    public void setMaxVar(boolean maxVar) {
        isMaxVar = maxVar;
    }

    public void setDriftedXY(long currentTime) {
        double timeS = (currentTime - creationTime) / 1000.0;
        driftedX = (float) (x + windx * timeS);
        driftedY = (float) (y + windy * timeS);
    }


    public String getCSVString() {
        return latLongFormat.format(location.getLatitude())
                + "," + latLongFormat.format(location.getLongitude())
                + "," + location.getTime()
                + "," + location.getAccuracy()
                + "," + location.getBearing()
                + "," + df3.format(location.getSpeed())
                + "," + df3.format(location.getAltitude())
                + "," + df3.format(this.getBaroAlt())
                + "," + df3.format(this.getVario())
                + "," + df3.format(this.getVario2());


    }

    public static String getCSVHeaders() {
        return "#Latitude, Longitude, Time(ms), Accuracy, Bearing, Speed, GPS Altidude, Pressure Altidude, Max Vario, Max Vario2";


    }
}
