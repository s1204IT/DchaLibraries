package com.android.gallery3d.ui;

import android.os.PowerManager;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.ui.MenuExecutor;

public class WakeLockHoldingProgressListener implements MenuExecutor.ProgressListener {
    private static final String DEFAULT_WAKE_LOCK_LABEL = "Gallery Progress Listener";
    private AbstractGalleryActivity mActivity;
    private PowerManager.WakeLock mWakeLock;

    public WakeLockHoldingProgressListener(AbstractGalleryActivity abstractGalleryActivity) {
        this(abstractGalleryActivity, DEFAULT_WAKE_LOCK_LABEL);
    }

    public WakeLockHoldingProgressListener(AbstractGalleryActivity abstractGalleryActivity, String str) {
        this.mActivity = abstractGalleryActivity;
        this.mWakeLock = ((PowerManager) this.mActivity.getSystemService("power")).newWakeLock(6, str);
    }

    @Override
    public void onProgressComplete(int i) {
        this.mWakeLock.release();
    }

    @Override
    public void onProgressStart() {
        this.mWakeLock.acquire();
    }

    protected AbstractGalleryActivity getActivity() {
        return this.mActivity;
    }

    @Override
    public void onProgressUpdate(int i) {
    }

    @Override
    public void onConfirmDialogDismissed(boolean z) {
    }

    @Override
    public void onConfirmDialogShown() {
    }
}
