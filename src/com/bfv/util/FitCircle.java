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

public class FitCircle {

    public static double[] taubinNewton(double[][] points) {
        int nPoints = points.length;
        if (nPoints < 3)
            throw new IllegalArgumentException("Too few points");
        double[] centroid = Centroid.getCentroid(points);
        double Mxx = 0, Myy = 0, Mxy = 0, Mxz = 0, Myz = 0, Mzz = 0;
        for (int i = 0; i < nPoints; i++) {
            double Xi = points[i][0] - centroid[0];
            double Yi = points[i][1] - centroid[1];
            double Zi = Xi * Xi + Yi * Yi;
            Mxy += Xi * Yi;
            Mxx += Xi * Xi;
            Myy += Yi * Yi;
            Mxz += Xi * Zi;
            Myz += Yi * Zi;
            Mzz += Zi * Zi;

        }
        Mxx /= nPoints;
        Myy /= nPoints;
        Mxy /= nPoints;
        Mxz /= nPoints;
        Myz /= nPoints;
        Mzz /= nPoints;

        double Mz = Mxx + Myy;
        double Cov_xy = Mxx * Myy - Mxy * Mxy;
        double A3 = 4 * Mz;
        double A2 = -3 * Mz * Mz - Mzz;
        double A1 = Mzz * Mz + 4 * Cov_xy * Mz - Mxz * Mxz - Myz * Myz - Mz
                * Mz * Mz;
        double A0 = Mxz * Mxz * Myy + Myz * Myz * Mxx - Mzz * Cov_xy - 2 * Mxz
                * Myz * Mxy + Mz * Mz * Cov_xy;
        double A22 = A2 + A2;
        double A33 = A3 + A3 + A3;

        double xnew = 0;
        double ynew = 1e+20;
        double epsilon = 1e-12;
        double iterMax = 20;

        for (int iter = 0; iter < iterMax; iter++) {
            double yold = ynew;
            ynew = A0 + xnew * (A1 + xnew * (A2 + xnew * A3));
            if (Math.abs(ynew) > Math.abs(yold)) {
                System.out
                        .println("Newton-Taubin goes wrong direction: |ynew| > |yold|");
                xnew = 0;
                break;
            }
            double Dy = A1 + xnew * (A22 + xnew * A33);
            double xold = xnew;
            xnew = xold - ynew / Dy;
            if (Math.abs((xnew - xold) / xnew) < epsilon) {

                break;
            }
            if (iter >= iterMax) {
                System.out.println("Newton-Taubin will not converge");
                xnew = 0;
            }
            if (xnew < 0.) {
                System.out.println("Newton-Taubin negative root: x = " + xnew);
                xnew = 0;
            }
        }
        double[] centreRadius = new double[3];
        double det = xnew * xnew - xnew * Mz + Cov_xy;
        double x = (Mxz * (Myy - xnew) - Myz * Mxy) / (det * 2);
        double y = (Myz * (Mxx - xnew) - Mxz * Mxy) / (det * 2);
        centreRadius[0] = x + centroid[0];
        centreRadius[1] = y + centroid[1];
        centreRadius[2] = Math.sqrt(x * x + y * y + Mz);

        return centreRadius;
    }

    /**
     * Calculate the mean squared errors between the fit circle and the
     * coordinates
     *
     * @param points
     * @param abR
     * @return double[] containing mean squared errors in x, y, R and sum of (x,
     *         y, R)
     */
    public static double[] getErrors(double[][] points, double[] abR) {
        int nPoints = points.length;

        double a = abR[0];
        double b = abR[1];
        double R = abR[2];
        double sumX2 = 0;
        double sumY2 = 0;
        double sumR2 = 0;

        for (int i = 0; i < nPoints; i++) {
            double x = points[i][0];
            double y = points[i][1];
            double r = Math.sqrt((x - a) * (x - a) + (y - b) * (y - b));
            double theta = Math.atan2((y - b), (x - a));
            double xt = R * Math.cos(theta) + a;
            double yt = R * Math.sin(theta) + b;

            sumX2 += (x - xt) * (x - xt);
            sumY2 += (y - yt) * (y - yt);
            sumR2 += (R - r) * (R - r);
        }
        double[] errors = new double[4];
        errors[0] = sumX2 / nPoints;
        errors[1] = sumY2 / nPoints;
        errors[2] = sumR2 / nPoints;
        errors[3] = errors[0] + errors[1] + errors[2];
        return errors;
    }
}
