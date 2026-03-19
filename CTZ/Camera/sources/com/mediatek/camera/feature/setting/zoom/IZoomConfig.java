package com.mediatek.camera.feature.setting.zoom;

public interface IZoomConfig {

    public interface OnZoomLevelUpdateListener {
        String onGetOverrideValue();

        void onZoomLevelUpdate(String str);
    }

    void onScalePerformed(double d);

    void onScaleStatus(boolean z);
}
