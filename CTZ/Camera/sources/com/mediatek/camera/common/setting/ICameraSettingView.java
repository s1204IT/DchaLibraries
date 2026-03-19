package com.mediatek.camera.common.setting;

import android.preference.PreferenceFragment;

public interface ICameraSettingView {
    boolean isEnabled();

    void loadView(PreferenceFragment preferenceFragment);

    void refreshView();

    void unloadView();
}
