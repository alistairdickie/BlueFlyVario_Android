package com.bfv;

import android.location.Location;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 14/09/12
 * Time: 9:21 PM
 */
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
