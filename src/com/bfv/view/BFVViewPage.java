package com.bfv.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import com.bfv.BlueFlyVario;
import com.bfv.view.component.BFVViewComponent;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/09/12
 * Time: 3:02 PM
 */
public class BFVViewPage extends BFVViewComponent {  //extending BFVViewComponent is just a convenience so we can read parameters.


    private ArrayList<BFVViewComponent> viewComponents;
    private VarioSurfaceView surfaceView;


    private int backColor = Color.BLACK;
    private RectF pageFrame;
    private int orientation;


    public BFVViewPage(RectF pageFrame, VarioSurfaceView surfaceView) {
        super(new RectF(), surfaceView);
        this.surfaceView = surfaceView;
        this.pageFrame = pageFrame;
        viewComponents = new ArrayList<BFVViewComponent>();
        orientation = BlueFlyVario.blueFlyVario.getRequestedOrientation();
    }

    public void addViewComponent(BFVViewComponent viewComponent) {
        viewComponents.add(viewComponent);
    }

    public void addViewComponentBack(BFVViewComponent viewComponent) {
        viewComponents.add(0, viewComponent);
    }

    public void setPageFrame(RectF pageFrame) {
        this.pageFrame = pageFrame;
    }

    public RectF getPageFrame() {
        return pageFrame;
    }

    @Override
    public String getViewComponentType() {
        return "View Page";
    }

    public void addToCanvas(Canvas canvas, Paint paint) {
        canvas.drawColor(backColor);
        int alpha = 200;
        BFVViewComponent draging = null;
        for (int i = 0; i < viewComponents.size(); i++) {
            BFVViewComponent viewComponent = viewComponents.get(i);
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
        }


    }
}
