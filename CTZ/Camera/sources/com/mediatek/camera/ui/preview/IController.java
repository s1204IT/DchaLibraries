package com.mediatek.camera.ui.preview;

import android.view.View;
import com.mediatek.camera.common.IAppUiListener;

interface IController {
    void clearPreviewStatusListener(IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener);

    void onPause();

    void removeTopSurface();

    void setEnabled(boolean z);

    void setOnLayoutChangeListener(View.OnLayoutChangeListener onLayoutChangeListener);

    void setOnTouchListener(View.OnTouchListener onTouchListener);

    void updatePreviewSize(int i, int i2, IAppUiListener.ISurfaceStatusListener iSurfaceStatusListener);
}
