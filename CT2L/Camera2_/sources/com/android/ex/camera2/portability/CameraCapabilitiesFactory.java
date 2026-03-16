package com.android.ex.camera2.portability;

import android.hardware.Camera;
import com.android.ex.camera2.portability.debug.Log;

public class CameraCapabilitiesFactory {
    private static Log.Tag TAG = new Log.Tag("CamCapabsFact");

    public static CameraCapabilities createFrom(Camera.Parameters p) {
        if (p != null) {
            return new AndroidCameraCapabilities(p);
        }
        Log.w(TAG, "Null parameter passed in.");
        return null;
    }
}
