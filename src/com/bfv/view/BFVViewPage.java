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

package com.bfv.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.util.Log;
import android.view.View;
import com.bfv.BlueFlyVario;
import com.bfv.model.LocationAltVar;
import com.bfv.view.component.BFVViewComponent;
import com.bfv.view.map.BFVMapOverlay;

import java.util.ArrayList;

public class BFVViewPage implements ParamatizedComponent {  //extending BFVViewComponent is just a convenience so we can read parameters.


    private ArrayList<BFVViewComponent> viewComponents;
    private ArrayList<BFVMapOverlay> mapOverlays;
    private VarioSurfaceView surfaceView;


    private int backColor = Color.argb(0, 0, 0, 0);
    private RectF pageFrame;
    private int orientation;
    private boolean drawMap;
    private boolean autoPanMap;
    private boolean mapSatelliteMode;
    private int mapZoomLevel;

    private BFVViewComponent draging;
    private BFVViewComponent viewComponent;


    public BFVViewPage(RectF pageFrame, VarioSurfaceView surfaceView) {
        //super(new RectF(), surfaceView);
        this.surfaceView = surfaceView;
        this.pageFrame = pageFrame;
        viewComponents = new ArrayList<BFVViewComponent>();
        mapOverlays = new ArrayList<BFVMapOverlay>();
        orientation = BlueFlyVario.blueFlyVario.getRequestedOrientation();
    }

    public void addViewComponent(BFVViewComponent viewComponent) {
        viewComponents.add(viewComponent);
    }

    public void addViewComponentBack(BFVViewComponent viewComponent) {
        viewComponents.add(0, viewComponent);
    }

    public void addMapOverlay(BFVMapOverlay mapOverlay) {
        mapOverlays.add(mapOverlay);

    }

    public void addMapOverlayBack(BFVMapOverlay mapOverlay) {
        mapOverlays.add(0, mapOverlay);
    }


    public ArrayList<BFVMapOverlay> getMapOverlays() {
        return mapOverlays;
    }

    public void setPageFrame(RectF pageFrame) {
        this.pageFrame = pageFrame;
    }

    public RectF getPageFrame() {
        return pageFrame;
    }

    public boolean drawMap() {
        return drawMap;
    }

    public boolean autoPanMap() {
        return autoPanMap;
    }

    public boolean isMapSatelliteMode() {
        return mapSatelliteMode;
    }

    public int getMapZoomLevel() {
        return mapZoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.mapZoomLevel = zoomLevel;

    }

    public String getParamatizedComponentName() {
        return "View Page";
    }

    public int getParamatizedComponentType() {
        return ParamatizedComponent.TYPE_VIEW_PAGE;
    }

    public void addToCanvas(Canvas canvas, Paint paint) {
        canvas.drawColor(backColor);


        draging = null;
        for (int i = 0; i < viewComponents.size(); i++) {
            viewComponent = viewComponents.get(i);
            if (!viewComponent.isDraging()) {
                viewComponent.addToCanvas(canvas, paint);
                viewComponent.finished(canvas);

            } else {
                draging = viewComponent;
            }

        }

        if (draging != null) {
            draging.addToCanvas(canvas, paint);
            draging.finished(canvas);

        }

    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public ArrayList<BFVViewComponent> getViewComponents() {
        return viewComponents;
    }

    public ArrayList<ViewComponentParameter> getParameters() {
        ArrayList<ViewComponentParameter> parameters = new ArrayList<ViewComponentParameter>();

        parameters.add(new ViewComponentParameter("backColor").setColor(backColor));
        parameters.add(new ViewComponentParameter("frameWidth").setDecimalFormat("0").setDouble(pageFrame.width()));
        parameters.add(new ViewComponentParameter("frameHeight").setDecimalFormat("0").setDouble(pageFrame.height()));
        parameters.add(new ViewComponentParameter("orientation").setIntList(orientation, new String[]{"Landscape", "Portrait"}));
        parameters.add(new ViewComponentParameter("drawMap").setBoolean(drawMap));
        parameters.add(new ViewComponentParameter("autoPanMap").setBoolean(autoPanMap));
        parameters.add(new ViewComponentParameter("mapSatelliteMode").setBoolean(mapSatelliteMode));
        parameters.add(new ViewComponentParameter("mapZoomLevel").setInt(mapZoomLevel));


        return parameters;
    }

    public void setParameterValue(ViewComponentParameter parameter) {

        String name = parameter.getName();
        if (name.equals("backColor")) {
            backColor = parameter.getColorValue();
        } else if (name.equals("frameWidth")) {
            pageFrame.right = (float) parameter.getDoubleValue();
        } else if (name.equals("frameHeight")) {
            pageFrame.bottom = (float) parameter.getDoubleValue();
        } else if (name.equals("orientation")) {
            orientation = parameter.getIntValue();
            //Log.i("BFV", "orient" + orientation);
            surfaceView.setCurrentViewPageOrientation(orientation, true);
            //Log.i("BFV", "orient2" + orientation);
        } else if (name.equals("drawMap")) {
            drawMap = parameter.getBooleanValue();
            surfaceView.drawMap(drawMap());

        } else if (name.equals("autoPanMap")) {
            autoPanMap = parameter.getBooleanValue();
            Location current = BlueFlyVario.blueFlyVario.getVarioService().getBfvLocationManager().getLocation();
            if (current != null) {
                surfaceView.updateLocation(new LocationAltVar(current, 0, 0, 0));
            }


        } else if (name.equals("mapSatelliteMode")) {
            mapSatelliteMode = parameter.getBooleanValue();
            surfaceView.setDrawSatellite(mapSatelliteMode);

        } else if (name.equals("mapZoomLevel")) {
            mapZoomLevel = parameter.getIntValue();
            if (mapZoomLevel < 1) {
                mapZoomLevel = 1;
            }
            if (mapZoomLevel > BlueFlyVario.blueFlyVario.getMapViewManager().getMap().getMaxZoomLevel()) {
                mapZoomLevel = BlueFlyVario.blueFlyVario.getMapViewManager().getMap().getMaxZoomLevel();
            }
            surfaceView.setMapZoom(mapZoomLevel);

        }


    }
}
