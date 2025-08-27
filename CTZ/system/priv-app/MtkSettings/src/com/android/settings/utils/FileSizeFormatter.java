package com.android.settings.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.BidiFormatter;
import android.text.format.Formatter;

/* loaded from: classes.dex */
public final class FileSizeFormatter {
    public static String formatFileSize(Context context, long j, int i, long j2) {
        if (context == null) {
            return "";
        }
        Formatter.BytesResult bytes = formatBytes(context.getResources(), j, i, j2);
        return BidiFormatter.getInstance().unicodeWrap(context.getString(getFileSizeSuffix(context), bytes.value, bytes.units));
    }

    private static int getFileSizeSuffix(Context context) {
        return context.getResources().getIdentifier("fileSizeSuffix", "string", "android");
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x003e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static Formatter.BytesResult formatBytes(Resources resources, long j, int i, long j2) {
        String str;
        int i2;
        String str2;
        int i3;
        boolean z = j < 0;
        if (z) {
            j = -j;
        }
        float f = j / j2;
        if (j2 == 1) {
            str = "%.0f";
        } else {
            if (f < 1.0f) {
                i2 = 100;
                str2 = "%.2f";
            } else if (f < 10.0f) {
                i2 = 10;
                str2 = "%.1f";
            } else {
                str = "%.0f";
            }
            String str3 = str2;
            i3 = i2;
            str = str3;
            if (z) {
                f = -f;
            }
            return new Formatter.BytesResult(String.format(str, Float.valueOf(f)), resources.getString(i), (Math.round(f * i3) * j2) / i3);
        }
        i3 = 1;
        if (z) {
        }
        return new Formatter.BytesResult(String.format(str, Float.valueOf(f)), resources.getString(i), (Math.round(f * i3) * j2) / i3);
    }
}
