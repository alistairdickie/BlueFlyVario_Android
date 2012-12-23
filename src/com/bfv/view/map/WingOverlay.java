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

package com.bfv.view.map;

import android.graphics.*;
import android.view.MotionEvent;
import com.bfv.BFVLocationManager;
import com.bfv.model.LocationAltVar;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import java.util.ArrayList;

public class WingOverlay extends BFVMapOverlay {

    private float wingSize = 10.0f;
    private int color = Color.RED;
    private BFVLocationManager bfvLocationManager;
    private Projection proj;
    private Paint paint;
    private Point wingPoint;

    public WingOverlay(VarioSurfaceView view) {
        super(view);
        bfvLocationManager = view.service.getBfvLocationManager();
        paint = new Paint();
        wingPoint = new Point();
        paint.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean b) {
        super.draw(canvas, mapView, b);
        canvas.save();

        proj = mapView.getProjection();
        ArrayList<LocationAltVar> locations = bfvLocationManager.getDisplayableLocations();
        if (locations.size() > 0) {
            LocationAltVar last = locations.get(0);
            GeoPoint lastPoint = new GeoPoint((int) (last.getLocation().getLatitude() * 1E6), (int) (last.getLocation().getLongitude() * 1E6));
            wingPoint = proj.toPixels(lastPoint, wingPoint);

            canvas.translate(wingPoint.x, wingPoint.y);
            canvas.rotate(last.getLocation().getBearing());

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


        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent, MapView mapView) {
        return super.onTouchEvent(motionEvent, mapView);

    }

    public String getParamatizedComponentName() {
        return "Wing";
    }


    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();

        parameters.add(new ViewComponentParameter("wingSize").setDecimalFormat("0.0").setDouble(wingSize));


        return parameters;
    }

    public void setParameterValue(ViewComponentParameter parameter) {
        String name = parameter.getName();
        if (name.equals("wingSize")) {
            wingSize = (float) parameter.getDoubleValue();

        }
    }
}
