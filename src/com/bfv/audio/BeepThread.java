package com.bfv.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import com.bfv.BFVSettings;
import com.bfv.R;
import com.bfv.VarioChangeListener;
import com.bfv.model.Vario;
import com.bfv.util.PiecewiseLinearFunction;
import com.bfv.util.Point2d;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 14/06/12
 * Time: 9:47 PM
 */
public class BeepThread implements Runnable, VarioChangeListener, SoundPool.OnLoadCompleteListener {
    private Vario vario;
    private boolean running;
    private double minVar;

    private double base = 700.0;
    private double increment = 100.0;

    private double var;
    private double cadence = 0.5;

    private SoundPool soundPool;

    private int tone_1000;
    private Thread thread;

    private PiecewiseLinearFunction cadenceFunction;


    public BeepThread(Context context, Vario vario, double minVar) {

        this.vario = vario;

        this.minVar = minVar;

        SharedPreferences sharedPrefs = BFVSettings.sharedPrefs;
        base = Integer.valueOf(sharedPrefs.getString("audio_basehz", "700"));
        increment = Integer.valueOf(sharedPrefs.getString("audio_incrementhz", "100"));

        cadenceFunction = new PiecewiseLinearFunction(new Point2d(0, 0.4763));
        cadenceFunction.addNewPoint(new Point2d(0.135, 0.4755));
        cadenceFunction.addNewPoint(new Point2d(0.441, 0.3619));
        cadenceFunction.addNewPoint(new Point2d(1.029, 0.2238));
        cadenceFunction.addNewPoint(new Point2d(1.559, 0.1565));
        cadenceFunction.addNewPoint(new Point2d(2.471, 0.0985));
        cadenceFunction.addNewPoint(new Point2d(3.571, 0.0741));

        running = true;
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(this);
        tone_1000 = soundPool.load(context, R.raw.tone_1000mhz, 1);


    }

    public void run() {
        soundPool.play(tone_1000, 1.0f, 1.0f, 0, -1, 0.7f);//need to set rate to something other than 1.0f to start with for Android 4.1 based Nexus 7. Perhaps a bug?

        while (running) {


            try {
                Thread.sleep((int) (cadence * 1000));
            } catch (InterruptedException e) {

            }

            soundPool.setVolume(tone_1000, 0.0f, 0.0f);

            try {
                Thread.sleep((int) (cadence * 1000));
            } catch (InterruptedException e) {

            }
            if (var > minVar) {
                soundPool.setVolume(tone_1000, 1.0f, 1.0f);

            }
        }

        vario.removeChangeListener(this);

        soundPool.stop(tone_1000);
        soundPool.release();

        soundPool = null;


    }


    public void setRunning(boolean running) {

        this.running = running;
    }


    public synchronized void varioChanged(double newVar) {
        var = newVar;
        if (soundPool != null) {
            double rate = getRateFromTone1000(var);
            // Log.i("BFV", "Rate" + rate);

            soundPool.setRate(tone_1000, getRateFromTone1000(var));
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
        vario.addChangeListener(this);
        thread = new Thread(this);

        thread.start();

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
