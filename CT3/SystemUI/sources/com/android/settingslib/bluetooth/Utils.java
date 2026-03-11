package com.android.settingslib.bluetooth;

import android.content.Context;

public class Utils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    static void showError(Context context, String name, int messageResId) {
        if (sErrorListener == null) {
            return;
        }
        sErrorListener.onShowError(context, name, messageResId);
    }

    public static void setErrorListener(ErrorListener listener) {
        sErrorListener = listener;
    }
}
