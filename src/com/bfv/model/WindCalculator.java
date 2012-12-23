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

package com.bfv.model;

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
