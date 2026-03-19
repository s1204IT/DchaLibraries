package com.android.deskclock.widget.toast;

import android.support.design.widget.Snackbar;
import java.lang.ref.WeakReference;

public final class SnackbarManager {
    private static WeakReference<Snackbar> sSnackbar = null;

    private SnackbarManager() {
    }

    public static void show(Snackbar snackbar) {
        sSnackbar = new WeakReference<>(snackbar);
        snackbar.show();
    }

    public static void dismiss() {
        Snackbar snackbar;
        if (sSnackbar != null) {
            snackbar = sSnackbar.get();
        } else {
            snackbar = null;
        }
        if (snackbar != null) {
            snackbar.dismiss();
            sSnackbar = null;
        }
    }
}
