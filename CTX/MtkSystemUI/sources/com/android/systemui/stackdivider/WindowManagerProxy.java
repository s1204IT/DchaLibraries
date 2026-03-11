package com.android.systemui.stackdivider;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.GuardedBy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WindowManagerProxy {
    private static final WindowManagerProxy sInstance = new WindowManagerProxy();
    private float mDimLayerAlpha;
    private int mDimLayerTargetWindowingMode;
    private boolean mDimLayerVisible;

    @GuardedBy("mDockedRect")
    private final Rect mDockedRect = new Rect();
    private final Rect mTempDockedTaskRect = new Rect();
    private final Rect mTempDockedInsetRect = new Rect();
    private final Rect mTempOtherTaskRect = new Rect();
    private final Rect mTempOtherInsetRect = new Rect();
    private final Rect mTmpRect1 = new Rect();
    private final Rect mTmpRect2 = new Rect();
    private final Rect mTmpRect3 = new Rect();
    private final Rect mTmpRect4 = new Rect();
    private final Rect mTmpRect5 = new Rect();

    @GuardedBy("mDockedRect")
    private final Rect mTouchableRegion = new Rect();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Runnable mResizeRunnable = new Runnable() {
        @Override
        public void run() {
            Rect rect;
            Rect rect2;
            Rect rect3;
            Rect rect4;
            synchronized (WindowManagerProxy.this.mDockedRect) {
                WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mDockedRect);
                WindowManagerProxy.this.mTmpRect2.set(WindowManagerProxy.this.mTempDockedTaskRect);
                WindowManagerProxy.this.mTmpRect3.set(WindowManagerProxy.this.mTempDockedInsetRect);
                WindowManagerProxy.this.mTmpRect4.set(WindowManagerProxy.this.mTempOtherTaskRect);
                WindowManagerProxy.this.mTmpRect5.set(WindowManagerProxy.this.mTempOtherInsetRect);
            }
            try {
                IActivityManager service = ActivityManager.getService();
                Rect rect5 = WindowManagerProxy.this.mTmpRect1;
                if (!WindowManagerProxy.this.mTmpRect2.isEmpty()) {
                    rect = WindowManagerProxy.this.mTmpRect2;
                } else {
                    rect = null;
                }
                if (!WindowManagerProxy.this.mTmpRect3.isEmpty()) {
                    rect2 = WindowManagerProxy.this.mTmpRect3;
                } else {
                    rect2 = null;
                }
                if (!WindowManagerProxy.this.mTmpRect4.isEmpty()) {
                    rect3 = WindowManagerProxy.this.mTmpRect4;
                } else {
                    rect3 = null;
                }
                if (!WindowManagerProxy.this.mTmpRect5.isEmpty()) {
                    rect4 = WindowManagerProxy.this.mTmpRect5;
                } else {
                    rect4 = null;
                }
                service.resizeDockedStack(rect5, rect, rect2, rect3, rect4);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mDismissRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                ActivityManager.getService().dismissSplitScreenMode(false);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to remove stack: " + e);
            }
        }
    };
    private final Runnable mMaximizeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                ActivityManager.getService().dismissSplitScreenMode(true);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mDimLayerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                WindowManagerGlobal.getWindowManagerService().setResizeDimLayer(WindowManagerProxy.this.mDimLayerVisible, WindowManagerProxy.this.mDimLayerTargetWindowingMode, WindowManagerProxy.this.mDimLayerAlpha);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mSetTouchableRegionRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                synchronized (WindowManagerProxy.this.mDockedRect) {
                    WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mTouchableRegion);
                }
                WindowManagerGlobal.getWindowManagerService().setDockedStackDividerTouchRegion(WindowManagerProxy.this.mTmpRect1);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to set touchable region: " + e);
            }
        }
    };

    private WindowManagerProxy() {
    }

    public static WindowManagerProxy getInstance() {
        return sInstance;
    }

    public void resizeDockedStack(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5) {
        synchronized (this.mDockedRect) {
            this.mDockedRect.set(rect);
            if (rect2 != null) {
                this.mTempDockedTaskRect.set(rect2);
            } else {
                this.mTempDockedTaskRect.setEmpty();
            }
            if (rect3 != null) {
                this.mTempDockedInsetRect.set(rect3);
            } else {
                this.mTempDockedInsetRect.setEmpty();
            }
            if (rect4 != null) {
                this.mTempOtherTaskRect.set(rect4);
            } else {
                this.mTempOtherTaskRect.setEmpty();
            }
            if (rect5 != null) {
                this.mTempOtherInsetRect.set(rect5);
            } else {
                this.mTempOtherInsetRect.setEmpty();
            }
        }
        this.mExecutor.execute(this.mResizeRunnable);
    }

    public void dismissDockedStack() {
        this.mExecutor.execute(this.mDismissRunnable);
    }

    public void maximizeDockedStack() {
        this.mExecutor.execute(this.mMaximizeRunnable);
    }

    public void setResizing(final boolean z) {
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ActivityManager.getService().setSplitScreenResizing(z);
                } catch (RemoteException e) {
                    Log.w("WindowManagerProxy", "Error calling setDockedStackResizing: " + e);
                }
            }
        });
    }

    public int getDockSide() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
        } catch (RemoteException e) {
            Log.w("WindowManagerProxy", "Failed to get dock side: " + e);
            return -1;
        }
    }

    public void setResizeDimLayer(boolean z, int i, float f) {
        this.mDimLayerVisible = z;
        this.mDimLayerTargetWindowingMode = i;
        this.mDimLayerAlpha = f;
        this.mExecutor.execute(this.mDimLayerRunnable);
    }

    public void setTouchRegion(Rect rect) {
        synchronized (this.mDockedRect) {
            this.mTouchableRegion.set(rect);
        }
        this.mExecutor.execute(this.mSetTouchableRegionRunnable);
    }
}
