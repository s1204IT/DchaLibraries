package com.android.camera.data;

import android.view.View;
import com.android.camera2.R;

public class CameraPreviewData extends SimpleViewData {
    private boolean mPreviewLocked;

    public CameraPreviewData(View v, int width, int height) {
        super(v, LocalDataViewType.CAMERA_PREVIEW, width, height, -1, -1);
        v.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(LocalDataViewType.CAMERA_PREVIEW.ordinal()));
        this.mPreviewLocked = true;
    }

    @Override
    public int getViewType() {
        return 1;
    }

    @Override
    public int getLocalDataType() {
        return 1;
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return !this.mPreviewLocked;
    }

    public void lockPreview(boolean lock) {
        this.mPreviewLocked = lock;
    }
}
