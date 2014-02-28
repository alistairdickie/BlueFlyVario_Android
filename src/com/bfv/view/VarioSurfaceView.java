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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.os.Message;
import android.text.InputFilter;
import android.util.Log;
import android.util.Xml;
import android.view.*;
import android.widget.EditText;
import com.bfv.*;
import com.bfv.model.*;
import com.bfv.util.Point2d;
import com.bfv.view.component.*;
import com.bfv.view.map.BFVMapOverlay;
import com.bfv.view.map.LocationRingOverlay;
import com.bfv.view.map.MapViewManager;
import com.bfv.view.map.WingOverlay;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.ArrayList;

public class VarioSurfaceView extends SurfaceView implements Runnable, SurfaceHolder.Callback, View.OnTouchListener, GestureDetector.OnGestureListener {

    public static ParamatizedComponent editingComponent;//todo - this is nasty, there must be a better way in android...
    public static BFVViewPage editingViewPage;
    public static VarioSurfaceView varioSurfaceView;

    public static final String defaultFileName = "myBlueFlyVarioViews.xml";


    private Thread thread = null;
    private SurfaceHolder surfaceHolder;
    private volatile boolean running = false;
    private volatile boolean surfaceCreated = false;


    public BlueFlyVario bfv;
    public BFVService service;
    public KalmanFilteredAltitude alt;
    public KalmanFilteredVario kalmanVario;
    public KalmanFilteredVario dampedVario;


    private boolean scheduledSetUpData;
    private boolean scheduleRemoveCurrentView;
    private Rect frame;
    private Rect oldFrame;

    private FieldManager fieldManager;

    private ArrayList<BFVViewPage> viewPages;
    private int currentView = 0;
    private int newViewIndex = -1;

    private double fps;

    private GestureDetector gestureDetector;

    private BFVViewComponent selectedComponent;
    private PointF downPoint;
    private boolean touchedMap;

    private boolean allowDragOnTouch = false;

    private boolean firstRun;

    private boolean layoutEnabled;
    private int layoutParameterSelectRadius;
    private boolean layoutDragEnabled;
    private int layoutDragSelectRadius;
    private boolean drawTouchPoints;
    private boolean layoutResizeOrientationChange;
    private boolean layoutResizeImport;

    private boolean layoutResizeOrientationChangeFlag;

    private boolean loading;


    public VarioSurfaceView(BlueFlyVario bfv, BFVService service) {
        super(bfv);
        varioSurfaceView = this;
        this.bfv = bfv;
        this.service = service;
        firstRun = true;
        surfaceHolder = getHolder();

        // this.setBackgroundColor(Color.argb(50,50,200,50));


        bfv.setRequestedOrientation(bfv.getResources().getConfiguration().orientation);//this makes sure it is set at least once to whatever we started with

        surfaceHolder.addCallback(this);
        fieldManager = new FieldManager(this);
        this.setOnTouchListener(this);
        gestureDetector = new GestureDetector(bfv, this);

        alt = service.getAltitude();
        kalmanVario = alt.getKalmanVario();
        dampedVario = alt.getDampedVario();

        readSettings();

    }

    public void onDestroy() {
        //Log.i("BFV", "onDestroySurface");
        this.saveViewsToXML(true, defaultFileName);
        this.saveViewsToXML(false, defaultFileName);
    }


    public void scheduleSetUpData() {
        Log.i("BFV", "ScheduleSetUpData " + running);
        if (!running) {
            service.setUpData();
        } else {
            this.scheduledSetUpData = true;
        }

    }


    public synchronized void onResumeVarioSurfaceView() {
        if (!running && surfaceCreated) {

            thread = new Thread(this);
            running = true;
            thread.start();

        }


    }

    public synchronized void onPauseVarioSurfaceView() {
        if (thread == null) {
            return;
        }
        boolean retry = true;
        running = false;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }

    }

    public void setUpDefaultViews() {
        InputStream inputStream = BlueFlyVario.blueFlyVario.getResources().openRawResource(R.raw.default_layout);
        boolean oldLayoutResizeImport = layoutResizeImport;
        layoutResizeImport = true;
        loadViewsFromXML(inputStream);
        layoutResizeImport = oldLayoutResizeImport;

    }


    public void setUpDefaultViewsManually() {
        // Log.i("BFV", "SetUpDefault");
        //this.stopRunning();

        int orientation = BlueFlyVario.blueFlyVario.getRequestedOrientation();
        viewPages = new ArrayList<BFVViewPage>();
        BFVViewPage viewPage0 = new BFVViewPage(new RectF(frame), this);
        viewPage0.setOrientation(orientation);

        RectF altRect = new RectF(1, 1, 100, 61);
        FieldViewComponent altViewComp = new FieldViewComponent(altRect, this, fieldManager, FieldManager.FIELD_DAMPED_ALTITUDE);
        //altViewComp.setMultiplierIndex(1);
        altViewComp.setDefaultLabel();
        viewPage0.addViewComponent(altViewComp);

        RectF varioRect = new RectF(1, 66, 100, 126);
        FieldViewComponent varioViewComp = new FieldViewComponent(varioRect, this, fieldManager, FieldManager.FIELD_VARIO2);
        viewPage0.addViewComponent(varioViewComp);

        RectF batRect = new RectF(1, 131, 100, 191);
        FieldViewComponent batViewComp = new FieldViewComponent(batRect, this, fieldManager, FieldManager.FIELD_FLIGHT_TIME);
        viewPage0.addViewComponent(batViewComp);

        RectF fpsRect = new RectF(1, 196, 100, 256);
        FieldViewComponent fpsViewComp = new FieldViewComponent(fpsRect, this, fieldManager, FieldManager.FIELD_BAT_PERCENT);
        viewPage0.addViewComponent(fpsViewComp);

        RectF varioComponentRect = new RectF(1, 258, frame.width() - 1, frame.height() - 1);
        VarioTraceViewComponent varioTrace = new VarioTraceViewComponent(varioComponentRect, this);
        viewPage0.addViewComponent(varioTrace);

        RectF locationComponentRect = new RectF(102, 1, frame.width() - 1, 258);
        LocationViewComponent locationView = new LocationViewComponent(locationComponentRect, this);
        viewPage0.addViewComponent(locationView);
        viewPages.add(viewPage0);

        BFVViewPage viewPage1 = new BFVViewPage(new RectF(frame), this);
        viewPage1.setOrientation(orientation);
        RectF label1Rect = new RectF(1, 1, frame.width() - 1, 50);
        LabelViewComponent label1 = new LabelViewComponent(label1Rect, this);
        label1.setLabel("All Fields");

        viewPage1.addViewComponent(label1);
        ArrayList<Field> fields = fieldManager.getFields();

        int cols = 2;
        float fieldHeight = 60;
        float fieldWidth = (float) frame.width() / cols - cols;

        int col = 0;
        int row = 0;

        for (int i = 0; i < fields.size(); i++) {

            Field field = fields.get(i);
            float top = 61 + row * fieldHeight + row * 3;
            float left = 1 + col * fieldWidth + col * 3;
            RectF rect = new RectF(left, top, left + fieldWidth, top + fieldHeight);
            FieldViewComponent fieldViewComponent = new FieldViewComponent(rect, this, fieldManager, field.getId());
            viewPage1.addViewComponent(fieldViewComponent);

            col++;
            if (col == cols) {
                row++;
                col = 0;

            }


        }


        viewPages.add(viewPage1);

//        BFVViewPage viewPage2 = new BFVViewPage(new RectF(frame), this);
//        viewPage2.setOrientation(orientation);
//        RectF label2Rect = new RectF(1, 1, frame.width() - 1, 50);
//        LabelViewComponent label2 = new LabelViewComponent(label2Rect, this);
//        label2.setLabel("Location Example");
//        viewPage2.addViewComponent(label2);
//
//        RectF locationComponentRect2 = new RectF(1, frame.height() - frame.width(), frame.width() - 1, frame.height() - 1);
//        LocationViewComponent locationView2 = new LocationViewComponent(locationComponentRect2, this);
//        viewPage2.addViewComponent(locationView2);
//        viewPages.add(viewPage2);
//
//        BFVViewPage viewPage3 = new BFVViewPage(new RectF(frame), this);
//        viewPage3.setOrientation(orientation);
//        RectF label3Rect = new RectF(1, 1, frame.width() - 1, 50);
//        LabelViewComponent label3 = new LabelViewComponent(label3Rect, this);
//        label3.setLabel("Wind Trace Test");
//        viewPage3.addViewComponent(label3);
//        RectF windComponentRect3 = new RectF(1, frame.height() - frame.width(), frame.width() - 1, frame.height() - 1);
//        WindTraceViewComponent windTrace = new WindTraceViewComponent(windComponentRect3, this);
//        viewPage3.addViewComponent(windTrace);
//        viewPages.add(viewPage3);


        setViewPage(0);
        // this.onResumeVarioSurfaceView();


    }

    public int getNumViews() {
        return viewPages.size();

    }

    public void addView() {
        int orientation = BlueFlyVario.blueFlyVario.getRequestedOrientation();
        viewPages.add(currentView, new BFVViewPage(new RectF(frame), this));
        viewPages.get(currentView).setOrientation(orientation);

    }

    public void chooseAddViewComponent(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final Context con = context;

        builder.setTitle("Select Component Type");

        builder.setItems(ParamatizedComponentManager.viewComponentTypes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

                addDefaultViewComponent(ParamatizedComponentManager.viewComponentTypes[item], con);


            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void chooseAddMapOverlay(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Select Overlay Type");

        ArrayList<String> remainingOverlayTypes = new ArrayList<String>();
        for (int i = 0; i < ParamatizedComponentManager.mapOverlayTypes.length; i++) {
            String overlayType = ParamatizedComponentManager.mapOverlayTypes[i];
            ArrayList<BFVMapOverlay> mapOverlays = viewPages.get(currentView).getMapOverlays();
            boolean add = true;
            for (int j = 0; j < mapOverlays.size(); j++) {
                String mapOverlayName = mapOverlays.get(j).getParamatizedComponentName();
                if (mapOverlayName.equals(overlayType)) {
                    add = false;
                    break;
                }


            }
            if (add) {
                remainingOverlayTypes.add(overlayType);
            }

        }

        final String[] types = new String[remainingOverlayTypes.size()];
        for (int q = 0; q < types.length; q++) {
            types[q] = remainingOverlayTypes.get(q);

        }

        builder.setItems(types, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

                addDefaultMapOverlay(types[item]);


            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }


    public void addDefaultViewComponent(String name, Context context) {
        RectF rect = new RectF(10, 10, frame.width() - 10, frame.height() - 10);
        if (name.equals("Field")) {
            final String[] names = fieldManager.getFieldNames();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final Context con = context;
            final VarioSurfaceView view = this;

            builder.setTitle("Select Field Type");

            builder.setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    RectF rect = new RectF(frame.centerX() - 50, frame.centerY() - 30, frame.centerX() + 50, frame.centerY() + 30);
                    FieldViewComponent component = new FieldViewComponent(rect, view, fieldManager, names[item]);
                    addViewComponent(component);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();


        } else if (name.equals("Location")) {

            addViewComponent(new LocationViewComponent(rect, this));


        } else if (name.equals("VarioTrace")) {

            addViewComponent(new VarioTraceViewComponent(rect, this));

        } else if (name.equals("WindTrace")) {

            addViewComponent(new WindTraceViewComponent(rect, this));

        } else if (name.equals("Label")) {

            RectF labRect = new RectF(20, frame.height() / 2 - 20, frame.width() - 20, frame.height() / 2 + 20);
            addViewComponent(new LabelViewComponent(labRect, this));

        }


    }

    public void addDefaultMapOverlay(String name) {

        if (name.equals("Wing")) {
            addMapOverlay(new WingOverlay(this));


        } else if (name.equals("LocationRing")) {

            addMapOverlay(new LocationRingOverlay(this));


        }

        switchOverlays();

    }

    private void addViewComponent(BFVViewComponent newComponent) {
        viewPages.get(currentView).addViewComponent(newComponent);
        setEditingComponent(newComponent);
        Intent parameterIntent = new Intent(bfv, ParamatizedComponentListActivity.class);
        bfv.startActivity(parameterIntent);
    }

    private void addMapOverlay(BFVMapOverlay mapOverlay) {
        viewPages.get(currentView).addMapOverlay(mapOverlay);
        setEditingComponent(mapOverlay);
        Intent parameterIntent = new Intent(bfv, ParamatizedComponentListActivity.class);
        bfv.startActivity(parameterIntent);
    }


    public void deleteView(Context context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Are you sure you want to delete the current view?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        scheduleRemoveCurrentView = true;
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void removeCurrentView() {
        viewPages.remove(currentView);
        if (viewPages.size() == 0) {
            viewPages.add(new BFVViewPage(new RectF(frame), this));
            setViewPage(0);
        }

        if (currentView >= viewPages.size()) {
            setViewPage(viewPages.size() - 1);
        }
        scheduleRemoveCurrentView = false;

    }


    public void run() {
        frame = null;
        while (frame == null) {
            if (surfaceHolder.getSurface().isValid()) {
                Canvas c = surfaceHolder.lockCanvas();
                Rect r = surfaceHolder.getSurfaceFrame();
                if (r != null) {
                    frame = new Rect(r);
                }

                surfaceHolder.unlockCanvasAndPost(c);
            }

        }


        if (firstRun) { //have to do this in here so we get the frame size for setting up default views.


            try {
                InputStream in = BlueFlyVario.blueFlyVario.openFileInput(defaultFileName);

                viewPages = readViewsFromXML(in);
                setViewPage(newViewIndex);
                newViewIndex = -1;
                if (viewPages == null) {
                    Log.i("BFV", "nullViewPages");
                    this.setUpDefaultViews();

                }

                if (viewPages == null) {
                    setUpDefaultViewsManually();
                }


            } catch (FileNotFoundException e) {
                Log.i("BFV", "run" + e);
                this.setUpDefaultViews();
            }

            firstRun = false;
        }


        Canvas c;
        Rect r;
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        //fps stuff
        double timeavg = 0.0;
        long last = System.nanoTime();
        long current;
        double s;
        double targetTime;
        double sleepTime;
        int fps_limit = Integer.valueOf(BFVSettings.sharedPrefs.getString("fps_limit", "30"));

        BFVViewPage viewPage;
        ArrayList<BFVViewComponent> components;


        while (running) {
            //fps stuff
            current = System.nanoTime();
            s = (current - last) / 1000000000.0;
            timeavg = 0.1 * s + 0.9 * timeavg; //filter the time to get average time
            fps = (1.0 / timeavg);
            last = current;


            if (scheduledSetUpData) {
                service.setUpData();
                scheduledSetUpData = false;

            }

            if (scheduleRemoveCurrentView) {
                removeCurrentView();
            }
            if (surfaceHolder.getSurface().isValid()) {


                c = surfaceHolder.lockCanvas();

                r = surfaceHolder.getSurfaceFrame();

                if (r != null) {
                    frame = new Rect(r);
                    if (layoutResizeOrientationChangeFlag && oldFrame.width() != frame.width()) {


                        viewPage = viewPages.get(currentView);
                        components = viewPage.getViewComponents();
                        for (int j = 0; j < components.size(); j++) {
                            BFVViewComponent viewComponent = components.get(j);
                            viewComponent.scaleComponent((double) frame.width() / (double) oldFrame.width(), (double) frame.height() / (double) oldFrame.height());

                        }
                        viewPage.setPageFrame(new RectF(frame));
                        layoutResizeOrientationChangeFlag = false;
                        oldFrame = null;
                    }


                }


                c.drawColor(0, PorterDuff.Mode.CLEAR); //clear canvas with transparant background

                if (viewPages.size() > 0 && currentView >= 0 && currentView < viewPages.size()) {
                    viewPage = viewPages.get(currentView);
                    viewPage.addToCanvas(c, paint);
                }


                surfaceHolder.unlockCanvasAndPost(c);
            }


            if (fps > fps_limit) {//do some sleeping to slow down rendering.
                current = System.nanoTime();
                double renderTime = (current - last) / 1000000000.0;
                targetTime = 1.0 / fps_limit;
                sleepTime = (targetTime - renderTime);

                if (sleepTime > 0.0) {
                    // Log.i("BFV", "time," + targetTime + "," + timeavg + "," + sleepTime);
                    try {
                        Thread.sleep((int) (sleepTime * 1000.0));
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }

                }


            }

        }


    }

    private void stopRunning() {
        boolean retry = true;
        running = false;

        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }

    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Log.i("BFV", "surfaceCreated");


    }

    public synchronized void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // Log.i("BFV", "surfaceChanged");

        if (running) {
            stopRunning();

        }
        thread = new Thread(this);
        running = true;
        thread.start();
        surfaceCreated = true;

    }

    public synchronized void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // Log.i("BFV", "surfaceDestroyed");


        stopRunning();
        surfaceCreated = false;
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public BFVService getService() {
        return service;
    }

    public double getFps() {
        return fps;
    }

    public void setViewPage(int viewPage) {


        this.currentView = viewPage;
        if (viewPages != null) {
            int orientation = viewPages.get(viewPage).getOrientation();
            setCurrentViewPageOrientation(orientation, false);

            boolean drawMap = viewPages.get(viewPage).drawMap();
            drawMap(drawMap);

            boolean drawSatellite = viewPages.get(viewPage).isMapSatelliteMode();
            setDrawSatellite(drawSatellite);

            setMapZoom(viewPages.get(viewPage).getMapZoomLevel());


        }

        switchOverlays();

        service.getmHandler().obtainMessage(BlueFlyVario.MESSAGE_VIEW_PAGE_CHANGE, currentView, -1).sendToTarget();

    }

    public void setMapZoom(int zoomLevel) {
        MapViewManager mapViewManager = BlueFlyVario.blueFlyVario.getMapViewManager();
        mapViewManager.setZoomLevel(zoomLevel);
    }

    public void processMapZoom(int zoomLevel) {
        if (viewPages != null && viewPages.get(currentView) != null) {
            viewPages.get(currentView).setZoomLevel(zoomLevel);
        }

    }

    public void switchOverlays() {
        MapViewManager mapViewManager = BlueFlyVario.blueFlyVario.getMapViewManager();
        mapViewManager.clearOverlays();
        ArrayList<BFVMapOverlay> mapOverlays = viewPages.get(currentView).getMapOverlays();
        if (mapOverlays != null) {
            for (int i = 0; i < mapOverlays.size(); i++) {
                BFVMapOverlay mapOverlay = mapOverlays.get(i);
                mapViewManager.addOverlay(mapOverlay);
            }

        }


    }

    public void drawMap(boolean drawMap) {
        Message msg = service.getHandler().obtainMessage(BlueFlyVario.MESSAGE_DRAW_MAP);
        Bundle bundle = new Bundle();
        bundle.putBoolean(BlueFlyVario.DRAW_MAP, drawMap);
        msg.setData(bundle);
        service.getHandler().sendMessage(msg);
    }

    public void setDrawSatellite(boolean drawSatellite) {
        MapViewManager mapViewManager = BlueFlyVario.blueFlyVario.getMapViewManager();
        mapViewManager.setDrawSatellite(drawSatellite);
    }

    public void setCurrentViewPageOrientation(int orientation, boolean maybeResize) {
        //Log.i("BFV", "loading" + loading);
        if (loading) {
            return;
        }
        int viewOrientation = BlueFlyVario.blueFlyVario.getRequestedOrientation();
        if (orientation != viewOrientation) {

            if (layoutResizeOrientationChange && maybeResize) {

                oldFrame = new Rect(frame);

                stopRunning();
                BlueFlyVario.blueFlyVario.setRequestedOrientation(orientation);
                layoutResizeOrientationChangeFlag = true;
                thread = new Thread(this);
                running = true;
                thread.start();

            } else {
                BlueFlyVario.blueFlyVario.setRequestedOrientation(orientation);
            }

        }
    }

    public void updateLocation(LocationAltVar loc) {
        if (viewPages == null) {
            return;
        }
        if (viewPages.get(currentView).drawMap() && viewPages.get(currentView).autoPanMap()) {
            BlueFlyVario.blueFlyVario.getMapViewManager().updateLocation(loc);
        }
    }

    public void incrementViewPage() {
        if (currentView >= viewPages.size() - 1) {
            setViewPage(0);
        } else {
            setViewPage(currentView + 1);
        }


    }

    public void decrementViewPage() {
        if (currentView <= 0) {
            setViewPage(viewPages.size() - 1);
        } else {
            setViewPage(currentView - 1);
        }

    }

    public void readSettings() {
        layoutEnabled = BFVSettings.sharedPrefs.getBoolean("layout_enabled", false);
        layoutParameterSelectRadius = Integer.valueOf(BFVSettings.sharedPrefs.getString("layout_parameter_select_radius", "50"));
        layoutDragEnabled = BFVSettings.sharedPrefs.getBoolean("layout_drag_enabled", false);
        layoutDragSelectRadius = Integer.valueOf(BFVSettings.sharedPrefs.getString("layout_drag_select_radius", "20"));
        drawTouchPoints = BFVSettings.sharedPrefs.getBoolean("layout_draw_touch_points", false);
        layoutResizeImport = BFVSettings.sharedPrefs.getBoolean("layout_resize_import", true);
        layoutResizeOrientationChange = BFVSettings.sharedPrefs.getBoolean("layout_resize_orientation_change", true);


    }

    public boolean drawTouchPoints() {
        return layoutEnabled && drawTouchPoints;
    }

    public float getTouchRadius() {
        return layoutParameterSelectRadius;
    }


    public boolean onTouch(View view, MotionEvent e) {
        // Log.i("BFV", "onTouch");
        bfv.doubleBackToExitPressedOnce = false;
        //cancel selected component or map
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (selectedComponent != null) {


                if (selectedComponent.isDraging()) {
                    viewPages.get(currentView).getViewComponents().remove(selectedComponent);
                    viewPages.get(currentView).getViewComponents().add(selectedComponent);

                }
                selectedComponent.setSelected(false, BFVViewComponent.SELECTED_CENTER);


                selectedComponent = null;
                downPoint = null;
                allowDragOnTouch = false;
            }
            if (touchedMap) {
                touchedMap = false;
                downPoint = null;

            }

        }


        //check if we are we touching the map
        if (!touchedMap && e.getAction() == MotionEvent.ACTION_DOWN && viewPages.get(currentView).drawMap()) {   //only check if we have touched components if the map is drawn underneath
            Log.i("BFV", "checkMapTouch");
            ArrayList<BFVViewComponent> viewComponents = viewPages.get(currentView).getViewComponents();
            PointF press = new PointF(e.getX(), e.getY());
            boolean touchedComponent = false;
            for (int i = 0; i < viewComponents.size(); i++) {
                BFVViewComponent viewComponent = viewComponents.get(i);
                if (viewComponent.contains(press)) {
                    touchedComponent = true;
                    break;
                }
            }
            if (!touchedComponent) {
                downPoint = press;
                touchedMap = true;
                Log.i("BFV", "touchedMap");
            }

        }
        //process a touch for a selected component
        if (selectedComponent != null && allowDragOnTouch) {
            selectedComponent.processScroll(e.getX() - downPoint.x, e.getY() - downPoint.y, frame);


        }
        //process a touch for map related event
        if (touchedMap) {
            BlueFlyVario.blueFlyVario.getMapViewManager().processTouchEvent(downPoint, e);
        }

        return gestureDetector.onTouchEvent(e);


    }

    public boolean onDown(MotionEvent motionEvent) {
        // Log.i("BFV", "onDown");

        return true;
    }

    public void onShowPress(MotionEvent motionEvent) {
        // Log.i("BFV", "onShowPress" + motionEvent.getX());
        //are we going to select a component
        if (layoutEnabled && layoutDragEnabled) {

            ArrayList<BFVViewComponent> viewComponents = viewPages.get(currentView).getViewComponents();
            double min = Double.MAX_VALUE;
            BFVViewComponent closestComponent = null;
            Point2d press = new Point2d(motionEvent.getX(), motionEvent.getY());
            for (int i = 0; i < viewComponents.size(); i++) {
                BFVViewComponent viewComponent = viewComponents.get(i);
                viewComponent.setSelected(false, BFVViewComponent.SELECTED_CENTER);
                double dist = viewComponent.distance(press);
                if (dist < min) {
                    min = dist;
                    closestComponent = viewComponent;
                }
            }

            if (min < layoutDragSelectRadius && closestComponent != null) {
                int index = closestComponent.closest(press);
                closestComponent.setSelected(true, index);
                selectedComponent = closestComponent;
                downPoint = new PointF(motionEvent.getX(), motionEvent.getY());
                return;
            }
        }


    }

    public boolean onSingleTapUp(MotionEvent motionEvent) {
        // Log.i("BFV", "onSingleTapUp");
        if (layoutEnabled) {


            double min = Double.MAX_VALUE;
            ArrayList<BFVViewComponent> viewComponents = viewPages.get(currentView).getViewComponents();
            BFVViewComponent closestComponent = null;
            Point2d press = new Point2d(motionEvent.getX(), motionEvent.getY());
            for (int i = 0; i < viewComponents.size(); i++) {
                BFVViewComponent viewComponent = viewComponents.get(i);
                viewComponent.setSelected(false, BFVViewComponent.SELECTED_CENTER);
                double dist = viewComponent.distanceCenter(press);
                if (dist < min) {
                    min = dist;
                    closestComponent = viewComponent;
                }


            }


            if (min < layoutParameterSelectRadius && closestComponent != null) {
                setEditingComponent(closestComponent);
                Intent parameterIntent = new Intent(bfv, ParamatizedComponentListActivity.class);
                bfv.startActivity(parameterIntent);
                downPoint = null;

            }
        }

        return false;
    }


    public boolean onScroll(MotionEvent e, MotionEvent e1, float x, float y) {
//        //Log.i("BFV", "onScroll");
//        if (touchedMap) {
//            return BlueFlyVario.blueFlyVario.getMapViewManager().processScroll(x, y);
//        }

        return false;
    }

    public void onLongPress(MotionEvent e) {
        // Log.i("BFV", "onLongPress" + e.getX());
        if (selectedComponent != null && layoutDragEnabled) {
            selectedComponent.setDraging(true);
            allowDragOnTouch = true;

        }


    }

    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float x, float y) {
        //Log.i("BFV", "onFling");
        if (x < -500.0) {
            incrementViewPage();

        }
        if (x > 500.0) {
            decrementViewPage();

        }
        return true;
    }

    public void viewPageProperties() {
        setEditingComponent(viewPages.get(currentView));
        Intent parameterIntent = new Intent(bfv, ParamatizedComponentListActivity.class);
        bfv.startActivity(parameterIntent);
    }

    public void overlayProperties(Context context) {

        ArrayList<BFVMapOverlay> mapOverlays = viewPages.get(currentView).getMapOverlays();


        final String[] names = new String[mapOverlays.size()];

        for (int j = 0; j < mapOverlays.size(); j++) {
            String mapOverlayName = mapOverlays.get(j).getParamatizedComponentName();
            names[j] = mapOverlayName;

        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        builder.setTitle("Select Overlay");

        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                setEditingComponent(viewPages.get(currentView).getMapOverlays().get(item));
                Intent parameterIntent = new Intent(bfv, ParamatizedComponentListActivity.class);
                bfv.startActivity(parameterIntent);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void setEditingComponent(ParamatizedComponent component) {
        editingComponent = component;
        editingViewPage = viewPages.get(currentView);
    }


    public boolean saveViewsToXML(boolean internal, String name) {
        if (viewPages == null) {
            return false;
        }
        XmlSerializer serializer = Xml.newSerializer();


        FileOutputStream out = null;

        try {
            if (internal) {
                out = BlueFlyVario.blueFlyVario.openFileOutput(name, Context.MODE_PRIVATE);
                //  Log.i("BFV", "internal");
            } else {
                out = new FileOutputStream(new File(BlueFlyVario.blueFlyVario.getExternalFilesDir(null), name));
                //  Log.i("BFV", "external");
            }


            serializer.setOutput(out, "UTF-8");
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "BlueFlyVarioViewPages");
            serializer.attribute("", "currentViewPage", currentView + "");

            for (int i = 0; i < viewPages.size(); i++) {
                BFVViewPage bfvViewPage = viewPages.get(i);

                serializer.startTag("", "BFVViewPage");
                ArrayList<ViewComponentParameter> viewParameters = bfvViewPage.getParameters();
                for (int k = 0; k < viewParameters.size(); k++) {
                    ViewComponentParameter parameter = viewParameters.get(k);
                    serializer.attribute("", parameter.getName(), parameter.getXmlValue());

                }
                ArrayList<BFVViewComponent> components = bfvViewPage.getViewComponents();
                for (int j = 0; j < components.size(); j++) {
                    BFVViewComponent blueFlyViewComponent = components.get(j);
                    String type = blueFlyViewComponent.getParamatizedComponentName();
                    serializer.startTag("", type);
                    ArrayList<ViewComponentParameter> parameters = blueFlyViewComponent.getParameters();
                    for (int k = 0; k < parameters.size(); k++) {
                        ViewComponentParameter parameter = parameters.get(k);
                        serializer.attribute("", parameter.getName(), parameter.getXmlValue());

                    }
                    serializer.endTag("", type);
                }
                ArrayList<BFVMapOverlay> overlays = bfvViewPage.getMapOverlays();
                for (int j = 0; j < overlays.size(); j++) {
                    BFVMapOverlay mapOverlay = overlays.get(j);
                    String type = mapOverlay.getParamatizedComponentName();
                    serializer.startTag("", type);
                    ArrayList<ViewComponentParameter> parameters = mapOverlay.getParameters();
                    for (int k = 0; k < parameters.size(); k++) {
                        ViewComponentParameter parameter = parameters.get(k);
                        serializer.attribute("", parameter.getName(), parameter.getXmlValue());

                    }
                    serializer.endTag("", type);
                }
                serializer.endTag("", "BFVViewPage");
            }
            serializer.endTag("", "BlueFlyVarioViewPages");
            serializer.endDocument();
            serializer.flush();
            out.close();
        } catch (IOException e) {
            Log.e("BFV", "saveViewsToXML" + e);
            return false;
        }
        return true;

    }

    public ArrayList<BFVViewPage> readViewsFromXML(boolean internal, String name) {

        FileInputStream in = null;


        try {
            if (internal) {
                in = BlueFlyVario.blueFlyVario.openFileInput(name);
            } else {
                in = new FileInputStream(new File(BlueFlyVario.blueFlyVario.getExternalFilesDir(null), name));
            }
            return readViewsFromXML(in);


        } catch (Exception e) {


            return null;
        }


    }

    public void exportViews() {
        AlertDialog.Builder alert = new AlertDialog.Builder(bfv);

        alert.setTitle("File Name");
        alert.setMessage("Save the view with this filename");

        // Set an EditText view to get user input
        final EditText input = new EditText(bfv);
        input.setFilters(new InputFilter[]{new FilenameInputFilter()});

        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                saveViewsToXML(false, value + ".xml");

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void loadViewsFromXML(InputStream in) {

        ArrayList<BFVViewPage> newViewPages = readViewsFromXML(in);

        if (layoutResizeImport) {
            for (int i = 0; i < newViewPages.size(); i++) {
                BFVViewPage viewPage = newViewPages.get(i);
                double pageWidth = viewPage.getPageFrame().width();
                double pageHeight = viewPage.getPageFrame().height();
                ArrayList<BFVViewComponent> components = viewPage.getViewComponents();
                for (int j = 0; j < components.size(); j++) {
                    BFVViewComponent viewComponent = components.get(j);
                    viewComponent.scaleComponent(frame.width() / pageWidth, frame.height() / pageHeight);


                }
                viewPage.setPageFrame(new RectF(frame));
                viewPage.setOrientation(BlueFlyVario.blueFlyVario.getRequestedOrientation());

            }

        }

        if (newViewPages != null) {
            viewPages = newViewPages;
            setViewPage(newViewIndex);
            newViewIndex = -1;


        }


    }

    public ArrayList<BFVViewPage> readViewsFromXML(InputStream in) {
        loading = true;
        // Log.i("BFV", "readViews" + in);
        XmlPullParser parser = null;

        ArrayList<BFVViewPage> viewPages = null;
        newViewIndex = -1;

        try {
            parser = XmlPullParserFactory.newInstance().newPullParser();
        } catch (XmlPullParserException e) {
            return null;

        }


        try {

            parser.setInput(in, "UTF-8");

            BFVViewPage currentViewPage = null;

            int eventType = parser.next();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                //   Log.i("BFV", "event" + eventType);

                switch (eventType) {
                    case (XmlPullParser.START_DOCUMENT):
                        break;
                    case (XmlPullParser.START_TAG):
                        String tag = parser.getName();
                        // Log.i("BFV", "tag" + tag);
                        if (tag.equals("BlueFlyVarioViewPages")) {
                            viewPages = new ArrayList<BFVViewPage>();

                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("currentViewPage")) {
                                    newViewIndex = Integer.parseInt(parser.getAttributeValue(i));
                                }


                            }
                        } else if (tag.equals("BFVViewPage")) {
                            currentViewPage = new BFVViewPage(new RectF(frame), this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                //           Log.i("BFV", "attribute" + parser.getAttributeName(i));
                                currentViewPage.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            viewPages.add(currentViewPage);
                        } else if (tag.startsWith("Field")) {
                            String[] split = tag.split("_");
                            FieldViewComponent component = new FieldViewComponent(new RectF(), this, fieldManager, split[1]);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                component.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addViewComponent(component);
                        } else if (tag.equals("Location")) {
                            LocationViewComponent component = new LocationViewComponent(new RectF(), this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                component.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addViewComponent(component);
                        } else if (tag.equals("VarioTrace")) {
                            VarioTraceViewComponent component = new VarioTraceViewComponent(new RectF(), this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                component.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addViewComponent(component);
                        } else if (tag.equals("WindTrace")) {
                            WindTraceViewComponent component = new WindTraceViewComponent(new RectF(), this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                component.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addViewComponent(component);
                        } else if (tag.equals("Label")) {
                            LabelViewComponent component = new LabelViewComponent(new RectF(), this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                component.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addViewComponent(component);
                        } else if (tag.equals("Wing")) {
                            WingOverlay overlay = new WingOverlay(this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                overlay.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addMapOverlay(overlay);
                        } else if (tag.equals("LocationRing")) {
                            LocationRingOverlay overlay = new LocationRingOverlay(this);
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                overlay.setParameterValue(new ViewComponentParameter(parser.getAttributeName(i), parser.getAttributeValue(i)));
                            }
                            currentViewPage.addMapOverlay(overlay);
                        } else {  //unknown tag - quit
                            return null;
                        }


                        break;
                    case (XmlPullParser.END_TAG):
                        //     Log.i("BFV", "end" + parser.getName());
                        if (parser.getName().equals("BlueFlyVarioViewPages")) {
                            in.close();
                            if (newViewIndex == -1) {
                                newViewIndex = 0;
                            }

                            loading = false;
                            return viewPages;
                        }
                        break;
                    case (XmlPullParser.TEXT):
                        //       Log.i("BFV", "text" + parser.getText());
                        break;

                }


                eventType = parser.next();

            }

        } catch (XmlPullParserException e) {
            Log.i("BFV", e.getMessage() + e.toString());
            loading = false;
            return null;
        } catch (IOException e) {
            Log.i("BFV", e.getMessage() + e.toString());
            loading = false;
            return null;
        }
        loading = false;
        return null;

    }


    public static void removeEditingComponent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BlueFlyVario.blueFlyVario);
        builder.setMessage("Are you sure you want to delete this component?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (editingComponent instanceof BFVViewComponent) {
                            editingViewPage.getViewComponents().remove(editingComponent);
                        }
                        if (editingComponent instanceof BFVMapOverlay) {
                            editingViewPage.getMapOverlays().remove(editingComponent);
                            varioSurfaceView.switchOverlays();
                        }

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();


    }

    public static void moveEditingComponentFront() {
        if (editingComponent instanceof BFVViewComponent) {
            editingViewPage.getViewComponents().remove(editingComponent);
            editingViewPage.addViewComponent((BFVViewComponent) editingComponent);
        }
        if (editingComponent instanceof BFVMapOverlay) {
            editingViewPage.getMapOverlays().remove(editingComponent);
            editingViewPage.addMapOverlay((BFVMapOverlay) editingComponent);
        }

    }

    public static void moveEditingComponentBack() {
        if (editingComponent instanceof BFVViewComponent) {
            editingViewPage.getViewComponents().remove(editingComponent);
            editingViewPage.addViewComponentBack((BFVViewComponent) editingComponent);
        }
        if (editingComponent instanceof BFVMapOverlay) {
            editingViewPage.getMapOverlays().remove(editingComponent);
            editingViewPage.addMapOverlayBack((BFVMapOverlay) editingComponent);
        }

    }
}
