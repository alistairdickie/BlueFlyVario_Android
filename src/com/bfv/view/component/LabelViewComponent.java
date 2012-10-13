package com.bfv.view.component;

import android.graphics.*;
import android.util.Log;
import com.bfv.BFVLocationManager;
import com.bfv.model.LocationAltVar;
import com.bfv.view.*;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 29/08/12
 * Time: 8:06 PM
 */
public class LabelViewComponent extends BFVViewComponent {

    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    private String label = "label";

    private float labelSize;

    private int labelAlignment;

    private Paint labelPaint;
    private float buffer;

    private int labelColor = Color.WHITE;

    private Rect textBounds = new Rect();


    public LabelViewComponent(RectF rect, VarioSurfaceView view) {
        super(rect, view);
        setDefaults();
    }

    public void setDefaults() {
        //Log.i("BFV", "SetDefaults");


        buffer = 2.0f;
        labelSize = rect.height() - 2.0f * buffer;


        labelAlignment = ALIGN_LEFT;

        setLabelPaint();

    }

    @Override
    public String getParamatizedComponentName() {
        return "Label";
    }

    public void addToCanvas(Canvas canvas, Paint paint) {
        super.addToCanvas(canvas, paint);


        //drawLabel

        labelPaint.getTextBounds(label, 0, label.length(), textBounds);


        float locY = rect.height() / 2.0f + textBounds.height() / 2.0f; //todo - for some reason this does not properly center the text
        if (labelAlignment == ALIGN_LEFT) {
            canvas.drawText(label, buffer, locY, labelPaint);
        } else if (labelAlignment == ALIGN_RIGHT) {

            canvas.drawText(label, rect.width() - textBounds.width() - buffer, locY, labelPaint);
        } else if (labelAlignment == ALIGN_CENTER) {

            canvas.drawText(label, rect.width() / 2.0f - textBounds.width() / 2.0f, locY, labelPaint);
        }


    }

    public void setLabelPaint() {
        labelPaint = new Paint();
        this.labelPaint.setTextSize(labelSize);
        this.labelPaint.setColor(labelColor);
        this.labelPaint.setStyle(Paint.Style.FILL);
        this.labelPaint.setAntiAlias(true);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLabelSize(float labelSize) {
        this.labelSize = labelSize;
    }

    public void setLabelAlignment(int labelAlignment) {
        this.labelAlignment = labelAlignment;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();
        parameters.add(new ViewComponentParameter("label").setString(label));
        parameters.add(new ViewComponentParameter("labelSize").setDecimalFormat("0.0").setDouble(labelSize));

        parameters.add(new ViewComponentParameter("labelAlignment").setIntList(labelAlignment, new String[]{"Center", "Left", "Right"}));

        parameters.add(new ViewComponentParameter("labelColor").setColor(labelColor));


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

        } else if (name.equals("labelAlignment")) {
            labelAlignment = parameter.getIntValue();
        } else if (name.equals("labelColor")) {
            labelColor = parameter.getColorValue();
            setLabelPaint();
        }

    }


}
