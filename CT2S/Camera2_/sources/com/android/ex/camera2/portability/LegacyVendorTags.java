package com.android.ex.camera2.portability;

import android.util.Log;

public class LegacyVendorTags {
    public static final int CONTROL_SCENE_MODE_HDR;
    private static final String TAG = "LegacyVendorTags";

    static {
        int tempSceneMode = -1;
        try {
            tempSceneMode = Class.forName("android.hardware.camera2.CameraCharacteristics").getField("CONTROL_SCENE_MODE_HDR").getInt(null);
        } catch (Exception e) {
            Log.e(TAG, "Error while reflecting on SCENE_MODE_HDR enum, HDR will not be available: " + e);
        } finally {
            CONTROL_SCENE_MODE_HDR = tempSceneMode;
        }
    }

    private LegacyVendorTags() {
        throw new AssertionError();
    }
}
