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

import java.util.ArrayList;

public class PiecewiseLinearFunction {
    private ArrayList<Point2d> points;
    private double posInfValue;
    private double negInfValue;


    /**
     * Creates a piecewise linear function from an ArrayList of
     * javax.Vecmath.Point2d objects. Throws a runtime exception if the
     * arraylist contains other than Point2d objects. Adds points to the list of
     * points using the addNewPoint(Point2d) method.
     */

    public PiecewiseLinearFunction(ArrayList<Point2d> point2ds) {
        points = new ArrayList<Point2d>();
        posInfValue = Double.NaN;
        negInfValue = Double.NaN;
        for (int i = 0; i < point2ds.size(); i++) {
            Point2d point;
            try {
                point = point2ds.get(i);
            } catch (ClassCastException e) {

                System.out.println("Must use only javax.Vecmath.Point2d objects");
                e.printStackTrace();
                throw new RuntimeException();
            }
            this.addNewPoint(point);
        }
        if (points.size() < 1) {
            System.out.println("Must have at least one valid point");

            throw new RuntimeException();
        }
    }

    /**
     * Creates a piecewiseLinearFunction with only one point
     * This would normally be added to later with the addNewPoint method
     */

    public PiecewiseLinearFunction(Point2d point) {
        points = new ArrayList<Point2d>();
        posInfValue = Double.NaN;
        negInfValue = Double.NaN;
        this.addNewPoint(point);
        if (points.size() < 1) {
            System.out.println("Must have at least one valid point");
            throw new RuntimeException();
        }
    }

    /**
     * Point2d objects with a x value of Double.NaN are ignored.
     * Those with an x value equal to Double.PositiveInfinity or
     * Double.NegativeInfinity are used to set the value of
     * the posInfValue and negInfValue fields respectively.
     * If the values are not specified then posInfValue and negInfValue
     * are set to Double.NaN.
     * If the y value == Double.NaN, PositiveInfinity or Negative Infinity
     * the point will not be added.
     */

    public void addNewPoint(Point2d point) {
        if (point.x == Double.NaN || point.y == Double.NaN ||
                point.y == Double.POSITIVE_INFINITY || point.y == Double.NEGATIVE_INFINITY) {
            return;
        } else if (point.x == Double.POSITIVE_INFINITY) {
            posInfValue = point.y;
        } else if (point.x == Double.NEGATIVE_INFINITY) {
            negInfValue = point.y;
        } else if (points.size() == 0) {
            points.add(point);
            return;
        } else if (point.x > (points.get(points.size() - 1)).x) {
            points.add(point);
        } else {
            for (int i = 0; i < points.size(); i++) {
                if ((points.get(i)).x > point.x) {
                    points.add(i, point);
                    return;
                }
            }
        }
    }

    /**
     * Calls addNewPoint(Point2d) provided point is not null and is of length two. Returns true if successful
     */

    public boolean addNewPoint(double[] point) {
        if (point != null && point.length == 2) {

            this.addNewPoint(new Point2d(point[0], point[1]));

            return true;


        }

        return false;
    }

    /**
     * Returns the linear interpolation between two points. If s is
     * less than the first point, or the greater than the last point then that
     * that points y value is returned. If x  == pos or neg Infinity then the value
     * of those fields are returned.
     */

    public double getValue(double x) {
        if (x == Double.POSITIVE_INFINITY) {
            return posInfValue;
        } else if (x == Double.NEGATIVE_INFINITY) {
            return negInfValue;
        } else if (points.size() == 1) {
            return (points.get(0)).y;
        } else {
            Point2d point;
            Point2d lastPoint = points.get(0);
            if (x <= lastPoint.x) {
                return lastPoint.y;
            }
            for (int i = 1; i < points.size(); i++) {
                point = points.get(i);
                if (x <= point.x) {
                    double ratio = (x - lastPoint.x) / (point.x - lastPoint.x);
                    return lastPoint.y + ratio * (point.y - lastPoint.y);
                }
                lastPoint = point;
            }
            return lastPoint.y;
        }
    }

    public String toString() {
        String returnValue = new String();
        if (!Double.isNaN(negInfValue)) {
            returnValue = returnValue + ("(negInfinity, " + negInfValue + ")\n");
        }
        for (int i = 0; i < points.size(); i++) {
            Point2d point = points.get(i);
            returnValue = returnValue + (point + "\n");
        }
        if (!Double.isNaN(posInfValue)) {
            returnValue = returnValue + ("(posInfinity, " + posInfValue + ")\n");
        }
        return returnValue;
    }


}




