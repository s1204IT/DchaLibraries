package com.android.camera.app;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import com.android.camera.MediaSaverImpl;
import com.android.camera.debug.LogHelper;
import com.android.camera.processing.ProcessingServiceManager;
import com.android.camera.remote.RemoteShutterListener;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera.session.CaptureSessionManagerImpl;
import com.android.camera.session.PlaceholderManager;
import com.android.camera.session.SessionStorageManager;
import com.android.camera.session.SessionStorageManagerImpl;
import com.android.camera.settings.SettingsManager;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.RemoteShutterHelper;
import com.android.camera.util.SessionStatsCollector;
import com.android.camera.util.UsageStatistics;

public class CameraApp extends Application implements CameraServices {
    private MediaSaver mMediaSaver;
    private MemoryManagerImpl mMemoryManager;
    private MotionManager mMotionManager;
    private PlaceholderManager mPlaceHolderManager;
    private RemoteShutterListener mRemoteShutterListener;
    private CaptureSessionManager mSessionManager;
    private SessionStorageManager mSessionStorageManager;
    private SettingsManager mSettingsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        LogHelper.initialize(context);
        UsageStatistics.instance().initialize(this);
        SessionStatsCollector.instance().initialize(this);
        CameraUtil.initialize(this);
        ProcessingServiceManager.initSingleton(context);
        this.mMediaSaver = new MediaSaverImpl();
        this.mPlaceHolderManager = new PlaceholderManager(context);
        this.mSessionStorageManager = SessionStorageManagerImpl.create(this);
        this.mSessionManager = new CaptureSessionManagerImpl(this.mMediaSaver, getContentResolver(), this.mPlaceHolderManager, this.mSessionStorageManager);
        this.mMemoryManager = MemoryManagerImpl.create(getApplicationContext(), this.mMediaSaver);
        this.mRemoteShutterListener = RemoteShutterHelper.create(this);
        this.mSettingsManager = new SettingsManager(this);
        clearNotifications();
        this.mMotionManager = new MotionManager(context);
    }

    @Override
    public CaptureSessionManager getCaptureSessionManager() {
        return this.mSessionManager;
    }

    @Override
    public MemoryManager getMemoryManager() {
        return this.mMemoryManager;
    }

    @Override
    public MotionManager getMotionManager() {
        return this.mMotionManager;
    }

    @Override
    @Deprecated
    public MediaSaver getMediaSaver() {
        return this.mMediaSaver;
    }

    @Override
    public RemoteShutterListener getRemoteShutterListener() {
        return this.mRemoteShutterListener;
    }

    @Override
    public SettingsManager getSettingsManager() {
        return this.mSettingsManager;
    }

    private void clearNotifications() {
        NotificationManager manager = (NotificationManager) getSystemService("notification");
        if (manager != null) {
            manager.cancelAll();
        }
    }
}
