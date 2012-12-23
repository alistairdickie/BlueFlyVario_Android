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

package com.bfv.util;

public class RegressionSlope {


    /**
     * sum of x values
     */
    private double sumX = 0d;

    /**
     * total variation in x (sum of squared deviations from xbar)
     */
    private double sumXX = 0d;

    /**
     * sum of y values
     */
    private double sumY = 0d;

    /**
     * sum of products
     */
    private double sumXY = 0d;

    /**
     * number of observations
     */
    private long n = 0;


    public void addData(double x, double y) {

        sumXX += x * x;
        sumXY += x * y;
        sumX += x;
        sumY += y;
        n++;


    }

    public void removeData(double x, double y) {
        if (n > 0) {

            sumXX -= x * x;
            sumXY -= x * y;
            sumX -= x;
            sumY -= y;
            n--;


        }
    }


    public void clear() {
        sumX = 0d;
        sumXX = 0d;
        sumY = 0d;

        sumXY = 0d;
        n = 0;
    }


    public long getN() {
        return n;
    }


    public double getSlope() {
        if (n < 2) {
            return Double.NaN; //not enough data
        }
        if (Math.abs(sumXX) < 10 * Double.MIN_VALUE) {
            return Double.NaN; //not enough variation in x
        }
        double top = n * sumXY - sumX * sumY;
        double bottom = n * sumXX - sumX * sumX;
        return top / bottom;
    }


}