package com.mediatek.camera.ui.preview;

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.common.widget.PreviewFrameLayout;
import com.mediatek.camera.portability.SystemProperties;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class PreviewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewManager.class.getSimpleName());
    private IApp mApp;
    private View.OnTouchListener mOnTouchListener;
    private IController mPreviewController;
    private PreviewFrameLayout mPreviewFrameLayout;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private RectF mPreviewArea = new RectF();
    private View.OnLayoutChangeListener mOnLayoutChangeCallback = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            PreviewManager.this.mPreviewArea.set(i, i2, i3, i4);
            PreviewManager.this.mPreviewFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    PreviewManager.this.notifyPreviewAreaChanged();
                    PreviewManager.this.mPreviewFrameLayout.setPreviewSize((int) PreviewManager.this.mPreviewArea.width(), (int) PreviewManager.this.mPreviewArea.height());
                }
            });
        }
    };
    private View.OnTouchListener mOnTouchListenerImpl = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (PreviewManager.this.mOnTouchListener != null) {
                return PreviewManager.this.mOnTouchListener.onTouch(view, motionEvent);
            }
            return false;
        }
    };
    private final CopyOnWriteArrayList<IAppUiListener.OnPreviewAreaChangedListener> mPreviewAreaChangedListeners = new CopyOnWriteArrayList<>();

    public PreviewManager(IApp iApp) {
        this.mApp = iApp;
        this.mPreviewFrameLayout = (PreviewFrameLayout) this.mApp.getActivity().findViewById(R.id.preview_layout_container);
        int i = SystemProperties.getInt("vendor.debug.surface.enabled", 0);
        int i2 = SystemProperties.getInt("ro.vendor.mtk_camera_app_version", 2);
        LogHelper.i(TAG, "enabledValue = " + i + " appVersion " + i2);
        if (i == 1 || i2 == 2) {
            this.mPreviewController = new SurfaceViewController(iApp);
        } else {
            this.mPreviewController = new TextureViewController(iApp);
        }
        this.mPreviewController.setOnLayoutChangeListener(this.mOnLayoutChangeCallback);
        this.mPreviewController.setOnTouchListener(this.mOnTouchListenerImpl);
    }

    public void updatePreviewSize(int i, int i2, IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        LogHelper.i(TAG, "updatePreviewSize: new size (" + i + " , " + i2 + " ) current size (" + this.mPreviewWidth + " , " + this.mPreviewHeight + " )");
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
        if (this.mPreviewController != null) {
            this.mPreviewController.updatePreviewSize(i, i2, iSurfaceStatusListener);
        }
    }

    public void clearPreviewStatusListener(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener) {
        this.mPreviewController.clearPreviewStatusListener(iSurfaceStatusListener);
    }

    public void registerPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener) {
        if (onPreviewAreaChangedListener != null && !this.mPreviewAreaChangedListeners.contains(onPreviewAreaChangedListener)) {
            this.mPreviewAreaChangedListeners.add(onPreviewAreaChangedListener);
            if (this.mPreviewArea.width() != 0.0f || this.mPreviewArea.height() != 0.0f) {
                onPreviewAreaChangedListener.onPreviewAreaChanged(this.mPreviewArea, new Size(this.mPreviewWidth, this.mPreviewHeight));
            }
        }
    }

    public void unregisterPreviewAreaChangedListener(IAppUiListener.OnPreviewAreaChangedListener onPreviewAreaChangedListener) {
        if (onPreviewAreaChangedListener != null && this.mPreviewAreaChangedListeners.contains(onPreviewAreaChangedListener)) {
            this.mPreviewAreaChangedListeners.remove(onPreviewAreaChangedListener);
        }
    }

    public void setEnabled(boolean z) {
        this.mPreviewController.setEnabled(z);
    }

    public void onPause() {
        this.mPreviewController.onPause();
    }

    public PreviewFrameLayout getPreviewFrameLayout() {
        return this.mPreviewFrameLayout;
    }

    public void removeTopSurface() {
        if (this.mPreviewController != null) {
            this.mPreviewController.removeTopSurface();
        }
    }

    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.mOnTouchListener = onTouchListener;
    }

    private void notifyPreviewAreaChanged() {
        Iterator<IAppUiListener.OnPreviewAreaChangedListener> it = this.mPreviewAreaChangedListeners.iterator();
        while (it.hasNext()) {
            it.next().onPreviewAreaChanged(this.mPreviewArea, new Size(this.mPreviewWidth, this.mPreviewHeight));
        }
    }
}
