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


import com.bfv.DataSource;
import com.bfv.VarioChangeListener;
import com.bfv.util.RegressionSlope;

import java.util.ArrayList;

public class KalmanFilteredVario implements DataSource {


    public static int KALMAN_VARIO = 1;
    public static int DAMPED_VARIO = 2;


    private double varDamp;


    public double var;
    public double dampedVar;

    private int varType;

    public double lastVar;

    private ArrayList<VarioChangeListener> varioChangeListeners;

    public double maxVarSinceLast = -1000.0;
    public double minVarSinceLast = 1000.0;
    public double avgVarSinceLast = 0.0;
    public int countSinceLast = 0;

    public KalmanFilteredAltitude altitude;


    public KalmanFilteredVario(KalmanFilteredAltitude altitude, double varDamp, int varType) {
        this.altitude = altitude;
        this.varDamp = varDamp;
        this.varType = varType;
    }

    public void setVarDamp(double varDamp) {
        this.varDamp = varDamp;
    }


    public void notifyListeners(double changedVar) {
        if (varioChangeListeners == null) {
            return;
        }
        for (int i = 0; i < varioChangeListeners.size(); i++) {
            varioChangeListeners.get(i).varioChanged(changedVar);

        }

    }


    public synchronized double getValue() {
        if (varType == DAMPED_VARIO) {
            return dampedVar;
        }
        return var;
    }


    public synchronized void addChangeListener(VarioChangeListener varioChangeListener) {
        if (varioChangeListeners == null) {
            varioChangeListeners = new ArrayList<VarioChangeListener>();

        }
        varioChangeListeners.add(varioChangeListener);
    }

    public synchronized void removeChangeListener(VarioChangeListener varioChangeListener) {
        if (varioChangeListeners != null && varioChangeListeners.contains(varioChangeListener)) {
            varioChangeListeners.remove(varioChangeListener);

        }
    }

    public synchronized double[] getMinMaxAvgSinceLast() {
        double[] ret = null;
        if (countSinceLast > 0) {
            ret = new double[]{minVarSinceLast, maxVarSinceLast, avgVarSinceLast, countSinceLast};
        } else {
            ret = new double[]{0.0, 0.0, 0.0, 0.0};
        }
        maxVarSinceLast = -1000.0;
        minVarSinceLast = 1000.0;
        avgVarSinceLast = 0.0;
        countSinceLast = 0;
        return ret;
    }


    public synchronized double addData(double time) {


        var = altitude.getVarioValue();
        dampedVar = dampedVar + varDamp * (var - dampedVar);

        if (varType == DAMPED_VARIO) {
            this.notifyListeners(dampedVar);


            lastVar = dampedVar;


            if (dampedVar > maxVarSinceLast) {
                maxVarSinceLast = dampedVar;
            }
            if (dampedVar < minVarSinceLast) {
                minVarSinceLast = dampedVar;
            }
            countSinceLast++;
            avgVarSinceLast += (dampedVar - avgVarSinceLast) / countSinceLast;
            return dampedVar;

        } else {
            this.notifyListeners(var);


            lastVar = var;


            if (var > maxVarSinceLast) {
                maxVarSinceLast = var;
            }
            if (var < minVarSinceLast) {
                minVarSinceLast = var;
            }
            countSinceLast++;
            avgVarSinceLast += (var - avgVarSinceLast) / countSinceLast;
            return var;

        }


    }


}



