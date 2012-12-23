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

import android.location.Location;

import java.io.*;

public class FlightSimulator implements Runnable {
    private BFVLocationManager locationManager;
    private double speed;
    private BufferedReader reader = null;


    public FlightSimulator(BFVLocationManager locationManager, InputStream fileInput, double speed) {
        this.locationManager = locationManager;
        this.speed = speed;
        reader = new BufferedReader(new InputStreamReader(fileInput));

        new Thread(this).start();
    }

    public void run() {
        String line = null;

        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split(",");
                // #Latitude	 Longitude	 Time(ms)	 Accuracy	 Bearing	 Speed	 GPS Altidude	 Pressure Altidude	 Max Vario
                double latitude = Double.parseDouble(split[0]);
                double longitude = Double.parseDouble(split[1]);
                long time = Long.parseLong(split[2]);
                float accuracy = Float.parseFloat(split[3]);
                float bearing = Float.parseFloat(split[4]);
                float speed = Float.parseFloat(split[5]);
                double alt = Double.parseDouble(split[6]);
                Location loc = new Location("");
                loc.setLatitude(latitude);
                loc.setLongitude(longitude);
                loc.setTime(time);
                loc.setAccuracy(accuracy);
                loc.setBearing(bearing);
                loc.setSpeed(speed);
                loc.setAltitude(alt);
                locationManager.onLocationChanged(loc);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }


            }
        } catch (IOException e) {

        }
        locationManager.simulationFinished();
    }
}
