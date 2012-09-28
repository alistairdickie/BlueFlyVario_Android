package com.bfv.util;

import android.graphics.PointF;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 21/08/12
 * Time: 5:44 PM
 */
public class Point2d {
    public double x;
    public double y;

    public Point2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point2d(PointF p) {
        this.x = p.x;
        this.y = p.y;
    }

    public Point2d() {

    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
