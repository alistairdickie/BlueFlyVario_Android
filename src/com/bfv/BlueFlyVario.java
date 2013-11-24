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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bfv.audio.BeepThread;
import com.bfv.model.Altitude;
import com.bfv.model.KalmanFilteredAltitude;
import com.bfv.model.KalmanFilteredVario;
import com.bfv.model.Vario;
import com.bfv.view.map.MapViewManager;
import com.bfv.view.VarioSurfaceView;
import com.google.android.maps.MapActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.prefs.Preferences;

public class BlueFlyVario extends MapActivity {

    // Debugging
    private static final String TAG = "BFV";
    private static final boolean D = false;

    // Message types sent from the BFVService Handler
    public static final int MESSAGE_SERVICE_STATE_CHANGE = 1;
    public static final int MESSAGE_FLIGHT_STATE_CHANGE = 2;
    public static final int MESSAGE_VIEW_PAGE_CHANGE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_GPS_STATE_CHANGE = 7;
    public static final int MESSAGE_DRAW_MAP = 8;

    // Key names received from the BFVService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String DRAW_MAP = "draw_map";


    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_SETTINGS = 3;
    public static final int REQUEST_ENABLE_GPS = 4;
    public static final int REQUEST_FILE = 5;

    public static BlueFlyVario blueFlyVario;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private BFVService varioService = null;

    private BluetoothDevice device;

    private VarioSurfaceView varioSurface;
    private KalmanFilteredVario kalmanVario;
    private KalmanFilteredVario dampedVario;
    private KalmanFilteredAltitude alt;

    private BeepThread beeps;

    private boolean setAltFlag = false;

    private ImageView serviceStatus;
    private ImageView gpsStatus;
    private ImageView beepStatus;
    private ImageView flightStatus;
    private TextView viewPage;

    private MapViewManager mapViewManager;
    public boolean doubleBackToExitPressedOnce;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");


        blueFlyVario = this;


        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        BFVSettings.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        //check first run

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            //if (true){
            if (BFVSettings.sharedPrefs.getLong("lastRunVersionCode", 0) < pInfo.versionCode) {

                BFVSettings.setDefaultValues(this);
                firstRun();
            }
        } catch (PackageManager.NameNotFoundException e) {


        }


        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        this.setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // Set up the custom title
        // Layout Views
        TextView mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);

        serviceStatus = (ImageView) findViewById(R.id.title_status_bfv);
        gpsStatus = (ImageView) findViewById(R.id.title_status_gps);
        beepStatus = (ImageView) findViewById(R.id.title_status_audio);
        flightStatus = (ImageView) findViewById(R.id.title_status_flight);
        viewPage = (TextView) findViewById(R.id.title_view_page);


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupAltitudes();

        // Initialize the BFVService to perform bluetooth connections
        varioService = new BFVService(this, mHandler);
        varioService.setAltitude(alt);
        varioService.setUpLocationManager();   //must call after the altitude has been added.


        RelativeLayout layout = (RelativeLayout) this.findViewById(R.id.mainLayout);

        //initialize the surface
        varioSurface = new VarioSurfaceView(this, varioService);
        varioSurface.setZOrderOnTop(true);
        varioSurface.getHolder().setFormat(PixelFormat.TRANSPARENT);


        //initialize the map

        mapViewManager = new MapViewManager(this, varioSurface);

        layout.addView(mapViewManager.getMap());
        layout.addView(varioSurface);

        // If BT is not on, request that it be enabled.

        if (mBluetoothAdapter.isEnabled()) {

            tryAutoConnect();


        }
    }

    public void firstRun() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String version = "";
        int versionCode = -1;
        try {
            PackageInfo pInfo = BlueFlyVario.blueFlyVario.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {

        }

        builder.setTitle("BlueFlyVario " + version + "  [" + versionCode + "]")
                .setMessage("Thank you for installing this new version.\nDefault Settings have been enabled.")
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {


                        dialog.cancel();
                    }


                });

        AlertDialog alert = builder.create();
        alert.show();

        TextView messageText = (TextView) alert.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);


    }

    public MapViewManager getMapViewManager() {
        return mapViewManager;
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "++ ON START ++");


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    public boolean tryAutoConnect() {
        if (BFVSettings.sharedPrefs.getBoolean("bluetooth_autoconnect", false)) {
            String macAddress = BFVSettings.sharedPrefs.getString("bluetooth_macAddress", "");
            if (!macAddress.equals("")) {
                device = mBluetoothAdapter.getRemoteDevice(macAddress);
                varioService.connect(device);
                return true;
            }

        }
        return false;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        if (varioSurface != null) {
            varioSurface.onResumeVarioSurfaceView();
        }
        this.doubleBackToExitPressedOnce = false;

    }


    @Override
    public synchronized void onPause() {
        super.onPause();
        if (varioSurface != null) {
            varioSurface.onPauseVarioSurfaceView();
        }


        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (varioService != null) varioService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
        if (beeps != null) {
            beeps.onDestroy();
            beeps = null;
        }
        if (varioService != null) {
            varioService.onDestroy();
            varioService = null;
        }
        if (varioSurface != null) {
            varioSurface.onDestroy();
            varioSurface = null;
        }

    }

    @Override
    public void onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.exit_press_back_twice_message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected boolean isLocationDisplayed() {
        return super.isLocationDisplayed();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected int onGetMapDataSource() {
        return super.onGetMapDataSource();    //To change body of overridden methods use File | Settings | File Templates.
    }


    private void setupAltitudes() {

        SharedPreferences sharedPref = BFVSettings.sharedPrefs;

        double alt_setqnh = Double.valueOf(sharedPref.getString("alt_setqnh", "1013.25")) * 100;
        double alt_damp = Double.valueOf(sharedPref.getString("alt_damp", "0.05"));
        double var2_damp = Double.valueOf(sharedPref.getString("var2_damp", "0.05"));
        double kalman_noise = Double.valueOf(sharedPref.getString("kalman_noise", "0.2"));

        alt = new KalmanFilteredAltitude(alt_setqnh, "Alt");
        alt.setAltDamp(alt_damp);
        alt.setPositionNoise(kalman_noise);
        kalmanVario = alt.getKalmanVario();

        dampedVario = alt.getDampedVario();
        dampedVario.setVarDamp(var2_damp);


    }


//    /**
//     * Sends a message.
//     * @param message  A string of text to send.
//     */
//    private void sendMessage(String message) {
//        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BFVService.STATE_CONNECTED) {
//            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BFVService to write
//            byte[] send = message.getBytes();
//            mChatService.write(send);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
//        }
//    }

//    // The action listener for the EditText widget, to listen for the return key
//    private TextView.OnEditorActionListener mWriteListener =
//        new TextView.OnEditorActionListener() {
//        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//            // If the action is a key-up event on the return key, send the message
//            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//            if(D) Log.i(TAG, "END onEditorAction");
//            return true;
//        }
//    };


    // The Handler that gets information back from the BFVService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SERVICE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_SERVICE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BFVService.STATE_CONNECTED:
                            serviceStatus.setImageResource(R.drawable.ic_bfv_purple);
                            break;

                        case BFVService.STATE_CONNECTEDANDPRESSURE:
                            serviceStatus.setImageResource(R.drawable.ic_bfv_blue);
                            if (setAltFlag) {
                                boolean alt_setgps = BFVSettings.sharedPrefs.getBoolean("alt_setgps", false);
                                if (alt_setgps) {
                                    if (varioService.getBfvLocationManager() != null) {
                                        varioService.getBfvLocationManager().setGpsAltUpdateFlag(true);
                                    }

                                } else {
                                    int alt_setalt = Integer.valueOf(BFVSettings.sharedPrefs.getString("alt_setalt", "0"));

                                    double qnh = alt.setAltitude(alt_setalt) / 100.0;
                                    SharedPreferences.Editor prefsEditor = BFVSettings.sharedPrefs.edit();
                                    DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
                                    prefsEditor.putString("alt_setqnh", df.format(qnh));
                                    prefsEditor.commit();

                                    varioSurface.scheduleSetUpData();
                                }

                            }

                            boolean audio_enabled = BFVSettings.sharedPrefs.getBoolean("audio_enabled", false);
                            setBeeps(audio_enabled);
//
                            break;

                        case BFVService.STATE_CONNECTING:
                            serviceStatus.setImageResource(R.drawable.ic_bfv_yellow);
                            break;

                        case BFVService.STATE_NONE:
                            serviceStatus.setImageResource(R.drawable.ic_bfv_red);

                            setBeeps(false);
                            break;
                    }
                    break;

                case MESSAGE_GPS_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_GPS_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BFVLocationManager.STATE_GPS_NOT_ENABLED:
                            gpsStatus.setImageResource(R.drawable.ic_gps_red);
                            break;

                        case BFVLocationManager.STATE_GPS_OUT_OF_SERVICE:
                            gpsStatus.setImageResource(R.drawable.ic_gps_yellow);
                            break;

                        case BFVLocationManager.STATE_GPS_HAS_LOCATION:
                            gpsStatus.setImageResource(R.drawable.ic_gps_blue);
                            break;

                        case BFVLocationManager.STATE_GPS_TEMPORARILY_UNAVAILABLE:
                            gpsStatus.setImageResource(R.drawable.ic_gps_purple);
                            break;
                    }
                    break;
                case MESSAGE_FLIGHT_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_FLIGHT_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BFVService.FLIGHT_STATUS_FLYING:
                            flightStatus.setImageResource(R.drawable.ic_flight_blue);
                            break;

                        case BFVService.FLIGHT_STATUS_NONE:
                            flightStatus.setImageResource(R.drawable.ic_flight_purple);
                            break;
                    }
                    break;

                case MESSAGE_VIEW_PAGE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_VIEW_PAGE_CHANGE: " + msg.arg1);
                    viewPage.setText("" + msg.arg1);
                    doubleBackToExitPressedOnce = false;
                    break;


                case MESSAGE_DEVICE_NAME:

                    String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_DRAW_MAP:
                    boolean drawMap = msg.getData().getBoolean(DRAW_MAP);
                    if (mapViewManager != null) {
                        mapViewManager.setDrawMap(drawMap);
                    }

                    break;


            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    device = mBluetoothAdapter.getRemoteDevice(address);

                    varioService.connect(device);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    if (!tryAutoConnect()) {
                        Intent serverIntent = new Intent(this, DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    }


                } else {
                    // User did not enable Bluetooth or an error occured
                    //Log.d(TAG, "BT not enabled");
//                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//                    finish();
                }
                break;
            case REQUEST_ENABLE_GPS:
                if (varioService != null && varioService.getBfvLocationManager() != null) {
                    varioService.getBfvLocationManager().startLocationUpdates();
                }
                break;
            case REQUEST_FILE:
                if (data != null && data.getExtras() != null) {
                    String result = data.getExtras().getString("File");
                    if (result != null) {
                        File file = new File(result);
                        // Log.i("BFV", "File " + file.getAbsolutePath());
                        try {
                            FileInputStream in = new FileInputStream(file);
                            //   Log.i("BFV", "In " + in.toString());
                            varioSurface.loadViewsFromXML(in);
                        } catch (FileNotFoundException e) {

                        }
                    }

                }


                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.doubleBackToExitPressedOnce = false;
        MenuItem scan = menu.findItem(R.id.scan);
        if (varioService.getState() == BFVService.STATE_CONNECTED || varioService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE || varioService.getState() == BFVService.STATE_CONNECTING) {
            scan.setTitle(R.string.disconnect);
        } else if (varioService.getState() == BFVService.STATE_NONE) {
            scan.setTitle(R.string.connect);
        }
        boolean layoutEnabled = BFVSettings.sharedPrefs.getBoolean("layout_enabled", false);
        MenuItem layout = menu.findItem(R.id.layout);
        layout.setVisible(layoutEnabled);
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                if (item.getTitle().equals(getResources().getText(R.string.connect))) {

                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        return true;

                    } else {

                        Intent serverIntent = new Intent(this, DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                        return true;

                    }


                } else if (item.getTitle().equals(getResources().getText(R.string.disconnect))) {
                    varioService.disconnect();
                    return true;
                }

            case R.id.settings:

                Intent settingsIntent = new Intent(this, BFVSettings.class);
                //   settingsIntent.putExtra("firstRunDefault", firstRun);
                startActivityForResult(settingsIntent, REQUEST_SETTINGS);
                return true;


            case R.id.flight:
                if (varioService != null) {
                    Flight flight = varioService.getFlight();
                    if (flight == null) {
                        //  CharSequence[] items = {"Start", "Sim Example", "Cancel"};
                        CharSequence[] items = {"Start", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Flight Control");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {
                                    varioService.startFlight();
                                }
//                                if (item == 1) {
//                                    varioService.getBfvLocationManager().startSimulation();
//                                }

                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        CharSequence[] items = {"Stop", "Restart", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Flight Control");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {
                                    varioService.stopFlight();
                                }
                                if (item == 1) {
                                    varioService.stopFlight();
                                    varioService.startFlight();
                                }

                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }

                return true;
            case R.id.layout:
                if (varioSurface != null) {
                    CharSequence[] items = new CharSequence[]{"Add View Component", "Add Map Overlay", "Overlay Properties", "Page Properties", "New Page", "Delete Page", "Export Layout", "Import Layout", "Load Default Layout"};

                    final Context context = this;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Layout");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {

                            if (item == 0) {
                                varioSurface.chooseAddViewComponent(context);
                            }
                            if (item == 1) {
                                varioSurface.chooseAddMapOverlay(context);
                            } else if (item == 2) {
                                varioSurface.overlayProperties(context);
                            } else if (item == 3) {
                                varioSurface.viewPageProperties();
                            } else if (item == 4) {
                                varioSurface.addView();
                            } else if (item == 5) {
                                varioSurface.deleteView(context);
                            } else if (item == 6) {
                                varioSurface.exportViews();
                            } else if (item == 7) {
                                FileChooserListActivity.title = "Layout File";
                                FileChooserListActivity.fileExt = "xml";
                                FileChooserListActivity.dir = context.getExternalFilesDir(null);
                                Intent intent = new Intent(context, FileChooserListActivity.class);
                                startActivityForResult(intent, REQUEST_FILE);
                            } else if (item == 8) {
                                varioSurface.setUpDefaultViews();
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return true;
        }
        return false;
    }

    public BFVService getVarioService() {
        return varioService;
    }

    public KalmanFilteredVario getKalmanVario() {
        return kalmanVario;
    }

    public KalmanFilteredVario getDampedVario() {
        return dampedVario;
    }

    public KalmanFilteredAltitude getAlt() {
        return alt;
    }

    public boolean isSetAltFlag() {
        return setAltFlag;
    }

    public void setSetAltFlag(boolean setAltFlag) {
        this.setAltFlag = setAltFlag;
    }

    public VarioSurfaceView getVarioSurface() {
        return varioSurface;
    }

    public void setBeeps(boolean enableBeeping) {
        if (enableBeeping) {
            if (beeps == null) {
                beeps = new BeepThread(this, kalmanVario);
            } else {
                beeps.setRunning(false);
                beeps = new BeepThread(this, kalmanVario);
            }

            beepStatus.setImageResource(R.drawable.ic_audio_blue);


        } else {
            if (beeps != null) {
                beeps.setRunning(false);
                beeps = null;
            }
            beepStatus.setImageResource(R.drawable.ic_audio_purple);

        }


    }

    public BeepThread getBeeps() {
        return beeps;
    }

}