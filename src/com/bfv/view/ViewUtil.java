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

import android.graphics.*;

public class ViewUtil {

    private static Rect textBounds = new Rect();
    private static int r;
    private static int g;
    private static int b;


    public static void addTextBubble(String text, Canvas c, Paint p, float xCenter, float yCenter) {
        addTextBubble(text, c, p, xCenter, yCenter, 4.0f, Color.BLACK, Color.GRAY);


    }

    public static void addTextBubble(String text, Canvas c, Paint p, float xCenter, float yCenter, float padding, int backColor, int frontColor) {


        p.setStyle(Paint.Style.FILL);
        p.getTextBounds(text, 0, text.length(), textBounds);
        float width = textBounds.width() + padding * 2.0f;
        float height = textBounds.height() + padding * 2.0f;
        RectF bubble = new RectF(xCenter - width / 2.0f, yCenter - height / 2.0f, xCenter + width / 2.0f, yCenter + height / 2.0f);

        float radius = 0.0f;

        p.setStyle(Paint.Style.FILL);
        p.setColor(backColor);
        c.drawRoundRect(bubble, radius, radius, p);

        p.setColor(frontColor);
        p.setStyle(Paint.Style.FILL);
        c.drawText(text, xCenter - textBounds.width() / 2.0f, yCenter + textBounds.height() / 2.0f, p);


    }

    public static int getColor(double var) {
        return getColor(var, 255);
    }

    public static int getColor(double var, int alpha) {
        if (var >= 0) {
            g = 255;
            b = 55;
            r = (int) (255 - (var * 200));
            if (r < 55) {
                r = 55;
            }
            return Color.argb(alpha, r, g, b);

        } else {
            g = (int) (255 + (var * 200));
            b = 55;
            r = 255;
            if (g < 55) {
                g = 55;
            }
            return Color.argb(alpha, r, g, b);

        }

    }
}
