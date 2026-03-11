package com.android.settingslib.utils;

import android.content.Context;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.text.SpannableStringBuilder;
import android.text.style.TtsSpan;
import java.util.ArrayList;

public class StringUtil {
    public static CharSequence formatElapsedTime(Context context, double d, boolean z) {
        int i;
        int i2;
        int i3;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        int iFloor = (int) Math.floor(d / 1000.0d);
        if (!z) {
            iFloor += 30;
        }
        if (iFloor >= 86400) {
            i = iFloor / 86400;
            iFloor -= 86400 * i;
        } else {
            i = 0;
        }
        if (iFloor >= 3600) {
            i2 = iFloor / 3600;
            iFloor -= i2 * 3600;
        } else {
            i2 = 0;
        }
        if (iFloor >= 60) {
            i3 = iFloor / 60;
            iFloor -= i3 * 60;
        } else {
            i3 = 0;
        }
        ArrayList arrayList = new ArrayList(4);
        if (i > 0) {
            arrayList.add(new Measure(Integer.valueOf(i), MeasureUnit.DAY));
        }
        if (i2 > 0) {
            arrayList.add(new Measure(Integer.valueOf(i2), MeasureUnit.HOUR));
        }
        if (i3 > 0) {
            arrayList.add(new Measure(Integer.valueOf(i3), MeasureUnit.MINUTE));
        }
        if (z && iFloor > 0) {
            arrayList.add(new Measure(Integer.valueOf(iFloor), MeasureUnit.SECOND));
        }
        if (arrayList.size() == 0) {
            arrayList.add(new Measure(0, z ? MeasureUnit.SECOND : MeasureUnit.MINUTE));
        }
        Measure[] measureArr = (Measure[]) arrayList.toArray(new Measure[arrayList.size()]);
        spannableStringBuilder.append((CharSequence) MeasureFormat.getInstance(context.getResources().getConfiguration().locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(measureArr));
        if (measureArr.length == 1 && MeasureUnit.MINUTE.equals(measureArr[0].getUnit())) {
            spannableStringBuilder.setSpan(new TtsSpan.MeasureBuilder().setNumber(i3).setUnit("minute").build(), 0, spannableStringBuilder.length(), 33);
        }
        return spannableStringBuilder;
    }
}
