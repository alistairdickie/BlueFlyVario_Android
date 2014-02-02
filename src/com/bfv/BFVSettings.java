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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.bfv.hardware.HardwareListActivity;
import com.bfv.model.Vario;
import com.bfv.view.VarioSurfaceView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class BFVSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static SharedPreferences sharedPrefs;


    public static boolean stopSetQNH = false;

    public static void setDefaultValues(Context context) {
        sharedPrefs.edit().clear().commit();
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            if (sharedPrefs.getLong("lastRunVersionCode", 0) < pInfo.versionCode) {

                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putLong("lastRunVersionCode", pInfo.versionCode);
                editor.commit();
            }
        } catch (PackageManager.NameNotFoundException e) {


        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.e("BFV", "CREATE Settings");
        super.onCreate(savedInstanceState);

        if (sharedPrefs == null) {
            Context context = getApplicationContext();
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }


        addPreferencesFromResource(R.xml.preferences);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.findPreference("default").setOnPreferenceClickListener(this);
        this.findPreference("about").setOnPreferenceClickListener(this);
        //  this.findPreference("calibrate").setOnPreferenceClickListener(this);
        Preference hardware = this.findPreference("hardware");


        if (BlueFlyVario.blueFlyVario.getVarioService().getmState() == BFVService.STATE_CONNECTED || BlueFlyVario.blueFlyVario.getVarioService().getmState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
            hardware.setEnabled(true);
        } else {
            hardware.setEnabled(false);
        }
        hardware.setOnPreferenceClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        Preference hardware = this.findPreference("hardware");
        if (hardware != null) {
            if (BlueFlyVario.blueFlyVario.getVarioService().getmState() == BFVService.STATE_CONNECTED || BlueFlyVario.blueFlyVario.getVarioService().getmState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
                hardware.setEnabled(true);
            } else {
                hardware.setEnabled(false);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }


    public void resetDefaults(boolean makeToast) {


        BlueFlyVario.blueFlyVario.getVarioService().getBfvLocationManager().setGpsAltUpdateFlag(false);
        BlueFlyVario.blueFlyVario.setSetAltFlag(false);

        //sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        stopSetQNH = true;

        setDefaultValues(BlueFlyVario.blueFlyVario);

        if (makeToast) {
            Toast toast = Toast.makeText(this, "Default Settings Restored", Toast.LENGTH_SHORT);
            toast.show();

        }

        //sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        //this.findPreference("default").setOnPreferenceClickListener(this);


        BlueFlyVario bfv = BlueFlyVario.blueFlyVario;

        double alt_setqnh = Double.valueOf(sharedPrefs.getString("alt_setqnh", "1013.25")) * 100.0;
        bfv.getAlt().setSeaLevelPressure(alt_setqnh);

        double alt_damp = Double.valueOf(sharedPrefs.getString("alt_damp", "0.05"));
        bfv.getAlt().setAltDamp(alt_damp);

        double kalman_noise = Double.valueOf(sharedPrefs.getString("kalman_noise", "0.2"));
        bfv.getAlt().setPositionNoise(kalman_noise);

        double var2_damp = Double.valueOf(sharedPrefs.getString("var2_damp", "0.05"));
        bfv.getDampedVario().setVarDamp(var2_damp);

        boolean audio_enabled = sharedPrefs.getBoolean("audio_enabled", false);
        BlueFlyVario.blueFlyVario.setBeeps(audio_enabled);

        if (BlueFlyVario.blueFlyVario.getBeeps() != null) {
            int audio_basehz = Integer.valueOf(sharedPrefs.getString("audio_basehz", "1000"));
            BlueFlyVario.blueFlyVario.getBeeps().setBase(audio_basehz);


            int audio_incrementhz = Integer.valueOf(sharedPrefs.getString("audio_incrementhz", "100"));
            BlueFlyVario.blueFlyVario.getBeeps().setIncrement(audio_incrementhz);


            double vario_audio_threshold = Double.valueOf(sharedPrefs.getString("vario_audio_threshold", "0.2"));
            BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioThreshold(vario_audio_threshold);

            double vario_audio_cutoff = Double.valueOf(sharedPrefs.getString("vario_audio_cutoff", "0.05"));
            BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioCutoff(vario_audio_cutoff);

            int sink_audio_basehz = Integer.valueOf(sharedPrefs.getString("sink_audio_basehz", "500"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkBase(sink_audio_basehz);


            int sink_audio_incrementhz = Integer.valueOf(sharedPrefs.getString("sink_audio_incrementhz", "100"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkIncrement(sink_audio_incrementhz);


            double sink_audio_threshold = Double.valueOf(sharedPrefs.getString("sink_audio_threshold", "-2.0"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioThreshold(sink_audio_threshold);

            double sink_audio_cutoff = Double.valueOf(sharedPrefs.getString("sink_audio_cutoff", "-1.5"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioCutoff(sink_audio_cutoff);

        }


        bfv.getVarioSurface().scheduleSetUpData();
        bfv.getVarioSurface().readSettings();
        // stopCommitsInListener = false;

    }


    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("default")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);


            builder.setMessage("Are you sure you want to restore the default settings?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            resetDefaults(true);

                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

            return true;

        } else if (preference.getKey().equals("about")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            BlueFlyVario.blueFlyVario.getVarioService().calibratePitot();

            String version = "";
            int versionCode = -1;
            try {
                PackageInfo pInfo = BlueFlyVario.blueFlyVario.getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
                versionCode = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {

            }


            builder.setMessage("BlueFlyVario " + version + "  [" + versionCode + "]")
                    .setCancelable(true)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {


                            dialog.cancel();
                        }


                    });
            AlertDialog alert = builder.create();
            alert.show();

            return true;

        } else if (preference.getKey().equals("calibrate")) {
            BlueFlyVario.blueFlyVario.getVarioService().calibratePitot();
            return true;
        } else if (preference.getKey().equals("hardware")) {

            if (BlueFlyVario.blueFlyVario.getVarioService().getHardwareVersion() < 6) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);


                builder.setMessage("Hardware version: "
                        + BlueFlyVario.blueFlyVario.getVarioService().getHardwareParameters().getHardwareVersion()
                        + "\n\nSetting hardware parameters not supported on hardware versions earlier than 6")
                        .setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }


                        });
                AlertDialog alert = builder.create();
                alert.show();

                return true;

            } else {

                Intent parameterIntent = new Intent(BlueFlyVario.blueFlyVario, HardwareListActivity.class);
                BlueFlyVario.blueFlyVario.startActivity(parameterIntent);
                return true;
            }

        }
        return false;

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


        //Log.i("BFV", "pref:" + key + setPrefChangeFlag);
        BFVService varioService = BlueFlyVario.blueFlyVario.getVarioService();
        VarioSurfaceView varioSurface = BlueFlyVario.blueFlyVario.getVarioSurface();

        if (key.equals("alt_setalt")) {
            int alt_setalt = Integer.valueOf(sharedPreferences.getString("alt_setalt", "0"));

            if (BlueFlyVario.blueFlyVario.getVarioService().getState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
                double qnh = BlueFlyVario.blueFlyVario.getAlt().setAltitude(alt_setalt) / 100.0;

                //Log.e("BFV", "setalt" + alt_setalt + " notQNH?" + stopSetQNH);
                if (!stopSetQNH) {
                    SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                    DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
                    prefsEditor.putString("alt_setqnh", df.format(qnh));
                    prefsEditor.commit();
                }
                stopSetQNH = false;
                varioSurface.scheduleSetUpData();


            } else {
                BlueFlyVario.blueFlyVario.setSetAltFlag(true);
            }


        }

        if (key.equals("alt_setqnh")) {
            //Log.e("BFV", "setqnh");
            double alt_setqnh = Double.valueOf(sharedPreferences.getString("alt_setqnh", "1013.25")) * 100.0;
            BlueFlyVario.blueFlyVario.getAlt().setSeaLevelPressure(alt_setqnh);
            varioSurface.scheduleSetUpData();
        }

        if (key.equals("alt_setgps")) {
            boolean alt_setgps = sharedPreferences.getBoolean("alt_setgps", false);
            //Log.e("BFV", "setgps" + alt_setgps);
            if (alt_setgps) {
                if (varioService != null && varioService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
                    if (varioService.getBfvLocationManager() != null) {
                        varioService.getBfvLocationManager().setGpsAltUpdateFlag(true);
                    }

                } else {
                    BlueFlyVario.blueFlyVario.setSetAltFlag(true);
                }
            } else {
                if (varioService != null && varioService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
                    if (varioService.getBfvLocationManager() != null) {
                        varioService.getBfvLocationManager().setGpsAltUpdateFlag(false);
                    }

                } else {
                    BlueFlyVario.blueFlyVario.setSetAltFlag(false);
                }

            }


        }

        if (key.equals("alt_damp")) {
            double alt_damp = Double.valueOf(sharedPreferences.getString("alt_damp", "0.05"));
            BlueFlyVario.blueFlyVario.getAlt().setAltDamp(alt_damp);
        }

        if (key.equals("kalman_noise")) {
            double kalman_noise = Double.valueOf(sharedPreferences.getString("kalman_noise", "0.2"));
            BlueFlyVario.blueFlyVario.getAlt().setPositionNoise(kalman_noise);
        }


        if (key.equals("var2_damp")) {
            double var2_damp = Double.valueOf(sharedPreferences.getString("var2_damp", "0.05"));
            BlueFlyVario.blueFlyVario.getDampedVario().setVarDamp(var2_damp);
        }


        if (key.equals("display_varioBufferSize")) {
            //int varioBufferSize = Integer.valueOf(sharedPreferences.getString("display_varioBufferSize", "500" ));
            varioSurface.scheduleSetUpData();
        }
        if (key.equals("display_varioBufferRate")) {
            //int varioBufferRate = Integer.valueOf(sharedPreferences.getString("display_varioBufferRate", "3" ));
            varioSurface.scheduleSetUpData();
        }

        if (key.equals("audio_basehz")) {
            int audio_basehz = Integer.valueOf(sharedPreferences.getString("audio_basehz", "1000"));
            BlueFlyVario.blueFlyVario.getBeeps().setBase(audio_basehz);
        }
        if (key.equals("audio_incrementhz")) {
            int audio_incrementhz = Integer.valueOf(sharedPreferences.getString("audio_incrementhz", "100"));
            BlueFlyVario.blueFlyVario.getBeeps().setIncrement(audio_incrementhz);

        }

        if (key.equals("vario_audio_threshold")) {
            double vario_audio_threshold = Double.valueOf(sharedPreferences.getString("vario_audio_threshold", "0.2"));
            BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioThreshold(vario_audio_threshold);
        }
        if (key.equals("vario_audio_cutoff")) {
            double vario_audio_cutoff = Double.valueOf(sharedPreferences.getString("vario_audio_cutoff", "0.05"));
            BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioCutoff(vario_audio_cutoff);

        }

        if (key.equals("sink_audio_basehz")) {
            int sink_audio_basehz = Integer.valueOf(sharedPreferences.getString("sink_audio_basehz", "500"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkBase(sink_audio_basehz);
        }
        if (key.equals("sink_audio_incrementhz")) {
            int sink_audio_incrementhz = Integer.valueOf(sharedPreferences.getString("sink_audio_incrementhz", "100"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkIncrement(sink_audio_incrementhz);

        }

        if (key.equals("sink_audio_threshold")) {
            double sink_audio_threshold = Double.valueOf(sharedPreferences.getString("sink_audio_threshold", "-2.0"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioThreshold(sink_audio_threshold);
        }

        if (key.equals("sink_audio_cutoff")) {
            double sink_audio_cutoff = Double.valueOf(sharedPreferences.getString("sink_audio_cutoff", "-1.5"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioCutoff(sink_audio_cutoff);
        }

        if (key.equals("audio_enabled")) {
            boolean audio_enabled = sharedPreferences.getBoolean("audio_enabled", true);

            BlueFlyVario.blueFlyVario.setBeeps(audio_enabled);


        }

        if (key.equals("location_askEnableGPS")) {
            BlueFlyVario.blueFlyVario.getVarioService().getBfvLocationManager().maybeAskEnableGPS();

        }

        if (key.equals("layout_enabled")) {
            boolean layout_enabled = sharedPreferences.getBoolean("layout_enabled", true);
            if (!layout_enabled) {
                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                prefsEditor.putBoolean("layout_drag_enabled", false);
                prefsEditor.commit();

            }
            varioSurface.readSettings();
        }
        if (key.equals("layout_parameter_select_radius")) {
            varioSurface.readSettings();

        }
        if (key.equals("layout_drag_enabled")) {
            varioSurface.readSettings();

        }
        if (key.equals("layout_drag_select_radius")) {
            varioSurface.readSettings();

        }
        if (key.equals("layout_draw_touch_points")) {
            varioSurface.readSettings();

        }
        if (key.equals("layout_resize_orientation_change")) {
            varioSurface.readSettings();

        }
        if (key.equals("layout_resize_import")) {
            varioSurface.readSettings();

        }


    }


}
