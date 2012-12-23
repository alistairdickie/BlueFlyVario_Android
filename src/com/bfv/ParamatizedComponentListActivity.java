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

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.bfv.view.*;
import com.bfv.view.component.FieldViewComponent;

import java.util.ArrayList;


/**
 * This Activity appears as a dialog.
 */
public class ParamatizedComponentListActivity extends Activity implements ColorPickerDialog.OnColorChangedListener {
    // Debugging
    private static final String TAG = "ParamatizedComponentListActvity";
    private static final boolean D = true;


    // Member fields

    private ArrayAdapter<String> viewComponentParameterArrayAdapter;
    private ParamatizedComponent selectedComponent;

    private String colorName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.view_component_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to quit
        Button scanButton = (Button) findViewById(R.id.button_done);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        Button delete = (Button) findViewById(R.id.button_delete);

        delete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                VarioSurfaceView.removeEditingComponent();
                finish();

            }
        });


        Button front = (Button) findViewById(R.id.button_move_front);

        front.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                VarioSurfaceView.moveEditingComponentFront();
                finish();

            }
        });

        Button back = (Button) findViewById(R.id.button_move_back);

        back.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                VarioSurfaceView.moveEditingComponentBack();
                finish();

            }
        });


        // Initialize array adapters.
        viewComponentParameterArrayAdapter = new ArrayAdapter<String>(this, R.layout.view_parameter);


        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.view_component_list);
        pairedListView.setAdapter(viewComponentParameterArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        selectedComponent = VarioSurfaceView.editingComponent;

        //Find the name
        TextView textView = (TextView) findViewById(R.id.title_view_component_list);
        String paramatizedComponentName = selectedComponent.getParamatizedComponentName();
        int type = selectedComponent.getParamatizedComponentType();


        if (type == ParamatizedComponent.TYPE_VIEW_PAGE) {
            delete.setVisibility(View.INVISIBLE);
            front.setVisibility(View.INVISIBLE);
            back.setVisibility(View.INVISIBLE);

        }
        if (paramatizedComponentName.equals("Field")) {
            paramatizedComponentName = paramatizedComponentName + ":" + ((FieldViewComponent) selectedComponent).getField().getFieldName();
        }

        textView.setText(paramatizedComponentName + " " + textView.getText());


        if (selectedComponent != null) {
            ArrayList<ViewComponentParameter> parameters = selectedComponent.getParameters();
            if (parameters.size() > 0) {
                findViewById(R.id.title_view_component_list).setVisibility(View.VISIBLE);
            }
            for (int i = 0; i < parameters.size(); i++) {
                ViewComponentParameter parameter = parameters.get(i);
                viewComponentParameterArrayAdapter.add(parameter.getName() + "\n   " + parameter.getValue());
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    private boolean setParameterValue(String name, String value) {
        //  Log.i("BFV", "setParam" + name + value);
        if (selectedComponent != null) {
            ArrayList<ViewComponentParameter> parameters = selectedComponent.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                ViewComponentParameter parameter = parameters.get(i);
                if (parameter.getName().equals(name)) {
                    //   Log.i("BFV", "setParamName" + name + value);
                    parameter.setValue(value);
                    selectedComponent.setParameterValue(parameter);
                    viewComponentParameterArrayAdapter.clear();

                    break;
                }
            }
            for (int i = 0; i < parameters.size(); i++) {
                ViewComponentParameter parameter = parameters.get(i);
                viewComponentParameterArrayAdapter.add(parameter.getName() + "\n   " + parameter.getValue());
            }
        }

        return false;

    }

    private boolean setColorParameterValue(String name, int color) {
        //  Log.i("BFV", "setParam" + name + value);
        if (selectedComponent != null) {
            ArrayList<ViewComponentParameter> parameters = selectedComponent.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                ViewComponentParameter parameter = parameters.get(i);
                if (parameter.getName().equals(name)) {
                    //   Log.i("BFV", "setParamName" + name + value);
                    parameter.setColor(color);
                    selectedComponent.setParameterValue(parameter);
                    viewComponentParameterArrayAdapter.clear();

                    break;
                }
            }
            for (int i = 0; i < parameters.size(); i++) {
                ViewComponentParameter parameter = parameters.get(i);
                viewComponentParameterArrayAdapter.add(parameter.getName() + "\n   " + parameter.getValue());
            }
        }

        return false;

    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            ViewComponentParameter parameter = selectedComponent.getParameters().get(position);
            showParameterDialog(parameter);
//
//
//            // Get the device MAC address, which is the last 17 chars in the View
//            String info = ((TextView) v).getText().toString();
//            String address = info.substring(info.length() - 17);
//
//            // Create the result Intent and include the MAC address
//            Intent intent = new Intent();
//            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
//
//            // Set result and finish this Activity
//            setResult(Activity.RESULT_OK, intent);
//            finish();
        }
    };

    private void showParameterDialog(ViewComponentParameter parameter) {

        int type = parameter.getType();
        switch (type) {
            case (ViewComponentParameter.TYPE_DOUBLE):
                final EditText inputDouble = new EditText(this);
                inputDouble.setKeyListener(new DigitsKeyListener(true, true));
                final String nameDouble = parameter.getName();
                inputDouble.setText(parameter.getValue());

                new AlertDialog.Builder(this)
                        .setTitle("Edit Decimal Parameter")
                        .setMessage(parameter.getName())
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
            case (ViewComponentParameter.TYPE_INT):
                final EditText inputInt = new EditText(this);
                inputInt.setKeyListener(new DigitsKeyListener(false, false));
                final String nameInt = parameter.getName();
                inputInt.setText(parameter.getValue());

                new AlertDialog.Builder(this)
                        .setTitle("Edit Integer Parameter")
                        .setMessage(parameter.getName())
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

            case (ViewComponentParameter.TYPE_COLOR):
                int color = parameter.getColorValue();
                colorName = parameter.getName();
                new ColorPickerDialog(this, this, color).show();
                break;


            case (ViewComponentParameter.TYPE_STRING):
                final EditText inputString = new EditText(this);
                final String nameString = parameter.getName();
                inputString.setText(parameter.getValue());

                new AlertDialog.Builder(this)
                        .setTitle("Edit Text Parameter")
                        .setMessage(parameter.getName())
                        .setView(inputString)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                setParameterValue(nameString, inputString.getText().toString());

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

                break;

            case (ViewComponentParameter.TYPE_BOOLEAN):
                final CheckBox check = new CheckBox(this);
                check.setGravity(Gravity.CENTER);
                final String nameBoolean = parameter.getName();
                boolean val = parameter.getBooleanValue();

                check.setChecked(val);

                new AlertDialog.Builder(this)
                        .setTitle("Edit Boolean Parameter")
                        .setMessage(parameter.getName())
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
            case (ViewComponentParameter.TYPE_INTLIST):
                int item = parameter.getIntValue();
                final String[] names = parameter.getNames();
                final String nameIntList = parameter.getName();


                new AlertDialog.Builder(this)
                        .setTitle("Edit Parameter")
                                //.setMessage(parameter.getIntListName())
                        .setSingleChoiceItems(names, item, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {

                                setParameterValue(nameIntList, item + "");
                                dialog.dismiss();

                            }
                        }).show();

                break;
        }


    }

    public void colorChanged(int newColor) {
        setColorParameterValue(colorName, newColor);
    }


}
