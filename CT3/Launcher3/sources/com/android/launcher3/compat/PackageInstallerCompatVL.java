package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.util.SparseArray;
import com.android.launcher3.IconCache;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.util.HashMap;

@TargetApi(21)
public class PackageInstallerCompatVL extends PackageInstallerCompat {
    final PackageInstaller mInstaller;
    final SparseArray<String> mActiveSessions = new SparseArray<>();
    private final PackageInstaller.SessionCallback mCallback = new PackageInstaller.SessionCallback() {
        @Override
        public void onCreated(int sessionId) {
            pushSessionDisplayToLauncher(sessionId);
        }

        @Override
        public void onFinished(int sessionId, boolean success) {
            String packageName = PackageInstallerCompatVL.this.mActiveSessions.get(sessionId);
            PackageInstallerCompatVL.this.mActiveSessions.remove(sessionId);
            if (packageName == null) {
                return;
            }
            PackageInstallerCompatVL.this.sendUpdate(new PackageInstallerCompat.PackageInstallInfo(packageName, success ? 0 : 2, 0));
        }

        @Override
        public void onProgressChanged(int sessionId, float progress) {
            PackageInstaller.SessionInfo session = PackageInstallerCompatVL.this.mInstaller.getSessionInfo(sessionId);
            if (session == null || session.getAppPackageName() == null) {
                return;
            }
            PackageInstallerCompatVL.this.sendUpdate(new PackageInstallerCompat.PackageInstallInfo(session.getAppPackageName(), 1, (int) (session.getProgress() * 100.0f)));
        }

        @Override
        public void onActiveChanged(int sessionId, boolean active) {
        }

        @Override
        public void onBadgingChanged(int sessionId) {
            pushSessionDisplayToLauncher(sessionId);
        }

        private void pushSessionDisplayToLauncher(int sessionId) {
            PackageInstaller.SessionInfo session = PackageInstallerCompatVL.this.mInstaller.getSessionInfo(sessionId);
            if (session == null || session.getAppPackageName() == null) {
                return;
            }
            PackageInstallerCompatVL.this.addSessionInfoToCahce(session, UserHandleCompat.myUserHandle());
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app == null) {
                return;
            }
            app.getModel().updateSessionDisplayInfo(session.getAppPackageName());
        }
    };
    private final IconCache mCache = LauncherAppState.getInstance().getIconCache();
    private final Handler mWorker = new Handler(LauncherModel.getWorkerLooper());

    PackageInstallerCompatVL(Context context) {
        this.mInstaller = context.getPackageManager().getPackageInstaller();
        this.mInstaller.registerSessionCallback(this.mCallback, this.mWorker);
    }

    @Override
    public HashMap<String, Integer> updateAndGetActiveSessionCache() {
        HashMap<String, Integer> activePackages = new HashMap<>();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        for (PackageInstaller.SessionInfo info : this.mInstaller.getAllSessions()) {
            addSessionInfoToCahce(info, user);
            if (info.getAppPackageName() != null) {
                activePackages.put(info.getAppPackageName(), Integer.valueOf((int) (info.getProgress() * 100.0f)));
                this.mActiveSessions.put(info.getSessionId(), info.getAppPackageName());
            }
        }
        return activePackages;
    }

    void addSessionInfoToCahce(PackageInstaller.SessionInfo info, UserHandleCompat user) {
        String packageName = info.getAppPackageName();
        if (packageName == null) {
            return;
        }
        this.mCache.cachePackageInstallInfo(packageName, user, info.getAppIcon(), info.getAppLabel());
    }

    @Override
    public void onStop() {
        this.mInstaller.unregisterSessionCallback(this.mCallback);
    }

    void sendUpdate(PackageInstallerCompat.PackageInstallInfo info) {
        LauncherAppState app = LauncherAppState.getInstanceNoCreate();
        if (app == null) {
            return;
        }
        app.getModel().setPackageState(info);
    }
}
