package com.mediatek.camera.ui;

import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.utils.CameraUtil;
import junit.framework.Assert;

public abstract class AbstractViewManager implements IViewManager {
    protected final IApp mApp;
    private final OnOrientationChangeListenerImpl mOrientationChangeListener;
    protected final ViewGroup mParentView;
    private View mView;

    protected abstract View getView();

    public AbstractViewManager(IApp iApp, ViewGroup viewGroup) {
        Assert.assertNotNull(iApp);
        this.mApp = iApp;
        this.mParentView = viewGroup;
        this.mOrientationChangeListener = new OnOrientationChangeListenerImpl();
    }

    @Override
    public void setVisibility(int i) {
        if (i == 0) {
            show();
        } else if (i == 4) {
            hide(4);
        } else if (i == 8) {
            hide(8);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mView != null) {
            this.mView.setEnabled(z);
            this.mView.setClickable(z);
        }
    }

    public boolean isEnabled() {
        if (this.mView != null) {
            return this.mView.isEnabled();
        }
        return false;
    }

    @Override
    public void onCreate() {
        this.mApp.registerOnOrientationChangeListener(this.mOrientationChangeListener);
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationChangeListener);
    }

    public void updateViewOrientation() {
        CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mView, this.mApp.getGSensorOrientation(), false);
    }

    private void show() {
        if (this.mView == null) {
            this.mView = getView();
        }
        if (this.mView != null) {
            this.mView.setVisibility(0);
            this.mView.setClickable(true);
        }
    }

    private void hide(int i) {
        if (this.mView == null) {
            this.mView = getView();
        }
        if (this.mView != null) {
            this.mView.setVisibility(i);
        }
    }

    private class OnOrientationChangeListenerImpl implements IApp.OnOrientationChangeListener {
        private OnOrientationChangeListenerImpl() {
        }

        @Override
        public void onOrientationChanged(int i) {
            if (AbstractViewManager.this.mView != null) {
                CameraUtil.rotateRotateLayoutChildView(AbstractViewManager.this.mApp.getActivity(), AbstractViewManager.this.mView, i, true);
            }
        }
    }
}
