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

import android.graphics.Canvas;
import android.view.MotionEvent;
import com.bfv.view.ParamatizedComponent;
import com.bfv.view.VarioSurfaceView;
import com.bfv.view.ViewComponentParameter;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import java.util.ArrayList;

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
