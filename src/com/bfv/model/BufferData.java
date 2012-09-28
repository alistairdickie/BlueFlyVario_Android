package com.bfv.model;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 16/06/12
 * Time: 10:53 AM
 */
public class BufferData {
    private int position;
    private double[] data;

    public BufferData(int position, double[] data) {
        this.position = position;
        this.data = data;
    }

    public int getPosition() {
        return position;
    }

    public double[] getData() {
        return data;
    }

    public void update(int position, double[] dataToCopy) {
        this.position = position;
        for (int i = 0; i < data.length; i++) {
            data[i] = dataToCopy[i];

        }
    }

}
