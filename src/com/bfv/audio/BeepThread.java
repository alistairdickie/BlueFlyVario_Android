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

package com.bfv.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import com.bfv.BFVSettings;
import com.bfv.R;
import com.bfv.VarioChangeListener;
import com.bfv.model.Vario;
import com.bfv.util.PiecewiseLinearFunction;
import com.bfv.util.Point2d;

public class BeepThread implements Runnable, VarioChangeListener, SoundPool.OnLoadCompleteListener {
    private Vario vario;
    private boolean running;
    private double varioAudioThreshold;
    private double varioAudioCutoff;
    private double sinkAudioThreshold;

    private double base = 700.0;
    private double increment = 100.0;

    private double var;
    private double cadence = 0.5;

    private SoundPool soundPool;

    private int numSounds = 2;
    private int soundsLoaded = 0;

    private int tone_1000;
    private int sink;

    private int tone_1000_stream;
    private int sink_stream;

    private boolean beeping;

    private Thread thread;

    private PiecewiseLinearFunction cadenceFunction;


    public BeepThread(Context context, Vario vario) {

        this.vario = vario;

        SharedPreferences sharedPrefs = BFVSettings.sharedPrefs;
        base = Integer.valueOf(sharedPrefs.getString("audio_basehz", "700"));
        increment = Integer.valueOf(sharedPrefs.getString("audio_incrementhz", "100"));
        varioAudioCutoff = Double.valueOf(sharedPrefs.getString("vario_audio_cutoff", "0.05"));
        varioAudioThreshold = Double.valueOf(sharedPrefs.getString("vario_audio_threshold", "0.2"));
        sinkAudioThreshold = Double.valueOf(sharedPrefs.getString("sink_audio_threshold", "-2.0"));


        cadenceFunction = new PiecewiseLinearFunction(new Point2d(0, 0.4763));
        cadenceFunction.addNewPoint(new Point2d(0.135, 0.4755));
        cadenceFunction.addNewPoint(new Point2d(0.441, 0.3619));
        cadenceFunction.addNewPoint(new Point2d(1.029, 0.2238));
        cadenceFunction.addNewPoint(new Point2d(1.559, 0.1565));
        cadenceFunction.addNewPoint(new Point2d(2.471, 0.0985));
        cadenceFunction.addNewPoint(new Point2d(3.571, 0.0741));

        running = true;
        soundPool = new SoundPool(numSounds, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(this);
        tone_1000 = soundPool.load(context, R.raw.tone_1000mhz, 1);
        sink = soundPool.load(context, R.raw.sink, 1);


    }

    public void setVarioAudioThreshold(double varioAudioThreshold) {
        this.varioAudioThreshold = varioAudioThreshold;
    }

    public void setVarioAudioCutoff(double varioAudioCutoff) {
        this.varioAudioCutoff = varioAudioCutoff;
    }

    public void setSinkAudioThreshold(double sinkAudioThreshold) {
        this.sinkAudioThreshold = sinkAudioThreshold;
    }

    public void run() {
        tone_1000_stream = soundPool.play(tone_1000, 1.0f, 1.0f, 0, -1, 0.7f);//need to set rate to something other than 1.0f to start with for Android 4.1 based Nexus 7. Perhaps a bug?
        sink_stream = soundPool.play(sink, 1.0f, 1.0f, 0, -1, 0.99f);
        soundPool.setVolume(sink_stream, 0.0f, 0.0f);
        beeping = false;

        while (running) {


            try {
                Thread.sleep((int) (cadence * 1000));
            } catch (InterruptedException e) {

            }


            soundPool.setVolume(tone_1000_stream, 0.0f, 0.0f);


            try {
                Thread.sleep((int) (cadence * 1000));
            } catch (InterruptedException e) {

            }
            if (beeping) {

                soundPool.setVolume(tone_1000_stream, 1.0f, 1.0f);


            }
        }

        vario.removeChangeListener(this);

        soundPool.stop(tone_1000_stream);
        soundPool.stop(sink_stream);
        soundPool.release();

        soundPool = null;


    }


    public void setRunning(boolean running) {

        this.running = running;
    }


    public synchronized void varioChanged(double newVar) {
        var = newVar;
        if (soundPool != null) {

            soundPool.setRate(tone_1000_stream, getRateFromTone1000(var));

            if (var >= varioAudioThreshold) {
                beeping = true;
            }
            if (var < varioAudioCutoff) {
                beeping = false;
            }


            if (var < sinkAudioThreshold) {
                soundPool.setVolume(sink_stream, 1.0f, 1.0f);
            } else {
                soundPool.setVolume(sink_stream, 0.0f, 0.0f);
            }
        }


        cadence = cadenceFunction.getValue(var);
    }

    public float getRateFromTone1000(double var) {
        double hZ = base + increment * var;

        float rate = (float) hZ / 1000.0f;
        if (rate < 0.5f) {
            rate = 0.5f;
        } else if (rate > 2.0f) {
            rate = 2.0f;
        }
        return rate;


    }

    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
        soundsLoaded++;
        if (soundsLoaded == numSounds) {

            vario.addChangeListener(this);
            thread = new Thread(this);
            thread.start();
        }


    }

    public synchronized void setBase(double base) {
        this.base = base;

    }

    public synchronized void setIncrement(double increment) {
        this.increment = increment;
    }

    public void onDestroy() {

        this.setRunning(false);
        thread = null;

    }


}
