package com.bfv.view;

import android.graphics.*;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 28/08/12
 * Time: 8:08 PM
 */
public class ViewUtil {

    private static Rect textBounds = new Rect();


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
            int g = 255;
            int b = 55;
            int r = (int) (255 - (var * 200));
            if (r < 55) {
                r = 55;
            }
            return Color.argb(alpha, r, g, b);

        } else {
            int g = (int) (255 + (var * 200));
            int b = 55;
            int r = 255;
            if (g < 55) {
                g = 55;
            }
            return Color.argb(alpha, r, g, b);

        }

    }
}
