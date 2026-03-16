package com.android.camera.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import com.android.camera.ButtonManager;
import com.android.camera.SoundPlayer;
import com.android.camera.module.ModuleController;
import com.android.camera.one.OneCameraManager;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.AbstractTutorialOverlay;
import com.android.camera.ui.PreviewStatusListener;

public interface AppController {

    public interface ShutterEventsListener {
        void onShutterClicked();

        void onShutterLongPressed();

        void onShutterPressed();

        void onShutterReleased();
    }

    void addPreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener previewAreaChangedListener);

    void cancelPostCaptureAnimation();

    void cancelPreCaptureAnimation();

    void enableKeepScreenOn(boolean z);

    void freezeScreenUntilPreviewReady();

    Context getAndroidContext();

    ButtonManager getButtonManager();

    CameraAppUI getCameraAppUI();

    OneCameraManager getCameraManager();

    CameraProvider getCameraProvider();

    String getCameraScope();

    int getCurrentCameraId();

    ModuleController getCurrentModuleController();

    int getCurrentModuleIndex();

    RectF getFullscreenRect();

    LocationManager getLocationManager();

    FrameLayout getModuleLayoutRoot();

    ModuleManager getModuleManager();

    String getModuleScope();

    OrientationManager getOrientationManager();

    int getPreferredChildModeIndex(int i);

    SurfaceTexture getPreviewBuffer();

    int getQuickSwitchToModuleId(int i);

    CameraServices getServices();

    SettingsManager getSettingsManager();

    SoundPlayer getSoundPlayer();

    boolean isAutoRotateScreen();

    boolean isPaused();

    boolean isShutterEnabled();

    void launchActivityByIntent(Intent intent);

    void lockOrientation();

    void notifyNewMedia(Uri uri);

    void onModeSelected(int i);

    void onPreviewReadyToStart();

    void onPreviewStarted();

    void onSettingsSelected();

    void openContextMenu(View view);

    void registerForContextMenu(View view);

    void removePreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener previewAreaChangedListener);

    void setPreviewStatusListener(PreviewStatusListener previewStatusListener);

    void setShutterEnabled(boolean z);

    void setShutterEventsListener(ShutterEventsListener shutterEventsListener);

    void setupOneShotPreviewListener();

    void showErrorAndFinish(int i);

    void showTutorial(AbstractTutorialOverlay abstractTutorialOverlay);

    void startPostCaptureAnimation();

    void startPostCaptureAnimation(Bitmap bitmap);

    void startPreCaptureAnimation();

    void startPreCaptureAnimation(boolean z);

    void unlockOrientation();

    void updatePreviewAspectRatio(float f);

    void updatePreviewTransform(Matrix matrix);

    void updatePreviewTransformFullscreen(Matrix matrix, float f);
}
