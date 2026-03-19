package com.android.settings.applications.manageapplications;

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

    public void onRestoreInstanceState(Bundle bundle) {
        if (bundle != null && bundle.getBoolean("resetDialog")) {
            buildResetDialog();
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        if (this.mResetDialog != null) {
            bundle.putBoolean("resetDialog", true);
        }
    }

    public void stop() {
        if (this.mResetDialog != null) {
            this.mResetDialog.dismiss();
            this.mResetDialog = null;
        }
    }

    void buildResetDialog() {
        if (this.mResetDialog == null) {
            this.mResetDialog = new AlertDialog.Builder(this.mContext).setTitle(R.string.reset_app_preferences_title).setMessage(R.string.reset_app_preferences_desc).setPositiveButton(R.string.reset_app_preferences_button, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setOnDismissListener(this).show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (this.mResetDialog == dialogInterface) {
            this.mResetDialog = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mResetDialog != dialogInterface) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> installedApplications = ResetAppsHelper.this.mPm.getInstalledApplications(512);
                for (int i2 = 0; i2 < installedApplications.size(); i2++) {
                    ApplicationInfo applicationInfo = installedApplications.get(i2);
                    try {
                        ResetAppsHelper.this.mNm.setNotificationsEnabledForPackage(applicationInfo.packageName, applicationInfo.uid, true);
                    } catch (RemoteException e) {
                    }
                    if (!applicationInfo.enabled && ResetAppsHelper.this.mPm.getApplicationEnabledSetting(applicationInfo.packageName) == 3 && !ResetAppsHelper.this.isNonEnableableFallback(applicationInfo.packageName)) {
                        ResetAppsHelper.this.mPm.setApplicationEnabledSetting(applicationInfo.packageName, 0, 1);
                    }
                }
                try {
                    ResetAppsHelper.this.mIPm.resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e2) {
                }
                ResetAppsHelper.this.mAom.resetAllModes();
                int[] uidsWithPolicy = ResetAppsHelper.this.mNpm.getUidsWithPolicy(1);
                int currentUser = ActivityManager.getCurrentUser();
                for (int i3 : uidsWithPolicy) {
                    if (UserHandle.getUserId(i3) == currentUser) {
                        ResetAppsHelper.this.mNpm.setUidPolicy(i3, 0);
                    }
                }
            }
        });
    }

    private boolean isNonEnableableFallback(String str) {
        try {
            return this.mWvus.isFallbackPackage(str);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
