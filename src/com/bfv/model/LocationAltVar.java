package com.bfv.model;

import android.location.Location;
import android.util.Log;

import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 20/08/12
 * Time: 9:32 PM
 */
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
    private DecimalFormat latLongFormat = new DecimalFormat("0.000000");
    private DecimalFormat df3 = new DecimalFormat("0.000");

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
