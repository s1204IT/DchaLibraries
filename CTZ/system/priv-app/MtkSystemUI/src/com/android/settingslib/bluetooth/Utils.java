package com.android.settingslib.bluetooth;

import android.content.Context;

/* loaded from: classes.dex */
public class Utils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    static void showError(Context context, String str, int i) {
        if (sErrorListener != null) {
            sErrorListener.onShowError(context, str, i);
        }
    }

    public static void setErrorListener(ErrorListener errorListener) {
        sErrorListener = errorListener;
    }
}
