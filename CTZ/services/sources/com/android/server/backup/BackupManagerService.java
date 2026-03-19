package com.android.server.backup;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IBackupAgent;
import android.app.PendingIntent;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.BatteryService;
import com.android.server.EventLogTags;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.fullbackup.FullBackupEntry;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.backup.internal.BackupRequest;
import com.android.server.backup.internal.ClearDataObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.internal.PerformInitializeTask;
import com.android.server.backup.internal.ProvisionedObserver;
import com.android.server.backup.internal.RunBackupReceiver;
import com.android.server.backup.internal.RunInitializeReceiver;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import com.android.server.backup.transport.OnTransportRegisteredListener;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.backup.utils.SparseArrayUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.usage.AppStandbyController;
import com.google.android.collect.Sets;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BackupManagerService implements BackupManagerServiceInterface {
    private static final String BACKUP_ENABLE_FILE = "backup_enabled";
    public static final String BACKUP_FILE_HEADER_MAGIC = "ANDROID BACKUP\n";
    public static final int BACKUP_FILE_VERSION = 5;
    public static final String BACKUP_FINISHED_ACTION = "android.intent.action.BACKUP_FINISHED";
    public static final String BACKUP_FINISHED_PACKAGE_EXTRA = "packageName";
    public static final String BACKUP_MANIFEST_FILENAME = "_manifest";
    public static final int BACKUP_MANIFEST_VERSION = 1;
    public static final String BACKUP_METADATA_FILENAME = "_meta";
    public static final int BACKUP_METADATA_VERSION = 1;
    public static final int BACKUP_WIDGET_METADATA_TOKEN = 33549569;
    private static final int BUSY_BACKOFF_FUZZ = 7200000;
    private static final long BUSY_BACKOFF_MIN_MILLIS = 3600000;
    private static final boolean COMPRESS_FULL_BACKUPS = true;
    private static final int CURRENT_ANCESTRAL_RECORD_VERSION = 1;
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_BACKUP_TRACE = true;
    public static final boolean DEBUG_SCHEDULING = true;
    private static final long INITIALIZATION_DELAY_MILLIS = 3000;
    private static final String INIT_SENTINEL_FILE_NAME = "_need_init_";
    public static final String KEY_WIDGET_STATE = "￭￭widget";
    public static final boolean MORE_DEBUG = false;
    private static final int OP_ACKNOWLEDGED = 1;
    public static final int OP_PENDING = 0;
    private static final int OP_TIMEOUT = -1;
    public static final int OP_TYPE_BACKUP = 2;
    public static final int OP_TYPE_BACKUP_WAIT = 0;
    public static final int OP_TYPE_RESTORE_WAIT = 1;
    public static final String PACKAGE_MANAGER_SENTINEL = "@pm@";
    public static final String RUN_BACKUP_ACTION = "android.app.backup.intent.RUN";
    public static final String RUN_INITIALIZE_ACTION = "android.app.backup.intent.INIT";
    private static final int SCHEDULE_FILE_VERSION = 1;
    private static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    public static final String SETTINGS_PACKAGE = "com.android.providers.settings";
    public static final String SHARED_BACKUP_AGENT_PACKAGE = "com.android.sharedstoragebackup";
    public static final String TAG = "BackupManagerService";
    private static final long TIMEOUT_FULL_CONFIRMATION = 60000;
    private static final long TIMEOUT_INTERVAL = 10000;
    private static final long TRANSPORT_RETRY_INTERVAL = 3600000;
    static Trampoline sInstance;
    private ActiveRestoreSession mActiveRestoreSession;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private AlarmManager mAlarmManager;
    private boolean mAutoRestore;
    private BackupHandler mBackupHandler;
    private IBackupManager mBackupManagerBinder;
    private final BackupPasswordManager mBackupPasswordManager;
    private BackupPolicyEnforcer mBackupPolicyEnforcer;
    private volatile boolean mBackupRunning;
    private File mBaseStateDir;
    private volatile boolean mClearingData;
    private IBackupAgent mConnectedAgent;
    private volatile boolean mConnecting;
    private BackupManagerConstants mConstants;
    private Context mContext;
    private File mDataDir;
    private boolean mEnabled;

    @GuardedBy("mQueueLock")
    private ArrayList<FullBackupEntry> mFullBackupQueue;
    private File mFullBackupScheduleFile;

    @GuardedBy("mPendingRestores")
    private boolean mIsRestoreInProgress;
    private DataChangedJournal mJournal;
    private File mJournalDir;
    private volatile long mLastBackupPass;
    private PackageManager mPackageManager;
    private PowerManager mPowerManager;
    private ProcessedPackagesJournal mProcessedPackagesJournal;
    private boolean mProvisioned;
    private ContentObserver mProvisionedObserver;
    private final long mRegisterTransportsRequestedTime;
    private PendingIntent mRunBackupIntent;
    private BroadcastReceiver mRunBackupReceiver;
    private PendingIntent mRunInitIntent;
    private BroadcastReceiver mRunInitReceiver;

    @GuardedBy("mQueueLock")
    private PerformFullTransportBackupTask mRunningFullBackupTask;
    private File mTokenFile;
    private final TransportManager mTransportManager;
    private PowerManager.WakeLock mWakelock;
    private final SparseArray<HashSet<String>> mBackupParticipants = new SparseArray<>();
    private HashMap<String, BackupRequest> mPendingBackups = new HashMap<>();
    private final Object mQueueLock = new Object();
    private final Object mAgentConnectLock = new Object();
    private final List<String> mBackupTrace = new ArrayList();
    private final Object mClearDataLock = new Object();

    @GuardedBy("mPendingRestores")
    private final Queue<PerformUnifiedRestoreTask> mPendingRestores = new ArrayDeque();

    @GuardedBy("mCurrentOpLock")
    private final SparseArray<Operation> mCurrentOperations = new SparseArray<>();
    private final Object mCurrentOpLock = new Object();
    private final Random mTokenGenerator = new Random();
    final AtomicInteger mNextToken = new AtomicInteger();
    private final SparseArray<AdbParams> mAdbBackupRestoreConfirmations = new SparseArray<>();
    private final SecureRandom mRng = new SecureRandom();
    private Set<String> mAncestralPackages = null;
    private long mAncestralToken = 0;
    private long mCurrentToken = 0;
    private final ArraySet<String> mPendingInits = new ArraySet<>();
    private Runnable mFullBackupScheduleWriter = new Runnable() {
        @Override
        public void run() {
            synchronized (BackupManagerService.this.mQueueLock) {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4096);
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                    dataOutputStream.writeInt(1);
                    int size = BackupManagerService.this.mFullBackupQueue.size();
                    dataOutputStream.writeInt(size);
                    for (int i = 0; i < size; i++) {
                        FullBackupEntry fullBackupEntry = (FullBackupEntry) BackupManagerService.this.mFullBackupQueue.get(i);
                        dataOutputStream.writeUTF(fullBackupEntry.packageName);
                        dataOutputStream.writeLong(fullBackupEntry.lastBackup);
                    }
                    dataOutputStream.flush();
                    AtomicFile atomicFile = new AtomicFile(BackupManagerService.this.mFullBackupScheduleFile);
                    FileOutputStream fileOutputStreamStartWrite = atomicFile.startWrite();
                    fileOutputStreamStartWrite.write(byteArrayOutputStream.toByteArray());
                    atomicFile.finishWrite(fileOutputStreamStartWrite);
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "Unable to write backup schedule!", e);
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new AnonymousClass2();
    private IPackageManager mPackageManagerBinder = AppGlobals.getPackageManager();
    private IActivityManager mActivityManager = ActivityManager.getService();
    private IStorageManager mStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));

    static Trampoline getInstance() {
        return sInstance;
    }

    public BackupManagerConstants getConstants() {
        return this.mConstants;
    }

    @Override
    public BackupAgentTimeoutParameters getAgentTimeoutParameters() {
        return this.mAgentTimeoutParameters;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public PackageManager getPackageManager() {
        return this.mPackageManager;
    }

    public void setPackageManager(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    public IPackageManager getPackageManagerBinder() {
        return this.mPackageManagerBinder;
    }

    public void setPackageManagerBinder(IPackageManager iPackageManager) {
        this.mPackageManagerBinder = iPackageManager;
    }

    public IActivityManager getActivityManager() {
        return this.mActivityManager;
    }

    public void setActivityManager(IActivityManager iActivityManager) {
        this.mActivityManager = iActivityManager;
    }

    public AlarmManager getAlarmManager() {
        return this.mAlarmManager;
    }

    public void setAlarmManager(AlarmManager alarmManager) {
        this.mAlarmManager = alarmManager;
    }

    public void setBackupManagerBinder(IBackupManager iBackupManager) {
        this.mBackupManagerBinder = iBackupManager;
    }

    public TransportManager getTransportManager() {
        return this.mTransportManager;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    public boolean isProvisioned() {
        return this.mProvisioned;
    }

    public void setProvisioned(boolean z) {
        this.mProvisioned = z;
    }

    public PowerManager.WakeLock getWakelock() {
        return this.mWakelock;
    }

    public void setWakelock(PowerManager.WakeLock wakeLock) {
        this.mWakelock = wakeLock;
    }

    public Handler getBackupHandler() {
        return this.mBackupHandler;
    }

    public void setBackupHandler(BackupHandler backupHandler) {
        this.mBackupHandler = backupHandler;
    }

    public PendingIntent getRunInitIntent() {
        return this.mRunInitIntent;
    }

    public void setRunInitIntent(PendingIntent pendingIntent) {
        this.mRunInitIntent = pendingIntent;
    }

    public HashMap<String, BackupRequest> getPendingBackups() {
        return this.mPendingBackups;
    }

    public void setPendingBackups(HashMap<String, BackupRequest> map) {
        this.mPendingBackups = map;
    }

    public Object getQueueLock() {
        return this.mQueueLock;
    }

    public boolean isBackupRunning() {
        return this.mBackupRunning;
    }

    public void setBackupRunning(boolean z) {
        this.mBackupRunning = z;
    }

    public long getLastBackupPass() {
        return this.mLastBackupPass;
    }

    public void setLastBackupPass(long j) {
        this.mLastBackupPass = j;
    }

    public Object getClearDataLock() {
        return this.mClearDataLock;
    }

    public boolean isClearingData() {
        return this.mClearingData;
    }

    public void setClearingData(boolean z) {
        this.mClearingData = z;
    }

    public boolean isRestoreInProgress() {
        return this.mIsRestoreInProgress;
    }

    public void setRestoreInProgress(boolean z) {
        this.mIsRestoreInProgress = z;
    }

    public Queue<PerformUnifiedRestoreTask> getPendingRestores() {
        return this.mPendingRestores;
    }

    public ActiveRestoreSession getActiveRestoreSession() {
        return this.mActiveRestoreSession;
    }

    public void setActiveRestoreSession(ActiveRestoreSession activeRestoreSession) {
        this.mActiveRestoreSession = activeRestoreSession;
    }

    public SparseArray<Operation> getCurrentOperations() {
        return this.mCurrentOperations;
    }

    public Object getCurrentOpLock() {
        return this.mCurrentOpLock;
    }

    public SparseArray<AdbParams> getAdbBackupRestoreConfirmations() {
        return this.mAdbBackupRestoreConfirmations;
    }

    public File getBaseStateDir() {
        return this.mBaseStateDir;
    }

    public void setBaseStateDir(File file) {
        this.mBaseStateDir = file;
    }

    public File getDataDir() {
        return this.mDataDir;
    }

    public void setDataDir(File file) {
        this.mDataDir = file;
    }

    public DataChangedJournal getJournal() {
        return this.mJournal;
    }

    public void setJournal(DataChangedJournal dataChangedJournal) {
        this.mJournal = dataChangedJournal;
    }

    public SecureRandom getRng() {
        return this.mRng;
    }

    public Set<String> getAncestralPackages() {
        return this.mAncestralPackages;
    }

    public void setAncestralPackages(Set<String> set) {
        this.mAncestralPackages = set;
    }

    public long getAncestralToken() {
        return this.mAncestralToken;
    }

    public void setAncestralToken(long j) {
        this.mAncestralToken = j;
    }

    public long getCurrentToken() {
        return this.mCurrentToken;
    }

    public void setCurrentToken(long j) {
        this.mCurrentToken = j;
    }

    public ArraySet<String> getPendingInits() {
        return this.mPendingInits;
    }

    public void clearPendingInits() {
        this.mPendingInits.clear();
    }

    public PerformFullTransportBackupTask getRunningFullBackupTask() {
        return this.mRunningFullBackupTask;
    }

    public void setRunningFullBackupTask(PerformFullTransportBackupTask performFullTransportBackupTask) {
        this.mRunningFullBackupTask = performFullTransportBackupTask;
    }

    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
            BackupManagerService.sInstance = new Trampoline(context);
        }

        @Override
        public void onStart() {
            publishBinderService(BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD, BackupManagerService.sInstance);
        }

        @Override
        public void onUnlockUser(int i) {
            if (i == 0) {
                BackupManagerService.sInstance.unlockSystemUser();
            }
        }
    }

    @Override
    public void unlockSystemUser() {
        Trace.traceBegin(64L, "backup migrate");
        if (!backupSettingMigrated(0)) {
            Slog.i(TAG, "Backup enable apparently not migrated");
            ContentResolver contentResolver = sInstance.mContext.getContentResolver();
            int intForUser = Settings.Secure.getIntForUser(contentResolver, BACKUP_ENABLE_FILE, -1, 0);
            if (intForUser >= 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Migrating enable state ");
                sb.append(intForUser != 0);
                Slog.i(TAG, sb.toString());
                writeBackupEnableState(intForUser != 0, 0);
                Settings.Secure.putStringForUser(contentResolver, BACKUP_ENABLE_FILE, null, 0);
            } else {
                Slog.i(TAG, "Backup not yet configured; retaining null enable state");
            }
        }
        Trace.traceEnd(64L);
        Trace.traceBegin(64L, "backup enable");
        try {
            sInstance.setBackupEnabled(readBackupEnableState(0));
        } catch (RemoteException e) {
        }
        Trace.traceEnd(64L);
    }

    @Override
    public int generateRandomIntegerToken() {
        int iNextInt = this.mTokenGenerator.nextInt();
        if (iNextInt < 0) {
            iNextInt = -iNextInt;
        }
        return (iNextInt & (-256)) | (this.mNextToken.incrementAndGet() & 255);
    }

    public PackageManagerBackupAgent makeMetadataAgent() {
        PackageManagerBackupAgent packageManagerBackupAgent = new PackageManagerBackupAgent(this.mPackageManager);
        packageManagerBackupAgent.attach(this.mContext);
        packageManagerBackupAgent.onCreate();
        return packageManagerBackupAgent;
    }

    public PackageManagerBackupAgent makeMetadataAgent(List<PackageInfo> list) {
        PackageManagerBackupAgent packageManagerBackupAgent = new PackageManagerBackupAgent(this.mPackageManager, list);
        packageManagerBackupAgent.attach(this.mContext);
        packageManagerBackupAgent.onCreate();
        return packageManagerBackupAgent;
    }

    public void addBackupTrace(String str) {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.add(str);
        }
    }

    public void clearBackupTrace() {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.clear();
        }
    }

    public static BackupManagerService create(Context context, Trampoline trampoline, HandlerThread handlerThread) {
        Set backupTransportWhitelist = SystemConfig.getInstance().getBackupTransportWhitelist();
        if (backupTransportWhitelist == null) {
            backupTransportWhitelist = Collections.emptySet();
        }
        String string = Settings.Secure.getString(context.getContentResolver(), "backup_transport");
        if (TextUtils.isEmpty(string)) {
            string = null;
        }
        Slog.v(TAG, "Starting with transport " + string);
        return new BackupManagerService(context, trampoline, handlerThread, new File(Environment.getDataDirectory(), BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD), new File(Environment.getDownloadCacheDirectory(), "backup_stage"), new TransportManager(context, backupTransportWhitelist, string));
    }

    @VisibleForTesting
    BackupManagerService(Context context, Trampoline trampoline, HandlerThread handlerThread, File file, File file2, TransportManager transportManager) throws Exception {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mBackupManagerBinder = Trampoline.asInterface(trampoline.asBinder());
        this.mAgentTimeoutParameters = new BackupAgentTimeoutParameters(Handler.getMain(), this.mContext.getContentResolver());
        this.mAgentTimeoutParameters.start();
        this.mBackupHandler = new BackupHandler(this, handlerThread.getLooper());
        ContentResolver contentResolver = context.getContentResolver();
        this.mProvisioned = Settings.Global.getInt(contentResolver, "device_provisioned", 0) != 0;
        this.mAutoRestore = Settings.Secure.getInt(contentResolver, "backup_auto_restore", 1) != 0;
        this.mProvisionedObserver = new ProvisionedObserver(this, this.mBackupHandler);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mProvisionedObserver);
        this.mBaseStateDir = file;
        this.mBaseStateDir.mkdirs();
        if (!SELinux.restorecon(this.mBaseStateDir)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBaseStateDir);
        }
        this.mDataDir = file2;
        this.mBackupPasswordManager = new BackupPasswordManager(this.mContext, this.mBaseStateDir, this.mRng);
        this.mRunBackupReceiver = new RunBackupReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RUN_BACKUP_ACTION);
        context.registerReceiver(this.mRunBackupReceiver, intentFilter, "android.permission.BACKUP", null);
        this.mRunInitReceiver = new RunInitializeReceiver(this);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(RUN_INITIALIZE_ACTION);
        context.registerReceiver(this.mRunInitReceiver, intentFilter2, "android.permission.BACKUP", null);
        Intent intent = new Intent(RUN_BACKUP_ACTION);
        intent.addFlags(1073741824);
        this.mRunBackupIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Intent intent2 = new Intent(RUN_INITIALIZE_ACTION);
        intent2.addFlags(1073741824);
        this.mRunInitIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
        this.mJournalDir = new File(this.mBaseStateDir, "pending");
        this.mJournalDir.mkdirs();
        this.mJournal = null;
        this.mConstants = new BackupManagerConstants(this.mBackupHandler, this.mContext.getContentResolver());
        this.mConstants.start();
        this.mFullBackupScheduleFile = new File(this.mBaseStateDir, "fb-schedule");
        initPackageTracking();
        synchronized (this.mBackupParticipants) {
            addPackageParticipantsLocked(null);
        }
        this.mTransportManager = transportManager;
        this.mTransportManager.setOnTransportRegisteredListener(new OnTransportRegisteredListener() {
            @Override
            public final void onTransportRegistered(String str, String str2) {
                this.f$0.onTransportRegistered(str, str2);
            }
        });
        this.mRegisterTransportsRequestedTime = SystemClock.elapsedRealtime();
        BackupHandler backupHandler = this.mBackupHandler;
        final TransportManager transportManager2 = this.mTransportManager;
        Objects.requireNonNull(transportManager2);
        backupHandler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                transportManager2.registerTransports();
            }
        }, INITIALIZATION_DELAY_MILLIS);
        this.mBackupHandler.postDelayed(new Runnable() {
            @Override
            public final void run() throws Exception {
                this.f$0.parseLeftoverJournals();
            }
        }, INITIALIZATION_DELAY_MILLIS);
        this.mWakelock = this.mPowerManager.newWakeLock(1, "*backup*");
        this.mBackupPolicyEnforcer = new BackupPolicyEnforcer(context);
    }

    private void initPackageTracking() throws Exception {
        this.mTokenFile = new File(this.mBaseStateDir, "ancestral");
        try {
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.mTokenFile)));
            try {
                if (dataInputStream.readInt() == 1) {
                    this.mAncestralToken = dataInputStream.readLong();
                    this.mCurrentToken = dataInputStream.readLong();
                    int i = dataInputStream.readInt();
                    if (i >= 0) {
                        this.mAncestralPackages = new HashSet();
                        for (int i2 = 0; i2 < i; i2++) {
                            this.mAncestralPackages.add(dataInputStream.readUTF());
                        }
                    }
                }
            } finally {
                $closeResource(null, dataInputStream);
            }
        } catch (FileNotFoundException e) {
            Slog.v(TAG, "No ancestral data");
        } catch (IOException e2) {
            Slog.w(TAG, "Unable to read token file", e2);
        }
        this.mProcessedPackagesJournal = new ProcessedPackagesJournal(this.mBaseStateDir);
        this.mProcessedPackagesJournal.init();
        synchronized (this.mQueueLock) {
            this.mFullBackupQueue = readFullBackupSchedule();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2);
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private ArrayList<FullBackupEntry> readFullBackupSchedule() throws Throwable {
        int i;
        ?? r6;
        int i2;
        ?? r62;
        List<PackageInfo> storableApplications = PackageManagerBackupAgent.getStorableApplications(this.mPackageManager);
        int i3 = 0;
        ?? r63 = 1;
        ?? r7 = 0;
        Object obj = null;
        r7 = 0;
         = 0;
        th = null;
        Throwable th = null;
        ?? r72 = 0;
        if (this.mFullBackupScheduleFile.exists()) {
            try {
                try {
                    FileInputStream fileInputStream = new FileInputStream(this.mFullBackupScheduleFile);
                    try {
                        try {
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                            try {
                                try {
                                    DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
                                    try {
                                        int i4 = dataInputStream.readInt();
                                        if (i4 != 1) {
                                            try {
                                                Slog.e(TAG, "Unknown backup schedule version " + i4);
                                                try {
                                                    $closeResource(null, dataInputStream);
                                                    try {
                                                        $closeResource(null, bufferedInputStream);
                                                        try {
                                                            $closeResource(null, fileInputStream);
                                                            return null;
                                                        } catch (Exception e) {
                                                            e = e;
                                                            r63 = 0;
                                                            Slog.e(TAG, "Unable to read backup schedule", e);
                                                            this.mFullBackupScheduleFile.delete();
                                                            r6 = r63;
                                                            if (r6 != 0) {
                                                            }
                                                            if (i2 != 0) {
                                                            }
                                                            return r62;
                                                        }
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        $closeResource(r7, fileInputStream);
                                                        throw th;
                                                    }
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    $closeResource(r72, bufferedInputStream);
                                                    throw th;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                                i = 0;
                                                th = th;
                                                throw th;
                                            }
                                        }
                                        int i5 = dataInputStream.readInt();
                                        ArrayList arrayList = new ArrayList(i5);
                                        HashSet hashSet = new HashSet(i5);
                                        int i6 = 0;
                                        while (i6 < i5) {
                                            try {
                                                try {
                                                    String utf = dataInputStream.readUTF();
                                                    try {
                                                        long j = dataInputStream.readLong();
                                                        hashSet.add(utf);
                                                        try {
                                                            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(utf, i3);
                                                            if (AppBackupUtils.appGetsFullBackup(packageInfo) && AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, this.mPackageManager)) {
                                                                arrayList.add(new FullBackupEntry(utf, j));
                                                            } else {
                                                                Slog.i(TAG, "Package " + utf + " no longer eligible for full backup");
                                                            }
                                                        } catch (PackageManager.NameNotFoundException e2) {
                                                            Slog.i(TAG, "Package " + utf + " not installed; dropping from full backup");
                                                        }
                                                        i6++;
                                                        i3 = 0;
                                                        obj = null;
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        i = 0;
                                                        throw th;
                                                    }
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    th = null;
                                                    $closeResource(th, dataInputStream);
                                                    throw th;
                                                }
                                            } catch (Throwable th7) {
                                                th = th7;
                                                i = 0;
                                                th = th;
                                                throw th;
                                            }
                                        }
                                        try {
                                            i3 = 0;
                                            for (PackageInfo packageInfo2 : storableApplications) {
                                                try {
                                                    try {
                                                        if (AppBackupUtils.appGetsFullBackup(packageInfo2) && AppBackupUtils.appIsEligibleForBackup(packageInfo2.applicationInfo, this.mPackageManager) && !hashSet.contains(packageInfo2.packageName)) {
                                                            arrayList.add(new FullBackupEntry(packageInfo2.packageName, 0L));
                                                            i3 = 1;
                                                        }
                                                    } catch (Throwable th8) {
                                                        th = th8;
                                                        i = i3;
                                                    }
                                                } catch (Throwable th9) {
                                                    th = th9;
                                                    th = null;
                                                    $closeResource(th, dataInputStream);
                                                    throw th;
                                                }
                                            }
                                            Collections.sort(arrayList);
                                            $closeResource(null, dataInputStream);
                                            $closeResource(null, bufferedInputStream);
                                            $closeResource(null, fileInputStream);
                                            r6 = arrayList;
                                        } catch (Throwable th10) {
                                            th = th10;
                                            th = null;
                                            $closeResource(th, dataInputStream);
                                            throw th;
                                        }
                                    } catch (Throwable th11) {
                                        th = th11;
                                    }
                                    try {
                                        throw th;
                                    } catch (Throwable th12) {
                                        th = th12;
                                        $closeResource(th, dataInputStream);
                                        throw th;
                                    }
                                } catch (Throwable th13) {
                                    th = th13;
                                }
                            } catch (Throwable th14) {
                                r72 = th14;
                                throw r72;
                            }
                        } catch (Throwable th15) {
                            th = th15;
                        }
                    } catch (Throwable th16) {
                        r7 = th16;
                        throw r7;
                    }
                } catch (Exception e3) {
                    e = e3;
                    r63 = 0;
                    i3 = 0;
                }
            } catch (Exception e4) {
                e = e4;
            }
        } else {
            r6 = 0;
            i3 = 0;
        }
        if (r6 != 0) {
            ArrayList arrayList2 = new ArrayList(storableApplications.size());
            for (PackageInfo packageInfo3 : storableApplications) {
                if (AppBackupUtils.appGetsFullBackup(packageInfo3) && AppBackupUtils.appIsEligibleForBackup(packageInfo3.applicationInfo, this.mPackageManager)) {
                    arrayList2.add(new FullBackupEntry(packageInfo3.packageName, 0L));
                }
            }
            i2 = 1;
            r62 = arrayList2;
        } else {
            i2 = i3;
            r62 = r6;
        }
        if (i2 != 0) {
            writeFullBackupScheduleAsync();
        }
        return r62;
    }

    private void writeFullBackupScheduleAsync() {
        this.mBackupHandler.removeCallbacks(this.mFullBackupScheduleWriter);
        this.mBackupHandler.post(this.mFullBackupScheduleWriter);
    }

    private void parseLeftoverJournals() throws Exception {
        for (DataChangedJournal dataChangedJournal : DataChangedJournal.listJournals(this.mJournalDir)) {
            if (!dataChangedJournal.equals(this.mJournal)) {
                try {
                    dataChangedJournal.forEach(new DataChangedJournal.Consumer() {
                        @Override
                        public final void accept(String str) {
                            BackupManagerService.lambda$parseLeftoverJournals$0(this.f$0, str);
                        }
                    });
                } catch (IOException e) {
                    Slog.e(TAG, "Can't read " + dataChangedJournal, e);
                }
            }
        }
    }

    public static void lambda$parseLeftoverJournals$0(BackupManagerService backupManagerService, String str) {
        Slog.i(TAG, "Found stale backup journal, scheduling");
        backupManagerService.dataChangedImpl(str);
    }

    public byte[] randomBytes(int i) {
        byte[] bArr = new byte[i / 8];
        this.mRng.nextBytes(bArr);
        return bArr;
    }

    @Override
    public boolean setBackupPassword(String str, String str2) {
        return this.mBackupPasswordManager.setBackupPassword(str, str2);
    }

    @Override
    public boolean hasBackupPassword() {
        return this.mBackupPasswordManager.hasBackupPassword();
    }

    public boolean backupPasswordMatches(String str) {
        return this.mBackupPasswordManager.backupPasswordMatches(str);
    }

    public void recordInitPending(boolean z, String str, String str2) {
        synchronized (this.mQueueLock) {
            File file = new File(new File(this.mBaseStateDir, str2), INIT_SENTINEL_FILE_NAME);
            if (z) {
                this.mPendingInits.add(str);
                try {
                    new FileOutputStream(file).close();
                } catch (IOException e) {
                }
            } else {
                file.delete();
                this.mPendingInits.remove(str);
            }
        }
    }

    public void resetBackupState(File file) {
        int i;
        synchronized (this.mQueueLock) {
            this.mProcessedPackagesJournal.reset();
            this.mCurrentToken = 0L;
            writeRestoreTokens();
            for (File file2 : file.listFiles()) {
                if (!file2.getName().equals(INIT_SENTINEL_FILE_NAME)) {
                    file2.delete();
                }
            }
        }
        synchronized (this.mBackupParticipants) {
            int size = this.mBackupParticipants.size();
            for (i = 0; i < size; i++) {
                HashSet<String> hashSetValueAt = this.mBackupParticipants.valueAt(i);
                if (hashSetValueAt != null) {
                    Iterator<String> it = hashSetValueAt.iterator();
                    while (it.hasNext()) {
                        dataChangedImpl(it.next());
                    }
                }
            }
        }
    }

    private void onTransportRegistered(String str, String str2) {
        Slog.d(TAG, "Transport " + str + " registered " + (SystemClock.elapsedRealtime() - this.mRegisterTransportsRequestedTime) + "ms after first request (delay = " + INITIALIZATION_DELAY_MILLIS + "ms)");
        File file = new File(this.mBaseStateDir, str2);
        file.mkdirs();
        if (new File(file, INIT_SENTINEL_FILE_NAME).exists()) {
            synchronized (this.mQueueLock) {
                this.mPendingInits.add(str);
                this.mAlarmManager.set(0, System.currentTimeMillis() + 60000, this.mRunInitIntent);
            }
        }
    }

    class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String[] stringArrayExtra;
            boolean z;
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            boolean zEquals = true;
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                Uri data = intent.getData();
                if (data == null) {
                    return;
                }
                final String schemeSpecificPart = data.getSchemeSpecificPart();
                stringArrayExtra = schemeSpecificPart != null ? new String[]{schemeSpecificPart} : null;
                if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    final String[] stringArrayExtra2 = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                    BackupManagerService.this.mBackupHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            BackupManagerService.this.mTransportManager.onPackageChanged(schemeSpecificPart, stringArrayExtra2);
                        }
                    });
                    return;
                } else {
                    zEquals = "android.intent.action.PACKAGE_ADDED".equals(action);
                    z = extras.getBoolean("android.intent.extra.REPLACING", false);
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                z = false;
            } else {
                stringArrayExtra = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action) ? intent.getStringArrayExtra("android.intent.extra.changed_package_list") : null;
                z = false;
                zEquals = false;
            }
            if (stringArrayExtra == null || stringArrayExtra.length == 0) {
                return;
            }
            int i = extras.getInt("android.intent.extra.UID");
            if (zEquals) {
                synchronized (BackupManagerService.this.mBackupParticipants) {
                    if (z) {
                        try {
                            BackupManagerService.this.removePackageParticipantsLocked(stringArrayExtra, i);
                        } finally {
                        }
                    }
                    BackupManagerService.this.addPackageParticipantsLocked(stringArrayExtra);
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                for (final String str : stringArrayExtra) {
                    try {
                        PackageInfo packageInfo = BackupManagerService.this.mPackageManager.getPackageInfo(str, 0);
                        if (!AppBackupUtils.appGetsFullBackup(packageInfo) || !AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, BackupManagerService.this.mPackageManager)) {
                            synchronized (BackupManagerService.this.mQueueLock) {
                                BackupManagerService.this.dequeueFullBackupLocked(str);
                            }
                            BackupManagerService.this.writeFullBackupScheduleAsync();
                        } else {
                            BackupManagerService.this.enqueueFullBackup(str, jCurrentTimeMillis);
                            BackupManagerService.this.scheduleNextFullBackupJob(0L);
                        }
                        BackupManagerService.this.mBackupHandler.post(new Runnable() {
                            @Override
                            public final void run() {
                                BackupManagerService.this.mTransportManager.onPackageAdded(str);
                            }
                        });
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.w(BackupManagerService.TAG, "Can't resolve new app " + str);
                    }
                }
                BackupManagerService.this.dataChangedImpl(BackupManagerService.PACKAGE_MANAGER_SENTINEL);
                return;
            }
            if (!z) {
                synchronized (BackupManagerService.this.mBackupParticipants) {
                    BackupManagerService.this.removePackageParticipantsLocked(stringArrayExtra, i);
                }
            }
            for (final String str2 : stringArrayExtra) {
                BackupManagerService.this.mBackupHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        BackupManagerService.this.mTransportManager.onPackageRemoved(str2);
                    }
                });
            }
        }
    }

    private void addPackageParticipantsLocked(String[] strArr) {
        List<PackageInfo> listAllAgentPackages = allAgentPackages();
        if (strArr != null) {
            for (String str : strArr) {
                addPackageParticipantsLockedInner(str, listAllAgentPackages);
            }
            return;
        }
        addPackageParticipantsLockedInner(null, listAllAgentPackages);
    }

    private void addPackageParticipantsLockedInner(String str, List<PackageInfo> list) {
        for (PackageInfo packageInfo : list) {
            if (str == null || packageInfo.packageName.equals(str)) {
                int i = packageInfo.applicationInfo.uid;
                HashSet<String> hashSet = this.mBackupParticipants.get(i);
                if (hashSet == null) {
                    hashSet = new HashSet<>();
                    this.mBackupParticipants.put(i, hashSet);
                }
                hashSet.add(packageInfo.packageName);
                this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(16, packageInfo.packageName));
            }
        }
    }

    private void removePackageParticipantsLocked(String[] strArr, int i) {
        if (strArr == null) {
            Slog.w(TAG, "removePackageParticipants with null list");
            return;
        }
        for (String str : strArr) {
            HashSet<String> hashSet = this.mBackupParticipants.get(i);
            if (hashSet != null && hashSet.contains(str)) {
                removePackageFromSetLocked(hashSet, str);
                if (hashSet.isEmpty()) {
                    this.mBackupParticipants.remove(i);
                }
            }
        }
    }

    private void removePackageFromSetLocked(HashSet<String> hashSet, String str) {
        if (hashSet.contains(str)) {
            hashSet.remove(str);
            this.mPendingBackups.remove(str);
        }
    }

    private List<PackageInfo> allAgentPackages() {
        List<PackageInfo> installedPackages = this.mPackageManager.getInstalledPackages(134217728);
        for (int size = installedPackages.size() - 1; size >= 0; size--) {
            PackageInfo packageInfo = installedPackages.get(size);
            try {
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                if ((applicationInfo.flags & 32768) == 0 || applicationInfo.backupAgentName == null || (applicationInfo.flags & 67108864) != 0) {
                    installedPackages.remove(size);
                } else {
                    packageInfo.applicationInfo.sharedLibraryFiles = this.mPackageManager.getApplicationInfo(packageInfo.packageName, 1024).sharedLibraryFiles;
                }
            } catch (PackageManager.NameNotFoundException e) {
                installedPackages.remove(size);
            }
        }
        return installedPackages;
    }

    public void logBackupComplete(String str) {
        if (str.equals(PACKAGE_MANAGER_SENTINEL)) {
            return;
        }
        for (String str2 : this.mConstants.getBackupFinishedNotificationReceivers()) {
            Intent intent = new Intent();
            intent.setAction(BACKUP_FINISHED_ACTION);
            intent.setPackage(str2);
            intent.addFlags(268435488);
            intent.putExtra(BACKUP_FINISHED_PACKAGE_EXTRA, str);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.OWNER);
        }
        this.mProcessedPackagesJournal.addPackage(str);
    }

    public void writeRestoreTokens() throws Exception {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(this.mTokenFile, "rwd");
            try {
                randomAccessFile.writeInt(1);
                randomAccessFile.writeLong(this.mAncestralToken);
                randomAccessFile.writeLong(this.mCurrentToken);
                if (this.mAncestralPackages == null) {
                    randomAccessFile.writeInt(-1);
                } else {
                    randomAccessFile.writeInt(this.mAncestralPackages.size());
                    Slog.v(TAG, "Ancestral packages:  " + this.mAncestralPackages.size());
                    Iterator<String> it = this.mAncestralPackages.iterator();
                    while (it.hasNext()) {
                        randomAccessFile.writeUTF(it.next());
                    }
                }
            } finally {
                $closeResource(null, randomAccessFile);
            }
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write token file:", e);
        }
    }

    @Override
    public IBackupAgent bindToAgentSynchronous(ApplicationInfo applicationInfo, int i) {
        IBackupAgent iBackupAgent;
        synchronized (this.mAgentConnectLock) {
            this.mConnecting = true;
            iBackupAgent = null;
            this.mConnectedAgent = null;
            try {
                if (this.mActivityManager.bindBackupAgent(applicationInfo.packageName, i, 0)) {
                    Slog.d(TAG, "awaiting agent for " + applicationInfo);
                    long jCurrentTimeMillis = System.currentTimeMillis() + 10000;
                    while (this.mConnecting && this.mConnectedAgent == null && System.currentTimeMillis() < jCurrentTimeMillis) {
                        try {
                            this.mAgentConnectLock.wait(5000L);
                        } catch (InterruptedException e) {
                            Slog.w(TAG, "Interrupted: " + e);
                            this.mConnecting = false;
                            this.mConnectedAgent = null;
                        }
                    }
                    if (this.mConnecting) {
                        Slog.w(TAG, "Timeout waiting for agent " + applicationInfo);
                        this.mConnectedAgent = null;
                    }
                    Slog.i(TAG, "got agent " + this.mConnectedAgent);
                    iBackupAgent = this.mConnectedAgent;
                }
            } catch (RemoteException e2) {
            }
        }
        if (iBackupAgent == null) {
            try {
                this.mActivityManager.clearPendingBackup();
            } catch (RemoteException e3) {
            }
        }
        return iBackupAgent;
    }

    public void clearApplicationDataSynchronous(String str, boolean z) {
        try {
            if ((this.mPackageManager.getPackageInfo(str, 0).applicationInfo.flags & 64) == 0) {
                return;
            }
            ClearDataObserver clearDataObserver = new ClearDataObserver(this);
            synchronized (this.mClearDataLock) {
                this.mClearingData = true;
                try {
                    this.mActivityManager.clearApplicationUserData(str, z, clearDataObserver, 0);
                } catch (RemoteException e) {
                }
                long jCurrentTimeMillis = System.currentTimeMillis() + 10000;
                while (this.mClearingData && System.currentTimeMillis() < jCurrentTimeMillis) {
                    try {
                        this.mClearDataLock.wait(5000L);
                    } catch (InterruptedException e2) {
                        this.mClearingData = false;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e3) {
            Slog.w(TAG, "Tried to clear data for " + str + " but not found");
        }
    }

    @Override
    public long getAvailableRestoreToken(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreToken");
        long j = this.mAncestralToken;
        synchronized (this.mQueueLock) {
            if (this.mCurrentToken != 0 && this.mProcessedPackagesJournal.hasBeenProcessed(str)) {
                j = this.mCurrentToken;
            }
        }
        return j;
    }

    @Override
    public int requestBackup(String[] strArr, IBackupObserver iBackupObserver, int i) {
        return requestBackup(strArr, iBackupObserver, null, i);
    }

    @Override
    public int requestBackup(String[] strArr, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, int i) {
        int i2;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "requestBackup");
        if (strArr == null || strArr.length < 1) {
            Slog.e(TAG, "No packages named for backup request");
            BackupObserverUtils.sendBackupFinished(iBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, 49, null, 1, null);
            throw new IllegalArgumentException("No packages are provided for backup");
        }
        if (!this.mEnabled || !this.mProvisioned) {
            Slog.i(TAG, "Backup requested but e=" + this.mEnabled + " p=" + this.mProvisioned);
            BackupObserverUtils.sendBackupFinished(iBackupObserver, -2001);
            if (this.mProvisioned) {
                i2 = 13;
            } else {
                i2 = 14;
            }
            BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, i2, null, 3, null);
            return -2001;
        }
        try {
            String transportDirName = this.mTransportManager.getTransportDirName(this.mTransportManager.getCurrentTransportName());
            final TransportClient currentTransportClientOrThrow = this.mTransportManager.getCurrentTransportClientOrThrow("BMS.requestBackup()");
            OnTaskFinishedListener onTaskFinishedListener = new OnTaskFinishedListener() {
                @Override
                public final void onFinished(String str) {
                    this.f$0.mTransportManager.disposeOfTransportClient(currentTransportClientOrThrow, str);
                }
            };
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            for (String str : strArr) {
                if (PACKAGE_MANAGER_SENTINEL.equals(str)) {
                    arrayList2.add(str);
                } else {
                    try {
                        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 134217728);
                        if (!AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, this.mPackageManager)) {
                            BackupObserverUtils.sendBackupOnPackageResult(iBackupObserver, str, -2001);
                        } else if (AppBackupUtils.appGetsFullBackup(packageInfo)) {
                            arrayList.add(packageInfo.packageName);
                        } else {
                            arrayList2.add(packageInfo.packageName);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        BackupObserverUtils.sendBackupOnPackageResult(iBackupObserver, str, -2002);
                    }
                }
            }
            EventLog.writeEvent(EventLogTags.BACKUP_REQUESTED, Integer.valueOf(strArr.length), Integer.valueOf(arrayList2.size()), Integer.valueOf(arrayList.size()));
            boolean z = (i & 1) != 0;
            Message messageObtainMessage = this.mBackupHandler.obtainMessage(15);
            messageObtainMessage.obj = new BackupParams(currentTransportClientOrThrow, transportDirName, arrayList2, arrayList, iBackupObserver, iBackupManagerMonitor, onTaskFinishedListener, true, z);
            this.mBackupHandler.sendMessage(messageObtainMessage);
            return 0;
        } catch (TransportNotRegisteredException e2) {
            BackupObserverUtils.sendBackupFinished(iBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, 50, null, 1, null);
            return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
    }

    @Override
    public void cancelBackups() {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "cancelBackups");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ArrayList arrayList = new ArrayList();
            synchronized (this.mCurrentOpLock) {
                for (int i = 0; i < this.mCurrentOperations.size(); i++) {
                    Operation operationValueAt = this.mCurrentOperations.valueAt(i);
                    int iKeyAt = this.mCurrentOperations.keyAt(i);
                    if (operationValueAt.type == 2) {
                        arrayList.add(Integer.valueOf(iKeyAt));
                    }
                }
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                handleCancel(((Integer) it.next()).intValue(), true);
            }
            KeyValueBackupJob.schedule(this.mContext, AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT, this.mConstants);
            FullBackupJob.schedule(this.mContext, AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, this.mConstants);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void prepareOperationTimeout(int i, long j, BackupRestoreTask backupRestoreTask, int i2) {
        if (i2 != 0 && i2 != 1) {
            Slog.wtf(TAG, "prepareOperationTimeout() doesn't support operation " + Integer.toHexString(i) + " of type " + i2);
            return;
        }
        synchronized (this.mCurrentOpLock) {
            this.mCurrentOperations.put(i, new Operation(0, backupRestoreTask, i2));
            this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(getMessageIdForOperationType(i2), i, 0, backupRestoreTask), j);
        }
    }

    private int getMessageIdForOperationType(int i) {
        switch (i) {
            case 0:
                return 17;
            case 1:
                return 18;
            default:
                Slog.wtf(TAG, "getMessageIdForOperationType called on invalid operation type: " + i);
                return -1;
        }
    }

    public void removeOperation(int i) {
        synchronized (this.mCurrentOpLock) {
            if (this.mCurrentOperations.get(i) == null) {
                Slog.w(TAG, "Duplicate remove for operation. token=" + Integer.toHexString(i));
            }
            this.mCurrentOperations.remove(i);
        }
    }

    @Override
    public boolean waitUntilOperationComplete(int i) {
        Operation operation;
        int i2;
        synchronized (this.mCurrentOpLock) {
            while (true) {
                operation = this.mCurrentOperations.get(i);
                if (operation != null) {
                    if (operation.state == 0) {
                        try {
                            this.mCurrentOpLock.wait();
                        } catch (InterruptedException e) {
                        }
                    } else {
                        i2 = operation.state;
                        break;
                    }
                } else {
                    i2 = 0;
                    break;
                }
            }
        }
        removeOperation(i);
        if (operation != null) {
            this.mBackupHandler.removeMessages(getMessageIdForOperationType(operation.type));
        }
        if (i2 != 1) {
            return false;
        }
        return true;
    }

    public void handleCancel(int i, boolean z) {
        Operation operation;
        synchronized (this.mCurrentOpLock) {
            operation = this.mCurrentOperations.get(i);
            int i2 = operation != null ? operation.state : -1;
            if (i2 == 1) {
                Slog.w(TAG, "Operation already got an ack.Should have been removed from mCurrentOperations.");
                operation = null;
                this.mCurrentOperations.delete(i);
            } else if (i2 == 0) {
                Slog.v(TAG, "Cancel: token=" + Integer.toHexString(i));
                operation.state = -1;
                if (operation.type == 0 || operation.type == 1) {
                    this.mBackupHandler.removeMessages(getMessageIdForOperationType(operation.type));
                }
            }
            this.mCurrentOpLock.notifyAll();
        }
        if (operation != null && operation.callback != null) {
            operation.callback.handleCancel(z);
        }
    }

    public boolean isBackupOperationInProgress() {
        synchronized (this.mCurrentOpLock) {
            for (int i = 0; i < this.mCurrentOperations.size(); i++) {
                Operation operationValueAt = this.mCurrentOperations.valueAt(i);
                if (operationValueAt.type == 2 && operationValueAt.state == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void tearDownAgentAndKill(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return;
        }
        try {
            this.mActivityManager.unbindBackupAgent(applicationInfo);
            if (applicationInfo.uid >= 10000 && !applicationInfo.packageName.equals("com.android.backupconfirm")) {
                this.mActivityManager.killApplicationProcess(applicationInfo.processName, applicationInfo.uid);
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "Lost app trying to shut down");
        }
    }

    public boolean deviceIsEncrypted() {
        try {
            if (this.mStorageManager.getEncryptionState() != 1) {
                if (this.mStorageManager.getPasswordType() != 1) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "Unable to communicate with storagemanager service: " + e.getMessage());
            return true;
        }
    }

    public void scheduleNextFullBackupJob(long j) {
        synchronized (this.mQueueLock) {
            if (this.mFullBackupQueue.size() > 0) {
                long jCurrentTimeMillis = System.currentTimeMillis() - this.mFullBackupQueue.get(0).lastBackup;
                long fullBackupIntervalMilliseconds = this.mConstants.getFullBackupIntervalMilliseconds();
                final long jMax = Math.max(j, jCurrentTimeMillis < fullBackupIntervalMilliseconds ? fullBackupIntervalMilliseconds - jCurrentTimeMillis : 0L);
                this.mBackupHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FullBackupJob.schedule(BackupManagerService.this.mContext, jMax, BackupManagerService.this.mConstants);
                    }
                }, 2500L);
            } else {
                Slog.i(TAG, "Full backup queue empty; not scheduling");
            }
        }
    }

    @GuardedBy("mQueueLock")
    private void dequeueFullBackupLocked(String str) {
        for (int size = this.mFullBackupQueue.size() - 1; size >= 0; size--) {
            if (str.equals(this.mFullBackupQueue.get(size).packageName)) {
                this.mFullBackupQueue.remove(size);
            }
        }
    }

    public void enqueueFullBackup(String str, long j) {
        int size;
        FullBackupEntry fullBackupEntry = new FullBackupEntry(str, j);
        synchronized (this.mQueueLock) {
            dequeueFullBackupLocked(str);
            if (j > 0) {
                size = this.mFullBackupQueue.size() - 1;
                while (true) {
                    if (size < 0) {
                        break;
                    }
                    if (this.mFullBackupQueue.get(size).lastBackup > j) {
                        size--;
                    } else {
                        this.mFullBackupQueue.add(size + 1, fullBackupEntry);
                        break;
                    }
                }
            } else {
                size = -1;
            }
            if (size < 0) {
                this.mFullBackupQueue.add(0, fullBackupEntry);
            }
        }
        writeFullBackupScheduleAsync();
    }

    private boolean fullBackupAllowable(String str) {
        if (!this.mTransportManager.isTransportRegistered(str)) {
            Slog.w(TAG, "Transport not registered; full data backup not performed");
            return false;
        }
        try {
            if (new File(new File(this.mBaseStateDir, this.mTransportManager.getTransportDirName(str)), PACKAGE_MANAGER_SENTINEL).length() <= 0) {
                Slog.i(TAG, "Full backup requested but dataset not yet initialized");
                return false;
            }
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Unable to get transport name: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean beginFullBackup(FullBackupJob fullBackupJob) {
        long fullBackupIntervalMilliseconds;
        long keyValueBackupIntervalMilliseconds;
        long j;
        PackageInfo packageInfo;
        long jCurrentTimeMillis;
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        synchronized (this.mConstants) {
            fullBackupIntervalMilliseconds = this.mConstants.getFullBackupIntervalMilliseconds();
            keyValueBackupIntervalMilliseconds = this.mConstants.getKeyValueBackupIntervalMilliseconds();
        }
        int i = 0;
        if (!this.mEnabled || !this.mProvisioned) {
            return false;
        }
        if (this.mPowerManager.getPowerSaveState(4).batterySaverEnabled) {
            Slog.i(TAG, "Deferring scheduled full backups in battery saver mode");
            FullBackupJob.schedule(this.mContext, keyValueBackupIntervalMilliseconds, this.mConstants);
            return false;
        }
        Slog.i(TAG, "Beginning scheduled full backup operation");
        synchronized (this.mQueueLock) {
            if (this.mRunningFullBackupTask != null) {
                Slog.e(TAG, "Backup triggered but one already/still running!");
                return false;
            }
            FullBackupEntry fullBackupEntry = null;
            final long j2 = fullBackupIntervalMilliseconds;
            int i2 = 1;
            while (true) {
                if (this.mFullBackupQueue.size() == 0) {
                    Slog.i(TAG, "Backup queue empty; doing nothing");
                    i2 = i;
                    break;
                }
                if (!fullBackupAllowable(this.mTransportManager.getCurrentTransportName())) {
                    j2 = keyValueBackupIntervalMilliseconds;
                    i2 = i;
                }
                if (i2 != 0) {
                    fullBackupEntry = this.mFullBackupQueue.get(i);
                    long j3 = jCurrentTimeMillis2 - fullBackupEntry.lastBackup;
                    int i3 = j3 >= fullBackupIntervalMilliseconds ? 1 : i;
                    if (i3 == 0) {
                        j2 = fullBackupIntervalMilliseconds - j3;
                        i2 = i3;
                        break;
                    }
                    try {
                        packageInfo = this.mPackageManager.getPackageInfo(fullBackupEntry.packageName, i);
                    } catch (PackageManager.NameNotFoundException e) {
                        j = j2;
                        i = 0;
                    } catch (RemoteException e2) {
                        j = j2;
                        i = 0;
                    }
                    if (AppBackupUtils.appGetsFullBackup(packageInfo)) {
                        int i4 = ((packageInfo.applicationInfo.privateFlags & 8192) == 0 && this.mActivityManager.isAppForeground(packageInfo.applicationInfo.uid)) ? 1 : i;
                        if (i4 != 0) {
                            try {
                                j = j2;
                                jCurrentTimeMillis = System.currentTimeMillis() + AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT + ((long) this.mTokenGenerator.nextInt(BUSY_BACKOFF_FUZZ));
                            } catch (PackageManager.NameNotFoundException e3) {
                                j = j2;
                            } catch (RemoteException e4) {
                                j = j2;
                            }
                            try {
                                Slog.i(TAG, "Full backup time but " + fullBackupEntry.packageName + " is busy; deferring to " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(jCurrentTimeMillis)));
                                enqueueFullBackup(fullBackupEntry.packageName, jCurrentTimeMillis - fullBackupIntervalMilliseconds);
                            } catch (PackageManager.NameNotFoundException e5) {
                                i = i4;
                                i2 = this.mFullBackupQueue.size() <= 1 ? 1 : 0;
                                if (i != 0) {
                                }
                            } catch (RemoteException e6) {
                                i = i4;
                            }
                        } else {
                            j = j2;
                        }
                        i = i4;
                        i2 = i3;
                    } else {
                        try {
                            this.mFullBackupQueue.remove(i);
                            j = j2;
                            i2 = i3;
                            i = 1;
                        } catch (PackageManager.NameNotFoundException e7) {
                            j = j2;
                            if (this.mFullBackupQueue.size() <= 1) {
                            }
                        } catch (RemoteException e8) {
                            j = j2;
                            i2 = i3;
                        }
                    }
                } else {
                    j = j2;
                    i = 0;
                }
                if (i != 0) {
                    j2 = j;
                    break;
                }
                j2 = j;
                i = 0;
            }
            if (i2 != 0) {
                this.mFullBackupQueue.remove(0);
                this.mRunningFullBackupTask = PerformFullTransportBackupTask.newWithCurrentTransport(this, null, new String[]{fullBackupEntry.packageName}, true, fullBackupJob, new CountDownLatch(1), null, null, false, "BMS.beginFullBackup()");
                this.mWakelock.acquire();
                new Thread(this.mRunningFullBackupTask).start();
                return true;
            }
            Slog.i(TAG, "Nothing pending full backup; rescheduling +" + j2);
            this.mBackupHandler.post(new Runnable() {
                @Override
                public void run() {
                    FullBackupJob.schedule(BackupManagerService.this.mContext, j2, BackupManagerService.this.mConstants);
                }
            });
            return false;
        }
    }

    @Override
    public void endFullBackup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PerformFullTransportBackupTask performFullTransportBackupTask;
                synchronized (BackupManagerService.this.mQueueLock) {
                    if (BackupManagerService.this.mRunningFullBackupTask != null) {
                        performFullTransportBackupTask = BackupManagerService.this.mRunningFullBackupTask;
                    } else {
                        performFullTransportBackupTask = null;
                    }
                }
                if (performFullTransportBackupTask != null) {
                    Slog.i(BackupManagerService.TAG, "Telling running backup to stop");
                    performFullTransportBackupTask.handleCancel(true);
                }
            }
        }, "end-full-backup").start();
    }

    public void restoreWidgetData(String str, byte[] bArr) {
        AppWidgetBackupBridge.restoreWidgetState(str, bArr, 0);
    }

    public void dataChangedImpl(String str) {
        dataChangedImpl(str, dataChangedTargets(str));
    }

    private void dataChangedImpl(String str, HashSet<String> hashSet) {
        if (hashSet == null) {
            Slog.w(TAG, "dataChanged but no participant pkg='" + str + "' uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mQueueLock) {
            if (hashSet.contains(str)) {
                if (this.mPendingBackups.put(str, new BackupRequest(str)) == null) {
                    writeToJournalLocked(str);
                }
            }
        }
        KeyValueBackupJob.schedule(this.mContext, this.mConstants);
    }

    private HashSet<String> dataChangedTargets(String str) {
        HashSet<String> hashSetUnion;
        HashSet<String> hashSet;
        if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
            synchronized (this.mBackupParticipants) {
                hashSet = this.mBackupParticipants.get(Binder.getCallingUid());
            }
            return hashSet;
        }
        if (PACKAGE_MANAGER_SENTINEL.equals(str)) {
            return Sets.newHashSet(new String[]{PACKAGE_MANAGER_SENTINEL});
        }
        synchronized (this.mBackupParticipants) {
            hashSetUnion = SparseArrayUtils.union(this.mBackupParticipants);
        }
        return hashSetUnion;
    }

    private void writeToJournalLocked(String str) throws Exception {
        try {
            if (this.mJournal == null) {
                this.mJournal = DataChangedJournal.newJournal(this.mJournalDir);
            }
            this.mJournal.addPackage(str);
        } catch (IOException e) {
            Slog.e(TAG, "Can't write " + str + " to backup journal", e);
            this.mJournal = null;
        }
    }

    @Override
    public void dataChanged(final String str) {
        if (UserHandle.getCallingUserId() != 0) {
            return;
        }
        final HashSet<String> hashSetDataChangedTargets = dataChangedTargets(str);
        if (hashSetDataChangedTargets == null) {
            Slog.w(TAG, "dataChanged but no participant pkg='" + str + "' uid=" + Binder.getCallingUid());
            return;
        }
        this.mBackupHandler.post(new Runnable() {
            @Override
            public void run() {
                BackupManagerService.this.dataChangedImpl(str, hashSetDataChangedTargets);
            }
        });
    }

    @Override
    public void initializeTransports(String[] strArr, IBackupObserver iBackupObserver) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "initializeTransport");
        Slog.v(TAG, "initializeTransport(): " + Arrays.asList(strArr));
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mWakelock.acquire();
            this.mBackupHandler.post(new PerformInitializeTask(this, strArr, iBackupObserver, new OnTaskFinishedListener() {
                @Override
                public final void onFinished(String str) {
                    this.f$0.mWakelock.release();
                }
            }));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void clearBackupData(String str, String str2) {
        HashSet<String> packagesCopy;
        Slog.v(TAG, "clearBackupData() of " + str2 + " on " + str);
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str2, 134217728);
            if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                packagesCopy = this.mBackupParticipants.get(Binder.getCallingUid());
            } else {
                packagesCopy = this.mProcessedPackagesJournal.getPackagesCopy();
            }
            if (packagesCopy.contains(str2)) {
                this.mBackupHandler.removeMessages(12);
                synchronized (this.mQueueLock) {
                    final TransportClient transportClient = this.mTransportManager.getTransportClient(str, "BMS.clearBackupData()");
                    if (transportClient == null) {
                        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(12, new ClearRetryParams(str, str2)), AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                        return;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    OnTaskFinishedListener onTaskFinishedListener = new OnTaskFinishedListener() {
                        @Override
                        public final void onFinished(String str3) {
                            this.f$0.mTransportManager.disposeOfTransportClient(transportClient, str3);
                        }
                    };
                    this.mWakelock.acquire();
                    this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(4, new ClearParams(transportClient, packageInfo, onTaskFinishedListener)));
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(TAG, "No such package '" + str2 + "' - not clearing backup data");
        }
    }

    @Override
    public void backupNow() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "backupNow");
        if (this.mPowerManager.getPowerSaveState(5).batterySaverEnabled) {
            Slog.v(TAG, "Not running backup while in battery save mode");
            KeyValueBackupJob.schedule(this.mContext, this.mConstants);
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

    public boolean deviceIsProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    @Override
    public void adbBackup(ParcelFileDescriptor parcelFileDescriptor, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, boolean z8, String[] strArr) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbBackup");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Backup supported only for the device owner");
        }
        if (!z5 && !z3 && (strArr == null || strArr.length == 0)) {
            throw new IllegalArgumentException("Backup requested but neither shared nor any apps named");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!deviceIsProvisioned()) {
                Slog.i(TAG, "Backup not supported before setup");
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IO error closing output for adb backup: " + e.getMessage());
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Slog.d(TAG, "Adb backup processing complete.");
                return;
            }
            Slog.v(TAG, "Requesting backup: apks=" + z + " obb=" + z2 + " shared=" + z3 + " all=" + z5 + " system=" + z6 + " includekeyvalue=" + z8 + " pkgs=" + strArr);
            Slog.i(TAG, "Beginning adb backup...");
            AdbBackupParams adbBackupParams = new AdbBackupParams(parcelFileDescriptor, z, z2, z3, z4, z5, z6, z7, z8, strArr);
            int iGenerateRandomIntegerToken = generateRandomIntegerToken();
            synchronized (this.mAdbBackupRestoreConfirmations) {
                this.mAdbBackupRestoreConfirmations.put(iGenerateRandomIntegerToken, adbBackupParams);
            }
            Slog.d(TAG, "Starting backup confirmation UI, token=" + iGenerateRandomIntegerToken);
            if (!startConfirmationUi(iGenerateRandomIntegerToken, "fullback")) {
                Slog.e(TAG, "Unable to launch backup confirmation UI");
                this.mAdbBackupRestoreConfirmations.delete(iGenerateRandomIntegerToken);
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e2) {
                    Slog.e(TAG, "IO error closing output for adb backup: " + e2.getMessage());
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Slog.d(TAG, "Adb backup processing complete.");
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(iGenerateRandomIntegerToken, adbBackupParams);
            Slog.d(TAG, "Waiting for backup completion...");
            waitForCompletion(adbBackupParams);
            try {
                parcelFileDescriptor.close();
            } catch (IOException e3) {
                Slog.e(TAG, "IO error closing output for adb backup: " + e3.getMessage());
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Slog.d(TAG, "Adb backup processing complete.");
        } catch (Throwable th) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e4) {
                Slog.e(TAG, "IO error closing output for adb backup: " + e4.getMessage());
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Slog.d(TAG, "Adb backup processing complete.");
            throw th;
        }
    }

    @Override
    public void fullTransportBackup(String[] strArr) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullTransportBackup");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        if (!fullBackupAllowable(this.mTransportManager.getCurrentTransportName())) {
            Slog.i(TAG, "Full backup not currently possible -- key/value backup not yet run?");
        } else {
            Slog.d(TAG, "fullTransportBackup()");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                PerformFullTransportBackupTask performFullTransportBackupTaskNewWithCurrentTransport = PerformFullTransportBackupTask.newWithCurrentTransport(this, null, strArr, false, null, countDownLatch, null, null, false, "BMS.fullTransportBackup()");
                this.mWakelock.acquire();
                new Thread(performFullTransportBackupTaskNewWithCurrentTransport, "full-transport-master").start();
                while (true) {
                    try {
                        countDownLatch.await();
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                for (String str : strArr) {
                    enqueueFullBackup(str, jCurrentTimeMillis);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        Slog.d(TAG, "Done with full transport backup.");
    }

    @Override
    public void adbRestore(ParcelFileDescriptor parcelFileDescriptor) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbRestore");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!deviceIsProvisioned()) {
                Slog.i(TAG, "Full restore not permitted before setup");
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Error trying to close fd after adb restore: " + e);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Slog.i(TAG, "adb restore processing complete.");
                return;
            }
            Slog.i(TAG, "Beginning restore...");
            AdbRestoreParams adbRestoreParams = new AdbRestoreParams(parcelFileDescriptor);
            int iGenerateRandomIntegerToken = generateRandomIntegerToken();
            synchronized (this.mAdbBackupRestoreConfirmations) {
                this.mAdbBackupRestoreConfirmations.put(iGenerateRandomIntegerToken, adbRestoreParams);
            }
            Slog.d(TAG, "Starting restore confirmation UI, token=" + iGenerateRandomIntegerToken);
            if (!startConfirmationUi(iGenerateRandomIntegerToken, "fullrest")) {
                Slog.e(TAG, "Unable to launch restore confirmation");
                this.mAdbBackupRestoreConfirmations.delete(iGenerateRandomIntegerToken);
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Error trying to close fd after adb restore: " + e2);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Slog.i(TAG, "adb restore processing complete.");
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(iGenerateRandomIntegerToken, adbRestoreParams);
            Slog.d(TAG, "Waiting for restore completion...");
            waitForCompletion(adbRestoreParams);
            try {
                parcelFileDescriptor.close();
            } catch (IOException e3) {
                Slog.w(TAG, "Error trying to close fd after adb restore: " + e3);
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Slog.i(TAG, "adb restore processing complete.");
        } catch (Throwable th) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e4) {
                Slog.w(TAG, "Error trying to close fd after adb restore: " + e4);
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Slog.i(TAG, "adb restore processing complete.");
            throw th;
        }
    }

    private boolean startConfirmationUi(int i, String str) {
        try {
            Intent intent = new Intent(str);
            intent.setClassName("com.android.backupconfirm", "com.android.backupconfirm.BackupRestoreConfirmation");
            intent.putExtra("conftoken", i);
            intent.addFlags(536870912);
            this.mContext.startActivityAsUser(intent, UserHandle.SYSTEM);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private void startConfirmationTimeout(int i, AdbParams adbParams) {
        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(9, i, 0, adbParams), 60000L);
    }

    private void waitForCompletion(AdbParams adbParams) {
        synchronized (adbParams.latch) {
            while (!adbParams.latch.get()) {
                try {
                    adbParams.latch.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void signalAdbBackupRestoreCompletion(AdbParams adbParams) {
        synchronized (adbParams.latch) {
            adbParams.latch.set(true);
            adbParams.latch.notifyAll();
        }
    }

    @Override
    public void acknowledgeAdbBackupOrRestore(int i, boolean z, String str, String str2, IFullBackupRestoreObserver iFullBackupRestoreObserver) {
        int i2;
        Slog.d(TAG, "acknowledgeAdbBackupOrRestore : token=" + i + " allow=" + z);
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "acknowledgeAdbBackupOrRestore");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mAdbBackupRestoreConfirmations) {
                AdbParams adbParams = this.mAdbBackupRestoreConfirmations.get(i);
                if (adbParams != null) {
                    this.mBackupHandler.removeMessages(9, adbParams);
                    this.mAdbBackupRestoreConfirmations.delete(i);
                    if (z) {
                        if (adbParams instanceof AdbBackupParams) {
                            i2 = 2;
                        } else {
                            i2 = 10;
                        }
                        adbParams.observer = iFullBackupRestoreObserver;
                        adbParams.curPassword = str;
                        adbParams.encryptPassword = str2;
                        this.mWakelock.acquire();
                        this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(i2, adbParams));
                    } else {
                        Slog.w(TAG, "User rejected full backup/restore operation");
                        signalAdbBackupRestoreCompletion(adbParams);
                    }
                } else {
                    Slog.w(TAG, "Attempted to ack full backup/restore with invalid token");
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static boolean backupSettingMigrated(int i) {
        return new File(new File(Environment.getDataDirectory(), BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD), BACKUP_ENABLE_FILE).exists();
    }

    private static boolean readBackupEnableState(int i) throws Exception {
        File file = new File(new File(Environment.getDataDirectory(), BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD), BACKUP_ENABLE_FILE);
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                Throwable th = null;
                try {
                    return fileInputStream.read() != 0;
                } finally {
                    $closeResource(th, fileInputStream);
                }
            } catch (IOException e) {
                Slog.e(TAG, "Cannot read enable state; assuming disabled");
            }
        } else {
            Slog.i(TAG, "isBackupEnabled() => false due to absent settings file");
        }
        return false;
    }

    private static void writeBackupEnableState(boolean z, int i) {
        Throwable th;
        File file = new File(Environment.getDataDirectory(), BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD);
        File file2 = new File(file, BACKUP_ENABLE_FILE);
        File file3 = new File(file, "backup_enabled-stage");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            try {
                fileOutputStream.write(z ? 1 : 0);
                fileOutputStream.close();
                file3.renameTo(file2);
                $closeResource(null, fileOutputStream);
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, fileOutputStream);
                throw th;
            }
        } catch (IOException | RuntimeException e) {
            Slog.e(TAG, "Unable to record backup enable state; reverting to disabled: " + e.getMessage());
            Settings.Secure.putStringForUser(sInstance.mContext.getContentResolver(), BACKUP_ENABLE_FILE, null, i);
            file2.delete();
            file3.delete();
        }
    }

    @Override
    public void setBackupEnabled(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupEnabled");
        if (!z && this.mBackupPolicyEnforcer.getMandatoryBackupTransport() != null) {
            Slog.w(TAG, "Cannot disable backups when the mandatory backups policy is active.");
            return;
        }
        Slog.i(TAG, "Backup enabled => " + z);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            boolean z2 = this.mEnabled;
            synchronized (this) {
                writeBackupEnableState(z, 0);
                this.mEnabled = z;
            }
            synchronized (this.mQueueLock) {
                if (!z || z2) {
                    if (!z) {
                        KeyValueBackupJob.cancel(this.mContext);
                        if (z2 && this.mProvisioned) {
                            final ArrayList arrayList = new ArrayList();
                            final ArrayList arrayList2 = new ArrayList();
                            this.mTransportManager.forEachRegisteredTransport(new Consumer() {
                                @Override
                                public final void accept(Object obj) {
                                    BackupManagerService.lambda$setBackupEnabled$4(this.f$0, arrayList, arrayList2, (String) obj);
                                }
                            });
                            for (int i = 0; i < arrayList.size(); i++) {
                                recordInitPending(true, (String) arrayList.get(i), (String) arrayList2.get(i));
                            }
                            this.mAlarmManager.set(0, System.currentTimeMillis(), this.mRunInitIntent);
                        }
                    }
                } else {
                    try {
                        if (this.mProvisioned) {
                            KeyValueBackupJob.schedule(this.mContext, this.mConstants);
                            scheduleNextFullBackupJob(0L);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static void lambda$setBackupEnabled$4(BackupManagerService backupManagerService, List list, List list2, String str) {
        try {
            String transportDirName = backupManagerService.mTransportManager.getTransportDirName(str);
            list.add(str);
            list2.add(transportDirName);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unexpected unregistered transport", e);
        }
    }

    @Override
    public void setAutoRestore(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setAutoRestore");
        Slog.i(TAG, "Auto restore => " + z);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "backup_auto_restore", z ? 1 : 0);
                this.mAutoRestore = z;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void setBackupProvisioned(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupProvisioned");
    }

    @Override
    public boolean isBackupEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isBackupEnabled");
        return this.mEnabled;
    }

    @Override
    public String getCurrentTransport() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getCurrentTransport");
        return this.mTransportManager.getCurrentTransportName();
    }

    @Override
    public String[] listAllTransports() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransports");
        return this.mTransportManager.getRegisteredTransportNames();
    }

    @Override
    public ComponentName[] listAllTransportComponents() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransportComponents");
        return this.mTransportManager.getRegisteredTransportComponents();
    }

    @Override
    public String[] getTransportWhitelist() {
        Set<ComponentName> transportWhitelist = this.mTransportManager.getTransportWhitelist();
        String[] strArr = new String[transportWhitelist.size()];
        Iterator<ComponentName> it = transportWhitelist.iterator();
        int i = 0;
        while (it.hasNext()) {
            strArr[i] = it.next().flattenToShortString();
            i++;
        }
        return strArr;
    }

    @Override
    public void updateTransportAttributes(ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) {
        updateTransportAttributes(Binder.getCallingUid(), componentName, str, intent, str2, intent2, str3);
    }

    @VisibleForTesting
    void updateTransportAttributes(int i, ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "updateTransportAttributes");
        Preconditions.checkNotNull(componentName, "transportComponent can't be null");
        Preconditions.checkNotNull(str, "name can't be null");
        Preconditions.checkNotNull(str2, "currentDestinationString can't be null");
        Preconditions.checkArgument((intent2 == null) == (str3 == null), "dataManagementLabel should be null iff dataManagementIntent is null");
        try {
            if (i != this.mContext.getPackageManager().getPackageUid(componentName.getPackageName(), 0)) {
                throw new SecurityException("Only the transport can change its description");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mTransportManager.updateTransportAttributes(componentName, str, intent, str2, intent2, str3);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Transport package not found", e);
        }
    }

    @Override
    @Deprecated
    public String selectBackupTransport(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransport");
        if (!isAllowedByMandatoryBackupTransportPolicy(str)) {
            Slog.w(TAG, "Failed to select transport - disallowed by device owner policy.");
            return this.mTransportManager.getCurrentTransportName();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String strSelectTransport = this.mTransportManager.selectTransport(str);
            updateStateForTransport(str);
            Slog.v(TAG, "selectBackupTransport(transport = " + str + "): previous transport = " + strSelectTransport);
            return strSelectTransport;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void selectBackupTransportAsync(final ComponentName componentName, final ISelectBackupTransportCallback iSelectBackupTransportCallback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransportAsync");
        if (!isAllowedByMandatoryBackupTransportPolicy(componentName)) {
            if (iSelectBackupTransportCallback != null) {
                try {
                    Slog.w(TAG, "Failed to select transport - disallowed by device owner policy.");
                    iSelectBackupTransportCallback.onFailure(-2001);
                    return;
                } catch (RemoteException e) {
                    Slog.e(TAG, "ISelectBackupTransportCallback listener not available");
                    return;
                }
            }
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Slog.v(TAG, "selectBackupTransportAsync(transport = " + componentName.flattenToShortString() + ")");
            this.mBackupHandler.post(new Runnable() {
                @Override
                public final void run() {
                    BackupManagerService.lambda$selectBackupTransportAsync$5(this.f$0, componentName, iSelectBackupTransportCallback);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static void lambda$selectBackupTransportAsync$5(BackupManagerService backupManagerService, ComponentName componentName, ISelectBackupTransportCallback iSelectBackupTransportCallback) {
        String transportName;
        int iRegisterAndSelectTransport = backupManagerService.mTransportManager.registerAndSelectTransport(componentName);
        String str = null;
        if (iRegisterAndSelectTransport == 0) {
            try {
                transportName = backupManagerService.mTransportManager.getTransportName(componentName);
            } catch (TransportNotRegisteredException e) {
            }
            try {
                backupManagerService.updateStateForTransport(transportName);
                str = transportName;
            } catch (TransportNotRegisteredException e2) {
                str = transportName;
                Slog.e(TAG, "Transport got unregistered");
                iRegisterAndSelectTransport = -1;
            }
        }
        if (iSelectBackupTransportCallback != null) {
            try {
                if (str != null) {
                    iSelectBackupTransportCallback.onSuccess(str);
                } else {
                    iSelectBackupTransportCallback.onFailure(iRegisterAndSelectTransport);
                }
            } catch (RemoteException e3) {
                Slog.e(TAG, "ISelectBackupTransportCallback listener not available");
            }
        }
    }

    private boolean isAllowedByMandatoryBackupTransportPolicy(String str) {
        ComponentName mandatoryBackupTransport = this.mBackupPolicyEnforcer.getMandatoryBackupTransport();
        if (mandatoryBackupTransport == null) {
            return true;
        }
        try {
            return TextUtils.equals(this.mTransportManager.getTransportName(mandatoryBackupTransport), str);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "mandatory backup transport not registered!");
            return false;
        }
    }

    private boolean isAllowedByMandatoryBackupTransportPolicy(ComponentName componentName) {
        ComponentName mandatoryBackupTransport = this.mBackupPolicyEnforcer.getMandatoryBackupTransport();
        if (mandatoryBackupTransport == null) {
            return true;
        }
        return mandatoryBackupTransport.equals(componentName);
    }

    private void updateStateForTransport(String str) {
        Settings.Secure.putString(this.mContext.getContentResolver(), "backup_transport", str);
        TransportClient transportClient = this.mTransportManager.getTransportClient(str, "BMS.updateStateForTransport()");
        if (transportClient != null) {
            try {
                this.mCurrentToken = transportClient.connectOrThrow("BMS.updateStateForTransport()").getCurrentRestoreSet();
            } catch (Exception e) {
                this.mCurrentToken = 0L;
                Slog.w(TAG, "Transport " + str + " not available: current token = 0");
            }
            this.mTransportManager.disposeOfTransportClient(transportClient, "BMS.updateStateForTransport()");
            return;
        }
        Slog.w(TAG, "Transport " + str + " not registered: current token = 0");
        this.mCurrentToken = 0L;
    }

    @Override
    public Intent getConfigurationIntent(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getConfigurationIntent");
        try {
            return this.mTransportManager.getTransportConfigurationIntent(str);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unable to get configuration intent from transport: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getDestinationString(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDestinationString");
        try {
            return this.mTransportManager.getTransportCurrentDestinationString(str);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unable to get destination string from transport: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getDataManagementIntent(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementIntent");
        try {
            return this.mTransportManager.getTransportDataManagementIntent(str);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unable to get management intent from transport: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getDataManagementLabel(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementLabel");
        try {
            return this.mTransportManager.getTransportDataManagementLabel(str);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unable to get management label from transport: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void agentConnected(String str, IBinder iBinder) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                Slog.d(TAG, "agentConnected pkg=" + str + " agent=" + iBinder);
                this.mConnectedAgent = IBackupAgent.Stub.asInterface(iBinder);
                this.mConnecting = false;
            } else {
                Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent connected");
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    @Override
    public void agentDisconnected(String str) {
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

    @Override
    public void restoreAtInstall(String str, int i) {
        boolean z;
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " attemping install-time restore");
            return;
        }
        long availableRestoreToken = getAvailableRestoreToken(str);
        Slog.v(TAG, "restoreAtInstall pkg=" + str + " token=" + Integer.toHexString(i) + " restoreSet=" + Long.toHexString(availableRestoreToken));
        boolean z2 = availableRestoreToken == 0;
        final TransportClient currentTransportClient = this.mTransportManager.getCurrentTransportClient("BMS.restoreAtInstall()");
        if (currentTransportClient == null) {
            Slog.w(TAG, "No transport client");
            z2 = true;
        }
        if (this.mAutoRestore) {
            z = z2;
        } else {
            Slog.w(TAG, "Non-restorable state: auto=" + this.mAutoRestore);
            z = true;
        }
        if (!z) {
            try {
                this.mWakelock.acquire();
                OnTaskFinishedListener onTaskFinishedListener = new OnTaskFinishedListener() {
                    @Override
                    public final void onFinished(String str2) {
                        BackupManagerService.lambda$restoreAtInstall$6(this.f$0, currentTransportClient, str2);
                    }
                };
                Message messageObtainMessage = this.mBackupHandler.obtainMessage(3);
                messageObtainMessage.obj = RestoreParams.createForRestoreAtInstall(currentTransportClient, null, null, availableRestoreToken, str, i, onTaskFinishedListener);
                this.mBackupHandler.sendMessage(messageObtainMessage);
            } catch (Exception e) {
                Slog.e(TAG, "Unable to contact transport: " + e.getMessage());
                z = true;
            }
        }
        if (z) {
            if (currentTransportClient != null) {
                this.mTransportManager.disposeOfTransportClient(currentTransportClient, "BMS.restoreAtInstall()");
            }
            Slog.v(TAG, "Finishing install immediately");
            try {
                this.mPackageManagerBinder.finishPackageInstall(i, false);
            } catch (RemoteException e2) {
            }
        }
    }

    public static void lambda$restoreAtInstall$6(BackupManagerService backupManagerService, TransportClient transportClient, String str) {
        backupManagerService.mTransportManager.disposeOfTransportClient(transportClient, str);
        backupManagerService.mWakelock.release();
    }

    @Override
    public IRestoreSession beginRestoreSession(String str, String str2) {
        Slog.v(TAG, "beginRestoreSession: pkg=" + str + " transport=" + str2);
        boolean z = true;
        if (str2 == null) {
            str2 = this.mTransportManager.getCurrentTransportName();
            if (str != null) {
                try {
                    if (this.mPackageManager.getPackageInfo(str, 0).applicationInfo.uid == Binder.getCallingUid()) {
                        z = false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(TAG, "Asked to restore nonexistent pkg " + str);
                    throw new IllegalArgumentException("Package " + str + " not found");
                }
            }
        }
        if (z) {
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
            this.mActiveRestoreSession = new ActiveRestoreSession(this, str, str2);
            this.mBackupHandler.sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
            return this.mActiveRestoreSession;
        }
    }

    public void clearRestoreSession(ActiveRestoreSession activeRestoreSession) {
        synchronized (this) {
            if (activeRestoreSession != this.mActiveRestoreSession) {
                Slog.e(TAG, "ending non-current restore session");
            } else {
                Slog.v(TAG, "Clearing restore session and halting timeout");
                this.mActiveRestoreSession = null;
                this.mBackupHandler.removeMessages(8);
            }
        }
    }

    @Override
    public void opComplete(int i, long j) {
        Operation operation;
        synchronized (this.mCurrentOpLock) {
            Operation operation2 = this.mCurrentOperations.get(i);
            operation = null;
            if (operation2 != null) {
                if (operation2.state == -1) {
                    this.mCurrentOperations.delete(i);
                } else if (operation2.state == 1) {
                    Slog.w(TAG, "Received duplicate ack for token=" + Integer.toHexString(i));
                    this.mCurrentOperations.remove(i);
                } else {
                    if (operation2.state == 0) {
                        operation2.state = 1;
                    }
                    operation = operation2;
                }
                this.mCurrentOpLock.notifyAll();
            } else {
                operation = operation2;
                this.mCurrentOpLock.notifyAll();
            }
        }
        if (operation != null && operation.callback != null) {
            this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(21, Pair.create(operation.callback, Long.valueOf(j))));
        }
    }

    @Override
    public boolean isAppEligibleForBackup(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isAppEligibleForBackup");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            TransportClient currentTransportClient = this.mTransportManager.getCurrentTransportClient("BMS.isAppEligibleForBackup");
            boolean zAppIsRunningAndEligibleForBackupWithTransport = AppBackupUtils.appIsRunningAndEligibleForBackupWithTransport(currentTransportClient, str, this.mPackageManager);
            if (currentTransportClient != null) {
                this.mTransportManager.disposeOfTransportClient(currentTransportClient, "BMS.isAppEligibleForBackup");
            }
            return zAppIsRunningAndEligibleForBackupWithTransport;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public String[] filterAppsEligibleForBackup(String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "filterAppsEligibleForBackup");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            TransportClient currentTransportClient = this.mTransportManager.getCurrentTransportClient("BMS.filterAppsEligibleForBackup");
            LinkedList linkedList = new LinkedList();
            for (String str : strArr) {
                if (AppBackupUtils.appIsRunningAndEligibleForBackupWithTransport(currentTransportClient, str, this.mPackageManager)) {
                    linkedList.add(str);
                }
            }
            if (currentTransportClient != null) {
                this.mTransportManager.disposeOfTransportClient(currentTransportClient, "BMS.filterAppsEligibleForBackup");
            }
            return (String[]) linkedList.toArray(new String[linkedList.size()]);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if (strArr != null) {
                try {
                    for (String str : strArr) {
                        if ("-h".equals(str)) {
                            printWriter.println("'dumpsys backup' optional arguments:");
                            printWriter.println("  -h       : this help text");
                            printWriter.println("  a[gents] : dump information about defined backup agents");
                            return;
                        } else if ("agents".startsWith(str)) {
                            dumpAgents(printWriter);
                            return;
                        } else if ("transportclients".equals(str.toLowerCase())) {
                            this.mTransportManager.dumpTransportClients(printWriter);
                            return;
                        } else {
                            if ("transportstats".equals(str.toLowerCase())) {
                                this.mTransportManager.dumpTransportStats(printWriter);
                                return;
                            }
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            dumpInternal(printWriter);
        }
    }

    private void dumpAgents(PrintWriter printWriter) {
        List<PackageInfo> listAllAgentPackages = allAgentPackages();
        printWriter.println("Defined backup agents:");
        for (PackageInfo packageInfo : listAllAgentPackages) {
            printWriter.print("  ");
            printWriter.print(packageInfo.packageName);
            printWriter.println(':');
            printWriter.print("      ");
            printWriter.println(packageInfo.applicationInfo.backupAgentName);
        }
    }

    private void dumpInternal(PrintWriter printWriter) {
        Iterator<String> it;
        synchronized (this.mQueueLock) {
            StringBuilder sb = new StringBuilder();
            sb.append("Backup Manager is ");
            sb.append(this.mEnabled ? "enabled" : "disabled");
            sb.append(" / ");
            sb.append(!this.mProvisioned ? "not " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb.append("provisioned / ");
            sb.append(this.mPendingInits.size() == 0 ? "not " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb.append("pending init");
            printWriter.println(sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Auto-restore is ");
            sb2.append(this.mAutoRestore ? "enabled" : "disabled");
            printWriter.println(sb2.toString());
            if (this.mBackupRunning) {
                printWriter.println("Backup currently running");
            }
            printWriter.println(isBackupOperationInProgress() ? "Backup in progress" : "No backups running");
            printWriter.println("Last backup pass started: " + this.mLastBackupPass + " (now = " + System.currentTimeMillis() + ')');
            StringBuilder sb3 = new StringBuilder();
            sb3.append("  next scheduled: ");
            sb3.append(KeyValueBackupJob.nextScheduled());
            printWriter.println(sb3.toString());
            printWriter.println("Transport whitelist:");
            for (ComponentName componentName : this.mTransportManager.getTransportWhitelist()) {
                printWriter.print("    ");
                printWriter.println(componentName.flattenToShortString());
            }
            printWriter.println("Available transports:");
            String[] strArrListAllTransports = listAllTransports();
            if (strArrListAllTransports != null) {
                for (String str : strArrListAllTransports) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(str.equals(this.mTransportManager.getCurrentTransportName()) ? "  * " : "    ");
                    sb4.append(str);
                    printWriter.println(sb4.toString());
                    try {
                        File file = new File(this.mBaseStateDir, this.mTransportManager.getTransportDirName(str));
                        printWriter.println("       destination: " + this.mTransportManager.getTransportCurrentDestinationString(str));
                        printWriter.println("       intent: " + this.mTransportManager.getTransportConfigurationIntent(str));
                        File[] fileArrListFiles = file.listFiles();
                        int length = fileArrListFiles.length;
                        for (int i = 0; i < length; i++) {
                            File file2 = fileArrListFiles[i];
                            printWriter.println("       " + file2.getName() + " - " + file2.length() + " state bytes");
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Error in transport", e);
                        printWriter.println("        Error: " + e);
                    }
                }
                this.mTransportManager.dumpTransportClients(printWriter);
                printWriter.println("Pending init: " + this.mPendingInits.size());
                it = this.mPendingInits.iterator();
                while (it.hasNext()) {
                    printWriter.println("    " + it.next());
                }
                synchronized (this.mBackupTrace) {
                    if (!this.mBackupTrace.isEmpty()) {
                        printWriter.println("Most recent backup trace:");
                        Iterator<String> it2 = this.mBackupTrace.iterator();
                        while (it2.hasNext()) {
                            printWriter.println("   " + it2.next());
                        }
                    }
                }
                printWriter.print("Ancestral: ");
                printWriter.println(Long.toHexString(this.mAncestralToken));
                printWriter.print("Current:   ");
                printWriter.println(Long.toHexString(this.mCurrentToken));
                int size = this.mBackupParticipants.size();
                printWriter.println("Participants:");
                for (int i2 = 0; i2 < size; i2++) {
                    int iKeyAt = this.mBackupParticipants.keyAt(i2);
                    printWriter.print("  uid: ");
                    printWriter.println(iKeyAt);
                    Iterator<String> it3 = this.mBackupParticipants.valueAt(i2).iterator();
                    while (it3.hasNext()) {
                        printWriter.println("    " + it3.next());
                    }
                }
                StringBuilder sb5 = new StringBuilder();
                sb5.append("Ancestral packages: ");
                sb5.append(this.mAncestralPackages == null ? "none" : Integer.valueOf(this.mAncestralPackages.size()));
                printWriter.println(sb5.toString());
                if (this.mAncestralPackages != null) {
                    Iterator<String> it4 = this.mAncestralPackages.iterator();
                    while (it4.hasNext()) {
                        printWriter.println("    " + it4.next());
                    }
                }
                Set<String> packagesCopy = this.mProcessedPackagesJournal.getPackagesCopy();
                printWriter.println("Ever backed up: " + packagesCopy.size());
                Iterator<String> it5 = packagesCopy.iterator();
                while (it5.hasNext()) {
                    printWriter.println("    " + it5.next());
                }
                printWriter.println("Pending key/value backup: " + this.mPendingBackups.size());
                Iterator<BackupRequest> it6 = this.mPendingBackups.values().iterator();
                while (it6.hasNext()) {
                    printWriter.println("    " + it6.next());
                }
                printWriter.println("Full backup queue:" + this.mFullBackupQueue.size());
                for (FullBackupEntry fullBackupEntry : this.mFullBackupQueue) {
                    printWriter.print("    ");
                    printWriter.print(fullBackupEntry.lastBackup);
                    printWriter.print(" : ");
                    printWriter.println(fullBackupEntry.packageName);
                }
            } else {
                this.mTransportManager.dumpTransportClients(printWriter);
                printWriter.println("Pending init: " + this.mPendingInits.size());
                it = this.mPendingInits.iterator();
                while (it.hasNext()) {
                }
                synchronized (this.mBackupTrace) {
                }
            }
        }
    }

    @Override
    public IBackupManager getBackupManagerBinder() {
        return this.mBackupManagerBinder;
    }
}
