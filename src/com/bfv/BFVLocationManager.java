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

package com.bfv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.bfv.model.*;
import com.bfv.util.ArrayUtil;
import com.bfv.util.FitCircle;
import com.bfv.view.VarioSurfaceView;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class BFVLocationManager implements LocationListener {

    public static final int STATE_GPS_NOT_ENABLED = 0;
    public static final int STATE_GPS_TEMPORARILY_UNAVAILABLE = 1;
    public static final int STATE_GPS_HAS_LOCATION = 2;
    public static final int STATE_GPS_OUT_OF_SERVICE = 3;
    public static final int STATE_GPS_SIM = 5;    //todo - use this at some point


    private Context context;
    private Location location;
    private ArrayList<LocationAltVar> locations;
    private ArrayList<LocationAltVar> displayableLocations;

    private ArrayList<LocationAltVar> locationsToRemove;
    private double timeCut;

    private BFVService bfvService;
    private KalmanFilteredAltitude alt;
    private KalmanFilteredVario kalmanVario;
    private KalmanFilteredVario dampedVario;

    private double maxDistance;
    private double maxDriftedDistance;


    private double latDegLength;
    private double longDegLength;

    private boolean gpsAltUpdateFlag;

    private int state;

    //    private Thread locationTimeout;
//    private int locationTimeoutPauseMS = 10000;
    private Handler mHandler;

    private double[][] headingArray;
    private double[] heading;

    private double[] wind;
    private double[] windError;

    private boolean hasWind = false;

    private WindCalculator windCalculator;


    public BFVLocationManager(Context context, BFVService bfvService, Handler mHandler) {
        this.context = context;
        this.bfvService = bfvService;
        this.mHandler = mHandler;

        alt = bfvService.getAltitude();
        kalmanVario = alt.getKalmanVario();
        dampedVario = alt.getDampedVario();
        state = STATE_GPS_NOT_ENABLED;

        boolean alt_setgps = BFVSettings.sharedPrefs.getBoolean("alt_setgps", false);
        if (alt_setgps) {
            this.setGpsAltUpdateFlag(true);
        }

        SharedPreferences sharedPrefs = BFVSettings.sharedPrefs;

        int bufferSize = Integer.valueOf(sharedPrefs.getString("display_varioBufferSize", "500"));
        int bufferRate = Integer.valueOf(sharedPrefs.getString("display_varioBufferRate", "3"));

        setTimeCut(bufferSize / 50.0 * bufferRate);     //todo different setting
        windCalculator = new WindCalculator(16, 0.3, 300);     //todo settings


        location = new Location("");
        locations = new ArrayList<LocationAltVar>();
        displayableLocations = new ArrayList<LocationAltVar>();
        locationsToRemove = new ArrayList<LocationAltVar>();
        wind = new double[3];
        windError = new double[3];


        maybeAskEnableGPS();
        //startSimulation();

    }

    public void startSimulation() {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);

        location = new Location("");
        locations = new ArrayList<LocationAltVar>();
        displayableLocations = new ArrayList<LocationAltVar>();
        locationsToRemove = new ArrayList<LocationAltVar>();
        wind = new double[3];
        windError = new double[3];

        InputStream inputStream = BlueFlyVario.blueFlyVario.getResources().openRawResource(R.raw.flight_sim_test);
        FlightSimulator flightSimulator = new FlightSimulator(this, inputStream, 1.0f);

    }

    public void simulationFinished() {
        maybeAskEnableGPS();
    }

    public Location getLocation() {
        return location;
    }

    public double[] getHeading() {
        return new double[]{Math.sin(Math.toRadians(location.getBearing())) * location.getSpeed(), Math.cos(Math.toRadians(location.getBearing())) * location.getSpeed()};
    }

    public void maybeAskEnableGPS() {

        boolean location_askEnableGPS = BFVSettings.sharedPrefs.getBoolean("location_askEnableGPS", false);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) && location_askEnableGPS) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Do you want to enable GPS?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            enableLocationSettings();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setState(STATE_GPS_NOT_ENABLED);
                            dialog.cancel();

                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            this.startLocationUpdates();
        }


    }

    private synchronized void setState(int state) {
        if (this.state == state) {
            return;
        }

        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BlueFlyVario.MESSAGE_GPS_STATE_CHANGE, state, -1).sendToTarget();
    }

    public void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            setState(STATE_GPS_NOT_ENABLED);
        }
    }

    public void stopLocationUpdates() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);

    }


    public void onDestroy() {
        stopLocationUpdates();
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        BlueFlyVario.blueFlyVario.startActivityForResult(settingsIntent, BlueFlyVario.REQUEST_ENABLE_GPS);
    }


    public synchronized void onLocationChanged(Location location) {

        setState(STATE_GPS_HAS_LOCATION);
//        startTimeout();

        if (this.location != null) {
            float[] dist = {0};
            Location.distanceBetween(this.location.getLatitude(), this.location.getLongitude(), location.getLatitude(), location.getLongitude(), dist);
            if (dist[0] < 0.001) {//must have moved more than a mm for the location to register.
                return;
            }
        }

        this.location = location;

        if (bfvService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE && location.hasAltitude() && gpsAltUpdateFlag) {
            this.setAltFromGPS();
        }


        LocationAltVar newLoc = new LocationAltVar(location, alt.getDampedAltitude(), kalmanVario.getMinMaxAvgSinceLast()[1], dampedVario.getMinMaxAvgSinceLast()[1]);//take the max one
        locations.add(newLoc);


        calculateDisplayableLocations();
        bfvService.updateLocation(newLoc);


    }

    public void onStatusChanged(String s, int i, Bundle bundle) {
        //Log.i("BFV", "GPS Status Changed " + i);
        startLocationUpdates();
        if (i == LocationProvider.TEMPORARILY_UNAVAILABLE) {
            setState(STATE_GPS_TEMPORARILY_UNAVAILABLE);
        } else if (i == LocationProvider.OUT_OF_SERVICE) {
            setState(STATE_GPS_OUT_OF_SERVICE);
        }
    }

    public void onProviderEnabled(String s) {
        startLocationUpdates();
    }

    public void onProviderDisabled(String s) {
        stopLocationUpdates();
        setState(STATE_GPS_NOT_ENABLED);
    }


    public synchronized void calculateDisplayableLocations() {
        this.calculateLatLongDegLength();
        if (location == null) {
            return;
        }
        double tempMaxDistance = 0.0;
        float[] dist = {0.0f};

        LocationAltVar tempMaxVario = new LocationAltVar();

        displayableLocations.clear();

        long currentTime = System.currentTimeMillis();

        for (int i = locations.size() - 1; i >= 0; i--) {
            LocationAltVar locationAltVar = locations.get(i);
            Location oldLocation = locationAltVar.getLocation();

            double ageInSeconds = (location.getTime() - oldLocation.getTime()) / 1000.0;


            if (ageInSeconds < timeCut) {   //we are going to display this location

                //find the max distance for the displayable ones
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), oldLocation.getLatitude(), oldLocation.getLongitude(), dist);
                if (dist[0] > tempMaxDistance) {
                    tempMaxDistance = dist[0];

                }

                //find the max vario for the displayable ones
                locationAltVar.setMaxVar(false);
                if (locationAltVar.getVario() >= tempMaxVario.getVario()) {       //todo setting for vario1 or vario2
                    tempMaxVario = locationAltVar;

                }


                //prepare the x and y meters
                locationAltVar.y = (float) ((locationAltVar.getLocation().getLatitude() - location.getLatitude()) * latDegLength);
                locationAltVar.x = (float) ((locationAltVar.getLocation().getLongitude() - location.getLongitude()) * longDegLength);

                //prepare drift
                locationAltVar.setDriftedXY(currentTime);

                //add it to displayable
                displayableLocations.add(locationAltVar);
            } else {
                break; //assumes a time ordered list, oldest last
            }


        }

        maxDistance = tempMaxDistance;
        tempMaxVario.setMaxVar(true);


        //old wind calculation
//        int windSize = 90;//todo setting
//        int index = 0;
//
//        if (locations.size() > windSize) {
//            hasWind = true;
//            headingArray = new double[windSize][2];
//
//            for (int i = locations.size() - 1; i >= 0; i--) {
//                if (index >= windSize) {
//                    break;
//                }
//                LocationAltVar locationAltVar = locations.get(i);
//
//
//                headingArray[index][0] = Math.sin(Math.toRadians(locationAltVar.getLocation().getBearing())) * locationAltVar.getLocation().getSpeed();
//                headingArray[index][1] = Math.cos(Math.toRadians(locationAltVar.getLocation().getBearing())) * locationAltVar.getLocation().getSpeed();
//                index++;
//            }
//            wind = FitCircle.taubinNewton(headingArray);
//            windError = FitCircle.getErrors(headingArray, wind);
//            locations.get(locations.size() - 1).setWind(wind);//reset the wind on the most recent one.
//        }

        //new wind calculation
        windCalculator.addSpeedVector(location.getBearing(), location.getSpeed(), location.getTime() / 1000.0);

        headingArray = windCalculator.getPoints();
        if (headingArray.length > 2) {
            wind = FitCircle.taubinNewton(headingArray);
            windError = FitCircle.getErrors(headingArray, wind);
            locations.get(locations.size() - 1).setWind(wind);//reset the wind on the most recent one.
            hasWind = true;
        } else {
            hasWind = false;
        }


        //maxDriftDistance
        maxDriftedDistance = 0.0;
        for (int i = 0; i < displayableLocations.size(); i++) {
            LocationAltVar locationAltVar = displayableLocations.get(i);
            double driftedDist = Math.sqrt(locationAltVar.driftedX * locationAltVar.driftedX + locationAltVar.driftedY * locationAltVar.driftedY);
            if (driftedDist > maxDriftedDistance) {
                maxDriftedDistance = driftedDist;
            }

        }


    }

    public boolean hasWind() {
        return hasWind;
    }

    public synchronized ArrayList<LocationAltVar> getDisplayableLocations() {
        ArrayList<LocationAltVar> ret = new ArrayList<LocationAltVar>();
        for (int i = 0; i < displayableLocations.size(); i++) {
            LocationAltVar locationAltVar = displayableLocations.get(i);
            ret.add(locationAltVar);

        }
        return ret;
    }

    public double[][] getHeadingArray() {
        return headingArray;
    }

    public synchronized double getWindSpeed() {
        return Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1]);
    }

    public synchronized double getWindSpeedError() {
        return Math.sqrt(windError[0] * windError[0] + windError[1] * windError[1]);
    }

    public synchronized double getAirSpeed() {
        return wind[2];
    }

    public synchronized double getAirSpeedError() {
        return windError[2];
    }

    public synchronized double[] getWindError() {
        return ArrayUtil.copy(windError);
    }

    public synchronized double[] getWind() {
        return ArrayUtil.copy(wind);
    }

    public synchronized double getWindDirection() {
        return resolveDegrees(Math.toDegrees(Math.atan2(wind[1], wind[0]))); //the direction the wind is coming from! (not the wind vector direction - that would be + 90.0 instead
    }

    public double resolveDegrees(double degrees) {
        if (degrees < 0.0) {
            return resolveDegrees(degrees + 360.0);

        }
        if (degrees > 360.0) {
            return resolveDegrees(degrees - 360.0);
        }
        return degrees;
    }


    public void calculateLatLongDegLength() {
        if (location != null) {
            float[] results = {0.0f};
            Location.distanceBetween(location.getLatitude() - 0.005, location.getLongitude(), location.getLatitude() + 0.005, location.getLongitude(), results);
            latDegLength = results[0] * 100.0f;

            Location.distanceBetween(location.getLatitude(), location.getLongitude() - 0.005, location.getLatitude(), location.getLongitude() + 0.005, results);
            longDegLength = results[0] * 100.0f;


        }
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double getMaxDriftedDistance() {
        return maxDriftedDistance;
    }

    public void setTimeCut(double timeCut) {
        this.timeCut = timeCut;
    }

    public void setGpsAltUpdateFlag(boolean gpsAltUpdateFlag) {
        //Log.i("BFV", "gpsAltUpdateFlag" + gpsAltUpdateFlag);
        this.gpsAltUpdateFlag = gpsAltUpdateFlag;
    }

    public void setAltFromGPS() {
        gpsAltUpdateFlag = false;
        SharedPreferences.Editor prefsEditor = BFVSettings.sharedPrefs.edit();

        double qnh = BlueFlyVario.blueFlyVario.getAlt().setAltitude(location.getAltitude()) / 100.0;
        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
        prefsEditor.putString("alt_setqnh", df.format(qnh));
        prefsEditor.commit();
        Toast toast = Toast.makeText(context, "Altitude set from GPS to " + (int) (location.getAltitude()) + "m", Toast.LENGTH_SHORT);
        toast.show();
        VarioSurfaceView varioSurface = BlueFlyVario.blueFlyVario.getVarioSurface();
        if (varioSurface != null) {
            varioSurface.scheduleSetUpData();
        }

    }


//    public void startTimeout(){
//        if(locationTimeout != null && locationTimeout.isAlive()){
//            locationTimeout.interrupt();
//        }
//        locationTimeout = new Thread(this);
//        locationTimeout.start();
//    }
//
//
//    public void run() {
//        try {
//            Thread.sleep(locationTimeoutPauseMS);
//            setState(STATE_GPS_HAS_OLDLOCATION);
//        } catch (InterruptedException e) {
//
//        }
//    }
}
