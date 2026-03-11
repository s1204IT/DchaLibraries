package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.webkit.IWebViewUpdateService;
import com.android.settings.R;
import java.util.List;

public class ResetAppsHelper implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private final AppOpsManager mAom;
    private final Context mContext;
    private final NetworkPolicyManager mNpm;
    private final PackageManager mPm;
    private AlertDialog mResetDialog;
    private final IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private final INotificationManager mNm = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
    private final IWebViewUpdateService mWvus = IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));

    public ResetAppsHelper(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mNpm = NetworkPolicyManager.from(context);
        this.mAom = (AppOpsManager) context.getSystemService("appops");
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null || !savedInstanceState.getBoolean("resetDialog")) {
            return;
        }
        buildResetDialog();
    }

    public void onSaveInstanceState(Bundle outState) {
        if (this.mResetDialog == null) {
            return;
        }
        outState.putBoolean("resetDialog", true);
    }

    public void stop() {
        if (this.mResetDialog == null) {
            return;
        }
        this.mResetDialog.dismiss();
        this.mResetDialog = null;
    }

    void buildResetDialog() {
        if (this.mResetDialog == null) {
            this.mResetDialog = new AlertDialog.Builder(this.mContext).setTitle(R.string.reset_app_preferences_title).setMessage(R.string.reset_app_preferences_desc).setPositiveButton(R.string.reset_app_preferences_button, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setOnDismissListener(this).show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.mResetDialog != dialog) {
            return;
        }
        this.mResetDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mResetDialog != dialog) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> apps = ResetAppsHelper.this.mPm.getInstalledApplications(512);
                for (int i = 0; i < apps.size(); i++) {
                    ApplicationInfo app = apps.get(i);
                    try {
                        ResetAppsHelper.this.mNm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                    } catch (RemoteException e) {
                    }
                    if (!app.enabled && ResetAppsHelper.this.mPm.getApplicationEnabledSetting(app.packageName) == 3 && !ResetAppsHelper.this.isNonEnableableFallback(app.packageName)) {
                        ResetAppsHelper.this.mPm.setApplicationEnabledSetting(app.packageName, 0, 1);
                    }
                }
                try {
                    ResetAppsHelper.this.mIPm.resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e2) {
                }
                ResetAppsHelper.this.mAom.resetAllModes();
                int[] restrictedUids = ResetAppsHelper.this.mNpm.getUidsWithPolicy(1);
                int currentUserId = ActivityManager.getCurrentUser();
                for (int uid : restrictedUids) {
                    if (UserHandle.getUserId(uid) == currentUserId) {
                        ResetAppsHelper.this.mNpm.setUidPolicy(uid, 0);
                    }
                }
            }
        });
    }

    public boolean isNonEnableableFallback(String packageName) {
        try {
            return this.mWvus.isFallbackPackage(packageName);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
