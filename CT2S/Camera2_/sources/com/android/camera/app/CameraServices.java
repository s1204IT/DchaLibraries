package com.android.camera.app;

import com.android.camera.remote.RemoteShutterListener;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera.settings.SettingsManager;

public interface CameraServices {
    CaptureSessionManager getCaptureSessionManager();

    @Deprecated
    MediaSaver getMediaSaver();

    MemoryManager getMemoryManager();

    MotionManager getMotionManager();

    RemoteShutterListener getRemoteShutterListener();

    SettingsManager getSettingsManager();
}
