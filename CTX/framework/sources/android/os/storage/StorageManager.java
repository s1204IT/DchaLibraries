package android.os.storage;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.IPackageMoveObserver;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.ProxyFileDescriptorCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IObbActionListener;
import android.os.storage.IStorageEventListener;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.DataUnit;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.AppFuseMount;
import com.android.internal.os.FuseAppLoop;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageManager {
    public static final String ACTION_MANAGE_STORAGE = "android.os.storage.action.MANAGE_STORAGE";
    public static final int CRYPT_TYPE_DEFAULT = 1;
    public static final int CRYPT_TYPE_PASSWORD = 0;
    public static final int CRYPT_TYPE_PATTERN = 2;
    public static final int CRYPT_TYPE_PIN = 3;
    public static final int DEBUG_ADOPTABLE_FORCE_OFF = 2;
    public static final int DEBUG_ADOPTABLE_FORCE_ON = 1;
    public static final int DEBUG_EMULATE_FBE = 4;
    public static final int DEBUG_SDCARDFS_FORCE_OFF = 16;
    public static final int DEBUG_SDCARDFS_FORCE_ON = 8;
    public static final int DEBUG_VIRTUAL_DISK = 32;
    private static final int DEFAULT_CACHE_PERCENTAGE = 10;
    private static final int DEFAULT_THRESHOLD_PERCENTAGE = 5;
    public static final int ENCRYPTION_STATE_ERROR_CORRUPT = -4;
    public static final int ENCRYPTION_STATE_ERROR_INCOMPLETE = -2;
    public static final int ENCRYPTION_STATE_ERROR_INCONSISTENT = -3;
    public static final int ENCRYPTION_STATE_ERROR_UNKNOWN = -1;
    public static final int ENCRYPTION_STATE_NONE = 1;
    public static final int ENCRYPTION_STATE_OK = 0;
    public static final String EXTRA_REQUESTED_BYTES = "android.os.storage.extra.REQUESTED_BYTES";
    public static final String EXTRA_UUID = "android.os.storage.extra.UUID";

    @SystemApi
    public static final int FLAG_ALLOCATE_AGGRESSIVE = 1;
    public static final int FLAG_ALLOCATE_DEFY_ALL_RESERVED = 2;
    public static final int FLAG_ALLOCATE_DEFY_HALF_RESERVED = 4;
    public static final int FLAG_FOR_WRITE = 256;
    public static final int FLAG_INCLUDE_INVISIBLE = 1024;
    public static final int FLAG_REAL_STATE = 512;
    public static final int FLAG_STORAGE_CE = 2;
    public static final int FLAG_STORAGE_DE = 1;
    public static final int FSTRIM_FLAG_DEEP = 1;
    public static final String OWNER_INFO_KEY = "OwnerInfo";
    public static final String PASSWORD_VISIBLE_KEY = "PasswordVisible";
    public static final String PATTERN_VISIBLE_KEY = "PatternVisible";
    public static final String PROP_ADOPTABLE = "persist.sys.adoptable";
    public static final String PROP_EMULATE_FBE = "persist.sys.emulate_fbe";
    public static final String PROP_HAS_ADOPTABLE = "vold.has_adoptable";
    public static final String PROP_HAS_RESERVED = "vold.has_reserved";
    public static final String PROP_PRIMARY_PHYSICAL = "ro.vold.primary_physical";
    public static final String PROP_SDCARDFS = "persist.sys.sdcardfs";
    public static final String PROP_VIRTUAL_DISK = "persist.sys.virtual_disk";
    public static final String SYSTEM_LOCALE_KEY = "SystemLocale";
    private static final String TAG = "StorageManager";
    public static final String UUID_PRIMARY_PHYSICAL = "primary_physical";
    public static final String UUID_SYSTEM = "system";
    private static final String XATTR_CACHE_GROUP = "user.cache_group";
    private static final String XATTR_CACHE_TOMBSTONE = "user.cache_tombstone";
    private final Context mContext;
    private final Looper mLooper;
    private final ContentResolver mResolver;
    public static final String UUID_PRIVATE_INTERNAL = null;
    public static final UUID UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69");
    public static final UUID UUID_PRIMARY_PHYSICAL_ = UUID.fromString("0f95a519-dae7-5abf-9519-fbd6209e05fd");
    public static final UUID UUID_SYSTEM_ = UUID.fromString("5d258386-e60d-59e3-826d-0089cdd42cc0");
    private static volatile IStorageManager sStorageManager = null;
    private static final long DEFAULT_THRESHOLD_MAX_BYTES = DataUnit.MEBIBYTES.toBytes(500);
    private static final long DEFAULT_CACHE_MAX_BYTES = DataUnit.GIBIBYTES.toBytes(5);
    private static final long DEFAULT_FULL_THRESHOLD_BYTES = DataUnit.MEBIBYTES.toBytes(1);
    private final AtomicInteger mNextNonce = new AtomicInteger(0);
    private final ArrayList<StorageEventListenerDelegate> mDelegates = new ArrayList<>();
    private final ObbActionListener mObbActionListener = new ObbActionListener();
    private final Object mFuseAppLoopLock = new Object();

    @GuardedBy("mFuseAppLoopLock")
    private FuseAppLoop mFuseAppLoop = null;
    private final IStorageManager mStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getServiceOrThrow("mount"));

    @Retention(RetentionPolicy.SOURCE)
    public @interface AllocateFlags {
    }

    private static class StorageEventListenerDelegate extends IStorageEventListener.Stub implements Handler.Callback {
        private static final int MSG_DISK_DESTROYED = 6;
        private static final int MSG_DISK_SCANNED = 5;
        private static final int MSG_STORAGE_STATE_CHANGED = 1;
        private static final int MSG_VOLUME_FORGOTTEN = 4;
        private static final int MSG_VOLUME_RECORD_CHANGED = 3;
        private static final int MSG_VOLUME_STATE_CHANGED = 2;
        final StorageEventListener mCallback;
        final Handler mHandler;

        public StorageEventListenerDelegate(StorageEventListener storageEventListener, Looper looper) {
            this.mCallback = storageEventListener;
            this.mHandler = new Handler(looper, this);
        }

        @Override
        public boolean handleMessage(Message message) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            switch (message.what) {
                case 1:
                    this.mCallback.onStorageStateChanged((String) someArgs.arg1, (String) someArgs.arg2, (String) someArgs.arg3);
                    someArgs.recycle();
                    return true;
                case 2:
                    this.mCallback.onVolumeStateChanged((VolumeInfo) someArgs.arg1, someArgs.argi2, someArgs.argi3);
                    someArgs.recycle();
                    return true;
                case 3:
                    this.mCallback.onVolumeRecordChanged((VolumeRecord) someArgs.arg1);
                    someArgs.recycle();
                    return true;
                case 4:
                    this.mCallback.onVolumeForgotten((String) someArgs.arg1);
                    someArgs.recycle();
                    return true;
                case 5:
                    this.mCallback.onDiskScanned((DiskInfo) someArgs.arg1, someArgs.argi2);
                    someArgs.recycle();
                    return true;
                case 6:
                    this.mCallback.onDiskDestroyed((DiskInfo) someArgs.arg1);
                    someArgs.recycle();
                    return true;
                default:
                    someArgs.recycle();
                    return false;
            }
        }

        @Override
        public void onUsbMassStorageConnectionChanged(boolean z) throws RemoteException {
        }

        @Override
        public void onStorageStateChanged(String str, String str2, String str3) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            someArgsObtain.arg3 = str3;
            this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
        }

        @Override
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = volumeInfo;
            someArgsObtain.argi2 = i;
            someArgsObtain.argi3 = i2;
            this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = volumeRecord;
            this.mHandler.obtainMessage(3, someArgsObtain).sendToTarget();
        }

        @Override
        public void onVolumeForgotten(String str) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            this.mHandler.obtainMessage(4, someArgsObtain).sendToTarget();
        }

        @Override
        public void onDiskScanned(DiskInfo diskInfo, int i) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = diskInfo;
            someArgsObtain.argi2 = i;
            this.mHandler.obtainMessage(5, someArgsObtain).sendToTarget();
        }

        @Override
        public void onDiskDestroyed(DiskInfo diskInfo) throws RemoteException {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = diskInfo;
            this.mHandler.obtainMessage(6, someArgsObtain).sendToTarget();
        }
    }

    private class ObbActionListener extends IObbActionListener.Stub {
        private SparseArray<ObbListenerDelegate> mListeners;

        private ObbActionListener() {
            this.mListeners = new SparseArray<>();
        }

        @Override
        public void onObbResult(String str, int i, int i2) {
            ObbListenerDelegate obbListenerDelegate;
            synchronized (this.mListeners) {
                obbListenerDelegate = this.mListeners.get(i);
                if (obbListenerDelegate != null) {
                    this.mListeners.remove(i);
                }
            }
            if (obbListenerDelegate != null) {
                obbListenerDelegate.sendObbStateChanged(str, i2);
            }
        }

        public int addListener(OnObbStateChangeListener onObbStateChangeListener) {
            ObbListenerDelegate obbListenerDelegate = StorageManager.this.new ObbListenerDelegate(onObbStateChangeListener);
            synchronized (this.mListeners) {
                this.mListeners.put(obbListenerDelegate.nonce, obbListenerDelegate);
            }
            return obbListenerDelegate.nonce;
        }
    }

    private int getNextNonce() {
        return this.mNextNonce.getAndIncrement();
    }

    private class ObbListenerDelegate {
        private final Handler mHandler;
        private final WeakReference<OnObbStateChangeListener> mObbEventListenerRef;
        private final int nonce;

        ObbListenerDelegate(OnObbStateChangeListener onObbStateChangeListener) {
            this.nonce = StorageManager.this.getNextNonce();
            this.mObbEventListenerRef = new WeakReference<>(onObbStateChangeListener);
            this.mHandler = new Handler(StorageManager.this.mLooper) {
                @Override
                public void handleMessage(Message message) {
                    OnObbStateChangeListener listener = ObbListenerDelegate.this.getListener();
                    if (listener == null) {
                        return;
                    }
                    listener.onObbStateChange((String) message.obj, message.arg1);
                }
            };
        }

        OnObbStateChangeListener getListener() {
            if (this.mObbEventListenerRef == null) {
                return null;
            }
            return this.mObbEventListenerRef.get();
        }

        void sendObbStateChanged(String str, int i) {
            this.mHandler.obtainMessage(0, i, 0, str).sendToTarget();
        }
    }

    @Deprecated
    public static StorageManager from(Context context) {
        return (StorageManager) context.getSystemService(StorageManager.class);
    }

    public StorageManager(Context context, Looper looper) throws ServiceManager.ServiceNotFoundException {
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mLooper = looper;
    }

    public void registerListener(StorageEventListener storageEventListener) {
        synchronized (this.mDelegates) {
            StorageEventListenerDelegate storageEventListenerDelegate = new StorageEventListenerDelegate(storageEventListener, this.mLooper);
            try {
                this.mStorageManager.registerListener(storageEventListenerDelegate);
                this.mDelegates.add(storageEventListenerDelegate);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void unregisterListener(StorageEventListener storageEventListener) {
        synchronized (this.mDelegates) {
            Iterator<StorageEventListenerDelegate> it = this.mDelegates.iterator();
            while (it.hasNext()) {
                StorageEventListenerDelegate next = it.next();
                if (next.mCallback == storageEventListener) {
                    try {
                        this.mStorageManager.unregisterListener(next);
                        it.remove();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    @Deprecated
    public void enableUsbMassStorage() {
    }

    @Deprecated
    public void disableUsbMassStorage() {
    }

    @Deprecated
    public boolean isUsbMassStorageConnected() {
        return false;
    }

    @Deprecated
    public boolean isUsbMassStorageEnabled() {
        return false;
    }

    public boolean mountObb(String str, String str2, OnObbStateChangeListener onObbStateChangeListener) {
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        Preconditions.checkNotNull(onObbStateChangeListener, "listener cannot be null");
        try {
            this.mStorageManager.mountObb(str, new File(str).getCanonicalPath(), str2, this.mObbActionListener, this.mObbActionListener.addListener(onObbStateChangeListener));
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (IOException e2) {
            throw new IllegalArgumentException("Failed to resolve path: " + str, e2);
        }
    }

    public boolean unmountObb(String str, boolean z, OnObbStateChangeListener onObbStateChangeListener) {
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        Preconditions.checkNotNull(onObbStateChangeListener, "listener cannot be null");
        try {
            this.mStorageManager.unmountObb(str, z, this.mObbActionListener, this.mObbActionListener.addListener(onObbStateChangeListener));
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isObbMounted(String str) {
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        try {
            return this.mStorageManager.isObbMounted(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMountedObbPath(String str) {
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        try {
            return this.mStorageManager.getMountedObbPath(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<DiskInfo> getDisks() {
        try {
            return Arrays.asList(this.mStorageManager.getDisks());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public DiskInfo findDiskById(String str) {
        Preconditions.checkNotNull(str);
        for (DiskInfo diskInfo : getDisks()) {
            if (Objects.equals(diskInfo.id, str)) {
                return diskInfo;
            }
        }
        return null;
    }

    public VolumeInfo findVolumeById(String str) {
        Preconditions.checkNotNull(str);
        for (VolumeInfo volumeInfo : getVolumes()) {
            if (Objects.equals(volumeInfo.id, str)) {
                return volumeInfo;
            }
        }
        return null;
    }

    public VolumeInfo findVolumeByUuid(String str) {
        Preconditions.checkNotNull(str);
        for (VolumeInfo volumeInfo : getVolumes()) {
            if (Objects.equals(volumeInfo.fsUuid, str)) {
                return volumeInfo;
            }
        }
        return null;
    }

    public VolumeRecord findRecordByUuid(String str) {
        Preconditions.checkNotNull(str);
        for (VolumeRecord volumeRecord : getVolumeRecords()) {
            if (Objects.equals(volumeRecord.fsUuid, str)) {
                return volumeRecord;
            }
        }
        return null;
    }

    public VolumeInfo findPrivateForEmulated(VolumeInfo volumeInfo) {
        if (volumeInfo != null) {
            return findVolumeById(volumeInfo.getId().replace(VolumeInfo.ID_EMULATED_INTERNAL, VolumeInfo.ID_PRIVATE_INTERNAL));
        }
        return null;
    }

    public VolumeInfo findEmulatedForPrivate(VolumeInfo volumeInfo) {
        if (volumeInfo != null) {
            return findVolumeById(volumeInfo.getId().replace(VolumeInfo.ID_PRIVATE_INTERNAL, VolumeInfo.ID_EMULATED_INTERNAL));
        }
        return null;
    }

    public VolumeInfo findVolumeByQualifiedUuid(String str) {
        if (Objects.equals(UUID_PRIVATE_INTERNAL, str)) {
            return findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL);
        }
        if (Objects.equals(UUID_PRIMARY_PHYSICAL, str)) {
            return getPrimaryPhysicalVolume();
        }
        return findVolumeByUuid(str);
    }

    public UUID getUuidForPath(File file) throws IOException {
        Preconditions.checkNotNull(file);
        String canonicalPath = file.getCanonicalPath();
        if (FileUtils.contains(Environment.getDataDirectory().getAbsolutePath(), canonicalPath)) {
            return UUID_DEFAULT;
        }
        try {
            for (VolumeInfo volumeInfo : this.mStorageManager.getVolumes(0)) {
                if (volumeInfo.path != null && FileUtils.contains(volumeInfo.path, canonicalPath) && volumeInfo.type != 0) {
                    try {
                        return convert(volumeInfo.fsUuid);
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
            throw new FileNotFoundException("Failed to find a storage device for " + file);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public File findPathForUuid(String str) throws FileNotFoundException {
        VolumeInfo volumeInfoFindVolumeByQualifiedUuid = findVolumeByQualifiedUuid(str);
        if (volumeInfoFindVolumeByQualifiedUuid != null) {
            return volumeInfoFindVolumeByQualifiedUuid.getPath();
        }
        throw new FileNotFoundException("Failed to find a storage device for " + str);
    }

    public boolean isAllocationSupported(FileDescriptor fileDescriptor) {
        try {
            getUuidForPath(ParcelFileDescriptor.getFile(fileDescriptor));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public List<VolumeInfo> getVolumes() {
        try {
            return Arrays.asList(this.mStorageManager.getVolumes(0));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<VolumeInfo> getWritablePrivateVolumes() {
        try {
            ArrayList arrayList = new ArrayList();
            for (VolumeInfo volumeInfo : this.mStorageManager.getVolumes(0)) {
                if (volumeInfo.getType() == 1 && volumeInfo.isMountedWritable()) {
                    arrayList.add(volumeInfo);
                }
            }
            return arrayList;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<VolumeRecord> getVolumeRecords() {
        try {
            return Arrays.asList(this.mStorageManager.getVolumeRecords(0));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getBestVolumeDescription(VolumeInfo volumeInfo) {
        VolumeRecord volumeRecordFindRecordByUuid;
        if (volumeInfo == null) {
            return null;
        }
        if (!TextUtils.isEmpty(volumeInfo.fsUuid) && (volumeRecordFindRecordByUuid = findRecordByUuid(volumeInfo.fsUuid)) != null && !TextUtils.isEmpty(volumeRecordFindRecordByUuid.nickname)) {
            return volumeRecordFindRecordByUuid.nickname;
        }
        if (!TextUtils.isEmpty(volumeInfo.getDescription())) {
            return volumeInfo.getDescription();
        }
        if (volumeInfo.disk == null) {
            return null;
        }
        return volumeInfo.disk.getDescription();
    }

    public VolumeInfo getPrimaryPhysicalVolume() {
        for (VolumeInfo volumeInfo : getVolumes()) {
            if (volumeInfo.isPrimaryPhysical()) {
                return volumeInfo;
            }
        }
        return null;
    }

    public void mount(String str) {
        try {
            this.mStorageManager.mount(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unmount(String str) {
        try {
            this.mStorageManager.unmount(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void format(String str) {
        try {
            this.mStorageManager.format(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public long benchmark(String str) {
        final CompletableFuture completableFuture = new CompletableFuture();
        benchmark(str, new IVoldTaskListener.Stub() {
            @Override
            public void onStatus(int i, PersistableBundle persistableBundle) {
            }

            @Override
            public void onFinished(int i, PersistableBundle persistableBundle) {
                completableFuture.complete(persistableBundle);
            }
        });
        try {
            return ((PersistableBundle) completableFuture.get(3L, TimeUnit.MINUTES)).getLong("run", Long.MAX_VALUE) * TimeUtils.NANOS_PER_MS;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    public void benchmark(String str, IVoldTaskListener iVoldTaskListener) {
        try {
            this.mStorageManager.benchmark(str, iVoldTaskListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void partitionPublic(String str) {
        try {
            this.mStorageManager.partitionPublic(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void partitionPrivate(String str) {
        try {
            this.mStorageManager.partitionPrivate(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void partitionMixed(String str, int i) {
        try {
            this.mStorageManager.partitionMixed(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void wipeAdoptableDisks() {
        for (DiskInfo diskInfo : getDisks()) {
            String id = diskInfo.getId();
            if (diskInfo.isAdoptable()) {
                Slog.d(TAG, "Found adoptable " + id + "; wiping");
                try {
                    this.mStorageManager.partitionPublic(id);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to wipe " + id + ", but soldiering onward", e);
                }
            } else {
                Slog.d(TAG, "Ignorning non-adoptable disk " + diskInfo.getId());
            }
        }
    }

    public void setVolumeNickname(String str, String str2) {
        try {
            this.mStorageManager.setVolumeNickname(str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVolumeInited(String str, boolean z) {
        try {
            this.mStorageManager.setVolumeUserFlags(str, z ? 1 : 0, 1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setVolumeSnoozed(String str, boolean z) {
        try {
            this.mStorageManager.setVolumeUserFlags(str, z ? 2 : 0, 2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forgetVolume(String str) {
        try {
            this.mStorageManager.forgetVolume(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getPrimaryStorageUuid() {
        try {
            return this.mStorageManager.getPrimaryStorageUuid();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPrimaryStorageUuid(String str, IPackageMoveObserver iPackageMoveObserver) {
        try {
            this.mStorageManager.setPrimaryStorageUuid(str, iPackageMoveObserver);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public StorageVolume getStorageVolume(File file) {
        return getStorageVolume(getVolumeList(), file);
    }

    public static StorageVolume getStorageVolume(File file, int i) {
        return getStorageVolume(getVolumeList(i, 0), file);
    }

    private static StorageVolume getStorageVolume(StorageVolume[] storageVolumeArr, File file) {
        if (file == null) {
            return null;
        }
        try {
            File canonicalFile = file.getCanonicalFile();
            for (StorageVolume storageVolume : storageVolumeArr) {
                if (FileUtils.contains(storageVolume.getPathFile().getCanonicalFile(), canonicalFile)) {
                    return storageVolume;
                }
            }
            return null;
        } catch (IOException e) {
            Slog.d(TAG, "Could not get canonical path for " + file);
            return null;
        }
    }

    @Deprecated
    public String getVolumeState(String str) {
        StorageVolume storageVolume = getStorageVolume(new File(str));
        if (storageVolume != null) {
            return storageVolume.getState();
        }
        return "unknown";
    }

    public List<StorageVolume> getStorageVolumes() {
        ArrayList arrayList = new ArrayList();
        Collections.addAll(arrayList, getVolumeList(this.mContext.getUserId(), 1536));
        return arrayList;
    }

    public StorageVolume getPrimaryStorageVolume() {
        return getVolumeList(this.mContext.getUserId(), 1536)[0];
    }

    public static Pair<String, Long> getPrimaryStoragePathAndSize() {
        return Pair.create(null, Long.valueOf(FileUtils.roundStorageSize(Environment.getDataDirectory().getTotalSpace() + Environment.getRootDirectory().getTotalSpace())));
    }

    public long getPrimaryStorageSize() {
        return FileUtils.roundStorageSize(Environment.getDataDirectory().getTotalSpace() + Environment.getRootDirectory().getTotalSpace());
    }

    public void mkdirs(File file) {
        try {
            this.mStorageManager.mkdirs(this.mContext.getOpPackageName(), file.getAbsolutePath());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public StorageVolume[] getVolumeList() {
        return getVolumeList(this.mContext.getUserId(), 0);
    }

    public static StorageVolume[] getVolumeList(int i, int i2) {
        IStorageManager iStorageManagerAsInterface = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        try {
            String strCurrentOpPackageName = ActivityThread.currentOpPackageName();
            if (strCurrentOpPackageName == null) {
                String[] packagesForUid = ActivityThread.getPackageManager().getPackagesForUid(Process.myUid());
                if (packagesForUid != null && packagesForUid.length > 0) {
                    strCurrentOpPackageName = packagesForUid[0];
                }
                return new StorageVolume[0];
            }
            int packageUid = ActivityThread.getPackageManager().getPackageUid(strCurrentOpPackageName, 268435456, i);
            if (packageUid <= 0) {
                return new StorageVolume[0];
            }
            return iStorageManagerAsInterface.getVolumeList(packageUid, strCurrentOpPackageName, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public String[] getVolumePaths() {
        StorageVolume[] volumeList = getVolumeList();
        int length = volumeList.length;
        String[] strArr = new String[length];
        for (int i = 0; i < length; i++) {
            strArr[i] = volumeList[i].getPath();
        }
        return strArr;
    }

    public StorageVolume getPrimaryVolume() {
        return getPrimaryVolume(getVolumeList());
    }

    public static StorageVolume getPrimaryVolume(StorageVolume[] storageVolumeArr) {
        for (StorageVolume storageVolume : storageVolumeArr) {
            if (storageVolume.isPrimary()) {
                return storageVolume;
            }
        }
        throw new IllegalStateException("Missing primary storage");
    }

    public long getStorageBytesUntilLow(File file) {
        return file.getUsableSpace() - getStorageFullBytes(file);
    }

    public long getStorageLowBytes(File file) {
        return Math.min((file.getTotalSpace() * ((long) Settings.Global.getInt(this.mResolver, Settings.Global.SYS_STORAGE_THRESHOLD_PERCENTAGE, 5))) / 100, Settings.Global.getLong(this.mResolver, Settings.Global.SYS_STORAGE_THRESHOLD_MAX_BYTES, DEFAULT_THRESHOLD_MAX_BYTES));
    }

    public long getStorageCacheBytes(File file, int i) {
        long jMin = Math.min((file.getTotalSpace() * ((long) Settings.Global.getInt(this.mResolver, Settings.Global.SYS_STORAGE_CACHE_PERCENTAGE, 10))) / 100, Settings.Global.getLong(this.mResolver, Settings.Global.SYS_STORAGE_CACHE_MAX_BYTES, DEFAULT_CACHE_MAX_BYTES));
        if ((i & 1) != 0 || (i & 2) != 0) {
            return 0L;
        }
        if ((i & 4) != 0) {
            return jMin / 2;
        }
        return jMin;
    }

    public long getStorageFullBytes(File file) {
        return Settings.Global.getLong(this.mResolver, Settings.Global.SYS_STORAGE_FULL_THRESHOLD_BYTES, DEFAULT_FULL_THRESHOLD_BYTES);
    }

    public void createUserKey(int i, int i2, boolean z) {
        try {
            this.mStorageManager.createUserKey(i, i2, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void destroyUserKey(int i) {
        try {
            this.mStorageManager.destroyUserKey(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unlockUserKey(int i, int i2, byte[] bArr, byte[] bArr2) {
        try {
            this.mStorageManager.unlockUserKey(i, i2, bArr, bArr2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void lockUserKey(int i) {
        try {
            this.mStorageManager.lockUserKey(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void prepareUserStorage(String str, int i, int i2, int i3) {
        try {
            this.mStorageManager.prepareUserStorage(str, i, i2, i3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void destroyUserStorage(String str, int i, int i2) {
        try {
            this.mStorageManager.destroyUserStorage(str, i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isUserKeyUnlocked(int i) {
        if (sStorageManager == null) {
            sStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        }
        if (sStorageManager == null) {
            Slog.w(TAG, "Early during boot, assuming locked");
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                return sStorageManager.isUserKeyUnlocked(i);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isEncrypted(File file) {
        if (FileUtils.contains(Environment.getDataDirectory(), file)) {
            return isEncrypted();
        }
        if (FileUtils.contains(Environment.getExpandDirectory(), file)) {
            return true;
        }
        return false;
    }

    public static boolean isEncryptable() {
        return RoSystemProperties.CRYPTO_ENCRYPTABLE;
    }

    public static boolean isEncrypted() {
        return RoSystemProperties.CRYPTO_ENCRYPTED;
    }

    public static boolean isFileEncryptedNativeOnly() {
        if (!isEncrypted()) {
            return false;
        }
        return RoSystemProperties.CRYPTO_FILE_ENCRYPTED;
    }

    public static boolean isBlockEncrypted() {
        if (!isEncrypted()) {
            return false;
        }
        return RoSystemProperties.CRYPTO_BLOCK_ENCRYPTED;
    }

    public static boolean isNonDefaultBlockEncrypted() {
        if (!isBlockEncrypted()) {
            return false;
        }
        try {
            return IStorageManager.Stub.asInterface(ServiceManager.getService("mount")).getPasswordType() != 1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting encryption type");
            return false;
        }
    }

    public static boolean isBlockEncrypting() {
        return !"".equalsIgnoreCase(SystemProperties.get("vold.encrypt_progress", ""));
    }

    public static boolean inCryptKeeperBounce() {
        return "trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt"));
    }

    public static boolean isFileEncryptedEmulatedOnly() {
        return SystemProperties.getBoolean(PROP_EMULATE_FBE, false);
    }

    public static boolean isFileEncryptedNativeOrEmulated() {
        return isFileEncryptedNativeOnly() || isFileEncryptedEmulatedOnly();
    }

    public static boolean hasAdoptable() {
        return SystemProperties.getBoolean(PROP_HAS_ADOPTABLE, false);
    }

    public static File maybeTranslateEmulatedPathToInternal(File file) {
        return file;
    }

    @VisibleForTesting
    public ParcelFileDescriptor openProxyFileDescriptor(int i, ProxyFileDescriptorCallback proxyFileDescriptorCallback, Handler handler, ThreadFactory threadFactory) throws IOException {
        ParcelFileDescriptor parcelFileDescriptorOpenProxyFileDescriptor;
        Preconditions.checkNotNull(proxyFileDescriptorCallback);
        MetricsLogger.count(this.mContext, "storage_open_proxy_file_descriptor", 1);
        while (true) {
            try {
                synchronized (this.mFuseAppLoopLock) {
                    boolean z = false;
                    if (this.mFuseAppLoop == null) {
                        AppFuseMount appFuseMountMountProxyFileDescriptorBridge = this.mStorageManager.mountProxyFileDescriptorBridge();
                        if (appFuseMountMountProxyFileDescriptorBridge == null) {
                            throw new IOException("Failed to mount proxy bridge");
                        }
                        this.mFuseAppLoop = new FuseAppLoop(appFuseMountMountProxyFileDescriptorBridge.mountPointId, appFuseMountMountProxyFileDescriptorBridge.fd, threadFactory);
                        z = true;
                    }
                    if (handler == null) {
                        handler = new Handler(Looper.getMainLooper());
                    }
                    try {
                        int iRegisterCallback = this.mFuseAppLoop.registerCallback(proxyFileDescriptorCallback, handler);
                        parcelFileDescriptorOpenProxyFileDescriptor = this.mStorageManager.openProxyFileDescriptor(this.mFuseAppLoop.getMountPointId(), iRegisterCallback, i);
                        if (parcelFileDescriptorOpenProxyFileDescriptor == null) {
                            this.mFuseAppLoop.unregisterCallback(iRegisterCallback);
                            throw new FuseUnavailableMountException(this.mFuseAppLoop.getMountPointId());
                        }
                    } catch (FuseUnavailableMountException e) {
                        if (z) {
                            throw new IOException(e);
                        }
                        this.mFuseAppLoop = null;
                    }
                }
                return parcelFileDescriptorOpenProxyFileDescriptor;
            } catch (RemoteException e2) {
                throw new IOException(e2);
            }
        }
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int i, ProxyFileDescriptorCallback proxyFileDescriptorCallback) throws IOException {
        return openProxyFileDescriptor(i, proxyFileDescriptorCallback, null, null);
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int i, ProxyFileDescriptorCallback proxyFileDescriptorCallback, Handler handler) throws IOException {
        Preconditions.checkNotNull(handler);
        return openProxyFileDescriptor(i, proxyFileDescriptorCallback, handler, null);
    }

    @VisibleForTesting
    public int getProxyFileDescriptorMountPointId() {
        int mountPointId;
        synchronized (this.mFuseAppLoopLock) {
            mountPointId = this.mFuseAppLoop != null ? this.mFuseAppLoop.getMountPointId() : -1;
        }
        return mountPointId;
    }

    public long getCacheQuotaBytes(UUID uuid) throws Throwable {
        try {
            return this.mStorageManager.getCacheQuotaBytes(convert(uuid), this.mContext.getApplicationInfo().uid);
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public long getCacheSizeBytes(UUID uuid) throws Throwable {
        try {
            return this.mStorageManager.getCacheSizeBytes(convert(uuid), this.mContext.getApplicationInfo().uid);
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public long getAllocatableBytes(UUID uuid) throws IOException {
        return getAllocatableBytes(uuid, 0);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public long getAllocatableBytes(UUID uuid, int i) throws Throwable {
        try {
            return this.mStorageManager.getAllocatableBytes(convert(uuid), i, this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void allocateBytes(UUID uuid, long j) throws Throwable {
        allocateBytes(uuid, j, 0);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void allocateBytes(UUID uuid, long j, int i) throws Throwable {
        try {
            this.mStorageManager.allocateBytes(convert(uuid), j, i, this.mContext.getOpPackageName());
        } catch (ParcelableException e) {
            e.maybeRethrow(IOException.class);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void allocateBytes(FileDescriptor fileDescriptor, long j) throws Throwable {
        allocateBytes(fileDescriptor, j, 0);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public void allocateBytes(FileDescriptor fileDescriptor, long j, int i) throws Throwable {
        File file = ParcelFileDescriptor.getFile(fileDescriptor);
        UUID uuidForPath = getUuidForPath(file);
        for (int i2 = 0; i2 < 3; i2++) {
            try {
                long j2 = j - (Os.fstat(fileDescriptor).st_blocks * 512);
                if (j2 > 0) {
                    allocateBytes(uuidForPath, j2, i);
                }
                try {
                    Os.posix_fallocate(fileDescriptor, 0L, j);
                    return;
                } catch (ErrnoException e) {
                    if (e.errno != OsConstants.ENOSYS && e.errno != OsConstants.ENOTSUP) {
                        throw e;
                    }
                    Log.w(TAG, "fallocate() not supported; falling back to ftruncate()");
                    Os.ftruncate(fileDescriptor, j);
                    return;
                }
            } catch (ErrnoException e2) {
                if (e2.errno == OsConstants.ENOSPC) {
                    Log.w(TAG, "Odd, not enough space; let's try again?");
                } else {
                    throw e2.rethrowAsIOException();
                }
            }
        }
        throw new IOException("Well this is embarassing; we can't allocate " + j + " for " + file);
    }

    private static void setCacheBehavior(File file, String str, boolean z) throws IOException {
        if (!file.isDirectory()) {
            throw new IOException("Cache behavior can only be set on directories");
        }
        if (z) {
            try {
                Os.setxattr(file.getAbsolutePath(), str, WifiEnterpriseConfig.ENGINE_ENABLE.getBytes(StandardCharsets.UTF_8), 0);
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        } else {
            try {
                Os.removexattr(file.getAbsolutePath(), str);
            } catch (ErrnoException e2) {
                if (e2.errno != OsConstants.ENODATA) {
                    throw e2.rethrowAsIOException();
                }
            }
        }
    }

    private static boolean isCacheBehavior(File file, String str) throws IOException {
        try {
            Os.getxattr(file.getAbsolutePath(), str);
            return true;
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.ENODATA) {
                throw e.rethrowAsIOException();
            }
            return false;
        }
    }

    public void setCacheBehaviorGroup(File file, boolean z) throws IOException {
        setCacheBehavior(file, XATTR_CACHE_GROUP, z);
    }

    public boolean isCacheBehaviorGroup(File file) throws IOException {
        return isCacheBehavior(file, XATTR_CACHE_GROUP);
    }

    public void setCacheBehaviorTombstone(File file, boolean z) throws IOException {
        setCacheBehavior(file, XATTR_CACHE_TOMBSTONE, z);
    }

    public boolean isCacheBehaviorTombstone(File file) throws IOException {
        return isCacheBehavior(file, XATTR_CACHE_TOMBSTONE);
    }

    public static UUID convert(String str) {
        if (Objects.equals(str, UUID_PRIVATE_INTERNAL)) {
            return UUID_DEFAULT;
        }
        if (Objects.equals(str, UUID_PRIMARY_PHYSICAL)) {
            return UUID_PRIMARY_PHYSICAL_;
        }
        if (Objects.equals(str, UUID_SYSTEM)) {
            return UUID_SYSTEM_;
        }
        return UUID.fromString(str);
    }

    public static String convert(UUID uuid) {
        if (UUID_DEFAULT.equals(uuid)) {
            return UUID_PRIVATE_INTERNAL;
        }
        if (UUID_PRIMARY_PHYSICAL_.equals(uuid)) {
            return UUID_PRIMARY_PHYSICAL;
        }
        if (UUID_SYSTEM_.equals(uuid)) {
            return UUID_SYSTEM;
        }
        return uuid.toString();
    }
}
