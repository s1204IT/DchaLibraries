package com.android.camera.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import com.android.camera.app.CameraApp;

public class ManagedSwitchPreference extends SwitchPreference {
    public ManagedSwitchPreference(Context context) {
        super(context);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        SettingsManager settingsManager;
        CameraApp cameraApp = getCameraApp();
        if (cameraApp != null && (settingsManager = cameraApp.getSettingsManager()) != null) {
            return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, getKey());
        }
        return defaultReturnValue;
    }

    @Override
    public boolean persistBoolean(boolean value) {
        SettingsManager settingsManager;
        CameraApp cameraApp = getCameraApp();
        if (cameraApp == null || (settingsManager = cameraApp.getSettingsManager()) == null) {
            return false;
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, getKey(), value);
        return true;
    }

    private CameraApp getCameraApp() {
        Context context = getContext();
        if (context instanceof Activity) {
            Application application = ((Activity) context).getApplication();
            if (application instanceof CameraApp) {
                return (CameraApp) application;
            }
        }
        return null;
    }
}
