package com.bfv.model;

import com.bfv.DataSource;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 11/06/12
 * Time: 8:35 AM
 */
public class Altitude implements DataSource {
    public static double STANDARD_SEA_LEVEL_PRESSURE = 101325;  //in pascals
    private double seaLevelPressure;
    private double altDamp = 1.0;
    private double rawPressure;
    private double rawAltitude;
    private double dampedAltitude;
    private boolean dampedAltStarted = false;
    private String name;
    private ArrayList varios;


    public Altitude(double seaLevelPressure, double altDamp, String name) {
        this.seaLevelPressure = seaLevelPressure;
        this.altDamp = altDamp;
        this.name = name;
    }

    public synchronized double addPressure(double pressure, double time) {

        rawPressure = pressure;
        rawAltitude = 44330.0 * (1 - Math.pow((pressure / seaLevelPressure), 0.190295));
        if (dampedAltStarted) {
            dampedAltitude = dampedAltitude + altDamp * (rawAltitude - dampedAltitude);
        } else {
            dampedAltitude = rawAltitude;
            dampedAltStarted = true;
        }

        if (varios != null) {
            for (int i = 0; i < varios.size(); i++) {
                Vario vario = (Vario) varios.get(i);
                vario.addData(time);
            }
        }

        return rawAltitude;
    }

    /**
     * Sets the altitude to a specific level in m and updates the sea level pressure. Returns the new
     * sea level pressure value.
     */
    public synchronized double setAltitude(double alt) {
        double qnh = rawPressure / Math.pow(1.0 - (alt / 44330.0), 5.255);
        rawAltitude = alt;
        dampedAltitude = rawAltitude;
        dampedAltStarted = true;
        seaLevelPressure = qnh;

        for (int i = 0; i < varios.size(); i++) {
            Vario vario = (Vario) varios.get(i);
            vario.resetWindow();
        }

        return seaLevelPressure;

    }

    public double getSeaLevelPressure() {

        return seaLevelPressure;
    }

    public void setSeaLevelPressure(double seaLevelPressure) {
        this.seaLevelPressure = seaLevelPressure;
        dampedAltStarted = false;
    }

    public double getAltDamp() {
        return altDamp;
    }

    public void setAltDamp(double altDamp) {
        this.altDamp = altDamp;
    }

    public double getRawPressure() {
        return rawPressure;
    }

    public synchronized double getRawAltitude() {
        return rawAltitude;
    }

    public synchronized double getDampedAltitude() {
//        Log.i("BFVAltitude", this.toString());
        return dampedAltitude;
    }

    public boolean isDampedAltStarted() {
        return dampedAltStarted;
    }

    public String getName() {
        return name;
    }

    public void addVario(Vario vario) {
        if (varios == null) {
            varios = new ArrayList();
        }
        varios.add(vario);
        vario.registerAltitude(this);
    }

    public Vario getVario(String name) {
        if (varios != null) {
            for (int i = 0; i < varios.size(); i++) {
                Vario vario = (Vario) varios.get(i);
                if (vario.getName().matches(name)) {
                    return vario;
                }

            }
        }
        return null;
    }

    public String toString() {
        return "Alt:" + name + ":" + rawAltitude;
    }

    public synchronized double getValue() {
        return dampedAltitude;
    }
}
