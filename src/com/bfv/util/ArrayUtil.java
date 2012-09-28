package com.bfv.util;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 11/06/12
 * Time: 8:11 PM
 */
public class ArrayUtil {
    public static double[] getMaxValue(double[] val) {
        double max = Double.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < val.length; i++) {
            double v = val[i];
            if (v > max) {
                max = v;
                index = i;
            }
        }
        return new double[]{max, index};

    }

    public static double[] getMinValue(double[] val) {
        double min = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < val.length; i++) {
            double v = val[i];
            if (v < min) {
                min = v;
                index = i;
            }
        }
        return new double[]{min, index};

    }

    public static double[] copy(double[] dataToCopy) {
        if (dataToCopy == null) {
            return null;
        }

        double[] ret = new double[dataToCopy.length];
        System.arraycopy(dataToCopy, 0, ret, 0, dataToCopy.length);

        return ret;
    }
}
