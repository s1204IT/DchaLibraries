package com.android.camera.module;

import com.android.camera.CameraActivity;
import com.android.camera.ShutterButton;
import com.android.camera.app.CameraAppUI;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.settings.SettingsManager;
import com.android.ex.camera2.portability.CameraAgent;

public interface ModuleController extends ShutterButton.OnShutterButtonListener {
    public static final int VISIBILITY_COVERED = 1;
    public static final int VISIBILITY_HIDDEN = 2;
    public static final int VISIBILITY_VISIBLE = 0;

    void destroy();

    CameraAppUI.BottomBarUISpec getBottomBarSpec();

    HardwareSpec getHardwareSpec();

    String getModuleStringIdentifier();

    void hardResetSettings(SettingsManager settingsManager);

    void init(CameraActivity cameraActivity, boolean z, boolean z2);

    boolean isUsingBottomBar();

    boolean onBackPressed();

    void onCameraAvailable(CameraAgent.CameraProxy cameraProxy);

    void onLayoutOrientationChanged(boolean z);

    void onOrientationChanged(int i);

    void onPreviewVisibilityChanged(int i);

    void pause();

    void resume();
}
