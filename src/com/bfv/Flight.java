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

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import com.bfv.model.LocationAltVar;

import java.io.*;
import java.util.ArrayList;

public class Flight {

    public static int FLIGHT_CSV = 0;

    private ArrayList<LocationAltVar> locations;
    private long startTime;
    private long stopTime;
    boolean mExternalStorageAvailable = false;
    PrintWriter out = null;
    private boolean started;


    public Flight(int outputType, Context context) {      //todo - implement output types such as IGC, KML, etc
        startTime = System.currentTimeMillis();
        locations = new ArrayList<LocationAltVar>();


        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // We can read and write the media
            // Log.i("BFV", "Media");

            mExternalStorageAvailable = true;
            Time time = new Time();
            time.set(startTime);
            String startString = time.format("%Y-%m-%d_%H-%M-%S");

            File myFile = new File(context.getExternalFilesDir(null), "Flight_" + startString + ".csv");
            try {
                out = new PrintWriter(new FileWriter(myFile));
                out.println("#BlueFlyVario CSV Flight File");
                out.println("#Start Time," + startString);
                out.println(LocationAltVar.getCSVHeaders());
            } catch (IOException e) {
                Log.e("BFV", "Media Exception " + e.getLocalizedMessage());
            }

        } else {
            mExternalStorageAvailable = false;
        }
        started = true;
    }

    public synchronized void addLocationAltVar(LocationAltVar loc) {
        if (started) {
            locations.add(loc);
            if (out != null) {
                out.println(loc.getCSVString());
            }
        }


    }

    public synchronized void stopFlight() {
        started = false;
        stopTime = System.currentTimeMillis();

        Time time = new Time();
        time.set(stopTime);
        String stopString = time.format("%Y-%m-%d_%H-%M-%S");
        if (out != null) {
            out.println("#Stop Time," + stopString);
            out.close();
        }

    }

    public double getFlightTimeSeconds() {
        if (started) {
            return (System.currentTimeMillis() - startTime) / 1000.0;
        } else {
            return startTime - stopTime;
        }

    }

    public double getFlightDistanceFromStart() {
        if (locations.size() > 0) {
            Location start = locations.get(0).getLocation();
            Location current = locations.get(locations.size() - 1).getLocation();
            float[] dist = {0.0f};
            Location.distanceBetween(start.getLatitude(), start.getLongitude(), current.getLatitude(), current.getLongitude(), dist);
            return dist[0];
        } else {
            return 0.0;
        }
    }

    public double getFlightAltFromStart() {
        if (locations.size() > 0) {
            double startAlt = locations.get(0).getBaroAlt();
            double currentAlt = locations.get(locations.size() - 1).getBaroAlt();
            return currentAlt - startAlt;
        } else {
            return 0.0;
        }
    }

    public boolean isExternalStorageAvailable() {
        return mExternalStorageAvailable;
    }


}
