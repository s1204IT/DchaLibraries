package com.android.camera.util;

import android.content.Context;
import android.net.Uri;
import com.android.camera.CameraModule;
import com.android.camera.app.AppController;

public class RefocusHelper {
    public static CameraModule createRefocusModule(AppController app) {
        return null;
    }

    public static boolean hasRefocusCapture(Context context) {
        return false;
    }

    public static boolean isRGBZ(Context context, Uri contentUri) {
        return false;
    }
}
