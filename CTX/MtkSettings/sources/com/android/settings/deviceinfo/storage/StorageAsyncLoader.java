package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.utils.AsyncLoader;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StorageAsyncLoader extends AsyncLoader<SparseArray<AppsStorageResult>> {
    private PackageManagerWrapper mPackageManager;
    private ArraySet<String> mSeenPackages;
    private StorageStatsSource mStatsManager;
    private UserManager mUserManager;
    private String mUuid;

    public static class AppsStorageResult {
        public long cacheSize;
        public StorageStatsSource.ExternalStorageStats externalStats;
        public long gamesSize;
        public long musicAppsSize;
        public long otherAppsSize;
        public long photosAppsSize;
        public long videoAppsSize;
    }

    public interface ResultHandler {
        void handleResult(SparseArray<AppsStorageResult> sparseArray);
    }

    public StorageAsyncLoader(Context context, UserManager userManager, String str, StorageStatsSource storageStatsSource, PackageManagerWrapper packageManagerWrapper) {
        super(context);
        this.mUserManager = userManager;
        this.mUuid = str;
        this.mStatsManager = storageStatsSource;
        this.mPackageManager = packageManagerWrapper;
    }

    @Override
    public SparseArray<AppsStorageResult> loadInBackground() {
        return loadApps();
    }

    private SparseArray<AppsStorageResult> loadApps() {
        this.mSeenPackages = new ArraySet<>();
        SparseArray<AppsStorageResult> sparseArray = new SparseArray<>();
        List users = this.mUserManager.getUsers();
        Collections.sort(users, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo userInfo, UserInfo userInfo2) {
                return Integer.compare(userInfo.id, userInfo2.id);
            }
        });
        int size = users.size();
        for (int i = 0; i < size; i++) {
            UserInfo userInfo = (UserInfo) users.get(i);
            sparseArray.put(userInfo.id, getStorageResultForUser(userInfo.id));
        }
        return sparseArray;
    }

    private AppsStorageResult getStorageResultForUser(int i) {
        Log.d("StorageAsyncLoader", "Loading apps");
        List<ApplicationInfo> installedApplicationsAsUser = this.mPackageManager.getInstalledApplicationsAsUser(0, i);
        AppsStorageResult appsStorageResult = new AppsStorageResult();
        UserHandle userHandleOf = UserHandle.of(i);
        int size = installedApplicationsAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ApplicationInfo applicationInfo = installedApplicationsAsUser.get(i2);
            try {
                StorageStatsSource.AppStorageStats statsForPackage = this.mStatsManager.getStatsForPackage(this.mUuid, applicationInfo.packageName, userHandleOf);
                long dataBytes = statsForPackage.getDataBytes();
                long cacheQuotaBytes = this.mStatsManager.getCacheQuotaBytes(this.mUuid, applicationInfo.uid);
                long cacheBytes = statsForPackage.getCacheBytes();
                if (cacheQuotaBytes < cacheBytes) {
                    dataBytes = (dataBytes - cacheBytes) + cacheQuotaBytes;
                }
                if (!this.mSeenPackages.contains(applicationInfo.packageName)) {
                    dataBytes += statsForPackage.getCodeBytes();
                    this.mSeenPackages.add(applicationInfo.packageName);
                }
                switch (applicationInfo.category) {
                    case 0:
                        appsStorageResult.gamesSize += dataBytes;
                        break;
                    case 1:
                        appsStorageResult.musicAppsSize += dataBytes;
                        break;
                    case 2:
                        appsStorageResult.videoAppsSize += dataBytes;
                        break;
                    case 3:
                        appsStorageResult.photosAppsSize += dataBytes;
                        break;
                    default:
                        if ((applicationInfo.flags & 33554432) == 0) {
                            appsStorageResult.otherAppsSize += dataBytes;
                        } else {
                            appsStorageResult.gamesSize += dataBytes;
                        }
                        break;
                }
            } catch (PackageManager.NameNotFoundException | IOException e) {
                Log.w("StorageAsyncLoader", "App unexpectedly not found", e);
            }
        }
        Log.d("StorageAsyncLoader", "Loading external stats");
        try {
            appsStorageResult.externalStats = this.mStatsManager.getExternalStorageStats(this.mUuid, UserHandle.of(i));
        } catch (IOException e2) {
            Log.w("StorageAsyncLoader", e2);
        }
        Log.d("StorageAsyncLoader", "Obtaining result completed");
        return appsStorageResult;
    }

    @Override
    protected void onDiscardResult(SparseArray<AppsStorageResult> sparseArray) {
    }
}
