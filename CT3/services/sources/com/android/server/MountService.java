package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IActivityManager;
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
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.IMountService;
import android.os.storage.IMountServiceListener;
import android.os.storage.IMountShutdownObserver;
import android.os.storage.IObbActionListener;
import android.os.storage.MountServiceInternal;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.Xml;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.NativeDaemonConnector;
import com.android.server.Watchdog;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.usage.UnixCalendar;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Lists;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class MountService extends IMountService.Stub implements INativeDaemonConnectorCallbacks, Watchdog.Monitor {
    public static final String ACTION_ENCRYPTION_TYPE_CHANGED = "com.mediatek.intent.extra.ACTION_ENCRYPTION_TYPE_CHANGED";
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_FORCE_ADOPTABLE = "forceAdoptable";
    private static final String ATTR_FS_UUID = "fsUuid";
    private static final String ATTR_LAST_BENCH_MILLIS = "lastBenchMillis";
    private static final String ATTR_LAST_TRIM_MILLIS = "lastTrimMillis";
    private static final String ATTR_NICKNAME = "nickname";
    private static final String ATTR_PART_GUID = "partGuid";
    private static final String ATTR_PRIMARY_STORAGE_UUID = "primaryStorageUuid";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USER_FLAGS = "userFlags";
    private static final String ATTR_VERSION = "version";
    private static final String BOOT_IPO = "android.intent.action.ACTION_BOOT_IPO";
    private static final String CRYPTD_TAG = "CryptdConnector";
    private static final int CRYPTO_ALGORITHM_KEY_SIZE = 128;
    private static final boolean DEBUG_EVENTS = true;
    private static final boolean DEBUG_OBB = true;
    private static final int H_DAEMON_CONNECTED = 2;
    private static final int H_FSTRIM = 4;
    private static final int H_INTERNAL_BROADCAST = 7;
    private static final int H_PARTITION_FORGET = 9;
    private static final int H_RESET = 10;
    private static final int H_SHUTDOWN = 3;
    private static final int H_SYSTEM_READY = 1;
    private static final int H_VOLUME_BROADCAST = 6;
    private static final int H_VOLUME_MOUNT = 5;
    private static final int H_VOLUME_UNMOUNT = 8;
    private static final String INSERT_OTG = "insert_otg";
    private static final String LAST_FSTRIM_FILE = "last-fstrim";
    private static final int MAX_CONTAINERS = 250;
    private static final int MOVE_STATUS_COPY_FINISHED = 82;
    private static final int OBB_FLUSH_MOUNT_STATE = 5;
    private static final int OBB_MCS_BOUND = 2;
    private static final int OBB_MCS_RECONNECT = 4;
    private static final int OBB_MCS_UNBIND = 3;
    private static final int OBB_RUN_ACTION = 1;
    private static final String OMADM_SD_FORMAT = "com.mediatek.dm.LAWMO_WIPE";
    private static final String OMADM_USB_DISABLE = "com.mediatek.dm.LAWMO_LOCK";
    private static final String OMADM_USB_ENABLE = "com.mediatek.dm.LAWMO_UNLOCK";
    private static final int PBKDF2_HASH_ROUNDS = 1024;
    private static final String PRIVACY_PROTECTION_LOCK = "com.mediatek.ppl.NOTIFY_LOCK";
    private static final String PRIVACY_PROTECTION_UNLOCK = "com.mediatek.ppl.NOTIFY_UNLOCK";
    private static final String PRIVACY_PROTECTION_WIPE = "com.mediatek.ppl.NOTIFY_MOUNT_SERVICE_WIPE";
    private static final String PRIVACY_PROTECTION_WIPE_DONE = "com.mediatek.ppl.MOUNT_SERVICE_WIPE_RESPONSE";
    private static final String PROP_DM_APP = "ro.mtk_dm_app";
    private static final String PROP_VOLD_DECRYPT = "vold.decrypt";
    private static final String TAG = "MountService";
    private static final String TAG_STORAGE_BENCHMARK = "storage_benchmark";
    private static final String TAG_STORAGE_TRIM = "storage_trim";
    private static final String TAG_VOLUME = "volume";
    private static final String TAG_VOLUMES = "volumes";
    private static final int VERSION_ADD_PRIMARY = 2;
    private static final int VERSION_FIX_PRIMARY = 3;
    private static final int VERSION_INIT = 1;
    private static final String VOLD_TAG = "VoldConnector";
    private static final boolean WATCHDOG_ENABLE = false;
    private final Callbacks mCallbacks;
    private final NativeDaemonConnector mConnector;
    private final Thread mConnectorThread;
    private final Context mContext;
    private final NativeDaemonConnector mCryptConnector;
    private final Thread mCryptConnectorThread;

    @GuardedBy("mLock")
    private boolean mForceAdoptable;
    private final Handler mHandler;
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
    private boolean mUmsEnabling;

    @GuardedBy("mUnmountLock")
    private CountDownLatch mUnmountSignal;
    static MountService sSelf = null;
    private static final boolean LOG_ENABLE = SystemProperties.get("ro.build.type").equals("eng");
    public static final String[] CRYPTO_TYPES = {"password", "default", "pattern", "pin"};
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");
    private static boolean isBootingPhase = false;
    private static boolean isShuttingDown = false;
    private static final Object OMADM_SYNC_LOCK = new Object();
    private static final Object FORMAT_LOCK = new Object();
    private static final Object TURNONUSB_SYNC_LOCK = new Object();
    private static boolean isIPOBooting = false;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int[] mLocalUnlockedUsers = EmptyArray.INT;

    @GuardedBy("mLock")
    private int[] mSystemUnlockedUsers = EmptyArray.INT;

    @GuardedBy("mLock")
    private ArrayMap<String, DiskInfo> mDisks = new ArrayMap<>();

    @GuardedBy("mLock")
    private final ArrayMap<String, VolumeInfo> mVolumes = new ArrayMap<>();

    @GuardedBy("mLock")
    private ArrayMap<String, VolumeRecord> mRecords = new ArrayMap<>();

    @GuardedBy("mLock")
    private ArrayMap<String, CountDownLatch> mDiskScanLatches = new ArrayMap<>();
    private volatile int mCurrentUserId = 0;
    private volatile boolean mSystemReady = false;
    private volatile boolean mBootCompleted = false;
    private volatile boolean mDaemonConnected = false;
    private final CountDownLatch mConnectedSignal = new CountDownLatch(2);
    private final CountDownLatch mAsecsScanned = new CountDownLatch(1);
    private final Object mUnmountLock = new Object();
    private final HashSet<String> mAsecMountSet = new HashSet<>();
    private final Map<IBinder, List<ObbState>> mObbMounts = new HashMap();
    private final Map<String, ObbState> mObbPathToStateMap = new HashMap();
    private final MountServiceInternalImpl mMountServiceInternal = new MountServiceInternalImpl(this, null);
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    private IMediaContainerService mContainerService = null;
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            Preconditions.checkArgument(userId >= 0);
            try {
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    Slog.i(MountService.TAG, "onReceive:ACTION_USER_ADDED");
                    UserManager um = (UserManager) MountService.this.mContext.getSystemService(UserManager.class);
                    int userSerialNumber = um.getUserSerialNumber(userId);
                    MountService.this.mConnector.execute(MountService.TAG_VOLUME, "user_added", Integer.valueOf(userId), Integer.valueOf(userSerialNumber));
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    synchronized (MountService.this.mVolumes) {
                        int size = MountService.this.mVolumes.size();
                        for (int i = 0; i < size; i++) {
                            VolumeInfo vol = (VolumeInfo) MountService.this.mVolumes.valueAt(i);
                            if (vol.mountUserId == userId) {
                                vol.mountUserId = -10000;
                                MountService.this.mHandler.obtainMessage(8, vol).sendToTarget();
                            }
                        }
                    }
                    Slog.i(MountService.TAG, "onReceive:ACTION_USER_REMOVED");
                    MountService.this.mConnector.execute(MountService.TAG_VOLUME, "user_removed", Integer.valueOf(userId));
                    return;
                }
                if (!"android.intent.action.USER_SWITCHED".equals(action)) {
                    return;
                }
                Slog.i(MountService.TAG, "ACTION_USER_SWITCHED");
                MountService.this.mCurrentUserId = userId;
                MountService.this.updateDefaultPathForUserSwitch();
            } catch (NativeDaemonConnectorException e) {
                Slog.w(MountService.TAG, "Failed to send user details to vold", e);
            }
        }
    };
    private boolean isDiskInsert = false;
    private final BroadcastReceiver mDMReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.dm.LAWMO_UNLOCK")) {
                Slog.d(MountService.TAG, "mDMReceiver USB enable");
                new Thread() {
                    @Override
                    public void run() {
                        synchronized (MountService.OMADM_SYNC_LOCK) {
                            MountService.this.enableUSBFuction(true);
                        }
                    }
                }.start();
            } else if (action.equals("com.mediatek.dm.LAWMO_LOCK")) {
                Slog.d(MountService.TAG, "mDMReceiver USB disable");
                new Thread() {
                    @Override
                    public void run() {
                        synchronized (MountService.OMADM_SYNC_LOCK) {
                            MountService.this.enableUSBFuction(false);
                        }
                    }
                }.start();
            } else {
                if (!action.equals(MountService.OMADM_SD_FORMAT)) {
                    return;
                }
                Slog.d(MountService.TAG, "mDMReceiver format SD");
                new Thread() {
                    @Override
                    public void run() {
                        VolumeInfo[] volumes = MountService.this.getVolumes(0);
                        synchronized (MountService.OMADM_SYNC_LOCK) {
                            int unused = MountService.this.mCurrentUserId;
                            for (VolumeInfo vol : volumes) {
                                if (!vol.isVisible() || vol.getType() != 0) {
                                    Slog.d(MountService.TAG, "no need format, skip volume=" + vol);
                                } else {
                                    try {
                                        try {
                                            try {
                                                try {
                                                    if (vol.getState() == 2) {
                                                        MountService.this.unmount(vol.getId());
                                                        int j = 0;
                                                        while (true) {
                                                            if (j >= 20) {
                                                                break;
                                                            }
                                                            sleep(1000L);
                                                            if (vol.getState() != 0) {
                                                                j++;
                                                            } else {
                                                                Slog.d(MountService.TAG, "Unmount Succeeded, volume=" + vol);
                                                                break;
                                                            }
                                                        }
                                                    } else if (vol.getState() == 9) {
                                                        Slog.d(MountService.TAG, "volume is shared, unshared firstly, volume=" + vol);
                                                        MountService.this.doShareUnshareVolume(vol.getId(), false);
                                                    }
                                                    MountService.this.format(vol.getId());
                                                    Slog.d(MountService.TAG, "format Succeed! volume=" + vol);
                                                } catch (NullPointerException e) {
                                                    Slog.e(MountService.TAG, "SD format exception", e);
                                                }
                                            } catch (IllegalArgumentException e2) {
                                                Slog.e(MountService.TAG, "SD format exception", e2);
                                            }
                                        } catch (SecurityException e3) {
                                            Slog.e(MountService.TAG, "SD format exception", e3);
                                        }
                                    } catch (InterruptedException e4) {
                                        Slog.e(MountService.TAG, "SD format exception", e4);
                                    }
                                }
                            }
                        }
                    }
                }.start();
            }
        }
    };
    private final BroadcastReceiver mPrivacyProtectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.ppl.NOTIFY_UNLOCK")) {
                Slog.i(MountService.TAG, "Privacy Protection unlock!");
                new Thread() {
                    @Override
                    public void run() {
                        MountService.this.enableUSBFuction(true);
                    }
                }.start();
            } else if (action.equals("com.mediatek.ppl.NOTIFY_LOCK")) {
                Slog.i(MountService.TAG, "Privacy Protection lock!");
                new Thread() {
                    @Override
                    public void run() {
                        MountService.this.enableUSBFuction(false);
                    }
                }.start();
            } else {
                if (!action.equals(MountService.PRIVACY_PROTECTION_WIPE)) {
                    return;
                }
                Slog.i(MountService.TAG, "Privacy Protection wipe!");
                MountService.this.formatPhoneStorageAndExternalSDCard();
            }
        }
    };
    private boolean mIsTurnOnOffUsb = false;
    private boolean mIsUsbConnected = false;
    private boolean mSendUmsConnectedOnBoot = false;
    private int mUMSCount = 0;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.i(MountService.TAG, "mUsbReceiver onReceive, intent=" + intent);
            boolean isConnected = intent.getBooleanExtra("connected", false) && intent.getBooleanExtra("mass_storage", false) && !intent.getBooleanExtra("SettingUsbCharging", false);
            MountService.this.mIsUsbConnected = isConnected;
            MountService.this.notifyShareAvailabilityChange(isConnected);
        }
    };
    private final BroadcastReceiver mBootIPOReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread() {
                @Override
                public void run() {
                    Slog.d(MountService.TAG, "MountService BOOT_IPO");
                    boolean unused = MountService.isIPOBooting = true;
                    try {
                        Slog.d(MountService.TAG, "Notify VOLD IPO startup");
                        MountService.this.mConnector.execute(MountService.TAG_VOLUME, "ipo", "startup");
                    } catch (NativeDaemonConnectorException e) {
                        Slog.e(MountService.TAG, "Error reinit SD card while IPO", e);
                    }
                    MountService.this.waitAllVolumeMounted();
                    Slog.d(MountService.TAG, "MountService BOOT_IPO finish");
                    boolean unused2 = MountService.isIPOBooting = false;
                    MountService.this.updateDefaultPathIfNeed();
                }
            }.start();
        }
    };
    private int mOldEncryptionType = -1;

    public static class Lifecycle extends SystemService {
        private MountService mMountService;
        private String oldDefaultPath;

        public Lifecycle(Context context) {
            super(context);
            this.oldDefaultPath = "";
        }

        @Override
        public void onStart() {
            Slog.d(MountService.TAG, "MountService onStart");
            boolean unused = MountService.isBootingPhase = true;
            this.mMountService = new MountService(getContext());
            publishBinderService("mount", this.mMountService);
            this.mMountService.start();
            this.oldDefaultPath = MountService.sSelf.getDefaultPath();
            Slog.d(MountService.TAG, "get Default path onStart default path=" + this.oldDefaultPath);
        }

        @Override
        public void onBootPhase(int phase) {
            Slog.d(MountService.TAG, "MountService onBootPhase");
            if (phase == 550) {
                this.mMountService.systemReady();
            } else if (phase == 1000) {
                this.mMountService.bootCompleted();
            }
            if (phase != 1000) {
                return;
            }
            Slog.d(MountService.TAG, "MountService onBootPhase: PHASE_BOOT_COMPLETED");
            boolean unused = MountService.isBootingPhase = false;
            if (this.oldDefaultPath.contains("emulated") || "".equals(this.oldDefaultPath)) {
                return;
            }
            Slog.d(MountService.TAG, "set defaut path to " + this.oldDefaultPath);
            MountService.sSelf.setDefaultPath(this.oldDefaultPath);
            MountService.sSelf.updateDefaultPathIfNeed();
        }

        @Override
        public void onSwitchUser(int userHandle) {
            this.mMountService.mCurrentUserId = userHandle;
        }

        @Override
        public void onUnlockUser(int userHandle) {
            Slog.d(MountService.TAG, "MountService onUnlockUser, userHandle=" + userHandle);
            this.mMountService.onUnlockUser(userHandle);
        }

        @Override
        public void onCleanupUser(int userHandle) {
            Slog.d(MountService.TAG, "MountService onCleanupUser, userHandle=" + userHandle);
            this.mMountService.onCleanupUser(userHandle);
        }
    }

    class VoldResponseCode {
        public static final int AsecListResult = 111;
        public static final int AsecPathResult = 211;
        public static final int BENCHMARK_RESULT = 661;
        public static final int CryptfsGetfieldResult = 113;
        public static final int DISK_CREATED = 640;
        public static final int DISK_DESTROYED = 649;
        public static final int DISK_LABEL_CHANGED = 642;
        public static final int DISK_SCANNED = 643;
        public static final int DISK_SIZE_CHANGED = 641;
        public static final int DISK_SYS_PATH_CHANGED = 644;
        public static final int MOVE_STATUS = 660;
        public static final int OpFailedMediaBlank = 402;
        public static final int OpFailedMediaCorrupt = 403;
        public static final int OpFailedNoMedia = 401;
        public static final int OpFailedStorageBusy = 405;
        public static final int OpFailedStorageNotFound = 406;
        public static final int OpFailedVolNotMounted = 404;
        public static final int ShareEnabledResult = 212;
        public static final int ShareStatusResult = 210;
        public static final int StorageUsersListResult = 112;
        public static final int TRIM_RESULT = 662;
        public static final int VOLUME_CREATED = 650;
        public static final int VOLUME_DESTROYED = 659;
        public static final int VOLUME_FS_LABEL_CHANGED = 654;
        public static final int VOLUME_FS_TYPE_CHANGED = 652;
        public static final int VOLUME_FS_UUID_CHANGED = 653;
        public static final int VOLUME_INTERNAL_PATH_CHANGED = 656;
        public static final int VOLUME_PATH_CHANGED = 655;
        public static final int VOLUME_STATE_CHANGED = 651;
        public static final int VolumeListResult = 110;

        VoldResponseCode() {
        }
    }

    private VolumeInfo findVolumeByIdOrThrow(String id) {
        synchronized (this.mLock) {
            VolumeInfo vol = this.mVolumes.get(id);
            if (vol != null) {
                return vol;
            }
            throw new IllegalArgumentException("No volume found for ID " + id);
        }
    }

    private String findVolumeIdForPathOrThrow(String path) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.path != null && path.startsWith(vol.path)) {
                    return vol.id;
                }
            }
            throw new IllegalArgumentException("No volume found for path " + path);
        }
    }

    private VolumeRecord findRecordForPath(String path) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.path != null && path.startsWith(vol.path)) {
                    return this.mRecords.get(vol.fsUuid);
                }
            }
            return null;
        }
    }

    private String scrubPath(String path) {
        if (path.startsWith(Environment.getDataDirectory().getAbsolutePath())) {
            return "internal";
        }
        VolumeRecord rec = findRecordForPath(path);
        if (rec == null || rec.createdMillis == 0) {
            return "unknown";
        }
        return "ext:" + ((int) ((System.currentTimeMillis() - rec.createdMillis) / UnixCalendar.WEEK_IN_MILLIS)) + "w";
    }

    private VolumeInfo findStorageForUuid(String volumeUuid) {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            return storage.findVolumeById("emulated");
        }
        if (Objects.equals("primary_physical", volumeUuid)) {
            return storage.getPrimaryPhysicalVolume();
        }
        return storage.findEmulatedForPrivate(storage.findVolumeByUuid(volumeUuid));
    }

    private boolean shouldBenchmark() {
        long benchInterval = Settings.Global.getLong(this.mContext.getContentResolver(), "storage_benchmark_interval", UnixCalendar.WEEK_IN_MILLIS);
        if (benchInterval == -1) {
            return false;
        }
        if (benchInterval == 0) {
            return true;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                VolumeRecord rec = this.mRecords.get(vol.fsUuid);
                if (vol.isMountedWritable() && rec != null) {
                    long benchAge = System.currentTimeMillis() - rec.lastBenchMillis;
                    if (benchAge >= benchInterval) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private CountDownLatch findOrCreateDiskScanLatch(String diskId) {
        CountDownLatch latch;
        synchronized (this.mLock) {
            latch = this.mDiskScanLatches.get(diskId);
            if (latch == null) {
                latch = new CountDownLatch(1);
                this.mDiskScanLatches.put(diskId, latch);
            }
        }
        return latch;
    }

    private static String escapeNull(String arg) {
        if (TextUtils.isEmpty(arg)) {
            return "!";
        }
        if (arg.indexOf(0) != -1 || arg.indexOf(32) != -1) {
            throw new IllegalArgumentException(arg);
        }
        return arg;
    }

    class ObbState implements IBinder.DeathRecipient {
        final String canonicalPath;
        final int nonce;
        final int ownerGid;
        final String rawPath;
        final IObbActionListener token;

        public ObbState(String rawPath, String canonicalPath, int callingUid, IObbActionListener token, int nonce) {
            this.rawPath = rawPath;
            this.canonicalPath = canonicalPath;
            this.ownerGid = UserHandle.getSharedAppGid(callingUid);
            this.token = token;
            this.nonce = nonce;
        }

        public IBinder getBinder() {
            return this.token.asBinder();
        }

        @Override
        public void binderDied() {
            ObbAction action = MountService.this.new UnmountObbAction(this, true);
            MountService.this.mObbActionHandler.sendMessage(MountService.this.mObbActionHandler.obtainMessage(1, action));
        }

        public void link() throws RemoteException {
            getBinder().linkToDeath(this, 0);
        }

        public void unlink() {
            getBinder().unlinkToDeath(this, 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ObbState{");
            sb.append("rawPath=").append(this.rawPath);
            sb.append(",canonicalPath=").append(this.canonicalPath);
            sb.append(",ownerGid=").append(this.ownerGid);
            sb.append(",token=").append(this.token);
            sb.append(",binder=").append(getBinder());
            sb.append('}');
            return sb.toString();
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(MountService.TAG, "onServiceConnected");
            IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
            MountService.this.mObbActionHandler.sendMessage(MountService.this.mObbActionHandler.obtainMessage(2, imcs));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(MountService.TAG, "onServiceDisconnected");
        }
    }

    class MountServiceHandler extends Handler {
        public MountServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            VolumeInfo curVol;
            switch (msg.what) {
                case 1:
                    Slog.i(MountService.TAG, "handleMessage:H_SYSTEM_READY");
                    MountService.this.handleSystemReady();
                    return;
                case 2:
                    Slog.i(MountService.TAG, "H_DAEMON_CONNECTED");
                    MountService.this.handleDaemonConnected();
                    return;
                case 3:
                    Slog.i(MountService.TAG, "H_SHUTDOWN");
                    boolean unused = MountService.isShuttingDown = true;
                    IMountShutdownObserver obs = (IMountShutdownObserver) msg.obj;
                    boolean success = false;
                    try {
                        success = MountService.this.mConnector.execute(MountService.TAG_VOLUME, "shutdown").isClassOk();
                        break;
                    } catch (NativeDaemonConnectorException e) {
                    }
                    if (obs != null) {
                        try {
                            obs.onShutDownComplete(success ? 0 : -1);
                            break;
                        } catch (RemoteException e2) {
                        }
                    }
                    Slog.i(MountService.TAG, "finsh shut down");
                    boolean unused2 = MountService.isShuttingDown = false;
                    return;
                case 4:
                    Slog.i(MountService.TAG, "H_FSTRIM");
                    if (!MountService.this.isReady()) {
                        Slog.i(MountService.TAG, "fstrim requested, but no daemon connection yet; trying again");
                        sendMessageDelayed(obtainMessage(4, msg.obj), 1000L);
                        return;
                    }
                    Slog.i(MountService.TAG, "Running fstrim idle maintenance");
                    try {
                        MountService.this.mLastMaintenance = System.currentTimeMillis();
                        MountService.this.mLastMaintenanceFile.setLastModified(MountService.this.mLastMaintenance);
                        break;
                    } catch (Exception e3) {
                        Slog.e(MountService.TAG, "Unable to record last fstrim!");
                    }
                    boolean shouldBenchmark = MountService.this.shouldBenchmark();
                    try {
                        NativeDaemonConnector nativeDaemonConnector = MountService.this.mConnector;
                        Object[] objArr = new Object[1];
                        objArr[0] = shouldBenchmark ? "dotrimbench" : "dotrim";
                        nativeDaemonConnector.execute("fstrim", objArr);
                        break;
                    } catch (NativeDaemonConnectorException e4) {
                        Slog.e(MountService.TAG, "Failed to run fstrim!");
                    }
                    Runnable callback = (Runnable) msg.obj;
                    if (callback == null) {
                        return;
                    }
                    callback.run();
                    return;
                case 5:
                    Slog.i(MountService.TAG, "H_VOLUME_MOUNT");
                    VolumeInfo vol = (VolumeInfo) msg.obj;
                    if (MountService.this.isMountDisallowed(vol)) {
                        Slog.i(MountService.TAG, "Ignoring mount " + vol.getId() + " due to policy");
                        return;
                    }
                    int rc = 0;
                    try {
                        MountService.this.mConnector.execute(MountService.TAG_VOLUME, "mount", vol.id, Integer.valueOf(vol.mountFlags), Integer.valueOf(vol.mountUserId));
                        break;
                    } catch (NativeDaemonConnectorException ignored) {
                        rc = ignored.getCode();
                        Slog.w(MountService.TAG, "mount volume fail, ignored=" + ignored);
                    }
                    if (rc == 0) {
                        synchronized (MountService.this.mLock) {
                            curVol = (VolumeInfo) MountService.this.mVolumes.get(vol.getId());
                        }
                        if (!MountService.this.isShowDefaultPathDialog(curVol)) {
                            return;
                        }
                        MountService.this.showDefaultPathDialog(curVol);
                        return;
                    }
                    MountService.this.isDiskInsert = false;
                    Slog.w(MountService.TAG, "mount volume fail, vol=" + vol + ", return code=" + rc);
                    return;
                case 6:
                    Slog.i(MountService.TAG, "H_VOLUME_BROADCAST");
                    StorageVolume userVol = (StorageVolume) msg.obj;
                    String envState = userVol.getState();
                    Slog.d(MountService.TAG, "Volume " + userVol.getId() + " broadcasting " + envState + " to " + userVol.getOwner());
                    String action = VolumeInfo.getBroadcastForEnvironment(envState);
                    if (action == null) {
                        return;
                    }
                    Intent intent = new Intent(action, Uri.fromFile(userVol.getPathFile()));
                    intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", userVol);
                    intent.addFlags(67108864);
                    Slog.i(MountService.TAG, "sendBroadcastAsUser, intent=" + intent + ", userVol=" + userVol);
                    MountService.this.mContext.sendBroadcastAsUser(intent, userVol.getOwner());
                    return;
                case 7:
                    MountService.this.mContext.sendBroadcastAsUser((Intent) msg.obj, UserHandle.ALL, "android.permission.WRITE_MEDIA_STORAGE");
                    return;
                case 8:
                    MountService.this.unmount(((VolumeInfo) msg.obj).getId());
                    return;
                case 9:
                    String partGuid = (String) msg.obj;
                    MountService.this.forgetPartition(partGuid);
                    return;
                case 10:
                    MountService.this.resetIfReadyAndConnected();
                    return;
                default:
                    return;
            }
        }
    }

    public void waitForAsecScan() {
        waitForLatch(this.mAsecsScanned, "mAsecsScanned");
    }

    private void waitForReady() {
        waitForLatch(this.mConnectedSignal, "mConnectedSignal");
    }

    private void waitForLatch(CountDownLatch latch, String condition) {
        try {
            waitForLatch(latch, condition, -1L);
        } catch (TimeoutException e) {
        }
    }

    private void waitForLatch(CountDownLatch latch, String condition, long timeoutMillis) throws TimeoutException {
        long startMillis = SystemClock.elapsedRealtime();
        while (!latch.await(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC, TimeUnit.MILLISECONDS)) {
            try {
                Slog.w(TAG, "Thread " + Thread.currentThread().getName() + " still waiting for " + condition + "...");
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupt while waiting for " + condition);
            }
            if (timeoutMillis > 0 && SystemClock.elapsedRealtime() > startMillis + timeoutMillis) {
                throw new TimeoutException("Thread " + Thread.currentThread().getName() + " gave up waiting for " + condition + " after " + timeoutMillis + "ms");
            }
        }
    }

    private boolean isReady() {
        try {
            return this.mConnectedSignal.await(0L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void handleSystemReady() {
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        if (this.mSendUmsConnectedOnBoot) {
            sendUmsIntent(true);
            this.mSendUmsConnectedOnBoot = false;
        }
        MountServiceIdler.scheduleIdlePass(this.mContext);
    }

    @Deprecated
    private void killMediaProvider(List<UserInfo> users) {
        ProviderInfo provider;
        if (users == null) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (UserInfo user : users) {
                if (!user.isSystemOnly() && (provider = this.mPms.resolveContentProvider("media", 786432, user.id)) != null) {
                    IActivityManager am = ActivityManagerNative.getDefault();
                    try {
                        am.killApplication(provider.applicationInfo.packageName, UserHandle.getAppId(provider.applicationInfo.uid), -1, "vold reset");
                        break;
                    } catch (RemoteException e) {
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void addInternalVolumeLocked() {
        VolumeInfo internal = new VolumeInfo("private", 1, (DiskInfo) null, (String) null);
        internal.state = 2;
        internal.path = Environment.getDataDirectory().getAbsolutePath();
        this.mVolumes.put(internal.id, internal);
    }

    private void initIfReadyAndConnected() {
        Slog.d(TAG, "Thinking about init, mSystemReady=" + this.mSystemReady + ", mDaemonConnected=" + this.mDaemonConnected);
        if (!this.mSystemReady || !this.mDaemonConnected || StorageManager.isFileEncryptedNativeOnly()) {
            return;
        }
        boolean initLocked = StorageManager.isFileEncryptedEmulatedOnly();
        Slog.d(TAG, "Setting up emulation state, initlocked=" + initLocked);
        List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
        for (UserInfo user : users) {
            if (initLocked) {
                try {
                    this.mCryptConnector.execute("cryptfs", "lock_user_key", Integer.valueOf(user.id));
                } catch (NativeDaemonConnectorException e) {
                    Slog.w(TAG, "Failed to init vold", e);
                }
            } else {
                this.mCryptConnector.execute("cryptfs", "unlock_user_key", Integer.valueOf(user.id), Integer.valueOf(user.serialNumber), "!", "!");
            }
        }
    }

    private void resetIfReadyAndConnected() {
        int[] systemUnlockedUsers;
        Slog.d(TAG, "Thinking about reset, mSystemReady=" + this.mSystemReady + ", mDaemonConnected=" + this.mDaemonConnected);
        if (!this.mSystemReady || !this.mDaemonConnected) {
            return;
        }
        List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
        killMediaProvider(users);
        synchronized (this.mLock) {
            systemUnlockedUsers = this.mSystemUnlockedUsers;
            this.mDisks.clear();
            this.mVolumes.clear();
            addInternalVolumeLocked();
        }
        try {
            this.mConnector.execute(TAG_VOLUME, "reset");
            for (UserInfo user : users) {
                this.mConnector.execute(TAG_VOLUME, "user_added", Integer.valueOf(user.id), Integer.valueOf(user.serialNumber));
            }
            for (int userId : systemUnlockedUsers) {
                this.mConnector.execute(TAG_VOLUME, "user_started", Integer.valueOf(userId));
            }
        } catch (NativeDaemonConnectorException e) {
            Slog.w(TAG, "Failed to reset vold", e);
        }
    }

    private void onUnlockUser(int userId) {
        Slog.d(TAG, "onUnlockUser " + userId);
        try {
            this.mConnector.execute(TAG_VOLUME, "user_started", Integer.valueOf(userId));
        } catch (NativeDaemonConnectorException e) {
        }
        synchronized (this.mVolumes) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.isVisibleForRead(userId) && vol.isMountedReadable()) {
                    StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                    this.mHandler.obtainMessage(6, userVol).sendToTarget();
                    String envState = VolumeInfo.getEnvironmentForState(vol.getState());
                    this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), envState, envState);
                }
            }
            this.mSystemUnlockedUsers = ArrayUtils.appendInt(this.mSystemUnlockedUsers, userId);
        }
    }

    private void onCleanupUser(int userId) {
        Slog.d(TAG, "onCleanupUser " + userId);
        try {
            this.mConnector.execute(TAG_VOLUME, "user_stopped", Integer.valueOf(userId));
        } catch (NativeDaemonConnectorException e) {
        }
        synchronized (this.mVolumes) {
            this.mSystemUnlockedUsers = ArrayUtils.removeInt(this.mSystemUnlockedUsers, userId);
        }
    }

    void runIdleMaintenance(Runnable callback) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, callback));
    }

    public void runMaintenance() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        runIdleMaintenance(null);
    }

    public long lastMaintenance() {
        return this.mLastMaintenance;
    }

    @Override
    public void onDaemonConnected() {
        this.mDaemonConnected = true;
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    private void handleDaemonConnected() {
        Slog.i(TAG, "handleDaemonConnected");
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        this.mConnectedSignal.countDown();
        if (this.mConnectedSignal.getCount() != 0) {
            return;
        }
        if ("".equals(SystemProperties.get("vold.encrypt_progress"))) {
            copyLocaleFromMountService();
        }
        this.mPms.scanAvailableAsecs();
        this.mAsecsScanned.countDown();
    }

    private void copyLocaleFromMountService() {
        try {
            String systemLocale = getField("SystemLocale");
            if (TextUtils.isEmpty(systemLocale)) {
                return;
            }
            Slog.d(TAG, "Got locale " + systemLocale + " from mount service");
            Locale locale = Locale.forLanguageTag(systemLocale);
            Configuration config = new Configuration();
            config.setLocale(locale);
            try {
                ActivityManagerNative.getDefault().updatePersistentConfiguration(config);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error setting system locale from mount service", e);
            }
            Slog.d(TAG, "Setting system properties to " + systemLocale + " from mount service");
            SystemProperties.set("persist.sys.locale", locale.toLanguageTag());
        } catch (RemoteException e2) {
        }
    }

    @Override
    public boolean onCheckHoldWakeLock(int code) {
        return false;
    }

    @Override
    public boolean onEvent(int code, String raw, String[] cooked) {
        boolean zOnEventLocked;
        synchronized (this.mLock) {
            zOnEventLocked = onEventLocked(code, raw, cooked);
        }
        return zOnEventLocked;
    }

    private boolean onEventLocked(int code, String raw, String[] cooked) {
        VolumeInfo vol;
        VolumeInfo vol2;
        VolumeInfo vol3;
        VolumeInfo vol4;
        VolumeInfo vol5;
        DiskInfo disk;
        DiskInfo disk2;
        DiskInfo disk3;
        switch (code) {
            case VoldResponseCode.DISK_CREATED:
                Slog.d(TAG, "DISK_CREATED");
                if (cooked.length == 3) {
                    String id = cooked[1];
                    int flags = Integer.parseInt(cooked[2]);
                    if (SystemProperties.getBoolean("persist.fw.force_adoptable", false) || this.mForceAdoptable) {
                        flags |= 1;
                    }
                    this.mDisks.put(id, new DiskInfo(id, flags));
                    Slog.d(TAG, "create diskInfo=" + this.mDisks.get(id));
                    this.isDiskInsert = true;
                }
                break;
            case VoldResponseCode.DISK_SIZE_CHANGED:
                Slog.d(TAG, "DISK_SIZE_CHANGED");
                if (cooked.length == 3 && (disk3 = this.mDisks.get(cooked[1])) != null) {
                    Slog.d(TAG, "disk size change from + " + disk3.size + " to " + Long.parseLong(cooked[2]));
                    disk3.size = Long.parseLong(cooked[2]);
                    break;
                }
                break;
            case VoldResponseCode.DISK_LABEL_CHANGED:
                Slog.d(TAG, "DISK_LABEL_CHANGED");
                DiskInfo disk4 = this.mDisks.get(cooked[1]);
                if (disk4 != null) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 2; i < cooked.length; i++) {
                        builder.append(cooked[i]).append(' ');
                    }
                    disk4.label = builder.toString().trim();
                    Slog.d(TAG, "DISK_LABEL_CHANGED, new label = " + disk4.label + ", diskInfo=" + disk4);
                }
                break;
            case VoldResponseCode.DISK_SCANNED:
                Slog.d(TAG, "DISK_SCANNED");
                if (cooked.length == 2 && (disk2 = this.mDisks.get(cooked[1])) != null) {
                    onDiskScannedLocked(disk2);
                    break;
                }
                break;
            case VoldResponseCode.DISK_SYS_PATH_CHANGED:
                if (cooked.length == 3 && (disk = this.mDisks.get(cooked[1])) != null) {
                    disk.sysPath = cooked[2];
                    break;
                }
                break;
            case 645:
            case 646:
            case 647:
            case 648:
            case 657:
            case 658:
            default:
                Slog.d(TAG, "Unhandled vold event " + code);
                break;
            case VoldResponseCode.DISK_DESTROYED:
                Slog.d(TAG, "DISK_DESTROYED");
                if (cooked.length == 2) {
                    DiskInfo disk5 = this.mDisks.remove(cooked[1]);
                    if (disk5 != null) {
                        this.mCallbacks.notifyDiskDestroyed(disk5);
                    }
                    updateDefaultPathIfNeed();
                }
                break;
            case VoldResponseCode.VOLUME_CREATED:
                Slog.d(TAG, "VOLUME_CREATED");
                String id2 = cooked[1];
                int type = Integer.parseInt(cooked[2]);
                String diskId = TextUtils.nullIfEmpty(cooked[3]);
                String partGuid = TextUtils.nullIfEmpty(cooked[4]);
                VolumeInfo vol6 = new VolumeInfo(id2, type, this.mDisks.get(diskId), partGuid);
                this.mVolumes.put(id2, vol6);
                onVolumeCreatedLocked(vol6);
                Slog.d(TAG, "create volumeInfo=" + this.mVolumes.get(id2));
                break;
            case VoldResponseCode.VOLUME_STATE_CHANGED:
                Slog.d(TAG, "VOLUME_STATE_CHANGED");
                if (cooked.length == 3 && (vol5 = this.mVolumes.get(cooked[1])) != null) {
                    int oldState = vol5.state;
                    int newState = Integer.parseInt(cooked[2]);
                    vol5.state = newState;
                    onVolumeStateChangedLocked(vol5, oldState, newState);
                    break;
                }
                break;
            case VoldResponseCode.VOLUME_FS_TYPE_CHANGED:
                Slog.d(TAG, "VOLUME_FS_TYPE_CHANGED");
                if (cooked.length == 3 && (vol4 = this.mVolumes.get(cooked[1])) != null) {
                    vol4.fsType = cooked[2];
                    Slog.d(TAG, "new fsType=" + vol4.fsType + ", volumeInfo=" + vol4);
                    break;
                }
                break;
            case VoldResponseCode.VOLUME_FS_UUID_CHANGED:
                Slog.d(TAG, "VOLUME_FS_UUID_CHANGED");
                if (cooked.length == 3 && (vol3 = this.mVolumes.get(cooked[1])) != null) {
                    vol3.fsUuid = cooked[2];
                    Slog.d(TAG, "new fsUuid=" + vol3.fsUuid + ", volumeInfo=" + vol3);
                    break;
                }
                break;
            case VoldResponseCode.VOLUME_FS_LABEL_CHANGED:
                Slog.d(TAG, "VOLUME_FS_LABEL_CHANGED");
                VolumeInfo vol7 = this.mVolumes.get(cooked[1]);
                if (vol7 != null) {
                    StringBuilder builder2 = new StringBuilder();
                    for (int i2 = 2; i2 < cooked.length; i2++) {
                        builder2.append(cooked[i2]).append(' ');
                    }
                    vol7.fsLabel = builder2.toString().trim();
                    Slog.d(TAG, "new fsLabel=" + vol7.fsLabel + ", volumeInfo=" + vol7);
                }
                break;
            case VoldResponseCode.VOLUME_PATH_CHANGED:
                Slog.d(TAG, "VOLUME_PATH_CHANGED");
                if (cooked.length == 3 && (vol2 = this.mVolumes.get(cooked[1])) != null) {
                    vol2.path = cooked[2];
                    Slog.d(TAG, "new path= " + vol2.path + ", volumeInfo=" + vol2);
                    break;
                }
                break;
            case VoldResponseCode.VOLUME_INTERNAL_PATH_CHANGED:
                Slog.d(TAG, "VOLUME_INTERNAL_PATH_CHANGED");
                if (cooked.length == 3 && (vol = this.mVolumes.get(cooked[1])) != null) {
                    vol.internalPath = cooked[2];
                    Slog.d(TAG, "new internal path= " + vol.internalPath + ", volumeInfo=" + vol);
                    break;
                }
                break;
            case VoldResponseCode.VOLUME_DESTROYED:
                Slog.d(TAG, "VOLUME_DESTROYED");
                if (cooked.length == 2) {
                    Slog.d(TAG, "destroyed volumeInfo=" + this.mVolumes.get(cooked[1]));
                    this.mVolumes.remove(cooked[1]);
                }
                break;
            case VoldResponseCode.MOVE_STATUS:
                Slog.d(TAG, "MOVE_STATUS");
                int status = Integer.parseInt(cooked[1]);
                onMoveStatusLocked(status);
                break;
            case VoldResponseCode.BENCHMARK_RESULT:
                if (cooked.length == 7) {
                    String path = cooked[1];
                    String ident = cooked[2];
                    long create = Long.parseLong(cooked[3]);
                    Long.parseLong(cooked[4]);
                    long run = Long.parseLong(cooked[5]);
                    long destroy = Long.parseLong(cooked[6]);
                    DropBoxManager dropBox = (DropBoxManager) this.mContext.getSystemService(DropBoxManager.class);
                    dropBox.addText(TAG_STORAGE_BENCHMARK, scrubPath(path) + " " + ident + " " + create + " " + run + " " + destroy);
                    VolumeRecord rec = findRecordForPath(path);
                    if (rec != null) {
                        rec.lastBenchMillis = System.currentTimeMillis();
                        writeSettingsLocked();
                    }
                }
                break;
            case VoldResponseCode.TRIM_RESULT:
                if (cooked.length == 4) {
                    String path2 = cooked[1];
                    long bytes = Long.parseLong(cooked[2]);
                    long time = Long.parseLong(cooked[3]);
                    DropBoxManager dropBox2 = (DropBoxManager) this.mContext.getSystemService(DropBoxManager.class);
                    dropBox2.addText(TAG_STORAGE_TRIM, scrubPath(path2) + " " + bytes + " " + time);
                    VolumeRecord rec2 = findRecordForPath(path2);
                    if (rec2 != null) {
                        rec2.lastTrimMillis = System.currentTimeMillis();
                        writeSettingsLocked();
                    }
                }
                break;
        }
        return true;
    }

    private void onDiskScannedLocked(DiskInfo disk) {
        Slog.d(TAG, "onDiskScannedLocked, diskInfo=" + disk);
        int volumeCount = 0;
        for (int i = 0; i < this.mVolumes.size(); i++) {
            VolumeInfo vol = this.mVolumes.valueAt(i);
            if (Objects.equals(disk.id, vol.getDiskId())) {
                volumeCount++;
            }
        }
        Slog.d(TAG, "this disk has " + volumeCount + " volumes");
        Intent intent = new Intent("android.os.storage.action.DISK_SCANNED");
        intent.addFlags(83886080);
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.id);
        intent.putExtra("android.os.storage.extra.VOLUME_COUNT", volumeCount);
        Slog.d(TAG, "sendBroadcastAsUser, intent=" + intent + ", disk.id=" + disk.id + ", volumeCount=" + volumeCount);
        this.mHandler.obtainMessage(7, intent).sendToTarget();
        CountDownLatch latch = this.mDiskScanLatches.remove(disk.id);
        if (latch != null) {
            latch.countDown();
        }
        disk.volumeCount = volumeCount;
        this.mCallbacks.notifyDiskScanned(disk, volumeCount);
    }

    private void onVolumeCreatedLocked(VolumeInfo vol) {
        if (this.mPms.isOnlyCoreApps()) {
            Slog.d(TAG, "System booted in core-only mode; ignoring volume " + vol.getId());
            return;
        }
        Slog.d(TAG, "onVolumeCreatedLocked, volumeInfo=" + vol);
        if (vol.type == 2) {
            StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            VolumeInfo privateVol = storage.findPrivateForEmulated(vol);
            Slog.d(TAG, "privateVol=" + privateVol);
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid) && "private".equals(privateVol.id)) {
                Slog.v(TAG, "Found primary storage at " + vol);
                vol.mountFlags |= 1;
                vol.mountFlags |= 2;
                this.mHandler.obtainMessage(5, vol).sendToTarget();
                return;
            }
            if (!Objects.equals(privateVol.fsUuid, this.mPrimaryStorageUuid)) {
                return;
            }
            Slog.v(TAG, "Found primary storage at " + vol);
            vol.mountFlags |= 1;
            vol.mountFlags |= 2;
            this.mHandler.obtainMessage(5, vol).sendToTarget();
            return;
        }
        if (vol.type == 0) {
            if (Objects.equals("primary_physical", this.mPrimaryStorageUuid) && vol.disk.isDefaultPrimary()) {
                Slog.v(TAG, "Found primary storage at " + vol);
                vol.mountFlags |= 1;
                vol.mountFlags |= 2;
            }
            if (vol.disk.isAdoptable() || vol.isPhoneStorage()) {
                vol.mountFlags |= 2;
            }
            vol.mountUserId = this.mCurrentUserId;
            this.mHandler.obtainMessage(5, vol).sendToTarget();
            return;
        }
        if (vol.type == 1) {
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else {
            Slog.d(TAG, "Skipping automatic mounting of " + vol);
        }
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

    private void onVolumeStateChangedLocked(VolumeInfo vol, int oldState, int newState) {
        Slog.d(TAG, "onVolumeStateChangedLocked, oldState=" + VolumeInfo.getEnvironmentForState(oldState) + ", newState=" + VolumeInfo.getEnvironmentForState(newState) + ", volumeInfo=" + vol);
        if (vol.isMountedReadable() && !TextUtils.isEmpty(vol.fsUuid)) {
            VolumeRecord rec = this.mRecords.get(vol.fsUuid);
            if (rec == null) {
                VolumeRecord rec2 = new VolumeRecord(vol.type, vol.fsUuid);
                rec2.partGuid = vol.partGuid;
                rec2.createdMillis = System.currentTimeMillis();
                if (vol.type == 1) {
                    rec2.nickname = vol.disk.getDescription();
                }
                this.mRecords.put(rec2.fsUuid, rec2);
                writeSettingsLocked();
            } else if (TextUtils.isEmpty(rec.partGuid)) {
                rec.partGuid = vol.partGuid;
                writeSettingsLocked();
            }
        }
        this.mCallbacks.notifyVolumeStateChanged(vol, oldState, newState);
        if (this.mBootCompleted && isBroadcastWorthy(vol)) {
            Intent intent = new Intent("android.os.storage.action.VOLUME_STATE_CHANGED");
            intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.id);
            intent.putExtra("android.os.storage.extra.VOLUME_STATE", newState);
            intent.putExtra("android.os.storage.extra.FS_UUID", vol.fsUuid);
            intent.addFlags(83886080);
            Slog.d(TAG, "sendBroadcastAsUser, intent=" + intent + ", vol.id=" + vol.id + ", newState=" + newState + ", vol.fsUuid=" + vol.fsUuid + ", flags=FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT");
            this.mHandler.obtainMessage(7, intent).sendToTarget();
        }
        String oldStateEnv = VolumeInfo.getEnvironmentForState(oldState);
        String newStateEnv = VolumeInfo.getEnvironmentForState(newState);
        if (!Objects.equals(oldStateEnv, newStateEnv)) {
            for (int userId : this.mSystemUnlockedUsers) {
                if (vol.isVisibleForRead(userId)) {
                    StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                    this.mHandler.obtainMessage(6, userVol).sendToTarget();
                    Slog.d(TAG, "notify callbacks StorageStateChanged, storageVolume=" + userVol);
                    this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), oldStateEnv, newStateEnv);
                }
            }
        }
        if (vol.type == 0 && vol.state == 5) {
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(5, vol.path));
        }
        updateDefaultPathIfNeed();
    }

    private void onMoveStatusLocked(int status) {
        if (this.mMoveCallback == null) {
            Slog.w(TAG, "Odd, status but no move requested");
            return;
        }
        try {
            this.mMoveCallback.onStatusChanged(-1, status, -1L);
        } catch (RemoteException e) {
        }
        if (status == 82) {
            Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " copy phase finshed; persisting");
            this.mPrimaryStorageUuid = this.mMoveTargetUuid;
            writeSettingsLocked();
        }
        if (!PackageManager.isMoveStatusFinished(status)) {
            return;
        }
        Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " finished with status " + status);
        this.mMoveCallback = null;
        this.mMoveTargetUuid = null;
    }

    private void enforcePermission(String perm) {
        this.mContext.enforceCallingOrSelfPermission(perm, perm);
    }

    private boolean isMountDisallowed(VolumeInfo vol) {
        if (vol.type != 0 && vol.type != 1) {
            return false;
        }
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        return userManager.hasUserRestriction("no_physical_media", Binder.getCallingUserHandle());
    }

    private void enforceAdminUser() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            boolean isAdmin = um.getUserInfo(callingUserId).isAdmin();
            if (isAdmin) {
            } else {
                throw new SecurityException("Only admin users can adopt sd cards");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public MountService(Context context) {
        sSelf = this;
        this.mContext = context;
        this.mCallbacks = new Callbacks(FgThread.get().getLooper());
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPms = (PackageManagerService) ServiceManager.getService("package");
        HandlerThread hthread = new HandlerThread(TAG);
        hthread.start();
        this.mHandler = new MountServiceHandler(hthread.getLooper());
        this.mObbActionHandler = new ObbActionHandler(IoThread.get().getLooper());
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        this.mLastMaintenanceFile = new File(systemDir, LAST_FSTRIM_FILE);
        if (!this.mLastMaintenanceFile.exists()) {
            try {
                new FileOutputStream(this.mLastMaintenanceFile).close();
            } catch (IOException e) {
                Slog.e(TAG, "Unable to create fstrim record " + this.mLastMaintenanceFile.getPath());
            }
        } else {
            this.mLastMaintenance = this.mLastMaintenanceFile.lastModified();
        }
        this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "storage.xml"));
        synchronized (this.mLock) {
            readSettingsLocked();
        }
        LocalServices.addService(MountServiceInternal.class, this.mMountServiceInternal);
        this.mConnector = new NativeDaemonConnector(this, "vold", SystemService.PHASE_SYSTEM_SERVICES_READY, VOLD_TAG, 25, null);
        this.mConnector.setDebug(true);
        this.mConnector.setWarnIfHeld(this.mLock);
        this.mConnectorThread = new Thread(this.mConnector, VOLD_TAG);
        this.mCryptConnector = new NativeDaemonConnector(this, "cryptd", SystemService.PHASE_SYSTEM_SERVICES_READY, CRYPTD_TAG, 25, null);
        this.mCryptConnector.setDebug(true);
        this.mCryptConnectorThread = new Thread(this.mCryptConnector, CRYPTD_TAG);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        synchronized (this.mLock) {
            addInternalVolumeLocked();
        }
        initMTKFeature();
    }

    private void start() {
        this.mConnectorThread.start();
        this.mCryptConnectorThread.start();
    }

    private void systemReady() {
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

    private void readSettingsLocked() {
        Slog.i(TAG, "readSettingsLocked");
        this.mRecords.clear();
        this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
        this.mForceAdoptable = false;
        FileInputStream fis = null;
        try {
            fis = this.mSettingsFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int type = in.next();
                if (type == 1) {
                    return;
                }
                if (type == 2) {
                    String tag = in.getName();
                    if (TAG_VOLUMES.equals(tag)) {
                        int version = XmlUtils.readIntAttribute(in, ATTR_VERSION, 1);
                        boolean primaryPhysical = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                        boolean validAttr = version < 3 ? version >= 2 && !primaryPhysical : true;
                        if (validAttr) {
                            this.mPrimaryStorageUuid = XmlUtils.readStringAttribute(in, ATTR_PRIMARY_STORAGE_UUID);
                        }
                        this.mForceAdoptable = XmlUtils.readBooleanAttribute(in, ATTR_FORCE_ADOPTABLE, false);
                        Slog.i(TAG, "read start tag: version=" + version + ", primaryPhysical=" + primaryPhysical + ", mPrimaryStorageUuid=" + this.mPrimaryStorageUuid + ", mForceAdoptable=" + this.mForceAdoptable);
                    } else if (TAG_VOLUME.equals(tag)) {
                        VolumeRecord rec = readVolumeRecord(in);
                        Slog.i(TAG, "read volume tag: volumeRecode=" + rec);
                        this.mRecords.put(rec.fsUuid, rec);
                    }
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            Slog.wtf(TAG, "Failed reading metadata", e2);
        } catch (XmlPullParserException e3) {
            Slog.wtf(TAG, "Failed reading metadata", e3);
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    private void writeSettingsLocked() {
        Slog.i(TAG, "writeSettingsLocked");
        FileOutputStream fos = null;
        try {
            fos = this.mSettingsFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_VOLUMES);
            XmlUtils.writeIntAttribute(fastXmlSerializer, ATTR_VERSION, 3);
            XmlUtils.writeStringAttribute(fastXmlSerializer, ATTR_PRIMARY_STORAGE_UUID, this.mPrimaryStorageUuid);
            XmlUtils.writeBooleanAttribute(fastXmlSerializer, ATTR_FORCE_ADOPTABLE, this.mForceAdoptable);
            Slog.i(TAG, "write start tag: version=3, mPrimaryStorageUuid=" + this.mPrimaryStorageUuid + ", mForceAdoptable=" + this.mForceAdoptable);
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                VolumeRecord rec = this.mRecords.valueAt(i);
                Slog.i(TAG, "write volume record: " + rec);
                writeVolumeRecord(fastXmlSerializer, rec);
            }
            fastXmlSerializer.endTag(null, TAG_VOLUMES);
            fastXmlSerializer.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mSettingsFile.failWrite(fos);
            }
        }
    }

    public static VolumeRecord readVolumeRecord(XmlPullParser in) throws IOException {
        int type = XmlUtils.readIntAttribute(in, "type");
        String fsUuid = XmlUtils.readStringAttribute(in, ATTR_FS_UUID);
        VolumeRecord meta = new VolumeRecord(type, fsUuid);
        meta.partGuid = XmlUtils.readStringAttribute(in, ATTR_PART_GUID);
        meta.nickname = XmlUtils.readStringAttribute(in, ATTR_NICKNAME);
        meta.userFlags = XmlUtils.readIntAttribute(in, ATTR_USER_FLAGS);
        meta.createdMillis = XmlUtils.readLongAttribute(in, ATTR_CREATED_MILLIS);
        meta.lastTrimMillis = XmlUtils.readLongAttribute(in, ATTR_LAST_TRIM_MILLIS);
        meta.lastBenchMillis = XmlUtils.readLongAttribute(in, ATTR_LAST_BENCH_MILLIS);
        return meta;
    }

    public static void writeVolumeRecord(XmlSerializer out, VolumeRecord rec) throws IOException {
        out.startTag(null, TAG_VOLUME);
        XmlUtils.writeIntAttribute(out, "type", rec.type);
        XmlUtils.writeStringAttribute(out, ATTR_FS_UUID, rec.fsUuid);
        XmlUtils.writeStringAttribute(out, ATTR_PART_GUID, rec.partGuid);
        XmlUtils.writeStringAttribute(out, ATTR_NICKNAME, rec.nickname);
        XmlUtils.writeIntAttribute(out, ATTR_USER_FLAGS, rec.userFlags);
        XmlUtils.writeLongAttribute(out, ATTR_CREATED_MILLIS, rec.createdMillis);
        XmlUtils.writeLongAttribute(out, ATTR_LAST_TRIM_MILLIS, rec.lastTrimMillis);
        XmlUtils.writeLongAttribute(out, ATTR_LAST_BENCH_MILLIS, rec.lastBenchMillis);
        out.endTag(null, TAG_VOLUME);
    }

    public void registerListener(IMountServiceListener listener) {
        this.mCallbacks.register(listener);
    }

    public void unregisterListener(IMountServiceListener listener) {
        this.mCallbacks.unregister(listener);
    }

    public void shutdown(IMountShutdownObserver observer) {
        enforcePermission("android.permission.SHUTDOWN");
        Slog.i(TAG, "Shutting down");
        waitMTKNetlogStopped();
        this.mHandler.obtainMessage(3, observer).sendToTarget();
    }

    public boolean isUsbMassStorageConnected() {
        Slog.i(TAG, "isUsbMassStorageConnected");
        waitForReady();
        if (getUmsEnabling()) {
            Slog.i(TAG, "isUsbMassStorageConnected return true");
            return true;
        }
        Slog.i(TAG, "isUsbMassStorageConnected return " + this.mIsUsbConnected);
        return this.mIsUsbConnected;
    }

    public void setUsbMassStorageEnabled(boolean enable) {
        Slog.d(TAG, "setUsbMassStorageEnabled, enable=" + enable);
        waitForReady();
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        validateUserRestriction("no_usb_file_transfer");
        int userId = this.mCurrentUserId;
        this.mIsTurnOnOffUsb = true;
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (!vol.isAllowUsbMassStorage(userId)) {
                    Slog.d(TAG, "no need share, skip volume=" + vol);
                } else if (enable) {
                    setUmsEnabling(true);
                    if (vol.getState() == 2 || vol.getState() == 3) {
                        Slog.d(TAG, "setUsbMassStorageEnabled, first unmount volume=" + vol);
                        unmount(vol.getId());
                        Slog.d(TAG, "setUsbMassStorageEnabled, second share volume");
                        doShareUnshareVolume(vol.getId(), true);
                    } else if (vol.getState() == 0) {
                        Slog.d(TAG, "setUsbMassStorageEnabled, just share volume");
                        doShareUnshareVolume(vol.getId(), true);
                    }
                    setUmsEnabling(false);
                } else if (vol.getState() != 7 && vol.getState() != 8) {
                    Slog.d(TAG, "setUsbMassStorageEnabled, first unshare volume=" + vol);
                    doShareUnshareVolume(vol.getId(), false);
                    Slog.d(TAG, "setUsbMassStorageEnabled, second mount volume");
                    mount(vol.getId());
                }
            }
        }
        this.mIsTurnOnOffUsb = false;
    }

    public boolean isUsbMassStorageEnabled() {
        Slog.i(TAG, "isUsbMassStorageEnabled");
        waitForReady();
        boolean result = false;
        synchronized (this.mVolumes) {
            int i = 0;
            while (true) {
                if (i >= this.mVolumes.size()) {
                    break;
                }
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (!isVolumeSharedEnable(vol)) {
                    i++;
                } else {
                    result = true;
                    break;
                }
            }
        }
        Slog.i(TAG, "isUsbMassStorageEnabled return + " + result);
        return result;
    }

    public String getVolumeState(String mountPoint) {
        throw new UnsupportedOperationException();
    }

    public boolean isExternalStorageEmulated() {
        throw new UnsupportedOperationException();
    }

    public int mountVolume(String path) {
        Slog.i(TAG, "mountVolume, path=" + path);
        mount(findVolumeIdForPathOrThrow(path));
        return 0;
    }

    public void unmountVolume(String path, boolean force, boolean removeEncryption) {
        Slog.i(TAG, "unmountVolume, path=" + path);
        unmount(findVolumeIdForPathOrThrow(path));
    }

    public int formatVolume(String path) {
        Slog.i(TAG, "formatVolume, path=" + path);
        format(findVolumeIdForPathOrThrow(path));
        return 0;
    }

    public void mount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        Slog.i(TAG, "mount, volId=" + volId + ", volumeInfo=" + vol);
        if (isMountDisallowed(vol)) {
            throw new SecurityException("Mounting " + volId + " restricted by policy");
        }
        try {
            this.mConnector.execute(TAG_VOLUME, "mount", vol.id, Integer.valueOf(vol.mountFlags), Integer.valueOf(vol.mountUserId));
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "mount" + vol + "ERROR!!");
        }
    }

    public void unmount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        Slog.i(TAG, "unmount, volId=" + volId + ", volumeInfo=" + vol);
        if (vol.isPrimaryPhysical()) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mUnmountLock) {
                    this.mUnmountSignal = new CountDownLatch(1);
                    this.mPms.updateExternalMediaStatus(false, true);
                    waitForLatch(this.mUnmountSignal, "mUnmountSignal");
                    this.mUnmountSignal = null;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        try {
            this.mConnector.execute(TAG_VOLUME, "unmount", vol.id);
            updateDefaultPathIfNeed();
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void format(String volId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        waitForReady();
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        Slog.i(TAG, "format, volId=" + volId + ", volumeInfo=" + vol);
        try {
            this.mConnector.execute(TAG_VOLUME, "format", vol.id, "auto");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public long benchmark(String volId) throws Throwable {
        Slog.i(TAG, "benchmark, volId=" + volId);
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        waitForReady();
        try {
            NativeDaemonEvent res = this.mConnector.execute(180000L, TAG_VOLUME, "benchmark", volId);
            return Long.parseLong(res.getMessage());
        } catch (NativeDaemonTimeoutException e) {
            return JobStatus.NO_LATEST_RUNTIME;
        } catch (NativeDaemonConnectorException e2) {
            throw e2.rethrowAsParcelableException();
        }
    }

    public void partitionPublic(String diskId) {
        Slog.i(TAG, "partitionPublic, diskId=" + diskId);
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        waitForReady();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mConnector.execute(TAG_VOLUME, "partition", diskId, "public");
            waitForLatch(latch, "partitionPublic", 180000L);
            Slog.i(TAG, "partitionPublic return");
        } catch (NativeDaemonConnectorException e) {
            Slog.i(TAG, "partitionPublic NativeDaemonConnectorException, e=" + e.getMessage());
            popFormatFailToast();
            throw e.rethrowAsParcelableException();
        } catch (TimeoutException e2) {
            Slog.i(TAG, "partitionPublic timeout exception, e=" + e2.getMessage());
            popFormatFailToast();
            throw new IllegalStateException(e2);
        }
    }

    public void partitionPrivate(String diskId) {
        Slog.i(TAG, "partitionPrivate, diskId=" + diskId);
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        waitForReady();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mConnector.execute(TAG_VOLUME, "partition", diskId, "private");
            waitForLatch(latch, "partitionPrivate", 180000L);
            Slog.i(TAG, "partitionPrivate return");
        } catch (NativeDaemonConnectorException e) {
            Slog.i(TAG, "partitionPrivate NativeDaemonConnectorException, e=" + e.getMessage());
            popFormatFailToast();
            throw e.rethrowAsParcelableException();
        } catch (TimeoutException e2) {
            Slog.i(TAG, "partitionPrivate timeout exception, e=" + e2.getMessage());
            popFormatFailToast();
            throw new IllegalStateException(e2);
        }
    }

    public void partitionMixed(String diskId, int ratio) {
        Slog.i(TAG, "partitionMixed, diskId=" + diskId + ", ratio=" + ratio);
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        waitForReady();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mConnector.execute(TAG_VOLUME, "partition", diskId, "mixed", Integer.valueOf(ratio));
            waitForLatch(latch, "partitionMixed", 180000L);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        } catch (TimeoutException e2) {
            throw new IllegalStateException(e2);
        }
    }

    public void setVolumeNickname(String fsUuid, String nickname) {
        Slog.i(TAG, "setVolumeNickname, fsUuid=" + fsUuid + ", nickname=" + nickname);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.get(fsUuid);
            rec.nickname = nickname;
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void setVolumeUserFlags(String fsUuid, int flags, int mask) {
        Slog.i(TAG, "setVolumeUserFlags, fsUuid=" + fsUuid + ", flags=" + flags + ", mask=" + mask);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.get(fsUuid);
            rec.userFlags = (rec.userFlags & (~mask)) | (flags & mask);
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void forgetVolume(String fsUuid) {
        Slog.i(TAG, "forgetVolume, fsUuid=" + fsUuid);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.remove(fsUuid);
            if (rec != null && !TextUtils.isEmpty(rec.partGuid)) {
                this.mHandler.obtainMessage(9, rec.partGuid).sendToTarget();
            }
            this.mCallbacks.notifyVolumeForgotten(fsUuid);
            if (Objects.equals(this.mPrimaryStorageUuid, fsUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
                this.mHandler.obtainMessage(10).sendToTarget();
            }
            writeSettingsLocked();
        }
    }

    public void forgetAllVolumes() {
        Slog.i(TAG, "forgetAllVolumes");
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                String fsUuid = this.mRecords.keyAt(i);
                VolumeRecord rec = this.mRecords.valueAt(i);
                if (!TextUtils.isEmpty(rec.partGuid)) {
                    this.mHandler.obtainMessage(9, rec.partGuid).sendToTarget();
                }
                this.mCallbacks.notifyVolumeForgotten(fsUuid);
            }
            this.mRecords.clear();
            if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
            }
            writeSettingsLocked();
            resetIfReadyAndConnected();
        }
    }

    private void forgetPartition(String partGuid) {
        Slog.i(TAG, "forgetPartition, partGuid=" + partGuid);
        try {
            this.mConnector.execute(TAG_VOLUME, "forget_partition", partGuid);
        } catch (NativeDaemonConnectorException e) {
            Slog.w(TAG, "Failed to forget key for " + partGuid + ": " + e);
        }
    }

    private void remountUidExternalStorage(int uid, int mode) {
        waitForReady();
        String modeName = "none";
        switch (mode) {
            case 1:
                modeName = "default";
                break;
            case 2:
                modeName = "read";
                break;
            case 3:
                modeName = "write";
                break;
        }
        try {
            this.mConnector.execute(TAG_VOLUME, "remount_uid", Integer.valueOf(uid), modeName);
        } catch (NativeDaemonConnectorException e) {
            Slog.w(TAG, "Failed to remount UID " + uid + " as " + modeName + ": " + e);
        }
    }

    public void setDebugFlags(int flags, int mask) {
        String value;
        long token;
        Slog.i(TAG, "setDebugFlags, flags=" + flags + ", mask=" + mask);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        if ((mask & 2) != 0) {
            if (StorageManager.isFileEncryptedNativeOnly()) {
                throw new IllegalStateException("Emulation not available on device with native FBE");
            }
            if (this.mLockPatternUtils.isCredentialRequiredToDecrypt(false)) {
                throw new IllegalStateException("Emulation requires disabling 'Secure start-up' in Settings > Security");
            }
            token = Binder.clearCallingIdentity();
            boolean emulateFbe = (flags & 2) != 0;
            try {
                SystemProperties.set("persist.sys.emulate_fbe", Boolean.toString(emulateFbe));
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(null);
            } finally {
            }
        }
        if ((mask & 1) != 0) {
            synchronized (this.mLock) {
                this.mForceAdoptable = (flags & 1) != 0;
                writeSettingsLocked();
                this.mHandler.obtainMessage(10).sendToTarget();
            }
        }
        if ((mask & 12) == 0) {
            return;
        }
        if ((flags & 4) != 0) {
            value = "force_on";
        } else if ((flags & 8) != 0) {
            value = "force_off";
        } else {
            value = "";
        }
        token = Binder.clearCallingIdentity();
        try {
            SystemProperties.set("persist.sys.sdcardfs", value);
            this.mHandler.obtainMessage(10).sendToTarget();
        } finally {
        }
    }

    public String getPrimaryStorageUuid() {
        String str;
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        synchronized (this.mLock) {
            Slog.i(TAG, "getPrimaryStorageUuid, mPrimaryStorageUuid=" + this.mPrimaryStorageUuid);
            str = this.mPrimaryStorageUuid;
        }
        return str;
    }

    public void setPrimaryStorageUuid(String volumeUuid, IPackageMoveObserver callback) {
        Slog.i(TAG, "setPrimaryStorageUuid, volumeUuid=" + volumeUuid);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        synchronized (this.mLock) {
            if (Objects.equals(this.mPrimaryStorageUuid, volumeUuid)) {
                throw new IllegalArgumentException("Primary storage already at " + volumeUuid);
            }
            if (this.mMoveCallback != null) {
                throw new IllegalStateException("Move already in progress");
            }
            this.mMoveCallback = callback;
            this.mMoveTargetUuid = volumeUuid;
            if (Objects.equals("primary_physical", this.mPrimaryStorageUuid) || Objects.equals("primary_physical", volumeUuid)) {
                Slog.d(TAG, "Skipping move to/from primary physical");
                onMoveStatusLocked(82);
                onMoveStatusLocked(-100);
                this.mHandler.obtainMessage(10).sendToTarget();
                return;
            }
            VolumeInfo from = findStorageForUuid(this.mPrimaryStorageUuid);
            VolumeInfo to = findStorageForUuid(volumeUuid);
            if (from == null) {
                Slog.w(TAG, "Failing move due to missing from volume " + this.mPrimaryStorageUuid);
                onMoveStatusLocked(-6);
            } else if (to == null) {
                Slog.w(TAG, "Failing move due to missing to volume " + volumeUuid);
                onMoveStatusLocked(-6);
            } else {
                try {
                    this.mConnector.execute(TAG_VOLUME, "move_storage", from.id, to.id);
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public int[] getStorageUsers(String path) {
        Slog.i(TAG, "getStorageUsers, path=" + path);
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        try {
            String[] r = NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("storage", DatabaseHelper.SoundModelContract.KEY_USERS, path), 112);
            int[] data = new int[r.length];
            for (int i = 0; i < r.length; i++) {
                String[] tok = r[i].split(" ");
                try {
                    data[i] = Integer.parseInt(tok[0]);
                } catch (NumberFormatException e) {
                    Slog.e(TAG, String.format("Error parsing pid %s", tok[0]));
                    return new int[0];
                }
            }
            return data;
        } catch (NativeDaemonConnectorException e2) {
            Slog.e(TAG, "Failed to retrieve storage users list", e2);
            return new int[0];
        }
    }

    private void warnOnNotMounted() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.isPrimary() && vol.isMountedWritable()) {
                    return;
                }
            }
            Slog.w(TAG, "No primary storage mounted!");
        }
    }

    public String[] getSecureContainerList() {
        enforcePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("asec", "list"), 111);
        } catch (NativeDaemonConnectorException e) {
            return new String[0];
        }
    }

    public int createSecureContainer(String id, int sizeMb, String fstype, String key, int ownerUid, boolean external) {
        Slog.i(TAG, "createSecureContainer, id=" + id + ", sizeMb=" + sizeMb + ", fstype=" + fstype + ", key=" + key);
        enforcePermission("android.permission.ASEC_CREATE");
        waitForReady();
        warnOnNotMounted();
        int rc = 0;
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[7];
            objArr[0] = "create";
            objArr[1] = id;
            objArr[2] = Integer.valueOf(sizeMb);
            objArr[3] = fstype;
            objArr[4] = new NativeDaemonConnector.SensitiveArg(key);
            objArr[5] = Integer.valueOf(ownerUid);
            objArr[6] = external ? "1" : "0";
            nativeDaemonConnector.execute("asec", objArr);
        } catch (NativeDaemonConnectorException e) {
            rc = -1;
        }
        if (rc == 0) {
            synchronized (this.mAsecMountSet) {
                this.mAsecMountSet.add(id);
            }
        }
        return rc;
    }

    public int resizeSecureContainer(String id, int sizeMb, String key) {
        Slog.i(TAG, "resizeSecureContainer, id=" + id + ", sizeMb=" + sizeMb + ", key=" + key);
        enforcePermission("android.permission.ASEC_CREATE");
        waitForReady();
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "resize", id, Integer.valueOf(sizeMb), new NativeDaemonConnector.SensitiveArg(key));
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int finalizeSecureContainer(String id) {
        Slog.i(TAG, "finalizeSecureContainer, id=" + id);
        enforcePermission("android.permission.ASEC_CREATE");
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "finalize", id);
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int fixPermissionsSecureContainer(String id, int gid, String filename) {
        Slog.i(TAG, "fixPermissionsSecureContainer, id=" + id + ", gid=" + gid + ", filename=" + filename);
        enforcePermission("android.permission.ASEC_CREATE");
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "fixperms", id, Integer.valueOf(gid), filename);
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int destroySecureContainer(String id, boolean force) {
        Slog.i(TAG, "destroySecureContainer, id=" + id + ", force=" + force);
        enforcePermission("android.permission.ASEC_DESTROY");
        waitForReady();
        warnOnNotMounted();
        Runtime.getRuntime().gc();
        int rc = 0;
        try {
            NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("asec", "destroy", id);
            if (force) {
                cmd.appendArg("force");
            }
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 405) {
                rc = -7;
            } else {
                rc = -1;
            }
        }
        if (rc == 0) {
            synchronized (this.mAsecMountSet) {
                if (this.mAsecMountSet.contains(id)) {
                    this.mAsecMountSet.remove(id);
                }
            }
        }
        return rc;
    }

    public int mountSecureContainer(String id, String key, int ownerUid, boolean readOnly) {
        Slog.i(TAG, "mountSecureContainer, id=" + id + ", key=" + key + ", ownerUid=" + ownerUid + ", readOnly=" + readOnly);
        enforcePermission("android.permission.ASEC_MOUNT_UNMOUNT");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (this.mAsecMountSet.contains(id)) {
                return -6;
            }
            int rc = 0;
            try {
                NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                Object[] objArr = new Object[5];
                objArr[0] = "mount";
                objArr[1] = id;
                objArr[2] = new NativeDaemonConnector.SensitiveArg(key);
                objArr[3] = Integer.valueOf(ownerUid);
                objArr[4] = readOnly ? "ro" : "rw";
                nativeDaemonConnector.execute("asec", objArr);
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                if (code != 405) {
                    rc = -1;
                }
            }
            if (rc == 0) {
                synchronized (this.mAsecMountSet) {
                    this.mAsecMountSet.add(id);
                }
            }
            return rc;
        }
    }

    public int unmountSecureContainer(String id, boolean force) {
        Slog.i(TAG, "unmountSecureContainer, id=" + id + ", force=" + force);
        enforcePermission("android.permission.ASEC_MOUNT_UNMOUNT");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (!this.mAsecMountSet.contains(id)) {
                Slog.i(TAG, "OperationFailedStorageNotMounted");
                return -5;
            }
            Runtime.getRuntime().gc();
            System.runFinalization();
            int rc = 0;
            try {
                NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("asec", "unmount", id);
                if (force) {
                    cmd.appendArg("force");
                }
                this.mConnector.execute(cmd);
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                if (code == 405) {
                    rc = -7;
                } else {
                    rc = -1;
                }
            }
            if (rc == 0) {
                synchronized (this.mAsecMountSet) {
                    this.mAsecMountSet.remove(id);
                }
            }
            return rc;
        }
    }

    public boolean isSecureContainerMounted(String id) {
        boolean zContains;
        enforcePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            zContains = this.mAsecMountSet.contains(id);
        }
        return zContains;
    }

    public int renameSecureContainer(String oldId, String newId) {
        Slog.i(TAG, "renameSecureContainer, oldId=" + oldId + ", newId=" + newId);
        enforcePermission("android.permission.ASEC_RENAME");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (!this.mAsecMountSet.contains(oldId)) {
                if (!this.mAsecMountSet.contains(newId)) {
                    try {
                        this.mConnector.execute("asec", "rename", oldId, newId);
                        return 0;
                    } catch (NativeDaemonConnectorException e) {
                        return -1;
                    }
                }
            }
            return -6;
        }
    }

    public String getSecureContainerPath(String id) {
        Slog.i(TAG, "getSecureContainerPath, id=" + id);
        enforcePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            NativeDaemonEvent event = this.mConnector.execute("asec", "path", id);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 406) {
                Slog.i(TAG, String.format("Container '%s' not found", id));
                return null;
            }
            throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
        }
    }

    public String getSecureContainerFilesystemPath(String id) {
        Slog.i(TAG, "getSecureContainerFilesystemPath, id=" + id);
        enforcePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            NativeDaemonEvent event = this.mConnector.execute("asec", "fspath", id);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 406) {
                Slog.i(TAG, String.format("Container '%s' not found", id));
                return null;
            }
            throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
        }
    }

    public void finishMediaUpdate() {
        Slog.i(TAG, "finishMediaUpdate");
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to call finishMediaUpdate()");
        }
        if (this.mUnmountSignal != null) {
            this.mUnmountSignal.countDown();
        } else {
            Slog.w(TAG, "Odd, nobody asked to unmount?");
        }
    }

    private boolean isUidOwnerOfPackageOrSystem(String packageName, int callerUid) {
        if (callerUid == 1000) {
            return true;
        }
        if (packageName == null) {
            return false;
        }
        int packageUid = this.mPms.getPackageUid(packageName, 268435456, UserHandle.getUserId(callerUid));
        Slog.d(TAG, "packageName = " + packageName + ", packageUid = " + packageUid + ", callerUid = " + callerUid);
        return callerUid == packageUid;
    }

    public String getMountedObbPath(String rawPath) {
        ObbState state;
        Slog.i(TAG, "getMountedObbPath, rawPath=" + rawPath);
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mObbMounts) {
            state = this.mObbPathToStateMap.get(rawPath);
        }
        if (state == null) {
            Slog.w(TAG, "Failed to find OBB mounted at " + rawPath);
            return null;
        }
        try {
            NativeDaemonEvent event = this.mConnector.execute("obb", "path", state.canonicalPath);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 406) {
                return null;
            }
            throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
        }
    }

    public boolean isObbMounted(String rawPath) {
        boolean zContainsKey;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            zContainsKey = this.mObbPathToStateMap.containsKey(rawPath);
        }
        return zContainsKey;
    }

    public void mountObb(String rawPath, String canonicalPath, String key, IObbActionListener token, int nonce) {
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        Preconditions.checkNotNull(canonicalPath, "canonicalPath cannot be null");
        Preconditions.checkNotNull(token, "token cannot be null");
        int callingUid = Binder.getCallingUid();
        ObbState obbState = new ObbState(rawPath, canonicalPath, callingUid, token, nonce);
        ObbAction action = new MountObbAction(obbState, key, callingUid);
        this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, action));
        Slog.i(TAG, "Send to OBB handler: " + action.toString());
    }

    public void unmountObb(String rawPath, boolean force, IObbActionListener token, int nonce) {
        ObbState existingState;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            existingState = this.mObbPathToStateMap.get(rawPath);
        }
        if (existingState != null) {
            int callingUid = Binder.getCallingUid();
            ObbState newState = new ObbState(rawPath, existingState.canonicalPath, callingUid, token, nonce);
            ObbAction action = new UnmountObbAction(newState, force);
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, action));
            Slog.i(TAG, "Send to OBB handler: " + action.toString());
            return;
        }
        Slog.w(TAG, "Unknown OBB mount at " + rawPath);
    }

    public int getEncryptionState() {
        Slog.i(TAG, "getEncryptionState");
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "cryptocomplete");
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            Slog.w(TAG, "Error in communicating with cryptfs in validating");
            return -1;
        } catch (NumberFormatException e2) {
            Slog.w(TAG, "Unable to parse result from cryptfs cryptocomplete");
            return -1;
        }
    }

    public int decryptStorage(String password) {
        if (LOG_ENABLE) {
            Slog.i(TAG, "decryptStorage, password=" + password);
        }
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        Slog.i(TAG, "decrypting storage...");
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "checkpw", new NativeDaemonConnector.SensitiveArg(password));
            int code = Integer.parseInt(event.getMessage());
            if (code == 0) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MountService.this.mCryptConnector.execute("cryptfs", "restart");
                        } catch (NativeDaemonConnectorException e) {
                            Slog.e(MountService.TAG, "problem executing in background", e);
                        }
                    }
                }, 1000L);
            }
            return code;
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int encryptStorage(int type, String password) {
        if (LOG_ENABLE) {
            Slog.i(TAG, "encryptStorage, type=" + CRYPTO_TYPES[type] + ", password=" + password);
        }
        if (TextUtils.isEmpty(password) && type != 1) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        Slog.i(TAG, "encrypting storage...");
        waitMtkLogStopped();
        try {
            if (type == 1) {
                this.mCryptConnector.execute("cryptfs", "enablecrypto", "inplace", CRYPTO_TYPES[type]);
            } else {
                this.mCryptConnector.execute("cryptfs", "enablecrypto", "inplace", CRYPTO_TYPES[type], new NativeDaemonConnector.SensitiveArg(password));
            }
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int changeEncryptionPassword(int type, String password) {
        if (LOG_ENABLE) {
            Slog.i(TAG, "changeEncryptionPassword, type=" + CRYPTO_TYPES[type] + ", password=" + password);
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        Slog.i(TAG, "changing encryption password...");
        if (this.mOldEncryptionType == -1) {
            this.mOldEncryptionType = getPasswordType();
        }
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "changepw", CRYPTO_TYPES[type], new NativeDaemonConnector.SensitiveArg(password));
            if (type != this.mOldEncryptionType) {
                Slog.i(TAG, "Encryption type changed from " + this.mOldEncryptionType + " to " + type);
                this.mOldEncryptionType = type;
                sendEncryptionTypeIntent();
            }
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int verifyEncryptionPassword(String password) throws RemoteException {
        if (LOG_ENABLE) {
            Slog.i(TAG, "verifyEncryptionPassword, password=" + password);
        }
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to access the crypt keeper");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        waitForReady();
        Slog.i(TAG, "validating encryption password...");
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "verifypw", new NativeDaemonConnector.SensitiveArg(password));
            Slog.i(TAG, "cryptfs verifypw => " + event.getMessage());
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int getPasswordType() {
        Slog.i(TAG, "getPasswordType");
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "getpwtype");
            for (int i = 0; i < CRYPTO_TYPES.length; i++) {
                Slog.i(TAG, "CRYPTO_TYPES[" + i + "]=" + CRYPTO_TYPES[i] + ", event.getMessage()=" + event.getMessage());
                if (CRYPTO_TYPES[i].equals(event.getMessage())) {
                    Slog.i(TAG, "return CRYPTO_TYPES=" + CRYPTO_TYPES[i]);
                    return i;
                }
            }
            throw new IllegalStateException("unexpected return from cryptfs");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setField(String field, String contents) throws RemoteException {
        Slog.i(TAG, "setField, field=" + field + ", contens=" + contents);
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "no permission to access the crypt keeper");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "setfield", field, contents);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String getField(String field) throws RemoteException {
        Slog.i(TAG, "getField, field=" + field);
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "no permission to access the crypt keeper");
        waitForReady();
        try {
            String[] contents = NativeDaemonEvent.filterMessageList(this.mCryptConnector.executeForList("cryptfs", "getfield", field), 113);
            String result = new String();
            for (String content : contents) {
                result = result + content;
            }
            Slog.i(TAG, "getField, return " + result);
            return result;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isConvertibleToFBE() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "isConvertibleToFBE");
            return Integer.parseInt(event.getMessage()) != 0;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String getPassword() throws RemoteException {
        Slog.i(TAG, "getPassword");
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE", "only keyguard can retrieve password");
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "no permission to access the crypt keeper");
        if (!isReady()) {
            Slog.i(TAG, "not ready, reutn null");
            return new String();
        }
        try {
            NativeDaemonEvent event = this.mCryptConnector.execute("cryptfs", "getpw");
            if ("-1".equals(event.getMessage())) {
                Slog.i(TAG, "no password, reutn null");
                return null;
            }
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        } catch (IllegalArgumentException e2) {
            Slog.e(TAG, "Invalid response to getPassword");
            return null;
        }
    }

    public void clearPassword() throws RemoteException {
        Slog.i(TAG, "clearPassword");
        this.mContext.enforceCallingOrSelfPermission("android.permission.STORAGE_INTERNAL", "only keyguard can clear password");
        if (!isReady()) {
            return;
        }
        try {
            this.mCryptConnector.execute("cryptfs", "clearpw");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void createUserKey(int userId, int serialNumber, boolean ephemeral) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mCryptConnector;
            Object[] objArr = new Object[4];
            objArr[0] = "create_user_key";
            objArr[1] = Integer.valueOf(userId);
            objArr[2] = Integer.valueOf(serialNumber);
            objArr[3] = Integer.valueOf(ephemeral ? 1 : 0);
            nativeDaemonConnector.execute("cryptfs", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void destroyUserKey(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "destroy_user_key", Integer.valueOf(userId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private NativeDaemonConnector.SensitiveArg encodeBytes(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return new NativeDaemonConnector.SensitiveArg("!");
        }
        return new NativeDaemonConnector.SensitiveArg(HexDump.toHexString(bytes));
    }

    public void changeUserKey(int userId, int serialNumber, byte[] token, byte[] oldSecret, byte[] newSecret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "change_user_key", Integer.valueOf(userId), Integer.valueOf(serialNumber), encodeBytes(token), encodeBytes(oldSecret), encodeBytes(newSecret));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addUserKeyAuth(int userId, int serialNumber, byte[] token, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "add_user_key_auth", Integer.valueOf(userId), Integer.valueOf(serialNumber), encodeBytes(token), encodeBytes(secret));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void fixateNewestUserKeyAuth(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "fixate_newest_user_key_auth", Integer.valueOf(userId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void unlockUserKey(int userId, int serialNumber, byte[] token, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (this.mLockPatternUtils.isSecure(userId) && ArrayUtils.isEmpty(token)) {
                throw new IllegalStateException("Token required to unlock secure user " + userId);
            }
            try {
                this.mCryptConnector.execute("cryptfs", "unlock_user_key", Integer.valueOf(userId), Integer.valueOf(serialNumber), encodeBytes(token), encodeBytes(secret));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        synchronized (this.mLock) {
            this.mLocalUnlockedUsers = ArrayUtils.appendInt(this.mLocalUnlockedUsers, userId);
        }
    }

    public void lockUserKey(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "lock_user_key", Integer.valueOf(userId));
            synchronized (this.mLock) {
                this.mLocalUnlockedUsers = ArrayUtils.removeInt(this.mLocalUnlockedUsers, userId);
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isUserKeyUnlocked(int userId) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mLocalUnlockedUsers, userId);
        }
        return zContains;
    }

    public void prepareUserStorage(String volumeUuid, int userId, int serialNumber, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "prepare_user_storage", escapeNull(volumeUuid), Integer.valueOf(userId), Integer.valueOf(serialNumber), Integer.valueOf(flags));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void destroyUserStorage(String volumeUuid, int userId, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        waitForReady();
        try {
            this.mCryptConnector.execute("cryptfs", "destroy_user_storage", escapeNull(volumeUuid), Integer.valueOf(userId), Integer.valueOf(flags));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public ParcelFileDescriptor mountAppFuse(final String name) throws RemoteException {
        try {
            final int uid = Binder.getCallingUid();
            final int pid = Binder.getCallingPid();
            NativeDaemonEvent event = this.mConnector.execute("appfuse", "mount", Integer.valueOf(uid), Integer.valueOf(pid), name);
            if (event.getFileDescriptors() == null) {
                throw new RemoteException("AppFuse FD from vold is null.");
            }
            return ParcelFileDescriptor.fromFd(event.getFileDescriptors()[0], this.mHandler, new ParcelFileDescriptor.OnCloseListener() {
                @Override
                public void onClose(IOException e) {
                    try {
                        MountService.this.mConnector.execute("appfuse", "unmount", Integer.valueOf(uid), Integer.valueOf(pid), name);
                    } catch (NativeDaemonConnectorException e2) {
                        Log.e(MountService.TAG, "Failed to unmount appfuse.");
                    }
                }
            });
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        } catch (IOException e2) {
            throw new RemoteException(e2.getMessage());
        }
    }

    public int mkdirs(String callingPkg, String appPath) {
        Slog.i(TAG, "mkdirs, callingPkg=" + callingPkg + ", appPath=" + appPath);
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        Environment.UserEnvironment userEnv = new Environment.UserEnvironment(userId);
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        appOps.checkPackage(Binder.getCallingUid(), callingPkg);
        try {
            File appFile = new File(appPath).getCanonicalFile();
            if (FileUtils.contains(userEnv.buildExternalStorageAppDataDirs(callingPkg), appFile) || FileUtils.contains(userEnv.buildExternalStorageAppObbDirs(callingPkg), appFile) || FileUtils.contains(userEnv.buildExternalStorageAppMediaDirs(callingPkg), appFile)) {
                String appPath2 = appFile.getAbsolutePath();
                if (!appPath2.endsWith("/")) {
                    appPath2 = appPath2 + "/";
                }
                try {
                    this.mConnector.execute(TAG_VOLUME, "mkdirs", appPath2);
                    return 0;
                } catch (NativeDaemonConnectorException e) {
                    return e.getCode();
                }
            }
            throw new SecurityException("Invalid mkdirs path: " + appFile);
        } catch (IOException e2) {
            Slog.e(TAG, "Failed to resolve " + appPath + ": " + e2);
            return -1;
        }
    }

    public StorageVolume[] getVolumeList(int uid, String packageName, int flags) {
        boolean match;
        int userId = UserHandle.getUserId(uid);
        boolean forWrite = (flags & 256) != 0;
        boolean realState = (flags & 512) != 0;
        boolean includeInvisible = (flags & 1024) != 0;
        long token = Binder.clearCallingIdentity();
        try {
            boolean userKeyUnlocked = isUserKeyUnlocked(userId);
            boolean storagePermission = this.mMountServiceInternal.hasExternalStorage(uid, packageName);
            Binder.restoreCallingIdentity(token);
            boolean foundPrimary = false;
            ArrayList<StorageVolume> res = new ArrayList<>();
            synchronized (this.mLock) {
                for (int i = 0; i < this.mVolumes.size(); i++) {
                    VolumeInfo vol = this.mVolumes.valueAt(i);
                    switch (vol.getType()) {
                        case 0:
                        case 2:
                            if (forWrite) {
                                match = vol.isVisibleForWrite(userId);
                            } else if (vol.isVisibleForRead(userId)) {
                                match = true;
                            } else {
                                match = includeInvisible && vol.getPath() != null;
                            }
                            if (match) {
                                boolean reportUnmounted = false;
                                if (vol.getType() == 2 && !userKeyUnlocked) {
                                    reportUnmounted = true;
                                } else if (!storagePermission && !realState) {
                                    reportUnmounted = true;
                                }
                                StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, reportUnmounted);
                                if (vol.isPrimary()) {
                                    res.add(0, userVol);
                                    foundPrimary = true;
                                } else {
                                    res.add(userVol);
                                }
                            }
                            break;
                    }
                }
            }
            if (!foundPrimary) {
                Log.w(TAG, "No primary storage defined yet; hacking together a stub");
                boolean primaryPhysical = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                File path = Environment.getLegacyExternalStorageDirectory();
                String description = this.mContext.getString(R.string.unknownName);
                boolean emulated = !primaryPhysical;
                UserHandle owner = new UserHandle(userId);
                res.add(0, new StorageVolume("stub_primary", 0, path, description, true, primaryPhysical, emulated, 0L, false, 0L, owner, null, "removed"));
            }
            return (StorageVolume[]) res.toArray(new StorageVolume[res.size()]);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public DiskInfo[] getDisks() {
        DiskInfo[] res;
        synchronized (this.mLock) {
            res = new DiskInfo[this.mDisks.size()];
            for (int i = 0; i < this.mDisks.size(); i++) {
                res[i] = this.mDisks.valueAt(i);
            }
        }
        return res;
    }

    public VolumeInfo[] getVolumes(int flags) {
        VolumeInfo[] res;
        synchronized (this.mLock) {
            res = new VolumeInfo[this.mVolumes.size()];
            for (int i = 0; i < this.mVolumes.size(); i++) {
                res[i] = this.mVolumes.valueAt(i);
            }
        }
        return res;
    }

    public VolumeRecord[] getVolumeRecords(int flags) {
        VolumeRecord[] res;
        synchronized (this.mLock) {
            res = new VolumeRecord[this.mRecords.size()];
            for (int i = 0; i < this.mRecords.size(); i++) {
                res[i] = this.mRecords.valueAt(i);
            }
        }
        return res;
    }

    private void addObbStateLocked(ObbState obbState) throws RemoteException {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates == null) {
            obbStates = new ArrayList<>();
            this.mObbMounts.put(binder, obbStates);
        } else {
            for (ObbState o : obbStates) {
                if (o.rawPath.equals(obbState.rawPath)) {
                    throw new IllegalStateException("Attempt to add ObbState twice. This indicates an error in the MountService logic.");
                }
            }
        }
        obbStates.add(obbState);
        try {
            obbState.link();
            this.mObbPathToStateMap.put(obbState.rawPath, obbState);
        } catch (RemoteException e) {
            obbStates.remove(obbState);
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
            throw e;
        }
    }

    private void removeObbStateLocked(ObbState obbState) {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates != null) {
            if (obbStates.remove(obbState)) {
                obbState.unlink();
            }
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
        }
        this.mObbPathToStateMap.remove(obbState.rawPath);
    }

    private class ObbActionHandler extends Handler {
        private final List<ObbAction> mActions;
        private boolean mBound;

        ObbActionHandler(Looper l) {
            super(l);
            this.mBound = false;
            this.mActions = new LinkedList();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ObbAction action = (ObbAction) msg.obj;
                    Slog.i(MountService.TAG, "OBB_RUN_ACTION: " + action.toString());
                    if (!this.mBound && !connectToService()) {
                        Slog.e(MountService.TAG, "Failed to bind to media container service");
                        action.handleError();
                        return;
                    } else {
                        this.mActions.add(action);
                        return;
                    }
                case 2:
                    Slog.i(MountService.TAG, "OBB_MCS_BOUND");
                    if (msg.obj != null) {
                        MountService.this.mContainerService = (IMediaContainerService) msg.obj;
                    }
                    if (MountService.this.mContainerService == null) {
                        Slog.e(MountService.TAG, "Cannot bind to media container service");
                        Iterator action$iterator = this.mActions.iterator();
                        while (action$iterator.hasNext()) {
                            ((ObbAction) action$iterator.next()).handleError();
                        }
                        this.mActions.clear();
                        return;
                    }
                    if (this.mActions.size() > 0) {
                        ObbAction action2 = this.mActions.get(0);
                        if (action2 == null) {
                            return;
                        }
                        action2.execute(this);
                        return;
                    }
                    Slog.w(MountService.TAG, "Empty queue");
                    return;
                case 3:
                    Slog.i(MountService.TAG, "OBB_MCS_UNBIND");
                    if (this.mActions.size() > 0) {
                        this.mActions.remove(0);
                    }
                    if (this.mActions.size() == 0) {
                        if (!this.mBound) {
                            return;
                        }
                        disconnectService();
                        return;
                    }
                    MountService.this.mObbActionHandler.sendEmptyMessage(2);
                    return;
                case 4:
                    Slog.i(MountService.TAG, "OBB_MCS_RECONNECT");
                    if (this.mActions.size() <= 0) {
                        return;
                    }
                    if (this.mBound) {
                        disconnectService();
                    }
                    if (connectToService()) {
                        return;
                    }
                    Slog.e(MountService.TAG, "Failed to bind to media container service");
                    Iterator action$iterator2 = this.mActions.iterator();
                    while (action$iterator2.hasNext()) {
                        ((ObbAction) action$iterator2.next()).handleError();
                    }
                    this.mActions.clear();
                    return;
                case 5:
                    String path = (String) msg.obj;
                    Slog.i(MountService.TAG, "Flushing all OBB state for path " + path);
                    synchronized (MountService.this.mObbMounts) {
                        List<ObbState> obbStatesToRemove = new LinkedList<>();
                        for (ObbState state : MountService.this.mObbPathToStateMap.values()) {
                            if (state.canonicalPath.startsWith(path)) {
                                obbStatesToRemove.add(state);
                            }
                        }
                        for (ObbState obbState : obbStatesToRemove) {
                            Slog.i(MountService.TAG, "Removing state for " + obbState.rawPath);
                            MountService.this.removeObbStateLocked(obbState);
                            try {
                                obbState.token.onObbResult(obbState.rawPath, obbState.nonce, 2);
                            } catch (RemoteException e) {
                                Slog.i(MountService.TAG, "Couldn't send unmount notification for  OBB: " + obbState.rawPath);
                            }
                            break;
                        }
                    }
                    return;
                default:
                    return;
            }
        }

        private boolean connectToService() {
            Slog.i(MountService.TAG, "Trying to bind to DefaultContainerService");
            Intent service = new Intent().setComponent(MountService.DEFAULT_CONTAINER_COMPONENT);
            if (MountService.this.mContext.bindServiceAsUser(service, MountService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                this.mBound = true;
                return true;
            }
            return false;
        }

        private void disconnectService() {
            MountService.this.mContainerService = null;
            this.mBound = false;
            MountService.this.mContext.unbindService(MountService.this.mDefContainerConn);
        }
    }

    abstract class ObbAction {
        private static final int MAX_RETRIES = 3;
        ObbState mObbState;
        private int mRetries;

        abstract void handleError();

        abstract void handleExecute() throws RemoteException, IOException;

        ObbAction(ObbState obbState) {
            this.mObbState = obbState;
        }

        public void execute(ObbActionHandler handler) {
            try {
                Slog.i(MountService.TAG, "Starting to execute action: " + toString());
                this.mRetries++;
                if (this.mRetries > 3) {
                    Slog.w(MountService.TAG, "Failed to invoke remote methods on default container service. Giving up");
                    MountService.this.mObbActionHandler.sendEmptyMessage(3);
                    handleError();
                } else {
                    handleExecute();
                    Slog.i(MountService.TAG, "Posting install MCS_UNBIND");
                    MountService.this.mObbActionHandler.sendEmptyMessage(3);
                }
            } catch (RemoteException e) {
                Slog.i(MountService.TAG, "Posting install MCS_RECONNECT");
                MountService.this.mObbActionHandler.sendEmptyMessage(4);
            } catch (Exception e2) {
                Slog.d(MountService.TAG, "Error handling OBB action", e2);
                handleError();
                MountService.this.mObbActionHandler.sendEmptyMessage(3);
            }
        }

        protected ObbInfo getObbInfo() throws IOException {
            ObbInfo obbInfo;
            try {
                obbInfo = MountService.this.mContainerService.getObbInfo(this.mObbState.canonicalPath);
            } catch (RemoteException e) {
                Slog.d(MountService.TAG, "Couldn't call DefaultContainerService to fetch OBB info for " + this.mObbState.canonicalPath);
                obbInfo = null;
            }
            if (obbInfo == null) {
                throw new IOException("Couldn't read OBB file: " + this.mObbState.canonicalPath);
            }
            return obbInfo;
        }

        protected void sendNewStatusOrIgnore(int status) {
            if (this.mObbState == null || this.mObbState.token == null) {
                return;
            }
            try {
                this.mObbState.token.onObbResult(this.mObbState.rawPath, this.mObbState.nonce, status);
            } catch (RemoteException e) {
                Slog.w(MountService.TAG, "MountServiceListener went away while calling onObbStateChanged");
            }
        }
    }

    class MountObbAction extends ObbAction {
        private final int mCallingUid;
        private final String mKey;

        MountObbAction(ObbState obbState, String key, int callingUid) {
            super(obbState);
            this.mKey = key;
            this.mCallingUid = callingUid;
        }

        @Override
        public void handleExecute() throws IOException, RemoteException {
            boolean isMounted;
            String hashedKey;
            MountService.this.waitForReady();
            MountService.this.warnOnNotMounted();
            ObbInfo obbInfo = getObbInfo();
            if (!MountService.this.isUidOwnerOfPackageOrSystem(obbInfo.packageName, this.mCallingUid)) {
                Slog.w(MountService.TAG, "Denied attempt to mount OBB " + obbInfo.filename + " which is owned by " + obbInfo.packageName);
                sendNewStatusOrIgnore(25);
                return;
            }
            synchronized (MountService.this.mObbMounts) {
                isMounted = MountService.this.mObbPathToStateMap.containsKey(this.mObbState.rawPath);
            }
            if (isMounted) {
                Slog.w(MountService.TAG, "Attempt to mount OBB which is already mounted: " + obbInfo.filename);
                sendNewStatusOrIgnore(24);
                return;
            }
            if (this.mKey == null) {
                hashedKey = "none";
            } else {
                try {
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec ks = new PBEKeySpec(this.mKey.toCharArray(), obbInfo.salt, 1024, 128);
                    SecretKey key = factory.generateSecret(ks);
                    BigInteger bi = new BigInteger(key.getEncoded());
                    hashedKey = bi.toString(16);
                } catch (NoSuchAlgorithmException e) {
                    Slog.e(MountService.TAG, "Could not load PBKDF2 algorithm", e);
                    sendNewStatusOrIgnore(20);
                    return;
                } catch (InvalidKeySpecException e2) {
                    Slog.e(MountService.TAG, "Invalid key spec when loading PBKDF2 algorithm", e2);
                    sendNewStatusOrIgnore(20);
                    return;
                }
            }
            int rc = 0;
            try {
                MountService.this.mConnector.execute("obb", "mount", this.mObbState.canonicalPath, new NativeDaemonConnector.SensitiveArg(hashedKey), Integer.valueOf(this.mObbState.ownerGid));
            } catch (NativeDaemonConnectorException e3) {
                int code = e3.getCode();
                if (code != 405) {
                    rc = -1;
                }
            }
            if (rc == 0) {
                Slog.d(MountService.TAG, "Successfully mounted OBB " + this.mObbState.canonicalPath);
                synchronized (MountService.this.mObbMounts) {
                    MountService.this.addObbStateLocked(this.mObbState);
                }
                sendNewStatusOrIgnore(1);
                return;
            }
            Slog.e(MountService.TAG, "Couldn't mount OBB file: " + rc);
            sendNewStatusOrIgnore(21);
        }

        @Override
        public void handleError() {
            sendNewStatusOrIgnore(20);
        }

        public String toString() {
            return "MountObbAction{" + this.mObbState + '}';
        }
    }

    class UnmountObbAction extends ObbAction {
        private final boolean mForceUnmount;

        UnmountObbAction(ObbState obbState, boolean force) {
            super(obbState);
            this.mForceUnmount = force;
        }

        @Override
        public void handleExecute() throws IOException {
            ObbState existingState;
            MountService.this.waitForReady();
            MountService.this.warnOnNotMounted();
            synchronized (MountService.this.mObbMounts) {
                existingState = (ObbState) MountService.this.mObbPathToStateMap.get(this.mObbState.rawPath);
            }
            if (existingState == null) {
                sendNewStatusOrIgnore(23);
                return;
            }
            if (existingState.ownerGid != this.mObbState.ownerGid) {
                Slog.w(MountService.TAG, "Permission denied attempting to unmount OBB " + existingState.rawPath + " (owned by GID " + existingState.ownerGid + ")");
                sendNewStatusOrIgnore(25);
                return;
            }
            int rc = 0;
            try {
                NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("obb", "unmount", this.mObbState.canonicalPath);
                if (this.mForceUnmount) {
                    cmd.appendArg("force");
                }
                MountService.this.mConnector.execute(cmd);
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                rc = code == 405 ? -7 : code == 406 ? 0 : -1;
            }
            if (rc != 0) {
                Slog.w(MountService.TAG, "Could not unmount OBB: " + existingState);
                sendNewStatusOrIgnore(22);
            } else {
                synchronized (MountService.this.mObbMounts) {
                    MountService.this.removeObbStateLocked(existingState);
                }
                sendNewStatusOrIgnore(2);
            }
        }

        @Override
        public void handleError() {
            sendNewStatusOrIgnore(20);
        }

        public String toString() {
            return "UnmountObbAction{" + this.mObbState + ",force=" + this.mForceUnmount + '}';
        }
    }

    private static class Callbacks extends Handler {
        private static final int MSG_DISK_DESTROYED = 6;
        private static final int MSG_DISK_SCANNED = 5;
        private static final int MSG_STORAGE_STATE_CHANGED = 1;
        private static final int MSG_UMS_CONNECTION_CHANGED = 7;
        private static final int MSG_VOLUME_FORGOTTEN = 4;
        private static final int MSG_VOLUME_RECORD_CHANGED = 3;
        private static final int MSG_VOLUME_STATE_CHANGED = 2;
        private final RemoteCallbackList<IMountServiceListener> mCallbacks;

        public Callbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
        }

        public void register(IMountServiceListener callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IMountServiceListener callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IMountServiceListener callback = (IMountServiceListener) this.mCallbacks.getBroadcastItem(i);
                try {
                    invokeCallback(callback, msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IMountServiceListener callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onStorageStateChanged((String) args.arg1, (String) args.arg2, (String) args.arg3);
                    break;
                case 2:
                    callback.onVolumeStateChanged((VolumeInfo) args.arg1, args.argi2, args.argi3);
                    break;
                case 3:
                    callback.onVolumeRecordChanged((VolumeRecord) args.arg1);
                    break;
                case 4:
                    callback.onVolumeForgotten((String) args.arg1);
                    break;
                case 5:
                    callback.onDiskScanned((DiskInfo) args.arg1, args.argi2);
                    break;
                case 6:
                    callback.onDiskDestroyed((DiskInfo) args.arg1);
                    break;
                case 7:
                    callback.onUsbMassStorageConnectionChanged(((Boolean) args.arg1).booleanValue());
                    break;
            }
        }

        private void notifyStorageStateChanged(String path, String oldState, String newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = path;
            args.arg2 = oldState;
            args.arg3 = newState;
            obtainMessage(1, args).sendToTarget();
        }

        private void notifyVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = vol.clone();
            args.argi2 = oldState;
            args.argi3 = newState;
            obtainMessage(2, args).sendToTarget();
        }

        private void notifyVolumeRecordChanged(VolumeRecord rec) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = rec.clone();
            obtainMessage(3, args).sendToTarget();
        }

        private void notifyVolumeForgotten(String fsUuid) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = fsUuid;
            obtainMessage(4, args).sendToTarget();
        }

        private void notifyDiskScanned(DiskInfo disk, int volumeCount) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            args.argi2 = volumeCount;
            obtainMessage(5, args).sendToTarget();
        }

        private void notifyDiskDestroyed(DiskInfo disk) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            obtainMessage(6, args).sendToTarget();
        }

        private void onUsbMassStorageConnectionChanged(boolean connected) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Boolean.valueOf(connected);
            obtainMessage(7, args).sendToTarget();
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(writer, "  ", 160);
        synchronized (this.mLock) {
            indentingPrintWriter.println("Disks:");
            indentingPrintWriter.increaseIndent();
            for (int i = 0; i < this.mDisks.size(); i++) {
                DiskInfo disk = this.mDisks.valueAt(i);
                disk.dump(indentingPrintWriter);
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Volumes:");
            indentingPrintWriter.increaseIndent();
            for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
                VolumeInfo vol = this.mVolumes.valueAt(i2);
                if (!"private".equals(vol.id)) {
                    vol.dump(indentingPrintWriter);
                }
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Records:");
            indentingPrintWriter.increaseIndent();
            for (int i3 = 0; i3 < this.mRecords.size(); i3++) {
                VolumeRecord note = this.mRecords.valueAt(i3);
                note.dump(indentingPrintWriter);
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Primary storage UUID: " + this.mPrimaryStorageUuid);
            indentingPrintWriter.println("Force adoptable: " + this.mForceAdoptable);
            indentingPrintWriter.println();
            indentingPrintWriter.println("Local unlocked users: " + Arrays.toString(this.mLocalUnlockedUsers));
            indentingPrintWriter.println("System unlocked users: " + Arrays.toString(this.mSystemUnlockedUsers));
        }
        synchronized (this.mObbMounts) {
            indentingPrintWriter.println();
            indentingPrintWriter.println("mObbMounts:");
            indentingPrintWriter.increaseIndent();
            for (Map.Entry<IBinder, List<ObbState>> e : this.mObbMounts.entrySet()) {
                indentingPrintWriter.println(e.getKey() + ":");
                indentingPrintWriter.increaseIndent();
                List<ObbState> obbStates = e.getValue();
                for (ObbState obbState : obbStates) {
                    indentingPrintWriter.println(obbState);
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("mObbPathToStateMap:");
            indentingPrintWriter.increaseIndent();
            for (Map.Entry<String, ObbState> e2 : this.mObbPathToStateMap.entrySet()) {
                indentingPrintWriter.print(e2.getKey());
                indentingPrintWriter.print(" -> ");
                indentingPrintWriter.println(e2.getValue());
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.println();
        indentingPrintWriter.println("mConnector:");
        indentingPrintWriter.increaseIndent();
        this.mConnector.dump(fd, indentingPrintWriter, args);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println("mCryptConnector:");
        indentingPrintWriter.increaseIndent();
        this.mCryptConnector.dump(fd, indentingPrintWriter, args);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.print("Last maintenance: ");
        indentingPrintWriter.println(TimeUtils.formatForLogging(this.mLastMaintenance));
    }

    @Override
    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
        if (this.mCryptConnector == null) {
            return;
        }
        this.mCryptConnector.monitor();
    }

    private final class MountServiceInternalImpl extends MountServiceInternal {
        private final CopyOnWriteArrayList<MountServiceInternal.ExternalStorageMountPolicy> mPolicies;

        MountServiceInternalImpl(MountService this$0, MountServiceInternalImpl mountServiceInternalImpl) {
            this();
        }

        private MountServiceInternalImpl() {
            this.mPolicies = new CopyOnWriteArrayList<>();
        }

        public void addExternalStoragePolicy(MountServiceInternal.ExternalStorageMountPolicy policy) {
            this.mPolicies.add(policy);
        }

        public void onExternalStoragePolicyChanged(int uid, String packageName) {
            int mountMode = getExternalStorageMountMode(uid, packageName);
            MountService.this.remountUidExternalStorage(uid, mountMode);
        }

        public int getExternalStorageMountMode(int uid, String packageName) {
            int mountMode = Integer.MAX_VALUE;
            for (MountServiceInternal.ExternalStorageMountPolicy policy : this.mPolicies) {
                int policyMode = policy.getMountMode(uid, packageName);
                if (policyMode == 0) {
                    return 0;
                }
                mountMode = Math.min(mountMode, policyMode);
            }
            if (mountMode == Integer.MAX_VALUE) {
                return 0;
            }
            return mountMode;
        }

        public boolean hasExternalStorage(int uid, String packageName) {
            if (uid == 1000) {
                return true;
            }
            for (MountServiceInternal.ExternalStorageMountPolicy policy : this.mPolicies) {
                boolean policyHasStorage = policy.hasExternalStorage(uid, packageName);
                if (!policyHasStorage) {
                    return false;
                }
            }
            return true;
        }
    }

    private void initMTKFeature() {
        registerDMAPPReceiver();
        registerPrivacyProtectionReceiver();
        registerUsbStateReceiver();
        registerBootIPOReceiver();
    }

    private void popFormatFailToast() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MountService.this.mContext, MountService.this.mContext.getString(com.mediatek.internal.R.string.format_error), 1).show();
            }
        });
    }

    private void waitMtkLogStopped() {
        Slog.i(TAG, "waitMtkLogStopped...");
        VolumeInfo emulatedVolume = null;
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                if (i >= this.mVolumes.size()) {
                    break;
                }
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.getType() != 2 || vol.getState() != 2) {
                    i++;
                } else {
                    emulatedVolume = vol;
                    break;
                }
            }
        }
        if (emulatedVolume == null) {
            Slog.i(TAG, "cannot find emulated volume, return");
            return;
        }
        StorageVolume userVol = emulatedVolume.buildStorageVolume(this.mContext, this.mCurrentUserId, false);
        Intent intent = new Intent("android.intent.action.MEDIA_EJECT", Uri.fromFile(userVol.getPathFile()));
        intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", userVol);
        intent.addFlags(67108864);
        Slog.i(TAG, "sendBroadcastAsUser, intent=" + intent + ", userVol=" + userVol);
        this.mContext.sendBroadcastAsUser(intent, userVol.getOwner());
        int tryCount = 0;
        while (true) {
            if (SystemProperties.get("debug.mtklog.netlog.Running", "0").equals("0") && SystemProperties.get("debug.mdlogger.Running", "0").equals("0") && SystemProperties.get("debug.MB.running", "0").equals("0") && SystemProperties.get("debug.gpsdbglog.enable", "0").equals("0")) {
                break;
            }
            if (tryCount == 60) {
                Slog.i(TAG, "try count = 60, break");
                break;
            }
            try {
                Slog.i(TAG, "debug.mtklog.netlog.Running=" + SystemProperties.get("debug.mtklog.netlog.Running"));
                Slog.i(TAG, "debug.mdlogger.Running=" + SystemProperties.get("debug.mdlogger.Running"));
                Slog.i(TAG, "debug.MB.running=" + SystemProperties.get("debug.MB.running"));
                Slog.i(TAG, "debug.gpsdbglog.enable=" + SystemProperties.get("debug.gpsdbglog.enable"));
                Thread.sleep(500L);
                tryCount++;
            } catch (Exception e) {
            }
        }
        if (tryCount != 60) {
            try {
                Thread.sleep(3000L);
            } catch (Exception e2) {
            }
        }
        Slog.i(TAG, "waitMtkLogStopped done");
    }

    public void setDefaultPath(String path) {
        if (path == null) {
            Slog.e(TAG, "setDefaultPath error! path=null");
            return;
        }
        try {
            SystemProperties.set("persist.sys.sd.defaultpath", path);
            Slog.e(TAG, "setDefaultPath new path=" + path);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "IllegalArgumentException when set default path:", e);
        }
    }

    private void updateDefaultPathForUserSwitch() {
        Slog.i(TAG, "updateDefaultPathForUserSwitch");
        String defaultPath = getDefaultPath();
        Slog.i(TAG, "current default path = " + defaultPath);
        if (defaultPath.contains("emulated")) {
            String defaultPath2 = "/storage/emulated/" + this.mCurrentUserId;
            Slog.i(TAG, "change default path to " + defaultPath2);
            setDefaultPath(defaultPath2);
            return;
        }
        updateDefaultPathIfNeed();
    }

    private void updateDefaultPathIfNeed() {
        Slog.i(TAG, "updateDefaultPathIfNeed");
        if (isBootingPhase) {
            Slog.i(TAG, "In booting phase, don't update default path");
            return;
        }
        if (isIPOBooting) {
            Slog.i(TAG, "In IPO booting phase, don't update default path");
            return;
        }
        if (isShuttingDown) {
            Slog.i(TAG, "In shutting down, don't update default path");
            return;
        }
        String defaultPath = getDefaultPath();
        Slog.i(TAG, "current default path = " + defaultPath);
        String newPath = "";
        boolean needChange = false;
        boolean isFindCurrentDefaultPathVolume = false;
        int userId = this.mCurrentUserId;
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                if (i >= this.mVolumes.size()) {
                    break;
                }
                VolumeInfo vol = this.mVolumes.valueAt(i);
                File pathFile = vol.getPathForUser(userId);
                if (pathFile != null && pathFile.getAbsolutePath().equals(defaultPath)) {
                    Slog.i(TAG, "find default path volume= " + vol);
                    isFindCurrentDefaultPathVolume = true;
                    if (vol.getState() != 2) {
                        Slog.i(TAG, "old default path is not mounted");
                        needChange = true;
                        break;
                    } else {
                        if (!vol.isVisibleForWrite(userId)) {
                            Slog.i(TAG, "old default path is not visible for write, userId=" + userId);
                            needChange = true;
                            break;
                        }
                        Slog.i(TAG, "old default path is visible for write, userId=" + userId);
                    }
                }
                i++;
            }
        }
        if (needChange || !isFindCurrentDefaultPathVolume) {
            Slog.i(TAG, "need change default path " + defaultPath);
            synchronized (this.mLock) {
                int i2 = 0;
                while (true) {
                    if (i2 >= this.mVolumes.size()) {
                        break;
                    }
                    VolumeInfo vol2 = this.mVolumes.valueAt(i2);
                    if (vol2.getState() != 2 || !vol2.isVisibleForWrite(userId)) {
                        i2++;
                    } else {
                        newPath = vol2.getPathForUser(userId).getAbsolutePath();
                        Slog.i(TAG, "updateDefaultPathIfNeed from " + defaultPath + " to " + newPath);
                        setDefaultPath(newPath);
                        if (defaultPath.contains("emulated") && newPath.contains("emulated")) {
                            Slog.i(TAG, "no need to pop toast");
                        } else {
                            popDefaultPathChangedToast();
                        }
                    }
                }
            }
            if (!newPath.equals("")) {
                return;
            }
            Slog.i(TAG, "not find mounted and visible volume, keep old default path:" + defaultPath);
            return;
        }
        Slog.i(TAG, "no need change default path, keep default path:" + defaultPath);
    }

    private String getDefaultPath() {
        new StorageManagerEx();
        return StorageManagerEx.getDefaultPath();
    }

    private boolean isShowDefaultPathDialog(VolumeInfo curVol) {
        Slog.i(TAG, "isShowDefaultPathDialog, curVol=" + curVol);
        if (curVol == null) {
            Slog.i(TAG, "curVolume is null, skip it.");
            return false;
        }
        if (isBootingPhase) {
            Slog.i(TAG, "in booting phase, not show defaultPathDialog, skip it.");
            return false;
        }
        if (isIPOBooting) {
            Slog.i(TAG, "in IPO booting phase, not show defaultPathDialog, skip it.");
            return false;
        }
        if (curVol.getState() != 2 && curVol.getState() != 1) {
            Slog.i(TAG, "this volume state is not mounted/checking, skip it.");
            return false;
        }
        if (!this.isDiskInsert) {
            Slog.i(TAG, "not disk insert, no need show dialog, return false");
            return false;
        }
        this.isDiskInsert = false;
        int mountCount = 0;
        if (!"file".equalsIgnoreCase(SystemProperties.get("ro.crypto.type", "")) && SystemProperties.get(PROP_VOLD_DECRYPT).equals("trigger_restart_min_framework")) {
            Slog.i(TAG, "PROP_VOLD_DECRYPT=trigger_restart_min_framework, return false");
            return false;
        }
        if (curVol.getType() == 2 && curVol.getDiskId() != null) {
            Slog.i(TAG, "isShowDefaultPathDialog, emulated volume, return false");
            return false;
        }
        int userId = this.mCurrentUserId;
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.getState() == 2 && vol.isVisibleForWrite(userId)) {
                    Slog.i(TAG, "find a visibe & mounted volume, volumeId=" + vol.getId());
                    mountCount++;
                }
            }
            Slog.i(TAG, "mount and visible volumes count=" + mountCount);
        }
        return mountCount > 1;
    }

    private void popDefaultPathChangedToast() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MountService.this.mContext, MountService.this.mContext.getString(134545529), 1).show();
            }
        });
    }

    private void showDefaultPathDialog(VolumeInfo vol) {
        Slog.i(TAG, "showDefaultPathDialog, vol=" + vol);
        int userId = this.mCurrentUserId;
        if (vol.isVisibleForWrite(userId)) {
            return;
        }
        Slog.i(TAG, "showDefaultPathDialog,but vol is not visible to userID=" + userId + ", volumeInfo=" + vol);
    }

    private void registerDMAPPReceiver() {
        if (!SystemProperties.get(PROP_DM_APP).equals("1")) {
            return;
        }
        IntentFilter DMFilter = new IntentFilter();
        DMFilter.addAction("com.mediatek.dm.LAWMO_UNLOCK");
        DMFilter.addAction("com.mediatek.dm.LAWMO_LOCK");
        DMFilter.addAction(OMADM_SD_FORMAT);
        this.mContext.registerReceiver(this.mDMReceiver, DMFilter, null, this.mHandler);
    }

    private void enableUSBFuction(boolean enable) {
        waitForReady();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[1];
            objArr[0] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute("USB", objArr);
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "enableUSBFunction failed, ", e);
        }
    }

    private void registerPrivacyProtectionReceiver() {
        IntentFilter privacyProtectionFilter = new IntentFilter();
        privacyProtectionFilter.addAction("com.mediatek.ppl.NOTIFY_LOCK");
        privacyProtectionFilter.addAction("com.mediatek.ppl.NOTIFY_UNLOCK");
        privacyProtectionFilter.addAction(PRIVACY_PROTECTION_WIPE);
        this.mContext.registerReceiver(this.mPrivacyProtectionReceiver, privacyProtectionFilter, null, this.mHandler);
    }

    private ArrayList<VolumeInfo> findVolumeListNeedFormat() {
        Slog.i(TAG, "findVolumeListNeedFormat");
        ArrayList<VolumeInfo> tempVolumes = Lists.newArrayList();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if ((!vol.isUSBOTG() && vol.isVisible() && vol.getType() == 0) || (vol.getType() == 1 && vol.getDiskId() != null)) {
                    tempVolumes.add(vol);
                    Slog.i(TAG, "i will try to format volume= " + vol);
                }
            }
        }
        return tempVolumes;
    }

    private void formatPhoneStorageAndExternalSDCard() {
        final ArrayList<VolumeInfo> tempVolumes = findVolumeListNeedFormat();
        new Thread() {
            @Override
            public void run() {
                synchronized (MountService.FORMAT_LOCK) {
                    int unused = MountService.this.mCurrentUserId;
                    for (int i = 0; i < tempVolumes.size(); i++) {
                        VolumeInfo vol = (VolumeInfo) tempVolumes.get(i);
                        if (vol.getType() == 1 && vol.getDiskId() != null) {
                            Slog.i(MountService.TAG, "use partition public to format, volume= " + vol);
                            MountService.this.partitionPublic(vol.getDiskId());
                            if (vol.getFsUuid() != null) {
                                MountService.this.forgetVolume(vol.getFsUuid());
                            }
                        } else {
                            if (vol.getState() == 1) {
                                Slog.i(MountService.TAG, "volume is checking, wait..");
                                int j = 0;
                                while (true) {
                                    if (j >= 30) {
                                        break;
                                    }
                                    try {
                                        sleep(1000L);
                                    } catch (InterruptedException ex) {
                                        Slog.e(MountService.TAG, "Exception when wait!", ex);
                                    }
                                    if (vol.getState() == 1) {
                                        j++;
                                    } else {
                                        Slog.i(MountService.TAG, "volume wait checking done!");
                                        break;
                                    }
                                }
                            }
                            if (vol.getState() == 2) {
                                Slog.i(MountService.TAG, "volume is mounted, unmount firstly, volume=" + vol);
                                MountService.this.unmount(vol.getId());
                                int j2 = 0;
                                while (true) {
                                    if (j2 >= 30) {
                                        break;
                                    }
                                    try {
                                        sleep(1000L);
                                    } catch (InterruptedException ex2) {
                                        Slog.e(MountService.TAG, "Exception when wait!", ex2);
                                    }
                                    if (vol.getState() != 0) {
                                        j2++;
                                    } else {
                                        Slog.i(MountService.TAG, "wait unmount done!");
                                        break;
                                    }
                                }
                            }
                            if (vol.getState() == 9) {
                                Slog.i(MountService.TAG, "volume is shared, unshared firstly volume=" + vol);
                                MountService.this.doShareUnshareVolume(vol.getId(), false);
                                int j3 = 0;
                                while (true) {
                                    if (j3 >= 30) {
                                        break;
                                    }
                                    try {
                                        sleep(1000L);
                                    } catch (InterruptedException ex3) {
                                        Slog.e(MountService.TAG, "Exception when wait!", ex3);
                                    }
                                    if (vol.getState() != 0) {
                                        j3++;
                                    } else {
                                        Slog.i(MountService.TAG, "wait unshare done!");
                                        break;
                                    }
                                }
                            }
                            MountService.this.format(vol.getId());
                            Slog.d(MountService.TAG, "format Succeed! volume=" + vol);
                        }
                    }
                    Intent intent = new Intent(MountService.PRIVACY_PROTECTION_WIPE_DONE);
                    MountService.this.mContext.sendBroadcast(intent);
                    Slog.d(MountService.TAG, "Privacy Protection wipe: send " + intent);
                }
            }
        }.start();
    }

    private void registerUsbStateReceiver() {
        this.mContext.registerReceiver(this.mUsbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"), null, this.mHandler);
    }

    private void notifyShareAvailabilityChange(boolean isConnected) {
        Slog.i(TAG, "notifyShareAvailabilityChange, isConnected=" + isConnected);
        this.mCallbacks.onUsbMassStorageConnectionChanged(isConnected);
        if (this.mSystemReady) {
            sendUmsIntent(isConnected);
        } else {
            this.mSendUmsConnectedOnBoot = isConnected;
        }
        if (isConnected) {
            return;
        }
        boolean needTurnOff = false;
        if (this.mIsTurnOnOffUsb) {
            needTurnOff = true;
        } else {
            synchronized (this.mLock) {
                int i = 0;
                while (true) {
                    if (i >= this.mVolumes.size()) {
                        break;
                    }
                    VolumeInfo vol = this.mVolumes.valueAt(i);
                    if (vol.getState() != 9) {
                        i++;
                    } else {
                        needTurnOff = true;
                        break;
                    }
                }
            }
        }
        if (!needTurnOff) {
            return;
        }
        new Thread("MountService#turnOffUMS") {
            @Override
            public void run() {
                synchronized (MountService.TURNONUSB_SYNC_LOCK) {
                    MountService.this.setUsbMassStorageEnabled(false);
                }
            }
        }.start();
    }

    private void doShareUnshareVolume(String volId, boolean enable) {
        Slog.i(TAG, "doShareUnshareVolume, volId=" + volId + ", enable=" + enable);
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        Slog.i(TAG, "doShareUnshareVolume, find volumeInfo=" + vol);
        if (vol.getType() == 2) {
            Slog.i(TAG, "emulated storage no need to share/unshare");
            return;
        }
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = enable ? "share" : "unshare";
            objArr[1] = volId;
            objArr[2] = "ums";
            nativeDaemonConnector.execute(TAG_VOLUME, objArr);
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to share/unshare", e);
        }
    }

    private boolean getUmsEnabling() {
        return this.mUmsEnabling;
    }

    private void setUmsEnabling(boolean enable) {
        this.mUmsEnabling = enable;
    }

    private void sendUmsIntent(boolean c) {
        this.mContext.sendBroadcastAsUser(new Intent(c ? "android.intent.action.UMS_CONNECTED" : "android.intent.action.UMS_DISCONNECTED"), UserHandle.ALL);
    }

    private boolean isVolumeSharedEnable(VolumeInfo vol) {
        int userId = this.mCurrentUserId;
        if (!vol.isAllowUsbMassStorage(userId)) {
            Slog.i(TAG, "not able to shared Volume=" + vol);
            return false;
        }
        boolean result = doGetVolumeShared(vol.getId());
        Slog.i(TAG, "isVolumeSharedEnable return " + result);
        return result;
    }

    private boolean doGetVolumeShared(String volId) {
        Slog.i(TAG, "doGetVolumeShared volId=" + volId);
        try {
            NativeDaemonEvent event = this.mConnector.execute(TAG_VOLUME, "shared", volId, "ums");
            if (event.getCode() == 212) {
                return event.getMessage().endsWith("enabled");
            }
            return false;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to read response to volume shared " + volId + " ums");
            return false;
        }
    }

    private void validateUserRestriction(String restriction) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        if (um == null || !um.hasUserRestriction(restriction, Binder.getCallingUserHandle())) {
        } else {
            throw new SecurityException("User has restriction " + restriction);
        }
    }

    private void registerBootIPOReceiver() {
        IntentFilter bootIPOFilter = new IntentFilter();
        bootIPOFilter.addAction("android.intent.action.ACTION_BOOT_IPO");
        this.mContext.registerReceiver(this.mBootIPOReceiver, bootIPOFilter, null, this.mHandler);
    }

    private void waitAllVolumeMounted() {
        try {
            Slog.d(TAG, "waitAllVolumeMounted when ipo startup");
            int retryCount = 0;
            while (retryCount < 10) {
                boolean isNeedWait = false;
                VolumeInfo[] volumes = getVolumes(0);
                int i = 0;
                while (true) {
                    if (i >= volumes.length) {
                        break;
                    }
                    VolumeInfo vol = volumes[i];
                    if ((vol.getType() != 0 && vol.getType() != 2) || !vol.isVisibleForWrite(this.mCurrentUserId) || vol.getState() == 2) {
                        i++;
                    } else {
                        Slog.i(TAG, "volume is not mounted, wait...");
                        isNeedWait = true;
                        retryCount++;
                        Thread.sleep(1000L);
                        break;
                    }
                }
                if (!isNeedWait) {
                    Slog.i(TAG, "all visible volume is mounted");
                    return;
                }
            }
        } catch (Exception e) {
        }
    }

    private void waitMTKNetlogStopped() {
        Slog.i(TAG, "waitMTKNetlogStopped...");
        int tryCount = 0;
        while (true) {
            if (SystemProperties.get("debug.mtklog.netlog.Running", "0").equals("0")) {
                break;
            }
            if (tryCount == 60) {
                Slog.i(TAG, "try count = 60, break");
                break;
            } else {
                try {
                    Slog.i(TAG, "debug.mtklog.netlog.Running=" + SystemProperties.get("debug.mtklog.netlog.Running"));
                    Thread.sleep(500L);
                    tryCount++;
                } catch (Exception e) {
                }
            }
        }
        Slog.i(TAG, "waitMTKNetlogStopped done");
    }

    private void sendEncryptionTypeIntent() {
        this.mContext.sendBroadcastAsUser(new Intent(ACTION_ENCRYPTION_TYPE_CHANGED), UserHandle.ALL);
    }

    public boolean isSetPrimaryStorageUuidFinished() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mMoveCallback == null;
        }
        return z;
    }
}
