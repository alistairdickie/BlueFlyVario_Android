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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private BFVService service;

    public ConnectThread(BluetoothDevice device, int connectMethod, BFVService service) {
        this.service = service;
        mmDevice = device;
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice


        if (connectMethod == BFVService.CONNECT_NORMAL) {
            try {
                tmp = device.createRfcommSocketToServiceRecord(BFVService.MY_UUID);
            } catch (IOException e) {

                service.connectionFailed("CS: " + e.getLocalizedMessage());
            }
        } else if (connectMethod == BFVService.CONNECT_REFLECT) {
            try {
                Method m;
                m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                tmp = (BluetoothSocket) m.invoke(mmDevice, Integer.valueOf(1));
            } catch (NoSuchMethodException e) {
                service.connectionFailed("CS1: " + e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                service.connectionFailed("CS2: " + e.getLocalizedMessage());
            } catch (InvocationTargetException e) {
                service.connectionFailed("CS3: " + e.getLocalizedMessage());
            }

        } else {//default to connect normal method
            try {
                tmp = device.createRfcommSocketToServiceRecord(BFVService.MY_UUID);
            } catch (IOException e) {
                service.connectionFailed("CS: " + e.getLocalizedMessage());
            }
        }


        mmSocket = tmp;
    }

    public void run() {
        //Log.i(BFVService.TAG, "BEGIN mConnectThread");
        setName("ConnectThread");

        // Always cancel discovery because it will slow down a connection
        service.getAdapter().cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mmSocket.connect();
        } catch (IOException e) {
            service.connectionFailed("Socket: " + e.getLocalizedMessage());
            // Close the socket
            try {
                mmSocket.close();
            } catch (IOException e2) {
                Log.e(BFVService.TAG, "unable to close() socket during connection failure", e2);
            }
//                // Start the service over to restart listening mode
//                BFVService.this.start();
            return;
        }

        // Reset the ConnectThread because we're done
        synchronized (service) {
            service.setConnectThread(null);
        }

        // Start the connected thread
        service.connected(mmSocket, mmDevice);
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(BFVService.TAG, "close() of connect socket failed", e);
        }
    }
}