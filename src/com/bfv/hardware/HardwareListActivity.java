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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.bfv.*;

import java.util.ArrayList;


/**
 * This Activity appears as a dialog.
 */
public class HardwareListActivity extends Activity {
    // Debugging
    private static final String TAG = "HardwareListActvity";
    private static final boolean D = true;
    public static final int MESSAGE_UPDATE_HARDWARE_PARAMETERS = 1;

    // Member fields

    private ArrayAdapter<String> hardwareParameterArrayAdapter;
    private HardwareParameters hardwareParameters;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.hardware_parameter_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to quit
        Button scanButton = (Button) findViewById(R.id.hardware_button_done);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK);
                finish();
            }
        });


        // Initialize array adapters.
        hardwareParameterArrayAdapter = new ArrayAdapter<String>(this, R.layout.hardware_parameter);


        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.hardware_parameter_list);
        pairedListView.setAdapter(hardwareParameterArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        hardwareParameters = BlueFlyVario.blueFlyVario.getVarioService().getHardwareParameters();


        //Find the name
        TextView textView = (TextView) findViewById(R.id.title_hardware_parameter_list);
        textView.setText("Hardware Version: " + hardwareParameters.getHardwareVersion());


        if (hardwareParameters != null) {
            hardwareParameters.setHardwareListActivity(this);
            hardwareParameters.requestParameterValues();
            ArrayList<HardwareParameter> parameters = hardwareParameters.getParameters();
            if (parameters.size() > 0) {
                findViewById(R.id.title_hardware_parameter_list).setVisibility(View.VISIBLE);
            }
            resetArrayAdapter(parameters);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    private boolean setParameterValue(String name, String value) {
        //  Log.i("BFV", "setParam" + name + value);
        if (hardwareParameters != null) {
            ArrayList<HardwareParameter> parameters = hardwareParameters.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                HardwareParameter parameter = parameters.get(i);
                if (parameter.getName().equals(name)) {
                    //   Log.i("BFV", "setParamName" + name + value);
                    parameter.setValueFromDialog(value);
                    hardwareParameters.sendParameterValue(parameter);


                    break;
                }
            }
            resetArrayAdapter(parameters);

        }

        return false;

    }

    private void resetArrayAdapter(ArrayList<HardwareParameter> parameters) {
        hardwareParameterArrayAdapter.clear();
        for (int i = 0; i < parameters.size(); i++) {
            HardwareParameter parameter = parameters.get(i);
            if (parameter.getMinHWVersion() <= hardwareParameters.getService().getHardwareVersion()) {
                hardwareParameterArrayAdapter.add(parameter.getName() + "\n   " + parameter.getValue());
            }
        }
    }


    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            HardwareParameter parameter = hardwareParameters.getParameters().get(position);
            showParameterDialog(parameter);

        }
    };

    private void showParameterDialog(HardwareParameter parameter) {

        int type = parameter.getType();
        switch (type) {
            case (HardwareParameter.TYPE_DOUBLE):
                final EditText inputDouble = new EditText(this);
                inputDouble.setSingleLine();

                inputDouble.setKeyListener(new DigitsKeyListener(true, true));
                final String nameDouble = parameter.getName();
                inputDouble.setText(parameter.getValue());


                new AlertDialog.Builder(this)
                        .setTitle("Edit Decimal Parameter")
                        .setMessage(parameter.getName() + ": " + parameter.getMessage(true))
                        .setView(inputDouble)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                setParameterValue(nameDouble, inputDouble.getText().toString());

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                break;
            case (HardwareParameter.TYPE_INT):
                final EditText inputInt = new EditText(this);
                inputInt.setSingleLine();
                inputInt.setKeyListener(new DigitsKeyListener(false, false));
                final String nameInt = parameter.getName();
                inputInt.setText(parameter.getValue());

                new AlertDialog.Builder(this)
                        .setTitle("Edit Integer Parameter")
                        .setMessage(parameter.getName() + ": " + parameter.getMessage(true))
                        .setView(inputInt)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                setParameterValue(nameInt, inputInt.getText().toString());

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                break;

            case (HardwareParameter.TYPE_INTOFFSET):
                final EditText inputIntOff = new EditText(this);
                inputIntOff.setSingleLine();
                inputIntOff.setKeyListener(new DigitsKeyListener(false, false));
                final String nameIntOff = parameter.getName();
                inputIntOff.setText(parameter.getValue());

                new AlertDialog.Builder(this)
                        .setTitle("Edit Integer Parameter")
                        .setMessage(parameter.getName() + ": " + parameter.getMessage(true))
                        .setView(inputIntOff)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                setParameterValue(nameIntOff, inputIntOff.getText().toString());

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                break;


            case (HardwareParameter.TYPE_BOOLEAN):
                final CheckBox check = new CheckBox(this);
                check.setGravity(Gravity.CENTER);
                final String nameBoolean = parameter.getName();
                boolean val = parameter.getBooleanValue();

                check.setChecked(val);

                new AlertDialog.Builder(this)
                        .setTitle("Edit Boolean Parameter")
                        .setMessage(parameter.getName() + ": " + parameter.getMessage(false))
                        .setView(check)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                setParameterValue(nameBoolean, check.isChecked() + "");

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                break;
            case (HardwareParameter.TYPE_INTLIST):
                int item = parameter.getIntValue();
                final String[] names = parameter.getNames();
                final String nameIntList = parameter.getName();


                new AlertDialog.Builder(this)
                        .setTitle("Edit Parameter")
                        .setMessage(parameter.getName() + ": " + parameter.getMessage(false))
                        .setSingleChoiceItems(names, item, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {

                                setParameterValue(nameIntList, item + "");
                                dialog.dismiss();

                            }
                        }).show();

                break;
        }


    }

    public Handler getMessageHandler() {
        return mHandler;
    }

    public void updatedHardwareSettingsValues() {

        hardwareParameterArrayAdapter.clear();
        ArrayList<HardwareParameter> parameters = hardwareParameters.getParameters();

        resetArrayAdapter(parameters);

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_HARDWARE_PARAMETERS:
                    updatedHardwareSettingsValues();
                    break;


            }
        }
    };


}
