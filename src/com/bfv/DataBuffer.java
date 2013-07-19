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

import com.bfv.model.BufferData;
import com.bfv.util.ArrayUtil;

import java.util.ArrayList;

public class DataBuffer {
    private ArrayList<DataSource> dataSources;
    private ArrayList<BufferData> data;
    private int bufferSize;
    private int position;
    private boolean full;
    private boolean empty;
    private int sampleRate;
    private int sampleRateCounter;


    public DataBuffer(ArrayList<DataSource> dataSources, int bufferSize, int sampleRate) {
        this.dataSources = dataSources;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        initBuffer();

    }

    public synchronized void initBuffer() {
        data = new ArrayList<BufferData>();
        for (int i = 0; i < dataSources.size(); i++) {
            data.add(new BufferData(bufferSize));

        }
        empty = true;
        position = 0;
        sampleRateCounter = sampleRate;  //we are going to take the first data point we get.


    }


    public synchronized void addData() {
        if (sampleRateCounter < sampleRate) {
            sampleRateCounter++;
            return;
        }
        sampleRateCounter = 1;
        position++;
        if (position >= bufferSize) {
            position = 0;
            full = true;
        }

        for (int i = 0; i < dataSources.size(); i++) {
            if (empty) {
                for (int pos = 0; pos < bufferSize; pos++) {
                    data.get(i).getData()[pos] = dataSources.get(i).getValue();
                }

            }
            data.get(i).getData()[position] = dataSources.get(i).getValue();
            data.get(i).setPosition(position);

        }
        if (empty) {
            empty = false;
        }

    }

    public synchronized BufferData getData(DataSource dataSource) {

        if (dataSources.contains(dataSource)) {
            return data.get(dataSources.indexOf(dataSource));

        }
        return null;
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
