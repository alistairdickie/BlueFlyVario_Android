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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Beep implements Runnable {
    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private double duration; // seconds

    private double freqOfTone; // hz

    private int sampleRate = 8000;
    private int numSamples;
    private double[] sample;

    private byte[] generatedSnd = new byte[2 * numSamples];

    private Thread thread;

    public Beep(double duration, double freq) {

        this.duration = duration;

        this.freqOfTone = freq;
        numSamples = (int) (duration * sampleRate * 2);
        if (numSamples % 2 == 1) {
            numSamples += 1;
        }
        int buf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (numSamples < buf) {
            numSamples = buf;
        }
        sample = new double[numSamples];
        generatedSnd = new byte[2 * numSamples];

        thread = new Thread(this);
        thread.start();
    }

    public void waitUntilFinish() {
        try {
            thread.join();
        } catch (InterruptedException e) {

        }
    }

    public void run() {
        genTone();
        playSound();
    }

    public void genTone() {
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            if (i < numSamples / 2.0) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
            } else {
                sample[i] = 0;
            }

        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    private void playSound() {

//        Log.i("BFV_samples","" + numSamples ) ;
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
        audioTrack.flush();
        audioTrack.release();

    }
}

