package com.android.server.storage;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AppCollector {
    private static String TAG = "AppCollector";
    private final BackgroundHandler mBackgroundHandler;
    private CompletableFuture<List<PackageStats>> mStats;

    public AppCollector(Context context, VolumeInfo volumeInfo) {
        Preconditions.checkNotNull(volumeInfo);
        this.mBackgroundHandler = new BackgroundHandler(BackgroundThread.get().getLooper(), volumeInfo, context.getPackageManager(), (UserManager) context.getSystemService("user"), (StorageStatsManager) context.getSystemService("storagestats"));
    }

    public List<PackageStats> getPackageStats(long j) {
        synchronized (this) {
            if (this.mStats == null) {
                this.mStats = new CompletableFuture<>();
                this.mBackgroundHandler.sendEmptyMessage(0);
            }
        }
        try {
            return this.mStats.get(j, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "An exception occurred while getting app storage", e);
            return null;
        } catch (TimeoutException e2) {
            Log.e(TAG, "AppCollector timed out");
            return null;
        }
    }

    private class BackgroundHandler extends Handler {
        static final int MSG_START_LOADING_SIZES = 0;
        private final PackageManager mPm;
        private final StorageStatsManager mStorageStatsManager;
        private final UserManager mUm;
        private final VolumeInfo mVolume;

        BackgroundHandler(Looper looper, VolumeInfo volumeInfo, PackageManager packageManager, UserManager userManager, StorageStatsManager storageStatsManager) {
            super(looper);
            this.mVolume = volumeInfo;
            this.mPm = packageManager;
            this.mUm = userManager;
            this.mStorageStatsManager = storageStatsManager;
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                ArrayList arrayList = new ArrayList();
                List users = this.mUm.getUsers();
                int size = users.size();
                for (int i = 0; i < size; i++) {
                    UserInfo userInfo = (UserInfo) users.get(i);
                    List installedApplicationsAsUser = this.mPm.getInstalledApplicationsAsUser(512, userInfo.id);
                    int size2 = installedApplicationsAsUser.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        ApplicationInfo applicationInfo = (ApplicationInfo) installedApplicationsAsUser.get(i2);
                        if (Objects.equals(applicationInfo.volumeUuid, this.mVolume.getFsUuid())) {
                            try {
                                StorageStats storageStatsQueryStatsForPackage = this.mStorageStatsManager.queryStatsForPackage(applicationInfo.storageUuid, applicationInfo.packageName, userInfo.getUserHandle());
                                PackageStats packageStats = new PackageStats(applicationInfo.packageName, userInfo.id);
                                packageStats.cacheSize = storageStatsQueryStatsForPackage.getCacheBytes();
                                packageStats.codeSize = storageStatsQueryStatsForPackage.getAppBytes();
                                packageStats.dataSize = storageStatsQueryStatsForPackage.getDataBytes();
                                arrayList.add(packageStats);
                            } catch (PackageManager.NameNotFoundException | IOException e) {
                                Log.e(AppCollector.TAG, "An exception occurred while fetching app size", e);
                            }
                        }
                    }
                }
                AppCollector.this.mStats.complete(arrayList);
            }
        }
    }
}
