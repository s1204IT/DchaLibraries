package com.android.launcher3.compat;

import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.launcher3.IconCache;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PackageInstallerCompatVL extends PackageInstallerCompat {
    private static final boolean DEBUG = false;
    private final Context mAppContext;
    private final IconCache mCache;
    final PackageInstaller mInstaller;
    final SparseArray<String> mActiveSessions = new SparseArray<>();
    private final HashMap<String, Boolean> mSessionVerifiedMap = new HashMap<>();
    private final PackageInstaller.SessionCallback mCallback = new PackageInstaller.SessionCallback() {
        @Override
        public void onCreated(int i) {
            pushSessionDisplayToLauncher(i);
        }

        @Override
        public void onFinished(int i, boolean z) {
            String str = PackageInstallerCompatVL.this.mActiveSessions.get(i);
            PackageInstallerCompatVL.this.mActiveSessions.remove(i);
            if (str != null) {
                PackageInstallerCompatVL.this.sendUpdate(PackageInstallerCompat.PackageInstallInfo.fromState(z ? 0 : 2, str));
            }
        }

        @Override
        public void onProgressChanged(int i, float f) {
            PackageInstaller.SessionInfo sessionInfoVerify = PackageInstallerCompatVL.this.verify(PackageInstallerCompatVL.this.mInstaller.getSessionInfo(i));
            if (sessionInfoVerify != null && sessionInfoVerify.getAppPackageName() != null) {
                PackageInstallerCompatVL.this.sendUpdate(PackageInstallerCompat.PackageInstallInfo.fromInstallingState(sessionInfoVerify));
            }
        }

        @Override
        public void onActiveChanged(int i, boolean z) {
        }

        @Override
        public void onBadgingChanged(int i) {
            pushSessionDisplayToLauncher(i);
        }

        private PackageInstaller.SessionInfo pushSessionDisplayToLauncher(int i) {
            PackageInstaller.SessionInfo sessionInfoVerify = PackageInstallerCompatVL.this.verify(PackageInstallerCompatVL.this.mInstaller.getSessionInfo(i));
            if (sessionInfoVerify != null && sessionInfoVerify.getAppPackageName() != null) {
                PackageInstallerCompatVL.this.mActiveSessions.put(i, sessionInfoVerify.getAppPackageName());
                PackageInstallerCompatVL.this.addSessionInfoToCache(sessionInfoVerify, Process.myUserHandle());
                LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
                if (instanceNoCreate != null) {
                    instanceNoCreate.getModel().updateSessionDisplayInfo(sessionInfoVerify.getAppPackageName());
                }
                return sessionInfoVerify;
            }
            return null;
        }
    };
    private final Handler mWorker = new Handler(LauncherModel.getWorkerLooper());

    PackageInstallerCompatVL(Context context) {
        this.mAppContext = context.getApplicationContext();
        this.mInstaller = context.getPackageManager().getPackageInstaller();
        this.mCache = LauncherAppState.getInstance(context).getIconCache();
        this.mInstaller.registerSessionCallback(this.mCallback, this.mWorker);
    }

    @Override
    public HashMap<String, PackageInstaller.SessionInfo> updateAndGetActiveSessionCache() {
        HashMap<String, PackageInstaller.SessionInfo> map = new HashMap<>();
        UserHandle userHandleMyUserHandle = Process.myUserHandle();
        for (PackageInstaller.SessionInfo sessionInfo : getAllVerifiedSessions()) {
            addSessionInfoToCache(sessionInfo, userHandleMyUserHandle);
            if (sessionInfo.getAppPackageName() != null) {
                map.put(sessionInfo.getAppPackageName(), sessionInfo);
                this.mActiveSessions.put(sessionInfo.getSessionId(), sessionInfo.getAppPackageName());
            }
        }
        return map;
    }

    void addSessionInfoToCache(PackageInstaller.SessionInfo sessionInfo, UserHandle userHandle) {
        String appPackageName = sessionInfo.getAppPackageName();
        if (appPackageName != null) {
            this.mCache.cachePackageInstallInfo(appPackageName, userHandle, sessionInfo.getAppIcon(), sessionInfo.getAppLabel());
        }
    }

    @Override
    public void onStop() {
        this.mInstaller.unregisterSessionCallback(this.mCallback);
    }

    void sendUpdate(PackageInstallerCompat.PackageInstallInfo packageInstallInfo) {
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if (instanceNoCreate != null) {
            instanceNoCreate.getModel().setPackageState(packageInstallInfo);
        }
    }

    public PackageInstaller.SessionInfo verify(PackageInstaller.SessionInfo sessionInfo) {
        if (sessionInfo == null || sessionInfo.getInstallerPackageName() == null || TextUtils.isEmpty(sessionInfo.getAppPackageName())) {
            return null;
        }
        String installerPackageName = sessionInfo.getInstallerPackageName();
        synchronized (this.mSessionVerifiedMap) {
            if (!this.mSessionVerifiedMap.containsKey(installerPackageName)) {
                boolean z = true;
                if (LauncherAppsCompat.getInstance(this.mAppContext).getApplicationInfo(installerPackageName, 1, Process.myUserHandle()) == null) {
                    z = false;
                }
                this.mSessionVerifiedMap.put(installerPackageName, Boolean.valueOf(z));
            }
        }
        if (this.mSessionVerifiedMap.get(installerPackageName).booleanValue()) {
            return sessionInfo;
        }
        return null;
    }

    @Override
    public List<PackageInstaller.SessionInfo> getAllVerifiedSessions() {
        ArrayList arrayList = new ArrayList(this.mInstaller.getAllSessions());
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            if (verify((PackageInstaller.SessionInfo) it.next()) == null) {
                it.remove();
            }
        }
        return arrayList;
    }
}
