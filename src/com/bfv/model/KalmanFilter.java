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

/**
 * This class is developed from code released in the public domain here:
 * https://code.google.com/p/pressure-altimeter/
 */
public class KalmanFilter {
    // The state we are tracking, namely:
    private double x_abs;  // The absolute value of x.
    private double x_vel;  // The rate of change of x.

    // Covariance matrix for the state.
    private double p_abs_abs;
    private double p_abs_vel;
    private double p_vel_vel;

    // The variance of the acceleration noise input in the system model.
    private double var_accel;

    //calculation variables
    double y;
    double s_inv;
    double k_abs;
    double k_vel;

    // Constructor. Assumes a variance of 1.0 for the system model's
    // acceleration noise input, in units per second squared.
    public KalmanFilter() {
        setAccelerationVariance(1.0);
        reset();
    }

    // Constructor. Caller supplies the variance for the system model's
    // acceleration noise input, in units per second squared.
    public KalmanFilter(double var_accel) {
        setAccelerationVariance(var_accel);
        reset();
    }

    // The following three methods reset the filter. All of them assign a huge
    // variance to the tracked absolute quantity and a var_accel variance to
    // its derivative, so the very next measurement will essentially be
    // copied directly into the filter. Still, we provide methods that allow
    // you to specify initial settings for the filter's tracked state.

    public void reset() {
        reset(0.0, 0.0);
    }

    public void reset(double abs_value) {
        reset(abs_value, 0.0);
    }

    public void reset(double abs_value, double vel_value) {
        x_abs = abs_value;
        x_vel = vel_value;
        p_abs_abs = 1.0e10;
        p_abs_vel = 0.0;
        p_vel_vel = var_accel;
    }

    // Sets the variance for the acceleration noise input in the system model,
    // in units per second squared.
    public void setAccelerationVariance(double var_accel) {
        this.var_accel = var_accel;
    }

    // Updates state given a sensor measurement of the absolute value of x,
    // the variance of that measurement, and the interval since the last
    // measurement in seconds. This interval must be greater than 0; for the
    // first measurement after a reset(), it's safe to use 1.0.
    public void update(double z_abs, double var_z_abs, double dt) {
        // Note: math is not optimized by hand. Let the compiler sort it out.
        // Predict step.
        // Update state estimate.
        x_abs += x_vel * dt;
        // Update state covariance. The last term mixes in acceleration noise.
        p_abs_abs += 2.0 * dt * p_abs_vel + dt * dt * p_vel_vel + var_accel * dt * dt * dt * dt / 4.0;
        p_abs_vel += dt * p_vel_vel + var_accel * dt * dt * dt / 2.0;
        p_vel_vel += var_accel * dt * dt;

        // Update step.
        y = z_abs - x_abs;  // Innovation.
        s_inv = 1. / (p_abs_abs + var_z_abs);  // Innovation precision.
        k_abs = p_abs_abs * s_inv;  // Kalman gain
        k_vel = p_abs_vel * s_inv;
        // Update state estimate.
        x_abs += k_abs * y;
        x_vel += k_vel * y;
        // Update state covariance.
        p_vel_vel -= p_abs_vel * k_vel;
        p_abs_vel -= p_abs_vel * k_abs;
        p_abs_abs -= p_abs_abs * k_abs;
    }

    // Getters for the state and its covariance.
    public double getXAbs() {
        return x_abs;
    }

    public double getXVel() {
        return x_vel;
    }

    public double getCovAbsAbs() {
        return p_abs_abs;
    }

    public double getCovAbsVel() {
        return p_abs_vel;
    }

    public double getCovVelVel() {
        return p_vel_vel;
    }
}