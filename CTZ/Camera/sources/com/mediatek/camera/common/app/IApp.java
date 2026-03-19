package com.mediatek.camera.common.app;

import android.app.Activity;
import android.net.Uri;
import android.view.KeyEvent;
import com.mediatek.camera.common.IAppUi;

public interface IApp {

    public interface BackPressedListener {
        boolean onBackPressed();
    }

    public interface KeyEventListener {
        boolean onKeyDown(int i, KeyEvent keyEvent);

        boolean onKeyUp(int i, KeyEvent keyEvent);
    }

    public interface OnOrientationChangeListener {
        void onOrientationChanged(int i);
    }

    void disableGSensorOrientation();

    void enableGSensorOrientation();

    void enableKeepScreenOn(boolean z);

    Activity getActivity();

    IAppUi getAppUi();

    int getGSensorOrientation();

    boolean notifyCameraSelected(String str);

    void notifyNewMedia(Uri uri, boolean z);

    void registerBackPressedListener(BackPressedListener backPressedListener, int i);

    void registerKeyEventListener(KeyEventListener keyEventListener, int i);

    void registerOnOrientationChangeListener(OnOrientationChangeListener onOrientationChangeListener);

    void unRegisterBackPressedListener(BackPressedListener backPressedListener);

    void unRegisterKeyEventListener(KeyEventListener keyEventListener);

    void unregisterOnOrientationChangeListener(OnOrientationChangeListener onOrientationChangeListener);
}
