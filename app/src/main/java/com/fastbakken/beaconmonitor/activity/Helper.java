package com.fastbakken.beaconmonitor.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.fastbakken.beaconmonitor.db.EddyStoneDevice;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Helper {
    public static void sortTagsByRssi(List<EddyStoneDevice> tags) {
        Collections.sort(tags, new Comparator<EddyStoneDevice>() {
            @Override public int compare(EddyStoneDevice o1, EddyStoneDevice o2) {
                return o2.rssi - o1.rssi;
            }
        });
    }


    public static Bitmap createBall(int radius, int ballColor, int letterColor, String letter) {
        letter = letter.toUpperCase();
        Bitmap bitmap = Bitmap.createBitmap(radius*2, radius*2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setColor(ballColor);
        canvas.drawCircle(radius, radius, (float) radius, paint2);
        Paint paint = new Paint();
        paint.setColor(letterColor);
        paint.setTextSize(100);
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        Rect textBounds = new Rect();
        paint.getTextBounds(letter, 0, letter.length(), textBounds);
        canvas.drawText(letter, radius - textBounds.exactCenterX(), radius - textBounds.exactCenterY(), paint);
        return bitmap;
    }


    public static String strDescribingTimeSince(Date date) {
        String output = "";
        Date dateNow = new Date();
        long diffInMS = dateNow.getTime() - date.getTime();
        // show date if the tag has not been seen for 24h
        if (diffInMS > 24 * 60 * 60 * 1000) {
            output += date.toString();
        } else {
            int seconds = (int) (diffInMS / 1000) % 60 ;
            int minutes = (int) ((diffInMS / (1000*60)) % 60);
            int hours   = (int) ((diffInMS / (1000*60*60)) % 24);
            if (hours > 0) output += hours + " h ";
            if (minutes > 0) output += minutes + " min ";
            output += seconds + " s ago";
        }
        return output;
    }
}
