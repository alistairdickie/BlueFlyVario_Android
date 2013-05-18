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
import com.bfv.model.KalmanFilteredVario;
import com.bfv.model.Vario;
import com.bfv.util.PiecewiseLinearFunction;
import com.bfv.util.Point2d;

public class BeepThread implements Runnable, VarioChangeListener, SoundPool.OnLoadCompleteListener {

    private KalmanFilteredVario vario;
    private boolean running;
    private double varioAudioThreshold;
    private double varioAudioCutoff;
    private double sinkAudioThreshold;
    private double sinkAudioCutoff;


    private double base = 1000.0;
    private double increment = 100.0;

    private double sinkBase = 500.0;
    private double sinkIncrement = 100.0;

    private double var;

    private SoundPool soundPool;

    private int numSounds = 2;
    private int soundsLoaded = 0;

    private int tone_1000;
    private int sink;

    private int tone_1000_stream;
    private int sink_stream;


    private Thread thread;

    private PiecewiseLinearFunction cadenceFunction;

    private boolean beepOn;
    private boolean sinkOn;
    private boolean silentPause;

    private boolean sinking;
    private boolean beeping;


    public BeepThread(Context context, KalmanFilteredVario vario) {

        this.vario = vario;

        SharedPreferences sharedPrefs = BFVSettings.sharedPrefs;
        base = Integer.valueOf(sharedPrefs.getString("audio_basehz", "1000"));
        increment = Integer.valueOf(sharedPrefs.getString("audio_incrementhz", "100"));
        varioAudioCutoff = Double.valueOf(sharedPrefs.getString("vario_audio_cutoff", "0.05"));
        varioAudioThreshold = Double.valueOf(sharedPrefs.getString("vario_audio_threshold", "0.2"));
        sinkAudioThreshold = Double.valueOf(sharedPrefs.getString("sink_audio_threshold", "-2.0"));
        sinkAudioCutoff = Double.valueOf(sharedPrefs.getString("sink_audio_cutoff", "-1.5"));
        sinkBase = Integer.valueOf(sharedPrefs.getString("sink_audio_basehz", "500"));
        sinkIncrement = Integer.valueOf(sharedPrefs.getString("sink_audio_incrementhz", "100"));


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
        sink = soundPool.load(context, R.raw.sink_tone500mhz, 1);


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

    public void setSinkAudioCutoff(double sinkAudioCutoff) {
        this.sinkAudioCutoff = sinkAudioCutoff;
    }

    public void setSinkBase(double sinkBase) {
        this.sinkBase = sinkBase;
    }

    public void setSinkIncrement(double sinkIncrement) {
        this.sinkIncrement = sinkIncrement;
    }

    public void run() {


        while (running) {

            try {
                while (beepOn) {

                    tone_1000_stream = soundPool.play(tone_1000, 1.0f, 1.0f, 0, -1, getRateFromTone1000(var));//need to set rate to something other than 1.0f to start with for Android 4.1 based Nexus 7. Perhaps a bug?
                    Thread.sleep((int) (cadenceFunction.getValue(var) * 1000));
                    soundPool.setVolume(tone_1000_stream, 0.0f, 0.0f);
                    Thread.sleep((int) (cadenceFunction.getValue(var) * 1000));
                }
            } catch (InterruptedException e) {
                soundPool.stop(tone_1000_stream);
            }


            try {
                while (sinkOn) {

                    sink_stream = soundPool.play(sink, 1.0f, 1.0f, 0, -1, getRateFromTone500(var));//need to set rate to something other than 1.0f to start with for Android 4.1 based Nexus 7. Perhaps a bug?

                    Thread.sleep(Long.MAX_VALUE);

                }


            } catch (InterruptedException e) {
                soundPool.stop(sink_stream);

            }

            try {

                while (silentPause) {

                    Thread.sleep(Long.MAX_VALUE);
                }


            } catch (InterruptedException e) {

            }


        }

        vario.removeChangeListener(this);

        soundPool.stop(tone_1000_stream);
        soundPool.stop(sink_stream);
        soundPool.release();

        soundPool = null;


    }


    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (beepOn || sinkOn || silentPause) {
            thread.interrupt();
        }
    }


    public synchronized void varioChanged(double newVar) {
        var = newVar;
        if (soundPool != null) {

            if (var >= varioAudioThreshold && !beeping && !sinking) {//need to start beeping
                beepOn = true;
                sinkOn = false;
                silentPause = false;
                beeping = true;
                thread.interrupt();

            } else if (var < varioAudioCutoff && beeping) {//need to stop beeping
                beepOn = false;
                sinkOn = false;
                silentPause = true;
                beeping = false;
                thread.interrupt();

            } else if (beeping) {//just stay beeping and alter the rate
                soundPool.setRate(tone_1000_stream, getRateFromTone1000(var));

            }

            if (var <= sinkAudioThreshold && !sinking && !beeping) {//need to start sinking
                beepOn = false;
                sinkOn = true;
                silentPause = false;
                sinking = true;
                thread.interrupt();
            } else if (var > sinkAudioCutoff && sinking) {//need to stop sinking
                beepOn = false;
                sinkOn = false;
                silentPause = true;
                sinking = false;
                thread.interrupt();

            } else if (sinking) {
                // just stay sinking and alter the rate;
                soundPool.setRate(sink_stream, getRateFromTone500(var));
            }


        }


    }


    public float getRateFromTone1000(double var) {
        double hZ = base + increment * var;

        float rate = (float) hZ / 1000.0f;
        if (rate < 0.5f) {
            rate = 0.5f;
        } else if (rate > 2.0f) {
            rate = 2.0f;
        } else if (rate == 1.0f) {
            rate = 1.0f + Float.MIN_VALUE;
        }
        return rate;


    }

    public float getRateFromTone500(double var) {
        double hZ = sinkBase + sinkIncrement * var;

        float rate = (float) hZ / 500.0f;
        if (rate < 0.5f) {
            rate = 0.5f;
        } else if (rate > 2.0f) {
            rate = 2.0f;
        } else if (rate == 1.0f) {
            rate = 1.0f + Float.MIN_VALUE;
        }
        return rate;


    }

    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
        soundsLoaded++;
        if (soundsLoaded == numSounds) {

            vario.addChangeListener(this);
            thread = new Thread(this);
            thread.setPriority(Thread.MAX_PRIORITY);
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
