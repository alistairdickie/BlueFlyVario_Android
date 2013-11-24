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
    public static final int UPDATE_VER = 3;
    public static final int UPDATE_BAT = 4;
    public static final int UPDATE_KEYS = 5;
    public static final int UPDATE_VALUES = 6;


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
            tmpReader = new BufferedReader(new InputStreamReader(tmpIn), 256);
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

        try {
            if (line == null || service == null) {
                return;
            }
            String[] split = line.split(" ");
            if (split[0] == null) {
                return;
            }

            if (split[0].equals("PRS")) {

                if (this.lastUpdateType == UPDATE_BAT) {
                    time = 0.04;
                } else {
                    time = 0.02;
                }

                this.lastUpdateType = UPDATE_PRS;

                int pressure = Integer.parseInt(split[1], 16);
                service.updatePressure(pressure, time);
                if (!firstPressure) {
                    service.firstPressure();
                    firstPressure = true;
                }

                if (split.length > 2) {
                    int pressure2 = Integer.parseInt(split[2], 16);

                    service.updatePitotPressures(pressure2, pressure);
                }


            } else if (split[0].equals("TMP")) {

                this.lastUpdateType = UPDATE_TMP;
                service.updateTemperature(Integer.parseInt(split[1]));

            } else if (split[0].equals("BAT")) {

                this.lastUpdateType = UPDATE_BAT;
                int bat = Integer.parseInt(split[1], 16);     //bat is in mV
                service.updateBattery(chargeFromVolts.getValue(bat / 1000.0));

            } else if (split[0].equals("BFV")) {


                this.lastUpdateType = UPDATE_VER;
                try {
                    int ver = Integer.parseInt(split[1]);
                    service.setHardwareVersion(ver);
                } catch (NumberFormatException e) {
                    service.setHardwareVersion(0);
                }


            } else if (split[0].equals("BST")) {


                this.lastUpdateType = UPDATE_KEYS;
                service.updateHardwareSettingsKeys(line);

            } else if (split[0].equals("SET")) {


                this.lastUpdateType = UPDATE_VALUES;
                service.updateHardwareSettingsValues(line);

            }
        } catch (NumberFormatException e) {
            return;
        }


    }


}
