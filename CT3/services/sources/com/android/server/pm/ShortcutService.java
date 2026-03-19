package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutService;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutServiceInternal;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ShortcutService extends IShortcutService.Stub {
    private static final String ATTR_VALUE = "value";
    static final boolean DEBUG = false;
    static final boolean DEBUG_LOAD = false;
    static final boolean DEBUG_PROCSTATE = false;
    static final String DEFAULT_ICON_PERSIST_FORMAT = Bitmap.CompressFormat.PNG.name();
    static final int DEFAULT_ICON_PERSIST_QUALITY = 100;
    static final int DEFAULT_MAX_ICON_DIMENSION_DP = 96;
    static final int DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP = 48;
    static final int DEFAULT_MAX_SHORTCUTS_PER_APP = 5;
    static final int DEFAULT_MAX_UPDATES_PER_INTERVAL = 10;
    static final long DEFAULT_RESET_INTERVAL_SEC = 86400;
    static final int DEFAULT_SAVE_DELAY_MS = 3000;
    static final String DIRECTORY_BITMAPS = "bitmaps";
    static final String DIRECTORY_PER_USER = "shortcut_service";
    public static final boolean FEATURE_ENABLED = false;
    static final String FILENAME_BASE_STATE = "shortcut_service.xml";
    static final String FILENAME_USER_PACKAGES = "shortcuts.xml";
    private static final int PACKAGE_MATCH_FLAGS = 794624;
    private static final int PROCESS_STATE_FOREGROUND_THRESHOLD = 4;
    static final String TAG = "ShortcutService";
    private static final String TAG_LAST_RESET_TIME = "last_reset_time";
    private static final String TAG_LOCALE_CHANGE_SEQUENCE_NUMBER = "locale_seq_no";
    private static final String TAG_ROOT = "root";
    final Context mContext;

    @GuardedBy("mStatLock")
    private final int[] mCountStats;

    @GuardedBy("mLock")
    private List<Integer> mDirtyUserIds;

    @GuardedBy("mStatLock")
    private final long[] mDurationStats;
    private final Handler mHandler;
    private final IPackageManager mIPackageManager;
    private Bitmap.CompressFormat mIconPersistFormat;
    private int mIconPersistQuality;

    @GuardedBy("mLock")
    private final ArrayList<ShortcutServiceInternal.ShortcutChangeListener> mListeners;
    private final AtomicLong mLocaleChangeSequenceNumber;
    private final Object mLock;
    private int mMaxDynamicShortcuts;
    private int mMaxIconDimension;
    int mMaxUpdatesPerInterval;
    private final PackageManagerInternal mPackageManagerInternal;
    final PackageMonitor mPackageMonitor;

    @GuardedBy("mLock")
    private long mRawLastResetTime;
    private long mResetInterval;
    private int mSaveDelayMillis;
    private final Runnable mSaveDirtyInfoRunner;
    final Object mStatLock;

    @GuardedBy("mLock")
    final SparseLongArray mUidLastForegroundElapsedTime;
    private final IUidObserver mUidObserver;

    @GuardedBy("mLock")
    final SparseIntArray mUidState;
    private final UserManager mUserManager;

    @GuardedBy("mLock")
    private final SparseArray<ShortcutUser> mUsers;

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

    interface Stats {
        public static final int COUNT = 5;
        public static final int GET_APPLICATION_INFO = 3;
        public static final int GET_DEFAULT_HOME = 0;
        public static final int GET_PACKAGE_INFO = 1;
        public static final int GET_PACKAGE_INFO_WITH_SIG = 2;
        public static final int LAUNCHER_PERMISSION_CHECK = 4;
    }

    public ShortcutService(Context context) {
        this(context, BackgroundThread.get().getLooper());
    }

    ShortcutService(Context context, Looper looper) {
        this.mLock = new Object();
        this.mListeners = new ArrayList<>(1);
        this.mUsers = new SparseArray<>();
        this.mUidState = new SparseIntArray();
        this.mUidLastForegroundElapsedTime = new SparseLongArray();
        this.mDirtyUserIds = new ArrayList();
        this.mLocaleChangeSequenceNumber = new AtomicLong();
        this.mStatLock = new Object();
        this.mCountStats = new int[5];
        this.mDurationStats = new long[5];
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int uid, int procState) throws RemoteException {
                ShortcutService.this.handleOnUidStateChanged(uid, procState);
            }

            public void onUidGone(int uid) throws RemoteException {
                ShortcutService.this.handleOnUidStateChanged(uid, 16);
            }

            public void onUidActive(int uid) throws RemoteException {
            }

            public void onUidIdle(int uid) throws RemoteException {
            }
        };
        this.mSaveDirtyInfoRunner = new Runnable() {
            @Override
            public void run() {
                ShortcutService.this.m2581com_android_server_pm_ShortcutServicemthref0();
            }
        };
        this.mPackageMonitor = new PackageMonitor() {
            public void onPackageAdded(String packageName, int uid) {
                ShortcutService.this.handlePackageAdded(packageName, getChangingUserId());
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                ShortcutService.this.handlePackageUpdateFinished(packageName, getChangingUserId());
            }

            public void onPackageRemoved(String packageName, int uid) {
                ShortcutService.this.handlePackageRemoved(packageName, getChangingUserId());
            }

            public void onPackageDataCleared(String packageName, int uid) {
                ShortcutService.this.handlePackageDataCleared(packageName, getChangingUserId());
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context);
        LocalServices.addService(ShortcutServiceInternal.class, new LocalService(this, null));
        this.mHandler = new Handler(looper);
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
    }

    void logDurationStat(int statId, long start) {
        synchronized (this.mStatLock) {
            int[] iArr = this.mCountStats;
            iArr[statId] = iArr[statId] + 1;
            long[] jArr = this.mDurationStats;
            jArr[statId] = jArr[statId] + (System.currentTimeMillis() - start);
        }
    }

    public long getLocaleChangeSequenceNumber() {
        return this.mLocaleChangeSequenceNumber.get();
    }

    void handleOnUidStateChanged(int uid, int procState) {
        synchronized (this.mLock) {
            this.mUidState.put(uid, procState);
            if (isProcessStateForeground(procState)) {
                this.mUidLastForegroundElapsedTime.put(uid, injectElapsedRealtime());
            }
        }
    }

    private boolean isProcessStateForeground(int processState) {
        return processState <= 4;
    }

    boolean isUidForegroundLocked(int uid) {
        if (uid == 1000) {
            return true;
        }
        return isProcessStateForeground(this.mUidState.get(uid, 16));
    }

    long getUidLastForegroundElapsedTimeLocked(int uid) {
        return this.mUidLastForegroundElapsedTime.get(uid);
    }

    public static final class Lifecycle extends SystemService {
        final ShortcutService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ShortcutService(context);
        }

        @Override
        public void onStart() {
            publishBinderService("shortcut", this.mService);
        }

        @Override
        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }

        @Override
        public void onCleanupUser(int userHandle) {
            this.mService.handleCleanupUser(userHandle);
        }

        @Override
        public void onUnlockUser(int userId) {
            this.mService.handleUnlockUser(userId);
        }
    }

    void onBootPhase(int phase) {
        switch (phase) {
            case SystemService.PHASE_LOCK_SETTINGS_READY:
                initialize();
                break;
        }
    }

    void handleUnlockUser(int userId) {
    }

    void handleCleanupUser(int userId) {
    }

    private void unloadUserLocked(int userId) {
        m2581com_android_server_pm_ShortcutServicemthref0();
        this.mUsers.delete(userId);
    }

    private AtomicFile getBaseStateFile() {
        File path = new File(injectSystemDataPath(), FILENAME_BASE_STATE);
        path.mkdirs();
        return new AtomicFile(path);
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

    boolean updateConfigurationLocked(String config) {
        int i;
        boolean result = true;
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(config);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad shortcut manager settings", e);
            result = false;
        }
        this.mSaveDelayMillis = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_SAVE_DELAY_MILLIS, 3000L));
        this.mResetInterval = Math.max(1L, parser.getLong(ConfigConstants.KEY_RESET_INTERVAL_SEC, DEFAULT_RESET_INTERVAL_SEC) * 1000);
        this.mMaxUpdatesPerInterval = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_UPDATES_PER_INTERVAL, 10L));
        this.mMaxDynamicShortcuts = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_SHORTCUTS, 5L));
        if (injectIsLowRamDevice()) {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP_LOWRAM, 48L);
        } else {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP, 96L);
        }
        int iconDimensionDp = Math.max(1, i);
        this.mMaxIconDimension = injectDipToPixel(iconDimensionDp);
        this.mIconPersistFormat = Bitmap.CompressFormat.valueOf(parser.getString(ConfigConstants.KEY_ICON_FORMAT, DEFAULT_ICON_PERSIST_FORMAT));
        this.mIconPersistQuality = (int) parser.getLong(ConfigConstants.KEY_ICON_QUALITY, 100L);
        return result;
    }

    String injectShortcutManagerConstants() {
        return Settings.Global.getString(this.mContext.getContentResolver(), "shortcut_manager_constants");
    }

    int injectDipToPixel(int dip) {
        return (int) TypedValue.applyDimension(1, dip, this.mContext.getResources().getDisplayMetrics());
    }

    static String parseStringAttribute(XmlPullParser parser, String attribute) {
        return parser.getAttributeValue(null, attribute);
    }

    static boolean parseBooleanAttribute(XmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute) == 1;
    }

    static int parseIntAttribute(XmlPullParser parser, String attribute) {
        return (int) parseLongAttribute(parser, attribute);
    }

    static int parseIntAttribute(XmlPullParser parser, String attribute, int def) {
        return (int) parseLongAttribute(parser, attribute, def);
    }

    static long parseLongAttribute(XmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute, 0L);
    }

    static long parseLongAttribute(XmlPullParser parser, String attribute, long def) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return def;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Error parsing long " + value);
            return def;
        }
    }

    static ComponentName parseComponentNameAttribute(XmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return ComponentName.unflattenFromString(value);
    }

    static Intent parseIntentAttribute(XmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Intent.parseUri(value, 0);
        } catch (URISyntaxException e) {
            Slog.e(TAG, "Error parsing intent", e);
            return null;
        }
    }

    static void writeTagValue(XmlSerializer out, String tag, String value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        out.startTag(null, tag);
        out.attribute(null, ATTR_VALUE, value);
        out.endTag(null, tag);
    }

    static void writeTagValue(XmlSerializer out, String tag, long value) throws IOException {
        writeTagValue(out, tag, Long.toString(value));
    }

    static void writeTagValue(XmlSerializer out, String tag, ComponentName name) throws IOException {
        if (name == null) {
            return;
        }
        writeTagValue(out, tag, name.flattenToString());
    }

    static void writeTagExtra(XmlSerializer out, String tag, PersistableBundle bundle) throws XmlPullParserException, IOException {
        if (bundle == null) {
            return;
        }
        out.startTag(null, tag);
        bundle.saveToXml(out);
        out.endTag(null, tag);
    }

    static void writeAttr(XmlSerializer out, String name, String value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        out.attribute(null, name, value);
    }

    static void writeAttr(XmlSerializer out, String name, long value) throws IOException {
        writeAttr(out, name, String.valueOf(value));
    }

    static void writeAttr(XmlSerializer out, String name, boolean value) throws IOException {
        if (!value) {
            return;
        }
        writeAttr(out, name, "1");
    }

    static void writeAttr(XmlSerializer out, String name, ComponentName comp) throws IOException {
        if (comp == null) {
            return;
        }
        writeAttr(out, name, comp.flattenToString());
    }

    static void writeAttr(XmlSerializer out, String name, Intent intent) throws IOException {
        if (intent == null) {
            return;
        }
        writeAttr(out, name, intent.toUri(0));
    }

    void saveBaseStateLocked() {
        AtomicFile file = getBaseStateFile();
        FileOutputStream outs = null;
        try {
            outs = file.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outs, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_ROOT);
            writeTagValue((XmlSerializer) fastXmlSerializer, TAG_LAST_RESET_TIME, this.mRawLastResetTime);
            writeTagValue((XmlSerializer) fastXmlSerializer, TAG_LOCALE_CHANGE_SEQUENCE_NUMBER, this.mLocaleChangeSequenceNumber.get());
            fastXmlSerializer.endTag(null, TAG_ROOT);
            fastXmlSerializer.endDocument();
            file.finishWrite(outs);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(outs);
        }
    }

    private void loadBaseStateLocked() {
        this.mRawLastResetTime = 0L;
        AtomicFile file = getBaseStateFile();
        Throwable th = null;
        FileInputStream in = null;
        try {
            try {
                try {
                    in = file.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(in, StandardCharsets.UTF_8.name());
                    while (true) {
                        int type = parser.next();
                        if (type == 1) {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            if (th != null) {
                                throw th;
                            }
                        } else if (type == 2) {
                            int depth = parser.getDepth();
                            String tag = parser.getName();
                            if (depth == 1) {
                                if (!TAG_ROOT.equals(tag)) {
                                    break;
                                }
                            } else if (tag.equals(TAG_LAST_RESET_TIME)) {
                                this.mRawLastResetTime = parseLongAttribute(parser, ATTR_VALUE);
                            } else if (tag.equals(TAG_LOCALE_CHANGE_SEQUENCE_NUMBER)) {
                                this.mLocaleChangeSequenceNumber.set(parseLongAttribute(parser, ATTR_VALUE));
                            } else {
                                Slog.e(TAG, "Invalid tag: " + tag);
                            }
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e);
                    this.mRawLastResetTime = 0L;
                }
            } catch (Throwable th3) {
                th = th3;
                if (in != null) {
                }
                if (th != null) {
                }
            }
        } catch (FileNotFoundException e2) {
        }
        getLastResetTimeLocked();
        return;
        if (th != null) {
            throw th;
        }
    }

    private void saveUserLocked(int userId) {
        File path = new File(injectUserDataPath(userId), FILENAME_USER_PACKAGES);
        path.mkdirs();
        AtomicFile file = new AtomicFile(path);
        FileOutputStream os = null;
        try {
            os = file.startWrite();
            saveUserInternalLocked(userId, os, false);
            file.finishWrite(os);
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(os);
        }
    }

    private void saveUserInternalLocked(int userId, OutputStream os, boolean forBackup) throws XmlPullParserException, IOException {
        BufferedOutputStream bos = new BufferedOutputStream(os);
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(bos, StandardCharsets.UTF_8.name());
        out.startDocument(null, true);
        getUserShortcutsLocked(userId).saveToXml(this, out, forBackup);
        out.endDocument();
        bos.flush();
        os.flush();
    }

    static IOException throwForInvalidTag(int depth, String tag) throws IOException {
        throw new IOException(String.format("Invalid tag '%s' found at depth %d", tag, Integer.valueOf(depth)));
    }

    static void warnForInvalidTag(int depth, String tag) throws IOException {
        Slog.w(TAG, String.format("Invalid tag '%s' found at depth %d", tag, Integer.valueOf(depth)));
    }

    private ShortcutUser loadUserLocked(int userId) {
        File path = new File(injectUserDataPath(userId), FILENAME_USER_PACKAGES);
        AtomicFile file = new AtomicFile(path);
        try {
            FileInputStream in = file.openRead();
            try {
                return loadUserInternal(userId, in, false);
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e);
                return null;
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    private ShortcutUser loadUserInternal(int userId, InputStream is, boolean fromBackup) throws XmlPullParserException, IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ShortcutUser ret = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(bis, StandardCharsets.UTF_8.name());
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    if (depth == 1 && "user".equals(tag)) {
                        ret = ShortcutUser.loadFromXml(this, parser, userId, fromBackup);
                    } else {
                        throwForInvalidTag(depth, tag);
                    }
                }
            } else {
                return ret;
            }
        }
    }

    private void scheduleSaveBaseState() {
        scheduleSaveInner(-10000);
    }

    void scheduleSaveUser(int userId) {
        scheduleSaveInner(userId);
    }

    private void scheduleSaveInner(int userId) {
        synchronized (this.mLock) {
            if (!this.mDirtyUserIds.contains(Integer.valueOf(userId))) {
                this.mDirtyUserIds.add(Integer.valueOf(userId));
            }
        }
        this.mHandler.removeCallbacks(this.mSaveDirtyInfoRunner);
        this.mHandler.postDelayed(this.mSaveDirtyInfoRunner, this.mSaveDelayMillis);
    }

    void m2581com_android_server_pm_ShortcutServicemthref0() {
        synchronized (this.mLock) {
            for (int i = this.mDirtyUserIds.size() - 1; i >= 0; i--) {
                int userId = this.mDirtyUserIds.get(i).intValue();
                if (userId == -10000) {
                    saveBaseStateLocked();
                } else {
                    saveUserLocked(userId);
                }
            }
            this.mDirtyUserIds.clear();
        }
    }

    long getLastResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime;
    }

    long getNextResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime + this.mResetInterval;
    }

    static boolean isClockValid(long time) {
        return time >= 1420070400;
    }

    private void updateTimesLocked() {
        long now = injectCurrentTimeMillis();
        long prevLastResetTime = this.mRawLastResetTime;
        if (this.mRawLastResetTime == 0) {
            this.mRawLastResetTime = now;
        } else if (now < this.mRawLastResetTime) {
            if (isClockValid(now)) {
                Slog.w(TAG, "Clock rewound");
                this.mRawLastResetTime = now;
            }
        } else if (this.mRawLastResetTime + this.mResetInterval <= now) {
            long offset = this.mRawLastResetTime % this.mResetInterval;
            this.mRawLastResetTime = ((now / this.mResetInterval) * this.mResetInterval) + offset;
        }
        if (prevLastResetTime == this.mRawLastResetTime) {
            return;
        }
        scheduleSaveBaseState();
    }

    @GuardedBy("mLock")
    private boolean isUserLoadedLocked(int userId) {
        return this.mUsers.get(userId) != null;
    }

    @GuardedBy("mLock")
    ShortcutUser getUserShortcutsLocked(int userId) {
        ShortcutUser userPackages = this.mUsers.get(userId);
        if (userPackages == null) {
            userPackages = loadUserLocked(userId);
            if (userPackages == null) {
                userPackages = new ShortcutUser(userId);
            }
            this.mUsers.put(userId, userPackages);
        }
        return userPackages;
    }

    void forEachLoadedUserLocked(Consumer<ShortcutUser> c) {
        for (int i = this.mUsers.size() - 1; i >= 0; i--) {
            c.accept(this.mUsers.valueAt(i));
        }
    }

    @GuardedBy("mLock")
    ShortcutPackage getPackageShortcutsLocked(String packageName, int userId) {
        return getUserShortcutsLocked(userId).getPackageShortcuts(this, packageName);
    }

    @GuardedBy("mLock")
    ShortcutLauncher getLauncherShortcutsLocked(String packageName, int ownerUserId, int launcherUserId) {
        return getUserShortcutsLocked(ownerUserId).getLauncherShortcuts(this, packageName, launcherUserId);
    }

    void removeIcon(int userId, ShortcutInfo shortcut) {
        if (shortcut.getBitmapPath() == null) {
            return;
        }
        new File(shortcut.getBitmapPath()).delete();
        shortcut.setBitmapPath(null);
        shortcut.setIconResourceId(0);
        shortcut.clearFlags(12);
    }

    public void cleanupBitmapsForPackage(int userId, String packageName) {
        File packagePath = new File(getUserBitmapFilePath(userId), packageName);
        if (!packagePath.isDirectory()) {
            return;
        }
        if (FileUtils.deleteContents(packagePath) ? packagePath.delete() : false) {
            return;
        }
        Slog.w(TAG, "Unable to remove directory " + packagePath);
    }

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

    FileOutputStreamWithPath openIconFileForWrite(int userId, ShortcutInfo shortcut) throws IOException {
        File packagePath = new File(getUserBitmapFilePath(userId), shortcut.getPackageName());
        if (!packagePath.isDirectory()) {
            packagePath.mkdirs();
            if (!packagePath.isDirectory()) {
                throw new IOException("Unable to create directory " + packagePath);
            }
            SELinux.restorecon(packagePath);
        }
        String baseName = String.valueOf(injectCurrentTimeMillis());
        int suffix = 0;
        while (true) {
            String filename = (suffix == 0 ? baseName : baseName + "_" + suffix) + ".png";
            File file = new File(packagePath, filename);
            if (file.exists()) {
                suffix++;
            } else {
                return new FileOutputStreamWithPath(file);
            }
        }
    }

    void saveIconAndFixUpShortcut(int userId, ShortcutInfo shortcut) {
        if (shortcut.hasIconFile() || shortcut.hasIconResource()) {
            return;
        }
        long token = injectClearCallingIdentity();
        try {
            shortcut.setIconResourceId(0);
            shortcut.setBitmapPath(null);
            Icon icon = shortcut.getIcon();
            if (icon == null) {
                return;
            }
            try {
                switch (icon.getType()) {
                    case 1:
                        Bitmap bitmap = icon.getBitmap();
                        if (bitmap == null) {
                            Slog.e(TAG, "Null bitmap detected");
                            return;
                        }
                        File file = null;
                        try {
                            FileOutputStreamWithPath out = openIconFileForWrite(userId, shortcut);
                            try {
                                out.getFile();
                                Bitmap shrunk = shrinkBitmap(bitmap, this.mMaxIconDimension);
                                try {
                                    shrunk.compress(this.mIconPersistFormat, this.mIconPersistQuality, out);
                                    shortcut.setBitmapPath(out.getFile().getAbsolutePath());
                                    shortcut.addFlags(8);
                                } finally {
                                    if (bitmap != shrunk) {
                                        shrunk.recycle();
                                    }
                                }
                            } finally {
                                IoUtils.closeQuietly(out);
                            }
                        } catch (IOException | RuntimeException e) {
                            Slog.wtf(TAG, "Unable to write bitmap to file", e);
                            if (0 != 0 && file.exists()) {
                                file.delete();
                            }
                        }
                        return;
                    case 2:
                        injectValidateIconResPackage(shortcut, icon);
                        shortcut.setIconResourceId(icon.getResId());
                        shortcut.addFlags(4);
                        return;
                    default:
                        throw ShortcutInfo.getInvalidIconException();
                }
            } finally {
                shortcut.clearIcon();
            }
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    void injectValidateIconResPackage(ShortcutInfo shortcut, Icon icon) {
        if (shortcut.getPackageName().equals(icon.getResPackage())) {
        } else {
            throw new IllegalArgumentException("Icon resource must reside in shortcut owner package");
        }
    }

    static Bitmap shrinkBitmap(Bitmap in, int maxSize) {
        int ow = in.getWidth();
        int oh = in.getHeight();
        if (ow <= maxSize && oh <= maxSize) {
            return in;
        }
        int longerDimension = Math.max(ow, oh);
        int nw = (ow * maxSize) / longerDimension;
        int nh = (oh * maxSize) / longerDimension;
        Bitmap scaledBitmap = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(scaledBitmap);
        RectF dst = new RectF(0.0f, 0.0f, nw, nh);
        c.drawBitmap(in, (Rect) null, dst, (Paint) null);
        return scaledBitmap;
    }

    private boolean isCallerSystem() {
        int callingUid = injectBinderCallingUid();
        return UserHandle.isSameApp(callingUid, 1000);
    }

    private boolean isCallerShell() {
        int callingUid = injectBinderCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    private void enforceSystemOrShell() {
        Preconditions.checkState(!isCallerSystem() ? isCallerShell() : true, "Caller must be system or shell");
    }

    private void enforceShell() {
        Preconditions.checkState(isCallerShell(), "Caller must be shell");
    }

    private void enforceSystem() {
        Preconditions.checkState(isCallerSystem(), "Caller must be system");
    }

    private void enforceResetThrottlingPermission() {
        if (isCallerSystem()) {
            return;
        }
        injectEnforceCallingPermission("android.permission.RESET_SHORTCUT_MANAGER_THROTTLING", null);
    }

    void injectEnforceCallingPermission(String permission, String message) {
        this.mContext.enforceCallingPermission(permission, message);
    }

    private void verifyCaller(String packageName, int userId) {
        Preconditions.checkStringNotEmpty(packageName, "packageName");
        if (isCallerSystem()) {
            return;
        }
        int callingUid = injectBinderCallingUid();
        if (UserHandle.getUserId(callingUid) != userId) {
            throw new SecurityException("Invalid user-ID");
        }
        if (injectGetPackageUid(packageName, userId) == injectBinderCallingUid()) {
        } else {
            throw new SecurityException("Calling package name mismatch");
        }
    }

    void postToHandler(Runnable r) {
        this.mHandler.post(r);
    }

    void enforceMaxDynamicShortcuts(int numShortcuts) {
        if (numShortcuts <= this.mMaxDynamicShortcuts) {
        } else {
            throw new IllegalArgumentException("Max number of dynamic shortcuts exceeded");
        }
    }

    void packageShortcutsChanged(String packageName, int userId) {
        notifyListeners(packageName, userId);
        scheduleSaveUser(userId);
    }

    private void notifyListeners(final String packageName, final int userId) {
        if (!this.mUserManager.isUserRunning(userId)) {
            return;
        }
        postToHandler(new Runnable() {
            @Override
            public void run() {
                ShortcutService.this.m2587com_android_server_pm_ShortcutService_lambda$6(packageName, userId);
            }
        });
    }

    void m2587com_android_server_pm_ShortcutService_lambda$6(String packageName, int userId) {
        ArrayList<ShortcutServiceInternal.ShortcutChangeListener> copy;
        synchronized (this.mLock) {
            copy = new ArrayList<>(this.mListeners);
        }
        for (int i = copy.size() - 1; i >= 0; i--) {
            copy.get(i).onShortcutChanged(packageName, userId);
        }
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcut, boolean forUpdate) {
        Preconditions.checkNotNull(shortcut, "Null shortcut detected");
        if (shortcut.getActivityComponent() != null) {
            Preconditions.checkState(shortcut.getPackageName().equals(shortcut.getActivityComponent().getPackageName()), "Activity package name mismatch");
        }
        if (!forUpdate) {
            shortcut.enforceMandatoryFields();
        }
        if (shortcut.getIcon() != null) {
            ShortcutInfo.validateIcon(shortcut.getIcon());
        }
        validateForXml(shortcut.getId());
        validateForXml(shortcut.getTitle());
        validatePersistableBundleForXml(shortcut.getIntentPersistableExtras());
        validatePersistableBundleForXml(shortcut.getExtras());
        shortcut.replaceFlags(0);
    }

    private static void validatePersistableBundleForXml(PersistableBundle b) {
        if (b == null || b.size() == 0) {
            return;
        }
        for (String key : b.keySet()) {
            validateForXml(key);
            Object value = b.get(key);
            if (value != null) {
                if (value instanceof String) {
                    validateForXml((String) value);
                } else if (value instanceof String[]) {
                    for (String v : (String[]) value) {
                        validateForXml(v);
                    }
                } else if (value instanceof PersistableBundle) {
                    validatePersistableBundleForXml((PersistableBundle) value);
                }
            }
        }
    }

    private static void validateForXml(String s) {
        if (TextUtils.isEmpty(s)) {
            return;
        }
        for (int i = s.length() - 1; i >= 0; i--) {
            if (!isAllowedInXml(s.charAt(i))) {
                throw new IllegalArgumentException("Unsupported character detected in: " + s);
            }
        }
    }

    private static boolean isAllowedInXml(char c) {
        if (c < ' ' || c > 55295) {
            return c >= 57344 && c <= 65533;
        }
        return true;
    }

    public boolean setDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        int size = newShortcuts.size();
        synchronized (this.mLock) {
            ShortcutPackage ps = getPackageShortcutsLocked(packageName, userId);
            if (!ps.tryApiCall(this)) {
                return false;
            }
            enforceMaxDynamicShortcuts(size);
            for (int i = 0; i < size; i++) {
                fixUpIncomingShortcutInfo(newShortcuts.get(i), false);
            }
            ps.deleteAllDynamicShortcuts(this);
            for (int i2 = 0; i2 < size; i2++) {
                ShortcutInfo newShortcut = newShortcuts.get(i2);
                ps.addDynamicShortcut(this, newShortcut);
            }
            packageShortcutsChanged(packageName, userId);
            return true;
        }
    }

    public boolean updateShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        int size = newShortcuts.size();
        synchronized (this.mLock) {
            ShortcutPackage ps = getPackageShortcutsLocked(packageName, userId);
            if (!ps.tryApiCall(this)) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                ShortcutInfo source = newShortcuts.get(i);
                fixUpIncomingShortcutInfo(source, true);
                ShortcutInfo target = ps.findShortcutById(source.getId());
                if (target != null) {
                    boolean replacingIcon = source.getIcon() != null;
                    if (replacingIcon) {
                        removeIcon(userId, target);
                    }
                    target.copyNonNullFieldsFrom(source);
                    if (replacingIcon) {
                        saveIconAndFixUpShortcut(userId, target);
                    }
                }
            }
            packageShortcutsChanged(packageName, userId);
            return true;
        }
    }

    public boolean addDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        int size = newShortcuts.size();
        synchronized (this.mLock) {
            ShortcutPackage ps = getPackageShortcutsLocked(packageName, userId);
            if (!ps.tryApiCall(this)) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                ShortcutInfo newShortcut = newShortcuts.get(i);
                fixUpIncomingShortcutInfo(newShortcut, false);
                ps.addDynamicShortcut(this, newShortcut);
            }
            packageShortcutsChanged(packageName, userId);
            return true;
        }
    }

    public void removeDynamicShortcuts(String packageName, List shortcutIds, int userId) {
        verifyCaller(packageName, userId);
        Preconditions.checkNotNull(shortcutIds, "shortcutIds must be provided");
        synchronized (this.mLock) {
            for (int i = shortcutIds.size() - 1; i >= 0; i--) {
                getPackageShortcutsLocked(packageName, userId).deleteDynamicWithId(this, (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i)));
            }
        }
        packageShortcutsChanged(packageName, userId);
    }

    public void removeAllDynamicShortcuts(String packageName, int userId) {
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            getPackageShortcutsLocked(packageName, userId).deleteAllDynamicShortcuts(this);
        }
        packageShortcutsChanged(packageName, userId);
    }

    public ParceledListSlice<ShortcutInfo> getDynamicShortcuts(String packageName, int userId) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 1, new Predicate() {
                @Override
                public boolean test(Object arg0) {
                    return ((ShortcutInfo) arg0).isDynamic();
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    public ParceledListSlice<ShortcutInfo> getPinnedShortcuts(String packageName, int userId) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 1, new Predicate() {
                @Override
                public boolean test(Object arg0) {
                    return ((ShortcutInfo) arg0).isPinned();
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    private ParceledListSlice<ShortcutInfo> getShortcutsWithQueryLocked(String packageName, int userId, int cloneFlags, Predicate<ShortcutInfo> query) {
        ArrayList<ShortcutInfo> ret = new ArrayList<>();
        getPackageShortcutsLocked(packageName, userId).findAll(this, ret, query, cloneFlags);
        return new ParceledListSlice<>(ret);
    }

    public int getMaxDynamicShortcutCount(String packageName, int userId) throws RemoteException {
        verifyCaller(packageName, userId);
        return this.mMaxDynamicShortcuts;
    }

    public int getRemainingCallCount(String packageName, int userId) {
        int apiCallCount;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            apiCallCount = this.mMaxUpdatesPerInterval - getPackageShortcutsLocked(packageName, userId).getApiCallCount(this);
        }
        return apiCallCount;
    }

    public long getRateLimitResetTime(String packageName, int userId) {
        long nextResetTimeLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            nextResetTimeLocked = getNextResetTimeLocked();
        }
        return nextResetTimeLocked;
    }

    public int getIconMaxDimensions(String packageName, int userId) throws RemoteException {
        int i;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            i = this.mMaxIconDimension;
        }
        return i;
    }

    public void resetThrottling() {
        enforceSystemOrShell();
        resetThrottlingInner(getCallingUserId());
    }

    void resetThrottlingInner(int userId) {
        synchronized (this.mLock) {
            getUserShortcutsLocked(userId).resetThrottling();
        }
        scheduleSaveUser(userId);
        Slog.i(TAG, "ShortcutManager: throttling counter reset for user " + userId);
    }

    void resetAllThrottlingInner() {
        synchronized (this.mLock) {
            this.mRawLastResetTime = injectCurrentTimeMillis();
        }
        scheduleSaveBaseState();
        Slog.i(TAG, "ShortcutManager: throttling counter reset for all users");
    }

    void resetPackageThrottling(String packageName, int userId) {
        synchronized (this.mLock) {
            getPackageShortcutsLocked(packageName, userId).resetRateLimitingForCommandLineNoSaving();
            saveUserLocked(userId);
        }
    }

    public void onApplicationActive(String packageName, int userId) {
        enforceResetThrottlingPermission();
        resetPackageThrottling(packageName, userId);
    }

    boolean hasShortcutHostPermission(String callingPackage, int userId) {
        return hasShortcutHostPermissionInner(callingPackage, userId);
    }

    boolean hasShortcutHostPermissionInner(String callingPackage, int userId) {
        ComponentName detected;
        synchronized (this.mLock) {
            long start = System.currentTimeMillis();
            ShortcutUser user = getUserShortcutsLocked(userId);
            List<ResolveInfo> allHomeCandidates = new ArrayList<>();
            long startGetHomeActivitiesAsUser = System.currentTimeMillis();
            ComponentName defaultLauncher = injectPackageManagerInternal().getHomeActivitiesAsUser(allHomeCandidates, userId);
            logDurationStat(0, startGetHomeActivitiesAsUser);
            if (defaultLauncher != null) {
                detected = defaultLauncher;
            } else {
                detected = user.getLauncherComponent();
            }
            if (detected == null) {
                int size = allHomeCandidates.size();
                int lastPriority = Integer.MIN_VALUE;
                for (int i = 0; i < size; i++) {
                    ResolveInfo ri = allHomeCandidates.get(i);
                    if (ri.activityInfo.applicationInfo.isSystemApp() && ri.priority >= lastPriority) {
                        detected = ri.activityInfo.getComponentName();
                        lastPriority = ri.priority;
                    }
                }
            }
            logDurationStat(4, start);
            if (detected != null) {
                user.setLauncherComponent(this, detected);
                return detected.getPackageName().equals(callingPackage);
            }
            return false;
        }
    }

    private void cleanUpPackageForAllLoadedUsers(final String packageName, final int packageUserId) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() {
                @Override
                public void accept(Object arg0) {
                    ShortcutService.this.m2588com_android_server_pm_ShortcutService_lambda$9(packageName, packageUserId, (ShortcutUser) arg0);
                }
            });
        }
    }

    void m2588com_android_server_pm_ShortcutService_lambda$9(String packageName, int packageUserId, ShortcutUser user) {
        cleanUpPackageLocked(packageName, user.getUserId(), packageUserId);
    }

    void cleanUpPackageLocked(final String packageName, int owningUserId, final int packageUserId) {
        boolean wasUserLoaded = isUserLoadedLocked(owningUserId);
        ShortcutUser user = getUserShortcutsLocked(owningUserId);
        boolean doNotify = false;
        if (packageUserId == owningUserId && user.removePackage(this, packageName) != null) {
            doNotify = true;
        }
        user.removeLauncher(packageUserId, packageName);
        user.forAllLaunchers(new Consumer() {
            @Override
            public void accept(Object arg0) {
                ((ShortcutLauncher) arg0).cleanUpPackage(packageName, packageUserId);
            }
        });
        user.forAllPackages(new Consumer() {
            @Override
            public void accept(Object arg0) {
                ShortcutService.this.m2582com_android_server_pm_ShortcutService_lambda$11((ShortcutPackage) arg0);
            }
        });
        scheduleSaveUser(owningUserId);
        if (doNotify) {
            notifyListeners(packageName, owningUserId);
        }
        if (wasUserLoaded) {
            return;
        }
        unloadUserLocked(owningUserId);
    }

    void m2582com_android_server_pm_ShortcutService_lambda$11(ShortcutPackage p) {
        p.refreshPinnedFlags(this);
    }

    private class LocalService extends ShortcutServiceInternal {

        final class void_onSystemLocaleChangedNoLock__LambdaImpl0 implements Runnable {
            public void_onSystemLocaleChangedNoLock__LambdaImpl0() {
            }

            @Override
            public void run() {
                LocalService.this.m2592com_android_server_pm_ShortcutService$LocalService_lambda$4();
            }
        }

        LocalService(ShortcutService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public List<ShortcutInfo> getShortcuts(final int launcherUserId, final String callingPackage, final long changedSince, String packageName, List<String> shortcutIds, final ComponentName componentName, final int queryFlags, final int userId) {
            final int cloneFlag;
            final ArrayList<ShortcutInfo> ret = new ArrayList<>();
            if ((queryFlags & 4) == 0) {
                cloneFlag = 3;
            } else {
                cloneFlag = 4;
            }
            if (packageName == null) {
                shortcutIds = null;
            }
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave(ShortcutService.this);
                if (packageName != null) {
                    getShortcutsInnerLocked(launcherUserId, callingPackage, packageName, shortcutIds, changedSince, componentName, queryFlags, userId, ret, cloneFlag);
                } else {
                    final List<String> shortcutIdsF = shortcutIds;
                    ShortcutService.this.getUserShortcutsLocked(userId).forAllPackages(new Consumer() {
                        @Override
                        public void accept(Object arg0) {
                            LocalService.this.m2591com_android_server_pm_ShortcutService$LocalService_lambda$1(launcherUserId, callingPackage, shortcutIdsF, changedSince, componentName, queryFlags, userId, ret, cloneFlag, (ShortcutPackage) arg0);
                        }
                    });
                }
            }
            return ret;
        }

        void m2591com_android_server_pm_ShortcutService$LocalService_lambda$1(int launcherUserId, String callingPackage, List shortcutIdsF, long changedSince, ComponentName componentName, int queryFlags, int userId, ArrayList ret, int cloneFlag, ShortcutPackage p) {
            getShortcutsInnerLocked(launcherUserId, callingPackage, p.getPackageName(), shortcutIdsF, changedSince, componentName, queryFlags, userId, ret, cloneFlag);
        }

        private void getShortcutsInnerLocked(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, final long changedSince, final ComponentName componentName, final int queryFlags, int userId, ArrayList<ShortcutInfo> ret, int cloneFlag) {
            final ArraySet arraySet = shortcutIds == null ? null : new ArraySet(shortcutIds);
            ShortcutService.this.getPackageShortcutsLocked(packageName, userId).findAll(ShortcutService.this, ret, new Predicate() {
                @Override
                public boolean test(Object arg0) {
                    return LocalService.m2589com_android_server_pm_ShortcutService$LocalService_lambda$2(changedSince, arraySet, componentName, queryFlags, (ShortcutInfo) arg0);
                }
            }, cloneFlag, callingPackage, launcherUserId);
        }

        static boolean m2589com_android_server_pm_ShortcutService$LocalService_lambda$2(long changedSince, ArraySet ids, ComponentName componentName, int queryFlags, ShortcutInfo si) {
            boolean zIsDynamic;
            boolean zIsPinned;
            if (si.getLastChangedTimestamp() < changedSince) {
                return false;
            }
            if (ids != null && !ids.contains(si.getId())) {
                return false;
            }
            if (componentName != null && !componentName.equals(si.getActivityComponent())) {
                return false;
            }
            if ((queryFlags & 1) == 0) {
                zIsDynamic = false;
            } else {
                zIsDynamic = si.isDynamic();
            }
            if ((queryFlags & 2) == 0) {
                zIsPinned = false;
            } else {
                zIsPinned = si.isPinned();
            }
            if (zIsDynamic) {
                return true;
            }
            return zIsPinned;
        }

        public boolean isPinnedByCaller(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            boolean zIsPinned;
            Preconditions.checkStringNotEmpty(packageName, "packageName");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave(ShortcutService.this);
                ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId);
                zIsPinned = si != null ? si.isPinned() : false;
            }
            return zIsPinned;
        }

        private ShortcutInfo getShortcutInfoLocked(int launcherUserId, String callingPackage, String packageName, final String shortcutId, int userId) {
            Preconditions.checkStringNotEmpty(packageName, "packageName");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            ArrayList<ShortcutInfo> list = new ArrayList<>(1);
            ShortcutService.this.getPackageShortcutsLocked(packageName, userId).findAll(ShortcutService.this, list, new Predicate() {
                @Override
                public boolean test(Object arg0) {
                    return LocalService.m2590com_android_server_pm_ShortcutService$LocalService_lambda$3(shortcutId, (ShortcutInfo) arg0);
                }
            }, 0, callingPackage, launcherUserId);
            if (list.size() == 0) {
                return null;
            }
            return list.get(0);
        }

        static boolean m2590com_android_server_pm_ShortcutService$LocalService_lambda$3(String shortcutId, ShortcutInfo si) {
            return shortcutId.equals(si.getId());
        }

        public void pinShortcuts(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId) {
            Preconditions.checkStringNotEmpty(packageName, "packageName");
            Preconditions.checkNotNull(shortcutIds, "shortcutIds");
            synchronized (ShortcutService.this.mLock) {
                ShortcutLauncher launcher = ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId);
                launcher.attemptToRestoreIfNeededAndSave(ShortcutService.this);
                launcher.pinShortcuts(ShortcutService.this, userId, packageName, shortcutIds);
            }
            ShortcutService.this.packageShortcutsChanged(packageName, userId);
        }

        public Intent createShortcutIntent(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Preconditions.checkStringNotEmpty(packageName, "packageName can't be empty");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId can't be empty");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave(ShortcutService.this);
                ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId);
                if (si == null || !(si.isDynamic() || si.isPinned())) {
                    return null;
                }
                return si.getIntent();
            }
        }

        public void addListener(ShortcutServiceInternal.ShortcutChangeListener listener) {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.mListeners.add((ShortcutServiceInternal.ShortcutChangeListener) Preconditions.checkNotNull(listener));
            }
        }

        public int getShortcutIconResId(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            int iconResourceId;
            Preconditions.checkNotNull(callingPackage, "callingPackage");
            Preconditions.checkNotNull(packageName, "packageName");
            Preconditions.checkNotNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave(ShortcutService.this);
                ShortcutInfo shortcutInfo = ShortcutService.this.getPackageShortcutsLocked(packageName, userId).findShortcutById(shortcutId);
                iconResourceId = (shortcutInfo == null || !shortcutInfo.hasIconResource()) ? 0 : shortcutInfo.getIconResourceId();
            }
            return iconResourceId;
        }

        public ParcelFileDescriptor getShortcutIconFd(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Preconditions.checkNotNull(callingPackage, "callingPackage");
            Preconditions.checkNotNull(packageName, "packageName");
            Preconditions.checkNotNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave(ShortcutService.this);
                ShortcutInfo shortcutInfo = ShortcutService.this.getPackageShortcutsLocked(packageName, userId).findShortcutById(shortcutId);
                if (shortcutInfo == null || !shortcutInfo.hasIconFile()) {
                    return null;
                }
                try {
                    if (shortcutInfo.getBitmapPath() == null) {
                        Slog.w(ShortcutService.TAG, "null bitmap detected in getShortcutIconFd()");
                        return null;
                    }
                    return ParcelFileDescriptor.open(new File(shortcutInfo.getBitmapPath()), 268435456);
                } catch (FileNotFoundException e) {
                    Slog.e(ShortcutService.TAG, "Icon file not found: " + shortcutInfo.getBitmapPath());
                    return null;
                }
            }
        }

        public boolean hasShortcutHostPermission(int launcherUserId, String callingPackage) {
            return ShortcutService.this.hasShortcutHostPermission(callingPackage, launcherUserId);
        }

        public void onSystemLocaleChangedNoLock() {
        }

        void m2592com_android_server_pm_ShortcutService$LocalService_lambda$4() {
            ShortcutService.this.scheduleSaveBaseState();
        }
    }

    void checkPackageChanges(final int ownerUserId) {
        final ArrayList<ShortcutUser.PackageWithUser> gonePackages = new ArrayList<>();
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(ownerUserId);
            user.forAllPackageItems(new Consumer() {
                @Override
                public void accept(Object arg0) {
                    ShortcutService.this.m2583com_android_server_pm_ShortcutService_lambda$12(ownerUserId, gonePackages, (ShortcutPackageItem) arg0);
                }
            });
            if (gonePackages.size() > 0) {
                for (int i = gonePackages.size() - 1; i >= 0; i--) {
                    ShortcutUser.PackageWithUser pu = gonePackages.get(i);
                    cleanUpPackageLocked(pu.packageName, ownerUserId, pu.userId);
                }
            }
        }
    }

    void m2583com_android_server_pm_ShortcutService_lambda$12(int ownerUserId, ArrayList gonePackages, ShortcutPackageItem spi) {
        if (spi.getPackageInfo().isShadow()) {
            return;
        }
        int versionCode = getApplicationVersionCode(spi.getPackageName(), spi.getPackageUserId());
        if (versionCode >= 0) {
            getUserShortcutsLocked(ownerUserId).handlePackageUpdated(this, spi.getPackageName(), versionCode);
        } else {
            gonePackages.add(ShortcutUser.PackageWithUser.of(spi));
        }
    }

    private void handlePackageAdded(final String packageName, final int userId) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() {
                @Override
                public void accept(Object arg0) {
                    ShortcutService.this.m2584com_android_server_pm_ShortcutService_lambda$13(packageName, userId, (ShortcutUser) arg0);
                }
            });
        }
    }

    void m2584com_android_server_pm_ShortcutService_lambda$13(String packageName, int userId, ShortcutUser user) {
        user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
    }

    private void handlePackageUpdateFinished(final String packageName, final int userId) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() {
                @Override
                public void accept(Object arg0) {
                    ShortcutService.this.m2585com_android_server_pm_ShortcutService_lambda$14(packageName, userId, (ShortcutUser) arg0);
                }
            });
            int versionCode = getApplicationVersionCode(packageName, userId);
            if (versionCode < 0) {
                return;
            }
            getUserShortcutsLocked(userId).handlePackageUpdated(this, packageName, versionCode);
        }
    }

    void m2585com_android_server_pm_ShortcutService_lambda$14(String packageName, int userId, ShortcutUser user) {
        user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
    }

    private void handlePackageRemoved(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId);
    }

    private void handlePackageDataCleared(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId);
    }

    PackageInfo getPackageInfoWithSignatures(String packageName, int userId) {
        return injectPackageInfo(packageName, userId, true);
    }

    int injectGetPackageUid(String packageName, int userId) {
        long token = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getPackageUid(packageName, PACKAGE_MATCH_FLAGS, userId);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return -1;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    PackageInfo injectPackageInfo(String packageName, int userId, boolean getSignatures) {
        long start = System.currentTimeMillis();
        long token = injectClearCallingIdentity();
        try {
            try {
                PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(packageName, (getSignatures ? 64 : 0) | PACKAGE_MATCH_FLAGS, userId);
                injectRestoreCallingIdentity(token);
                logDurationStat(getSignatures ? 2 : 1, start);
                return packageInfo;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "RemoteException", e);
                injectRestoreCallingIdentity(token);
                logDurationStat(getSignatures ? 2 : 1, start);
                return null;
            }
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(getSignatures ? 2 : 1, start);
            throw th;
        }
    }

    ApplicationInfo injectApplicationInfo(String packageName, int userId) {
        long start = System.currentTimeMillis();
        long token = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getApplicationInfo(packageName, PACKAGE_MATCH_FLAGS, userId);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(3, start);
        }
    }

    private boolean isApplicationFlagSet(String packageName, int userId, int flags) {
        ApplicationInfo ai = injectApplicationInfo(packageName, userId);
        return ai != null && (ai.flags & flags) == flags;
    }

    boolean isPackageInstalled(String packageName, int userId) {
        return isApplicationFlagSet(packageName, userId, 8388608);
    }

    int getApplicationVersionCode(String packageName, int userId) {
        ApplicationInfo ai = injectApplicationInfo(packageName, userId);
        if (ai == null || (ai.flags & 8388608) == 0) {
            return -1;
        }
        return ai.versionCode;
    }

    boolean shouldBackupApp(String packageName, int userId) {
        return isApplicationFlagSet(packageName, userId, PackageManagerService.DumpState.DUMP_VERSION);
    }

    boolean shouldBackupApp(PackageInfo pi) {
        return (pi.applicationInfo.flags & PackageManagerService.DumpState.DUMP_VERSION) != 0;
    }

    public byte[] getBackupPayload(int userId) {
        enforceSystem();
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(userId);
            if (user == null) {
                Slog.w(TAG, "Can't backup: user not found: id=" + userId);
                return null;
            }
            user.forAllPackageItems(new Consumer() {
                @Override
                public void accept(Object arg0) {
                    ShortcutService.this.m2586com_android_server_pm_ShortcutService_lambda$15((ShortcutPackageItem) arg0);
                }
            });
            ByteArrayOutputStream os = new ByteArrayOutputStream(PackageManagerService.DumpState.DUMP_VERSION);
            try {
                saveUserInternalLocked(userId, os, true);
                return os.toByteArray();
            } catch (IOException | XmlPullParserException e) {
                Slog.w(TAG, "Backup failed.", e);
                return null;
            }
        }
    }

    void m2586com_android_server_pm_ShortcutService_lambda$15(ShortcutPackageItem spi) {
        spi.refreshPackageInfoAndSave(this);
    }

    public void applyRestore(byte[] payload, int userId) {
        enforceSystem();
        ByteArrayInputStream is = new ByteArrayInputStream(payload);
        try {
            ShortcutUser user = loadUserInternal(userId, is, true);
            synchronized (this.mLock) {
                this.mUsers.put(userId, user);
                File bitmapPath = getUserBitmapFilePath(userId);
                boolean success = FileUtils.deleteContents(bitmapPath);
                if (!success) {
                    Slog.w(TAG, "Failed to delete " + bitmapPath);
                }
                saveUserLocked(userId);
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.w(TAG, "Restoration failed.", e);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump UserManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
        } else {
            dumpInner(pw, args);
        }
    }

    void dumpInner(PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            long now = injectCurrentTimeMillis();
            pw.print("Now: [");
            pw.print(now);
            pw.print("] ");
            pw.print(formatTime(now));
            pw.print("  Raw last reset: [");
            pw.print(this.mRawLastResetTime);
            pw.print("] ");
            pw.print(formatTime(this.mRawLastResetTime));
            long last = getLastResetTimeLocked();
            pw.print("  Last reset: [");
            pw.print(last);
            pw.print("] ");
            pw.print(formatTime(last));
            long next = getNextResetTimeLocked();
            pw.print("  Next reset: [");
            pw.print(next);
            pw.print("] ");
            pw.print(formatTime(next));
            pw.print("  Locale change seq#: ");
            pw.print(this.mLocaleChangeSequenceNumber.get());
            pw.println();
            pw.print("  Config:");
            pw.print("    Max icon dim: ");
            pw.println(this.mMaxIconDimension);
            pw.print("    Icon format: ");
            pw.println(this.mIconPersistFormat);
            pw.print("    Icon quality: ");
            pw.println(this.mIconPersistQuality);
            pw.print("    saveDelayMillis: ");
            pw.println(this.mSaveDelayMillis);
            pw.print("    resetInterval: ");
            pw.println(this.mResetInterval);
            pw.print("    maxUpdatesPerInterval: ");
            pw.println(this.mMaxUpdatesPerInterval);
            pw.print("    maxDynamicShortcuts: ");
            pw.println(this.mMaxDynamicShortcuts);
            pw.println();
            pw.println("  Stats:");
            synchronized (this.mStatLock) {
                dumpStatLS(pw, "    ", 0, "getHomeActivities()");
                dumpStatLS(pw, "    ", 4, "Launcher permission check");
                dumpStatLS(pw, "    ", 1, "getPackageInfo()");
                dumpStatLS(pw, "    ", 2, "getPackageInfo(SIG)");
                dumpStatLS(pw, "    ", 3, "getApplicationInfo");
            }
            for (int i = 0; i < this.mUsers.size(); i++) {
                pw.println();
                this.mUsers.valueAt(i).dump(this, pw, "  ");
            }
            pw.println();
            pw.println("  UID state:");
            for (int i2 = 0; i2 < this.mUidState.size(); i2++) {
                int uid = this.mUidState.keyAt(i2);
                int state = this.mUidState.valueAt(i2);
                pw.print("    UID=");
                pw.print(uid);
                pw.print(" state=");
                pw.print(state);
                if (isProcessStateForeground(state)) {
                    pw.print("  [FG]");
                }
                pw.print("  last FG=");
                pw.print(this.mUidLastForegroundElapsedTime.get(uid));
                pw.println();
            }
        }
    }

    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    private void dumpStatLS(PrintWriter pw, String prefix, int statId, String label) {
        pw.print(prefix);
        int count = this.mCountStats[statId];
        long dur = this.mDurationStats[statId];
        Object[] objArr = new Object[4];
        objArr[0] = label;
        objArr[1] = Integer.valueOf(count);
        objArr[2] = Long.valueOf(dur);
        objArr[3] = Double.valueOf(count == 0 ? 0.0d : dur / ((double) count));
        pw.println(String.format("%s: count=%d, total=%dms, avg=%.1fms", objArr));
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
        enforceShell();
        new MyShellCommand(this, null).exec(this, in, out, err, args, resultReceiver);
    }

    static class CommandException extends Exception {
        public CommandException(String message) {
            super(message);
        }
    }

    private class MyShellCommand extends ShellCommand {
        private int mUserId;

        MyShellCommand(ShortcutService this$0, MyShellCommand myShellCommand) {
            this();
        }

        private MyShellCommand() {
            this.mUserId = 0;
        }

        private void parseOptions(boolean takeUser) throws CommandException {
            String opt;
            while (true) {
                opt = getNextOption();
                if (opt == null) {
                    return;
                }
                if (!opt.equals("--user") || !takeUser) {
                    break;
                } else {
                    this.mUserId = UserHandle.parseUserArg(getNextArgRequired());
                }
            }
            throw new CommandException("Unknown option: " + opt);
        }

        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                if (cmd.equals("reset-package-throttling")) {
                    handleResetPackageThrottling();
                } else if (cmd.equals("reset-throttling")) {
                    handleResetThrottling();
                } else if (cmd.equals("reset-all-throttling")) {
                    handleResetAllThrottling();
                } else if (cmd.equals("override-config")) {
                    handleOverrideConfig();
                } else if (cmd.equals("reset-config")) {
                    handleResetConfig();
                } else if (cmd.equals("clear-default-launcher")) {
                    handleClearDefaultLauncher();
                } else if (cmd.equals("get-default-launcher")) {
                    handleGetDefaultLauncher();
                } else if (cmd.equals("refresh-default-launcher")) {
                    handleRefreshDefaultLauncher();
                } else if (cmd.equals("unload-user")) {
                    handleUnloadUser();
                } else {
                    if (!cmd.equals("clear-shortcuts")) {
                        return handleDefaultCommands(cmd);
                    }
                    handleClearShortcuts();
                }
                pw.println("Success");
                return 0;
            } catch (CommandException e) {
                pw.println("Error: " + e.getMessage());
                return 1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Usage: cmd shortcut COMMAND [options ...]");
            pw.println();
            pw.println("cmd shortcut reset-package-throttling [--user USER_ID] PACKAGE");
            pw.println("    Reset throttling for a package");
            pw.println();
            pw.println("cmd shortcut reset-throttling [--user USER_ID]");
            pw.println("    Reset throttling for all packages and users");
            pw.println();
            pw.println("cmd shortcut reset-all-throttling");
            pw.println("    Reset the throttling state for all users");
            pw.println();
            pw.println("cmd shortcut override-config CONFIG");
            pw.println("    Override the configuration for testing (will last until reboot)");
            pw.println();
            pw.println("cmd shortcut reset-config");
            pw.println("    Reset the configuration set with \"update-config\"");
            pw.println();
            pw.println("cmd shortcut clear-default-launcher [--user USER_ID]");
            pw.println("    Clear the cached default launcher");
            pw.println();
            pw.println("cmd shortcut get-default-launcher [--user USER_ID]");
            pw.println("    Show the cached default launcher");
            pw.println();
            pw.println("cmd shortcut refresh-default-launcher [--user USER_ID]");
            pw.println("    Refresh the cached default launcher");
            pw.println();
            pw.println("cmd shortcut unload-user [--user USER_ID]");
            pw.println("    Unload a user from the memory");
            pw.println("    (This should not affect any observable behavior)");
            pw.println();
            pw.println("cmd shortcut clear-shortcuts [--user USER_ID] PACKAGE");
            pw.println("    Remove all shortcuts from a package, including pinned shortcuts");
            pw.println();
        }

        private void handleResetThrottling() throws CommandException {
            parseOptions(true);
            Slog.i(ShortcutService.TAG, "cmd: handleResetThrottling");
            ShortcutService.this.resetThrottlingInner(this.mUserId);
        }

        private void handleResetAllThrottling() {
            Slog.i(ShortcutService.TAG, "cmd: handleResetAllThrottling");
            ShortcutService.this.resetAllThrottlingInner();
        }

        private void handleResetPackageThrottling() throws CommandException {
            parseOptions(true);
            String packageName = getNextArgRequired();
            Slog.i(ShortcutService.TAG, "cmd: handleResetPackageThrottling: " + packageName);
            ShortcutService.this.resetPackageThrottling(packageName, this.mUserId);
        }

        private void handleOverrideConfig() throws CommandException {
            String config = getNextArgRequired();
            Slog.i(ShortcutService.TAG, "cmd: handleOverrideConfig: " + config);
            synchronized (ShortcutService.this.mLock) {
                if (!ShortcutService.this.updateConfigurationLocked(config)) {
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
                ShortcutService.this.getUserShortcutsLocked(this.mUserId).setLauncherComponent(ShortcutService.this, null);
            }
        }

        private void showLauncher() {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.hasShortcutHostPermissionInner("-", this.mUserId);
                getOutPrintWriter().println("Launcher: " + ShortcutService.this.getUserShortcutsLocked(this.mUserId).getLauncherComponent());
            }
        }

        private void handleClearDefaultLauncher() throws CommandException {
            parseOptions(true);
            clearLauncher();
        }

        private void handleGetDefaultLauncher() throws CommandException {
            parseOptions(true);
            showLauncher();
        }

        private void handleRefreshDefaultLauncher() throws CommandException {
            parseOptions(true);
            clearLauncher();
            showLauncher();
        }

        private void handleUnloadUser() throws CommandException {
            parseOptions(true);
            Slog.i(ShortcutService.TAG, "cmd: handleUnloadUser: " + this.mUserId);
            ShortcutService.this.handleCleanupUser(this.mUserId);
        }

        private void handleClearShortcuts() throws CommandException {
            parseOptions(true);
            String packageName = getNextArgRequired();
            Slog.i(ShortcutService.TAG, "cmd: handleClearShortcuts: " + this.mUserId + ", " + packageName);
            ShortcutService.this.cleanUpPackageForAllLoadedUsers(packageName, this.mUserId);
        }
    }

    long injectCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    long injectElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    int injectBinderCallingUid() {
        return getCallingUid();
    }

    private int getCallingUserId() {
        return UserHandle.getUserId(injectBinderCallingUid());
    }

    long injectClearCallingIdentity() {
        return Binder.clearCallingIdentity();
    }

    void injectRestoreCallingIdentity(long token) {
        Binder.restoreCallingIdentity(token);
    }

    final void wtf(String message) {
        wtf(message, null);
    }

    void wtf(String message, Exception e) {
        Slog.wtf(TAG, message, e);
    }

    File injectSystemDataPath() {
        return Environment.getDataSystemDirectory();
    }

    File injectUserDataPath(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), DIRECTORY_PER_USER);
    }

    boolean injectIsLowRamDevice() {
        return ActivityManager.isLowRamDeviceStatic();
    }

    void injectRegisterUidObserver(IUidObserver observer, int which) {
        try {
            ActivityManagerNative.getDefault().registerUidObserver(observer, which);
        } catch (RemoteException e) {
        }
    }

    PackageManagerInternal injectPackageManagerInternal() {
        return this.mPackageManagerInternal;
    }

    File getUserBitmapFilePath(int userId) {
        return new File(injectUserDataPath(userId), DIRECTORY_BITMAPS);
    }

    SparseArray<ShortcutUser> getShortcutsForTest() {
        return this.mUsers;
    }

    int getMaxDynamicShortcutsForTest() {
        return this.mMaxDynamicShortcuts;
    }

    int getMaxUpdatesPerIntervalForTest() {
        return this.mMaxUpdatesPerInterval;
    }

    long getResetIntervalForTest() {
        return this.mResetInterval;
    }

    int getMaxIconDimensionForTest() {
        return this.mMaxIconDimension;
    }

    Bitmap.CompressFormat getIconPersistFormatForTest() {
        return this.mIconPersistFormat;
    }

    int getIconPersistQualityForTest() {
        return this.mIconPersistQuality;
    }

    ShortcutInfo getPackageShortcutForTest(String packageName, String shortcutId, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = this.mUsers.get(userId);
            if (user == null) {
                return null;
            }
            ShortcutPackage pkg = user.getAllPackagesForTest().get(packageName);
            if (pkg == null) {
                return null;
            }
            return pkg.findShortcutById(shortcutId);
        }
    }
}
