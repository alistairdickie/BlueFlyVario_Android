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
import com.bfv.view.Field;
import com.bfv.view.FieldManager;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.bfv.view.component.BFVViewComponent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class FieldViewComponent extends BFVViewComponent {

    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;


    private FieldManager fieldManager;

    private Field field;


    private String label;
    private float buffer;

    private float labelSize;
    private float valueSize;

    private int labelAlignment;
    private int valueAlignment;

    private Paint labelPaint;
    private Paint valuePaint;

    private int labelColor = Color.WHITE;
    private int valueColor = Color.GREEN;

    private DecimalFormat df;

    private Rect textBounds = new Rect();
    private int multiplierIndex;

    public FieldViewComponent(RectF rect, VarioSurfaceView view, FieldManager fieldManager, String fieldName) {
        super(rect, view);


        this.fieldManager = fieldManager;
        field = fieldManager.findFieldForString(fieldName);

        setDefaults();

    }

    public FieldViewComponent(RectF rect, VarioSurfaceView view, FieldManager fieldManager, int fieldId) {
        super(rect, view);


        this.fieldManager = fieldManager;
        field = fieldManager.findFieldForId(fieldId);

        setDefaults();

    }


    public void setDefaults() {
//Log.i("BFV", "SetDefaults");
        if (field != null) {
            df = new DecimalFormat(field.getDefaultDecimalFormat(), DecimalFormatSymbols.getInstance(Locale.US));
            multiplierIndex = field.getDefaultMultiplierIndex();
            setDefaultLabel();
        }
        buffer = 2.0f;

        labelSize = 0.3f * rect.height() - buffer * 2.0f;
        valueSize = 0.7f * rect.height() - buffer * 2.0f;

        labelAlignment = ALIGN_LEFT;
        valueAlignment = ALIGN_CENTER;


        setLabelPaint();
        setValuePaint();

    }

    public void setDefaultLabel() {
        if (field != null) {

            label = field.getDefaultLabel() + ": " + field.getUnitMultiplierName(multiplierIndex);
        }

    }

    public void setDecimalFormat(String format) {
        this.df = new DecimalFormat(format, DecimalFormatSymbols.getInstance(Locale.US));
    }

    @Override
    public String getParamatizedComponentName() {
        return "Field_" + field.getFieldName();
    }


    @Override
    public void addToCanvas(Canvas canvas, Paint paint) {
        super.addToCanvas(canvas, paint);

        float prop = labelSize / valueSize;
        //draw RectLine


        canvas.drawLine(0.0f, prop * rect.height(), rect.width(), prop * rect.height(), paint);


        //drawLabel

        labelPaint.getTextBounds(label, 0, label.length(), textBounds);
        float locY = prop * rect.height() / 2.0f + textBounds.height() / 2.0f;
        if (labelAlignment == ALIGN_LEFT) {
            canvas.drawText(label, buffer, locY, labelPaint);
        } else if (labelAlignment == ALIGN_RIGHT) {

            canvas.drawText(label, rect.width() - textBounds.width() - buffer, locY, labelPaint);
        } else if (labelAlignment == ALIGN_CENTER) {

            canvas.drawText(label, rect.width() / 2.0f - textBounds.width() / 2.0f, locY, labelPaint);
        }

        //drawValue
        String value = fieldManager.getValue(field, df, multiplierIndex);
        valuePaint.getTextBounds(value, 0, value.length(), textBounds);
        locY = prop * rect.height() + (1.0f - prop) * rect.height() / 2.0f + textBounds.height() / 2.0f;
        if (valueAlignment == ALIGN_LEFT) {
            canvas.drawText(value, buffer, locY, valuePaint);
        } else if (valueAlignment == ALIGN_RIGHT) {

            canvas.drawText(value, rect.width() - textBounds.width() - buffer, locY, valuePaint);
        } else if (valueAlignment == ALIGN_CENTER) {

            canvas.drawText(value, rect.width() / 2.0f - textBounds.width() / 2.0f, locY, valuePaint);
        }
    }

    public void setLabelPaint() {
        labelPaint = new Paint();
        this.labelPaint.setTextSize(labelSize);
        this.labelPaint.setColor(labelColor);
        this.labelPaint.setStyle(Paint.Style.FILL);
        this.labelPaint.setAntiAlias(true);
    }

    public void setValuePaint() {
        valuePaint = new Paint();
        this.valuePaint.setTextSize(valueSize);
        this.valuePaint.setColor(valueColor);
        this.valuePaint.setStyle(Paint.Style.FILL);
        this.valuePaint.setAntiAlias(true);
    }

    public Field getField() {
        return field;
    }

    public void setMultiplierIndex(int multiplierIndex) {
        this.multiplierIndex = multiplierIndex;
    }

    @Override
    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();
        parameters.add(new ViewComponentParameter("label").setString(label));
        parameters.add(new ViewComponentParameter("labelSize").setDecimalFormat("0.0").setDouble(labelSize));
        parameters.add(new ViewComponentParameter("valueSize").setDecimalFormat("0.0").setDouble(valueSize));
        parameters.add(new ViewComponentParameter("labelAlignment").setIntList(labelAlignment, new String[]{"Center", "Left", "Right"}));
        parameters.add(new ViewComponentParameter("valueAlignment").setIntList(valueAlignment, new String[]{"Center", "Left", "Right"}));
        parameters.add(new ViewComponentParameter("labelColor").setColor(labelColor));
        parameters.add(new ViewComponentParameter("valueColor").setColor(valueColor));
        if (field.getUnits().size() > 1) {
            parameters.add(new ViewComponentParameter("fieldUnits").setIntList(multiplierIndex, field.getUnitNames()));

        }

        parameters.add(new ViewComponentParameter("valueDecimialFormat").setString(df.toPattern()));


        return parameters;

    }

    @Override
    public void setParameterValue(ViewComponentParameter parameter) {
        super.setParameterValue(parameter);
        String name = parameter.getName();
        if (name.equals("label")) {
            label = parameter.getValue();
        } else if (name.equals("labelSize")) {
            labelSize = (float) parameter.getDoubleValue();
            setLabelPaint();
        } else if (name.equals("valueSize")) {
            valueSize = (float) parameter.getDoubleValue();
            setValuePaint();
        } else if (name.equals("labelAlignment")) {
            labelAlignment = parameter.getIntValue();
        } else if (name.equals("valueAlignment")) {
            valueAlignment = parameter.getIntValue();
        } else if (name.equals("labelColor")) {
            labelColor = parameter.getColorValue();
            setLabelPaint();
        } else if (name.equals("valueColor")) {
            valueColor = parameter.getColorValue();
            setValuePaint();
        } else if (name.equals("fieldUnits")) {
            multiplierIndex = parameter.getIntValue();
            setDefaultLabel();
        } else if (name.equals("valueDecimialFormat")) {
            setDecimalFormat(parameter.getValue());

        }

    }
}
