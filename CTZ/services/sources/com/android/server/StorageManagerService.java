package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.admin.SecurityLog;
import android.app.usage.StorageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.ObbInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IStoraged;
import android.os.IVold;
import android.os.IVoldListener;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.IObbActionListener;
import android.os.storage.IStorageEventListener;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageShutdownObserver;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.os.AppFuseMount;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.BatteryService;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.storage.AppFuseBridge;
import com.android.server.usage.UnixCalendar;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class StorageManagerService extends IStorageManager.Stub implements Watchdog.Monitor, ActivityManagerInternal.ScreenObserver {
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_FS_UUID = "fsUuid";
    private static final String ATTR_LAST_BENCH_MILLIS = "lastBenchMillis";
    private static final String ATTR_LAST_TRIM_MILLIS = "lastTrimMillis";
    private static final String ATTR_NICKNAME = "nickname";
    private static final String ATTR_PART_GUID = "partGuid";
    private static final String ATTR_PRIMARY_STORAGE_UUID = "primaryStorageUuid";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USER_FLAGS = "userFlags";
    private static final String ATTR_VERSION = "version";
    private static final int CRYPTO_ALGORITHM_KEY_SIZE = 128;
    private static final boolean DEBUG_EVENTS = false;
    private static final boolean DEBUG_OBB = false;
    private static final boolean EMULATE_FBE_SUPPORTED = true;
    private static final int H_ABORT_IDLE_MAINT = 12;
    private static final int H_DAEMON_CONNECTED = 2;
    private static final int H_FSTRIM = 4;
    private static final int H_INTERNAL_BROADCAST = 7;
    private static final int H_PARTITION_FORGET = 9;
    private static final int H_RESET = 10;
    private static final int H_RUN_IDLE_MAINT = 11;
    private static final int H_SHUTDOWN = 3;
    private static final int H_SYSTEM_READY = 1;
    private static final int H_VOLUME_BROADCAST = 6;
    private static final int H_VOLUME_MOUNT = 5;
    private static final int H_VOLUME_UNMOUNT = 8;
    private static final String LAST_FSTRIM_FILE = "last-fstrim";
    private static final int MOVE_STATUS_COPY_FINISHED = 82;
    private static final int OBB_FLUSH_MOUNT_STATE = 5;
    private static final int OBB_MCS_BOUND = 2;
    private static final int OBB_MCS_RECONNECT = 4;
    private static final int OBB_MCS_UNBIND = 3;
    private static final int OBB_RUN_ACTION = 1;
    private static final int PBKDF2_HASH_ROUNDS = 1024;
    private static final String TAG = "StorageManagerService";
    private static final String TAG_STORAGE_BENCHMARK = "storage_benchmark";
    private static final String TAG_STORAGE_TRIM = "storage_trim";
    private static final String TAG_VOLUME = "volume";
    private static final String TAG_VOLUMES = "volumes";
    private static final int VERSION_ADD_PRIMARY = 2;
    private static final int VERSION_FIX_PRIMARY = 3;
    private static final int VERSION_INIT = 1;
    private static final boolean WATCHDOG_ENABLE = false;
    private static final String ZRAM_ENABLED_PROPERTY = "persist.sys.zram_enabled";
    private final Callbacks mCallbacks;
    protected final Context mContext;
    protected final Handler mHandler;
    private long mLastMaintenance;
    private final File mLastMaintenanceFile;
    private final LockPatternUtils mLockPatternUtils;

    @GuardedBy("mLock")
    private IPackageMoveObserver mMoveCallback;

    @GuardedBy("mLock")
    private String mMoveTargetUuid;
    private final ObbActionHandler mObbActionHandler;
    private PackageManagerService mPms;

    @GuardedBy("mLock")
    private String mPrimaryStorageUuid;
    private final AtomicFile mSettingsFile;
    private volatile IStoraged mStoraged;
    private volatile IVold mVold;
    static StorageManagerService sSelf = null;
    public static final String[] CRYPTO_TYPES = {"password", BatteryService.HealthServiceWrapper.INSTANCE_VENDOR, "pattern", "pin"};
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    protected final Object mLock = LockGuard.installNewLock(4);

    @GuardedBy("mLock")
    private int[] mLocalUnlockedUsers = EmptyArray.INT;

    @GuardedBy("mLock")
    private int[] mSystemUnlockedUsers = EmptyArray.INT;

    @GuardedBy("mLock")
    private ArrayMap<String, DiskInfo> mDisks = new ArrayMap<>();

    @GuardedBy("mLock")
    protected final ArrayMap<String, VolumeInfo> mVolumes = new ArrayMap<>();

    @GuardedBy("mLock")
    private ArrayMap<String, VolumeRecord> mRecords = new ArrayMap<>();

    @GuardedBy("mLock")
    private ArrayMap<String, CountDownLatch> mDiskScanLatches = new ArrayMap<>();
    protected volatile int mCurrentUserId = 0;
    private final Object mAppFuseLock = new Object();

    @GuardedBy("mAppFuseLock")
    private int mNextAppFuseName = 0;

    @GuardedBy("mAppFuseLock")
    private AppFuseBridge mAppFuseBridge = null;
    private volatile boolean mSystemReady = false;
    private volatile boolean mBootCompleted = false;
    private volatile boolean mDaemonConnected = false;
    private volatile boolean mSecureKeyguardShowing = true;
    private final Map<IBinder, List<ObbState>> mObbMounts = new HashMap();
    private final Map<String, ObbState> mObbPathToStateMap = new HashMap();
    private final StorageManagerInternalImpl mStorageManagerInternal = new StorageManagerInternalImpl();
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    private IMediaContainerService mContainerService = null;
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
            Preconditions.checkArgument(intExtra >= 0);
            try {
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    StorageManagerService.this.mVold.onUserAdded(intExtra, ((UserManager) StorageManagerService.this.mContext.getSystemService(UserManager.class)).getUserSerialNumber(intExtra));
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    synchronized (StorageManagerService.this.mVolumes) {
                        int size = StorageManagerService.this.mVolumes.size();
                        for (int i = 0; i < size; i++) {
                            VolumeInfo volumeInfoValueAt = StorageManagerService.this.mVolumes.valueAt(i);
                            if (volumeInfoValueAt.mountUserId == intExtra) {
                                volumeInfoValueAt.mountUserId = -10000;
                                StorageManagerService.this.mHandler.obtainMessage(8, volumeInfoValueAt).sendToTarget();
                            }
                        }
                    }
                    StorageManagerService.this.mVold.onUserRemoved(intExtra);
                }
            } catch (Exception e) {
                Slog.wtf(StorageManagerService.TAG, e);
            }
        }
    };
    private final IVoldListener mListener = new IVoldListener.Stub() {
        @Override
        public void onDiskCreated(String str, int i) {
            synchronized (StorageManagerService.this.mLock) {
                String str2 = SystemProperties.get("persist.sys.adoptable");
                byte b = -1;
                int iHashCode = str2.hashCode();
                if (iHashCode != 464944051) {
                    if (iHashCode == 1528363547 && str2.equals("force_off")) {
                        b = 1;
                    }
                } else if (str2.equals("force_on")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        i |= 1;
                        break;
                    case 1:
                        i &= -2;
                        break;
                }
                StorageManagerService.this.mDisks.put(str, new DiskInfo(str, i));
            }
        }

        @Override
        public void onDiskScanned(String str) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo diskInfo = (DiskInfo) StorageManagerService.this.mDisks.get(str);
                if (diskInfo != null) {
                    StorageManagerService.this.onDiskScannedLocked(diskInfo);
                }
            }
        }

        @Override
        public void onDiskMetadataChanged(String str, long j, String str2, String str3) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo diskInfo = (DiskInfo) StorageManagerService.this.mDisks.get(str);
                if (diskInfo != null) {
                    diskInfo.size = j;
                    diskInfo.label = str2;
                    diskInfo.sysPath = str3;
                }
            }
        }

        @Override
        public void onDiskDestroyed(String str) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo diskInfo = (DiskInfo) StorageManagerService.this.mDisks.remove(str);
                if (diskInfo != null) {
                    StorageManagerService.this.mCallbacks.notifyDiskDestroyed(diskInfo);
                }
            }
        }

        @Override
        public void onVolumeCreated(String str, int i, String str2, String str3) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo volumeInfo = new VolumeInfo(str, i, (DiskInfo) StorageManagerService.this.mDisks.get(str2), str3);
                StorageManagerService.this.mVolumes.put(str, volumeInfo);
                StorageManagerService.this.onVolumeCreatedLocked(volumeInfo);
            }
        }

        @Override
        public void onVolumeStateChanged(String str, int i) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo volumeInfo = StorageManagerService.this.mVolumes.get(str);
                if (volumeInfo != null) {
                    int i2 = volumeInfo.state;
                    volumeInfo.state = i;
                    StorageManagerService.this.onVolumeStateChangedLocked(volumeInfo, i2, i);
                }
            }
        }

        @Override
        public void onVolumeMetadataChanged(String str, String str2, String str3, String str4) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo volumeInfo = StorageManagerService.this.mVolumes.get(str);
                if (volumeInfo != null) {
                    volumeInfo.fsType = str2;
                    volumeInfo.fsUuid = str3;
                    volumeInfo.fsLabel = str4;
                }
            }
        }

        @Override
        public void onVolumePathChanged(String str, String str2) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo volumeInfo = StorageManagerService.this.mVolumes.get(str);
                if (volumeInfo != null) {
                    volumeInfo.path = str2;
                }
            }
        }

        @Override
        public void onVolumeInternalPathChanged(String str, String str2) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo volumeInfo = StorageManagerService.this.mVolumes.get(str);
                if (volumeInfo != null) {
                    volumeInfo.internalPath = str2;
                }
            }
        }

        @Override
        public void onVolumeDestroyed(String str) {
            synchronized (StorageManagerService.this.mLock) {
                StorageManagerService.this.mVolumes.remove(str);
            }
        }
    };

    public static class Lifecycle extends SystemService {
        protected StorageManagerService mStorageManagerService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mStorageManagerService = new StorageManagerService(getContext());
            publishBinderService("mount", this.mStorageManagerService);
            this.mStorageManagerService.start();
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mStorageManagerService.systemReady();
            } else if (i == 1000) {
                this.mStorageManagerService.bootCompleted();
            }
        }

        @Override
        public void onSwitchUser(int i) {
            this.mStorageManagerService.mCurrentUserId = i;
        }

        @Override
        public void onUnlockUser(int i) {
            this.mStorageManagerService.onUnlockUser(i);
        }

        @Override
        public void onCleanupUser(int i) {
            this.mStorageManagerService.onCleanupUser(i);
        }
    }

    private VolumeInfo findVolumeByIdOrThrow(String str) {
        synchronized (this.mLock) {
            VolumeInfo volumeInfo = this.mVolumes.get(str);
            if (volumeInfo != null) {
                return volumeInfo;
            }
            throw new IllegalArgumentException("No volume found for ID " + str);
        }
    }

    private String findVolumeIdForPathOrThrow(String str) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i);
                if (volumeInfoValueAt.path != null && str.startsWith(volumeInfoValueAt.path)) {
                    return volumeInfoValueAt.id;
                }
            }
            throw new IllegalArgumentException("No volume found for path " + str);
        }
    }

    private VolumeRecord findRecordForPath(String str) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i);
                if (volumeInfoValueAt.path != null && str.startsWith(volumeInfoValueAt.path)) {
                    return this.mRecords.get(volumeInfoValueAt.fsUuid);
                }
            }
            return null;
        }
    }

    private String scrubPath(String str) {
        if (str.startsWith(Environment.getDataDirectory().getAbsolutePath())) {
            return "internal";
        }
        VolumeRecord volumeRecordFindRecordForPath = findRecordForPath(str);
        if (volumeRecordFindRecordForPath == null || volumeRecordFindRecordForPath.createdMillis == 0) {
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        return "ext:" + ((int) ((System.currentTimeMillis() - volumeRecordFindRecordForPath.createdMillis) / UnixCalendar.WEEK_IN_MILLIS)) + "w";
    }

    private VolumeInfo findStorageForUuid(String str) {
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, str)) {
            return storageManager.findVolumeById("emulated");
        }
        if (Objects.equals("primary_physical", str)) {
            return storageManager.getPrimaryPhysicalVolume();
        }
        return storageManager.findEmulatedForPrivate(storageManager.findVolumeByUuid(str));
    }

    private boolean shouldBenchmark() {
        long j = Settings.Global.getLong(this.mContext.getContentResolver(), "storage_benchmark_interval", UnixCalendar.WEEK_IN_MILLIS);
        if (j == -1) {
            return false;
        }
        if (j == 0) {
            return true;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i);
                VolumeRecord volumeRecord = this.mRecords.get(volumeInfoValueAt.fsUuid);
                if (volumeInfoValueAt.isMountedWritable() && volumeRecord != null && System.currentTimeMillis() - volumeRecord.lastBenchMillis >= j) {
                    return true;
                }
            }
            return false;
        }
    }

    private CountDownLatch findOrCreateDiskScanLatch(String str) {
        CountDownLatch countDownLatch;
        synchronized (this.mLock) {
            countDownLatch = this.mDiskScanLatches.get(str);
            if (countDownLatch == null) {
                countDownLatch = new CountDownLatch(1);
                this.mDiskScanLatches.put(str, countDownLatch);
            }
        }
        return countDownLatch;
    }

    class ObbState implements IBinder.DeathRecipient {
        final String canonicalPath;
        final int nonce;
        final int ownerGid;
        final String rawPath;
        final IObbActionListener token;
        String volId;

        public ObbState(String str, String str2, int i, IObbActionListener iObbActionListener, int i2, String str3) {
            this.rawPath = str;
            this.canonicalPath = str2;
            this.ownerGid = UserHandle.getSharedAppGid(i);
            this.token = iObbActionListener;
            this.nonce = i2;
            this.volId = str3;
        }

        public IBinder getBinder() {
            return this.token.asBinder();
        }

        @Override
        public void binderDied() {
            StorageManagerService.this.mObbActionHandler.sendMessage(StorageManagerService.this.mObbActionHandler.obtainMessage(1, StorageManagerService.this.new UnmountObbAction(this, true)));
        }

        public void link() throws RemoteException {
            getBinder().linkToDeath(this, 0);
        }

        public void unlink() {
            getBinder().unlinkToDeath(this, 0);
        }

        public String toString() {
            return "ObbState{rawPath=" + this.rawPath + ",canonicalPath=" + this.canonicalPath + ",ownerGid=" + this.ownerGid + ",token=" + this.token + ",binder=" + getBinder() + ",volId=" + this.volId + '}';
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            StorageManagerService.this.mObbActionHandler.sendMessage(StorageManagerService.this.mObbActionHandler.obtainMessage(2, IMediaContainerService.Stub.asInterface(iBinder)));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    }

    class StorageManagerServiceHandler extends Handler {
        public StorageManagerServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            boolean z;
            switch (message.what) {
                case 1:
                    StorageManagerService.this.handleSystemReady();
                    break;
                case 2:
                    StorageManagerService.this.handleDaemonConnected();
                    break;
                case 3:
                    IStorageShutdownObserver iStorageShutdownObserver = (IStorageShutdownObserver) message.obj;
                    try {
                        StorageManagerService.this.mVold.shutdown();
                        z = true;
                    } catch (Exception e) {
                        Slog.wtf(StorageManagerService.TAG, e);
                        z = false;
                    }
                    if (iStorageShutdownObserver != null) {
                        try {
                            iStorageShutdownObserver.onShutDownComplete(z ? 0 : -1);
                        } catch (Exception e2) {
                            return;
                        }
                    }
                    break;
                case 4:
                    Slog.i(StorageManagerService.TAG, "Running fstrim idle maintenance");
                    try {
                        StorageManagerService.this.mLastMaintenance = System.currentTimeMillis();
                        StorageManagerService.this.mLastMaintenanceFile.setLastModified(StorageManagerService.this.mLastMaintenance);
                    } catch (Exception e3) {
                        Slog.e(StorageManagerService.TAG, "Unable to record last fstrim!");
                    }
                    StorageManagerService.this.fstrim(0, null);
                    Runnable runnable = (Runnable) message.obj;
                    if (runnable != null) {
                        runnable.run();
                    }
                    break;
                case 5:
                    VolumeInfo volumeInfo = (VolumeInfo) message.obj;
                    if (!StorageManagerService.this.isMountDisallowed(volumeInfo)) {
                        try {
                            StorageManagerService.this.mVold.mount(volumeInfo.id, volumeInfo.mountFlags, volumeInfo.mountUserId);
                        } catch (Exception e4) {
                            Slog.wtf(StorageManagerService.TAG, e4);
                            return;
                        }
                    } else {
                        Slog.i(StorageManagerService.TAG, "Ignoring mount " + volumeInfo.getId() + " due to policy");
                    }
                    break;
                case 6:
                    StorageVolume storageVolume = (StorageVolume) message.obj;
                    String state = storageVolume.getState();
                    Slog.d(StorageManagerService.TAG, "Volume " + storageVolume.getId() + " broadcasting " + state + " to " + storageVolume.getOwner());
                    String broadcastForEnvironment = VolumeInfo.getBroadcastForEnvironment(state);
                    if (broadcastForEnvironment != null) {
                        Intent intent = new Intent(broadcastForEnvironment, Uri.fromFile(storageVolume.getPathFile()));
                        intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", storageVolume);
                        intent.addFlags(83886080);
                        StorageManagerService.this.mContext.sendBroadcastAsUser(intent, storageVolume.getOwner());
                    }
                    break;
                case 7:
                    StorageManagerService.this.mContext.sendBroadcastAsUser((Intent) message.obj, UserHandle.ALL, "android.permission.WRITE_MEDIA_STORAGE");
                    break;
                case 8:
                    StorageManagerService.this.unmount(((VolumeInfo) message.obj).getId());
                    break;
                case 9:
                    VolumeRecord volumeRecord = (VolumeRecord) message.obj;
                    StorageManagerService.this.forgetPartition(volumeRecord.partGuid, volumeRecord.fsUuid);
                    break;
                case 10:
                    StorageManagerService.this.resetIfReadyAndConnected();
                    break;
                case 11:
                    Slog.i(StorageManagerService.TAG, "Running idle maintenance");
                    StorageManagerService.this.runIdleMaint((Runnable) message.obj);
                    break;
                case 12:
                    Slog.i(StorageManagerService.TAG, "Aborting idle maintenance");
                    StorageManagerService.this.abortIdleMaint((Runnable) message.obj);
                    break;
            }
        }
    }

    private void waitForLatch(CountDownLatch countDownLatch, String str, long j) throws TimeoutException {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        while (!countDownLatch.await(5000L, TimeUnit.MILLISECONDS)) {
            try {
                Slog.w(TAG, "Thread " + Thread.currentThread().getName() + " still waiting for " + str + "...");
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupt while waiting for " + str);
            }
            if (j > 0 && SystemClock.elapsedRealtime() > jElapsedRealtime + j) {
                throw new TimeoutException("Thread " + Thread.currentThread().getName() + " gave up waiting for " + str + " after " + j + "ms");
            }
        }
    }

    private void handleSystemReady() {
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        MountServiceIdler.scheduleIdlePass(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zram_enabled"), false, new ContentObserver(null) {
            @Override
            public void onChange(boolean z) {
                StorageManagerService.this.refreshZramSettings();
            }
        });
        refreshZramSettings();
    }

    private void refreshZramSettings() {
        String str = SystemProperties.get(ZRAM_ENABLED_PROPERTY);
        if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str)) {
            return;
        }
        String str2 = Settings.Global.getInt(this.mContext.getContentResolver(), "zram_enabled", 1) != 0 ? "1" : "0";
        if (!str2.equals(str)) {
            SystemProperties.set(ZRAM_ENABLED_PROPERTY, str2);
        }
    }

    @Deprecated
    private void killMediaProvider(List<UserInfo> list) {
        ProviderInfo providerInfoResolveContentProvider;
        if (list == null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            for (UserInfo userInfo : list) {
                if (!userInfo.isSystemOnly() && (providerInfoResolveContentProvider = this.mPms.resolveContentProvider("media", 786432, userInfo.id)) != null) {
                    try {
                        ActivityManager.getService().killApplication(providerInfoResolveContentProvider.applicationInfo.packageName, UserHandle.getAppId(providerInfoResolveContentProvider.applicationInfo.uid), -1, "vold reset");
                        break;
                    } catch (RemoteException e) {
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mLock")
    private void addInternalVolumeLocked() {
        VolumeInfo volumeInfo = new VolumeInfo("private", 1, (DiskInfo) null, (String) null);
        volumeInfo.state = 2;
        volumeInfo.path = Environment.getDataDirectory().getAbsolutePath();
        this.mVolumes.put(volumeInfo.id, volumeInfo);
    }

    private void initIfReadyAndConnected() {
        Slog.d(TAG, "Thinking about init, mSystemReady=" + this.mSystemReady + ", mDaemonConnected=" + this.mDaemonConnected);
        if (this.mSystemReady && this.mDaemonConnected && !StorageManager.isFileEncryptedNativeOnly()) {
            boolean zIsFileEncryptedEmulatedOnly = StorageManager.isFileEncryptedEmulatedOnly();
            Slog.d(TAG, "Setting up emulation state, initlocked=" + zIsFileEncryptedEmulatedOnly);
            for (UserInfo userInfo : ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers()) {
                if (zIsFileEncryptedEmulatedOnly) {
                    try {
                        this.mVold.lockUserKey(userInfo.id);
                    } catch (Exception e) {
                        Slog.wtf(TAG, e);
                    }
                } else {
                    this.mVold.unlockUserKey(userInfo.id, userInfo.serialNumber, encodeBytes(null), encodeBytes(null));
                }
            }
        }
    }

    private void resetIfReadyAndConnected() {
        int[] iArr;
        Slog.d(TAG, "Thinking about reset, mSystemReady=" + this.mSystemReady + ", mDaemonConnected=" + this.mDaemonConnected);
        if (this.mSystemReady && this.mDaemonConnected) {
            List users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            killMediaProvider(users);
            synchronized (this.mLock) {
                iArr = this.mSystemUnlockedUsers;
                this.mDisks.clear();
                this.mVolumes.clear();
                addInternalVolumeLocked();
            }
            try {
                this.mVold.reset();
                for (UserInfo userInfo : users) {
                    this.mVold.onUserAdded(userInfo.id, userInfo.serialNumber);
                }
                for (int i : iArr) {
                    this.mVold.onUserStarted(i);
                    this.mStoraged.onUserStarted(i);
                }
                this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
            } catch (Exception e) {
                Slog.wtf(TAG, e);
            }
        }
    }

    private void onUnlockUser(int i) {
        Slog.d(TAG, "onUnlockUser " + i);
        try {
            this.mVold.onUserStarted(i);
            this.mStoraged.onUserStarted(i);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        synchronized (this.mLock) {
            for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
                VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i2);
                if (volumeInfoValueAt.isVisibleForRead(i) && volumeInfoValueAt.isMountedReadable()) {
                    StorageVolume storageVolumeBuildStorageVolume = volumeInfoValueAt.buildStorageVolume(this.mContext, i, false);
                    this.mHandler.obtainMessage(6, storageVolumeBuildStorageVolume).sendToTarget();
                    String environmentForState = VolumeInfo.getEnvironmentForState(volumeInfoValueAt.getState());
                    this.mCallbacks.notifyStorageStateChanged(storageVolumeBuildStorageVolume.getPath(), environmentForState, environmentForState);
                }
            }
            this.mSystemUnlockedUsers = ArrayUtils.appendInt(this.mSystemUnlockedUsers, i);
        }
    }

    private void onCleanupUser(int i) {
        Slog.d(TAG, "onCleanupUser " + i);
        try {
            this.mVold.onUserStopped(i);
            this.mStoraged.onUserStopped(i);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        synchronized (this.mLock) {
            this.mSystemUnlockedUsers = ArrayUtils.removeInt(this.mSystemUnlockedUsers, i);
        }
    }

    public void onAwakeStateChanged(boolean z) {
    }

    public void onKeyguardStateChanged(boolean z) {
        this.mSecureKeyguardShowing = z && ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isDeviceSecure();
        try {
            this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    void runIdleMaintenance(Runnable runnable) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, runnable));
    }

    public void runMaintenance() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        runIdleMaintenance(null);
    }

    public long lastMaintenance() {
        return this.mLastMaintenance;
    }

    public void onDaemonConnected() {
        this.mDaemonConnected = true;
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    private void handleDaemonConnected() {
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(SystemProperties.get("vold.encrypt_progress"))) {
            copyLocaleFromMountService();
        }
    }

    private void copyLocaleFromMountService() {
        try {
            String field = getField("SystemLocale");
            if (TextUtils.isEmpty(field)) {
                return;
            }
            Slog.d(TAG, "Got locale " + field + " from mount service");
            Locale localeForLanguageTag = Locale.forLanguageTag(field);
            Configuration configuration = new Configuration();
            configuration.setLocale(localeForLanguageTag);
            try {
                ActivityManager.getService().updatePersistentConfiguration(configuration);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error setting system locale from mount service", e);
            }
            Slog.d(TAG, "Setting system properties to " + field + " from mount service");
            SystemProperties.set("persist.sys.locale", localeForLanguageTag.toLanguageTag());
        } catch (RemoteException e2) {
        }
    }

    @GuardedBy("mLock")
    private void onDiskScannedLocked(DiskInfo diskInfo) {
        int i = 0;
        for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
            if (Objects.equals(diskInfo.id, this.mVolumes.valueAt(i2).getDiskId())) {
                i++;
            }
        }
        Intent intent = new Intent("android.os.storage.action.DISK_SCANNED");
        intent.addFlags(83886080);
        intent.putExtra("android.os.storage.extra.DISK_ID", diskInfo.id);
        intent.putExtra("android.os.storage.extra.VOLUME_COUNT", i);
        this.mHandler.obtainMessage(7, intent).sendToTarget();
        CountDownLatch countDownLatchRemove = this.mDiskScanLatches.remove(diskInfo.id);
        if (countDownLatchRemove != null) {
            countDownLatchRemove.countDown();
        }
        diskInfo.volumeCount = i;
        this.mCallbacks.notifyDiskScanned(diskInfo, i);
    }

    @GuardedBy("mLock")
    private void onVolumeCreatedLocked(VolumeInfo volumeInfo) {
        if (this.mPms.isOnlyCoreApps()) {
            Slog.d(TAG, "System booted in core-only mode; ignoring volume " + volumeInfo.getId());
            return;
        }
        if (volumeInfo.type == 2) {
            VolumeInfo volumeInfoFindPrivateForEmulated = ((StorageManager) this.mContext.getSystemService(StorageManager.class)).findPrivateForEmulated(volumeInfo);
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid) && "private".equals(volumeInfoFindPrivateForEmulated.id)) {
                Slog.v(TAG, "Found primary storage at " + volumeInfo);
                volumeInfo.mountFlags = volumeInfo.mountFlags | 1;
                volumeInfo.mountFlags = volumeInfo.mountFlags | 2;
                this.mHandler.obtainMessage(5, volumeInfo).sendToTarget();
                return;
            }
            if (Objects.equals(volumeInfoFindPrivateForEmulated.fsUuid, this.mPrimaryStorageUuid)) {
                Slog.v(TAG, "Found primary storage at " + volumeInfo);
                volumeInfo.mountFlags = volumeInfo.mountFlags | 1;
                volumeInfo.mountFlags = volumeInfo.mountFlags | 2;
                this.mHandler.obtainMessage(5, volumeInfo).sendToTarget();
                return;
            }
            return;
        }
        if (volumeInfo.type == 0) {
            if (Objects.equals("primary_physical", this.mPrimaryStorageUuid) && volumeInfo.disk.isDefaultPrimary()) {
                Slog.v(TAG, "Found primary storage at " + volumeInfo);
                volumeInfo.mountFlags = volumeInfo.mountFlags | 1;
                volumeInfo.mountFlags = volumeInfo.mountFlags | 2;
            }
            if (volumeInfo.disk.isAdoptable()) {
                volumeInfo.mountFlags |= 2;
            }
            volumeInfo.mountUserId = this.mCurrentUserId;
            this.mHandler.obtainMessage(5, volumeInfo).sendToTarget();
            return;
        }
        if (volumeInfo.type == 1) {
            this.mHandler.obtainMessage(5, volumeInfo).sendToTarget();
            return;
        }
        Slog.d(TAG, "Skipping automatic mounting of " + volumeInfo);
    }

    private boolean isBroadcastWorthy(android.os.storage.VolumeInfo r3) {
        switch (r3.getType()) {
            case 0:
            case 1:
            case 2:
                switch (r3.getState()) {
                }
        }
        return false;
    }

    @GuardedBy("mLock")
    private void onVolumeStateChangedLocked(VolumeInfo volumeInfo, int i, int i2) {
        if (volumeInfo.isMountedReadable() && !TextUtils.isEmpty(volumeInfo.fsUuid)) {
            VolumeRecord volumeRecord = this.mRecords.get(volumeInfo.fsUuid);
            if (volumeRecord == null) {
                VolumeRecord volumeRecord2 = new VolumeRecord(volumeInfo.type, volumeInfo.fsUuid);
                volumeRecord2.partGuid = volumeInfo.partGuid;
                volumeRecord2.createdMillis = System.currentTimeMillis();
                if (volumeInfo.type == 1) {
                    volumeRecord2.nickname = volumeInfo.disk.getDescription();
                }
                this.mRecords.put(volumeRecord2.fsUuid, volumeRecord2);
                writeSettingsLocked();
            } else if (TextUtils.isEmpty(volumeRecord.partGuid)) {
                volumeRecord.partGuid = volumeInfo.partGuid;
                writeSettingsLocked();
            }
        }
        this.mCallbacks.notifyVolumeStateChanged(volumeInfo, i, i2);
        if (this.mBootCompleted && isBroadcastWorthy(volumeInfo)) {
            Intent intent = new Intent("android.os.storage.action.VOLUME_STATE_CHANGED");
            intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.id);
            intent.putExtra("android.os.storage.extra.VOLUME_STATE", i2);
            intent.putExtra("android.os.storage.extra.FS_UUID", volumeInfo.fsUuid);
            intent.addFlags(83886080);
            this.mHandler.obtainMessage(7, intent).sendToTarget();
        }
        String environmentForState = VolumeInfo.getEnvironmentForState(i);
        String environmentForState2 = VolumeInfo.getEnvironmentForState(i2);
        if (!Objects.equals(environmentForState, environmentForState2)) {
            for (int i3 : this.mSystemUnlockedUsers) {
                if (volumeInfo.isVisibleForRead(i3)) {
                    StorageVolume storageVolumeBuildStorageVolume = volumeInfo.buildStorageVolume(this.mContext, i3, false);
                    this.mHandler.obtainMessage(6, storageVolumeBuildStorageVolume).sendToTarget();
                    this.mCallbacks.notifyStorageStateChanged(storageVolumeBuildStorageVolume.getPath(), environmentForState, environmentForState2);
                }
            }
        }
        if (volumeInfo.type == 0 && volumeInfo.state == 5) {
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(5, volumeInfo.path));
        }
        maybeLogMediaMount(volumeInfo, i2);
    }

    private void maybeLogMediaMount(VolumeInfo volumeInfo, int i) {
        DiskInfo disk;
        if (!SecurityLog.isLoggingEnabled() || (disk = volumeInfo.getDisk()) == null || (disk.flags & 12) == 0) {
            return;
        }
        String strTrim = disk.label != null ? disk.label.trim() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (i == 2 || i == 3) {
            SecurityLog.writeEvent(210013, new Object[]{volumeInfo.path, strTrim});
        } else if (i == 0 || i == 8) {
            SecurityLog.writeEvent(210014, new Object[]{volumeInfo.path, strTrim});
        }
    }

    @GuardedBy("mLock")
    private void onMoveStatusLocked(int i) {
        if (this.mMoveCallback == null) {
            Slog.w(TAG, "Odd, status but no move requested");
            return;
        }
        try {
            this.mMoveCallback.onStatusChanged(-1, i, -1L);
        } catch (RemoteException e) {
        }
        if (i == 82) {
            Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " copy phase finshed; persisting");
            this.mPrimaryStorageUuid = this.mMoveTargetUuid;
            writeSettingsLocked();
        }
        if (PackageManager.isMoveStatusFinished(i)) {
            Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " finished with status " + i);
            this.mMoveCallback = null;
            this.mMoveTargetUuid = null;
        }
    }

    private void enforcePermission(String str) {
        this.mContext.enforceCallingOrSelfPermission(str, str);
    }

    private boolean isMountDisallowed(VolumeInfo volumeInfo) {
        boolean zHasUserRestriction;
        boolean zHasUserRestriction2;
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (volumeInfo.disk != null && volumeInfo.disk.isUsb()) {
            zHasUserRestriction = userManager.hasUserRestriction("no_usb_file_transfer", Binder.getCallingUserHandle());
        } else {
            zHasUserRestriction = false;
        }
        if (volumeInfo.type == 0 || volumeInfo.type == 1) {
            zHasUserRestriction2 = userManager.hasUserRestriction("no_physical_media", Binder.getCallingUserHandle());
        } else {
            zHasUserRestriction2 = false;
        }
        return zHasUserRestriction || zHasUserRestriction2;
    }

    private void enforceAdminUser() {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!userManager.getUserInfo(callingUserId).isAdmin()) {
                throw new SecurityException("Only admin users can adopt sd cards");
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public StorageManagerService(Context context) {
        sSelf = this;
        this.mContext = context;
        this.mCallbacks = new Callbacks(FgThread.get().getLooper());
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPms = (PackageManagerService) ServiceManager.getService(com.android.server.pm.Settings.ATTR_PACKAGE);
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new StorageManagerServiceHandler(handlerThread.getLooper());
        this.mObbActionHandler = new ObbActionHandler(IoThread.get().getLooper());
        this.mLastMaintenanceFile = new File(new File(Environment.getDataDirectory(), "system"), LAST_FSTRIM_FILE);
        if (!this.mLastMaintenanceFile.exists()) {
            try {
                new FileOutputStream(this.mLastMaintenanceFile).close();
            } catch (IOException e) {
                Slog.e(TAG, "Unable to create fstrim record " + this.mLastMaintenanceFile.getPath());
            }
        } else {
            this.mLastMaintenance = this.mLastMaintenanceFile.lastModified();
        }
        this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "storage.xml"), "storage-settings");
        synchronized (this.mLock) {
            readSettingsLocked();
        }
        LocalServices.addService(StorageManagerInternal.class, this.mStorageManagerInternal);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserReceiver, intentFilter, null, this.mHandler);
        synchronized (this.mLock) {
            addInternalVolumeLocked();
        }
    }

    public void start() {
        connect();
    }

    private void connect() {
        IBinder service = ServiceManager.getService("storaged");
        if (service != null) {
            try {
                service.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "storaged died; reconnecting");
                        StorageManagerService.this.mStoraged = null;
                        StorageManagerService.this.connect();
                    }
                }, 0);
            } catch (RemoteException e) {
                service = null;
            }
        }
        if (service != null) {
            this.mStoraged = IStoraged.Stub.asInterface(service);
        } else {
            Slog.w(TAG, "storaged not found; trying again");
        }
        IBinder service2 = ServiceManager.getService("vold");
        if (service2 != null) {
            try {
                service2.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "vold died; reconnecting");
                        StorageManagerService.this.mVold = null;
                        StorageManagerService.this.connect();
                    }
                }, 0);
            } catch (RemoteException e2) {
                service2 = null;
            }
        }
        if (service2 != null) {
            this.mVold = IVold.Stub.asInterface(service2);
            try {
                this.mVold.setListener(this.mListener);
            } catch (RemoteException e3) {
                this.mVold = null;
                Slog.w(TAG, "vold listener rejected; trying again", e3);
            }
        } else {
            Slog.w(TAG, "vold not found; trying again");
        }
        if (this.mStoraged == null || this.mVold == null) {
            BackgroundThread.getHandler().postDelayed(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.connect();
                }
            }, 1000L);
        } else {
            onDaemonConnected();
        }
    }

    private void systemReady() {
        ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
        this.mSystemReady = true;
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void bootCompleted() {
        this.mBootCompleted = true;
    }

    private String getDefaultPrimaryStorageUuid() {
        if (SystemProperties.getBoolean("ro.vold.primary_physical", false)) {
            return "primary_physical";
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    @GuardedBy("mLock")
    private void readSettingsLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        XmlPullParserException e;
        IOException e2;
        this.mRecords.clear();
        this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
        try {
            try {
                fileInputStreamOpenRead = this.mSettingsFile.openRead();
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    while (true) {
                        int next = xmlPullParserNewPullParser.next();
                        boolean z = true;
                        if (next == 1) {
                            break;
                        }
                        if (next == 2) {
                            String name = xmlPullParserNewPullParser.getName();
                            if (TAG_VOLUMES.equals(name)) {
                                int intAttribute = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, ATTR_VERSION, 1);
                                boolean z2 = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                                if (intAttribute < 3 && (intAttribute < 2 || z2)) {
                                    z = false;
                                }
                                if (z) {
                                    this.mPrimaryStorageUuid = XmlUtils.readStringAttribute(xmlPullParserNewPullParser, ATTR_PRIMARY_STORAGE_UUID);
                                }
                            } else if (TAG_VOLUME.equals(name)) {
                                VolumeRecord volumeRecord = readVolumeRecord(xmlPullParserNewPullParser);
                                this.mRecords.put(volumeRecord.fsUuid, volumeRecord);
                            }
                        }
                    }
                } catch (FileNotFoundException e3) {
                } catch (IOException e4) {
                    e2 = e4;
                    Slog.wtf(TAG, "Failed reading metadata", e2);
                } catch (XmlPullParserException e5) {
                    e = e5;
                    Slog.wtf(TAG, "Failed reading metadata", e);
                }
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        } catch (FileNotFoundException e6) {
            fileInputStreamOpenRead = null;
        } catch (IOException e7) {
            fileInputStreamOpenRead = null;
            e2 = e7;
        } catch (XmlPullParserException e8) {
            fileInputStreamOpenRead = null;
            e = e8;
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        IoUtils.closeQuietly(fileInputStreamOpenRead);
    }

    @GuardedBy("mLock")
    private void writeSettingsLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        try {
            fileOutputStreamStartWrite = this.mSettingsFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_VOLUMES);
                XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 3);
                XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_PRIMARY_STORAGE_UUID, this.mPrimaryStorageUuid);
                int size = this.mRecords.size();
                for (int i = 0; i < size; i++) {
                    writeVolumeRecord(fastXmlSerializer, this.mRecords.valueAt(i));
                }
                fastXmlSerializer.endTag(null, TAG_VOLUMES);
                fastXmlSerializer.endDocument();
                this.mSettingsFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e) {
                if (fileOutputStreamStartWrite != null) {
                    this.mSettingsFile.failWrite(fileOutputStreamStartWrite);
                }
            }
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
        }
    }

    public static VolumeRecord readVolumeRecord(XmlPullParser xmlPullParser) throws IOException {
        VolumeRecord volumeRecord = new VolumeRecord(XmlUtils.readIntAttribute(xmlPullParser, "type"), XmlUtils.readStringAttribute(xmlPullParser, ATTR_FS_UUID));
        volumeRecord.partGuid = XmlUtils.readStringAttribute(xmlPullParser, ATTR_PART_GUID);
        volumeRecord.nickname = XmlUtils.readStringAttribute(xmlPullParser, ATTR_NICKNAME);
        volumeRecord.userFlags = XmlUtils.readIntAttribute(xmlPullParser, ATTR_USER_FLAGS);
        volumeRecord.createdMillis = XmlUtils.readLongAttribute(xmlPullParser, ATTR_CREATED_MILLIS);
        volumeRecord.lastTrimMillis = XmlUtils.readLongAttribute(xmlPullParser, ATTR_LAST_TRIM_MILLIS);
        volumeRecord.lastBenchMillis = XmlUtils.readLongAttribute(xmlPullParser, ATTR_LAST_BENCH_MILLIS);
        return volumeRecord;
    }

    public static void writeVolumeRecord(XmlSerializer xmlSerializer, VolumeRecord volumeRecord) throws IOException {
        xmlSerializer.startTag(null, TAG_VOLUME);
        XmlUtils.writeIntAttribute(xmlSerializer, "type", volumeRecord.type);
        XmlUtils.writeStringAttribute(xmlSerializer, ATTR_FS_UUID, volumeRecord.fsUuid);
        XmlUtils.writeStringAttribute(xmlSerializer, ATTR_PART_GUID, volumeRecord.partGuid);
        XmlUtils.writeStringAttribute(xmlSerializer, ATTR_NICKNAME, volumeRecord.nickname);
        XmlUtils.writeIntAttribute(xmlSerializer, ATTR_USER_FLAGS, volumeRecord.userFlags);
        XmlUtils.writeLongAttribute(xmlSerializer, ATTR_CREATED_MILLIS, volumeRecord.createdMillis);
        XmlUtils.writeLongAttribute(xmlSerializer, ATTR_LAST_TRIM_MILLIS, volumeRecord.lastTrimMillis);
        XmlUtils.writeLongAttribute(xmlSerializer, ATTR_LAST_BENCH_MILLIS, volumeRecord.lastBenchMillis);
        xmlSerializer.endTag(null, TAG_VOLUME);
    }

    public void registerListener(IStorageEventListener iStorageEventListener) {
        this.mCallbacks.register(iStorageEventListener);
    }

    public void unregisterListener(IStorageEventListener iStorageEventListener) {
        this.mCallbacks.unregister(iStorageEventListener);
    }

    public void shutdown(IStorageShutdownObserver iStorageShutdownObserver) {
        enforcePermission("android.permission.SHUTDOWN");
        Slog.i(TAG, "Shutting down");
        this.mHandler.obtainMessage(3, iStorageShutdownObserver).sendToTarget();
    }

    public void mount(String str) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        VolumeInfo volumeInfoFindVolumeByIdOrThrow = findVolumeByIdOrThrow(str);
        if (isMountDisallowed(volumeInfoFindVolumeByIdOrThrow)) {
            throw new SecurityException("Mounting " + str + " restricted by policy");
        }
        try {
            this.mVold.mount(volumeInfoFindVolumeByIdOrThrow.id, volumeInfoFindVolumeByIdOrThrow.mountFlags, volumeInfoFindVolumeByIdOrThrow.mountUserId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void unmount(String str) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        try {
            this.mVold.unmount(findVolumeByIdOrThrow(str).id);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void format(String str) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.format(findVolumeByIdOrThrow(str).id, UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void benchmark(String str, final IVoldTaskListener iVoldTaskListener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.benchmark(str, new IVoldTaskListener.Stub() {
                @Override
                public void onStatus(int i, PersistableBundle persistableBundle) {
                    StorageManagerService.this.dispatchOnStatus(iVoldTaskListener, i, persistableBundle);
                }

                @Override
                public void onFinished(int i, PersistableBundle persistableBundle) {
                    StorageManagerService.this.dispatchOnFinished(iVoldTaskListener, i, persistableBundle);
                    String string = persistableBundle.getString("path");
                    String string2 = persistableBundle.getString("ident");
                    long j = persistableBundle.getLong("create");
                    long j2 = persistableBundle.getLong("run");
                    long j3 = persistableBundle.getLong("destroy");
                    ((DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class)).addText(StorageManagerService.TAG_STORAGE_BENCHMARK, StorageManagerService.this.scrubPath(string) + " " + string2 + " " + j + " " + j2 + " " + j3);
                    synchronized (StorageManagerService.this.mLock) {
                        VolumeRecord volumeRecordFindRecordForPath = StorageManagerService.this.findRecordForPath(string);
                        if (volumeRecordFindRecordForPath != null) {
                            volumeRecordFindRecordForPath.lastBenchMillis = System.currentTimeMillis();
                            StorageManagerService.this.writeSettingsLocked();
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void partitionPublic(String str) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        CountDownLatch countDownLatchFindOrCreateDiskScanLatch = findOrCreateDiskScanLatch(str);
        try {
            this.mVold.partition(str, 0, -1);
            waitForLatch(countDownLatchFindOrCreateDiskScanLatch, "partitionPublic", 180000L);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionPrivate(String str) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch countDownLatchFindOrCreateDiskScanLatch = findOrCreateDiskScanLatch(str);
        try {
            this.mVold.partition(str, 1, -1);
            waitForLatch(countDownLatchFindOrCreateDiskScanLatch, "partitionPrivate", 180000L);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionMixed(String str, int i) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch countDownLatchFindOrCreateDiskScanLatch = findOrCreateDiskScanLatch(str);
        try {
            this.mVold.partition(str, 2, i);
            waitForLatch(countDownLatchFindOrCreateDiskScanLatch, "partitionMixed", 180000L);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void setVolumeNickname(String str, String str2) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(str);
        synchronized (this.mLock) {
            VolumeRecord volumeRecord = this.mRecords.get(str);
            volumeRecord.nickname = str2;
            this.mCallbacks.notifyVolumeRecordChanged(volumeRecord);
            writeSettingsLocked();
        }
    }

    public void setVolumeUserFlags(String str, int i, int i2) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(str);
        synchronized (this.mLock) {
            VolumeRecord volumeRecord = this.mRecords.get(str);
            volumeRecord.userFlags = (i & i2) | (volumeRecord.userFlags & (~i2));
            this.mCallbacks.notifyVolumeRecordChanged(volumeRecord);
            writeSettingsLocked();
        }
    }

    public void forgetVolume(String str) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(str);
        synchronized (this.mLock) {
            VolumeRecord volumeRecordRemove = this.mRecords.remove(str);
            if (volumeRecordRemove != null && !TextUtils.isEmpty(volumeRecordRemove.partGuid)) {
                this.mHandler.obtainMessage(9, volumeRecordRemove).sendToTarget();
            }
            this.mCallbacks.notifyVolumeForgotten(str);
            if (Objects.equals(this.mPrimaryStorageUuid, str)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
                this.mHandler.obtainMessage(10).sendToTarget();
            }
            writeSettingsLocked();
        }
    }

    public void forgetAllVolumes() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                String strKeyAt = this.mRecords.keyAt(i);
                VolumeRecord volumeRecordValueAt = this.mRecords.valueAt(i);
                if (!TextUtils.isEmpty(volumeRecordValueAt.partGuid)) {
                    this.mHandler.obtainMessage(9, volumeRecordValueAt).sendToTarget();
                }
                this.mCallbacks.notifyVolumeForgotten(strKeyAt);
            }
            this.mRecords.clear();
            if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
            }
            writeSettingsLocked();
            this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    private void forgetPartition(String str, String str2) {
        try {
            this.mVold.forgetPartition(str, str2);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fstrim(int i, final IVoldTaskListener iVoldTaskListener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.fstrim(i, new IVoldTaskListener.Stub() {
                @Override
                public void onStatus(int i2, PersistableBundle persistableBundle) {
                    StorageManagerService.this.dispatchOnStatus(iVoldTaskListener, i2, persistableBundle);
                    if (i2 != 0) {
                        return;
                    }
                    String string = persistableBundle.getString("path");
                    long j = persistableBundle.getLong("bytes");
                    long j2 = persistableBundle.getLong("time");
                    ((DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class)).addText(StorageManagerService.TAG_STORAGE_TRIM, StorageManagerService.this.scrubPath(string) + " " + j + " " + j2);
                    synchronized (StorageManagerService.this.mLock) {
                        VolumeRecord volumeRecordFindRecordForPath = StorageManagerService.this.findRecordForPath(string);
                        if (volumeRecordFindRecordForPath != null) {
                            volumeRecordFindRecordForPath.lastTrimMillis = System.currentTimeMillis();
                            StorageManagerService.this.writeSettingsLocked();
                        }
                    }
                }

                @Override
                public void onFinished(int i2, PersistableBundle persistableBundle) {
                    StorageManagerService.this.dispatchOnFinished(iVoldTaskListener, i2, persistableBundle);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    void runIdleMaint(final Runnable runnable) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.runIdleMaint(new IVoldTaskListener.Stub() {
                @Override
                public void onStatus(int i, PersistableBundle persistableBundle) {
                }

                @Override
                public void onFinished(int i, PersistableBundle persistableBundle) {
                    if (runnable != null) {
                        BackgroundThread.getHandler().post(runnable);
                    }
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void runIdleMaintenance() {
        runIdleMaint(null);
    }

    void abortIdleMaint(final Runnable runnable) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.abortIdleMaint(new IVoldTaskListener.Stub() {
                @Override
                public void onStatus(int i, PersistableBundle persistableBundle) {
                }

                @Override
                public void onFinished(int i, PersistableBundle persistableBundle) {
                    if (runnable != null) {
                        BackgroundThread.getHandler().post(runnable);
                    }
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void abortIdleMaintenance() {
        abortIdleMaint(null);
    }

    private void remountUidExternalStorage(int i, int i2) {
        try {
            this.mVold.remountUid(i, i2);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void setDebugFlags(int i, int i2) {
        long jClearCallingIdentity;
        String str;
        String str2;
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        if ((i2 & 4) != 0) {
            if (StorageManager.isFileEncryptedNativeOnly()) {
                throw new IllegalStateException("Emulation not supported on device with native FBE");
            }
            if (this.mLockPatternUtils.isCredentialRequiredToDecrypt(false)) {
                throw new IllegalStateException("Emulation requires disabling 'Secure start-up' in Settings > Security");
            }
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.emulate_fbe", Boolean.toString((i & 4) != 0));
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(null);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } finally {
            }
        }
        if ((i2 & 3) != 0) {
            if ((i & 1) != 0) {
                str2 = "force_on";
            } else if ((i & 2) != 0) {
                str2 = "force_off";
            } else {
                str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.adoptable", str2);
                this.mHandler.obtainMessage(10).sendToTarget();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } finally {
            }
        }
        if ((i2 & 24) != 0) {
            if ((i & 8) != 0) {
                str = "force_on";
            } else if ((i & 16) != 0) {
                str = "force_off";
            } else {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.sdcardfs", str);
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
            }
        }
        if ((i2 & 32) != 0) {
            boolean z = (i & 32) != 0;
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.virtual_disk", Boolean.toString(z));
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
            }
        }
    }

    public String getPrimaryStorageUuid() {
        String str;
        synchronized (this.mLock) {
            str = this.mPrimaryStorageUuid;
        }
        return str;
    }

    public void setPrimaryStorageUuid(String str, IPackageMoveObserver iPackageMoveObserver) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            if (Objects.equals(this.mPrimaryStorageUuid, str)) {
                throw new IllegalArgumentException("Primary storage already at " + str);
            }
            if (this.mMoveCallback != null) {
                throw new IllegalStateException("Move already in progress");
            }
            this.mMoveCallback = iPackageMoveObserver;
            this.mMoveTargetUuid = str;
            for (UserInfo userInfo : ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers()) {
                if (StorageManager.isFileEncryptedNativeOrEmulated() && !isUserKeyUnlocked(userInfo.id)) {
                    Slog.w(TAG, "Failing move due to locked user " + userInfo.id);
                    onMoveStatusLocked(-10);
                    return;
                }
            }
            if (!Objects.equals("primary_physical", this.mPrimaryStorageUuid) && !Objects.equals("primary_physical", str)) {
                VolumeInfo volumeInfoFindStorageForUuid = findStorageForUuid(this.mPrimaryStorageUuid);
                VolumeInfo volumeInfoFindStorageForUuid2 = findStorageForUuid(str);
                if (volumeInfoFindStorageForUuid == null) {
                    Slog.w(TAG, "Failing move due to missing from volume " + this.mPrimaryStorageUuid);
                    onMoveStatusLocked(-6);
                    return;
                }
                if (volumeInfoFindStorageForUuid2 == null) {
                    Slog.w(TAG, "Failing move due to missing to volume " + str);
                    onMoveStatusLocked(-6);
                    return;
                }
                try {
                    this.mVold.moveStorage(volumeInfoFindStorageForUuid.id, volumeInfoFindStorageForUuid2.id, new IVoldTaskListener.Stub() {
                        @Override
                        public void onStatus(int i, PersistableBundle persistableBundle) {
                            synchronized (StorageManagerService.this.mLock) {
                                StorageManagerService.this.onMoveStatusLocked(i);
                            }
                        }

                        @Override
                        public void onFinished(int i, PersistableBundle persistableBundle) {
                        }
                    });
                    return;
                } catch (Exception e) {
                    Slog.wtf(TAG, e);
                    return;
                }
            }
            Slog.d(TAG, "Skipping move to/from primary physical");
            onMoveStatusLocked(82);
            onMoveStatusLocked(-100);
            this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    private void warnOnNotMounted() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i);
                if (volumeInfoValueAt.isPrimary() && volumeInfoValueAt.isMountedWritable()) {
                    return;
                }
            }
            Slog.w(TAG, "No primary storage mounted!");
        }
    }

    private boolean isUidOwnerOfPackageOrSystem(String str, int i) {
        if (i == 1000) {
            return true;
        }
        if (str != null && i == this.mPms.getPackageUid(str, 268435456, UserHandle.getUserId(i))) {
            return true;
        }
        return false;
    }

    public String getMountedObbPath(String str) {
        ObbState obbState;
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        warnOnNotMounted();
        synchronized (this.mObbMounts) {
            obbState = this.mObbPathToStateMap.get(str);
        }
        if (obbState == null) {
            Slog.w(TAG, "Failed to find OBB mounted at " + str);
            return null;
        }
        return findVolumeByIdOrThrow(obbState.volId).getPath().getAbsolutePath();
    }

    public boolean isObbMounted(String str) {
        boolean zContainsKey;
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            zContainsKey = this.mObbPathToStateMap.containsKey(str);
        }
        return zContainsKey;
    }

    public void mountObb(String str, String str2, String str3, IObbActionListener iObbActionListener, int i) {
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        Preconditions.checkNotNull(str2, "canonicalPath cannot be null");
        Preconditions.checkNotNull(iObbActionListener, "token cannot be null");
        int callingUid = Binder.getCallingUid();
        this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, new MountObbAction(new ObbState(str, str2, callingUid, iObbActionListener, i, null), str3, callingUid)));
    }

    public void unmountObb(String str, boolean z, IObbActionListener iObbActionListener, int i) {
        ObbState obbState;
        Preconditions.checkNotNull(str, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            obbState = this.mObbPathToStateMap.get(str);
        }
        if (obbState != null) {
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, new UnmountObbAction(new ObbState(str, obbState.canonicalPath, Binder.getCallingUid(), iObbActionListener, i, obbState.volId), z)));
        } else {
            Slog.w(TAG, "Unknown OBB mount at " + str);
        }
    }

    public int getEncryptionState() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.fdeComplete();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int decryptStorage(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeCheckPassword(str);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    StorageManagerService.lambda$decryptStorage$1(this.f$0);
                }
            }, 1000L);
            return 0;
        } catch (ServiceSpecificException e) {
            return e.errorCode;
        } catch (Exception e2) {
            Slog.wtf(TAG, e2);
            return -1;
        }
    }

    public static void lambda$decryptStorage$1(StorageManagerService storageManagerService) {
        try {
            storageManagerService.mVold.fdeRestart();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public int encryptStorage(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (i == 1) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeEnable(i, str, 0);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int changeEncryptionPassword(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (StorageManager.isFileEncryptedNativeOnly()) {
            return -1;
        }
        if (i == 1) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeChangePassword(i, str);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int verifyEncryptionPassword(String str) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to access the crypt keeper");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeVerifyPassword(str);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int getPasswordType() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.fdeGetPasswordType();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public void setField(String str, String str2) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (StorageManager.isFileEncryptedNativeOnly()) {
            return;
        }
        try {
            this.mVold.fdeSetField(str, str2);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public String getField(String str) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (StorageManager.isFileEncryptedNativeOnly()) {
            return null;
        }
        try {
            return this.mVold.fdeGetField(str);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return null;
        }
    }

    public boolean isConvertibleToFBE() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.isConvertibleToFbe();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return false;
        }
    }

    public String getPassword() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "only keyguard can retrieve password");
        try {
            return this.mVold.fdeGetPassword();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return null;
        }
    }

    public void clearPassword() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "only keyguard can clear password");
        try {
            this.mVold.fdeClearPassword();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void createUserKey(int i, int i2, boolean z) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.createUserKey(i, i2, z);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void destroyUserKey(int i) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.destroyUserKey(i);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    private String encodeBytes(byte[] bArr) {
        if (ArrayUtils.isEmpty(bArr)) {
            return "!";
        }
        return HexDump.toHexString(bArr);
    }

    public void addUserKeyAuth(int i, int i2, byte[] bArr, byte[] bArr2) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.addUserKeyAuth(i, i2, encodeBytes(bArr), encodeBytes(bArr2));
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fixateNewestUserKeyAuth(int i) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.fixateNewestUserKeyAuth(i);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void unlockUserKey(int i, int i2, byte[] bArr, byte[] bArr2) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (this.mLockPatternUtils.isSecure(i) && ArrayUtils.isEmpty(bArr2)) {
                throw new IllegalStateException("Secret required to unlock secure user " + i);
            }
            try {
                this.mVold.unlockUserKey(i, i2, encodeBytes(bArr), encodeBytes(bArr2));
            } catch (Exception e) {
                Slog.wtf(TAG, e);
                return;
            }
        }
        synchronized (this.mLock) {
            this.mLocalUnlockedUsers = ArrayUtils.appendInt(this.mLocalUnlockedUsers, i);
        }
    }

    public void lockUserKey(int i) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.lockUserKey(i);
            synchronized (this.mLock) {
                this.mLocalUnlockedUsers = ArrayUtils.removeInt(this.mLocalUnlockedUsers, i);
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public boolean isUserKeyUnlocked(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mLocalUnlockedUsers, i);
        }
        return zContains;
    }

    public void prepareUserStorage(String str, int i, int i2, int i3) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.prepareUserStorage(str, i, i2, i3);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void destroyUserStorage(String str, int i, int i2) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.destroyUserStorage(str, i, i2);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    class AppFuseMountScope extends AppFuseBridge.MountScope {
        boolean opened;

        public AppFuseMountScope(int i, int i2, int i3) {
            super(i, i2, i3);
            this.opened = false;
        }

        @Override
        public ParcelFileDescriptor open() throws NativeDaemonConnectorException {
            try {
                return new ParcelFileDescriptor(StorageManagerService.this.mVold.mountAppFuse(this.uid, Process.myPid(), this.mountId));
            } catch (Exception e) {
                throw new NativeDaemonConnectorException("Failed to mount", e);
            }
        }

        @Override
        public void close() throws Exception {
            if (this.opened) {
                StorageManagerService.this.mVold.unmountAppFuse(this.uid, Process.myPid(), this.mountId);
                this.opened = false;
            }
        }
    }

    public AppFuseMount mountProxyFileDescriptorBridge() {
        AppFuseMount appFuseMount;
        Slog.v(TAG, "mountProxyFileDescriptorBridge");
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        while (true) {
            synchronized (this.mAppFuseLock) {
                boolean z = false;
                if (this.mAppFuseBridge == null) {
                    this.mAppFuseBridge = new AppFuseBridge();
                    new Thread(this.mAppFuseBridge, AppFuseBridge.TAG).start();
                    z = true;
                }
                try {
                    int i = this.mNextAppFuseName;
                    this.mNextAppFuseName = i + 1;
                    try {
                        appFuseMount = new AppFuseMount(i, this.mAppFuseBridge.addBridge(new AppFuseMountScope(callingUid, callingPid, i)));
                    } catch (FuseUnavailableMountException e) {
                        if (z) {
                            Slog.e(TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                            return null;
                        }
                        this.mAppFuseBridge = null;
                    }
                } catch (NativeDaemonConnectorException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
            return appFuseMount;
        }
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int i, int i2, int i3) {
        Slog.v(TAG, "mountProxyFileDescriptor");
        int callingPid = Binder.getCallingPid();
        try {
            synchronized (this.mAppFuseLock) {
                if (this.mAppFuseBridge == null) {
                    Slog.e(TAG, "FuseBridge has not been created");
                    return null;
                }
                return this.mAppFuseBridge.openFile(callingPid, i, i2, i3);
            }
        } catch (FuseUnavailableMountException | InterruptedException e) {
            Slog.v(TAG, "The mount point has already been invalid", e);
            return null;
        }
    }

    public void mkdirs(String str, String str2) {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        Environment.UserEnvironment userEnvironment = new Environment.UserEnvironment(userId);
        String str3 = "sys.user." + userId + ".ce_available";
        if (!isUserKeyUnlocked(userId)) {
            throw new IllegalStateException("Failed to prepare " + str2);
        }
        if (userId == 0 && !SystemProperties.getBoolean(str3, false)) {
            throw new IllegalStateException("Failed to prepare " + str2);
        }
        ((AppOpsManager) this.mContext.getSystemService("appops")).checkPackage(Binder.getCallingUid(), str);
        try {
            File canonicalFile = new File(str2).getCanonicalFile();
            if (FileUtils.contains(userEnvironment.buildExternalStorageAppDataDirs(str), canonicalFile) || FileUtils.contains(userEnvironment.buildExternalStorageAppObbDirs(str), canonicalFile) || FileUtils.contains(userEnvironment.buildExternalStorageAppMediaDirs(str), canonicalFile)) {
                String absolutePath = canonicalFile.getAbsolutePath();
                if (!absolutePath.endsWith(SliceClientPermissions.SliceAuthority.DELIMITER)) {
                    absolutePath = absolutePath + SliceClientPermissions.SliceAuthority.DELIMITER;
                }
                try {
                    this.mVold.mkdirs(absolutePath);
                    return;
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to prepare " + absolutePath + ": " + e);
                }
            }
            throw new SecurityException("Invalid mkdirs path: " + canonicalFile);
        } catch (IOException e2) {
            throw new IllegalStateException("Failed to resolve " + str2 + ": " + e2);
        }
    }

    public StorageVolume[] getVolumeList(int i, String str, int i2) {
        boolean z;
        boolean zIsVisibleForWrite;
        int userId = UserHandle.getUserId(i);
        boolean z2 = (i2 & 256) != 0;
        boolean z3 = (i2 & 512) != 0;
        boolean z4 = (i2 & 1024) != 0;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            boolean zIsUserKeyUnlocked = isUserKeyUnlocked(userId);
            boolean zHasExternalStorage = this.mStorageManagerInternal.hasExternalStorage(i, str);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            ArrayList arrayList = new ArrayList();
            synchronized (this.mLock) {
                z = false;
                for (int i3 = 0; i3 < this.mVolumes.size(); i3++) {
                    VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i3);
                    int type = volumeInfoValueAt.getType();
                    if (type == 0 || type == 2) {
                        if (z2) {
                            zIsVisibleForWrite = volumeInfoValueAt.isVisibleForWrite(userId);
                        } else {
                            zIsVisibleForWrite = volumeInfoValueAt.isVisibleForRead(userId) || (z4 && volumeInfoValueAt.getPath() != null);
                        }
                        if (zIsVisibleForWrite) {
                            StorageVolume storageVolumeBuildStorageVolume = volumeInfoValueAt.buildStorageVolume(this.mContext, userId, (volumeInfoValueAt.getType() == 2 && !zIsUserKeyUnlocked) || !(zHasExternalStorage || z3));
                            if (volumeInfoValueAt.isPrimary()) {
                                arrayList.add(0, storageVolumeBuildStorageVolume);
                                z = true;
                            } else {
                                arrayList.add(storageVolumeBuildStorageVolume);
                            }
                        }
                    }
                }
            }
            if (!z) {
                Log.w(TAG, "No primary storage defined yet; hacking together a stub");
                boolean z5 = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                File legacyExternalStorageDirectory = Environment.getLegacyExternalStorageDirectory();
                arrayList.add(0, new StorageVolume("stub_primary", legacyExternalStorageDirectory, legacyExternalStorageDirectory, this.mContext.getString(R.string.unknownName), true, z5, !z5, false, 0L, new UserHandle(userId), null, "removed"));
            }
            return (StorageVolume[]) arrayList.toArray(new StorageVolume[arrayList.size()]);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public DiskInfo[] getDisks() {
        DiskInfo[] diskInfoArr;
        synchronized (this.mLock) {
            diskInfoArr = new DiskInfo[this.mDisks.size()];
            for (int i = 0; i < this.mDisks.size(); i++) {
                diskInfoArr[i] = this.mDisks.valueAt(i);
            }
        }
        return diskInfoArr;
    }

    public VolumeInfo[] getVolumes(int i) {
        VolumeInfo[] volumeInfoArr;
        synchronized (this.mLock) {
            volumeInfoArr = new VolumeInfo[this.mVolumes.size()];
            for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
                volumeInfoArr[i2] = this.mVolumes.valueAt(i2);
            }
        }
        return volumeInfoArr;
    }

    public VolumeRecord[] getVolumeRecords(int i) {
        VolumeRecord[] volumeRecordArr;
        synchronized (this.mLock) {
            volumeRecordArr = new VolumeRecord[this.mRecords.size()];
            for (int i2 = 0; i2 < this.mRecords.size(); i2++) {
                volumeRecordArr[i2] = this.mRecords.valueAt(i2);
            }
        }
        return volumeRecordArr;
    }

    public long getCacheQuotaBytes(String str, int i) {
        if (i != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return ((StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class)).getCacheQuotaBytes(str, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public long getCacheSizeBytes(String str, int i) {
        if (i != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                return ((StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class)).queryStatsForUid(str, i).getCacheBytes();
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int adjustAllocateFlags(int i, int i2, String str) {
        if ((i & 1) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ALLOCATE_AGGRESSIVE", TAG);
        }
        int i3 = i & (-3) & (-5);
        AppOpsManager appOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (appOpsManager.isOperationActive(26, i2, str)) {
                Slog.d(TAG, "UID " + i2 + " is actively using camera; letting them defy reserved cached data");
                i3 |= 4;
            }
            return i3;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public long getAllocatableBytes(String str, int i, String str2) {
        int iAdjustAllocateFlags = adjustAllocateFlags(i, Binder.getCallingUid(), str2);
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        StorageStatsManager storageStatsManager = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                File fileFindPathForUuid = storageManager.findPathForUuid(str);
                long usableSpace = fileFindPathForUuid.getUsableSpace();
                long storageLowBytes = storageManager.getStorageLowBytes(fileFindPathForUuid);
                long storageFullBytes = storageManager.getStorageFullBytes(fileFindPathForUuid);
                if (!storageStatsManager.isQuotaSupported(str)) {
                    return (iAdjustAllocateFlags & 1) != 0 ? Math.max(0L, usableSpace - storageFullBytes) : Math.max(0L, usableSpace - storageLowBytes);
                }
                long jMax = Math.max(0L, storageStatsManager.getCacheBytes(str) - storageManager.getStorageCacheBytes(fileFindPathForUuid, iAdjustAllocateFlags));
                return (iAdjustAllocateFlags & 1) != 0 ? Math.max(0L, (usableSpace + jMax) - storageFullBytes) : Math.max(0L, (usableSpace + jMax) - storageLowBytes);
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void allocateBytes(String str, long j, int i, String str2) throws ParcelableException {
        long storageLowBytes;
        int iAdjustAllocateFlags = adjustAllocateFlags(i, Binder.getCallingUid(), str2);
        long allocatableBytes = getAllocatableBytes(str, iAdjustAllocateFlags, str2);
        if (j > allocatableBytes) {
            throw new ParcelableException(new IOException("Failed to allocate " + j + " because only " + allocatableBytes + " allocatable"));
        }
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                File fileFindPathForUuid = storageManager.findPathForUuid(str);
                if ((iAdjustAllocateFlags & 1) != 0) {
                    storageLowBytes = j + storageManager.getStorageFullBytes(fileFindPathForUuid);
                } else {
                    storageLowBytes = j + storageManager.getStorageLowBytes(fileFindPathForUuid);
                }
                this.mPms.freeStorage(str, storageLowBytes, iAdjustAllocateFlags);
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void addObbStateLocked(ObbState obbState) throws RemoteException {
        IBinder binder = obbState.getBinder();
        List<ObbState> arrayList = this.mObbMounts.get(binder);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mObbMounts.put(binder, arrayList);
        } else {
            Iterator<ObbState> it = arrayList.iterator();
            while (it.hasNext()) {
                if (it.next().rawPath.equals(obbState.rawPath)) {
                    throw new IllegalStateException("Attempt to add ObbState twice. This indicates an error in the StorageManagerService logic.");
                }
            }
        }
        arrayList.add(obbState);
        try {
            obbState.link();
            this.mObbPathToStateMap.put(obbState.rawPath, obbState);
        } catch (RemoteException e) {
            arrayList.remove(obbState);
            if (arrayList.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
            throw e;
        }
    }

    private void removeObbStateLocked(ObbState obbState) {
        IBinder binder = obbState.getBinder();
        List<ObbState> list = this.mObbMounts.get(binder);
        if (list != null) {
            if (list.remove(obbState)) {
                obbState.unlink();
            }
            if (list.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
        }
        this.mObbPathToStateMap.remove(obbState.rawPath);
    }

    private class ObbActionHandler extends Handler {
        private final List<ObbAction> mActions;
        private boolean mBound;

        ObbActionHandler(Looper looper) {
            super(looper);
            this.mBound = false;
            this.mActions = new LinkedList();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    ObbAction obbAction = (ObbAction) message.obj;
                    if (!this.mBound && !connectToService()) {
                        obbAction.notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                        return;
                    } else {
                        this.mActions.add(obbAction);
                        return;
                    }
                case 2:
                    if (message.obj != null) {
                        StorageManagerService.this.mContainerService = (IMediaContainerService) message.obj;
                    }
                    if (StorageManagerService.this.mContainerService == null) {
                        Iterator<ObbAction> it = this.mActions.iterator();
                        while (it.hasNext()) {
                            it.next().notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                        }
                        this.mActions.clear();
                        return;
                    }
                    if (this.mActions.size() > 0) {
                        ObbAction obbAction2 = this.mActions.get(0);
                        if (obbAction2 != null) {
                            obbAction2.execute(this);
                            return;
                        }
                        return;
                    }
                    Slog.w(StorageManagerService.TAG, "Empty queue");
                    return;
                case 3:
                    if (this.mActions.size() > 0) {
                        this.mActions.remove(0);
                    }
                    if (this.mActions.size() != 0) {
                        StorageManagerService.this.mObbActionHandler.sendEmptyMessage(2);
                        return;
                    } else {
                        if (this.mBound) {
                            disconnectService();
                            return;
                        }
                        return;
                    }
                case 4:
                    if (this.mActions.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            Iterator<ObbAction> it2 = this.mActions.iterator();
                            while (it2.hasNext()) {
                                it2.next().notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                            }
                            this.mActions.clear();
                            return;
                        }
                        return;
                    }
                    return;
                case 5:
                    String str = (String) message.obj;
                    synchronized (StorageManagerService.this.mObbMounts) {
                        LinkedList<ObbState> linkedList = new LinkedList();
                        for (ObbState obbState : StorageManagerService.this.mObbPathToStateMap.values()) {
                            if (obbState.canonicalPath.startsWith(str)) {
                                linkedList.add(obbState);
                            }
                        }
                        for (ObbState obbState2 : linkedList) {
                            StorageManagerService.this.removeObbStateLocked(obbState2);
                            try {
                                obbState2.token.onObbResult(obbState2.rawPath, obbState2.nonce, 2);
                            } catch (RemoteException e) {
                                Slog.i(StorageManagerService.TAG, "Couldn't send unmount notification for  OBB: " + obbState2.rawPath);
                            }
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        private boolean connectToService() {
            if (StorageManagerService.this.mContext.bindServiceAsUser(new Intent().setComponent(StorageManagerService.DEFAULT_CONTAINER_COMPONENT), StorageManagerService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                this.mBound = true;
                return true;
            }
            return false;
        }

        private void disconnectService() {
            StorageManagerService.this.mContainerService = null;
            this.mBound = false;
            StorageManagerService.this.mContext.unbindService(StorageManagerService.this.mDefContainerConn);
        }
    }

    private static class ObbException extends Exception {
        public final int status;

        public ObbException(int i, String str) {
            super(str);
            this.status = i;
        }

        public ObbException(int i, Throwable th) {
            super(th.getMessage(), th);
            this.status = i;
        }
    }

    abstract class ObbAction {
        private static final int MAX_RETRIES = 3;
        ObbState mObbState;
        private int mRetries;

        abstract void handleExecute() throws ObbException;

        ObbAction(ObbState obbState) {
            this.mObbState = obbState;
        }

        public void execute(ObbActionHandler obbActionHandler) {
            try {
                this.mRetries++;
                if (this.mRetries > 3) {
                    StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
                    notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                } else {
                    handleExecute();
                    StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
                }
            } catch (ObbException e) {
                notifyObbStateChange(e);
                StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
            }
        }

        protected ObbInfo getObbInfo() throws ObbException {
            try {
                ObbInfo obbInfo = StorageManagerService.this.mContainerService.getObbInfo(this.mObbState.canonicalPath);
                if (obbInfo != null) {
                    return obbInfo;
                }
                throw new ObbException(20, "Missing OBB info for: " + this.mObbState.canonicalPath);
            } catch (Exception e) {
                throw new ObbException(25, e);
            }
        }

        protected void notifyObbStateChange(ObbException obbException) {
            Slog.w(StorageManagerService.TAG, obbException);
            notifyObbStateChange(obbException.status);
        }

        protected void notifyObbStateChange(int i) {
            if (this.mObbState == null || this.mObbState.token == null) {
                return;
            }
            try {
                this.mObbState.token.onObbResult(this.mObbState.rawPath, this.mObbState.nonce, i);
            } catch (RemoteException e) {
                Slog.w(StorageManagerService.TAG, "StorageEventListener went away while calling onObbStateChanged");
            }
        }
    }

    class MountObbAction extends ObbAction {
        private final int mCallingUid;
        private final String mKey;

        MountObbAction(ObbState obbState, String str, int i) {
            super(obbState);
            this.mKey = str;
            this.mCallingUid = i;
        }

        @Override
        public void handleExecute() throws ObbException {
            boolean zContainsKey;
            String string;
            StorageManagerService.this.warnOnNotMounted();
            ObbInfo obbInfo = getObbInfo();
            if (StorageManagerService.this.isUidOwnerOfPackageOrSystem(obbInfo.packageName, this.mCallingUid)) {
                synchronized (StorageManagerService.this.mObbMounts) {
                    zContainsKey = StorageManagerService.this.mObbPathToStateMap.containsKey(this.mObbState.rawPath);
                }
                if (zContainsKey) {
                    throw new ObbException(24, "Attempt to mount OBB which is already mounted: " + obbInfo.filename);
                }
                if (this.mKey == null) {
                    string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                } else {
                    try {
                        string = new BigInteger(SecretKeyFactory.getInstance(BackupPasswordManager.PBKDF_CURRENT).generateSecret(new PBEKeySpec(this.mKey.toCharArray(), obbInfo.salt, 1024, 128)).getEncoded()).toString(16);
                    } catch (GeneralSecurityException e) {
                        throw new ObbException(20, e);
                    }
                }
                try {
                    this.mObbState.volId = StorageManagerService.this.mVold.createObb(this.mObbState.canonicalPath, string, this.mObbState.ownerGid);
                    StorageManagerService.this.mVold.mount(this.mObbState.volId, 0, -1);
                    synchronized (StorageManagerService.this.mObbMounts) {
                        StorageManagerService.this.addObbStateLocked(this.mObbState);
                    }
                    notifyObbStateChange(1);
                    return;
                } catch (Exception e2) {
                    throw new ObbException(21, e2);
                }
            }
            throw new ObbException(25, "Denied attempt to mount OBB " + obbInfo.filename + " which is owned by " + obbInfo.packageName);
        }

        public String toString() {
            return "MountObbAction{" + this.mObbState + '}';
        }
    }

    class UnmountObbAction extends ObbAction {
        private final boolean mForceUnmount;

        UnmountObbAction(ObbState obbState, boolean z) {
            super(obbState);
            this.mForceUnmount = z;
        }

        @Override
        public void handleExecute() throws ObbException {
            ObbState obbState;
            StorageManagerService.this.warnOnNotMounted();
            synchronized (StorageManagerService.this.mObbMounts) {
                obbState = (ObbState) StorageManagerService.this.mObbPathToStateMap.get(this.mObbState.rawPath);
            }
            if (obbState == null) {
                throw new ObbException(23, "Missing existingState");
            }
            if (obbState.ownerGid == this.mObbState.ownerGid) {
                try {
                    StorageManagerService.this.mVold.unmount(this.mObbState.volId);
                    StorageManagerService.this.mVold.destroyObb(this.mObbState.volId);
                    this.mObbState.volId = null;
                    synchronized (StorageManagerService.this.mObbMounts) {
                        StorageManagerService.this.removeObbStateLocked(obbState);
                    }
                    notifyObbStateChange(2);
                    return;
                } catch (Exception e) {
                    throw new ObbException(22, e);
                }
            }
            notifyObbStateChange(new ObbException(25, "Permission denied to unmount OBB " + obbState.rawPath + " (owned by GID " + obbState.ownerGid + ")"));
        }

        public String toString() {
            return "UnmountObbAction{" + this.mObbState + ",force=" + this.mForceUnmount + '}';
        }
    }

    private void dispatchOnStatus(IVoldTaskListener iVoldTaskListener, int i, PersistableBundle persistableBundle) {
        if (iVoldTaskListener != null) {
            try {
                iVoldTaskListener.onStatus(i, persistableBundle);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchOnFinished(IVoldTaskListener iVoldTaskListener, int i, PersistableBundle persistableBundle) {
        if (iVoldTaskListener != null) {
            try {
                iVoldTaskListener.onFinished(i, persistableBundle);
            } catch (RemoteException e) {
            }
        }
    }

    private static class Callbacks extends Handler {
        private static final int MSG_DISK_DESTROYED = 6;
        private static final int MSG_DISK_SCANNED = 5;
        private static final int MSG_STORAGE_STATE_CHANGED = 1;
        private static final int MSG_VOLUME_FORGOTTEN = 4;
        private static final int MSG_VOLUME_RECORD_CHANGED = 3;
        private static final int MSG_VOLUME_STATE_CHANGED = 2;
        private final RemoteCallbackList<IStorageEventListener> mCallbacks;

        public Callbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
        }

        public void register(IStorageEventListener iStorageEventListener) {
            this.mCallbacks.register(iStorageEventListener);
        }

        public void unregister(IStorageEventListener iStorageEventListener) {
            this.mCallbacks.unregister(iStorageEventListener);
        }

        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                try {
                    invokeCallback((IStorageEventListener) this.mCallbacks.getBroadcastItem(i), message.what, someArgs);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            someArgs.recycle();
        }

        private void invokeCallback(IStorageEventListener iStorageEventListener, int i, SomeArgs someArgs) throws RemoteException {
            switch (i) {
                case 1:
                    iStorageEventListener.onStorageStateChanged((String) someArgs.arg1, (String) someArgs.arg2, (String) someArgs.arg3);
                    break;
                case 2:
                    iStorageEventListener.onVolumeStateChanged((VolumeInfo) someArgs.arg1, someArgs.argi2, someArgs.argi3);
                    break;
                case 3:
                    iStorageEventListener.onVolumeRecordChanged((VolumeRecord) someArgs.arg1);
                    break;
                case 4:
                    iStorageEventListener.onVolumeForgotten((String) someArgs.arg1);
                    break;
                case 5:
                    iStorageEventListener.onDiskScanned((DiskInfo) someArgs.arg1, someArgs.argi2);
                    break;
                case 6:
                    iStorageEventListener.onDiskDestroyed((DiskInfo) someArgs.arg1);
                    break;
            }
        }

        private void notifyStorageStateChanged(String str, String str2, String str3) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            someArgsObtain.arg3 = str3;
            obtainMessage(1, someArgsObtain).sendToTarget();
        }

        private void notifyVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = volumeInfo.clone();
            someArgsObtain.argi2 = i;
            someArgsObtain.argi3 = i2;
            obtainMessage(2, someArgsObtain).sendToTarget();
        }

        private void notifyVolumeRecordChanged(VolumeRecord volumeRecord) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = volumeRecord.clone();
            obtainMessage(3, someArgsObtain).sendToTarget();
        }

        private void notifyVolumeForgotten(String str) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            obtainMessage(4, someArgsObtain).sendToTarget();
        }

        private void notifyDiskScanned(DiskInfo diskInfo, int i) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = diskInfo.clone();
            someArgsObtain.argi2 = i;
            obtainMessage(5, someArgsObtain).sendToTarget();
        }

        private void notifyDiskDestroyed(DiskInfo diskInfo) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = diskInfo.clone();
            obtainMessage(6, someArgsObtain).sendToTarget();
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ", 160);
            synchronized (this.mLock) {
                indentingPrintWriter.println("Disks:");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < this.mDisks.size(); i++) {
                    this.mDisks.valueAt(i).dump(indentingPrintWriter);
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("Volumes:");
                indentingPrintWriter.increaseIndent();
                for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
                    VolumeInfo volumeInfoValueAt = this.mVolumes.valueAt(i2);
                    if (!"private".equals(volumeInfoValueAt.id)) {
                        volumeInfoValueAt.dump(indentingPrintWriter);
                    }
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("Records:");
                indentingPrintWriter.increaseIndent();
                for (int i3 = 0; i3 < this.mRecords.size(); i3++) {
                    this.mRecords.valueAt(i3).dump(indentingPrintWriter);
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("Primary storage UUID: " + this.mPrimaryStorageUuid);
                Pair primaryStoragePathAndSize = StorageManager.getPrimaryStoragePathAndSize();
                if (primaryStoragePathAndSize == null) {
                    indentingPrintWriter.println("Internal storage total size: N/A");
                } else {
                    indentingPrintWriter.print("Internal storage (");
                    indentingPrintWriter.print((String) primaryStoragePathAndSize.first);
                    indentingPrintWriter.print(") total size: ");
                    indentingPrintWriter.print(primaryStoragePathAndSize.second);
                    indentingPrintWriter.print(" (");
                    indentingPrintWriter.print(DataUnit.MEBIBYTES.toBytes(((Long) primaryStoragePathAndSize.second).longValue()));
                    indentingPrintWriter.println(" MiB)");
                }
                indentingPrintWriter.println("Local unlocked users: " + Arrays.toString(this.mLocalUnlockedUsers));
                indentingPrintWriter.println("System unlocked users: " + Arrays.toString(this.mSystemUnlockedUsers));
            }
            synchronized (this.mObbMounts) {
                indentingPrintWriter.println();
                indentingPrintWriter.println("mObbMounts:");
                indentingPrintWriter.increaseIndent();
                for (Map.Entry<IBinder, List<ObbState>> entry : this.mObbMounts.entrySet()) {
                    indentingPrintWriter.println(entry.getKey() + ":");
                    indentingPrintWriter.increaseIndent();
                    Iterator<ObbState> it = entry.getValue().iterator();
                    while (it.hasNext()) {
                        indentingPrintWriter.println(it.next());
                    }
                    indentingPrintWriter.decreaseIndent();
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("mObbPathToStateMap:");
                indentingPrintWriter.increaseIndent();
                for (Map.Entry<String, ObbState> entry2 : this.mObbPathToStateMap.entrySet()) {
                    indentingPrintWriter.print(entry2.getKey());
                    indentingPrintWriter.print(" -> ");
                    indentingPrintWriter.println(entry2.getValue());
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.print("Last maintenance: ");
            indentingPrintWriter.println(TimeUtils.formatForLogging(this.mLastMaintenance));
        }
    }

    @Override
    public void monitor() {
        try {
            this.mVold.monitor();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    private final class StorageManagerInternalImpl extends StorageManagerInternal {
        private final CopyOnWriteArrayList<StorageManagerInternal.ExternalStorageMountPolicy> mPolicies;

        private StorageManagerInternalImpl() {
            this.mPolicies = new CopyOnWriteArrayList<>();
        }

        public void addExternalStoragePolicy(StorageManagerInternal.ExternalStorageMountPolicy externalStorageMountPolicy) {
            this.mPolicies.add(externalStorageMountPolicy);
        }

        public void onExternalStoragePolicyChanged(int i, String str) {
            StorageManagerService.this.remountUidExternalStorage(i, getExternalStorageMountMode(i, str));
        }

        public int getExternalStorageMountMode(int i, String str) {
            Iterator<StorageManagerInternal.ExternalStorageMountPolicy> it = this.mPolicies.iterator();
            int iMin = Integer.MAX_VALUE;
            while (it.hasNext()) {
                int mountMode = it.next().getMountMode(i, str);
                if (mountMode == 0) {
                    return 0;
                }
                iMin = Math.min(iMin, mountMode);
            }
            if (iMin == Integer.MAX_VALUE) {
                return 0;
            }
            return iMin;
        }

        public boolean hasExternalStorage(int i, String str) {
            if (i == 1000) {
                return true;
            }
            Iterator<StorageManagerInternal.ExternalStorageMountPolicy> it = this.mPolicies.iterator();
            while (it.hasNext()) {
                if (!it.next().hasExternalStorage(i, str)) {
                    return false;
                }
            }
            return true;
        }
    }
}
