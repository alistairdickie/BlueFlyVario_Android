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

package com.bfv.model;

public class BufferData {
    private int position;
    private double[] data;

    public BufferData(int position, double[] data) {
        this.position = position;
        this.data = data;
    }

    public int getPosition() {
        return position;
    }

    public double[] getData() {
        return data;
    }

    public void update(int position, double[] dataToCopy) {
        this.position = position;
        for (int i = 0; i < data.length; i++) {
            data[i] = dataToCopy[i];

        }
    }

}
