package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.utils.AsyncLoader;
import java.io.IOException;

/* loaded from: classes.dex */
public class FetchPackageStorageAsyncLoader extends AsyncLoader<StorageStatsSource.AppStorageStats> {
    private final ApplicationInfo mInfo;
    private final StorageStatsSource mSource;
    private final UserHandle mUser;

    public FetchPackageStorageAsyncLoader(Context context, StorageStatsSource storageStatsSource, ApplicationInfo applicationInfo, UserHandle userHandle) {
        super(context);
        this.mSource = (StorageStatsSource) Preconditions.checkNotNull(storageStatsSource);
        this.mInfo = applicationInfo;
        this.mUser = userHandle;
    }

    /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
    @Override // android.content.AsyncTaskLoader
    public StorageStatsSource.AppStorageStats loadInBackground() {
        try {
            return this.mSource.getStatsForPackage(this.mInfo.volumeUuid, this.mInfo.packageName, this.mUser);
        } catch (PackageManager.NameNotFoundException | IOException e) {
            Log.w("FetchPackageStorage", "Package may have been removed during query, failing gracefully", e);
            return null;
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onDiscardResult(Ljava/lang/Object;)V */
    @Override // com.android.settingslib.utils.AsyncLoader
    protected void onDiscardResult(StorageStatsSource.AppStorageStats appStorageStats) {
    }
}
