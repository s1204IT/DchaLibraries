package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.ResourcesManager;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.SecurityLog;
import android.app.backup.IBackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AppsQueryHelper;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.ComponentInfo;
import android.content.pm.FallbackCategoryProvider;
import android.content.pm.FeatureInfo;
import android.content.pm.IDexModuleRegisterCallback;
import android.content.pm.IOnPermissionsChangeListener;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManagerNative;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstantAppInfo;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageList;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.PackageStats;
import android.content.pm.PackageUserState;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.SELinuxUtil;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.VerifierInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.IArtManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.security.KeyStore;
import android.security.SystemKeyStore;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.ByteStringUtils;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.LongSparseArray;
import android.util.LongSparseLongArray;
import android.util.MathUtils;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.PrintStreamPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimingsTraceLog;
import android.util.Xml;
import android.util.jar.StrictJarFile;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.IParcelFileDescriptorFactory;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.BatteryService;
import com.android.server.DeviceIdleController;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.CompilerStats;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.ParallelPackageParser;
import com.android.server.pm.Settings;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.dex.DexLogger;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.permission.BasePermission;
import com.android.server.pm.permission.DefaultPermissionGrantPolicy;
import com.android.server.pm.permission.PermissionManagerInternal;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.security.VerityUtils;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.usage.AppStandbyController;
import com.android.server.usage.UnixCalendar;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.server.MtkSystemServer;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.PmsExt;
import com.mediatek.server.powerhal.PowerHalManager;
import dalvik.system.CloseGuard;
import dalvik.system.VMRuntime;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageManagerService extends IPackageManager.Stub implements PackageSender {
    private static final String ATTR_IS_GRANTED = "g";
    private static final String ATTR_PACKAGE_NAME = "pkg";
    private static final String ATTR_PERMISSION_NAME = "name";
    private static final String ATTR_REVOKE_ON_UPGRADE = "rou";
    private static final String ATTR_USER_FIXED = "fixed";
    private static final String ATTR_USER_SET = "set";
    private static final int BLUETOOTH_UID = 1002;
    static final int BROADCAST_DELAY = 10000;
    static final int CHECK_PENDING_VERIFICATION = 16;
    static final boolean CLEAR_RUNTIME_PERMISSIONS_ON_UPGRADE = false;
    public static final String COMPRESSED_EXTENSION = ".gz";
    static final boolean DEBUG_SD_INSTALL = false;
    private static final long DEFAULT_MANDATORY_FSTRIM_INTERVAL = 259200000;
    private static final boolean DEFAULT_PACKAGE_PARSER_CACHE_ENABLED = true;
    private static final long DEFAULT_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD = 7200000;
    private static final int DEFAULT_VERIFICATION_RESPONSE = 1;
    private static final long DEFAULT_VERIFICATION_TIMEOUT = 10000;
    private static final boolean DEFAULT_VERIFY_ENABLE = true;
    static final int DEF_CONTAINER_BIND = 21;
    static final int END_COPY = 4;
    static final int FIND_INSTALL_LOC = 8;
    private static final boolean HIDE_EPHEMERAL_APIS = false;
    static final int INIT_COPY = 5;
    private static final String INSTALL_PACKAGE_SUFFIX = "-";
    private static final String[] INSTANT_APP_BROADCAST_PERMISSION;
    static final int INSTANT_APP_RESOLUTION_PHASE_TWO = 20;
    static final int INTENT_FILTER_VERIFIED = 18;
    private static final String KILL_APP_REASON_GIDS_CHANGED = "permission grant or revoke changed gids";
    private static final String KILL_APP_REASON_PERMISSIONS_REVOKED = "permissions revoked";
    private static final int LOG_UID = 1007;
    static final int MCS_BOUND = 3;
    static final int MCS_CHECK_SERVICE_CONNECT = 22;
    static final int MCS_GIVE_UP = 11;
    static final int MCS_RECONNECT = 10;
    static final int MCS_UNBIND = 6;
    private static final int NFC_UID = 1027;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String PACKAGE_PARSER_CACHE_VERSION = "1";
    private static final String PACKAGE_SCHEME = "package";
    static final int PACKAGE_VERIFIED = 15;
    public static final String PLATFORM_PACKAGE_NAME = "android";
    static final int POST_INSTALL = 9;
    private static final String PRODUCT_OVERLAY_DIR = "/product/overlay";
    private static final Set<String> PROTECTED_ACTIONS;
    private static final int RADIO_UID = 1001;
    public static final int REASON_AB_OTA = 4;
    public static final int REASON_BACKGROUND_DEXOPT = 3;
    public static final int REASON_BOOT = 1;
    public static final int REASON_FIRST_BOOT = 0;
    public static final int REASON_INACTIVE_PACKAGE_DOWNGRADE = 5;
    public static final int REASON_INSTALL = 2;
    public static final int REASON_LAST = 6;
    public static final int REASON_SHARED = 6;
    public static final int REASON_UNKNOWN = -1;
    static final int SCAN_AS_FULL_APP = 32768;
    static final int SCAN_AS_INSTANT_APP = 16384;
    static final int SCAN_AS_OEM = 524288;
    static final int SCAN_AS_PRIVILEGED = 262144;
    static final int SCAN_AS_PRODUCT = 2097152;
    static final int SCAN_AS_SYSTEM = 131072;
    static final int SCAN_AS_VENDOR = 1048576;
    static final int SCAN_AS_VIRTUAL_PRELOAD = 65536;
    static final int SCAN_BOOTING = 16;
    static final int SCAN_CHECK_ONLY = 1024;
    static final int SCAN_DELETE_DATA_ON_FAILURES = 64;
    static final int SCAN_DONT_KILL_APP = 2048;
    static final int SCAN_FIRST_BOOT_OR_UPGRADE = 8192;
    static final int SCAN_IGNORE_FROZEN = 4096;
    static final int SCAN_INITIAL = 512;
    static final int SCAN_MOVE = 256;
    static final int SCAN_NEW_INSTALL = 4;
    static final int SCAN_NO_DEX = 1;
    static final int SCAN_REQUIRE_KNOWN = 128;
    static final int SCAN_UPDATE_SIGNATURE = 2;
    static final int SCAN_UPDATE_TIME = 8;
    private static final String SD_ENCRYPTION_ALGORITHM = "AES";
    private static final String SD_ENCRYPTION_KEYSTORE_NAME = "AppsOnSD";
    static final int SEND_PENDING_BROADCAST = 1;
    private static final int SE_UID = 1068;
    private static final int SHELL_UID = 2000;
    static final int START_CLEANING_PACKAGE = 7;
    static final int START_INTENT_FILTER_VERIFICATIONS = 17;
    private static final String STATIC_SHARED_LIB_DELIMITER = "_";
    public static final String STUB_SUFFIX = "-Stub";
    private static final int SYSTEM_RUNTIME_GRANT_MASK = 52;
    static final String TAG = "PackageManager";
    private static final String TAG_ALL_GRANTS = "rt-grants";
    private static final String TAG_DEFAULT_APPS = "da";
    private static final String TAG_GRANT = "grant";
    private static final String TAG_INTENT_FILTER_VERIFICATION = "iv";
    private static final String TAG_PERMISSION = "perm";
    private static final String TAG_PERMISSION_BACKUP = "perm-grant-backup";
    private static final String TAG_PREFERRED_BACKUP = "pa";
    private static final int TYPE_ACTIVITY = 1;
    private static final int TYPE_PROVIDER = 4;
    private static final int TYPE_RECEIVER = 2;
    private static final int TYPE_SERVICE = 3;
    private static final int TYPE_UNKNOWN = 0;
    private static final int USER_RUNTIME_GRANT_MASK = 11;
    private static final String VENDOR_OVERLAY_DIR = "/vendor/overlay";
    static final long WATCHDOG_TIMEOUT = 600000;
    static final int WRITE_PACKAGE_LIST = 19;
    static final int WRITE_PACKAGE_RESTRICTIONS = 14;
    static final int WRITE_SETTINGS = 13;
    static final int WRITE_SETTINGS_DELAY = 10000;
    private static final Comparator<ProviderInfo> mProviderInitOrderSorter;
    private static final Comparator<ResolveInfo> mResolvePrioritySorter;
    private static final File sAppInstallDir;
    private static final File sAppLib32InstallDir;
    private static final CtaManager sCtaManager;
    private static final File sDrmAppPrivateInstallDir;
    private static MtkSystemServer sMtkSystemServerIns;
    private static PmsExt sPmsExt;
    static UserManagerService sUserManager;
    private ActivityManagerInternal mActivityManagerInternal;
    ApplicationInfo mAndroidApplication;
    final ArtManagerService mArtManagerService;

    @GuardedBy("mAvailableFeatures")
    final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    private File mCacheDir;

    @GuardedBy("mPackages")
    int mChangedPackagesSequenceNumber;
    final Context mContext;
    ComponentName mCustomResolverComponentName;
    final int mDefParseFlags;
    final DefaultPermissionGrantPolicy mDefaultPermissionPolicy;
    private boolean mDeferProtectedFilters;
    private DeviceIdleController.LocalService mDeviceIdleController;
    private final DexManager mDexManager;

    @GuardedBy("mPackages")
    private boolean mDexOptDialogShown;
    PackageManagerInternal.ExternalSourcesPolicy mExternalSourcesPolicy;
    final boolean mFactoryTest;
    boolean mFirstBoot;
    final PackageHandler mHandler;
    final ServiceThread mHandlerThread;
    volatile boolean mHasSystemUidErrors;

    @GuardedBy("mInstallLock")
    final Installer mInstaller;
    final PackageInstallerService mInstallerService;
    ActivityInfo mInstantAppInstallerActivity;
    private final InstantAppRegistry mInstantAppRegistry;
    final InstantAppResolverConnection mInstantAppResolverConnection;
    final ComponentName mInstantAppResolverSettingsComponent;
    private final IntentFilterVerifier<PackageParser.ActivityIntentInfo> mIntentFilterVerifier;
    private final ComponentName mIntentFilterVerifierComponent;
    final boolean mIsPreNMR1Upgrade;
    final boolean mIsPreNUpgrade;
    final boolean mIsUpgrade;
    private List<String> mKeepUninstalledPackages;
    final DisplayMetrics mMetrics;
    private final MoveCallbacks mMoveCallbacks;
    private final OnPermissionChangeListeners mOnPermissionChangeListeners;
    final boolean mOnlyCore;
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final PermissionManagerInternal mPermissionManager;
    PackageParser.Package mPlatformPackage;
    private ArrayList<Message> mPostSystemReadyMessages;
    private Future<?> mPrepareAppDataFuture;
    private final ProcessLoggingHandler mProcessLoggingHandler;
    boolean mPromoteSystemApps;
    final ProtectedPackages mProtectedPackages;
    final ProviderIntentResolver mProviders;
    final String mRequiredInstallerPackage;
    final String mRequiredUninstallerPackage;
    final String mRequiredVerifierPackage;
    ComponentName mResolveComponentName;
    volatile boolean mSafeMode;
    final String[] mSeparateProcesses;
    final ServiceIntentResolver mServices;
    final String mServicesSystemSharedLibraryPackageName;

    @GuardedBy("mPackages")
    final Settings mSettings;
    final String mSetupWizardPackage;
    final String mSharedSystemSharedLibraryPackageName;
    final String mStorageManagerPackage;
    volatile boolean mSystemReady;
    final String mSystemTextClassifierPackage;
    private UserManagerInternal mUserManagerInternal;
    private volatile boolean mWebInstantAppsDisabled;
    public static boolean DEBUG_SETTINGS = false;
    public static boolean DEBUG_PREFERRED = false;
    public static boolean DEBUG_UPGRADE = false;
    public static boolean DEBUG_DOMAIN_VERIFICATION = false;
    public static boolean DEBUG_BACKUP = false;
    public static boolean DEBUG_INSTALL = false;
    public static boolean DEBUG_REMOVE = false;
    public static boolean DEBUG_BROADCASTS = false;
    public static boolean DEBUG_SHOW_INFO = false;
    public static boolean DEBUG_PACKAGE_INFO = false;
    public static boolean DEBUG_INTENT_MATCHING = false;
    public static boolean DEBUG_PACKAGE_SCANNING = false;
    public static boolean DEBUG_VERIFY = false;
    public static boolean DEBUG_FILTERS = false;
    public static boolean DEBUG_PERMISSIONS = false;
    public static boolean DEBUG_SHARED_LIBRARIES = false;
    public static final boolean DEBUG_COMPRESSION = Build.IS_DEBUGGABLE;
    public static boolean DEBUG_DEXOPT = false;
    public static boolean DEBUG_ABI_SELECTION = false;
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    public static boolean DEBUG_TRIAGED_MISSING = false;
    public static boolean DEBUG_APP_DATA = false;
    private static final boolean ENABLE_FREE_CACHE_V2 = SystemProperties.getBoolean("fw.free_cache_v2", true);
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    public static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    private static final Intent sBrowserIntent = new Intent();
    final int mSdkVersion = Build.VERSION.SDK_INT;

    @GuardedBy("mPackages")
    boolean mDefaultContainerWhitelisted = false;
    final Object mInstallLock = new Object();

    @GuardedBy("mPackages")
    final ArrayMap<String, PackageParser.Package> mPackages = new ArrayMap<>();
    final ArrayMap<String, Set<String>> mKnownCodebase = new ArrayMap<>();

    @GuardedBy("mPackages")
    final SparseIntArray mIsolatedOwners = new SparseIntArray();
    private final ArrayMap<String, File> mExpectingBetter = new ArrayMap<>();
    private final List<PackageParser.ActivityIntentInfo> mProtectedFilters = new ArrayList();
    private final ArraySet<String> mExistingSystemPackages = new ArraySet<>();

    @GuardedBy("mPackages")
    final ArraySet<String> mFrozenPackages = new ArraySet<>();

    @GuardedBy("mLoadedVolumes")
    final ArraySet<String> mLoadedVolumes = new ArraySet<>();

    @GuardedBy("mPackages")
    final SparseArray<SparseArray<String>> mChangedPackages = new SparseArray<>();

    @GuardedBy("mPackages")
    final SparseArray<Map<String, Integer>> mChangedPackagesSequenceNumbers = new SparseArray<>();

    @GuardedBy("mPackages")
    private final ArraySet<PackageManagerInternal.PackageListObserver> mPackageListObservers = new ArraySet<>();
    final PackageParser.Callback mPackageParserCallback = new PackageParserCallback();
    final ParallelPackageParserCallback mParallelPackageParserCallback = new ParallelPackageParserCallback();
    final ArrayMap<String, LongSparseArray<SharedLibraryEntry>> mSharedLibraries = new ArrayMap<>();
    final ArrayMap<String, LongSparseArray<SharedLibraryEntry>> mStaticLibsByDeclaringPackage = new ArrayMap<>();
    final ActivityIntentResolver mActivities = new ActivityIntentResolver();
    final ActivityIntentResolver mReceivers = new ActivityIntentResolver();
    final ArrayMap<String, PackageParser.Provider> mProvidersByAuthority = new ArrayMap<>();
    final ArrayMap<ComponentName, PackageParser.Instrumentation> mInstrumentation = new ArrayMap<>();
    final ArraySet<String> mTransferedPackages = new ArraySet<>();

    @GuardedBy("mProtectedBroadcasts")
    final ArraySet<String> mProtectedBroadcasts = new ArraySet<>();
    final SparseArray<PackageVerificationState> mPendingVerification = new SparseArray<>();
    private AtomicInteger mNextMoveId = new AtomicInteger();
    SparseBooleanArray mUserNeedsBadging = new SparseBooleanArray();
    private int mPendingVerificationToken = 0;
    final ActivityInfo mResolveActivity = new ActivityInfo();
    final ResolveInfo mResolveInfo = new ResolveInfo();
    boolean mResolverReplaced = false;
    private int mIntentFilterVerificationToken = 0;
    final ResolveInfo mInstantAppInstallerInfo = new ResolveInfo();
    final SparseArray<IntentFilterVerificationState> mIntentFilterVerificationStates = new SparseArray<>();
    final PendingPackageBroadcasts mPendingBroadcasts = new PendingPackageBroadcasts();
    private IMediaContainerService mContainerService = null;
    private ArraySet<Integer> mDirtyUsers = new ArraySet<>();
    private boolean mDefContainerConnected = false;
    private int mCheckServiceConnectedCount = 0;
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    final SparseArray<PostInstallData> mRunningInstalls = new SparseArray<>();
    int mNextInstallToken = 1;
    private final PackageUsage mPackageUsage = new PackageUsage();
    private final CompilerStats mCompilerStats = new CompilerStats();
    private PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private PermissionManagerInternal.PermissionCallback mPermissionCallback = new PermissionManagerInternal.PermissionCallback() {
        @Override
        public void onGidsChanged(final int i, final int i2) {
            PackageManagerService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PackageManagerService.this.killUid(i, i2, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                }
            });
        }

        @Override
        public void onPermissionGranted(int i, int i2) {
            PackageManagerService.this.mOnPermissionChangeListeners.onPermissionsChanged(i);
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.writeRuntimePermissionsForUserLPr(i2, false);
            }
        }

        @Override
        public void onInstallPermissionGranted() {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.scheduleWriteSettingsLocked();
            }
        }

        @Override
        public void onPermissionRevoked(int i, int i2) {
            PackageManagerService.this.mOnPermissionChangeListeners.onPermissionsChanged(i);
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.writeRuntimePermissionsForUserLPr(i2, true);
            }
            PackageManagerService.this.killUid(UserHandle.getAppId(i), i2, PackageManagerService.KILL_APP_REASON_PERMISSIONS_REVOKED);
        }

        @Override
        public void onInstallPermissionRevoked() {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.scheduleWriteSettingsLocked();
            }
        }

        @Override
        public void onPermissionUpdated(int[] iArr, boolean z) {
            synchronized (PackageManagerService.this.mPackages) {
                for (int i : iArr) {
                    PackageManagerService.this.mSettings.writeRuntimePermissionsForUserLPr(i, z);
                }
            }
        }

        @Override
        public void onInstallPermissionUpdated() {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.scheduleWriteSettingsLocked();
            }
        }

        @Override
        public void onPermissionRemoved() {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.writeLPr();
            }
        }
    };
    private StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            if (volumeInfo.type == 1) {
                if (volumeInfo.state != 2) {
                    if (volumeInfo.state == 5) {
                        PackageManagerService.this.unloadPrivatePackages(volumeInfo);
                    }
                } else {
                    String fsUuid = volumeInfo.getFsUuid();
                    PackageManagerService.sUserManager.reconcileUsers(fsUuid);
                    PackageManagerService.this.reconcileApps(fsUuid);
                    PackageManagerService.this.mInstallerService.onPrivateVolumeMounted(fsUuid);
                    PackageManagerService.this.loadPrivatePackages(volumeInfo);
                }
            }
        }

        public void onVolumeForgotten(String str) {
            if (TextUtils.isEmpty(str)) {
                Slog.e(PackageManagerService.TAG, "Forgetting internal storage is probably a mistake; ignoring");
                return;
            }
            synchronized (PackageManagerService.this.mPackages) {
                for (PackageSetting packageSetting : PackageManagerService.this.mSettings.getVolumePackagesLPr(str)) {
                    Slog.d(PackageManagerService.TAG, "Destroying " + packageSetting.name + " because volume was forgotten");
                    PackageManagerService.this.deletePackageVersioned(new VersionedPackage(packageSetting.name, -1), new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), 0, 2);
                    AttributeCache.instance().removePackage(packageSetting.name);
                }
                PackageManagerService.this.mSettings.onVolumeForgotten(str);
                PackageManagerService.this.mSettings.writeLPr();
            }
        }
    };
    private boolean mMediaMounted = false;

    private interface BlobXmlRestorer {
        void apply(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ComponentType {
    }

    private interface IntentFilterVerifier<T extends IntentFilter> {
        boolean addOneIntentFilterVerification(int i, int i2, int i3, T t, String str);

        void receiveVerificationResponse(int i);

        void startVerifications(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanFlags {
    }

    static int access$4408(PackageManagerService packageManagerService) {
        int i = packageManagerService.mPendingVerificationToken;
        packageManagerService.mPendingVerificationToken = i + 1;
        return i;
    }

    static int access$608(PackageManagerService packageManagerService) {
        int i = packageManagerService.mCheckServiceConnectedCount;
        packageManagerService.mCheckServiceConnectedCount = i + 1;
        return i;
    }

    static {
        sBrowserIntent.setAction("android.intent.action.VIEW");
        sBrowserIntent.addCategory("android.intent.category.BROWSABLE");
        sBrowserIntent.setData(Uri.parse("http:"));
        sBrowserIntent.addFlags(512);
        PROTECTED_ACTIONS = new ArraySet();
        PROTECTED_ACTIONS.add("android.intent.action.SEND");
        PROTECTED_ACTIONS.add("android.intent.action.SENDTO");
        PROTECTED_ACTIONS.add("android.intent.action.SEND_MULTIPLE");
        PROTECTED_ACTIONS.add("android.intent.action.VIEW");
        INSTANT_APP_BROADCAST_PERMISSION = new String[]{"android.permission.ACCESS_INSTANT_APPS"};
        sAppInstallDir = new File(Environment.getDataDirectory(), "app");
        sAppLib32InstallDir = new File(Environment.getDataDirectory(), "app-lib");
        sDrmAppPrivateInstallDir = new File(Environment.getDataDirectory(), "app-private");
        sMtkSystemServerIns = MtkSystemServer.getInstance();
        sPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();
        mResolvePrioritySorter = new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo resolveInfo, ResolveInfo resolveInfo2) {
                int i = resolveInfo.priority;
                int i2 = resolveInfo2.priority;
                if (i != i2) {
                    return i > i2 ? -1 : 1;
                }
                int i3 = resolveInfo.preferredOrder;
                int i4 = resolveInfo2.preferredOrder;
                if (i3 != i4) {
                    return i3 > i4 ? -1 : 1;
                }
                if (resolveInfo.isDefault != resolveInfo2.isDefault) {
                    return resolveInfo.isDefault ? -1 : 1;
                }
                int i5 = resolveInfo.match;
                int i6 = resolveInfo2.match;
                if (i5 != i6) {
                    return i5 > i6 ? -1 : 1;
                }
                if (resolveInfo.system != resolveInfo2.system) {
                    return resolveInfo.system ? -1 : 1;
                }
                if (resolveInfo.activityInfo != null) {
                    return resolveInfo.activityInfo.packageName.compareTo(resolveInfo2.activityInfo.packageName);
                }
                if (resolveInfo.serviceInfo != null) {
                    return resolveInfo.serviceInfo.packageName.compareTo(resolveInfo2.serviceInfo.packageName);
                }
                if (resolveInfo.providerInfo != null) {
                    return resolveInfo.providerInfo.packageName.compareTo(resolveInfo2.providerInfo.packageName);
                }
                return 0;
            }
        };
        mProviderInitOrderSorter = new Comparator<ProviderInfo>() {
            @Override
            public int compare(ProviderInfo providerInfo, ProviderInfo providerInfo2) {
                int i = providerInfo.initOrder;
                int i2 = providerInfo2.initOrder;
                if (i > i2) {
                    return -1;
                }
                return i < i2 ? 1 : 0;
            }
        };
        sCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
    }

    class PackageParserCallback implements PackageParser.Callback {
        PackageParserCallback() {
        }

        public final boolean hasFeature(String str) {
            return PackageManagerService.this.hasSystemFeature(str, 0);
        }

        final List<PackageParser.Package> getStaticOverlayPackages(Collection<PackageParser.Package> collection, String str) {
            ArrayList arrayList = null;
            if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str)) {
                return null;
            }
            for (PackageParser.Package r0 : collection) {
                if (str.equals(r0.mOverlayTarget) && r0.mOverlayIsStatic) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(r0);
                }
            }
            if (arrayList != null) {
                Collections.sort(arrayList, new Comparator<PackageParser.Package>() {
                    @Override
                    public int compare(PackageParser.Package r1, PackageParser.Package r2) {
                        return r1.mOverlayPriority - r2.mOverlayPriority;
                    }
                });
            }
            return arrayList;
        }

        final String[] getStaticOverlayPaths(List<PackageParser.Package> list, String str) {
            if (list == null || list.isEmpty()) {
                return null;
            }
            ArrayList arrayList = null;
            for (PackageParser.Package r2 : list) {
                if (str == null) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(r2.baseCodePath);
                } else {
                    try {
                        PackageManagerService.this.mInstaller.idmap(str, r2.baseCodePath, UserHandle.getSharedAppGid(UserHandle.getUserGid(0)));
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(r2.baseCodePath);
                    } catch (Installer.InstallerException e) {
                        Slog.e(PackageManagerService.TAG, "Failed to generate idmap for " + str + " and " + r2.baseCodePath);
                    }
                }
            }
            if (arrayList == null) {
                return null;
            }
            return (String[]) arrayList.toArray(new String[0]);
        }

        String[] getStaticOverlayPaths(String str, String str2) {
            List<PackageParser.Package> staticOverlayPackages;
            String[] staticOverlayPaths;
            synchronized (PackageManagerService.this.mInstallLock) {
                synchronized (PackageManagerService.this.mPackages) {
                    staticOverlayPackages = getStaticOverlayPackages(PackageManagerService.this.mPackages.values(), str);
                }
                staticOverlayPaths = getStaticOverlayPaths(staticOverlayPackages, str2);
            }
            return staticOverlayPaths;
        }

        public final String[] getOverlayApks(String str) {
            return getStaticOverlayPaths(str, (String) null);
        }

        public final String[] getOverlayPaths(String str, String str2) {
            return getStaticOverlayPaths(str, str2);
        }
    }

    class ParallelPackageParserCallback extends PackageParserCallback {
        List<PackageParser.Package> mOverlayPackages;

        ParallelPackageParserCallback() {
            super();
            this.mOverlayPackages = null;
        }

        void findStaticOverlayPackages() {
            synchronized (PackageManagerService.this.mPackages) {
                for (PackageParser.Package r2 : PackageManagerService.this.mPackages.values()) {
                    if (r2.mOverlayIsStatic) {
                        if (this.mOverlayPackages == null) {
                            this.mOverlayPackages = new ArrayList();
                        }
                        this.mOverlayPackages.add(r2);
                    }
                }
            }
        }

        @Override
        synchronized String[] getStaticOverlayPaths(String str, String str2) {
            return this.mOverlayPackages == null ? null : getStaticOverlayPaths(getStaticOverlayPackages(this.mOverlayPackages, str), str2);
        }
    }

    public static final class SharedLibraryEntry {
        public final String apk;
        public final SharedLibraryInfo info;
        public final String path;

        SharedLibraryEntry(String str, String str2, String str3, long j, int i, String str4, long j2) {
            this.path = str;
            this.apk = str2;
            this.info = new SharedLibraryInfo(str3, j, i, new VersionedPackage(str4, j2), null);
        }
    }

    private static class IFVerificationParams {
        PackageParser.Package pkg;
        boolean replacing;
        int userId;
        int verifierUid;

        public IFVerificationParams(PackageParser.Package r1, boolean z, int i, int i2) {
            this.pkg = r1;
            this.replacing = z;
            this.userId = i;
            this.replacing = z;
            this.verifierUid = i2;
        }
    }

    private class IntentVerifierProxy implements IntentFilterVerifier<PackageParser.ActivityIntentInfo> {
        private Context mContext;
        private ArrayList<Integer> mCurrentIntentFilterVerifications = new ArrayList<>();
        private ComponentName mIntentFilterVerifierComponent;

        public IntentVerifierProxy(Context context, ComponentName componentName) {
            this.mContext = context;
            this.mIntentFilterVerifierComponent = componentName;
        }

        private String getDefaultScheme() {
            return "https";
        }

        @Override
        public void startVerifications(int i) {
            int size = this.mCurrentIntentFilterVerifications.size();
            for (int i2 = 0; i2 < size; i2++) {
                int iIntValue = this.mCurrentIntentFilterVerifications.get(i2).intValue();
                IntentFilterVerificationState intentFilterVerificationState = PackageManagerService.this.mIntentFilterVerificationStates.get(iIntValue);
                String packageName = intentFilterVerificationState.getPackageName();
                ArrayList<PackageParser.ActivityIntentInfo> filters = intentFilterVerificationState.getFilters();
                int size2 = filters.size();
                ArraySet<String> arraySet = new ArraySet<>();
                for (int i3 = 0; i3 < size2; i3++) {
                    arraySet.addAll(filters.get(i3).getHostsList());
                }
                synchronized (PackageManagerService.this.mPackages) {
                    if (PackageManagerService.this.mSettings.createIntentFilterVerificationIfNeededLPw(packageName, arraySet) != null) {
                        PackageManagerService.this.scheduleWriteSettingsLocked();
                    }
                }
                sendVerificationRequest(iIntValue, intentFilterVerificationState);
            }
            this.mCurrentIntentFilterVerifications.clear();
        }

        private void sendVerificationRequest(int i, IntentFilterVerificationState intentFilterVerificationState) {
            Intent intent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION");
            intent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_ID", i);
            intent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_URI_SCHEME", getDefaultScheme());
            intent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_HOSTS", intentFilterVerificationState.getHostsString());
            intent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_PACKAGE_NAME", intentFilterVerificationState.getPackageName());
            intent.setComponent(this.mIntentFilterVerifierComponent);
            intent.addFlags(268435456);
            long verificationTimeout = PackageManagerService.this.getVerificationTimeout();
            BroadcastOptions.makeBasic().setTemporaryAppWhitelistDuration(verificationTimeout);
            PackageManagerService.this.getDeviceIdleController().addPowerSaveTempWhitelistApp(Process.myUid(), this.mIntentFilterVerifierComponent.getPackageName(), verificationTimeout, 0, true, "intent filter verifier");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(PackageManagerService.TAG, "Sending IntentFilter verification broadcast");
            }
        }

        @Override
        public void receiveVerificationResponse(int i) {
            IntentFilterVerificationInfo intentFilterVerificationLPr;
            IntentFilterVerificationState intentFilterVerificationState = PackageManagerService.this.mIntentFilterVerificationStates.get(i);
            boolean zIsVerified = intentFilterVerificationState.isVerified();
            ArrayList<PackageParser.ActivityIntentInfo> filters = intentFilterVerificationState.getFilters();
            int size = filters.size();
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(PackageManagerService.TAG, "Received verification response " + i + " for " + size + " filters, verified=" + zIsVerified);
            }
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                PackageParser.ActivityIntentInfo activityIntentInfo = filters.get(i3);
                activityIntentInfo.setVerified(zIsVerified);
                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                    Slog.d(PackageManagerService.TAG, "IntentFilter " + activityIntentInfo.toString() + " verified with result:" + zIsVerified + " and hosts:" + intentFilterVerificationState.getHostsString());
                }
            }
            PackageManagerService.this.mIntentFilterVerificationStates.remove(i);
            String packageName = intentFilterVerificationState.getPackageName();
            synchronized (PackageManagerService.this.mPackages) {
                intentFilterVerificationLPr = PackageManagerService.this.mSettings.getIntentFilterVerificationLPr(packageName);
            }
            if (intentFilterVerificationLPr == null) {
                Slog.w(PackageManagerService.TAG, "IntentFilterVerificationInfo not found for verificationId:" + i + " packageName:" + packageName);
                return;
            }
            synchronized (PackageManagerService.this.mPackages) {
                boolean z = true;
                try {
                    if (zIsVerified) {
                        intentFilterVerificationLPr.setStatus(2);
                    } else {
                        intentFilterVerificationLPr.setStatus(1);
                    }
                    PackageManagerService.this.scheduleWriteSettingsLocked();
                    int userId = intentFilterVerificationState.getUserId();
                    if (userId != -1) {
                        int intentFilterVerificationStatusLPr = PackageManagerService.this.mSettings.getIntentFilterVerificationStatusLPr(packageName, userId);
                        switch (intentFilterVerificationStatusLPr) {
                            case 0:
                                if (zIsVerified) {
                                    i2 = 2;
                                }
                                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                    Slog.d(PackageManagerService.TAG, "Applying update; old=" + intentFilterVerificationStatusLPr + " new=" + i2);
                                }
                                break;
                            case 1:
                                if (zIsVerified) {
                                    i2 = 2;
                                } else {
                                    z = false;
                                }
                                break;
                            case 2:
                                if (!zIsVerified) {
                                    if (!SystemConfig.getInstance().getLinkedApps().contains(packageName)) {
                                        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                            Slog.d(PackageManagerService.TAG, "Formerly validated but now failing; demoting");
                                        }
                                    } else {
                                        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                                            Slog.d(PackageManagerService.TAG, "Updating bundled package " + packageName + " failed autoVerify, but sysconfig supersedes");
                                        }
                                        z = false;
                                    }
                                    break;
                                }
                                break;
                            default:
                                z = false;
                                break;
                        }
                        if (z) {
                            PackageManagerService.this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, i2, userId);
                            PackageManagerService.this.scheduleWritePackageRestrictionsLocked(userId);
                        }
                    } else {
                        Slog.i(PackageManagerService.TAG, "autoVerify ignored when installing for all users");
                    }
                } finally {
                }
            }
        }

        @Override
        public boolean addOneIntentFilterVerification(int i, int i2, int i3, PackageParser.ActivityIntentInfo activityIntentInfo, String str) {
            if (!PackageManagerService.hasValidDomains(activityIntentInfo)) {
                return false;
            }
            IntentFilterVerificationState intentFilterVerificationStateCreateDomainVerificationState = PackageManagerService.this.mIntentFilterVerificationStates.get(i3);
            if (intentFilterVerificationStateCreateDomainVerificationState == null) {
                intentFilterVerificationStateCreateDomainVerificationState = createDomainVerificationState(i, i2, i3, str);
            }
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(PackageManagerService.TAG, "Adding verification filter for " + str + ": " + activityIntentInfo);
            }
            intentFilterVerificationStateCreateDomainVerificationState.addFilter(activityIntentInfo);
            return true;
        }

        private IntentFilterVerificationState createDomainVerificationState(int i, int i2, int i3, String str) {
            IntentFilterVerificationState intentFilterVerificationState = new IntentFilterVerificationState(i, i2, str);
            intentFilterVerificationState.setPendingState();
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIntentFilterVerificationStates.append(i3, intentFilterVerificationState);
                this.mCurrentIntentFilterVerifications.add(Integer.valueOf(i3));
            }
            return intentFilterVerificationState;
        }
    }

    private static boolean hasValidDomains(PackageParser.ActivityIntentInfo activityIntentInfo) {
        return activityIntentInfo.hasCategory("android.intent.category.BROWSABLE") && (activityIntentInfo.hasDataScheme("http") || activityIntentInfo.hasDataScheme("https"));
    }

    static class PendingPackageBroadcasts {
        final SparseArray<ArrayMap<String, ArrayList<String>>> mUidMap = new SparseArray<>(2);

        public ArrayList<String> get(int i, String str) {
            return getOrAllocate(i).get(str);
        }

        public void put(int i, String str, ArrayList<String> arrayList) {
            getOrAllocate(i).put(str, arrayList);
        }

        public void remove(int i, String str) {
            ArrayMap<String, ArrayList<String>> arrayMap = this.mUidMap.get(i);
            if (arrayMap != null) {
                arrayMap.remove(str);
            }
        }

        public void remove(int i) {
            this.mUidMap.remove(i);
        }

        public int userIdCount() {
            return this.mUidMap.size();
        }

        public int userIdAt(int i) {
            return this.mUidMap.keyAt(i);
        }

        public ArrayMap<String, ArrayList<String>> packagesForUserId(int i) {
            return this.mUidMap.get(i);
        }

        public int size() {
            int size = 0;
            for (int i = 0; i < this.mUidMap.size(); i++) {
                size += this.mUidMap.valueAt(i).size();
            }
            return size;
        }

        public void clear() {
            this.mUidMap.clear();
        }

        private ArrayMap<String, ArrayList<String>> getOrAllocate(int i) {
            ArrayMap<String, ArrayList<String>> arrayMap = this.mUidMap.get(i);
            if (arrayMap == null) {
                ArrayMap<String, ArrayList<String>> arrayMap2 = new ArrayMap<>();
                this.mUidMap.put(i, arrayMap2);
                return arrayMap2;
            }
            return arrayMap;
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PackageManagerService.this.mDefContainerConnected = true;
            PackageManagerService.this.mCheckServiceConnectedCount = 0;
            PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(3, IMediaContainerService.Stub.asInterface(Binder.allowBlocking(iBinder))));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            PackageManagerService.this.mDefContainerConnected = false;
        }
    }

    static class PostInstallData {
        public InstallArgs args;
        public PackageInstalledInfo res;

        PostInstallData(InstallArgs installArgs, PackageInstalledInfo packageInstalledInfo) {
            this.args = installArgs;
            this.res = packageInstalledInfo;
        }
    }

    class PackageHandler extends Handler {
        private boolean mBound;
        final ArrayList<HandlerParams> mPendingInstalls;

        private boolean connectToService() {
            if (PackageManagerService.DEBUG_INSTALL) {
                Log.i(PackageManagerService.TAG, "Trying to bind to DefaultContainerService");
            }
            Intent component = new Intent().setComponent(PackageManagerService.DEFAULT_CONTAINER_COMPONENT);
            Process.setThreadPriority(0);
            if (PackageManagerService.this.mContext.bindServiceAsUser(component, PackageManagerService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                Process.setThreadPriority(10);
                this.mBound = true;
                PackageManagerService.this.mHandler.sendMessageDelayed(PackageManagerService.this.mHandler.obtainMessage(22), 1000L);
                return true;
            }
            Process.setThreadPriority(10);
            return false;
        }

        private void disconnectService() {
            PackageManagerService.this.mDefContainerConnected = false;
            PackageManagerService.this.mContainerService = null;
            this.mBound = false;
            Process.setThreadPriority(0);
            PackageManagerService.this.mContext.unbindService(PackageManagerService.this.mDefContainerConn);
            Process.setThreadPriority(10);
        }

        PackageHandler(Looper looper) {
            super(looper);
            this.mBound = false;
            this.mPendingInstalls = new ArrayList<>();
        }

        @Override
        public void handleMessage(Message message) {
            try {
                doHandleMessage(message);
            } finally {
                Process.setThreadPriority(10);
            }
        }

        void doHandleMessage(Message message) throws RemoteException {
            int uid;
            boolean z;
            int size;
            int iCopyApk;
            int iCopyApk2 = -22;
            int i = 0;
            switch (message.what) {
                case 1:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        if (PackageManagerService.this.mPendingBroadcasts == null) {
                            return;
                        }
                        int size2 = PackageManagerService.this.mPendingBroadcasts.size();
                        if (size2 <= 0) {
                            return;
                        }
                        String[] strArr = new String[size2];
                        ArrayList[] arrayListArr = new ArrayList[size2];
                        int[] iArr = new int[size2];
                        int i2 = 0;
                        for (int i3 = 0; i3 < PackageManagerService.this.mPendingBroadcasts.userIdCount(); i3++) {
                            int iUserIdAt = PackageManagerService.this.mPendingBroadcasts.userIdAt(i3);
                            Iterator<Map.Entry<String, ArrayList<String>>> it = PackageManagerService.this.mPendingBroadcasts.packagesForUserId(iUserIdAt).entrySet().iterator();
                            while (it.hasNext() && i2 < size2) {
                                Map.Entry<String, ArrayList<String>> next = it.next();
                                strArr[i2] = next.getKey();
                                arrayListArr[i2] = next.getValue();
                                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(next.getKey());
                                if (packageSetting != null) {
                                    uid = UserHandle.getUid(iUserIdAt, packageSetting.appId);
                                } else {
                                    uid = -1;
                                }
                                iArr[i2] = uid;
                                i2++;
                            }
                        }
                        PackageManagerService.this.mPendingBroadcasts.clear();
                        while (i < i2) {
                            PackageManagerService.this.sendPackageChangedBroadcast(strArr[i], true, arrayListArr[i], iArr[i]);
                            i++;
                        }
                        Process.setThreadPriority(10);
                        return;
                    }
                case 2:
                case 4:
                case 8:
                case 12:
                default:
                    return;
                case 3:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_bound");
                    }
                    if (message.obj != null) {
                        PackageManagerService.this.mContainerService = (IMediaContainerService) message.obj;
                        Trace.asyncTraceEnd(262144L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                    }
                    if (PackageManagerService.this.mContainerService == null) {
                        if (!this.mBound) {
                            Slog.e(PackageManagerService.TAG, "Cannot bind to media container service");
                            for (HandlerParams handlerParams : this.mPendingInstalls) {
                                handlerParams.serviceError();
                                Trace.asyncTraceEnd(262144L, "queueInstall", System.identityHashCode(handlerParams));
                                if (handlerParams.traceMethod != null) {
                                    Trace.asyncTraceEnd(262144L, handlerParams.traceMethod, handlerParams.traceCookie);
                                }
                            }
                            this.mPendingInstalls.clear();
                            return;
                        }
                        Slog.w(PackageManagerService.TAG, "Waiting to connect to media container service");
                        return;
                    }
                    if (this.mPendingInstalls.size() > 0) {
                        HandlerParams handlerParams2 = this.mPendingInstalls.get(0);
                        if (handlerParams2 != null) {
                            Trace.asyncTraceEnd(262144L, "queueInstall", System.identityHashCode(handlerParams2));
                            Trace.traceBegin(262144L, "startCopy");
                            if (handlerParams2.startCopy()) {
                                if (this.mPendingInstalls.size() > 0) {
                                    this.mPendingInstalls.remove(0);
                                }
                                if (this.mPendingInstalls.size() == 0) {
                                    if (this.mBound) {
                                        removeMessages(6);
                                        sendMessageDelayed(obtainMessage(6), 10000L);
                                    }
                                } else {
                                    PackageManagerService.this.mHandler.sendEmptyMessage(3);
                                }
                            }
                            Trace.traceEnd(262144L);
                            return;
                        }
                        return;
                    }
                    Slog.w(PackageManagerService.TAG, "Empty queue");
                    return;
                case 5:
                    HandlerParams handlerParams3 = (HandlerParams) message.obj;
                    int size3 = this.mPendingInstalls.size();
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "init_copy idx=" + size3 + ": " + handlerParams3);
                    }
                    if (!this.mBound) {
                        Trace.asyncTraceBegin(262144L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                            handlerParams3.serviceError();
                            Trace.asyncTraceEnd(262144L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                            if (handlerParams3.traceMethod != null) {
                                Trace.asyncTraceEnd(262144L, handlerParams3.traceMethod, handlerParams3.traceCookie);
                                return;
                            }
                            return;
                        }
                        this.mPendingInstalls.add(size3, handlerParams3);
                        return;
                    }
                    this.mPendingInstalls.add(size3, handlerParams3);
                    if (size3 == 0) {
                        PackageManagerService.this.mHandler.sendEmptyMessage(3);
                        return;
                    }
                    return;
                case 6:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_unbind");
                    }
                    if (this.mPendingInstalls.size() == 0 && PackageManagerService.this.mPendingVerification.size() == 0) {
                        if (this.mBound) {
                            if (PackageManagerService.DEBUG_INSTALL) {
                                Slog.i(PackageManagerService.TAG, "calling disconnectService()");
                            }
                            disconnectService();
                            return;
                        }
                        return;
                    }
                    if (this.mPendingInstalls.size() > 0) {
                        PackageManagerService.this.mHandler.sendEmptyMessage(3);
                        return;
                    }
                    return;
                case 7:
                    Process.setThreadPriority(0);
                    String str = (String) message.obj;
                    int i4 = message.arg1;
                    z = message.arg2 != 0;
                    synchronized (PackageManagerService.this.mPackages) {
                        try {
                            if (i4 == -1) {
                                int[] userIds = PackageManagerService.sUserManager.getUserIds();
                                int length = userIds.length;
                                while (i < length) {
                                    PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(userIds[i], str, z));
                                    i++;
                                }
                            } else {
                                PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(i4, str, z));
                            }
                        } finally {
                        }
                        break;
                    }
                    Process.setThreadPriority(10);
                    PackageManagerService.this.startCleaningPackages();
                    return;
                case 9:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Log.v(PackageManagerService.TAG, "Handling post-install for " + message.arg1);
                    }
                    PostInstallData postInstallData = PackageManagerService.this.mRunningInstalls.get(message.arg1);
                    boolean z2 = message.arg2 != 0;
                    PackageManagerService.this.mRunningInstalls.delete(message.arg1);
                    if (postInstallData != null) {
                        InstallArgs installArgs = postInstallData.args;
                        PackageInstalledInfo packageInstalledInfo = postInstallData.res;
                        boolean z3 = (installArgs.installFlags & 256) != 0;
                        boolean z4 = (installArgs.installFlags & 4096) == 0;
                        z = (installArgs.installFlags & 65536) != 0;
                        String[] strArr2 = installArgs.installGrantPermissions;
                        PackageManagerService.this.handlePackagePostInstall(packageInstalledInfo, z3, z4, z, strArr2, z2, installArgs.installerPackageName, installArgs.observer);
                        if (packageInstalledInfo.addedChildPackages != null) {
                            size = packageInstalledInfo.addedChildPackages.size();
                        } else {
                            size = 0;
                        }
                        while (i < size) {
                            PackageManagerService.this.handlePackagePostInstall(packageInstalledInfo.addedChildPackages.valueAt(i), z3, z4, z, strArr2, false, installArgs.installerPackageName, installArgs.observer);
                            i++;
                        }
                        if (installArgs.traceMethod != null) {
                            Trace.asyncTraceEnd(262144L, installArgs.traceMethod, installArgs.traceCookie);
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Bogus post-install token " + message.arg1);
                    }
                    Trace.asyncTraceEnd(262144L, "postInstall", message.arg1);
                    return;
                case 10:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_reconnect");
                    }
                    if (this.mPendingInstalls.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                            for (HandlerParams handlerParams4 : this.mPendingInstalls) {
                                handlerParams4.serviceError();
                                Trace.asyncTraceEnd(262144L, "queueInstall", System.identityHashCode(handlerParams4));
                            }
                            this.mPendingInstalls.clear();
                            return;
                        }
                        return;
                    }
                    return;
                case 11:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_giveup too many retries");
                    }
                    Trace.asyncTraceEnd(262144L, "queueInstall", System.identityHashCode(this.mPendingInstalls.remove(0)));
                    return;
                case 13:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(13);
                        removeMessages(14);
                        PackageManagerService.this.mSettings.writeLPr();
                        PackageManagerService.this.mDirtyUsers.clear();
                        break;
                    }
                    Process.setThreadPriority(10);
                    return;
                case 14:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(14);
                        Iterator it2 = PackageManagerService.this.mDirtyUsers.iterator();
                        while (it2.hasNext()) {
                            PackageManagerService.this.mSettings.writePackageRestrictionsLPr(((Integer) it2.next()).intValue());
                        }
                        PackageManagerService.this.mDirtyUsers.clear();
                        break;
                    }
                    Process.setThreadPriority(10);
                    return;
                case 15:
                    int i5 = message.arg1;
                    PackageVerificationState packageVerificationState = PackageManagerService.this.mPendingVerification.get(i5);
                    if (packageVerificationState == null) {
                        Slog.w(PackageManagerService.TAG, "Invalid verification token " + i5 + " received");
                        return;
                    }
                    PackageVerificationResponse packageVerificationResponse = (PackageVerificationResponse) message.obj;
                    packageVerificationState.setVerifierResponse(packageVerificationResponse.callerUid, packageVerificationResponse.code);
                    if (packageVerificationState.isVerificationComplete()) {
                        PackageManagerService.this.mPendingVerification.remove(i5);
                        InstallArgs installArgs2 = packageVerificationState.getInstallArgs();
                        Uri uriFromFile = Uri.fromFile(installArgs2.origin.resolvedFile);
                        if (packageVerificationState.isInstallAllowed()) {
                            iCopyApk2 = RequestStatus.SYS_ETIMEDOUT;
                            PackageManagerService.this.broadcastPackageVerified(i5, uriFromFile, packageVerificationResponse.code, packageVerificationState.getInstallArgs().getUser());
                            try {
                                iCopyApk2 = installArgs2.copyApk(PackageManagerService.this.mContainerService, true);
                            } catch (RemoteException e) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                            }
                        }
                        Trace.asyncTraceEnd(262144L, "verification", i5);
                        PackageManagerService.this.processPendingInstall(installArgs2, iCopyApk2);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        return;
                    }
                    return;
                case 16:
                    int i6 = message.arg1;
                    PackageVerificationState packageVerificationState2 = PackageManagerService.this.mPendingVerification.get(i6);
                    if (packageVerificationState2 != null && !packageVerificationState2.timeoutExtended()) {
                        InstallArgs installArgs3 = packageVerificationState2.getInstallArgs();
                        Uri uriFromFile2 = Uri.fromFile(installArgs3.origin.resolvedFile);
                        Slog.i(PackageManagerService.TAG, "Verification timed out for " + uriFromFile2);
                        PackageManagerService.this.mPendingVerification.remove(i6);
                        UserHandle user = installArgs3.getUser();
                        if (PackageManagerService.this.getDefaultVerificationResponse(user) != 1) {
                            PackageManagerService.this.broadcastPackageVerified(i6, uriFromFile2, -1, user);
                        } else {
                            Slog.i(PackageManagerService.TAG, "Continuing with installation of " + uriFromFile2);
                            packageVerificationState2.setVerifierResponse(Binder.getCallingUid(), 2);
                            PackageManagerService.this.broadcastPackageVerified(i6, uriFromFile2, 1, user);
                            try {
                                iCopyApk = installArgs3.copyApk(PackageManagerService.this.mContainerService, true);
                                break;
                            } catch (RemoteException e2) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                                iCopyApk = -22;
                            }
                            Trace.asyncTraceEnd(262144L, "verification", i6);
                            PackageManagerService.this.processPendingInstall(installArgs3, iCopyApk);
                            PackageManagerService.this.mHandler.sendEmptyMessage(6);
                            return;
                        }
                        iCopyApk = -22;
                        Trace.asyncTraceEnd(262144L, "verification", i6);
                        PackageManagerService.this.processPendingInstall(installArgs3, iCopyApk);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        return;
                    }
                    return;
                case 17:
                    IFVerificationParams iFVerificationParams = (IFVerificationParams) message.obj;
                    PackageManagerService.this.verifyIntentFiltersIfNeeded(iFVerificationParams.userId, iFVerificationParams.verifierUid, iFVerificationParams.replacing, iFVerificationParams.pkg);
                    return;
                case 18:
                    int i7 = message.arg1;
                    IntentFilterVerificationState intentFilterVerificationState = PackageManagerService.this.mIntentFilterVerificationStates.get(i7);
                    if (intentFilterVerificationState == null) {
                        Slog.w(PackageManagerService.TAG, "Invalid IntentFilter verification token " + i7 + " received");
                        return;
                    }
                    int userId = intentFilterVerificationState.getUserId();
                    if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "Processing IntentFilter verification with token:" + i7 + " and userId:" + userId);
                    }
                    IntentFilterVerificationResponse intentFilterVerificationResponse = (IntentFilterVerificationResponse) message.obj;
                    intentFilterVerificationState.setVerifierResponse(intentFilterVerificationResponse.callerUid, intentFilterVerificationResponse.code);
                    if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "IntentFilter verification with token:" + i7 + " and userId:" + userId + " is settings verifier response with response code:" + intentFilterVerificationResponse.code);
                    }
                    if (intentFilterVerificationResponse.code == -1 && PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "Domains failing verification: " + intentFilterVerificationResponse.getFailedDomainsString());
                    }
                    if (intentFilterVerificationState.isVerificationComplete()) {
                        PackageManagerService.this.mIntentFilterVerifier.receiveVerificationResponse(i7);
                        return;
                    }
                    if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "IntentFilter verification with token:" + i7 + " was not said to be complete");
                        return;
                    }
                    return;
                case 19:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(19);
                        PackageManagerService.this.mSettings.writePackageListLPr(message.arg1);
                        break;
                    }
                    Process.setThreadPriority(10);
                    return;
                case 20:
                    InstantAppResolver.doInstantAppResolutionPhaseTwo(PackageManagerService.this.mContext, PackageManagerService.this.mInstantAppResolverConnection, (InstantAppRequest) message.obj, PackageManagerService.this.mInstantAppInstallerActivity, PackageManagerService.this.mHandler);
                    break;
                case 21:
                    if (!this.mBound) {
                        Trace.asyncTraceBegin(262144L, "earlyBindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                        }
                        Trace.asyncTraceEnd(262144L, "earlyBindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                        return;
                    }
                    return;
                case 22:
                    break;
            }
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.i(PackageManagerService.TAG, "mcs_check");
            }
            if (!PackageManagerService.this.mDefContainerConnected && PackageManagerService.access$608(PackageManagerService.this) < 5) {
                connectToService();
            }
        }
    }

    private void handlePackagePostInstall(PackageInstalledInfo packageInstalledInfo, boolean z, boolean z2, boolean z3, String[] strArr, boolean z4, String str, IPackageInstallObserver2 iPackageInstallObserver2) {
        String str2;
        String str3;
        int[] iArr;
        String str4;
        PackageSetting packageSetting;
        int[] iArr2;
        boolean z5;
        if (packageInstalledInfo.returnCode == 1) {
            if (packageInstalledInfo.removedInfo != null) {
                packageInstalledInfo.removedInfo.sendPackageRemovedBroadcasts(z2);
            }
            if (z) {
                this.mPermissionManager.grantRequestedRuntimePermissions(packageInstalledInfo.pkg, packageInstalledInfo.newUsers, strArr, Binder.getCallingUid(), this.mPermissionCallback);
            }
            boolean z6 = (packageInstalledInfo.removedInfo == null || packageInstalledInfo.removedInfo.removedPackage == null) ? false : true;
            if (packageInstalledInfo.installerPackageName != null) {
                str3 = packageInstalledInfo.installerPackageName;
            } else if (packageInstalledInfo.removedInfo != null) {
                str3 = packageInstalledInfo.removedInfo.installerPackageName;
            } else {
                str2 = null;
                grantCtaRuntimePerm(z6, packageInstalledInfo);
                if (packageInstalledInfo.pkg.parentPackage != null) {
                    this.mPermissionManager.grantRuntimePermissionsGrantedToDisabledPackage(packageInstalledInfo.pkg, Binder.getCallingUid(), this.mPermissionCallback);
                }
                synchronized (this.mPackages) {
                    this.mInstantAppRegistry.onPackageInstalledLPw(packageInstalledInfo.pkg, packageInstalledInfo.newUsers);
                }
                String str5 = packageInstalledInfo.pkg.applicationInfo.packageName;
                int[] iArr3 = EMPTY_INT_ARRAY;
                int[] iArr4 = EMPTY_INT_ARRAY;
                int[] iArr5 = EMPTY_INT_ARRAY;
                int[] iArr6 = EMPTY_INT_ARRAY;
                boolean z7 = packageInstalledInfo.origUsers == null || packageInstalledInfo.origUsers.length == 0;
                PackageSetting packageSetting2 = (PackageSetting) packageInstalledInfo.pkg.mExtras;
                int[] iArr7 = packageInstalledInfo.newUsers;
                int length = iArr7.length;
                int[] iArrAppendInt = iArr6;
                int[] iArrAppendInt2 = iArr4;
                int[] iArrAppendInt3 = iArr5;
                int[] iArrAppendInt4 = iArr3;
                int i = 0;
                while (i < length) {
                    int i2 = iArr7[i];
                    boolean instantApp = packageSetting2.getInstantApp(i2);
                    if (z7) {
                        if (instantApp) {
                            iArrAppendInt2 = ArrayUtils.appendInt(iArrAppendInt2, i2);
                        } else {
                            iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i2);
                        }
                        packageSetting = packageSetting2;
                        iArr2 = iArr7;
                    } else {
                        int[] iArr8 = packageInstalledInfo.origUsers;
                        packageSetting = packageSetting2;
                        int length2 = iArr8.length;
                        iArr2 = iArr7;
                        int i3 = 0;
                        while (true) {
                            if (i3 < length2) {
                                int i4 = length2;
                                if (iArr8[i3] != i2) {
                                    i3++;
                                    length2 = i4;
                                } else {
                                    z5 = false;
                                    break;
                                }
                            } else {
                                z5 = true;
                                break;
                            }
                        }
                        if (z5) {
                            if (instantApp) {
                                iArrAppendInt2 = ArrayUtils.appendInt(iArrAppendInt2, i2);
                            } else {
                                iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i2);
                            }
                        } else if (instantApp) {
                            iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i2);
                        } else {
                            iArrAppendInt3 = ArrayUtils.appendInt(iArrAppendInt3, i2);
                        }
                    }
                    i++;
                    packageSetting2 = packageSetting;
                    iArr7 = iArr2;
                }
                if (packageInstalledInfo.pkg.staticSharedLibName == null) {
                    this.mProcessLoggingHandler.invalidateProcessLoggingBaseApkHash(packageInstalledInfo.pkg.baseCodePath);
                    int[] iArr9 = iArrAppendInt3;
                    iArr = iArrAppendInt4;
                    int[] iArr10 = iArrAppendInt2;
                    sendPackageAddedForNewUsers(str5, packageInstalledInfo.pkg.applicationInfo.isSystemApp() || z3, z3, UserHandle.getAppId(packageInstalledInfo.uid), iArr, iArr10);
                    Bundle bundle = new Bundle(1);
                    bundle.putInt("android.intent.extra.UID", packageInstalledInfo.uid);
                    if (z6) {
                        bundle.putBoolean("android.intent.extra.REPLACING", true);
                    }
                    String str6 = str2;
                    sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", str5, bundle, 0, null, null, iArr9, iArrAppendInt);
                    if (this.mPowerHalManager != null) {
                        this.mPowerHalManager.setInstallationBoost(false);
                    }
                    if (str6 != null) {
                        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", str5, bundle, 0, str6, null, iArr9, iArrAppendInt);
                    }
                    boolean z8 = (this.mRequiredVerifierPackage == null || this.mRequiredVerifierPackage.equals(str6)) ? false : true;
                    if (z8) {
                        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", str5, bundle, 0, this.mRequiredVerifierPackage, null, iArr9, iArrAppendInt);
                    }
                    if (z6) {
                        sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", str5, bundle, 0, null, null, iArr9, iArrAppendInt);
                        if (str6 != null) {
                            sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", str5, bundle, 0, str6, null, iArr9, iArrAppendInt);
                        }
                        if (z8) {
                            sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", str5, bundle, 0, this.mRequiredVerifierPackage, null, iArr9, iArrAppendInt);
                        }
                        sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, str5, null, iArr9, iArrAppendInt);
                    } else {
                        if (z4 && !isSystemApp(packageInstalledInfo.pkg)) {
                            if (DEBUG_BACKUP) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Post-restore of ");
                                str4 = str5;
                                sb.append(str4);
                                sb.append(" sending FIRST_LAUNCH in ");
                                sb.append(Arrays.toString(iArr));
                                Slog.i(TAG, sb.toString());
                            } else {
                                str4 = str5;
                            }
                            sendFirstLaunchBroadcast(str4, str, iArr, iArr10);
                        }
                        if (!packageInstalledInfo.pkg.isForwardLocked() || isExternal(packageInstalledInfo.pkg)) {
                            if (DEBUG_INSTALL) {
                                Slog.i(TAG, "upgrading pkg " + packageInstalledInfo.pkg + " is ASEC-hosted -> AVAILABLE");
                            }
                            int[] iArr11 = {packageInstalledInfo.pkg.applicationInfo.uid};
                            ArrayList<String> arrayList = new ArrayList<>(1);
                            arrayList.add(str4);
                            sendResourcesChangedBroadcast(true, true, arrayList, iArr11, (IIntentReceiver) null);
                        }
                    }
                    str4 = str5;
                    if (!packageInstalledInfo.pkg.isForwardLocked()) {
                        if (DEBUG_INSTALL) {
                        }
                        int[] iArr112 = {packageInstalledInfo.pkg.applicationInfo.uid};
                        ArrayList<String> arrayList2 = new ArrayList<>(1);
                        arrayList2.add(str4);
                        sendResourcesChangedBroadcast(true, true, arrayList2, iArr112, (IIntentReceiver) null);
                    }
                } else {
                    iArr = iArrAppendInt4;
                    str4 = str5;
                }
                if (iArr != null && iArr.length > 0) {
                    synchronized (this.mPackages) {
                        for (int i5 : iArr) {
                            if (packageIsBrowser(str4, i5)) {
                                this.mSettings.setDefaultBrowserPackageNameLPw(null, i5);
                            }
                            this.mSettings.applyPendingPermissionGrantsLPw(str4, i5);
                        }
                    }
                }
                if (z7 && !z6) {
                    notifyPackageAdded(str4);
                }
                EventLog.writeEvent(EventLogTags.UNKNOWN_SOURCES_ENABLED, getUnknownSourcesSettings());
                if (packageInstalledInfo.removedInfo != null && packageInstalledInfo.removedInfo.args != null) {
                    Runtime.getRuntime().gc();
                    synchronized (this.mInstallLock) {
                        packageInstalledInfo.removedInfo.args.doPostDeleteLI(true);
                    }
                } else {
                    VMRuntime.getRuntime().requestConcurrentGC();
                }
                for (int i6 : iArr) {
                    PackageInfo packageInfo = getPackageInfo(str4, 0, i6);
                    if (packageInfo != null) {
                        this.mDexManager.notifyPackageInstalled(packageInfo, i6);
                        sPmsExt.onPackageAdded(str4, i6);
                    }
                }
            }
            str2 = str3;
            grantCtaRuntimePerm(z6, packageInstalledInfo);
            if (packageInstalledInfo.pkg.parentPackage != null) {
            }
            synchronized (this.mPackages) {
            }
        }
        if (iPackageInstallObserver2 != null) {
            try {
                iPackageInstallObserver2.onPackageInstalled(packageInstalledInfo.name, packageInstalledInfo.returnCode, packageInstalledInfo.returnMsg, extrasForInstallResult(packageInstalledInfo));
            } catch (RemoteException e) {
                Slog.i(TAG, "Observer no longer exists.");
            }
        }
    }

    Bundle extrasForInstallResult(PackageInstalledInfo packageInstalledInfo) {
        int i = packageInstalledInfo.returnCode;
        if (i == -112) {
            Bundle bundle = new Bundle();
            bundle.putString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION", packageInstalledInfo.origPermission);
            bundle.putString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE", packageInstalledInfo.origPackage);
            return bundle;
        }
        if (i == 1) {
            Bundle bundle2 = new Bundle();
            bundle2.putBoolean("android.intent.extra.REPLACING", (packageInstalledInfo.removedInfo == null || packageInstalledInfo.removedInfo.removedPackage == null) ? false : true);
            return bundle2;
        }
        return null;
    }

    public void scheduleWriteSettingsLocked() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessageDelayed(13, 10000L);
        }
    }

    void scheduleWritePackageListLocked(int i) {
        if (!this.mHandler.hasMessages(19)) {
            Message messageObtainMessage = this.mHandler.obtainMessage(19);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessageDelayed(messageObtainMessage, 10000L);
        }
    }

    void scheduleWritePackageRestrictionsLocked(UserHandle userHandle) {
        scheduleWritePackageRestrictionsLocked(userHandle == null ? -1 : userHandle.getIdentifier());
    }

    void scheduleWritePackageRestrictionsLocked(int i) {
        int[] userIds;
        if (i == -1) {
            userIds = sUserManager.getUserIds();
        } else {
            userIds = new int[]{i};
        }
        for (int i2 : userIds) {
            if (!sUserManager.exists(i2)) {
                return;
            }
            this.mDirtyUsers.add(Integer.valueOf(i2));
            if (!this.mHandler.hasMessages(14)) {
                this.mHandler.sendEmptyMessageDelayed(14, 10000L);
            }
        }
    }

    public static PackageManagerService main(Context context, Installer installer, boolean z, boolean z2) {
        PackageManagerServiceCompilerMapping.checkProperties();
        ?? packageManagerService = new PackageManagerService(context, installer, z, z2);
        packageManagerService.enableSystemUserPackages();
        ServiceManager.addService("package", (IBinder) packageManagerService);
        Objects.requireNonNull(packageManagerService);
        ServiceManager.addService("package_native", new PackageManagerNative());
        return packageManagerService;
    }

    private void enableSystemUserPackages() {
        boolean zContains;
        if (!UserManager.isSplitSystemUser()) {
            return;
        }
        AppsQueryHelper appsQueryHelper = new AppsQueryHelper(this);
        ArraySet arraySet = new ArraySet();
        arraySet.addAll(appsQueryHelper.queryApps(AppsQueryHelper.GET_NON_LAUNCHABLE_APPS | AppsQueryHelper.GET_APPS_WITH_INTERACT_ACROSS_USERS_PERM | AppsQueryHelper.GET_IMES, true, UserHandle.SYSTEM));
        arraySet.addAll((Collection) SystemConfig.getInstance().getSystemUserWhitelistedApps());
        arraySet.addAll(appsQueryHelper.queryApps(AppsQueryHelper.GET_REQUIRED_FOR_SYSTEM_USER, false, UserHandle.SYSTEM));
        arraySet.removeAll((Collection<?>) SystemConfig.getInstance().getSystemUserBlacklistedApps());
        Log.i(TAG, "Applications installed for system user: " + arraySet);
        List listQueryApps = appsQueryHelper.queryApps(0, false, UserHandle.SYSTEM);
        int size = listQueryApps.size();
        synchronized (this.mPackages) {
            for (int i = 0; i < size; i++) {
                try {
                    String str = (String) listQueryApps.get(i);
                    PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                    if (packageSetting != null && packageSetting.getInstalled(0) != (zContains = arraySet.contains(str))) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(zContains ? "Installing " : "Uninstalling ");
                        sb.append(str);
                        sb.append(" for system user");
                        Log.i(TAG, sb.toString());
                        packageSetting.setInstalled(zContains, 0);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            scheduleWritePackageRestrictionsLocked(0);
        }
    }

    private static void getDefaultDisplayMetrics(Context context, DisplayMetrics displayMetrics) {
        ((DisplayManager) context.getSystemService("display")).getDisplay(0).getMetrics(displayMetrics);
    }

    private static void requestCopyPreoptedFiles() {
        if (SystemProperties.getInt("ro.cp_system_other_odex", 0) == 1) {
            SystemProperties.set("sys.cppreopt", "requested");
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j = 100000 + jUptimeMillis;
            long jUptimeMillis2 = jUptimeMillis;
            while (true) {
                if (SystemProperties.get("sys.cppreopt").equals("finished")) {
                    break;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                }
                jUptimeMillis2 = SystemClock.uptimeMillis();
                if (jUptimeMillis2 > j) {
                    SystemProperties.set("sys.cppreopt", "timed-out");
                    Slog.wtf(TAG, "cppreopt did not finish!");
                    break;
                }
            }
            Slog.i(TAG, "cppreopts took " + (jUptimeMillis2 - jUptimeMillis) + " ms");
        }
    }

    public PackageManagerService(Context context, Installer installer, boolean z, boolean z2) throws Throwable {
        ArrayMap<String, PackageParser.Package> arrayMap;
        File canonicalFile;
        File canonicalFile2;
        File canonicalFile3;
        File canonicalFile4;
        File canonicalFile5;
        File canonicalFile6;
        int i;
        String str;
        File file;
        File file2;
        File file3;
        File file4;
        int i2;
        File file5;
        File file6;
        File file7;
        File file8;
        File file9;
        File file10;
        File file11;
        File file12;
        File file13;
        int i3;
        int i4;
        int i5;
        String str2;
        ArrayList arrayList;
        this.mDeferProtectedFilters = true;
        this.mServices = new ServiceIntentResolver();
        this.mProviders = new ProviderIntentResolver();
        LockGuard.installLock(this.mPackages, 3);
        Trace.traceBegin(262144L, "create package manager");
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START, SystemClock.uptimeMillis());
        sMtkSystemServerIns.addBootEvent("Android:PackageManagerService_Start");
        if (this.mSdkVersion <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }
        this.mContext = context;
        this.mFactoryTest = z;
        this.mOnlyCore = z2;
        this.mMetrics = new DisplayMetrics();
        this.mInstaller = installer;
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                LocalServices.addService(PackageManagerInternal.class, new PackageManagerInternalImpl());
                sUserManager = new UserManagerService(context, this, new UserDataPreparer(this.mInstaller, this.mInstallLock, this.mContext, this.mOnlyCore), this.mPackages);
                this.mPermissionManager = PermissionManagerService.create(context, new DefaultPermissionGrantPolicy.DefaultPermissionGrantedCallback() {
                    @Override
                    public void onDefaultRuntimePermissionsGranted(int i6) {
                        synchronized (PackageManagerService.this.mPackages) {
                            PackageManagerService.this.mSettings.onDefaultRuntimePermissionsGrantedLPr(i6);
                        }
                    }
                }, this.mPackages);
                this.mDefaultPermissionPolicy = this.mPermissionManager.getDefaultPermissionGrantPolicy();
                this.mSettings = new Settings(this.mPermissionManager.getPermissionSettings(), this.mPackages);
            }
        }
        this.mSettings.addSharedUserLPw("android.uid.system", 1000, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.phone", 1001, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.log", LOG_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.nfc", 1027, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.bluetooth", BLUETOOTH_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.shell", 2000, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.se", SE_UID, 1, 8);
        String str3 = SystemProperties.get("debug.separate_processes");
        if (str3 == null || str3.length() <= 0) {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = null;
        } else if ("*".equals(str3)) {
            this.mDefParseFlags = 2;
            this.mSeparateProcesses = null;
            Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
        } else {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = str3.split(",");
            Slog.w(TAG, "Running with debug.separate_processes: " + str3);
        }
        this.mPackageDexOptimizer = new PackageDexOptimizer(installer, this.mInstallLock, context, "*dexopt*");
        long j = 262144;
        this.mDexManager = new DexManager(this.mContext, this, this.mPackageDexOptimizer, installer, this.mInstallLock, DexLogger.getListener(this, installer, this.mInstallLock));
        this.mArtManagerService = new ArtManagerService(this.mContext, this, installer, this.mInstallLock);
        this.mMoveCallbacks = new MoveCallbacks(FgThread.get().getLooper());
        this.mOnPermissionChangeListeners = new OnPermissionChangeListeners(FgThread.get().getLooper());
        getDefaultDisplayMetrics(context, this.mMetrics);
        Trace.traceBegin(262144L, "get system config");
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mAvailableFeatures = systemConfig.getAvailableFeatures();
        Trace.traceEnd(262144L);
        this.mProtectedPackages = new ProtectedPackages(this.mContext);
        Object obj = this.mInstallLock;
        synchronized (obj) {
            try {
                try {
                    ArrayMap<String, PackageParser.Package> arrayMap2 = this.mPackages;
                    synchronized (arrayMap2) {
                        try {
                            this.mHandlerThread = new ServiceThread(TAG, 10, true);
                            this.mHandlerThread.start();
                            this.mHandler = new PackageHandler(this.mHandlerThread.getLooper());
                            this.mProcessLoggingHandler = new ProcessLoggingHandler();
                            Watchdog.getInstance().addThread(this.mHandler, 600000L);
                            this.mInstantAppRegistry = new InstantAppRegistry(this);
                            ArrayMap sharedLibraries = systemConfig.getSharedLibraries();
                            int size = sharedLibraries.size();
                            int i6 = 0;
                            while (i6 < size) {
                                int i7 = size;
                                int i8 = i6;
                                arrayMap = arrayMap2;
                                Object obj2 = obj;
                                long j2 = j;
                                try {
                                    addSharedLibraryLPw((String) sharedLibraries.valueAt(i6), null, (String) sharedLibraries.keyAt(i6), -1L, 0, PLATFORM_PACKAGE_NAME, 0L);
                                    i6 = i8 + 1;
                                    j = j2;
                                    arrayMap2 = arrayMap;
                                    obj = obj2;
                                    size = i7;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                            arrayMap = arrayMap2;
                            Object obj3 = obj;
                            long j3 = j;
                            SELinuxMMAC.readInstallPolicy();
                            Trace.traceBegin(j3, "loadFallbacks");
                            FallbackCategoryProvider.loadFallbacks();
                            Trace.traceEnd(j3);
                            Trace.traceBegin(j3, "read user settings");
                            this.mFirstBoot = !this.mSettings.readLPw(sUserManager.getUsers(false));
                            Trace.traceEnd(j3);
                            sPmsExt.init(this, sUserManager);
                            for (int size2 = this.mSettings.mPackages.size() - 1; size2 >= 0; size2--) {
                                PackageSetting packageSettingValueAt = this.mSettings.mPackages.valueAt(size2);
                                if (!isExternal(packageSettingValueAt) && ((packageSettingValueAt.codePath == null || !packageSettingValueAt.codePath.exists()) && this.mSettings.getDisabledSystemPkgLPr(packageSettingValueAt.name) != null)) {
                                    this.mSettings.mPackages.removeAt(size2);
                                    this.mSettings.enableSystemPackageLPw(packageSettingValueAt.name);
                                }
                            }
                            if (!this.mOnlyCore && this.mFirstBoot) {
                                requestCopyPreoptedFiles();
                            }
                            String string = Resources.getSystem().getString(R.string.accessibility_system_action_notifications_label);
                            if (!TextUtils.isEmpty(string)) {
                                this.mCustomResolverComponentName = ComponentName.unflattenFromString(string);
                            }
                            long jUptimeMillis = SystemClock.uptimeMillis();
                            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, jUptimeMillis);
                            sMtkSystemServerIns.addBootEvent("Android:PMS_scan_START");
                            sCtaManager.createCtaPermsController(this.mContext);
                            sPmsExt.initBeforeScan();
                            String str4 = System.getenv("BOOTCLASSPATH");
                            String str5 = System.getenv("SYSTEMSERVERCLASSPATH");
                            if (str4 == null) {
                                Slog.w(TAG, "No BOOTCLASSPATH found!");
                            }
                            if (str5 == null) {
                                Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
                            }
                            File file14 = new File(Environment.getRootDirectory(), "framework");
                            Settings.VersionInfo internalVersion = this.mSettings.getInternalVersion();
                            this.mIsUpgrade = !Build.FINGERPRINT.equals(internalVersion.fingerprint);
                            if (this.mIsUpgrade) {
                                PackageManagerServiceUtils.logCriticalInfo(4, "Upgrading from " + internalVersion.fingerprint + " to " + Build.FINGERPRINT);
                            }
                            this.mPromoteSystemApps = this.mIsUpgrade && internalVersion.sdkVersion <= 22;
                            this.mIsPreNUpgrade = this.mIsUpgrade && internalVersion.sdkVersion < 24;
                            this.mIsPreNMR1Upgrade = this.mIsUpgrade && internalVersion.sdkVersion < 25;
                            if (this.mPromoteSystemApps) {
                                for (PackageSetting packageSetting : this.mSettings.mPackages.values()) {
                                    if (isSystemApp(packageSetting)) {
                                        this.mExistingSystemPackages.add(packageSetting.name);
                                    }
                                }
                            }
                            this.mCacheDir = preparePackageParserCache(this.mIsUpgrade);
                            int i9 = (this.mIsUpgrade || this.mFirstBoot) ? 8720 : 528;
                            int i10 = i9 | 131072;
                            int i11 = i10 | 1048576;
                            scanDirTracedLI(new File(VENDOR_OVERLAY_DIR), this.mDefParseFlags | 16, i11, 0L);
                            int i12 = i10 | 2097152;
                            scanDirTracedLI(new File(PRODUCT_OVERLAY_DIR), this.mDefParseFlags | 16, i12, 0L);
                            sPmsExt.scanDirLI(9, this.mDefParseFlags, i9, 0L);
                            this.mParallelPackageParserCallback.findStaticOverlayPackages();
                            scanDirTracedLI(file14, this.mDefParseFlags | 16, i9 | 1 | 131072 | 262144, 0L);
                            sPmsExt.scanDirLI(10, this.mDefParseFlags, i9, 0L);
                            File file15 = new File(Environment.getRootDirectory(), "priv-app");
                            int i13 = i10 | 262144;
                            scanDirTracedLI(file15, this.mDefParseFlags | 16, i13, 0L);
                            File file16 = new File(Environment.getRootDirectory(), "app");
                            scanDirTracedLI(file16, this.mDefParseFlags | 16, i10, 0L);
                            File file17 = new File(Environment.getVendorDirectory(), "priv-app");
                            try {
                                canonicalFile = file17.getCanonicalFile();
                            } catch (IOException e) {
                                canonicalFile = file17;
                            }
                            int i14 = i11 | 262144;
                            File file18 = canonicalFile;
                            scanDirTracedLI(canonicalFile, this.mDefParseFlags | 16, i14, 0L);
                            File file19 = new File(Environment.getVendorDirectory(), "app");
                            try {
                                canonicalFile2 = file19.getCanonicalFile();
                            } catch (IOException e2) {
                                canonicalFile2 = file19;
                            }
                            File file20 = canonicalFile2;
                            scanDirTracedLI(canonicalFile2, this.mDefParseFlags | 16, i11, 0L);
                            File file21 = new File(Environment.getOdmDirectory(), "priv-app");
                            try {
                                canonicalFile3 = file21.getCanonicalFile();
                            } catch (IOException e3) {
                                canonicalFile3 = file21;
                            }
                            File file22 = canonicalFile3;
                            scanDirTracedLI(canonicalFile3, this.mDefParseFlags | 16, i14, 0L);
                            File file23 = new File(Environment.getOdmDirectory(), "app");
                            try {
                                canonicalFile4 = file23.getCanonicalFile();
                            } catch (IOException e4) {
                                canonicalFile4 = file23;
                            }
                            File file24 = canonicalFile4;
                            scanDirTracedLI(canonicalFile4, this.mDefParseFlags | 16, i11, 0L);
                            File file25 = new File(Environment.getOemDirectory(), "app");
                            int i15 = i10 | 524288;
                            File file26 = file25;
                            scanDirTracedLI(file25, this.mDefParseFlags | 16, i15, 0L);
                            File file27 = new File(Environment.getProductDirectory(), "priv-app");
                            try {
                                canonicalFile5 = file27.getCanonicalFile();
                            } catch (IOException e5) {
                                canonicalFile5 = file27;
                            }
                            int i16 = i12 | 262144;
                            File file28 = canonicalFile5;
                            scanDirTracedLI(canonicalFile5, this.mDefParseFlags | 16, i16, 0L);
                            File file29 = new File(Environment.getProductDirectory(), "app");
                            try {
                                canonicalFile6 = file29.getCanonicalFile();
                            } catch (IOException e6) {
                                canonicalFile6 = file29;
                            }
                            File file30 = canonicalFile6;
                            scanDirTracedLI(canonicalFile6, this.mDefParseFlags | 16, i12, 0L);
                            sPmsExt.scanMoreDirLi(this.mDefParseFlags, i9);
                            ArrayList<String> arrayList2 = new ArrayList();
                            ArrayList arrayList3 = new ArrayList();
                            if (!this.mOnlyCore) {
                                for (PackageParser.Package r2 : this.mPackages.values()) {
                                    if (r2.isStub) {
                                        arrayList3.add(r2.packageName);
                                    }
                                }
                                Iterator<PackageSetting> it = this.mSettings.mPackages.values().iterator();
                                while (it.hasNext()) {
                                    PackageSetting next = it.next();
                                    if ((next.pkgFlags & 1) != 0) {
                                        PackageParser.Package r3 = this.mPackages.get(next.name);
                                        if (r3 == null) {
                                            arrayList = arrayList3;
                                            if (this.mSettings.isDisabledSystemPackageLPr(next.name)) {
                                                PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(next.name);
                                                if (disabledSystemPkgLPr.codePath == null || !disabledSystemPkgLPr.codePath.exists() || disabledSystemPkgLPr.pkg == null) {
                                                    arrayList2.add(next.name);
                                                }
                                            } else {
                                                it.remove();
                                                PackageManagerServiceUtils.logCriticalInfo(5, "System package " + next.name + " no longer exists; it's data will be wiped");
                                            }
                                        } else if (this.mSettings.isDisabledSystemPackageLPr(next.name)) {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("Expecting better updated system app for ");
                                            sb.append(next.name);
                                            sb.append("; removing system app.  Last known codePath=");
                                            sb.append(next.codePathString);
                                            sb.append(", versionCode=");
                                            arrayList = arrayList3;
                                            sb.append(next.versionCode);
                                            sb.append("; scanned versionCode=");
                                            sb.append(r3.getLongVersionCode());
                                            PackageManagerServiceUtils.logCriticalInfo(5, sb.toString());
                                            removePackageLI(r3, true);
                                            this.mExpectingBetter.put(next.name, next.codePath);
                                        }
                                        arrayList3 = arrayList;
                                    }
                                }
                            }
                            ArrayList arrayList4 = arrayList3;
                            deleteTempPackageFiles();
                            int i17 = PackageParser.sCachedPackageReadCount.get();
                            this.mSettings.pruneSharedUsersLPw();
                            long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
                            int size3 = this.mPackages.size();
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Finished scanning system apps. Time: ");
                            sb2.append(jUptimeMillis2);
                            sb2.append(" ms, packageCount: ");
                            sb2.append(size3);
                            sb2.append(" , timePerPackage: ");
                            sb2.append(size3 == 0 ? 0L : jUptimeMillis2 / ((long) size3));
                            sb2.append(" , cached: ");
                            sb2.append(i17);
                            Slog.i(TAG, sb2.toString());
                            if (this.mIsUpgrade && size3 > 0) {
                                MetricsLogger.histogram((Context) null, "ota_package_manager_system_app_avg_scan_time", ((int) jUptimeMillis2) / size3);
                            }
                            if (!this.mOnlyCore) {
                                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START, SystemClock.uptimeMillis());
                                int i18 = i9 | 128;
                                int i19 = 5;
                                scanDirTracedLI(sAppInstallDir, 0, i18, 0L);
                                scanDirTracedLI(sDrmAppPrivateInstallDir, 4 | this.mDefParseFlags, i18, 0L);
                                for (String str6 : arrayList2) {
                                    PackageParser.Package r22 = this.mPackages.get(str6);
                                    this.mSettings.removeDisabledSystemPackageLPw(str6);
                                    if (r22 == null) {
                                        str2 = "Updated system package " + str6 + " no longer exists; removing its data";
                                    } else {
                                        String str7 = "Updated system package + " + str6 + " no longer exists; revoking system privileges";
                                        PackageSetting packageSetting2 = this.mSettings.mPackages.get(str6);
                                        r22.applicationInfo.flags &= -2;
                                        packageSetting2.pkgFlags &= -2;
                                        str2 = str7;
                                    }
                                    PackageManagerServiceUtils.logCriticalInfo(5, str2);
                                }
                                int i20 = 0;
                                while (i20 < this.mExpectingBetter.size()) {
                                    String strKeyAt = this.mExpectingBetter.keyAt(i20);
                                    if (this.mPackages.containsKey(strKeyAt)) {
                                        file = file18;
                                        file2 = file26;
                                        file3 = file28;
                                        file4 = file30;
                                        i2 = i17;
                                        file5 = file22;
                                        file6 = file24;
                                        file7 = file20;
                                    } else {
                                        File fileValueAt = this.mExpectingBetter.valueAt(i20);
                                        PackageManagerServiceUtils.logCriticalInfo(i19, "Expected better " + strKeyAt + " but never showed up; reverting to system");
                                        try {
                                            if (FileUtils.contains(file15, fileValueAt)) {
                                                i3 = this.mDefParseFlags | 16;
                                                i4 = i13;
                                            } else if (FileUtils.contains(file16, fileValueAt)) {
                                                i3 = this.mDefParseFlags | 16;
                                                i4 = i10;
                                            } else {
                                                file8 = file18;
                                                if (FileUtils.contains(file8, fileValueAt)) {
                                                    file9 = file20;
                                                    file10 = file22;
                                                } else {
                                                    file10 = file22;
                                                    if (FileUtils.contains(file10, fileValueAt)) {
                                                        file9 = file20;
                                                    } else {
                                                        file9 = file20;
                                                        if (FileUtils.contains(file9, fileValueAt)) {
                                                            file11 = file24;
                                                        } else {
                                                            File file31 = file24;
                                                            if (FileUtils.contains(file31, fileValueAt)) {
                                                                file11 = file31;
                                                            } else {
                                                                File file32 = file26;
                                                                if (FileUtils.contains(file32, fileValueAt)) {
                                                                    file12 = file32;
                                                                    file11 = file31;
                                                                    i3 = this.mDefParseFlags | 16;
                                                                    i4 = i15;
                                                                    file3 = file28;
                                                                    file13 = file30;
                                                                    this.mSettings.enableSystemPackageLPw(strKeyAt);
                                                                    file2 = file12;
                                                                    file4 = file13;
                                                                    file6 = file11;
                                                                    file7 = file9;
                                                                    int i21 = i4;
                                                                    file = file8;
                                                                    file5 = file10;
                                                                    i2 = i17;
                                                                    scanPackageTracedLI(fileValueAt, i3, i21, 0L, (UserHandle) null);
                                                                } else {
                                                                    file3 = file28;
                                                                    if (FileUtils.contains(file3, fileValueAt)) {
                                                                        file12 = file32;
                                                                        i5 = this.mDefParseFlags | 16;
                                                                        file11 = file31;
                                                                        i4 = i16;
                                                                        file13 = file30;
                                                                    } else {
                                                                        file12 = file32;
                                                                        File file33 = file30;
                                                                        if (FileUtils.contains(file33, fileValueAt)) {
                                                                            file13 = file33;
                                                                            i5 = this.mDefParseFlags | 16;
                                                                            file11 = file31;
                                                                            i4 = i12;
                                                                        } else {
                                                                            Slog.e(TAG, "Ignoring unexpected fallback path " + fileValueAt);
                                                                            file7 = file9;
                                                                            file = file8;
                                                                            file5 = file10;
                                                                            i2 = i17;
                                                                            file2 = file12;
                                                                            file4 = file33;
                                                                            file6 = file31;
                                                                        }
                                                                    }
                                                                    i3 = i5;
                                                                    this.mSettings.enableSystemPackageLPw(strKeyAt);
                                                                    file2 = file12;
                                                                    file4 = file13;
                                                                    file6 = file11;
                                                                    file7 = file9;
                                                                    int i212 = i4;
                                                                    file = file8;
                                                                    file5 = file10;
                                                                    i2 = i17;
                                                                    scanPackageTracedLI(fileValueAt, i3, i212, 0L, (UserHandle) null);
                                                                }
                                                            }
                                                        }
                                                        file12 = file26;
                                                        file3 = file28;
                                                        file13 = file30;
                                                        i3 = this.mDefParseFlags | 16;
                                                        i4 = i11;
                                                        this.mSettings.enableSystemPackageLPw(strKeyAt);
                                                        file2 = file12;
                                                        file4 = file13;
                                                        file6 = file11;
                                                        file7 = file9;
                                                        int i2122 = i4;
                                                        file = file8;
                                                        file5 = file10;
                                                        i2 = i17;
                                                        scanPackageTracedLI(fileValueAt, i3, i2122, 0L, (UserHandle) null);
                                                    }
                                                }
                                                file11 = file24;
                                                file12 = file26;
                                                file3 = file28;
                                                file13 = file30;
                                                i3 = this.mDefParseFlags | 16;
                                                i4 = i14;
                                                this.mSettings.enableSystemPackageLPw(strKeyAt);
                                                file2 = file12;
                                                file4 = file13;
                                                file6 = file11;
                                                file7 = file9;
                                                int i21222 = i4;
                                                file = file8;
                                                file5 = file10;
                                                i2 = i17;
                                                scanPackageTracedLI(fileValueAt, i3, i21222, 0L, (UserHandle) null);
                                            }
                                            scanPackageTracedLI(fileValueAt, i3, i21222, 0L, (UserHandle) null);
                                        } catch (PackageManagerException e7) {
                                            Slog.e(TAG, "Failed to parse original system package: " + e7.getMessage());
                                        }
                                        file8 = file18;
                                        file9 = file20;
                                        file10 = file22;
                                        file11 = file24;
                                        file12 = file26;
                                        file3 = file28;
                                        file13 = file30;
                                        this.mSettings.enableSystemPackageLPw(strKeyAt);
                                        file2 = file12;
                                        file4 = file13;
                                        file6 = file11;
                                        file7 = file9;
                                        int i212222 = i4;
                                        file = file8;
                                        file5 = file10;
                                        i2 = i17;
                                    }
                                    i20++;
                                    file28 = file3;
                                    file30 = file4;
                                    i17 = i2;
                                    file20 = file7;
                                    i19 = 5;
                                    file18 = file;
                                    file24 = file6;
                                    file22 = file5;
                                    file26 = file2;
                                }
                                decompressSystemApplications(arrayList4, i9);
                                int i22 = PackageParser.sCachedPackageReadCount.get() - i17;
                                long jUptimeMillis3 = (SystemClock.uptimeMillis() - jUptimeMillis2) - jUptimeMillis;
                                int size4 = this.mPackages.size() - size3;
                                StringBuilder sb3 = new StringBuilder();
                                sb3.append("Finished scanning non-system apps. Time: ");
                                sb3.append(jUptimeMillis3);
                                sb3.append(" ms, packageCount: ");
                                sb3.append(size4);
                                sb3.append(" , timePerPackage: ");
                                sb3.append(size4 != 0 ? jUptimeMillis3 / ((long) size4) : 0L);
                                sb3.append(" , cached: ");
                                sb3.append(i22);
                                Slog.i(TAG, sb3.toString());
                                if (this.mIsUpgrade && size4 > 0) {
                                    MetricsLogger.histogram((Context) null, "ota_package_manager_data_app_avg_scan_time", ((int) jUptimeMillis3) / size4);
                                }
                            }
                            this.mExpectingBetter.clear();
                            this.mStorageManagerPackage = getStorageManagerPackageName();
                            this.mSetupWizardPackage = getSetupWizardPackageName();
                            if (this.mProtectedFilters.size() > 0) {
                                if (DEBUG_FILTERS && this.mSetupWizardPackage == null) {
                                    Slog.i(TAG, "No setup wizard; All protected intents capped to priority 0");
                                }
                                for (PackageParser.ActivityIntentInfo activityIntentInfo : this.mProtectedFilters) {
                                    if (!activityIntentInfo.activity.info.packageName.equals(this.mSetupWizardPackage)) {
                                        if (DEBUG_FILTERS) {
                                            Slog.i(TAG, "Protected action; cap priority to 0; package: " + activityIntentInfo.activity.info.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                                        }
                                        activityIntentInfo.setPriority(0);
                                    } else if (DEBUG_FILTERS) {
                                        Slog.i(TAG, "Found setup wizard; allow priority " + activityIntentInfo.getPriority() + "; package: " + activityIntentInfo.activity.info.packageName + " activity: " + activityIntentInfo.activity.className + " priority: " + activityIntentInfo.getPriority());
                                    }
                                }
                            }
                            this.mSystemTextClassifierPackage = getSystemTextClassifierPackageName();
                            this.mDeferProtectedFilters = false;
                            this.mProtectedFilters.clear();
                            updateAllSharedLibrariesLPw(null);
                            for (SharedUserSetting sharedUserSetting : this.mSettings.getAllSharedUsersLPw()) {
                                List<String> listAdjustCpuAbisForSharedUserLPw = adjustCpuAbisForSharedUserLPw(sharedUserSetting.packages, null);
                                if (listAdjustCpuAbisForSharedUserLPw != null && listAdjustCpuAbisForSharedUserLPw.size() > 0) {
                                    for (int size5 = listAdjustCpuAbisForSharedUserLPw.size() - 1; size5 >= 0; size5--) {
                                        try {
                                            this.mInstaller.rmdex(listAdjustCpuAbisForSharedUserLPw.get(size5), InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                                        } catch (Installer.InstallerException e8) {
                                        }
                                    }
                                }
                                sharedUserSetting.fixSeInfoLocked();
                            }
                            this.mPackageUsage.read(this.mPackages);
                            this.mCompilerStats.read();
                            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END, SystemClock.uptimeMillis());
                            sMtkSystemServerIns.addBootEvent("Android:PMS_scan_END");
                            Slog.i(TAG, "Time to scan packages: " + ((SystemClock.uptimeMillis() - jUptimeMillis) / 1000.0f) + " seconds");
                            boolean z3 = internalVersion.sdkVersion != this.mSdkVersion;
                            if (z3) {
                                Slog.i(TAG, "Platform changed from " + internalVersion.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for internal storage");
                            }
                            this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, z3, this.mPackages.values(), this.mPermissionCallback);
                            internalVersion.sdkVersion = this.mSdkVersion;
                            if (z2 || !(this.mPromoteSystemApps || this.mFirstBoot)) {
                                i = 1;
                            } else {
                                i = 1;
                                for (UserInfo userInfo : sUserManager.getUsers(true)) {
                                    this.mSettings.applyDefaultPreferredAppsLPw(this, userInfo.id);
                                    applyFactoryDefaultBrowserLPw(userInfo.id);
                                    primeDomainVerificationsLPw(userInfo.id);
                                }
                            }
                            final int i23 = StorageManager.isFileEncryptedNativeOrEmulated() ? i : 3;
                            final List<String> listReconcileAppsDataLI = reconcileAppsDataLI(StorageManager.UUID_PRIVATE_INTERNAL, 0, i23, true, true);
                            this.mPrepareAppDataFuture = SystemServerInitThreadPool.get().submit(new Runnable() {
                                @Override
                                public final void run() {
                                    PackageManagerService.lambda$new$0(this.f$0, listReconcileAppsDataLI, i23);
                                }
                            }, "prepareAppData");
                            if (this.mIsUpgrade && !z2) {
                                Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                                for (int i24 = 0; i24 < this.mSettings.mPackages.size(); i24++) {
                                    PackageSetting packageSettingValueAt2 = this.mSettings.mPackages.valueAt(i24);
                                    if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, packageSettingValueAt2.volumeUuid)) {
                                        clearAppDataLIF(packageSettingValueAt2.pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                                    }
                                }
                                internalVersion.fingerprint = Build.FINGERPRINT;
                            }
                            checkDefaultBrowser();
                            this.mExistingSystemPackages.clear();
                            this.mPromoteSystemApps = false;
                            internalVersion.databaseVersion = 3;
                            Trace.traceBegin(262144L, "write settings");
                            this.mSettings.writeLPr();
                            Trace.traceEnd(262144L);
                            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY, SystemClock.uptimeMillis());
                            sMtkSystemServerIns.addBootEvent("Android:PMS_READY");
                            if (this.mOnlyCore) {
                                this.mRequiredVerifierPackage = null;
                                this.mRequiredInstallerPackage = null;
                                this.mRequiredUninstallerPackage = null;
                                this.mIntentFilterVerifierComponent = null;
                                this.mIntentFilterVerifier = null;
                                this.mServicesSystemSharedLibraryPackageName = null;
                                this.mSharedSystemSharedLibraryPackageName = null;
                            } else {
                                this.mRequiredVerifierPackage = getRequiredButNotReallyRequiredVerifierLPr();
                                this.mRequiredInstallerPackage = getRequiredInstallerLPr();
                                this.mRequiredUninstallerPackage = getRequiredUninstallerLPr();
                                this.mIntentFilterVerifierComponent = getIntentFilterVerifierComponentNameLPr();
                                if (this.mIntentFilterVerifierComponent != null) {
                                    this.mIntentFilterVerifier = new IntentVerifierProxy(this.mContext, this.mIntentFilterVerifierComponent);
                                } else {
                                    this.mIntentFilterVerifier = null;
                                }
                                this.mServicesSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.services", -1);
                                this.mSharedSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.shared", -1);
                            }
                            this.mInstallerService = new PackageInstallerService(context, this);
                            Pair<ComponentName, String> instantAppResolverLPr = getInstantAppResolverLPr();
                            if (instantAppResolverLPr != null) {
                                if (DEBUG_INSTANT) {
                                    Slog.d(TAG, "Set ephemeral resolver: " + instantAppResolverLPr);
                                }
                                this.mInstantAppResolverConnection = new InstantAppResolverConnection(this.mContext, (ComponentName) instantAppResolverLPr.first, (String) instantAppResolverLPr.second);
                                this.mInstantAppResolverSettingsComponent = getInstantAppResolverSettingsLPr((ComponentName) instantAppResolverLPr.first);
                                str = null;
                            } else {
                                str = null;
                                this.mInstantAppResolverConnection = null;
                                this.mInstantAppResolverSettingsComponent = null;
                            }
                            updateInstantAppInstallerLocked(str);
                            HashMap map = new HashMap();
                            for (int i25 : UserManagerService.getInstance().getUserIds()) {
                                map.put(Integer.valueOf(i25), getInstalledPackages(0, i25).getList());
                            }
                            this.mDexManager.load(map);
                            if (this.mIsUpgrade) {
                                MetricsLogger.histogram((Context) null, "ota_package_manager_init_time", (int) (SystemClock.uptimeMillis() - jUptimeMillis));
                            }
                            Trace.traceBegin(262144L, "GC");
                            Runtime.getRuntime().gc();
                            Trace.traceEnd(262144L);
                            this.mInstaller.setWarnIfHeld(this.mPackages);
                            sPmsExt.initAfterScan(this.mSettings.mPackages);
                            Trace.traceEnd(262144L);
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            arrayMap = arrayMap2;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
        throw th;
    }

    public static void lambda$new$0(PackageManagerService packageManagerService, List list, int i) {
        TimingsTraceLog timingsTraceLog = new TimingsTraceLog("SystemServerTimingAsync", 262144L);
        timingsTraceLog.traceBegin("AppDataFixup");
        try {
            packageManagerService.mInstaller.fixupAppData(StorageManager.UUID_PRIVATE_INTERNAL, 3);
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Trouble fixing GIDs", e);
        }
        timingsTraceLog.traceEnd();
        timingsTraceLog.traceBegin("AppDataPrepare");
        if (list == null || list.isEmpty()) {
            return;
        }
        Iterator it = list.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            String str = (String) it.next();
            PackageParser.Package r4 = null;
            synchronized (packageManagerService.mPackages) {
                PackageSetting packageLPr = packageManagerService.mSettings.getPackageLPr(str);
                if (packageLPr != null && packageLPr.getInstalled(0)) {
                    r4 = packageLPr.pkg;
                }
            }
            if (r4 != null) {
                synchronized (packageManagerService.mInstallLock) {
                    packageManagerService.prepareAppDataAndMigrateLIF(r4, 0, i, true);
                }
                i2++;
            }
        }
        timingsTraceLog.traceEnd();
        Slog.i(TAG, "Deferred reconcileAppsData finished " + i2 + " packages");
    }

    private void decompressSystemApplications(List<String> list, int i) throws Throwable {
        for (int size = list.size() - 1; size >= 0; size--) {
            String str = list.get(size);
            if (this.mSettings.isDisabledSystemPackageLPr(str)) {
                list.remove(size);
            } else {
                PackageParser.Package r2 = this.mPackages.get(str);
                if (r2 == null) {
                    list.remove(size);
                } else {
                    PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                    if (packageSetting != null && packageSetting.getEnabled(0) == 3) {
                        list.remove(size);
                    } else {
                        if (DEBUG_COMPRESSION) {
                            Slog.i(TAG, "Uncompressing system stub; pkg: " + str);
                        }
                        File fileDecompressPackage = decompressPackage(r2);
                        if (fileDecompressPackage != null) {
                            try {
                                this.mSettings.disableSystemPackageLPw(str, true);
                                removePackageLI(r2, true);
                                scanPackageTracedLI(fileDecompressPackage, 0, i, 0L, (UserHandle) null);
                                packageSetting.setEnabled(0, 0, PLATFORM_PACKAGE_NAME);
                                list.remove(size);
                            } catch (PackageManagerException e) {
                                Slog.e(TAG, "Failed to parse uncompressed system package: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        for (int size2 = list.size() - 1; size2 >= 0; size2 += -1) {
            String str2 = list.get(size2);
            this.mSettings.mPackages.get(str2).setEnabled(2, 0, PLATFORM_PACKAGE_NAME);
            PackageManagerServiceUtils.logCriticalInfo(6, "Stub disabled; pkg: " + str2);
        }
    }

    private File decompressPackage(PackageParser.Package r14) throws Throwable {
        int iCopyNativeBinariesWithOverride;
        NativeLibraryHelper.Handle handleCreate;
        File[] compressedFiles = PackageManagerServiceUtils.getCompressedFiles(r14.codePath);
        if (compressedFiles == null || compressedFiles.length == 0) {
            if (DEBUG_COMPRESSION) {
                Slog.i(TAG, "No files to decompress: " + r14.baseCodePath);
            }
            return null;
        }
        File nextCodePath = getNextCodePath(Environment.getDataAppDirectory(null), r14.packageName);
        try {
            Os.mkdir(nextCodePath.getAbsolutePath(), 493);
            Os.chmod(nextCodePath.getAbsolutePath(), 493);
            int length = compressedFiles.length;
            iCopyNativeBinariesWithOverride = 1;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                try {
                    File file = compressedFiles[i];
                    String name = file.getName();
                    String strSubstring = name.substring(0, name.length() - COMPRESSED_EXTENSION.length());
                    int iDecompressFile = PackageManagerServiceUtils.decompressFile(file, new File(nextCodePath, strSubstring));
                    if (iDecompressFile != 1) {
                        try {
                            break;
                        } catch (ErrnoException e) {
                            e = e;
                            iCopyNativeBinariesWithOverride = iDecompressFile;
                            PackageManagerServiceUtils.logCriticalInfo(6, "Failed to decompress; pkg: " + r14.packageName + ", err: " + e.errno);
                        }
                    } else {
                        i++;
                        iCopyNativeBinariesWithOverride = iDecompressFile;
                    }
                } catch (ErrnoException e2) {
                    e = e2;
                }
            }
        } catch (ErrnoException e3) {
            e = e3;
            iCopyNativeBinariesWithOverride = 1;
        }
        if (iCopyNativeBinariesWithOverride == 1) {
            File file2 = new File(nextCodePath, "lib");
            try {
                handleCreate = NativeLibraryHelper.Handle.create(nextCodePath);
                try {
                    try {
                        iCopyNativeBinariesWithOverride = NativeLibraryHelper.copyNativeBinariesWithOverride(handleCreate, file2, (String) null);
                    } catch (IOException e4) {
                        PackageManagerServiceUtils.logCriticalInfo(6, "Failed to extract native libraries; pkg: " + r14.packageName);
                        iCopyNativeBinariesWithOverride = RequestStatus.SYS_ETIMEDOUT;
                    }
                } catch (Throwable th) {
                    th = th;
                    IoUtils.closeQuietly(handleCreate);
                    throw th;
                }
            } catch (IOException e5) {
                handleCreate = null;
            } catch (Throwable th2) {
                th = th2;
                handleCreate = null;
                IoUtils.closeQuietly(handleCreate);
                throw th;
            }
            IoUtils.closeQuietly(handleCreate);
        }
        if (iCopyNativeBinariesWithOverride == 1) {
            return nextCodePath;
        }
        if (nextCodePath == null || !nextCodePath.exists()) {
            return null;
        }
        removeCodePathLI(nextCodePath);
        return null;
    }

    private void updateInstantAppInstallerLocked(String str) {
        if (this.mInstantAppInstallerActivity != null && !this.mInstantAppInstallerActivity.getComponentName().getPackageName().equals(str)) {
            return;
        }
        setUpInstantAppInstallerActivityLP(getInstantAppInstallerLPr());
    }

    private static File preparePackageParserCache(boolean z) {
        if (Build.IS_ENG) {
            return null;
        }
        if (SystemProperties.getBoolean("pm.boot.disable_package_cache", false)) {
            Slog.i(TAG, "Disabling package parser cache due to system property.");
            return null;
        }
        File fileCreateDir = FileUtils.createDir(Environment.getDataSystemDirectory(), "package_cache");
        if (fileCreateDir == null) {
            return null;
        }
        if (z) {
            FileUtils.deleteContents(fileCreateDir);
        }
        File fileCreateDir2 = FileUtils.createDir(fileCreateDir, PACKAGE_PARSER_CACHE_VERSION);
        if (fileCreateDir2 == null) {
            Slog.wtf(TAG, "Cache directory cannot be created - wiping base dir " + fileCreateDir);
            FileUtils.deleteContentsAndDir(fileCreateDir);
            return null;
        }
        if (Build.IS_USERDEBUG && Build.VERSION.INCREMENTAL.startsWith("eng.")) {
            Slog.w(TAG, "Wiping cache directory because the system partition changed.");
            if (fileCreateDir2.lastModified() < new File(Environment.getRootDirectory(), "framework").lastModified()) {
                FileUtils.deleteContents(fileCreateDir);
                return FileUtils.createDir(fileCreateDir, PACKAGE_PARSER_CACHE_VERSION);
            }
            return fileCreateDir2;
        }
        return fileCreateDir2;
    }

    public boolean isFirstBoot() {
        return this.mFirstBoot;
    }

    public boolean isOnlyCoreApps() {
        return this.mOnlyCore;
    }

    public boolean isUpgrade() {
        return this.mIsUpgrade || SystemProperties.getBoolean("persist.pm.mock-upgrade", false);
    }

    private String getRequiredButNotReallyRequiredVerifierLPr() {
        List<ResolveInfo> listQueryIntentReceiversInternal = queryIntentReceiversInternal(new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION"), PACKAGE_MIME_TYPE, 1835008, 0, false);
        if (listQueryIntentReceiversInternal.size() == 1) {
            return listQueryIntentReceiversInternal.get(0).getComponentInfo().packageName;
        }
        if (listQueryIntentReceiversInternal.size() == 0) {
            Log.e(TAG, "There should probably be a verifier, but, none were found");
            return null;
        }
        throw new RuntimeException("There must be exactly one verifier; found " + listQueryIntentReceiversInternal);
    }

    private String getRequiredSharedLibraryLPr(String str, int i) {
        String str2;
        synchronized (this.mPackages) {
            SharedLibraryEntry sharedLibraryEntryLPr = getSharedLibraryEntryLPr(str, i);
            if (sharedLibraryEntryLPr == null) {
                throw new IllegalStateException("Missing required shared library:" + str);
            }
            str2 = sharedLibraryEntryLPr.apk;
        }
        return str2;
    }

    private String getRequiredInstallerLPr() {
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        if (listQueryIntentActivitiesInternal.size() == 1) {
            if (!listQueryIntentActivitiesInternal.get(0).activityInfo.applicationInfo.isPrivilegedApp()) {
                throw new RuntimeException("The installer must be a privileged app");
            }
            return listQueryIntentActivitiesInternal.get(0).getComponentInfo().packageName;
        }
        throw new RuntimeException("There must be exactly one installer; found " + listQueryIntentActivitiesInternal);
    }

    private String getRequiredUninstallerLPr() {
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(Uri.fromParts("package", "foo.bar", null));
        ResolveInfo resolveInfoResolveIntent = resolveIntent(intent, null, 1835008, 0);
        if (resolveInfoResolveIntent == null || this.mResolveActivity.name.equals(resolveInfoResolveIntent.getComponentInfo().name)) {
            throw new RuntimeException("There must be exactly one uninstaller; found " + resolveInfoResolveIntent);
        }
        return resolveInfoResolveIntent.getComponentInfo().packageName;
    }

    private ComponentName getIntentFilterVerifierComponentNameLPr() {
        List<ResolveInfo> listQueryIntentReceiversInternal = queryIntentReceiversInternal(new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION"), PACKAGE_MIME_TYPE, 1835008, 0, false);
        int size = listQueryIntentReceiversInternal.size();
        ResolveInfo resolveInfo = null;
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo2 = listQueryIntentReceiversInternal.get(i);
            if (checkPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", resolveInfo2.getComponentInfo().packageName, 0) == 0 && (resolveInfo == null || resolveInfo2.priority > resolveInfo.priority)) {
                resolveInfo = resolveInfo2;
            }
        }
        if (resolveInfo != null) {
            return resolveInfo.getComponentInfo().getComponentName();
        }
        Slog.w(TAG, "Intent filter verifier not found");
        return null;
    }

    public ComponentName getInstantAppResolverComponent() {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            Pair<ComponentName, String> instantAppResolverLPr = getInstantAppResolverLPr();
            if (instantAppResolverLPr == null) {
                return null;
            }
            return (ComponentName) instantAppResolverLPr.first;
        }
    }

    private Pair<ComponentName, String> getInstantAppResolverLPr() {
        String[] stringArray = this.mContext.getResources().getStringArray(R.array.config_biometric_protected_package_names);
        if (stringArray.length == 0 && !Build.IS_DEBUGGABLE) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Ephemeral resolver NOT found; empty package list");
            }
            return null;
        }
        List<ResolveInfo> listQueryIntentServicesInternal = queryIntentServicesInternal(new Intent("android.intent.action.RESOLVE_INSTANT_APP_PACKAGE"), null, 786432 | (!Build.IS_DEBUGGABLE ? 1048576 : 0), 0, Binder.getCallingUid(), false);
        int size = listQueryIntentServicesInternal.size();
        if (size == 0) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Ephemeral resolver NOT found; no matching intent filters");
            }
            return null;
        }
        ArraySet arraySet = new ArraySet(Arrays.asList(stringArray));
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = listQueryIntentServicesInternal.get(i);
            if (resolveInfo.serviceInfo != null) {
                String str = resolveInfo.serviceInfo.packageName;
                if (!arraySet.contains(str) && !Build.IS_DEBUGGABLE) {
                    if (DEBUG_INSTANT) {
                        Slog.d(TAG, "Ephemeral resolver not in allowed package list; pkg: " + str + ", info:" + resolveInfo);
                    }
                } else {
                    if (DEBUG_INSTANT) {
                        Slog.v(TAG, "Ephemeral resolver found; pkg: " + str + ", info:" + resolveInfo);
                    }
                    return new Pair<>(new ComponentName(str, resolveInfo.serviceInfo.name), "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
                }
            }
        }
        if (DEBUG_INSTANT) {
            Slog.v(TAG, "Ephemeral resolver NOT found");
        }
        return null;
    }

    private ActivityInfo getInstantAppInstallerLPr() {
        String[] strArr;
        if (Build.IS_ENG) {
            strArr = new String[]{"android.intent.action.INSTALL_INSTANT_APP_PACKAGE_TEST", "android.intent.action.INSTALL_INSTANT_APP_PACKAGE"};
        } else {
            strArr = new String[]{"android.intent.action.INSTALL_INSTANT_APP_PACKAGE"};
        }
        int i = 786944 | (!Build.IS_ENG ? 1048576 : 0);
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        int length = strArr.length;
        int i2 = 0;
        List<ResolveInfo> list = null;
        while (true) {
            if (i2 >= length) {
                break;
            }
            String str = strArr[i2];
            intent.setAction(str);
            List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, i, 0);
            if (listQueryIntentActivitiesInternal.isEmpty()) {
                if (DEBUG_INSTANT) {
                    Slog.d(TAG, "Instant App installer not found with " + str);
                }
                i2++;
                list = listQueryIntentActivitiesInternal;
            } else {
                list = listQueryIntentActivitiesInternal;
                break;
            }
        }
        Iterator<ResolveInfo> it = list.iterator();
        while (it.hasNext()) {
            if (checkPermission("android.permission.INSTALL_PACKAGES", it.next().activityInfo.packageName, 0) != 0 && !Build.IS_ENG) {
                it.remove();
            }
        }
        if (list.size() == 0) {
            return null;
        }
        if (list.size() == 1) {
            return (ActivityInfo) list.get(0).getComponentInfo();
        }
        throw new RuntimeException("There must be at most one ephemeral installer; found " + list);
    }

    private ComponentName getInstantAppResolverSettingsLPr(ComponentName componentName) {
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(new Intent("android.intent.action.INSTANT_APP_RESOLVER_SETTINGS").addCategory("android.intent.category.DEFAULT").setPackage(componentName.getPackageName()), null, 786432, 0);
        if (listQueryIntentActivitiesInternal.isEmpty()) {
            return null;
        }
        return listQueryIntentActivitiesInternal.get(0).getComponentInfo().getComponentName();
    }

    private void primeDomainVerificationsLPw(int i) {
        if (DEBUG_DOMAIN_VERIFICATION) {
            Slog.d(TAG, "Priming domain verifications in user " + i);
        }
        for (String str : SystemConfig.getInstance().getLinkedApps()) {
            PackageParser.Package r2 = this.mPackages.get(str);
            if (r2 != null) {
                if (!r2.isSystem()) {
                    Slog.w(TAG, "Non-system app '" + str + "' in sysconfig <app-link>");
                } else {
                    ArraySet<String> arraySet = null;
                    Iterator it = r2.activities.iterator();
                    while (it.hasNext()) {
                        for (PackageParser.ActivityIntentInfo activityIntentInfo : ((PackageParser.Activity) it.next()).intents) {
                            if (hasValidDomains(activityIntentInfo)) {
                                if (arraySet == null) {
                                    arraySet = new ArraySet<>();
                                }
                                arraySet.addAll(activityIntentInfo.getHostsList());
                            }
                        }
                    }
                    if (arraySet != null && arraySet.size() > 0) {
                        if (DEBUG_DOMAIN_VERIFICATION) {
                            Slog.v(TAG, "      + " + str);
                        }
                        this.mSettings.createIntentFilterVerificationIfNeededLPw(str, arraySet).setStatus(0);
                        this.mSettings.updateIntentFilterVerificationStatusLPw(str, 2, i);
                    } else {
                        Slog.w(TAG, "Sysconfig <app-link> package '" + str + "' does not handle web links");
                    }
                }
            } else {
                Slog.w(TAG, "Unknown package " + str + " in sysconfig <app-link>");
            }
        }
        scheduleWritePackageRestrictionsLocked(i);
        scheduleWriteSettingsLocked();
    }

    private void applyFactoryDefaultBrowserLPw(int i) {
        String string = this.mContext.getResources().getString(R.string.bg_user_sound_notification_button_switch_user);
        if (!TextUtils.isEmpty(string)) {
            if (this.mSettings.mPackages.get(string) == null) {
                Slog.e(TAG, "Product default browser app does not exist: " + string);
                string = null;
            } else {
                this.mSettings.setDefaultBrowserPackageNameLPw(string, i);
            }
        }
        if (string == null) {
            calculateDefaultBrowserLPw(i);
        }
    }

    private void calculateDefaultBrowserLPw(int i) {
        List<String> listResolveAllBrowserApps = resolveAllBrowserApps(i);
        this.mSettings.setDefaultBrowserPackageNameLPw(listResolveAllBrowserApps.size() == 1 ? listResolveAllBrowserApps.get(0) : null, i);
    }

    private List<String> resolveAllBrowserApps(int i) {
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, i);
        int size = listQueryIntentActivitiesInternal.size();
        ArrayList arrayList = new ArrayList(size);
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = listQueryIntentActivitiesInternal.get(i2);
            if (resolveInfo.activityInfo != null && resolveInfo.handleAllWebDataURI && (resolveInfo.activityInfo.applicationInfo.flags & 1) != 0 && !arrayList.contains(resolveInfo.activityInfo.packageName)) {
                arrayList.add(resolveInfo.activityInfo.packageName);
            }
        }
        return arrayList;
    }

    private boolean packageIsBrowser(String str, int i) {
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, i);
        int size = listQueryIntentActivitiesInternal.size();
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = listQueryIntentActivitiesInternal.get(i2);
            if (resolveInfo.priority >= 0 && str.equals(resolveInfo.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private void checkDefaultBrowser() {
        int iMyUserId = UserHandle.myUserId();
        String defaultBrowserPackageName = getDefaultBrowserPackageName(iMyUserId);
        if (defaultBrowserPackageName != null && getPackageInfo(defaultBrowserPackageName, 0, iMyUserId) == null) {
            Slog.w(TAG, "Default browser no longer installed: " + defaultBrowserPackageName);
            synchronized (this.mPackages) {
                applyFactoryDefaultBrowserLPw(iMyUserId);
            }
        }
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException) && !(e instanceof IllegalArgumentException)) {
                Slog.wtf(TAG, "Package Manager Crash", e);
            }
            throw e;
        }
    }

    static int[] appendInts(int[] iArr, int[] iArr2) {
        if (iArr2 == null) {
            return iArr;
        }
        if (iArr == null) {
            return iArr2;
        }
        for (int i : iArr2) {
            iArr = ArrayUtils.appendInt(iArr, i);
        }
        return iArr;
    }

    private boolean canViewInstantApps(int i, int i2) {
        ComponentName defaultHomeActivity;
        if (i < 10000 || this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS") == 0) {
            return true;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIEW_INSTANT_APPS") == 0 && (defaultHomeActivity = getDefaultHomeActivity(i2)) != null && isCallerSameApp(defaultHomeActivity.getPackageName(), i)) {
            return true;
        }
        return false;
    }

    private PackageInfo generatePackageInfo(final PackageSetting packageSetting, int i, final int i2) {
        Set<String> set;
        if (!sUserManager.exists(i2) || packageSetting == null || filterAppAccessLPr(packageSetting, Binder.getCallingUid(), i2)) {
            return null;
        }
        if ((i & 8192) != 0 && packageSetting.isSystem()) {
            i |= DumpState.DUMP_CHANGES;
        }
        int i3 = i;
        PackageUserState userState = packageSetting.readUserState(i2);
        PackageParser.Package r14 = packageSetting.pkg;
        if (r14 != null) {
            PermissionsState permissionsState = packageSetting.getPermissionsState();
            int[] iArrComputeGids = (i3 & 256) == 0 ? EMPTY_INT_ARRAY : permissionsState.computeGids(i2);
            Set<String> setEmptySet = ArrayUtils.isEmpty(r14.requestedPermissions) ? Collections.emptySet() : permissionsState.getPermissions(i2);
            if (userState.instantApp) {
                ArraySet arraySet = new ArraySet(setEmptySet);
                arraySet.removeIf(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return PackageManagerService.lambda$generatePackageInfo$1(this.f$0, i2, packageSetting, (String) obj);
                    }
                });
                set = arraySet;
            } else {
                set = setEmptySet;
            }
            PackageInfo packageInfoGeneratePackageInfo = PackageParser.generatePackageInfo(r14, iArrComputeGids, i3, packageSetting.firstInstallTime, packageSetting.lastUpdateTime, set, userState, i2);
            if (packageInfoGeneratePackageInfo == null) {
                return null;
            }
            ApplicationInfo applicationInfo = packageInfoGeneratePackageInfo.applicationInfo;
            String strResolveExternalPackageNameLPr = resolveExternalPackageNameLPr(r14);
            applicationInfo.packageName = strResolveExternalPackageNameLPr;
            packageInfoGeneratePackageInfo.packageName = strResolveExternalPackageNameLPr;
            return sPmsExt.updatePackageInfoForRemovable(packageInfoGeneratePackageInfo);
        }
        if ((i3 & 8192) == 0 || !userState.isAvailable(i3)) {
            return null;
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageSetting.name;
        packageInfo.setLongVersionCode(packageSetting.versionCode);
        packageInfo.sharedUserId = packageSetting.sharedUser != null ? packageSetting.sharedUser.name : null;
        packageInfo.firstInstallTime = packageSetting.firstInstallTime;
        packageInfo.lastUpdateTime = packageSetting.lastUpdateTime;
        ApplicationInfo applicationInfo2 = new ApplicationInfo();
        applicationInfo2.packageName = packageSetting.name;
        applicationInfo2.uid = UserHandle.getUid(i2, packageSetting.appId);
        applicationInfo2.primaryCpuAbi = packageSetting.primaryCpuAbiString;
        applicationInfo2.secondaryCpuAbi = packageSetting.secondaryCpuAbiString;
        applicationInfo2.setVersionCode(packageSetting.versionCode);
        applicationInfo2.flags = packageSetting.pkgFlags;
        applicationInfo2.privateFlags = packageSetting.pkgPrivateFlags;
        packageInfo.applicationInfo = PackageParser.generateApplicationInfo(applicationInfo2, i3, userState, i2);
        if (DEBUG_PACKAGE_INFO) {
            Log.v(TAG, "ps.pkg is n/a for [" + packageSetting.name + "]. Provides a minimum info.");
        }
        return packageInfo;
    }

    public static boolean lambda$generatePackageInfo$1(PackageManagerService packageManagerService, int i, PackageSetting packageSetting, String str) {
        BasePermission permissionTEMP = packageManagerService.mPermissionManager.getPermissionTEMP(str);
        if (permissionTEMP == null) {
            return true;
        }
        if (permissionTEMP.isInstant()) {
            return false;
        }
        EventLog.writeEvent(1397638484, "140256621", Integer.valueOf(UserHandle.getUid(i, packageSetting.appId)), str);
        return true;
    }

    public void checkPackageStartable(String str, int i) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        boolean zIsUserKeyUnlocked = StorageManager.isUserKeyUnlocked(i);
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null || filterAppAccessLPr(packageSetting, callingUid, i)) {
                throw new SecurityException("Package " + str + " was not found!");
            }
            if (!packageSetting.getInstalled(i)) {
                throw new SecurityException("Package " + str + " was not installed for user " + i + "!");
            }
            if (this.mSafeMode && !packageSetting.isSystem()) {
                throw new SecurityException("Package " + str + " not a system app!");
            }
            if (this.mFrozenPackages.contains(str)) {
                throw new SecurityException("Package " + str + " is currently frozen!");
            }
            if (!zIsUserKeyUnlocked && !packageSetting.pkg.applicationInfo.isEncryptionAware()) {
                throw new SecurityException("Package " + str + " is not encryption aware!");
            }
        }
    }

    public boolean isPackageAvailable(String str, int i) {
        PackageUserState userState;
        if (!sUserManager.exists(i)) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, false, false, "is package available");
        synchronized (this.mPackages) {
            PackageParser.Package r9 = this.mPackages.get(str);
            if (r9 != null) {
                PackageSetting packageSetting = (PackageSetting) r9.mExtras;
                if (filterAppAccessLPr(packageSetting, callingUid, i)) {
                    return false;
                }
                if (packageSetting != null && (userState = packageSetting.readUserState(i)) != null) {
                    return PackageParser.isAvailable(userState);
                }
            }
            return false;
        }
    }

    public PackageInfo getPackageInfo(String str, int i, int i2) {
        return getPackageInfoInternal(str, -1L, i, Binder.getCallingUid(), i2);
    }

    public PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage, int i, int i2) {
        return getPackageInfoInternal(versionedPackage.getPackageName(), versionedPackage.getLongVersionCode(), i, Binder.getCallingUid(), i2);
    }

    private PackageInfo getPackageInfoInternal(String str, long j, int i, int i2, int i3) {
        PackageSetting disabledSystemPkgLPr;
        if (!sUserManager.exists(i3)) {
            return null;
        }
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i3, str);
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i3, false, false, "get package info");
        synchronized (this.mPackages) {
            String strResolveInternalPackageNameLPr = resolveInternalPackageNameLPr(str, j);
            boolean z = (2097152 & iUpdateFlagsForPackage) != 0;
            if (z && (disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(strResolveInternalPackageNameLPr)) != null) {
                if (filterSharedLibPackageLPr(disabledSystemPkgLPr, i2, i3, iUpdateFlagsForPackage)) {
                    return null;
                }
                if (filterAppAccessLPr(disabledSystemPkgLPr, i2, i3)) {
                    return null;
                }
                return generatePackageInfo(disabledSystemPkgLPr, iUpdateFlagsForPackage, i3);
            }
            PackageParser.Package r11 = this.mPackages.get(strResolveInternalPackageNameLPr);
            if (z && r11 != null && !isSystemApp(r11)) {
                return null;
            }
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getPackageInfo " + strResolveInternalPackageNameLPr + ": " + r11);
            }
            if (r11 != null) {
                PackageSetting packageSetting = (PackageSetting) r11.mExtras;
                if (filterSharedLibPackageLPr(packageSetting, i2, i3, iUpdateFlagsForPackage)) {
                    return null;
                }
                if (packageSetting == null || !filterAppAccessLPr(packageSetting, i2, i3)) {
                    return generatePackageInfo((PackageSetting) r11.mExtras, iUpdateFlagsForPackage, i3);
                }
                return null;
            }
            if (z || (4202496 & iUpdateFlagsForPackage) == 0) {
                return null;
            }
            PackageSetting packageSetting2 = this.mSettings.mPackages.get(strResolveInternalPackageNameLPr);
            if (packageSetting2 == null) {
                return null;
            }
            if (filterSharedLibPackageLPr(packageSetting2, i2, i3, iUpdateFlagsForPackage)) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting2, i2, i3)) {
                return null;
            }
            return generatePackageInfo(packageSetting2, iUpdateFlagsForPackage, i3);
        }
    }

    private boolean isComponentVisibleToInstantApp(ComponentName componentName) {
        if (isComponentVisibleToInstantApp(componentName, 1) || isComponentVisibleToInstantApp(componentName, 3) || isComponentVisibleToInstantApp(componentName, 4)) {
            return true;
        }
        return false;
    }

    private boolean isComponentVisibleToInstantApp(ComponentName componentName, int i) {
        if (i != 1) {
            if (i != 2) {
                if (i != 3) {
                    if (i != 4) {
                        if (i == 0) {
                            return isComponentVisibleToInstantApp(componentName);
                        }
                        return false;
                    }
                    PackageParser.Provider provider = (PackageParser.Provider) this.mProviders.mProviders.get(componentName);
                    return (provider == null || (provider.info.flags & 1048576) == 0) ? false : true;
                }
                PackageParser.Service service = (PackageParser.Service) this.mServices.mServices.get(componentName);
                return (service == null || (service.info.flags & 1048576) == 0) ? false : true;
            }
            PackageParser.Activity activity = (PackageParser.Activity) this.mReceivers.mActivities.get(componentName);
            if (activity == null) {
                return false;
            }
            return ((activity.info.flags & 1048576) != 0) && !((activity.info.flags & 2097152) == 0);
        }
        PackageParser.Activity activity2 = (PackageParser.Activity) this.mActivities.mActivities.get(componentName);
        if (activity2 == null) {
            return false;
        }
        return ((activity2.info.flags & 1048576) != 0) && ((activity2.info.flags & 2097152) == 0);
    }

    private boolean filterAppAccessLPr(PackageSetting packageSetting, int i, ComponentName componentName, int i2, int i3) {
        if (Process.isIsolated(i)) {
            i = this.mIsolatedOwners.get(i);
        }
        boolean z = getInstantAppPackageName(i) != null;
        if (packageSetting == null) {
            return z;
        }
        if (isCallerSameApp(packageSetting.name, i)) {
            return false;
        }
        if (z) {
            if (packageSetting.getInstantApp(i3)) {
                return true;
            }
            if (componentName != null) {
                PackageParser.Instrumentation instrumentation = this.mInstrumentation.get(componentName);
                if (instrumentation != null && isCallerSameApp(instrumentation.info.targetPackage, i)) {
                    return false;
                }
                return !isComponentVisibleToInstantApp(componentName, i2);
            }
            return !packageSetting.pkg.visibleToInstantApps;
        }
        if (!packageSetting.getInstantApp(i3) || canViewInstantApps(i, i3)) {
            return false;
        }
        if (componentName != null) {
            return true;
        }
        return !this.mInstantAppRegistry.isInstantAccessGranted(i3, UserHandle.getAppId(i), packageSetting.appId);
    }

    private boolean filterAppAccessLPr(PackageSetting packageSetting, int i, int i2) {
        return filterAppAccessLPr(packageSetting, i, null, 0, i2);
    }

    private boolean filterSharedLibPackageLPr(PackageSetting packageSetting, int i, int i2, int i3) {
        SharedLibraryEntry sharedLibraryEntryLPr;
        int iIndexOf;
        int appId;
        if (((i3 & 67108864) != 0 && ((appId = UserHandle.getAppId(i)) == 1000 || appId == 2000 || appId == 0 || checkUidPermission("android.permission.INSTALL_PACKAGES", i) == 0)) || packageSetting == null || packageSetting.pkg == null || !packageSetting.pkg.applicationInfo.isStaticSharedLibrary() || (sharedLibraryEntryLPr = getSharedLibraryEntryLPr(packageSetting.pkg.staticSharedLibName, packageSetting.pkg.staticSharedLibVersion)) == null) {
            return false;
        }
        String[] packagesForUid = getPackagesForUid(UserHandle.getUid(i2, UserHandle.getAppId(i)));
        if (packagesForUid == null) {
            return true;
        }
        for (String str : packagesForUid) {
            if (packageSetting.name.equals(str)) {
                return false;
            }
            PackageSetting packageLPr = this.mSettings.getPackageLPr(str);
            if (packageLPr != null && (iIndexOf = ArrayUtils.indexOf(packageLPr.usesStaticLibraries, sharedLibraryEntryLPr.info.getName())) >= 0 && packageLPr.pkg.usesStaticLibrariesVersions[iIndexOf] == sharedLibraryEntryLPr.info.getLongVersion()) {
                return false;
            }
        }
        return true;
    }

    public String[] currentToCanonicalPackageNames(String[] strArr) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return strArr;
        }
        String[] strArr2 = new String[strArr.length];
        synchronized (this.mPackages) {
            int userId = UserHandle.getUserId(callingUid);
            boolean zCanViewInstantApps = canViewInstantApps(callingUid, userId);
            for (int length = strArr.length - 1; length >= 0; length--) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(strArr[length]);
                boolean z = false;
                if (packageSetting != null && packageSetting.realName != null && (!packageSetting.getInstantApp(userId) || zCanViewInstantApps || this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), packageSetting.appId))) {
                    z = true;
                }
                strArr2[length] = z ? packageSetting.realName : strArr[length];
            }
        }
        return strArr2;
    }

    public String[] canonicalToCurrentPackageNames(String[] strArr) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return strArr;
        }
        String[] strArr2 = new String[strArr.length];
        synchronized (this.mPackages) {
            int userId = UserHandle.getUserId(callingUid);
            boolean zCanViewInstantApps = canViewInstantApps(callingUid, userId);
            for (int length = strArr.length - 1; length >= 0; length--) {
                String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(strArr[length]);
                boolean z = false;
                if (renamedPackageLPr != null) {
                    PackageSetting packageSetting = this.mSettings.mPackages.get(strArr[length]);
                    if (!(packageSetting != null && packageSetting.getInstantApp(userId)) || zCanViewInstantApps || this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), packageSetting.appId)) {
                        z = true;
                    }
                }
                if (!z) {
                    renamedPackageLPr = strArr[length];
                }
                strArr2[length] = renamedPackageLPr;
            }
        }
        return strArr2;
    }

    public int getPackageUid(String str, int i, int i2) {
        PackageSetting packageSetting;
        if (!sUserManager.exists(i2)) {
            return -1;
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i2, str);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "getPackageUid");
        synchronized (this.mPackages) {
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 != null && r3.isMatch(iUpdateFlagsForPackage)) {
                if (filterAppAccessLPr((PackageSetting) r3.mExtras, callingUid, i2)) {
                    return -1;
                }
                return UserHandle.getUid(i2, r3.applicationInfo.uid);
            }
            if ((4202496 & iUpdateFlagsForPackage) == 0 || (packageSetting = this.mSettings.mPackages.get(str)) == null || !packageSetting.isMatch(iUpdateFlagsForPackage) || filterAppAccessLPr(packageSetting, callingUid, i2)) {
                return -1;
            }
            return UserHandle.getUid(i2, packageSetting.appId);
        }
    }

    public int[] getPackageGids(String str, int i, int i2) {
        PackageSetting packageSetting;
        if (!sUserManager.exists(i2)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i2, str);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "getPackageGids");
        synchronized (this.mPackages) {
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 != null && r3.isMatch(iUpdateFlagsForPackage)) {
                PackageSetting packageSetting2 = (PackageSetting) r3.mExtras;
                if (filterAppAccessLPr(packageSetting2, callingUid, i2)) {
                    return null;
                }
                return packageSetting2.getPermissionsState().computeGids(i2);
            }
            if ((4202496 & iUpdateFlagsForPackage) == 0 || (packageSetting = this.mSettings.mPackages.get(str)) == null || !packageSetting.isMatch(iUpdateFlagsForPackage) || filterAppAccessLPr(packageSetting, callingUid, i2)) {
                return null;
            }
            return packageSetting.getPermissionsState().computeGids(i2);
        }
    }

    public PermissionInfo getPermissionInfo(String str, String str2, int i) {
        return this.mPermissionManager.getPermissionInfo(str, str2, i, getCallingUid());
    }

    public ParceledListSlice<PermissionInfo> queryPermissionsByGroup(String str, int i) {
        List<PermissionInfo> permissionInfoByGroup = this.mPermissionManager.getPermissionInfoByGroup(str, i, getCallingUid());
        if (permissionInfoByGroup == null) {
            return null;
        }
        return new ParceledListSlice<>(permissionInfoByGroup);
    }

    public PermissionGroupInfo getPermissionGroupInfo(String str, int i) {
        return this.mPermissionManager.getPermissionGroupInfo(str, i, getCallingUid());
    }

    public ParceledListSlice<PermissionGroupInfo> getAllPermissionGroups(int i) {
        List<PermissionGroupInfo> allPermissionGroups = this.mPermissionManager.getAllPermissionGroups(i, getCallingUid());
        return allPermissionGroups == null ? ParceledListSlice.emptyList() : new ParceledListSlice<>(allPermissionGroups);
    }

    private ApplicationInfo generateApplicationInfoFromSettingsLPw(String str, int i, int i2, int i3) {
        PackageSetting packageSetting;
        if (!sUserManager.exists(i3) || (packageSetting = this.mSettings.mPackages.get(str)) == null || filterSharedLibPackageLPr(packageSetting, i2, i3, i) || filterAppAccessLPr(packageSetting, i2, i3)) {
            return null;
        }
        if (packageSetting.pkg == null) {
            PackageInfo packageInfoGeneratePackageInfo = generatePackageInfo(packageSetting, i, i3);
            if (packageInfoGeneratePackageInfo != null) {
                return packageInfoGeneratePackageInfo.applicationInfo;
            }
            return null;
        }
        ApplicationInfo applicationInfoGenerateApplicationInfo = PackageParser.generateApplicationInfo(packageSetting.pkg, i, packageSetting.readUserState(i3), i3);
        if (applicationInfoGenerateApplicationInfo != null) {
            applicationInfoGenerateApplicationInfo.packageName = resolveExternalPackageNameLPr(packageSetting.pkg);
        }
        return applicationInfoGenerateApplicationInfo;
    }

    public ApplicationInfo getApplicationInfo(String str, int i, int i2) {
        return getApplicationInfoInternal(str, i, Binder.getCallingUid(), i2);
    }

    private ApplicationInfo getApplicationInfoInternal(String str, int i, int i2, int i3) {
        if (!sUserManager.exists(i3)) {
            return null;
        }
        int iUpdateFlagsForApplication = updateFlagsForApplication(i, i3, str);
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i3, false, false, "get application info");
        synchronized (this.mPackages) {
            String strResolveInternalPackageNameLPr = resolveInternalPackageNameLPr(str, -1L);
            PackageParser.Package r2 = this.mPackages.get(strResolveInternalPackageNameLPr);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getApplicationInfo " + strResolveInternalPackageNameLPr + ": " + r2);
            }
            if (r2 != null) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(strResolveInternalPackageNameLPr);
                if (packageSetting == null) {
                    return null;
                }
                if (filterSharedLibPackageLPr(packageSetting, i2, i3, iUpdateFlagsForApplication)) {
                    return null;
                }
                if (filterAppAccessLPr(packageSetting, i2, i3)) {
                    return null;
                }
                ApplicationInfo applicationInfoGenerateApplicationInfo = PackageParser.generateApplicationInfo(r2, iUpdateFlagsForApplication, packageSetting.readUserState(i3), i3);
                if (applicationInfoGenerateApplicationInfo != null) {
                    applicationInfoGenerateApplicationInfo.packageName = resolveExternalPackageNameLPr(r2);
                }
                return sPmsExt.updateApplicationInfoForRemovable(applicationInfoGenerateApplicationInfo);
            }
            if (!PLATFORM_PACKAGE_NAME.equals(strResolveInternalPackageNameLPr) && !"system".equals(strResolveInternalPackageNameLPr)) {
                if ((4202496 & iUpdateFlagsForApplication) == 0) {
                    return null;
                }
                return sPmsExt.updateApplicationInfoForRemovable(generateApplicationInfoFromSettingsLPw(strResolveInternalPackageNameLPr, iUpdateFlagsForApplication, i2, i3));
            }
            return this.mAndroidApplication;
        }
    }

    private String normalizePackageNameLPr(String str) {
        String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(str);
        return renamedPackageLPr != null ? renamedPackageLPr : str;
    }

    public void deletePreloadsFileCache() {
        if (!UserHandle.isSameApp(Binder.getCallingUid(), 1000)) {
            throw new SecurityException("Only system or settings may call deletePreloadsFileCache");
        }
        File dataPreloadsFileCacheDirectory = Environment.getDataPreloadsFileCacheDirectory();
        Slog.i(TAG, "Deleting preloaded file cache " + dataPreloadsFileCacheDirectory);
        FileUtils.deleteContents(dataPreloadsFileCacheDirectory);
    }

    public void freeStorageAndNotify(final String str, final long j, final int i, final IPackageDataObserver iPackageDataObserver) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                PackageManagerService.lambda$freeStorageAndNotify$2(this.f$0, str, j, i, iPackageDataObserver);
            }
        });
    }

    public static void lambda$freeStorageAndNotify$2(PackageManagerService packageManagerService, String str, long j, int i, IPackageDataObserver iPackageDataObserver) {
        boolean z;
        try {
            packageManagerService.freeStorage(str, j, i);
            z = true;
        } catch (IOException e) {
            Slog.w(TAG, e);
            z = false;
        }
        if (iPackageDataObserver != null) {
            try {
                iPackageDataObserver.onRemoveCompleted((String) null, z);
            } catch (RemoteException e2) {
                Slog.w(TAG, e2);
            }
        }
    }

    public void freeStorage(final String str, final long j, final int i, final IntentSender intentSender) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", TAG);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                PackageManagerService.lambda$freeStorage$3(this.f$0, str, j, i, intentSender);
            }
        });
    }

    public static void lambda$freeStorage$3(PackageManagerService packageManagerService, String str, long j, int i, IntentSender intentSender) {
        int i2;
        try {
            packageManagerService.freeStorage(str, j, i);
            i2 = 1;
        } catch (IOException e) {
            Slog.w(TAG, e);
            i2 = 0;
        }
        int i3 = i2;
        if (intentSender != null) {
            try {
                intentSender.sendIntent(null, i3, null, null, null);
            } catch (IntentSender.SendIntentException e2) {
                Slog.w(TAG, e2);
            }
        }
    }

    public void freeStorage(String str, long j, int i) throws IOException {
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        File fileFindPathForUuid = storageManager.findPathForUuid(str);
        if (fileFindPathForUuid.getUsableSpace() >= j) {
            return;
        }
        if (ENABLE_FREE_CACHE_V2) {
            boolean zEquals = Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, str);
            boolean z = (i & 1) != 0;
            long storageCacheBytes = storageManager.getStorageCacheBytes(fileFindPathForUuid, i);
            if (zEquals && (z || SystemProperties.getBoolean("persist.sys.preloads.file_cache_expired", false))) {
                deletePreloadsFileCache();
                if (fileFindPathForUuid.getUsableSpace() >= j) {
                    return;
                }
            }
            if (zEquals && z) {
                FileUtils.deleteContents(this.mCacheDir);
                if (fileFindPathForUuid.getUsableSpace() >= j) {
                    return;
                }
            }
            try {
                this.mInstaller.freeCache(str, j, storageCacheBytes, 8192);
            } catch (Installer.InstallerException e) {
            }
            if (fileFindPathForUuid.getUsableSpace() >= j) {
                return;
            }
            if (zEquals && pruneUnusedStaticSharedLibraries(j, Settings.Global.getLong(this.mContext.getContentResolver(), "unused_static_shared_lib_min_cache_period", 7200000L))) {
                return;
            }
            if (zEquals && this.mInstantAppRegistry.pruneInstalledInstantApps(j, Settings.Global.getLong(this.mContext.getContentResolver(), "installed_instant_app_min_cache_period", UnixCalendar.WEEK_IN_MILLIS))) {
                return;
            }
            try {
                this.mInstaller.freeCache(str, j, storageCacheBytes, 24576);
            } catch (Installer.InstallerException e2) {
            }
            if (fileFindPathForUuid.getUsableSpace() >= j) {
                return;
            }
            if (zEquals && this.mInstantAppRegistry.pruneUninstalledInstantApps(j, Settings.Global.getLong(this.mContext.getContentResolver(), "uninstalled_instant_app_min_cache_period", UnixCalendar.WEEK_IN_MILLIS))) {
                return;
            }
        } else {
            try {
                this.mInstaller.freeCache(str, j, 0L, 0);
            } catch (Installer.InstallerException e3) {
            }
            if (fileFindPathForUuid.getUsableSpace() >= j) {
                return;
            }
        }
        throw new IOException("Failed to free " + j + " on storage device at " + fileFindPathForUuid);
    }

    private boolean pruneUnusedStaticSharedLibraries(long j, long j2) throws IOException {
        ArrayList arrayList;
        int i;
        File fileFindPathForUuid = ((StorageManager) this.mContext.getSystemService(StorageManager.class)).findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.mPackages) {
            sUserManager.getUserIds();
            int size = this.mSharedLibraries.size();
            arrayList = null;
            for (int i2 = 0; i2 < size; i2++) {
                LongSparseArray<SharedLibraryEntry> longSparseArrayValueAt = this.mSharedLibraries.valueAt(i2);
                if (longSparseArrayValueAt != null) {
                    int size2 = longSparseArrayValueAt.size();
                    ArrayList arrayList2 = arrayList;
                    int i3 = 0;
                    while (i3 < size2) {
                        SharedLibraryInfo sharedLibraryInfo = longSparseArrayValueAt.valueAt(i3).info;
                        if (!sharedLibraryInfo.isStatic()) {
                            break;
                        }
                        VersionedPackage declaringPackage = sharedLibraryInfo.getDeclaringPackage();
                        String strResolveInternalPackageNameLPr = resolveInternalPackageNameLPr(declaringPackage.getPackageName(), declaringPackage.getLongVersionCode());
                        PackageSetting packageLPr = this.mSettings.getPackageLPr(strResolveInternalPackageNameLPr);
                        if (packageLPr == null || jCurrentTimeMillis - packageLPr.lastUpdateTime < j2) {
                            i = i3;
                        } else {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList();
                            }
                            i = i3;
                            arrayList2.add(new VersionedPackage(strResolveInternalPackageNameLPr, declaringPackage.getLongVersionCode()));
                        }
                        i3 = i + 1;
                    }
                    arrayList = arrayList2;
                }
            }
        }
        if (arrayList != null) {
            int size3 = arrayList.size();
            for (int i4 = 0; i4 < size3; i4++) {
                VersionedPackage versionedPackage = (VersionedPackage) arrayList.get(i4);
                if (deletePackageX(versionedPackage.getPackageName(), versionedPackage.getLongVersionCode(), 0, 2) == 1 && fileFindPathForUuid.getUsableSpace() >= j) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private int updateFlags(int i, int i2) {
        if ((i & 786432) == 0) {
            if (getUserManagerInternal().isUserUnlockingOrUnlocked(i2)) {
                return i | 786432;
            }
            return i | 524288;
        }
        return i;
    }

    private UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    private ActivityManagerInternal getActivityManagerInternal() {
        if (this.mActivityManagerInternal == null) {
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
        return this.mActivityManagerInternal;
    }

    private DeviceIdleController.LocalService getDeviceIdleController() {
        if (this.mDeviceIdleController == null) {
            this.mDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        }
        return this.mDeviceIdleController;
    }

    private int updateFlagsForPackage(int i, int i2, Object obj) {
        int i3;
        boolean z = UserHandle.getCallingUserId() == 0;
        boolean z2 = (269492224 & i) == 0 ? false : (i & 15) == 0 || (269221888 & i) != 0;
        if ((i & DumpState.DUMP_CHANGES) != 0) {
            this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i2, false, false, !isRecentsAccessingChildProfiles(Binder.getCallingUid(), i2), "MATCH_ANY_USER flag requires INTERACT_ACROSS_USERS permission at " + Debug.getCallers(5));
        } else {
            if ((i & 8192) != 0 && z && sUserManager.hasManagedProfile(0)) {
                i3 = i | DumpState.DUMP_CHANGES;
            }
            if (DEBUG_TRIAGED_MISSING && Binder.getCallingUid() == 1000 && !z2) {
                Log.w(TAG, "Caller hasn't been triaged for missing apps; they asked about " + obj + " with flags 0x" + Integer.toHexString(i3), new Throwable());
            }
            return updateFlags(i3, i2);
        }
        i3 = i;
        if (DEBUG_TRIAGED_MISSING) {
            Log.w(TAG, "Caller hasn't been triaged for missing apps; they asked about " + obj + " with flags 0x" + Integer.toHexString(i3), new Throwable());
        }
        return updateFlags(i3, i2);
    }

    private int updateFlagsForApplication(int i, int i2, Object obj) {
        return updateFlagsForPackage(i, i2, obj);
    }

    private int updateFlagsForComponent(int i, int i2, Object obj) {
        if ((obj instanceof Intent) && (((Intent) obj).getFlags() & 256) != 0) {
            i |= 268435456;
        }
        boolean z = true;
        if ((269221888 & i) == 0) {
            z = false;
        }
        if (DEBUG_TRIAGED_MISSING && Binder.getCallingUid() == 1000 && !z) {
            Log.w(TAG, "Caller hasn't been triaged for missing apps; they asked about " + obj + " with flags 0x" + Integer.toHexString(i), new Throwable());
        }
        return updateFlags(i, i2);
    }

    private Intent updateIntentForResolve(Intent intent) {
        if (intent.getSelector() != null) {
            intent = intent.getSelector();
        }
        if (DEBUG_PREFERRED) {
            intent.addFlags(8);
        }
        return intent;
    }

    int updateFlagsForResolve(int i, int i2, Intent intent, int i3) {
        return updateFlagsForResolve(i, i2, intent, i3, false, false);
    }

    int updateFlagsForResolve(int i, int i2, Intent intent, int i3, boolean z) {
        return updateFlagsForResolve(i, i2, intent, i3, z, false);
    }

    int updateFlagsForResolve(int i, int i2, Intent intent, int i3, boolean z, boolean z2) {
        int i4;
        if (this.mSafeMode) {
            i |= 1048576;
        }
        if (getInstantAppPackageName(i3) != null) {
            if (z2) {
                i |= 33554432;
            }
            i4 = i | DumpState.DUMP_SERVICE_PERMISSIONS | DumpState.DUMP_VOLUMES;
        } else {
            boolean z3 = false;
            boolean z4 = (i & DumpState.DUMP_VOLUMES) != 0;
            if (z || (z4 && canViewInstantApps(i3, i2))) {
                z3 = true;
            }
            i4 = i & (-50331649);
            if (!z3) {
                i4 &= -8388609;
            }
        }
        return updateFlagsForComponent(i4, i2, intent);
    }

    public ActivityInfo getActivityInfo(ComponentName componentName, int i, int i2) {
        return getActivityInfoInternal(componentName, i, Binder.getCallingUid(), i2);
    }

    private ActivityInfo getActivityInfoInternal(ComponentName componentName, int i, int i2, int i3) {
        if (!sUserManager.exists(i3)) {
            return null;
        }
        int iUpdateFlagsForComponent = updateFlagsForComponent(i, i3, componentName);
        if (!isRecentsAccessingChildProfiles(Binder.getCallingUid(), i3)) {
            this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i3, false, false, "get activity info");
        }
        synchronized (this.mPackages) {
            PackageParser.Activity activity = (PackageParser.Activity) this.mActivities.mActivities.get(componentName);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getActivityInfo " + componentName + ": " + activity);
            }
            if (activity != null && this.mSettings.isEnabledAndMatchLPr(activity.info, iUpdateFlagsForComponent, i3)) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
                if (packageSetting == null) {
                    return null;
                }
                if (filterAppAccessLPr(packageSetting, i2, componentName, 1, i3)) {
                    return null;
                }
                return PackageParser.generateActivityInfo(activity, iUpdateFlagsForComponent, packageSetting.readUserState(i3), i3);
            }
            if (!this.mResolveComponentName.equals(componentName)) {
                return null;
            }
            return PackageParser.generateActivityInfo(this.mResolveActivity, iUpdateFlagsForComponent, new PackageUserState(), i3);
        }
    }

    private boolean isRecentsAccessingChildProfiles(int i, int i2) {
        if (!getActivityManagerInternal().isCallerRecents(i)) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int userId = UserHandle.getUserId(i);
            if (ActivityManager.getCurrentUser() != userId) {
                return false;
            }
            return sUserManager.isSameProfileGroup(userId, i2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean activitySupportsIntent(ComponentName componentName, Intent intent, String str) {
        synchronized (this.mPackages) {
            if (componentName.equals(this.mResolveComponentName)) {
                return true;
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            PackageParser.Activity activity = (PackageParser.Activity) this.mActivities.mActivities.get(componentName);
            if (activity == null) {
                return false;
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
            if (packageSetting == null) {
                return false;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, componentName, 1, userId)) {
                return false;
            }
            for (int i = 0; i < activity.intents.size(); i++) {
                if (((PackageParser.ActivityIntentInfo) activity.intents.get(i)).match(intent.getAction(), str, intent.getScheme(), intent.getData(), intent.getCategories(), TAG) >= 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName, int i, int i2) {
        if (!sUserManager.exists(i2)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForComponent = updateFlagsForComponent(i, i2, componentName);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "get receiver info");
        synchronized (this.mPackages) {
            PackageParser.Activity activity = (PackageParser.Activity) this.mReceivers.mActivities.get(componentName);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getReceiverInfo " + componentName + ": " + activity);
            }
            if (activity == null || !this.mSettings.isEnabledAndMatchLPr(activity.info, iUpdateFlagsForComponent, i2)) {
                return null;
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
            if (packageSetting == null) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, componentName, 2, i2)) {
                return null;
            }
            return PackageParser.generateActivityInfo(activity, iUpdateFlagsForComponent, packageSetting.readUserState(i2), i2);
        }
    }

    public ParceledListSlice<SharedLibraryInfo> getSharedLibraries(String str, int i, int i2) {
        ParceledListSlice<SharedLibraryInfo> parceledListSlice;
        boolean z;
        if (!sUserManager.exists(i2)) {
            return null;
        }
        Preconditions.checkArgumentNonnegative(i2, "userId must be >= 0");
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i2, null);
        int i3 = 0;
        boolean z2 = this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0 || canRequestPackageInstallsInternal(str, 67108864, i2, false) || this.mContext.checkCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES") == 0;
        synchronized (this.mPackages) {
            int size = this.mSharedLibraries.size();
            ArrayList arrayList = null;
            int i4 = 0;
            while (i4 < size) {
                LongSparseArray<SharedLibraryEntry> longSparseArrayValueAt = this.mSharedLibraries.valueAt(i4);
                if (longSparseArrayValueAt == null) {
                    z = z2;
                } else {
                    int size2 = longSparseArrayValueAt.size();
                    ArrayList arrayList2 = arrayList;
                    int i5 = i3;
                    while (i5 < size2) {
                        SharedLibraryInfo sharedLibraryInfo = longSparseArrayValueAt.valueAt(i5).info;
                        if (!z2 && sharedLibraryInfo.isStatic()) {
                            break;
                        }
                        boolean z3 = z2;
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            if (getPackageInfoVersioned(sharedLibraryInfo.getDeclaringPackage(), iUpdateFlagsForPackage | 67108864, i2) == null) {
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                            } else {
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                                SharedLibraryInfo sharedLibraryInfo2 = new SharedLibraryInfo(sharedLibraryInfo.getName(), sharedLibraryInfo.getLongVersion(), sharedLibraryInfo.getType(), sharedLibraryInfo.getDeclaringPackage(), getPackagesUsingSharedLibraryLPr(sharedLibraryInfo, iUpdateFlagsForPackage, i2));
                                if (arrayList2 == null) {
                                    arrayList2 = new ArrayList();
                                }
                                arrayList2.add(sharedLibraryInfo2);
                            }
                            i5++;
                            z2 = z3;
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    }
                    z = z2;
                    arrayList = arrayList2;
                }
                i4++;
                z2 = z;
                i3 = 0;
            }
            parceledListSlice = arrayList != null ? new ParceledListSlice<>(arrayList) : null;
        }
        return parceledListSlice;
    }

    private List<VersionedPackage> getPackagesUsingSharedLibraryLPr(SharedLibraryInfo sharedLibraryInfo, int i, int i2) {
        int size = this.mSettings.mPackages.size();
        ArrayList arrayList = null;
        for (int i3 = 0; i3 < size; i3++) {
            PackageSetting packageSettingValueAt = this.mSettings.mPackages.valueAt(i3);
            if (packageSettingValueAt != null && packageSettingValueAt.getUserState().get(i2).isAvailable(i)) {
                String name = sharedLibraryInfo.getName();
                if (sharedLibraryInfo.isStatic()) {
                    int iIndexOf = ArrayUtils.indexOf(packageSettingValueAt.usesStaticLibraries, name);
                    if (iIndexOf >= 0 && packageSettingValueAt.usesStaticLibrariesVersions[iIndexOf] == sharedLibraryInfo.getLongVersion()) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        String str = packageSettingValueAt.name;
                        if (packageSettingValueAt.pkg != null && packageSettingValueAt.pkg.applicationInfo.isStaticSharedLibrary()) {
                            str = packageSettingValueAt.pkg.manifestPackageName;
                        }
                        arrayList.add(new VersionedPackage(str, packageSettingValueAt.versionCode));
                    }
                } else if (packageSettingValueAt.pkg != null && (ArrayUtils.contains(packageSettingValueAt.pkg.usesLibraries, name) || ArrayUtils.contains(packageSettingValueAt.pkg.usesOptionalLibraries, name))) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(new VersionedPackage(packageSettingValueAt.name, packageSettingValueAt.versionCode));
                }
            }
        }
        return arrayList;
    }

    public ServiceInfo getServiceInfo(ComponentName componentName, int i, int i2) {
        if (!sUserManager.exists(i2)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForComponent = updateFlagsForComponent(i, i2, componentName);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "get service info");
        synchronized (this.mPackages) {
            PackageParser.Service service = (PackageParser.Service) this.mServices.mServices.get(componentName);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getServiceInfo " + componentName + ": " + service);
            }
            if (service == null || !this.mSettings.isEnabledAndMatchLPr(service.info, iUpdateFlagsForComponent, i2)) {
                return null;
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
            if (packageSetting == null) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, componentName, 3, i2)) {
                return null;
            }
            return PackageParser.generateServiceInfo(service, iUpdateFlagsForComponent, packageSetting.readUserState(i2), i2);
        }
    }

    public ProviderInfo getProviderInfo(ComponentName componentName, int i, int i2) {
        if (!sUserManager.exists(i2)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForComponent = updateFlagsForComponent(i, i2, componentName);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "get provider info");
        synchronized (this.mPackages) {
            PackageParser.Provider provider = (PackageParser.Provider) this.mProviders.mProviders.get(componentName);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getProviderInfo " + componentName + ": " + provider);
            }
            if (provider == null || !this.mSettings.isEnabledAndMatchLPr(provider.info, iUpdateFlagsForComponent, i2)) {
                return null;
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
            if (packageSetting == null) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, componentName, 4, i2)) {
                return null;
            }
            return PackageParser.generateProviderInfo(provider, iUpdateFlagsForComponent, packageSetting.readUserState(i2), i2);
        }
    }

    public String[] getSystemSharedLibraryNames() {
        synchronized (this.mPackages) {
            int size = this.mSharedLibraries.size();
            ArraySet arraySet = null;
            for (int i = 0; i < size; i++) {
                LongSparseArray<SharedLibraryEntry> longSparseArrayValueAt = this.mSharedLibraries.valueAt(i);
                if (longSparseArrayValueAt != null) {
                    int size2 = longSparseArrayValueAt.size();
                    int i2 = 0;
                    while (true) {
                        if (i2 < size2) {
                            SharedLibraryEntry sharedLibraryEntryValueAt = longSparseArrayValueAt.valueAt(i2);
                            if (!sharedLibraryEntryValueAt.info.isStatic()) {
                                if (arraySet == null) {
                                    arraySet = new ArraySet();
                                }
                                arraySet.add(sharedLibraryEntryValueAt.info.getName());
                            } else {
                                PackageSetting packageLPr = this.mSettings.getPackageLPr(sharedLibraryEntryValueAt.apk);
                                if (packageLPr == null || filterSharedLibPackageLPr(packageLPr, Binder.getCallingUid(), UserHandle.getUserId(Binder.getCallingUid()), 67108864)) {
                                    i2++;
                                } else {
                                    if (arraySet == null) {
                                        arraySet = new ArraySet();
                                    }
                                    arraySet.add(sharedLibraryEntryValueAt.info.getName());
                                }
                            }
                        }
                    }
                }
            }
            if (arraySet == null) {
                return null;
            }
            String[] strArr = new String[arraySet.size()];
            arraySet.toArray(strArr);
            return strArr;
        }
    }

    public String getServicesSystemSharedLibraryPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mServicesSystemSharedLibraryPackageName;
        }
        return str;
    }

    public String getSharedSystemSharedLibraryPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mSharedSystemSharedLibraryPackageName;
        }
        return str;
    }

    private void updateSequenceNumberLP(PackageSetting packageSetting, int[] iArr) {
        for (int length = iArr.length - 1; length >= 0; length--) {
            int i = iArr[length];
            if (!packageSetting.getInstantApp(i)) {
                SparseArray<String> sparseArray = this.mChangedPackages.get(i);
                if (sparseArray == null) {
                    sparseArray = new SparseArray<>();
                    this.mChangedPackages.put(i, sparseArray);
                }
                Map<String, Integer> map = this.mChangedPackagesSequenceNumbers.get(i);
                if (map == null) {
                    map = new HashMap<>();
                    this.mChangedPackagesSequenceNumbers.put(i, map);
                }
                Integer num = map.get(packageSetting.name);
                if (num != null) {
                    sparseArray.remove(num.intValue());
                }
                sparseArray.put(this.mChangedPackagesSequenceNumber, packageSetting.name);
                map.put(packageSetting.name, Integer.valueOf(this.mChangedPackagesSequenceNumber));
            }
        }
        this.mChangedPackagesSequenceNumber++;
    }

    public ChangedPackages getChangedPackages(int i, int i2) {
        ChangedPackages changedPackages = null;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            if (i >= this.mChangedPackagesSequenceNumber) {
                return null;
            }
            SparseArray<String> sparseArray = this.mChangedPackages.get(i2);
            if (sparseArray == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList(this.mChangedPackagesSequenceNumber - i);
            while (i < this.mChangedPackagesSequenceNumber) {
                String str = sparseArray.get(i);
                if (str != null) {
                    arrayList.add(str);
                }
                i++;
            }
            if (!arrayList.isEmpty()) {
                changedPackages = new ChangedPackages(this.mChangedPackagesSequenceNumber, arrayList);
            }
            return changedPackages;
        }
    }

    public ParceledListSlice<FeatureInfo> getSystemAvailableFeatures() {
        ArrayList arrayList;
        synchronized (this.mAvailableFeatures) {
            arrayList = new ArrayList(this.mAvailableFeatures.size() + 1);
            arrayList.addAll(this.mAvailableFeatures.values());
        }
        FeatureInfo featureInfo = new FeatureInfo();
        featureInfo.reqGlEsVersion = SystemProperties.getInt("ro.opengles.version", 0);
        arrayList.add(featureInfo);
        return new ParceledListSlice<>(arrayList);
    }

    public boolean hasSystemFeature(String str, int i) {
        synchronized (this.mAvailableFeatures) {
            FeatureInfo featureInfo = this.mAvailableFeatures.get(str);
            if (featureInfo == null) {
                return false;
            }
            return featureInfo.version >= i;
        }
    }

    public int checkPermission(String str, String str2, int i) {
        return this.mPermissionManager.checkPermission(str, str2, getCallingUid(), i);
    }

    public int checkUidPermission(String str, int i) {
        PackageParser.Package r1;
        int iCheckUidPermission;
        sCtaManager.reportPermRequestUsage(str, i);
        synchronized (this.mPackages) {
            String[] packagesForUid = getPackagesForUid(i);
            if (packagesForUid != null && packagesForUid.length > 0) {
                r1 = this.mPackages.get(packagesForUid[0]);
            } else {
                r1 = null;
            }
            iCheckUidPermission = this.mPermissionManager.checkUidPermission(str, r1, i, getCallingUid());
        }
        return iCheckUidPermission;
    }

    public boolean isPermissionRevokedByPolicy(String str, String str2, int i) {
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "isPermissionRevokedByPolicy for user " + i);
        }
        if (checkPermission(str, str2, i) == 0) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            if (!isCallerSameApp(str2, callingUid)) {
                return false;
            }
        } else if (isInstantApp(str2, i)) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return (getPermissionFlags(str, str2, i) & 4) != 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String getPermissionControllerPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mRequiredInstallerPackage;
        }
        return str;
    }

    private boolean addDynamicPermission(PermissionInfo permissionInfo, final boolean z) {
        return this.mPermissionManager.addDynamicPermission(permissionInfo, z, getCallingUid(), new PermissionManagerInternal.PermissionCallback() {
            @Override
            public void onPermissionChanged() {
                if (!z) {
                    PackageManagerService.this.mSettings.writeLPr();
                } else {
                    PackageManagerService.this.scheduleWriteSettingsLocked();
                }
            }
        });
    }

    public boolean addPermission(PermissionInfo permissionInfo) {
        boolean zAddDynamicPermission;
        synchronized (this.mPackages) {
            zAddDynamicPermission = addDynamicPermission(permissionInfo, false);
        }
        return zAddDynamicPermission;
    }

    public boolean addPermissionAsync(PermissionInfo permissionInfo) {
        boolean zAddDynamicPermission;
        synchronized (this.mPackages) {
            zAddDynamicPermission = addDynamicPermission(permissionInfo, true);
        }
        return zAddDynamicPermission;
    }

    public void removePermission(String str) {
        this.mPermissionManager.removeDynamicPermission(str, getCallingUid(), this.mPermissionCallback);
    }

    public void grantRuntimePermission(String str, String str2, int i) {
        this.mPermissionManager.grantRuntimePermission(str2, str, false, getCallingUid(), i, this.mPermissionCallback);
    }

    public void revokeRuntimePermission(String str, String str2, int i) {
        this.mPermissionManager.revokeRuntimePermission(str2, str, false, getCallingUid(), i, this.mPermissionCallback);
    }

    public void resetRuntimePermissions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "resetRuntimePermissions");
        }
        synchronized (this.mPackages) {
            this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false, this.mPackages.values(), this.mPermissionCallback);
            for (int i : UserManagerService.getInstance().getUserIds()) {
                int size = this.mPackages.size();
                for (int i2 = 0; i2 < size; i2++) {
                    PackageParser.Package packageValueAt = this.mPackages.valueAt(i2);
                    if (packageValueAt.mExtras instanceof PackageSetting) {
                        resetUserChangesToRuntimePermissionsAndFlagsLPw((PackageSetting) packageValueAt.mExtras, i);
                    }
                }
            }
        }
    }

    public int getPermissionFlags(String str, String str2, int i) {
        return this.mPermissionManager.getPermissionFlags(str, str2, getCallingUid(), i);
    }

    public void updatePermissionFlags(String str, String str2, int i, int i2, int i3) {
        this.mPermissionManager.updatePermissionFlags(str, str2, i, i2, getCallingUid(), i3, this.mPermissionCallback);
    }

    public void updatePermissionFlagsForAllApps(int i, int i2, int i3) {
        synchronized (this.mPackages) {
            if (this.mPermissionManager.updatePermissionFlagsForAllApps(i, i2, getCallingUid(), i3, this.mPackages.values(), this.mPermissionCallback)) {
                this.mSettings.writeRuntimePermissionsForUserLPr(i3, false);
            }
        }
    }

    public boolean shouldShowRequestPermissionRationale(String str, String str2, int i) {
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "canShowRequestPermissionRationale for user " + i);
        }
        if (UserHandle.getAppId(getCallingUid()) != UserHandle.getAppId(getPackageUid(str2, 268435456, i)) || checkPermission(str, str2, i) == 0) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int permissionFlags = getPermissionFlags(str, str2, i);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return (permissionFlags & 22) == 0 && (permissionFlags & 1) != 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public void addOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS", "addOnPermissionsChangeListener");
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.addListenerLocked(iOnPermissionsChangeListener);
        }
    }

    public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.removeListenerLocked(iOnPermissionsChangeListener);
        }
    }

    public boolean isProtectedBroadcast(String str) {
        synchronized (this.mProtectedBroadcasts) {
            if (this.mProtectedBroadcasts.contains(str)) {
                return true;
            }
            return str != null && (str.startsWith("android.net.netmon.lingerExpired") || str.startsWith("com.android.server.sip.SipWakeupTimer") || str.startsWith("com.android.internal.telephony.data-reconnect") || str.startsWith("android.net.netmon.launchCaptivePortalApp"));
        }
    }

    public int checkSignatures(String str, String str2) {
        synchronized (this.mPackages) {
            PackageParser.Package r7 = this.mPackages.get(str);
            PackageParser.Package r8 = this.mPackages.get(str2);
            if (r7 != null && r7.mExtras != null && r8 != null && r8.mExtras != null) {
                int callingUid = Binder.getCallingUid();
                int userId = UserHandle.getUserId(callingUid);
                PackageSetting packageSetting = (PackageSetting) r7.mExtras;
                PackageSetting packageSetting2 = (PackageSetting) r8.mExtras;
                if (!filterAppAccessLPr(packageSetting, callingUid, userId) && !filterAppAccessLPr(packageSetting2, callingUid, userId)) {
                    return PackageManagerServiceUtils.compareSignatures(r7.mSigningDetails.signatures, r8.mSigningDetails.signatures);
                }
                return -4;
            }
            return -4;
        }
    }

    public int checkUidSignatures(int i, int i2) {
        Signature[] signatureArr;
        Signature[] signatureArr2;
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        boolean z = getInstantAppPackageName(callingUid) != null;
        int appId = UserHandle.getAppId(i);
        int appId2 = UserHandle.getAppId(i2);
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(appId);
            if (userIdLPr == null) {
                return -4;
            }
            if (userIdLPr instanceof SharedUserSetting) {
                if (z) {
                    return -4;
                }
                signatureArr = ((SharedUserSetting) userIdLPr).signatures.mSigningDetails.signatures;
            } else {
                if (!(userIdLPr instanceof PackageSetting)) {
                    return -4;
                }
                PackageSetting packageSetting = (PackageSetting) userIdLPr;
                if (filterAppAccessLPr(packageSetting, callingUid, userId)) {
                    return -4;
                }
                signatureArr = packageSetting.signatures.mSigningDetails.signatures;
            }
            Object userIdLPr2 = this.mSettings.getUserIdLPr(appId2);
            if (userIdLPr2 == null) {
                return -4;
            }
            if (userIdLPr2 instanceof SharedUserSetting) {
                if (z) {
                    return -4;
                }
                signatureArr2 = ((SharedUserSetting) userIdLPr2).signatures.mSigningDetails.signatures;
            } else {
                if (!(userIdLPr2 instanceof PackageSetting)) {
                    return -4;
                }
                PackageSetting packageSetting2 = (PackageSetting) userIdLPr2;
                if (filterAppAccessLPr(packageSetting2, callingUid, userId)) {
                    return -4;
                }
                signatureArr2 = packageSetting2.signatures.mSigningDetails.signatures;
            }
            return PackageManagerServiceUtils.compareSignatures(signatureArr, signatureArr2);
        }
    }

    public boolean hasSigningCertificate(String str, byte[] bArr, int i) {
        synchronized (this.mPackages) {
            PackageParser.Package r6 = this.mPackages.get(str);
            if (r6 != null && r6.mExtras != null) {
                int callingUid = Binder.getCallingUid();
                if (filterAppAccessLPr((PackageSetting) r6.mExtras, callingUid, UserHandle.getUserId(callingUid))) {
                    return false;
                }
                switch (i) {
                    case 0:
                        return r6.mSigningDetails.hasCertificate(bArr);
                    case 1:
                        return r6.mSigningDetails.hasSha256Certificate(bArr);
                    default:
                        return false;
                }
            }
            return false;
        }
    }

    public boolean hasUidSigningCertificate(int i, byte[] bArr, int i2) {
        PackageParser.SigningDetails signingDetails;
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        int appId = UserHandle.getAppId(i);
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(appId);
            if (userIdLPr == null) {
                return false;
            }
            if (userIdLPr instanceof SharedUserSetting) {
                if (getInstantAppPackageName(callingUid) != null) {
                    return false;
                }
                signingDetails = ((SharedUserSetting) userIdLPr).signatures.mSigningDetails;
            } else {
                if (!(userIdLPr instanceof PackageSetting)) {
                    return false;
                }
                PackageSetting packageSetting = (PackageSetting) userIdLPr;
                if (filterAppAccessLPr(packageSetting, callingUid, userId)) {
                    return false;
                }
                signingDetails = packageSetting.signatures.mSigningDetails;
            }
            switch (i2) {
                case 0:
                    return signingDetails.hasCertificate(bArr);
                case 1:
                    return signingDetails.hasSha256Certificate(bArr);
                default:
                    return false;
            }
        }
    }

    private void killUid(int i, int i2, String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            IActivityManager service = ActivityManager.getService();
            if (service != null) {
                try {
                    service.killUid(i, i2, str);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isCompatSignatureUpdateNeeded(PackageParser.Package r2) {
        return getSettingsVersionForPackage(r2).databaseVersion < 2;
    }

    private boolean isRecoverSignatureUpdateNeeded(PackageParser.Package r2) {
        return getSettingsVersionForPackage(r2).databaseVersion < 3;
    }

    public List<String> getAllPackages() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0 && callingUid != 2000) {
            throw new SecurityException("getAllPackages is limited to privileged callers");
        }
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mPackages) {
            if (canViewInstantApps(callingUid, userId)) {
                return new ArrayList(this.mPackages.keySet());
            }
            String instantAppPackageName = getInstantAppPackageName(callingUid);
            ArrayList arrayList = new ArrayList();
            if (instantAppPackageName != null) {
                for (PackageParser.Package r1 : this.mPackages.values()) {
                    if (r1.visibleToInstantApps) {
                        arrayList.add(r1.packageName);
                    }
                }
            } else {
                for (PackageParser.Package r5 : this.mPackages.values()) {
                    PackageSetting packageSetting = r5.mExtras != null ? (PackageSetting) r5.mExtras : null;
                    if (packageSetting == null || !packageSetting.getInstantApp(userId) || this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), packageSetting.appId)) {
                        arrayList.add(r5.packageName);
                    }
                }
            }
            return arrayList;
        }
    }

    public String[] getPackagesForUid(int i) {
        int callingUid = Binder.getCallingUid();
        int i2 = 0;
        boolean z = getInstantAppPackageName(callingUid) != null;
        int userId = UserHandle.getUserId(i);
        int appId = UserHandle.getAppId(i);
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(appId);
            if (userIdLPr instanceof SharedUserSetting) {
                if (z) {
                    return null;
                }
                SharedUserSetting sharedUserSetting = (SharedUserSetting) userIdLPr;
                String[] strArr = new String[sharedUserSetting.packages.size()];
                for (PackageSetting packageSetting : sharedUserSetting.packages) {
                    if (packageSetting.getInstalled(userId)) {
                        strArr[i2] = packageSetting.name;
                        i2++;
                    } else {
                        strArr = (String[]) ArrayUtils.removeElement(String.class, strArr, strArr[i2]);
                    }
                }
                return strArr;
            }
            if (userIdLPr instanceof PackageSetting) {
                PackageSetting packageSetting2 = (PackageSetting) userIdLPr;
                if (packageSetting2.getInstalled(userId) && !filterAppAccessLPr(packageSetting2, callingUid, userId)) {
                    return new String[]{packageSetting2.name};
                }
            }
            return null;
        }
    }

    public String getNameForUid(int i) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(UserHandle.getAppId(i));
            if (userIdLPr instanceof SharedUserSetting) {
                SharedUserSetting sharedUserSetting = (SharedUserSetting) userIdLPr;
                return sharedUserSetting.name + ":" + sharedUserSetting.userId;
            }
            if (!(userIdLPr instanceof PackageSetting)) {
                return null;
            }
            PackageSetting packageSetting = (PackageSetting) userIdLPr;
            if (filterAppAccessLPr(packageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                return null;
            }
            return packageSetting.name;
        }
    }

    public String[] getNamesForUids(int[] iArr) {
        if (iArr == null || iArr.length == 0) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        String[] strArr = new String[iArr.length];
        synchronized (this.mPackages) {
            for (int length = iArr.length - 1; length >= 0; length--) {
                Object userIdLPr = this.mSettings.getUserIdLPr(UserHandle.getAppId(iArr[length]));
                if (userIdLPr instanceof SharedUserSetting) {
                    strArr[length] = "shared:" + ((SharedUserSetting) userIdLPr).name;
                } else if (userIdLPr instanceof PackageSetting) {
                    PackageSetting packageSetting = (PackageSetting) userIdLPr;
                    if (filterAppAccessLPr(packageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                        strArr[length] = null;
                    } else {
                        strArr[length] = packageSetting.name;
                    }
                } else {
                    strArr[length] = null;
                }
            }
        }
        return strArr;
    }

    public int getUidForSharedUser(String str) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || str == null) {
            return -1;
        }
        synchronized (this.mPackages) {
            try {
                try {
                    SharedUserSetting sharedUserLPw = this.mSettings.getSharedUserLPw(str, 0, 0, false);
                    if (sharedUserLPw != null) {
                        return sharedUserLPw.userId;
                    }
                } finally {
                }
            } catch (PackageManagerException e) {
            }
            return -1;
        }
    }

    public int getFlagsForUid(int i) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(UserHandle.getAppId(i));
            if (userIdLPr instanceof SharedUserSetting) {
                return ((SharedUserSetting) userIdLPr).pkgFlags;
            }
            if (!(userIdLPr instanceof PackageSetting)) {
                return 0;
            }
            PackageSetting packageSetting = (PackageSetting) userIdLPr;
            if (filterAppAccessLPr(packageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                return 0;
            }
            return packageSetting.pkgFlags;
        }
    }

    public int getPrivateFlagsForUid(int i) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(UserHandle.getAppId(i));
            if (userIdLPr instanceof SharedUserSetting) {
                return ((SharedUserSetting) userIdLPr).pkgPrivateFlags;
            }
            if (!(userIdLPr instanceof PackageSetting)) {
                return 0;
            }
            PackageSetting packageSetting = (PackageSetting) userIdLPr;
            if (filterAppAccessLPr(packageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                return 0;
            }
            return packageSetting.pkgPrivateFlags;
        }
    }

    public boolean isUidPrivileged(int i) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return false;
        }
        int appId = UserHandle.getAppId(i);
        synchronized (this.mPackages) {
            Object userIdLPr = this.mSettings.getUserIdLPr(appId);
            if (userIdLPr instanceof SharedUserSetting) {
                Iterator<PackageSetting> it = ((SharedUserSetting) userIdLPr).packages.iterator();
                while (it.hasNext()) {
                    if (it.next().isPrivileged()) {
                        return true;
                    }
                }
            } else if (userIdLPr instanceof PackageSetting) {
                return ((PackageSetting) userIdLPr).isPrivileged();
            }
            return false;
        }
    }

    public String[] getAppOpPermissionPackages(String str) {
        return this.mPermissionManager.getAppOpPermissionPackages(str);
    }

    public ResolveInfo resolveIntent(Intent intent, String str, int i, int i2) {
        return resolveIntentInternal(intent, str, i, i2, false, Binder.getCallingUid());
    }

    private ResolveInfo resolveIntentInternal(Intent intent, String str, int i, int i2, boolean z, int i3) {
        try {
            Trace.traceBegin(262144L, "resolveIntent");
            if (sUserManager.exists(i2)) {
                int callingUid = Binder.getCallingUid();
                int iUpdateFlagsForResolve = updateFlagsForResolve(i, i2, intent, i3, z);
                this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "resolve intent");
                Trace.traceBegin(262144L, "queryIntentActivities");
                List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intent, str, iUpdateFlagsForResolve, i3, i2, z, true);
                Trace.traceEnd(262144L);
                return chooseBestActivity(intent, str, iUpdateFlagsForResolve, listQueryIntentActivitiesInternal, i2);
            }
            return null;
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    public ResolveInfo findPersistentPreferredActivity(Intent intent, int i) {
        ResolveInfo resolveInfoFindPersistentPreferredActivityLP;
        if (!UserHandle.isSameApp(Binder.getCallingUid(), 1000)) {
            throw new SecurityException("findPersistentPreferredActivity can only be run by the system");
        }
        if (!sUserManager.exists(i)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        Intent intentUpdateIntentForResolve = updateIntentForResolve(intent);
        String strResolveTypeIfNeeded = intentUpdateIntentForResolve.resolveTypeIfNeeded(this.mContext.getContentResolver());
        int iUpdateFlagsForResolve = updateFlagsForResolve(0, i, intentUpdateIntentForResolve, callingUid, false);
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intentUpdateIntentForResolve, strResolveTypeIfNeeded, iUpdateFlagsForResolve, i);
        synchronized (this.mPackages) {
            resolveInfoFindPersistentPreferredActivityLP = findPersistentPreferredActivityLP(intentUpdateIntentForResolve, strResolveTypeIfNeeded, iUpdateFlagsForResolve, listQueryIntentActivitiesInternal, false, i);
        }
        return resolveInfoFindPersistentPreferredActivityLP;
    }

    public void setLastChosenActivity(Intent intent, String str, int i, IntentFilter intentFilter, int i2, ComponentName componentName) {
        int i3;
        ComponentName componentName2;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG_PREFERRED) {
            StringBuilder sb = new StringBuilder();
            sb.append("setLastChosenActivity intent=");
            sb.append(intent);
            sb.append(" resolvedType=");
            sb.append(str);
            sb.append(" flags=");
            sb.append(i);
            sb.append(" filter=");
            sb.append(intentFilter);
            sb.append(" match=");
            i3 = i2;
            sb.append(i3);
            sb.append(" activity=");
            componentName2 = componentName;
            sb.append(componentName2);
            Log.v(TAG, sb.toString());
            intentFilter.dump(new PrintStreamPrinter(System.out), "    ");
        } else {
            i3 = i2;
            componentName2 = componentName;
        }
        intent.setComponent(null);
        findPreferredActivity(intent, str, i, queryIntentActivitiesInternal(intent, str, i, callingUserId), 0, false, true, false, callingUserId);
        addPreferredActivityInternal(intentFilter, i3, null, componentName2, false, callingUserId, "Setting last chosen");
    }

    public ResolveInfo getLastChosenActivity(Intent intent, String str, int i) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG_PREFERRED) {
            Log.v(TAG, "Querying last chosen activity for " + intent);
        }
        return findPreferredActivity(intent, str, i, queryIntentActivitiesInternal(intent, str, i, callingUserId), 0, false, false, false, callingUserId);
    }

    private boolean areWebInstantAppsDisabled() {
        return this.mWebInstantAppsDisabled;
    }

    private boolean isInstantAppResolutionAllowed(Intent intent, List<ResolveInfo> list, int i, boolean z) {
        int size;
        int domainVerificationStatusLPr;
        if (this.mInstantAppResolverConnection == null || this.mInstantAppInstallerActivity == null || intent.getComponent() != null || (intent.getFlags() & 512) != 0) {
            return false;
        }
        if (!z && intent.getPackage() != null) {
            return false;
        }
        if (!intent.isWebIntent()) {
            if ((list != null && list.size() != 0) || (intent.getFlags() & 2048) == 0) {
                return false;
            }
        } else if (intent.getData() == null || TextUtils.isEmpty(intent.getData().getHost()) || areWebInstantAppsDisabled()) {
            return false;
        }
        synchronized (this.mPackages) {
            if (list != null) {
                size = list.size();
            } else {
                size = 0;
            }
            for (int i2 = 0; i2 < size; i2++) {
                ResolveInfo resolveInfo = list.get(i2);
                String str = resolveInfo.activityInfo.packageName;
                PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                if (packageSetting != null) {
                    if (!resolveInfo.handleAllWebDataURI && ((domainVerificationStatusLPr = (int) (getDomainVerificationStatusLPr(packageSetting, i) >> 32)) == 2 || domainVerificationStatusLPr == 4)) {
                        if (DEBUG_INSTANT) {
                            Slog.v(TAG, "DENY instant app; pkg: " + str + ", status: " + domainVerificationStatusLPr);
                        }
                        return false;
                    }
                    if (packageSetting.getInstantApp(i)) {
                        if (DEBUG_INSTANT) {
                            Slog.v(TAG, "DENY instant app installed; pkg: " + str);
                        }
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo auxiliaryResolveInfo, Intent intent, String str, String str2, Bundle bundle, int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(20, new InstantAppRequest(auxiliaryResolveInfo, intent, str, str2, i, bundle, false)));
    }

    private ResolveInfo chooseBestActivity(Intent intent, String str, int i, List<ResolveInfo> list, int i2) {
        if (list != null) {
            int size = list.size();
            if (size == 1) {
                return list.get(0);
            }
            if (size > 1) {
                boolean z = (intent.getFlags() & 8) != 0;
                ResolveInfo resolveInfo = list.get(0);
                ResolveInfo resolveInfo2 = list.get(1);
                if (DEBUG_INTENT_MATCHING || z) {
                    Slog.v(TAG, resolveInfo.activityInfo.name + "=" + resolveInfo.priority + " vs " + resolveInfo2.activityInfo.name + "=" + resolveInfo2.priority);
                }
                if (resolveInfo.priority == resolveInfo2.priority && resolveInfo.preferredOrder == resolveInfo2.preferredOrder && resolveInfo.isDefault == resolveInfo2.isDefault) {
                    ResolveInfo resolveInfoFindPreferredActivity = findPreferredActivity(intent, str, i, list, resolveInfo.priority, true, false, z, i2);
                    if (resolveInfoFindPreferredActivity != null) {
                        return resolveInfoFindPreferredActivity;
                    }
                    for (int i3 = 0; i3 < size; i3++) {
                        ResolveInfo resolveInfo3 = list.get(i3);
                        if (resolveInfo3.activityInfo.applicationInfo.isInstantApp()) {
                            if (((int) (getDomainVerificationStatusLPr(this.mSettings.mPackages.get(resolveInfo3.activityInfo.packageName), i2) >> 32)) != 4) {
                                return resolveInfo3;
                            }
                        }
                    }
                    ResolveInfo resolveInfo4 = new ResolveInfo(this.mResolveInfo);
                    resolveInfo4.activityInfo = new ActivityInfo(resolveInfo4.activityInfo);
                    resolveInfo4.activityInfo.labelRes = ResolverActivity.getLabelRes(intent.getAction());
                    String str2 = intent.getPackage();
                    if (!TextUtils.isEmpty(str2) && allHavePackage(list, str2)) {
                        ApplicationInfo applicationInfo = list.get(0).activityInfo.applicationInfo;
                        resolveInfo4.resolvePackageName = str2;
                        if (userNeedsBadging(i2)) {
                            resolveInfo4.noResourceId = true;
                        } else {
                            resolveInfo4.icon = applicationInfo.icon;
                        }
                        resolveInfo4.iconResourceId = applicationInfo.icon;
                        resolveInfo4.labelRes = applicationInfo.labelRes;
                    }
                    resolveInfo4.activityInfo.applicationInfo = new ApplicationInfo(resolveInfo4.activityInfo.applicationInfo);
                    if (i2 != 0) {
                        resolveInfo4.activityInfo.applicationInfo.uid = UserHandle.getUid(i2, UserHandle.getAppId(resolveInfo4.activityInfo.applicationInfo.uid));
                    }
                    if (resolveInfo4.activityInfo.metaData == null) {
                        resolveInfo4.activityInfo.metaData = new Bundle();
                    }
                    resolveInfo4.activityInfo.metaData.putBoolean("android.dock_home", true);
                    return resolveInfo4;
                }
                return list.get(0);
            }
            return null;
        }
        return null;
    }

    private boolean allHavePackage(List<ResolveInfo> list, String str) {
        if (ArrayUtils.isEmpty(list)) {
            return false;
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = list.get(i);
            ActivityInfo activityInfo = resolveInfo != null ? resolveInfo.activityInfo : null;
            if (activityInfo == null || !str.equals(activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }

    private ResolveInfo findPersistentPreferredActivityLP(Intent intent, String str, int i, List<ResolveInfo> list, boolean z, int i2) {
        List<PersistentPreferredActivity> listQueryIntent;
        int size = list.size();
        PersistentPreferredIntentResolver persistentPreferredIntentResolver = this.mSettings.mPersistentPreferredActivities.get(i2);
        if (DEBUG_PREFERRED || z) {
            Slog.v(TAG, "Looking for presistent preferred activities...");
        }
        if (persistentPreferredIntentResolver != null) {
            listQueryIntent = persistentPreferredIntentResolver.queryIntent(intent, str, (65536 & i) != 0, i2);
        } else {
            listQueryIntent = null;
        }
        if (listQueryIntent != null && listQueryIntent.size() > 0) {
            int size2 = listQueryIntent.size();
            for (int i3 = 0; i3 < size2; i3++) {
                PersistentPreferredActivity persistentPreferredActivity = listQueryIntent.get(i3);
                if (DEBUG_PREFERRED || z) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Checking PersistentPreferredActivity ds=");
                    sb.append(persistentPreferredActivity.countDataSchemes() > 0 ? persistentPreferredActivity.getDataScheme(0) : "<none>");
                    sb.append("\n  component=");
                    sb.append(persistentPreferredActivity.mComponent);
                    Slog.v(TAG, sb.toString());
                    persistentPreferredActivity.dump(new LogPrinter(2, TAG, 3), "  ");
                }
                ActivityInfo activityInfo = getActivityInfo(persistentPreferredActivity.mComponent, i | 512, i2);
                if (DEBUG_PREFERRED || z) {
                    Slog.v(TAG, "Found persistent preferred activity:");
                    if (activityInfo != null) {
                        activityInfo.dump(new LogPrinter(2, TAG, 3), "  ");
                    } else {
                        Slog.v(TAG, "  null");
                    }
                }
                if (activityInfo != null) {
                    for (int i4 = 0; i4 < size; i4++) {
                        ResolveInfo resolveInfo = list.get(i4);
                        if (resolveInfo.activityInfo.applicationInfo.packageName.equals(activityInfo.applicationInfo.packageName) && resolveInfo.activityInfo.name.equals(activityInfo.name)) {
                            if (DEBUG_PREFERRED || z) {
                                Slog.v(TAG, "Returning persistent preferred activity: " + resolveInfo.activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + resolveInfo.activityInfo.name);
                            }
                            return resolveInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    ResolveInfo findPreferredActivity(Intent intent, String str, int i, List<ResolveInfo> list, int i2, boolean z, boolean z2, boolean z3, int i3) {
        boolean z4;
        int size;
        int i4;
        int size2;
        int i5;
        boolean z5;
        PreferredActivity preferredActivity;
        int i6;
        List<PreferredActivity> list2;
        int i7;
        boolean z6;
        if (!sUserManager.exists(i3)) {
            return null;
        }
        int iUpdateFlagsForResolve = updateFlagsForResolve(i, i3, intent, Binder.getCallingUid(), false);
        Intent intentUpdateIntentForResolve = updateIntentForResolve(intent);
        synchronized (this.mPackages) {
            ResolveInfo resolveInfoFindPersistentPreferredActivityLP = findPersistentPreferredActivityLP(intentUpdateIntentForResolve, str, iUpdateFlagsForResolve, list, z3, i3);
            if (resolveInfoFindPersistentPreferredActivityLP != null) {
                return resolveInfoFindPersistentPreferredActivityLP;
            }
            PreferredIntentResolver preferredIntentResolver = this.mSettings.mPreferredActivities.get(i3);
            if (DEBUG_PREFERRED || z3) {
                Slog.v(TAG, "Looking for preferred activities...");
            }
            List<PreferredActivity> listQueryIntent = preferredIntentResolver != null ? preferredIntentResolver.queryIntent(intentUpdateIntentForResolve, str, (65536 & iUpdateFlagsForResolve) != 0, i3) : null;
            if (listQueryIntent != null && listQueryIntent.size() > 0) {
                try {
                    if (DEBUG_PREFERRED || z3) {
                        Slog.v(TAG, "Figuring out best match...");
                    }
                    size = list.size();
                    int i8 = 0;
                    for (int i9 = 0; i9 < size; i9++) {
                        ResolveInfo resolveInfo = list.get(i9);
                        if (DEBUG_PREFERRED || z3) {
                            Slog.v(TAG, "Match for " + resolveInfo.activityInfo + ": 0x" + Integer.toHexString(i8));
                        }
                        if (resolveInfo.match > i8) {
                            i8 = resolveInfo.match;
                        }
                    }
                    if (DEBUG_PREFERRED || z3) {
                        Slog.v(TAG, "Best match: 0x" + Integer.toHexString(i8));
                    }
                    i4 = 268369920 & i8;
                    size2 = listQueryIntent.size();
                    i5 = 0;
                    z5 = false;
                } catch (Throwable th) {
                    th = th;
                    z4 = false;
                }
                while (i5 < size2) {
                    try {
                        preferredActivity = listQueryIntent.get(i5);
                        i6 = size2;
                        if (DEBUG_PREFERRED || z3) {
                            StringBuilder sb = new StringBuilder();
                            list2 = listQueryIntent;
                            sb.append("Checking PreferredActivity ds=");
                            sb.append(preferredActivity.countDataSchemes() > 0 ? preferredActivity.getDataScheme(0) : "<none>");
                            sb.append("\n  component=");
                            sb.append(preferredActivity.mPref.mComponent);
                            Slog.v(TAG, sb.toString());
                            z4 = z5;
                            preferredActivity.dump(new LogPrinter(2, TAG, 3), "  ");
                        } else {
                            list2 = listQueryIntent;
                            z4 = z5;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        z4 = z5;
                    }
                    if (preferredActivity.mPref.mMatch != i4) {
                        try {
                            if (DEBUG_PREFERRED || z3) {
                                Slog.v(TAG, "Skipping bad match " + Integer.toHexString(preferredActivity.mPref.mMatch));
                            }
                            i7 = i4;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } else if (!z || preferredActivity.mPref.mAlways) {
                        ActivityInfo activityInfo = getActivityInfo(preferredActivity.mPref.mComponent, iUpdateFlagsForResolve | 512 | 524288 | 262144, i3);
                        if (DEBUG_PREFERRED || z3) {
                            Slog.v(TAG, "Found preferred activity:");
                            if (activityInfo != null) {
                                i7 = i4;
                                activityInfo.dump(new LogPrinter(2, TAG, 3), "  ");
                            } else {
                                i7 = i4;
                                Slog.v(TAG, "  null");
                            }
                        } else {
                            i7 = i4;
                        }
                        if (activityInfo == null) {
                            Slog.w(TAG, "Removing dangling preferred activity: " + preferredActivity.mPref.mComponent);
                            preferredIntentResolver.removeFilter(preferredActivity);
                        } else {
                            for (int i10 = 0; i10 < size; i10++) {
                                ResolveInfo resolveInfo2 = list.get(i10);
                                if (resolveInfo2.activityInfo.applicationInfo.packageName.equals(activityInfo.applicationInfo.packageName) && resolveInfo2.activityInfo.name.equals(activityInfo.name)) {
                                    if (z2) {
                                        preferredIntentResolver.removeFilter(preferredActivity);
                                        try {
                                            if (DEBUG_PREFERRED) {
                                                Slog.v(TAG, "Removing match " + preferredActivity.mPref.mComponent);
                                            }
                                        } catch (Throwable th4) {
                                            th = th4;
                                            z4 = true;
                                        }
                                    } else {
                                        if (!z || preferredActivity.mPref.sameSet(list)) {
                                            z6 = z4;
                                        } else {
                                            if (!preferredActivity.mPref.isSuperset(list)) {
                                                Slog.i(TAG, "Result set changed, dropping preferred activity for " + intentUpdateIntentForResolve + " type " + str);
                                                if (DEBUG_PREFERRED) {
                                                    Slog.v(TAG, "Removing preferred activity since set changed " + preferredActivity.mPref.mComponent);
                                                }
                                                preferredIntentResolver.removeFilter(preferredActivity);
                                                preferredIntentResolver.addFilter(new PreferredActivity(preferredActivity, preferredActivity.mPref.mMatch, null, preferredActivity.mPref.mComponent, false));
                                                if (DEBUG_PREFERRED) {
                                                    Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                                                }
                                                scheduleWritePackageRestrictionsLocked(i3);
                                                return null;
                                            }
                                            if (DEBUG_PREFERRED) {
                                                Slog.i(TAG, "Result set changed, but PreferredActivity is still valid as only non-preferred components were removed for " + intentUpdateIntentForResolve + " type " + str);
                                            }
                                            PreferredActivity preferredActivity2 = new PreferredActivity(preferredActivity, preferredActivity.mPref.mMatch, preferredActivity.mPref.discardObsoleteComponents(list), preferredActivity.mPref.mComponent, preferredActivity.mPref.mAlways);
                                            preferredIntentResolver.removeFilter(preferredActivity);
                                            preferredIntentResolver.addFilter(preferredActivity2);
                                            z6 = true;
                                        }
                                        try {
                                            if (DEBUG_PREFERRED || z3) {
                                                Slog.v(TAG, "Returning preferred activity: " + resolveInfo2.activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + resolveInfo2.activityInfo.name);
                                            }
                                            if (z6) {
                                                if (DEBUG_PREFERRED) {
                                                    Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                                                }
                                                scheduleWritePackageRestrictionsLocked(i3);
                                            }
                                            return resolveInfo2;
                                        } catch (Throwable th5) {
                                            th = th5;
                                            z4 = z6;
                                        }
                                    }
                                    if (z4) {
                                        if (DEBUG_PREFERRED) {
                                            Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                                        }
                                        scheduleWritePackageRestrictionsLocked(i3);
                                    }
                                    throw th;
                                }
                            }
                        }
                        z4 = true;
                        i5++;
                        size2 = i6;
                        listQueryIntent = list2;
                        z5 = z4;
                        i4 = i7;
                    } else {
                        if (DEBUG_PREFERRED || z3) {
                            Slog.v(TAG, "Skipping mAlways=false entry");
                        }
                        i7 = i4;
                    }
                    i5++;
                    size2 = i6;
                    listQueryIntent = list2;
                    z5 = z4;
                    i4 = i7;
                }
                if (z5) {
                    if (DEBUG_PREFERRED) {
                        Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                    }
                    scheduleWritePackageRestrictionsLocked(i3);
                }
            }
            if (!DEBUG_PREFERRED && !z3) {
                return null;
            }
            Slog.v(TAG, "No preferred activity to return");
            return null;
        }
    }

    public boolean canForwardTo(Intent intent, String str, int i, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        List<CrossProfileIntentFilter> matchingCrossProfileIntentFilters = getMatchingCrossProfileIntentFilters(intent, str, i);
        boolean z = true;
        if (matchingCrossProfileIntentFilters != null) {
            int size = matchingCrossProfileIntentFilters.size();
            for (int i3 = 0; i3 < size; i3++) {
                if (matchingCrossProfileIntentFilters.get(i3).getTargetUserId() == i2) {
                    return true;
                }
            }
        }
        if (!intent.hasWebURI()) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        UserInfo profileParent = getProfileParent(i);
        synchronized (this.mPackages) {
            if (getCrossProfileDomainPreferredLpr(intent, str, updateFlagsForResolve(0, profileParent.id, intent, callingUid, false), i, profileParent.id) == null) {
                z = false;
            }
        }
        return z;
    }

    private UserInfo getProfileParent(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return sUserManager.getProfileParent(i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<CrossProfileIntentFilter> getMatchingCrossProfileIntentFilters(Intent intent, String str, int i) {
        CrossProfileIntentResolver crossProfileIntentResolver = this.mSettings.mCrossProfileIntentResolvers.get(i);
        if (crossProfileIntentResolver != null) {
            return crossProfileIntentResolver.queryIntent(intent, str, false, i);
        }
        return null;
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent, String str, int i, int i2) {
        try {
            Trace.traceBegin(262144L, "queryIntentActivities");
            return new ParceledListSlice<>(queryIntentActivitiesInternal(intent, str, i, i2));
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private String getInstantAppPackageName(int i) {
        synchronized (this.mPackages) {
            if (Process.isIsolated(i)) {
                i = this.mIsolatedOwners.get(i);
            }
            Object userIdLPr = this.mSettings.getUserIdLPr(UserHandle.getAppId(i));
            if (!(userIdLPr instanceof PackageSetting)) {
                return null;
            }
            PackageSetting packageSetting = (PackageSetting) userIdLPr;
            return packageSetting.getInstantApp(UserHandle.getUserId(i)) ? packageSetting.pkg.packageName : null;
        }
    }

    public List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String str, int i, int i2) {
        return queryIntentActivitiesInternal(intent, str, i, Binder.getCallingUid(), i2, false, true);
    }

    private List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String str, int i, int i2, int i3, boolean z, boolean z2) {
        Intent intent2;
        ComponentName component;
        int i4;
        boolean z3;
        boolean zIsInstantAppResolutionAllowed;
        List<ResolveInfo> arrayList;
        List<ResolveInfo> listFilterCandidatesWithDomainPreferredActivitiesLPr;
        if (!sUserManager.exists(i3)) {
            return Collections.emptyList();
        }
        String instantAppPackageName = getInstantAppPackageName(i2);
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i3, false, false, "query intent activities");
        String str2 = intent.getPackage();
        ComponentName component2 = intent.getComponent();
        if (component2 != null || intent.getSelector() == null) {
            intent2 = intent;
            component = component2;
        } else {
            Intent selector = intent.getSelector();
            intent2 = selector;
            component = selector.getComponent();
        }
        boolean z4 = true;
        int iUpdateFlagsForResolve = updateFlagsForResolve(i, i3, intent2, i2, z, (component == null && str2 == null) ? false : true);
        if (component != null) {
            ArrayList arrayList2 = new ArrayList(1);
            ActivityInfo activityInfo = getActivityInfo(component, iUpdateFlagsForResolve, i3);
            if (activityInfo != null) {
                boolean z5 = (8388608 & iUpdateFlagsForResolve) != 0;
                boolean z6 = (16777216 & iUpdateFlagsForResolve) != 0;
                boolean z7 = (33554432 & iUpdateFlagsForResolve) != 0;
                boolean z8 = instantAppPackageName != null;
                boolean zEquals = component.getPackageName().equals(instantAppPackageName);
                boolean z9 = (activityInfo.applicationInfo.privateFlags & 128) != 0;
                boolean z10 = (activityInfo.flags & 1048576) != 0;
                boolean z11 = !z10 || (z7 && !(z10 && (activityInfo.flags & 2097152) == 0));
                if (zEquals || ((z5 || z8 || !z9) && (!z6 || !z8 || !z11))) {
                    z4 = false;
                }
                if (!z4) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = activityInfo;
                    arrayList2.add(resolveInfo);
                }
            }
            return applyPostResolutionFilter(arrayList2, instantAppPackageName, z2, i2, z, i3, intent2);
        }
        synchronized (this.mPackages) {
            try {
                if (str2 == null) {
                    List<CrossProfileIntentFilter> matchingCrossProfileIntentFilters = getMatchingCrossProfileIntentFilters(intent2, str, i3);
                    ResolveInfo resolveInfoQuerySkipCurrentProfileIntents = querySkipCurrentProfileIntents(matchingCrossProfileIntentFilters, intent2, str, iUpdateFlagsForResolve, i3);
                    if (resolveInfoQuerySkipCurrentProfileIntents != null) {
                        ArrayList arrayList3 = new ArrayList(1);
                        arrayList3.add(resolveInfoQuerySkipCurrentProfileIntents);
                        return applyPostResolutionFilter(filterIfNotSystemUser(arrayList3, i3), instantAppPackageName, z2, i2, z, i3, intent2);
                    }
                    List<ResolveInfo> listFilterIfNotSystemUser = filterIfNotSystemUser(this.mActivities.queryIntent(intent2, str, iUpdateFlagsForResolve, i3), i3);
                    boolean zIsInstantAppResolutionAllowed2 = isInstantAppResolutionAllowed(intent2, listFilterIfNotSystemUser, i3, false);
                    boolean z12 = false;
                    i4 = iUpdateFlagsForResolve;
                    ResolveInfo resolveInfoQueryCrossProfileIntents = queryCrossProfileIntents(matchingCrossProfileIntentFilters, intent2, str, iUpdateFlagsForResolve, i3, hasNonNegativePriority(listFilterIfNotSystemUser));
                    if (resolveInfoQueryCrossProfileIntents != null && isUserEnabled(resolveInfoQueryCrossProfileIntents.targetUserId)) {
                        if (filterIfNotSystemUser(Collections.singletonList(resolveInfoQueryCrossProfileIntents), i3).size() > 0) {
                            listFilterIfNotSystemUser.add(resolveInfoQueryCrossProfileIntents);
                            z12 = true;
                        }
                    }
                    if (intent2.hasWebURI()) {
                        UserInfo profileParent = getProfileParent(i3);
                        CrossProfileDomainInfo crossProfileDomainPreferredLpr = profileParent != null ? getCrossProfileDomainPreferredLpr(intent2, str, i4, i3, profileParent.id) : null;
                        if (crossProfileDomainPreferredLpr != null) {
                            if (resolveInfoQueryCrossProfileIntents != null) {
                                listFilterIfNotSystemUser.remove(resolveInfoQueryCrossProfileIntents);
                            }
                            if (listFilterIfNotSystemUser.size() == 0 && !zIsInstantAppResolutionAllowed2) {
                                listFilterIfNotSystemUser.add(crossProfileDomainPreferredLpr.resolveInfo);
                                return applyPostResolutionFilter(listFilterIfNotSystemUser, instantAppPackageName, z2, i2, z, i3, intent2);
                            }
                        } else if (listFilterIfNotSystemUser.size() <= 1 && !zIsInstantAppResolutionAllowed2) {
                            return applyPostResolutionFilter(listFilterIfNotSystemUser, instantAppPackageName, z2, i2, z, i3, intent2);
                        }
                        listFilterCandidatesWithDomainPreferredActivitiesLPr = filterCandidatesWithDomainPreferredActivitiesLPr(intent2, i4, listFilterIfNotSystemUser, crossProfileDomainPreferredLpr, i3);
                    } else {
                        listFilterCandidatesWithDomainPreferredActivitiesLPr = listFilterIfNotSystemUser;
                        z4 = z12;
                    }
                    arrayList = listFilterCandidatesWithDomainPreferredActivitiesLPr;
                    z3 = z4;
                    zIsInstantAppResolutionAllowed = zIsInstantAppResolutionAllowed2;
                } else {
                    i4 = iUpdateFlagsForResolve;
                    z3 = false;
                    PackageParser.Package r1 = this.mPackages.get(str2);
                    List<ResolveInfo> listFilterIfNotSystemUser2 = r1 != null ? filterIfNotSystemUser(this.mActivities.queryIntentForPackage(intent2, str, i4, r1.activities, i3), i3) : null;
                    if (listFilterIfNotSystemUser2 == null || listFilterIfNotSystemUser2.size() == 0) {
                        zIsInstantAppResolutionAllowed = isInstantAppResolutionAllowed(intent2, null, i3, true);
                        arrayList = listFilterIfNotSystemUser2 == null ? new ArrayList() : listFilterIfNotSystemUser2;
                    } else {
                        arrayList = listFilterIfNotSystemUser2;
                        zIsInstantAppResolutionAllowed = false;
                    }
                }
                if (zIsInstantAppResolutionAllowed) {
                    arrayList = maybeAddInstantAppInstaller(arrayList, intent2, str, i4, i3, z);
                }
                if (z3) {
                    Collections.sort(arrayList, mResolvePrioritySorter);
                }
                return applyPostResolutionFilter(arrayList, instantAppPackageName, z2, i2, z, i3, intent2);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private List<ResolveInfo> maybeAddInstantAppInstaller(List<ResolveInfo> list, Intent intent, String str, int i, int i2, boolean z) {
        Intent intent2;
        String str2;
        ResolveInfo resolveInfo;
        boolean z2;
        PackageSetting packageSetting;
        ResolveInfo resolveInfo2;
        AuxiliaryResolveInfo auxiliaryResolveInfo = null;
        if (!((i & DumpState.DUMP_VOLUMES) != 0)) {
            intent2 = intent;
            str2 = str;
            List<ResolveInfo> listQueryIntent = this.mActivities.queryIntent(intent2, str2, i | 64 | DumpState.DUMP_VOLUMES | DumpState.DUMP_SERVICE_PERMISSIONS, i2);
            for (int size = listQueryIntent.size() - 1; size >= 0; size--) {
                resolveInfo = listQueryIntent.get(size);
                String str3 = resolveInfo.activityInfo.packageName;
                PackageSetting packageSetting2 = this.mSettings.mPackages.get(str3);
                if (packageSetting2.getInstantApp(i2)) {
                    if (((int) (getDomainVerificationStatusLPr(packageSetting2, i2) >> 32)) == 3) {
                        if (DEBUG_INSTANT) {
                            Slog.v(TAG, "Instant app marked to never run; pkg: " + str3);
                        }
                        resolveInfo = null;
                        z2 = true;
                        if (!z2) {
                            if (resolveInfo == null) {
                                Trace.traceBegin(262144L, "resolveEphemeral");
                                auxiliaryResolveInfo = InstantAppResolver.doInstantAppResolutionPhaseOne(this.mInstantAppResolverConnection, new InstantAppRequest((AuxiliaryResolveInfo) null, intent2, str2, (String) null, i2, (Bundle) null, z));
                                Trace.traceEnd(262144L);
                            } else {
                                ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
                                auxiliaryResolveInfo = new AuxiliaryResolveInfo((ComponentName) null, applicationInfo.packageName, applicationInfo.longVersionCode, (String) null);
                            }
                        }
                        if ((!intent.isWebIntent() && auxiliaryResolveInfo == null) || (packageSetting = this.mSettings.mPackages.get(this.mInstantAppInstallerActivity.packageName)) == null || packageSetting.getUserState().get(i2) == null || !packageSetting.getUserState().get(i2).isEnabled(this.mInstantAppInstallerActivity, 0)) {
                            return list;
                        }
                        resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
                        resolveInfo2.activityInfo = PackageParser.generateActivityInfo(this.mInstantAppInstallerActivity, 0, packageSetting.readUserState(i2), i2);
                        resolveInfo2.match = 5799936;
                        resolveInfo2.filter = new IntentFilter();
                        if (intent.getAction() != null) {
                            resolveInfo2.filter.addAction(intent.getAction());
                        }
                        if (intent.getData() != null && intent.getData().getPath() != null) {
                            resolveInfo2.filter.addDataPath(intent.getData().getPath(), 0);
                        }
                        resolveInfo2.isInstantAppAvailable = true;
                        resolveInfo2.isDefault = true;
                        resolveInfo2.auxiliaryInfo = auxiliaryResolveInfo;
                        if (DEBUG_INSTANT) {
                            Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
                        }
                        list.add(resolveInfo2);
                        return list;
                    }
                    if (DEBUG_INSTANT) {
                        Slog.v(TAG, "Found installed instant app; pkg: " + str3);
                    }
                    z2 = false;
                    if (!z2) {
                    }
                    if (!intent.isWebIntent()) {
                    }
                    resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
                    resolveInfo2.activityInfo = PackageParser.generateActivityInfo(this.mInstantAppInstallerActivity, 0, packageSetting.readUserState(i2), i2);
                    resolveInfo2.match = 5799936;
                    resolveInfo2.filter = new IntentFilter();
                    if (intent.getAction() != null) {
                    }
                    if (intent.getData() != null) {
                        resolveInfo2.filter.addDataPath(intent.getData().getPath(), 0);
                    }
                    resolveInfo2.isInstantAppAvailable = true;
                    resolveInfo2.isDefault = true;
                    resolveInfo2.auxiliaryInfo = auxiliaryResolveInfo;
                    if (DEBUG_INSTANT) {
                    }
                    list.add(resolveInfo2);
                    return list;
                }
            }
        } else {
            intent2 = intent;
            str2 = str;
        }
        resolveInfo = null;
        z2 = false;
        if (!z2) {
        }
        if (!intent.isWebIntent()) {
        }
        resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
        resolveInfo2.activityInfo = PackageParser.generateActivityInfo(this.mInstantAppInstallerActivity, 0, packageSetting.readUserState(i2), i2);
        resolveInfo2.match = 5799936;
        resolveInfo2.filter = new IntentFilter();
        if (intent.getAction() != null) {
        }
        if (intent.getData() != null) {
        }
        resolveInfo2.isInstantAppAvailable = true;
        resolveInfo2.isDefault = true;
        resolveInfo2.auxiliaryInfo = auxiliaryResolveInfo;
        if (DEBUG_INSTANT) {
        }
        list.add(resolveInfo2);
        return list;
    }

    private static class CrossProfileDomainInfo {
        int bestDomainVerificationStatus;
        ResolveInfo resolveInfo;

        private CrossProfileDomainInfo() {
        }
    }

    private CrossProfileDomainInfo getCrossProfileDomainPreferredLpr(Intent intent, String str, int i, int i2, int i3) {
        List<ResolveInfo> listQueryIntent;
        if (!sUserManager.hasUserRestriction("allow_parent_profile_app_linking", i2) || (listQueryIntent = this.mActivities.queryIntent(intent, str, i, i3)) == null || listQueryIntent.isEmpty()) {
            return null;
        }
        int size = listQueryIntent.size();
        CrossProfileDomainInfo crossProfileDomainInfo = null;
        for (int i4 = 0; i4 < size; i4++) {
            ResolveInfo resolveInfo = listQueryIntent.get(i4);
            if (!resolveInfo.handleAllWebDataURI) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(resolveInfo.activityInfo.packageName);
                if (packageSetting != null) {
                    int domainVerificationStatusLPr = (int) (getDomainVerificationStatusLPr(packageSetting, i3) >> 32);
                    if (crossProfileDomainInfo == null) {
                        crossProfileDomainInfo = new CrossProfileDomainInfo();
                        crossProfileDomainInfo.resolveInfo = createForwardingResolveInfoUnchecked(new IntentFilter(), i2, i3);
                        crossProfileDomainInfo.bestDomainVerificationStatus = domainVerificationStatusLPr;
                    } else {
                        crossProfileDomainInfo.bestDomainVerificationStatus = bestDomainVerificationStatus(domainVerificationStatusLPr, crossProfileDomainInfo.bestDomainVerificationStatus);
                    }
                }
            }
        }
        if (crossProfileDomainInfo == null || crossProfileDomainInfo.bestDomainVerificationStatus != 3) {
            return crossProfileDomainInfo;
        }
        return null;
    }

    private int bestDomainVerificationStatus(int i, int i2) {
        if (i == 3) {
            return i2;
        }
        if (i2 == 3) {
            return i;
        }
        return (int) MathUtils.max(i, i2);
    }

    private boolean isUserEnabled(int i) {
        boolean z;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = sUserManager.getUserInfo(i);
            if (userInfo != null) {
                z = userInfo.isEnabled();
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<ResolveInfo> filterIfNotSystemUser(List<ResolveInfo> list, int i) {
        if (i == 0) {
            return list;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            if ((list.get(size).activityInfo.flags & 536870912) != 0) {
                list.remove(size);
            }
        }
        return list;
    }

    private List<ResolveInfo> applyPostResolutionFilter(List<ResolveInfo> list, String str, boolean z, int i, boolean z2, int i2, Intent intent) {
        boolean z3 = intent.isWebIntent() && areWebInstantAppsDisabled();
        int size = list.size() - 1;
        while (size >= 0) {
            ResolveInfo resolveInfo = list.get(size);
            if (resolveInfo.isInstantAppAvailable && z3) {
                list.remove(size);
            } else {
                if (!z || resolveInfo.activityInfo == null || resolveInfo.activityInfo.splitName == null || ArrayUtils.contains(resolveInfo.activityInfo.applicationInfo.splitNames, resolveInfo.activityInfo.splitName)) {
                    if (str != null && !str.equals(resolveInfo.activityInfo.packageName) && ((!z2 || ((!intent.isWebIntent() && (intent.getFlags() & 2048) == 0) || intent.getPackage() != null || intent.getComponent() != null)) && (resolveInfo.activityInfo.applicationInfo.isInstantApp() || (resolveInfo.activityInfo.flags & 1048576) == 0))) {
                        list.remove(size);
                    }
                } else if (this.mInstantAppInstallerActivity == null) {
                    if (DEBUG_INSTALL) {
                        Slog.v(TAG, "No installer - not adding it to the ResolveInfo list");
                    }
                    list.remove(size);
                } else if (z3 && isInstantApp(resolveInfo.activityInfo.packageName, i2)) {
                    list.remove(size);
                } else {
                    if (DEBUG_INSTALL) {
                        Slog.v(TAG, "Adding installer to the ResolveInfo list");
                    }
                    ResolveInfo resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
                    resolveInfo2.auxiliaryInfo = new AuxiliaryResolveInfo(findInstallFailureActivity(resolveInfo.activityInfo.packageName, i, i2), resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.applicationInfo.longVersionCode, resolveInfo.activityInfo.splitName);
                    resolveInfo2.filter = new IntentFilter();
                    resolveInfo2.resolvePackageName = resolveInfo.getComponentInfo().packageName;
                    resolveInfo2.labelRes = resolveInfo.resolveLabelResId();
                    resolveInfo2.icon = resolveInfo.resolveIconResId();
                    resolveInfo2.isInstantAppAvailable = true;
                    size = size;
                    list.set(size, resolveInfo2);
                }
                size--;
            }
            size--;
        }
        return list;
    }

    private ComponentName findInstallFailureActivity(String str, int i, int i2) {
        Intent intent = new Intent("android.intent.action.INSTALL_FAILURE");
        intent.setPackage(str);
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intent, null, 0, i, i2, false, false);
        int size = listQueryIntentActivitiesInternal.size();
        if (size > 0) {
            for (int i3 = 0; i3 < size; i3++) {
                ResolveInfo resolveInfo = listQueryIntentActivitiesInternal.get(i3);
                if (resolveInfo.activityInfo.splitName == null) {
                    return new ComponentName(str, resolveInfo.activityInfo.name);
                }
            }
            return null;
        }
        return null;
    }

    private boolean hasNonNegativePriority(List<ResolveInfo> list) {
        return list.size() > 0 && list.get(0).priority >= 0;
    }

    private List<ResolveInfo> filterCandidatesWithDomainPreferredActivitiesLPr(Intent intent, int i, List<ResolveInfo> list, CrossProfileDomainInfo crossProfileDomainInfo, int i2) throws Throwable {
        boolean z;
        int i3;
        ArrayList arrayList;
        ArrayMap<String, PackageParser.Package> arrayMap;
        PackageManagerService packageManagerService = this;
        List<ResolveInfo> list2 = list;
        boolean z2 = (intent.getFlags() & 8) != 0;
        if (DEBUG_PREFERRED || DEBUG_DOMAIN_VERIFICATION) {
            Slog.v(TAG, "Filtering results with preferred activities. Candidates count: " + list.size());
        }
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        ArrayList arrayList5 = new ArrayList();
        ArrayList arrayList6 = new ArrayList();
        ArrayList arrayList7 = new ArrayList();
        ArrayMap<String, PackageParser.Package> arrayMap2 = packageManagerService.mPackages;
        synchronized (arrayMap2) {
            try {
                try {
                    int size = list.size();
                    int i4 = 0;
                    while (i4 < size) {
                        ResolveInfo resolveInfo = list2.get(i4);
                        int i5 = size;
                        PackageSetting packageSetting = packageManagerService.mSettings.mPackages.get(resolveInfo.activityInfo.packageName);
                        if (packageSetting == null) {
                            arrayList = arrayList7;
                            arrayMap = arrayMap2;
                        } else if (resolveInfo.handleAllWebDataURI) {
                            arrayList7.add(resolveInfo);
                            arrayList = arrayList7;
                            arrayMap = arrayMap2;
                        } else {
                            long domainVerificationStatusLPr = packageManagerService.getDomainVerificationStatusLPr(packageSetting, i2);
                            int i6 = (int) (domainVerificationStatusLPr >> 32);
                            arrayList = arrayList7;
                            arrayMap = arrayMap2;
                            int i7 = (int) (domainVerificationStatusLPr & (-1));
                            if (i6 == 2) {
                                if (DEBUG_DOMAIN_VERIFICATION || z2) {
                                    Slog.i(TAG, "  + always: " + resolveInfo.activityInfo.packageName + " : linkgen=" + i7);
                                }
                                resolveInfo.preferredOrder = i7;
                                arrayList3.add(resolveInfo);
                            } else if (i6 == 3) {
                                if (DEBUG_DOMAIN_VERIFICATION || z2) {
                                    Slog.i(TAG, "  + never: " + resolveInfo.activityInfo.packageName);
                                }
                                arrayList6.add(resolveInfo);
                            } else if (i6 == 4) {
                                if (DEBUG_DOMAIN_VERIFICATION || z2) {
                                    Slog.i(TAG, "  + always-ask: " + resolveInfo.activityInfo.packageName);
                                }
                                arrayList5.add(resolveInfo);
                            } else if (i6 == 0 || i6 == 1) {
                                if (DEBUG_DOMAIN_VERIFICATION || z2) {
                                    Slog.i(TAG, "  + ask: " + resolveInfo.activityInfo.packageName);
                                }
                                arrayList4.add(resolveInfo);
                            }
                        }
                        i4++;
                        size = i5;
                        arrayList7 = arrayList;
                        arrayMap2 = arrayMap;
                        packageManagerService = this;
                        list2 = list;
                    }
                    ArrayList arrayList8 = arrayList7;
                    ArrayMap<String, PackageParser.Package> arrayMap3 = arrayMap2;
                    boolean z3 = true;
                    if (arrayList3.size() > 0) {
                        arrayList2.addAll(arrayList3);
                        z = false;
                    } else {
                        arrayList2.addAll(arrayList4);
                        if (crossProfileDomainInfo != null && crossProfileDomainInfo.bestDomainVerificationStatus != 3) {
                            arrayList2.add(crossProfileDomainInfo.resolveInfo);
                        }
                        z = true;
                    }
                    if (arrayList5.size() > 0) {
                        Iterator it = arrayList2.iterator();
                        while (it.hasNext()) {
                            ((ResolveInfo) it.next()).preferredOrder = 0;
                        }
                        i3 = 0;
                        arrayList2.addAll(arrayList5);
                    } else {
                        i3 = 0;
                        z3 = z;
                    }
                    if (z3) {
                        if (DEBUG_DOMAIN_VERIFICATION) {
                            Slog.v(TAG, "   ...including browsers in candidate set");
                        }
                        if ((i & 131072) == 0) {
                            String defaultBrowserPackageName = getDefaultBrowserPackageName(i2);
                            ResolveInfo resolveInfo2 = null;
                            int size2 = arrayList8.size();
                            int i8 = i3;
                            while (i3 < size2) {
                                ResolveInfo resolveInfo3 = (ResolveInfo) arrayList8.get(i3);
                                if (resolveInfo3.priority > i8) {
                                    i8 = resolveInfo3.priority;
                                }
                                if (resolveInfo3.activityInfo.packageName.equals(defaultBrowserPackageName) && (resolveInfo2 == null || resolveInfo2.priority < resolveInfo3.priority)) {
                                    if (z2) {
                                        Slog.v(TAG, "Considering default browser match " + resolveInfo3);
                                    }
                                    resolveInfo2 = resolveInfo3;
                                }
                                i3++;
                            }
                            if (resolveInfo2 != null && resolveInfo2.priority >= i8 && !TextUtils.isEmpty(defaultBrowserPackageName)) {
                                if (z2) {
                                    Slog.v(TAG, "Default browser match " + resolveInfo2);
                                }
                                arrayList2.add(resolveInfo2);
                            } else {
                                arrayList2.addAll(arrayList8);
                            }
                        } else {
                            arrayList2.addAll(arrayList8);
                        }
                        if (arrayList2.size() == 0) {
                            arrayList2.addAll(list);
                            arrayList2.removeAll(arrayList6);
                        }
                    }
                    if (DEBUG_PREFERRED || DEBUG_DOMAIN_VERIFICATION) {
                        Slog.v(TAG, "Filtered results with preferred activities. New candidates count: " + arrayList2.size());
                        Iterator it2 = arrayList2.iterator();
                        while (it2.hasNext()) {
                            Slog.v(TAG, "  + " + ((ResolveInfo) it2.next()).activityInfo);
                        }
                    }
                    return arrayList2;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                ArrayMap<String, PackageParser.Package> arrayMap4 = arrayMap2;
                throw th;
            }
        }
    }

    private long getDomainVerificationStatusLPr(PackageSetting packageSetting, int i) {
        long domainVerificationStatusForUser = packageSetting.getDomainVerificationStatusForUser(i);
        if ((domainVerificationStatusForUser >> 32) == 0 && packageSetting.getIntentFilterVerificationInfo() != null) {
            return ((long) packageSetting.getIntentFilterVerificationInfo().getStatus()) << 32;
        }
        return domainVerificationStatusForUser;
    }

    private ResolveInfo querySkipCurrentProfileIntents(List<CrossProfileIntentFilter> list, Intent intent, String str, int i, int i2) {
        ResolveInfo resolveInfoCreateForwardingResolveInfo;
        if (list != null) {
            int size = list.size();
            for (int i3 = 0; i3 < size; i3++) {
                CrossProfileIntentFilter crossProfileIntentFilter = list.get(i3);
                if ((crossProfileIntentFilter.getFlags() & 2) != 0 && (resolveInfoCreateForwardingResolveInfo = createForwardingResolveInfo(crossProfileIntentFilter, intent, str, i, i2)) != null) {
                    return resolveInfoCreateForwardingResolveInfo;
                }
            }
            return null;
        }
        return null;
    }

    private ResolveInfo queryCrossProfileIntents(List<CrossProfileIntentFilter> list, Intent intent, String str, int i, int i2, boolean z) {
        if (list != null) {
            SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
            int size = list.size();
            for (int i3 = 0; i3 < size; i3++) {
                CrossProfileIntentFilter crossProfileIntentFilter = list.get(i3);
                int targetUserId = crossProfileIntentFilter.getTargetUserId();
                boolean z2 = (crossProfileIntentFilter.getFlags() & 2) != 0;
                boolean z3 = (crossProfileIntentFilter.getFlags() & 4) != 0;
                if (!z2 && !sparseBooleanArray.get(targetUserId) && (!z3 || !z)) {
                    ResolveInfo resolveInfoCreateForwardingResolveInfo = createForwardingResolveInfo(crossProfileIntentFilter, intent, str, i, i2);
                    if (resolveInfoCreateForwardingResolveInfo != null) {
                        return resolveInfoCreateForwardingResolveInfo;
                    }
                    sparseBooleanArray.put(targetUserId, true);
                }
            }
            return null;
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfo(CrossProfileIntentFilter crossProfileIntentFilter, Intent intent, String str, int i, int i2) {
        int targetUserId = crossProfileIntentFilter.getTargetUserId();
        List<ResolveInfo> listQueryIntent = this.mActivities.queryIntent(intent, str, i, targetUserId);
        if (listQueryIntent != null && isUserEnabled(targetUserId)) {
            for (int size = listQueryIntent.size() - 1; size >= 0; size--) {
                if ((listQueryIntent.get(size).activityInfo.applicationInfo.flags & 1073741824) == 0) {
                    return createForwardingResolveInfoUnchecked(crossProfileIntentFilter, i2, targetUserId);
                }
            }
            return null;
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfoUnchecked(IntentFilter intentFilter, int i, int i2) {
        String str;
        ResolveInfo resolveInfo = new ResolveInfo();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            boolean zIsManagedProfile = sUserManager.getUserInfo(i2).isManagedProfile();
            if (zIsManagedProfile) {
                str = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE;
            } else {
                str = IntentForwarderActivity.FORWARD_INTENT_TO_PARENT;
            }
            ActivityInfo activityInfo = getActivityInfo(new ComponentName(this.mAndroidApplication.packageName, str), 0, i);
            if (!zIsManagedProfile) {
                activityInfo.showUserIcon = i2;
                resolveInfo.noResourceId = true;
            }
            resolveInfo.activityInfo = activityInfo;
            resolveInfo.priority = 0;
            resolveInfo.preferredOrder = 0;
            resolveInfo.match = 0;
            resolveInfo.isDefault = true;
            resolveInfo.filter = intentFilter;
            resolveInfo.targetUserId = i2;
            return resolveInfo;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivityOptions(ComponentName componentName, Intent[] intentArr, String[] strArr, Intent intent, String str, int i, int i2) {
        return new ParceledListSlice<>(queryIntentActivityOptionsInternal(componentName, intentArr, strArr, intent, str, i, i2));
    }

    private List<ResolveInfo> queryIntentActivityOptionsInternal(ComponentName componentName, Intent[] intentArr, String[] strArr, Intent intent, String str, int i, int i2) {
        int i3;
        Iterator<String> itActionsIterator;
        ComponentName componentName2;
        ResolveInfo resolveInfo;
        ActivityInfo activityInfo;
        int size;
        int i4;
        PackageManagerService packageManagerService = this;
        Intent[] intentArr2 = intentArr;
        if (!sUserManager.exists(i2)) {
            return Collections.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        int iUpdateFlagsForResolve = packageManagerService.updateFlagsForResolve(i, i2, intent, callingUid, false);
        packageManagerService.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "query intent activity options");
        String action = intent.getAction();
        List<ResolveInfo> listQueryIntentActivitiesInternal = packageManagerService.queryIntentActivitiesInternal(intent, str, iUpdateFlagsForResolve | 64, i2);
        if (DEBUG_INTENT_MATCHING) {
            Log.v(TAG, "Query " + intent + ": " + listQueryIntentActivitiesInternal);
        }
        if (intentArr2 != null) {
            int i5 = 0;
            i3 = 0;
            while (i5 < intentArr2.length) {
                Intent intent2 = intentArr2[i5];
                if (intent2 != null) {
                    if (DEBUG_INTENT_MATCHING) {
                        Log.v(TAG, "Specific #" + i5 + ": " + intent2);
                    }
                    String action2 = intent2.getAction();
                    if (action != null && action.equals(action2)) {
                        action2 = null;
                    }
                    ComponentName component = intent2.getComponent();
                    if (component == null) {
                        resolveInfo = packageManagerService.resolveIntent(intent2, strArr != null ? strArr[i5] : null, iUpdateFlagsForResolve, i2);
                        if (resolveInfo != null) {
                            ResolveInfo resolveInfo2 = packageManagerService.mResolveInfo;
                            activityInfo = resolveInfo.activityInfo;
                            componentName2 = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
                            if (DEBUG_INTENT_MATCHING) {
                                Log.v(TAG, "Specific #" + i5 + ": " + activityInfo);
                            }
                            size = listQueryIntentActivitiesInternal.size();
                            i4 = i3;
                            while (i4 < size) {
                                ResolveInfo resolveInfo3 = listQueryIntentActivitiesInternal.get(i4);
                                if ((resolveInfo3.activityInfo.name.equals(componentName2.getClassName()) && resolveInfo3.activityInfo.applicationInfo.packageName.equals(componentName2.getPackageName())) || (action2 != null && resolveInfo3.filter.matchAction(action2))) {
                                    listQueryIntentActivitiesInternal.remove(i4);
                                    if (DEBUG_INTENT_MATCHING) {
                                        Log.v(TAG, "Removing duplicate item from " + i4 + " due to specific " + i3);
                                    }
                                    if (resolveInfo == null) {
                                        resolveInfo = resolveInfo3;
                                    }
                                    i4--;
                                    size--;
                                }
                                i4++;
                            }
                            if (resolveInfo == null) {
                                resolveInfo = new ResolveInfo();
                                resolveInfo.activityInfo = activityInfo;
                            }
                            listQueryIntentActivitiesInternal.add(i3, resolveInfo);
                            resolveInfo.specificIndex = i5;
                            i3++;
                        }
                    } else {
                        ActivityInfo activityInfo2 = packageManagerService.getActivityInfo(component, iUpdateFlagsForResolve, i2);
                        if (activityInfo2 != null) {
                            componentName2 = component;
                            resolveInfo = null;
                            activityInfo = activityInfo2;
                            if (DEBUG_INTENT_MATCHING) {
                            }
                            size = listQueryIntentActivitiesInternal.size();
                            i4 = i3;
                            while (i4 < size) {
                            }
                            if (resolveInfo == null) {
                            }
                            listQueryIntentActivitiesInternal.add(i3, resolveInfo);
                            resolveInfo.specificIndex = i5;
                            i3++;
                        }
                    }
                }
                i5++;
                packageManagerService = this;
                intentArr2 = intentArr;
            }
        } else {
            i3 = 0;
        }
        int size2 = listQueryIntentActivitiesInternal.size();
        while (i3 < size2 - 1) {
            ResolveInfo resolveInfo4 = listQueryIntentActivitiesInternal.get(i3);
            if (resolveInfo4.filter != null && (itActionsIterator = resolveInfo4.filter.actionsIterator()) != null) {
                while (itActionsIterator.hasNext()) {
                    String next = itActionsIterator.next();
                    if (action == null || !action.equals(next)) {
                        int i6 = i3 + 1;
                        while (i6 < size2) {
                            ResolveInfo resolveInfo5 = listQueryIntentActivitiesInternal.get(i6);
                            if (resolveInfo5.filter != null && resolveInfo5.filter.hasAction(next)) {
                                listQueryIntentActivitiesInternal.remove(i6);
                                if (DEBUG_INTENT_MATCHING) {
                                    Log.v(TAG, "Removing duplicate item from " + i6 + " due to action " + next + " at " + i3);
                                }
                                i6--;
                                size2--;
                            }
                            i6++;
                        }
                    }
                }
                if ((iUpdateFlagsForResolve & 64) == 0) {
                    resolveInfo4.filter = null;
                }
            }
            i3++;
        }
        if (componentName != null) {
            int size3 = listQueryIntentActivitiesInternal.size();
            int i7 = 0;
            while (true) {
                if (i7 >= size3) {
                    break;
                }
                ActivityInfo activityInfo3 = listQueryIntentActivitiesInternal.get(i7).activityInfo;
                if (!componentName.getPackageName().equals(activityInfo3.applicationInfo.packageName) || !componentName.getClassName().equals(activityInfo3.name)) {
                    i7++;
                } else {
                    listQueryIntentActivitiesInternal.remove(i7);
                    break;
                }
            }
        }
        if ((iUpdateFlagsForResolve & 64) == 0) {
            int size4 = listQueryIntentActivitiesInternal.size();
            for (int i8 = 0; i8 < size4; i8++) {
                listQueryIntentActivitiesInternal.get(i8).filter = null;
            }
        }
        if (DEBUG_INTENT_MATCHING) {
            Log.v(TAG, "Result: " + listQueryIntentActivitiesInternal);
        }
        return listQueryIntentActivitiesInternal;
    }

    public ParceledListSlice<ResolveInfo> queryIntentReceivers(Intent intent, String str, int i, int i2) {
        return new ParceledListSlice<>(queryIntentReceiversInternal(intent, str, i, i2, false));
    }

    private List<ResolveInfo> queryIntentReceiversInternal(Intent intent, String str, int i, int i2, boolean z) {
        Intent intent2;
        if (!sUserManager.exists(i2)) {
            return Collections.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "query intent receivers");
        String instantAppPackageName = getInstantAppPackageName(callingUid);
        int iUpdateFlagsForResolve = updateFlagsForResolve(i, i2, intent, callingUid, false);
        ComponentName component = intent.getComponent();
        if (component != null || intent.getSelector() == null) {
            intent2 = intent;
        } else {
            Intent selector = intent.getSelector();
            intent2 = selector;
            component = selector.getComponent();
        }
        if (component != null) {
            ArrayList arrayList = new ArrayList(1);
            ActivityInfo receiverInfo = getReceiverInfo(component, iUpdateFlagsForResolve, i2);
            if (receiverInfo != null) {
                boolean z2 = false;
                boolean z3 = (8388608 & iUpdateFlagsForResolve) != 0;
                boolean z4 = (16777216 & iUpdateFlagsForResolve) != 0;
                boolean z5 = (iUpdateFlagsForResolve & 33554432) != 0;
                boolean z6 = instantAppPackageName != null;
                boolean zEquals = component.getPackageName().equals(instantAppPackageName);
                boolean z7 = (receiverInfo.applicationInfo.privateFlags & 128) != 0;
                boolean z8 = (receiverInfo.flags & 1048576) != 0;
                boolean z9 = !z8 || (z5 && !(z8 && (receiverInfo.flags & 2097152) == 0));
                if (!zEquals && ((!z3 && !z6 && z7) || (z4 && z6 && z9))) {
                    z2 = true;
                }
                if (!z2) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = receiverInfo;
                    arrayList.add(resolveInfo);
                }
            }
            return applyPostResolutionFilter(arrayList, instantAppPackageName, z, callingUid, false, i2, intent2);
        }
        synchronized (this.mPackages) {
            String str2 = intent2.getPackage();
            if (str2 == null) {
                return applyPostResolutionFilter(this.mReceivers.queryIntent(intent2, str, iUpdateFlagsForResolve, i2), instantAppPackageName, z, callingUid, false, i2, intent2);
            }
            PackageParser.Package r1 = this.mPackages.get(str2);
            if (r1 != null) {
                return applyPostResolutionFilter(this.mReceivers.queryIntentForPackage(intent2, str, iUpdateFlagsForResolve, r1.receivers, i2), instantAppPackageName, z, callingUid, false, i2, intent2);
            }
            return Collections.emptyList();
        }
    }

    public ResolveInfo resolveService(Intent intent, String str, int i, int i2) {
        return resolveServiceInternal(intent, str, i, i2, Binder.getCallingUid());
    }

    private ResolveInfo resolveServiceInternal(Intent intent, String str, int i, int i2, int i3) {
        List<ResolveInfo> listQueryIntentServicesInternal;
        if (sUserManager.exists(i2) && (listQueryIntentServicesInternal = queryIntentServicesInternal(intent, str, updateFlagsForResolve(i, i2, intent, i3, false), i2, i3, false)) != null && listQueryIntentServicesInternal.size() >= 1) {
            return listQueryIntentServicesInternal.get(0);
        }
        return null;
    }

    public ParceledListSlice<ResolveInfo> queryIntentServices(Intent intent, String str, int i, int i2) {
        return new ParceledListSlice<>(queryIntentServicesInternal(intent, str, i, i2, Binder.getCallingUid(), false));
    }

    private List<ResolveInfo> queryIntentServicesInternal(Intent intent, String str, int i, int i2, int i3, boolean z) {
        if (!sUserManager.exists(i2)) {
            return Collections.emptyList();
        }
        this.mPermissionManager.enforceCrossUserPermission(i3, i2, false, false, "query intent receivers");
        String instantAppPackageName = getInstantAppPackageName(i3);
        int iUpdateFlagsForResolve = updateFlagsForResolve(i, i2, intent, i3, z);
        ComponentName component = intent.getComponent();
        if (component == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            component = intent.getComponent();
        }
        Intent intent2 = intent;
        if (component != null) {
            boolean z2 = true;
            ArrayList arrayList = new ArrayList(1);
            ServiceInfo serviceInfo = getServiceInfo(component, iUpdateFlagsForResolve, i2);
            if (serviceInfo != null) {
                boolean z3 = (8388608 & iUpdateFlagsForResolve) != 0;
                boolean z4 = (16777216 & iUpdateFlagsForResolve) != 0;
                boolean z5 = instantAppPackageName != null;
                boolean zEquals = component.getPackageName().equals(instantAppPackageName);
                boolean z6 = (serviceInfo.applicationInfo.privateFlags & 128) != 0;
                boolean z7 = (serviceInfo.flags & 1048576) == 0;
                if (zEquals || ((z3 || z5 || !z6) && (!z4 || !z5 || !z7))) {
                    z2 = false;
                }
                if (!z2) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.serviceInfo = serviceInfo;
                    arrayList.add(resolveInfo);
                }
            }
            return arrayList;
        }
        synchronized (this.mPackages) {
            String str2 = intent2.getPackage();
            if (str2 == null) {
                return applyPostServiceResolutionFilter(this.mServices.queryIntent(intent2, str, iUpdateFlagsForResolve, i2), instantAppPackageName);
            }
            PackageParser.Package r11 = this.mPackages.get(str2);
            if (r11 != null) {
                return applyPostServiceResolutionFilter(this.mServices.queryIntentForPackage(intent2, str, iUpdateFlagsForResolve, r11.services, i2), instantAppPackageName);
            }
            return Collections.emptyList();
        }
    }

    private List<ResolveInfo> applyPostServiceResolutionFilter(List<ResolveInfo> list, String str) {
        if (str == null) {
            return list;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            ResolveInfo resolveInfo = list.get(size);
            boolean zIsInstantApp = resolveInfo.serviceInfo.applicationInfo.isInstantApp();
            if (zIsInstantApp && str.equals(resolveInfo.serviceInfo.packageName)) {
                if (resolveInfo.serviceInfo.splitName != null && !ArrayUtils.contains(resolveInfo.serviceInfo.applicationInfo.splitNames, resolveInfo.serviceInfo.splitName)) {
                    if (DEBUG_INSTANT) {
                        Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
                    }
                    ResolveInfo resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
                    resolveInfo2.auxiliaryInfo = new AuxiliaryResolveInfo((ComponentName) null, resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.applicationInfo.longVersionCode, resolveInfo.serviceInfo.splitName);
                    resolveInfo2.filter = new IntentFilter();
                    resolveInfo2.resolvePackageName = resolveInfo.getComponentInfo().packageName;
                    list.set(size, resolveInfo2);
                }
            } else if (zIsInstantApp || (resolveInfo.serviceInfo.flags & 1048576) == 0) {
                list.remove(size);
            }
        }
        return list;
    }

    public ParceledListSlice<ResolveInfo> queryIntentContentProviders(Intent intent, String str, int i, int i2) {
        return new ParceledListSlice<>(queryIntentContentProvidersInternal(intent, str, i, i2));
    }

    private List<ResolveInfo> queryIntentContentProvidersInternal(Intent intent, String str, int i, int i2) {
        Intent intent2;
        if (!sUserManager.exists(i2)) {
            return Collections.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        String instantAppPackageName = getInstantAppPackageName(callingUid);
        int iUpdateFlagsForResolve = updateFlagsForResolve(i, i2, intent, callingUid, false);
        ComponentName component = intent.getComponent();
        if (component != null || intent.getSelector() == null) {
            intent2 = intent;
        } else {
            Intent selector = intent.getSelector();
            intent2 = selector;
            component = selector.getComponent();
        }
        if (component != null) {
            boolean z = true;
            ArrayList arrayList = new ArrayList(1);
            ProviderInfo providerInfo = getProviderInfo(component, iUpdateFlagsForResolve, i2);
            if (providerInfo != null) {
                boolean z2 = (8388608 & iUpdateFlagsForResolve) != 0;
                boolean z3 = (iUpdateFlagsForResolve & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
                boolean z4 = instantAppPackageName != null;
                boolean zEquals = component.getPackageName().equals(instantAppPackageName);
                boolean z5 = (providerInfo.applicationInfo.privateFlags & 128) != 0;
                boolean z6 = (providerInfo.flags & 1048576) == 0;
                if (zEquals || ((z2 || z4 || !z5) && (!z3 || !z4 || !z6))) {
                    z = false;
                }
                if (!z) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.providerInfo = providerInfo;
                    arrayList.add(resolveInfo);
                }
            }
            return arrayList;
        }
        synchronized (this.mPackages) {
            String str2 = intent2.getPackage();
            if (str2 == null) {
                return applyPostContentProviderResolutionFilter(this.mProviders.queryIntent(intent2, str, iUpdateFlagsForResolve, i2), instantAppPackageName);
            }
            PackageParser.Package r1 = this.mPackages.get(str2);
            if (r1 != null) {
                return applyPostContentProviderResolutionFilter(this.mProviders.queryIntentForPackage(intent2, str, iUpdateFlagsForResolve, r1.providers, i2), instantAppPackageName);
            }
            return Collections.emptyList();
        }
    }

    private List<ResolveInfo> applyPostContentProviderResolutionFilter(List<ResolveInfo> list, String str) {
        if (str == null) {
            return list;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            ResolveInfo resolveInfo = list.get(size);
            boolean zIsInstantApp = resolveInfo.providerInfo.applicationInfo.isInstantApp();
            if (zIsInstantApp && str.equals(resolveInfo.providerInfo.packageName)) {
                if (resolveInfo.providerInfo.splitName != null && !ArrayUtils.contains(resolveInfo.providerInfo.applicationInfo.splitNames, resolveInfo.providerInfo.splitName)) {
                    if (DEBUG_INSTANT) {
                        Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
                    }
                    ResolveInfo resolveInfo2 = new ResolveInfo(this.mInstantAppInstallerInfo);
                    resolveInfo2.auxiliaryInfo = new AuxiliaryResolveInfo((ComponentName) null, resolveInfo.providerInfo.packageName, resolveInfo.providerInfo.applicationInfo.longVersionCode, resolveInfo.providerInfo.splitName);
                    resolveInfo2.filter = new IntentFilter();
                    resolveInfo2.resolvePackageName = resolveInfo.getComponentInfo().packageName;
                    list.set(size, resolveInfo2);
                }
            } else if (zIsInstantApp || (resolveInfo.providerInfo.flags & 1048576) == 0) {
                list.remove(size);
            }
        }
        return list;
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int i, int i2) {
        ArrayList arrayList;
        ParceledListSlice<PackageInfo> parceledListSlice;
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        if (!sUserManager.exists(i2)) {
            return ParceledListSlice.emptyList();
        }
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i2, null);
        boolean z = (4202496 & iUpdateFlagsForPackage) != 0;
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "get installed packages");
        synchronized (this.mPackages) {
            try {
                if (z) {
                    arrayList = new ArrayList(this.mSettings.mPackages.size());
                    for (PackageSetting packageSetting : this.mSettings.mPackages.values()) {
                        if (!filterSharedLibPackageLPr(packageSetting, callingUid, i2, iUpdateFlagsForPackage) && !filterAppAccessLPr(packageSetting, callingUid, i2)) {
                            PackageInfo packageInfoGeneratePackageInfo = generatePackageInfo(packageSetting, iUpdateFlagsForPackage, i2);
                            if (packageInfoGeneratePackageInfo != null) {
                                arrayList.add(packageInfoGeneratePackageInfo);
                            }
                        }
                    }
                } else {
                    arrayList = new ArrayList(this.mPackages.size());
                    for (PackageParser.Package r3 : this.mPackages.values()) {
                        PackageSetting packageSetting2 = (PackageSetting) r3.mExtras;
                        if (!filterSharedLibPackageLPr(packageSetting2, callingUid, i2, iUpdateFlagsForPackage) && !filterAppAccessLPr(packageSetting2, callingUid, i2)) {
                            PackageInfo packageInfoGeneratePackageInfo2 = generatePackageInfo((PackageSetting) r3.mExtras, iUpdateFlagsForPackage, i2);
                            if (packageInfoGeneratePackageInfo2 != null) {
                                arrayList.add(packageInfoGeneratePackageInfo2);
                            }
                        }
                    }
                }
                parceledListSlice = new ParceledListSlice<>(arrayList);
            } catch (Throwable th) {
                throw th;
            }
        }
        return parceledListSlice;
    }

    private void addPackageHoldingPermissions(ArrayList<PackageInfo> arrayList, PackageSetting packageSetting, String[] strArr, boolean[] zArr, int i, int i2) {
        PackageInfo packageInfoGeneratePackageInfo;
        int i3 = 0;
        for (int i4 = 0; i4 < strArr.length; i4++) {
            if (checkPermission(strArr[i4], packageSetting.name, i2) == 0) {
                zArr[i4] = true;
                i3++;
            } else {
                zArr[i4] = false;
            }
        }
        if (i3 != 0 && (packageInfoGeneratePackageInfo = generatePackageInfo(packageSetting, i, i2)) != null) {
            if ((i & 4096) == 0) {
                if (i3 == strArr.length) {
                    packageInfoGeneratePackageInfo.requestedPermissions = strArr;
                } else {
                    packageInfoGeneratePackageInfo.requestedPermissions = new String[i3];
                    int i5 = 0;
                    for (int i6 = 0; i6 < strArr.length; i6++) {
                        if (zArr[i6]) {
                            packageInfoGeneratePackageInfo.requestedPermissions[i5] = strArr[i6];
                            i5++;
                        }
                    }
                }
            }
            arrayList.add(packageInfoGeneratePackageInfo);
        }
    }

    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] strArr, int i, int i2) {
        ParceledListSlice<PackageInfo> parceledListSlice;
        if (!sUserManager.exists(i2)) {
            return ParceledListSlice.emptyList();
        }
        int iUpdateFlagsForPackage = updateFlagsForPackage(i, i2, strArr);
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i2, true, false, "get packages holding permissions");
        boolean z = (4202496 & iUpdateFlagsForPackage) != 0;
        synchronized (this.mPackages) {
            ArrayList<PackageInfo> arrayList = new ArrayList<>();
            boolean[] zArr = new boolean[strArr.length];
            if (z) {
                Iterator<PackageSetting> it = this.mSettings.mPackages.values().iterator();
                while (it.hasNext()) {
                    addPackageHoldingPermissions(arrayList, it.next(), strArr, zArr, iUpdateFlagsForPackage, i2);
                }
            } else {
                Iterator<PackageParser.Package> it2 = this.mPackages.values().iterator();
                while (it2.hasNext()) {
                    PackageSetting packageSetting = (PackageSetting) it2.next().mExtras;
                    if (packageSetting != null) {
                        addPackageHoldingPermissions(arrayList, packageSetting, strArr, zArr, iUpdateFlagsForPackage, i2);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice<>(arrayList);
        }
        return parceledListSlice;
    }

    public ParceledListSlice<ApplicationInfo> getInstalledApplications(int i, int i2) {
        ArrayList arrayList;
        ParceledListSlice<ApplicationInfo> parceledListSlice;
        int i3;
        ApplicationInfo applicationInfoGenerateApplicationInfoFromSettingsLPw;
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        if (!sUserManager.exists(i2)) {
            return ParceledListSlice.emptyList();
        }
        int iUpdateFlagsForApplication = updateFlagsForApplication(i, i2, null);
        boolean z = (4202496 & iUpdateFlagsForApplication) != 0;
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, false, false, "get installed application info");
        synchronized (this.mPackages) {
            try {
                if (z) {
                    arrayList = new ArrayList(this.mSettings.mPackages.size());
                    for (PackageSetting packageSetting : this.mSettings.mPackages.values()) {
                        if (packageSetting.isSystem()) {
                            i3 = 4194304 | iUpdateFlagsForApplication;
                        } else {
                            i3 = iUpdateFlagsForApplication;
                        }
                        if (packageSetting.pkg != null) {
                            if (!filterSharedLibPackageLPr(packageSetting, callingUid, i2, iUpdateFlagsForApplication) && !filterAppAccessLPr(packageSetting, callingUid, i2)) {
                                applicationInfoGenerateApplicationInfoFromSettingsLPw = PackageParser.generateApplicationInfo(packageSetting.pkg, i3, packageSetting.readUserState(i2), i2);
                                if (applicationInfoGenerateApplicationInfoFromSettingsLPw != null) {
                                    applicationInfoGenerateApplicationInfoFromSettingsLPw.packageName = resolveExternalPackageNameLPr(packageSetting.pkg);
                                }
                            }
                        } else {
                            applicationInfoGenerateApplicationInfoFromSettingsLPw = generateApplicationInfoFromSettingsLPw(packageSetting.name, callingUid, i3, i2);
                        }
                        if (applicationInfoGenerateApplicationInfoFromSettingsLPw != null) {
                            ApplicationInfo applicationInfoUpdateApplicationInfoForRemovable = sPmsExt.updateApplicationInfoForRemovable(applicationInfoGenerateApplicationInfoFromSettingsLPw);
                            if (!sPmsExt.needSkipAppInfo(applicationInfoUpdateApplicationInfoForRemovable)) {
                                arrayList.add(applicationInfoUpdateApplicationInfoForRemovable);
                            }
                        }
                    }
                } else {
                    arrayList = new ArrayList(this.mPackages.size());
                    for (PackageParser.Package r3 : this.mPackages.values()) {
                        if (r3.mExtras != null) {
                            PackageSetting packageSetting2 = (PackageSetting) r3.mExtras;
                            if (!filterSharedLibPackageLPr(packageSetting2, Binder.getCallingUid(), i2, iUpdateFlagsForApplication) && !filterAppAccessLPr(packageSetting2, callingUid, i2)) {
                                ApplicationInfo applicationInfoGenerateApplicationInfo = PackageParser.generateApplicationInfo(r3, iUpdateFlagsForApplication, packageSetting2.readUserState(i2), i2);
                                if (applicationInfoGenerateApplicationInfo != null) {
                                    ApplicationInfo applicationInfoUpdateApplicationInfoForRemovable2 = sPmsExt.updateApplicationInfoForRemovable(applicationInfoGenerateApplicationInfo);
                                    if (!sPmsExt.needSkipAppInfo(applicationInfoUpdateApplicationInfoForRemovable2)) {
                                        applicationInfoUpdateApplicationInfoForRemovable2.packageName = resolveExternalPackageNameLPr(r3);
                                        arrayList.add(applicationInfoUpdateApplicationInfoForRemovable2);
                                    }
                                }
                            }
                        }
                    }
                }
                parceledListSlice = new ParceledListSlice<>(arrayList);
            } catch (Throwable th) {
                throw th;
            }
        }
        return parceledListSlice;
    }

    public ParceledListSlice<InstantAppInfo> getInstantApps(int i) {
        if (!canViewInstantApps(Binder.getCallingUid(), i)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getEphemeralApplications");
        }
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "getEphemeralApplications");
        synchronized (this.mPackages) {
            List<InstantAppInfo> instantAppsLPr = this.mInstantAppRegistry.getInstantAppsLPr(i);
            if (instantAppsLPr != null) {
                return new ParceledListSlice<>(instantAppsLPr);
            }
            return null;
        }
    }

    public boolean isInstantApp(String str, int i) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "isInstantApp");
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            if (Process.isIsolated(callingUid)) {
                callingUid = this.mIsolatedOwners.get(callingUid);
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            this.mPackages.get(str);
            if (!(packageSetting != null && (isCallerSameApp(str, callingUid) || canViewInstantApps(callingUid, i) || this.mInstantAppRegistry.isInstantAccessGranted(i, UserHandle.getAppId(callingUid), packageSetting.appId)))) {
                return false;
            }
            return packageSetting.getInstantApp(i);
        }
    }

    public byte[] getInstantAppCookie(String str, int i) {
        byte[] instantAppCookieLPw;
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "getInstantAppCookie");
        if (!isCallerSameApp(str, Binder.getCallingUid())) {
            return null;
        }
        synchronized (this.mPackages) {
            instantAppCookieLPw = this.mInstantAppRegistry.getInstantAppCookieLPw(str, i);
        }
        return instantAppCookieLPw;
    }

    public boolean setInstantAppCookie(String str, byte[] bArr, int i) {
        boolean instantAppCookieLPw;
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, true, "setInstantAppCookie");
        if (!isCallerSameApp(str, Binder.getCallingUid())) {
            return false;
        }
        synchronized (this.mPackages) {
            instantAppCookieLPw = this.mInstantAppRegistry.setInstantAppCookieLPw(str, bArr, i);
        }
        return instantAppCookieLPw;
    }

    public Bitmap getInstantAppIcon(String str, int i) {
        Bitmap instantAppIconLPw;
        if (!canViewInstantApps(Binder.getCallingUid(), i)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppIcon");
        }
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "getInstantAppIcon");
        synchronized (this.mPackages) {
            instantAppIconLPw = this.mInstantAppRegistry.getInstantAppIconLPw(str, i);
        }
        return instantAppIconLPw;
    }

    private boolean isCallerSameApp(String str, int i) {
        PackageParser.Package r2 = this.mPackages.get(str);
        return r2 != null && UserHandle.getAppId(i) == r2.applicationInfo.uid;
    }

    public ParceledListSlice<ApplicationInfo> getPersistentApplications(int i) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return ParceledListSlice.emptyList();
        }
        return new ParceledListSlice<>(getPersistentApplicationsInternal(i));
    }

    private List<ApplicationInfo> getPersistentApplicationsInternal(int i) {
        PackageSetting packageSetting;
        ApplicationInfo applicationInfoGenerateApplicationInfo;
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPackages) {
            int callingUserId = UserHandle.getCallingUserId();
            for (PackageParser.Package r4 : this.mPackages.values()) {
                if (r4.applicationInfo != null) {
                    boolean z = false;
                    boolean z2 = ((262144 & i) == 0 || r4.applicationInfo.isDirectBootAware()) ? false : true;
                    if ((524288 & i) != 0 && r4.applicationInfo.isDirectBootAware()) {
                        z = true;
                    }
                    if ((r4.applicationInfo.flags & 8) != 0 && ((!this.mSafeMode || isSystemApp(r4)) && ((z2 || z) && (packageSetting = this.mSettings.mPackages.get(r4.packageName)) != null && (applicationInfoGenerateApplicationInfo = PackageParser.generateApplicationInfo(r4, i, packageSetting.readUserState(callingUserId), callingUserId)) != null))) {
                        arrayList.add(applicationInfoGenerateApplicationInfo);
                    }
                }
            }
        }
        return arrayList;
    }

    public ProviderInfo resolveContentProvider(String str, int i, int i2) {
        return resolveContentProviderInternal(str, i, i2);
    }

    private ProviderInfo resolveContentProviderInternal(String str, int i, int i2) {
        PackageSetting packageSetting;
        if (!sUserManager.exists(i2)) {
            return null;
        }
        int iUpdateFlagsForComponent = updateFlagsForComponent(i, i2, str);
        int callingUid = Binder.getCallingUid();
        synchronized (this.mPackages) {
            PackageParser.Provider provider = this.mProvidersByAuthority.get(str);
            if (provider != null) {
                packageSetting = this.mSettings.mPackages.get(provider.owner.packageName);
            } else {
                packageSetting = null;
            }
            if (packageSetting == null) {
                return null;
            }
            if (!this.mSettings.isEnabledAndMatchLPr(provider.info, iUpdateFlagsForComponent, i2)) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, new ComponentName(provider.info.packageName, provider.info.name), 4, i2)) {
                return null;
            }
            return PackageParser.generateProviderInfo(provider, iUpdateFlagsForComponent, packageSetting.readUserState(i2), i2);
        }
    }

    @Deprecated
    public void querySyncProviders(List<String> list, List<ProviderInfo> list2) {
        ProviderInfo providerInfoGenerateProviderInfo;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        synchronized (this.mPackages) {
            int callingUserId = UserHandle.getCallingUserId();
            for (Map.Entry<String, PackageParser.Provider> entry : this.mProvidersByAuthority.entrySet()) {
                PackageParser.Provider value = entry.getValue();
                PackageSetting packageSetting = this.mSettings.mPackages.get(value.owner.packageName);
                if (packageSetting != null && value.syncable && ((!this.mSafeMode || (value.info.applicationInfo.flags & 1) != 0) && (providerInfoGenerateProviderInfo = PackageParser.generateProviderInfo(value, 0, packageSetting.readUserState(callingUserId), callingUserId)) != null)) {
                    list.add(entry.getKey());
                    list2.add(providerInfoGenerateProviderInfo);
                }
            }
        }
    }

    public ParceledListSlice<ProviderInfo> queryContentProviders(String str, int i, int i2, String str2) {
        ArrayList arrayList;
        PackageManagerService packageManagerService = this;
        int callingUid = Binder.getCallingUid();
        int userId = str != null ? UserHandle.getUserId(i) : UserHandle.getCallingUserId();
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        int iUpdateFlagsForComponent = packageManagerService.updateFlagsForComponent(i2, userId, str);
        synchronized (packageManagerService.mPackages) {
            arrayList = null;
            for (PackageParser.Provider provider : packageManagerService.mProviders.mProviders.values()) {
                PackageSetting packageSetting = packageManagerService.mSettings.mPackages.get(provider.owner.packageName);
                if (packageSetting != null && provider.info.authority != null && (str == null || (provider.info.processName.equals(str) && UserHandle.isSameApp(provider.info.applicationInfo.uid, i)))) {
                    if (packageManagerService.mSettings.isEnabledAndMatchLPr(provider.info, iUpdateFlagsForComponent, userId) && (str2 == null || (provider.metaData != null && provider.metaData.containsKey(str2)))) {
                        if (!packageManagerService.filterAppAccessLPr(packageSetting, callingUid, new ComponentName(provider.info.packageName, provider.info.name), 4, userId)) {
                            if (arrayList == null) {
                                arrayList = new ArrayList(3);
                            }
                            ProviderInfo providerInfoGenerateProviderInfo = PackageParser.generateProviderInfo(provider, iUpdateFlagsForComponent, packageSetting.readUserState(userId), userId);
                            if (providerInfoGenerateProviderInfo != null) {
                                arrayList.add(providerInfoGenerateProviderInfo);
                            }
                        }
                    }
                }
                packageManagerService = this;
            }
        }
        if (arrayList != null) {
            Collections.sort(arrayList, mProviderInitOrderSorter);
            return new ParceledListSlice<>(arrayList);
        }
        return ParceledListSlice.emptyList();
    }

    public InstrumentationInfo getInstrumentationInfo(ComponentName componentName, int i) {
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            PackageSetting packageSetting = this.mSettings.mPackages.get(componentName.getPackageName());
            if (packageSetting == null) {
                return null;
            }
            if (filterAppAccessLPr(packageSetting, callingUid, componentName, 0, userId)) {
                return null;
            }
            return PackageParser.generateInstrumentationInfo(this.mInstrumentation.get(componentName), i);
        }
    }

    public ParceledListSlice<InstrumentationInfo> queryInstrumentation(String str, int i) {
        int callingUid = Binder.getCallingUid();
        if (filterAppAccessLPr(this.mSettings.mPackages.get(str), callingUid, UserHandle.getUserId(callingUid))) {
            return ParceledListSlice.emptyList();
        }
        return new ParceledListSlice<>(queryInstrumentationInternal(str, i));
    }

    private List<InstrumentationInfo> queryInstrumentationInternal(String str, int i) {
        InstrumentationInfo instrumentationInfoGenerateInstrumentationInfo;
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPackages) {
            for (PackageParser.Instrumentation instrumentation : this.mInstrumentation.values()) {
                if ((str == null || str.equals(instrumentation.info.targetPackage)) && (instrumentationInfoGenerateInstrumentationInfo = PackageParser.generateInstrumentationInfo(instrumentation, i)) != null) {
                    arrayList.add(instrumentationInfoGenerateInstrumentationInfo);
                }
            }
        }
        return arrayList;
    }

    public void scanDirTracedLI(File file, int i, int i2, long j) {
        Trace.traceBegin(262144L, "scanDir [" + file.getAbsolutePath() + "]");
        try {
            scanDirLI(file, i, i2, j);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private void scanDirLI(File file, int i, int i2, long j) throws Exception {
        PackageParser.Callback callback;
        int i3;
        File[] fileArrListFiles = file.listFiles();
        if (ArrayUtils.isEmpty(fileArrListFiles)) {
            Log.d(TAG, "No files in app dir " + file);
            return;
        }
        sMtkSystemServerIns.addBootEvent("Android:PMS_scan_data:" + file.getPath().toString());
        if (DEBUG_PACKAGE_SCANNING) {
            Log.d(TAG, "Scanning app dir " + file + " scanFlags=" + i2 + " flags=0x" + Integer.toHexString(i));
        }
        String[] strArr = this.mSeparateProcesses;
        boolean z = this.mOnlyCore;
        DisplayMetrics displayMetrics = this.mMetrics;
        File file2 = this.mCacheDir;
        PackageParser.Callback callback2 = this.mParallelPackageParserCallback;
        PackageParser.Callback parallelPackageParser = new ParallelPackageParser(strArr, z, displayMetrics, file2, callback2);
        try {
            try {
                try {
                    int length = fileArrListFiles.length;
                    int i4 = 0;
                    int i5 = 0;
                    while (true) {
                        boolean z2 = true;
                        if (i4 >= length) {
                            break;
                        }
                        try {
                            File file3 = fileArrListFiles[i4];
                            if ((!PackageParser.isApkFile(file3) && !file3.isDirectory()) || PackageInstallerService.isStageName(file3.getName())) {
                                z2 = false;
                            }
                            if (z2) {
                                parallelPackageParser.submit(file3, i);
                                i5++;
                            }
                            i4++;
                        } catch (Throwable th) {
                            throw th;
                        }
                        throw th;
                    }
                    int i6 = i5;
                    while (i6 > 0) {
                        ParallelPackageParser.ParseResult parseResultTake = parallelPackageParser.take();
                        PackageParser.PackageParserException packageParserException = parseResultTake.throwable;
                        if (packageParserException == null) {
                            if (parseResultTake.pkg.applicationInfo.isStaticSharedLibrary()) {
                                renameStaticSharedLibraryPackage(parseResultTake.pkg);
                            }
                            try {
                                callback = parallelPackageParser;
                                try {
                                    try {
                                        scanPackageChildLI(parseResultTake.pkg, i, i2, j, null);
                                        i3 = 1;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } catch (PackageManagerException e) {
                                    e = e;
                                    i3 = e.error;
                                    Slog.w(TAG, "Failed to scan " + parseResultTake.scanFile + ": " + e.getMessage());
                                }
                            } catch (PackageManagerException e2) {
                                e = e2;
                                callback = parallelPackageParser;
                            }
                        } else {
                            callback = parallelPackageParser;
                            if (!(packageParserException instanceof PackageParser.PackageParserException)) {
                                throw new IllegalStateException("Unexpected exception occurred while parsing " + parseResultTake.scanFile, packageParserException);
                            }
                            PackageParser.PackageParserException packageParserException2 = packageParserException;
                            i3 = packageParserException2.error;
                            Slog.w(TAG, "Failed to parse " + parseResultTake.scanFile + ": " + packageParserException2.getMessage());
                        }
                        if ((131072 & i2) == 0 && i3 != 1) {
                            PackageManagerServiceUtils.logCriticalInfo(5, "Deleting invalid package at " + parseResultTake.scanFile);
                            removeCodePathLI(parseResultTake.scanFile);
                        }
                        i6--;
                        parallelPackageParser = callback;
                    }
                    $closeResource(null, parallelPackageParser);
                } catch (Throwable th3) {
                    th = th3;
                    $closeResource(null, callback2);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Throwable th5) {
            th = th5;
            callback2 = parallelPackageParser;
            $closeResource(null, callback2);
            throw th;
        }
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

    public static void reportSettingsProblem(int i, String str) {
        PackageManagerServiceUtils.logCriticalInfo(i, str);
    }

    private void collectCertificatesLI(PackageSetting packageSetting, PackageParser.Package r6, boolean z, boolean z2) throws PackageManagerException {
        long jLastModified = this.mIsPreNMR1Upgrade ? new File(r6.codePath).lastModified() : PackageManagerServiceUtils.getLastModifiedTime(r6);
        if (packageSetting != null && !z && packageSetting.codePathString.equals(r6.codePath) && packageSetting.timeStamp == jLastModified && !isCompatSignatureUpdateNeeded(r6) && !isRecoverSignatureUpdateNeeded(r6)) {
            if (packageSetting.signatures.mSigningDetails.signatures != null && packageSetting.signatures.mSigningDetails.signatures.length != 0 && packageSetting.signatures.mSigningDetails.signatureSchemeVersion != 0) {
                r6.mSigningDetails = new PackageParser.SigningDetails(packageSetting.signatures.mSigningDetails);
                return;
            }
            Slog.w(TAG, "PackageSetting for " + packageSetting.name + " is missing signatures.  Collecting certs again to recover them.");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(r6.codePath);
            sb.append(" changed; collecting certs");
            sb.append(z ? " (forced)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            Slog.i(TAG, sb.toString());
        }
        try {
            try {
                Trace.traceBegin(262144L, "collectCertificates");
                PackageParser.collectCertificates(r6, z2);
            } catch (PackageParser.PackageParserException e) {
                throw PackageManagerException.from(e);
            }
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private void maybeClearProfilesForUpgradesLI(PackageSetting packageSetting, PackageParser.Package r6) {
        if (packageSetting == null || !isUpgrade() || packageSetting.versionCode == r6.mVersionCode) {
            return;
        }
        clearAppProfilesLIF(r6, -1);
        if (DEBUG_INSTALL) {
            Slog.d(TAG, packageSetting.name + " clear profile due to version change " + packageSetting.versionCode + " != " + r6.mVersionCode);
        }
    }

    public PackageParser.Package scanPackageTracedLI(File file, int i, int i2, long j, UserHandle userHandle) throws PackageManagerException {
        Trace.traceBegin(262144L, "scanPackage [" + file.toString() + "]");
        try {
            return scanPackageLI(file, i, i2, j, userHandle);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private PackageParser.Package scanPackageLI(File file, int i, int i2, long j, UserHandle userHandle) throws PackageManagerException {
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Parsing: " + file);
        }
        PackageParser packageParser = new PackageParser();
        packageParser.setSeparateProcesses(this.mSeparateProcesses);
        packageParser.setOnlyCoreApps(this.mOnlyCore);
        packageParser.setDisplayMetrics(this.mMetrics);
        packageParser.setCallback(this.mPackageParserCallback);
        Trace.traceBegin(262144L, "parsePackage");
        try {
            try {
                PackageParser.Package r2 = packageParser.parsePackage(file, i);
                Trace.traceEnd(262144L);
                if (r2.applicationInfo.isStaticSharedLibrary()) {
                    renameStaticSharedLibraryPackage(r2);
                }
                return scanPackageChildLI(r2, i, i2, j, userHandle);
            } catch (PackageParser.PackageParserException e) {
                throw PackageManagerException.from(e);
            }
        } catch (Throwable th) {
            Trace.traceEnd(262144L);
            throw th;
        }
    }

    private PackageParser.Package scanPackageChildLI(PackageParser.Package r17, int i, int i2, long j, UserHandle userHandle) throws Exception {
        int i3 = i2;
        if ((i3 & 1024) == 0) {
            if (r17.childPackages != null && r17.childPackages.size() > 0) {
                i3 |= 1024;
            }
        } else {
            i3 &= -1025;
        }
        int i4 = i3;
        PackageParser.Package packageAddForInitLI = addForInitLI(r17, i, i4, j, userHandle);
        int size = r17.childPackages != null ? r17.childPackages.size() : 0;
        for (int i5 = 0; i5 < size; i5++) {
            addForInitLI((PackageParser.Package) r17.childPackages.get(i5), i, i4, j, userHandle);
        }
        if ((i4 & 1024) != 0) {
            return scanPackageChildLI(r17, i, i4, j, userHandle);
        }
        return packageAddForInitLI;
    }

    private boolean canSkipFullPackageVerification(PackageParser.Package r4) {
        if (!canSkipFullApkVerification(r4.baseCodePath)) {
            return false;
        }
        if (!ArrayUtils.isEmpty(r4.splitCodePaths)) {
            for (int i = 0; i < r4.splitCodePaths.length; i++) {
                if (!canSkipFullApkVerification(r4.splitCodePaths[i])) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean canSkipFullApkVerification(String str) {
        try {
            byte[] bArrGenerateFsverityRootHash = VerityUtils.generateFsverityRootHash(str);
            if (bArrGenerateFsverityRootHash == null) {
                return false;
            }
            synchronized (this.mInstallLock) {
                this.mInstaller.assertFsverityRootHashMatches(str, bArrGenerateFsverityRootHash);
            }
            return true;
        } catch (Installer.InstallerException | IOException | DigestException | NoSuchAlgorithmException e) {
            Slog.w(TAG, "Error in fsverity check. Fallback to full apk verification.", e);
            return false;
        }
    }

    private PackageParser.Package addForInitLI(PackageParser.Package r29, int i, int i2, long j, UserHandle userHandle) throws Exception {
        SharedUserSetting sharedUserLPw;
        PackageSetting packageSetting;
        PackageSetting packageSetting2;
        ArrayMap<String, PackageParser.Package> arrayMap;
        boolean z;
        PackageSetting packageSetting3;
        boolean z2;
        boolean z3;
        boolean z4;
        Throwable th;
        int size;
        int size2;
        boolean z5;
        int i3 = i & 16;
        boolean z6 = i3 != 0;
        r29.setApplicationVolumeUuid(r29.volumeUuid);
        r29.setApplicationInfoCodePath(r29.codePath);
        r29.setApplicationInfoBaseCodePath(r29.baseCodePath);
        r29.setApplicationInfoSplitCodePaths(r29.splitCodePaths);
        r29.setApplicationInfoResourcePath(r29.codePath);
        r29.setApplicationInfoBaseResourcePath(r29.baseCodePath);
        r29.setApplicationInfoSplitResourcePaths(r29.splitCodePaths);
        ArrayMap<String, PackageParser.Package> arrayMap2 = this.mPackages;
        synchronized (arrayMap2) {
            try {
                try {
                    String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(r29.mRealPackage);
                    if (getRealPackageName(r29, renamedPackageLPr) != null) {
                        ensurePackageRenamed(r29, renamedPackageLPr);
                    }
                    PackageSetting originalPackageLocked = getOriginalPackageLocked(r29, renamedPackageLPr);
                    PackageSetting packageLPr = originalPackageLocked == null ? this.mSettings.getPackageLPr(r29.packageName) : originalPackageLocked;
                    boolean z7 = packageLPr != null;
                    PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(z7 ? packageLPr.name : r29.packageName);
                    boolean z8 = disabledSystemPkgLPr != null;
                    if (DEBUG_INSTALL && z8) {
                        Slog.d(TAG, "updatedPkg = " + disabledSystemPkgLPr);
                    }
                    if (sPmsExt.needSkipScanning(r29, disabledSystemPkgLPr, packageLPr)) {
                        return null;
                    }
                    if (r29.mSharedUserId != null) {
                        sharedUserLPw = this.mSettings.getSharedUserLPw(r29.mSharedUserId, 0, 0, true);
                    } else {
                        sharedUserLPw = null;
                    }
                    if (DEBUG_PACKAGE_SCANNING && (Integer.MIN_VALUE & i) != 0 && sharedUserLPw != null) {
                        Log.d(TAG, "Shared UserID " + r29.mSharedUserId + " (uid=" + sharedUserLPw.userId + "): packages=" + sharedUserLPw.packages);
                    }
                    if (z6 && z8) {
                        if (r29.childPackages != null) {
                            size = r29.childPackages.size();
                        } else {
                            size = 0;
                        }
                        if (disabledSystemPkgLPr.childPackageNames != null) {
                            size2 = disabledSystemPkgLPr.childPackageNames.size();
                        } else {
                            size2 = 0;
                        }
                        for (int i4 = 0; i4 < size2; i4++) {
                            String str = disabledSystemPkgLPr.childPackageNames.get(i4);
                            int i5 = 0;
                            while (true) {
                                if (i5 < size) {
                                    if (!((PackageParser.Package) r29.childPackages.get(i5)).packageName.equals(str)) {
                                        i5++;
                                    } else {
                                        z5 = true;
                                        break;
                                    }
                                } else {
                                    z5 = false;
                                    break;
                                }
                            }
                            if (!z5) {
                                this.mSettings.removeDisabledSystemPackageLPw(str);
                            }
                        }
                        packageSetting = disabledSystemPkgLPr;
                        packageSetting2 = packageLPr;
                        arrayMap = arrayMap2;
                        z = false;
                        ScanRequest scanRequest = new ScanRequest(r29, sharedUserLPw, null, disabledSystemPkgLPr, null, null, null, i, i2, r29 == this.mPlatformPackage, userHandle);
                        applyPolicy(r29, i, i2, this.mPlatformPackage);
                        scanPackageOnlyLI(scanRequest, this.mFactoryTest, -1L);
                    } else {
                        packageSetting = disabledSystemPkgLPr;
                        packageSetting2 = packageLPr;
                        arrayMap = arrayMap2;
                        z = false;
                    }
                    if (z7) {
                        packageSetting3 = packageSetting2;
                        if (!packageSetting3.codePathString.equals(r29.codePath)) {
                            z2 = true;
                        }
                        boolean z9 = (z7 || r29.getLongVersionCode() <= packageSetting3.versionCode) ? z : true;
                        z3 = (!z6 && z8 && z2 && z9) ? true : z;
                        if (z3) {
                            synchronized (this.mPackages) {
                                this.mPackages.remove(packageSetting3.name);
                            }
                            PackageManagerServiceUtils.logCriticalInfo(5, "System package updated; name: " + packageSetting3.name + "; " + packageSetting3.versionCode + " --> " + r29.getLongVersionCode() + "; " + packageSetting3.codePathString + " --> " + r29.codePath);
                            createInstallArgsForExisting(packageFlagsToInstallFlags(packageSetting3), packageSetting3.codePathString, packageSetting3.resourcePathString, InstructionSets.getAppDexInstructionSets(packageSetting3)).cleanUpResourcesLI();
                            synchronized (this.mPackages) {
                                this.mSettings.enableSystemPackageLPw(packageSetting3.name);
                            }
                        }
                        if (!z6 && z8 && !z3) {
                            synchronized (this.mPackages) {
                                if (!this.mPackages.containsKey(r29.packageName)) {
                                    this.mPackages.put(r29.packageName, r29);
                                }
                            }
                            throw new PackageManagerException(5, "Package " + r29.packageName + " at " + r29.codePath + " ignored: updated version " + packageSetting3.versionCode + " better than this " + r29.getLongVersionCode());
                        }
                        boolean zIsApkVerificationForced = PackageManagerServiceUtils.isApkVerificationForced(packageSetting);
                        collectCertificatesLI(packageSetting3, r29, zIsApkVerificationForced, (i3 == 0 || (zIsApkVerificationForced && canSkipFullPackageVerification(r29))) ? true : z);
                        maybeClearProfilesForUpgradesLI(packageSetting3, r29);
                        if (z6 || z8 || !z7 || packageSetting3.isSystem()) {
                            z4 = true;
                        } else {
                            z4 = true;
                            if (!r29.mSigningDetails.checkCapability(packageSetting3.signatures.mSigningDetails, 1) && !packageSetting3.signatures.mSigningDetails.checkCapability(r29.mSigningDetails, 8)) {
                                PackageManagerServiceUtils.logCriticalInfo(5, "System package signature mismatch; name: " + packageSetting3.name);
                                PackageFreezer packageFreezerFreezePackage = freezePackage(r29.packageName, "scanPackageInternalLI");
                                try {
                                    deletePackageLIF(r29.packageName, null, true, null, 0, null, false, null);
                                    if (packageFreezerFreezePackage != null) {
                                        $closeResource(null, packageFreezerFreezePackage);
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    th = null;
                                    if (packageFreezerFreezePackage != null) {
                                    }
                                    throw th;
                                }
                            } else if (z9) {
                                PackageManagerServiceUtils.logCriticalInfo(5, "System package enabled; name: " + packageSetting3.name + "; " + packageSetting3.versionCode + " --> " + r29.getLongVersionCode() + "; " + packageSetting3.codePathString + " --> " + r29.codePath);
                                InstallArgs installArgsCreateInstallArgsForExisting = createInstallArgsForExisting(packageFlagsToInstallFlags(packageSetting3), packageSetting3.codePathString, packageSetting3.resourcePathString, InstructionSets.getAppDexInstructionSets(packageSetting3));
                                synchronized (this.mInstallLock) {
                                    installArgsCreateInstallArgsForExisting.cleanUpResourcesLI();
                                }
                            } else {
                                PackageManagerServiceUtils.logCriticalInfo(4, "System package disabled; name: " + packageSetting3.name + "; old: " + packageSetting3.codePathString + " @ " + packageSetting3.versionCode + "; new: " + r29.codePath + " @ " + r29.codePath);
                                z = true;
                            }
                        }
                        PackageParser.Package packageScanPackageNewLI = scanPackageNewLI(r29, i, i2 | 2, j, userHandle);
                        if (z) {
                            synchronized (this.mPackages) {
                                this.mSettings.disableSystemPackageLPw(r29.packageName, z4);
                            }
                        }
                        return packageScanPackageNewLI;
                    }
                    packageSetting3 = packageSetting2;
                    z2 = z;
                    if (z7) {
                    }
                    if (!z6) {
                    }
                    if (z3) {
                    }
                    if (!z6) {
                    }
                    boolean zIsApkVerificationForced2 = PackageManagerServiceUtils.isApkVerificationForced(packageSetting);
                    collectCertificatesLI(packageSetting3, r29, zIsApkVerificationForced2, (i3 == 0 || (zIsApkVerificationForced2 && canSkipFullPackageVerification(r29))) ? true : z);
                    maybeClearProfilesForUpgradesLI(packageSetting3, r29);
                    if (z6) {
                        z4 = true;
                    }
                    PackageParser.Package packageScanPackageNewLI2 = scanPackageNewLI(r29, i, i2 | 2, j, userHandle);
                    if (z) {
                    }
                    return packageScanPackageNewLI2;
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private static void renameStaticSharedLibraryPackage(PackageParser.Package r3) {
        r3.setPackageName(r3.packageName + STATIC_SHARED_LIB_DELIMITER + r3.staticSharedLibVersion);
    }

    private static String fixProcessName(String str, String str2) {
        if (str2 == null) {
            return str;
        }
        return str2;
    }

    private static final void enforceSystemOrRoot(String str) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0) {
            throw new SecurityException(str);
        }
    }

    public void performFstrimIfNeeded() {
        boolean z;
        enforceSystemOrRoot("Only the system can request fstrim");
        try {
            IStorageManager storageManager = PackageHelper.getStorageManager();
            if (storageManager != null) {
                boolean z2 = false;
                long j = Settings.Global.getLong(this.mContext.getContentResolver(), "fstrim_mandatory_interval", DEFAULT_MANDATORY_FSTRIM_INTERVAL);
                if (j > 0) {
                    long jCurrentTimeMillis = System.currentTimeMillis() - storageManager.lastMaintenance();
                    if (jCurrentTimeMillis > j) {
                        Slog.w(TAG, "No disk maintenance in " + jCurrentTimeMillis + "; running immediately");
                        z2 = true;
                    }
                }
                if (z2) {
                    synchronized (this.mPackages) {
                        z = this.mDexOptDialogShown;
                    }
                    if (!isFirstBoot() && z) {
                        try {
                            ActivityManager.getService().showBootMessage(this.mContext.getResources().getString(R.string.PERSOSUBSTATE_RUIM_NETWORK1_IN_PROGRESS), true);
                        } catch (RemoteException e) {
                        }
                    }
                    storageManager.runMaintenance();
                }
                return;
            }
            Slog.e(TAG, "storageManager service unavailable!");
        } catch (RemoteException e2) {
        }
    }

    public void updatePackagesIfNeeded() {
        List<PackageParser.Package> packagesForDexopt;
        enforceSystemOrRoot("Only the system can request package update");
        boolean zIsUpgrade = isUpgrade();
        int i = (isFirstBoot() || this.mIsPreNUpgrade) ? 1 : 0;
        boolean zDidPruneDalvikCache = VMRuntime.didPruneDalvikCache();
        if (!zIsUpgrade && i == 0 && !zDidPruneDalvikCache) {
            return;
        }
        synchronized (this.mPackages) {
            packagesForDexopt = PackageManagerServiceUtils.getPackagesForDexopt(this.mPackages.values(), this);
        }
        long jNanoTime = System.nanoTime();
        int[] iArrPerformDexOptUpgrade = performDexOptUpgrade(packagesForDexopt, this.mIsPreNUpgrade, i ^ 1, false);
        int seconds = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - jNanoTime);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_dexopted", iArrPerformDexOptUpgrade[0]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_skipped", iArrPerformDexOptUpgrade[1]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_failed", iArrPerformDexOptUpgrade[2]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_total", getOptimizablePackages().size());
        MetricsLogger.histogram(this.mContext, "opt_dialog_time_s", seconds);
    }

    private static String getPrebuildProfilePath(PackageParser.Package r1) {
        return r1.baseCodePath + ".prof";
    }

    private int[] performDexOptUpgrade(List<PackageParser.Package> list, boolean z, int i, boolean z2) {
        char c;
        char c2;
        int size = list.size();
        char c3 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        for (PackageParser.Package r8 : list) {
            int i6 = i2 + 1;
            if ((isFirstBoot() || isUpgrade()) && isSystemApp(r8)) {
                File file = new File(getPrebuildProfilePath(r8));
                if (file.exists()) {
                    try {
                        if (!this.mInstaller.copySystemProfile(file.getAbsolutePath(), r8.applicationInfo.uid, r8.packageName, ArtManager.getProfileName((String) null))) {
                            Log.e(TAG, "Installer failed to copy system profile!");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy profile " + file.getAbsolutePath() + " ", e);
                    }
                } else {
                    PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(r8.packageName);
                    if (disabledSystemPkgLPr != null && disabledSystemPkgLPr.pkg.isStub) {
                        File file2 = new File(getPrebuildProfilePath(disabledSystemPkgLPr.pkg).replace(STUB_SUFFIX, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        if (file2.exists()) {
                            try {
                                if (!this.mInstaller.copySystemProfile(file2.getAbsolutePath(), r8.applicationInfo.uid, r8.packageName, ArtManager.getProfileName((String) null))) {
                                    Log.e(TAG, "Failed to copy system profile for stub package!");
                                    c = c3;
                                } else {
                                    c = 1;
                                }
                                c2 = c;
                            } catch (Exception e2) {
                                Log.e(TAG, "Failed to copy profile " + file2.getAbsolutePath() + " ", e2);
                                c2 = c3;
                            }
                        }
                    }
                }
                c2 = c3;
            } else {
                c2 = c3;
            }
            if (!PackageDexOptimizer.canOptimizePackage(r8)) {
                if (DEBUG_DEXOPT) {
                    Log.i(TAG, "Skipping update of of non-optimizable app " + r8.packageName);
                }
                i4++;
                i2 = i6;
            } else {
                if (DEBUG_DEXOPT) {
                    Log.i(TAG, "Updating app " + i6 + " of " + size + ": " + r8.packageName);
                }
                if (z) {
                    try {
                        IActivityManager service = ActivityManager.getService();
                        Resources resources = this.mContext.getResources();
                        Object[] objArr = new Object[2];
                        objArr[c3] = Integer.valueOf(i6);
                        objArr[1] = Integer.valueOf(size);
                        service.showBootMessage(resources.getString(R.string.PERSOSUBSTATE_RUIM_NETWORK1_ENTRY, objArr), true);
                    } catch (RemoteException e3) {
                    }
                    synchronized (this.mPackages) {
                        this.mDexOptDialogShown = true;
                    }
                }
                int i7 = c2 != 0 ? 3 : i;
                int i8 = z2 ? 4 : 0;
                if (i == 0) {
                    i8 |= 1024;
                }
                int iPerformDexOptTraced = performDexOptTraced(new DexoptOptions(r8.packageName, i7, i8));
                switch (iPerformDexOptTraced) {
                    case -1:
                        i5++;
                        break;
                    case 0:
                        i4++;
                        break;
                    case 1:
                        i3++;
                        break;
                    default:
                        Log.e(TAG, "Unexpected dexopt return code " + iPerformDexOptTraced);
                        break;
                }
                i2 = i6;
                c3 = 0;
            }
        }
        return new int[]{i3, i4, i5};
    }

    public void notifyPackageUse(String str, int i) {
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            if (getInstantAppPackageName(callingUid) != null) {
                if (!isCallerSameApp(str, callingUid)) {
                    return;
                }
            } else if (isInstantApp(str, userId)) {
                return;
            }
            notifyPackageUseLocked(str, i);
        }
    }

    @GuardedBy("mPackages")
    private void notifyPackageUseLocked(String str, int i) {
        PackageParser.Package r3 = this.mPackages.get(str);
        if (r3 == null) {
            return;
        }
        r3.mLastPackageUsageTimeInMills[i] = System.currentTimeMillis();
    }

    public void notifyDexLoad(String str, List<String> list, List<String> list2, String str2) {
        int callingUserId = UserHandle.getCallingUserId();
        ApplicationInfo applicationInfo = getApplicationInfo(str, 0, callingUserId);
        if (applicationInfo == null) {
            Slog.w(TAG, "Loading a package that does not exist for the calling user. package=" + str + ", user=" + callingUserId);
            return;
        }
        this.mDexManager.notifyDexLoad(applicationInfo, list, list2, str2, callingUserId);
    }

    public void registerDexModule(String str, final String str2, boolean z, final IDexModuleRegisterCallback iDexModuleRegisterCallback) {
        final DexManager.RegisterDexModuleResult registerDexModuleResultRegisterDexModule;
        int callingUserId = UserHandle.getCallingUserId();
        ApplicationInfo applicationInfo = getApplicationInfo(str, 0, callingUserId);
        if (applicationInfo == null) {
            Slog.w(TAG, "Registering a dex module for a package that does not exist for the calling user. package=" + str + ", user=" + callingUserId);
            registerDexModuleResultRegisterDexModule = new DexManager.RegisterDexModuleResult(false, "Package not installed");
        } else {
            registerDexModuleResultRegisterDexModule = this.mDexManager.registerDexModule(applicationInfo, str2, z, callingUserId);
        }
        if (iDexModuleRegisterCallback != null) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PackageManagerService.lambda$registerDexModule$4(iDexModuleRegisterCallback, str2, registerDexModuleResultRegisterDexModule);
                }
            });
        }
    }

    static void lambda$registerDexModule$4(IDexModuleRegisterCallback iDexModuleRegisterCallback, String str, DexManager.RegisterDexModuleResult registerDexModuleResult) {
        try {
            iDexModuleRegisterCallback.onDexModuleRegistered(str, registerDexModuleResult.success, registerDexModuleResult.message);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to callback after module registration " + str, e);
        }
    }

    public boolean performDexOptMode(String str, boolean z, String str2, boolean z2, boolean z3, String str3) {
        return performDexOpt(new DexoptOptions(str, -1, str2, str3, (z ? 1 : 0) | (z2 ? 2 : 0) | (z3 ? 4 : 0)));
    }

    public boolean performDexOptSecondary(String str, String str2, boolean z) {
        return performDexOpt(new DexoptOptions(str, str2, (z ? 2 : 0) | 13));
    }

    boolean performDexOpt(DexoptOptions dexoptOptions) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || isInstantApp(dexoptOptions.getPackageName(), UserHandle.getCallingUserId())) {
            return false;
        }
        if (dexoptOptions.isDexoptOnlySecondaryDex()) {
            return this.mDexManager.dexoptSecondaryDex(dexoptOptions);
        }
        return performDexOptWithStatus(dexoptOptions) != -1;
    }

    int performDexOptWithStatus(DexoptOptions dexoptOptions) {
        return performDexOptTraced(dexoptOptions);
    }

    private int performDexOptTraced(DexoptOptions dexoptOptions) {
        sMtkSystemServerIns.addBootEvent("PMS:performDexOpt:" + dexoptOptions.getPackageName());
        Trace.traceBegin(262144L, "dexopt");
        try {
            return performDexOptInternal(dexoptOptions);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private int performDexOptInternal(DexoptOptions dexoptOptions) {
        int iPerformDexOptInternalWithDependenciesLI;
        synchronized (this.mPackages) {
            PackageParser.Package r1 = this.mPackages.get(dexoptOptions.getPackageName());
            if (r1 == null) {
                return -1;
            }
            this.mPackageUsage.maybeWriteAsync(this.mPackages);
            this.mCompilerStats.maybeWriteAsync();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mInstallLock) {
                    iPerformDexOptInternalWithDependenciesLI = performDexOptInternalWithDependenciesLI(r1, dexoptOptions);
                }
                return iPerformDexOptInternalWithDependenciesLI;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public ArraySet<String> getOptimizablePackages() {
        ArraySet<String> arraySet = new ArraySet<>();
        synchronized (this.mPackages) {
            for (PackageParser.Package r3 : this.mPackages.values()) {
                if (PackageDexOptimizer.canOptimizePackage(r3)) {
                    arraySet.add(r3.packageName);
                }
            }
        }
        return arraySet;
    }

    private int performDexOptInternalWithDependenciesLI(PackageParser.Package r19, DexoptOptions dexoptOptions) {
        PackageDexOptimizer forcedUpdatePackageDexOptimizer;
        if (dexoptOptions.isForce()) {
            forcedUpdatePackageDexOptimizer = new PackageDexOptimizer.ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        } else {
            forcedUpdatePackageDexOptimizer = this.mPackageDexOptimizer;
        }
        List<PackageParser.Package> listFindSharedNonSystemLibraries = findSharedNonSystemLibraries(r19);
        String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(r19.applicationInfo);
        if (!listFindSharedNonSystemLibraries.isEmpty()) {
            DexoptOptions dexoptOptions2 = new DexoptOptions(dexoptOptions.getPackageName(), dexoptOptions.getCompilationReason(), dexoptOptions.getCompilerFilter(), dexoptOptions.getSplitName(), dexoptOptions.getFlags() | 64);
            for (PackageParser.Package r4 : listFindSharedNonSystemLibraries) {
                forcedUpdatePackageDexOptimizer.performDexOpt(r4, null, appDexInstructionSets, getOrCreateCompilerPackageStats(r4), this.mDexManager.getPackageUseInfoOrDefault(r4.packageName), dexoptOptions2);
            }
        }
        return forcedUpdatePackageDexOptimizer.performDexOpt(r19, r19.usesLibraryFiles, appDexInstructionSets, getOrCreateCompilerPackageStats(r19), this.mDexManager.getPackageUseInfoOrDefault(r19.packageName), dexoptOptions);
    }

    public void reconcileSecondaryDexFiles(String str) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || isInstantApp(str, UserHandle.getCallingUserId())) {
            return;
        }
        this.mDexManager.reconcileSecondaryDexFiles(str);
    }

    DexManager getDexManager() {
        return this.mDexManager;
    }

    public boolean runBackgroundDexoptJob(List<String> list) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return false;
        }
        return BackgroundDexOptService.runIdleOptimizationsNow(this, this.mContext, list);
    }

    List<PackageParser.Package> findSharedNonSystemLibraries(PackageParser.Package r3) {
        if (r3.usesLibraries != null || r3.usesOptionalLibraries != null || r3.usesStaticLibraries != null) {
            ArrayList<PackageParser.Package> arrayList = new ArrayList<>();
            findSharedNonSystemLibrariesRecursive(r3, arrayList, new HashSet());
            arrayList.remove(r3);
            return arrayList;
        }
        return Collections.emptyList();
    }

    private void findSharedNonSystemLibrariesRecursive(PackageParser.Package r3, ArrayList<PackageParser.Package> arrayList, Set<String> set) {
        if (!set.contains(r3.packageName)) {
            set.add(r3.packageName);
            arrayList.add(r3);
            if (r3.usesLibraries != null) {
                findSharedNonSystemLibrariesRecursive(r3.usesLibraries, null, arrayList, set);
            }
            if (r3.usesOptionalLibraries != null) {
                findSharedNonSystemLibrariesRecursive(r3.usesOptionalLibraries, null, arrayList, set);
            }
            if (r3.usesStaticLibraries != null) {
                findSharedNonSystemLibrariesRecursive(r3.usesStaticLibraries, r3.usesStaticLibrariesVersions, arrayList, set);
            }
        }
    }

    private void findSharedNonSystemLibrariesRecursive(ArrayList<String> arrayList, long[] jArr, ArrayList<PackageParser.Package> arrayList2, Set<String> set) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            PackageParser.Package packageFindSharedNonSystemLibrary = findSharedNonSystemLibrary(arrayList.get(i), (jArr == null || jArr.length != size) ? -1L : jArr[i]);
            if (packageFindSharedNonSystemLibrary != null) {
                findSharedNonSystemLibrariesRecursive(packageFindSharedNonSystemLibrary, arrayList2, set);
            }
        }
    }

    private PackageParser.Package findSharedNonSystemLibrary(String str, long j) {
        synchronized (this.mPackages) {
            SharedLibraryEntry sharedLibraryEntryLPr = getSharedLibraryEntryLPr(str, j);
            if (sharedLibraryEntryLPr != null) {
                return this.mPackages.get(sharedLibraryEntryLPr.apk);
            }
            return null;
        }
    }

    private SharedLibraryEntry getSharedLibraryEntryLPr(String str, long j) {
        LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(str);
        if (longSparseArray == null) {
            return null;
        }
        return longSparseArray.get(j);
    }

    private SharedLibraryEntry getLatestSharedLibraVersionLPr(PackageParser.Package r11) {
        LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(r11.staticSharedLibName);
        if (longSparseArray == null) {
            return null;
        }
        long jMax = -1;
        int size = longSparseArray.size();
        for (int i = 0; i < size; i++) {
            long jKeyAt = longSparseArray.keyAt(i);
            if (jKeyAt < r11.staticSharedLibVersion) {
                jMax = Math.max(jMax, jKeyAt);
            }
        }
        if (jMax < 0) {
            return null;
        }
        return longSparseArray.get(jMax);
    }

    public void shutdown() {
        this.mPackageUsage.writeNow(this.mPackages);
        this.mCompilerStats.writeNow();
        this.mDexManager.writePackageDexUsageNow();
        sCtaManager.shutdown();
        synchronized (this.mPackages) {
            if (this.mHandler.hasMessages(14)) {
                this.mHandler.removeMessages(14);
                Iterator<Integer> it = this.mDirtyUsers.iterator();
                while (it.hasNext()) {
                    this.mSettings.writePackageRestrictionsLPr(it.next().intValue());
                }
                this.mDirtyUsers.clear();
            }
        }
    }

    public void dumpProfiles(String str) {
        PackageParser.Package r1;
        synchronized (this.mPackages) {
            r1 = this.mPackages.get(str);
            if (r1 == null) {
                throw new IllegalArgumentException("Unknown package: " + str);
            }
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid != 2000 && callingUid != 0 && callingUid != r1.applicationInfo.uid) {
            throw new SecurityException("dumpProfiles");
        }
        synchronized (this.mInstallLock) {
            Trace.traceBegin(262144L, "dump profiles");
            this.mArtManagerService.dumpProfiles(r1);
            Trace.traceEnd(262144L);
        }
    }

    public void forceDexOpt(String str) {
        PackageParser.Package r1;
        enforceSystemOrRoot("forceDexOpt");
        synchronized (this.mPackages) {
            r1 = this.mPackages.get(str);
            if (r1 == null) {
                throw new IllegalArgumentException("Unknown package: " + str);
            }
        }
        synchronized (this.mInstallLock) {
            Trace.traceBegin(262144L, "dexopt");
            int iPerformDexOptInternalWithDependenciesLI = performDexOptInternalWithDependenciesLI(r1, new DexoptOptions(str, PackageManagerServiceCompilerMapping.getDefaultCompilerFilter(), 6));
            Trace.traceEnd(262144L);
            if (iPerformDexOptInternalWithDependenciesLI != 1) {
                throw new IllegalStateException("Failed to dexopt: " + iPerformDexOptInternalWithDependenciesLI);
            }
        }
    }

    private boolean verifyPackageUpdateLPr(PackageSetting packageSetting, PackageParser.Package r6) {
        if ((packageSetting.pkgFlags & 1) == 0) {
            Slog.w(TAG, "Unable to update from " + packageSetting.name + " to " + r6.packageName + ": old package not in system partition");
            return false;
        }
        if (this.mPackages.get(packageSetting.name) == null) {
            return true;
        }
        Slog.w(TAG, "Unable to update from " + packageSetting.name + " to " + r6.packageName + ": old package still exists");
        return false;
    }

    void removeCodePathLI(File file) {
        if (file.isDirectory()) {
            try {
                this.mInstaller.rmPackageDir(file.getAbsolutePath());
                return;
            } catch (Installer.InstallerException e) {
                Slog.w(TAG, "Failed to remove code path", e);
                return;
            }
        }
        file.delete();
    }

    private int[] resolveUserIds(int i) {
        return i == -1 ? sUserManager.getUserIds() : new int[]{i};
    }

    private void clearAppDataLIF(PackageParser.Package r4, int i, int i2) {
        if (r4 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        clearAppDataLeafLIF(r4, i, i2);
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i3 = 0; i3 < size; i3++) {
            clearAppDataLeafLIF((PackageParser.Package) r4.childPackages.get(i3), i, i2);
        }
        clearAppProfilesLIF(r4, -1);
    }

    private void clearAppDataLeafLIF(PackageParser.Package r11, int i, int i2) {
        PackageSetting packageSetting;
        synchronized (this.mPackages) {
            packageSetting = this.mSettings.mPackages.get(r11.packageName);
        }
        for (int i3 : resolveUserIds(i)) {
            try {
                this.mInstaller.clearAppData(r11.volumeUuid, r11.packageName, i3, i2, packageSetting != null ? packageSetting.getCeDataInode(i3) : 0L);
            } catch (Installer.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
        }
    }

    private void destroyAppDataLIF(PackageParser.Package r4, int i, int i2) {
        if (r4 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppDataLeafLIF(r4, i, i2);
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i3 = 0; i3 < size; i3++) {
            destroyAppDataLeafLIF((PackageParser.Package) r4.childPackages.get(i3), i, i2);
        }
    }

    private void destroyAppDataLeafLIF(PackageParser.Package r12, int i, int i2) {
        PackageSetting packageSetting;
        synchronized (this.mPackages) {
            packageSetting = this.mSettings.mPackages.get(r12.packageName);
        }
        for (int i3 : resolveUserIds(i)) {
            try {
                this.mInstaller.destroyAppData(r12.volumeUuid, r12.packageName, i3, i2, packageSetting != null ? packageSetting.getCeDataInode(i3) : 0L);
            } catch (Installer.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
            this.mDexManager.notifyPackageDataDestroyed(r12.packageName, i);
        }
    }

    private void destroyAppProfilesLIF(PackageParser.Package r3, int i) {
        if (r3 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppProfilesLeafLIF(r3);
        int size = r3.childPackages != null ? r3.childPackages.size() : 0;
        for (int i2 = 0; i2 < size; i2++) {
            destroyAppProfilesLeafLIF((PackageParser.Package) r3.childPackages.get(i2));
        }
    }

    private void destroyAppProfilesLeafLIF(PackageParser.Package r2) {
        try {
            this.mInstaller.destroyAppProfiles(r2.packageName);
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    private void clearAppProfilesLIF(PackageParser.Package r4, int i) {
        if (r4 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        this.mArtManagerService.clearAppProfiles(r4);
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i2 = 0; i2 < size; i2++) {
            this.mArtManagerService.clearAppProfiles((PackageParser.Package) r4.childPackages.get(i2));
        }
    }

    private void setInstallAndUpdateTime(PackageParser.Package r4, long j, long j2) {
        PackageSetting packageSetting = (PackageSetting) r4.mExtras;
        if (packageSetting != null) {
            packageSetting.firstInstallTime = j;
            packageSetting.lastUpdateTime = j2;
        }
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            PackageSetting packageSetting2 = (PackageSetting) ((PackageParser.Package) r4.childPackages.get(i)).mExtras;
            if (packageSetting2 != null) {
                packageSetting2.firstInstallTime = j;
                packageSetting2.lastUpdateTime = j2;
            }
        }
    }

    private void addSharedLibraryLPr(Set<String> set, SharedLibraryEntry sharedLibraryEntry, PackageParser.Package r5) {
        if (sharedLibraryEntry.path != null) {
            set.add(sharedLibraryEntry.path);
            return;
        }
        PackageParser.Package r0 = this.mPackages.get(sharedLibraryEntry.apk);
        if (r5 == null || !r5.packageName.equals(sharedLibraryEntry.apk) || (r0 != null && !r0.packageName.equals(r5.packageName))) {
            r5 = r0;
        }
        if (r5 != null) {
            set.addAll(r5.getAllCodePaths());
            if (r5.usesLibraryFiles != null) {
                Collections.addAll(set, r5.usesLibraryFiles);
            }
        }
    }

    private void updateSharedLibrariesLPr(PackageParser.Package r22, PackageParser.Package r23) throws PackageManagerException {
        if (r22 == null) {
            return;
        }
        Set<String> setAddSharedLibrariesLPw = r22.usesLibraries != null ? addSharedLibrariesLPw(r22.usesLibraries, null, null, r22.packageName, r23, true, r22.applicationInfo.targetSdkVersion, null) : null;
        if (r22.usesStaticLibraries != null) {
            setAddSharedLibrariesLPw = addSharedLibrariesLPw(r22.usesStaticLibraries, r22.usesStaticLibrariesVersions, r22.usesStaticLibrariesCertDigests, r22.packageName, r23, true, r22.applicationInfo.targetSdkVersion, setAddSharedLibrariesLPw);
        }
        Set<String> setAddSharedLibrariesLPw2 = setAddSharedLibrariesLPw;
        if (r22.usesOptionalLibraries != null) {
            setAddSharedLibrariesLPw2 = addSharedLibrariesLPw(r22.usesOptionalLibraries, null, null, r22.packageName, r23, false, r22.applicationInfo.targetSdkVersion, setAddSharedLibrariesLPw2);
        }
        Set<String> set = setAddSharedLibrariesLPw2;
        if (!ArrayUtils.isEmpty(set)) {
            r22.usesLibraryFiles = (String[]) set.toArray(new String[set.size()]);
        } else {
            r22.usesLibraryFiles = null;
        }
    }

    private Set<String> addSharedLibrariesLPw(List<String> list, long[] jArr, String[][] strArr, String str, PackageParser.Package r22, boolean z, int i, Set<String> set) throws PackageManagerException {
        String[] strArrComputeSignaturesSha256Digests;
        int size = list.size();
        int i2 = 0;
        Set<String> linkedHashSet = set;
        for (int i3 = 0; i3 < size; i3++) {
            String str2 = list.get(i3);
            SharedLibraryEntry sharedLibraryEntryLPr = getSharedLibraryEntryLPr(str2, jArr != null ? jArr[i3] : -1L);
            if (sharedLibraryEntryLPr == null) {
                if (z) {
                    throw new PackageManagerException(-9, "Package " + str + " requires unavailable shared library " + str2 + "; failing!");
                }
                if (DEBUG_SHARED_LIBRARIES) {
                    Slog.i(TAG, "Package " + str + " desires unavailable shared library " + str2 + "; ignoring!");
                }
            } else {
                if (jArr != null && strArr != null) {
                    if (sharedLibraryEntryLPr.info.getLongVersion() != jArr[i3]) {
                        throw new PackageManagerException(-9, "Package " + str + " requires unavailable static shared library " + str2 + " version " + sharedLibraryEntryLPr.info.getLongVersion() + "; failing!");
                    }
                    PackageParser.Package r9 = this.mPackages.get(sharedLibraryEntryLPr.apk);
                    if (r9 == null) {
                        throw new PackageManagerException(-9, "Package " + str + " requires unavailable static shared library; failing!");
                    }
                    String[] strArr2 = strArr[i3];
                    if (strArr2.length > 1) {
                        if (i >= 27) {
                            strArrComputeSignaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(r9.mSigningDetails.signatures);
                        } else {
                            Signature[] signatureArr = new Signature[1];
                            signatureArr[i2] = r9.mSigningDetails.signatures[i2];
                            strArrComputeSignaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(signatureArr);
                        }
                        if (strArr2.length != strArrComputeSignaturesSha256Digests.length) {
                            throw new PackageManagerException(-9, "Package " + str + " requires differently signed static shared library; failing!");
                        }
                        Arrays.sort(strArrComputeSignaturesSha256Digests);
                        Arrays.sort(strArr2);
                        int length = strArrComputeSignaturesSha256Digests.length;
                        for (int i4 = i2; i4 < length; i4++) {
                            if (!strArrComputeSignaturesSha256Digests[i4].equalsIgnoreCase(strArr2[i4])) {
                                throw new PackageManagerException(-9, "Package " + str + " requires differently signed static shared library; failing!");
                            }
                        }
                        i2 = 0;
                    } else {
                        i2 = 0;
                        if (!r9.mSigningDetails.hasSha256Certificate(ByteStringUtils.fromHexToByteArray(strArr2[0]))) {
                            throw new PackageManagerException(-9, "Package " + str + " requires differently signed static shared library; failing!");
                        }
                    }
                }
                if (linkedHashSet == null) {
                    linkedHashSet = new LinkedHashSet<>();
                }
                addSharedLibraryLPr(linkedHashSet, sharedLibraryEntryLPr, r22);
            }
        }
        return linkedHashSet;
    }

    private static boolean hasString(List<String> list, List<String> list2) {
        if (list == null) {
            return false;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            for (int size2 = list2.size() - 1; size2 >= 0; size2--) {
                if (list2.get(size2).equals(list.get(size))) {
                    return true;
                }
            }
        }
        return false;
    }

    private ArrayList<PackageParser.Package> updateAllSharedLibrariesLPw(PackageParser.Package r16) {
        ArrayList<PackageParser.Package> arrayList = null;
        for (PackageParser.Package r1 : this.mPackages.values()) {
            if (r16 != null && !hasString(r1.usesLibraries, r16.libraryNames) && !hasString(r1.usesOptionalLibraries, r16.libraryNames) && !ArrayUtils.contains(r1.usesStaticLibraries, r16.staticSharedLibName)) {
                return null;
            }
            if (arrayList == null) {
                arrayList = new ArrayList<>();
            }
            ArrayList<PackageParser.Package> arrayList2 = arrayList;
            arrayList2.add(r1);
            try {
                updateSharedLibrariesLPr(r1, r16);
            } catch (PackageManagerException e) {
                if (!r1.isSystem() || r1.isUpdatedSystemApp()) {
                    deletePackageLIF(r1.packageName, null, true, sUserManager.getUserIds(), r1.isUpdatedSystemApp() ? 1 : 0, null, true, null);
                }
                Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
            }
            arrayList = arrayList2;
        }
        return arrayList;
    }

    private PackageParser.Package scanPackageTracedLI(PackageParser.Package r18, int i, int i2, long j, UserHandle userHandle) throws PackageManagerException {
        int i3 = i2;
        Trace.traceBegin(262144L, "scanPackage");
        if ((i3 & 1024) == 0) {
            if (r18.childPackages != null && r18.childPackages.size() > 0) {
                i3 |= 1024;
            }
        } else {
            i3 &= -1025;
        }
        int i4 = i3;
        try {
            PackageParser.Package packageScanPackageNewLI = scanPackageNewLI(r18, i, i4, j, userHandle);
            int i5 = 0;
            int size = r18.childPackages != null ? r18.childPackages.size() : 0;
            while (i5 < size) {
                int i6 = i4;
                scanPackageNewLI((PackageParser.Package) r18.childPackages.get(i5), i, i4, j, userHandle);
                i5++;
                i4 = i6;
            }
            int i7 = i4;
            Trace.traceEnd(262144L);
            if ((i7 & 1024) != 0) {
                return scanPackageTracedLI(r18, i, i7, j, userHandle);
            }
            return packageScanPackageNewLI;
        } catch (Throwable th) {
            Trace.traceEnd(262144L);
            throw th;
        }
    }

    private static class ScanResult {
        public final List<String> changedAbiCodePath;
        public final PackageSetting pkgSetting;
        public final boolean success;

        public ScanResult(boolean z, PackageSetting packageSetting, List<String> list) {
            this.success = z;
            this.pkgSetting = packageSetting;
            this.changedAbiCodePath = list;
        }
    }

    private static class ScanRequest {
        public final PackageSetting disabledPkgSetting;
        public final boolean isPlatformPackage;
        public final PackageParser.Package oldPkg;
        public final PackageSetting oldPkgSetting;
        public final PackageSetting originalPkgSetting;
        public final int parseFlags;
        public final PackageParser.Package pkg;
        public final PackageSetting pkgSetting;
        public final String realPkgName;
        public final int scanFlags;
        public final SharedUserSetting sharedUserSetting;
        public final UserHandle user;

        public ScanRequest(PackageParser.Package r1, SharedUserSetting sharedUserSetting, PackageParser.Package r3, PackageSetting packageSetting, PackageSetting packageSetting2, PackageSetting packageSetting3, String str, int i, int i2, boolean z, UserHandle userHandle) {
            this.pkg = r1;
            this.oldPkg = r3;
            this.pkgSetting = packageSetting;
            this.sharedUserSetting = sharedUserSetting;
            this.oldPkgSetting = packageSetting == null ? null : new PackageSetting(packageSetting);
            this.disabledPkgSetting = packageSetting2;
            this.originalPkgSetting = packageSetting3;
            this.realPkgName = str;
            this.parseFlags = i;
            this.scanFlags = i2;
            this.isPlatformPackage = z;
            this.user = userHandle;
        }
    }

    private int adjustScanFlags(int i, PackageSetting packageSetting, PackageSetting packageSetting2, UserHandle userHandle, PackageParser.Package r9) throws PackageManagerException {
        int identifier;
        if (packageSetting2 != null) {
            i |= 131072;
            if ((packageSetting2.pkgPrivateFlags & 8) != 0) {
                i |= 262144;
            }
            if ((131072 & packageSetting2.pkgPrivateFlags) != 0) {
                i |= 524288;
            }
            if ((packageSetting2.pkgPrivateFlags & 262144) != 0) {
                i |= 1048576;
            }
            if ((packageSetting2.pkgPrivateFlags & 524288) != 0) {
                i |= 2097152;
            }
        }
        if (packageSetting != null) {
            if (userHandle != null) {
                identifier = userHandle.getIdentifier();
            } else {
                identifier = 0;
            }
            if (packageSetting.getInstantApp(identifier)) {
                i |= 16384;
            }
            if (packageSetting.getVirtulalPreload(identifier)) {
                i |= 65536;
            }
        }
        boolean z = (i & 1048576) != 0 && SystemProperties.getInt("ro.vndk.version", 28) < 28;
        if ((i & 262144) == 0 && !r9.isPrivileged() && r9.mSharedUserId != null && !z) {
            SharedUserSetting sharedUserLPw = null;
            try {
                sharedUserLPw = this.mSettings.getSharedUserLPw(r9.mSharedUserId, 0, 0, false);
            } catch (PackageManagerException e) {
            }
            if (sharedUserLPw != null && sharedUserLPw.isPrivileged()) {
                synchronized (this.mPackages) {
                    if (PackageManagerServiceUtils.compareSignatures(this.mSettings.mPackages.get(PLATFORM_PACKAGE_NAME).signatures.mSigningDetails.signatures, r9.mSigningDetails.signatures) != 0) {
                        i |= 262144;
                    }
                }
            }
        }
        return i;
    }

    @GuardedBy("mInstallLock")
    private PackageParser.Package scanPackageNewLI(PackageParser.Package r21, int i, int i2, long j, UserHandle userHandle) throws PackageManagerException {
        SharedUserSetting sharedUserSetting;
        PackageParser.Package r4;
        String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(r21.mRealPackage);
        String realPackageName = getRealPackageName(r21, renamedPackageLPr);
        if (realPackageName != null) {
            ensurePackageRenamed(r21, renamedPackageLPr);
        }
        PackageSetting originalPackageLocked = getOriginalPackageLocked(r21, renamedPackageLPr);
        PackageSetting packageLPr = this.mSettings.getPackageLPr(r21.packageName);
        PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(r21.packageName);
        if (this.mTransferedPackages.contains(r21.packageName)) {
            Slog.w(TAG, "Package " + r21.packageName + " was transferred to another, but its .apk remains");
        }
        int iAdjustScanFlags = adjustScanFlags(i2, packageLPr, disabledSystemPkgLPr, userHandle, r21);
        synchronized (this.mPackages) {
            applyPolicy(r21, i, iAdjustScanFlags, this.mPlatformPackage);
            assertPackageIsValid(r21, i, iAdjustScanFlags);
            if (r21.mSharedUserId != null) {
                SharedUserSetting sharedUserLPw = this.mSettings.getSharedUserLPw(r21.mSharedUserId, 0, 0, true);
                if (DEBUG_PACKAGE_SCANNING && (Integer.MIN_VALUE & i) != 0) {
                    Log.d(TAG, "Shared UserID " + r21.mSharedUserId + " (uid=" + sharedUserLPw.userId + "): packages=" + sharedUserLPw.packages);
                }
                sharedUserSetting = sharedUserLPw;
            } else {
                sharedUserSetting = null;
            }
            try {
                r4 = r21;
                try {
                    ScanRequest scanRequest = new ScanRequest(r21, sharedUserSetting, packageLPr != null ? packageLPr.pkg : null, packageLPr, disabledSystemPkgLPr, originalPackageLocked, realPackageName, i, iAdjustScanFlags, r21 == this.mPlatformPackage, userHandle);
                    ScanResult scanResultScanPackageOnlyLI = scanPackageOnlyLI(scanRequest, this.mFactoryTest, j);
                    if (scanResultScanPackageOnlyLI.success) {
                        commitScanResultsLocked(scanRequest, scanResultScanPackageOnlyLI);
                    }
                } catch (Throwable th) {
                    th = th;
                    if ((iAdjustScanFlags & 64) != 0) {
                        destroyAppDataLIF(r4, -1, 3);
                        destroyAppProfilesLIF(r4, -1);
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                r4 = r21;
            }
        }
        return r4;
    }

    @GuardedBy("mPackages")
    private void commitScanResultsLocked(ScanRequest scanRequest, ScanResult scanResult) throws Throwable {
        boolean z;
        int identifier;
        SharedLibraryEntry latestSharedLibraVersionLPr;
        PackageParser.Package r2 = scanRequest.pkg;
        PackageParser.Package r3 = scanRequest.oldPkg;
        int i = scanRequest.parseFlags;
        int i2 = scanRequest.scanFlags;
        PackageSetting packageSetting = scanRequest.oldPkgSetting;
        PackageSetting packageSetting2 = scanRequest.originalPkgSetting;
        PackageSetting packageSetting3 = scanRequest.disabledPkgSetting;
        UserHandle userHandle = scanRequest.user;
        String str = scanRequest.realPkgName;
        PackageSetting packageSetting4 = scanResult.pkgSetting;
        List<String> list = scanResult.changedAbiCodePath;
        if (scanResult.pkgSetting != scanRequest.pkgSetting) {
            if (packageSetting2 != null) {
                this.mSettings.addRenamedPackageLPw(r2.packageName, packageSetting2.name);
            }
            this.mSettings.addUserToSettingLPw(packageSetting4);
            if (packageSetting2 != null && (i2 & 1024) == 0) {
                this.mTransferedPackages.add(packageSetting2.name);
            }
        }
        r2.applicationInfo.uid = packageSetting4.appId;
        this.mSettings.writeUserRestrictionsLPw(packageSetting4, packageSetting);
        int i3 = i2 & 1024;
        if (i3 == 0 && str != null) {
            this.mTransferedPackages.add(r2.packageName);
        }
        if ((i2 & 16) == 0 && (i & 16) == 0) {
            updateSharedLibrariesLPr(r2, null);
        }
        PackageSetting packageLPr = (!r2.applicationInfo.isStaticSharedLibrary() || (latestSharedLibraVersionLPr = getLatestSharedLibraVersionLPr(r2)) == null) ? packageSetting4 : this.mSettings.getPackageLPr(latestSharedLibraVersionLPr.apk);
        KeySetManagerService keySetManagerService = this.mSettings.mKeySetManagerService;
        if (keySetManagerService.shouldCheckUpgradeKeySetLocked(packageLPr, i2)) {
            if (keySetManagerService.checkUpgradeKeySetLocked(packageLPr, r2)) {
                packageSetting4.signatures.mSigningDetails = r2.mSigningDetails;
            } else {
                if ((i & 16) == 0) {
                    throw new PackageManagerException(-7, "Package " + r2.packageName + " upgrade keys do not match the previously installed version");
                }
                packageSetting4.signatures.mSigningDetails = r2.mSigningDetails;
                reportSettingsProblem(5, "System package " + r2.packageName + " signature changed; retaining data.");
            }
        } else {
            try {
                if (PackageManagerServiceUtils.verifySignatures(packageLPr, packageSetting3, r2.mSigningDetails, isCompatSignatureUpdateNeeded(r2), isRecoverSignatureUpdateNeeded(r2))) {
                    synchronized (this.mPackages) {
                        keySetManagerService.removeAppKeySetDataLPw(r2.packageName);
                    }
                }
                packageSetting4.signatures.mSigningDetails = r2.mSigningDetails;
                if (packageLPr.sharedUser != null) {
                    if (r2.mSigningDetails.hasAncestor(packageLPr.sharedUser.signatures.mSigningDetails)) {
                        packageLPr.sharedUser.signatures.mSigningDetails = r2.mSigningDetails;
                    }
                    if (packageLPr.sharedUser.signaturesChanged == null) {
                        packageLPr.sharedUser.signaturesChanged = Boolean.FALSE;
                    }
                }
            } catch (PackageManagerException e) {
                if ((i & 16) == 0) {
                    throw e;
                }
                packageSetting4.signatures.mSigningDetails = r2.mSigningDetails;
                if (packageLPr.sharedUser != null) {
                    if (packageLPr.sharedUser.signaturesChanged != null && PackageManagerServiceUtils.compareSignatures(packageLPr.sharedUser.signatures.mSigningDetails.signatures, r2.mSigningDetails.signatures) != 0) {
                        throw new PackageManagerException(-104, "Signature mismatch for shared user: " + packageSetting4.sharedUser);
                    }
                    packageLPr.sharedUser.signatures.mSigningDetails = r2.mSigningDetails;
                    packageLPr.sharedUser.signaturesChanged = Boolean.TRUE;
                }
                reportSettingsProblem(5, "System package " + r2.packageName + " signature changed; retaining data.");
            } catch (IllegalArgumentException e2) {
                throw new PackageManagerException(-104, "Signing certificates comparison made on incomparable signing details but somehow passed verifySignatures!");
            }
        }
        if (i3 == 0 && r2.mAdoptPermissions != null) {
            for (int size = r2.mAdoptPermissions.size() - 1; size >= 0; size--) {
                String str2 = (String) r2.mAdoptPermissions.get(size);
                PackageSetting packageLPr2 = this.mSettings.getPackageLPr(str2);
                if (packageLPr2 != null && verifyPackageUpdateLPr(packageLPr2, r2)) {
                    Slog.i(TAG, "Adopting permissions from " + str2 + " to " + r2.packageName);
                    this.mSettings.mPermissions.transferPermissions(str2, r2.packageName);
                }
            }
        }
        if (list != null && list.size() > 0) {
            z = true;
            for (int size2 = list.size() - 1; size2 >= 0; size2--) {
                try {
                    this.mInstaller.rmdex(list.get(size2), InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                } catch (Installer.InstallerException e3) {
                }
            }
        } else {
            z = true;
        }
        if (i3 != 0) {
            if (packageSetting != null) {
                synchronized (this.mPackages) {
                    this.mSettings.mPackages.put(packageSetting.name, packageSetting);
                }
                return;
            }
            return;
        }
        if (userHandle != null) {
            identifier = userHandle.getIdentifier();
        } else {
            identifier = 0;
        }
        if ((Integer.MIN_VALUE & i) == 0) {
            z = false;
        }
        commitPackageSettings(r2, r3, packageSetting4, userHandle, i2, z);
        if (packageSetting4.getInstantApp(identifier)) {
            this.mInstantAppRegistry.addInstantAppLPw(identifier, packageSetting4.appId);
        }
    }

    private static String getRealPackageName(PackageParser.Package r0, String str) {
        if (isPackageRenamed(r0, str)) {
            return r0.mRealPackage;
        }
        return null;
    }

    private static boolean isPackageRenamed(PackageParser.Package r1, String str) {
        return r1.mOriginalPackages != null && r1.mOriginalPackages.contains(str);
    }

    @GuardedBy("mPackages")
    private PackageSetting getOriginalPackageLocked(PackageParser.Package r6, String str) {
        if (!isPackageRenamed(r6, str)) {
            return null;
        }
        for (int size = r6.mOriginalPackages.size() - 1; size >= 0; size--) {
            PackageSetting packageLPr = this.mSettings.getPackageLPr((String) r6.mOriginalPackages.get(size));
            if (packageLPr != null && verifyPackageUpdateLPr(packageLPr, r6)) {
                if (packageLPr.sharedUser != null) {
                    if (!packageLPr.sharedUser.name.equals(r6.mSharedUserId)) {
                        Slog.w(TAG, "Unable to migrate data from " + packageLPr.name + " to " + r6.packageName + ": old uid " + packageLPr.sharedUser.name + " differs from " + r6.mSharedUserId);
                    }
                } else if (DEBUG_UPGRADE) {
                    Log.v(TAG, "Renaming new package " + r6.packageName + " to old name " + packageLPr.name);
                }
                return packageLPr;
            }
        }
        return null;
    }

    private static void ensurePackageRenamed(PackageParser.Package r1, String str) {
        if (r1.mOriginalPackages == null || !r1.mOriginalPackages.contains(str) || r1.packageName.equals(str)) {
            return;
        }
        r1.setPackageName(str);
    }

    @GuardedBy("mInstallLock")
    private static ScanResult scanPackageOnlyLI(ScanRequest scanRequest, boolean z, long j) throws Throwable {
        String str;
        String str2;
        String str3;
        String[] strArr;
        boolean z2;
        boolean z3;
        String str4;
        UserHandle userHandle;
        SharedUserSetting sharedUserSetting;
        int i;
        int i2;
        PackageSetting packageSetting;
        String str5;
        PackageSetting packageSetting2;
        int identifier;
        int i3;
        char c;
        boolean z4;
        String str6;
        PackageParser.Package r3 = scanRequest.pkg;
        PackageSetting packageSettingCreateNewSetting = scanRequest.pkgSetting;
        PackageSetting packageSetting3 = scanRequest.disabledPkgSetting;
        PackageSetting packageSetting4 = scanRequest.originalPkgSetting;
        int i4 = scanRequest.parseFlags;
        int i5 = scanRequest.scanFlags;
        String str7 = scanRequest.realPkgName;
        SharedUserSetting sharedUserSetting2 = scanRequest.sharedUserSetting;
        UserHandle userHandle2 = scanRequest.user;
        boolean z5 = scanRequest.isPlatformPackage;
        if (DEBUG_PACKAGE_SCANNING && (Integer.MIN_VALUE & i4) != 0) {
            Log.d(TAG, "Scanning package " + r3.packageName);
        }
        DexManager.maybeLogUnexpectedPackageDetails(r3);
        new File(r3.codePath);
        File file = new File(r3.applicationInfo.getCodePath());
        File file2 = new File(r3.applicationInfo.getResourcePath());
        boolean z6 = (i5 & 8192) != 0;
        List<String> listAdjustCpuAbisForSharedUserLPw = null;
        if (z6) {
            str = null;
            str2 = null;
        } else if (packageSettingCreateNewSetting != null) {
            str2 = packageSettingCreateNewSetting.primaryCpuAbiString;
            str = packageSettingCreateNewSetting.secondaryCpuAbiString;
        } else {
            str = null;
            str2 = null;
            z6 = true;
        }
        if (packageSettingCreateNewSetting == null || packageSettingCreateNewSetting.sharedUser == sharedUserSetting2) {
            str3 = str;
        } else {
            StringBuilder sb = new StringBuilder();
            str3 = str;
            sb.append("Package ");
            sb.append(r3.packageName);
            sb.append(" shared user changed from ");
            sb.append(packageSettingCreateNewSetting.sharedUser != null ? packageSettingCreateNewSetting.sharedUser.name : "<nothing>");
            sb.append(" to ");
            sb.append(sharedUserSetting2 != null ? sharedUserSetting2.name : "<nothing>");
            sb.append("; replacing with new");
            reportSettingsProblem(5, sb.toString());
            packageSettingCreateNewSetting = null;
        }
        if (r3.usesStaticLibraries != null) {
            strArr = new String[r3.usesStaticLibraries.size()];
            r3.usesStaticLibraries.toArray(strArr);
        } else {
            strArr = null;
        }
        boolean z7 = packageSettingCreateNewSetting == null;
        if (z7) {
            if (r3.parentPackage != null) {
                str6 = r3.parentPackage.packageName;
            } else {
                str6 = null;
            }
            z2 = z5;
            z3 = z6;
            str5 = str3;
            str4 = str2;
            userHandle = userHandle2;
            sharedUserSetting = sharedUserSetting2;
            i = i5;
            i2 = i4;
            packageSetting2 = packageSetting3;
            packageSettingCreateNewSetting = Settings.createNewSetting(r3.packageName, packageSetting4, packageSetting3, str7, sharedUserSetting2, file, file2, r3.applicationInfo.nativeLibraryRootDir, r3.applicationInfo.primaryCpuAbi, r3.applicationInfo.secondaryCpuAbi, r3.mVersionCode, r3.applicationInfo.flags, r3.applicationInfo.privateFlags, userHandle, true, (i5 & 16384) != 0, (65536 & i5) != 0, str6, r3.getChildPackageNames(), UserManagerService.getInstance(), strArr, r3.usesStaticLibrariesVersions);
            packageSetting = packageSetting4;
        } else {
            z2 = z5;
            z3 = z6;
            str4 = str2;
            userHandle = userHandle2;
            sharedUserSetting = sharedUserSetting2;
            i = i5;
            i2 = i4;
            packageSetting = packageSetting4;
            str5 = str3;
            packageSetting2 = packageSetting3;
            Settings.updatePackageSetting(packageSettingCreateNewSetting, packageSetting2, sharedUserSetting, file, file2, r3.applicationInfo.nativeLibraryDir, r3.applicationInfo.primaryCpuAbi, r3.applicationInfo.secondaryCpuAbi, r3.applicationInfo.flags, r3.applicationInfo.privateFlags, r3.getChildPackageNames(), UserManagerService.getInstance(), strArr, r3.usesStaticLibrariesVersions);
        }
        if (z7 && packageSetting != null) {
            r3.setPackageName(packageSetting.name);
            reportSettingsProblem(5, "New package " + packageSettingCreateNewSetting.realName + " renamed to replace old package " + packageSettingCreateNewSetting.name);
        }
        UserHandle userHandle3 = userHandle;
        if (userHandle3 != null) {
            identifier = userHandle3.getIdentifier();
        } else {
            identifier = 0;
        }
        if (!z7) {
            i3 = i;
            setInstantAppForUser(packageSettingCreateNewSetting, identifier, (i3 & 16384) != 0, (32768 & i3) != 0);
        } else {
            i3 = i;
        }
        if (packageSetting2 != null) {
            r3.applicationInfo.flags |= 128;
        }
        SharedUserSetting sharedUserSetting3 = sharedUserSetting;
        r3.applicationInfo.seInfo = SELinuxMMAC.getSeInfo(r3, sharedUserSetting3 != null ? sharedUserSetting3.isPrivileged() | r3.isPrivileged() : r3.isPrivileged(), r3.applicationInfo.targetSandboxVersion, (sharedUserSetting3 == null || sharedUserSetting3.packages.size() == 0) ? r3.applicationInfo.targetSdkVersion : sharedUserSetting3.seInfoTargetSdkVersion);
        ApplicationInfo applicationInfo = r3.applicationInfo;
        if (identifier == -1) {
            identifier = 0;
        }
        applicationInfo.seInfoUser = SELinuxUtil.assignSeinfoUser(packageSettingCreateNewSetting.readUserState(identifier));
        r3.mExtras = packageSettingCreateNewSetting;
        r3.applicationInfo.processName = fixProcessName(r3.applicationInfo.packageName, r3.applicationInfo.processName);
        if (!z2) {
            c = 0;
            r3.applicationInfo.initForUser(0);
        } else {
            c = 0;
        }
        String strDeriveAbiOverride = PackageManagerServiceUtils.deriveAbiOverride(r3.cpuAbiOverride, packageSettingCreateNewSetting);
        int i6 = i3 & 4;
        if (i6 == 0) {
            if (z3) {
                Trace.traceBegin(262144L, "derivePackageAbi");
                derivePackageAbi(r3, strDeriveAbiOverride, !r3.isLibrary());
                Trace.traceEnd(262144L);
                if (isSystemApp(r3) && !r3.isUpdatedSystemApp() && r3.applicationInfo.primaryCpuAbi == null) {
                    setBundledAppAbisAndRoots(r3, packageSettingCreateNewSetting);
                    setNativeLibraryPaths(r3, sAppLib32InstallDir);
                }
            } else {
                r3.applicationInfo.primaryCpuAbi = str4;
                r3.applicationInfo.secondaryCpuAbi = str5;
                setNativeLibraryPaths(r3, sAppLib32InstallDir);
                if (DEBUG_ABI_SELECTION) {
                    Slog.i(TAG, "Using ABIS and native lib paths from settings : " + r3.packageName + " " + r3.applicationInfo.primaryCpuAbi + ", " + r3.applicationInfo.secondaryCpuAbi);
                }
            }
        } else {
            if ((i3 & 256) != 0) {
                r3.applicationInfo.primaryCpuAbi = packageSettingCreateNewSetting.primaryCpuAbiString;
                r3.applicationInfo.secondaryCpuAbi = packageSettingCreateNewSetting.secondaryCpuAbiString;
            }
            setNativeLibraryPaths(r3, sAppLib32InstallDir);
        }
        if (z2) {
            r3.applicationInfo.primaryCpuAbi = VMRuntime.getRuntime().is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[c] : Build.SUPPORTED_32_BIT_ABIS[c];
        }
        if ((i3 & 1) == 0 && i6 != 0 && strDeriveAbiOverride == null && r3.packageName != null) {
            Slog.w(TAG, "Ignoring persisted ABI override " + strDeriveAbiOverride + " for package " + r3.packageName);
        }
        packageSettingCreateNewSetting.primaryCpuAbiString = r3.applicationInfo.primaryCpuAbi;
        packageSettingCreateNewSetting.secondaryCpuAbiString = r3.applicationInfo.secondaryCpuAbi;
        packageSettingCreateNewSetting.cpuAbiOverrideString = strDeriveAbiOverride;
        r3.cpuAbiOverride = strDeriveAbiOverride;
        if (DEBUG_ABI_SELECTION) {
            Slog.d(TAG, "Resolved nativeLibraryRoot for " + r3.packageName + " to root=" + r3.applicationInfo.nativeLibraryRootDir + ", isa=" + r3.applicationInfo.nativeLibraryRootRequiresIsa);
        }
        packageSettingCreateNewSetting.legacyNativeLibraryPathString = r3.applicationInfo.nativeLibraryRootDir;
        if (DEBUG_ABI_SELECTION) {
            Log.d(TAG, "Abis for package[" + r3.packageName + "] are primary=" + r3.applicationInfo.primaryCpuAbi + " secondary=" + r3.applicationInfo.secondaryCpuAbi);
        }
        if ((i3 & 16) == 0 && packageSettingCreateNewSetting.sharedUser != null) {
            listAdjustCpuAbisForSharedUserLPw = adjustCpuAbisForSharedUserLPw(packageSettingCreateNewSetting.sharedUser.packages, r3);
        }
        List<String> list = listAdjustCpuAbisForSharedUserLPw;
        if (z && r3.requestedPermissions.contains("android.permission.FACTORY_TEST")) {
            r3.applicationInfo.flags |= 16;
        }
        if (isSystemApp(r3)) {
            z4 = true;
            packageSettingCreateNewSetting.isOrphaned = true;
        } else {
            z4 = true;
        }
        long lastModifiedTime = PackageManagerServiceUtils.getLastModifiedTime(r3);
        if (j != 0) {
            if (packageSettingCreateNewSetting.firstInstallTime == 0) {
                packageSettingCreateNewSetting.lastUpdateTime = j;
                packageSettingCreateNewSetting.firstInstallTime = j;
            } else if ((i3 & 8) != 0) {
                packageSettingCreateNewSetting.lastUpdateTime = j;
            }
        } else if (packageSettingCreateNewSetting.firstInstallTime == 0) {
            packageSettingCreateNewSetting.lastUpdateTime = lastModifiedTime;
            packageSettingCreateNewSetting.firstInstallTime = lastModifiedTime;
        } else if ((i2 & 16) != 0 && lastModifiedTime != packageSettingCreateNewSetting.timeStamp) {
            packageSettingCreateNewSetting.lastUpdateTime = lastModifiedTime;
        }
        packageSettingCreateNewSetting.setTimeStamp(lastModifiedTime);
        packageSettingCreateNewSetting.pkg = r3;
        packageSettingCreateNewSetting.pkgFlags = r3.applicationInfo.flags;
        if (r3.getLongVersionCode() != packageSettingCreateNewSetting.versionCode) {
            packageSettingCreateNewSetting.versionCode = r3.getLongVersionCode();
        }
        String str8 = r3.applicationInfo.volumeUuid;
        if (!Objects.equals(str8, packageSettingCreateNewSetting.volumeUuid)) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Update");
            sb2.append(packageSettingCreateNewSetting.isSystem() ? " system" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb2.append(" package ");
            sb2.append(r3.packageName);
            sb2.append(" volume from ");
            sb2.append(packageSettingCreateNewSetting.volumeUuid);
            sb2.append(" to ");
            sb2.append(str8);
            Slog.i(TAG, sb2.toString());
            packageSettingCreateNewSetting.volumeUuid = str8;
        }
        return new ScanResult(z4, packageSettingCreateNewSetting, list);
    }

    private static boolean apkHasCode(String str) throws Throwable {
        StrictJarFile strictJarFile;
        StrictJarFile strictJarFile2 = null;
        try {
            strictJarFile = new StrictJarFile(str, false, false);
        } catch (IOException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            boolean z = strictJarFile.findEntry("classes.dex") != null;
            try {
                strictJarFile.close();
            } catch (IOException e2) {
            }
            return z;
        } catch (IOException e3) {
            strictJarFile2 = strictJarFile;
            if (strictJarFile2 != null) {
                try {
                    strictJarFile2.close();
                } catch (IOException e4) {
                }
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
            strictJarFile2 = strictJarFile;
            if (strictJarFile2 != null) {
                try {
                    strictJarFile2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    private static void assertCodePolicy(PackageParser.Package r5) throws PackageManagerException {
        if (((r5.applicationInfo.flags & 4) != 0) && !apkHasCode(r5.baseCodePath)) {
            throw new PackageManagerException(-2, "Package " + r5.baseCodePath + " code is missing");
        }
        if (!ArrayUtils.isEmpty(r5.splitCodePaths)) {
            for (int i = 0; i < r5.splitCodePaths.length; i++) {
                if (((r5.splitFlags[i] & 4) != 0) && !apkHasCode(r5.splitCodePaths[i])) {
                    throw new PackageManagerException(-2, "Package " + r5.splitCodePaths[i] + " code is missing");
                }
            }
        }
    }

    private static void applyPolicy(PackageParser.Package r9, int i, int i2, PackageParser.Package r12) {
        if ((i2 & 131072) != 0) {
            r9.applicationInfo.flags |= 1;
            if (r9.applicationInfo.isDirectBootAware()) {
                for (PackageParser.Service service : r9.services) {
                    ServiceInfo serviceInfo = service.info;
                    service.info.directBootAware = true;
                    serviceInfo.encryptionAware = true;
                }
                for (PackageParser.Provider provider : r9.providers) {
                    ProviderInfo providerInfo = provider.info;
                    provider.info.directBootAware = true;
                    providerInfo.encryptionAware = true;
                }
                for (PackageParser.Activity activity : r9.activities) {
                    ActivityInfo activityInfo = activity.info;
                    activity.info.directBootAware = true;
                    activityInfo.encryptionAware = true;
                }
                for (PackageParser.Activity activity2 : r9.receivers) {
                    ActivityInfo activityInfo2 = activity2.info;
                    activity2.info.directBootAware = true;
                    activityInfo2.encryptionAware = true;
                }
            }
            if (PackageManagerServiceUtils.compressedFileExists(r9.codePath)) {
                r9.isStub = true;
            }
        } else {
            r9.coreApp = false;
            r9.applicationInfo.flags &= -9;
            r9.applicationInfo.privateFlags &= -33;
            r9.applicationInfo.privateFlags &= -65;
            r9.protectedBroadcasts = null;
            if (r9.permissionGroups != null && r9.permissionGroups.size() > 0) {
                for (int size = r9.permissionGroups.size() - 1; size >= 0; size--) {
                    ((PackageParser.PermissionGroup) r9.permissionGroups.get(size)).info.priority = 0;
                }
            }
        }
        int i3 = i2 & 262144;
        if (i3 == 0) {
            if (r9.receivers != null) {
                for (int size2 = r9.receivers.size() - 1; size2 >= 0; size2--) {
                    PackageParser.Activity activity3 = (PackageParser.Activity) r9.receivers.get(size2);
                    if ((activity3.info.flags & 1073741824) != 0) {
                        activity3.info.exported = false;
                    }
                }
            }
            if (r9.services != null) {
                for (int size3 = r9.services.size() - 1; size3 >= 0; size3--) {
                    PackageParser.Service service2 = (PackageParser.Service) r9.services.get(size3);
                    if ((service2.info.flags & 1073741824) != 0) {
                        service2.info.exported = false;
                    }
                }
            }
            if (r9.providers != null) {
                for (int size4 = r9.providers.size() - 1; size4 >= 0; size4--) {
                    PackageParser.Provider provider2 = (PackageParser.Provider) r9.providers.get(size4);
                    if ((provider2.info.flags & 1073741824) != 0) {
                        provider2.info.exported = false;
                    }
                }
            }
        }
        if (i3 != 0) {
            r9.applicationInfo.privateFlags |= 8;
        }
        if ((i2 & 524288) != 0) {
            ApplicationInfo applicationInfo = r9.applicationInfo;
            applicationInfo.privateFlags = 131072 | applicationInfo.privateFlags;
        }
        if ((i2 & 1048576) != 0) {
            ApplicationInfo applicationInfo2 = r9.applicationInfo;
            applicationInfo2.privateFlags = 262144 | applicationInfo2.privateFlags;
        }
        if ((i2 & 2097152) != 0) {
            r9.applicationInfo.privateFlags |= 524288;
        }
        if (PLATFORM_PACKAGE_NAME.equals(r9.packageName) || (r12 != null && PackageManagerServiceUtils.compareSignatures(r12.mSigningDetails.signatures, r9.mSigningDetails.signatures) == 0)) {
            ApplicationInfo applicationInfo3 = r9.applicationInfo;
            applicationInfo3.privateFlags = 1048576 | applicationInfo3.privateFlags;
        }
        if (!isSystemApp(r9)) {
            r9.mOriginalPackages = null;
            r9.mRealPackage = null;
            r9.mAdoptPermissions = null;
        }
    }

    private static <T> T assertNotNull(T t, String str) throws PackageManagerException {
        if (t == null) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, str);
        }
        return t;
    }

    private void assertPackageIsValid(PackageParser.Package r22, int i, int i2) throws PackageManagerException {
        PackageParser.Package r0;
        SharedUserSetting sharedUserLPw;
        long longVersionCode;
        long jMin;
        if ((i & 64) != 0) {
            assertCodePolicy(r22);
        }
        if (r22.applicationInfo.getCodePath() == null || r22.applicationInfo.getResourcePath() == null) {
            throw new PackageManagerException(-2, "Code and resource paths haven't been set correctly");
        }
        this.mSettings.mKeySetManagerService.assertScannedPackageValid(r22);
        synchronized (this.mPackages) {
            if (r22.packageName.equals(PLATFORM_PACKAGE_NAME) && this.mAndroidApplication != null) {
                Slog.w(TAG, "*************************************************");
                Slog.w(TAG, "Core android package being redefined.  Skipping.");
                Slog.w(TAG, " codePath=" + r22.codePath);
                Slog.w(TAG, "*************************************************");
                throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
            }
            sPmsExt.checkMtkResPkg(r22);
            if (this.mPackages.containsKey(r22.packageName)) {
                throw new PackageManagerException(-5, "Application package " + r22.packageName + " already installed.  Skipping duplicate.");
            }
            if (r22.applicationInfo.isStaticSharedLibrary()) {
                if (this.mPackages.containsKey(r22.manifestPackageName)) {
                    throw new PackageManagerException("Duplicate static shared lib provider package");
                }
                if (r22.applicationInfo.targetSdkVersion < 26) {
                    throw new PackageManagerException("Packages declaring static-shared libs must target O SDK or higher");
                }
                if ((i2 & 16384) != 0) {
                    throw new PackageManagerException("Packages declaring static-shared libs cannot be instant apps");
                }
                if (!ArrayUtils.isEmpty(r22.mOriginalPackages)) {
                    throw new PackageManagerException("Packages declaring static-shared libs cannot be renamed");
                }
                if (!ArrayUtils.isEmpty(r22.childPackages)) {
                    throw new PackageManagerException("Packages declaring static-shared libs cannot have child packages");
                }
                if (!ArrayUtils.isEmpty(r22.libraryNames)) {
                    throw new PackageManagerException("Packages declaring static-shared libs cannot declare dynamic libs");
                }
                if (r22.mSharedUserId != null) {
                    throw new PackageManagerException("Packages declaring static-shared libs cannot declare shared users");
                }
                if (!r22.activities.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare activities");
                }
                if (!r22.services.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare services");
                }
                if (!r22.providers.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare content providers");
                }
                if (!r22.receivers.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare broadcast receivers");
                }
                if (!r22.permissionGroups.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare permission groups");
                }
                if (!r22.permissions.isEmpty()) {
                    throw new PackageManagerException("Static shared libs cannot declare permissions");
                }
                if (r22.protectedBroadcasts != null) {
                    throw new PackageManagerException("Static shared libs cannot declare protected broadcasts");
                }
                if (r22.mOverlayTarget != null) {
                    throw new PackageManagerException("Static shared libs cannot be overlay targets");
                }
                LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(r22.staticSharedLibName);
                if (longSparseArray == null) {
                    longVersionCode = Long.MIN_VALUE;
                    jMin = Long.MAX_VALUE;
                } else {
                    int size = longSparseArray.size();
                    jMin = Long.MAX_VALUE;
                    long jMax = Long.MIN_VALUE;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= size) {
                            longVersionCode = jMax;
                            break;
                        }
                        SharedLibraryInfo sharedLibraryInfo = longSparseArray.valueAt(i3).info;
                        longVersionCode = sharedLibraryInfo.getDeclaringPackage().getLongVersionCode();
                        int i4 = i3;
                        if (sharedLibraryInfo.getLongVersion() >= r22.staticSharedLibVersion) {
                            long j = jMax;
                            if (sharedLibraryInfo.getLongVersion() > r22.staticSharedLibVersion) {
                                jMin = Math.min(jMin, longVersionCode - 1);
                                jMax = j;
                            } else {
                                jMin = longVersionCode;
                                break;
                            }
                        } else {
                            jMax = Math.max(jMax, longVersionCode + 1);
                        }
                        i3 = i4 + 1;
                    }
                }
                if (r22.getLongVersionCode() < longVersionCode || r22.getLongVersionCode() > jMin) {
                    throw new PackageManagerException("Static shared lib version codes must be ordered as lib versions");
                }
            }
            if (r22.childPackages != null && !r22.childPackages.isEmpty()) {
                if ((262144 & i2) == 0) {
                    throw new PackageManagerException("Only privileged apps can add child packages. Ignoring package " + r22.packageName);
                }
                int size2 = r22.childPackages.size();
                for (int i5 = 0; i5 < size2; i5++) {
                    if (this.mSettings.hasOtherDisabledSystemPkgWithChildLPr(r22.packageName, ((PackageParser.Package) r22.childPackages.get(i5)).packageName)) {
                        throw new PackageManagerException("Can't override child of another disabled app. Ignoring package " + r22.packageName);
                    }
                }
            }
            if ((i2 & 128) != 0) {
                if (this.mExpectingBetter.containsKey(r22.packageName)) {
                    PackageManagerServiceUtils.logCriticalInfo(5, "Relax SCAN_REQUIRE_KNOWN requirement for package " + r22.packageName);
                } else {
                    PackageSetting packageLPr = this.mSettings.getPackageLPr(r22.packageName);
                    if (packageLPr != null) {
                        if (DEBUG_PACKAGE_SCANNING) {
                            Log.d(TAG, "Examining " + r22.codePath + " and requiring known paths " + packageLPr.codePathString + " & " + packageLPr.resourcePathString);
                        }
                        if (!r22.applicationInfo.getCodePath().equals(packageLPr.codePathString) || !r22.applicationInfo.getResourcePath().equals(packageLPr.resourcePathString)) {
                            throw new PackageManagerException(-23, "Application package " + r22.packageName + " found at " + r22.applicationInfo.getCodePath() + " but expected at " + packageLPr.codePathString + "; ignoring.");
                        }
                    } else {
                        throw new PackageManagerException(-19, "Application package " + r22.packageName + " not found; ignoring.");
                    }
                }
            }
            if ((i2 & 4) != 0) {
                int size3 = r22.providers.size();
                for (int i6 = 0; i6 < size3; i6++) {
                    PackageParser.Provider provider = (PackageParser.Provider) r22.providers.get(i6);
                    if (provider.info.authority != null) {
                        String[] strArrSplit = provider.info.authority.split(";");
                        for (int i7 = 0; i7 < strArrSplit.length; i7++) {
                            if (this.mProvidersByAuthority.containsKey(strArrSplit[i7])) {
                                PackageParser.Provider provider2 = this.mProvidersByAuthority.get(strArrSplit[i7]);
                                throw new PackageManagerException(-13, "Can't install because provider name " + strArrSplit[i7] + " (in package " + r22.applicationInfo.packageName + ") is already used by " + ((provider2 == null || provider2.getComponentName() == null) ? "?" : provider2.getComponentName().getPackageName()));
                            }
                        }
                    }
                }
            }
            if (!r22.isPrivileged() && r22.mSharedUserId != null) {
                try {
                    sharedUserLPw = this.mSettings.getSharedUserLPw(r22.mSharedUserId, 0, 0, false);
                } catch (PackageManagerException e) {
                    sharedUserLPw = null;
                }
                if (sharedUserLPw != null && sharedUserLPw.isPrivileged()) {
                    PackageSetting packageSetting = this.mSettings.mPackages.get(PLATFORM_PACKAGE_NAME);
                    if (packageSetting.signatures.mSigningDetails != PackageParser.SigningDetails.UNKNOWN && PackageManagerServiceUtils.compareSignatures(packageSetting.signatures.mSigningDetails.signatures, r22.mSigningDetails.signatures) != 0) {
                        throw new PackageManagerException("Apps that share a user with a privileged app must themselves be marked as privileged. " + r22.packageName + " shares privileged user " + r22.mSharedUserId + ".");
                    }
                }
            }
            if (r22.mOverlayTarget != null) {
                if ((131072 & i2) != 0) {
                    if ((i & 16) == 0) {
                        PackageSetting packageSetting2 = (PackageSetting) assertNotNull(this.mSettings.getPackageLPr(r22.packageName), "previous package state not present");
                        PackageParser.Package r4 = packageSetting2.pkg;
                        if (r4 == null) {
                            try {
                                r0 = new PackageParser().parsePackage(packageSetting2.codePath, i | 16);
                            } catch (PackageParser.PackageParserException e2) {
                                Slog.w(TAG, "failed to parse " + packageSetting2.codePath, e2);
                                r0 = r4;
                            }
                            if (r0 != null && r0.mOverlayIsStatic) {
                                throw new PackageManagerException("Overlay " + r22.packageName + " is static and cannot be upgraded.");
                            }
                            if (!r22.mOverlayIsStatic) {
                                throw new PackageManagerException("Overlay " + r22.packageName + " cannot be upgraded into a static overlay.");
                            }
                        } else {
                            r0 = r4;
                            if (r0 != null) {
                                throw new PackageManagerException("Overlay " + r22.packageName + " is static and cannot be upgraded.");
                            }
                            if (!r22.mOverlayIsStatic) {
                            }
                        }
                    }
                } else {
                    if (r22.mOverlayIsStatic) {
                        throw new PackageManagerException("Overlay " + r22.packageName + " is static but not pre-installed.");
                    }
                    PackageSetting packageLPr2 = this.mSettings.getPackageLPr(PLATFORM_PACKAGE_NAME);
                    if (packageLPr2.signatures.mSigningDetails != PackageParser.SigningDetails.UNKNOWN && PackageManagerServiceUtils.compareSignatures(packageLPr2.signatures.mSigningDetails.signatures, r22.mSigningDetails.signatures) != 0) {
                        throw new PackageManagerException("Overlay " + r22.packageName + " must be signed with the platform certificate.");
                    }
                }
            }
        }
    }

    private boolean addSharedLibraryLPw(String str, String str2, String str3, long j, int i, String str4, long j2) {
        int i2;
        String str5;
        LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(str3);
        if (longSparseArray == null) {
            longSparseArray = new LongSparseArray<>();
            this.mSharedLibraries.put(str3, longSparseArray);
            i2 = i;
            if (i2 == 2) {
                str5 = str4;
                this.mStaticLibsByDeclaringPackage.put(str5, longSparseArray);
            } else {
                str5 = str4;
            }
        } else {
            i2 = i;
            str5 = str4;
            if (longSparseArray.indexOfKey(j) >= 0) {
                return false;
            }
        }
        longSparseArray.put(j, new SharedLibraryEntry(str, str2, str3, j, i2, str5, j2));
        return true;
    }

    private boolean removeSharedLibraryLPw(String str, long j) {
        int iIndexOfKey;
        LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(str);
        if (longSparseArray == null || (iIndexOfKey = longSparseArray.indexOfKey(j)) < 0) {
            return false;
        }
        SharedLibraryEntry sharedLibraryEntryValueAt = longSparseArray.valueAt(iIndexOfKey);
        longSparseArray.remove(j);
        if (longSparseArray.size() <= 0) {
            this.mSharedLibraries.remove(str);
            if (sharedLibraryEntryValueAt.info.getType() == 2) {
                this.mStaticLibsByDeclaringPackage.remove(sharedLibraryEntryValueAt.info.getDeclaringPackage().getPackageName());
                return true;
            }
            return true;
        }
        return true;
    }

    private void commitPackageSettings(final PackageParser.Package r22, final PackageParser.Package r23, PackageSetting packageSetting, UserHandle userHandle, int i, boolean z) throws Throwable {
        ArrayMap<String, PackageParser.Package> arrayMap;
        int i2;
        boolean z2;
        String str;
        ?? UpdateAllSharedLibrariesLPw;
        List<String> listAddAllPermissions;
        boolean z3;
        PackageParser.Provider provider;
        int i3;
        String str2;
        int i4;
        String str3 = r22.packageName;
        if (this.mCustomResolverComponentName != null && this.mCustomResolverComponentName.getPackageName().equals(r22.packageName)) {
            setUpCustomResolverActivity(r22);
        }
        if (r22.packageName.equals(PLATFORM_PACKAGE_NAME)) {
            synchronized (this.mPackages) {
                if ((i & 1024) == 0) {
                    try {
                        this.mPlatformPackage = r22;
                        r22.mVersionCode = this.mSdkVersion;
                        r22.mVersionCodeMajor = 0;
                        this.mAndroidApplication = r22.applicationInfo;
                        if (!this.mResolverReplaced) {
                            this.mResolveActivity.applicationInfo = this.mAndroidApplication;
                            this.mResolveActivity.name = ResolverActivity.class.getName();
                            this.mResolveActivity.packageName = this.mAndroidApplication.packageName;
                            this.mResolveActivity.processName = "system:ui";
                            this.mResolveActivity.launchMode = 0;
                            this.mResolveActivity.documentLaunchMode = 3;
                            this.mResolveActivity.flags = 32;
                            this.mResolveActivity.theme = R.style.Theme.Material.Dialog.Alert;
                            this.mResolveActivity.exported = true;
                            this.mResolveActivity.enabled = true;
                            this.mResolveActivity.resizeMode = 2;
                            this.mResolveActivity.configChanges = 3504;
                            this.mResolveInfo.activityInfo = this.mResolveActivity;
                            this.mResolveInfo.priority = 0;
                            this.mResolveInfo.preferredOrder = 0;
                            this.mResolveInfo.match = 0;
                            this.mResolveComponentName = new ComponentName(this.mAndroidApplication.packageName, this.mResolveActivity.name);
                        }
                    } finally {
                    }
                }
            }
        }
        ArrayMap<String, PackageParser.Package> arrayMap2 = this.mPackages;
        synchronized (arrayMap2) {
            try {
                try {
                    if (r22.staticSharedLibName != null) {
                        arrayMap = arrayMap2;
                        i2 = 1;
                        if (!addSharedLibraryLPw(null, r22.packageName, r22.staticSharedLibName, r22.staticSharedLibVersion, 2, r22.manifestPackageName, r22.getLongVersionCode())) {
                            Slog.w(TAG, "Package " + r22.packageName + " library " + r22.staticSharedLibName + " already exists; skipping");
                        } else {
                            z2 = true;
                            String str4 = null;
                            if (z2 && (r22.applicationInfo.flags & i2) != 0 && r22.libraryNames != null) {
                                int i5 = 0;
                                while (i5 < r22.libraryNames.size()) {
                                    String str5 = (String) r22.libraryNames.get(i5);
                                    if (r22.isUpdatedSystemApp()) {
                                        PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(r22.packageName);
                                        if (disabledSystemPkgLPr.pkg != null && disabledSystemPkgLPr.pkg.libraryNames != null) {
                                            for (int i6 = 0; i6 < disabledSystemPkgLPr.pkg.libraryNames.size(); i6++) {
                                                if (str5.equals(disabledSystemPkgLPr.pkg.libraryNames.get(i6))) {
                                                    i3 = i2;
                                                    break;
                                                }
                                            }
                                            i3 = 0;
                                        } else {
                                            i3 = 0;
                                        }
                                    } else {
                                        i3 = i2;
                                    }
                                    if (i3 != 0) {
                                        str2 = str4;
                                        i4 = i5;
                                        if (!addSharedLibraryLPw(null, r22.packageName, str5, -1L, 1, r22.packageName, r22.getLongVersionCode())) {
                                            Slog.w(TAG, "Package " + r22.packageName + " library " + str5 + " already exists; skipping");
                                        }
                                    } else {
                                        str2 = str4;
                                        i4 = i5;
                                        Slog.w(TAG, "Package " + r22.packageName + " declares lib " + str5 + " that is not declared on system image; skipping");
                                    }
                                    i5 = i4 + 1;
                                    str4 = str2;
                                    i2 = 1;
                                }
                                str = str4;
                                if ((i & 16) == 0) {
                                    UpdateAllSharedLibrariesLPw = updateAllSharedLibrariesLPw(r22);
                                }
                                if ((i & 16) == 0 && (i & 2048) == 0 && (i & 4096) == 0) {
                                    checkPackageFrozen(str3);
                                }
                                if (UpdateAllSharedLibrariesLPw != 0) {
                                    for (int i7 = 0; i7 < UpdateAllSharedLibrariesLPw.size(); i7++) {
                                        PackageParser.Package r2 = (PackageParser.Package) UpdateAllSharedLibrariesLPw.get(i7);
                                        killApplication(r2.applicationInfo.packageName, r2.applicationInfo.uid, "update lib");
                                    }
                                }
                                Trace.traceBegin(262144L, "updateSettings");
                                synchronized (this.mPackages) {
                                    this.mSettings.insertPackageSettingLPw(packageSetting, r22);
                                    this.mPackages.put(r22.applicationInfo.packageName, r22);
                                    Iterator<PackageCleanItem> it = this.mSettings.mPackagesToBeCleaned.iterator();
                                    while (it.hasNext()) {
                                        if (str3.equals(it.next().packageName)) {
                                            it.remove();
                                        }
                                    }
                                    this.mSettings.mKeySetManagerService.addScannedPackageLPw(r22);
                                    int size = r22.providers.size();
                                    ?? sb = str;
                                    int i8 = 0;
                                    while (i8 < size) {
                                        PackageParser.Provider provider2 = (PackageParser.Provider) r22.providers.get(i8);
                                        provider2.info.processName = fixProcessName(r22.applicationInfo.processName, provider2.info.processName);
                                        this.mProviders.addProvider(provider2);
                                        provider2.syncable = provider2.info.isSyncable;
                                        if (provider2.info.authority != null) {
                                            String[] strArrSplit = provider2.info.authority.split(";");
                                            provider2.info.authority = str;
                                            provider = provider2;
                                            for (int i9 = 0; i9 < strArrSplit.length; i9++) {
                                                if (i9 == 1 && provider.syncable) {
                                                    PackageParser.Provider provider3 = new PackageParser.Provider(provider);
                                                    provider3.syncable = false;
                                                    provider = provider3;
                                                }
                                                if (!this.mProvidersByAuthority.containsKey(strArrSplit[i9])) {
                                                    this.mProvidersByAuthority.put(strArrSplit[i9], provider);
                                                    if (provider.info.authority == null) {
                                                        provider.info.authority = strArrSplit[i9];
                                                    } else {
                                                        provider.info.authority = provider.info.authority + ";" + strArrSplit[i9];
                                                    }
                                                    if (DEBUG_PACKAGE_SCANNING && z) {
                                                        Log.d(TAG, "Registered content provider: " + strArrSplit[i9] + ", className = " + provider.info.name + ", isSyncable = " + provider.info.isSyncable);
                                                    }
                                                } else {
                                                    PackageParser.Provider provider4 = this.mProvidersByAuthority.get(strArrSplit[i9]);
                                                    StringBuilder sb2 = new StringBuilder();
                                                    sb2.append("Skipping provider name ");
                                                    sb2.append(strArrSplit[i9]);
                                                    sb2.append(" (in package ");
                                                    sb2.append(r22.applicationInfo.packageName);
                                                    sb2.append("): name already used by ");
                                                    sb2.append((provider4 == null || provider4.getComponentName() == null) ? "?" : provider4.getComponentName().getPackageName());
                                                    Slog.w(TAG, sb2.toString());
                                                }
                                            }
                                            z3 = z;
                                        } else {
                                            z3 = z;
                                            provider = provider2;
                                        }
                                        if (z3) {
                                            if (sb == 0) {
                                                sb = new StringBuilder(256);
                                            } else {
                                                sb.append(' ');
                                                sb = sb;
                                            }
                                            sb.append(provider.info.name);
                                        }
                                        i8++;
                                        str = null;
                                        sb = sb;
                                    }
                                    if (sb != 0 && DEBUG_PACKAGE_SCANNING) {
                                        Log.d(TAG, "  Providers: " + sb);
                                    }
                                    int size2 = r22.services.size();
                                    StringBuilder sb3 = null;
                                    for (int i10 = 0; i10 < size2; i10++) {
                                        PackageParser.Service service = (PackageParser.Service) r22.services.get(i10);
                                        service.info.processName = fixProcessName(r22.applicationInfo.processName, service.info.processName);
                                        this.mServices.addService(service);
                                        if (z) {
                                            if (sb3 == null) {
                                                sb3 = new StringBuilder(256);
                                            } else {
                                                sb3.append(' ');
                                            }
                                            sb3.append(service.info.name);
                                        }
                                    }
                                    if (sb3 != null && DEBUG_PACKAGE_SCANNING) {
                                        Log.d(TAG, "  Services: " + ((Object) sb3));
                                    }
                                    int size3 = r22.receivers.size();
                                    StringBuilder sb4 = null;
                                    for (int i11 = 0; i11 < size3; i11++) {
                                        PackageParser.Activity activity = (PackageParser.Activity) r22.receivers.get(i11);
                                        activity.info.processName = fixProcessName(r22.applicationInfo.processName, activity.info.processName);
                                        this.mReceivers.addActivity(activity, "receiver");
                                        if (z) {
                                            if (sb4 == null) {
                                                sb4 = new StringBuilder(256);
                                            } else {
                                                sb4.append(' ');
                                            }
                                            sb4.append(activity.info.name);
                                        }
                                    }
                                    if (sb4 != null && DEBUG_PACKAGE_SCANNING) {
                                        Log.d(TAG, "  Receivers: " + ((Object) sb4));
                                    }
                                    int size4 = r22.activities.size();
                                    StringBuilder sb5 = null;
                                    for (int i12 = 0; i12 < size4; i12++) {
                                        PackageParser.Activity activity2 = (PackageParser.Activity) r22.activities.get(i12);
                                        activity2.info.processName = fixProcessName(r22.applicationInfo.processName, activity2.info.processName);
                                        this.mActivities.addActivity(activity2, "activity");
                                        if (z) {
                                            if (sb5 == null) {
                                                sb5 = new StringBuilder(256);
                                            } else {
                                                sb5.append(' ');
                                            }
                                            sb5.append(activity2.info.name);
                                        }
                                    }
                                    if (sb5 != null && DEBUG_PACKAGE_SCANNING) {
                                        Log.d(TAG, "  Activities: " + ((Object) sb5));
                                    }
                                    int i13 = i & 16384;
                                    if (i13 == 0) {
                                        this.mPermissionManager.addAllPermissionGroups(r22, z);
                                    } else {
                                        Slog.w(TAG, "Permission groups from package " + r22.packageName + " ignored: instant apps cannot define new permission groups.");
                                    }
                                    if (i13 != 0) {
                                        Slog.w(TAG, "Permissions from package " + r22.packageName + " ignored: instant apps cannot define new permissions.");
                                        listAddAllPermissions = null;
                                    } else {
                                        listAddAllPermissions = this.mPermissionManager.addAllPermissions(r22, z);
                                    }
                                    int size5 = r22.instrumentation.size();
                                    StringBuilder sb6 = null;
                                    for (int i14 = 0; i14 < size5; i14++) {
                                        PackageParser.Instrumentation instrumentation = (PackageParser.Instrumentation) r22.instrumentation.get(i14);
                                        instrumentation.info.packageName = r22.applicationInfo.packageName;
                                        instrumentation.info.sourceDir = r22.applicationInfo.sourceDir;
                                        instrumentation.info.publicSourceDir = r22.applicationInfo.publicSourceDir;
                                        instrumentation.info.splitNames = r22.splitNames;
                                        instrumentation.info.splitSourceDirs = r22.applicationInfo.splitSourceDirs;
                                        instrumentation.info.splitPublicSourceDirs = r22.applicationInfo.splitPublicSourceDirs;
                                        instrumentation.info.splitDependencies = r22.applicationInfo.splitDependencies;
                                        instrumentation.info.dataDir = r22.applicationInfo.dataDir;
                                        instrumentation.info.deviceProtectedDataDir = r22.applicationInfo.deviceProtectedDataDir;
                                        instrumentation.info.credentialProtectedDataDir = r22.applicationInfo.credentialProtectedDataDir;
                                        instrumentation.info.primaryCpuAbi = r22.applicationInfo.primaryCpuAbi;
                                        instrumentation.info.secondaryCpuAbi = r22.applicationInfo.secondaryCpuAbi;
                                        instrumentation.info.nativeLibraryDir = r22.applicationInfo.nativeLibraryDir;
                                        instrumentation.info.secondaryNativeLibraryDir = r22.applicationInfo.secondaryNativeLibraryDir;
                                        this.mInstrumentation.put(instrumentation.getComponentName(), instrumentation);
                                        if (z) {
                                            if (sb6 == null) {
                                                sb6 = new StringBuilder(256);
                                            } else {
                                                sb6.append(' ');
                                            }
                                            sb6.append(instrumentation.info.name);
                                        }
                                    }
                                    if (sb6 != null && DEBUG_PACKAGE_SCANNING) {
                                        Log.d(TAG, "  Instrumentation: " + ((Object) sb6));
                                    }
                                    if (r22.protectedBroadcasts != null) {
                                        int size6 = r22.protectedBroadcasts.size();
                                        synchronized (this.mProtectedBroadcasts) {
                                            for (int i15 = 0; i15 < size6; i15++) {
                                                try {
                                                    this.mProtectedBroadcasts.add((String) r22.protectedBroadcasts.get(i15));
                                                } finally {
                                                }
                                            }
                                        }
                                    }
                                    final boolean z4 = r23 != null;
                                    final boolean z5 = !CollectionUtils.isEmpty(listAddAllPermissions);
                                    if (z4 || z5) {
                                        final ArrayList arrayList = new ArrayList(this.mPackages.keySet());
                                        final List<String> list = listAddAllPermissions;
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public final void run() {
                                                PackageManagerService.lambda$commitPackageSettings$5(this.f$0, z4, r22, r23, arrayList, z5, list);
                                            }
                                        });
                                    }
                                }
                                Trace.traceEnd(262144L);
                                return;
                            }
                            str = null;
                            UpdateAllSharedLibrariesLPw = str;
                            if ((i & 16) == 0) {
                                checkPackageFrozen(str3);
                            }
                            if (UpdateAllSharedLibrariesLPw != 0) {
                            }
                            Trace.traceBegin(262144L, "updateSettings");
                            synchronized (this.mPackages) {
                            }
                        }
                    } else {
                        arrayMap = arrayMap2;
                        i2 = 1;
                    }
                    z2 = false;
                    String str42 = null;
                    if (z2) {
                        str = null;
                        UpdateAllSharedLibrariesLPw = str;
                    }
                    if ((i & 16) == 0) {
                    }
                    if (UpdateAllSharedLibrariesLPw != 0) {
                    }
                    Trace.traceBegin(262144L, "updateSettings");
                    synchronized (this.mPackages) {
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        throw th;
    }

    public static void lambda$commitPackageSettings$5(PackageManagerService packageManagerService, boolean z, PackageParser.Package r3, PackageParser.Package r4, ArrayList arrayList, boolean z2, List list) {
        if (z) {
            packageManagerService.mPermissionManager.revokeRuntimePermissionsIfGroupChanged(r3, r4, arrayList, packageManagerService.mPermissionCallback);
        }
        if (z2) {
            packageManagerService.mPermissionManager.revokeRuntimePermissionsIfPermissionDefinitionChanged(list, arrayList, packageManagerService.mPermissionCallback);
        }
    }

    private static void derivePackageAbi(PackageParser.Package r11, String str, boolean z) throws Throwable {
        AutoCloseable autoCloseableCreate;
        int iFindSupportedAbi;
        int iFindSupportedAbi2;
        int iFindSupportedAbi3;
        setNativeLibraryPaths(r11, sAppLib32InstallDir);
        if (r11.isForwardLocked() || r11.applicationInfo.isExternalAsec() || (isSystemApp(r11) && !r11.isUpdatedSystemApp())) {
            z = false;
        }
        String str2 = r11.applicationInfo.nativeLibraryRootDir;
        boolean z2 = r11.applicationInfo.nativeLibraryRootRequiresIsa;
        AutoCloseable autoCloseable = null;
        try {
            try {
                autoCloseableCreate = NativeLibraryHelper.Handle.create(r11);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            autoCloseableCreate = autoCloseable;
        }
        try {
            File file = new File(str2);
            r11.applicationInfo.primaryCpuAbi = null;
            r11.applicationInfo.secondaryCpuAbi = null;
            int i = -114;
            if (isMultiArch(r11.applicationInfo)) {
                if (r11.cpuAbiOverride != null && !INSTALL_PACKAGE_SUFFIX.equals(r11.cpuAbiOverride)) {
                    Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                }
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    if (z) {
                        Trace.traceBegin(262144L, "copyNativeBinaries");
                        iFindSupportedAbi2 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseableCreate, file, Build.SUPPORTED_32_BIT_ABIS, z2);
                    } else {
                        Trace.traceBegin(262144L, "findSupportedAbi");
                        iFindSupportedAbi2 = NativeLibraryHelper.findSupportedAbi(autoCloseableCreate, Build.SUPPORTED_32_BIT_ABIS);
                    }
                    Trace.traceEnd(262144L);
                } else {
                    iFindSupportedAbi2 = -114;
                }
                if (iFindSupportedAbi2 >= 0 && r11.isLibrary() && z) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                }
                maybeThrowExceptionForMultiArchCopy("Error unpackaging 32 bit native libs for multiarch app.", iFindSupportedAbi2);
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    if (z) {
                        Trace.traceBegin(262144L, "copyNativeBinaries");
                        iFindSupportedAbi3 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseableCreate, file, Build.SUPPORTED_64_BIT_ABIS, z2);
                    } else {
                        Trace.traceBegin(262144L, "findSupportedAbi");
                        iFindSupportedAbi3 = NativeLibraryHelper.findSupportedAbi(autoCloseableCreate, Build.SUPPORTED_64_BIT_ABIS);
                    }
                    i = iFindSupportedAbi3;
                    Trace.traceEnd(262144L);
                }
                maybeThrowExceptionForMultiArchCopy("Error unpackaging 64 bit native libs for multiarch app.", i);
                if (i >= 0) {
                    if (z && r11.isLibrary()) {
                        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                    }
                    r11.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[i];
                }
                if (iFindSupportedAbi2 >= 0) {
                    String str3 = Build.SUPPORTED_32_BIT_ABIS[iFindSupportedAbi2];
                    if (i < 0) {
                        r11.applicationInfo.primaryCpuAbi = str3;
                    } else if (r11.use32bitAbi) {
                        r11.applicationInfo.secondaryCpuAbi = r11.applicationInfo.primaryCpuAbi;
                        r11.applicationInfo.primaryCpuAbi = str3;
                    } else {
                        r11.applicationInfo.secondaryCpuAbi = str3;
                    }
                }
            } else {
                boolean z3 = true;
                String[] strArr = str != null ? new String[]{str} : Build.SUPPORTED_ABIS;
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && str == null && NativeLibraryHelper.hasRenderscriptBitcode(autoCloseableCreate)) {
                    strArr = Build.SUPPORTED_32_BIT_ABIS;
                } else {
                    z3 = false;
                }
                if (z) {
                    Trace.traceBegin(262144L, "copyNativeBinaries");
                    iFindSupportedAbi = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseableCreate, file, strArr, z2);
                } else {
                    Trace.traceBegin(262144L, "findSupportedAbi");
                    iFindSupportedAbi = NativeLibraryHelper.findSupportedAbi(autoCloseableCreate, strArr);
                }
                Trace.traceEnd(262144L);
                if (iFindSupportedAbi < 0 && iFindSupportedAbi != -114) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Error unpackaging native libs for app, errorCode=" + iFindSupportedAbi);
                }
                if (iFindSupportedAbi >= 0) {
                    if (r11.isLibrary()) {
                        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library with native libs must be multiarch");
                    }
                    r11.applicationInfo.primaryCpuAbi = strArr[iFindSupportedAbi];
                } else if (iFindSupportedAbi == -114 && str != null) {
                    r11.applicationInfo.primaryCpuAbi = str;
                } else if (z3) {
                    r11.applicationInfo.primaryCpuAbi = strArr[0];
                }
            }
            IoUtils.closeQuietly(autoCloseableCreate);
        } catch (IOException e2) {
            e = e2;
            autoCloseable = autoCloseableCreate;
            Slog.e(TAG, "Unable to get canonical file " + e.toString());
            IoUtils.closeQuietly(autoCloseable);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(autoCloseableCreate);
            throw th;
        }
        setNativeLibraryPaths(r11, sAppLib32InstallDir);
    }

    private static List<String> adjustCpuAbisForSharedUserLPw(Set<PackageSetting> set, PackageParser.Package r9) {
        String instructionSet;
        String str;
        ArrayList arrayList = null;
        if (r9 != null && r9.applicationInfo.primaryCpuAbi != null) {
            instructionSet = VMRuntime.getInstructionSet(r9.applicationInfo.primaryCpuAbi);
        } else {
            instructionSet = null;
        }
        PackageSetting packageSetting = null;
        for (PackageSetting packageSetting2 : set) {
            if (r9 == null || !r9.packageName.equals(packageSetting2.name)) {
                if (packageSetting2.primaryCpuAbiString != null) {
                    String instructionSet2 = VMRuntime.getInstructionSet(packageSetting2.primaryCpuAbiString);
                    if (instructionSet != null && !instructionSet2.equals(instructionSet)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Instruction set mismatch, ");
                        sb.append(packageSetting == null ? "[caller]" : packageSetting);
                        sb.append(" requires ");
                        sb.append(instructionSet);
                        sb.append(" whereas ");
                        sb.append(packageSetting2);
                        sb.append(" requires ");
                        sb.append(instructionSet2);
                        Slog.w(TAG, sb.toString());
                    }
                    if (instructionSet == null) {
                        packageSetting = packageSetting2;
                        instructionSet = instructionSet2;
                    }
                }
            }
        }
        if (instructionSet != null) {
            if (packageSetting != null) {
                str = packageSetting.primaryCpuAbiString;
                if (r9 != null) {
                    r9.applicationInfo.primaryCpuAbi = str;
                }
            } else {
                str = r9.applicationInfo.primaryCpuAbi;
            }
            for (PackageSetting packageSetting3 : set) {
                if (r9 == null || !r9.packageName.equals(packageSetting3.name)) {
                    if (packageSetting3.primaryCpuAbiString == null) {
                        packageSetting3.primaryCpuAbiString = str;
                        if (packageSetting3.pkg != null && packageSetting3.pkg.applicationInfo != null && !TextUtils.equals(str, packageSetting3.pkg.applicationInfo.primaryCpuAbi)) {
                            packageSetting3.pkg.applicationInfo.primaryCpuAbi = str;
                            if (DEBUG_ABI_SELECTION) {
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("Adjusting ABI for ");
                                sb2.append(packageSetting3.name);
                                sb2.append(" to ");
                                sb2.append(str);
                                sb2.append(" (requirer=");
                                sb2.append(packageSetting != null ? packageSetting.pkg : "null");
                                sb2.append(", scannedPackage=");
                                sb2.append(r9 != null ? r9 : "null");
                                sb2.append(")");
                                Slog.i(TAG, sb2.toString());
                            }
                            if (arrayList == null) {
                                arrayList = new ArrayList();
                            }
                            arrayList.add(packageSetting3.codePathString);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    private void setUpCustomResolverActivity(PackageParser.Package r5) {
        synchronized (this.mPackages) {
            this.mResolverReplaced = true;
            this.mResolveActivity.applicationInfo = r5.applicationInfo;
            this.mResolveActivity.name = this.mCustomResolverComponentName.getClassName();
            this.mResolveActivity.packageName = r5.applicationInfo.packageName;
            this.mResolveActivity.processName = r5.applicationInfo.packageName;
            this.mResolveActivity.launchMode = 0;
            this.mResolveActivity.flags = 288;
            this.mResolveActivity.theme = 0;
            this.mResolveActivity.exported = true;
            this.mResolveActivity.enabled = true;
            this.mResolveInfo.activityInfo = this.mResolveActivity;
            this.mResolveInfo.priority = 0;
            this.mResolveInfo.preferredOrder = 0;
            this.mResolveInfo.match = 0;
            this.mResolveComponentName = this.mCustomResolverComponentName;
            Slog.i(TAG, "Replacing default ResolverActivity with custom activity: " + this.mResolveComponentName);
        }
    }

    private void setUpInstantAppInstallerActivityLP(ActivityInfo activityInfo) {
        if (activityInfo == null) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Clear ephemeral installer activity");
            }
            this.mInstantAppInstallerActivity = null;
            return;
        }
        if (DEBUG_INSTANT) {
            Slog.d(TAG, "Set ephemeral installer activity: " + activityInfo.getComponentName());
        }
        this.mInstantAppInstallerActivity = activityInfo;
        this.mInstantAppInstallerActivity.flags |= 288;
        this.mInstantAppInstallerActivity.exported = true;
        this.mInstantAppInstallerActivity.enabled = true;
        this.mInstantAppInstallerInfo.activityInfo = this.mInstantAppInstallerActivity;
        this.mInstantAppInstallerInfo.priority = 1;
        this.mInstantAppInstallerInfo.preferredOrder = 1;
        this.mInstantAppInstallerInfo.isDefault = true;
        this.mInstantAppInstallerInfo.match = 5799936;
    }

    private static String calculateBundledApkRoot(String str) {
        File canonicalFile;
        File file = new File(str);
        if (FileUtils.contains(Environment.getRootDirectory(), file)) {
            canonicalFile = Environment.getRootDirectory();
        } else if (FileUtils.contains(Environment.getOemDirectory(), file)) {
            canonicalFile = Environment.getOemDirectory();
        } else if (FileUtils.contains(Environment.getVendorDirectory(), file)) {
            canonicalFile = Environment.getVendorDirectory();
        } else if (FileUtils.contains(Environment.getOdmDirectory(), file)) {
            canonicalFile = Environment.getOdmDirectory();
        } else if (FileUtils.contains(Environment.getProductDirectory(), file)) {
            canonicalFile = Environment.getProductDirectory();
        } else {
            try {
                canonicalFile = file.getCanonicalFile();
                File parentFile = canonicalFile.getParentFile();
                while (true) {
                    File parentFile2 = parentFile.getParentFile();
                    if (parentFile2 == null) {
                        break;
                    }
                    canonicalFile = parentFile;
                    parentFile = parentFile2;
                }
                Slog.w(TAG, "Unrecognized code path " + file + " - using " + canonicalFile);
            } catch (IOException e) {
                Slog.w(TAG, "Can't canonicalize code path " + file);
                return Environment.getRootDirectory().getPath();
            }
        }
        return canonicalFile.getPath();
    }

    private static void setNativeLibraryPaths(PackageParser.Package r8, File file) {
        ApplicationInfo applicationInfo = r8.applicationInfo;
        String str = r8.codePath;
        File file2 = new File(str);
        boolean z = applicationInfo.isSystemApp() && !applicationInfo.isUpdatedSystemApp();
        boolean z2 = applicationInfo.isForwardLocked() || applicationInfo.isExternalAsec();
        applicationInfo.nativeLibraryRootDir = null;
        applicationInfo.nativeLibraryRootRequiresIsa = false;
        applicationInfo.nativeLibraryDir = null;
        applicationInfo.secondaryNativeLibraryDir = null;
        if (PackageParser.isApkFile(file2)) {
            if (z) {
                String strCalculateBundledApkRoot = calculateBundledApkRoot(applicationInfo.sourceDir);
                boolean zIs64BitInstructionSet = VMRuntime.is64BitInstructionSet(InstructionSets.getPrimaryInstructionSet(applicationInfo));
                String strDeriveCodePathName = deriveCodePathName(str);
                applicationInfo.nativeLibraryRootDir = Environment.buildPath(new File(strCalculateBundledApkRoot), new String[]{zIs64BitInstructionSet ? "lib64" : "lib", strDeriveCodePathName}).getAbsolutePath();
                if (applicationInfo.secondaryCpuAbi != null) {
                    applicationInfo.secondaryNativeLibraryDir = Environment.buildPath(new File(strCalculateBundledApkRoot), new String[]{zIs64BitInstructionSet ? "lib" : "lib64", strDeriveCodePathName}).getAbsolutePath();
                }
            } else if (z2) {
                applicationInfo.nativeLibraryRootDir = new File(file2.getParentFile(), "lib").getAbsolutePath();
            } else {
                applicationInfo.nativeLibraryRootDir = new File(file, deriveCodePathName(str)).getAbsolutePath();
            }
            applicationInfo.nativeLibraryRootRequiresIsa = false;
            applicationInfo.nativeLibraryDir = applicationInfo.nativeLibraryRootDir;
            return;
        }
        if (!sPmsExt.updateNativeLibDir(applicationInfo, str)) {
            applicationInfo.nativeLibraryRootDir = new File(file2, "lib").getAbsolutePath();
            applicationInfo.nativeLibraryRootRequiresIsa = true;
            applicationInfo.nativeLibraryDir = new File(applicationInfo.nativeLibraryRootDir, InstructionSets.getPrimaryInstructionSet(applicationInfo)).getAbsolutePath();
            if (applicationInfo.secondaryCpuAbi != null) {
                applicationInfo.secondaryNativeLibraryDir = new File(applicationInfo.nativeLibraryRootDir, VMRuntime.getInstructionSet(applicationInfo.secondaryCpuAbi)).getAbsolutePath();
            }
        }
    }

    private static void setBundledAppAbisAndRoots(PackageParser.Package r2, PackageSetting packageSetting) {
        setBundledAppAbi(r2, calculateBundledApkRoot(r2.applicationInfo.sourceDir), deriveCodePathName(r2.applicationInfo.getCodePath()));
        if (packageSetting != null) {
            packageSetting.primaryCpuAbiString = r2.applicationInfo.primaryCpuAbi;
            packageSetting.secondaryCpuAbiString = r2.applicationInfo.secondaryCpuAbi;
        }
    }

    private static void setBundledAppAbi(PackageParser.Package r5, String str, String str2) {
        boolean zExists;
        boolean zExists2;
        File file = new File(r5.codePath);
        if (PackageParser.isApkFile(file)) {
            zExists = new File(str, new File("lib64", str2).getPath()).exists();
            zExists2 = new File(str, new File("lib", str2).getPath()).exists();
        } else {
            File file2 = new File(file, "lib");
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS[0])) {
                zExists = new File(file2, VMRuntime.getInstructionSet(Build.SUPPORTED_64_BIT_ABIS[0])).exists();
            } else {
                zExists = false;
            }
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS[0])) {
                zExists2 = new File(file2, VMRuntime.getInstructionSet(Build.SUPPORTED_32_BIT_ABIS[0])).exists();
            } else {
                zExists2 = false;
            }
        }
        if (zExists && !zExists2) {
            r5.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            r5.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (zExists2 && !zExists) {
            r5.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            r5.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (zExists2 && zExists) {
            if ((r5.applicationInfo.flags & Integer.MIN_VALUE) == 0) {
                Slog.e(TAG, "Package " + r5 + " has multiple bundled libs, but is not multiarch.");
            }
            if (VMRuntime.is64BitInstructionSet(InstructionSets.getPreferredInstructionSet())) {
                r5.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
                r5.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
                return;
            }
            r5.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            r5.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            return;
        }
        r5.applicationInfo.primaryCpuAbi = null;
        r5.applicationInfo.secondaryCpuAbi = null;
    }

    private void killApplication(String str, int i, String str2) {
        killApplication(str, i, -1, str2);
    }

    private void killApplication(String str, int i, int i2, String str2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            IActivityManager service = ActivityManager.getService();
            if (service != null) {
                try {
                    service.killApplication(str, i, i2, str2);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void removePackageLI(PackageParser.Package r4, boolean z) {
        PackageSetting packageSetting = (PackageSetting) r4.mExtras;
        if (packageSetting != null) {
            removePackageLI(packageSetting, z);
        }
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            PackageSetting packageSetting2 = (PackageSetting) ((PackageParser.Package) r4.childPackages.get(i)).mExtras;
            if (packageSetting2 != null) {
                removePackageLI(packageSetting2, z);
            }
        }
    }

    void removePackageLI(PackageSetting packageSetting, boolean z) {
        if (DEBUG_INSTALL && z) {
            Log.d(TAG, "Removing package " + packageSetting.name);
        }
        synchronized (this.mPackages) {
            this.mPackages.remove(packageSetting.name);
            PackageParser.Package r4 = packageSetting.pkg;
            if (r4 != null) {
                cleanPackageDataStructuresLILPw(r4, z);
            }
        }
    }

    void removeInstalledPackageLI(PackageParser.Package r7, boolean z) {
        if (DEBUG_INSTALL && z) {
            Log.d(TAG, "Removing package " + r7.applicationInfo.packageName);
        }
        synchronized (this.mPackages) {
            this.mPackages.remove(r7.applicationInfo.packageName);
            cleanPackageDataStructuresLILPw(r7, z);
            int size = r7.childPackages != null ? r7.childPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                PackageParser.Package r3 = (PackageParser.Package) r7.childPackages.get(i);
                this.mPackages.remove(r3.applicationInfo.packageName);
                cleanPackageDataStructuresLILPw(r3, z);
            }
        }
    }

    void cleanPackageDataStructuresLILPw(PackageParser.Package r14, boolean z) {
        int size = r14.providers.size();
        StringBuilder sb = null;
        StringBuilder sb2 = null;
        for (int i = 0; i < size; i++) {
            PackageParser.Provider provider = (PackageParser.Provider) r14.providers.get(i);
            this.mProviders.removeProvider(provider);
            if (provider.info.authority != null) {
                String[] strArrSplit = provider.info.authority.split(";");
                for (int i2 = 0; i2 < strArrSplit.length; i2++) {
                    if (this.mProvidersByAuthority.get(strArrSplit[i2]) == provider) {
                        this.mProvidersByAuthority.remove(strArrSplit[i2]);
                        if (DEBUG_REMOVE && z) {
                            Log.d(TAG, "Unregistered content provider: " + strArrSplit[i2] + ", className = " + provider.info.name + ", isSyncable = " + provider.info.isSyncable);
                        }
                    }
                }
                if (DEBUG_REMOVE && z) {
                    if (sb2 == null) {
                        sb2 = new StringBuilder(256);
                    } else {
                        sb2.append(' ');
                    }
                    sb2.append(provider.info.name);
                }
            }
        }
        if (sb2 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Providers: " + ((Object) sb2));
        }
        int size2 = r14.services.size();
        StringBuilder sb3 = null;
        for (int i3 = 0; i3 < size2; i3++) {
            PackageParser.Service service = (PackageParser.Service) r14.services.get(i3);
            this.mServices.removeService(service);
            if (z) {
                if (sb3 == null) {
                    sb3 = new StringBuilder(256);
                } else {
                    sb3.append(' ');
                }
                sb3.append(service.info.name);
            }
        }
        if (sb3 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Services: " + ((Object) sb3));
        }
        int size3 = r14.receivers.size();
        StringBuilder sb4 = null;
        for (int i4 = 0; i4 < size3; i4++) {
            PackageParser.Activity activity = (PackageParser.Activity) r14.receivers.get(i4);
            this.mReceivers.removeActivity(activity, "receiver");
            if (DEBUG_REMOVE && z) {
                if (sb4 == null) {
                    sb4 = new StringBuilder(256);
                } else {
                    sb4.append(' ');
                }
                sb4.append(activity.info.name);
            }
        }
        if (sb4 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Receivers: " + ((Object) sb4));
        }
        int size4 = r14.activities.size();
        StringBuilder sb5 = null;
        for (int i5 = 0; i5 < size4; i5++) {
            PackageParser.Activity activity2 = (PackageParser.Activity) r14.activities.get(i5);
            this.mActivities.removeActivity(activity2, "activity");
            if (DEBUG_REMOVE && z) {
                if (sb5 == null) {
                    sb5 = new StringBuilder(256);
                } else {
                    sb5.append(' ');
                }
                sb5.append(activity2.info.name);
            }
        }
        if (sb5 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Activities: " + ((Object) sb5));
        }
        this.mPermissionManager.removeAllPermissions(r14, z);
        int size5 = r14.instrumentation.size();
        StringBuilder sb6 = null;
        for (int i6 = 0; i6 < size5; i6++) {
            PackageParser.Instrumentation instrumentation = (PackageParser.Instrumentation) r14.instrumentation.get(i6);
            this.mInstrumentation.remove(instrumentation.getComponentName());
            if (DEBUG_REMOVE && z) {
                if (sb6 == null) {
                    sb6 = new StringBuilder(256);
                } else {
                    sb6.append(' ');
                }
                sb6.append(instrumentation.info.name);
            }
        }
        if (sb6 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Instrumentation: " + ((Object) sb6));
        }
        if ((r14.applicationInfo.flags & 1) != 0 && r14.libraryNames != null) {
            StringBuilder sb7 = null;
            for (int i7 = 0; i7 < r14.libraryNames.size(); i7++) {
                String str = (String) r14.libraryNames.get(i7);
                if (removeSharedLibraryLPw(str, 0L) && DEBUG_REMOVE && z) {
                    if (sb7 == null) {
                        sb7 = new StringBuilder(256);
                    } else {
                        sb7.append(' ');
                    }
                    sb7.append(str);
                }
            }
        }
        if (r14.staticSharedLibName != null && removeSharedLibraryLPw(r14.staticSharedLibName, r14.staticSharedLibVersion) && DEBUG_REMOVE && z) {
            sb = new StringBuilder(256);
            sb.append(r14.staticSharedLibName);
        }
        if (sb == null || !DEBUG_REMOVE) {
            return;
        }
        Log.d(TAG, "  Libraries: " + ((Object) sb));
    }

    final class ActivityIntentResolver extends IntentResolver<PackageParser.ActivityIntentInfo, ResolveInfo> {
        private final ArrayMap<ComponentName, PackageParser.Activity> mActivities = new ArrayMap<>();
        private int mFlags;

        ActivityIntentResolver() {
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String str, boolean z, int i) {
            if (!PackageManagerService.sUserManager.exists(i)) {
                return null;
            }
            this.mFlags = z ? 65536 : 0;
            return super.queryIntent(intent, str, z, i);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String str, int i, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2)) {
                return null;
            }
            this.mFlags = i;
            return super.queryIntent(intent, str, (i & 65536) != 0, i2);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String str, int i, ArrayList<PackageParser.Activity> arrayList, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2) || arrayList == null) {
                return null;
            }
            this.mFlags = i;
            boolean z = (i & 65536) != 0;
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i3 = 0; i3 < size; i3++) {
                ArrayList arrayList3 = arrayList.get(i3).intents;
                if (arrayList3 != null && arrayList3.size() > 0) {
                    PackageParser.ActivityIntentInfo[] activityIntentInfoArr = new PackageParser.ActivityIntentInfo[arrayList3.size()];
                    arrayList3.toArray(activityIntentInfoArr);
                    arrayList2.add(activityIntentInfoArr);
                }
            }
            return super.queryIntentFromList(intent, str, z, arrayList2, i2);
        }

        private PackageParser.Activity findMatchingActivity(List<PackageParser.Activity> list, ActivityInfo activityInfo) {
            for (PackageParser.Activity activity : list) {
                if (activity.info.name.equals(activityInfo.name)) {
                    return activity;
                }
                if (activity.info.name.equals(activityInfo.targetActivity)) {
                    return activity;
                }
                if (activity.info.targetActivity != null) {
                    if (activity.info.targetActivity.equals(activityInfo.name)) {
                        return activity;
                    }
                    if (activity.info.targetActivity.equals(activityInfo.targetActivity)) {
                        return activity;
                    }
                }
            }
            return null;
        }

        public class IterGenerator<E> {
            public IterGenerator() {
            }

            public Iterator<E> generate(PackageParser.ActivityIntentInfo activityIntentInfo) {
                return null;
            }
        }

        public class ActionIterGenerator extends IterGenerator<String> {
            public ActionIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo activityIntentInfo) {
                return activityIntentInfo.actionsIterator();
            }
        }

        public class CategoriesIterGenerator extends IterGenerator<String> {
            public CategoriesIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo activityIntentInfo) {
                return activityIntentInfo.categoriesIterator();
            }
        }

        public class SchemesIterGenerator extends IterGenerator<String> {
            public SchemesIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo activityIntentInfo) {
                return activityIntentInfo.schemesIterator();
            }
        }

        public class AuthoritiesIterGenerator extends IterGenerator<IntentFilter.AuthorityEntry> {
            public AuthoritiesIterGenerator() {
                super();
            }

            @Override
            public Iterator<IntentFilter.AuthorityEntry> generate(PackageParser.ActivityIntentInfo activityIntentInfo) {
                return activityIntentInfo.authoritiesIterator();
            }
        }

        private <T> void getIntentListSubset(List<PackageParser.ActivityIntentInfo> list, IterGenerator<T> iterGenerator, Iterator<T> it) {
            while (it.hasNext() && list.size() != 0) {
                T next = it.next();
                Iterator<PackageParser.ActivityIntentInfo> it2 = list.iterator();
                while (it2.hasNext()) {
                    boolean z = false;
                    Iterator<T> itGenerate = iterGenerator.generate(it2.next());
                    while (true) {
                        if (itGenerate != null && itGenerate.hasNext()) {
                            T next2 = itGenerate.next();
                            if (next2 != null && next2.equals(next)) {
                                z = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!z) {
                        it2.remove();
                    }
                }
            }
        }

        private boolean isProtectedAction(PackageParser.ActivityIntentInfo activityIntentInfo) {
            Iterator itActionsIterator = activityIntentInfo.actionsIterator();
            while (itActionsIterator != null && itActionsIterator.hasNext()) {
                if (PackageManagerService.PROTECTED_ACTIONS.contains((String) itActionsIterator.next())) {
                    return true;
                }
            }
            return false;
        }

        private void adjustPriority(List<PackageParser.Activity> list, PackageParser.ActivityIntentInfo activityIntentInfo) {
            if (activityIntentInfo.getPriority() <= 0) {
                return;
            }
            ActivityInfo activityInfo = activityIntentInfo.activity.info;
            ApplicationInfo applicationInfo = activityInfo.applicationInfo;
            int iMax = 0;
            if (!((applicationInfo.privateFlags & 8) != 0)) {
                if (PackageManagerService.DEBUG_FILTERS) {
                    Slog.i(PackageManagerService.TAG, "Non-privileged app; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                }
                activityIntentInfo.setPriority(0);
                return;
            }
            if (list == null) {
                if (isProtectedAction(activityIntentInfo)) {
                    if (PackageManagerService.this.mDeferProtectedFilters) {
                        PackageManagerService.this.mProtectedFilters.add(activityIntentInfo);
                        if (PackageManagerService.DEBUG_FILTERS) {
                            Slog.i(PackageManagerService.TAG, "Protected action; save for later; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                            return;
                        }
                        return;
                    }
                    if (PackageManagerService.DEBUG_FILTERS && PackageManagerService.this.mSetupWizardPackage == null) {
                        Slog.i(PackageManagerService.TAG, "No setup wizard; All protected intents capped to priority 0");
                    }
                    if (activityIntentInfo.activity.info.packageName.equals(PackageManagerService.this.mSetupWizardPackage)) {
                        if (PackageManagerService.DEBUG_FILTERS) {
                            Slog.i(PackageManagerService.TAG, "Found setup wizard; allow priority " + activityIntentInfo.getPriority() + "; package: " + activityIntentInfo.activity.info.packageName + " activity: " + activityIntentInfo.activity.className + " priority: " + activityIntentInfo.getPriority());
                            return;
                        }
                        return;
                    }
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Protected action; cap priority to 0; package: " + activityIntentInfo.activity.info.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                    }
                    activityIntentInfo.setPriority(0);
                    return;
                }
                return;
            }
            PackageParser.Activity activityFindMatchingActivity = findMatchingActivity(list, activityInfo);
            if (activityFindMatchingActivity == null) {
                if (PackageManagerService.DEBUG_FILTERS) {
                    Slog.i(PackageManagerService.TAG, "New activity; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                }
                activityIntentInfo.setPriority(0);
                return;
            }
            ArrayList arrayList = new ArrayList(activityFindMatchingActivity.intents);
            findFilters(activityIntentInfo);
            Iterator itActionsIterator = activityIntentInfo.actionsIterator();
            if (itActionsIterator != null) {
                getIntentListSubset(arrayList, new ActionIterGenerator(), itActionsIterator);
                if (arrayList.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched action; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                    }
                    activityIntentInfo.setPriority(0);
                    return;
                }
            }
            Iterator itCategoriesIterator = activityIntentInfo.categoriesIterator();
            if (itCategoriesIterator != null) {
                getIntentListSubset(arrayList, new CategoriesIterGenerator(), itCategoriesIterator);
                if (arrayList.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched category; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                    }
                    activityIntentInfo.setPriority(0);
                    return;
                }
            }
            Iterator itSchemesIterator = activityIntentInfo.schemesIterator();
            if (itSchemesIterator != null) {
                getIntentListSubset(arrayList, new SchemesIterGenerator(), itSchemesIterator);
                if (arrayList.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched scheme; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                    }
                    activityIntentInfo.setPriority(0);
                    return;
                }
            }
            Iterator itAuthoritiesIterator = activityIntentInfo.authoritiesIterator();
            if (itAuthoritiesIterator != null) {
                getIntentListSubset(arrayList, new AuthoritiesIterGenerator(), itAuthoritiesIterator);
                if (arrayList.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched authority; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                    }
                    activityIntentInfo.setPriority(0);
                    return;
                }
            }
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                iMax = Math.max(iMax, arrayList.get(size).getPriority());
            }
            if (activityIntentInfo.getPriority() > iMax) {
                if (PackageManagerService.DEBUG_FILTERS) {
                    Slog.i(PackageManagerService.TAG, "Found matching filter(s); cap priority to " + iMax + "; package: " + applicationInfo.packageName + " activity: " + activityIntentInfo.activity.className + " origPrio: " + activityIntentInfo.getPriority());
                }
                activityIntentInfo.setPriority(iMax);
            }
        }

        public final void addActivity(PackageParser.Activity activity, String str) {
            this.mActivities.put(activity.getComponentName(), activity);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(str);
                sb.append(" ");
                sb.append(activity.info.nonLocalizedLabel != null ? activity.info.nonLocalizedLabel : activity.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
            }
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "    Class=" + activity.info.name);
            }
            int size = activity.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) activity.intents.get(i);
                if ("activity".equals(str)) {
                    PackageSetting disabledSystemPkgLPr = PackageManagerService.this.mSettings.getDisabledSystemPkgLPr(activityIntentInfo.activity.info.packageName);
                    adjustPriority((disabledSystemPkgLPr == null || disabledSystemPkgLPr.pkg == null) ? null : disabledSystemPkgLPr.pkg.activities, activityIntentInfo);
                }
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    activityIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!activityIntentInfo.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Activity " + activity.info.name);
                }
                addFilter(activityIntentInfo);
            }
        }

        public final void removeActivity(PackageParser.Activity activity, String str) {
            this.mActivities.remove(activity.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(str);
                sb.append(" ");
                sb.append(activity.info.nonLocalizedLabel != null ? activity.info.nonLocalizedLabel : activity.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
                Log.v(PackageManagerService.TAG, "    Class=" + activity.info.name);
            }
            int size = activity.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) activity.intents.get(i);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    activityIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(activityIntentInfo);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ActivityIntentInfo activityIntentInfo, List<ResolveInfo> list) {
            ActivityInfo activityInfo = activityIntentInfo.activity.info;
            for (int size = list.size() - 1; size >= 0; size--) {
                ActivityInfo activityInfo2 = list.get(size).activityInfo;
                if (activityInfo2.name == activityInfo.name && activityInfo2.packageName == activityInfo.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ActivityIntentInfo[] newArray(int i) {
            return new PackageParser.ActivityIntentInfo[i];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ActivityIntentInfo activityIntentInfo, int i) {
            PackageSetting packageSetting;
            if (!PackageManagerService.sUserManager.exists(i)) {
                return true;
            }
            PackageParser.Package r4 = activityIntentInfo.activity.owner;
            return r4 != null && (packageSetting = (PackageSetting) r4.mExtras) != null && (packageSetting.pkgFlags & 1) == 0 && packageSetting.getStopped(i);
        }

        @Override
        protected boolean isPackageForFilter(String str, PackageParser.ActivityIntentInfo activityIntentInfo) {
            return str.equals(activityIntentInfo.activity.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ActivityIntentInfo activityIntentInfo, int i, int i2) {
            PackageUserState userState;
            ActivityInfo activityInfoGenerateActivityInfo;
            if (!PackageManagerService.sUserManager.exists(i2) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(activityIntentInfo.activity.info, this.mFlags, i2)) {
                return null;
            }
            PackageParser.Activity activity = activityIntentInfo.activity;
            PackageSetting packageSetting = (PackageSetting) activity.owner.mExtras;
            if (packageSetting == null || (activityInfoGenerateActivityInfo = PackageParser.generateActivityInfo(activity, this.mFlags, (userState = packageSetting.readUserState(i2)), i2)) == null) {
                return null;
            }
            boolean z = (this.mFlags & 33554432) != 0;
            boolean z2 = (this.mFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
            boolean z3 = z2 && activityIntentInfo.isVisibleToInstantApp() && (!z || activityIntentInfo.isExplicitlyVisibleToInstantApp());
            boolean z4 = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (z2 && !z3 && !userState.instantApp) {
                return null;
            }
            if (!z4 && userState.instantApp) {
                return null;
            }
            if (userState.instantApp && packageSetting.isUpdateAvailable()) {
                return null;
            }
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfoGenerateActivityInfo;
            if ((this.mFlags & 64) != 0) {
                resolveInfo.filter = activityIntentInfo;
            }
            if (activityIntentInfo != null) {
                resolveInfo.handleAllWebDataURI = activityIntentInfo.handleAllWebDataURI();
            }
            resolveInfo.priority = activityIntentInfo.getPriority();
            resolveInfo.preferredOrder = activity.owner.mPreferredOrder;
            resolveInfo.match = i;
            resolveInfo.isDefault = activityIntentInfo.hasDefault;
            resolveInfo.labelRes = activityIntentInfo.labelRes;
            resolveInfo.nonLocalizedLabel = activityIntentInfo.nonLocalizedLabel;
            if (PackageManagerService.this.userNeedsBadging(i2)) {
                resolveInfo.noResourceId = true;
            } else {
                resolveInfo.icon = activityIntentInfo.icon;
            }
            resolveInfo.iconResourceId = activityIntentInfo.icon;
            resolveInfo.system = resolveInfo.activityInfo.applicationInfo.isSystemApp();
            resolveInfo.isInstantAppAvailable = userState.instantApp;
            return resolveInfo;
        }

        @Override
        protected void sortResults(List<ResolveInfo> list) {
            Collections.sort(list, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter printWriter, String str, PackageParser.ActivityIntentInfo activityIntentInfo) {
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(activityIntentInfo.activity)));
            printWriter.print(' ');
            activityIntentInfo.activity.printComponentShortName(printWriter);
            printWriter.print(" filter ");
            printWriter.println(Integer.toHexString(System.identityHashCode(activityIntentInfo)));
        }

        @Override
        protected Object filterToLabel(PackageParser.ActivityIntentInfo activityIntentInfo) {
            return activityIntentInfo.activity;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter printWriter, String str, Object obj, int i) {
            PackageParser.Activity activity = (PackageParser.Activity) obj;
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(activity)));
            printWriter.print(' ');
            activity.printComponentShortName(printWriter);
            if (i > 1) {
                printWriter.print(" (");
                printWriter.print(i);
                printWriter.print(" filters)");
            }
            printWriter.println();
        }
    }

    private final class ServiceIntentResolver extends IntentResolver<PackageParser.ServiceIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, PackageParser.Service> mServices;

        private ServiceIntentResolver() {
            this.mServices = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String str, boolean z, int i) {
            this.mFlags = z ? 65536 : 0;
            return super.queryIntent(intent, str, z, i);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String str, int i, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2)) {
                return null;
            }
            this.mFlags = i;
            return super.queryIntent(intent, str, (i & 65536) != 0, i2);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String str, int i, ArrayList<PackageParser.Service> arrayList, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2) || arrayList == null) {
                return null;
            }
            this.mFlags = i;
            boolean z = (i & 65536) != 0;
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i3 = 0; i3 < size; i3++) {
                ArrayList arrayList3 = arrayList.get(i3).intents;
                if (arrayList3 != null && arrayList3.size() > 0) {
                    PackageParser.ServiceIntentInfo[] serviceIntentInfoArr = new PackageParser.ServiceIntentInfo[arrayList3.size()];
                    arrayList3.toArray(serviceIntentInfoArr);
                    arrayList2.add(serviceIntentInfoArr);
                }
            }
            return super.queryIntentFromList(intent, str, z, arrayList2, i2);
        }

        public final void addService(PackageParser.Service service) {
            this.mServices.put(service.getComponentName(), service);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(service.info.nonLocalizedLabel != null ? service.info.nonLocalizedLabel : service.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
                Log.v(PackageManagerService.TAG, "    Class=" + service.info.name);
            }
            int size = service.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ServiceIntentInfo serviceIntentInfo = (PackageParser.ServiceIntentInfo) service.intents.get(i);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    serviceIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!serviceIntentInfo.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Service " + service.info.name);
                }
                addFilter(serviceIntentInfo);
            }
        }

        public final void removeService(PackageParser.Service service) {
            this.mServices.remove(service.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(service.info.nonLocalizedLabel != null ? service.info.nonLocalizedLabel : service.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
                Log.v(PackageManagerService.TAG, "    Class=" + service.info.name);
            }
            int size = service.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ServiceIntentInfo serviceIntentInfo = (PackageParser.ServiceIntentInfo) service.intents.get(i);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    serviceIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(serviceIntentInfo);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ServiceIntentInfo serviceIntentInfo, List<ResolveInfo> list) {
            ServiceInfo serviceInfo = serviceIntentInfo.service.info;
            for (int size = list.size() - 1; size >= 0; size--) {
                ServiceInfo serviceInfo2 = list.get(size).serviceInfo;
                if (serviceInfo2.name == serviceInfo.name && serviceInfo2.packageName == serviceInfo.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ServiceIntentInfo[] newArray(int i) {
            return new PackageParser.ServiceIntentInfo[i];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ServiceIntentInfo serviceIntentInfo, int i) {
            PackageSetting packageSetting;
            if (!PackageManagerService.sUserManager.exists(i)) {
                return true;
            }
            PackageParser.Package r4 = serviceIntentInfo.service.owner;
            return r4 != null && (packageSetting = (PackageSetting) r4.mExtras) != null && (packageSetting.pkgFlags & 1) == 0 && packageSetting.getStopped(i);
        }

        @Override
        protected boolean isPackageForFilter(String str, PackageParser.ServiceIntentInfo serviceIntentInfo) {
            return str.equals(serviceIntentInfo.service.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ServiceIntentInfo serviceIntentInfo, int i, int i2) {
            PackageUserState userState;
            ServiceInfo serviceInfoGenerateServiceInfo;
            if (!PackageManagerService.sUserManager.exists(i2) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(serviceIntentInfo.service.info, this.mFlags, i2)) {
                return null;
            }
            PackageParser.Service service = serviceIntentInfo.service;
            PackageSetting packageSetting = (PackageSetting) service.owner.mExtras;
            if (packageSetting == null || (serviceInfoGenerateServiceInfo = PackageParser.generateServiceInfo(service, this.mFlags, (userState = packageSetting.readUserState(i2)), i2)) == null) {
                return null;
            }
            boolean z = (this.mFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
            boolean z2 = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (z && !serviceIntentInfo.isVisibleToInstantApp() && !userState.instantApp) {
                return null;
            }
            if (!z2 && userState.instantApp) {
                return null;
            }
            if (userState.instantApp && packageSetting.isUpdateAvailable()) {
                return null;
            }
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.serviceInfo = serviceInfoGenerateServiceInfo;
            if ((this.mFlags & 64) != 0) {
                resolveInfo.filter = serviceIntentInfo;
            }
            resolveInfo.priority = serviceIntentInfo.getPriority();
            resolveInfo.preferredOrder = service.owner.mPreferredOrder;
            resolveInfo.match = i;
            resolveInfo.isDefault = serviceIntentInfo.hasDefault;
            resolveInfo.labelRes = serviceIntentInfo.labelRes;
            resolveInfo.nonLocalizedLabel = serviceIntentInfo.nonLocalizedLabel;
            resolveInfo.icon = serviceIntentInfo.icon;
            resolveInfo.system = resolveInfo.serviceInfo.applicationInfo.isSystemApp();
            return resolveInfo;
        }

        @Override
        protected void sortResults(List<ResolveInfo> list) {
            Collections.sort(list, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter printWriter, String str, PackageParser.ServiceIntentInfo serviceIntentInfo) {
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(serviceIntentInfo.service)));
            printWriter.print(' ');
            serviceIntentInfo.service.printComponentShortName(printWriter);
            printWriter.print(" filter ");
            printWriter.print(Integer.toHexString(System.identityHashCode(serviceIntentInfo)));
            if (serviceIntentInfo.service.info.permission != null) {
                printWriter.print(" permission ");
                printWriter.println(serviceIntentInfo.service.info.permission);
            } else {
                printWriter.println();
            }
        }

        @Override
        protected Object filterToLabel(PackageParser.ServiceIntentInfo serviceIntentInfo) {
            return serviceIntentInfo.service;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter printWriter, String str, Object obj, int i) {
            PackageParser.Service service = (PackageParser.Service) obj;
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(service)));
            printWriter.print(' ');
            service.printComponentShortName(printWriter);
            if (i > 1) {
                printWriter.print(" (");
                printWriter.print(i);
                printWriter.print(" filters)");
            }
            printWriter.println();
        }
    }

    private final class ProviderIntentResolver extends IntentResolver<PackageParser.ProviderIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, PackageParser.Provider> mProviders;

        private ProviderIntentResolver() {
            this.mProviders = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String str, boolean z, int i) {
            this.mFlags = z ? 65536 : 0;
            return super.queryIntent(intent, str, z, i);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String str, int i, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2)) {
                return null;
            }
            this.mFlags = i;
            return super.queryIntent(intent, str, (i & 65536) != 0, i2);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String str, int i, ArrayList<PackageParser.Provider> arrayList, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2) || arrayList == null) {
                return null;
            }
            this.mFlags = i;
            boolean z = (i & 65536) != 0;
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i3 = 0; i3 < size; i3++) {
                ArrayList arrayList3 = arrayList.get(i3).intents;
                if (arrayList3 != null && arrayList3.size() > 0) {
                    PackageParser.ProviderIntentInfo[] providerIntentInfoArr = new PackageParser.ProviderIntentInfo[arrayList3.size()];
                    arrayList3.toArray(providerIntentInfoArr);
                    arrayList2.add(providerIntentInfoArr);
                }
            }
            return super.queryIntentFromList(intent, str, z, arrayList2, i2);
        }

        public final void addProvider(PackageParser.Provider provider) {
            if (this.mProviders.containsKey(provider.getComponentName())) {
                Slog.w(PackageManagerService.TAG, "Provider " + provider.getComponentName() + " already defined; ignoring");
                return;
            }
            this.mProviders.put(provider.getComponentName(), provider);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(provider.info.nonLocalizedLabel != null ? provider.info.nonLocalizedLabel : provider.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
                Log.v(PackageManagerService.TAG, "    Class=" + provider.info.name);
            }
            int size = provider.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ProviderIntentInfo providerIntentInfo = (PackageParser.ProviderIntentInfo) provider.intents.get(i);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    providerIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!providerIntentInfo.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Provider " + provider.info.name);
                }
                addFilter(providerIntentInfo);
            }
        }

        public final void removeProvider(PackageParser.Provider provider) {
            this.mProviders.remove(provider.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(provider.info.nonLocalizedLabel != null ? provider.info.nonLocalizedLabel : provider.info.name);
                sb.append(":");
                Log.v(PackageManagerService.TAG, sb.toString());
                Log.v(PackageManagerService.TAG, "    Class=" + provider.info.name);
            }
            int size = provider.intents.size();
            for (int i = 0; i < size; i++) {
                PackageParser.ProviderIntentInfo providerIntentInfo = (PackageParser.ProviderIntentInfo) provider.intents.get(i);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    providerIntentInfo.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(providerIntentInfo);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ProviderIntentInfo providerIntentInfo, List<ResolveInfo> list) {
            ProviderInfo providerInfo = providerIntentInfo.provider.info;
            for (int size = list.size() - 1; size >= 0; size--) {
                ProviderInfo providerInfo2 = list.get(size).providerInfo;
                if (providerInfo2.name == providerInfo.name && providerInfo2.packageName == providerInfo.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ProviderIntentInfo[] newArray(int i) {
            return new PackageParser.ProviderIntentInfo[i];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ProviderIntentInfo providerIntentInfo, int i) {
            PackageSetting packageSetting;
            if (!PackageManagerService.sUserManager.exists(i)) {
                return true;
            }
            PackageParser.Package r4 = providerIntentInfo.provider.owner;
            return r4 != null && (packageSetting = (PackageSetting) r4.mExtras) != null && (packageSetting.pkgFlags & 1) == 0 && packageSetting.getStopped(i);
        }

        @Override
        protected boolean isPackageForFilter(String str, PackageParser.ProviderIntentInfo providerIntentInfo) {
            return str.equals(providerIntentInfo.provider.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ProviderIntentInfo providerIntentInfo, int i, int i2) {
            ProviderInfo providerInfoGenerateProviderInfo;
            if (!PackageManagerService.sUserManager.exists(i2) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(providerIntentInfo.provider.info, this.mFlags, i2)) {
                return null;
            }
            PackageParser.Provider provider = providerIntentInfo.provider;
            PackageSetting packageSetting = (PackageSetting) provider.owner.mExtras;
            if (packageSetting == null) {
                return null;
            }
            PackageUserState userState = packageSetting.readUserState(i2);
            boolean z = (this.mFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0;
            boolean z2 = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (z && !providerIntentInfo.isVisibleToInstantApp() && !userState.instantApp) {
                return null;
            }
            if (!z2 && userState.instantApp) {
                return null;
            }
            if ((userState.instantApp && packageSetting.isUpdateAvailable()) || (providerInfoGenerateProviderInfo = PackageParser.generateProviderInfo(provider, this.mFlags, userState, i2)) == null) {
                return null;
            }
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.providerInfo = providerInfoGenerateProviderInfo;
            if ((this.mFlags & 64) != 0) {
                resolveInfo.filter = providerIntentInfo;
            }
            resolveInfo.priority = providerIntentInfo.getPriority();
            resolveInfo.preferredOrder = provider.owner.mPreferredOrder;
            resolveInfo.match = i;
            resolveInfo.isDefault = providerIntentInfo.hasDefault;
            resolveInfo.labelRes = providerIntentInfo.labelRes;
            resolveInfo.nonLocalizedLabel = providerIntentInfo.nonLocalizedLabel;
            resolveInfo.icon = providerIntentInfo.icon;
            resolveInfo.system = resolveInfo.providerInfo.applicationInfo.isSystemApp();
            return resolveInfo;
        }

        @Override
        protected void sortResults(List<ResolveInfo> list) {
            Collections.sort(list, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter printWriter, String str, PackageParser.ProviderIntentInfo providerIntentInfo) {
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(providerIntentInfo.provider)));
            printWriter.print(' ');
            providerIntentInfo.provider.printComponentShortName(printWriter);
            printWriter.print(" filter ");
            printWriter.println(Integer.toHexString(System.identityHashCode(providerIntentInfo)));
        }

        @Override
        protected Object filterToLabel(PackageParser.ProviderIntentInfo providerIntentInfo) {
            return providerIntentInfo.provider;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter printWriter, String str, Object obj, int i) {
            PackageParser.Provider provider = (PackageParser.Provider) obj;
            printWriter.print(str);
            printWriter.print(Integer.toHexString(System.identityHashCode(provider)));
            printWriter.print(' ');
            provider.printComponentShortName(printWriter);
            if (i > 1) {
                printWriter.print(" (");
                printWriter.print(i);
                printWriter.print(" filters)");
            }
            printWriter.println();
        }
    }

    static final class InstantAppIntentResolver extends IntentResolver<AuxiliaryResolveInfo.AuxiliaryFilter, AuxiliaryResolveInfo.AuxiliaryFilter> {
        final ArrayMap<String, Pair<Integer, InstantAppResolveInfo>> mOrderResult = new ArrayMap<>();

        InstantAppIntentResolver() {
        }

        @Override
        protected AuxiliaryResolveInfo.AuxiliaryFilter[] newArray(int i) {
            return new AuxiliaryResolveInfo.AuxiliaryFilter[i];
        }

        @Override
        protected boolean isPackageForFilter(String str, AuxiliaryResolveInfo.AuxiliaryFilter auxiliaryFilter) {
            return true;
        }

        @Override
        protected AuxiliaryResolveInfo.AuxiliaryFilter newResult(AuxiliaryResolveInfo.AuxiliaryFilter auxiliaryFilter, int i, int i2) {
            if (!PackageManagerService.sUserManager.exists(i2)) {
                return null;
            }
            String packageName = auxiliaryFilter.resolveInfo.getPackageName();
            Integer numValueOf = Integer.valueOf(auxiliaryFilter.getOrder());
            Pair<Integer, InstantAppResolveInfo> pair = this.mOrderResult.get(packageName);
            if (pair != null && ((Integer) pair.first).intValue() >= numValueOf.intValue()) {
                return null;
            }
            InstantAppResolveInfo instantAppResolveInfo = auxiliaryFilter.resolveInfo;
            if (numValueOf.intValue() > 0) {
                this.mOrderResult.put(packageName, new Pair<>(numValueOf, instantAppResolveInfo));
            }
            return auxiliaryFilter;
        }

        @Override
        protected void filterResults(List<AuxiliaryResolveInfo.AuxiliaryFilter> list) {
            if (this.mOrderResult.size() == 0) {
                return;
            }
            int size = list.size();
            int i = 0;
            while (i < size) {
                InstantAppResolveInfo instantAppResolveInfo = list.get(i).resolveInfo;
                String packageName = instantAppResolveInfo.getPackageName();
                Pair<Integer, InstantAppResolveInfo> pair = this.mOrderResult.get(packageName);
                if (pair != null) {
                    if (pair.second == instantAppResolveInfo) {
                        this.mOrderResult.remove(packageName);
                        if (this.mOrderResult.size() == 0) {
                            return;
                        }
                    } else {
                        list.remove(i);
                        size--;
                        i--;
                    }
                }
                i++;
            }
        }
    }

    @Override
    public void sendPackageBroadcast(final String str, final String str2, final Bundle bundle, final int i, final String str3, final IIntentReceiver iIntentReceiver, final int[] iArr, final int[] iArr2) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int[] runningUserIds;
                try {
                    IActivityManager service = ActivityManager.getService();
                    if (service == null) {
                        return;
                    }
                    if (iArr == null) {
                        runningUserIds = service.getRunningUserIds();
                    } else {
                        runningUserIds = iArr;
                    }
                    PackageManagerService.this.doSendBroadcast(service, str, str2, bundle, i, str3, iIntentReceiver, runningUserIds, false);
                    if (iArr2 != null && iArr2 != PackageManagerService.EMPTY_INT_ARRAY) {
                        PackageManagerService.this.doSendBroadcast(service, str, str2, bundle, i, str3, iIntentReceiver, iArr2, true);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    @Override
    public void notifyPackageAdded(String str) {
        synchronized (this.mPackages) {
            if (this.mPackageListObservers.size() == 0) {
                return;
            }
            PackageManagerInternal.PackageListObserver[] packageListObserverArr = (PackageManagerInternal.PackageListObserver[]) this.mPackageListObservers.toArray();
            for (int length = packageListObserverArr.length - 1; length >= 0; length--) {
                packageListObserverArr[length].onPackageAdded(str);
            }
        }
    }

    @Override
    public void notifyPackageRemoved(String str) {
        synchronized (this.mPackages) {
            if (this.mPackageListObservers.size() == 0) {
                return;
            }
            PackageManagerInternal.PackageListObserver[] packageListObserverArr = (PackageManagerInternal.PackageListObserver[]) this.mPackageListObservers.toArray();
            for (int length = packageListObserverArr.length - 1; length >= 0; length--) {
                packageListObserverArr[length].onPackageRemoved(str);
            }
        }
    }

    private void doSendBroadcast(IActivityManager iActivityManager, String str, String str2, Bundle bundle, int i, String str3, IIntentReceiver iIntentReceiver, int[] iArr, boolean z) throws RemoteException {
        for (int i2 : iArr) {
            Intent intent = new Intent(str, str2 != null ? Uri.fromParts("package", str2, null) : null);
            String[] strArr = z ? INSTANT_APP_BROADCAST_PERMISSION : null;
            if (bundle != null) {
                intent.putExtras(bundle);
            }
            if (str3 != null) {
                intent.setPackage(str3);
            }
            int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
            if (intExtra > 0 && UserHandle.getUserId(intExtra) != i2) {
                intent.putExtra("android.intent.extra.UID", UserHandle.getUid(i2, UserHandle.getAppId(intExtra)));
            }
            intent.putExtra("android.intent.extra.user_handle", i2);
            intent.addFlags(67108864 | i);
            if (DEBUG_BROADCASTS) {
                RuntimeException runtimeException = new RuntimeException("here");
                runtimeException.fillInStackTrace();
                Slog.d(TAG, "Sending to user " + i2 + ": " + intent.toShortString(false, true, false, false) + " " + intent.getExtras(), runtimeException);
            }
            iActivityManager.broadcastIntent((IApplicationThread) null, intent, (String) null, iIntentReceiver, 0, (String) null, (Bundle) null, strArr, -1, (Bundle) null, iIntentReceiver != null, false, i2);
        }
    }

    private boolean isExternalMediaAvailable() {
        return this.mMediaMounted || Environment.isExternalStorageEmulated();
    }

    public PackageCleanItem nextPackageToClean(PackageCleanItem packageCleanItem) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || !isExternalMediaAvailable()) {
            return null;
        }
        synchronized (this.mPackages) {
            ArrayList<PackageCleanItem> arrayList = this.mSettings.mPackagesToBeCleaned;
            if (packageCleanItem != null) {
                arrayList.remove(packageCleanItem);
            }
            if (arrayList.size() <= 0) {
                return null;
            }
            return arrayList.get(0);
        }
    }

    void schedulePackageCleaning(String str, int i, boolean z) {
        Message messageObtainMessage = this.mHandler.obtainMessage(7, i, z ? 1 : 0, str);
        if (this.mSystemReady) {
            messageObtainMessage.sendToTarget();
            return;
        }
        if (this.mPostSystemReadyMessages == null) {
            this.mPostSystemReadyMessages = new ArrayList<>();
        }
        this.mPostSystemReadyMessages.add(messageObtainMessage);
    }

    void startCleaningPackages() {
        if (!isExternalMediaAvailable()) {
            return;
        }
        synchronized (this.mPackages) {
            if (this.mSettings.mPackagesToBeCleaned.isEmpty()) {
                return;
            }
            Intent intent = new Intent("android.content.pm.CLEAN_EXTERNAL_STORAGE");
            intent.setComponent(DEFAULT_CONTAINER_COMPONENT);
            IActivityManager service = ActivityManager.getService();
            if (service != null) {
                int uid = -1;
                synchronized (this.mPackages) {
                    if (!this.mDefaultContainerWhitelisted) {
                        this.mDefaultContainerWhitelisted = true;
                        uid = UserHandle.getUid(0, this.mSettings.mPackages.get(DEFAULT_CONTAINER_PACKAGE).appId);
                    }
                }
                if (uid > 0) {
                    try {
                        service.backgroundWhitelistUid(uid);
                    } catch (RemoteException e) {
                        return;
                    }
                }
                service.startService((IApplicationThread) null, intent, (String) null, false, this.mContext.getOpPackageName(), 0);
            }
        }
    }

    private int fixUpInstallReason(String str, int i, int i2) {
        if (checkUidPermission("android.permission.INSTALL_PACKAGES", i) == 0) {
            return i2;
        }
        String deviceOwnerOrProfileOwnerPackage = this.mProtectedPackages.getDeviceOwnerOrProfileOwnerPackage(UserHandle.getUserId(i));
        if (deviceOwnerOrProfileOwnerPackage != null && deviceOwnerOrProfileOwnerPackage.equals(str)) {
            return 1;
        }
        if (i2 == 1) {
            return 0;
        }
        return i2;
    }

    void earlyBindToDefContainer() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21));
    }

    void installStage(String str, File file, IPackageInstallObserver2 iPackageInstallObserver2, PackageInstaller.SessionParams sessionParams, String str2, int i, UserHandle userHandle, PackageParser.SigningDetails signingDetails) {
        if (DEBUG_INSTANT && (sessionParams.installFlags & 2048) != 0) {
            Slog.d(TAG, "Ephemeral install of " + str);
        }
        VerificationInfo verificationInfo = new VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, i);
        OriginInfo originInfoFromStagedFile = OriginInfo.fromStagedFile(file);
        Message messageObtainMessage = this.mHandler.obtainMessage(5);
        InstallParams installParams = new InstallParams(originInfoFromStagedFile, null, iPackageInstallObserver2, sessionParams.installFlags, str2, sessionParams.volumeUuid, verificationInfo, userHandle, sessionParams.abiOverride, sessionParams.grantedRuntimePermissions, signingDetails, fixUpInstallReason(str2, i, sessionParams.installReason));
        installParams.setTraceMethod("installStage").setTraceCookie(System.identityHashCode(installParams));
        messageObtainMessage.obj = installParams;
        Trace.asyncTraceBegin(262144L, "installStage", System.identityHashCode(messageObtainMessage.obj));
        Trace.asyncTraceBegin(262144L, "queueInstall", System.identityHashCode(messageObtainMessage.obj));
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void sendPackageAddedForUser(String str, PackageSetting packageSetting, int i) {
        boolean z = isSystemApp(packageSetting) || isUpdatedSystemApp(packageSetting);
        boolean instantApp = packageSetting.getInstantApp(i);
        sendPackageAddedForNewUsers(str, z, false, packageSetting.appId, instantApp ? EMPTY_INT_ARRAY : new int[]{i}, instantApp ? new int[]{i} : EMPTY_INT_ARRAY);
        PackageInstaller.SessionInfo sessionInfo = new PackageInstaller.SessionInfo();
        sessionInfo.installReason = packageSetting.getInstallReason(i);
        sessionInfo.appPackageName = str;
        sendSessionCommitBroadcast(sessionInfo, i);
        sPmsExt.onPackageAdded(str, i);
    }

    @Override
    public void sendPackageAddedForNewUsers(final String str, boolean z, final boolean z2, int i, final int[] iArr, int[] iArr2) {
        if (ArrayUtils.isEmpty(iArr) && ArrayUtils.isEmpty(iArr2)) {
            return;
        }
        Bundle bundle = new Bundle(1);
        bundle.putInt("android.intent.extra.UID", UserHandle.getUid(ArrayUtils.isEmpty(iArr) ? iArr2[0] : iArr[0], i));
        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", str, bundle, 0, null, null, iArr, iArr2);
        if (z && !ArrayUtils.isEmpty(iArr)) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PackageManagerService.lambda$sendPackageAddedForNewUsers$6(this.f$0, iArr, str, z2);
                }
            });
        }
    }

    public static void lambda$sendPackageAddedForNewUsers$6(PackageManagerService packageManagerService, int[] iArr, String str, boolean z) {
        for (int i : iArr) {
            packageManagerService.sendBootCompletedBroadcastToSystemApp(str, z, i);
        }
    }

    private void sendBootCompletedBroadcastToSystemApp(String str, boolean z, int i) {
        if (!this.mUserManagerInternal.isUserRunning(i)) {
            return;
        }
        IActivityManager service = ActivityManager.getService();
        try {
            Intent intent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED").setPackage(str);
            if (z) {
                intent.addFlags(32);
            }
            String[] strArr = {"android.permission.RECEIVE_BOOT_COMPLETED"};
            service.broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, strArr, -1, (Bundle) null, false, false, i);
            if (this.mUserManagerInternal.isUserUnlockingOrUnlocked(i)) {
                Intent intent2 = new Intent("android.intent.action.BOOT_COMPLETED").setPackage(str);
                if (z) {
                    intent2.addFlags(32);
                }
                service.broadcastIntent((IApplicationThread) null, intent2, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, strArr, -1, (Bundle) null, false, false, i);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setApplicationHiddenSettingAsUser(String str, boolean z, int i) {
        boolean z2;
        boolean z3;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "setApplicationHiddenSetting for user " + i);
        if (z && isPackageDeviceAdmin(str, i)) {
            Slog.w(TAG, "Not hiding package " + str + ": has active device admin");
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                if (packageSetting == null) {
                    return false;
                }
                if (filterAppAccessLPr(packageSetting, callingUid, i)) {
                    return false;
                }
                if (PLATFORM_PACKAGE_NAME.equals(str)) {
                    Slog.w(TAG, "Cannot hide package: android");
                    return false;
                }
                PackageParser.Package r6 = this.mPackages.get(str);
                if (r6 != null && r6.staticSharedLibName != null) {
                    Slog.w(TAG, "Cannot hide package: " + str + " providing static shared library: " + r6.staticSharedLibName);
                    return false;
                }
                if (z && !UserHandle.isSameApp(callingUid, packageSetting.appId) && this.mProtectedPackages.isPackageStateProtected(i, str)) {
                    Slog.w(TAG, "Not hiding protected package: " + str);
                    return false;
                }
                if (packageSetting.getHidden(i) != z) {
                    packageSetting.setHidden(z, i);
                    this.mSettings.writePackageRestrictionsLPr(i);
                    if (z) {
                        z2 = false;
                        z3 = true;
                    } else {
                        z3 = false;
                        z2 = true;
                    }
                } else {
                    z2 = false;
                    z3 = false;
                }
                if (z2) {
                    sendPackageAddedForUser(str, packageSetting, i);
                    return true;
                }
                if (!z3) {
                    return false;
                }
                killApplication(str, UserHandle.getUid(i, packageSetting.appId), "hiding pkg");
                sendApplicationHiddenForUser(str, packageSetting, i);
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendApplicationHiddenForUser(String str, PackageSetting packageSetting, int i) {
        PackageRemovedInfo packageRemovedInfo = new PackageRemovedInfo(this);
        packageRemovedInfo.removedPackage = str;
        packageRemovedInfo.installerPackageName = packageSetting.installerPackageName;
        packageRemovedInfo.removedUsers = new int[]{i};
        packageRemovedInfo.broadcastUsers = new int[]{i};
        packageRemovedInfo.uid = UserHandle.getUid(i, packageSetting.appId);
        packageRemovedInfo.sendPackageRemovedBroadcasts(true);
    }

    private void sendPackagesSuspendedForUser(String[] strArr, int i, boolean z, PersistableBundle persistableBundle) {
        if (strArr.length > 0) {
            Bundle bundle = new Bundle(1);
            bundle.putStringArray("android.intent.extra.changed_package_list", strArr);
            if (persistableBundle != null) {
                bundle.putBundle("android.intent.extra.LAUNCHER_EXTRAS", new Bundle(persistableBundle.deepCopy()));
            }
            sendPackageBroadcast(z ? "android.intent.action.PACKAGES_SUSPENDED" : "android.intent.action.PACKAGES_UNSUSPENDED", null, bundle, 1073741824, null, null, new int[]{i}, null);
        }
    }

    public boolean getApplicationHiddenSettingAsUser(String str, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "getApplicationHidden for user " + i);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                if (packageSetting == null) {
                    return true;
                }
                if (filterAppAccessLPr(packageSetting, callingUid, i)) {
                    return true;
                }
                return packageSetting.getHidden(i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int installExistingPackageAsUser(String str, int i, int i2, int i3) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "installExistingPackage for user " + i);
        if (isUserRestricted(i, "no_install_apps")) {
            return -111;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean z2 = (i2 & 2048) != 0;
        boolean z3 = (i2 & 16384) != 0;
        try {
            synchronized (this.mPackages) {
                PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                if (packageSetting == null) {
                    return -3;
                }
                if (!canViewInstantApps(callingUid, UserHandle.getUserId(callingUid))) {
                    boolean z4 = false;
                    for (int i4 : sUserManager.getUserIds()) {
                        z4 = !packageSetting.getInstantApp(i4);
                        if (z4) {
                            break;
                        }
                    }
                    if (!z4) {
                        return -3;
                    }
                }
                if (packageSetting.getInstalled(i)) {
                    z = z3 && packageSetting.getInstantApp(i);
                    setInstantAppForUser(packageSetting, i, z2, z3);
                    if (z) {
                        if (packageSetting.pkg != null) {
                            synchronized (this.mInstallLock) {
                                prepareAppDataAfterInstallLIF(packageSetting.pkg);
                            }
                        }
                        sendPackageAddedForUser(str, packageSetting, i);
                        synchronized (this.mPackages) {
                            updateSequenceNumberLP(packageSetting, new int[]{i});
                        }
                    }
                    return 1;
                }
                packageSetting.setInstalled(true, i);
                packageSetting.setHidden(false, i);
                packageSetting.setInstallReason(i3, i);
                this.mSettings.writePackageRestrictionsLPr(i);
                this.mSettings.writeKernelMappingLPr(packageSetting);
                setInstantAppForUser(packageSetting, i, z2, z3);
                if (z) {
                }
                return 1;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    static void setInstantAppForUser(PackageSetting packageSetting, int i, boolean z, boolean z2) {
        if (!z && !z2) {
            return;
        }
        if (i != -1) {
            if (z && !packageSetting.getInstantApp(i)) {
                packageSetting.setInstantApp(true, i);
                return;
            } else {
                if (z2 && packageSetting.getInstantApp(i)) {
                    packageSetting.setInstantApp(false, i);
                    return;
                }
                return;
            }
        }
        for (int i2 : sUserManager.getUserIds()) {
            if (z && !packageSetting.getInstantApp(i2)) {
                packageSetting.setInstantApp(true, i2);
            } else if (z2 && packageSetting.getInstantApp(i2)) {
                packageSetting.setInstantApp(false, i2);
            }
        }
    }

    boolean isUserRestricted(int i, String str) {
        if (!sUserManager.getUserRestrictions(i).getBoolean(str, false)) {
            return false;
        }
        Log.w(TAG, "User is restricted: " + str);
        return true;
    }

    public String[] setPackagesSuspendedAsUser(String[] strArr, boolean z, PersistableBundle persistableBundle, PersistableBundle persistableBundle2, String str, String str2, int i) throws Throwable {
        long j;
        long j2;
        int i2;
        String[] strArr2 = strArr;
        boolean z2 = z;
        String str3 = str2;
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", "setPackagesSuspendedAsUser");
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000 && getPackageUid(str3, 0, i) != callingUid) {
            throw new SecurityException("Calling package " + str3 + " in user " + i + " does not belong to calling uid " + callingUid);
        }
        if (!PLATFORM_PACKAGE_NAME.equals(str3) && this.mProtectedPackages.getDeviceOwnerOrProfileOwnerPackage(i) != null) {
            throw new UnsupportedOperationException("Cannot suspend/unsuspend packages. User " + i + " has an active DO or PO");
        }
        if (ArrayUtils.isEmpty(strArr)) {
            return strArr2;
        }
        ArrayList arrayList = new ArrayList(strArr2.length);
        ArrayList arrayList2 = new ArrayList(strArr2.length);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                int i3 = 0;
                while (i3 < strArr2.length) {
                    try {
                        String str4 = strArr2[i3];
                        if (str3.equals(str4)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Calling package: ");
                            sb.append(str3);
                            sb.append(" trying to ");
                            sb.append(z2 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "un");
                            sb.append("suspend itself. Ignoring");
                            Slog.w(TAG, sb.toString());
                            arrayList2.add(str4);
                        } else {
                            PackageSetting packageSetting = this.mSettings.mPackages.get(str4);
                            if (packageSetting == null || filterAppAccessLPr(packageSetting, callingUid, i)) {
                                i2 = i3;
                                j2 = jClearCallingIdentity;
                                Slog.w(TAG, "Could not find package setting for package: " + str4 + ". Skipping suspending/un-suspending.");
                                arrayList2.add(str4);
                            } else if (z2 && !canSuspendPackageForUserLocked(str4, i)) {
                                arrayList2.add(str4);
                            } else {
                                boolean z3 = z2;
                                i2 = i3;
                                j2 = jClearCallingIdentity;
                                try {
                                    packageSetting.setSuspended(z3, str3, str, persistableBundle, persistableBundle2, i);
                                    arrayList.add(str4);
                                } catch (Throwable th) {
                                    th = th;
                                    j = j2;
                                    while (true) {
                                        try {
                                            try {
                                                throw th;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                Binder.restoreCallingIdentity(j);
                                                throw th;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                }
                            }
                            i3 = i2 + 1;
                            jClearCallingIdentity = j2;
                            strArr2 = strArr;
                            z2 = z;
                            str3 = str2;
                        }
                        i2 = i3;
                        j2 = jClearCallingIdentity;
                        i3 = i2 + 1;
                        jClearCallingIdentity = j2;
                        strArr2 = strArr;
                        z2 = z;
                        str3 = str2;
                    } catch (Throwable th4) {
                        th = th4;
                        j = jClearCallingIdentity;
                    }
                }
                j2 = jClearCallingIdentity;
                Binder.restoreCallingIdentity(j2);
                if (!arrayList.isEmpty()) {
                    String[] strArr3 = (String[]) arrayList.toArray(new String[arrayList.size()]);
                    sendPackagesSuspendedForUser(strArr3, i, z, persistableBundle2);
                    sendMyPackageSuspendedOrUnsuspended(strArr3, z, persistableBundle, i);
                    synchronized (this.mPackages) {
                        scheduleWritePackageRestrictionsLocked(i);
                    }
                }
                return (String[]) arrayList2.toArray(new String[arrayList2.size()]);
            }
        } catch (Throwable th5) {
            th = th5;
            j = jClearCallingIdentity;
        }
    }

    public PersistableBundle getSuspendedPackageAppExtras(String str, int i) {
        int callingUid = Binder.getCallingUid();
        if (getPackageUid(str, 0, i) != callingUid) {
            throw new SecurityException("Calling package " + str + " does not belong to calling uid " + callingUid);
        }
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null || filterAppAccessLPr(packageSetting, callingUid, i)) {
                throw new IllegalArgumentException("Unknown target package: " + str);
            }
            PackageUserState userState = packageSetting.readUserState(i);
            if (userState.suspended) {
                return userState.suspendedAppExtras;
            }
            return null;
        }
    }

    private void sendMyPackageSuspendedOrUnsuspended(final String[] strArr, final boolean z, PersistableBundle persistableBundle, final int i) {
        final String str;
        final Bundle bundle = new Bundle();
        if (z) {
            if (persistableBundle != null) {
                bundle.putBundle("android.intent.extra.SUSPENDED_PACKAGE_EXTRAS", new Bundle(persistableBundle.deepCopy()));
            }
            str = "android.intent.action.MY_PACKAGE_SUSPENDED";
        } else {
            str = "android.intent.action.MY_PACKAGE_UNSUSPENDED";
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IActivityManager service = ActivityManager.getService();
                    if (service == null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("IActivityManager null. Cannot send MY_PACKAGE_ ");
                        sb.append(z ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "UN");
                        sb.append("SUSPENDED broadcasts");
                        Slog.wtf(PackageManagerService.TAG, sb.toString());
                        return;
                    }
                    int[] iArr = {i};
                    for (String str2 : strArr) {
                        PackageManagerService.this.doSendBroadcast(service, str, null, bundle, DumpState.DUMP_SERVICE_PERMISSIONS, str2, null, iArr, false);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    public boolean isPackageSuspendedForUser(String str, int i) {
        boolean suspended;
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "isPackageSuspendedForUser for user " + i);
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null || filterAppAccessLPr(packageSetting, callingUid, i)) {
                throw new IllegalArgumentException("Unknown target package: " + str);
            }
            suspended = packageSetting.getSuspended(i);
        }
        return suspended;
    }

    void unsuspendForSuspendingPackage(final String str, int i) {
        for (int i2 : i == -1 ? sUserManager.getUserIds() : new int[]{i}) {
            Objects.requireNonNull(str);
            unsuspendForSuspendingPackages(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return str.equals((String) obj);
                }
            }, i2);
        }
    }

    void unsuspendForNonSystemSuspendingPackages(ArraySet<Integer> arraySet) {
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            unsuspendForSuspendingPackages(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return PackageManagerService.lambda$unsuspendForNonSystemSuspendingPackages$7((String) obj);
                }
            }, arraySet.valueAt(i).intValue());
        }
    }

    static boolean lambda$unsuspendForNonSystemSuspendingPackages$7(String str) {
        return !PLATFORM_PACKAGE_NAME.equals(str);
    }

    private void unsuspendForSuspendingPackages(Predicate<String> predicate, int i) {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mPackages) {
            for (PackageSetting packageSetting : this.mSettings.mPackages.values()) {
                PackageUserState userState = packageSetting.readUserState(i);
                if (userState.suspended && predicate.test(userState.suspendingPackage)) {
                    packageSetting.setSuspended(false, null, null, null, null, i);
                    arrayList.add(packageSetting.name);
                }
            }
        }
        if (!arrayList.isEmpty()) {
            String[] strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
            sendMyPackageSuspendedOrUnsuspended(strArr, false, null, i);
            sendPackagesSuspendedForUser(strArr, i, false, null);
            this.mSettings.writePackageRestrictionsLPr(i);
        }
    }

    @GuardedBy("mPackages")
    private boolean canSuspendPackageForUserLocked(String str, int i) {
        if (isPackageDeviceAdmin(str, i)) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": has an active device admin");
            return false;
        }
        if (str.equals(getActiveLauncherPackageName(i))) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": contains the active launcher");
            return false;
        }
        if (str.equals(this.mRequiredInstallerPackage)) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": required for package installation");
            return false;
        }
        if (str.equals(this.mRequiredUninstallerPackage)) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": required for package uninstallation");
            return false;
        }
        if (str.equals(this.mRequiredVerifierPackage)) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": required for package verification");
            return false;
        }
        if (str.equals(getDefaultDialerPackageName(i))) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": is the default dialer");
            return false;
        }
        if (this.mProtectedPackages.isPackageStateProtected(i, str)) {
            Slog.w(TAG, "Cannot suspend package \"" + str + "\": protected package");
            return false;
        }
        PackageParser.Package r6 = this.mPackages.get(str);
        if (r6 != null && r6.applicationInfo.isStaticSharedLibrary()) {
            Slog.w(TAG, "Cannot suspend package: " + str + " providing static shared library: " + r6.staticSharedLibName);
            return false;
        }
        if (PLATFORM_PACKAGE_NAME.equals(str)) {
            Slog.w(TAG, "Cannot suspend package: " + str);
            return false;
        }
        return true;
    }

    private String getActiveLauncherPackageName(int i) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfoResolveIntent = resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 65536, i);
        if (resolveInfoResolveIntent == null) {
            return null;
        }
        return resolveInfoResolveIntent.activityInfo.packageName;
    }

    private String getDefaultDialerPackageName(int i) {
        String defaultDialerPackageNameLPw;
        synchronized (this.mPackages) {
            defaultDialerPackageNameLPw = this.mSettings.getDefaultDialerPackageNameLPw(i);
        }
        return defaultDialerPackageNameLPw;
    }

    public void verifyPendingInstall(int i, int i2) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can verify applications");
        Message messageObtainMessage = this.mHandler.obtainMessage(15);
        PackageVerificationResponse packageVerificationResponse = new PackageVerificationResponse(i2, Binder.getCallingUid());
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = packageVerificationResponse;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void extendVerificationTimeout(int i, int i2, long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can extend verification timeouts");
        PackageVerificationState packageVerificationState = this.mPendingVerification.get(i);
        PackageVerificationResponse packageVerificationResponse = new PackageVerificationResponse(i2, Binder.getCallingUid());
        if (j > AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) {
            j = 3600000;
        }
        if (j < 0) {
            j = 0;
        }
        if (i2 == 1 || i2 != -1) {
        }
        if (packageVerificationState != null && !packageVerificationState.timeoutExtended()) {
            packageVerificationState.extendTimeout();
            Message messageObtainMessage = this.mHandler.obtainMessage(15);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.obj = packageVerificationResponse;
            this.mHandler.sendMessageDelayed(messageObtainMessage, j);
        }
    }

    private void broadcastPackageVerified(int i, Uri uri, int i2, UserHandle userHandle) {
        Intent intent = new Intent("android.intent.action.PACKAGE_VERIFIED");
        intent.setDataAndType(uri, PACKAGE_MIME_TYPE);
        intent.addFlags(1);
        intent.putExtra("android.content.pm.extra.VERIFICATION_ID", i);
        intent.putExtra("android.content.pm.extra.VERIFICATION_RESULT", i2);
        this.mContext.sendBroadcastAsUser(intent, userHandle, "android.permission.PACKAGE_VERIFICATION_AGENT");
    }

    private ComponentName matchComponentForVerifier(String str, List<ResolveInfo> list) {
        ActivityInfo activityInfo;
        int size = list.size();
        int i = 0;
        while (true) {
            if (i < size) {
                ResolveInfo resolveInfo = list.get(i);
                if (resolveInfo.activityInfo == null || !str.equals(resolveInfo.activityInfo.packageName)) {
                    i++;
                } else {
                    activityInfo = resolveInfo.activityInfo;
                    break;
                }
            } else {
                activityInfo = null;
                break;
            }
        }
        if (activityInfo == null) {
            return null;
        }
        return new ComponentName(activityInfo.packageName, activityInfo.name);
    }

    private List<ComponentName> matchVerifiers(PackageInfoLite packageInfoLite, List<ResolveInfo> list, PackageVerificationState packageVerificationState) {
        int uidForVerifier;
        if (packageInfoLite.verifiers.length == 0) {
            return null;
        }
        int length = packageInfoLite.verifiers.length;
        ArrayList arrayList = new ArrayList(length + 1);
        for (int i = 0; i < length; i++) {
            VerifierInfo verifierInfo = packageInfoLite.verifiers[i];
            ComponentName componentNameMatchComponentForVerifier = matchComponentForVerifier(verifierInfo.packageName, list);
            if (componentNameMatchComponentForVerifier != null && (uidForVerifier = getUidForVerifier(verifierInfo)) != -1) {
                if (DEBUG_VERIFY) {
                    Slog.d(TAG, "Added sufficient verifier " + verifierInfo.packageName + " with the correct signature");
                }
                arrayList.add(componentNameMatchComponentForVerifier);
                packageVerificationState.addSufficientVerifier(uidForVerifier);
            }
        }
        return arrayList;
    }

    private int getUidForVerifier(VerifierInfo verifierInfo) {
        synchronized (this.mPackages) {
            PackageParser.Package r1 = this.mPackages.get(verifierInfo.packageName);
            if (r1 == null) {
                return -1;
            }
            if (r1.mSigningDetails.signatures.length != 1) {
                Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " has more than one signature; ignoring");
                return -1;
            }
            try {
                if (!Arrays.equals(verifierInfo.publicKey.getEncoded(), r1.mSigningDetails.signatures[0].getPublicKey().getEncoded())) {
                    Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " does not have the expected public key; ignoring");
                    return -1;
                }
                return r1.applicationInfo.uid;
            } catch (CertificateException e) {
                return -1;
            }
        }
    }

    public void finishPackageInstall(int i, boolean z) {
        enforceSystemOrRoot("Only the system is allowed to finish installs");
        if (DEBUG_INSTALL) {
            Slog.v(TAG, "BM finishing package install for " + i);
        }
        Trace.asyncTraceEnd(262144L, "restore", i);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, i, z ? 1 : 0));
    }

    private long getVerificationTimeout() {
        return Settings.Global.getLong(this.mContext.getContentResolver(), "verifier_timeout", 10000L);
    }

    private int getDefaultVerificationResponse(UserHandle userHandle) {
        if (sUserManager.hasUserRestriction("ensure_verify_apps", userHandle.getIdentifier())) {
            return -1;
        }
        return Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_default_response", 1);
    }

    private boolean isVerificationEnabled(int i, int i2, int i3) {
        boolean zIsUserRestricted = isUserRestricted(i, "ensure_verify_apps");
        if ((i2 & 32) != 0) {
            if (ActivityManager.isRunningInTestHarness()) {
                return false;
            }
            if (zIsUserRestricted) {
                return true;
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) == 0) {
                return false;
            }
        } else if ((i2 & 2048) != 0 && this.mInstantAppInstallerActivity != null && this.mInstantAppInstallerActivity.packageName.equals(this.mRequiredVerifierPackage)) {
            try {
                ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(i3, this.mRequiredVerifierPackage);
                if (DEBUG_VERIFY) {
                    Slog.i(TAG, "disable verification for instant app");
                }
                return false;
            } catch (SecurityException e) {
            }
        }
        return zIsUserRestricted || Settings.Global.getInt(this.mContext.getContentResolver(), "package_verifier_enable", 1) == 1;
    }

    public void verifyIntentFilter(int i, int i2, List<String> list) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", "Only intentfilter verification agents can verify applications");
        Message messageObtainMessage = this.mHandler.obtainMessage(18);
        IntentFilterVerificationResponse intentFilterVerificationResponse = new IntentFilterVerificationResponse(Binder.getCallingUid(), i2, list);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = intentFilterVerificationResponse;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public int getIntentVerificationStatus(String str, int i) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getUserId(callingUid) != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "getIntentVerificationStatus" + i);
        }
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting != null && !filterAppAccessLPr(packageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                return this.mSettings.getIntentFilterVerificationStatusLPr(str, i);
            }
            return 0;
        }
    }

    public boolean updateIntentVerificationStatus(String str, int i, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.mPackages.get(str), Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                return false;
            }
            boolean zUpdateIntentFilterVerificationStatusLPw = this.mSettings.updateIntentFilterVerificationStatusLPw(str, i, i2);
            if (zUpdateIntentFilterVerificationStatusLPw) {
                scheduleWritePackageRestrictionsLocked(i2);
            }
            return zUpdateIntentFilterVerificationStatusLPw;
        }
    }

    public ParceledListSlice<IntentFilterVerificationInfo> getIntentFilterVerifications(String str) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.mPackages.get(str), callingUid, UserHandle.getUserId(callingUid))) {
                return ParceledListSlice.emptyList();
            }
            return new ParceledListSlice<>(this.mSettings.getIntentFilterVerificationsLPr(str));
        }
    }

    public ParceledListSlice<IntentFilter> getAllIntentFilters(String str) {
        if (TextUtils.isEmpty(str)) {
            return ParceledListSlice.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mPackages) {
            PackageParser.Package r7 = this.mPackages.get(str);
            if (r7 != null && r7.activities != null) {
                if (r7.mExtras == null) {
                    return ParceledListSlice.emptyList();
                }
                if (filterAppAccessLPr((PackageSetting) r7.mExtras, callingUid, userId)) {
                    return ParceledListSlice.emptyList();
                }
                int size = r7.activities.size();
                ArrayList arrayList = new ArrayList();
                for (int i = 0; i < size; i++) {
                    PackageParser.Activity activity = (PackageParser.Activity) r7.activities.get(i);
                    if (activity.intents != null && activity.intents.size() > 0) {
                        arrayList.addAll(activity.intents);
                    }
                }
                return new ParceledListSlice<>(arrayList);
            }
            return ParceledListSlice.emptyList();
        }
    }

    public boolean setDefaultBrowserPackageName(String str, int i) {
        boolean defaultBrowserPackageNameLPw;
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        synchronized (this.mPackages) {
            defaultBrowserPackageNameLPw = this.mSettings.setDefaultBrowserPackageNameLPw(str, i);
            if (str != null) {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultBrowser(str, i);
            }
        }
        return defaultBrowserPackageNameLPw;
    }

    public String getDefaultBrowserPackageName(int i) {
        String defaultBrowserPackageNameLPw;
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            defaultBrowserPackageNameLPw = this.mSettings.getDefaultBrowserPackageNameLPw(i);
        }
        return defaultBrowserPackageNameLPw;
    }

    private int getUnknownSourcesSettings() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "install_non_market_apps", -1);
    }

    public void setInstallerPackageName(String str, String str2) {
        PackageSetting packageSetting;
        Signature[] signatureArr;
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return;
        }
        synchronized (this.mPackages) {
            PackageSetting packageSetting2 = this.mSettings.mPackages.get(str);
            if (packageSetting2 == null || filterAppAccessLPr(packageSetting2, callingUid, UserHandle.getUserId(callingUid))) {
                throw new IllegalArgumentException("Unknown target package: " + str);
            }
            PackageSetting packageSetting3 = null;
            if (str2 != null) {
                packageSetting = this.mSettings.mPackages.get(str2);
                if (packageSetting == null) {
                    throw new IllegalArgumentException("Unknown installer package: " + str2);
                }
            } else {
                packageSetting = null;
            }
            Object userIdLPr = this.mSettings.getUserIdLPr(callingUid);
            if (userIdLPr != null) {
                if (userIdLPr instanceof SharedUserSetting) {
                    signatureArr = ((SharedUserSetting) userIdLPr).signatures.mSigningDetails.signatures;
                } else if (userIdLPr instanceof PackageSetting) {
                    signatureArr = ((PackageSetting) userIdLPr).signatures.mSigningDetails.signatures;
                } else {
                    throw new SecurityException("Bad object " + userIdLPr + " for uid " + callingUid);
                }
                if (packageSetting != null && PackageManagerServiceUtils.compareSignatures(signatureArr, packageSetting.signatures.mSigningDetails.signatures) != 0) {
                    throw new SecurityException("Caller does not have same cert as new installer package " + str2);
                }
                String str3 = packageSetting2.installerPackageName;
                if (str3 != null) {
                    packageSetting3 = this.mSettings.mPackages.get(str3);
                }
                if (packageSetting3 != null) {
                    if (PackageManagerServiceUtils.compareSignatures(signatureArr, packageSetting3.signatures.mSigningDetails.signatures) != 0) {
                        throw new SecurityException("Caller does not have same cert as old installer package " + str3);
                    }
                } else if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                    EventLog.writeEvent(1397638484, "150857253", Integer.valueOf(callingUid), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    if (getUidTargetSdkVersionLockedLPr(callingUid) <= 29) {
                        return;
                    }
                    throw new SecurityException("Neither user " + callingUid + " nor current process has android.permission.INSTALL_PACKAGES");
                }
                packageSetting2.installerPackageName = str2;
                if (str2 != null) {
                    this.mSettings.mInstallerPackages.add(str2);
                }
                scheduleWriteSettingsLocked();
                return;
            }
            throw new SecurityException("Unknown calling UID: " + callingUid);
        }
    }

    public void setApplicationCategoryHint(String str, int i, String str2) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str2);
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                throw new IllegalArgumentException("Unknown target package " + str);
            }
            if (filterAppAccessLPr(packageSetting, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                throw new IllegalArgumentException("Unknown target package " + str);
            }
            if (!Objects.equals(str2, packageSetting.installerPackageName)) {
                throw new IllegalArgumentException("Calling package " + str2 + " is not installer for " + str);
            }
            if (packageSetting.categoryHint != i) {
                packageSetting.categoryHint = i;
                scheduleWriteSettingsLocked();
            }
        }
    }

    private void processPendingInstall(final InstallArgs installArgs, final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int i2;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                PackageInstalledInfo packageInstalledInfo = new PackageInstalledInfo();
                packageInstalledInfo.setReturnCode(i);
                packageInstalledInfo.uid = -1;
                packageInstalledInfo.pkg = null;
                packageInstalledInfo.removedInfo = null;
                if (packageInstalledInfo.returnCode == 1) {
                    installArgs.doPreInstall(packageInstalledInfo.returnCode);
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.installPackageTracedLI(installArgs, packageInstalledInfo);
                    }
                    installArgs.doPostInstall(packageInstalledInfo.returnCode, packageInstalledInfo.uid);
                }
                boolean z = (packageInstalledInfo.removedInfo == null || packageInstalledInfo.removedInfo.removedPackage == null) ? false : true;
                if (packageInstalledInfo.pkg != null) {
                    i2 = packageInstalledInfo.pkg.applicationInfo.flags;
                } else {
                    i2 = 0;
                }
                boolean z2 = (z || (32768 & i2) == 0) ? false : true;
                if (PackageManagerService.this.mNextInstallToken < 0) {
                    PackageManagerService.this.mNextInstallToken = 1;
                }
                PackageManagerService packageManagerService = PackageManagerService.this;
                int i3 = packageManagerService.mNextInstallToken;
                packageManagerService.mNextInstallToken = i3 + 1;
                PackageManagerService.this.mRunningInstalls.put(i3, new PostInstallData(installArgs, packageInstalledInfo));
                if (PackageManagerService.DEBUG_INSTALL) {
                    Log.v(PackageManagerService.TAG, "+ starting restore round-trip " + i3);
                }
                if (packageInstalledInfo.returnCode == 1 && z2) {
                    IBackupManager iBackupManagerAsInterface = IBackupManager.Stub.asInterface(ServiceManager.getService(BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD));
                    if (iBackupManagerAsInterface != null) {
                        if (PackageManagerService.DEBUG_INSTALL) {
                            Log.v(PackageManagerService.TAG, "token " + i3 + " to BM for possible restore");
                        }
                        Trace.asyncTraceBegin(262144L, "restore", i3);
                        try {
                            if (iBackupManagerAsInterface.isBackupServiceActive(0)) {
                                iBackupManagerAsInterface.restoreAtInstall(packageInstalledInfo.pkg.applicationInfo.packageName, i3);
                            } else {
                                z2 = false;
                            }
                        } catch (RemoteException e) {
                        } catch (Exception e2) {
                            Slog.e(PackageManagerService.TAG, "Exception trying to enqueue restore", e2);
                            z2 = false;
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Backup Manager not found!");
                    }
                    z2 = false;
                }
                if (!z2) {
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Log.v(PackageManagerService.TAG, "No restore - queue post-install for " + i3);
                    }
                    Trace.asyncTraceBegin(262144L, "postInstall", i3);
                    PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(9, i3, 0));
                }
            }
        });
    }

    void notifyFirstLaunch(final String str, final String str2, final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i2 = 0; i2 < PackageManagerService.this.mRunningInstalls.size(); i2++) {
                    PostInstallData postInstallDataValueAt = PackageManagerService.this.mRunningInstalls.valueAt(i2);
                    if (postInstallDataValueAt.res.returnCode == 1 && str.equals(postInstallDataValueAt.res.pkg.applicationInfo.packageName)) {
                        for (int i3 = 0; i3 < postInstallDataValueAt.res.newUsers.length; i3++) {
                            if (i == postInstallDataValueAt.res.newUsers[i3]) {
                                if (PackageManagerService.DEBUG_BACKUP) {
                                    Slog.i(PackageManagerService.TAG, "Package " + str + " being restored so deferring FIRST_LAUNCH");
                                    return;
                                }
                                return;
                            }
                        }
                    }
                }
                if (PackageManagerService.DEBUG_BACKUP) {
                    Slog.i(PackageManagerService.TAG, "Package " + str + " sending normal FIRST_LAUNCH");
                }
                boolean zIsInstantApp = PackageManagerService.this.isInstantApp(str, i);
                PackageManagerService.this.sendFirstLaunchBroadcast(str, str2, zIsInstantApp ? PackageManagerService.EMPTY_INT_ARRAY : new int[]{i}, zIsInstantApp ? new int[]{i} : PackageManagerService.EMPTY_INT_ARRAY);
            }
        });
    }

    private void sendFirstLaunchBroadcast(String str, String str2, int[] iArr, int[] iArr2) {
        sendPackageBroadcast("android.intent.action.PACKAGE_FIRST_LAUNCH", str, null, 0, str2, null, iArr, iArr2);
    }

    private abstract class HandlerParams {
        private static final int MAX_RETRIES = 4;
        private int mRetries = 0;
        private final UserHandle mUser;
        int traceCookie;
        String traceMethod;

        abstract void handleReturnCode();

        abstract void handleServiceError();

        abstract void handleStartCopy() throws RemoteException;

        HandlerParams(UserHandle userHandle) {
            this.mUser = userHandle;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        HandlerParams setTraceMethod(String str) {
            this.traceMethod = str;
            return this;
        }

        HandlerParams setTraceCookie(int i) {
            this.traceCookie = i;
            return this;
        }

        final boolean startCopy() {
            int i;
            boolean z = false;
            try {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.i(PackageManagerService.TAG, "startCopy " + this.mUser + ": " + this);
                }
                i = this.mRetries + 1;
                this.mRetries = i;
            } catch (RemoteException e) {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.i(PackageManagerService.TAG, "Posting install MCS_RECONNECT");
                }
                PackageManagerService.this.mHandler.sendEmptyMessage(10);
            }
            if (i > 4) {
                Slog.w(PackageManagerService.TAG, "Failed to invoke remote methods on default container service. Giving up");
                PackageManagerService.this.mHandler.sendEmptyMessage(11);
                handleServiceError();
                return false;
            }
            handleStartCopy();
            z = true;
            handleReturnCode();
            return z;
        }

        final void serviceError() {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.i(PackageManagerService.TAG, "serviceError");
            }
            handleServiceError();
            handleReturnCode();
        }
    }

    private static void clearDirectory(IMediaContainerService iMediaContainerService, File[] fileArr) {
        for (File file : fileArr) {
            try {
                iMediaContainerService.clearDirectory(file.getAbsolutePath());
            } catch (RemoteException e) {
            }
        }
    }

    static class OriginInfo {
        final boolean existing;
        final File file;
        final File resolvedFile;
        final String resolvedPath;
        final boolean staged;

        static OriginInfo fromNothing() {
            return new OriginInfo(null, false, false);
        }

        static OriginInfo fromUntrustedFile(File file) {
            return new OriginInfo(file, false, false);
        }

        static OriginInfo fromExistingFile(File file) {
            return new OriginInfo(file, false, true);
        }

        static OriginInfo fromStagedFile(File file) {
            return new OriginInfo(file, true, false);
        }

        private OriginInfo(File file, boolean z, boolean z2) {
            this.file = file;
            this.staged = z;
            this.existing = z2;
            if (file != null) {
                this.resolvedPath = file.getAbsolutePath();
                this.resolvedFile = file;
            } else {
                this.resolvedPath = null;
                this.resolvedFile = null;
            }
        }
    }

    static class MoveInfo {
        final int appId;
        final String dataAppName;
        final String fromUuid;
        final int moveId;
        final String packageName;
        final String seinfo;
        final int targetSdkVersion;
        final String toUuid;

        public MoveInfo(int i, String str, String str2, String str3, String str4, int i2, String str5, int i3) {
            this.moveId = i;
            this.fromUuid = str;
            this.toUuid = str2;
            this.packageName = str3;
            this.dataAppName = str4;
            this.appId = i2;
            this.seinfo = str5;
            this.targetSdkVersion = i3;
        }
    }

    static class VerificationInfo {
        public static final int NO_UID = -1;
        final int installerUid;
        final int originatingUid;
        final Uri originatingUri;
        final Uri referrer;

        VerificationInfo(Uri uri, Uri uri2, int i, int i2) {
            this.originatingUri = uri;
            this.referrer = uri2;
            this.originatingUid = i;
            this.installerUid = i2;
        }
    }

    class InstallParams extends HandlerParams {
        final String[] grantedRuntimePermissions;
        int installFlags;
        final int installReason;
        final String installerPackageName;
        private InstallArgs mArgs;
        private int mRet;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final String packageAbiOverride;
        final PackageParser.SigningDetails signingDetails;
        final VerificationInfo verificationInfo;
        final String volumeUuid;

        InstallParams(OriginInfo originInfo, MoveInfo moveInfo, IPackageInstallObserver2 iPackageInstallObserver2, int i, String str, String str2, VerificationInfo verificationInfo, UserHandle userHandle, String str3, String[] strArr, PackageParser.SigningDetails signingDetails, int i2) {
            super(userHandle);
            this.origin = originInfo;
            this.move = moveInfo;
            this.observer = iPackageInstallObserver2;
            this.installFlags = i;
            this.installerPackageName = str;
            this.volumeUuid = str2;
            this.verificationInfo = verificationInfo;
            this.packageAbiOverride = str3;
            this.grantedRuntimePermissions = strArr;
            this.signingDetails = signingDetails;
            this.installReason = i2;
        }

        public String toString() {
            return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.origin.file + "}";
        }

        private int installLocationPolicy(PackageInfoLite packageInfoLite) {
            PackageParser.Package r0;
            PackageSetting packageSetting;
            String str = packageInfoLite.packageName;
            int i = packageInfoLite.installLocation;
            boolean z = false;
            boolean z2 = (this.installFlags & 8) != 0;
            synchronized (PackageManagerService.this.mPackages) {
                PackageParser.Package r6 = PackageManagerService.this.mPackages.get(str);
                if (r6 == null && (packageSetting = PackageManagerService.this.mSettings.mPackages.get(str)) != null) {
                    r0 = packageSetting.pkg;
                } else {
                    r0 = r6;
                }
                if (r0 != null) {
                    boolean z3 = (this.installFlags & 128) != 0;
                    boolean z4 = (r0.applicationInfo.flags & 2) != 0;
                    if (z3 && (Build.IS_DEBUGGABLE || z4)) {
                        z = true;
                    }
                    if (!z) {
                        try {
                            PackageManagerService.checkDowngrade(r0, packageInfoLite);
                        } catch (PackageManagerException e) {
                            Slog.w(PackageManagerService.TAG, "Downgrade detected: " + e.getMessage());
                            return -7;
                        }
                    }
                }
                if (r6 != null) {
                    if ((this.installFlags & 2) != 0) {
                        if ((r6.applicationInfo.flags & 1) != 0) {
                            if (!z2) {
                                return 1;
                            }
                            Slog.w(PackageManagerService.TAG, "Cannot install update to system app on sdcard");
                            return -3;
                        }
                        if (z2) {
                            return 2;
                        }
                        if (i == 1) {
                            return 1;
                        }
                        if (i != 2) {
                            return PackageManagerService.isExternal(r6) ? 2 : 1;
                        }
                    } else {
                        return -4;
                    }
                }
                if (z2) {
                    return 2;
                }
                return packageInfoLite.recommendedInstallLocation;
            }
        }

        @Override
        public void handleStartCopy() throws RemoteException {
            PackageInfoLite minimalPackageInfo;
            int i;
            PackageInfoLite minimalPackageInfo2;
            if (this.origin.staged) {
                if (this.origin.file == null) {
                    throw new IllegalStateException("Invalid stage location");
                }
                this.installFlags |= 16;
                this.installFlags &= -9;
            }
            boolean z = (this.installFlags & 8) != 0;
            boolean z2 = (this.installFlags & 16) != 0;
            boolean z3 = (this.installFlags & 2048) != 0;
            int iCopyApk = -19;
            if (z2 && z) {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing on both internal and external");
            } else {
                if (!z || !z3) {
                    minimalPackageInfo = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                    if (PackageManagerService.DEBUG_INSTANT && z3) {
                        Slog.v(PackageManagerService.TAG, "pkgLite for install: " + minimalPackageInfo);
                    }
                    if (!this.origin.staged && minimalPackageInfo.recommendedInstallLocation == -1) {
                        try {
                            PackageManagerService.this.mInstaller.freeCache(null, PackageManagerService.this.mContainerService.calculateInstalledSize(this.origin.resolvedPath, this.packageAbiOverride) + StorageManager.from(PackageManagerService.this.mContext).getStorageLowBytes(Environment.getDataDirectory()), 0L, 0);
                            minimalPackageInfo2 = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                        } catch (Installer.InstallerException e) {
                            Slog.w(PackageManagerService.TAG, "Failed to free cache", e);
                            minimalPackageInfo2 = minimalPackageInfo;
                        }
                        if (minimalPackageInfo2.recommendedInstallLocation == -6) {
                            minimalPackageInfo2.recommendedInstallLocation = -1;
                        }
                        minimalPackageInfo = minimalPackageInfo2;
                    }
                    i = 1;
                    if (i != 1) {
                        iCopyApk = i;
                    } else {
                        int i2 = minimalPackageInfo.recommendedInstallLocation;
                        if (i2 != -3) {
                            if (i2 == -4) {
                                iCopyApk = -1;
                            } else if (i2 == -1) {
                                iCopyApk = -4;
                            } else if (i2 == -2) {
                                iCopyApk = -2;
                            } else if (i2 == -6) {
                                iCopyApk = -3;
                            } else if (i2 == -5) {
                                iCopyApk = -20;
                            } else {
                                this.installFlags = PackageManagerService.sPmsExt.customizeInstallPkgFlags(this.installFlags, minimalPackageInfo, PackageManagerService.this.mSettings.mPackages, getUser());
                                int iInstallLocationPolicy = installLocationPolicy(minimalPackageInfo);
                                if (iInstallLocationPolicy == -7) {
                                    iCopyApk = -25;
                                } else {
                                    if (!z && !z2) {
                                        if (iInstallLocationPolicy == 2) {
                                            this.installFlags |= 8;
                                            this.installFlags &= -17;
                                        } else if (iInstallLocationPolicy == 3) {
                                            if (PackageManagerService.DEBUG_INSTANT) {
                                                Slog.v(PackageManagerService.TAG, "...setting INSTALL_EPHEMERAL install flag");
                                            }
                                            this.installFlags |= 2048;
                                            this.installFlags &= -25;
                                        } else {
                                            this.installFlags |= 16;
                                            this.installFlags &= -9;
                                        }
                                    }
                                    iCopyApk = i;
                                }
                            }
                        }
                    }
                    InstallArgs installArgsCreateInstallArgs = PackageManagerService.this.createInstallArgs(this);
                    this.mArgs = installArgsCreateInstallArgs;
                    if (iCopyApk == 1) {
                        UserHandle user = getUser();
                        if (user == UserHandle.ALL) {
                            user = UserHandle.SYSTEM;
                        }
                        UserHandle userHandle = user;
                        int packageUid = PackageManagerService.this.mRequiredVerifierPackage == null ? -1 : PackageManagerService.this.getPackageUid(PackageManagerService.this.mRequiredVerifierPackage, 268435456, userHandle.getIdentifier());
                        int i3 = this.verificationInfo == null ? -1 : this.verificationInfo.installerUid;
                        if (this.origin.existing || packageUid == -1 || !PackageManagerService.this.isVerificationEnabled(userHandle.getIdentifier(), this.installFlags, i3)) {
                            iCopyApk = installArgsCreateInstallArgs.copyApk(PackageManagerService.this.mContainerService, true);
                        } else {
                            Intent intent = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                            intent.addFlags(268435456);
                            intent.setDataAndType(Uri.fromFile(new File(this.origin.resolvedPath)), PackageManagerService.PACKAGE_MIME_TYPE);
                            intent.addFlags(1);
                            List listQueryIntentReceiversInternal = PackageManagerService.this.queryIntentReceiversInternal(intent, PackageManagerService.PACKAGE_MIME_TYPE, 0, userHandle.getIdentifier(), false);
                            if (PackageManagerService.DEBUG_VERIFY) {
                                Slog.d(PackageManagerService.TAG, "Found " + listQueryIntentReceiversInternal.size() + " verifiers for intent " + intent.toString() + " with " + minimalPackageInfo.verifiers.length + " optional verifiers");
                            }
                            final int iAccess$4408 = PackageManagerService.access$4408(PackageManagerService.this);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_ID", iAccess$4408);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", this.installerPackageName);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_INSTALL_FLAGS", this.installFlags);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_PACKAGE_NAME", minimalPackageInfo.packageName);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_VERSION_CODE", minimalPackageInfo.versionCode);
                            intent.putExtra("android.content.pm.extra.VERIFICATION_LONG_VERSION_CODE", minimalPackageInfo.getLongVersionCode());
                            if (this.verificationInfo != null) {
                                if (this.verificationInfo.originatingUri != null) {
                                    intent.putExtra("android.intent.extra.ORIGINATING_URI", this.verificationInfo.originatingUri);
                                }
                                if (this.verificationInfo.referrer != null) {
                                    intent.putExtra("android.intent.extra.REFERRER", this.verificationInfo.referrer);
                                }
                                if (this.verificationInfo.originatingUid >= 0) {
                                    intent.putExtra("android.intent.extra.ORIGINATING_UID", this.verificationInfo.originatingUid);
                                }
                                if (this.verificationInfo.installerUid >= 0) {
                                    intent.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", this.verificationInfo.installerUid);
                                }
                            }
                            PackageVerificationState packageVerificationState = new PackageVerificationState(packageUid, installArgsCreateInstallArgs);
                            PackageManagerService.this.mPendingVerification.append(iAccess$4408, packageVerificationState);
                            List listMatchVerifiers = PackageManagerService.this.matchVerifiers(minimalPackageInfo, listQueryIntentReceiversInternal, packageVerificationState);
                            DeviceIdleController.LocalService deviceIdleController = PackageManagerService.this.getDeviceIdleController();
                            long verificationTimeout = PackageManagerService.this.getVerificationTimeout();
                            if (listMatchVerifiers != null) {
                                int size = listMatchVerifiers.size();
                                if (size == 0) {
                                    Slog.i(PackageManagerService.TAG, "Additional verifiers required, but none installed.");
                                    iCopyApk = -22;
                                } else {
                                    for (int i4 = 0; i4 < size; i4++) {
                                        ComponentName componentName = (ComponentName) listMatchVerifiers.get(i4);
                                        deviceIdleController.addPowerSaveTempWhitelistApp(Process.myUid(), componentName.getPackageName(), verificationTimeout, userHandle.getIdentifier(), false, "package verifier");
                                        Intent intent2 = new Intent(intent);
                                        intent2.setComponent(componentName);
                                        PackageManagerService.this.mContext.sendBroadcastAsUser(intent2, userHandle);
                                    }
                                }
                            }
                            ComponentName componentNameMatchComponentForVerifier = PackageManagerService.this.matchComponentForVerifier(PackageManagerService.this.mRequiredVerifierPackage, listQueryIntentReceiversInternal);
                            if (iCopyApk == 1 && PackageManagerService.this.mRequiredVerifierPackage != null) {
                                Trace.asyncTraceBegin(262144L, "verification", iAccess$4408);
                                intent.setComponent(componentNameMatchComponentForVerifier);
                                deviceIdleController.addPowerSaveTempWhitelistApp(Process.myUid(), PackageManagerService.this.mRequiredVerifierPackage, verificationTimeout, userHandle.getIdentifier(), false, "package verifier");
                                PackageManagerService.this.mContext.sendOrderedBroadcastAsUser(intent, userHandle, "android.permission.PACKAGE_VERIFICATION_AGENT", new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent3) {
                                        Message messageObtainMessage = PackageManagerService.this.mHandler.obtainMessage(16);
                                        messageObtainMessage.arg1 = iAccess$4408;
                                        PackageManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage, PackageManagerService.this.getVerificationTimeout());
                                    }
                                }, null, 0, null, null);
                                this.mArgs = null;
                            }
                        }
                    }
                    this.mRet = iCopyApk;
                }
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing ephemeral on external");
            }
            i = -19;
            minimalPackageInfo = null;
            if (i != 1) {
            }
            InstallArgs installArgsCreateInstallArgs2 = PackageManagerService.this.createInstallArgs(this);
            this.mArgs = installArgsCreateInstallArgs2;
            if (iCopyApk == 1) {
            }
            this.mRet = iCopyApk;
        }

        @Override
        void handleReturnCode() {
            if (this.mArgs != null) {
                PackageManagerService.this.processPendingInstall(this.mArgs, this.mRet);
            }
        }

        @Override
        void handleServiceError() {
            this.mArgs = PackageManagerService.this.createInstallArgs(this);
            this.mRet = RequestStatus.SYS_ETIMEDOUT;
        }
    }

    private InstallArgs createInstallArgs(InstallParams installParams) {
        if (installParams.move != null) {
            return new MoveInstallArgs(installParams);
        }
        return new FileInstallArgs(installParams);
    }

    private InstallArgs createInstallArgsForExisting(int i, String str, String str2, String[] strArr) {
        return new FileInstallArgs(str, str2, strArr);
    }

    static abstract class InstallArgs {
        final String abiOverride;
        final int installFlags;
        final String[] installGrantPermissions;
        final int installReason;
        final String installerPackageName;
        String[] instructionSets;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final PackageParser.SigningDetails signingDetails;
        final int traceCookie;
        final String traceMethod;
        final UserHandle user;
        final String volumeUuid;

        abstract void cleanUpResourcesLI();

        abstract int copyApk(IMediaContainerService iMediaContainerService, boolean z) throws RemoteException;

        abstract boolean doPostDeleteLI(boolean z);

        abstract int doPostInstall(int i, int i2);

        abstract int doPreInstall(int i);

        abstract boolean doRename(int i, PackageParser.Package r2, String str);

        abstract String getCodePath();

        abstract String getResourcePath();

        InstallArgs(OriginInfo originInfo, MoveInfo moveInfo, IPackageInstallObserver2 iPackageInstallObserver2, int i, String str, String str2, UserHandle userHandle, String[] strArr, String str3, String[] strArr2, String str4, int i2, PackageParser.SigningDetails signingDetails, int i3) {
            this.origin = originInfo;
            this.move = moveInfo;
            this.installFlags = i;
            this.observer = iPackageInstallObserver2;
            this.installerPackageName = str;
            this.volumeUuid = str2;
            this.user = userHandle;
            this.instructionSets = strArr;
            this.abiOverride = str3;
            this.installGrantPermissions = strArr2;
            this.traceMethod = str4;
            this.traceCookie = i2;
            this.signingDetails = signingDetails;
            this.installReason = i3;
        }

        int doPreCopy() {
            return 1;
        }

        int doPostCopy(int i) {
            return 1;
        }

        protected boolean isFwdLocked() {
            return (this.installFlags & 1) != 0;
        }

        protected boolean isExternalAsec() {
            return (this.installFlags & 8) != 0;
        }

        protected boolean isEphemeral() {
            return (this.installFlags & 2048) != 0;
        }

        UserHandle getUser() {
            return this.user;
        }
    }

    void removeDexFiles(List<String> list, String[] strArr) {
        if (!list.isEmpty()) {
            if (strArr == null) {
                throw new IllegalStateException("instructionSet == null");
            }
            String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(strArr);
            for (String str : list) {
                for (String str2 : dexCodeInstructionSets) {
                    try {
                        this.mInstaller.rmdex(str, str2);
                    } catch (Installer.InstallerException e) {
                    }
                }
            }
        }
    }

    class FileInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        FileInstallArgs(InstallParams installParams) {
            super(installParams.origin, installParams.move, installParams.observer, installParams.installFlags, installParams.installerPackageName, installParams.volumeUuid, installParams.getUser(), null, installParams.packageAbiOverride, installParams.grantedRuntimePermissions, installParams.traceMethod, installParams.traceCookie, installParams.signingDetails, installParams.installReason);
            if (isFwdLocked()) {
                throw new IllegalArgumentException("Forward locking only supported in ASEC");
            }
        }

        FileInstallArgs(String str, String str2, String[] strArr) {
            super(OriginInfo.fromNothing(), null, null, 0, null, null, null, strArr, null, null, null, 0, PackageParser.SigningDetails.UNKNOWN, 0);
            this.codeFile = str != null ? new File(str) : null;
            this.resourceFile = str2 != null ? new File(str2) : null;
        }

        @Override
        int copyApk(IMediaContainerService iMediaContainerService, boolean z) throws RemoteException {
            Trace.traceBegin(262144L, "copyApk");
            try {
                return doCopyApk(iMediaContainerService, z);
            } finally {
                Trace.traceEnd(262144L);
            }
        }

        private int doCopyApk(IMediaContainerService iMediaContainerService, boolean z) throws Throwable {
            AutoCloseable autoCloseableCreate;
            if (this.origin.staged) {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.d(PackageManagerService.TAG, this.origin.file + " already staged; skipping copy");
                }
                this.codeFile = this.origin.file;
                this.resourceFile = this.origin.file;
                return 1;
            }
            try {
                File fileAllocateStageDirLegacy = PackageManagerService.this.mInstallerService.allocateStageDirLegacy(this.volumeUuid, (this.installFlags & 2048) != 0);
                this.codeFile = fileAllocateStageDirLegacy;
                this.resourceFile = fileAllocateStageDirLegacy;
                int iCopyPackage = iMediaContainerService.copyPackage(this.origin.file.getAbsolutePath(), new IParcelFileDescriptorFactory.Stub() {
                    public ParcelFileDescriptor open(String str, int i) throws RemoteException {
                        if (!FileUtils.isValidExtFilename(str)) {
                            throw new IllegalArgumentException("Invalid filename: " + str);
                        }
                        try {
                            File file = new File(FileInstallArgs.this.codeFile, str);
                            FileDescriptor fileDescriptorOpen = Os.open(file.getAbsolutePath(), OsConstants.O_RDWR | OsConstants.O_CREAT, 420);
                            Os.chmod(file.getAbsolutePath(), 420);
                            return new ParcelFileDescriptor(fileDescriptorOpen);
                        } catch (ErrnoException e) {
                            throw new RemoteException("Failed to open: " + e.getMessage());
                        }
                    }
                });
                if (iCopyPackage != 1) {
                    Slog.e(PackageManagerService.TAG, "Failed to copy package");
                    return iCopyPackage;
                }
                File file = new File(this.codeFile, "lib");
                AutoCloseable autoCloseable = null;
                try {
                    try {
                        autoCloseableCreate = NativeLibraryHelper.Handle.create(this.codeFile);
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    int iCopyNativeBinariesWithOverride = NativeLibraryHelper.copyNativeBinariesWithOverride(autoCloseableCreate, file, this.abiOverride);
                    IoUtils.closeQuietly(autoCloseableCreate);
                    return iCopyNativeBinariesWithOverride;
                } catch (IOException e2) {
                    e = e2;
                    autoCloseable = autoCloseableCreate;
                    Slog.e(PackageManagerService.TAG, "Copying native libraries failed", e);
                    IoUtils.closeQuietly(autoCloseable);
                    return RequestStatus.SYS_ETIMEDOUT;
                } catch (Throwable th2) {
                    th = th2;
                    autoCloseable = autoCloseableCreate;
                    IoUtils.closeQuietly(autoCloseable);
                    throw th;
                }
            } catch (IOException e3) {
                Slog.w(PackageManagerService.TAG, "Failed to create copy file: " + e3);
                return -4;
            }
        }

        @Override
        int doPreInstall(int i) {
            if (i != 1) {
                cleanUp();
            }
            return i;
        }

        @Override
        boolean doRename(int i, PackageParser.Package r7, String str) {
            if (i != 1) {
                cleanUp();
                return false;
            }
            File parentFile = this.codeFile.getParentFile();
            File file = this.codeFile;
            File nextCodePath = PackageManagerService.this.getNextCodePath(parentFile, r7.packageName);
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d(PackageManagerService.TAG, "Renaming " + file + " to " + nextCodePath);
            }
            try {
                Os.rename(file.getAbsolutePath(), nextCodePath.getAbsolutePath());
                if (!SELinux.restoreconRecursive(nextCodePath)) {
                    Slog.w(PackageManagerService.TAG, "Failed to restorecon");
                    return false;
                }
                this.codeFile = nextCodePath;
                this.resourceFile = nextCodePath;
                try {
                    r7.setCodePath(nextCodePath.getCanonicalPath());
                    r7.setBaseCodePath(FileUtils.rewriteAfterRename(file, nextCodePath, r7.baseCodePath));
                    r7.setSplitCodePaths(FileUtils.rewriteAfterRename(file, nextCodePath, r7.splitCodePaths));
                    r7.setApplicationVolumeUuid(r7.volumeUuid);
                    r7.setApplicationInfoCodePath(r7.codePath);
                    r7.setApplicationInfoBaseCodePath(r7.baseCodePath);
                    r7.setApplicationInfoSplitCodePaths(r7.splitCodePaths);
                    r7.setApplicationInfoResourcePath(r7.codePath);
                    r7.setApplicationInfoBaseResourcePath(r7.baseCodePath);
                    r7.setApplicationInfoSplitResourcePaths(r7.splitCodePaths);
                    return true;
                } catch (IOException e) {
                    Slog.e(PackageManagerService.TAG, "Failed to get path: " + nextCodePath, e);
                    return false;
                }
            } catch (ErrnoException e2) {
                Slog.w(PackageManagerService.TAG, "Failed to rename", e2);
                return false;
            }
        }

        @Override
        int doPostInstall(int i, int i2) {
            if (i != 1) {
                cleanUp();
            }
            return i;
        }

        @Override
        String getCodePath() {
            if (this.codeFile != null) {
                return this.codeFile.getAbsolutePath();
            }
            return null;
        }

        @Override
        String getResourcePath() {
            if (this.resourceFile != null) {
                return this.resourceFile.getAbsolutePath();
            }
            return null;
        }

        private boolean cleanUp() {
            if (this.codeFile == null || !this.codeFile.exists()) {
                return false;
            }
            PackageManagerService.this.removeCodePathLI(this.codeFile);
            if (this.resourceFile != null && !FileUtils.contains(this.codeFile, this.resourceFile)) {
                this.resourceFile.delete();
                return true;
            }
            return true;
        }

        @Override
        void cleanUpResourcesLI() {
            List<String> allCodePaths = Collections.EMPTY_LIST;
            if (this.codeFile != null && this.codeFile.exists()) {
                try {
                    allCodePaths = PackageParser.parsePackageLite(this.codeFile, 0).getAllCodePaths();
                } catch (PackageParser.PackageParserException e) {
                }
            }
            cleanUp();
            PackageManagerService.this.removeDexFiles(allCodePaths, this.instructionSets);
        }

        @Override
        boolean doPostDeleteLI(boolean z) {
            cleanUpResourcesLI();
            return true;
        }
    }

    private static void maybeThrowExceptionForMultiArchCopy(String str, int i) throws PackageManagerException {
        if (i < 0 && i != -114 && i != -113) {
            throw new PackageManagerException(i, str);
        }
    }

    static String cidFromCodePath(String str) {
        int iLastIndexOf = str.lastIndexOf(SliceClientPermissions.SliceAuthority.DELIMITER);
        String strSubstring = str.substring(0, iLastIndexOf);
        return strSubstring.substring(strSubstring.lastIndexOf(SliceClientPermissions.SliceAuthority.DELIMITER) + 1, iLastIndexOf);
    }

    class MoveInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        MoveInstallArgs(InstallParams installParams) {
            super(installParams.origin, installParams.move, installParams.observer, installParams.installFlags, installParams.installerPackageName, installParams.volumeUuid, installParams.getUser(), null, installParams.packageAbiOverride, installParams.grantedRuntimePermissions, installParams.traceMethod, installParams.traceCookie, installParams.signingDetails, installParams.installReason);
        }

        @Override
        int copyApk(IMediaContainerService iMediaContainerService, boolean z) {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d(PackageManagerService.TAG, "Moving " + this.move.packageName + " from " + this.move.fromUuid + " to " + this.move.toUuid);
            }
            synchronized (PackageManagerService.this.mInstaller) {
                try {
                    PackageManagerService.this.mInstaller.moveCompleteApp(this.move.fromUuid, this.move.toUuid, this.move.packageName, this.move.dataAppName, this.move.appId, this.move.seinfo, this.move.targetSdkVersion);
                } catch (Installer.InstallerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move app", e);
                    return RequestStatus.SYS_ETIMEDOUT;
                }
            }
            this.codeFile = new File(Environment.getDataAppDirectory(this.move.toUuid), this.move.dataAppName);
            this.resourceFile = this.codeFile;
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d(PackageManagerService.TAG, "codeFile after move is " + this.codeFile);
                return 1;
            }
            return 1;
        }

        @Override
        int doPreInstall(int i) {
            if (i != 1) {
                cleanUp(this.move.toUuid);
            }
            return i;
        }

        @Override
        boolean doRename(int i, PackageParser.Package r2, String str) {
            if (i != 1) {
                cleanUp(this.move.toUuid);
                return false;
            }
            r2.setApplicationVolumeUuid(r2.volumeUuid);
            r2.setApplicationInfoCodePath(r2.codePath);
            r2.setApplicationInfoBaseCodePath(r2.baseCodePath);
            r2.setApplicationInfoSplitCodePaths(r2.splitCodePaths);
            r2.setApplicationInfoResourcePath(r2.codePath);
            r2.setApplicationInfoBaseResourcePath(r2.baseCodePath);
            r2.setApplicationInfoSplitResourcePaths(r2.splitCodePaths);
            return true;
        }

        @Override
        int doPostInstall(int i, int i2) {
            if (i == 1) {
                cleanUp(this.move.fromUuid);
            } else {
                cleanUp(this.move.toUuid);
            }
            return i;
        }

        @Override
        String getCodePath() {
            if (this.codeFile != null) {
                return this.codeFile.getAbsolutePath();
            }
            return null;
        }

        @Override
        String getResourcePath() {
            if (this.resourceFile != null) {
                return this.resourceFile.getAbsolutePath();
            }
            return null;
        }

        private boolean cleanUp(String str) {
            File file = new File(Environment.getDataAppDirectory(str), this.move.dataAppName);
            Slog.d(PackageManagerService.TAG, "Cleaning up " + this.move.packageName + " on " + str);
            int[] userIds = PackageManagerService.sUserManager.getUserIds();
            synchronized (PackageManagerService.this.mInstallLock) {
                for (int i : userIds) {
                    try {
                        PackageManagerService.this.mInstaller.destroyAppData(str, this.move.packageName, i, 3, 0L);
                    } catch (Installer.InstallerException e) {
                        Slog.w(PackageManagerService.TAG, String.valueOf(e));
                    }
                }
                PackageManagerService.this.removeCodePathLI(file);
            }
            return true;
        }

        @Override
        void cleanUpResourcesLI() {
            throw new UnsupportedOperationException();
        }

        @Override
        boolean doPostDeleteLI(boolean z) {
            throw new UnsupportedOperationException();
        }
    }

    static String getAsecPackageName(String str) {
        int iLastIndexOf = str.lastIndexOf(INSTALL_PACKAGE_SUFFIX);
        if (iLastIndexOf == -1) {
            return str;
        }
        return str.substring(0, iLastIndexOf);
    }

    private static String getNextCodePath(String str, String str2, String str3) {
        String strSubstring;
        int i;
        int i2 = 1;
        if (str != null) {
            if (str3 != null && str.endsWith(str3)) {
                str = str.substring(0, str.length() - str3.length());
            }
            int iLastIndexOf = str.lastIndexOf(str2);
            if (iLastIndexOf != -1 && (strSubstring = str.substring(iLastIndexOf + str2.length())) != null) {
                if (strSubstring.startsWith(INSTALL_PACKAGE_SUFFIX)) {
                    strSubstring = strSubstring.substring(INSTALL_PACKAGE_SUFFIX.length());
                }
                try {
                    int i3 = Integer.parseInt(strSubstring);
                    if (i3 <= 1) {
                        i = i3 + 1;
                    } else {
                        i = i3 - 1;
                    }
                    i2 = i;
                } catch (NumberFormatException e) {
                }
            }
        }
        return str2 + (INSTALL_PACKAGE_SUFFIX + Integer.toString(i2));
    }

    private File getNextCodePath(File file, String str) {
        File file2;
        SecureRandom secureRandom = new SecureRandom();
        byte[] bArr = new byte[16];
        do {
            secureRandom.nextBytes(bArr);
            file2 = new File(file, str + INSTALL_PACKAGE_SUFFIX + Base64.encodeToString(bArr, 10));
        } while (file2.exists());
        return file2;
    }

    public static String deriveCodePathName(String str) {
        if (str == null) {
            return null;
        }
        File file = new File(str);
        String name = file.getName();
        if (file.isDirectory()) {
            return name;
        }
        if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            return name.substring(0, name.lastIndexOf(46));
        }
        Slog.w(TAG, "Odd, " + str + " doesn't look like an APK");
        return null;
    }

    static class PackageInstalledInfo {
        ArrayMap<String, PackageInstalledInfo> addedChildPackages;
        String installerPackageName;
        String name;
        int[] newUsers;
        String origPackage;
        String origPermission;
        int[] origUsers;
        PackageParser.Package pkg;
        PackageRemovedInfo removedInfo;
        int returnCode;
        String returnMsg;
        int uid;

        PackageInstalledInfo() {
        }

        public void setError(int i, String str) {
            setReturnCode(i);
            setReturnMessage(str);
            Slog.w(PackageManagerService.TAG, str);
        }

        public void setError(String str, PackageParser.PackageParserException packageParserException) {
            setReturnCode(packageParserException.error);
            setReturnMessage(ExceptionUtils.getCompleteMessage(str, packageParserException));
            int size = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                this.addedChildPackages.valueAt(i).setError(str, packageParserException);
            }
            Slog.w(PackageManagerService.TAG, str, packageParserException);
        }

        public void setError(String str, PackageManagerException packageManagerException) {
            this.returnCode = packageManagerException.error;
            setReturnMessage(ExceptionUtils.getCompleteMessage(str, packageManagerException));
            int size = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                this.addedChildPackages.valueAt(i).setError(str, packageManagerException);
            }
            Slog.w(PackageManagerService.TAG, str, packageManagerException);
        }

        public void setReturnCode(int i) {
            this.returnCode = i;
            int size = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i2 = 0; i2 < size; i2++) {
                this.addedChildPackages.valueAt(i2).returnCode = i;
            }
        }

        private void setReturnMessage(String str) {
            this.returnMsg = str;
            int size = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                this.addedChildPackages.valueAt(i).returnMsg = str;
            }
        }
    }

    private void installNewPackageLIF(PackageParser.Package r16, int i, int i2, UserHandle userHandle, String str, String str2, PackageInstalledInfo packageInstalledInfo, int i3) throws Throwable {
        Trace.traceBegin(262144L, "installNewPackage");
        String str3 = r16.packageName;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "installNewPackageLI: " + r16);
        }
        synchronized (this.mPackages) {
            String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(str3);
            if (renamedPackageLPr != null) {
                packageInstalledInfo.setError(-1, "Attempt to re-install " + str3 + " without first uninstalling package running as " + renamedPackageLPr);
                return;
            }
            if (this.mPackages.containsKey(str3)) {
                packageInstalledInfo.setError(-1, "Attempt to re-install " + str3 + " without first uninstalling.");
                return;
            }
            try {
                PackageParser.Package packageScanPackageTracedLI = scanPackageTracedLI(r16, i, i2, System.currentTimeMillis(), userHandle);
                updateSettingsLI(packageScanPackageTracedLI, str, null, packageInstalledInfo, userHandle, i3);
                if (packageInstalledInfo.returnCode == 1) {
                    prepareAppDataAfterInstallLIF(packageScanPackageTracedLI);
                } else {
                    deletePackageLIF(str3, UserHandle.ALL, false, null, 1, packageInstalledInfo.removedInfo, true, null);
                }
            } catch (PackageManagerException e) {
                packageInstalledInfo.setError("Package couldn't be installed in " + r16.codePath, e);
            }
            Trace.traceEnd(262144L);
        }
    }

    private static void updateDigest(MessageDigest messageDigest, File file) throws Exception {
        DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), messageDigest);
        do {
            Throwable th = null;
            try {
            } finally {
                $closeResource(th, digestInputStream);
            }
        } while (digestInputStream.read() != -1);
    }

    private void replacePackageLIF(PackageParser.Package r20, int i, int i2, UserHandle userHandle, String str, PackageInstalledInfo packageInstalledInfo, int i3) throws Throwable {
        int i4;
        int size;
        boolean z;
        PackageInstalledInfo packageInstalledInfo2;
        boolean z2 = (i2 & 16384) != 0;
        String str2 = r20.packageName;
        synchronized (this.mPackages) {
            PackageParser.Package r8 = this.mPackages.get(str2);
            if (DEBUG_INSTALL) {
                Slog.d(TAG, "replacePackageLI: new=" + r20 + ", old=" + r8);
            }
            boolean z3 = r8.applicationInfo.targetSdkVersion == 10000;
            boolean z4 = r20.applicationInfo.targetSdkVersion == 10000;
            if (!z3 || z4) {
                i4 = i;
            } else {
                i4 = i;
                if ((i4 & 128) == 0) {
                    Slog.w(TAG, "Can't install package targeting released sdk");
                    packageInstalledInfo.setReturnCode(-7);
                    return;
                }
            }
            PackageSetting packageSetting = this.mSettings.mPackages.get(str2);
            KeySetManagerService keySetManagerService = this.mSettings.mKeySetManagerService;
            if (keySetManagerService.shouldCheckUpgradeKeySetLocked(packageSetting, i2)) {
                if (!keySetManagerService.checkUpgradeKeySetLocked(packageSetting, r20)) {
                    packageInstalledInfo.setError(-7, "New package not signed by keys specified by upgrade-keysets: " + str2);
                    return;
                }
            } else if (!r20.mSigningDetails.checkCapability(r8.mSigningDetails, 1) && !r8.mSigningDetails.checkCapability(r20.mSigningDetails, 8)) {
                packageInstalledInfo.setError(-7, "New package has a different signature: " + str2);
                return;
            }
            if (r8.restrictUpdateHash != null && r8.isSystem()) {
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
                    updateDigest(messageDigest, new File(r20.baseCodePath));
                    if (!ArrayUtils.isEmpty(r20.splitCodePaths)) {
                        for (String str3 : r20.splitCodePaths) {
                            updateDigest(messageDigest, new File(str3));
                        }
                    }
                    if (!Arrays.equals(r8.restrictUpdateHash, messageDigest.digest())) {
                        packageInstalledInfo.setError(-2, "New package fails restrict-update check: " + str2);
                        return;
                    }
                    r20.restrictUpdateHash = r8.restrictUpdateHash;
                } catch (IOException | NoSuchAlgorithmException e) {
                    packageInstalledInfo.setError(-2, "Could not compute hash: " + str2);
                    return;
                }
            }
            String parentOrChildPackageChangedSharedUser = getParentOrChildPackageChangedSharedUser(r8, r20);
            if (parentOrChildPackageChangedSharedUser != null) {
                packageInstalledInfo.setError(-8, "Package " + parentOrChildPackageChangedSharedUser + " tried to change user " + r8.mSharedUserId);
                return;
            }
            boolean z5 = r8.applicationInfo.secondaryCpuAbi != null;
            boolean z6 = r20.applicationInfo.secondaryCpuAbi != null;
            if (isSystemApp(r8) && z5 && !z6) {
                packageInstalledInfo.setError(-7, "Update to package " + str2 + " doesn't support multi arch");
                return;
            }
            int[] userIds = sUserManager.getUserIds();
            int[] iArrQueryInstalledUsers = packageSetting.queryInstalledUsers(userIds, true);
            if (z2) {
                if (userHandle == null || userHandle.getIdentifier() == -1) {
                    for (int i5 : userIds) {
                        if (!packageSetting.getInstantApp(i5)) {
                            Slog.w(TAG, "Can't replace full app with instant app: " + str2 + " for user: " + i5);
                            packageInstalledInfo.setReturnCode(-116);
                            return;
                        }
                    }
                } else if (!packageSetting.getInstantApp(userHandle.getIdentifier())) {
                    Slog.w(TAG, "Can't replace full app with instant app: " + str2 + " for user: " + userHandle.getIdentifier());
                    packageInstalledInfo.setReturnCode(-116);
                    return;
                }
            }
            packageInstalledInfo.removedInfo = new PackageRemovedInfo(this);
            packageInstalledInfo.removedInfo.uid = r8.applicationInfo.uid;
            packageInstalledInfo.removedInfo.removedPackage = r8.packageName;
            packageInstalledInfo.removedInfo.installerPackageName = packageSetting.installerPackageName;
            packageInstalledInfo.removedInfo.isStaticSharedLib = r20.staticSharedLibName != null;
            packageInstalledInfo.removedInfo.isUpdate = true;
            packageInstalledInfo.removedInfo.origUsers = iArrQueryInstalledUsers;
            packageInstalledInfo.removedInfo.installReasons = new SparseArray<>(iArrQueryInstalledUsers.length);
            for (int i6 : iArrQueryInstalledUsers) {
                packageInstalledInfo.removedInfo.installReasons.put(i6, Integer.valueOf(packageSetting.getInstallReason(i6)));
            }
            if (r8.childPackages != null) {
                size = r8.childPackages.size();
            } else {
                size = 0;
            }
            for (int i7 = 0; i7 < size; i7++) {
                PackageParser.Package r4 = (PackageParser.Package) r8.childPackages.get(i7);
                PackageSetting packageLPr = this.mSettings.getPackageLPr(r4.packageName);
                if (packageInstalledInfo.addedChildPackages == null || (packageInstalledInfo2 = packageInstalledInfo.addedChildPackages.get(r4.packageName)) == null) {
                    z = false;
                } else {
                    packageInstalledInfo2.removedInfo.uid = r4.applicationInfo.uid;
                    packageInstalledInfo2.removedInfo.removedPackage = r4.packageName;
                    if (packageLPr != null) {
                        packageInstalledInfo2.removedInfo.installerPackageName = packageLPr.installerPackageName;
                    }
                    packageInstalledInfo2.removedInfo.isUpdate = true;
                    packageInstalledInfo2.removedInfo.installReasons = packageInstalledInfo.removedInfo.installReasons;
                    z = true;
                }
                if (!z) {
                    PackageRemovedInfo packageRemovedInfo = new PackageRemovedInfo(this);
                    packageRemovedInfo.removedPackage = r4.packageName;
                    if (packageLPr != null) {
                        packageRemovedInfo.installerPackageName = packageLPr.installerPackageName;
                    }
                    packageRemovedInfo.isUpdate = false;
                    packageRemovedInfo.dataRemoved = true;
                    synchronized (this.mPackages) {
                        if (packageLPr != null) {
                            try {
                                packageRemovedInfo.origUsers = packageLPr.queryInstalledUsers(userIds, true);
                            } finally {
                            }
                        }
                    }
                    if (packageInstalledInfo.removedInfo.removedChildPackages == null) {
                        packageInstalledInfo.removedInfo.removedChildPackages = new ArrayMap<>();
                    }
                    packageInstalledInfo.removedInfo.removedChildPackages.put(r4.packageName, packageRemovedInfo);
                }
            }
            if (isSystemApp(r8)) {
                replaceSystemPackageLIF(r8, r20, i4, 131072 | i2 | ((r8.applicationInfo.privateFlags & 8) != 0 ? 262144 : 0) | ((r8.applicationInfo.privateFlags & 131072) != 0 ? 524288 : 0) | ((r8.applicationInfo.privateFlags & 262144) != 0 ? 1048576 : 0) | ((r8.applicationInfo.privateFlags & 524288) != 0 ? 2097152 : 0), userHandle, userIds, str, packageInstalledInfo, i3);
                return;
            }
            replaceNonSystemPackageLIF(r8, r20, i4, i2, userHandle, userIds, str, packageInstalledInfo, i3);
        }
    }

    private void replaceNonSystemPackageLIF(PackageParser.Package r23, PackageParser.Package r24, int i, int i2, UserHandle userHandle, int[] iArr, String str, PackageInstalledInfo packageInstalledInfo, int i3) throws Throwable {
        int i4;
        boolean z;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "replaceNonSystemPackageLI: new=" + r24 + ", old=" + r23);
        }
        String str2 = r23.packageName;
        int i5 = (i2 & 2048) == 0 ? 1 : 0;
        int i6 = 1 | (i5 != 0 ? 0 : 8);
        long j = r24.mExtras != null ? ((PackageSetting) r24.mExtras).lastUpdateTime : 0L;
        if (!deletePackageLIF(str2, null, true, iArr, i6, packageInstalledInfo.removedInfo, true, r24)) {
            packageInstalledInfo.setError(-10, "replaceNonSystemPackageLI");
            z = false;
            i5 = 0;
            i4 = 0;
        } else {
            if (r23.isForwardLocked() || isExternal(r23)) {
                if (DEBUG_INSTALL) {
                    Slog.i(TAG, "upgrading pkg " + r23 + " is ASEC-hosted -> UNAVAILABLE");
                }
                int[] iArr2 = {r23.applicationInfo.uid};
                ArrayList<String> arrayList = new ArrayList<>(1);
                arrayList.add(r23.applicationInfo.packageName);
                sendResourcesChangedBroadcast(false, true, arrayList, iArr2, (IIntentReceiver) null);
            }
            clearAppDataLIF(r24, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
            try {
                PackageParser.Package packageScanPackageTracedLI = scanPackageTracedLI(r24, i, i2 | 8, System.currentTimeMillis(), userHandle);
                updateSettingsLI(packageScanPackageTracedLI, str, iArr, packageInstalledInfo, userHandle, i3);
                PackageSetting packageSetting = this.mSettings.mPackages.get(str2);
                try {
                    if (i5 == 0) {
                        if (packageSetting.oldCodePaths == null) {
                            try {
                                packageSetting.oldCodePaths = new ArraySet();
                            } catch (PackageManagerException e) {
                                e = e;
                                i5 = 0;
                                i4 = 0;
                                packageInstalledInfo.setError("Package couldn't be installed in " + r24.codePath, e);
                                z = true;
                                if (packageInstalledInfo.returnCode != 1) {
                                }
                            }
                        }
                        Set<String> set = packageSetting.oldCodePaths;
                        String[] strArr = new String[1];
                        i5 = 0;
                        strArr[0] = r23.baseCodePath;
                        Collections.addAll(set, strArr);
                        if (r23.splitCodePaths != null) {
                            Collections.addAll(packageSetting.oldCodePaths, r23.splitCodePaths);
                        }
                    } else {
                        i5 = 0;
                        packageSetting.oldCodePaths = null;
                    }
                    if (packageSetting.childPackageNames != null) {
                        for (int size = packageSetting.childPackageNames.size() - 1; size >= 0; size--) {
                            this.mSettings.mPackages.get(packageSetting.childPackageNames.get(size)).oldCodePaths = packageSetting.oldCodePaths;
                        }
                    }
                    prepareAppDataAfterInstallLIF(packageScanPackageTracedLI);
                    try {
                        this.mDexManager.notifyPackageUpdated(packageScanPackageTracedLI.packageName, packageScanPackageTracedLI.baseCodePath, packageScanPackageTracedLI.splitCodePaths);
                        z = true;
                        i4 = 1;
                    } catch (PackageManagerException e2) {
                        e = e2;
                        i4 = 1;
                        packageInstalledInfo.setError("Package couldn't be installed in " + r24.codePath, e);
                        z = true;
                    }
                } catch (PackageManagerException e3) {
                    e = e3;
                    i4 = i5;
                    packageInstalledInfo.setError("Package couldn't be installed in " + r24.codePath, e);
                    z = true;
                    if (packageInstalledInfo.returnCode != 1) {
                    }
                }
            } catch (PackageManagerException e4) {
                e = e4;
                i5 = 0;
            }
        }
        if (packageInstalledInfo.returnCode != 1) {
            if (DEBUG_INSTALL) {
                Slog.d(TAG, "Install failed, rolling pack: " + str2);
            }
            if (i4 != 0) {
                deletePackageLIF(str2, null, true, iArr, i6, packageInstalledInfo.removedInfo, true, null);
            }
            if (z) {
                if (DEBUG_INSTALL) {
                    Slog.d(TAG, "Install failed, reinstalling: " + r23);
                }
                File file = new File(r23.codePath);
                boolean zIsExternal = isExternal(r23);
                int i7 = this.mDefParseFlags | Integer.MIN_VALUE | (r23.isForwardLocked() ? 4 : i5);
                if (zIsExternal) {
                    i5 = 8;
                }
                try {
                    scanPackageTracedLI(file, i7 | i5, 10, j, (UserHandle) null);
                    synchronized (this.mPackages) {
                        setInstallerPackageNameLPw(r23, str);
                        this.mPermissionManager.updatePermissions(r23.packageName, r23, false, this.mPackages.values(), this.mPermissionCallback);
                        this.mSettings.writeLPr();
                    }
                    Slog.i(TAG, "Successfully restored package : " + str2 + " after failed upgrade");
                    return;
                } catch (PackageManagerException e5) {
                    Slog.e(TAG, "Failed to restore package : " + str2 + " after failed upgrade: " + e5.getMessage());
                    return;
                }
            }
            return;
        }
        synchronized (this.mPackages) {
            PackageSetting packageLPr = this.mSettings.getPackageLPr(r24.packageName);
            if (packageLPr != null) {
                packageInstalledInfo.removedInfo.removedForAllUsers = this.mPackages.get(packageLPr.name) == null ? 1 : i5;
                if (packageInstalledInfo.removedInfo.removedChildPackages != null) {
                    for (int size2 = packageInstalledInfo.removedInfo.removedChildPackages.size() - 1; size2 >= 0; size2--) {
                        if (packageInstalledInfo.addedChildPackages.containsKey(packageInstalledInfo.removedInfo.removedChildPackages.keyAt(size2))) {
                            packageInstalledInfo.removedInfo.removedChildPackages.removeAt(size2);
                        } else {
                            PackageRemovedInfo packageRemovedInfoValueAt = packageInstalledInfo.removedInfo.removedChildPackages.valueAt(size2);
                            packageRemovedInfoValueAt.removedForAllUsers = this.mPackages.get(packageRemovedInfoValueAt.removedPackage) == null ? 1 : i5;
                        }
                    }
                }
            }
        }
    }

    private void replaceSystemPackageLIF(PackageParser.Package r21, PackageParser.Package r22, int i, int i2, UserHandle userHandle, int[] iArr, String str, PackageInstalledInfo packageInstalledInfo, int i3) throws Throwable {
        boolean zDisableSystemPackageLPw;
        PackageParser.Package packageScanPackageTracedLI;
        int size;
        int size2;
        int i4;
        int i5;
        PackageSetting disabledSystemPkgLPr;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "replaceSystemPackageLI: new=" + r22 + ", old=" + r21);
        }
        removePackageLI(r21, true);
        synchronized (this.mPackages) {
            zDisableSystemPackageLPw = disableSystemPackageLPw(r21, r22);
        }
        int i6 = 0;
        if (zDisableSystemPackageLPw) {
            packageInstalledInfo.removedInfo.args = null;
        } else {
            packageInstalledInfo.removedInfo.args = createInstallArgsForExisting(0, r21.applicationInfo.getCodePath(), r21.applicationInfo.getResourcePath(), InstructionSets.getAppDexInstructionSets(r21.applicationInfo));
        }
        clearAppDataLIF(r22, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
        packageInstalledInfo.setReturnCode(1);
        r22.setApplicationInfoFlags(128, 128);
        try {
            packageScanPackageTracedLI = scanPackageTracedLI(r22, i, i2, 0L, userHandle);
            try {
                setInstallAndUpdateTime(packageScanPackageTracedLI, ((PackageSetting) r21.mExtras).firstInstallTime, System.currentTimeMillis());
                if (packageInstalledInfo.returnCode == 1) {
                    if (r21.childPackages != null) {
                        size = r21.childPackages.size();
                    } else {
                        size = 0;
                    }
                    if (packageScanPackageTracedLI.childPackages != null) {
                        size2 = packageScanPackageTracedLI.childPackages.size();
                    } else {
                        size2 = 0;
                    }
                    int i7 = 0;
                    while (i7 < size) {
                        PackageParser.Package r1 = (PackageParser.Package) r21.childPackages.get(i7);
                        int i8 = i6;
                        while (true) {
                            if (i8 < size2) {
                                if (!r1.packageName.equals(((PackageParser.Package) packageScanPackageTracedLI.childPackages.get(i8)).packageName)) {
                                    i8++;
                                } else {
                                    i4 = i6;
                                    break;
                                }
                            } else {
                                i4 = 1;
                                break;
                            }
                        }
                        if (i4 == 0 || (disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(r1.packageName)) == null || packageInstalledInfo.removedInfo.removedChildPackages == null) {
                            i5 = i7;
                        } else {
                            PackageRemovedInfo packageRemovedInfo = packageInstalledInfo.removedInfo.removedChildPackages.get(r1.packageName);
                            i5 = i7;
                            removePackageDataLIF(disabledSystemPkgLPr, iArr, packageRemovedInfo, 0, false);
                            packageRemovedInfo.removedForAllUsers = this.mPackages.get(disabledSystemPkgLPr.name) == null;
                        }
                        i7 = i5 + 1;
                        i6 = 0;
                    }
                    updateSettingsLI(packageScanPackageTracedLI, str, iArr, packageInstalledInfo, userHandle, i3);
                    prepareAppDataAfterInstallLIF(packageScanPackageTracedLI);
                    this.mDexManager.notifyPackageUpdated(packageScanPackageTracedLI.packageName, packageScanPackageTracedLI.baseCodePath, packageScanPackageTracedLI.splitCodePaths);
                }
            } catch (PackageManagerException e) {
                e = e;
                packageInstalledInfo.setReturnCode(RequestStatus.SYS_ETIMEDOUT);
                packageInstalledInfo.setError("Package couldn't be installed in " + r22.codePath, e);
            }
        } catch (PackageManagerException e2) {
            e = e2;
            packageScanPackageTracedLI = null;
        }
        if (packageInstalledInfo.returnCode != 1) {
            if (packageScanPackageTracedLI != null) {
                removeInstalledPackageLI(packageScanPackageTracedLI, true);
            }
            try {
                scanPackageTracedLI(r21, i, 2, 0L, userHandle);
            } catch (PackageManagerException e3) {
                Slog.e(TAG, "Failed to restore original package: " + e3.getMessage());
            }
            synchronized (this.mPackages) {
                if (zDisableSystemPackageLPw) {
                    try {
                        enableSystemPackageLPw(r21);
                    } finally {
                    }
                }
                setInstallerPackageNameLPw(r21, str);
                this.mPermissionManager.updatePermissions(r21.packageName, r21, false, this.mPackages.values(), this.mPermissionCallback);
                this.mSettings.writeLPr();
            }
            Slog.i(TAG, "Successfully restored package : " + r21.packageName + " after failed upgrade");
        }
    }

    private String getParentOrChildPackageChangedSharedUser(PackageParser.Package r10, PackageParser.Package r11) {
        if (!Objects.equals(r10.mSharedUserId, r11.mSharedUserId)) {
            return r11.packageName;
        }
        int size = r10.childPackages != null ? r10.childPackages.size() : 0;
        int size2 = r11.childPackages != null ? r11.childPackages.size() : 0;
        for (int i = 0; i < size2; i++) {
            PackageParser.Package r4 = (PackageParser.Package) r11.childPackages.get(i);
            for (int i2 = 0; i2 < size; i2++) {
                PackageParser.Package r6 = (PackageParser.Package) r10.childPackages.get(i2);
                if (r4.packageName.equals(r6.packageName) && !Objects.equals(r4.mSharedUserId, r6.mSharedUserId)) {
                    return r4.packageName;
                }
            }
        }
        return null;
    }

    private void removeNativeBinariesLI(PackageSetting packageSetting) {
        PackageSetting packageLPr;
        if (packageSetting != null) {
            NativeLibraryHelper.removeNativeBinariesLI(packageSetting.legacyNativeLibraryPathString);
            int size = packageSetting.childPackageNames != null ? packageSetting.childPackageNames.size() : 0;
            for (int i = 0; i < size; i++) {
                synchronized (this.mPackages) {
                    packageLPr = this.mSettings.getPackageLPr(packageSetting.childPackageNames.get(i));
                }
                if (packageLPr != null) {
                    NativeLibraryHelper.removeNativeBinariesLI(packageLPr.legacyNativeLibraryPathString);
                }
            }
        }
    }

    private void enableSystemPackageLPw(PackageParser.Package r5) {
        this.mSettings.enableSystemPackageLPw(r5.packageName);
        int size = r5.childPackages != null ? r5.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            this.mSettings.enableSystemPackageLPw(((PackageParser.Package) r5.childPackages.get(i)).packageName);
        }
    }

    private boolean disableSystemPackageLPw(PackageParser.Package r7, PackageParser.Package r8) {
        boolean zDisableSystemPackageLPw = this.mSettings.disableSystemPackageLPw(r7.packageName, true);
        int size = r7.childPackages != null ? r7.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            PackageParser.Package r3 = (PackageParser.Package) r7.childPackages.get(i);
            zDisableSystemPackageLPw |= this.mSettings.disableSystemPackageLPw(r3.packageName, r8.hasChildPackage(r3.packageName));
        }
        return zDisableSystemPackageLPw;
    }

    private void setInstallerPackageNameLPw(PackageParser.Package r5, String str) {
        this.mSettings.setInstallerPackageName(r5.packageName, str);
        int size = r5.childPackages != null ? r5.childPackages.size() : 0;
        for (int i = 0; i < size; i++) {
            this.mSettings.setInstallerPackageName(((PackageParser.Package) r5.childPackages.get(i)).packageName, str);
        }
    }

    private void updateSettingsLI(PackageParser.Package r19, String str, int[] iArr, PackageInstalledInfo packageInstalledInfo, UserHandle userHandle, int i) throws Throwable {
        int size;
        updateSettingsInternalLI(r19, str, iArr, packageInstalledInfo.origUsers, packageInstalledInfo, userHandle, i);
        if (r19.childPackages != null) {
            size = r19.childPackages.size();
        } else {
            size = 0;
        }
        for (int i2 = 0; i2 < size; i2++) {
            PackageParser.Package r11 = (PackageParser.Package) r19.childPackages.get(i2);
            PackageInstalledInfo packageInstalledInfo2 = packageInstalledInfo.addedChildPackages.get(r11.packageName);
            updateSettingsInternalLI(r11, str, iArr, packageInstalledInfo2.origUsers, packageInstalledInfo2, userHandle, i);
        }
    }

    private void updateSettingsInternalLI(PackageParser.Package r21, String str, int[] iArr, int[] iArr2, PackageInstalledInfo packageInstalledInfo, UserHandle userHandle, int i) throws Throwable {
        boolean z;
        boolean z2;
        int i2;
        Trace.traceBegin(262144L, "updateSettings");
        String str2 = r21.packageName;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "New package installed in " + r21.codePath);
        }
        synchronized (this.mPackages) {
            try {
                try {
                    this.mPermissionManager.updatePermissions(r21.packageName, r21, true, this.mPackages.values(), this.mPermissionCallback);
                    PackageSetting packageSetting = this.mSettings.mPackages.get(str2);
                    int identifier = userHandle.getIdentifier();
                    if (packageSetting != null) {
                        if (isSystemApp(r21)) {
                            if (DEBUG_INSTALL) {
                                Slog.d(TAG, "Implicitly enabling system package on upgrade: " + str2);
                            }
                            if (packageInstalledInfo.origUsers != null) {
                                for (int i3 : packageInstalledInfo.origUsers) {
                                    if (identifier == -1 || identifier == i3) {
                                        packageSetting.setEnabled(0, i3, str);
                                    }
                                }
                            }
                            if (iArr != null && iArr2 != null) {
                                int length = iArr.length;
                                int i4 = 0;
                                while (i4 < length) {
                                    int i5 = iArr[i4];
                                    boolean zContains = ArrayUtils.contains(iArr2, i5);
                                    if (DEBUG_INSTALL) {
                                        StringBuilder sb = new StringBuilder();
                                        i2 = length;
                                        sb.append("    user ");
                                        sb.append(i5);
                                        sb.append(" => ");
                                        sb.append(zContains);
                                        Slog.d(TAG, sb.toString());
                                    } else {
                                        i2 = length;
                                    }
                                    packageSetting.setInstalled(zContains, i5);
                                    i4++;
                                    length = i2;
                                }
                            }
                        }
                        if (identifier != -1) {
                            z2 = true;
                            packageSetting.setInstalled(true, identifier);
                            z = false;
                            packageSetting.setEnabled(0, identifier, str);
                        } else {
                            z = false;
                            z2 = true;
                        }
                        sPmsExt.updatePackageSettings(identifier, str2, r21, packageSetting, iArr, str);
                        ArraySet arraySet = new ArraySet();
                        if (packageInstalledInfo.removedInfo != null && packageInstalledInfo.removedInfo.installReasons != null) {
                            int size = packageInstalledInfo.removedInfo.installReasons.size();
                            for (int i6 = 0; i6 < size; i6++) {
                                int iKeyAt = packageInstalledInfo.removedInfo.installReasons.keyAt(i6);
                                packageSetting.setInstallReason(packageInstalledInfo.removedInfo.installReasons.valueAt(i6).intValue(), iKeyAt);
                                arraySet.add(Integer.valueOf(iKeyAt));
                            }
                        }
                        if (identifier == -1) {
                            for (int i7 : sUserManager.getUserIds()) {
                                if (!arraySet.contains(Integer.valueOf(i7))) {
                                    packageSetting.setInstallReason(i, i7);
                                }
                            }
                        } else if (!arraySet.contains(Integer.valueOf(identifier))) {
                            packageSetting.setInstallReason(i, identifier);
                        }
                        this.mSettings.writeKernelMappingLPr(packageSetting);
                    }
                    packageInstalledInfo.name = str2;
                    packageInstalledInfo.uid = r21.applicationInfo.uid;
                    packageInstalledInfo.pkg = r21;
                    this.mSettings.setInstallerPackageName(str2, str);
                    packageInstalledInfo.setReturnCode(1);
                    Trace.traceBegin(262144L, "writeSettings");
                    this.mSettings.writeLPr();
                    Trace.traceEnd(262144L);
                    Trace.traceEnd(262144L);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void installPackageTracedLI(InstallArgs installArgs, PackageInstalledInfo packageInstalledInfo) {
        try {
            Trace.traceBegin(262144L, "installPackage");
            installPackageLI(installArgs, packageInstalledInfo);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private void installPackageLI(InstallArgs installArgs, PackageInstalledInfo packageInstalledInfo) throws Exception {
        long j;
        String str;
        String str2;
        boolean z;
        boolean z2;
        String str3;
        int i;
        int i2;
        boolean z3;
        String str4;
        ?? r9;
        String str5;
        boolean z4;
        String str6;
        ?? r4;
        boolean z5;
        String str7;
        Throwable th;
        PackageParser.Package r0;
        int i3;
        String str8;
        String str9;
        boolean z6;
        boolean zCheckUpgradeKeySetLocked;
        SharedLibraryEntry latestSharedLibraVersionLPr;
        String str10;
        int i4 = installArgs.installFlags;
        String str11 = installArgs.installerPackageName;
        String str12 = installArgs.volumeUuid;
        File file = new File(installArgs.getCodePath());
        int i5 = 1;
        boolean z7 = (i4 & 1) != 0;
        boolean z8 = ((i4 & 8) == 0 && installArgs.volumeUuid == null) ? false : true;
        boolean z9 = (i4 & 2048) != 0;
        boolean z10 = (i4 & 16384) != 0;
        boolean z11 = (i4 & 8192) != 0;
        boolean z12 = (i4 & 65536) != 0;
        int i6 = installArgs.move != null ? UsbTerminalTypes.TERMINAL_IN_PROC_MIC_ARRAY : 6;
        if ((i4 & 4096) != 0) {
            i6 |= 2048;
        }
        if (z9) {
            i6 |= 16384;
        }
        if (z10) {
            i6 |= 32768;
        }
        if (z12) {
            i6 |= 65536;
        }
        packageInstalledInfo.setReturnCode(1);
        packageInstalledInfo.installerPackageName = str11;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "installPackageLI: path=" + file);
        }
        if (z9 && (z7 || z8)) {
            Slog.i(TAG, "Incompatible ephemeral install; fwdLocked=" + z7 + " external=" + z8);
            packageInstalledInfo.setReturnCode(-116);
            return;
        }
        int i7 = (z11 ? 128 : 0) | this.mDefParseFlags | Integer.MIN_VALUE | 64 | (z7 ? 4 : 0) | (z8 ? 8 : 0);
        PackageParser packageParser = new PackageParser();
        packageParser.setSeparateProcesses(this.mSeparateProcesses);
        packageParser.setDisplayMetrics(this.mMetrics);
        packageParser.setCallback(this.mPackageParserCallback);
        int i8 = i6;
        Trace.traceBegin(262144L, "parsePackage");
        try {
            PackageParser.Package r13 = packageParser.parsePackage(file, i7);
            DexMetadataHelper.validatePackageDexMetadata(r13);
            Trace.traceEnd(262144L);
            if (z9) {
                if (r13.applicationInfo.targetSdkVersion < 26) {
                    Slog.w(TAG, "Instant app package " + r13.packageName + " does not target at least O");
                    packageInstalledInfo.setError(-116, "Instant app package must target at least O");
                    return;
                }
                if (r13.applicationInfo.targetSandboxVersion != 2) {
                    Slog.w(TAG, "Instant app package " + r13.packageName + " does not target targetSandboxVersion 2");
                    packageInstalledInfo.setError(-116, "Instant app package must use targetSandboxVersion 2");
                    return;
                }
                if (r13.mSharedUserId != null) {
                    Slog.w(TAG, "Instant app package " + r13.packageName + " may not declare sharedUserId.");
                    packageInstalledInfo.setError(-116, "Instant app package may not declare a sharedUserId");
                    return;
                }
            }
            if (r13.applicationInfo.isStaticSharedLibrary()) {
                renameStaticSharedLibraryPackage(r13);
                if (z8) {
                    Slog.i(TAG, "Static shared libs can only be installed on internal storage.");
                    packageInstalledInfo.setError(-19, "Packages declaring static-shared libs cannot be updated");
                    return;
                }
            }
            if (r13.childPackages != null) {
                synchronized (this.mPackages) {
                    int size = r13.childPackages.size();
                    int i9 = 0;
                    while (i9 < size) {
                        PackageParser.Package r3 = (PackageParser.Package) r13.childPackages.get(i9);
                        PackageInstalledInfo packageInstalledInfo2 = new PackageInstalledInfo();
                        packageInstalledInfo2.setReturnCode(i5);
                        packageInstalledInfo2.pkg = r3;
                        packageInstalledInfo2.name = r3.packageName;
                        int i10 = size;
                        PackageSetting packageLPr = this.mSettings.getPackageLPr(r3.packageName);
                        if (packageLPr != null) {
                            str10 = str12;
                            packageInstalledInfo2.origUsers = packageLPr.queryInstalledUsers(sUserManager.getUserIds(), true);
                        } else {
                            str10 = str12;
                        }
                        if (this.mPackages.containsKey(r3.packageName)) {
                            packageInstalledInfo2.removedInfo = new PackageRemovedInfo(this);
                            packageInstalledInfo2.removedInfo.removedPackage = r3.packageName;
                            packageInstalledInfo2.removedInfo.installerPackageName = packageLPr.installerPackageName;
                        }
                        if (packageInstalledInfo.addedChildPackages == null) {
                            packageInstalledInfo.addedChildPackages = new ArrayMap<>();
                        }
                        packageInstalledInfo.addedChildPackages.put(r3.packageName, packageInstalledInfo2);
                        i9++;
                        size = i10;
                        str12 = str10;
                        i5 = 1;
                    }
                    str = str12;
                }
            } else {
                str = str12;
            }
            if (TextUtils.isEmpty(r13.cpuAbiOverride)) {
                r13.cpuAbiOverride = installArgs.abiOverride;
            }
            String str13 = r13.packageName;
            packageInstalledInfo.name = str13;
            if ((r13.applicationInfo.flags & 256) != 0 && (i4 & 4) == 0) {
                packageInstalledInfo.setError(-15, "installPackageLI");
                return;
            }
            try {
                if (installArgs.signingDetails != PackageParser.SigningDetails.UNKNOWN) {
                    r13.setSigningDetails(installArgs.signingDetails);
                } else {
                    PackageParser.collectCertificates(r13, false);
                }
                if (z9 && r13.mSigningDetails.signatureSchemeVersion < 2) {
                    Slog.w(TAG, "Instant app package " + r13.packageName + " is not signed with at least APK Signature Scheme v2");
                    packageInstalledInfo.setError(-116, "Instant app package must be signed with APK Signature Scheme v2 or greater");
                    return;
                }
                synchronized (this.mPackages) {
                    if ((i4 & 2) != 0) {
                        try {
                            String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(str13);
                            if (r13.mOriginalPackages != null && r13.mOriginalPackages.contains(renamedPackageLPr) && this.mPackages.containsKey(renamedPackageLPr)) {
                                r13.setPackageName(renamedPackageLPr);
                                str13 = r13.packageName;
                                if (DEBUG_INSTALL) {
                                    Slog.d(TAG, "Replacing existing renamed package: oldName=" + renamedPackageLPr + " pkgName=" + str13);
                                }
                            } else if (!this.mPackages.containsKey(str13)) {
                                str2 = str13;
                                z = false;
                                if (r13.parentPackage == null) {
                                    packageInstalledInfo.setError(-106, "Package " + r13.packageName + " is child of package " + r13.parentPackage.parentPackage + ". Child packages can be updated only through the parent package.");
                                    return;
                                }
                                if (z) {
                                    PackageParser.Package r8 = this.mPackages.get(str2);
                                    int i11 = r8.applicationInfo.targetSdkVersion;
                                    int i12 = r13.applicationInfo.targetSdkVersion;
                                    if (i11 > 22 && i12 <= 22) {
                                        packageInstalledInfo.setError(-26, "Package " + r13.packageName + " new target SDK " + i12 + " doesn't support runtime permissions but the old target SDK " + i11 + " does.");
                                        return;
                                    }
                                    if ((r8.applicationInfo.flags & 8) != 0) {
                                        packageInstalledInfo.setError(-2, "Package " + r8.packageName + " is a persistent app. Persistent apps are not updateable.");
                                        return;
                                    }
                                    if (r8.parentPackage != null) {
                                        packageInstalledInfo.setError(-106, "Package " + r13.packageName + " is child of package " + r8.parentPackage + ". Child packages can be updated only through the parent package.");
                                        return;
                                    }
                                }
                                z2 = z;
                                str3 = str2;
                            } else if (DEBUG_INSTALL) {
                                Slog.d(TAG, "Replace existing pacakge: " + str13);
                            }
                            str2 = str13;
                            z = true;
                            if (r13.parentPackage == null) {
                            }
                        } finally {
                        }
                    } else {
                        str3 = str13;
                        z2 = false;
                    }
                    PackageSetting packageSetting = this.mSettings.mPackages.get(str3);
                    if (packageSetting != null) {
                        if (DEBUG_INSTALL) {
                            Slog.d(TAG, "Existing package: " + packageSetting);
                        }
                        PackageSetting packageLPr2 = (!r13.applicationInfo.isStaticSharedLibrary() || (latestSharedLibraVersionLPr = getLatestSharedLibraVersionLPr(r13)) == null) ? packageSetting : this.mSettings.getPackageLPr(latestSharedLibraVersionLPr.apk);
                        KeySetManagerService keySetManagerService = this.mSettings.mKeySetManagerService;
                        int i13 = i8;
                        if (!keySetManagerService.shouldCheckUpgradeKeySetLocked(packageLPr2, i13)) {
                            str4 = str11;
                            try {
                                i2 = i7;
                                i = i4;
                                z3 = z2;
                                if (PackageManagerServiceUtils.verifySignatures(packageLPr2, null, r13.mSigningDetails, isCompatSignatureUpdateNeeded(r13), isRecoverSignatureUpdateNeeded(r13))) {
                                    synchronized (this.mPackages) {
                                        keySetManagerService.removeAppKeySetDataLPw(r13.packageName);
                                    }
                                }
                            } catch (PackageManagerException e) {
                                packageInstalledInfo.setError(e.error, e.getMessage());
                                return;
                            }
                        } else {
                            if (!keySetManagerService.checkUpgradeKeySetLocked(packageLPr2, r13)) {
                                packageInstalledInfo.setError(-7, "Package " + r13.packageName + " upgrade keys do not match the previously installed version");
                                return;
                            }
                            i = i4;
                            i2 = i7;
                            z3 = z2;
                            str4 = str11;
                        }
                        str5 = this.mSettings.mPackages.get(str3).codePathString;
                        z4 = (packageSetting.pkg == null || packageSetting.pkg.applicationInfo == null || (packageSetting.pkg.applicationInfo.flags & 1) == 0) ? false : true;
                        packageInstalledInfo.origUsers = packageSetting.queryInstalledUsers(sUserManager.getUserIds(), true);
                        r9 = i13;
                    } else {
                        i = i4;
                        i2 = i7;
                        z3 = z2;
                        str4 = str11;
                        r9 = i8;
                        str5 = null;
                        z4 = false;
                    }
                    int size2 = r13.permissions.size() - 1;
                    while (size2 >= 0) {
                        PackageParser.Permission permission = (PackageParser.Permission) r13.permissions.get(size2);
                        BasePermission permissionTEMP = this.mPermissionManager.getPermissionTEMP(permission.info.name);
                        if ((permission.info.protectionLevel & 4096) == 0 || z4) {
                            str8 = str5;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            str8 = str5;
                            sb.append("Non-System package ");
                            sb.append(r13.packageName);
                            sb.append(" attempting to delcare ephemeral permission ");
                            sb.append(permission.info.name);
                            sb.append("; Removing ephemeral.");
                            Slog.w(TAG, sb.toString());
                            permission.info.protectionLevel &= -4097;
                        }
                        if (permissionTEMP != null) {
                            String sourcePackageName = permissionTEMP.getSourcePackageName();
                            PackageSettingBase sourcePackageSetting = permissionTEMP.getSourcePackageSetting();
                            ?? r82 = this.mSettings.mKeySetManagerService;
                            z6 = z7;
                            if (sourcePackageName.equals(r13.packageName) && r82.shouldCheckUpgradeKeySetLocked(sourcePackageSetting, r9)) {
                                zCheckUpgradeKeySetLocked = r82.checkUpgradeKeySetLocked(sourcePackageSetting, r13);
                                str9 = str3;
                            } else {
                                str9 = str3;
                                if (sourcePackageSetting.signatures.mSigningDetails.checkCapability(r13.mSigningDetails, 4)) {
                                    zCheckUpgradeKeySetLocked = true;
                                } else {
                                    if (r13.mSigningDetails.checkCapability(sourcePackageSetting.signatures.mSigningDetails, 4)) {
                                        sourcePackageSetting.signatures.mSigningDetails = r13.mSigningDetails;
                                        zCheckUpgradeKeySetLocked = true;
                                    } else {
                                        zCheckUpgradeKeySetLocked = false;
                                    }
                                    if (zCheckUpgradeKeySetLocked) {
                                        if (!sourcePackageName.equals(PLATFORM_PACKAGE_NAME)) {
                                            packageInstalledInfo.setError(-112, "Package " + r13.packageName + " attempting to redeclare permission " + permission.info.name + " already owned by " + sourcePackageName);
                                            packageInstalledInfo.origPermission = permission.info.name;
                                            packageInstalledInfo.origPackage = sourcePackageName;
                                            return;
                                        }
                                        Slog.w(TAG, "Package " + r13.packageName + " attempting to redeclare system permission " + permission.info.name + "; ignoring new declaration");
                                        r13.permissions.remove(size2);
                                    } else if (!PLATFORM_PACKAGE_NAME.equals(r13.packageName) && (permission.info.protectionLevel & 15) == 1 && permissionTEMP != null && !permissionTEMP.isRuntime()) {
                                        Slog.w(TAG, "Package " + r13.packageName + " trying to change a non-runtime permission " + permission.info.name + " to runtime; keeping old protection level");
                                        permission.info.protectionLevel = permissionTEMP.getProtectionLevel();
                                    }
                                }
                            }
                            if (zCheckUpgradeKeySetLocked) {
                            }
                        } else {
                            str9 = str3;
                            z6 = z7;
                        }
                        size2--;
                        str5 = str8;
                        z7 = z6;
                        str3 = str9;
                    }
                    String str14 = str5;
                    String str15 = str3;
                    boolean z13 = z7;
                    if (z4) {
                        if (z8) {
                            packageInstalledInfo.setError(-19, "Cannot install updates to system apps on sdcard");
                            return;
                        } else if (z9) {
                            packageInstalledInfo.setError(-116, "Cannot update a system app with an instant app");
                            return;
                        }
                    }
                    if (installArgs.move != null) {
                        int i14 = r9 | 1 | 256;
                        synchronized (this.mPackages) {
                            str6 = str15;
                            PackageSetting packageSetting2 = this.mSettings.mPackages.get(str6);
                            if (packageSetting2 == null) {
                                packageInstalledInfo.setError(RequestStatus.SYS_ETIMEDOUT, "Missing settings for moved package " + str6);
                            }
                            r13.applicationInfo.primaryCpuAbi = packageSetting2.primaryCpuAbiString;
                            r13.applicationInfo.secondaryCpuAbi = packageSetting2.secondaryCpuAbiString;
                        }
                        r4 = i14;
                    } else {
                        str6 = str15;
                        if (z13 || r13.applicationInfo.isExternalAsec()) {
                            r4 = r9;
                        } else {
                            int i15 = r9 | 1;
                            try {
                                derivePackageAbi(r13, TextUtils.isEmpty(r13.cpuAbiOverride) ? installArgs.abiOverride : r13.cpuAbiOverride, !r13.isLibrary());
                                synchronized (this.mPackages) {
                                    try {
                                        updateSharedLibrariesLPr(r13, null);
                                    } catch (PackageManagerException e2) {
                                        Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e2.getMessage());
                                    }
                                }
                                r4 = i15;
                            } catch (PackageManagerException e3) {
                                Slog.e(TAG, "Error deriving application ABI", e3);
                                packageInstalledInfo.setError(RequestStatus.SYS_ETIMEDOUT, "Error deriving application ABI");
                                return;
                            }
                        }
                    }
                    if (!installArgs.doRename(packageInstalledInfo.returnCode, r13, str14)) {
                        packageInstalledInfo.setError(-4, "Failed rename");
                        return;
                    }
                    if (PackageManagerServiceUtils.isApkVerityEnabled()) {
                        synchronized (this.mPackages) {
                            PackageSetting packageSetting3 = this.mSettings.mPackages.get(str6);
                            r9 = (packageSetting3 == null || !packageSetting3.isPrivileged()) ? 0 : r13.baseCodePath;
                        }
                        if (r9 != 0) {
                            VerityUtils.SetupResult setupResultGenerateApkVeritySetupData = VerityUtils.generateApkVeritySetupData(r9);
                            if (setupResultGenerateApkVeritySetupData.isOk()) {
                                if (Build.IS_DEBUGGABLE) {
                                    Slog.i(TAG, "Enabling apk verity to " + r9);
                                }
                                FileDescriptor unownedFileDescriptor = setupResultGenerateApkVeritySetupData.getUnownedFileDescriptor();
                                try {
                                    byte[] bArrGenerateFsverityRootHash = VerityUtils.generateFsverityRootHash(r9);
                                    this.mInstaller.installApkVerity(r9, unownedFileDescriptor, setupResultGenerateApkVeritySetupData.getContentSize());
                                    this.mInstaller.assertFsverityRootHashMatches(r9, bArrGenerateFsverityRootHash);
                                } catch (Installer.InstallerException | IOException | DigestException | NoSuchAlgorithmException e4) {
                                    packageInstalledInfo.setError(RequestStatus.SYS_ETIMEDOUT, "Failed to set up verity: " + e4);
                                    return;
                                } finally {
                                    IoUtils.closeQuietly(unownedFileDescriptor);
                                }
                            } else if (setupResultGenerateApkVeritySetupData.isFailed()) {
                                packageInstalledInfo.setError(RequestStatus.SYS_ETIMEDOUT, "Failed to generate verity");
                                return;
                            }
                        }
                    }
                    if (z9) {
                        z5 = z3;
                        if (DEBUG_DOMAIN_VERIFICATION) {
                            Slog.d(TAG, "Not verifying instant app install for app links: " + str6);
                        }
                    } else {
                        z5 = z3;
                        startIntentFilterVerifications(installArgs.user.getIdentifier(), z5, r13);
                    }
                    PackageFreezer packageFreezerFreezePackageForInstall = freezePackageForInstall(str6, i, "installPackageLI");
                    try {
                        if (z5) {
                            try {
                                if (r13.applicationInfo.isStaticSharedLibrary() && (r0 = this.mPackages.get(r13.packageName)) != null && r0.getLongVersionCode() != r13.getLongVersionCode()) {
                                    packageInstalledInfo.setError(-5, "Packages declaring static-shared libs cannot be updated");
                                    if (packageFreezerFreezePackageForInstall != null) {
                                        $closeResource(null, packageFreezerFreezePackageForInstall);
                                        return;
                                    }
                                    return;
                                }
                                str7 = str6;
                                replacePackageLIF(r13, i2, r4, installArgs.user, str4, packageInstalledInfo, installArgs.installReason);
                                th = null;
                                if (packageFreezerFreezePackageForInstall != null) {
                                    $closeResource(th, packageFreezerFreezePackageForInstall);
                                }
                                this.mArtManagerService.prepareAppProfiles(r13, resolveUserIds(installArgs.user.getIdentifier()));
                                if ((packageInstalledInfo.returnCode == 1 || z13 || r13.applicationInfo.isExternalAsec() || (z9 && Settings.Global.getInt(this.mContext.getContentResolver(), "instant_app_dexopt_enabled", 0) == 0) || (r13.applicationInfo.flags & 2) != 0) ? false : true) {
                                    Trace.traceBegin(262144L, "dexopt");
                                    this.mPackageDexOptimizer.performDexOpt(r13, r13.usesLibraryFiles, null, getOrCreateCompilerPackageStats(r13), this.mDexManager.getPackageUseInfoOrDefault(r13.packageName), new DexoptOptions(r13.packageName, 2, UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS));
                                    Trace.traceEnd(262144L);
                                }
                                BackgroundDexOptService.notifyPackageChanged(r13.packageName);
                                synchronized (this.mPackages) {
                                    String str16 = str7;
                                    PackageSetting packageSetting4 = this.mSettings.mPackages.get(str16);
                                    if (packageSetting4 != null) {
                                        packageInstalledInfo.newUsers = packageSetting4.queryInstalledUsers(sUserManager.getUserIds(), true);
                                        i3 = 0;
                                        packageSetting4.setUpdateAvailable(false);
                                    } else {
                                        i3 = 0;
                                    }
                                    int size3 = r13.childPackages != null ? r13.childPackages.size() : i3;
                                    while (i3 < size3) {
                                        PackageParser.Package r42 = (PackageParser.Package) r13.childPackages.get(i3);
                                        PackageInstalledInfo packageInstalledInfo3 = packageInstalledInfo.addedChildPackages.get(r42.packageName);
                                        PackageSetting packageLPr3 = this.mSettings.getPackageLPr(r42.packageName);
                                        if (packageLPr3 != null) {
                                            packageInstalledInfo3.newUsers = packageLPr3.queryInstalledUsers(sUserManager.getUserIds(), true);
                                        }
                                        i3++;
                                    }
                                    if (packageInstalledInfo.returnCode == 1) {
                                        updateSequenceNumberLP(packageSetting4, packageInstalledInfo.newUsers);
                                        updateInstantAppInstallerLocked(str16);
                                    }
                                }
                                return;
                            } catch (Throwable th2) {
                                th = th2;
                                r9 = 0;
                                if (packageFreezerFreezePackageForInstall != null) {
                                }
                                throw th;
                            }
                        }
                        str7 = str6;
                        r9 = 0;
                        try {
                            th = null;
                            try {
                                installNewPackageLIF(r13, i2, r4 | 64, installArgs.user, str4, str, packageInstalledInfo, installArgs.installReason);
                                if (packageFreezerFreezePackageForInstall != null) {
                                }
                                this.mArtManagerService.prepareAppProfiles(r13, resolveUserIds(installArgs.user.getIdentifier()));
                                if ((packageInstalledInfo.returnCode == 1 || z13 || r13.applicationInfo.isExternalAsec() || (z9 && Settings.Global.getInt(this.mContext.getContentResolver(), "instant_app_dexopt_enabled", 0) == 0) || (r13.applicationInfo.flags & 2) != 0) ? false : true) {
                                }
                                BackgroundDexOptService.notifyPackageChanged(r13.packageName);
                                synchronized (this.mPackages) {
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                r9 = 0;
                                if (packageFreezerFreezePackageForInstall != null) {
                                    $closeResource(r9, packageFreezerFreezePackageForInstall);
                                }
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
            } catch (PackageParser.PackageParserException e5) {
                packageInstalledInfo.setError("Failed collect during installPackageLI", e5);
            }
        } catch (PackageParser.PackageParserException e6) {
            j = 262144;
            try {
                packageInstalledInfo.setError("Failed parse during installPackageLI", e6);
                Trace.traceEnd(262144L);
            } catch (Throwable th6) {
                th = th6;
                Trace.traceEnd(j);
                throw th;
            }
        } catch (Throwable th7) {
            th = th7;
            j = 262144;
            Trace.traceEnd(j);
            throw th;
        }
    }

    private void startIntentFilterVerifications(int i, boolean z, PackageParser.Package r10) {
        if (this.mIntentFilterVerifierComponent == null) {
            Slog.w(TAG, "No IntentFilter verification will not be done as there is no IntentFilterVerifier available!");
            return;
        }
        int packageUid = getPackageUid(this.mIntentFilterVerifierComponent.getPackageName(), 268435456, i == -1 ? 0 : i);
        Message messageObtainMessage = this.mHandler.obtainMessage(17);
        messageObtainMessage.obj = new IFVerificationParams(r10, z, i, packageUid);
        this.mHandler.sendMessage(messageObtainMessage);
        int size = r10.childPackages != null ? r10.childPackages.size() : 0;
        for (int i2 = 0; i2 < size; i2++) {
            PackageParser.Package r4 = (PackageParser.Package) r10.childPackages.get(i2);
            Message messageObtainMessage2 = this.mHandler.obtainMessage(17);
            messageObtainMessage2.obj = new IFVerificationParams(r4, z, i, packageUid);
            this.mHandler.sendMessage(messageObtainMessage2);
        }
    }

    private void verifyIntentFiltersIfNeeded(int i, int i2, boolean z, PackageParser.Package r25) {
        Iterator it;
        Iterator it2;
        int i3;
        int size = r25.activities.size();
        if (size == 0) {
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No activity, so no need to verify any IntentFilter!");
                return;
            }
            return;
        }
        if (!hasDomainURLs(r25)) {
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No domain URLs, so no need to verify any IntentFilter!");
                return;
            }
            return;
        }
        if (DEBUG_DOMAIN_VERIFICATION) {
            Slog.d(TAG, "Checking for userId:" + i + " if any IntentFilter from the " + size + " Activities needs verification ...");
        }
        String str = r25.packageName;
        ArraySet arraySet = new ArraySet();
        synchronized (this.mPackages) {
            IntentFilterVerificationInfo intentFilterVerificationLPr = this.mSettings.getIntentFilterVerificationLPr(str);
            boolean z2 = false;
            int i4 = 0;
            boolean z3 = intentFilterVerificationLPr != null;
            if (!z && z3) {
                if (DEBUG_DOMAIN_VERIFICATION) {
                    Slog.i(TAG, "Package " + str + " already verified: status=" + intentFilterVerificationLPr.getStatusString());
                }
                return;
            }
            if (DEBUG_DOMAIN_VERIFICATION) {
                StringBuilder sb = new StringBuilder();
                sb.append("    Previous verified hosts: ");
                sb.append(intentFilterVerificationLPr == null ? "[none]" : intentFilterVerificationLPr.getDomainsString());
                Slog.i(TAG, sb.toString());
            }
            boolean zNeedsNetworkVerificationLPr = needsNetworkVerificationLPr(str);
            Iterator it3 = r25.activities.iterator();
            boolean z4 = false;
            while (it3.hasNext()) {
                Iterator it4 = ((PackageParser.Activity) it3.next()).intents.iterator();
                while (true) {
                    if (it4.hasNext()) {
                        PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) it4.next();
                        if (activityIntentInfo.handlesWebUris(true)) {
                        }
                        if (zNeedsNetworkVerificationLPr && activityIntentInfo.needsVerification()) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.d(TAG, "autoVerify requested, processing all filters");
                            }
                            z4 = true;
                        }
                    }
                }
                z4 = z4;
            }
            if (z4 || z3) {
                int i5 = this.mIntentFilterVerificationToken;
                this.mIntentFilterVerificationToken = i5 + 1;
                Iterator it5 = r25.activities.iterator();
                int i6 = 0;
                while (it5.hasNext()) {
                    Iterator it6 = ((PackageParser.Activity) it5.next()).intents.iterator();
                    int i7 = i6;
                    while (it6.hasNext()) {
                        PackageParser.ActivityIntentInfo activityIntentInfo2 = (PackageParser.ActivityIntentInfo) it6.next();
                        if (activityIntentInfo2.handlesWebUris(z2)) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.d(TAG, "Verification needed for IntentFilter:" + activityIntentInfo2.toString());
                            }
                            it = it6;
                            it2 = it5;
                            i3 = i5;
                            this.mIntentFilterVerifier.addOneIntentFilterVerification(i2, i, i5, activityIntentInfo2, str);
                            arraySet.addAll(activityIntentInfo2.getHostsList());
                            i7++;
                        } else {
                            it = it6;
                            it2 = it5;
                            i3 = i5;
                        }
                        it6 = it;
                        it5 = it2;
                        i5 = i3;
                        z2 = false;
                    }
                    i6 = i7;
                    z2 = false;
                }
                i4 = i6;
            }
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(TAG, "    Update published hosts: " + arraySet.toString());
            }
            boolean z5 = !(!z3 || (intentFilterVerificationLPr != null && !intentFilterVerificationLPr.getDomains().containsAll(arraySet))) && this.mSettings.getIntentFilterVerificationStatusLPr(str, i) == 2;
            if (z4 && z5) {
                if (DEBUG_DOMAIN_VERIFICATION) {
                    Slog.i(TAG, "Host set not expanding + ALWAYS -> no need to reverify");
                }
                intentFilterVerificationLPr.setDomains(arraySet);
                scheduleWriteSettingsLocked();
                return;
            }
            if (z3 && !z4) {
                clearIntentFilterVerificationsLPw(str, i, !z5);
                return;
            }
            if (z4 && i4 > 0) {
                if (DEBUG_DOMAIN_VERIFICATION) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Starting ");
                    sb2.append(i4);
                    sb2.append(" IntentFilter verification");
                    sb2.append(i4 > 1 ? "s" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    sb2.append(" for userId:");
                    sb2.append(i);
                    Slog.d(TAG, sb2.toString());
                }
                this.mIntentFilterVerifier.startVerifications(i);
                return;
            }
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No web filters or no new host policy for " + str);
            }
        }
    }

    private boolean needsNetworkVerificationLPr(String str) {
        IntentFilterVerificationInfo intentFilterVerificationLPr = this.mSettings.getIntentFilterVerificationLPr(str);
        if (intentFilterVerificationLPr == null) {
            return true;
        }
        switch (intentFilterVerificationLPr.getStatus()) {
        }
        return true;
    }

    private static boolean isMultiArch(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & Integer.MIN_VALUE) != 0;
    }

    private static boolean isExternal(PackageParser.Package r1) {
        return (r1.applicationInfo.flags & 262144) != 0;
    }

    private static boolean isExternal(PackageSetting packageSetting) {
        return (packageSetting.pkgFlags & 262144) != 0;
    }

    private static boolean isSystemApp(PackageParser.Package r1) {
        return (r1.applicationInfo.flags & 1) != 0;
    }

    private static boolean isPrivilegedApp(PackageParser.Package r0) {
        return (r0.applicationInfo.privateFlags & 8) != 0;
    }

    private static boolean isOemApp(PackageParser.Package r1) {
        return (r1.applicationInfo.privateFlags & 131072) != 0;
    }

    private static boolean isVendorApp(PackageParser.Package r1) {
        return (r1.applicationInfo.privateFlags & 262144) != 0;
    }

    private static boolean isProductApp(PackageParser.Package r1) {
        return (r1.applicationInfo.privateFlags & 524288) != 0;
    }

    private static boolean hasDomainURLs(PackageParser.Package r0) {
        return (r0.applicationInfo.privateFlags & 16) != 0;
    }

    private static boolean isSystemApp(PackageSetting packageSetting) {
        return (packageSetting.pkgFlags & 1) != 0;
    }

    private static boolean isUpdatedSystemApp(PackageSetting packageSetting) {
        return (packageSetting.pkgFlags & 128) != 0;
    }

    private int packageFlagsToInstallFlags(PackageSetting packageSetting) {
        int i;
        if (isExternal(packageSetting) && TextUtils.isEmpty(packageSetting.volumeUuid)) {
            i = 8;
        } else {
            i = 0;
        }
        if (packageSetting.isForwardLocked()) {
            return i | 1;
        }
        return i;
    }

    private Settings.VersionInfo getSettingsVersionForPackage(PackageParser.Package r2) {
        if (isExternal(r2)) {
            if (TextUtils.isEmpty(r2.volumeUuid)) {
                return this.mSettings.getExternalVersion();
            }
            return this.mSettings.findOrCreateVersion(r2.volumeUuid);
        }
        return this.mSettings.getInternalVersion();
    }

    private void deleteTempPackageFiles() {
        for (File file : sDrmAppPrivateInstallDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file2, String str) {
                return str.startsWith("vmdl") && str.endsWith(".tmp");
            }
        })) {
            file.delete();
        }
    }

    public void deletePackageAsUser(String str, int i, IPackageDeleteObserver iPackageDeleteObserver, int i2, int i3) {
        deletePackageVersioned(new VersionedPackage(str, i), new PackageManager.LegacyPackageDeleteObserver(iPackageDeleteObserver).getBinder(), i2, i3);
    }

    public void deletePackageVersioned(VersionedPackage versionedPackage, final IPackageDeleteObserver2 iPackageDeleteObserver2, final int i, final int i2) {
        final String strResolveInternalPackageNameLPr;
        final int callingUid = Binder.getCallingUid();
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        final boolean zCanViewInstantApps = canViewInstantApps(callingUid, i);
        Preconditions.checkNotNull(versionedPackage);
        Preconditions.checkNotNull(iPackageDeleteObserver2);
        Preconditions.checkArgumentInRange(versionedPackage.getLongVersionCode(), -1L, JobStatus.NO_LATEST_RUNTIME, "versionCode must be >= -1");
        final String packageName = versionedPackage.getPackageName();
        final long longVersionCode = versionedPackage.getLongVersionCode();
        synchronized (this.mPackages) {
            strResolveInternalPackageNameLPr = resolveInternalPackageNameLPr(packageName, longVersionCode);
        }
        int callingUid2 = Binder.getCallingUid();
        if (!isOrphaned(strResolveInternalPackageNameLPr) && !isCallerAllowedToSilentlyUninstall(callingUid2, strResolveInternalPackageNameLPr)) {
            try {
                Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
                intent.setData(Uri.fromParts("package", packageName, null));
                intent.putExtra("android.content.pm.extra.CALLBACK", iPackageDeleteObserver2.asBinder());
                iPackageDeleteObserver2.onUserActionRequired(intent);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        boolean z = (i2 & 2) != 0;
        final int[] userIds = z ? sUserManager.getUserIds() : new int[]{i};
        if (UserHandle.getUserId(callingUid2) != i || (z && userIds.length > 1)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "deletePackage for user " + i);
        }
        if (isUserRestricted(i, "no_uninstall_apps")) {
            try {
                iPackageDeleteObserver2.onPackageDeleted(packageName, -3, (String) null);
                return;
            } catch (RemoteException e2) {
                return;
            }
        }
        if (!z && getBlockUninstallForUser(strResolveInternalPackageNameLPr, i)) {
            try {
                iPackageDeleteObserver2.onPackageDeleted(packageName, -4, (String) null);
                return;
            } catch (RemoteException e3) {
                return;
            }
        }
        if (DEBUG_REMOVE) {
            StringBuilder sb = new StringBuilder();
            sb.append("deletePackageAsUser: pkg=");
            sb.append(strResolveInternalPackageNameLPr);
            sb.append(" user=");
            sb.append(i);
            sb.append(" deleteAllUsers: ");
            sb.append(z);
            sb.append(" version=");
            sb.append(longVersionCode == -1 ? "VERSION_CODE_HIGHEST" : Long.valueOf(longVersionCode));
            Slog.d(TAG, sb.toString());
        }
        final boolean z2 = z;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() throws Throwable {
                int iDeletePackageX;
                int iDeletePackageX2;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(strResolveInternalPackageNameLPr);
                if (packageSetting == null || !packageSetting.getInstantApp(UserHandle.getUserId(callingUid)) || zCanViewInstantApps) {
                    if (z2) {
                        int[] blockUninstallForUsers = PackageManagerService.this.getBlockUninstallForUsers(strResolveInternalPackageNameLPr, userIds);
                        if (ArrayUtils.isEmpty(blockUninstallForUsers)) {
                            iDeletePackageX = PackageManagerService.this.deletePackageX(strResolveInternalPackageNameLPr, longVersionCode, i, i2);
                        } else {
                            int i3 = i2 & (-3);
                            for (int i4 : userIds) {
                                if (!ArrayUtils.contains(blockUninstallForUsers, i4) && (iDeletePackageX2 = PackageManagerService.this.deletePackageX(strResolveInternalPackageNameLPr, longVersionCode, i4, i3)) != 1) {
                                    Slog.w(PackageManagerService.TAG, "Package delete failed for user " + i4 + ", returnCode " + iDeletePackageX2);
                                }
                            }
                            iDeletePackageX = -4;
                        }
                    } else {
                        iDeletePackageX = PackageManagerService.this.deletePackageX(strResolveInternalPackageNameLPr, longVersionCode, i, i2);
                    }
                } else {
                    iDeletePackageX = -1;
                }
                try {
                    iPackageDeleteObserver2.onPackageDeleted(packageName, iDeletePackageX, (String) null);
                } catch (RemoteException e4) {
                    Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                }
            }
        });
    }

    private String resolveExternalPackageNameLPr(PackageParser.Package r2) {
        if (r2.staticSharedLibName != null) {
            return r2.manifestPackageName;
        }
        return r2.packageName;
    }

    private String resolveInternalPackageNameLPr(String str, long j) {
        LongSparseLongArray longSparseLongArray;
        String renamedPackageLPr = this.mSettings.getRenamedPackageLPr(str);
        if (renamedPackageLPr != null) {
            str = renamedPackageLPr;
        }
        LongSparseArray<SharedLibraryEntry> longSparseArray = this.mStaticLibsByDeclaringPackage.get(str);
        if (longSparseArray == null || longSparseArray.size() <= 0) {
            return str;
        }
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        SharedLibraryEntry sharedLibraryEntry = null;
        if (appId != 1000 && appId != 2000 && appId != 0) {
            longSparseLongArray = new LongSparseLongArray();
            String name = longSparseArray.valueAt(0).info.getName();
            String[] packagesForUid = getPackagesForUid(Binder.getCallingUid());
            if (packagesForUid != null) {
                for (String str2 : packagesForUid) {
                    PackageSetting packageLPr = this.mSettings.getPackageLPr(str2);
                    int iIndexOf = ArrayUtils.indexOf(packageLPr.usesStaticLibraries, name);
                    if (iIndexOf >= 0) {
                        long j2 = packageLPr.usesStaticLibrariesVersions[iIndexOf];
                        longSparseLongArray.append(j2, j2);
                    }
                }
            }
        } else {
            longSparseLongArray = null;
        }
        if (longSparseLongArray != null && longSparseLongArray.size() <= 0) {
            return str;
        }
        int size = longSparseArray.size();
        for (int i = 0; i < size; i++) {
            SharedLibraryEntry sharedLibraryEntryValueAt = longSparseArray.valueAt(i);
            if (longSparseLongArray == null || longSparseLongArray.indexOfKey(sharedLibraryEntryValueAt.info.getLongVersion()) >= 0) {
                long longVersionCode = sharedLibraryEntryValueAt.info.getDeclaringPackage().getLongVersionCode();
                if (j != -1) {
                    if (longVersionCode == j) {
                        return sharedLibraryEntryValueAt.apk;
                    }
                } else if (sharedLibraryEntry == null || longVersionCode > sharedLibraryEntry.info.getDeclaringPackage().getLongVersionCode()) {
                    sharedLibraryEntry = sharedLibraryEntryValueAt;
                }
            }
        }
        if (sharedLibraryEntry != null) {
            return sharedLibraryEntry.apk;
        }
        return str;
    }

    boolean isCallerVerifier(int i) {
        return this.mRequiredVerifierPackage != null && i == getPackageUid(this.mRequiredVerifierPackage, 0, UserHandle.getUserId(i));
    }

    private boolean isCallerAllowedToSilentlyUninstall(int i, String str) {
        if (i == 2000 || i == 0 || UserHandle.getAppId(i) == 1000) {
            return true;
        }
        int userId = UserHandle.getUserId(i);
        if (i == getPackageUid(getInstallerPackageName(str), 0, userId)) {
            return true;
        }
        if (this.mRequiredVerifierPackage != null && i == getPackageUid(this.mRequiredVerifierPackage, 0, userId)) {
            return true;
        }
        if (this.mRequiredUninstallerPackage == null || i != getPackageUid(this.mRequiredUninstallerPackage, 0, userId)) {
            return (this.mStorageManagerPackage != null && i == getPackageUid(this.mStorageManagerPackage, 0, userId)) || checkUidPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", i) == 0;
        }
        return true;
    }

    private int[] getBlockUninstallForUsers(String str, int[] iArr) {
        int[] iArrAppendInt = EMPTY_INT_ARRAY;
        for (int i : iArr) {
            if (getBlockUninstallForUser(str, i)) {
                iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i);
            }
        }
        return iArrAppendInt;
    }

    public boolean isPackageDeviceAdminOnAnyUser(String str) {
        int callingUid = Binder.getCallingUid();
        if (checkUidPermission("android.permission.MANAGE_USERS", callingUid) != 0) {
            EventLog.writeEvent(1397638484, "128599183", -1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            throw new SecurityException("android.permission.MANAGE_USERS permission is required to call this API");
        }
        if (getInstantAppPackageName(callingUid) == null || isCallerSameApp(str, callingUid)) {
            return isPackageDeviceAdmin(str, -1);
        }
        return false;
    }

    private boolean isPackageDeviceAdmin(String str, int i) {
        int[] userIds;
        IDevicePolicyManager iDevicePolicyManagerAsInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (iDevicePolicyManagerAsInterface != null) {
            try {
                ComponentName deviceOwnerComponent = iDevicePolicyManagerAsInterface.getDeviceOwnerComponent(false);
                if (str.equals(deviceOwnerComponent == null ? null : deviceOwnerComponent.getPackageName())) {
                    return true;
                }
                if (i == -1) {
                    userIds = sUserManager.getUserIds();
                } else {
                    userIds = new int[]{i};
                }
                for (int i2 : userIds) {
                    if (iDevicePolicyManagerAsInterface.packageHasActiveAdmins(str, i2)) {
                        return true;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    private boolean shouldKeepUninstalledPackageLPr(String str) {
        return this.mKeepUninstalledPackages != null && this.mKeepUninstalledPackages.contains(str);
    }

    public int deletePackageX(String str, long j, int i, int i2) throws Throwable {
        Object obj;
        Throwable th;
        PackageFreezer packageFreezer;
        int i3;
        SharedLibraryEntry sharedLibraryEntryLPr;
        PackageRemovedInfo packageRemovedInfo = new PackageRemovedInfo(this);
        int i4 = (i2 & 2) != 0 ? -1 : i;
        if (isPackageDeviceAdmin(str, i4)) {
            Slog.w(TAG, "Not removing package " + str + ": has active device admin");
            return -2;
        }
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                Slog.w(TAG, "Not removing non-existent package " + str);
                return -1;
            }
            if (j != -1 && packageSetting.versionCode != j) {
                Slog.w(TAG, "Not removing package " + str + " with versionCode " + packageSetting.versionCode + " != " + j);
                return -1;
            }
            PackageParser.Package r15 = this.mPackages.get(str);
            int[] userIds = sUserManager.getUserIds();
            if (r15 != null && r15.staticSharedLibName != null && (sharedLibraryEntryLPr = getSharedLibraryEntryLPr(r15.staticSharedLibName, r15.staticSharedLibVersion)) != null) {
                for (int i5 : userIds) {
                    if (i4 == -1 || i4 == i5) {
                        List<VersionedPackage> packagesUsingSharedLibraryLPr = getPackagesUsingSharedLibraryLPr(sharedLibraryEntryLPr.info, 4202496, i5);
                        if (!ArrayUtils.isEmpty(packagesUsingSharedLibraryLPr)) {
                            Slog.w(TAG, "Not removing package " + r15.manifestPackageName + " hosting lib " + sharedLibraryEntryLPr.info.getName() + " version " + sharedLibraryEntryLPr.info.getLongVersion() + " used by " + packagesUsingSharedLibraryLPr + " for user " + i5);
                            return -6;
                        }
                    }
                }
            }
            packageRemovedInfo.origUsers = packageSetting.queryInstalledUsers(userIds, true);
            int i6 = (isUpdatedSystemApp(packageSetting) && (i2 & 4) == 0) ? -1 : i4;
            Object obj2 = this.mInstallLock;
            synchronized (obj2) {
                try {
                    try {
                        if (DEBUG_REMOVE) {
                            Slog.d(TAG, "deletePackageX: pkg=" + str + " user=" + i);
                        }
                        PackageFreezer packageFreezerFreezePackageForDelete = freezePackageForDelete(str, i6, i2, "deletePackageX");
                        try {
                            packageFreezer = packageFreezerFreezePackageForDelete;
                            obj = obj2;
                            try {
                                boolean zDeletePackageLIF = deletePackageLIF(str, UserHandle.of(i4), true, userIds, i2 | Integer.MIN_VALUE, packageRemovedInfo, true, null);
                                if (packageFreezer != null) {
                                    $closeResource(null, packageFreezer);
                                }
                                synchronized (this.mPackages) {
                                    if (zDeletePackageLIF) {
                                        if (r15 != null) {
                                            this.mInstantAppRegistry.onPackageUninstalledLPw(r15, packageRemovedInfo.removedUsers);
                                        }
                                        updateSequenceNumberLP(packageSetting, packageRemovedInfo.removedUsers);
                                        updateInstantAppInstallerLocked(str);
                                    }
                                }
                                if (zDeletePackageLIF) {
                                    packageRemovedInfo.sendPackageRemovedBroadcasts((i2 & 8) == 0);
                                    packageRemovedInfo.sendSystemPackageUpdatedBroadcasts();
                                    packageRemovedInfo.sendSystemPackageAppearedBroadcasts();
                                }
                                Runtime.getRuntime().gc();
                                if (packageRemovedInfo.args != null) {
                                    synchronized (this.mInstallLock) {
                                        i3 = 1;
                                        packageRemovedInfo.args.doPostDeleteLI(true);
                                    }
                                } else {
                                    i3 = 1;
                                }
                                if (zDeletePackageLIF) {
                                    return i3;
                                }
                                return -1;
                            } catch (Throwable th2) {
                                th = th2;
                                th = null;
                                if (packageFreezer != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            th = null;
                            packageFreezer = packageFreezerFreezePackageForDelete;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        obj = obj2;
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            }
        }
    }

    public static class PackageRemovedInfo {
        ArrayMap<String, PackageInstalledInfo> appearedChildPackages;
        boolean dataRemoved;
        SparseArray<Integer> installReasons;
        String installerPackageName;
        boolean isStaticSharedLib;
        boolean isUpdate;
        int[] origUsers;
        final PackageSender packageSender;
        ArrayMap<String, PackageRemovedInfo> removedChildPackages;
        boolean removedForAllUsers;
        String removedPackage;
        int uid = -1;
        int removedAppId = -1;
        int[] removedUsers = null;
        int[] broadcastUsers = null;
        int[] instantUserIds = null;
        boolean isRemovedPackageSystemUpdate = false;
        InstallArgs args = null;

        PackageRemovedInfo(PackageSender packageSender) {
            this.packageSender = packageSender;
        }

        void sendPackageRemovedBroadcasts(boolean z) {
            sendPackageRemovedBroadcastInternal(z);
            int size = this.removedChildPackages != null ? this.removedChildPackages.size() : 0;
            for (int i = 0; i < size; i++) {
                this.removedChildPackages.valueAt(i).sendPackageRemovedBroadcastInternal(z);
            }
        }

        void sendSystemPackageUpdatedBroadcasts() {
            int size;
            if (this.isRemovedPackageSystemUpdate) {
                sendSystemPackageUpdatedBroadcastsInternal();
                if (this.removedChildPackages != null) {
                    size = this.removedChildPackages.size();
                } else {
                    size = 0;
                }
                for (int i = 0; i < size; i++) {
                    PackageRemovedInfo packageRemovedInfoValueAt = this.removedChildPackages.valueAt(i);
                    if (packageRemovedInfoValueAt.isRemovedPackageSystemUpdate) {
                        packageRemovedInfoValueAt.sendSystemPackageUpdatedBroadcastsInternal();
                    }
                }
            }
        }

        void sendSystemPackageAppearedBroadcasts() {
            int size;
            if (this.appearedChildPackages != null) {
                size = this.appearedChildPackages.size();
            } else {
                size = 0;
            }
            for (int i = 0; i < size; i++) {
                PackageInstalledInfo packageInstalledInfoValueAt = this.appearedChildPackages.valueAt(i);
                this.packageSender.sendPackageAddedForNewUsers(packageInstalledInfoValueAt.name, true, false, UserHandle.getAppId(packageInstalledInfoValueAt.uid), packageInstalledInfoValueAt.newUsers, null);
            }
        }

        private void sendSystemPackageUpdatedBroadcastsInternal() {
            Bundle bundle = new Bundle(2);
            bundle.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            bundle.putBoolean("android.intent.extra.REPLACING", true);
            this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.removedPackage, bundle, 0, null, null, null, null);
            this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.removedPackage, bundle, 0, null, null, null, null);
            this.packageSender.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, this.removedPackage, null, null, null);
            if (this.installerPackageName != null) {
                this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.removedPackage, bundle, 0, this.installerPackageName, null, null, null);
                this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.removedPackage, bundle, 0, this.installerPackageName, null, null, null);
            }
        }

        private void sendPackageRemovedBroadcastInternal(boolean z) {
            if (this.isStaticSharedLib) {
                return;
            }
            Bundle bundle = new Bundle(2);
            bundle.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            bundle.putBoolean("android.intent.extra.DATA_REMOVED", this.dataRemoved);
            bundle.putBoolean("android.intent.extra.DONT_KILL_APP", !z);
            if (this.isUpdate || this.isRemovedPackageSystemUpdate) {
                bundle.putBoolean("android.intent.extra.REPLACING", true);
            }
            bundle.putBoolean("android.intent.extra.REMOVED_FOR_ALL_USERS", this.removedForAllUsers);
            if (this.removedPackage != null) {
                this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, bundle, 0, null, null, this.broadcastUsers, this.instantUserIds);
                if (this.installerPackageName != null) {
                    this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, bundle, 0, this.installerPackageName, null, this.broadcastUsers, this.instantUserIds);
                }
                if (this.dataRemoved && !this.isRemovedPackageSystemUpdate) {
                    this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_FULLY_REMOVED", this.removedPackage, bundle, DumpState.DUMP_SERVICE_PERMISSIONS, null, null, this.broadcastUsers, this.instantUserIds);
                    this.packageSender.notifyPackageRemoved(this.removedPackage);
                }
            }
            if (this.removedAppId >= 0) {
                this.packageSender.sendPackageBroadcast("android.intent.action.UID_REMOVED", null, bundle, DumpState.DUMP_SERVICE_PERMISSIONS, null, null, this.broadcastUsers, this.instantUserIds);
            }
        }

        void populateUsers(int[] iArr, PackageSetting packageSetting) {
            this.removedUsers = iArr;
            if (this.removedUsers != null) {
                this.broadcastUsers = PackageManagerService.EMPTY_INT_ARRAY;
                this.instantUserIds = PackageManagerService.EMPTY_INT_ARRAY;
                for (int length = iArr.length - 1; length >= 0; length--) {
                    int i = iArr[length];
                    if (packageSetting.getInstantApp(i)) {
                        this.instantUserIds = ArrayUtils.appendInt(this.instantUserIds, i);
                    } else {
                        this.broadcastUsers = ArrayUtils.appendInt(this.broadcastUsers, i);
                    }
                }
                return;
            }
            this.broadcastUsers = null;
        }
    }

    private void removePackageDataLIF(PackageSetting packageSetting, int[] iArr, PackageRemovedInfo packageRemovedInfo, int i, boolean z) {
        PackageParser.Package r7;
        final PackageSetting packageSetting2;
        int i2;
        int iRemovePackageLPw;
        String str = packageSetting.name;
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "removePackageDataLI: " + packageSetting);
        }
        synchronized (this.mPackages) {
            r7 = this.mPackages.get(str);
            packageSetting2 = this.mSettings.mPackages.get(str);
            i2 = 0;
            if (packageRemovedInfo != null) {
                packageRemovedInfo.removedPackage = str;
                packageRemovedInfo.installerPackageName = packageSetting.installerPackageName;
                packageRemovedInfo.isStaticSharedLib = (r7 == null || r7.staticSharedLibName == null) ? false : true;
                packageRemovedInfo.populateUsers(packageSetting2 == null ? null : packageSetting2.queryInstalledUsers(sUserManager.getUserIds(), true), packageSetting2);
            }
        }
        removePackageLI(packageSetting, (i & Integer.MIN_VALUE) != 0);
        int i3 = i & 1;
        if (i3 == 0) {
            if (r7 == null) {
                r7 = new PackageParser.Package(packageSetting.name);
                r7.setVolumeUuid(packageSetting.volumeUuid);
            }
            destroyAppDataLIF(r7, -1, 3);
            destroyAppProfilesLIF(r7, -1);
            if (packageRemovedInfo != null) {
                packageRemovedInfo.dataRemoved = true;
            }
            schedulePackageCleaning(str, -1, true);
        }
        synchronized (this.mPackages) {
            if (packageSetting2 != null) {
                if (i3 == 0) {
                    try {
                        clearIntentFilterVerificationsLPw(packageSetting2.name, -1, true);
                        clearDefaultBrowserIfNeeded(str);
                        this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(str);
                        iRemovePackageLPw = this.mSettings.removePackageLPw(str);
                        if (packageRemovedInfo != null) {
                            packageRemovedInfo.removedAppId = iRemovePackageLPw;
                        }
                        this.mPermissionManager.updatePermissions(packageSetting2.name, null, false, this.mPackages.values(), this.mPermissionCallback);
                        if (packageSetting2.sharedUser != null) {
                            for (int i4 : UserManagerService.getInstance().getUserIds()) {
                                int iUpdateSharedUserPermsLPw = this.mSettings.updateSharedUserPermsLPw(packageSetting2, i4);
                                if (iUpdateSharedUserPermsLPw != -1 && iUpdateSharedUserPermsLPw < 0) {
                                }
                                this.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        PackageManagerService.this.killApplication(packageSetting2.name, packageSetting2.appId, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                                    }
                                });
                                break;
                            }
                        }
                        clearPackagePreferredActivitiesLPw(packageSetting2.name, -1);
                    } finally {
                    }
                } else {
                    iRemovePackageLPw = -1;
                }
                if (iArr != null && packageRemovedInfo != null && packageRemovedInfo.origUsers != null) {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Propagating install state across downgrade");
                    }
                    int length = iArr.length;
                    int i5 = 0;
                    while (i2 < length) {
                        int i6 = iArr[i2];
                        boolean zContains = ArrayUtils.contains(packageRemovedInfo.origUsers, i6);
                        if (DEBUG_REMOVE) {
                            Slog.d(TAG, "    user " + i6 + " => " + zContains);
                        }
                        if (zContains != packageSetting.getInstalled(i6)) {
                            i5 = 1;
                        }
                        packageSetting.setInstalled(zContains, i6);
                        i2++;
                    }
                    i2 = i5;
                }
            } else {
                iRemovePackageLPw = -1;
            }
            if (z) {
                this.mSettings.writeLPr();
            }
            if (i2 != 0) {
                this.mSettings.writeKernelMappingLPr(packageSetting);
            }
        }
        if (iRemovePackageLPw != -1) {
            removeKeystoreDataIfNeeded(-1, iRemovePackageLPw);
        }
    }

    static boolean locationIsPrivileged(String str) {
        try {
            File file = new File(Environment.getRootDirectory(), "priv-app");
            File file2 = new File(Environment.getVendorDirectory(), "priv-app");
            File file3 = new File(Environment.getOdmDirectory(), "priv-app");
            File file4 = new File(Environment.getProductDirectory(), "priv-app");
            if (!str.startsWith(file.getCanonicalPath()) && !str.startsWith(file2.getCanonicalPath()) && !str.startsWith(file3.getCanonicalPath())) {
                if (!str.startsWith(file4.getCanonicalPath())) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + str);
            return false;
        }
    }

    static boolean locationIsOem(String str) {
        try {
            return str.startsWith(Environment.getOemDirectory().getCanonicalPath());
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + str);
            return false;
        }
    }

    static boolean locationIsVendor(String str) {
        try {
            if (!str.startsWith(Environment.getVendorDirectory().getCanonicalPath())) {
                if (!str.startsWith(Environment.getOdmDirectory().getCanonicalPath())) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + str);
            return false;
        }
    }

    static boolean locationIsProduct(String str) {
        try {
            return str.startsWith(Environment.getProductDirectory().getCanonicalPath());
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + str);
            return false;
        }
    }

    private boolean deleteSystemPackageLIF(PackageParser.Package r17, PackageSetting packageSetting, int[] iArr, int i, PackageRemovedInfo packageRemovedInfo, boolean z) {
        PackageSetting disabledSystemPkgLPr;
        PackageRemovedInfo packageRemovedInfo2;
        if (packageSetting.parentPackageName != null) {
            Slog.w(TAG, "Attempt to delete child system package " + r17.packageName);
            return false;
        }
        boolean z2 = (iArr == null || packageRemovedInfo == null || packageRemovedInfo.origUsers == null) ? false : true;
        synchronized (this.mPackages) {
            disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(packageSetting.name);
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "deleteSystemPackageLI: newPs=" + r17.packageName + " disabledPs=" + disabledSystemPkgLPr);
        }
        if (disabledSystemPkgLPr == null) {
            Slog.w(TAG, "Attempt to delete unknown system package " + r17.packageName);
            return false;
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "Deleting system pkg from data partition");
        }
        if (DEBUG_REMOVE && z2) {
            Slog.d(TAG, "Remembering install states:");
            for (int i2 : iArr) {
                Slog.d(TAG, "   u=" + i2 + " inst=" + ArrayUtils.contains(packageRemovedInfo.origUsers, i2));
            }
        }
        if (packageRemovedInfo != null) {
            packageRemovedInfo.isRemovedPackageSystemUpdate = true;
            if (packageRemovedInfo.removedChildPackages != null) {
                int size = packageSetting.childPackageNames != null ? packageSetting.childPackageNames.size() : 0;
                for (int i3 = 0; i3 < size; i3++) {
                    String str = packageSetting.childPackageNames.get(i3);
                    if (disabledSystemPkgLPr.childPackageNames != null && disabledSystemPkgLPr.childPackageNames.contains(str) && (packageRemovedInfo2 = packageRemovedInfo.removedChildPackages.get(str)) != null) {
                        packageRemovedInfo2.isRemovedPackageSystemUpdate = true;
                    }
                }
            }
        }
        if (!deleteInstalledPackageLIF(packageSetting, true, disabledSystemPkgLPr.versionCode < packageSetting.versionCode ? i & (-2) : i | 1, iArr, packageRemovedInfo, z, disabledSystemPkgLPr.pkg)) {
            return false;
        }
        synchronized (this.mPackages) {
            enableSystemPackageLPw(disabledSystemPkgLPr.pkg);
            removeNativeBinariesLI(packageSetting);
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "Re-installing system package: " + disabledSystemPkgLPr);
        }
        try {
            try {
                installPackageFromSystemLIF(disabledSystemPkgLPr.codePathString, false, iArr, packageRemovedInfo.origUsers, packageSetting.getPermissionsState(), z);
                if (disabledSystemPkgLPr.pkg.isStub) {
                    this.mSettings.disableSystemPackageLPw(disabledSystemPkgLPr.name, true);
                }
                return true;
            } catch (PackageManagerException e) {
                Slog.w(TAG, "Failed to restore system package:" + r17.packageName + ": " + e.getMessage());
                if (disabledSystemPkgLPr.pkg.isStub) {
                    this.mSettings.disableSystemPackageLPw(disabledSystemPkgLPr.name, true);
                }
                return false;
            }
        } catch (Throwable th) {
            if (disabledSystemPkgLPr.pkg.isStub) {
                this.mSettings.disableSystemPackageLPw(disabledSystemPkgLPr.name, true);
            }
            throw th;
        }
    }

    private PackageParser.Package installPackageFromSystemLIF(String str, boolean z, int[] iArr, int[] iArr2, PermissionsState permissionsState, boolean z2) throws PackageManagerException {
        int i;
        int i2 = this.mDefParseFlags | 1 | 16;
        if (z || locationIsPrivileged(str)) {
            i = 393216;
        } else {
            i = 131072;
        }
        if (locationIsOem(str)) {
            i |= 524288;
        }
        if (locationIsVendor(str)) {
            i |= 1048576;
        }
        if (locationIsProduct(str)) {
            i |= 2097152;
        }
        PackageParser.Package packageScanPackageTracedLI = scanPackageTracedLI(new File(str), i2, i, 0L, (UserHandle) null);
        try {
            updateSharedLibrariesLPr(packageScanPackageTracedLI, null);
        } catch (PackageManagerException e) {
            Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
        }
        prepareAppDataAfterInstallLIF(packageScanPackageTracedLI);
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(packageScanPackageTracedLI.packageName);
            if (permissionsState != null) {
                packageSetting.getPermissionsState().copyFrom(permissionsState);
            }
            this.mPermissionManager.updatePermissions(packageScanPackageTracedLI.packageName, packageScanPackageTracedLI, true, this.mPackages.values(), this.mPermissionCallback);
            if ((iArr == null || iArr2 == null) ? false : true) {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "Propagating install state across reinstall");
                }
                boolean z3 = false;
                for (int i3 : iArr) {
                    boolean zContains = ArrayUtils.contains(iArr2, i3);
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "    user " + i3 + " => " + zContains);
                    }
                    if (zContains != packageSetting.getInstalled(i3)) {
                        z3 = true;
                    }
                    packageSetting.setInstalled(zContains, i3);
                    this.mSettings.writeRuntimePermissionsForUserLPr(i3, false);
                }
                this.mSettings.writeAllUsersPackageRestrictionsLPr();
                if (z3) {
                    this.mSettings.writeKernelMappingLPr(packageSetting);
                }
            }
            if (z2) {
                this.mSettings.writeLPr();
            }
        }
        return packageScanPackageTracedLI;
    }

    private boolean deleteInstalledPackageLIF(PackageSetting packageSetting, boolean z, int i, int[] iArr, PackageRemovedInfo packageRemovedInfo, boolean z2, PackageParser.Package r19) {
        PackageSetting packageLPr;
        int size;
        synchronized (this.mPackages) {
            if (packageRemovedInfo != null) {
                try {
                    packageRemovedInfo.uid = packageSetting.appId;
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (packageRemovedInfo != null && packageRemovedInfo.removedChildPackages != null) {
                if (packageSetting.childPackageNames != null) {
                    size = packageSetting.childPackageNames.size();
                } else {
                    size = 0;
                }
                for (int i2 = 0; i2 < size; i2++) {
                    String str = packageSetting.childPackageNames.get(i2);
                    PackageSetting packageSetting2 = this.mSettings.mPackages.get(str);
                    if (packageSetting2 == null) {
                        return false;
                    }
                    PackageRemovedInfo packageRemovedInfo2 = packageRemovedInfo.removedChildPackages.get(str);
                    if (packageRemovedInfo2 != null) {
                        packageRemovedInfo2.uid = packageSetting2.appId;
                    }
                }
            }
            removePackageDataLIF(packageSetting, iArr, packageRemovedInfo, i, z2);
            int size2 = packageSetting.childPackageNames != null ? packageSetting.childPackageNames.size() : 0;
            for (int i3 = 0; i3 < size2; i3++) {
                synchronized (this.mPackages) {
                    packageLPr = this.mSettings.getPackageLPr(packageSetting.childPackageNames.get(i3));
                }
                if (packageLPr != null) {
                    removePackageDataLIF(packageLPr, iArr, (packageRemovedInfo == null || packageRemovedInfo.removedChildPackages == null) ? null : packageRemovedInfo.removedChildPackages.get(packageLPr.name), ((i & 1) == 0 || r19 == null || r19.hasChildPackage(packageLPr.name)) ? i : i & (-2), z2);
                }
            }
            if (packageSetting.parentPackageName == null && z && packageRemovedInfo != null) {
                packageRemovedInfo.args = createInstallArgsForExisting(packageFlagsToInstallFlags(packageSetting), packageSetting.codePathString, packageSetting.resourcePathString, InstructionSets.getAppDexInstructionSets(packageSetting));
                return true;
            }
            return true;
        }
    }

    public boolean setBlockUninstallForUser(String str, boolean z, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        synchronized (this.mPackages) {
            PackageParser.Package r1 = this.mPackages.get(str);
            if (r1 != null && r1.staticSharedLibName != null) {
                Slog.w(TAG, "Cannot block uninstall of package: " + str + " providing static shared library: " + r1.staticSharedLibName);
                return false;
            }
            this.mSettings.setBlockUninstallLPw(i, str, z);
            this.mSettings.writePackageRestrictionsLPr(i);
            return true;
        }
    }

    public boolean getBlockUninstallForUser(String str, int i) {
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting != null && !filterAppAccessLPr(packageSetting, Binder.getCallingUid(), i)) {
                return this.mSettings.getBlockUninstallLPr(i, str);
            }
            return false;
        }
    }

    public boolean setRequiredForSystemUser(String str, boolean z) {
        enforceSystemOrRoot("setRequiredForSystemUser can only be run by the system or root");
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                Log.w(TAG, "Package doesn't exist: " + str);
                return false;
            }
            if (z) {
                packageSetting.pkgPrivateFlags |= 512;
            } else {
                packageSetting.pkgPrivateFlags &= -513;
            }
            this.mSettings.writeLPr();
            return true;
        }
    }

    private boolean deletePackageLIF(String str, UserHandle userHandle, boolean z, int[] iArr, int i, PackageRemovedInfo packageRemovedInfo, boolean z2, PackageParser.Package r23) {
        int identifier;
        boolean zDeleteInstalledPackageLIF;
        int size;
        PackageSetting packageLPr;
        if (str == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "deletePackageLI: " + str + " user " + userHandle);
        }
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                Slog.w(TAG, "Package named '" + str + "' doesn't exist.");
                return false;
            }
            if (packageSetting.parentPackageName != null && (!isSystemApp(packageSetting) || (i & 4) != 0)) {
                if (DEBUG_REMOVE) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Uninstalled child package:");
                    sb.append(str);
                    sb.append(" for user:");
                    sb.append(userHandle == null ? -1 : userHandle);
                    Slog.d(TAG, sb.toString());
                }
                if (!clearPackageStateForUserLIF(packageSetting, userHandle != null ? userHandle.getIdentifier() : -1, packageRemovedInfo)) {
                    return false;
                }
                markPackageUninstalledForUserLPw(packageSetting, userHandle);
                scheduleWritePackageRestrictionsLocked(userHandle);
                return true;
            }
            if (userHandle != null) {
                identifier = userHandle.getIdentifier();
            } else {
                identifier = -1;
            }
            if (checkPermission("android.permission.SUSPEND_APPS", str, identifier) == 0) {
                unsuspendForSuspendingPackage(str, identifier);
            }
            if ((!isSystemApp(packageSetting) || (i & 4) != 0) && userHandle != null && userHandle.getIdentifier() != -1) {
                markPackageUninstalledForUserLPw(packageSetting, userHandle);
                if (!isSystemApp(packageSetting)) {
                    boolean zShouldKeepUninstalledPackageLPr = shouldKeepUninstalledPackageLPr(str);
                    if (packageSetting.isAnyInstalled(sUserManager.getUserIds()) || zShouldKeepUninstalledPackageLPr) {
                        if (DEBUG_REMOVE) {
                            Slog.d(TAG, "Still installed by other users");
                        }
                        if (!clearPackageStateForUserLIF(packageSetting, userHandle.getIdentifier(), packageRemovedInfo)) {
                            return false;
                        }
                        scheduleWritePackageRestrictionsLocked(userHandle);
                        return true;
                    }
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Not installed by other users, full delete");
                    }
                    packageSetting.setInstalled(true, userHandle.getIdentifier());
                    this.mSettings.writeKernelMappingLPr(packageSetting);
                } else {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Deleting system app");
                    }
                    if (!clearPackageStateForUserLIF(packageSetting, userHandle.getIdentifier(), packageRemovedInfo)) {
                        return false;
                    }
                    scheduleWritePackageRestrictionsLocked(userHandle);
                    return true;
                }
            }
            if (packageSetting.childPackageNames != null && packageRemovedInfo != null) {
                synchronized (this.mPackages) {
                    int size2 = packageSetting.childPackageNames.size();
                    packageRemovedInfo.removedChildPackages = new ArrayMap<>(size2);
                    for (int i2 = 0; i2 < size2; i2++) {
                        String str2 = packageSetting.childPackageNames.get(i2);
                        PackageRemovedInfo packageRemovedInfo2 = new PackageRemovedInfo(this);
                        packageRemovedInfo2.removedPackage = str2;
                        packageRemovedInfo2.installerPackageName = packageSetting.installerPackageName;
                        packageRemovedInfo.removedChildPackages.put(str2, packageRemovedInfo2);
                        PackageSetting packageLPr2 = this.mSettings.getPackageLPr(str2);
                        if (packageLPr2 != null) {
                            packageRemovedInfo2.origUsers = packageLPr2.queryInstalledUsers(iArr, true);
                        }
                    }
                }
            }
            if (isSystemApp(packageSetting) && !sPmsExt.isRemovableSysApp(str)) {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "Removing system package: " + packageSetting.name);
                }
                zDeleteInstalledPackageLIF = deleteSystemPackageLIF(packageSetting.pkg, packageSetting, iArr, i, packageRemovedInfo, z2);
            } else {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "Removing non-system package: " + packageSetting.name);
                }
                zDeleteInstalledPackageLIF = deleteInstalledPackageLIF(packageSetting, z, i, iArr, packageRemovedInfo, z2, r23);
            }
            if (packageRemovedInfo != null) {
                packageRemovedInfo.removedForAllUsers = this.mPackages.get(packageSetting.name) == null;
                if (packageRemovedInfo.removedChildPackages != null) {
                    synchronized (this.mPackages) {
                        int size3 = packageRemovedInfo.removedChildPackages.size();
                        for (int i3 = 0; i3 < size3; i3++) {
                            PackageRemovedInfo packageRemovedInfoValueAt = packageRemovedInfo.removedChildPackages.valueAt(i3);
                            if (packageRemovedInfoValueAt != null) {
                                packageRemovedInfoValueAt.removedForAllUsers = this.mPackages.get(packageRemovedInfoValueAt.removedPackage) == null;
                            }
                        }
                    }
                }
                if (isSystemApp(packageSetting) && !sPmsExt.isRemovableSysApp(str)) {
                    synchronized (this.mPackages) {
                        PackageSetting packageLPr3 = this.mSettings.getPackageLPr(packageSetting.name);
                        if (packageLPr3.childPackageNames != null) {
                            size = packageLPr3.childPackageNames.size();
                        } else {
                            size = 0;
                        }
                        for (int i4 = 0; i4 < size; i4++) {
                            String str3 = packageLPr3.childPackageNames.get(i4);
                            if ((packageRemovedInfo.removedChildPackages == null || packageRemovedInfo.removedChildPackages.indexOfKey(str3) < 0) && (packageLPr = this.mSettings.getPackageLPr(str3)) != null) {
                                PackageInstalledInfo packageInstalledInfo = new PackageInstalledInfo();
                                packageInstalledInfo.name = str3;
                                packageInstalledInfo.newUsers = packageLPr.queryInstalledUsers(iArr, true);
                                packageInstalledInfo.pkg = this.mPackages.get(str3);
                                packageInstalledInfo.uid = packageLPr.pkg.applicationInfo.uid;
                                if (packageRemovedInfo.appearedChildPackages == null) {
                                    packageRemovedInfo.appearedChildPackages = new ArrayMap<>();
                                }
                                packageRemovedInfo.appearedChildPackages.put(str3, packageInstalledInfo);
                            }
                        }
                    }
                }
            }
            return zDeleteInstalledPackageLIF;
        }
    }

    private void markPackageUninstalledForUserLPw(PackageSetting packageSetting, UserHandle userHandle) {
        PackageSetting packageSetting2 = packageSetting;
        int[] userIds = (userHandle == null || userHandle.getIdentifier() == -1) ? sUserManager.getUserIds() : new int[]{userHandle.getIdentifier()};
        int length = userIds.length;
        int i = 0;
        while (i < length) {
            int i2 = userIds[i];
            if (DEBUG_REMOVE) {
                Slog.d(TAG, "Marking package:" + packageSetting2.name + " uninstalled for user:" + i2);
            }
            packageSetting2.setUserState(i2, 0L, 0, false, true, true, false, false, null, null, null, null, false, false, null, null, null, packageSetting2.readUserState(i2).domainVerificationStatus, 0, 0, null);
            i++;
            length = length;
            userIds = userIds;
            packageSetting2 = packageSetting;
        }
        this.mSettings.writeKernelMappingLPr(packageSetting);
    }

    private boolean clearPackageStateForUserLIF(PackageSetting packageSetting, int i, PackageRemovedInfo packageRemovedInfo) {
        PackageParser.Package r1;
        synchronized (this.mPackages) {
            r1 = this.mPackages.get(packageSetting.name);
        }
        boolean z = false;
        int[] userIds = i == -1 ? sUserManager.getUserIds() : new int[]{i};
        for (int i2 : userIds) {
            if (DEBUG_REMOVE) {
                Slog.d(TAG, "Updating package:" + packageSetting.name + " install state for user:" + i2);
            }
            destroyAppDataLIF(r1, i, 3);
            destroyAppProfilesLIF(r1, i);
            clearDefaultBrowserIfNeededForUser(packageSetting.name, i);
            removeKeystoreDataIfNeeded(i2, packageSetting.appId);
            schedulePackageCleaning(packageSetting.name, i2, false);
            synchronized (this.mPackages) {
                if (clearPackagePreferredActivitiesLPw(packageSetting.name, i2)) {
                    scheduleWritePackageRestrictionsLocked(i2);
                }
                resetUserChangesToRuntimePermissionsAndFlagsLPw(packageSetting, i2);
            }
        }
        if (packageRemovedInfo != null) {
            packageRemovedInfo.removedPackage = packageSetting.name;
            packageRemovedInfo.installerPackageName = packageSetting.installerPackageName;
            if (r1 != null && r1.staticSharedLibName != null) {
                z = true;
            }
            packageRemovedInfo.isStaticSharedLib = z;
            packageRemovedInfo.removedAppId = packageSetting.appId;
            packageRemovedInfo.removedUsers = userIds;
            packageRemovedInfo.broadcastUsers = userIds;
        }
        return true;
    }

    private final class ClearStorageConnection implements ServiceConnection {
        IMediaContainerService mContainerService;

        private ClearStorageConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this) {
                this.mContainerService = IMediaContainerService.Stub.asInterface(Binder.allowBlocking(iBinder));
                notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    }

    private void clearExternalStorageDataSync(String str, int i, boolean z) {
        boolean z2;
        int[] userIds;
        if (DEFAULT_CONTAINER_PACKAGE.equals(str)) {
            return;
        }
        if (!Environment.isExternalStorageEmulated()) {
            String externalStorageState = Environment.getExternalStorageState();
            if (!externalStorageState.equals("mounted") && !externalStorageState.equals("mounted_ro")) {
                z2 = false;
            }
        } else {
            z2 = true;
        }
        if (!z2) {
            return;
        }
        Intent component = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        if (i == -1) {
            userIds = sUserManager.getUserIds();
        } else {
            userIds = new int[]{i};
        }
        ClearStorageConnection clearStorageConnection = new ClearStorageConnection();
        if (this.mContext.bindServiceAsUser(component, clearStorageConnection, 1, UserHandle.SYSTEM)) {
            try {
                for (int i2 : userIds) {
                    long jUptimeMillis = SystemClock.uptimeMillis() + 5000;
                    synchronized (clearStorageConnection) {
                        while (clearStorageConnection.mContainerService == null) {
                            long jUptimeMillis2 = SystemClock.uptimeMillis();
                            if (jUptimeMillis2 >= jUptimeMillis) {
                                break;
                            } else {
                                try {
                                    clearStorageConnection.wait(jUptimeMillis - jUptimeMillis2);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    if (clearStorageConnection.mContainerService == null) {
                        return;
                    }
                    Environment.UserEnvironment userEnvironment = new Environment.UserEnvironment(i2);
                    clearDirectory(clearStorageConnection.mContainerService, userEnvironment.buildExternalStorageAppCacheDirs(str));
                    if (z) {
                        clearDirectory(clearStorageConnection.mContainerService, userEnvironment.buildExternalStorageAppDataDirs(str));
                        clearDirectory(clearStorageConnection.mContainerService, userEnvironment.buildExternalStorageAppMediaDirs(str));
                    }
                }
            } finally {
                this.mContext.unbindService(clearStorageConnection);
            }
        }
    }

    public void clearApplicationProfileData(String str) throws Exception {
        PackageParser.Package r1;
        enforceSystemOrRoot("Only the system can clear all profile data");
        synchronized (this.mPackages) {
            r1 = this.mPackages.get(str);
        }
        PackageFreezer packageFreezerFreezePackage = freezePackage(str, "clearApplicationProfileData");
        Throwable th = null;
        try {
            synchronized (this.mInstallLock) {
                clearAppProfilesLIF(r1, -1);
            }
        } finally {
            if (packageFreezerFreezePackage != null) {
                $closeResource(th, packageFreezerFreezePackage);
            }
        }
    }

    public void clearApplicationUserData(final String str, final IPackageDataObserver iPackageDataObserver, final int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_USER_DATA", null);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "clear application data");
        PackageSetting packageLPr = this.mSettings.getPackageLPr(str);
        final boolean z = packageLPr != null && filterAppAccessLPr(packageLPr, callingUid, i);
        if (!z && this.mProtectedPackages.isPackageDataProtected(i, str)) {
            throw new SecurityException("Cannot clear data for a protected package: " + str);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean zClearApplicationUserDataLIF;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                if (!z) {
                    PackageFreezer packageFreezerFreezePackage = PackageManagerService.this.freezePackage(str, "clearApplicationUserData");
                    Throwable th = null;
                    try {
                        synchronized (PackageManagerService.this.mInstallLock) {
                            zClearApplicationUserDataLIF = PackageManagerService.this.clearApplicationUserDataLIF(str, i);
                        }
                        PackageManagerService.this.clearExternalStorageDataSync(str, i, true);
                        synchronized (PackageManagerService.this.mPackages) {
                            PackageManagerService.this.mInstantAppRegistry.deleteInstantApplicationMetadataLPw(str, i);
                        }
                        if (packageFreezerFreezePackage != null) {
                            packageFreezerFreezePackage.close();
                        }
                        if (zClearApplicationUserDataLIF) {
                            DeviceStorageMonitorInternal deviceStorageMonitorInternal = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
                            if (deviceStorageMonitorInternal != null) {
                                deviceStorageMonitorInternal.checkMemory();
                            }
                            if (PackageManagerService.this.checkPermission("android.permission.SUSPEND_APPS", str, i) == 0) {
                                PackageManagerService.this.unsuspendForSuspendingPackage(str, i);
                            }
                        }
                    } catch (Throwable th2) {
                        if (packageFreezerFreezePackage != null) {
                            if (0 != 0) {
                                try {
                                    packageFreezerFreezePackage.close();
                                } catch (Throwable th3) {
                                    th.addSuppressed(th3);
                                }
                            } else {
                                packageFreezerFreezePackage.close();
                            }
                        }
                        throw th2;
                    }
                } else {
                    zClearApplicationUserDataLIF = false;
                }
                if (iPackageDataObserver != null) {
                    try {
                        iPackageDataObserver.onRemoveCompleted(str, zClearApplicationUserDataLIF);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            }
        });
    }

    private boolean clearApplicationUserDataLIF(String str, int i) {
        PackageSetting packageSetting;
        if (str == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package r2 = this.mPackages.get(str);
            if (r2 == null && (packageSetting = this.mSettings.mPackages.get(str)) != null) {
                r2 = packageSetting.pkg;
            }
            if (r2 == null) {
                Slog.w(TAG, "Package named '" + str + "' doesn't exist.");
                return false;
            }
            resetUserChangesToRuntimePermissionsAndFlagsLPw((PackageSetting) r2.mExtras, i);
            int i2 = 3;
            clearAppDataLIF(r2, i, 3);
            removeKeystoreDataIfNeeded(i, UserHandle.getAppId(r2.applicationInfo.uid));
            UserManagerInternal userManagerInternal = getUserManagerInternal();
            if (!userManagerInternal.isUserUnlockingOrUnlocked(i)) {
                i2 = userManagerInternal.isUserRunning(i) ? 1 : 0;
            }
            prepareAppDataContentsLIF(r2, i, i2);
            return true;
        }
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(int i) {
        int size = this.mPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            resetUserChangesToRuntimePermissionsAndFlagsLPw((PackageSetting) this.mPackages.valueAt(i2).mExtras, i);
        }
    }

    private void resetNetworkPolicies(int i) {
        ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).resetUserState(i);
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(PackageSetting packageSetting, final int i) {
        boolean z;
        if (packageSetting.pkg == null) {
            return;
        }
        int size = packageSetting.pkg.requestedPermissions.size();
        boolean zIsPackageNeedsReview = isPackageNeedsReview(packageSetting.pkg, this.mSettings);
        boolean z2 = false;
        boolean z3 = false;
        for (int i2 = 0; i2 < size; i2++) {
            String str = (String) packageSetting.pkg.requestedPermissions.get(i2);
            BasePermission permissionTEMP = this.mPermissionManager.getPermissionTEMP(str);
            if (permissionTEMP != null) {
                if (packageSetting.sharedUser != null) {
                    int size2 = packageSetting.sharedUser.packages.size();
                    int i3 = 0;
                    while (true) {
                        if (i3 < size2) {
                            PackageSetting packageSettingValueAt = packageSetting.sharedUser.packages.valueAt(i3);
                            if (packageSettingValueAt.pkg == null || packageSettingValueAt.pkg.packageName.equals(packageSetting.pkg.packageName) || !packageSettingValueAt.pkg.requestedPermissions.contains(str)) {
                                i3++;
                            } else {
                                z = true;
                            }
                        } else {
                            z = false;
                        }
                    }
                    if (!z) {
                        PermissionsState permissionsState = packageSetting.getPermissionsState();
                        int permissionFlags = permissionsState.getPermissionFlags(str, i);
                        boolean z4 = permissionsState.getInstallPermissionState(str) != null;
                        int i4 = (!sCtaManager.isCtaSupported() ? !(!this.mSettings.mPermissions.mPermissionReviewRequired || packageSetting.pkg.applicationInfo.targetSdkVersion >= 23) : zIsPackageNeedsReview && permissionTEMP.isRuntime() && sCtaManager.isPlatformPermission(permissionTEMP.getSourcePackageName(), permissionTEMP.getName()) && (permissionFlags & 16) == 0) ? 0 : 64;
                        if (permissionsState.updatePermissionFlags(permissionTEMP, i, 75, i4)) {
                            if (z4) {
                                z3 = true;
                            } else {
                                z2 = true;
                            }
                        }
                        if (permissionTEMP.isRuntime() && (permissionFlags & 20) == 0) {
                            if ((permissionFlags & 32) != 0) {
                                if (permissionsState.grantRuntimePermission(permissionTEMP, i) != -1) {
                                    z2 = true;
                                }
                            } else if ((i4 & 64) == 0) {
                                switch (permissionsState.revokeRuntimePermission(permissionTEMP, i)) {
                                    case 0:
                                    case 1:
                                        final int i5 = packageSetting.appId;
                                        this.mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                PackageManagerService.this.killUid(i5, i, PackageManagerService.KILL_APP_REASON_PERMISSIONS_REVOKED);
                                            }
                                        });
                                        z2 = true;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (z2) {
            this.mSettings.writeRuntimePermissionsForUserLPr(i, true);
        }
        if (z3) {
            this.mSettings.writeLPr();
        }
    }

    private static void removeKeystoreDataIfNeeded(int i, int i2) {
        if (i2 < 0) {
            return;
        }
        KeyStore keyStore = KeyStore.getInstance();
        if (keyStore != null) {
            if (i == -1) {
                for (int i3 : sUserManager.getUserIds()) {
                    keyStore.clearUid(UserHandle.getUid(i3, i2));
                }
                return;
            }
            keyStore.clearUid(UserHandle.getUid(i, i2));
            return;
        }
        Slog.w(TAG, "Could not contact keystore to clear entries for app id " + i2);
    }

    public void deleteApplicationCacheFiles(String str, IPackageDataObserver iPackageDataObserver) {
        deleteApplicationCacheFilesAsUser(str, UserHandle.getCallingUserId(), iPackageDataObserver);
    }

    public void deleteApplicationCacheFilesAsUser(final String str, final int i, final IPackageDataObserver iPackageDataObserver) {
        final PackageParser.Package r2;
        final int callingUid = Binder.getCallingUid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_DELETE_CACHE_FILES") != 0) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_CACHE_FILES") == 0) {
                Slog.w(TAG, "Calling uid " + callingUid + " does not have android.permission.INTERNAL_DELETE_CACHE_FILES, silently ignoring");
                return;
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNAL_DELETE_CACHE_FILES", null);
        }
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "delete application cache files");
        final int iCheckCallingOrSelfPermission = this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS");
        synchronized (this.mPackages) {
            r2 = this.mPackages.get(str);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageSetting packageSetting = r2 == null ? null : (PackageSetting) r2.mExtras;
                if (packageSetting == null || !packageSetting.getInstantApp(UserHandle.getUserId(callingUid)) || iCheckCallingOrSelfPermission == 0) {
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.clearAppDataLIF(r2, i, 259);
                        PackageManagerService.this.clearAppDataLIF(r2, i, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                    }
                    PackageManagerService.this.clearExternalStorageDataSync(str, i, false);
                }
                if (iPackageDataObserver != null) {
                    try {
                        iPackageDataObserver.onRemoveCompleted(str, true);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            }
        });
    }

    public void getPackageSizeInfo(String str, int i, IPackageStatsObserver iPackageStatsObserver) {
        throw new UnsupportedOperationException("Shame on you for calling the hidden API getPackageSizeInfo(). Shame!");
    }

    private boolean getPackageSizeInfoLI(String str, int i, PackageStats packageStats) {
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                Slog.w(TAG, "Failed to find settings for " + str);
                return false;
            }
            try {
                this.mInstaller.getAppSize(packageSetting.volumeUuid, new String[]{str}, i, 0, packageSetting.appId, new long[]{packageSetting.getCeDataInode(i)}, new String[]{packageSetting.codePathString}, packageStats);
                if (isSystemApp(packageSetting) && !isUpdatedSystemApp(packageSetting)) {
                    packageStats.codeSize = 0L;
                }
                packageStats.dataSize -= packageStats.cacheSize;
                return true;
            } catch (Installer.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
                return false;
            }
        }
    }

    private int getUidTargetSdkVersionLockedLPr(int i) {
        int i2;
        Object userIdLPr = this.mSettings.getUserIdLPr(i);
        int i3 = 10000;
        if (userIdLPr instanceof SharedUserSetting) {
            for (PackageSetting packageSetting : ((SharedUserSetting) userIdLPr).packages) {
                if (packageSetting.pkg != null && (i2 = packageSetting.pkg.applicationInfo.targetSdkVersion) < i3) {
                    i3 = i2;
                }
            }
            return i3;
        }
        if (userIdLPr instanceof PackageSetting) {
            PackageSetting packageSetting2 = (PackageSetting) userIdLPr;
            if (packageSetting2.pkg != null) {
                return packageSetting2.pkg.applicationInfo.targetSdkVersion;
            }
        }
        return 10000;
    }

    private int getPackageTargetSdkVersionLockedLPr(String str) {
        PackageParser.Package r2 = this.mPackages.get(str);
        if (r2 != null) {
            return r2.applicationInfo.targetSdkVersion;
        }
        return 10000;
    }

    public void addPreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) {
        addPreferredActivityInternal(intentFilter, i, componentNameArr, componentName, true, i2, "Adding preferred");
    }

    private void addPreferredActivityInternal(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, boolean z, int i2, String str) {
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, true, false, "add preferred activity");
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a preferred activity with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                    Slog.w(TAG, "Ignoring addPreferredActivity() from uid " + callingUid);
                    return;
                }
                this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            }
            PreferredIntentResolver preferredIntentResolverEditPreferredActivitiesLPw = this.mSettings.editPreferredActivitiesLPw(i2);
            Slog.i(TAG, str + " activity " + componentName.flattenToShortString() + " for user " + i2 + ":");
            intentFilter.dump(new LogPrinter(4, TAG), "  ");
            preferredIntentResolverEditPreferredActivitiesLPw.addFilter(new PreferredActivity(intentFilter, i, componentNameArr, componentName, z));
            scheduleWritePackageRestrictionsLocked(i2);
            postPreferredActivityChangedBroadcast(i2);
        }
    }

    private void postPreferredActivityChangedBroadcast(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                PackageManagerService.lambda$postPreferredActivityChangedBroadcast$8(i);
            }
        });
    }

    static void lambda$postPreferredActivityChangedBroadcast$8(int i) {
        IActivityManager service = ActivityManager.getService();
        if (service == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
        intent.putExtra("android.intent.extra.user_handle", i);
        try {
            service.broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, i);
        } catch (RemoteException e) {
        }
    }

    public void replacePreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) {
        ComponentName[] componentNameArr2;
        ComponentName componentName2;
        if (intentFilter.countActions() != 1) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have only 1 action.");
        }
        if (intentFilter.countDataAuthorities() != 0 || intentFilter.countDataPaths() != 0 || intentFilter.countDataSchemes() > 1 || intentFilter.countDataTypes() != 0) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have no data authorities, paths, or types; and at most one scheme.");
        }
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, true, false, "replace preferred activity");
        synchronized (this.mPackages) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                    Slog.w(TAG, "Ignoring replacePreferredActivity() from uid " + Binder.getCallingUid());
                    return;
                }
                this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            }
            PreferredIntentResolver preferredIntentResolver = this.mSettings.mPreferredActivities.get(i2);
            if (preferredIntentResolver != null) {
                ArrayList<PreferredActivity> arrayListFindFilters = preferredIntentResolver.findFilters(intentFilter);
                if (arrayListFindFilters == null || arrayListFindFilters.size() != 1) {
                    componentNameArr2 = componentNameArr;
                    componentName2 = componentName;
                    if (arrayListFindFilters != null) {
                        if (DEBUG_PREFERRED) {
                            Slog.i(TAG, arrayListFindFilters.size() + " existing preferred matches for:");
                            intentFilter.dump(new LogPrinter(4, TAG), "  ");
                        }
                        for (int i3 = 0; i3 < arrayListFindFilters.size(); i3++) {
                            PreferredActivity preferredActivity = arrayListFindFilters.get(i3);
                            if (DEBUG_PREFERRED) {
                                Slog.i(TAG, "Removing existing preferred activity " + preferredActivity.mPref.mComponent + ":");
                                preferredActivity.dump(new LogPrinter(4, TAG), "  ");
                            }
                            preferredIntentResolver.removeFilter(preferredActivity);
                        }
                    }
                } else {
                    PreferredActivity preferredActivity2 = arrayListFindFilters.get(0);
                    if (DEBUG_PREFERRED) {
                        Slog.i(TAG, "Checking replace of preferred:");
                        intentFilter.dump(new LogPrinter(4, TAG), "  ");
                        if (!preferredActivity2.mPref.mAlways) {
                            Slog.i(TAG, "  -- CUR; not mAlways!");
                        } else {
                            Slog.i(TAG, "  -- CUR: mMatch=" + preferredActivity2.mPref.mMatch);
                            Slog.i(TAG, "  -- CUR: mSet=" + Arrays.toString(preferredActivity2.mPref.mSetComponents));
                            Slog.i(TAG, "  -- CUR: mComponent=" + preferredActivity2.mPref.mShortComponent);
                            Slog.i(TAG, "  -- NEW: mMatch=" + (i & 268369920));
                            Slog.i(TAG, "  -- CUR: mSet=" + Arrays.toString(componentNameArr));
                            Slog.i(TAG, "  -- CUR: mComponent=" + componentName.flattenToShortString());
                        }
                    }
                    if (preferredActivity2.mPref.mAlways) {
                        componentName2 = componentName;
                        if (preferredActivity2.mPref.mComponent.equals(componentName2) && preferredActivity2.mPref.mMatch == (i & 268369920)) {
                            componentNameArr2 = componentNameArr;
                            if (preferredActivity2.mPref.sameSet(componentNameArr2)) {
                                if (DEBUG_PREFERRED) {
                                    Slog.i(TAG, "Replacing with same preferred activity " + preferredActivity2.mPref.mShortComponent + " for user " + i2 + ":");
                                    intentFilter.dump(new LogPrinter(4, TAG), "  ");
                                }
                                return;
                            }
                        } else {
                            componentNameArr2 = componentNameArr;
                        }
                    }
                    if (arrayListFindFilters != null) {
                    }
                }
            } else {
                componentNameArr2 = componentNameArr;
                componentName2 = componentName;
            }
            addPreferredActivityInternal(intentFilter, i, componentNameArr2, componentName2, true, i2, "Replacing preferred");
        }
    }

    public void clearPackagePreferredActivities(String str) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return;
        }
        synchronized (this.mPackages) {
            PackageParser.Package r2 = this.mPackages.get(str);
            if ((r2 == null || r2.applicationInfo.uid != callingUid) && this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                    Slog.w(TAG, "Ignoring clearPackagePreferredActivities() from uid " + callingUid);
                    return;
                }
                this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            }
            PackageSetting packageLPr = this.mSettings.getPackageLPr(str);
            if (packageLPr == null || !filterAppAccessLPr(packageLPr, callingUid, UserHandle.getUserId(callingUid))) {
                int callingUserId = UserHandle.getCallingUserId();
                if (clearPackagePreferredActivitiesLPw(str, callingUserId)) {
                    scheduleWritePackageRestrictionsLocked(callingUserId);
                }
            }
        }
    }

    boolean clearPackagePreferredActivitiesLPw(String str, int i) {
        boolean z = false;
        ArrayList arrayList = null;
        for (int i2 = 0; i2 < this.mSettings.mPreferredActivities.size(); i2++) {
            int iKeyAt = this.mSettings.mPreferredActivities.keyAt(i2);
            PreferredIntentResolver preferredIntentResolverValueAt = this.mSettings.mPreferredActivities.valueAt(i2);
            if (i == -1 || i == iKeyAt) {
                Iterator<PreferredActivity> itFilterIterator = preferredIntentResolverValueAt.filterIterator();
                while (itFilterIterator.hasNext()) {
                    PreferredActivity next = itFilterIterator.next();
                    if (str == null || (next.mPref.mComponent.getPackageName().equals(str) && next.mPref.mAlways)) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(next);
                    }
                }
                if (arrayList != null) {
                    for (int i3 = 0; i3 < arrayList.size(); i3++) {
                        preferredIntentResolverValueAt.removeFilter((PreferredActivity) arrayList.get(i3));
                    }
                    z = true;
                }
            }
        }
        if (z) {
            postPreferredActivityChangedBroadcast(i);
        }
        return z;
    }

    private void clearIntentFilterVerificationsLPw(int i) {
        int size = this.mPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            clearIntentFilterVerificationsLPw(this.mPackages.valueAt(i2).packageName, i, true);
        }
    }

    void clearIntentFilterVerificationsLPw(String str, int i, boolean z) {
        if (i == -1) {
            if (this.mSettings.removeIntentFilterVerificationLPw(str, sUserManager.getUserIds())) {
                for (int i2 : sUserManager.getUserIds()) {
                    scheduleWritePackageRestrictionsLocked(i2);
                }
                return;
            }
            return;
        }
        if (this.mSettings.removeIntentFilterVerificationLPw(str, i, z)) {
            scheduleWritePackageRestrictionsLocked(i);
        }
    }

    void clearDefaultBrowserIfNeeded(String str) {
        for (int i : sUserManager.getUserIds()) {
            clearDefaultBrowserIfNeededForUser(str, i);
        }
    }

    private void clearDefaultBrowserIfNeededForUser(String str, int i) {
        String defaultBrowserPackageName = getDefaultBrowserPackageName(i);
        if (!TextUtils.isEmpty(defaultBrowserPackageName) && str.equals(defaultBrowserPackageName)) {
            setDefaultBrowserPackageName(null, i);
        }
    }

    public void resetApplicationPreferences(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                clearPackagePreferredActivitiesLPw(null, i);
                this.mSettings.applyDefaultPreferredAppsLPw(this, i);
                applyFactoryDefaultBrowserLPw(i);
                clearIntentFilterVerificationsLPw(i);
                primeDomainVerificationsLPw(i);
                resetUserChangesToRuntimePermissionsAndFlagsLPw(i);
                scheduleWritePackageRestrictionsLocked(i);
            }
            resetNetworkPolicies(i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getPreferredActivities(List<IntentFilter> list, List<ComponentName> list2, String str) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return 0;
        }
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mPackages) {
            PreferredIntentResolver preferredIntentResolver = this.mSettings.mPreferredActivities.get(callingUserId);
            if (preferredIntentResolver != null) {
                Iterator<PreferredActivity> itFilterIterator = preferredIntentResolver.filterIterator();
                while (itFilterIterator.hasNext()) {
                    PreferredActivity next = itFilterIterator.next();
                    if (str == null || (next.mPref.mComponent.getPackageName().equals(str) && next.mPref.mAlways)) {
                        if (list != null) {
                            list.add(new IntentFilter(next));
                        }
                        if (list2 != null) {
                            list2.add(next.mPref.mComponent);
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void addPersistentPreferredActivity(IntentFilter intentFilter, ComponentName componentName, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("addPersistentPreferredActivity can only be run by the system");
        }
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a preferred activity with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            Slog.i(TAG, "Adding persistent preferred activity " + componentName + " for user " + i + ":");
            intentFilter.dump(new LogPrinter(4, TAG), "  ");
            this.mSettings.editPersistentPreferredActivitiesLPw(i).addFilter(new PersistentPreferredActivity(intentFilter, componentName));
            scheduleWritePackageRestrictionsLocked(i);
            postPreferredActivityChangedBroadcast(i);
        }
    }

    public void clearPackagePersistentPreferredActivities(String str, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("clearPackagePersistentPreferredActivities can only be run by the system");
        }
        synchronized (this.mPackages) {
            ArrayList arrayList = null;
            boolean z = false;
            for (int i2 = 0; i2 < this.mSettings.mPersistentPreferredActivities.size(); i2++) {
                int iKeyAt = this.mSettings.mPersistentPreferredActivities.keyAt(i2);
                PersistentPreferredIntentResolver persistentPreferredIntentResolverValueAt = this.mSettings.mPersistentPreferredActivities.valueAt(i2);
                if (i == iKeyAt) {
                    Iterator<PersistentPreferredActivity> itFilterIterator = persistentPreferredIntentResolverValueAt.filterIterator();
                    while (itFilterIterator.hasNext()) {
                        PersistentPreferredActivity next = itFilterIterator.next();
                        if (next.mComponent.getPackageName().equals(str)) {
                            if (arrayList == null) {
                                arrayList = new ArrayList();
                            }
                            arrayList.add(next);
                        }
                    }
                    if (arrayList != null) {
                        for (int i3 = 0; i3 < arrayList.size(); i3++) {
                            persistentPreferredIntentResolverValueAt.removeFilter((PersistentPreferredActivity) arrayList.get(i3));
                        }
                        z = true;
                    }
                }
            }
            if (z) {
                scheduleWritePackageRestrictionsLocked(i);
                postPreferredActivityChangedBroadcast(i);
            }
        }
    }

    private void restoreFromXml(XmlPullParser xmlPullParser, int i, String str, BlobXmlRestorer blobXmlRestorer) throws XmlPullParserException, IOException {
        int next;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Didn't find start tag during restore");
                return;
            }
            return;
        }
        Slog.v(TAG, ":: restoreFromXml() : got to tag " + xmlPullParser.getName());
        if (!str.equals(xmlPullParser.getName())) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Found unexpected tag " + xmlPullParser.getName());
                return;
            }
            return;
        }
        while (xmlPullParser.next() == 4) {
        }
        Slog.v(TAG, ":: stepped forward, applying functor at tag " + xmlPullParser.getName());
        blobXmlRestorer.apply(xmlPullParser, i);
    }

    public byte[] getPreferredActivityBackup(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPreferredActivityBackup()");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_PREFERRED_BACKUP);
            synchronized (this.mPackages) {
                this.mSettings.writePreferredActivitiesLPr(fastXmlSerializer, i, true);
            }
            fastXmlSerializer.endTag(null, TAG_PREFERRED_BACKUP);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write preferred activities for backup", e);
            }
            return null;
        }
    }

    public void restorePreferredActivities(byte[] bArr, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
            restoreFromXml(xmlPullParserNewPullParser, i, TAG_PREFERRED_BACKUP, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser xmlPullParser, int i2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readPreferredActivitiesLPw(xmlPullParser, i2);
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
            }
        }
    }

    public byte[] getDefaultAppsBackup(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getDefaultAppsBackup()");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_DEFAULT_APPS);
            synchronized (this.mPackages) {
                this.mSettings.writeDefaultAppsLPr(fastXmlSerializer, i);
            }
            fastXmlSerializer.endTag(null, TAG_DEFAULT_APPS);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restoreDefaultApps(byte[] bArr, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restoreDefaultApps()");
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
            restoreFromXml(xmlPullParserNewPullParser, i, TAG_DEFAULT_APPS, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser xmlPullParser, int i2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readDefaultAppsLPw(xmlPullParser, i2);
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Exception restoring default apps: " + e.getMessage());
            }
        }
    }

    public byte[] getIntentFilterVerificationBackup(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getIntentFilterVerificationBackup()");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_INTENT_FILTER_VERIFICATION);
            synchronized (this.mPackages) {
                this.mSettings.writeAllDomainVerificationsLPr(fastXmlSerializer, i);
            }
            fastXmlSerializer.endTag(null, TAG_INTENT_FILTER_VERIFICATION);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restoreIntentFilterVerification(byte[] bArr, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
            restoreFromXml(xmlPullParserNewPullParser, i, TAG_INTENT_FILTER_VERIFICATION, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser xmlPullParser, int i2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readAllDomainVerificationsLPr(xmlPullParser, i2);
                        PackageManagerService.this.mSettings.writeLPr();
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
            }
        }
    }

    public byte[] getPermissionGrantBackup(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPermissionGrantBackup()");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_PERMISSION_BACKUP);
            synchronized (this.mPackages) {
                serializeRuntimePermissionGrantsLPr(fastXmlSerializer, i);
            }
            fastXmlSerializer.endTag(null, TAG_PERMISSION_BACKUP);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restorePermissionGrants(byte[] bArr, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePermissionGrants()");
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(new ByteArrayInputStream(bArr), StandardCharsets.UTF_8.name());
            restoreFromXml(xmlPullParserNewPullParser, i, TAG_PERMISSION_BACKUP, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser xmlPullParser, int i2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.processRestoredPermissionGrantsLPr(xmlPullParser, i2);
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
            }
        }
    }

    private void serializeRuntimePermissionGrantsLPr(XmlSerializer xmlSerializer, int i) throws IOException {
        boolean zIsGranted;
        xmlSerializer.startTag(null, TAG_ALL_GRANTS);
        int size = this.mSettings.mPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            boolean z = false;
            for (PermissionsState.PermissionState permissionState : this.mSettings.mPackages.valueAt(i2).getPermissionsState().getRuntimePermissionStates(i)) {
                int flags = permissionState.getFlags();
                if ((flags & 52) == 0 && ((zIsGranted = permissionState.isGranted()) || (flags & 11) != 0)) {
                    String strKeyAt = this.mSettings.mPackages.keyAt(i2);
                    boolean z2 = true;
                    if (!z) {
                        xmlSerializer.startTag(null, TAG_GRANT);
                        xmlSerializer.attribute(null, ATTR_PACKAGE_NAME, strKeyAt);
                        z = true;
                    }
                    boolean z3 = (flags & 1) != 0;
                    boolean z4 = (flags & 2) != 0;
                    if ((flags & 8) == 0) {
                        z2 = false;
                    }
                    xmlSerializer.startTag(null, TAG_PERMISSION);
                    xmlSerializer.attribute(null, "name", permissionState.getName());
                    if (zIsGranted) {
                        xmlSerializer.attribute(null, ATTR_IS_GRANTED, "true");
                    }
                    if (z3) {
                        xmlSerializer.attribute(null, ATTR_USER_SET, "true");
                    }
                    if (z4) {
                        xmlSerializer.attribute(null, ATTR_USER_FIXED, "true");
                    }
                    if (z2) {
                        xmlSerializer.attribute(null, ATTR_REVOKE_ON_UPGRADE, "true");
                    }
                    xmlSerializer.endTag(null, TAG_PERMISSION);
                }
            }
            if (z) {
                xmlSerializer.endTag(null, TAG_GRANT);
            }
        }
        xmlSerializer.endTag(null, TAG_ALL_GRANTS);
    }

    private void processRestoredPermissionGrantsLPr(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        String str = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                if (name.equals(TAG_GRANT)) {
                    String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                    if (DEBUG_BACKUP) {
                        Slog.v(TAG, "+++ Restoring grants for package " + attributeValue);
                    }
                    str = attributeValue;
                } else if (name.equals(TAG_PERMISSION)) {
                    boolean zEquals = "true".equals(xmlPullParser.getAttributeValue(null, ATTR_IS_GRANTED));
                    String attributeValue2 = xmlPullParser.getAttributeValue(null, "name");
                    int i2 = "true".equals(xmlPullParser.getAttributeValue(null, ATTR_USER_SET)) ? 1 : 0;
                    if ("true".equals(xmlPullParser.getAttributeValue(null, ATTR_USER_FIXED))) {
                        i2 |= 2;
                    }
                    int i3 = "true".equals(xmlPullParser.getAttributeValue(null, ATTR_REVOKE_ON_UPGRADE)) ? i2 | 8 : i2;
                    if (DEBUG_BACKUP) {
                        Slog.v(TAG, "  + Restoring grant: pkg=" + str + " perm=" + attributeValue2 + " granted=" + zEquals + " bits=0x" + Integer.toHexString(i3));
                    }
                    PackageSetting packageSetting = this.mSettings.mPackages.get(str);
                    if (packageSetting != null) {
                        if (DEBUG_BACKUP) {
                            Slog.v(TAG, "        + already installed; applying");
                        }
                        PermissionsState permissionsState = packageSetting.getPermissionsState();
                        BasePermission permissionTEMP = this.mPermissionManager.getPermissionTEMP(attributeValue2);
                        if (permissionTEMP != null) {
                            if (zEquals) {
                                permissionsState.grantRuntimePermission(permissionTEMP, i);
                            }
                            if (i3 != 0) {
                                permissionsState.updatePermissionFlags(permissionTEMP, i, 11, i3);
                            }
                        }
                    } else {
                        if (DEBUG_BACKUP) {
                            Slog.v(TAG, "        - not yet installed; saving for later");
                        }
                        this.mSettings.processRestoredPermissionGrantLPr(str, attributeValue2, zEquals, i3, i);
                    }
                } else {
                    reportSettingsProblem(5, "Unknown element under <perm-grant-backup>: " + name);
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        scheduleWriteSettingsLocked();
        this.mSettings.writeRuntimePermissionsForUserLPr(i, false);
    }

    public void addCrossProfileIntentFilter(IntentFilter intentFilter, String str, int i, int i2, int i3) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(str, callingUid);
        PackageManagerServiceUtils.enforceShellRestriction("no_debugging_features", callingUid, i);
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a crossProfile intent filter with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            CrossProfileIntentFilter crossProfileIntentFilter = new CrossProfileIntentFilter(intentFilter, str, i2, i3);
            CrossProfileIntentResolver crossProfileIntentResolverEditCrossProfileIntentResolverLPw = this.mSettings.editCrossProfileIntentResolverLPw(i);
            ArrayList<CrossProfileIntentFilter> arrayListFindFilters = crossProfileIntentResolverEditCrossProfileIntentResolverLPw.findFilters(intentFilter);
            if (arrayListFindFilters != null) {
                int size = arrayListFindFilters.size();
                for (int i4 = 0; i4 < size; i4++) {
                    if (crossProfileIntentFilter.equalsIgnoreFilter(arrayListFindFilters.get(i4))) {
                        return;
                    }
                }
            }
            crossProfileIntentResolverEditCrossProfileIntentResolverLPw.addFilter(crossProfileIntentFilter);
            scheduleWritePackageRestrictionsLocked(i);
        }
    }

    public void clearCrossProfileIntentFilters(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(str, callingUid);
        PackageManagerServiceUtils.enforceShellRestriction("no_debugging_features", callingUid, i);
        synchronized (this.mPackages) {
            CrossProfileIntentResolver crossProfileIntentResolverEditCrossProfileIntentResolverLPw = this.mSettings.editCrossProfileIntentResolverLPw(i);
            for (CrossProfileIntentFilter crossProfileIntentFilter : new ArraySet(crossProfileIntentResolverEditCrossProfileIntentResolverLPw.filterSet())) {
                if (crossProfileIntentFilter.getOwnerPackage().equals(str)) {
                    crossProfileIntentResolverEditCrossProfileIntentResolverLPw.removeFilter(crossProfileIntentFilter);
                }
            }
            scheduleWritePackageRestrictionsLocked(i);
        }
    }

    private void enforceOwnerRights(String str, int i) {
        if (UserHandle.getAppId(i) == 1000) {
            return;
        }
        int userId = UserHandle.getUserId(i);
        PackageInfo packageInfo = getPackageInfo(str, 0, userId);
        if (packageInfo == null) {
            throw new IllegalArgumentException("Unknown package " + str + " on user " + userId);
        }
        if (!UserHandle.isSameApp(packageInfo.applicationInfo.uid, i)) {
            throw new SecurityException("Calling uid " + i + " does not own package " + str);
        }
    }

    public ComponentName getHomeActivities(List<ResolveInfo> list) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return getHomeActivitiesAsUser(list, UserHandle.getCallingUserId());
    }

    public void sendSessionCommitBroadcast(PackageInstaller.SessionInfo sessionInfo, int i) {
        UserManagerService userManagerService = UserManagerService.getInstance();
        if (userManagerService != null) {
            UserInfo profileParent = userManagerService.getProfileParent(i);
            int i2 = profileParent != null ? profileParent.id : i;
            ComponentName defaultHomeActivity = getDefaultHomeActivity(i2);
            if (defaultHomeActivity != null) {
                this.mContext.sendBroadcastAsUser(new Intent("android.content.pm.action.SESSION_COMMITTED").putExtra("android.content.pm.extra.SESSION", sessionInfo).putExtra("android.intent.extra.USER", UserHandle.of(i)).setPackage(defaultHomeActivity.getPackageName()), UserHandle.of(i2));
            }
        }
    }

    private ComponentName getDefaultHomeActivity(int i) {
        ArrayList arrayList = new ArrayList();
        ComponentName homeActivitiesAsUser = getHomeActivitiesAsUser(arrayList, i);
        if (homeActivitiesAsUser != null) {
            return homeActivitiesAsUser;
        }
        int i2 = Integer.MIN_VALUE;
        int size = arrayList.size();
        ComponentName componentName = null;
        for (int i3 = 0; i3 < size; i3++) {
            ResolveInfo resolveInfo = arrayList.get(i3);
            if (resolveInfo.priority > i2) {
                componentName = resolveInfo.activityInfo.getComponentName();
                i2 = resolveInfo.priority;
            } else if (resolveInfo.priority == i2) {
                componentName = null;
            }
        }
        return componentName;
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getHomeFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MAIN");
        intentFilter.addCategory("android.intent.category.HOME");
        intentFilter.addCategory("android.intent.category.DEFAULT");
        return intentFilter;
    }

    ComponentName getHomeActivitiesAsUser(List<ResolveInfo> list, int i) {
        Intent homeIntent = getHomeIntent();
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(homeIntent, null, 128, i);
        ResolveInfo resolveInfoFindPreferredActivity = findPreferredActivity(homeIntent, null, 0, listQueryIntentActivitiesInternal, 0, true, false, false, i);
        list.clear();
        if (listQueryIntentActivitiesInternal != null) {
            Iterator<ResolveInfo> it = listQueryIntentActivitiesInternal.iterator();
            while (it.hasNext()) {
                list.add(it.next());
            }
        }
        if (resolveInfoFindPreferredActivity == null || resolveInfoFindPreferredActivity.activityInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfoFindPreferredActivity.activityInfo.packageName, resolveInfoFindPreferredActivity.activityInfo.name);
    }

    public void setHomeActivity(ComponentName componentName, int i) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        getHomeActivitiesAsUser(arrayList, i);
        int size = arrayList.size();
        ComponentName[] componentNameArr = new ComponentName[size];
        boolean z = false;
        for (int i2 = 0; i2 < size; i2++) {
            ActivityInfo activityInfo = ((ResolveInfo) arrayList.get(i2)).activityInfo;
            ComponentName componentName2 = new ComponentName(activityInfo.packageName, activityInfo.name);
            componentNameArr[i2] = componentName2;
            if (!z && componentName2.equals(componentName)) {
                z = true;
            }
        }
        if (!z) {
            throw new IllegalArgumentException("Component " + componentName + " cannot be home on user " + i);
        }
        replacePreferredActivity(getHomeFilter(), 1048576, componentNameArr, componentName, i);
    }

    private String getSetupWizardPackageName() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.SETUP_WIZARD");
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(intent, null, 1835520, UserHandle.myUserId());
        if (listQueryIntentActivitiesInternal.size() == 1) {
            return listQueryIntentActivitiesInternal.get(0).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one setup wizard; found " + listQueryIntentActivitiesInternal.size() + ": matches=" + listQueryIntentActivitiesInternal);
        return null;
    }

    private String getStorageManagerPackageName() {
        List<ResolveInfo> listQueryIntentActivitiesInternal = queryIntentActivitiesInternal(new Intent("android.os.storage.action.MANAGE_STORAGE"), null, 1835520, UserHandle.myUserId());
        if (listQueryIntentActivitiesInternal.size() == 1) {
            return listQueryIntentActivitiesInternal.get(0).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one storage manager; found " + listQueryIntentActivitiesInternal.size() + ": matches=" + listQueryIntentActivitiesInternal);
        return null;
    }

    public String getSystemTextClassifierPackageName() {
        return ensureSystemPackageName(this.mContext.getString(R.string.activity_chooser_view_see_all));
    }

    private String ensureSystemPackageName(String str) {
        if (str == null) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (getPackageInfo(str, 2097152, 0) == null) {
                PackageInfo packageInfo = getPackageInfo(str, 0, 0);
                if (packageInfo != null) {
                    EventLog.writeEvent(1397638484, "145981139", Integer.valueOf(packageInfo.applicationInfo.uid), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                return null;
            }
            return str;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setApplicationEnabledSetting(String str, int i, int i2, int i3, String str2) throws Throwable {
        if (sUserManager.exists(i3)) {
            if (str2 == null) {
                str2 = Integer.toString(Binder.getCallingUid());
            }
            setEnabledSetting(str, null, i, i2, i3, str2);
        }
    }

    public void setUpdateAvailable(String str, boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting != null) {
                packageSetting.setUpdateAvailable(z);
            }
        }
    }

    public void setComponentEnabledSetting(ComponentName componentName, int i, int i2, int i3) throws Throwable {
        if (sUserManager.exists(i3)) {
            setEnabledSetting(componentName.getPackageName(), componentName.getClassName(), i, i2, i3, null);
        }
    }

    private void setEnabledSetting(String str, String str2, int i, int i2, int i3, String str3) throws Throwable {
        PackageSetting packageSetting;
        PackageSetting packageSetting2;
        String str4;
        ArrayList<String> arrayList;
        int i4;
        boolean z;
        boolean z2;
        String str5;
        Object obj;
        ?? r15;
        PackageFreezer packageFreezerFreezePackage;
        PackageFreezer packageFreezer;
        String str6;
        String str7;
        ?? r12 = 1;
        if (i != 0 && i != 1 && i != 2 && i != 3 && i != 4) {
            throw new IllegalArgumentException("Invalid new component state: " + i);
        }
        int callingUid = Binder.getCallingUid();
        int iCheckCallingOrSelfPermission = callingUid == 1000 ? 0 : this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i3, false, true, "set enabled");
        boolean z3 = iCheckCallingOrSelfPermission == 0;
        boolean z4 = str2 == null;
        boolean z5 = getInstantAppPackageName(callingUid) != null;
        String str8 = z4 ? str : str2;
        synchronized (this.mPackages) {
            packageSetting = this.mSettings.mPackages.get(str);
            if (packageSetting == null) {
                if (!z5) {
                    if (str2 == null) {
                        throw new IllegalArgumentException("Unknown package: " + str);
                    }
                    throw new IllegalArgumentException("Unknown component: " + str + SliceClientPermissions.SliceAuthority.DELIMITER + str2);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Attempt to change component state; pid=");
                sb.append(Binder.getCallingPid());
                sb.append(", uid=");
                sb.append(callingUid);
                if (str2 == null) {
                    str7 = ", package=" + str;
                } else {
                    str7 = ", component=" + str + SliceClientPermissions.SliceAuthority.DELIMITER + str2;
                }
                sb.append(str7);
                throw new SecurityException(sb.toString());
            }
        }
        if (!UserHandle.isSameApp(callingUid, packageSetting.appId)) {
            if (!z3 || filterAppAccessLPr(packageSetting, callingUid, i3)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Attempt to change component state; pid=");
                sb2.append(Binder.getCallingPid());
                sb2.append(", uid=");
                sb2.append(callingUid);
                if (str2 == null) {
                    str6 = ", package=" + str;
                } else {
                    str6 = ", component=" + str + SliceClientPermissions.SliceAuthority.DELIMITER + str2;
                }
                sb2.append(str6);
                throw new SecurityException(sb2.toString());
            }
            if (this.mProtectedPackages.isPackageStateProtected(i3, str)) {
                throw new SecurityException("Cannot disable a protected package: " + str);
            }
        }
        synchronized (this.mPackages) {
            if (callingUid == 2000) {
                try {
                    if ((packageSetting.pkgFlags & 256) == 0) {
                        int enabled = packageSetting.getEnabled(i3);
                        if (str2 != null || ((enabled != 3 && enabled != 0 && enabled != 1) || (i != 3 && i != 0 && i != 1))) {
                            throw new SecurityException("Shell cannot change component state for " + str + SliceClientPermissions.SliceAuthority.DELIMITER + str2 + " to " + i);
                        }
                    }
                } finally {
                }
            }
        }
        if (str2 == null) {
            synchronized (this.mPackages) {
                if (packageSetting.getEnabled(i3) == i) {
                    return;
                }
                PackageParser.Package r13 = packageSetting.pkg;
                Throwable th = null;
                if ((r13.isStub && r13.isSystem()) && (i == 0 || i == 1)) {
                    File fileDecompressPackage = decompressPackage(r13);
                    if (fileDecompressPackage == null) {
                        Slog.e(TAG, "couldn't decompress pkg: " + packageSetting.name);
                        return;
                    }
                    PackageParser packageParser = new PackageParser();
                    packageParser.setSeparateProcesses(this.mSeparateProcesses);
                    packageParser.setDisplayMetrics(this.mMetrics);
                    packageParser.setCallback(this.mPackageParserCallback);
                    try {
                        PackageParser.Package r0 = packageParser.parsePackage(fileDecompressPackage, 16 | this.mDefParseFlags | 1);
                        Object obj2 = this.mInstallLock;
                        synchronized (obj2) {
                            try {
                                try {
                                    removePackageLI(r13, true);
                                    synchronized (this.mPackages) {
                                        try {
                                            disableSystemPackageLPw(r13, r0);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            while (true) {
                                                try {
                                                    throw th;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                }
                                            }
                                        }
                                    }
                                    try {
                                        try {
                                            PackageFreezer packageFreezerFreezePackage2 = freezePackage(r13.packageName, "setEnabledSetting");
                                            try {
                                                packageFreezer = packageFreezerFreezePackage2;
                                                packageSetting2 = packageSetting;
                                                str4 = str8;
                                                str5 = null;
                                            } catch (Throwable th4) {
                                                th = th4;
                                                packageFreezer = packageFreezerFreezePackage2;
                                            }
                                            try {
                                                PackageParser.Package packageScanPackageTracedLI = scanPackageTracedLI(fileDecompressPackage, this.mDefParseFlags | Integer.MIN_VALUE | 64, 0, 0L, (UserHandle) null);
                                                prepareAppDataAfterInstallLIF(packageScanPackageTracedLI);
                                                synchronized (this.mPackages) {
                                                    try {
                                                        updateSharedLibrariesLPr(packageScanPackageTracedLI, null);
                                                    } catch (PackageManagerException e) {
                                                        Slog.e(TAG, "updateAllSharedLibrariesLPw failed: ", e);
                                                    }
                                                    this.mPermissionManager.updatePermissions(packageScanPackageTracedLI.packageName, packageScanPackageTracedLI, true, this.mPackages.values(), this.mPermissionCallback);
                                                    this.mSettings.writeLPr();
                                                }
                                                if (packageFreezer != null) {
                                                    $closeResource(null, packageFreezer);
                                                }
                                                clearAppDataLIF(packageScanPackageTracedLI, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                                                this.mDexManager.notifyPackageUpdated(packageScanPackageTracedLI.packageName, packageScanPackageTracedLI.baseCodePath, packageScanPackageTracedLI.splitCodePaths);
                                            } catch (Throwable th5) {
                                                th = th5;
                                                th = th;
                                                try {
                                                    throw th;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    if (packageFreezer != null) {
                                                        $closeResource(th, packageFreezer);
                                                    }
                                                    throw th;
                                                }
                                            }
                                        } catch (PackageManagerException e2) {
                                            e = e2;
                                            Slog.w(TAG, "Failed to install compressed system package:" + r12.name, e);
                                            removeCodePathLI(fileDecompressPackage);
                                            try {
                                                try {
                                                    packageFreezerFreezePackage = freezePackage(r13.packageName, "setEnabledSetting");
                                                } catch (Throwable th7) {
                                                    synchronized (this.mPackages) {
                                                        this.mSettings.disableSystemPackageLPw(r13.packageName, true);
                                                        this.mSettings.writeLPr();
                                                        throw th7;
                                                    }
                                                }
                                            } catch (PackageManagerException e3) {
                                                Slog.w(TAG, "Failed to restore system package:" + r13.packageName, e3);
                                                synchronized (this.mPackages) {
                                                    this.mSettings.disableSystemPackageLPw(r13.packageName, true);
                                                    this.mSettings.writeLPr();
                                                }
                                            }
                                            try {
                                                synchronized (this.mPackages) {
                                                    enableSystemPackageLPw(r13);
                                                }
                                                installPackageFromSystemLIF(r13.codePath, false, null, null, null, true);
                                                if (packageFreezerFreezePackage != null) {
                                                    $closeResource(r15, packageFreezerFreezePackage);
                                                }
                                                synchronized (this.mPackages) {
                                                    try {
                                                        this.mSettings.disableSystemPackageLPw(r13.packageName, true);
                                                        this.mSettings.writeLPr();
                                                    } catch (Throwable th8) {
                                                        throw th8;
                                                    }
                                                }
                                                return;
                                            } catch (Throwable th9) {
                                                try {
                                                    throw th9;
                                                } catch (Throwable th10) {
                                                    th = th10;
                                                    r15 = th9;
                                                    if (packageFreezerFreezePackage != null) {
                                                        $closeResource(r15, packageFreezerFreezePackage);
                                                    }
                                                    throw th;
                                                }
                                            }
                                        }
                                    } catch (PackageManagerException e4) {
                                        e = e4;
                                        obj = obj2;
                                        r12 = packageSetting;
                                        r15 = 0;
                                        Slog.w(TAG, "Failed to install compressed system package:" + r12.name, e);
                                        removeCodePathLI(fileDecompressPackage);
                                        packageFreezerFreezePackage = freezePackage(r13.packageName, "setEnabledSetting");
                                        synchronized (this.mPackages) {
                                        }
                                    }
                                } catch (Throwable th11) {
                                    th = th11;
                                    obj = obj2;
                                    throw th;
                                }
                            } catch (Throwable th12) {
                                th = th12;
                            }
                        }
                    } catch (PackageParser.PackageParserException e5) {
                        Slog.w(TAG, "Failed to parse compressed system package:" + packageSetting.name, e5);
                        return;
                    }
                } else {
                    packageSetting2 = packageSetting;
                    str4 = str8;
                    str5 = null;
                }
                if (i != 0 && i != 1) {
                    str5 = str3;
                }
                synchronized (this.mPackages) {
                    packageSetting2.setEnabled(i, i3, str5);
                }
            }
        } else {
            packageSetting2 = packageSetting;
            str4 = str8;
            synchronized (this.mPackages) {
                PackageParser.Package r3 = packageSetting2.pkg;
                if (r3 == null || !r3.hasComponentClassName(str2)) {
                    if (r3 != null && r3.applicationInfo.targetSdkVersion >= 16) {
                        throw new IllegalArgumentException("Component class " + str2 + " does not exist in " + str);
                    }
                    Slog.w(TAG, "Failed setComponentEnabledSetting: component class " + str2 + " does not exist in " + str);
                }
                switch (i) {
                    case 0:
                        if (!packageSetting2.restoreComponentLPw(str2, i3)) {
                            return;
                        }
                        break;
                    case 1:
                        if (!packageSetting2.enableComponentLPw(str2, i3)) {
                            return;
                        }
                        break;
                    case 2:
                        if (!packageSetting2.disableComponentLPw(str2, i3)) {
                            return;
                        }
                        break;
                    default:
                        Slog.e(TAG, "Invalid new component state: " + i);
                        return;
                }
            }
        }
        synchronized (this.mPackages) {
            scheduleWritePackageRestrictionsLocked(i3);
            updateSequenceNumberLP(packageSetting2, new int[]{i3});
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                updateInstantAppInstallerLocked(str);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                arrayList = this.mPendingBroadcasts.get(i3, str);
                boolean z6 = arrayList == null;
                if (z6) {
                    arrayList = new ArrayList<>();
                }
                String str9 = str4;
                if (!arrayList.contains(str9)) {
                    arrayList.add(str9);
                }
                i4 = i2 & 1;
                if (i4 == 0) {
                    this.mPendingBroadcasts.remove(i3, str);
                    z = true;
                    z2 = true;
                } else {
                    if (z6) {
                        this.mPendingBroadcasts.put(i3, str, arrayList);
                    }
                    z = true;
                    if (!this.mHandler.hasMessages(1)) {
                        this.mHandler.sendEmptyMessageDelayed(1, 10000L);
                    }
                    z2 = false;
                }
            } finally {
            }
        }
        long jClearCallingIdentity2 = Binder.clearCallingIdentity();
        if (z2) {
            try {
                sendPackageChangedBroadcast(str, i4 != 0 ? z : false, arrayList, UserHandle.getUid(i3, packageSetting2.appId));
            } finally {
            }
        }
    }

    public void flushPackageRestrictionsAsUser(int i) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || !sUserManager.exists(i)) {
            return;
        }
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, false, false, "flushPackageRestrictions");
        synchronized (this.mPackages) {
            this.mSettings.writePackageRestrictionsLPr(i);
            this.mDirtyUsers.remove(Integer.valueOf(i));
            if (this.mDirtyUsers.isEmpty()) {
                this.mHandler.removeMessages(14);
            }
        }
    }

    private void sendPackageChangedBroadcast(String str, boolean z, ArrayList<String> arrayList, int i) {
        int[] iArr;
        if (DEBUG_INSTALL) {
            Log.v(TAG, "Sending package changed: package=" + str + " components=" + arrayList);
        }
        Bundle bundle = new Bundle(4);
        bundle.putString("android.intent.extra.changed_component_name", arrayList.get(0));
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        bundle.putStringArray("android.intent.extra.changed_component_name_list", strArr);
        bundle.putBoolean("android.intent.extra.DONT_KILL_APP", z);
        bundle.putInt("android.intent.extra.UID", i);
        int i2 = !arrayList.contains(str) ? 1073741824 : 0;
        int userId = UserHandle.getUserId(i);
        boolean zIsInstantApp = isInstantApp(str, userId);
        int[] iArr2 = zIsInstantApp ? EMPTY_INT_ARRAY : new int[]{userId};
        if (zIsInstantApp) {
            iArr = new int[]{userId};
        } else {
            iArr = EMPTY_INT_ARRAY;
        }
        sendPackageBroadcast("android.intent.action.PACKAGE_CHANGED", str, bundle, i2, null, null, iArr2, iArr);
    }

    public void setPackageStoppedState(String str, boolean z, int i) {
        if (sUserManager.exists(i)) {
            int callingUid = Binder.getCallingUid();
            if (getInstantAppPackageName(callingUid) != null) {
                return;
            }
            boolean z2 = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE") == 0;
            this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "stop package");
            synchronized (this.mPackages) {
                if (!filterAppAccessLPr(this.mSettings.mPackages.get(str), callingUid, i) && this.mSettings.setPackageStoppedStateLPw(this, str, z, z2, callingUid, i)) {
                    scheduleWritePackageRestrictionsLocked(i);
                }
            }
        }
    }

    public String getInstallerPackageName(String str) {
        int callingUid = Binder.getCallingUid();
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.mPackages.get(str), callingUid, UserHandle.getUserId(callingUid))) {
                return null;
            }
            return this.mSettings.getInstallerPackageNameLPr(str);
        }
    }

    public boolean isOrphaned(String str) {
        boolean zIsOrphaned;
        synchronized (this.mPackages) {
            zIsOrphaned = this.mSettings.isOrphaned(str);
        }
        return zIsOrphaned;
    }

    public int getApplicationEnabledSetting(String str, int i) {
        if (!sUserManager.exists(i)) {
            return 2;
        }
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, false, false, "get enabled");
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.getPackageLPr(str), callingUid, i)) {
                return 2;
            }
            return this.mSettings.getApplicationEnabledSettingLPr(str, i);
        }
    }

    public int getComponentEnabledSetting(ComponentName componentName, int i) {
        if (!sUserManager.exists(i)) {
            return 2;
        }
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, false, false, "getComponentEnabled");
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.getPackageLPr(componentName.getPackageName()), callingUid, componentName, 0, i)) {
                return 2;
            }
            return this.mSettings.getComponentEnabledSettingLPr(componentName, i);
        }
    }

    public void enterSafeMode() {
        enforceSystemOrRoot("Only the system can request entering safe mode");
        if (!this.mSystemReady) {
            this.mSafeMode = true;
        }
    }

    public void systemReady() {
        int[] iArrAppendInt;
        enforceSystemOrRoot("Only the system can claim the system is ready");
        boolean z = true;
        this.mSystemReady = true;
        final ContentResolver contentResolver = this.mContext.getContentResolver();
        ContentObserver contentObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z2) {
                PackageManagerService packageManagerService = PackageManagerService.this;
                boolean z3 = true;
                if (Settings.Global.getInt(contentResolver, "enable_ephemeral_feature", 1) != 0 && Settings.Secure.getInt(contentResolver, "instant_apps_enabled", 1) != 0) {
                    z3 = false;
                }
                packageManagerService.mWebInstantAppsDisabled = z3;
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("enable_ephemeral_feature"), false, contentObserver, 0);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("instant_apps_enabled"), false, contentObserver, 0);
        contentObserver.onChange(true);
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(this.mContext.getOpPackageName(), this, this.mContext.getContentResolver(), 0);
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "compatibility_mode", 1) != 1) {
            z = false;
        }
        PackageParser.setCompatibilityModeEnabled(z);
        if (DEBUG_SETTINGS) {
            Log.d(TAG, "compatibility mode:" + z);
        }
        int[] iArr = EMPTY_INT_ARRAY;
        synchronized (this.mPackages) {
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
                PreferredIntentResolver preferredIntentResolverValueAt = this.mSettings.mPreferredActivities.valueAt(i);
                arrayList.clear();
                for (PreferredActivity preferredActivity : preferredIntentResolverValueAt.filterSet()) {
                    if (this.mActivities.mActivities.get(preferredActivity.mPref.mComponent) == null) {
                        arrayList.add(preferredActivity);
                    }
                }
                if (arrayList.size() > 0) {
                    for (int i2 = 0; i2 < arrayList.size(); i2++) {
                        PreferredActivity preferredActivity2 = (PreferredActivity) arrayList.get(i2);
                        Slog.w(TAG, "Removing dangling preferred activity: " + preferredActivity2.mPref.mComponent);
                        preferredIntentResolverValueAt.removeFilter(preferredActivity2);
                    }
                    this.mSettings.writePackageRestrictionsLPr(this.mSettings.mPreferredActivities.keyAt(i));
                }
            }
            iArrAppendInt = iArr;
            for (int i3 : UserManagerService.getInstance().getUserIds()) {
                if (!this.mSettings.areDefaultRuntimePermissionsGrantedLPr(i3)) {
                    iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i3);
                }
            }
        }
        sUserManager.systemReady();
        for (int i4 : iArrAppendInt) {
            this.mDefaultPermissionPolicy.grantDefaultPermissions(i4);
        }
        if (iArrAppendInt == EMPTY_INT_ARRAY) {
            this.mDefaultPermissionPolicy.scheduleReadDefaultPermissionExceptions();
        }
        synchronized (this.mPackages) {
            this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false, this.mPackages.values(), this.mPermissionCallback);
        }
        if (this.mPostSystemReadyMessages != null) {
            Iterator<Message> it = this.mPostSystemReadyMessages.iterator();
            while (it.hasNext()) {
                it.next().sendToTarget();
            }
            this.mPostSystemReadyMessages = null;
        }
        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).registerListener(this.mStorageListener);
        this.mInstallerService.systemReady();
        this.mDexManager.systemReady();
        this.mPackageDexOptimizer.systemReady();
        ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).addExternalStoragePolicy(new StorageManagerInternal.ExternalStorageMountPolicy() {
            public int getMountMode(int i5, String str) {
                if (Process.isIsolated(i5)) {
                    return 0;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.READ_EXTERNAL_STORAGE", i5) == -1) {
                    return 1;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.WRITE_EXTERNAL_STORAGE", i5) == -1) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int i5, String str) {
                return true;
            }
        });
        sUserManager.reconcileUsers(StorageManager.UUID_PRIVATE_INTERNAL);
        reconcileApps(StorageManager.UUID_PRIVATE_INTERNAL);
        this.mPermissionManager.systemReady();
        if (this.mInstantAppResolverConnection != null) {
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    PackageManagerService.this.mInstantAppResolverConnection.optimisticBind();
                    PackageManagerService.this.mContext.unregisterReceiver(this);
                }
            }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        }
        sCtaManager.systemReady();
    }

    public void waitForAppDataPrepared() {
        if (this.mPrepareAppDataFuture == null) {
            return;
        }
        ConcurrentUtils.waitForFutureNoInterrupt(this.mPrepareAppDataFuture, "wait for prepareAppData");
        this.mPrepareAppDataFuture = null;
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean hasSystemUidErrors() {
        return this.mHasSystemUidErrors;
    }

    static String arrayToString(int[] iArr) {
        StringBuffer stringBuffer = new StringBuffer(128);
        stringBuffer.append('[');
        if (iArr != null) {
            for (int i = 0; i < iArr.length; i++) {
                if (i > 0) {
                    stringBuffer.append(", ");
                }
                stringBuffer.append(iArr[i]);
            }
        }
        stringBuffer.append(']');
        return stringBuffer.toString();
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new PackageManagerShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) throws Throwable {
        String str;
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            return;
        }
        DumpState dumpState = new DumpState();
        int i = 0;
        boolean z = false;
        while (true) {
            boolean z2 = true;
            if (i >= strArr.length || (str = strArr[i]) == null || str.length() <= 0 || str.charAt(0) != '-') {
                break;
            }
            i++;
            if (!"-a".equals(str)) {
                if ("-h".equals(str)) {
                    printWriter.println("Package manager dump options:");
                    printWriter.println("  [-h] [-f] [--checkin] [cmd] ...");
                    printWriter.println("    --checkin: dump for a checkin");
                    printWriter.println("    -f: print details of intent filters");
                    printWriter.println("    -h: print this help");
                    printWriter.println("  cmd may be one of:");
                    printWriter.println("    l[ibraries]: list known shared libraries");
                    printWriter.println("    f[eatures]: list device features");
                    printWriter.println("    k[eysets]: print known keysets");
                    printWriter.println("    r[esolvers] [activity|service|receiver|content]: dump intent resolvers");
                    printWriter.println("    perm[issions]: dump permissions");
                    printWriter.println("    permission [name ...]: dump declaration and use of given permission");
                    printWriter.println("    pref[erred]: print preferred package settings");
                    printWriter.println("    preferred-xml [--full]: print preferred package settings as xml");
                    printWriter.println("    prov[iders]: dump content providers");
                    printWriter.println("    p[ackages]: dump installed packages");
                    printWriter.println("    s[hared-users]: dump shared user IDs");
                    printWriter.println("    m[essages]: print collected runtime messages");
                    printWriter.println("    v[erifiers]: print package verifier info");
                    printWriter.println("    d[omain-preferred-apps]: print domains preferred apps");
                    printWriter.println("    i[ntent-filter-verifiers]|ifv: print intent filter verifier info");
                    printWriter.println("    version: print database version info");
                    printWriter.println("    write: write current settings now");
                    printWriter.println("    installs: details about install sessions");
                    printWriter.println("    check-permission <permission> <package> [<user>]: does pkg hold perm?");
                    printWriter.println("    dexopt: dump dexopt state");
                    printWriter.println("    compiler-stats: dump compiler statistics");
                    printWriter.println("    service-permissions: dump permissions required by services");
                    printWriter.println("    <package.name>: info about given package");
                    return;
                }
                if (!"--checkin".equals(str)) {
                    if ("-f".equals(str)) {
                        dumpState.setOptionEnabled(1);
                    } else {
                        if (PriorityDump.PROTO_ARG.equals(str)) {
                            dumpProto(fileDescriptor);
                            return;
                        }
                        printWriter.println("Unknown argument: " + str + "; use -h for help");
                    }
                } else {
                    z = true;
                }
            }
        }
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mPackages) {
            long jStart = protoOutputStream.start(1146756268033L);
            protoOutputStream.write(1138166333441L, this.mRequiredVerifierPackage);
            protoOutputStream.write(1120986464258L, getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
            protoOutputStream.end(jStart);
            if (this.mIntentFilterVerifierComponent != null) {
                String packageName = this.mIntentFilterVerifierComponent.getPackageName();
                long jStart2 = protoOutputStream.start(1146756268034L);
                protoOutputStream.write(1138166333441L, packageName);
                protoOutputStream.write(1120986464258L, getPackageUid(packageName, 268435456, 0));
                protoOutputStream.end(jStart2);
            }
            dumpSharedLibrariesProto(protoOutputStream);
            dumpFeaturesProto(protoOutputStream);
            this.mSettings.dumpPackagesProto(protoOutputStream);
            this.mSettings.dumpSharedUsersProto(protoOutputStream);
            PackageManagerServiceUtils.dumpCriticalInfo(protoOutputStream);
        }
        protoOutputStream.flush();
    }

    private void dumpFeaturesProto(ProtoOutputStream protoOutputStream) {
        synchronized (this.mAvailableFeatures) {
            int size = this.mAvailableFeatures.size();
            for (int i = 0; i < size; i++) {
                this.mAvailableFeatures.valueAt(i).writeToProto(protoOutputStream, 2246267895812L);
            }
        }
    }

    private void dumpSharedLibrariesProto(ProtoOutputStream protoOutputStream) {
        int size = this.mSharedLibraries.size();
        for (int i = 0; i < size; i++) {
            LongSparseArray<SharedLibraryEntry> longSparseArray = this.mSharedLibraries.get(this.mSharedLibraries.keyAt(i));
            if (longSparseArray != null) {
                int size2 = longSparseArray.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    SharedLibraryEntry sharedLibraryEntryValueAt = longSparseArray.valueAt(i2);
                    long jStart = protoOutputStream.start(2246267895811L);
                    protoOutputStream.write(1138166333441L, sharedLibraryEntryValueAt.info.getName());
                    boolean z = sharedLibraryEntryValueAt.path != null;
                    protoOutputStream.write(1133871366146L, z);
                    if (z) {
                        protoOutputStream.write(1138166333443L, sharedLibraryEntryValueAt.path);
                    } else {
                        protoOutputStream.write(1138166333444L, sharedLibraryEntryValueAt.apk);
                    }
                    protoOutputStream.end(jStart);
                }
            }
        }
    }

    private void dumpDexoptStateLPr(PrintWriter printWriter, String str) {
        Collection<PackageParser.Package> collectionValues;
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println();
        indentingPrintWriter.println("Dexopt state:");
        indentingPrintWriter.increaseIndent();
        if (str != null) {
            PackageParser.Package r5 = this.mPackages.get(str);
            if (r5 != null) {
                collectionValues = Collections.singletonList(r5);
            } else {
                indentingPrintWriter.println("Unable to find package: " + str);
                return;
            }
        } else {
            collectionValues = this.mPackages.values();
        }
        for (PackageParser.Package r6 : collectionValues) {
            indentingPrintWriter.println("[" + r6.packageName + "]");
            indentingPrintWriter.increaseIndent();
            this.mPackageDexOptimizer.dumpDexoptState(indentingPrintWriter, r6, this.mDexManager.getPackageUseInfoOrDefault(r6.packageName));
            indentingPrintWriter.decreaseIndent();
        }
    }

    private void dumpCompilerStatsLPr(PrintWriter printWriter, String str) {
        Collection<PackageParser.Package> collectionValues;
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println();
        indentingPrintWriter.println("Compiler stats:");
        indentingPrintWriter.increaseIndent();
        if (str != null) {
            PackageParser.Package r4 = this.mPackages.get(str);
            if (r4 != null) {
                collectionValues = Collections.singletonList(r4);
            } else {
                indentingPrintWriter.println("Unable to find package: " + str);
                return;
            }
        } else {
            collectionValues = this.mPackages.values();
        }
        for (PackageParser.Package r5 : collectionValues) {
            indentingPrintWriter.println("[" + r5.packageName + "]");
            indentingPrintWriter.increaseIndent();
            CompilerStats.PackageStats compilerPackageStats = getCompilerPackageStats(r5.packageName);
            if (compilerPackageStats == null) {
                indentingPrintWriter.println("(No recorded stats)");
            } else {
                compilerPackageStats.dump(indentingPrintWriter);
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    private String dumpDomainString(String str) {
        List list = getIntentFilterVerifications(str).getList();
        List<IntentFilter> list2 = getAllIntentFilters(str).getList();
        ArraySet<String> arraySet = new ArraySet();
        if (list.size() > 0) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Iterator it2 = ((IntentFilterVerificationInfo) it.next()).getDomains().iterator();
                while (it2.hasNext()) {
                    arraySet.add((String) it2.next());
                }
            }
        }
        if (list2 != null && list2.size() > 0) {
            for (IntentFilter intentFilter : list2) {
                if (intentFilter.hasCategory("android.intent.category.BROWSABLE") && (intentFilter.hasDataScheme("http") || intentFilter.hasDataScheme("https"))) {
                    arraySet.addAll(intentFilter.getHostsList());
                }
            }
        }
        StringBuilder sb = new StringBuilder(arraySet.size() * 16);
        for (String str2 : arraySet) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    static String getEncryptKey() {
        try {
            String strRetrieveKeyHexString = SystemKeyStore.getInstance().retrieveKeyHexString(SD_ENCRYPTION_KEYSTORE_NAME);
            if (strRetrieveKeyHexString == null && (strRetrieveKeyHexString = SystemKeyStore.getInstance().generateNewKeyHexString(128, SD_ENCRYPTION_ALGORITHM, SD_ENCRYPTION_KEYSTORE_NAME)) == null) {
                Slog.e(TAG, "Failed to create encryption keys");
                return null;
            }
            return strRetrieveKeyHexString;
        } catch (IOException e) {
            Slog.e(TAG, "Failed to retrieve encryption keys with exception: " + e);
            return null;
        } catch (NoSuchAlgorithmException e2) {
            Slog.e(TAG, "Failed to create encryption keys with exception: " + e2);
            return null;
        }
    }

    private void sendResourcesChangedBroadcast(boolean z, boolean z2, ArrayList<ApplicationInfo> arrayList, IIntentReceiver iIntentReceiver) {
        int size = arrayList.size();
        String[] strArr = new String[size];
        int[] iArr = new int[size];
        for (int i = 0; i < size; i++) {
            ApplicationInfo applicationInfo = arrayList.get(i);
            strArr[i] = applicationInfo.packageName;
            iArr[i] = applicationInfo.uid;
        }
        sendResourcesChangedBroadcast(z, z2, strArr, iArr, iIntentReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean z, boolean z2, ArrayList<String> arrayList, int[] iArr, IIntentReceiver iIntentReceiver) {
        sendResourcesChangedBroadcast(z, z2, (String[]) arrayList.toArray(new String[arrayList.size()]), iArr, iIntentReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean z, boolean z2, String[] strArr, int[] iArr, IIntentReceiver iIntentReceiver) {
        if (strArr.length > 0) {
            Bundle bundle = new Bundle();
            bundle.putStringArray("android.intent.extra.changed_package_list", strArr);
            if (iArr != null) {
                bundle.putIntArray("android.intent.extra.changed_uid_list", iArr);
            }
            if (z2) {
                bundle.putBoolean("android.intent.extra.REPLACING", z2);
            }
            sendPackageBroadcast(z ? "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE" : "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE", null, bundle, 0, null, iIntentReceiver, null, null);
        }
    }

    private void loadPrivatePackages(final VolumeInfo volumeInfo) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() throws Throwable {
                PackageManagerService.this.loadPrivatePackagesInner(volumeInfo);
            }
        });
    }

    private void loadPrivatePackagesInner(VolumeInfo volumeInfo) throws Throwable {
        Settings.VersionInfo versionInfoFindOrCreateVersion;
        List<PackageSetting> volumePackagesLPr;
        int i;
        Object obj;
        int i2;
        PackageSetting packageSetting;
        String str = volumeInfo.fsUuid;
        if (TextUtils.isEmpty(str)) {
            Slog.e(TAG, "Loading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList<ApplicationInfo> arrayList2 = new ArrayList<>();
        int i3 = this.mDefParseFlags | 8;
        synchronized (this.mPackages) {
            versionInfoFindOrCreateVersion = this.mSettings.findOrCreateVersion(str);
            volumePackagesLPr = this.mSettings.getVolumePackagesLPr(str);
        }
        for (PackageSetting packageSetting2 : volumePackagesLPr) {
            arrayList.add(freezePackage(packageSetting2.name, "loadPrivatePackagesInner"));
            Object obj2 = this.mInstallLock;
            synchronized (obj2) {
                try {
                    int i4 = i3;
                    obj = obj2;
                    i2 = i3;
                    packageSetting = packageSetting2;
                    try {
                        try {
                            arrayList2.add(scanPackageTracedLI(packageSetting2.codePath, i4, 512, 0L, (UserHandle) null).applicationInfo);
                        } catch (PackageManagerException e) {
                            e = e;
                            Slog.w(TAG, "Failed to scan " + packageSetting.codePath + ": " + e.getMessage());
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (PackageManagerException e2) {
                    e = e2;
                    obj = obj2;
                    i2 = i3;
                    packageSetting = packageSetting2;
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj2;
                    throw th;
                }
                if (!Build.FINGERPRINT.equals(versionInfoFindOrCreateVersion.fingerprint)) {
                    clearAppDataLIF(packageSetting.pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                }
                i3 = i2;
            }
        }
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal userManagerInternal = getUserManagerInternal();
        for (UserInfo userInfo : userManager.getUsers()) {
            if (userManagerInternal.isUserUnlockingOrUnlocked(userInfo.id)) {
                i = 3;
            } else if (userManagerInternal.isUserRunning(userInfo.id)) {
                i = 1;
            } else {
                continue;
            }
            storageManager.prepareUserStorage(str, userInfo.id, userInfo.serialNumber, i);
            synchronized (this.mInstallLock) {
                reconcileAppsDataLI(str, userInfo.id, i, true);
            }
        }
        synchronized (this.mPackages) {
            boolean z = versionInfoFindOrCreateVersion.sdkVersion != this.mSdkVersion;
            if (z) {
                PackageManagerServiceUtils.logCriticalInfo(4, "Platform changed from " + versionInfoFindOrCreateVersion.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for " + str);
            }
            this.mPermissionManager.updateAllPermissions(str, z, this.mPackages.values(), this.mPermissionCallback);
            versionInfoFindOrCreateVersion.forceCurrent();
            this.mSettings.writeLPr();
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((PackageFreezer) it.next()).close();
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Loaded packages " + arrayList2);
        }
        sendResourcesChangedBroadcast(true, false, arrayList2, null);
        this.mLoadedVolumes.add(volumeInfo.getId());
    }

    private void unloadPrivatePackages(final VolumeInfo volumeInfo) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.unloadPrivatePackagesInner(volumeInfo);
            }
        });
    }

    private void unloadPrivatePackagesInner(VolumeInfo volumeInfo) {
        PackageFreezer packageFreezer;
        Throwable th;
        Iterator<PackageSetting> it;
        String str = volumeInfo.fsUuid;
        if (TextUtils.isEmpty(str)) {
            Slog.e(TAG, "Unloading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList<ApplicationInfo> arrayList = new ArrayList<>();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                Iterator<PackageSetting> it2 = this.mSettings.getVolumePackagesLPr(str).iterator();
                while (it2.hasNext()) {
                    PackageSetting next = it2.next();
                    if (next.pkg != null) {
                        ApplicationInfo applicationInfo = next.pkg.applicationInfo;
                        PackageRemovedInfo packageRemovedInfo = new PackageRemovedInfo(this);
                        PackageFreezer packageFreezerFreezePackageForDelete = freezePackageForDelete(next.name, 1, "unloadPrivatePackagesInner");
                        try {
                            packageFreezer = packageFreezerFreezePackageForDelete;
                            it = it2;
                        } catch (Throwable th2) {
                            th = th2;
                            packageFreezer = packageFreezerFreezePackageForDelete;
                        }
                        try {
                            if (deletePackageLIF(next.name, null, false, null, 1, packageRemovedInfo, false, null)) {
                                arrayList.add(applicationInfo);
                            } else {
                                Slog.w(TAG, "Failed to unload " + next.codePath);
                            }
                            if (packageFreezer != null) {
                                $closeResource(null, packageFreezer);
                            }
                            AttributeCache.instance().removePackage(next.name);
                            it2 = it;
                        } catch (Throwable th3) {
                            th = th3;
                            th = null;
                            if (packageFreezer != null) {
                            }
                            throw th;
                        }
                    }
                }
                this.mSettings.writeLPr();
            }
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Unloaded packages " + arrayList);
        }
        sendResourcesChangedBroadcast(false, false, arrayList, null);
        this.mLoadedVolumes.remove(volumeInfo.getId());
        ResourcesManager.getInstance().invalidatePath(volumeInfo.getPath().getAbsolutePath());
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
        }
    }

    private void assertPackageKnown(String str, String str2) throws PackageManagerException {
        synchronized (this.mPackages) {
            String strNormalizePackageNameLPr = normalizePackageNameLPr(str2);
            PackageSetting packageSetting = this.mSettings.mPackages.get(strNormalizePackageNameLPr);
            if (packageSetting == null) {
                throw new PackageManagerException("Package " + strNormalizePackageNameLPr + " is unknown");
            }
            if (!TextUtils.equals(str, packageSetting.volumeUuid)) {
                throw new PackageManagerException("Package " + strNormalizePackageNameLPr + " found on unknown volume " + str + "; expected volume " + packageSetting.volumeUuid);
            }
        }
    }

    private void assertPackageKnownAndInstalled(String str, String str2, int i) throws PackageManagerException {
        synchronized (this.mPackages) {
            String strNormalizePackageNameLPr = normalizePackageNameLPr(str2);
            PackageSetting packageSetting = this.mSettings.mPackages.get(strNormalizePackageNameLPr);
            if (packageSetting == null) {
                throw new PackageManagerException("Package " + strNormalizePackageNameLPr + " is unknown");
            }
            if (!TextUtils.equals(str, packageSetting.volumeUuid)) {
                throw new PackageManagerException("Package " + strNormalizePackageNameLPr + " found on unknown volume " + str + "; expected volume " + packageSetting.volumeUuid);
            }
            if (!packageSetting.getInstalled(i)) {
                throw new PackageManagerException("Package " + strNormalizePackageNameLPr + " not installed for user " + i);
            }
        }
    }

    private List<String> collectAbsoluteCodePaths() {
        ArrayList arrayList;
        synchronized (this.mPackages) {
            arrayList = new ArrayList();
            int size = this.mSettings.mPackages.size();
            for (int i = 0; i < size; i++) {
                arrayList.add(this.mSettings.mPackages.valueAt(i).codePath.getAbsolutePath());
            }
        }
        return arrayList;
    }

    private void reconcileApps(String str) {
        List<String> listCollectAbsoluteCodePaths = collectAbsoluteCodePaths();
        ArrayList arrayList = null;
        for (File file : FileUtils.listFilesOrEmpty(Environment.getDataAppDirectory(str))) {
            boolean z = true;
            if ((PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName())) {
                String absolutePath = file.getAbsolutePath();
                int size = listCollectAbsoluteCodePaths.size();
                int i = 0;
                while (true) {
                    if (i < size) {
                        if (absolutePath.startsWith(listCollectAbsoluteCodePaths.get(i))) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(file);
                }
            }
        }
        if (arrayList != null) {
            int size2 = arrayList.size();
            for (int i2 = 0; i2 < size2; i2++) {
                File file2 = (File) arrayList.get(i2);
                PackageManagerServiceUtils.logCriticalInfo(5, "Destroying orphaned" + file2);
                synchronized (this.mInstallLock) {
                    removeCodePathLI(file2);
                }
            }
        }
    }

    void reconcileAppsData(int i, int i2, boolean z) {
        Iterator it = ((StorageManager) this.mContext.getSystemService(StorageManager.class)).getWritablePrivateVolumes().iterator();
        while (it.hasNext()) {
            String fsUuid = ((VolumeInfo) it.next()).getFsUuid();
            synchronized (this.mInstallLock) {
                reconcileAppsDataLI(fsUuid, i, i2, z);
            }
        }
    }

    private void reconcileAppsDataLI(String str, int i, int i2, boolean z) {
        reconcileAppsDataLI(str, i, i2, z, false);
    }

    private List<String> reconcileAppsDataLI(String str, int i, int i2, boolean z, boolean z2) {
        List<PackageSetting> volumePackagesLPr;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        File[] fileArr;
        Slog.v(TAG, "reconcileAppsData for " + str + " u" + i + " 0x" + Integer.toHexString(i2) + " migrateAppData=" + z);
        sMtkSystemServerIns.addBootEvent("PMS:reconcileAppsDataLI");
        ArrayList arrayList = z2 ? new ArrayList() : null;
        File dataUserCeDirectory = Environment.getDataUserCeDirectory(str, i);
        File dataUserDeDirectory = Environment.getDataUserDeDirectory(str, i);
        if ((i2 & 2) != 0) {
            if (StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(i)) {
                throw new RuntimeException("Yikes, someone asked us to reconcile CE storage while " + i + " was still locked; this would have caused massive data loss!");
            }
            File[] fileArrListFilesOrEmpty = FileUtils.listFilesOrEmpty(dataUserCeDirectory);
            int length = fileArrListFilesOrEmpty.length;
            int i8 = 0;
            while (i8 < length) {
                File file = fileArrListFilesOrEmpty[i8];
                String name = file.getName();
                try {
                    assertPackageKnownAndInstalled(str, name, i);
                    i5 = i8;
                    i6 = length;
                    fileArr = fileArrListFilesOrEmpty;
                } catch (PackageManagerException e) {
                    PackageManagerServiceUtils.logCriticalInfo(5, "Destroying " + file + " due to: " + e);
                    try {
                        i5 = i8;
                        i6 = length;
                        i7 = 5;
                        fileArr = fileArrListFilesOrEmpty;
                        try {
                            this.mInstaller.destroyAppData(str, name, i, 2, 0L);
                        } catch (Installer.InstallerException e2) {
                            e = e2;
                            PackageManagerServiceUtils.logCriticalInfo(i7, "Failed to destroy: " + e);
                        }
                    } catch (Installer.InstallerException e3) {
                        e = e3;
                        i5 = i8;
                        i6 = length;
                        i7 = 5;
                        fileArr = fileArrListFilesOrEmpty;
                    }
                }
                i8 = i5 + 1;
                length = i6;
                fileArrListFilesOrEmpty = fileArr;
            }
        }
        if ((i2 & 1) != 0) {
            File[] fileArrListFilesOrEmpty2 = FileUtils.listFilesOrEmpty(dataUserDeDirectory);
            int length2 = fileArrListFilesOrEmpty2.length;
            int i9 = 0;
            while (i9 < length2) {
                File file2 = fileArrListFilesOrEmpty2[i9];
                String name2 = file2.getName();
                try {
                    assertPackageKnownAndInstalled(str, name2, i);
                    i3 = length2;
                    i4 = i9;
                } catch (PackageManagerException e4) {
                    PackageManagerServiceUtils.logCriticalInfo(5, "Destroying " + file2 + " due to: " + e4);
                    try {
                        i3 = length2;
                        i4 = i9;
                        try {
                            this.mInstaller.destroyAppData(str, name2, i, 1, 0L);
                        } catch (Installer.InstallerException e5) {
                            e = e5;
                            PackageManagerServiceUtils.logCriticalInfo(5, "Failed to destroy: " + e);
                        }
                    } catch (Installer.InstallerException e6) {
                        e = e6;
                        i3 = length2;
                        i4 = i9;
                    }
                }
                i9 = i4 + 1;
                length2 = i3;
            }
        }
        synchronized (this.mPackages) {
            volumePackagesLPr = this.mSettings.getVolumePackagesLPr(str);
        }
        int i10 = 0;
        for (PackageSetting packageSetting : volumePackagesLPr) {
            String str2 = packageSetting.name;
            if (packageSetting.pkg == null) {
                Slog.w(TAG, "Odd, missing scanned package " + str2);
            } else if (z2 && !packageSetting.pkg.coreApp) {
                arrayList.add(str2);
            } else if (packageSetting.getInstalled(i)) {
                prepareAppDataAndMigrateLIF(packageSetting.pkg, i, i2, z);
                i10++;
            }
        }
        Slog.v(TAG, "reconcileAppsData finished " + i10 + " packages");
        return arrayList;
    }

    private void prepareAppDataAfterInstallLIF(PackageParser.Package r7) {
        PackageSetting packageSetting;
        int i;
        synchronized (this.mPackages) {
            packageSetting = this.mSettings.mPackages.get(r7.packageName);
            this.mSettings.writeKernelMappingLPr(packageSetting);
        }
        UserManagerService userManagerService = sUserManager;
        UserManagerInternal userManagerInternal = getUserManagerInternal();
        for (UserInfo userInfo : userManagerService.getUsers(false)) {
            if (userManagerInternal.isUserUnlockingOrUnlocked(userInfo.id)) {
                i = 3;
            } else if (userManagerInternal.isUserRunning(userInfo.id)) {
                i = 1;
            }
            if (packageSetting.getInstalled(userInfo.id)) {
                prepareAppDataLIF(r7, userInfo.id, i);
            }
        }
    }

    private void prepareAppDataLIF(PackageParser.Package r4, int i, int i2) {
        if (r4 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataLeafLIF(r4, i, i2);
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i3 = 0; i3 < size; i3++) {
            prepareAppDataLeafLIF((PackageParser.Package) r4.childPackages.get(i3), i, i2);
        }
    }

    private void prepareAppDataAndMigrateLIF(PackageParser.Package r1, int i, int i2, boolean z) {
        prepareAppDataLIF(r1, i, i2);
        if (z && maybeMigrateAppDataLIF(r1, i)) {
            prepareAppDataLIF(r1, i, i2);
        }
    }

    private void prepareAppDataLeafLIF(PackageParser.Package r21, int i, int i2) {
        PackageSetting packageSetting;
        ApplicationInfo applicationInfoGenerateApplicationInfo;
        long jCreateAppData;
        int i3;
        if (DEBUG_APP_DATA) {
            Slog.v(TAG, "prepareAppData for " + r21.packageName + " u" + i + " 0x" + Integer.toHexString(i2));
        }
        synchronized (this.mPackages) {
            packageSetting = this.mSettings.mPackages.get(r21.packageName);
        }
        String str = r21.volumeUuid;
        String str2 = r21.packageName;
        if (packageSetting == null) {
            applicationInfoGenerateApplicationInfo = r21.applicationInfo;
        } else {
            applicationInfoGenerateApplicationInfo = PackageParser.generateApplicationInfo(r21, 0, packageSetting.readUserState(i), i);
        }
        if (applicationInfoGenerateApplicationInfo == null) {
            applicationInfoGenerateApplicationInfo = r21.applicationInfo;
        }
        ApplicationInfo applicationInfo = applicationInfoGenerateApplicationInfo;
        int appId = UserHandle.getAppId(applicationInfo.uid);
        Preconditions.checkNotNull(applicationInfo.seInfo);
        StringBuilder sb = new StringBuilder();
        sb.append(applicationInfo.seInfo);
        sb.append(applicationInfo.seInfoUser != null ? applicationInfo.seInfoUser : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        String string = sb.toString();
        try {
            jCreateAppData = this.mInstaller.createAppData(str, str2, i, i2, appId, string, applicationInfo.targetSdkVersion);
        } catch (Installer.InstallerException e) {
            if (applicationInfo.isSystemApp()) {
                PackageManagerServiceUtils.logCriticalInfo(6, "Failed to create app data for " + str2 + ", but trying to recover: " + e);
                destroyAppDataLeafLIF(r21, i, i2);
                try {
                    i3 = 3;
                    try {
                        jCreateAppData = this.mInstaller.createAppData(str, str2, i, i2, appId, string, applicationInfo.targetSdkVersion);
                        try {
                            PackageManagerServiceUtils.logCriticalInfo(3, "Recovery succeeded!");
                        } catch (Installer.InstallerException e2) {
                            PackageManagerServiceUtils.logCriticalInfo(i3, "Recovery failed!");
                        }
                    } catch (Installer.InstallerException e3) {
                        jCreateAppData = -1;
                        PackageManagerServiceUtils.logCriticalInfo(i3, "Recovery failed!");
                        if (!this.mIsUpgrade) {
                            this.mArtManagerService.prepareAppProfiles(r21, i);
                        }
                        if ((i2 & 2) != 0) {
                            synchronized (this.mPackages) {
                            }
                        }
                        prepareAppDataContentsLeafLIF(r21, i, i2);
                    }
                } catch (Installer.InstallerException e4) {
                    i3 = 3;
                }
            } else {
                Slog.e(TAG, "Failed to create app data for " + str2 + ": " + e);
                jCreateAppData = -1L;
            }
        }
        if (!this.mIsUpgrade || this.mFirstBoot || i != 0) {
            this.mArtManagerService.prepareAppProfiles(r21, i);
        }
        if ((i2 & 2) != 0 && jCreateAppData != -1) {
            synchronized (this.mPackages) {
                if (packageSetting != null) {
                    try {
                        packageSetting.setCeDataInode(jCreateAppData, i);
                    } finally {
                    }
                }
            }
        }
        prepareAppDataContentsLeafLIF(r21, i, i2);
    }

    private void prepareAppDataContentsLIF(PackageParser.Package r4, int i, int i2) {
        if (r4 == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataContentsLeafLIF(r4, i, i2);
        int size = r4.childPackages != null ? r4.childPackages.size() : 0;
        for (int i3 = 0; i3 < size; i3++) {
            prepareAppDataContentsLeafLIF((PackageParser.Package) r4.childPackages.get(i3), i, i2);
        }
    }

    private void prepareAppDataContentsLeafLIF(PackageParser.Package r3, int i, int i2) {
        String str = r3.volumeUuid;
        String str2 = r3.packageName;
        ApplicationInfo applicationInfo = r3.applicationInfo;
        if ((i2 & 2) != 0 && applicationInfo.primaryCpuAbi != null && !VMRuntime.is64BitAbi(applicationInfo.primaryCpuAbi)) {
            try {
                this.mInstaller.linkNativeLibraryDirectory(str, str2, applicationInfo.nativeLibraryDir, i);
            } catch (Installer.InstallerException e) {
                Slog.e(TAG, "Failed to link native for " + str2 + ": " + e);
            }
        }
    }

    private boolean maybeMigrateAppDataLIF(PackageParser.Package r6, int i) {
        int i2;
        if (r6.isSystem() && !StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (!r6.applicationInfo.isDefaultToDeviceProtectedStorage()) {
                i2 = 2;
            } else {
                i2 = 1;
            }
            try {
                this.mInstaller.migrateAppData(r6.volumeUuid, r6.packageName, i, i2);
            } catch (Installer.InstallerException e) {
                PackageManagerServiceUtils.logCriticalInfo(5, "Failed to migrate " + r6.packageName + ": " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    public PackageFreezer freezePackage(String str, String str2) {
        return freezePackage(str, -1, str2);
    }

    public PackageFreezer freezePackage(String str, int i, String str2) {
        return new PackageFreezer(str, i, str2);
    }

    public PackageFreezer freezePackageForInstall(String str, int i, String str2) {
        return freezePackageForInstall(str, -1, i, str2);
    }

    public PackageFreezer freezePackageForInstall(String str, int i, int i2, String str2) {
        if ((i2 & 4096) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(str, i, str2);
    }

    public PackageFreezer freezePackageForDelete(String str, int i, String str2) {
        return freezePackageForDelete(str, -1, i, str2);
    }

    public PackageFreezer freezePackageForDelete(String str, int i, int i2, String str2) {
        if ((i2 & 8) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(str, i, str2);
    }

    private class PackageFreezer implements AutoCloseable {
        private final PackageFreezer[] mChildren;
        private final CloseGuard mCloseGuard;
        private final AtomicBoolean mClosed;
        private final String mPackageName;
        private final boolean mWeFroze;

        public PackageFreezer() {
            this.mClosed = new AtomicBoolean();
            this.mCloseGuard = CloseGuard.get();
            this.mPackageName = null;
            this.mChildren = null;
            this.mWeFroze = false;
            this.mCloseGuard.open("close");
        }

        public PackageFreezer(String str, int i, String str2) {
            this.mClosed = new AtomicBoolean();
            this.mCloseGuard = CloseGuard.get();
            synchronized (PackageManagerService.this.mPackages) {
                this.mPackageName = str;
                this.mWeFroze = PackageManagerService.this.mFrozenPackages.add(this.mPackageName);
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(this.mPackageName);
                if (packageSetting != null) {
                    PackageManagerService.this.killApplication(packageSetting.name, packageSetting.appId, i, str2);
                }
                PackageParser.Package r8 = PackageManagerService.this.mPackages.get(str);
                if (r8 != null && r8.childPackages != null) {
                    int size = r8.childPackages.size();
                    this.mChildren = new PackageFreezer[size];
                    for (int i2 = 0; i2 < size; i2++) {
                        this.mChildren[i2] = PackageManagerService.this.new PackageFreezer(((PackageParser.Package) r8.childPackages.get(i2)).packageName, i, str2);
                    }
                } else {
                    this.mChildren = null;
                }
            }
            this.mCloseGuard.open("close");
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }

        @Override
        public void close() {
            this.mCloseGuard.close();
            if (this.mClosed.compareAndSet(false, true)) {
                synchronized (PackageManagerService.this.mPackages) {
                    if (this.mWeFroze) {
                        PackageManagerService.this.mFrozenPackages.remove(this.mPackageName);
                    }
                    if (this.mChildren != null) {
                        for (PackageFreezer packageFreezer : this.mChildren) {
                            packageFreezer.close();
                        }
                    }
                }
            }
        }
    }

    private void checkPackageFrozen(String str) {
        synchronized (this.mPackages) {
            if (!this.mFrozenPackages.contains(str)) {
                Slog.wtf(TAG, "Expected " + str + " to be frozen!", new Throwable());
            }
        }
    }

    public int movePackage(final String str, final String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final int callingUid = Binder.getCallingUid();
        final UserHandle userHandle = new UserHandle(UserHandle.getUserId(callingUid));
        final int andIncrement = this.mNextMoveId.getAndIncrement();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManagerService.this.movePackageInternal(str, str2, andIncrement, callingUid, userHandle);
                } catch (PackageManagerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move " + str, e);
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(andIncrement, e.error);
                }
            }
        });
        return andIncrement;
    }

    private void movePackageInternal(String str, String str2, final int i, int i2, UserHandle userHandle) throws PackageManagerException {
        String str3;
        boolean z;
        String str4;
        File file;
        String str5;
        String str6;
        int appId;
        String str7;
        String strValueOf;
        int i3;
        final PackageFreezer packageFreezerFreezePackage;
        int[] iArrQueryInstalledUsers;
        final File dataAppDirectory;
        int i4;
        boolean z2;
        long j;
        IPackageInstallObserver2 iPackageInstallObserver2;
        String str8;
        String str9;
        MoveInfo moveInfo;
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        PackageManager packageManager = this.mContext.getPackageManager();
        synchronized (this.mPackages) {
            PackageParser.Package r4 = this.mPackages.get(str);
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (r4 == null || packageSetting == null || filterAppAccessLPr(packageSetting, i2, userHandle.getIdentifier())) {
                throw new PackageManagerException(-2, "Missing package");
            }
            if (r4.applicationInfo.isSystemApp()) {
                throw new PackageManagerException(-3, "Cannot move system application");
            }
            boolean zEquals = "private".equals(str2);
            boolean z3 = this.mContext.getResources().getBoolean(R.^attr-private.__removed5);
            if (zEquals && !z3) {
                throw new PackageManagerException(-9, "3rd party apps are not allowed on internal storage");
            }
            if (r4.applicationInfo.isExternalAsec()) {
                str4 = "primary_physical";
            } else if (r4.applicationInfo.isForwardLocked()) {
                str4 = "forward_locked";
            } else {
                String str10 = packageSetting.volumeUuid;
                File file2 = new File(r4.codePath);
                File file3 = new File(file2, "oat");
                if (!file2.isDirectory() || !file3.isDirectory()) {
                    throw new PackageManagerException(-6, "Move only supported for modern cluster style installs");
                }
                str3 = str10;
                z = false;
                if (!Objects.equals(str3, str2)) {
                    throw new PackageManagerException(-6, "Package already moved to " + str2);
                }
                if (r4.applicationInfo.isInternal() && isPackageDeviceAdminOnAnyUser(str)) {
                    throw new PackageManagerException(-8, "Device admin cannot be moved");
                }
                if (this.mFrozenPackages.contains(str)) {
                    throw new PackageManagerException(-7, "Failed to move already frozen package");
                }
                file = new File(r4.codePath);
                str5 = packageSetting.installerPackageName;
                str6 = packageSetting.cpuAbiOverrideString;
                appId = UserHandle.getAppId(r4.applicationInfo.uid);
                str7 = r4.applicationInfo.seInfo;
                strValueOf = String.valueOf(packageManager.getApplicationLabel(r4.applicationInfo));
                i3 = r4.applicationInfo.targetSdkVersion;
                packageFreezerFreezePackage = freezePackage(str, "movePackageInternal");
                iArrQueryInstalledUsers = packageSetting.queryInstalledUsers(sUserManager.getUserIds(), true);
            }
            str3 = str4;
            z = true;
            if (!Objects.equals(str3, str2)) {
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        bundle.putString("android.intent.extra.TITLE", strValueOf);
        this.mMoveCallbacks.notifyCreated(i, bundle);
        if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, str2)) {
            if (Objects.equals("primary_physical", str2)) {
                dataAppDirectory = storageManager.getPrimaryPhysicalVolume().getPath();
                i4 = 8;
                z2 = false;
            } else {
                VolumeInfo volumeInfoFindVolumeByUuid = storageManager.findVolumeByUuid(str2);
                if (volumeInfoFindVolumeByUuid == null || volumeInfoFindVolumeByUuid.getType() != 1 || !volumeInfoFindVolumeByUuid.isMountedWritable()) {
                    packageFreezerFreezePackage.close();
                    throw new PackageManagerException(-6, "Move location not mounted private volume");
                }
                Preconditions.checkState(!z);
                dataAppDirectory = Environment.getDataAppDirectory(str2);
                i4 = 16;
                z2 = true;
            }
        } else {
            z2 = !z;
            dataAppDirectory = Environment.getDataAppDirectory(str2);
            i4 = 16;
        }
        if (z2) {
            for (int i5 : iArrQueryInstalledUsers) {
                if (StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(i5)) {
                    throw new PackageManagerException(-10, "User " + i5 + " must be unlocked");
                }
            }
        }
        PackageStats packageStats = new PackageStats(null, -1);
        synchronized (this.mInstaller) {
            int length = iArrQueryInstalledUsers.length;
            int i6 = 0;
            while (i6 < length) {
                int i7 = length;
                if (getPackageSizeInfoLI(str, iArrQueryInstalledUsers[i6], packageStats)) {
                    i6++;
                    length = i7;
                } else {
                    packageFreezerFreezePackage.close();
                    throw new PackageManagerException(-6, "Failed to measure package size");
                }
            }
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Measured code size " + packageStats.codeSize + ", data size " + packageStats.dataSize);
        }
        final long usableSpace = dataAppDirectory.getUsableSpace();
        if (z2) {
            j = packageStats.codeSize + packageStats.dataSize;
        } else {
            j = packageStats.codeSize;
        }
        if (j > storageManager.getStorageBytesUntilLow(dataAppDirectory)) {
            packageFreezerFreezePackage.close();
            throw new PackageManagerException(-6, "Not enough free space to move");
        }
        this.mMoveCallbacks.notifyStatusChanged(i, 10);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        IPackageInstallObserver2 iPackageInstallObserver22 = new IPackageInstallObserver2.Stub() {
            public void onUserActionRequired(Intent intent) throws RemoteException {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String str11, int i8, String str12, Bundle bundle2) throws RemoteException {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.d(PackageManagerService.TAG, "Install result for move: " + PackageManager.installStatusToString(i8, str12));
                }
                countDownLatch.countDown();
                packageFreezerFreezePackage.close();
                int iInstallStatusToPublicStatus = PackageManager.installStatusToPublicStatus(i8);
                if (iInstallStatusToPublicStatus == 0) {
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i, -100);
                } else if (iInstallStatusToPublicStatus != 6) {
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i, -6);
                } else {
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i, -1);
                }
            }
        };
        if (z2) {
            iPackageInstallObserver2 = iPackageInstallObserver22;
            str8 = str6;
            final long j2 = j;
            new Thread() {
                @Override
                public void run() {
                    while (!countDownLatch.await(1L, TimeUnit.SECONDS)) {
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i, 10 + ((int) MathUtils.constrain(((usableSpace - dataAppDirectory.getUsableSpace()) * 80) / j2, 0L, 80L)));
                    }
                }
            }.start();
            str9 = str2;
            moveInfo = new MoveInfo(i, str3, str9, str, file.getName(), appId, str7, i3);
        } else {
            iPackageInstallObserver2 = iPackageInstallObserver22;
            str8 = str6;
            str9 = str2;
            moveInfo = null;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(5);
        InstallParams installParams = new InstallParams(OriginInfo.fromExistingFile(file), moveInfo, iPackageInstallObserver2, i4 | 2, str5, str9, null, userHandle, str8, null, PackageParser.SigningDetails.UNKNOWN, 0);
        installParams.setTraceMethod("movePackage").setTraceCookie(System.identityHashCode(installParams));
        messageObtainMessage.obj = installParams;
        Trace.asyncTraceBegin(262144L, "movePackage", System.identityHashCode(messageObtainMessage.obj));
        Trace.asyncTraceBegin(262144L, "queueInstall", System.identityHashCode(messageObtainMessage.obj));
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public int movePrimaryStorage(String str) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final int andIncrement = this.mNextMoveId.getAndIncrement();
        Bundle bundle = new Bundle();
        bundle.putString("android.os.storage.extra.FS_UUID", str);
        this.mMoveCallbacks.notifyCreated(andIncrement, bundle);
        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).setPrimaryStorageUuid(str, new IPackageMoveObserver.Stub() {
            public void onCreated(int i, Bundle bundle2) {
            }

            public void onStatusChanged(int i, int i2, long j) {
                PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(andIncrement, i2, j);
            }
        });
        return andIncrement;
    }

    public int getMoveStatus(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        return this.mMoveCallbacks.mLastStatus.get(i);
    }

    public void registerMoveCallback(IPackageMoveObserver iPackageMoveObserver) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.register(iPackageMoveObserver);
    }

    public void unregisterMoveCallback(IPackageMoveObserver iPackageMoveObserver) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.unregister(iPackageMoveObserver);
    }

    public boolean setInstallLocation(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
        if (getInstallLocation() == i) {
            return true;
        }
        if (i == 0 || i == 1 || i == 2) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "default_install_location", i);
            return true;
        }
        return false;
    }

    public int getInstallLocation() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "default_install_location", 0);
    }

    void cleanUpUser(UserManagerService userManagerService, int i) {
        synchronized (this.mPackages) {
            this.mDirtyUsers.remove(Integer.valueOf(i));
            this.mUserNeedsBadging.delete(i);
            this.mSettings.removeUserLPw(i);
            this.mPendingBroadcasts.remove(i);
            this.mInstantAppRegistry.onUserRemovedLPw(i);
            removeUnusedPackagesLPw(userManagerService, i);
        }
    }

    private void removeUnusedPackagesLPw(UserManagerService userManagerService, final int i) {
        int[] userIds = userManagerService.getUserIds();
        for (PackageSetting packageSetting : this.mSettings.mPackages.values()) {
            if (packageSetting.pkg != null) {
                final String str = packageSetting.pkg.packageName;
                if ((packageSetting.pkgFlags & 1) == 0 && TextUtils.isEmpty(packageSetting.pkg.staticSharedLibName)) {
                    boolean zShouldKeepUninstalledPackageLPr = shouldKeepUninstalledPackageLPr(str);
                    if (!zShouldKeepUninstalledPackageLPr) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= userIds.length) {
                                break;
                            }
                            if (userIds[i2] == i || !packageSetting.getInstalled(userIds[i2])) {
                                i2++;
                            } else {
                                zShouldKeepUninstalledPackageLPr = true;
                                break;
                            }
                        }
                    }
                    if (!zShouldKeepUninstalledPackageLPr) {
                        this.mHandler.post(new Runnable() {
                            @Override
                            public void run() throws Throwable {
                                PackageManagerService.this.deletePackageX(str, -1L, i, 0);
                            }
                        });
                    }
                }
            }
        }
    }

    void createNewUser(int i, String[] strArr) {
        synchronized (this.mInstallLock) {
            this.mSettings.createNewUserLI(this, this.mInstaller, i, strArr);
        }
        synchronized (this.mPackages) {
            scheduleWritePackageRestrictionsLocked(i);
            scheduleWritePackageListLocked(i);
            applyFactoryDefaultBrowserLPw(i);
            primeDomainVerificationsLPw(i);
        }
    }

    void onNewUserCreated(int i) {
        this.mDefaultPermissionPolicy.grantDefaultPermissions(i);
        synchronized (this.mPackages) {
            if (this.mSettings.mPermissions.mPermissionReviewRequired) {
                this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, true, this.mPackages.values(), this.mPermissionCallback);
            }
        }
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
        VerifierDeviceIdentity verifierDeviceIdentityLPw;
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can read the verifier device identity");
        synchronized (this.mPackages) {
            verifierDeviceIdentityLPw = this.mSettings.getVerifierDeviceIdentityLPw();
        }
        return verifierDeviceIdentityLPw;
    }

    public void setPermissionEnforced(String str, boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "setPermissionEnforced");
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(str)) {
            synchronized (this.mPackages) {
                if (this.mSettings.mReadExternalStorageEnforced == null || this.mSettings.mReadExternalStorageEnforced.booleanValue() != z) {
                    this.mSettings.mReadExternalStorageEnforced = z ? Boolean.TRUE : Boolean.FALSE;
                    this.mSettings.writeLPr();
                }
            }
            IActivityManager service = ActivityManager.getService();
            if (service != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    service.killProcessesBelowForeground("setPermissionEnforcement");
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("No selective enforcement for " + str);
    }

    @Deprecated
    public boolean isPermissionEnforced(String str) {
        return true;
    }

    public boolean isStorageLow() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            DeviceStorageMonitorInternal deviceStorageMonitorInternal = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            if (deviceStorageMonitorInternal != null) {
                return deviceStorageMonitorInternal.isMemoryLow();
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public IPackageInstaller getPackageInstaller() {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return this.mInstallerService;
    }

    public IArtManager getArtManager() {
        return this.mArtManagerService;
    }

    private boolean userNeedsBadging(int i) {
        boolean z;
        int iIndexOfKey = this.mUserNeedsBadging.indexOfKey(i);
        if (iIndexOfKey < 0) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = sUserManager.getUserInfo(i);
                if (userInfo != null && userInfo.isManagedProfile()) {
                    z = true;
                } else {
                    z = false;
                }
                this.mUserNeedsBadging.put(i, z);
                return z;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return this.mUserNeedsBadging.valueAt(iIndexOfKey);
    }

    public KeySet getKeySetByAlias(String str, String str2) {
        KeySet keySet;
        if (str == null || str2 == null) {
            return null;
        }
        synchronized (this.mPackages) {
            PackageParser.Package r1 = this.mPackages.get(str);
            if (r1 == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + str);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            if (filterAppAccessLPr((PackageSetting) r1.mExtras, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                Slog.w(TAG, "KeySet requested for filtered package: " + str);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            keySet = new KeySet(this.mSettings.mKeySetManagerService.getKeySetByAliasAndPackageNameLPr(str, str2));
        }
        return keySet;
    }

    public KeySet getSigningKeySet(String str) {
        KeySet keySet;
        if (str == null) {
            return null;
        }
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + str);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            if (filterAppAccessLPr((PackageSetting) r3.mExtras, callingUid, userId)) {
                Slog.w(TAG, "KeySet requested for filtered package: " + str + ", uid:" + callingUid);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            if (r3.applicationInfo.uid != callingUid && 1000 != callingUid) {
                throw new SecurityException("May not access signing KeySet of other apps.");
            }
            keySet = new KeySet(this.mSettings.mKeySetManagerService.getSigningKeySetByPackageNameLPr(str));
        }
        return keySet;
    }

    public boolean isPackageSignedByKeySet(String str, KeySet keySet) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || str == null || keySet == null) {
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 == null || filterAppAccessLPr((PackageSetting) r3.mExtras, callingUid, UserHandle.getUserId(callingUid))) {
                Slog.w(TAG, "KeySet requested for unknown package: " + str);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            IBinder token = keySet.getToken();
            if (!(token instanceof KeySetHandle)) {
                return false;
            }
            return this.mSettings.mKeySetManagerService.packageIsSignedByLPr(str, (KeySetHandle) token);
        }
    }

    public boolean isPackageSignedByKeySetExactly(String str, KeySet keySet) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || str == null || keySet == null) {
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 == null || filterAppAccessLPr((PackageSetting) r3.mExtras, callingUid, UserHandle.getUserId(callingUid))) {
                Slog.w(TAG, "KeySet requested for unknown package: " + str);
                throw new IllegalArgumentException("Unknown package: " + str);
            }
            IBinder token = keySet.getToken();
            if (!(token instanceof KeySetHandle)) {
                return false;
            }
            return this.mSettings.mKeySetManagerService.packageIsSignedByExactlyLPr(str, (KeySetHandle) token);
        }
    }

    private void deletePackageIfUnusedLPr(final String str) {
        PackageSetting packageSetting = this.mSettings.mPackages.get(str);
        if (packageSetting != null && !packageSetting.isAnyInstalled(sUserManager.getUserIds())) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() throws Throwable {
                    PackageManagerService.this.deletePackageX(str, -1L, 0, 2);
                }
            });
        }
    }

    private static void checkDowngrade(PackageParser.Package r7, PackageInfoLite packageInfoLite) throws PackageManagerException {
        if (packageInfoLite.getLongVersionCode() < r7.getLongVersionCode()) {
            throw new PackageManagerException(-25, "Update version code " + packageInfoLite.versionCode + " is older than current " + r7.getLongVersionCode());
        }
        if (packageInfoLite.getLongVersionCode() == r7.getLongVersionCode()) {
            if (packageInfoLite.baseRevisionCode < r7.baseRevisionCode) {
                throw new PackageManagerException(-25, "Update base revision code " + packageInfoLite.baseRevisionCode + " is older than current " + r7.baseRevisionCode);
            }
            if (!ArrayUtils.isEmpty(packageInfoLite.splitNames)) {
                for (int i = 0; i < packageInfoLite.splitNames.length; i++) {
                    String str = packageInfoLite.splitNames[i];
                    int iIndexOf = ArrayUtils.indexOf(r7.splitNames, str);
                    if (iIndexOf != -1 && packageInfoLite.splitRevisionCodes[i] < r7.splitRevisionCodes[iIndexOf]) {
                        throw new PackageManagerException(-25, "Update split " + str + " revision code " + packageInfoLite.splitRevisionCodes[i] + " is older than current " + r7.splitRevisionCodes[iIndexOf]);
                    }
                }
            }
        }
    }

    private static class MoveCallbacks extends Handler {
        private static final int MSG_CREATED = 1;
        private static final int MSG_STATUS_CHANGED = 2;
        private final RemoteCallbackList<IPackageMoveObserver> mCallbacks;
        private final SparseIntArray mLastStatus;

        public MoveCallbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
            this.mLastStatus = new SparseIntArray();
        }

        public void register(IPackageMoveObserver iPackageMoveObserver) {
            this.mCallbacks.register(iPackageMoveObserver);
        }

        public void unregister(IPackageMoveObserver iPackageMoveObserver) {
            this.mCallbacks.unregister(iPackageMoveObserver);
        }

        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                try {
                    invokeCallback((IPackageMoveObserver) this.mCallbacks.getBroadcastItem(i), message.what, someArgs);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            someArgs.recycle();
        }

        private void invokeCallback(IPackageMoveObserver iPackageMoveObserver, int i, SomeArgs someArgs) throws RemoteException {
            switch (i) {
                case 1:
                    iPackageMoveObserver.onCreated(someArgs.argi1, (Bundle) someArgs.arg2);
                    break;
                case 2:
                    iPackageMoveObserver.onStatusChanged(someArgs.argi1, someArgs.argi2, ((Long) someArgs.arg3).longValue());
                    break;
            }
        }

        private void notifyCreated(int i, Bundle bundle) {
            Slog.v(PackageManagerService.TAG, "Move " + i + " created " + bundle.toString());
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = i;
            someArgsObtain.arg2 = bundle;
            obtainMessage(1, someArgsObtain).sendToTarget();
        }

        private void notifyStatusChanged(int i, int i2) {
            notifyStatusChanged(i, i2, -1L);
        }

        private void notifyStatusChanged(int i, int i2, long j) {
            Slog.v(PackageManagerService.TAG, "Move " + i + " status " + i2);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = i;
            someArgsObtain.argi2 = i2;
            someArgsObtain.arg3 = Long.valueOf(j);
            obtainMessage(2, someArgsObtain).sendToTarget();
            synchronized (this.mLastStatus) {
                this.mLastStatus.put(i, i2);
            }
        }
    }

    private static final class OnPermissionChangeListeners extends Handler {
        private static final int MSG_ON_PERMISSIONS_CHANGED = 1;
        private final RemoteCallbackList<IOnPermissionsChangeListener> mPermissionListeners;

        public OnPermissionChangeListeners(Looper looper) {
            super(looper);
            this.mPermissionListeners = new RemoteCallbackList<>();
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                handleOnPermissionsChanged(message.arg1);
            }
        }

        public void addListenerLocked(IOnPermissionsChangeListener iOnPermissionsChangeListener) {
            this.mPermissionListeners.register(iOnPermissionsChangeListener);
        }

        public void removeListenerLocked(IOnPermissionsChangeListener iOnPermissionsChangeListener) {
            this.mPermissionListeners.unregister(iOnPermissionsChangeListener);
        }

        public void onPermissionsChanged(int i) {
            if (this.mPermissionListeners.getRegisteredCallbackCount() > 0) {
                obtainMessage(1, i, 0).sendToTarget();
            }
        }

        private void handleOnPermissionsChanged(int i) {
            int iBeginBroadcast = this.mPermissionListeners.beginBroadcast();
            for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                try {
                    try {
                        this.mPermissionListeners.getBroadcastItem(i2).onPermissionsChanged(i);
                    } catch (RemoteException e) {
                        Log.e(PackageManagerService.TAG, "Permission listener is dead", e);
                    }
                } finally {
                    this.mPermissionListeners.finishBroadcast();
                }
            }
        }
    }

    private class PackageManagerNative extends IPackageManagerNative.Stub {
        private PackageManagerNative() {
        }

        public String[] getNamesForUids(int[] iArr) throws RemoteException {
            String[] namesForUids = PackageManagerService.this.getNamesForUids(iArr);
            for (int length = namesForUids.length - 1; length >= 0; length--) {
                if (namesForUids[length] == null) {
                    namesForUids[length] = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                }
            }
            return namesForUids;
        }

        public String getInstallerForPackage(String str) throws RemoteException {
            String installerPackageName = PackageManagerService.this.getInstallerPackageName(str);
            if (!TextUtils.isEmpty(installerPackageName)) {
                return installerPackageName;
            }
            ApplicationInfo applicationInfo = PackageManagerService.this.getApplicationInfo(str, 0, UserHandle.getUserId(Binder.getCallingUid()));
            if (applicationInfo != null && (applicationInfo.flags & 1) != 0) {
                return "preload";
            }
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }

        public long getVersionCodeForPackage(String str) throws RemoteException {
            try {
                PackageInfo packageInfo = PackageManagerService.this.getPackageInfo(str, 0, UserHandle.getUserId(Binder.getCallingUid()));
                if (packageInfo != null) {
                    return packageInfo.getLongVersionCode();
                }
                return 0L;
            } catch (Exception e) {
                return 0L;
            }
        }
    }

    private class PackageManagerInternalImpl extends PackageManagerInternal {
        private PackageManagerInternalImpl() {
        }

        public void updatePermissionFlagsTEMP(String str, String str2, int i, int i2, int i3) {
            PackageManagerService.this.updatePermissionFlags(str, str2, i, i2, i3);
        }

        public boolean isDataRestoreSafe(byte[] bArr, String str) {
            PackageParser.SigningDetails signingDetails = getSigningDetails(str);
            if (signingDetails == null) {
                return false;
            }
            return signingDetails.hasSha256Certificate(bArr, 1);
        }

        public boolean isDataRestoreSafe(Signature signature, String str) {
            PackageParser.SigningDetails signingDetails = getSigningDetails(str);
            if (signingDetails == null) {
                return false;
            }
            return signingDetails.hasCertificate(signature, 1);
        }

        public boolean hasSignatureCapability(int i, int i2, @PackageParser.SigningDetails.CertCapabilities int i3) {
            PackageParser.SigningDetails signingDetails = getSigningDetails(i);
            PackageParser.SigningDetails signingDetails2 = getSigningDetails(i2);
            return signingDetails.checkCapability(signingDetails2, i3) || signingDetails2.hasAncestorOrSelf(signingDetails);
        }

        private PackageParser.SigningDetails getSigningDetails(String str) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageParser.Package r3 = PackageManagerService.this.mPackages.get(str);
                if (r3 == null) {
                    return null;
                }
                return r3.mSigningDetails;
            }
        }

        private PackageParser.SigningDetails getSigningDetails(int i) {
            synchronized (PackageManagerService.this.mPackages) {
                Object userIdLPr = PackageManagerService.this.mSettings.getUserIdLPr(UserHandle.getAppId(i));
                if (userIdLPr != null) {
                    if (userIdLPr instanceof SharedUserSetting) {
                        return ((SharedUserSetting) userIdLPr).signatures.mSigningDetails;
                    }
                    if (userIdLPr instanceof PackageSetting) {
                        return ((PackageSetting) userIdLPr).signatures.mSigningDetails;
                    }
                }
                return PackageParser.SigningDetails.UNKNOWN;
            }
        }

        public int getPermissionFlagsTEMP(String str, String str2, int i) {
            return PackageManagerService.this.getPermissionFlags(str, str2, i);
        }

        public boolean isInstantApp(String str, int i) {
            return PackageManagerService.this.isInstantApp(str, i);
        }

        public String getInstantAppPackageName(int i) {
            return PackageManagerService.this.getInstantAppPackageName(i);
        }

        public boolean filterAppAccess(PackageParser.Package r3, int i, int i2) {
            boolean zFilterAppAccessLPr;
            synchronized (PackageManagerService.this.mPackages) {
                zFilterAppAccessLPr = PackageManagerService.this.filterAppAccessLPr((PackageSetting) r3.mExtras, i, i2);
            }
            return zFilterAppAccessLPr;
        }

        public PackageParser.Package getPackage(String str) {
            PackageParser.Package r5;
            synchronized (PackageManagerService.this.mPackages) {
                r5 = PackageManagerService.this.mPackages.get(PackageManagerService.this.resolveInternalPackageNameLPr(str, -1L));
            }
            return r5;
        }

        public PackageList getPackageList(PackageManagerInternal.PackageListObserver packageListObserver) {
            PackageList packageList;
            synchronized (PackageManagerService.this.mPackages) {
                int size = PackageManagerService.this.mPackages.size();
                ArrayList arrayList = new ArrayList(size);
                for (int i = 0; i < size; i++) {
                    arrayList.add(PackageManagerService.this.mPackages.keyAt(i));
                }
                packageList = new PackageList(arrayList, packageListObserver);
                if (packageListObserver != null) {
                    PackageManagerService.this.mPackageListObservers.add(packageList);
                }
            }
            return packageList;
        }

        public void removePackageListObserver(PackageManagerInternal.PackageListObserver packageListObserver) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mPackageListObservers.remove(packageListObserver);
            }
        }

        public PackageParser.Package getDisabledPackage(String str) {
            PackageParser.Package r3;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting disabledSystemPkgLPr = PackageManagerService.this.mSettings.getDisabledSystemPkgLPr(str);
                r3 = disabledSystemPkgLPr != null ? disabledSystemPkgLPr.pkg : null;
            }
            return r3;
        }

        public String getKnownPackageName(int i, int i2) {
            switch (i) {
                case 0:
                    return PackageManagerService.PLATFORM_PACKAGE_NAME;
                case 1:
                    return PackageManagerService.this.mSetupWizardPackage;
                case 2:
                    return PackageManagerService.this.mRequiredInstallerPackage;
                case 3:
                    return PackageManagerService.this.mRequiredVerifierPackage;
                case 4:
                    return PackageManagerService.this.getDefaultBrowserPackageName(i2);
                case 5:
                    return PackageManagerService.this.mSystemTextClassifierPackage;
                default:
                    return null;
            }
        }

        public boolean isResolveActivityComponent(ComponentInfo componentInfo) {
            return PackageManagerService.this.mResolveActivity.packageName.equals(componentInfo.packageName) && PackageManagerService.this.mResolveActivity.name.equals(componentInfo.name);
        }

        public void setLocationPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setLocationPackagesProvider(packagesProvider);
        }

        public void setVoiceInteractionPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setVoiceInteractionPackagesProvider(packagesProvider);
        }

        public void setSmsAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setSmsAppPackagesProvider(packagesProvider);
        }

        public void setDialerAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setDialerAppPackagesProvider(packagesProvider);
        }

        public void setSimCallManagerPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setSimCallManagerPackagesProvider(packagesProvider);
        }

        public void setUseOpenWifiAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setUseOpenWifiAppPackagesProvider(packagesProvider);
        }

        public void setSyncAdapterPackagesprovider(PackageManagerInternal.SyncAdapterPackagesProvider syncAdapterPackagesProvider) {
            PackageManagerService.this.mDefaultPermissionPolicy.setSyncAdapterPackagesProvider(syncAdapterPackagesProvider);
        }

        public void grantDefaultPermissionsToDefaultSmsApp(String str, int i) {
            PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSmsApp(str, i);
        }

        public void grantDefaultPermissionsToDefaultDialerApp(String str, int i) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.setDefaultDialerPackageNameLPw(str, i);
            }
            PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultDialerApp(str, i);
        }

        public void grantDefaultPermissionsToDefaultSimCallManager(String str, int i) {
            PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSimCallManager(str, i);
        }

        public void grantDefaultPermissionsToDefaultUseOpenWifiApp(String str, int i) {
            PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultUseOpenWifiApp(str, i);
        }

        public void setKeepUninstalledPackages(List<String> list) {
            Preconditions.checkNotNull(list);
            synchronized (PackageManagerService.this.mPackages) {
                ArrayList arrayList = null;
                if (PackageManagerService.this.mKeepUninstalledPackages != null) {
                    int size = PackageManagerService.this.mKeepUninstalledPackages.size();
                    ArrayList arrayList2 = null;
                    for (int i = 0; i < size; i++) {
                        String str = (String) PackageManagerService.this.mKeepUninstalledPackages.get(i);
                        if (list == null || !list.contains(str)) {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList();
                            }
                            arrayList2.add(str);
                        }
                    }
                    arrayList = arrayList2;
                }
                PackageManagerService.this.mKeepUninstalledPackages = new ArrayList(list);
                if (arrayList != null) {
                    int size2 = arrayList.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        PackageManagerService.this.deletePackageIfUnusedLPr((String) arrayList.get(i2));
                    }
                }
            }
        }

        public boolean isPermissionsReviewRequired(String str, int i) {
            boolean zIsPermissionsReviewRequired;
            synchronized (PackageManagerService.this.mPackages) {
                zIsPermissionsReviewRequired = PackageManagerService.this.mPermissionManager.isPermissionsReviewRequired(PackageManagerService.this.mPackages.get(str), i);
            }
            return zIsPermissionsReviewRequired;
        }

        public PackageInfo getPackageInfo(String str, int i, int i2, int i3) {
            return PackageManagerService.this.getPackageInfoInternal(str, -1L, i, i2, i3);
        }

        public Bundle getSuspendedPackageLauncherExtras(String str, int i) {
            PersistableBundle persistableBundle;
            Bundle bundle;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(str);
                if (packageSetting != null) {
                    persistableBundle = packageSetting.readUserState(i).suspendedLauncherExtras;
                } else {
                    persistableBundle = null;
                }
                bundle = persistableBundle != null ? new Bundle(persistableBundle.deepCopy()) : null;
            }
            return bundle;
        }

        public boolean isPackageSuspended(String str, int i) {
            boolean suspended;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(str);
                suspended = packageSetting != null ? packageSetting.getSuspended(i) : false;
            }
            return suspended;
        }

        public String getSuspendingPackage(String str, int i) {
            String str2;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(str);
                str2 = packageSetting != null ? packageSetting.readUserState(i).suspendingPackage : null;
            }
            return str2;
        }

        public String getSuspendedDialogMessage(String str, int i) {
            String str2;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(str);
                str2 = packageSetting != null ? packageSetting.readUserState(i).dialogMessage : null;
            }
            return str2;
        }

        public int getPackageUid(String str, int i, int i2) {
            return PackageManagerService.this.getPackageUid(str, i, i2);
        }

        public ApplicationInfo getApplicationInfo(String str, int i, int i2, int i3) {
            return PackageManagerService.this.getApplicationInfoInternal(str, i, i2, i3);
        }

        public ActivityInfo getActivityInfo(ComponentName componentName, int i, int i2, int i3) {
            return PackageManagerService.this.getActivityInfoInternal(componentName, i, i2, i3);
        }

        public List<ResolveInfo> queryIntentActivities(Intent intent, int i, int i2, int i3) {
            return PackageManagerService.this.queryIntentActivitiesInternal(intent, intent.resolveTypeIfNeeded(PackageManagerService.this.mContext.getContentResolver()), i, i2, i3, false, true);
        }

        public List<ResolveInfo> queryIntentServices(Intent intent, int i, int i2, int i3) {
            return PackageManagerService.this.queryIntentServicesInternal(intent, intent.resolveTypeIfNeeded(PackageManagerService.this.mContext.getContentResolver()), i, i3, i2, false);
        }

        public ComponentName getHomeActivitiesAsUser(List<ResolveInfo> list, int i) {
            return PackageManagerService.this.getHomeActivitiesAsUser(list, i);
        }

        public ComponentName getDefaultHomeActivity(int i) {
            return PackageManagerService.this.getDefaultHomeActivity(i);
        }

        public void setDeviceAndProfileOwnerPackages(int i, String str, SparseArray<String> sparseArray) {
            PackageManagerService.this.mProtectedPackages.setDeviceAndProfileOwnerPackages(i, str, sparseArray);
            ArraySet<Integer> arraySet = new ArraySet<>();
            if (str != null) {
                arraySet.add(Integer.valueOf(i));
            }
            int size = sparseArray.size();
            for (int i2 = 0; i2 < size; i2++) {
                if (sparseArray.valueAt(i2) != null) {
                    arraySet.add(Integer.valueOf(sparseArray.keyAt(i2)));
                }
            }
            PackageManagerService.this.unsuspendForNonSystemSuspendingPackages(arraySet);
        }

        public boolean isPackageDataProtected(int i, String str) {
            return PackageManagerService.this.mProtectedPackages.isPackageDataProtected(i, str);
        }

        public boolean isPackageStateProtected(String str, int i) {
            return PackageManagerService.this.mProtectedPackages.isPackageStateProtected(i, str);
        }

        public boolean isPackageEphemeral(int i, String str) {
            boolean instantApp;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(str);
                instantApp = packageSetting != null ? packageSetting.getInstantApp(i) : false;
            }
            return instantApp;
        }

        public boolean wasPackageEverLaunched(String str, int i) {
            boolean zWasPackageEverLaunchedLPr;
            synchronized (PackageManagerService.this.mPackages) {
                zWasPackageEverLaunchedLPr = PackageManagerService.this.mSettings.wasPackageEverLaunchedLPr(str, i);
            }
            return zWasPackageEverLaunchedLPr;
        }

        public void grantRuntimePermission(String str, String str2, int i, boolean z) {
            PackageManagerService.this.mPermissionManager.grantRuntimePermission(str2, str, z, Binder.getCallingUid(), i, PackageManagerService.this.mPermissionCallback);
        }

        public void revokeRuntimePermission(String str, String str2, int i, boolean z) {
            PackageManagerService.this.mPermissionManager.revokeRuntimePermission(str2, str, z, Binder.getCallingUid(), i, PackageManagerService.this.mPermissionCallback);
        }

        public String getNameForUid(int i) {
            return PackageManagerService.this.getNameForUid(i);
        }

        public void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo auxiliaryResolveInfo, Intent intent, String str, String str2, Bundle bundle, int i) {
            PackageManagerService.this.requestInstantAppResolutionPhaseTwo(auxiliaryResolveInfo, intent, str, str2, bundle, i);
        }

        public void grantEphemeralAccess(int i, Intent intent, int i2, int i3) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mInstantAppRegistry.grantInstantAccessLPw(i, intent, i2, i3);
            }
        }

        public boolean isInstantAppInstallerComponent(ComponentName componentName) {
            boolean z;
            synchronized (PackageManagerService.this.mPackages) {
                z = PackageManagerService.this.mInstantAppInstallerActivity != null && PackageManagerService.this.mInstantAppInstallerActivity.getComponentName().equals(componentName);
            }
            return z;
        }

        public void pruneInstantApps() {
            PackageManagerService.this.mInstantAppRegistry.pruneInstantApps();
        }

        public String getSetupWizardPackageName() {
            return PackageManagerService.this.mSetupWizardPackage;
        }

        public void setExternalSourcesPolicy(PackageManagerInternal.ExternalSourcesPolicy externalSourcesPolicy) {
            if (externalSourcesPolicy != null) {
                PackageManagerService.this.mExternalSourcesPolicy = externalSourcesPolicy;
            }
        }

        public boolean isPackagePersistent(String str) {
            boolean z;
            synchronized (PackageManagerService.this.mPackages) {
                PackageParser.Package r4 = PackageManagerService.this.mPackages.get(str);
                z = false;
                if (r4 != null && (r4.applicationInfo.flags & 9) == 9) {
                    z = true;
                }
            }
            return z;
        }

        public boolean isLegacySystemApp(PackageParser.Package r3) {
            boolean z;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting packageSetting = (PackageSetting) r3.mExtras;
                z = PackageManagerService.this.mPromoteSystemApps && packageSetting.isSystem() && PackageManagerService.this.mExistingSystemPackages.contains(packageSetting.name);
            }
            return z;
        }

        public List<PackageInfo> getOverlayPackages(int i) {
            PackageInfo packageInfoGeneratePackageInfo;
            ArrayList arrayList = new ArrayList();
            synchronized (PackageManagerService.this.mPackages) {
                for (PackageParser.Package r3 : PackageManagerService.this.mPackages.values()) {
                    if (r3.mOverlayTarget != null && (packageInfoGeneratePackageInfo = PackageManagerService.this.generatePackageInfo((PackageSetting) r3.mExtras, 0, i)) != null) {
                        arrayList.add(packageInfoGeneratePackageInfo);
                    }
                }
            }
            return arrayList;
        }

        public List<String> getTargetPackageNames(int i) {
            ArrayList arrayList = new ArrayList();
            synchronized (PackageManagerService.this.mPackages) {
                for (PackageParser.Package r2 : PackageManagerService.this.mPackages.values()) {
                    if (r2.mOverlayTarget == null) {
                        arrayList.add(r2.packageName);
                    }
                }
            }
            return arrayList;
        }

        public boolean setEnabledOverlayPackages(int i, String str, List<String> list) {
            synchronized (PackageManagerService.this.mPackages) {
                if (str != null) {
                    try {
                        if (PackageManagerService.this.mPackages.get(str) != null) {
                            ArrayList arrayList = null;
                            if (list != null && list.size() > 0) {
                                int size = list.size();
                                ArrayList arrayList2 = new ArrayList(size);
                                for (int i2 = 0; i2 < size; i2++) {
                                    String str2 = list.get(i2);
                                    PackageParser.Package r6 = PackageManagerService.this.mPackages.get(str2);
                                    if (r6 == null) {
                                        Slog.e(PackageManagerService.TAG, "failed to find package " + str2);
                                        return false;
                                    }
                                    arrayList2.add(r6.baseCodePath);
                                }
                                arrayList = arrayList2;
                            }
                            PackageManagerService.this.mSettings.mPackages.get(str).setOverlayPaths(arrayList, i);
                            return true;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                Slog.e(PackageManagerService.TAG, "failed to find package " + str);
                return false;
            }
        }

        public ResolveInfo resolveIntent(Intent intent, String str, int i, int i2, boolean z, int i3) {
            return PackageManagerService.this.resolveIntentInternal(intent, str, i, i2, z, i3);
        }

        public ResolveInfo resolveService(Intent intent, String str, int i, int i2, int i3) {
            return PackageManagerService.this.resolveServiceInternal(intent, str, i, i2, i3);
        }

        public ProviderInfo resolveContentProvider(String str, int i, int i2) {
            return PackageManagerService.this.resolveContentProviderInternal(str, i, i2);
        }

        public void addIsolatedUid(int i, int i2) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIsolatedOwners.put(i, i2);
            }
        }

        public void removeIsolatedUid(int i) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIsolatedOwners.delete(i);
            }
        }

        public int getUidTargetSdkVersion(int i) {
            int uidTargetSdkVersionLockedLPr;
            synchronized (PackageManagerService.this.mPackages) {
                uidTargetSdkVersionLockedLPr = PackageManagerService.this.getUidTargetSdkVersionLockedLPr(i);
            }
            return uidTargetSdkVersionLockedLPr;
        }

        public int getPackageTargetSdkVersion(String str) {
            int packageTargetSdkVersionLockedLPr;
            synchronized (PackageManagerService.this.mPackages) {
                packageTargetSdkVersionLockedLPr = PackageManagerService.this.getPackageTargetSdkVersionLockedLPr(str);
            }
            return packageTargetSdkVersionLockedLPr;
        }

        public boolean canAccessInstantApps(int i, int i2) {
            return PackageManagerService.this.canViewInstantApps(i, i2);
        }

        public boolean canAccessComponent(int i, ComponentName componentName, int i2) {
            boolean z;
            synchronized (PackageManagerService.this.mPackages) {
                z = !PackageManagerService.this.filterAppAccessLPr(PackageManagerService.this.mSettings.mPackages.get(componentName.getPackageName()), i, componentName, 0, i2);
            }
            return z;
        }

        public boolean hasInstantApplicationMetadata(String str, int i) {
            boolean zHasInstantApplicationMetadataLPr;
            synchronized (PackageManagerService.this.mPackages) {
                zHasInstantApplicationMetadataLPr = PackageManagerService.this.mInstantAppRegistry.hasInstantApplicationMetadataLPr(str, i);
            }
            return zHasInstantApplicationMetadataLPr;
        }

        public void notifyPackageUse(String str, int i) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.notifyPackageUseLocked(str, i);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] strArr, int i) {
        enforceSystemOrPhoneCaller("grantPermissionsToEnabledCarrierApps");
        synchronized (this.mPackages) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledCarrierApps(strArr, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledImsServices(String[] strArr, int i) {
        enforceSystemOrPhoneCaller("grantDefaultPermissionsToEnabledImsServices");
        synchronized (this.mPackages) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledImsServices(strArr, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledTelephonyDataServices(final String[] strArr, final int i) {
        enforceSystemOrPhoneCaller("grantDefaultPermissionsToEnabledTelephonyDataServices");
        synchronized (this.mPackages) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
                public final void runOrThrow() {
                    this.f$0.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledTelephonyDataServices(strArr, i);
                }
            });
        }
    }

    public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(final String[] strArr, final int i) {
        enforceSystemOrPhoneCaller("revokeDefaultPermissionsFromDisabledTelephonyDataServices");
        synchronized (this.mPackages) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
                public final void runOrThrow() {
                    this.f$0.mDefaultPermissionPolicy.revokeDefaultPermissionsFromDisabledTelephonyDataServices(strArr, i);
                }
            });
        }
    }

    public void grantDefaultPermissionsToActiveLuiApp(String str, int i) {
        enforceSystemOrPhoneCaller("grantDefaultPermissionsToActiveLuiApp");
        synchronized (this.mPackages) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToActiveLuiApp(str, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void revokeDefaultPermissionsFromLuiApps(String[] strArr, int i) {
        enforceSystemOrPhoneCaller("revokeDefaultPermissionsFromLuiApps");
        synchronized (this.mPackages) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.revokeDefaultPermissionsFromLuiApps(strArr, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private static void enforceSystemOrPhoneCaller(String str) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1001 && callingUid != 1000) {
            throw new SecurityException("Cannot call " + str + " from UID " + callingUid);
        }
    }

    boolean isHistoricalPackageUsageAvailable() {
        return this.mPackageUsage.isHistoricalPackageUsageAvailable();
    }

    Collection<PackageParser.Package> getPackages() {
        ArrayList arrayList;
        synchronized (this.mPackages) {
            arrayList = new ArrayList(this.mPackages.values());
        }
        return arrayList;
    }

    public void logAppProcessStartIfNeeded(String str, int i, String str2, String str3, int i2) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || !SecurityLog.isLoggingEnabled()) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putLong("startTimestamp", System.currentTimeMillis());
        bundle.putString("processName", str);
        bundle.putInt(WatchlistLoggingHandler.WatchlistEventKeys.UID, i);
        bundle.putString("seinfo", str2);
        bundle.putString("apkFile", str3);
        bundle.putInt("pid", i2);
        Message messageObtainMessage = this.mProcessLoggingHandler.obtainMessage(1);
        messageObtainMessage.setData(bundle);
        this.mProcessLoggingHandler.sendMessage(messageObtainMessage);
    }

    public CompilerStats.PackageStats getCompilerPackageStats(String str) {
        return this.mCompilerStats.getPackageStats(str);
    }

    public CompilerStats.PackageStats getOrCreateCompilerPackageStats(PackageParser.Package r1) {
        return getOrCreateCompilerPackageStats(r1.packageName);
    }

    public CompilerStats.PackageStats getOrCreateCompilerPackageStats(String str) {
        return this.mCompilerStats.getOrCreatePackageStats(str);
    }

    public void deleteCompilerPackageStats(String str) {
        this.mCompilerStats.deletePackageStats(str);
    }

    public int getInstallReason(String str, int i) {
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, false, "get install reason");
        synchronized (this.mPackages) {
            PackageSetting packageSetting = this.mSettings.mPackages.get(str);
            if (filterAppAccessLPr(packageSetting, callingUid, i)) {
                return 0;
            }
            if (packageSetting == null) {
                return 0;
            }
            return packageSetting.getInstallReason(i);
        }
    }

    public boolean canRequestPackageInstalls(String str, int i) {
        return canRequestPackageInstallsInternal(str, 0, i, true);
    }

    private boolean canRequestPackageInstallsInternal(String str, int i, int i2, boolean z) {
        int callingUid = Binder.getCallingUid();
        int packageUid = getPackageUid(str, 0, i2);
        if (callingUid != packageUid && callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("Caller uid " + callingUid + " does not own package " + str);
        }
        ApplicationInfo applicationInfo = getApplicationInfo(str, i, i2);
        if (applicationInfo == null || applicationInfo.targetSdkVersion < 26 || isInstantApp(str, i2)) {
            return false;
        }
        if (ArrayUtils.contains(getAppOpPermissionPackages("android.permission.REQUEST_INSTALL_PACKAGES"), str)) {
            return (sUserManager.hasUserRestriction("no_install_unknown_sources", i2) || this.mExternalSourcesPolicy == null || this.mExternalSourcesPolicy.getPackageTrustedToInstallApps(str, packageUid) != 0) ? false : true;
        }
        if (!z) {
            Slog.e(TAG, "Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api");
            return false;
        }
        throw new SecurityException("Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api");
    }

    public ComponentName getInstantAppResolverSettingsComponent() {
        return this.mInstantAppResolverSettingsComponent;
    }

    public ComponentName getInstantAppInstallerComponent() {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null && this.mInstantAppInstallerActivity != null) {
            return this.mInstantAppInstallerActivity.getComponentName();
        }
        return null;
    }

    public String getInstantAppAndroidId(String str, int i) {
        String instantAppAndroidIdLPw;
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppAndroidId");
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), i, true, false, "getInstantAppAndroidId");
        if (!isInstantApp(str, i)) {
            return null;
        }
        synchronized (this.mPackages) {
            instantAppAndroidIdLPw = this.mInstantAppRegistry.getInstantAppAndroidIdLPw(str, i);
        }
        return instantAppAndroidIdLPw;
    }

    boolean canHaveOatDir(String str) {
        synchronized (this.mPackages) {
            PackageParser.Package r3 = this.mPackages.get(str);
            if (r3 == null) {
                return false;
            }
            return r3.canHaveOatDir();
        }
    }

    private String getOatDir(PackageParser.Package r3) {
        if (!r3.canHaveOatDir()) {
            return null;
        }
        File file = new File(r3.codePath);
        if (file.isDirectory()) {
            return PackageDexOptimizer.getOatDir(file).getAbsolutePath();
        }
        return null;
    }

    void deleteOatArtifactsOfPackage(String str) {
        PackageParser.Package r10;
        synchronized (this.mPackages) {
            r10 = this.mPackages.get(str);
        }
        String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(r10.applicationInfo);
        List<String> allCodePaths = r10.getAllCodePaths();
        String oatDir = getOatDir(r10);
        for (String str2 : allCodePaths) {
            for (String str3 : appDexInstructionSets) {
                try {
                    this.mInstaller.deleteOdex(str2, str3, oatDir);
                } catch (Installer.InstallerException e) {
                    Log.e(TAG, "Failed deleting oat files for " + str2, e);
                }
            }
        }
    }

    Set<String> getUnusedPackages(long j) {
        PackageManagerService packageManagerService = this;
        HashSet hashSet = new HashSet();
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (packageManagerService.mPackages) {
            Iterator<PackageParser.Package> it = packageManagerService.mPackages.values().iterator();
            while (it.hasNext()) {
                PackageParser.Package next = it.next();
                PackageSetting packageSetting = packageManagerService.mSettings.mPackages.get(next.packageName);
                if (packageSetting != null) {
                    Iterator<PackageParser.Package> it2 = it;
                    if (PackageManagerServiceUtils.isUnusedSinceTimeInMillis(packageSetting.firstInstallTime, jCurrentTimeMillis, j, getDexManager().getPackageUseInfoOrDefault(next.packageName), next.getLatestPackageUseTimeInMills(), next.getLatestForegroundPackageUseTimeInMills())) {
                        hashSet.add(next.packageName);
                    }
                    it = it2;
                    packageManagerService = this;
                }
            }
        }
        return hashSet;
    }

    public void onAmsAddedtoServiceMgr() {
        if (!sCtaManager.isCtaSupported() || !this.mIsPreNUpgrade) {
            return;
        }
        for (int i : UserManagerService.getInstance().getUserIds()) {
            this.mDefaultPermissionPolicy.grantCtaPermToPreInstalledPackage(i);
        }
    }

    private void grantCtaRuntimePerm(boolean z, PackageInstalledInfo packageInstalledInfo) {
        if (sCtaManager.needGrantCtaRuntimePerm(z, packageInstalledInfo.pkg.applicationInfo.targetSdkVersion)) {
            this.mPermissionManager.grantRequestedRuntimePermissions(packageInstalledInfo.pkg, packageInstalledInfo.newUsers, sCtaManager.getCtaOnlyPermissions(), Binder.getCallingUid(), this.mPermissionCallback);
        }
    }

    public boolean isPackageNeedsReview(PackageParser.Package r6, Settings settings) {
        SharedUserSetting sharedUserLPw;
        if (!sCtaManager.isCtaSupported()) {
            return false;
        }
        boolean z = r6.applicationInfo.targetSdkVersion >= 23;
        if (r6.mSharedUserId == null) {
            return (z && isSystemApp(r6)) ? false : true;
        }
        try {
            sharedUserLPw = settings.getSharedUserLPw(r6.mSharedUserId, 0, 0, false);
        } catch (PackageManagerException e) {
            sharedUserLPw = null;
        }
        if (sharedUserLPw != null) {
            for (PackageSetting packageSetting : sharedUserLPw.packages) {
                if (z) {
                    if (isSystemApp(packageSetting.pkg)) {
                        return false;
                    }
                } else if (packageSetting.pkg.applicationInfo.targetSdkVersion >= 23) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setHarmfulAppWarning(String str, CharSequence charSequence, int i) {
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "setHarmfulAppInfo");
        if (appId != 1000 && appId != 0 && checkUidPermission("android.permission.SET_HARMFUL_APP_WARNINGS", callingUid) != 0) {
            throw new SecurityException("Caller must have the android.permission.SET_HARMFUL_APP_WARNINGS permission.");
        }
        synchronized (this.mPackages) {
            this.mSettings.setHarmfulAppWarningLPw(str, charSequence, i);
            scheduleWritePackageRestrictionsLocked(i);
        }
    }

    public CharSequence getHarmfulAppWarning(String str, int i) {
        String harmfulAppWarningLPr;
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, true, true, "getHarmfulAppInfo");
        if (appId != 1000 && appId != 0 && checkUidPermission("android.permission.SET_HARMFUL_APP_WARNINGS", callingUid) != 0) {
            throw new SecurityException("Caller must have the android.permission.SET_HARMFUL_APP_WARNINGS permission.");
        }
        synchronized (this.mPackages) {
            harmfulAppWarningLPr = this.mSettings.getHarmfulAppWarningLPr(str, i);
        }
        return harmfulAppWarningLPr;
    }

    public boolean isPackageStateProtected(String str, int i) {
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i, false, true, "isPackageStateProtected");
        if (appId != 1000 && appId != 0 && checkUidPermission("android.permission.MANAGE_DEVICE_ADMINS", callingUid) != 0) {
            throw new SecurityException("Caller must have the android.permission.MANAGE_DEVICE_ADMINS permission.");
        }
        return this.mProtectedPackages.isPackageStateProtected(i, str);
    }
}
