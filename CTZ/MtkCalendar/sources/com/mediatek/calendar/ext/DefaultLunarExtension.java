package com.mediatek.calendar.ext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.Time;

public class DefaultLunarExtension implements ILunarExt {
    @Override
    public String getLunarDisplayedDate(String str, long j, long j2, boolean z) {
        return "";
    }

    @Override
    public String buildLunarDate(Time time, String str, long j) {
        return "";
    }

    @Override
    public void drawLunarString(Context context, Canvas canvas, Paint paint, int i, int i2, Time time) {
    }
}
