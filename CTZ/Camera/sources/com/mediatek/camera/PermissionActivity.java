package com.mediatek.camera;

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.permission.PermissionManager;

public abstract class PermissionActivity extends QuickActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PermissionActivity.class.getSimpleName());
    private int mActivityState = 4;
    private PermissionManager mPermissionManager;
    private Bundle mSavedInstanceState;

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        LogHelper.d(TAG, "onRequestPermissionsResult(), grantResults = " + iArr.length);
        if (iArr.length > 0 && this.mPermissionManager.getCameraLaunchPermissionRequestCode() == i && !this.mPermissionManager.isCameraLaunchPermissionsResultReady(strArr, iArr)) {
            finish();
        }
    }

    @Override
    protected void onPermissionCreateTasks(Bundle bundle) {
        this.mPermissionManager = new PermissionManager(this);
        this.mSavedInstanceState = bundle;
        if (this.mPermissionManager.checkCameraLaunchPermissions()) {
            onCreateTasks(bundle);
            this.mActivityState = 1;
        }
    }

    @Override
    protected void onPermissionStartTasks() {
        onStartTasks();
    }

    @Override
    protected void onPermissionResumeTasks() {
        if (!this.mPermissionManager.checkCameraLaunchPermissions() && !this.mPermissionManager.requestCameraAllPermissions()) {
            return;
        }
        if (this.mActivityState == 4) {
            onCreateTasks(this.mSavedInstanceState);
        }
        this.mSavedInstanceState = null;
        onResumeTasks();
        this.mActivityState = 2;
    }

    @Override
    protected void onPermissionPauseTasks() {
        if (this.mActivityState == 2) {
            onPauseTasks();
            this.mActivityState = 3;
        }
    }

    @Override
    protected void onPermissionStopTasks() {
        onStopTasks();
    }

    @Override
    protected void onPermissionDestroyTasks() {
        if (this.mActivityState != 4) {
            onDestroyTasks();
            this.mActivityState = 4;
        }
    }

    protected void onCreateTasks(Bundle bundle) {
    }

    protected void onStartTasks() {
    }

    protected void onResumeTasks() {
    }

    protected void onPauseTasks() {
    }

    protected void onStopTasks() {
    }

    protected void onDestroyTasks() {
    }
}
