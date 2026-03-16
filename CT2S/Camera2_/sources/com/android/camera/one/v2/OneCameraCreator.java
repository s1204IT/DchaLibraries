package com.android.camera.one.v2;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.util.DisplayMetrics;
import com.android.camera.SoundPlayer;
import com.android.camera.one.OneCamera;
import com.android.camera.util.Size;

public class OneCameraCreator {
    public static OneCamera create(Context context, boolean useHdr, CameraDevice device, CameraCharacteristics characteristics, Size pictureSize, int maxMemoryMB, DisplayMetrics displayMetrics, SoundPlayer soundPlayer) {
        return new OneCameraImpl(device, characteristics, pictureSize);
    }
}
