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

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/10/12
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocationRingOverlay extends BFVMapOverlay {

    private float radius = 20.0f;
    private int color = Color.RED;
    private BFVLocationManager bfvLocationManager;
    private Projection proj;
    private Paint paint;
    private Point locationPoint;

    public LocationRingOverlay(VarioSurfaceView view) {
        super(view);
        bfvLocationManager = view.service.getBfvLocationManager();
        paint = new Paint();
        locationPoint = new Point();
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
            locationPoint = proj.toPixels(lastPoint, locationPoint);

            canvas.translate(locationPoint.x, locationPoint.y);


            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);
            canvas.drawCircle(0.0f, 0.0f, radius, paint);
        }


        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent, MapView mapView) {
        return super.onTouchEvent(motionEvent, mapView);

    }

    public String getParamatizedComponentName() {
        return "LocationRing";
    }


    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = super.getParameters();

        parameters.add(new ViewComponentParameter("radius").setDecimalFormat("0.0").setDouble(radius));


        return parameters;

    }

    public void setParameterValue(ViewComponentParameter parameter) {
        String name = parameter.getName();
        if (name.equals("radius")) {
            radius = (float) parameter.getDoubleValue();

        }

    }
}
