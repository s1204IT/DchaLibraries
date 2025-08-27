package com.android.settings.applications;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

/* loaded from: classes.dex */
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

    public AppStateBaseBridge(ApplicationsState applicationsState, Callback callback) {
        this.mAppState = applicationsState;
        this.mAppSession = this.mAppState != null ? this.mAppState.newSession(this) : null;
        this.mCallback = callback;
        this.mHandler = new BackgroundHandler(this.mAppState != null ? this.mAppState.getBackgroundLooper() : Looper.getMainLooper());
        this.mMainHandler = new MainHandler();
    }

    public void resume() {
        this.mHandler.sendEmptyMessage(1);
        this.mAppSession.onResume();
    }

    public void pause() {
        this.mAppSession.onPause();
    }

    public void release() {
        this.mAppSession.onDestroy();
    }

    public void forceUpdate(String str, int i) {
        this.mHandler.obtainMessage(2, i, 0, str).sendToTarget();
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageListChanged() {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLoadEntriesCompleted() {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRunningStateChanged(boolean z) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageIconChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String str) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onAllSizesComputed() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLauncherInfoChanged() {
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 1) {
                AppStateBaseBridge.this.mCallback.onExtraInfoUpdated();
            }
        }
    }

    private class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AppStateBaseBridge.this.loadAllExtraInfo();
                    AppStateBaseBridge.this.mMainHandler.sendEmptyMessage(1);
                    break;
                case 2:
                    ArrayList<ApplicationsState.AppEntry> allApps = AppStateBaseBridge.this.mAppSession.getAllApps();
                    int size = allApps.size();
                    String str = (String) message.obj;
                    int i = message.arg1;
                    for (int i2 = 0; i2 < size; i2++) {
                        ApplicationsState.AppEntry appEntry = allApps.get(i2);
                        if (appEntry.info.uid == i && str.equals(appEntry.info.packageName)) {
                            AppStateBaseBridge.this.updateExtraInfo(appEntry, str, i);
                        }
                    }
                    AppStateBaseBridge.this.mMainHandler.sendEmptyMessage(1);
                    break;
            }
        }
    }
}
