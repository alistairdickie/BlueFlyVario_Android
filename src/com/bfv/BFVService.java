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

import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.bfv.model.Altitude;
import com.bfv.model.KalmanFilteredAltitude;
import com.bfv.model.LocationAltVar;


/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BFVService {
    // Debugging
    public static final String TAG = "BFVService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BlueFlyVario";

    // Unique UUID for this application
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing

    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_CONNECTEDANDPRESSURE = 4;  // now connected to a remote device that is spitting pressure

    public static final int FLIGHT_STATUS_NONE = 0;
    public static final int FLIGHT_STATUS_FLYING = 1;


    // Connect Methods
    public static final int CONNECT_NORMAL = 0; //use the normal connection
    public static final int CONNECT_REFLECT = 1; //use reflection to try to connect.


    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    //key data
    private KalmanFilteredAltitude altitude;
    private double temperature;
    private double battery;

    private DataBuffer dataBuffer;
    private Context context;

    private BFVLocationManager bfvLocationManager;

    private Flight flight;

    private boolean hasPressure;


    /**
     * Constructor. Prepares a new BlueFlyVario session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BFVService(Context context, Handler handler) {
        this.context = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;


    }

    public void setUpData() {
//        Log.i("BFV", "setUpData");
        ArrayList<DataSource> dataSources = new ArrayList<DataSource>();
        dataSources.add(altitude);
        dataSources.add(altitude.getKalmanVario());
        dataSources.add(altitude.getDampedVario());


        SharedPreferences sharedPrefs = BFVSettings.sharedPrefs;

        int bufferSize = Integer.valueOf(sharedPrefs.getString("display_varioBufferSize", "500"));
        int bufferRate = Integer.valueOf(sharedPrefs.getString("display_varioBufferRate", "3"));


        dataBuffer = new DataBuffer(dataSources, bufferSize, bufferRate);


    }


    private synchronized void setState(int state) {

        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BlueFlyVario.MESSAGE_SERVICE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public void firstPressure() {
        Thread thread = new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(1000);
                    hasPressure = true;
                    setState(STATE_CONNECTEDANDPRESSURE);
                } catch (InterruptedException e) {

                }

            }
        };
        thread.start();


    }

    public boolean hasPressure() {
        return hasPressure;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public BluetoothAdapter getAdapter() {

        return mAdapter;
    }

    public BFVLocationManager getBfvLocationManager() {
        return bfvLocationManager;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {

        return mState;
    }

    public void setConnectThread(ConnectThread mConnectThread) {
        this.mConnectThread = mConnectThread;
    }


    //    /**
//     * Start the chat service. Specifically start AcceptThread to begin a
//     * session in listening (server) mode. Called by the Activity onResume() */
//    public synchronized void start() {
//        if (D) Log.d(TAG, "start");
//
//        // Cancel any thread attempting to make a connection
//        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
//
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
//
//
//    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {

        if (mState == STATE_CONNECTED || mState == STATE_CONNECTEDANDPRESSURE) {
            return;
        }

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        int connectMethod = Integer.valueOf(BFVSettings.sharedPrefs.getString("bluetooth_connectMethod", CONNECT_REFLECT + ""));
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, connectMethod, this);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void disconnect() {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
//                boolean retry = true;
//                while(retry){
//                    try {
//                        mConnectThread.join();
//                    } catch (InterruptedException e) {
//                        retry = false;
//
//                    }
//                }

                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
//            boolean retry = true;
//            while(retry){
//                try {
//                    mConnectedThread.join();
//                } catch (InterruptedException e) {
//                    retry = false;
//
//                }
//            }
//            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {


        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, this);
        mConnectedThread.start();

        //update the sharedPreferences with the last connected devices
        SharedPreferences sharedPref = BFVSettings.sharedPrefs;
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        prefsEditor.putString("bluetooth_macAddress", device.getAddress());
        prefsEditor.putString("bluetooth_deviceName", device.getName());
        prefsEditor.commit();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BlueFlyVario.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BlueFlyVario.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
//    public void write(byte[] out) {
//        // Create temporary object
//        ConnectedThread r;
//        // Synchronize a copy of the ConnectedThread
//        synchronized (this) {
//            if (mState != STATE_CONNECTED) return;
//            r = mConnectedThread;
//        }
//        // Perform the write unsynchronized
//        r.write(out);
//    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    public void connectionFailed(String s) {

        this.setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BlueFlyVario.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BlueFlyVario.TOAST, "Unable to connect device\n" + s);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    public void connectionLost() {

        setState(STATE_NONE);


        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BlueFlyVario.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BlueFlyVario.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);


    }

    public synchronized void updatePressure(int pressure, double time) {


        altitude.addPressure((double) pressure, time);

        if (dataBuffer != null) {
            dataBuffer.addData();
        }


    }

    public synchronized DataBuffer getDataBuffer() {
        return dataBuffer;
    }


    public synchronized void setAltitude(KalmanFilteredAltitude altitude) {
        this.altitude = altitude;
        this.setUpData();
    }

    public synchronized KalmanFilteredAltitude getAltitude() {
        return altitude;
    }

    public synchronized void updateTemperature(int temperature) {
        this.temperature = temperature / 10.0;

    }

    public synchronized double getTemperature() {
        return temperature;
    }

    public synchronized void updateBattery(double battery) {

        this.battery = battery;

    }

    public synchronized double getBattery() {
        return battery;
    }


    public synchronized void updateMessage(String message) {
//        Message msg = this.getHandler().obtainMessage(BlueFlyVario.MESSAGE_READLINE);
//        Bundle bundle = new Bundle();
//        bundle.putString(BlueFlyVario.LINE, message);
//        msg.setData(bundle);
//        this.getHandler().sendMessage(msg);

    }

    public void setUpLocationManager() {
        bfvLocationManager = new BFVLocationManager(context, this, mHandler);
    }

    public void startFlight() {
        if (flight != null) {
            flight.stopFlight();
        }
        flight = new Flight(Flight.FLIGHT_CSV, context);
        setFlightStatus(BFVService.FLIGHT_STATUS_FLYING);
    }

    public void stopFlight() {
        flight.stopFlight();
        flight = null;
        setFlightStatus(BFVService.FLIGHT_STATUS_NONE);
    }

    public void updateLocation(LocationAltVar loc) {
        updateFlight(loc);
        BlueFlyVario.blueFlyVario.getVarioSurface().updateLocation(loc);
    }

    public void updateFlight(LocationAltVar loc) {
        if (flight != null) {
            flight.addLocationAltVar(loc);
        }
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlightStatus(int flightStatus) {
        mHandler.obtainMessage(BlueFlyVario.MESSAGE_FLIGHT_STATE_CHANGE, flightStatus, -1).sendToTarget();
    }

    public void onDestroy() {
        if (bfvLocationManager != null) {
            bfvLocationManager.onDestroy();
        }
        stop();
    }

    public Handler getmHandler() {
        return mHandler;
    }
}


