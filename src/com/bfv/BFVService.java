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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import com.bfv.hardware.HardwareParameters;
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

    private ConnectTask mConnectTask;
    private ConnectedThread mConnectedThread;
    private int mState;

    private HardwareParameters hardwareParameters;


    //key data
    private KalmanFilteredAltitude altitude;
    private double temperature;
    private double battery;

    private DataBuffer dataBuffer;
    private Context context;

    private BFVLocationManager bfvLocationManager;

    private Flight flight;

    private boolean hasPressure;

    private int hardwareVersion;

    private boolean flagPitotCalibrated;
    private int pitotCalibrateCount;
    private double pitotCalibration;
    private double pressureDiff;
    private double pitotSpeed;


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

    public synchronized void setUpData() {
        Log.i("BFV", "setUpData");
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

    public int getmState() {
        return mState;
    }

    public void firstPressure() {
        Thread thread = new Thread() {
            @Override
            public void run() {

                try {
                    calibratePitot();
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

//    public void setConnectTask(ConnectTask mConnectTask) {
//        this.mConnectTask = mConnectTask;
//    }


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
        //   Log.i("BFV", "connect:" + device.getAddress());
        if (mState == STATE_CONNECTED || mState == STATE_CONNECTEDANDPRESSURE) {
            return;
        }

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectTask != null) {
                mConnectTask.cancel(true);
                mConnectTask = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //  int connectMethod = Integer.valueOf(BFVSettings.sharedPrefs.getString("bluetooth_connectMethod", CONNECT_REFLECT + ""));
        // Start the thread to connect with the given device
        mConnectTask = new ConnectTask();
        mConnectTask.execute(device);

        setState(STATE_CONNECTING);
    }

    public synchronized void disconnect() {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectTask != null) {
                mConnectTask.cancel(true);

                mConnectTask = null;
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
//        if (mConnectTask != null) {
//            mConnectTask.cancel(true);
//            mConnectTask = null;
//        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //hardwareParameters
        hardwareParameters = new HardwareParameters(this);

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, this);
        mConnectedThread.setPriority(Thread.MAX_PRIORITY);
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

        if (mConnectTask != null) {
            mConnectTask.cancel(true);
            mConnectTask = null;
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

        Log.i(BFVService.TAG, s);
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

    public synchronized void updatePitotPressures(int pitotPressure, int staticPressure) {

        if (flagPitotCalibrated) {
            pressureDiff = 0.95 * pressureDiff + 0.05 * (pitotPressure - staticPressure - pitotCalibration);
            double root = 2 * pressureDiff / 1.2754;
            if (root < 0) {
                root = 0.0;
            }
            pitotSpeed = Math.sqrt(root);


        } else {
            pitotCalibrateCount++;
            if (pitotCalibrateCount > 50) {
                if (pitotCalibrateCount <= 500) {
                    pitotCalibration += (pitotPressure - staticPressure);


                } else {
                    pitotCalibration = pitotCalibration / 450.0;
                    pitotCalibrateCount = 0;
                    flagPitotCalibrated = true;
                }
            }
        }
    }

    public synchronized double getPitotSpeed() {
        if (flagPitotCalibrated) {
            return pitotSpeed;
        } else {
            return -99.0;
        }

    }

    public synchronized void calibratePitot() {
        flagPitotCalibrated = false;
        pitotCalibrateCount = 0;
        pitotSpeed = 0;
        pressureDiff = 0;
    }

    public double getPitotCalibration() {
        return pitotCalibration;
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

    public synchronized void updateHardwareSettingsKeys(String line) {
        if (hardwareParameters != null) {
            hardwareParameters.updateHardwareSettingsKeys(line);
        }


    }

    public synchronized void updateHardwareSettingsValues(String line) {
        if (hardwareParameters != null) {
            hardwareParameters.updateHardwareSettingsValues(line);
        }

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

    public HardwareParameters getHardwareParameters() {
        return hardwareParameters;
    }

    public boolean sendConnectedHardwareMessage(String message) {
        if (mConnectedThread != null) {
            mConnectedThread.write(message.getBytes());
            return true;
        }
        return false;
    }

    public int getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(int hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    private class ConnectTask extends AsyncTask<BluetoothDevice, Void, Void> {
        String errorMessage;
        BluetoothDevice device;
        BluetoothSocket socket;


        protected Void doInBackground(BluetoothDevice... devices) {
            device = devices[0];
            //Log.i(BFVService.TAG, "address " + device.getAddress());
            //Log.i(BFVService.TAG, "name " + device.getName());
            //Log.i(BFVService.TAG, "class " + device.getBluetoothClass().toString());
            //Log.i(BFVService.TAG, "bondstate " + device.getBondState());

            //try uuidLookup - sometimes it might help for api v14 or later.
            uuidLookup(device);

            int connectMethod = Integer.valueOf(BFVSettings.sharedPrefs.getString("bluetooth_connectMethod", CONNECT_REFLECT + ""));
            //Log.i(BFVService.TAG, "connectMethod " + connectMethod);

            if (connectMethod == BFVService.CONNECT_NORMAL) {  //this should be the default
                try {
                    //Log.i(BFVService.TAG, "Try create secure");
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    boolean success = tryConnectToSocket(socket);
                    if (success) {
                        return null;
                    } else {
                        //Log.i(BFVService.TAG, "Try create insecure");
                        socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    }
                    success = tryConnectToSocket(socket);

                    if (!success) {
                        connectionFailed("Socket: " + errorMessage);
                    }

                    return null;


                } catch (IOException e) {

                    connectionFailed("CS: " + e.getLocalizedMessage());
                }
            }

            if (connectMethod == BFVService.CONNECT_REFLECT) {
                try {
                    //Log.i(BFVService.TAG, "Try create from reflect");
                    Method m;
                    m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    socket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
                    boolean success = tryConnectToSocket(socket);
                    if (!success) {
                        connectionFailed("Reflect Socket: " + errorMessage);
                    }
                    return null;
                } catch (NoSuchMethodException e) {
                    connectionFailed("RCS1: " + e.getLocalizedMessage());
                } catch (IllegalAccessException e) {
                    connectionFailed("RCS2: " + e.getLocalizedMessage());
                } catch (InvocationTargetException e) {
                    connectionFailed("RCS3: " + e.getLocalizedMessage());
                }

            }
            return null;
        }

        private boolean tryConnectToSocket(BluetoothSocket socket) {
            if (isCancelled()) {
                Log.i(BFVService.TAG, "tryConectToSocket Cancelled ");
                return false;
            }
            Log.i(BFVService.TAG, "Try cancelDiscovery");
            getAdapter().cancelDiscovery();
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.i(BFVService.TAG, "Try connect");
                socket.connect();
                Log.i(BFVService.TAG, "Passed Connect");
                connected(socket, device);
                return true;
            } catch (IOException e) {

                // Close the socket
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(BFVService.TAG, "unable to close() socket during connection failure", e2);
                }
                errorMessage = e.getLocalizedMessage();

            }
            return false;

        }

        private boolean uuidLookup(BluetoothDevice device) {

            //Log.i(BFVService.TAG, "Finding UUIDs");
            try {
                Method m = device.getClass().getMethod("getUuids", null);
                ParcelUuid[] uuids = (ParcelUuid[]) m.invoke(device, null);
                if (uuids == null) {
                    //Log.i(BFVService.TAG, "Null UUIDs");
                    //return false;
                }

                if (uuids != null) {
                    for (int i = 0; i < uuids.length; i++) {
                        ParcelUuid uuid = uuids[i];
                        Log.i(BFVService.TAG, uuid.toString());
                        if (uuid.getUuid().compareTo(MY_UUID) == 0) {
                            //Log.i(BFVService.TAG, "Contains SPP UUID");
                            return true;
                        }

                    }
                }


                //Log.i(BFVService.TAG, "SPP UUID Not found");
                //Log.i(BFVService.TAG, "Invoking fetch");
                Method fetch = device.getClass().getMethod("fetchUuidsWithSdp", null);
                fetch.invoke(device, null);
                return false;


            } catch (NoSuchMethodException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            Log.i(BFVService.TAG, "Finding Uuids could not be invoked");
            return false;


        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();    //To change body of overridden methods use File | Settings | File Templates.
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(BFVService.TAG, "close() of connect socket failed", e);
            }
        }
    }
}


