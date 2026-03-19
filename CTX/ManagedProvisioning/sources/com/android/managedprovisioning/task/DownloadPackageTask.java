package com.android.managedprovisioning.task;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.PackageDownloadInfo;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.io.File;

public class DownloadPackageTask extends AbstractProvisioningTask {
    private boolean mDoneDownloading;
    private long mDownloadId;
    private String mDownloadLocationTo;
    private final DownloadManager mDownloadManager;
    private final PackageDownloadInfo mPackageDownloadInfo;
    private final String mPackageName;
    private BroadcastReceiver mReceiver;
    private final Utils mUtils;

    public DownloadPackageTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(new Utils(), context, provisioningParams, callback);
    }

    @VisibleForTesting
    DownloadPackageTask(Utils utils, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mDownloadManager = (DownloadManager) context.getSystemService("download");
        this.mDownloadManager.setAccessFilename(true);
        this.mPackageName = provisioningParams.inferDeviceAdminPackageName();
        this.mPackageDownloadInfo = (PackageDownloadInfo) Preconditions.checkNotNull(provisioningParams.deviceAdminDownloadInfo);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_download;
    }

    @Override
    public void run(int i) {
        startTaskTimer();
        if (!this.mUtils.packageRequiresUpdate(this.mPackageName, this.mPackageDownloadInfo.minVersion, this.mContext)) {
            success();
            return;
        }
        if (!this.mUtils.isConnectedToNetwork(this.mContext)) {
            ProvisionLogger.loge("DownloadPackageTask: not connected to the network, can't download the package");
            error(1);
            return;
        }
        setDpcDownloadedSetting(this.mContext);
        this.mReceiver = createDownloadReceiver();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"), null, new Handler(Looper.myLooper()));
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(this.mPackageDownloadInfo.location));
        File file = new File(this.mContext.getExternalFilesDir(null) + "/download_cache/managed_provisioning_downloaded_app.apk");
        file.getParentFile().mkdirs();
        request.setDestinationUri(Uri.fromFile(file));
        if (this.mPackageDownloadInfo.cookieHeader != null) {
            request.addRequestHeader("Cookie", this.mPackageDownloadInfo.cookieHeader);
        }
        this.mDownloadId = this.mDownloadManager.enqueue(request);
    }

    private static void setDpcDownloadedSetting(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "managed_provisioning_dpc_downloaded", 1);
    }

    @Override
    protected int getMetricsCategory() {
        return 622;
    }

    private BroadcastReceiver createDownloadReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.DOWNLOAD_COMPLETE".equals(intent.getAction())) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(DownloadPackageTask.this.mDownloadId);
                    Cursor cursorQuery = DownloadPackageTask.this.mDownloadManager.query(query);
                    if (cursorQuery.moveToFirst()) {
                        int columnIndex = cursorQuery.getColumnIndex("status");
                        if (8 == cursorQuery.getInt(columnIndex)) {
                            DownloadPackageTask.this.mDownloadLocationTo = cursorQuery.getString(cursorQuery.getColumnIndex("local_filename"));
                            cursorQuery.close();
                            DownloadPackageTask.this.onDownloadSuccess();
                        } else if (16 == cursorQuery.getInt(columnIndex)) {
                            int columnIndex2 = cursorQuery.getColumnIndex("reason");
                            cursorQuery.close();
                            DownloadPackageTask.this.onDownloadFail(columnIndex2);
                        }
                    }
                }
            }
        };
    }

    private void onDownloadSuccess() {
        if (this.mDoneDownloading) {
            return;
        }
        ProvisionLogger.logd("Downloaded succesfully to: " + this.mDownloadLocationTo);
        this.mDoneDownloading = true;
        stopTaskTimer();
        success();
    }

    public String getDownloadedPackageLocation() {
        return this.mDownloadLocationTo;
    }

    private void onDownloadFail(int i) {
        ProvisionLogger.loge("Downloading package failed.");
        ProvisionLogger.loge("COLUMN_REASON in DownloadManager response has value: " + i);
        error(0);
    }
}
