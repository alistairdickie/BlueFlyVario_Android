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
import com.bfv.model.BufferData;
import com.bfv.DataBuffer;
import com.bfv.model.KalmanFilteredVario;
import com.bfv.model.Vario;
import com.bfv.util.ArrayUtil;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.bfv.view.ViewUtil;
import com.bfv.view.component.BFVViewComponent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class VarioTraceViewComponent extends BFVViewComponent {

    public static final int VARIO1 = 0;
    public static final int VARIO2 = 1;


    private float prop;
    private int traceVario;
    private int scaleVario;
    private double maxThreshold;    //for the blue dot
    private boolean dotsInsteadOfLines = false;


    private KalmanFilteredVario traceVar;
    private KalmanFilteredVario scaleVar;

    private BufferData varBufferData;
    //    private BufferData varBufferData2;
    private BufferData altBufferData;
    private Rect textBounds = new Rect();
    private DecimalFormat dfVarioScale = new DecimalFormat("+0;-0", DecimalFormatSymbols.getInstance(Locale.US));
    private DecimalFormat dfVarioScale1 = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
    private DecimalFormat dfAlt = new DecimalFormat("0.0m ", DecimalFormatSymbols.getInstance(Locale.US));


    public VarioTraceViewComponent(RectF rect, VarioSurfaceView view) {
        super(rect, view);
        prop = 0.25f;
        setTraceVario(VARIO2);
        setScaleVario(VARIO1);
        maxThreshold = 0.2;

        varBufferData = null;
        altBufferData = null;


    }

    @Override
    public String getParamatizedComponentName() {
        return "VarioTrace";
    }

    @Override
    public void addToCanvas(Canvas canvas, Paint paint) {
        super.addToCanvas(canvas, paint);
        float xLoc = 0.0f;
        float yLoc = 0.0f;
        float width = rect.width();
        float height = rect.height();

        DataBuffer buffer = view.service.getDataBuffer();
        int size = buffer.getBufferSize();
        varBufferData = buffer.getData(traceVar);
//        varBufferData2 = buffer.getData(scaleVar, varBufferData2);
        altBufferData = buffer.getData(view.alt);

        double[] varData = varBufferData.getData();
//        double[] varData2 = varBufferData2.getData();
        double[] altData = altBufferData.getData();


        double min1 = Math.abs(ArrayUtil.getMinValue(varData)[0]);
        double[] maxVar = ArrayUtil.getMaxValue(varData);
        double max1 = Math.abs(maxVar[0]);
        int maxVarIndex = (int) maxVar[1];

        float mag = (float) Math.max(min1, max1);

        if (mag < 1.5f) {
            mag = 1.5f;
        }

        float yScale = (height - 2.0f) / mag / 2.0f;
        float yZero = yLoc + height / 2.0f;

        double minAlt = ArrayUtil.getMinValue(altData)[0];
        double maxAlt = ArrayUtil.getMaxValue(altData)[0];

        double currentAlt = view.service.getAltitude().getDampedAltitude();
        double altScale = Math.max(maxAlt - currentAlt, currentAlt - minAlt);
        if (altScale < 1.0) {
            altScale = 1.0;
        }


        //scaleRect

        canvas.drawRect(xLoc, yLoc, xLoc + width * prop, yLoc + height, paint);

        //graphRect

        canvas.drawRect(xLoc + width * prop, yLoc, xLoc + width, yLoc + height, paint);

        //varscale
        float interval;
        float textSize = 30.0f;
        if (mag <= 2.5f) {
            interval = 0.5f;

        } else if (mag <= 5.0f) {
            interval = 1.0f;
        } else {
            interval = 2.0f;
        }
        float triProp = 0.8f;

        for (float y = 0.0f; y < mag; y += interval) {
            float yPix = (y * yScale);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(xLoc + 2.0f, yZero - yPix, xLoc + width * prop - 4.0f, yZero - yPix, paint);
            canvas.drawLine(xLoc + 2.0f, yZero + yPix, xLoc + width * prop - 4.0f, yZero + yPix, paint);


            if (y < 0.00001f) {

                paint.setTextSize(textSize);
                paint.setStyle(Paint.Style.FILL);
                ViewUtil.addTextBubble("0", canvas, paint, xLoc + width * prop * 0.3f, yZero - yPix);


            } else if (y % (interval * 2.0f) < 0.00001f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                paint.setColor(Color.GRAY);
                canvas.drawLine(xLoc + 2.0f, yZero - yPix, xLoc + width * prop - 4.0f, yZero - yPix, paint);
                canvas.drawLine(xLoc + 2.0f, yZero + yPix, xLoc + width * prop - 4.0f, yZero + yPix, paint);
                paint.setStrokeWidth(1.0f);

                paint.setTextSize(textSize);
                paint.setStyle(Paint.Style.FILL);

                ViewUtil.addTextBubble(dfVarioScale.format(y), canvas, paint, xLoc + width * prop * triProp / 2.0f, yZero - yPix);
                ViewUtil.addTextBubble(dfVarioScale.format(-y), canvas, paint, xLoc + width * prop * triProp / 2.0f, yZero + yPix);


            }
        }

        //varMark

        float value = (float) scaleVar.getValue();

        Path mark = new Path();
        float yPix = yZero - yScale * value;
        mark.moveTo(xLoc + width * prop - 1.0f, yPix);
        mark.lineTo(xLoc + width * prop * triProp, yPix + textSize / 2.0f);
        mark.lineTo(xLoc + width * prop * triProp, yPix - textSize / 2.0f);
        mark.close();
        paint.setColor(ViewUtil.getColor(value));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mark, paint);
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mark, paint);


        RectF markRect = new RectF(xLoc + 2.0f, yPix - textSize / 2.0f, xLoc + width * prop * triProp, yPix + textSize / 2.0f);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(markRect, paint);
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(markRect, paint);

        String text = dfVarioScale1.format(scaleVar.getValue());
        if (text.equals("-0.0")) {
            text = "0.0";
        }
        paint.setStyle(Paint.Style.FILL);
        paint.getTextBounds(text, 0, text.length(), textBounds);

        paint.setColor(Color.WHITE);

        float xCent = xLoc + (width * prop * triProp) / 2.0f;

        canvas.drawText(text, xCent - textBounds.width() / 2.0f, yPix + textBounds.height() / 2.0f, paint);


        //alt
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);

        Path altShape = new Path();
        float xAlt = xLoc + width * prop + 0.5f;

        float xStep = (width * (1.0f - prop) - 1.0f) / (size - 1);
        altShape.moveTo(xAlt, yLoc + height / 2.0f);

        float altPoint = 0.0f;
        int i = altBufferData.getPosition();
        int count = 0;

        while (count < size) {

            altPoint = (float) altData[i];

            float yAlt = (float) (yLoc + height / 2.0f + (currentAlt - altPoint) * (height - 2.0f) / altScale / 2.0f);

            altShape.lineTo(xAlt, yAlt);

            if (i == 0) {

                i = size - 1;

            } else {

                i--;
            }

            xAlt += xStep;
            count++;


        }
        altShape.lineTo(xLoc + width - 1.0f, yLoc + height - 1.0f);
        altShape.lineTo(xLoc + width * prop + 0.5f, yLoc + height - 1.0f);
        altShape.close();

        canvas.drawPath(altShape, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(20.0f);
        canvas.drawText(dfAlt.format(altScale), xLoc + width * prop + 1.5f, yLoc + height - 2.0f, paint);


        //trace
        paint.setStrokeWidth(4.0f);
        paint.setStrokeCap(Paint.Cap.ROUND);


        float x = xLoc + width * prop + 0.5f;

        i = varBufferData.getPosition();
        count = 0;

        float var = 0.0f;
        float varPrevious = 0.0f;
        float maxX = x;
        while (count < size - 1) {


            var = (float) varData[i];


            if (i == 0) {
                varPrevious = (float) varData[size - 1];
                i = size - 1;

            } else {
                varPrevious = (float) varData[i - 1];
                i--;
            }

            if (i == maxVarIndex) {
                maxX = x;
            }


            paint.setColor(ViewUtil.getColor(var));

            if (dotsInsteadOfLines) {
                canvas.drawCircle(x, yZero - yScale * var, 2.0f, paint);

            } else {
                canvas.drawLine(x, yZero - yScale * var, x + xStep, yZero - yScale * varPrevious, paint);
            }


            x += xStep;
            count++;

        }
        paint.setStrokeCap(Paint.Cap.BUTT);

//         //trace2
//        paint.setStrokeWidth(1.0f);
//        paint.setStrokeCap(Paint.Cap.ROUND);
//
//
//
//        x = xLoc + width * prop + 0.5f;
//
//        i = varBufferData.getPosition();
//        count = 0;
//
//        maxX = x;
//        while (count < size - 1) {
//
//
//            var = (float) varData2[i];
//
//
//            if (i == 0) {
//                varPrevious = (float) varData2[size - 1];
//                i = size - 1;
//
//            } else {
//                varPrevious = (float) varData2[i - 1];
//                i--;
//            }
//
//            if (i == maxVarIndex) {
//                maxX = x;
//            }
//
//
//            paint.setColor(Color.WHITE);
//
//           canvas.drawLine(x, yZero - yScale * var, x + xStep, yZero - yScale * varPrevious, paint);
//
//
//
//            x += xStep;
//            count++;
//
//        }
//        paint.setStrokeCap(Paint.Cap.BUTT);


        //drawMaxDot
        if (max1 >= maxThreshold) {

            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawCircle(maxX, (float) (yZero - yScale * varData[maxVarIndex]), 5.0f, paint);


            paint.setStyle(Paint.Style.STROKE);


        }
    }

    public void setTraceVario(int traceVario) {
        this.traceVario = traceVario;
        if (traceVario == VARIO2) {
            traceVar = view.dampedVario;
        } else {
            traceVar = view.kalmanVario;
        }

    }

    public void setScaleVario(int scaleVario) {
        this.scaleVario = scaleVario;
        if (scaleVario == VARIO2) {
            scaleVar = view.dampedVario;
        } else {
            scaleVar = view.kalmanVario;
        }
    }


    @Override
    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();
        parameters.add(new ViewComponentParameter("prop").setDecimalFormat("0.00").setDouble(prop));
        parameters.add(new ViewComponentParameter("traceVario").setIntList(traceVario, new String[]{"vario1", "vario2"}));
        parameters.add(new ViewComponentParameter("scaleVario").setIntList(scaleVario, new String[]{"vario1", "vario2"}));
        parameters.add(new ViewComponentParameter("maxThreshold").setDecimalFormat("0.00").setDouble(maxThreshold));
        parameters.add(new ViewComponentParameter("dotsInsteadOfLines").setBoolean(dotsInsteadOfLines));

        return parameters;
    }

    @Override
    public void setParameterValue(ViewComponentParameter parameter) {
        super.setParameterValue(parameter);
        String name = parameter.getName();
        if (name.equals("prop")) {
            prop = (float) parameter.getDoubleValue();
        } else if (name.equals("traceVario")) {
            setTraceVario(parameter.getIntValue());
        } else if (name.equals("scaleVario")) {
            setScaleVario(parameter.getIntValue());
        } else if (name.equals("maxThreshold")) {
            maxThreshold = parameter.getDoubleValue();
        } else if (name.equals("dotsInsteadOfLines")) {
            dotsInsteadOfLines = parameter.getBooleanValue();
        }
    }


}
