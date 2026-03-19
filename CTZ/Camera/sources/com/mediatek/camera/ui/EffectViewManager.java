package com.mediatek.camera.ui;

import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.utils.CameraUtil;

class EffectViewManager extends AbstractViewManager {
    private ViewGroup mEffectViewGroup;
    private View mViewEntry;

    public EffectViewManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
    }

    @Override
    protected View getView() {
        this.mEffectViewGroup = (ViewGroup) this.mParentView.findViewById(R.id.effect);
        return this.mEffectViewGroup;
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mViewEntry != null) {
            this.mViewEntry.setEnabled(z);
        }
    }

    public synchronized void setViewEntry(View view) {
        this.mViewEntry = view;
    }

    public synchronized void attachViewEntry() {
        this.mEffectViewGroup.removeAllViews();
        if (this.mViewEntry != null) {
            CameraUtil.rotateRotateLayoutChildView(this.mApp.getActivity(), this.mViewEntry, this.mApp.getGSensorOrientation(), false);
            this.mEffectViewGroup.addView(this.mViewEntry);
        }
    }
}
