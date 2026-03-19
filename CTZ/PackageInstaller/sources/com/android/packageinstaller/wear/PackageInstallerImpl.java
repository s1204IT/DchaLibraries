package com.android.packageinstaller.wear;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class PackageInstallerImpl {
    private final Context mContext;
    private final Map<String, PackageInstaller.Session> mOpenSessionMap;
    private final PackageInstaller mPackageInstaller;
    private final Map<String, PackageInstaller.SessionInfo> mSessionInfoMap = new HashMap();

    public interface InstallListener {
        void installBeginning();

        void installFailed(int i, String str);

        void installSucceeded();
    }

    public PackageInstallerImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPackageInstaller = this.mContext.getPackageManager().getPackageInstaller();
        List<PackageInstaller.SessionInfo> mySessions = this.mPackageInstaller.getMySessions();
        for (int i = 0; i < mySessions.size(); i++) {
            PackageInstaller.SessionInfo sessionInfo = mySessions.get(i);
            String appPackageName = sessionInfo.getAppPackageName();
            PackageInstaller.SessionInfo sessionInfoPut = this.mSessionInfoMap.put(appPackageName, sessionInfo);
            if (sessionInfoPut != null) {
                Log.w("PackageInstallerImpl", "Multiple sessions for " + appPackageName + " found. Removing " + sessionInfoPut.getSessionId() + " & keeping " + mySessions.get(i).getSessionId());
            }
        }
        this.mOpenSessionMap = new HashMap();
    }

    public void install(String str, ParcelFileDescriptor parcelFileDescriptor, InstallListener installListener) throws Throwable {
        PackageInstaller.Session sessionOpenSession = null;
        try {
            PackageInstaller.SessionInfo sessionInfo = this.mSessionInfoMap.get(str);
            if (sessionInfo != null) {
                sessionOpenSession = getSession(str);
            }
            if (sessionOpenSession == null) {
                try {
                    innerCreateSession(str);
                    sessionInfo = this.mSessionInfoMap.get(str);
                    try {
                        sessionOpenSession = this.mPackageInstaller.openSession(sessionInfo.getSessionId());
                        this.mOpenSessionMap.put(str, sessionOpenSession);
                    } catch (SecurityException e) {
                        Log.e("PackageInstallerImpl", "Can't open session for " + str + ": " + e.getMessage());
                        installListener.installFailed(-613, "Can't open session");
                        this.mSessionInfoMap.remove(str);
                        return;
                    }
                } catch (IOException e2) {
                    Log.e("PackageInstallerImpl", "Can't create session for " + str + ": " + e2.getMessage());
                    installListener.installFailed(-612, "Could not create session");
                    this.mSessionInfoMap.remove(str);
                    return;
                }
            }
            PackageInstaller.SessionInfo sessionInfo2 = sessionInfo;
            InstallTask installTask = new InstallTask(this.mContext, str, parcelFileDescriptor, installListener, sessionOpenSession, getCommitCallback(str, sessionInfo2.getSessionId(), installListener));
            installTask.execute();
            if (installTask.isError()) {
                cancelSession(sessionInfo2.getSessionId(), str);
            }
        } catch (Exception e3) {
            Log.e("PackageInstallerImpl", "Unexpected exception while installing: " + str + ": " + e3.getMessage());
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected exception while installing ");
            sb.append(str);
            installListener.installFailed(-616, sb.toString());
        }
    }

    private PackageInstaller.Session getSession(String str) {
        PackageInstaller.Session session = this.mOpenSessionMap.get(str);
        if (session != null) {
            try {
                session.getNames();
                return session;
            } catch (IOException e) {
                Log.e("PackageInstallerImpl", "Stale open session for " + str + ": " + e.getMessage());
                this.mOpenSessionMap.remove(str);
            } catch (SecurityException e2) {
                Log.e("PackageInstallerImpl", "Stale open session for " + str + ": " + e2.getMessage());
                this.mOpenSessionMap.remove(str);
            }
        }
        PackageInstaller.SessionInfo sessionInfo = this.mSessionInfoMap.get(str);
        if (sessionInfo == null) {
            return null;
        }
        try {
            PackageInstaller.Session sessionOpenSession = this.mPackageInstaller.openSession(sessionInfo.getSessionId());
            this.mOpenSessionMap.put(str, sessionOpenSession);
            return sessionOpenSession;
        } catch (IOException e3) {
            Log.w("PackageInstallerImpl", "IOException opening old session for " + e3.getMessage() + " - deleting info");
            this.mSessionInfoMap.remove(str);
            return null;
        } catch (SecurityException e4) {
            Log.w("PackageInstallerImpl", "SessionInfo was stale for " + str + " - deleting info");
            this.mSessionInfoMap.remove(str);
            return null;
        }
    }

    private void innerCreateSession(String str) throws IOException {
        if (this.mSessionInfoMap.containsKey(str)) {
            Log.w("PackageInstallerImpl", "Creating session for " + str + " when one already exists");
            return;
        }
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.setAppPackageName(str);
        this.mSessionInfoMap.put(str, this.mPackageInstaller.getSessionInfo(this.mPackageInstaller.createSession(sessionParams)));
    }

    private void cancelSession(int i, String str) {
        closeSession(str);
        this.mSessionInfoMap.remove(str);
        try {
            this.mPackageInstaller.abandonSession(i);
        } catch (SecurityException e) {
        }
    }

    private void closeSession(String str) {
        PackageInstaller.Session sessionRemove = this.mOpenSessionMap.remove(str);
        if (sessionRemove != null) {
            try {
                sessionRemove.close();
            } catch (Exception e) {
                Log.w("PackageInstallerImpl", "Unexpected error closing session for " + str + ": " + e.getMessage());
            }
        }
    }

    private IntentSender getCommitCallback(final String str, final int i, final InstallListener installListener) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PackageInstallerImpl.this.mContext.unregisterReceiver(this);
                PackageInstallerImpl.this.handleCommitCallback(intent, str, i, installListener);
            }
        };
        String str2 = "com.android.vending.INTENT_PACKAGE_INSTALL_COMMIT." + str;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(str2);
        this.mContext.registerReceiver(broadcastReceiver, intentFilter);
        return PendingIntent.getBroadcast(this.mContext, str.hashCode(), new Intent(str2), 1207959552).getIntentSender();
    }

    private void handleCommitCallback(Intent intent, String str, int i, InstallListener installListener) {
        if (Log.isLoggable("PackageInstallerImpl", 3)) {
            Log.d("PackageInstallerImpl", "Installation of " + str + " finished with extras " + intent.getExtras());
        }
        String stringExtra = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
        int intExtra = intent.getIntExtra("android.content.pm.extra.STATUS", Integer.MIN_VALUE);
        if (intExtra == 0) {
            cancelSession(i, str);
            installListener.installSucceeded();
            return;
        }
        if (intExtra == -1) {
            cancelSession(i, str);
            installListener.installFailed(-617, "Unexpected: user action required");
            return;
        }
        cancelSession(i, str);
        int packageManagerErrorCode = getPackageManagerErrorCode(intExtra);
        Log.e("PackageInstallerImpl", "Error " + packageManagerErrorCode + " while installing " + str + ": " + stringExtra);
        installListener.installFailed(packageManagerErrorCode, null);
    }

    private int getPackageManagerErrorCode(int i) {
        if (i == Integer.MIN_VALUE) {
            return -618;
        }
        return (-500) - i;
    }
}
