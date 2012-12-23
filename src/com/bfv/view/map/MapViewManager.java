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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.location.Location;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import com.bfv.model.LocationAltVar;
import com.bfv.view.VarioSurfaceView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MapViewManager {
    private MapView map;
    private MapController controller;

    //    private RelativeLayout.LayoutParams mapParams;
    private VarioSurfaceView surfaceView;
    private boolean drawMap;


//    private PointF mid = new PointF();
//    private double pointerDistStart;
//    private boolean zoom;


    public MapViewManager(Context context, VarioSurfaceView surfaceView) {
        this.surfaceView = surfaceView;
//        map = new MapView(context, "0WwWrZfN5JFYpfjMfI76HF0x3d7dpqP8ABNpxFA") {  //debug
        map = new MapView(context, "0WwWrZfN5JFZ7-lAkVT6ld4jENc-EE4TQ0c4ZNw") {  //release
            public void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                processZoom(map.getZoomLevel());

            }
        };    //todo - will need to fix this to be other than the debug key
        //map.setBuiltInZoomControls(true);
        //       map.getController().animateTo(new GeoPoint(0,0));
        //       map.getController().setZoom(18);

        map.setClickable(true);
        map.setEnabled(true);
        map.setVisibility(View.VISIBLE);


        controller = map.getController();

//        BFVMapOverlay overlay = new WingOverlay(surfaceView);
//        addOverlay(overlay);

    }

    public void addOverlay(BFVMapOverlay overlay) {
        map.getOverlays().add(overlay);

    }

    public MapView getMap() {
        return map;
    }

    public void setDrawMap(boolean drawMap) {
        this.drawMap = drawMap;
        if (drawMap) {
            map.setVisibility(View.VISIBLE);

        } else {
            map.setVisibility(View.GONE);
        }
    }

    public void setDrawSatellite(boolean drawSatellite) {
        map.setSatellite(drawSatellite);
    }

    public void setZoomLevel(int zoomLevel) {
        controller.setZoom(zoomLevel);
    }

    public void updateLocation(LocationAltVar loc) {
        Location location = loc.getLocation();
        int latE6 = (int) (location.getLatitude() * 1e6);
        int longE6 = (int) (location.getLongitude() * 1e6);
        controller.animateTo(new GeoPoint(latE6, longE6));

    }

    public void processZoom(int zoom) {
        surfaceView.processMapZoom(zoom);

    }

    public boolean processTouchEvent(PointF downPoint, MotionEvent e) {
        return map.onTouchEvent(e);
//        //Log.i("BFV", "event" + e);
//       if(e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN){
////           PointF pointer1 = new PointF(e.getX(0), e.getY(0));
////           PointF pointer2 = new PointF(e.getX(1), e.getY(1));
//           mid.set((e.getX(0) + e.getX(1))/2.0f, (e.getY(0) + e.getY(1) )/2.0f);
//           float x = e.getX(0) - e.getX(1);
//           float y = e.getY(0) - e.getY(1);
//           pointerDistStart = Math.sqrt(x * x + y * y);
//          // Log.i("BFV", "distSTart" + pointerDistStart);
//           zoom = true;
//
//       }
//       else if(e.getAction() == MotionEvent.ACTION_MOVE && zoom){
//           float x = e.getX(0) - e.getX(1);
//          float y = e.getY(0) - e.getY(1);
//          double pointerDist = Math.sqrt(x * x + y * y);
//          // Log.i("BFV", "dist" + pointerDist);
//           double zoomFactor = pointerDist/pointerDistStart;
//           if(zoomFactor > 1.5){
//               controller.zoomInFixing((int)mid.x, (int)mid.y);
//               pointerDistStart = pointerDist;
//
//           }
//           else if(zoomFactor < 0.75){
//               controller.zoomOutFixing((int)mid.x, (int)mid.y);
//               pointerDistStart = pointerDist;
//           }
//
//
//
//
//       }
//       else if(e.getActionMasked() == MotionEvent.ACTION_POINTER_UP){
//           zoom = false;
//       }
//
//        return true;
    }

//    public boolean processScroll(float x, float y){
////        controller.scrollBy((int)x, (int)y);
//        return true;
//    }

    public void clearOverlays() {
        map.getOverlays().clear();
    }
}
