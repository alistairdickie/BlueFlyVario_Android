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

import android.graphics.RectF;

public class DistanceUtil {
    //Compute the dot product AB . AC
    public static double dotProduct(Point2d pointA, Point2d pointB, Point2d pointC) {
        Point2d AB = new Point2d();
        Point2d BC = new Point2d();
        AB.x = pointB.x - pointA.x;
        AB.y = pointB.y - pointA.y;
        BC.x = pointC.x - pointB.x;
        BC.y = pointC.y - pointB.y;
        return AB.x * BC.x + AB.y * BC.y;


    }

    //Compute the cross product AB x AC
    public static double crossProduct(Point2d pointA, Point2d pointB, Point2d pointC) {
        Point2d AB = new Point2d();
        Point2d AC = new Point2d();
        AB.x = pointB.x - pointA.x;
        AB.y = pointB.y - pointA.y;
        AC.x = pointC.x - pointA.x;
        AC.y = pointC.y - pointA.y;
        return AB.x * AC.y - AB.y * AC.x;


    }

    //Compute the distance from A to B
    public static double distance(Point2d pointA, Point2d pointB) {
        double d1 = pointA.x - pointB.x;
        double d2 = pointA.y - pointB.y;

        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    //Compute the distance from uv to p
    //if isSegment is true, uv is a segment, not a line.
    public static double pointLineDistance(Point2d u, Point2d v, Point2d p, boolean isSegment) {
        double dist = crossProduct(u, v, p) / distance(u, v);
        if (isSegment) {
            double dot1 = dotProduct(u, v, p);
            if (dot1 > 0)
                return distance(v, p);

            double dot2 = dotProduct(v, u, p);
            if (dot2 > 0)
                return distance(u, p);
        }
        return Math.abs(dist);
    }

    public static double pointLineDistance(Line2d line, Point2d p, boolean isSegment) {
        return pointLineDistance(line.u, line.v, p, isSegment);
    }


    public static double pointRectFDistance(Point2d p, RectF rect) {
        double min = Double.MAX_VALUE;
        Line2d[] lines = getLines(rect);
        for (int i = 0; i < lines.length; i++) {
            Line2d line = lines[i];
            double dist = pointLineDistance(line, p, true);
            if (dist < min) {
                min = dist;
            }
        }

        return min;


    }

    public static Line2d[] getLines(RectF rect) {
        Line2d[] lines = new Line2d[4];
        lines[0] = new Line2d(new Point2d(rect.left, rect.top), new Point2d(rect.right, rect.top));
        lines[1] = new Line2d(new Point2d(rect.right, rect.top), new Point2d(rect.right, rect.bottom));
        lines[2] = new Line2d(new Point2d(rect.right, rect.bottom), new Point2d(rect.left, rect.bottom));
        lines[3] = new Line2d(new Point2d(rect.left, rect.bottom), new Point2d(rect.left, rect.top));
        return lines;

    }
}
