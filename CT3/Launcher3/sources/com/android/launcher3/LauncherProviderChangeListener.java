package com.android.launcher3;

public interface LauncherProviderChangeListener {
    void onAppWidgetHostReset();

    void onLauncherProviderChange();

    void onSettingsChanged(String str, boolean z);
}
