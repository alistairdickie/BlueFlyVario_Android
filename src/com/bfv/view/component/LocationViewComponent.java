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

package com.bfv.view.component;

import android.graphics.*;
import com.bfv.BFVLocationManager;
import com.bfv.model.LocationAltVar;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.bfv.view.ViewUtil;
import com.bfv.view.component.BFVViewComponent;

import java.util.ArrayList;

public class LocationViewComponent extends BFVViewComponent {

    boolean rotate = true;
    float innerRing = 50.0f;
    float outerRing = 300.0f;
    float wingSize = 10.0f;
    float traceWidth = 5.0f;
    private double displayThreshold; // for all
    private boolean dotsInsteadOfLines = false;
    private boolean driftLocations = false;

    public LocationViewComponent(RectF rect, VarioSurfaceView view) {
        super(rect, view);

    }

    @Override
    public String getParamatizedComponentName() {
        return "Location";
    }

    @Override
    public void addToCanvas(Canvas canvas, Paint paint) {
        super.addToCanvas(canvas, paint);
        BFVLocationManager bfvLocationManager = view.service.getBfvLocationManager();

        float radius = Math.min(rect.height() * 0.90f, rect.width() * 0.90f) / 2.0f;
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f);

        canvas.translate(-rect.left, -rect.top);
        canvas.translate(rect.centerX(), rect.centerY());


        // canvas.drawText(bfvLocationManager.getMaxBounds()+"", -100.0f, -70.0f, paint);
        float pixelScale;

        if (driftLocations) {
            pixelScale = (float) (radius / bfvLocationManager.getMaxDriftedDistance());
        } else {
            pixelScale = (float) (radius / bfvLocationManager.getMaxDistance());
        }


        if (pixelScale > radius / innerRing) {
            pixelScale = (float) radius / innerRing;
        }
        canvas.scale(pixelScale, pixelScale);//meters scale

        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(1.0f / pixelScale);

        if (innerRing < bfvLocationManager.getMaxDistance()) {
            canvas.drawCircle(0.0f, 0.0f, innerRing, paint);
        }
        if (outerRing < bfvLocationManager.getMaxDistance()) {
            canvas.drawCircle(0.0f, 0.0f, outerRing, paint);
        }

        //draw trace
        ArrayList<LocationAltVar> locations = bfvLocationManager.getDisplayableLocations();
        if (locations.size() > 0) {


            if (rotate) {
                canvas.rotate(-locations.get(0).getLocation().getBearing());
            }
            // Log.i("BFV", "wingSize" + locations.wingSize());

            // North Ring
            paint.setColor(Color.RED);
            canvas.scale(1.0f / pixelScale, 1.0f / pixelScale);// pixels scale
            paint.setStrokeWidth(2.0f);
            canvas.drawCircle(0.0f, 0.0f, radius, paint);
            paint.setTextSize(20.0f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            ViewUtil.addTextBubble("N", canvas, paint, 0.0f, -radius, 2.0f, Color.BLACK, Color.RED);
            ViewUtil.addTextBubble("S", canvas, paint, 0.0f, radius, 2.0f, Color.BLACK, Color.RED);
            ViewUtil.addTextBubble("E", canvas, paint, radius, 0.0f, 2.0f, Color.BLACK, Color.RED);
            ViewUtil.addTextBubble("W", canvas, paint, -radius, 0.0f, 2.0f, Color.BLACK, Color.RED);
            paint.setTypeface(Typeface.DEFAULT);


            //locations
            //long currentTime = locations.get(locations.wingSize() - 1).getLocation().getTime();
            canvas.scale(pixelScale, pixelScale);// meters scale
            LocationAltVar maxVarLocation = null;

            LocationAltVar last = locations.get(locations.size() - 1);
            float x;
            float y;
            float xLast;
            float yLast;

            for (int i = locations.size() - 2; i >= 0; i--) {

                LocationAltVar locationAltVar = locations.get(i);
                if (locationAltVar.isMaxVar()) {
                    maxVarLocation = locationAltVar;
                }

                if (driftLocations) {
                    x = locationAltVar.driftedX;
                    y = locationAltVar.driftedY;
                    xLast = last.driftedX;
                    yLast = last.driftedY;
                } else {
                    x = locationAltVar.x;
                    y = locationAltVar.y;
                    xLast = last.x;
                    yLast = last.y;
                }


                paint.setStrokeWidth(traceWidth / pixelScale);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(ViewUtil.getColor(locationAltVar.getVario()));

                //               double varFade = 0.5;   //todo setting
                //               int alpha = Math.abs((int)(locationAltVar.getVario()*255/varFade));
                //               if(alpha > 255){
                //                   alpha = 255;
                //               }
                //               paint.setAlpha(alpha);

                if (locationAltVar.getVario() >= displayThreshold) {
                    if (dotsInsteadOfLines) {

                        canvas.drawCircle(x, -y, paint.getStrokeWidth() / 2.0f, paint);
                    } else {
                        canvas.drawLine(xLast, -yLast, x, -y, paint);
                    }
                }


                last = locationAltVar;


            }

            paint.setAlpha(255);

            //blue dot
            if (maxVarLocation != null && maxVarLocation.getVario() > 0.2f) {//todo - use setting
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.FILL);
                if (driftLocations) {
                    x = maxVarLocation.driftedX;
                    y = maxVarLocation.driftedY;

                } else {
                    x = maxVarLocation.x;
                    y = maxVarLocation.y;

                }
                canvas.drawCircle(x, -y, 5.0f / pixelScale, paint);
            }


            //draw wing
            canvas.rotate(last.getLocation().getBearing());
            paint.setColor(Color.WHITE);
            canvas.scale(1.0f / pixelScale, 1.0f / pixelScale); //back to pixels scale
            Path mark = new Path();

            mark.moveTo(0.0f, -wingSize);
            mark.lineTo(wingSize * 2.0f, wingSize);
            mark.quadTo(0.0f, 0.0f, -wingSize * 2, wingSize);
            mark.close();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);
            canvas.drawPath(mark, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1.0f);
            canvas.drawCircle(0.0f, 0.0f, 2.0f, paint);
        }

        //reset matrix
        canvas.setMatrix(new Matrix());
    }

    @Override
    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();
        parameters.add(new ViewComponentParameter("rotate").setBoolean(rotate));
        parameters.add(new ViewComponentParameter("innerRing").setDouble(innerRing));
        parameters.add(new ViewComponentParameter("outerRing").setDouble(outerRing));
        parameters.add(new ViewComponentParameter("wingSize").setDouble(wingSize));
        parameters.add(new ViewComponentParameter("traceWidth").setDouble(traceWidth));
        parameters.add(new ViewComponentParameter("displayThreshold").setDecimalFormat("0.00").setDouble(displayThreshold));
        parameters.add(new ViewComponentParameter("dotsInsteadOfLines").setBoolean(dotsInsteadOfLines));
        parameters.add(new ViewComponentParameter("driftLocations").setBoolean(driftLocations));
        return parameters;
    }

    @Override
    public void setParameterValue(ViewComponentParameter parameter) {
        super.setParameterValue(parameter);
        String name = parameter.getName();
        if (name.equals("rotate")) {
            rotate = parameter.getBooleanValue();
        } else if (name.equals("innerRing")) {
            innerRing = (int) parameter.getDoubleValue();
        } else if (name.equals("outerRing")) {
            outerRing = (int) parameter.getDoubleValue();
        } else if (name.equals("wingSize")) {
            wingSize = (float) parameter.getDoubleValue();
        } else if (name.equals("traceWidth")) {
            traceWidth = (float) parameter.getDoubleValue();
        } else if (name.equals("displayThreshold")) {
            displayThreshold = parameter.getDoubleValue();
        } else if (name.equals("dotsInsteadOfLines")) {
            dotsInsteadOfLines = parameter.getBooleanValue();
        } else if (name.equals("driftLocations")) {
            driftLocations = parameter.getBooleanValue();
        }
    }
}
