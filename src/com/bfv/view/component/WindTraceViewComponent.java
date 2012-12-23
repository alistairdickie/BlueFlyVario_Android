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
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.bfv.view.component.BFVViewComponent;

import java.util.ArrayList;

public class WindTraceViewComponent extends BFVViewComponent {

    private float scale = 40.0f;
    private boolean manualScale = false;


    public WindTraceViewComponent(RectF rect, VarioSurfaceView view) {
        super(rect, view);

    }

    @Override
    public String getParamatizedComponentName() {
        return "WindTrace";
    }

    @Override
    public void addToCanvas(Canvas canvas, Paint paint) {
        super.addToCanvas(canvas, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f);

        canvas.translate(-rect.left, -rect.top);
        canvas.translate(rect.centerX(), rect.centerY());
        canvas.scale(1.0f, -1.0f);//flip y

        paint.setColor(Color.RED);
        canvas.drawCircle(0, 0, 2, paint);
        canvas.drawLine(-rect.centerX(), 0, rect.centerX(), 0, paint);
        canvas.drawLine(0, -rect.centerY(), 0, rect.centerY(), paint);


        BFVLocationManager bfvLocationManager = view.service.getBfvLocationManager();

        double[][] headingArray = bfvLocationManager.getHeadingArray();
        if (headingArray == null) {
            return;
        }

        if (!manualScale) {
            double maxX = 0.0;
            double maxY = 0.0;
            for (int i = 0; i < headingArray.length; i++) {
                double x = Math.abs(headingArray[i][0]);
                if (x > maxX) {
                    maxX = x;
                }
                double y = Math.abs(headingArray[i][1]);
                if (y > maxY) {
                    maxY = y;
                }

            }
            scale = (float) Math.min(0.8 * rect.width() / 2.0 / maxX, 0.8 * rect.height() / 2.0 / maxY);
            if (scale < 1.0f) {
                scale = 1.0f;
            }
        }

        canvas.drawCircle(0, 0, 10.0f * scale, paint);
        canvas.drawCircle(0, 0, 20.0f * scale, paint);


        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < headingArray.length; i++) {
            canvas.drawCircle((float) (headingArray[i][0] * scale), (float) (headingArray[i][1] * scale), 5, paint);

        }

        double[] heading = bfvLocationManager.getHeading();
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle((float) (heading[0] * scale), (float) (heading[1] * scale), 5, paint);


        double[] wind = bfvLocationManager.getWind();
        double[] errors = bfvLocationManager.getWindError();
        double x = wind[0] * scale;
        double y = wind[1] * scale;
        double r = wind[2] * scale;
        //center mse ellipse


        paint.setStrokeWidth(5.0f);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle((float) x, (float) y, (float) errors[2], paint);
        canvas.drawLine(0, 0, (float) x, (float) y, paint);


        canvas.drawCircle((float) x, (float) y, (float) r, paint);
        //reset matrix
        canvas.setMatrix(new Matrix());
    }

    @Override
    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();
        parameters.add(new ViewComponentParameter("manualScale").setBoolean(manualScale));
        parameters.add(new ViewComponentParameter("scale").setDouble(scale));
//        parameters.add(new ViewComponentParameter("outerRing").setDouble(outerRing));
//        parameters.add(new ViewComponentParameter("wingSize").setDouble(wingSize));
//        parameters.add(new ViewComponentParameter("traceWidth").setDouble(traceWidth));
        return parameters;
    }

    @Override
    public void setParameterValue(ViewComponentParameter parameter) {
        super.setParameterValue(parameter);
        String name = parameter.getName();
        if (name.equals("scale")) {
            scale = (float) parameter.getDoubleValue();
        } else if (name.equals("manualScale")) {
            manualScale = parameter.getBooleanValue();
        }
//        else if (name.equals("outerRing")) {
//            outerRing = (int) parameter.getDoubleValue();
//        } else if (name.equals("wingSize")) {
//            wingSize = (float) parameter.getDoubleValue();
//        }
//        else if (name.equals("traceWidth")) {
//            traceWidth = (float) parameter.getDoubleValue();
//        }
    }
}
