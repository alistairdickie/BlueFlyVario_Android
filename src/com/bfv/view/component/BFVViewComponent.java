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
import android.util.Log;
import android.widget.FrameLayout;
import com.bfv.util.DistanceUtil;
import com.bfv.util.Line2d;
import com.bfv.util.Point2d;
import com.bfv.view.ParamatizedComponent;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;

import java.util.ArrayList;

public abstract class BFVViewComponent implements ParamatizedComponent {

    public static final int SELECTED_CENTER = 4;
    public static final int SELECTED_TOP = 0;
    public static final int SELECTED_RIGHT = 1;
    public static final int SELECTED_BOTTOM = 2;
    public static final int SELECTED_LEFT = 3;


    protected float lineWidth = 1.0f;
    private int lineColor = Color.WHITE;
    private int backColor = Color.BLACK;
    protected float cornerRadius = 0.0f;

    public VarioSurfaceView view;
    protected RectF rect;

    public boolean selected;
    public int selectionType = SELECTED_CENTER;
    public RectF selectDownRect;

    public Paint selectedPaint;

    public boolean draging;
    public Paint dragingPaint;


    public BFVViewComponent(RectF rect, VarioSurfaceView view) {
        this.rect = new RectF(rect);
        this.view = view;
        selectedPaint = new Paint();
        selectedPaint.setColor(Color.BLUE);
        selectedPaint.setStrokeWidth(lineWidth);
        selectedPaint.setStyle(Paint.Style.STROKE);
        dragingPaint = new Paint();
        dragingPaint.setColor(Color.YELLOW);
        dragingPaint.setStrokeWidth(lineWidth);
        dragingPaint.setStyle(Paint.Style.STROKE);

        //selectedPaint.setAlpha(100);
    }

    public int getParamatizedComponentType() {
        return ParamatizedComponent.TYPE_VIEW_COMPONENT;
    }

    public abstract String getParamatizedComponentName();


    public void addToCanvas(Canvas canvas, Paint paint) {
        canvas.save();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(lineColor);
        paint.setStrokeWidth(lineWidth);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        canvas.clipRect(rect.left, rect.top, rect.right, rect.bottom, Region.Op.REPLACE);
        canvas.translate(rect.left, rect.top);


    }

    public void scaleComponent(double scaleWidth, double scaleHeight) {
        //Log.i("BFV", "scale" + scaleWidth + " " + scaleHeight);
        rect.top = (float) (rect.top * scaleHeight);
        rect.bottom = (float) (rect.bottom * scaleHeight);
        rect.left = (float) (rect.left * scaleWidth);
        rect.right = (float) (rect.right * scaleWidth);

    }

    public void finished(Canvas canvas) {

        canvas.restore();
        if (selected) {
            Paint paint = selectedPaint;
            if (draging) {
                paint = dragingPaint;
            }
            paint.setStyle(Paint.Style.STROKE);

            switch (selectionType) {
                case SELECTED_CENTER:
                    canvas.drawRect(rect, paint);
                    break;
                case SELECTED_TOP:
                    canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
                    break;
                case SELECTED_LEFT:
                    canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, paint);
                    break;
                case SELECTED_BOTTOM:
                    canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
                    break;
                case SELECTED_RIGHT:
                    canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint);
                    break;

            }


        }

        canvas.save();
        canvas.clipRect(rect.left, rect.top, rect.right, rect.bottom, Region.Op.REPLACE);
        if (view.drawTouchPoints()) {

            Paint paint = selectedPaint;
            if (draging) {
                paint = dragingPaint;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(100);

            canvas.drawCircle(rect.centerX(), rect.centerY(), view.getTouchRadius(), paint);
            paint.setAlpha(255);


        }

        canvas.restore();


    }


    public void processScroll(float x, float y, Rect frame) {
        if (selected) {
            switch (selectionType) {
                case SELECTED_CENTER:
                    rect.left = (int) (selectDownRect.left + x);
                    rect.right = (int) (selectDownRect.right + x);
                    rect.top = (int) (selectDownRect.top + y);
                    rect.bottom = (int) (selectDownRect.bottom + y);

                    if (rect.left < 1) {
                        rect.left = 1;
                        rect.right = selectDownRect.width() + 1;
                    }
                    if (rect.right > frame.width() - 1) {
                        rect.right = frame.width() - 1;
                        rect.left = rect.right - selectDownRect.width();
                    }
                    if (rect.top < 1) {
                        rect.top = 1;
                        rect.bottom = selectDownRect.height() + 1;
                    }
                    if (rect.bottom > frame.height() - 1) {
                        rect.bottom = frame.height() - 1;
                        rect.top = rect.bottom - selectDownRect.height();
                    }

                    break;
                case SELECTED_TOP:
                    rect.top = (int) (selectDownRect.top + y);
                    if (rect.top < 1) {
                        rect.top = 1;
                    }
                    break;
                case SELECTED_LEFT:
                    rect.left = (int) (selectDownRect.left + x);
                    if (rect.left < 1) {
                        rect.left = 1;
                    }
                    break;
                case SELECTED_BOTTOM:
                    rect.bottom = (int) (selectDownRect.bottom + y);
                    if (rect.bottom > frame.height() - 1) {
                        rect.bottom = frame.height() - 1;
                    }
                    break;
                case SELECTED_RIGHT:
                    rect.right = (int) (selectDownRect.right + x);
                    if (rect.right > frame.width() - 1) {
                        rect.right = frame.width() - 1;
                    }
                    break;

            }

        }
    }

    public void setSelected(boolean selected, int selectionType) {
        this.selected = selected;

        this.selectionType = selectionType;
        if (selected) {
            selectDownRect = new RectF(rect);
        } else {
            draging = false;
            selectDownRect = null;
        }
    }

    public boolean isDraging() {
        return draging;
    }

    public void setDraging(boolean draging) {
        this.draging = draging;
    }

    public double distance(Point2d point) {
        return Math.min(DistanceUtil.pointRectFDistance(point, rect), DistanceUtil.distance(point, new Point2d(rect.centerX(), rect.centerY())));

    }

    public double distanceCenter(Point2d point) {
        return DistanceUtil.distance(point, new Point2d(rect.centerX(), rect.centerY()));

    }

    public boolean contains(PointF point) {
        return rect.contains(point.x, point.y);
    }

    public int closest(Point2d point) {
        Line2d[] lines = new Line2d[4];
        lines[0] = new Line2d(new Point2d(rect.left, rect.top), new Point2d(rect.right, rect.top));
        lines[1] = new Line2d(new Point2d(rect.right, rect.top), new Point2d(rect.right, rect.bottom));
        lines[2] = new Line2d(new Point2d(rect.right, rect.bottom), new Point2d(rect.left, rect.bottom));
        lines[3] = new Line2d(new Point2d(rect.left, rect.bottom), new Point2d(rect.left, rect.top));

        int closestIndex = SELECTED_CENTER;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < lines.length; i++) {
            Line2d line = lines[i];
            double dist = DistanceUtil.pointLineDistance(line, point, true);
            if (dist < min) {
                min = dist;
                closestIndex = i;
            }
        }
        double dist = DistanceUtil.distance(point, new Point2d(rect.centerX(), rect.centerY()));
        if (dist < min) {

            closestIndex = SELECTED_CENTER;
        }
        return closestIndex;

    }

    public int getSelectionType() {
        return selectionType;
    }

    public void resetRect() {
        rect = new RectF(selectDownRect);

    }

    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = new ArrayList<ViewComponentParameter>();
        parameters.add(new ViewComponentParameter("top").setDecimalFormat("0.0").setDouble(rect.top));
        parameters.add(new ViewComponentParameter("right").setDecimalFormat("0.0").setDouble(rect.right));
        parameters.add(new ViewComponentParameter("bottom").setDecimalFormat("0.0").setDouble(rect.bottom));
        parameters.add(new ViewComponentParameter("left").setDecimalFormat("0.0").setDouble(rect.left));
        parameters.add(new ViewComponentParameter("lineWidth").setDecimalFormat("0.0").setDouble(lineWidth));
        parameters.add(new ViewComponentParameter("cornerRadius").setDecimalFormat("0.0").setDouble(cornerRadius));
        parameters.add(new ViewComponentParameter("lineColor").setColor(lineColor));
        parameters.add(new ViewComponentParameter("backColor").setColor(backColor));


        return parameters;
    }

    public void setParameterValue(ViewComponentParameter parameter) {
        String name = parameter.getName();
        if (name.equals("top")) {

            rect.top = (float) parameter.getDoubleValue();
        } else if (name.equals("bottom")) {
            rect.bottom = (float) parameter.getDoubleValue();
        } else if (name.equals("right")) {
            rect.right = (float) parameter.getDoubleValue();
        } else if (name.equals("left")) {
            rect.left = (float) parameter.getDoubleValue();
        } else if (name.equals("lineWidth")) {
            lineWidth = (float) parameter.getDoubleValue();
            selectedPaint.setStrokeWidth(lineWidth);
            dragingPaint.setStrokeWidth(lineWidth);
        } else if (name.equals("cornerRadius")) {
            cornerRadius = (float) parameter.getDoubleValue();

        } else if (name.equals("lineColor")) {
            lineColor = parameter.getColorValue();
        } else if (name.equals("backColor")) {
            backColor = parameter.getColorValue();
        }

    }


}
