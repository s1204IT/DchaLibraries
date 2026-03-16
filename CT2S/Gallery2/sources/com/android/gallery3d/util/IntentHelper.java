package com.android.gallery3d.util;

import android.content.Context;
import android.content.Intent;

public class IntentHelper {
    public static Intent getCameraIntent(Context context) {
        return new Intent("android.intent.action.MAIN").setClassName("com.android.camera2", "com.android.camera.CameraLauncher");
    }

    public static Intent getGalleryIntent(Context context) {
        return new Intent("android.intent.action.MAIN").setClassName("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");
    }
}
