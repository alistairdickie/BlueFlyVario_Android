package com.bfv.model;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 13/11/12
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class WindCalculator {
    private int sectors;
    private double filterIIR;
    private double cutOffTime;

    private boolean[] hasSectorData;
    private double[][] sectorData;
    private double[] sectorDataAge;

    private double[][] points;

    public WindCalculator(int sectors, double filterIIR, double cutOffTime) {
        setSectorSize(sectors);
        this.filterIIR = filterIIR;
        this.cutOffTime = cutOffTime;

    }

    public void setSectorSize(int sectors) {
        this.sectors = sectors;
        hasSectorData = new boolean[sectors];
        sectorData = new double[sectors][2];
        sectorDataAge = new double[sectors];

    }

    public void addSpeedVector(double bearing, double speed, double time) {
        //purge old measurements
        for (int i = 0; i < sectorDataAge.length; i++) {
            double previousTime = sectorDataAge[i];
            if (time - previousTime > cutOffTime) {
                sectorData[i][0] = 0;
                sectorData[i][1] = 0;
                hasSectorData[i] = false;
            }
        }

        //determineSector

        int sector = (int) Math.floor(bearing / (360.0 / sectors));

        //addDataToSector
        if (hasSectorData[sector]) {
            sectorData[sector][0] = ((filterIIR) * Math.sin(Math.toRadians(bearing)) * speed) + (1.0 - filterIIR) * sectorData[sector][0];
            sectorData[sector][1] = ((filterIIR) * Math.cos(Math.toRadians(bearing)) * speed) + (1.0 - filterIIR) * sectorData[sector][1];

            hasSectorData[sector] = true;
            sectorDataAge[sector] = time;

        } else {
            sectorData[sector][0] = Math.sin(Math.toRadians(bearing)) * speed;
            sectorData[sector][1] = Math.cos(Math.toRadians(bearing)) * speed;
            hasSectorData[sector] = true;
            sectorDataAge[sector] = time;
        }
        calculatePointsForFit();
    }

    public synchronized void calculatePointsForFit() {
        //count points
        int count = 0;
        for (int i = 0; i < hasSectorData.length; i++) {
            if (hasSectorData[i]) {
                count++;
            }

        }
        points = new double[count][2];
        int p = 0;
        for (int i = 0; i < hasSectorData.length; i++) {
            if (hasSectorData[i]) {
                points[p][0] = sectorData[i][0];
                points[p][1] = sectorData[i][1];
                p++;

            }

        }

    }

    public synchronized double[][] getPoints() {
        return points;
    }


}
