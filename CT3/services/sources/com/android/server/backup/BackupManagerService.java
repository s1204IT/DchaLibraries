package com.android.server.backup;

import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IBackupAgent;
import android.app.PackageInstallObserver;
import android.app.PendingIntent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupProgress;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.RestoreDescription;
import android.app.backup.RestoreSet;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StringBuilderPrinter;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.backup.IObbBackupService;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.pm.PackageManagerService;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import libcore.io.IoUtils;

public class BackupManagerService {
    static final String BACKUP_ENABLE_FILE = "backup_enabled";
    static final String BACKUP_FILE_HEADER_MAGIC = "ANDROID BACKUP\n";
    static final int BACKUP_FILE_VERSION = 4;
    static final String BACKUP_MANIFEST_FILENAME = "_manifest";
    static final int BACKUP_MANIFEST_VERSION = 1;
    static final String BACKUP_METADATA_FILENAME = "_meta";
    static final int BACKUP_METADATA_VERSION = 1;
    static final int BACKUP_PW_FILE_VERSION = 2;
    static final int BACKUP_WIDGET_METADATA_TOKEN = 33549569;
    static final int BUSY_BACKOFF_FUZZ = 7200000;
    static final long BUSY_BACKOFF_MIN_MILLIS = 3600000;
    static final boolean COMPRESS_FULL_BACKUPS = true;
    static final int CURRENT_ANCESTRAL_RECORD_VERSION = 1;
    static final boolean DEBUG = true;
    static final boolean DEBUG_BACKUP_TRACE = true;
    static final boolean DEBUG_SCHEDULING = true;
    static final String ENCRYPTION_ALGORITHM_NAME = "AES-256";
    static final String INIT_SENTINEL_FILE_NAME = "_need_init_";
    static final String KEY_WIDGET_STATE = "￭￭widget";
    static final long MIN_FULL_BACKUP_INTERVAL = 86400000;
    static final boolean MORE_DEBUG = false;
    static final int MSG_BACKUP_RESTORE_STEP = 20;
    private static final int MSG_FULL_CONFIRMATION_TIMEOUT = 9;
    static final int MSG_OP_COMPLETE = 21;
    private static final int MSG_REQUEST_BACKUP = 15;
    private static final int MSG_RESTORE_TIMEOUT = 8;
    private static final int MSG_RETRY_CLEAR = 12;
    private static final int MSG_RETRY_INIT = 11;
    private static final int MSG_RUN_ADB_BACKUP = 2;
    private static final int MSG_RUN_ADB_RESTORE = 10;
    private static final int MSG_RUN_BACKUP = 1;
    private static final int MSG_RUN_CLEAR = 4;
    private static final int MSG_RUN_FULL_TRANSPORT_BACKUP = 14;
    private static final int MSG_RUN_GET_RESTORE_SETS = 6;
    private static final int MSG_RUN_INITIALIZE = 5;
    private static final int MSG_RUN_RESTORE = 3;
    private static final int MSG_TIMEOUT = 7;
    private static final int MSG_WIDGET_BROADCAST = 13;
    static final int OP_ACKNOWLEDGED = 1;
    static final int OP_PENDING = 0;
    static final int OP_TIMEOUT = -1;
    static final String PACKAGE_MANAGER_SENTINEL = "@pm@";
    static final int PBKDF2_HASH_ROUNDS = 10000;
    static final int PBKDF2_KEY_SIZE = 256;
    static final int PBKDF2_SALT_SIZE = 512;
    static final String PBKDF_CURRENT = "PBKDF2WithHmacSHA1";
    static final String PBKDF_FALLBACK = "PBKDF2WithHmacSHA1And8bit";
    private static final String RUN_BACKUP_ACTION = "android.app.backup.intent.RUN";
    private static final String RUN_INITIALIZE_ACTION = "android.app.backup.intent.INIT";
    static final int SCHEDULE_FILE_VERSION = 1;
    static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    static final String SETTINGS_PACKAGE = "com.android.providers.settings";
    static final String SHARED_BACKUP_AGENT_PACKAGE = "com.android.sharedstoragebackup";
    private static final String TAG = "BackupManagerService";
    static final long TIMEOUT_BACKUP_INTERVAL = 30000;
    static final long TIMEOUT_FULL_BACKUP_INTERVAL = 300000;
    static final long TIMEOUT_FULL_CONFIRMATION = 60000;
    static final long TIMEOUT_INTERVAL = 10000;
    static final long TIMEOUT_RESTORE_FINISHED_INTERVAL = 30000;
    static final long TIMEOUT_RESTORE_INTERVAL = 60000;
    static final long TIMEOUT_SHARED_BACKUP_INTERVAL = 1800000;
    private static final long TRANSPORT_RETRY_INTERVAL = 3600000;
    static Trampoline sInstance;
    ActiveRestoreSession mActiveRestoreSession;
    private AlarmManager mAlarmManager;
    boolean mAutoRestore;
    BackupHandler mBackupHandler;
    IBackupManager mBackupManagerBinder;
    volatile boolean mBackupRunning;
    File mBaseStateDir;
    volatile boolean mClearingData;
    IBackupAgent mConnectedAgent;
    volatile boolean mConnecting;
    Context mContext;
    String mCurrentTransport;
    File mDataDir;
    boolean mEnabled;
    private File mEverStored;

    @GuardedBy("mQueueLock")
    ArrayList<FullBackupEntry> mFullBackupQueue;
    File mFullBackupScheduleFile;
    File mJournal;
    File mJournalDir;
    volatile long mLastBackupPass;
    private PackageManager mPackageManager;
    private String mPasswordHash;
    private File mPasswordHashFile;
    private byte[] mPasswordSalt;
    private int mPasswordVersion;
    private File mPasswordVersionFile;
    private PowerManager mPowerManager;
    boolean mProvisioned;
    ContentObserver mProvisionedObserver;
    PendingIntent mRunBackupIntent;
    BroadcastReceiver mRunBackupReceiver;
    PendingIntent mRunInitIntent;
    BroadcastReceiver mRunInitReceiver;

    @GuardedBy("mQueueLock")
    PerformFullTransportBackupTask mRunningFullBackupTask;
    File mTokenFile;
    final ArraySet<ComponentName> mTransportWhitelist;
    PowerManager.WakeLock mWakelock;
    final SparseArray<HashSet<String>> mBackupParticipants = new SparseArray<>();
    HashMap<String, BackupRequest> mPendingBackups = new HashMap<>();
    final Object mQueueLock = new Object();
    final Object mAgentConnectLock = new Object();
    final List<String> mBackupTrace = new ArrayList();
    final Object mClearDataLock = new Object();
    final Intent mTransportServiceIntent = new Intent(SERVICE_ACTION_TRANSPORT_HOST);
    final ArrayMap<String, String> mTransportNames = new ArrayMap<>();
    final ArrayMap<String, IBackupTransport> mTransports = new ArrayMap<>();
    final ArrayMap<String, TransportConnection> mTransportConnections = new ArrayMap<>();
    final SparseArray<Operation> mCurrentOperations = new SparseArray<>();
    final Object mCurrentOpLock = new Object();
    final Random mTokenGenerator = new Random();
    final SparseArray<FullParams> mFullConfirmations = new SparseArray<>();
    private final SecureRandom mRng = new SecureRandom();
    HashSet<String> mEverStoredApps = new HashSet<>();
    Set<String> mAncestralPackages = null;
    long mAncestralToken = 0;
    long mCurrentToken = 0;
    HashSet<String> mPendingInits = new HashSet<>();
    Runnable mFullBackupScheduleWriter = new Runnable() {
        @Override
        public void run() {
            synchronized (BackupManagerService.this.mQueueLock) {
                try {
                    ByteArrayOutputStream bufStream = new ByteArrayOutputStream(4096);
                    DataOutputStream bufOut = new DataOutputStream(bufStream);
                    bufOut.writeInt(1);
                    int N = BackupManagerService.this.mFullBackupQueue.size();
                    bufOut.writeInt(N);
                    for (int i = 0; i < N; i++) {
                        FullBackupEntry entry = BackupManagerService.this.mFullBackupQueue.get(i);
                        bufOut.writeUTF(entry.packageName);
                        bufOut.writeLong(entry.lastBackup);
                    }
                    bufOut.flush();
                    AtomicFile af = new AtomicFile(BackupManagerService.this.mFullBackupScheduleFile);
                    FileOutputStream out = af.startWrite();
                    out.write(bufStream.toByteArray());
                    af.finishWrite(out);
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "Unable to write backup schedule!", e);
                }
            }
        }
    };
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean replacing = false;
            boolean added = false;
            Bundle extras = intent.getExtras();
            String[] pkgList = null;
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                String pkgName = uri.getSchemeSpecificPart();
                if (pkgName != null) {
                    pkgList = new String[]{pkgName};
                }
                boolean changed = "android.intent.action.PACKAGE_CHANGED".equals(action);
                if (changed) {
                    try {
                        String[] components = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                        boolean tryBind = true;
                        synchronized (BackupManagerService.this.mTransports) {
                            TransportConnection conn = BackupManagerService.this.mTransportConnections.get(pkgName);
                            if (conn != null) {
                                ServiceInfo svc = conn.mTransport;
                                ComponentName svcName = new ComponentName(svc.packageName, svc.name);
                                if (svc.packageName.equals(pkgName)) {
                                    String className = svcName.getClassName();
                                    boolean isTransport = false;
                                    int i = 0;
                                    while (true) {
                                        if (i >= components.length) {
                                            break;
                                        } else if (className.equals(components[i])) {
                                            break;
                                        } else {
                                            i++;
                                        }
                                    }
                                    if (!isTransport) {
                                        tryBind = false;
                                    }
                                }
                            }
                        }
                        if (tryBind) {
                            BackupManagerService.this.checkForTransportAndBind(BackupManagerService.this.mPackageManager.getPackageInfo(pkgName, 0));
                            return;
                        }
                        return;
                    } catch (PackageManager.NameNotFoundException e) {
                        return;
                    }
                }
                added = "android.intent.action.PACKAGE_ADDED".equals(action);
                replacing = extras.getBoolean("android.intent.extra.REPLACING", false);
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                added = true;
                pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                added = false;
                pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            }
            if (pkgList == null || pkgList.length == 0) {
                return;
            }
            int uid = extras.getInt("android.intent.extra.UID");
            if (added) {
                synchronized (BackupManagerService.this.mBackupParticipants) {
                    if (replacing) {
                        BackupManagerService.this.removePackageParticipantsLocked(pkgList, uid);
                        BackupManagerService.this.addPackageParticipantsLocked(pkgList);
                    } else {
                        BackupManagerService.this.addPackageParticipantsLocked(pkgList);
                    }
                }
                long now = System.currentTimeMillis();
                int i2 = 0;
                int length = pkgList.length;
                while (true) {
                    int i3 = i2;
                    if (i3 < length) {
                        String packageName = pkgList[i3];
                        try {
                            PackageInfo app = BackupManagerService.this.mPackageManager.getPackageInfo(packageName, 0);
                            if (BackupManagerService.appGetsFullBackup(app) && BackupManagerService.appIsEligibleForBackup(app.applicationInfo)) {
                                BackupManagerService.this.enqueueFullBackup(packageName, now);
                                BackupManagerService.this.scheduleNextFullBackupJob(0L);
                            } else {
                                synchronized (BackupManagerService.this.mQueueLock) {
                                    BackupManagerService.this.dequeueFullBackupLocked(packageName);
                                }
                                BackupManagerService.this.writeFullBackupScheduleAsync();
                            }
                            synchronized (BackupManagerService.this.mTransports) {
                                TransportConnection conn2 = BackupManagerService.this.mTransportConnections.get(packageName);
                                if (conn2 != null) {
                                    BackupManagerService.this.bindTransport(conn2.mTransport);
                                } else {
                                    BackupManagerService.this.checkForTransportAndBind(app);
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e2) {
                            Slog.w(BackupManagerService.TAG, "Can't resolve new app " + packageName);
                        }
                        i2 = i3 + 1;
                    } else {
                        BackupManagerService.this.dataChangedImpl(BackupManagerService.PACKAGE_MANAGER_SENTINEL);
                        return;
                    }
                }
            } else {
                if (replacing) {
                    return;
                }
                synchronized (BackupManagerService.this.mBackupParticipants) {
                    BackupManagerService.this.removePackageParticipantsLocked(pkgList, uid);
                }
            }
        }
    };
    IPackageManager mPackageManagerBinder = AppGlobals.getPackageManager();
    private IActivityManager mActivityManager = ActivityManagerNative.getDefault();
    private IMountService mMountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
    HandlerThread mHandlerThread = new HandlerThread("backup", 10);

    interface BackupRestoreTask {
        void execute();

        void handleTimeout();

        void operationComplete(long j);
    }

    interface FullBackupPreflight {
        long getExpectedSizeOrErrorCode();

        int preflightFullBackup(PackageInfo packageInfo, IBackupAgent iBackupAgent);
    }

    class BackupRequest {
        public String packageName;

        BackupRequest(String pkgName) {
            this.packageName = pkgName;
        }

        public String toString() {
            return "BackupRequest{pkg=" + this.packageName + "}";
        }
    }

    static Trampoline getInstance() {
        return sInstance;
    }

    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
            BackupManagerService.sInstance = new Trampoline(context);
        }

        @Override
        public void onStart() {
            publishBinderService("backup", BackupManagerService.sInstance);
        }

        @Override
        public void onUnlockUser(int userId) throws Throwable {
            if (userId != 0) {
                return;
            }
            BackupManagerService.sInstance.initialize(userId);
            if (!BackupManagerService.backupSettingMigrated(userId)) {
                Slog.i(BackupManagerService.TAG, "Backup enable apparently not migrated");
                ContentResolver r = BackupManagerService.sInstance.mContext.getContentResolver();
                int enableState = Settings.Secure.getIntForUser(r, BackupManagerService.BACKUP_ENABLE_FILE, -1, userId);
                if (enableState >= 0) {
                    Slog.i(BackupManagerService.TAG, "Migrating enable state " + (enableState != 0));
                    BackupManagerService.writeBackupEnableState(enableState != 0, userId);
                    Settings.Secure.putStringForUser(r, BackupManagerService.BACKUP_ENABLE_FILE, null, userId);
                } else {
                    Slog.i(BackupManagerService.TAG, "Backup not yet configured; retaining null enable state");
                }
            }
            try {
                BackupManagerService.sInstance.setBackupEnabled(BackupManagerService.readBackupEnableState(userId));
            } catch (RemoteException e) {
            }
        }
    }

    class ProvisionedObserver extends ContentObserver {
        public ProvisionedObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean wasProvisioned = BackupManagerService.this.mProvisioned;
            boolean isProvisioned = BackupManagerService.this.deviceIsProvisioned();
            BackupManagerService backupManagerService = BackupManagerService.this;
            if (wasProvisioned) {
                isProvisioned = true;
            }
            backupManagerService.mProvisioned = isProvisioned;
            synchronized (BackupManagerService.this.mQueueLock) {
                if (BackupManagerService.this.mProvisioned && !wasProvisioned && BackupManagerService.this.mEnabled) {
                    KeyValueBackupJob.schedule(BackupManagerService.this.mContext);
                    BackupManagerService.this.scheduleNextFullBackupJob(0L);
                }
            }
        }
    }

    class RestoreGetSetsParams {
        public IRestoreObserver observer;
        public ActiveRestoreSession session;
        public IBackupTransport transport;

        RestoreGetSetsParams(IBackupTransport _transport, ActiveRestoreSession _session, IRestoreObserver _observer) {
            this.transport = _transport;
            this.session = _session;
            this.observer = _observer;
        }
    }

    class RestoreParams {
        public String dirName;
        public String[] filterSet;
        public boolean isSystemRestore;
        public IRestoreObserver observer;
        public PackageInfo pkgInfo;
        public int pmToken;
        public long token;
        public IBackupTransport transport;

        RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, long _token, PackageInfo _pkg) {
            this.transport = _transport;
            this.dirName = _dirName;
            this.observer = _obs;
            this.token = _token;
            this.pkgInfo = _pkg;
            this.pmToken = 0;
            this.isSystemRestore = false;
            this.filterSet = null;
        }

        RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, long _token, String _pkgName, int _pmToken) {
            this.transport = _transport;
            this.dirName = _dirName;
            this.observer = _obs;
            this.token = _token;
            this.pkgInfo = null;
            this.pmToken = _pmToken;
            this.isSystemRestore = false;
            this.filterSet = new String[]{_pkgName};
        }

        RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, long _token) {
            this.transport = _transport;
            this.dirName = _dirName;
            this.observer = _obs;
            this.token = _token;
            this.pkgInfo = null;
            this.pmToken = 0;
            this.isSystemRestore = true;
            this.filterSet = null;
        }

        RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, long _token, String[] _filterSet, boolean _isSystemRestore) {
            this.transport = _transport;
            this.dirName = _dirName;
            this.observer = _obs;
            this.token = _token;
            this.pkgInfo = null;
            this.pmToken = 0;
            this.isSystemRestore = _isSystemRestore;
            this.filterSet = _filterSet;
        }
    }

    class ClearParams {
        public PackageInfo packageInfo;
        public IBackupTransport transport;

        ClearParams(IBackupTransport _transport, PackageInfo _info) {
            this.transport = _transport;
            this.packageInfo = _info;
        }
    }

    class ClearRetryParams {
        public String packageName;
        public String transportName;

        ClearRetryParams(String transport, String pkg) {
            this.transportName = transport;
            this.packageName = pkg;
        }
    }

    class FullParams {
        public String curPassword;
        public String encryptPassword;
        public ParcelFileDescriptor fd;
        public final AtomicBoolean latch = new AtomicBoolean(false);
        public IFullBackupRestoreObserver observer;

        FullParams() {
        }
    }

    class FullBackupParams extends FullParams {
        public boolean allApps;
        public boolean doCompress;
        public boolean doWidgets;
        public boolean includeApks;
        public boolean includeObbs;
        public boolean includeShared;
        public boolean includeSystem;
        public String[] packages;

        FullBackupParams(ParcelFileDescriptor output, boolean saveApks, boolean saveObbs, boolean saveShared, boolean alsoWidgets, boolean doAllApps, boolean doSystem, boolean compress, String[] pkgList) {
            super();
            this.fd = output;
            this.includeApks = saveApks;
            this.includeObbs = saveObbs;
            this.includeShared = saveShared;
            this.doWidgets = alsoWidgets;
            this.allApps = doAllApps;
            this.includeSystem = doSystem;
            this.doCompress = compress;
            this.packages = pkgList;
        }
    }

    class FullRestoreParams extends FullParams {
        FullRestoreParams(ParcelFileDescriptor input) {
            super();
            this.fd = input;
        }
    }

    class BackupParams {
        public String dirName;
        public ArrayList<String> fullPackages;
        public ArrayList<String> kvPackages;
        public IBackupObserver observer;
        public IBackupTransport transport;
        public boolean userInitiated;

        BackupParams(IBackupTransport transport, String dirName, ArrayList<String> kvPackages, ArrayList<String> fullPackages, IBackupObserver observer, boolean userInitiated) {
            this.transport = transport;
            this.dirName = dirName;
            this.kvPackages = kvPackages;
            this.fullPackages = fullPackages;
            this.observer = observer;
            this.userInitiated = userInitiated;
        }
    }

    class Operation {
        public BackupRestoreTask callback;
        public int state;

        Operation(int initialState, BackupRestoreTask callbackObj) {
            this.state = initialState;
            this.callback = callbackObj;
        }
    }

    class FullBackupEntry implements Comparable<FullBackupEntry> {
        long lastBackup;
        String packageName;

        FullBackupEntry(String pkg, long when) {
            this.packageName = pkg;
            this.lastBackup = when;
        }

        @Override
        public int compareTo(FullBackupEntry other) {
            if (this.lastBackup < other.lastBackup) {
                return -1;
            }
            return this.lastBackup > other.lastBackup ? 1 : 0;
        }
    }

    int generateToken() {
        int token;
        do {
            synchronized (this.mTokenGenerator) {
                token = this.mTokenGenerator.nextInt();
            }
        } while (token < 0);
        return token;
    }

    public static boolean appIsEligibleForBackup(ApplicationInfo app) {
        if ((app.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0) {
            return false;
        }
        return (app.uid >= 10000 || app.backupAgentName != null) && !app.packageName.equals(SHARED_BACKUP_AGENT_PACKAGE);
    }

    private static boolean appIsStopped(ApplicationInfo app) {
        return (app.flags & 2097152) != 0;
    }

    private static boolean appGetsFullBackup(PackageInfo pkg) {
        return pkg.applicationInfo.backupAgentName == null || (pkg.applicationInfo.flags & 67108864) != 0;
    }

    private static boolean appIsKeyValueOnly(PackageInfo pkg) {
        return (SETTINGS_PACKAGE.equals(pkg.packageName) || appGetsFullBackup(pkg)) ? false : true;
    }

    private class BackupHandler extends Handler {
        public BackupHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            HashSet<String> queue;
            switch (msg.what) {
                case 1:
                    BackupManagerService.this.mLastBackupPass = System.currentTimeMillis();
                    IBackupTransport transport = BackupManagerService.this.getTransport(BackupManagerService.this.mCurrentTransport);
                    if (transport == null) {
                        Slog.v(BackupManagerService.TAG, "Backup requested but no transport available");
                        synchronized (BackupManagerService.this.mQueueLock) {
                            BackupManagerService.this.mBackupRunning = false;
                        }
                        BackupManagerService.this.mWakelock.release();
                        return;
                    }
                    ArrayList<BackupRequest> queue2 = new ArrayList<>();
                    File oldJournal = BackupManagerService.this.mJournal;
                    synchronized (BackupManagerService.this.mQueueLock) {
                        if (BackupManagerService.this.mPendingBackups.size() > 0) {
                            for (BackupRequest b : BackupManagerService.this.mPendingBackups.values()) {
                                queue2.add(b);
                            }
                            Slog.v(BackupManagerService.TAG, "clearing pending backups");
                            BackupManagerService.this.mPendingBackups.clear();
                            BackupManagerService.this.mJournal = null;
                        }
                        break;
                    }
                    boolean staged = true;
                    if (queue2.size() > 0) {
                        try {
                            String dirName = transport.transportDirName();
                            PerformBackupTask pbt = BackupManagerService.this.new PerformBackupTask(transport, dirName, queue2, oldJournal, null, null, false);
                            Message pbtMessage = obtainMessage(20, pbt);
                            sendMessage(pbtMessage);
                        } catch (RemoteException e) {
                            Slog.e(BackupManagerService.TAG, "Transport became unavailable attempting backup");
                            staged = false;
                        }
                        break;
                    } else {
                        Slog.v(BackupManagerService.TAG, "Backup requested but nothing pending");
                        staged = false;
                    }
                    if (staged) {
                        return;
                    }
                    synchronized (BackupManagerService.this.mQueueLock) {
                        BackupManagerService.this.mBackupRunning = false;
                    }
                    BackupManagerService.this.mWakelock.release();
                    return;
                case 2:
                    FullBackupParams params = (FullBackupParams) msg.obj;
                    PerformAdbBackupTask task = BackupManagerService.this.new PerformAdbBackupTask(params.fd, params.observer, params.includeApks, params.includeObbs, params.includeShared, params.doWidgets, params.curPassword, params.encryptPassword, params.allApps, params.includeSystem, params.doCompress, params.packages, params.latch);
                    new Thread(task, "adb-backup").start();
                    return;
                case 3:
                    RestoreParams params2 = (RestoreParams) msg.obj;
                    Slog.d(BackupManagerService.TAG, "MSG_RUN_RESTORE observer=" + params2.observer);
                    BackupRestoreTask task2 = BackupManagerService.this.new PerformUnifiedRestoreTask(params2.transport, params2.observer, params2.token, params2.pkgInfo, params2.pmToken, params2.isSystemRestore, params2.filterSet);
                    Message restoreMsg = obtainMessage(20, task2);
                    sendMessage(restoreMsg);
                    return;
                case 4:
                    ClearParams params3 = (ClearParams) msg.obj;
                    BackupManagerService.this.new PerformClearTask(params3.transport, params3.packageInfo).run();
                    return;
                case 5:
                    synchronized (BackupManagerService.this.mQueueLock) {
                        queue = new HashSet<>(BackupManagerService.this.mPendingInits);
                        BackupManagerService.this.mPendingInits.clear();
                    }
                    BackupManagerService.this.new PerformInitializeTask(queue).run();
                    return;
                case 6:
                    RestoreGetSetsParams params4 = (RestoreGetSetsParams) msg.obj;
                    try {
                        try {
                            RestoreSet[] sets = params4.transport.getAvailableRestoreSets();
                            synchronized (params4.session) {
                                params4.session.mRestoreSets = sets;
                            }
                            if (sets == null) {
                                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                            }
                            if (params4.observer != null) {
                                try {
                                    params4.observer.restoreSetsAvailable(sets);
                                } catch (RemoteException e2) {
                                    Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                                } catch (Exception e3) {
                                    Slog.e(BackupManagerService.TAG, "Restore observer threw", e3);
                                }
                                break;
                            }
                            removeMessages(8);
                            sendEmptyMessageDelayed(8, 60000L);
                            BackupManagerService.this.mWakelock.release();
                            return;
                        } catch (Exception e4) {
                            Slog.e(BackupManagerService.TAG, "Error from transport getting set list");
                            if (params4.observer != null) {
                                try {
                                    params4.observer.restoreSetsAvailable((RestoreSet[]) null);
                                } catch (RemoteException e5) {
                                    Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                                } catch (Exception e6) {
                                    Slog.e(BackupManagerService.TAG, "Restore observer threw", e6);
                                }
                                break;
                            }
                            removeMessages(8);
                            sendEmptyMessageDelayed(8, 60000L);
                            BackupManagerService.this.mWakelock.release();
                            return;
                        }
                    } catch (Throwable th) {
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable((RestoreSet[]) null);
                            } catch (RemoteException e7) {
                                Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e8) {
                                Slog.e(BackupManagerService.TAG, "Restore observer threw", e8);
                            }
                            break;
                        }
                        removeMessages(8);
                        sendEmptyMessageDelayed(8, 60000L);
                        BackupManagerService.this.mWakelock.release();
                        throw th;
                    }
                case 7:
                    BackupManagerService.this.handleTimeout(msg.arg1, msg.obj);
                    return;
                case 8:
                    synchronized (BackupManagerService.this) {
                        if (BackupManagerService.this.mActiveRestoreSession != null) {
                            Slog.w(BackupManagerService.TAG, "Restore session timed out; aborting");
                            BackupManagerService.this.mActiveRestoreSession.markTimedOut();
                            ActiveRestoreSession activeRestoreSession = BackupManagerService.this.mActiveRestoreSession;
                            activeRestoreSession.getClass();
                            post(activeRestoreSession.new EndRestoreRunnable(BackupManagerService.this, BackupManagerService.this.mActiveRestoreSession));
                        }
                        break;
                    }
                    return;
                case 9:
                    synchronized (BackupManagerService.this.mFullConfirmations) {
                        FullParams params5 = BackupManagerService.this.mFullConfirmations.get(msg.arg1);
                        if (params5 != null) {
                            Slog.i(BackupManagerService.TAG, "Full backup/restore timed out waiting for user confirmation");
                            BackupManagerService.this.signalFullBackupRestoreCompletion(params5);
                            BackupManagerService.this.mFullConfirmations.delete(msg.arg1);
                            if (params5.observer != null) {
                                try {
                                    params5.observer.onTimeout();
                                    break;
                                } catch (RemoteException e9) {
                                }
                            }
                        } else {
                            Slog.d(BackupManagerService.TAG, "couldn't find params for token " + msg.arg1);
                        }
                    }
                    return;
                case 10:
                    FullRestoreParams params6 = (FullRestoreParams) msg.obj;
                    PerformAdbRestoreTask task3 = BackupManagerService.this.new PerformAdbRestoreTask(params6.fd, params6.curPassword, params6.encryptPassword, params6.observer, params6.latch);
                    new Thread(task3, "adb-restore").start();
                    return;
                case 11:
                    synchronized (BackupManagerService.this.mQueueLock) {
                        BackupManagerService.this.recordInitPendingLocked(msg.arg1 != 0, (String) msg.obj);
                        BackupManagerService.this.mAlarmManager.set(0, System.currentTimeMillis(), BackupManagerService.this.mRunInitIntent);
                    }
                    return;
                case 12:
                    ClearRetryParams params7 = (ClearRetryParams) msg.obj;
                    BackupManagerService.this.clearBackupData(params7.transportName, params7.packageName);
                    return;
                case 13:
                    Intent intent = (Intent) msg.obj;
                    BackupManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                    return;
                case 14:
                    PerformFullTransportBackupTask task4 = (PerformFullTransportBackupTask) msg.obj;
                    new Thread(task4, "transport-backup").start();
                    return;
                case 15:
                    BackupParams params8 = (BackupParams) msg.obj;
                    ArrayList<BackupRequest> kvQueue = new ArrayList<>();
                    for (String packageName : params8.kvPackages) {
                        kvQueue.add(BackupManagerService.this.new BackupRequest(packageName));
                    }
                    BackupManagerService.this.mBackupRunning = true;
                    BackupManagerService.this.mWakelock.acquire();
                    PerformBackupTask pbt2 = BackupManagerService.this.new PerformBackupTask(params8.transport, params8.dirName, kvQueue, null, params8.observer, params8.fullPackages, true);
                    Message pbtMessage2 = obtainMessage(20, pbt2);
                    sendMessage(pbtMessage2);
                    return;
                case 16:
                case 17:
                case 18:
                case 19:
                default:
                    return;
                case 20:
                    try {
                        BackupRestoreTask task5 = (BackupRestoreTask) msg.obj;
                        task5.execute();
                        return;
                    } catch (ClassCastException e10) {
                        Slog.e(BackupManagerService.TAG, "Invalid backup task in flight, obj=" + msg.obj);
                        return;
                    }
                case 21:
                    try {
                        Pair<BackupRestoreTask, Long> taskWithResult = (Pair) msg.obj;
                        ((BackupRestoreTask) taskWithResult.first).operationComplete(((Long) taskWithResult.second).longValue());
                        return;
                    } catch (ClassCastException e11) {
                        Slog.e(BackupManagerService.TAG, "Invalid completion in flight, obj=" + msg.obj);
                        return;
                    }
            }
        }
    }

    void addBackupTrace(String s) {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.add(s);
        }
    }

    void clearBackupTrace() {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.clear();
        }
    }

    public BackupManagerService(Context context, Trampoline parent) throws Throwable {
        FileInputStream fin;
        DataInputStream in;
        FileInputStream fin2;
        DataInputStream in2;
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mBackupManagerBinder = Trampoline.asInterface(parent.asBinder());
        this.mHandlerThread.start();
        this.mBackupHandler = new BackupHandler(this.mHandlerThread.getLooper());
        ContentResolver resolver = context.getContentResolver();
        this.mProvisioned = Settings.Global.getInt(resolver, "device_provisioned", 0) != 0;
        this.mAutoRestore = Settings.Secure.getInt(resolver, "backup_auto_restore", 1) != 0;
        this.mProvisionedObserver = new ProvisionedObserver(this.mBackupHandler);
        resolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mProvisionedObserver);
        this.mBaseStateDir = new File(Environment.getDataDirectory(), "backup");
        this.mBaseStateDir.mkdirs();
        if (!SELinux.restorecon(this.mBaseStateDir)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBaseStateDir);
        }
        this.mDataDir = new File(Environment.getDownloadCacheDirectory(), "backup_stage");
        this.mPasswordVersion = 1;
        this.mPasswordVersionFile = new File(this.mBaseStateDir, "pwversion");
        if (this.mPasswordVersionFile.exists()) {
            FileInputStream fin3 = null;
            DataInputStream in3 = null;
            try {
                try {
                    fin2 = new FileInputStream(this.mPasswordVersionFile);
                    try {
                        in2 = new DataInputStream(fin2);
                    } catch (IOException e) {
                        fin3 = fin2;
                    } catch (Throwable th) {
                        th = th;
                        fin3 = fin2;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e2) {
            }
            try {
                this.mPasswordVersion = in2.readInt();
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (IOException e3) {
                        Slog.w(TAG, "Error closing pw version files");
                    }
                }
                if (fin2 != null) {
                    fin2.close();
                }
            } catch (IOException e4) {
                in3 = in2;
                fin3 = fin2;
                Slog.e(TAG, "Unable to read backup pw version");
                if (in3 != null) {
                    try {
                        in3.close();
                    } catch (IOException e5) {
                        Slog.w(TAG, "Error closing pw version files");
                    }
                }
                if (fin3 != null) {
                    fin3.close();
                }
            } catch (Throwable th3) {
                th = th3;
                in3 = in2;
                fin3 = fin2;
                if (in3 != null) {
                    try {
                        in3.close();
                    } catch (IOException e6) {
                        Slog.w(TAG, "Error closing pw version files");
                        throw th;
                    }
                }
                if (fin3 != null) {
                    fin3.close();
                }
                throw th;
            }
        }
        this.mPasswordHashFile = new File(this.mBaseStateDir, "pwhash");
        if (this.mPasswordHashFile.exists()) {
            FileInputStream fin4 = null;
            DataInputStream in4 = null;
            try {
                try {
                    fin = new FileInputStream(this.mPasswordHashFile);
                    try {
                        in = new DataInputStream(new BufferedInputStream(fin));
                    } catch (IOException e7) {
                        fin4 = fin;
                    } catch (Throwable th4) {
                        th = th4;
                        fin4 = fin;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (IOException e8) {
            }
            try {
                int saltLen = in.readInt();
                byte[] salt = new byte[saltLen];
                in.readFully(salt);
                this.mPasswordHash = in.readUTF();
                this.mPasswordSalt = salt;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e9) {
                        Slog.w(TAG, "Unable to close streams");
                    }
                }
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e10) {
                in4 = in;
                fin4 = fin;
                Slog.e(TAG, "Unable to read saved backup pw hash");
                if (in4 != null) {
                    try {
                        in4.close();
                    } catch (IOException e11) {
                        Slog.w(TAG, "Unable to close streams");
                    }
                }
                if (fin4 != null) {
                    fin4.close();
                }
            } catch (Throwable th6) {
                th = th6;
                in4 = in;
                fin4 = fin;
                if (in4 != null) {
                    try {
                        in4.close();
                    } catch (IOException e12) {
                        Slog.w(TAG, "Unable to close streams");
                        throw th;
                    }
                }
                if (fin4 != null) {
                    fin4.close();
                }
                throw th;
            }
        }
        this.mRunBackupReceiver = new RunBackupReceiver(this, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RUN_BACKUP_ACTION);
        context.registerReceiver(this.mRunBackupReceiver, filter, "android.permission.BACKUP", null);
        this.mRunInitReceiver = new RunInitializeReceiver(this, null);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(RUN_INITIALIZE_ACTION);
        context.registerReceiver(this.mRunInitReceiver, filter2, "android.permission.BACKUP", null);
        Intent backupIntent = new Intent(RUN_BACKUP_ACTION);
        backupIntent.addFlags(1073741824);
        this.mRunBackupIntent = PendingIntent.getBroadcast(context, 1, backupIntent, 0);
        Intent initIntent = new Intent(RUN_INITIALIZE_ACTION);
        backupIntent.addFlags(1073741824);
        this.mRunInitIntent = PendingIntent.getBroadcast(context, 5, initIntent, 0);
        this.mJournalDir = new File(this.mBaseStateDir, "pending");
        this.mJournalDir.mkdirs();
        this.mJournal = null;
        this.mFullBackupScheduleFile = new File(this.mBaseStateDir, "fb-schedule");
        initPackageTracking();
        synchronized (this.mBackupParticipants) {
            addPackageParticipantsLocked(null);
        }
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mTransportWhitelist = systemConfig.getBackupTransportWhitelist();
        String transport = Settings.Secure.getString(context.getContentResolver(), "backup_transport");
        this.mCurrentTransport = TextUtils.isEmpty(transport) ? null : transport;
        Slog.v(TAG, "Starting with transport " + this.mCurrentTransport);
        List<ResolveInfo> hosts = this.mPackageManager.queryIntentServicesAsUser(this.mTransportServiceIntent, 0, 0);
        Slog.v(TAG, "Found transports: " + (hosts == null ? "null" : Integer.valueOf(hosts.size())));
        if (hosts != null) {
            for (int i = 0; i < hosts.size(); i++) {
                ServiceInfo transportService = hosts.get(i).serviceInfo;
                tryBindTransport(transportService);
            }
        }
        parseLeftoverJournals();
        this.mWakelock = this.mPowerManager.newWakeLock(1, "*backup*");
    }

    private class RunBackupReceiver extends BroadcastReceiver {
        RunBackupReceiver(BackupManagerService this$0, RunBackupReceiver runBackupReceiver) {
            this();
        }

        private RunBackupReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BackupManagerService.RUN_BACKUP_ACTION.equals(intent.getAction())) {
                return;
            }
            synchronized (BackupManagerService.this.mQueueLock) {
                if (BackupManagerService.this.mPendingInits.size() > 0) {
                    try {
                        BackupManagerService.this.mAlarmManager.cancel(BackupManagerService.this.mRunInitIntent);
                        BackupManagerService.this.mRunInitIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Slog.e(BackupManagerService.TAG, "Run init intent cancelled");
                    }
                } else if (BackupManagerService.this.mEnabled && BackupManagerService.this.mProvisioned) {
                    if (!BackupManagerService.this.mBackupRunning) {
                        Slog.v(BackupManagerService.TAG, "Running a backup pass");
                        BackupManagerService.this.mBackupRunning = true;
                        BackupManagerService.this.mWakelock.acquire();
                        Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(1);
                        BackupManagerService.this.mBackupHandler.sendMessage(msg);
                    } else {
                        Slog.i(BackupManagerService.TAG, "Backup time but one already running");
                    }
                } else {
                    Slog.w(BackupManagerService.TAG, "Backup pass but e=" + BackupManagerService.this.mEnabled + " p=" + BackupManagerService.this.mProvisioned);
                }
            }
        }
    }

    private class RunInitializeReceiver extends BroadcastReceiver {
        RunInitializeReceiver(BackupManagerService this$0, RunInitializeReceiver runInitializeReceiver) {
            this();
        }

        private RunInitializeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BackupManagerService.RUN_INITIALIZE_ACTION.equals(intent.getAction())) {
                return;
            }
            synchronized (BackupManagerService.this.mQueueLock) {
                Slog.v(BackupManagerService.TAG, "Running a device init");
                BackupManagerService.this.mWakelock.acquire();
                Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(5);
                BackupManagerService.this.mBackupHandler.sendMessage(msg);
            }
        }
    }

    private void initPackageTracking() throws Throwable {
        this.mTokenFile = new File(this.mBaseStateDir, "ancestral");
        try {
            RandomAccessFile tf = new RandomAccessFile(this.mTokenFile, "r");
            int version = tf.readInt();
            if (version == 1) {
                this.mAncestralToken = tf.readLong();
                this.mCurrentToken = tf.readLong();
                int numPackages = tf.readInt();
                if (numPackages >= 0) {
                    this.mAncestralPackages = new HashSet();
                    for (int i = 0; i < numPackages; i++) {
                        String pkgName = tf.readUTF();
                        this.mAncestralPackages.add(pkgName);
                    }
                }
            }
            tf.close();
        } catch (FileNotFoundException e) {
            Slog.v(TAG, "No ancestral data");
        } catch (IOException e2) {
            Slog.w(TAG, "Unable to read token file", e2);
        }
        this.mEverStored = new File(this.mBaseStateDir, "processed");
        File tempProcessedFile = new File(this.mBaseStateDir, "processed.new");
        if (tempProcessedFile.exists()) {
            tempProcessedFile.delete();
        }
        if (this.mEverStored.exists()) {
            RandomAccessFile temp = null;
            RandomAccessFile in = null;
            try {
                try {
                    RandomAccessFile temp2 = new RandomAccessFile(tempProcessedFile, "rws");
                    try {
                        RandomAccessFile in2 = new RandomAccessFile(this.mEverStored, "r");
                        while (true) {
                            try {
                                String pkg = in2.readUTF();
                                try {
                                    this.mPackageManager.getPackageInfo(pkg, 0);
                                    this.mEverStoredApps.add(pkg);
                                    temp2.writeUTF(pkg);
                                } catch (PackageManager.NameNotFoundException e3) {
                                }
                            } catch (EOFException e4) {
                                in = in2;
                                temp = temp2;
                                if (!tempProcessedFile.renameTo(this.mEverStored)) {
                                    Slog.e(TAG, "Error renaming " + tempProcessedFile + " to " + this.mEverStored);
                                }
                                if (temp != null) {
                                    try {
                                        temp.close();
                                    } catch (IOException e5) {
                                    }
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e6) {
                                    }
                                }
                                synchronized (this.mQueueLock) {
                                }
                            } catch (IOException e7) {
                                e = e7;
                                in = in2;
                                temp = temp2;
                                Slog.e(TAG, "Error in processed file", e);
                                if (temp != null) {
                                    try {
                                        temp.close();
                                    } catch (IOException e8) {
                                    }
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e9) {
                                    }
                                }
                                synchronized (this.mQueueLock) {
                                }
                            } catch (Throwable th) {
                                th = th;
                                in = in2;
                                temp = temp2;
                                if (temp != null) {
                                    try {
                                        temp.close();
                                    } catch (IOException e10) {
                                    }
                                }
                                if (in == null) {
                                    throw th;
                                }
                                try {
                                    in.close();
                                    throw th;
                                } catch (IOException e11) {
                                    throw th;
                                }
                            }
                        }
                    } catch (EOFException e12) {
                        temp = temp2;
                    } catch (IOException e13) {
                        e = e13;
                        temp = temp2;
                    } catch (Throwable th2) {
                        th = th2;
                        temp = temp2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (EOFException e14) {
            } catch (IOException e15) {
                e = e15;
            }
        }
        synchronized (this.mQueueLock) {
            this.mFullBackupQueue = readFullBackupSchedule();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
    }

    private ArrayList<FullBackupEntry> readFullBackupSchedule() throws Throwable {
        boolean changed = false;
        ArrayList<FullBackupEntry> schedule = null;
        List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(this.mPackageManager);
        if (this.mFullBackupScheduleFile.exists()) {
            FileInputStream fstream = null;
            BufferedInputStream bufStream = null;
            DataInputStream in = null;
            try {
                try {
                    FileInputStream fstream2 = new FileInputStream(this.mFullBackupScheduleFile);
                    try {
                        BufferedInputStream bufStream2 = new BufferedInputStream(fstream2);
                        try {
                            DataInputStream in2 = new DataInputStream(bufStream2);
                            try {
                                int version = in2.readInt();
                                if (version != 1) {
                                    Slog.e(TAG, "Unknown backup schedule version " + version);
                                    IoUtils.closeQuietly(in2);
                                    IoUtils.closeQuietly(bufStream2);
                                    IoUtils.closeQuietly(fstream2);
                                    return null;
                                }
                                int N = in2.readInt();
                                ArrayList<FullBackupEntry> schedule2 = new ArrayList<>(N);
                                try {
                                    HashSet<String> foundApps = new HashSet<>(N);
                                    for (int i = 0; i < N; i++) {
                                        String pkgName = in2.readUTF();
                                        long lastBackup = in2.readLong();
                                        foundApps.add(pkgName);
                                        try {
                                            PackageInfo pkg = this.mPackageManager.getPackageInfo(pkgName, 0);
                                            if (appGetsFullBackup(pkg) && appIsEligibleForBackup(pkg.applicationInfo)) {
                                                schedule2.add(new FullBackupEntry(pkgName, lastBackup));
                                            } else {
                                                Slog.i(TAG, "Package " + pkgName + " no longer eligible for full backup");
                                            }
                                        } catch (PackageManager.NameNotFoundException e) {
                                            Slog.i(TAG, "Package " + pkgName + " not installed; dropping from full backup");
                                        }
                                    }
                                    for (PackageInfo app : apps) {
                                        if (appGetsFullBackup(app) && appIsEligibleForBackup(app.applicationInfo) && !foundApps.contains(app.packageName)) {
                                            schedule2.add(new FullBackupEntry(app.packageName, 0L));
                                            changed = true;
                                        }
                                    }
                                    Collections.sort(schedule2);
                                    IoUtils.closeQuietly(in2);
                                    IoUtils.closeQuietly(bufStream2);
                                    IoUtils.closeQuietly(fstream2);
                                    schedule = schedule2;
                                } catch (Exception e2) {
                                    e = e2;
                                    in = in2;
                                    bufStream = bufStream2;
                                    fstream = fstream2;
                                    Slog.e(TAG, "Unable to read backup schedule", e);
                                    this.mFullBackupScheduleFile.delete();
                                    schedule = null;
                                    IoUtils.closeQuietly(in);
                                    IoUtils.closeQuietly(bufStream);
                                    IoUtils.closeQuietly(fstream);
                                    if (schedule == null) {
                                    }
                                    if (changed) {
                                    }
                                    return schedule;
                                } catch (Throwable th) {
                                    th = th;
                                    in = in2;
                                    bufStream = bufStream2;
                                    fstream = fstream2;
                                    IoUtils.closeQuietly(in);
                                    IoUtils.closeQuietly(bufStream);
                                    IoUtils.closeQuietly(fstream);
                                    throw th;
                                }
                            } catch (Exception e3) {
                                e = e3;
                                in = in2;
                                bufStream = bufStream2;
                                fstream = fstream2;
                            } catch (Throwable th2) {
                                th = th2;
                                in = in2;
                                bufStream = bufStream2;
                                fstream = fstream2;
                            }
                        } catch (Exception e4) {
                            e = e4;
                            bufStream = bufStream2;
                            fstream = fstream2;
                        } catch (Throwable th3) {
                            th = th3;
                            bufStream = bufStream2;
                            fstream = fstream2;
                        }
                    } catch (Exception e5) {
                        e = e5;
                        fstream = fstream2;
                    } catch (Throwable th4) {
                        th = th4;
                        fstream = fstream2;
                    }
                } catch (Exception e6) {
                    e = e6;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
        if (schedule == null) {
            changed = true;
            schedule = new ArrayList<>(apps.size());
            for (PackageInfo info : apps) {
                if (appGetsFullBackup(info) && appIsEligibleForBackup(info.applicationInfo)) {
                    schedule.add(new FullBackupEntry(info.packageName, 0L));
                }
            }
        }
        if (changed) {
            writeFullBackupScheduleAsync();
        }
        return schedule;
    }

    private void writeFullBackupScheduleAsync() {
        this.mBackupHandler.removeCallbacks(this.mFullBackupScheduleWriter);
        this.mBackupHandler.post(this.mFullBackupScheduleWriter);
    }

    private void parseLeftoverJournals() throws Throwable {
        Throwable th;
        for (File f : this.mJournalDir.listFiles()) {
            if (this.mJournal == null || f.compareTo(this.mJournal) != 0) {
                RandomAccessFile in = null;
                try {
                    Slog.i(TAG, "Found stale backup journal, scheduling");
                    RandomAccessFile in2 = new RandomAccessFile(f, "r");
                    while (true) {
                        try {
                            String packageName = in2.readUTF();
                            dataChangedImpl(packageName);
                        } catch (EOFException e) {
                            in = in2;
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e2) {
                                }
                            }
                            f.delete();
                        } catch (Exception e3) {
                            e = e3;
                            in = in2;
                            try {
                                Slog.e(TAG, "Can't read " + f, e);
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e4) {
                                    }
                                }
                                f.delete();
                            } catch (Throwable th2) {
                                th = th2;
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e5) {
                                    }
                                }
                                f.delete();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            in = in2;
                            if (in != null) {
                            }
                            f.delete();
                            throw th;
                        }
                    }
                } catch (EOFException e6) {
                } catch (Exception e7) {
                    e = e7;
                }
            }
        }
    }

    private SecretKey buildPasswordKey(String algorithm, String pw, byte[] salt, int rounds) {
        return buildCharArrayKey(algorithm, pw.toCharArray(), salt, rounds);
    }

    private SecretKey buildCharArrayKey(String algorithm, char[] pwArray, byte[] salt, int rounds) {
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
            KeySpec ks = new PBEKeySpec(pwArray, salt, rounds, 256);
            return keyFactory.generateSecret(ks);
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "PBKDF2 unavailable!");
            return null;
        } catch (InvalidKeySpecException e2) {
            Slog.e(TAG, "Invalid key spec for PBKDF2!");
            return null;
        }
    }

    private String buildPasswordHash(String algorithm, String pw, byte[] salt, int rounds) {
        SecretKey key = buildPasswordKey(algorithm, pw, salt, rounds);
        if (key != null) {
            return byteArrayToHex(key.getEncoded());
        }
        return null;
    }

    private String byteArrayToHex(byte[] data) {
        StringBuilder buf = new StringBuilder(data.length * 2);
        for (byte b : data) {
            buf.append(Byte.toHexString(b, true));
        }
        return buf.toString();
    }

    private byte[] hexToByteArray(String digits) {
        int bytes = digits.length() / 2;
        if (bytes * 2 != digits.length()) {
            throw new IllegalArgumentException("Hex string must have an even number of digits");
        }
        byte[] result = new byte[bytes];
        for (int i = 0; i < digits.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(digits.substring(i, i + 2), 16);
        }
        return result;
    }

    private byte[] makeKeyChecksum(String algorithm, byte[] pwBytes, byte[] salt, int rounds) {
        char[] mkAsChar = new char[pwBytes.length];
        for (int i = 0; i < pwBytes.length; i++) {
            mkAsChar[i] = (char) pwBytes[i];
        }
        Key checksum = buildCharArrayKey(algorithm, mkAsChar, salt, rounds);
        return checksum.getEncoded();
    }

    private byte[] randomBytes(int bits) {
        byte[] array = new byte[bits / 8];
        this.mRng.nextBytes(array);
        return array;
    }

    boolean passwordMatchesSaved(String algorithm, String candidatePw, int rounds) {
        if (this.mPasswordHash == null) {
            if (candidatePw == null || "".equals(candidatePw)) {
                return true;
            }
        } else if (candidatePw != null && candidatePw.length() > 0) {
            String currentPwHash = buildPasswordHash(algorithm, candidatePw, this.mPasswordSalt, rounds);
            if (this.mPasswordHash.equalsIgnoreCase(currentPwHash)) {
                return true;
            }
        }
        return false;
    }

    public boolean setBackupPassword(String currentPw, String newPw) throws Throwable {
        OutputStream pwf;
        OutputStream buffer;
        DataOutputStream out;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupPassword");
        boolean pbkdf2Fallback = this.mPasswordVersion < 2;
        if (!passwordMatchesSaved(PBKDF_CURRENT, currentPw, 10000) && (!pbkdf2Fallback || !passwordMatchesSaved(PBKDF_FALLBACK, currentPw, 10000))) {
            return false;
        }
        this.mPasswordVersion = 2;
        FileOutputStream pwFout = null;
        DataOutputStream pwOut = null;
        try {
            try {
                FileOutputStream pwFout2 = new FileOutputStream(this.mPasswordVersionFile);
                try {
                    DataOutputStream pwOut2 = new DataOutputStream(pwFout2);
                    try {
                        pwOut2.writeInt(this.mPasswordVersion);
                        if (pwOut2 != null) {
                            try {
                                pwOut2.close();
                            } catch (IOException e) {
                                Slog.w(TAG, "Unable to close pw version record");
                            }
                        }
                        if (pwFout2 != null) {
                            pwFout2.close();
                        }
                        if (newPw == null || newPw.isEmpty()) {
                            if (this.mPasswordHashFile.exists() && !this.mPasswordHashFile.delete()) {
                                Slog.e(TAG, "Unable to clear backup password");
                                return false;
                            }
                            this.mPasswordHash = null;
                            this.mPasswordSalt = null;
                            return true;
                        }
                        try {
                            byte[] salt = randomBytes(512);
                            String newPwHash = buildPasswordHash(PBKDF_CURRENT, newPw, salt, 10000);
                            OutputStream pwf2 = null;
                            OutputStream buffer2 = null;
                            DataOutputStream out2 = null;
                            try {
                                pwf = new FileOutputStream(this.mPasswordHashFile);
                                try {
                                    buffer = new BufferedOutputStream(pwf);
                                    try {
                                        out = new DataOutputStream(buffer);
                                    } catch (Throwable th) {
                                        th = th;
                                        buffer2 = buffer;
                                        pwf2 = pwf;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    pwf2 = pwf;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            try {
                                out.writeInt(salt.length);
                                out.write(salt);
                                out.writeUTF(newPwHash);
                                out.flush();
                                this.mPasswordHash = newPwHash;
                                this.mPasswordSalt = salt;
                                if (out != null) {
                                    out.close();
                                }
                                if (buffer != null) {
                                    buffer.close();
                                }
                                if (pwf != null) {
                                    pwf.close();
                                }
                                return true;
                            } catch (Throwable th4) {
                                th = th4;
                                out2 = out;
                                buffer2 = buffer;
                                pwf2 = pwf;
                                if (out2 != null) {
                                    out2.close();
                                }
                                if (buffer2 != null) {
                                    buffer2.close();
                                }
                                if (pwf2 != null) {
                                    pwf2.close();
                                }
                                throw th;
                            }
                        } catch (IOException e2) {
                            Slog.e(TAG, "Unable to set backup password");
                            return false;
                        }
                    } catch (IOException e3) {
                        pwOut = pwOut2;
                        pwFout = pwFout2;
                        Slog.e(TAG, "Unable to write backup pw version; password not changed");
                        if (pwOut != null) {
                            try {
                                pwOut.close();
                            } catch (IOException e4) {
                                Slog.w(TAG, "Unable to close pw version record");
                                return false;
                            }
                        }
                        if (pwFout != null) {
                            pwFout.close();
                        }
                        return false;
                    } catch (Throwable th5) {
                        th = th5;
                        pwOut = pwOut2;
                        pwFout = pwFout2;
                        if (pwOut != null) {
                            try {
                                pwOut.close();
                            } catch (IOException e5) {
                                Slog.w(TAG, "Unable to close pw version record");
                                throw th;
                            }
                        }
                        if (pwFout != null) {
                            pwFout.close();
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    pwFout = pwFout2;
                } catch (Throwable th6) {
                    th = th6;
                    pwFout = pwFout2;
                }
            } catch (IOException e7) {
            }
        } catch (Throwable th7) {
            th = th7;
        }
    }

    public boolean hasBackupPassword() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "hasBackupPassword");
        return this.mPasswordHash != null && this.mPasswordHash.length() > 0;
    }

    private boolean backupPasswordMatches(String currentPw) {
        if (hasBackupPassword()) {
            boolean pbkdf2Fallback = this.mPasswordVersion < 2;
            if (!passwordMatchesSaved(PBKDF_CURRENT, currentPw, 10000)) {
                if (!pbkdf2Fallback || !passwordMatchesSaved(PBKDF_FALLBACK, currentPw, 10000)) {
                    Slog.w(TAG, "Backup password mismatch; aborting");
                    return false;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    void recordInitPendingLocked(boolean isPending, String transportName) {
        this.mBackupHandler.removeMessages(11);
        try {
            IBackupTransport transport = getTransport(transportName);
            if (transport != null) {
                String transportDirName = transport.transportDirName();
                File stateDir = new File(this.mBaseStateDir, transportDirName);
                File initPendingFile = new File(stateDir, INIT_SENTINEL_FILE_NAME);
                if (isPending) {
                    this.mPendingInits.add(transportName);
                    try {
                        new FileOutputStream(initPendingFile).close();
                        return;
                    } catch (IOException e) {
                        return;
                    }
                } else {
                    initPendingFile.delete();
                    this.mPendingInits.remove(transportName);
                    return;
                }
            }
        } catch (RemoteException e2) {
        }
        if (!isPending) {
            return;
        }
        this.mPendingInits.add(transportName);
        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(11, isPending ? 1 : 0, 0, transportName), 3600000L);
    }

    void resetBackupState(File stateFileDir) {
        synchronized (this.mQueueLock) {
            this.mEverStoredApps.clear();
            this.mEverStored.delete();
            this.mCurrentToken = 0L;
            writeRestoreTokens();
            for (File sf : stateFileDir.listFiles()) {
                if (!sf.getName().equals(INIT_SENTINEL_FILE_NAME)) {
                    sf.delete();
                }
            }
        }
        synchronized (this.mBackupParticipants) {
            int N = this.mBackupParticipants.size();
            for (int i = 0; i < N; i++) {
                HashSet<String> participants = this.mBackupParticipants.valueAt(i);
                if (participants != null) {
                    for (String packageName : participants) {
                        dataChangedImpl(packageName);
                    }
                }
            }
        }
    }

    private void registerTransport(String name, String component, IBackupTransport transport) {
        synchronized (this.mTransports) {
            Slog.v(TAG, "Registering transport " + component + "::" + name + " = " + transport);
            if (transport != null) {
                this.mTransports.put(name, transport);
                this.mTransportNames.put(component, name);
                try {
                    String transportName = transport.transportDirName();
                    File stateDir = new File(this.mBaseStateDir, transportName);
                    stateDir.mkdirs();
                    File initSentinel = new File(stateDir, INIT_SENTINEL_FILE_NAME);
                    if (!initSentinel.exists()) {
                        return;
                    }
                    synchronized (this.mQueueLock) {
                        this.mPendingInits.add(name);
                        this.mAlarmManager.set(0, System.currentTimeMillis() + 60000, this.mRunInitIntent);
                    }
                    return;
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to register transport as " + name);
                    this.mTransportNames.remove(component);
                    this.mTransports.remove(name);
                    return;
                }
            }
            this.mTransports.remove(this.mTransportNames.get(component));
            this.mTransportNames.remove(component);
        }
    }

    class TransportConnection implements ServiceConnection {
        ServiceInfo mTransport;

        public TransportConnection(ServiceInfo transport) {
            this.mTransport = transport;
        }

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            Slog.v(BackupManagerService.TAG, "Connected to transport " + component);
            String name = component.flattenToShortString();
            try {
                IBackupTransport transport = IBackupTransport.Stub.asInterface(service);
                BackupManagerService.this.registerTransport(transport.name(), name, transport);
                EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, name, 1);
            } catch (RemoteException e) {
                Slog.e(BackupManagerService.TAG, "Unable to register transport " + component);
                EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, name, 0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            Slog.v(BackupManagerService.TAG, "Disconnected from transport " + component);
            String name = component.flattenToShortString();
            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, name, 0);
            BackupManagerService.this.registerTransport(null, name, null);
        }
    }

    void checkForTransportAndBind(PackageInfo pkgInfo) {
        Intent intent = new Intent(this.mTransportServiceIntent).setPackage(pkgInfo.packageName);
        List<ResolveInfo> hosts = this.mPackageManager.queryIntentServicesAsUser(intent, 0, 0);
        if (hosts == null) {
            return;
        }
        int N = hosts.size();
        for (int i = 0; i < N; i++) {
            ServiceInfo info = hosts.get(i).serviceInfo;
            tryBindTransport(info);
        }
    }

    boolean tryBindTransport(ServiceInfo info) {
        PackageInfo packInfo;
        try {
            packInfo = this.mPackageManager.getPackageInfo(info.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Problem resolving transport package " + info.packageName);
        }
        if ((packInfo.applicationInfo.privateFlags & 8) != 0) {
            return bindTransport(info);
        }
        Slog.w(TAG, "Transport package " + info.packageName + " not privileged");
        return false;
    }

    boolean bindTransport(ServiceInfo transport) {
        TransportConnection connection;
        ComponentName svcName = new ComponentName(transport.packageName, transport.name);
        if (!this.mTransportWhitelist.contains(svcName)) {
            Slog.w(TAG, "Proposed transport " + svcName + " not whitelisted; ignoring");
            return false;
        }
        Intent intent = new Intent(this.mTransportServiceIntent);
        intent.setComponent(svcName);
        synchronized (this.mTransports) {
            connection = this.mTransportConnections.get(transport.packageName);
            if (connection == null) {
                connection = new TransportConnection(transport);
                this.mTransportConnections.put(transport.packageName, connection);
            } else {
                this.mContext.unbindService(connection);
            }
        }
        return this.mContext.bindServiceAsUser(intent, connection, 1, UserHandle.SYSTEM);
    }

    void addPackageParticipantsLocked(String[] packageNames) {
        List<PackageInfo> targetApps = allAgentPackages();
        if (packageNames != null) {
            for (String packageName : packageNames) {
                addPackageParticipantsLockedInner(packageName, targetApps);
            }
            return;
        }
        addPackageParticipantsLockedInner(null, targetApps);
    }

    private void addPackageParticipantsLockedInner(String packageName, List<PackageInfo> targetPkgs) {
        for (PackageInfo pkg : targetPkgs) {
            if (packageName == null || pkg.packageName.equals(packageName)) {
                int uid = pkg.applicationInfo.uid;
                HashSet<String> set = this.mBackupParticipants.get(uid);
                if (set == null) {
                    set = new HashSet<>();
                    this.mBackupParticipants.put(uid, set);
                }
                set.add(pkg.packageName);
                dataChangedImpl(pkg.packageName);
            }
        }
    }

    void removePackageParticipantsLocked(String[] packageNames, int oldUid) {
        if (packageNames == null) {
            Slog.w(TAG, "removePackageParticipants with null list");
            return;
        }
        for (String pkg : packageNames) {
            HashSet<String> set = this.mBackupParticipants.get(oldUid);
            if (set != null && set.contains(pkg)) {
                removePackageFromSetLocked(set, pkg);
                if (set.isEmpty()) {
                    this.mBackupParticipants.remove(oldUid);
                }
            }
        }
    }

    private void removePackageFromSetLocked(HashSet<String> set, String packageName) {
        if (!set.contains(packageName)) {
            return;
        }
        set.remove(packageName);
        this.mPendingBackups.remove(packageName);
    }

    List<PackageInfo> allAgentPackages() {
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackages(64);
        int N = packages.size();
        for (int a = N - 1; a >= 0; a--) {
            PackageInfo pkg = packages.get(a);
            try {
                ApplicationInfo app = pkg.applicationInfo;
                if ((app.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0 || app.backupAgentName == null || (app.flags & 67108864) != 0) {
                    packages.remove(a);
                } else {
                    pkg.applicationInfo.sharedLibraryFiles = this.mPackageManager.getApplicationInfo(pkg.packageName, 1024).sharedLibraryFiles;
                }
            } catch (PackageManager.NameNotFoundException e) {
                packages.remove(a);
            }
        }
        return packages;
    }

    void logBackupComplete(String packageName) {
        RandomAccessFile out;
        if (packageName.equals(PACKAGE_MANAGER_SENTINEL)) {
            return;
        }
        synchronized (this.mEverStoredApps) {
            if (this.mEverStoredApps.add(packageName)) {
                RandomAccessFile out2 = null;
                try {
                    try {
                        out = new RandomAccessFile(this.mEverStored, "rws");
                    } catch (IOException e) {
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    out.seek(out.length());
                    out.writeUTF(packageName);
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (IOException e3) {
                    out2 = out;
                    Slog.e(TAG, "Can't log backup of " + packageName + " to " + this.mEverStored);
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    out2 = out;
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            }
        }
    }

    void removeEverBackedUp(String packageName) {
        RandomAccessFile known;
        Slog.v(TAG, "Removing backed-up knowledge of " + packageName);
        synchronized (this.mEverStoredApps) {
            File tempKnownFile = new File(this.mBaseStateDir, "processed.new");
            RandomAccessFile known2 = null;
            try {
                try {
                    known = new RandomAccessFile(tempKnownFile, "rws");
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                this.mEverStoredApps.remove(packageName);
                for (String s : this.mEverStoredApps) {
                    known.writeUTF(s);
                }
                known.close();
                known2 = null;
                if (!tempKnownFile.renameTo(this.mEverStored)) {
                    throw new IOException("Can't rename " + tempKnownFile + " to " + this.mEverStored);
                }
            } catch (IOException e2) {
                e = e2;
                known2 = known;
                Slog.w(TAG, "Error rewriting " + this.mEverStored, e);
                this.mEverStoredApps.clear();
                tempKnownFile.delete();
                this.mEverStored.delete();
                if (known2 != null) {
                    try {
                        known2.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                known2 = known;
                if (known2 != null) {
                    try {
                        known2.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
    }

    void writeRestoreTokens() {
        try {
            RandomAccessFile af = new RandomAccessFile(this.mTokenFile, "rwd");
            af.writeInt(1);
            af.writeLong(this.mAncestralToken);
            af.writeLong(this.mCurrentToken);
            if (this.mAncestralPackages == null) {
                af.writeInt(-1);
            } else {
                af.writeInt(this.mAncestralPackages.size());
                Slog.v(TAG, "Ancestral packages:  " + this.mAncestralPackages.size());
                for (String pkgName : this.mAncestralPackages) {
                    af.writeUTF(pkgName);
                }
            }
            af.close();
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write token file:", e);
        }
    }

    private IBackupTransport getTransport(String transportName) {
        IBackupTransport transport;
        synchronized (this.mTransports) {
            transport = this.mTransports.get(transportName);
            if (transport == null) {
                Slog.w(TAG, "Requested unavailable transport: " + transportName);
            }
        }
        return transport;
    }

    private String getTransportName(IBackupTransport transport) {
        synchronized (this.mTransports) {
            int N = this.mTransports.size();
            for (int i = 0; i < N; i++) {
                if (this.mTransports.valueAt(i).equals(transport)) {
                    return this.mTransports.keyAt(i);
                }
            }
            return null;
        }
    }

    IBackupAgent bindToAgentSynchronous(ApplicationInfo app, int mode) {
        IBackupAgent agent = null;
        synchronized (this.mAgentConnectLock) {
            this.mConnecting = true;
            this.mConnectedAgent = null;
            try {
                if (this.mActivityManager.bindBackupAgent(app.packageName, mode, 0)) {
                    Slog.d(TAG, "awaiting agent for " + app);
                    long timeoutMark = System.currentTimeMillis() + 10000;
                    while (this.mConnecting && this.mConnectedAgent == null && System.currentTimeMillis() < timeoutMark) {
                        try {
                            this.mAgentConnectLock.wait(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                        } catch (InterruptedException e) {
                            Slog.w(TAG, "Interrupted: " + e);
                            this.mActivityManager.clearPendingBackup();
                            return null;
                        }
                    }
                    if (this.mConnecting) {
                        Slog.w(TAG, "Timeout waiting for agent " + app);
                        this.mActivityManager.clearPendingBackup();
                        return null;
                    }
                    Slog.i(TAG, "got agent " + this.mConnectedAgent);
                    agent = this.mConnectedAgent;
                }
            } catch (RemoteException e2) {
            }
            return agent;
        }
    }

    void clearApplicationDataSynchronous(String packageName) {
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 0);
            if ((info.applicationInfo.flags & 64) == 0) {
                return;
            }
            ClearDataObserver observer = new ClearDataObserver();
            synchronized (this.mClearDataLock) {
                this.mClearingData = true;
                try {
                    this.mActivityManager.clearApplicationUserData(packageName, observer, 0);
                } catch (RemoteException e) {
                }
                long timeoutMark = System.currentTimeMillis() + 10000;
                while (this.mClearingData && System.currentTimeMillis() < timeoutMark) {
                    try {
                        this.mClearDataLock.wait(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                    } catch (InterruptedException e2) {
                        this.mClearingData = false;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e3) {
            Slog.w(TAG, "Tried to clear data for " + packageName + " but not found");
        }
    }

    class ClearDataObserver extends IPackageDataObserver.Stub {
        ClearDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            synchronized (BackupManagerService.this.mClearDataLock) {
                BackupManagerService.this.mClearingData = false;
                BackupManagerService.this.mClearDataLock.notifyAll();
            }
        }
    }

    public long getAvailableRestoreToken(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreToken");
        long token = this.mAncestralToken;
        synchronized (this.mQueueLock) {
            if (this.mEverStoredApps.contains(packageName)) {
                token = this.mCurrentToken;
            }
        }
        return token;
    }

    public int requestBackup(String[] packages, IBackupObserver observer) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "requestBackup");
        if (packages == null || packages.length < 1) {
            Slog.e(TAG, "No packages named for backup request");
            sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            throw new IllegalArgumentException("No packages are provided for backup");
        }
        IBackupTransport transport = getTransport(this.mCurrentTransport);
        if (transport == null) {
            sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
        ArrayList<String> fullBackupList = new ArrayList<>();
        ArrayList<String> kvBackupList = new ArrayList<>();
        for (String packageName : packages) {
            try {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 64);
                if (!appIsEligibleForBackup(packageInfo.applicationInfo)) {
                    sendBackupOnPackageResult(observer, packageName, -2001);
                } else if (appGetsFullBackup(packageInfo)) {
                    fullBackupList.add(packageInfo.packageName);
                } else {
                    kvBackupList.add(packageInfo.packageName);
                }
            } catch (PackageManager.NameNotFoundException e) {
                sendBackupOnPackageResult(observer, packageName, -2002);
            }
        }
        EventLog.writeEvent(EventLogTags.BACKUP_REQUESTED, Integer.valueOf(packages.length), Integer.valueOf(kvBackupList.size()), Integer.valueOf(fullBackupList.size()));
        try {
            String dirName = transport.transportDirName();
            Message msg = this.mBackupHandler.obtainMessage(15);
            msg.obj = new BackupParams(transport, dirName, kvBackupList, fullBackupList, observer, true);
            this.mBackupHandler.sendMessage(msg);
            return 0;
        } catch (RemoteException e2) {
            Slog.e(TAG, "Transport became unavailable while attempting backup");
            sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
    }

    void prepareOperationTimeout(int token, long interval, BackupRestoreTask callback) {
        synchronized (this.mCurrentOpLock) {
            this.mCurrentOperations.put(token, new Operation(0, callback));
            Message msg = this.mBackupHandler.obtainMessage(7, token, 0, callback);
            this.mBackupHandler.sendMessageDelayed(msg, interval);
        }
    }

    boolean waitUntilOperationComplete(int token) {
        int finalState = 0;
        synchronized (this.mCurrentOpLock) {
            while (true) {
                Operation op = this.mCurrentOperations.get(token);
                if (op == null) {
                    break;
                }
                if (op.state != 0) {
                    break;
                }
                try {
                    this.mCurrentOpLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        this.mBackupHandler.removeMessages(7);
        return finalState == 1;
    }

    void handleTimeout(int token, Object obj) {
        Operation op;
        synchronized (this.mCurrentOpLock) {
            op = this.mCurrentOperations.get(token);
            int state = op != null ? op.state : -1;
            if (state == 1) {
                op = null;
                this.mCurrentOperations.delete(token);
            } else if (state == 0) {
                Slog.v(TAG, "TIMEOUT: token=" + Integer.toHexString(token));
                op.state = -1;
            }
            this.mCurrentOpLock.notifyAll();
        }
        if (op == null || op.callback == null) {
            return;
        }
        op.callback.handleTimeout();
    }

    enum BackupState {
        INITIAL,
        RUNNING_QUEUE,
        FINAL;

        public static BackupState[] valuesCustom() {
            return values();
        }
    }

    class PerformBackupTask implements BackupRestoreTask {

        private static final int[] f5xf4cc63f1 = null;
        private static final String TAG = "PerformBackupTask";
        final int[] $SWITCH_TABLE$com$android$server$backup$BackupManagerService$BackupState;
        IBackupAgent mAgentBinder;
        ParcelFileDescriptor mBackupData;
        File mBackupDataName;
        PackageInfo mCurrentPackage;
        BackupState mCurrentState = BackupState.INITIAL;
        boolean mFinished = false;
        File mJournal;
        ParcelFileDescriptor mNewState;
        File mNewStateName;
        IBackupObserver mObserver;
        ArrayList<BackupRequest> mOriginalQueue;
        ArrayList<String> mPendingFullBackups;
        ArrayList<BackupRequest> mQueue;
        ParcelFileDescriptor mSavedState;
        File mSavedStateName;
        File mStateDir;
        int mStatus;
        IBackupTransport mTransport;
        boolean mUserInitiated;

        private static int[] m1167xd1ebcd() {
            if (f5xf4cc63f1 != null) {
                return f5xf4cc63f1;
            }
            int[] iArr = new int[BackupState.valuesCustom().length];
            try {
                iArr[BackupState.FINAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[BackupState.INITIAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[BackupState.RUNNING_QUEUE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f5xf4cc63f1 = iArr;
            return iArr;
        }

        public PerformBackupTask(IBackupTransport transport, String dirName, ArrayList<BackupRequest> queue, File journal, IBackupObserver observer, ArrayList<String> pendingFullBackups, boolean userInitiated) {
            this.mTransport = transport;
            this.mOriginalQueue = queue;
            this.mJournal = journal;
            this.mObserver = observer;
            this.mPendingFullBackups = pendingFullBackups;
            this.mUserInitiated = userInitiated;
            this.mStateDir = new File(BackupManagerService.this.mBaseStateDir, dirName);
            BackupManagerService.this.addBackupTrace("STATE => INITIAL");
        }

        @Override
        public void execute() {
            switch (m1167xd1ebcd()[this.mCurrentState.ordinal()]) {
                case 1:
                    if (this.mFinished) {
                        Slog.e(TAG, "Duplicate finish");
                    } else {
                        finalizeBackup();
                    }
                    this.mFinished = true;
                    break;
                case 2:
                    beginBackup();
                    break;
                case 3:
                    invokeNextAgent();
                    break;
            }
        }

        void beginBackup() {
            BackupManagerService.this.clearBackupTrace();
            StringBuilder b = new StringBuilder(256);
            b.append("beginBackup: [");
            for (BackupRequest req : this.mOriginalQueue) {
                b.append(' ');
                b.append(req.packageName);
            }
            b.append(" ]");
            BackupManagerService.this.addBackupTrace(b.toString());
            this.mAgentBinder = null;
            this.mStatus = 0;
            if (this.mOriginalQueue.isEmpty() && this.mPendingFullBackups.isEmpty()) {
                Slog.w(TAG, "Backup begun with an empty queue - nothing to do.");
                BackupManagerService.this.addBackupTrace("queue empty at begin");
                BackupManagerService.sendBackupFinished(this.mObserver, 0);
                executeNextState(BackupState.FINAL);
                return;
            }
            this.mQueue = (ArrayList) this.mOriginalQueue.clone();
            int i = 0;
            while (true) {
                if (i >= this.mQueue.size()) {
                    break;
                }
                if (!BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(this.mQueue.get(i).packageName)) {
                    i++;
                } else {
                    this.mQueue.remove(i);
                    break;
                }
            }
            Slog.v(TAG, "Beginning backup of " + this.mQueue.size() + " targets");
            File pmState = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
            try {
                try {
                    String transportName = this.mTransport.transportDirName();
                    EventLog.writeEvent(EventLogTags.BACKUP_START, transportName);
                    if (this.mStatus == 0 && pmState.length() <= 0) {
                        Slog.i(TAG, "Initializing (wiping) backup state and transport storage");
                        BackupManagerService.this.addBackupTrace("initializing transport " + transportName);
                        BackupManagerService.this.resetBackupState(this.mStateDir);
                        this.mStatus = this.mTransport.initializeDevice();
                        BackupManagerService.this.addBackupTrace("transport.initializeDevice() == " + this.mStatus);
                        if (this.mStatus == 0) {
                            EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                        } else {
                            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                            Slog.e(TAG, "Transport error in initializeDevice()");
                        }
                    }
                    if (this.mStatus == 0) {
                        PackageManagerBackupAgent pmAgent = new PackageManagerBackupAgent(BackupManagerService.this.mPackageManager);
                        this.mStatus = invokeAgentForBackup(BackupManagerService.PACKAGE_MANAGER_SENTINEL, IBackupAgent.Stub.asInterface(pmAgent.onBind()), this.mTransport);
                        BackupManagerService.this.addBackupTrace("PMBA invoke: " + this.mStatus);
                        BackupManagerService.this.mBackupHandler.removeMessages(7);
                    }
                    if (this.mStatus == -1001) {
                        EventLog.writeEvent(EventLogTags.BACKUP_RESET, this.mTransport.transportDirName());
                    }
                    BackupManagerService.this.addBackupTrace("exiting prelim: " + this.mStatus);
                    if (this.mStatus == 0) {
                        return;
                    }
                    BackupManagerService.this.resetBackupState(this.mStateDir);
                    BackupManagerService.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    executeNextState(BackupState.FINAL);
                } catch (Exception e) {
                    Slog.e(TAG, "Error in backup thread", e);
                    BackupManagerService.this.addBackupTrace("Exception in backup thread: " + e);
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    BackupManagerService.this.addBackupTrace("exiting prelim: " + this.mStatus);
                    if (this.mStatus == 0) {
                        return;
                    }
                    BackupManagerService.this.resetBackupState(this.mStateDir);
                    BackupManagerService.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    executeNextState(BackupState.FINAL);
                }
            } catch (Throwable th) {
                BackupManagerService.this.addBackupTrace("exiting prelim: " + this.mStatus);
                if (this.mStatus != 0) {
                    BackupManagerService.this.resetBackupState(this.mStateDir);
                    BackupManagerService.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    executeNextState(BackupState.FINAL);
                }
                throw th;
            }
        }

        void invokeNextAgent() {
            this.mStatus = 0;
            BackupManagerService.this.addBackupTrace("invoke q=" + this.mQueue.size());
            if (this.mQueue.isEmpty()) {
                executeNextState(BackupState.FINAL);
                return;
            }
            BackupRequest request = this.mQueue.get(0);
            this.mQueue.remove(0);
            Slog.d(TAG, "starting key/value backup of " + request);
            BackupManagerService.this.addBackupTrace("launch agent for " + request.packageName);
            try {
                try {
                    this.mCurrentPackage = BackupManagerService.this.mPackageManager.getPackageInfo(request.packageName, 64);
                    if (!BackupManagerService.appIsEligibleForBackup(this.mCurrentPackage.applicationInfo)) {
                        Slog.i(TAG, "Package " + request.packageName + " no longer supports backup; skipping");
                        BackupManagerService.this.addBackupTrace("skipping - not eligible, completion is noop");
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                        executeNextState(BackupState.RUNNING_QUEUE);
                        BackupManagerService.this.mWakelock.setWorkSource(null);
                        if (this.mStatus == 0) {
                            BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                            return;
                        }
                        BackupState nextState = BackupState.RUNNING_QUEUE;
                        this.mAgentBinder = null;
                        if (this.mStatus == -1003) {
                            BackupManagerService.this.dataChangedImpl(request.packageName);
                            this.mStatus = 0;
                            if (this.mQueue.isEmpty()) {
                                nextState = BackupState.FINAL;
                            }
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                        } else if (this.mStatus == -1004) {
                            this.mStatus = 0;
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                        } else {
                            revertAndEndBackup();
                            nextState = BackupState.FINAL;
                        }
                        executeNextState(nextState);
                        return;
                    }
                    if (BackupManagerService.appGetsFullBackup(this.mCurrentPackage)) {
                        Slog.i(TAG, "Package " + request.packageName + " requests full-data rather than key/value; skipping");
                        BackupManagerService.this.addBackupTrace("skipping - fullBackupOnly, completion is noop");
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                        executeNextState(BackupState.RUNNING_QUEUE);
                        BackupManagerService.this.mWakelock.setWorkSource(null);
                        if (this.mStatus == 0) {
                            BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                            return;
                        }
                        BackupState nextState2 = BackupState.RUNNING_QUEUE;
                        this.mAgentBinder = null;
                        if (this.mStatus == -1003) {
                            BackupManagerService.this.dataChangedImpl(request.packageName);
                            this.mStatus = 0;
                            if (this.mQueue.isEmpty()) {
                                nextState2 = BackupState.FINAL;
                            }
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                        } else if (this.mStatus == -1004) {
                            this.mStatus = 0;
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                        } else {
                            revertAndEndBackup();
                            nextState2 = BackupState.FINAL;
                        }
                        executeNextState(nextState2);
                        return;
                    }
                    if (BackupManagerService.appIsStopped(this.mCurrentPackage.applicationInfo)) {
                        BackupManagerService.this.addBackupTrace("skipping - stopped");
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                        executeNextState(BackupState.RUNNING_QUEUE);
                        BackupManagerService.this.mWakelock.setWorkSource(null);
                        if (this.mStatus == 0) {
                            BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                            return;
                        }
                        BackupState nextState3 = BackupState.RUNNING_QUEUE;
                        this.mAgentBinder = null;
                        if (this.mStatus == -1003) {
                            BackupManagerService.this.dataChangedImpl(request.packageName);
                            this.mStatus = 0;
                            if (this.mQueue.isEmpty()) {
                                nextState3 = BackupState.FINAL;
                            }
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                        } else if (this.mStatus == -1004) {
                            this.mStatus = 0;
                            BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                        } else {
                            revertAndEndBackup();
                            nextState3 = BackupState.FINAL;
                        }
                        executeNextState(nextState3);
                        return;
                    }
                    try {
                        BackupManagerService.this.mWakelock.setWorkSource(new WorkSource(this.mCurrentPackage.applicationInfo.uid));
                        IBackupAgent agent = BackupManagerService.this.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
                        BackupManagerService.this.addBackupTrace("agent bound; a? = " + (agent != null));
                        if (agent != null) {
                            this.mAgentBinder = agent;
                            this.mStatus = invokeAgentForBackup(request.packageName, agent, this.mTransport);
                        } else {
                            this.mStatus = -1003;
                        }
                    } catch (SecurityException ex) {
                        Slog.d(TAG, "error in bind/backup", ex);
                        this.mStatus = -1003;
                        BackupManagerService.this.addBackupTrace("agent SE");
                    }
                    BackupManagerService.this.mWakelock.setWorkSource(null);
                    if (this.mStatus == 0) {
                        BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                        return;
                    }
                    BackupState nextState4 = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        BackupManagerService.this.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState4 = BackupState.FINAL;
                        }
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState4 = BackupState.FINAL;
                    }
                    executeNextState(nextState4);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.d(TAG, "Package does not exist; skipping");
                    BackupManagerService.this.addBackupTrace("no such package");
                    this.mStatus = -1004;
                    BackupManagerService.this.mWakelock.setWorkSource(null);
                    if (this.mStatus == 0) {
                        BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                        return;
                    }
                    BackupState nextState5 = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        BackupManagerService.this.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState5 = BackupState.FINAL;
                        }
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState5 = BackupState.FINAL;
                    }
                    executeNextState(nextState5);
                }
            } catch (Throwable th) {
                BackupManagerService.this.mWakelock.setWorkSource(null);
                if (this.mStatus != 0) {
                    BackupState nextState6 = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        BackupManagerService.this.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState6 = BackupState.FINAL;
                        }
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState6 = BackupState.FINAL;
                    }
                    executeNextState(nextState6);
                } else {
                    BackupManagerService.this.addBackupTrace("expecting completion/timeout callback");
                }
                throw th;
            }
        }

        void finalizeBackup() {
            BackupManagerService.this.addBackupTrace("finishing");
            if (this.mJournal != null && !this.mJournal.delete()) {
                Slog.e(TAG, "Unable to remove backup journal file " + this.mJournal);
            }
            if (BackupManagerService.this.mCurrentToken == 0 && this.mStatus == 0) {
                BackupManagerService.this.addBackupTrace("success; recording token");
                try {
                    BackupManagerService.this.mCurrentToken = this.mTransport.getCurrentRestoreSet();
                    BackupManagerService.this.writeRestoreTokens();
                } catch (RemoteException e) {
                    BackupManagerService.this.addBackupTrace("transport threw returning token");
                }
            }
            synchronized (BackupManagerService.this.mQueueLock) {
                BackupManagerService.this.mBackupRunning = false;
                if (this.mStatus == -1001) {
                    BackupManagerService.this.addBackupTrace("init required; rerunning");
                    try {
                        String name = BackupManagerService.this.getTransportName(this.mTransport);
                        if (name != null) {
                            BackupManagerService.this.mPendingInits.add(name);
                        } else {
                            Slog.w(TAG, "Couldn't find name of transport " + this.mTransport + " for init");
                        }
                    } catch (Exception e2) {
                        Slog.w(TAG, "Failed to query transport name heading for init", e2);
                    }
                    clearMetadata();
                    BackupManagerService.this.backupNow();
                }
            }
            BackupManagerService.this.clearBackupTrace();
            if (this.mStatus == 0 && this.mPendingFullBackups != null && !this.mPendingFullBackups.isEmpty()) {
                Slog.d(TAG, "Starting full backups for: " + this.mPendingFullBackups);
                CountDownLatch latch = new CountDownLatch(1);
                String[] fullBackups = (String[]) this.mPendingFullBackups.toArray(new String[this.mPendingFullBackups.size()]);
                PerformFullTransportBackupTask task = BackupManagerService.this.new PerformFullTransportBackupTask(null, fullBackups, false, null, latch, this.mObserver, this.mUserInitiated);
                BackupManagerService.this.mWakelock.acquire();
                new Thread(task, "full-transport-requested").start();
            } else {
                switch (this.mStatus) {
                    case JobSchedulerShellCommand.CMD_ERR_NO_JOB:
                        BackupManagerService.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                    case 0:
                        BackupManagerService.sendBackupFinished(this.mObserver, 0);
                        break;
                    default:
                        BackupManagerService.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                }
            }
            Slog.i(BackupManagerService.TAG, "K/V backup pass finished.");
            BackupManagerService.this.mWakelock.release();
        }

        void clearMetadata() {
            File pmState = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
            if (pmState.exists()) {
                pmState.delete();
            }
        }

        int invokeAgentForBackup(String packageName, IBackupAgent agent, IBackupTransport transport) {
            Slog.d(TAG, "invokeAgentForBackup on " + packageName);
            BackupManagerService.this.addBackupTrace("invoking " + packageName);
            this.mSavedStateName = new File(this.mStateDir, packageName);
            this.mBackupDataName = new File(BackupManagerService.this.mDataDir, packageName + ".data");
            this.mNewStateName = new File(this.mStateDir, packageName + ".new");
            this.mSavedState = null;
            this.mBackupData = null;
            this.mNewState = null;
            int token = BackupManagerService.this.generateToken();
            try {
                if (packageName.equals(BackupManagerService.PACKAGE_MANAGER_SENTINEL)) {
                    this.mCurrentPackage = new PackageInfo();
                    this.mCurrentPackage.packageName = packageName;
                }
                this.mSavedState = ParcelFileDescriptor.open(this.mSavedStateName, 402653184);
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                if (!SELinux.restorecon(this.mBackupDataName)) {
                    Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
                }
                this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
                BackupManagerService.this.addBackupTrace("setting timeout");
                BackupManagerService.this.prepareOperationTimeout(token, 30000L, this);
                BackupManagerService.this.addBackupTrace("calling agent doBackup()");
                agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, token, BackupManagerService.this.mBackupManagerBinder);
                BackupManagerService.this.addBackupTrace("invoke success");
                return 0;
            } catch (Exception e) {
                Slog.e(TAG, "Error invoking for backup on " + packageName);
                BackupManagerService.this.addBackupTrace("exception: " + e);
                EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, packageName, e.toString());
                agentErrorCleanup();
                return -1003;
            }
        }

        public void failAgent(IBackupAgent agent, String message) {
            try {
                agent.fail(message);
            } catch (Exception e) {
                Slog.w(TAG, "Error conveying failure to " + this.mCurrentPackage.packageName);
            }
        }

        private String SHA1Checksum(byte[] input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] checksum = md.digest(input);
                StringBuffer sb = new StringBuffer(checksum.length * 2);
                for (byte b : checksum) {
                    sb.append(Integer.toHexString(b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                Slog.e(TAG, "Unable to use SHA-1!");
                return "00";
            }
        }

        private void writeWidgetPayloadIfAppropriate(FileDescriptor fd, String pkgName) throws Throwable {
            FileOutputStream fout;
            DataOutputStream stateOut;
            FileInputStream fin;
            DataInputStream in;
            byte[] widgetState = AppWidgetBackupBridge.getWidgetState(pkgName, 0);
            File widgetFile = new File(this.mStateDir, pkgName + "_widget");
            boolean priorStateExists = widgetFile.exists();
            if (!priorStateExists && widgetState == null) {
                return;
            }
            String newChecksum = null;
            if (widgetState != null) {
                newChecksum = SHA1Checksum(widgetState);
                if (priorStateExists) {
                    Throwable th = null;
                    FileInputStream fin2 = null;
                    DataInputStream in2 = null;
                    try {
                        fin = new FileInputStream(widgetFile);
                        try {
                            in = new DataInputStream(fin);
                        } catch (Throwable th2) {
                            th = th2;
                            fin2 = fin;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                    try {
                        String priorChecksum = in.readUTF();
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (Throwable th5) {
                                th = th5;
                                if (th != null) {
                                    if (th != th) {
                                        th.addSuppressed(th);
                                        th = th;
                                    }
                                }
                                if (th != null) {
                                }
                            }
                            th = th;
                            if (th != null) {
                                throw th;
                            }
                            if (Objects.equals(newChecksum, priorChecksum)) {
                                return;
                            }
                        } else {
                            th = th;
                            if (th != null) {
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        in2 = in;
                        fin2 = fin;
                        if (in2 == null) {
                        }
                        if (fin2 != null) {
                        }
                        if (th != null) {
                        }
                    }
                }
            }
            BackupDataOutput out = new BackupDataOutput(fd);
            if (widgetState != null) {
                Throwable th7 = null;
                FileOutputStream fout2 = null;
                DataOutputStream stateOut2 = null;
                try {
                    fout = new FileOutputStream(widgetFile);
                    try {
                        stateOut = new DataOutputStream(fout);
                    } catch (Throwable th8) {
                        th = th8;
                        fout2 = fout;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
                try {
                    stateOut.writeUTF(newChecksum);
                    if (stateOut != null) {
                        try {
                            stateOut.close();
                        } catch (Throwable th10) {
                            th7 = th10;
                        }
                    }
                    if (fout != null) {
                        try {
                            fout.close();
                        } catch (Throwable th11) {
                            th = th11;
                            if (th7 != null) {
                                if (th7 != th) {
                                    th7.addSuppressed(th);
                                    th = th7;
                                }
                            }
                            if (th == null) {
                            }
                        }
                        th = th7;
                    } else {
                        th = th7;
                    }
                    if (th == null) {
                        throw th;
                    }
                    out.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, widgetState.length);
                    out.writeEntityData(widgetState, widgetState.length);
                } catch (Throwable th12) {
                    th = th12;
                    stateOut2 = stateOut;
                    fout2 = fout;
                    try {
                        throw th;
                    } catch (Throwable th13) {
                        th7 = th;
                        th = th13;
                        if (stateOut2 == null) {
                            try {
                                stateOut2.close();
                            } catch (Throwable th14) {
                                th = th14;
                                if (th7 != null) {
                                    if (th7 != th) {
                                        th7.addSuppressed(th);
                                        th = th7;
                                    }
                                }
                                if (fout2 != null) {
                                    try {
                                        fout2.close();
                                    } catch (Throwable th15) {
                                        th = th15;
                                        if (th != null) {
                                            if (th != th) {
                                                th.addSuppressed(th);
                                                th = th;
                                            }
                                        }
                                        if (th != null) {
                                            throw th;
                                        }
                                        throw th;
                                    }
                                    th = th;
                                } else {
                                    th = th;
                                }
                                if (th != null) {
                                }
                            }
                            th = th7;
                        } else {
                            th = th7;
                        }
                        if (fout2 != null) {
                        }
                        if (th != null) {
                        }
                    }
                }
            } else {
                out.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, -1);
                widgetFile.delete();
            }
        }

        @Override
        public void operationComplete(long unusedResult) throws Throwable {
            BackupState nextState;
            if (this.mBackupData == null) {
                String pkg = this.mCurrentPackage != null ? this.mCurrentPackage.packageName : "[none]";
                BackupManagerService.this.addBackupTrace("late opComplete; curPkg = " + pkg);
                return;
            }
            String pkgName = this.mCurrentPackage.packageName;
            long filepos = this.mBackupDataName.length();
            FileDescriptor fd = this.mBackupData.getFileDescriptor();
            try {
                if (this.mCurrentPackage.applicationInfo != null && (this.mCurrentPackage.applicationInfo.flags & 1) == 0) {
                    ParcelFileDescriptor readFd = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                    BackupDataInput in = new BackupDataInput(readFd.getFileDescriptor());
                    while (in.readNextHeader()) {
                        try {
                            String key = in.getKey();
                            if (key != null && key.charAt(0) >= 65280) {
                                failAgent(this.mAgentBinder, "Illegal backup key: " + key);
                                BackupManagerService.this.addBackupTrace("illegal key " + key + " from " + pkgName);
                                EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, pkgName, "bad key");
                                BackupManagerService.this.mBackupHandler.removeMessages(7);
                                BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, -1003);
                                agentErrorCleanup();
                                if (readFd != null) {
                                    return;
                                } else {
                                    return;
                                }
                            }
                            in.skipEntityData();
                        } finally {
                            if (readFd != null) {
                                readFd.close();
                            }
                        }
                    }
                    if (readFd != null) {
                        readFd.close();
                    }
                }
                writeWidgetPayloadIfAppropriate(fd, pkgName);
            } catch (IOException e) {
                Slog.w(TAG, "Unable to save widget state for " + pkgName);
                try {
                    Os.ftruncate(fd, filepos);
                } catch (ErrnoException e2) {
                    Slog.w(TAG, "Unable to roll back!");
                }
            }
            BackupManagerService.this.mBackupHandler.removeMessages(7);
            clearAgentState();
            BackupManagerService.this.addBackupTrace("operation complete");
            ParcelFileDescriptor backupData = null;
            this.mStatus = 0;
            long size = 0;
            try {
                try {
                    size = this.mBackupDataName.length();
                    if (size > 0) {
                        if (this.mStatus == 0) {
                            backupData = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                            BackupManagerService.this.addBackupTrace("sending data to transport");
                            int flags = this.mUserInitiated ? 1 : 0;
                            this.mStatus = this.mTransport.performBackup(this.mCurrentPackage, backupData, flags);
                        }
                        BackupManagerService.this.addBackupTrace("data delivered: " + this.mStatus);
                        if (this.mStatus == 0) {
                            BackupManagerService.this.addBackupTrace("finishing op on transport");
                            this.mStatus = this.mTransport.finishBackup();
                            BackupManagerService.this.addBackupTrace("finished: " + this.mStatus);
                        } else if (this.mStatus == -1002) {
                            BackupManagerService.this.addBackupTrace("transport rejected package");
                        }
                    } else {
                        BackupManagerService.this.addBackupTrace("no data to send");
                    }
                    if (this.mStatus == 0) {
                        this.mBackupDataName.delete();
                        this.mNewStateName.renameTo(this.mSavedStateName);
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, 0);
                        EventLog.writeEvent(EventLogTags.BACKUP_PACKAGE, pkgName, Long.valueOf(size));
                        BackupManagerService.this.logBackupComplete(pkgName);
                    } else if (this.mStatus == -1002) {
                        this.mBackupDataName.delete();
                        this.mNewStateName.delete();
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                        EventLogTags.writeBackupAgentFailure(pkgName, "Transport rejected");
                    } else if (this.mStatus == -1005) {
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, -1005);
                        EventLog.writeEvent(EventLogTags.BACKUP_QUOTA_EXCEEDED, pkgName);
                    } else {
                        BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                    }
                    if (backupData != null) {
                        try {
                            backupData.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            backupData.close();
                        } catch (IOException e4) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e5) {
                BackupManagerService.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                Slog.e(TAG, "Transport error backing up " + pkgName, e5);
                EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                if (0 != 0) {
                    try {
                        backupData.close();
                    } catch (IOException e6) {
                    }
                }
            }
            if (this.mStatus == 0 || this.mStatus == -1002) {
                nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
            } else if (this.mStatus == -1005) {
                if (this.mAgentBinder != null) {
                    try {
                        long quota = this.mTransport.getBackupQuota(this.mCurrentPackage.packageName, false);
                        this.mAgentBinder.doQuotaExceeded(size, quota);
                    } catch (RemoteException e7) {
                        Slog.e(TAG, "Unable to contact backup agent for quota exceeded");
                    }
                }
                nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
            } else {
                revertAndEndBackup();
                nextState = BackupState.FINAL;
            }
            executeNextState(nextState);
        }

        @Override
        public void handleTimeout() {
            Slog.e(TAG, "Timeout backing up " + this.mCurrentPackage.packageName);
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, this.mCurrentPackage.packageName, "timeout");
            BackupManagerService.this.addBackupTrace("timeout of " + this.mCurrentPackage.packageName);
            agentErrorCleanup();
            BackupManagerService.this.dataChangedImpl(this.mCurrentPackage.packageName);
        }

        void revertAndEndBackup() {
            long delay;
            BackupManagerService.this.addBackupTrace("transport error; reverting");
            try {
                delay = this.mTransport.requestBackupTime();
            } catch (Exception e) {
                Slog.w(TAG, "Unable to contact transport for recommended backoff");
                delay = 0;
            }
            KeyValueBackupJob.schedule(BackupManagerService.this.mContext, delay);
            for (BackupRequest request : this.mOriginalQueue) {
                BackupManagerService.this.dataChangedImpl(request.packageName);
            }
        }

        void agentErrorCleanup() {
            this.mBackupDataName.delete();
            this.mNewStateName.delete();
            clearAgentState();
            executeNextState(this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE);
        }

        void clearAgentState() {
            try {
                if (this.mSavedState != null) {
                    this.mSavedState.close();
                }
            } catch (IOException e) {
            }
            try {
                if (this.mBackupData != null) {
                    this.mBackupData.close();
                }
            } catch (IOException e2) {
            }
            try {
                if (this.mNewState != null) {
                    this.mNewState.close();
                }
            } catch (IOException e3) {
            }
            synchronized (BackupManagerService.this.mCurrentOpLock) {
                BackupManagerService.this.mCurrentOperations.clear();
                this.mNewState = null;
                this.mBackupData = null;
                this.mSavedState = null;
            }
            if (this.mCurrentPackage.applicationInfo == null) {
                return;
            }
            BackupManagerService.this.addBackupTrace("unbinding " + this.mCurrentPackage.packageName);
            try {
                BackupManagerService.this.mActivityManager.unbindBackupAgent(this.mCurrentPackage.applicationInfo);
            } catch (RemoteException e4) {
            }
        }

        void executeNextState(BackupState nextState) {
            BackupManagerService.this.addBackupTrace("executeNextState => " + nextState);
            this.mCurrentState = nextState;
            Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(20, this);
            BackupManagerService.this.mBackupHandler.sendMessage(msg);
        }
    }

    class FullBackupObbConnection implements ServiceConnection {
        volatile IObbBackupService mService = null;

        FullBackupObbConnection() {
        }

        public void establish() {
            Intent obbIntent = new Intent().setComponent(new ComponentName(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, "com.android.sharedstoragebackup.ObbBackupService"));
            BackupManagerService.this.mContext.bindServiceAsUser(obbIntent, this, 1, UserHandle.SYSTEM);
        }

        public void tearDown() {
            BackupManagerService.this.mContext.unbindService(this);
        }

        public boolean backupObbs(PackageInfo pkg, OutputStream out) {
            boolean success = false;
            waitForConnection();
            ParcelFileDescriptor[] pipes = null;
            try {
                try {
                    pipes = ParcelFileDescriptor.createPipe();
                    int token = BackupManagerService.this.generateToken();
                    BackupManagerService.this.prepareOperationTimeout(token, 300000L, null);
                    this.mService.backupObbs(pkg.packageName, pipes[1], token, BackupManagerService.this.mBackupManagerBinder);
                    BackupManagerService.this.routeSocketDataToOutput(pipes[0], out);
                    success = BackupManagerService.this.waitUntilOperationComplete(token);
                } catch (Exception e) {
                    Slog.w(BackupManagerService.TAG, "Unable to back up OBBs for " + pkg, e);
                    try {
                        out.flush();
                        if (pipes != null) {
                            if (pipes[0] != null) {
                                pipes[0].close();
                            }
                            if (pipes[1] != null) {
                                pipes[1].close();
                            }
                        }
                    } catch (IOException e2) {
                        Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e2);
                    }
                }
                return success;
            } finally {
                try {
                    out.flush();
                    if (pipes != null) {
                        if (pipes[(char) 0] != null) {
                            pipes[(char) 0].close();
                        }
                        if (pipes[(char) 1] != null) {
                            pipes[(char) 1].close();
                        }
                    }
                } catch (IOException e3) {
                    Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e3);
                }
            }
        }

        public void restoreObbFile(String pkgName, ParcelFileDescriptor data, long fileSize, int type, String path, long mode, long mtime, int token, IBackupManager callbackBinder) {
            waitForConnection();
            try {
                this.mService.restoreObbFile(pkgName, data, fileSize, type, path, mode, mtime, token, callbackBinder);
            } catch (Exception e) {
                Slog.w(BackupManagerService.TAG, "Unable to restore OBBs for " + pkgName, e);
            }
        }

        private void waitForConnection() {
            synchronized (this) {
                while (this.mService == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                this.mService = IObbBackupService.Stub.asInterface(service);
                notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (this) {
                this.mService = null;
                notifyAll();
            }
        }
    }

    private void routeSocketDataToOutput(ParcelFileDescriptor inPipe, OutputStream out) throws IOException {
        FileInputStream raw = new FileInputStream(inPipe.getFileDescriptor());
        DataInputStream in = new DataInputStream(raw);
        byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
        while (true) {
            int chunkTotal = in.readInt();
            if (chunkTotal <= 0) {
                return;
            }
            while (chunkTotal > 0) {
                int toRead = chunkTotal > buffer.length ? buffer.length : chunkTotal;
                int nRead = in.read(buffer, 0, toRead);
                out.write(buffer, 0, nRead);
                chunkTotal -= nRead;
            }
        }
    }

    void tearDownAgentAndKill(ApplicationInfo app) {
        if (app == null) {
            return;
        }
        try {
            this.mActivityManager.unbindBackupAgent(app);
            if (app.uid < 10000 || app.packageName.equals("com.android.backupconfirm")) {
                return;
            }
            this.mActivityManager.killApplicationProcess(app.processName, app.uid);
        } catch (RemoteException e) {
            Slog.d(TAG, "Lost app trying to shut down");
        }
    }

    class FullBackupEngine {
        IBackupAgent mAgent;
        boolean mIncludeApks;
        OutputStream mOutput;
        PackageInfo mPkg;
        FullBackupPreflight mPreflightHook;
        BackupRestoreTask mTimeoutMonitor;
        File mFilesDir = new File("/data/system");
        File mManifestFile = new File(this.mFilesDir, BackupManagerService.BACKUP_MANIFEST_FILENAME);
        File mMetadataFile = new File(this.mFilesDir, BackupManagerService.BACKUP_METADATA_FILENAME);

        class FullBackupRunner implements Runnable {
            IBackupAgent mAgent;
            PackageInfo mPackage;
            ParcelFileDescriptor mPipe;
            boolean mSendApk;
            int mToken;
            byte[] mWidgetData;
            boolean mWriteManifest;

            FullBackupRunner(PackageInfo pack, IBackupAgent agent, ParcelFileDescriptor pipe, int token, boolean sendApk, boolean writeManifest, byte[] widgetData) throws IOException {
                this.mPackage = pack;
                this.mWidgetData = widgetData;
                this.mAgent = agent;
                this.mPipe = ParcelFileDescriptor.dup(pipe.getFileDescriptor());
                this.mToken = token;
                this.mSendApk = sendApk;
                this.mWriteManifest = writeManifest;
            }

            @Override
            public void run() {
                try {
                    try {
                        FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe);
                        if (this.mWriteManifest) {
                            boolean writeWidgetData = this.mWidgetData != null;
                            FullBackupEngine.this.writeAppManifest(this.mPackage, FullBackupEngine.this.mManifestFile, this.mSendApk, writeWidgetData);
                            FullBackup.backupToTar(this.mPackage.packageName, (String) null, (String) null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mManifestFile.getAbsolutePath(), output);
                            FullBackupEngine.this.mManifestFile.delete();
                            if (writeWidgetData) {
                                FullBackupEngine.this.writeMetadata(this.mPackage, FullBackupEngine.this.mMetadataFile, this.mWidgetData);
                                FullBackup.backupToTar(this.mPackage.packageName, (String) null, (String) null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mMetadataFile.getAbsolutePath(), output);
                                FullBackupEngine.this.mMetadataFile.delete();
                            }
                        }
                        if (this.mSendApk) {
                            FullBackupEngine.this.writeApkToBackup(this.mPackage, output);
                        }
                        Slog.d(BackupManagerService.TAG, "Calling doFullBackup() on " + this.mPackage.packageName);
                        BackupManagerService.this.prepareOperationTimeout(this.mToken, 300000L, FullBackupEngine.this.mTimeoutMonitor);
                        this.mAgent.doFullBackup(this.mPipe, this.mToken, BackupManagerService.this.mBackupManagerBinder);
                    } catch (RemoteException e) {
                        Slog.e(BackupManagerService.TAG, "Remote agent vanished during full backup of " + this.mPackage.packageName);
                        try {
                            this.mPipe.close();
                        } catch (IOException e2) {
                        }
                    } catch (IOException e3) {
                        Slog.e(BackupManagerService.TAG, "Error running full backup for " + this.mPackage.packageName);
                        try {
                            this.mPipe.close();
                        } catch (IOException e4) {
                        }
                    }
                } finally {
                    try {
                        this.mPipe.close();
                    } catch (IOException e5) {
                    }
                }
            }
        }

        FullBackupEngine(OutputStream output, FullBackupPreflight preflightHook, PackageInfo pkg, boolean alsoApks, BackupRestoreTask timeoutMonitor) {
            this.mOutput = output;
            this.mPreflightHook = preflightHook;
            this.mPkg = pkg;
            this.mIncludeApks = alsoApks;
            this.mTimeoutMonitor = timeoutMonitor;
        }

        public int preflightCheck() throws RemoteException {
            if (this.mPreflightHook == null) {
                return 0;
            }
            if (initializeAgent()) {
                int result = this.mPreflightHook.preflightFullBackup(this.mPkg, this.mAgent);
                return result;
            }
            Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
            return -1003;
        }

        public int backupOnePackage() throws RemoteException {
            boolean sendApk;
            int result = -1003;
            if (initializeAgent()) {
                ParcelFileDescriptor[] pipes = null;
                try {
                    try {
                        pipes = ParcelFileDescriptor.createPipe();
                        ApplicationInfo app = this.mPkg.applicationInfo;
                        boolean isSharedStorage = this.mPkg.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                        if (!this.mIncludeApks || isSharedStorage || (app.privateFlags & 4) != 0) {
                            sendApk = false;
                        } else {
                            sendApk = (app.flags & 1) == 0 || (app.flags & 128) != 0;
                        }
                        byte[] widgetBlob = AppWidgetBackupBridge.getWidgetState(this.mPkg.packageName, 0);
                        int token = BackupManagerService.this.generateToken();
                        FullBackupRunner runner = new FullBackupRunner(this.mPkg, this.mAgent, pipes[1], token, sendApk, isSharedStorage ? false : true, widgetBlob);
                        pipes[1].close();
                        pipes[1] = null;
                        Thread t = new Thread(runner, "app-data-runner");
                        t.start();
                        BackupManagerService.this.routeSocketDataToOutput(pipes[0], this.mOutput);
                        if (!BackupManagerService.this.waitUntilOperationComplete(token)) {
                            Slog.e(BackupManagerService.TAG, "Full backup failed on package " + this.mPkg.packageName);
                        } else {
                            result = 0;
                        }
                        try {
                            this.mOutput.flush();
                            if (pipes != null) {
                                if (pipes[0] != null) {
                                    pipes[0].close();
                                }
                                if (pipes[1] != null) {
                                    pipes[1].close();
                                }
                            }
                        } catch (IOException e) {
                            Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                            result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        }
                    } catch (IOException e2) {
                        Slog.e(BackupManagerService.TAG, "Error backing up " + this.mPkg.packageName + ": " + e2.getMessage());
                        result = -1003;
                        try {
                            this.mOutput.flush();
                            if (pipes != null) {
                                if (pipes[0] != null) {
                                    pipes[0].close();
                                }
                                if (pipes[1] != null) {
                                    pipes[1].close();
                                }
                            }
                        } catch (IOException e3) {
                            Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                            result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        }
                    }
                } catch (Throwable th) {
                    try {
                        this.mOutput.flush();
                        if (pipes != null) {
                            if (pipes[0] != null) {
                                pipes[0].close();
                            }
                            if (pipes[1] != null) {
                                pipes[1].close();
                            }
                        }
                    } catch (IOException e4) {
                        Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                    }
                    throw th;
                }
            } else {
                Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
            }
            tearDown();
            return result;
        }

        public void sendQuotaExceeded(long backupDataBytes, long quotaBytes) {
            if (!initializeAgent()) {
                return;
            }
            try {
                this.mAgent.doQuotaExceeded(backupDataBytes, quotaBytes);
            } catch (RemoteException e) {
                Slog.e(BackupManagerService.TAG, "Remote exception while telling agent about quota exceeded");
            }
        }

        private boolean initializeAgent() {
            if (this.mAgent == null) {
                this.mAgent = BackupManagerService.this.bindToAgentSynchronous(this.mPkg.applicationInfo, 1);
            }
            return this.mAgent != null;
        }

        private void writeApkToBackup(PackageInfo pkg, FullBackupDataOutput output) {
            File[] obbFiles;
            String appSourceDir = pkg.applicationInfo.getBaseCodePath();
            String apkDir = new File(appSourceDir).getParent();
            FullBackup.backupToTar(pkg.packageName, "a", (String) null, apkDir, appSourceDir, output);
            Environment.UserEnvironment userEnv = new Environment.UserEnvironment(0);
            File obbDir = userEnv.buildExternalStorageAppObbDirs(pkg.packageName)[0];
            if (obbDir == null || (obbFiles = obbDir.listFiles()) == null) {
                return;
            }
            String obbDirName = obbDir.getAbsolutePath();
            for (File obb : obbFiles) {
                FullBackup.backupToTar(pkg.packageName, "obb", (String) null, obbDirName, obb.getAbsolutePath(), output);
            }
        }

        private void writeAppManifest(PackageInfo pkg, File manifestFile, boolean withApk, boolean withWidgets) throws IOException {
            StringBuilder builder = new StringBuilder(4096);
            StringBuilderPrinter printer = new StringBuilderPrinter(builder);
            printer.println(Integer.toString(1));
            printer.println(pkg.packageName);
            printer.println(Integer.toString(pkg.versionCode));
            printer.println(Integer.toString(Build.VERSION.SDK_INT));
            String installerName = BackupManagerService.this.mPackageManager.getInstallerPackageName(pkg.packageName);
            if (installerName == null) {
                installerName = "";
            }
            printer.println(installerName);
            printer.println(withApk ? "1" : "0");
            if (pkg.signatures == null) {
                printer.println("0");
            } else {
                printer.println(Integer.toString(pkg.signatures.length));
                for (Signature sig : pkg.signatures) {
                    printer.println(sig.toCharsString());
                }
            }
            FileOutputStream outstream = new FileOutputStream(manifestFile);
            outstream.write(builder.toString().getBytes());
            outstream.close();
            manifestFile.setLastModified(0L);
        }

        private void writeMetadata(PackageInfo pkg, File destination, byte[] widgetData) throws IOException {
            StringBuilder b = new StringBuilder(512);
            StringBuilderPrinter printer = new StringBuilderPrinter(b);
            printer.println(Integer.toString(1));
            printer.println(pkg.packageName);
            FileOutputStream fout = new FileOutputStream(destination);
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            DataOutputStream out = new DataOutputStream(bout);
            bout.write(b.toString().getBytes());
            if (widgetData != null && widgetData.length > 0) {
                out.writeInt(BackupManagerService.BACKUP_WIDGET_METADATA_TOKEN);
                out.writeInt(widgetData.length);
                out.write(widgetData);
            }
            bout.flush();
            out.close();
            destination.setLastModified(0L);
        }

        private void tearDown() {
            if (this.mPkg == null) {
                return;
            }
            BackupManagerService.this.tearDownAgentAndKill(this.mPkg.applicationInfo);
        }
    }

    abstract class FullBackupTask implements Runnable {
        IFullBackupRestoreObserver mObserver;

        FullBackupTask(IFullBackupRestoreObserver observer) {
            this.mObserver = observer;
        }

        final void sendStartBackup() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onStartBackup();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: startBackup");
                this.mObserver = null;
            }
        }

        final void sendOnBackupPackage(String name) {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onBackupPackage(name);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: backupPackage");
                this.mObserver = null;
            }
        }

        final void sendEndBackup() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onEndBackup();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: endBackup");
                this.mObserver = null;
            }
        }
    }

    boolean deviceIsEncrypted() {
        try {
            if (this.mMountService.getEncryptionState() != 1) {
                return this.mMountService.getPasswordType() != 1;
            }
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "Unable to communicate with mount service: " + e.getMessage());
            return true;
        }
    }

    class PerformAdbBackupTask extends FullBackupTask implements BackupRestoreTask {
        boolean mAllApps;
        FullBackupEngine mBackupEngine;
        boolean mCompress;
        String mCurrentPassword;
        PackageInfo mCurrentTarget;
        DeflaterOutputStream mDeflater;
        boolean mDoWidgets;
        String mEncryptPassword;
        boolean mIncludeApks;
        boolean mIncludeObbs;
        boolean mIncludeShared;
        boolean mIncludeSystem;
        final AtomicBoolean mLatch;
        ParcelFileDescriptor mOutputFile;
        ArrayList<String> mPackages;

        PerformAdbBackupTask(ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, String curPassword, String encryptPassword, boolean doAllApps, boolean doSystem, boolean doCompress, String[] packages, AtomicBoolean latch) {
            ArrayList<String> arrayList;
            super(observer);
            this.mLatch = latch;
            this.mOutputFile = fd;
            this.mIncludeApks = includeApks;
            this.mIncludeObbs = includeObbs;
            this.mIncludeShared = includeShared;
            this.mDoWidgets = doWidgets;
            this.mAllApps = doAllApps;
            this.mIncludeSystem = doSystem;
            if (packages == null) {
                arrayList = new ArrayList<>();
            } else {
                arrayList = new ArrayList<>(Arrays.asList(packages));
            }
            this.mPackages = arrayList;
            this.mCurrentPassword = curPassword;
            if (encryptPassword == null || "".equals(encryptPassword)) {
                this.mEncryptPassword = curPassword;
            } else {
                this.mEncryptPassword = encryptPassword;
            }
            this.mCompress = doCompress;
        }

        void addPackagesToSet(TreeMap<String, PackageInfo> set, List<String> pkgNames) {
            for (String pkgName : pkgNames) {
                if (!set.containsKey(pkgName)) {
                    try {
                        PackageInfo info = BackupManagerService.this.mPackageManager.getPackageInfo(pkgName, 64);
                        set.put(pkgName, info);
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.w(BackupManagerService.TAG, "Unknown package " + pkgName + ", skipping");
                    }
                }
            }
        }

        private OutputStream emitAesBackupHeader(StringBuilder headerbuf, OutputStream ofstream) throws Exception {
            byte[] newUserSalt = BackupManagerService.this.randomBytes(512);
            SecretKey userKey = BackupManagerService.this.buildPasswordKey(BackupManagerService.PBKDF_CURRENT, this.mEncryptPassword, newUserSalt, 10000);
            byte[] masterPw = new byte[32];
            BackupManagerService.this.mRng.nextBytes(masterPw);
            byte[] checksumSalt = BackupManagerService.this.randomBytes(512);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec masterKeySpec = new SecretKeySpec(masterPw, "AES");
            c.init(1, masterKeySpec);
            OutputStream finalOutput = new CipherOutputStream(ofstream, c);
            headerbuf.append(BackupManagerService.ENCRYPTION_ALGORITHM_NAME);
            headerbuf.append('\n');
            headerbuf.append(BackupManagerService.this.byteArrayToHex(newUserSalt));
            headerbuf.append('\n');
            headerbuf.append(BackupManagerService.this.byteArrayToHex(checksumSalt));
            headerbuf.append('\n');
            headerbuf.append(10000);
            headerbuf.append('\n');
            Cipher mkC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            mkC.init(1, userKey);
            headerbuf.append(BackupManagerService.this.byteArrayToHex(mkC.getIV()));
            headerbuf.append('\n');
            byte[] IV = c.getIV();
            byte[] mk = masterKeySpec.getEncoded();
            byte[] checksum = BackupManagerService.this.makeKeyChecksum(BackupManagerService.PBKDF_CURRENT, masterKeySpec.getEncoded(), checksumSalt, 10000);
            ByteArrayOutputStream blob = new ByteArrayOutputStream(IV.length + mk.length + checksum.length + 3);
            DataOutputStream mkOut = new DataOutputStream(blob);
            mkOut.writeByte(IV.length);
            mkOut.write(IV);
            mkOut.writeByte(mk.length);
            mkOut.write(mk);
            mkOut.writeByte(checksum.length);
            mkOut.write(checksum);
            mkOut.flush();
            byte[] encryptedMk = mkC.doFinal(blob.toByteArray());
            headerbuf.append(BackupManagerService.this.byteArrayToHex(encryptedMk));
            headerbuf.append('\n');
            return finalOutput;
        }

        private void finalizeBackup(OutputStream out) {
            try {
                byte[] eof = new byte[1024];
                out.write(eof);
            } catch (IOException e) {
                Slog.w(BackupManagerService.TAG, "Error attempting to finalize backup stream");
            }
        }

        @Override
        public void run() {
            OutputStream finalOutput;
            OutputStream finalOutput2;
            List<String> pkgs;
            Slog.i(BackupManagerService.TAG, "--- Performing full-dataset adb backup ---");
            TreeMap<String, PackageInfo> packagesToBackup = new TreeMap<>();
            FullBackupObbConnection obbConnection = BackupManagerService.this.new FullBackupObbConnection();
            obbConnection.establish();
            sendStartBackup();
            if (this.mAllApps) {
                List<PackageInfo> allPackages = BackupManagerService.this.mPackageManager.getInstalledPackages(64);
                for (int i = 0; i < allPackages.size(); i++) {
                    PackageInfo pkg = allPackages.get(i);
                    if (this.mIncludeSystem || (pkg.applicationInfo.flags & 1) == 0) {
                        packagesToBackup.put(pkg.packageName, pkg);
                    }
                }
            }
            if (this.mDoWidgets && (pkgs = AppWidgetBackupBridge.getWidgetParticipants(0)) != null) {
                addPackagesToSet(packagesToBackup, pkgs);
            }
            if (this.mPackages != null) {
                addPackagesToSet(packagesToBackup, this.mPackages);
            }
            Iterator<Map.Entry<String, PackageInfo>> iter = packagesToBackup.entrySet().iterator();
            while (iter.hasNext()) {
                PackageInfo pkg2 = iter.next().getValue();
                if (!BackupManagerService.appIsEligibleForBackup(pkg2.applicationInfo) || BackupManagerService.appIsStopped(pkg2.applicationInfo) || BackupManagerService.appIsKeyValueOnly(pkg2)) {
                    iter.remove();
                }
            }
            ArrayList<PackageInfo> backupQueue = new ArrayList<>(packagesToBackup.values());
            FileOutputStream ofstream = new FileOutputStream(this.mOutputFile.getFileDescriptor());
            OutputStream outputStream = null;
            try {
                try {
                    try {
                        boolean encrypting = this.mEncryptPassword != null && this.mEncryptPassword.length() > 0;
                        if (BackupManagerService.this.deviceIsEncrypted() && !encrypting) {
                            Slog.e(BackupManagerService.TAG, "Unencrypted backup of encrypted device; aborting");
                            try {
                                this.mOutputFile.close();
                            } catch (IOException e) {
                            }
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                            }
                            synchronized (this.mLatch) {
                                this.mLatch.set(true);
                                this.mLatch.notifyAll();
                            }
                            sendEndBackup();
                            obbConnection.tearDown();
                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                            BackupManagerService.this.mWakelock.release();
                            return;
                        }
                        if (!BackupManagerService.this.backupPasswordMatches(this.mCurrentPassword)) {
                            Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
                            try {
                                this.mOutputFile.close();
                            } catch (IOException e2) {
                            }
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                            }
                            synchronized (this.mLatch) {
                                this.mLatch.set(true);
                                this.mLatch.notifyAll();
                            }
                            sendEndBackup();
                            obbConnection.tearDown();
                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                            BackupManagerService.this.mWakelock.release();
                            return;
                        }
                        StringBuilder headerbuf = new StringBuilder(1024);
                        headerbuf.append(BackupManagerService.BACKUP_FILE_HEADER_MAGIC);
                        headerbuf.append(4);
                        headerbuf.append(this.mCompress ? "\n1\n" : "\n0\n");
                        try {
                            if (encrypting) {
                                OutputStream finalOutput3 = emitAesBackupHeader(headerbuf, ofstream);
                                finalOutput = finalOutput3;
                            } else {
                                headerbuf.append("none\n");
                                finalOutput = ofstream;
                            }
                        } catch (Exception e3) {
                            e = e3;
                        }
                        try {
                            byte[] header = headerbuf.toString().getBytes("UTF-8");
                            ofstream.write(header);
                            if (this.mCompress) {
                                Deflater deflater = new Deflater(9);
                                finalOutput2 = new DeflaterOutputStream(finalOutput, deflater, true);
                            } else {
                                finalOutput2 = finalOutput;
                            }
                            OutputStream out = finalOutput2;
                            if (this.mIncludeShared) {
                                try {
                                    backupQueue.add(BackupManagerService.this.mPackageManager.getPackageInfo(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, 0));
                                } catch (PackageManager.NameNotFoundException e4) {
                                    Slog.e(BackupManagerService.TAG, "Unable to find shared-storage backup handler");
                                }
                            }
                            int N = backupQueue.size();
                            for (int i2 = 0; i2 < N; i2++) {
                                PackageInfo pkg3 = backupQueue.get(i2);
                                boolean isSharedStorage = pkg3.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                                this.mBackupEngine = BackupManagerService.this.new FullBackupEngine(out, null, pkg3, this.mIncludeApks, this);
                                sendOnBackupPackage(isSharedStorage ? "Shared storage" : pkg3.packageName);
                                this.mCurrentTarget = pkg3;
                                this.mBackupEngine.backupOnePackage();
                                if (this.mIncludeObbs) {
                                    boolean obbOkay = obbConnection.backupObbs(pkg3, out);
                                    if (!obbOkay) {
                                        throw new RuntimeException("Failure writing OBB stack for " + pkg3);
                                    }
                                }
                            }
                            finalizeBackup(out);
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException e5) {
                                }
                            }
                            this.mOutputFile.close();
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                            }
                            synchronized (this.mLatch) {
                                this.mLatch.set(true);
                                this.mLatch.notifyAll();
                            }
                            sendEndBackup();
                            obbConnection.tearDown();
                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                            BackupManagerService.this.mWakelock.release();
                        } catch (Exception e6) {
                            e = e6;
                            Slog.e(BackupManagerService.TAG, "Unable to emit archive header", e);
                            try {
                                this.mOutputFile.close();
                            } catch (IOException e7) {
                            }
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                            }
                            synchronized (this.mLatch) {
                                this.mLatch.set(true);
                                this.mLatch.notifyAll();
                            }
                            sendEndBackup();
                            obbConnection.tearDown();
                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                            BackupManagerService.this.mWakelock.release();
                        }
                    } catch (Exception e8) {
                        Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e8);
                        if (0 != 0) {
                            try {
                                outputStream.close();
                            } catch (IOException e9) {
                                synchronized (BackupManagerService.this.mCurrentOpLock) {
                                    BackupManagerService.this.mCurrentOperations.clear();
                                    synchronized (this.mLatch) {
                                        this.mLatch.set(true);
                                        this.mLatch.notifyAll();
                                        sendEndBackup();
                                        obbConnection.tearDown();
                                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                        BackupManagerService.this.mWakelock.release();
                                        return;
                                    }
                                }
                            }
                        }
                        this.mOutputFile.close();
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                        }
                    }
                } catch (RemoteException e10) {
                    Slog.e(BackupManagerService.TAG, "App died during full backup");
                    if (0 != 0) {
                        try {
                            outputStream.close();
                        } catch (IOException e11) {
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                                synchronized (this.mLatch) {
                                    this.mLatch.set(true);
                                    this.mLatch.notifyAll();
                                    sendEndBackup();
                                    obbConnection.tearDown();
                                    Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                    BackupManagerService.this.mWakelock.release();
                                    return;
                                }
                            }
                        }
                    }
                    this.mOutputFile.close();
                    synchronized (BackupManagerService.this.mCurrentOpLock) {
                    }
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        outputStream.close();
                    } catch (IOException e12) {
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                            BackupManagerService.this.mCurrentOperations.clear();
                            synchronized (this.mLatch) {
                                this.mLatch.set(true);
                                this.mLatch.notifyAll();
                                sendEndBackup();
                                obbConnection.tearDown();
                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                BackupManagerService.this.mWakelock.release();
                                throw th;
                            }
                        }
                    }
                }
                this.mOutputFile.close();
                synchronized (BackupManagerService.this.mCurrentOpLock) {
                }
            }
        }

        @Override
        public void execute() {
        }

        @Override
        public void operationComplete(long result) {
        }

        @Override
        public void handleTimeout() {
            PackageInfo target = this.mCurrentTarget;
            Slog.w(BackupManagerService.TAG, "adb backup timeout of " + target);
            if (target == null) {
                return;
            }
            BackupManagerService.this.tearDownAgentAndKill(this.mCurrentTarget.applicationInfo);
        }
    }

    class PerformFullTransportBackupTask extends FullBackupTask {
        static final String TAG = "PFTBT";
        IBackupObserver mBackupObserver;
        FullBackupJob mJob;
        AtomicBoolean mKeepRunning;
        CountDownLatch mLatch;
        ArrayList<PackageInfo> mPackages;
        boolean mUpdateSchedule;
        boolean mUserInitiated;

        PerformFullTransportBackupTask(IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, boolean userInitiated) {
            super(observer);
            this.mUpdateSchedule = updateSchedule;
            this.mLatch = latch;
            this.mKeepRunning = new AtomicBoolean(true);
            this.mJob = runningJob;
            this.mPackages = new ArrayList<>(whichPackages.length);
            this.mBackupObserver = backupObserver;
            this.mUserInitiated = userInitiated;
            for (String pkg : whichPackages) {
                try {
                    PackageInfo info = BackupManagerService.this.mPackageManager.getPackageInfo(pkg, 64);
                    if (!BackupManagerService.appIsEligibleForBackup(info.applicationInfo)) {
                        BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                    } else if (!BackupManagerService.appGetsFullBackup(info)) {
                        BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                    } else if (BackupManagerService.appIsStopped(info.applicationInfo)) {
                        BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                    } else {
                        this.mPackages.add(info);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.i(TAG, "Requested package " + pkg + " not found; ignoring");
                }
            }
        }

        public void setRunning(boolean running) {
            this.mKeepRunning.set(running);
        }

        @Override
        public void run() {
            int backupRunnerResult;
            ParcelFileDescriptor[] enginePipes = null;
            ParcelFileDescriptor[] transportPipes = null;
            long backoff = 0;
            try {
                try {
                    if (!BackupManagerService.this.mEnabled || !BackupManagerService.this.mProvisioned) {
                        Slog.i(TAG, "full backup requested but e=" + BackupManagerService.this.mEnabled + " p=" + BackupManagerService.this.mProvisioned + "; ignoring");
                        this.mUpdateSchedule = false;
                        Slog.i(TAG, "Full backup completed with status: -2001");
                        BackupManagerService.sendBackupFinished(this.mBackupObserver, -2001);
                        cleanUpPipes(null);
                        cleanUpPipes(null);
                        if (this.mJob != null) {
                            this.mJob.finishBackupPass();
                        }
                        synchronized (BackupManagerService.this.mQueueLock) {
                            BackupManagerService.this.mRunningFullBackupTask = null;
                        }
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            BackupManagerService.this.scheduleNextFullBackupJob(0L);
                        }
                        Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                        BackupManagerService.this.mWakelock.release();
                        return;
                    }
                    IBackupTransport transport = BackupManagerService.this.getTransport(BackupManagerService.this.mCurrentTransport);
                    if (transport == null) {
                        Slog.w(TAG, "Transport not present; full data backup not performed");
                        Slog.i(TAG, "Full backup completed with status: " + JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        BackupManagerService.sendBackupFinished(this.mBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        cleanUpPipes(null);
                        cleanUpPipes(null);
                        if (this.mJob != null) {
                            this.mJob.finishBackupPass();
                        }
                        synchronized (BackupManagerService.this.mQueueLock) {
                            BackupManagerService.this.mRunningFullBackupTask = null;
                        }
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            BackupManagerService.this.scheduleNextFullBackupJob(0L);
                        }
                        Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                        BackupManagerService.this.mWakelock.release();
                        return;
                    }
                    int N = this.mPackages.size();
                    byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_PREFERRED_XML];
                    for (int i = 0; i < N; i++) {
                        PackageInfo currentPackage = this.mPackages.get(i);
                        String packageName = currentPackage.packageName;
                        Slog.i(TAG, "Initiating full-data transport backup of " + packageName);
                        EventLog.writeEvent(EventLogTags.FULL_BACKUP_PACKAGE, packageName);
                        transportPipes = ParcelFileDescriptor.createPipe();
                        int flags = this.mUserInitiated ? 1 : 0;
                        int backupPackageStatus = transport.performFullBackup(currentPackage, transportPipes[0], flags);
                        if (backupPackageStatus == 0) {
                            transportPipes[0].close();
                            transportPipes[0] = null;
                            enginePipes = ParcelFileDescriptor.createPipe();
                            SinglePackageBackupRunner backupRunner = new SinglePackageBackupRunner(enginePipes[1], currentPackage, transport);
                            enginePipes[1].close();
                            enginePipes[1] = null;
                            new Thread(backupRunner, "package-backup-bridge").start();
                            FileInputStream in = new FileInputStream(enginePipes[0].getFileDescriptor());
                            FileOutputStream out = new FileOutputStream(transportPipes[1].getFileDescriptor());
                            long totalRead = 0;
                            long preflightResult = backupRunner.getPreflightResultBlocking();
                            if (preflightResult < 0) {
                                backupPackageStatus = (int) preflightResult;
                            } else {
                                while (true) {
                                    if (!this.mKeepRunning.get()) {
                                        Slog.i(TAG, "Full backup task told to stop");
                                        break;
                                    }
                                    int nRead = in.read(buffer);
                                    if (nRead > 0) {
                                        out.write(buffer, 0, nRead);
                                        backupPackageStatus = transport.sendBackupData(nRead);
                                        totalRead += (long) nRead;
                                        if (this.mBackupObserver != null && preflightResult > 0) {
                                            BackupManagerService.sendBackupOnUpdate(this.mBackupObserver, packageName, new BackupProgress(preflightResult, totalRead));
                                        }
                                    }
                                    if (nRead <= 0 || backupPackageStatus != 0) {
                                        break;
                                    }
                                }
                                if (backupPackageStatus == -1005) {
                                    long quota = transport.getBackupQuota(packageName, true);
                                    Slog.w(TAG, "Package hit quota limit in-flight " + packageName + ": " + totalRead + " of " + quota);
                                    backupRunner.sendQuotaExceeded(totalRead, quota);
                                }
                            }
                            if (this.mKeepRunning.get()) {
                                int finishResult = transport.finishBackup();
                                if (backupPackageStatus == 0) {
                                    backupPackageStatus = finishResult;
                                }
                            } else {
                                backupPackageStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                transport.cancelFullBackup();
                            }
                            if (backupPackageStatus == 0 && (backupRunnerResult = backupRunner.getBackupResultBlocking()) != 0) {
                                backupPackageStatus = backupRunnerResult;
                            }
                            if (backupPackageStatus != 0) {
                                Slog.e(TAG, "Error " + backupPackageStatus + " backing up " + packageName);
                            }
                            backoff = transport.requestFullBackupTime();
                            Slog.i(TAG, "Transport suggested backoff=" + backoff);
                        }
                        if (this.mUpdateSchedule) {
                            BackupManagerService.this.enqueueFullBackup(packageName, System.currentTimeMillis());
                        }
                        if (backupPackageStatus == -1002) {
                            BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, packageName, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                            Slog.i(TAG, "Transport rejected backup of " + packageName + ", skipping");
                            EventLog.writeEvent(EventLogTags.FULL_BACKUP_AGENT_FAILURE, packageName, "transport rejected");
                        } else if (backupPackageStatus == -1005) {
                            BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, packageName, -1005);
                            Slog.i(TAG, "Transport quota exceeded for package: " + packageName);
                            EventLog.writeEvent(EventLogTags.FULL_BACKUP_QUOTA_EXCEEDED, packageName);
                        } else if (backupPackageStatus == -1003) {
                            BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, packageName, -1003);
                            Slog.w(TAG, "Application failure for package: " + packageName);
                            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, packageName);
                            BackupManagerService.this.tearDownAgentAndKill(currentPackage.applicationInfo);
                        } else {
                            if (backupPackageStatus != 0) {
                                BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, packageName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                Slog.w(TAG, "Transport failed; aborting backup: " + backupPackageStatus);
                                EventLog.writeEvent(EventLogTags.FULL_BACKUP_TRANSPORT_FAILURE, new Object[0]);
                                Slog.i(TAG, "Full backup completed with status: " + JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                BackupManagerService.sendBackupFinished(this.mBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                cleanUpPipes(transportPipes);
                                cleanUpPipes(enginePipes);
                                if (this.mJob != null) {
                                    this.mJob.finishBackupPass();
                                }
                                synchronized (BackupManagerService.this.mQueueLock) {
                                    BackupManagerService.this.mRunningFullBackupTask = null;
                                }
                                this.mLatch.countDown();
                                if (this.mUpdateSchedule) {
                                    BackupManagerService.this.scheduleNextFullBackupJob(backoff);
                                }
                                Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                                BackupManagerService.this.mWakelock.release();
                                return;
                            }
                            BackupManagerService.sendBackupOnPackageResult(this.mBackupObserver, packageName, 0);
                            EventLog.writeEvent(EventLogTags.FULL_BACKUP_SUCCESS, packageName);
                            BackupManagerService.this.logBackupComplete(packageName);
                        }
                        cleanUpPipes(transportPipes);
                        cleanUpPipes(enginePipes);
                    }
                    Slog.i(TAG, "Full backup completed with status: 0");
                    BackupManagerService.sendBackupFinished(this.mBackupObserver, 0);
                    cleanUpPipes(transportPipes);
                    cleanUpPipes(enginePipes);
                    if (this.mJob != null) {
                        this.mJob.finishBackupPass();
                    }
                    synchronized (BackupManagerService.this.mQueueLock) {
                        BackupManagerService.this.mRunningFullBackupTask = null;
                    }
                    this.mLatch.countDown();
                    if (this.mUpdateSchedule) {
                        BackupManagerService.this.scheduleNextFullBackupJob(backoff);
                    }
                    Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                    BackupManagerService.this.mWakelock.release();
                } catch (Exception e) {
                    Slog.w(TAG, "Exception trying full transport backup", e);
                    Slog.i(TAG, "Full backup completed with status: " + JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    BackupManagerService.sendBackupFinished(this.mBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    cleanUpPipes(null);
                    cleanUpPipes(null);
                    if (this.mJob != null) {
                        this.mJob.finishBackupPass();
                    }
                    synchronized (BackupManagerService.this.mQueueLock) {
                        BackupManagerService.this.mRunningFullBackupTask = null;
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            BackupManagerService.this.scheduleNextFullBackupJob(0L);
                        }
                        Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                        BackupManagerService.this.mWakelock.release();
                    }
                }
            } catch (Throwable th) {
                Slog.i(TAG, "Full backup completed with status: 0");
                BackupManagerService.sendBackupFinished(this.mBackupObserver, 0);
                cleanUpPipes(null);
                cleanUpPipes(null);
                if (this.mJob != null) {
                    this.mJob.finishBackupPass();
                }
                synchronized (BackupManagerService.this.mQueueLock) {
                    BackupManagerService.this.mRunningFullBackupTask = null;
                    this.mLatch.countDown();
                    if (this.mUpdateSchedule) {
                        BackupManagerService.this.scheduleNextFullBackupJob(0L);
                    }
                    Slog.i(BackupManagerService.TAG, "Full data backup pass finished.");
                    BackupManagerService.this.mWakelock.release();
                    throw th;
                }
            }
        }

        void cleanUpPipes(ParcelFileDescriptor[] pipes) {
            if (pipes == null) {
                return;
            }
            if (pipes[0] != null) {
                ParcelFileDescriptor fd = pipes[0];
                pipes[0] = null;
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
            if (pipes[1] == null) {
                return;
            }
            ParcelFileDescriptor fd2 = pipes[1];
            pipes[1] = null;
            try {
                fd2.close();
            } catch (IOException e2) {
                Slog.w(TAG, "Unable to close pipe!");
            }
        }

        class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
            final IBackupTransport mTransport;
            final AtomicLong mResult = new AtomicLong(-1003);
            final CountDownLatch mLatch = new CountDownLatch(1);

            public SinglePackageBackupPreflight(IBackupTransport transport) {
                this.mTransport = transport;
            }

            @Override
            public int preflightFullBackup(PackageInfo pkg, IBackupAgent agent) {
                try {
                    int token = BackupManagerService.this.generateToken();
                    BackupManagerService.this.prepareOperationTimeout(token, 300000L, this);
                    BackupManagerService.this.addBackupTrace("preflighting");
                    agent.doMeasureFullBackup(token, BackupManagerService.this.mBackupManagerBinder);
                    this.mLatch.await(300000L, TimeUnit.MILLISECONDS);
                    long totalSize = this.mResult.get();
                    if (totalSize < 0) {
                        return (int) totalSize;
                    }
                    int result = this.mTransport.checkFullBackupSize(totalSize);
                    if (result == -1005) {
                        long quota = this.mTransport.getBackupQuota(pkg.packageName, true);
                        agent.doQuotaExceeded(totalSize, quota);
                        return result;
                    }
                    return result;
                } catch (Exception e) {
                    Slog.w(PerformFullTransportBackupTask.TAG, "Exception preflighting " + pkg.packageName + ": " + e.getMessage());
                    return -1003;
                }
            }

            @Override
            public void execute() {
            }

            @Override
            public void operationComplete(long result) {
                this.mResult.set(result);
                this.mLatch.countDown();
            }

            @Override
            public void handleTimeout() {
                this.mResult.set(-1003L);
                this.mLatch.countDown();
            }

            @Override
            public long getExpectedSizeOrErrorCode() {
                try {
                    this.mLatch.await(300000L, TimeUnit.MILLISECONDS);
                    return this.mResult.get();
                } catch (InterruptedException e) {
                    return -1L;
                }
            }
        }

        class SinglePackageBackupRunner implements Runnable, BackupRestoreTask {
            private FullBackupEngine mEngine;
            final ParcelFileDescriptor mOutput;
            final FullBackupPreflight mPreflight;
            final PackageInfo mTarget;
            final CountDownLatch mPreflightLatch = new CountDownLatch(1);
            final CountDownLatch mBackupLatch = new CountDownLatch(1);
            private volatile int mPreflightResult = -1003;
            private volatile int mBackupResult = -1003;

            SinglePackageBackupRunner(ParcelFileDescriptor output, PackageInfo target, IBackupTransport transport) throws IOException {
                this.mOutput = ParcelFileDescriptor.dup(output.getFileDescriptor());
                this.mTarget = target;
                this.mPreflight = PerformFullTransportBackupTask.this.new SinglePackageBackupPreflight(transport);
            }

            @Override
            public void run() {
                FileOutputStream out = new FileOutputStream(this.mOutput.getFileDescriptor());
                this.mEngine = BackupManagerService.this.new FullBackupEngine(out, this.mPreflight, this.mTarget, false, this);
                try {
                    try {
                        try {
                            this.mPreflightResult = this.mEngine.preflightCheck();
                            this.mPreflightLatch.countDown();
                            if (this.mPreflightResult == 0) {
                                this.mBackupResult = this.mEngine.backupOnePackage();
                            }
                        } catch (Throwable th) {
                            this.mPreflightLatch.countDown();
                            throw th;
                        }
                    } catch (Exception e) {
                        Slog.e(PerformFullTransportBackupTask.TAG, "Exception during full package backup of " + this.mTarget.packageName);
                        this.mBackupLatch.countDown();
                        try {
                            this.mOutput.close();
                        } catch (IOException e2) {
                            Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                        }
                    }
                } finally {
                    this.mBackupLatch.countDown();
                    try {
                        this.mOutput.close();
                    } catch (IOException e3) {
                        Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                    }
                }
            }

            public void sendQuotaExceeded(long backupDataBytes, long quotaBytes) {
                this.mEngine.sendQuotaExceeded(backupDataBytes, quotaBytes);
            }

            long getPreflightResultBlocking() {
                try {
                    this.mPreflightLatch.await(300000L, TimeUnit.MILLISECONDS);
                    if (this.mPreflightResult == 0) {
                        return this.mPreflight.getExpectedSizeOrErrorCode();
                    }
                    return this.mPreflightResult;
                } catch (InterruptedException e) {
                    return -1003L;
                }
            }

            int getBackupResultBlocking() {
                try {
                    this.mBackupLatch.await(300000L, TimeUnit.MILLISECONDS);
                    return this.mBackupResult;
                } catch (InterruptedException e) {
                    return -1003;
                }
            }

            @Override
            public void execute() {
            }

            @Override
            public void operationComplete(long result) {
            }

            @Override
            public void handleTimeout() {
                Slog.w(PerformFullTransportBackupTask.TAG, "Full backup timeout of " + this.mTarget.packageName);
                BackupManagerService.this.tearDownAgentAndKill(this.mTarget.applicationInfo);
            }
        }
    }

    void scheduleNextFullBackupJob(long transportMinLatency) {
        synchronized (this.mQueueLock) {
            if (this.mFullBackupQueue.size() > 0) {
                long upcomingLastBackup = this.mFullBackupQueue.get(0).lastBackup;
                long timeSinceLast = System.currentTimeMillis() - upcomingLastBackup;
                long appLatency = timeSinceLast < 86400000 ? 86400000 - timeSinceLast : 0L;
                final long latency = Math.max(transportMinLatency, appLatency);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        FullBackupJob.schedule(BackupManagerService.this.mContext, latency);
                    }
                };
                this.mBackupHandler.postDelayed(r, 2500L);
            } else {
                Slog.i(TAG, "Full backup queue empty; not scheduling");
            }
        }
    }

    void dequeueFullBackupLocked(String packageName) {
        int N = this.mFullBackupQueue.size();
        for (int i = N - 1; i >= 0; i--) {
            FullBackupEntry e = this.mFullBackupQueue.get(i);
            if (packageName.equals(e.packageName)) {
                this.mFullBackupQueue.remove(i);
            }
        }
    }

    void enqueueFullBackup(String packageName, long lastBackedUp) {
        FullBackupEntry newEntry = new FullBackupEntry(packageName, lastBackedUp);
        synchronized (this.mQueueLock) {
            dequeueFullBackupLocked(packageName);
            int which = -1;
            if (lastBackedUp > 0) {
                which = this.mFullBackupQueue.size() - 1;
                while (true) {
                    if (which < 0) {
                        break;
                    }
                    FullBackupEntry entry = this.mFullBackupQueue.get(which);
                    if (entry.lastBackup > lastBackedUp) {
                        which--;
                    } else {
                        this.mFullBackupQueue.add(which + 1, newEntry);
                        break;
                    }
                }
            }
            if (which < 0) {
                this.mFullBackupQueue.add(0, newEntry);
            }
        }
        writeFullBackupScheduleAsync();
    }

    private boolean fullBackupAllowable(IBackupTransport transport) {
        if (transport == null) {
            Slog.w(TAG, "Transport not present; full data backup not performed");
            return false;
        }
        try {
            File stateDir = new File(this.mBaseStateDir, transport.transportDirName());
            File pmState = new File(stateDir, PACKAGE_MANAGER_SENTINEL);
            if (pmState.length() <= 0) {
                Slog.i(TAG, "Full backup requested but dataset not yet initialized");
                return false;
            }
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Unable to contact transport");
            return false;
        }
    }

    boolean beginFullBackup(FullBackupJob scheduledJob) {
        long now = System.currentTimeMillis();
        FullBackupEntry entry = null;
        long latency = 86400000;
        if (!this.mEnabled || !this.mProvisioned) {
            return false;
        }
        if (this.mPowerManager.isPowerSaveMode()) {
            Slog.i(TAG, "Deferring scheduled full backups in battery saver mode");
            FullBackupJob.schedule(this.mContext, 14400000L);
            return false;
        }
        Slog.i(TAG, "Beginning scheduled full backup operation");
        synchronized (this.mQueueLock) {
            if (this.mRunningFullBackupTask != null) {
                Slog.e(TAG, "Backup triggered but one already/still running!");
                return false;
            }
            boolean runBackup = true;
            while (true) {
                if (this.mFullBackupQueue.size() == 0) {
                    Slog.i(TAG, "Backup queue empty; doing nothing");
                    runBackup = false;
                    break;
                }
                boolean headBusy = false;
                if (!fullBackupAllowable(getTransport(this.mCurrentTransport))) {
                    runBackup = false;
                    latency = 14400000;
                }
                if (runBackup) {
                    entry = this.mFullBackupQueue.get(0);
                    long timeSinceRun = now - entry.lastBackup;
                    runBackup = timeSinceRun >= 86400000;
                    if (!runBackup) {
                        latency = 86400000 - timeSinceRun;
                        break;
                    }
                    try {
                        PackageInfo appInfo = this.mPackageManager.getPackageInfo(entry.packageName, 0);
                        if (!appGetsFullBackup(appInfo)) {
                            this.mFullBackupQueue.remove(0);
                            headBusy = true;
                        } else {
                            int privFlags = appInfo.applicationInfo.privateFlags;
                            if ((privFlags & 4096) != 0) {
                                headBusy = false;
                            } else {
                                headBusy = this.mActivityManager.isAppForeground(appInfo.applicationInfo.uid);
                            }
                            if (headBusy) {
                                long nextEligible = System.currentTimeMillis() + 3600000 + ((long) this.mTokenGenerator.nextInt(BUSY_BACKOFF_FUZZ));
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Slog.i(TAG, "Full backup time but " + entry.packageName + " is busy; deferring to " + sdf.format(new Date(nextEligible)));
                                enqueueFullBackup(entry.packageName, nextEligible - 86400000);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        runBackup = this.mFullBackupQueue.size() > 1;
                    } catch (RemoteException e2) {
                    }
                    if (headBusy) {
                        break;
                    }
                } else if (headBusy) {
                }
            }
            if (!runBackup) {
                Slog.i(TAG, "Nothing pending full backup; rescheduling +" + latency);
                final long deferTime = latency;
                this.mBackupHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FullBackupJob.schedule(BackupManagerService.this.mContext, deferTime);
                    }
                });
                return false;
            }
            this.mFullBackupQueue.remove(0);
            CountDownLatch latch = new CountDownLatch(1);
            String[] pkg = {entry.packageName};
            this.mRunningFullBackupTask = new PerformFullTransportBackupTask(null, pkg, true, scheduledJob, latch, null, false);
            this.mWakelock.acquire();
            new Thread(this.mRunningFullBackupTask).start();
            return true;
        }
    }

    void endFullBackup() {
        synchronized (this.mQueueLock) {
            if (this.mRunningFullBackupTask != null) {
                Slog.i(TAG, "Telling running backup to stop");
                this.mRunningFullBackupTask.setRunning(false);
            }
        }
    }

    abstract class RestoreEngine {
        public static final int SUCCESS = 0;
        static final String TAG = "RestoreEngine";
        public static final int TARGET_FAILURE = -2;
        public static final int TRANSPORT_FAILURE = -3;
        private AtomicBoolean mRunning = new AtomicBoolean(false);
        private AtomicInteger mResult = new AtomicInteger(0);

        RestoreEngine() {
        }

        public boolean isRunning() {
            return this.mRunning.get();
        }

        public void setRunning(boolean stillRunning) {
            synchronized (this.mRunning) {
                this.mRunning.set(stillRunning);
                this.mRunning.notifyAll();
            }
        }

        public int waitForResult() {
            synchronized (this.mRunning) {
                while (isRunning()) {
                    try {
                        this.mRunning.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return getResult();
        }

        public int getResult() {
            return this.mResult.get();
        }

        public void setResult(int result) {
            this.mResult.set(result);
        }
    }

    static class FileMetadata {
        String domain;
        String installerPackageName;
        long mode;
        long mtime;
        String packageName;
        String path;
        long size;
        int type;

        FileMetadata() {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("FileMetadata{");
            sb.append(this.packageName);
            sb.append(',');
            sb.append(this.type);
            sb.append(',');
            sb.append(this.domain);
            sb.append(':');
            sb.append(this.path);
            sb.append(',');
            sb.append(this.size);
            sb.append('}');
            return sb.toString();
        }
    }

    enum RestorePolicy {
        IGNORE,
        ACCEPT,
        ACCEPT_IF_APK;

        public static RestorePolicy[] valuesCustom() {
            return values();
        }
    }

    class FullRestoreEngine extends RestoreEngine {

        private static final int[] f3x6484bc22 = null;
        final int[] $SWITCH_TABLE$com$android$server$backup$BackupManagerService$RestorePolicy;
        IBackupAgent mAgent;
        String mAgentPackage;
        boolean mAllowApks;
        boolean mAllowObbs;
        byte[] mBuffer;
        long mBytes;
        final HashSet<String> mClearedPackages;
        final RestoreDeleteObserver mDeleteObserver;
        final RestoreInstallObserver mInstallObserver;
        final HashMap<String, Signature[]> mManifestSignatures;
        BackupRestoreTask mMonitorTask;
        FullBackupObbConnection mObbConnection;
        IFullBackupRestoreObserver mObserver;
        PackageInfo mOnlyPackage;
        final HashMap<String, String> mPackageInstallers;
        final HashMap<String, RestorePolicy> mPackagePolicies;
        ParcelFileDescriptor[] mPipes;
        ApplicationInfo mTargetApp;
        byte[] mWidgetData;

        private static int[] m1165x8547bcfe() {
            if (f3x6484bc22 != null) {
                return f3x6484bc22;
            }
            int[] iArr = new int[RestorePolicy.valuesCustom().length];
            try {
                iArr[RestorePolicy.ACCEPT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[RestorePolicy.IGNORE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f3x6484bc22 = iArr;
            return iArr;
        }

        class RestoreFileRunnable implements Runnable {
            IBackupAgent mAgent;
            FileMetadata mInfo;
            ParcelFileDescriptor mSocket;
            int mToken;

            RestoreFileRunnable(IBackupAgent agent, FileMetadata info, ParcelFileDescriptor socket, int token) throws IOException {
                this.mAgent = agent;
                this.mInfo = info;
                this.mToken = token;
                this.mSocket = ParcelFileDescriptor.dup(socket.getFileDescriptor());
            }

            @Override
            public void run() {
                try {
                    this.mAgent.doRestoreFile(this.mSocket, this.mInfo.size, this.mInfo.type, this.mInfo.domain, this.mInfo.path, this.mInfo.mode, this.mInfo.mtime, this.mToken, BackupManagerService.this.mBackupManagerBinder);
                } catch (RemoteException e) {
                }
            }
        }

        public FullRestoreEngine(BackupRestoreTask monitorTask, IFullBackupRestoreObserver observer, PackageInfo onlyPackage, boolean allowApks, boolean allowObbs) {
            super();
            this.mObbConnection = null;
            this.mPackagePolicies = new HashMap<>();
            this.mPackageInstallers = new HashMap<>();
            this.mManifestSignatures = new HashMap<>();
            this.mClearedPackages = new HashSet<>();
            this.mPipes = null;
            this.mWidgetData = null;
            this.mInstallObserver = new RestoreInstallObserver();
            this.mDeleteObserver = new RestoreDeleteObserver();
            this.mMonitorTask = monitorTask;
            this.mObserver = observer;
            this.mOnlyPackage = onlyPackage;
            this.mAllowApks = allowApks;
            this.mAllowObbs = allowObbs;
            this.mBuffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
            this.mBytes = 0L;
        }

        public IBackupAgent getAgent() {
            return this.mAgent;
        }

        public byte[] getWidgetData() {
            return this.mWidgetData;
        }

        public boolean restoreOneFile(InputStream instream, boolean mustKillAgent) {
            FileMetadata info;
            RestorePolicy restorePolicy;
            if (!isRunning()) {
                Slog.w("RestoreEngine", "Restore engine used after halting");
                return false;
            }
            try {
                info = readTarHeaders(instream);
                if (info != null) {
                    String pkg = info.packageName;
                    if (!pkg.equals(this.mAgentPackage)) {
                        if (this.mOnlyPackage != null && !pkg.equals(this.mOnlyPackage.packageName)) {
                            Slog.w("RestoreEngine", "Expected data for " + this.mOnlyPackage + " but saw " + pkg);
                            setResult(-3);
                            setRunning(false);
                            return false;
                        }
                        if (!this.mPackagePolicies.containsKey(pkg)) {
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                        if (this.mAgent != null) {
                            Slog.d("RestoreEngine", "Saw new package; finalizing old one");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp);
                            this.mTargetApp = null;
                            this.mAgentPackage = null;
                        }
                    }
                    if (info.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                        this.mPackagePolicies.put(pkg, readAppManifest(info, instream));
                        this.mPackageInstallers.put(pkg, info.installerPackageName);
                        skipTarPadding(info.size, instream);
                        sendOnRestorePackage(pkg);
                    } else if (info.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        readMetadata(info, instream);
                        skipTarPadding(info.size, instream);
                    } else {
                        boolean okay = true;
                        RestorePolicy policy = this.mPackagePolicies.get(pkg);
                        switch (m1165x8547bcfe()[policy.ordinal()]) {
                            case 1:
                                if (info.domain.equals("a")) {
                                    Slog.d("RestoreEngine", "apk present but ACCEPT");
                                    okay = false;
                                }
                                break;
                            case 2:
                                if (info.domain.equals("a")) {
                                    Slog.d("RestoreEngine", "APK file; installing");
                                    String installerName = this.mPackageInstallers.get(pkg);
                                    boolean okay2 = installApk(info, installerName, instream);
                                    HashMap<String, RestorePolicy> map = this.mPackagePolicies;
                                    if (okay2) {
                                        restorePolicy = RestorePolicy.ACCEPT;
                                    } else {
                                        restorePolicy = RestorePolicy.IGNORE;
                                    }
                                    map.put(pkg, restorePolicy);
                                    skipTarPadding(info.size, instream);
                                    return true;
                                }
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                okay = false;
                                break;
                                break;
                            case 3:
                                okay = false;
                                break;
                            default:
                                Slog.e("RestoreEngine", "Invalid policy from manifest");
                                okay = false;
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                break;
                        }
                        if (!isRestorableFile(info)) {
                            okay = false;
                        }
                        if (okay && this.mAgent != null) {
                            Slog.i("RestoreEngine", "Reusing existing agent instance");
                        }
                        if (okay && this.mAgent == null) {
                            Slog.d("RestoreEngine", "Need to launch agent for " + pkg);
                            try {
                                this.mTargetApp = BackupManagerService.this.mPackageManager.getApplicationInfo(pkg, 0);
                                if (!this.mClearedPackages.contains(pkg)) {
                                    if (this.mTargetApp.backupAgentName == null) {
                                        Slog.d("RestoreEngine", "Clearing app data preparatory to full restore");
                                        BackupManagerService.this.clearApplicationDataSynchronous(pkg);
                                    }
                                    this.mClearedPackages.add(pkg);
                                }
                                setUpPipes();
                                this.mAgent = BackupManagerService.this.bindToAgentSynchronous(this.mTargetApp, 3);
                                this.mAgentPackage = pkg;
                            } catch (PackageManager.NameNotFoundException e) {
                            } catch (IOException e2) {
                            }
                            if (this.mAgent == null) {
                                Slog.e("RestoreEngine", "Unable to create agent for " + pkg);
                                okay = false;
                                tearDownPipes();
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            }
                        }
                        if (okay && !pkg.equals(this.mAgentPackage)) {
                            Slog.e("RestoreEngine", "Restoring data for " + pkg + " but agent is for " + this.mAgentPackage);
                            okay = false;
                        }
                        if (okay) {
                            boolean agentSuccess = true;
                            long toCopy = info.size;
                            int token = BackupManagerService.this.generateToken();
                            try {
                                BackupManagerService.this.prepareOperationTimeout(token, 300000L, this.mMonitorTask);
                                if (info.domain.equals("obb")) {
                                    Slog.d("RestoreEngine", "Restoring OBB file for " + pkg + " : " + info.path);
                                    this.mObbConnection.restoreObbFile(pkg, this.mPipes[0], info.size, info.type, info.path, info.mode, info.mtime, token, BackupManagerService.this.mBackupManagerBinder);
                                } else if (this.mTargetApp.processName.equals("system")) {
                                    Slog.d("RestoreEngine", "system process agent - spinning a thread");
                                    RestoreFileRunnable runner = new RestoreFileRunnable(this.mAgent, info, this.mPipes[0], token);
                                    new Thread(runner, "restore-sys-runner").start();
                                } else {
                                    this.mAgent.doRestoreFile(this.mPipes[0], info.size, info.type, info.domain, info.path, info.mode, info.mtime, token, BackupManagerService.this.mBackupManagerBinder);
                                }
                            } catch (RemoteException e3) {
                                Slog.e("RestoreEngine", "Agent crashed during full restore");
                                agentSuccess = false;
                                okay = false;
                            } catch (IOException e4) {
                                Slog.d("RestoreEngine", "Couldn't establish restore");
                                agentSuccess = false;
                                okay = false;
                            }
                            if (okay) {
                                boolean pipeOkay = true;
                                FileOutputStream pipe = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                                while (toCopy > 0) {
                                    int toRead = toCopy > ((long) this.mBuffer.length) ? this.mBuffer.length : (int) toCopy;
                                    int nRead = instream.read(this.mBuffer, 0, toRead);
                                    if (nRead >= 0) {
                                        this.mBytes += (long) nRead;
                                    }
                                    if (nRead > 0) {
                                        toCopy -= (long) nRead;
                                        if (pipeOkay) {
                                            try {
                                                pipe.write(this.mBuffer, 0, nRead);
                                            } catch (IOException e5) {
                                                Slog.e("RestoreEngine", "Failed to write to restore pipe: " + e5.getMessage());
                                                pipeOkay = false;
                                            }
                                        }
                                    } else {
                                        skipTarPadding(info.size, instream);
                                        agentSuccess = BackupManagerService.this.waitUntilOperationComplete(token);
                                    }
                                }
                                skipTarPadding(info.size, instream);
                                agentSuccess = BackupManagerService.this.waitUntilOperationComplete(token);
                            }
                            if (!agentSuccess) {
                                Slog.w("RestoreEngine", "Agent failure; ending restore");
                                BackupManagerService.this.mBackupHandler.removeMessages(7);
                                tearDownPipes();
                                tearDownAgent(this.mTargetApp);
                                this.mAgent = null;
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                if (this.mOnlyPackage != null) {
                                    setResult(-2);
                                    setRunning(false);
                                    return false;
                                }
                            }
                        }
                        if (!okay) {
                            long bytesToConsume = (info.size + 511) & (-512);
                            while (bytesToConsume > 0) {
                                int toRead2 = bytesToConsume > ((long) this.mBuffer.length) ? this.mBuffer.length : (int) bytesToConsume;
                                long nRead2 = instream.read(this.mBuffer, 0, toRead2);
                                if (nRead2 >= 0) {
                                    this.mBytes += nRead2;
                                }
                                if (nRead2 > 0) {
                                    bytesToConsume -= nRead2;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e6) {
                Slog.w("RestoreEngine", "io exception on restore socket read: " + e6.getMessage());
                setResult(-3);
                info = null;
            }
            if (info == null) {
                tearDownPipes();
                setRunning(false);
                if (mustKillAgent) {
                    tearDownAgent(this.mTargetApp);
                }
            }
            return info != null;
        }

        void setUpPipes() throws IOException {
            this.mPipes = ParcelFileDescriptor.createPipe();
        }

        void tearDownPipes() {
            if (this.mPipes == null) {
                return;
            }
            try {
                this.mPipes[0].close();
                this.mPipes[0] = null;
                this.mPipes[1].close();
                this.mPipes[1] = null;
            } catch (IOException e) {
                Slog.w("RestoreEngine", "Couldn't close agent pipes", e);
            }
            this.mPipes = null;
        }

        void tearDownAgent(ApplicationInfo app) {
            if (this.mAgent == null) {
                return;
            }
            BackupManagerService.this.tearDownAgentAndKill(app);
            this.mAgent = null;
        }

        void handleTimeout() {
            tearDownPipes();
            setResult(-2);
            setRunning(false);
        }

        class RestoreInstallObserver extends PackageInstallObserver {
            final AtomicBoolean mDone = new AtomicBoolean();
            String mPackageName;
            int mResult;

            RestoreInstallObserver() {
            }

            public void reset() {
                synchronized (this.mDone) {
                    this.mDone.set(false);
                }
            }

            public void waitForCompletion() {
                synchronized (this.mDone) {
                    while (!this.mDone.get()) {
                        try {
                            this.mDone.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            int getResult() {
                return this.mResult;
            }

            public void onPackageInstalled(String packageName, int returnCode, String msg, Bundle extras) {
                synchronized (this.mDone) {
                    this.mResult = returnCode;
                    this.mPackageName = packageName;
                    this.mDone.set(true);
                    this.mDone.notifyAll();
                }
            }
        }

        class RestoreDeleteObserver extends IPackageDeleteObserver.Stub {
            final AtomicBoolean mDone = new AtomicBoolean();
            int mResult;

            RestoreDeleteObserver() {
            }

            public void reset() {
                synchronized (this.mDone) {
                    this.mDone.set(false);
                }
            }

            public void waitForCompletion() {
                synchronized (this.mDone) {
                    while (!this.mDone.get()) {
                        try {
                            this.mDone.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            public void packageDeleted(String packageName, int returnCode) throws RemoteException {
                synchronized (this.mDone) {
                    this.mResult = returnCode;
                    this.mDone.set(true);
                    this.mDone.notifyAll();
                }
            }
        }

        boolean installApk(FileMetadata info, String installerPackage, InputStream instream) {
            boolean okay = true;
            Slog.d("RestoreEngine", "Installing from backup: " + info.packageName);
            File apkFile = new File(BackupManagerService.this.mDataDir, info.packageName);
            try {
                FileOutputStream apkStream = new FileOutputStream(apkFile);
                byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
                long size = info.size;
                while (size > 0) {
                    long toRead = ((long) buffer.length) < size ? buffer.length : size;
                    int didRead = instream.read(buffer, 0, (int) toRead);
                    if (didRead >= 0) {
                        this.mBytes += (long) didRead;
                    }
                    apkStream.write(buffer, 0, didRead);
                    size -= (long) didRead;
                }
                apkStream.close();
                apkFile.setReadable(true, false);
                Uri packageUri = Uri.fromFile(apkFile);
                this.mInstallObserver.reset();
                BackupManagerService.this.mPackageManager.installPackage(packageUri, this.mInstallObserver, 34, installerPackage);
                this.mInstallObserver.waitForCompletion();
                if (this.mInstallObserver.getResult() == 1) {
                    boolean uninstall = false;
                    if (this.mInstallObserver.mPackageName.equals(info.packageName)) {
                        try {
                            PackageInfo pkg = BackupManagerService.this.mPackageManager.getPackageInfo(info.packageName, 64);
                            if ((pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0) {
                                Slog.w("RestoreEngine", "Restore stream contains apk of package " + info.packageName + " but it disallows backup/restore");
                                okay = false;
                            } else {
                                Signature[] sigs = this.mManifestSignatures.get(info.packageName);
                                if (!BackupManagerService.signaturesMatch(sigs, pkg)) {
                                    Slog.w("RestoreEngine", "Installed app " + info.packageName + " signatures do not match restore manifest");
                                    okay = false;
                                    uninstall = true;
                                } else if (pkg.applicationInfo.uid < 10000 && pkg.applicationInfo.backupAgentName == null) {
                                    Slog.w("RestoreEngine", "Installed app " + info.packageName + " has restricted uid and no agent");
                                    okay = false;
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w("RestoreEngine", "Install of package " + info.packageName + " succeeded but now not found");
                            okay = false;
                        }
                    } else {
                        Slog.w("RestoreEngine", "Restore stream claimed to include apk for " + info.packageName + " but apk was really " + this.mInstallObserver.mPackageName);
                        okay = false;
                        uninstall = true;
                    }
                    if (uninstall) {
                        this.mDeleteObserver.reset();
                        BackupManagerService.this.mPackageManager.deletePackage(this.mInstallObserver.mPackageName, this.mDeleteObserver, 0);
                        this.mDeleteObserver.waitForCompletion();
                    }
                } else if (this.mPackagePolicies.get(info.packageName) != RestorePolicy.ACCEPT) {
                    okay = false;
                }
                return okay;
            } catch (IOException e2) {
                Slog.e("RestoreEngine", "Unable to transcribe restored apk for install");
                return false;
            } finally {
                apkFile.delete();
            }
        }

        void skipTarPadding(long size, InputStream instream) throws IOException {
            long partial = (size + 512) % 512;
            if (partial <= 0) {
                return;
            }
            int needed = 512 - ((int) partial);
            byte[] buffer = new byte[needed];
            if (readExactly(instream, buffer, 0, needed) == needed) {
                this.mBytes += (long) needed;
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }

        void readMetadata(FileMetadata info, InputStream instream) throws IOException {
            if (info.size > 65536) {
                throw new IOException("Metadata too big; corrupt? size=" + info.size);
            }
            byte[] buffer = new byte[(int) info.size];
            if (readExactly(instream, buffer, 0, (int) info.size) != info.size) {
                throw new IOException("Unexpected EOF in widget data");
            }
            this.mBytes += info.size;
            String[] str = new String[1];
            int offset = extractLine(buffer, 0, str);
            int version = Integer.parseInt(str[0]);
            if (version != 1) {
                Slog.w("RestoreEngine", "Unsupported metadata version " + version);
                return;
            }
            int offset2 = extractLine(buffer, offset, str);
            String pkg = str[0];
            if (!info.packageName.equals(pkg)) {
                Slog.w("RestoreEngine", "Metadata mismatch: package " + info.packageName + " but widget data for " + pkg);
                return;
            }
            ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset2, buffer.length - offset2);
            DataInputStream in = new DataInputStream(bin);
            while (bin.available() > 0) {
                int token = in.readInt();
                int size = in.readInt();
                if (size <= 65536) {
                    switch (token) {
                        case BackupManagerService.BACKUP_WIDGET_METADATA_TOKEN:
                            this.mWidgetData = new byte[size];
                            in.read(this.mWidgetData);
                            break;
                        default:
                            Slog.i("RestoreEngine", "Ignoring metadata blob " + Integer.toHexString(token) + " for " + info.packageName);
                            in.skipBytes(size);
                            break;
                    }
                } else {
                    throw new IOException("Datum " + Integer.toHexString(token) + " too big; corrupt? size=" + info.size);
                }
            }
        }

        RestorePolicy readAppManifest(FileMetadata info, InputStream instream) throws IOException {
            if (info.size > 65536) {
                throw new IOException("Restore manifest too big; corrupt? size=" + info.size);
            }
            byte[] buffer = new byte[(int) info.size];
            if (readExactly(instream, buffer, 0, (int) info.size) != info.size) {
                throw new IOException("Unexpected EOF in manifest");
            }
            this.mBytes += info.size;
            RestorePolicy policy = RestorePolicy.IGNORE;
            String[] str = new String[1];
            try {
                int offset = extractLine(buffer, 0, str);
                int version = Integer.parseInt(str[0]);
                if (version == 1) {
                    int offset2 = extractLine(buffer, offset, str);
                    String manifestPackage = str[0];
                    if (manifestPackage.equals(info.packageName)) {
                        int offset3 = extractLine(buffer, offset2, str);
                        int version2 = Integer.parseInt(str[0]);
                        int offset4 = extractLine(buffer, offset3, str);
                        Integer.parseInt(str[0]);
                        int offset5 = extractLine(buffer, offset4, str);
                        info.installerPackageName = str[0].length() > 0 ? str[0] : null;
                        int offset6 = extractLine(buffer, offset5, str);
                        boolean hasApk = str[0].equals("1");
                        int offset7 = extractLine(buffer, offset6, str);
                        int numSigs = Integer.parseInt(str[0]);
                        if (numSigs > 0) {
                            Signature[] sigs = new Signature[numSigs];
                            for (int i = 0; i < numSigs; i++) {
                                offset7 = extractLine(buffer, offset7, str);
                                sigs[i] = new Signature(str[0]);
                            }
                            this.mManifestSignatures.put(info.packageName, sigs);
                            try {
                                PackageInfo pkgInfo = BackupManagerService.this.mPackageManager.getPackageInfo(info.packageName, 64);
                                int flags = pkgInfo.applicationInfo.flags;
                                if ((32768 & flags) == 0) {
                                    Slog.i("RestoreEngine", "Restore manifest from " + info.packageName + " but allowBackup=false");
                                } else if (pkgInfo.applicationInfo.uid < 10000 && pkgInfo.applicationInfo.backupAgentName == null) {
                                    Slog.w("RestoreEngine", "Package " + info.packageName + " is system level with no agent");
                                } else if (!BackupManagerService.signaturesMatch(sigs, pkgInfo)) {
                                    Slog.w("RestoreEngine", "Restore manifest signatures do not match installed application for " + info.packageName);
                                } else if (pkgInfo.versionCode >= version2) {
                                    Slog.i("RestoreEngine", "Sig + version match; taking data");
                                    policy = RestorePolicy.ACCEPT;
                                } else if (this.mAllowApks) {
                                    Slog.i("RestoreEngine", "Data version " + version2 + " is newer than installed version " + pkgInfo.versionCode + " - requiring apk");
                                    policy = RestorePolicy.ACCEPT_IF_APK;
                                } else {
                                    Slog.i("RestoreEngine", "Data requires newer version " + version2 + "; ignoring");
                                    policy = RestorePolicy.IGNORE;
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                if (this.mAllowApks) {
                                    Slog.i("RestoreEngine", "Package " + info.packageName + " not installed; requiring apk in dataset");
                                    policy = RestorePolicy.ACCEPT_IF_APK;
                                } else {
                                    policy = RestorePolicy.IGNORE;
                                }
                            }
                            if (policy == RestorePolicy.ACCEPT_IF_APK && !hasApk) {
                                Slog.i("RestoreEngine", "Cannot restore package " + info.packageName + " without the matching .apk");
                            }
                        } else {
                            Slog.i("RestoreEngine", "Missing signature on backed-up package " + info.packageName);
                        }
                    } else {
                        Slog.i("RestoreEngine", "Expected package " + info.packageName + " but restore manifest claims " + manifestPackage);
                    }
                } else {
                    Slog.i("RestoreEngine", "Unknown restore manifest version " + version + " for package " + info.packageName);
                }
            } catch (NumberFormatException e2) {
                Slog.w("RestoreEngine", "Corrupt restore manifest for package " + info.packageName);
            } catch (IllegalArgumentException e3) {
                Slog.w("RestoreEngine", e3.getMessage());
            }
            return policy;
        }

        int extractLine(byte[] buffer, int offset, String[] outStr) throws IOException {
            int end = buffer.length;
            if (offset >= end) {
                throw new IOException("Incomplete data");
            }
            int pos = offset;
            while (pos < end) {
                byte c = buffer[pos];
                if (c == 10) {
                    break;
                }
                pos++;
            }
            outStr[0] = new String(buffer, offset, pos - offset);
            return pos + 1;
        }

        void dumpFileMetadata(FileMetadata info) {
        }

        FileMetadata readTarHeaders(InputStream instream) throws IOException {
            FileMetadata info;
            byte[] block = new byte[512];
            if (!readTarHeader(instream, block)) {
                return null;
            }
            try {
                info = new FileMetadata();
            } catch (IOException e) {
                e = e;
            }
            try {
                info.size = extractRadix(block, 124, 12, 8);
                info.mtime = extractRadix(block, 136, 12, 8);
                info.mode = extractRadix(block, 100, 8, 8);
                info.path = extractString(block, 345, 155);
                String path = extractString(block, 0, 100);
                if (path.length() > 0) {
                    if (info.path.length() > 0) {
                        info.path += '/';
                    }
                    info.path += path;
                }
                int typeChar = block[156];
                if (typeChar == 120) {
                    boolean gotHeader = readPaxExtendedHeader(instream, info);
                    if (gotHeader) {
                        gotHeader = readTarHeader(instream, block);
                    }
                    if (!gotHeader) {
                        throw new IOException("Bad or missing pax header");
                    }
                    typeChar = block[156];
                }
                switch (typeChar) {
                    case 0:
                        return null;
                    case 48:
                        info.type = 1;
                        break;
                    case 53:
                        info.type = 2;
                        if (info.size != 0) {
                            Slog.w("RestoreEngine", "Directory entry with nonzero size in header");
                            info.size = 0L;
                        }
                        break;
                    default:
                        Slog.e("RestoreEngine", "Unknown tar entity type: " + typeChar);
                        throw new IOException("Unknown entity type " + typeChar);
                }
                if ("shared/".regionMatches(0, info.path, 0, "shared/".length())) {
                    info.path = info.path.substring("shared/".length());
                    info.packageName = BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                    info.domain = "shared";
                    Slog.i("RestoreEngine", "File in shared storage: " + info.path);
                } else if ("apps/".regionMatches(0, info.path, 0, "apps/".length())) {
                    info.path = info.path.substring("apps/".length());
                    int slash = info.path.indexOf(47);
                    if (slash < 0) {
                        throw new IOException("Illegal semantic path in " + info.path);
                    }
                    info.packageName = info.path.substring(0, slash);
                    info.path = info.path.substring(slash + 1);
                    if (!info.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME) && !info.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        int slash2 = info.path.indexOf(47);
                        if (slash2 < 0) {
                            throw new IOException("Illegal semantic path in non-manifest " + info.path);
                        }
                        info.domain = info.path.substring(0, slash2);
                        info.path = info.path.substring(slash2 + 1);
                    }
                }
                return info;
            } catch (IOException e2) {
                e = e2;
                Slog.e("RestoreEngine", "Parse error in header: " + e.getMessage());
                throw e;
            }
        }

        private boolean isRestorableFile(FileMetadata info) {
            if ("c".equals(info.domain)) {
                return false;
            }
            return (("r".equals(info.domain) && info.path.startsWith("no_backup/")) || info.path.contains("..") || info.path.contains("//")) ? false : true;
        }

        private void HEXLOG(byte[] block) {
            int offset = 0;
            int todo = block.length;
            StringBuilder buf = new StringBuilder(64);
            while (todo > 0) {
                buf.append(String.format("%04x   ", Integer.valueOf(offset)));
                int numThisLine = todo > 16 ? 16 : todo;
                for (int i = 0; i < numThisLine; i++) {
                    buf.append(String.format("%02x ", Byte.valueOf(block[offset + i])));
                }
                Slog.i("hexdump", buf.toString());
                buf.setLength(0);
                todo -= numThisLine;
                offset += numThisLine;
            }
        }

        int readExactly(InputStream in, byte[] buffer, int offset, int size) throws IOException {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            int soFar = 0;
            while (soFar < size) {
                int nRead = in.read(buffer, offset + soFar, size - soFar);
                if (nRead <= 0) {
                    break;
                }
                soFar += nRead;
            }
            return soFar;
        }

        boolean readTarHeader(InputStream instream, byte[] block) throws IOException {
            int got = readExactly(instream, block, 0, 512);
            if (got == 0) {
                return false;
            }
            if (got < 512) {
                throw new IOException("Unable to read full block header");
            }
            this.mBytes += 512;
            return true;
        }

        boolean readPaxExtendedHeader(InputStream instream, FileMetadata info) throws IOException {
            if (info.size > 32768) {
                Slog.w("RestoreEngine", "Suspiciously large pax header size " + info.size + " - aborting");
                throw new IOException("Sanity failure: pax header size " + info.size);
            }
            int numBlocks = (int) ((info.size + 511) >> 9);
            byte[] data = new byte[numBlocks * 512];
            if (readExactly(instream, data, 0, data.length) < data.length) {
                throw new IOException("Unable to read full pax header");
            }
            this.mBytes += (long) data.length;
            int contentSize = (int) info.size;
            int offset = 0;
            do {
                int eol = offset + 1;
                while (eol < contentSize && data[eol] != 32) {
                    eol++;
                }
                if (eol >= contentSize) {
                    throw new IOException("Invalid pax data");
                }
                int linelen = (int) extractRadix(data, offset, eol - offset, 10);
                int key = eol + 1;
                int eol2 = (offset + linelen) - 1;
                int value = key + 1;
                while (data[value] != 61 && value <= eol2) {
                    value++;
                }
                if (value > eol2) {
                    throw new IOException("Invalid pax declaration");
                }
                String keyStr = new String(data, key, value - key, "UTF-8");
                String valStr = new String(data, value + 1, (eol2 - value) - 1, "UTF-8");
                if ("path".equals(keyStr)) {
                    info.path = valStr;
                } else if ("size".equals(keyStr)) {
                    info.size = Long.parseLong(valStr);
                } else {
                    Slog.i("RestoreEngine", "Unhandled pax key: " + key);
                }
                offset += linelen;
            } while (offset < contentSize);
            return true;
        }

        long extractRadix(byte[] data, int offset, int maxChars, int radix) throws IOException {
            long value = 0;
            int end = offset + maxChars;
            for (int i = offset; i < end; i++) {
                byte b = data[i];
                if (b == 0 || b == 32) {
                    break;
                }
                if (b < 48 || b > (radix + 48) - 1) {
                    throw new IOException("Invalid number in header: '" + ((char) b) + "' for radix " + radix);
                }
                value = (((long) radix) * value) + ((long) (b - 48));
            }
            return value;
        }

        String extractString(byte[] data, int offset, int maxChars) throws IOException {
            int end = offset + maxChars;
            int eos = offset;
            while (eos < end && data[eos] != 0) {
                eos++;
            }
            return new String(data, offset, eos - offset, "US-ASCII");
        }

        void sendStartRestore() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onStartRestore();
            } catch (RemoteException e) {
                Slog.w("RestoreEngine", "full restore observer went away: startRestore");
                this.mObserver = null;
            }
        }

        void sendOnRestorePackage(String name) {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onRestorePackage(name);
            } catch (RemoteException e) {
                Slog.w("RestoreEngine", "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }

        void sendEndRestore() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onEndRestore();
            } catch (RemoteException e) {
                Slog.w("RestoreEngine", "full restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }

    class AdbRestoreFinishedLatch implements BackupRestoreTask {
        static final String TAG = "AdbRestoreFinishedLatch";
        final CountDownLatch mLatch = new CountDownLatch(1);

        AdbRestoreFinishedLatch() {
        }

        void await() {
            try {
                this.mLatch.await(300000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupted!");
            }
        }

        @Override
        public void execute() {
        }

        @Override
        public void operationComplete(long result) {
            this.mLatch.countDown();
        }

        @Override
        public void handleTimeout() {
            Slog.w(TAG, "adb onRestoreFinished() timed out");
            this.mLatch.countDown();
        }
    }

    class PerformAdbRestoreTask implements Runnable {

        private static final int[] f4x6484bc22 = null;
        final int[] $SWITCH_TABLE$com$android$server$backup$BackupManagerService$RestorePolicy;
        long mBytes;
        String mCurrentPassword;
        String mDecryptPassword;
        ParcelFileDescriptor mInputFile;
        AtomicBoolean mLatchObject;
        FullBackupObbConnection mObbConnection;
        IFullBackupRestoreObserver mObserver;
        ParcelFileDescriptor[] mPipes = null;
        byte[] mWidgetData = null;
        final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap<>();
        final HashMap<String, String> mPackageInstallers = new HashMap<>();
        final HashMap<String, Signature[]> mManifestSignatures = new HashMap<>();
        final HashSet<String> mClearedPackages = new HashSet<>();
        final RestoreInstallObserver mInstallObserver = new RestoreInstallObserver();
        final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
        IBackupAgent mAgent = null;
        String mAgentPackage = null;
        ApplicationInfo mTargetApp = null;

        private static int[] m1166x8547bcfe() {
            if (f4x6484bc22 != null) {
                return f4x6484bc22;
            }
            int[] iArr = new int[RestorePolicy.valuesCustom().length];
            try {
                iArr[RestorePolicy.ACCEPT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[RestorePolicy.IGNORE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f4x6484bc22 = iArr;
            return iArr;
        }

        class RestoreFinishedRunnable implements Runnable {
            final IBackupAgent mAgent;
            final int mToken;

            RestoreFinishedRunnable(IBackupAgent agent, int token) {
                this.mAgent = agent;
                this.mToken = token;
            }

            @Override
            public void run() {
                try {
                    this.mAgent.doRestoreFinished(this.mToken, BackupManagerService.this.mBackupManagerBinder);
                } catch (RemoteException e) {
                }
            }
        }

        PerformAdbRestoreTask(ParcelFileDescriptor fd, String curPassword, String decryptPassword, IFullBackupRestoreObserver observer, AtomicBoolean latch) {
            this.mObbConnection = null;
            this.mInputFile = fd;
            this.mCurrentPassword = curPassword;
            this.mDecryptPassword = decryptPassword;
            this.mObserver = observer;
            this.mLatchObject = latch;
            this.mObbConnection = BackupManagerService.this.new FullBackupObbConnection();
            this.mClearedPackages.add("android");
            this.mClearedPackages.add(BackupManagerService.SETTINGS_PACKAGE);
        }

        class RestoreFileRunnable implements Runnable {
            IBackupAgent mAgent;
            FileMetadata mInfo;
            ParcelFileDescriptor mSocket;
            int mToken;

            RestoreFileRunnable(IBackupAgent agent, FileMetadata info, ParcelFileDescriptor socket, int token) throws IOException {
                this.mAgent = agent;
                this.mInfo = info;
                this.mToken = token;
                this.mSocket = ParcelFileDescriptor.dup(socket.getFileDescriptor());
            }

            @Override
            public void run() {
                try {
                    this.mAgent.doRestoreFile(this.mSocket, this.mInfo.size, this.mInfo.type, this.mInfo.domain, this.mInfo.path, this.mInfo.mode, this.mInfo.mtime, this.mToken, BackupManagerService.this.mBackupManagerBinder);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void run() throws Throwable {
            DataInputStream rawDataIn;
            boolean compressed;
            InputStream preCompressStream;
            boolean okay;
            boolean didRestore;
            Slog.i(BackupManagerService.TAG, "--- Performing full-dataset restore ---");
            this.mObbConnection.establish();
            sendStartRestore();
            if (Environment.getExternalStorageState().equals("mounted")) {
                this.mPackagePolicies.put(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, RestorePolicy.ACCEPT);
            }
            FileInputStream fileInputStream = null;
            DataInputStream rawDataIn2 = null;
            try {
                try {
                    if (!BackupManagerService.this.backupPasswordMatches(this.mCurrentPassword)) {
                        Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        try {
                            this.mInputFile.close();
                        } catch (IOException e) {
                            Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e);
                        }
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                            BackupManagerService.this.mCurrentOperations.clear();
                        }
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        sendEndRestore();
                        Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                        BackupManagerService.this.mWakelock.release();
                        return;
                    }
                    this.mBytes = 0L;
                    byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
                    FileInputStream rawInStream = new FileInputStream(this.mInputFile.getFileDescriptor());
                    try {
                        rawDataIn = new DataInputStream(rawInStream);
                        compressed = false;
                        preCompressStream = rawInStream;
                        okay = false;
                    } catch (IOException e2) {
                        fileInputStream = rawInStream;
                    } catch (Throwable th) {
                        th = th;
                        fileInputStream = rawInStream;
                    }
                    try {
                        int headerLen = BackupManagerService.BACKUP_FILE_HEADER_MAGIC.length();
                        byte[] streamHeader = new byte[headerLen];
                        rawDataIn.readFully(streamHeader);
                        byte[] magicBytes = BackupManagerService.BACKUP_FILE_HEADER_MAGIC.getBytes("UTF-8");
                        if (Arrays.equals(magicBytes, streamHeader)) {
                            String s = readHeaderLine(rawInStream);
                            int archiveVersion = Integer.parseInt(s);
                            if (archiveVersion <= 4) {
                                boolean pbkdf2Fallback = archiveVersion == 1;
                                compressed = Integer.parseInt(readHeaderLine(rawInStream)) != 0;
                                String s2 = readHeaderLine(rawInStream);
                                if (s2.equals("none")) {
                                    okay = true;
                                } else if (this.mDecryptPassword == null || this.mDecryptPassword.length() <= 0) {
                                    Slog.w(BackupManagerService.TAG, "Archive is encrypted but no password given");
                                } else {
                                    preCompressStream = decodeAesHeaderAndInitialize(s2, pbkdf2Fallback, rawInStream);
                                    if (preCompressStream != null) {
                                        okay = true;
                                    }
                                }
                            } else {
                                Slog.w(BackupManagerService.TAG, "Wrong header version: " + s);
                            }
                        } else {
                            Slog.w(BackupManagerService.TAG, "Didn't read the right header magic");
                        }
                        if (!okay) {
                            Slog.w(BackupManagerService.TAG, "Invalid restore data; aborting.");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp, true);
                            if (rawDataIn != null) {
                                try {
                                    rawDataIn.close();
                                } catch (IOException e3) {
                                    Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e3);
                                }
                            }
                            if (rawInStream != null) {
                                rawInStream.close();
                            }
                            this.mInputFile.close();
                            synchronized (BackupManagerService.this.mCurrentOpLock) {
                                BackupManagerService.this.mCurrentOperations.clear();
                            }
                            synchronized (this.mLatchObject) {
                                this.mLatchObject.set(true);
                                this.mLatchObject.notifyAll();
                            }
                            this.mObbConnection.tearDown();
                            sendEndRestore();
                            Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                            BackupManagerService.this.mWakelock.release();
                            return;
                        }
                        InputStream in = compressed ? new InflaterInputStream(preCompressStream) : preCompressStream;
                        do {
                            didRestore = restoreOneFile(in, buffer);
                        } while (didRestore);
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (rawDataIn != null) {
                            try {
                                rawDataIn.close();
                            } catch (IOException e4) {
                                Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e4);
                            }
                        }
                        if (rawInStream != null) {
                            rawInStream.close();
                        }
                        this.mInputFile.close();
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                            BackupManagerService.this.mCurrentOperations.clear();
                        }
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        sendEndRestore();
                        Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                        BackupManagerService.this.mWakelock.release();
                    } catch (IOException e5) {
                        rawDataIn2 = rawDataIn;
                        fileInputStream = rawInStream;
                        Slog.e(BackupManagerService.TAG, "Unable to read restore input");
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (rawDataIn2 != null) {
                            try {
                                rawDataIn2.close();
                            } catch (IOException e6) {
                                Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e6);
                                synchronized (BackupManagerService.this.mCurrentOpLock) {
                                    BackupManagerService.this.mCurrentOperations.clear();
                                }
                                synchronized (this.mLatchObject) {
                                    this.mLatchObject.set(true);
                                    this.mLatchObject.notifyAll();
                                }
                                this.mObbConnection.tearDown();
                                sendEndRestore();
                                Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                                BackupManagerService.this.mWakelock.release();
                                return;
                            }
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        this.mInputFile.close();
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        rawDataIn2 = rawDataIn;
                        fileInputStream = rawInStream;
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (rawDataIn2 != null) {
                            try {
                                rawDataIn2.close();
                            } catch (IOException e7) {
                                Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e7);
                                synchronized (BackupManagerService.this.mCurrentOpLock) {
                                    BackupManagerService.this.mCurrentOperations.clear();
                                }
                                synchronized (this.mLatchObject) {
                                    this.mLatchObject.set(true);
                                    this.mLatchObject.notifyAll();
                                }
                                this.mObbConnection.tearDown();
                                sendEndRestore();
                                Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                                BackupManagerService.this.mWakelock.release();
                                throw th;
                            }
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        this.mInputFile.close();
                        synchronized (BackupManagerService.this.mCurrentOpLock) {
                        }
                    }
                } catch (IOException e8) {
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }

        String readHeaderLine(InputStream in) throws IOException {
            StringBuilder buffer = new StringBuilder(80);
            while (true) {
                int c = in.read();
                if (c < 0 || c == 10) {
                    break;
                }
                buffer.append((char) c);
            }
            return buffer.toString();
        }

        InputStream attemptMasterKeyDecryption(String algorithm, byte[] userSalt, byte[] ckSalt, int rounds, String userIvHex, String masterKeyBlobHex, InputStream rawInStream, boolean doLog) {
            InputStream result = null;
            try {
                Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey userKey = BackupManagerService.this.buildPasswordKey(algorithm, this.mDecryptPassword, userSalt, rounds);
                byte[] IV = BackupManagerService.this.hexToByteArray(userIvHex);
                IvParameterSpec ivSpec = new IvParameterSpec(IV);
                c.init(2, new SecretKeySpec(userKey.getEncoded(), "AES"), ivSpec);
                byte[] mkCipher = BackupManagerService.this.hexToByteArray(masterKeyBlobHex);
                byte[] mkBlob = c.doFinal(mkCipher);
                int len = mkBlob[0];
                byte[] IV2 = Arrays.copyOfRange(mkBlob, 1, len + 1);
                int offset = len + 1;
                int offset2 = offset + 1;
                int len2 = mkBlob[offset];
                byte[] mk = Arrays.copyOfRange(mkBlob, offset2, offset2 + len2);
                int offset3 = offset2 + len2;
                int offset4 = offset3 + 1;
                byte[] mkChecksum = Arrays.copyOfRange(mkBlob, offset4, offset4 + mkBlob[offset3]);
                byte[] calculatedCk = BackupManagerService.this.makeKeyChecksum(algorithm, mk, ckSalt, rounds);
                if (Arrays.equals(calculatedCk, mkChecksum)) {
                    IvParameterSpec ivSpec2 = new IvParameterSpec(IV2);
                    c.init(2, new SecretKeySpec(mk, "AES"), ivSpec2);
                    result = new CipherInputStream(rawInStream, c);
                } else if (doLog) {
                    Slog.w(BackupManagerService.TAG, "Incorrect password");
                }
            } catch (InvalidAlgorithmParameterException e) {
                if (doLog) {
                    Slog.e(BackupManagerService.TAG, "Needed parameter spec unavailable!", e);
                }
            } catch (InvalidKeyException e2) {
                if (doLog) {
                    Slog.w(BackupManagerService.TAG, "Illegal password; aborting");
                }
            } catch (NoSuchAlgorithmException e3) {
                if (doLog) {
                    Slog.e(BackupManagerService.TAG, "Needed decryption algorithm unavailable!");
                }
            } catch (BadPaddingException e4) {
                if (doLog) {
                    Slog.w(BackupManagerService.TAG, "Incorrect password");
                }
            } catch (IllegalBlockSizeException e5) {
                if (doLog) {
                    Slog.w(BackupManagerService.TAG, "Invalid block size in master key");
                }
            } catch (NoSuchPaddingException e6) {
                if (doLog) {
                    Slog.e(BackupManagerService.TAG, "Needed padding mechanism unavailable!");
                }
            }
            return result;
        }

        InputStream decodeAesHeaderAndInitialize(String encryptionName, boolean pbkdf2Fallback, InputStream rawInStream) {
            InputStream result = null;
            try {
                if (encryptionName.equals(BackupManagerService.ENCRYPTION_ALGORITHM_NAME)) {
                    String userSaltHex = readHeaderLine(rawInStream);
                    byte[] userSalt = BackupManagerService.this.hexToByteArray(userSaltHex);
                    String ckSaltHex = readHeaderLine(rawInStream);
                    byte[] ckSalt = BackupManagerService.this.hexToByteArray(ckSaltHex);
                    int rounds = Integer.parseInt(readHeaderLine(rawInStream));
                    String userIvHex = readHeaderLine(rawInStream);
                    String masterKeyBlobHex = readHeaderLine(rawInStream);
                    result = attemptMasterKeyDecryption(BackupManagerService.PBKDF_CURRENT, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, false);
                    if (result == null && pbkdf2Fallback) {
                        result = attemptMasterKeyDecryption(BackupManagerService.PBKDF_FALLBACK, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, true);
                    }
                } else {
                    Slog.w(BackupManagerService.TAG, "Unsupported encryption method: " + encryptionName);
                }
            } catch (IOException e) {
                Slog.w(BackupManagerService.TAG, "Can't read input header");
            } catch (NumberFormatException e2) {
                Slog.w(BackupManagerService.TAG, "Can't parse restore data header");
            }
            return result;
        }

        boolean restoreOneFile(InputStream instream, byte[] buffer) {
            FileMetadata info;
            RestorePolicy restorePolicy;
            try {
                info = readTarHeaders(instream);
                if (info != null) {
                    String pkg = info.packageName;
                    if (!pkg.equals(this.mAgentPackage)) {
                        if (!this.mPackagePolicies.containsKey(pkg)) {
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                        if (this.mAgent != null) {
                            Slog.d(BackupManagerService.TAG, "Saw new package; finalizing old one");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp, true);
                            this.mTargetApp = null;
                            this.mAgentPackage = null;
                        }
                    }
                    if (info.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                        this.mPackagePolicies.put(pkg, readAppManifest(info, instream));
                        this.mPackageInstallers.put(pkg, info.installerPackageName);
                        skipTarPadding(info.size, instream);
                        sendOnRestorePackage(pkg);
                    } else if (info.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        readMetadata(info, instream);
                        skipTarPadding(info.size, instream);
                    } else {
                        boolean okay = true;
                        RestorePolicy policy = this.mPackagePolicies.get(pkg);
                        switch (m1166x8547bcfe()[policy.ordinal()]) {
                            case 1:
                                if (info.domain.equals("a")) {
                                    Slog.d(BackupManagerService.TAG, "apk present but ACCEPT");
                                    okay = false;
                                }
                                break;
                            case 2:
                                if (info.domain.equals("a")) {
                                    Slog.d(BackupManagerService.TAG, "APK file; installing");
                                    String installerName = this.mPackageInstallers.get(pkg);
                                    boolean okay2 = installApk(info, installerName, instream);
                                    HashMap<String, RestorePolicy> map = this.mPackagePolicies;
                                    if (okay2) {
                                        restorePolicy = RestorePolicy.ACCEPT;
                                    } else {
                                        restorePolicy = RestorePolicy.IGNORE;
                                    }
                                    map.put(pkg, restorePolicy);
                                    skipTarPadding(info.size, instream);
                                    return true;
                                }
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                okay = false;
                                break;
                                break;
                            case 3:
                                okay = false;
                                break;
                            default:
                                Slog.e(BackupManagerService.TAG, "Invalid policy from manifest");
                                okay = false;
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                break;
                        }
                        if (info.path.contains("..") || info.path.contains("//")) {
                            okay = false;
                        }
                        if (okay && this.mAgent != null) {
                            Slog.i(BackupManagerService.TAG, "Reusing existing agent instance");
                        }
                        if (okay && this.mAgent == null) {
                            Slog.d(BackupManagerService.TAG, "Need to launch agent for " + pkg);
                            try {
                                this.mTargetApp = BackupManagerService.this.mPackageManager.getApplicationInfo(pkg, 0);
                                if (!this.mClearedPackages.contains(pkg)) {
                                    if (this.mTargetApp.backupAgentName == null) {
                                        Slog.d(BackupManagerService.TAG, "Clearing app data preparatory to full restore");
                                        BackupManagerService.this.clearApplicationDataSynchronous(pkg);
                                    } else {
                                        Slog.d(BackupManagerService.TAG, "backup agent (" + this.mTargetApp.backupAgentName + ") => no clear");
                                    }
                                    this.mClearedPackages.add(pkg);
                                } else {
                                    Slog.d(BackupManagerService.TAG, "We've initialized this app already; no clear required");
                                }
                                setUpPipes();
                                this.mAgent = BackupManagerService.this.bindToAgentSynchronous(this.mTargetApp, 3);
                                this.mAgentPackage = pkg;
                            } catch (PackageManager.NameNotFoundException e) {
                            } catch (IOException e2) {
                            }
                            if (this.mAgent == null) {
                                Slog.d(BackupManagerService.TAG, "Unable to create agent for " + pkg);
                                okay = false;
                                tearDownPipes();
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            }
                        }
                        if (okay && !pkg.equals(this.mAgentPackage)) {
                            Slog.e(BackupManagerService.TAG, "Restoring data for " + pkg + " but agent is for " + this.mAgentPackage);
                            okay = false;
                        }
                        if (okay) {
                            boolean agentSuccess = true;
                            long toCopy = info.size;
                            int token = BackupManagerService.this.generateToken();
                            try {
                                BackupManagerService.this.prepareOperationTimeout(token, 300000L, null);
                                if (info.domain.equals("obb")) {
                                    Slog.d(BackupManagerService.TAG, "Restoring OBB file for " + pkg + " : " + info.path);
                                    this.mObbConnection.restoreObbFile(pkg, this.mPipes[0], info.size, info.type, info.path, info.mode, info.mtime, token, BackupManagerService.this.mBackupManagerBinder);
                                } else {
                                    Slog.d(BackupManagerService.TAG, "Invoking agent to restore file " + info.path);
                                    if (this.mTargetApp.processName.equals("system")) {
                                        Slog.d(BackupManagerService.TAG, "system process agent - spinning a thread");
                                        RestoreFileRunnable runner = new RestoreFileRunnable(this.mAgent, info, this.mPipes[0], token);
                                        new Thread(runner, "restore-sys-runner").start();
                                    } else {
                                        this.mAgent.doRestoreFile(this.mPipes[0], info.size, info.type, info.domain, info.path, info.mode, info.mtime, token, BackupManagerService.this.mBackupManagerBinder);
                                    }
                                }
                            } catch (RemoteException e3) {
                                Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                agentSuccess = false;
                                okay = false;
                            } catch (IOException e4) {
                                Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                agentSuccess = false;
                                okay = false;
                            }
                            if (okay) {
                                boolean pipeOkay = true;
                                FileOutputStream pipe = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                                while (toCopy > 0) {
                                    int toRead = toCopy > ((long) buffer.length) ? buffer.length : (int) toCopy;
                                    int nRead = instream.read(buffer, 0, toRead);
                                    if (nRead >= 0) {
                                        this.mBytes += (long) nRead;
                                    }
                                    if (nRead > 0) {
                                        toCopy -= (long) nRead;
                                        if (pipeOkay) {
                                            try {
                                                pipe.write(buffer, 0, nRead);
                                            } catch (IOException e5) {
                                                Slog.e(BackupManagerService.TAG, "Failed to write to restore pipe", e5);
                                                pipeOkay = false;
                                            }
                                        }
                                    } else {
                                        skipTarPadding(info.size, instream);
                                        agentSuccess = BackupManagerService.this.waitUntilOperationComplete(token);
                                    }
                                }
                                skipTarPadding(info.size, instream);
                                agentSuccess = BackupManagerService.this.waitUntilOperationComplete(token);
                            }
                            if (!agentSuccess) {
                                Slog.d(BackupManagerService.TAG, "Agent failure restoring " + pkg + "; now ignoring");
                                BackupManagerService.this.mBackupHandler.removeMessages(7);
                                tearDownPipes();
                                tearDownAgent(this.mTargetApp, false);
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            }
                        }
                        if (!okay) {
                            Slog.d(BackupManagerService.TAG, "[discarding file content]");
                            long bytesToConsume = (info.size + 511) & (-512);
                            while (bytesToConsume > 0) {
                                int toRead2 = bytesToConsume > ((long) buffer.length) ? buffer.length : (int) bytesToConsume;
                                long nRead2 = instream.read(buffer, 0, toRead2);
                                if (nRead2 >= 0) {
                                    this.mBytes += nRead2;
                                }
                                if (nRead2 > 0) {
                                    bytesToConsume -= nRead2;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e6) {
                Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e6);
                info = null;
            }
            return info != null;
        }

        void setUpPipes() throws IOException {
            this.mPipes = ParcelFileDescriptor.createPipe();
        }

        void tearDownPipes() {
            if (this.mPipes == null) {
                return;
            }
            try {
                this.mPipes[0].close();
                this.mPipes[0] = null;
                this.mPipes[1].close();
                this.mPipes[1] = null;
            } catch (IOException e) {
                Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
            }
            this.mPipes = null;
        }

        void tearDownAgent(ApplicationInfo app, boolean doRestoreFinished) {
            if (this.mAgent == null) {
                return;
            }
            if (doRestoreFinished) {
                try {
                    int token = BackupManagerService.this.generateToken();
                    AdbRestoreFinishedLatch latch = BackupManagerService.this.new AdbRestoreFinishedLatch();
                    BackupManagerService.this.prepareOperationTimeout(token, 300000L, latch);
                    if (this.mTargetApp.processName.equals("system")) {
                        Runnable runner = new RestoreFinishedRunnable(this.mAgent, token);
                        new Thread(runner, "restore-sys-finished-runner").start();
                    } else {
                        this.mAgent.doRestoreFinished(token, BackupManagerService.this.mBackupManagerBinder);
                    }
                    latch.await();
                    BackupManagerService.this.mActivityManager.unbindBackupAgent(app);
                    if (app.uid < 10000 && !app.packageName.equals("com.android.backupconfirm")) {
                        Slog.d(BackupManagerService.TAG, "Killing host process");
                        BackupManagerService.this.mActivityManager.killApplicationProcess(app.processName, app.uid);
                    } else {
                        Slog.d(BackupManagerService.TAG, "Not killing after full restore");
                    }
                } catch (RemoteException e) {
                    Slog.d(BackupManagerService.TAG, "Lost app trying to shut down");
                }
            } else {
                BackupManagerService.this.mActivityManager.unbindBackupAgent(app);
                if (app.uid < 10000) {
                    Slog.d(BackupManagerService.TAG, "Not killing after full restore");
                }
            }
            this.mAgent = null;
        }

        class RestoreInstallObserver extends PackageInstallObserver {
            final AtomicBoolean mDone = new AtomicBoolean();
            String mPackageName;
            int mResult;

            RestoreInstallObserver() {
            }

            public void reset() {
                synchronized (this.mDone) {
                    this.mDone.set(false);
                }
            }

            public void waitForCompletion() {
                synchronized (this.mDone) {
                    while (!this.mDone.get()) {
                        try {
                            this.mDone.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            int getResult() {
                return this.mResult;
            }

            public void onPackageInstalled(String packageName, int returnCode, String msg, Bundle extras) {
                synchronized (this.mDone) {
                    this.mResult = returnCode;
                    this.mPackageName = packageName;
                    this.mDone.set(true);
                    this.mDone.notifyAll();
                }
            }
        }

        class RestoreDeleteObserver extends IPackageDeleteObserver.Stub {
            final AtomicBoolean mDone = new AtomicBoolean();
            int mResult;

            RestoreDeleteObserver() {
            }

            public void reset() {
                synchronized (this.mDone) {
                    this.mDone.set(false);
                }
            }

            public void waitForCompletion() {
                synchronized (this.mDone) {
                    while (!this.mDone.get()) {
                        try {
                            this.mDone.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            public void packageDeleted(String packageName, int returnCode) throws RemoteException {
                synchronized (this.mDone) {
                    this.mResult = returnCode;
                    this.mDone.set(true);
                    this.mDone.notifyAll();
                }
            }
        }

        boolean installApk(FileMetadata info, String installerPackage, InputStream instream) {
            boolean okay = true;
            Slog.d(BackupManagerService.TAG, "Installing from backup: " + info.packageName);
            File apkFile = new File(BackupManagerService.this.mDataDir, info.packageName);
            try {
                FileOutputStream apkStream = new FileOutputStream(apkFile);
                byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
                long size = info.size;
                while (size > 0) {
                    long toRead = ((long) buffer.length) < size ? buffer.length : size;
                    int didRead = instream.read(buffer, 0, (int) toRead);
                    if (didRead >= 0) {
                        this.mBytes += (long) didRead;
                    }
                    apkStream.write(buffer, 0, didRead);
                    size -= (long) didRead;
                }
                apkStream.close();
                apkFile.setReadable(true, false);
                Uri packageUri = Uri.fromFile(apkFile);
                this.mInstallObserver.reset();
                BackupManagerService.this.mPackageManager.installPackage(packageUri, this.mInstallObserver, 34, installerPackage);
                this.mInstallObserver.waitForCompletion();
                if (this.mInstallObserver.getResult() == 1) {
                    boolean uninstall = false;
                    if (this.mInstallObserver.mPackageName.equals(info.packageName)) {
                        try {
                            PackageInfo pkg = BackupManagerService.this.mPackageManager.getPackageInfo(info.packageName, 64);
                            if ((pkg.applicationInfo.flags & PackageManagerService.DumpState.DUMP_VERSION) == 0) {
                                Slog.w(BackupManagerService.TAG, "Restore stream contains apk of package " + info.packageName + " but it disallows backup/restore");
                                okay = false;
                            } else {
                                Signature[] sigs = this.mManifestSignatures.get(info.packageName);
                                if (!BackupManagerService.signaturesMatch(sigs, pkg)) {
                                    Slog.w(BackupManagerService.TAG, "Installed app " + info.packageName + " signatures do not match restore manifest");
                                    okay = false;
                                    uninstall = true;
                                } else if (pkg.applicationInfo.uid < 10000 && pkg.applicationInfo.backupAgentName == null) {
                                    Slog.w(BackupManagerService.TAG, "Installed app " + info.packageName + " has restricted uid and no agent");
                                    okay = false;
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(BackupManagerService.TAG, "Install of package " + info.packageName + " succeeded but now not found");
                            okay = false;
                        }
                    } else {
                        Slog.w(BackupManagerService.TAG, "Restore stream claimed to include apk for " + info.packageName + " but apk was really " + this.mInstallObserver.mPackageName);
                        okay = false;
                        uninstall = true;
                    }
                    if (uninstall) {
                        this.mDeleteObserver.reset();
                        BackupManagerService.this.mPackageManager.deletePackage(this.mInstallObserver.mPackageName, this.mDeleteObserver, 0);
                        this.mDeleteObserver.waitForCompletion();
                    }
                } else if (this.mPackagePolicies.get(info.packageName) != RestorePolicy.ACCEPT) {
                    okay = false;
                }
                return okay;
            } catch (IOException e2) {
                Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                return false;
            } finally {
                apkFile.delete();
            }
        }

        void skipTarPadding(long size, InputStream instream) throws IOException {
            long partial = (size + 512) % 512;
            if (partial <= 0) {
                return;
            }
            int needed = 512 - ((int) partial);
            byte[] buffer = new byte[needed];
            if (readExactly(instream, buffer, 0, needed) == needed) {
                this.mBytes += (long) needed;
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }

        void readMetadata(FileMetadata info, InputStream instream) throws IOException {
            if (info.size > 65536) {
                throw new IOException("Metadata too big; corrupt? size=" + info.size);
            }
            byte[] buffer = new byte[(int) info.size];
            if (readExactly(instream, buffer, 0, (int) info.size) != info.size) {
                throw new IOException("Unexpected EOF in widget data");
            }
            this.mBytes += info.size;
            String[] str = new String[1];
            int offset = extractLine(buffer, 0, str);
            int version = Integer.parseInt(str[0]);
            if (version != 1) {
                Slog.w(BackupManagerService.TAG, "Unsupported metadata version " + version);
                return;
            }
            int offset2 = extractLine(buffer, offset, str);
            String pkg = str[0];
            if (!info.packageName.equals(pkg)) {
                Slog.w(BackupManagerService.TAG, "Metadata mismatch: package " + info.packageName + " but widget data for " + pkg);
                return;
            }
            ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset2, buffer.length - offset2);
            DataInputStream in = new DataInputStream(bin);
            while (bin.available() > 0) {
                int token = in.readInt();
                int size = in.readInt();
                if (size <= 65536) {
                    switch (token) {
                        case BackupManagerService.BACKUP_WIDGET_METADATA_TOKEN:
                            this.mWidgetData = new byte[size];
                            in.read(this.mWidgetData);
                            break;
                        default:
                            Slog.i(BackupManagerService.TAG, "Ignoring metadata blob " + Integer.toHexString(token) + " for " + info.packageName);
                            in.skipBytes(size);
                            break;
                    }
                } else {
                    throw new IOException("Datum " + Integer.toHexString(token) + " too big; corrupt? size=" + info.size);
                }
            }
        }

        RestorePolicy readAppManifest(FileMetadata info, InputStream instream) throws IOException {
            if (info.size > 65536) {
                throw new IOException("Restore manifest too big; corrupt? size=" + info.size);
            }
            byte[] buffer = new byte[(int) info.size];
            if (readExactly(instream, buffer, 0, (int) info.size) != info.size) {
                throw new IOException("Unexpected EOF in manifest");
            }
            this.mBytes += info.size;
            RestorePolicy policy = RestorePolicy.IGNORE;
            String[] str = new String[1];
            try {
                int offset = extractLine(buffer, 0, str);
                int version = Integer.parseInt(str[0]);
                if (version == 1) {
                    int offset2 = extractLine(buffer, offset, str);
                    String manifestPackage = str[0];
                    if (manifestPackage.equals(info.packageName)) {
                        int offset3 = extractLine(buffer, offset2, str);
                        int version2 = Integer.parseInt(str[0]);
                        int offset4 = extractLine(buffer, offset3, str);
                        Integer.parseInt(str[0]);
                        int offset5 = extractLine(buffer, offset4, str);
                        info.installerPackageName = str[0].length() > 0 ? str[0] : null;
                        int offset6 = extractLine(buffer, offset5, str);
                        boolean hasApk = str[0].equals("1");
                        int offset7 = extractLine(buffer, offset6, str);
                        int numSigs = Integer.parseInt(str[0]);
                        if (numSigs > 0) {
                            Signature[] sigs = new Signature[numSigs];
                            for (int i = 0; i < numSigs; i++) {
                                offset7 = extractLine(buffer, offset7, str);
                                sigs[i] = new Signature(str[0]);
                            }
                            this.mManifestSignatures.put(info.packageName, sigs);
                            try {
                                PackageInfo pkgInfo = BackupManagerService.this.mPackageManager.getPackageInfo(info.packageName, 64);
                                int flags = pkgInfo.applicationInfo.flags;
                                if ((32768 & flags) == 0) {
                                    Slog.i(BackupManagerService.TAG, "Restore manifest from " + info.packageName + " but allowBackup=false");
                                } else if (pkgInfo.applicationInfo.uid < 10000 && pkgInfo.applicationInfo.backupAgentName == null) {
                                    Slog.w(BackupManagerService.TAG, "Package " + info.packageName + " is system level with no agent");
                                } else if (!BackupManagerService.signaturesMatch(sigs, pkgInfo)) {
                                    Slog.w(BackupManagerService.TAG, "Restore manifest signatures do not match installed application for " + info.packageName);
                                } else if (pkgInfo.versionCode >= version2) {
                                    Slog.i(BackupManagerService.TAG, "Sig + version match; taking data");
                                    policy = RestorePolicy.ACCEPT;
                                } else {
                                    Slog.d(BackupManagerService.TAG, "Data version " + version2 + " is newer than installed version " + pkgInfo.versionCode + " - requiring apk");
                                    policy = RestorePolicy.ACCEPT_IF_APK;
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                Slog.i(BackupManagerService.TAG, "Package " + info.packageName + " not installed; requiring apk in dataset");
                                policy = RestorePolicy.ACCEPT_IF_APK;
                            }
                            if (policy == RestorePolicy.ACCEPT_IF_APK && !hasApk) {
                                Slog.i(BackupManagerService.TAG, "Cannot restore package " + info.packageName + " without the matching .apk");
                            }
                        } else {
                            Slog.i(BackupManagerService.TAG, "Missing signature on backed-up package " + info.packageName);
                        }
                    } else {
                        Slog.i(BackupManagerService.TAG, "Expected package " + info.packageName + " but restore manifest claims " + manifestPackage);
                    }
                } else {
                    Slog.i(BackupManagerService.TAG, "Unknown restore manifest version " + version + " for package " + info.packageName);
                }
            } catch (NumberFormatException e2) {
                Slog.w(BackupManagerService.TAG, "Corrupt restore manifest for package " + info.packageName);
            } catch (IllegalArgumentException e3) {
                Slog.w(BackupManagerService.TAG, e3.getMessage());
            }
            return policy;
        }

        int extractLine(byte[] buffer, int offset, String[] outStr) throws IOException {
            int end = buffer.length;
            if (offset >= end) {
                throw new IOException("Incomplete data");
            }
            int pos = offset;
            while (pos < end) {
                byte c = buffer[pos];
                if (c == 10) {
                    break;
                }
                pos++;
            }
            outStr[0] = new String(buffer, offset, pos - offset);
            return pos + 1;
        }

        void dumpFileMetadata(FileMetadata info) {
            StringBuilder b = new StringBuilder(128);
            b.append(info.type == 2 ? 'd' : '-');
            b.append((info.mode & 256) != 0 ? 'r' : '-');
            b.append((info.mode & 128) != 0 ? 'w' : '-');
            b.append((info.mode & 64) != 0 ? 'x' : '-');
            b.append((info.mode & 32) != 0 ? 'r' : '-');
            b.append((info.mode & 16) != 0 ? 'w' : '-');
            b.append((info.mode & 8) != 0 ? 'x' : '-');
            b.append((info.mode & 4) == 0 ? '-' : 'r');
            b.append((info.mode & 2) == 0 ? '-' : 'w');
            b.append((info.mode & 1) == 0 ? '-' : 'x');
            b.append(String.format(" %9d ", Long.valueOf(info.size)));
            Date stamp = new Date(info.mtime);
            b.append(new SimpleDateFormat("MMM dd HH:mm:ss ").format(stamp));
            b.append(info.packageName);
            b.append(" :: ");
            b.append(info.domain);
            b.append(" :: ");
            b.append(info.path);
            Slog.i(BackupManagerService.TAG, b.toString());
        }

        FileMetadata readTarHeaders(InputStream instream) throws IOException {
            FileMetadata info;
            byte[] block = new byte[512];
            if (!readTarHeader(instream, block)) {
                return null;
            }
            try {
                info = new FileMetadata();
            } catch (IOException e) {
                e = e;
            }
            try {
                info.size = extractRadix(block, 124, 12, 8);
                info.mtime = extractRadix(block, 136, 12, 8);
                info.mode = extractRadix(block, 100, 8, 8);
                info.path = extractString(block, 345, 155);
                String path = extractString(block, 0, 100);
                if (path.length() > 0) {
                    if (info.path.length() > 0) {
                        info.path += '/';
                    }
                    info.path += path;
                }
                int typeChar = block[156];
                if (typeChar == 120) {
                    boolean gotHeader = readPaxExtendedHeader(instream, info);
                    if (gotHeader) {
                        gotHeader = readTarHeader(instream, block);
                    }
                    if (!gotHeader) {
                        throw new IOException("Bad or missing pax header");
                    }
                    typeChar = block[156];
                }
                switch (typeChar) {
                    case 0:
                        Slog.w(BackupManagerService.TAG, "Saw type=0 in tar header block, info=" + info);
                        return null;
                    case 48:
                        info.type = 1;
                        break;
                    case 53:
                        info.type = 2;
                        if (info.size != 0) {
                            Slog.w(BackupManagerService.TAG, "Directory entry with nonzero size in header");
                            info.size = 0L;
                        }
                        break;
                    default:
                        Slog.e(BackupManagerService.TAG, "Unknown tar entity type: " + typeChar);
                        throw new IOException("Unknown entity type " + typeChar);
                }
                if ("shared/".regionMatches(0, info.path, 0, "shared/".length())) {
                    info.path = info.path.substring("shared/".length());
                    info.packageName = BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                    info.domain = "shared";
                    Slog.i(BackupManagerService.TAG, "File in shared storage: " + info.path);
                } else if ("apps/".regionMatches(0, info.path, 0, "apps/".length())) {
                    info.path = info.path.substring("apps/".length());
                    int slash = info.path.indexOf(47);
                    if (slash < 0) {
                        throw new IOException("Illegal semantic path in " + info.path);
                    }
                    info.packageName = info.path.substring(0, slash);
                    info.path = info.path.substring(slash + 1);
                    if (!info.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME) && !info.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        int slash2 = info.path.indexOf(47);
                        if (slash2 < 0) {
                            throw new IOException("Illegal semantic path in non-manifest " + info.path);
                        }
                        info.domain = info.path.substring(0, slash2);
                        info.path = info.path.substring(slash2 + 1);
                    }
                }
                return info;
            } catch (IOException e2) {
                e = e2;
                Slog.e(BackupManagerService.TAG, "Parse error in header: " + e.getMessage());
                HEXLOG(block);
                throw e;
            }
        }

        private void HEXLOG(byte[] block) {
            int offset = 0;
            int todo = block.length;
            StringBuilder buf = new StringBuilder(64);
            while (todo > 0) {
                buf.append(String.format("%04x   ", Integer.valueOf(offset)));
                int numThisLine = todo > 16 ? 16 : todo;
                for (int i = 0; i < numThisLine; i++) {
                    buf.append(String.format("%02x ", Byte.valueOf(block[offset + i])));
                }
                Slog.i("hexdump", buf.toString());
                buf.setLength(0);
                todo -= numThisLine;
                offset += numThisLine;
            }
        }

        int readExactly(InputStream in, byte[] buffer, int offset, int size) throws IOException {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            int soFar = 0;
            while (soFar < size) {
                int nRead = in.read(buffer, offset + soFar, size - soFar);
                if (nRead <= 0) {
                    break;
                }
                soFar += nRead;
            }
            return soFar;
        }

        boolean readTarHeader(InputStream instream, byte[] block) throws IOException {
            int got = readExactly(instream, block, 0, 512);
            if (got == 0) {
                return false;
            }
            if (got < 512) {
                throw new IOException("Unable to read full block header");
            }
            this.mBytes += 512;
            return true;
        }

        boolean readPaxExtendedHeader(InputStream instream, FileMetadata info) throws IOException {
            if (info.size > 32768) {
                Slog.w(BackupManagerService.TAG, "Suspiciously large pax header size " + info.size + " - aborting");
                throw new IOException("Sanity failure: pax header size " + info.size);
            }
            int numBlocks = (int) ((info.size + 511) >> 9);
            byte[] data = new byte[numBlocks * 512];
            if (readExactly(instream, data, 0, data.length) < data.length) {
                throw new IOException("Unable to read full pax header");
            }
            this.mBytes += (long) data.length;
            int contentSize = (int) info.size;
            int offset = 0;
            do {
                int eol = offset + 1;
                while (eol < contentSize && data[eol] != 32) {
                    eol++;
                }
                if (eol >= contentSize) {
                    throw new IOException("Invalid pax data");
                }
                int linelen = (int) extractRadix(data, offset, eol - offset, 10);
                int key = eol + 1;
                int eol2 = (offset + linelen) - 1;
                int value = key + 1;
                while (data[value] != 61 && value <= eol2) {
                    value++;
                }
                if (value > eol2) {
                    throw new IOException("Invalid pax declaration");
                }
                String keyStr = new String(data, key, value - key, "UTF-8");
                String valStr = new String(data, value + 1, (eol2 - value) - 1, "UTF-8");
                if ("path".equals(keyStr)) {
                    info.path = valStr;
                } else if ("size".equals(keyStr)) {
                    info.size = Long.parseLong(valStr);
                } else {
                    Slog.i(BackupManagerService.TAG, "Unhandled pax key: " + key);
                }
                offset += linelen;
            } while (offset < contentSize);
            return true;
        }

        long extractRadix(byte[] data, int offset, int maxChars, int radix) throws IOException {
            long value = 0;
            int end = offset + maxChars;
            for (int i = offset; i < end; i++) {
                byte b = data[i];
                if (b == 0 || b == 32) {
                    break;
                }
                if (b < 48 || b > (radix + 48) - 1) {
                    throw new IOException("Invalid number in header: '" + ((char) b) + "' for radix " + radix);
                }
                value = (((long) radix) * value) + ((long) (b - 48));
            }
            return value;
        }

        String extractString(byte[] data, int offset, int maxChars) throws IOException {
            int end = offset + maxChars;
            int eos = offset;
            while (eos < end && data[eos] != 0) {
                eos++;
            }
            return new String(data, offset, eos - offset, "US-ASCII");
        }

        void sendStartRestore() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onStartRestore();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: startRestore");
                this.mObserver = null;
            }
        }

        void sendOnRestorePackage(String name) {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onRestorePackage(name);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }

        void sendEndRestore() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onEndRestore();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }

    static boolean signaturesMatch(Signature[] storedSigs, PackageInfo target) {
        if (target == null) {
            return false;
        }
        if ((target.applicationInfo.flags & 1) != 0) {
            return true;
        }
        Signature[] deviceSigs = target.signatures;
        if ((storedSigs == null || storedSigs.length == 0) && (deviceSigs == null || deviceSigs.length == 0)) {
            return true;
        }
        if (storedSigs == null || deviceSigs == null) {
            return false;
        }
        int nDevice = deviceSigs.length;
        for (Signature signature : storedSigs) {
            boolean match = false;
            int j = 0;
            while (true) {
                if (j >= nDevice) {
                    break;
                }
                if (!signature.equals(deviceSigs[j])) {
                    j++;
                } else {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    void restoreWidgetData(String packageName, byte[] widgetData) {
        AppWidgetBackupBridge.restoreWidgetState(packageName, widgetData, 0);
    }

    enum UnifiedRestoreState {
        INITIAL,
        RUNNING_QUEUE,
        RESTORE_KEYVALUE,
        RESTORE_FULL,
        RESTORE_FINISHED,
        FINAL;

        public static UnifiedRestoreState[] valuesCustom() {
            return values();
        }
    }

    class PerformUnifiedRestoreTask implements BackupRestoreTask {

        private static final int[] f6x9e9aa8d7 = null;
        final int[] $SWITCH_TABLE$com$android$server$backup$BackupManagerService$UnifiedRestoreState;
        private List<PackageInfo> mAcceptSet;
        private IBackupAgent mAgent;
        ParcelFileDescriptor mBackupData;
        private File mBackupDataName;
        private int mCount;
        private PackageInfo mCurrentPackage;
        private boolean mIsSystemRestore;
        ParcelFileDescriptor mNewState;
        private File mNewStateName;
        private IRestoreObserver mObserver;
        private PackageManagerBackupAgent mPmAgent;
        private int mPmToken;
        private RestoreDescription mRestoreDescription;
        private File mSavedStateName;
        private File mStageName;
        File mStateDir;
        private int mStatus;
        private PackageInfo mTargetPackage;
        private long mToken;
        private IBackupTransport mTransport;
        private byte[] mWidgetData;
        private UnifiedRestoreState mState = UnifiedRestoreState.INITIAL;
        private long mStartRealtime = SystemClock.elapsedRealtime();
        private boolean mFinished = false;
        private boolean mDidLaunch = false;

        private static int[] m1170x56f894b3() {
            if (f6x9e9aa8d7 != null) {
                return f6x9e9aa8d7;
            }
            int[] iArr = new int[UnifiedRestoreState.valuesCustom().length];
            try {
                iArr[UnifiedRestoreState.FINAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[UnifiedRestoreState.INITIAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[UnifiedRestoreState.RESTORE_FINISHED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[UnifiedRestoreState.RESTORE_FULL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[UnifiedRestoreState.RESTORE_KEYVALUE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[UnifiedRestoreState.RUNNING_QUEUE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            f6x9e9aa8d7 = iArr;
            return iArr;
        }

        PerformUnifiedRestoreTask(IBackupTransport transport, IRestoreObserver observer, long restoreSetToken, PackageInfo targetPackage, int pmToken, boolean isFullSystemRestore, String[] filterSet) {
            this.mTransport = transport;
            this.mObserver = observer;
            this.mToken = restoreSetToken;
            this.mPmToken = pmToken;
            this.mTargetPackage = targetPackage;
            this.mIsSystemRestore = isFullSystemRestore;
            if (targetPackage != null) {
                this.mAcceptSet = new ArrayList();
                this.mAcceptSet.add(targetPackage);
                return;
            }
            if (filterSet == null) {
                List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(BackupManagerService.this.mPackageManager);
                filterSet = packagesToNames(apps);
                Slog.i(BackupManagerService.TAG, "Full restore; asking about " + filterSet.length + " apps");
            }
            this.mAcceptSet = new ArrayList(filterSet.length);
            boolean hasSystem = false;
            boolean hasSettings = false;
            for (String str : filterSet) {
                try {
                    PackageInfo info = BackupManagerService.this.mPackageManager.getPackageInfo(str, 0);
                    if ("android".equals(info.packageName)) {
                        hasSystem = true;
                    } else if (BackupManagerService.SETTINGS_PACKAGE.equals(info.packageName)) {
                        hasSettings = true;
                    } else if (BackupManagerService.appIsEligibleForBackup(info.applicationInfo)) {
                        this.mAcceptSet.add(info);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (hasSystem) {
                try {
                    this.mAcceptSet.add(0, BackupManagerService.this.mPackageManager.getPackageInfo("android", 0));
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            if (!hasSettings) {
                return;
            }
            try {
                this.mAcceptSet.add(BackupManagerService.this.mPackageManager.getPackageInfo(BackupManagerService.SETTINGS_PACKAGE, 0));
            } catch (PackageManager.NameNotFoundException e3) {
            }
        }

        private String[] packagesToNames(List<PackageInfo> apps) {
            int N = apps.size();
            String[] names = new String[N];
            for (int i = 0; i < N; i++) {
                names[i] = apps.get(i).packageName;
            }
            return names;
        }

        @Override
        public void execute() {
            switch (m1170x56f894b3()[this.mState.ordinal()]) {
                case 1:
                    if (this.mFinished) {
                        Slog.e(BackupManagerService.TAG, "Duplicate finish");
                    } else {
                        finalizeRestore();
                    }
                    this.mFinished = true;
                    break;
                case 2:
                    startRestore();
                    break;
                case 3:
                    restoreFinished();
                    break;
                case 4:
                    restoreFull();
                    break;
                case 5:
                    restoreKeyValue();
                    break;
                case 6:
                    dispatchNextRestore();
                    break;
            }
        }

        private void startRestore() {
            sendStartRestore(this.mAcceptSet.size());
            if (this.mIsSystemRestore) {
                AppWidgetBackupBridge.restoreStarting(0);
            }
            try {
                String transportDir = this.mTransport.transportDirName();
                this.mStateDir = new File(BackupManagerService.this.mBaseStateDir, transportDir);
                PackageInfo pmPackage = new PackageInfo();
                pmPackage.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
                this.mAcceptSet.add(0, pmPackage);
                PackageInfo[] packages = (PackageInfo[]) this.mAcceptSet.toArray(new PackageInfo[0]);
                this.mStatus = this.mTransport.startRestore(this.mToken, packages);
                if (this.mStatus != 0) {
                    Slog.e(BackupManagerService.TAG, "Transport error " + this.mStatus + "; no restore possible");
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    executeNextState(UnifiedRestoreState.FINAL);
                    return;
                }
                RestoreDescription desc = this.mTransport.nextRestorePackage();
                if (desc == null) {
                    Slog.e(BackupManagerService.TAG, "No restore metadata available; halting");
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    executeNextState(UnifiedRestoreState.FINAL);
                    return;
                }
                if (!BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(desc.getPackageName())) {
                    Slog.e(BackupManagerService.TAG, "Required metadata but got " + desc.getPackageName());
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    executeNextState(UnifiedRestoreState.FINAL);
                    return;
                }
                this.mCurrentPackage = new PackageInfo();
                this.mCurrentPackage.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
                this.mPmAgent = new PackageManagerBackupAgent(BackupManagerService.this.mPackageManager, null);
                this.mAgent = IBackupAgent.Stub.asInterface(this.mPmAgent.onBind());
                initiateOneRestore(this.mCurrentPackage, 0);
                BackupManagerService.this.mBackupHandler.removeMessages(7);
                if (this.mPmAgent.hasMetadata()) {
                    return;
                }
                Slog.e(BackupManagerService.TAG, "No restore metadata available, so not restoring");
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, BackupManagerService.PACKAGE_MANAGER_SENTINEL, "Package manager restore metadata missing");
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                BackupManagerService.this.mBackupHandler.removeMessages(20, this);
                executeNextState(UnifiedRestoreState.FINAL);
            } catch (RemoteException e) {
                Slog.e(BackupManagerService.TAG, "Unable to contact transport for restore");
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                BackupManagerService.this.mBackupHandler.removeMessages(20, this);
                executeNextState(UnifiedRestoreState.FINAL);
            }
        }

        private void dispatchNextRestore() {
            UnifiedRestoreState nextState;
            UnifiedRestoreState nextState2 = UnifiedRestoreState.FINAL;
            try {
                this.mRestoreDescription = this.mTransport.nextRestorePackage();
                String packageName = this.mRestoreDescription != null ? this.mRestoreDescription.getPackageName() : null;
                if (packageName == null) {
                    Slog.e(BackupManagerService.TAG, "Failure getting next package name");
                    EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    nextState2 = UnifiedRestoreState.FINAL;
                    return;
                }
                if (this.mRestoreDescription == RestoreDescription.NO_MORE_PACKAGES) {
                    Slog.v(BackupManagerService.TAG, "No more packages; finishing restore");
                    int millis = (int) (SystemClock.elapsedRealtime() - this.mStartRealtime);
                    EventLog.writeEvent(EventLogTags.RESTORE_SUCCESS, Integer.valueOf(this.mCount), Integer.valueOf(millis));
                    nextState2 = UnifiedRestoreState.FINAL;
                    return;
                }
                Slog.i(BackupManagerService.TAG, "Next restore package: " + this.mRestoreDescription);
                sendOnRestorePackage(packageName);
                PackageManagerBackupAgent.Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
                if (metaInfo == null) {
                    Slog.e(BackupManagerService.TAG, "No metadata for " + packageName);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package metadata missing");
                    nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                    return;
                }
                try {
                    this.mCurrentPackage = BackupManagerService.this.mPackageManager.getPackageInfo(packageName, 64);
                    if (metaInfo.versionCode > this.mCurrentPackage.versionCode) {
                        if ((this.mCurrentPackage.applicationInfo.flags & PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0) {
                            String message = "Version " + metaInfo.versionCode + " > installed version " + this.mCurrentPackage.versionCode;
                            Slog.w(BackupManagerService.TAG, "Package " + packageName + ": " + message);
                            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, message);
                            nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                            return;
                        }
                        Slog.v(BackupManagerService.TAG, "Version " + metaInfo.versionCode + " > installed " + this.mCurrentPackage.versionCode + " but restoreAnyVersion");
                    }
                    this.mWidgetData = null;
                    int type = this.mRestoreDescription.getDataType();
                    if (type == 1) {
                        nextState = UnifiedRestoreState.RESTORE_KEYVALUE;
                    } else {
                        if (type != 2) {
                            Slog.e(BackupManagerService.TAG, "Unrecognized restore type " + type);
                            nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                            return;
                        }
                        nextState = UnifiedRestoreState.RESTORE_FULL;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(BackupManagerService.TAG, "Package not present: " + packageName);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package missing on device");
                    nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                }
            } catch (RemoteException e2) {
                Slog.e(BackupManagerService.TAG, "Can't get next target from transport; ending restore");
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                nextState2 = UnifiedRestoreState.FINAL;
            } finally {
                executeNextState(nextState2);
            }
        }

        private void restoreKeyValue() {
            String packageName = this.mCurrentPackage.packageName;
            if (this.mCurrentPackage.applicationInfo.backupAgentName == null || "".equals(this.mCurrentPackage.applicationInfo.backupAgentName)) {
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package has no agent");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                return;
            }
            PackageManagerBackupAgent.Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
            if (!BackupUtils.signaturesMatch(metaInfo.sigHashes, this.mCurrentPackage)) {
                Slog.w(BackupManagerService.TAG, "Signature mismatch restoring " + packageName);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Signature mismatch");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                return;
            }
            this.mAgent = BackupManagerService.this.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
            if (this.mAgent == null) {
                Slog.w(BackupManagerService.TAG, "Can't find backup agent for " + packageName);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Restore agent missing");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                return;
            }
            this.mDidLaunch = true;
            try {
                initiateOneRestore(this.mCurrentPackage, metaInfo.versionCode);
                this.mCount++;
            } catch (Exception e) {
                Slog.e(BackupManagerService.TAG, "Error when attempting restore: " + e.toString());
                keyValueAgentErrorCleanup();
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
        }

        void initiateOneRestore(PackageInfo app, int appVersionCode) {
            String packageName = app.packageName;
            Slog.d(BackupManagerService.TAG, "initiateOneRestore packageName=" + packageName);
            this.mBackupDataName = new File(BackupManagerService.this.mDataDir, packageName + ".restore");
            this.mStageName = new File(BackupManagerService.this.mDataDir, packageName + ".stage");
            this.mNewStateName = new File(this.mStateDir, packageName + ".new");
            this.mSavedStateName = new File(this.mStateDir, packageName);
            boolean staging = !packageName.equals("android");
            File downloadFile = staging ? this.mStageName : this.mBackupDataName;
            int token = BackupManagerService.this.generateToken();
            try {
                ParcelFileDescriptor stage = ParcelFileDescriptor.open(downloadFile, 1006632960);
                if (this.mTransport.getRestoreData(stage) != 0) {
                    Slog.e(BackupManagerService.TAG, "Error getting restore data for " + packageName);
                    EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    stage.close();
                    downloadFile.delete();
                    executeNextState(UnifiedRestoreState.FINAL);
                    return;
                }
                if (staging) {
                    stage.close();
                    stage = ParcelFileDescriptor.open(downloadFile, 268435456);
                    this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                    BackupDataInput in = new BackupDataInput(stage.getFileDescriptor());
                    BackupDataOutput out = new BackupDataOutput(this.mBackupData.getFileDescriptor());
                    byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_PREFERRED_XML];
                    while (in.readNextHeader()) {
                        String key = in.getKey();
                        int size = in.getDataSize();
                        if (key.equals(BackupManagerService.KEY_WIDGET_STATE)) {
                            Slog.i(BackupManagerService.TAG, "Restoring widget state for " + packageName);
                            this.mWidgetData = new byte[size];
                            in.readEntityData(this.mWidgetData, 0, size);
                        } else {
                            if (size > buffer.length) {
                                buffer = new byte[size];
                            }
                            in.readEntityData(buffer, 0, size);
                            out.writeEntityHeader(key, size);
                            out.writeEntityData(buffer, size);
                        }
                    }
                    this.mBackupData.close();
                }
                stage.close();
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
                BackupManagerService.this.prepareOperationTimeout(token, 60000L, this);
                this.mAgent.doRestore(this.mBackupData, appVersionCode, this.mNewState, token, BackupManagerService.this.mBackupManagerBinder);
            } catch (Exception e) {
                Slog.e(BackupManagerService.TAG, "Unable to call app for restore: " + packageName, e);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, e.toString());
                keyValueAgentErrorCleanup();
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
        }

        private void restoreFull() {
            try {
                StreamFeederThread feeder = new StreamFeederThread();
                new Thread(feeder, "unified-stream-feeder").start();
            } catch (IOException e) {
                Slog.e(BackupManagerService.TAG, "Unable to construct pipes for stream restore!");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
        }

        private void restoreFinished() {
            try {
                int token = BackupManagerService.this.generateToken();
                BackupManagerService.this.prepareOperationTimeout(token, 30000L, this);
                this.mAgent.doRestoreFinished(token, BackupManagerService.this.mBackupManagerBinder);
            } catch (Exception e) {
                String packageName = this.mCurrentPackage.packageName;
                Slog.e(BackupManagerService.TAG, "Unable to finalize restore of " + packageName);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, e.toString());
                keyValueAgentErrorCleanup();
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
        }

        class StreamFeederThread extends RestoreEngine implements Runnable, BackupRestoreTask {
            final String TAG;
            FullRestoreEngine mEngine;
            ParcelFileDescriptor[] mEnginePipes;
            EngineThread mEngineThread;
            ParcelFileDescriptor[] mTransportPipes;

            public StreamFeederThread() throws IOException {
                super();
                this.TAG = "StreamFeederThread";
                this.mTransportPipes = ParcelFileDescriptor.createPipe();
                this.mEnginePipes = ParcelFileDescriptor.createPipe();
                setRunning(true);
            }

            @Override
            public void run() {
                UnifiedRestoreState nextState;
                UnifiedRestoreState nextState2;
                UnifiedRestoreState unifiedRestoreState = UnifiedRestoreState.RUNNING_QUEUE;
                int status = 0;
                EventLog.writeEvent(EventLogTags.FULL_RESTORE_PACKAGE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                this.mEngine = BackupManagerService.this.new FullRestoreEngine(this, null, PerformUnifiedRestoreTask.this.mCurrentPackage, false, false);
                this.mEngineThread = PerformUnifiedRestoreTask.this.new EngineThread(this.mEngine, this.mEnginePipes[0]);
                ParcelFileDescriptor eWriteEnd = this.mEnginePipes[1];
                ParcelFileDescriptor tReadEnd = this.mTransportPipes[0];
                ParcelFileDescriptor tWriteEnd = this.mTransportPipes[1];
                int bufferSize = PackageManagerService.DumpState.DUMP_VERSION;
                byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
                FileOutputStream engineOut = new FileOutputStream(eWriteEnd.getFileDescriptor());
                FileInputStream transportIn = new FileInputStream(tReadEnd.getFileDescriptor());
                new Thread(this.mEngineThread, "unified-restore-engine").start();
                while (true) {
                    if (status != 0) {
                        break;
                    }
                    try {
                        try {
                            try {
                                int result = PerformUnifiedRestoreTask.this.mTransport.getNextFullRestoreDataChunk(tWriteEnd);
                                if (result > 0) {
                                    if (result > bufferSize) {
                                        bufferSize = result;
                                        buffer = new byte[result];
                                    }
                                    int toCopy = result;
                                    while (toCopy > 0) {
                                        int n = transportIn.read(buffer, 0, toCopy);
                                        engineOut.write(buffer, 0, n);
                                        toCopy -= n;
                                    }
                                } else if (result == -1) {
                                    status = 0;
                                    break;
                                } else {
                                    Slog.e("StreamFeederThread", "Error " + result + " streaming restore for " + PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                                    EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                                    status = result;
                                }
                            } catch (RemoteException e) {
                                Slog.e("StreamFeederThread", "Transport failed during restore");
                                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                                int status2 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                IoUtils.closeQuietly(this.mEnginePipes[1]);
                                IoUtils.closeQuietly(this.mTransportPipes[0]);
                                IoUtils.closeQuietly(this.mTransportPipes[1]);
                                this.mEngineThread.waitForResult();
                                IoUtils.closeQuietly(this.mEnginePipes[0]);
                                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                                try {
                                    PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                                } catch (RemoteException e2) {
                                    status2 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                }
                                BackupManagerService.this.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                                UnifiedRestoreState nextState3 = status2 == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                                PerformUnifiedRestoreTask.this.executeNextState(nextState3);
                                setRunning(false);
                                return;
                            }
                        } catch (IOException e3) {
                            Slog.e("StreamFeederThread", "Unable to route data for restore");
                            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, "I/O error on pipes");
                            int status3 = -1003;
                            IoUtils.closeQuietly(this.mEnginePipes[1]);
                            IoUtils.closeQuietly(this.mTransportPipes[0]);
                            IoUtils.closeQuietly(this.mTransportPipes[1]);
                            this.mEngineThread.waitForResult();
                            IoUtils.closeQuietly(this.mEnginePipes[0]);
                            PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                            try {
                                PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                            } catch (RemoteException e4) {
                                status3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                            }
                            BackupManagerService.this.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                            UnifiedRestoreState nextState4 = status3 == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                            PerformUnifiedRestoreTask.this.executeNextState(nextState4);
                            setRunning(false);
                            return;
                        }
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(this.mEnginePipes[1]);
                        IoUtils.closeQuietly(this.mTransportPipes[0]);
                        IoUtils.closeQuietly(this.mTransportPipes[1]);
                        this.mEngineThread.waitForResult();
                        IoUtils.closeQuietly(this.mEnginePipes[0]);
                        PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                        if (status == 0) {
                            nextState2 = UnifiedRestoreState.RESTORE_FINISHED;
                            PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                            PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                        } else {
                            try {
                                PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                            } catch (RemoteException e5) {
                                status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                            }
                            BackupManagerService.this.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                            nextState2 = status == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                        }
                        PerformUnifiedRestoreTask.this.executeNextState(nextState2);
                        setRunning(false);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (status == 0) {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                } else {
                    try {
                        PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                    } catch (RemoteException e6) {
                        status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    BackupManagerService.this.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                    nextState = status == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                }
                PerformUnifiedRestoreTask.this.executeNextState(nextState);
                setRunning(false);
            }

            @Override
            public void execute() {
            }

            @Override
            public void operationComplete(long result) {
            }

            @Override
            public void handleTimeout() {
                Slog.w("StreamFeederThread", "Full-data restore target timed out; shutting down");
                this.mEngineThread.handleTimeout();
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                this.mEnginePipes[1] = null;
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                this.mEnginePipes[0] = null;
            }
        }

        class EngineThread implements Runnable {
            FullRestoreEngine mEngine;
            FileInputStream mEngineStream;

            EngineThread(FullRestoreEngine engine, ParcelFileDescriptor engineSocket) {
                this.mEngine = engine;
                engine.setRunning(true);
                this.mEngineStream = new FileInputStream(engineSocket.getFileDescriptor(), true);
            }

            public boolean isRunning() {
                return this.mEngine.isRunning();
            }

            public int waitForResult() {
                return this.mEngine.waitForResult();
            }

            @Override
            public void run() {
                while (this.mEngine.isRunning()) {
                    try {
                        this.mEngine.restoreOneFile(this.mEngineStream, false);
                    } finally {
                        IoUtils.closeQuietly(this.mEngineStream);
                    }
                }
            }

            public void handleTimeout() {
                IoUtils.closeQuietly(this.mEngineStream);
                this.mEngine.handleTimeout();
            }
        }

        private void finalizeRestore() {
            try {
                this.mTransport.finishRestore();
            } catch (Exception e) {
                Slog.e(BackupManagerService.TAG, "Error finishing restore", e);
            }
            if (this.mObserver != null) {
                try {
                    this.mObserver.restoreFinished(this.mStatus);
                } catch (RemoteException e2) {
                    Slog.d(BackupManagerService.TAG, "Restore observer died at restoreFinished");
                }
            }
            BackupManagerService.this.mBackupHandler.removeMessages(8);
            if (this.mPmToken > 0) {
                try {
                    BackupManagerService.this.mPackageManagerBinder.finishPackageInstall(this.mPmToken, this.mDidLaunch);
                } catch (RemoteException e3) {
                }
            } else {
                BackupManagerService.this.mBackupHandler.sendEmptyMessageDelayed(8, 60000L);
            }
            AppWidgetBackupBridge.restoreFinished(0);
            if (this.mIsSystemRestore && this.mPmAgent != null) {
                BackupManagerService.this.mAncestralPackages = this.mPmAgent.getRestoredPackages();
                BackupManagerService.this.mAncestralToken = this.mToken;
                BackupManagerService.this.writeRestoreTokens();
            }
            Slog.i(BackupManagerService.TAG, "Restore complete.");
            BackupManagerService.this.mWakelock.release();
        }

        void keyValueAgentErrorCleanup() {
            BackupManagerService.this.clearApplicationDataSynchronous(this.mCurrentPackage.packageName);
            keyValueAgentCleanup();
        }

        void keyValueAgentCleanup() {
            boolean killAfterRestore;
            this.mBackupDataName.delete();
            this.mStageName.delete();
            try {
                if (this.mBackupData != null) {
                    this.mBackupData.close();
                }
            } catch (IOException e) {
            }
            try {
                if (this.mNewState != null) {
                    this.mNewState.close();
                }
            } catch (IOException e2) {
            }
            this.mNewState = null;
            this.mBackupData = null;
            this.mNewStateName.delete();
            if (this.mCurrentPackage.applicationInfo != null) {
                try {
                    BackupManagerService.this.mActivityManager.unbindBackupAgent(this.mCurrentPackage.applicationInfo);
                    int appFlags = this.mCurrentPackage.applicationInfo.flags;
                    if (this.mCurrentPackage.applicationInfo.uid < 10000) {
                        killAfterRestore = false;
                    } else {
                        killAfterRestore = this.mRestoreDescription.getDataType() == 2 || (65536 & appFlags) != 0;
                    }
                    if (this.mTargetPackage == null && killAfterRestore) {
                        Slog.d(BackupManagerService.TAG, "Restore complete, killing host process of " + this.mCurrentPackage.applicationInfo.processName);
                        BackupManagerService.this.mActivityManager.killApplicationProcess(this.mCurrentPackage.applicationInfo.processName, this.mCurrentPackage.applicationInfo.uid);
                    }
                } catch (RemoteException e3) {
                }
            }
            BackupManagerService.this.mBackupHandler.removeMessages(7, this);
            synchronized (BackupManagerService.this.mCurrentOpLock) {
                BackupManagerService.this.mCurrentOperations.clear();
            }
        }

        @Override
        public void operationComplete(long unusedResult) {
            UnifiedRestoreState nextState;
            switch (m1170x56f894b3()[this.mState.ordinal()]) {
                case 2:
                    nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    break;
                case 3:
                    int size = (int) this.mBackupDataName.length();
                    EventLog.writeEvent(EventLogTags.RESTORE_PACKAGE, this.mCurrentPackage.packageName, Integer.valueOf(size));
                    keyValueAgentCleanup();
                    if (this.mWidgetData != null) {
                        BackupManagerService.this.restoreWidgetData(this.mCurrentPackage.packageName, this.mWidgetData);
                    }
                    nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    break;
                case 4:
                case 5:
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    break;
                default:
                    Slog.e(BackupManagerService.TAG, "Unexpected restore callback into state " + this.mState);
                    keyValueAgentErrorCleanup();
                    nextState = UnifiedRestoreState.FINAL;
                    break;
            }
            executeNextState(nextState);
        }

        @Override
        public void handleTimeout() {
            Slog.e(BackupManagerService.TAG, "Timeout restoring application " + this.mCurrentPackage.packageName);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, this.mCurrentPackage.packageName, "restore timeout");
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }

        void executeNextState(UnifiedRestoreState nextState) {
            this.mState = nextState;
            Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(20, this);
            BackupManagerService.this.mBackupHandler.sendMessage(msg);
        }

        void sendStartRestore(int numPackages) {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.restoreStarting(numPackages);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: startRestore");
                this.mObserver = null;
            }
        }

        void sendOnRestorePackage(String name) {
            if (this.mObserver == null || this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onUpdate(this.mCount, name);
            } catch (RemoteException e) {
                Slog.d(BackupManagerService.TAG, "Restore observer died in onUpdate");
                this.mObserver = null;
            }
        }

        void sendEndRestore() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }

    class PerformClearTask implements Runnable {
        PackageInfo mPackage;
        IBackupTransport mTransport;

        PerformClearTask(IBackupTransport transport, PackageInfo packageInfo) {
            this.mTransport = transport;
            this.mPackage = packageInfo;
        }

        @Override
        public void run() {
            try {
                try {
                    File stateDir = new File(BackupManagerService.this.mBaseStateDir, this.mTransport.transportDirName());
                    File stateFile = new File(stateDir, this.mPackage.packageName);
                    stateFile.delete();
                    this.mTransport.clearBackupData(this.mPackage);
                } finally {
                    try {
                        this.mTransport.finishBackup();
                    } catch (RemoteException e) {
                    }
                    BackupManagerService.this.mWakelock.release();
                }
            } catch (RemoteException e2) {
                try {
                    this.mTransport.finishBackup();
                } catch (RemoteException e3) {
                }
                BackupManagerService.this.mWakelock.release();
            } catch (Exception e4) {
                Slog.e(BackupManagerService.TAG, "Transport threw attempting to clear data for " + this.mPackage);
                try {
                    this.mTransport.finishBackup();
                } catch (RemoteException e5) {
                }
                BackupManagerService.this.mWakelock.release();
            }
        }
    }

    class PerformInitializeTask implements Runnable {
        HashSet<String> mQueue;

        PerformInitializeTask(HashSet<String> transportNames) {
            this.mQueue = transportNames;
        }

        @Override
        public void run() {
            try {
                for (String transportName : this.mQueue) {
                    IBackupTransport transport = BackupManagerService.this.getTransport(transportName);
                    if (transport == null) {
                        Slog.e(BackupManagerService.TAG, "Requested init for " + transportName + " but not found");
                    } else {
                        Slog.i(BackupManagerService.TAG, "Initializing (wiping) backup transport storage: " + transportName);
                        EventLog.writeEvent(EventLogTags.BACKUP_START, transport.transportDirName());
                        long startRealtime = SystemClock.elapsedRealtime();
                        int status = transport.initializeDevice();
                        if (status == 0) {
                            status = transport.finishBackup();
                        }
                        if (status == 0) {
                            Slog.i(BackupManagerService.TAG, "Device init successful");
                            int millis = (int) (SystemClock.elapsedRealtime() - startRealtime);
                            EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                            BackupManagerService.this.resetBackupState(new File(BackupManagerService.this.mBaseStateDir, transport.transportDirName()));
                            EventLog.writeEvent(EventLogTags.BACKUP_SUCCESS, 0, Integer.valueOf(millis));
                            synchronized (BackupManagerService.this.mQueueLock) {
                                BackupManagerService.this.recordInitPendingLocked(false, transportName);
                            }
                        } else {
                            Slog.e(BackupManagerService.TAG, "Transport error in initializeDevice()");
                            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                            synchronized (BackupManagerService.this.mQueueLock) {
                                BackupManagerService.this.recordInitPendingLocked(true, transportName);
                            }
                            long delay = transport.requestBackupTime();
                            Slog.w(BackupManagerService.TAG, "Init failed on " + transportName + " resched in " + delay);
                            BackupManagerService.this.mAlarmManager.set(0, System.currentTimeMillis() + delay, BackupManagerService.this.mRunInitIntent);
                        }
                    }
                }
            } catch (RemoteException e) {
            } catch (Exception e2) {
                Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e2);
            } finally {
                BackupManagerService.this.mWakelock.release();
            }
        }
    }

    private void dataChangedImpl(String packageName) {
        HashSet<String> targets = dataChangedTargets(packageName);
        dataChangedImpl(packageName, targets);
    }

    private void dataChangedImpl(String packageName, HashSet<String> targets) {
        if (targets == null) {
            Slog.w(TAG, "dataChanged but no participant pkg='" + packageName + "' uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mQueueLock) {
            if (targets.contains(packageName)) {
                BackupRequest req = new BackupRequest(packageName);
                if (this.mPendingBackups.put(packageName, req) == null) {
                    writeToJournalLocked(packageName);
                }
            }
        }
        KeyValueBackupJob.schedule(this.mContext);
    }

    private HashSet<String> dataChangedTargets(String packageName) {
        HashSet<String> hashSet;
        if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
            synchronized (this.mBackupParticipants) {
                hashSet = this.mBackupParticipants.get(Binder.getCallingUid());
            }
            return hashSet;
        }
        HashSet<String> targets = new HashSet<>();
        if (PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
            targets.add(PACKAGE_MANAGER_SENTINEL);
        } else {
            synchronized (this.mBackupParticipants) {
                int N = this.mBackupParticipants.size();
                for (int i = 0; i < N; i++) {
                    HashSet<String> s = this.mBackupParticipants.valueAt(i);
                    if (s != null) {
                        targets.addAll(s);
                    }
                }
            }
        }
        return targets;
    }

    private void writeToJournalLocked(String str) throws Throwable {
        RandomAccessFile out;
        RandomAccessFile out2 = null;
        try {
            try {
                if (this.mJournal == null) {
                    this.mJournal = File.createTempFile("journal", null, this.mJournalDir);
                }
                out = new RandomAccessFile(this.mJournal, "rws");
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            out.seek(out.length());
            out.writeUTF(str);
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            e = e3;
            out2 = out;
            Slog.e(TAG, "Can't write " + str + " to backup journal", e);
            this.mJournal = null;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    public void dataChanged(final String packageName) {
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            return;
        }
        final HashSet<String> targets = dataChangedTargets(packageName);
        if (targets == null) {
            Slog.w(TAG, "dataChanged but no participant pkg='" + packageName + "' uid=" + Binder.getCallingUid());
        } else {
            this.mBackupHandler.post(new Runnable() {
                @Override
                public void run() {
                    BackupManagerService.this.dataChangedImpl(packageName, targets);
                }
            });
        }
    }

    public void clearBackupData(String transportName, String packageName) {
        HashSet<String> apps;
        Slog.v(TAG, "clearBackupData() of " + packageName + " on " + transportName);
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 64);
            if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                apps = this.mBackupParticipants.get(Binder.getCallingUid());
            } else {
                apps = new HashSet<>();
                int N = this.mBackupParticipants.size();
                for (int i = 0; i < N; i++) {
                    HashSet<String> s = this.mBackupParticipants.valueAt(i);
                    if (s != null) {
                        apps.addAll(s);
                    }
                }
            }
            if (!apps.contains(packageName)) {
                return;
            }
            this.mBackupHandler.removeMessages(12);
            synchronized (this.mQueueLock) {
                IBackupTransport transport = getTransport(transportName);
                if (transport == null) {
                    Message msg = this.mBackupHandler.obtainMessage(12, new ClearRetryParams(transportName, packageName));
                    this.mBackupHandler.sendMessageDelayed(msg, 3600000L);
                    return;
                }
                long oldId = Binder.clearCallingIdentity();
                this.mWakelock.acquire();
                Message msg2 = this.mBackupHandler.obtainMessage(4, new ClearParams(transport, info));
                this.mBackupHandler.sendMessage(msg2);
                Binder.restoreCallingIdentity(oldId);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(TAG, "No such package '" + packageName + "' - not clearing backup data");
        }
    }

    public void backupNow() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "backupNow");
        if (this.mPowerManager.isPowerSaveMode()) {
            Slog.v(TAG, "Not running backup while in battery save mode");
            KeyValueBackupJob.schedule(this.mContext);
            return;
        }
        Slog.v(TAG, "Scheduling immediate backup pass");
        synchronized (this.mQueueLock) {
            try {
                this.mRunBackupIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Slog.e(TAG, "run-backup intent cancelled!");
            }
            KeyValueBackupJob.cancel(this.mContext);
        }
    }

    boolean deviceIsProvisioned() {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Global.getInt(resolver, "device_provisioned", 0) != 0;
    }

    public void fullBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean compress, String[] pkgList) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullBackup");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Backup supported only for the device owner");
        }
        if (!doAllApps && !includeShared && (pkgList == null || pkgList.length == 0)) {
            throw new IllegalArgumentException("Backup requested but neither shared nor any apps named");
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (!deviceIsProvisioned()) {
                Slog.i(TAG, "Full backup not supported before setup");
                return;
            }
            Slog.v(TAG, "Requesting full backup: apks=" + includeApks + " obb=" + includeObbs + " shared=" + includeShared + " all=" + doAllApps + " system=" + includeSystem + " pkgs=" + pkgList);
            Slog.i(TAG, "Beginning full backup...");
            FullBackupParams params = new FullBackupParams(fd, includeApks, includeObbs, includeShared, doWidgets, doAllApps, includeSystem, compress, pkgList);
            int token = generateToken();
            synchronized (this.mFullConfirmations) {
                this.mFullConfirmations.put(token, params);
            }
            Slog.d(TAG, "Starting backup confirmation UI, token=" + token);
            if (!startConfirmationUi(token, "fullback")) {
                Slog.e(TAG, "Unable to launch full backup confirmation");
                this.mFullConfirmations.delete(token);
                try {
                    fd.close();
                } catch (IOException e) {
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.d(TAG, "Full backup processing complete.");
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(token, params);
            Slog.d(TAG, "Waiting for full backup completion...");
            waitForCompletion(params);
            try {
                fd.close();
            } catch (IOException e2) {
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.d(TAG, "Full backup processing complete.");
        } finally {
            try {
                fd.close();
            } catch (IOException e3) {
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.d(TAG, "Full backup processing complete.");
        }
    }

    public void fullTransportBackup(String[] pkgNames) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullTransportBackup");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        if (!fullBackupAllowable(getTransport(this.mCurrentTransport))) {
            Slog.i(TAG, "Full backup not currently possible -- key/value backup not yet run?");
        } else {
            Slog.d(TAG, "fullTransportBackup()");
            long oldId = Binder.clearCallingIdentity();
            try {
                CountDownLatch latch = new CountDownLatch(1);
                PerformFullTransportBackupTask task = new PerformFullTransportBackupTask(null, pkgNames, false, null, latch, null, false);
                this.mWakelock.acquire();
                new Thread(task, "full-transport-master").start();
                while (true) {
                    try {
                        latch.await();
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                long now = System.currentTimeMillis();
                for (String pkg : pkgNames) {
                    enqueueFullBackup(pkg, now);
                }
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }
        Slog.d(TAG, "Done with full transport backup.");
    }

    public void fullRestore(ParcelFileDescriptor fd) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullRestore");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (!deviceIsProvisioned()) {
                Slog.i(TAG, "Full restore not permitted before setup");
                return;
            }
            Slog.i(TAG, "Beginning full restore...");
            FullRestoreParams params = new FullRestoreParams(fd);
            int token = generateToken();
            synchronized (this.mFullConfirmations) {
                this.mFullConfirmations.put(token, params);
            }
            Slog.d(TAG, "Starting restore confirmation UI, token=" + token);
            if (!startConfirmationUi(token, "fullrest")) {
                Slog.e(TAG, "Unable to launch full restore confirmation");
                this.mFullConfirmations.delete(token);
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Error trying to close fd after full restore: " + e);
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.i(TAG, "Full restore processing complete.");
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(token, params);
            Slog.d(TAG, "Waiting for full restore completion...");
            waitForCompletion(params);
            try {
                fd.close();
            } catch (IOException e2) {
                Slog.w(TAG, "Error trying to close fd after full restore: " + e2);
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.i(TAG, "Full restore processing complete.");
        } finally {
            try {
                fd.close();
            } catch (IOException e3) {
                Slog.w(TAG, "Error trying to close fd after full restore: " + e3);
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.i(TAG, "Full restore processing complete.");
        }
    }

    boolean startConfirmationUi(int token, String action) {
        try {
            Intent confIntent = new Intent(action);
            confIntent.setClassName("com.android.backupconfirm", "com.android.backupconfirm.BackupRestoreConfirmation");
            confIntent.putExtra("conftoken", token);
            confIntent.addFlags(268435456);
            this.mContext.startActivityAsUser(confIntent, UserHandle.SYSTEM);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    void startConfirmationTimeout(int token, FullParams params) {
        Message msg = this.mBackupHandler.obtainMessage(9, token, 0, params);
        this.mBackupHandler.sendMessageDelayed(msg, 60000L);
    }

    void waitForCompletion(FullParams params) {
        synchronized (params.latch) {
            while (!params.latch.get()) {
                try {
                    params.latch.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    void signalFullBackupRestoreCompletion(FullParams params) {
        synchronized (params.latch) {
            params.latch.set(true);
            params.latch.notifyAll();
        }
    }

    public void acknowledgeFullBackupOrRestore(int token, boolean allow, String curPassword, String encPpassword, IFullBackupRestoreObserver observer) {
        int verb;
        Slog.d(TAG, "acknowledgeFullBackupOrRestore : token=" + token + " allow=" + allow);
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "acknowledgeFullBackupOrRestore");
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mFullConfirmations) {
                FullParams params = this.mFullConfirmations.get(token);
                if (params != null) {
                    this.mBackupHandler.removeMessages(9, params);
                    this.mFullConfirmations.delete(token);
                    if (allow) {
                        if (params instanceof FullBackupParams) {
                            verb = 2;
                        } else {
                            verb = 10;
                        }
                        params.observer = observer;
                        params.curPassword = curPassword;
                        params.encryptPassword = encPpassword;
                        this.mWakelock.acquire();
                        Message msg = this.mBackupHandler.obtainMessage(verb, params);
                        this.mBackupHandler.sendMessage(msg);
                    } else {
                        Slog.w(TAG, "User rejected full backup/restore operation");
                        signalFullBackupRestoreCompletion(params);
                    }
                } else {
                    Slog.w(TAG, "Attempted to ack full backup/restore with invalid token");
                }
            }
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    private static boolean backupSettingMigrated(int userId) {
        File base = new File(Environment.getDataDirectory(), "backup");
        File enableFile = new File(base, BACKUP_ENABLE_FILE);
        return enableFile.exists();
    }

    private static boolean readBackupEnableState(int userId) throws Throwable {
        Throwable th = null;
        File base = new File(Environment.getDataDirectory(), "backup");
        File enableFile = new File(base, BACKUP_ENABLE_FILE);
        if (enableFile.exists()) {
            FileInputStream fin = null;
            try {
                FileInputStream fin2 = new FileInputStream(enableFile);
                try {
                    int state = fin2.read();
                    boolean z = state != 0;
                    if (fin2 != null) {
                        try {
                            try {
                                fin2.close();
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (IOException e) {
                            Slog.e(TAG, "Cannot read enable state; assuming disabled");
                            return false;
                        }
                    }
                    if (th != null) {
                        throw th;
                    }
                    return z;
                } catch (Throwable th3) {
                    th = th3;
                    fin = fin2;
                    if (fin != null) {
                    }
                    if (th == null) {
                    }
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } else {
            Slog.i(TAG, "isBackupEnabled() => false due to absent settings file");
        }
        return false;
    }

    private static void writeBackupEnableState(boolean enable, int userId) throws Throwable {
        FileOutputStream fout;
        File base = new File(Environment.getDataDirectory(), "backup");
        File enableFile = new File(base, BACKUP_ENABLE_FILE);
        File stage = new File(base, "backup_enabled-stage");
        FileOutputStream fout2 = null;
        try {
            try {
                fout = new FileOutputStream(stage);
            } catch (IOException | RuntimeException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            fout.write(enable ? 1 : 0);
            fout.close();
            stage.renameTo(enableFile);
            IoUtils.closeQuietly(fout);
        } catch (IOException | RuntimeException e2) {
            e = e2;
            fout2 = fout;
            Slog.e(TAG, "Unable to record backup enable state; reverting to disabled: " + e.getMessage());
            ContentResolver r = sInstance.mContext.getContentResolver();
            Settings.Secure.putStringForUser(r, BACKUP_ENABLE_FILE, null, userId);
            enableFile.delete();
            stage.delete();
            IoUtils.closeQuietly(fout2);
        } catch (Throwable th2) {
            th = th2;
            fout2 = fout;
            IoUtils.closeQuietly(fout2);
            throw th;
        }
    }

    public void setBackupEnabled(boolean enable) {
        HashSet<String> allTransports;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupEnabled");
        Slog.i(TAG, "Backup enabled => " + enable);
        long oldId = Binder.clearCallingIdentity();
        try {
            boolean wasEnabled = this.mEnabled;
            synchronized (this) {
                writeBackupEnableState(enable, 0);
                this.mEnabled = enable;
            }
            synchronized (this.mQueueLock) {
                if (enable && !wasEnabled) {
                    if (this.mProvisioned) {
                        KeyValueBackupJob.schedule(this.mContext);
                        scheduleNextFullBackupJob(0L);
                    }
                }
                if (!enable) {
                    KeyValueBackupJob.cancel(this.mContext);
                    if (wasEnabled && this.mProvisioned) {
                        synchronized (this.mTransports) {
                            allTransports = new HashSet<>(this.mTransports.keySet());
                        }
                        for (String transport : allTransports) {
                            recordInitPendingLocked(true, transport);
                        }
                        this.mAlarmManager.set(0, System.currentTimeMillis(), this.mRunInitIntent);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void setAutoRestore(boolean doAutoRestore) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setAutoRestore");
        Slog.i(TAG, "Auto restore => " + doAutoRestore);
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "backup_auto_restore", doAutoRestore ? 1 : 0);
                this.mAutoRestore = doAutoRestore;
            }
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void setBackupProvisioned(boolean available) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupProvisioned");
    }

    public boolean isBackupEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isBackupEnabled");
        return this.mEnabled;
    }

    public String getCurrentTransport() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getCurrentTransport");
        return this.mCurrentTransport;
    }

    public String[] listAllTransports() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransports");
        ArrayList<String> known = new ArrayList<>();
        for (Map.Entry<String, IBackupTransport> entry : this.mTransports.entrySet()) {
            if (entry.getValue() != null) {
                known.add(entry.getKey());
            }
        }
        if (known.size() <= 0) {
            return null;
        }
        String[] list = new String[known.size()];
        known.toArray(list);
        return list;
    }

    public String[] getTransportWhitelist() {
        String[] whitelist = new String[this.mTransportWhitelist.size()];
        for (int i = this.mTransportWhitelist.size() - 1; i >= 0; i--) {
            whitelist[i] = this.mTransportWhitelist.valueAt(i).flattenToShortString();
        }
        return whitelist;
    }

    public String selectBackupTransport(String transport) {
        String prevTransport;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransport");
        synchronized (this.mTransports) {
            long oldId = Binder.clearCallingIdentity();
            try {
                prevTransport = this.mCurrentTransport;
                this.mCurrentTransport = transport;
                Settings.Secure.putString(this.mContext.getContentResolver(), "backup_transport", transport);
                Slog.v(TAG, "selectBackupTransport() set " + this.mCurrentTransport + " returning " + prevTransport);
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }
        return prevTransport;
    }

    public Intent getConfigurationIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getConfigurationIntent");
        synchronized (this.mTransports) {
            IBackupTransport transport = this.mTransports.get(transportName);
            if (transport != null) {
                try {
                    Intent intent = transport.configurationIntent();
                    return intent;
                } catch (RemoteException e) {
                }
            }
            return null;
        }
    }

    public String getDestinationString(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDestinationString");
        synchronized (this.mTransports) {
            IBackupTransport transport = this.mTransports.get(transportName);
            if (transport != null) {
                try {
                    String text = transport.currentDestinationString();
                    return text;
                } catch (RemoteException e) {
                }
            }
            return null;
        }
    }

    public Intent getDataManagementIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementIntent");
        synchronized (this.mTransports) {
            IBackupTransport transport = this.mTransports.get(transportName);
            if (transport != null) {
                try {
                    Intent intent = transport.dataManagementIntent();
                    return intent;
                } catch (RemoteException e) {
                }
            }
            return null;
        }
    }

    public String getDataManagementLabel(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementLabel");
        synchronized (this.mTransports) {
            IBackupTransport transport = this.mTransports.get(transportName);
            if (transport != null) {
                try {
                    String text = transport.dataManagementLabel();
                    return text;
                } catch (RemoteException e) {
                }
            }
            return null;
        }
    }

    public void agentConnected(String packageName, IBinder agentBinder) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                Slog.d(TAG, "agentConnected pkg=" + packageName + " agent=" + agentBinder);
                IBackupAgent agent = IBackupAgent.Stub.asInterface(agentBinder);
                this.mConnectedAgent = agent;
                this.mConnecting = false;
            } else {
                Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent connected");
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void agentDisconnected(String packageName) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                this.mConnectedAgent = null;
                this.mConnecting = false;
            } else {
                Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent disconnected");
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void restoreAtInstall(String packageName, int token) {
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " attemping install-time restore");
            return;
        }
        long restoreSet = getAvailableRestoreToken(packageName);
        Slog.v(TAG, "restoreAtInstall pkg=" + packageName + " token=" + Integer.toHexString(token) + " restoreSet=" + Long.toHexString(restoreSet));
        boolean skip = restoreSet == 0;
        IBackupTransport transport = getTransport(this.mCurrentTransport);
        if (transport == null) {
            Slog.w(TAG, "No transport");
            skip = true;
        }
        if (!this.mAutoRestore) {
            Slog.w(TAG, "Non-restorable state: auto=" + this.mAutoRestore);
            skip = true;
        }
        if (!skip) {
            try {
                String dirName = transport.transportDirName();
                this.mWakelock.acquire();
                Message msg = this.mBackupHandler.obtainMessage(3);
                msg.obj = new RestoreParams(transport, dirName, (IRestoreObserver) null, restoreSet, packageName, token);
                this.mBackupHandler.sendMessage(msg);
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to contact transport");
                skip = true;
            }
        }
        if (skip) {
            Slog.v(TAG, "Finishing install immediately");
            try {
                this.mPackageManagerBinder.finishPackageInstall(token, false);
            } catch (RemoteException e2) {
            }
        }
    }

    public IRestoreSession beginRestoreSession(String packageName, String transport) {
        Slog.v(TAG, "beginRestoreSession: pkg=" + packageName + " transport=" + transport);
        boolean needPermission = true;
        if (transport == null) {
            transport = this.mCurrentTransport;
            if (packageName != null) {
                try {
                    PackageInfo app = this.mPackageManager.getPackageInfo(packageName, 0);
                    if (app.applicationInfo.uid == Binder.getCallingUid()) {
                        needPermission = false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(TAG, "Asked to restore nonexistent pkg " + packageName);
                    throw new IllegalArgumentException("Package " + packageName + " not found");
                }
            }
        }
        if (needPermission) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "beginRestoreSession");
        } else {
            Slog.d(TAG, "restoring self on current transport; no permission needed");
        }
        synchronized (this) {
            if (this.mActiveRestoreSession != null) {
                Slog.i(TAG, "Restore session requested but one already active");
                return null;
            }
            if (this.mBackupRunning) {
                Slog.i(TAG, "Restore session requested but currently running backups");
                return null;
            }
            this.mActiveRestoreSession = new ActiveRestoreSession(packageName, transport);
            this.mBackupHandler.sendEmptyMessageDelayed(8, 60000L);
            return this.mActiveRestoreSession;
        }
    }

    void clearRestoreSession(ActiveRestoreSession currentSession) {
        synchronized (this) {
            if (currentSession != this.mActiveRestoreSession) {
                Slog.e(TAG, "ending non-current restore session");
            } else {
                Slog.v(TAG, "Clearing restore session and halting timeout");
                this.mActiveRestoreSession = null;
                this.mBackupHandler.removeMessages(8);
            }
        }
    }

    public void opComplete(int token, long result) {
        Operation op;
        synchronized (this.mCurrentOpLock) {
            op = this.mCurrentOperations.get(token);
            if (op != null) {
                if (op.state == -1) {
                    op = null;
                    this.mCurrentOperations.delete(token);
                } else {
                    op.state = 1;
                }
            }
            this.mCurrentOpLock.notifyAll();
        }
        if (op == null || op.callback == null) {
            return;
        }
        Pair<BackupRestoreTask, Long> callbackAndResult = Pair.create(op.callback, Long.valueOf(result));
        Message msg = this.mBackupHandler.obtainMessage(21, callbackAndResult);
        this.mBackupHandler.sendMessage(msg);
    }

    public boolean isAppEligibleForBackup(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isAppEligibleForBackup");
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 64);
            if (!appIsEligibleForBackup(packageInfo.applicationInfo) || appIsStopped(packageInfo.applicationInfo)) {
                return false;
            }
            IBackupTransport transport = getTransport(this.mCurrentTransport);
            if (transport != null) {
                try {
                    return transport.isAppEligibleForBackup(packageInfo, appGetsFullBackup(packageInfo));
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to contact transport");
                    return true;
                }
            }
            return true;
        } catch (PackageManager.NameNotFoundException e2) {
            return false;
        }
    }

    class ActiveRestoreSession extends IRestoreSession.Stub {
        private static final String TAG = "RestoreSession";
        private String mPackageName;
        private IBackupTransport mRestoreTransport;
        RestoreSet[] mRestoreSets = null;
        boolean mEnded = false;
        boolean mTimedOut = false;

        ActiveRestoreSession(String packageName, String transport) {
            this.mRestoreTransport = null;
            this.mPackageName = packageName;
            this.mRestoreTransport = BackupManagerService.this.getTransport(transport);
        }

        public void markTimedOut() {
            this.mTimedOut = true;
        }

        public synchronized int getAvailableRestoreSets(IRestoreObserver observer) {
            long oldId;
            BackupManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreSets");
            if (observer == null) {
                throw new IllegalArgumentException("Observer must not be null");
            }
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            }
            try {
                oldId = Binder.clearCallingIdentity();
                try {
                    if (this.mRestoreTransport == null) {
                        Slog.w(TAG, "Null transport getting restore sets");
                        return -1;
                    }
                    BackupManagerService.this.mBackupHandler.removeMessages(8);
                    BackupManagerService.this.mWakelock.acquire();
                    Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(6, BackupManagerService.this.new RestoreGetSetsParams(this.mRestoreTransport, this, observer));
                    BackupManagerService.this.mBackupHandler.sendMessage(msg);
                    return 0;
                } catch (Exception e) {
                    Slog.e(TAG, "Error in getAvailableRestoreSets", e);
                    return -1;
                }
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }

        public synchronized int restoreAll(long token, IRestoreObserver observer) {
            BackupManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
            Slog.d(TAG, "restoreAll token=" + Long.toHexString(token) + " observer=" + observer);
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            }
            if (this.mRestoreTransport == null || this.mRestoreSets == null) {
                Slog.e(TAG, "Ignoring restoreAll() with no restore set");
                return -1;
            }
            if (this.mPackageName != null) {
                Slog.e(TAG, "Ignoring restoreAll() on single-package session");
                return -1;
            }
            try {
                String dirName = this.mRestoreTransport.transportDirName();
                synchronized (BackupManagerService.this.mQueueLock) {
                    for (int i = 0; i < this.mRestoreSets.length; i++) {
                        if (token == this.mRestoreSets[i].token) {
                            BackupManagerService.this.mBackupHandler.removeMessages(8);
                            long oldId = Binder.clearCallingIdentity();
                            BackupManagerService.this.mWakelock.acquire();
                            Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(3);
                            msg.obj = BackupManagerService.this.new RestoreParams(this.mRestoreTransport, dirName, observer, token);
                            BackupManagerService.this.mBackupHandler.sendMessage(msg);
                            Binder.restoreCallingIdentity(oldId);
                            return 0;
                        }
                    }
                    Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                    return -1;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to contact transport for restore");
                return -1;
            }
        }

        public synchronized int restoreSome(long token, IRestoreObserver observer, String[] packages) {
            BackupManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
            StringBuilder b = new StringBuilder(128);
            b.append("restoreSome token=");
            b.append(Long.toHexString(token));
            b.append(" observer=");
            b.append(observer.toString());
            b.append(" packages=");
            if (packages == null) {
                b.append("null");
            } else {
                b.append('{');
                boolean first = true;
                for (String s : packages) {
                    if (!first) {
                        b.append(", ");
                    } else {
                        first = false;
                    }
                    b.append(s);
                }
                b.append('}');
            }
            Slog.d(TAG, b.toString());
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            }
            if (this.mRestoreTransport == null || this.mRestoreSets == null) {
                Slog.e(TAG, "Ignoring restoreAll() with no restore set");
                return -1;
            }
            if (this.mPackageName != null) {
                Slog.e(TAG, "Ignoring restoreAll() on single-package session");
                return -1;
            }
            try {
                String dirName = this.mRestoreTransport.transportDirName();
                synchronized (BackupManagerService.this.mQueueLock) {
                    for (int i = 0; i < this.mRestoreSets.length; i++) {
                        if (token == this.mRestoreSets[i].token) {
                            BackupManagerService.this.mBackupHandler.removeMessages(8);
                            long oldId = Binder.clearCallingIdentity();
                            BackupManagerService.this.mWakelock.acquire();
                            Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(3);
                            msg.obj = BackupManagerService.this.new RestoreParams(this.mRestoreTransport, dirName, observer, token, packages, packages.length > 1);
                            BackupManagerService.this.mBackupHandler.sendMessage(msg);
                            Binder.restoreCallingIdentity(oldId);
                            return 0;
                        }
                    }
                    Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                    return -1;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to contact transport for restore");
                return -1;
            }
        }

        public synchronized int restorePackage(String packageName, IRestoreObserver observer) {
            Slog.v(TAG, "restorePackage pkg=" + packageName + " obs=" + observer);
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            }
            if (this.mPackageName != null && !this.mPackageName.equals(packageName)) {
                Slog.e(TAG, "Ignoring attempt to restore pkg=" + packageName + " on session for package " + this.mPackageName);
                return -1;
            }
            try {
                PackageInfo app = BackupManagerService.this.mPackageManager.getPackageInfo(packageName, 0);
                int perm = BackupManagerService.this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid());
                if (perm == -1 && app.applicationInfo.uid != Binder.getCallingUid()) {
                    Slog.w(TAG, "restorePackage: bad packageName=" + packageName + " or calling uid=" + Binder.getCallingUid());
                    throw new SecurityException("No permission to restore other packages");
                }
                long oldId = Binder.clearCallingIdentity();
                try {
                    long token = BackupManagerService.this.getAvailableRestoreToken(packageName);
                    Slog.v(TAG, "restorePackage pkg=" + packageName + " token=" + Long.toHexString(token));
                    if (token == 0) {
                        Slog.w(TAG, "No data available for this package; not restoring");
                        return -1;
                    }
                    String dirName = this.mRestoreTransport.transportDirName();
                    BackupManagerService.this.mBackupHandler.removeMessages(8);
                    BackupManagerService.this.mWakelock.acquire();
                    Message msg = BackupManagerService.this.mBackupHandler.obtainMessage(3);
                    msg.obj = BackupManagerService.this.new RestoreParams(this.mRestoreTransport, dirName, observer, token, app);
                    BackupManagerService.this.mBackupHandler.sendMessage(msg);
                    return 0;
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to contact transport for restore");
                    return -1;
                } finally {
                    Binder.restoreCallingIdentity(oldId);
                }
            } catch (PackageManager.NameNotFoundException e2) {
                Slog.w(TAG, "Asked to restore nonexistent pkg " + packageName);
                return -1;
            }
        }

        class EndRestoreRunnable implements Runnable {
            BackupManagerService mBackupManager;
            ActiveRestoreSession mSession;

            EndRestoreRunnable(BackupManagerService manager, ActiveRestoreSession session) {
                this.mBackupManager = manager;
                this.mSession = session;
            }

            @Override
            public void run() {
                synchronized (this.mSession) {
                    this.mSession.mRestoreTransport = null;
                    this.mSession.mEnded = true;
                }
                this.mBackupManager.clearRestoreSession(this.mSession);
            }
        }

        public synchronized void endRestoreSession() {
            Slog.d(TAG, "endRestoreSession");
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
            } else {
                if (this.mEnded) {
                    throw new IllegalStateException("Restore session already ended");
                }
                BackupManagerService.this.mBackupHandler.post(new EndRestoreRunnable(BackupManagerService.this, this));
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        long identityToken = Binder.clearCallingIdentity();
        if (args != null) {
            try {
                for (String arg : args) {
                    if ("-h".equals(arg)) {
                        pw.println("'dumpsys backup' optional arguments:");
                        pw.println("  -h       : this help text");
                        pw.println("  a[gents] : dump information about defined backup agents");
                        return;
                    } else {
                        if ("agents".startsWith(arg)) {
                            dumpAgents(pw);
                            return;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }
        dumpInternal(pw);
    }

    private void dumpAgents(PrintWriter pw) {
        List<PackageInfo> agentPackages = allAgentPackages();
        pw.println("Defined backup agents:");
        for (PackageInfo pkg : agentPackages) {
            pw.print("  ");
            pw.print(pkg.packageName);
            pw.println(':');
            pw.print("      ");
            pw.println(pkg.applicationInfo.backupAgentName);
        }
    }

    private void dumpInternal(PrintWriter pw) {
        synchronized (this.mQueueLock) {
            pw.println("Backup Manager is " + (this.mEnabled ? "enabled" : "disabled") + " / " + (!this.mProvisioned ? "not " : "") + "provisioned / " + (this.mPendingInits.size() == 0 ? "not " : "") + "pending init");
            pw.println("Auto-restore is " + (this.mAutoRestore ? "enabled" : "disabled"));
            if (this.mBackupRunning) {
                pw.println("Backup currently running");
            }
            pw.println("Last backup pass started: " + this.mLastBackupPass + " (now = " + System.currentTimeMillis() + ')');
            pw.println("  next scheduled: " + KeyValueBackupJob.nextScheduled());
            pw.println("Transport whitelist:");
            for (ComponentName transport : this.mTransportWhitelist) {
                pw.print("    ");
                pw.println(transport.flattenToShortString());
            }
            pw.println("Available transports:");
            String[] transports = listAllTransports();
            if (transports != null) {
                for (String t : listAllTransports()) {
                    pw.println((t.equals(this.mCurrentTransport) ? "  * " : "    ") + t);
                    try {
                        IBackupTransport transport2 = getTransport(t);
                        File dir = new File(this.mBaseStateDir, transport2.transportDirName());
                        pw.println("       destination: " + transport2.currentDestinationString());
                        pw.println("       intent: " + transport2.configurationIntent());
                        for (File f : dir.listFiles()) {
                            pw.println("       " + f.getName() + " - " + f.length() + " state bytes");
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Error in transport", e);
                        pw.println("        Error: " + e);
                    }
                }
                pw.println("Pending init: " + this.mPendingInits.size());
                for (String s : this.mPendingInits) {
                    pw.println("    " + s);
                }
                synchronized (this.mBackupTrace) {
                    if (!this.mBackupTrace.isEmpty()) {
                        pw.println("Most recent backup trace:");
                        for (String s2 : this.mBackupTrace) {
                            pw.println("   " + s2);
                        }
                    }
                }
                pw.print("Ancestral: ");
                pw.println(Long.toHexString(this.mAncestralToken));
                pw.print("Current:   ");
                pw.println(Long.toHexString(this.mCurrentToken));
                int N = this.mBackupParticipants.size();
                pw.println("Participants:");
                for (int i = 0; i < N; i++) {
                    int uid = this.mBackupParticipants.keyAt(i);
                    pw.print("  uid: ");
                    pw.println(uid);
                    HashSet<String> participants = this.mBackupParticipants.valueAt(i);
                    for (String app : participants) {
                        pw.println("    " + app);
                    }
                }
                pw.println("Ancestral packages: " + (this.mAncestralPackages == null ? "none" : Integer.valueOf(this.mAncestralPackages.size())));
                if (this.mAncestralPackages != null) {
                    for (String pkg : this.mAncestralPackages) {
                        pw.println("    " + pkg);
                    }
                }
                pw.println("Ever backed up: " + this.mEverStoredApps.size());
                for (String pkg2 : this.mEverStoredApps) {
                    pw.println("    " + pkg2);
                }
                pw.println("Pending key/value backup: " + this.mPendingBackups.size());
                for (BackupRequest req : this.mPendingBackups.values()) {
                    pw.println("    " + req);
                }
                pw.println("Full backup queue:" + this.mFullBackupQueue.size());
                for (FullBackupEntry entry : this.mFullBackupQueue) {
                    pw.print("    ");
                    pw.print(entry.lastBackup);
                    pw.print(" : ");
                    pw.println(entry.packageName);
                }
            } else {
                pw.println("Pending init: " + this.mPendingInits.size());
                while (s$iterator.hasNext()) {
                }
                synchronized (this.mBackupTrace) {
                }
            }
        }
    }

    private static void sendBackupOnUpdate(IBackupObserver observer, String packageName, BackupProgress progress) {
        if (observer == null) {
            return;
        }
        try {
            observer.onUpdate(packageName, progress);
        } catch (RemoteException e) {
            Slog.w(TAG, "Backup observer went away: onUpdate");
        }
    }

    private static void sendBackupOnPackageResult(IBackupObserver observer, String packageName, int status) {
        if (observer == null) {
            return;
        }
        try {
            observer.onResult(packageName, status);
        } catch (RemoteException e) {
            Slog.w(TAG, "Backup observer went away: onResult");
        }
    }

    private static void sendBackupFinished(IBackupObserver observer, int status) {
        if (observer == null) {
            return;
        }
        try {
            observer.backupFinished(status);
        } catch (RemoteException e) {
            Slog.w(TAG, "Backup observer went away: backupFinished");
        }
    }
}
