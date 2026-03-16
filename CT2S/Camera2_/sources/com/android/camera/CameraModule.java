package com.android.camera;

import android.view.KeyEvent;
import android.view.View;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.CameraServices;
import com.android.camera.module.ModuleController;

public abstract class CameraModule implements ModuleController {
    private final CameraProvider mCameraProvider;
    private final CameraServices mServices;

    public abstract String getPeekAccessibilityString();

    @Deprecated
    public abstract boolean onKeyDown(int i, KeyEvent keyEvent);

    @Deprecated
    public abstract boolean onKeyUp(int i, KeyEvent keyEvent);

    @Deprecated
    public abstract void onSingleTapUp(View view, int i, int i2);

    public CameraModule(AppController app) {
        this.mServices = app.getServices();
        this.mCameraProvider = app.getCameraProvider();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
    }

    protected CameraServices getServices() {
        return this.mServices;
    }

    protected CameraProvider getCameraProvider() {
        return this.mCameraProvider;
    }

    protected void requestBackCamera() {
        int backCameraId = this.mCameraProvider.getFirstBackCameraId();
        if (backCameraId != -1) {
            this.mCameraProvider.requestCamera(backCameraId);
        }
    }

    public void onPreviewInitialDataReceived() {
    }

    protected void releaseBackCamera() {
        int backCameraId = this.mCameraProvider.getFirstBackCameraId();
        if (backCameraId != -1) {
            this.mCameraProvider.releaseCamera(backCameraId);
        }
    }
}
