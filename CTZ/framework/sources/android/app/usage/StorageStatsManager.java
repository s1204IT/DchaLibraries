package android.app.usage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.util.UUID;

public class StorageStatsManager {
    private final Context mContext;
    private final IStorageStatsManager mService;

    public StorageStatsManager(Context context, IStorageStatsManager iStorageStatsManager) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mService = (IStorageStatsManager) Preconditions.checkNotNull(iStorageStatsManager);
    }

    public boolean isQuotaSupported(UUID uuid) {
        try {
            return this.mService.isQuotaSupported(StorageManager.convert(uuid), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean isQuotaSupported(String str) {
        return isQuotaSupported(StorageManager.convert(str));
    }

    public boolean isReservedSupported(UUID uuid) {
        try {
            return this.mService.isReservedSupported(StorageManager.convert(uuid), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getTotalBytes(UUID uuid) throws Throwable {
        try {
            return this.mService.getTotalBytes(StorageManager.convert(uuid), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public long getTotalBytes(String str) throws IOException {
        return getTotalBytes(StorageManager.convert(str));
    }

    public long getFreeBytes(UUID uuid) throws Throwable {
        try {
            return this.mService.getFreeBytes(StorageManager.convert(uuid), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public long getFreeBytes(String str) throws IOException {
        return getFreeBytes(StorageManager.convert(str));
    }

    public long getCacheBytes(UUID uuid) throws Throwable {
        try {
            return this.mService.getCacheBytes(StorageManager.convert(uuid), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public long getCacheBytes(String str) throws IOException {
        return getCacheBytes(StorageManager.convert(str));
    }

    public StorageStats queryStatsForPackage(UUID uuid, String str, UserHandle userHandle) throws Throwable {
        try {
            return this.mService.queryStatsForPackage(StorageManager.convert(uuid), str, userHandle.getIdentifier(), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(PackageManager.NameNotFoundException.class);
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public StorageStats queryStatsForPackage(String str, String str2, UserHandle userHandle) throws PackageManager.NameNotFoundException, IOException {
        return queryStatsForPackage(StorageManager.convert(str), str2, userHandle);
    }

    public StorageStats queryStatsForUid(UUID uuid, int i) throws Throwable {
        try {
            return this.mService.queryStatsForUid(StorageManager.convert(uuid), i, this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public StorageStats queryStatsForUid(String str, int i) throws IOException {
        return queryStatsForUid(StorageManager.convert(str), i);
    }

    public StorageStats queryStatsForUser(UUID uuid, UserHandle userHandle) throws Throwable {
        try {
            return this.mService.queryStatsForUser(StorageManager.convert(uuid), userHandle.getIdentifier(), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public StorageStats queryStatsForUser(String str, UserHandle userHandle) throws IOException {
        return queryStatsForUser(StorageManager.convert(str), userHandle);
    }

    public ExternalStorageStats queryExternalStatsForUser(UUID uuid, UserHandle userHandle) throws Throwable {
        try {
            return this.mService.queryExternalStatsForUser(StorageManager.convert(uuid), userHandle.getIdentifier(), this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public ExternalStorageStats queryExternalStatsForUser(String str, UserHandle userHandle) throws IOException {
        return queryExternalStatsForUser(StorageManager.convert(str), userHandle);
    }

    public long getCacheQuotaBytes(String str, int i) {
        try {
            return this.mService.getCacheQuotaBytes(str, i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
