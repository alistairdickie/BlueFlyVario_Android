package com.bfv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.bfv.model.Vario;
import com.bfv.view.VarioSurfaceView;

import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 10/06/12
 * Time: 10:54 AM
 */
public class BFVSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static SharedPreferences sharedPrefs;


    public static boolean stopSetQNH = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.e("BFV", "CREATE Settings");
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);


        addPreferencesFromResource(R.xml.preferences);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.findPreference("default").setOnPreferenceClickListener(this);
        this.findPreference("about").setOnPreferenceClickListener(this);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void resetDefaults() {


        BlueFlyVario.blueFlyVario.getVarioService().getBfvLocationManager().setGpsAltUpdateFlag(false);
        BlueFlyVario.blueFlyVario.setSetAltFlag(false);

        //sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        stopSetQNH = true;
        sharedPrefs.edit().clear().commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        //addPreferencesFromResource(R.xml.preferences);

        Toast toast = Toast.makeText(this, "Default Settings Restored", Toast.LENGTH_SHORT);
        toast.show();

        //sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        //this.findPreference("default").setOnPreferenceClickListener(this);


        BlueFlyVario bfv = BlueFlyVario.blueFlyVario;

        double alt_setqnh = Double.valueOf(sharedPrefs.getString("alt_setqnh", "1013.25")) * 100.0;
        bfv.getAlt().setSeaLevelPressure(alt_setqnh);

        double alt_damp = Double.valueOf(sharedPrefs.getString("alt_damp", "0.05"));
        bfv.getAlt().setAltDamp(alt_damp);

        double var1_damp = Double.valueOf(sharedPrefs.getString("var1_damp", "0.05"));
        bfv.getVario().setDamp(var1_damp);

        int var1_window = Integer.valueOf(sharedPrefs.getString("var1_window", "75"));
        bfv.getVario().setWindowSize(var1_window);

        int var1_useAltType = Integer.valueOf(sharedPrefs.getString("var1_useAltType", Vario.VAR_USE_RAW_ALT + ""));
        bfv.getVario().setVarUseAltType(var1_useAltType);

        double var2_damp = Double.valueOf(sharedPrefs.getString("var2_damp", "1.0"));
        bfv.getVario2().setDamp(var2_damp);

        int var2_window = Integer.valueOf(sharedPrefs.getString("var2_window", "200"));
        bfv.getVario2().setWindowSize(var2_window);

        int var2_useAltType = Integer.valueOf(sharedPrefs.getString("var2_useAltType", "" + Vario.VAR_USE_DAMP_ALT));
        bfv.getVario2().setVarUseAltType(var2_useAltType);

        boolean audio_enabled = sharedPrefs.getBoolean("audio_enabled", false);
        BlueFlyVario.blueFlyVario.setBeeps(audio_enabled);


        int audio_basehz = Integer.valueOf(sharedPrefs.getString("audio_basehz", "700"));
        BlueFlyVario.blueFlyVario.getBeeps().setBase(audio_basehz);


        int audio_incrementhz = Integer.valueOf(sharedPrefs.getString("audio_incrementhz", "100"));
        BlueFlyVario.blueFlyVario.getBeeps().setIncrement(audio_incrementhz);


        double vario_audio_threshold = Double.valueOf(sharedPrefs.getString("vario_audio_threshold", "0.2"));
        BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioThreshold(vario_audio_threshold);

        double vario_audio_cutoff = Double.valueOf(sharedPrefs.getString("vario_audio_cutoff", "0.05"));
        BlueFlyVario.blueFlyVario.getBeeps().setVarioAudioCutoff(vario_audio_cutoff);


        double sink_audio_threshold = Double.valueOf(sharedPrefs.getString("sink_audio_threshold", "-2.0"));
        BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioThreshold(sink_audio_threshold);


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
                            resetDefaults();

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

            String version = "";
            int versionCode = -1;
            try {
                PackageInfo pInfo = BlueFlyVario.blueFlyVario.getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
                versionCode = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {

            }


            builder.setMessage("BlueFlyVario " + version + "  [" + versionCode + "]")
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {


                            finish();
                        }


                    });
            AlertDialog alert = builder.create();
            alert.show();

            return true;

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
                    DecimalFormat df = new DecimalFormat("0.00");
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
                if (varioService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
                    if (varioService.getBfvLocationManager() != null) {
                        varioService.getBfvLocationManager().setGpsAltUpdateFlag(true);
                    }

                } else {
                    BlueFlyVario.blueFlyVario.setSetAltFlag(true);
                }
            } else {
                if (varioService.getState() == BFVService.STATE_CONNECTEDANDPRESSURE) {
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

        if (key.equals("var1_damp")) {
            double var1_damp = Double.valueOf(sharedPreferences.getString("var1_damp", "0.05"));
            BlueFlyVario.blueFlyVario.getVario().setDamp(var1_damp);
        }

        if (key.equals("var1_window")) {
            int var1_window = Integer.valueOf(sharedPreferences.getString("var1_window", "75"));
            BlueFlyVario.blueFlyVario.getVario().setWindowSize(var1_window);
        }

        if (key.equals("var1_useAltType")) {
            int var1_useAltType = Integer.valueOf(sharedPreferences.getString("var1_useAltType", Vario.VAR_USE_RAW_ALT + ""));
            BlueFlyVario.blueFlyVario.getVario().setVarUseAltType(var1_useAltType);
        }

        if (key.equals("var2_damp")) {
            double var2_damp = Double.valueOf(sharedPreferences.getString("var2_damp", "1.0"));
            BlueFlyVario.blueFlyVario.getVario2().setDamp(var2_damp);
        }

        if (key.equals("var2_window")) {
            int var2_window = Integer.valueOf(sharedPreferences.getString("var2_window", "200"));
            BlueFlyVario.blueFlyVario.getVario2().setWindowSize(var2_window);
        }

        if (key.equals("var2_useAltType")) {
            int var2_useAltType = Integer.valueOf(sharedPreferences.getString("var2_useAltType", "" + Vario.VAR_USE_DAMP_ALT));
            BlueFlyVario.blueFlyVario.getVario2().setVarUseAltType(var2_useAltType);
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
            int audio_basehz = Integer.valueOf(sharedPreferences.getString("audio_basehz", "700"));
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
        if (key.equals("sink_audio_threshold")) {
            double sink_audio_threshold = Double.valueOf(sharedPreferences.getString("sink_audio_threshold", "-2.0"));
            BlueFlyVario.blueFlyVario.getBeeps().setSinkAudioThreshold(sink_audio_threshold);
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
