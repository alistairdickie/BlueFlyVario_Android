package com.bfv.view.map;

import android.graphics.Canvas;
import android.view.MotionEvent;
import com.bfv.view.ParamatizedComponent;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 1/10/12
 * Time: 10:51 AM
 */
public abstract class BFVMapOverlay extends Overlay implements ParamatizedComponent {

    public VarioSurfaceView view;

    public BFVMapOverlay(VarioSurfaceView view) {
        this.view = view;
    }

    public int getParamatizedComponentType() {
        return ParamatizedComponent.TYPE_MAP_OVERLAY;
    }

    public ArrayList<ViewComponentParameter> getParameters() {
        return new ArrayList<ViewComponentParameter>();
    }


}
