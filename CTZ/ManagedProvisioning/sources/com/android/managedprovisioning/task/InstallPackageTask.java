package com.android.managedprovisioning.task;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InstallPackageTask extends AbstractProvisioningTask {
    private static final String ACTION_INSTALL_DONE = InstallPackageTask.class.getName() + ".DONE.";
    private final DownloadPackageTask mDownloadPackageTask;
    private final DevicePolicyManager mDpm;
    private final PackageManager mPm;

    public InstallPackageTask(DownloadPackageTask downloadPackageTask, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mPm = context.getPackageManager();
        this.mDpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        this.mDownloadPackageTask = (DownloadPackageTask) Preconditions.checkNotNull(downloadPackageTask);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_install;
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[16384];
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                outputStream.write(bArr, 0, i);
            } else {
                return;
            }
        }
    }

    @Override
    public void run(int i) {
        int iCreateSession;
        PackageInstaller.Session sessionOpenSession;
        FileInputStream fileInputStream;
        Throwable th;
        Throwable th2;
        startTaskTimer();
        String downloadedPackageLocation = this.mDownloadPackageTask.getDownloadedPackageLocation();
        String strInferDeviceAdminPackageName = this.mProvisioningParams.inferDeviceAdminPackageName();
        ProvisionLogger.logi("Installing package " + strInferDeviceAdminPackageName);
        if (TextUtils.isEmpty(downloadedPackageLocation)) {
            success();
            return;
        }
        int i2 = this.mDpm.isDeviceOwnerApp(strInferDeviceAdminPackageName) ? 6 : 2;
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.installFlags = i2;
        File file = new File(downloadedPackageLocation);
        PackageInstaller packageInstaller = this.mPm.getPackageInstaller();
        try {
            try {
                iCreateSession = packageInstaller.createSession(sessionParams);
                sessionOpenSession = packageInstaller.openSession(iCreateSession);
                try {
                    try {
                        fileInputStream = new FileInputStream(file);
                    } catch (IOException e) {
                        sessionOpenSession.abandon();
                        throw e;
                    }
                } finally {
                    if (sessionOpenSession != null) {
                        $closeResource(null, sessionOpenSession);
                    }
                }
            } finally {
                file.delete();
            }
        } catch (IOException e2) {
            ProvisionLogger.loge("Installing package " + strInferDeviceAdminPackageName + " failed.", e2);
            error(1);
        }
        try {
            OutputStream outputStreamOpenWrite = sessionOpenSession.openWrite(file.getName(), 0L, -1L);
            try {
                copyStream(fileInputStream, outputStreamOpenWrite);
                if (outputStreamOpenWrite != null) {
                    $closeResource(null, outputStreamOpenWrite);
                }
                $closeResource(null, fileInputStream);
                String str = ACTION_INSTALL_DONE + iCreateSession;
                this.mContext.registerReceiver(new PackageInstallReceiver(strInferDeviceAdminPackageName), new IntentFilter(str));
                sessionOpenSession.commit(PendingIntent.getBroadcast(this.mContext, iCreateSession, new Intent(str), 1207959552).getIntentSender());
            } catch (Throwable th3) {
                th = th3;
                th2 = null;
                if (outputStreamOpenWrite != null) {
                }
            }
        } catch (Throwable th4) {
            th = th4;
            th = null;
            $closeResource(th, fileInputStream);
            throw th;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 623;
    }

    private class PackageInstallReceiver extends BroadcastReceiver {
        private final String mPackageName;

        public PackageInstallReceiver(String str) {
            this.mPackageName = str;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null || !intent.getAction().startsWith(InstallPackageTask.ACTION_INSTALL_DONE)) {
                ProvisionLogger.logw("Incorrect action");
                InstallPackageTask.this.error(1);
                return;
            }
            if (!intent.getStringExtra("android.content.pm.extra.PACKAGE_NAME").equals(this.mPackageName)) {
                ProvisionLogger.loge("Package doesn't have expected package name.");
                InstallPackageTask.this.error(0);
                return;
            }
            int intExtra = intent.getIntExtra("android.content.pm.extra.STATUS", 0);
            String stringExtra = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
            int intExtra2 = intent.getIntExtra("android.content.pm.extra.LEGACY_STATUS", 0);
            InstallPackageTask.this.mContext.unregisterReceiver(this);
            ProvisionLogger.logi(intExtra + " " + intExtra2 + " " + stringExtra);
            if (intExtra == 0) {
                ProvisionLogger.logd("Package " + this.mPackageName + " is succesfully installed.");
                InstallPackageTask.this.stopTaskTimer();
                InstallPackageTask.this.success();
                return;
            }
            if (intExtra2 == -25) {
                ProvisionLogger.logd("Current version of " + this.mPackageName + " higher than the version to be installed. It was not reinstalled.");
                InstallPackageTask.this.success();
                return;
            }
            ProvisionLogger.logd("Installing package " + this.mPackageName + " failed.");
            StringBuilder sb = new StringBuilder();
            sb.append("Status message returned  = ");
            sb.append(stringExtra);
            ProvisionLogger.logd(sb.toString());
            InstallPackageTask.this.error(1);
        }
    }
}
