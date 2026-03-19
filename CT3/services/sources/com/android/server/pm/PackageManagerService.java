package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.ResourcesManager;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.SecurityLog;
import android.app.backup.IBackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AppsQueryHelper;
import android.content.pm.EphemeralApplicationInfo;
import android.content.pm.EphemeralResolveInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IOnPermissionsChangeListener;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.PackageStats;
import android.content.pm.PackageUserState;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.PermissionRecords;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.VerifierInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.net.dhcp.DhcpPacket;
import android.os.AsyncTask;
import android.os.BenesseExtension;
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
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.IMountService;
import android.os.storage.MountServiceInternal;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
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
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.MathUtils;
import android.util.PrintStreamPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.Xml;
import android.util.jar.StrictJarFile;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.IParcelFileDescriptorFactory;
import com.android.internal.os.InstallerConnection;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppErrorDialog;
import com.android.server.am.MtkAppErrorDialog;
import com.android.server.am.ProcessList;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PermissionsState;
import com.android.server.pm.Settings;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.appworkingset.AWSDBHelper;
import com.mediatek.cta.CtaPackageManagerInternal;
import com.mediatek.cta.CtaUtils;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.pq.IAppDetectionService;
import com.mediatek.server.cta.CtaPackageManagerInternalImpl;
import com.mediatek.server.cta.CtaPermsController;
import com.mediatek.server.pm.MtkPermErrorDialog;
import dalvik.system.CloseGuard;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageManagerService extends IPackageManager.Stub {
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
    static boolean DEBUG_SD_INSTALL = false;
    private static final long DEFAULT_MANDATORY_FSTRIM_INTERVAL = 259200000;
    private static final int DEFAULT_VERIFICATION_RESPONSE = 1;
    private static final long DEFAULT_VERIFICATION_TIMEOUT = 10000;
    private static final boolean DEFAULT_VERIFY_ENABLE = true;
    private static final boolean DISABLE_EPHEMERAL_APPS = true;
    static final int END_COPY = 4;
    static final int FIND_INSTALL_LOC = 8;
    private static final int GRANT_DENIED = 1;
    private static final int GRANT_INSTALL = 2;
    private static final int GRANT_RUNTIME = 3;
    private static final int GRANT_UPGRADE = 4;
    static final int INIT_COPY = 5;
    private static final String INSTALL_PACKAGE_SUFFIX = "-";
    static final int INTENT_FILTER_VERIFIED = 18;
    private static final String KILL_APP_REASON_GIDS_CHANGED = "permission grant or revoke changed gids";
    private static final String KILL_APP_REASON_PERMISSIONS_REVOKED = "permissions revoked";
    private static final int LOG_UID = 1007;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    static final int MCS_BOUND = 3;
    static final int MCS_CHECK = 20;
    static final int MCS_GIVE_UP = 11;
    static final int MCS_RECONNECT = 10;
    static final int MCS_UNBIND = 6;
    private static final String MTK_GMO_RAM_OPTIMIZE = "ro.mtk_gmo_ram_optimize";
    private static final String MTK_GMO_ROM_OPTIMIZE = "ro.mtk_gmo_rom_optimize";
    private static final int NFC_UID = 1027;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    static final int PACKAGE_VERIFIED = 15;
    static final String PLATFORM_PACKAGE_NAME = "android";
    static final int POST_INSTALL = 9;
    private static final Set<String> PROTECTED_ACTIONS;
    private static final int RADIO_UID = 1001;
    public static final int REASON_AB_OTA = 4;
    public static final int REASON_BACKGROUND_DEXOPT = 3;
    public static final int REASON_BOOT = 1;
    public static final int REASON_CORE_APP = 8;
    public static final int REASON_FIRST_BOOT = 0;
    public static final int REASON_FORCED_DEXOPT = 7;
    public static final int REASON_INSTALL = 2;
    public static final int REASON_LAST = 8;
    public static final int REASON_NON_SYSTEM_LIBRARY = 5;
    public static final int REASON_SHARED_APK = 6;
    static final int REMOVE_CHATTY = 65536;
    static final int SCAN_BOOTING = 256;
    static final int SCAN_CHECK_ONLY = 32768;
    static final int SCAN_DEFER_DEX = 128;
    static final int SCAN_DELETE_DATA_ON_FAILURES = 1024;
    static final int SCAN_DONT_KILL_APP = 131072;
    static final int SCAN_FORCE_DEX = 4;
    static final int SCAN_IGNORE_FROZEN = 262144;
    static final int SCAN_INITIAL = 16384;
    static final int SCAN_MOVE = 8192;
    static final int SCAN_NEW_INSTALL = 16;
    static final int SCAN_NO_DEX = 2;
    static final int SCAN_NO_PATHS = 32;
    static final int SCAN_REPLACING = 2048;
    static final int SCAN_REQUIRE_KNOWN = 4096;
    static final int SCAN_TRUSTED_OVERLAY = 512;
    static final int SCAN_UPDATE_SIGNATURE = 8;
    static final int SCAN_UPDATE_TIME = 64;
    private static final String SD_ENCRYPTION_ALGORITHM = "AES";
    private static final String SD_ENCRYPTION_KEYSTORE_NAME = "AppsOnSD";
    static final int SEND_PENDING_BROADCAST = 1;
    private static final int SHELL_UID = 2000;
    private static final String SKIP_SHARED_LIBRARY_CHECK = "&";
    static final int START_CLEANING_PACKAGE = 7;
    static final int START_INTENT_FILTER_VERIFICATIONS = 17;
    private static final int SYSTEM_RUNTIME_GRANT_MASK = 52;
    static final String TAG = "PackageManager";
    private static final String TAG_ALL_GRANTS = "rt-grants";
    private static final String TAG_DEFAULT_APPS = "da";
    private static final String TAG_GRANT = "grant";
    private static final String TAG_INTENT_FILTER_VERIFICATION = "iv";
    private static final String TAG_PERMISSION = "perm";
    private static final String TAG_PERMISSION_BACKUP = "perm-grant-backup";
    private static final String TAG_PREFERRED_BACKUP = "pa";
    static final int UPDATED_MEDIA_STATUS = 12;
    static final int UPDATE_PERMISSIONS_ALL = 1;
    static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private static final int USER_RUNTIME_GRANT_MASK = 11;
    private static final String VENDOR_OVERLAY_DIR = "/vendor/overlay";
    private static final long WATCHDOG_TIMEOUT = 600000;
    static final int WRITE_PACKAGE_LIST = 19;
    static final int WRITE_PACKAGE_RESTRICTIONS = 14;
    static final int WRITE_SETTINGS = 13;
    static final int WRITE_SETTINGS_DELAY = 10000;
    private static final Comparator<ProviderInfo> mProviderInitOrderSorter;
    private static final Comparator<ResolveInfo> mResolvePrioritySorter;
    static UserManagerService sUserManager;
    ApplicationInfo mAndroidApplication;
    final File mAppInstallDir;
    private File mAppLib32InstallDir;
    final String mAsecInternalPath;
    final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    private final HashSet<String> mBlockPermReviewPkgs;
    final Context mContext;
    private CtaPermsController mCtaPermsController;
    ComponentName mCustomResolverComponentName;
    final int mDefParseFlags;
    private boolean mDeferProtectedFilters;
    final File mDrmAppPrivateInstallDir;
    private final EphemeralApplicationRegistry mEphemeralApplicationRegistry;
    final File mEphemeralInstallDir;
    final ComponentName mEphemeralInstallerComponent;
    final ComponentName mEphemeralResolverComponent;
    final EphemeralResolverConnection mEphemeralResolverConnection;
    final boolean mFactoryTest;
    private final HashSet<String> mForcePermReviewPkgs;
    boolean mFoundPolicyFile;
    final int[] mGlobalGids;
    final PackageHandler mHandler;
    final ServiceThread mHandlerThread;
    volatile boolean mHasSystemUidErrors;

    @GuardedBy("mInstallLock")
    final Installer mInstaller;
    final PackageInstallerService mInstallerService;
    private final IntentFilterVerifier<PackageParser.ActivityIntentInfo> mIntentFilterVerifier;
    private final ComponentName mIntentFilterVerifierComponent;
    final boolean mIsPreNUpgrade;
    final boolean mIsUpgrade;
    private List<String> mKeepUninstalledPackages;
    boolean mMTPROFDisable;
    ApplicationInfo mMediatekApplication;
    final DisplayMetrics mMetrics;
    private final MoveCallbacks mMoveCallbacks;
    private final OnPermissionChangeListeners mOnPermissionChangeListeners;
    final boolean mOnlyCore;
    final File mOperatorAppInstallDir;
    private final PackageDexOptimizer mPackageDexOptimizer;
    PackageParser.Package mPlatformPackage;
    final File mPluginAppInstallDir;
    private ArrayList<Message> mPostSystemReadyMessages;
    private final ProcessLoggingHandler mProcessLoggingHandler;
    boolean mPromoteSystemApps;
    final String mRequiredInstallerPackage;
    final String mRequiredVerifierPackage;
    ComponentName mResolveComponentName;
    boolean mRestoredSettings;
    volatile boolean mSafeMode;
    final String[] mSeparateProcesses;
    final String mServicesSystemSharedLibraryPackageName;

    @GuardedBy("mPackages")
    final Settings mSettings;
    final String mSetupWizardPackage;
    final String mSharedSystemSharedLibraryPackageName;
    final SparseArray<ArraySet<String>> mSystemPermissions;
    volatile boolean mSystemReady;
    private UserManagerInternal mUserManagerInternal;
    static boolean DEBUG_SETTINGS = false;
    static boolean DEBUG_PREFERRED = false;
    static boolean DEBUG_UPGRADE = false;
    static boolean DEBUG_DOMAIN_VERIFICATION = false;
    private static boolean DEBUG_BACKUP = true;
    private static boolean DEBUG_INSTALL = true;
    private static boolean DEBUG_REMOVE = true;
    private static boolean DEBUG_BROADCASTS = false;
    private static boolean DEBUG_SHOW_INFO = false;
    private static boolean DEBUG_PACKAGE_INFO = false;
    private static boolean DEBUG_INTENT_MATCHING = false;
    private static boolean DEBUG_PACKAGE_SCANNING = false;
    private static boolean DEBUG_VERIFY = false;
    private static boolean DEBUG_FILTERS = false;
    static boolean DEBUG_DEXOPT = false;
    private static boolean DEBUG_ABI_SELECTION = false;
    private static boolean DEBUG_EPHEMERAL = false;
    private static boolean DEBUG_TRIAGED_MISSING = false;
    private static boolean DEBUG_APP_DATA = false;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    private static final Intent sBrowserIntent = new Intent();
    final int mSdkVersion = Build.VERSION.SDK_INT;
    final Object mInstallLock = new Object();

    @GuardedBy("mPackages")
    final ArrayMap<String, PackageParser.Package> mPackages = new ArrayMap<>();
    final ArrayMap<String, Set<String>> mKnownCodebase = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, PackageParser.Package>> mOverlays = new ArrayMap<>();
    private final ArrayMap<String, File> mExpectingBetter = new ArrayMap<>();
    private final List<PackageParser.ActivityIntentInfo> mProtectedFilters = new ArrayList();
    private final ArraySet<String> mExistingSystemPackages = new ArraySet<>();

    @GuardedBy("mPackages")
    final ArraySet<String> mFrozenPackages = new ArraySet<>();
    final ProtectedPackages mProtectedPackages = new ProtectedPackages();
    final ArrayMap<String, SharedLibraryEntry> mSharedLibraries = new ArrayMap<>();
    final ActivityIntentResolver mActivities = new ActivityIntentResolver();
    final ActivityIntentResolver mReceivers = new ActivityIntentResolver();
    final ServiceIntentResolver mServices = new ServiceIntentResolver(this, null);
    final ProviderIntentResolver mProviders = new ProviderIntentResolver(this, null);
    final ArrayMap<String, PackageParser.Provider> mProvidersByAuthority = new ArrayMap<>();
    final ArrayMap<ComponentName, PackageParser.Instrumentation> mInstrumentation = new ArrayMap<>();
    final ArrayMap<String, PackageParser.PermissionGroup> mPermissionGroups = new ArrayMap<>();
    final ArraySet<String> mTransferedPackages = new ArraySet<>();
    final ArraySet<String> mProtectedBroadcasts = new ArraySet<>();
    final SparseArray<PackageVerificationState> mPendingVerification = new SparseArray<>();
    final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap<>();
    private AtomicInteger mNextMoveId = new AtomicInteger();
    SparseBooleanArray mUserNeedsBadging = new SparseBooleanArray();
    private int mPendingVerificationToken = 0;
    final ActivityInfo mResolveActivity = new ActivityInfo();
    final ResolveInfo mResolveInfo = new ResolveInfo();
    boolean mResolverReplaced = false;
    private int mIntentFilterVerificationToken = 0;
    final ActivityInfo mEphemeralInstallerActivity = new ActivityInfo();
    final ResolveInfo mEphemeralInstallerInfo = new ResolveInfo();
    final SparseArray<IntentFilterVerificationState> mIntentFilterVerificationStates = new SparseArray<>();
    final DefaultPermissionGrantPolicy mDefaultPermissionPolicy = new DefaultPermissionGrantPolicy(this);
    final PendingPackageBroadcasts mPendingBroadcasts = new PendingPackageBroadcasts();
    private IMediaContainerService mContainerService = null;
    private ArraySet<Integer> mDirtyUsers = new ArraySet<>();
    private boolean mServiceConnected = false;
    private int mServiceCheck = 0;
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    final SparseArray<PostInstallData> mRunningInstalls = new SparseArray<>();
    int mNextInstallToken = 1;
    private final PackageUsage mPackageUsage = new PackageUsage(this, null);
    private StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (vol.type == 1) {
                if (vol.state == 2) {
                    String volumeUuid = vol.getFsUuid();
                    PackageManagerService.this.reconcileUsers(volumeUuid);
                    PackageManagerService.this.reconcileApps(volumeUuid);
                    PackageManagerService.this.mInstallerService.onPrivateVolumeMounted(volumeUuid);
                    PackageManagerService.this.loadPrivatePackages(vol);
                } else if (vol.state == 5) {
                    PackageManagerService.this.unloadPrivatePackages(vol);
                }
            }
            if (vol.type != 0 || !vol.isPrimary()) {
                return;
            }
            if (vol.state == 2) {
                PackageManagerService.this.updateExternalMediaStatus(true, false);
            } else {
                if (vol.state != 5) {
                    return;
                }
                PackageManagerService.this.updateExternalMediaStatus(false, false);
            }
        }

        public void onVolumeForgotten(String fsUuid) {
            if (TextUtils.isEmpty(fsUuid)) {
                Slog.e(PackageManagerService.TAG, "Forgetting internal storage is probably a mistake; ignoring");
                return;
            }
            synchronized (PackageManagerService.this.mPackages) {
                List<PackageSetting> packages = PackageManagerService.this.mSettings.getVolumePackagesLPr(fsUuid);
                for (PackageSetting ps : packages) {
                    Slog.d(PackageManagerService.TAG, "Destroying " + ps.name + " because volume was forgotten");
                    PackageManagerService.this.deletePackage(ps.name, new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), 0, 2);
                }
                PackageManagerService.this.mSettings.onVolumeForgotten(fsUuid);
                PackageManagerService.this.mSettings.writeLPr();
            }
        }
    };
    private boolean mMediaMounted = false;

    private interface BlobXmlRestorer {
        void apply(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException;
    }

    private interface IntentFilterVerifier<T extends IntentFilter> {
        boolean addOneIntentFilterVerification(int i, int i2, int i3, T t, String str);

        void receiveVerificationResponse(int i);

        void startVerifications(int i);
    }

    static {
        sBrowserIntent.setAction("android.intent.action.VIEW");
        sBrowserIntent.addCategory("android.intent.category.BROWSABLE");
        sBrowserIntent.setData(Uri.parse("http:"));
        PROTECTED_ACTIONS = new ArraySet();
        PROTECTED_ACTIONS.add("android.intent.action.SEND");
        PROTECTED_ACTIONS.add("android.intent.action.SENDTO");
        PROTECTED_ACTIONS.add("android.intent.action.SEND_MULTIPLE");
        PROTECTED_ACTIONS.add("android.intent.action.VIEW");
        mResolvePrioritySorter = new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo r1, ResolveInfo r2) {
                int v1 = r1.priority;
                int v2 = r2.priority;
                if (v1 != v2) {
                    return v1 > v2 ? -1 : 1;
                }
                int v12 = r1.preferredOrder;
                int v22 = r2.preferredOrder;
                if (v12 != v22) {
                    return v12 > v22 ? -1 : 1;
                }
                if (r1.isDefault != r2.isDefault) {
                    return r1.isDefault ? -1 : 1;
                }
                int v13 = r1.match;
                int v23 = r2.match;
                if (v13 != v23) {
                    return v13 > v23 ? -1 : 1;
                }
                if (r1.system != r2.system) {
                    return r1.system ? -1 : 1;
                }
                if (r1.activityInfo != null) {
                    return r1.activityInfo.packageName.compareTo(r2.activityInfo.packageName);
                }
                if (r1.serviceInfo != null) {
                    return r1.serviceInfo.packageName.compareTo(r2.serviceInfo.packageName);
                }
                if (r1.providerInfo != null) {
                    return r1.providerInfo.packageName.compareTo(r2.providerInfo.packageName);
                }
                return 0;
            }
        };
        mProviderInitOrderSorter = new Comparator<ProviderInfo>() {
            @Override
            public int compare(ProviderInfo p1, ProviderInfo p2) {
                int v1 = p1.initOrder;
                int v2 = p2.initOrder;
                if (v1 > v2) {
                    return -1;
                }
                return v1 < v2 ? 1 : 0;
            }
        };
        DEBUG_SD_INSTALL = true;
    }

    public static final class SharedLibraryEntry {
        public final String apk;
        public final String path;

        SharedLibraryEntry(String _path, String _apk) {
            this.path = _path;
            this.apk = _apk;
        }
    }

    private static class IFVerificationParams {
        PackageParser.Package pkg;
        boolean replacing;
        int userId;
        int verifierUid;

        public IFVerificationParams(PackageParser.Package _pkg, boolean _replacing, int _userId, int _verifierUid) {
            this.pkg = _pkg;
            this.replacing = _replacing;
            this.userId = _userId;
            this.replacing = _replacing;
            this.verifierUid = _verifierUid;
        }
    }

    private class IntentVerifierProxy implements IntentFilterVerifier<PackageParser.ActivityIntentInfo> {
        private Context mContext;
        private ArrayList<Integer> mCurrentIntentFilterVerifications = new ArrayList<>();
        private ComponentName mIntentFilterVerifierComponent;

        public IntentVerifierProxy(Context context, ComponentName verifierComponent) {
            this.mContext = context;
            this.mIntentFilterVerifierComponent = verifierComponent;
        }

        private String getDefaultScheme() {
            return "https";
        }

        @Override
        public void startVerifications(int userId) {
            int count = this.mCurrentIntentFilterVerifications.size();
            for (int n = 0; n < count; n++) {
                int verificationId = this.mCurrentIntentFilterVerifications.get(n).intValue();
                IntentFilterVerificationState ivs = PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
                String packageName = ivs.getPackageName();
                ArrayList<PackageParser.ActivityIntentInfo> filters = ivs.getFilters();
                int filterCount = filters.size();
                ArraySet<String> domainsSet = new ArraySet<>();
                for (int m = 0; m < filterCount; m++) {
                    PackageParser.ActivityIntentInfo filter = filters.get(m);
                    domainsSet.addAll(filter.getHostsList());
                }
                ArrayList<String> domainsList = new ArrayList<>(domainsSet);
                synchronized (PackageManagerService.this.mPackages) {
                    if (PackageManagerService.this.mSettings.createIntentFilterVerificationIfNeededLPw(packageName, domainsList) != null) {
                        PackageManagerService.this.scheduleWriteSettingsLocked();
                    }
                }
                sendVerificationRequest(userId, verificationId, ivs);
            }
            this.mCurrentIntentFilterVerifications.clear();
        }

        private void sendVerificationRequest(int userId, int verificationId, IntentFilterVerificationState ivs) {
            Intent verificationIntent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION");
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_ID", verificationId);
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_URI_SCHEME", getDefaultScheme());
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_HOSTS", ivs.getHostsString());
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_PACKAGE_NAME", ivs.getPackageName());
            verificationIntent.setComponent(this.mIntentFilterVerifierComponent);
            verificationIntent.addFlags(268435456);
            UserHandle user = new UserHandle(userId);
            this.mContext.sendBroadcastAsUser(verificationIntent, user);
            if (!PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                return;
            }
            Slog.d(PackageManagerService.TAG, "Sending IntentFilter verification broadcast");
        }

        @Override
        public void receiveVerificationResponse(int verificationId) {
            IntentFilterVerificationInfo ivi;
            IntentFilterVerificationState ivs = PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
            boolean verified = ivs.isVerified();
            ArrayList<PackageParser.ActivityIntentInfo> filters = ivs.getFilters();
            int count = filters.size();
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.i(PackageManagerService.TAG, "Received verification response " + verificationId + " for " + count + " filters, verified=" + verified);
            }
            for (int n = 0; n < count; n++) {
                PackageParser.ActivityIntentInfo filter = filters.get(n);
                filter.setVerified(verified);
                if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                    Slog.d(PackageManagerService.TAG, "IntentFilter " + filter.toString() + " verified with result:" + verified + " and hosts:" + ivs.getHostsString());
                }
            }
            PackageManagerService.this.mIntentFilterVerificationStates.remove(verificationId);
            String packageName = ivs.getPackageName();
            synchronized (PackageManagerService.this.mPackages) {
                ivi = PackageManagerService.this.mSettings.getIntentFilterVerificationLPr(packageName);
            }
            if (ivi == null) {
                Slog.w(PackageManagerService.TAG, "IntentFilterVerificationInfo not found for verificationId:" + verificationId + " packageName:" + packageName);
                return;
            }
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(PackageManagerService.TAG, "Updating IntentFilterVerificationInfo for package " + packageName + " verificationId:" + verificationId);
            }
            synchronized (PackageManagerService.this.mPackages) {
                if (verified) {
                    ivi.setStatus(2);
                } else {
                    ivi.setStatus(1);
                }
                PackageManagerService.this.scheduleWriteSettingsLocked();
                int userId = ivs.getUserId();
                if (userId != -1) {
                    int userStatus = PackageManagerService.this.mSettings.getIntentFilterVerificationStatusLPr(packageName, userId);
                    int updatedStatus = 0;
                    boolean needUpdate = false;
                    switch (userStatus) {
                        case 0:
                            updatedStatus = verified ? 2 : 1;
                            needUpdate = true;
                            break;
                        case 1:
                            if (verified) {
                                updatedStatus = 2;
                                needUpdate = true;
                            }
                            break;
                    }
                    if (needUpdate) {
                        PackageManagerService.this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, updatedStatus, userId);
                        PackageManagerService.this.scheduleWritePackageRestrictionsLocked(userId);
                    }
                }
            }
        }

        @Override
        public boolean addOneIntentFilterVerification(int verifierUid, int userId, int verificationId, PackageParser.ActivityIntentInfo filter, String packageName) {
            if (!PackageManagerService.hasValidDomains(filter)) {
                return false;
            }
            IntentFilterVerificationState ivs = PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
            if (ivs == null) {
                ivs = createDomainVerificationState(verifierUid, userId, verificationId, packageName);
            }
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(PackageManagerService.TAG, "Adding verification filter for " + packageName + ": " + filter);
            }
            ivs.addFilter(filter);
            return true;
        }

        private IntentFilterVerificationState createDomainVerificationState(int verifierUid, int userId, int verificationId, String packageName) {
            IntentFilterVerificationState ivs = new IntentFilterVerificationState(verifierUid, userId, packageName);
            ivs.setPendingState();
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIntentFilterVerificationStates.append(verificationId, ivs);
                this.mCurrentIntentFilterVerifications.add(Integer.valueOf(verificationId));
            }
            return ivs;
        }
    }

    private static boolean hasValidDomains(PackageParser.ActivityIntentInfo filter) {
        if (!filter.hasCategory("android.intent.category.BROWSABLE")) {
            return false;
        }
        if (filter.hasDataScheme("http")) {
            return true;
        }
        return filter.hasDataScheme("https");
    }

    static class PendingPackageBroadcasts {
        final SparseArray<ArrayMap<String, ArrayList<String>>> mUidMap = new SparseArray<>(2);

        public ArrayList<String> get(int userId, String packageName) {
            ArrayMap<String, ArrayList<String>> packages = getOrAllocate(userId);
            return packages.get(packageName);
        }

        public void put(int userId, String packageName, ArrayList<String> components) {
            ArrayMap<String, ArrayList<String>> packages = getOrAllocate(userId);
            packages.put(packageName, components);
        }

        public void remove(int userId, String packageName) {
            ArrayMap<String, ArrayList<String>> packages = this.mUidMap.get(userId);
            if (packages == null) {
                return;
            }
            packages.remove(packageName);
        }

        public void remove(int userId) {
            this.mUidMap.remove(userId);
        }

        public int userIdCount() {
            return this.mUidMap.size();
        }

        public int userIdAt(int n) {
            return this.mUidMap.keyAt(n);
        }

        public ArrayMap<String, ArrayList<String>> packagesForUserId(int userId) {
            return this.mUidMap.get(userId);
        }

        public int size() {
            int num = 0;
            for (int i = 0; i < this.mUidMap.size(); i++) {
                num += this.mUidMap.valueAt(i).size();
            }
            return num;
        }

        public void clear() {
            this.mUidMap.clear();
        }

        private ArrayMap<String, ArrayList<String>> getOrAllocate(int userId) {
            ArrayMap<String, ArrayList<String>> map = this.mUidMap.get(userId);
            if (map == null) {
                ArrayMap<String, ArrayList<String>> map2 = new ArrayMap<>();
                this.mUidMap.put(userId, map2);
                return map2;
            }
            return map;
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "onServiceConnected");
            }
            PackageManagerService.this.mServiceConnected = true;
            PackageManagerService.this.mServiceCheck = 0;
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "onServiceConnected: " + PackageManagerService.this.mServiceConnected + ", " + PackageManagerService.this.mServiceCheck);
            }
            IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
            PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(3, imcs));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "onServiceDisconnected");
            }
            PackageManagerService.this.mServiceConnected = false;
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "onServiceDisconnected: " + PackageManagerService.this.mServiceConnected);
            }
        }
    }

    static class PostInstallData {
        public InstallArgs args;
        public PackageInstalledInfo res;

        PostInstallData(InstallArgs _a, PackageInstalledInfo _r) {
            this.args = _a;
            this.res = _r;
        }
    }

    private class PackageUsage {
        private static final String USAGE_FILE_MAGIC = "PACKAGE_USAGE__VERSION_";
        private static final String USAGE_FILE_MAGIC_VERSION_1 = "PACKAGE_USAGE__VERSION_1";
        private final int WRITE_INTERVAL;
        private final AtomicBoolean mBackgroundWriteRunning;
        private final Object mFileLock;
        private boolean mIsHistoricalPackageUsageAvailable;
        private final AtomicLong mLastWritten;

        PackageUsage(PackageManagerService this$0, PackageUsage packageUsage) {
            this();
        }

        private PackageUsage() {
            this.WRITE_INTERVAL = PackageManagerService.DEBUG_DEXOPT ? 0 : ProcessList.PSS_MAX_INTERVAL;
            this.mFileLock = new Object();
            this.mLastWritten = new AtomicLong(0L);
            this.mBackgroundWriteRunning = new AtomicBoolean(false);
            this.mIsHistoricalPackageUsageAvailable = true;
        }

        boolean isHistoricalPackageUsageAvailable() {
            return this.mIsHistoricalPackageUsageAvailable;
        }

        void write(boolean force) {
            if (force) {
                writeInternal();
            } else {
                if ((SystemClock.elapsedRealtime() - this.mLastWritten.get() < this.WRITE_INTERVAL && !PackageManagerService.DEBUG_DEXOPT) || !this.mBackgroundWriteRunning.compareAndSet(false, true)) {
                    return;
                }
                new Thread("PackageUsage_DiskWriter") {
                    @Override
                    public void run() {
                        try {
                            PackageUsage.this.writeInternal();
                        } finally {
                            PackageUsage.this.mBackgroundWriteRunning.set(false);
                        }
                    }
                }.start();
            }
        }

        private void writeInternal() {
            synchronized (PackageManagerService.this.mPackages) {
                synchronized (this.mFileLock) {
                    AtomicFile file = getFile();
                    FileOutputStream f = null;
                    try {
                        f = file.startWrite();
                        BufferedOutputStream out = new BufferedOutputStream(f);
                        FileUtils.setPermissions(file.getBaseFile().getPath(), 416, 1000, 1032);
                        StringBuilder sb = new StringBuilder();
                        sb.append(USAGE_FILE_MAGIC_VERSION_1);
                        sb.append('\n');
                        out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
                        for (PackageParser.Package pkg : PackageManagerService.this.mPackages.values()) {
                            if (pkg.getLatestPackageUseTimeInMills() != 0) {
                                sb.setLength(0);
                                sb.append(pkg.packageName);
                                for (long usageTimeInMillis : pkg.mLastPackageUsageTimeInMills) {
                                    sb.append(' ');
                                    sb.append(usageTimeInMillis);
                                }
                                sb.append('\n');
                                out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
                            }
                        }
                        out.flush();
                        file.finishWrite(f);
                    } catch (IOException e) {
                        if (f != null) {
                            file.failWrite(f);
                        }
                        Log.e(PackageManagerService.TAG, "Failed to write package usage times", e);
                    }
                }
            }
            this.mLastWritten.set(SystemClock.elapsedRealtime());
        }

        void readLP() {
            BufferedInputStream in;
            synchronized (this.mFileLock) {
                AtomicFile file = getFile();
                BufferedInputStream bufferedInputStream = null;
                try {
                    try {
                        in = new BufferedInputStream(file.openRead());
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (FileNotFoundException e) {
                } catch (IOException e2) {
                    e = e2;
                }
                try {
                    StringBuffer sb = new StringBuffer();
                    String firstLine = readLine(in, sb);
                    if (firstLine != null) {
                        if (USAGE_FILE_MAGIC_VERSION_1.equals(firstLine)) {
                            readVersion1LP(in, sb);
                        } else {
                            readVersion0LP(in, sb, firstLine);
                        }
                    }
                    IoUtils.closeQuietly(in);
                    bufferedInputStream = in;
                } catch (FileNotFoundException e3) {
                    bufferedInputStream = in;
                    this.mIsHistoricalPackageUsageAvailable = false;
                    IoUtils.closeQuietly(bufferedInputStream);
                } catch (IOException e4) {
                    e = e4;
                    bufferedInputStream = in;
                    Log.w(PackageManagerService.TAG, "Failed to read package usage times", e);
                    IoUtils.closeQuietly(bufferedInputStream);
                } catch (Throwable th2) {
                    th = th2;
                    bufferedInputStream = in;
                    IoUtils.closeQuietly(bufferedInputStream);
                    throw th;
                }
            }
            this.mLastWritten.set(SystemClock.elapsedRealtime());
        }

        private void readVersion0LP(InputStream in, StringBuffer sb, String firstLine) throws IOException {
            String line = firstLine;
            while (line != null) {
                String[] tokens = line.split(" ");
                if (tokens.length != 2) {
                    throw new IOException("Failed to parse " + line + " as package-timestamp pair.");
                }
                String packageName = tokens[0];
                PackageParser.Package pkg = PackageManagerService.this.mPackages.get(packageName);
                if (pkg != null) {
                    long timestamp = parseAsLong(tokens[1]);
                    for (int reason = 0; reason < 8; reason++) {
                        pkg.mLastPackageUsageTimeInMills[reason] = timestamp;
                    }
                }
                line = readLine(in, sb);
            }
        }

        private void readVersion1LP(InputStream in, StringBuffer sb) throws IOException {
            while (true) {
                String line = readLine(in, sb);
                if (line == null) {
                    return;
                }
                String[] tokens = line.split(" ");
                if (tokens.length != 9) {
                    throw new IOException("Failed to parse " + line + " as a timestamp array.");
                }
                String packageName = tokens[0];
                PackageParser.Package pkg = PackageManagerService.this.mPackages.get(packageName);
                if (pkg != null) {
                    for (int reason = 0; reason < 8; reason++) {
                        pkg.mLastPackageUsageTimeInMills[reason] = parseAsLong(tokens[reason + 1]);
                    }
                }
            }
        }

        private long parseAsLong(String token) throws IOException {
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException e) {
                throw new IOException("Failed to parse " + token + " as a long.", e);
            }
        }

        private String readLine(InputStream in, StringBuffer sb) throws IOException {
            return readToken(in, sb, '\n');
        }

        private String readToken(InputStream in, StringBuffer sb, char endOfToken) throws IOException {
            sb.setLength(0);
            while (true) {
                int ch = in.read();
                if (ch == -1) {
                    if (sb.length() == 0) {
                        return null;
                    }
                    throw new IOException("Unexpected EOF");
                }
                if (ch == endOfToken) {
                    return sb.toString();
                }
                sb.append((char) ch);
            }
        }

        private AtomicFile getFile() {
            File dataDir = Environment.getDataDirectory();
            File systemDir = new File(dataDir, "system");
            File fname = new File(systemDir, "package-usage.list");
            return new AtomicFile(fname);
        }
    }

    class PackageHandler extends Handler {
        private boolean mBound;
        final ArrayList<HandlerParams> mPendingInstalls;

        private boolean connectToService() {
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "Trying to bind to DefaultContainerService");
            }
            Intent service = new Intent().setComponent(PackageManagerService.DEFAULT_CONTAINER_COMPONENT);
            Process.setThreadPriority(0);
            if (PackageManagerService.this.mContext.bindServiceAsUser(service, PackageManagerService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                Process.setThreadPriority(10);
                this.mBound = true;
                Message msg = PackageManagerService.this.mHandler.obtainMessage(20);
                PackageManagerService.this.mHandler.sendMessageDelayed(msg, 1000L);
                return true;
            }
            Process.setThreadPriority(10);
            return false;
        }

        private void disconnectService() {
            PackageManagerService.this.mContainerService = null;
            this.mBound = false;
            PackageManagerService.this.mServiceConnected = false;
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Log.i(PackageManagerService.TAG, "disconnectService: " + PackageManagerService.this.mServiceConnected);
            }
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
        public void handleMessage(Message msg) {
            try {
                doHandleMessage(msg);
            } finally {
                Process.setThreadPriority(10);
            }
        }

        void doHandleMessage(Message msg) {
            int ret;
            switch (msg.what) {
                case 1:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        if (PackageManagerService.this.mPendingBroadcasts == null) {
                            return;
                        }
                        int size = PackageManagerService.this.mPendingBroadcasts.size();
                        if (size <= 0) {
                            return;
                        }
                        String[] packages = new String[size];
                        ArrayList<String>[] components = new ArrayList[size];
                        int[] uids = new int[size];
                        int i = 0;
                        for (int n = 0; n < PackageManagerService.this.mPendingBroadcasts.userIdCount(); n++) {
                            int packageUserId = PackageManagerService.this.mPendingBroadcasts.userIdAt(n);
                            Iterator<Map.Entry<String, ArrayList<String>>> it = PackageManagerService.this.mPendingBroadcasts.packagesForUserId(packageUserId).entrySet().iterator();
                            while (it.hasNext() && i < size) {
                                Map.Entry<String, ArrayList<String>> ent = it.next();
                                packages[i] = ent.getKey();
                                components[i] = ent.getValue();
                                PackageSetting ps = PackageManagerService.this.mSettings.mPackages.get(ent.getKey());
                                uids[i] = ps != null ? UserHandle.getUid(packageUserId, ps.appId) : -1;
                                i++;
                            }
                        }
                        int size2 = i;
                        PackageManagerService.this.mPendingBroadcasts.clear();
                        for (int i2 = 0; i2 < size2; i2++) {
                            PackageManagerService.this.sendPackageChangedBroadcast(packages[i2], true, components[i2], uids[i2]);
                        }
                        Process.setThreadPriority(10);
                        return;
                    }
                case 2:
                case 4:
                case 8:
                default:
                    return;
                case 3:
                    if (PackageManagerService.DEBUG_UPGRADE) {
                        Slog.i(PackageManagerService.TAG, "mcs_bound");
                    }
                    if (msg.obj != null) {
                        PackageManagerService.this.mContainerService = (IMediaContainerService) msg.obj;
                        Trace.asyncTraceEnd(1048576L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                    }
                    if (PackageManagerService.this.mContainerService == null) {
                        if (this.mBound) {
                            Slog.w(PackageManagerService.TAG, "Waiting to connect to media container service");
                            return;
                        }
                        Slog.e(PackageManagerService.TAG, "Cannot bind to media container service");
                        Iterator params$iterator = this.mPendingInstalls.iterator();
                        if (!params$iterator.hasNext()) {
                            this.mPendingInstalls.clear();
                            return;
                        }
                        HandlerParams params = (HandlerParams) params$iterator.next();
                        params.serviceError();
                        Trace.asyncTraceEnd(1048576L, "queueInstall", System.identityHashCode(params));
                        if (params.traceMethod != null) {
                            Trace.asyncTraceEnd(1048576L, params.traceMethod, params.traceCookie);
                            return;
                        }
                        return;
                    }
                    if (this.mPendingInstalls.size() <= 0) {
                        Slog.w(PackageManagerService.TAG, "Empty queue");
                        return;
                    }
                    HandlerParams params2 = this.mPendingInstalls.get(0);
                    if (params2 != null) {
                        Trace.asyncTraceEnd(1048576L, "queueInstall", System.identityHashCode(params2));
                        Trace.traceBegin(1048576L, "startCopy");
                        if (params2.startCopy()) {
                            if (PackageManagerService.DEBUG_UPGRADE) {
                                Log.i(PackageManagerService.TAG, "Checking for more work or unbind...");
                            }
                            if (this.mPendingInstalls.size() > 0) {
                                this.mPendingInstalls.remove(0);
                            }
                            if (this.mPendingInstalls.size() != 0) {
                                if (PackageManagerService.DEBUG_UPGRADE) {
                                    Log.i(PackageManagerService.TAG, "Posting MCS_BOUND for next work");
                                }
                                PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            } else if (this.mBound) {
                                if (PackageManagerService.DEBUG_UPGRADE) {
                                    Log.i(PackageManagerService.TAG, "Posting delayed MCS_UNBIND");
                                }
                                removeMessages(6);
                                Message ubmsg = obtainMessage(6);
                                sendMessageDelayed(ubmsg, 10000L);
                            }
                        }
                        Trace.traceEnd(1048576L);
                        return;
                    }
                    return;
                case 5:
                    HandlerParams params3 = (HandlerParams) msg.obj;
                    int idx = this.mPendingInstalls.size();
                    if (PackageManagerService.DEBUG_UPGRADE) {
                        Slog.i(PackageManagerService.TAG, "init_copy idx=" + idx + ": " + params3);
                    }
                    if (this.mBound) {
                        this.mPendingInstalls.add(idx, params3);
                        if (idx == 0) {
                            PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            return;
                        }
                        return;
                    }
                    Trace.asyncTraceBegin(1048576L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                    if (connectToService()) {
                        this.mPendingInstalls.add(idx, params3);
                        return;
                    }
                    Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                    params3.serviceError();
                    Trace.asyncTraceEnd(1048576L, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                    if (params3.traceMethod != null) {
                        Trace.asyncTraceEnd(1048576L, params3.traceMethod, params3.traceCookie);
                        return;
                    }
                    return;
                case 6:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_unbind");
                    }
                    if (this.mPendingInstalls.size() != 0 || PackageManagerService.this.mPendingVerification.size() != 0) {
                        if (this.mPendingInstalls.size() > 0) {
                            PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            return;
                        }
                        return;
                    } else {
                        if (this.mBound) {
                            if (PackageManagerService.DEBUG_INSTALL) {
                                Slog.i(PackageManagerService.TAG, "calling disconnectService()");
                            }
                            disconnectService();
                            return;
                        }
                        return;
                    }
                case 7:
                    Process.setThreadPriority(0);
                    String packageName = (String) msg.obj;
                    int userId = msg.arg1;
                    boolean andCode = msg.arg2 != 0;
                    synchronized (PackageManagerService.this.mPackages) {
                        if (userId == -1) {
                            int[] users = PackageManagerService.sUserManager.getUserIds();
                            for (int user : users) {
                                PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(user, packageName, andCode));
                            }
                        } else {
                            PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(userId, packageName, andCode));
                        }
                    }
                    Process.setThreadPriority(10);
                    PackageManagerService.this.startCleaningPackages();
                    return;
                case 9:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Log.v(PackageManagerService.TAG, "Handling post-install for " + msg.arg1);
                    }
                    PostInstallData data = PackageManagerService.this.mRunningInstalls.get(msg.arg1);
                    boolean didRestore = msg.arg2 != 0;
                    PackageManagerService.this.mRunningInstalls.delete(msg.arg1);
                    if (data != null) {
                        InstallArgs args = data.args;
                        PackageInstalledInfo parentRes = data.res;
                        boolean grantPermissions = (args.installFlags & 256) != 0;
                        boolean killApp = (args.installFlags & 4096) == 0;
                        String[] grantedPermissions = args.installGrantPermissions;
                        PackageManagerService.this.handlePackagePostInstall(parentRes, grantPermissions, killApp, grantedPermissions, didRestore, args.installerPackageName, args.observer);
                        int childCount = parentRes.addedChildPackages != null ? parentRes.addedChildPackages.size() : 0;
                        for (int i3 = 0; i3 < childCount; i3++) {
                            PackageInstalledInfo childRes = parentRes.addedChildPackages.valueAt(i3);
                            PackageManagerService.this.handlePackagePostInstall(childRes, grantPermissions, killApp, grantedPermissions, false, args.installerPackageName, args.observer);
                        }
                        if (args.traceMethod != null) {
                            Trace.asyncTraceEnd(1048576L, args.traceMethod, args.traceCookie);
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Bogus post-install token " + msg.arg1);
                    }
                    Trace.asyncTraceEnd(1048576L, "postInstall", msg.arg1);
                    return;
                case 10:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_reconnect");
                    }
                    if (this.mPendingInstalls.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (connectToService()) {
                            return;
                        }
                        Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                        for (HandlerParams params4 : this.mPendingInstalls) {
                            params4.serviceError();
                            Trace.asyncTraceEnd(1048576L, "queueInstall", System.identityHashCode(params4));
                        }
                        this.mPendingInstalls.clear();
                        return;
                    }
                    return;
                case 11:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_giveup too many retries");
                    }
                    Trace.asyncTraceEnd(1048576L, "queueInstall", System.identityHashCode(this.mPendingInstalls.remove(0)));
                    return;
                case 12:
                    if (PackageManagerService.DEBUG_SD_INSTALL) {
                        Log.i(PackageManagerService.TAG, "Got message UPDATED_MEDIA_STATUS");
                    }
                    boolean reportStatus = msg.arg1 == 1;
                    boolean doGc = msg.arg2 == 1;
                    if (PackageManagerService.DEBUG_SD_INSTALL) {
                        Log.i(PackageManagerService.TAG, "reportStatus=" + reportStatus + ", doGc = " + doGc);
                    }
                    if (doGc) {
                        Runtime.getRuntime().gc();
                    }
                    if (msg.obj != null) {
                        Set<AsecInstallArgs> args2 = (Set) msg.obj;
                        if (PackageManagerService.DEBUG_SD_INSTALL) {
                            Log.i(PackageManagerService.TAG, "Unloading all containers");
                        }
                        PackageManagerService.this.unloadAllContainers(args2);
                    }
                    if (reportStatus) {
                        try {
                            if (PackageManagerService.DEBUG_SD_INSTALL) {
                                Log.i(PackageManagerService.TAG, "Invoking MountService call back");
                            }
                            PackageHelper.getMountService().finishMediaUpdate();
                            return;
                        } catch (RemoteException e) {
                            Log.e(PackageManagerService.TAG, "MountService not running?");
                            return;
                        }
                    }
                    return;
                case 13:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(13);
                        removeMessages(14);
                        PackageManagerService.this.mSettings.writeLPr();
                        PackageManagerService.this.mDirtyUsers.clear();
                    }
                    Process.setThreadPriority(10);
                    return;
                case 14:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(14);
                        Iterator userId$iterator = PackageManagerService.this.mDirtyUsers.iterator();
                        while (userId$iterator.hasNext()) {
                            PackageManagerService.this.mSettings.writePackageRestrictionsLPr(((Integer) userId$iterator.next()).intValue());
                        }
                        PackageManagerService.this.mDirtyUsers.clear();
                    }
                    Process.setThreadPriority(10);
                    return;
                case 15:
                    int verificationId = msg.arg1;
                    PackageVerificationState state = PackageManagerService.this.mPendingVerification.get(verificationId);
                    if (state == null) {
                        Slog.w(PackageManagerService.TAG, "Invalid verification token " + verificationId + " received");
                        return;
                    }
                    PackageVerificationResponse response = (PackageVerificationResponse) msg.obj;
                    state.setVerifierResponse(response.callerUid, response.code);
                    if (state.isVerificationComplete()) {
                        PackageManagerService.this.mPendingVerification.remove(verificationId);
                        InstallArgs args3 = state.getInstallArgs();
                        Uri originUri = Uri.fromFile(args3.origin.resolvedFile);
                        if (state.isInstallAllowed()) {
                            ret = -110;
                            PackageManagerService.this.broadcastPackageVerified(verificationId, originUri, response.code, state.getInstallArgs().getUser());
                            try {
                                ret = args3.copyApk(PackageManagerService.this.mContainerService, true);
                            } catch (RemoteException e2) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                            }
                            break;
                        } else {
                            ret = -22;
                        }
                        Trace.asyncTraceEnd(1048576L, "verification", verificationId);
                        PackageManagerService.this.processPendingInstall(args3, ret);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        return;
                    }
                    return;
                case 16:
                    int verificationId2 = msg.arg1;
                    PackageVerificationState state2 = PackageManagerService.this.mPendingVerification.get(verificationId2);
                    if (state2 == null || state2.timeoutExtended()) {
                        return;
                    }
                    InstallArgs args4 = state2.getInstallArgs();
                    Uri originUri2 = Uri.fromFile(args4.origin.resolvedFile);
                    Slog.i(PackageManagerService.TAG, "Verification timed out for " + originUri2);
                    PackageManagerService.this.mPendingVerification.remove(verificationId2);
                    int ret2 = -22;
                    if (PackageManagerService.this.getDefaultVerificationResponse() == 1) {
                        Slog.i(PackageManagerService.TAG, "Continuing with installation of " + originUri2);
                        state2.setVerifierResponse(Binder.getCallingUid(), 2);
                        PackageManagerService.this.broadcastPackageVerified(verificationId2, originUri2, 1, state2.getInstallArgs().getUser());
                        try {
                            ret2 = args4.copyApk(PackageManagerService.this.mContainerService, true);
                        } catch (RemoteException e3) {
                            Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                        }
                        break;
                    } else {
                        PackageManagerService.this.broadcastPackageVerified(verificationId2, originUri2, -1, state2.getInstallArgs().getUser());
                    }
                    Trace.asyncTraceEnd(1048576L, "verification", verificationId2);
                    PackageManagerService.this.processPendingInstall(args4, ret2);
                    PackageManagerService.this.mHandler.sendEmptyMessage(6);
                    return;
                case 17:
                    IFVerificationParams params5 = (IFVerificationParams) msg.obj;
                    PackageManagerService.this.verifyIntentFiltersIfNeeded(params5.userId, params5.verifierUid, params5.replacing, params5.pkg);
                    return;
                case 18:
                    int verificationId3 = msg.arg1;
                    IntentFilterVerificationState state3 = PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId3);
                    if (state3 == null) {
                        Slog.w(PackageManagerService.TAG, "Invalid IntentFilter verification token " + verificationId3 + " received");
                        return;
                    }
                    int userId2 = state3.getUserId();
                    if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "Processing IntentFilter verification with token:" + verificationId3 + " and userId:" + userId2);
                    }
                    IntentFilterVerificationResponse response2 = (IntentFilterVerificationResponse) msg.obj;
                    state3.setVerifierResponse(response2.callerUid, response2.code);
                    if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "IntentFilter verification with token:" + verificationId3 + " and userId:" + userId2 + " is settings verifier response with response code:" + response2.code);
                    }
                    if (response2.code == -1 && PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                        Slog.d(PackageManagerService.TAG, "Domains failing verification: " + response2.getFailedDomainsString());
                    }
                    if (state3.isVerificationComplete()) {
                        PackageManagerService.this.mIntentFilterVerifier.receiveVerificationResponse(verificationId3);
                        return;
                    } else {
                        if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                            Slog.d(PackageManagerService.TAG, "IntentFilter verification with token:" + verificationId3 + " was not said to be complete");
                            return;
                        }
                        return;
                    }
                case 19:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(19);
                        PackageManagerService.this.mSettings.writePackageListLPr(msg.arg1);
                    }
                    Process.setThreadPriority(10);
                    return;
                case 20:
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i(PackageManagerService.TAG, "mcs_check");
                    }
                    PackageManagerService.this.mServiceCheck++;
                    Slog.i(PackageManagerService.TAG, "mcs_check(" + PackageManagerService.this.mServiceConnected + ", " + PackageManagerService.this.mServiceCheck + ")");
                    if (PackageManagerService.this.mServiceConnected || PackageManagerService.this.mServiceCheck > 3) {
                        return;
                    }
                    connectToService();
                    return;
            }
        }
    }

    private void handlePackagePostInstall(PackageInstalledInfo res, boolean grantPermissions, boolean killApp, String[] grantedPermissions, boolean launchedForRestore, String installerPackage, IPackageInstallObserver2 installObserver) {
        if (res.returnCode == 1) {
            if (res.removedInfo != null) {
                res.removedInfo.sendPackageRemovedBroadcasts(killApp);
            }
            if (grantPermissions && res.pkg.applicationInfo.targetSdkVersion >= 23) {
                grantRequestedRuntimePermissions(res.pkg, res.newUsers, grantedPermissions);
            }
            boolean update = (res.removedInfo == null || res.removedInfo.removedPackage == null) ? false : true;
            if (!update && CtaUtils.isCtaSupported() && res.pkg.applicationInfo.targetSdkVersion >= 23) {
                grantRequestedRuntimePermissions(res.pkg, res.newUsers, CtaUtils.getCtaOnlyPermissions());
            }
            if (res.pkg.parentPackage != null) {
                synchronized (this.mPackages) {
                    grantRuntimePermissionsGrantedToDisabledPrivSysPackageParentLPw(res.pkg);
                }
            }
            synchronized (this.mPackages) {
                this.mEphemeralApplicationRegistry.onPackageInstalledLPw(res.pkg);
            }
            String packageName = res.pkg.applicationInfo.packageName;
            Bundle extras = new Bundle(1);
            extras.putInt("android.intent.extra.UID", res.uid);
            int[] firstUsers = EMPTY_INT_ARRAY;
            int[] updateUsers = EMPTY_INT_ARRAY;
            if (res.origUsers == null || res.origUsers.length == 0) {
                firstUsers = res.newUsers;
            } else {
                for (int newUser : res.newUsers) {
                    boolean isNew = true;
                    int[] iArr = res.origUsers;
                    int i = 0;
                    int length = iArr.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        int origUser = iArr[i];
                        if (origUser != newUser) {
                            i++;
                        } else {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        firstUsers = ArrayUtils.appendInt(firstUsers, newUser);
                    } else {
                        updateUsers = ArrayUtils.appendInt(updateUsers, newUser);
                    }
                }
            }
            if (!isEphemeral(res.pkg)) {
                this.mProcessLoggingHandler.invalidateProcessLoggingBaseApkHash(res.pkg.baseCodePath);
                sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, firstUsers);
                if (update) {
                    extras.putBoolean("android.intent.extra.REPLACING", true);
                }
                sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, updateUsers);
                if (update) {
                    sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", packageName, extras, 0, null, null, updateUsers);
                    sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, packageName, null, updateUsers);
                } else if (launchedForRestore && !isSystemApp(res.pkg)) {
                    if (DEBUG_BACKUP) {
                        Slog.i(TAG, "Post-restore of " + packageName + " sending FIRST_LAUNCH in " + Arrays.toString(firstUsers));
                    }
                    sendFirstLaunchBroadcast(packageName, installerPackage, firstUsers);
                }
                if (res.pkg.isForwardLocked() || isExternal(res.pkg)) {
                    if (DEBUG_INSTALL) {
                        Slog.i(TAG, "upgrading pkg " + res.pkg + " is ASEC-hosted -> AVAILABLE");
                    }
                    int[] uidArray = {res.pkg.applicationInfo.uid};
                    ArrayList<String> pkgList = new ArrayList<>(1);
                    pkgList.add(packageName);
                    sendResourcesChangedBroadcast(true, true, pkgList, uidArray, (IIntentReceiver) null);
                }
            }
            if (firstUsers != null && firstUsers.length > 0) {
                synchronized (this.mPackages) {
                    for (int userId : firstUsers) {
                        if (packageIsBrowser(packageName, userId)) {
                            this.mSettings.setDefaultBrowserPackageNameLPw(null, userId);
                        }
                        this.mSettings.applyPendingPermissionGrantsLPw(packageName, userId);
                    }
                }
            }
            EventLog.writeEvent(EventLogTags.UNKNOWN_SOURCES_ENABLED, getUnknownSourcesSettings());
            Runtime.getRuntime().gc();
            if (res.removedInfo != null && res.removedInfo.args != null) {
                synchronized (this.mInstallLock) {
                    res.removedInfo.args.doPostDeleteLI(true);
                }
            }
        }
        if (installObserver == null) {
            return;
        }
        try {
            installObserver.onPackageInstalled(res.name, res.returnCode, res.returnMsg, extrasForInstallResult(res));
        } catch (RemoteException e) {
            Slog.i(TAG, "Observer no longer exists.");
        }
    }

    private void grantRuntimePermissionsGrantedToDisabledPrivSysPackageParentLPw(PackageParser.Package pkg) {
        PackageSetting disabledSysParentPs;
        if (pkg.parentPackage != null && pkg.requestedPermissions != null && (disabledSysParentPs = this.mSettings.getDisabledSystemPkgLPr(pkg.parentPackage.packageName)) != null && disabledSysParentPs.pkg != null && disabledSysParentPs.isPrivileged()) {
            if (disabledSysParentPs.childPackageNames != null && !disabledSysParentPs.childPackageNames.isEmpty()) {
                return;
            }
            int[] allUserIds = sUserManager.getUserIds();
            int permCount = pkg.requestedPermissions.size();
            for (int i = 0; i < permCount; i++) {
                String permission = (String) pkg.requestedPermissions.get(i);
                BasePermission bp = this.mSettings.mPermissions.get(permission);
                if (bp != null && (bp.isRuntime() || bp.isDevelopment())) {
                    for (int userId : allUserIds) {
                        if (disabledSysParentPs.getPermissionsState().hasRuntimePermission(permission, userId)) {
                            grantRuntimePermission(pkg.packageName, permission, userId);
                        }
                    }
                }
            }
        }
    }

    private void grantRequestedRuntimePermissions(PackageParser.Package pkg, int[] userIds, String[] grantedPermissions) {
        for (int userId : userIds) {
            grantRequestedRuntimePermissionsForUser(pkg, userId, grantedPermissions);
        }
        synchronized (this.mPackages) {
            this.mSettings.writePackageListLPr();
        }
    }

    private void grantRequestedRuntimePermissionsForUser(PackageParser.Package pkg, int userId, String[] grantedPermissions) {
        BasePermission bp;
        SettingBase sb = (SettingBase) pkg.mExtras;
        if (sb == null) {
            return;
        }
        PermissionsState permissionsState = sb.getPermissionsState();
        for (String permission : pkg.requestedPermissions) {
            synchronized (this.mPackages) {
                bp = this.mSettings.mPermissions.get(permission);
            }
            if (bp != null && (bp.isRuntime() || bp.isDevelopment())) {
                if (grantedPermissions == null || ArrayUtils.contains(grantedPermissions, permission)) {
                    int flags = permissionsState.getPermissionFlags(permission, userId);
                    if ((flags & 20) == 0) {
                        grantRuntimePermission(pkg.packageName, permission, userId);
                    }
                }
            }
        }
    }

    Bundle extrasForInstallResult(PackageInstalledInfo res) {
        boolean z = false;
        switch (res.returnCode) {
            case -112:
                Bundle extras = new Bundle();
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION", res.origPermission);
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE", res.origPackage);
                return extras;
            case 1:
                Bundle extras2 = new Bundle();
                if (res.removedInfo != null && res.removedInfo.removedPackage != null) {
                    z = true;
                }
                extras2.putBoolean("android.intent.extra.REPLACING", z);
                return extras2;
            default:
                return null;
        }
    }

    void scheduleWriteSettingsLocked() {
        if (this.mHandler.hasMessages(13)) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(13, 10000L);
    }

    void scheduleWritePackageListLocked(int userId) {
        if (this.mHandler.hasMessages(19)) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(19);
        msg.arg1 = userId;
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    void scheduleWritePackageRestrictionsLocked(UserHandle user) {
        int userId = user == null ? -1 : user.getIdentifier();
        scheduleWritePackageRestrictionsLocked(userId);
    }

    void scheduleWritePackageRestrictionsLocked(int userId) {
        int[] userIds = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
        for (int nextUserId : userIds) {
            if (!sUserManager.exists(nextUserId)) {
                return;
            }
            this.mDirtyUsers.add(Integer.valueOf(nextUserId));
            if (!this.mHandler.hasMessages(14)) {
                this.mHandler.sendEmptyMessageDelayed(14, 10000L);
            }
        }
    }

    public static PackageManagerService main(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        PackageManagerServiceCompilerMapping.checkProperties();
        IPackageManager packageManagerService = new PackageManagerService(context, installer, factoryTest, onlyCore);
        packageManagerService.enableSystemUserPackages();
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(context.getOpPackageName(), packageManagerService, 0);
        ServiceManager.addService("package", packageManagerService);
        return packageManagerService;
    }

    private void enableSystemUserPackages() {
        boolean install;
        if (UserManager.isSplitSystemUser()) {
            AppsQueryHelper queryHelper = new AppsQueryHelper(this);
            Set<String> enableApps = new ArraySet<>();
            enableApps.addAll(queryHelper.queryApps(AppsQueryHelper.GET_NON_LAUNCHABLE_APPS | AppsQueryHelper.GET_APPS_WITH_INTERACT_ACROSS_USERS_PERM | AppsQueryHelper.GET_IMES, true, UserHandle.SYSTEM));
            ArraySet<String> wlApps = SystemConfig.getInstance().getSystemUserWhitelistedApps();
            enableApps.addAll(wlApps);
            enableApps.addAll(queryHelper.queryApps(AppsQueryHelper.GET_REQUIRED_FOR_SYSTEM_USER, false, UserHandle.SYSTEM));
            ArraySet<String> blApps = SystemConfig.getInstance().getSystemUserBlacklistedApps();
            enableApps.removeAll(blApps);
            Log.i(TAG, "Applications installed for system user: " + enableApps);
            List<String> allAps = queryHelper.queryApps(0, false, UserHandle.SYSTEM);
            int allAppsSize = allAps.size();
            synchronized (this.mPackages) {
                for (int i = 0; i < allAppsSize; i++) {
                    String pName = allAps.get(i);
                    PackageSetting pkgSetting = this.mSettings.mPackages.get(pName);
                    if (pkgSetting != null && pkgSetting.getInstalled(0) != (install = enableApps.contains(pName))) {
                        Log.i(TAG, (install ? "Installing " : "Uninstalling ") + pName + " for system user");
                        pkgSetting.setInstalled(install, 0);
                    }
                }
            }
        }
    }

    private void addBootEvent(String bootevent) {
        if (this.mMTPROFDisable) {
            return;
        }
        try {
            FileOutputStream fbp = new FileOutputStream("/proc/bootprof");
            fbp.write(bootevent.getBytes());
            fbp.flush();
            fbp.close();
        } catch (FileNotFoundException e) {
            Slog.e("BOOTPROF", "Failure open /proc/bootprof, not found!", e);
            this.mMTPROFDisable = true;
        } catch (IOException e2) {
            Slog.e("BOOTPROF", "Failure open /proc/bootprof entry", e2);
            this.mMTPROFDisable = true;
        }
    }

    private static void getDefaultDisplayMetrics(Context context, DisplayMetrics metrics) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        displayManager.getDisplay(0).getMetrics(metrics);
    }

    public PackageManagerService(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        int reparseFlags;
        String msg;
        this.mDeferProtectedFilters = true;
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START, SystemClock.uptimeMillis());
        this.mMTPROFDisable = false;
        addBootEvent("Android:PackageManagerService_Start");
        if (this.mSdkVersion <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }
        this.mContext = context;
        this.mFactoryTest = factoryTest;
        this.mOnlyCore = onlyCore;
        this.mMetrics = new DisplayMetrics();
        this.mSettings = new Settings(this.mPackages);
        this.mSettings.addSharedUserLPw("android.uid.system", 1000, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.phone", 1001, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.log", LOG_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.nfc", NFC_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.bluetooth", 1002, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.shell", SHELL_UID, 1, 8);
        String separateProcesses = SystemProperties.get("debug.separate_processes");
        if (separateProcesses == null || separateProcesses.length() <= 0) {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = null;
        } else if ("*".equals(separateProcesses)) {
            this.mDefParseFlags = 8;
            this.mSeparateProcesses = null;
            Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
        } else {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = separateProcesses.split(",");
            Slog.w(TAG, "Running with debug.separate_processes: " + separateProcesses);
        }
        this.mInstaller = installer;
        this.mPackageDexOptimizer = new PackageDexOptimizer(installer, this.mInstallLock, context, "*dexopt*");
        this.mMoveCallbacks = new MoveCallbacks(FgThread.get().getLooper());
        this.mOnPermissionChangeListeners = new OnPermissionChangeListeners(FgThread.get().getLooper());
        getDefaultDisplayMetrics(context, this.mMetrics);
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mGlobalGids = systemConfig.getGlobalGids();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mAvailableFeatures = systemConfig.getAvailableFeatures();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                this.mHandlerThread = new ServiceThread(TAG, 10, true);
                this.mHandlerThread.start();
                this.mHandler = new PackageHandler(this.mHandlerThread.getLooper());
                this.mProcessLoggingHandler = new ProcessLoggingHandler();
                Watchdog.getInstance().addThread(this.mHandler, 600000L);
                File dataDir = Environment.getDataDirectory();
                this.mAppInstallDir = new File(dataDir, "app");
                this.mAppLib32InstallDir = new File(dataDir, "app-lib");
                this.mEphemeralInstallDir = new File(dataDir, "app-ephemeral");
                this.mAsecInternalPath = new File(dataDir, "app-asec").getPath();
                this.mDrmAppPrivateInstallDir = new File(dataDir, "app-private");
                sUserManager = new UserManagerService(context, this, this.mPackages);
                ArrayMap<String, SystemConfig.PermissionEntry> permConfig = systemConfig.getPermissions();
                for (int i = 0; i < permConfig.size(); i++) {
                    SystemConfig.PermissionEntry perm = permConfig.valueAt(i);
                    BasePermission bp = this.mSettings.mPermissions.get(perm.name);
                    if (bp == null) {
                        bp = new BasePermission(perm.name, PLATFORM_PACKAGE_NAME, 1);
                        this.mSettings.mPermissions.put(perm.name, bp);
                    }
                    if (perm.gids != null) {
                        bp.setGids(perm.gids, perm.perUser);
                    }
                }
                ArrayMap<String, String> libConfig = systemConfig.getSharedLibraries();
                for (int i2 = 0; i2 < libConfig.size(); i2++) {
                    this.mSharedLibraries.put(libConfig.keyAt(i2), new SharedLibraryEntry(libConfig.valueAt(i2), null));
                }
                this.mFoundPolicyFile = SELinuxMMAC.readInstallPolicy();
                this.mRestoredSettings = this.mSettings.readLPw(sUserManager.getUsers(false));
                String customResolverActivity = Resources.getSystem().getString(R.string.NetworkPreferenceSwitchSummary);
                if (!TextUtils.isEmpty(customResolverActivity)) {
                    this.mCustomResolverComponentName = ComponentName.unflattenFromString(customResolverActivity);
                }
                long startTime = SystemClock.uptimeMillis();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, startTime);
                if (CtaUtils.isCtaSupported()) {
                    this.mCtaPermsController = new CtaPermsController(this.mContext);
                    LocalServices.addService(CtaPackageManagerInternal.class, new CtaPackageManagerInternalImpl(this.mCtaPermsController));
                }
                addBootEvent("Android:PMS_scan_START");
                String bootClassPath = System.getenv("BOOTCLASSPATH");
                String systemServerClassPath = System.getenv("SYSTEMSERVERCLASSPATH");
                if (bootClassPath == null) {
                    Slog.w(TAG, "No BOOTCLASSPATH found!");
                }
                if (systemServerClassPath == null) {
                    Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
                }
                List<String> allInstructionSets = InstructionSets.getAllInstructionSets();
                String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets((String[]) allInstructionSets.toArray(new String[allInstructionSets.size()]));
                if (this.mSharedLibraries.size() > 0) {
                    for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                        for (SharedLibraryEntry libEntry : this.mSharedLibraries.values()) {
                            String lib = libEntry.path;
                            if (lib != null) {
                                try {
                                    try {
                                        int dexoptNeeded = DexFile.getDexOptNeeded(lib, dexCodeInstructionSet, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(6), false);
                                        if (dexoptNeeded != 0) {
                                            this.mInstaller.dexopt(lib, 1000, dexCodeInstructionSet, dexoptNeeded, 2, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(6), StorageManager.UUID_PRIVATE_INTERNAL, SKIP_SHARED_LIBRARY_CHECK);
                                        }
                                    } catch (IOException | InstallerConnection.InstallerException e) {
                                        Slog.w(TAG, "Cannot dexopt " + lib + "; is it an APK or JAR? " + e.getMessage());
                                    }
                                } catch (FileNotFoundException e2) {
                                    Slog.w(TAG, "Library not found: " + lib);
                                }
                            }
                        }
                    }
                }
                File frameworkDir = new File(Environment.getRootDirectory(), "framework");
                Settings.VersionInfo ver = this.mSettings.getInternalVersion();
                this.mIsUpgrade = !Build.FINGERPRINT.equals(ver.fingerprint);
                this.mPromoteSystemApps = this.mIsUpgrade && ver.sdkVersion <= 22;
                this.mIsPreNUpgrade = this.mIsUpgrade && ver.sdkVersion < 24;
                if (this.mPromoteSystemApps) {
                    for (PackageSetting ps : this.mSettings.mPackages.values()) {
                        if (isSystemApp(ps)) {
                            this.mExistingSystemPackages.add(ps.name);
                        }
                    }
                }
                File vendorOverlayDir = new File(VENDOR_OVERLAY_DIR);
                scanDirTracedLI(vendorOverlayDir, this.mDefParseFlags | 1 | 64 | 512, 17312, 0L);
                if (!SystemProperties.get("ro.mtk_carrierexpress_pack").equals("")) {
                    String overlayOptr = SystemProperties.get("persist.operator.optr");
                    if (overlayOptr.length() > 0) {
                        File operatorOverlayDir = new File("/custom/overlay", overlayOptr);
                        scanDirTracedLI(operatorOverlayDir, this.mDefParseFlags | 1 | 64 | 512, 17312, 0L);
                    }
                }
                File customFrameworkDir = new File("/custom/framework");
                scanDirLI(customFrameworkDir, 65, 16802, 0L);
                scanDirTracedLI(frameworkDir, this.mDefParseFlags | 1 | 64 | 128, 16802, 0L);
                File vendorFrameworkDir = new File(Environment.getVendorDirectory(), "framework");
                scanDirTracedLI(vendorFrameworkDir, 65, 16802, 0L);
                File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
                scanDirTracedLI(privilegedAppDir, this.mDefParseFlags | 1 | 64 | 128, 16800, 0L);
                File systemAppDir = new File(Environment.getRootDirectory(), "app");
                scanDirTracedLI(systemAppDir, this.mDefParseFlags | 1 | 64, 16800, 0L);
                File privilegedVendorAppDir = new File(Environment.getVendorDirectory(), "priv-app");
                scanDirLI(privilegedVendorAppDir, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS, 16800, 0L);
                new ResmonFilter();
                File vendorAppDir = new File(Environment.getVendorDirectory(), "app");
                try {
                    vendorAppDir = vendorAppDir.getCanonicalFile();
                } catch (IOException e3) {
                }
                scanDirTracedLI(vendorAppDir, this.mDefParseFlags | 1 | 64, 16800, 0L);
                this.mOperatorAppInstallDir = new File(Environment.getVendorDirectory(), "/operator/app");
                scanDirLI(this.mOperatorAppInstallDir, 8192, 16800, 0L);
                this.mPluginAppInstallDir = new File(Environment.getRootDirectory(), "plugin");
                scanDirLI(this.mPluginAppInstallDir, 8257, 16800, 0L);
                File vendorPluginDir = new File(Environment.getVendorDirectory(), "plugin");
                scanDirLI(vendorPluginDir, 8257, 16800, 0L);
                boolean carrierExpressInstEnabled = "1".equals(SystemProperties.get("ro.mtk_carrierexpress_inst_sup"));
                if (carrierExpressInstEnabled) {
                    String opStr = SystemProperties.get("persist.operator.optr");
                    if (opStr == null || opStr.length() <= 0) {
                        Slog.d(TAG, "No operater defined.");
                    } else {
                        String opFileName = "usp-apks-path-" + opStr + ".txt";
                        File customUniDir = new File("/custom/usp");
                        if (customUniDir != null) {
                            scanCxpApp(customUniDir, opFileName, 16800);
                        } else {
                            File systemUniDir = new File("/system/usp");
                            if (systemUniDir != null) {
                                scanCxpApp(systemUniDir, opFileName, 16800);
                            } else {
                                Slog.d(TAG, "No Carrier Express Pack directory.");
                            }
                        }
                    }
                } else {
                    File customAppInstallDir = new File("/custom/app");
                    File customPluginInstallDir = new File("/custom/plugin");
                    scanDirLI(customAppInstallDir, 65, 16800, 0L);
                    scanDirLI(customPluginInstallDir, 65, 16800, 0L);
                }
                File oemAppDir = new File(Environment.getOemDirectory(), "app");
                scanDirTracedLI(oemAppDir, this.mDefParseFlags | 1 | 64, 16800, 0L);
                List<String> possiblyDeletedUpdatedSystemApps = new ArrayList<>();
                if (!this.mOnlyCore) {
                    Iterator<PackageSetting> psit = this.mSettings.mPackages.values().iterator();
                    while (psit.hasNext()) {
                        PackageSetting ps2 = psit.next();
                        if ((ps2.pkgFlags & 1) != 0 || isVendorApp(ps2) || locationIsOperator(ps2.codePath)) {
                            PackageParser.Package scannedPkg = this.mPackages.get(ps2.name);
                            if (scannedPkg != null) {
                                if (this.mSettings.isDisabledSystemPackageLPr(ps2.name)) {
                                    logCriticalInfo(5, "Expecting better updated system app for " + ps2.name + "; removing system app.  Last known codePath=" + ps2.codePathString + ", installStatus=" + ps2.installStatus + ", versionCode=" + ps2.versionCode + "; scanned versionCode=" + scannedPkg.mVersionCode);
                                    removePackageLI(scannedPkg, true);
                                    this.mExpectingBetter.put(ps2.name, ps2.codePath);
                                }
                            } else if (this.mSettings.isDisabledSystemPackageLPr(ps2.name)) {
                                PackageSetting disabledPs = this.mSettings.getDisabledSystemPkgLPr(ps2.name);
                                if (disabledPs.codePath == null || !disabledPs.codePath.exists()) {
                                    possiblyDeletedUpdatedSystemApps.add(ps2.name);
                                }
                            } else {
                                psit.remove();
                                logCriticalInfo(5, "System package " + ps2.name + " no longer exists; it's data will be wiped");
                            }
                        }
                    }
                }
                ArrayList<PackageSetting> deletePkgsList = this.mSettings.getListOfIncompleteInstallPackagesLPr();
                for (int i3 = 0; i3 < deletePkgsList.size(); i3++) {
                    String packageName = deletePkgsList.get(i3).name;
                    logCriticalInfo(5, "Cleaning up incompletely installed app: " + packageName);
                    synchronized (this.mPackages) {
                        this.mSettings.removePackageLPw(packageName);
                    }
                }
                deleteTempPackageFiles();
                this.mSettings.pruneSharedUsersLPw();
                if (!this.mOnlyCore) {
                    EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START, SystemClock.uptimeMillis());
                    scanDirTracedLI(this.mAppInstallDir, 0, 20896, 0L);
                    scanDirTracedLI(this.mDrmAppPrivateInstallDir, this.mDefParseFlags | 16, 20896, 0L);
                    scanDirLI(this.mEphemeralInstallDir, this.mDefParseFlags | 2048, 20896, 0L);
                    for (String deletedAppName : possiblyDeletedUpdatedSystemApps) {
                        PackageParser.Package deletedPkg = this.mPackages.get(deletedAppName);
                        this.mSettings.removeDisabledSystemPackageLPw(deletedAppName);
                        if (deletedPkg == null) {
                            msg = "Updated system package " + deletedAppName + " no longer exists; it's data will be wiped";
                        } else {
                            msg = "Updated system app + " + deletedAppName + " no longer present; removing system privileges for " + deletedAppName;
                            deletedPkg.applicationInfo.flags &= -2;
                            deletedPkg.applicationInfo.flagsEx &= -2;
                            PackageSetting deletedPs = this.mSettings.mPackages.get(deletedAppName);
                            deletedPs.pkgFlags &= -2;
                            deletedPs.pkgFlagsEx &= -2;
                        }
                        logCriticalInfo(5, msg);
                    }
                    for (int i4 = 0; i4 < this.mExpectingBetter.size(); i4++) {
                        String packageName2 = this.mExpectingBetter.keyAt(i4);
                        if (!this.mPackages.containsKey(packageName2)) {
                            File scanFile = this.mExpectingBetter.valueAt(i4);
                            logCriticalInfo(5, "Expected better " + packageName2 + " but never showed up; reverting to system");
                            int i5 = this.mDefParseFlags;
                            if (FileUtils.contains(privilegedAppDir, scanFile)) {
                                reparseFlags = HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS;
                            } else if (FileUtils.contains(systemAppDir, scanFile) || FileUtils.contains(vendorAppDir, scanFile) || FileUtils.contains(oemAppDir, scanFile)) {
                                reparseFlags = 65;
                            } else {
                                Slog.e(TAG, "Ignoring unexpected fallback path " + scanFile);
                            }
                            this.mSettings.enableSystemPackageLPw(packageName2);
                            try {
                                scanPackageTracedLI(scanFile, reparseFlags, 16800, 0L, (UserHandle) null);
                            } catch (PackageManagerException e4) {
                                Slog.e(TAG, "Failed to parse original system package: " + e4.getMessage());
                            }
                        }
                    }
                }
                this.mExpectingBetter.clear();
                this.mSetupWizardPackage = getSetupWizardPackageName();
                if (this.mProtectedFilters.size() > 0) {
                    if (DEBUG_FILTERS && this.mSetupWizardPackage == null) {
                        Slog.i(TAG, "No setup wizard; All protected intents capped to priority 0");
                    }
                    for (PackageParser.ActivityIntentInfo filter : this.mProtectedFilters) {
                        if (!filter.activity.info.packageName.equals(this.mSetupWizardPackage)) {
                            Slog.w(TAG, "Protected action; cap priority to 0; package: " + filter.activity.info.packageName + " activity: " + filter.activity.className + " origPrio: " + filter.getPriority());
                            filter.setPriority(0);
                        } else if (DEBUG_FILTERS) {
                            Slog.i(TAG, "Found setup wizard; allow priority " + filter.getPriority() + "; package: " + filter.activity.info.packageName + " activity: " + filter.activity.className + " priority: " + filter.getPriority());
                        }
                    }
                }
                this.mDeferProtectedFilters = false;
                this.mProtectedFilters.clear();
                updateAllSharedLibrariesLPw();
                for (SharedUserSetting setting : this.mSettings.getAllSharedUsersLPw()) {
                    adjustCpuAbisForSharedUserLPw(setting.packages, null, false);
                }
                this.mPackageUsage.readLP();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END, SystemClock.uptimeMillis());
                addBootEvent("Android:PMS_scan_END");
                Slog.i(TAG, "Time to scan packages: " + ((SystemClock.uptimeMillis() - startTime) / 1000.0f) + " seconds");
                this.mForcePermReviewPkgs = new HashSet<>(Arrays.asList(this.mContext.getResources().getStringArray(com.mediatek.internal.R.array.force_review_pkgs)));
                Iterator pkg$iterator = this.mForcePermReviewPkgs.iterator();
                while (pkg$iterator.hasNext()) {
                    Slog.d(TAG, "mForcePermReviewPkgs pkg = " + ((String) pkg$iterator.next()));
                }
                this.mBlockPermReviewPkgs = new HashSet<>(Arrays.asList(this.mContext.getResources().getStringArray(com.mediatek.internal.R.array.block_review_pkgs)));
                Iterator pkg$iterator2 = this.mBlockPermReviewPkgs.iterator();
                while (pkg$iterator2.hasNext()) {
                    Slog.d(TAG, "mBlockPermReviewPkgs pkg = " + ((String) pkg$iterator2.next()));
                }
                int updateFlags = 1;
                if (ver.sdkVersion != this.mSdkVersion) {
                    Slog.i(TAG, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for internal storage");
                    updateFlags = 7;
                }
                updatePermissionsLPw(null, null, StorageManager.UUID_PRIVATE_INTERNAL, updateFlags);
                ver.sdkVersion = this.mSdkVersion;
                if (!onlyCore && (this.mPromoteSystemApps || !this.mRestoredSettings)) {
                    for (UserInfo user : sUserManager.getUsers(true)) {
                        this.mSettings.applyDefaultPreferredAppsLPw(this, user.id);
                        applyFactoryDefaultBrowserLPw(user.id);
                        primeDomainVerificationsLPw(user.id);
                    }
                }
                int storageFlags = StorageManager.isFileEncryptedNativeOrEmulated() ? 1 : 3;
                reconcileAppsDataLI(StorageManager.UUID_PRIVATE_INTERNAL, 0, storageFlags);
                if (this.mIsUpgrade && !onlyCore) {
                    Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                    for (int i6 = 0; i6 < this.mSettings.mPackages.size(); i6++) {
                        PackageSetting ps3 = this.mSettings.mPackages.valueAt(i6);
                        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps3.volumeUuid)) {
                            clearAppDataLIF(ps3.pkg, -1, 515);
                        }
                    }
                    ver.fingerprint = Build.FINGERPRINT;
                }
                checkDefaultBrowser();
                this.mExistingSystemPackages.clear();
                this.mPromoteSystemApps = false;
                ver.databaseVersion = 3;
                this.mSettings.writeLPr();
                if ((isFirstBoot() || isUpgrade() || VMRuntime.didPruneDalvikCache()) && !onlyCore) {
                    long start = System.nanoTime();
                    List<PackageParser.Package> coreApps = new ArrayList<>();
                    for (PackageParser.Package pkg : this.mPackages.values()) {
                        if (pkg.coreApp) {
                            coreApps.add(pkg);
                        }
                    }
                    int[] stats = performDexOpt(coreApps, false, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(8));
                    int elapsedTimeSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
                    MetricsLogger.histogram(this.mContext, "opt_coreapps_time_s", elapsedTimeSeconds);
                    if (DEBUG_DEXOPT) {
                        Slog.i(TAG, "Dex-opt core apps took : " + elapsedTimeSeconds + " seconds (" + stats[0] + ", " + stats[1] + ", " + stats[2] + ")");
                    }
                }
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY, SystemClock.uptimeMillis());
                addBootEvent("Android:PMS_READY");
                if (this.mOnlyCore) {
                    this.mRequiredVerifierPackage = null;
                    this.mRequiredInstallerPackage = null;
                    this.mIntentFilterVerifierComponent = null;
                    this.mIntentFilterVerifier = null;
                    this.mServicesSystemSharedLibraryPackageName = null;
                    this.mSharedSystemSharedLibraryPackageName = null;
                } else {
                    this.mRequiredVerifierPackage = getRequiredButNotReallyRequiredVerifierLPr();
                    this.mRequiredInstallerPackage = getRequiredInstallerLPr();
                    this.mIntentFilterVerifierComponent = getIntentFilterVerifierComponentNameLPr();
                    this.mIntentFilterVerifier = new IntentVerifierProxy(this.mContext, this.mIntentFilterVerifierComponent);
                    this.mServicesSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.services");
                    this.mSharedSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.shared");
                }
                this.mInstallerService = new PackageInstallerService(context, this);
                ComponentName ephemeralResolverComponent = getEphemeralResolverLPr();
                ComponentName ephemeralInstallerComponent = getEphemeralInstallerLPr();
                if (ephemeralInstallerComponent == null || ephemeralResolverComponent == null) {
                    if (DEBUG_EPHEMERAL) {
                        String missingComponent = ephemeralResolverComponent == null ? ephemeralInstallerComponent == null ? "resolver and installer" : "resolver" : "installer";
                        Slog.i(TAG, "Ephemeral deactivated; missing " + missingComponent);
                    }
                    this.mEphemeralResolverComponent = null;
                    this.mEphemeralInstallerComponent = null;
                    this.mEphemeralResolverConnection = null;
                } else {
                    if (DEBUG_EPHEMERAL) {
                        Slog.i(TAG, "Ephemeral activated; resolver: " + ephemeralResolverComponent + " installer:" + ephemeralInstallerComponent);
                    }
                    this.mEphemeralResolverComponent = ephemeralResolverComponent;
                    this.mEphemeralInstallerComponent = ephemeralInstallerComponent;
                    setUpEphemeralInstallerActivityLP(this.mEphemeralInstallerComponent);
                    this.mEphemeralResolverConnection = new EphemeralResolverConnection(this.mContext, this.mEphemeralResolverComponent);
                }
                this.mEphemeralApplicationRegistry = new EphemeralApplicationRegistry(this);
            }
        }
        Runtime.getRuntime().gc();
        this.mInstaller.setWarnIfHeld(this.mPackages);
        LocalServices.addService(PackageManagerInternal.class, new PackageManagerInternalImpl(this, null));
    }

    public boolean isFirstBoot() {
        return !this.mRestoredSettings;
    }

    public boolean isOnlyCoreApps() {
        return this.mOnlyCore;
    }

    public boolean isUpgrade() {
        return this.mIsUpgrade;
    }

    private String getRequiredButNotReallyRequiredVerifierLPr() {
        Intent intent = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        List<ResolveInfo> matches = queryIntentReceiversInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        Log.e(TAG, "There should probably be exactly one verifier; found " + matches);
        return null;
    }

    private String getRequiredSharedLibraryLPr(String libraryName) {
        String str;
        synchronized (this.mPackages) {
            SharedLibraryEntry libraryEntry = this.mSharedLibraries.get(libraryName);
            if (libraryEntry == null) {
                throw new IllegalStateException("Missing required shared library:" + libraryName);
            }
            str = libraryEntry.apk;
        }
        return str;
    }

    private String getRequiredInstallerLPr() {
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        if (matches.size() == 1) {
            ResolveInfo resolveInfo = matches.get(0);
            if (!resolveInfo.activityInfo.applicationInfo.isPrivilegedApp()) {
                throw new RuntimeException("The installer must be a privileged app");
            }
            return matches.get(0).getComponentInfo().packageName;
        }
        throw new RuntimeException("There must be exactly one installer; found " + matches);
    }

    private ComponentName getIntentFilterVerifierComponentNameLPr() {
        Intent intent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION");
        List<ResolveInfo> matches = queryIntentReceiversInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        ResolveInfo best = null;
        int N = matches.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo cur = matches.get(i);
            String packageName = cur.getComponentInfo().packageName;
            if (checkPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", packageName, 0) == 0 && (best == null || cur.priority > best.priority)) {
                best = cur;
            }
        }
        if (best != null) {
            return best.getComponentInfo().getComponentName();
        }
        throw new RuntimeException("There must be at least one intent filter verifier");
    }

    private ComponentName getEphemeralResolverLPr() {
        String[] packageArray = this.mContext.getResources().getStringArray(R.array.config_callBarringMMI);
        if (packageArray.length == 0) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "Ephemeral resolver NOT found; empty package list");
            }
            return null;
        }
        Intent resolverIntent = new Intent("android.intent.action.RESOLVE_EPHEMERAL_PACKAGE");
        List<ResolveInfo> resolvers = queryIntentServicesInternal(resolverIntent, null, 1835008, 0);
        int N = resolvers.size();
        if (N == 0) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "Ephemeral resolver NOT found; no matching intent filters");
            }
            return null;
        }
        Set<String> possiblePackages = new ArraySet<>(Arrays.asList(packageArray));
        for (int i = 0; i < N; i++) {
            ResolveInfo info = resolvers.get(i);
            if (info.serviceInfo != null) {
                String packageName = info.serviceInfo.packageName;
                if (!possiblePackages.contains(packageName)) {
                    if (DEBUG_EPHEMERAL) {
                        Slog.d(TAG, "Ephemeral resolver not in allowed package list; pkg: " + packageName + ", info:" + info);
                    }
                } else {
                    if (DEBUG_EPHEMERAL) {
                        Slog.v(TAG, "Ephemeral resolver found; pkg: " + packageName + ", info:" + info);
                    }
                    return new ComponentName(packageName, info.serviceInfo.name);
                }
            }
        }
        if (DEBUG_EPHEMERAL) {
            Slog.v(TAG, "Ephemeral resolver NOT found");
        }
        return null;
    }

    private ComponentName getEphemeralInstallerLPr() {
        Intent intent = new Intent("android.intent.action.INSTALL_EPHEMERAL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        if (matches.size() == 0) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().getComponentName();
        }
        throw new RuntimeException("There must be at most one ephemeral installer; found " + matches);
    }

    private void primeDomainVerificationsLPw(int userId) {
        if (DEBUG_DOMAIN_VERIFICATION) {
            Slog.d(TAG, "Priming domain verifications in user " + userId);
        }
        SystemConfig systemConfig = SystemConfig.getInstance();
        ArraySet<String> packages = systemConfig.getLinkedApps();
        ArraySet<String> domains = new ArraySet<>();
        for (String packageName : packages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg != null) {
                if (!pkg.isSystemApp()) {
                    Slog.w(TAG, "Non-system app '" + packageName + "' in sysconfig <app-link>");
                } else {
                    domains.clear();
                    for (PackageParser.Activity a : pkg.activities) {
                        for (PackageParser.ActivityIntentInfo filter : a.intents) {
                            if (hasValidDomains(filter)) {
                                domains.addAll(filter.getHostsList());
                            }
                        }
                    }
                    if (domains.size() > 0) {
                        if (DEBUG_DOMAIN_VERIFICATION) {
                            Slog.v(TAG, "      + " + packageName);
                        }
                        IntentFilterVerificationInfo ivi = this.mSettings.createIntentFilterVerificationIfNeededLPw(packageName, new ArrayList<>(domains));
                        ivi.setStatus(0);
                        this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, 2, userId);
                    } else {
                        Slog.w(TAG, "Sysconfig <app-link> package '" + packageName + "' does not handle web links");
                    }
                }
            } else {
                Slog.w(TAG, "Unknown package " + packageName + " in sysconfig <app-link>");
            }
        }
        scheduleWritePackageRestrictionsLocked(userId);
        scheduleWriteSettingsLocked();
    }

    private void applyFactoryDefaultBrowserLPw(int userId) {
        String browserPkg = this.mContext.getResources().getString(R.string.config_systemCallStreaming);
        if (!TextUtils.isEmpty(browserPkg)) {
            PackageSetting ps = this.mSettings.mPackages.get(browserPkg);
            if (ps == null) {
                Slog.e(TAG, "Product default browser app does not exist: " + browserPkg);
                browserPkg = null;
            } else {
                this.mSettings.setDefaultBrowserPackageNameLPw(browserPkg, userId);
            }
        }
        if (browserPkg != null) {
            return;
        }
        calculateDefaultBrowserLPw(userId);
    }

    private void calculateDefaultBrowserLPw(int userId) {
        List<String> allBrowsers = resolveAllBrowserApps(userId);
        this.mSettings.setDefaultBrowserPackageNameLPw(allBrowsers.size() == 1 ? allBrowsers.get(0) : null, userId);
    }

    private List<String> resolveAllBrowserApps(int userId) {
        List<ResolveInfo> list = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, userId);
        int count = list.size();
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && info.handleAllWebDataURI && (info.activityInfo.applicationInfo.flags & 1) != 0 && !result.contains(info.activityInfo.packageName)) {
                result.add(info.activityInfo.packageName);
            }
        }
        return result;
    }

    private boolean packageIsBrowser(String packageName, int userId) {
        List<ResolveInfo> list = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, userId);
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo info = list.get(i);
            if (packageName.equals(info.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private void checkDefaultBrowser() {
        int myUserId = UserHandle.myUserId();
        String packageName = getDefaultBrowserPackageName(myUserId);
        if (packageName == null) {
            return;
        }
        PackageInfo info = getPackageInfo(packageName, 0, myUserId);
        if (info != null) {
            return;
        }
        Slog.w(TAG, "Default browser no longer installed: " + packageName);
        synchronized (this.mPackages) {
            applyFactoryDefaultBrowserLPw(myUserId);
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException) && !(e instanceof IllegalArgumentException)) {
                Slog.wtf(TAG, "Package Manager Crash", e);
            }
            throw e;
        }
    }

    static int[] appendInts(int[] cur, int[] add) {
        if (add == null) {
            return cur;
        }
        if (cur == null) {
            return add;
        }
        for (int i : add) {
            cur = ArrayUtils.appendInt(cur, i);
        }
        return cur;
    }

    private PackageInfo generatePackageInfo(PackageSetting ps, int flags, int userId) {
        PackageParser.Package p;
        if (!sUserManager.exists(userId) || ps == null || (p = ps.pkg) == null) {
            return null;
        }
        PermissionsState permissionsState = ps.getPermissionsState();
        int[] gids = permissionsState.computeGids(userId);
        Set<String> permissions = permissionsState.getPermissions(userId);
        PackageUserState state = ps.readUserState(userId);
        return PackageParser.generatePackageInfo(p, gids, flags, ps.firstInstallTime, ps.lastUpdateTime, permissions, state, userId);
    }

    public void checkPackageStartable(String packageName, int userId) {
        boolean userKeyUnlocked = StorageManager.isUserKeyUnlocked(userId);
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new SecurityException("Package " + packageName + " was not found!");
            }
            if (!ps.getInstalled(userId)) {
                throw new SecurityException("Package " + packageName + " was not installed for user " + userId + "!");
            }
            if (this.mSafeMode && !ps.isSystem()) {
                throw new SecurityException("Package " + packageName + " not a system app!");
            }
            if (this.mFrozenPackages.contains(packageName)) {
                throw new SecurityException("Package " + packageName + " is currently frozen!");
            }
            if (!userKeyUnlocked && !ps.pkg.applicationInfo.isDirectBootAware() && !ps.pkg.applicationInfo.isPartiallyDirectBootAware()) {
                throw new SecurityException("Package " + packageName + " is not encryption aware!");
            }
        }
    }

    public boolean isPackageAvailable(String packageName, int userId) {
        PackageSetting ps;
        PackageUserState state;
        if (!sUserManager.exists(userId)) {
            return false;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "is package available");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p == null || (ps = (PackageSetting) p.mExtras) == null || (state = ps.readUserState(userId)) == null) {
                return false;
            }
            return PackageParser.isAvailable(state);
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package info");
        synchronized (this.mPackages) {
            boolean matchFactoryOnly = (2097152 & flags2) != 0;
            if (matchFactoryOnly) {
                PackageSetting ps = this.mSettings.getDisabledSystemPkgLPr(packageName);
                if (ps != null) {
                    return generatePackageInfo(ps, flags2, userId);
                }
            }
            PackageParser.Package p = this.mPackages.get(packageName);
            if (matchFactoryOnly && p != null && !isSystemApp(p)) {
                return null;
            }
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getPackageInfo " + packageName + ": " + p);
            }
            if (p != null) {
                return generatePackageInfo((PackageSetting) p.mExtras, flags2, userId);
            }
            if (matchFactoryOnly || (flags2 & 8192) == 0) {
                return null;
            }
            return generatePackageInfo(this.mSettings.mPackages.get(packageName), flags2, userId);
        }
    }

    public String[] currentToCanonicalPackageNames(String[] names) {
        String[] out = new String[names.length];
        synchronized (this.mPackages) {
            for (int i = names.length - 1; i >= 0; i--) {
                PackageSetting ps = this.mSettings.mPackages.get(names[i]);
                out[i] = (ps == null || ps.realName == null) ? names[i] : ps.realName;
            }
        }
        return out;
    }

    public String[] canonicalToCurrentPackageNames(String[] names) {
        String[] out = new String[names.length];
        synchronized (this.mPackages) {
            for (int i = names.length - 1; i >= 0; i--) {
                String cur = this.mSettings.mRenamedPackages.get(names[i]);
                if (cur == null) {
                    cur = names[i];
                }
                out[i] = cur;
            }
        }
        return out;
    }

    public int getPackageUid(String packageName, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId)) {
            return -1;
        }
        int flags2 = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package uid");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null && p.isMatch(flags2)) {
                return UserHandle.getUid(userId, p.applicationInfo.uid);
            }
            if ((flags2 & 8192) == 0 || (ps = this.mSettings.mPackages.get(packageName)) == null || !ps.isMatch(flags2)) {
                return -1;
            }
            return UserHandle.getUid(userId, ps.appId);
        }
    }

    public int[] getPackageGids(String packageName, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "getPackageGids");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null && p.isMatch(flags2)) {
                return ((PackageSetting) p.mExtras).getPermissionsState().computeGids(userId);
            }
            if ((flags2 & 8192) == 0 || (ps = this.mSettings.mPackages.get(packageName)) == null || !ps.isMatch(flags2)) {
                return null;
            }
            return ps.getPermissionsState().computeGids(userId);
        }
    }

    static PermissionInfo generatePermissionInfo(BasePermission bp, int flags) {
        if (bp.perm != null) {
            return PackageParser.generatePermissionInfo(bp.perm, flags);
        }
        PermissionInfo pi = new PermissionInfo();
        pi.name = bp.name;
        pi.packageName = bp.sourcePackage;
        pi.nonLocalizedLabel = bp.name;
        pi.protectionLevel = bp.protectionLevel;
        return pi;
    }

    public PermissionInfo getPermissionInfo(String name, int flags) {
        synchronized (this.mPackages) {
            BasePermission p = this.mSettings.mPermissions.get(name);
            if (p == null) {
                return null;
            }
            return generatePermissionInfo(p, flags);
        }
    }

    public ParceledListSlice<PermissionInfo> queryPermissionsByGroup(String group, int flags) {
        synchronized (this.mPackages) {
            if (group != null) {
                if (!this.mPermissionGroups.containsKey(group)) {
                    return null;
                }
                ArrayList<PermissionInfo> out = new ArrayList<>(10);
                for (BasePermission p : this.mSettings.mPermissions.values()) {
                    if (group == null) {
                        if (p.perm == null || p.perm.info.group == null) {
                            out.add(generatePermissionInfo(p, flags));
                        }
                    } else if (p.perm != null && group.equals(p.perm.info.group)) {
                        out.add(PackageParser.generatePermissionInfo(p.perm, flags));
                    }
                }
                return new ParceledListSlice<>(out);
            }
            ArrayList<PermissionInfo> out2 = new ArrayList<>(10);
            while (p$iterator.hasNext()) {
            }
            return new ParceledListSlice<>(out2);
        }
    }

    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) {
        PermissionGroupInfo permissionGroupInfoGeneratePermissionGroupInfo;
        synchronized (this.mPackages) {
            permissionGroupInfoGeneratePermissionGroupInfo = PackageParser.generatePermissionGroupInfo(this.mPermissionGroups.get(name), flags);
        }
        return permissionGroupInfoGeneratePermissionGroupInfo;
    }

    public ParceledListSlice<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        ParceledListSlice<PermissionGroupInfo> parceledListSlice;
        synchronized (this.mPackages) {
            int N = this.mPermissionGroups.size();
            ArrayList<PermissionGroupInfo> out = new ArrayList<>(N);
            for (PackageParser.PermissionGroup pg : this.mPermissionGroups.values()) {
                out.add(PackageParser.generatePermissionGroupInfo(pg, flags));
            }
            parceledListSlice = new ParceledListSlice<>(out);
        }
        return parceledListSlice;
    }

    private ApplicationInfo generateApplicationInfoFromSettingsLPw(String packageName, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId) || (ps = this.mSettings.mPackages.get(packageName)) == null) {
            return null;
        }
        if (ps.pkg == null) {
            PackageInfo pInfo = generatePackageInfo(ps, flags, userId);
            if (pInfo != null) {
                return pInfo.applicationInfo;
            }
            return null;
        }
        return PackageParser.generateApplicationInfo(ps.pkg, flags, ps.readUserState(userId), userId);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForApplication(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get application info");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getApplicationInfo " + packageName + ": " + p);
            }
            if (p != null) {
                PackageSetting ps = this.mSettings.mPackages.get(packageName);
                if (ps == null) {
                    return null;
                }
                return PackageParser.generateApplicationInfo(p, flags2, ps.readUserState(userId), userId);
            }
            if (PLATFORM_PACKAGE_NAME.equals(packageName) || "system".equals(packageName)) {
                return this.mAndroidApplication;
            }
            if ((flags2 & 8192) == 0) {
                return null;
            }
            return generateApplicationInfoFromSettingsLPw(packageName, flags2, userId);
        }
    }

    public void freeStorageAndNotify(final String volumeUuid, final long freeStorageSize, final IPackageDataObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.mHandler.removeCallbacks(this);
                boolean success = true;
                synchronized (PackageManagerService.this.mInstallLock) {
                    try {
                        PackageManagerService.this.mInstaller.freeCache(volumeUuid, freeStorageSize);
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.w(PackageManagerService.TAG, "Couldn't clear application caches: " + e);
                        success = false;
                    }
                }
                if (observer == null) {
                    return;
                }
                try {
                    observer.onRemoveCompleted((String) null, success);
                } catch (RemoteException e2) {
                    Slog.w(PackageManagerService.TAG, "RemoveException when invoking call back");
                }
            }
        });
    }

    public void freeStorage(final String volumeUuid, final long freeStorageSize, final IntentSender pi) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.mHandler.removeCallbacks(this);
                boolean success = true;
                synchronized (PackageManagerService.this.mInstallLock) {
                    try {
                        PackageManagerService.this.mInstaller.freeCache(volumeUuid, freeStorageSize);
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.w(PackageManagerService.TAG, "Couldn't clear application caches: " + e);
                        success = false;
                    }
                }
                if (pi == null) {
                    return;
                }
                int code = success ? 1 : 0;
                try {
                    pi.sendIntent(null, code, null, null, null);
                } catch (IntentSender.SendIntentException e2) {
                    Slog.i(PackageManagerService.TAG, "Failed to send pending intent");
                }
            }
        });
    }

    void freeStorage(String volumeUuid, long freeStorageSize) throws IOException {
        synchronized (this.mInstallLock) {
            try {
                this.mInstaller.freeCache(volumeUuid, freeStorageSize);
            } catch (InstallerConnection.InstallerException e) {
                throw new IOException("Failed to free enough space", e);
            }
        }
    }

    private int updateFlags(int flags, int userId) {
        if ((flags & 786432) == 0) {
            if (getUserManagerInternal().isUserUnlockingOrUnlocked(userId)) {
                return flags | 786432;
            }
            return flags | DumpState.DUMP_FROZEN;
        }
        return flags;
    }

    private UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    private int updateFlagsForPackage(int flags, int userId, Object cookie) {
        boolean triaged = true;
        if ((flags & 15) != 0 && (269221888 & flags) == 0) {
            triaged = false;
        }
        if ((269492224 & flags) == 0) {
            triaged = false;
        }
        if (DEBUG_TRIAGED_MISSING && Binder.getCallingUid() == 1000 && !triaged) {
            Log.w(TAG, "Caller hasn't been triaged for missing apps; they asked about " + cookie + " with flags 0x" + Integer.toHexString(flags), new Throwable());
        }
        return updateFlags(flags, userId);
    }

    private int updateFlagsForApplication(int flags, int userId, Object cookie) {
        return updateFlagsForPackage(flags, userId, cookie);
    }

    private int updateFlagsForComponent(int flags, int userId, Object cookie) {
        if ((cookie instanceof Intent) && (((Intent) cookie).getFlags() & 256) != 0) {
            flags |= 268435456;
        }
        boolean triaged = (269221888 & flags) != 0;
        if (DEBUG_TRIAGED_MISSING && Binder.getCallingUid() == 1000 && !triaged) {
            Log.w(TAG, "Caller hasn't been triaged for missing apps; they asked about " + cookie + " with flags 0x" + Integer.toHexString(flags), new Throwable());
        }
        return updateFlags(flags, userId);
    }

    int updateFlagsForResolve(int flags, int userId, Object cookie) {
        if (this.mSafeMode) {
            flags |= DumpState.DUMP_DEXOPT;
        }
        return updateFlagsForComponent(flags, userId, cookie);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get activity info");
        synchronized (this.mPackages) {
            PackageParser.Activity a = (PackageParser.Activity) this.mActivities.mActivities.get(component);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getActivityInfo " + component + ": " + a);
            }
            if (a != null && this.mSettings.isEnabledAndMatchLPr(a.info, flags2, userId)) {
                PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
                if (ps == null) {
                    return null;
                }
                return PackageParser.generateActivityInfo(a, flags2, ps.readUserState(userId), userId);
            }
            if (!this.mResolveComponentName.equals(component)) {
                return null;
            }
            return PackageParser.generateActivityInfo(this.mResolveActivity, flags2, new PackageUserState(), userId);
        }
    }

    public boolean activitySupportsIntent(ComponentName component, Intent intent, String resolvedType) {
        synchronized (this.mPackages) {
            if (component.equals(this.mResolveComponentName)) {
                return true;
            }
            PackageParser.Activity a = (PackageParser.Activity) this.mActivities.mActivities.get(component);
            if (a == null) {
                return false;
            }
            for (int i = 0; i < a.intents.size(); i++) {
                if (((PackageParser.ActivityIntentInfo) a.intents.get(i)).match(intent.getAction(), resolvedType, intent.getScheme(), intent.getData(), intent.getCategories(), TAG) >= 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public ActivityInfo getReceiverInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get receiver info");
        synchronized (this.mPackages) {
            PackageParser.Activity a = (PackageParser.Activity) this.mReceivers.mActivities.get(component);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getReceiverInfo " + component + ": " + a);
            }
            if (a == null || !this.mSettings.isEnabledAndMatchLPr(a.info, flags2, userId)) {
                return null;
            }
            PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                return null;
            }
            return PackageParser.generateActivityInfo(a, flags2, ps.readUserState(userId), userId);
        }
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get service info");
        synchronized (this.mPackages) {
            PackageParser.Service s = (PackageParser.Service) this.mServices.mServices.get(component);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getServiceInfo " + component + ": " + s);
            }
            if (s == null || !this.mSettings.isEnabledAndMatchLPr(s.info, flags2, userId)) {
                return null;
            }
            PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                return null;
            }
            return PackageParser.generateServiceInfo(s, flags2, ps.readUserState(userId), userId);
        }
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get provider info");
        synchronized (this.mPackages) {
            PackageParser.Provider p = (PackageParser.Provider) this.mProviders.mProviders.get(component);
            if (DEBUG_PACKAGE_INFO) {
                Log.v(TAG, "getProviderInfo " + component + ": " + p);
            }
            if (p == null || !this.mSettings.isEnabledAndMatchLPr(p.info, flags2, userId)) {
                return null;
            }
            PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                return null;
            }
            return PackageParser.generateProviderInfo(p, flags2, ps.readUserState(userId), userId);
        }
    }

    public String[] getSystemSharedLibraryNames() {
        synchronized (this.mPackages) {
            Set<String> libSet = this.mSharedLibraries.keySet();
            int size = libSet.size();
            if (size > 0) {
                String[] libs = new String[size];
                libSet.toArray(libs);
                return libs;
            }
            return null;
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

    public ParceledListSlice<FeatureInfo> getSystemAvailableFeatures() {
        ParceledListSlice<FeatureInfo> parceledListSlice;
        synchronized (this.mPackages) {
            ArrayList<FeatureInfo> res = new ArrayList<>(this.mAvailableFeatures.values());
            FeatureInfo fi = new FeatureInfo();
            fi.reqGlEsVersion = SystemProperties.getInt("ro.opengles.version", 0);
            res.add(fi);
            parceledListSlice = new ParceledListSlice<>(res);
        }
        return parceledListSlice;
    }

    public boolean hasSystemFeature(String name, int version) {
        synchronized (this.mPackages) {
            FeatureInfo feat = this.mAvailableFeatures.get(name);
            if (feat == null) {
                return false;
            }
            return feat.version >= version;
        }
    }

    public int checkPermission(String permName, String pkgName, int userId) {
        if (!sUserManager.exists(userId)) {
            Slog.w(TAG, "checkPermission() for " + pkgName + " failed, user id does not exist.");
            return -1;
        }
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(pkgName);
            if (p != null && p.mExtras != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                PermissionsState permissionsState = ps.getPermissionsState();
                if (permissionsState.hasPermission(permName, userId)) {
                    return 0;
                }
                if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName)) {
                    if (permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
                        return 0;
                    }
                }
            }
            Slog.w(TAG, "checkPermission(): " + permName + " of " + pkgName + " is denied.");
            return -1;
        }
    }

    public int checkUidPermission(String permName, int uid) {
        int userId = UserHandle.getUserId(uid);
        if (!sUserManager.exists(userId)) {
            Slog.w(TAG, "checkUidPermission() for " + uid + " failed, user id does not exist.");
            return -1;
        }
        if (this.mCtaPermsController != null) {
            this.mCtaPermsController.reportPermRequestUsage(permName, uid);
        }
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj != null) {
                SettingBase ps = (SettingBase) obj;
                PermissionsState permissionsState = ps.getPermissionsState();
                if (permissionsState.hasPermission(permName, userId)) {
                    return 0;
                }
                if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
                    return 0;
                }
            } else {
                ArraySet<String> perms = this.mSystemPermissions.get(uid);
                if (perms != null) {
                    if (perms.contains(permName)) {
                        return 0;
                    }
                    if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && perms.contains("android.permission.ACCESS_FINE_LOCATION")) {
                        return 0;
                    }
                }
            }
            if (DEBUG_PACKAGE_INFO) {
                Slog.w(TAG, "checkUidPermission(): " + permName + " of " + uid + " is denied.");
            }
            return -1;
        }
    }

    public boolean isPermissionRevokedByPolicy(String permission, String packageName, int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "isPermissionRevokedByPolicy for user " + userId);
        }
        if (checkPermission(permission, packageName, userId) == 0) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = getPermissionFlags(permission, packageName, userId);
            return (flags & 4) != 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public String getPermissionControllerPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mRequiredInstallerPackage;
        }
        return str;
    }

    void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            enforceShellRestriction("no_debugging_features", callingUid, userId);
        }
        if (userId == UserHandle.getUserId(callingUid) || callingUid == 1000 || callingUid == 0) {
            return;
        }
        if (requireFullPermission) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
            return;
        }
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", message);
        }
    }

    void enforceShellRestriction(String restriction, int callingUid, int userHandle) {
        if (callingUid != SHELL_UID) {
            return;
        }
        if (userHandle >= 0 && sUserManager.hasUserRestriction(restriction, userHandle)) {
            throw new SecurityException("Shell does not have permission to access user " + userHandle);
        }
        if (userHandle >= 0) {
            return;
        }
        Slog.e(TAG, "Unable to check shell permission for user " + userHandle + "\n\t" + Debug.getCallers(3));
    }

    private BasePermission findPermissionTreeLP(String permName) {
        for (BasePermission bp : this.mSettings.mPermissionTrees.values()) {
            if (permName.startsWith(bp.name) && permName.length() > bp.name.length() && permName.charAt(bp.name.length()) == '.') {
                return bp;
            }
        }
        return null;
    }

    private BasePermission checkPermissionTreeLP(String permName) {
        BasePermission bp;
        if (permName == null || (bp = findPermissionTreeLP(permName)) == null) {
            throw new SecurityException("No permission tree found for " + permName);
        }
        if (bp.uid == UserHandle.getAppId(Binder.getCallingUid())) {
            return bp;
        }
        throw new SecurityException("Calling uid " + Binder.getCallingUid() + " is not allowed to add to permission tree " + bp.name + " owned by uid " + bp.uid);
    }

    static boolean compareStrings(CharSequence s1, CharSequence s2) {
        if (s1 == null) {
            return s2 == null;
        }
        if (s2 != null && s1.getClass() == s2.getClass()) {
            return s1.equals(s2);
        }
        return false;
    }

    static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        return pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && compareStrings(pi1.name, pi2.name) && compareStrings(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && compareStrings(pi1.packageName, pi2.packageName);
    }

    int permissionInfoFootprint(PermissionInfo info) {
        int size = info.name.length();
        if (info.nonLocalizedLabel != null) {
            size += info.nonLocalizedLabel.length();
        }
        return info.nonLocalizedDescription != null ? size + info.nonLocalizedDescription.length() : size;
    }

    int calculateCurrentPermissionFootprintLocked(BasePermission tree) {
        int size = 0;
        for (BasePermission perm : this.mSettings.mPermissions.values()) {
            if (perm.uid == tree.uid) {
                size += perm.name.length() + permissionInfoFootprint(perm.perm.info);
            }
        }
        return size;
    }

    void enforcePermissionCapLocked(PermissionInfo info, BasePermission tree) {
        if (tree.uid == 1000) {
            return;
        }
        int curTreeSize = calculateCurrentPermissionFootprintLocked(tree);
        if (permissionInfoFootprint(info) + curTreeSize <= 32768) {
        } else {
            throw new SecurityException("Permission tree size cap exceeded");
        }
    }

    boolean addPermissionLocked(PermissionInfo info, boolean async) {
        if (info.labelRes == 0 && info.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        }
        BasePermission tree = checkPermissionTreeLP(info.name);
        BasePermission bp = this.mSettings.mPermissions.get(info.name);
        boolean added = bp == null;
        boolean changed = true;
        int fixedLevel = PermissionInfo.fixProtectionLevel(info.protectionLevel);
        if (added) {
            enforcePermissionCapLocked(info, tree);
            bp = new BasePermission(info.name, tree.sourcePackage, 2);
        } else {
            if (bp.type != 2) {
                throw new SecurityException("Not allowed to modify non-dynamic permission " + info.name);
            }
            if (bp.protectionLevel == fixedLevel && bp.perm.owner.equals(tree.perm.owner) && bp.uid == tree.uid && comparePermissionInfos(bp.perm.info, info)) {
                changed = false;
            }
        }
        bp.protectionLevel = fixedLevel;
        PermissionInfo info2 = new PermissionInfo(info);
        info2.protectionLevel = fixedLevel;
        bp.perm = new PackageParser.Permission(tree.perm.owner, info2);
        bp.perm.info.packageName = tree.perm.info.packageName;
        bp.uid = tree.uid;
        if (added) {
            this.mSettings.mPermissions.put(info2.name, bp);
        }
        if (changed) {
            if (!async) {
                this.mSettings.writeLPr();
            } else {
                scheduleWriteSettingsLocked();
            }
        }
        return added;
    }

    public boolean addPermission(PermissionInfo info) {
        boolean zAddPermissionLocked;
        synchronized (this.mPackages) {
            zAddPermissionLocked = addPermissionLocked(info, false);
        }
        return zAddPermissionLocked;
    }

    public boolean addPermissionAsync(PermissionInfo info) {
        boolean zAddPermissionLocked;
        synchronized (this.mPackages) {
            zAddPermissionLocked = addPermissionLocked(info, true);
        }
        return zAddPermissionLocked;
    }

    public void removePermission(String name) {
        synchronized (this.mPackages) {
            checkPermissionTreeLP(name);
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (bp != null) {
                if (bp.type != 2) {
                    throw new SecurityException("Not allowed to modify non-dynamic permission " + name);
                }
                this.mSettings.mPermissions.remove(name);
                this.mSettings.writeLPr();
            }
        }
    }

    private static void enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(PackageParser.Package pkg, BasePermission bp) {
        int index = pkg.requestedPermissions.indexOf(bp.name);
        if (index == -1) {
            throw new SecurityException("Package " + pkg.packageName + " has not requested permission " + bp.name);
        }
        if (bp.isRuntime() || bp.isDevelopment()) {
        } else {
            throw new SecurityException("Permission " + bp.name + " is not a changeable permission type");
        }
    }

    public void grantRuntimePermission(String packageName, String name, final int userId) {
        if (!sUserManager.exists(userId)) {
            Log.e(TAG, "No such user:" + userId);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "grantRuntimePermission");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "grantRuntimePermission");
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (bp == null) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
            enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(pkg, bp);
            if (Build.isPermissionReviewRequired() && pkg.applicationInfo.targetSdkVersion < 23 && bp.isRuntime()) {
                return;
            }
            int uid = UserHandle.getUid(userId, pkg.applicationInfo.uid);
            SettingBase sb = (SettingBase) pkg.mExtras;
            if (sb == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            PermissionsState permissionsState = sb.getPermissionsState();
            int flags = permissionsState.getPermissionFlags(name, userId);
            if ((flags & 16) != 0) {
                throw new SecurityException("Cannot grant system fixed permission " + name + " for package " + packageName);
            }
            if (bp.isDevelopment()) {
                if (permissionsState.grantInstallPermission(bp) != -1) {
                    scheduleWriteSettingsLocked();
                }
                return;
            }
            if (pkg.applicationInfo.targetSdkVersion < 23) {
                Slog.w(TAG, "Cannot grant runtime permission to a legacy app");
                return;
            }
            int result = permissionsState.grantRuntimePermission(bp, userId);
            switch (result) {
                case -1:
                    return;
                case 1:
                    final int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PackageManagerService.this.killUid(appId, userId, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                        }
                    });
                    break;
            }
            this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
            this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
            if (!"android.permission.READ_EXTERNAL_STORAGE".equals(name) && !"android.permission.WRITE_EXTERNAL_STORAGE".equals(name)) {
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                if (sUserManager.isInitialized(userId)) {
                    MountServiceInternal mountServiceInternal = (MountServiceInternal) LocalServices.getService(MountServiceInternal.class);
                    mountServiceInternal.onExternalStoragePolicyChanged(uid, packageName);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void revokeRuntimePermission(String packageName, String name, int userId) {
        if (!sUserManager.exists(userId)) {
            Log.e(TAG, "No such user:" + userId);
            return;
        }
        Slog.w(TAG, "Revoke runtime  permission: " + name + " for package " + packageName);
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "revokeRuntimePermission");
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (bp == null) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
            enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(pkg, bp);
            if (Build.isPermissionReviewRequired() && pkg.applicationInfo.targetSdkVersion < 23 && bp.isRuntime()) {
                return;
            }
            SettingBase sb = (SettingBase) pkg.mExtras;
            if (sb == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            PermissionsState permissionsState = sb.getPermissionsState();
            int flags = permissionsState.getPermissionFlags(name, userId);
            if ((flags & 16) != 0) {
                throw new SecurityException("Cannot revoke system fixed permission " + name + " for package " + packageName);
            }
            if (bp.isDevelopment()) {
                if (permissionsState.revokeInstallPermission(bp) != -1) {
                    scheduleWriteSettingsLocked();
                }
            } else {
                if (permissionsState.revokeRuntimePermission(bp, userId) == -1) {
                    return;
                }
                this.mOnPermissionChangeListeners.onPermissionsChanged(pkg.applicationInfo.uid);
                this.mSettings.writeRuntimePermissionsForUserLPr(userId, true);
                int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
                killUid(appId, userId, KILL_APP_REASON_PERMISSIONS_REVOKED);
            }
        }
    }

    private void revokeRuntimePermissionsIfGroupChanged(PackageParser.Package newPackage, PackageParser.Package oldPackage, ArrayList<String> allPackageNames) {
        int numOldPackagePermissions = oldPackage.permissions.size();
        ArrayMap<String, String> oldPermissionNameToGroupName = new ArrayMap<>(numOldPackagePermissions);
        for (int i = 0; i < numOldPackagePermissions; i++) {
            PackageParser.Permission permission = (PackageParser.Permission) oldPackage.permissions.get(i);
            if (permission.group != null) {
                oldPermissionNameToGroupName.put(permission.info.name, permission.group.info.name);
            }
        }
        int numNewPackagePermissions = newPackage.permissions.size();
        for (int newPermissionNum = 0; newPermissionNum < numNewPackagePermissions; newPermissionNum++) {
            PackageParser.Permission newPermission = (PackageParser.Permission) newPackage.permissions.get(newPermissionNum);
            int newProtection = newPermission.info.protectionLevel;
            if ((newProtection & 1) != 0) {
                String permissionName = newPermission.info.name;
                String str = newPermission.group == null ? null : newPermission.group.info.name;
                String oldPermissionGroupName = oldPermissionNameToGroupName.get(permissionName);
                if (str != null && !str.equals(oldPermissionGroupName)) {
                    List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
                    int numUsers = users.size();
                    for (int userNum = 0; userNum < numUsers; userNum++) {
                        int userId = users.get(userNum).id;
                        int numPackages = allPackageNames.size();
                        for (int packageNum = 0; packageNum < numPackages; packageNum++) {
                            String packageName = allPackageNames.get(packageNum);
                            if (checkPermission(permissionName, packageName, userId) == 0) {
                                EventLog.writeEvent(1397638484, "72710897", Integer.valueOf(newPackage.applicationInfo.uid), "Revoking permission", permissionName, "from package", packageName, "as the group changed from", oldPermissionGroupName, "to", str);
                                try {
                                    revokeRuntimePermission(packageName, permissionName, userId);
                                } catch (IllegalArgumentException e) {
                                    Slog.e(TAG, "Could not revoke " + permissionName + " from " + packageName, e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void resetRuntimePermissions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "resetRuntimePermissions");
        }
        synchronized (this.mPackages) {
            updatePermissionsLPw(null, null, 1);
            for (int userId : UserManagerService.getInstance().getUserIds()) {
                int packageCount = this.mPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    PackageParser.Package pkg = this.mPackages.valueAt(i);
                    if (pkg.mExtras instanceof PackageSetting) {
                        PackageSetting ps = (PackageSetting) pkg.mExtras;
                        resetUserChangesToRuntimePermissionsAndFlagsLPw(ps, userId);
                    }
                }
            }
        }
    }

    public int getPermissionFlags(String name, String packageName, int userId) {
        if (!sUserManager.exists(userId)) {
            return 0;
        }
        enforceGrantRevokeRuntimePermissionPermissions("getPermissionFlags");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getPermissionFlags");
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                return 0;
            }
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (bp == null) {
                return 0;
            }
            SettingBase sb = (SettingBase) pkg.mExtras;
            if (sb == null) {
                return 0;
            }
            PermissionsState permissionsState = sb.getPermissionsState();
            return permissionsState.getPermissionFlags(name, userId);
        }
    }

    public void updatePermissionFlags(String name, String packageName, int flagMask, int flagValues, int userId) {
        if (!sUserManager.exists(userId)) {
            return;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlags");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "updatePermissionFlags");
        if (getCallingUid() != 1000) {
            flagMask = flagMask & (-17) & (-33);
            flagValues = flagValues & (-17) & (-33) & (-65);
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (bp == null) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
            SettingBase sb = (SettingBase) pkg.mExtras;
            if (sb == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            PermissionsState permissionsState = sb.getPermissionsState();
            boolean hadState = permissionsState.getRuntimePermissionState(name, userId) != null;
            if (permissionsState.updatePermissionFlags(bp, userId, flagMask, flagValues)) {
                if (CtaUtils.isCtaSupported() && (flagMask & 64) != 0 && (flagValues & 64) == 0 && pkg.mSharedUserId != null && !permissionsState.isPermissionReviewRequired(userId)) {
                    permissionsState.updateReviewRequiredCache(userId);
                }
                if (permissionsState.getInstallPermissionState(name) != null) {
                    scheduleWriteSettingsLocked();
                } else if (permissionsState.getRuntimePermissionState(name, userId) != null || hadState) {
                    this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
                }
            }
        }
    }

    public void updatePermissionFlagsForAllApps(int flagMask, int flagValues, int userId) {
        if (!sUserManager.exists(userId)) {
            return;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlagsForAllApps");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "updatePermissionFlagsForAllApps");
        if (getCallingUid() != 1000) {
            flagMask &= -17;
            flagValues &= -17;
        }
        synchronized (this.mPackages) {
            boolean changed = false;
            int packageCount = this.mPackages.size();
            for (int pkgIndex = 0; pkgIndex < packageCount; pkgIndex++) {
                PackageParser.Package pkg = this.mPackages.valueAt(pkgIndex);
                SettingBase sb = (SettingBase) pkg.mExtras;
                if (sb != null) {
                    PermissionsState permissionsState = sb.getPermissionsState();
                    changed |= permissionsState.updatePermissionFlagsForAllPermissions(userId, flagMask, flagValues);
                }
            }
            if (changed) {
                this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
            }
        }
    }

    private void enforceGrantRevokeRuntimePermissionPermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(message + " requires android.permission.GRANT_RUNTIME_PERMISSIONS or android.permission.REVOKE_RUNTIME_PERMISSIONS");
        }
    }

    public boolean shouldShowRequestPermissionRationale(String permissionName, String packageName, int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "canShowRequestPermissionRationale for user " + userId);
        }
        int uid = getPackageUid(packageName, 268435456, userId);
        if (UserHandle.getAppId(getCallingUid()) != UserHandle.getAppId(uid) || checkPermission(permissionName, packageName, userId) == 0) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = getPermissionFlags(permissionName, packageName, userId);
            Binder.restoreCallingIdentity(identity);
            return (flags & 22) == 0 && (flags & 1) != 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void addOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS", "addOnPermissionsChangeListener");
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.addListenerLocked(listener);
        }
    }

    public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.removeListenerLocked(listener);
        }
    }

    public boolean isProtectedBroadcast(String actionName) {
        synchronized (this.mPackages) {
            if (this.mProtectedBroadcasts.contains(actionName)) {
                return true;
            }
            if (actionName != null) {
                if (!actionName.startsWith("android.net.netmon.lingerExpired") && !actionName.startsWith("com.android.server.sip.SipWakeupTimer") && !actionName.startsWith("com.android.internal.telephony.data-reconnect")) {
                }
                return true;
            }
            return false;
        }
    }

    public int checkSignatures(String pkg1, String pkg2) {
        synchronized (this.mPackages) {
            PackageParser.Package p1 = this.mPackages.get(pkg1);
            PackageParser.Package p2 = this.mPackages.get(pkg2);
            if (p1 == null || p1.mExtras == null || p2 == null || p2.mExtras == null) {
                return -4;
            }
            return compareSignatures(p1.mSignatures, p2.mSignatures);
        }
    }

    public int checkUidSignatures(int uid1, int uid2) {
        Signature[] s1;
        Signature[] s2;
        int uid12 = UserHandle.getAppId(uid1);
        int uid22 = UserHandle.getAppId(uid2);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid12);
            if (obj == null) {
                return -4;
            }
            if (obj instanceof SharedUserSetting) {
                s1 = ((SharedUserSetting) obj).signatures.mSignatures;
            } else {
                if (!(obj instanceof PackageSetting)) {
                    return -4;
                }
                s1 = ((PackageSetting) obj).signatures.mSignatures;
            }
            Object obj2 = this.mSettings.getUserIdLPr(uid22);
            if (obj2 == null) {
                return -4;
            }
            if (obj2 instanceof SharedUserSetting) {
                s2 = ((SharedUserSetting) obj2).signatures.mSignatures;
            } else {
                if (!(obj2 instanceof PackageSetting)) {
                    return -4;
                }
                s2 = ((PackageSetting) obj2).signatures.mSignatures;
            }
            return compareSignatures(s1, s2);
        }
    }

    private void killUid(int appId, int userId, String reason) {
        long identity = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am != null) {
                try {
                    am.killUid(appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    static int compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null) {
            return s2 == null ? 1 : -1;
        }
        if (s2 == null) {
            return -2;
        }
        if (s1.length != s2.length) {
            return -3;
        }
        if (s1.length == 1) {
            return s1[0].equals(s2[0]) ? 0 : -3;
        }
        ArraySet<Signature> set1 = new ArraySet<>();
        for (Signature sig : s1) {
            set1.add(sig);
        }
        ArraySet<Signature> set2 = new ArraySet<>();
        for (Signature sig2 : s2) {
            set2.add(sig2);
        }
        return set1.equals(set2) ? 0 : -3;
    }

    private boolean isCompatSignatureUpdateNeeded(PackageParser.Package scannedPkg) {
        Settings.VersionInfo ver = getSettingsVersionForPackage(scannedPkg);
        return ver.databaseVersion < 2;
    }

    private int compareSignaturesCompat(PackageSignatures existingSigs, PackageParser.Package scannedPkg) {
        if (!isCompatSignatureUpdateNeeded(scannedPkg)) {
            return -3;
        }
        ArraySet<Signature> existingSet = new ArraySet<>();
        for (Signature signature : existingSigs.mSignatures) {
            existingSet.add(signature);
        }
        ArraySet<Signature> scannedCompatSet = new ArraySet<>();
        for (Signature sig : scannedPkg.mSignatures) {
            try {
                Signature[] chainSignatures = sig.getChainSignatures();
                for (Signature chainSig : chainSignatures) {
                    scannedCompatSet.add(chainSig);
                }
            } catch (CertificateEncodingException e) {
                scannedCompatSet.add(sig);
            }
        }
        if (!scannedCompatSet.equals(existingSet)) {
            return -3;
        }
        existingSigs.assignSignatures(scannedPkg.mSignatures);
        synchronized (this.mPackages) {
            this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(scannedPkg.packageName);
        }
        return 0;
    }

    private boolean isRecoverSignatureUpdateNeeded(PackageParser.Package scannedPkg) {
        Settings.VersionInfo ver = getSettingsVersionForPackage(scannedPkg);
        return ver.databaseVersion < 3;
    }

    private int compareSignaturesRecover(PackageSignatures existingSigs, PackageParser.Package scannedPkg) {
        if (!isRecoverSignatureUpdateNeeded(scannedPkg)) {
            return -3;
        }
        String msg = null;
        try {
            if (Signature.areEffectiveMatch(existingSigs.mSignatures, scannedPkg.mSignatures)) {
                logCriticalInfo(4, "Recovered effectively matching certificates for " + scannedPkg.packageName);
                return 0;
            }
        } catch (CertificateException e) {
            msg = e.getMessage();
        }
        logCriticalInfo(4, "Failed to recover certificates for " + scannedPkg.packageName + ": " + msg);
        return -3;
    }

    public List<String> getAllPackages() {
        ArrayList arrayList;
        synchronized (this.mPackages) {
            arrayList = new ArrayList(this.mPackages.keySet());
        }
        return arrayList;
    }

    public String[] getPackagesForUid(int uid) {
        int uid2 = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid2);
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                int N = sus.packages.size();
                String[] res = new String[N];
                for (int i = 0; i < N; i++) {
                    res[i] = sus.packages.valueAt(i).name;
                }
                return res;
            }
            if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                return new String[]{ps.name};
            }
            return null;
        }
    }

    public String getNameForUid(int uid) {
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                return sus.name + ":" + sus.userId;
            }
            if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                return ps.name;
            }
            return null;
        }
    }

    public int getUidForSharedUser(String sharedUserName) {
        if (sharedUserName == null) {
            return -1;
        }
        synchronized (this.mPackages) {
            SharedUserSetting suid = this.mSettings.getSharedUserLPw(sharedUserName, 0, 0, false);
            if (suid == null) {
                return -1;
            }
            return suid.userId;
        }
    }

    public int getFlagsForUid(int uid) {
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                return sus.pkgFlags;
            }
            if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                return ps.pkgFlags;
            }
            return 0;
        }
    }

    public int getPrivateFlagsForUid(int uid) {
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                return sus.pkgPrivateFlags;
            }
            if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                return ps.pkgPrivateFlags;
            }
            return 0;
        }
    }

    public boolean isUidPrivileged(int uid) {
        int uid2 = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid2);
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                Iterator<PackageSetting> it = sus.packages.iterator();
                while (it.hasNext()) {
                    if (it.next().isPrivileged()) {
                        return true;
                    }
                }
            } else if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                return ps.isPrivileged();
            }
            return false;
        }
    }

    public String[] getAppOpPermissionPackages(String permissionName) {
        synchronized (this.mPackages) {
            ArraySet<String> pkgs = this.mAppOpPermissionPackages.get(permissionName);
            if (pkgs == null) {
                return null;
            }
            return (String[]) pkgs.toArray(new String[pkgs.size()]);
        }
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        try {
            Trace.traceBegin(1048576L, "resolveIntent");
            if (!sUserManager.exists(userId)) {
                return null;
            }
            int flags2 = updateFlagsForResolve(flags, userId, intent);
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "resolve intent");
            Trace.traceBegin(1048576L, "queryIntentActivities");
            List<ResolveInfo> query = queryIntentActivitiesInternal(intent, resolvedType, flags2, userId);
            Trace.traceEnd(1048576L);
            ResolveInfo bestChoice = chooseBestActivity(intent, resolvedType, flags2, query, userId);
            if (isEphemeralAllowed(intent, query, userId)) {
                Trace.traceBegin(1048576L, "resolveEphemeral");
                EphemeralResolveInfo ai = getEphemeralResolveInfo(intent, resolvedType, userId);
                if (ai != null) {
                    if (DEBUG_EPHEMERAL) {
                        Slog.v(TAG, "Returning an EphemeralResolveInfo");
                    }
                    bestChoice.ephemeralInstaller = this.mEphemeralInstallerInfo;
                    bestChoice.ephemeralResolveInfo = ai;
                }
                Trace.traceEnd(1048576L);
            }
            return bestChoice;
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    public void setLastChosenActivity(Intent intent, String resolvedType, int flags, IntentFilter filter, int match, ComponentName activity) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG_PREFERRED) {
            Log.v(TAG, "setLastChosenActivity intent=" + intent + " resolvedType=" + resolvedType + " flags=" + flags + " filter=" + filter + " match=" + match + " activity=" + activity);
            filter.dump(new PrintStreamPrinter(System.out), "    ");
        }
        intent.setComponent(null);
        List<ResolveInfo> query = queryIntentActivitiesInternal(intent, resolvedType, flags, userId);
        findPreferredActivity(intent, resolvedType, flags, query, 0, false, true, false, userId);
        addPreferredActivityInternal(filter, match, null, activity, false, userId, "Setting last chosen");
    }

    public ResolveInfo getLastChosenActivity(Intent intent, String resolvedType, int flags) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG_PREFERRED) {
            Log.v(TAG, "Querying last chosen activity for " + intent);
        }
        List<ResolveInfo> query = queryIntentActivitiesInternal(intent, resolvedType, flags, userId);
        return findPreferredActivity(intent, resolvedType, flags, query, 0, false, false, false, userId);
    }

    private boolean isEphemeralAllowed(Intent intent, List<ResolveInfo> resolvedActivites, int userId) {
        return false;
    }

    private EphemeralResolveInfo getEphemeralResolveInfo(Intent intent, String resolvedType, int userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hostBytes = intent.getData().getHost().getBytes();
            byte[] digestBytes = digest.digest(hostBytes);
            int shaPrefix = (digestBytes[0] << 24) | (digestBytes[1] << 16) | (digestBytes[2] << 8) | (digestBytes[3] << 0);
            List<EphemeralResolveInfo> ephemeralResolveInfoList = this.mEphemeralResolverConnection.getEphemeralResolveInfoList(shaPrefix);
            if (ephemeralResolveInfoList == null || ephemeralResolveInfoList.size() == 0) {
                return null;
            }
            for (int i = ephemeralResolveInfoList.size() - 1; i >= 0; i--) {
                EphemeralResolveInfo ephemeralApplication = ephemeralResolveInfoList.get(i);
                if (Arrays.equals(digestBytes, ephemeralApplication.getDigestBytes())) {
                    List<IntentFilter> filters = ephemeralApplication.getFilters();
                    if (filters.isEmpty()) {
                        continue;
                    } else {
                        EphemeralIntentResolver ephemeralResolver = new EphemeralIntentResolver(null);
                        for (int j = filters.size() - 1; j >= 0; j--) {
                            EphemeralResolveInfo.EphemeralResolveIntentInfo intentInfo = new EphemeralResolveInfo.EphemeralResolveIntentInfo(filters.get(j), ephemeralApplication);
                            ephemeralResolver.addFilter(intentInfo);
                        }
                        List<EphemeralResolveInfo> matchedResolveInfoList = ephemeralResolver.queryIntent(intent, resolvedType, false, userId);
                        if (!matchedResolveInfoList.isEmpty()) {
                            return matchedResolveInfoList.get(0);
                        }
                    }
                }
            }
            return null;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private ResolveInfo chooseBestActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        if (query == null) {
            return null;
        }
        int N = query.size();
        if (N == 1) {
            return query.get(0);
        }
        if (N <= 1) {
            return null;
        }
        boolean debug = (intent.getFlags() & 8) != 0;
        ResolveInfo r0 = query.get(0);
        ResolveInfo r1 = query.get(1);
        if (DEBUG_INTENT_MATCHING || debug) {
            Slog.v(TAG, r0.activityInfo.name + "=" + r0.priority + " vs " + r1.activityInfo.name + "=" + r1.priority);
        }
        if (r0.priority != r1.priority || r0.preferredOrder != r1.preferredOrder || r0.isDefault != r1.isDefault) {
            return query.get(0);
        }
        ResolveInfo ri = findPreferredActivity(intent, resolvedType, flags, query, r0.priority, true, false, debug, userId);
        if (ri != null) {
            return ri;
        }
        ResolveInfo ri2 = new ResolveInfo(this.mResolveInfo);
        ri2.activityInfo = new ActivityInfo(ri2.activityInfo);
        ri2.activityInfo.labelRes = ResolverActivity.getLabelRes(intent.getAction());
        String intentPackage = intent.getPackage();
        if (!TextUtils.isEmpty(intentPackage) && allHavePackage(query, intentPackage)) {
            ApplicationInfo appi = query.get(0).activityInfo.applicationInfo;
            ri2.resolvePackageName = intentPackage;
            if (userNeedsBadging(userId)) {
                ri2.noResourceId = true;
            } else {
                ri2.icon = appi.icon;
            }
            ri2.iconResourceId = appi.icon;
            ri2.labelRes = appi.labelRes;
        }
        ri2.activityInfo.applicationInfo = new ApplicationInfo(ri2.activityInfo.applicationInfo);
        if (userId != 0) {
            ri2.activityInfo.applicationInfo.uid = UserHandle.getUid(userId, UserHandle.getAppId(ri2.activityInfo.applicationInfo.uid));
        }
        if (ri2.activityInfo.metaData == null) {
            ri2.activityInfo.metaData = new Bundle();
        }
        ri2.activityInfo.metaData.putBoolean("android.dock_home", true);
        return ri2;
    }

    private boolean allHavePackage(List<ResolveInfo> list, String packageName) {
        if (ArrayUtils.isEmpty(list)) {
            return false;
        }
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo ri = list.get(i);
            ActivityInfo ai = ri != null ? ri.activityInfo : null;
            if (ai == null || !packageName.equals(ai.packageName)) {
                return false;
            }
        }
        return true;
    }

    private ResolveInfo findPersistentPreferredActivityLP(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, boolean debug, int userId) {
        int N = query.size();
        PersistentPreferredIntentResolver ppir = this.mSettings.mPersistentPreferredActivities.get(userId);
        if (DEBUG_PREFERRED || debug) {
            Slog.v(TAG, "Looking for presistent preferred activities...");
        }
        List<PersistentPreferredActivity> pprefs = ppir != null ? ppir.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId) : null;
        if (pprefs == null || pprefs.size() <= 0) {
            return null;
        }
        int M = pprefs.size();
        for (int i = 0; i < M; i++) {
            PersistentPreferredActivity ppa = pprefs.get(i);
            if (DEBUG_PREFERRED || debug) {
                Slog.v(TAG, "Checking PersistentPreferredActivity ds=" + (ppa.countDataSchemes() > 0 ? ppa.getDataScheme(0) : "<none>") + "\n  component=" + ppa.mComponent);
                ppa.dump(new LogPrinter(2, TAG, 3), "  ");
            }
            ActivityInfo ai = getActivityInfo(ppa.mComponent, flags | 512, userId);
            if (DEBUG_PREFERRED || debug) {
                Slog.v(TAG, "Found persistent preferred activity:");
                if (ai != null) {
                    ai.dump(new LogPrinter(2, TAG, 3), "  ");
                } else {
                    Slog.v(TAG, "  null");
                }
            }
            if (ai != null) {
                for (int j = 0; j < N; j++) {
                    ResolveInfo ri = query.get(j);
                    if (ri.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri.activityInfo.name.equals(ai.name)) {
                        if (DEBUG_PREFERRED || debug) {
                            Slog.v(TAG, "Returning persistent preferred activity: " + ri.activityInfo.packageName + "/" + ri.activityInfo.name);
                        }
                        return ri;
                    }
                }
            }
        }
        return null;
    }

    ResolveInfo findPreferredActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int priority, boolean always, boolean removeMatches, boolean debug, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        synchronized (this.mPackages) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
            }
            if (DEBUG_PREFERRED) {
                intent.addFlags(8);
            }
            ResolveInfo pri = findPersistentPreferredActivityLP(intent, resolvedType, flags2, query, debug, userId);
            if (pri != null) {
                return pri;
            }
            PreferredIntentResolver pir = this.mSettings.mPreferredActivities.get(userId);
            if (DEBUG_PREFERRED || debug) {
                Slog.v(TAG, "Looking for preferred activities...");
            }
            List<PreferredActivity> prefs = pir != null ? pir.queryIntent(intent, resolvedType, (65536 & flags2) != 0, userId) : null;
            if (prefs != null && prefs.size() > 0) {
                boolean changed = false;
                int match = 0;
                try {
                    if (DEBUG_PREFERRED || debug) {
                        Slog.v(TAG, "Figuring out best match...");
                    }
                    int N = query.size();
                    for (int j = 0; j < N; j++) {
                        ResolveInfo ri = query.get(j);
                        if (DEBUG_PREFERRED || debug) {
                            Slog.v(TAG, "Match for " + ri.activityInfo + ": 0x" + Integer.toHexString(match));
                        }
                        if (ri.match > match) {
                            match = ri.match;
                        }
                    }
                    if (DEBUG_PREFERRED || debug) {
                        Slog.v(TAG, "Best match: 0x" + Integer.toHexString(match));
                    }
                    int match2 = match & 268369920;
                    int M = prefs.size();
                    for (int i = 0; i < M; i++) {
                        PreferredActivity pa = prefs.get(i);
                        if (DEBUG_PREFERRED || debug) {
                            Slog.v(TAG, "Checking PreferredActivity ds=" + (pa.countDataSchemes() > 0 ? pa.getDataScheme(0) : "<none>") + "\n  component=" + pa.mPref.mComponent);
                            pa.dump(new LogPrinter(2, TAG, 3), "  ");
                        }
                        if (pa.mPref.mMatch != match2) {
                            if (DEBUG_PREFERRED || debug) {
                                Slog.v(TAG, "Skipping bad match " + Integer.toHexString(pa.mPref.mMatch));
                            }
                        } else if (!always || pa.mPref.mAlways) {
                            ActivityInfo ai = getActivityInfo(pa.mPref.mComponent, flags2 | 512 | DumpState.DUMP_FROZEN | 262144, userId);
                            if (DEBUG_PREFERRED || debug) {
                                Slog.v(TAG, "Found preferred activity:");
                                if (ai != null) {
                                    ai.dump(new LogPrinter(2, TAG, 3), "  ");
                                } else {
                                    Slog.v(TAG, "  null");
                                }
                            }
                            if (ai == null) {
                                Slog.w(TAG, "Removing dangling preferred activity: " + pa.mPref.mComponent);
                                pir.removeFilter(pa);
                                changed = true;
                            } else {
                                int j2 = 0;
                                while (true) {
                                    if (j2 < N) {
                                        ResolveInfo ri2 = query.get(j2);
                                        if (ri2.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri2.activityInfo.name.equals(ai.name)) {
                                            if (!removeMatches) {
                                                if (!always || pa.mPref.sameSet(query)) {
                                                    if (DEBUG_PREFERRED || debug) {
                                                        Slog.v(TAG, "Returning preferred activity: " + ri2.activityInfo.packageName + "/" + ri2.activityInfo.name);
                                                    }
                                                    if (changed) {
                                                        if (DEBUG_PREFERRED) {
                                                            Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                                                        }
                                                        scheduleWritePackageRestrictionsLocked(userId);
                                                    }
                                                    return ri2;
                                                }
                                                Slog.i(TAG, "Result set changed, dropping preferred activity for " + intent + " type " + resolvedType);
                                                if (DEBUG_PREFERRED) {
                                                    Slog.v(TAG, "Removing preferred activity since set changed " + pa.mPref.mComponent);
                                                }
                                                pir.removeFilter(pa);
                                                PreferredActivity lastChosen = new PreferredActivity(pa, pa.mPref.mMatch, null, pa.mPref.mComponent, false);
                                                pir.addFilter(lastChosen);
                                                changed = true;
                                                return null;
                                            }
                                            pir.removeFilter(pa);
                                            changed = true;
                                            if (DEBUG_PREFERRED) {
                                                Slog.v(TAG, "Removing match " + pa.mPref.mComponent);
                                            }
                                        } else {
                                            j2++;
                                        }
                                    }
                                }
                            }
                        } else if (DEBUG_PREFERRED || debug) {
                            Slog.v(TAG, "Skipping mAlways=false entry");
                        }
                    }
                    if (changed) {
                        if (DEBUG_PREFERRED) {
                            Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                        }
                        scheduleWritePackageRestrictionsLocked(userId);
                    }
                } finally {
                    if (0 != 0) {
                        if (DEBUG_PREFERRED) {
                            Slog.v(TAG, "Preferred activity bookkeeping changed; writing restrictions");
                        }
                        scheduleWritePackageRestrictionsLocked(userId);
                    }
                }
            }
            if (!DEBUG_PREFERRED && !debug) {
                return null;
            }
            Slog.v(TAG, "No preferred activity to return");
            return null;
        }
    }

    public boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        List<CrossProfileIntentFilter> matches = getMatchingCrossProfileIntentFilters(intent, resolvedType, sourceUserId);
        if (matches != null) {
            int size = matches.size();
            for (int i = 0; i < size; i++) {
                if (matches.get(i).getTargetUserId() == targetUserId) {
                    return true;
                }
            }
        }
        if (hasWebURI(intent)) {
            UserInfo parent = getProfileParent(sourceUserId);
            synchronized (this.mPackages) {
                int flags = updateFlagsForResolve(0, parent.id, intent);
                CrossProfileDomainInfo xpDomainInfo = getCrossProfileDomainPreferredLpr(intent, resolvedType, flags, sourceUserId, parent.id);
                z = xpDomainInfo != null;
            }
            return z;
        }
        return false;
    }

    private UserInfo getProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return sUserManager.getProfileParent(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private List<CrossProfileIntentFilter> getMatchingCrossProfileIntentFilters(Intent intent, String resolvedType, int userId) {
        CrossProfileIntentResolver resolver = this.mSettings.mCrossProfileIntentResolvers.get(userId);
        if (resolver != null) {
            return resolver.queryIntent(intent, resolvedType, false, userId);
        }
        return null;
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {
        try {
            Trace.traceBegin(1048576L, "queryIntentActivities");
            return new ParceledListSlice<>(queryIntentActivitiesInternal(intent, resolvedType, flags, userId));
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activities");
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ActivityInfo ai = getActivityInfo(comp, flags2, userId);
            if (ai != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                List<CrossProfileIntentFilter> matchingFilters = getMatchingCrossProfileIntentFilters(intent, resolvedType, userId);
                ResolveInfo xpResolveInfo = querySkipCurrentProfileIntents(matchingFilters, intent, resolvedType, flags2, userId);
                if (xpResolveInfo != null) {
                    List<ResolveInfo> result = new ArrayList<>(1);
                    result.add(xpResolveInfo);
                    return filterIfNotSystemUser(result, userId);
                }
                List<ResolveInfo> result2 = filterIfNotSystemUser(this.mActivities.queryIntent(intent, resolvedType, flags2, userId), userId);
                boolean hasNonNegativePriorityResult = hasNonNegativePriority(result2);
                ResolveInfo xpResolveInfo2 = queryCrossProfileIntents(matchingFilters, intent, resolvedType, flags2, userId, hasNonNegativePriorityResult);
                if (xpResolveInfo2 != null && isUserEnabled(xpResolveInfo2.targetUserId)) {
                    boolean isVisibleToUser = filterIfNotSystemUser(Collections.singletonList(xpResolveInfo2), userId).size() > 0;
                    if (isVisibleToUser) {
                        result2.add(xpResolveInfo2);
                        Collections.sort(result2, mResolvePrioritySorter);
                    }
                }
                if (hasWebURI(intent)) {
                    CrossProfileDomainInfo xpDomainInfo = null;
                    UserInfo parent = getProfileParent(userId);
                    if (parent != null) {
                        xpDomainInfo = getCrossProfileDomainPreferredLpr(intent, resolvedType, flags2, userId, parent.id);
                    }
                    if (xpDomainInfo != null) {
                        if (xpResolveInfo2 != null) {
                            result2.remove(xpResolveInfo2);
                        }
                        if (result2.size() == 0) {
                            result2.add(xpDomainInfo.resolveInfo);
                            return result2;
                        }
                    } else if (result2.size() <= 1) {
                        return result2;
                    }
                    result2 = filterCandidatesWithDomainPreferredActivitiesLPr(intent, flags2, result2, xpDomainInfo, userId);
                    Collections.sort(result2, mResolvePrioritySorter);
                }
                return result2;
            }
            PackageParser.Package pkg = this.mPackages.get(pkgName);
            if (pkg != null) {
                return filterIfNotSystemUser(this.mActivities.queryIntentForPackage(intent, resolvedType, flags2, pkg.activities, userId), userId);
            }
            return new ArrayList();
        }
    }

    private static class CrossProfileDomainInfo {
        int bestDomainVerificationStatus;
        ResolveInfo resolveInfo;

        CrossProfileDomainInfo(CrossProfileDomainInfo crossProfileDomainInfo) {
            this();
        }

        private CrossProfileDomainInfo() {
        }
    }

    private CrossProfileDomainInfo getCrossProfileDomainPreferredLpr(Intent intent, String resolvedType, int flags, int sourceUserId, int parentUserId) {
        List<ResolveInfo> resultTargetUser;
        if (!sUserManager.hasUserRestriction("allow_parent_profile_app_linking", sourceUserId) || (resultTargetUser = this.mActivities.queryIntent(intent, resolvedType, flags, parentUserId)) == null || resultTargetUser.isEmpty()) {
            return null;
        }
        CrossProfileDomainInfo result = null;
        int size = resultTargetUser.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo riTargetUser = resultTargetUser.get(i);
            if (!riTargetUser.handleAllWebDataURI) {
                String packageName = riTargetUser.activityInfo.packageName;
                PackageSetting ps = this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    long verificationState = getDomainVerificationStatusLPr(ps, parentUserId);
                    int status = (int) (verificationState >> 32);
                    if (result == null) {
                        result = new CrossProfileDomainInfo(null);
                        result.resolveInfo = createForwardingResolveInfoUnchecked(new IntentFilter(), sourceUserId, parentUserId);
                        result.bestDomainVerificationStatus = status;
                    } else {
                        result.bestDomainVerificationStatus = bestDomainVerificationStatus(status, result.bestDomainVerificationStatus);
                    }
                }
            }
        }
        if (result != null && result.bestDomainVerificationStatus == 3) {
            return null;
        }
        return result;
    }

    private int bestDomainVerificationStatus(int status1, int status2) {
        if (status1 == 3) {
            return status2;
        }
        if (status2 == 3) {
            return status1;
        }
        return (int) MathUtils.max(status1, status2);
    }

    private boolean isUserEnabled(int userId) {
        long callingId = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = sUserManager.getUserInfo(userId);
            return userInfo != null ? userInfo.isEnabled() : false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private List<ResolveInfo> filterIfNotSystemUser(List<ResolveInfo> resolveInfos, int userId) {
        if (userId == 0) {
            return resolveInfos;
        }
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = resolveInfos.get(i);
            if ((info.activityInfo.flags & 536870912) != 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    private boolean hasNonNegativePriority(List<ResolveInfo> resolveInfos) {
        return resolveInfos.size() > 0 && resolveInfos.get(0).priority >= 0;
    }

    private static boolean hasWebURI(Intent intent) {
        if (intent.getData() == null) {
            return false;
        }
        String scheme = intent.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return false;
        }
        if (scheme.equals("http")) {
            return true;
        }
        return scheme.equals("https");
    }

    private List<ResolveInfo> filterCandidatesWithDomainPreferredActivitiesLPr(Intent intent, int matchFlags, List<ResolveInfo> candidates, CrossProfileDomainInfo xpDomainInfo, int userId) {
        boolean debug = (intent.getFlags() & 8) != 0;
        if (DEBUG_PREFERRED || DEBUG_DOMAIN_VERIFICATION) {
            Slog.v(TAG, "Filtering results with preferred activities. Candidates count: " + candidates.size());
        }
        ArrayList<ResolveInfo> result = new ArrayList<>();
        ArrayList<ResolveInfo> alwaysList = new ArrayList<>();
        ArrayList<ResolveInfo> undefinedList = new ArrayList<>();
        ArrayList<ResolveInfo> alwaysAskList = new ArrayList<>();
        ArrayList<ResolveInfo> neverList = new ArrayList<>();
        ArrayList<ResolveInfo> matchAllList = new ArrayList<>();
        synchronized (this.mPackages) {
            int count = candidates.size();
            for (int n = 0; n < count; n++) {
                ResolveInfo info = candidates.get(n);
                String packageName = info.activityInfo.packageName;
                PackageSetting ps = this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    if (info.handleAllWebDataURI) {
                        matchAllList.add(info);
                    } else {
                        long packedStatus = getDomainVerificationStatusLPr(ps, userId);
                        int status = (int) (packedStatus >> 32);
                        int linkGeneration = (int) ((-1) & packedStatus);
                        if (status == 2) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.i(TAG, "  + always: " + info.activityInfo.packageName + " : linkgen=" + linkGeneration);
                            }
                            info.preferredOrder = linkGeneration;
                            alwaysList.add(info);
                        } else if (status == 3) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.i(TAG, "  + never: " + info.activityInfo.packageName);
                            }
                            neverList.add(info);
                        } else if (status == 4) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.i(TAG, "  + always-ask: " + info.activityInfo.packageName);
                            }
                            alwaysAskList.add(info);
                        } else if (status == 0 || status == 1) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.i(TAG, "  + ask: " + info.activityInfo.packageName);
                            }
                            undefinedList.add(info);
                        }
                    }
                }
            }
            boolean includeBrowser = false;
            if (alwaysList.size() > 0) {
                result.addAll(alwaysList);
            } else {
                result.addAll(undefinedList);
                if (xpDomainInfo != null && xpDomainInfo.bestDomainVerificationStatus != 3) {
                    result.add(xpDomainInfo.resolveInfo);
                }
                includeBrowser = true;
            }
            if (alwaysAskList.size() > 0) {
                for (ResolveInfo i : result) {
                    i.preferredOrder = 0;
                }
                result.addAll(alwaysAskList);
                includeBrowser = true;
            }
            if (includeBrowser) {
                if (DEBUG_DOMAIN_VERIFICATION) {
                    Slog.v(TAG, "   ...including browsers in candidate set");
                }
                if ((131072 & matchFlags) != 0) {
                    result.addAll(matchAllList);
                } else {
                    String defaultBrowserPackageName = getDefaultBrowserPackageName(userId);
                    int maxMatchPrio = 0;
                    ResolveInfo defaultBrowserMatch = null;
                    int numCandidates = matchAllList.size();
                    for (int n2 = 0; n2 < numCandidates; n2++) {
                        ResolveInfo info2 = matchAllList.get(n2);
                        if (info2.priority > maxMatchPrio) {
                            maxMatchPrio = info2.priority;
                        }
                        if (info2.activityInfo.packageName.equals(defaultBrowserPackageName) && (defaultBrowserMatch == null || defaultBrowserMatch.priority < info2.priority)) {
                            if (debug) {
                                Slog.v(TAG, "Considering default browser match " + info2);
                            }
                            defaultBrowserMatch = info2;
                        }
                    }
                    if (defaultBrowserMatch != null && defaultBrowserMatch.priority >= maxMatchPrio && !TextUtils.isEmpty(defaultBrowserPackageName)) {
                        if (debug) {
                            Slog.v(TAG, "Default browser match " + defaultBrowserMatch);
                        }
                        result.add(defaultBrowserMatch);
                    } else {
                        result.addAll(matchAllList);
                    }
                }
                if (result.size() == 0) {
                    result.addAll(candidates);
                    result.removeAll(neverList);
                }
            }
        }
        if (DEBUG_PREFERRED || DEBUG_DOMAIN_VERIFICATION) {
            Slog.v(TAG, "Filtered results with preferred activities. New candidates count: " + result.size());
            Iterator info$iterator = result.iterator();
            while (info$iterator.hasNext()) {
                Slog.v(TAG, "  + " + ((ResolveInfo) info$iterator.next()).activityInfo);
            }
        }
        return result;
    }

    private long getDomainVerificationStatusLPr(PackageSetting ps, int userId) {
        long result = ps.getDomainVerificationStatusForUser(userId);
        if ((result >> 32) == 0 && ps.getIntentFilterVerificationInfo() != null) {
            return ((long) ps.getIntentFilterVerificationInfo().getStatus()) << 32;
        }
        return result;
    }

    private ResolveInfo querySkipCurrentProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId) {
        ResolveInfo resolveInfo;
        if (matchingFilters != null) {
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = matchingFilters.get(i);
                if ((filter.getFlags() & 2) != 0 && (resolveInfo = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId)) != null) {
                    return resolveInfo;
                }
            }
        }
        return null;
    }

    private ResolveInfo queryCrossProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId, boolean matchInCurrentProfile) {
        if (matchingFilters != null) {
            SparseBooleanArray alreadyTriedUserIds = new SparseBooleanArray();
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = matchingFilters.get(i);
                int targetUserId = filter.getTargetUserId();
                boolean skipCurrentProfile = (filter.getFlags() & 2) != 0;
                boolean skipCurrentProfileIfNoMatchFound = (filter.getFlags() & 4) != 0;
                if (!skipCurrentProfile && !alreadyTriedUserIds.get(targetUserId) && (!skipCurrentProfileIfNoMatchFound || !matchInCurrentProfile)) {
                    ResolveInfo resolveInfo = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId);
                    if (resolveInfo != null) {
                        return resolveInfo;
                    }
                    alreadyTriedUserIds.put(targetUserId, true);
                }
            }
            return null;
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfo(CrossProfileIntentFilter filter, Intent intent, String resolvedType, int flags, int sourceUserId) {
        int targetUserId = filter.getTargetUserId();
        List<ResolveInfo> resultTargetUser = this.mActivities.queryIntent(intent, resolvedType, flags, targetUserId);
        if (resultTargetUser != null && isUserEnabled(targetUserId)) {
            for (int i = resultTargetUser.size() - 1; i >= 0; i--) {
                if ((resultTargetUser.get(i).activityInfo.applicationInfo.flags & 1073741824) == 0) {
                    return createForwardingResolveInfoUnchecked(filter, sourceUserId, targetUserId);
                }
            }
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfoUnchecked(IntentFilter filter, int sourceUserId, int targetUserId) {
        String className;
        ResolveInfo forwardingResolveInfo = new ResolveInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            boolean targetIsProfile = sUserManager.getUserInfo(targetUserId).isManagedProfile();
            if (targetIsProfile) {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE;
            } else {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_PARENT;
            }
            ComponentName forwardingActivityComponentName = new ComponentName(this.mAndroidApplication.packageName, className);
            ActivityInfo forwardingActivityInfo = getActivityInfo(forwardingActivityComponentName, 0, sourceUserId);
            if (!targetIsProfile) {
                forwardingActivityInfo.showUserIcon = targetUserId;
                forwardingResolveInfo.noResourceId = true;
            }
            forwardingResolveInfo.activityInfo = forwardingActivityInfo;
            forwardingResolveInfo.priority = 0;
            forwardingResolveInfo.preferredOrder = 0;
            forwardingResolveInfo.match = 0;
            forwardingResolveInfo.isDefault = true;
            forwardingResolveInfo.filter = filter;
            forwardingResolveInfo.targetUserId = targetUserId;
            return forwardingResolveInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice<>(queryIntentActivityOptionsInternal(caller, specifics, specificTypes, intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentActivityOptionsInternal(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) {
        Iterator<String> it;
        ActivityInfo ai;
        int N;
        int j;
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activity options");
        String resultsAction = intent.getAction();
        List<ResolveInfo> results = queryIntentActivitiesInternal(intent, resolvedType, flags2 | 64, userId);
        if (DEBUG_INTENT_MATCHING) {
            Log.v(TAG, "Query " + intent + ": " + results);
        }
        int specificsPos = 0;
        if (specifics != null) {
            for (int i = 0; i < specifics.length; i++) {
                Intent sintent = specifics[i];
                if (sintent != null) {
                    if (DEBUG_INTENT_MATCHING) {
                        Log.v(TAG, "Specific #" + i + ": " + sintent);
                    }
                    String action = sintent.getAction();
                    if (resultsAction != null && resultsAction.equals(action)) {
                        action = null;
                    }
                    ResolveInfo ri = null;
                    ComponentName comp = sintent.getComponent();
                    if (comp == null) {
                        ri = resolveIntent(sintent, specificTypes != null ? specificTypes[i] : null, flags2, userId);
                        if (ri != null) {
                            if (ri == this.mResolveInfo) {
                            }
                            ai = ri.activityInfo;
                            comp = new ComponentName(ai.applicationInfo.packageName, ai.name);
                            if (DEBUG_INTENT_MATCHING) {
                                Log.v(TAG, "Specific #" + i + ": " + ai);
                            }
                            N = results.size();
                            j = specificsPos;
                            while (j < N) {
                                ResolveInfo sri = results.get(j);
                                if ((sri.activityInfo.name.equals(comp.getClassName()) && sri.activityInfo.applicationInfo.packageName.equals(comp.getPackageName())) || (action != null && sri.filter.matchAction(action))) {
                                    results.remove(j);
                                    if (DEBUG_INTENT_MATCHING) {
                                        Log.v(TAG, "Removing duplicate item from " + j + " due to specific " + specificsPos);
                                    }
                                    if (ri == null) {
                                        ri = sri;
                                    }
                                    j--;
                                    N--;
                                }
                                j++;
                            }
                            if (ri == null) {
                                ri = new ResolveInfo();
                                ri.activityInfo = ai;
                            }
                            results.add(specificsPos, ri);
                            ri.specificIndex = i;
                            specificsPos++;
                        }
                    } else {
                        ai = getActivityInfo(comp, flags2, userId);
                        if (ai != null) {
                            if (DEBUG_INTENT_MATCHING) {
                            }
                            N = results.size();
                            j = specificsPos;
                            while (j < N) {
                            }
                            if (ri == null) {
                            }
                            results.add(specificsPos, ri);
                            ri.specificIndex = i;
                            specificsPos++;
                        }
                    }
                }
            }
        }
        int N2 = results.size();
        for (int i2 = specificsPos; i2 < N2 - 1; i2++) {
            ResolveInfo rii = results.get(i2);
            if (rii.filter != null && (it = rii.filter.actionsIterator()) != null) {
                while (it.hasNext()) {
                    String action2 = it.next();
                    if (resultsAction == null || !resultsAction.equals(action2)) {
                        int j2 = i2 + 1;
                        while (j2 < N2) {
                            ResolveInfo rij = results.get(j2);
                            if (rij.filter != null && rij.filter.hasAction(action2)) {
                                results.remove(j2);
                                if (DEBUG_INTENT_MATCHING) {
                                    Log.v(TAG, "Removing duplicate item from " + j2 + " due to action " + action2 + " at " + i2);
                                }
                                j2--;
                                N2--;
                            }
                            j2++;
                        }
                    }
                }
                if ((flags2 & 64) == 0) {
                    rii.filter = null;
                }
            }
        }
        if (caller != null) {
            int N3 = results.size();
            int i3 = 0;
            while (true) {
                if (i3 >= N3) {
                    break;
                }
                ActivityInfo ainfo = results.get(i3).activityInfo;
                if (!caller.getPackageName().equals(ainfo.applicationInfo.packageName) || !caller.getClassName().equals(ainfo.name)) {
                    i3++;
                } else {
                    results.remove(i3);
                    break;
                }
            }
        }
        if ((flags2 & 64) == 0) {
            int N4 = results.size();
            for (int i4 = 0; i4 < N4; i4++) {
                results.get(i4).filter = null;
            }
        }
        if (DEBUG_INTENT_MATCHING) {
            Log.v(TAG, "Result: " + results);
        }
        return results;
    }

    public ParceledListSlice<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice<>(queryIntentReceiversInternal(intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentReceiversInternal(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ActivityInfo ai = getReceiverInfo(comp, flags2, userId);
            if (ai != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                return this.mReceivers.queryIntent(intent, resolvedType, flags2, userId);
            }
            PackageParser.Package pkg = this.mPackages.get(pkgName);
            if (pkg != null) {
                return this.mReceivers.queryIntentForPackage(intent, resolvedType, flags2, pkg.receivers, userId);
            }
            return Collections.emptyList();
        }
    }

    public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) {
        List<ResolveInfo> query;
        if (sUserManager.exists(userId) && (query = queryIntentServicesInternal(intent, resolvedType, updateFlagsForResolve(flags, userId, intent), userId)) != null && query.size() >= 1) {
            return query.get(0);
        }
        return null;
    }

    public ParceledListSlice<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice<>(queryIntentServicesInternal(intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentServicesInternal(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ServiceInfo si = getServiceInfo(comp, flags2, userId);
            if (si != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.serviceInfo = si;
                list.add(ri);
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                return this.mServices.queryIntent(intent, resolvedType, flags2, userId);
            }
            PackageParser.Package pkg = this.mPackages.get(pkgName);
            if (pkg != null) {
                return this.mServices.queryIntentForPackage(intent, resolvedType, flags2, pkg.services, userId);
            }
            return Collections.emptyList();
        }
    }

    public ParceledListSlice<ResolveInfo> queryIntentContentProviders(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice<>(queryIntentContentProvidersInternal(intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentContentProvidersInternal(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int flags2 = updateFlagsForResolve(flags, userId, intent);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ProviderInfo pi = getProviderInfo(comp, flags2, userId);
            if (pi != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.providerInfo = pi;
                list.add(ri);
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                return this.mProviders.queryIntent(intent, resolvedType, flags2, userId);
            }
            PackageParser.Package pkg = this.mPackages.get(pkgName);
            if (pkg != null) {
                return this.mProviders.queryIntentForPackage(intent, resolvedType, flags2, pkg.providers, userId);
            }
            return Collections.emptyList();
        }
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) {
        ArrayList<PackageInfo> list;
        ParceledListSlice<PackageInfo> parceledListSlice;
        PackageInfo pi;
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        int flags2 = updateFlagsForPackage(flags, userId, null);
        boolean listUninstalled = (flags2 & 8192) != 0;
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "get installed packages");
        synchronized (this.mPackages) {
            if (listUninstalled) {
                list = new ArrayList<>(this.mSettings.mPackages.size());
                for (PackageSetting ps : this.mSettings.mPackages.values()) {
                    if (ps.pkg != null) {
                        pi = generatePackageInfo(ps, flags2, userId);
                    } else {
                        pi = generatePackageInfo(ps, flags2, userId);
                    }
                    if (pi != null) {
                        list.add(pi);
                    }
                }
            } else {
                list = new ArrayList<>(this.mPackages.size());
                for (PackageParser.Package p : this.mPackages.values()) {
                    PackageInfo pi2 = generatePackageInfo((PackageSetting) p.mExtras, flags2, userId);
                    if (pi2 != null) {
                        list.add(pi2);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice<>(list);
        }
        return parceledListSlice;
    }

    private void addPackageHoldingPermissions(ArrayList<PackageInfo> list, PackageSetting ps, String[] permissions, boolean[] tmp, int flags, int userId) {
        PackageInfo pi;
        int numMatch = 0;
        PermissionsState permissionsState = ps.getPermissionsState();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (permissionsState.hasPermission(permission, userId)) {
                tmp[i] = true;
                numMatch++;
            } else {
                tmp[i] = false;
            }
        }
        if (numMatch == 0) {
            return;
        }
        if (ps.pkg != null) {
            pi = generatePackageInfo(ps, flags, userId);
        } else {
            pi = generatePackageInfo(ps, flags, userId);
        }
        if (pi == null) {
            return;
        }
        if ((flags & 4096) == 0) {
            if (numMatch == permissions.length) {
                pi.requestedPermissions = permissions;
            } else {
                pi.requestedPermissions = new String[numMatch];
                int numMatch2 = 0;
                for (int i2 = 0; i2 < permissions.length; i2++) {
                    if (tmp[i2]) {
                        pi.requestedPermissions[numMatch2] = permissions[i2];
                        numMatch2++;
                    }
                }
            }
        }
        list.add(pi);
    }

    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags, int userId) {
        ParceledListSlice<PackageInfo> parceledListSlice;
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        int flags2 = updateFlagsForPackage(flags, userId, permissions);
        boolean listUninstalled = (flags2 & 8192) != 0;
        synchronized (this.mPackages) {
            ArrayList<PackageInfo> list = new ArrayList<>();
            boolean[] tmpBools = new boolean[permissions.length];
            if (listUninstalled) {
                Iterator ps$iterator = this.mSettings.mPackages.values().iterator();
                while (ps$iterator.hasNext()) {
                    addPackageHoldingPermissions(list, (PackageSetting) ps$iterator.next(), permissions, tmpBools, flags2, userId);
                }
            } else {
                for (PackageParser.Package pkg : this.mPackages.values()) {
                    PackageSetting ps = (PackageSetting) pkg.mExtras;
                    if (ps != null) {
                        addPackageHoldingPermissions(list, ps, permissions, tmpBools, flags2, userId);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice<>(list);
        }
        return parceledListSlice;
    }

    public ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        ArrayList<ApplicationInfo> list;
        ApplicationInfo ai;
        ParceledListSlice<ApplicationInfo> parceledListSlice;
        ApplicationInfo ai2;
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        int flags2 = updateFlagsForApplication(flags, userId, null);
        boolean listUninstalled = (flags2 & 8192) != 0;
        synchronized (this.mPackages) {
            if (listUninstalled) {
                list = new ArrayList<>(this.mSettings.mPackages.size());
                for (PackageSetting ps : this.mSettings.mPackages.values()) {
                    if (ps.pkg != null) {
                        ai2 = PackageParser.generateApplicationInfo(ps.pkg, flags2, ps.readUserState(userId), userId);
                    } else {
                        ai2 = generateApplicationInfoFromSettingsLPw(ps.name, flags2, userId);
                    }
                    if (ai2 != null && (!isVendorApp(ai2) || ps.getInstalled(userId))) {
                        list.add(ai2);
                    }
                }
            } else {
                list = new ArrayList<>(this.mPackages.size());
                for (PackageParser.Package p : this.mPackages.values()) {
                    if (p.mExtras != null && (ai = PackageParser.generateApplicationInfo(p, flags2, ((PackageSetting) p.mExtras).readUserState(userId), userId)) != null) {
                        list.add(ai);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice<>(list);
        }
        return parceledListSlice;
    }

    public ParceledListSlice<EphemeralApplicationInfo> getEphemeralApplications(int userId) {
        return null;
    }

    public boolean isEphemeralApplication(String packageName, int userId) {
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "isEphemeral");
        return false;
    }

    public byte[] getEphemeralApplicationCookie(String packageName, int userId) {
        return null;
    }

    public boolean setEphemeralApplicationCookie(String packageName, byte[] cookie, int userId) {
        return true;
    }

    public Bitmap getEphemeralApplicationIcon(String packageName, int userId) {
        return null;
    }

    private boolean isCallerSameApp(String packageName) {
        PackageParser.Package pkg = this.mPackages.get(packageName);
        return pkg != null && UserHandle.getAppId(Binder.getCallingUid()) == pkg.applicationInfo.uid;
    }

    public ParceledListSlice<ApplicationInfo> getPersistentApplications(int flags) {
        return new ParceledListSlice<>(getPersistentApplicationsInternal(flags));
    }

    private List<ApplicationInfo> getPersistentApplicationsInternal(int flags) {
        boolean zIsDirectBootAware;
        ApplicationInfo ai;
        ArrayList<ApplicationInfo> finalList = new ArrayList<>();
        synchronized (this.mPackages) {
            int userId = UserHandle.getCallingUserId();
            for (PackageParser.Package p : this.mPackages.values()) {
                if (p.applicationInfo != null) {
                    boolean matchesUnaware = ((262144 & flags) == 0 || p.applicationInfo.isDirectBootAware()) ? false : true;
                    if ((524288 & flags) == 0) {
                        zIsDirectBootAware = false;
                    } else {
                        zIsDirectBootAware = p.applicationInfo.isDirectBootAware();
                    }
                    if ((p.applicationInfo.flags & 8) != 0 && (!this.mSafeMode || isSystemApp(p))) {
                        if (matchesUnaware || zIsDirectBootAware) {
                            PackageSetting ps = this.mSettings.mPackages.get(p.packageName);
                            if (ps != null && (ai = PackageParser.generateApplicationInfo(p, flags, ps.readUserState(userId), userId)) != null) {
                                if (p.packageName.equals("com.android.phone")) {
                                    finalList.add(0, ai);
                                } else {
                                    finalList.add(ai);
                                }
                            }
                        }
                    }
                }
            }
        }
        return finalList;
    }

    public ProviderInfo resolveContentProvider(String name, int flags, int userId) {
        PackageSetting packageSetting;
        ProviderInfo providerInfoGenerateProviderInfo = null;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int flags2 = updateFlagsForComponent(flags, userId, name);
        synchronized (this.mPackages) {
            PackageParser.Provider provider = this.mProvidersByAuthority.get(name);
            if (provider != null) {
                packageSetting = this.mSettings.mPackages.get(provider.owner.packageName);
            } else {
                packageSetting = null;
            }
            if (packageSetting != null && this.mSettings.isEnabledAndMatchLPr(provider.info, flags2, userId)) {
                providerInfoGenerateProviderInfo = PackageParser.generateProviderInfo(provider, flags2, packageSetting.readUserState(userId), userId);
            }
        }
        return providerInfoGenerateProviderInfo;
    }

    @Deprecated
    public void querySyncProviders(List<String> outNames, List<ProviderInfo> outInfo) {
        synchronized (this.mPackages) {
            int userId = UserHandle.getCallingUserId();
            for (Map.Entry<String, PackageParser.Provider> entry : this.mProvidersByAuthority.entrySet()) {
                PackageParser.Provider p = entry.getValue();
                PackageSetting ps = this.mSettings.mPackages.get(p.owner.packageName);
                if (ps != null && p.syncable && (!this.mSafeMode || (p.info.applicationInfo.flags & 1) != 0)) {
                    ProviderInfo info = PackageParser.generateProviderInfo(p, 0, ps.readUserState(userId), userId);
                    if (info != null) {
                        outNames.add(entry.getKey());
                        outInfo.add(info);
                    }
                }
            }
        }
    }

    public ParceledListSlice<ProviderInfo> queryContentProviders(String processName, int uid, int flags) throws Throwable {
        Iterator<PackageParser.Provider> i;
        ArrayList<ProviderInfo> finalList;
        int userId = processName != null ? UserHandle.getUserId(uid) : UserHandle.getCallingUserId();
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        int flags2 = updateFlagsForComponent(flags, userId, processName);
        ArrayList<ProviderInfo> finalList2 = null;
        synchronized (this.mPackages) {
            try {
                i = this.mProviders.mProviders.values().iterator();
            } catch (Throwable th) {
                th = th;
            }
            while (true) {
                try {
                    finalList = finalList2;
                    if (!i.hasNext()) {
                        break;
                    }
                    PackageParser.Provider p = i.next();
                    PackageSetting ps = this.mSettings.mPackages.get(p.owner.packageName);
                    if (ps == null || p.info.authority == null || !(processName == null || (p.info.processName.equals(processName) && UserHandle.isSameApp(p.info.applicationInfo.uid, uid)))) {
                        finalList2 = finalList;
                    } else if (this.mSettings.isEnabledAndMatchLPr(p.info, flags2, userId)) {
                        finalList2 = finalList == null ? new ArrayList<>(3) : finalList;
                        ProviderInfo info = PackageParser.generateProviderInfo(p, flags2, ps.readUserState(userId), userId);
                        if (info != null) {
                            finalList2.add(info);
                        }
                    } else {
                        finalList2 = finalList;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                throw th;
            }
            if (finalList != null) {
                Collections.sort(finalList, mProviderInitOrderSorter);
                return new ParceledListSlice<>(finalList);
            }
            return ParceledListSlice.emptyList();
        }
    }

    public InstrumentationInfo getInstrumentationInfo(ComponentName name, int flags) {
        InstrumentationInfo instrumentationInfoGenerateInstrumentationInfo;
        synchronized (this.mPackages) {
            PackageParser.Instrumentation i = this.mInstrumentation.get(name);
            instrumentationInfoGenerateInstrumentationInfo = PackageParser.generateInstrumentationInfo(i, flags);
        }
        return instrumentationInfoGenerateInstrumentationInfo;
    }

    public ParceledListSlice<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return new ParceledListSlice<>(queryInstrumentationInternal(targetPackage, flags));
    }

    private List<InstrumentationInfo> queryInstrumentationInternal(String targetPackage, int flags) {
        ArrayList<InstrumentationInfo> finalList = new ArrayList<>();
        synchronized (this.mPackages) {
            for (PackageParser.Instrumentation p : this.mInstrumentation.values()) {
                if (targetPackage == null || targetPackage.equals(p.info.targetPackage)) {
                    InstrumentationInfo ii = PackageParser.generateInstrumentationInfo(p, flags);
                    if (ii != null) {
                        finalList.add(ii);
                    }
                }
            }
        }
        return finalList;
    }

    private void createIdmapsForPackageLI(PackageParser.Package pkg) {
        ArrayMap<String, PackageParser.Package> overlays = this.mOverlays.get(pkg.packageName);
        if (overlays == null) {
            Slog.w(TAG, "Unable to create idmap for " + pkg.packageName + ": no overlay packages");
            return;
        }
        for (PackageParser.Package opkg : overlays.values()) {
            createIdmapForPackagePairLI(pkg, opkg);
        }
    }

    private boolean createIdmapForPackagePairLI(PackageParser.Package pkg, PackageParser.Package opkg) {
        int i = 0;
        if (!opkg.mTrustedOverlay) {
            Slog.w(TAG, "Skipping target and overlay pair " + pkg.baseCodePath + " and " + opkg.baseCodePath + ": overlay not trusted");
            return false;
        }
        ArrayMap<String, PackageParser.Package> overlaySet = this.mOverlays.get(pkg.packageName);
        if (overlaySet == null) {
            Slog.e(TAG, "was about to create idmap for " + pkg.baseCodePath + " and " + opkg.baseCodePath + " but target package has no known overlays");
            return false;
        }
        int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
        try {
            this.mInstaller.idmap(pkg.baseCodePath, opkg.baseCodePath, sharedGid);
            PackageParser.Package[] overlayArray = (PackageParser.Package[]) overlaySet.values().toArray(new PackageParser.Package[0]);
            Comparator<PackageParser.Package> cmp = new Comparator<PackageParser.Package>() {
                @Override
                public int compare(PackageParser.Package p1, PackageParser.Package p2) {
                    return p1.mOverlayPriority - p2.mOverlayPriority;
                }
            };
            Arrays.sort(overlayArray, cmp);
            pkg.applicationInfo.resourceDirs = new String[overlayArray.length];
            int length = overlayArray.length;
            int i2 = 0;
            while (i < length) {
                PackageParser.Package p = overlayArray[i];
                pkg.applicationInfo.resourceDirs[i2] = p.baseCodePath;
                i++;
                i2++;
            }
            return true;
        } catch (InstallerConnection.InstallerException e) {
            Slog.e(TAG, "Failed to generate idmap for " + pkg.baseCodePath + " and " + opkg.baseCodePath);
            return false;
        }
    }

    private void scanDirTracedLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        Trace.traceBegin(1048576L, "scanDir");
        try {
            scanDirLI(dir, parseFlags, scanFlags, currentTime);
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private void scanDirLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        File[] files = dir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            Log.d(TAG, "No files in app dir " + dir);
            return;
        }
        addBootEvent("Android:PMS_scan_data:" + dir.getPath().toString());
        if (DEBUG_PACKAGE_SCANNING) {
            Log.d(TAG, "Scanning app dir " + dir + " scanFlags=" + scanFlags + " flags=0x" + Integer.toHexString(parseFlags));
        }
        int i = 0;
        int length = files.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            File file = files[i2];
            boolean isPackage = (PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
            if (isPackage) {
                long startScanTime = SystemClock.uptimeMillis();
                Slog.d(TAG, "scan package: " + file.toString() + " , start at: " + startScanTime + "ms.");
                try {
                    scanPackageTracedLI(file, parseFlags | 4, scanFlags, currentTime, (UserHandle) null);
                } catch (PackageManagerException e) {
                    Slog.w(TAG, "Failed to parse " + file + ": " + e.getMessage());
                    if ((parseFlags & 1) == 0 && e.error == -2) {
                        logCriticalInfo(5, "Deleting invalid package at " + file);
                        removeCodePathLI(file);
                    }
                }
                long endScanTime = SystemClock.uptimeMillis();
                Slog.d(TAG, "scan package: " + file.toString() + " , end at: " + endScanTime + "ms. elapsed time = " + (endScanTime - startScanTime) + "ms.");
            }
            i = i2 + 1;
        }
    }

    private static File getSettingsProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        File fname = new File(systemDir, "uiderrors.txt");
        return fname;
    }

    static void reportSettingsProblem(int priority, String msg) {
        logCriticalInfo(priority, msg);
    }

    static void logCriticalInfo(int priority, String msg) {
        Slog.println(priority, TAG, msg);
        EventLogTags.writePmCriticalInfo(msg);
        try {
            File fname = getSettingsProblemFile();
            FileOutputStream out = new FileOutputStream(fname, true);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(out);
            SimpleDateFormat formatter = new SimpleDateFormat();
            String dateString = formatter.format(new Date(System.currentTimeMillis()));
            fastPrintWriter.println(dateString + ": " + msg);
            fastPrintWriter.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    private void collectCertificatesLI(PackageSetting ps, PackageParser.Package pkg, File srcFile, int policyFlags) throws PackageManagerException {
        ArraySet<PublicKey> signingKs;
        if (ps != null && ps.codePath.equals(srcFile) && ps.timeStamp == srcFile.lastModified() && !isCompatSignatureUpdateNeeded(pkg) && !isRecoverSignatureUpdateNeeded(pkg)) {
            long mSigningKeySetId = ps.keySetData.getProperSigningKeySet();
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            synchronized (this.mPackages) {
                signingKs = ksms.getPublicKeysFromKeySetLPr(mSigningKeySetId);
            }
            if (ps.signatures.mSignatures != null && ps.signatures.mSignatures.length != 0 && signingKs != null) {
                pkg.mSignatures = ps.signatures.mSignatures;
                pkg.mSigningKeys = signingKs;
                return;
            }
            Slog.w(TAG, "PackageSetting for " + ps.name + " is missing signatures.  Collecting certs again to recover them.");
        } else {
            Log.i(TAG, srcFile.toString() + " changed; collecting certs");
        }
        try {
            PackageParser.collectCertificates(pkg, policyFlags);
        } catch (PackageParser.PackageParserException e) {
            throw PackageManagerException.from(e);
        }
    }

    private PackageParser.Package scanPackageTracedLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        Trace.traceBegin(1048576L, "scanPackage");
        try {
            return scanPackageLI(scanFile, parseFlags, scanFlags, currentTime, user);
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private PackageParser.Package scanPackageLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Parsing: " + scanFile);
        }
        PackageParser pp = new PackageParser();
        pp.setSeparateProcesses(this.mSeparateProcesses);
        pp.setOnlyCoreApps(this.mOnlyCore);
        pp.setDisplayMetrics(this.mMetrics);
        if ((scanFlags & 512) != 0) {
            parseFlags |= 512;
        }
        Trace.traceBegin(1048576L, "parsePackage");
        try {
            try {
                PackageParser.Package pkg = pp.parsePackage(scanFile, parseFlags);
                Trace.traceEnd(1048576L);
                return scanPackageLI(pkg, scanFile, parseFlags, scanFlags, currentTime, user);
            } catch (PackageParser.PackageParserException e) {
                throw PackageManagerException.from(e);
            }
        } catch (Throwable th) {
            Trace.traceEnd(1048576L);
            throw th;
        }
    }

    private PackageParser.Package scanPackageLI(PackageParser.Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws Throwable {
        if ((32768 & scanFlags) == 0) {
            if (pkg.childPackages != null && pkg.childPackages.size() > 0) {
                scanFlags |= DumpState.DUMP_VERSION;
            }
        } else {
            scanFlags &= -32769;
        }
        PackageParser.Package scannedPkg = scanPackageInternalLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPackage = (PackageParser.Package) pkg.childPackages.get(i);
            scanPackageInternalLI(childPackage, scanFile, policyFlags, scanFlags, currentTime, user);
        }
        if ((32768 & scanFlags) != 0) {
            return scanPackageLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user);
        }
        return scannedPkg;
    }

    private PackageParser.Package scanPackageInternalLI(PackageParser.Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws Throwable {
        Throwable th;
        PackageSetting disabledPs;
        PackageSetting ps = null;
        synchronized (this.mPackages) {
            String oldName = this.mSettings.mRenamedPackages.get(pkg.packageName);
            if (pkg.mOriginalPackages != null && pkg.mOriginalPackages.contains(oldName)) {
                ps = this.mSettings.peekPackageLPr(oldName);
            }
            if (ps == null) {
                ps = this.mSettings.peekPackageLPr(pkg.packageName);
            }
            PackageSetting updatedPkg = this.mSettings.getDisabledSystemPkgLPr(ps != null ? ps.name : pkg.packageName);
            if (DEBUG_INSTALL && updatedPkg != null) {
                Slog.d(TAG, "updatedPkg = " + updatedPkg);
            }
            if (!isFirstBoot() && ((isVendorApp(pkg) || isSystemApp(pkg)) && (updatedPkg != null || ps.getInstallStatus() == 0))) {
                Slog.d(TAG, "Skip scanning " + scanFile.toString() + ", pacakge " + updatedPkg + ", install status:  " + ps.getInstallStatus());
                return null;
            }
            if (ps == null && updatedPkg != null) {
                Slog.d(TAG, "Skip scanning uninstalled package: " + pkg.packageName);
                return null;
            }
            if ((policyFlags & 1) != 0 && (disabledPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName)) != null) {
                int scannedChildCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
                int disabledChildCount = disabledPs.childPackageNames != null ? disabledPs.childPackageNames.size() : 0;
                for (int i = 0; i < disabledChildCount; i++) {
                    String disabledChildPackageName = disabledPs.childPackageNames.get(i);
                    boolean disabledPackageAvailable = false;
                    int j = 0;
                    while (true) {
                        if (j >= scannedChildCount) {
                            break;
                        }
                        PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(j);
                        if (childPkg.packageName.equals(disabledChildPackageName)) {
                            disabledPackageAvailable = true;
                            break;
                        }
                        j++;
                    }
                    if (!disabledPackageAvailable) {
                        this.mSettings.removeDisabledSystemPackageLPw(disabledChildPackageName);
                    }
                }
            }
            boolean updatedPkgBetter = false;
            if (updatedPkg != null && ((policyFlags & 1) != 0 || (policyFlags & 8192) != 0)) {
                if (locationIsPrivileged(scanFile)) {
                    updatedPkg.pkgPrivateFlags |= 8;
                } else {
                    updatedPkg.pkgPrivateFlags &= -9;
                }
                if (ps != null && !ps.codePath.equals(scanFile)) {
                    if (DEBUG_INSTALL) {
                        Slog.d(TAG, "Path changing from " + ps.codePath);
                    }
                    if (pkg.mVersionCode < ps.versionCode || (policyFlags & 8192) != 0) {
                        if (DEBUG_INSTALL) {
                            Slog.i(TAG, "Package " + ps.name + " at " + scanFile + " ignored: updated version " + ps.versionCode + " better than this " + pkg.mVersionCode);
                        }
                        if (!updatedPkg.codePath.equals(scanFile)) {
                            Slog.w(TAG, "Code path for hidden system pkg " + ps.name + " changing from " + updatedPkg.codePathString + " to " + scanFile);
                            updatedPkg.codePath = scanFile;
                            updatedPkg.codePathString = scanFile.toString();
                            updatedPkg.resourcePath = scanFile;
                            updatedPkg.resourcePathString = scanFile.toString();
                        }
                        updatedPkg.pkg = pkg;
                        updatedPkg.versionCode = pkg.mVersionCode;
                        int childCount = updatedPkg.childPackageNames != null ? updatedPkg.childPackageNames.size() : 0;
                        for (int i2 = 0; i2 < childCount; i2++) {
                            String childPackageName = updatedPkg.childPackageNames.get(i2);
                            PackageSetting updatedChildPkg = this.mSettings.getDisabledSystemPkgLPr(childPackageName);
                            if (updatedChildPkg != null) {
                                updatedChildPkg.pkg = pkg;
                                updatedChildPkg.versionCode = pkg.mVersionCode;
                            }
                        }
                        throw new PackageManagerException(5, "Package " + ps.name + " at " + scanFile + " ignored: updated version " + ps.versionCode + " better than this " + pkg.mVersionCode);
                    }
                    synchronized (this.mPackages) {
                        this.mPackages.remove(ps.name);
                    }
                    logCriticalInfo(5, "Package " + ps.name + " at " + scanFile + " reverting from " + ps.codePathString + ": new version " + pkg.mVersionCode + " better than installed " + ps.versionCode);
                    InstallArgs args = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, InstructionSets.getAppDexInstructionSets(ps));
                    synchronized (this.mInstallLock) {
                        args.cleanUpResourcesLI();
                    }
                    synchronized (this.mPackages) {
                        this.mSettings.enableSystemPackageLPw(ps.name);
                    }
                    updatedPkgBetter = true;
                }
            }
            if (updatedPkg != null) {
                if (isSystemApp(updatedPkg)) {
                    policyFlags |= 1;
                }
                if ((isVendorApp(updatedPkg) || locationIsOperator(updatedPkg.codePath)) && locationIsOperator(ps.codePath)) {
                    policyFlags |= 8192;
                }
                if ((updatedPkg.pkgPrivateFlags & 8) != 0) {
                    policyFlags |= 128;
                }
            }
            collectCertificatesLI(ps, pkg, scanFile, policyFlags);
            boolean shouldHideSystemApp = false;
            if (updatedPkg == null && ps != null && (policyFlags & 64) != 0 && !isSystemApp(ps)) {
                if (compareSignatures(ps.signatures.mSignatures, pkg.mSignatures) != 0) {
                    logCriticalInfo(5, "Package " + ps.name + " appeared on system, but signatures don't match existing userdata copy; removing");
                    Throwable th2 = null;
                    PackageFreezer freezer = null;
                    try {
                        freezer = freezePackage(pkg.packageName, "scanPackageInternalLI");
                        deletePackageLIF(pkg.packageName, null, true, null, 0, null, false, null);
                        if (freezer != null) {
                            try {
                                freezer.close();
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        }
                        if (th2 != null) {
                            throw th2;
                        }
                        ps = null;
                    } catch (Throwable th4) {
                        th = th4;
                        th = null;
                        if (freezer != null) {
                        }
                        if (th == null) {
                        }
                    }
                } else if (pkg.mVersionCode <= ps.versionCode) {
                    shouldHideSystemApp = true;
                    logCriticalInfo(4, "Package " + ps.name + " appeared at " + scanFile + " but new version " + pkg.mVersionCode + " better than installed " + ps.versionCode + "; hiding system");
                } else {
                    logCriticalInfo(5, "Package " + ps.name + " at " + scanFile + " reverting from " + ps.codePathString + ": new version " + pkg.mVersionCode + " better than installed " + ps.versionCode);
                    InstallArgs args2 = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, InstructionSets.getAppDexInstructionSets(ps));
                    synchronized (this.mInstallLock) {
                        args2.cleanUpResourcesLI();
                    }
                }
            }
            if ((policyFlags & 64) == 0 && ps != null && !ps.codePath.equals(ps.resourcePath)) {
                policyFlags |= 16;
            }
            String resourcePath = null;
            String baseResourcePath = null;
            if ((policyFlags & 16) == 0 || updatedPkgBetter) {
                resourcePath = pkg.codePath;
                baseResourcePath = pkg.baseCodePath;
            } else if (ps == null || ps.resourcePathString == null) {
                Slog.e(TAG, "Resource path not set for package " + pkg.packageName);
            } else {
                resourcePath = ps.resourcePathString;
                baseResourcePath = ps.resourcePathString;
            }
            pkg.setApplicationVolumeUuid(pkg.volumeUuid);
            pkg.setApplicationInfoCodePath(pkg.codePath);
            pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
            pkg.setApplicationInfoResourcePath(resourcePath);
            pkg.setApplicationInfoBaseResourcePath(baseResourcePath);
            pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
            PackageParser.Package scannedPkg = scanPackageLI(pkg, policyFlags, scanFlags | 8, currentTime, user);
            if (shouldHideSystemApp) {
                synchronized (this.mPackages) {
                    this.mSettings.disableSystemPackageLPw(pkg.packageName, true);
                }
            }
            return scannedPkg;
        }
    }

    private static String fixProcessName(String defProcessName, String processName, int uid) {
        if (processName == null) {
            return defProcessName;
        }
        return processName;
    }

    private void verifySignaturesLP(PackageSetting pkgSetting, PackageParser.Package pkg) throws PackageManagerException {
        if (pkgSetting.signatures.mSignatures != null) {
            boolean match = compareSignatures(pkgSetting.signatures.mSignatures, pkg.mSignatures) == 0;
            if (!match) {
                match = compareSignaturesCompat(pkgSetting.signatures, pkg) == 0;
                if (DEBUG_SETTINGS) {
                    Log.i(TAG, "pkgSetting.sig  = " + pkgSetting.sharedUser.signatures.mSignatures[0].toCharsString());
                    Log.i(TAG, "pkg.mSignatures = " + pkg.mSignatures[0].toCharsString());
                }
            }
            if (!match) {
                match = compareSignaturesRecover(pkgSetting.signatures, pkg) == 0;
            }
            if (!match) {
                throw new PackageManagerException(-7, "Package " + pkg.packageName + " signatures do not match the previously installed version; ignoring!");
            }
        }
        if (pkgSetting.sharedUser == null || pkgSetting.sharedUser.signatures.mSignatures == null) {
            return;
        }
        boolean match2 = compareSignatures(pkgSetting.sharedUser.signatures.mSignatures, pkg.mSignatures) == 0;
        if (!match2) {
            match2 = compareSignaturesCompat(pkgSetting.sharedUser.signatures, pkg) == 0;
            if (DEBUG_SETTINGS) {
                Log.i(TAG, "pkgSetting.sig  = " + pkgSetting.sharedUser.signatures.mSignatures[0].toCharsString());
                Log.i(TAG, "pkg.mSignatures = " + pkg.mSignatures[0].toCharsString());
            }
        }
        if (!match2) {
            match2 = compareSignaturesRecover(pkgSetting.sharedUser.signatures, pkg) == 0;
        }
        if (!match2) {
            throw new PackageManagerException(-8, "Package " + pkg.packageName + " has no signatures that match those in shared user " + pkgSetting.sharedUser.name + "; ignoring!");
        }
    }

    private static final void enforceSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (uid == 1000 || uid == 0) {
        } else {
            throw new SecurityException(message);
        }
    }

    public void performFstrimIfNeeded() {
        enforceSystemOrRoot("Only the system can request fstrim");
        try {
            IMountService ms = PackageHelper.getMountService();
            if (ms != null) {
                boolean isUpgrade = isUpgrade();
                boolean doTrim = isUpgrade;
                if (isUpgrade) {
                    Slog.w(TAG, "Running disk maintenance immediately due to system update");
                } else {
                    long interval = Settings.Global.getLong(this.mContext.getContentResolver(), "fstrim_mandatory_interval", DEFAULT_MANDATORY_FSTRIM_INTERVAL);
                    if (interval > 0) {
                        long timeSinceLast = System.currentTimeMillis() - ms.lastMaintenance();
                        if (timeSinceLast > interval) {
                            doTrim = true;
                            Slog.w(TAG, "No disk maintenance in " + timeSinceLast + "; running immediately");
                        }
                    }
                }
                if (!doTrim) {
                    return;
                }
                if (!isFirstBoot()) {
                    try {
                        ActivityManagerNative.getDefault().showBootMessage(this.mContext.getResources().getString(R.string.error_handwriting_unsupported_password), true);
                    } catch (RemoteException e) {
                    }
                }
                ms.runMaintenance();
                return;
            }
            Slog.e(TAG, "Mount service unavailable!");
        } catch (RemoteException e2) {
        }
    }

    public void updatePackagesIfNeeded() {
        List<PackageParser.Package> pkgs;
        enforceSystemOrRoot("Only the system can request package update");
        boolean causeUpgrade = isUpgrade();
        boolean z = !isFirstBoot() ? this.mIsPreNUpgrade : true;
        boolean causePrunedCache = VMRuntime.didPruneDalvikCache();
        if (!causeUpgrade && !z && !causePrunedCache) {
            return;
        }
        synchronized (this.mPackages) {
            pkgs = PackageManagerServiceUtils.getPackagesForDexopt(this.mPackages.values(), this);
        }
        long startTime = System.nanoTime();
        int[] stats = performDexOpt(pkgs, this.mIsPreNUpgrade, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(z ? 0 : 1));
        int elapsedTimeSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_dexopted", stats[0]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_skipped", stats[1]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_failed", stats[2]);
        MetricsLogger.histogram(this.mContext, "opt_dialog_num_total", getOptimizablePackages().size());
        MetricsLogger.histogram(this.mContext, "opt_dialog_time_s", elapsedTimeSeconds);
    }

    private int[] performDexOpt(List<PackageParser.Package> pkgs, boolean showDialog, String compilerFilter) {
        int numberOfPackagesVisited = 0;
        int numberOfPackagesOptimized = 0;
        int numberOfPackagesSkipped = 0;
        int numberOfPackagesFailed = 0;
        int numberOfPackagesToDexopt = pkgs.size();
        for (PackageParser.Package pkg : pkgs) {
            numberOfPackagesVisited++;
            if (!PackageDexOptimizer.canOptimizePackage(pkg)) {
                if (DEBUG_DEXOPT) {
                    Log.i(TAG, "Skipping update of of non-optimizable app " + pkg.packageName);
                }
                numberOfPackagesSkipped++;
            } else {
                if (DEBUG_DEXOPT) {
                    Log.i(TAG, "Updating app " + numberOfPackagesVisited + " of " + numberOfPackagesToDexopt + ": " + pkg.packageName);
                }
                if (showDialog) {
                    try {
                        ActivityManagerNative.getDefault().showBootMessage(this.mContext.getResources().getString(R.string.etws_primary_default_message_earthquake_and_tsunami, Integer.valueOf(numberOfPackagesVisited), Integer.valueOf(numberOfPackagesToDexopt)), true);
                    } catch (RemoteException e) {
                    }
                }
                int dexOptStatus = performDexOptTraced(pkg.packageName, false, compilerFilter, false);
                switch (dexOptStatus) {
                    case -1:
                        numberOfPackagesFailed++;
                        break;
                    case 0:
                        numberOfPackagesSkipped++;
                        break;
                    case 1:
                        numberOfPackagesOptimized++;
                        break;
                    default:
                        Log.e(TAG, "Unexpected dexopt return code " + dexOptStatus);
                        break;
                }
            }
        }
        return new int[]{numberOfPackagesOptimized, numberOfPackagesSkipped, numberOfPackagesFailed};
    }

    public void notifyPackageUse(String packageName, int reason) {
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p == null) {
                return;
            }
            p.mLastPackageUsageTimeInMills[reason] = System.currentTimeMillis();
        }
    }

    public boolean performDexOptIfNeeded(String packageName) {
        int dexOptStatus = performDexOptTraced(packageName, false, PackageManagerServiceCompilerMapping.getFullCompilerFilter(), false);
        return dexOptStatus != -1;
    }

    public boolean performDexOpt(String packageName, boolean checkProfiles, int compileReason, boolean force) {
        int dexOptStatus = performDexOptTraced(packageName, checkProfiles, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(compileReason), force);
        return dexOptStatus != -1;
    }

    public boolean performDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force) {
        int dexOptStatus = performDexOptTraced(packageName, checkProfiles, targetCompilerFilter, force);
        return dexOptStatus != -1;
    }

    private int performDexOptTraced(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force) {
        addBootEvent("PMS:performDexOpt:" + packageName);
        Trace.traceBegin(1048576L, "dexopt");
        try {
            return performDexOptInternal(packageName, checkProfiles, targetCompilerFilter, force);
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private int performDexOptInternal(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force) {
        int iPerformDexOptInternalWithDependenciesLI;
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p == null) {
                return -1;
            }
            this.mPackageUsage.write(false);
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mInstallLock) {
                    iPerformDexOptInternalWithDependenciesLI = performDexOptInternalWithDependenciesLI(p, checkProfiles, targetCompilerFilter, force);
                }
                return iPerformDexOptInternalWithDependenciesLI;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    public ArraySet<String> getOptimizablePackages() {
        ArraySet<String> pkgs = new ArraySet<>();
        synchronized (this.mPackages) {
            for (PackageParser.Package p : this.mPackages.values()) {
                if (PackageDexOptimizer.canOptimizePackage(p)) {
                    pkgs.add(p.packageName);
                }
            }
        }
        return pkgs;
    }

    private int performDexOptInternalWithDependenciesLI(PackageParser.Package p, boolean checkProfiles, String targetCompilerFilter, boolean force) {
        PackageDexOptimizer pdo;
        if (force) {
            pdo = new PackageDexOptimizer.ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        } else {
            pdo = this.mPackageDexOptimizer;
        }
        Collection<PackageParser.Package> deps = findSharedNonSystemLibraries(p);
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(p.applicationInfo);
        if (!deps.isEmpty()) {
            for (PackageParser.Package depPackage : deps) {
                pdo.performDexOpt(depPackage, null, instructionSets, false, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(5));
            }
        }
        return pdo.performDexOpt(p, p.usesLibraryFiles, instructionSets, checkProfiles, targetCompilerFilter);
    }

    Collection<PackageParser.Package> findSharedNonSystemLibraries(PackageParser.Package p) {
        if (p.usesLibraries != null || p.usesOptionalLibraries != null) {
            ArrayList<PackageParser.Package> retValue = new ArrayList<>();
            Set<String> collectedNames = new HashSet<>();
            findSharedNonSystemLibrariesRecursive(p, retValue, collectedNames);
            retValue.remove(p);
            return retValue;
        }
        return Collections.emptyList();
    }

    private void findSharedNonSystemLibrariesRecursive(PackageParser.Package p, Collection<PackageParser.Package> collected, Set<String> collectedNames) {
        if (collectedNames.contains(p.packageName)) {
            return;
        }
        collectedNames.add(p.packageName);
        collected.add(p);
        if (p.usesLibraries != null) {
            findSharedNonSystemLibrariesRecursive(p.usesLibraries, collected, collectedNames);
        }
        if (p.usesOptionalLibraries == null) {
            return;
        }
        findSharedNonSystemLibrariesRecursive(p.usesOptionalLibraries, collected, collectedNames);
    }

    private void findSharedNonSystemLibrariesRecursive(Collection<String> libs, Collection<PackageParser.Package> collected, Set<String> collectedNames) {
        for (String libName : libs) {
            PackageParser.Package libPkg = findSharedNonSystemLibrary(libName);
            if (libPkg != null) {
                findSharedNonSystemLibrariesRecursive(libPkg, collected, collectedNames);
            }
        }
    }

    private PackageParser.Package findSharedNonSystemLibrary(String libName) {
        synchronized (this.mPackages) {
            SharedLibraryEntry lib = this.mSharedLibraries.get(libName);
            if (lib == null || lib.apk == null) {
                return null;
            }
            return this.mPackages.get(lib.apk);
        }
    }

    public void shutdown() {
        this.mPackageUsage.write(true);
        if (this.mCtaPermsController == null) {
            return;
        }
        this.mCtaPermsController.shutdown();
    }

    public void dumpProfiles(String packageName) {
        PackageParser.Package pkg;
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid != SHELL_UID && callingUid != 0 && callingUid != pkg.applicationInfo.uid) {
            throw new SecurityException("dumpProfiles");
        }
        synchronized (this.mInstallLock) {
            Trace.traceBegin(1048576L, "dump profiles");
            int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
            try {
                List<String> allCodePaths = pkg.getAllCodePathsExcludingResourceOnly();
                String gid = Integer.toString(sharedGid);
                String codePaths = TextUtils.join(";", allCodePaths);
                this.mInstaller.dumpProfiles(gid, packageName, codePaths);
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, "Failed to dump profiles", e);
            }
            Trace.traceEnd(1048576L);
        }
    }

    public void forceDexOpt(String packageName) {
        PackageParser.Package pkg;
        enforceSystemOrRoot("forceDexOpt");
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        synchronized (this.mInstallLock) {
            Trace.traceBegin(1048576L, "dexopt");
            int res = performDexOptInternalWithDependenciesLI(pkg, false, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(7), true);
            Trace.traceEnd(1048576L);
            if (res != 1) {
                throw new IllegalStateException("Failed to dexopt: " + res);
            }
        }
    }

    private boolean verifyPackageUpdateLPr(PackageSetting oldPkg, PackageParser.Package newPkg) {
        if ((oldPkg.pkgFlags & 1) == 0) {
            Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package not in system partition");
            return false;
        }
        if (this.mPackages.get(oldPkg.name) == null) {
            return true;
        }
        Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package still exists");
        return false;
    }

    void removeCodePathLI(File codePath) {
        if (codePath.isDirectory()) {
            try {
                this.mInstaller.rmPackageDir(codePath.getAbsolutePath());
                return;
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, "Failed to remove code path", e);
                return;
            }
        }
        codePath.delete();
    }

    private int[] resolveUserIds(int userId) {
        return userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
    }

    private void clearAppDataLIF(PackageParser.Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        clearAppDataLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            clearAppDataLeafLIF((PackageParser.Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void clearAppDataLeafLIF(PackageParser.Package pkg, int userId, int flags) {
        PackageSetting ps;
        synchronized (this.mPackages) {
            ps = this.mSettings.mPackages.get(pkg.packageName);
        }
        for (int realUserId : resolveUserIds(userId)) {
            long ceDataInode = ps != null ? ps.getCeDataInode(realUserId) : 0L;
            try {
                this.mInstaller.clearAppData(pkg.volumeUuid, pkg.packageName, realUserId, flags, ceDataInode);
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
        }
    }

    private void destroyAppDataLIF(PackageParser.Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppDataLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            destroyAppDataLeafLIF((PackageParser.Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void destroyAppDataLeafLIF(PackageParser.Package pkg, int userId, int flags) {
        PackageSetting ps;
        synchronized (this.mPackages) {
            ps = this.mSettings.mPackages.get(pkg.packageName);
        }
        for (int realUserId : resolveUserIds(userId)) {
            long ceDataInode = ps != null ? ps.getCeDataInode(realUserId) : 0L;
            try {
                this.mInstaller.destroyAppData(pkg.volumeUuid, pkg.packageName, realUserId, flags, ceDataInode);
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
        }
    }

    private void destroyAppProfilesLIF(PackageParser.Package pkg, int userId) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppProfilesLeafLIF(pkg);
        destroyAppReferenceProfileLeafLIF(pkg, userId, true);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            destroyAppProfilesLeafLIF((PackageParser.Package) pkg.childPackages.get(i));
            destroyAppReferenceProfileLeafLIF((PackageParser.Package) pkg.childPackages.get(i), userId, true);
        }
    }

    private void destroyAppReferenceProfileLeafLIF(PackageParser.Package pkg, int userId, boolean removeBaseMarker) {
        if (pkg.isForwardLocked()) {
            return;
        }
        for (String path : pkg.getAllCodePathsExcludingResourceOnly()) {
            try {
                String useMarker = PackageManagerServiceUtils.realpath(new File(path)).replace('/', '@');
                for (int realUserId : resolveUserIds(userId)) {
                    File profileDir = Environment.getDataProfilesDeForeignDexDirectory(realUserId);
                    if (removeBaseMarker) {
                        File foreignUseMark = new File(profileDir, useMarker);
                        if (foreignUseMark.exists() && !foreignUseMark.delete()) {
                            Slog.w(TAG, "Unable to delete foreign user mark for package: " + pkg.packageName);
                        }
                    }
                    File[] markers = profileDir.listFiles();
                    if (markers != null) {
                        String searchString = "@" + pkg.packageName + "@";
                        for (File marker : markers) {
                            if (marker.getName().indexOf(searchString) > 0 && !marker.delete()) {
                                Slog.w(TAG, "Unable to delete foreign user mark for package: " + pkg.packageName);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed to get canonical path", e);
            }
        }
    }

    private void destroyAppProfilesLeafLIF(PackageParser.Package pkg) {
        try {
            this.mInstaller.destroyAppProfiles(pkg.packageName);
        } catch (InstallerConnection.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    private void clearAppProfilesLIF(PackageParser.Package pkg, int userId) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        clearAppProfilesLeafLIF(pkg);
        destroyAppReferenceProfileLeafLIF(pkg, userId, false);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            clearAppProfilesLeafLIF((PackageParser.Package) pkg.childPackages.get(i));
        }
    }

    private void clearAppProfilesLeafLIF(PackageParser.Package pkg) {
        try {
            this.mInstaller.clearAppProfiles(pkg.packageName);
        } catch (InstallerConnection.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    private void setInstallAndUpdateTime(PackageParser.Package pkg, long firstInstallTime, long lastUpdateTime) {
        PackageSetting ps = (PackageSetting) pkg.mExtras;
        if (ps != null) {
            ps.firstInstallTime = firstInstallTime;
            ps.lastUpdateTime = lastUpdateTime;
        }
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            PackageSetting ps2 = (PackageSetting) childPkg.mExtras;
            if (ps2 != null) {
                ps2.firstInstallTime = firstInstallTime;
                ps2.lastUpdateTime = lastUpdateTime;
            }
        }
    }

    private void addSharedLibraryLPw(ArraySet<String> usesLibraryFiles, SharedLibraryEntry file, PackageParser.Package changingLib) {
        if (file.path != null) {
            usesLibraryFiles.add(file.path);
            return;
        }
        PackageParser.Package p = this.mPackages.get(file.apk);
        if (changingLib != null && changingLib.packageName.equals(file.apk) && (p == null || p.packageName.equals(changingLib.packageName))) {
            p = changingLib;
        }
        if (p == null) {
            return;
        }
        usesLibraryFiles.addAll(p.getAllCodePaths());
    }

    private void updateSharedLibrariesLPw(PackageParser.Package pkg, PackageParser.Package changingLib) throws PackageManagerException {
        if (pkg.usesLibraries == null && pkg.usesOptionalLibraries == null) {
            return;
        }
        ArraySet<String> usesLibraryFiles = new ArraySet<>();
        int N = pkg.usesLibraries != null ? pkg.usesLibraries.size() : 0;
        for (int i = 0; i < N; i++) {
            SharedLibraryEntry file = this.mSharedLibraries.get(pkg.usesLibraries.get(i));
            if (file == null) {
                throw new PackageManagerException(-9, "Package " + pkg.packageName + " requires unavailable shared library " + ((String) pkg.usesLibraries.get(i)) + "; failing!");
            }
            addSharedLibraryLPw(usesLibraryFiles, file, changingLib);
        }
        int N2 = pkg.usesOptionalLibraries != null ? pkg.usesOptionalLibraries.size() : 0;
        for (int i2 = 0; i2 < N2; i2++) {
            SharedLibraryEntry file2 = this.mSharedLibraries.get(pkg.usesOptionalLibraries.get(i2));
            if (file2 == null) {
                Slog.w(TAG, "Package " + pkg.packageName + " desires unavailable shared library " + ((String) pkg.usesOptionalLibraries.get(i2)) + "; ignoring!");
            } else {
                addSharedLibraryLPw(usesLibraryFiles, file2, changingLib);
            }
        }
        int N3 = usesLibraryFiles.size();
        if (N3 > 0) {
            pkg.usesLibraryFiles = (String[]) usesLibraryFiles.toArray(new String[N3]);
        } else {
            pkg.usesLibraryFiles = null;
        }
    }

    private static boolean hasString(List<String> list, List<String> which) {
        if (list == null) {
            return false;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = which.size() - 1; j >= 0; j--) {
                if (which.get(j).equals(list.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateAllSharedLibrariesLPw() {
        for (PackageParser.Package pkg : this.mPackages.values()) {
            try {
                updateSharedLibrariesLPw(pkg, null);
            } catch (PackageManagerException e) {
                Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
            }
        }
    }

    private ArrayList<PackageParser.Package> updateAllSharedLibrariesLPw(PackageParser.Package changingPkg) {
        ArrayList<PackageParser.Package> res = null;
        for (PackageParser.Package pkg : this.mPackages.values()) {
            if (hasString(pkg.usesLibraries, changingPkg.libraryNames) || hasString(pkg.usesOptionalLibraries, changingPkg.libraryNames)) {
                if (res == null) {
                    res = new ArrayList<>();
                }
                res.add(pkg);
                try {
                    updateSharedLibrariesLPw(pkg, changingPkg);
                } catch (PackageManagerException e) {
                    Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
                }
            }
        }
        return res;
    }

    private static String deriveAbiOverride(String abiOverride, PackageSetting settings) {
        if (INSTALL_PACKAGE_SUFFIX.equals(abiOverride)) {
            return null;
        }
        if (abiOverride != null) {
            return abiOverride;
        }
        if (settings == null) {
            return null;
        }
        String cpuAbiOverride = settings.cpuAbiOverrideString;
        return cpuAbiOverride;
    }

    private PackageParser.Package scanPackageTracedLI(PackageParser.Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        Trace.traceBegin(1048576L, "scanPackage");
        if ((32768 & scanFlags) == 0) {
            if (pkg.childPackages != null && pkg.childPackages.size() > 0) {
                scanFlags |= DumpState.DUMP_VERSION;
            }
        } else {
            scanFlags &= -32769;
        }
        try {
            PackageParser.Package scannedPkg = scanPackageLI(pkg, policyFlags, scanFlags, currentTime, user);
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
                scanPackageLI(childPkg, policyFlags, scanFlags, currentTime, user);
            }
            Trace.traceEnd(1048576L);
            if ((32768 & scanFlags) != 0) {
                return scanPackageTracedLI(pkg, policyFlags, scanFlags, currentTime, user);
            }
            return scannedPkg;
        } catch (Throwable th) {
            Trace.traceEnd(1048576L);
            throw th;
        }
    }

    private PackageParser.Package scanPackageLI(PackageParser.Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        boolean success = false;
        try {
            PackageParser.Package res = scanPackageDirtyLI(pkg, policyFlags, scanFlags, currentTime, user);
            success = true;
            return res;
        } finally {
            if (!success && (scanFlags & 1024) != 0) {
                destroyAppDataLIF(pkg, -1, 3);
                destroyAppProfilesLIF(pkg, -1);
            }
        }
    }

    private static boolean apkHasCode(String fileName) throws Throwable {
        StrictJarFile jarFile;
        StrictJarFile jarFile2 = null;
        try {
            jarFile = new StrictJarFile(fileName, false, false);
        } catch (IOException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            boolean z = jarFile.findEntry("classes.dex") != null;
            try {
                jarFile.close();
            } catch (IOException e2) {
            }
            return z;
        } catch (IOException e3) {
            jarFile2 = jarFile;
            try {
                jarFile2.close();
            } catch (IOException e4) {
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
            jarFile2 = jarFile;
            try {
                jarFile2.close();
            } catch (IOException e5) {
            }
            throw th;
        }
    }

    private static void enforceCodePolicy(PackageParser.Package pkg) throws PackageManagerException {
        boolean shouldHaveCode = (pkg.applicationInfo.flags & 4) != 0;
        if (shouldHaveCode && !apkHasCode(pkg.baseCodePath)) {
            throw new PackageManagerException(-2, "Package " + pkg.baseCodePath + " code is missing");
        }
        if (ArrayUtils.isEmpty(pkg.splitCodePaths)) {
            return;
        }
        for (int i = 0; i < pkg.splitCodePaths.length; i++) {
            boolean splitShouldHaveCode = (pkg.splitFlags[i] & 4) != 0;
            if (splitShouldHaveCode && !apkHasCode(pkg.splitCodePaths[i])) {
                throw new PackageManagerException(-2, "Package " + pkg.splitCodePaths[i] + " code is missing");
            }
        }
    }

    private PackageParser.Package scanPackageDirtyLI(final PackageParser.Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        PackageParser.Package r49;
        PackageSetting pkgSetting;
        PackageParser.Provider p;
        PackageSetting oldPs;
        PackageSetting foundPs;
        File scanFile = new File(pkg.codePath);
        if (pkg.applicationInfo.getCodePath() == null || pkg.applicationInfo.getResourcePath() == null) {
            throw new PackageManagerException(-2, "Code and resource paths haven't been set correctly");
        }
        if ((policyFlags & 1) != 0) {
            pkg.applicationInfo.flags |= 1;
            if (pkg.applicationInfo.isDirectBootAware()) {
                for (PackageParser.Service s : pkg.services) {
                    ServiceInfo serviceInfo = s.info;
                    s.info.directBootAware = true;
                    serviceInfo.encryptionAware = true;
                }
                for (PackageParser.Provider p2 : pkg.providers) {
                    ProviderInfo providerInfo = p2.info;
                    p2.info.directBootAware = true;
                    providerInfo.encryptionAware = true;
                }
                for (PackageParser.Activity a : pkg.activities) {
                    ActivityInfo activityInfo = a.info;
                    a.info.directBootAware = true;
                    activityInfo.encryptionAware = true;
                }
                for (PackageParser.Activity r : pkg.receivers) {
                    ActivityInfo activityInfo2 = r.info;
                    r.info.directBootAware = true;
                    activityInfo2.encryptionAware = true;
                }
            }
        } else {
            pkg.coreApp = false;
            pkg.applicationInfo.privateFlags &= -33;
            pkg.applicationInfo.privateFlags &= -65;
        }
        pkg.mTrustedOverlay = (policyFlags & 512) != 0;
        if ((policyFlags & 128) != 0) {
            pkg.applicationInfo.privateFlags |= 8;
        }
        if ((policyFlags & 8192) != 0) {
            pkg.applicationInfo.flagsEx |= 1;
        }
        if ((policyFlags & 1024) != 0) {
            enforceCodePolicy(pkg);
        }
        if (this.mCustomResolverComponentName != null && this.mCustomResolverComponentName.getPackageName().equals(pkg.packageName)) {
            setUpCustomResolverActivity(pkg);
        }
        if (pkg.packageName.equals(PLATFORM_PACKAGE_NAME)) {
            synchronized (this.mPackages) {
                if (this.mAndroidApplication != null) {
                    Slog.w(TAG, "*************************************************");
                    Slog.w(TAG, "Core android package being redefined.  Skipping.");
                    Slog.w(TAG, " file=" + scanFile);
                    Slog.w(TAG, "*************************************************");
                    throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
                }
                if ((32768 & scanFlags) == 0) {
                    this.mPlatformPackage = pkg;
                    pkg.mVersionCode = this.mSdkVersion;
                    this.mAndroidApplication = pkg.applicationInfo;
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
                }
            }
        }
        if (pkg.packageName.equals("com.mediatek")) {
            synchronized (this.mPackages) {
                if (this.mMediatekApplication != null) {
                    Slog.w(TAG, "*************************************************");
                    Slog.w(TAG, "Core mediatek package being redefined.  Skipping.");
                    Slog.w(TAG, " file=" + scanFile);
                    Slog.w(TAG, "*************************************************");
                    throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
                }
                this.mMediatekApplication = pkg.applicationInfo;
            }
        }
        if (DEBUG_PACKAGE_SCANNING && (policyFlags & 2) != 0) {
            Log.d(TAG, "Scanning package " + pkg.packageName);
        }
        synchronized (this.mPackages) {
            if (this.mPackages.containsKey(pkg.packageName) || this.mSharedLibraries.containsKey(pkg.packageName)) {
                throw new PackageManagerException(-5, "Application package " + pkg.packageName + " already installed.  Skipping duplicate.");
            }
            PackageSetting oldPkgSetting = this.mSettings.peekPackageLPr(pkg.packageName);
            r49 = oldPkgSetting == null ? null : oldPkgSetting.pkg;
            if ((scanFlags & 4096) != 0) {
                if (this.mExpectingBetter.containsKey(pkg.packageName)) {
                    logCriticalInfo(5, "Relax SCAN_REQUIRE_KNOWN requirement for package " + pkg.packageName);
                } else {
                    PackageSetting known = this.mSettings.peekPackageLPr(pkg.packageName);
                    if (known != null) {
                        if (DEBUG_PACKAGE_SCANNING) {
                            Log.d(TAG, "Examining " + pkg.codePath + " and requiring known paths " + known.codePathString + " & " + known.resourcePathString);
                        }
                        if (!pkg.applicationInfo.getCodePath().equals(known.codePathString) || !pkg.applicationInfo.getResourcePath().equals(known.resourcePathString)) {
                            throw new PackageManagerException(-23, "Application package " + pkg.packageName + " found at " + pkg.applicationInfo.getCodePath() + " but expected at " + known.codePathString + "; ignoring.");
                        }
                    }
                }
            }
        }
        File destCodeFile = new File(pkg.applicationInfo.getCodePath());
        File destResourceFile = new File(pkg.applicationInfo.getResourcePath());
        SharedUserSetting suid = null;
        if (!isSystemApp(pkg)) {
            pkg.mOriginalPackages = null;
            pkg.mRealPackage = null;
            pkg.mAdoptPermissions = null;
        }
        PackageSetting nonMutatedPs = null;
        synchronized (this.mPackages) {
            if (pkg.mSharedUserId != null) {
                suid = this.mSettings.getSharedUserLPw(pkg.mSharedUserId, 0, 0, true);
                if (suid == null) {
                    throw new PackageManagerException(-4, "Creating application package " + pkg.packageName + " for shared user failed");
                }
                if (DEBUG_PACKAGE_SCANNING && (policyFlags & 2) != 0) {
                    Log.d(TAG, "Shared UserID " + pkg.mSharedUserId + " (uid=" + suid.userId + "): packages=" + suid.packages);
                }
            }
            PackageSetting origPackage = null;
            String realName = null;
            if (pkg.mOriginalPackages != null) {
                String renamed = this.mSettings.mRenamedPackages.get(pkg.mRealPackage);
                if (pkg.mOriginalPackages.contains(renamed)) {
                    realName = pkg.mRealPackage;
                    if (!pkg.packageName.equals(renamed)) {
                        pkg.setPackageName(renamed);
                    }
                } else {
                    int i = pkg.mOriginalPackages.size() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        origPackage = this.mSettings.peekPackageLPr((String) pkg.mOriginalPackages.get(i));
                        if (origPackage != null) {
                            if (!verifyPackageUpdateLPr(origPackage, pkg)) {
                                origPackage = null;
                            } else if (origPackage.sharedUser != null) {
                                if (origPackage.sharedUser.name.equals(pkg.mSharedUserId)) {
                                    break;
                                }
                                Slog.w(TAG, "Unable to migrate data from " + origPackage.name + " to " + pkg.packageName + ": old uid " + origPackage.sharedUser.name + " differs from " + pkg.mSharedUserId);
                                origPackage = null;
                            } else if (DEBUG_UPGRADE) {
                                Log.v(TAG, "Renaming new package " + pkg.packageName + " to old name " + origPackage.name);
                            }
                        }
                        i--;
                    }
                }
            }
            if (this.mTransferedPackages.contains(pkg.packageName)) {
                Slog.w(TAG, "Package " + pkg.packageName + " was transferred to another, but its .apk remains");
            }
            if ((32768 & scanFlags) != 0 && (foundPs = this.mSettings.peekPackageLPr(pkg.packageName)) != null) {
                nonMutatedPs = new PackageSetting(foundPs);
            }
            pkgSetting = this.mSettings.getPackageLPw(pkg, origPackage, realName, suid, destCodeFile, destResourceFile, pkg.applicationInfo.nativeLibraryRootDir, pkg.applicationInfo.primaryCpuAbi, pkg.applicationInfo.secondaryCpuAbi, pkg.applicationInfo.flags, pkg.applicationInfo.privateFlags, pkg.applicationInfo.flagsEx, user, false);
            if (pkgSetting == null) {
                throw new PackageManagerException(-4, "Creating application package " + pkg.packageName + " failed");
            }
            if (pkgSetting.origPackage != null) {
                pkg.setPackageName(origPackage.name);
                String msg = "New package " + pkgSetting.realName + " renamed to replace old package " + pkgSetting.name;
                reportSettingsProblem(5, msg);
                if ((32768 & scanFlags) == 0) {
                    this.mTransferedPackages.add(origPackage.name);
                }
                pkgSetting.origPackage = null;
            }
            if ((32768 & scanFlags) == 0 && realName != null) {
                this.mTransferedPackages.add(pkg.packageName);
            }
            if (this.mSettings.isDisabledSystemPackageLPr(pkg.packageName) && (oldPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName)) != null && !isVendorApp(oldPs)) {
                pkg.applicationInfo.flags |= 128;
            }
            if ((policyFlags & 64) == 0) {
                updateSharedLibrariesLPw(pkg, null);
            }
            if (this.mFoundPolicyFile) {
                SELinuxMMAC.assignSeinfoValue(pkg);
            }
            pkg.applicationInfo.uid = pkgSetting.appId;
            pkg.mExtras = pkgSetting;
            if (!shouldCheckUpgradeKeySetLP(pkgSetting, scanFlags)) {
                try {
                    verifySignaturesLP(pkgSetting, pkg);
                    pkgSetting.signatures.mSignatures = pkg.mSignatures;
                } catch (PackageManagerException e) {
                    if ((policyFlags & 64) == 0 && (policyFlags & 8192) == 0) {
                        throw e;
                    }
                    pkgSetting.signatures.mSignatures = pkg.mSignatures;
                    if (pkgSetting.sharedUser != null && compareSignatures(pkgSetting.sharedUser.signatures.mSignatures, pkg.mSignatures) != 0) {
                        throw new PackageManagerException(-104, "Signature mismatch for shared user: " + pkgSetting.sharedUser);
                    }
                    String msg2 = "System package " + pkg.packageName + " signature changed; retaining data.";
                    reportSettingsProblem(5, msg2);
                }
            } else if (checkUpgradeKeySetLP(pkgSetting, pkg)) {
                pkgSetting.signatures.mSignatures = pkg.mSignatures;
            } else {
                if ((policyFlags & 64) == 0) {
                    throw new PackageManagerException(-7, "Package " + pkg.packageName + " upgrade keys do not match the previously installed version");
                }
                pkgSetting.signatures.mSignatures = pkg.mSignatures;
                String msg3 = "System package " + pkg.packageName + " signature changed; retaining data.";
                reportSettingsProblem(5, msg3);
            }
            if ((scanFlags & 16) != 0) {
                int N = pkg.providers.size();
                for (int i2 = 0; i2 < N; i2++) {
                    PackageParser.Provider p3 = (PackageParser.Provider) pkg.providers.get(i2);
                    if (p3.info.authority != null) {
                        String[] names = p3.info.authority.split(";");
                        for (int j = 0; j < names.length; j++) {
                            if (this.mProvidersByAuthority.containsKey(names[j])) {
                                PackageParser.Provider other = this.mProvidersByAuthority.get(names[j]);
                                String otherPackageName = (other == null || other.getComponentName() == null) ? "?" : other.getComponentName().getPackageName();
                                throw new PackageManagerException(-13, "Can't install because provider name " + names[j] + " (in package " + pkg.applicationInfo.packageName + ") is already used by " + otherPackageName);
                            }
                        }
                    }
                }
            }
            if ((32768 & scanFlags) == 0 && pkg.mAdoptPermissions != null) {
                for (int i3 = pkg.mAdoptPermissions.size() - 1; i3 >= 0; i3--) {
                    String origName = (String) pkg.mAdoptPermissions.get(i3);
                    PackageSetting orig = this.mSettings.peekPackageLPr(origName);
                    if (orig != null && verifyPackageUpdateLPr(orig, pkg)) {
                        Slog.i(TAG, "Adopting permissions from " + origName + " to " + pkg.packageName);
                        this.mSettings.transferPermissionsLPw(origName, pkg.packageName);
                    }
                }
            }
        }
        String pkgName = pkg.packageName;
        long scanFileTime = scanFile.lastModified();
        if ((scanFlags & 4) != 0) {
        }
        pkg.applicationInfo.processName = fixProcessName(pkg.applicationInfo.packageName, pkg.applicationInfo.processName, pkg.applicationInfo.uid);
        if (pkg != this.mPlatformPackage) {
            pkg.applicationInfo.initForUser(0);
        }
        scanFile.getPath();
        String cpuAbiOverride = deriveAbiOverride(pkg.cpuAbiOverride, pkgSetting);
        if ((scanFlags & 16) == 0) {
            derivePackageAbi(pkg, scanFile, cpuAbiOverride, true);
            if (isSystemApp(pkg) && !pkg.isUpdatedSystemApp() && pkg.applicationInfo.primaryCpuAbi == null) {
                setBundledAppAbisAndRoots(pkg, pkgSetting);
                setNativeLibraryPaths(pkg);
            }
        } else {
            if ((scanFlags & 8192) != 0) {
                pkg.applicationInfo.primaryCpuAbi = pkgSetting.primaryCpuAbiString;
                pkg.applicationInfo.secondaryCpuAbi = pkgSetting.secondaryCpuAbiString;
            }
            setNativeLibraryPaths(pkg);
        }
        if (this.mPlatformPackage == pkg) {
            pkg.applicationInfo.primaryCpuAbi = VMRuntime.getRuntime().is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        }
        if ((scanFlags & 2) == 0 && (scanFlags & 16) != 0 && cpuAbiOverride == null && pkgSetting.cpuAbiOverrideString != null) {
            Slog.w(TAG, "Ignoring persisted ABI override " + cpuAbiOverride + " for package " + pkg.packageName);
        }
        pkgSetting.primaryCpuAbiString = pkg.applicationInfo.primaryCpuAbi;
        pkgSetting.secondaryCpuAbiString = pkg.applicationInfo.secondaryCpuAbi;
        pkgSetting.cpuAbiOverrideString = cpuAbiOverride;
        pkg.cpuAbiOverride = cpuAbiOverride;
        if (DEBUG_ABI_SELECTION) {
            Slog.d(TAG, "Resolved nativeLibraryRoot for " + pkg.applicationInfo.packageName + " to root=" + pkg.applicationInfo.nativeLibraryRootDir + ", isa=" + pkg.applicationInfo.nativeLibraryRootRequiresIsa);
        }
        pkgSetting.legacyNativeLibraryPathString = pkg.applicationInfo.nativeLibraryRootDir;
        if (DEBUG_ABI_SELECTION) {
            Slog.d(TAG, "Abis for package[" + pkg.packageName + "] are primary=" + pkg.applicationInfo.primaryCpuAbi + " secondary=" + pkg.applicationInfo.secondaryCpuAbi);
        }
        if ((scanFlags & 256) == 0 && pkgSetting.sharedUser != null) {
            adjustCpuAbisForSharedUserLPw(pkgSetting.sharedUser.packages, pkg, true);
        }
        if (this.mFactoryTest && pkg.requestedPermissions.contains("android.permission.FACTORY_TEST")) {
            pkg.applicationInfo.flags |= 16;
        }
        ArrayList<PackageParser.Package> clientLibPkgs = null;
        if ((32768 & scanFlags) != 0) {
            if (nonMutatedPs != null) {
                synchronized (this.mPackages) {
                    this.mSettings.mPackages.put(nonMutatedPs.name, nonMutatedPs);
                }
            }
            return pkg;
        }
        if (pkg.childPackages != null && !pkg.childPackages.isEmpty()) {
            if ((policyFlags & 128) == 0) {
                throw new PackageManagerException("Only privileged apps and updated privileged apps can add child packages. Ignoring package " + pkg.packageName);
            }
            int childCount = pkg.childPackages.size();
            for (int i4 = 0; i4 < childCount; i4++) {
                PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i4);
                if (this.mSettings.hasOtherDisabledSystemPkgWithChildLPr(pkg.packageName, childPkg.packageName)) {
                    throw new PackageManagerException("Cannot override a child package of another disabled system app. Ignoring package " + pkg.packageName);
                }
            }
        }
        synchronized (this.mPackages) {
            if ((pkg.applicationInfo.flags & 1) != 0 && pkg.libraryNames != null) {
                for (int i5 = 0; i5 < pkg.libraryNames.size(); i5++) {
                    String name = (String) pkg.libraryNames.get(i5);
                    boolean allowed = false;
                    if (pkg.isUpdatedSystemApp()) {
                        PackageSetting sysPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                        if (sysPs.pkg != null && sysPs.pkg.libraryNames != null) {
                            int j2 = 0;
                            while (true) {
                                if (j2 >= sysPs.pkg.libraryNames.size()) {
                                    break;
                                }
                                if (name.equals(sysPs.pkg.libraryNames.get(j2))) {
                                    allowed = true;
                                    break;
                                }
                                j2++;
                            }
                        }
                    } else {
                        allowed = true;
                    }
                    if (!allowed) {
                        Slog.w(TAG, "Package " + pkg.packageName + " declares lib " + name + " that is not declared on system image; skipping");
                    } else if (!this.mSharedLibraries.containsKey(name)) {
                        this.mSharedLibraries.put(name, new SharedLibraryEntry(null, pkg.packageName));
                    } else if (!name.equals(pkg.packageName)) {
                        Slog.w(TAG, "Package " + pkg.packageName + " library " + name + " already exists; skipping");
                    }
                }
                if ((scanFlags & 256) == 0) {
                    clientLibPkgs = updateAllSharedLibrariesLPw(pkg);
                }
            }
        }
        if ((scanFlags & 256) == 0 && (131072 & scanFlags) == 0 && (262144 & scanFlags) == 0) {
            checkPackageFrozen(pkgName);
        }
        if (clientLibPkgs != null) {
            for (int i6 = 0; i6 < clientLibPkgs.size(); i6++) {
                PackageParser.Package clientPkg = clientLibPkgs.get(i6);
                killApplication(clientPkg.applicationInfo.packageName, clientPkg.applicationInfo.uid, "update lib");
            }
        }
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        ksms.assertScannedPackageValid(pkg);
        Trace.traceBegin(1048576L, "updateSettings");
        boolean createIdmapFailed = false;
        synchronized (this.mPackages) {
            if (pkgSetting.pkg != null) {
                PackageParser.Package r4 = pkgSetting.pkg;
                if (user == null) {
                    user = UserHandle.ALL;
                }
                maybeRenameForeignDexMarkers(r4, pkg, user);
            }
            this.mSettings.insertPackageSettingLPw(pkgSetting, pkg);
            this.mPackages.put(pkg.applicationInfo.packageName, pkg);
            Iterator<PackageCleanItem> iter = this.mSettings.mPackagesToBeCleaned.iterator();
            while (iter.hasNext()) {
                PackageCleanItem item = iter.next();
                if (pkgName.equals(item.packageName)) {
                    iter.remove();
                }
            }
            if (currentTime != 0) {
                if (pkgSetting.firstInstallTime == 0) {
                    pkgSetting.lastUpdateTime = currentTime;
                    pkgSetting.firstInstallTime = currentTime;
                } else if ((scanFlags & 64) != 0) {
                    pkgSetting.lastUpdateTime = currentTime;
                }
            } else if (pkgSetting.firstInstallTime == 0) {
                pkgSetting.lastUpdateTime = scanFileTime;
                pkgSetting.firstInstallTime = scanFileTime;
            } else if ((policyFlags & 64) != 0 && scanFileTime != pkgSetting.timeStamp) {
                pkgSetting.lastUpdateTime = scanFileTime;
            }
            ksms.addScannedPackageLPw(pkg);
            int N2 = pkg.providers.size();
            StringBuilder r2 = null;
            for (int i7 = 0; i7 < N2; i7++) {
                PackageParser.Provider p4 = (PackageParser.Provider) pkg.providers.get(i7);
                p4.info.processName = fixProcessName(pkg.applicationInfo.processName, p4.info.processName, pkg.applicationInfo.uid);
                this.mProviders.addProvider(p4);
                p4.syncable = p4.info.isSyncable;
                if (p4.info.authority != null) {
                    String[] names2 = p4.info.authority.split(";");
                    p4.info.authority = null;
                    int j3 = 0;
                    PackageParser.Provider p5 = p4;
                    while (j3 < names2.length) {
                        if (j3 == 1 && p5.syncable) {
                            p = new PackageParser.Provider(p5);
                            p.syncable = false;
                        } else {
                            p = p5;
                        }
                        if (this.mProvidersByAuthority.containsKey(names2[j3])) {
                            PackageParser.Provider other2 = this.mProvidersByAuthority.get(names2[j3]);
                            Slog.w(TAG, "Skipping provider name " + names2[j3] + " (in package " + pkg.applicationInfo.packageName + "): name already used by " + ((other2 == null || other2.getComponentName() == null) ? "?" : other2.getComponentName().getPackageName()));
                        } else {
                            this.mProvidersByAuthority.put(names2[j3], p);
                            if (p.info.authority == null) {
                                p.info.authority = names2[j3];
                            } else {
                                p.info.authority += ";" + names2[j3];
                            }
                            if (DEBUG_PACKAGE_SCANNING && (policyFlags & 2) != 0) {
                                Log.d(TAG, "Registered content provider: " + names2[j3] + ", className = " + p.info.name + ", isSyncable = " + p.info.isSyncable);
                            }
                        }
                        j3++;
                        p5 = p;
                    }
                    p4 = p5;
                }
                if ((policyFlags & 2) != 0) {
                    if (r2 == null) {
                        r2 = new StringBuilder(256);
                    } else {
                        r2.append(' ');
                    }
                    r2.append(p4.info.name);
                }
            }
            if (r2 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Providers: " + ((Object) r2));
            }
            int N3 = pkg.services.size();
            StringBuilder r3 = null;
            for (int i8 = 0; i8 < N3; i8++) {
                PackageParser.Service s2 = (PackageParser.Service) pkg.services.get(i8);
                s2.info.processName = fixProcessName(pkg.applicationInfo.processName, s2.info.processName, pkg.applicationInfo.uid);
                this.mServices.addService(s2);
                if ((policyFlags & 2) != 0) {
                    if (r3 == null) {
                        r3 = new StringBuilder(256);
                    } else {
                        r3.append(' ');
                    }
                    r3.append(s2.info.name);
                }
            }
            if (r3 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Services: " + ((Object) r3));
            }
            int N4 = pkg.receivers.size();
            StringBuilder r5 = null;
            for (int i9 = 0; i9 < N4; i9++) {
                PackageParser.Activity a2 = (PackageParser.Activity) pkg.receivers.get(i9);
                a2.info.processName = fixProcessName(pkg.applicationInfo.processName, a2.info.processName, pkg.applicationInfo.uid);
                this.mReceivers.addActivity(a2, "receiver");
                if ((policyFlags & 2) != 0) {
                    if (r5 == null) {
                        r5 = new StringBuilder(256);
                    } else {
                        r5.append(' ');
                    }
                    r5.append(a2.info.name);
                }
            }
            if (r5 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Receivers: " + ((Object) r5));
            }
            int N5 = pkg.activities.size();
            StringBuilder r6 = null;
            for (int i10 = 0; i10 < N5; i10++) {
                PackageParser.Activity a3 = (PackageParser.Activity) pkg.activities.get(i10);
                a3.info.processName = fixProcessName(pkg.applicationInfo.processName, a3.info.processName, pkg.applicationInfo.uid);
                this.mActivities.addActivity(a3, "activity");
                if ((policyFlags & 2) != 0) {
                    if (r6 == null) {
                        r6 = new StringBuilder(256);
                    } else {
                        r6.append(' ');
                    }
                    r6.append(a3.info.name);
                }
            }
            if (r6 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Activities: " + ((Object) r6));
            }
            int N6 = pkg.permissionGroups.size();
            StringBuilder r7 = null;
            for (int i11 = 0; i11 < N6; i11++) {
                PackageParser.PermissionGroup pg = (PackageParser.PermissionGroup) pkg.permissionGroups.get(i11);
                PackageParser.PermissionGroup cur = this.mPermissionGroups.get(pg.info.name);
                if (cur == null) {
                    this.mPermissionGroups.put(pg.info.name, pg);
                    if ((policyFlags & 2) != 0) {
                        if (r7 == null) {
                            r7 = new StringBuilder(256);
                        } else {
                            r7.append(' ');
                        }
                        r7.append(pg.info.name);
                    }
                } else {
                    Slog.w(TAG, "Permission group " + pg.info.name + " from package " + pg.info.packageName + " ignored: original from " + cur.info.packageName);
                    if ((policyFlags & 2) != 0) {
                        if (r7 == null) {
                            r7 = new StringBuilder(256);
                        } else {
                            r7.append(' ');
                        }
                        r7.append("DUP:");
                        r7.append(pg.info.name);
                    }
                }
            }
            if (r7 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Permission Groups: " + ((Object) r7));
            }
            int N7 = pkg.permissions.size();
            StringBuilder r8 = null;
            for (int i12 = 0; i12 < N7; i12++) {
                PackageParser.Permission p6 = (PackageParser.Permission) pkg.permissions.get(i12);
                p6.info.flags &= -1073741825;
                if (pkg.applicationInfo.targetSdkVersion > 22) {
                    p6.group = this.mPermissionGroups.get(p6.info.group);
                    if (p6.info.group != null && p6.group == null) {
                        Slog.w(TAG, "Permission " + p6.info.name + " from package " + p6.info.packageName + " in an unknown group " + p6.info.group);
                    }
                }
                ArrayMap<String, BasePermission> permissionMap = p6.tree ? this.mSettings.mPermissionTrees : this.mSettings.mPermissions;
                BasePermission bp = permissionMap.get(p6.info.name);
                if (bp != null && !Objects.equals(bp.sourcePackage, p6.info.packageName)) {
                    boolean currentOwnerIsSystem = bp.perm != null ? isSystemApp(bp.perm.owner) : false;
                    if (isSystemApp(p6.owner)) {
                        if (bp.type == 1 && bp.perm == null) {
                            bp.packageSetting = pkgSetting;
                            bp.perm = p6;
                            bp.uid = pkg.applicationInfo.uid;
                            bp.sourcePackage = p6.info.packageName;
                            p6.info.flags |= 1073741824;
                        } else if (!currentOwnerIsSystem) {
                            String msg4 = "New decl " + p6.owner + " of permission  " + p6.info.name + " is system; overriding " + bp.sourcePackage;
                            reportSettingsProblem(5, msg4);
                            bp = null;
                        }
                    }
                }
                if (bp == null) {
                    bp = new BasePermission(p6.info.name, p6.info.packageName, 0);
                    permissionMap.put(p6.info.name, bp);
                }
                if (bp.perm == null) {
                    if (bp.sourcePackage == null || bp.sourcePackage.equals(p6.info.packageName)) {
                        BasePermission tree = findPermissionTreeLP(p6.info.name);
                        if (tree == null || tree.sourcePackage.equals(p6.info.packageName)) {
                            bp.packageSetting = pkgSetting;
                            bp.perm = p6;
                            bp.uid = pkg.applicationInfo.uid;
                            bp.sourcePackage = p6.info.packageName;
                            p6.info.flags |= 1073741824;
                            if ((policyFlags & 2) != 0) {
                                if (r8 == null) {
                                    r8 = new StringBuilder(256);
                                } else {
                                    r8.append(' ');
                                }
                                r8.append(p6.info.name);
                            }
                        } else {
                            Slog.w(TAG, "Permission " + p6.info.name + " from package " + p6.info.packageName + " ignored: base tree " + tree.name + " is from package " + tree.sourcePackage);
                        }
                    } else {
                        Slog.w(TAG, "Permission " + p6.info.name + " from package " + p6.info.packageName + " ignored: original from " + bp.sourcePackage);
                    }
                } else if ((policyFlags & 2) != 0) {
                    if (r8 == null) {
                        r8 = new StringBuilder(256);
                    } else {
                        r8.append(' ');
                    }
                    r8.append("DUP:");
                    r8.append(p6.info.name);
                }
                if (bp.perm == p6) {
                    bp.protectionLevel = p6.info.protectionLevel;
                }
            }
            if (r8 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Permissions: " + ((Object) r8));
            }
            int N8 = pkg.instrumentation.size();
            StringBuilder r9 = null;
            for (int i13 = 0; i13 < N8; i13++) {
                PackageParser.Instrumentation a4 = (PackageParser.Instrumentation) pkg.instrumentation.get(i13);
                a4.info.packageName = pkg.applicationInfo.packageName;
                a4.info.sourceDir = pkg.applicationInfo.sourceDir;
                a4.info.publicSourceDir = pkg.applicationInfo.publicSourceDir;
                a4.info.splitSourceDirs = pkg.applicationInfo.splitSourceDirs;
                a4.info.splitPublicSourceDirs = pkg.applicationInfo.splitPublicSourceDirs;
                a4.info.dataDir = pkg.applicationInfo.dataDir;
                a4.info.deviceProtectedDataDir = pkg.applicationInfo.deviceProtectedDataDir;
                a4.info.credentialProtectedDataDir = pkg.applicationInfo.credentialProtectedDataDir;
                a4.info.nativeLibraryDir = pkg.applicationInfo.nativeLibraryDir;
                a4.info.secondaryNativeLibraryDir = pkg.applicationInfo.secondaryNativeLibraryDir;
                this.mInstrumentation.put(a4.getComponentName(), a4);
                if ((policyFlags & 2) != 0) {
                    if (r9 == null) {
                        r9 = new StringBuilder(256);
                    } else {
                        r9.append(' ');
                    }
                    r9.append(a4.info.name);
                }
            }
            if (r9 != null && DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Instrumentation: " + ((Object) r9));
            }
            if (pkg.protectedBroadcasts != null) {
                int N9 = pkg.protectedBroadcasts.size();
                for (int i14 = 0; i14 < N9; i14++) {
                    this.mProtectedBroadcasts.add((String) pkg.protectedBroadcasts.get(i14));
                }
            }
            pkgSetting.setTimeStamp(scanFileTime);
            if (pkg.mOverlayTarget != null) {
                if (pkg.mOverlayTarget != null && !pkg.mOverlayTarget.equals(PLATFORM_PACKAGE_NAME) && !pkg.mOverlayTarget.equals("com.mediatek")) {
                    if (!this.mOverlays.containsKey(pkg.mOverlayTarget)) {
                        this.mOverlays.put(pkg.mOverlayTarget, new ArrayMap<>());
                    }
                    ArrayMap<String, PackageParser.Package> map = this.mOverlays.get(pkg.mOverlayTarget);
                    map.put(pkg.packageName, pkg);
                    PackageParser.Package orig2 = this.mPackages.get(pkg.mOverlayTarget);
                    if (orig2 != null && !createIdmapForPackagePairLI(orig2, pkg)) {
                        createIdmapFailed = true;
                    }
                }
            } else if (this.mOverlays.containsKey(pkg.packageName) && !pkg.packageName.equals(PLATFORM_PACKAGE_NAME) && !pkg.packageName.equals("com.mediatek")) {
                createIdmapsForPackageLI(pkg);
            }
            if (r49 != null) {
                final ArrayList<String> allPackageNames = new ArrayList<>(this.mPackages.keySet());
                final PackageParser.Package r22 = r49;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        PackageManagerService.this.revokeRuntimePermissionsIfGroupChanged(pkg, r22, allPackageNames);
                    }
                });
            }
        }
        Trace.traceEnd(1048576L);
        if (createIdmapFailed) {
            throw new PackageManagerException(-7, "scanPackageLI failed to createIdmap");
        }
        return pkg;
    }

    private void maybeRenameForeignDexMarkers(PackageParser.Package existing, PackageParser.Package update, UserHandle user) {
        if (existing.applicationInfo == null || update.applicationInfo == null) {
            return;
        }
        File oldCodePath = new File(existing.applicationInfo.getCodePath());
        File newCodePath = new File(update.applicationInfo.getCodePath());
        if (Objects.equals(oldCodePath, newCodePath)) {
            return;
        }
        try {
            File canonicalNewCodePath = new File(PackageManagerServiceUtils.realpath(newCodePath));
            File canonicalOldCodePath = new File(canonicalNewCodePath.getParentFile(), oldCodePath.getName());
            String oldMarkerPrefix = canonicalOldCodePath.getAbsolutePath().replace('/', '@');
            if (!oldMarkerPrefix.endsWith("@")) {
                oldMarkerPrefix = oldMarkerPrefix + "@";
            }
            String newMarkerPrefix = canonicalNewCodePath.getAbsolutePath().replace('/', '@');
            if (!newMarkerPrefix.endsWith("@")) {
                newMarkerPrefix = newMarkerPrefix + "@";
            }
            List<String> updatedPaths = update.getAllCodePathsExcludingResourceOnly();
            List<String> markerSuffixes = new ArrayList<>(updatedPaths.size());
            for (String updatedPath : updatedPaths) {
                String updatedPathName = new File(updatedPath).getName();
                markerSuffixes.add(updatedPathName.replace('/', '@'));
            }
            for (int userId : resolveUserIds(user.getIdentifier())) {
                File profileDir = Environment.getDataProfilesDeForeignDexDirectory(userId);
                for (String markerSuffix : markerSuffixes) {
                    File oldForeignUseMark = new File(profileDir, oldMarkerPrefix + markerSuffix);
                    File newForeignUseMark = new File(profileDir, newMarkerPrefix + markerSuffix);
                    if (oldForeignUseMark.exists()) {
                        try {
                            Os.rename(oldForeignUseMark.getAbsolutePath(), newForeignUseMark.getAbsolutePath());
                        } catch (ErrnoException e) {
                            Slog.w(TAG, "Failed to rename foreign use marker", e);
                            oldForeignUseMark.delete();
                        }
                    }
                }
            }
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to get canonical path.", e2);
        }
    }

    private void derivePackageAbi(PackageParser.Package pkg, File scanFile, String cpuAbiOverride, boolean extractLibs) throws PackageManagerException {
        int copyRet;
        setNativeLibraryPaths(pkg);
        if (pkg.isForwardLocked() || pkg.applicationInfo.isExternalAsec() || (isSystemApp(pkg) && !pkg.isUpdatedSystemApp())) {
            extractLibs = false;
        }
        String nativeLibraryRootStr = pkg.applicationInfo.nativeLibraryRootDir;
        boolean useIsaSpecificSubdirs = pkg.applicationInfo.nativeLibraryRootRequiresIsa;
        try {
            try {
                NativeLibraryHelper.Handle handle = NativeLibraryHelper.Handle.create(pkg);
                File nativeLibraryRoot = new File(nativeLibraryRootStr);
                pkg.applicationInfo.primaryCpuAbi = null;
                pkg.applicationInfo.secondaryCpuAbi = null;
                if (isMultiArch(pkg.applicationInfo)) {
                    if (pkg.cpuAbiOverride != null && !INSTALL_PACKAGE_SUFFIX.equals(pkg.cpuAbiOverride)) {
                        Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                    }
                    int abi32 = -114;
                    int abi64 = -114;
                    if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                        if (extractLibs) {
                            abi32 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, Build.SUPPORTED_32_BIT_ABIS, useIsaSpecificSubdirs);
                        } else {
                            abi32 = NativeLibraryHelper.findSupportedAbi(handle, Build.SUPPORTED_32_BIT_ABIS);
                        }
                    }
                    maybeThrowExceptionForMultiArchCopy("Error unpackaging 32 bit native libs for multiarch app.", abi32);
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                        if (extractLibs) {
                            abi64 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, Build.SUPPORTED_64_BIT_ABIS, useIsaSpecificSubdirs);
                        } else {
                            abi64 = NativeLibraryHelper.findSupportedAbi(handle, Build.SUPPORTED_64_BIT_ABIS);
                        }
                    }
                    maybeThrowExceptionForMultiArchCopy("Error unpackaging 64 bit native libs for multiarch app.", abi64);
                    if (abi64 >= 0) {
                        pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[abi64];
                    }
                    if (abi32 >= 0) {
                        String abi = Build.SUPPORTED_32_BIT_ABIS[abi32];
                        if (abi64 >= 0) {
                            if (pkg.use32bitAbi) {
                                pkg.applicationInfo.secondaryCpuAbi = pkg.applicationInfo.primaryCpuAbi;
                                pkg.applicationInfo.primaryCpuAbi = abi;
                            } else {
                                pkg.applicationInfo.secondaryCpuAbi = abi;
                            }
                        } else {
                            pkg.applicationInfo.primaryCpuAbi = abi;
                        }
                    }
                } else {
                    String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
                    boolean needsRenderScriptOverride = false;
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && NativeLibraryHelper.hasRenderscriptBitcode(handle)) {
                        abiList = Build.SUPPORTED_32_BIT_ABIS;
                        needsRenderScriptOverride = true;
                    }
                    if (extractLibs) {
                        copyRet = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, abiList, useIsaSpecificSubdirs);
                    } else {
                        copyRet = NativeLibraryHelper.findSupportedAbi(handle, abiList);
                    }
                    if (copyRet < 0 && copyRet != -114) {
                        throw new PackageManagerException(-110, "Error unpackaging native libs for app, errorCode=" + copyRet);
                    }
                    if (copyRet >= 0) {
                        pkg.applicationInfo.primaryCpuAbi = abiList[copyRet];
                    } else if (copyRet == -114 && cpuAbiOverride != null) {
                        pkg.applicationInfo.primaryCpuAbi = cpuAbiOverride;
                    } else if (needsRenderScriptOverride) {
                        pkg.applicationInfo.primaryCpuAbi = abiList[0];
                    }
                }
                IoUtils.closeQuietly(handle);
            } catch (IOException ioe) {
                Slog.e(TAG, "Unable to get canonical file " + ioe.toString());
                IoUtils.closeQuietly((AutoCloseable) null);
            }
            setNativeLibraryPaths(pkg);
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private void adjustCpuAbisForSharedUserLPw(Set<PackageSetting> packagesForUser, PackageParser.Package scannedPackage, boolean bootComplete) {
        String adjustedAbi;
        String requiredInstructionSet = null;
        if (scannedPackage != null && scannedPackage.applicationInfo.primaryCpuAbi != null) {
            requiredInstructionSet = VMRuntime.getInstructionSet(scannedPackage.applicationInfo.primaryCpuAbi);
        }
        PackageSetting requirer = null;
        for (PackageSetting ps : packagesForUser) {
            if (scannedPackage == null || !scannedPackage.packageName.equals(ps.name)) {
                if (ps.primaryCpuAbiString != null) {
                    String instructionSet = VMRuntime.getInstructionSet(ps.primaryCpuAbiString);
                    if (requiredInstructionSet != null && !instructionSet.equals(requiredInstructionSet)) {
                        String errorMessage = "Instruction set mismatch, " + (requirer == null ? "[caller]" : requirer) + " requires " + requiredInstructionSet + " whereas " + ps + " requires " + instructionSet;
                        Slog.w(TAG, errorMessage);
                    }
                    if (requiredInstructionSet == null || ps.name.equals(PLATFORM_PACKAGE_NAME)) {
                        requiredInstructionSet = instructionSet;
                        requirer = ps;
                    }
                }
            }
        }
        if (requiredInstructionSet != null) {
            if (requirer != null) {
                adjustedAbi = requirer.primaryCpuAbiString;
                if (scannedPackage != null) {
                    scannedPackage.applicationInfo.primaryCpuAbi = adjustedAbi;
                }
            } else {
                adjustedAbi = scannedPackage.applicationInfo.primaryCpuAbi;
            }
            for (PackageSetting ps2 : packagesForUser) {
                if (scannedPackage == null || !scannedPackage.packageName.equals(ps2.name)) {
                    if (ps2.primaryCpuAbiString == null) {
                        ps2.primaryCpuAbiString = adjustedAbi;
                        if (ps2.pkg != null && ps2.pkg.applicationInfo != null && !TextUtils.equals(adjustedAbi, ps2.pkg.applicationInfo.primaryCpuAbi)) {
                            ps2.pkg.applicationInfo.primaryCpuAbi = adjustedAbi;
                            Slog.i(TAG, "Adjusting ABI for " + ps2.name + " to " + adjustedAbi + " (requirer=" + (requirer == null ? "null" : requirer.pkg.packageName) + ", scannedPackage=" + (scannedPackage != null ? scannedPackage.packageName : "null") + ")");
                            try {
                                this.mInstaller.rmdex(ps2.codePathString, InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                            } catch (InstallerConnection.InstallerException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private void setUpCustomResolverActivity(PackageParser.Package pkg) {
        synchronized (this.mPackages) {
            this.mResolverReplaced = true;
            this.mResolveActivity.applicationInfo = pkg.applicationInfo;
            this.mResolveActivity.name = this.mCustomResolverComponentName.getClassName();
            this.mResolveActivity.packageName = pkg.applicationInfo.packageName;
            this.mResolveActivity.processName = pkg.applicationInfo.packageName;
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

    private void setUpEphemeralInstallerActivityLP(ComponentName installerComponent) {
        PackageParser.Package pkg = this.mPackages.get(installerComponent.getPackageName());
        this.mEphemeralInstallerActivity.applicationInfo = pkg.applicationInfo;
        this.mEphemeralInstallerActivity.name = this.mEphemeralInstallerComponent.getClassName();
        this.mEphemeralInstallerActivity.packageName = pkg.applicationInfo.packageName;
        this.mEphemeralInstallerActivity.processName = pkg.applicationInfo.packageName;
        this.mEphemeralInstallerActivity.launchMode = 0;
        this.mEphemeralInstallerActivity.flags = 288;
        this.mEphemeralInstallerActivity.theme = 0;
        this.mEphemeralInstallerActivity.exported = true;
        this.mEphemeralInstallerActivity.enabled = true;
        this.mEphemeralInstallerInfo.activityInfo = this.mEphemeralInstallerActivity;
        this.mEphemeralInstallerInfo.priority = 0;
        this.mEphemeralInstallerInfo.preferredOrder = 0;
        this.mEphemeralInstallerInfo.match = 0;
        if (!DEBUG_EPHEMERAL) {
            return;
        }
        Slog.d(TAG, "Set ephemeral installer activity: " + this.mEphemeralInstallerComponent);
    }

    private static String calculateBundledApkRoot(String codePathString) {
        File codeRoot;
        File codePath = new File(codePathString);
        if (FileUtils.contains(Environment.getRootDirectory(), codePath)) {
            codeRoot = Environment.getRootDirectory();
        } else if (FileUtils.contains(Environment.getOemDirectory(), codePath)) {
            codeRoot = Environment.getOemDirectory();
        } else if (FileUtils.contains(Environment.getVendorDirectory(), codePath)) {
            codeRoot = Environment.getVendorDirectory();
        } else {
            try {
                File f = codePath.getCanonicalFile();
                File parent = f.getParentFile();
                while (true) {
                    File tmp = parent.getParentFile();
                    if (tmp == null) {
                        break;
                    }
                    f = parent;
                    parent = tmp;
                }
                codeRoot = f;
                Slog.w(TAG, "Unrecognized code path " + codePath + " - using " + codeRoot);
            } catch (IOException e) {
                Slog.w(TAG, "Can't canonicalize code path " + codePath);
                return Environment.getRootDirectory().getPath();
            }
        }
        return codeRoot.getPath();
    }

    private void setNativeLibraryPaths(PackageParser.Package pkg) {
        ApplicationInfo info = pkg.applicationInfo;
        String codePath = pkg.codePath;
        File codeFile = new File(codePath);
        boolean bundledApp = info.isSystemApp() && !info.isUpdatedSystemApp();
        boolean zIsExternalAsec = !info.isForwardLocked() ? info.isExternalAsec() : true;
        info.nativeLibraryRootDir = null;
        info.nativeLibraryRootRequiresIsa = false;
        info.nativeLibraryDir = null;
        info.secondaryNativeLibraryDir = null;
        if (PackageParser.isApkFile(codeFile)) {
            if (bundledApp) {
                String apkRoot = calculateBundledApkRoot(info.sourceDir);
                boolean is64Bit = VMRuntime.is64BitInstructionSet(InstructionSets.getPrimaryInstructionSet(info));
                String apkName = deriveCodePathName(codePath);
                String libDir = is64Bit ? "lib64" : "lib";
                info.nativeLibraryRootDir = Environment.buildPath(new File(apkRoot), new String[]{libDir, apkName}).getAbsolutePath();
                if (info.secondaryCpuAbi != null) {
                    String secondaryLibDir = is64Bit ? "lib" : "lib64";
                    info.secondaryNativeLibraryDir = Environment.buildPath(new File(apkRoot), new String[]{secondaryLibDir, apkName}).getAbsolutePath();
                }
            } else if (zIsExternalAsec) {
                info.nativeLibraryRootDir = new File(codeFile.getParentFile(), "lib").getAbsolutePath();
            } else {
                String apkName2 = deriveCodePathName(codePath);
                info.nativeLibraryRootDir = new File(this.mAppLib32InstallDir, apkName2).getAbsolutePath();
            }
            info.nativeLibraryRootRequiresIsa = false;
            info.nativeLibraryDir = info.nativeLibraryRootDir;
            return;
        }
        if (isVendorApp(info)) {
            String apkName3 = deriveCodePathName(codePath);
            info.nativeLibraryRootDir = new File(this.mAppLib32InstallDir, apkName3).getAbsolutePath();
            info.nativeLibraryRootRequiresIsa = false;
            info.nativeLibraryDir = info.nativeLibraryRootDir;
            return;
        }
        info.nativeLibraryRootDir = new File(codeFile, "lib").getAbsolutePath();
        info.nativeLibraryRootRequiresIsa = true;
        info.nativeLibraryDir = new File(info.nativeLibraryRootDir, InstructionSets.getPrimaryInstructionSet(info)).getAbsolutePath();
        if (info.secondaryCpuAbi == null) {
            return;
        }
        info.secondaryNativeLibraryDir = new File(info.nativeLibraryRootDir, VMRuntime.getInstructionSet(info.secondaryCpuAbi)).getAbsolutePath();
    }

    private void setBundledAppAbisAndRoots(PackageParser.Package pkg, PackageSetting pkgSetting) {
        String apkName = deriveCodePathName(pkg.applicationInfo.getCodePath());
        String apkRoot = calculateBundledApkRoot(pkg.applicationInfo.sourceDir);
        setBundledAppAbi(pkg, apkRoot, apkName);
        if (pkgSetting == null) {
            return;
        }
        pkgSetting.primaryCpuAbiString = pkg.applicationInfo.primaryCpuAbi;
        pkgSetting.secondaryCpuAbiString = pkg.applicationInfo.secondaryCpuAbi;
    }

    private static void setBundledAppAbi(PackageParser.Package pkg, String apkRoot, String apkName) {
        boolean has64BitLibs;
        boolean zExists;
        File codeFile = new File(pkg.codePath);
        if (PackageParser.isApkFile(codeFile)) {
            has64BitLibs = new File(apkRoot, new File("lib64", apkName).getPath()).exists();
            zExists = new File(apkRoot, new File("lib", apkName).getPath()).exists();
        } else {
            File rootDir = new File(codeFile, "lib");
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS[0])) {
                String isa = VMRuntime.getInstructionSet(Build.SUPPORTED_64_BIT_ABIS[0]);
                has64BitLibs = new File(rootDir, isa).exists();
            } else {
                has64BitLibs = false;
            }
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS[0])) {
                String isa2 = VMRuntime.getInstructionSet(Build.SUPPORTED_32_BIT_ABIS[0]);
                zExists = new File(rootDir, isa2).exists();
            } else {
                zExists = false;
            }
        }
        if (has64BitLibs && !zExists) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (zExists && !has64BitLibs) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (zExists && has64BitLibs) {
            if ((pkg.applicationInfo.flags & Integer.MIN_VALUE) == 0) {
                Slog.e(TAG, "Package " + pkg + " has multiple bundled libs, but is not multiarch.");
            }
            if (VMRuntime.is64BitInstructionSet(InstructionSets.getPreferredInstructionSet())) {
                pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
                pkg.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
                return;
            }
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            return;
        }
        pkg.applicationInfo.primaryCpuAbi = null;
        pkg.applicationInfo.secondaryCpuAbi = null;
    }

    private void killApplication(String pkgName, int appId, String reason) {
        killApplication(pkgName, appId, -1, reason);
    }

    private void killApplication(String pkgName, int appId, int userId, String reason) {
        long token = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am != null) {
                try {
                    am.killApplication(pkgName, appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void removePackageLI(PackageParser.Package pkg, boolean chatty) {
        PackageSetting ps = (PackageSetting) pkg.mExtras;
        if (ps != null) {
            removePackageLI(ps, chatty);
        }
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            PackageSetting ps2 = (PackageSetting) childPkg.mExtras;
            if (ps2 != null) {
                removePackageLI(ps2, chatty);
            }
        }
    }

    void removePackageLI(PackageSetting ps, boolean chatty) {
        if (DEBUG_INSTALL && chatty) {
            Log.d(TAG, "Removing package " + ps.name);
        }
        synchronized (this.mPackages) {
            this.mPackages.remove(ps.name);
            PackageParser.Package pkg = ps.pkg;
            if (pkg != null) {
                cleanPackageDataStructuresLILPw(pkg, chatty);
            }
        }
    }

    void removeInstalledPackageLI(PackageParser.Package pkg, boolean chatty) {
        if (DEBUG_INSTALL && chatty) {
            Log.d(TAG, "Removing package " + pkg.applicationInfo.packageName);
        }
        synchronized (this.mPackages) {
            this.mPackages.remove(pkg.applicationInfo.packageName);
            cleanPackageDataStructuresLILPw(pkg, chatty);
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
                this.mPackages.remove(childPkg.applicationInfo.packageName);
                cleanPackageDataStructuresLILPw(childPkg, chatty);
            }
        }
    }

    void cleanPackageDataStructuresLILPw(PackageParser.Package pkg, boolean chatty) {
        ArraySet<String> appOpPkgs;
        ArraySet<String> appOpPkgs2;
        int N = pkg.providers.size();
        StringBuilder r = null;
        for (int i = 0; i < N; i++) {
            PackageParser.Provider p = (PackageParser.Provider) pkg.providers.get(i);
            this.mProviders.removeProvider(p);
            if (p.info.authority != null) {
                String[] names = p.info.authority.split(";");
                for (int j = 0; j < names.length; j++) {
                    if (this.mProvidersByAuthority.get(names[j]) == p) {
                        this.mProvidersByAuthority.remove(names[j]);
                        if (DEBUG_REMOVE && chatty) {
                            Log.d(TAG, "Unregistered content provider: " + names[j] + ", className = " + p.info.name + ", isSyncable = " + p.info.isSyncable);
                        }
                    }
                }
                if (DEBUG_REMOVE && chatty) {
                    if (r == null) {
                        r = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(p.info.name);
                }
            }
        }
        if (r != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Providers: " + ((Object) r));
        }
        int N2 = pkg.services.size();
        StringBuilder r2 = null;
        for (int i2 = 0; i2 < N2; i2++) {
            PackageParser.Service s = (PackageParser.Service) pkg.services.get(i2);
            this.mServices.removeService(s);
            if (chatty) {
                if (r2 == null) {
                    r2 = new StringBuilder(256);
                } else {
                    r2.append(' ');
                }
                r2.append(s.info.name);
            }
        }
        if (r2 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Services: " + ((Object) r2));
        }
        int N3 = pkg.receivers.size();
        StringBuilder r3 = null;
        for (int i3 = 0; i3 < N3; i3++) {
            PackageParser.Activity a = (PackageParser.Activity) pkg.receivers.get(i3);
            this.mReceivers.removeActivity(a, "receiver");
            if (DEBUG_REMOVE && chatty) {
                if (r3 == null) {
                    r3 = new StringBuilder(256);
                } else {
                    r3.append(' ');
                }
                r3.append(a.info.name);
            }
        }
        if (r3 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Receivers: " + ((Object) r3));
        }
        int N4 = pkg.activities.size();
        StringBuilder r4 = null;
        for (int i4 = 0; i4 < N4; i4++) {
            PackageParser.Activity a2 = (PackageParser.Activity) pkg.activities.get(i4);
            this.mActivities.removeActivity(a2, "activity");
            if (DEBUG_REMOVE && chatty) {
                if (r4 == null) {
                    r4 = new StringBuilder(256);
                } else {
                    r4.append(' ');
                }
                r4.append(a2.info.name);
            }
        }
        if (r4 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Activities: " + ((Object) r4));
        }
        int N5 = pkg.permissions.size();
        StringBuilder r5 = null;
        for (int i5 = 0; i5 < N5; i5++) {
            PackageParser.Permission p2 = (PackageParser.Permission) pkg.permissions.get(i5);
            BasePermission bp = this.mSettings.mPermissions.get(p2.info.name);
            if (bp == null) {
                bp = this.mSettings.mPermissionTrees.get(p2.info.name);
            }
            if (bp != null && bp.perm == p2) {
                bp.perm = null;
                if (DEBUG_REMOVE && chatty) {
                    if (r5 == null) {
                        r5 = new StringBuilder(256);
                    } else {
                        r5.append(' ');
                    }
                    r5.append(p2.info.name);
                }
            }
            if ((p2.info.protectionLevel & 64) != 0 && (appOpPkgs2 = this.mAppOpPermissionPackages.get(p2.info.name)) != null) {
                appOpPkgs2.remove(pkg.packageName);
            }
        }
        if (r5 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Permissions: " + ((Object) r5));
        }
        int N6 = pkg.requestedPermissions.size();
        for (int i6 = 0; i6 < N6; i6++) {
            String perm = (String) pkg.requestedPermissions.get(i6);
            BasePermission bp2 = this.mSettings.mPermissions.get(perm);
            if (bp2 != null && (bp2.protectionLevel & 64) != 0 && (appOpPkgs = this.mAppOpPermissionPackages.get(perm)) != null) {
                appOpPkgs.remove(pkg.packageName);
                if (appOpPkgs.isEmpty()) {
                    this.mAppOpPermissionPackages.remove(perm);
                }
            }
        }
        int N7 = pkg.instrumentation.size();
        StringBuilder r6 = null;
        for (int i7 = 0; i7 < N7; i7++) {
            PackageParser.Instrumentation a3 = (PackageParser.Instrumentation) pkg.instrumentation.get(i7);
            this.mInstrumentation.remove(a3.getComponentName());
            if (DEBUG_REMOVE && chatty) {
                if (r6 == null) {
                    r6 = new StringBuilder(256);
                } else {
                    r6.append(' ');
                }
                r6.append(a3.info.name);
            }
        }
        if (r6 != null && DEBUG_REMOVE) {
            Log.d(TAG, "  Instrumentation: " + ((Object) r6));
        }
        StringBuilder r7 = null;
        if ((pkg.applicationInfo.flags & 1) != 0 && pkg.libraryNames != null) {
            for (int i8 = 0; i8 < pkg.libraryNames.size(); i8++) {
                String name = (String) pkg.libraryNames.get(i8);
                SharedLibraryEntry cur = this.mSharedLibraries.get(name);
                if (cur != null && cur.apk != null && cur.apk.equals(pkg.packageName)) {
                    this.mSharedLibraries.remove(name);
                    if (DEBUG_REMOVE && chatty) {
                        if (r7 == null) {
                            r7 = new StringBuilder(256);
                        } else {
                            r7.append(' ');
                        }
                        r7.append(name);
                    }
                }
            }
        }
        if (r7 == null || !DEBUG_REMOVE) {
            return;
        }
        Log.d(TAG, "  Libraries: " + ((Object) r7));
    }

    private static boolean hasPermission(PackageParser.Package pkgInfo, String perm) {
        for (int i = pkgInfo.permissions.size() - 1; i >= 0; i--) {
            if (((PackageParser.Permission) pkgInfo.permissions.get(i)).info.name.equals(perm)) {
                return true;
            }
        }
        return false;
    }

    private void updatePermissionsLPw(PackageParser.Package pkg, int flags) {
        updatePermissionsLPw(pkg.packageName, pkg, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            updatePermissionsLPw(childPkg.packageName, childPkg, flags);
        }
    }

    private void updatePermissionsLPw(String changingPkg, PackageParser.Package pkgInfo, int flags) {
        String volumeUuid = pkgInfo != null ? getVolumeUuidForPackage(pkgInfo) : null;
        updatePermissionsLPw(changingPkg, pkgInfo, volumeUuid, flags);
    }

    private void updatePermissionsLPw(String changingPkg, PackageParser.Package pkgInfo, String replaceVolumeUuid, int flags) {
        BasePermission tree;
        Iterator<BasePermission> it = this.mSettings.mPermissionTrees.values().iterator();
        while (it.hasNext()) {
            BasePermission bp = it.next();
            if (bp.packageSetting == null) {
                bp.packageSetting = this.mSettings.mPackages.get(bp.sourcePackage);
            }
            if (bp.packageSetting == null) {
                Slog.w(TAG, "Removing dangling permission tree: " + bp.name + " from package " + bp.sourcePackage);
                it.remove();
            } else if (changingPkg != null && changingPkg.equals(bp.sourcePackage) && (pkgInfo == null || !hasPermission(pkgInfo, bp.name))) {
                Slog.i(TAG, "Removing old permission tree: " + bp.name + " from package " + bp.sourcePackage);
                flags |= 1;
                it.remove();
            }
        }
        Iterator<BasePermission> it2 = this.mSettings.mPermissions.values().iterator();
        while (it2.hasNext()) {
            BasePermission bp2 = it2.next();
            if (bp2.type == 2) {
                if (DEBUG_SETTINGS) {
                    Log.v(TAG, "Dynamic permission: name=" + bp2.name + " pkg=" + bp2.sourcePackage + " info=" + bp2.pendingInfo);
                }
                if (bp2.packageSetting == null && bp2.pendingInfo != null && (tree = findPermissionTreeLP(bp2.name)) != null && tree.perm != null) {
                    bp2.packageSetting = tree.packageSetting;
                    bp2.perm = new PackageParser.Permission(tree.perm.owner, new PermissionInfo(bp2.pendingInfo));
                    bp2.perm.info.packageName = tree.perm.info.packageName;
                    bp2.perm.info.name = bp2.name;
                    bp2.uid = tree.uid;
                }
            }
            if (bp2.packageSetting == null) {
                bp2.packageSetting = this.mSettings.mPackages.get(bp2.sourcePackage);
            }
            if (bp2.packageSetting == null) {
                Slog.w(TAG, "Removing dangling permission: " + bp2.name + " from package " + bp2.sourcePackage);
                it2.remove();
            } else if (changingPkg != null && changingPkg.equals(bp2.sourcePackage) && (pkgInfo == null || !hasPermission(pkgInfo, bp2.name))) {
                Slog.i(TAG, "Removing old permission: " + bp2.name + " from package " + bp2.sourcePackage);
                flags |= 1;
                it2.remove();
            }
        }
        if ((flags & 1) != 0) {
            for (PackageParser.Package pkg : this.mPackages.values()) {
                if (pkg != pkgInfo) {
                    String volumeUuid = getVolumeUuidForPackage(pkg);
                    boolean replace = (flags & 4) != 0 ? Objects.equals(replaceVolumeUuid, volumeUuid) : false;
                    grantPermissionsLPw(pkg, replace, changingPkg);
                }
            }
        }
        if (pkgInfo != null) {
            String volumeUuid2 = getVolumeUuidForPackage(pkgInfo);
            boolean replace2 = (flags & 2) != 0 ? Objects.equals(replaceVolumeUuid, volumeUuid2) : false;
            grantPermissionsLPw(pkgInfo, replace2, changingPkg);
        }
    }

    private void grantPermissionsLPw(PackageParser.Package pkg, boolean replace, String packageOfInterest) {
        PackageSetting ps = (PackageSetting) pkg.mExtras;
        if (ps == null) {
            return;
        }
        Trace.traceBegin(1048576L, "grantPermissions");
        PermissionsState permissionsState = ps.getPermissionsState();
        PermissionsState origPermissions = permissionsState;
        int[] currentUserIds = UserManagerService.getInstance().getUserIds();
        boolean runtimePermissionsRevoked = false;
        int[] changedRuntimePermissionUserIds = EMPTY_INT_ARRAY;
        boolean changedInstallPermission = false;
        if (replace) {
            ps.installPermissionsFixed = false;
            if (ps.isSharedUser()) {
                changedRuntimePermissionUserIds = revokeUnusedSharedUserPermissionsLPw(ps.sharedUser, UserManagerService.getInstance().getUserIds());
                if (!ArrayUtils.isEmpty(changedRuntimePermissionUserIds)) {
                    runtimePermissionsRevoked = true;
                }
            } else {
                origPermissions = new PermissionsState(permissionsState);
                permissionsState.reset();
            }
        }
        permissionsState.setGlobalGids(this.mGlobalGids);
        int N = pkg.requestedPermissions.size();
        boolean pkgReviewRequired = isPackageNeedsReview(pkg);
        for (int i = 0; i < N; i++) {
            String name = (String) pkg.requestedPermissions.get(i);
            BasePermission bp = this.mSettings.mPermissions.get(name);
            if (DEBUG_PACKAGE_INFO) {
                Log.i(TAG, "Package " + pkg.packageName + " checking " + name + ": " + bp);
            }
            if (bp != null && bp.packageSetting != null) {
                String perm = bp.name;
                boolean allowedSig = false;
                int grant = 1;
                if ((bp.protectionLevel & 64) != 0) {
                    ArraySet<String> pkgs = this.mAppOpPermissionPackages.get(bp.name);
                    if (pkgs == null) {
                        pkgs = new ArraySet<>();
                        this.mAppOpPermissionPackages.put(bp.name, pkgs);
                    }
                    pkgs.add(pkg.packageName);
                }
                int level = bp.protectionLevel & 15;
                boolean appSupportsRuntimePermissions = pkg.applicationInfo.targetSdkVersion >= 23;
                switch (level) {
                    case 0:
                        grant = 2;
                        break;
                    case 1:
                        if ((!appSupportsRuntimePermissions && !Build.isPermissionReviewRequired()) || BenesseExtension.getDchaState() != 0) {
                            grant = 2;
                        } else if (origPermissions.hasInstallPermission(bp.name)) {
                            grant = 4;
                        } else if (this.mPromoteSystemApps && isSystemApp(ps) && this.mExistingSystemPackages.contains(ps.name)) {
                            grant = 4;
                        } else {
                            grant = 3;
                        }
                        break;
                    case 2:
                        allowedSig = grantSignaturePermission(perm, pkg, bp, origPermissions) || BenesseExtension.getDchaState() != 0;
                        if (allowedSig) {
                            grant = 2;
                        }
                        break;
                }
                if (DEBUG_PACKAGE_INFO) {
                    Log.i(TAG, "Package " + pkg.packageName + " granting " + perm);
                }
                if (grant != 1) {
                    if (!isSystemApp(ps) && ps.installPermissionsFixed && !allowedSig && !origPermissions.hasInstallPermission(perm) && !isNewPlatformPermissionForPackage(perm, pkg)) {
                        grant = 1;
                    }
                    switch (grant) {
                        case 2:
                            for (int userId : UserManagerService.getInstance().getUserIds()) {
                                if (origPermissions.getRuntimePermissionState(bp.name, userId) != null) {
                                    origPermissions.revokeRuntimePermission(bp, userId);
                                    origPermissions.updatePermissionFlags(bp, userId, DhcpPacket.MAX_OPTION_LEN, 0);
                                    changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId);
                                }
                            }
                            if (permissionsState.grantInstallPermission(bp) != -1) {
                                changedInstallPermission = true;
                            }
                            break;
                        case 3:
                            for (int userId2 : UserManagerService.getInstance().getUserIds()) {
                                PermissionsState.PermissionState permissionState = origPermissions.getRuntimePermissionState(bp.name, userId2);
                                int flags = permissionState != null ? permissionState.getFlags() : 0;
                                if (origPermissions.hasRuntimePermission(bp.name, userId2)) {
                                    if (permissionsState.grantRuntimePermission(bp, userId2) == -1) {
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                    }
                                    if (Build.isPermissionReviewRequired() && appSupportsRuntimePermissions && (flags & 64) != 0) {
                                        flags &= -65;
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                    }
                                } else if (Build.isPermissionReviewRequired() && !appSupportsRuntimePermissions) {
                                    if (CtaUtils.isPlatformPermission(bp.sourcePackage, bp.name) && pkgReviewRequired && (flags & 64) == 0) {
                                        Slog.d(TAG, "add review UI for legacy pkg = " + pkg.packageName + ", permission = " + bp.name + ", userId = " + userId2 + ", sharedUid = " + pkg.mSharedUserId);
                                        flags |= 64;
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                    }
                                    if (permissionsState.grantRuntimePermission(bp, userId2) != -1) {
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                    }
                                } else if (appSupportsRuntimePermissions && pkgReviewRequired && CtaUtils.isPlatformPermission(bp.sourcePackage, bp.name) && (flags & 64) == 0 && (flags & 16) == 0) {
                                    Slog.d(TAG, "add review UI for non-legacy pkg = " + pkg.packageName + ", permission = " + bp.name + ", userId = " + userId2 + ", sharedUid = " + pkg.mSharedUserId);
                                    flags |= 64;
                                    changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                }
                                permissionsState.updatePermissionFlags(bp, userId2, flags, flags);
                            }
                            break;
                        case 4:
                            PermissionsState.PermissionState permissionState2 = origPermissions.getInstallPermissionState(bp.name);
                            int flags2 = permissionState2 != null ? permissionState2.getFlags() : 0;
                            if (origPermissions.revokeInstallPermission(bp) != -1) {
                                origPermissions.updatePermissionFlags(bp, -1, DhcpPacket.MAX_OPTION_LEN, 0);
                                changedInstallPermission = true;
                            }
                            if ((flags2 & 8) == 0) {
                                for (int userId3 : currentUserIds) {
                                    if (permissionsState.grantRuntimePermission(bp, userId3) != -1) {
                                        permissionsState.updatePermissionFlags(bp, userId3, flags2, flags2);
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId3);
                                    }
                                }
                            }
                            break;
                        default:
                            if (packageOfInterest == null || packageOfInterest.equals(pkg.packageName)) {
                                Slog.w(TAG, "Not granting permission " + perm + " to package " + pkg.packageName + " because it was previously installed without");
                            }
                            break;
                    }
                } else if (permissionsState.revokeInstallPermission(bp) != -1) {
                    permissionsState.updatePermissionFlags(bp, -1, DhcpPacket.MAX_OPTION_LEN, 0);
                    changedInstallPermission = true;
                    Slog.i(TAG, "Un-granting permission " + perm + " from package " + pkg.packageName + " (protectionLevel=" + bp.protectionLevel + " flags=0x" + Integer.toHexString(pkg.applicationInfo.flags) + ")");
                } else if ((bp.protectionLevel & 64) == 0 && (packageOfInterest == null || packageOfInterest.equals(pkg.packageName))) {
                    Slog.w(TAG, "Not granting permission " + perm + " to package " + pkg.packageName + " (protectionLevel=" + bp.protectionLevel + " flags=0x" + Integer.toHexString(pkg.applicationInfo.flags) + ")");
                }
            } else if (packageOfInterest == null || packageOfInterest.equals(pkg.packageName)) {
                Slog.w(TAG, "Unknown permission " + name + " in package " + pkg.packageName);
            }
        }
        if (((changedInstallPermission || replace) && !ps.installPermissionsFixed && !isSystemApp(ps)) || isUpdatedSystemApp(ps)) {
            ps.installPermissionsFixed = true;
        }
        for (int i2 : changedRuntimePermissionUserIds) {
            this.mSettings.writeRuntimePermissionsForUserLPr(i2, runtimePermissionsRevoked);
        }
        Trace.traceEnd(1048576L);
    }

    private boolean isNewPlatformPermissionForPackage(String perm, PackageParser.Package pkg) {
        int NP = PackageParser.NEW_PERMISSIONS.length;
        for (int ip = 0; ip < NP; ip++) {
            PackageParser.NewPermissionInfo npi = PackageParser.NEW_PERMISSIONS[ip];
            if (npi.name.equals(perm) && pkg.applicationInfo.targetSdkVersion < npi.sdkVersion) {
                Log.i(TAG, "Auto-granting " + perm + " to old pkg " + pkg.packageName);
                return true;
            }
        }
        return false;
    }

    private boolean grantSignaturePermission(String perm, PackageParser.Package pkg, BasePermission bp, PermissionsState origPermissions) {
        PackageSetting disabledSysParentPs;
        boolean allowed = true;
        if (compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) != 0 && compareSignatures(this.mPlatformPackage.mSignatures, pkg.mSignatures) != 0) {
            allowed = false;
        }
        if (!allowed && (bp.protectionLevel & 16) != 0 && isSystemApp(pkg)) {
            if (pkg.isUpdatedSystemApp()) {
                PackageSetting sysPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                if (sysPs != null && sysPs.getPermissionsState().hasInstallPermission(perm)) {
                    if (sysPs.isPrivileged()) {
                        allowed = true;
                    }
                } else {
                    if (sysPs != null && sysPs.pkg != null && sysPs.isPrivileged()) {
                        int j = 0;
                        while (true) {
                            if (j >= sysPs.pkg.requestedPermissions.size()) {
                                break;
                            }
                            if (!perm.equals(sysPs.pkg.requestedPermissions.get(j))) {
                                j++;
                            } else {
                                allowed = true;
                                break;
                            }
                        }
                    }
                    if (pkg.parentPackage != null && (disabledSysParentPs = this.mSettings.getDisabledSystemPkgLPr(pkg.parentPackage.packageName)) != null && disabledSysParentPs.pkg != null && disabledSysParentPs.isPrivileged()) {
                        if (isPackageRequestingPermission(disabledSysParentPs.pkg, perm)) {
                            allowed = true;
                        } else if (disabledSysParentPs.pkg.childPackages != null) {
                            int count = disabledSysParentPs.pkg.childPackages.size();
                            int i = 0;
                            while (true) {
                                if (i >= count) {
                                    break;
                                }
                                PackageParser.Package disabledSysChildPkg = (PackageParser.Package) disabledSysParentPs.pkg.childPackages.get(i);
                                if (!isPackageRequestingPermission(disabledSysChildPkg, perm)) {
                                    i++;
                                } else {
                                    allowed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                allowed = isPrivilegedApp(pkg);
            }
        }
        if (!allowed) {
            if (!allowed && (bp.protectionLevel & 128) != 0 && pkg.applicationInfo.targetSdkVersion < 23) {
                allowed = true;
            }
            if (!allowed && (bp.protectionLevel & 256) != 0 && pkg.packageName.equals(this.mRequiredInstallerPackage)) {
                allowed = true;
            }
            if (!allowed && (bp.protectionLevel & 512) != 0 && pkg.packageName.equals(this.mRequiredVerifierPackage)) {
                allowed = true;
            }
            if (!allowed && (bp.protectionLevel & 1024) != 0 && isSystemApp(pkg)) {
                allowed = true;
            }
            if (!allowed && (bp.protectionLevel & 32) != 0) {
                allowed = origPermissions.hasInstallPermission(perm);
            }
            if (!allowed && (bp.protectionLevel & 2048) != 0 && pkg.packageName.equals(this.mSetupWizardPackage)) {
                return true;
            }
            return allowed;
        }
        return allowed;
    }

    private boolean isPackageRequestingPermission(PackageParser.Package pkg, String permission) {
        int permCount = pkg.requestedPermissions.size();
        for (int j = 0; j < permCount; j++) {
            String requestedPermission = (String) pkg.requestedPermissions.get(j);
            if (permission.equals(requestedPermission)) {
                return true;
            }
        }
        return false;
    }

    final class ActivityIntentResolver extends IntentResolver<PackageParser.ActivityIntentInfo, ResolveInfo> {
        private final ArrayMap<ComponentName, PackageParser.Activity> mActivities = new ArrayMap<>();
        private int mFlags;

        ActivityIntentResolver() {
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Activity> packageActivities, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageActivities == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageActivities.size();
            ArrayList<PackageParser.ActivityIntentInfo[]> listCut = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                ArrayList<PackageParser.ActivityIntentInfo> intentFilters = packageActivities.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PackageParser.ActivityIntentInfo[] array = new PackageParser.ActivityIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        private PackageParser.Activity findMatchingActivity(List<PackageParser.Activity> activityList, ActivityInfo activityInfo) {
            for (PackageParser.Activity sysActivity : activityList) {
                if (sysActivity.info.name.equals(activityInfo.name)) {
                    return sysActivity;
                }
                if (sysActivity.info.name.equals(activityInfo.targetActivity)) {
                    return sysActivity;
                }
                if (sysActivity.info.targetActivity != null) {
                    if (sysActivity.info.targetActivity.equals(activityInfo.name)) {
                        return sysActivity;
                    }
                    if (sysActivity.info.targetActivity.equals(activityInfo.targetActivity)) {
                        return sysActivity;
                    }
                }
            }
            return null;
        }

        public class IterGenerator<E> {
            public IterGenerator() {
            }

            public Iterator<E> generate(PackageParser.ActivityIntentInfo info) {
                return null;
            }
        }

        public class ActionIterGenerator extends IterGenerator<String> {
            public ActionIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo info) {
                return info.actionsIterator();
            }
        }

        public class CategoriesIterGenerator extends IterGenerator<String> {
            public CategoriesIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo info) {
                return info.categoriesIterator();
            }
        }

        public class SchemesIterGenerator extends IterGenerator<String> {
            public SchemesIterGenerator() {
                super();
            }

            @Override
            public Iterator<String> generate(PackageParser.ActivityIntentInfo info) {
                return info.schemesIterator();
            }
        }

        public class AuthoritiesIterGenerator extends IterGenerator<IntentFilter.AuthorityEntry> {
            public AuthoritiesIterGenerator() {
                super();
            }

            @Override
            public Iterator<IntentFilter.AuthorityEntry> generate(PackageParser.ActivityIntentInfo info) {
                return info.authoritiesIterator();
            }
        }

        private <T> void getIntentListSubset(List<PackageParser.ActivityIntentInfo> intentList, IterGenerator<T> generator, Iterator<T> searchIterator) {
            while (searchIterator.hasNext() && intentList.size() != 0) {
                T searchAction = searchIterator.next();
                Iterator<PackageParser.ActivityIntentInfo> intentIter = intentList.iterator();
                while (intentIter.hasNext()) {
                    PackageParser.ActivityIntentInfo intentInfo = intentIter.next();
                    boolean selectionFound = false;
                    Iterator<T> intentSelectionIter = generator.generate(intentInfo);
                    while (true) {
                        if (intentSelectionIter != null && intentSelectionIter.hasNext()) {
                            T intentSelection = intentSelectionIter.next();
                            if (intentSelection != null && intentSelection.equals(searchAction)) {
                                selectionFound = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!selectionFound) {
                        intentIter.remove();
                    }
                }
            }
        }

        private boolean isProtectedAction(PackageParser.ActivityIntentInfo filter) {
            Iterator<String> actionsIter = filter.actionsIterator();
            while (actionsIter != null && actionsIter.hasNext()) {
                String filterAction = actionsIter.next();
                if (PackageManagerService.PROTECTED_ACTIONS.contains(filterAction)) {
                    return true;
                }
            }
            return false;
        }

        private void adjustPriority(List<PackageParser.Activity> systemActivities, PackageParser.ActivityIntentInfo intent) {
            if (intent.getPriority() <= 0) {
                return;
            }
            ActivityInfo activityInfo = intent.activity.info;
            ApplicationInfo applicationInfo = activityInfo.applicationInfo;
            boolean privilegedApp = (applicationInfo.privateFlags & 8) != 0;
            if (!privilegedApp) {
                Slog.w(PackageManagerService.TAG, "Non-privileged app; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                intent.setPriority(0);
                return;
            }
            if (systemActivities == null) {
                if (isProtectedAction(intent)) {
                    if (PackageManagerService.this.mDeferProtectedFilters) {
                        PackageManagerService.this.mProtectedFilters.add(intent);
                        if (PackageManagerService.DEBUG_FILTERS) {
                            Slog.i(PackageManagerService.TAG, "Protected action; save for later; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                            return;
                        }
                        return;
                    }
                    if (PackageManagerService.DEBUG_FILTERS && PackageManagerService.this.mSetupWizardPackage == null) {
                        Slog.i(PackageManagerService.TAG, "No setup wizard; All protected intents capped to priority 0");
                    }
                    if (!intent.activity.info.packageName.equals(PackageManagerService.this.mSetupWizardPackage)) {
                        Slog.w(PackageManagerService.TAG, "Protected action; cap priority to 0; package: " + intent.activity.info.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                        intent.setPriority(0);
                        return;
                    } else {
                        if (PackageManagerService.DEBUG_FILTERS) {
                            Slog.i(PackageManagerService.TAG, "Found setup wizard; allow priority " + intent.getPriority() + "; package: " + intent.activity.info.packageName + " activity: " + intent.activity.className + " priority: " + intent.getPriority());
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            PackageParser.Activity foundActivity = findMatchingActivity(systemActivities, activityInfo);
            if (foundActivity == null) {
                if (PackageManagerService.DEBUG_FILTERS) {
                    Slog.i(PackageManagerService.TAG, "New activity; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                }
                intent.setPriority(0);
                return;
            }
            List<PackageParser.ActivityIntentInfo> intentListCopy = new ArrayList<>(foundActivity.intents);
            findFilters(intent);
            Iterator<String> actionsIterator = intent.actionsIterator();
            if (actionsIterator != null) {
                getIntentListSubset(intentListCopy, new ActionIterGenerator(), actionsIterator);
                if (intentListCopy.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched action; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                    }
                    intent.setPriority(0);
                    return;
                }
            }
            Iterator<String> categoriesIterator = intent.categoriesIterator();
            if (categoriesIterator != null) {
                getIntentListSubset(intentListCopy, new CategoriesIterGenerator(), categoriesIterator);
                if (intentListCopy.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched category; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                    }
                    intent.setPriority(0);
                    return;
                }
            }
            Iterator<String> schemesIterator = intent.schemesIterator();
            if (schemesIterator != null) {
                getIntentListSubset(intentListCopy, new SchemesIterGenerator(), schemesIterator);
                if (intentListCopy.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched scheme; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                    }
                    intent.setPriority(0);
                    return;
                }
            }
            Iterator<IntentFilter.AuthorityEntry> authoritiesIterator = intent.authoritiesIterator();
            if (authoritiesIterator != null) {
                getIntentListSubset(intentListCopy, new AuthoritiesIterGenerator(), authoritiesIterator);
                if (intentListCopy.size() == 0) {
                    if (PackageManagerService.DEBUG_FILTERS) {
                        Slog.i(PackageManagerService.TAG, "Mismatched authority; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                    }
                    intent.setPriority(0);
                    return;
                }
            }
            int cappedPriority = 0;
            for (int i = intentListCopy.size() - 1; i >= 0; i--) {
                cappedPriority = Math.max(cappedPriority, intentListCopy.get(i).getPriority());
            }
            if (intent.getPriority() > cappedPriority) {
                if (PackageManagerService.DEBUG_FILTERS) {
                    Slog.i(PackageManagerService.TAG, "Found matching filter(s); cap priority to " + cappedPriority + "; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                }
                intent.setPriority(cappedPriority);
            }
        }

        public final void addActivity(PackageParser.Activity a, String type) {
            this.mActivities.put(a.getComponentName(), a);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + type + " " + (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel : a.info.name) + ":");
            }
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "    Class=" + a.info.name);
            }
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ActivityIntentInfo intent = (PackageParser.ActivityIntentInfo) a.intents.get(j);
                if ("activity".equals(type)) {
                    PackageSetting ps = PackageManagerService.this.mSettings.getDisabledSystemPkgLPr(intent.activity.info.packageName);
                    List<PackageParser.Activity> systemActivities = (ps == null || ps.pkg == null) ? null : ps.pkg.activities;
                    adjustPriority(systemActivities, intent);
                }
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Activity " + a.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeActivity(PackageParser.Activity a, String type) {
            this.mActivities.remove(a.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + type + " " + (a.info.nonLocalizedLabel != null ? a.info.nonLocalizedLabel : a.info.name) + ":");
                Log.v(PackageManagerService.TAG, "    Class=" + a.info.name);
            }
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ActivityIntentInfo intent = (PackageParser.ActivityIntentInfo) a.intents.get(j);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ActivityIntentInfo filter, List<ResolveInfo> dest) {
            ActivityInfo filterAi = filter.activity.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ActivityInfo destAi = dest.get(i).activityInfo;
                if (destAi.name == filterAi.name && destAi.packageName == filterAi.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ActivityIntentInfo[] newArray(int size) {
            return new PackageParser.ActivityIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ActivityIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            PackageParser.Package p = filter.activity.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null || (ps.pkgFlags & 1) != 0) {
                return false;
            }
            return ps.getStopped(userId);
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ActivityIntentInfo info) {
            return packageName.equals(info.activity.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ActivityIntentInfo info, int match, int userId) {
            ActivityInfo ai;
            if (!PackageManagerService.sUserManager.exists(userId) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(info.activity.info, this.mFlags, userId)) {
                return null;
            }
            PackageParser.Activity activity = info.activity;
            PackageSetting ps = (PackageSetting) activity.owner.mExtras;
            if (ps == null || (ai = PackageParser.generateActivityInfo(activity, this.mFlags, ps.readUserState(userId), userId)) == null) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.activityInfo = ai;
            if ((this.mFlags & 64) != 0) {
                res.filter = info;
            }
            if (info != null) {
                res.handleAllWebDataURI = info.handleAllWebDataURI();
            }
            res.priority = info.getPriority();
            res.preferredOrder = activity.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            if (PackageManagerService.this.userNeedsBadging(userId)) {
                res.noResourceId = true;
            } else {
                res.icon = info.icon;
            }
            res.iconResourceId = info.icon;
            res.system = res.activityInfo.applicationInfo.isSystemApp();
            return res;
        }

        @Override
        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter out, String prefix, PackageParser.ActivityIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.activity)));
            out.print(' ');
            filter.activity.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        @Override
        protected Object filterToLabel(PackageParser.ActivityIntentInfo filter) {
            return filter.activity;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            PackageParser.Activity activity = (PackageParser.Activity) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(activity)));
            out.print(' ');
            activity.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    private final class ServiceIntentResolver extends IntentResolver<PackageParser.ServiceIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, PackageParser.Service> mServices;

        ServiceIntentResolver(PackageManagerService this$0, ServiceIntentResolver serviceIntentResolver) {
            this();
        }

        private ServiceIntentResolver() {
            this.mServices = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Service> packageServices, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageServices == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageServices.size();
            ArrayList<PackageParser.ServiceIntentInfo[]> listCut = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                ArrayList<PackageParser.ServiceIntentInfo> intentFilters = packageServices.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PackageParser.ServiceIntentInfo[] array = new PackageParser.ServiceIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addService(PackageParser.Service s) {
            this.mServices.put(s.getComponentName(), s);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + (s.info.nonLocalizedLabel != null ? s.info.nonLocalizedLabel : s.info.name) + ":");
                Log.v(PackageManagerService.TAG, "    Class=" + s.info.name);
            }
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ServiceIntentInfo intent = (PackageParser.ServiceIntentInfo) s.intents.get(j);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Service " + s.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeService(PackageParser.Service s) {
            this.mServices.remove(s.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + (s.info.nonLocalizedLabel != null ? s.info.nonLocalizedLabel : s.info.name) + ":");
                Log.v(PackageManagerService.TAG, "    Class=" + s.info.name);
            }
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ServiceIntentInfo intent = (PackageParser.ServiceIntentInfo) s.intents.get(j);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ServiceIntentInfo filter, List<ResolveInfo> dest) {
            ServiceInfo filterSi = filter.service.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ServiceInfo destAi = dest.get(i).serviceInfo;
                if (destAi.name == filterSi.name && destAi.packageName == filterSi.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ServiceIntentInfo[] newArray(int size) {
            return new PackageParser.ServiceIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ServiceIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            PackageParser.Package p = filter.service.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null || (ps.pkgFlags & 1) != 0) {
                return false;
            }
            return ps.getStopped(userId);
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ServiceIntentInfo info) {
            return packageName.equals(info.service.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ServiceIntentInfo filter, int match, int userId) {
            ServiceInfo si;
            if (!PackageManagerService.sUserManager.exists(userId) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(filter.service.info, this.mFlags, userId)) {
                return null;
            }
            PackageParser.Service service = filter.service;
            PackageSetting ps = (PackageSetting) service.owner.mExtras;
            if (ps == null || (si = PackageParser.generateServiceInfo(service, this.mFlags, ps.readUserState(userId), userId)) == null) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.serviceInfo = si;
            if ((this.mFlags & 64) != 0) {
                res.filter = filter;
            }
            res.priority = filter.getPriority();
            res.preferredOrder = service.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = filter.hasDefault;
            res.labelRes = filter.labelRes;
            res.nonLocalizedLabel = filter.nonLocalizedLabel;
            res.icon = filter.icon;
            res.system = res.serviceInfo.applicationInfo.isSystemApp();
            return res;
        }

        @Override
        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter out, String prefix, PackageParser.ServiceIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.service)));
            out.print(' ');
            filter.service.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        @Override
        protected Object filterToLabel(PackageParser.ServiceIntentInfo filter) {
            return filter.service;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            PackageParser.Service service = (PackageParser.Service) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(service)));
            out.print(' ');
            service.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    private final class ProviderIntentResolver extends IntentResolver<PackageParser.ProviderIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, PackageParser.Provider> mProviders;

        ProviderIntentResolver(PackageManagerService this$0, ProviderIntentResolver providerIntentResolver) {
            this();
        }

        private ProviderIntentResolver() {
            this.mProviders = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Provider> packageProviders, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageProviders == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageProviders.size();
            ArrayList<PackageParser.ProviderIntentInfo[]> listCut = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                ArrayList<PackageParser.ProviderIntentInfo> intentFilters = packageProviders.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    PackageParser.ProviderIntentInfo[] array = new PackageParser.ProviderIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addProvider(PackageParser.Provider p) {
            if (this.mProviders.containsKey(p.getComponentName())) {
                Slog.w(PackageManagerService.TAG, "Provider " + p.getComponentName() + " already defined; ignoring");
                return;
            }
            this.mProviders.put(p.getComponentName(), p);
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + (p.info.nonLocalizedLabel != null ? p.info.nonLocalizedLabel : p.info.name) + ":");
                Log.v(PackageManagerService.TAG, "    Class=" + p.info.name);
            }
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ProviderIntentInfo intent = (PackageParser.ProviderIntentInfo) p.intents.get(j);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Provider " + p.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeProvider(PackageParser.Provider p) {
            this.mProviders.remove(p.getComponentName());
            if (PackageManagerService.DEBUG_SHOW_INFO) {
                Log.v(PackageManagerService.TAG, "  " + (p.info.nonLocalizedLabel != null ? p.info.nonLocalizedLabel : p.info.name) + ":");
                Log.v(PackageManagerService.TAG, "    Class=" + p.info.name);
            }
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ProviderIntentInfo intent = (PackageParser.ProviderIntentInfo) p.intents.get(j);
                if (PackageManagerService.DEBUG_SHOW_INFO) {
                    Log.v(PackageManagerService.TAG, "    IntentFilter:");
                    intent.dump(new LogPrinter(2, PackageManagerService.TAG), "      ");
                }
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(PackageParser.ProviderIntentInfo filter, List<ResolveInfo> dest) {
            ProviderInfo filterPi = filter.provider.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ProviderInfo destPi = dest.get(i).providerInfo;
                if (destPi.name == filterPi.name && destPi.packageName == filterPi.packageName) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected PackageParser.ProviderIntentInfo[] newArray(int size) {
            return new PackageParser.ProviderIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ProviderIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            PackageParser.Package p = filter.provider.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null || (ps.pkgFlags & 1) != 0) {
                return false;
            }
            return ps.getStopped(userId);
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ProviderIntentInfo info) {
            return packageName.equals(info.provider.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ProviderIntentInfo filter, int match, int userId) {
            ProviderInfo pi;
            if (!PackageManagerService.sUserManager.exists(userId) || !PackageManagerService.this.mSettings.isEnabledAndMatchLPr(filter.provider.info, this.mFlags, userId)) {
                return null;
            }
            PackageParser.Provider provider = filter.provider;
            PackageSetting ps = (PackageSetting) provider.owner.mExtras;
            if (ps == null || (pi = PackageParser.generateProviderInfo(provider, this.mFlags, ps.readUserState(userId), userId)) == null) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.providerInfo = pi;
            if ((this.mFlags & 64) != 0) {
                res.filter = filter;
            }
            res.priority = filter.getPriority();
            res.preferredOrder = provider.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = filter.hasDefault;
            res.labelRes = filter.labelRes;
            res.nonLocalizedLabel = filter.nonLocalizedLabel;
            res.icon = filter.icon;
            res.system = res.providerInfo.applicationInfo.isSystemApp();
            return res;
        }

        @Override
        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter out, String prefix, PackageParser.ProviderIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.provider)));
            out.print(' ');
            filter.provider.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        @Override
        protected Object filterToLabel(PackageParser.ProviderIntentInfo filter) {
            return filter.provider;
        }

        @Override
        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            PackageParser.Provider provider = (PackageParser.Provider) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(provider)));
            out.print(' ');
            provider.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    private static final class EphemeralIntentResolver extends IntentResolver<EphemeralResolveInfo.EphemeralResolveIntentInfo, EphemeralResolveInfo> {
        EphemeralIntentResolver(EphemeralIntentResolver ephemeralIntentResolver) {
            this();
        }

        private EphemeralIntentResolver() {
        }

        @Override
        protected EphemeralResolveInfo.EphemeralResolveIntentInfo[] newArray(int size) {
            return new EphemeralResolveInfo.EphemeralResolveIntentInfo[size];
        }

        @Override
        protected boolean isPackageForFilter(String packageName, EphemeralResolveInfo.EphemeralResolveIntentInfo info) {
            return true;
        }

        @Override
        protected EphemeralResolveInfo newResult(EphemeralResolveInfo.EphemeralResolveIntentInfo info, int match, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            return info.getEphemeralResolveInfo();
        }
    }

    final void sendPackageBroadcast(final String action, final String pkg, final Bundle extras, final int flags, final String targetPkg, final IIntentReceiver finishedReceiver, final int[] userIds) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    IActivityManager am = ActivityManagerNative.getDefault();
                    if (am == null) {
                        return;
                    }
                    int[] resolvedUserIds = userIds == null ? am.getRunningUserIds() : userIds;
                    for (int id : resolvedUserIds) {
                        Intent intent = new Intent(action, pkg != null ? Uri.fromParts("package", pkg, null) : null);
                        if (extras != null) {
                            intent.putExtras(extras);
                        }
                        if (targetPkg != null) {
                            intent.setPackage(targetPkg);
                        }
                        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                        if (uid > 0 && UserHandle.getUserId(uid) != id) {
                            intent.putExtra("android.intent.extra.UID", UserHandle.getUid(id, UserHandle.getAppId(uid)));
                        }
                        intent.putExtra("android.intent.extra.user_handle", id);
                        intent.addFlags(flags | 67108864);
                        if (PackageManagerService.DEBUG_BROADCASTS) {
                            RuntimeException here = new RuntimeException("here");
                            here.fillInStackTrace();
                            Slog.d(PackageManagerService.TAG, "Sending to user " + id + ": " + intent.toShortString(false, true, false, false) + " " + intent.getExtras(), here);
                        }
                        am.broadcastIntent((IApplicationThread) null, intent, (String) null, finishedReceiver, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, finishedReceiver != null, false, id);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    private boolean isExternalMediaAvailable() {
        if (this.mMediaMounted) {
            return true;
        }
        return Environment.isExternalStorageEmulated();
    }

    public PackageCleanItem nextPackageToClean(PackageCleanItem lastPackage) {
        synchronized (this.mPackages) {
            if (!isExternalMediaAvailable()) {
                return null;
            }
            ArrayList<PackageCleanItem> pkgs = this.mSettings.mPackagesToBeCleaned;
            if (lastPackage != null) {
                pkgs.remove(lastPackage);
            }
            if (pkgs.size() <= 0) {
                return null;
            }
            return pkgs.get(0);
        }
    }

    void schedulePackageCleaning(String packageName, int userId, boolean andCode) {
        Message msg = this.mHandler.obtainMessage(7, userId, andCode ? 1 : 0, packageName);
        if (this.mSystemReady) {
            msg.sendToTarget();
            return;
        }
        if (this.mPostSystemReadyMessages == null) {
            this.mPostSystemReadyMessages = new ArrayList<>();
        }
        this.mPostSystemReadyMessages.add(msg);
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
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am == null) {
                return;
            }
            try {
                am.startService((IApplicationThread) null, intent, (String) null, this.mContext.getOpPackageName(), 0);
            } catch (RemoteException e) {
            }
        }
    }

    public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, int userId) {
        int installFlags2;
        UserHandle user;
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, true, "installPackageAsUser");
        if (isUserRestricted(userId, "no_install_apps")) {
            if (observer != null) {
                try {
                    observer.onPackageInstalled("", -111, (String) null, (Bundle) null);
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            return;
        }
        if (callingUid == SHELL_UID || callingUid == 0) {
            installFlags2 = installFlags | 32;
        } else {
            installFlags2 = installFlags & (-33) & (-65);
        }
        if ((installFlags2 & 64) != 0) {
            user = UserHandle.ALL;
        } else {
            user = new UserHandle(userId);
        }
        if ((installFlags2 & 256) != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") == -1) {
            throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
        }
        File originFile = new File(originPath);
        OriginInfo origin = OriginInfo.fromUntrustedFile(originFile);
        Message msg = this.mHandler.obtainMessage(5);
        VerificationInfo verificationInfo = new VerificationInfo(null, null, -1, callingUid);
        InstallParams params = new InstallParams(origin, null, observer, installFlags2, installerPackageName, null, verificationInfo, user, null, null, null);
        params.setTraceMethod("installAsUser").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(1048576L, "installAsUser", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(1048576L, "queueInstall", System.identityHashCode(msg.obj));
        this.mHandler.sendMessage(msg);
    }

    void installStage(String packageName, File stagedDir, String stagedCid, IPackageInstallObserver2 observer, PackageInstaller.SessionParams sessionParams, String installerPackageName, int installerUid, UserHandle user, Certificate[][] certificates) {
        OriginInfo origin;
        if (DEBUG_EPHEMERAL && (sessionParams.installFlags & 2048) != 0) {
            Slog.d(TAG, "Ephemeral install of " + packageName);
        }
        VerificationInfo verificationInfo = new VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, installerUid);
        if (stagedDir != null) {
            origin = OriginInfo.fromStagedFile(stagedDir);
        } else {
            origin = OriginInfo.fromStagedContainer(stagedCid);
        }
        Message msg = this.mHandler.obtainMessage(5);
        InstallParams params = new InstallParams(origin, null, observer, sessionParams.installFlags, installerPackageName, sessionParams.volumeUuid, verificationInfo, user, sessionParams.abiOverride, sessionParams.grantedRuntimePermissions, certificates);
        params.setTraceMethod("installStage").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(1048576L, "installStage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(1048576L, "queueInstall", System.identityHashCode(msg.obj));
        this.mHandler.sendMessage(msg);
    }

    private void sendPackageAddedForUser(String packageName, PackageSetting pkgSetting, int userId) {
        sendPackageAddedForUser(packageName, !isSystemApp(pkgSetting) ? isUpdatedSystemApp(pkgSetting) : true, pkgSetting.appId, userId);
    }

    private void sendPackageAddedForUser(String packageName, boolean isSystem, int appId, int userId) {
        Bundle extras = new Bundle(1);
        extras.putInt("android.intent.extra.UID", UserHandle.getUid(userId, appId));
        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, new int[]{userId});
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            if (!isSystem || !am.isUserRunning(userId, 0)) {
                return;
            }
            Intent bcIntent = new Intent("android.intent.action.BOOT_COMPLETED").addFlags(32).setPackage(packageName);
            am.broadcastIntent((IApplicationThread) null, bcIntent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, userId);
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to bootstrap installed package", e);
        }
    }

    public boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, true, true, "setApplicationHiddenSetting for user " + userId);
        if (hidden && isPackageDeviceAdmin(packageName, userId)) {
            Slog.w(TAG, "Not hiding package " + packageName + ": has active device admin");
            return false;
        }
        long callingId = Binder.clearCallingIdentity();
        boolean sendAdded = false;
        boolean sendRemoved = false;
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    return false;
                }
                if (pkgSetting.getHidden(userId) != hidden) {
                    pkgSetting.setHidden(hidden, userId);
                    this.mSettings.writePackageRestrictionsLPr(userId);
                    if (hidden) {
                        sendRemoved = true;
                    } else {
                        sendAdded = true;
                    }
                }
                if (sendAdded) {
                    sendPackageAddedForUser(packageName, pkgSetting, userId);
                    return true;
                }
                if (!sendRemoved) {
                    return false;
                }
                killApplication(packageName, UserHandle.getUid(userId, pkgSetting.appId), "hiding pkg");
                sendApplicationHiddenForUser(packageName, pkgSetting, userId);
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void sendApplicationHiddenForUser(String packageName, PackageSetting pkgSetting, int userId) {
        PackageRemovedInfo info = new PackageRemovedInfo();
        info.removedPackage = packageName;
        info.removedUsers = new int[]{userId};
        info.uid = UserHandle.getUid(userId, pkgSetting.appId);
        info.sendPackageRemovedBroadcasts(true);
    }

    private void sendPackagesSuspendedForUser(String[] pkgList, int userId, boolean suspended) {
        if (pkgList.length <= 0) {
            return;
        }
        Bundle extras = new Bundle(1);
        extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
        sendPackageBroadcast(suspended ? "android.intent.action.PACKAGES_SUSPENDED" : "android.intent.action.PACKAGES_UNSUSPENDED", null, extras, 1073741824, null, null, new int[]{userId});
    }

    public boolean getApplicationHiddenSettingAsUser(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getApplicationHidden for user " + userId);
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    return true;
                }
                return pkgSetting.getHidden(userId);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int installExistingPackageAsUser(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, true, true, "installExistingPackage for user " + userId);
        if (isUserRestricted(userId, "no_install_apps")) {
            return -111;
        }
        long callingId = Binder.clearCallingIdentity();
        boolean installed = false;
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                if (pkgSetting != null) {
                    if (!pkgSetting.getInstalled(userId)) {
                        pkgSetting.setInstalled(true, userId);
                        pkgSetting.setHidden(false, userId);
                        this.mSettings.writePackageRestrictionsLPr(userId);
                        installed = true;
                    }
                    if (installed) {
                        if (pkgSetting.pkg != null) {
                            synchronized (this.mInstallLock) {
                                prepareAppDataAfterInstallLIF(pkgSetting.pkg);
                            }
                        }
                        sendPackageAddedForUser(packageName, pkgSetting, userId);
                    }
                    return 1;
                }
                return -3;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    boolean isUserRestricted(int userId, String restrictionKey) {
        Bundle restrictions = sUserManager.getUserRestrictions(userId);
        if (!restrictions.getBoolean(restrictionKey, false)) {
            return false;
        }
        Log.w(TAG, "User is restricted: " + restrictionKey);
        return true;
    }

    public String[] setPackagesSuspendedAsUser(String[] packageNames, boolean suspended, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "setPackagesSuspended for user " + userId);
        if (ArrayUtils.isEmpty(packageNames)) {
            return packageNames;
        }
        List<String> changedPackages = new ArrayList<>(packageNames.length);
        List<String> unactionedPackages = new ArrayList<>(packageNames.length);
        long callingId = Binder.clearCallingIdentity();
        for (String packageName : packageNames) {
            try {
                boolean changed = false;
                synchronized (this.mPackages) {
                    PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                    if (pkgSetting == null) {
                        Slog.w(TAG, "Could not find package setting for package \"" + packageName + "\". Skipping suspending/un-suspending.");
                        unactionedPackages.add(packageName);
                    } else {
                        int appId = pkgSetting.appId;
                        if (pkgSetting.getSuspended(userId) != suspended) {
                            if (!canSuspendPackageForUserLocked(packageName, userId)) {
                                unactionedPackages.add(packageName);
                            } else {
                                pkgSetting.setSuspended(suspended, userId);
                                this.mSettings.writePackageRestrictionsLPr(userId);
                                changed = true;
                                changedPackages.add(packageName);
                                if (!changed) {
                                }
                            }
                        } else if (!changed && suspended) {
                            killApplication(packageName, UserHandle.getUid(userId, appId), "suspending package");
                        }
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingId);
                throw th;
            }
        }
        Binder.restoreCallingIdentity(callingId);
        if (!changedPackages.isEmpty()) {
            sendPackagesSuspendedForUser((String[]) changedPackages.toArray(new String[changedPackages.size()]), userId, suspended);
        }
        return (String[]) unactionedPackages.toArray(new String[unactionedPackages.size()]);
    }

    public boolean isPackageSuspendedForUser(String packageName, int userId) {
        boolean suspended;
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "isPackageSuspendedForUser for user " + userId);
        synchronized (this.mPackages) {
            PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
            if (pkgSetting == null) {
                throw new IllegalArgumentException("Unknown target package: " + packageName);
            }
            suspended = pkgSetting.getSuspended(userId);
        }
        return suspended;
    }

    private boolean canSuspendPackageForUserLocked(String packageName, int userId) {
        if (isPackageDeviceAdmin(packageName, userId)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": has an active device admin");
            return false;
        }
        String activeLauncherPackageName = getActiveLauncherPackageName(userId);
        if (packageName.equals(activeLauncherPackageName)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": contains the active launcher");
            return false;
        }
        if (packageName.equals(this.mRequiredInstallerPackage)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": required for package installation");
            return false;
        }
        if (packageName.equals(this.mRequiredVerifierPackage)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": required for package verification");
            return false;
        }
        if (packageName.equals(getDefaultDialerPackageName(userId))) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": is the default dialer");
            return false;
        }
        return true;
    }

    private String getActiveLauncherPackageName(int userId) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfo = resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 65536, userId);
        if (resolveInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.packageName;
    }

    private String getDefaultDialerPackageName(int userId) {
        String defaultDialerPackageNameLPw;
        synchronized (this.mPackages) {
            defaultDialerPackageNameLPw = this.mSettings.getDefaultDialerPackageNameLPw(userId);
        }
        return defaultDialerPackageNameLPw;
    }

    public void verifyPendingInstall(int id, int verificationCode) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can verify applications");
        Message msg = this.mHandler.obtainMessage(15);
        PackageVerificationResponse response = new PackageVerificationResponse(verificationCode, Binder.getCallingUid());
        msg.arg1 = id;
        msg.obj = response;
        this.mHandler.sendMessage(msg);
    }

    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can extend verification timeouts");
        PackageVerificationState state = this.mPendingVerification.get(id);
        PackageVerificationResponse response = new PackageVerificationResponse(verificationCodeAtTimeout, Binder.getCallingUid());
        if (millisecondsToDelay > 3600000) {
            millisecondsToDelay = 3600000;
        }
        if (millisecondsToDelay < 0) {
            millisecondsToDelay = 0;
        }
        if (verificationCodeAtTimeout == 1 || verificationCodeAtTimeout != -1) {
        }
        if (state == null || state.timeoutExtended()) {
            return;
        }
        state.extendTimeout();
        Message msg = this.mHandler.obtainMessage(15);
        msg.arg1 = id;
        msg.obj = response;
        this.mHandler.sendMessageDelayed(msg, millisecondsToDelay);
    }

    private void broadcastPackageVerified(int verificationId, Uri packageUri, int verificationCode, UserHandle user) {
        Intent intent = new Intent("android.intent.action.PACKAGE_VERIFIED");
        intent.setDataAndType(packageUri, PACKAGE_MIME_TYPE);
        intent.addFlags(1);
        intent.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
        intent.putExtra("android.content.pm.extra.VERIFICATION_RESULT", verificationCode);
        this.mContext.sendBroadcastAsUser(intent, user, "android.permission.PACKAGE_VERIFICATION_AGENT");
    }

    private ComponentName matchComponentForVerifier(String packageName, List<ResolveInfo> receivers) {
        ActivityInfo targetReceiver = null;
        int NR = receivers.size();
        int i = 0;
        while (true) {
            if (i >= NR) {
                break;
            }
            ResolveInfo info = receivers.get(i);
            if (info.activityInfo == null || !packageName.equals(info.activityInfo.packageName)) {
                i++;
            } else {
                targetReceiver = info.activityInfo;
                break;
            }
        }
        if (targetReceiver == null) {
            return null;
        }
        return new ComponentName(targetReceiver.packageName, targetReceiver.name);
    }

    private List<ComponentName> matchVerifiers(PackageInfoLite pkgInfo, List<ResolveInfo> receivers, PackageVerificationState verificationState) {
        int verifierUid;
        if (pkgInfo.verifiers.length == 0) {
            return null;
        }
        int N = pkgInfo.verifiers.length;
        List<ComponentName> sufficientVerifiers = new ArrayList<>(N + 1);
        for (int i = 0; i < N; i++) {
            VerifierInfo verifierInfo = pkgInfo.verifiers[i];
            ComponentName comp = matchComponentForVerifier(verifierInfo.packageName, receivers);
            if (comp != null && (verifierUid = getUidForVerifier(verifierInfo)) != -1) {
                if (DEBUG_VERIFY) {
                    Slog.d(TAG, "Added sufficient verifier " + verifierInfo.packageName + " with the correct signature");
                }
                sufficientVerifiers.add(comp);
                verificationState.addSufficientVerifier(verifierUid);
            }
        }
        return sufficientVerifiers;
    }

    private int getUidForVerifier(VerifierInfo verifierInfo) {
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(verifierInfo.packageName);
            if (pkg == null) {
                return -1;
            }
            if (pkg.mSignatures.length != 1) {
                Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " has more than one signature; ignoring");
                return -1;
            }
            try {
                Signature verifierSig = pkg.mSignatures[0];
                PublicKey publicKey = verifierSig.getPublicKey();
                byte[] expectedPublicKey = publicKey.getEncoded();
                byte[] actualPublicKey = verifierInfo.publicKey.getEncoded();
                if (!Arrays.equals(actualPublicKey, expectedPublicKey)) {
                    Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " does not have the expected public key; ignoring");
                    return -1;
                }
                return pkg.applicationInfo.uid;
            } catch (CertificateException e) {
                return -1;
            }
        }
    }

    public void finishPackageInstall(int token, boolean didLaunch) {
        enforceSystemOrRoot("Only the system is allowed to finish installs");
        if (DEBUG_INSTALL) {
            Slog.v(TAG, "BM finishing package install for " + token);
        }
        Trace.asyncTraceEnd(1048576L, "restore", token);
        Message msg = this.mHandler.obtainMessage(9, token, didLaunch ? 1 : 0);
        this.mHandler.sendMessage(msg);
    }

    private long getVerificationTimeout() {
        return Settings.Global.getLong(this.mContext.getContentResolver(), "verifier_timeout", 10000L);
    }

    private int getDefaultVerificationResponse() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_default_response", 1);
    }

    private boolean isVerificationEnabled(int userId, int installFlags) {
        if ((installFlags & 2048) != 0) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "INSTALL_EPHEMERAL so skipping verification");
            }
            return false;
        }
        boolean ensureVerifyAppsEnabled = isUserRestricted(userId, "ensure_verify_apps");
        if ((installFlags & 32) != 0) {
            if (ActivityManager.isRunningInTestHarness()) {
                return false;
            }
            if (ensureVerifyAppsEnabled) {
                return true;
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) == 0) {
                return false;
            }
        }
        return ensureVerifyAppsEnabled || Settings.Global.getInt(this.mContext.getContentResolver(), "package_verifier_enable", 1) == 1;
    }

    public void verifyIntentFilter(int id, int verificationCode, List<String> failedDomains) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", "Only intentfilter verification agents can verify applications");
        Message msg = this.mHandler.obtainMessage(18);
        IntentFilterVerificationResponse response = new IntentFilterVerificationResponse(Binder.getCallingUid(), verificationCode, failedDomains);
        msg.arg1 = id;
        msg.obj = response;
        this.mHandler.sendMessage(msg);
    }

    public int getIntentVerificationStatus(String packageName, int userId) {
        int intentFilterVerificationStatusLPr;
        synchronized (this.mPackages) {
            intentFilterVerificationStatusLPr = this.mSettings.getIntentFilterVerificationStatusLPr(packageName, userId);
        }
        return intentFilterVerificationStatusLPr;
    }

    public boolean updateIntentVerificationStatus(String packageName, int status, int userId) {
        boolean result;
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        synchronized (this.mPackages) {
            result = this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, status, userId);
        }
        if (result) {
            scheduleWritePackageRestrictionsLocked(userId);
        }
        return result;
    }

    public ParceledListSlice<IntentFilterVerificationInfo> getIntentFilterVerifications(String packageName) {
        ParceledListSlice<IntentFilterVerificationInfo> parceledListSlice;
        synchronized (this.mPackages) {
            parceledListSlice = new ParceledListSlice<>(this.mSettings.getIntentFilterVerificationsLPr(packageName));
        }
        return parceledListSlice;
    }

    public ParceledListSlice<IntentFilter> getAllIntentFilters(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return ParceledListSlice.emptyList();
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null || pkg.activities == null) {
                return ParceledListSlice.emptyList();
            }
            int count = pkg.activities.size();
            ArrayList<IntentFilter> result = new ArrayList<>();
            for (int n = 0; n < count; n++) {
                PackageParser.Activity activity = (PackageParser.Activity) pkg.activities.get(n);
                if (activity.intents != null && activity.intents.size() > 0) {
                    result.addAll(activity.intents);
                }
            }
            return new ParceledListSlice<>(result);
        }
    }

    public boolean setDefaultBrowserPackageName(String packageName, int userId) {
        boolean result;
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        synchronized (this.mPackages) {
            result = this.mSettings.setDefaultBrowserPackageNameLPw(packageName, userId);
            if (packageName != null) {
                result |= updateIntentVerificationStatus(packageName, 2, userId);
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultBrowserLPr(packageName, userId);
            }
        }
        return result;
    }

    public String getDefaultBrowserPackageName(int userId) {
        String defaultBrowserPackageNameLPw;
        synchronized (this.mPackages) {
            defaultBrowserPackageNameLPw = this.mSettings.getDefaultBrowserPackageNameLPw(userId);
        }
        return defaultBrowserPackageNameLPw;
    }

    private int getUnknownSourcesSettings() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "install_non_market_apps", -1);
    }

    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        PackageSetting installerPackageSetting;
        Signature[] callerSignature;
        PackageSetting setting;
        int uid = Binder.getCallingUid();
        synchronized (this.mPackages) {
            PackageSetting targetPackageSetting = this.mSettings.mPackages.get(targetPackage);
            if (targetPackageSetting == null) {
                throw new IllegalArgumentException("Unknown target package: " + targetPackage);
            }
            if (installerPackageName != null) {
                installerPackageSetting = this.mSettings.mPackages.get(installerPackageName);
                if (installerPackageSetting == null) {
                    throw new IllegalArgumentException("Unknown installer package: " + installerPackageName);
                }
            } else {
                installerPackageSetting = null;
            }
            Object obj = this.mSettings.getUserIdLPr(uid);
            if (obj != null) {
                if (obj instanceof SharedUserSetting) {
                    callerSignature = ((SharedUserSetting) obj).signatures.mSignatures;
                } else if (obj instanceof PackageSetting) {
                    callerSignature = ((PackageSetting) obj).signatures.mSignatures;
                } else {
                    throw new SecurityException("Bad object " + obj + " for uid " + uid);
                }
                if (installerPackageSetting != null && compareSignatures(callerSignature, installerPackageSetting.signatures.mSignatures) != 0) {
                    throw new SecurityException("Caller does not have same cert as new installer package " + installerPackageName);
                }
                if (targetPackageSetting.installerPackageName != null && (setting = this.mSettings.mPackages.get(targetPackageSetting.installerPackageName)) != null && compareSignatures(callerSignature, setting.signatures.mSignatures) != 0) {
                    throw new SecurityException("Caller does not have same cert as old installer package " + targetPackageSetting.installerPackageName);
                }
                targetPackageSetting.installerPackageName = installerPackageName;
                if (installerPackageName != null) {
                    this.mSettings.mInstallerPackages.add(installerPackageName);
                }
                scheduleWriteSettingsLocked();
            } else {
                throw new SecurityException("Unknown calling UID: " + uid);
            }
        }
    }

    private void processPendingInstall(final InstallArgs args, final int currentStatus) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.mHandler.removeCallbacks(this);
                PackageInstalledInfo res = new PackageInstalledInfo();
                res.setReturnCode(currentStatus);
                res.uid = -1;
                res.pkg = null;
                res.removedInfo = null;
                if (res.returnCode == 1) {
                    args.doPreInstall(res.returnCode);
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.installPackageTracedLI(args, res);
                    }
                    args.doPostInstall(res.returnCode, res.uid);
                }
                boolean update = (res.removedInfo == null || res.removedInfo.removedPackage == null) ? false : true;
                int flags = res.pkg == null ? 0 : res.pkg.applicationInfo.flags;
                boolean doRestore = (update || (32768 & flags) == 0) ? false : true;
                if (PackageManagerService.this.mNextInstallToken < 0) {
                    PackageManagerService.this.mNextInstallToken = 1;
                }
                PackageManagerService packageManagerService = PackageManagerService.this;
                int token = packageManagerService.mNextInstallToken;
                packageManagerService.mNextInstallToken = token + 1;
                PostInstallData data = new PostInstallData(args, res);
                PackageManagerService.this.mRunningInstalls.put(token, data);
                if (PackageManagerService.DEBUG_INSTALL) {
                    Log.v(PackageManagerService.TAG, "+ starting restore round-trip " + token);
                }
                if (res.returnCode == 1 && doRestore) {
                    IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    if (bm != null) {
                        if (PackageManagerService.DEBUG_INSTALL) {
                            Log.v(PackageManagerService.TAG, "token " + token + " to BM for possible restore");
                        }
                        Trace.asyncTraceBegin(1048576L, "restore", token);
                        try {
                            if (bm.isBackupServiceActive(0)) {
                                bm.restoreAtInstall(res.pkg.applicationInfo.packageName, token);
                            } else {
                                doRestore = false;
                            }
                        } catch (RemoteException e) {
                        } catch (Exception e2) {
                            Slog.e(PackageManagerService.TAG, "Exception trying to enqueue restore", e2);
                            doRestore = false;
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Backup Manager not found!");
                        doRestore = false;
                    }
                }
                if (doRestore) {
                    return;
                }
                if (PackageManagerService.DEBUG_INSTALL) {
                    Log.v(PackageManagerService.TAG, "No restore - queue post-install for " + token);
                }
                Trace.asyncTraceBegin(1048576L, "postInstall", token);
                Message msg = PackageManagerService.this.mHandler.obtainMessage(9, token, 0);
                PackageManagerService.this.mHandler.sendMessage(msg);
            }
        });
    }

    void notifyFirstLaunch(final String pkgName, final String installerPackage, final int userId) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < PackageManagerService.this.mRunningInstalls.size(); i++) {
                    PostInstallData data = PackageManagerService.this.mRunningInstalls.valueAt(i);
                    if (pkgName.equals(data.res.pkg.applicationInfo.packageName)) {
                        for (int uIndex = 0; uIndex < data.res.newUsers.length; uIndex++) {
                            if (userId == data.res.newUsers[uIndex]) {
                                if (PackageManagerService.DEBUG_BACKUP) {
                                    Slog.i(PackageManagerService.TAG, "Package " + pkgName + " being restored so deferring FIRST_LAUNCH");
                                    return;
                                }
                                return;
                            }
                        }
                    }
                }
                if (PackageManagerService.DEBUG_BACKUP) {
                    Slog.i(PackageManagerService.TAG, "Package " + pkgName + " sending normal FIRST_LAUNCH");
                }
                PackageManagerService.this.sendFirstLaunchBroadcast(pkgName, installerPackage, new int[]{userId});
            }
        });
    }

    private void sendFirstLaunchBroadcast(String pkgName, String installerPkg, int[] userIds) {
        sendPackageBroadcast("android.intent.action.PACKAGE_FIRST_LAUNCH", pkgName, null, 0, installerPkg, null, userIds);
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

        HandlerParams(UserHandle user) {
            this.mUser = user;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        HandlerParams setTraceMethod(String traceMethod) {
            this.traceMethod = traceMethod;
            return this;
        }

        HandlerParams setTraceCookie(int traceCookie) {
            this.traceCookie = traceCookie;
            return this;
        }

        final boolean startCopy() {
            boolean res;
            int i;
            try {
                i = this.mRetries + 1;
                this.mRetries = i;
            } catch (RemoteException e) {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.i(PackageManagerService.TAG, "Posting install MCS_RECONNECT");
                }
                PackageManagerService.this.mHandler.sendEmptyMessage(10);
                res = false;
            }
            if (i > 4) {
                Slog.w(PackageManagerService.TAG, "Failed to invoke remote methods on default container service. Giving up");
                PackageManagerService.this.mHandler.sendEmptyMessage(11);
                handleServiceError();
                return false;
            }
            handleStartCopy();
            res = true;
            handleReturnCode();
            return res;
        }

        final void serviceError() {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.i(PackageManagerService.TAG, "serviceError");
            }
            handleServiceError();
            handleReturnCode();
        }
    }

    class MeasureParams extends HandlerParams {
        private final IPackageStatsObserver mObserver;
        private final PackageStats mStats;
        private boolean mSuccess;

        public MeasureParams(PackageStats stats, IPackageStatsObserver observer) {
            super(new UserHandle(stats.userHandle));
            this.mObserver = observer;
            this.mStats = stats;
        }

        public String toString() {
            return "MeasureParams{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mStats.packageName + "}";
        }

        @Override
        void handleStartCopy() throws RemoteException {
            synchronized (PackageManagerService.this.mInstallLock) {
                this.mSuccess = PackageManagerService.this.getPackageSizeInfoLI(this.mStats.packageName, this.mStats.userHandle, this.mStats);
            }
            if (!this.mSuccess) {
                return;
            }
            boolean mounted = false;
            try {
                String status = Environment.getExternalStorageState();
                if ("mounted".equals(status)) {
                    mounted = true;
                } else {
                    mounted = "mounted_ro".equals(status);
                }
            } catch (Exception e) {
            }
            if (!mounted) {
                return;
            }
            Environment.UserEnvironment userEnv = new Environment.UserEnvironment(this.mStats.userHandle);
            this.mStats.externalCacheSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppCacheDirs(this.mStats.packageName));
            this.mStats.externalDataSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppDataDirs(this.mStats.packageName));
            this.mStats.externalDataSize -= this.mStats.externalCacheSize;
            this.mStats.externalMediaSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppMediaDirs(this.mStats.packageName));
            this.mStats.externalObbSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppObbDirs(this.mStats.packageName));
        }

        @Override
        void handleReturnCode() {
            if (this.mObserver == null) {
                return;
            }
            try {
                this.mObserver.onGetStatsCompleted(this.mStats, this.mSuccess);
            } catch (RemoteException e) {
                Slog.i(PackageManagerService.TAG, "Observer no longer exists.");
            }
        }

        @Override
        void handleServiceError() {
            Slog.e(PackageManagerService.TAG, "Could not measure application " + this.mStats.packageName + " external storage");
        }
    }

    private static long calculateDirectorySize(IMediaContainerService mcs, File[] paths) throws RemoteException {
        long result = 0;
        for (File path : paths) {
            result += mcs.calculateDirectorySize(path.getAbsolutePath());
        }
        return result;
    }

    private static void clearDirectory(IMediaContainerService mcs, File[] paths) {
        for (File path : paths) {
            try {
                mcs.clearDirectory(path.getAbsolutePath());
            } catch (RemoteException e) {
            }
        }
    }

    static class OriginInfo {
        final String cid;
        final boolean existing;
        final File file;
        final File resolvedFile;
        final String resolvedPath;
        final boolean staged;

        static OriginInfo fromNothing() {
            return new OriginInfo(null, null, false, false);
        }

        static OriginInfo fromUntrustedFile(File file) {
            return new OriginInfo(file, null, false, false);
        }

        static OriginInfo fromExistingFile(File file) {
            return new OriginInfo(file, null, false, true);
        }

        static OriginInfo fromStagedFile(File file) {
            return new OriginInfo(file, null, true, false);
        }

        static OriginInfo fromStagedContainer(String cid) {
            return new OriginInfo(null, cid, true, false);
        }

        private OriginInfo(File file, String cid, boolean staged, boolean existing) {
            this.file = file;
            this.cid = cid;
            this.staged = staged;
            this.existing = existing;
            if (cid != null) {
                this.resolvedPath = PackageHelper.getSdDir(cid);
                this.resolvedFile = new File(this.resolvedPath);
            } else if (file != null) {
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

        public MoveInfo(int moveId, String fromUuid, String toUuid, String packageName, String dataAppName, int appId, String seinfo, int targetSdkVersion) {
            this.moveId = moveId;
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.packageName = packageName;
            this.dataAppName = dataAppName;
            this.appId = appId;
            this.seinfo = seinfo;
            this.targetSdkVersion = targetSdkVersion;
        }
    }

    static class VerificationInfo {
        public static final int NO_UID = -1;
        final int installerUid;
        final int originatingUid;
        final Uri originatingUri;
        final Uri referrer;

        VerificationInfo(Uri originatingUri, Uri referrer, int originatingUid, int installerUid) {
            this.originatingUri = originatingUri;
            this.referrer = referrer;
            this.originatingUid = originatingUid;
            this.installerUid = installerUid;
        }
    }

    class InstallParams extends HandlerParams {
        final Certificate[][] certificates;
        final String[] grantedRuntimePermissions;
        int installFlags;
        final String installerPackageName;
        private InstallArgs mArgs;
        private int mRet;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final String packageAbiOverride;
        final VerificationInfo verificationInfo;
        String volumeUuid;

        InstallParams(OriginInfo origin, MoveInfo move, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, String volumeUuid, VerificationInfo verificationInfo, UserHandle user, String packageAbiOverride, String[] grantedPermissions, Certificate[][] certificates) {
            super(user);
            this.origin = origin;
            this.move = move;
            this.observer = observer;
            this.installFlags = installFlags;
            this.installerPackageName = installerPackageName;
            this.volumeUuid = volumeUuid;
            this.verificationInfo = verificationInfo;
            this.packageAbiOverride = packageAbiOverride;
            this.grantedRuntimePermissions = grantedPermissions;
            this.certificates = certificates;
        }

        public String toString() {
            return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.origin.file + " cid=" + this.origin.cid + "}";
        }

        private int installLocationPolicy(PackageInfoLite pkgLite) {
            PackageSetting ps;
            String packageName = pkgLite.packageName;
            int installLocation = pkgLite.installLocation;
            boolean onSd = (this.installFlags & 8) != 0;
            synchronized (PackageManagerService.this.mPackages) {
                PackageParser.Package installedPkg = PackageManagerService.this.mPackages.get(packageName);
                PackageParser.Package dataOwnerPkg = installedPkg;
                if (installedPkg == null && (ps = PackageManagerService.this.mSettings.mPackages.get(packageName)) != null) {
                    dataOwnerPkg = ps.pkg;
                }
                if (dataOwnerPkg != null) {
                    boolean downgradeRequested = (this.installFlags & 128) != 0;
                    boolean packageDebuggable = (dataOwnerPkg.applicationInfo.flags & 2) != 0;
                    boolean downgradePermitted = downgradeRequested ? !Build.IS_DEBUGGABLE ? packageDebuggable : true : false;
                    if (!downgradePermitted) {
                        try {
                            PackageManagerService.checkDowngrade(dataOwnerPkg, pkgLite);
                        } catch (PackageManagerException e) {
                            Slog.w(PackageManagerService.TAG, "Downgrade detected: " + e.getMessage());
                            return -7;
                        }
                    }
                }
                if (installedPkg != null) {
                    if ((this.installFlags & 2) != 0) {
                        if ((installedPkg.applicationInfo.flags & 1) != 0) {
                            if (!onSd) {
                                return 1;
                            }
                            Slog.w(PackageManagerService.TAG, "Cannot install update to system app on sdcard");
                            return -3;
                        }
                        if (onSd) {
                            return 2;
                        }
                        if (installLocation == 1) {
                            return 1;
                        }
                        if (installLocation != 2) {
                            return PackageManagerService.isExternal(installedPkg) ? 2 : 1;
                        }
                    } else {
                        return -4;
                    }
                }
                if (onSd) {
                    return 2;
                }
                return pkgLite.recommendedInstallLocation;
            }
        }

        @Override
        public void handleStartCopy() throws RemoteException {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.i(PackageManagerService.TAG, "startCopy " + getUser() + ": " + this);
            }
            int ret = 1;
            if (this.origin.staged) {
                if (this.origin.file != null) {
                    this.installFlags |= 16;
                    this.installFlags &= -9;
                } else {
                    if (this.origin.cid == null) {
                        throw new IllegalStateException("Invalid stage location");
                    }
                    this.installFlags |= 8;
                    this.installFlags &= -17;
                }
            }
            boolean onSd = (this.installFlags & 8) != 0;
            boolean onInt = (this.installFlags & 16) != 0;
            boolean ephemeral = (this.installFlags & 2048) != 0;
            PackageInfoLite pkgLite = null;
            if (onInt && onSd) {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing on both internal and external");
                ret = -19;
            } else if (onSd && ephemeral) {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing ephemeral on external");
                ret = -19;
            } else {
                pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                if (PackageManagerService.DEBUG_EPHEMERAL && ephemeral) {
                    Slog.v(PackageManagerService.TAG, "pkgLite for install: " + pkgLite);
                }
                if (!this.origin.staged && pkgLite.recommendedInstallLocation == -1) {
                    StorageManager storage = StorageManager.from(PackageManagerService.this.mContext);
                    long lowThreshold = storage.getStorageLowBytes(Environment.getDataDirectory());
                    long sizeBytes = PackageManagerService.this.mContainerService.calculateInstalledSize(this.origin.resolvedPath, isForwardLocked(), this.packageAbiOverride);
                    try {
                        PackageManagerService.this.mInstaller.freeCache(null, sizeBytes + lowThreshold);
                        pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.w(PackageManagerService.TAG, "Failed to free cache", e);
                    }
                    if (pkgLite.recommendedInstallLocation == -6) {
                        pkgLite.recommendedInstallLocation = -1;
                    }
                }
            }
            if (ret == 1) {
                int loc = pkgLite.recommendedInstallLocation;
                if (loc == -3) {
                    ret = -19;
                } else if (loc == -4) {
                    ret = -1;
                } else if (loc == -1) {
                    ret = -4;
                } else if (loc == -2) {
                    ret = -2;
                } else if (loc == -6) {
                    ret = -3;
                } else if (loc == -5) {
                    ret = -20;
                } else {
                    int loc2 = installLocationPolicy(pkgLite);
                    if (loc2 == -7) {
                        ret = -25;
                    } else if (!onSd && !onInt) {
                        if (loc2 == 2) {
                            this.installFlags |= 8;
                            this.installFlags &= -17;
                        } else if (loc2 == 3) {
                            if (PackageManagerService.DEBUG_EPHEMERAL) {
                                Slog.v(PackageManagerService.TAG, "...setting INSTALL_EPHEMERAL install flag");
                            }
                            this.installFlags |= 2048;
                            this.installFlags &= -25;
                        } else {
                            this.installFlags |= 16;
                            this.installFlags &= -9;
                        }
                    }
                }
                boolean selectInsLoc = "1".equals(SystemProperties.get("ro.mtk_select_ins_loc"));
                if (selectInsLoc && this.volumeUuid == null) {
                    String volumeuuid = null;
                    try {
                        try {
                            long sizeBytes2 = PackageManagerService.this.mContainerService.calculateInstalledSize(this.origin.resolvedPath, isForwardLocked(), this.packageAbiOverride);
                            volumeuuid = PackageHelper.resolveInstallVolume(PackageManagerService.this.mContext, this.installerPackageName, pkgLite.installLocation, sizeBytes2);
                            this.volumeUuid = volumeuuid;
                        } catch (IOException e2) {
                            Slog.w(PackageManagerService.TAG, "Exception happend: " + e2);
                            this.volumeUuid = null;
                        }
                        Slog.w(PackageManagerService.TAG, "Best volume for " + this.installerPackageName + " : " + volumeuuid);
                    } catch (Throwable th) {
                        this.volumeUuid = null;
                        throw th;
                    }
                }
            }
            InstallArgs args = PackageManagerService.this.createInstallArgs(this);
            this.mArgs = args;
            if (ret == 1) {
                UserHandle verifierUser = getUser();
                if (verifierUser == UserHandle.ALL) {
                    verifierUser = UserHandle.SYSTEM;
                }
                int requiredUid = PackageManagerService.this.mRequiredVerifierPackage == null ? -1 : PackageManagerService.this.getPackageUid(PackageManagerService.this.mRequiredVerifierPackage, 268435456, verifierUser.getIdentifier());
                if (this.origin.existing || requiredUid == -1 || !PackageManagerService.this.isVerificationEnabled(verifierUser.getIdentifier(), this.installFlags)) {
                    ret = args.copyApk(PackageManagerService.this.mContainerService, true);
                } else {
                    Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                    verification.addFlags(268435456);
                    verification.setDataAndType(Uri.fromFile(new File(this.origin.resolvedPath)), PackageManagerService.PACKAGE_MIME_TYPE);
                    verification.addFlags(1);
                    List<ResolveInfo> receivers = PackageManagerService.this.queryIntentReceiversInternal(verification, PackageManagerService.PACKAGE_MIME_TYPE, 0, verifierUser.getIdentifier());
                    if (PackageManagerService.DEBUG_VERIFY) {
                        Slog.d(PackageManagerService.TAG, "Found " + receivers.size() + " verifiers for intent " + verification.toString() + " with " + pkgLite.verifiers.length + " optional verifiers");
                    }
                    PackageManagerService packageManagerService = PackageManagerService.this;
                    final int verificationId = packageManagerService.mPendingVerificationToken;
                    packageManagerService.mPendingVerificationToken = verificationId + 1;
                    verification.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", this.installerPackageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALL_FLAGS", this.installFlags);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_PACKAGE_NAME", pkgLite.packageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_VERSION_CODE", pkgLite.versionCode);
                    if (this.verificationInfo != null) {
                        if (this.verificationInfo.originatingUri != null) {
                            verification.putExtra("android.intent.extra.ORIGINATING_URI", this.verificationInfo.originatingUri);
                        }
                        if (this.verificationInfo.referrer != null) {
                            verification.putExtra("android.intent.extra.REFERRER", this.verificationInfo.referrer);
                        }
                        if (this.verificationInfo.originatingUid >= 0) {
                            verification.putExtra("android.intent.extra.ORIGINATING_UID", this.verificationInfo.originatingUid);
                        }
                        if (this.verificationInfo.installerUid >= 0) {
                            verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", this.verificationInfo.installerUid);
                        }
                    }
                    PackageVerificationState verificationState = new PackageVerificationState(requiredUid, args);
                    PackageManagerService.this.mPendingVerification.append(verificationId, verificationState);
                    List<ComponentName> sufficientVerifiers = PackageManagerService.this.matchVerifiers(pkgLite, receivers, verificationState);
                    if (sufficientVerifiers != null) {
                        int N = sufficientVerifiers.size();
                        if (N == 0) {
                            Slog.i(PackageManagerService.TAG, "Additional verifiers required, but none installed.");
                            ret = -22;
                        } else {
                            for (int i = 0; i < N; i++) {
                                ComponentName verifierComponent = sufficientVerifiers.get(i);
                                Intent sufficientIntent = new Intent(verification);
                                sufficientIntent.setComponent(verifierComponent);
                                PackageManagerService.this.mContext.sendBroadcastAsUser(sufficientIntent, verifierUser);
                            }
                        }
                    }
                    ComponentName requiredVerifierComponent = PackageManagerService.this.matchComponentForVerifier(PackageManagerService.this.mRequiredVerifierPackage, receivers);
                    if (ret == 1 && PackageManagerService.this.mRequiredVerifierPackage != null) {
                        Trace.asyncTraceBegin(1048576L, "verification", verificationId);
                        verification.setComponent(requiredVerifierComponent);
                        PackageManagerService.this.mContext.sendOrderedBroadcastAsUser(verification, verifierUser, "android.permission.PACKAGE_VERIFICATION_AGENT", new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                Message msg = PackageManagerService.this.mHandler.obtainMessage(16);
                                msg.arg1 = verificationId;
                                PackageManagerService.this.mHandler.sendMessageDelayed(msg, PackageManagerService.this.getVerificationTimeout());
                            }
                        }, null, 0, null, null);
                        this.mArgs = null;
                    }
                }
            }
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.i(PackageManagerService.TAG, "Apk copy done");
            }
            this.mRet = ret;
        }

        @Override
        void handleReturnCode() {
            if (this.mArgs == null) {
                return;
            }
            PackageManagerService.this.processPendingInstall(this.mArgs, this.mRet);
        }

        @Override
        void handleServiceError() {
            this.mArgs = PackageManagerService.this.createInstallArgs(this);
            this.mRet = -110;
        }

        public boolean isForwardLocked() {
            return (this.installFlags & 1) != 0;
        }
    }

    private static boolean installOnExternalAsec(int installFlags) {
        return (installFlags & 16) == 0 && (installFlags & 8) != 0;
    }

    private static boolean installForwardLocked(int installFlags) {
        return (installFlags & 1) != 0;
    }

    private InstallArgs createInstallArgs(InstallParams params) {
        if (params.move != null) {
            return new MoveInstallArgs(params);
        }
        if (installOnExternalAsec(params.installFlags) || params.isForwardLocked()) {
            return new AsecInstallArgs(params);
        }
        return new FileInstallArgs(params);
    }

    private InstallArgs createInstallArgsForExisting(int installFlags, String codePath, String resourcePath, String[] instructionSets) {
        boolean isInAsec;
        if (installOnExternalAsec(installFlags)) {
            isInAsec = true;
        } else if (installForwardLocked(installFlags) && !codePath.startsWith(this.mDrmAppPrivateInstallDir.getAbsolutePath())) {
            isInAsec = true;
        } else {
            isInAsec = false;
        }
        if (isInAsec) {
            return new AsecInstallArgs(codePath, instructionSets, installOnExternalAsec(installFlags), installForwardLocked(installFlags));
        }
        return new FileInstallArgs(codePath, resourcePath, instructionSets);
    }

    static abstract class InstallArgs {
        final String abiOverride;
        final Certificate[][] certificates;
        final int installFlags;
        final String[] installGrantPermissions;
        final String installerPackageName;
        String[] instructionSets;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
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

        InstallArgs(OriginInfo origin, MoveInfo move, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, String volumeUuid, UserHandle user, String[] instructionSets, String abiOverride, String[] installGrantPermissions, String traceMethod, int traceCookie, Certificate[][] certificates) {
            this.origin = origin;
            this.move = move;
            this.installFlags = installFlags;
            this.observer = observer;
            this.installerPackageName = installerPackageName;
            this.volumeUuid = volumeUuid;
            this.user = user;
            this.instructionSets = instructionSets;
            this.abiOverride = abiOverride;
            this.installGrantPermissions = installGrantPermissions;
            this.traceMethod = traceMethod;
            this.traceCookie = traceCookie;
            this.certificates = certificates;
        }

        int doPreCopy() {
            return 1;
        }

        int doPostCopy(int uid) {
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

    private void removeDexFiles(List<String> allCodePaths, String[] instructionSets) {
        if (allCodePaths.isEmpty()) {
            return;
        }
        if (instructionSets == null) {
            throw new IllegalStateException("instructionSet == null");
        }
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
        for (String codePath : allCodePaths) {
            for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                try {
                    this.mInstaller.rmdex(codePath, dexCodeInstructionSet);
                } catch (InstallerConnection.InstallerException e) {
                }
            }
        }
    }

    class FileInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        FileInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates);
            if (!isFwdLocked()) {
            } else {
                throw new IllegalArgumentException("Forward locking only supported in ASEC");
            }
        }

        FileInstallArgs(String codePath, String resourcePath, String[] instructionSets) {
            super(OriginInfo.fromNothing(), null, null, 0, null, null, null, instructionSets, null, null, null, 0, null);
            this.codeFile = codePath != null ? new File(codePath) : null;
            this.resourceFile = resourcePath != null ? new File(resourcePath) : null;
        }

        @Override
        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            Trace.traceBegin(1048576L, "copyApk");
            try {
                return doCopyApk(imcs, temp);
            } finally {
                Trace.traceEnd(1048576L);
            }
        }

        private int doCopyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            if (this.origin.staged) {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.d(PackageManagerService.TAG, this.origin.file + " already staged; skipping copy");
                }
                this.codeFile = this.origin.file;
                this.resourceFile = this.origin.file;
                return 1;
            }
            try {
                boolean isEphemeral = (this.installFlags & 2048) != 0;
                File tempDir = PackageManagerService.this.mInstallerService.allocateStageDirLegacy(this.volumeUuid, isEphemeral);
                this.codeFile = tempDir;
                this.resourceFile = tempDir;
                int ret = imcs.copyPackage(this.origin.file.getAbsolutePath(), new IParcelFileDescriptorFactory.Stub() {
                    public ParcelFileDescriptor open(String name, int mode) throws RemoteException {
                        if (!FileUtils.isValidExtFilename(name)) {
                            throw new IllegalArgumentException("Invalid filename: " + name);
                        }
                        try {
                            File file = new File(FileInstallArgs.this.codeFile, name);
                            FileDescriptor fd = Os.open(file.getAbsolutePath(), OsConstants.O_RDWR | OsConstants.O_CREAT, 420);
                            Os.chmod(file.getAbsolutePath(), 420);
                            return new ParcelFileDescriptor(fd);
                        } catch (ErrnoException e) {
                            throw new RemoteException("Failed to open: " + e.getMessage());
                        }
                    }
                });
                if (ret != 1) {
                    Slog.e(PackageManagerService.TAG, "Failed to copy package");
                    return ret;
                }
                File libraryRoot = new File(this.codeFile, "lib");
                NativeLibraryHelper.Handle handle = null;
                try {
                    handle = NativeLibraryHelper.Handle.create(this.codeFile);
                    return NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libraryRoot, this.abiOverride);
                } catch (IOException e) {
                    Slog.e(PackageManagerService.TAG, "Copying native libraries failed", e);
                    return -110;
                } finally {
                    IoUtils.closeQuietly(handle);
                }
            } catch (IOException e2) {
                Slog.w(PackageManagerService.TAG, "Failed to create copy file: " + e2);
                return -4;
            }
        }

        @Override
        int doPreInstall(int status) {
            if (status != 1) {
                cleanUp();
            }
            return status;
        }

        @Override
        boolean doRename(int status, PackageParser.Package pkg, String oldCodePath) {
            if (status != 1) {
                cleanUp();
                return false;
            }
            File targetDir = this.codeFile.getParentFile();
            File beforeCodeFile = this.codeFile;
            File afterCodeFile = PackageManagerService.this.getNextCodePath(targetDir, pkg.packageName);
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d(PackageManagerService.TAG, "Renaming " + beforeCodeFile + " to " + afterCodeFile);
            }
            try {
                Os.rename(beforeCodeFile.getAbsolutePath(), afterCodeFile.getAbsolutePath());
                if (!SELinux.restoreconRecursive(afterCodeFile)) {
                    Slog.w(PackageManagerService.TAG, "Failed to restorecon");
                    return false;
                }
                this.codeFile = afterCodeFile;
                this.resourceFile = afterCodeFile;
                pkg.setCodePath(afterCodeFile.getAbsolutePath());
                pkg.setBaseCodePath(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath));
                pkg.setSplitCodePaths(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths));
                pkg.setApplicationVolumeUuid(pkg.volumeUuid);
                pkg.setApplicationInfoCodePath(pkg.codePath);
                pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
                pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
                pkg.setApplicationInfoResourcePath(pkg.codePath);
                pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
                pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
                return true;
            } catch (ErrnoException e) {
                Slog.w(PackageManagerService.TAG, "Failed to rename", e);
                return false;
            }
        }

        @Override
        int doPostInstall(int status, int uid) {
            if (status != 1) {
                cleanUp();
            }
            return status;
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
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(this.codeFile, 0);
                    allCodePaths = pkg.getAllCodePaths();
                } catch (PackageParser.PackageParserException e) {
                }
            }
            cleanUp();
            PackageManagerService.this.removeDexFiles(allCodePaths, this.instructionSets);
        }

        @Override
        boolean doPostDeleteLI(boolean delete) {
            cleanUpResourcesLI();
            return true;
        }
    }

    private boolean isAsecExternal(String cid) {
        String asecPath = PackageHelper.getSdFilesystem(cid);
        return (asecPath == null || asecPath.startsWith(this.mAsecInternalPath)) ? false : true;
    }

    private static void maybeThrowExceptionForMultiArchCopy(String message, int copyRet) throws PackageManagerException {
        if (copyRet >= 0 || copyRet == -114 || copyRet == -113) {
        } else {
            throw new PackageManagerException(copyRet, message);
        }
    }

    static String cidFromCodePath(String fullCodePath) {
        int eidx = fullCodePath.lastIndexOf("/");
        String subStr1 = fullCodePath.substring(0, eidx);
        int sidx = subStr1.lastIndexOf("/");
        return subStr1.substring(sidx + 1, eidx);
    }

    class AsecInstallArgs extends InstallArgs {
        static final String PUBLIC_RES_FILE_NAME = "res.zip";
        static final String RES_FILE_NAME = "pkg.apk";
        String cid;
        String packagePath;
        String resourcePath;

        AsecInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates);
        }

        AsecInstallArgs(String fullCodePath, String[] instructionSets, boolean isExternal, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, null, (isExternal ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null, null, null, 0, null);
            fullCodePath = fullCodePath.endsWith(RES_FILE_NAME) ? fullCodePath : new File(fullCodePath, RES_FILE_NAME).getAbsolutePath();
            int eidx = fullCodePath.lastIndexOf("/");
            String subStr1 = fullCodePath.substring(0, eidx);
            int sidx = subStr1.lastIndexOf("/");
            this.cid = subStr1.substring(sidx + 1, eidx);
            setMountPath(subStr1);
        }

        AsecInstallArgs(String cid, String[] instructionSets, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, null, (PackageManagerService.this.isAsecExternal(cid) ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null, null, null, 0, null);
            this.cid = cid;
            setMountPath(PackageHelper.getSdDir(cid));
        }

        void createCopyFile() {
            this.cid = PackageManagerService.this.mInstallerService.allocateExternalStageCidLegacy();
        }

        @Override
        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            if (this.origin.staged && this.origin.cid != null) {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.d(PackageManagerService.TAG, this.origin.cid + " already staged; skipping copy");
                }
                this.cid = this.origin.cid;
                setMountPath(PackageHelper.getSdDir(this.cid));
                return 1;
            }
            if (temp) {
                createCopyFile();
            } else {
                PackageHelper.destroySdDir(this.cid);
            }
            String newMountPath = imcs.copyPackageToContainer(this.origin.file.getAbsolutePath(), this.cid, PackageManagerService.getEncryptKey(), isExternalAsec(), isFwdLocked(), PackageManagerService.deriveAbiOverride(this.abiOverride, null));
            if (newMountPath != null) {
                setMountPath(newMountPath);
                return 1;
            }
            return -18;
        }

        @Override
        String getCodePath() {
            return this.packagePath;
        }

        @Override
        String getResourcePath() {
            return this.resourcePath;
        }

        @Override
        int doPreInstall(int status) {
            if (status != 1) {
                PackageHelper.destroySdDir(this.cid);
            } else {
                boolean mounted = PackageHelper.isContainerMounted(this.cid);
                if (!mounted) {
                    String newMountPath = PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), 1000);
                    if (newMountPath != null) {
                        setMountPath(newMountPath);
                    } else {
                        return -18;
                    }
                }
            }
            return status;
        }

        @Override
        boolean doRename(int status, PackageParser.Package pkg, String oldCodePath) {
            String newMountPath;
            String newCacheId = PackageManagerService.getNextCodePath(oldCodePath, pkg.packageName, "/pkg.apk");
            if (PackageHelper.isContainerMounted(this.cid) && !PackageHelper.unMountSdDir(this.cid)) {
                Slog.i(PackageManagerService.TAG, "Failed to unmount " + this.cid + " before renaming");
                return false;
            }
            if (!PackageHelper.renameSdDir(this.cid, newCacheId)) {
                Slog.e(PackageManagerService.TAG, "Failed to rename " + this.cid + " to " + newCacheId + " which might be stale. Will try to clean up.");
                if (!PackageHelper.destroySdDir(newCacheId)) {
                    Slog.e(PackageManagerService.TAG, "Very strange. Cannot clean up stale container " + newCacheId);
                    return false;
                }
                if (!PackageHelper.renameSdDir(this.cid, newCacheId)) {
                    Slog.e(PackageManagerService.TAG, "Failed to rename " + this.cid + " to " + newCacheId + " inspite of cleaning it up.");
                    return false;
                }
            }
            if (!PackageHelper.isContainerMounted(newCacheId)) {
                Slog.w(PackageManagerService.TAG, "Mounting container " + newCacheId);
                newMountPath = PackageHelper.mountSdDir(newCacheId, PackageManagerService.getEncryptKey(), 1000);
            } else {
                newMountPath = PackageHelper.getSdDir(newCacheId);
            }
            if (newMountPath == null) {
                Slog.w(PackageManagerService.TAG, "Failed to get cache path for  " + newCacheId);
                return false;
            }
            Log.i(PackageManagerService.TAG, "Succesfully renamed " + this.cid + " to " + newCacheId + " at new path: " + newMountPath);
            this.cid = newCacheId;
            File beforeCodeFile = new File(this.packagePath);
            setMountPath(newMountPath);
            File afterCodeFile = new File(this.packagePath);
            pkg.setCodePath(afterCodeFile.getAbsolutePath());
            pkg.setBaseCodePath(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath));
            pkg.setSplitCodePaths(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths));
            pkg.setApplicationVolumeUuid(pkg.volumeUuid);
            pkg.setApplicationInfoCodePath(pkg.codePath);
            pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
            pkg.setApplicationInfoResourcePath(pkg.codePath);
            pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
            return true;
        }

        private void setMountPath(String mountPath) {
            File mountFile = new File(mountPath);
            File monolithicFile = new File(mountFile, RES_FILE_NAME);
            if (monolithicFile.exists()) {
                this.packagePath = monolithicFile.getAbsolutePath();
                if (isFwdLocked()) {
                    this.resourcePath = new File(mountFile, PUBLIC_RES_FILE_NAME).getAbsolutePath();
                    return;
                } else {
                    this.resourcePath = this.packagePath;
                    return;
                }
            }
            this.packagePath = mountFile.getAbsolutePath();
            this.resourcePath = this.packagePath;
        }

        @Override
        int doPostInstall(int status, int uid) {
            int groupOwner;
            String str;
            if (status != 1) {
                cleanUp();
            } else {
                if (isFwdLocked()) {
                    groupOwner = UserHandle.getSharedAppGid(uid);
                    str = RES_FILE_NAME;
                } else {
                    groupOwner = -1;
                    str = null;
                }
                if (uid < 10000 || !PackageHelper.fixSdPermissions(this.cid, groupOwner, str)) {
                    Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
                    PackageHelper.destroySdDir(this.cid);
                    return -18;
                }
                boolean mounted = PackageHelper.isContainerMounted(this.cid);
                if (!mounted) {
                    PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), Process.myUid());
                }
            }
            return status;
        }

        private void cleanUp() {
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Slog.i(PackageManagerService.TAG, "cleanUp");
            }
            PackageHelper.destroySdDir(this.cid);
        }

        private List<String> getAllCodePaths() {
            File codeFile = new File(getCodePath());
            if (codeFile != null && codeFile.exists()) {
                try {
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(codeFile, 0);
                    return pkg.getAllCodePaths();
                } catch (PackageParser.PackageParserException e) {
                }
            }
            return Collections.EMPTY_LIST;
        }

        @Override
        void cleanUpResourcesLI() {
            cleanUpResourcesLI(getAllCodePaths());
        }

        private void cleanUpResourcesLI(List<String> allCodePaths) {
            cleanUp();
            PackageManagerService.this.removeDexFiles(allCodePaths, this.instructionSets);
        }

        String getPackageName() {
            return PackageManagerService.getAsecPackageName(this.cid);
        }

        @Override
        boolean doPostDeleteLI(boolean delete) {
            if (PackageManagerService.DEBUG_SD_INSTALL) {
                Slog.i(PackageManagerService.TAG, "doPostDeleteLI() del=" + delete);
            }
            List<String> allCodePaths = getAllCodePaths();
            boolean mounted = PackageHelper.isContainerMounted(this.cid);
            if (mounted && PackageHelper.unMountSdDir(this.cid)) {
                mounted = false;
            }
            if (!mounted && delete) {
                cleanUpResourcesLI(allCodePaths);
            }
            return !mounted;
        }

        @Override
        int doPreCopy() {
            if (isFwdLocked() && !PackageHelper.fixSdPermissions(this.cid, PackageManagerService.this.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, DumpState.DUMP_DEXOPT, 0), RES_FILE_NAME)) {
                return -18;
            }
            return 1;
        }

        @Override
        int doPostCopy(int uid) {
            if (isFwdLocked()) {
                if (uid < 10000 || !PackageHelper.fixSdPermissions(this.cid, UserHandle.getSharedAppGid(uid), RES_FILE_NAME)) {
                    Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
                    PackageHelper.destroySdDir(this.cid);
                    return -18;
                }
                return 1;
            }
            return 1;
        }
    }

    class MoveInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        MoveInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates);
        }

        @Override
        int copyApk(IMediaContainerService imcs, boolean temp) {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d(PackageManagerService.TAG, "Moving " + this.move.packageName + " from " + this.move.fromUuid + " to " + this.move.toUuid);
            }
            synchronized (PackageManagerService.this.mInstaller) {
                try {
                    PackageManagerService.this.mInstaller.moveCompleteApp(this.move.fromUuid, this.move.toUuid, this.move.packageName, this.move.dataAppName, this.move.appId, this.move.seinfo, this.move.targetSdkVersion);
                } catch (InstallerConnection.InstallerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move app", e);
                    return -110;
                }
            }
            this.codeFile = new File(Environment.getDataAppDirectory(this.move.toUuid), this.move.dataAppName);
            this.resourceFile = this.codeFile;
            if (!PackageManagerService.DEBUG_INSTALL) {
                return 1;
            }
            Slog.d(PackageManagerService.TAG, "codeFile after move is " + this.codeFile);
            return 1;
        }

        @Override
        int doPreInstall(int status) {
            if (status != 1) {
                cleanUp(this.move.toUuid);
            }
            return status;
        }

        @Override
        boolean doRename(int status, PackageParser.Package pkg, String oldCodePath) {
            if (status != 1) {
                cleanUp(this.move.toUuid);
                return false;
            }
            pkg.setApplicationVolumeUuid(pkg.volumeUuid);
            pkg.setApplicationInfoCodePath(pkg.codePath);
            pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
            pkg.setApplicationInfoResourcePath(pkg.codePath);
            pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
            return true;
        }

        @Override
        int doPostInstall(int status, int uid) {
            if (status == 1) {
                cleanUp(this.move.fromUuid);
            } else {
                cleanUp(this.move.toUuid);
            }
            return status;
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

        private boolean cleanUp(String volumeUuid) {
            File codeFile = new File(Environment.getDataAppDirectory(volumeUuid), this.move.dataAppName);
            Slog.d(PackageManagerService.TAG, "Cleaning up " + this.move.packageName + " on " + volumeUuid);
            int[] userIds = PackageManagerService.sUserManager.getUserIds();
            synchronized (PackageManagerService.this.mInstallLock) {
                for (int userId : userIds) {
                    try {
                        PackageManagerService.this.mInstaller.destroyAppData(volumeUuid, this.move.packageName, userId, 3, 0L);
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.w(PackageManagerService.TAG, String.valueOf(e));
                    }
                }
                PackageManagerService.this.removeCodePathLI(codeFile);
            }
            return true;
        }

        @Override
        void cleanUpResourcesLI() {
            throw new UnsupportedOperationException();
        }

        @Override
        boolean doPostDeleteLI(boolean delete) {
            throw new UnsupportedOperationException();
        }
    }

    static String getAsecPackageName(String packageCid) {
        int idx = packageCid.lastIndexOf(INSTALL_PACKAGE_SUFFIX);
        if (idx == -1) {
            return packageCid;
        }
        return packageCid.substring(0, idx);
    }

    private static String getNextCodePath(String oldCodePath, String prefix, String suffix) {
        String subStr;
        int idx = 1;
        if (oldCodePath != null) {
            String subStr2 = oldCodePath;
            if (suffix != null && oldCodePath.endsWith(suffix)) {
                subStr2 = oldCodePath.substring(0, oldCodePath.length() - suffix.length());
            }
            int sidx = subStr2.lastIndexOf(prefix);
            if (sidx != -1 && (subStr = subStr2.substring(prefix.length() + sidx)) != null) {
                if (subStr.startsWith(INSTALL_PACKAGE_SUFFIX)) {
                    subStr = subStr.substring(INSTALL_PACKAGE_SUFFIX.length());
                }
                try {
                    int idx2 = Integer.parseInt(subStr);
                    idx = idx2 <= 1 ? idx2 + 1 : idx2 - 1;
                } catch (NumberFormatException e) {
                }
            }
        }
        String idxStr = INSTALL_PACKAGE_SUFFIX + Integer.toString(idx);
        return prefix + idxStr;
    }

    private File getNextCodePath(File targetDir, String packageName) {
        File result;
        int suffix = 1;
        do {
            result = new File(targetDir, packageName + INSTALL_PACKAGE_SUFFIX + suffix);
            suffix++;
        } while (result.exists());
        return result;
    }

    static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        File codeFile = new File(codePath);
        String name = codeFile.getName();
        if (codeFile.isDirectory()) {
            return name;
        }
        if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            int lastDot = name.lastIndexOf(46);
            return name.substring(0, lastDot);
        }
        Slog.w(TAG, "Odd, " + codePath + " doesn't look like an APK");
        return null;
    }

    static class PackageInstalledInfo {
        ArrayMap<String, PackageInstalledInfo> addedChildPackages;
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

        public void setError(int code, String msg) {
            setReturnCode(code);
            setReturnMessage(msg);
            Slog.w(PackageManagerService.TAG, msg);
        }

        public void setError(String msg, PackageParser.PackageParserException e) {
            setReturnCode(e.error);
            setReturnMessage(ExceptionUtils.getCompleteMessage(msg, e));
            Slog.w(PackageManagerService.TAG, msg, e);
        }

        public void setError(String msg, PackageManagerException e) {
            this.returnCode = e.error;
            setReturnMessage(ExceptionUtils.getCompleteMessage(msg, e));
            Slog.w(PackageManagerService.TAG, msg, e);
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                this.addedChildPackages.valueAt(i).returnCode = returnCode;
            }
        }

        private void setReturnMessage(String returnMsg) {
            this.returnMsg = returnMsg;
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                this.addedChildPackages.valueAt(i).returnMsg = returnMsg;
            }
        }
    }

    private void installNewPackageLIF(PackageParser.Package pkg, int policyFlags, int scanFlags, UserHandle user, String installerPackageName, String volumeUuid, PackageInstalledInfo res) {
        Trace.traceBegin(1048576L, "installNewPackage");
        String pkgName = pkg.packageName;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "installNewPackageLI: " + pkg);
        }
        synchronized (this.mPackages) {
            if (this.mSettings.mRenamedPackages.containsKey(pkgName)) {
                res.setError(-1, "Attempt to re-install " + pkgName + " without first uninstalling package running as " + this.mSettings.mRenamedPackages.get(pkgName));
                return;
            }
            if (this.mPackages.containsKey(pkgName)) {
                res.setError(-1, "Attempt to re-install " + pkgName + " without first uninstalling.");
                return;
            }
            try {
                PackageParser.Package newPackage = scanPackageTracedLI(pkg, policyFlags, scanFlags, System.currentTimeMillis(), user);
                updateSettingsLI(newPackage, installerPackageName, null, res, user);
                if (res.returnCode == 1) {
                    prepareAppDataAfterInstallLIF(newPackage);
                } else {
                    deletePackageLIF(pkgName, UserHandle.ALL, false, null, 1, res.removedInfo, true, null);
                }
            } catch (PackageManagerException e) {
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
            }
            Trace.traceEnd(1048576L);
        }
    }

    private boolean shouldCheckUpgradeKeySetLP(PackageSetting oldPs, int scanFlags) {
        if (oldPs == null || (scanFlags & 16384) != 0 || oldPs.sharedUser != null || !oldPs.keySetData.isUsingUpgradeKeySets()) {
            return false;
        }
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        long[] upgradeKeySets = oldPs.keySetData.getUpgradeKeySets();
        for (int i = 0; i < upgradeKeySets.length; i++) {
            if (!ksms.isIdValidKeySetId(upgradeKeySets[i])) {
                Slog.wtf(TAG, "Package " + (oldPs.name != null ? oldPs.name : "<null>") + " contains upgrade-key-set reference to unknown key-set: " + upgradeKeySets[i] + " reverting to signatures check.");
                return false;
            }
        }
        return true;
    }

    private boolean checkUpgradeKeySetLP(PackageSetting oldPS, PackageParser.Package newPkg) {
        long[] upgradeKeySets = oldPS.keySetData.getUpgradeKeySets();
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        for (long j : upgradeKeySets) {
            Set<PublicKey> upgradeSet = ksms.getPublicKeysFromKeySetLPr(j);
            if (upgradeSet != null && newPkg.mSigningKeys.containsAll(upgradeSet)) {
                return true;
            }
        }
        return false;
    }

    private static void updateDigest(MessageDigest digest, File file) throws Throwable {
        Throwable th = null;
        DigestInputStream digestStream = null;
        try {
            DigestInputStream digestStream2 = new DigestInputStream(new FileInputStream(file), digest);
            do {
                try {
                } catch (Throwable th2) {
                    th = th2;
                    digestStream = digestStream2;
                    if (digestStream != null) {
                    }
                    if (th != null) {
                    }
                }
            } while (digestStream2.read() != -1);
            if (digestStream2 != null) {
                try {
                    digestStream2.close();
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (th != null) {
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    private void replacePackageLIF(PackageParser.Package pkg, int policyFlags, int scanFlags, UserHandle user, String installerPackageName, PackageInstalledInfo res) throws Throwable {
        PackageInstalledInfo childRes;
        boolean isEphemeral = (policyFlags & 2048) != 0;
        String pkgName = pkg.packageName;
        synchronized (this.mPackages) {
            PackageParser.Package oldPackage = this.mPackages.get(pkgName);
            if (DEBUG_INSTALL) {
                Slog.d(TAG, "replacePackageLI: new=" + pkg + ", old=" + oldPackage);
            }
            boolean oldTargetsPreRelease = oldPackage.applicationInfo.targetSdkVersion == 10000;
            boolean newTargetsPreRelease = pkg.applicationInfo.targetSdkVersion == 10000;
            if (oldTargetsPreRelease && !newTargetsPreRelease && (policyFlags & 4096) == 0) {
                Slog.w(TAG, "Can't install package targeting released sdk");
                res.setReturnCode(-7);
                return;
            }
            boolean oldIsEphemeral = oldPackage.applicationInfo.isEphemeralApp();
            if (isEphemeral && !oldIsEphemeral) {
                Slog.w(TAG, "Can't replace app with ephemeral: " + pkgName);
                res.setReturnCode(-116);
                return;
            }
            PackageSetting ps = this.mSettings.mPackages.get(pkgName);
            if (shouldCheckUpgradeKeySetLP(ps, scanFlags)) {
                if (!checkUpgradeKeySetLP(ps, pkg)) {
                    res.setError(-7, "New package not signed by keys specified by upgrade-keysets: " + pkgName);
                    return;
                }
            } else if (compareSignatures(oldPackage.mSignatures, pkg.mSignatures) != 0) {
                res.setError(-7, "New package has a different signature: " + pkgName);
                return;
            }
            if (oldPackage.restrictUpdateHash != null && oldPackage.isSystemApp()) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-512");
                    updateDigest(digest, new File(pkg.baseCodePath));
                    if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
                        for (String path : pkg.splitCodePaths) {
                            updateDigest(digest, new File(path));
                        }
                    }
                    byte[] digestBytes = digest.digest();
                    if (!Arrays.equals(oldPackage.restrictUpdateHash, digestBytes)) {
                        res.setError(-2, "New package fails restrict-update check: " + pkgName);
                        return;
                    }
                    pkg.restrictUpdateHash = oldPackage.restrictUpdateHash;
                } catch (IOException | NoSuchAlgorithmException e) {
                    res.setError(-2, "Could not compute hash: " + pkgName);
                    return;
                }
            }
            String invalidPackageName = getParentOrChildPackageChangedSharedUser(oldPackage, pkg);
            if (invalidPackageName != null) {
                res.setError(-8, "Package " + invalidPackageName + " tried to change user " + oldPackage.mSharedUserId);
                return;
            }
            int[] allUsers = sUserManager.getUserIds();
            int[] installedUsers = ps.queryInstalledUsers(allUsers, true);
            res.removedInfo = new PackageRemovedInfo();
            res.removedInfo.uid = oldPackage.applicationInfo.uid;
            res.removedInfo.removedPackage = oldPackage.packageName;
            res.removedInfo.isUpdate = true;
            res.removedInfo.origUsers = installedUsers;
            int childCount = oldPackage.childPackages != null ? oldPackage.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                boolean childPackageUpdated = false;
                PackageParser.Package childPkg = (PackageParser.Package) oldPackage.childPackages.get(i);
                if (res.addedChildPackages != null && (childRes = res.addedChildPackages.get(childPkg.packageName)) != null) {
                    childRes.removedInfo.uid = childPkg.applicationInfo.uid;
                    childRes.removedInfo.removedPackage = childPkg.packageName;
                    childRes.removedInfo.isUpdate = true;
                    childPackageUpdated = true;
                }
                if (!childPackageUpdated) {
                    PackageRemovedInfo childRemovedRes = new PackageRemovedInfo();
                    childRemovedRes.removedPackage = childPkg.packageName;
                    childRemovedRes.isUpdate = false;
                    childRemovedRes.dataRemoved = true;
                    synchronized (this.mPackages) {
                        PackageSetting childPs = this.mSettings.peekPackageLPr(childPkg.packageName);
                        if (childPs != null) {
                            childRemovedRes.origUsers = childPs.queryInstalledUsers(allUsers, true);
                        }
                    }
                    if (res.removedInfo.removedChildPackages == null) {
                        res.removedInfo.removedChildPackages = new ArrayMap<>();
                    }
                    res.removedInfo.removedChildPackages.put(childPkg.packageName, childRemovedRes);
                }
            }
            boolean sysPkg = isSystemApp(oldPackage);
            boolean operatorPkg = isVendorApp(oldPackage);
            if (sysPkg) {
                boolean privileged = (oldPackage.applicationInfo.privateFlags & 8) != 0;
                int systemPolicyFlags = policyFlags | 1 | (privileged ? 128 : 0);
                replaceSystemPackageLIF(oldPackage, pkg, systemPolicyFlags, scanFlags, user, allUsers, installerPackageName, res);
            } else if (operatorPkg) {
                replaceSystemPackageLIF(oldPackage, pkg, policyFlags, scanFlags, user, allUsers, installerPackageName, res);
            } else {
                replaceNonSystemPackageLIF(oldPackage, pkg, policyFlags, scanFlags, user, allUsers, installerPackageName, res);
            }
        }
    }

    public List<String> getPreviousCodePaths(String packageName) {
        PackageSetting ps = this.mSettings.mPackages.get(packageName);
        List<String> result = new ArrayList<>();
        if (ps != null && ps.oldCodePaths != null) {
            result.addAll(ps.oldCodePaths);
        }
        return result;
    }

    private void replaceNonSystemPackageLIF(PackageParser.Package deletedPackage, PackageParser.Package pkg, int policyFlags, int scanFlags, UserHandle user, int[] allUsers, String installerPackageName, PackageInstalledInfo res) {
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "replaceNonSystemPackageLI: new=" + pkg + ", old=" + deletedPackage);
        }
        String pkgName = deletedPackage.packageName;
        boolean deletedPkg = true;
        boolean addedPkg = false;
        boolean killApp = (131072 & scanFlags) == 0;
        int deleteFlags = (killApp ? 0 : 8) | 1;
        long origUpdateTime = pkg.mExtras != null ? ((PackageSetting) pkg.mExtras).lastUpdateTime : 0L;
        if (!deletePackageLIF(pkgName, null, true, allUsers, deleteFlags, res.removedInfo, true, pkg)) {
            res.setError(-10, "replaceNonSystemPackageLI");
            deletedPkg = false;
        } else {
            if (deletedPackage.isForwardLocked() || isExternal(deletedPackage)) {
                if (DEBUG_INSTALL) {
                    Slog.i(TAG, "upgrading pkg " + deletedPackage + " is ASEC-hosted -> UNAVAILABLE");
                }
                int[] uidArray = {deletedPackage.applicationInfo.uid};
                ArrayList<String> pkgList = new ArrayList<>(1);
                pkgList.add(deletedPackage.applicationInfo.packageName);
                sendResourcesChangedBroadcast(false, true, pkgList, uidArray, (IIntentReceiver) null);
            }
            clearAppDataLIF(pkg, -1, 515);
            clearAppProfilesLIF(deletedPackage, -1);
            try {
                PackageParser.Package newPackage = scanPackageTracedLI(pkg, policyFlags, scanFlags | 64, System.currentTimeMillis(), user);
                updateSettingsLI(newPackage, installerPackageName, allUsers, res, user);
                PackageSetting ps = this.mSettings.mPackages.get(pkgName);
                if (!killApp) {
                    if (ps.oldCodePaths == null) {
                        ps.oldCodePaths = new ArraySet();
                    }
                    Collections.addAll(ps.oldCodePaths, deletedPackage.baseCodePath);
                    if (deletedPackage.splitCodePaths != null) {
                        Collections.addAll(ps.oldCodePaths, deletedPackage.splitCodePaths);
                    }
                } else {
                    ps.oldCodePaths = null;
                }
                if (ps.childPackageNames != null) {
                    for (int i = ps.childPackageNames.size() - 1; i >= 0; i--) {
                        String childPkgName = ps.childPackageNames.get(i);
                        PackageSetting childPs = this.mSettings.mPackages.get(childPkgName);
                        childPs.oldCodePaths = ps.oldCodePaths;
                    }
                }
                prepareAppDataAfterInstallLIF(newPackage);
                addedPkg = true;
            } catch (PackageManagerException e) {
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
            }
        }
        if (res.returnCode != 1) {
            if (DEBUG_INSTALL) {
                Slog.d(TAG, "Install failed, rolling pack: " + pkgName);
            }
            if (addedPkg) {
                deletePackageLIF(pkgName, null, true, allUsers, deleteFlags, res.removedInfo, true, null);
            }
            if (!deletedPkg) {
                return;
            }
            if (DEBUG_INSTALL) {
                Slog.d(TAG, "Install failed, reinstalling: " + deletedPackage);
            }
            File restoreFile = new File(deletedPackage.codePath);
            boolean oldExternal = isExternal(deletedPackage);
            int oldParseFlags = this.mDefParseFlags | 2 | (deletedPackage.isForwardLocked() ? 16 : 0) | (oldExternal ? 32 : 0);
            try {
                scanPackageTracedLI(restoreFile, oldParseFlags, 72, origUpdateTime, (UserHandle) null);
                synchronized (this.mPackages) {
                    setInstallerPackageNameLPw(deletedPackage, installerPackageName);
                    updatePermissionsLPw(deletedPackage, 1);
                    this.mSettings.writeLPr();
                }
                Slog.i(TAG, "Successfully restored package : " + pkgName + " after failed upgrade");
                return;
            } catch (PackageManagerException e2) {
                Slog.e(TAG, "Failed to restore package : " + pkgName + " after failed upgrade: " + e2.getMessage());
                return;
            }
        }
        synchronized (this.mPackages) {
            PackageSetting ps2 = this.mSettings.peekPackageLPr(pkg.packageName);
            if (ps2 != null) {
                res.removedInfo.removedForAllUsers = this.mPackages.get(ps2.name) == null;
                if (res.removedInfo.removedChildPackages != null) {
                    int childCount = res.removedInfo.removedChildPackages.size();
                    for (int i2 = childCount - 1; i2 >= 0; i2--) {
                        String childPackageName = res.removedInfo.removedChildPackages.keyAt(i2);
                        if (res.addedChildPackages.containsKey(childPackageName)) {
                            res.removedInfo.removedChildPackages.removeAt(i2);
                        } else {
                            PackageRemovedInfo childInfo = res.removedInfo.removedChildPackages.valueAt(i2);
                            childInfo.removedForAllUsers = this.mPackages.get(childInfo.removedPackage) == null;
                        }
                    }
                }
            }
        }
    }

    private void replaceSystemPackageLIF(PackageParser.Package deletedPackage, PackageParser.Package pkg, int policyFlags, int scanFlags, UserHandle user, int[] allUsers, String installerPackageName, PackageInstalledInfo res) throws Throwable {
        boolean oldPkgInstalled;
        boolean disabledSystem;
        PackageParser.Package newPackage;
        PackageSetting ps;
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "replaceSystemPackageLI: new=" + pkg + ", old=" + deletedPackage);
        }
        boolean isOldPkgVendorApp = isVendorApp(deletedPackage);
        String packageName = deletedPackage.packageName;
        synchronized (this.mPackages) {
            oldPkgInstalled = this.mSettings.mPackages.get(packageName).getInstalled(UserHandle.myUserId());
        }
        removePackageLI(deletedPackage, true);
        synchronized (this.mPackages) {
            disabledSystem = disableSystemPackageLPw(deletedPackage, pkg);
        }
        if (!disabledSystem) {
            res.removedInfo.args = createInstallArgsForExisting(0, deletedPackage.applicationInfo.getCodePath(), deletedPackage.applicationInfo.getResourcePath(), InstructionSets.getAppDexInstructionSets(deletedPackage.applicationInfo));
        } else {
            res.removedInfo.args = null;
            PackageSetting ps2 = this.mSettings.mPackages.get(packageName);
            if (ps2 != null && (isVendorApp(deletedPackage) || isSystemApp(deletedPackage))) {
                if (InstructionSets.getPrimaryInstructionSet(ps2.pkg.applicationInfo) == null) {
                    Slog.d(TAG, "Try to remove dex file, but no primary cpu abi.");
                } else {
                    try {
                        this.mInstaller.rmdexcache(ps2.pkg.baseCodePath, InstructionSets.getPrimaryInstructionSet(ps2.pkg.applicationInfo));
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.d(TAG, "Try to remove dex file but failed, code path: " + ps2.pkg.baseCodePath);
                    }
                }
            }
        }
        clearAppDataLIF(pkg, -1, 515);
        clearAppProfilesLIF(deletedPackage, -1);
        res.setReturnCode(1);
        if (!isOldPkgVendorApp) {
            pkg.setApplicationInfoFlags(128, 128);
        }
        try {
            newPackage = scanPackageTracedLI(pkg, policyFlags, scanFlags, 0L, user);
            try {
                PackageSetting deletedPkgSetting = (PackageSetting) deletedPackage.mExtras;
                setInstallAndUpdateTime(newPackage, deletedPkgSetting.firstInstallTime, System.currentTimeMillis());
                if (res.returnCode == 1) {
                    int deletedChildCount = deletedPackage.childPackages != null ? deletedPackage.childPackages.size() : 0;
                    int newChildCount = newPackage.childPackages != null ? newPackage.childPackages.size() : 0;
                    for (int i = 0; i < deletedChildCount; i++) {
                        PackageParser.Package deletedChildPkg = (PackageParser.Package) deletedPackage.childPackages.get(i);
                        boolean childPackageDeleted = true;
                        int j = 0;
                        while (true) {
                            if (j >= newChildCount) {
                                break;
                            }
                            PackageParser.Package newChildPkg = (PackageParser.Package) newPackage.childPackages.get(j);
                            if (!deletedChildPkg.packageName.equals(newChildPkg.packageName)) {
                                j++;
                            } else {
                                childPackageDeleted = false;
                                break;
                            }
                        }
                        if (childPackageDeleted && (ps = this.mSettings.getDisabledSystemPkgLPr(deletedChildPkg.packageName)) != null && res.removedInfo.removedChildPackages != null) {
                            PackageRemovedInfo removedChildRes = res.removedInfo.removedChildPackages.get(deletedChildPkg.packageName);
                            removePackageDataLIF(ps, allUsers, removedChildRes, 0, false);
                            removedChildRes.removedForAllUsers = this.mPackages.get(ps.name) == null;
                        }
                    }
                    updateSettingsLI(newPackage, installerPackageName, allUsers, res, user);
                    prepareAppDataAfterInstallLIF(newPackage);
                }
            } catch (PackageManagerException e2) {
                e = e2;
                res.setReturnCode(-110);
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
            }
        } catch (PackageManagerException e3) {
            e = e3;
            newPackage = null;
        }
        if (res.returnCode == 1) {
            return;
        }
        if (newPackage != null) {
            removeInstalledPackageLI(newPackage, true);
        }
        int adjustFlags = policyFlags;
        if (isOldPkgVendorApp) {
            adjustFlags = policyFlags | 8192;
        }
        try {
            scanPackageTracedLI(deletedPackage, adjustFlags, 8, 0L, user);
        } catch (PackageManagerException e4) {
            Slog.e(TAG, "Failed to restore original package: " + e4.getMessage());
        }
        synchronized (this.mPackages) {
            if (disabledSystem) {
                try {
                    enableSystemPackageLPw(deletedPackage);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            }
            setInstallerPackageNameLPw(deletedPackage, installerPackageName);
            updatePermissionsLPw(deletedPackage, 1);
            if (!oldPkgInstalled) {
                PackageSetting oldPkgSetting = this.mSettings.mPackages.get(packageName);
                if (oldPkgSetting != null) {
                    try {
                        oldPkgSetting.setUserState(UserHandle.myUserId(), 0L, 0, false, true, true, false, false, null, null, null, false, 0, 0);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
            this.mSettings.writeLPr();
            Slog.i(TAG, "Successfully restored package : " + deletedPackage.packageName + " after failed upgrade");
        }
    }

    private String getParentOrChildPackageChangedSharedUser(PackageParser.Package oldPkg, PackageParser.Package newPkg) {
        if (!Objects.equals(oldPkg.mSharedUserId, newPkg.mSharedUserId)) {
            return newPkg.packageName;
        }
        int oldChildCount = oldPkg.childPackages != null ? oldPkg.childPackages.size() : 0;
        int newChildCount = newPkg.childPackages != null ? newPkg.childPackages.size() : 0;
        for (int i = 0; i < newChildCount; i++) {
            PackageParser.Package newChildPkg = (PackageParser.Package) newPkg.childPackages.get(i);
            for (int j = 0; j < oldChildCount; j++) {
                PackageParser.Package oldChildPkg = (PackageParser.Package) oldPkg.childPackages.get(j);
                if (newChildPkg.packageName.equals(oldChildPkg.packageName) && !Objects.equals(newChildPkg.mSharedUserId, oldChildPkg.mSharedUserId)) {
                    return newChildPkg.packageName;
                }
            }
        }
        return null;
    }

    private void removeNativeBinariesLI(PackageSetting ps) {
        PackageSetting childPs;
        if (ps == null) {
            return;
        }
        NativeLibraryHelper.removeNativeBinariesLI(ps.legacyNativeLibraryPathString);
        int childCount = ps.childPackageNames != null ? ps.childPackageNames.size() : 0;
        for (int i = 0; i < childCount; i++) {
            synchronized (this.mPackages) {
                childPs = this.mSettings.peekPackageLPr(ps.childPackageNames.get(i));
            }
            if (childPs != null) {
                NativeLibraryHelper.removeNativeBinariesLI(childPs.legacyNativeLibraryPathString);
            }
        }
    }

    private void enableSystemPackageLPw(PackageParser.Package pkg) {
        this.mSettings.enableSystemPackageLPw(pkg.packageName);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            this.mSettings.enableSystemPackageLPw(childPkg.packageName);
        }
    }

    private boolean disableSystemPackageLPw(PackageParser.Package oldPkg, PackageParser.Package newPkg) {
        boolean disabled = this.mSettings.disableSystemPackageLPw(oldPkg.packageName, true);
        int childCount = oldPkg.childPackages != null ? oldPkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) oldPkg.childPackages.get(i);
            boolean replace = newPkg.hasChildPackage(childPkg.packageName);
            disabled |= this.mSettings.disableSystemPackageLPw(childPkg.packageName, replace);
        }
        return disabled;
    }

    private void setInstallerPackageNameLPw(PackageParser.Package pkg, String installerPackageName) {
        this.mSettings.setInstallerPackageName(pkg.packageName, installerPackageName);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            this.mSettings.setInstallerPackageName(childPkg.packageName, installerPackageName);
        }
    }

    private int[] revokeUnusedSharedUserPermissionsLPw(SharedUserSetting su, int[] allUserIds) {
        BasePermission bp;
        BasePermission bp2;
        ArraySet<String> usedPermissions = new ArraySet<>();
        int packageCount = su.packages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageSetting ps = su.packages.valueAt(i);
            if (ps.pkg != null) {
                int requestedPermCount = ps.pkg.requestedPermissions.size();
                for (int j = 0; j < requestedPermCount; j++) {
                    String permission = (String) ps.pkg.requestedPermissions.get(j);
                    if (this.mSettings.mPermissions.get(permission) != null) {
                        usedPermissions.add(permission);
                    }
                }
            }
        }
        PermissionsState permissionsState = su.getPermissionsState();
        List<PermissionsState.PermissionState> installPermStates = permissionsState.getInstallPermissionStates();
        int installPermCount = installPermStates.size();
        for (int i2 = installPermCount - 1; i2 >= 0; i2--) {
            PermissionsState.PermissionState permissionState = installPermStates.get(i2);
            if (!usedPermissions.contains(permissionState.getName()) && (bp2 = this.mSettings.mPermissions.get(permissionState.getName())) != null) {
                permissionsState.revokeInstallPermission(bp2);
                permissionsState.updatePermissionFlags(bp2, -1, DhcpPacket.MAX_OPTION_LEN, 0);
            }
        }
        int[] runtimePermissionChangedUserIds = EmptyArray.INT;
        for (int userId : allUserIds) {
            List<PermissionsState.PermissionState> runtimePermStates = permissionsState.getRuntimePermissionStates(userId);
            int runtimePermCount = runtimePermStates.size();
            for (int i3 = runtimePermCount - 1; i3 >= 0; i3--) {
                PermissionsState.PermissionState permissionState2 = runtimePermStates.get(i3);
                if (!usedPermissions.contains(permissionState2.getName()) && (bp = this.mSettings.mPermissions.get(permissionState2.getName())) != null) {
                    permissionsState.revokeRuntimePermission(bp, userId);
                    permissionsState.updatePermissionFlags(bp, userId, DhcpPacket.MAX_OPTION_LEN, 0);
                    runtimePermissionChangedUserIds = ArrayUtils.appendInt(runtimePermissionChangedUserIds, userId);
                }
            }
        }
        return runtimePermissionChangedUserIds;
    }

    private void updateSettingsLI(PackageParser.Package newPackage, String installerPackageName, int[] allUsers, PackageInstalledInfo res, UserHandle user) {
        updateSettingsInternalLI(newPackage, installerPackageName, allUsers, res.origUsers, res, user);
        int childCount = newPackage.childPackages != null ? newPackage.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPackage = (PackageParser.Package) newPackage.childPackages.get(i);
            PackageInstalledInfo childRes = res.addedChildPackages.get(childPackage.packageName);
            updateSettingsInternalLI(childPackage, installerPackageName, allUsers, childRes.origUsers, childRes, user);
        }
    }

    private void updateSettingsInternalLI(PackageParser.Package newPackage, String installerPackageName, int[] allUsers, int[] installedForUsers, PackageInstalledInfo res, UserHandle user) {
        Trace.traceBegin(1048576L, "updateSettings");
        String pkgName = newPackage.packageName;
        synchronized (this.mPackages) {
            this.mSettings.setInstallStatus(pkgName, 0);
            Trace.traceBegin(1048576L, "writeSettings");
            this.mSettings.writeLPr();
            Trace.traceEnd(1048576L);
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "New package installed in " + newPackage.codePath);
        }
        synchronized (this.mPackages) {
            updatePermissionsLPw(newPackage.packageName, newPackage, (newPackage.permissions.size() > 0 ? 1 : 0) | 2);
            PackageSetting ps = this.mSettings.mPackages.get(pkgName);
            int userId = user.getIdentifier();
            if (ps != null) {
                if (isSystemApp(newPackage)) {
                    if (DEBUG_INSTALL) {
                        Slog.d(TAG, "Implicitly enabling system package on upgrade: " + pkgName);
                    }
                    if (res.origUsers != null) {
                        for (int origUserId : res.origUsers) {
                            if (userId == -1 || userId == origUserId) {
                                ps.setEnabled(0, origUserId, installerPackageName);
                            }
                        }
                    }
                    if (allUsers != null && installedForUsers != null) {
                        for (int currentUserId : allUsers) {
                            boolean installed = ArrayUtils.contains(installedForUsers, currentUserId);
                            if (DEBUG_INSTALL) {
                                Slog.d(TAG, "    user " + currentUserId + " => " + installed);
                            }
                            ps.setInstalled(installed, currentUserId);
                        }
                    }
                }
                if (userId != -1) {
                    ps.setInstalled(true, userId);
                    ps.setEnabled(0, userId, installerPackageName);
                }
            }
            res.name = pkgName;
            res.uid = newPackage.applicationInfo.uid;
            res.pkg = newPackage;
            this.mSettings.setInstallStatus(pkgName, 1);
            this.mSettings.setInstallerPackageName(pkgName, installerPackageName);
            res.setReturnCode(1);
            Trace.traceBegin(1048576L, "writeSettings");
            this.mSettings.writeLPr();
            Trace.traceEnd(1048576L);
        }
        Trace.traceEnd(1048576L);
    }

    private void installPackageTracedLI(InstallArgs args, PackageInstalledInfo res) {
        try {
            Trace.traceBegin(1048576L, "installPackage");
            installPackageLI(args, res);
        } finally {
            Trace.traceEnd(1048576L);
        }
    }

    private void installPackageLI(InstallArgs args, PackageInstalledInfo res) throws Throwable {
        PackageSetting ps;
        int i;
        Throwable th;
        Throwable th2;
        int installFlags = args.installFlags;
        String installerPackageName = args.installerPackageName;
        String volumeUuid = args.volumeUuid;
        File tmpPackageFile = new File(args.getCodePath());
        boolean forwardLocked = (installFlags & 1) != 0;
        boolean onExternal = ((installFlags & 8) == 0 && args.volumeUuid == null) ? false : true;
        boolean ephemeral = (installFlags & 2048) != 0;
        boolean forceSdk = (installFlags & 8192) != 0;
        boolean replace = false;
        int scanFlags = args.move != null ? 16408 : 24;
        if ((installFlags & 4096) != 0) {
            scanFlags |= 131072;
        }
        res.setReturnCode(1);
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "installPackageLI: path=" + tmpPackageFile);
        }
        if (ephemeral && (forwardLocked || onExternal)) {
            Slog.i(TAG, "Incompatible ephemeral install; fwdLocked=" + forwardLocked + " external=" + onExternal);
            res.setReturnCode(-116);
            return;
        }
        int parseFlags = this.mDefParseFlags | 2 | 1024 | (forwardLocked ? 16 : 0) | (onExternal ? 32 : 0) | (ephemeral ? 2048 : 0) | (forceSdk ? 4096 : 0);
        PackageParser pp = new PackageParser();
        pp.setSeparateProcesses(this.mSeparateProcesses);
        pp.setDisplayMetrics(this.mMetrics);
        Trace.traceBegin(1048576L, "parsePackage");
        try {
            try {
                if (DEBUG_INSTALL) {
                    Slog.i(TAG, "Start parsing apk: " + installerPackageName);
                }
                PackageParser.Package pkg = pp.parsePackage(tmpPackageFile, parseFlags);
                if (DEBUG_INSTALL) {
                    Slog.i(TAG, "Parsing done for apk: " + installerPackageName);
                }
                Trace.traceEnd(1048576L);
                if (pkg.childPackages != null) {
                    synchronized (this.mPackages) {
                        int childCount = pkg.childPackages.size();
                        for (int i2 = 0; i2 < childCount; i2++) {
                            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i2);
                            PackageInstalledInfo childRes = new PackageInstalledInfo();
                            childRes.setReturnCode(1);
                            childRes.pkg = childPkg;
                            childRes.name = childPkg.packageName;
                            PackageSetting childPs = this.mSettings.peekPackageLPr(childPkg.packageName);
                            if (childPs != null) {
                                childRes.origUsers = childPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                            }
                            if (this.mPackages.containsKey(childPkg.packageName)) {
                                childRes.removedInfo = new PackageRemovedInfo();
                                childRes.removedInfo.removedPackage = childPkg.packageName;
                            }
                            if (res.addedChildPackages == null) {
                                res.addedChildPackages = new ArrayMap<>();
                            }
                            res.addedChildPackages.put(childPkg.packageName, childRes);
                        }
                    }
                }
                if (TextUtils.isEmpty(pkg.cpuAbiOverride)) {
                    pkg.cpuAbiOverride = args.abiOverride;
                }
                String pkgName = pkg.packageName;
                res.name = pkgName;
                if ((pkg.applicationInfo.flags & 256) != 0 && (installFlags & 4) == 0) {
                    res.setError(-15, "installPackageLI");
                    return;
                }
                try {
                    if (args.certificates != null) {
                        try {
                            PackageParser.populateCertificates(pkg, args.certificates);
                        } catch (PackageParser.PackageParserException e) {
                            PackageParser.collectCertificates(pkg, parseFlags);
                        }
                    } else {
                        PackageParser.collectCertificates(pkg, parseFlags);
                    }
                    String oldCodePath = null;
                    boolean systemApp = false;
                    boolean vendorApp = false;
                    synchronized (this.mPackages) {
                        if ((installFlags & 2) != 0) {
                            String oldName = this.mSettings.mRenamedPackages.get(pkgName);
                            if (pkg.mOriginalPackages != null && pkg.mOriginalPackages.contains(oldName) && this.mPackages.containsKey(oldName)) {
                                pkg.setPackageName(oldName);
                                pkgName = pkg.packageName;
                                replace = true;
                                if (DEBUG_INSTALL) {
                                    Slog.d(TAG, "Replacing existing renamed package: oldName=" + oldName + " pkgName=" + pkgName);
                                }
                            } else if (this.mPackages.containsKey(pkgName)) {
                                replace = true;
                                if (DEBUG_INSTALL) {
                                    Slog.d(TAG, "Replace existing pacakge: " + pkgName);
                                }
                            }
                            if (pkg.parentPackage != null) {
                                res.setError(-106, "Package " + pkg.packageName + " is child of package " + pkg.parentPackage.parentPackage + ". Child packages can be updated only through the parent package.");
                                return;
                            }
                            if (replace) {
                                PackageParser.Package oldPackage = this.mPackages.get(pkgName);
                                int oldTargetSdk = oldPackage.applicationInfo.targetSdkVersion;
                                int newTargetSdk = pkg.applicationInfo.targetSdkVersion;
                                if (oldTargetSdk > 22 && newTargetSdk <= 22) {
                                    res.setError(-26, "Package " + pkg.packageName + " new target SDK " + newTargetSdk + " doesn't support runtime permissions but the old target SDK " + oldTargetSdk + " does.");
                                    return;
                                } else if (oldPackage.parentPackage != null) {
                                    res.setError(-106, "Package " + pkg.packageName + " is child of package " + oldPackage.parentPackage + ". Child packages can be updated only through the parent package.");
                                    return;
                                }
                            }
                            ps = this.mSettings.mPackages.get(pkgName);
                            if (ps != null) {
                                if (DEBUG_INSTALL) {
                                    Slog.d(TAG, "Existing package: " + ps);
                                }
                                if (!shouldCheckUpgradeKeySetLP(ps, scanFlags)) {
                                    try {
                                        verifySignaturesLP(ps, pkg);
                                    } catch (PackageManagerException e2) {
                                        res.setError(e2.error, e2.getMessage());
                                        return;
                                    }
                                } else if (!checkUpgradeKeySetLP(ps, pkg)) {
                                    res.setError(-7, "Package " + pkg.packageName + " upgrade keys do not match the previously installed version");
                                    return;
                                }
                                oldCodePath = this.mSettings.mPackages.get(pkgName).codePathString;
                                if (ps.pkg != null && ps.pkg.applicationInfo != null) {
                                    systemApp = (ps.pkg.applicationInfo.flags & 1) != 0;
                                    vendorApp = isVendorApp(ps.pkg);
                                }
                                res.origUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                            }
                            int N = pkg.permissions.size();
                            for (i = N - 1; i >= 0; i--) {
                                PackageParser.Permission perm = (PackageParser.Permission) pkg.permissions.get(i);
                                BasePermission bp = this.mSettings.mPermissions.get(perm.info.name);
                                if (bp != null) {
                                    boolean sigsOk = (bp.sourcePackage.equals(pkg.packageName) && (bp.packageSetting instanceof PackageSetting) && shouldCheckUpgradeKeySetLP((PackageSetting) bp.packageSetting, scanFlags)) ? checkUpgradeKeySetLP((PackageSetting) bp.packageSetting, pkg) : compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) == 0;
                                    if (!sigsOk) {
                                        if (!bp.sourcePackage.equals(PLATFORM_PACKAGE_NAME)) {
                                            res.setError(-112, "Package " + pkg.packageName + " attempting to redeclare permission " + perm.info.name + " already owned by " + bp.sourcePackage);
                                            res.origPermission = perm.info.name;
                                            res.origPackage = bp.sourcePackage;
                                            return;
                                        }
                                        Slog.w(TAG, "Package " + pkg.packageName + " attempting to redeclare system permission " + perm.info.name + "; ignoring new declaration");
                                        pkg.permissions.remove(i);
                                    } else if (!PLATFORM_PACKAGE_NAME.equals(pkg.packageName) && (perm.info.protectionLevel & 15) == 1 && bp != null && !bp.isRuntime()) {
                                        Slog.w(TAG, "Package " + pkg.packageName + " trying to change a non-runtime permission " + perm.info.name + " to runtime; keeping old protection level");
                                        perm.info.protectionLevel = bp.protectionLevel;
                                    }
                                }
                            }
                            if (!systemApp || vendorApp) {
                                if (!onExternal) {
                                    res.setError(-19, "Cannot install updates to system apps on sdcard");
                                    return;
                                } else if (ephemeral) {
                                    res.setError(-116, "Cannot update a system app with an ephemeral app");
                                    return;
                                }
                            }
                            if (args.move == null) {
                                scanFlags = scanFlags | 2 | 8192;
                                synchronized (this.mPackages) {
                                    PackageSetting ps2 = this.mSettings.mPackages.get(pkgName);
                                    if (ps2 == null) {
                                        res.setError(-110, "Missing settings for moved package " + pkgName);
                                    }
                                    pkg.applicationInfo.primaryCpuAbi = ps2.primaryCpuAbiString;
                                    pkg.applicationInfo.secondaryCpuAbi = ps2.secondaryCpuAbiString;
                                }
                            } else if (!forwardLocked && !pkg.applicationInfo.isExternalAsec()) {
                                scanFlags |= 2;
                                try {
                                    String abiOverride = TextUtils.isEmpty(pkg.cpuAbiOverride) ? args.abiOverride : pkg.cpuAbiOverride;
                                    derivePackageAbi(pkg, new File(pkg.codePath), abiOverride, true);
                                    synchronized (this.mPackages) {
                                        try {
                                            updateSharedLibrariesLPw(pkg, null);
                                        } catch (PackageManagerException e3) {
                                            Slog.e(TAG, "updateSharedLibrariesLPw failed: " + e3.getMessage());
                                        }
                                    }
                                    Trace.traceBegin(1048576L, "dexopt");
                                    this.mPackageDexOptimizer.performDexOpt(pkg, pkg.usesLibraryFiles, null, false, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(2));
                                    Trace.traceEnd(1048576L);
                                    BackgroundDexOptService.notifyPackageChanged(pkg.packageName);
                                } catch (PackageManagerException pme) {
                                    Slog.e(TAG, "Error deriving application ABI", pme);
                                    res.setError(-110, "Error deriving application ABI");
                                    return;
                                }
                            }
                            if (args.doRename(res.returnCode, pkg, oldCodePath)) {
                                res.setError(-4, "Failed rename");
                                return;
                            }
                            startIntentFilterVerifications(args.user.getIdentifier(), replace, pkg);
                            Throwable th3 = null;
                            AutoCloseable autoCloseable = null;
                            try {
                                PackageFreezer freezer = freezePackageForInstall(pkgName, installFlags, "installPackageLI");
                                if (replace) {
                                    replacePackageLIF(pkg, parseFlags, scanFlags | 2048, args.user, installerPackageName, res);
                                } else {
                                    installNewPackageLIF(pkg, parseFlags, scanFlags | 1024, args.user, installerPackageName, volumeUuid, res);
                                }
                                if (freezer != null) {
                                    try {
                                        freezer.close();
                                    } catch (Throwable th4) {
                                        th3 = th4;
                                    }
                                }
                                if (th3 != null) {
                                    throw th3;
                                }
                                if (DEBUG_INSTALL) {
                                    Slog.i(TAG, "Installation done for package: " + installerPackageName);
                                }
                                if ("1".equals(SystemProperties.get("ro.globalpq.support"))) {
                                    Slog.i(TAG, "UpdateAPCategoryInfo for package: " + pkg.packageName);
                                    IBinder b = ServiceManager.getService("appdetection");
                                    if (b != null) {
                                        IAppDetectionService mAppDetectionService = IAppDetectionService.Stub.asInterface(b);
                                        try {
                                            mAppDetectionService.updateAPCategoryInfo(pkg.packageName);
                                        } catch (RemoteException e4) {
                                            e4.printStackTrace();
                                        }
                                    }
                                }
                                synchronized (this.mPackages) {
                                    PackageSetting ps3 = this.mSettings.mPackages.get(pkgName);
                                    if (ps3 != null) {
                                        res.newUsers = ps3.queryInstalledUsers(sUserManager.getUserIds(), true);
                                    }
                                    int childCount2 = pkg.childPackages != null ? pkg.childPackages.size() : 0;
                                    for (int i3 = 0; i3 < childCount2; i3++) {
                                        PackageParser.Package childPkg2 = (PackageParser.Package) pkg.childPackages.get(i3);
                                        PackageInstalledInfo childRes2 = res.addedChildPackages.get(childPkg2.packageName);
                                        PackageSetting childPs2 = this.mSettings.peekPackageLPr(childPkg2.packageName);
                                        if (childPs2 != null) {
                                            childRes2.newUsers = childPs2.queryInstalledUsers(sUserManager.getUserIds(), true);
                                        }
                                    }
                                }
                                return;
                            } catch (Throwable th5) {
                                try {
                                    throw th5;
                                } catch (Throwable th6) {
                                    th = th5;
                                    th2 = th6;
                                    if (0 != 0) {
                                        try {
                                            autoCloseable.close();
                                        } catch (Throwable th7) {
                                            if (th == null) {
                                                th = th7;
                                            } else if (th != th7) {
                                                th.addSuppressed(th7);
                                            }
                                        }
                                    }
                                    if (th != null) {
                                        throw th2;
                                    }
                                    throw th;
                                }
                            }
                        } else {
                            ps = this.mSettings.mPackages.get(pkgName);
                            if (ps != null) {
                            }
                            int N2 = pkg.permissions.size();
                            while (i >= 0) {
                            }
                            if (!systemApp) {
                                if (!onExternal) {
                                }
                            }
                            if (args.move == null) {
                            }
                            if (args.doRename(res.returnCode, pkg, oldCodePath)) {
                            }
                        }
                    }
                } catch (PackageParser.PackageParserException e5) {
                    res.setError("Failed collect during installPackageLI", e5);
                }
            } catch (Throwable th8) {
                Trace.traceEnd(1048576L);
                throw th8;
            }
        } catch (PackageParser.PackageParserException e6) {
            res.setError("Failed parse during installPackageLI", e6);
            Trace.traceEnd(1048576L);
        }
    }

    private void startIntentFilterVerifications(int userId, boolean replacing, PackageParser.Package pkg) {
        if (this.mIntentFilterVerifierComponent == null) {
            Slog.w(TAG, "No IntentFilter verification will not be done as there is no IntentFilterVerifier available!");
            return;
        }
        int verifierUid = getPackageUid(this.mIntentFilterVerifierComponent.getPackageName(), 268435456, userId == -1 ? 0 : userId);
        Message msg = this.mHandler.obtainMessage(17);
        msg.obj = new IFVerificationParams(pkg, replacing, userId, verifierUid);
        this.mHandler.sendMessage(msg);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            PackageParser.Package childPkg = (PackageParser.Package) pkg.childPackages.get(i);
            Message msg2 = this.mHandler.obtainMessage(17);
            msg2.obj = new IFVerificationParams(childPkg, replacing, userId, verifierUid);
            this.mHandler.sendMessage(msg2);
        }
    }

    private void verifyIntentFiltersIfNeeded(int userId, int verifierUid, boolean replacing, PackageParser.Package pkg) {
        int size = pkg.activities.size();
        if (size == 0) {
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No activity, so no need to verify any IntentFilter!");
                return;
            }
            return;
        }
        boolean hasDomainURLs = hasDomainURLs(pkg);
        if (!hasDomainURLs) {
            if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No domain URLs, so no need to verify any IntentFilter!");
                return;
            }
            return;
        }
        if (DEBUG_DOMAIN_VERIFICATION) {
            Slog.d(TAG, "Checking for userId:" + userId + " if any IntentFilter from the " + size + " Activities needs verification ...");
        }
        int count = 0;
        String packageName = pkg.packageName;
        synchronized (this.mPackages) {
            if (!replacing) {
                IntentFilterVerificationInfo ivi = this.mSettings.getIntentFilterVerificationLPr(packageName);
                if (ivi != null) {
                    if (DEBUG_DOMAIN_VERIFICATION) {
                        Slog.i(TAG, "Package " + packageName + " already verified: status=" + ivi.getStatusString());
                    }
                    return;
                }
            }
            boolean needToVerify = false;
            for (PackageParser.Activity a : pkg.activities) {
                Iterator filter$iterator = a.intents.iterator();
                while (true) {
                    if (filter$iterator.hasNext()) {
                        PackageParser.ActivityIntentInfo filter = (PackageParser.ActivityIntentInfo) filter$iterator.next();
                        if (filter.needsVerification() && needsNetworkVerificationLPr(filter)) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.d(TAG, "Intent filter needs verification, so processing all filters");
                            }
                            needToVerify = true;
                        }
                    }
                }
            }
            if (needToVerify) {
                int verificationId = this.mIntentFilterVerificationToken;
                this.mIntentFilterVerificationToken = verificationId + 1;
                for (PackageParser.Activity a2 : pkg.activities) {
                    for (PackageParser.ActivityIntentInfo filter2 : a2.intents) {
                        if (filter2.handlesWebUris(true) && needsNetworkVerificationLPr(filter2)) {
                            if (DEBUG_DOMAIN_VERIFICATION) {
                                Slog.d(TAG, "Verification needed for IntentFilter:" + filter2.toString());
                            }
                            this.mIntentFilterVerifier.addOneIntentFilterVerification(verifierUid, userId, verificationId, filter2, packageName);
                            count++;
                        }
                    }
                }
            }
            if (count > 0) {
                if (DEBUG_DOMAIN_VERIFICATION) {
                    Slog.d(TAG, "Starting " + count + " IntentFilter verification" + (count > 1 ? "s" : "") + " for userId:" + userId);
                }
                this.mIntentFilterVerifier.startVerifications(userId);
            } else if (DEBUG_DOMAIN_VERIFICATION) {
                Slog.d(TAG, "No filters or not all autoVerify for " + packageName);
            }
        }
    }

    private boolean needsNetworkVerificationLPr(PackageParser.ActivityIntentInfo filter) {
        ComponentName cn = filter.activity.getComponentName();
        String packageName = cn.getPackageName();
        IntentFilterVerificationInfo ivi = this.mSettings.getIntentFilterVerificationLPr(packageName);
        if (ivi == null) {
            return true;
        }
        int status = ivi.getStatus();
        switch (status) {
        }
        return true;
    }

    private static boolean isMultiArch(ApplicationInfo info) {
        return (info.flags & Integer.MIN_VALUE) != 0;
    }

    private static boolean isExternal(PackageParser.Package pkg) {
        return (pkg.applicationInfo.flags & 262144) != 0;
    }

    private static boolean isExternal(PackageSetting ps) {
        return (ps.pkgFlags & 262144) != 0;
    }

    private static boolean isEphemeral(PackageParser.Package pkg) {
        return pkg.applicationInfo.isEphemeralApp();
    }

    private static boolean isEphemeral(PackageSetting ps) {
        if (ps.pkg != null) {
            return isEphemeral(ps.pkg);
        }
        return false;
    }

    private static boolean isSystemApp(PackageParser.Package pkg) {
        return (pkg.applicationInfo.flags & 1) != 0;
    }

    private static boolean isPrivilegedApp(PackageParser.Package pkg) {
        return (pkg.applicationInfo.privateFlags & 8) != 0;
    }

    private static boolean hasDomainURLs(PackageParser.Package pkg) {
        return (pkg.applicationInfo.privateFlags & 16) != 0;
    }

    private static boolean isSystemApp(PackageSetting ps) {
        return (ps.pkgFlags & 1) != 0;
    }

    private static boolean isUpdatedSystemApp(PackageSetting ps) {
        return (ps.pkgFlags & 128) != 0;
    }

    static boolean locationIsOperator(File path) {
        if (path != null) {
            try {
                return path.getCanonicalPath().contains("vendor/operator/app");
            } catch (IOException e) {
                Slog.e(TAG, "Unable to access code path " + path);
                return false;
            }
        }
        return false;
    }

    static boolean isVendorApp(PackageSetting ps) {
        return (ps.pkgFlagsEx & 1) != 0;
    }

    static boolean isVendorApp(PackageParser.Package pkg) {
        return (pkg.applicationInfo.flagsEx & 1) != 0;
    }

    static boolean isVendorApp(ApplicationInfo info) {
        return (info.flagsEx & 1) != 0;
    }

    private int packageFlagsToInstallFlags(PackageSetting ps) {
        int installFlags = 0;
        if (isEphemeral(ps)) {
            installFlags = 2048;
        }
        if (isExternal(ps) && TextUtils.isEmpty(ps.volumeUuid)) {
            installFlags |= 8;
        }
        if (ps.isForwardLocked()) {
            return installFlags | 1;
        }
        return installFlags;
    }

    private String getVolumeUuidForPackage(PackageParser.Package pkg) {
        if (isExternal(pkg)) {
            if (TextUtils.isEmpty(pkg.volumeUuid)) {
                return "primary_physical";
            }
            return pkg.volumeUuid;
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    private Settings.VersionInfo getSettingsVersionForPackage(PackageParser.Package pkg) {
        if (isExternal(pkg)) {
            if (TextUtils.isEmpty(pkg.volumeUuid)) {
                return this.mSettings.getExternalVersion();
            }
            return this.mSettings.findOrCreateVersion(pkg.volumeUuid);
        }
        return this.mSettings.getInternalVersion();
    }

    private void deleteTempPackageFiles() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("vmdl")) {
                    return name.endsWith(".tmp");
                }
                return false;
            }
        };
        for (File file : this.mDrmAppPrivateInstallDir.listFiles(filter)) {
            file.delete();
        }
    }

    public void deletePackageAsUser(String packageName, IPackageDeleteObserver observer, int userId, int flags) {
        deletePackage(packageName, new PackageManager.LegacyPackageDeleteObserver(observer).getBinder(), userId, flags);
    }

    public void deletePackage(final String packageName, final IPackageDeleteObserver2 observer, final int userId, final int deleteFlags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(observer);
        int uid = Binder.getCallingUid();
        final boolean deleteAllUsers = (deleteFlags & 2) != 0;
        final int[] users = deleteAllUsers ? sUserManager.getUserIds() : new int[]{userId};
        if (UserHandle.getUserId(uid) != userId || (deleteAllUsers && users.length > 1)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "deletePackage for user " + userId);
        }
        if (isUserRestricted(userId, "no_uninstall_apps")) {
            try {
                observer.onPackageDeleted(packageName, -3, (String) null);
            } catch (RemoteException e) {
            }
        } else if (!deleteAllUsers && getBlockUninstallForUser(packageName, userId)) {
            try {
                observer.onPackageDeleted(packageName, -4, (String) null);
            } catch (RemoteException e2) {
            }
        } else {
            if (DEBUG_REMOVE) {
                Slog.d(TAG, "deletePackageAsUser: pkg=" + packageName + " user=" + userId + " deleteAllUsers: " + deleteAllUsers);
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int returnCode;
                    int returnCode2;
                    PackageManagerService.this.mHandler.removeCallbacks(this);
                    if (!deleteAllUsers) {
                        returnCode = PackageManagerService.this.deletePackageX(packageName, userId, deleteFlags);
                    } else {
                        int[] blockUninstallUserIds = PackageManagerService.this.getBlockUninstallForUsers(packageName, users);
                        if (ArrayUtils.isEmpty(blockUninstallUserIds)) {
                            returnCode = PackageManagerService.this.deletePackageX(packageName, userId, deleteFlags);
                        } else {
                            int userFlags = deleteFlags & (-3);
                            for (int userId2 : users) {
                                if (!ArrayUtils.contains(blockUninstallUserIds, userId2) && (returnCode2 = PackageManagerService.this.deletePackageX(packageName, userId2, userFlags)) != 1) {
                                    Slog.w(PackageManagerService.TAG, "Package delete failed for user " + userId2 + ", returnCode " + returnCode2);
                                }
                            }
                            returnCode = -4;
                        }
                    }
                    try {
                        observer.onPackageDeleted(packageName, returnCode, (String) null);
                    } catch (RemoteException e3) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            });
        }
    }

    private int[] getBlockUninstallForUsers(String packageName, int[] userIds) {
        int[] result = EMPTY_INT_ARRAY;
        for (int userId : userIds) {
            if (getBlockUninstallForUser(packageName, userId)) {
                result = ArrayUtils.appendInt(result, userId);
            }
        }
        return result;
    }

    public boolean isPackageDeviceAdminOnAnyUser(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (checkUidPermission("android.permission.MANAGE_USERS", callingUid) != 0) {
            EventLog.writeEvent(1397638484, "128599183", -1, "");
            throw new SecurityException("android.permission.MANAGE_USERS permission is required to call this API");
        }
        return isPackageDeviceAdmin(packageName, -1);
    }

    private boolean isPackageDeviceAdmin(String packageName, int userId) {
        int[] users;
        IDevicePolicyManager dpm = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (dpm != null) {
            try {
                ComponentName deviceOwnerComponentName = dpm.getDeviceOwnerComponent(false);
                if (packageName.equals(deviceOwnerComponentName == null ? null : deviceOwnerComponentName.getPackageName())) {
                    return true;
                }
                if (userId == -1) {
                    users = sUserManager.getUserIds();
                } else {
                    users = new int[]{userId};
                }
                for (int i : users) {
                    if (dpm.packageHasActiveAdmins(packageName, i)) {
                        return true;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    private boolean shouldKeepUninstalledPackageLPr(String packageName) {
        if (this.mKeepUninstalledPackages != null) {
            return this.mKeepUninstalledPackages.contains(packageName);
        }
        return false;
    }

    private int deletePackageX(String packageName, int userId, int deleteFlags) {
        int freezeUser;
        Throwable th;
        boolean res;
        PackageRemovedInfo info = new PackageRemovedInfo();
        int removeUser = (deleteFlags & 2) != 0 ? -1 : userId;
        if (isPackageDeviceAdmin(packageName, removeUser)) {
            Slog.w(TAG, "Not removing package " + packageName + ": has active device admin");
            return -2;
        }
        synchronized (this.mPackages) {
            PackageSetting uninstalledPs = this.mSettings.mPackages.get(packageName);
            if (uninstalledPs == null) {
                Slog.w(TAG, "Not removing non-existent package " + packageName);
                return -1;
            }
            int[] allUsers = sUserManager.getUserIds();
            info.origUsers = uninstalledPs.queryInstalledUsers(allUsers, true);
            if (isUpdatedSystemApp(uninstalledPs) && (deleteFlags & 4) == 0) {
                freezeUser = -1;
            } else {
                freezeUser = removeUser;
            }
            synchronized (this.mInstallLock) {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "deletePackageX: pkg=" + packageName + " user=" + userId);
                }
                Throwable th2 = null;
                PackageFreezer freezer = null;
                try {
                    freezer = freezePackageForDelete(packageName, freezeUser, deleteFlags, "deletePackageX");
                    res = deletePackageLIF(packageName, UserHandle.of(removeUser), true, allUsers, deleteFlags | 65536, info, true, null);
                    if (freezer != null) {
                        try {
                            freezer.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                    synchronized (this.mPackages) {
                        if (res) {
                            this.mEphemeralApplicationRegistry.onPackageUninstalledLPw(uninstalledPs.pkg);
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    th = null;
                    if (freezer != null) {
                    }
                    if (th == null) {
                    }
                }
            }
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps != null && isVendorApp(ps)) {
                if (InstructionSets.getPrimaryInstructionSet(ps.pkg.applicationInfo) == null) {
                    Slog.d(TAG, "Try to remove dex file, but no primary cpu abi.");
                } else {
                    try {
                        this.mInstaller.rmdexcache(ps.pkg.baseCodePath, InstructionSets.getPrimaryInstructionSet(ps.pkg.applicationInfo));
                    } catch (InstallerConnection.InstallerException e) {
                        Slog.d(TAG, "Try to remove dex file but failed, code path: " + ps.pkg.baseCodePath);
                    }
                }
            }
            if (res) {
                boolean killApp = (deleteFlags & 8) == 0;
                info.sendPackageRemovedBroadcasts(killApp);
                info.sendSystemPackageUpdatedBroadcasts();
                info.sendSystemPackageAppearedBroadcasts();
            }
            Runtime.getRuntime().gc();
            if (info.args != null) {
                synchronized (this.mInstallLock) {
                    info.args.doPostDeleteLI(true);
                }
            }
            return res ? 1 : -1;
        }
    }

    class PackageRemovedInfo {
        ArrayMap<String, PackageInstalledInfo> appearedChildPackages;
        boolean dataRemoved;
        boolean isUpdate;
        int[] origUsers;
        ArrayMap<String, PackageRemovedInfo> removedChildPackages;
        boolean removedForAllUsers;
        String removedPackage;
        int uid = -1;
        int removedAppId = -1;
        int[] removedUsers = null;
        boolean isRemovedPackageSystemUpdate = false;
        InstallArgs args = null;

        PackageRemovedInfo() {
        }

        void sendPackageRemovedBroadcasts(boolean killApp) {
            sendPackageRemovedBroadcastInternal(killApp);
            int childCount = this.removedChildPackages != null ? this.removedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                PackageRemovedInfo childInfo = this.removedChildPackages.valueAt(i);
                childInfo.sendPackageRemovedBroadcastInternal(killApp);
            }
        }

        void sendSystemPackageUpdatedBroadcasts() {
            if (!this.isRemovedPackageSystemUpdate) {
                return;
            }
            sendSystemPackageUpdatedBroadcastsInternal();
            int childCount = this.removedChildPackages != null ? this.removedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                PackageRemovedInfo childInfo = this.removedChildPackages.valueAt(i);
                if (childInfo.isRemovedPackageSystemUpdate) {
                    childInfo.sendSystemPackageUpdatedBroadcastsInternal();
                }
            }
        }

        void sendSystemPackageAppearedBroadcasts() {
            int packageCount = this.appearedChildPackages != null ? this.appearedChildPackages.size() : 0;
            for (int i = 0; i < packageCount; i++) {
                PackageInstalledInfo installedInfo = this.appearedChildPackages.valueAt(i);
                for (int userId : installedInfo.newUsers) {
                    PackageManagerService.this.sendPackageAddedForUser(installedInfo.name, true, UserHandle.getAppId(installedInfo.uid), userId);
                }
            }
        }

        private void sendSystemPackageUpdatedBroadcastsInternal() {
            Bundle extras = new Bundle(2);
            extras.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            extras.putBoolean("android.intent.extra.REPLACING", true);
            PackageManagerService.this.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.removedPackage, extras, 0, null, null, null);
            PackageManagerService.this.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.removedPackage, extras, 0, null, null, null);
            PackageManagerService.this.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, this.removedPackage, null, null);
        }

        private void sendPackageRemovedBroadcastInternal(boolean killApp) {
            Bundle extras = new Bundle(2);
            extras.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            extras.putBoolean("android.intent.extra.DATA_REMOVED", this.dataRemoved);
            extras.putBoolean("android.intent.extra.DONT_KILL_APP", !killApp);
            if (this.isUpdate || this.isRemovedPackageSystemUpdate) {
                extras.putBoolean("android.intent.extra.REPLACING", true);
            }
            extras.putBoolean("android.intent.extra.REMOVED_FOR_ALL_USERS", this.removedForAllUsers);
            if (this.removedPackage != null) {
                PackageManagerService.this.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, extras, 0, null, null, this.removedUsers);
                if (this.dataRemoved && !this.isRemovedPackageSystemUpdate) {
                    PackageManagerService.this.sendPackageBroadcast("android.intent.action.PACKAGE_FULLY_REMOVED", this.removedPackage, extras, 0, null, null, this.removedUsers);
                }
            }
            if (this.removedAppId < 0) {
                return;
            }
            PackageManagerService.this.sendPackageBroadcast("android.intent.action.UID_REMOVED", null, extras, 0, null, null, this.removedUsers);
        }
    }

    private void removePackageDataLIF(PackageSetting ps, int[] allUserHandles, PackageRemovedInfo outInfo, int flags, boolean writeSettings) {
        PackageParser.Package deletedPkg;
        final PackageSetting deletedPs;
        PackageParser.Package resolvedPkg;
        int[] iArrQueryInstalledUsers;
        String packageName = ps.name;
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "removePackageDataLI: " + ps);
        }
        synchronized (this.mPackages) {
            deletedPkg = this.mPackages.get(packageName);
            deletedPs = this.mSettings.mPackages.get(packageName);
            if (outInfo != null) {
                outInfo.removedPackage = packageName;
                if (deletedPs != null) {
                    iArrQueryInstalledUsers = deletedPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                } else {
                    iArrQueryInstalledUsers = null;
                }
                outInfo.removedUsers = iArrQueryInstalledUsers;
            }
        }
        removePackageLI(ps, (65536 & flags) != 0);
        if ((flags & 1) == 0) {
            if (deletedPkg != null) {
                resolvedPkg = deletedPkg;
            } else {
                resolvedPkg = new PackageParser.Package(ps.name);
                resolvedPkg.setVolumeUuid(ps.volumeUuid);
            }
            destroyAppDataLIF(resolvedPkg, -1, 3);
            destroyAppProfilesLIF(resolvedPkg, -1);
            if (outInfo != null) {
                outInfo.dataRemoved = true;
            }
            schedulePackageCleaning(packageName, -1, true);
        }
        synchronized (this.mPackages) {
            if (deletedPs != null) {
                if ((flags & 1) == 0) {
                    clearIntentFilterVerificationsLPw(deletedPs.name, -1);
                    clearDefaultBrowserIfNeeded(packageName);
                    if (outInfo != null) {
                        this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(packageName);
                        outInfo.removedAppId = this.mSettings.removePackageLPw(packageName);
                    }
                    updatePermissionsLPw(deletedPs.name, null, 0);
                    if (deletedPs.sharedUser != null) {
                        for (int i : UserManagerService.getInstance().getUserIds()) {
                            int userIdToKill = this.mSettings.updateSharedUserPermsLPw(deletedPs, i);
                            if (userIdToKill == -1 || userIdToKill >= 0) {
                                this.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        PackageManagerService.this.killApplication(deletedPs.name, deletedPs.appId, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                                    }
                                });
                                break;
                            }
                        }
                    }
                    clearPackagePreferredActivitiesLPw(deletedPs.name, -1);
                }
                if (allUserHandles != null && outInfo != null && outInfo.origUsers != null) {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Propagating install state across downgrade");
                    }
                    for (int userId : allUserHandles) {
                        boolean installed = ArrayUtils.contains(outInfo.origUsers, userId);
                        if (DEBUG_REMOVE) {
                            Slog.d(TAG, "    user " + userId + " => " + installed);
                        }
                        ps.setInstalled(installed, userId);
                    }
                }
                if (writeSettings) {
                }
            } else if (writeSettings) {
                this.mSettings.writeLPr();
            }
        }
        if (outInfo == null) {
            return;
        }
        removeKeystoreDataIfNeeded(-1, outInfo.removedAppId);
    }

    static boolean locationIsPrivileged(File path) {
        try {
            String privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app").getCanonicalPath();
            String privilegedAppVendorDir = new File(Environment.getVendorDirectory(), "priv-app").getCanonicalPath();
            if (path.getCanonicalPath().startsWith(privilegedAppDir)) {
                return true;
            }
            return path.getCanonicalPath().startsWith(privilegedAppVendorDir);
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + path);
            return false;
        }
    }

    private boolean deleteSystemPackageLIF(PackageParser.Package deletedPkg, PackageSetting deletedPs, int[] allUserHandles, int flags, PackageRemovedInfo outInfo, boolean writeSettings) {
        PackageSetting disabledPs;
        int flags2;
        int parseFlags;
        PackageRemovedInfo childInfo;
        if (deletedPs.parentPackageName != null) {
            Slog.w(TAG, "Attempt to delete child system package " + deletedPkg.packageName);
            return false;
        }
        boolean applyUserRestrictions = (allUserHandles == null || outInfo.origUsers == null) ? false : true;
        boolean isVendor = isVendorApp(deletedPkg);
        synchronized (this.mPackages) {
            disabledPs = this.mSettings.getDisabledSystemPkgLPr(deletedPs.name);
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "deleteSystemPackageLI: newPs=" + deletedPkg.packageName + " disabledPs=" + disabledPs);
        }
        if (disabledPs == null) {
            Slog.w(TAG, "Attempt to delete unknown system package " + deletedPkg.packageName);
            return false;
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "Deleting system pkg from data partition");
        }
        if (DEBUG_REMOVE && applyUserRestrictions) {
            Slog.d(TAG, "Remembering install states:");
            for (int userId : allUserHandles) {
                boolean finstalled = ArrayUtils.contains(outInfo.origUsers, userId);
                Slog.d(TAG, "   u=" + userId + " inst=" + finstalled);
            }
        }
        if (!isVendor) {
            outInfo.isRemovedPackageSystemUpdate = true;
            if (outInfo.removedChildPackages != null) {
                int childCount = deletedPs.childPackageNames != null ? deletedPs.childPackageNames.size() : 0;
                for (int i = 0; i < childCount; i++) {
                    String childPackageName = deletedPs.childPackageNames.get(i);
                    if (disabledPs.childPackageNames != null && disabledPs.childPackageNames.contains(childPackageName) && (childInfo = outInfo.removedChildPackages.get(childPackageName)) != null) {
                        childInfo.isRemovedPackageSystemUpdate = true;
                    }
                }
            }
        }
        if (disabledPs.versionCode < deletedPs.versionCode) {
            flags2 = flags & (-2);
        } else {
            flags2 = flags | 1;
        }
        boolean ret = deleteInstalledPackageLIF(deletedPs, true, flags2, allUserHandles, outInfo, writeSettings, disabledPs.pkg);
        if (!ret) {
            return false;
        }
        synchronized (this.mPackages) {
            enableSystemPackageLPw(disabledPs.pkg);
            removeNativeBinariesLI(deletedPs);
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "Re-installing system package: " + disabledPs);
        }
        int parseFlags2 = this.mDefParseFlags | 4;
        if (isVendor) {
            parseFlags = parseFlags2 | 8192;
        } else {
            parseFlags = parseFlags2 | 65;
            if (locationIsPrivileged(disabledPs.codePath)) {
                parseFlags |= 128;
            }
        }
        try {
            PackageParser.Package newPkg = scanPackageTracedLI(disabledPs.codePath, parseFlags, 32, 0L, (UserHandle) null);
            prepareAppDataAfterInstallLIF(newPkg);
            synchronized (this.mPackages) {
                PackageSetting ps = this.mSettings.mPackages.get(newPkg.packageName);
                ps.getPermissionsState().copyFrom(deletedPs.getPermissionsState());
                updatePermissionsLPw(newPkg.packageName, newPkg, 3);
                if (applyUserRestrictions) {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Propagating install state across reinstall");
                    }
                    for (int userId2 : allUserHandles) {
                        boolean installed = ArrayUtils.contains(outInfo.origUsers, userId2);
                        if (DEBUG_REMOVE) {
                            Slog.d(TAG, "    user " + userId2 + " => " + installed);
                        }
                        ps.setInstalled(installed, userId2);
                        this.mSettings.writeRuntimePermissionsForUserLPr(userId2, false);
                    }
                    this.mSettings.writeAllUsersPackageRestrictionsLPr();
                }
                if (writeSettings) {
                    this.mSettings.writeLPr();
                }
            }
            return true;
        } catch (PackageManagerException e) {
            Slog.w(TAG, "Failed to restore system package:" + deletedPkg.packageName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean deleteInstalledPackageLIF(PackageSetting ps, boolean deleteCodeAndResources, int flags, int[] allUserHandles, PackageRemovedInfo outInfo, boolean writeSettings, PackageParser.Package replacingPackage) {
        int childCount;
        int i;
        PackageSetting childPs;
        int childCount2;
        int i2;
        synchronized (this.mPackages) {
            if (outInfo != null) {
                outInfo.uid = ps.appId;
                if (outInfo != null && outInfo.removedChildPackages != null) {
                    childCount2 = ps.childPackageNames == null ? ps.childPackageNames.size() : 0;
                    for (i2 = 0; i2 < childCount2; i2++) {
                        String childPackageName = ps.childPackageNames.get(i2);
                        PackageSetting childPs2 = this.mSettings.mPackages.get(childPackageName);
                        if (childPs2 == null) {
                            return false;
                        }
                        PackageRemovedInfo childInfo = outInfo.removedChildPackages.get(childPackageName);
                        if (childInfo != null) {
                            childInfo.uid = childPs2.appId;
                        }
                    }
                }
                removePackageDataLIF(ps, allUserHandles, outInfo, flags, writeSettings);
                childCount = ps.childPackageNames == null ? ps.childPackageNames.size() : 0;
                for (i = 0; i < childCount; i++) {
                    synchronized (this.mPackages) {
                        childPs = this.mSettings.peekPackageLPr(ps.childPackageNames.get(i));
                    }
                    if (childPs != null) {
                        PackageRemovedInfo packageRemovedInfo = (outInfo == null || outInfo.removedChildPackages == null) ? null : outInfo.removedChildPackages.get(childPs.name);
                        int deleteFlags = ((flags & 1) == 0 || replacingPackage == null || replacingPackage.hasChildPackage(childPs.name)) ? flags : flags & (-2);
                        removePackageDataLIF(childPs, allUserHandles, packageRemovedInfo, deleteFlags, writeSettings);
                    }
                }
                if (ps.parentPackageName != null && deleteCodeAndResources && outInfo != null) {
                    outInfo.args = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, InstructionSets.getAppDexInstructionSets(ps));
                    if (DEBUG_SD_INSTALL) {
                        Slog.i(TAG, "args=" + outInfo.args);
                        return true;
                    }
                    return true;
                }
                return true;
            }
            if (outInfo != null) {
                if (ps.childPackageNames == null) {
                }
                while (i2 < childCount2) {
                }
            }
            removePackageDataLIF(ps, allUserHandles, outInfo, flags, writeSettings);
            if (ps.childPackageNames == null) {
            }
            while (i < childCount) {
            }
            return ps.parentPackageName != null ? true : true;
        }
    }

    public boolean setBlockUninstallForUser(String packageName, boolean blockUninstall, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Log.i(TAG, "Package doesn't exist in set block uninstall " + packageName);
                return false;
            }
            if (!ps.getInstalled(userId)) {
                Log.i(TAG, "Package not installed in set block uninstall " + packageName);
                return false;
            }
            ps.setBlockUninstall(blockUninstall, userId);
            this.mSettings.writePackageRestrictionsLPr(userId);
            return true;
        }
    }

    public boolean getBlockUninstallForUser(String packageName, int userId) {
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Log.i(TAG, "Package doesn't exist in get block uninstall " + packageName);
                return false;
            }
            return ps.getBlockUninstall(userId);
        }
    }

    public boolean setRequiredForSystemUser(String packageName, boolean systemUserApp) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0) {
            throw new SecurityException("setRequiredForSystemUser can only be run by the system or root");
        }
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Log.w(TAG, "Package doesn't exist: " + packageName);
                return false;
            }
            if (systemUserApp) {
                ps.pkgPrivateFlags |= 1024;
            } else {
                ps.pkgPrivateFlags &= -1025;
            }
            this.mSettings.writeLPr();
            return true;
        }
    }

    private boolean deletePackageLIF(String packageName, UserHandle user, boolean deleteCodeAndResources, int[] allUserHandles, int flags, PackageRemovedInfo outInfo, boolean writeSettings, PackageParser.Package replacingPackage) {
        boolean ret;
        PackageSetting childPs;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        if (DEBUG_REMOVE) {
            Slog.d(TAG, "deletePackageLI: " + packageName + " user " + user);
        }
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                return false;
            }
            if (ps.parentPackageName != null && (!isSystemApp(ps) || (flags & 4) != 0)) {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "Uninstalled child package:" + packageName + " for user:" + (user == null ? -1 : user));
                }
                int removedUserId = user != null ? user.getIdentifier() : -1;
                if (!clearPackageStateForUserLIF(ps, removedUserId, outInfo)) {
                    return false;
                }
                markPackageUninstalledForUserLPw(ps, user);
                scheduleWritePackageRestrictionsLocked(user);
                return true;
            }
            if ((!isSystemApp(ps) || (flags & 4) != 0) && user != null && user.getIdentifier() != -1) {
                markPackageUninstalledForUserLPw(ps, user);
                if (isSystemApp(ps)) {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Deleting system app");
                    }
                    if (!clearPackageStateForUserLIF(ps, user.getIdentifier(), outInfo)) {
                        return false;
                    }
                    scheduleWritePackageRestrictionsLocked(user);
                    return true;
                }
                boolean keepUninstalledPackage = shouldKeepUninstalledPackageLPr(packageName);
                if (ps.isAnyInstalled(sUserManager.getUserIds()) || isVendorApp(ps) || keepUninstalledPackage) {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Still installed by other users");
                    }
                    if (!clearPackageStateForUserLIF(ps, user.getIdentifier(), outInfo)) {
                        return false;
                    }
                    scheduleWritePackageRestrictionsLocked(user);
                    PackageSetting updatedVendorPackage = this.mSettings.getDisabledSystemPkgLPr(packageName);
                    if (updatedVendorPackage == null) {
                        return true;
                    }
                    Slog.d(TAG, "Still need to remove the updated one.");
                } else {
                    if (DEBUG_REMOVE) {
                        Slog.d(TAG, "Not installed by other users, full delete");
                    }
                    ps.setInstalled(true, user.getIdentifier());
                }
            }
            if (ps.childPackageNames != null && outInfo != null) {
                synchronized (this.mPackages) {
                    int childCount = ps.childPackageNames.size();
                    outInfo.removedChildPackages = new ArrayMap<>(childCount);
                    for (int i = 0; i < childCount; i++) {
                        String childPackageName = ps.childPackageNames.get(i);
                        PackageRemovedInfo childInfo = new PackageRemovedInfo();
                        childInfo.removedPackage = childPackageName;
                        outInfo.removedChildPackages.put(childPackageName, childInfo);
                        PackageSetting childPs2 = this.mSettings.peekPackageLPr(childPackageName);
                        if (childPs2 != null) {
                            childInfo.origUsers = childPs2.queryInstalledUsers(allUserHandles, true);
                        }
                    }
                }
            }
            if (isSystemApp(ps) || isVendorApp(ps)) {
                Slog.d(TAG, "Removing " + (isVendorApp(ps) ? "Vendor" : "system") + "package: " + ps.name);
                ret = deleteSystemPackageLIF(ps.pkg, ps, allUserHandles, flags, outInfo, writeSettings);
                if (isVendorApp(ps) && outInfo != null) {
                    int uninstallUser = user != null ? user.getIdentifier() : UserHandle.myUserId();
                    PackageSetting newPs = this.mSettings.mPackages.get(packageName);
                    if (newPs != null) {
                        newPs.setUserState(uninstallUser, 0L, 0, false, true, true, false, false, null, null, null, false, 0, 0);
                    }
                }
            } else {
                if (DEBUG_REMOVE) {
                    Slog.d(TAG, "Removing non-system package: " + ps.name);
                }
                ret = deleteInstalledPackageLIF(ps, deleteCodeAndResources, flags, allUserHandles, outInfo, writeSettings, replacingPackage);
            }
            if (outInfo != null) {
                outInfo.removedForAllUsers = this.mPackages.get(ps.name) == null;
                if (outInfo.removedChildPackages != null) {
                    synchronized (this.mPackages) {
                        int childCount2 = outInfo.removedChildPackages.size();
                        for (int i2 = 0; i2 < childCount2; i2++) {
                            PackageRemovedInfo childInfo2 = outInfo.removedChildPackages.valueAt(i2);
                            if (childInfo2 != null) {
                                childInfo2.removedForAllUsers = this.mPackages.get(childInfo2.removedPackage) == null;
                            }
                        }
                    }
                }
                if (isSystemApp(ps)) {
                    synchronized (this.mPackages) {
                        PackageSetting updatedPs = this.mSettings.peekPackageLPr(ps.name);
                        int childCount3 = updatedPs.childPackageNames != null ? updatedPs.childPackageNames.size() : 0;
                        for (int i3 = 0; i3 < childCount3; i3++) {
                            String childPackageName2 = updatedPs.childPackageNames.get(i3);
                            if ((outInfo.removedChildPackages == null || outInfo.removedChildPackages.indexOfKey(childPackageName2) < 0) && (childPs = this.mSettings.peekPackageLPr(childPackageName2)) != null) {
                                PackageInstalledInfo installRes = new PackageInstalledInfo();
                                installRes.name = childPackageName2;
                                installRes.newUsers = childPs.queryInstalledUsers(allUserHandles, true);
                                installRes.pkg = this.mPackages.get(childPackageName2);
                                installRes.uid = childPs.pkg.applicationInfo.uid;
                                if (outInfo.appearedChildPackages == null) {
                                    outInfo.appearedChildPackages = new ArrayMap<>();
                                }
                                outInfo.appearedChildPackages.put(childPackageName2, installRes);
                            }
                        }
                    }
                }
            }
            return ret;
        }
    }

    private void markPackageUninstalledForUserLPw(PackageSetting ps, UserHandle user) {
        int[] userIds = (user == null || user.getIdentifier() == -1) ? sUserManager.getUserIds() : new int[]{user.getIdentifier()};
        int i = 0;
        int length = userIds.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            int nextUserId = userIds[i2];
            if (DEBUG_REMOVE) {
                Slog.d(TAG, "Marking package:" + ps.name + " uninstalled for user:" + nextUserId);
            }
            ps.setUserState(nextUserId, 0L, 0, false, true, true, false, false, null, null, null, false, ps.readUserState(nextUserId).domainVerificationStatus, 0);
            i = i2 + 1;
        }
    }

    private boolean clearPackageStateForUserLIF(PackageSetting ps, int userId, PackageRemovedInfo outInfo) {
        PackageParser.Package pkg;
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(ps.name);
        }
        int[] userIds = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
        for (int nextUserId : userIds) {
            if (DEBUG_REMOVE) {
                Slog.d(TAG, "Updating package:" + ps.name + " install state for user:" + nextUserId);
            }
            destroyAppDataLIF(pkg, userId, 3);
            destroyAppProfilesLIF(pkg, userId);
            removeKeystoreDataIfNeeded(nextUserId, ps.appId);
            schedulePackageCleaning(ps.name, nextUserId, false);
            synchronized (this.mPackages) {
                if (clearPackagePreferredActivitiesLPw(ps.name, nextUserId)) {
                    scheduleWritePackageRestrictionsLocked(nextUserId);
                }
                resetUserChangesToRuntimePermissionsAndFlagsLPw(ps, nextUserId);
            }
        }
        if (outInfo != null) {
            outInfo.removedPackage = ps.name;
            outInfo.removedAppId = ps.appId;
            outInfo.removedUsers = userIds;
        }
        return true;
    }

    private final class ClearStorageConnection implements ServiceConnection {
        IMediaContainerService mContainerService;

        ClearStorageConnection(PackageManagerService this$0, ClearStorageConnection clearStorageConnection) {
            this();
        }

        private ClearStorageConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                this.mContainerService = IMediaContainerService.Stub.asInterface(service);
                notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private void clearExternalStorageDataSync(String packageName, int userId, boolean allData) {
        boolean mounted;
        int[] users;
        if (DEFAULT_CONTAINER_PACKAGE.equals(packageName)) {
            return;
        }
        if (Environment.isExternalStorageEmulated()) {
            mounted = true;
        } else {
            String status = Environment.getExternalStorageState();
            if (status.equals("mounted")) {
                mounted = true;
            } else {
                mounted = status.equals("mounted_ro");
            }
        }
        if (!mounted) {
            return;
        }
        Intent containerIntent = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        if (userId == -1) {
            users = sUserManager.getUserIds();
        } else {
            users = new int[]{userId};
        }
        ClearStorageConnection conn = new ClearStorageConnection(this, null);
        if (!this.mContext.bindServiceAsUser(containerIntent, conn, 1, UserHandle.SYSTEM)) {
            return;
        }
        try {
            for (int curUser : users) {
                long timeout = SystemClock.uptimeMillis() + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
                synchronized (conn) {
                    while (conn.mContainerService == null) {
                        long now = SystemClock.uptimeMillis();
                        if (now >= timeout) {
                            break;
                        } else {
                            try {
                                conn.wait(timeout - now);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
                if (conn.mContainerService == null) {
                    return;
                }
                Environment.UserEnvironment userEnv = new Environment.UserEnvironment(curUser);
                clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppCacheDirs(packageName));
                if (allData) {
                    clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppDataDirs(packageName));
                    clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppMediaDirs(packageName));
                }
            }
        } finally {
            this.mContext.unbindService(conn);
        }
    }

    public void clearApplicationProfileData(String packageName) throws Throwable {
        PackageParser.Package pkg;
        Throwable th;
        Throwable th2 = null;
        enforceSystemOrRoot("Only the system can clear all profile data");
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
        }
        AutoCloseable autoCloseable = null;
        try {
            PackageFreezer freezer = freezePackage(packageName, "clearApplicationProfileData");
            synchronized (this.mInstallLock) {
                clearAppProfilesLIF(pkg, -1);
                destroyAppReferenceProfileLeafLIF(pkg, -1, true);
            }
            if (freezer != null) {
                try {
                    freezer.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
        } catch (Throwable th4) {
            try {
                throw th4;
            } catch (Throwable th5) {
                th2 = th4;
                th = th5;
                if (0 != 0) {
                    try {
                        autoCloseable.close();
                    } catch (Throwable th6) {
                        if (th2 == null) {
                            th2 = th6;
                        } else if (th2 != th6) {
                            th2.addSuppressed(th6);
                        }
                    }
                }
                if (th2 != null) {
                    throw th;
                }
                throw th2;
            }
        }
    }

    public void clearApplicationUserData(final String packageName, final IPackageDataObserver observer, final int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_USER_DATA", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "clear application data");
        if (this.mProtectedPackages.canPackageBeWiped(userId, packageName)) {
            throw new SecurityException("Cannot clear data for a device owner or a profile owner");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() throws Throwable {
                Throwable th;
                boolean succeeded;
                DeviceStorageMonitorInternal dsm;
                Throwable th2 = null;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                AutoCloseable autoCloseable = null;
                try {
                    PackageFreezer freezer = PackageManagerService.this.freezePackage(packageName, "clearApplicationUserData");
                    synchronized (PackageManagerService.this.mInstallLock) {
                        succeeded = PackageManagerService.this.clearApplicationUserDataLIF(packageName, userId);
                    }
                    PackageManagerService.this.clearExternalStorageDataSync(packageName, userId, true);
                    if (freezer != null) {
                        try {
                            freezer.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                    if (succeeded && (dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class)) != null) {
                        dsm.checkMemory();
                    }
                    if (observer == null) {
                        return;
                    }
                    try {
                        observer.onRemoveCompleted(packageName, succeeded);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                } catch (Throwable th4) {
                    try {
                        throw th4;
                    } catch (Throwable th5) {
                        th2 = th4;
                        th = th5;
                        if (0 != 0) {
                            try {
                                autoCloseable.close();
                            } catch (Throwable th6) {
                                if (th2 == null) {
                                    th2 = th6;
                                } else if (th2 != th6) {
                                    th2.addSuppressed(th6);
                                }
                            }
                        }
                        if (th2 != null) {
                            throw th;
                        }
                        throw th2;
                    }
                }
            }
        });
    }

    private boolean clearApplicationUserDataLIF(String packageName, int userId) {
        int flags;
        PackageSetting ps;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null && (ps = this.mSettings.mPackages.get(packageName)) != null) {
                pkg = ps.pkg;
            }
            if (pkg == null) {
                Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                return false;
            }
            resetUserChangesToRuntimePermissionsAndFlagsLPw((PackageSetting) pkg.mExtras, userId);
            clearAppDataLIF(pkg, userId, 3);
            int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
            removeKeystoreDataIfNeeded(userId, appId);
            UserManagerInternal umInternal = getUserManagerInternal();
            if (umInternal.isUserUnlockingOrUnlocked(userId)) {
                flags = 3;
            } else if (umInternal.isUserRunning(userId)) {
                flags = 1;
            } else {
                flags = 0;
            }
            prepareAppDataContentsLIF(pkg, userId, flags);
            return true;
        }
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(int userId) {
        int packageCount = this.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageParser.Package pkg = this.mPackages.valueAt(i);
            PackageSetting ps = (PackageSetting) pkg.mExtras;
            resetUserChangesToRuntimePermissionsAndFlagsLPw(ps, userId);
        }
    }

    private void resetNetworkPolicies(int userId) {
        ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).resetUserState(userId);
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(PackageSetting ps, final int userId) {
        if (ps.pkg == null) {
            return;
        }
        boolean writeInstallPermissions = false;
        boolean writeRuntimePermissions = false;
        int permissionCount = ps.pkg.requestedPermissions.size();
        boolean pkgReviewRequired = isPackageNeedsReview(ps.pkg);
        for (int i = 0; i < permissionCount; i++) {
            String permission = (String) ps.pkg.requestedPermissions.get(i);
            BasePermission bp = this.mSettings.mPermissions.get(permission);
            if (bp != null) {
                if (ps.sharedUser != null) {
                    boolean used = false;
                    int packageCount = ps.sharedUser.packages.size();
                    int j = 0;
                    while (true) {
                        if (j < packageCount) {
                            PackageSetting pkg = ps.sharedUser.packages.valueAt(j);
                            if (pkg.pkg == null || pkg.pkg.packageName.equals(ps.pkg.packageName) || !pkg.pkg.requestedPermissions.contains(permission)) {
                                j++;
                            } else {
                                used = true;
                            }
                        }
                    }
                    if (!used) {
                        PermissionsState permissionsState = ps.getPermissionsState();
                        int oldFlags = permissionsState.getPermissionFlags(bp.name, userId);
                        boolean hasInstallState = permissionsState.getInstallPermissionState(bp.name) != null;
                        int flags = 0;
                        if (!CtaUtils.isCtaSupported()) {
                            if (Build.isPermissionReviewRequired() && ps.pkg.applicationInfo.targetSdkVersion < 23) {
                                flags = 64;
                            }
                        } else if (pkgReviewRequired && bp.isRuntime() && CtaUtils.isPlatformPermission(bp.sourcePackage, bp.name) && (oldFlags & 16) == 0) {
                            flags = 64;
                        }
                        if (permissionsState.updatePermissionFlags(bp, userId, 75, flags)) {
                            if (hasInstallState) {
                                writeInstallPermissions = true;
                            } else {
                                writeRuntimePermissions = true;
                            }
                        }
                        if (bp.isRuntime() && (oldFlags & 20) == 0) {
                            if ((oldFlags & 32) != 0) {
                                if (permissionsState.grantRuntimePermission(bp, userId) != -1) {
                                    writeRuntimePermissions = true;
                                }
                            } else if ((flags & 64) == 0) {
                                int revokeResult = permissionsState.revokeRuntimePermission(bp, userId);
                                switch (revokeResult) {
                                    case 0:
                                    case 1:
                                        writeRuntimePermissions = true;
                                        final int appId = ps.appId;
                                        this.mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                PackageManagerService.this.killUid(appId, userId, PackageManagerService.KILL_APP_REASON_PERMISSIONS_REVOKED);
                                            }
                                        });
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (writeRuntimePermissions) {
            this.mSettings.writeRuntimePermissionsForUserLPr(userId, true);
        }
        if (!writeInstallPermissions) {
            return;
        }
        this.mSettings.writeLPr();
    }

    private static void removeKeystoreDataIfNeeded(int userId, int appId) {
        if (appId < 0) {
            return;
        }
        KeyStore keyStore = KeyStore.getInstance();
        if (keyStore != null) {
            if (userId == -1) {
                for (int individual : sUserManager.getUserIds()) {
                    keyStore.clearUid(UserHandle.getUid(individual, appId));
                }
                return;
            }
            keyStore.clearUid(UserHandle.getUid(userId, appId));
            return;
        }
        Slog.w(TAG, "Could not contact keystore to clear entries for app id " + appId);
    }

    public void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer) {
        int userId = UserHandle.getCallingUserId();
        deleteApplicationCacheFilesAsUser(packageName, userId, observer);
    }

    public void deleteApplicationCacheFilesAsUser(final String packageName, final int userId, final IPackageDataObserver observer) {
        final PackageParser.Package pkg;
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_CACHE_FILES", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "delete application cache files");
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (PackageManagerService.this.mInstallLock) {
                    PackageManagerService.this.clearAppDataLIF(pkg, userId, 259);
                    PackageManagerService.this.clearAppDataLIF(pkg, userId, 515);
                }
                PackageManagerService.this.clearExternalStorageDataSync(packageName, userId, false);
                if (observer == null) {
                    return;
                }
                try {
                    observer.onRemoveCompleted(packageName, true);
                } catch (RemoteException e) {
                    Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                }
            }
        });
    }

    public void getPackageSizeInfo(String packageName, int userHandle, IPackageStatsObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GET_PACKAGE_SIZE", null);
        if (packageName == null) {
            throw new IllegalArgumentException("Attempt to get size of null packageName");
        }
        PackageStats stats = new PackageStats(packageName, userHandle);
        Message msg = this.mHandler.obtainMessage(5);
        msg.obj = new MeasureParams(stats, observer);
        this.mHandler.sendMessage(msg);
    }

    private boolean getPackageSizeInfoLI(String packageName, int userId, PackageStats stats) {
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Slog.w(TAG, "Failed to find settings for " + packageName);
                return false;
            }
            try {
                this.mInstaller.getAppSize(ps.volumeUuid, packageName, userId, 3, ps.getCeDataInode(userId), ps.codePathString, stats);
                if (isSystemApp(ps) && !isUpdatedSystemApp(ps)) {
                    stats.codeSize = 0L;
                    return true;
                }
                return true;
            } catch (InstallerConnection.InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
                return false;
            }
        }
    }

    private int getUidTargetSdkVersionLockedLPr(int uid) {
        int v;
        Object obj = this.mSettings.getUserIdLPr(uid);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            int vers = 10000;
            for (PackageSetting ps : sus.packages) {
                if (ps.pkg != null && (v = ps.pkg.applicationInfo.targetSdkVersion) < vers) {
                    vers = v;
                }
            }
            return vers;
        }
        if (obj instanceof PackageSetting) {
            PackageSetting ps2 = (PackageSetting) obj;
            if (ps2.pkg != null) {
                return ps2.pkg.applicationInfo.targetSdkVersion;
            }
            return 10000;
        }
        return 10000;
    }

    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        addPreferredActivityInternal(filter, match, set, activity, true, userId, "Adding preferred");
    }

    private void addPreferredActivityInternal(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always, int userId, String opname) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "add preferred activity");
        if (filter.countActions() == 0) {
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
            PreferredIntentResolver pir = this.mSettings.editPreferredActivitiesLPw(userId);
            Slog.i(TAG, opname + " activity " + activity.flattenToShortString() + " for user " + userId + ":");
            filter.dump(new LogPrinter(4, TAG), "  ");
            pir.addFilter(new PreferredActivity(filter, match, set, activity, always));
            scheduleWritePackageRestrictionsLocked(userId);
        }
    }

    public void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        if (filter.countActions() != 1) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have only 1 action.");
        }
        if (filter.countDataAuthorities() != 0 || filter.countDataPaths() != 0 || filter.countDataSchemes() > 1 || filter.countDataTypes() != 0) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have no data authorities, paths, or types; and at most one scheme.");
        }
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "replace preferred activity");
        synchronized (this.mPackages) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                    Slog.w(TAG, "Ignoring replacePreferredActivity() from uid " + Binder.getCallingUid());
                    return;
                }
                this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            }
            PreferredIntentResolver pir = this.mSettings.mPreferredActivities.get(userId);
            if (pir != null) {
                ArrayList<PreferredActivity> existing = pir.findFilters(filter);
                if (existing != null && existing.size() == 1) {
                    PreferredActivity cur = existing.get(0);
                    if (DEBUG_PREFERRED) {
                        Slog.i(TAG, "Checking replace of preferred:");
                        filter.dump(new LogPrinter(4, TAG), "  ");
                        if (cur.mPref.mAlways) {
                            Slog.i(TAG, "  -- CUR: mMatch=" + cur.mPref.mMatch);
                            Slog.i(TAG, "  -- CUR: mSet=" + Arrays.toString(cur.mPref.mSetComponents));
                            Slog.i(TAG, "  -- CUR: mComponent=" + cur.mPref.mShortComponent);
                            Slog.i(TAG, "  -- NEW: mMatch=" + (268369920 & match));
                            Slog.i(TAG, "  -- CUR: mSet=" + Arrays.toString(set));
                            Slog.i(TAG, "  -- CUR: mComponent=" + activity.flattenToShortString());
                        } else {
                            Slog.i(TAG, "  -- CUR; not mAlways!");
                        }
                    }
                    if (cur.mPref.mAlways && cur.mPref.mComponent.equals(activity) && cur.mPref.mMatch == (268369920 & match) && cur.mPref.sameSet(set)) {
                        if (DEBUG_PREFERRED) {
                            Slog.i(TAG, "Replacing with same preferred activity " + cur.mPref.mShortComponent + " for user " + userId + ":");
                            filter.dump(new LogPrinter(4, TAG), "  ");
                        }
                        return;
                    }
                }
                if (existing != null) {
                    if (DEBUG_PREFERRED) {
                        Slog.i(TAG, existing.size() + " existing preferred matches for:");
                        filter.dump(new LogPrinter(4, TAG), "  ");
                    }
                    for (int i = 0; i < existing.size(); i++) {
                        PreferredActivity pa = existing.get(i);
                        if (DEBUG_PREFERRED) {
                            Slog.i(TAG, "Removing existing preferred activity " + pa.mPref.mComponent + ":");
                            pa.dump(new LogPrinter(4, TAG), "  ");
                        }
                        pir.removeFilter(pa);
                    }
                }
            }
            addPreferredActivityInternal(filter, match, set, activity, true, userId, "Replacing preferred");
        }
    }

    public void clearPackagePreferredActivities(String packageName) {
        int uid = Binder.getCallingUid();
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if ((pkg == null || pkg.applicationInfo.uid != uid) && this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                if (getUidTargetSdkVersionLockedLPr(Binder.getCallingUid()) < 8) {
                    Slog.w(TAG, "Ignoring clearPackagePreferredActivities() from uid " + Binder.getCallingUid());
                    return;
                }
                this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            }
            int user = UserHandle.getCallingUserId();
            if (clearPackagePreferredActivitiesLPw(packageName, user)) {
                scheduleWritePackageRestrictionsLocked(user);
            }
        }
    }

    boolean clearPackagePreferredActivitiesLPw(String packageName, int userId) {
        ArrayList<PreferredActivity> removed = null;
        boolean changed = false;
        for (int i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
            int thisUserId = this.mSettings.mPreferredActivities.keyAt(i);
            PreferredIntentResolver pir = this.mSettings.mPreferredActivities.valueAt(i);
            if (userId == -1 || userId == thisUserId) {
                Iterator<PreferredActivity> it = pir.filterIterator();
                while (it.hasNext()) {
                    PreferredActivity pa = it.next();
                    if (packageName == null || (pa.mPref.mComponent.getPackageName().equals(packageName) && pa.mPref.mAlways)) {
                        if (removed == null) {
                            removed = new ArrayList<>();
                        }
                        removed.add(pa);
                    }
                }
                if (removed != null) {
                    for (int j = 0; j < removed.size(); j++) {
                        pir.removeFilter(removed.get(j));
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void clearIntentFilterVerificationsLPw(int userId) {
        int packageCount = this.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageParser.Package pkg = this.mPackages.valueAt(i);
            clearIntentFilterVerificationsLPw(pkg.packageName, userId);
        }
    }

    void clearIntentFilterVerificationsLPw(String packageName, int userId) {
        if (userId == -1) {
            if (!this.mSettings.removeIntentFilterVerificationLPw(packageName, sUserManager.getUserIds())) {
                return;
            }
            for (int oneUserId : sUserManager.getUserIds()) {
                scheduleWritePackageRestrictionsLocked(oneUserId);
            }
            return;
        }
        if (!this.mSettings.removeIntentFilterVerificationLPw(packageName, userId)) {
            return;
        }
        scheduleWritePackageRestrictionsLocked(userId);
    }

    void clearDefaultBrowserIfNeeded(String packageName) {
        for (int oneUserId : sUserManager.getUserIds()) {
            String defaultBrowserPackageName = getDefaultBrowserPackageName(oneUserId);
            if (!TextUtils.isEmpty(defaultBrowserPackageName) && packageName.equals(defaultBrowserPackageName)) {
                setDefaultBrowserPackageName(null, oneUserId);
            }
        }
    }

    public void resetApplicationPreferences(int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                clearPackagePreferredActivitiesLPw(null, userId);
                this.mSettings.applyDefaultPreferredAppsLPw(this, userId);
                applyFactoryDefaultBrowserLPw(userId);
                clearIntentFilterVerificationsLPw(userId);
                primeDomainVerificationsLPw(userId);
                resetUserChangesToRuntimePermissionsAndFlagsLPw(userId);
                scheduleWritePackageRestrictionsLocked(userId);
            }
            resetNetworkPolicies(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mPackages) {
            PreferredIntentResolver pir = this.mSettings.mPreferredActivities.get(userId);
            if (pir != null) {
                Iterator<PreferredActivity> it = pir.filterIterator();
                while (it.hasNext()) {
                    PreferredActivity pa = it.next();
                    if (packageName == null || (pa.mPref.mComponent.getPackageName().equals(packageName) && pa.mPref.mAlways)) {
                        if (outFilters != null) {
                            outFilters.add(new IntentFilter(pa));
                        }
                        if (outActivities != null) {
                            outActivities.add(pa.mPref.mComponent);
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("addPersistentPreferredActivity can only be run by the system");
        }
        if (filter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a preferred activity with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            Slog.i(TAG, "Adding persistent preferred activity " + activity + " for user " + userId + ":");
            filter.dump(new LogPrinter(4, TAG), "  ");
            this.mSettings.editPersistentPreferredActivitiesLPw(userId).addFilter(new PersistentPreferredActivity(filter, activity));
            scheduleWritePackageRestrictionsLocked(userId);
        }
    }

    public void clearPackagePersistentPreferredActivities(String packageName, int userId) throws Throwable {
        ArrayList<PersistentPreferredActivity> removed;
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("clearPackagePersistentPreferredActivities can only be run by the system");
        }
        ArrayList<PersistentPreferredActivity> removed2 = null;
        boolean changed = false;
        synchronized (this.mPackages) {
            for (int i = 0; i < this.mSettings.mPersistentPreferredActivities.size(); i++) {
                try {
                    int thisUserId = this.mSettings.mPersistentPreferredActivities.keyAt(i);
                    PersistentPreferredIntentResolver ppir = this.mSettings.mPersistentPreferredActivities.valueAt(i);
                    if (userId == thisUserId) {
                        Iterator<PersistentPreferredActivity> it = ppir.filterIterator();
                        while (true) {
                            try {
                                removed = removed2;
                                if (!it.hasNext()) {
                                    break;
                                }
                                PersistentPreferredActivity ppa = it.next();
                                if (ppa.mComponent.getPackageName().equals(packageName)) {
                                    removed2 = removed == null ? new ArrayList<>() : removed;
                                    removed2.add(ppa);
                                } else {
                                    removed2 = removed;
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        if (removed != null) {
                            for (int j = 0; j < removed.size(); j++) {
                                ppir.removeFilter(removed.get(j));
                            }
                            changed = true;
                            removed2 = removed;
                        } else {
                            removed2 = removed;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (changed) {
                scheduleWritePackageRestrictionsLocked(userId);
            }
        }
    }

    private void restoreFromXml(XmlPullParser parser, int userId, String expectedStartTag, BlobXmlRestorer functor) throws XmlPullParserException, IOException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Didn't find start tag during restore");
                return;
            }
            return;
        }
        Slog.v(TAG, ":: restoreFromXml() : got to tag " + parser.getName());
        if (!expectedStartTag.equals(parser.getName())) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Found unexpected tag " + parser.getName());
            }
        } else {
            while (parser.next() == 4) {
            }
            Slog.v(TAG, ":: stepped forward, applying functor at tag " + parser.getName());
            functor.apply(parser, userId);
        }
    }

    public byte[] getPreferredActivityBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPreferredActivityBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_PREFERRED_BACKUP);
            synchronized (this.mPackages) {
                this.mSettings.writePreferredActivitiesLPr(serializer, userId, true);
            }
            serializer.endTag(null, TAG_PREFERRED_BACKUP);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write preferred activities for backup", e);
            }
            return null;
        }
    }

    public void restorePreferredActivities(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_PREFERRED_BACKUP, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser parser2, int userId2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readPreferredActivitiesLPw(parser2, userId2);
                    }
                }
            });
        } catch (Exception e) {
            if (!DEBUG_BACKUP) {
                return;
            }
            Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
        }
    }

    public byte[] getDefaultAppsBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getDefaultAppsBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_DEFAULT_APPS);
            synchronized (this.mPackages) {
                this.mSettings.writeDefaultAppsLPr(serializer, userId);
            }
            serializer.endTag(null, TAG_DEFAULT_APPS);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restoreDefaultApps(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restoreDefaultApps()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_DEFAULT_APPS, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser parser2, int userId2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readDefaultAppsLPw(parser2, userId2);
                    }
                }
            });
        } catch (Exception e) {
            if (!DEBUG_BACKUP) {
                return;
            }
            Slog.e(TAG, "Exception restoring default apps: " + e.getMessage());
        }
    }

    public byte[] getIntentFilterVerificationBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getIntentFilterVerificationBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_INTENT_FILTER_VERIFICATION);
            synchronized (this.mPackages) {
                this.mSettings.writeAllDomainVerificationsLPr(serializer, userId);
            }
            serializer.endTag(null, TAG_INTENT_FILTER_VERIFICATION);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restoreIntentFilterVerification(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_INTENT_FILTER_VERIFICATION, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser parser2, int userId2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readAllDomainVerificationsLPr(parser2, userId2);
                        PackageManagerService.this.mSettings.writeLPr();
                    }
                }
            });
        } catch (Exception e) {
            if (!DEBUG_BACKUP) {
                return;
            }
            Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
        }
    }

    public byte[] getPermissionGrantBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPermissionGrantBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_PERMISSION_BACKUP);
            synchronized (this.mPackages) {
                serializeRuntimePermissionGrantsLPr(fastXmlSerializer, userId);
            }
            fastXmlSerializer.endTag(null, TAG_PERMISSION_BACKUP);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            if (DEBUG_BACKUP) {
                Slog.e(TAG, "Unable to write default apps for backup", e);
            }
            return null;
        }
    }

    public void restorePermissionGrants(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePermissionGrants()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_PERMISSION_BACKUP, new BlobXmlRestorer() {
                @Override
                public void apply(XmlPullParser parser2, int userId2) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.processRestoredPermissionGrantsLPr(parser2, userId2);
                    }
                }
            });
        } catch (Exception e) {
            if (!DEBUG_BACKUP) {
                return;
            }
            Slog.e(TAG, "Exception restoring preferred activities: " + e.getMessage());
        }
    }

    private void serializeRuntimePermissionGrantsLPr(XmlSerializer serializer, int userId) throws IOException {
        boolean isGranted;
        serializer.startTag(null, TAG_ALL_GRANTS);
        int N = this.mSettings.mPackages.size();
        for (int i = 0; i < N; i++) {
            PackageSetting ps = this.mSettings.mPackages.valueAt(i);
            boolean pkgGrantsKnown = false;
            PermissionsState packagePerms = ps.getPermissionsState();
            for (PermissionsState.PermissionState state : packagePerms.getRuntimePermissionStates(userId)) {
                int grantFlags = state.getFlags();
                if ((grantFlags & 52) == 0 && ((isGranted = state.isGranted()) || (grantFlags & 11) != 0)) {
                    String packageName = this.mSettings.mPackages.keyAt(i);
                    if (!pkgGrantsKnown) {
                        serializer.startTag(null, TAG_GRANT);
                        serializer.attribute(null, ATTR_PACKAGE_NAME, packageName);
                        pkgGrantsKnown = true;
                    }
                    boolean userSet = (grantFlags & 1) != 0;
                    boolean userFixed = (grantFlags & 2) != 0;
                    boolean revoke = (grantFlags & 8) != 0;
                    serializer.startTag(null, TAG_PERMISSION);
                    serializer.attribute(null, ATTR_PERMISSION_NAME, state.getName());
                    if (isGranted) {
                        serializer.attribute(null, ATTR_IS_GRANTED, "true");
                    }
                    if (userSet) {
                        serializer.attribute(null, ATTR_USER_SET, "true");
                    }
                    if (userFixed) {
                        serializer.attribute(null, ATTR_USER_FIXED, "true");
                    }
                    if (revoke) {
                        serializer.attribute(null, ATTR_REVOKE_ON_UPGRADE, "true");
                    }
                    serializer.endTag(null, TAG_PERMISSION);
                }
            }
            if (pkgGrantsKnown) {
                serializer.endTag(null, TAG_GRANT);
            }
        }
        serializer.endTag(null, TAG_ALL_GRANTS);
    }

    private void processRestoredPermissionGrantsLPr(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        String pkgName = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_GRANT)) {
                    pkgName = parser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                    if (DEBUG_BACKUP) {
                        Slog.v(TAG, "+++ Restoring grants for package " + pkgName);
                    }
                } else if (tagName.equals(TAG_PERMISSION)) {
                    boolean isGranted = "true".equals(parser.getAttributeValue(null, ATTR_IS_GRANTED));
                    String permName = parser.getAttributeValue(null, ATTR_PERMISSION_NAME);
                    int newFlagSet = "true".equals(parser.getAttributeValue(null, ATTR_USER_SET)) ? 1 : 0;
                    if ("true".equals(parser.getAttributeValue(null, ATTR_USER_FIXED))) {
                        newFlagSet |= 2;
                    }
                    if ("true".equals(parser.getAttributeValue(null, ATTR_REVOKE_ON_UPGRADE))) {
                        newFlagSet |= 8;
                    }
                    if (DEBUG_BACKUP) {
                        Slog.v(TAG, "  + Restoring grant: pkg=" + pkgName + " perm=" + permName + " granted=" + isGranted + " bits=0x" + Integer.toHexString(newFlagSet));
                    }
                    PackageSetting ps = this.mSettings.mPackages.get(pkgName);
                    if (ps != null) {
                        if (DEBUG_BACKUP) {
                            Slog.v(TAG, "        + already installed; applying");
                        }
                        PermissionsState perms = ps.getPermissionsState();
                        BasePermission bp = this.mSettings.mPermissions.get(permName);
                        if (bp != null) {
                            if (isGranted) {
                                perms.grantRuntimePermission(bp, userId);
                            }
                            if (newFlagSet != 0) {
                                perms.updatePermissionFlags(bp, userId, 11, newFlagSet);
                            }
                        }
                    } else {
                        if (DEBUG_BACKUP) {
                            Slog.v(TAG, "        - not yet installed; saving for later");
                        }
                        this.mSettings.processRestoredPermissionGrantLPr(pkgName, permName, isGranted, newFlagSet, userId);
                    }
                } else {
                    reportSettingsProblem(5, "Unknown element under <perm-grant-backup>: " + tagName);
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        scheduleWriteSettingsLocked();
        this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
    }

    public void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage, int sourceUserId, int targetUserId, int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a crossProfile intent filter with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            CrossProfileIntentFilter newFilter = new CrossProfileIntentFilter(intentFilter, ownerPackage, targetUserId, flags);
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArrayList<CrossProfileIntentFilter> existing = resolver.findFilters(intentFilter);
            if (existing != null) {
                int size = existing.size();
                for (int i = 0; i < size; i++) {
                    if (newFilter.equalsIgnoreFilter(existing.get(i))) {
                        return;
                    }
                }
            }
            resolver.addFilter(newFilter);
            scheduleWritePackageRestrictionsLocked(sourceUserId);
        }
    }

    public void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        synchronized (this.mPackages) {
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArraySet<CrossProfileIntentFilter> set = new ArraySet<>(resolver.filterSet());
            for (CrossProfileIntentFilter filter : set) {
                if (filter.getOwnerPackage().equals(ownerPackage)) {
                    resolver.removeFilter(filter);
                }
            }
            scheduleWritePackageRestrictionsLocked(sourceUserId);
        }
    }

    private void enforceOwnerRights(String pkg, int callingUid) {
        if (UserHandle.getAppId(callingUid) == 1000) {
            return;
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        PackageInfo pi = getPackageInfo(pkg, 0, callingUserId);
        if (pi == null) {
            throw new IllegalArgumentException("Unknown package " + pkg + " on user " + callingUserId);
        }
        if (UserHandle.isSameApp(pi.applicationInfo.uid, callingUid)) {
        } else {
            throw new SecurityException("Calling uid " + callingUid + " does not own package " + pkg);
        }
    }

    public ComponentName getHomeActivities(List<ResolveInfo> allHomeCandidates) {
        return getHomeActivitiesAsUser(allHomeCandidates, UserHandle.getCallingUserId());
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        return intent;
    }

    private IntentFilter getHomeFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
        Intent intent = getHomeIntent();
        List<ResolveInfo> list = queryIntentActivitiesInternal(intent, null, 128, userId);
        ResolveInfo preferred = findPreferredActivity(intent, null, 0, list, 0, true, false, false, userId);
        allHomeCandidates.clear();
        if (list != null) {
            for (ResolveInfo ri : list) {
                allHomeCandidates.add(ri);
            }
        }
        if (preferred == null || preferred.activityInfo == null) {
            return null;
        }
        return new ComponentName(preferred.activityInfo.packageName, preferred.activityInfo.name);
    }

    public void setHomeActivity(ComponentName comp, int userId) {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        getHomeActivitiesAsUser(homeActivities, userId);
        boolean found = false;
        int size = homeActivities.size();
        ComponentName[] set = new ComponentName[size];
        for (int i = 0; i < size; i++) {
            ResolveInfo candidate = homeActivities.get(i);
            ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            set[i] = activityName;
            if (!found && activityName.equals(comp)) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Component " + comp + " cannot be home on user " + userId);
        }
        replacePreferredActivity(getHomeFilter(), DumpState.DUMP_DEXOPT, set, comp, userId);
    }

    private String getSetupWizardPackageName() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.SETUP_WIZARD");
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, null, 1835520, UserHandle.myUserId());
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one setup wizard; found " + matches.size() + ": matches=" + matches);
        return null;
    }

    public void setApplicationEnabledSetting(String appPackageName, int newState, int flags, int userId, String callingPackage) {
        if (sUserManager.exists(userId)) {
            if (callingPackage == null) {
                callingPackage = Integer.toString(Binder.getCallingUid());
            }
            setEnabledSetting(appPackageName, null, newState, flags, userId, callingPackage);
        }
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
        if (sUserManager.exists(userId)) {
            setEnabledSetting(componentName.getPackageName(), componentName.getClassName(), newState, flags, userId, null);
        }
    }

    private void setEnabledSetting(String packageName, String className, int newState, int flags, int userId, String callingPackage) {
        PackageSetting pkgSetting;
        if (newState != 0 && newState != 1 && newState != 2 && newState != 3 && newState != 4) {
            throw new IllegalArgumentException("Invalid new component state: " + newState);
        }
        int uid = Binder.getCallingUid();
        int permission = uid == 1000 ? 0 : this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
        enforceCrossUserPermission(uid, userId, false, true, "set enabled");
        boolean allowedByPermission = permission == 0;
        boolean sendNow = false;
        boolean isApp = className == null;
        String componentName = isApp ? packageName : className;
        synchronized (this.mPackages) {
            pkgSetting = this.mSettings.mPackages.get(packageName);
            if (pkgSetting == null) {
                if (className != null) {
                    throw new IllegalArgumentException("Unknown component: " + packageName + "/" + className);
                }
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        if (!UserHandle.isSameApp(uid, pkgSetting.appId)) {
            if (!allowedByPermission) {
                throw new SecurityException("Permission Denial: attempt to change component state from pid=" + Binder.getCallingPid() + ", uid=" + uid + ", package uid=" + pkgSetting.appId);
            }
            if (this.mProtectedPackages.canPackageStateBeChanged(userId, packageName)) {
                throw new SecurityException("Cannot disable a device owner or a profile owner");
            }
        }
        synchronized (this.mPackages) {
            if (uid == SHELL_UID) {
                int oldState = pkgSetting.getEnabled(userId);
                if (className != null || ((oldState != 3 && oldState != 0 && oldState != 1) || (newState != 3 && newState != 0 && newState != 1))) {
                    throw new SecurityException("Shell cannot change component state for " + packageName + "/" + className + " to " + newState);
                }
            }
            if (className != null) {
                PackageParser.Package pkg = pkgSetting.pkg;
                if (pkg == null || !pkg.hasComponentClassName(className)) {
                    if (pkg == null || pkg.applicationInfo == null) {
                        throw new IllegalArgumentException("Unknown component: " + packageName + "/" + className);
                    }
                    if (pkg != null && pkg.applicationInfo.targetSdkVersion >= 16) {
                        throw new IllegalArgumentException("Component class " + className + " does not exist in " + packageName);
                    }
                    Slog.w(TAG, "Failed setComponentEnabledSetting: component class " + className + " does not exist in " + packageName);
                }
                switch (newState) {
                    case 0:
                        if (!pkgSetting.restoreComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    case 1:
                        if (!pkgSetting.enableComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    case 2:
                        if (!pkgSetting.disableComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    default:
                        Slog.e(TAG, "Invalid new component state: " + newState);
                        return;
                }
            } else {
                if (pkgSetting.getEnabled(userId) == newState) {
                    return;
                }
                if (newState == 0 || newState == 1) {
                    callingPackage = null;
                }
                pkgSetting.setEnabled(newState, userId, callingPackage);
            }
            scheduleWritePackageRestrictionsLocked(userId);
            ArrayList<String> components = this.mPendingBroadcasts.get(userId, packageName);
            boolean newPackage = components == null;
            if (newPackage) {
                components = new ArrayList<>();
            }
            if (!components.contains(componentName)) {
                components.add(componentName);
            }
            if ((flags & 1) == 0) {
                sendNow = true;
                this.mPendingBroadcasts.remove(userId, packageName);
            } else {
                if (newPackage) {
                    this.mPendingBroadcasts.put(userId, packageName, components);
                }
                if (!this.mHandler.hasMessages(1)) {
                    this.mHandler.sendEmptyMessageDelayed(1, 10000L);
                }
            }
            long callingId = Binder.clearCallingIdentity();
            if (sendNow) {
                try {
                    int packageUid = UserHandle.getUid(userId, pkgSetting.appId);
                    sendPackageChangedBroadcast(packageName, (flags & 1) != 0, components, packageUid);
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
    }

    public void flushPackageRestrictionsAsUser(int userId) {
        if (!sUserManager.exists(userId)) {
            return;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "flushPackageRestrictions");
        synchronized (this.mPackages) {
            this.mSettings.writePackageRestrictionsLPr(userId);
            this.mDirtyUsers.remove(Integer.valueOf(userId));
            if (this.mDirtyUsers.isEmpty()) {
                this.mHandler.removeMessages(14);
            }
        }
    }

    private void sendPackageChangedBroadcast(String packageName, boolean killFlag, ArrayList<String> componentNames, int packageUid) {
        if (DEBUG_INSTALL) {
            Log.v(TAG, "Sending package changed: package=" + packageName + " components=" + componentNames);
        }
        Bundle extras = new Bundle(4);
        extras.putString("android.intent.extra.changed_component_name", componentNames.get(0));
        String[] nameList = new String[componentNames.size()];
        componentNames.toArray(nameList);
        extras.putStringArray("android.intent.extra.changed_component_name_list", nameList);
        extras.putBoolean("android.intent.extra.DONT_KILL_APP", killFlag);
        extras.putInt("android.intent.extra.UID", packageUid);
        int flags = !componentNames.contains(packageName) ? 1073741824 : 0;
        sendPackageBroadcast("android.intent.action.PACKAGE_CHANGED", packageName, extras, flags, null, null, new int[]{UserHandle.getUserId(packageUid)});
    }

    public void setPackageStoppedState(String packageName, boolean stopped, int userId) {
        if (sUserManager.exists(userId)) {
            int uid = Binder.getCallingUid();
            int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
            boolean allowedByPermission = permission == 0;
            enforceCrossUserPermission(uid, userId, true, true, "stop package");
            synchronized (this.mPackages) {
                if (this.mSettings.setPackageStoppedStateLPw(this, packageName, stopped, allowedByPermission, uid, userId)) {
                    scheduleWritePackageRestrictionsLocked(userId);
                }
            }
        }
    }

    public String getInstallerPackageName(String packageName) {
        String installerPackageNameLPr;
        synchronized (this.mPackages) {
            installerPackageNameLPr = this.mSettings.getInstallerPackageNameLPr(packageName);
        }
        return installerPackageNameLPr;
    }

    public boolean isOrphaned(String packageName) {
        boolean zIsOrphaned;
        synchronized (this.mPackages) {
            zIsOrphaned = this.mSettings.isOrphaned(packageName);
        }
        return zIsOrphaned;
    }

    public int getApplicationEnabledSetting(String packageName, int userId) {
        int applicationEnabledSettingLPr;
        if (!sUserManager.exists(userId)) {
            return 2;
        }
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, false, false, "get enabled");
        synchronized (this.mPackages) {
            applicationEnabledSettingLPr = this.mSettings.getApplicationEnabledSettingLPr(packageName, userId);
        }
        return applicationEnabledSettingLPr;
    }

    public int getComponentEnabledSetting(ComponentName componentName, int userId) {
        int componentEnabledSettingLPr;
        if (!sUserManager.exists(userId)) {
            return 2;
        }
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, false, false, "get component enabled");
        synchronized (this.mPackages) {
            componentEnabledSettingLPr = this.mSettings.getComponentEnabledSettingLPr(componentName, userId);
        }
        return componentEnabledSettingLPr;
    }

    public void enterSafeMode() {
        enforceSystemOrRoot("Only the system can request entering safe mode");
        if (this.mSystemReady) {
            return;
        }
        this.mSafeMode = true;
    }

    public void systemReady() {
        this.mSystemReady = true;
        boolean compatibilityModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "compatibility_mode", 1) == 1;
        PackageParser.setCompatibilityModeEnabled(compatibilityModeEnabled);
        if (DEBUG_SETTINGS) {
            Log.d(TAG, "compatibility mode:" + compatibilityModeEnabled);
        }
        int[] grantPermissionsUserIds = EMPTY_INT_ARRAY;
        synchronized (this.mPackages) {
            ArrayList<PreferredActivity> removed = new ArrayList<>();
            for (int i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
                PreferredIntentResolver pir = this.mSettings.mPreferredActivities.valueAt(i);
                removed.clear();
                for (PreferredActivity pa : pir.filterSet()) {
                    if (this.mActivities.mActivities.get(pa.mPref.mComponent) == null) {
                        removed.add(pa);
                    }
                }
                if (removed.size() > 0) {
                    for (int r = 0; r < removed.size(); r++) {
                        PreferredActivity pa2 = removed.get(r);
                        Slog.w(TAG, "Removing dangling preferred activity: " + pa2.mPref.mComponent);
                        pir.removeFilter(pa2);
                    }
                    this.mSettings.writePackageRestrictionsLPr(this.mSettings.mPreferredActivities.keyAt(i));
                }
            }
            for (int userId : UserManagerService.getInstance().getUserIds()) {
                if (!this.mSettings.areDefaultRuntimePermissionsGrantedLPr(userId)) {
                    grantPermissionsUserIds = ArrayUtils.appendInt(grantPermissionsUserIds, userId);
                }
            }
        }
        sUserManager.systemReady();
        for (int i2 : grantPermissionsUserIds) {
            this.mDefaultPermissionPolicy.grantDefaultPermissions(i2);
        }
        if (this.mPostSystemReadyMessages != null) {
            for (Message msg : this.mPostSystemReadyMessages) {
                msg.sendToTarget();
            }
            this.mPostSystemReadyMessages = null;
        }
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        storage.registerListener(this.mStorageListener);
        this.mInstallerService.systemReady();
        this.mPackageDexOptimizer.systemReady();
        MountServiceInternal mountServiceInternal = (MountServiceInternal) LocalServices.getService(MountServiceInternal.class);
        mountServiceInternal.addExternalStoragePolicy(new MountServiceInternal.ExternalStorageMountPolicy() {
            public int getMountMode(int uid, String packageName) {
                if (Process.isIsolated(uid)) {
                    return 0;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.WRITE_MEDIA_STORAGE", uid) == 0 || PackageManagerService.this.checkUidPermission("android.permission.READ_EXTERNAL_STORAGE", uid) == -1) {
                    return 1;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.WRITE_EXTERNAL_STORAGE", uid) == -1) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int uid, String packageName) {
                return true;
            }
        });
        reconcileUsers(StorageManager.UUID_PRIVATE_INTERNAL);
        reconcileApps(StorageManager.UUID_PRIVATE_INTERNAL);
        if (this.mCtaPermsController == null) {
            return;
        }
        this.mCtaPermsController.systemReady();
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean hasSystemUidErrors() {
        return this.mHasSystemUidErrors;
    }

    static String arrayToString(int[] array) {
        StringBuffer buf = new StringBuffer(128);
        buf.append('[');
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(array[i]);
            }
        }
        buf.append(']');
        return buf.toString();
    }

    static class DumpState {
        public static final int DUMP_ACTIVITY_RESOLVERS = 4;
        public static final int DUMP_CONTENT_RESOLVERS = 32;
        public static final int DUMP_DEXOPT = 1048576;
        public static final int DUMP_DOMAIN_PREFERRED = 262144;
        public static final int DUMP_FEATURES = 2;
        public static final int DUMP_FROZEN = 524288;
        public static final int DUMP_INSTALLS = 65536;
        public static final int DUMP_INTENT_FILTER_VERIFIERS = 131072;
        public static final int DUMP_KEYSETS = 16384;
        public static final int DUMP_LIBS = 1;
        public static final int DUMP_MESSAGES = 512;
        public static final int DUMP_PACKAGES = 128;
        public static final int DUMP_PERMISSIONS = 64;
        public static final int DUMP_PREFERRED = 4096;
        public static final int DUMP_PREFERRED_XML = 8192;
        public static final int DUMP_PROVIDERS = 1024;
        public static final int DUMP_RECEIVER_RESOLVERS = 16;
        public static final int DUMP_SERVICE_RESOLVERS = 8;
        public static final int DUMP_SHARED_USERS = 256;
        public static final int DUMP_VERIFIERS = 2048;
        public static final int DUMP_VERSION = 32768;
        public static final int OPTION_SHOW_FILTERS = 1;
        private int mOptions;
        private SharedUserSetting mSharedUser;
        private boolean mTitlePrinted;
        private int mTypes;

        DumpState() {
        }

        public boolean isDumping(int type) {
            return (this.mTypes == 0 && type != 8192) || (this.mTypes & type) != 0;
        }

        public void setDump(int type) {
            this.mTypes |= type;
        }

        public boolean isOptionEnabled(int option) {
            return (this.mOptions & option) != 0;
        }

        public void setOptionEnabled(int option) {
            this.mOptions |= option;
        }

        public boolean onTitlePrinted() {
            boolean printed = this.mTitlePrinted;
            this.mTitlePrinted = true;
            return printed;
        }

        public boolean getTitlePrinted() {
            return this.mTitlePrinted;
        }

        public void setTitlePrinted(boolean enabled) {
            this.mTitlePrinted = enabled;
        }

        public SharedUserSetting getSharedUser() {
            return this.mSharedUser;
        }

        public void setSharedUser(SharedUserSetting user) {
            this.mSharedUser = user;
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
        new PackageManagerShellCommand(this).exec(this, in, out, err, args, resultReceiver);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String opt;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ActivityManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        DumpState dumpState = new DumpState();
        boolean fullPreferred = false;
        boolean checkin = false;
        String packageName = null;
        ArraySet<String> permissionNames = null;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if (!"-a".equals(opt)) {
                if ("-h".equals(opt)) {
                    pw.println("Package manager dump options:");
                    pw.println("  [-h] [-f] [--checkin] [cmd] ...");
                    pw.println("    --checkin: dump for a checkin");
                    pw.println("    -f: print details of intent filters");
                    pw.println("    -h: print this help");
                    pw.println("  cmd may be one of:");
                    pw.println("    l[ibraries]: list known shared libraries");
                    pw.println("    f[eatures]: list device features");
                    pw.println("    k[eysets]: print known keysets");
                    pw.println("    r[esolvers] [activity|service|receiver|content]: dump intent resolvers");
                    pw.println("    perm[issions]: dump permissions");
                    pw.println("    permission [name ...]: dump declaration and use of given permission");
                    pw.println("    pref[erred]: print preferred package settings");
                    pw.println("    preferred-xml [--full]: print preferred package settings as xml");
                    pw.println("    prov[iders]: dump content providers");
                    pw.println("    p[ackages]: dump installed packages");
                    pw.println("    s[hared-users]: dump shared user IDs");
                    pw.println("    m[essages]: print collected runtime messages");
                    pw.println("    v[erifiers]: print package verifier info");
                    pw.println("    d[omain-preferred-apps]: print domains preferred apps");
                    pw.println("    i[ntent-filter-verifiers]|ifv: print intent filter verifier info");
                    pw.println("    version: print database version info");
                    pw.println("    write: write current settings now");
                    pw.println("    installs: details about install sessions");
                    pw.println("    check-permission <permission> <package> [<user>]: does pkg hold perm?");
                    pw.println("    dexopt: dump dexopt state");
                    pw.println("    <package.name>: info about given package");
                    return;
                }
                if ("--checkin".equals(opt)) {
                    checkin = true;
                } else if ("-f".equals(opt)) {
                    dumpState.setOptionEnabled(1);
                } else {
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
            }
        }
        if (opti < args.length) {
            String cmd = args[opti];
            int opti2 = opti + 1;
            if (PLATFORM_PACKAGE_NAME.equals(cmd) || cmd.contains(".")) {
                packageName = cmd;
                dumpState.setOptionEnabled(1);
            } else {
                if ("check-permission".equals(cmd)) {
                    if (opti2 >= args.length) {
                        pw.println("Error: check-permission missing permission argument");
                        return;
                    }
                    String perm = args[opti2];
                    int opti3 = opti2 + 1;
                    if (opti3 >= args.length) {
                        pw.println("Error: check-permission missing package argument");
                        return;
                    }
                    String pkg = args[opti3];
                    int opti4 = opti3 + 1;
                    int user = UserHandle.getUserId(Binder.getCallingUid());
                    if (opti4 < args.length) {
                        try {
                            user = Integer.parseInt(args[opti4]);
                        } catch (NumberFormatException e) {
                            pw.println("Error: check-permission user argument is not a number: " + args[opti4]);
                            return;
                        }
                    }
                    pw.println(checkPermission(perm, pkg, user));
                    return;
                }
                if ("l".equals(cmd) || "libraries".equals(cmd)) {
                    dumpState.setDump(1);
                } else if ("f".equals(cmd) || "features".equals(cmd)) {
                    dumpState.setDump(2);
                } else if ("r".equals(cmd) || "resolvers".equals(cmd)) {
                    if (opti2 >= args.length) {
                        dumpState.setDump(60);
                    } else {
                        while (opti2 < args.length) {
                            String name = args[opti2];
                            if ("a".equals(name) || "activity".equals(name)) {
                                dumpState.setDump(4);
                            } else if ("s".equals(name) || "service".equals(name)) {
                                dumpState.setDump(8);
                            } else if ("r".equals(name) || "receiver".equals(name)) {
                                dumpState.setDump(16);
                            } else {
                                if (!"c".equals(name) && !"content".equals(name)) {
                                    pw.println("Error: unknown resolver table type: " + name);
                                    return;
                                }
                                dumpState.setDump(32);
                            }
                            opti2++;
                        }
                    }
                } else if (TAG_PERMISSION.equals(cmd) || "permissions".equals(cmd)) {
                    dumpState.setDump(64);
                } else if ("permission".equals(cmd)) {
                    if (opti2 >= args.length) {
                        pw.println("Error: permission requires permission name");
                        return;
                    }
                    permissionNames = new ArraySet<>();
                    while (opti2 < args.length) {
                        permissionNames.add(args[opti2]);
                        opti2++;
                    }
                    dumpState.setDump(448);
                } else if ("pref".equals(cmd) || "preferred".equals(cmd)) {
                    dumpState.setDump(4096);
                } else if ("preferred-xml".equals(cmd)) {
                    dumpState.setDump(8192);
                    if (opti2 < args.length && "--full".equals(args[opti2])) {
                        fullPreferred = true;
                        int i = opti2 + 1;
                    }
                } else if ("d".equals(cmd) || "domain-preferred-apps".equals(cmd)) {
                    dumpState.setDump(262144);
                } else if ("p".equals(cmd) || "packages".equals(cmd)) {
                    dumpState.setDump(128);
                } else if ("s".equals(cmd) || "shared-users".equals(cmd)) {
                    dumpState.setDump(256);
                } else if ("prov".equals(cmd) || "providers".equals(cmd)) {
                    dumpState.setDump(1024);
                } else if ("m".equals(cmd) || "messages".equals(cmd)) {
                    dumpState.setDump(512);
                } else if ("v".equals(cmd) || "verifiers".equals(cmd)) {
                    dumpState.setDump(2048);
                } else if ("i".equals(cmd) || "ifv".equals(cmd) || "intent-filter-verifiers".equals(cmd)) {
                    dumpState.setDump(131072);
                } else if ("version".equals(cmd)) {
                    dumpState.setDump(DumpState.DUMP_VERSION);
                } else if ("k".equals(cmd) || "keysets".equals(cmd)) {
                    dumpState.setDump(16384);
                } else if ("installs".equals(cmd)) {
                    dumpState.setDump(65536);
                } else {
                    if ("log".equals(cmd)) {
                        configLogTag(pw, args, opti2);
                        return;
                    }
                    if ("frozen".equals(cmd)) {
                        dumpState.setDump(DumpState.DUMP_FROZEN);
                    } else if ("dexopt".equals(cmd)) {
                        dumpState.setDump(DumpState.DUMP_DEXOPT);
                    } else if ("write".equals(cmd)) {
                        synchronized (this.mPackages) {
                            this.mSettings.writeLPr();
                            pw.println("Settings written.");
                        }
                        return;
                    }
                }
            }
        }
        if (checkin) {
            pw.println("vers,1");
        }
        synchronized (this.mPackages) {
            if (dumpState.isDumping(DumpState.DUMP_VERSION) && packageName == null && !checkin) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                pw.println("Database versions:");
                this.mSettings.dumpVersionLPr(new IndentingPrintWriter(pw, "  "));
            }
            if (dumpState.isDumping(2048) && packageName == null) {
                if (!checkin) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Verifiers:");
                    pw.print("  Required: ");
                    pw.print(this.mRequiredVerifierPackage);
                    pw.print(" (uid=");
                    pw.print(getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
                    pw.println(")");
                } else if (this.mRequiredVerifierPackage != null) {
                    pw.print("vrfy,");
                    pw.print(this.mRequiredVerifierPackage);
                    pw.print(",");
                    pw.println(getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
                }
            }
            if (dumpState.isDumping(131072) && packageName == null) {
                if (this.mIntentFilterVerifierComponent != null) {
                    String verifierPackageName = this.mIntentFilterVerifierComponent.getPackageName();
                    if (!checkin) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        pw.println("Intent Filter Verifier:");
                        pw.print("  Using: ");
                        pw.print(verifierPackageName);
                        pw.print(" (uid=");
                        pw.print(getPackageUid(verifierPackageName, 268435456, 0));
                        pw.println(")");
                    } else if (verifierPackageName != null) {
                        pw.print("ifv,");
                        pw.print(verifierPackageName);
                        pw.print(",");
                        pw.println(getPackageUid(verifierPackageName, 268435456, 0));
                    }
                } else {
                    pw.println();
                    pw.println("No Intent Filter Verifier available!");
                }
            }
            if (dumpState.isDumping(1) && packageName == null) {
                boolean printedHeader = false;
                for (String name2 : this.mSharedLibraries.keySet()) {
                    SharedLibraryEntry ent = this.mSharedLibraries.get(name2);
                    if (checkin) {
                        pw.print("lib,");
                    } else {
                        if (!printedHeader) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Libraries:");
                            printedHeader = true;
                        }
                        pw.print("  ");
                    }
                    pw.print(name2);
                    if (!checkin) {
                        pw.print(" -> ");
                    }
                    if (ent.path != null) {
                        if (checkin) {
                            pw.print(",jar,");
                            pw.print(ent.path);
                        } else {
                            pw.print("(jar) ");
                            pw.print(ent.path);
                        }
                    } else if (checkin) {
                        pw.print(",apk,");
                        pw.print(ent.apk);
                    } else {
                        pw.print("(apk) ");
                        pw.print(ent.apk);
                    }
                    pw.println();
                }
            }
            if (dumpState.isDumping(2) && packageName == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                if (!checkin) {
                    pw.println("Features:");
                }
                for (FeatureInfo feat : this.mAvailableFeatures.values()) {
                    if (checkin) {
                        pw.print("feat,");
                        pw.print(feat.name);
                        pw.print(",");
                        pw.println(feat.version);
                    } else {
                        pw.print("  ");
                        pw.print(feat.name);
                        if (feat.version > 0) {
                            pw.print(" version=");
                            pw.print(feat.version);
                        }
                        pw.println();
                    }
                }
            }
            if (!checkin && dumpState.isDumping(4)) {
                if (this.mActivities.dump(pw, dumpState.getTitlePrinted() ? "\nActivity Resolver Table:" : "Activity Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
                    dumpState.setTitlePrinted(true);
                }
            }
            if (!checkin && dumpState.isDumping(16)) {
                if (this.mReceivers.dump(pw, dumpState.getTitlePrinted() ? "\nReceiver Resolver Table:" : "Receiver Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
                    dumpState.setTitlePrinted(true);
                }
            }
            if (!checkin && dumpState.isDumping(8)) {
                if (this.mServices.dump(pw, dumpState.getTitlePrinted() ? "\nService Resolver Table:" : "Service Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
                    dumpState.setTitlePrinted(true);
                }
            }
            if (!checkin && dumpState.isDumping(32)) {
                if (this.mProviders.dump(pw, dumpState.getTitlePrinted() ? "\nProvider Resolver Table:" : "Provider Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), true)) {
                    dumpState.setTitlePrinted(true);
                }
            }
            if (!checkin && dumpState.isDumping(4096)) {
                for (int i2 = 0; i2 < this.mSettings.mPreferredActivities.size(); i2++) {
                    PreferredIntentResolver pir = this.mSettings.mPreferredActivities.valueAt(i2);
                    int user2 = this.mSettings.mPreferredActivities.keyAt(i2);
                    if (pir.dump(pw, dumpState.getTitlePrinted() ? "\nPreferred Activities User " + user2 + ":" : "Preferred Activities User " + user2 + ":", "  ", packageName, true, false)) {
                        dumpState.setTitlePrinted(true);
                    }
                }
            }
            if (!checkin && dumpState.isDumping(8192)) {
                pw.flush();
                FileOutputStream fout = new FileOutputStream(fd);
                BufferedOutputStream str = new BufferedOutputStream(fout);
                XmlSerializer serializer = new FastXmlSerializer();
                try {
                    try {
                        serializer.setOutput(str, StandardCharsets.UTF_8.name());
                        serializer.startDocument(null, true);
                        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        this.mSettings.writePreferredActivitiesLPr(serializer, 0, fullPreferred);
                        serializer.endDocument();
                        serializer.flush();
                    } catch (IllegalArgumentException e2) {
                        pw.println("Failed writing: " + e2);
                    }
                } catch (IOException e3) {
                    pw.println("Failed writing: " + e3);
                } catch (IllegalStateException e4) {
                    pw.println("Failed writing: " + e4);
                }
            }
            if (!checkin && dumpState.isDumping(262144) && packageName == null) {
                pw.println();
                int count = this.mSettings.mPackages.size();
                if (count == 0) {
                    pw.println("No applications!");
                    pw.println();
                } else {
                    Collection<PackageSetting> allPackageSettings = this.mSettings.mPackages.values();
                    if (allPackageSettings.size() == 0) {
                        pw.println("No domain preferred apps!");
                        pw.println();
                    } else {
                        pw.println("App verification status:");
                        pw.println();
                        int count2 = 0;
                        Iterator ps$iterator = allPackageSettings.iterator();
                        while (ps$iterator.hasNext()) {
                            IntentFilterVerificationInfo ivi = ((PackageSetting) ps$iterator.next()).getIntentFilterVerificationInfo();
                            if (ivi != null && ivi.getPackageName() != null) {
                                pw.println("  Package: " + ivi.getPackageName());
                                pw.println("  Domains: " + ivi.getDomainsString());
                                pw.println("  Status:  " + ivi.getStatusString());
                                pw.println();
                                count2++;
                            }
                        }
                        if (count2 == 0) {
                            pw.println("  No app verification established.");
                            pw.println();
                        }
                        for (int userId : sUserManager.getUserIds()) {
                            pw.println("App linkages for user " + userId + ":");
                            pw.println();
                            int count3 = 0;
                            for (PackageSetting ps : allPackageSettings) {
                                long status = ps.getDomainVerificationStatusForUser(userId);
                                if ((status >> 32) != 0) {
                                    pw.println("  Package: " + ps.name);
                                    pw.println("  Domains: " + dumpDomainString(ps.name));
                                    String statusStr = IntentFilterVerificationInfo.getStatusStringFromValue(status);
                                    pw.println("  Status:  " + statusStr);
                                    pw.println();
                                    count3++;
                                }
                            }
                            if (count3 == 0) {
                                pw.println("  No configured app linkages.");
                                pw.println();
                            }
                        }
                    }
                }
            }
            if (!checkin && dumpState.isDumping(64)) {
                this.mSettings.dumpPermissionsLPr(pw, packageName, permissionNames, dumpState);
                if (packageName == null && permissionNames == null) {
                    for (int iperm = 0; iperm < this.mAppOpPermissionPackages.size(); iperm++) {
                        if (iperm == 0) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("AppOp Permissions:");
                        }
                        pw.print("  AppOp Permission ");
                        pw.print(this.mAppOpPermissionPackages.keyAt(iperm));
                        pw.println(":");
                        ArraySet<String> pkgs = this.mAppOpPermissionPackages.valueAt(iperm);
                        for (int ipkg = 0; ipkg < pkgs.size(); ipkg++) {
                            pw.print("    ");
                            pw.println(pkgs.valueAt(ipkg));
                        }
                    }
                }
            }
            if (!checkin && dumpState.isDumping(1024)) {
                boolean printedSomething = false;
                for (PackageParser.Provider p : this.mProviders.mProviders.values()) {
                    if (packageName == null || packageName.equals(p.info.packageName)) {
                        if (!printedSomething) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Registered ContentProviders:");
                            printedSomething = true;
                        }
                        pw.print("  ");
                        p.printComponentShortName(pw);
                        pw.println(":");
                        pw.print("    ");
                        pw.println(p.toString());
                    }
                }
                boolean printedSomething2 = false;
                for (Map.Entry<String, PackageParser.Provider> entry : this.mProvidersByAuthority.entrySet()) {
                    PackageParser.Provider p2 = entry.getValue();
                    if (packageName == null || packageName.equals(p2.info.packageName)) {
                        if (!printedSomething2) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("ContentProvider Authorities:");
                            printedSomething2 = true;
                        }
                        pw.print("  [");
                        pw.print(entry.getKey());
                        pw.println("]:");
                        pw.print("    ");
                        pw.println(p2.toString());
                        if (p2.info != null && p2.info.applicationInfo != null) {
                            String appInfo = p2.info.applicationInfo.toString();
                            pw.print("      applicationInfo=");
                            pw.println(appInfo);
                        }
                    }
                }
            }
            if (!checkin && dumpState.isDumping(16384)) {
                this.mSettings.mKeySetManagerService.dumpLPr(pw, packageName, dumpState);
            }
            if (dumpState.isDumping(128)) {
                this.mSettings.dumpPackagesLPr(pw, packageName, permissionNames, dumpState, checkin);
            }
            if (dumpState.isDumping(256)) {
                this.mSettings.dumpSharedUsersLPr(pw, packageName, permissionNames, dumpState, checkin);
            }
            if (!checkin && dumpState.isDumping(64) && packageName == null) {
                this.mSettings.dumpRestoredPermissionGrantsLPr(pw, dumpState);
            }
            if (!checkin && dumpState.isDumping(65536) && packageName == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                this.mInstallerService.dump(new IndentingPrintWriter(pw, "  ", 120));
            }
            if (!checkin && dumpState.isDumping(DumpState.DUMP_FROZEN) && packageName == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
                ipw.println();
                ipw.println("Frozen packages:");
                ipw.increaseIndent();
                if (this.mFrozenPackages.size() == 0) {
                    ipw.println("(none)");
                } else {
                    for (int i3 = 0; i3 < this.mFrozenPackages.size(); i3++) {
                        ipw.println(this.mFrozenPackages.valueAt(i3));
                    }
                }
                ipw.decreaseIndent();
            }
            if (!checkin && dumpState.isDumping(DumpState.DUMP_DEXOPT)) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                dumpDexoptStateLPr(pw, packageName);
            }
            if (!checkin && dumpState.isDumping(512) && packageName == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                this.mSettings.dumpReadMessagesLPr(pw, dumpState);
                pw.println();
                pw.println("Package warning messages:");
                BufferedReader in = null;
                try {
                    BufferedReader in2 = new BufferedReader(new FileReader(getSettingsProblemFile()));
                    while (true) {
                        try {
                            String line = in2.readLine();
                            if (line == null) {
                                break;
                            } else if (!line.contains("ignored: updated version")) {
                                pw.println(line);
                            }
                        } catch (IOException e5) {
                            in = in2;
                            IoUtils.closeQuietly(in);
                        } catch (Throwable th) {
                            th = th;
                            in = in2;
                            IoUtils.closeQuietly(in);
                            throw th;
                        }
                    }
                    IoUtils.closeQuietly(in2);
                } catch (IOException e6) {
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (checkin && dumpState.isDumping(512)) {
                BufferedReader in3 = null;
                try {
                    BufferedReader in4 = new BufferedReader(new FileReader(getSettingsProblemFile()));
                    while (true) {
                        try {
                            String line2 = in4.readLine();
                            if (line2 == null) {
                                break;
                            } else if (!line2.contains("ignored: updated version")) {
                                pw.print("msg,");
                                pw.println(line2);
                            }
                        } catch (IOException e7) {
                            in3 = in4;
                            IoUtils.closeQuietly(in3);
                        } catch (Throwable th3) {
                            th = th3;
                            in3 = in4;
                            IoUtils.closeQuietly(in3);
                            throw th;
                        }
                    }
                    IoUtils.closeQuietly(in4);
                } catch (IOException e8) {
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }
    }

    protected void configLogTag(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 >= args.length) {
            pw.println("  Invalid argument!");
            return;
        }
        String tag = args[opti];
        boolean on = "on".equals(args[opti + 1]);
        if (tag.equals("a")) {
            DEBUG_SETTINGS = on;
            DEBUG_PREFERRED = on;
            DEBUG_UPGRADE = on;
            DEBUG_DOMAIN_VERIFICATION = on;
            DEBUG_BACKUP = on;
            DEBUG_INSTALL = on;
            DEBUG_SD_INSTALL = on;
            DEBUG_REMOVE = on;
            DEBUG_SHOW_INFO = on;
            DEBUG_PACKAGE_INFO = on;
            DEBUG_INTENT_MATCHING = on;
            DEBUG_PACKAGE_SCANNING = on;
            DEBUG_VERIFY = on;
            DEBUG_BROADCASTS = on;
            DEBUG_DEXOPT = on;
            DEBUG_ABI_SELECTION = on;
            DEBUG_EPHEMERAL = on;
            DEBUG_TRIAGED_MISSING = on;
            DEBUG_APP_DATA = on;
            if (this.mCtaPermsController == null) {
                return;
            }
            this.mCtaPermsController.configDebugFlag(on);
            return;
        }
        if (tag.equals("se")) {
            DEBUG_SETTINGS = on;
            return;
        }
        if (tag.equals("pr")) {
            DEBUG_PREFERRED = on;
            return;
        }
        if (tag.equals("up")) {
            DEBUG_UPGRADE = on;
        } else {
            if (!tag.equals("in")) {
                return;
            }
            DEBUG_INSTALL = on;
            DEBUG_SD_INSTALL = on;
            DEBUG_BROADCASTS = on;
        }
    }

    private void dumpDexoptStateLPr(PrintWriter pw, String packageName) {
        Collection<PackageParser.Package> packages;
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
        ipw.println();
        ipw.println("Dexopt state:");
        ipw.increaseIndent();
        if (packageName != null) {
            PackageParser.Package targetPackage = this.mPackages.get(packageName);
            if (targetPackage != null) {
                packages = Collections.singletonList(targetPackage);
            } else {
                ipw.println("Unable to find package: " + packageName);
                return;
            }
        } else {
            packages = this.mPackages.values();
        }
        for (PackageParser.Package pkg : packages) {
            ipw.println("[" + pkg.packageName + "]");
            ipw.increaseIndent();
            this.mPackageDexOptimizer.dumpDexoptState(ipw, pkg);
            ipw.decreaseIndent();
        }
    }

    private String dumpDomainString(String packageName) {
        List<IntentFilterVerificationInfo> iviList = getIntentFilterVerifications(packageName).getList();
        List<IntentFilter> filters = getAllIntentFilters(packageName).getList();
        ArraySet<String> result = new ArraySet<>();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory("android.intent.category.BROWSABLE") && (filter.hasDataScheme("http") || filter.hasDataScheme("https"))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        StringBuilder sb = new StringBuilder(result.size() * 16);
        for (String domain : result) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(domain);
        }
        return sb.toString();
    }

    static String getEncryptKey() {
        try {
            String sdEncKey = SystemKeyStore.getInstance().retrieveKeyHexString(SD_ENCRYPTION_KEYSTORE_NAME);
            if (sdEncKey == null && (sdEncKey = SystemKeyStore.getInstance().generateNewKeyHexString(128, SD_ENCRYPTION_ALGORITHM, SD_ENCRYPTION_KEYSTORE_NAME)) == null) {
                Slog.e(TAG, "Failed to create encryption keys");
                return null;
            }
            return sdEncKey;
        } catch (IOException ioe) {
            Slog.e(TAG, "Failed to retrieve encryption keys with exception: " + ioe);
            return null;
        } catch (NoSuchAlgorithmException nsae) {
            Slog.e(TAG, "Failed to create encryption keys with exception: " + nsae);
            return null;
        }
    }

    public void updateExternalMediaStatus(final boolean mediaStatus, final boolean reportStatus) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("Media status can only be updated by the system");
        }
        synchronized (this.mPackages) {
            Log.i(TAG, "Updating external media status from " + (this.mMediaMounted ? "mounted" : "unmounted") + " to " + (mediaStatus ? "mounted" : "unmounted"));
            if (DEBUG_SD_INSTALL) {
                Log.i(TAG, "updateExternalMediaStatus:: mediaStatus=" + mediaStatus + ", mMediaMounted=" + this.mMediaMounted);
            }
            if (mediaStatus == this.mMediaMounted) {
                Message msg = this.mHandler.obtainMessage(12, reportStatus ? 1 : 0, -1);
                this.mHandler.sendMessage(msg);
            } else {
                this.mMediaMounted = mediaStatus;
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        PackageManagerService.this.updateExternalMediaStatusInner(mediaStatus, reportStatus, true);
                    }
                });
            }
        }
    }

    public void scanAvailableAsecs() {
        updateExternalMediaStatusInner(true, false, false);
    }

    private void updateExternalMediaStatusInner(boolean isMounted, boolean reportStatus, boolean externalStorage) {
        ArrayMap<AsecInstallArgs, String> processCids = new ArrayMap<>();
        int[] uidArr = EmptyArray.INT;
        String[] list = PackageHelper.getSecureContainerList();
        if (ArrayUtils.isEmpty(list)) {
            Log.i(TAG, "No secure containers found");
        } else {
            synchronized (this.mPackages) {
                for (String cid : list) {
                    if (!PackageInstallerService.isStageName(cid)) {
                        if (DEBUG_SD_INSTALL) {
                            Log.i(TAG, "Processing container " + cid);
                        }
                        String pkgName = getAsecPackageName(cid);
                        if (pkgName == null) {
                            Slog.i(TAG, "Found stale container " + cid + " with no package name");
                        } else {
                            if (DEBUG_SD_INSTALL) {
                                Log.i(TAG, "Looking for pkg : " + pkgName);
                            }
                            PackageSetting ps = this.mSettings.mPackages.get(pkgName);
                            if (ps == null) {
                                Slog.i(TAG, "Found stale container " + cid + " with no matching settings");
                            } else if (!externalStorage || isMounted || isExternal(ps)) {
                                AsecInstallArgs args = new AsecInstallArgs(cid, InstructionSets.getAppDexInstructionSets(ps), ps.isForwardLocked());
                                if (ps.codePathString != null && ps.codePathString.startsWith(args.getCodePath())) {
                                    if (DEBUG_SD_INSTALL) {
                                        Log.i(TAG, "Container : " + cid + " corresponds to pkg : " + pkgName + " at code path: " + ps.codePathString);
                                    }
                                    processCids.put(args, ps.codePathString);
                                    int uid = ps.appId;
                                    if (uid != -1) {
                                        uidArr = ArrayUtils.appendInt(uidArr, uid);
                                    }
                                } else {
                                    Slog.i(TAG, "Found stale container " + cid + ": expected codePath=" + ps.codePathString);
                                }
                            }
                        }
                    }
                }
            }
            Arrays.sort(uidArr);
        }
        if (isMounted) {
            if (DEBUG_SD_INSTALL) {
                Log.i(TAG, "Loading packages");
            }
            loadMediaPackages(processCids, uidArr, externalStorage);
            startCleaningPackages();
            this.mInstallerService.onSecureContainersAvailable();
            return;
        }
        if (DEBUG_SD_INSTALL) {
            Log.i(TAG, "Unloading packages");
        }
        unloadMediaPackages(processCids, uidArr, reportStatus);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<ApplicationInfo> infos, IIntentReceiver finishedReceiver) {
        int size = infos.size();
        String[] packageNames = new String[size];
        int[] packageUids = new int[size];
        for (int i = 0; i < size; i++) {
            ApplicationInfo info = infos.get(i);
            packageNames[i] = info.packageName;
            packageUids[i] = info.uid;
        }
        sendResourcesChangedBroadcast(mediaStatus, replacing, packageNames, packageUids, finishedReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<String> pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        sendResourcesChangedBroadcast(mediaStatus, replacing, (String[]) pkgList.toArray(new String[pkgList.size()]), uidArr, finishedReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, String[] pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        int size = pkgList.length;
        if (size <= 0) {
            return;
        }
        Bundle extras = new Bundle();
        extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
        if (uidArr != null) {
            extras.putIntArray("android.intent.extra.changed_uid_list", uidArr);
        }
        if (replacing) {
            extras.putBoolean("android.intent.extra.REPLACING", replacing);
        }
        String action = mediaStatus ? "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE" : "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
        sendPackageBroadcast(action, null, extras, 0, null, finishedReceiver, null);
    }

    private void loadMediaPackages(ArrayMap<AsecInstallArgs, String> processCids, int[] uidArr, boolean externalStorage) {
        ArrayList<String> pkgList = new ArrayList<>();
        Set<AsecInstallArgs> keys = processCids.keySet();
        for (AsecInstallArgs args : keys) {
            String codePath = processCids.get(args);
            if (DEBUG_SD_INSTALL) {
                Log.i(TAG, "Loading container : " + args.cid);
            }
            int retCode = -18;
            try {
                if (args.doPreInstall(1) != 1) {
                    Slog.e(TAG, "Failed to mount cid : " + args.cid + " when installing from sdcard");
                    Log.w(TAG, "Container " + args.cid + " is stale, retCode=-18");
                } else if (codePath == null || !codePath.startsWith(args.getCodePath())) {
                    Slog.e(TAG, "Container " + args.cid + " cachepath " + args.getCodePath() + " does not match one in settings " + codePath);
                    Log.w(TAG, "Container " + args.cid + " is stale, retCode=-18");
                } else {
                    int parseFlags = this.mDefParseFlags;
                    if (args.isExternalAsec()) {
                        parseFlags |= 32;
                    }
                    if (args.isFwdLocked()) {
                        parseFlags |= 16;
                    }
                    synchronized (this.mInstallLock) {
                        PackageParser.Package pkg = null;
                        try {
                            pkg = scanPackageTracedLI(new File(codePath), parseFlags, 262144, 0L, (UserHandle) null);
                        } catch (PackageManagerException e) {
                            Slog.w(TAG, "Failed to scan " + codePath + ": " + e.getMessage());
                        }
                        if (pkg != null) {
                            synchronized (this.mPackages) {
                                retCode = 1;
                                pkgList.add(pkg.packageName);
                                args.doPostInstall(1, pkg.applicationInfo.uid);
                            }
                        } else {
                            Slog.i(TAG, "Failed to install pkg from  " + codePath + " from sdcard");
                        }
                    }
                    if (retCode != 1) {
                        Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + retCode);
                    }
                }
            } catch (Throwable th) {
                if (retCode != 1) {
                    Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + retCode);
                }
                throw th;
            }
        }
        synchronized (this.mPackages) {
            Settings.VersionInfo ver = externalStorage ? this.mSettings.getExternalVersion() : this.mSettings.getInternalVersion();
            String volumeUuid = externalStorage ? "primary_physical" : StorageManager.UUID_PRIVATE_INTERNAL;
            int updateFlags = 1;
            if (ver.sdkVersion != this.mSdkVersion) {
                logCriticalInfo(4, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for external");
                updateFlags = 7;
            }
            updatePermissionsLPw(null, null, volumeUuid, updateFlags);
            ver.forceCurrent();
            this.mSettings.writeLPr();
        }
        if (pkgList.size() > 0) {
            sendResourcesChangedBroadcast(true, false, pkgList, uidArr, (IIntentReceiver) null);
        }
    }

    private void unloadAllContainers(Set<AsecInstallArgs> cidArgs) {
        for (AsecInstallArgs arg : cidArgs) {
            synchronized (this.mInstallLock) {
                arg.doPostDeleteLI(false);
            }
        }
    }

    private void unloadMediaPackages(ArrayMap<AsecInstallArgs, String> processCids, int[] uidArr, final boolean reportStatus) {
        Throwable th;
        Throwable th2;
        if (DEBUG_SD_INSTALL) {
            Log.i(TAG, "unloading media packages");
        }
        ArrayList<String> pkgList = new ArrayList<>();
        ArrayList<AsecInstallArgs> failedList = new ArrayList<>();
        final Set<AsecInstallArgs> keys = processCids.keySet();
        for (AsecInstallArgs args : keys) {
            String pkgName = args.getPackageName();
            if (DEBUG_SD_INSTALL) {
                Log.i(TAG, "Trying to unload pkg : " + pkgName);
            }
            PackageRemovedInfo outInfo = new PackageRemovedInfo();
            synchronized (this.mInstallLock) {
                Throwable th3 = null;
                PackageFreezer freezer = null;
                try {
                    freezer = freezePackageForDelete(pkgName, 1, "unloadMediaPackages");
                    boolean res = deletePackageLIF(pkgName, null, false, null, 1, outInfo, false, null);
                    if (freezer != null) {
                        try {
                            freezer.close();
                        } catch (Throwable th4) {
                            th3 = th4;
                        }
                    }
                    if (th3 != null) {
                        throw th3;
                    }
                    if (res) {
                        pkgList.add(pkgName);
                    } else {
                        Slog.e(TAG, "Failed to delete pkg from sdcard : " + pkgName);
                        failedList.add(args);
                    }
                } catch (Throwable th5) {
                    try {
                        throw th5;
                    } catch (Throwable th6) {
                        th = th5;
                        th2 = th6;
                        if (freezer != null) {
                            try {
                                freezer.close();
                            } catch (Throwable th7) {
                                if (th == null) {
                                    th = th7;
                                } else if (th != th7) {
                                    th.addSuppressed(th7);
                                }
                            }
                        }
                        if (th != null) {
                            throw th2;
                        }
                        throw th;
                    }
                }
            }
        }
        synchronized (this.mPackages) {
            if (pkgList.size() > 0) {
                if (DEBUG_SD_INSTALL) {
                    Log.i(TAG, "update mSettings.writeLP ");
                }
                this.mSettings.writeLPr();
            }
        }
        if (pkgList.size() > 0) {
            sendResourcesChangedBroadcast(false, false, pkgList, uidArr, (IIntentReceiver) new IIntentReceiver.Stub() {
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                    Message msg = PackageManagerService.this.mHandler.obtainMessage(12, reportStatus ? 1 : 0, 1, keys);
                    PackageManagerService.this.mHandler.sendMessage(msg);
                }
            });
        } else {
            Message msg = this.mHandler.obtainMessage(12, reportStatus ? 1 : 0, -1, keys);
            this.mHandler.sendMessage(msg);
        }
    }

    private void loadPrivatePackages(final VolumeInfo vol) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.loadPrivatePackagesInner(vol);
            }
        });
    }

    private void loadPrivatePackagesInner(VolumeInfo vol) {
        Settings.VersionInfo ver;
        List<PackageSetting> packages;
        int flags;
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e(TAG, "Loading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList<PackageFreezer> freezers = new ArrayList<>();
        ArrayList<ApplicationInfo> loaded = new ArrayList<>();
        int parseFlags = this.mDefParseFlags | 32;
        synchronized (this.mPackages) {
            ver = this.mSettings.findOrCreateVersion(volumeUuid);
            packages = this.mSettings.getVolumePackagesLPr(volumeUuid);
        }
        for (PackageSetting ps : packages) {
            freezers.add(freezePackage(ps.name, "loadPrivatePackagesInner"));
            synchronized (this.mInstallLock) {
                try {
                    PackageParser.Package pkg = scanPackageTracedLI(ps.codePath, parseFlags, 16384, 0L, (UserHandle) null);
                    loaded.add(pkg.applicationInfo);
                } catch (PackageManagerException e) {
                    Slog.w(TAG, "Failed to scan " + ps.codePath + ": " + e.getMessage());
                }
                if (!Build.FINGERPRINT.equals(ver.fingerprint)) {
                    clearAppDataLIF(ps.pkg, -1, 515);
                }
            }
        }
        StorageManager sm = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal umInternal = getUserManagerInternal();
        for (UserInfo user : um.getUsers()) {
            if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
                flags = 3;
            } else if (umInternal.isUserRunning(user.id)) {
                flags = 1;
            } else {
                continue;
            }
            sm.prepareUserStorage(volumeUuid, user.id, user.serialNumber, flags);
            synchronized (this.mInstallLock) {
                reconcileAppsDataLI(volumeUuid, user.id, flags);
            }
        }
        synchronized (this.mPackages) {
            int updateFlags = 1;
            if (ver.sdkVersion != this.mSdkVersion) {
                logCriticalInfo(4, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for " + volumeUuid);
                updateFlags = 7;
            }
            updatePermissionsLPw(null, null, volumeUuid, updateFlags);
            ver.forceCurrent();
            this.mSettings.writeLPr();
        }
        for (PackageFreezer freezer : freezers) {
            freezer.close();
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Loaded packages " + loaded);
        }
        sendResourcesChangedBroadcast(true, false, loaded, null);
    }

    private void unloadPrivatePackages(final VolumeInfo vol) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.unloadPrivatePackagesInner(vol);
            }
        });
    }

    private void unloadPrivatePackagesInner(VolumeInfo vol) {
        Throwable th;
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e(TAG, "Unloading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList<ApplicationInfo> unloaded = new ArrayList<>();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                List<PackageSetting> packages = this.mSettings.getVolumePackagesLPr(volumeUuid);
                for (PackageSetting ps : packages) {
                    if (ps.pkg != null) {
                        ApplicationInfo info = ps.pkg.applicationInfo;
                        PackageRemovedInfo outInfo = new PackageRemovedInfo();
                        Throwable th2 = null;
                        PackageFreezer freezer = null;
                        try {
                            freezer = freezePackageForDelete(ps.name, 1, "unloadPrivatePackagesInner");
                            if (deletePackageLIF(ps.name, null, false, null, 1, outInfo, false, null)) {
                                unloaded.add(info);
                            } else {
                                Slog.w(TAG, "Failed to unload " + ps.codePath);
                            }
                            if (freezer != null) {
                                try {
                                    freezer.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                throw th2;
                            }
                            AttributeCache.instance().removePackage(ps.name);
                        } catch (Throwable th4) {
                            th = th4;
                            th = null;
                            if (freezer != null) {
                            }
                            if (th != null) {
                            }
                        }
                    }
                }
                this.mSettings.writeLPr();
            }
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Unloaded packages " + unloaded);
        }
        sendResourcesChangedBroadcast(false, false, unloaded, null);
        ResourcesManager.getInstance().invalidatePath(vol.getPath().getAbsolutePath());
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
        }
    }

    void prepareUserData(int userId, int userSerial, int flags) {
        synchronized (this.mInstallLock) {
            StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
                String volumeUuid = vol.getFsUuid();
                prepareUserDataLI(volumeUuid, userId, userSerial, flags, true);
            }
        }
    }

    private void prepareUserDataLI(String volumeUuid, int userId, int userSerial, int flags, boolean allowRecover) throws InstallerConnection.InstallerException {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        try {
            storage.prepareUserStorage(volumeUuid, userId, userSerial, flags);
            if ((flags & 1) != 0 && !this.mOnlyCore) {
                UserManagerService.enforceSerialNumber(Environment.getDataUserDeDirectory(volumeUuid, userId), userSerial);
                if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                    UserManagerService.enforceSerialNumber(Environment.getDataSystemDeDirectory(userId), userSerial);
                }
            }
            if ((flags & 2) != 0 && !this.mOnlyCore) {
                UserManagerService.enforceSerialNumber(Environment.getDataUserCeDirectory(volumeUuid, userId), userSerial);
                if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                    UserManagerService.enforceSerialNumber(Environment.getDataSystemCeDirectory(userId), userSerial);
                }
            }
            synchronized (this.mInstallLock) {
                this.mInstaller.createUserData(volumeUuid, userId, userSerial, flags);
            }
        } catch (Exception e) {
            logCriticalInfo(5, "Destroying user " + userId + " on volume " + volumeUuid + " because we failed to prepare: " + e);
            destroyUserDataLI(volumeUuid, userId, 3);
            if (!allowRecover) {
                return;
            }
            prepareUserDataLI(volumeUuid, userId, userSerial, flags, false);
        }
    }

    void destroyUserData(int userId, int flags) {
        synchronized (this.mInstallLock) {
            StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
                String volumeUuid = vol.getFsUuid();
                destroyUserDataLI(volumeUuid, userId, flags);
            }
        }
    }

    private void destroyUserDataLI(String volumeUuid, int userId, int flags) throws InstallerConnection.InstallerException {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        try {
            this.mInstaller.destroyUserData(volumeUuid, userId, flags);
            if (Objects.equals(volumeUuid, StorageManager.UUID_PRIVATE_INTERNAL)) {
                if ((flags & 1) != 0) {
                    FileUtils.deleteContentsAndDir(Environment.getUserSystemDirectory(userId));
                    FileUtils.deleteContentsAndDir(Environment.getDataSystemDeDirectory(userId));
                }
                if ((flags & 2) != 0) {
                    FileUtils.deleteContentsAndDir(Environment.getDataSystemCeDirectory(userId));
                }
            }
            storage.destroyUserStorage(volumeUuid, userId, flags);
        } catch (Exception e) {
            logCriticalInfo(5, "Failed to destroy user " + userId + " on volume " + volumeUuid + ": " + e);
        }
    }

    private void reconcileUsers(String volumeUuid) {
        List<File> files = new ArrayList<>();
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataUserDeDirectory(volumeUuid)));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataUserCeDirectory(volumeUuid)));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataSystemDeDirectory()));
        Collections.addAll(files, FileUtils.listFilesOrEmpty(Environment.getDataSystemCeDirectory()));
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    int userId = Integer.parseInt(file.getName());
                    UserInfo info = sUserManager.getUserInfo(userId);
                    boolean destroyUser = false;
                    if (info == null) {
                        logCriticalInfo(5, "Destroying user directory " + file + " because no matching user was found");
                        destroyUser = true;
                    } else if (!this.mOnlyCore) {
                        try {
                            UserManagerService.enforceSerialNumber(file, info.serialNumber);
                        } catch (IOException e) {
                            logCriticalInfo(5, "Destroying user directory " + file + " because we failed to enforce serial number: " + e);
                            destroyUser = true;
                        }
                    }
                    if (destroyUser) {
                        synchronized (this.mInstallLock) {
                            destroyUserDataLI(volumeUuid, userId, 3);
                        }
                    } else {
                        continue;
                    }
                } catch (NumberFormatException e2) {
                    Slog.w(TAG, "Invalid user directory " + file);
                }
            }
        }
    }

    private void assertPackageKnown(String volumeUuid, String packageName) throws PackageManagerException {
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new PackageManagerException("Package " + packageName + " is unknown");
            }
            if (!TextUtils.equals(volumeUuid, ps.volumeUuid)) {
                throw new PackageManagerException("Package " + packageName + " found on unknown volume " + volumeUuid + "; expected volume " + ps.volumeUuid);
            }
        }
    }

    private void assertPackageKnownAndInstalled(String volumeUuid, String packageName, int userId) throws PackageManagerException {
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new PackageManagerException("Package " + packageName + " is unknown");
            }
            if (!TextUtils.equals(volumeUuid, ps.volumeUuid)) {
                throw new PackageManagerException("Package " + packageName + " found on unknown volume " + volumeUuid + "; expected volume " + ps.volumeUuid);
            }
            if (!ps.getInstalled(userId)) {
                throw new PackageManagerException("Package " + packageName + " not installed for user " + userId);
            }
        }
    }

    private void reconcileApps(String volumeUuid) {
        File[] files = FileUtils.listFilesOrEmpty(Environment.getDataAppDirectory(volumeUuid));
        for (File file : files) {
            boolean isPackage = (PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
            if (isPackage) {
                try {
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(file, 4);
                    assertPackageKnown(volumeUuid, pkg.packageName);
                } catch (PackageParser.PackageParserException | PackageManagerException e) {
                    logCriticalInfo(5, "Destroying " + file + " due to: " + e);
                    synchronized (this.mInstallLock) {
                        removeCodePathLI(file);
                    }
                }
            }
        }
    }

    void reconcileAppsData(int userId, int flags) {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            String volumeUuid = vol.getFsUuid();
            synchronized (this.mInstallLock) {
                reconcileAppsDataLI(volumeUuid, userId, flags);
            }
        }
    }

    private void reconcileAppsDataLI(String volumeUuid, int userId, int flags) {
        List<PackageSetting> packages;
        Slog.v(TAG, "reconcileAppsData for " + volumeUuid + " u" + userId + " 0x" + Integer.toHexString(flags));
        addBootEvent("PMS:reconcileAppsDataLI");
        File ceDir = Environment.getDataUserCeDirectory(volumeUuid, userId);
        File deDir = Environment.getDataUserDeDirectory(volumeUuid, userId);
        boolean restoreconNeeded = false;
        if ((flags & 2) != 0) {
            if (StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(userId)) {
                throw new RuntimeException("Yikes, someone asked us to reconcile CE storage while " + userId + " was still locked; this would have caused massive data loss!");
            }
            restoreconNeeded = SELinuxMMAC.isRestoreconNeeded(ceDir);
            File[] files = FileUtils.listFilesOrEmpty(ceDir);
            int i = 0;
            int length = files.length;
            while (true) {
                int i2 = i;
                if (i2 >= length) {
                    break;
                }
                File file = files[i2];
                String packageName = file.getName();
                try {
                    assertPackageKnownAndInstalled(volumeUuid, packageName, userId);
                } catch (PackageManagerException e) {
                    logCriticalInfo(5, "Destroying " + file + " due to: " + e);
                    try {
                        this.mInstaller.destroyAppData(volumeUuid, packageName, userId, 2, 0L);
                    } catch (InstallerConnection.InstallerException e2) {
                        logCriticalInfo(5, "Failed to destroy: " + e2);
                    }
                }
                i = i2 + 1;
            }
        }
        if ((flags & 1) != 0) {
            restoreconNeeded |= SELinuxMMAC.isRestoreconNeeded(deDir);
            File[] files2 = FileUtils.listFilesOrEmpty(deDir);
            int i3 = 0;
            int length2 = files2.length;
            while (true) {
                int i4 = i3;
                if (i4 >= length2) {
                    break;
                }
                File file2 = files2[i4];
                String packageName2 = file2.getName();
                try {
                    assertPackageKnownAndInstalled(volumeUuid, packageName2, userId);
                } catch (PackageManagerException e3) {
                    logCriticalInfo(5, "Destroying " + file2 + " due to: " + e3);
                    try {
                        this.mInstaller.destroyAppData(volumeUuid, packageName2, userId, 1, 0L);
                    } catch (InstallerConnection.InstallerException e22) {
                        logCriticalInfo(5, "Failed to destroy: " + e22);
                    }
                }
                i3 = i4 + 1;
            }
        }
        synchronized (this.mPackages) {
            packages = this.mSettings.getVolumePackagesLPr(volumeUuid);
        }
        int preparedCount = 0;
        for (PackageSetting ps : packages) {
            String packageName3 = ps.name;
            if (ps.pkg == null) {
                Slog.w(TAG, "Odd, missing scanned package " + packageName3);
            } else if (ps.getInstalled(userId)) {
                prepareAppDataLIF(ps.pkg, userId, flags, restoreconNeeded);
                if (maybeMigrateAppDataLIF(ps.pkg, userId)) {
                    prepareAppDataLIF(ps.pkg, userId, flags, restoreconNeeded);
                }
                preparedCount++;
            }
        }
        if (restoreconNeeded) {
            if ((flags & 2) != 0) {
                SELinuxMMAC.setRestoreconDone(ceDir);
            }
            if ((flags & 1) != 0) {
                SELinuxMMAC.setRestoreconDone(deDir);
            }
        }
        Slog.v(TAG, "reconcileAppsData finished " + preparedCount + " packages; restoreconNeeded was " + restoreconNeeded);
    }

    private void prepareAppDataAfterInstallLIF(PackageParser.Package pkg) {
        PackageSetting ps;
        int flags;
        synchronized (this.mPackages) {
            ps = this.mSettings.mPackages.get(pkg.packageName);
            this.mSettings.writeKernelMappingLPr(ps);
        }
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal umInternal = getUserManagerInternal();
        for (UserInfo user : um.getUsers()) {
            if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
                flags = 3;
            } else if (umInternal.isUserRunning(user.id)) {
                flags = 1;
            }
            if (ps.getInstalled(user.id)) {
                prepareAppDataLIF(pkg, user.id, flags, true);
            }
        }
    }

    private void prepareAppDataLIF(PackageParser.Package pkg, int userId, int flags, boolean restoreconNeeded) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataLeafLIF(pkg, userId, flags, restoreconNeeded);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            prepareAppDataLeafLIF((PackageParser.Package) pkg.childPackages.get(i), userId, flags, restoreconNeeded);
        }
    }

    private void prepareAppDataLeafLIF(PackageParser.Package pkg, int userId, int flags, boolean restoreconNeeded) {
        if (DEBUG_APP_DATA) {
            Slog.v(TAG, "prepareAppData for " + pkg.packageName + " u" + userId + " 0x" + Integer.toHexString(flags) + (restoreconNeeded ? " restoreconNeeded" : ""));
        }
        String volumeUuid = pkg.volumeUuid;
        String packageName = pkg.packageName;
        ApplicationInfo app = pkg.applicationInfo;
        int appId = UserHandle.getAppId(app.uid);
        Preconditions.checkNotNull(app.seinfo);
        try {
            this.mInstaller.createAppData(volumeUuid, packageName, userId, flags, appId, app.seinfo, app.targetSdkVersion);
        } catch (InstallerConnection.InstallerException e) {
            if (app.isSystemApp()) {
                logCriticalInfo(6, "Failed to create app data for " + packageName + ", but trying to recover: " + e);
                destroyAppDataLeafLIF(pkg, userId, flags);
                try {
                    this.mInstaller.createAppData(volumeUuid, packageName, userId, flags, appId, app.seinfo, app.targetSdkVersion);
                    logCriticalInfo(3, "Recovery succeeded!");
                } catch (InstallerConnection.InstallerException e2) {
                    logCriticalInfo(3, "Recovery failed!");
                }
            } else {
                Slog.e(TAG, "Failed to create app data for " + packageName + ": " + e);
            }
        }
        if (restoreconNeeded) {
            try {
                this.mInstaller.restoreconAppData(volumeUuid, packageName, userId, flags, appId, app.seinfo);
            } catch (InstallerConnection.InstallerException e3) {
                Slog.e(TAG, "Failed to restorecon for " + packageName + ": " + e3);
            }
        }
        if ((flags & 2) != 0) {
            try {
                long ceDataInode = this.mInstaller.getAppDataInode(volumeUuid, packageName, userId, 2);
                synchronized (this.mPackages) {
                    PackageSetting ps = this.mSettings.mPackages.get(packageName);
                    if (ps != null) {
                        ps.setCeDataInode(ceDataInode, userId);
                    }
                }
            } catch (InstallerConnection.InstallerException e4) {
                Slog.e(TAG, "Failed to find inode for " + packageName + ": " + e4);
            }
        }
        prepareAppDataContentsLeafLIF(pkg, userId, flags);
    }

    private void prepareAppDataContentsLIF(PackageParser.Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataContentsLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            prepareAppDataContentsLeafLIF((PackageParser.Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void prepareAppDataContentsLeafLIF(PackageParser.Package pkg, int userId, int flags) {
        String volumeUuid = pkg.volumeUuid;
        String packageName = pkg.packageName;
        ApplicationInfo app = pkg.applicationInfo;
        if ((flags & 2) == 0 || app.primaryCpuAbi == null || VMRuntime.is64BitAbi(app.primaryCpuAbi)) {
            return;
        }
        String nativeLibPath = app.nativeLibraryDir;
        try {
            this.mInstaller.linkNativeLibraryDirectory(volumeUuid, packageName, nativeLibPath, userId);
        } catch (InstallerConnection.InstallerException e) {
            Slog.e(TAG, "Failed to link native for " + packageName + ": " + e);
        }
    }

    private boolean maybeMigrateAppDataLIF(PackageParser.Package pkg, int userId) {
        if (pkg.isSystemApp() && !StorageManager.isFileEncryptedNativeOrEmulated()) {
            int storageTarget = pkg.applicationInfo.isDefaultToDeviceProtectedStorage() ? 1 : 2;
            try {
                this.mInstaller.migrateAppData(pkg.volumeUuid, pkg.packageName, userId, storageTarget);
                return true;
            } catch (InstallerConnection.InstallerException e) {
                logCriticalInfo(5, "Failed to migrate " + pkg.packageName + ": " + e.getMessage());
                return true;
            }
        }
        return false;
    }

    public PackageFreezer freezePackage(String packageName, String killReason) {
        return freezePackage(packageName, -1, killReason);
    }

    public PackageFreezer freezePackage(String packageName, int userId, String killReason) {
        return new PackageFreezer(packageName, userId, killReason);
    }

    public PackageFreezer freezePackageForInstall(String packageName, int installFlags, String killReason) {
        return freezePackageForInstall(packageName, -1, installFlags, killReason);
    }

    public PackageFreezer freezePackageForInstall(String packageName, int userId, int installFlags, String killReason) {
        if ((installFlags & 4096) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(packageName, userId, killReason);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int deleteFlags, String killReason) {
        return freezePackageForDelete(packageName, -1, deleteFlags, killReason);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int userId, int deleteFlags, String killReason) {
        if ((deleteFlags & 8) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(packageName, userId, killReason);
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

        public PackageFreezer(String packageName, int userId, String killReason) {
            this.mClosed = new AtomicBoolean();
            this.mCloseGuard = CloseGuard.get();
            synchronized (PackageManagerService.this.mPackages) {
                this.mPackageName = packageName;
                this.mWeFroze = PackageManagerService.this.mFrozenPackages.add(this.mPackageName);
                PackageSetting ps = PackageManagerService.this.mSettings.mPackages.get(this.mPackageName);
                if (ps != null) {
                    PackageManagerService.this.killApplication(ps.name, ps.appId, userId, killReason);
                }
                PackageParser.Package p = PackageManagerService.this.mPackages.get(packageName);
                if (p != null && p.childPackages != null) {
                    int N = p.childPackages.size();
                    this.mChildren = new PackageFreezer[N];
                    for (int i = 0; i < N; i++) {
                        this.mChildren[i] = PackageManagerService.this.new PackageFreezer(((PackageParser.Package) p.childPackages.get(i)).packageName, userId, killReason);
                    }
                } else {
                    this.mChildren = null;
                }
            }
            this.mCloseGuard.open("close");
        }

        protected void finalize() throws Throwable {
            try {
                this.mCloseGuard.warnIfOpen();
                close();
            } finally {
                super.finalize();
            }
        }

        @Override
        public void close() {
            this.mCloseGuard.close();
            if (!this.mClosed.compareAndSet(false, true)) {
                return;
            }
            synchronized (PackageManagerService.this.mPackages) {
                if (this.mWeFroze) {
                    PackageManagerService.this.mFrozenPackages.remove(this.mPackageName);
                }
                if (this.mChildren != null) {
                    for (PackageFreezer freezer : this.mChildren) {
                        freezer.close();
                    }
                }
            }
        }
    }

    private void checkPackageFrozen(String packageName) {
        synchronized (this.mPackages) {
            if (!this.mFrozenPackages.contains(packageName)) {
                Slog.wtf(TAG, "Expected " + packageName + " to be frozen!", new Throwable());
            }
        }
    }

    public int movePackage(final String packageName, final String volumeUuid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final UserHandle user = new UserHandle(UserHandle.getCallingUserId());
        final int moveId = this.mNextMoveId.getAndIncrement();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManagerService.this.movePackageInternal(packageName, volumeUuid, moveId, user);
                } catch (PackageManagerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move " + packageName, e);
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, -6);
                }
            }
        });
        return moveId;
    }

    private void movePackageInternal(String packageName, String volumeUuid, final int moveId, UserHandle user) throws PackageManagerException {
        boolean currentAsec;
        String currentVolumeUuid;
        File codeFile;
        String installerPackageName;
        String packageAbiOverride;
        int appId;
        String seinfo;
        String label;
        int targetSdkVersion;
        final PackageFreezer freezer;
        int[] installedUserIds;
        int installFlags;
        boolean moveCompleteApp;
        final File measurePath;
        final long sizeBytes;
        MoveInfo moveInfo;
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        PackageManager pm = this.mContext.getPackageManager();
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (pkg == null || ps == null) {
                throw new PackageManagerException(-2, "Missing package");
            }
            if (pkg.applicationInfo.isSystemApp()) {
                throw new PackageManagerException(-3, "Cannot move system application");
            }
            if (pkg.applicationInfo.isExternalAsec()) {
                currentAsec = true;
                currentVolumeUuid = "primary_physical";
            } else if (pkg.applicationInfo.isForwardLocked()) {
                currentAsec = true;
                currentVolumeUuid = "forward_locked";
            } else {
                currentAsec = false;
                currentVolumeUuid = ps.volumeUuid;
                File probe = new File(pkg.codePath);
                File probeOat = new File(probe, "oat");
                if (!probe.isDirectory() || !probeOat.isDirectory()) {
                    throw new PackageManagerException(-6, "Move only supported for modern cluster style installs");
                }
            }
            if (Objects.equals(currentVolumeUuid, volumeUuid)) {
                throw new PackageManagerException(-6, "Package already moved to " + volumeUuid);
            }
            if (pkg.applicationInfo.isInternal() && isPackageDeviceAdminOnAnyUser(packageName)) {
                throw new PackageManagerException(-8, "Device admin cannot be moved");
            }
            if (this.mFrozenPackages.contains(packageName)) {
                throw new PackageManagerException(-7, "Failed to move already frozen package");
            }
            codeFile = new File(pkg.codePath);
            installerPackageName = ps.installerPackageName;
            packageAbiOverride = ps.cpuAbiOverrideString;
            appId = UserHandle.getAppId(pkg.applicationInfo.uid);
            seinfo = pkg.applicationInfo.seinfo;
            label = String.valueOf(pm.getApplicationLabel(pkg.applicationInfo));
            targetSdkVersion = pkg.applicationInfo.targetSdkVersion;
            freezer = freezePackage(packageName, "movePackageInternal");
            installedUserIds = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
        }
        Bundle extras = new Bundle();
        extras.putString("android.intent.extra.PACKAGE_NAME", packageName);
        extras.putString("android.intent.extra.TITLE", label);
        this.mMoveCallbacks.notifyCreated(moveId, extras);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            installFlags = 16;
            moveCompleteApp = !currentAsec;
            measurePath = Environment.getDataAppDirectory(volumeUuid);
        } else if (Objects.equals("primary_physical", volumeUuid)) {
            installFlags = 8;
            moveCompleteApp = false;
            measurePath = storage.getPrimaryPhysicalVolume().getPath();
        } else {
            VolumeInfo volume = storage.findVolumeByUuid(volumeUuid);
            if (volume == null || volume.getType() != 1 || !volume.isMountedWritable()) {
                freezer.close();
                throw new PackageManagerException(-6, "Move location not mounted private volume");
            }
            Preconditions.checkState(!currentAsec);
            installFlags = 16;
            moveCompleteApp = true;
            measurePath = Environment.getDataAppDirectory(volumeUuid);
        }
        PackageStats stats = new PackageStats(null, -1);
        synchronized (this.mInstaller) {
            for (int userId : installedUserIds) {
                if (!getPackageSizeInfoLI(packageName, userId, stats)) {
                    freezer.close();
                    throw new PackageManagerException(-6, "Failed to measure package size");
                }
            }
        }
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Measured code size " + stats.codeSize + ", data size " + stats.dataSize);
        }
        final long startFreeBytes = measurePath.getFreeSpace();
        if (moveCompleteApp) {
            sizeBytes = stats.codeSize + stats.dataSize;
        } else {
            sizeBytes = stats.codeSize;
        }
        if (sizeBytes > storage.getStorageBytesUntilLow(measurePath)) {
            freezer.close();
            throw new PackageManagerException(-6, "Not enough free space to move");
        }
        this.mMoveCallbacks.notifyStatusChanged(moveId, 10);
        final CountDownLatch installedLatch = new CountDownLatch(1);
        IPackageInstallObserver2.Stub stub = new IPackageInstallObserver2.Stub() {
            public void onUserActionRequired(Intent intent) throws RemoteException {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras2) throws RemoteException {
                if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.d(PackageManagerService.TAG, "Install result for move: " + PackageManager.installStatusToString(returnCode, msg));
                }
                installedLatch.countDown();
                freezer.close();
                int status = PackageManager.installStatusToPublicStatus(returnCode);
                switch (status) {
                    case 0:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, -100);
                        break;
                    case 6:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, -1);
                        break;
                    default:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, -6);
                        break;
                }
            }
        };
        if (moveCompleteApp) {
            new Thread() {
                @Override
                public void run() {
                    while (!installedLatch.await(1L, TimeUnit.SECONDS)) {
                        long deltaFreeBytes = startFreeBytes - measurePath.getFreeSpace();
                        int progress = ((int) MathUtils.constrain((deltaFreeBytes * 80) / sizeBytes, 0L, 80L)) + 10;
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, progress);
                    }
                }
            }.start();
            String dataAppName = codeFile.getName();
            moveInfo = new MoveInfo(moveId, currentVolumeUuid, volumeUuid, packageName, dataAppName, appId, seinfo, targetSdkVersion);
        } else {
            moveInfo = null;
        }
        Message msg = this.mHandler.obtainMessage(5);
        OriginInfo origin = OriginInfo.fromExistingFile(codeFile);
        InstallParams params = new InstallParams(origin, moveInfo, stub, installFlags | 2, installerPackageName, volumeUuid, null, user, packageAbiOverride, null, null);
        params.setTraceMethod("movePackage").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(1048576L, "movePackage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(1048576L, "queueInstall", System.identityHashCode(msg.obj));
        this.mHandler.sendMessage(msg);
    }

    public int movePrimaryStorage(String volumeUuid) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final int realMoveId = this.mNextMoveId.getAndIncrement();
        Bundle extras = new Bundle();
        extras.putString("android.os.storage.extra.FS_UUID", volumeUuid);
        this.mMoveCallbacks.notifyCreated(realMoveId, extras);
        IPackageMoveObserver callback = new IPackageMoveObserver.Stub() {
            public void onCreated(int moveId, Bundle extras2) {
            }

            public void onStatusChanged(int moveId, int status, long estMillis) {
                PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(realMoveId, status, estMillis);
            }
        };
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        storage.setPrimaryStorageUuid(volumeUuid, callback);
        return realMoveId;
    }

    public int getMoveStatus(int moveId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        return this.mMoveCallbacks.mLastStatus.get(moveId);
    }

    public void registerMoveCallback(IPackageMoveObserver callback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.register(callback);
    }

    public void unregisterMoveCallback(IPackageMoveObserver callback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.unregister(callback);
    }

    public boolean setInstallLocation(int loc) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
        if (getInstallLocation() == loc) {
            return true;
        }
        if (loc != 0 && loc != 1 && loc != 2) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "default_install_location", loc);
        return true;
    }

    public int getInstallLocation() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "default_install_location", 0);
    }

    void cleanUpUser(UserManagerService userManager, int userHandle) {
        synchronized (this.mPackages) {
            this.mDirtyUsers.remove(Integer.valueOf(userHandle));
            this.mUserNeedsBadging.delete(userHandle);
            this.mSettings.removeUserLPw(userHandle);
            this.mPendingBroadcasts.remove(userHandle);
            this.mEphemeralApplicationRegistry.onUserRemovedLPw(userHandle);
            removeUnusedPackagesLPw(userManager, userHandle);
        }
    }

    private void removeUnusedPackagesLPw(UserManagerService userManager, final int userHandle) {
        int[] users = userManager.getUserIds();
        for (PackageSetting ps : this.mSettings.mPackages.values()) {
            if (ps.pkg != null) {
                final String packageName = ps.pkg.packageName;
                if ((ps.pkgFlags & 1) == 0) {
                    boolean keep = shouldKeepUninstalledPackageLPr(packageName);
                    if (!keep) {
                        int i = 0;
                        while (true) {
                            if (i >= users.length) {
                                break;
                            }
                            if (users[i] == userHandle || !ps.getInstalled(users[i])) {
                                i++;
                            } else {
                                keep = true;
                                break;
                            }
                        }
                    }
                    if (!keep) {
                        this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                PackageManagerService.this.deletePackageX(packageName, userHandle, 0);
                            }
                        });
                    }
                }
            }
        }
    }

    void createNewUser(int userId) {
        synchronized (this.mInstallLock) {
            this.mSettings.createNewUserLI(this, this.mInstaller, userId);
        }
        synchronized (this.mPackages) {
            scheduleWritePackageRestrictionsLocked(userId);
            scheduleWritePackageListLocked(userId);
            applyFactoryDefaultBrowserLPw(userId);
            primeDomainVerificationsLPw(userId);
        }
    }

    void onBeforeUserStartUninitialized(int userId) {
        synchronized (this.mPackages) {
            if (this.mSettings.areDefaultRuntimePermissionsGrantedLPr(userId)) {
                return;
            }
            this.mDefaultPermissionPolicy.grantDefaultPermissions(userId);
            if (!Build.isPermissionReviewRequired()) {
                return;
            }
            updatePermissionsLPw(null, null, 5);
        }
    }

    public int checkAPKSignatures(String pkg) {
        if (checkSignatures(pkg, PLATFORM_PACKAGE_NAME) == 0) {
            return 1;
        }
        if (checkSignatures(pkg, "com.android.providers.media") == 0) {
            return 2;
        }
        if (checkSignatures(pkg, "com.android.contacts") == 0) {
            return 3;
        }
        if (checkSignatures(pkg, "com.android.mms") == 0) {
            return 4;
        }
        return 5;
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
        VerifierDeviceIdentity verifierDeviceIdentityLPw;
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can read the verifier device identity");
        synchronized (this.mPackages) {
            verifierDeviceIdentityLPw = this.mSettings.getVerifierDeviceIdentityLPw();
        }
        return verifierDeviceIdentityLPw;
    }

    public void setPermissionEnforced(String permission, boolean enforced) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "setPermissionEnforced");
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(permission)) {
            synchronized (this.mPackages) {
                if (this.mSettings.mReadExternalStorageEnforced == null || this.mSettings.mReadExternalStorageEnforced.booleanValue() != enforced) {
                    this.mSettings.mReadExternalStorageEnforced = Boolean.valueOf(enforced);
                    this.mSettings.writeLPr();
                }
            }
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am == null) {
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                am.killProcessesBelowForeground("setPermissionEnforcement");
                return;
            } catch (RemoteException e) {
                return;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        throw new IllegalArgumentException("No selective enforcement for " + permission);
    }

    @Deprecated
    public boolean isPermissionEnforced(String permission) {
        return true;
    }

    public boolean isStorageLow() {
        long token = Binder.clearCallingIdentity();
        try {
            DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            if (dsm != null) {
                return dsm.isMemoryLow();
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean isGmoROM() {
        boolean enabled = "1".equals(SystemProperties.get(MTK_GMO_ROM_OPTIMIZE));
        Log.d(TAG, "isGmoROM() return " + enabled);
        return enabled;
    }

    public static boolean isGmoRAM() {
        boolean enabled = "1".equals(SystemProperties.get(MTK_GMO_RAM_OPTIMIZE));
        Log.d(TAG, "isGmoROM() return " + enabled);
        return enabled;
    }

    public IPackageInstaller getPackageInstaller() {
        return this.mInstallerService;
    }

    private boolean userNeedsBadging(int userId) {
        boolean b;
        int index = this.mUserNeedsBadging.indexOfKey(userId);
        if (index < 0) {
            long token = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = sUserManager.getUserInfo(userId);
                if (userInfo != null && userInfo.isManagedProfile()) {
                    b = true;
                } else {
                    b = false;
                }
                this.mUserNeedsBadging.put(userId, b);
                return b;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return this.mUserNeedsBadging.valueAt(index);
    }

    public KeySet getKeySetByAlias(String packageName, String alias) {
        KeySet keySet;
        if (packageName == null || alias == null) {
            return null;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            keySet = new KeySet(ksms.getKeySetByAliasAndPackageNameLPr(packageName, alias));
        }
        return keySet;
    }

    public KeySet getSigningKeySet(String packageName) {
        KeySet keySet;
        if (packageName == null) {
            return null;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            if (pkg.applicationInfo.uid != Binder.getCallingUid() && 1000 != Binder.getCallingUid()) {
                throw new SecurityException("May not access signing KeySet of other apps.");
            }
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            keySet = new KeySet(ksms.getSigningKeySetByPackageNameLPr(packageName));
        }
        return keySet;
    }

    public boolean isPackageSignedByKeySet(String packageName, KeySet ks) {
        if (packageName == null || ks == null) {
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            IBinder ksh = ks.getToken();
            if (!(ksh instanceof KeySetHandle)) {
                return false;
            }
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            return ksms.packageIsSignedByLPr(packageName, (KeySetHandle) ksh);
        }
    }

    public boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) {
        if (packageName == null || ks == null) {
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            IBinder ksh = ks.getToken();
            if (!(ksh instanceof KeySetHandle)) {
                return false;
            }
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            return ksms.packageIsSignedByExactlyLPr(packageName, (KeySetHandle) ksh);
        }
    }

    private void deletePackageIfUnusedLPr(final String packageName) {
        PackageSetting ps = this.mSettings.mPackages.get(packageName);
        if (ps == null || ps.isAnyInstalled(sUserManager.getUserIds())) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.deletePackageX(packageName, 0, 2);
            }
        });
    }

    private static void checkDowngrade(PackageParser.Package before, PackageInfoLite after) throws PackageManagerException {
        if (after.versionCode < before.mVersionCode) {
            throw new PackageManagerException(-25, "Update version code " + after.versionCode + " is older than current " + before.mVersionCode);
        }
        if (after.versionCode == before.mVersionCode) {
            if (after.baseRevisionCode < before.baseRevisionCode) {
                throw new PackageManagerException(-25, "Update base revision code " + after.baseRevisionCode + " is older than current " + before.baseRevisionCode);
            }
            if (ArrayUtils.isEmpty(after.splitNames)) {
                return;
            }
            for (int i = 0; i < after.splitNames.length; i++) {
                String splitName = after.splitNames[i];
                int j = ArrayUtils.indexOf(before.splitNames, splitName);
                if (j != -1 && after.splitRevisionCodes[i] < before.splitRevisionCodes[j]) {
                    throw new PackageManagerException(-25, "Update split " + splitName + " revision code " + after.splitRevisionCodes[i] + " is older than current " + before.splitRevisionCodes[j]);
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

        public void register(IPackageMoveObserver callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IPackageMoveObserver callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IPackageMoveObserver callback = (IPackageMoveObserver) this.mCallbacks.getBroadcastItem(i);
                try {
                    invokeCallback(callback, msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IPackageMoveObserver callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onCreated(args.argi1, (Bundle) args.arg2);
                    break;
                case 2:
                    callback.onStatusChanged(args.argi1, args.argi2, ((Long) args.arg3).longValue());
                    break;
            }
        }

        private void notifyCreated(int moveId, Bundle extras) {
            Slog.v(PackageManagerService.TAG, "Move " + moveId + " created " + extras.toString());
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.arg2 = extras;
            obtainMessage(1, args).sendToTarget();
        }

        private void notifyStatusChanged(int moveId, int status) {
            notifyStatusChanged(moveId, status, -1L);
        }

        private void notifyStatusChanged(int moveId, int status, long estMillis) {
            Slog.v(PackageManagerService.TAG, "Move " + moveId + " status " + status);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.argi2 = status;
            args.arg3 = Long.valueOf(estMillis);
            obtainMessage(2, args).sendToTarget();
            synchronized (this.mLastStatus) {
                this.mLastStatus.put(moveId, status);
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int uid = msg.arg1;
                    handleOnPermissionsChanged(uid);
                    break;
            }
        }

        public void addListenerLocked(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.register(listener);
        }

        public void removeListenerLocked(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.unregister(listener);
        }

        public void onPermissionsChanged(int uid) {
            if (this.mPermissionListeners.getRegisteredCallbackCount() <= 0) {
                return;
            }
            obtainMessage(1, uid, 0).sendToTarget();
        }

        private void handleOnPermissionsChanged(int uid) {
            int count = this.mPermissionListeners.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    IOnPermissionsChangeListener callback = this.mPermissionListeners.getBroadcastItem(i);
                    try {
                        callback.onPermissionsChanged(uid);
                    } catch (RemoteException e) {
                        Log.e(PackageManagerService.TAG, "Permission listener is dead", e);
                    }
                } finally {
                    this.mPermissionListeners.finishBroadcast();
                }
            }
        }
    }

    private class PackageManagerInternalImpl extends PackageManagerInternal {
        PackageManagerInternalImpl(PackageManagerService this$0, PackageManagerInternalImpl packageManagerInternalImpl) {
            this();
        }

        private PackageManagerInternalImpl() {
        }

        public void setLocationPackagesProvider(PackageManagerInternal.PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setLocationPackagesProviderLPw(provider);
            }
        }

        public void setVoiceInteractionPackagesProvider(PackageManagerInternal.PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setVoiceInteractionPackagesProviderLPw(provider);
            }
        }

        public void setSmsAppPackagesProvider(PackageManagerInternal.PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSmsAppPackagesProviderLPw(provider);
            }
        }

        public void setDialerAppPackagesProvider(PackageManagerInternal.PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setDialerAppPackagesProviderLPw(provider);
            }
        }

        public void setSimCallManagerPackagesProvider(PackageManagerInternal.PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSimCallManagerPackagesProviderLPw(provider);
            }
        }

        public void setSyncAdapterPackagesprovider(PackageManagerInternal.SyncAdapterPackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSyncAdapterPackagesProviderLPw(provider);
            }
        }

        public void grantDefaultPermissionsToDefaultSmsApp(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSmsAppLPr(packageName, userId);
            }
        }

        public void grantDefaultPermissionsToDefaultDialerApp(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.setDefaultDialerPackageNameLPw(packageName, userId);
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultDialerAppLPr(packageName, userId);
            }
        }

        public void grantDefaultPermissionsToDefaultSimCallManager(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSimCallManagerLPr(packageName, userId);
            }
        }

        public void setKeepUninstalledPackages(List<String> packageList) throws Throwable {
            List<String> removedFromList;
            Preconditions.checkNotNull(packageList);
            List<String> list = null;
            synchronized (PackageManagerService.this.mPackages) {
                try {
                    if (PackageManagerService.this.mKeepUninstalledPackages != null) {
                        int packagesCount = PackageManagerService.this.mKeepUninstalledPackages.size();
                        int i = 0;
                        List<String> removedFromList2 = null;
                        while (i < packagesCount) {
                            try {
                                String oldPackage = (String) PackageManagerService.this.mKeepUninstalledPackages.get(i);
                                if (packageList == null || !packageList.contains(oldPackage)) {
                                    removedFromList = removedFromList2 == null ? new ArrayList<>() : removedFromList2;
                                    removedFromList.add(oldPackage);
                                } else {
                                    removedFromList = removedFromList2;
                                }
                                i++;
                                removedFromList2 = removedFromList;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        list = removedFromList2;
                    }
                    PackageManagerService.this.mKeepUninstalledPackages = new ArrayList(packageList);
                    if (list != null) {
                        int removedCount = list.size();
                        for (int i2 = 0; i2 < removedCount; i2++) {
                            PackageManagerService.this.deletePackageIfUnusedLPr(list.get(i2));
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public boolean isPermissionsReviewRequired(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                if (!Build.isPermissionReviewRequired()) {
                    return false;
                }
                PackageSetting packageSetting = PackageManagerService.this.mSettings.mPackages.get(packageName);
                if (packageSetting == null) {
                    return false;
                }
                if (packageSetting.pkg.applicationInfo.targetSdkVersion >= 23 && !CtaUtils.isCtaSupported()) {
                    return false;
                }
                PermissionsState permissionsState = packageSetting.getPermissionsState();
                boolean reviewRequired = permissionsState.isPermissionReviewRequired(userId);
                if (PackageManagerService.this.mCtaPermsController != null) {
                    reviewRequired = PackageManagerService.this.mCtaPermsController.isPermissionReviewRequired(packageSetting.pkg, userId, reviewRequired);
                }
                if (reviewRequired) {
                    Slog.d(PackageManagerService.TAG, "packageName = " + packageName + " needs permission review");
                }
                return reviewRequired;
            }
        }

        public ApplicationInfo getApplicationInfo(String packageName, int userId) {
            return PackageManagerService.this.getApplicationInfo(packageName, 0, userId);
        }

        public ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
            return PackageManagerService.this.getHomeActivitiesAsUser(allHomeCandidates, userId);
        }

        public void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
            PackageManagerService.this.mProtectedPackages.setDeviceAndProfileOwnerPackages(deviceOwnerUserId, deviceOwnerPackage, profileOwnerPackages);
        }

        public boolean canPackageBeWiped(int userId, String packageName) {
            return PackageManagerService.this.mProtectedPackages.canPackageBeWiped(userId, packageName);
        }

        public void initMtkPermErrorDialog(AMEventHookData.BeforeShowAppErrorDialog data, AMEventHookResult result) {
            PackageManagerService.this.initMtkPermErrorDialog(data, result);
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] packageNames, int userId) {
        enforceSystemOrPhoneCaller("grantPermissionsToEnabledCarrierApps");
        synchronized (this.mPackages) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledCarrierAppsLPr(packageNames, userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private static void enforceSystemOrPhoneCaller(String tag) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1001 || callingUid == 1000) {
        } else {
            throw new SecurityException("Cannot call " + tag + " from UID " + callingUid);
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

    public void logAppProcessStartIfNeeded(String processName, int uid, String seinfo, String apkFile, int pid) {
        if (!SecurityLog.isLoggingEnabled()) {
            return;
        }
        Bundle data = new Bundle();
        data.putLong("startTimestamp", System.currentTimeMillis());
        data.putString("processName", processName);
        data.putInt(AWSDBHelper.PackageProcessList.KEY_UID, uid);
        data.putString("seinfo", seinfo);
        data.putString("apkFile", apkFile);
        data.putInt("pid", pid);
        Message msg = this.mProcessLoggingHandler.obtainMessage(1);
        msg.setData(data);
        this.mProcessLoggingHandler.sendMessage(msg);
    }

    public List<String> getPermRecordPkgs() {
        if (this.mCtaPermsController == null) {
            return null;
        }
        return this.mCtaPermsController.getPermRecordPkgs();
    }

    public List<String> getPermRecordPerms(String packageName) {
        if (this.mCtaPermsController == null) {
            return null;
        }
        return this.mCtaPermsController.getPermRecordPerms(packageName);
    }

    public PermissionRecords getPermRecords(String packageName, String permName) {
        if (this.mCtaPermsController == null) {
            return null;
        }
        return this.mCtaPermsController.getPermRecords(packageName, permName);
    }

    private boolean isPackageNeedsReview(PackageParser.Package pkg) {
        if (!CtaUtils.isCtaSupported()) {
            return false;
        }
        boolean appSupportsRuntimePermissions = pkg.applicationInfo.targetSdkVersion >= 23;
        if (pkg.mSharedUserId == null) {
            return (appSupportsRuntimePermissions && pkg.isSystemApp()) ? this.mForcePermReviewPkgs.contains(pkg.packageName) : !this.mBlockPermReviewPkgs.contains(pkg.packageName);
        }
        SharedUserSetting suid = this.mSettings.getSharedUserLPw(pkg.mSharedUserId, 0, 0, false);
        if (suid != null) {
            for (PackageSetting setting : suid.packages) {
                if (appSupportsRuntimePermissions) {
                    if (setting.pkg.isSystemApp()) {
                        return false;
                    }
                } else if (setting.pkg.applicationInfo.targetSdkVersion >= 23) {
                    return false;
                }
            }
        }
        return !this.mBlockPermReviewPkgs.contains(pkg.packageName);
    }

    void initMtkPermErrorDialog(AMEventHookData.BeforeShowAppErrorDialog data, AMEventHookResult result) {
        AppErrorDialog.Data appErrorDialogData = (AppErrorDialog.Data) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.data);
        int uid = ((Integer) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.uid)).intValue();
        String exceptionMsg = appErrorDialogData.exceptionMsg;
        ActivityManagerService am = (ActivityManagerService) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.ams);
        List<MtkAppErrorDialog> dialogList = (List) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.dialogList);
        String processName = (String) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.processName);
        String pkgName = (String) data.get(AMEventHookData.BeforeShowAppErrorDialog.Index.pkgName);
        String permName = this.mCtaPermsController != null ? this.mCtaPermsController.parsePermName(uid, pkgName, exceptionMsg) : null;
        if (permName == null) {
            return;
        }
        dialogList.add(new MtkPermErrorDialog(this.mContext, am, appErrorDialogData, permName, processName, pkgName));
        result.addAction(AMEventHookAction.AM_ReplaceDialog);
    }

    public void onAmsAddedtoServiceMgr() {
        Slog.d(TAG, "onAmsAddedtoServiceMgr mIsPreNUpgrade = " + this.mIsPreNUpgrade);
        if (!CtaUtils.isCtaSupported() || !this.mIsPreNUpgrade) {
            return;
        }
        for (int userId : UserManagerService.getInstance().getUserIds()) {
            this.mDefaultPermissionPolicy.grantCtaPermToPreInstalledPackage(userId);
        }
    }

    private void scanCxpApp(File uniPath, String opFileName, int scanFlags) {
        File opFilePath = new File(uniPath, opFileName);
        List<String> appPathList = readPathsFromFile(opFilePath);
        for (int i = 0; i < appPathList.size(); i++) {
            String path = appPathList.get(i);
            File file = new File(path);
            int flag = path.contains("removable") ? 8192 : 65;
            long startScanTime = SystemClock.uptimeMillis();
            Slog.d(TAG, "scan package: " + file.toString() + " , start at: " + startScanTime + "ms.");
            try {
                scanPackageLI(file, flag | 4, scanFlags, 0L, (UserHandle) null);
            } catch (PackageManagerException e) {
                Slog.w(TAG, "Failed to parse " + file + ": " + e.getMessage());
            }
            long endScanTime = SystemClock.uptimeMillis();
            Slog.d(TAG, "scan package: " + file.toString() + " , end at: " + endScanTime + "ms. elapsed time = " + (endScanTime - startScanTime) + "ms.");
        }
    }

    private List<String> readPathsFromFile(File packagePathsFile) {
        int length = (int) packagePathsFile.length();
        byte[] bArr = new byte[length];
        List<String> fileContents = new ArrayList<>();
        try {
            FileInputStream inputStream = new FileInputStream(packagePathsFile);
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                while (true) {
                    String receiveString = bufferedReader.readLine();
                    if (receiveString == null) {
                        break;
                    }
                    fileContents.add(receiveString);
                }
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e2) {
            Log.e(TAG, "Can not read file: " + e2.toString());
        }
        return fileContents;
    }
}
