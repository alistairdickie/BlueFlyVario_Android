package com.bfv;

import android.bluetooth.BluetoothSocket;
//import android.os.Bundle;
//import android.os.Message;
import android.util.Log;
import com.bfv.util.PiecewiseLinearFunction;
import com.bfv.util.Point2d;

import java.io.*;

/**
 * This thread runs during a connection with a remote device.
 * It handles all incoming and outgoing transmissions.
 */
public class ConnectedThread extends Thread {
    public static final int UPDATE_NONE = 0;
    public static final int UPDATE_PRS = 1;
    public static final int UPDATE_TMP = 2;
    public static final int UPDATE_MSG = 3;
    public static final int UPDATE_BAT = 4;


    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;
    private final BufferedReader mmReader;
    private BFVService service;
    private PiecewiseLinearFunction chargeFromVolts;
    private boolean firstPressure;

    private int lastUpdateType = UPDATE_NONE;
//
//
//    private long lastTime;
//
//    private double pressurePressureDuration;
//    private double pauseTime;
//    private boolean pressureTimePaused;
//
//    private int pressureMeasurements;
//    private int pauses;

    private double time;
    private double timeInterval = 0.02;  //50Hz = 20 ms


    public ConnectedThread(BluetoothSocket socket, BFVService service) {

        this.service = service;

        Log.d(BFVService.TAG, "create ConnectedThread");
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        BufferedReader tmpReader = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
            tmpReader = new BufferedReader(new InputStreamReader(tmpIn));
        } catch (IOException e) {
            Log.e(BFVService.TAG, "temp sockets not created", e);
        }


        mmOutStream = tmpOut;
        mmReader = tmpReader;

        chargeFromVolts = new PiecewiseLinearFunction(new Point2d(3.6, 0.0));
        chargeFromVolts.addNewPoint(new Point2d(3.682, 0.032));
        chargeFromVolts.addNewPoint(new Point2d(3.696, 0.124));
        chargeFromVolts.addNewPoint(new Point2d(3.75, 0.212));
        chargeFromVolts.addNewPoint(new Point2d(3.875, 0.624));
        chargeFromVolts.addNewPoint(new Point2d(3.96, 0.73));
        chargeFromVolts.addNewPoint(new Point2d(4.16, 1.0));

        firstPressure = false;


    }

    public void run() {
        //Log.i(BFVService.TAG, "BEGIN mConnectedThread");

        String line = null;

        while (true) {
            try {
                // Read from the InputStream
                line = mmReader.readLine();
                this.handleLine(line);

            } catch (IOException e) {
                Log.d(BFVService.TAG, "disconnected", e);
                service.connectionLost();
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);

        } catch (IOException e) {
            Log.e(BFVService.TAG, "Exception during write", e);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(BFVService.TAG, "close() of connect socket failed", e);
        }
    }

    public void handleLine(String line) {
        time += timeInterval;
        if (line == null || service == null) {
            return;
        }
        String[] split = line.split(" ");
        if (split[0] == null) {
            return;
        }
        if (split[0].equals("PRS")) {

            this.lastUpdateType = UPDATE_PRS;


            service.updatePressure(Integer.parseInt(split[1], 16), time);
            if (!firstPressure) {
                service.firstPressure();
                firstPressure = true;
            }


        } else if (split[0].equals("TMP")) {

            this.lastUpdateType = UPDATE_TMP;
            service.updateTemperature(Integer.parseInt(split[1]));

        } else if (split[0].equals("BAT")) {

            this.lastUpdateType = UPDATE_BAT;
            int bat = Integer.parseInt(split[1], 16);     //bat is in mV
            service.updateBattery(chargeFromVolts.getValue(bat / 1000.0));

        } else if (split[0].equals("BFV")) {
            time = 0; //the device has been reset so start from scratch


            this.lastUpdateType = UPDATE_MSG;
            service.updateMessage(split[1]);

        }


    }


}
