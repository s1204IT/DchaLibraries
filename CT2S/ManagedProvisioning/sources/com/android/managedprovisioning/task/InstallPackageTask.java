package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.managedprovisioning.ProvisionLogger;

public class InstallPackageTask {
    private final Callback mCallback;
    private final Context mContext;
    private String mPackageLocation = null;
    private final String mPackageName;
    private int mPackageVerifierEnable;
    private PackageManager mPm;

    public static abstract class Callback {
        public abstract void onError(int i);

        public abstract void onSuccess();
    }

    public InstallPackageTask(Context context, String packageName, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
        this.mPackageName = packageName;
    }

    public void run(String packageLocation) {
        if (TextUtils.isEmpty(packageLocation)) {
            ProvisionLogger.loge("Package Location is empty.");
            this.mCallback.onError(0);
            return;
        }
        this.mPackageLocation = packageLocation;
        IPackageInstallObserver packageInstallObserver = new PackageInstallObserver();
        this.mPm = this.mContext.getPackageManager();
        if (packageContentIsCorrect()) {
            this.mPackageVerifierEnable = Settings.Global.getInt(this.mContext.getContentResolver(), "package_verifier_enable", 1);
            Settings.Global.putInt(this.mContext.getContentResolver(), "package_verifier_enable", 0);
            Uri packageUri = Uri.parse("file://" + this.mPackageLocation);
            this.mPm.installPackage(packageUri, packageInstallObserver, 2, this.mContext.getPackageName());
        }
    }

    private boolean packageContentIsCorrect() {
        PackageInfo pi = this.mPm.getPackageArchiveInfo(this.mPackageLocation, 2);
        if (pi == null) {
            ProvisionLogger.loge("Package could not be parsed successfully.");
            this.mCallback.onError(0);
            return false;
        }
        if (!pi.packageName.equals(this.mPackageName)) {
            ProvisionLogger.loge("Package name in apk (" + pi.packageName + ") does not match package name specified by programmer (" + this.mPackageName + ").");
            this.mCallback.onError(0);
            return false;
        }
        ActivityInfo[] arr$ = pi.receivers;
        for (ActivityInfo ai : arr$) {
            if (!TextUtils.isEmpty(ai.permission) && ai.permission.equals("android.permission.BIND_DEVICE_ADMIN")) {
                return true;
            }
        }
        ProvisionLogger.loge("Installed package has no admin receiver.");
        this.mCallback.onError(0);
        return false;
    }

    private class PackageInstallObserver extends IPackageInstallObserver.Stub {
        private PackageInstallObserver() {
        }

        public void packageInstalled(String packageName, int returnCode) {
            Settings.Global.putInt(InstallPackageTask.this.mContext.getContentResolver(), "package_verifier_enable", InstallPackageTask.this.mPackageVerifierEnable);
            if (returnCode == 1 && InstallPackageTask.this.mPackageName.equals(packageName)) {
                ProvisionLogger.logd("Package " + InstallPackageTask.this.mPackageName + " is succesfully installed.");
                InstallPackageTask.this.mCallback.onSuccess();
            } else if (returnCode == -25) {
                ProvisionLogger.logd("Current version of " + InstallPackageTask.this.mPackageName + " higher than the version to be installed.");
                ProvisionLogger.logd("Package " + InstallPackageTask.this.mPackageName + " was not reinstalled.");
                InstallPackageTask.this.mCallback.onSuccess();
            } else {
                ProvisionLogger.logd("Installing package " + InstallPackageTask.this.mPackageName + " failed.");
                ProvisionLogger.logd("Errorcode returned by IPackageInstallObserver = " + returnCode);
                InstallPackageTask.this.mCallback.onError(1);
            }
        }
    }
}
