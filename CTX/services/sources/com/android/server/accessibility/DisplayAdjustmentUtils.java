package com.android.server.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Binder;
import android.provider.Settings;
import com.android.server.LocalServices;
import com.android.server.display.DisplayTransformManager;

class DisplayAdjustmentUtils {
    private static final int DEFAULT_DISPLAY_DALTONIZER = 12;
    private static final float[] MATRIX_GRAYSCALE = {0.2126f, 0.2126f, 0.2126f, 0.0f, 0.7152f, 0.7152f, 0.7152f, 0.0f, 0.0722f, 0.0722f, 0.0722f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] MATRIX_INVERT_COLOR = {0.402f, -0.598f, -0.599f, 0.0f, -1.174f, -0.174f, -1.175f, 0.0f, -0.228f, -0.228f, 0.772f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    DisplayAdjustmentUtils() {
    }

    public static void applyDaltonizerSetting(Context context, int i) {
        int intForUser;
        ContentResolver contentResolver = context.getContentResolver();
        DisplayTransformManager displayTransformManager = (DisplayTransformManager) LocalServices.getService(DisplayTransformManager.class);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (Settings.Secure.getIntForUser(contentResolver, "accessibility_display_daltonizer_enabled", 0, i) != 0) {
                intForUser = Settings.Secure.getIntForUser(contentResolver, "accessibility_display_daltonizer", 12, i);
            } else {
                intForUser = -1;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            float[] fArr = null;
            if (intForUser == 0) {
                fArr = MATRIX_GRAYSCALE;
                intForUser = -1;
            }
            displayTransformManager.setColorMatrix(200, fArr);
            displayTransformManager.setDaltonizerMode(intForUser);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public static void applyInversionSetting(Context context, int i) {
        ContentResolver contentResolver = context.getContentResolver();
        DisplayTransformManager displayTransformManager = (DisplayTransformManager) LocalServices.getService(DisplayTransformManager.class);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            displayTransformManager.setColorMatrix(DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR, Settings.Secure.getIntForUser(contentResolver, "accessibility_display_inversion_enabled", 0, i) != 0 ? MATRIX_INVERT_COLOR : null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
