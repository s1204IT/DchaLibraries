package com.android.server.pm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.admin.IDevicePolicyManager;
import android.app.backup.IBackupManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
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
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstrumentationInfo;
import android.content.pm.KeySet;
import android.content.pm.ManifestDigest;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageStats;
import android.content.pm.PackageUserState;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerificationParams;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.VerifierInfo;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.net.Uri;
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
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
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
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.os.IParcelFileDescriptorFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.am.ProcessList;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import dalvik.system.DexFile;
import dalvik.system.StaleDexCacheError;
import dalvik.system.VMRuntime;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlSerializer;

public class PackageManagerService extends IPackageManager.Stub {
    private static final int BLUETOOTH_UID = 1002;
    static final int BROADCAST_DELAY = 10000;
    static final int CHECK_PENDING_VERIFICATION = 16;
    private static final boolean DEBUG_ABI_SELECTION = false;
    private static final boolean DEBUG_BROADCASTS = false;
    private static final boolean DEBUG_DEXOPT = false;
    private static final boolean DEBUG_FILTERS = false;
    private static final boolean DEBUG_INSTALL = false;
    private static final boolean DEBUG_INTENT_MATCHING = false;
    private static final boolean DEBUG_PACKAGE_INFO = false;
    private static final boolean DEBUG_PACKAGE_SCANNING = false;
    static final boolean DEBUG_PREFERRED = false;
    private static final boolean DEBUG_REMOVE = false;
    static final boolean DEBUG_SD_INSTALL = false;
    static final boolean DEBUG_SETTINGS = false;
    private static final boolean DEBUG_SHOW_INFO = false;
    static final boolean DEBUG_UPGRADE = false;
    private static final boolean DEBUG_VERIFY = false;
    private static final long DEFAULT_MANDATORY_FSTRIM_INTERVAL = 259200000;
    private static final int DEFAULT_VERIFICATION_RESPONSE = 1;
    private static final long DEFAULT_VERIFICATION_TIMEOUT = 10000;
    private static final boolean DEFAULT_VERIFY_ENABLE = true;
    static final int DEX_OPT_DEFERRED = 2;
    static final int DEX_OPT_FAILED = -1;
    static final int DEX_OPT_PERFORMED = 1;
    static final int DEX_OPT_SKIPPED = 0;
    static final int END_COPY = 4;
    static final int FIND_INSTALL_LOC = 8;
    private static final String IDMAP_PREFIX = "/data/resource-cache/";
    private static final String IDMAP_SUFFIX = "@idmap";
    static final int INIT_COPY = 5;
    private static final String INSTALL_PACKAGE_SUFFIX = "-";
    private static final int LOG_UID = 1007;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    static final int MCS_BOUND = 3;
    static final int MCS_GIVE_UP = 11;
    static final int MCS_RECONNECT = 10;
    static final int MCS_UNBIND = 6;
    private static final int NFC_UID = 1027;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    static final int PACKAGE_VERIFIED = 15;
    static final int POST_INSTALL = 9;
    private static final int RADIO_UID = 1001;
    static final int REMOVE_CHATTY = 65536;
    static final int SCAN_BOOTING = 256;
    static final int SCAN_DEFER_DEX = 128;
    static final int SCAN_DELETE_DATA_ON_FAILURES = 1024;
    static final int SCAN_FORCE_DEX = 4;
    static final int SCAN_NEW_INSTALL = 16;
    static final int SCAN_NO_DEX = 2;
    static final int SCAN_NO_PATHS = 32;
    static final int SCAN_REPLACING = 2048;
    static final int SCAN_TRUSTED_OVERLAY = 512;
    static final int SCAN_UPDATE_SIGNATURE = 8;
    static final int SCAN_UPDATE_TIME = 64;
    private static final String SD_ENCRYPTION_ALGORITHM = "AES";
    private static final String SD_ENCRYPTION_KEYSTORE_NAME = "AppsOnSD";
    static final int SEND_PENDING_BROADCAST = 1;
    private static final int SHELL_UID = 2000;
    private static final int SMARTCARD_UID = 1036;
    static final int START_CLEANING_PACKAGE = 7;
    static final String TAG = "PackageManager";
    static final int UPDATED_MEDIA_STATUS = 12;
    static final int UPDATE_PERMISSIONS_ALL = 1;
    static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private static final String VENDOR_OVERLAY_DIR = "/vendor/overlay";
    private static final long WATCHDOG_TIMEOUT = 600000;
    static final int WRITE_PACKAGE_RESTRICTIONS = 14;
    static final int WRITE_SETTINGS = 13;
    static final int WRITE_SETTINGS_DELAY = 10000;
    private static String sPreferredInstructionSet;
    static UserManagerService sUserManager;
    ApplicationInfo mAndroidApplication;
    final File mAppDataDir;
    final File mAppInstallDir;
    private File mAppLib32InstallDir;
    final String mAsecInternalPath;
    final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    final Context mContext;
    ComponentName mCustomResolverComponentName;
    final int mDefParseFlags;
    final long mDexOptLRUThresholdInMills;
    final File mDrmAppPrivateInstallDir;
    final boolean mFactoryTest;
    boolean mFoundPolicyFile;
    final int[] mGlobalGids;
    final PackageHandler mHandler;
    final ServiceThread mHandlerThread;
    volatile boolean mHasSystemUidErrors;
    final Installer mInstaller;
    final PackageInstallerService mInstallerService;
    final boolean mIsUpgrade;
    final boolean mLazyDexOpt;
    final DisplayMetrics mMetrics;
    final boolean mOnlyCore;
    PackageParser.Package mPlatformPackage;
    private ArrayList<Message> mPostSystemReadyMessages;
    private final String mRequiredVerifierPackage;
    ComponentName mResolveComponentName;
    boolean mRestoredSettings;
    volatile boolean mSafeMode;
    final String[] mSeparateProcesses;
    final Settings mSettings;
    final SparseArray<ArraySet<String>> mSystemPermissions;
    volatile boolean mSystemReady;
    final File mUserAppDataDir;
    static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    private static final Comparator<ResolveInfo> mResolvePrioritySorter = new Comparator<ResolveInfo>() {
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
                return v12 <= v22 ? 1 : -1;
            }
            if (r1.isDefault != r2.isDefault) {
                return !r1.isDefault ? 1 : -1;
            }
            int v13 = r1.match;
            int v23 = r2.match;
            if (v13 != v23) {
                return v13 <= v23 ? 1 : -1;
            }
            if (r1.system != r2.system) {
                return !r1.system ? 1 : -1;
            }
            return 0;
        }
    };
    private static final Comparator<ProviderInfo> mProviderInitOrderSorter = new Comparator<ProviderInfo>() {
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
    final int mSdkVersion = Build.VERSION.SDK_INT;
    final Object mInstallLock = new Object();
    final ArrayMap<String, PackageParser.Package> mPackages = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, PackageParser.Package>> mOverlays = new ArrayMap<>();
    private boolean mShouldRestoreconData = SELinuxMMAC.shouldRestorecon();
    final ArrayMap<String, SharedLibraryEntry> mSharedLibraries = new ArrayMap<>();
    final ActivityIntentResolver mActivities = new ActivityIntentResolver();
    final ActivityIntentResolver mReceivers = new ActivityIntentResolver();
    final ServiceIntentResolver mServices = new ServiceIntentResolver();
    final ProviderIntentResolver mProviders = new ProviderIntentResolver();
    final ArrayMap<String, PackageParser.Provider> mProvidersByAuthority = new ArrayMap<>();
    final ArrayMap<ComponentName, PackageParser.Instrumentation> mInstrumentation = new ArrayMap<>();
    final ArrayMap<String, PackageParser.PermissionGroup> mPermissionGroups = new ArrayMap<>();
    final ArraySet<String> mTransferedPackages = new ArraySet<>();
    final ArraySet<String> mProtectedBroadcasts = new ArraySet<>();
    final SparseArray<PackageVerificationState> mPendingVerification = new SparseArray<>();
    final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap<>();
    ArraySet<PackageParser.Package> mDeferredDexOpt = null;
    SparseBooleanArray mUserNeedsBadging = new SparseBooleanArray();
    private int mPendingVerificationToken = 0;
    final ActivityInfo mResolveActivity = new ActivityInfo();
    final ResolveInfo mResolveInfo = new ResolveInfo();
    boolean mResolverReplaced = false;
    final PendingPackageBroadcasts mPendingBroadcasts = new PendingPackageBroadcasts();
    private IMediaContainerService mContainerService = null;
    private ArraySet<Integer> mDirtyUsers = new ArraySet<>();
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    final SparseArray<PostInstallData> mRunningInstalls = new SparseArray<>();
    int mNextInstallToken = 1;
    private final PackageUsage mPackageUsage = new PackageUsage();
    private boolean mMediaMounted = false;

    static int access$3008(PackageManagerService x0) {
        int i = x0.mPendingVerificationToken;
        x0.mPendingVerificationToken = i + 1;
        return i;
    }

    public static final class SharedLibraryEntry {
        public final String apk;
        public final String path;

        SharedLibraryEntry(String _path, String _apk) {
            this.path = _path;
            this.apk = _apk;
        }
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
            if (packages != null) {
                packages.remove(packageName);
            }
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
            IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
            PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(3, imcs));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    class PostInstallData {
        public InstallArgs args;
        public PackageInstalledInfo res;

        PostInstallData(InstallArgs _a, PackageInstalledInfo _r) {
            this.args = _a;
            this.res = _r;
        }
    }

    private class PackageUsage {
        private static final int WRITE_INTERVAL = 1800000;
        private final AtomicBoolean mBackgroundWriteRunning;
        private final Object mFileLock;
        private boolean mIsHistoricalPackageUsageAvailable;
        private final AtomicLong mLastWritten;

        private PackageUsage() {
            this.mFileLock = new Object();
            this.mLastWritten = new AtomicLong(0L);
            this.mBackgroundWriteRunning = new AtomicBoolean(false);
            this.mIsHistoricalPackageUsageAvailable = PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        boolean isHistoricalPackageUsageAvailable() {
            return this.mIsHistoricalPackageUsageAvailable;
        }

        void write(boolean force) {
            if (force) {
                writeInternal();
            } else if (SystemClock.elapsedRealtime() - this.mLastWritten.get() >= 1800000 && this.mBackgroundWriteRunning.compareAndSet(false, PackageManagerService.DEFAULT_VERIFY_ENABLE)) {
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
                        for (PackageParser.Package pkg : PackageManagerService.this.mPackages.values()) {
                            if (pkg.mLastPackageUsageTimeInMills != 0) {
                                sb.setLength(0);
                                sb.append(pkg.packageName);
                                sb.append(' ');
                                sb.append(pkg.mLastPackageUsageTimeInMills);
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
                BufferedInputStream in2 = null;
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
                    while (true) {
                        String packageName = readToken(in, sb, ' ');
                        if (packageName == null) {
                            IoUtils.closeQuietly(in);
                            in2 = in;
                            break;
                        }
                        String timeInMillisString = readToken(in, sb, '\n');
                        if (timeInMillisString == null) {
                            throw new IOException("Failed to find last usage time for package " + packageName);
                        }
                        PackageParser.Package pkg = PackageManagerService.this.mPackages.get(packageName);
                        if (pkg != null) {
                            try {
                                long timeInMillis = Long.parseLong(timeInMillisString.toString());
                                pkg.mLastPackageUsageTimeInMills = timeInMillis;
                            } catch (NumberFormatException e3) {
                                throw new IOException("Failed to parse " + timeInMillisString + " as a long.", e3);
                            }
                        }
                    }
                } catch (FileNotFoundException e4) {
                    in2 = in;
                    this.mIsHistoricalPackageUsageAvailable = false;
                    IoUtils.closeQuietly(in2);
                    this.mLastWritten.set(SystemClock.elapsedRealtime());
                } catch (IOException e5) {
                    e = e5;
                    in2 = in;
                    Log.w(PackageManagerService.TAG, "Failed to read package usage times", e);
                    IoUtils.closeQuietly(in2);
                    this.mLastWritten.set(SystemClock.elapsedRealtime());
                } catch (Throwable th2) {
                    th = th2;
                    in2 = in;
                    IoUtils.closeQuietly(in2);
                    throw th;
                }
            }
            this.mLastWritten.set(SystemClock.elapsedRealtime());
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
            Intent service = new Intent().setComponent(PackageManagerService.DEFAULT_CONTAINER_COMPONENT);
            Process.setThreadPriority(0);
            if (PackageManagerService.this.mContext.bindServiceAsUser(service, PackageManagerService.this.mDefContainerConn, 1, UserHandle.OWNER)) {
                Process.setThreadPriority(10);
                this.mBound = PackageManagerService.DEFAULT_VERIFY_ENABLE;
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            Process.setThreadPriority(10);
            return false;
        }

        private void disconnectService() {
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
        public void handleMessage(Message msg) {
            try {
                doHandleMessage(msg);
            } finally {
                Process.setThreadPriority(10);
            }
        }

        void doHandleMessage(Message msg) {
            int ret;
            int[] firstUsers;
            switch (msg.what) {
                case 1:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        if (PackageManagerService.this.mPendingBroadcasts != null) {
                            int size = PackageManagerService.this.mPendingBroadcasts.size();
                            if (size > 0) {
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
                                    PackageManagerService.this.sendPackageChangedBroadcast(packages[i2], PackageManagerService.DEFAULT_VERIFY_ENABLE, components[i2], uids[i2]);
                                }
                                Process.setThreadPriority(10);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                case 2:
                case 4:
                case 8:
                default:
                    return;
                case 3:
                    if (msg.obj != null) {
                        PackageManagerService.this.mContainerService = (IMediaContainerService) msg.obj;
                    }
                    if (PackageManagerService.this.mContainerService == null) {
                        Slog.e(PackageManagerService.TAG, "Cannot bind to media container service");
                        Iterator<HandlerParams> it2 = this.mPendingInstalls.iterator();
                        while (it2.hasNext()) {
                            it2.next().serviceError();
                        }
                        this.mPendingInstalls.clear();
                        return;
                    }
                    if (this.mPendingInstalls.size() > 0) {
                        HandlerParams params = this.mPendingInstalls.get(0);
                        if (params != null && params.startCopy()) {
                            if (this.mPendingInstalls.size() > 0) {
                                this.mPendingInstalls.remove(0);
                            }
                            if (this.mPendingInstalls.size() == 0) {
                                if (this.mBound) {
                                    removeMessages(6);
                                    Message ubmsg = obtainMessage(6);
                                    sendMessageDelayed(ubmsg, PackageManagerService.DEFAULT_VERIFICATION_TIMEOUT);
                                    return;
                                }
                                return;
                            }
                            PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            return;
                        }
                        return;
                    }
                    Slog.w(PackageManagerService.TAG, "Empty queue");
                    return;
                case 5:
                    HandlerParams params2 = (HandlerParams) msg.obj;
                    int idx = this.mPendingInstalls.size();
                    if (!this.mBound) {
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                            params2.serviceError();
                            return;
                        } else {
                            this.mPendingInstalls.add(idx, params2);
                            return;
                        }
                    }
                    this.mPendingInstalls.add(idx, params2);
                    if (idx == 0) {
                        PackageManagerService.this.mHandler.sendEmptyMessage(3);
                        return;
                    }
                    return;
                case 6:
                    if (this.mPendingInstalls.size() == 0 && PackageManagerService.this.mPendingVerification.size() == 0) {
                        if (this.mBound) {
                            disconnectService();
                            return;
                        }
                        return;
                    } else {
                        if (this.mPendingInstalls.size() > 0) {
                            PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            return;
                        }
                        return;
                    }
                case 7:
                    Process.setThreadPriority(0);
                    String packageName = (String) msg.obj;
                    int userId = msg.arg1;
                    boolean andCode = msg.arg2 != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
                    synchronized (PackageManagerService.this.mPackages) {
                        if (userId == -1) {
                            int[] users = PackageManagerService.sUserManager.getUserIds();
                            for (int i3 : users) {
                                PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(i3, packageName, andCode));
                            }
                        } else {
                            PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(userId, packageName, andCode));
                        }
                        break;
                    }
                    Process.setThreadPriority(10);
                    PackageManagerService.this.startCleaningPackages();
                    return;
                case 9:
                    PostInstallData data = PackageManagerService.this.mRunningInstalls.get(msg.arg1);
                    PackageManagerService.this.mRunningInstalls.delete(msg.arg1);
                    boolean deleteOld = false;
                    if (data != null) {
                        InstallArgs args = data.args;
                        PackageInstalledInfo res = data.res;
                        if (res.returnCode == 1) {
                            res.removedInfo.sendBroadcast(false, PackageManagerService.DEFAULT_VERIFY_ENABLE, false);
                            Bundle extras = new Bundle(1);
                            extras.putInt("android.intent.extra.UID", res.uid);
                            int[] updateUsers = new int[0];
                            if (res.origUsers == null || res.origUsers.length == 0) {
                                firstUsers = res.newUsers;
                            } else {
                                firstUsers = new int[0];
                                for (int i4 = 0; i4 < res.newUsers.length; i4++) {
                                    int user = res.newUsers[i4];
                                    boolean isNew = PackageManagerService.DEFAULT_VERIFY_ENABLE;
                                    int j = 0;
                                    while (true) {
                                        if (j < res.origUsers.length) {
                                            if (res.origUsers[j] != user) {
                                                j++;
                                            } else {
                                                isNew = false;
                                            }
                                        }
                                    }
                                    if (isNew) {
                                        int[] newFirst = new int[firstUsers.length + 1];
                                        System.arraycopy(firstUsers, 0, newFirst, 0, firstUsers.length);
                                        newFirst[firstUsers.length] = user;
                                        firstUsers = newFirst;
                                    } else {
                                        int[] newUpdate = new int[updateUsers.length + 1];
                                        System.arraycopy(updateUsers, 0, newUpdate, 0, updateUsers.length);
                                        newUpdate[updateUsers.length] = user;
                                        updateUsers = newUpdate;
                                    }
                                }
                            }
                            PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", res.pkg.applicationInfo.packageName, extras, null, null, firstUsers);
                            boolean update = res.removedInfo.removedPackage != null ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
                            if (update) {
                                extras.putBoolean("android.intent.extra.REPLACING", PackageManagerService.DEFAULT_VERIFY_ENABLE);
                            }
                            PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", res.pkg.applicationInfo.packageName, extras, null, null, updateUsers);
                            if (update) {
                                PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", res.pkg.applicationInfo.packageName, extras, null, null, updateUsers);
                                PackageManagerService.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, res.pkg.applicationInfo.packageName, null, updateUsers);
                                if (PackageManagerService.isForwardLocked(res.pkg) || PackageManagerService.isExternal(res.pkg)) {
                                    int[] uidArray = {res.pkg.applicationInfo.uid};
                                    ArrayList<String> pkgList = new ArrayList<>(1);
                                    pkgList.add(res.pkg.applicationInfo.packageName);
                                    PackageManagerService.this.sendResourcesChangedBroadcast(PackageManagerService.DEFAULT_VERIFY_ENABLE, PackageManagerService.DEFAULT_VERIFY_ENABLE, pkgList, uidArray, null);
                                }
                            }
                            if (res.removedInfo.args != null) {
                                deleteOld = PackageManagerService.DEFAULT_VERIFY_ENABLE;
                            }
                            EventLog.writeEvent(EventLogTags.UNKNOWN_SOURCES_ENABLED, PackageManagerService.this.getUnknownSourcesSettings());
                        }
                        Runtime.getRuntime().gc();
                        if (deleteOld) {
                            synchronized (PackageManagerService.this.mInstallLock) {
                                res.removedInfo.args.doPostDeleteLI(PackageManagerService.DEFAULT_VERIFY_ENABLE);
                                break;
                            }
                        }
                        if (args.observer != null) {
                            try {
                                args.observer.onPackageInstalled(res.name, res.returnCode, res.returnMsg, PackageManagerService.this.extrasForInstallResult(res));
                                return;
                            } catch (RemoteException e) {
                                Slog.i(PackageManagerService.TAG, "Observer no longer exists.");
                                return;
                            }
                        }
                        return;
                    }
                    Slog.e(PackageManagerService.TAG, "Bogus post-install token " + msg.arg1);
                    return;
                case 10:
                    if (this.mPendingInstalls.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                            Iterator<HandlerParams> it3 = this.mPendingInstalls.iterator();
                            while (it3.hasNext()) {
                                it3.next().serviceError();
                            }
                            this.mPendingInstalls.clear();
                            return;
                        }
                        return;
                    }
                    return;
                case 11:
                    this.mPendingInstalls.remove(0);
                    return;
                case 12:
                    boolean reportStatus = msg.arg1 == 1 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
                    boolean doGc = msg.arg2 == 1 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
                    if (doGc) {
                        Runtime.getRuntime().gc();
                    }
                    if (msg.obj != null) {
                        PackageManagerService.this.unloadAllContainers((Set) msg.obj);
                    }
                    if (reportStatus) {
                        try {
                            PackageHelper.getMountService().finishMediaUpdate();
                            return;
                        } catch (RemoteException e2) {
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
                        break;
                    }
                    Process.setThreadPriority(10);
                    return;
                case 14:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(14);
                        Iterator i$ = PackageManagerService.this.mDirtyUsers.iterator();
                        while (i$.hasNext()) {
                            PackageManagerService.this.mSettings.writePackageRestrictionsLPr(((Integer) i$.next()).intValue());
                        }
                        PackageManagerService.this.mDirtyUsers.clear();
                        break;
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
                        InstallArgs args2 = state.getInstallArgs();
                        Uri originUri = Uri.fromFile(args2.origin.resolvedFile);
                        if (state.isInstallAllowed()) {
                            ret = -110;
                            PackageManagerService.this.broadcastPackageVerified(verificationId, originUri, response.code, state.getInstallArgs().getUser());
                            try {
                                ret = args2.copyApk(PackageManagerService.this.mContainerService, PackageManagerService.DEFAULT_VERIFY_ENABLE);
                            } catch (RemoteException e3) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                            }
                            break;
                        } else {
                            ret = -22;
                        }
                        PackageManagerService.this.processPendingInstall(args2, ret);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        return;
                    }
                    return;
                case 16:
                    int verificationId2 = msg.arg1;
                    PackageVerificationState state2 = PackageManagerService.this.mPendingVerification.get(verificationId2);
                    if (state2 != null && !state2.timeoutExtended()) {
                        InstallArgs args3 = state2.getInstallArgs();
                        Uri originUri2 = Uri.fromFile(args3.origin.resolvedFile);
                        Slog.i(PackageManagerService.TAG, "Verification timed out for " + originUri2);
                        PackageManagerService.this.mPendingVerification.remove(verificationId2);
                        int ret2 = -22;
                        if (PackageManagerService.this.getDefaultVerificationResponse() != 1) {
                            PackageManagerService.this.broadcastPackageVerified(verificationId2, originUri2, -1, state2.getInstallArgs().getUser());
                        } else {
                            Slog.i(PackageManagerService.TAG, "Continuing with installation of " + originUri2);
                            state2.setVerifierResponse(Binder.getCallingUid(), 2);
                            PackageManagerService.this.broadcastPackageVerified(verificationId2, originUri2, 1, state2.getInstallArgs().getUser());
                            try {
                                ret2 = args3.copyApk(PackageManagerService.this.mContainerService, PackageManagerService.DEFAULT_VERIFY_ENABLE);
                            } catch (RemoteException e4) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                            }
                            break;
                        }
                        PackageManagerService.this.processPendingInstall(args3, ret2);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        return;
                    }
                    return;
            }
        }
    }

    Bundle extrasForInstallResult(PackageInstalledInfo res) {
        switch (res.returnCode) {
            case -112:
                Bundle extras = new Bundle();
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION", res.origPermission);
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE", res.origPackage);
                return extras;
            default:
                return null;
        }
    }

    void scheduleWriteSettingsLocked() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessageDelayed(13, DEFAULT_VERIFICATION_TIMEOUT);
        }
    }

    void scheduleWritePackageRestrictionsLocked(int userId) {
        if (sUserManager.exists(userId)) {
            this.mDirtyUsers.add(Integer.valueOf(userId));
            if (!this.mHandler.hasMessages(14)) {
                this.mHandler.sendEmptyMessageDelayed(14, DEFAULT_VERIFICATION_TIMEOUT);
            }
        }
    }

    public static final PackageManagerService main(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        ?? packageManagerService = new PackageManagerService(context, installer, factoryTest, onlyCore);
        ServiceManager.addService("package", (IBinder) packageManagerService);
        return packageManagerService;
    }

    static String[] splitString(String str, char sep) {
        int count = 1;
        int i = 0;
        while (true) {
            int i2 = str.indexOf(sep, i);
            if (i2 < 0) {
                break;
            }
            count++;
            i = i2 + 1;
        }
        String[] res = new String[count];
        int i3 = 0;
        int count2 = 0;
        int lastI = 0;
        while (true) {
            int i4 = str.indexOf(sep, i3);
            if (i4 >= 0) {
                res[count2] = str.substring(lastI, i4);
                count2++;
                i3 = i4 + 1;
                lastI = i3;
            } else {
                res[count2] = str.substring(lastI, str.length());
                return res;
            }
        }
    }

    private static void getDefaultDisplayMetrics(Context context, DisplayMetrics metrics) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        displayManager.getDisplay(0).getMetrics(metrics);
    }

    public PackageManagerService(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        long dexOptLRUThresholdInMinutes;
        int reparseFlags;
        String msg;
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START, SystemClock.uptimeMillis());
        if (this.mSdkVersion <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }
        this.mContext = context;
        this.mFactoryTest = factoryTest;
        this.mOnlyCore = onlyCore;
        this.mLazyDexOpt = "eng".equals(SystemProperties.get("ro.build.type"));
        this.mMetrics = new DisplayMetrics();
        this.mSettings = new Settings(context);
        this.mSettings.addSharedUserLPw("android.uid.system", 1000, 1073741825);
        this.mSettings.addSharedUserLPw("android.uid.phone", RADIO_UID, 1073741825);
        this.mSettings.addSharedUserLPw("android.uid.log", LOG_UID, 1073741825);
        this.mSettings.addSharedUserLPw("android.uid.nfc", NFC_UID, 1073741825);
        this.mSettings.addSharedUserLPw("android.uid.bluetooth", BLUETOOTH_UID, 1073741825);
        this.mSettings.addSharedUserLPw("android.uid.shell", SHELL_UID, 1073741825);
        if (this.mLazyDexOpt) {
            dexOptLRUThresholdInMinutes = 30;
        } else {
            dexOptLRUThresholdInMinutes = 10080;
        }
        this.mDexOptLRUThresholdInMills = 60 * dexOptLRUThresholdInMinutes * 1000;
        String separateProcesses = SystemProperties.get("debug.separate_processes");
        if (separateProcesses != null && separateProcesses.length() > 0) {
            if ("*".equals(separateProcesses)) {
                this.mDefParseFlags = 8;
                this.mSeparateProcesses = null;
                Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
            } else {
                this.mDefParseFlags = 0;
                this.mSeparateProcesses = separateProcesses.split(",");
                Slog.w(TAG, "Running with debug.separate_processes: " + separateProcesses);
            }
        } else {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = null;
        }
        this.mInstaller = installer;
        getDefaultDisplayMetrics(context, this.mMetrics);
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mGlobalGids = systemConfig.getGlobalGids();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mAvailableFeatures = systemConfig.getAvailableFeatures();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                this.mHandlerThread = new ServiceThread(TAG, 10, DEFAULT_VERIFY_ENABLE);
                this.mHandlerThread.start();
                this.mHandler = new PackageHandler(this.mHandlerThread.getLooper());
                Watchdog.getInstance().addThread(this.mHandler, 600000L);
                File dataDir = Environment.getDataDirectory();
                this.mAppDataDir = new File(dataDir, DatabaseHelper.SoundModelContract.KEY_DATA);
                this.mAppInstallDir = new File(dataDir, "app");
                this.mAppLib32InstallDir = new File(dataDir, "app-lib");
                this.mAsecInternalPath = new File(dataDir, "app-asec").getPath();
                this.mUserAppDataDir = new File(dataDir, "user");
                this.mDrmAppPrivateInstallDir = new File(dataDir, "app-private");
                sUserManager = new UserManagerService(context, this, this.mInstallLock, this.mPackages);
                ArrayMap<String, SystemConfig.PermissionEntry> permConfig = systemConfig.getPermissions();
                for (int i = 0; i < permConfig.size(); i++) {
                    SystemConfig.PermissionEntry perm = permConfig.valueAt(i);
                    BasePermission bp = this.mSettings.mPermissions.get(perm.name);
                    if (bp == null) {
                        bp = new BasePermission(perm.name, "android", 1);
                        this.mSettings.mPermissions.put(perm.name, bp);
                    }
                    if (perm.gids != null) {
                        bp.gids = appendInts(bp.gids, perm.gids);
                    }
                }
                ArrayMap<String, String> libConfig = systemConfig.getSharedLibraries();
                for (int i2 = 0; i2 < libConfig.size(); i2++) {
                    this.mSharedLibraries.put(libConfig.keyAt(i2), new SharedLibraryEntry(libConfig.valueAt(i2), null));
                }
                this.mFoundPolicyFile = SELinuxMMAC.readInstallPolicy();
                this.mRestoredSettings = this.mSettings.readLPw(this, sUserManager.getUsers(false), this.mSdkVersion, this.mOnlyCore);
                String customResolverActivity = Resources.getSystem().getString(R.string.config_systemSettingsIntelligence);
                if (!TextUtils.isEmpty(customResolverActivity)) {
                    this.mCustomResolverComponentName = ComponentName.unflattenFromString(customResolverActivity);
                }
                long startTime = SystemClock.uptimeMillis();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, startTime);
                ArraySet<String> alreadyDexOpted = new ArraySet<>();
                String bootClassPath = System.getenv("BOOTCLASSPATH");
                String systemServerClassPath = System.getenv("SYSTEMSERVERCLASSPATH");
                if (bootClassPath != null) {
                    String[] bootClassPathElements = splitString(bootClassPath, ':');
                    for (String element : bootClassPathElements) {
                        alreadyDexOpted.add(element);
                    }
                } else {
                    Slog.w(TAG, "No BOOTCLASSPATH found!");
                }
                if (systemServerClassPath != null) {
                    String[] systemServerClassPathElements = splitString(systemServerClassPath, ':');
                    for (String element2 : systemServerClassPathElements) {
                        alreadyDexOpted.add(element2);
                    }
                } else {
                    Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
                }
                List<String> allInstructionSets = getAllInstructionSets();
                String[] dexCodeInstructionSets = getDexCodeInstructionSets((String[]) allInstructionSets.toArray(new String[allInstructionSets.size()]));
                if (this.mSharedLibraries.size() > 0) {
                    for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                        for (SharedLibraryEntry libEntry : this.mSharedLibraries.values()) {
                            String lib = libEntry.path;
                            if (lib != null) {
                                try {
                                    byte dexoptRequired = DexFile.isDexOptNeededInternal(lib, null, dexCodeInstructionSet, false);
                                    if (dexoptRequired != 0) {
                                        alreadyDexOpted.add(lib);
                                        if (dexoptRequired == 2) {
                                            this.mInstaller.dexopt(lib, 1000, DEFAULT_VERIFY_ENABLE, dexCodeInstructionSet);
                                        } else {
                                            this.mInstaller.patchoat(lib, 1000, DEFAULT_VERIFY_ENABLE, dexCodeInstructionSet);
                                        }
                                    }
                                } catch (FileNotFoundException e) {
                                    Slog.w(TAG, "Library not found: " + lib);
                                } catch (IOException e2) {
                                    Slog.w(TAG, "Cannot dexopt " + lib + "; is it an APK or JAR? " + e2.getMessage());
                                }
                            }
                        }
                    }
                }
                File frameworkDir = new File(Environment.getRootDirectory(), "framework");
                alreadyDexOpted.add(frameworkDir.getPath() + "/framework-res.apk");
                alreadyDexOpted.add(frameworkDir.getPath() + "/core-libart.jar");
                String[] frameworkFiles = frameworkDir.list();
                if (frameworkFiles != null) {
                    for (String dexCodeInstructionSet2 : dexCodeInstructionSets) {
                        for (String str : frameworkFiles) {
                            File libPath = new File(frameworkDir, str);
                            String path = libPath.getPath();
                            if (!alreadyDexOpted.contains(path) && (path.endsWith(".apk") || path.endsWith(".jar"))) {
                                try {
                                    try {
                                        byte dexoptRequired2 = DexFile.isDexOptNeededInternal(path, null, dexCodeInstructionSet2, false);
                                        if (dexoptRequired2 == 2) {
                                            this.mInstaller.dexopt(path, 1000, DEFAULT_VERIFY_ENABLE, dexCodeInstructionSet2);
                                        } else if (dexoptRequired2 == 1) {
                                            this.mInstaller.patchoat(path, 1000, DEFAULT_VERIFY_ENABLE, dexCodeInstructionSet2);
                                        }
                                    } catch (FileNotFoundException e3) {
                                        Slog.w(TAG, "Jar not found: " + path);
                                    }
                                } catch (IOException e4) {
                                    Slog.w(TAG, "Exception reading jar: " + path, e4);
                                }
                            }
                        }
                    }
                }
                File vendorOverlayDir = new File(VENDOR_OVERLAY_DIR);
                scanDirLI(vendorOverlayDir, 65, 928, 0L);
                scanDirLI(frameworkDir, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS, 418, 0L);
                File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
                scanDirLI(privilegedAppDir, HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS, 416, 0L);
                File systemAppDir = new File(Environment.getRootDirectory(), "app");
                scanDirLI(systemAppDir, 65, 416, 0L);
                File vendorAppDir = new File("/vendor/app");
                try {
                    vendorAppDir = vendorAppDir.getCanonicalFile();
                } catch (IOException e5) {
                }
                scanDirLI(vendorAppDir, 65, 416, 0L);
                File oemAppDir = new File(Environment.getOemDirectory(), "app");
                scanDirLI(oemAppDir, 65, 416, 0L);
                this.mInstaller.moveFiles();
                List<String> possiblyDeletedUpdatedSystemApps = new ArrayList<>();
                ArrayMap<String, File> expectingBetter = new ArrayMap<>();
                if (!this.mOnlyCore) {
                    Iterator<PackageSetting> psit = this.mSettings.mPackages.values().iterator();
                    while (psit.hasNext()) {
                        PackageSetting ps = psit.next();
                        if ((ps.pkgFlags & 1) != 0) {
                            PackageParser.Package scannedPkg = this.mPackages.get(ps.name);
                            if (scannedPkg != null) {
                                if (this.mSettings.isDisabledSystemPackageLPr(ps.name)) {
                                    logCriticalInfo(5, "Expecting better updated system app for " + ps.name + "; removing system app.  Last known codePath=" + ps.codePathString + ", installStatus=" + ps.installStatus + ", versionCode=" + ps.versionCode + "; scanned versionCode=" + scannedPkg.mVersionCode);
                                    removePackageLI(ps, DEFAULT_VERIFY_ENABLE);
                                    expectingBetter.put(ps.name, ps.codePath);
                                }
                            } else if (!this.mSettings.isDisabledSystemPackageLPr(ps.name)) {
                                psit.remove();
                                logCriticalInfo(5, "System package " + ps.name + " no longer exists; wiping its data");
                                removeDataDirsLI(ps.name);
                            } else {
                                PackageSetting disabledPs = this.mSettings.getDisabledSystemPkgLPr(ps.name);
                                if (disabledPs.codePath == null || !disabledPs.codePath.exists()) {
                                    possiblyDeletedUpdatedSystemApps.add(ps.name);
                                }
                            }
                        }
                    }
                }
                ArrayList<PackageSetting> deletePkgsList = this.mSettings.getListOfIncompleteInstallPackagesLPr();
                for (int i3 = 0; i3 < deletePkgsList.size(); i3++) {
                    cleanupInstallFailedPackage(deletePkgsList.get(i3));
                }
                deleteTempPackageFiles();
                this.mSettings.pruneSharedUsersLPw();
                if (!this.mOnlyCore) {
                    EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START, SystemClock.uptimeMillis());
                    scanDirLI(this.mAppInstallDir, 0, 416, 0L);
                    scanDirLI(this.mDrmAppPrivateInstallDir, 16, 416, 0L);
                    for (String deletedAppName : possiblyDeletedUpdatedSystemApps) {
                        PackageParser.Package deletedPkg = this.mPackages.get(deletedAppName);
                        this.mSettings.removeDisabledSystemPackageLPw(deletedAppName);
                        if (deletedPkg == null) {
                            msg = "Updated system package " + deletedAppName + " no longer exists; wiping its data";
                            removeDataDirsLI(deletedAppName);
                        } else {
                            msg = "Updated system app + " + deletedAppName + " no longer present; removing system privileges for " + deletedAppName;
                            deletedPkg.applicationInfo.flags &= -2;
                            PackageSetting deletedPs = this.mSettings.mPackages.get(deletedAppName);
                            deletedPs.pkgFlags &= -2;
                        }
                        logCriticalInfo(5, msg);
                    }
                    for (int i4 = 0; i4 < expectingBetter.size(); i4++) {
                        String packageName = expectingBetter.keyAt(i4);
                        if (!this.mPackages.containsKey(packageName)) {
                            File scanFile = expectingBetter.valueAt(i4);
                            logCriticalInfo(5, "Expected better " + packageName + " but never showed up; reverting to system");
                            if (FileUtils.contains(privilegedAppDir, scanFile)) {
                                reparseFlags = HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS;
                            } else if (FileUtils.contains(systemAppDir, scanFile) || FileUtils.contains(vendorAppDir, scanFile) || FileUtils.contains(oemAppDir, scanFile)) {
                                reparseFlags = 65;
                            } else {
                                Slog.e(TAG, "Ignoring unexpected fallback path " + scanFile);
                            }
                            this.mSettings.enableSystemPackageLPw(packageName);
                            try {
                                scanPackageLI(scanFile, reparseFlags, 416, 0L, (UserHandle) null);
                            } catch (PackageManagerException e6) {
                                Slog.e(TAG, "Failed to parse original system package: " + e6.getMessage());
                            }
                        }
                    }
                }
                updateAllSharedLibrariesLPw();
                for (SharedUserSetting setting : this.mSettings.getAllSharedUsersLPw()) {
                    adjustCpuAbisForSharedUserLPw(setting.packages, null, false, false);
                }
                this.mPackageUsage.readLP();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END, SystemClock.uptimeMillis());
                Slog.i(TAG, "Time to scan packages: " + ((SystemClock.uptimeMillis() - startTime) / 1000.0f) + " seconds");
                boolean regrantPermissions = this.mSettings.mInternalSdkPlatform != this.mSdkVersion ? DEFAULT_VERIFY_ENABLE : false;
                if (regrantPermissions) {
                    Slog.i(TAG, "Platform changed from " + this.mSettings.mInternalSdkPlatform + " to " + this.mSdkVersion + "; regranting permissions for internal storage");
                }
                this.mSettings.mInternalSdkPlatform = this.mSdkVersion;
                updatePermissionsLPw(null, null, (regrantPermissions ? 6 : 0) | 1);
                if (!this.mRestoredSettings && !onlyCore) {
                    this.mSettings.readDefaultPreferredAppsLPw(this, 0);
                }
                this.mIsUpgrade = !Build.FINGERPRINT.equals(this.mSettings.mFingerprint) ? DEFAULT_VERIFY_ENABLE : false;
                if (this.mIsUpgrade && !onlyCore) {
                    Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                    for (String pkgName : this.mSettings.mPackages.keySet()) {
                        deleteCodeCacheDirsLI(pkgName);
                    }
                    this.mSettings.mFingerprint = Build.FINGERPRINT;
                }
                this.mSettings.updateInternalDatabaseVersion();
                this.mSettings.writeLPr();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY, SystemClock.uptimeMillis());
                this.mRequiredVerifierPackage = getRequiredVerifierLPr();
            }
        }
        this.mInstallerService = new PackageInstallerService(context, this, this.mAppInstallDir);
        Runtime.getRuntime().gc();
    }

    public boolean isFirstBoot() {
        if (this.mRestoredSettings) {
            return false;
        }
        return DEFAULT_VERIFY_ENABLE;
    }

    public boolean isOnlyCoreApps() {
        return this.mOnlyCore;
    }

    public boolean isUpgrade() {
        return this.mIsUpgrade;
    }

    private String getRequiredVerifierLPr() {
        String packageName;
        PackageSetting ps;
        Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        List<ResolveInfo> receivers = queryIntentReceivers(verification, PACKAGE_MIME_TYPE, 512, 0);
        String requiredVerifier = null;
        int N = receivers.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo info = receivers.get(i);
            if (info.activityInfo != null && (ps = this.mSettings.mPackages.get((packageName = info.activityInfo.packageName))) != null) {
                GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
                if (!gp.grantedPermissions.contains("android.permission.PACKAGE_VERIFICATION_AGENT")) {
                    continue;
                } else {
                    if (requiredVerifier != null) {
                        throw new RuntimeException("There can be only one required verifier");
                    }
                    requiredVerifier = packageName;
                }
            }
        }
        return requiredVerifier;
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

    void cleanupInstallFailedPackage(PackageSetting ps) {
        logCriticalInfo(5, "Cleaning up incompletely installed app: " + ps.name);
        removeDataDirsLI(ps.name);
        if (ps.codePath != null) {
            if (ps.codePath.isDirectory()) {
                FileUtils.deleteContents(ps.codePath);
            }
            ps.codePath.delete();
        }
        if (ps.resourcePath != null && !ps.resourcePath.equals(ps.codePath)) {
            if (ps.resourcePath.isDirectory()) {
                FileUtils.deleteContents(ps.resourcePath);
            }
            ps.resourcePath.delete();
        }
        this.mSettings.removePackageLPw(ps.name);
    }

    static int[] appendInts(int[] cur, int[] add) {
        if (add != null) {
            if (cur == null) {
                return add;
            }
            for (int i : add) {
                cur = ArrayUtils.appendInt(cur, i);
            }
            return cur;
        }
        return cur;
    }

    static int[] removeInts(int[] cur, int[] rem) {
        if (rem != null && cur != null) {
            for (int i : rem) {
                cur = ArrayUtils.removeInt(cur, i);
            }
        }
        return cur;
    }

    PackageInfo generatePackageInfo(PackageParser.Package p, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId) || (ps = (PackageSetting) p.mExtras) == null) {
            return null;
        }
        GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
        PackageUserState state = ps.readUserState(userId);
        return PackageParser.generatePackageInfo(p, gp.gids, flags, ps.firstInstallTime, ps.lastUpdateTime, gp.grantedPermissions, state, userId);
    }

    public boolean isPackageAvailable(String packageName, int userId) {
        PackageSetting ps;
        PackageUserState state;
        boolean zIsAvailable = false;
        if (sUserManager.exists(userId)) {
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "is package available");
            synchronized (this.mPackages) {
                PackageParser.Package p = this.mPackages.get(packageName);
                if (p != null && (ps = (PackageSetting) p.mExtras) != null && (state = ps.readUserState(userId)) != null) {
                    zIsAvailable = PackageParser.isAvailable(state);
                }
            }
        }
        return zIsAvailable;
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        PackageInfo packageInfoGeneratePackageInfoFromSettingsLPw;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package info");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null) {
                packageInfoGeneratePackageInfoFromSettingsLPw = generatePackageInfo(p, flags, userId);
            } else {
                packageInfoGeneratePackageInfoFromSettingsLPw = (flags & DumpState.DUMP_INSTALLS) != 0 ? generatePackageInfoFromSettingsLPw(packageName, flags, userId) : null;
            }
        }
        return packageInfoGeneratePackageInfoFromSettingsLPw;
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

    public int getPackageUid(String packageName, int userId) {
        if (!sUserManager.exists(userId)) {
            return -1;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package uid");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null) {
                return UserHandle.getUid(userId, p.applicationInfo.uid);
            }
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null || ps.pkg == null || ps.pkg.applicationInfo == null) {
                return -1;
            }
            PackageParser.Package p2 = ps.pkg;
            return p2 != null ? UserHandle.getUid(userId, p2.applicationInfo.uid) : -1;
        }
    }

    public int[] getPackageGids(String packageName) {
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                return ps.getGids();
            }
            return new int[0];
        }
    }

    static final PermissionInfo generatePermissionInfo(BasePermission bp, int flags) {
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
        PermissionInfo permissionInfoGeneratePermissionInfo;
        synchronized (this.mPackages) {
            BasePermission p = this.mSettings.mPermissions.get(name);
            permissionInfoGeneratePermissionInfo = p != null ? generatePermissionInfo(p, flags) : null;
        }
        return permissionInfoGeneratePermissionInfo;
    }

    public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) {
        ArrayList<PermissionInfo> out;
        synchronized (this.mPackages) {
            out = new ArrayList<>(10);
            for (BasePermission p : this.mSettings.mPermissions.values()) {
                if (group == null) {
                    if (p.perm == null || p.perm.info.group == null) {
                        out.add(generatePermissionInfo(p, flags));
                    }
                } else if (p.perm != null && group.equals(p.perm.info.group)) {
                    out.add(PackageParser.generatePermissionInfo(p.perm, flags));
                }
            }
            if (out.size() <= 0 && !this.mPermissionGroups.containsKey(group)) {
                out = null;
            }
        }
        return out;
    }

    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) {
        PermissionGroupInfo permissionGroupInfoGeneratePermissionGroupInfo;
        synchronized (this.mPackages) {
            permissionGroupInfoGeneratePermissionGroupInfo = PackageParser.generatePermissionGroupInfo(this.mPermissionGroups.get(name), flags);
        }
        return permissionGroupInfoGeneratePermissionGroupInfo;
    }

    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        ArrayList<PermissionGroupInfo> out;
        synchronized (this.mPackages) {
            int N = this.mPermissionGroups.size();
            out = new ArrayList<>(N);
            for (PackageParser.PermissionGroup pg : this.mPermissionGroups.values()) {
                out.add(PackageParser.generatePermissionGroupInfo(pg, flags));
            }
        }
        return out;
    }

    private ApplicationInfo generateApplicationInfoFromSettingsLPw(String packageName, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId) || (ps = this.mSettings.mPackages.get(packageName)) == null) {
            return null;
        }
        if (ps.pkg == null) {
            PackageInfo pInfo = generatePackageInfoFromSettingsLPw(packageName, flags, userId);
            if (pInfo != null) {
                return pInfo.applicationInfo;
            }
            return null;
        }
        return PackageParser.generateApplicationInfo(ps.pkg, flags, ps.readUserState(userId), userId);
    }

    private PackageInfo generatePackageInfoFromSettingsLPw(String packageName, int flags, int userId) {
        PackageSetting ps;
        if (!sUserManager.exists(userId) || (ps = this.mSettings.mPackages.get(packageName)) == null) {
            return null;
        }
        PackageParser.Package pkg = ps.pkg;
        if (pkg == null) {
            if ((flags & DumpState.DUMP_INSTALLS) == 0) {
                return null;
            }
            pkg = new PackageParser.Package(packageName);
            pkg.applicationInfo.packageName = packageName;
            pkg.applicationInfo.flags = ps.pkgFlags | 16777216;
            pkg.applicationInfo.dataDir = getDataPathForPackage(packageName, 0).getPath();
            pkg.applicationInfo.primaryCpuAbi = ps.primaryCpuAbiString;
            pkg.applicationInfo.secondaryCpuAbi = ps.secondaryCpuAbiString;
        }
        return generatePackageInfo(pkg, flags, userId);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        ApplicationInfo applicationInfoGenerateApplicationInfoFromSettingsLPw;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get application info");
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p != null) {
                PackageSetting ps = this.mSettings.mPackages.get(packageName);
                applicationInfoGenerateApplicationInfoFromSettingsLPw = ps == null ? null : PackageParser.generateApplicationInfo(p, flags, ps.readUserState(userId), userId);
            } else if ("android".equals(packageName) || "system".equals(packageName)) {
                applicationInfoGenerateApplicationInfoFromSettingsLPw = this.mAndroidApplication;
            } else {
                applicationInfoGenerateApplicationInfoFromSettingsLPw = (flags & DumpState.DUMP_INSTALLS) != 0 ? generateApplicationInfoFromSettingsLPw(packageName, flags, userId) : null;
            }
        }
        return applicationInfoGenerateApplicationInfoFromSettingsLPw;
    }

    public void freeStorageAndNotify(final long freeStorageSize, final IPackageDataObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int retCode;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                synchronized (PackageManagerService.this.mInstallLock) {
                    retCode = PackageManagerService.this.mInstaller.freeCache(freeStorageSize);
                    if (retCode < 0) {
                        Slog.w(PackageManagerService.TAG, "Couldn't clear application caches");
                    }
                }
                if (observer != null) {
                    try {
                        observer.onRemoveCompleted((String) null, retCode >= 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false);
                    } catch (RemoteException e) {
                        Slog.w(PackageManagerService.TAG, "RemoveException when invoking call back");
                    }
                }
            }
        });
    }

    public void freeStorage(final long freeStorageSize, final IntentSender pi) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int retCode;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                synchronized (PackageManagerService.this.mInstallLock) {
                    retCode = PackageManagerService.this.mInstaller.freeCache(freeStorageSize);
                    if (retCode < 0) {
                        Slog.w(PackageManagerService.TAG, "Couldn't clear application caches");
                    }
                }
                if (pi != null) {
                    int code = retCode >= 0 ? 1 : 0;
                    try {
                        pi.sendIntent(null, code, null, null, null);
                    } catch (IntentSender.SendIntentException e) {
                        Slog.i(PackageManagerService.TAG, "Failed to send pending intent");
                    }
                }
            }
        });
    }

    void freeStorage(long freeStorageSize) throws IOException {
        synchronized (this.mInstallLock) {
            if (this.mInstaller.freeCache(freeStorageSize) < 0) {
                throw new IOException("Failed to free enough space");
            }
        }
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        ActivityInfo activityInfoGenerateActivityInfo;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get activity info");
        synchronized (this.mPackages) {
            PackageParser.Activity a = (PackageParser.Activity) this.mActivities.mActivities.get(component);
            if (a != null && this.mSettings.isEnabledLPr(a.info, flags, userId)) {
                PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
                activityInfoGenerateActivityInfo = ps == null ? null : PackageParser.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
            } else {
                activityInfoGenerateActivityInfo = this.mResolveComponentName.equals(component) ? PackageParser.generateActivityInfo(this.mResolveActivity, flags, new PackageUserState(), userId) : null;
            }
        }
        return activityInfoGenerateActivityInfo;
    }

    public boolean activitySupportsIntent(ComponentName component, Intent intent, String resolvedType) {
        synchronized (this.mPackages) {
            PackageParser.Activity a = (PackageParser.Activity) this.mActivities.mActivities.get(component);
            if (a == null) {
                return false;
            }
            for (int i = 0; i < a.intents.size(); i++) {
                if (((PackageParser.ActivityIntentInfo) a.intents.get(i)).match(intent.getAction(), resolvedType, intent.getScheme(), intent.getData(), intent.getCategories(), TAG) >= 0) {
                    return DEFAULT_VERIFY_ENABLE;
                }
            }
            return false;
        }
    }

    public ActivityInfo getReceiverInfo(ComponentName component, int flags, int userId) {
        ActivityInfo activityInfoGenerateActivityInfo;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get receiver info");
        synchronized (this.mPackages) {
            PackageParser.Activity a = (PackageParser.Activity) this.mReceivers.mActivities.get(component);
            if (a == null || !this.mSettings.isEnabledLPr(a.info, flags, userId)) {
                activityInfoGenerateActivityInfo = null;
            } else {
                PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
                activityInfoGenerateActivityInfo = ps == null ? null : PackageParser.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
            }
        }
        return activityInfoGenerateActivityInfo;
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        ServiceInfo serviceInfoGenerateServiceInfo;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get service info");
        synchronized (this.mPackages) {
            PackageParser.Service s = (PackageParser.Service) this.mServices.mServices.get(component);
            if (s == null || !this.mSettings.isEnabledLPr(s.info, flags, userId)) {
                serviceInfoGenerateServiceInfo = null;
            } else {
                PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
                serviceInfoGenerateServiceInfo = ps == null ? null : PackageParser.generateServiceInfo(s, flags, ps.readUserState(userId), userId);
            }
        }
        return serviceInfoGenerateServiceInfo;
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        ProviderInfo providerInfoGenerateProviderInfo;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get provider info");
        synchronized (this.mPackages) {
            PackageParser.Provider p = (PackageParser.Provider) this.mProviders.mProviders.get(component);
            if (p == null || !this.mSettings.isEnabledLPr(p.info, flags, userId)) {
                providerInfoGenerateProviderInfo = null;
            } else {
                PackageSetting ps = this.mSettings.mPackages.get(component.getPackageName());
                providerInfoGenerateProviderInfo = ps == null ? null : PackageParser.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
            }
        }
        return providerInfoGenerateProviderInfo;
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

    public FeatureInfo[] getSystemAvailableFeatures() {
        synchronized (this.mPackages) {
            Collection<FeatureInfo> featSet = this.mAvailableFeatures.values();
            int size = featSet.size();
            if (size > 0) {
                FeatureInfo[] features = new FeatureInfo[size + 1];
                featSet.toArray(features);
                FeatureInfo fi = new FeatureInfo();
                fi.reqGlEsVersion = SystemProperties.getInt("ro.opengles.version", 0);
                features[size] = fi;
                return features;
            }
            return null;
        }
    }

    public boolean hasSystemFeature(String name) {
        boolean zContainsKey;
        synchronized (this.mPackages) {
            zContainsKey = ("android.software.leanback".equals(name) && Arrays.asList(getPackagesForUid(Binder.getCallingUid())).contains("com.google.android.xts.audio")) ? DEFAULT_VERIFY_ENABLE : this.mAvailableFeatures.containsKey(name);
        }
        return zContainsKey;
    }

    private void checkValidCaller(int uid, int userId) {
        if (UserHandle.getUserId(uid) == userId || uid == 1000 || uid == 0) {
        } else {
            throw new SecurityException("Caller uid=" + uid + " is not privileged to communicate with user=" + userId);
        }
    }

    public int checkPermission(String permName, String pkgName) {
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(pkgName);
            if (p != null && p.mExtras != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                if (ps.sharedUser != null) {
                    if (ps.sharedUser.grantedPermissions.contains(permName)) {
                        return 0;
                    }
                } else if (ps.grantedPermissions.contains(permName)) {
                    return 0;
                }
            }
            return -1;
        }
    }

    public int checkUidPermission(String permName, int uid) {
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj != null) {
                GrantedPermissions gp = (GrantedPermissions) obj;
                if (gp.grantedPermissions.contains(permName)) {
                    return 0;
                }
            } else {
                ArraySet<String> perms = this.mSystemPermissions.get(uid);
                if (perms != null && perms.contains(permName)) {
                    return 0;
                }
            }
            return -1;
        }
    }

    void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            enforceShellRestriction("no_debugging_features", callingUid, userId);
        }
        if (userId != UserHandle.getUserId(callingUid) && callingUid != 1000 && callingUid != 0) {
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
    }

    void enforceShellRestriction(String restriction, int callingUid, int userHandle) {
        if (callingUid == SHELL_UID) {
            if (userHandle >= 0 && sUserManager.hasUserRestriction(restriction, userHandle)) {
                throw new SecurityException("Shell does not have permission to access user " + userHandle);
            }
            if (userHandle < 0) {
                Slog.e(TAG, "Unable to check shell permission for user " + userHandle + "\n\t" + Debug.getCallers(3));
            }
        }
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
        if (permName != null && (bp = findPermissionTreeLP(permName)) != null) {
            if (bp.uid == UserHandle.getAppId(Binder.getCallingUid())) {
                return bp;
            }
            throw new SecurityException("Calling uid " + Binder.getCallingUid() + " is not allowed to add to permission tree " + bp.name + " owned by uid " + bp.uid);
        }
        throw new SecurityException("No permission tree found for " + permName);
    }

    static boolean compareStrings(CharSequence s1, CharSequence s2) {
        if (s1 == null) {
            if (s2 == null) {
                return DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }
        if (s2 == null || s1.getClass() != s2.getClass()) {
            return false;
        }
        return s1.equals(s2);
    }

    static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        if (pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && compareStrings(pi1.name, pi2.name) && compareStrings(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && compareStrings(pi1.packageName, pi2.packageName)) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
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
        if (tree.uid != 1000) {
            int curTreeSize = calculateCurrentPermissionFootprintLocked(tree);
            if (permissionInfoFootprint(info) + curTreeSize > MAX_PERMISSION_TREE_FOOTPRINT) {
                throw new SecurityException("Permission tree size cap exceeded");
            }
        }
    }

    boolean addPermissionLocked(PermissionInfo info, boolean async) {
        if (info.labelRes == 0 && info.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        }
        BasePermission tree = checkPermissionTreeLP(info.name);
        BasePermission bp = this.mSettings.mPermissions.get(info.name);
        boolean added = bp == null ? DEFAULT_VERIFY_ENABLE : false;
        boolean changed = DEFAULT_VERIFY_ENABLE;
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
            zAddPermissionLocked = addPermissionLocked(info, DEFAULT_VERIFY_ENABLE);
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

    private static void checkGrantRevokePermissions(PackageParser.Package pkg, BasePermission bp) {
        int index = pkg.requestedPermissions.indexOf(bp.name);
        if (index == -1) {
            throw new SecurityException("Package " + pkg.packageName + " has not requested permission " + bp.name);
        }
        boolean isNormal = (bp.protectionLevel & 15) == 0;
        boolean isDangerous = (bp.protectionLevel & 15) == 1;
        boolean isDevelopment = (bp.protectionLevel & 32) != 0;
        if (!isNormal && !isDangerous && !isDevelopment) {
            throw new SecurityException("Permission " + bp.name + " is not a changeable permission type");
        }
        if ((isNormal || isDangerous) && ((Boolean) pkg.requestedPermissionsRequired.get(index)).booleanValue()) {
            throw new SecurityException("Can't change " + bp.name + ". It is required by the application");
        }
    }

    public void grantPermission(String packageName, String permissionName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_REVOKE_PERMISSIONS", null);
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            BasePermission bp = this.mSettings.mPermissions.get(permissionName);
            if (bp == null) {
                throw new IllegalArgumentException("Unknown permission: " + permissionName);
            }
            checkGrantRevokePermissions(pkg, bp);
            PackageSetting ps = (PackageSetting) pkg.mExtras;
            if (ps != null) {
                GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
                if (gp.grantedPermissions.add(permissionName)) {
                    if (ps.haveGids) {
                        gp.gids = appendInts(gp.gids, bp.gids);
                    }
                    this.mSettings.writeLPr();
                }
            }
        }
    }

    public void revokePermission(String packageName, String permissionName) {
        IActivityManager am;
        int changedAppId = -1;
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            if (pkg.applicationInfo.uid != Binder.getCallingUid()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_REVOKE_PERMISSIONS", null);
            }
            BasePermission bp = this.mSettings.mPermissions.get(permissionName);
            if (bp == null) {
                throw new IllegalArgumentException("Unknown permission: " + permissionName);
            }
            checkGrantRevokePermissions(pkg, bp);
            PackageSetting ps = (PackageSetting) pkg.mExtras;
            if (ps != null) {
                GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
                if (gp.grantedPermissions.remove(permissionName)) {
                    gp.grantedPermissions.remove(permissionName);
                    if (ps.haveGids) {
                        gp.gids = removeInts(gp.gids, bp.gids);
                    }
                    this.mSettings.writeLPr();
                    changedAppId = ps.appId;
                }
                if (changedAppId >= 0 && (am = ActivityManagerNative.getDefault()) != null) {
                    UserHandle.getCallingUserId();
                    long ident = Binder.clearCallingIdentity();
                    try {
                        int[] users = sUserManager.getUserIds();
                        for (int user : users) {
                            am.killUid(UserHandle.getUid(user, changedAppId), "revoke " + permissionName);
                        }
                    } catch (RemoteException e) {
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    public boolean isProtectedBroadcast(String actionName) {
        boolean zContains;
        synchronized (this.mPackages) {
            zContains = this.mProtectedBroadcasts.contains(actionName);
        }
        return zContains;
    }

    public int checkSignatures(String pkg1, String pkg2) {
        int iCompareSignatures;
        synchronized (this.mPackages) {
            PackageParser.Package p1 = this.mPackages.get(pkg1);
            PackageParser.Package p2 = this.mPackages.get(pkg2);
            iCompareSignatures = (p1 == null || p1.mExtras == null || p2 == null || p2.mExtras == null) ? -4 : compareSignatures(p1.mSignatures, p2.mSignatures);
        }
        return iCompareSignatures;
    }

    public int checkUidSignatures(int uid1, int uid2) {
        Signature[] s1;
        Signature[] s2;
        int iCompareSignatures = -4;
        int uid12 = UserHandle.getAppId(uid1);
        int uid22 = UserHandle.getAppId(uid2);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid12);
            if (obj != null) {
                if (obj instanceof SharedUserSetting) {
                    s1 = ((SharedUserSetting) obj).signatures.mSignatures;
                } else if (obj instanceof PackageSetting) {
                    s1 = ((PackageSetting) obj).signatures.mSignatures;
                }
                Object obj2 = this.mSettings.getUserIdLPr(uid22);
                if (obj2 != null) {
                    if (obj2 instanceof SharedUserSetting) {
                        s2 = ((SharedUserSetting) obj2).signatures.mSignatures;
                    } else if (obj2 instanceof PackageSetting) {
                        s2 = ((PackageSetting) obj2).signatures.mSignatures;
                    }
                    iCompareSignatures = compareSignatures(s1, s2);
                }
            }
        }
        return iCompareSignatures;
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
        if (!(isExternal(scannedPkg) && this.mSettings.isExternalDatabaseVersionOlderThan(2)) && (isExternal(scannedPkg) || !this.mSettings.isInternalDatabaseVersionOlderThan(2))) {
            return false;
        }
        return DEFAULT_VERIFY_ENABLE;
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
        Signature[] arr$ = scannedPkg.mSignatures;
        for (Signature sig : arr$) {
            try {
                Signature[] chainSignatures = sig.getChainSignatures();
                for (Signature chainSig : chainSignatures) {
                    scannedCompatSet.add(chainSig);
                }
            } catch (CertificateEncodingException e) {
                scannedCompatSet.add(sig);
            }
        }
        if (scannedCompatSet.equals(existingSet)) {
            existingSigs.assignSignatures(scannedPkg.mSignatures);
            synchronized (this.mPackages) {
                this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(scannedPkg.packageName);
            }
            return 0;
        }
        return -3;
    }

    private boolean isRecoverSignatureUpdateNeeded(PackageParser.Package scannedPkg) {
        return isExternal(scannedPkg) ? this.mSettings.isExternalDatabaseVersionOlderThan(3) : this.mSettings.isInternalDatabaseVersionOlderThan(3);
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

    public String[] getPackagesForUid(int uid) {
        int uid2 = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid2);
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                int N = sus.packages.size();
                String[] res = new String[N];
                Iterator<PackageSetting> it = sus.packages.iterator();
                int i = 0;
                while (it.hasNext()) {
                    res[i] = it.next().name;
                    i++;
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
        int i = -1;
        if (sharedUserName != null) {
            synchronized (this.mPackages) {
                SharedUserSetting suid = this.mSettings.getSharedUserLPw(sharedUserName, 0, false);
                if (suid != null) {
                    i = suid.userId;
                }
            }
        }
        return i;
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

    public boolean isUidPrivileged(int uid) {
        int uid2 = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid2);
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                Iterator<PackageSetting> it = sus.packages.iterator();
                while (it.hasNext()) {
                    if (it.next().isPrivileged()) {
                        return DEFAULT_VERIFY_ENABLE;
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
        String[] strArr;
        synchronized (this.mPackages) {
            ArraySet<String> pkgs = this.mAppOpPermissionPackages.get(permissionName);
            strArr = pkgs == null ? null : (String[]) pkgs.toArray(new String[pkgs.size()]);
        }
        return strArr;
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "resolve intent");
        List<ResolveInfo> query = queryIntentActivities(intent, resolvedType, flags, userId);
        return chooseBestActivity(intent, resolvedType, flags, query, userId);
    }

    public void setLastChosenActivity(Intent intent, String resolvedType, int flags, IntentFilter filter, int match, ComponentName activity) {
        int userId = UserHandle.getCallingUserId();
        intent.setComponent(null);
        List<ResolveInfo> query = queryIntentActivities(intent, resolvedType, flags, userId);
        findPreferredActivity(intent, resolvedType, flags, query, 0, false, DEFAULT_VERIFY_ENABLE, false, userId);
        addPreferredActivityInternal(filter, match, null, activity, false, userId, "Setting last chosen");
    }

    public ResolveInfo getLastChosenActivity(Intent intent, String resolvedType, int flags) {
        int userId = UserHandle.getCallingUserId();
        List<ResolveInfo> query = queryIntentActivities(intent, resolvedType, flags, userId);
        return findPreferredActivity(intent, resolvedType, flags, query, 0, false, false, false, userId);
    }

    private ResolveInfo chooseBestActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        if (query != null) {
            int N = query.size();
            if (N == 1) {
                return query.get(0);
            }
            if (N > 1) {
                boolean debug = (intent.getFlags() & 8) != 0 ? DEFAULT_VERIFY_ENABLE : false;
                ResolveInfo r0 = query.get(0);
                ResolveInfo r1 = query.get(1);
                if (debug) {
                    Slog.v(TAG, r0.activityInfo.name + "=" + r0.priority + " vs " + r1.activityInfo.name + "=" + r1.priority);
                }
                if (r0.priority != r1.priority || r0.preferredOrder != r1.preferredOrder || r0.isDefault != r1.isDefault) {
                    return query.get(0);
                }
                ResolveInfo ri = findPreferredActivity(intent, resolvedType, flags, query, r0.priority, DEFAULT_VERIFY_ENABLE, false, debug, userId);
                if (ri != null) {
                    return ri;
                }
                if (userId != 0) {
                    ResolveInfo ri2 = new ResolveInfo(this.mResolveInfo);
                    ri2.activityInfo = new ActivityInfo(ri2.activityInfo);
                    ri2.activityInfo.applicationInfo = new ApplicationInfo(ri2.activityInfo.applicationInfo);
                    ri2.activityInfo.applicationInfo.uid = UserHandle.getUid(userId, UserHandle.getAppId(ri2.activityInfo.applicationInfo.uid));
                    return ri2;
                }
                return this.mResolveInfo;
            }
        }
        return null;
    }

    private ResolveInfo findPersistentPreferredActivityLP(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, boolean debug, int userId) {
        List<PersistentPreferredActivity> pprefs;
        int N = query.size();
        PersistentPreferredIntentResolver ppir = this.mSettings.mPersistentPreferredActivities.get(userId);
        if (debug) {
            Slog.v(TAG, "Looking for presistent preferred activities...");
        }
        if (ppir != null) {
            pprefs = ppir.queryIntent(intent, resolvedType, (REMOVE_CHATTY & flags) != 0 ? DEFAULT_VERIFY_ENABLE : false, userId);
        } else {
            pprefs = null;
        }
        if (pprefs != null && pprefs.size() > 0) {
            int M = pprefs.size();
            for (int i = 0; i < M; i++) {
                PersistentPreferredActivity ppa = pprefs.get(i);
                if (debug) {
                    Slog.v(TAG, "Checking PersistentPreferredActivity ds=" + (ppa.countDataSchemes() > 0 ? ppa.getDataScheme(0) : "<none>") + "\n  component=" + ppa.mComponent);
                    ppa.dump(new LogPrinter(2, TAG, 3), "  ");
                }
                ActivityInfo ai = getActivityInfo(ppa.mComponent, flags | 512, userId);
                if (debug) {
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
                            if (debug) {
                                Slog.v(TAG, "Returning persistent preferred activity: " + ri.activityInfo.packageName + "/" + ri.activityInfo.name);
                                return ri;
                            }
                            return ri;
                        }
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
        synchronized (this.mPackages) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
            }
            ResolveInfo pri = findPersistentPreferredActivityLP(intent, resolvedType, flags, query, debug, userId);
            if (pri != null) {
                return pri;
            }
            PreferredIntentResolver pir = this.mSettings.mPreferredActivities.get(userId);
            if (debug) {
                Slog.v(TAG, "Looking for preferred activities...");
            }
            List<PreferredActivity> prefs = pir != null ? pir.queryIntent(intent, resolvedType, (REMOVE_CHATTY & flags) != 0 ? DEFAULT_VERIFY_ENABLE : false, userId) : null;
            if (prefs != null && prefs.size() > 0) {
                boolean changed = false;
                int match = 0;
                if (debug) {
                    try {
                        Slog.v(TAG, "Figuring out best match...");
                    } finally {
                        if (0 != 0) {
                            scheduleWritePackageRestrictionsLocked(userId);
                        }
                    }
                }
                int N = query.size();
                for (int j = 0; j < N; j++) {
                    ResolveInfo ri = query.get(j);
                    if (debug) {
                        Slog.v(TAG, "Match for " + ri.activityInfo + ": 0x" + Integer.toHexString(match));
                    }
                    if (ri.match > match) {
                        match = ri.match;
                    }
                }
                if (debug) {
                    Slog.v(TAG, "Best match: 0x" + Integer.toHexString(match));
                }
                int match2 = match & 268369920;
                int M = prefs.size();
                for (int i = 0; i < M; i++) {
                    PreferredActivity pa = prefs.get(i);
                    if (debug) {
                        Slog.v(TAG, "Checking PreferredActivity ds=" + (pa.countDataSchemes() > 0 ? pa.getDataScheme(0) : "<none>") + "\n  component=" + pa.mPref.mComponent);
                        pa.dump(new LogPrinter(2, TAG, 3), "  ");
                    }
                    if (pa.mPref.mMatch != match2) {
                        if (debug) {
                            Slog.v(TAG, "Skipping bad match " + Integer.toHexString(pa.mPref.mMatch));
                        }
                    } else if (!always || pa.mPref.mAlways) {
                        ActivityInfo ai = getActivityInfo(pa.mPref.mComponent, flags | 512, userId);
                        if (debug) {
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
                            changed = DEFAULT_VERIFY_ENABLE;
                        } else {
                            int j2 = 0;
                            while (true) {
                                if (j2 < N) {
                                    ResolveInfo ri2 = query.get(j2);
                                    if (ri2.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri2.activityInfo.name.equals(ai.name)) {
                                        if (!removeMatches) {
                                            if (!always || pa.mPref.sameSet(query)) {
                                                if (debug) {
                                                    Slog.v(TAG, "Returning preferred activity: " + ri2.activityInfo.packageName + "/" + ri2.activityInfo.name);
                                                }
                                                return ri2;
                                            }
                                            Slog.i(TAG, "Result set changed, dropping preferred activity for " + intent + " type " + resolvedType);
                                            pir.removeFilter(pa);
                                            PreferredActivity lastChosen = new PreferredActivity(pa, pa.mPref.mMatch, null, pa.mPref.mComponent, false);
                                            pir.addFilter(lastChosen);
                                            if (1 != 0) {
                                                scheduleWritePackageRestrictionsLocked(userId);
                                            }
                                            return null;
                                        }
                                        pir.removeFilter(pa);
                                        changed = DEFAULT_VERIFY_ENABLE;
                                    } else {
                                        j2++;
                                    }
                                }
                            }
                        }
                    } else if (debug) {
                        Slog.v(TAG, "Skipping mAlways=false entry");
                    }
                }
                if (changed) {
                    scheduleWritePackageRestrictionsLocked(userId);
                }
            }
            if (debug) {
                Slog.v(TAG, "No preferred activity to return");
            }
            return null;
        }
    }

    public boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        List<CrossProfileIntentFilter> matches = getMatchingCrossProfileIntentFilters(intent, resolvedType, sourceUserId);
        if (matches != null) {
            int size = matches.size();
            for (int i = 0; i < size; i++) {
                if (matches.get(i).getTargetUserId() == targetUserId) {
                    return DEFAULT_VERIFY_ENABLE;
                }
            }
        }
        return false;
    }

    private List<CrossProfileIntentFilter> getMatchingCrossProfileIntentFilters(Intent intent, String resolvedType, int userId) {
        CrossProfileIntentResolver resolver = this.mSettings.mCrossProfileIntentResolvers.get(userId);
        if (resolver != null) {
            return resolver.queryIntent(intent, resolvedType, false, userId);
        }
        return null;
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activities");
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ActivityInfo ai = getActivityInfo(comp, flags, userId);
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
                ResolveInfo resolveInfo = querySkipCurrentProfileIntents(matchingFilters, intent, resolvedType, flags, userId);
                if (resolveInfo != null) {
                    List<ResolveInfo> result = new ArrayList<>(1);
                    result.add(resolveInfo);
                    return result;
                }
                ResolveInfo resolveInfo2 = queryCrossProfileIntents(matchingFilters, intent, resolvedType, flags, userId);
                List<ResolveInfo> result2 = this.mActivities.queryIntent(intent, resolvedType, flags, userId);
                if (resolveInfo2 != null) {
                    result2.add(resolveInfo2);
                    Collections.sort(result2, mResolvePrioritySorter);
                }
                return result2;
            }
            PackageParser.Package pkg = this.mPackages.get(pkgName);
            if (pkg != null) {
                return this.mActivities.queryIntentForPackage(intent, resolvedType, flags, pkg.activities, userId);
            }
            return new ArrayList();
        }
    }

    private ResolveInfo querySkipCurrentProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId) {
        ResolveInfo resolveInfo;
        if (matchingFilters != null) {
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = matchingFilters.get(i);
                if ((filter.getFlags() & 2) != 0 && (resolveInfo = checkTargetCanHandle(filter, intent, resolvedType, flags, sourceUserId)) != null) {
                    return resolveInfo;
                }
            }
        }
        return null;
    }

    private ResolveInfo queryCrossProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId) {
        if (matchingFilters != null) {
            SparseBooleanArray alreadyTriedUserIds = new SparseBooleanArray();
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = matchingFilters.get(i);
                int targetUserId = filter.getTargetUserId();
                if ((filter.getFlags() & 2) == 0 && !alreadyTriedUserIds.get(targetUserId)) {
                    ResolveInfo resolveInfo = checkTargetCanHandle(filter, intent, resolvedType, flags, sourceUserId);
                    if (resolveInfo == null) {
                        alreadyTriedUserIds.put(targetUserId, DEFAULT_VERIFY_ENABLE);
                    } else {
                        return resolveInfo;
                    }
                }
            }
        }
        return null;
    }

    private ResolveInfo checkTargetCanHandle(CrossProfileIntentFilter filter, Intent intent, String resolvedType, int flags, int sourceUserId) {
        List<ResolveInfo> resultTargetUser = this.mActivities.queryIntent(intent, resolvedType, flags, filter.getTargetUserId());
        if (resultTargetUser == null || resultTargetUser.isEmpty()) {
            return null;
        }
        return createForwardingResolveInfo(filter, sourceUserId, filter.getTargetUserId());
    }

    private ResolveInfo createForwardingResolveInfo(IntentFilter filter, int sourceUserId, int targetUserId) {
        String className;
        ResolveInfo forwardingResolveInfo = new ResolveInfo();
        if (targetUserId == 0) {
            className = IntentForwarderActivity.FORWARD_INTENT_TO_USER_OWNER;
        } else {
            className = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE;
        }
        ComponentName forwardingActivityComponentName = new ComponentName(this.mAndroidApplication.packageName, className);
        ActivityInfo forwardingActivityInfo = getActivityInfo(forwardingActivityComponentName, 0, sourceUserId);
        if (targetUserId == 0) {
            forwardingActivityInfo.showUserIcon = 0;
            forwardingResolveInfo.noResourceId = DEFAULT_VERIFY_ENABLE;
        }
        forwardingResolveInfo.activityInfo = forwardingActivityInfo;
        forwardingResolveInfo.priority = 0;
        forwardingResolveInfo.preferredOrder = 0;
        forwardingResolveInfo.match = 0;
        forwardingResolveInfo.isDefault = DEFAULT_VERIFY_ENABLE;
        forwardingResolveInfo.filter = filter;
        forwardingResolveInfo.targetUserId = targetUserId;
        return forwardingResolveInfo;
    }

    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) {
        Iterator<String> it;
        ActivityInfo ai;
        int N;
        int j;
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activity options");
        String resultsAction = intent.getAction();
        List<ResolveInfo> results = queryIntentActivities(intent, resolvedType, flags | 64, userId);
        int specificsPos = 0;
        if (specifics != null) {
            for (int i = 0; i < specifics.length; i++) {
                Intent sintent = specifics[i];
                if (sintent != null) {
                    String action = sintent.getAction();
                    if (resultsAction != null && resultsAction.equals(action)) {
                        action = null;
                    }
                    ResolveInfo ri = null;
                    ComponentName comp = sintent.getComponent();
                    if (comp == null) {
                        ri = resolveIntent(sintent, specificTypes != null ? specificTypes[i] : null, flags, userId);
                        if (ri != null) {
                            if (ri == this.mResolveInfo) {
                            }
                            ai = ri.activityInfo;
                            comp = new ComponentName(ai.applicationInfo.packageName, ai.name);
                            N = results.size();
                            j = specificsPos;
                            while (j < N) {
                                ResolveInfo sri = results.get(j);
                                if ((sri.activityInfo.name.equals(comp.getClassName()) && sri.activityInfo.applicationInfo.packageName.equals(comp.getPackageName())) || (action != null && sri.filter.matchAction(action))) {
                                    results.remove(j);
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
                        ai = getActivityInfo(comp, flags, userId);
                        if (ai != null) {
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
                                j2--;
                                N2--;
                            }
                            j2++;
                        }
                    }
                }
                if ((flags & 64) == 0) {
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
        if ((flags & 64) == 0) {
            int N4 = results.size();
            for (int i4 = 0; i4 < N4; i4++) {
                results.get(i4).filter = null;
            }
            return results;
        }
        return results;
    }

    public List<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId) {
        List<ResolveInfo> listQueryIntentForPackage;
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ActivityInfo ai = getReceiverInfo(comp, flags, userId);
            if (ai != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
                return list;
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                listQueryIntentForPackage = this.mReceivers.queryIntent(intent, resolvedType, flags, userId);
            } else {
                PackageParser.Package pkg = this.mPackages.get(pkgName);
                if (pkg != null) {
                    listQueryIntentForPackage = this.mReceivers.queryIntentForPackage(intent, resolvedType, flags, pkg.receivers, userId);
                } else {
                    listQueryIntentForPackage = null;
                }
            }
        }
        return listQueryIntentForPackage;
    }

    public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) {
        List<ResolveInfo> query = queryIntentServices(intent, resolvedType, flags, userId);
        if (sUserManager.exists(userId) && query != null && query.size() >= 1) {
            return query.get(0);
        }
        return null;
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags, int userId) {
        List<ResolveInfo> listQueryIntentForPackage;
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ServiceInfo si = getServiceInfo(comp, flags, userId);
            if (si != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.serviceInfo = si;
                list.add(ri);
                return list;
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                listQueryIntentForPackage = this.mServices.queryIntent(intent, resolvedType, flags, userId);
            } else {
                PackageParser.Package pkg = this.mPackages.get(pkgName);
                if (pkg != null) {
                    listQueryIntentForPackage = this.mServices.queryIntentForPackage(intent, resolvedType, flags, pkg.services, userId);
                } else {
                    listQueryIntentForPackage = null;
                }
            }
        }
        return listQueryIntentForPackage;
    }

    public List<ResolveInfo> queryIntentContentProviders(Intent intent, String resolvedType, int flags, int userId) {
        List<ResolveInfo> listQueryIntentForPackage;
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList<>(1);
            ProviderInfo pi = getProviderInfo(comp, flags, userId);
            if (pi != null) {
                ResolveInfo ri = new ResolveInfo();
                ri.providerInfo = pi;
                list.add(ri);
                return list;
            }
            return list;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                listQueryIntentForPackage = this.mProviders.queryIntent(intent, resolvedType, flags, userId);
            } else {
                PackageParser.Package pkg = this.mPackages.get(pkgName);
                if (pkg != null) {
                    listQueryIntentForPackage = this.mProviders.queryIntentForPackage(intent, resolvedType, flags, pkg.providers, userId);
                } else {
                    listQueryIntentForPackage = null;
                }
            }
        }
        return listQueryIntentForPackage;
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) {
        ArrayList<PackageInfo> list;
        ParceledListSlice<PackageInfo> parceledListSlice;
        PackageInfo pi;
        boolean listUninstalled = (flags & DumpState.DUMP_INSTALLS) != 0;
        enforceCrossUserPermission(Binder.getCallingUid(), userId, DEFAULT_VERIFY_ENABLE, false, "get installed packages");
        synchronized (this.mPackages) {
            if (listUninstalled) {
                list = new ArrayList<>(this.mSettings.mPackages.size());
                for (PackageSetting ps : this.mSettings.mPackages.values()) {
                    if (ps.pkg != null) {
                        pi = generatePackageInfo(ps.pkg, flags, userId);
                    } else {
                        pi = generatePackageInfoFromSettingsLPw(ps.name, flags, userId);
                    }
                    if (pi != null) {
                        list.add(pi);
                    }
                }
            } else {
                list = new ArrayList<>(this.mPackages.size());
                for (PackageParser.Package p : this.mPackages.values()) {
                    PackageInfo pi2 = generatePackageInfo(p, flags, userId);
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
        GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
        for (int i = 0; i < permissions.length; i++) {
            if (gp.grantedPermissions.contains(permissions[i])) {
                tmp[i] = DEFAULT_VERIFY_ENABLE;
                numMatch++;
            } else {
                tmp[i] = false;
            }
        }
        if (numMatch != 0) {
            if (ps.pkg != null) {
                pi = generatePackageInfo(ps.pkg, flags, userId);
            } else {
                pi = generatePackageInfoFromSettingsLPw(ps.name, flags, userId);
            }
            if (pi != null) {
                if ((flags & DumpState.DUMP_VERSION) == 0) {
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
        }
    }

    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags, int userId) {
        ParceledListSlice<PackageInfo> parceledListSlice;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        boolean listUninstalled = (flags & DumpState.DUMP_INSTALLS) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        synchronized (this.mPackages) {
            ArrayList<PackageInfo> list = new ArrayList<>();
            boolean[] tmpBools = new boolean[permissions.length];
            if (listUninstalled) {
                Iterator<PackageSetting> it = this.mSettings.mPackages.values().iterator();
                while (it.hasNext()) {
                    addPackageHoldingPermissions(list, it.next(), permissions, tmpBools, flags, userId);
                }
            } else {
                for (PackageParser.Package pkg : this.mPackages.values()) {
                    PackageSetting ps = (PackageSetting) pkg.mExtras;
                    if (ps != null) {
                        addPackageHoldingPermissions(list, ps, permissions, tmpBools, flags, userId);
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
            return null;
        }
        boolean listUninstalled = (flags & DumpState.DUMP_INSTALLS) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        synchronized (this.mPackages) {
            if (listUninstalled) {
                list = new ArrayList<>(this.mSettings.mPackages.size());
                for (PackageSetting ps : this.mSettings.mPackages.values()) {
                    if (ps.pkg != null) {
                        ai2 = PackageParser.generateApplicationInfo(ps.pkg, flags, ps.readUserState(userId), userId);
                    } else {
                        ai2 = generateApplicationInfoFromSettingsLPw(ps.name, flags, userId);
                    }
                    if (ai2 != null) {
                        list.add(ai2);
                    }
                }
            } else {
                list = new ArrayList<>(this.mPackages.size());
                for (PackageParser.Package p : this.mPackages.values()) {
                    if (p.mExtras != null && (ai = PackageParser.generateApplicationInfo(p, flags, ((PackageSetting) p.mExtras).readUserState(userId), userId)) != null) {
                        list.add(ai);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice<>(list);
        }
        return parceledListSlice;
    }

    public List<ApplicationInfo> getPersistentApplications(int flags) {
        ApplicationInfo ai;
        ArrayList<ApplicationInfo> finalList = new ArrayList<>();
        synchronized (this.mPackages) {
            int userId = UserHandle.getCallingUserId();
            for (PackageParser.Package p : this.mPackages.values()) {
                if (p.applicationInfo != null && (p.applicationInfo.flags & 8) != 0 && (!this.mSafeMode || isSystemApp(p))) {
                    PackageSetting ps = this.mSettings.mPackages.get(p.packageName);
                    if (ps != null && (ai = PackageParser.generateApplicationInfo(p, flags, ps.readUserState(userId), userId)) != null) {
                        finalList.add(ai);
                    }
                }
            }
        }
        return finalList;
    }

    public ProviderInfo resolveContentProvider(String name, int flags, int userId) {
        ProviderInfo providerInfoGenerateProviderInfo;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        synchronized (this.mPackages) {
            PackageParser.Provider provider = this.mProvidersByAuthority.get(name);
            PackageSetting ps = provider != null ? this.mSettings.mPackages.get(provider.owner.packageName) : null;
            providerInfoGenerateProviderInfo = (ps == null || !this.mSettings.isEnabledLPr(provider.info, flags, userId) || (this.mSafeMode && (provider.info.applicationInfo.flags & 1) == 0)) ? null : PackageParser.generateProviderInfo(provider, flags, ps.readUserState(userId), userId);
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

    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) throws Throwable {
        ArrayList<ProviderInfo> finalList;
        synchronized (this.mPackages) {
            try {
                int userId = processName != null ? UserHandle.getUserId(uid) : UserHandle.getCallingUserId();
                ArrayList<ProviderInfo> finalList2 = null;
                for (PackageParser.Provider p : this.mProviders.mProviders.values()) {
                    try {
                        PackageSetting ps = this.mSettings.mPackages.get(p.owner.packageName);
                        if (ps == null || p.info.authority == null || !((processName == null || (p.info.processName.equals(processName) && UserHandle.isSameApp(p.info.applicationInfo.uid, uid))) && this.mSettings.isEnabledLPr(p.info, flags, userId) && !(this.mSafeMode && (p.info.applicationInfo.flags & 1) == 0))) {
                            finalList = finalList2;
                        } else {
                            finalList = finalList2 == null ? new ArrayList<>(3) : finalList2;
                            ProviderInfo info = PackageParser.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
                            if (info != null) {
                                finalList.add(info);
                            }
                        }
                        finalList2 = finalList;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (finalList2 != null) {
                    Collections.sort(finalList2, mProviderInitOrderSorter);
                }
                return finalList2;
            } catch (Throwable th2) {
                th = th2;
            }
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

    public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
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
        if (this.mInstaller.idmap(pkg.baseCodePath, opkg.baseCodePath, sharedGid) != 0) {
            Slog.e(TAG, "Failed to generate idmap for " + pkg.baseCodePath + " and " + opkg.baseCodePath);
            return false;
        }
        PackageParser.Package[] overlayArray = (PackageParser.Package[]) overlaySet.values().toArray(new PackageParser.Package[0]);
        Comparator<PackageParser.Package> cmp = new Comparator<PackageParser.Package>() {
            @Override
            public int compare(PackageParser.Package p1, PackageParser.Package p2) {
                return p1.mOverlayPriority - p2.mOverlayPriority;
            }
        };
        Arrays.sort(overlayArray, cmp);
        pkg.applicationInfo.resourceDirs = new String[overlayArray.length];
        int len$ = overlayArray.length;
        int i$ = 0;
        int i = 0;
        while (i$ < len$) {
            PackageParser.Package p = overlayArray[i$];
            pkg.applicationInfo.resourceDirs[i] = p.baseCodePath;
            i$++;
            i++;
        }
        return DEFAULT_VERIFY_ENABLE;
    }

    private void scanDirLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        File[] files = dir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            Log.d(TAG, "No files in app dir " + dir);
            return;
        }
        for (File file : files) {
            boolean isPackage = ((PackageParser.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName())) ? DEFAULT_VERIFY_ENABLE : false;
            if (isPackage) {
                try {
                    scanPackageLI(file, parseFlags | 4, scanFlags, currentTime, (UserHandle) null);
                } catch (PackageManagerException e) {
                    Slog.w(TAG, "Failed to parse " + file + ": " + e.getMessage());
                    if ((parseFlags & 1) == 0 && e.error == -2) {
                        logCriticalInfo(5, "Deleting invalid package at " + file);
                        if (file.isDirectory()) {
                            FileUtils.deleteContents(file);
                        }
                        file.delete();
                    }
                }
            }
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
            FileOutputStream out = new FileOutputStream(fname, DEFAULT_VERIFY_ENABLE);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(out);
            SimpleDateFormat formatter = new SimpleDateFormat();
            String dateString = formatter.format(new Date(System.currentTimeMillis()));
            fastPrintWriter.println(dateString + ": " + msg);
            fastPrintWriter.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    private void collectCertificatesLI(PackageParser pp, PackageSetting ps, PackageParser.Package pkg, File srcFile, int parseFlags) throws PackageManagerException {
        if (ps != null && ps.codePath.equals(srcFile) && ps.timeStamp == srcFile.lastModified() && !isCompatSignatureUpdateNeeded(pkg) && !isRecoverSignatureUpdateNeeded(pkg)) {
            long mSigningKeySetId = ps.keySetData.getProperSigningKeySet();
            if (ps.signatures.mSignatures != null && ps.signatures.mSignatures.length != 0 && mSigningKeySetId != -1) {
                pkg.mSignatures = ps.signatures.mSignatures;
                KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
                synchronized (this.mPackages) {
                    pkg.mSigningKeys = ksms.getPublicKeysFromKeySetLPr(mSigningKeySetId);
                }
                return;
            }
            Slog.w(TAG, "PackageSetting for " + ps.name + " is missing signatures.  Collecting certs again to recover them.");
        } else {
            Log.i(TAG, srcFile.toString() + " changed; collecting certs");
        }
        try {
            pp.collectCertificates(pkg, parseFlags);
            pp.collectManifestDigest(pkg);
        } catch (PackageParser.PackageParserException e) {
            throw PackageManagerException.from(e);
        }
    }

    private PackageParser.Package scanPackageLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        PackageSetting updatedPkg;
        int parseFlags2 = parseFlags | this.mDefParseFlags;
        PackageParser pp = new PackageParser();
        pp.setSeparateProcesses(this.mSeparateProcesses);
        pp.setOnlyCoreApps(this.mOnlyCore);
        pp.setDisplayMetrics(this.mMetrics);
        if ((scanFlags & 512) != 0) {
            parseFlags2 |= 512;
        }
        try {
            PackageParser.Package pkg = pp.parsePackage(scanFile, parseFlags2);
            PackageSetting ps = null;
            synchronized (this.mPackages) {
                String oldName = this.mSettings.mRenamedPackages.get(pkg.packageName);
                if (pkg.mOriginalPackages != null && pkg.mOriginalPackages.contains(oldName)) {
                    ps = this.mSettings.peekPackageLPr(oldName);
                }
                if (ps == null) {
                    ps = this.mSettings.peekPackageLPr(pkg.packageName);
                }
                updatedPkg = this.mSettings.getDisabledSystemPkgLPr(ps != null ? ps.name : pkg.packageName);
            }
            boolean updatedPkgBetter = false;
            if (updatedPkg != null && (parseFlags2 & 1) != 0) {
                if (locationIsPrivileged(scanFile)) {
                    updatedPkg.pkgFlags |= 1073741824;
                } else {
                    updatedPkg.pkgFlags &= -1073741825;
                }
                if (ps != null && !ps.codePath.equals(scanFile)) {
                    if (pkg.mVersionCode <= ps.versionCode) {
                        Slog.i(TAG, "Package " + ps.name + " at " + scanFile + " ignored: updated version " + ps.versionCode + " better than this " + pkg.mVersionCode);
                        if (!updatedPkg.codePath.equals(scanFile)) {
                            Slog.w(TAG, "Code path for hidden system pkg : " + ps.name + " changing from " + updatedPkg.codePathString + " to " + scanFile);
                            updatedPkg.codePath = scanFile;
                            updatedPkg.codePathString = scanFile.toString();
                        }
                        updatedPkg.pkg = pkg;
                        throw new PackageManagerException(-5, null);
                    }
                    synchronized (this.mPackages) {
                        this.mPackages.remove(ps.name);
                    }
                    logCriticalInfo(5, "Package " + ps.name + " at " + scanFile + " reverting from " + ps.codePathString + ": new version " + pkg.mVersionCode + " better than installed " + ps.versionCode);
                    InstallArgs args = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, ps.legacyNativeLibraryPathString, getAppDexInstructionSets(ps));
                    synchronized (this.mInstallLock) {
                        args.cleanUpResourcesLI();
                    }
                    synchronized (this.mPackages) {
                        this.mSettings.enableSystemPackageLPw(ps.name);
                    }
                    updatedPkgBetter = DEFAULT_VERIFY_ENABLE;
                }
            }
            if (updatedPkg != null) {
                parseFlags2 |= 1;
                if ((updatedPkg.pkgFlags & 1073741824) != 0) {
                    parseFlags2 |= 128;
                }
            }
            collectCertificatesLI(pp, ps, pkg, scanFile, parseFlags2);
            boolean shouldHideSystemApp = false;
            if (updatedPkg == null && ps != null && (parseFlags2 & 64) != 0 && !isSystemApp(ps)) {
                if (compareSignatures(ps.signatures.mSignatures, pkg.mSignatures) != 0) {
                    logCriticalInfo(5, "Package " + ps.name + " appeared on system, but signatures don't match existing userdata copy; removing");
                    deletePackageLI(pkg.packageName, null, DEFAULT_VERIFY_ENABLE, null, null, 0, null, false);
                    ps = null;
                } else if (pkg.mVersionCode <= ps.versionCode) {
                    shouldHideSystemApp = DEFAULT_VERIFY_ENABLE;
                    logCriticalInfo(4, "Package " + ps.name + " appeared at " + scanFile + " but new version " + pkg.mVersionCode + " better than installed " + ps.versionCode + "; hiding system");
                } else {
                    logCriticalInfo(5, "Package " + ps.name + " at " + scanFile + " reverting from " + ps.codePathString + ": new version " + pkg.mVersionCode + " better than installed " + ps.versionCode);
                    InstallArgs args2 = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, ps.legacyNativeLibraryPathString, getAppDexInstructionSets(ps));
                    synchronized (this.mInstallLock) {
                        args2.cleanUpResourcesLI();
                    }
                }
            }
            if ((parseFlags2 & 64) == 0 && ps != null && !ps.codePath.equals(ps.resourcePath)) {
                parseFlags2 |= 16;
            }
            String resourcePath = null;
            String baseResourcePath = null;
            if ((parseFlags2 & 16) != 0 && !updatedPkgBetter) {
                if (ps != null && ps.resourcePathString != null) {
                    resourcePath = ps.resourcePathString;
                    baseResourcePath = ps.resourcePathString;
                } else {
                    Slog.e(TAG, "Resource path not set for pkg : " + pkg.packageName);
                }
            } else {
                resourcePath = pkg.codePath;
                baseResourcePath = pkg.baseCodePath;
            }
            pkg.applicationInfo.setCodePath(pkg.codePath);
            pkg.applicationInfo.setBaseCodePath(pkg.baseCodePath);
            pkg.applicationInfo.setSplitCodePaths(pkg.splitCodePaths);
            pkg.applicationInfo.setResourcePath(resourcePath);
            pkg.applicationInfo.setBaseResourcePath(baseResourcePath);
            pkg.applicationInfo.setSplitResourcePaths(pkg.splitCodePaths);
            PackageParser.Package scannedPkg = scanPackageLI(pkg, parseFlags2, scanFlags | 8, currentTime, user);
            if (shouldHideSystemApp) {
                synchronized (this.mPackages) {
                    grantPermissionsLPw(pkg, DEFAULT_VERIFY_ENABLE, pkg.packageName);
                    this.mSettings.disableSystemPackageLPw(pkg.packageName);
                }
            }
            return scannedPkg;
        } catch (PackageParser.PackageParserException e) {
            throw PackageManagerException.from(e);
        }
    }

    private static String fixProcessName(String defProcessName, String processName, int uid) {
        return processName == null ? defProcessName : processName;
    }

    private void verifySignaturesLP(PackageSetting pkgSetting, PackageParser.Package pkg) throws PackageManagerException {
        if (pkgSetting.signatures.mSignatures != null) {
            boolean match = compareSignatures(pkgSetting.signatures.mSignatures, pkg.mSignatures) == 0;
            if (!match) {
                match = compareSignaturesCompat(pkgSetting.signatures, pkg) == 0;
            }
            if (!match) {
                match = compareSignaturesRecover(pkgSetting.signatures, pkg) == 0;
            }
            if (!match) {
                throw new PackageManagerException(-7, "Package " + pkg.packageName + " signatures do not match the previously installed version; ignoring!");
            }
        }
        if (pkgSetting.sharedUser != null && pkgSetting.sharedUser.signatures.mSignatures != null) {
            boolean match2 = compareSignatures(pkgSetting.sharedUser.signatures.mSignatures, pkg.mSignatures) == 0;
            if (!match2) {
                match2 = compareSignaturesCompat(pkgSetting.sharedUser.signatures, pkg) == 0;
            }
            if (!match2) {
                match2 = compareSignaturesRecover(pkgSetting.sharedUser.signatures, pkg) == 0;
            }
            if (!match2) {
                throw new PackageManagerException(-8, "Package " + pkg.packageName + " has no signatures that match those in shared user " + pkgSetting.sharedUser.name + "; ignoring!");
            }
        }
    }

    private static final void enforceSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (uid != 1000 && uid != 0) {
            throw new SecurityException(message);
        }
    }

    public void performBootDexOpt() {
        ArraySet<PackageParser.Package> pkgs;
        enforceSystemOrRoot("Only the system can request dexopt be performed");
        try {
            IMountService ms = PackageHelper.getMountService();
            if (ms != null) {
                boolean isUpgrade = isUpgrade();
                boolean doTrim = isUpgrade;
                if (doTrim) {
                    Slog.w(TAG, "Running disk maintenance immediately due to system update");
                } else {
                    long interval = Settings.Global.getLong(this.mContext.getContentResolver(), "fstrim_mandatory_interval", DEFAULT_MANDATORY_FSTRIM_INTERVAL);
                    if (interval > 0) {
                        long timeSinceLast = System.currentTimeMillis() - ms.lastMaintenance();
                        if (timeSinceLast > interval) {
                            doTrim = DEFAULT_VERIFY_ENABLE;
                            Slog.w(TAG, "No disk maintenance in " + timeSinceLast + "; running immediately");
                        }
                    }
                }
                if (doTrim) {
                    if (!isFirstBoot()) {
                        try {
                            ActivityManagerNative.getDefault().showBootMessage(this.mContext.getResources().getString(R.string.hearing_device_status_disconnected), DEFAULT_VERIFY_ENABLE);
                        } catch (RemoteException e) {
                        }
                    }
                    ms.runMaintenance();
                }
            } else {
                Slog.e(TAG, "Mount service unavailable!");
            }
        } catch (RemoteException e2) {
        }
        synchronized (this.mPackages) {
            pkgs = this.mDeferredDexOpt;
            this.mDeferredDexOpt = null;
        }
        if (pkgs != null) {
            ArrayList<PackageParser.Package> sortedPkgs = new ArrayList<>();
            Iterator<PackageParser.Package> it = pkgs.iterator();
            while (it.hasNext()) {
                PackageParser.Package pkg = it.next();
                if (pkg.coreApp) {
                    sortedPkgs.add(pkg);
                    it.remove();
                }
            }
            Intent intent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
            ArraySet<String> pkgNames = getPackageNamesForIntent(intent);
            Iterator<PackageParser.Package> it2 = pkgs.iterator();
            while (it2.hasNext()) {
                PackageParser.Package pkg2 = it2.next();
                if (pkgNames.contains(pkg2.packageName)) {
                    sortedPkgs.add(pkg2);
                    it2.remove();
                }
            }
            Iterator<PackageParser.Package> it3 = pkgs.iterator();
            while (it3.hasNext()) {
                PackageParser.Package pkg3 = it3.next();
                if (isSystemApp(pkg3) && !isUpdatedSystemApp(pkg3)) {
                    sortedPkgs.add(pkg3);
                    it3.remove();
                }
            }
            Iterator<PackageParser.Package> it4 = pkgs.iterator();
            while (it4.hasNext()) {
                PackageParser.Package pkg4 = it4.next();
                if (isUpdatedSystemApp(pkg4)) {
                    sortedPkgs.add(pkg4);
                    it4.remove();
                }
            }
            Intent intent2 = new Intent("android.intent.action.BOOT_COMPLETED");
            ArraySet<String> pkgNames2 = getPackageNamesForIntent(intent2);
            Iterator<PackageParser.Package> it5 = pkgs.iterator();
            while (it5.hasNext()) {
                PackageParser.Package pkg5 = it5.next();
                if (pkgNames2.contains(pkg5.packageName)) {
                    sortedPkgs.add(pkg5);
                    it5.remove();
                }
            }
            filterRecentlyUsedApps(pkgs);
            for (PackageParser.Package pkg6 : pkgs) {
                sortedPkgs.add(pkg6);
            }
            if (this.mLazyDexOpt) {
                filterRecentlyUsedApps(sortedPkgs);
            }
            int i = 0;
            int total = sortedPkgs.size();
            File dataDir = Environment.getDataDirectory();
            long lowThreshold = StorageManager.from(this.mContext).getStorageLowBytes(dataDir);
            if (lowThreshold == 0) {
                throw new IllegalStateException("Invalid low memory threshold");
            }
            for (PackageParser.Package pkg7 : sortedPkgs) {
                long usableSpace = dataDir.getUsableSpace();
                if (usableSpace < lowThreshold) {
                    Log.w(TAG, "Not running dexopt on remaining apps due to low memory: " + usableSpace);
                    return;
                } else {
                    i++;
                    performBootDexOpt(pkg7, i, total);
                }
            }
        }
    }

    private void filterRecentlyUsedApps(Collection<PackageParser.Package> pkgs) {
        if (this.mLazyDexOpt || (!isFirstBoot() && this.mPackageUsage.isHistoricalPackageUsageAvailable())) {
            pkgs.size();
            int skipped = 0;
            long now = System.currentTimeMillis();
            Iterator<PackageParser.Package> i = pkgs.iterator();
            while (i.hasNext()) {
                PackageParser.Package pkg = i.next();
                long then = pkg.mLastPackageUsageTimeInMills;
                if (this.mDexOptLRUThresholdInMills + then < now) {
                    i.remove();
                    skipped++;
                }
            }
        }
    }

    private ArraySet<String> getPackageNamesForIntent(Intent intent) {
        List<ResolveInfo> ris = null;
        try {
            ris = AppGlobals.getPackageManager().queryIntentReceivers(intent, (String) null, 0, 0);
        } catch (RemoteException e) {
        }
        ArraySet<String> pkgNames = new ArraySet<>();
        if (ris != null) {
            for (ResolveInfo ri : ris) {
                pkgNames.add(ri.activityInfo.packageName);
            }
        }
        return pkgNames;
    }

    private void performBootDexOpt(PackageParser.Package pkg, int curr, int total) {
        if (!isFirstBoot()) {
            try {
                ActivityManagerNative.getDefault().showBootMessage(this.mContext.getResources().getString(R.string.hearing_device_status_loading, Integer.valueOf(curr), Integer.valueOf(total)), DEFAULT_VERIFY_ENABLE);
            } catch (RemoteException e) {
            }
        }
        synchronized (this.mInstallLock) {
            performDexOptLI(pkg, (String[]) null, false, false, DEFAULT_VERIFY_ENABLE);
        }
    }

    public boolean performDexOptIfNeeded(String packageName, String instructionSet) {
        return performDexOpt(packageName, instructionSet, false);
    }

    private static String getPrimaryInstructionSet(ApplicationInfo info) {
        return info.primaryCpuAbi == null ? getPreferredInstructionSet() : VMRuntime.getInstructionSet(info.primaryCpuAbi);
    }

    public boolean performDexOpt(String packageName, String instructionSet, boolean backgroundDexopt) {
        boolean z;
        boolean dexopt = this.mLazyDexOpt || backgroundDexopt;
        boolean updateUsage = !backgroundDexopt;
        if (!dexopt && !updateUsage) {
            return false;
        }
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            if (p == null) {
                return false;
            }
            if (updateUsage) {
                p.mLastPackageUsageTimeInMills = System.currentTimeMillis();
            }
            this.mPackageUsage.write(false);
            if (!dexopt) {
                return false;
            }
            String targetInstructionSet = instructionSet != null ? instructionSet : getPrimaryInstructionSet(p.applicationInfo);
            if (p.mDexOptPerformed.contains(targetInstructionSet)) {
                return false;
            }
            synchronized (this.mInstallLock) {
                String[] instructionSets = {targetInstructionSet};
                z = performDexOptLI(p, instructionSets, false, false, DEFAULT_VERIFY_ENABLE) == 1;
            }
            return z;
        }
    }

    public ArraySet<String> getPackagesThatNeedDexOpt() throws Throwable {
        synchronized (this.mPackages) {
            try {
                ArraySet<String> pkgs = null;
                for (PackageParser.Package p : this.mPackages.values()) {
                    try {
                        if (p.mDexOptPerformed.isEmpty()) {
                            ArraySet<String> pkgs2 = pkgs == null ? new ArraySet<>() : pkgs;
                            pkgs2.add(p.packageName);
                            pkgs = pkgs2;
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                return pkgs;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void shutdown() {
        this.mPackageUsage.write(DEFAULT_VERIFY_ENABLE);
    }

    private void performDexOptLibsLI(ArrayList<String> libs, String[] instructionSets, boolean forceDex, boolean defer, ArraySet<String> done) {
        String libName;
        PackageParser.Package libPkg;
        for (int i = 0; i < libs.size(); i++) {
            synchronized (this.mPackages) {
                libName = libs.get(i);
                SharedLibraryEntry lib = this.mSharedLibraries.get(libName);
                if (lib != null && lib.apk != null) {
                    libPkg = this.mPackages.get(lib.apk);
                } else {
                    libPkg = null;
                }
            }
            if (libPkg != null && !done.contains(libName)) {
                performDexOptLI(libPkg, instructionSets, forceDex, defer, done);
            }
        }
    }

    private int performDexOptLI(PackageParser.Package pkg, String[] targetInstructionSets, boolean forceDex, boolean defer, ArraySet<String> done) {
        String[] instructionSets = targetInstructionSets != null ? targetInstructionSets : getAppDexInstructionSets(pkg.applicationInfo);
        if (done != null) {
            done.add(pkg.packageName);
            if (pkg.usesLibraries != null) {
                performDexOptLibsLI(pkg.usesLibraries, instructionSets, forceDex, defer, done);
            }
            if (pkg.usesOptionalLibraries != null) {
                performDexOptLibsLI(pkg.usesOptionalLibraries, instructionSets, forceDex, defer, done);
            }
        }
        if ((pkg.applicationInfo.flags & 4) == 0) {
            return 0;
        }
        boolean vmSafeMode = (pkg.applicationInfo.flags & 16384) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        List<String> paths = pkg.getAllCodePathsExcludingResourceOnly();
        boolean performedDexOpt = false;
        String[] dexCodeInstructionSets = getDexCodeInstructionSets(instructionSets);
        int len$ = dexCodeInstructionSets.length;
        int i$ = 0;
        loop0: while (true) {
            int i$2 = i$;
            if (i$2 >= len$) {
                return performedDexOpt ? 1 : 0;
            }
            String dexCodeInstructionSet = dexCodeInstructionSets[i$2];
            if (forceDex || !pkg.mDexOptPerformed.contains(dexCodeInstructionSet)) {
                for (String path : paths) {
                    try {
                        byte isDexOptNeeded = DexFile.isDexOptNeededInternal(path, pkg.packageName, dexCodeInstructionSet, defer);
                        if (forceDex || (!defer && isDexOptNeeded == 2)) {
                            Log.i(TAG, "Running dexopt on: " + path + " pkg=" + pkg.applicationInfo.packageName + " isa=" + dexCodeInstructionSet + " vmSafeMode=" + vmSafeMode);
                            int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
                            int ret = this.mInstaller.dexopt(path, sharedGid, !isForwardLocked(pkg) ? DEFAULT_VERIFY_ENABLE : false, pkg.packageName, dexCodeInstructionSet, vmSafeMode);
                            if (ret < 0) {
                                return -1;
                            }
                            performedDexOpt = DEFAULT_VERIFY_ENABLE;
                        } else if (!defer && isDexOptNeeded == 1) {
                            Log.i(TAG, "Running patchoat on: " + pkg.applicationInfo.packageName);
                            int sharedGid2 = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
                            int ret2 = this.mInstaller.patchoat(path, sharedGid2, !isForwardLocked(pkg) ? DEFAULT_VERIFY_ENABLE : false, pkg.packageName, dexCodeInstructionSet);
                            if (ret2 < 0) {
                                return -1;
                            }
                            performedDexOpt = DEFAULT_VERIFY_ENABLE;
                        }
                        if (defer && isDexOptNeeded != 0) {
                            break loop0;
                        }
                    } catch (FileNotFoundException e) {
                        Slog.w(TAG, "Apk not found for dexopt: " + path);
                        return -1;
                    } catch (Exception e2) {
                        Slog.w(TAG, "Exception when doing dexopt : ", e2);
                        return -1;
                    } catch (StaleDexCacheError e3) {
                        Slog.w(TAG, "StaleDexCacheError when reading apk: " + path, e3);
                        return -1;
                    } catch (IOException e4) {
                        Slog.w(TAG, "IOException reading apk: " + path, e4);
                        return -1;
                    }
                }
                pkg.mDexOptPerformed.add(dexCodeInstructionSet);
            }
            i$ = i$2 + 1;
        }
    }

    private static String[] getAppDexInstructionSets(ApplicationInfo info) {
        if (info.primaryCpuAbi != null) {
            if (info.secondaryCpuAbi != null) {
                return new String[]{VMRuntime.getInstructionSet(info.primaryCpuAbi), VMRuntime.getInstructionSet(info.secondaryCpuAbi)};
            }
            return new String[]{VMRuntime.getInstructionSet(info.primaryCpuAbi)};
        }
        return new String[]{getPreferredInstructionSet()};
    }

    private static String[] getAppDexInstructionSets(PackageSetting ps) {
        if (ps.primaryCpuAbiString != null) {
            if (ps.secondaryCpuAbiString != null) {
                return new String[]{VMRuntime.getInstructionSet(ps.primaryCpuAbiString), VMRuntime.getInstructionSet(ps.secondaryCpuAbiString)};
            }
            return new String[]{VMRuntime.getInstructionSet(ps.primaryCpuAbiString)};
        }
        return new String[]{getPreferredInstructionSet()};
    }

    private static String getPreferredInstructionSet() {
        if (sPreferredInstructionSet == null) {
            sPreferredInstructionSet = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[0]);
        }
        return sPreferredInstructionSet;
    }

    private static List<String> getAllInstructionSets() {
        String[] allAbis = Build.SUPPORTED_ABIS;
        List<String> allInstructionSets = new ArrayList<>(allAbis.length);
        for (String abi : allAbis) {
            String instructionSet = VMRuntime.getInstructionSet(abi);
            if (!allInstructionSets.contains(instructionSet)) {
                allInstructionSets.add(instructionSet);
            }
        }
        return allInstructionSets;
    }

    private static String getDexCodeInstructionSet(String sharedLibraryIsa) {
        String dexCodeIsa = SystemProperties.get("ro.dalvik.vm.isa." + sharedLibraryIsa);
        return dexCodeIsa.isEmpty() ? sharedLibraryIsa : dexCodeIsa;
    }

    private static String[] getDexCodeInstructionSets(String[] instructionSets) {
        ArraySet<String> dexCodeInstructionSets = new ArraySet<>(instructionSets.length);
        for (String instructionSet : instructionSets) {
            dexCodeInstructionSets.add(getDexCodeInstructionSet(instructionSet));
        }
        return (String[]) dexCodeInstructionSets.toArray(new String[dexCodeInstructionSets.size()]);
    }

    public static String[] getAllDexCodeInstructionSets() {
        String[] supportedInstructionSets = new String[Build.SUPPORTED_ABIS.length];
        for (int i = 0; i < supportedInstructionSets.length; i++) {
            String abi = Build.SUPPORTED_ABIS[i];
            supportedInstructionSets[i] = VMRuntime.getInstructionSet(abi);
        }
        return getDexCodeInstructionSets(supportedInstructionSets);
    }

    public void forceDexOpt(String packageName) {
        PackageParser.Package pkg;
        enforceSystemOrRoot("forceDexOpt");
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Missing package: " + packageName);
            }
        }
        synchronized (this.mInstallLock) {
            String[] instructionSets = {getPrimaryInstructionSet(pkg.applicationInfo)};
            int res = performDexOptLI(pkg, instructionSets, DEFAULT_VERIFY_ENABLE, false, DEFAULT_VERIFY_ENABLE);
            if (res != 1) {
                throw new IllegalStateException("Failed to dexopt: " + res);
            }
        }
    }

    private int performDexOptLI(PackageParser.Package pkg, String[] instructionSets, boolean forceDex, boolean defer, boolean inclDependencies) {
        ArraySet<String> done;
        if (inclDependencies && (pkg.usesLibraries != null || pkg.usesOptionalLibraries != null)) {
            done = new ArraySet<>();
            done.add(pkg.packageName);
        } else {
            done = null;
        }
        return performDexOptLI(pkg, instructionSets, forceDex, defer, done);
    }

    private boolean verifyPackageUpdateLPr(PackageSetting oldPkg, PackageParser.Package newPkg) {
        if ((oldPkg.pkgFlags & 1) == 0) {
            Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package not in system partition");
            return false;
        }
        if (this.mPackages.get(oldPkg.name) != null) {
            Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package still exists");
            return false;
        }
        return DEFAULT_VERIFY_ENABLE;
    }

    File getDataPathForUser(int userId) {
        return new File(this.mUserAppDataDir.getAbsolutePath() + File.separator + userId);
    }

    private File getDataPathForPackage(String packageName, int userId) {
        return userId == 0 ? new File(this.mAppDataDir, packageName) : new File(this.mUserAppDataDir.getAbsolutePath() + File.separator + userId + File.separator + packageName);
    }

    private int createDataDirsLI(String packageName, int uid, String seinfo) {
        int[] users = sUserManager.getUserIds();
        int res = this.mInstaller.install(packageName, uid, uid, seinfo);
        if (res < 0) {
            return res;
        }
        for (int user : users) {
            if (user != 0 && (res = this.mInstaller.createUserData(packageName, UserHandle.getUid(user, uid), user, seinfo)) < 0) {
                return res;
            }
        }
        return res;
    }

    private int removeDataDirsLI(String packageName) {
        int[] users = sUserManager.getUserIds();
        int res = 0;
        for (int user : users) {
            int resInner = this.mInstaller.remove(packageName, user);
            if (resInner < 0) {
                res = resInner;
            }
        }
        return res;
    }

    private int deleteCodeCacheDirsLI(String packageName) {
        int[] users = sUserManager.getUserIds();
        int res = 0;
        for (int user : users) {
            int resInner = this.mInstaller.deleteCodeCacheFiles(packageName, user);
            if (resInner < 0) {
                res = resInner;
            }
        }
        return res;
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
        if (p != null) {
            usesLibraryFiles.addAll(p.getAllCodePaths());
        }
    }

    private void updateSharedLibrariesLPw(PackageParser.Package pkg, PackageParser.Package changingLib) throws PackageManagerException {
        if (pkg.usesLibraries != null || pkg.usesOptionalLibraries != null) {
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
    }

    private static boolean hasString(List<String> list, List<String> which) {
        if (list == null) {
            return false;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = which.size() - 1; j >= 0; j--) {
                if (which.get(j).equals(list.get(i))) {
                    return DEFAULT_VERIFY_ENABLE;
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

    private PackageParser.Package scanPackageLI(PackageParser.Package pkg, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        boolean success = false;
        try {
            PackageParser.Package res = scanPackageDirtyLI(pkg, parseFlags, scanFlags, currentTime, user);
            success = DEFAULT_VERIFY_ENABLE;
            return res;
        } finally {
            if (!success && (scanFlags & 1024) != 0) {
                removeDataDirsLI(pkg.packageName);
            }
        }
    }

    private PackageParser.Package scanPackageDirtyLI(PackageParser.Package r85, int i, int i2, long j, UserHandle userHandle) throws PackageManagerException {
        PackageSetting packageLPw;
        int iCopyNativeBinariesForSupportedAbi;
        PackageParser.Provider provider;
        File file = new File(r85.codePath);
        if (r85.applicationInfo.getCodePath() == null || r85.applicationInfo.getResourcePath() == null) {
            throw new PackageManagerException(-2, "Code and resource paths haven't been set correctly");
        }
        if ((i & 1) != 0) {
            r85.applicationInfo.flags |= 1;
        } else {
            r85.coreApp = false;
        }
        if ((i & 128) != 0) {
            r85.applicationInfo.flags |= 1073741824;
        }
        if (this.mCustomResolverComponentName != null && this.mCustomResolverComponentName.getPackageName().equals(r85.packageName)) {
            setUpCustomResolverActivity(r85);
        }
        if (r85.packageName.equals("android")) {
            synchronized (this.mPackages) {
                if (this.mAndroidApplication != null) {
                    Slog.w(TAG, "*************************************************");
                    Slog.w(TAG, "Core android package being redefined.  Skipping.");
                    Slog.w(TAG, " file=" + file);
                    Slog.w(TAG, "*************************************************");
                    throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
                }
                this.mPlatformPackage = r85;
                r85.mVersionCode = this.mSdkVersion;
                this.mAndroidApplication = r85.applicationInfo;
                if (!this.mResolverReplaced) {
                    this.mResolveActivity.applicationInfo = this.mAndroidApplication;
                    this.mResolveActivity.name = ResolverActivity.class.getName();
                    this.mResolveActivity.packageName = this.mAndroidApplication.packageName;
                    this.mResolveActivity.processName = "system:ui";
                    this.mResolveActivity.launchMode = 0;
                    this.mResolveActivity.documentLaunchMode = 3;
                    this.mResolveActivity.flags = 32;
                    this.mResolveActivity.theme = R.style.Widget.DeviceDefault.Light.FragmentBreadCrumbs;
                    this.mResolveActivity.exported = DEFAULT_VERIFY_ENABLE;
                    this.mResolveActivity.enabled = DEFAULT_VERIFY_ENABLE;
                    this.mResolveInfo.activityInfo = this.mResolveActivity;
                    this.mResolveInfo.priority = 0;
                    this.mResolveInfo.preferredOrder = 0;
                    this.mResolveInfo.match = 0;
                    this.mResolveComponentName = new ComponentName(this.mAndroidApplication.packageName, this.mResolveActivity.name);
                }
            }
        }
        if (this.mPackages.containsKey(r85.packageName) || this.mSharedLibraries.containsKey(r85.packageName)) {
            throw new PackageManagerException(-5, "Application package " + r85.packageName + " already installed.  Skipping duplicate.");
        }
        File file2 = new File(r85.applicationInfo.getCodePath());
        File file3 = new File(r85.applicationInfo.getResourcePath());
        SharedUserSetting sharedUserLPw = null;
        if (!isSystemApp(r85)) {
            r85.mOriginalPackages = null;
            r85.mRealPackage = null;
            r85.mAdoptPermissions = null;
        }
        synchronized (this.mPackages) {
            if (r85.mSharedUserId != null && (sharedUserLPw = this.mSettings.getSharedUserLPw(r85.mSharedUserId, 0, DEFAULT_VERIFY_ENABLE)) == null) {
                throw new PackageManagerException(-4, "Creating application package " + r85.packageName + " for shared user failed");
            }
            PackageSetting packageSettingPeekPackageLPr = null;
            String str = null;
            if (r85.mOriginalPackages != null) {
                String str2 = this.mSettings.mRenamedPackages.get(r85.mRealPackage);
                if (r85.mOriginalPackages.contains(str2)) {
                    str = r85.mRealPackage;
                    if (!r85.packageName.equals(str2)) {
                        r85.setPackageName(str2);
                    }
                } else {
                    for (int size = r85.mOriginalPackages.size() - 1; size >= 0; size--) {
                        packageSettingPeekPackageLPr = this.mSettings.peekPackageLPr((String) r85.mOriginalPackages.get(size));
                        if (packageSettingPeekPackageLPr != null) {
                            if (!verifyPackageUpdateLPr(packageSettingPeekPackageLPr, r85)) {
                                packageSettingPeekPackageLPr = null;
                            } else {
                                if (packageSettingPeekPackageLPr.sharedUser == null || packageSettingPeekPackageLPr.sharedUser.name.equals(r85.mSharedUserId)) {
                                    break;
                                }
                                Slog.w(TAG, "Unable to migrate data from " + packageSettingPeekPackageLPr.name + " to " + r85.packageName + ": old uid " + packageSettingPeekPackageLPr.sharedUser.name + " differs from " + r85.mSharedUserId);
                                packageSettingPeekPackageLPr = null;
                            }
                        }
                    }
                }
            }
            if (this.mTransferedPackages.contains(r85.packageName)) {
                Slog.w(TAG, "Package " + r85.packageName + " was transferred to another, but its .apk remains");
            }
            packageLPw = this.mSettings.getPackageLPw(r85, packageSettingPeekPackageLPr, str, sharedUserLPw, file2, file3, r85.applicationInfo.nativeLibraryRootDir, r85.applicationInfo.primaryCpuAbi, r85.applicationInfo.secondaryCpuAbi, r85.applicationInfo.flags, userHandle, false);
            if (packageLPw == null) {
                throw new PackageManagerException(-4, "Creating application package " + r85.packageName + " failed");
            }
            if (packageLPw.origPackage != null) {
                r85.setPackageName(packageSettingPeekPackageLPr.name);
                reportSettingsProblem(5, "New package " + packageLPw.realName + " renamed to replace old package " + packageLPw.name);
                this.mTransferedPackages.add(packageSettingPeekPackageLPr.name);
                packageLPw.origPackage = null;
            }
            if (str != null) {
                this.mTransferedPackages.add(r85.packageName);
            }
            if (this.mSettings.isDisabledSystemPackageLPr(r85.packageName)) {
                r85.applicationInfo.flags |= 128;
            }
            if ((i & 64) == 0) {
                updateSharedLibrariesLPw(r85, null);
            }
            if (this.mFoundPolicyFile) {
                SELinuxMMAC.assignSeinfoValue(r85);
            }
            r85.applicationInfo.uid = packageLPw.appId;
            r85.mExtras = packageLPw;
            if (!packageLPw.keySetData.isUsingUpgradeKeySets() || packageLPw.sharedUser != null) {
                try {
                    verifySignaturesLP(packageLPw, r85);
                    packageLPw.signatures.mSignatures = r85.mSignatures;
                } catch (PackageManagerException e) {
                    if ((i & 64) == 0) {
                        throw e;
                    }
                    packageLPw.signatures.mSignatures = r85.mSignatures;
                    if (packageLPw.sharedUser != null && compareSignatures(packageLPw.sharedUser.signatures.mSignatures, r85.mSignatures) != 0) {
                        throw new PackageManagerException(-104, "Signature mismatch for shared user : " + packageLPw.sharedUser);
                    }
                    reportSettingsProblem(5, "System package " + r85.packageName + " signature changed; retaining data.");
                }
            } else {
                if (!checkUpgradeKeySetLP(packageLPw, r85)) {
                    throw new PackageManagerException(-7, "Package " + r85.packageName + " upgrade keys do not match the previously installed version");
                }
                packageLPw.signatures.mSignatures = r85.mSignatures;
            }
            if ((i2 & 16) != 0) {
                int size2 = r85.providers.size();
                for (int i3 = 0; i3 < size2; i3++) {
                    PackageParser.Provider provider2 = (PackageParser.Provider) r85.providers.get(i3);
                    if (provider2.info.authority != null) {
                        String[] strArrSplit = provider2.info.authority.split(";");
                        for (int i4 = 0; i4 < strArrSplit.length; i4++) {
                            if (this.mProvidersByAuthority.containsKey(strArrSplit[i4])) {
                                PackageParser.Provider provider3 = this.mProvidersByAuthority.get(strArrSplit[i4]);
                                throw new PackageManagerException(-13, "Can't install because provider name " + strArrSplit[i4] + " (in package " + r85.applicationInfo.packageName + ") is already used by " + ((provider3 == null || provider3.getComponentName() == null) ? "?" : provider3.getComponentName().getPackageName()));
                            }
                        }
                    }
                }
            }
            if (r85.mAdoptPermissions != null) {
                for (int size3 = r85.mAdoptPermissions.size() - 1; size3 >= 0; size3--) {
                    String str3 = (String) r85.mAdoptPermissions.get(size3);
                    PackageSetting packageSettingPeekPackageLPr2 = this.mSettings.peekPackageLPr(str3);
                    if (packageSettingPeekPackageLPr2 != null && verifyPackageUpdateLPr(packageSettingPeekPackageLPr2, r85)) {
                        Slog.i(TAG, "Adopting permissions from " + str3 + " to " + r85.packageName);
                        this.mSettings.transferPermissionsLPw(str3, r85.packageName);
                    }
                }
            }
        }
        String str4 = r85.packageName;
        long jLastModified = file.lastModified();
        boolean z = (i2 & 4) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        r85.applicationInfo.processName = fixProcessName(r85.applicationInfo.packageName, r85.applicationInfo.processName, r85.applicationInfo.uid);
        if (this.mPlatformPackage == r85) {
            r85.applicationInfo.dataDir = new File(Environment.getDataDirectory(), "system").getPath();
        } else {
            File dataPathForPackage = getDataPathForPackage(r85.packageName, 0);
            boolean z2 = false;
            if (dataPathForPackage.exists()) {
                int i5 = 0;
                try {
                    i5 = Os.stat(dataPathForPackage.getPath()).st_uid;
                } catch (ErrnoException e2) {
                    Slog.e(TAG, "Couldn't stat path " + dataPathForPackage.getPath(), e2);
                }
                if (i5 != r85.applicationInfo.uid) {
                    boolean z3 = false;
                    if (i5 == 0 && this.mInstaller.fixUid(str4, r85.applicationInfo.uid, r85.applicationInfo.uid) >= 0) {
                        z3 = DEFAULT_VERIFY_ENABLE;
                        reportSettingsProblem(5, "Package " + r85.packageName + " unexpectedly changed to uid 0; recovered to " + r85.applicationInfo.uid);
                    }
                    if (!z3 && ((i & 1) != 0 || (i2 & 256) != 0)) {
                        if (removeDataDirsLI(str4) >= 0) {
                            String str5 = (i & 1) != 0 ? "System package " : "Third party package ";
                            reportSettingsProblem(5, str5 + r85.packageName + " has changed from uid: " + i5 + " to " + r85.applicationInfo.uid + "; old data erased");
                            z3 = DEFAULT_VERIFY_ENABLE;
                            if (createDataDirsLI(str4, r85.applicationInfo.uid, r85.applicationInfo.seinfo) == -1) {
                                String str6 = str5 + r85.packageName + " could not have data directory re-created after delete.";
                                reportSettingsProblem(5, str6);
                                throw new PackageManagerException(-4, str6);
                            }
                        }
                        if (!z3) {
                            this.mHasSystemUidErrors = DEFAULT_VERIFY_ENABLE;
                        }
                    } else if (!z3) {
                        throw new PackageManagerException(-24, "scanPackageLI");
                    }
                    if (!z3) {
                        r85.applicationInfo.dataDir = "/mismatched_uid/settings_" + r85.applicationInfo.uid + "/fs_" + i5;
                        r85.applicationInfo.nativeLibraryDir = r85.applicationInfo.dataDir;
                        r85.applicationInfo.nativeLibraryRootDir = r85.applicationInfo.dataDir;
                        String str7 = "Package " + r85.packageName + " has mismatched uid: " + i5 + " on disk, " + r85.applicationInfo.uid + " in settings";
                        synchronized (this.mPackages) {
                            this.mSettings.mReadMessages.append(str7);
                            this.mSettings.mReadMessages.append('\n');
                            z2 = DEFAULT_VERIFY_ENABLE;
                            if (!packageLPw.uidError) {
                                reportSettingsProblem(6, str7);
                            }
                        }
                    }
                }
                r85.applicationInfo.dataDir = dataPathForPackage.getPath();
                if (this.mShouldRestoreconData) {
                    Slog.i(TAG, "SELinux relabeling of " + r85.packageName + " issued.");
                    this.mInstaller.restoreconData(r85.packageName, r85.applicationInfo.seinfo, r85.applicationInfo.uid);
                }
            } else {
                int iCreateDataDirsLI = createDataDirsLI(str4, r85.applicationInfo.uid, r85.applicationInfo.seinfo);
                if (iCreateDataDirsLI < 0) {
                    throw new PackageManagerException(-4, "Unable to create data dirs [errorCode=" + iCreateDataDirsLI + "]");
                }
                if (dataPathForPackage.exists()) {
                    r85.applicationInfo.dataDir = dataPathForPackage.getPath();
                } else {
                    Slog.w(TAG, "Unable to create data directory: " + dataPathForPackage);
                    r85.applicationInfo.dataDir = null;
                }
            }
            packageLPw.uidError = z2;
        }
        file.getPath();
        r85.applicationInfo.getCodePath();
        String strDeriveAbiOverride = deriveAbiOverride(r85.cpuAbiOverride, packageLPw);
        if (isSystemApp(r85) && !isUpdatedSystemApp(r85)) {
            setBundledAppAbisAndRoots(r85, packageLPw);
            if (r85.applicationInfo.primaryCpuAbi == null && r85.applicationInfo.secondaryCpuAbi == null && Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                NativeLibraryHelper.Handle handleCreate = null;
                try {
                    handleCreate = NativeLibraryHelper.Handle.create(file);
                    if (NativeLibraryHelper.hasRenderscriptBitcode(handleCreate)) {
                        r85.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
                    }
                } catch (IOException e3) {
                    Slog.w(TAG, "Error scanning system app : " + e3);
                } finally {
                }
            }
            setNativeLibraryPaths(r85);
        } else {
            setNativeLibraryPaths(r85);
            boolean z4 = (isForwardLocked(r85) || isExternal(r85)) ? DEFAULT_VERIFY_ENABLE : false;
            String str8 = r85.applicationInfo.nativeLibraryRootDir;
            boolean z5 = r85.applicationInfo.nativeLibraryRootRequiresIsa;
            AutoCloseable autoCloseable = null;
            try {
                NativeLibraryHelper.Handle handleCreate2 = NativeLibraryHelper.Handle.create(file);
                File file4 = new File(str8);
                r85.applicationInfo.primaryCpuAbi = null;
                r85.applicationInfo.secondaryCpuAbi = null;
                if (isMultiArch(r85.applicationInfo)) {
                    if (r85.cpuAbiOverride != null && !INSTALL_PACKAGE_SUFFIX.equals(r85.cpuAbiOverride)) {
                        Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                    }
                    int iCopyNativeBinariesForSupportedAbi2 = -114;
                    int iCopyNativeBinariesForSupportedAbi3 = -114;
                    if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                        if (z4) {
                            iCopyNativeBinariesForSupportedAbi2 = NativeLibraryHelper.findSupportedAbi(handleCreate2, Build.SUPPORTED_32_BIT_ABIS);
                        } else {
                            iCopyNativeBinariesForSupportedAbi2 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handleCreate2, file4, Build.SUPPORTED_32_BIT_ABIS, z5);
                        }
                    }
                    maybeThrowExceptionForMultiArchCopy("Error unpackaging 32 bit native libs for multiarch app.", iCopyNativeBinariesForSupportedAbi2);
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                        if (z4) {
                            iCopyNativeBinariesForSupportedAbi3 = NativeLibraryHelper.findSupportedAbi(handleCreate2, Build.SUPPORTED_64_BIT_ABIS);
                        } else {
                            iCopyNativeBinariesForSupportedAbi3 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handleCreate2, file4, Build.SUPPORTED_64_BIT_ABIS, z5);
                        }
                    }
                    maybeThrowExceptionForMultiArchCopy("Error unpackaging 64 bit native libs for multiarch app.", iCopyNativeBinariesForSupportedAbi3);
                    if (iCopyNativeBinariesForSupportedAbi3 >= 0) {
                        r85.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[iCopyNativeBinariesForSupportedAbi3];
                    }
                    if (iCopyNativeBinariesForSupportedAbi2 >= 0) {
                        String str9 = Build.SUPPORTED_32_BIT_ABIS[iCopyNativeBinariesForSupportedAbi2];
                        if (iCopyNativeBinariesForSupportedAbi3 >= 0) {
                            r85.applicationInfo.secondaryCpuAbi = str9;
                        } else {
                            r85.applicationInfo.primaryCpuAbi = str9;
                        }
                    }
                } else {
                    String[] strArr = strDeriveAbiOverride != null ? new String[]{strDeriveAbiOverride} : Build.SUPPORTED_ABIS;
                    boolean z6 = false;
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && strDeriveAbiOverride == null && NativeLibraryHelper.hasRenderscriptBitcode(handleCreate2)) {
                        strArr = Build.SUPPORTED_32_BIT_ABIS;
                        z6 = DEFAULT_VERIFY_ENABLE;
                    }
                    if (z4) {
                        iCopyNativeBinariesForSupportedAbi = NativeLibraryHelper.findSupportedAbi(handleCreate2, strArr);
                    } else {
                        iCopyNativeBinariesForSupportedAbi = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handleCreate2, file4, strArr, z5);
                    }
                    if (iCopyNativeBinariesForSupportedAbi < 0 && iCopyNativeBinariesForSupportedAbi != -114) {
                        throw new PackageManagerException(-110, "Error unpackaging native libs for app, errorCode=" + iCopyNativeBinariesForSupportedAbi);
                    }
                    if (iCopyNativeBinariesForSupportedAbi >= 0) {
                        r85.applicationInfo.primaryCpuAbi = strArr[iCopyNativeBinariesForSupportedAbi];
                    } else if (iCopyNativeBinariesForSupportedAbi == -114 && strDeriveAbiOverride != null) {
                        r85.applicationInfo.primaryCpuAbi = strDeriveAbiOverride;
                    } else if (z6) {
                        r85.applicationInfo.primaryCpuAbi = strArr[0];
                    }
                }
            } catch (IOException e4) {
                Slog.e(TAG, "Unable to get canonical file " + e4.toString());
            } finally {
            }
            setNativeLibraryPaths(r85);
            int[] userIds = sUserManager.getUserIds();
            synchronized (this.mInstallLock) {
                if (r85.applicationInfo.primaryCpuAbi != null && !VMRuntime.is64BitAbi(r85.applicationInfo.primaryCpuAbi)) {
                    String str10 = r85.applicationInfo.nativeLibraryDir;
                    for (int i6 : userIds) {
                        if (this.mInstaller.linkNativeLibraryDirectory(r85.packageName, str10, i6) < 0) {
                            throw new PackageManagerException(-110, "Failed linking native library dir (user=" + i6 + ")");
                        }
                    }
                }
            }
        }
        if (this.mPlatformPackage == r85) {
            r85.applicationInfo.primaryCpuAbi = VMRuntime.getRuntime().is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        }
        packageLPw.primaryCpuAbiString = r85.applicationInfo.primaryCpuAbi;
        packageLPw.secondaryCpuAbiString = r85.applicationInfo.secondaryCpuAbi;
        packageLPw.cpuAbiOverrideString = strDeriveAbiOverride;
        r85.cpuAbiOverride = strDeriveAbiOverride;
        packageLPw.legacyNativeLibraryPathString = r85.applicationInfo.nativeLibraryRootDir;
        if ((i2 & 256) == 0 && packageLPw.sharedUser != null) {
            adjustCpuAbisForSharedUserLPw(packageLPw.sharedUser.packages, r85, z, (i2 & 128) != 0 ? DEFAULT_VERIFY_ENABLE : false);
        }
        if ((i2 & 2) == 0) {
            if (performDexOptLI(r85, (String[]) null, z, (i2 & 128) != 0 ? DEFAULT_VERIFY_ENABLE : false, false) == -1) {
                throw new PackageManagerException(-11, "scanPackageLI");
            }
        }
        if (this.mFactoryTest && r85.requestedPermissions.contains("android.permission.FACTORY_TEST")) {
            r85.applicationInfo.flags |= 16;
        }
        ArrayList<PackageParser.Package> arrayListUpdateAllSharedLibrariesLPw = null;
        synchronized (this.mPackages) {
            if ((r85.applicationInfo.flags & 1) != 0 && r85.libraryNames != null) {
                for (int i7 = 0; i7 < r85.libraryNames.size(); i7++) {
                    String str11 = (String) r85.libraryNames.get(i7);
                    boolean z7 = false;
                    if (isUpdatedSystemApp(r85)) {
                        PackageSetting disabledSystemPkgLPr = this.mSettings.getDisabledSystemPkgLPr(r85.packageName);
                        if (disabledSystemPkgLPr.pkg != null && disabledSystemPkgLPr.pkg.libraryNames != null) {
                            int i8 = 0;
                            while (true) {
                                if (i8 >= disabledSystemPkgLPr.pkg.libraryNames.size()) {
                                    break;
                                }
                                if (!str11.equals(disabledSystemPkgLPr.pkg.libraryNames.get(i8))) {
                                    i8++;
                                } else {
                                    z7 = DEFAULT_VERIFY_ENABLE;
                                    break;
                                }
                            }
                        }
                    } else {
                        z7 = DEFAULT_VERIFY_ENABLE;
                    }
                    if (z7) {
                        if (!this.mSharedLibraries.containsKey(str11)) {
                            this.mSharedLibraries.put(str11, new SharedLibraryEntry(null, r85.packageName));
                        } else if (!str11.equals(r85.packageName)) {
                            Slog.w(TAG, "Package " + r85.packageName + " library " + str11 + " already exists; skipping");
                        }
                    } else {
                        Slog.w(TAG, "Package " + r85.packageName + " declares lib " + str11 + " that is not declared on system image; skipping");
                    }
                }
                if ((i2 & 256) == 0) {
                    arrayListUpdateAllSharedLibrariesLPw = updateAllSharedLibrariesLPw(r85);
                }
            }
        }
        if (arrayListUpdateAllSharedLibrariesLPw != null && (i2 & 2) == 0) {
            for (int i9 = 0; i9 < arrayListUpdateAllSharedLibrariesLPw.size(); i9++) {
                if (performDexOptLI(arrayListUpdateAllSharedLibrariesLPw.get(i9), (String[]) null, z, (i2 & 128) != 0 ? DEFAULT_VERIFY_ENABLE : false, false) == -1) {
                    throw new PackageManagerException(-11, "scanPackageLI failed to dexopt clientLibPkgs");
                }
            }
        }
        if ((i2 & 2048) != 0) {
            killApplication(r85.applicationInfo.packageName, r85.applicationInfo.uid, "update pkg");
        }
        if (arrayListUpdateAllSharedLibrariesLPw != null) {
            for (int i10 = 0; i10 < arrayListUpdateAllSharedLibrariesLPw.size(); i10++) {
                PackageParser.Package r12 = arrayListUpdateAllSharedLibrariesLPw.get(i10);
                killApplication(r12.applicationInfo.packageName, r12.applicationInfo.uid, "update lib");
            }
        }
        synchronized (this.mPackages) {
            this.mSettings.insertPackageSettingLPw(packageLPw, r85);
            this.mPackages.put(r85.applicationInfo.packageName, r85);
            Iterator<PackageCleanItem> it = this.mSettings.mPackagesToBeCleaned.iterator();
            while (it.hasNext()) {
                if (str4.equals(it.next().packageName)) {
                    it.remove();
                }
            }
            if (j != 0) {
                if (packageLPw.firstInstallTime == 0) {
                    packageLPw.lastUpdateTime = j;
                    packageLPw.firstInstallTime = j;
                } else if ((i2 & 64) != 0) {
                    packageLPw.lastUpdateTime = j;
                }
            } else if (packageLPw.firstInstallTime == 0) {
                packageLPw.lastUpdateTime = jLastModified;
                packageLPw.firstInstallTime = jLastModified;
            } else if ((i & 64) != 0 && jLastModified != packageLPw.timeStamp) {
                packageLPw.lastUpdateTime = jLastModified;
            }
            KeySetManagerService keySetManagerService = this.mSettings.mKeySetManagerService;
            try {
                keySetManagerService.removeAppKeySetDataLPw(r85.packageName);
                keySetManagerService.addSigningKeySetToPackageLPw(r85.packageName, r85.mSigningKeys);
                if (r85.mKeySetMapping != null) {
                    for (Map.Entry entry : r85.mKeySetMapping.entrySet()) {
                        if (entry.getValue() != null) {
                            keySetManagerService.addDefinedKeySetToPackageLPw(r85.packageName, (ArraySet) entry.getValue(), (String) entry.getKey());
                        }
                    }
                    if (r85.mUpgradeKeySets != null) {
                        Iterator it2 = r85.mUpgradeKeySets.iterator();
                        while (it2.hasNext()) {
                            keySetManagerService.addUpgradeKeySetToPackageLPw(r85.packageName, (String) it2.next());
                        }
                    }
                }
            } catch (IllegalArgumentException e5) {
                Slog.e(TAG, "Could not add KeySet to malformed package" + r85.packageName, e5);
            } catch (NullPointerException e6) {
                Slog.e(TAG, "Could not add KeySet to " + r85.packageName, e6);
            }
            int size4 = r85.providers.size();
            StringBuilder sb = null;
            for (int i11 = 0; i11 < size4; i11++) {
                PackageParser.Provider provider4 = (PackageParser.Provider) r85.providers.get(i11);
                provider4.info.processName = fixProcessName(r85.applicationInfo.processName, provider4.info.processName, r85.applicationInfo.uid);
                this.mProviders.addProvider(provider4);
                provider4.syncable = provider4.info.isSyncable;
                if (provider4.info.authority != null) {
                    String[] strArrSplit2 = provider4.info.authority.split(";");
                    provider4.info.authority = null;
                    int i12 = 0;
                    PackageParser.Provider provider5 = provider4;
                    while (i12 < strArrSplit2.length) {
                        if (i12 == 1 && provider5.syncable) {
                            provider = new PackageParser.Provider(provider5);
                            provider.syncable = false;
                        } else {
                            provider = provider5;
                        }
                        if (!this.mProvidersByAuthority.containsKey(strArrSplit2[i12])) {
                            this.mProvidersByAuthority.put(strArrSplit2[i12], provider);
                            if (provider.info.authority == null) {
                                provider.info.authority = strArrSplit2[i12];
                            } else {
                                provider.info.authority += ";" + strArrSplit2[i12];
                            }
                        } else {
                            PackageParser.Provider provider6 = this.mProvidersByAuthority.get(strArrSplit2[i12]);
                            Slog.w(TAG, "Skipping provider name " + strArrSplit2[i12] + " (in package " + r85.applicationInfo.packageName + "): name already used by " + ((provider6 == null || provider6.getComponentName() == null) ? "?" : provider6.getComponentName().getPackageName()));
                        }
                        i12++;
                        provider5 = provider;
                    }
                    provider4 = provider5;
                }
                if ((i & 2) != 0) {
                    if (sb == null) {
                        sb = new StringBuilder(256);
                    } else {
                        sb.append(' ');
                    }
                    sb.append(provider4.info.name);
                }
            }
            if (sb != null) {
            }
            int size5 = r85.services.size();
            StringBuilder sb2 = null;
            for (int i13 = 0; i13 < size5; i13++) {
                PackageParser.Service service = (PackageParser.Service) r85.services.get(i13);
                service.info.processName = fixProcessName(r85.applicationInfo.processName, service.info.processName, r85.applicationInfo.uid);
                this.mServices.addService(service);
                if ((i & 2) != 0) {
                    if (sb2 == null) {
                        sb2 = new StringBuilder(256);
                    } else {
                        sb2.append(' ');
                    }
                    sb2.append(service.info.name);
                }
            }
            if (sb2 != null) {
            }
            int size6 = r85.receivers.size();
            StringBuilder sb3 = null;
            for (int i14 = 0; i14 < size6; i14++) {
                PackageParser.Activity activity = (PackageParser.Activity) r85.receivers.get(i14);
                activity.info.processName = fixProcessName(r85.applicationInfo.processName, activity.info.processName, r85.applicationInfo.uid);
                this.mReceivers.addActivity(activity, "receiver");
                if ((i & 2) != 0) {
                    if (sb3 == null) {
                        sb3 = new StringBuilder(256);
                    } else {
                        sb3.append(' ');
                    }
                    sb3.append(activity.info.name);
                }
            }
            if (sb3 != null) {
            }
            int size7 = r85.activities.size();
            StringBuilder sb4 = null;
            for (int i15 = 0; i15 < size7; i15++) {
                PackageParser.Activity activity2 = (PackageParser.Activity) r85.activities.get(i15);
                activity2.info.processName = fixProcessName(r85.applicationInfo.processName, activity2.info.processName, r85.applicationInfo.uid);
                this.mActivities.addActivity(activity2, "activity");
                if ((i & 2) != 0) {
                    if (sb4 == null) {
                        sb4 = new StringBuilder(256);
                    } else {
                        sb4.append(' ');
                    }
                    sb4.append(activity2.info.name);
                }
            }
            if (sb4 != null) {
            }
            int size8 = r85.permissionGroups.size();
            StringBuilder sb5 = null;
            for (int i16 = 0; i16 < size8; i16++) {
                PackageParser.PermissionGroup permissionGroup = (PackageParser.PermissionGroup) r85.permissionGroups.get(i16);
                PackageParser.PermissionGroup permissionGroup2 = this.mPermissionGroups.get(permissionGroup.info.name);
                if (permissionGroup2 == null) {
                    this.mPermissionGroups.put(permissionGroup.info.name, permissionGroup);
                    if ((i & 2) != 0) {
                        if (sb5 == null) {
                            sb5 = new StringBuilder(256);
                        } else {
                            sb5.append(' ');
                        }
                        sb5.append(permissionGroup.info.name);
                    }
                } else {
                    Slog.w(TAG, "Permission group " + permissionGroup.info.name + " from package " + permissionGroup.info.packageName + " ignored: original from " + permissionGroup2.info.packageName);
                    if ((i & 2) != 0) {
                        if (sb5 == null) {
                            sb5 = new StringBuilder(256);
                        } else {
                            sb5.append(' ');
                        }
                        sb5.append("DUP:");
                        sb5.append(permissionGroup.info.name);
                    }
                }
            }
            if (sb5 != null) {
            }
            int size9 = r85.permissions.size();
            StringBuilder sb6 = null;
            for (int i17 = 0; i17 < size9; i17++) {
                PackageParser.Permission permission = (PackageParser.Permission) r85.permissions.get(i17);
                ArrayMap<String, BasePermission> arrayMap = permission.tree ? this.mSettings.mPermissionTrees : this.mSettings.mPermissions;
                permission.group = this.mPermissionGroups.get(permission.info.group);
                if (permission.info.group == null || permission.group != null) {
                    BasePermission basePermission = arrayMap.get(permission.info.name);
                    if (basePermission != null && !Objects.equals(basePermission.sourcePackage, permission.info.packageName)) {
                        boolean z8 = (basePermission.perm == null || !isSystemApp(basePermission.perm.owner)) ? false : DEFAULT_VERIFY_ENABLE;
                        if (isSystemApp(permission.owner)) {
                            if (basePermission.type == 1 && basePermission.perm == null) {
                                basePermission.packageSetting = packageLPw;
                                basePermission.perm = permission;
                                basePermission.uid = r85.applicationInfo.uid;
                                basePermission.sourcePackage = permission.info.packageName;
                            } else if (!z8) {
                                reportSettingsProblem(5, "New decl " + permission.owner + " of permission  " + permission.info.name + " is system; overriding " + basePermission.sourcePackage);
                                basePermission = null;
                            }
                        }
                    }
                    if (basePermission == null) {
                        basePermission = new BasePermission(permission.info.name, permission.info.packageName, 0);
                        arrayMap.put(permission.info.name, basePermission);
                    }
                    if (basePermission.perm == null) {
                        if (basePermission.sourcePackage == null || basePermission.sourcePackage.equals(permission.info.packageName)) {
                            BasePermission basePermissionFindPermissionTreeLP = findPermissionTreeLP(permission.info.name);
                            if (basePermissionFindPermissionTreeLP == null || basePermissionFindPermissionTreeLP.sourcePackage.equals(permission.info.packageName)) {
                                basePermission.packageSetting = packageLPw;
                                basePermission.perm = permission;
                                basePermission.uid = r85.applicationInfo.uid;
                                basePermission.sourcePackage = permission.info.packageName;
                                if ((i & 2) != 0) {
                                    if (sb6 == null) {
                                        sb6 = new StringBuilder(256);
                                    } else {
                                        sb6.append(' ');
                                    }
                                    sb6.append(permission.info.name);
                                }
                            } else {
                                Slog.w(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " ignored: base tree " + basePermissionFindPermissionTreeLP.name + " is from package " + basePermissionFindPermissionTreeLP.sourcePackage);
                            }
                        } else {
                            Slog.w(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " ignored: original from " + basePermission.sourcePackage);
                        }
                    } else if ((i & 2) != 0) {
                        if (sb6 == null) {
                            sb6 = new StringBuilder(256);
                        } else {
                            sb6.append(' ');
                        }
                        sb6.append("DUP:");
                        sb6.append(permission.info.name);
                    }
                    if (basePermission.perm == permission) {
                        basePermission.protectionLevel = permission.info.protectionLevel;
                    }
                } else {
                    Slog.w(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " ignored: no group " + permission.group);
                }
            }
            if (sb6 != null) {
            }
            int size10 = r85.instrumentation.size();
            StringBuilder sb7 = null;
            for (int i18 = 0; i18 < size10; i18++) {
                PackageParser.Instrumentation instrumentation = (PackageParser.Instrumentation) r85.instrumentation.get(i18);
                instrumentation.info.packageName = r85.applicationInfo.packageName;
                instrumentation.info.sourceDir = r85.applicationInfo.sourceDir;
                instrumentation.info.publicSourceDir = r85.applicationInfo.publicSourceDir;
                instrumentation.info.splitSourceDirs = r85.applicationInfo.splitSourceDirs;
                instrumentation.info.splitPublicSourceDirs = r85.applicationInfo.splitPublicSourceDirs;
                instrumentation.info.dataDir = r85.applicationInfo.dataDir;
                instrumentation.info.nativeLibraryDir = r85.applicationInfo.nativeLibraryDir;
                this.mInstrumentation.put(instrumentation.getComponentName(), instrumentation);
                if ((i & 2) != 0) {
                    if (sb7 == null) {
                        sb7 = new StringBuilder(256);
                    } else {
                        sb7.append(' ');
                    }
                    sb7.append(instrumentation.info.name);
                }
            }
            if (sb7 != null) {
            }
            if (r85.protectedBroadcasts != null) {
                int size11 = r85.protectedBroadcasts.size();
                for (int i19 = 0; i19 < size11; i19++) {
                    this.mProtectedBroadcasts.add((String) r85.protectedBroadcasts.get(i19));
                }
            }
            packageLPw.setTimeStamp(jLastModified);
            if (r85.mOverlayTarget != null) {
                if (r85.mOverlayTarget != null && !r85.mOverlayTarget.equals("android")) {
                    if (!this.mOverlays.containsKey(r85.mOverlayTarget)) {
                        this.mOverlays.put(r85.mOverlayTarget, new ArrayMap<>());
                    }
                    this.mOverlays.get(r85.mOverlayTarget).put(r85.packageName, r85);
                    PackageParser.Package r54 = this.mPackages.get(r85.mOverlayTarget);
                    if (r54 != null && !createIdmapForPackagePairLI(r54, r85)) {
                        throw new PackageManagerException(-7, "scanPackageLI failed to createIdmap");
                    }
                }
            } else if (this.mOverlays.containsKey(r85.packageName) && !r85.packageName.equals("android")) {
                createIdmapsForPackageLI(r85);
            }
        }
        return r85;
    }

    private void adjustCpuAbisForSharedUserLPw(Set<PackageSetting> packagesForUser, PackageParser.Package scannedPackage, boolean forceDexOpt, boolean deferDexOpt) {
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
                    if (requiredInstructionSet == null) {
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
                        if (ps2.pkg != null && ps2.pkg.applicationInfo != null) {
                            ps2.pkg.applicationInfo.primaryCpuAbi = adjustedAbi;
                            Slog.i(TAG, "Adjusting ABI for : " + ps2.name + " to " + adjustedAbi);
                            if (performDexOptLI(ps2.pkg, (String[]) null, forceDexOpt, deferDexOpt, DEFAULT_VERIFY_ENABLE) == -1) {
                                ps2.primaryCpuAbiString = null;
                                ps2.pkg.applicationInfo.primaryCpuAbi = null;
                                return;
                            }
                            this.mInstaller.rmdex(ps2.codePathString, getDexCodeInstructionSet(getPreferredInstructionSet()));
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
    }

    private void setUpCustomResolverActivity(PackageParser.Package pkg) {
        synchronized (this.mPackages) {
            this.mResolverReplaced = DEFAULT_VERIFY_ENABLE;
            this.mResolveActivity.applicationInfo = pkg.applicationInfo;
            this.mResolveActivity.name = this.mCustomResolverComponentName.getClassName();
            this.mResolveActivity.packageName = pkg.applicationInfo.packageName;
            this.mResolveActivity.processName = pkg.applicationInfo.packageName;
            this.mResolveActivity.launchMode = 0;
            this.mResolveActivity.flags = 288;
            this.mResolveActivity.theme = 0;
            this.mResolveActivity.exported = DEFAULT_VERIFY_ENABLE;
            this.mResolveActivity.enabled = DEFAULT_VERIFY_ENABLE;
            this.mResolveInfo.activityInfo = this.mResolveActivity;
            this.mResolveInfo.priority = 0;
            this.mResolveInfo.preferredOrder = 0;
            this.mResolveInfo.match = 0;
            this.mResolveComponentName = this.mCustomResolverComponentName;
            Slog.i(TAG, "Replacing default ResolverActivity with custom activity: " + this.mResolveComponentName);
        }
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
        boolean bundledApp = (!isSystemApp(info) || isUpdatedSystemApp(info)) ? false : DEFAULT_VERIFY_ENABLE;
        boolean asecApp = (isForwardLocked(info) || isExternal(info)) ? DEFAULT_VERIFY_ENABLE : false;
        info.nativeLibraryRootDir = null;
        info.nativeLibraryRootRequiresIsa = false;
        info.nativeLibraryDir = null;
        info.secondaryNativeLibraryDir = null;
        if (PackageParser.isApkFile(codeFile)) {
            if (bundledApp) {
                String apkRoot = calculateBundledApkRoot(info.sourceDir);
                boolean is64Bit = VMRuntime.is64BitInstructionSet(getPrimaryInstructionSet(info));
                String apkName = deriveCodePathName(codePath);
                String libDir = is64Bit ? "lib64" : "lib";
                info.nativeLibraryRootDir = Environment.buildPath(new File(apkRoot), new String[]{libDir, apkName}).getAbsolutePath();
                if (info.secondaryCpuAbi != null) {
                    String secondaryLibDir = is64Bit ? "lib" : "lib64";
                    info.secondaryNativeLibraryDir = Environment.buildPath(new File(apkRoot), new String[]{secondaryLibDir, apkName}).getAbsolutePath();
                }
            } else if (asecApp) {
                info.nativeLibraryRootDir = new File(codeFile.getParentFile(), "lib").getAbsolutePath();
            } else {
                info.nativeLibraryRootDir = new File(this.mAppLib32InstallDir, deriveCodePathName(codePath)).getAbsolutePath();
            }
            info.nativeLibraryRootRequiresIsa = false;
            info.nativeLibraryDir = info.nativeLibraryRootDir;
            return;
        }
        info.nativeLibraryRootDir = new File(codeFile, "lib").getAbsolutePath();
        info.nativeLibraryRootRequiresIsa = DEFAULT_VERIFY_ENABLE;
        info.nativeLibraryDir = new File(info.nativeLibraryRootDir, getPrimaryInstructionSet(info)).getAbsolutePath();
        if (info.secondaryCpuAbi != null) {
            info.secondaryNativeLibraryDir = new File(info.nativeLibraryRootDir, VMRuntime.getInstructionSet(info.secondaryCpuAbi)).getAbsolutePath();
        }
    }

    private void setBundledAppAbisAndRoots(PackageParser.Package pkg, PackageSetting pkgSetting) {
        String apkName = deriveCodePathName(pkg.applicationInfo.getCodePath());
        String apkRoot = calculateBundledApkRoot(pkg.applicationInfo.sourceDir);
        setBundledAppAbi(pkg, apkRoot, apkName);
        if (pkgSetting != null) {
            pkgSetting.primaryCpuAbiString = pkg.applicationInfo.primaryCpuAbi;
            pkgSetting.secondaryCpuAbiString = pkg.applicationInfo.secondaryCpuAbi;
        }
    }

    private static void setBundledAppAbi(PackageParser.Package pkg, String apkRoot, String apkName) {
        boolean has64BitLibs;
        boolean has32BitLibs;
        File codeFile = new File(pkg.codePath);
        if (PackageParser.isApkFile(codeFile)) {
            has64BitLibs = new File(apkRoot, new File("lib64", apkName).getPath()).exists();
            has32BitLibs = new File(apkRoot, new File("lib", apkName).getPath()).exists();
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
                has32BitLibs = new File(rootDir, isa2).exists();
            } else {
                has32BitLibs = false;
            }
        }
        if (has64BitLibs && !has32BitLibs) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (has32BitLibs && !has64BitLibs) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
            return;
        }
        if (has32BitLibs && has64BitLibs) {
            if ((pkg.applicationInfo.flags & SoundTriggerHelper.STATUS_ERROR) == 0) {
                Slog.e(TAG, "Package: " + pkg + " has multiple bundled libs, but is not multiarch.");
            }
            if (VMRuntime.is64BitInstructionSet(getPreferredInstructionSet())) {
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
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            try {
                am.killApplicationWithAppId(pkgName, appId, reason);
            } catch (RemoteException e) {
            }
        }
    }

    void removePackageLI(PackageSetting ps, boolean chatty) {
        synchronized (this.mPackages) {
            this.mPackages.remove(ps.name);
            PackageParser.Package pkg = ps.pkg;
            if (pkg != null) {
                cleanPackageDataStructuresLILPw(pkg, chatty);
            }
        }
    }

    void removeInstalledPackageLI(PackageParser.Package pkg, boolean chatty) {
        synchronized (this.mPackages) {
            this.mPackages.remove(pkg.applicationInfo.packageName);
            cleanPackageDataStructuresLILPw(pkg, chatty);
        }
    }

    void cleanPackageDataStructuresLILPw(PackageParser.Package pkg, boolean chatty) {
        ArraySet<String> appOpPerms;
        ArraySet<String> appOpPerms2;
        int N = pkg.providers.size();
        for (int i = 0; i < N; i++) {
            PackageParser.Provider p = (PackageParser.Provider) pkg.providers.get(i);
            this.mProviders.removeProvider(p);
            if (p.info.authority != null) {
                String[] names = p.info.authority.split(";");
                for (int j = 0; j < names.length; j++) {
                    if (this.mProvidersByAuthority.get(names[j]) == p) {
                        this.mProvidersByAuthority.remove(names[j]);
                    }
                }
            }
        }
        if (0 != 0) {
        }
        int N2 = pkg.services.size();
        StringBuilder r = null;
        for (int i2 = 0; i2 < N2; i2++) {
            PackageParser.Service s = (PackageParser.Service) pkg.services.get(i2);
            this.mServices.removeService(s);
            if (chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(s.info.name);
            }
        }
        if (r != null) {
        }
        int N3 = pkg.receivers.size();
        for (int i3 = 0; i3 < N3; i3++) {
            PackageParser.Activity a = (PackageParser.Activity) pkg.receivers.get(i3);
            this.mReceivers.removeActivity(a, "receiver");
        }
        if (0 != 0) {
        }
        int N4 = pkg.activities.size();
        for (int i4 = 0; i4 < N4; i4++) {
            PackageParser.Activity a2 = (PackageParser.Activity) pkg.activities.get(i4);
            this.mActivities.removeActivity(a2, "activity");
        }
        if (0 != 0) {
        }
        int N5 = pkg.permissions.size();
        for (int i5 = 0; i5 < N5; i5++) {
            PackageParser.Permission p2 = (PackageParser.Permission) pkg.permissions.get(i5);
            BasePermission bp = this.mSettings.mPermissions.get(p2.info.name);
            if (bp == null) {
                bp = this.mSettings.mPermissionTrees.get(p2.info.name);
            }
            if (bp != null && bp.perm == p2) {
                bp.perm = null;
            }
            if ((p2.info.protectionLevel & 64) != 0 && (appOpPerms2 = this.mAppOpPermissionPackages.get(p2.info.name)) != null) {
                appOpPerms2.remove(pkg.packageName);
            }
        }
        if (0 != 0) {
        }
        int N6 = pkg.requestedPermissions.size();
        for (int i6 = 0; i6 < N6; i6++) {
            String perm = (String) pkg.requestedPermissions.get(i6);
            BasePermission bp2 = this.mSettings.mPermissions.get(perm);
            if (bp2 != null && (bp2.protectionLevel & 64) != 0 && (appOpPerms = this.mAppOpPermissionPackages.get(perm)) != null) {
                appOpPerms.remove(pkg.packageName);
                if (appOpPerms.isEmpty()) {
                    this.mAppOpPermissionPackages.remove(perm);
                }
            }
        }
        if (0 != 0) {
        }
        int N7 = pkg.instrumentation.size();
        for (int i7 = 0; i7 < N7; i7++) {
            PackageParser.Instrumentation a3 = (PackageParser.Instrumentation) pkg.instrumentation.get(i7);
            this.mInstrumentation.remove(a3.getComponentName());
        }
        if (0 != 0) {
        }
        if ((pkg.applicationInfo.flags & 1) != 0 && pkg.libraryNames != null) {
            for (int i8 = 0; i8 < pkg.libraryNames.size(); i8++) {
                String name = (String) pkg.libraryNames.get(i8);
                SharedLibraryEntry cur = this.mSharedLibraries.get(name);
                if (cur != null && cur.apk != null && cur.apk.equals(pkg.packageName)) {
                    this.mSharedLibraries.remove(name);
                }
            }
        }
        if (0 != 0) {
        }
    }

    private static boolean hasPermission(PackageParser.Package pkgInfo, String perm) {
        for (int i = pkgInfo.permissions.size() - 1; i >= 0; i--) {
            if (((PackageParser.Permission) pkgInfo.permissions.get(i)).info.name.equals(perm)) {
                return DEFAULT_VERIFY_ENABLE;
            }
        }
        return false;
    }

    private void updatePermissionsLPw(String changingPkg, PackageParser.Package pkgInfo, int flags) {
        BasePermission tree;
        boolean z = DEFAULT_VERIFY_ENABLE;
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
            if (bp2.type == 2 && bp2.packageSetting == null && bp2.pendingInfo != null && (tree = findPermissionTreeLP(bp2.name)) != null && tree.perm != null) {
                bp2.packageSetting = tree.packageSetting;
                bp2.perm = new PackageParser.Permission(tree.perm.owner, new PermissionInfo(bp2.pendingInfo));
                bp2.perm.info.packageName = tree.perm.info.packageName;
                bp2.perm.info.name = bp2.name;
                bp2.uid = tree.uid;
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
                    grantPermissionsLPw(pkg, (flags & 4) != 0, changingPkg);
                }
            }
        }
        if (pkgInfo != null) {
            if ((flags & 2) == 0) {
                z = false;
            }
            grantPermissionsLPw(pkgInfo, z, changingPkg);
        }
    }

    private void grantPermissionsLPw(PackageParser.Package pkg, boolean replace, String packageOfInterest) {
        boolean allowed;
        PackageSetting ps = (PackageSetting) pkg.mExtras;
        if (ps != null) {
            GrantedPermissions gp = ps.sharedUser != null ? ps.sharedUser : ps;
            ArraySet<String> origPermissions = gp.grantedPermissions;
            boolean changedPermission = false;
            if (replace) {
                ps.permissionsFixed = false;
                if (gp == ps) {
                    origPermissions = new ArraySet<>(gp.grantedPermissions);
                    gp.grantedPermissions.clear();
                    gp.gids = this.mGlobalGids;
                }
            }
            if (gp.gids == null) {
                gp.gids = this.mGlobalGids;
            }
            int N = pkg.requestedPermissions.size();
            for (int i = 0; i < N; i++) {
                String name = (String) pkg.requestedPermissions.get(i);
                boolean required = ((Boolean) pkg.requestedPermissionsRequired.get(i)).booleanValue();
                BasePermission bp = this.mSettings.mPermissions.get(name);
                if (bp == null || bp.packageSetting == null) {
                    if (packageOfInterest == null || packageOfInterest.equals(pkg.packageName)) {
                        Slog.w(TAG, "Unknown permission " + name + " in package " + pkg.packageName);
                    }
                } else {
                    String perm = bp.name;
                    boolean allowedSig = false;
                    if ((bp.protectionLevel & 64) != 0) {
                        ArraySet<String> pkgs = this.mAppOpPermissionPackages.get(bp.name);
                        if (pkgs == null) {
                            pkgs = new ArraySet<>();
                            this.mAppOpPermissionPackages.put(bp.name, pkgs);
                        }
                        pkgs.add(pkg.packageName);
                    }
                    int level = bp.protectionLevel & 15;
                    if (level == 0 || level == 1) {
                        allowed = (required || origPermissions.contains(perm) || (isSystemApp(ps) && !isUpdatedSystemApp(ps))) ? DEFAULT_VERIFY_ENABLE : false;
                    } else if (bp.packageSetting != null && level == 2) {
                        allowed = grantSignaturePermission(perm, pkg, bp, origPermissions);
                        if (allowed) {
                            allowedSig = DEFAULT_VERIFY_ENABLE;
                        }
                    } else {
                        allowed = false;
                    }
                    if (allowed) {
                        if (!isSystemApp(ps) && ps.permissionsFixed && !allowedSig && !gp.grantedPermissions.contains(perm)) {
                            allowed = isNewPlatformPermissionForPackage(perm, pkg);
                        }
                        if (allowed) {
                            if (!gp.grantedPermissions.contains(perm)) {
                                changedPermission = DEFAULT_VERIFY_ENABLE;
                                gp.grantedPermissions.add(perm);
                                gp.gids = appendInts(gp.gids, bp.gids);
                            } else if (!ps.haveGids) {
                                gp.gids = appendInts(gp.gids, bp.gids);
                            }
                        } else if (packageOfInterest == null || packageOfInterest.equals(pkg.packageName)) {
                            Slog.w(TAG, "Not granting permission " + perm + " to package " + pkg.packageName + " because it was previously installed without");
                        }
                    } else if (gp.grantedPermissions.remove(perm)) {
                        changedPermission = DEFAULT_VERIFY_ENABLE;
                        gp.gids = removeInts(gp.gids, bp.gids);
                        Slog.i(TAG, "Un-granting permission " + perm + " from package " + pkg.packageName + " (protectionLevel=" + bp.protectionLevel + " flags=0x" + Integer.toHexString(pkg.applicationInfo.flags) + ")");
                    } else if ((bp.protectionLevel & 64) == 0 && (packageOfInterest == null || packageOfInterest.equals(pkg.packageName))) {
                        Slog.w(TAG, "Not granting permission " + perm + " to package " + pkg.packageName + " (protectionLevel=" + bp.protectionLevel + " flags=0x" + Integer.toHexString(pkg.applicationInfo.flags) + ")");
                    }
                }
            }
            if (((changedPermission || replace) && !ps.permissionsFixed && !isSystemApp(ps)) || isUpdatedSystemApp(ps)) {
                ps.permissionsFixed = DEFAULT_VERIFY_ENABLE;
            }
            ps.haveGids = DEFAULT_VERIFY_ENABLE;
        }
    }

    private boolean isNewPlatformPermissionForPackage(String perm, PackageParser.Package pkg) {
        int NP = PackageParser.NEW_PERMISSIONS.length;
        for (int ip = 0; ip < NP; ip++) {
            PackageParser.NewPermissionInfo npi = PackageParser.NEW_PERMISSIONS[ip];
            if (npi.name.equals(perm) && pkg.applicationInfo.targetSdkVersion < npi.sdkVersion) {
                Log.i(TAG, "Auto-granting " + perm + " to old pkg " + pkg.packageName);
                return DEFAULT_VERIFY_ENABLE;
            }
        }
        return false;
    }

    private boolean grantSignaturePermission(String perm, PackageParser.Package pkg, BasePermission bp, ArraySet<String> origPermissions) {
        boolean allowed = (compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) == 0 || compareSignatures(this.mPlatformPackage.mSignatures, pkg.mSignatures) == 0) ? DEFAULT_VERIFY_ENABLE : false;
        if (!allowed && (bp.protectionLevel & 16) != 0 && isSystemApp(pkg)) {
            if (isUpdatedSystemApp(pkg)) {
                PackageSetting sysPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                GrantedPermissions origGp = sysPs.sharedUser != null ? sysPs.sharedUser : sysPs;
                if (origGp.grantedPermissions.contains(perm)) {
                    if (sysPs.isPrivileged()) {
                        allowed = DEFAULT_VERIFY_ENABLE;
                    }
                } else if (sysPs.pkg != null && sysPs.isPrivileged()) {
                    int j = 0;
                    while (true) {
                        if (j >= sysPs.pkg.requestedPermissions.size()) {
                            break;
                        }
                        if (!perm.equals(sysPs.pkg.requestedPermissions.get(j))) {
                            j++;
                        } else {
                            allowed = DEFAULT_VERIFY_ENABLE;
                            break;
                        }
                    }
                }
            } else {
                allowed = isPrivilegedApp(pkg);
            }
        }
        if (!allowed && (bp.protectionLevel & 32) != 0) {
            return origPermissions.contains(perm);
        }
        return allowed;
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
            this.mFlags = defaultOnly ? PackageManagerService.REMOVE_CHATTY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Activity> packageActivities, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageActivities == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
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
            Iterator<PackageParser.Activity> it = activityList.iterator();
            while (it.hasNext()) {
                PackageParser.Activity sysActivity = it.next();
                if (!sysActivity.info.name.equals(activityInfo.name) && !sysActivity.info.name.equals(activityInfo.targetActivity)) {
                    if (sysActivity.info.targetActivity != null && (sysActivity.info.targetActivity.equals(activityInfo.name) || sysActivity.info.targetActivity.equals(activityInfo.targetActivity))) {
                        return sysActivity;
                    }
                } else {
                    return sysActivity;
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
                                selectionFound = PackageManagerService.DEFAULT_VERIFY_ENABLE;
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

        private void adjustPriority(List<PackageParser.Activity> systemActivities, PackageParser.ActivityIntentInfo intent) {
            if (intent.getPriority() > 0) {
                ActivityInfo activityInfo = intent.activity.info;
                ApplicationInfo applicationInfo = activityInfo.applicationInfo;
                boolean systemApp = PackageManagerService.isSystemApp(applicationInfo);
                if (!systemApp) {
                    Slog.w(PackageManagerService.TAG, "Non-system app; cap priority to 0; package: " + applicationInfo.packageName + " activity: " + intent.activity.className + " origPrio: " + intent.getPriority());
                    intent.setPriority(0);
                    return;
                }
                if (systemActivities != null) {
                    PackageParser.Activity foundActivity = findMatchingActivity(systemActivities, activityInfo);
                    if (foundActivity == null) {
                        intent.setPriority(0);
                        return;
                    }
                    List<PackageParser.ActivityIntentInfo> intentListCopy = new ArrayList<>(foundActivity.intents);
                    findFilters(intent);
                    Iterator<String> actionsIterator = intent.actionsIterator();
                    if (actionsIterator != null) {
                        getIntentListSubset(intentListCopy, new ActionIterGenerator(), actionsIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<String> categoriesIterator = intent.categoriesIterator();
                    if (categoriesIterator != null) {
                        getIntentListSubset(intentListCopy, new CategoriesIterGenerator(), categoriesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<String> schemesIterator = intent.schemesIterator();
                    if (schemesIterator != null) {
                        getIntentListSubset(intentListCopy, new SchemesIterGenerator(), schemesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<IntentFilter.AuthorityEntry> authoritiesIterator = intent.authoritiesIterator();
                    if (authoritiesIterator != null) {
                        getIntentListSubset(intentListCopy, new AuthoritiesIterGenerator(), authoritiesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    int cappedPriority = 0;
                    for (int i = intentListCopy.size() - 1; i >= 0; i--) {
                        cappedPriority = Math.max(cappedPriority, intentListCopy.get(i).getPriority());
                    }
                    if (intent.getPriority() > cappedPriority) {
                        intent.setPriority(cappedPriority);
                    }
                }
            }
        }

        public final void addActivity(PackageParser.Activity a, String type) {
            PackageManagerService.isSystemApp(a.info.applicationInfo);
            this.mActivities.put(a.getComponentName(), a);
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ActivityIntentInfo intent = (PackageParser.ActivityIntentInfo) a.intents.get(j);
                if ("activity".equals(type)) {
                    PackageSetting ps = PackageManagerService.this.mSettings.getDisabledSystemPkgLPr(intent.activity.info.packageName);
                    List<PackageParser.Activity> systemActivities = (ps == null || ps.pkg == null) ? null : ps.pkg.activities;
                    adjustPriority(systemActivities, intent);
                }
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Activity " + a.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeActivity(PackageParser.Activity a, String type) {
            this.mActivities.remove(a.getComponentName());
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ActivityIntentInfo intent = (PackageParser.ActivityIntentInfo) a.intents.get(j);
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
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        @Override
        protected PackageParser.ActivityIntentInfo[] newArray(int size) {
            return new PackageParser.ActivityIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ActivityIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            PackageParser.Package p = filter.activity.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null) {
                return false;
            }
            if ((ps.pkgFlags & 1) == 0 && ps.getStopped(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ActivityIntentInfo info) {
            return packageName.equals(info.activity.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ActivityIntentInfo info, int match, int userId) {
            PackageSetting ps;
            ActivityInfo ai;
            ResolveInfo res = null;
            if (PackageManagerService.sUserManager.exists(userId) && PackageManagerService.this.mSettings.isEnabledLPr(info.activity.info, this.mFlags, userId)) {
                PackageParser.Activity activity = info.activity;
                if ((!PackageManagerService.this.mSafeMode || (activity.info.applicationInfo.flags & 1) != 0) && (ps = (PackageSetting) activity.owner.mExtras) != null && (ai = PackageParser.generateActivityInfo(activity, this.mFlags, ps.readUserState(userId), userId)) != null) {
                    res = new ResolveInfo();
                    res.activityInfo = ai;
                    if ((this.mFlags & 64) != 0) {
                        res.filter = info;
                    }
                    res.priority = info.getPriority();
                    res.preferredOrder = activity.owner.mPreferredOrder;
                    res.match = match;
                    res.isDefault = info.hasDefault;
                    res.labelRes = info.labelRes;
                    res.nonLocalizedLabel = info.nonLocalizedLabel;
                    if (PackageManagerService.this.userNeedsBadging(userId)) {
                        res.noResourceId = PackageManagerService.DEFAULT_VERIFY_ENABLE;
                    } else {
                        res.icon = info.icon;
                    }
                    res.system = PackageManagerService.isSystemApp(res.activityInfo.applicationInfo);
                }
            }
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

        private ServiceIntentResolver() {
            this.mServices = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? PackageManagerService.REMOVE_CHATTY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Service> packageServices, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageServices == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
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
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ServiceIntentInfo intent = (PackageParser.ServiceIntentInfo) s.intents.get(j);
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Service " + s.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeService(PackageParser.Service s) {
            this.mServices.remove(s.getComponentName());
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ServiceIntentInfo intent = (PackageParser.ServiceIntentInfo) s.intents.get(j);
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
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        @Override
        protected PackageParser.ServiceIntentInfo[] newArray(int size) {
            return new PackageParser.ServiceIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ServiceIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            PackageParser.Package p = filter.service.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null) {
                return false;
            }
            if ((ps.pkgFlags & 1) == 0 && ps.getStopped(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ServiceIntentInfo info) {
            return packageName.equals(info.service.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ServiceIntentInfo filter, int match, int userId) {
            PackageSetting ps;
            ServiceInfo si;
            ResolveInfo res = null;
            if (PackageManagerService.sUserManager.exists(userId) && PackageManagerService.this.mSettings.isEnabledLPr(filter.service.info, this.mFlags, userId)) {
                PackageParser.Service service = filter.service;
                if ((!PackageManagerService.this.mSafeMode || (service.info.applicationInfo.flags & 1) != 0) && (ps = (PackageSetting) service.owner.mExtras) != null && (si = PackageParser.generateServiceInfo(service, this.mFlags, ps.readUserState(userId), userId)) != null) {
                    res = new ResolveInfo();
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
                    res.system = PackageManagerService.isSystemApp(res.serviceInfo.applicationInfo);
                }
            }
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

        private ProviderIntentResolver() {
            this.mProviders = new ArrayMap<>();
        }

        @Override
        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? PackageManagerService.REMOVE_CHATTY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            return super.queryIntent(intent, resolvedType, (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<PackageParser.Provider> packageProviders, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageProviders == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (PackageManagerService.REMOVE_CHATTY & flags) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
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
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ProviderIntentInfo intent = (PackageParser.ProviderIntentInfo) p.intents.get(j);
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Provider " + p.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeProvider(PackageParser.Provider p) {
            this.mProviders.remove(p.getComponentName());
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                PackageParser.ProviderIntentInfo intent = (PackageParser.ProviderIntentInfo) p.intents.get(j);
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
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        @Override
        protected PackageParser.ProviderIntentInfo[] newArray(int size) {
            return new PackageParser.ProviderIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(PackageParser.ProviderIntentInfo filter, int userId) {
            PackageSetting ps;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            PackageParser.Package p = filter.provider.owner;
            if (p == null || (ps = (PackageSetting) p.mExtras) == null) {
                return false;
            }
            if ((ps.pkgFlags & 1) == 0 && ps.getStopped(userId)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        @Override
        protected boolean isPackageForFilter(String packageName, PackageParser.ProviderIntentInfo info) {
            return packageName.equals(info.provider.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(PackageParser.ProviderIntentInfo filter, int match, int userId) {
            PackageSetting ps;
            ProviderInfo pi;
            ResolveInfo res = null;
            if (PackageManagerService.sUserManager.exists(userId) && PackageManagerService.this.mSettings.isEnabledLPr(filter.provider.info, this.mFlags, userId)) {
                PackageParser.Provider provider = filter.provider;
                if ((!PackageManagerService.this.mSafeMode || (provider.info.applicationInfo.flags & 1) != 0) && (ps = (PackageSetting) provider.owner.mExtras) != null && (pi = PackageParser.generateProviderInfo(provider, this.mFlags, ps.readUserState(userId), userId)) != null) {
                    res = new ResolveInfo();
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
                    res.system = PackageManagerService.isSystemApp(res.providerInfo.applicationInfo);
                }
            }
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

    static final void sendPackageBroadcast(String action, String pkg, Bundle extras, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds) {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            if (userIds == null) {
                try {
                    userIds = am.getRunningUserIds();
                } catch (RemoteException e) {
                    return;
                }
            }
            int[] arr$ = userIds;
            for (int id : arr$) {
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
                intent.addFlags(67108864);
                am.broadcastIntent((IApplicationThread) null, intent, (String) null, finishedReceiver, 0, (String) null, (Bundle) null, (String) null, -1, finishedReceiver != null ? DEFAULT_VERIFY_ENABLE : false, false, id);
            }
        }
    }

    private boolean isExternalMediaAvailable() {
        if (this.mMediaMounted || Environment.isExternalStorageEmulated()) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    public PackageCleanItem nextPackageToClean(PackageCleanItem lastPackage) {
        PackageCleanItem packageCleanItem = null;
        synchronized (this.mPackages) {
            if (isExternalMediaAvailable()) {
                ArrayList<PackageCleanItem> pkgs = this.mSettings.mPackagesToBeCleaned;
                if (lastPackage != null) {
                    pkgs.remove(lastPackage);
                }
                if (pkgs.size() > 0) {
                    packageCleanItem = pkgs.get(0);
                }
            }
        }
        return packageCleanItem;
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
        synchronized (this.mPackages) {
            if (isExternalMediaAvailable()) {
                if (!this.mSettings.mPackagesToBeCleaned.isEmpty()) {
                    Intent intent = new Intent("android.content.pm.CLEAN_EXTERNAL_STORAGE");
                    intent.setComponent(DEFAULT_CONTAINER_COMPONENT);
                    IActivityManager am = ActivityManagerNative.getDefault();
                    if (am != null) {
                        try {
                            am.startService((IApplicationThread) null, intent, (String) null, 0);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }
    }

    public void installPackage(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, VerificationParams verificationParams, String packageAbiOverride) {
        installPackageAsUser(originPath, observer, installFlags, installerPackageName, verificationParams, packageAbiOverride, UserHandle.getCallingUserId());
    }

    public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, VerificationParams verificationParams, String packageAbiOverride, int userId) {
        int installFlags2;
        UserHandle user;
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, DEFAULT_VERIFY_ENABLE, DEFAULT_VERIFY_ENABLE, "installPackageAsUser");
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
        verificationParams.setInstallerUid(callingUid);
        File originFile = new File(originPath);
        OriginInfo origin = OriginInfo.fromUntrustedFile(originFile);
        Message msg = this.mHandler.obtainMessage(5);
        msg.obj = new InstallParams(origin, observer, installFlags2, installerPackageName, verificationParams, user, packageAbiOverride);
        this.mHandler.sendMessage(msg);
    }

    void installStage(String packageName, File stagedDir, String stagedCid, IPackageInstallObserver2 observer, PackageInstaller.SessionParams params, String installerPackageName, int installerUid, UserHandle user) {
        OriginInfo origin;
        VerificationParams verifParams = new VerificationParams((Uri) null, params.originatingUri, params.referrerUri, installerUid, (ManifestDigest) null);
        if (stagedDir != null) {
            origin = OriginInfo.fromStagedFile(stagedDir);
        } else {
            origin = OriginInfo.fromStagedContainer(stagedCid);
        }
        Message msg = this.mHandler.obtainMessage(5);
        msg.obj = new InstallParams(origin, observer, params.installFlags, installerPackageName, verifParams, user, params.abiOverride);
        this.mHandler.sendMessage(msg);
    }

    private void sendPackageAddedForUser(String packageName, PackageSetting pkgSetting, int userId) {
        Bundle extras = new Bundle(1);
        extras.putInt("android.intent.extra.UID", UserHandle.getUid(userId, pkgSetting.appId));
        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, null, null, new int[]{userId});
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            boolean isSystem = (isSystemApp(pkgSetting) || isUpdatedSystemApp(pkgSetting)) ? DEFAULT_VERIFY_ENABLE : false;
            if (isSystem && am.isUserRunning(userId, false)) {
                Intent bcIntent = new Intent("android.intent.action.BOOT_COMPLETED").addFlags(32).setPackage(packageName);
                am.broadcastIntent((IApplicationThread) null, bcIntent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String) null, -1, false, false, userId);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to bootstrap installed package", e);
        }
    }

    public boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, DEFAULT_VERIFY_ENABLE, DEFAULT_VERIFY_ENABLE, "setApplicationHiddenSetting for user " + userId);
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
                        sendRemoved = DEFAULT_VERIFY_ENABLE;
                    } else {
                        sendAdded = DEFAULT_VERIFY_ENABLE;
                    }
                }
                if (sendAdded) {
                    sendPackageAddedForUser(packageName, pkgSetting, userId);
                    return DEFAULT_VERIFY_ENABLE;
                }
                if (sendRemoved) {
                    killApplication(packageName, UserHandle.getUid(userId, pkgSetting.appId), "hiding pkg");
                    sendApplicationHiddenForUser(packageName, pkgSetting, userId);
                }
                return false;
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
        info.sendBroadcast(false, false, false);
    }

    public boolean getApplicationHiddenSettingAsUser(String packageName, int userId) {
        boolean hidden = DEFAULT_VERIFY_ENABLE;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, DEFAULT_VERIFY_ENABLE, false, "getApplicationHidden for user " + userId);
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                if (pkgSetting != null) {
                    hidden = pkgSetting.getHidden(userId);
                }
            }
            return hidden;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int installExistingPackageAsUser(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int uid = Binder.getCallingUid();
        enforceCrossUserPermission(uid, userId, DEFAULT_VERIFY_ENABLE, DEFAULT_VERIFY_ENABLE, "installExistingPackage for user " + userId);
        if (isUserRestricted(userId, "no_install_apps")) {
            return -111;
        }
        long callingId = Binder.clearCallingIdentity();
        boolean sendAdded = false;
        try {
            new Bundle(1);
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    return -3;
                }
                if (!pkgSetting.getInstalled(userId)) {
                    pkgSetting.setInstalled(DEFAULT_VERIFY_ENABLE, userId);
                    pkgSetting.setHidden(false, userId);
                    this.mSettings.writePackageRestrictionsLPr(userId);
                    sendAdded = DEFAULT_VERIFY_ENABLE;
                }
                if (sendAdded) {
                    sendPackageAddedForUser(packageName, pkgSetting, userId);
                }
                return 1;
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
        return DEFAULT_VERIFY_ENABLE;
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
        if (state != null && !state.timeoutExtended()) {
            state.extendTimeout();
            Message msg = this.mHandler.obtainMessage(15);
            msg.arg1 = id;
            msg.obj = response;
            this.mHandler.sendMessageDelayed(msg, millisecondsToDelay);
        }
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
                sufficientVerifiers.add(comp);
                verificationState.addSufficientVerifier(verifierUid);
            }
        }
        return sufficientVerifiers;
    }

    private int getUidForVerifier(VerifierInfo verifierInfo) {
        int i = -1;
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(verifierInfo.packageName);
            if (pkg != null) {
                if (pkg.mSignatures.length != 1) {
                    Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " has more than one signature; ignoring");
                } else {
                    try {
                        Signature verifierSig = pkg.mSignatures[0];
                        PublicKey publicKey = verifierSig.getPublicKey();
                        byte[] expectedPublicKey = publicKey.getEncoded();
                        byte[] actualPublicKey = verifierInfo.publicKey.getEncoded();
                        if (!Arrays.equals(actualPublicKey, expectedPublicKey)) {
                            Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " does not have the expected public key; ignoring");
                        } else {
                            i = pkg.applicationInfo.uid;
                        }
                    } catch (CertificateException e) {
                    }
                }
            }
        }
        return i;
    }

    public void finishPackageInstall(int token) {
        enforceSystemOrRoot("Only the system is allowed to finish installs");
        Message msg = this.mHandler.obtainMessage(9, token, 0);
        this.mHandler.sendMessage(msg);
    }

    private long getVerificationTimeout() {
        return Settings.Global.getLong(this.mContext.getContentResolver(), "verifier_timeout", DEFAULT_VERIFICATION_TIMEOUT);
    }

    private int getDefaultVerificationResponse() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_default_response", 1);
    }

    private boolean isVerificationEnabled(int userId, int installFlags) {
        boolean ensureVerifyAppsEnabled = isUserRestricted(userId, "ensure_verify_apps");
        if ((installFlags & 32) != 0) {
            if (ActivityManager.isRunningInTestHarness()) {
                return false;
            }
            if (ensureVerifyAppsEnabled) {
                return DEFAULT_VERIFY_ENABLE;
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) == 0) {
                return false;
            }
        }
        if (ensureVerifyAppsEnabled || Settings.Global.getInt(this.mContext.getContentResolver(), "package_verifier_enable", 1) == 1) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private int getUnknownSourcesSettings() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "install_non_market_apps", -1);
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
                scheduleWriteSettingsLocked();
            } else {
                throw new SecurityException("Unknown calling uid " + uid);
            }
        }
    }

    private void processPendingInstall(final InstallArgs args, final int currentStatus) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PackageManagerService.this.mHandler.removeCallbacks(this);
                PackageInstalledInfo res = PackageManagerService.this.new PackageInstalledInfo();
                res.returnCode = currentStatus;
                res.uid = -1;
                res.pkg = null;
                res.removedInfo = new PackageRemovedInfo();
                if (res.returnCode == 1) {
                    args.doPreInstall(res.returnCode);
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.installPackageLI(args, res);
                    }
                    args.doPostInstall(res.returnCode, res.uid);
                }
                boolean update = res.removedInfo.removedPackage != null;
                int flags = res.pkg == null ? 0 : res.pkg.applicationInfo.flags;
                boolean doRestore = (update || (PackageManagerService.MAX_PERMISSION_TREE_FOOTPRINT & flags) == 0) ? false : true;
                if (PackageManagerService.this.mNextInstallToken < 0) {
                    PackageManagerService.this.mNextInstallToken = 1;
                }
                PackageManagerService packageManagerService = PackageManagerService.this;
                int token = packageManagerService.mNextInstallToken;
                packageManagerService.mNextInstallToken = token + 1;
                PostInstallData data = PackageManagerService.this.new PostInstallData(args, res);
                PackageManagerService.this.mRunningInstalls.put(token, data);
                if (res.returnCode == 1 && doRestore) {
                    IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    if (bm != null) {
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
                if (!doRestore) {
                    Message msg = PackageManagerService.this.mHandler.obtainMessage(9, token, 0);
                    PackageManagerService.this.mHandler.sendMessage(msg);
                }
            }
        });
    }

    private abstract class HandlerParams {
        private static final int MAX_RETRIES = 4;
        private int mRetries = 0;
        private final UserHandle mUser;

        abstract void handleReturnCode();

        abstract void handleServiceError();

        abstract void handleStartCopy() throws RemoteException;

        HandlerParams(UserHandle user) {
            this.mUser = user;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        final boolean startCopy() {
            boolean res;
            int i;
            try {
                i = this.mRetries + 1;
                this.mRetries = i;
            } catch (RemoteException e) {
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
            res = PackageManagerService.DEFAULT_VERIFY_ENABLE;
            handleReturnCode();
            return res;
        }

        final void serviceError() {
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
            boolean mounted;
            synchronized (PackageManagerService.this.mInstallLock) {
                this.mSuccess = PackageManagerService.this.getPackageSizeInfoLI(this.mStats.packageName, this.mStats.userHandle, this.mStats);
            }
            if (this.mSuccess) {
                if (Environment.isExternalStorageEmulated()) {
                    mounted = PackageManagerService.DEFAULT_VERIFY_ENABLE;
                } else {
                    String status = Environment.getExternalStorageState();
                    mounted = ("mounted".equals(status) || "mounted_ro".equals(status)) ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
                }
                if (mounted) {
                    Environment.UserEnvironment userEnv = new Environment.UserEnvironment(this.mStats.userHandle);
                    this.mStats.externalCacheSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppCacheDirs(this.mStats.packageName));
                    this.mStats.externalDataSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppDataDirs(this.mStats.packageName));
                    this.mStats.externalDataSize -= this.mStats.externalCacheSize;
                    this.mStats.externalMediaSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppMediaDirs(this.mStats.packageName));
                    this.mStats.externalObbSize = PackageManagerService.calculateDirectorySize(PackageManagerService.this.mContainerService, userEnv.buildExternalStorageAppObbDirs(this.mStats.packageName));
                }
            }
        }

        @Override
        void handleReturnCode() {
            if (this.mObserver != null) {
                try {
                    this.mObserver.onGetStatsCompleted(this.mStats, this.mSuccess);
                } catch (RemoteException e) {
                    Slog.i(PackageManagerService.TAG, "Observer no longer exists.");
                }
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
            return new OriginInfo(file, null, false, PackageManagerService.DEFAULT_VERIFY_ENABLE);
        }

        static OriginInfo fromStagedFile(File file) {
            return new OriginInfo(file, null, PackageManagerService.DEFAULT_VERIFY_ENABLE, false);
        }

        static OriginInfo fromStagedContainer(String cid) {
            return new OriginInfo(null, cid, PackageManagerService.DEFAULT_VERIFY_ENABLE, false);
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

    class InstallParams extends HandlerParams {
        int installFlags;
        final String installerPackageName;
        private InstallArgs mArgs;
        private int mRet;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final String packageAbiOverride;
        final VerificationParams verificationParams;

        InstallParams(OriginInfo origin, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, VerificationParams verificationParams, UserHandle user, String packageAbiOverride) {
            super(user);
            this.origin = origin;
            this.observer = observer;
            this.installFlags = installFlags;
            this.installerPackageName = installerPackageName;
            this.verificationParams = verificationParams;
            this.packageAbiOverride = packageAbiOverride;
        }

        public String toString() {
            return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.origin.file + " cid=" + this.origin.cid + "}";
        }

        public ManifestDigest getManifestDigest() {
            if (this.verificationParams == null) {
                return null;
            }
            return this.verificationParams.getManifestDigest();
        }

        private int installLocationPolicy(PackageInfoLite pkgLite) {
            String packageName = pkgLite.packageName;
            int installLocation = pkgLite.installLocation;
            boolean onSd = (this.installFlags & 8) != 0;
            synchronized (PackageManagerService.this.mPackages) {
                PackageParser.Package pkg = PackageManagerService.this.mPackages.get(packageName);
                if (pkg != null) {
                    if ((this.installFlags & 2) != 0) {
                        if ((this.installFlags & 128) == 0) {
                            try {
                                PackageManagerService.checkDowngrade(pkg, pkgLite);
                            } catch (PackageManagerException e) {
                                Slog.w(PackageManagerService.TAG, "Downgrade detected: " + e.getMessage());
                                return -7;
                            }
                        }
                        if ((pkg.applicationInfo.flags & 1) != 0) {
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
                            return PackageManagerService.isExternal(pkg) ? 2 : 1;
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
            boolean onSd = (this.installFlags & 8) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
            boolean onInt = (this.installFlags & 16) != 0 ? PackageManagerService.DEFAULT_VERIFY_ENABLE : false;
            PackageInfoLite pkgLite = null;
            if (!onInt || !onSd) {
                pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                if (!this.origin.staged && pkgLite.recommendedInstallLocation == -1) {
                    StorageManager storage = StorageManager.from(PackageManagerService.this.mContext);
                    long lowThreshold = storage.getStorageLowBytes(Environment.getDataDirectory());
                    long sizeBytes = PackageManagerService.this.mContainerService.calculateInstalledSize(this.origin.resolvedPath, isForwardLocked(), this.packageAbiOverride);
                    if (PackageManagerService.this.mInstaller.freeCache(sizeBytes + lowThreshold) >= 0) {
                        pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                    }
                    if (pkgLite.recommendedInstallLocation == -6) {
                        pkgLite.recommendedInstallLocation = -1;
                    }
                }
            } else {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing on both internal and external");
                ret = -19;
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
                        } else {
                            this.installFlags |= 16;
                            this.installFlags &= -9;
                        }
                    }
                }
            }
            InstallArgs args = PackageManagerService.this.createInstallArgs(this);
            this.mArgs = args;
            if (ret == 1) {
                int userIdentifier = getUser().getIdentifier();
                if (userIdentifier == -1 && (this.installFlags & 32) != 0) {
                    userIdentifier = 0;
                }
                int requiredUid = PackageManagerService.this.mRequiredVerifierPackage == null ? -1 : PackageManagerService.this.getPackageUid(PackageManagerService.this.mRequiredVerifierPackage, userIdentifier);
                if (this.origin.existing || requiredUid == -1) {
                    ret = args.copyApk(PackageManagerService.this.mContainerService, PackageManagerService.DEFAULT_VERIFY_ENABLE);
                } else if (PackageManagerService.this.isVerificationEnabled(userIdentifier, this.installFlags)) {
                    Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                    verification.addFlags(268435456);
                    verification.setDataAndType(Uri.fromFile(new File(this.origin.resolvedPath)), PackageManagerService.PACKAGE_MIME_TYPE);
                    verification.addFlags(1);
                    List<ResolveInfo> receivers = PackageManagerService.this.queryIntentReceivers(verification, PackageManagerService.PACKAGE_MIME_TYPE, 512, 0);
                    final int verificationId = PackageManagerService.access$3008(PackageManagerService.this);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", this.installerPackageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALL_FLAGS", this.installFlags);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_PACKAGE_NAME", pkgLite.packageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_VERSION_CODE", pkgLite.versionCode);
                    if (this.verificationParams != null) {
                        if (this.verificationParams.getVerificationURI() != null) {
                            verification.putExtra("android.content.pm.extra.VERIFICATION_URI", this.verificationParams.getVerificationURI());
                        }
                        if (this.verificationParams.getOriginatingURI() != null) {
                            verification.putExtra("android.intent.extra.ORIGINATING_URI", this.verificationParams.getOriginatingURI());
                        }
                        if (this.verificationParams.getReferrer() != null) {
                            verification.putExtra("android.intent.extra.REFERRER", this.verificationParams.getReferrer());
                        }
                        if (this.verificationParams.getOriginatingUid() >= 0) {
                            verification.putExtra("android.intent.extra.ORIGINATING_UID", this.verificationParams.getOriginatingUid());
                        }
                        if (this.verificationParams.getInstallerUid() >= 0) {
                            verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", this.verificationParams.getInstallerUid());
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
                                PackageManagerService.this.mContext.sendBroadcastAsUser(sufficientIntent, getUser());
                            }
                        }
                    }
                    ComponentName requiredVerifierComponent = PackageManagerService.this.matchComponentForVerifier(PackageManagerService.this.mRequiredVerifierPackage, receivers);
                    if (ret == 1 && PackageManagerService.this.mRequiredVerifierPackage != null) {
                        verification.setComponent(requiredVerifierComponent);
                        PackageManagerService.this.mContext.sendOrderedBroadcastAsUser(verification, getUser(), "android.permission.PACKAGE_VERIFICATION_AGENT", new BroadcastReceiver() {
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
            this.mRet = ret;
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
            this.mRet = -110;
        }

        public boolean isForwardLocked() {
            if ((this.installFlags & 1) != 0) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }
    }

    private static boolean installOnSd(int installFlags) {
        if ((installFlags & 16) == 0 && (installFlags & 8) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean installForwardLocked(int installFlags) {
        if ((installFlags & 1) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private InstallArgs createInstallArgs(InstallParams params) {
        return (installOnSd(params.installFlags) || params.isForwardLocked()) ? new AsecInstallArgs(params) : new FileInstallArgs(params);
    }

    private InstallArgs createInstallArgsForExisting(int installFlags, String codePath, String resourcePath, String nativeLibraryRoot, String[] instructionSets) {
        boolean isInAsec;
        if (installOnSd(installFlags)) {
            isInAsec = DEFAULT_VERIFY_ENABLE;
        } else if (installForwardLocked(installFlags) && !codePath.startsWith(this.mDrmAppPrivateInstallDir.getAbsolutePath())) {
            isInAsec = DEFAULT_VERIFY_ENABLE;
        } else {
            isInAsec = false;
        }
        if (isInAsec) {
            return new AsecInstallArgs(codePath, instructionSets, installOnSd(installFlags), installForwardLocked(installFlags));
        }
        return new FileInstallArgs(codePath, resourcePath, nativeLibraryRoot, instructionSets);
    }

    static abstract class InstallArgs {
        final String abiOverride;
        final int installFlags;
        final String installerPackageName;
        String[] instructionSets;
        final ManifestDigest manifestDigest;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final UserHandle user;

        abstract boolean checkFreeStorage(IMediaContainerService iMediaContainerService) throws RemoteException;

        abstract void cleanUpResourcesLI();

        abstract int copyApk(IMediaContainerService iMediaContainerService, boolean z) throws RemoteException;

        abstract boolean doPostDeleteLI(boolean z);

        abstract int doPostInstall(int i, int i2);

        abstract int doPreInstall(int i);

        abstract boolean doRename(int i, PackageParser.Package r2, String str);

        abstract String getCodePath();

        abstract String getLegacyNativeLibraryPath();

        abstract String getResourcePath();

        InstallArgs(OriginInfo origin, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, ManifestDigest manifestDigest, UserHandle user, String[] instructionSets, String abiOverride) {
            this.origin = origin;
            this.installFlags = installFlags;
            this.observer = observer;
            this.installerPackageName = installerPackageName;
            this.manifestDigest = manifestDigest;
            this.user = user;
            this.instructionSets = instructionSets;
            this.abiOverride = abiOverride;
        }

        int doPreCopy() {
            return 1;
        }

        int doPostCopy(int uid) {
            return 1;
        }

        protected boolean isFwdLocked() {
            if ((this.installFlags & 1) != 0) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        protected boolean isExternal() {
            if ((this.installFlags & 8) != 0) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        UserHandle getUser() {
            return this.user;
        }
    }

    class FileInstallArgs extends InstallArgs {
        private File codeFile;
        private File legacyNativeLibraryPath;
        private File resourceFile;

        FileInstallArgs(InstallParams params) {
            super(params.origin, params.observer, params.installFlags, params.installerPackageName, params.getManifestDigest(), params.getUser(), null, params.packageAbiOverride);
            if (isFwdLocked()) {
                throw new IllegalArgumentException("Forward locking only supported in ASEC");
            }
        }

        FileInstallArgs(String codePath, String resourcePath, String legacyNativeLibraryPath, String[] instructionSets) {
            super(OriginInfo.fromNothing(), null, 0, null, null, null, instructionSets, null);
            this.codeFile = codePath != null ? new File(codePath) : null;
            this.resourceFile = resourcePath != null ? new File(resourcePath) : null;
            this.legacyNativeLibraryPath = legacyNativeLibraryPath != null ? new File(legacyNativeLibraryPath) : null;
        }

        @Override
        boolean checkFreeStorage(IMediaContainerService imcs) throws RemoteException {
            long sizeBytes = imcs.calculateInstalledSize(this.origin.file.getAbsolutePath(), isFwdLocked(), this.abiOverride);
            StorageManager storage = StorageManager.from(PackageManagerService.this.mContext);
            if (sizeBytes <= storage.getStorageBytesUntilLow(Environment.getDataDirectory())) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        @Override
        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            int ret;
            if (this.origin.staged) {
                Slog.d(PackageManagerService.TAG, this.origin.file + " already staged; skipping copy");
                this.codeFile = this.origin.file;
                this.resourceFile = this.origin.file;
                return 1;
            }
            try {
                File tempDir = PackageManagerService.this.mInstallerService.allocateInternalStageDirLegacy();
                this.codeFile = tempDir;
                this.resourceFile = tempDir;
                int ret2 = imcs.copyPackage(this.origin.file.getAbsolutePath(), new IParcelFileDescriptorFactory.Stub() {
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
                if (ret2 != 1) {
                    Slog.e(PackageManagerService.TAG, "Failed to copy package");
                    return ret2;
                }
                File libraryRoot = new File(this.codeFile, "lib");
                NativeLibraryHelper.Handle handle = null;
                try {
                    handle = NativeLibraryHelper.Handle.create(this.codeFile);
                    ret = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libraryRoot, this.abiOverride);
                } catch (IOException e) {
                    Slog.e(PackageManagerService.TAG, "Copying native libraries failed", e);
                    ret = -110;
                } finally {
                    IoUtils.closeQuietly(handle);
                }
                return ret;
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
            File beforeCodeFile = this.codeFile;
            File afterCodeFile = PackageManagerService.this.getNextCodePath(pkg.packageName);
            Slog.d(PackageManagerService.TAG, "Renaming " + beforeCodeFile + " to " + afterCodeFile);
            try {
                Os.rename(beforeCodeFile.getAbsolutePath(), afterCodeFile.getAbsolutePath());
                if (!SELinux.restoreconRecursive(afterCodeFile)) {
                    Slog.d(PackageManagerService.TAG, "Failed to restorecon");
                    return false;
                }
                this.codeFile = afterCodeFile;
                this.resourceFile = afterCodeFile;
                pkg.codePath = afterCodeFile.getAbsolutePath();
                pkg.baseCodePath = FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath);
                pkg.splitCodePaths = FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths);
                pkg.applicationInfo.setCodePath(pkg.codePath);
                pkg.applicationInfo.setBaseCodePath(pkg.baseCodePath);
                pkg.applicationInfo.setSplitCodePaths(pkg.splitCodePaths);
                pkg.applicationInfo.setResourcePath(pkg.codePath);
                pkg.applicationInfo.setBaseResourcePath(pkg.baseCodePath);
                pkg.applicationInfo.setSplitResourcePaths(pkg.splitCodePaths);
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            } catch (ErrnoException e) {
                Slog.d(PackageManagerService.TAG, "Failed to rename", e);
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

        @Override
        String getLegacyNativeLibraryPath() {
            if (this.legacyNativeLibraryPath != null) {
                return this.legacyNativeLibraryPath.getAbsolutePath();
            }
            return null;
        }

        private boolean cleanUp() {
            if (this.codeFile == null || !this.codeFile.exists()) {
                return false;
            }
            if (this.codeFile.isDirectory()) {
                FileUtils.deleteContents(this.codeFile);
            }
            this.codeFile.delete();
            if (this.resourceFile != null && !FileUtils.contains(this.codeFile, this.resourceFile)) {
                this.resourceFile.delete();
            }
            if (this.legacyNativeLibraryPath != null && !FileUtils.contains(this.codeFile, this.legacyNativeLibraryPath)) {
                if (!FileUtils.deleteContents(this.legacyNativeLibraryPath)) {
                    Slog.w(PackageManagerService.TAG, "Couldn't delete native library directory " + this.legacyNativeLibraryPath);
                }
                this.legacyNativeLibraryPath.delete();
            }
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
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
            if (!allCodePaths.isEmpty()) {
                if (this.instructionSets != null) {
                    String[] dexCodeInstructionSets = PackageManagerService.getDexCodeInstructionSets(this.instructionSets);
                    for (String codePath : allCodePaths) {
                        for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                            int retCode = PackageManagerService.this.mInstaller.rmdex(codePath, dexCodeInstructionSet);
                            if (retCode < 0) {
                                Slog.w(PackageManagerService.TAG, "Couldn't remove dex file for package:  at location " + codePath + ", retcode=" + retCode);
                            }
                        }
                    }
                    return;
                }
                throw new IllegalStateException("instructionSet == null");
            }
        }

        @Override
        boolean doPostDeleteLI(boolean delete) {
            cleanUpResourcesLI();
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }
    }

    private boolean isAsecExternal(String cid) {
        String asecPath = PackageHelper.getSdFilesystem(cid);
        if (asecPath.startsWith(this.mAsecInternalPath)) {
            return false;
        }
        return DEFAULT_VERIFY_ENABLE;
    }

    private static void maybeThrowExceptionForMultiArchCopy(String message, int copyRet) throws PackageManagerException {
        if (copyRet < 0 && copyRet != -114 && copyRet != -113) {
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
        String legacyNativeLibraryDir;
        String packagePath;
        String resourcePath;

        AsecInstallArgs(InstallParams params) {
            super(params.origin, params.observer, params.installFlags, params.installerPackageName, params.getManifestDigest(), params.getUser(), null, params.packageAbiOverride);
        }

        AsecInstallArgs(String fullCodePath, String[] instructionSets, boolean isExternal, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, (isExternal ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null);
            fullCodePath = fullCodePath.endsWith(RES_FILE_NAME) ? fullCodePath : new File(fullCodePath, RES_FILE_NAME).getAbsolutePath();
            int eidx = fullCodePath.lastIndexOf("/");
            String subStr1 = fullCodePath.substring(0, eidx);
            int sidx = subStr1.lastIndexOf("/");
            this.cid = subStr1.substring(sidx + 1, eidx);
            setMountPath(subStr1);
        }

        AsecInstallArgs(String cid, String[] instructionSets, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, (PackageManagerService.this.isAsecExternal(cid) ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null);
            this.cid = cid;
            setMountPath(PackageHelper.getSdDir(cid));
        }

        void createCopyFile() {
            this.cid = PackageManagerService.this.mInstallerService.allocateExternalStageCidLegacy();
        }

        @Override
        boolean checkFreeStorage(IMediaContainerService imcs) throws RemoteException {
            File target;
            long sizeBytes = imcs.calculateInstalledSize(this.packagePath, isFwdLocked(), this.abiOverride);
            if (isExternal()) {
                target = new Environment.UserEnvironment(0).getExternalStorageDirectory();
            } else {
                target = Environment.getDataDirectory();
            }
            StorageManager storage = StorageManager.from(PackageManagerService.this.mContext);
            if (sizeBytes <= storage.getStorageBytesUntilLow(target)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        @Override
        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            if (this.origin.staged) {
                Slog.d(PackageManagerService.TAG, this.origin.cid + " already staged; skipping copy");
                this.cid = this.origin.cid;
                setMountPath(PackageHelper.getSdDir(this.cid));
                return 1;
            }
            if (temp) {
                createCopyFile();
            } else {
                PackageHelper.destroySdDir(this.cid);
            }
            String newMountPath = imcs.copyPackageToContainer(this.origin.file.getAbsolutePath(), this.cid, PackageManagerService.getEncryptKey(), isExternal(), isFwdLocked(), PackageManagerService.deriveAbiOverride(this.abiOverride, null));
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
        String getLegacyNativeLibraryPath() {
            return this.legacyNativeLibraryDir;
        }

        @Override
        int doPreInstall(int status) {
            if (status != 1) {
                PackageHelper.destroySdDir(this.cid);
                return status;
            }
            boolean mounted = PackageHelper.isContainerMounted(this.cid);
            if (!mounted) {
                String newMountPath = PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), 1000);
                if (newMountPath != null) {
                    setMountPath(newMountPath);
                    return status;
                }
                return -18;
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
            pkg.codePath = afterCodeFile.getAbsolutePath();
            pkg.baseCodePath = FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath);
            pkg.splitCodePaths = FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths);
            pkg.applicationInfo.setCodePath(pkg.codePath);
            pkg.applicationInfo.setBaseCodePath(pkg.baseCodePath);
            pkg.applicationInfo.setSplitCodePaths(pkg.splitCodePaths);
            pkg.applicationInfo.setResourcePath(pkg.codePath);
            pkg.applicationInfo.setBaseResourcePath(pkg.baseCodePath);
            pkg.applicationInfo.setSplitResourcePaths(pkg.splitCodePaths);
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        private void setMountPath(String mountPath) {
            File mountFile = new File(mountPath);
            File monolithicFile = new File(mountFile, RES_FILE_NAME);
            if (monolithicFile.exists()) {
                this.packagePath = monolithicFile.getAbsolutePath();
                if (isFwdLocked()) {
                    this.resourcePath = new File(mountFile, PUBLIC_RES_FILE_NAME).getAbsolutePath();
                } else {
                    this.resourcePath = this.packagePath;
                }
            } else {
                this.packagePath = mountFile.getAbsolutePath();
                this.resourcePath = this.packagePath;
            }
            this.legacyNativeLibraryDir = new File(mountFile, "lib").getAbsolutePath();
        }

        @Override
        int doPostInstall(int status, int uid) {
            int groupOwner;
            String protectedFile;
            if (status != 1) {
                cleanUp();
                return status;
            }
            if (isFwdLocked()) {
                groupOwner = UserHandle.getSharedAppGid(uid);
                protectedFile = RES_FILE_NAME;
            } else {
                groupOwner = -1;
                protectedFile = null;
            }
            if (uid < 10000 || !PackageHelper.fixSdPermissions(this.cid, groupOwner, protectedFile)) {
                Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
                PackageHelper.destroySdDir(this.cid);
                return -18;
            }
            boolean mounted = PackageHelper.isContainerMounted(this.cid);
            if (!mounted) {
                PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), Process.myUid());
                return status;
            }
            return status;
        }

        private void cleanUp() {
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
            if (!allCodePaths.isEmpty()) {
                if (this.instructionSets != null) {
                    String[] dexCodeInstructionSets = PackageManagerService.getDexCodeInstructionSets(this.instructionSets);
                    for (String codePath : allCodePaths) {
                        for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                            int retCode = PackageManagerService.this.mInstaller.rmdex(codePath, dexCodeInstructionSet);
                            if (retCode < 0) {
                                Slog.w(PackageManagerService.TAG, "Couldn't remove dex file for package:  at location " + codePath + ", retcode=" + retCode);
                            }
                        }
                    }
                    return;
                }
                throw new IllegalStateException("instructionSet == null");
            }
        }

        boolean matchContainer(String app) {
            if (this.cid.startsWith(app)) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        String getPackageName() {
            return PackageManagerService.getAsecPackageName(this.cid);
        }

        @Override
        boolean doPostDeleteLI(boolean delete) {
            List<String> allCodePaths = getAllCodePaths();
            boolean mounted = PackageHelper.isContainerMounted(this.cid);
            if (mounted && PackageHelper.unMountSdDir(this.cid)) {
                mounted = false;
            }
            if (!mounted && delete) {
                cleanUpResourcesLI(allCodePaths);
            }
            if (mounted) {
                return false;
            }
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        @Override
        int doPreCopy() {
            return (!isFwdLocked() || PackageHelper.fixSdPermissions(this.cid, PackageManagerService.this.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, 0), RES_FILE_NAME)) ? 1 : -18;
        }

        @Override
        int doPostCopy(int uid) {
            if (!isFwdLocked() || (uid >= 10000 && PackageHelper.fixSdPermissions(this.cid, UserHandle.getSharedAppGid(uid), RES_FILE_NAME))) {
                return 1;
            }
            Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
            PackageHelper.destroySdDir(this.cid);
            return -18;
        }
    }

    static String getAsecPackageName(String packageCid) {
        int idx = packageCid.lastIndexOf(INSTALL_PACKAGE_SUFFIX);
        return idx == -1 ? packageCid : packageCid.substring(0, idx);
    }

    private static String getNextCodePath(String oldCodePath, String prefix, String suffix) {
        String subStr;
        int idx = 1;
        if (oldCodePath != null) {
            String subStr2 = oldCodePath;
            if (suffix != null && subStr2.endsWith(suffix)) {
                subStr2 = subStr2.substring(0, subStr2.length() - suffix.length());
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

    private File getNextCodePath(String packageName) {
        File result;
        int suffix = 1;
        do {
            result = new File(this.mAppInstallDir, packageName + INSTALL_PACKAGE_SUFFIX + suffix);
            suffix++;
        } while (result.exists());
        return result;
    }

    private static boolean ignoreCodePath(String fullPathStr) {
        String apkName = deriveCodePathName(fullPathStr);
        int idx = apkName.lastIndexOf(INSTALL_PACKAGE_SUFFIX);
        if (idx != -1 && idx + 1 < apkName.length()) {
            String version = apkName.substring(idx + 1);
            try {
                Integer.parseInt(version);
                return DEFAULT_VERIFY_ENABLE;
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        File codeFile = new File(codePath);
        String name = codeFile.getName();
        if (!codeFile.isDirectory()) {
            if (name.endsWith(".apk") || name.endsWith(".tmp")) {
                int lastDot = name.lastIndexOf(46);
                return name.substring(0, lastDot);
            }
            Slog.w(TAG, "Odd, " + codePath + " doesn't look like an APK");
            return null;
        }
        return name;
    }

    class PackageInstalledInfo {
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
            this.returnCode = code;
            this.returnMsg = msg;
            Slog.w(PackageManagerService.TAG, msg);
        }

        public void setError(String msg, PackageParser.PackageParserException e) {
            this.returnCode = e.error;
            this.returnMsg = ExceptionUtils.getCompleteMessage(msg, e);
            Slog.w(PackageManagerService.TAG, msg, e);
        }

        public void setError(String msg, PackageManagerException e) {
            this.returnCode = e.error;
            this.returnMsg = ExceptionUtils.getCompleteMessage(msg, e);
            Slog.w(PackageManagerService.TAG, msg, e);
        }
    }

    private void installNewPackageLI(PackageParser.Package pkg, int parseFlags, int scanFlags, UserHandle user, String installerPackageName, PackageInstalledInfo res) {
        String pkgName = pkg.packageName;
        boolean dataDirExists = getDataPathForPackage(pkg.packageName, 0).exists();
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
                PackageParser.Package newPackage = scanPackageLI(pkg, parseFlags, scanFlags, System.currentTimeMillis(), user);
                updateSettingsLI(newPackage, installerPackageName, null, null, res);
                if (res.returnCode != 1) {
                    deletePackageLI(pkgName, UserHandle.ALL, false, null, null, dataDirExists ? 1 : 0, res.removedInfo, DEFAULT_VERIFY_ENABLE);
                }
            } catch (PackageManagerException e) {
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
            }
        }
    }

    private boolean checkUpgradeKeySetLP(PackageSetting oldPS, PackageParser.Package newPkg) {
        long[] upgradeKeySets = oldPS.keySetData.getUpgradeKeySets();
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        for (long j : upgradeKeySets) {
            Set<PublicKey> upgradeSet = ksms.getPublicKeysFromKeySetLPr(j);
            if (newPkg.mSigningKeys.containsAll(upgradeSet)) {
                return DEFAULT_VERIFY_ENABLE;
            }
        }
        return false;
    }

    private void replacePackageLI(PackageParser.Package pkg, int parseFlags, int scanFlags, UserHandle user, String installerPackageName, PackageInstalledInfo res) {
        String pkgName = pkg.packageName;
        synchronized (this.mPackages) {
            PackageParser.Package oldPackage = this.mPackages.get(pkgName);
            PackageSetting ps = this.mSettings.mPackages.get(pkgName);
            if (ps == null || !ps.keySetData.isUsingUpgradeKeySets() || ps.sharedUser != null) {
                if (compareSignatures(oldPackage.mSignatures, pkg.mSignatures) != 0) {
                    res.setError(-7, "New package has a different signature: " + pkgName);
                    return;
                }
            } else if (!checkUpgradeKeySetLP(ps, pkg)) {
                res.setError(-7, "New package not signed by keys specified by upgrade-keysets: " + pkgName);
                return;
            }
            int[] allUsers = sUserManager.getUserIds();
            boolean[] perUserInstalled = new boolean[allUsers.length];
            for (int i = 0; i < allUsers.length; i++) {
                perUserInstalled[i] = ps != null ? ps.getInstalled(allUsers[i]) : false;
            }
            boolean sysPkg = isSystemApp(oldPackage);
            if (sysPkg) {
                replaceSystemPackageLI(oldPackage, pkg, parseFlags, scanFlags, user, allUsers, perUserInstalled, installerPackageName, res);
            } else {
                replaceNonSystemPackageLI(oldPackage, pkg, parseFlags, scanFlags, user, allUsers, perUserInstalled, installerPackageName, res);
            }
        }
    }

    private void replaceNonSystemPackageLI(PackageParser.Package deletedPackage, PackageParser.Package pkg, int parseFlags, int scanFlags, UserHandle user, int[] allUsers, boolean[] perUserInstalled, String installerPackageName, PackageInstalledInfo res) {
        long origUpdateTime;
        String pkgName = deletedPackage.packageName;
        boolean deletedPkg = DEFAULT_VERIFY_ENABLE;
        boolean updatedSettings = false;
        if (pkg.mExtras != null) {
            origUpdateTime = ((PackageSetting) pkg.mExtras).lastUpdateTime;
        } else {
            origUpdateTime = 0;
        }
        if (!deletePackageLI(pkgName, null, DEFAULT_VERIFY_ENABLE, null, null, 1, res.removedInfo, DEFAULT_VERIFY_ENABLE)) {
            res.setError(-10, "replaceNonSystemPackageLI");
            deletedPkg = false;
        } else {
            if (isForwardLocked(deletedPackage) || isExternal(deletedPackage)) {
                int[] uidArray = {deletedPackage.applicationInfo.uid};
                ArrayList<String> pkgList = new ArrayList<>(1);
                pkgList.add(deletedPackage.applicationInfo.packageName);
                sendResourcesChangedBroadcast(false, DEFAULT_VERIFY_ENABLE, pkgList, uidArray, null);
            }
            deleteCodeCacheDirsLI(pkgName);
            try {
                PackageParser.Package newPackage = scanPackageLI(pkg, parseFlags, scanFlags | 64, System.currentTimeMillis(), user);
                updateSettingsLI(newPackage, installerPackageName, allUsers, perUserInstalled, res);
                updatedSettings = DEFAULT_VERIFY_ENABLE;
            } catch (PackageManagerException e) {
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
            }
        }
        if (res.returnCode != 1) {
            if (updatedSettings) {
                deletePackageLI(pkgName, null, DEFAULT_VERIFY_ENABLE, allUsers, perUserInstalled, 1, res.removedInfo, DEFAULT_VERIFY_ENABLE);
            }
            if (deletedPkg) {
                File restoreFile = new File(deletedPackage.codePath);
                boolean oldOnSd = isExternal(deletedPackage);
                int oldParseFlags = this.mDefParseFlags | 2 | (isForwardLocked(deletedPackage) ? 16 : 0) | (oldOnSd ? 32 : 0);
                try {
                    scanPackageLI(restoreFile, oldParseFlags, 72, origUpdateTime, (UserHandle) null);
                    synchronized (this.mPackages) {
                        updatePermissionsLPw(deletedPackage.packageName, deletedPackage, 1);
                        this.mSettings.writeLPr();
                    }
                    Slog.i(TAG, "Successfully restored package : " + pkgName + " after failed upgrade");
                } catch (PackageManagerException e2) {
                    Slog.e(TAG, "Failed to restore package : " + pkgName + " after failed upgrade: " + e2.getMessage());
                }
            }
        }
    }

    private void replaceSystemPackageLI(PackageParser.Package deletedPackage, PackageParser.Package pkg, int parseFlags, int scanFlags, UserHandle user, int[] allUsers, boolean[] perUserInstalled, String installerPackageName, PackageInstalledInfo res) {
        boolean disabledSystem;
        PackageParser.Package newPackage;
        boolean updatedSettings = false;
        int parseFlags2 = parseFlags | 1;
        if ((deletedPackage.applicationInfo.flags & 1073741824) != 0) {
            parseFlags2 |= 128;
        }
        String packageName = deletedPackage.packageName;
        if (packageName == null) {
            res.setError(-10, "Attempt to delete null packageName.");
            return;
        }
        synchronized (this.mPackages) {
            PackageParser.Package oldPkg = this.mPackages.get(packageName);
            PackageSetting oldPkgSetting = this.mSettings.mPackages.get(packageName);
            if (oldPkg == null || oldPkg.applicationInfo == null || oldPkgSetting == null) {
                res.setError(-10, "Couldn't find package:" + packageName + " information");
            } else {
                killApplication(packageName, oldPkg.applicationInfo.uid, "replace sys pkg");
                res.removedInfo.uid = oldPkg.applicationInfo.uid;
                res.removedInfo.removedPackage = packageName;
                removePackageLI(oldPkgSetting, DEFAULT_VERIFY_ENABLE);
                synchronized (this.mPackages) {
                    disabledSystem = this.mSettings.disableSystemPackageLPw(packageName);
                    if (!disabledSystem && deletedPackage != null) {
                        res.removedInfo.args = createInstallArgsForExisting(0, deletedPackage.applicationInfo.getCodePath(), deletedPackage.applicationInfo.getResourcePath(), deletedPackage.applicationInfo.nativeLibraryRootDir, getAppDexInstructionSets(deletedPackage.applicationInfo));
                    } else {
                        res.removedInfo.args = null;
                    }
                }
                deleteCodeCacheDirsLI(packageName);
                res.returnCode = 1;
                pkg.applicationInfo.flags |= 128;
                try {
                    newPackage = scanPackageLI(pkg, parseFlags2, scanFlags, 0L, user);
                    try {
                        if (newPackage.mExtras != null) {
                            PackageSetting newPkgSetting = (PackageSetting) newPackage.mExtras;
                            newPkgSetting.firstInstallTime = oldPkgSetting.firstInstallTime;
                            newPkgSetting.lastUpdateTime = System.currentTimeMillis();
                            if (oldPkgSetting.sharedUser != newPkgSetting.sharedUser) {
                                res.setError(-8, "Forbidding shared user change from " + oldPkgSetting.sharedUser + " to " + newPkgSetting.sharedUser);
                                updatedSettings = DEFAULT_VERIFY_ENABLE;
                            }
                        }
                        if (res.returnCode == 1) {
                            updateSettingsLI(newPackage, installerPackageName, allUsers, perUserInstalled, res);
                            updatedSettings = DEFAULT_VERIFY_ENABLE;
                        }
                    } catch (PackageManagerException e) {
                        e = e;
                        res.setError("Package couldn't be installed in " + pkg.codePath, e);
                    }
                } catch (PackageManagerException e2) {
                    e = e2;
                    newPackage = null;
                }
                if (res.returnCode != 1) {
                    if (newPackage != null) {
                        removeInstalledPackageLI(newPackage, DEFAULT_VERIFY_ENABLE);
                    }
                    try {
                        scanPackageLI(oldPkg, parseFlags2, 8, 0L, user);
                    } catch (PackageManagerException e3) {
                        Slog.e(TAG, "Failed to restore original package: " + e3.getMessage());
                    }
                    synchronized (this.mPackages) {
                        if (disabledSystem) {
                            this.mSettings.enableSystemPackageLPw(packageName);
                            if (updatedSettings) {
                                this.mSettings.setInstallerPackageName(packageName, oldPkgSetting.installerPackageName);
                            }
                            this.mSettings.writeLPr();
                        } else {
                            if (updatedSettings) {
                            }
                            this.mSettings.writeLPr();
                        }
                    }
                }
            }
        }
    }

    private void updateSettingsLI(PackageParser.Package newPackage, String installerPackageName, int[] allUsers, boolean[] perUserInstalled, PackageInstalledInfo res) {
        PackageSetting ps;
        String pkgName = newPackage.packageName;
        synchronized (this.mPackages) {
            this.mSettings.setInstallStatus(pkgName, 0);
            this.mSettings.writeLPr();
        }
        synchronized (this.mPackages) {
            updatePermissionsLPw(newPackage.packageName, newPackage, (newPackage.permissions.size() > 0 ? 1 : 0) | 2);
            if (isSystemApp(newPackage) && (ps = this.mSettings.mPackages.get(pkgName)) != null) {
                if (res.origUsers != null) {
                    int[] arr$ = res.origUsers;
                    for (int userHandle : arr$) {
                        ps.setEnabled(0, userHandle, installerPackageName);
                    }
                }
                if (allUsers != null && perUserInstalled != null) {
                    for (int i = 0; i < allUsers.length; i++) {
                        ps.setInstalled(perUserInstalled[i], allUsers[i]);
                    }
                }
            }
            res.name = pkgName;
            res.uid = newPackage.applicationInfo.uid;
            res.pkg = newPackage;
            this.mSettings.setInstallStatus(pkgName, 1);
            this.mSettings.setInstallerPackageName(pkgName, installerPackageName);
            res.returnCode = 1;
            this.mSettings.writeLPr();
        }
    }

    private void installPackageLI(InstallArgs args, PackageInstalledInfo res) {
        PackageSetting ps;
        int i;
        boolean sigsOk;
        int installFlags = args.installFlags;
        String installerPackageName = args.installerPackageName;
        File tmpPackageFile = new File(args.getCodePath());
        boolean forwardLocked = (installFlags & 1) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        boolean onSd = (installFlags & 8) != 0 ? DEFAULT_VERIFY_ENABLE : false;
        boolean replace = false;
        res.returnCode = 1;
        int parseFlags = this.mDefParseFlags | 2 | (forwardLocked ? 16 : 0) | (onSd ? 32 : 0);
        PackageParser pp = new PackageParser();
        pp.setSeparateProcesses(this.mSeparateProcesses);
        pp.setDisplayMetrics(this.mMetrics);
        try {
            PackageParser.Package pkg = pp.parsePackage(tmpPackageFile, parseFlags);
            pkg.cpuAbiOverride = args.abiOverride;
            String pkgName = pkg.packageName;
            res.name = pkgName;
            if ((pkg.applicationInfo.flags & 256) != 0 && (installFlags & 4) == 0) {
                res.setError(-15, "installPackageLI");
                return;
            }
            try {
                pp.collectCertificates(pkg, parseFlags);
                pp.collectManifestDigest(pkg);
                if (args.manifestDigest != null && !args.manifestDigest.equals(pkg.manifestDigest)) {
                    res.setError(-23, "Manifest digest changed");
                    return;
                }
                String oldCodePath = null;
                boolean systemApp = false;
                synchronized (this.mPackages) {
                    if ((installFlags & 2) != 0) {
                        String oldName = this.mSettings.mRenamedPackages.get(pkgName);
                        if (pkg.mOriginalPackages != null && pkg.mOriginalPackages.contains(oldName) && this.mPackages.containsKey(oldName)) {
                            pkg.setPackageName(oldName);
                            pkgName = pkg.packageName;
                            replace = DEFAULT_VERIFY_ENABLE;
                        } else if (this.mPackages.containsKey(pkgName)) {
                            replace = DEFAULT_VERIFY_ENABLE;
                        }
                        ps = this.mSettings.mPackages.get(pkgName);
                        if (ps != null) {
                            if (!ps.keySetData.isUsingUpgradeKeySets() || ps.sharedUser != null) {
                                try {
                                    verifySignaturesLP(ps, pkg);
                                } catch (PackageManagerException e) {
                                    res.setError(e.error, e.getMessage());
                                    return;
                                }
                            } else if (!checkUpgradeKeySetLP(ps, pkg)) {
                                res.setError(-7, "Package " + pkg.packageName + " upgrade keys do not match the previously installed version");
                                return;
                            }
                            oldCodePath = this.mSettings.mPackages.get(pkgName).codePathString;
                            if (ps.pkg != null && ps.pkg.applicationInfo != null) {
                                systemApp = (ps.pkg.applicationInfo.flags & 1) != 0 ? DEFAULT_VERIFY_ENABLE : false;
                            }
                            res.origUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), DEFAULT_VERIFY_ENABLE);
                        }
                        int N = pkg.permissions.size();
                        for (i = N - 1; i >= 0; i--) {
                            PackageParser.Permission perm = (PackageParser.Permission) pkg.permissions.get(i);
                            BasePermission bp = this.mSettings.mPermissions.get(perm.info.name);
                            if (bp != null) {
                                if (!bp.sourcePackage.equals(pkg.packageName) || !(bp.packageSetting instanceof PackageSetting) || !bp.packageSetting.keySetData.isUsingUpgradeKeySets() || ((PackageSetting) bp.packageSetting).sharedUser != null) {
                                    sigsOk = compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) == 0 ? DEFAULT_VERIFY_ENABLE : false;
                                } else {
                                    sigsOk = checkUpgradeKeySetLP((PackageSetting) bp.packageSetting, pkg);
                                }
                                if (sigsOk) {
                                    continue;
                                } else {
                                    if (!bp.sourcePackage.equals("android")) {
                                        res.setError(-112, "Package " + pkg.packageName + " attempting to redeclare permission " + perm.info.name + " already owned by " + bp.sourcePackage);
                                        res.origPermission = perm.info.name;
                                        res.origPackage = bp.sourcePackage;
                                        return;
                                    }
                                    Slog.w(TAG, "Package " + pkg.packageName + " attempting to redeclare system permission " + perm.info.name + "; ignoring new declaration");
                                    pkg.permissions.remove(i);
                                }
                            }
                        }
                        if (!systemApp && onSd) {
                            res.setError(-19, "Cannot install updates to system apps on sdcard");
                            return;
                        }
                        if (args.doRename(res.returnCode, pkg, oldCodePath)) {
                            res.setError(-4, "Failed rename");
                            return;
                        }
                        if (replace) {
                            replacePackageLI(pkg, parseFlags, 2076, args.user, installerPackageName, res);
                        } else {
                            installNewPackageLI(pkg, parseFlags, 1052, args.user, installerPackageName, res);
                        }
                        synchronized (this.mPackages) {
                            PackageSetting ps2 = this.mSettings.mPackages.get(pkgName);
                            if (ps2 != null) {
                                res.newUsers = ps2.queryInstalledUsers(sUserManager.getUserIds(), DEFAULT_VERIFY_ENABLE);
                            }
                        }
                        return;
                    }
                    ps = this.mSettings.mPackages.get(pkgName);
                    if (ps != null) {
                    }
                    int N2 = pkg.permissions.size();
                    while (i >= 0) {
                    }
                    if (!systemApp) {
                    }
                    if (args.doRename(res.returnCode, pkg, oldCodePath)) {
                    }
                }
            } catch (PackageParser.PackageParserException e2) {
                res.setError("Failed collect during installPackageLI", e2);
            }
        } catch (PackageParser.PackageParserException e3) {
            res.setError("Failed parse during installPackageLI", e3);
        }
    }

    private static boolean isForwardLocked(PackageParser.Package pkg) {
        if ((pkg.applicationInfo.flags & 536870912) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isForwardLocked(ApplicationInfo info) {
        if ((info.flags & 536870912) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private boolean isForwardLocked(PackageSetting ps) {
        if ((ps.pkgFlags & 536870912) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isMultiArch(PackageSetting ps) {
        if ((ps.pkgFlags & SoundTriggerHelper.STATUS_ERROR) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isMultiArch(ApplicationInfo info) {
        if ((info.flags & SoundTriggerHelper.STATUS_ERROR) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isExternal(PackageParser.Package pkg) {
        if ((pkg.applicationInfo.flags & 262144) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isExternal(PackageSetting ps) {
        if ((ps.pkgFlags & 262144) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isExternal(ApplicationInfo info) {
        if ((info.flags & 262144) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isSystemApp(PackageParser.Package pkg) {
        if ((pkg.applicationInfo.flags & 1) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isPrivilegedApp(PackageParser.Package pkg) {
        if ((pkg.applicationInfo.flags & 1073741824) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isSystemApp(ApplicationInfo info) {
        if ((info.flags & 1) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isSystemApp(PackageSetting ps) {
        if ((ps.pkgFlags & 1) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isUpdatedSystemApp(PackageSetting ps) {
        if ((ps.pkgFlags & 128) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isUpdatedSystemApp(PackageParser.Package pkg) {
        if ((pkg.applicationInfo.flags & 128) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private static boolean isUpdatedSystemApp(ApplicationInfo info) {
        if ((info.flags & 128) != 0) {
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    private int packageFlagsToInstallFlags(PackageSetting ps) {
        int installFlags = 0;
        if (isExternal(ps)) {
            installFlags = 0 | 8;
        }
        if (isForwardLocked(ps)) {
            return installFlags | 1;
        }
        return installFlags;
    }

    private void deleteTempPackageFiles() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("vmdl") && name.endsWith(".tmp")) {
                    return PackageManagerService.DEFAULT_VERIFY_ENABLE;
                }
                return false;
            }
        };
        File[] arr$ = this.mDrmAppPrivateInstallDir.listFiles(filter);
        for (File file : arr$) {
            file.delete();
        }
    }

    public void deletePackageAsUser(String packageName, IPackageDeleteObserver observer, int userId, int flags) {
        deletePackage(packageName, new PackageManager.LegacyPackageDeleteObserver(observer).getBinder(), userId, flags);
    }

    public void deletePackage(final String packageName, final IPackageDeleteObserver2 observer, final int userId, final int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        int uid = Binder.getCallingUid();
        if (UserHandle.getUserId(uid) != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "deletePackage for user " + userId);
        }
        if (isUserRestricted(userId, "no_uninstall_apps")) {
            try {
                observer.onPackageDeleted(packageName, -3, (String) null);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        boolean uninstallBlocked = false;
        if ((flags & 2) != 0) {
            int[] users = sUserManager.getUserIds();
            int i = 0;
            while (true) {
                if (i >= users.length) {
                    break;
                }
                if (!getBlockUninstallForUser(packageName, users[i])) {
                    i++;
                } else {
                    uninstallBlocked = DEFAULT_VERIFY_ENABLE;
                    break;
                }
            }
        } else {
            uninstallBlocked = getBlockUninstallForUser(packageName, userId);
        }
        if (uninstallBlocked) {
            try {
                observer.onPackageDeleted(packageName, -4, (String) null);
            } catch (RemoteException e2) {
            }
        } else {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PackageManagerService.this.mHandler.removeCallbacks(this);
                    int returnCode = PackageManagerService.this.deletePackageX(packageName, userId, flags);
                    if (observer != null) {
                        try {
                            observer.onPackageDeleted(packageName, returnCode, (String) null);
                        } catch (RemoteException e3) {
                            Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                        }
                    }
                }
            });
        }
    }

    private boolean isPackageDeviceAdmin(String packageName, int userId) {
        int[] users;
        IDevicePolicyManager dpm = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (dpm != null) {
            try {
                if (dpm.isDeviceOwner(packageName)) {
                    return DEFAULT_VERIFY_ENABLE;
                }
                if (userId == -1) {
                    users = sUserManager.getUserIds();
                } else {
                    users = new int[]{userId};
                }
                for (int i : users) {
                    if (dpm.packageHasActiveAdmins(packageName, i)) {
                        return DEFAULT_VERIFY_ENABLE;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    private int deletePackageX(String packageName, int userId, int flags) {
        int[] allUsers;
        boolean[] perUserInstalled;
        boolean res;
        boolean systemUpdate;
        PackageRemovedInfo info = new PackageRemovedInfo();
        UserHandle removeForUser = (flags & 2) != 0 ? UserHandle.ALL : new UserHandle(userId);
        if (isPackageDeviceAdmin(packageName, removeForUser.getIdentifier())) {
            Slog.w(TAG, "Not removing package " + packageName + ": has active device admin");
            return -2;
        }
        boolean removedForAllUsers = false;
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            allUsers = sUserManager.getUserIds();
            perUserInstalled = new boolean[allUsers.length];
            for (int i = 0; i < allUsers.length; i++) {
                perUserInstalled[i] = ps != null ? ps.getInstalled(allUsers[i]) : false;
            }
        }
        synchronized (this.mInstallLock) {
            res = deletePackageLI(packageName, removeForUser, DEFAULT_VERIFY_ENABLE, allUsers, perUserInstalled, flags | REMOVE_CHATTY, info, DEFAULT_VERIFY_ENABLE);
            systemUpdate = info.isRemovedPackageSystemUpdate;
            if (res && !systemUpdate && this.mPackages.get(packageName) == null) {
                removedForAllUsers = DEFAULT_VERIFY_ENABLE;
            }
        }
        if (res) {
            info.sendBroadcast(DEFAULT_VERIFY_ENABLE, systemUpdate, removedForAllUsers);
            if (systemUpdate) {
                Bundle extras = new Bundle(1);
                extras.putInt("android.intent.extra.UID", info.removedAppId >= 0 ? info.removedAppId : info.uid);
                extras.putBoolean("android.intent.extra.REPLACING", DEFAULT_VERIFY_ENABLE);
                sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, null, null, null);
                sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", packageName, extras, null, null, null);
                sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, packageName, null, null);
            }
        }
        Runtime.getRuntime().gc();
        if (info.args != null) {
            synchronized (this.mInstallLock) {
                info.args.doPostDeleteLI(DEFAULT_VERIFY_ENABLE);
            }
        }
        return res ? 1 : -1;
    }

    static class PackageRemovedInfo {
        String removedPackage;
        int uid = -1;
        int removedAppId = -1;
        int[] removedUsers = null;
        boolean isRemovedPackageSystemUpdate = false;
        InstallArgs args = null;

        PackageRemovedInfo() {
        }

        void sendBroadcast(boolean fullRemove, boolean replacing, boolean removedForAllUsers) {
            Bundle extras = new Bundle(1);
            extras.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            extras.putBoolean("android.intent.extra.DATA_REMOVED", fullRemove);
            if (replacing) {
                extras.putBoolean("android.intent.extra.REPLACING", PackageManagerService.DEFAULT_VERIFY_ENABLE);
            }
            extras.putBoolean("android.intent.extra.REMOVED_FOR_ALL_USERS", removedForAllUsers);
            if (this.removedPackage != null) {
                PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, extras, null, null, this.removedUsers);
                if (fullRemove && !replacing) {
                    PackageManagerService.sendPackageBroadcast("android.intent.action.PACKAGE_FULLY_REMOVED", this.removedPackage, extras, null, null, this.removedUsers);
                }
            }
            if (this.removedAppId >= 0) {
                PackageManagerService.sendPackageBroadcast("android.intent.action.UID_REMOVED", null, extras, null, null, this.removedUsers);
            }
        }
    }

    private void removePackageDataLI(PackageSetting ps, int[] allUserHandles, boolean[] perUserInstalled, PackageRemovedInfo outInfo, int flags, boolean writeSettings) {
        PackageSetting deletedPs;
        String packageName = ps.name;
        removePackageLI(ps, (REMOVE_CHATTY & flags) != 0);
        synchronized (this.mPackages) {
            deletedPs = this.mSettings.mPackages.get(packageName);
            if (outInfo != null) {
                outInfo.removedPackage = packageName;
                outInfo.removedUsers = deletedPs != null ? deletedPs.queryInstalledUsers(sUserManager.getUserIds(), DEFAULT_VERIFY_ENABLE) : null;
            }
        }
        if ((flags & 1) == 0) {
            removeDataDirsLI(packageName);
            schedulePackageCleaning(packageName, -1, DEFAULT_VERIFY_ENABLE);
        }
        synchronized (this.mPackages) {
            if (deletedPs != null) {
                if ((flags & 1) == 0) {
                    if (outInfo != null) {
                        this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(packageName);
                        outInfo.removedAppId = this.mSettings.removePackageLPw(packageName);
                    }
                    if (deletedPs != null) {
                        updatePermissionsLPw(deletedPs.name, null, 0);
                        if (deletedPs.sharedUser != null) {
                            this.mSettings.updateSharedUserPermsLPw(deletedPs, this.mGlobalGids);
                        }
                    }
                    clearPackagePreferredActivitiesLPw(deletedPs.name, -1);
                }
                if (allUserHandles != null && perUserInstalled != null) {
                    for (int i = 0; i < allUserHandles.length; i++) {
                        ps.setInstalled(perUserInstalled[i], allUserHandles[i]);
                    }
                }
                if (writeSettings) {
                }
            } else if (writeSettings) {
                this.mSettings.writeLPr();
            }
        }
        if (outInfo != null) {
            removeKeystoreDataIfNeeded(-1, outInfo.removedAppId);
        }
    }

    static boolean locationIsPrivileged(File path) {
        try {
            String privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app").getCanonicalPath();
            return path.getCanonicalPath().startsWith(privilegedAppDir);
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + path);
            return false;
        }
    }

    private boolean deleteSystemPackageLI(PackageSetting newPs, int[] allUserHandles, boolean[] perUserInstalled, int flags, PackageRemovedInfo outInfo, boolean writeSettings) {
        PackageSetting disabledPs;
        int flags2;
        boolean applyUserRestrictions = (allUserHandles == null || perUserInstalled == null) ? false : DEFAULT_VERIFY_ENABLE;
        synchronized (this.mPackages) {
            disabledPs = this.mSettings.getDisabledSystemPkgLPr(newPs.name);
        }
        if (disabledPs == null) {
            Slog.w(TAG, "Attempt to delete unknown system package " + newPs.name);
            return false;
        }
        outInfo.isRemovedPackageSystemUpdate = DEFAULT_VERIFY_ENABLE;
        if (disabledPs.versionCode < newPs.versionCode) {
            flags2 = flags & (-2);
        } else {
            flags2 = flags | 1;
        }
        boolean ret = deleteInstalledPackageLI(newPs, DEFAULT_VERIFY_ENABLE, flags2, allUserHandles, perUserInstalled, outInfo, writeSettings);
        if (!ret) {
            return false;
        }
        synchronized (this.mPackages) {
            this.mSettings.enableSystemPackageLPw(newPs.name);
            NativeLibraryHelper.removeNativeBinariesLI(newPs.legacyNativeLibraryPathString);
        }
        int parseFlags = 5;
        if (locationIsPrivileged(disabledPs.codePath)) {
            parseFlags = 5 | 128;
        }
        try {
            PackageParser.Package newPkg = scanPackageLI(disabledPs.codePath, parseFlags, 32, 0L, (UserHandle) null);
            synchronized (this.mPackages) {
                PackageSetting ps = this.mSettings.mPackages.get(newPkg.packageName);
                updatePermissionsLPw(newPkg.packageName, newPkg, 3);
                if (applyUserRestrictions) {
                    for (int i = 0; i < allUserHandles.length; i++) {
                        ps.setInstalled(perUserInstalled[i], allUserHandles[i]);
                    }
                    this.mSettings.writeAllUsersPackageRestrictionsLPr();
                }
                if (writeSettings) {
                    this.mSettings.writeLPr();
                }
            }
            return DEFAULT_VERIFY_ENABLE;
        } catch (PackageManagerException e) {
            Slog.w(TAG, "Failed to restore system package:" + newPs.name + ": " + e.getMessage());
            return false;
        }
    }

    private boolean deleteInstalledPackageLI(PackageSetting ps, boolean deleteCodeAndResources, int flags, int[] allUserHandles, boolean[] perUserInstalled, PackageRemovedInfo outInfo, boolean writeSettings) {
        if (outInfo != null) {
            outInfo.uid = ps.appId;
        }
        removePackageDataLI(ps, allUserHandles, perUserInstalled, outInfo, flags, writeSettings);
        if (deleteCodeAndResources && outInfo != null) {
            outInfo.args = createInstallArgsForExisting(packageFlagsToInstallFlags(ps), ps.codePathString, ps.resourcePathString, ps.legacyNativeLibraryPathString, getAppDexInstructionSets(ps));
            return DEFAULT_VERIFY_ENABLE;
        }
        return DEFAULT_VERIFY_ENABLE;
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
            return DEFAULT_VERIFY_ENABLE;
        }
    }

    public boolean getBlockUninstallForUser(String packageName, int userId) {
        boolean blockUninstall;
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Log.i(TAG, "Package doesn't exist in get block uninstall " + packageName);
                blockUninstall = false;
            } else {
                blockUninstall = ps.getBlockUninstall(userId);
            }
        }
        return blockUninstall;
    }

    private boolean deletePackageLI(String packageName, UserHandle user, boolean deleteCodeAndResources, int[] allUserHandles, boolean[] perUserInstalled, int flags, PackageRemovedInfo outInfo, boolean writeSettings) {
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        int removeUser = -1;
        int appId = -1;
        synchronized (this.mPackages) {
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                return false;
            }
            if ((!isSystemApp(ps) || (flags & 4) != 0) && user != null && user.getIdentifier() != -1) {
                ps.setUserState(user.getIdentifier(), 0, false, DEFAULT_VERIFY_ENABLE, DEFAULT_VERIFY_ENABLE, false, null, null, null, false);
                if (!isSystemApp(ps)) {
                    if (ps.isAnyInstalled(sUserManager.getUserIds())) {
                        removeUser = user.getIdentifier();
                        appId = ps.appId;
                        this.mSettings.writePackageRestrictionsLPr(removeUser);
                    } else {
                        ps.setInstalled(DEFAULT_VERIFY_ENABLE, user.getIdentifier());
                    }
                } else {
                    removeUser = user.getIdentifier();
                    appId = ps.appId;
                    this.mSettings.writePackageRestrictionsLPr(removeUser);
                }
            }
            if (removeUser >= 0) {
                if (outInfo != null) {
                    outInfo.removedPackage = packageName;
                    outInfo.removedAppId = appId;
                    outInfo.removedUsers = new int[]{removeUser};
                }
                this.mInstaller.clearUserData(packageName, removeUser);
                removeKeystoreDataIfNeeded(removeUser, appId);
                schedulePackageCleaning(packageName, removeUser, false);
                return DEFAULT_VERIFY_ENABLE;
            }
            if (0 != 0) {
                removePackageDataLI(ps, null, null, outInfo, flags, writeSettings);
                return DEFAULT_VERIFY_ENABLE;
            }
            if (isSystemApp(ps)) {
                boolean ret = deleteSystemPackageLI(ps, allUserHandles, perUserInstalled, flags, outInfo, writeSettings);
                return ret;
            }
            killApplication(packageName, ps.appId, "uninstall pkg");
            boolean ret2 = deleteInstalledPackageLI(ps, deleteCodeAndResources, flags, allUserHandles, perUserInstalled, outInfo, writeSettings);
            return ret2;
        }
    }

    private final class ClearStorageConnection implements ServiceConnection {
        IMediaContainerService mContainerService;

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
        if (Environment.isExternalStorageEmulated()) {
            mounted = DEFAULT_VERIFY_ENABLE;
        } else {
            String status = Environment.getExternalStorageState();
            mounted = (status.equals("mounted") || status.equals("mounted_ro")) ? DEFAULT_VERIFY_ENABLE : false;
        }
        if (mounted) {
            Intent containerIntent = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
            if (userId == -1) {
                users = sUserManager.getUserIds();
            } else {
                users = new int[]{userId};
            }
            ClearStorageConnection conn = new ClearStorageConnection();
            if (this.mContext.bindServiceAsUser(containerIntent, conn, 1, UserHandle.OWNER)) {
                int[] arr$ = users;
                try {
                    for (int curUser : arr$) {
                        long timeout = SystemClock.uptimeMillis() + 5000;
                        synchronized (conn) {
                            long now = SystemClock.uptimeMillis();
                            while (conn.mContainerService == null && now < timeout) {
                                try {
                                    conn.wait(timeout - now);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        if (conn.mContainerService != null) {
                            Environment.UserEnvironment userEnv = new Environment.UserEnvironment(curUser);
                            clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppCacheDirs(packageName));
                            if (allData) {
                                clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppDataDirs(packageName));
                                clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppMediaDirs(packageName));
                            }
                        } else {
                            return;
                        }
                    }
                } finally {
                    this.mContext.unbindService(conn);
                }
            }
        }
    }

    public void clearApplicationUserData(final String packageName, final IPackageDataObserver observer, final int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_USER_DATA", null);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, DEFAULT_VERIFY_ENABLE, false, "clear application data");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean succeeded;
                DeviceStorageMonitorInternal dsm;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                synchronized (PackageManagerService.this.mInstallLock) {
                    succeeded = PackageManagerService.this.clearApplicationUserDataLI(packageName, userId);
                }
                PackageManagerService.this.clearExternalStorageDataSync(packageName, userId, PackageManagerService.DEFAULT_VERIFY_ENABLE);
                if (succeeded && (dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class)) != null) {
                    dsm.checkMemory();
                }
                if (observer != null) {
                    try {
                        observer.onRemoveCompleted(packageName, succeeded);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            }
        });
    }

    private boolean clearApplicationUserDataLI(String packageName, int userId) {
        PackageParser.Package pkg;
        PackageSetting ps;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(packageName);
            if (pkg == null && (ps = this.mSettings.mPackages.get(packageName)) != null) {
                pkg = ps.pkg;
            }
        }
        if (pkg == null) {
            Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
        }
        int retCode = this.mInstaller.clearUserData(packageName, userId);
        if (retCode < 0) {
            Slog.w(TAG, "Couldn't remove cache files for package: " + packageName);
            return false;
        }
        if (pkg == null) {
            return false;
        }
        if (pkg != null && pkg.applicationInfo != null) {
            int appId = pkg.applicationInfo.uid;
            removeKeystoreDataIfNeeded(userId, appId);
        }
        if (pkg != null && pkg.applicationInfo.primaryCpuAbi != null && !VMRuntime.is64BitAbi(pkg.applicationInfo.primaryCpuAbi)) {
            String nativeLibPath = pkg.applicationInfo.nativeLibraryDir;
            if (this.mInstaller.linkNativeLibraryDirectory(pkg.packageName, nativeLibPath, userId) < 0) {
                Slog.w(TAG, "Failed linking native library dir");
                return false;
            }
        }
        return DEFAULT_VERIFY_ENABLE;
    }

    private static void removeKeystoreDataIfNeeded(int userId, int appId) {
        if (appId >= 0) {
            KeyStore keyStore = KeyStore.getInstance();
            if (keyStore != null) {
                if (userId == -1) {
                    int[] arr$ = sUserManager.getUserIds();
                    for (int individual : arr$) {
                        keyStore.clearUid(UserHandle.getUid(individual, appId));
                    }
                    return;
                }
                keyStore.clearUid(UserHandle.getUid(userId, appId));
                return;
            }
            Slog.w(TAG, "Could not contact keystore to clear entries for app id " + appId);
        }
    }

    public void deleteApplicationCacheFiles(final String packageName, final IPackageDataObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_CACHE_FILES", null);
        final int userId = UserHandle.getCallingUserId();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean succeded;
                PackageManagerService.this.mHandler.removeCallbacks(this);
                synchronized (PackageManagerService.this.mInstallLock) {
                    succeded = PackageManagerService.this.deleteApplicationCacheFilesLI(packageName, userId);
                }
                PackageManagerService.this.clearExternalStorageDataSync(packageName, userId, false);
                if (observer != null) {
                    try {
                        observer.onRemoveCompleted(packageName, succeded);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            }
        });
    }

    private boolean deleteApplicationCacheFilesLI(String packageName, int userId) {
        PackageParser.Package p;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            p = this.mPackages.get(packageName);
        }
        if (p == null) {
            Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
            return false;
        }
        ApplicationInfo applicationInfo = p.applicationInfo;
        if (applicationInfo == null) {
            Slog.w(TAG, "Package " + packageName + " has no applicationInfo.");
            return false;
        }
        int retCode = this.mInstaller.deleteCacheFiles(packageName, userId);
        if (retCode < 0) {
            Slog.w(TAG, "Couldn't remove cache files for package: " + packageName + " u" + userId);
            return false;
        }
        return DEFAULT_VERIFY_ENABLE;
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

    private boolean getPackageSizeInfoLI(String packageName, int userHandle, PackageStats pStats) {
        String secureContainerId;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to get size of null packageName.");
            return false;
        }
        boolean dataOnly = false;
        String libDirRoot = null;
        String asecPath = null;
        synchronized (this.mPackages) {
            PackageParser.Package p = this.mPackages.get(packageName);
            PackageSetting ps = this.mSettings.mPackages.get(packageName);
            if (p == null) {
                dataOnly = DEFAULT_VERIFY_ENABLE;
                if (ps == null || ps.pkg == null) {
                    Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                    return false;
                }
                p = ps.pkg;
            }
            if (ps != null) {
                libDirRoot = ps.legacyNativeLibraryPathString;
            }
            if (p != null && ((isExternal(p) || isForwardLocked(p)) && (secureContainerId = cidFromCodePath(p.applicationInfo.getBaseCodePath())) != null)) {
                asecPath = PackageHelper.getSdFilesystem(secureContainerId);
            }
            String publicSrcDir = null;
            if (!dataOnly) {
                ApplicationInfo applicationInfo = p.applicationInfo;
                if (applicationInfo == null) {
                    Slog.w(TAG, "Package " + packageName + " has no applicationInfo.");
                    return false;
                }
                if (isForwardLocked(p)) {
                    publicSrcDir = applicationInfo.getBaseResourcePath();
                }
            }
            String[] dexCodeInstructionSets = getDexCodeInstructionSets(getAppDexInstructionSets(ps));
            int res = this.mInstaller.getSizeInfo(packageName, userHandle, p.baseCodePath, libDirRoot, publicSrcDir, asecPath, dexCodeInstructionSets, pStats);
            if (res < 0) {
                return false;
            }
            if (!isExternal(p)) {
                pStats.codeSize += pStats.externalCodeSize;
                pStats.externalCodeSize = 0L;
            }
            return DEFAULT_VERIFY_ENABLE;
        }
    }

    public void addPackageToPreferred(String packageName) {
        Slog.w(TAG, "addPackageToPreferred: this is now a no-op");
    }

    public void removePackageFromPreferred(String packageName) {
        Slog.w(TAG, "removePackageFromPreferred: this is now a no-op");
    }

    public List<PackageInfo> getPreferredPackages(int flags) {
        return new ArrayList();
    }

    private int getUidTargetSdkVersionLockedLPr(int uid) {
        int v;
        Object obj = this.mSettings.getUserIdLPr(uid);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            int vers = ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE;
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
        }
        return ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE;
    }

    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        addPreferredActivityInternal(filter, match, set, activity, DEFAULT_VERIFY_ENABLE, userId, "Adding preferred");
    }

    private void addPreferredActivityInternal(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always, int userId, String opname) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, DEFAULT_VERIFY_ENABLE, false, "add preferred activity");
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
        enforceCrossUserPermission(callingUid, userId, DEFAULT_VERIFY_ENABLE, false, "replace preferred activity");
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
                    if (cur.mPref.mAlways && cur.mPref.mComponent.equals(activity) && cur.mPref.mMatch == (268369920 & match) && cur.mPref.sameSet(set)) {
                        return;
                    }
                }
                if (existing != null) {
                    for (int i = 0; i < existing.size(); i++) {
                        PreferredActivity pa = existing.get(i);
                        pir.removeFilter(pa);
                    }
                }
            }
            addPreferredActivityInternal(filter, match, set, activity, DEFAULT_VERIFY_ENABLE, userId, "Replacing preferred");
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
                    changed = DEFAULT_VERIFY_ENABLE;
                }
            }
        }
        return changed;
    }

    public void resetPreferredActivities(int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        synchronized (this.mPackages) {
            int user = UserHandle.getCallingUserId();
            clearPackagePreferredActivitiesLPw(null, user);
            this.mSettings.readDefaultPreferredAppsLPw(this, user);
            scheduleWritePackageRestrictionsLocked(user);
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
            Slog.i(TAG, "Adding persistent preferred activity " + activity + " for user " + userId + " :");
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
                            changed = DEFAULT_VERIFY_ENABLE;
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

    public void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage, int ownerUserId, int sourceUserId, int targetUserId, int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, ownerUserId, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a crossProfile intent filter with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            CrossProfileIntentFilter newFilter = new CrossProfileIntentFilter(intentFilter, ownerPackage, UserHandle.getUserId(callingUid), targetUserId, flags);
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArrayList<CrossProfileIntentFilter> existing = resolver.findFilters(intentFilter);
            if (existing != null) {
                int size = existing.size();
                for (int i = 0; i < size; i++) {
                    if (newFilter.equalsIgnoreFilter(existing.get(i))) {
                        break;
                    }
                }
                resolver.addFilter(newFilter);
                scheduleWritePackageRestrictionsLocked(sourceUserId);
            } else {
                resolver.addFilter(newFilter);
                scheduleWritePackageRestrictionsLocked(sourceUserId);
            }
        }
    }

    public void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage, int ownerUserId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, ownerUserId, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        int callingUserId = UserHandle.getUserId(callingUid);
        synchronized (this.mPackages) {
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArraySet<CrossProfileIntentFilter> set = new ArraySet<>(resolver.filterSet());
            for (CrossProfileIntentFilter filter : set) {
                if (filter.getOwnerPackage().equals(ownerPackage) && filter.getOwnerUserId() == callingUserId) {
                    resolver.removeFilter(filter);
                }
            }
            scheduleWritePackageRestrictionsLocked(sourceUserId);
        }
    }

    private void enforceOwnerRights(String pkg, int userId, int callingUid) {
        if (UserHandle.getAppId(callingUid) != 1000) {
            int callingUserId = UserHandle.getUserId(callingUid);
            if (callingUserId != userId) {
                throw new SecurityException("calling uid " + callingUid + " pretends to own " + pkg + " on user " + userId + " but belongs to user " + callingUserId);
            }
            PackageInfo pi = getPackageInfo(pkg, 0, callingUserId);
            if (pi == null) {
                throw new IllegalArgumentException("Unknown package " + pkg + " on user " + callingUserId);
            }
            if (!UserHandle.isSameApp(pi.applicationInfo.uid, callingUid)) {
                throw new SecurityException("Calling uid " + callingUid + " does not own package " + pkg);
            }
        }
    }

    public ComponentName getHomeActivities(List<ResolveInfo> allHomeCandidates) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        int callingUserId = UserHandle.getCallingUserId();
        List<ResolveInfo> list = queryIntentActivities(intent, null, 128, callingUserId);
        ResolveInfo preferred = findPreferredActivity(intent, null, 0, list, 0, DEFAULT_VERIFY_ENABLE, false, false, callingUserId);
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
        if (newState != 0 && newState != 1 && newState != 2 && newState != 3 && newState != 4) {
            throw new IllegalArgumentException("Invalid new component state: " + newState);
        }
        int uid = Binder.getCallingUid();
        int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
        enforceCrossUserPermission(uid, userId, false, DEFAULT_VERIFY_ENABLE, "set enabled");
        boolean allowedByPermission = permission == 0 ? DEFAULT_VERIFY_ENABLE : false;
        boolean sendNow = false;
        boolean isApp = className == null ? DEFAULT_VERIFY_ENABLE : false;
        String componentName = isApp ? packageName : className;
        synchronized (this.mPackages) {
            PackageSetting pkgSetting = this.mSettings.mPackages.get(packageName);
            if (pkgSetting == null) {
                if (className == null) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                throw new IllegalArgumentException("Unknown component: " + packageName + "/" + className);
            }
            if (!allowedByPermission && !UserHandle.isSameApp(uid, pkgSetting.appId)) {
                throw new SecurityException("Permission Denial: attempt to change component state from pid=" + Binder.getCallingPid() + ", uid=" + uid + ", package uid=" + pkgSetting.appId);
            }
            if (className == null) {
                if (pkgSetting.getEnabled(userId) != newState) {
                    if (newState == 0 || newState == 1) {
                        callingPackage = null;
                    }
                    pkgSetting.setEnabled(newState, userId, callingPackage);
                } else {
                    return;
                }
            } else {
                PackageParser.Package pkg = pkgSetting.pkg;
                if (pkg == null || !pkg.hasComponentClassName(className)) {
                    if (pkg.applicationInfo.targetSdkVersion >= 16) {
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
            }
            this.mSettings.writePackageRestrictionsLPr(userId);
            ArrayList<String> components = this.mPendingBroadcasts.get(userId, packageName);
            boolean newPackage = components == null ? DEFAULT_VERIFY_ENABLE : false;
            if (newPackage) {
                components = new ArrayList<>();
            }
            if (!components.contains(componentName)) {
                components.add(componentName);
            }
            if ((flags & 1) == 0) {
                sendNow = DEFAULT_VERIFY_ENABLE;
                this.mPendingBroadcasts.remove(userId, packageName);
            } else {
                if (newPackage) {
                    this.mPendingBroadcasts.put(userId, packageName, components);
                }
                if (!this.mHandler.hasMessages(1)) {
                    this.mHandler.sendEmptyMessageDelayed(1, DEFAULT_VERIFICATION_TIMEOUT);
                }
            }
            long callingId = Binder.clearCallingIdentity();
            if (sendNow) {
                try {
                    int packageUid = UserHandle.getUid(userId, pkgSetting.appId);
                    sendPackageChangedBroadcast(packageName, (flags & 1) != 0 ? DEFAULT_VERIFY_ENABLE : false, components, packageUid);
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
    }

    private void sendPackageChangedBroadcast(String packageName, boolean killFlag, ArrayList<String> componentNames, int packageUid) {
        Bundle extras = new Bundle(4);
        extras.putString("android.intent.extra.changed_component_name", componentNames.get(0));
        String[] nameList = new String[componentNames.size()];
        componentNames.toArray(nameList);
        extras.putStringArray("android.intent.extra.changed_component_name_list", nameList);
        extras.putBoolean("android.intent.extra.DONT_KILL_APP", killFlag);
        extras.putInt("android.intent.extra.UID", packageUid);
        sendPackageBroadcast("android.intent.action.PACKAGE_CHANGED", packageName, extras, null, null, new int[]{UserHandle.getUserId(packageUid)});
    }

    public void setPackageStoppedState(String packageName, boolean stopped, int userId) {
        if (sUserManager.exists(userId)) {
            int uid = Binder.getCallingUid();
            int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
            boolean allowedByPermission = permission == 0;
            enforceCrossUserPermission(uid, userId, DEFAULT_VERIFY_ENABLE, DEFAULT_VERIFY_ENABLE, "stop package");
            synchronized (this.mPackages) {
                if (this.mSettings.setPackageStoppedStateLPw(packageName, stopped, allowedByPermission, uid, userId)) {
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
        if (!this.mSystemReady) {
            this.mSafeMode = DEFAULT_VERIFY_ENABLE;
        }
    }

    public void systemReady() {
        boolean compatibilityModeEnabled = DEFAULT_VERIFY_ENABLE;
        this.mSystemReady = DEFAULT_VERIFY_ENABLE;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "compatibility_mode", 1) != 1) {
            compatibilityModeEnabled = false;
        }
        PackageParser.setCompatibilityModeEnabled(compatibilityModeEnabled);
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
        }
        sUserManager.systemReady();
        if (this.mPostSystemReadyMessages != null) {
            for (Message msg : this.mPostSystemReadyMessages) {
                msg.sendToTarget();
            }
            this.mPostSystemReadyMessages = null;
        }
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
        public static final int DUMP_FEATURES = 2;
        public static final int DUMP_INSTALLS = 8192;
        public static final int DUMP_KEYSETS = 2048;
        public static final int DUMP_LIBS = 1;
        public static final int DUMP_MESSAGES = 64;
        public static final int DUMP_PACKAGES = 16;
        public static final int DUMP_PERMISSIONS = 8;
        public static final int DUMP_PREFERRED = 512;
        public static final int DUMP_PREFERRED_XML = 1024;
        public static final int DUMP_PROVIDERS = 128;
        public static final int DUMP_RESOLVERS = 4;
        public static final int DUMP_SHARED_USERS = 32;
        public static final int DUMP_VERIFIERS = 256;
        public static final int DUMP_VERSION = 4096;
        public static final int OPTION_SHOW_FILTERS = 1;
        private int mOptions;
        private SharedUserSetting mSharedUser;
        private boolean mTitlePrinted;
        private int mTypes;

        DumpState() {
        }

        public boolean isDumping(int type) {
            if ((this.mTypes != 0 || type == 1024) && (this.mTypes & type) == 0) {
                return false;
            }
            return PackageManagerService.DEFAULT_VERIFY_ENABLE;
        }

        public void setDump(int type) {
            this.mTypes |= type;
        }

        public boolean isOptionEnabled(int option) {
            if ((this.mOptions & option) != 0) {
                return PackageManagerService.DEFAULT_VERIFY_ENABLE;
            }
            return false;
        }

        public void setOptionEnabled(int option) {
            this.mOptions |= option;
        }

        public boolean onTitlePrinted() {
            boolean printed = this.mTitlePrinted;
            this.mTitlePrinted = PackageManagerService.DEFAULT_VERIFY_ENABLE;
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
                    pw.println("    f[ibraries]: list device features");
                    pw.println("    k[eysets]: print known keysets");
                    pw.println("    r[esolvers]: dump intent resolvers");
                    pw.println("    perm[issions]: dump permissions");
                    pw.println("    pref[erred]: print preferred package settings");
                    pw.println("    preferred-xml [--full]: print preferred package settings as xml");
                    pw.println("    prov[iders]: dump content providers");
                    pw.println("    p[ackages]: dump installed packages");
                    pw.println("    s[hared-users]: dump shared user IDs");
                    pw.println("    m[essages]: print collected runtime messages");
                    pw.println("    v[erifiers]: print package verifier info");
                    pw.println("    version: print database version info");
                    pw.println("    write: write current settings now");
                    pw.println("    <package.name>: info about given package");
                    pw.println("    installs: details about install sessions");
                    return;
                }
                if ("--checkin".equals(opt)) {
                    checkin = DEFAULT_VERIFY_ENABLE;
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
            if ("android".equals(cmd) || cmd.contains(".")) {
                packageName = cmd;
                dumpState.setOptionEnabled(1);
            } else if ("l".equals(cmd) || "libraries".equals(cmd)) {
                dumpState.setDump(1);
            } else if ("f".equals(cmd) || "features".equals(cmd)) {
                dumpState.setDump(2);
            } else if ("r".equals(cmd) || "resolvers".equals(cmd)) {
                dumpState.setDump(4);
            } else if ("perm".equals(cmd) || "permissions".equals(cmd)) {
                dumpState.setDump(8);
            } else if ("pref".equals(cmd) || "preferred".equals(cmd)) {
                dumpState.setDump(512);
            } else if ("preferred-xml".equals(cmd)) {
                dumpState.setDump(1024);
                if (opti2 < args.length && "--full".equals(args[opti2])) {
                    fullPreferred = DEFAULT_VERIFY_ENABLE;
                    int i = opti2 + 1;
                }
            } else if ("p".equals(cmd) || "packages".equals(cmd)) {
                dumpState.setDump(16);
            } else if ("s".equals(cmd) || "shared-users".equals(cmd)) {
                dumpState.setDump(32);
            } else if ("prov".equals(cmd) || "providers".equals(cmd)) {
                dumpState.setDump(128);
            } else if ("m".equals(cmd) || "messages".equals(cmd)) {
                dumpState.setDump(64);
            } else if ("v".equals(cmd) || "verifiers".equals(cmd)) {
                dumpState.setDump(256);
            } else if ("version".equals(cmd)) {
                dumpState.setDump(DumpState.DUMP_VERSION);
            } else if ("k".equals(cmd) || "keysets".equals(cmd)) {
                dumpState.setDump(2048);
            } else if ("installs".equals(cmd)) {
                dumpState.setDump(DumpState.DUMP_INSTALLS);
            } else if ("write".equals(cmd)) {
                synchronized (this.mPackages) {
                    this.mSettings.writeLPr();
                    pw.println("Settings written.");
                }
                return;
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
                pw.print("  SDK Version:");
                pw.print(" internal=");
                pw.print(this.mSettings.mInternalSdkPlatform);
                pw.print(" external=");
                pw.println(this.mSettings.mExternalSdkPlatform);
                pw.print("  DB Version:");
                pw.print(" internal=");
                pw.print(this.mSettings.mInternalDatabaseVersion);
                pw.print(" external=");
                pw.println(this.mSettings.mExternalDatabaseVersion);
            }
            if (dumpState.isDumping(256) && packageName == null) {
                if (!checkin) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Verifiers:");
                    pw.print("  Required: ");
                    pw.print(this.mRequiredVerifierPackage);
                    pw.print(" (uid=");
                    pw.print(getPackageUid(this.mRequiredVerifierPackage, 0));
                    pw.println(")");
                } else if (this.mRequiredVerifierPackage != null) {
                    pw.print("vrfy,");
                    pw.print(this.mRequiredVerifierPackage);
                    pw.print(",");
                    pw.println(getPackageUid(this.mRequiredVerifierPackage, 0));
                }
            }
            if (dumpState.isDumping(1) && packageName == null) {
                boolean printedHeader = false;
                for (String name : this.mSharedLibraries.keySet()) {
                    SharedLibraryEntry ent = this.mSharedLibraries.get(name);
                    if (!checkin) {
                        if (!printedHeader) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Libraries:");
                            printedHeader = DEFAULT_VERIFY_ENABLE;
                        }
                        pw.print("  ");
                    } else {
                        pw.print("lib,");
                    }
                    pw.print(name);
                    if (!checkin) {
                        pw.print(" -> ");
                    }
                    if (ent.path != null) {
                        if (!checkin) {
                            pw.print("(jar) ");
                            pw.print(ent.path);
                        } else {
                            pw.print(",jar,");
                            pw.print(ent.path);
                        }
                    } else if (!checkin) {
                        pw.print("(apk) ");
                        pw.print(ent.apk);
                    } else {
                        pw.print(",apk,");
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
                for (String name2 : this.mAvailableFeatures.keySet()) {
                    if (!checkin) {
                        pw.print("  ");
                    } else {
                        pw.print("feat,");
                    }
                    pw.println(name2);
                }
            }
            if (!checkin && dumpState.isDumping(4)) {
                if (this.mActivities.dump(pw, dumpState.getTitlePrinted() ? "\nActivity Resolver Table:" : "Activity Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), DEFAULT_VERIFY_ENABLE)) {
                    dumpState.setTitlePrinted(DEFAULT_VERIFY_ENABLE);
                }
                if (this.mReceivers.dump(pw, dumpState.getTitlePrinted() ? "\nReceiver Resolver Table:" : "Receiver Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), DEFAULT_VERIFY_ENABLE)) {
                    dumpState.setTitlePrinted(DEFAULT_VERIFY_ENABLE);
                }
                if (this.mServices.dump(pw, dumpState.getTitlePrinted() ? "\nService Resolver Table:" : "Service Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), DEFAULT_VERIFY_ENABLE)) {
                    dumpState.setTitlePrinted(DEFAULT_VERIFY_ENABLE);
                }
                if (this.mProviders.dump(pw, dumpState.getTitlePrinted() ? "\nProvider Resolver Table:" : "Provider Resolver Table:", "  ", packageName, dumpState.isOptionEnabled(1), DEFAULT_VERIFY_ENABLE)) {
                    dumpState.setTitlePrinted(DEFAULT_VERIFY_ENABLE);
                }
            }
            if (!checkin && dumpState.isDumping(512)) {
                for (int i2 = 0; i2 < this.mSettings.mPreferredActivities.size(); i2++) {
                    PreferredIntentResolver pir = this.mSettings.mPreferredActivities.valueAt(i2);
                    int user = this.mSettings.mPreferredActivities.keyAt(i2);
                    if (pir.dump(pw, dumpState.getTitlePrinted() ? "\nPreferred Activities User " + user + ":" : "Preferred Activities User " + user + ":", "  ", packageName, DEFAULT_VERIFY_ENABLE, false)) {
                        dumpState.setTitlePrinted(DEFAULT_VERIFY_ENABLE);
                    }
                }
            }
            if (!checkin && dumpState.isDumping(1024)) {
                pw.flush();
                FileOutputStream fout = new FileOutputStream(fd);
                BufferedOutputStream str = new BufferedOutputStream(fout);
                XmlSerializer serializer = new FastXmlSerializer();
                try {
                    try {
                        serializer.setOutput(str, "utf-8");
                        serializer.startDocument(null, Boolean.valueOf(DEFAULT_VERIFY_ENABLE));
                        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", DEFAULT_VERIFY_ENABLE);
                        this.mSettings.writePreferredActivitiesLPr(serializer, 0, fullPreferred);
                        serializer.endDocument();
                        serializer.flush();
                    } catch (IllegalArgumentException e) {
                        pw.println("Failed writing: " + e);
                    }
                } catch (IOException e2) {
                    pw.println("Failed writing: " + e2);
                } catch (IllegalStateException e3) {
                    pw.println("Failed writing: " + e3);
                }
            }
            if (!checkin && dumpState.isDumping(8)) {
                this.mSettings.dumpPermissionsLPr(pw, packageName, dumpState);
                if (packageName == null) {
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
            if (!checkin && dumpState.isDumping(128)) {
                boolean printedSomething = false;
                for (PackageParser.Provider p : this.mProviders.mProviders.values()) {
                    if (packageName == null || packageName.equals(p.info.packageName)) {
                        if (!printedSomething) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Registered ContentProviders:");
                            printedSomething = DEFAULT_VERIFY_ENABLE;
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
                            printedSomething2 = DEFAULT_VERIFY_ENABLE;
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
            if (!checkin && dumpState.isDumping(2048)) {
                this.mSettings.mKeySetManagerService.dumpLPr(pw, packageName, dumpState);
            }
            if (dumpState.isDumping(16)) {
                this.mSettings.dumpPackagesLPr(pw, packageName, dumpState, checkin);
            }
            if (dumpState.isDumping(32)) {
                this.mSettings.dumpSharedUsersLPr(pw, packageName, dumpState, checkin);
            }
            if (!checkin && dumpState.isDumping(DumpState.DUMP_INSTALLS) && packageName == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                this.mInstallerService.dump(new IndentingPrintWriter(pw, "  ", 120));
            }
            if (!checkin && dumpState.isDumping(64) && packageName == null) {
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
                        } catch (IOException e4) {
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
                } catch (IOException e5) {
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (checkin && dumpState.isDumping(64)) {
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
                        } catch (IOException e6) {
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
                } catch (IOException e7) {
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }
    }

    static String getEncryptKey() {
        try {
            String sdEncKey = SystemKeyStore.getInstance().retrieveKeyHexString(SD_ENCRYPTION_KEYSTORE_NAME);
            if (sdEncKey == null) {
                String sdEncKey2 = SystemKeyStore.getInstance().generateNewKeyHexString(128, SD_ENCRYPTION_ALGORITHM, SD_ENCRYPTION_KEYSTORE_NAME);
                if (sdEncKey2 == null) {
                    Slog.e(TAG, "Failed to create encryption keys");
                    return null;
                }
                return sdEncKey2;
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
            if (mediaStatus == this.mMediaMounted) {
                Message msg = this.mHandler.obtainMessage(12, reportStatus ? 1 : 0, -1);
                this.mHandler.sendMessage(msg);
            } else {
                this.mMediaMounted = mediaStatus;
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        PackageManagerService.this.updateExternalMediaStatusInner(mediaStatus, reportStatus, PackageManagerService.DEFAULT_VERIFY_ENABLE);
                    }
                });
            }
        }
    }

    public void scanAvailableAsecs() throws Throwable {
        updateExternalMediaStatusInner(DEFAULT_VERIFY_ENABLE, false, false);
        if (this.mShouldRestoreconData) {
            SELinuxMMAC.setRestoreconDone();
            this.mShouldRestoreconData = false;
        }
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
                        String pkgName = getAsecPackageName(cid);
                        if (pkgName == null) {
                            Slog.i(TAG, "Found stale container " + cid + " with no package name");
                        } else {
                            PackageSetting ps = this.mSettings.mPackages.get(pkgName);
                            if (ps == null) {
                                Slog.i(TAG, "Found stale container " + cid + " with no matching settings");
                            } else if (!externalStorage || isMounted || isExternal(ps)) {
                                AsecInstallArgs args = new AsecInstallArgs(cid, getAppDexInstructionSets(ps), isForwardLocked(ps));
                                if (ps.codePathString != null && ps.codePathString.startsWith(args.getCodePath())) {
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
            loadMediaPackages(processCids, uidArr);
            startCleaningPackages();
            this.mInstallerService.onSecureContainersAvailable();
            return;
        }
        unloadMediaPackages(processCids, uidArr, reportStatus);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<String> pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        int size = pkgList.size();
        if (size > 0) {
            Bundle extras = new Bundle();
            extras.putStringArray("android.intent.extra.changed_package_list", (String[]) pkgList.toArray(new String[size]));
            if (uidArr != null) {
                extras.putIntArray("android.intent.extra.changed_uid_list", uidArr);
            }
            if (replacing) {
                extras.putBoolean("android.intent.extra.REPLACING", replacing);
            }
            String action = mediaStatus ? "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE" : "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
            sendPackageBroadcast(action, null, extras, null, finishedReceiver, null);
        }
    }

    private void loadMediaPackages(ArrayMap<AsecInstallArgs, String> processCids, int[] uidArr) {
        ArrayList<String> pkgList = new ArrayList<>();
        Set<AsecInstallArgs> keys = processCids.keySet();
        for (AsecInstallArgs args : keys) {
            String codePath = processCids.get(args);
            int retCode = -18;
            try {
                if (args.doPreInstall(1) != 1) {
                    Slog.e(TAG, "Failed to mount cid : " + args.cid + " when installing from sdcard");
                    if (-18 != 1) {
                        Log.w(TAG, "Container " + args.cid + " is stale, retCode=-18");
                    }
                } else if (codePath == null || !codePath.startsWith(args.getCodePath())) {
                    Slog.e(TAG, "Container " + args.cid + " cachepath " + args.getCodePath() + " does not match one in settings " + codePath);
                    if (-18 != 1) {
                        Log.w(TAG, "Container " + args.cid + " is stale, retCode=-18");
                    }
                } else {
                    int parseFlags = this.mDefParseFlags;
                    if (args.isExternal()) {
                        parseFlags |= 32;
                    }
                    if (args.isFwdLocked()) {
                        parseFlags |= 16;
                    }
                    synchronized (this.mInstallLock) {
                        PackageParser.Package pkg = null;
                        try {
                            pkg = scanPackageLI(new File(codePath), parseFlags, 0, 0L, (UserHandle) null);
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
            boolean regrantPermissions = this.mSettings.mExternalSdkPlatform != this.mSdkVersion ? DEFAULT_VERIFY_ENABLE : false;
            if (regrantPermissions) {
                Slog.i(TAG, "Platform changed from " + this.mSettings.mExternalSdkPlatform + " to " + this.mSdkVersion + "; regranting permissions for external storage");
            }
            this.mSettings.mExternalSdkPlatform = this.mSdkVersion;
            updatePermissionsLPw(null, null, (regrantPermissions ? 6 : 0) | 1);
            this.mSettings.updateExternalDatabaseVersion();
            this.mSettings.writeLPr();
        }
        if (pkgList.size() > 0) {
            sendResourcesChangedBroadcast(DEFAULT_VERIFY_ENABLE, false, pkgList, uidArr, null);
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
        ArrayList<String> pkgList = new ArrayList<>();
        ArrayList<AsecInstallArgs> failedList = new ArrayList<>();
        final Set<AsecInstallArgs> keys = processCids.keySet();
        for (AsecInstallArgs args : keys) {
            String pkgName = args.getPackageName();
            PackageRemovedInfo outInfo = new PackageRemovedInfo();
            synchronized (this.mInstallLock) {
                boolean res = deletePackageLI(pkgName, null, false, null, null, 1, outInfo, false);
                if (res) {
                    pkgList.add(pkgName);
                } else {
                    Slog.e(TAG, "Failed to delete pkg from sdcard : " + pkgName);
                    failedList.add(args);
                }
            }
        }
        synchronized (this.mPackages) {
            this.mSettings.writeLPr();
        }
        if (pkgList.size() > 0) {
            sendResourcesChangedBroadcast(false, false, pkgList, uidArr, new IIntentReceiver.Stub() {
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

    public void movePackage(final String packageName, final IPackageMoveObserver observer, int flags) throws Throwable {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        UserHandle user = new UserHandle(UserHandle.getCallingUserId());
        int returnCode = 1;
        int newInstallFlags = 0;
        File codeFile = null;
        String installerPackageName = null;
        String packageAbiOverride = null;
        synchronized (this.mPackages) {
            try {
                PackageParser.Package pkg = this.mPackages.get(packageName);
                PackageSetting ps = this.mSettings.mPackages.get(packageName);
                if (pkg == null || ps == null) {
                    returnCode = -2;
                } else {
                    if (pkg.applicationInfo != null && isSystemApp(pkg)) {
                        Slog.w(TAG, "Cannot move system application");
                        returnCode = -3;
                    } else if (pkg.mOperationPending) {
                        Slog.w(TAG, "Attempt to move package which has pending operations");
                        returnCode = -7;
                    } else {
                        if ((flags & 2) != 0 && (flags & 1) != 0) {
                            Slog.w(TAG, "Ambigous flags specified for move location.");
                            returnCode = -5;
                        } else {
                            newInstallFlags = (flags & 2) != 0 ? 8 : 16;
                            int currInstallFlags = isExternal(pkg) ? 8 : 16;
                            if (newInstallFlags == currInstallFlags) {
                                Slog.w(TAG, "No move required. Trying to move to same location");
                                returnCode = -5;
                            } else if (isForwardLocked(pkg)) {
                                int i = currInstallFlags | 1;
                                newInstallFlags |= 1;
                            }
                        }
                        if (returnCode == 1) {
                            pkg.mOperationPending = DEFAULT_VERIFY_ENABLE;
                        }
                    }
                    File codeFile2 = new File(pkg.codePath);
                    try {
                        installerPackageName = ps.installerPackageName;
                        packageAbiOverride = ps.cpuAbiOverrideString;
                        codeFile = codeFile2;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (returnCode != 1) {
                    try {
                        observer.packageMoved(packageName, returnCode);
                    } catch (RemoteException e) {
                    }
                } else {
                    Message msg = this.mHandler.obtainMessage(5);
                    OriginInfo origin = OriginInfo.fromExistingFile(codeFile);
                    msg.obj = new InstallParams(origin, new IPackageInstallObserver2.Stub() {
                        public void onUserActionRequired(Intent intent) throws RemoteException {
                            throw new IllegalStateException();
                        }

                        public void onPackageInstalled(String basePackageName, int returnCode2, String msg2, Bundle extras) throws RemoteException {
                            Slog.d(PackageManagerService.TAG, "Install result for move: " + PackageManager.installStatusToString(returnCode2, msg2));
                            synchronized (PackageManagerService.this.mPackages) {
                                PackageParser.Package pkg2 = PackageManagerService.this.mPackages.get(packageName);
                                if (pkg2 != null) {
                                    pkg2.mOperationPending = false;
                                }
                            }
                            int status = PackageManager.installStatusToPublicStatus(returnCode2);
                            switch (status) {
                                case 0:
                                    observer.packageMoved(packageName, 1);
                                    return;
                                case 6:
                                    observer.packageMoved(packageName, -1);
                                    return;
                                default:
                                    observer.packageMoved(packageName, -6);
                                    return;
                            }
                        }
                    }, newInstallFlags | 2, installerPackageName, null, user, packageAbiOverride);
                    this.mHandler.sendMessage(msg);
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public boolean setInstallLocation(int loc) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
        if (getInstallLocation() == loc) {
            return DEFAULT_VERIFY_ENABLE;
        }
        if (loc == 0 || loc == 1 || loc == 2) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "default_install_location", loc);
            return DEFAULT_VERIFY_ENABLE;
        }
        return false;
    }

    public int getInstallLocation() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "default_install_location", 0);
    }

    void cleanUpUserLILPw(UserManagerService userManager, int userHandle) {
        this.mDirtyUsers.remove(Integer.valueOf(userHandle));
        this.mSettings.removeUserLPw(userHandle);
        this.mPendingBroadcasts.remove(userHandle);
        if (this.mInstaller != null) {
            this.mInstaller.removeUserDataDirs(userHandle);
        }
        this.mUserNeedsBadging.delete(userHandle);
        removeUnusedPackagesLILPw(userManager, userHandle);
    }

    private void removeUnusedPackagesLILPw(UserManagerService userManager, final int userHandle) {
        int[] users = userManager.getUserIdsLPr();
        for (PackageSetting ps : this.mSettings.mPackages.values()) {
            if (ps.pkg != null) {
                final String packageName = ps.pkg.packageName;
                if ((ps.pkgFlags & 1) == 0) {
                    boolean keep = false;
                    int i = 0;
                    while (true) {
                        if (i >= users.length) {
                            break;
                        }
                        if (users[i] == userHandle || !ps.getInstalled(users[i])) {
                            i++;
                        } else {
                            keep = DEFAULT_VERIFY_ENABLE;
                            break;
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

    void createNewUserLILPw(int userHandle, File path) {
        if (this.mInstaller != null) {
            this.mInstaller.createUserConfig(userHandle);
            this.mSettings.createNewUserLILPw(this, this.mInstaller, userHandle, path);
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

    public void setPermissionEnforced(String permission, boolean enforced) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_REVOKE_PERMISSIONS", null);
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(permission)) {
            synchronized (this.mPackages) {
                if (this.mSettings.mReadExternalStorageEnforced == null || this.mSettings.mReadExternalStorageEnforced.booleanValue() != enforced) {
                    this.mSettings.mReadExternalStorageEnforced = Boolean.valueOf(enforced);
                    this.mSettings.writeLPr();
                }
            }
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am != null) {
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
            return;
        }
        throw new IllegalArgumentException("No selective enforcement for " + permission);
    }

    @Deprecated
    public boolean isPermissionEnforced(String permission) {
        return DEFAULT_VERIFY_ENABLE;
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
                    b = DEFAULT_VERIFY_ENABLE;
                } else {
                    b = false;
                }
                this.mUserNeedsBadging.put(userId, b);
                return b;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        boolean b2 = this.mUserNeedsBadging.valueAt(index);
        return b2;
    }

    public KeySet getKeySetByAlias(String packageName, String alias) {
        KeySet keySet;
        if (packageName == null || alias == null) {
            return null;
        }
        synchronized (this.mPackages) {
            PackageParser.Package pkg = this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package:" + packageName);
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
                Slog.w(TAG, "KeySet requested for unknown package:" + packageName);
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
        boolean zPackageIsSignedByLPr = false;
        if (packageName != null && ks != null) {
            synchronized (this.mPackages) {
                PackageParser.Package pkg = this.mPackages.get(packageName);
                if (pkg == null) {
                    Slog.w(TAG, "KeySet requested for unknown package:" + packageName);
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                IBinder ksh = ks.getToken();
                if (ksh instanceof KeySetHandle) {
                    KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
                    zPackageIsSignedByLPr = ksms.packageIsSignedByLPr(packageName, (KeySetHandle) ksh);
                }
            }
        }
        return zPackageIsSignedByLPr;
    }

    public boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) {
        boolean zPackageIsSignedByExactlyLPr = false;
        if (packageName != null && ks != null) {
            synchronized (this.mPackages) {
                PackageParser.Package pkg = this.mPackages.get(packageName);
                if (pkg == null) {
                    Slog.w(TAG, "KeySet requested for unknown package:" + packageName);
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                IBinder ksh = ks.getToken();
                if (ksh instanceof KeySetHandle) {
                    KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
                    zPackageIsSignedByExactlyLPr = ksms.packageIsSignedByExactlyLPr(packageName, (KeySetHandle) ksh);
                }
            }
        }
        return zPackageIsSignedByExactlyLPr;
    }

    public void getUsageStatsIfNoPackageUsageInfo() {
        if (!this.mPackageUsage.isHistoricalPackageUsageAvailable()) {
            UsageStatsManager usm = (UsageStatsManager) this.mContext.getSystemService("usagestats");
            if (usm == null) {
                throw new IllegalStateException("UsageStatsManager must be initialized");
            }
            long now = System.currentTimeMillis();
            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(now - this.mDexOptLRUThresholdInMills, now);
            for (Map.Entry<String, UsageStats> entry : stats.entrySet()) {
                String packageName = entry.getKey();
                PackageParser.Package pkg = this.mPackages.get(packageName);
                if (pkg != null) {
                    UsageStats usage = entry.getValue();
                    pkg.mLastPackageUsageTimeInMills = usage.getLastTimeUsed();
                    this.mPackageUsage.mIsHistoricalPackageUsageAvailable = DEFAULT_VERIFY_ENABLE;
                }
            }
        }
    }

    private static void checkDowngrade(PackageParser.Package before, PackageInfoLite after) throws PackageManagerException {
        if (after.versionCode < before.mVersionCode) {
            throw new PackageManagerException(-25, "Update version code " + after.versionCode + " is older than current " + before.mVersionCode);
        }
        if (after.versionCode == before.mVersionCode) {
            if (after.baseRevisionCode < before.baseRevisionCode) {
                throw new PackageManagerException(-25, "Update base revision code " + after.baseRevisionCode + " is older than current " + before.baseRevisionCode);
            }
            if (!ArrayUtils.isEmpty(after.splitNames)) {
                for (int i = 0; i < after.splitNames.length; i++) {
                    String splitName = after.splitNames[i];
                    int j = ArrayUtils.indexOf(before.splitNames, splitName);
                    if (j != -1 && after.splitRevisionCodes[i] < before.splitRevisionCodes[j]) {
                        throw new PackageManagerException(-25, "Update split " + splitName + " revision code " + after.splitRevisionCodes[i] + " is older than current " + before.splitRevisionCodes[j]);
                    }
                }
            }
        }
    }
}
