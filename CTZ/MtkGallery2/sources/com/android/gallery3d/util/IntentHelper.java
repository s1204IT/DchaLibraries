package com.android.gallery3d.util;

import android.content.Context;
import android.content.Intent;

public class IntentHelper {
    public static Intent getCameraIntent(Context context) {
        return new Intent("android.intent.action.MAIN").setClassName("com.android.camera", "com.android.camera.CameraActivity");
    }

    public static Intent getGalleryIntent(Context context) {
        return new Intent("android.intent.action.MAIN").setClassName("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");
    }
}
