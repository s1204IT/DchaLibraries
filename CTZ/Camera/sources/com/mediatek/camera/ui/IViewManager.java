package com.mediatek.camera.ui;

public interface IViewManager {
    void onCreate();

    void onDestroy();

    void onPause();

    void onResume();

    void setEnabled(boolean z);

    void setVisibility(int i);
}
