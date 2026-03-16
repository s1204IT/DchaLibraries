package com.android.camera.util;

import android.content.ContentResolver;
import com.android.camera.CameraModule;
import com.android.camera.app.AppController;

public class GcamHelper {
    public static CameraModule createGcamModule(AppController app) {
        return null;
    }

    public static boolean hasGcamAsSeparateModule() {
        return false;
    }

    public static boolean hasGcamCapture() {
        return false;
    }

    public static boolean hasGcamAsHDRMode() {
        return false;
    }

    public static void init(ContentResolver contentResolver) {
    }
}
