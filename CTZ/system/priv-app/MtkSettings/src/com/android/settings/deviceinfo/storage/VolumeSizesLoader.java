package com.android.settings.deviceinfo.storage;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.storage.VolumeInfo;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;
import com.android.settingslib.utils.AsyncLoader;
import java.io.IOException;

/* loaded from: classes.dex */
public class VolumeSizesLoader extends AsyncLoader<PrivateStorageInfo> {
    private StorageStatsManager mStats;
    private VolumeInfo mVolume;
    private StorageVolumeProvider mVolumeProvider;

    public VolumeSizesLoader(Context context, StorageVolumeProvider storageVolumeProvider, StorageStatsManager storageStatsManager, VolumeInfo volumeInfo) {
        super(context);
        this.mVolumeProvider = storageVolumeProvider;
        this.mStats = storageStatsManager;
        this.mVolume = volumeInfo;
    }

    /* JADX DEBUG: Method merged with bridge method: onDiscardResult(Ljava/lang/Object;)V */
    @Override // com.android.settingslib.utils.AsyncLoader
    protected void onDiscardResult(PrivateStorageInfo privateStorageInfo) {
    }

    /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
    @Override // android.content.AsyncTaskLoader
    public PrivateStorageInfo loadInBackground() {
        try {
            return getVolumeSize(this.mVolumeProvider, this.mStats, this.mVolume);
        } catch (IOException e) {
            return null;
        }
    }

    static PrivateStorageInfo getVolumeSize(StorageVolumeProvider storageVolumeProvider, StorageStatsManager storageStatsManager, VolumeInfo volumeInfo) throws IOException {
        return new PrivateStorageInfo(storageVolumeProvider.getFreeBytes(storageStatsManager, volumeInfo), storageVolumeProvider.getTotalBytes(storageStatsManager, volumeInfo));
    }
}
