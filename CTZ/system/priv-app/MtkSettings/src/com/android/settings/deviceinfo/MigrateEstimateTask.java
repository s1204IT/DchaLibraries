package com.android.settings.deviceinfo;

import android.app.usage.ExternalStorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telecom.Log;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import java.io.IOException;
import java.util.UUID;

/* loaded from: classes.dex */
public abstract class MigrateEstimateTask extends AsyncTask<Void, Void, Long> {
    private final Context mContext;
    private long mSizeBytes = -1;

    public abstract void onPostExecute(String str, String str2);

    public MigrateEstimateTask(Context context) {
        this.mContext = context;
    }

    public void copyFrom(Intent intent) {
        this.mSizeBytes = intent.getLongExtra("size_bytes", -1L);
    }

    /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.os.AsyncTask
    protected Long doInBackground(Void... voidArr) throws IOException {
        if (this.mSizeBytes != -1) {
            return Long.valueOf(this.mSizeBytes);
        }
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        StorageStatsManager storageStatsManager = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        VolumeInfo volumeInfoFindEmulatedForPrivate = storageManager.findEmulatedForPrivate(this.mContext.getPackageManager().getPrimaryStorageCurrentVolume());
        if (volumeInfoFindEmulatedForPrivate == null) {
            Log.w("StorageSettings", "Failed to find current primary storage", new Object[0]);
            return -1L;
        }
        try {
            UUID uuidForPath = storageManager.getUuidForPath(volumeInfoFindEmulatedForPrivate.getPath());
            Log.d("StorageSettings", "Measuring size of " + uuidForPath, new Object[0]);
            long totalBytes = 0;
            for (UserInfo userInfo : userManager.getUsers()) {
                ExternalStorageStats externalStorageStatsQueryExternalStatsForUser = storageStatsManager.queryExternalStatsForUser(uuidForPath, UserHandle.of(userInfo.id));
                totalBytes += externalStorageStatsQueryExternalStatsForUser.getTotalBytes();
                if (userInfo.id == 0) {
                    totalBytes += externalStorageStatsQueryExternalStatsForUser.getObbBytes();
                }
            }
            return Long.valueOf(totalBytes);
        } catch (IOException e) {
            Log.w("StorageSettings", "Failed to measure", new Object[]{e});
            return -1L;
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
    @Override // android.os.AsyncTask
    protected void onPostExecute(Long l) {
        this.mSizeBytes = l.longValue();
        onPostExecute(Formatter.formatFileSize(this.mContext, this.mSizeBytes), DateUtils.formatDuration(Math.max((this.mSizeBytes * 1000) / 10485760, 1000L)).toString());
    }
}
