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