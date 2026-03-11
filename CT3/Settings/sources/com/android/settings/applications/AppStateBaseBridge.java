package com.android.settings.applications;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.settingslib.applications.ApplicationsState;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;

public abstract class AppStateBaseBridge implements ApplicationsState.Callbacks {
    protected final ApplicationsState.Session mAppSession;
    protected final ApplicationsState mAppState;
    protected final Callback mCallback;
    protected final BackgroundHandler mHandler;
    protected final MainHandler mMainHandler;

    public interface Callback {
        void onExtraInfoUpdated();
    }

    protected abstract void loadAllExtraInfo();

    protected abstract void updateExtraInfo(ApplicationsState.AppEntry appEntry, String str, int i);

    public AppStateBaseBridge(ApplicationsState appState, Callback callback) {
        MainHandler mainHandler = null;
        this.mAppState = appState;
        this.mAppSession = this.mAppState != null ? this.mAppState.newSession(this) : null;
        this.mCallback = callback;
        this.mHandler = new BackgroundHandler(this.mAppState != null ? this.mAppState.getBackgroundLooper() : Looper.getMainLooper());
        this.mMainHandler = new MainHandler(this, mainHandler);
    }

    public void resume() {
        this.mHandler.sendEmptyMessage(1);
        this.mAppSession.resume();
    }

    public void pause() {
        this.mAppSession.pause();
    }

    public void release() {
        this.mAppSession.release();
    }

    public void forceUpdate(String pkg, int uid) {
        this.mHandler.obtainMessage(2, uid, 0, pkg).sendToTarget();
    }

    @Override
    public void onPackageListChanged() {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onLoadEntriesCompleted() {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    private class MainHandler extends Handler {
        MainHandler(AppStateBaseBridge this$0, MainHandler mainHandler) {
            this();
        }

        private MainHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    AppStateBaseBridge.this.mCallback.onExtraInfoUpdated();
                    break;
            }
        }
    }

    private class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    AppStateBaseBridge.this.loadAllExtraInfo();
                    AppStateBaseBridge.this.mMainHandler.sendEmptyMessage(1);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    ArrayList<ApplicationsState.AppEntry> apps = AppStateBaseBridge.this.mAppSession.getAllApps();
                    int N = apps.size();
                    String pkg = (String) msg.obj;
                    int uid = msg.arg1;
                    for (int i = 0; i < N; i++) {
                        ApplicationsState.AppEntry app = apps.get(i);
                        if (app.info.uid == uid && pkg.equals(app.info.packageName)) {
                            AppStateBaseBridge.this.updateExtraInfo(app, pkg, uid);
                        }
                    }
                    AppStateBaseBridge.this.mMainHandler.sendEmptyMessage(1);
                    break;
            }
        }
    }
}
