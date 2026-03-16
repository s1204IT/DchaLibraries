package com.android.gallery3d.ui;

import android.os.PowerManager;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.ui.MenuExecutor;

public class WakeLockHoldingProgressListener implements MenuExecutor.ProgressListener {
    private AbstractGalleryActivity mActivity;
    private PowerManager.WakeLock mWakeLock;

    public WakeLockHoldingProgressListener(AbstractGalleryActivity galleryActivity, String label) {
        this.mActivity = galleryActivity;
        PowerManager pm = (PowerManager) this.mActivity.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(6, label);
    }

    @Override
    public void onProgressComplete(int result) {
        this.mWakeLock.release();
    }

    @Override
    public void onProgressStart() {
        this.mWakeLock.acquire();
    }

    @Override
    public void onProgressUpdate(int index) {
    }

    @Override
    public void onConfirmDialogDismissed(boolean confirmed) {
    }

    @Override
    public void onConfirmDialogShown() {
    }
}
