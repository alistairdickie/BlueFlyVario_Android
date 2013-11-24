/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2013 Alistair Dickie

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

package com.bfv.hardware;

import android.util.Log;
import com.bfv.BFVService;
import com.bfv.view.ViewComponentParameter;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 19/05/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class HardwareParameters {

    private String keys;
    private String values;

    private BFVService service;

    ArrayList<HardwareParameter> parameters;
    HardwareListActivity hardwareListActivity;

    public HardwareParameters(BFVService service) {
        this.service = service;
        this.setupParameters();
    }

    public ArrayList<HardwareParameter> getParameters() {
        return parameters;
    }

    public void setupParameters() {

        parameters = new ArrayList<HardwareParameter>();


        parameters.add(new HardwareParameter("BAC", 6, HardwareParameter.TYPE_BOOLEAN, "useAudioWhenConnected", 1.0, 0, 1)
                .setMessage("Check to enable hardware audio when connected."));

        parameters.add(new HardwareParameter("BAD", 6, HardwareParameter.TYPE_BOOLEAN, "useAudioWhenDisconnected", 1.0, 0, 1)
                .setMessage("Check to enable hardware audio when disconnected."));

        parameters.add(new HardwareParameter("BFK", 6, HardwareParameter.TYPE_DOUBLE, "positionNoise", 1000.0, 10, 10000)
                .setDecimalFormat("0.00")
                .setMessage("Kalman filter position noise."));

        parameters.add(new HardwareParameter("BFL", 6, HardwareParameter.TYPE_DOUBLE, "liftThreshold", 100.0, 0, 1000)
                .setDecimalFormat("0.00")
                .setMessage("The value in m/s of lift when the audio beeping will start."));

        parameters.add(new HardwareParameter("BOL", 6, HardwareParameter.TYPE_DOUBLE, "liftOffThreshold", 100.0, 0, 1000)
                .setDecimalFormat("0.00")
                .setMessage("The value in m/s of lift when the audio beeping will stop."));

        parameters.add(new HardwareParameter("BFQ", 6, HardwareParameter.TYPE_INT, "liftFreqBase", 1.0, 500, 2000)
                .setMessage("The audio frequency for lift beeps in Hz of 0 m/s."));

        parameters.add(new HardwareParameter("BFI", 6, HardwareParameter.TYPE_INT, "liftFreqIncrement", 1.0, 0, 1000)
                .setMessage("The increase in audio frequency for lift beeps in Hz for each 1 m/s."));


        parameters.add(new HardwareParameter("BFS", 6, HardwareParameter.TYPE_DOUBLE, "sinkThreshold", 100.0, 0, 1000)
                .setDecimalFormat("0.00")
                .setMessage("The value in -m/s of sink when the sink tone will start."));


        parameters.add(new HardwareParameter("BOS", 6, HardwareParameter.TYPE_DOUBLE, "sinkOffThreshold", 100.0, 0, 1000)
                .setDecimalFormat("0.00")
                .setMessage("The value in -m/s of sink when the sink tone will stop."));

        parameters.add(new HardwareParameter("BSQ", 6, HardwareParameter.TYPE_INT, "sinkFreqBase", 1.0, 250, 1000)
                .setMessage("The audio frequency for the sink tone in Hz of 0 m/s."));

        parameters.add(new HardwareParameter("BSI", 6, HardwareParameter.TYPE_INT, "sinkFreqIncrement", 1.0, 0, 1000)
                .setMessage("The decrease in audio frequency for sink tone in Hz for each -1 m/s."));


        parameters.add(new HardwareParameter("BTH", 6, HardwareParameter.TYPE_INT, "secondsBluetoothWait", 1.0, 0, 10000)
                .setMessage("The time that the hardware will be allow establishment of a bluetooth connection for when turned on."));


        parameters.add(new HardwareParameter("BRM", 6, HardwareParameter.TYPE_DOUBLE, "rateMultiplier", 100.0, 10, 100)
                .setDecimalFormat("0.00")
                .setMessage("The lift beep cadence -> 0.5 = beeping twice as fast as normal."));

        parameters.add(new HardwareParameter("BVL", 6, HardwareParameter.TYPE_DOUBLE, "volume", 1000.0, 1, 1000)
                .setDecimalFormat("0.000")
                .setMessage("The volume of beeps ->  0.1 is only about 1/2 as loud as 1.0."));

        parameters.add(new HardwareParameter("BOM", 7, HardwareParameter.TYPE_INT, "outputMode", 1.0, 0, 3)
                .setMessage("The output mode -> 0-BlueFlyVario(default), 1-LK8EX1, 2-LX, 3-FlyNet."));

        parameters.add(new HardwareParameter("BOF", 7, HardwareParameter.TYPE_INT, "outputFrequency", 1.0, 1, 50)
                .setMessage("The output frequency -> 1-every 20ms ... 50-every 20msx50=1000ms"));

        parameters.add(new HardwareParameter("BQH", 7, HardwareParameter.TYPE_INTOFFSET, "outputQNH", 80000.0, 0, 65535)
                .setMessage("QNH (in Pa), used for output alt for some output modes - (default 101325)"));


    }

    public HardwareParameter getParameter(String code) {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                HardwareParameter parameter = parameters.get(i);
                if (parameter.getCode().equals(code)) {
                    return parameter;
                }
            }
        }
        return null;
    }

    public void sendParameterValue(HardwareParameter parameter) {
        String code = parameter.getCode();
        int hardwareValue = parameter.getHardwareValue();
        String message = "$" + code + " " + hardwareValue + "*";
        service.sendConnectedHardwareMessage(message);
        //  Log.i("BFV", message);
    }

    public void requestParameterValues() {
        service.sendConnectedHardwareMessage("$BST*");
    }

    public void updateHardwareSettingsKeys(String line) {
        this.keys = line;


    }

    public void updateHardwareSettingsValues(String line) {
        this.values = line;

        if (keys != null) {
            String[] keySplit = keys.split(" ");
            String[] valueSplit = values.split(" ");
            for (int i = 1; i < keySplit.length; i++) {
                String key = keySplit[i];

                String value;
                if (i + 1 < valueSplit.length) {
                    value = valueSplit[i + 1];
                    HardwareParameter parameter = getParameter(key);
                    if (parameter != null) {
                        parameter.setHardwareValue(value);
                    }


                }

            }

        }

        hardwareListActivity.getMessageHandler().obtainMessage(HardwareListActivity.MESSAGE_UPDATE_HARDWARE_PARAMETERS).sendToTarget();


    }

    public void setHardwareListActivity(HardwareListActivity hardwareListActivity) {
        this.hardwareListActivity = hardwareListActivity;
    }

    public BFVService getService() {
        return service;
    }

    public String getHardwareVersion() {
        if (service.getHardwareVersion() >= 6) {
            return service.getHardwareVersion() + "";

        } else {
            return "3-4-5";
        }
    }


}
