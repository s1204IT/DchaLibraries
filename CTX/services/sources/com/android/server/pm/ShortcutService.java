package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutService;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.Xml;
import android.view.IWindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.ShortcutUser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ShortcutService extends IShortcutService.Stub {
    private static final String ATTR_VALUE = "value";
    static final boolean DEBUG = false;
    static final boolean DEBUG_LOAD = false;
    static final boolean DEBUG_PROCSTATE = false;

    @VisibleForTesting
    static final int DEFAULT_ICON_PERSIST_QUALITY = 100;

    @VisibleForTesting
    static final int DEFAULT_MAX_ICON_DIMENSION_DP = 96;

    @VisibleForTesting
    static final int DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP = 48;

    @VisibleForTesting
    static final int DEFAULT_MAX_SHORTCUTS_PER_APP = 5;

    @VisibleForTesting
    static final int DEFAULT_MAX_UPDATES_PER_INTERVAL = 10;

    @VisibleForTesting
    static final long DEFAULT_RESET_INTERVAL_SEC = 86400;

    @VisibleForTesting
    static final int DEFAULT_SAVE_DELAY_MS = 3000;
    static final String DIRECTORY_BITMAPS = "bitmaps";

    @VisibleForTesting
    static final String DIRECTORY_DUMP = "shortcut_dump";

    @VisibleForTesting
    static final String DIRECTORY_PER_USER = "shortcut_service";
    private static final String DUMMY_MAIN_ACTIVITY = "android.__dummy__";

    @VisibleForTesting
    static final String FILENAME_BASE_STATE = "shortcut_service.xml";

    @VisibleForTesting
    static final String FILENAME_USER_PACKAGES = "shortcuts.xml";
    private static final String KEY_ICON_SIZE = "iconSize";
    private static final String KEY_LOW_RAM = "lowRam";
    private static final String KEY_SHORTCUT = "shortcut";
    private static final String LAUNCHER_INTENT_CATEGORY = "android.intent.category.LAUNCHER";
    static final int OPERATION_ADD = 1;
    static final int OPERATION_SET = 0;
    static final int OPERATION_UPDATE = 2;
    private static final int PACKAGE_MATCH_FLAGS = 794624;
    private static final int PROCESS_STATE_FOREGROUND_THRESHOLD = 4;
    static final String TAG = "ShortcutService";
    private static final String TAG_LAST_RESET_TIME = "last_reset_time";
    private static final String TAG_ROOT = "root";
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AtomicBoolean mBootCompleted;
    final Context mContext;

    @GuardedBy("mLock")
    private List<Integer> mDirtyUserIds;
    private final Handler mHandler;
    private final IPackageManager mIPackageManager;
    private Bitmap.CompressFormat mIconPersistFormat;
    private int mIconPersistQuality;

    @GuardedBy("mLock")
    private Exception mLastWtfStacktrace;

    @GuardedBy("mLock")
    private final ArrayList<ShortcutServiceInternal.ShortcutChangeListener> mListeners;
    private final Object mLock;
    private int mMaxIconDimension;
    private int mMaxShortcuts;
    int mMaxUpdatesPerInterval;
    private final PackageManagerInternal mPackageManagerInternal;

    @VisibleForTesting
    final BroadcastReceiver mPackageMonitor;

    @GuardedBy("mLock")
    private long mRawLastResetTime;
    final BroadcastReceiver mReceiver;
    private long mResetInterval;
    private int mSaveDelayMillis;
    private final Runnable mSaveDirtyInfoRunner;
    private final ShortcutBitmapSaver mShortcutBitmapSaver;
    private final ShortcutDumpFiles mShortcutDumpFiles;

    @GuardedBy("mLock")
    private final SparseArray<ShortcutNonPersistentUser> mShortcutNonPersistentUsers;
    private final ShortcutRequestPinProcessor mShortcutRequestPinProcessor;
    private final StatLogger mStatLogger;

    @GuardedBy("mLock")
    final SparseLongArray mUidLastForegroundElapsedTime;
    private final IUidObserver mUidObserver;

    @GuardedBy("mLock")
    final SparseIntArray mUidState;

    @GuardedBy("mUnlockedUsers")
    final SparseBooleanArray mUnlockedUsers;
    private final UsageStatsManagerInternal mUsageStatsManagerInternal;
    private final UserManagerInternal mUserManagerInternal;

    @GuardedBy("mLock")
    private final SparseArray<ShortcutUser> mUsers;

    @GuardedBy("mLock")
    private int mWtfCount;

    @VisibleForTesting
    static final String DEFAULT_ICON_PERSIST_FORMAT = Bitmap.CompressFormat.PNG.name();
    private static List<ResolveInfo> EMPTY_RESOLVE_INFO = new ArrayList(0);
    private static Predicate<ResolveInfo> ACTIVITY_NOT_EXPORTED = new Predicate<ResolveInfo>() {
        @Override
        public boolean test(ResolveInfo resolveInfo) {
            return !resolveInfo.activityInfo.exported;
        }
    };
    private static Predicate<PackageInfo> PACKAGE_NOT_INSTALLED = new Predicate<PackageInfo>() {
        @Override
        public boolean test(PackageInfo packageInfo) {
            return !ShortcutService.isInstalled(packageInfo);
        }
    };

    @VisibleForTesting
    interface ConfigConstants {
        public static final String KEY_ICON_FORMAT = "icon_format";
        public static final String KEY_ICON_QUALITY = "icon_quality";
        public static final String KEY_MAX_ICON_DIMENSION_DP = "max_icon_dimension_dp";
        public static final String KEY_MAX_ICON_DIMENSION_DP_LOWRAM = "max_icon_dimension_dp_lowram";
        public static final String KEY_MAX_SHORTCUTS = "max_shortcuts";
        public static final String KEY_MAX_UPDATES_PER_INTERVAL = "max_updates_per_interval";
        public static final String KEY_RESET_INTERVAL_SEC = "reset_interval_sec";
        public static final String KEY_SAVE_DELAY_MILLIS = "save_delay_ms";
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ShortcutOperation {
    }

    @VisibleForTesting
    interface Stats {
        public static final int ASYNC_PRELOAD_USER_DELAY = 15;
        public static final int CHECK_LAUNCHER_ACTIVITY = 12;
        public static final int CHECK_PACKAGE_CHANGES = 8;
        public static final int CLEANUP_DANGLING_BITMAPS = 5;
        public static final int COUNT = 17;
        public static final int GET_ACTIVITY_WITH_METADATA = 6;
        public static final int GET_APPLICATION_INFO = 3;
        public static final int GET_APPLICATION_RESOURCES = 9;
        public static final int GET_DEFAULT_HOME = 0;
        public static final int GET_DEFAULT_LAUNCHER = 16;
        public static final int GET_INSTALLED_PACKAGES = 7;
        public static final int GET_LAUNCHER_ACTIVITY = 11;
        public static final int GET_PACKAGE_INFO = 1;
        public static final int GET_PACKAGE_INFO_WITH_SIG = 2;
        public static final int IS_ACTIVITY_ENABLED = 13;
        public static final int LAUNCHER_PERMISSION_CHECK = 4;
        public static final int PACKAGE_UPDATE_CHECK = 14;
        public static final int RESOURCE_NAME_LOOKUP = 10;
    }

    static class InvalidFileFormatException extends Exception {
        public InvalidFileFormatException(String str, Throwable th) {
            super(str, th);
        }
    }

    public ShortcutService(Context context) {
        this(context, BackgroundThread.get().getLooper(), false);
    }

    @VisibleForTesting
    ShortcutService(Context context, Looper looper, boolean z) {
        this.mLock = new Object();
        this.mListeners = new ArrayList<>(1);
        this.mUsers = new SparseArray<>();
        this.mShortcutNonPersistentUsers = new SparseArray<>();
        this.mUidState = new SparseIntArray();
        this.mUidLastForegroundElapsedTime = new SparseLongArray();
        this.mDirtyUserIds = new ArrayList();
        this.mBootCompleted = new AtomicBoolean();
        this.mUnlockedUsers = new SparseBooleanArray();
        this.mStatLogger = new StatLogger(new String[]{"getHomeActivities()", "Launcher permission check", "getPackageInfo()", "getPackageInfo(SIG)", "getApplicationInfo", "cleanupDanglingBitmaps", "getActivity+metadata", "getInstalledPackages", "checkPackageChanges", "getApplicationResources", "resourceNameLookup", "getLauncherActivity", "checkLauncherActivity", "isActivityEnabled", "packageUpdateCheck", "asyncPreloadUserDelay", "getDefaultLauncher()"});
        this.mWtfCount = 0;
        this.mUidObserver = new AnonymousClass3();
        this.mSaveDirtyInfoRunner = new Runnable() {
            @Override
            public final void run() {
                this.f$0.saveDirtyInfo();
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!ShortcutService.this.mBootCompleted.get()) {
                    return;
                }
                try {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                        ShortcutService.this.handleLocaleChanged();
                    }
                } catch (Exception e) {
                    ShortcutService.this.wtf("Exception in mReceiver.onReceive", e);
                }
            }
        };
        this.mPackageMonitor = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (intExtra == -10000) {
                    Slog.w(ShortcutService.TAG, "Intent broadcast does not contain user handle: " + intent);
                    return;
                }
                String action = intent.getAction();
                long jInjectClearCallingIdentity = ShortcutService.this.injectClearCallingIdentity();
                try {
                    try {
                    } catch (Exception e) {
                        ShortcutService.this.wtf("Exception in mPackageMonitor.onReceive", e);
                    }
                    synchronized (ShortcutService.this.mLock) {
                        if (ShortcutService.this.isUserUnlockedL(intExtra)) {
                            ShortcutService.this.getUserShortcutsLocked(intExtra).clearLauncher();
                            if ("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED".equals(action)) {
                                return;
                            }
                            Uri data = intent.getData();
                            String schemeSpecificPart = data != null ? data.getSchemeSpecificPart() : null;
                            if (schemeSpecificPart == null) {
                                Slog.w(ShortcutService.TAG, "Intent broadcast does not contain package name: " + intent);
                                return;
                            }
                            byte b = 0;
                            boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                            int iHashCode = action.hashCode();
                            if (iHashCode == 172491798) {
                                if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                                    b = 2;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode == 267468725) {
                                if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                                    b = 3;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode != 525384130) {
                                if (iHashCode != 1544582882 || !action.equals("android.intent.action.PACKAGE_ADDED")) {
                                    b = -1;
                                }
                                switch (b) {
                                    case 0:
                                        if (!booleanExtra) {
                                            ShortcutService.this.handlePackageAdded(schemeSpecificPart, intExtra);
                                        } else {
                                            ShortcutService.this.handlePackageUpdateFinished(schemeSpecificPart, intExtra);
                                        }
                                        break;
                                    case 1:
                                        if (!booleanExtra) {
                                            ShortcutService.this.handlePackageRemoved(schemeSpecificPart, intExtra);
                                        }
                                        break;
                                    case 2:
                                        ShortcutService.this.handlePackageChanged(schemeSpecificPart, intExtra);
                                        break;
                                    case 3:
                                        ShortcutService.this.handlePackageDataCleared(schemeSpecificPart, intExtra);
                                        break;
                                }
                            } else {
                                if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                                    b = 1;
                                }
                                switch (b) {
                                }
                            }
                        }
                    }
                } finally {
                    ShortcutService.this.injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                }
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context);
        LocalServices.addService(ShortcutServiceInternal.class, new LocalService());
        this.mHandler = new Handler(looper);
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) Preconditions.checkNotNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Preconditions.checkNotNull((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        this.mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mShortcutRequestPinProcessor = new ShortcutRequestPinProcessor(this, this.mLock);
        this.mShortcutBitmapSaver = new ShortcutBitmapSaver(this);
        this.mShortcutDumpFiles = new ShortcutDumpFiles(this);
        if (z) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        intentFilter.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mPackageMonitor, UserHandle.ALL, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
        intentFilter2.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mPackageMonitor, UserHandle.ALL, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.LOCALE_CHANGED");
        intentFilter3.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter3, null, this.mHandler);
        injectRegisterUidObserver(this.mUidObserver, 3);
    }

    long getStatStartTime() {
        return this.mStatLogger.getTime();
    }

    void logDurationStat(int i, long j) {
        this.mStatLogger.logDurationStat(i, j);
    }

    public String injectGetLocaleTagsForUser(int i) {
        return LocaleList.getDefault().toLanguageTags();
    }

    class AnonymousClass3 extends IUidObserver.Stub {
        AnonymousClass3() {
        }

        public void onUidStateChanged(final int i, final int i2, long j) {
            ShortcutService.this.injectPostToHandler(new Runnable() {
                @Override
                public final void run() {
                    ShortcutService.this.handleOnUidStateChanged(i, i2);
                }
            });
        }

        public void onUidGone(final int i, boolean z) {
            ShortcutService.this.injectPostToHandler(new Runnable() {
                @Override
                public final void run() {
                    ShortcutService.this.handleOnUidStateChanged(i, 19);
                }
            });
        }

        public void onUidActive(int i) {
        }

        public void onUidIdle(int i, boolean z) {
        }

        public void onUidCachedChanged(int i, boolean z) {
        }
    }

    void handleOnUidStateChanged(int i, int i2) {
        synchronized (this.mLock) {
            this.mUidState.put(i, i2);
            if (isProcessStateForeground(i2)) {
                this.mUidLastForegroundElapsedTime.put(i, injectElapsedRealtime());
            }
        }
    }

    private boolean isProcessStateForeground(int i) {
        return i <= 4;
    }

    @GuardedBy("mLock")
    boolean isUidForegroundLocked(int i) {
        if (i == 1000 || isProcessStateForeground(this.mUidState.get(i, 19))) {
            return true;
        }
        return isProcessStateForeground(this.mActivityManagerInternal.getUidProcessState(i));
    }

    @GuardedBy("mLock")
    long getUidLastForegroundElapsedTimeLocked(int i) {
        return this.mUidLastForegroundElapsedTime.get(i);
    }

    public static final class Lifecycle extends SystemService {
        final ShortcutService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ShortcutService(context);
        }

        @Override
        public void onStart() {
            publishBinderService(ShortcutService.KEY_SHORTCUT, this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            this.mService.onBootPhase(i);
        }

        @Override
        public void onStopUser(int i) {
            this.mService.handleStopUser(i);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.handleUnlockUser(i);
        }
    }

    void onBootPhase(int i) {
        if (i == 480) {
            initialize();
        } else if (i == 1000) {
            this.mBootCompleted.set(true);
        }
    }

    void handleUnlockUser(final int i) {
        synchronized (this.mUnlockedUsers) {
            this.mUnlockedUsers.put(i, true);
        }
        final long statStartTime = getStatStartTime();
        injectRunOnNewThread(new Runnable() {
            @Override
            public final void run() {
                ShortcutService.lambda$handleUnlockUser$0(this.f$0, statStartTime, i);
            }
        });
    }

    public static void lambda$handleUnlockUser$0(ShortcutService shortcutService, long j, int i) {
        synchronized (shortcutService.mLock) {
            shortcutService.logDurationStat(15, j);
            shortcutService.getUserShortcutsLocked(i);
        }
    }

    void handleStopUser(int i) {
        synchronized (this.mLock) {
            unloadUserLocked(i);
            synchronized (this.mUnlockedUsers) {
                this.mUnlockedUsers.put(i, false);
            }
        }
    }

    @GuardedBy("mLock")
    private void unloadUserLocked(int i) {
        saveDirtyInfo();
        this.mUsers.delete(i);
    }

    private AtomicFile getBaseStateFile() {
        File file = new File(injectSystemDataPath(), FILENAME_BASE_STATE);
        file.mkdirs();
        return new AtomicFile(file);
    }

    private void initialize() {
        synchronized (this.mLock) {
            loadConfigurationLocked();
            loadBaseStateLocked();
        }
    }

    private void loadConfigurationLocked() {
        updateConfigurationLocked(injectShortcutManagerConstants());
    }

    @VisibleForTesting
    boolean updateConfigurationLocked(String str) {
        boolean z;
        int i;
        KeyValueListParser keyValueListParser = new KeyValueListParser(',');
        try {
            keyValueListParser.setString(str);
            z = true;
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad shortcut manager settings", e);
            z = false;
        }
        this.mSaveDelayMillis = Math.max(0, (int) keyValueListParser.getLong(ConfigConstants.KEY_SAVE_DELAY_MILLIS, 3000L));
        this.mResetInterval = Math.max(1L, keyValueListParser.getLong(ConfigConstants.KEY_RESET_INTERVAL_SEC, DEFAULT_RESET_INTERVAL_SEC) * 1000);
        this.mMaxUpdatesPerInterval = Math.max(0, (int) keyValueListParser.getLong(ConfigConstants.KEY_MAX_UPDATES_PER_INTERVAL, 10L));
        this.mMaxShortcuts = Math.max(0, (int) keyValueListParser.getLong(ConfigConstants.KEY_MAX_SHORTCUTS, 5L));
        if (injectIsLowRamDevice()) {
            i = (int) keyValueListParser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP_LOWRAM, 48L);
        } else {
            i = (int) keyValueListParser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP, 96L);
        }
        this.mMaxIconDimension = injectDipToPixel(Math.max(1, i));
        this.mIconPersistFormat = Bitmap.CompressFormat.valueOf(keyValueListParser.getString(ConfigConstants.KEY_ICON_FORMAT, DEFAULT_ICON_PERSIST_FORMAT));
        this.mIconPersistQuality = (int) keyValueListParser.getLong(ConfigConstants.KEY_ICON_QUALITY, 100L);
        return z;
    }

    @VisibleForTesting
    String injectShortcutManagerConstants() {
        return Settings.Global.getString(this.mContext.getContentResolver(), "shortcut_manager_constants");
    }

    @VisibleForTesting
    int injectDipToPixel(int i) {
        return (int) TypedValue.applyDimension(1, i, this.mContext.getResources().getDisplayMetrics());
    }

    static String parseStringAttribute(XmlPullParser xmlPullParser, String str) {
        return xmlPullParser.getAttributeValue(null, str);
    }

    static boolean parseBooleanAttribute(XmlPullParser xmlPullParser, String str) {
        return parseLongAttribute(xmlPullParser, str) == 1;
    }

    static boolean parseBooleanAttribute(XmlPullParser xmlPullParser, String str, boolean z) {
        return parseLongAttribute(xmlPullParser, str, z ? 1L : 0L) == 1;
    }

    static int parseIntAttribute(XmlPullParser xmlPullParser, String str) {
        return (int) parseLongAttribute(xmlPullParser, str);
    }

    static int parseIntAttribute(XmlPullParser xmlPullParser, String str, int i) {
        return (int) parseLongAttribute(xmlPullParser, str, i);
    }

    static long parseLongAttribute(XmlPullParser xmlPullParser, String str) {
        return parseLongAttribute(xmlPullParser, str, 0L);
    }

    static long parseLongAttribute(XmlPullParser xmlPullParser, String str, long j) {
        String stringAttribute = parseStringAttribute(xmlPullParser, str);
        if (TextUtils.isEmpty(stringAttribute)) {
            return j;
        }
        try {
            return Long.parseLong(stringAttribute);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Error parsing long " + stringAttribute);
            return j;
        }
    }

    static ComponentName parseComponentNameAttribute(XmlPullParser xmlPullParser, String str) {
        String stringAttribute = parseStringAttribute(xmlPullParser, str);
        if (TextUtils.isEmpty(stringAttribute)) {
            return null;
        }
        return ComponentName.unflattenFromString(stringAttribute);
    }

    static Intent parseIntentAttributeNoDefault(XmlPullParser xmlPullParser, String str) {
        String stringAttribute = parseStringAttribute(xmlPullParser, str);
        if (!TextUtils.isEmpty(stringAttribute)) {
            try {
                return Intent.parseUri(stringAttribute, 0);
            } catch (URISyntaxException e) {
                Slog.e(TAG, "Error parsing intent", e);
            }
        }
        return null;
    }

    static Intent parseIntentAttribute(XmlPullParser xmlPullParser, String str) {
        Intent intentAttributeNoDefault = parseIntentAttributeNoDefault(xmlPullParser, str);
        if (intentAttributeNoDefault == null) {
            return new Intent("android.intent.action.VIEW");
        }
        return intentAttributeNoDefault;
    }

    static void writeTagValue(XmlSerializer xmlSerializer, String str, String str2) throws IOException {
        if (TextUtils.isEmpty(str2)) {
            return;
        }
        xmlSerializer.startTag(null, str);
        xmlSerializer.attribute(null, ATTR_VALUE, str2);
        xmlSerializer.endTag(null, str);
    }

    static void writeTagValue(XmlSerializer xmlSerializer, String str, long j) throws IOException {
        writeTagValue(xmlSerializer, str, Long.toString(j));
    }

    static void writeTagValue(XmlSerializer xmlSerializer, String str, ComponentName componentName) throws IOException {
        if (componentName == null) {
            return;
        }
        writeTagValue(xmlSerializer, str, componentName.flattenToString());
    }

    static void writeTagExtra(XmlSerializer xmlSerializer, String str, PersistableBundle persistableBundle) throws XmlPullParserException, IOException {
        if (persistableBundle == null) {
            return;
        }
        xmlSerializer.startTag(null, str);
        persistableBundle.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, str);
    }

    static void writeAttr(XmlSerializer xmlSerializer, String str, CharSequence charSequence) throws IOException {
        if (TextUtils.isEmpty(charSequence)) {
            return;
        }
        xmlSerializer.attribute(null, str, charSequence.toString());
    }

    static void writeAttr(XmlSerializer xmlSerializer, String str, long j) throws IOException {
        writeAttr(xmlSerializer, str, String.valueOf(j));
    }

    static void writeAttr(XmlSerializer xmlSerializer, String str, boolean z) throws IOException {
        if (z) {
            writeAttr(xmlSerializer, str, "1");
        } else {
            writeAttr(xmlSerializer, str, "0");
        }
    }

    static void writeAttr(XmlSerializer xmlSerializer, String str, ComponentName componentName) throws IOException {
        if (componentName == null) {
            return;
        }
        writeAttr(xmlSerializer, str, componentName.flattenToString());
    }

    static void writeAttr(XmlSerializer xmlSerializer, String str, Intent intent) throws IOException {
        if (intent == null) {
            return;
        }
        writeAttr(xmlSerializer, str, intent.toUri(0));
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void saveBaseStateLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        AtomicFile baseStateFile = getBaseStateFile();
        try {
            fileOutputStreamStartWrite = baseStateFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_ROOT);
                writeTagValue((XmlSerializer) fastXmlSerializer, TAG_LAST_RESET_TIME, this.mRawLastResetTime);
                fastXmlSerializer.endTag(null, TAG_ROOT);
                fastXmlSerializer.endDocument();
                baseStateFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e2) {
                e = e2;
                Slog.e(TAG, "Failed to write to file " + baseStateFile.getBaseFile(), e);
                baseStateFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException e3) {
            fileOutputStreamStartWrite = null;
            e = e3;
        }
    }

    @GuardedBy("mLock")
    private void loadBaseStateLocked() {
        this.mRawLastResetTime = 0L;
        AtomicFile baseStateFile = getBaseStateFile();
        try {
            try {
                FileInputStream fileInputStreamOpenRead = baseStateFile.openRead();
                Throwable th = null;
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    while (true) {
                        int next = xmlPullParserNewPullParser.next();
                        if (next != 1) {
                            if (next == 2) {
                                int depth = xmlPullParserNewPullParser.getDepth();
                                String name = xmlPullParserNewPullParser.getName();
                                if (depth != 1) {
                                    byte b = -1;
                                    if (name.hashCode() == -68726522 && name.equals(TAG_LAST_RESET_TIME)) {
                                        b = 0;
                                    }
                                    if (b != 0) {
                                        Slog.e(TAG, "Invalid tag: " + name);
                                    } else {
                                        this.mRawLastResetTime = parseLongAttribute(xmlPullParserNewPullParser, ATTR_VALUE);
                                    }
                                } else if (!TAG_ROOT.equals(name)) {
                                    break;
                                }
                            }
                        } else if (fileInputStreamOpenRead != null) {
                            fileInputStreamOpenRead.close();
                        }
                    }
                } catch (Throwable th2) {
                    if (fileInputStreamOpenRead != null) {
                        if (0 != 0) {
                            try {
                                fileInputStreamOpenRead.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            fileInputStreamOpenRead.close();
                        }
                    }
                    throw th2;
                }
            } catch (FileNotFoundException e) {
            }
        } catch (IOException | XmlPullParserException e2) {
            Slog.e(TAG, "Failed to read file " + baseStateFile.getBaseFile(), e2);
            this.mRawLastResetTime = 0L;
        }
        getLastResetTimeLocked();
    }

    @VisibleForTesting
    final File getUserFile(int i) {
        return new File(injectUserDataPath(i), FILENAME_USER_PACKAGES);
    }

    @GuardedBy("mLock")
    private void saveUserLocked(int i) {
        FileOutputStream fileOutputStreamStartWrite;
        File userFile = getUserFile(i);
        this.mShortcutBitmapSaver.waitForAllSavesLocked();
        userFile.getParentFile().mkdirs();
        AtomicFile atomicFile = new AtomicFile(userFile);
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
            try {
                saveUserInternalLocked(i, fileOutputStreamStartWrite, false);
                atomicFile.finishWrite(fileOutputStreamStartWrite);
                cleanupDanglingBitmapDirectoriesLocked(i);
            } catch (IOException | XmlPullParserException e) {
                e = e;
                Slog.e(TAG, "Failed to write to file " + atomicFile.getBaseFile(), e);
                atomicFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException | XmlPullParserException e2) {
            e = e2;
            fileOutputStreamStartWrite = null;
        }
    }

    @GuardedBy("mLock")
    private void saveUserInternalLocked(int i, OutputStream outputStream, boolean z) throws XmlPullParserException, IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
        fastXmlSerializer.startDocument(null, true);
        getUserShortcutsLocked(i).saveToXml(fastXmlSerializer, z);
        fastXmlSerializer.endDocument();
        bufferedOutputStream.flush();
        outputStream.flush();
    }

    static IOException throwForInvalidTag(int i, String str) throws IOException {
        throw new IOException(String.format("Invalid tag '%s' found at depth %d", str, Integer.valueOf(i)));
    }

    static void warnForInvalidTag(int i, String str) throws IOException {
        Slog.w(TAG, String.format("Invalid tag '%s' found at depth %d", str, Integer.valueOf(i)));
    }

    private ShortcutUser loadUserLocked(int i) {
        AtomicFile atomicFile = new AtomicFile(getUserFile(i));
        try {
            FileInputStream fileInputStreamOpenRead = atomicFile.openRead();
            try {
                return loadUserInternal(i, fileInputStreamOpenRead, false);
            } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to read file " + atomicFile.getBaseFile(), e);
                return null;
            } finally {
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    private ShortcutUser loadUserInternal(int i, InputStream inputStream, boolean z) throws XmlPullParserException, IOException, InvalidFileFormatException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(bufferedInputStream, StandardCharsets.UTF_8.name());
        ShortcutUser shortcutUserLoadFromXml = null;
        while (true) {
            int next = xmlPullParserNewPullParser.next();
            if (next != 1) {
                if (next == 2) {
                    int depth = xmlPullParserNewPullParser.getDepth();
                    String name = xmlPullParserNewPullParser.getName();
                    if (depth == 1 && "user".equals(name)) {
                        shortcutUserLoadFromXml = ShortcutUser.loadFromXml(this, xmlPullParserNewPullParser, i, z);
                    } else {
                        throwForInvalidTag(depth, name);
                    }
                }
            } else {
                return shortcutUserLoadFromXml;
            }
        }
    }

    private void scheduleSaveBaseState() {
        scheduleSaveInner(-10000);
    }

    void scheduleSaveUser(int i) {
        scheduleSaveInner(i);
    }

    private void scheduleSaveInner(int i) {
        synchronized (this.mLock) {
            if (!this.mDirtyUserIds.contains(Integer.valueOf(i))) {
                this.mDirtyUserIds.add(Integer.valueOf(i));
            }
        }
        this.mHandler.removeCallbacks(this.mSaveDirtyInfoRunner);
        this.mHandler.postDelayed(this.mSaveDirtyInfoRunner, this.mSaveDelayMillis);
    }

    @VisibleForTesting
    void saveDirtyInfo() {
        try {
            synchronized (this.mLock) {
                for (int size = this.mDirtyUserIds.size() - 1; size >= 0; size--) {
                    int iIntValue = this.mDirtyUserIds.get(size).intValue();
                    if (iIntValue == -10000) {
                        saveBaseStateLocked();
                    } else {
                        saveUserLocked(iIntValue);
                    }
                }
                this.mDirtyUserIds.clear();
            }
        } catch (Exception e) {
            wtf("Exception in saveDirtyInfo", e);
        }
    }

    @GuardedBy("mLock")
    long getLastResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime;
    }

    @GuardedBy("mLock")
    long getNextResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime + this.mResetInterval;
    }

    static boolean isClockValid(long j) {
        return j >= 1420070400;
    }

    @GuardedBy("mLock")
    private void updateTimesLocked() {
        long jInjectCurrentTimeMillis = injectCurrentTimeMillis();
        long j = this.mRawLastResetTime;
        if (this.mRawLastResetTime == 0) {
            this.mRawLastResetTime = jInjectCurrentTimeMillis;
        } else if (jInjectCurrentTimeMillis < this.mRawLastResetTime) {
            if (isClockValid(jInjectCurrentTimeMillis)) {
                Slog.w(TAG, "Clock rewound");
                this.mRawLastResetTime = jInjectCurrentTimeMillis;
            }
        } else if (this.mRawLastResetTime + this.mResetInterval <= jInjectCurrentTimeMillis) {
            this.mRawLastResetTime = ((jInjectCurrentTimeMillis / this.mResetInterval) * this.mResetInterval) + (this.mRawLastResetTime % this.mResetInterval);
        }
        if (j != this.mRawLastResetTime) {
            scheduleSaveBaseState();
        }
    }

    protected boolean isUserUnlockedL(int i) {
        synchronized (this.mUnlockedUsers) {
            if (this.mUnlockedUsers.get(i)) {
                return true;
            }
            return this.mUserManagerInternal.isUserUnlockingOrUnlocked(i);
        }
    }

    void throwIfUserLockedL(int i) {
        if (!isUserUnlockedL(i)) {
            throw new IllegalStateException("User " + i + " is locked or not running");
        }
    }

    @GuardedBy("mLock")
    private boolean isUserLoadedLocked(int i) {
        return this.mUsers.get(i) != null;
    }

    @GuardedBy("mLock")
    ShortcutUser getUserShortcutsLocked(int i) {
        if (!isUserUnlockedL(i)) {
            wtf("User still locked");
        }
        ShortcutUser shortcutUserLoadUserLocked = this.mUsers.get(i);
        if (shortcutUserLoadUserLocked == null) {
            shortcutUserLoadUserLocked = loadUserLocked(i);
            if (shortcutUserLoadUserLocked == null) {
                shortcutUserLoadUserLocked = new ShortcutUser(this, i);
            }
            this.mUsers.put(i, shortcutUserLoadUserLocked);
            checkPackageChanges(i);
        }
        return shortcutUserLoadUserLocked;
    }

    @GuardedBy("mLock")
    ShortcutNonPersistentUser getNonPersistentUserLocked(int i) {
        ShortcutNonPersistentUser shortcutNonPersistentUser = this.mShortcutNonPersistentUsers.get(i);
        if (shortcutNonPersistentUser == null) {
            ShortcutNonPersistentUser shortcutNonPersistentUser2 = new ShortcutNonPersistentUser(this, i);
            this.mShortcutNonPersistentUsers.put(i, shortcutNonPersistentUser2);
            return shortcutNonPersistentUser2;
        }
        return shortcutNonPersistentUser;
    }

    @GuardedBy("mLock")
    void forEachLoadedUserLocked(Consumer<ShortcutUser> consumer) {
        for (int size = this.mUsers.size() - 1; size >= 0; size--) {
            consumer.accept(this.mUsers.valueAt(size));
        }
    }

    @GuardedBy("mLock")
    ShortcutPackage getPackageShortcutsLocked(String str, int i) {
        return getUserShortcutsLocked(i).getPackageShortcuts(str);
    }

    @GuardedBy("mLock")
    ShortcutPackage getPackageShortcutsForPublisherLocked(String str, int i) {
        ShortcutPackage packageShortcuts = getUserShortcutsLocked(i).getPackageShortcuts(str);
        packageShortcuts.getUser().onCalledByPublisher(str);
        return packageShortcuts;
    }

    @GuardedBy("mLock")
    ShortcutLauncher getLauncherShortcutsLocked(String str, int i, int i2) {
        return getUserShortcutsLocked(i).getLauncherShortcuts(str, i2);
    }

    void removeIconLocked(ShortcutInfo shortcutInfo) {
        this.mShortcutBitmapSaver.removeIcon(shortcutInfo);
    }

    public void cleanupBitmapsForPackage(int i, String str) {
        File file = new File(getUserBitmapFilePath(i), str);
        if (!file.isDirectory()) {
            return;
        }
        if (!FileUtils.deleteContents(file) || !file.delete()) {
            Slog.w(TAG, "Unable to remove directory " + file);
        }
    }

    @GuardedBy("mLock")
    private void cleanupDanglingBitmapDirectoriesLocked(int i) {
        long statStartTime = getStatStartTime();
        ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
        File[] fileArrListFiles = getUserBitmapFilePath(i).listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file : fileArrListFiles) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!userShortcutsLocked.hasPackage(name)) {
                    cleanupBitmapsForPackage(i, name);
                } else {
                    cleanupDanglingBitmapFilesLocked(i, userShortcutsLocked, name, file);
                }
            }
        }
        logDurationStat(5, statStartTime);
    }

    private void cleanupDanglingBitmapFilesLocked(int i, ShortcutUser shortcutUser, String str, File file) {
        ArraySet<String> usedBitmapFiles = shortcutUser.getPackageShortcuts(str).getUsedBitmapFiles();
        File[] fileArrListFiles = file.listFiles();
        for (File file2 : fileArrListFiles) {
            if (file2.isFile() && !usedBitmapFiles.contains(file2.getName())) {
                file2.delete();
            }
        }
    }

    @VisibleForTesting
    static class FileOutputStreamWithPath extends FileOutputStream {
        private final File mFile;

        public FileOutputStreamWithPath(File file) throws FileNotFoundException {
            super(file);
            this.mFile = file;
        }

        public File getFile() {
            return this.mFile;
        }
    }

    FileOutputStreamWithPath openIconFileForWrite(int i, ShortcutInfo shortcutInfo) throws IOException {
        String str;
        File file = new File(getUserBitmapFilePath(i), shortcutInfo.getPackage());
        if (!file.isDirectory()) {
            file.mkdirs();
            if (!file.isDirectory()) {
                throw new IOException("Unable to create directory " + file);
            }
            SELinux.restorecon(file);
        }
        String strValueOf = String.valueOf(injectCurrentTimeMillis());
        int i2 = 0;
        while (true) {
            StringBuilder sb = new StringBuilder();
            if (i2 == 0) {
                str = strValueOf;
            } else {
                str = strValueOf + "_" + i2;
            }
            sb.append(str);
            sb.append(".png");
            File file2 = new File(file, sb.toString());
            if (file2.exists()) {
                i2++;
            } else {
                return new FileOutputStreamWithPath(file2);
            }
        }
    }

    void saveIconAndFixUpShortcutLocked(ShortcutInfo shortcutInfo) {
        if (shortcutInfo.hasIconFile() || shortcutInfo.hasIconResource()) {
            return;
        }
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            removeIconLocked(shortcutInfo);
            Icon icon = shortcutInfo.getIcon();
            if (icon == null) {
                return;
            }
            int extraInsetFraction = this.mMaxIconDimension;
            try {
                int type = icon.getType();
                if (type != 5) {
                    switch (type) {
                        case 1:
                            icon.getBitmap();
                            break;
                        case 2:
                            injectValidateIconResPackage(shortcutInfo, icon);
                            shortcutInfo.setIconResourceId(icon.getResId());
                            shortcutInfo.addFlags(4);
                            return;
                        default:
                            throw ShortcutInfo.getInvalidIconException();
                    }
                } else {
                    icon.getBitmap();
                    extraInsetFraction = (int) (extraInsetFraction * (1.0f + (2.0f * AdaptiveIconDrawable.getExtraInsetFraction())));
                }
                this.mShortcutBitmapSaver.saveBitmapLocked(shortcutInfo, extraInsetFraction, this.mIconPersistFormat, this.mIconPersistQuality);
            } finally {
                shortcutInfo.clearIcon();
            }
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    void injectValidateIconResPackage(ShortcutInfo shortcutInfo, Icon icon) {
        if (!shortcutInfo.getPackage().equals(icon.getResPackage())) {
            throw new IllegalArgumentException("Icon resource must reside in shortcut owner package");
        }
    }

    static Bitmap shrinkBitmap(Bitmap bitmap, int i) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= i && height <= i) {
            return bitmap;
        }
        int iMax = Math.max(width, height);
        int i2 = (width * i) / iMax;
        int i3 = (height * i) / iMax;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
        new Canvas(bitmapCreateBitmap).drawBitmap(bitmap, (Rect) null, new RectF(0.0f, 0.0f, i2, i3), (Paint) null);
        return bitmapCreateBitmap;
    }

    void fixUpShortcutResourceNamesAndValues(ShortcutInfo shortcutInfo) {
        Resources resourcesInjectGetResourcesForApplicationAsUser = injectGetResourcesForApplicationAsUser(shortcutInfo.getPackage(), shortcutInfo.getUserId());
        if (resourcesInjectGetResourcesForApplicationAsUser != null) {
            long statStartTime = getStatStartTime();
            try {
                shortcutInfo.lookupAndFillInResourceNames(resourcesInjectGetResourcesForApplicationAsUser);
                logDurationStat(10, statStartTime);
                shortcutInfo.resolveResourceStrings(resourcesInjectGetResourcesForApplicationAsUser);
            } catch (Throwable th) {
                logDurationStat(10, statStartTime);
                throw th;
            }
        }
    }

    private boolean isCallerSystem() {
        return UserHandle.isSameApp(injectBinderCallingUid(), 1000);
    }

    private boolean isCallerShell() {
        int iInjectBinderCallingUid = injectBinderCallingUid();
        return iInjectBinderCallingUid == 2000 || iInjectBinderCallingUid == 0;
    }

    private void enforceSystemOrShell() {
        if (!isCallerSystem() && !isCallerShell()) {
            throw new SecurityException("Caller must be system or shell");
        }
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    private void enforceSystem() {
        if (!isCallerSystem()) {
            throw new SecurityException("Caller must be system");
        }
    }

    private void enforceResetThrottlingPermission() {
        if (isCallerSystem()) {
            return;
        }
        enforceCallingOrSelfPermission("android.permission.RESET_SHORTCUT_MANAGER_THROTTLING", null);
    }

    private void enforceCallingOrSelfPermission(String str, String str2) {
        if (isCallerSystem()) {
            return;
        }
        injectEnforceCallingPermission(str, str2);
    }

    @VisibleForTesting
    void injectEnforceCallingPermission(String str, String str2) {
        this.mContext.enforceCallingPermission(str, str2);
    }

    private void verifyCaller(String str, int i) {
        Preconditions.checkStringNotEmpty(str, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
        if (isCallerSystem()) {
            return;
        }
        int iInjectBinderCallingUid = injectBinderCallingUid();
        if (UserHandle.getUserId(iInjectBinderCallingUid) != i) {
            throw new SecurityException("Invalid user-ID");
        }
        if (injectGetPackageUid(str, i) != iInjectBinderCallingUid) {
            throw new SecurityException("Calling package name mismatch");
        }
        Preconditions.checkState(!isEphemeralApp(str, i), "Ephemeral apps can't use ShortcutManager");
    }

    private void verifyShortcutInfoPackage(String str, ShortcutInfo shortcutInfo) {
        if (shortcutInfo != null && !Objects.equals(str, shortcutInfo.getPackage())) {
            EventLog.writeEvent(1397638484, "109824443", -1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            throw new SecurityException("Shortcut package name mismatch");
        }
    }

    private void verifyShortcutInfoPackages(String str, List<ShortcutInfo> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            verifyShortcutInfoPackage(str, list.get(i));
        }
    }

    void injectPostToHandler(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    void injectRunOnNewThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    void enforceMaxActivityShortcuts(int i) {
        if (i > this.mMaxShortcuts) {
            throw new IllegalArgumentException("Max number of dynamic shortcuts exceeded");
        }
    }

    int getMaxActivityShortcuts() {
        return this.mMaxShortcuts;
    }

    void packageShortcutsChanged(String str, int i) {
        notifyListeners(str, i);
        scheduleSaveUser(i);
    }

    private void notifyListeners(final String str, final int i) {
        injectPostToHandler(new Runnable() {
            @Override
            public final void run() {
                ShortcutService.lambda$notifyListeners$1(this.f$0, i, str);
            }
        });
    }

    public static void lambda$notifyListeners$1(ShortcutService shortcutService, int i, String str) {
        try {
            synchronized (shortcutService.mLock) {
                if (shortcutService.isUserUnlockedL(i)) {
                    ArrayList arrayList = new ArrayList(shortcutService.mListeners);
                    for (int size = arrayList.size() - 1; size >= 0; size--) {
                        ((ShortcutServiceInternal.ShortcutChangeListener) arrayList.get(size)).onShortcutChanged(str, i);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcutInfo, boolean z, boolean z2) {
        if (shortcutInfo.isReturnedByServer()) {
            Log.w(TAG, "Re-publishing ShortcutInfo returned by server is not supported. Some information such as icon may lost from shortcut.");
        }
        Preconditions.checkNotNull(shortcutInfo, "Null shortcut detected");
        if (shortcutInfo.getActivity() != null) {
            Preconditions.checkState(shortcutInfo.getPackage().equals(shortcutInfo.getActivity().getPackageName()), "Cannot publish shortcut: activity " + shortcutInfo.getActivity() + " does not belong to package " + shortcutInfo.getPackage());
            Preconditions.checkState(injectIsMainActivity(shortcutInfo.getActivity(), shortcutInfo.getUserId()), "Cannot publish shortcut: activity " + shortcutInfo.getActivity() + " is not main activity");
        }
        if (!z) {
            shortcutInfo.enforceMandatoryFields(z2);
            if (!z2) {
                Preconditions.checkState(shortcutInfo.getActivity() != null, "Cannot publish shortcut: target activity is not set");
            }
        }
        if (shortcutInfo.getIcon() != null) {
            ShortcutInfo.validateIcon(shortcutInfo.getIcon());
        }
        shortcutInfo.replaceFlags(0);
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcutInfo, boolean z) {
        fixUpIncomingShortcutInfo(shortcutInfo, z, false);
    }

    public void validateShortcutForPinRequest(ShortcutInfo shortcutInfo) {
        fixUpIncomingShortcutInfo(shortcutInfo, false, true);
    }

    private void fillInDefaultActivity(List<ShortcutInfo> list) {
        ComponentName componentNameInjectGetDefaultMainActivity = null;
        for (int size = list.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfo = list.get(size);
            if (shortcutInfo.getActivity() == null) {
                if (componentNameInjectGetDefaultMainActivity == null) {
                    componentNameInjectGetDefaultMainActivity = injectGetDefaultMainActivity(shortcutInfo.getPackage(), shortcutInfo.getUserId());
                    Preconditions.checkState(componentNameInjectGetDefaultMainActivity != null, "Launcher activity not found for package " + shortcutInfo.getPackage());
                }
                shortcutInfo.setActivity(componentNameInjectGetDefaultMainActivity);
            }
        }
    }

    private void assignImplicitRanks(List<ShortcutInfo> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            list.get(size).setImplicitRank(size);
        }
    }

    private List<ShortcutInfo> setReturnedByServer(List<ShortcutInfo> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            list.get(size).setReturnedByServer();
        }
        return list;
    }

    public boolean setDynamicShortcuts(String str, ParceledListSlice parceledListSlice, int i) {
        verifyCaller(str, i);
        List<ShortcutInfo> list = parceledListSlice.getList();
        verifyShortcutInfoPackages(str, list);
        int size = list.size();
        boolean zInjectHasUnlimitedShortcutsApiCallsPermission = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncluded(list, true);
            fillInDefaultActivity(list);
            packageShortcutsForPublisherLocked.enforceShortcutCountsBeforeOperation(list, 0);
            if (!packageShortcutsForPublisherLocked.tryApiCall(zInjectHasUnlimitedShortcutsApiCallsPermission)) {
                return false;
            }
            packageShortcutsForPublisherLocked.clearAllImplicitRanks();
            assignImplicitRanks(list);
            for (int i2 = 0; i2 < size; i2++) {
                fixUpIncomingShortcutInfo(list.get(i2), false);
            }
            packageShortcutsForPublisherLocked.deleteAllDynamicShortcuts(true);
            for (int i3 = 0; i3 < size; i3++) {
                packageShortcutsForPublisherLocked.addOrReplaceDynamicShortcut(list.get(i3));
            }
            packageShortcutsForPublisherLocked.adjustRanks();
            packageShortcutsChanged(str, i);
            verifyStates();
            return true;
        }
    }

    public boolean updateShortcuts(String str, ParceledListSlice parceledListSlice, int i) {
        verifyCaller(str, i);
        List<ShortcutInfo> list = parceledListSlice.getList();
        verifyShortcutInfoPackages(str, list);
        int size = list.size();
        boolean zInjectHasUnlimitedShortcutsApiCallsPermission = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncluded(list, true);
            packageShortcutsForPublisherLocked.enforceShortcutCountsBeforeOperation(list, 2);
            if (!packageShortcutsForPublisherLocked.tryApiCall(zInjectHasUnlimitedShortcutsApiCallsPermission)) {
                return false;
            }
            packageShortcutsForPublisherLocked.clearAllImplicitRanks();
            assignImplicitRanks(list);
            for (int i2 = 0; i2 < size; i2++) {
                ShortcutInfo shortcutInfo = list.get(i2);
                fixUpIncomingShortcutInfo(shortcutInfo, true);
                ShortcutInfo shortcutInfoFindShortcutById = packageShortcutsForPublisherLocked.findShortcutById(shortcutInfo.getId());
                if (shortcutInfoFindShortcutById != null && shortcutInfoFindShortcutById.isVisibleToPublisher()) {
                    if (shortcutInfoFindShortcutById.isEnabled() != shortcutInfo.isEnabled()) {
                        Slog.w(TAG, "ShortcutInfo.enabled cannot be changed with updateShortcuts()");
                    }
                    if (shortcutInfo.hasRank()) {
                        shortcutInfoFindShortcutById.setRankChanged();
                        shortcutInfoFindShortcutById.setImplicitRank(shortcutInfo.getImplicitRank());
                    }
                    boolean z = shortcutInfo.getIcon() != null;
                    if (z) {
                        removeIconLocked(shortcutInfoFindShortcutById);
                    }
                    shortcutInfoFindShortcutById.copyNonNullFieldsFrom(shortcutInfo);
                    shortcutInfoFindShortcutById.setTimestamp(injectCurrentTimeMillis());
                    if (z) {
                        saveIconAndFixUpShortcutLocked(shortcutInfoFindShortcutById);
                    }
                    if (z || shortcutInfo.hasStringResources()) {
                        fixUpShortcutResourceNamesAndValues(shortcutInfoFindShortcutById);
                    }
                }
            }
            packageShortcutsForPublisherLocked.adjustRanks();
            packageShortcutsChanged(str, i);
            verifyStates();
            return true;
        }
    }

    public boolean addDynamicShortcuts(String str, ParceledListSlice parceledListSlice, int i) {
        verifyCaller(str, i);
        List<ShortcutInfo> list = parceledListSlice.getList();
        verifyShortcutInfoPackages(str, list);
        int size = list.size();
        boolean zInjectHasUnlimitedShortcutsApiCallsPermission = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncluded(list, true);
            fillInDefaultActivity(list);
            packageShortcutsForPublisherLocked.enforceShortcutCountsBeforeOperation(list, 1);
            packageShortcutsForPublisherLocked.clearAllImplicitRanks();
            assignImplicitRanks(list);
            if (!packageShortcutsForPublisherLocked.tryApiCall(zInjectHasUnlimitedShortcutsApiCallsPermission)) {
                return false;
            }
            for (int i2 = 0; i2 < size; i2++) {
                ShortcutInfo shortcutInfo = list.get(i2);
                fixUpIncomingShortcutInfo(shortcutInfo, false);
                shortcutInfo.setRankChanged();
                packageShortcutsForPublisherLocked.addOrReplaceDynamicShortcut(shortcutInfo);
            }
            packageShortcutsForPublisherLocked.adjustRanks();
            packageShortcutsChanged(str, i);
            verifyStates();
            return true;
        }
    }

    public boolean requestPinShortcut(String str, ShortcutInfo shortcutInfo, IntentSender intentSender, int i) {
        Preconditions.checkNotNull(shortcutInfo);
        Preconditions.checkArgument(shortcutInfo.isEnabled(), "Shortcut must be enabled");
        return requestPinItem(str, i, shortcutInfo, null, null, intentSender);
    }

    public Intent createShortcutResultIntent(String str, ShortcutInfo shortcutInfo, int i) throws RemoteException {
        Intent intentCreateShortcutResultIntent;
        Preconditions.checkNotNull(shortcutInfo);
        Preconditions.checkArgument(shortcutInfo.isEnabled(), "Shortcut must be enabled");
        verifyCaller(str, i);
        verifyShortcutInfoPackage(str, shortcutInfo);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            intentCreateShortcutResultIntent = this.mShortcutRequestPinProcessor.createShortcutResultIntent(shortcutInfo, i);
        }
        verifyStates();
        return intentCreateShortcutResultIntent;
    }

    private boolean requestPinItem(String str, int i, ShortcutInfo shortcutInfo, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle, IntentSender intentSender) {
        boolean zRequestPinItemLocked;
        verifyCaller(str, i);
        verifyShortcutInfoPackage(str, shortcutInfo);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            Preconditions.checkState(isUidForegroundLocked(injectBinderCallingUid()), "Calling application must have a foreground activity or a foreground service");
            if (shortcutInfo != null) {
                ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
                if (packageShortcutsForPublisherLocked.isShortcutExistsAndInvisibleToPublisher(shortcutInfo.getId())) {
                    packageShortcutsForPublisherLocked.updateInvisibleShortcutForPinRequestWith(shortcutInfo);
                    packageShortcutsChanged(str, i);
                }
            }
            zRequestPinItemLocked = this.mShortcutRequestPinProcessor.requestPinItemLocked(shortcutInfo, appWidgetProviderInfo, bundle, i, intentSender);
        }
        verifyStates();
        return zRequestPinItemLocked;
    }

    public void disableShortcuts(String str, List list, CharSequence charSequence, int i, int i2) {
        verifyCaller(str, i2);
        Preconditions.checkNotNull(list, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(i2);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i2);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncludedWithIds(list, true);
            String string = charSequence == null ? null : charSequence.toString();
            for (int size = list.size() - 1; size >= 0; size--) {
                String str2 = (String) Preconditions.checkStringNotEmpty((String) list.get(size));
                if (packageShortcutsForPublisherLocked.isShortcutExistsAndVisibleToPublisher(str2)) {
                    packageShortcutsForPublisherLocked.disableWithId(str2, string, i, false, true, 1);
                }
            }
            packageShortcutsForPublisherLocked.adjustRanks();
        }
        packageShortcutsChanged(str, i2);
        verifyStates();
    }

    public void enableShortcuts(String str, List list, int i) {
        verifyCaller(str, i);
        Preconditions.checkNotNull(list, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncludedWithIds(list, true);
            for (int size = list.size() - 1; size >= 0; size--) {
                String str2 = (String) Preconditions.checkStringNotEmpty((String) list.get(size));
                if (packageShortcutsForPublisherLocked.isShortcutExistsAndVisibleToPublisher(str2)) {
                    packageShortcutsForPublisherLocked.enableWithId(str2);
                }
            }
        }
        packageShortcutsChanged(str, i);
        verifyStates();
    }

    public void removeDynamicShortcuts(String str, List list, int i) {
        verifyCaller(str, i);
        Preconditions.checkNotNull(list, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsForPublisherLocked = getPackageShortcutsForPublisherLocked(str, i);
            packageShortcutsForPublisherLocked.ensureImmutableShortcutsNotIncludedWithIds(list, true);
            for (int size = list.size() - 1; size >= 0; size--) {
                String str2 = (String) Preconditions.checkStringNotEmpty((String) list.get(size));
                if (packageShortcutsForPublisherLocked.isShortcutExistsAndVisibleToPublisher(str2)) {
                    packageShortcutsForPublisherLocked.deleteDynamicWithId(str2, true);
                }
            }
            packageShortcutsForPublisherLocked.adjustRanks();
        }
        packageShortcutsChanged(str, i);
        verifyStates();
    }

    public void removeAllDynamicShortcuts(String str, int i) {
        verifyCaller(str, i);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            getPackageShortcutsForPublisherLocked(str, i).deleteAllDynamicShortcuts(true);
        }
        packageShortcutsChanged(str, i);
        verifyStates();
    }

    public ParceledListSlice<ShortcutInfo> getDynamicShortcuts(String str, int i) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(str, i);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(str, i, 9, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((ShortcutInfo) obj).isDynamicVisible();
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    public ParceledListSlice<ShortcutInfo> getManifestShortcuts(String str, int i) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(str, i);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(str, i, 9, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((ShortcutInfo) obj).isManifestVisible();
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    public ParceledListSlice<ShortcutInfo> getPinnedShortcuts(String str, int i) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(str, i);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(str, i, 9, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((ShortcutInfo) obj).isPinnedVisible();
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    @GuardedBy("mLock")
    private ParceledListSlice<ShortcutInfo> getShortcutsWithQueryLocked(String str, int i, int i2, Predicate<ShortcutInfo> predicate) {
        ArrayList arrayList = new ArrayList();
        getPackageShortcutsForPublisherLocked(str, i).findAll(arrayList, predicate, i2);
        return new ParceledListSlice<>(setReturnedByServer(arrayList));
    }

    public int getMaxShortcutCountPerActivity(String str, int i) throws RemoteException {
        verifyCaller(str, i);
        return this.mMaxShortcuts;
    }

    public int getRemainingCallCount(String str, int i) {
        int apiCallCount;
        verifyCaller(str, i);
        boolean zInjectHasUnlimitedShortcutsApiCallsPermission = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            apiCallCount = this.mMaxUpdatesPerInterval - getPackageShortcutsForPublisherLocked(str, i).getApiCallCount(zInjectHasUnlimitedShortcutsApiCallsPermission);
        }
        return apiCallCount;
    }

    public long getRateLimitResetTime(String str, int i) {
        long nextResetTimeLocked;
        verifyCaller(str, i);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            nextResetTimeLocked = getNextResetTimeLocked();
        }
        return nextResetTimeLocked;
    }

    public int getIconMaxDimensions(String str, int i) {
        int i2;
        verifyCaller(str, i);
        synchronized (this.mLock) {
            i2 = this.mMaxIconDimension;
        }
        return i2;
    }

    public void reportShortcutUsed(String str, String str2, int i) {
        verifyCaller(str, i);
        Preconditions.checkNotNull(str2);
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            if (getPackageShortcutsForPublisherLocked(str, i).findShortcutById(str2) == null) {
                Log.w(TAG, String.format("reportShortcutUsed: package %s doesn't have shortcut %s", str, str2));
                return;
            }
            long jInjectClearCallingIdentity = injectClearCallingIdentity();
            try {
                this.mUsageStatsManagerInternal.reportShortcutUsage(str, str2, i);
            } finally {
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            }
        }
    }

    public boolean isRequestPinItemSupported(int i, int i2) {
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return this.mShortcutRequestPinProcessor.isRequestPinItemSupported(i, i2);
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    public void resetThrottling() {
        enforceSystemOrShell();
        resetThrottlingInner(getCallingUserId());
    }

    void resetThrottlingInner(int i) {
        synchronized (this.mLock) {
            if (!isUserUnlockedL(i)) {
                Log.w(TAG, "User " + i + " is locked or not running");
                return;
            }
            getUserShortcutsLocked(i).resetThrottling();
            scheduleSaveUser(i);
            Slog.i(TAG, "ShortcutManager: throttling counter reset for user " + i);
        }
    }

    void resetAllThrottlingInner() {
        synchronized (this.mLock) {
            this.mRawLastResetTime = injectCurrentTimeMillis();
        }
        scheduleSaveBaseState();
        Slog.i(TAG, "ShortcutManager: throttling counter reset for all users");
    }

    public void onApplicationActive(String str, int i) {
        enforceResetThrottlingPermission();
        synchronized (this.mLock) {
            if (isUserUnlockedL(i)) {
                getPackageShortcutsLocked(str, i).resetRateLimitingForCommandLineNoSaving();
                saveUserLocked(i);
            }
        }
    }

    boolean hasShortcutHostPermission(String str, int i, int i2, int i3) {
        if (canSeeAnyPinnedShortcut(str, i, i2, i3)) {
            return true;
        }
        long statStartTime = getStatStartTime();
        try {
            return hasShortcutHostPermissionInner(str, i);
        } finally {
            logDurationStat(4, statStartTime);
        }
    }

    boolean canSeeAnyPinnedShortcut(String str, int i, int i2, int i3) {
        boolean zHasHostPackage;
        if (injectHasAccessShortcutsPermission(i2, i3)) {
            return true;
        }
        synchronized (this.mLock) {
            zHasHostPackage = getNonPersistentUserLocked(i).hasHostPackage(str);
        }
        return zHasHostPackage;
    }

    @VisibleForTesting
    boolean injectHasAccessShortcutsPermission(int i, int i2) {
        return this.mContext.checkPermission("android.permission.ACCESS_SHORTCUTS", i, i2) == 0;
    }

    @VisibleForTesting
    boolean injectHasUnlimitedShortcutsApiCallsPermission(int i, int i2) {
        return this.mContext.checkPermission("android.permission.UNLIMITED_SHORTCUTS_API_CALLS", i, i2) == 0;
    }

    @VisibleForTesting
    boolean hasShortcutHostPermissionInner(String str, int i) {
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
            ComponentName cachedLauncher = userShortcutsLocked.getCachedLauncher();
            if (cachedLauncher != null && cachedLauncher.getPackageName().equals(str)) {
                return true;
            }
            ComponentName defaultLauncher = getDefaultLauncher(i);
            userShortcutsLocked.setLauncher(defaultLauncher);
            if (defaultLauncher != null) {
                return defaultLauncher.getPackageName().equals(str);
            }
            return false;
        }
    }

    ComponentName getDefaultLauncher(int i) {
        ComponentName homeActivitiesAsUser;
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                throwIfUserLockedL(i);
                ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
                ArrayList arrayList = new ArrayList();
                long statStartTime2 = getStatStartTime();
                homeActivitiesAsUser = this.mPackageManagerInternal.getHomeActivitiesAsUser(arrayList, i);
                logDurationStat(0, statStartTime2);
                if (homeActivitiesAsUser == null && (homeActivitiesAsUser = userShortcutsLocked.getLastKnownLauncher()) != null && !injectIsActivityEnabledAndExported(homeActivitiesAsUser, i)) {
                    Slog.w(TAG, "Cached launcher " + homeActivitiesAsUser + " no longer exists");
                    homeActivitiesAsUser = null;
                    userShortcutsLocked.clearLauncher();
                }
                if (homeActivitiesAsUser == null) {
                    int size = arrayList.size();
                    int i2 = Integer.MIN_VALUE;
                    for (int i3 = 0; i3 < size; i3++) {
                        ResolveInfo resolveInfo = (ResolveInfo) arrayList.get(i3);
                        if (resolveInfo.activityInfo.applicationInfo.isSystemApp() && resolveInfo.priority >= i2) {
                            homeActivitiesAsUser = resolveInfo.activityInfo.getComponentName();
                            i2 = resolveInfo.priority;
                        }
                    }
                }
            }
            return homeActivitiesAsUser;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(16, statStartTime);
        }
    }

    public void setShortcutHostPackage(String str, String str2, int i) {
        synchronized (this.mLock) {
            getNonPersistentUserLocked(i).setShortcutHostPackage(str, str2);
        }
    }

    private void cleanUpPackageForAllLoadedUsers(final String str, final int i, final boolean z) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.cleanUpPackageLocked(str, ((ShortcutUser) obj).getUserId(), i, z);
                }
            });
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void cleanUpPackageLocked(final String str, int i, final int i2, boolean z) {
        boolean zIsUserLoadedLocked = isUserLoadedLocked(i);
        ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
        boolean z2 = i2 == i && userShortcutsLocked.removePackage(str) != null;
        userShortcutsLocked.removeLauncher(i2, str);
        userShortcutsLocked.forAllLaunchers(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ShortcutLauncher) obj).cleanUpPackage(str, i2);
            }
        });
        userShortcutsLocked.forAllPackages(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ShortcutPackage) obj).refreshPinnedFlags();
            }
        });
        scheduleSaveUser(i);
        if (z2) {
            notifyListeners(str, i);
        }
        if (z && i2 == i) {
            userShortcutsLocked.rescanPackageIfNeeded(str, true);
        }
        if (!zIsUserLoadedLocked) {
            unloadUserLocked(i);
        }
    }

    private class LocalService extends ShortcutServiceInternal {
        private LocalService() {
        }

        public List<ShortcutInfo> getShortcuts(final int i, final String str, final long j, String str2, List<String> list, final ComponentName componentName, final int i2, final int i3, final int i4, final int i5) {
            int i6;
            ArrayList<ShortcutInfo> arrayList;
            final ArrayList<ShortcutInfo> arrayList2 = new ArrayList<>();
            if (!((i2 & 4) != 0)) {
                i6 = 11;
            } else {
                i6 = 4;
            }
            final int i7 = i6;
            final List<String> list2 = str2 == null ? null : list;
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i3);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutService.this.getLauncherShortcutsLocked(str, i3, i).attemptToRestoreIfNeededAndSave();
                if (str2 != null) {
                    getShortcutsInnerLocked(i, str, str2, list2, j, componentName, i2, i3, arrayList2, i7, i4, i5);
                    arrayList = arrayList2;
                } else {
                    arrayList = arrayList2;
                    ShortcutService.this.getUserShortcutsLocked(i3).forAllPackages(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            this.f$0.getShortcutsInnerLocked(i, str, ((ShortcutPackage) obj).getPackageName(), list2, j, componentName, i2, i3, arrayList2, i7, i4, i5);
                        }
                    });
                }
            }
            return ShortcutService.this.setReturnedByServer(arrayList);
        }

        @GuardedBy("ShortcutService.this.mLock")
        private void getShortcutsInnerLocked(int i, String str, String str2, List<String> list, final long j, final ComponentName componentName, int i2, int i3, ArrayList<ShortcutInfo> arrayList, int i4, int i5, int i6) {
            final ArraySet arraySet;
            if (list != null) {
                arraySet = new ArraySet(list);
            } else {
                arraySet = null;
            }
            ShortcutPackage packageShortcutsIfExists = ShortcutService.this.getUserShortcutsLocked(i3).getPackageShortcutsIfExists(str2);
            if (packageShortcutsIfExists == null) {
                return;
            }
            final boolean z = (i2 & 1) != 0;
            final boolean z2 = (i2 & 2) != 0;
            final boolean z3 = (i2 & 8) != 0;
            boolean z4 = ShortcutService.this.canSeeAnyPinnedShortcut(str, i, i5, i6) && (i2 & 1024) != 0;
            final boolean z5 = z4;
            packageShortcutsIfExists.findAll(arrayList, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ShortcutService.LocalService.lambda$getShortcutsInnerLocked$1(j, arraySet, componentName, z, z2, z5, z3, (ShortcutInfo) obj);
                }
            }, i4, str, i, z4);
        }

        static boolean lambda$getShortcutsInnerLocked$1(long j, ArraySet arraySet, ComponentName componentName, boolean z, boolean z2, boolean z3, boolean z4, ShortcutInfo shortcutInfo) {
            if (shortcutInfo.getLastChangedTimestamp() < j) {
                return false;
            }
            if (arraySet != null && !arraySet.contains(shortcutInfo.getId())) {
                return false;
            }
            if (componentName != null && shortcutInfo.getActivity() != null && !shortcutInfo.getActivity().equals(componentName)) {
                return false;
            }
            if (z && shortcutInfo.isDynamic()) {
                return true;
            }
            if ((z2 || z3) && shortcutInfo.isPinned()) {
                return true;
            }
            return z4 && shortcutInfo.isDeclaredInManifest();
        }

        public boolean isPinnedByCaller(int i, String str, String str2, String str3, int i2) {
            boolean z;
            Preconditions.checkStringNotEmpty(str2, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
            Preconditions.checkStringNotEmpty(str3, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i2);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                ShortcutInfo shortcutInfoLocked = getShortcutInfoLocked(i, str, str2, str3, i2, false);
                z = shortcutInfoLocked != null && shortcutInfoLocked.isPinned();
            }
            return z;
        }

        @GuardedBy("ShortcutService.this.mLock")
        private ShortcutInfo getShortcutInfoLocked(int i, String str, String str2, final String str3, int i2, boolean z) {
            Preconditions.checkStringNotEmpty(str2, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
            Preconditions.checkStringNotEmpty(str3, "shortcutId");
            ShortcutService.this.throwIfUserLockedL(i2);
            ShortcutService.this.throwIfUserLockedL(i);
            ShortcutPackage packageShortcutsIfExists = ShortcutService.this.getUserShortcutsLocked(i2).getPackageShortcutsIfExists(str2);
            if (packageShortcutsIfExists == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList(1);
            packageShortcutsIfExists.findAll(arrayList, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return str3.equals(((ShortcutInfo) obj).getId());
                }
            }, 0, str, i, z);
            if (arrayList.size() == 0) {
                return null;
            }
            return (ShortcutInfo) arrayList.get(0);
        }

        public void pinShortcuts(int i, String str, String str2, List<String> list, int i2) {
            Preconditions.checkStringNotEmpty(str2, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
            Preconditions.checkNotNull(list, "shortcutIds");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i2);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutLauncher launcherShortcutsLocked = ShortcutService.this.getLauncherShortcutsLocked(str, i2, i);
                launcherShortcutsLocked.attemptToRestoreIfNeededAndSave();
                launcherShortcutsLocked.pinShortcuts(i2, str2, list, false);
            }
            ShortcutService.this.packageShortcutsChanged(str2, i2);
            ShortcutService.this.verifyStates();
        }

        public Intent[] createShortcutIntents(int i, String str, String str2, String str3, int i2, int i3, int i4) {
            Preconditions.checkStringNotEmpty(str2, "packageName can't be empty");
            Preconditions.checkStringNotEmpty(str3, "shortcutId can't be empty");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i2);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                boolean zCanSeeAnyPinnedShortcut = ShortcutService.this.canSeeAnyPinnedShortcut(str, i, i3, i4);
                ShortcutInfo shortcutInfoLocked = getShortcutInfoLocked(i, str, str2, str3, i2, zCanSeeAnyPinnedShortcut);
                if (shortcutInfoLocked != null && shortcutInfoLocked.isEnabled() && (shortcutInfoLocked.isAlive() || zCanSeeAnyPinnedShortcut)) {
                    return shortcutInfoLocked.getIntents();
                }
                Log.e(ShortcutService.TAG, "Shortcut " + str3 + " does not exist or disabled");
                return null;
            }
        }

        public void addListener(ShortcutServiceInternal.ShortcutChangeListener shortcutChangeListener) {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.mListeners.add((ShortcutServiceInternal.ShortcutChangeListener) Preconditions.checkNotNull(shortcutChangeListener));
            }
        }

        public int getShortcutIconResId(int i, String str, String str2, String str3, int i2) {
            Preconditions.checkNotNull(str, "callingPackage");
            Preconditions.checkNotNull(str2, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
            Preconditions.checkNotNull(str3, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i2);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                ShortcutPackage packageShortcutsIfExists = ShortcutService.this.getUserShortcutsLocked(i2).getPackageShortcutsIfExists(str2);
                int iconResourceId = 0;
                if (packageShortcutsIfExists == null) {
                    return 0;
                }
                ShortcutInfo shortcutInfoFindShortcutById = packageShortcutsIfExists.findShortcutById(str3);
                if (shortcutInfoFindShortcutById != null && shortcutInfoFindShortcutById.hasIconResource()) {
                    iconResourceId = shortcutInfoFindShortcutById.getIconResourceId();
                }
                return iconResourceId;
            }
        }

        public ParcelFileDescriptor getShortcutIconFd(int i, String str, String str2, String str3, int i2) {
            Preconditions.checkNotNull(str, "callingPackage");
            Preconditions.checkNotNull(str2, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
            Preconditions.checkNotNull(str3, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(i2);
                ShortcutService.this.throwIfUserLockedL(i);
                ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                ShortcutPackage packageShortcutsIfExists = ShortcutService.this.getUserShortcutsLocked(i2).getPackageShortcutsIfExists(str2);
                if (packageShortcutsIfExists == null) {
                    return null;
                }
                ShortcutInfo shortcutInfoFindShortcutById = packageShortcutsIfExists.findShortcutById(str3);
                if (shortcutInfoFindShortcutById != null && shortcutInfoFindShortcutById.hasIconFile()) {
                    String bitmapPathMayWaitLocked = ShortcutService.this.mShortcutBitmapSaver.getBitmapPathMayWaitLocked(shortcutInfoFindShortcutById);
                    if (bitmapPathMayWaitLocked == null) {
                        Slog.w(ShortcutService.TAG, "null bitmap detected in getShortcutIconFd()");
                        return null;
                    }
                    try {
                        return ParcelFileDescriptor.open(new File(bitmapPathMayWaitLocked), 268435456);
                    } catch (FileNotFoundException e) {
                        Slog.e(ShortcutService.TAG, "Icon file not found: " + bitmapPathMayWaitLocked);
                        return null;
                    }
                }
                return null;
            }
        }

        public boolean hasShortcutHostPermission(int i, String str, int i2, int i3) {
            return ShortcutService.this.hasShortcutHostPermission(str, i, i2, i3);
        }

        public void setShortcutHostPackage(String str, String str2, int i) {
            ShortcutService.this.setShortcutHostPackage(str, str2, i);
        }

        public boolean requestPinAppWidget(String str, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle, IntentSender intentSender, int i) {
            Preconditions.checkNotNull(appWidgetProviderInfo);
            return ShortcutService.this.requestPinItem(str, i, null, appWidgetProviderInfo, bundle, intentSender);
        }

        public boolean isRequestPinItemSupported(int i, int i2) {
            return ShortcutService.this.isRequestPinItemSupported(i, i2);
        }

        public boolean isForegroundDefaultLauncher(String str, int i) {
            Preconditions.checkNotNull(str);
            ComponentName defaultLauncher = ShortcutService.this.getDefaultLauncher(UserHandle.getUserId(i));
            if (defaultLauncher != null && str.equals(defaultLauncher.getPackageName())) {
                synchronized (ShortcutService.this.mLock) {
                    if (!ShortcutService.this.isUidForegroundLocked(i)) {
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    void handleLocaleChanged() {
        scheduleSaveBaseState();
        synchronized (this.mLock) {
            long jInjectClearCallingIdentity = injectClearCallingIdentity();
            try {
                forEachLoadedUserLocked(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ShortcutUser) obj).detectLocaleChange();
                    }
                });
            } finally {
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            }
        }
    }

    @VisibleForTesting
    void checkPackageChanges(int i) {
        if (injectIsSafeModeEnabled()) {
            Slog.i(TAG, "Safe mode, skipping checkPackageChanges()");
            return;
        }
        long statStartTime = getStatStartTime();
        try {
            final ArrayList arrayList = new ArrayList();
            synchronized (this.mLock) {
                ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
                userShortcutsLocked.forAllPackageItems(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ShortcutService.lambda$checkPackageChanges$6(this.f$0, arrayList, (ShortcutPackageItem) obj);
                    }
                });
                if (arrayList.size() > 0) {
                    for (int size = arrayList.size() - 1; size >= 0; size--) {
                        ShortcutUser.PackageWithUser packageWithUser = (ShortcutUser.PackageWithUser) arrayList.get(size);
                        cleanUpPackageLocked(packageWithUser.packageName, i, packageWithUser.userId, false);
                    }
                }
                rescanUpdatedPackagesLocked(i, userShortcutsLocked.getLastAppScanTime());
            }
            logDurationStat(8, statStartTime);
            verifyStates();
        } catch (Throwable th) {
            logDurationStat(8, statStartTime);
            throw th;
        }
    }

    public static void lambda$checkPackageChanges$6(ShortcutService shortcutService, ArrayList arrayList, ShortcutPackageItem shortcutPackageItem) {
        if (!shortcutPackageItem.getPackageInfo().isShadow() && !shortcutService.isPackageInstalled(shortcutPackageItem.getPackageName(), shortcutPackageItem.getPackageUserId())) {
            arrayList.add(ShortcutUser.PackageWithUser.of(shortcutPackageItem));
        }
    }

    @GuardedBy("mLock")
    private void rescanUpdatedPackagesLocked(final int i, long j) {
        final ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
        long jInjectCurrentTimeMillis = injectCurrentTimeMillis();
        forUpdatedPackages(i, j, !injectBuildFingerprint().equals(userShortcutsLocked.getLastAppScanOsFingerprint()), new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutService.lambda$rescanUpdatedPackagesLocked$7(this.f$0, userShortcutsLocked, i, (ApplicationInfo) obj);
            }
        });
        userShortcutsLocked.setLastAppScanTime(jInjectCurrentTimeMillis);
        userShortcutsLocked.setLastAppScanOsFingerprint(injectBuildFingerprint());
        scheduleSaveUser(i);
    }

    public static void lambda$rescanUpdatedPackagesLocked$7(ShortcutService shortcutService, ShortcutUser shortcutUser, int i, ApplicationInfo applicationInfo) {
        shortcutUser.attemptToRestoreIfNeededAndSave(shortcutService, applicationInfo.packageName, i);
        shortcutUser.rescanPackageIfNeeded(applicationInfo.packageName, true);
    }

    private void handlePackageAdded(String str, int i) {
        synchronized (this.mLock) {
            ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
            userShortcutsLocked.attemptToRestoreIfNeededAndSave(this, str, i);
            userShortcutsLocked.rescanPackageIfNeeded(str, true);
        }
        verifyStates();
    }

    private void handlePackageUpdateFinished(String str, int i) {
        synchronized (this.mLock) {
            ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
            userShortcutsLocked.attemptToRestoreIfNeededAndSave(this, str, i);
            if (isPackageInstalled(str, i)) {
                userShortcutsLocked.rescanPackageIfNeeded(str, true);
            }
        }
        verifyStates();
    }

    private void handlePackageRemoved(String str, int i) {
        cleanUpPackageForAllLoadedUsers(str, i, false);
        verifyStates();
    }

    private void handlePackageDataCleared(String str, int i) {
        cleanUpPackageForAllLoadedUsers(str, i, true);
        verifyStates();
    }

    private void handlePackageChanged(String str, int i) {
        if (!isPackageInstalled(str, i)) {
            handlePackageRemoved(str, i);
            return;
        }
        synchronized (this.mLock) {
            getUserShortcutsLocked(i).rescanPackageIfNeeded(str, true);
        }
        verifyStates();
    }

    final PackageInfo getPackageInfoWithSignatures(String str, int i) {
        return getPackageInfo(str, i, true);
    }

    final PackageInfo getPackageInfo(String str, int i) {
        return getPackageInfo(str, i, false);
    }

    int injectGetPackageUid(String str, int i) {
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getPackageUid(str, PACKAGE_MATCH_FLAGS, i);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return -1;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    @VisibleForTesting
    final PackageInfo getPackageInfo(String str, int i, boolean z) {
        return isInstalledOrNull(injectPackageInfoWithUninstalled(str, i, z));
    }

    @VisibleForTesting
    PackageInfo injectPackageInfoWithUninstalled(String str, int i, boolean z) {
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            try {
                PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(str, PACKAGE_MATCH_FLAGS | (z ? 134217728 : 0), i);
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                logDurationStat(z ? 2 : 1, statStartTime);
                return packageInfo;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "RemoteException", e);
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                logDurationStat(z ? 2 : 1, statStartTime);
                return null;
            }
        } catch (Throwable th) {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(z ? 2 : 1, statStartTime);
            throw th;
        }
    }

    @VisibleForTesting
    final ApplicationInfo getApplicationInfo(String str, int i) {
        return isInstalledOrNull(injectApplicationInfoWithUninstalled(str, i));
    }

    @VisibleForTesting
    ApplicationInfo injectApplicationInfoWithUninstalled(String str, int i) {
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getApplicationInfo(str, PACKAGE_MATCH_FLAGS, i);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(3, statStartTime);
        }
    }

    final ActivityInfo getActivityInfoWithMetadata(ComponentName componentName, int i) {
        return isInstalledOrNull(injectGetActivityInfoWithMetadataWithUninstalled(componentName, i));
    }

    @VisibleForTesting
    ActivityInfo injectGetActivityInfoWithMetadataWithUninstalled(ComponentName componentName, int i) {
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getActivityInfo(componentName, 794752, i);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(6, statStartTime);
        }
    }

    @VisibleForTesting
    final List<PackageInfo> getInstalledPackages(int i) {
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            List<PackageInfo> listInjectGetPackagesWithUninstalled = injectGetPackagesWithUninstalled(i);
            listInjectGetPackagesWithUninstalled.removeIf(PACKAGE_NOT_INSTALLED);
            return listInjectGetPackagesWithUninstalled;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(7, statStartTime);
        }
    }

    @VisibleForTesting
    List<PackageInfo> injectGetPackagesWithUninstalled(int i) throws RemoteException {
        ParceledListSlice installedPackages = this.mIPackageManager.getInstalledPackages(PACKAGE_MATCH_FLAGS, i);
        if (installedPackages == null) {
            return Collections.emptyList();
        }
        return installedPackages.getList();
    }

    private void forUpdatedPackages(int i, long j, boolean z, Consumer<ApplicationInfo> consumer) {
        List<PackageInfo> installedPackages = getInstalledPackages(i);
        for (int size = installedPackages.size() - 1; size >= 0; size--) {
            PackageInfo packageInfo = installedPackages.get(size);
            if (z || packageInfo.lastUpdateTime >= j) {
                consumer.accept(packageInfo.applicationInfo);
            }
        }
    }

    private boolean isApplicationFlagSet(String str, int i, int i2) {
        ApplicationInfo applicationInfoInjectApplicationInfoWithUninstalled = injectApplicationInfoWithUninstalled(str, i);
        return applicationInfoInjectApplicationInfoWithUninstalled != null && (applicationInfoInjectApplicationInfoWithUninstalled.flags & i2) == i2;
    }

    private static boolean isInstalled(ApplicationInfo applicationInfo) {
        return (applicationInfo == null || !applicationInfo.enabled || (applicationInfo.flags & DumpState.DUMP_VOLUMES) == 0) ? false : true;
    }

    private static boolean isEphemeralApp(ApplicationInfo applicationInfo) {
        return applicationInfo != null && applicationInfo.isInstantApp();
    }

    private static boolean isInstalled(PackageInfo packageInfo) {
        return packageInfo != null && isInstalled(packageInfo.applicationInfo);
    }

    private static boolean isInstalled(ActivityInfo activityInfo) {
        return activityInfo != null && isInstalled(activityInfo.applicationInfo);
    }

    private static ApplicationInfo isInstalledOrNull(ApplicationInfo applicationInfo) {
        if (isInstalled(applicationInfo)) {
            return applicationInfo;
        }
        return null;
    }

    private static PackageInfo isInstalledOrNull(PackageInfo packageInfo) {
        if (isInstalled(packageInfo)) {
            return packageInfo;
        }
        return null;
    }

    private static ActivityInfo isInstalledOrNull(ActivityInfo activityInfo) {
        if (isInstalled(activityInfo)) {
            return activityInfo;
        }
        return null;
    }

    boolean isPackageInstalled(String str, int i) {
        return getApplicationInfo(str, i) != null;
    }

    boolean isEphemeralApp(String str, int i) {
        return isEphemeralApp(getApplicationInfo(str, i));
    }

    XmlResourceParser injectXmlMetaData(ActivityInfo activityInfo, String str) {
        return activityInfo.loadXmlMetaData(this.mContext.getPackageManager(), str);
    }

    Resources injectGetResourcesForApplicationAsUser(String str, int i) {
        long statStartTime = getStatStartTime();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return this.mContext.getPackageManager().getResourcesForApplicationAsUser(str, i);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Resources for package " + str + " not found");
            return null;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            logDurationStat(9, statStartTime);
        }
    }

    private Intent getMainActivityIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(LAUNCHER_INTENT_CATEGORY);
        return intent;
    }

    @VisibleForTesting
    List<ResolveInfo> queryActivities(Intent intent, String str, ComponentName componentName, int i) {
        intent.setPackage((String) Preconditions.checkNotNull(str));
        if (componentName != null) {
            intent.setComponent(componentName);
        }
        return queryActivities(intent, i, true);
    }

    List<ResolveInfo> queryActivities(Intent intent, int i, boolean z) {
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            List<ResolveInfo> listQueryIntentActivitiesAsUser = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, PACKAGE_MATCH_FLAGS, i);
            if (listQueryIntentActivitiesAsUser == null || listQueryIntentActivitiesAsUser.size() == 0) {
                return EMPTY_RESOLVE_INFO;
            }
            if (!isInstalled(listQueryIntentActivitiesAsUser.get(0).activityInfo)) {
                return EMPTY_RESOLVE_INFO;
            }
            if (z) {
                listQueryIntentActivitiesAsUser.removeIf(ACTIVITY_NOT_EXPORTED);
            }
            return listQueryIntentActivitiesAsUser;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    ComponentName injectGetDefaultMainActivity(String str, int i) {
        long statStartTime = getStatStartTime();
        try {
            ComponentName componentName = null;
            List<ResolveInfo> listQueryActivities = queryActivities(getMainActivityIntent(), str, null, i);
            if (listQueryActivities.size() != 0) {
                componentName = listQueryActivities.get(0).activityInfo.getComponentName();
            }
            return componentName;
        } finally {
            logDurationStat(11, statStartTime);
        }
    }

    boolean injectIsMainActivity(ComponentName componentName, int i) {
        long statStartTime = getStatStartTime();
        try {
            if (componentName == null) {
                wtf("null activity detected");
                return false;
            }
            if (DUMMY_MAIN_ACTIVITY.equals(componentName.getClassName())) {
                return true;
            }
            return queryActivities(getMainActivityIntent(), componentName.getPackageName(), componentName, i).size() > 0;
        } finally {
            logDurationStat(12, statStartTime);
        }
    }

    ComponentName getDummyMainActivity(String str) {
        return new ComponentName(str, DUMMY_MAIN_ACTIVITY);
    }

    boolean isDummyMainActivity(ComponentName componentName) {
        return componentName != null && DUMMY_MAIN_ACTIVITY.equals(componentName.getClassName());
    }

    List<ResolveInfo> injectGetMainActivities(String str, int i) {
        long statStartTime = getStatStartTime();
        try {
            return queryActivities(getMainActivityIntent(), str, null, i);
        } finally {
            logDurationStat(12, statStartTime);
        }
    }

    @VisibleForTesting
    boolean injectIsActivityEnabledAndExported(ComponentName componentName, int i) {
        long statStartTime = getStatStartTime();
        try {
            return queryActivities(new Intent(), componentName.getPackageName(), componentName, i).size() > 0;
        } finally {
            logDurationStat(13, statStartTime);
        }
    }

    ComponentName injectGetPinConfirmationActivity(String str, int i, int i2) {
        String str2;
        Preconditions.checkNotNull(str);
        if (i2 == 1) {
            str2 = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
        } else {
            str2 = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
        }
        Iterator<ResolveInfo> it = queryActivities(new Intent(str2).setPackage(str), i, false).iterator();
        if (it.hasNext()) {
            return it.next().activityInfo.getComponentName();
        }
        return null;
    }

    boolean injectIsSafeModeEnabled() {
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window")).isSafeModeEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    int getParentOrSelfUserId(int i) {
        return this.mUserManagerInternal.getProfileParentId(i);
    }

    void injectSendIntentSender(IntentSender intentSender, Intent intent) {
        if (intentSender == null) {
            return;
        }
        try {
            intentSender.sendIntent(this.mContext, 0, intent, null, null);
        } catch (IntentSender.SendIntentException e) {
            Slog.w(TAG, "sendIntent failed().", e);
        }
    }

    boolean shouldBackupApp(String str, int i) {
        return isApplicationFlagSet(str, i, 32768);
    }

    static boolean shouldBackupApp(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo.flags & 32768) != 0;
    }

    public byte[] getBackupPayload(int i) {
        enforceSystem();
        synchronized (this.mLock) {
            if (!isUserUnlockedL(i)) {
                wtf("Can't backup: user " + i + " is locked or not running");
                return null;
            }
            ShortcutUser userShortcutsLocked = getUserShortcutsLocked(i);
            if (userShortcutsLocked == null) {
                wtf("Can't backup: user not found: id=" + i);
                return null;
            }
            userShortcutsLocked.forAllPackageItems(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((ShortcutPackageItem) obj).refreshPackageSignatureAndSave();
                }
            });
            userShortcutsLocked.forAllPackages(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((ShortcutPackage) obj).rescanPackageIfNeeded(false, true);
                }
            });
            userShortcutsLocked.forAllLaunchers(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((ShortcutLauncher) obj).ensurePackageInfo();
                }
            });
            scheduleSaveUser(i);
            saveDirtyInfo();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(32768);
            try {
                saveUserInternalLocked(i, byteArrayOutputStream, true);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                this.mShortcutDumpFiles.save("backup-1-payload.txt", byteArray);
                return byteArray;
            } catch (IOException | XmlPullParserException e) {
                Slog.w(TAG, "Backup failed.", e);
                return null;
            }
        }
    }

    public void applyRestore(byte[] bArr, int i) {
        enforceSystem();
        synchronized (this.mLock) {
            if (!isUserUnlockedL(i)) {
                wtf("Can't restore: user " + i + " is locked or not running");
                return;
            }
            this.mShortcutDumpFiles.save("restore-0-start.txt", new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ShortcutService.lambda$applyRestore$11(this.f$0, (PrintWriter) obj);
                }
            });
            this.mShortcutDumpFiles.save("restore-1-payload.xml", bArr);
            try {
                ShortcutUser shortcutUserLoadUserInternal = loadUserInternal(i, new ByteArrayInputStream(bArr), true);
                this.mShortcutDumpFiles.save("restore-2.txt", new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        this.f$0.dumpInner((PrintWriter) obj);
                    }
                });
                getUserShortcutsLocked(i).mergeRestoredFile(shortcutUserLoadUserInternal);
                this.mShortcutDumpFiles.save("restore-3.txt", new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        this.f$0.dumpInner((PrintWriter) obj);
                    }
                });
                rescanUpdatedPackagesLocked(i, 0L);
                this.mShortcutDumpFiles.save("restore-4.txt", new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        this.f$0.dumpInner((PrintWriter) obj);
                    }
                });
                this.mShortcutDumpFiles.save("restore-5-finish.txt", new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ShortcutService.lambda$applyRestore$12(this.f$0, (PrintWriter) obj);
                    }
                });
                saveUserLocked(i);
            } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                Slog.w(TAG, "Restoration failed.", e);
            }
        }
    }

    public static void lambda$applyRestore$11(ShortcutService shortcutService, PrintWriter printWriter) {
        printWriter.print("Start time: ");
        shortcutService.dumpCurrentTime(printWriter);
        printWriter.println();
    }

    public static void lambda$applyRestore$12(ShortcutService shortcutService, PrintWriter printWriter) {
        printWriter.print("Finish time: ");
        shortcutService.dumpCurrentTime(printWriter);
        printWriter.println();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            dumpNoCheck(fileDescriptor, printWriter, strArr);
        }
    }

    @VisibleForTesting
    void dumpNoCheck(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        DumpFilter dumpArgs = parseDumpArgs(strArr);
        if (dumpArgs.shouldDumpCheckIn()) {
            dumpCheckin(printWriter, dumpArgs.shouldCheckInClear());
            return;
        }
        if (dumpArgs.shouldDumpMain()) {
            dumpInner(printWriter, dumpArgs);
            printWriter.println();
        }
        if (dumpArgs.shouldDumpUid()) {
            dumpUid(printWriter);
            printWriter.println();
        }
        if (dumpArgs.shouldDumpFiles()) {
            dumpDumpFiles(printWriter);
            printWriter.println();
        }
    }

    private static DumpFilter parseDumpArgs(String[] strArr) {
        DumpFilter dumpFilter = new DumpFilter();
        if (strArr == null) {
            return dumpFilter;
        }
        int i = 0;
        while (true) {
            if (i >= strArr.length) {
                break;
            }
            int i2 = i + 1;
            String str = strArr[i];
            if ("-c".equals(str)) {
                dumpFilter.setDumpCheckIn(true);
            } else if ("--checkin".equals(str)) {
                dumpFilter.setDumpCheckIn(true);
                dumpFilter.setCheckInClear(true);
            } else if ("-a".equals(str) || "--all".equals(str)) {
                dumpFilter.setDumpUid(true);
                dumpFilter.setDumpFiles(true);
            } else if ("-u".equals(str) || "--uid".equals(str)) {
                dumpFilter.setDumpUid(true);
            } else if ("-f".equals(str) || "--files".equals(str)) {
                dumpFilter.setDumpFiles(true);
            } else if ("-n".equals(str) || "--no-main".equals(str)) {
                dumpFilter.setDumpMain(false);
            } else if ("--user".equals(str)) {
                if (i2 >= strArr.length) {
                    throw new IllegalArgumentException("Missing user ID for --user");
                }
                i = i2 + 1;
                try {
                    dumpFilter.addUser(Integer.parseInt(strArr[i2]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid user ID", e);
                }
            } else if ("-p".equals(str) || "--package".equals(str)) {
                if (i2 >= strArr.length) {
                    throw new IllegalArgumentException("Missing package name for --package");
                }
                i = i2 + 1;
                dumpFilter.addPackageRegex(strArr[i2]);
                dumpFilter.setDumpDetails(false);
            } else {
                if (str.startsWith("-")) {
                    throw new IllegalArgumentException("Unknown option " + str);
                }
                i = i2;
            }
            i = i2;
        }
    }

    static class DumpFilter {
        private boolean mDumpCheckIn = false;
        private boolean mCheckInClear = false;
        private boolean mDumpMain = true;
        private boolean mDumpUid = false;
        private boolean mDumpFiles = false;
        private boolean mDumpDetails = true;
        private List<Pattern> mPackagePatterns = new ArrayList();
        private List<Integer> mUsers = new ArrayList();

        DumpFilter() {
        }

        void addPackageRegex(String str) {
            this.mPackagePatterns.add(Pattern.compile(str));
        }

        public void addPackage(String str) {
            addPackageRegex(Pattern.quote(str));
        }

        void addUser(int i) {
            this.mUsers.add(Integer.valueOf(i));
        }

        boolean isPackageMatch(String str) {
            if (this.mPackagePatterns.size() == 0) {
                return true;
            }
            for (int i = 0; i < this.mPackagePatterns.size(); i++) {
                if (this.mPackagePatterns.get(i).matcher(str).find()) {
                    return true;
                }
            }
            return false;
        }

        boolean isUserMatch(int i) {
            if (this.mUsers.size() == 0) {
                return true;
            }
            for (int i2 = 0; i2 < this.mUsers.size(); i2++) {
                if (this.mUsers.get(i2).intValue() == i) {
                    return true;
                }
            }
            return false;
        }

        public boolean shouldDumpCheckIn() {
            return this.mDumpCheckIn;
        }

        public void setDumpCheckIn(boolean z) {
            this.mDumpCheckIn = z;
        }

        public boolean shouldCheckInClear() {
            return this.mCheckInClear;
        }

        public void setCheckInClear(boolean z) {
            this.mCheckInClear = z;
        }

        public boolean shouldDumpMain() {
            return this.mDumpMain;
        }

        public void setDumpMain(boolean z) {
            this.mDumpMain = z;
        }

        public boolean shouldDumpUid() {
            return this.mDumpUid;
        }

        public void setDumpUid(boolean z) {
            this.mDumpUid = z;
        }

        public boolean shouldDumpFiles() {
            return this.mDumpFiles;
        }

        public void setDumpFiles(boolean z) {
            this.mDumpFiles = z;
        }

        public boolean shouldDumpDetails() {
            return this.mDumpDetails;
        }

        public void setDumpDetails(boolean z) {
            this.mDumpDetails = z;
        }
    }

    private void dumpInner(PrintWriter printWriter) {
        dumpInner(printWriter, new DumpFilter());
    }

    private void dumpInner(PrintWriter printWriter, DumpFilter dumpFilter) {
        synchronized (this.mLock) {
            if (dumpFilter.shouldDumpDetails()) {
                long jInjectCurrentTimeMillis = injectCurrentTimeMillis();
                printWriter.print("Now: [");
                printWriter.print(jInjectCurrentTimeMillis);
                printWriter.print("] ");
                printWriter.print(formatTime(jInjectCurrentTimeMillis));
                printWriter.print("  Raw last reset: [");
                printWriter.print(this.mRawLastResetTime);
                printWriter.print("] ");
                printWriter.print(formatTime(this.mRawLastResetTime));
                long lastResetTimeLocked = getLastResetTimeLocked();
                printWriter.print("  Last reset: [");
                printWriter.print(lastResetTimeLocked);
                printWriter.print("] ");
                printWriter.print(formatTime(lastResetTimeLocked));
                long nextResetTimeLocked = getNextResetTimeLocked();
                printWriter.print("  Next reset: [");
                printWriter.print(nextResetTimeLocked);
                printWriter.print("] ");
                printWriter.print(formatTime(nextResetTimeLocked));
                printWriter.println();
                printWriter.println();
                printWriter.print("  Config:");
                printWriter.print("    Max icon dim: ");
                printWriter.println(this.mMaxIconDimension);
                printWriter.print("    Icon format: ");
                printWriter.println(this.mIconPersistFormat);
                printWriter.print("    Icon quality: ");
                printWriter.println(this.mIconPersistQuality);
                printWriter.print("    saveDelayMillis: ");
                printWriter.println(this.mSaveDelayMillis);
                printWriter.print("    resetInterval: ");
                printWriter.println(this.mResetInterval);
                printWriter.print("    maxUpdatesPerInterval: ");
                printWriter.println(this.mMaxUpdatesPerInterval);
                printWriter.print("    maxShortcutsPerActivity: ");
                printWriter.println(this.mMaxShortcuts);
                printWriter.println();
                this.mStatLogger.dump(printWriter, "  ");
                printWriter.println();
                printWriter.print("  #Failures: ");
                printWriter.println(this.mWtfCount);
                if (this.mLastWtfStacktrace != null) {
                    printWriter.print("  Last failure stack trace: ");
                    printWriter.println(Log.getStackTraceString(this.mLastWtfStacktrace));
                }
                printWriter.println();
                this.mShortcutBitmapSaver.dumpLocked(printWriter, "  ");
                printWriter.println();
            }
            for (int i = 0; i < this.mUsers.size(); i++) {
                ShortcutUser shortcutUserValueAt = this.mUsers.valueAt(i);
                if (dumpFilter.isUserMatch(shortcutUserValueAt.getUserId())) {
                    shortcutUserValueAt.dump(printWriter, "  ", dumpFilter);
                    printWriter.println();
                }
            }
            for (int i2 = 0; i2 < this.mShortcutNonPersistentUsers.size(); i2++) {
                ShortcutNonPersistentUser shortcutNonPersistentUserValueAt = this.mShortcutNonPersistentUsers.valueAt(i2);
                if (dumpFilter.isUserMatch(shortcutNonPersistentUserValueAt.getUserId())) {
                    shortcutNonPersistentUserValueAt.dump(printWriter, "  ", dumpFilter);
                    printWriter.println();
                }
            }
        }
    }

    private void dumpUid(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println("** SHORTCUT MANAGER UID STATES (dumpsys shortcut -n -u)");
            for (int i = 0; i < this.mUidState.size(); i++) {
                int iKeyAt = this.mUidState.keyAt(i);
                int iValueAt = this.mUidState.valueAt(i);
                printWriter.print("    UID=");
                printWriter.print(iKeyAt);
                printWriter.print(" state=");
                printWriter.print(iValueAt);
                if (isProcessStateForeground(iValueAt)) {
                    printWriter.print("  [FG]");
                }
                printWriter.print("  last FG=");
                printWriter.print(this.mUidLastForegroundElapsedTime.get(iKeyAt));
                printWriter.println();
            }
        }
    }

    static String formatTime(long j) {
        Time time = new Time();
        time.set(j);
        return time.format("%Y-%m-%d %H:%M:%S");
    }

    private void dumpCurrentTime(PrintWriter printWriter) {
        printWriter.print(formatTime(injectCurrentTimeMillis()));
    }

    private void dumpCheckin(PrintWriter printWriter, boolean z) {
        synchronized (this.mLock) {
            try {
                JSONArray jSONArray = new JSONArray();
                for (int i = 0; i < this.mUsers.size(); i++) {
                    jSONArray.put(this.mUsers.valueAt(i).dumpCheckin(z));
                }
                JSONObject jSONObject = new JSONObject();
                jSONObject.put(KEY_SHORTCUT, jSONArray);
                jSONObject.put(KEY_LOW_RAM, injectIsLowRamDevice());
                jSONObject.put(KEY_ICON_SIZE, this.mMaxIconDimension);
                printWriter.println(jSONObject.toString(1));
            } catch (JSONException e) {
                Slog.e(TAG, "Unable to write in json", e);
            }
        }
    }

    private void dumpDumpFiles(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println("** SHORTCUT MANAGER FILES (dumpsys shortcut -n -f)");
            this.mShortcutDumpFiles.dumpAll(printWriter);
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        enforceShell();
        long jInjectClearCallingIdentity = injectClearCallingIdentity();
        try {
            resultReceiver.send(new MyShellCommand().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver), null);
        } finally {
            injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    static class CommandException extends Exception {
        public CommandException(String str) {
            super(str);
        }
    }

    private class MyShellCommand extends ShellCommand {
        private int mUserId;

        private MyShellCommand() {
            this.mUserId = 0;
        }

        private void parseOptionsLocked(boolean z) throws CommandException {
            do {
                String nextOption = getNextOption();
                if (nextOption != null) {
                    byte b = -1;
                    if (nextOption.hashCode() == 1333469547 && nextOption.equals("--user")) {
                        b = 0;
                    }
                    if (b == 0 && z) {
                        this.mUserId = UserHandle.parseUserArg(getNextArgRequired());
                    } else {
                        throw new CommandException("Unknown option: " + nextOption);
                    }
                } else {
                    return;
                }
            } while (ShortcutService.this.isUserUnlockedL(this.mUserId));
            throw new CommandException("User " + this.mUserId + " is not running or locked");
        }

        public int onCommand(String str) {
            if (str == null) {
                return handleDefaultCommands(str);
            }
            PrintWriter outPrintWriter = getOutPrintWriter();
            try {
                switch (str) {
                    case "reset-throttling":
                        handleResetThrottling();
                        break;
                    case "reset-all-throttling":
                        handleResetAllThrottling();
                        break;
                    case "override-config":
                        handleOverrideConfig();
                        break;
                    case "reset-config":
                        handleResetConfig();
                        break;
                    case "clear-default-launcher":
                        handleClearDefaultLauncher();
                        break;
                    case "get-default-launcher":
                        handleGetDefaultLauncher();
                        break;
                    case "unload-user":
                        handleUnloadUser();
                        break;
                    case "clear-shortcuts":
                        handleClearShortcuts();
                        break;
                    case "verify-states":
                        handleVerifyStates();
                        break;
                    default:
                        return handleDefaultCommands(str);
                }
                outPrintWriter.println("Success");
                return 0;
            } catch (CommandException e) {
                outPrintWriter.println("Error: " + e.getMessage());
                return 1;
            }
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Usage: cmd shortcut COMMAND [options ...]");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut reset-throttling [--user USER_ID]");
            outPrintWriter.println("    Reset throttling for all packages and users");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut reset-all-throttling");
            outPrintWriter.println("    Reset the throttling state for all users");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut override-config CONFIG");
            outPrintWriter.println("    Override the configuration for testing (will last until reboot)");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut reset-config");
            outPrintWriter.println("    Reset the configuration set with \"update-config\"");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut clear-default-launcher [--user USER_ID]");
            outPrintWriter.println("    Clear the cached default launcher");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut get-default-launcher [--user USER_ID]");
            outPrintWriter.println("    Show the default launcher");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut unload-user [--user USER_ID]");
            outPrintWriter.println("    Unload a user from the memory");
            outPrintWriter.println("    (This should not affect any observable behavior)");
            outPrintWriter.println();
            outPrintWriter.println("cmd shortcut clear-shortcuts [--user USER_ID] PACKAGE");
            outPrintWriter.println("    Remove all shortcuts from a package, including pinned shortcuts");
            outPrintWriter.println();
        }

        private void handleResetThrottling() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                Slog.i(ShortcutService.TAG, "cmd: handleResetThrottling: user=" + this.mUserId);
                ShortcutService.this.resetThrottlingInner(this.mUserId);
            }
        }

        private void handleResetAllThrottling() {
            Slog.i(ShortcutService.TAG, "cmd: handleResetAllThrottling");
            ShortcutService.this.resetAllThrottlingInner();
        }

        private void handleOverrideConfig() throws CommandException {
            String nextArgRequired = getNextArgRequired();
            Slog.i(ShortcutService.TAG, "cmd: handleOverrideConfig: " + nextArgRequired);
            synchronized (ShortcutService.this.mLock) {
                if (!ShortcutService.this.updateConfigurationLocked(nextArgRequired)) {
                    throw new CommandException("override-config failed.  See logcat for details.");
                }
            }
        }

        private void handleResetConfig() {
            Slog.i(ShortcutService.TAG, "cmd: handleResetConfig");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.loadConfigurationLocked();
            }
        }

        private void clearLauncher() {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getUserShortcutsLocked(this.mUserId).forceClearLauncher();
            }
        }

        private void showLauncher() {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.hasShortcutHostPermissionInner("-", this.mUserId);
                getOutPrintWriter().println("Launcher: " + ShortcutService.this.getUserShortcutsLocked(this.mUserId).getLastKnownLauncher());
            }
        }

        private void handleClearDefaultLauncher() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                clearLauncher();
            }
        }

        private void handleGetDefaultLauncher() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                clearLauncher();
                showLauncher();
            }
        }

        private void handleUnloadUser() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                Slog.i(ShortcutService.TAG, "cmd: handleUnloadUser: user=" + this.mUserId);
                ShortcutService.this.handleStopUser(this.mUserId);
            }
        }

        private void handleClearShortcuts() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String nextArgRequired = getNextArgRequired();
                Slog.i(ShortcutService.TAG, "cmd: handleClearShortcuts: user" + this.mUserId + ", " + nextArgRequired);
                ShortcutService.this.cleanUpPackageForAllLoadedUsers(nextArgRequired, this.mUserId, true);
            }
        }

        private void handleVerifyStates() throws CommandException {
            try {
                ShortcutService.this.verifyStatesForce();
            } catch (Throwable th) {
                throw new CommandException(th.getMessage() + "\n" + Log.getStackTraceString(th));
            }
        }
    }

    @VisibleForTesting
    long injectCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @VisibleForTesting
    long injectElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @VisibleForTesting
    long injectUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    @VisibleForTesting
    int injectBinderCallingUid() {
        return getCallingUid();
    }

    @VisibleForTesting
    int injectBinderCallingPid() {
        return getCallingPid();
    }

    private int getCallingUserId() {
        return UserHandle.getUserId(injectBinderCallingUid());
    }

    @VisibleForTesting
    long injectClearCallingIdentity() {
        return Binder.clearCallingIdentity();
    }

    @VisibleForTesting
    void injectRestoreCallingIdentity(long j) {
        Binder.restoreCallingIdentity(j);
    }

    String injectBuildFingerprint() {
        return Build.FINGERPRINT;
    }

    final void wtf(String str) {
        wtf(str, null);
    }

    void wtf(String str, Throwable th) {
        if (th == null) {
            th = new RuntimeException("Stacktrace");
        }
        synchronized (this.mLock) {
            this.mWtfCount++;
            this.mLastWtfStacktrace = new Exception("Last failure was logged here:");
        }
        Slog.wtf(TAG, str, th);
    }

    @VisibleForTesting
    File injectSystemDataPath() {
        return Environment.getDataSystemDirectory();
    }

    @VisibleForTesting
    File injectUserDataPath(int i) {
        return new File(Environment.getDataSystemCeDirectory(i), DIRECTORY_PER_USER);
    }

    public File getDumpPath() {
        return new File(injectUserDataPath(0), DIRECTORY_DUMP);
    }

    @VisibleForTesting
    boolean injectIsLowRamDevice() {
        return ActivityManager.isLowRamDeviceStatic();
    }

    @VisibleForTesting
    void injectRegisterUidObserver(IUidObserver iUidObserver, int i) {
        try {
            ActivityManager.getService().registerUidObserver(iUidObserver, i, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    File getUserBitmapFilePath(int i) {
        return new File(injectUserDataPath(i), DIRECTORY_BITMAPS);
    }

    @VisibleForTesting
    SparseArray<ShortcutUser> getShortcutsForTest() {
        return this.mUsers;
    }

    @VisibleForTesting
    int getMaxShortcutsForTest() {
        return this.mMaxShortcuts;
    }

    @VisibleForTesting
    int getMaxUpdatesPerIntervalForTest() {
        return this.mMaxUpdatesPerInterval;
    }

    @VisibleForTesting
    long getResetIntervalForTest() {
        return this.mResetInterval;
    }

    @VisibleForTesting
    int getMaxIconDimensionForTest() {
        return this.mMaxIconDimension;
    }

    @VisibleForTesting
    Bitmap.CompressFormat getIconPersistFormatForTest() {
        return this.mIconPersistFormat;
    }

    @VisibleForTesting
    int getIconPersistQualityForTest() {
        return this.mIconPersistQuality;
    }

    @VisibleForTesting
    ShortcutPackage getPackageShortcutForTest(String str, int i) {
        synchronized (this.mLock) {
            ShortcutUser shortcutUser = this.mUsers.get(i);
            if (shortcutUser == null) {
                return null;
            }
            return shortcutUser.getAllPackagesForTest().get(str);
        }
    }

    @VisibleForTesting
    ShortcutInfo getPackageShortcutForTest(String str, String str2, int i) {
        synchronized (this.mLock) {
            ShortcutPackage packageShortcutForTest = getPackageShortcutForTest(str, i);
            if (packageShortcutForTest == null) {
                return null;
            }
            return packageShortcutForTest.findShortcutById(str2);
        }
    }

    @VisibleForTesting
    ShortcutLauncher getLauncherShortcutForTest(String str, int i) {
        synchronized (this.mLock) {
            ShortcutUser shortcutUser = this.mUsers.get(i);
            if (shortcutUser == null) {
                return null;
            }
            return shortcutUser.getAllLaunchersForTest().get(ShortcutUser.PackageWithUser.of(i, str));
        }
    }

    @VisibleForTesting
    ShortcutRequestPinProcessor getShortcutRequestPinProcessorForTest() {
        return this.mShortcutRequestPinProcessor;
    }

    @VisibleForTesting
    boolean injectShouldPerformVerification() {
        return false;
    }

    final void verifyStates() {
        if (injectShouldPerformVerification()) {
            verifyStatesInner();
        }
    }

    private final void verifyStatesForce() {
        verifyStatesInner();
    }

    private void verifyStatesInner() {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((ShortcutUser) obj).forAllPackageItems(new Consumer() {
                        @Override
                        public final void accept(Object obj2) {
                            ((ShortcutPackageItem) obj2).verifyStates();
                        }
                    });
                }
            });
        }
    }

    @VisibleForTesting
    void waitForBitmapSavesForTest() {
        synchronized (this.mLock) {
            this.mShortcutBitmapSaver.waitForAllSavesLocked();
        }
    }
}
