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

import android.location.Location;
import com.bfv.BFVService;
import com.bfv.BFVLocationManager;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class FieldManager {

    public static final int FIELD_DAMPED_ALTITUDE = 0;
    public static final int FIELD_VARIO1 = 1;
    public static final int FIELD_VARIO2 = 2;
    public static final int FIELD_BAT_PERCENT = 3;
    public static final int FIELD_VIEW_FPS = 4;
    public static final int FIELD_RAW_ALTITUDE = 5;
    public static final int FIELD_LOCATION_LATITUDE = 6;
    public static final int FIELD_LOCATION_LONGITUDE = 7;
    public static final int FIELD_LOCATION_ACCURACY = 8;
    public static final int FIELD_LOCATION_ALTITUDE = 9;
    public static final int FIELD_LOCATION_SPEED = 10;
    public static final int FIELD_LOCATION_HEADING = 11;
    public static final int FIELD_WIND_DIRECTION = 12;
    public static final int FIELD_WIND_SPEED = 13;
    public static final int FIELD_AIR_SPEED = 14;
    public static final int FIELD_QNH = 15;
    public static final int FIELD_FLIGHT_TIME = 16;
    public static final int FIELD_FLIGHT_DISTANCE = 17;
    public static final int FIELD_FLIGHT_ALT = 18;
    public static final int FIELD_PITOT_SPEED = 19;
    public static final int FIELD_PITOT_CALIBRATION = 20;


    private VarioSurfaceView surfaceView;
    private BFVLocationManager bfvLocationManager;
    private BFVService varioService;

    private ArrayList<Field> fields;

    public FieldManager(VarioSurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        varioService = surfaceView.getService();
        bfvLocationManager = varioService.getBfvLocationManager();
        fields = new ArrayList<Field>();
        this.setupFields();
    }

    //field names must be able to be concatenated to 'Field_' and remain a valid xml element name.
    public void setupFields() {//todo xml parsing stuff from a resource file type
        Field field = new Field(FIELD_DAMPED_ALTITUDE, "dampedAltitude", "0", "Alt", "m");
        field.addUnits("ft", 3.2808399);
        fields.add(field);

        field = new Field(FIELD_RAW_ALTITUDE, "rawAltitude", "0", "Raw Alt", "m");
        field.addUnits("ft", 3.2808399);
        fields.add(field);

        field = new Field(FIELD_VARIO1, "vario1", "0.0", "Var", "m/s");
        field.addUnits("00's ft/min", 1.96850394);
        fields.add(field);

        field = new Field(FIELD_VARIO2, "vario2", "0.0", "Var Avg", "m/s");
        field.addUnits("00's ft/min", 1.96850394);
        fields.add(field);

        fields.add(new Field(FIELD_BAT_PERCENT, "bat", "0%", "Battery", ""));

        fields.add(new Field(FIELD_VIEW_FPS, "fps", "0.0", "ViewFPS", ""));

        field = new Field(FIELD_LOCATION_LATITUDE, "latitude", "0.00000", "Latitude", "d.d");
        field.addUnits("d:m.m", 1.0);
        field.addUnits("d:m:s.s", 1.0);
        fields.add(field);

        field = new Field(FIELD_LOCATION_LONGITUDE, "longitude", "0.00000", "Longitude", "d.d");
        field.addUnits("d:m.m", 1.0);
        field.addUnits("d:m:s.s", 1.0);
        fields.add(field);

        field = new Field(FIELD_LOCATION_ACCURACY, "gpsAccuracy", "0", "gpsAccuracy", "m");
        fields.add(field);

        field = new Field(FIELD_LOCATION_ALTITUDE, "gpsAltitude", "0.0", "GPSAlt", "m");
        field.addUnits("ft", 3.2808399);
        fields.add(field);

        field = new Field(FIELD_LOCATION_SPEED, "groundSpeed", "0.0", "GroundSpeed", "m/s");
        field.addUnits("km/hr", 3.6);
        field.addUnits("mph", 2.23693629);
        field.addUnits("knts", 1.94384449);
        field.setDefaultMultiplierIndex(1);
        fields.add(field);

        field = new Field(FIELD_LOCATION_HEADING, "heading", "0", "Heading", "\u00B0");
        fields.add(field);

        field = new Field(FIELD_WIND_DIRECTION, "windDirection", "0", "Wind", "\u00B0");
        fields.add(field);

        field = new Field(FIELD_WIND_SPEED, "windSpeed", "0.0", "WindSpeed", "m/s");
        field.addUnits("km/hr", 3.6);
        field.addUnits("mph", 2.23693629);
        field.addUnits("knts", 1.94384449);
        field.setDefaultMultiplierIndex(1);
        fields.add(field);

        field = new Field(FIELD_AIR_SPEED, "airSpeed", "0.0", "AirSpeed", "m/s");
        field.addUnits("km/hr", 3.6);
        field.addUnits("mph", 2.23693629);
        field.addUnits("knts", 1.94384449);
        field.setDefaultMultiplierIndex(1);
        fields.add(field);

        field = new Field(FIELD_QNH, "qnh", "0.00", "QNH", "hPa");
        fields.add(field);

        field = new Field(FIELD_FLIGHT_TIME, "flightTime", "0", "Flight Time", "s");
        fields.add(field);

        field = new Field(FIELD_FLIGHT_DISTANCE, "flightDistance", "0.000", "Flight Distance", "m");
        field.addUnits("km", 0.001);
        field.addUnits("mi", 0.000621371192);
        field.addUnits("nm", 0.000539956803);
        field.setDefaultMultiplierIndex(1);
        fields.add(field);

        field = new Field(FIELD_FLIGHT_ALT, "flightAltitude", "0.0", "Flight Altitude", "m");
        field.addUnits("ft", 3.2808399);
        fields.add(field);

        field = new Field(FIELD_PITOT_SPEED, "pitotSpeed", "0.0", "PitotSpeed", "m/s");
        field.addUnits("km/hr", 3.6);
        field.addUnits("mph", 2.23693629);
        field.addUnits("knts", 1.94384449);
        field.setDefaultMultiplierIndex(0);
        fields.add(field);

//        field = new Field(FIELD_PITOT_CALIBRATION, "pitotCalibration", "0.0", "pitotCalibration", "pa");
//        fields.add(field);


    }

    public String getValue(Field field, DecimalFormat df, int multiplierIndex) {

        double value = 0.0;
        switch (field.getId()) {
            case FIELD_DAMPED_ALTITUDE:
                if (varioService.hasPressure()) {
                    value = surfaceView.alt.getValue();
                } else {
                    return "--";
                }

                break;
            case FIELD_RAW_ALTITUDE:
                if (varioService.hasPressure()) {
                    value = surfaceView.alt.getRawAltitude();
                } else {
                    return "--";
                }

                break;

            case FIELD_VARIO1:
                if (varioService.hasPressure()) {
                    value = surfaceView.kalmanVario.getValue();
                } else {
                    return "--";
                }

                break;
            case FIELD_VARIO2:
                if (varioService.hasPressure()) {
                    value = surfaceView.dampedVario.getValue();
                } else {
                    return "--";
                }

                break;
            case FIELD_BAT_PERCENT:
                if (varioService.hasPressure()) {
                    value = varioService.getBattery();
                } else {
                    return "--";
                }

                break;
            case FIELD_VIEW_FPS:
                value = surfaceView.getFps();
                break;
            case FIELD_LOCATION_LATITUDE:
                switch (multiplierIndex) {

                    case Location.FORMAT_DEGREES:
                        return Location.convert(bfvLocationManager.getLocation().getLatitude(), Location.FORMAT_DEGREES);
                    case Location.FORMAT_MINUTES:
                        return Location.convert(bfvLocationManager.getLocation().getLatitude(), Location.FORMAT_MINUTES);
                    case Location.FORMAT_SECONDS:
                        return Location.convert(bfvLocationManager.getLocation().getLatitude(), Location.FORMAT_SECONDS);
                }
                break;
            case FIELD_LOCATION_LONGITUDE:
                switch (multiplierIndex) {
                    case Location.FORMAT_DEGREES:
                        return Location.convert(bfvLocationManager.getLocation().getLongitude(), Location.FORMAT_DEGREES);
                    case Location.FORMAT_MINUTES:
                        return Location.convert(bfvLocationManager.getLocation().getLongitude(), Location.FORMAT_MINUTES);
                    case Location.FORMAT_SECONDS:
                        return Location.convert(bfvLocationManager.getLocation().getLongitude(), Location.FORMAT_SECONDS);
                }
                break;
            case FIELD_LOCATION_ACCURACY:
                if (bfvLocationManager.getLocation().hasAccuracy()) {
                    value = bfvLocationManager.getLocation().getAccuracy();
                } else {
                    return "--";
                }

                break;
            case FIELD_LOCATION_ALTITUDE:
                value = bfvLocationManager.getLocation().getAltitude();
                break;
            case FIELD_LOCATION_SPEED:
                if (bfvLocationManager.getLocation().hasSpeed()) {
                    value = bfvLocationManager.getLocation().getSpeed();
                } else {
                    return "--";
                }

                break;
            case FIELD_LOCATION_HEADING:
                if (bfvLocationManager.getLocation().hasBearing()) {
                    value = bfvLocationManager.getLocation().getBearing();
                } else {
                    return "--";
                }

                break;
            case FIELD_WIND_DIRECTION:
                if (bfvLocationManager.hasWind()) {
                    value = bfvLocationManager.getWindDirection();
                } else {
                    return "--";
                }


                break;
            case FIELD_WIND_SPEED:
                if (bfvLocationManager.hasWind()) {
                    value = bfvLocationManager.getWindSpeed();
                } else {
                    return "--";
                }

                break;
            case FIELD_AIR_SPEED:
                if (bfvLocationManager.hasWind()) {
                    value = bfvLocationManager.getAirSpeed();
                } else {
                    return "--";
                }

                break;
            case FIELD_QNH:
                value = surfaceView.alt.getSeaLevelPressure() / 100.0;
                break;
            case FIELD_FLIGHT_TIME:
                if (varioService.getFlight() != null) {
                    value = varioService.getFlight().getFlightTimeSeconds();
                } else {
                    return "--";
                }
                break;

            case FIELD_FLIGHT_DISTANCE:
                if (varioService.getFlight() != null) {
                    value = varioService.getFlight().getFlightDistanceFromStart();
                } else {
                    return "--";
                }
                break;
            case FIELD_FLIGHT_ALT:
                if (varioService.getFlight() != null) {
                    value = varioService.getFlight().getFlightAltFromStart();
                } else {
                    return "--";
                }
                break;
            case FIELD_PITOT_SPEED:
                if (varioService.hasPressure()) {
                    value = varioService.getPitotSpeed();
                } else {
                    return "--";
                }

                break;
            case FIELD_PITOT_CALIBRATION:
                if (varioService.hasPressure()) {
                    value = varioService.getPitotCalibration();
                } else {
                    return "--";
                }

                break;
        }
        value = value * field.getUnitMultiplier(multiplierIndex);

        String ret = df.format(value);
        if (ret.equals("-0.0")) {
            ret = "0.0";
        }

        return ret;
    }

    public Field findFieldForString(String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }
        return null;

    }

    public Field findFieldForId(int fieldId) {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (field.getId() == fieldId) {
                return field;
            }
        }
        return null;

    }

    public ArrayList<Field> getFields() {
        return fields;
    }

    public String[] getFieldNames() {
        String[] names = new String[fields.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = fields.get(i).getFieldName();

        }
        return names;
    }


}
