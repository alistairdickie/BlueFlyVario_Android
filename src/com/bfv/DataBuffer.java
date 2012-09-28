package com.bfv;

import com.bfv.model.BufferData;
import com.bfv.util.ArrayUtil;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 11/06/12
 * Time: 5:43 PM
 */
public class DataBuffer {
    private ArrayList<DataSource> dataSources;
    private ArrayList<double[]> data;
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
        data = new ArrayList<double[]>();
        for (int i = 0; i < dataSources.size(); i++) {
            data.add(new double[bufferSize]);

        }
        empty = true;
        position = 0;
        sampleRateCounter = sampleRate;  //we are going to take the first data point we get.


    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        this.initBuffer();
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        this.initBuffer();
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
                    data.get(i)[pos] = dataSources.get(i).getValue();
                }

            }
            data.get(i)[position] = dataSources.get(i).getValue();


        }
        if (empty) {
            empty = false;
        }

    }

    public int indexOf(DataSource source) {
        return dataSources.indexOf(source);
    }

    public synchronized BufferData getData(DataSource dataSource, BufferData bufferData) {

        if (dataSources.contains(dataSource)) {
            double[] dataToCopy = data.get(dataSources.indexOf(dataSource));
            if (bufferData == null) {

                return new BufferData(position, ArrayUtil.copy(dataToCopy));

            } else {
                bufferData.update(position, dataToCopy);
            }

        }
        return bufferData;
    }

    public int getPosition() {
        return position;
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
