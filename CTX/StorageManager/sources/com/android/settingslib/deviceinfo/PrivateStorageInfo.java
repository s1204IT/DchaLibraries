package com.android.settingslib.deviceinfo;

import android.app.AppGlobals;
import android.app.usage.StorageStatsManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import java.io.IOException;

public class PrivateStorageInfo {
    public final long freeBytes;
    public final long totalBytes;

    public PrivateStorageInfo(long j, long j2) {
        this.freeBytes = j;
        this.totalBytes = j2;
    }

    public static PrivateStorageInfo getPrivateStorageInfo(StorageVolumeProvider storageVolumeProvider) {
        StorageStatsManager storageStatsManager = (StorageStatsManager) AppGlobals.getInitialApplication().getSystemService(StorageStatsManager.class);
        long freeBytes = 0;
        long totalBytes = 0;
        for (VolumeInfo volumeInfo : storageVolumeProvider.getVolumes()) {
            if (volumeInfo.getType() == 1 && volumeInfo.isMountedReadable()) {
                try {
                    totalBytes += storageVolumeProvider.getTotalBytes(storageStatsManager, volumeInfo);
                    freeBytes += storageVolumeProvider.getFreeBytes(storageStatsManager, volumeInfo);
                } catch (IOException e) {
                    Log.w("PrivateStorageInfo", e);
                }
            }
        }
        return new PrivateStorageInfo(freeBytes, totalBytes);
    }
}
