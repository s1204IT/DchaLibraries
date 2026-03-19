package com.android.deskclock.widget.toast;

import android.widget.Toast;

public final class ToastManager {
    private static Toast sToast = null;

    private ToastManager() {
    }

    public static void setToast(Toast toast) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = toast;
    }

    public static void cancelToast() {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = null;
    }
}
