package com.android.camera.one.v1;

import android.os.Handler;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraManager;
import com.android.camera.util.Size;

public class OneCameraManagerImpl extends OneCameraManager {
    @Override
    public void open(OneCamera.Facing facing, boolean enableHdr, Size pictureSize, OneCamera.OpenCallback callback, Handler handler) {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public boolean hasCameraFacing(OneCamera.Facing facing) {
        throw new RuntimeException("Not implemented yet.");
    }
}
