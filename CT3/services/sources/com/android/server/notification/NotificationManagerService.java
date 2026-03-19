package com.android.server.notification;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.app.usage.UsageStatsManagerInternal;
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
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.IRingtonePlayer;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.NotificationRankingUpdate;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.server.DeviceIdleController;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.audio.AudioService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.vr.VrManagerInternal;
import com.mediatek.common.dm.DmAgent;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NotificationManagerService extends SystemService {
    private static final String ATTR_VERSION = "version";
    static final boolean DBG = true;
    private static final int DB_VERSION = 1;
    static final float DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE = 50.0f;
    static final int DEFAULT_STREAM_TYPE = 5;
    static final boolean ENABLE_BLOCKED_NOTIFICATIONS = true;
    static final boolean ENABLE_BLOCKED_TOASTS = true;
    private static final int EVENTLOG_ENQUEUE_STATUS_IGNORED = 2;
    private static final int EVENTLOG_ENQUEUE_STATUS_NEW = 0;
    private static final int EVENTLOG_ENQUEUE_STATUS_UPDATE = 1;
    static final int LONG_DELAY = 3500;
    static final int MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS = 3000;
    static final float MATCHES_CALL_FILTER_TIMEOUT_AFFINITY = 1.0f;
    static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    static final int MESSAGE_LISTENER_HINTS_CHANGED = 5;
    static final int MESSAGE_LISTENER_NOTIFICATION_FILTER_CHANGED = 6;
    private static final int MESSAGE_RANKING_SORT = 1001;
    private static final int MESSAGE_RECONSIDER_RANKING = 1000;
    static final int MESSAGE_SAVE_POLICY_FILE = 3;
    static final int MESSAGE_SEND_RANKING_UPDATE = 4;
    static final int MESSAGE_TIMEOUT = 2;
    private static final long MIN_PACKAGE_OVERRATE_LOG_INTERVAL = 5000;
    public static final String OMADM_LAWMO_LOCK = "com.mediatek.dm.LAWMO_LOCK";
    public static final String OMADM_LAWMO_UNLOCK = "com.mediatek.dm.LAWMO_UNLOCK";
    public static final String PPL_LOCK = "com.mediatek.ppl.NOTIFY_LOCK";
    public static final String PPL_UNLOCK = "com.mediatek.ppl.NOTIFY_UNLOCK";
    static final int SHORT_DELAY = 2000;
    static final String TAG = "NotificationService";
    private static final String TAG_NOTIFICATION_POLICY = "notification-policy";
    static final int VIBRATE_PATTERN_MAXLEN = 17;
    private IActivityManager mAm;
    private AppOpsManager mAppOps;
    private UsageStatsManagerInternal mAppUsageStats;
    private Archive mArchive;
    Light mAttentionLight;
    AudioManager mAudioManager;
    AudioManagerInternal mAudioManagerInternal;
    final ArrayMap<Integer, ArrayMap<String, String>> mAutobundledSummaries;
    private final Runnable mBuzzBeepBlinked;
    private int mCallState;
    private ConditionProviders mConditionProviders;
    private int mDefaultNotificationColor;
    private int mDefaultNotificationLedOff;
    private int mDefaultNotificationLedOn;
    private long[] mDefaultVibrationPattern;
    private boolean mDisableNotificationEffects;
    int mDisabledNotifications;
    private boolean mDmLock;
    private List<ComponentName> mEffectsSuppressors;
    private long[] mFallbackVibrationPattern;
    final IBinder mForegroundToken;
    private Handler mHandler;
    private boolean mInCall;
    private final BroadcastReceiver mIntentReceiver;
    private final NotificationManagerInternal mInternalService;
    private int mInterruptionFilter;
    private long mLastOverRateLogTime;
    ArrayList<String> mLights;
    private int mListenerHints;
    private NotificationListeners mListeners;
    private final SparseArray<ArraySet<ManagedServices.ManagedServiceInfo>> mListenersDisablingEffects;
    private float mMaxPackageEnqueueRate;
    private final NotificationDelegate mNotificationDelegate;
    private Light mNotificationLight;
    final ArrayList<NotificationRecord> mNotificationList;
    private boolean mNotificationPulseEnabled;
    final ArrayMap<String, NotificationRecord> mNotificationsByKey;
    private final BroadcastReceiver mPackageIntentReceiver;
    final PolicyAccess mPolicyAccess;
    private AtomicFile mPolicyFile;
    private boolean mPplLock;
    private String mRankerServicePackageName;
    private NotificationRankers mRankerServices;
    private RankingHandler mRankingHandler;
    private RankingHelper mRankingHelper;
    private final HandlerThread mRankingThread;
    private boolean mScreenOn;
    private final IBinder mService;
    private SettingsObserver mSettingsObserver;
    private String mSoundNotificationKey;
    StatusBarManagerInternal mStatusBar;
    final ArrayMap<String, NotificationRecord> mSummaryByGroupKey;
    boolean mSystemReady;
    final ArrayList<ToastRecord> mToastQueue;
    private NotificationUsageStats mUsageStats;
    private boolean mUseAttentionLight;
    private final ManagedServices.UserProfiles mUserProfiles;
    private String mVibrateNotificationKey;
    Vibrator mVibrator;
    private VrManagerInternal mVrManagerInternal;
    private ZenModeHelper mZenModeHelper;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final int MY_UID = Process.myUid();
    private static final int MY_PID = Process.myPid();

    private static class Archive {
        final ArrayDeque<StatusBarNotification> mBuffer;
        final int mBufferSize;

        public Archive(int size) {
            this.mBufferSize = size;
            this.mBuffer = new ArrayDeque<>(this.mBufferSize);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int N = this.mBuffer.size();
            sb.append("Archive (");
            sb.append(N);
            sb.append(" notification");
            sb.append(N == 1 ? ")" : "s)");
            return sb.toString();
        }

        public void record(StatusBarNotification nr) {
            if (this.mBuffer.size() == this.mBufferSize) {
                this.mBuffer.removeFirst();
            }
            this.mBuffer.addLast(nr.cloneLight());
        }

        public Iterator<StatusBarNotification> descendingIterator() {
            return this.mBuffer.descendingIterator();
        }

        public StatusBarNotification[] getArray(int count) {
            if (count == 0) {
                count = this.mBufferSize;
            }
            StatusBarNotification[] a = new StatusBarNotification[Math.min(count, this.mBuffer.size())];
            Iterator<StatusBarNotification> iter = descendingIterator();
            for (int i = 0; iter.hasNext() && i < count; i++) {
                a[i] = iter.next();
            }
            return a;
        }
    }

    private void readPolicyXml(InputStream stream, boolean forRestore) throws XmlPullParserException, IOException, NumberFormatException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, StandardCharsets.UTF_8.name());
        while (parser.next() != 1) {
            this.mZenModeHelper.readXml(parser, forRestore);
            this.mRankingHelper.readXml(parser, forRestore);
        }
    }

    private void loadPolicyFile() {
        Slog.d(TAG, "loadPolicyFile");
        synchronized (this.mPolicyFile) {
            FileInputStream infile = null;
            try {
                try {
                    try {
                        infile = this.mPolicyFile.openRead();
                        readPolicyXml(infile, false);
                    } catch (NumberFormatException e) {
                        Log.wtf(TAG, "Unable to parse notification policy", e);
                        IoUtils.closeQuietly(infile);
                    } catch (XmlPullParserException e2) {
                        Log.wtf(TAG, "Unable to parse notification policy", e2);
                    }
                } catch (FileNotFoundException e3) {
                    IoUtils.closeQuietly(infile);
                } catch (IOException e4) {
                    Log.wtf(TAG, "Unable to read notification policy", e4);
                    IoUtils.closeQuietly(infile);
                }
            } finally {
                IoUtils.closeQuietly(infile);
            }
        }
    }

    public void savePolicyFile() {
        this.mHandler.removeMessages(3);
        this.mHandler.sendEmptyMessage(3);
    }

    private void handleSavePolicyFile() {
        Slog.d(TAG, "handleSavePolicyFile");
        synchronized (this.mPolicyFile) {
            try {
                FileOutputStream stream = this.mPolicyFile.startWrite();
                try {
                    writePolicyXml(stream, false);
                    this.mPolicyFile.finishWrite(stream);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to save policy file, restoring backup", e);
                    this.mPolicyFile.failWrite(stream);
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to save policy file", e2);
                return;
            }
        }
        BackupManager.dataChanged(getContext().getPackageName());
    }

    private void writePolicyXml(OutputStream stream, boolean forBackup) throws IOException {
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(stream, StandardCharsets.UTF_8.name());
        out.startDocument(null, true);
        out.startTag(null, TAG_NOTIFICATION_POLICY);
        out.attribute(null, ATTR_VERSION, Integer.toString(1));
        this.mZenModeHelper.writeXml(out, forBackup);
        this.mRankingHelper.writeXml(out, forBackup);
        out.endTag(null, TAG_NOTIFICATION_POLICY);
        out.endDocument();
    }

    private boolean noteNotificationOp(String pkg, int uid) {
        if (this.mAppOps.noteOpNoThrow(11, uid, pkg) != 0) {
            Slog.v(TAG, "notifications are disabled by AppOps for " + pkg);
            return false;
        }
        return true;
    }

    private boolean checkNotificationOp(String pkg, int uid) {
        return this.mAppOps.checkOp(11, uid, pkg) == 0 && !isPackageSuspendedForUser(pkg, uid);
    }

    private static final class ToastRecord {
        final ITransientNotification callback;
        int duration;
        final int pid;
        final String pkg;

        ToastRecord(int pid, String pkg, ITransientNotification callback, int duration) {
            this.pid = pid;
            this.pkg = pkg;
            this.callback = callback;
            this.duration = duration;
        }

        void update(int duration) {
            this.duration = duration;
        }

        void dump(PrintWriter pw, String prefix, DumpFilter filter) {
            if (filter != null && !filter.matches(this.pkg)) {
                return;
            }
            pw.println(prefix + this);
        }

        public final String toString() {
            return "ToastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " pkg=" + this.pkg + " callback=" + this.callback + " duration=" + this.duration;
        }
    }

    private void clearSoundLocked() {
        this.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
            if (player != null) {
                player.stopAsync();
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void clearVibrateLocked() {
        this.mVibrateNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            this.mVibrator.cancel();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void clearLightsLocked() {
        this.mLights.clear();
        updateLightsLocked();
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;
        private final Uri NOTIFICATION_RATE_LIMIT_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.NOTIFICATION_RATE_LIMIT_URI = Settings.Global.getUriFor("max_notification_enqueue_rate");
        }

        void observe() {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_RATE_LIMIT_URI, false, this, -1);
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                boolean pulseEnabled = Settings.System.getInt(resolver, "notification_light_pulse", 0) != 0;
                if (NotificationManagerService.this.mNotificationPulseEnabled != pulseEnabled) {
                    NotificationManagerService.this.mNotificationPulseEnabled = pulseEnabled;
                    NotificationManagerService.this.updateNotificationPulse();
                }
            }
            if (uri != null && !this.NOTIFICATION_RATE_LIMIT_URI.equals(uri)) {
                return;
            }
            NotificationManagerService.this.mMaxPackageEnqueueRate = Settings.Global.getFloat(resolver, "max_notification_enqueue_rate", NotificationManagerService.this.mMaxPackageEnqueueRate);
        }
    }

    static long[] getLongArray(Resources r, int resid, int maxlen, long[] def) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return def;
        }
        int len = ar.length > maxlen ? maxlen : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    public NotificationManagerService(Context context) {
        super(context);
        this.mForegroundToken = new Binder();
        this.mRankingThread = new HandlerThread("ranker", 10);
        this.mListenersDisablingEffects = new SparseArray<>();
        this.mEffectsSuppressors = new ArrayList();
        this.mInterruptionFilter = 0;
        this.mScreenOn = true;
        this.mInCall = false;
        this.mNotificationList = new ArrayList<>();
        this.mNotificationsByKey = new ArrayMap<>();
        this.mAutobundledSummaries = new ArrayMap<>();
        this.mToastQueue = new ArrayList<>();
        this.mSummaryByGroupKey = new ArrayMap<>();
        this.mPolicyAccess = new PolicyAccess(this, null);
        this.mLights = new ArrayList<>();
        this.mUserProfiles = new ManagedServices.UserProfiles();
        this.mDmLock = false;
        this.mPplLock = false;
        this.mMaxPackageEnqueueRate = DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE;
        this.mNotificationDelegate = new NotificationDelegate() {
            @Override
            public void onSetDisabled(int status) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationManagerService.this.mDisabledNotifications = status;
                    NotificationManagerService.this.mDisableNotificationEffects = (262144 & status) != 0;
                    if (NotificationManagerService.this.disableNotificationEffects(null) != null) {
                        long identity = Binder.clearCallingIdentity();
                        try {
                            IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                            if (player != null) {
                                player.stopAsync();
                            }
                        } catch (RemoteException e) {
                        } finally {
                        }
                        identity = Binder.clearCallingIdentity();
                        try {
                            NotificationManagerService.this.mVibrator.cancel();
                        } finally {
                        }
                    }
                }
            }

            @Override
            public void onClearAll(int callingUid, int callingPid, int userId) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, userId, 3, null, true);
                }
            }

            @Override
            public void onNotificationClick(int callingUid, int callingPid, String key) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r == null) {
                        Log.w(NotificationManagerService.TAG, "No notification with key: " + key);
                        return;
                    }
                    long now = System.currentTimeMillis();
                    EventLogTags.writeNotificationClicked(key, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now));
                    StatusBarNotification sbn = r.sbn;
                    NotificationManagerService.this.cancelNotification(callingUid, callingPid, sbn.getPackageName(), sbn.getTag(), sbn.getId(), 16, 64, false, r.getUserId(), 1, null);
                }
            }

            @Override
            public void onNotificationActionClick(int callingUid, int callingPid, String key, int actionIndex) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r == null) {
                        Log.w(NotificationManagerService.TAG, "No notification with key: " + key);
                    } else {
                        long now = System.currentTimeMillis();
                        EventLogTags.writeNotificationActionClicked(key, actionIndex, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now));
                    }
                }
            }

            @Override
            public void onNotificationClear(int callingUid, int callingPid, String pkg, String tag, int id, int userId) {
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 66, true, userId, 2, null);
            }

            @Override
            public void onPanelRevealed(boolean clearEffects, int items) {
                EventLogTags.writeNotificationPanelRevealed(items);
                if (!clearEffects) {
                    return;
                }
                clearEffects();
            }

            @Override
            public void onPanelHidden() {
                EventLogTags.writeNotificationPanelHidden();
            }

            @Override
            public void clearEffects() {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    Slog.d(NotificationManagerService.TAG, "clearEffects");
                    NotificationManagerService.this.clearSoundLocked();
                    NotificationManagerService.this.clearVibrateLocked();
                    NotificationManagerService.this.clearLightsLocked();
                }
            }

            @Override
            public void onNotificationError(int callingUid, int callingPid, String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
                Slog.d(NotificationManagerService.TAG, "onNotification error pkg=" + pkg + " tag=" + tag + " id=" + id + "; will crashApplication(uid=" + uid + ", pid=" + initialPid + ")");
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 0, false, userId, 4, null);
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityManagerNative.getDefault().crashApplication(uid, initialPid, pkg, "Bad notification posted from package " + pkg + ": " + message);
                } catch (RemoteException e) {
                }
                Binder.restoreCallingIdentity(ident);
            }

            @Override
            public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    for (NotificationVisibility nv : newlyVisibleKeys) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(nv.key);
                        if (r != null) {
                            r.setVisibility(true, nv.rank);
                            nv.recycle();
                        }
                    }
                    for (NotificationVisibility nv2 : noLongerVisibleKeys) {
                        NotificationRecord r2 = NotificationManagerService.this.mNotificationsByKey.get(nv2.key);
                        if (r2 != null) {
                            r2.setVisibility(false, nv2.rank);
                            nv2.recycle();
                        }
                    }
                }
            }

            @Override
            public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r != null) {
                        r.stats.onExpansionChanged(userAction, expanded);
                        long now = System.currentTimeMillis();
                        EventLogTags.writeNotificationExpansion(key, userAction ? 1 : 0, expanded ? 1 : 0, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now));
                    }
                }
            }
        };
        this.mPackageIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String pkgName;
                String[] pkgList;
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                boolean queryRestart = false;
                boolean queryRemove = false;
                boolean packageChanged = false;
                boolean cancelNotifications = true;
                int reason = 5;
                if (action.equals("android.intent.action.PACKAGE_ADDED") || (queryRemove = action.equals("android.intent.action.PACKAGE_REMOVED")) || action.equals("android.intent.action.PACKAGE_RESTARTED") || (packageChanged = action.equals("android.intent.action.PACKAGE_CHANGED")) || (queryRestart = action.equals("android.intent.action.QUERY_PACKAGE_RESTART")) || action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE") || action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    int changeUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    boolean booleanExtra = queryRemove ? intent.getBooleanExtra("android.intent.extra.REPLACING", false) : false;
                    Slog.i(NotificationManagerService.TAG, "action=" + action + " queryReplace=" + booleanExtra);
                    if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                        pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                    } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                        pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        reason = 14;
                    } else if (queryRestart) {
                        pkgList = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                    } else {
                        Uri uri = intent.getData();
                        if (uri == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
                            return;
                        }
                        if (packageChanged) {
                            try {
                                IPackageManager pm = AppGlobals.getPackageManager();
                                int enabled = pm.getApplicationEnabledSetting(pkgName, changeUserId != -1 ? changeUserId : 0);
                                if (enabled == 1 || enabled == 0) {
                                    cancelNotifications = false;
                                }
                            } catch (RemoteException e) {
                            } catch (IllegalArgumentException e2) {
                                Slog.i(NotificationManagerService.TAG, "Exception trying to look up app enabled setting", e2);
                            }
                        }
                        pkgList = new String[]{pkgName};
                    }
                    if (pkgList != null && pkgList.length > 0) {
                        int i = 0;
                        int length = pkgList.length;
                        while (true) {
                            int i2 = i;
                            if (i2 >= length) {
                                break;
                            }
                            String pkgName2 = pkgList[i2];
                            if (cancelNotifications) {
                                NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkgName2, 0, 0, !queryRestart, changeUserId, reason, null);
                            }
                            i = i2 + 1;
                        }
                    }
                    NotificationManagerService.this.mListeners.onPackagesChanged(booleanExtra, pkgList);
                    NotificationManagerService.this.mRankerServices.onPackagesChanged(booleanExtra, pkgList);
                    NotificationManagerService.this.mConditionProviders.onPackagesChanged(booleanExtra, pkgList);
                    NotificationManagerService.this.mRankingHelper.onPackagesChanged(booleanExtra, pkgList);
                }
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    NotificationManagerService.this.mScreenOn = true;
                    NotificationManagerService.this.updateNotificationPulse();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    NotificationManagerService.this.mScreenOn = false;
                    NotificationManagerService.this.updateNotificationPulse();
                    return;
                }
                if (action.equals("android.intent.action.PHONE_STATE")) {
                    NotificationManagerService.this.mInCall = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra(AudioService.CONNECT_INTENT_KEY_STATE));
                    NotificationManagerService.this.updateNotificationPulse();
                    return;
                }
                if (action.equals("android.intent.action.USER_STOPPED")) {
                    int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle < 0) {
                        return;
                    }
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, 0, 0, true, userHandle, 6, null);
                    return;
                }
                if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                    int userHandle2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle2 < 0) {
                        return;
                    }
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, 0, 0, true, userHandle2, 15, null);
                    return;
                }
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                    if (NotificationManagerService.this.mStatusBar == null) {
                        return;
                    }
                    NotificationManagerService.this.mStatusBar.notificationLightOff();
                    return;
                }
                if (action.equals("android.intent.action.USER_SWITCHED")) {
                    int user = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mSettingsObserver.update(null);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    NotificationManagerService.this.mConditionProviders.onUserSwitched(user);
                    NotificationManagerService.this.mListeners.onUserSwitched(user);
                    NotificationManagerService.this.mRankerServices.onUserSwitched(user);
                    NotificationManagerService.this.mZenModeHelper.onUserSwitched(user);
                    return;
                }
                if (action.equals("android.intent.action.USER_ADDED")) {
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    return;
                }
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    NotificationManagerService.this.mZenModeHelper.onUserRemoved(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                    return;
                }
                if (action.equals("android.intent.action.USER_UNLOCKED")) {
                    int user2 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mConditionProviders.onUserUnlocked(user2);
                    NotificationManagerService.this.mListeners.onUserUnlocked(user2);
                    NotificationManagerService.this.mRankerServices.onUserUnlocked(user2);
                    NotificationManagerService.this.mZenModeHelper.onUserUnlocked(user2);
                    return;
                }
                if (action.equals(NotificationManagerService.OMADM_LAWMO_LOCK)) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                    NotificationManagerService.this.mDmLock = true;
                } else {
                    if (action.equals(NotificationManagerService.OMADM_LAWMO_UNLOCK)) {
                        NotificationManagerService.this.mDmLock = false;
                        return;
                    }
                    if (action.equals(NotificationManagerService.PPL_LOCK)) {
                        NotificationManagerService.this.mNotificationLight.turnOff();
                        NotificationManagerService.this.mPplLock = true;
                    } else {
                        if (!action.equals(NotificationManagerService.PPL_UNLOCK)) {
                            return;
                        }
                        NotificationManagerService.this.mPplLock = false;
                    }
                }
            }
        };
        this.mBuzzBeepBlinked = new Runnable() {
            @Override
            public void run() {
                if (NotificationManagerService.this.mStatusBar == null) {
                    return;
                }
                NotificationManagerService.this.mStatusBar.buzzBeepBlinked();
            }
        };
        this.mService = new INotificationManager.Stub() {
            public void enqueueToast(String pkg, ITransientNotification callback, int duration) {
                String str;
                Slog.i(NotificationManagerService.TAG, "enqueueToast pkg=" + pkg + " callback=" + callback + " duration=" + duration);
                if (pkg == null || callback == null) {
                    Slog.e(NotificationManagerService.TAG, "Not doing toast. pkg=" + pkg + " callback=" + callback);
                    return;
                }
                boolean zEquals = !NotificationManagerService.isCallerSystem() ? "android".equals(pkg) : true;
                boolean isPackageSuspended = NotificationManagerService.this.isPackageSuspendedForUser(pkg, Binder.getCallingUid());
                if ((!NotificationManagerService.this.noteNotificationOp(pkg, Binder.getCallingUid()) || isPackageSuspended) && !zEquals) {
                    StringBuilder sbAppend = new StringBuilder().append("Suppressing toast from package ").append(pkg);
                    if (isPackageSuspended) {
                        str = " due to package suspended by administrator.";
                    } else {
                        str = " by user request.";
                    }
                    Slog.e(NotificationManagerService.TAG, sbAppend.append(str).toString());
                    return;
                }
                synchronized (NotificationManagerService.this.mToastQueue) {
                    int callingPid = Binder.getCallingPid();
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int index = NotificationManagerService.this.indexOfToastLocked(pkg, callback);
                        if (index >= 0) {
                            ToastRecord record = NotificationManagerService.this.mToastQueue.get(index);
                            record.update(duration);
                        } else {
                            if (!zEquals) {
                                int count = 0;
                                int N = NotificationManagerService.this.mToastQueue.size();
                                for (int i = 0; i < N; i++) {
                                    ToastRecord r = NotificationManagerService.this.mToastQueue.get(i);
                                    if (r.pkg.equals(pkg) && (count = count + 1) >= 50) {
                                        Slog.e(NotificationManagerService.TAG, "Package has already posted " + count + " toasts. Not showing more. Package=" + pkg);
                                        return;
                                    }
                                }
                            }
                            ToastRecord record2 = new ToastRecord(callingPid, pkg, callback, duration);
                            NotificationManagerService.this.mToastQueue.add(record2);
                            index = NotificationManagerService.this.mToastQueue.size() - 1;
                            NotificationManagerService.this.keepProcessAliveLocked(callingPid);
                        }
                        if (index == 0) {
                            NotificationManagerService.this.showNextToastLocked();
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            }

            public void cancelToast(String pkg, ITransientNotification callback) {
                Slog.i(NotificationManagerService.TAG, "cancelToast pkg=" + pkg + " callback=" + callback);
                if (pkg == null || callback == null) {
                    Slog.e(NotificationManagerService.TAG, "Not cancelling notification. pkg=" + pkg + " callback=" + callback);
                    return;
                }
                synchronized (NotificationManagerService.this.mToastQueue) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int index = NotificationManagerService.this.indexOfToastLocked(pkg, callback);
                        if (index >= 0) {
                            NotificationManagerService.this.cancelToastLocked(index);
                        } else {
                            Slog.w(NotificationManagerService.TAG, "Toast already cancelled. pkg=" + pkg + " callback=" + callback);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            }

            public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int[] idOut, int userId) throws RemoteException {
                NotificationManagerService.this.enqueueNotificationInternal(pkg, opPkg, Binder.getCallingUid(), Binder.getCallingPid(), tag, id, notification, idOut, userId);
            }

            public void cancelNotificationWithTag(String pkg, String tag, int id, int userId) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                NotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), pkg, tag, id, 0, (Binder.getCallingUid() == 1000 ? 0 : 64) | (Binder.getCallingUid() == 1000 ? 0 : 1024), false, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelNotificationWithTag", pkg), 8, null);
            }

            public void cancelAllNotifications(String pkg, int userId) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                NotificationManagerService.this.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), pkg, 0, 64, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelAllNotifications", pkg), 9, null);
            }

            public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.setNotificationsEnabledForPackageImpl(pkg, uid, enabled);
                NotificationManagerService.this.mRankingHelper.setEnabled(pkg, uid, enabled);
                NotificationManagerService.this.savePolicyFile();
            }

            public boolean areNotificationsEnabled(String pkg) {
                return areNotificationsEnabledForPackage(pkg, Binder.getCallingUid());
            }

            public boolean areNotificationsEnabledForPackage(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                if (UserHandle.getCallingUserId() != UserHandle.getUserId(uid)) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "canNotifyAsPackage for uid " + uid);
                }
                return NotificationManagerService.this.mAppOps.checkOpNoThrow(11, uid, pkg) == 0 && !NotificationManagerService.this.isPackageSuspendedForUser(pkg, uid);
            }

            public void setPriority(String pkg, int uid, int priority) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.mRankingHelper.setPriority(pkg, uid, priority);
                NotificationManagerService.this.savePolicyFile();
            }

            public int getPriority(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getPriority(pkg, uid);
            }

            public void setVisibilityOverride(String pkg, int uid, int visibility) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.mRankingHelper.setVisibilityOverride(pkg, uid, visibility);
                NotificationManagerService.this.savePolicyFile();
            }

            public int getVisibilityOverride(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getVisibilityOverride(pkg, uid);
            }

            public void setImportance(String pkg, int uid, int importance) {
                enforceSystemOrSystemUI("Caller not system or systemui");
                NotificationManagerService.this.setNotificationsEnabledForPackageImpl(pkg, uid, importance != 0);
                NotificationManagerService.this.mRankingHelper.setImportance(pkg, uid, importance);
                NotificationManagerService.this.savePolicyFile();
            }

            public int getPackageImportance(String pkg) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                return NotificationManagerService.this.mRankingHelper.getImportance(pkg, Binder.getCallingUid());
            }

            public int getImportance(String pkg, int uid) {
                enforceSystemOrSystemUI("Caller not system or systemui");
                return NotificationManagerService.this.mRankingHelper.getImportance(pkg, uid);
            }

            public StatusBarNotification[] getActiveNotifications(String callingPkg) {
                NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getActiveNotifications");
                StatusBarNotification[] tmp = null;
                int uid = Binder.getCallingUid();
                if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, uid, callingPkg) == 0) {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        tmp = new StatusBarNotification[NotificationManagerService.this.mNotificationList.size()];
                        int N = NotificationManagerService.this.mNotificationList.size();
                        for (int i = 0; i < N; i++) {
                            tmp[i] = NotificationManagerService.this.mNotificationList.get(i).sbn;
                        }
                    }
                }
                return tmp;
            }

            public ParceledListSlice<StatusBarNotification> getAppActiveNotifications(String pkg, int incomingUserId) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                int userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), incomingUserId, true, false, "getAppActiveNotifications", pkg);
                ArrayList<StatusBarNotification> list = new ArrayList<>(NotificationManagerService.this.mNotificationList.size());
                synchronized (NotificationManagerService.this.mNotificationList) {
                    int N = NotificationManagerService.this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        StatusBarNotification sbn = NotificationManagerService.this.mNotificationList.get(i).sbn;
                        if (sbn.getPackageName().equals(pkg) && sbn.getUserId() == userId && (sbn.getNotification().flags & 1024) == 0) {
                            StatusBarNotification sbnOut = new StatusBarNotification(sbn.getPackageName(), sbn.getOpPkg(), sbn.getId(), sbn.getTag(), sbn.getUid(), sbn.getInitialPid(), 0, sbn.getNotification().clone(), sbn.getUser(), sbn.getPostTime());
                            list.add(sbnOut);
                        }
                    }
                }
                return new ParceledListSlice<>(list);
            }

            public StatusBarNotification[] getHistoricalNotifications(String callingPkg, int count) {
                NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getHistoricalNotifications");
                StatusBarNotification[] tmp = null;
                int uid = Binder.getCallingUid();
                if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, uid, callingPkg) == 0) {
                    synchronized (NotificationManagerService.this.mArchive) {
                        tmp = NotificationManagerService.this.mArchive.getArray(count);
                    }
                }
                return tmp;
            }

            public void registerListener(INotificationListener listener, ComponentName component, int userid) {
                enforceSystemOrSystemUI("INotificationManager.registerListener");
                NotificationManagerService.this.mListeners.registerService(listener, component, userid);
            }

            public void unregisterListener(INotificationListener token, int userid) {
                NotificationManagerService.this.mListeners.unregisterService((IInterface) token, userid);
            }

            public void cancelNotificationsFromListener(INotificationListener token, String[] keys) {
                int callingUid = Binder.getCallingUid();
                int callingPid = Binder.getCallingPid();
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        if (keys != null) {
                            for (String str : keys) {
                                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(str);
                                if (r != null) {
                                    int userId = r.sbn.getUserId();
                                    if (userId != info.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                        throw new SecurityException("Disallowed call from listener: " + info.service);
                                    }
                                    cancelNotificationFromListenerLocked(info, callingUid, callingPid, r.sbn.getPackageName(), r.sbn.getTag(), r.sbn.getId(), userId);
                                }
                            }
                        } else {
                            NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, info.userid, 11, info, info.supportsProfiles());
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void requestBindListener(ComponentName component) {
                ManagedServices manager;
                NotificationManagerService.checkCallerIsSystemOrSameApp(component.getPackageName());
                long identity = Binder.clearCallingIdentity();
                try {
                    if (NotificationManagerService.this.mRankerServices.isComponentEnabledForCurrentProfiles(component)) {
                        manager = NotificationManagerService.this.mRankerServices;
                    } else {
                        manager = NotificationManagerService.this.mListeners;
                    }
                    manager.setComponentState(component, true);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void requestUnbindListener(INotificationListener token) {
                long identity = Binder.clearCallingIdentity();
                try {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    info.getOwner().setComponentState(info.component, false);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void setNotificationsShownFromListener(INotificationListener token, String[] keys) {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        if (keys != null) {
                            int N = keys.length;
                            for (int i = 0; i < N; i++) {
                                NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                                if (r != null) {
                                    int userId = r.sbn.getUserId();
                                    if (userId != info.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                        throw new SecurityException("Disallowed call from listener: " + info.service);
                                    }
                                    if (!r.isSeen()) {
                                        Slog.d(NotificationManagerService.TAG, "Marking notification as seen " + keys[i]);
                                        UsageStatsManagerInternal usageStatsManagerInternal = NotificationManagerService.this.mAppUsageStats;
                                        String packageName = r.sbn.getPackageName();
                                        if (userId == -1) {
                                            userId = 0;
                                        }
                                        usageStatsManagerInternal.reportEvent(packageName, userId, 7);
                                        r.setSeen();
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            private void cancelNotificationFromListenerLocked(ManagedServices.ManagedServiceInfo info, int callingUid, int callingPid, String pkg, String tag, int id, int userId) {
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 66, true, userId, 10, info);
            }

            public void cancelNotificationFromListener(INotificationListener token, String pkg, String tag, int id) {
                int callingUid = Binder.getCallingUid();
                int callingPid = Binder.getCallingPid();
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        if (info.supportsProfiles()) {
                            Log.e(NotificationManagerService.TAG, "Ignoring deprecated cancelNotification(pkg, tag, id) from " + info.component + " use cancelNotification(key) instead.");
                        } else {
                            cancelNotificationFromListenerLocked(info, callingUid, callingPid, pkg, tag, id, info.userid);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public ParceledListSlice<StatusBarNotification> getActiveNotificationsFromListener(INotificationListener token, String[] keys, int trim) {
                ParceledListSlice<StatusBarNotification> parceledListSlice;
                NotificationRecord r;
                synchronized (NotificationManagerService.this.mNotificationList) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    boolean getKeys = keys != null;
                    int N = getKeys ? keys.length : NotificationManagerService.this.mNotificationList.size();
                    ArrayList<StatusBarNotification> list = new ArrayList<>(N);
                    for (int i = 0; i < N; i++) {
                        if (getKeys) {
                            r = NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                        } else {
                            r = NotificationManagerService.this.mNotificationList.get(i);
                        }
                        if (r != null) {
                            StatusBarNotification sbn = r.sbn;
                            if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                                StatusBarNotification sbnToSend = trim == 0 ? sbn : sbn.cloneLight();
                                list.add(sbnToSend);
                            }
                        }
                    }
                    parceledListSlice = new ParceledListSlice<>(list);
                }
                return parceledListSlice;
            }

            public void requestHintsFromListener(INotificationListener token, int hints) {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        boolean disableEffects = (hints & 7) != 0;
                        if (disableEffects) {
                            NotificationManagerService.this.addDisabledHints(info, hints);
                        } else {
                            NotificationManagerService.this.removeDisabledHints(info, hints);
                        }
                        NotificationManagerService.this.updateListenerHintsLocked();
                        NotificationManagerService.this.updateEffectsSuppressorLocked();
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public int getHintsFromListener(INotificationListener token) {
                int i;
                synchronized (NotificationManagerService.this.mNotificationList) {
                    i = NotificationManagerService.this.mListenerHints;
                }
                return i;
            }

            public void requestInterruptionFilterFromListener(INotificationListener token, int interruptionFilter) throws RemoteException {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        NotificationManagerService.this.mZenModeHelper.requestFromListener(info.component, interruptionFilter);
                        NotificationManagerService.this.updateInterruptionFilterLocked();
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public int getInterruptionFilterFromListener(INotificationListener token) throws RemoteException {
                int i;
                synchronized (NotificationManagerService.this.mNotificationLight) {
                    i = NotificationManagerService.this.mInterruptionFilter;
                }
                return i;
            }

            public void setOnNotificationPostedTrimFromListener(INotificationListener token, int trim) throws RemoteException {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    if (info == null) {
                        return;
                    }
                    NotificationManagerService.this.mListeners.setOnNotificationPostedTrimLocked(info, trim);
                }
            }

            public int getZenMode() {
                return NotificationManagerService.this.mZenModeHelper.getZenMode();
            }

            public ZenModeConfig getZenModeConfig() {
                enforceSystemOrSystemUIOrVolume("INotificationManager.getZenModeConfig");
                return NotificationManagerService.this.mZenModeHelper.getConfig();
            }

            public void setZenMode(int mode, Uri conditionId, String reason) throws RemoteException {
                enforceSystemOrSystemUIOrVolume("INotificationManager.setZenMode");
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setManualZenMode(mode, conditionId, reason);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public List<ZenModeConfig.ZenRule> getZenRules() throws RemoteException {
                enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRules");
                return NotificationManagerService.this.mZenModeHelper.getZenRules();
            }

            public AutomaticZenRule getAutomaticZenRule(String id) throws RemoteException {
                Preconditions.checkNotNull(id, "Id is null");
                enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.getAutomaticZenRule(id);
            }

            public String addAutomaticZenRule(AutomaticZenRule automaticZenRule) throws RemoteException {
                Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
                Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
                Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
                Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
                enforcePolicyAccess(Binder.getCallingUid(), "addAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.addAutomaticZenRule(automaticZenRule, "addAutomaticZenRule");
            }

            public boolean updateAutomaticZenRule(String id, AutomaticZenRule automaticZenRule) throws RemoteException {
                Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
                Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
                Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
                Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
                enforcePolicyAccess(Binder.getCallingUid(), "updateAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.updateAutomaticZenRule(id, automaticZenRule, "updateAutomaticZenRule");
            }

            public boolean removeAutomaticZenRule(String id) throws RemoteException {
                Preconditions.checkNotNull(id, "Id is null");
                enforcePolicyAccess(Binder.getCallingUid(), "removeAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRule(id, "removeAutomaticZenRule");
            }

            public boolean removeAutomaticZenRules(String packageName) throws RemoteException {
                Preconditions.checkNotNull(packageName, "Package name is null");
                enforceSystemOrSystemUI("removeAutomaticZenRules");
                return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRules(packageName, "removeAutomaticZenRules");
            }

            public int getRuleInstanceCount(ComponentName owner) throws RemoteException {
                Preconditions.checkNotNull(owner, "Owner is null");
                enforceSystemOrSystemUI("getRuleInstanceCount");
                return NotificationManagerService.this.mZenModeHelper.getCurrentInstanceCount(owner);
            }

            public void setInterruptionFilter(String pkg, int filter) throws RemoteException {
                enforcePolicyAccess(pkg, "setInterruptionFilter");
                int zen = NotificationManager.zenModeFromInterruptionFilter(filter, -1);
                if (zen == -1) {
                    throw new IllegalArgumentException("Invalid filter: " + filter);
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setManualZenMode(zen, null, "setInterruptionFilter");
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void notifyConditions(final String pkg, IConditionProvider provider, final Condition[] conditions) {
                final ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                NotificationManagerService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationManagerService.this.mConditionProviders.notifyConditions(pkg, info, conditions);
                    }
                });
            }

            private void enforceSystemOrSystemUIOrVolume(String message) {
                int vcuid;
                if (NotificationManagerService.this.mAudioManagerInternal != null && (vcuid = NotificationManagerService.this.mAudioManagerInternal.getVolumeControllerUid()) > 0 && Binder.getCallingUid() == vcuid) {
                    return;
                }
                enforceSystemOrSystemUI(message);
            }

            private void enforceSystemOrSystemUI(String message) {
                if (NotificationManagerService.isCallerSystem()) {
                    return;
                }
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
            }

            private void enforceSystemOrSystemUIOrSamePackage(String pkg, String message) {
                try {
                    NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                } catch (SecurityException e) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
                }
            }

            private void enforcePolicyAccess(int uid, String method) {
                if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
                    return;
                }
                boolean accessAllowed = false;
                String[] packages = NotificationManagerService.this.getContext().getPackageManager().getPackagesForUid(uid);
                for (String str : packages) {
                    if (checkPolicyAccess(str)) {
                        accessAllowed = true;
                    }
                }
                if (accessAllowed) {
                    return;
                }
                Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + method);
                throw new SecurityException("Notification policy access denied");
            }

            private void enforcePolicyAccess(String pkg, String method) {
                if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
                    return;
                }
                NotificationManagerService.checkCallerIsSameApp(pkg);
                if (checkPolicyAccess(pkg)) {
                    return;
                }
                Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + method);
                throw new SecurityException("Notification policy access denied");
            }

            private boolean checkPackagePolicyAccess(String pkg) {
                return NotificationManagerService.this.mPolicyAccess.isPackageGranted(pkg);
            }

            private boolean checkPolicyAccess(String pkg) {
                try {
                    int uid = NotificationManagerService.this.getContext().getPackageManager().getPackageUidAsUser(pkg, UserHandle.getCallingUserId());
                    if (ActivityManager.checkComponentPermission("android.permission.MANAGE_NOTIFICATIONS", uid, -1, true) == 0 || checkPackagePolicyAccess(pkg)) {
                        return true;
                    }
                    return NotificationManagerService.this.mListeners.isComponentEnabledForPackage(pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (NotificationManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump NotificationManager from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                    return;
                }
                DumpFilter filter = DumpFilter.parseFromArguments(args);
                if (filter == null || !filter.stats) {
                    NotificationManagerService.this.dumpImpl(pw, filter);
                } else {
                    NotificationManagerService.this.dumpJson(pw, filter);
                }
            }

            public void removeAllNotifications(String pkg, int userId) {
                NotificationManagerService.checkCallerIsSystem();
                Slog.v(NotificationManagerService.TAG, " removeAllNotifications, for " + pkg);
                synchronized (NotificationManagerService.this.mNotificationList) {
                    int N = NotificationManagerService.this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationList.get(i);
                        if (r.sbn.getPackageName().equals(pkg)) {
                            NotificationManagerService.this.cancelNotificationLocked(r, false, 2);
                        }
                    }
                }
            }

            public ComponentName getEffectsSuppressor() {
                enforceSystemOrSystemUIOrVolume("INotificationManager.getEffectsSuppressor");
                if (NotificationManagerService.this.mEffectsSuppressors.isEmpty()) {
                    return null;
                }
                return (ComponentName) NotificationManagerService.this.mEffectsSuppressors.get(0);
            }

            public boolean matchesCallFilter(Bundle extras) {
                enforceSystemOrSystemUI("INotificationManager.matchesCallFilter");
                return NotificationManagerService.this.mZenModeHelper.matchesCallFilter(Binder.getCallingUserHandle(), extras, (ValidateNotificationPeople) NotificationManagerService.this.mRankingHelper.findExtractor(ValidateNotificationPeople.class), NotificationManagerService.MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS, NotificationManagerService.MATCHES_CALL_FILTER_TIMEOUT_AFFINITY);
            }

            public boolean isSystemConditionProviderEnabled(String path) {
                enforceSystemOrSystemUIOrVolume("INotificationManager.isSystemConditionProviderEnabled");
                return NotificationManagerService.this.mConditionProviders.isSystemProviderEnabled(path);
            }

            public byte[] getBackupPayload(int user) {
                Slog.d(NotificationManagerService.TAG, "getBackupPayload u=" + user);
                if (user != 0) {
                    Slog.w(NotificationManagerService.TAG, "getBackupPayload: cannot backup policy for user " + user);
                    return null;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    NotificationManagerService.this.writePolicyXml(baos, true);
                    return baos.toByteArray();
                } catch (IOException e) {
                    Slog.w(NotificationManagerService.TAG, "getBackupPayload: error writing payload for user " + user, e);
                    return null;
                }
            }

            public void applyRestore(byte[] payload, int user) {
                Slog.d(NotificationManagerService.TAG, "applyRestore u=" + user + " payload=" + (payload != null ? new String(payload, StandardCharsets.UTF_8) : null));
                if (payload == null) {
                    Slog.w(NotificationManagerService.TAG, "applyRestore: no payload to restore for user " + user);
                    return;
                }
                if (user != 0) {
                    Slog.w(NotificationManagerService.TAG, "applyRestore: cannot restore policy for user " + user);
                    return;
                }
                ByteArrayInputStream bais = new ByteArrayInputStream(payload);
                try {
                    NotificationManagerService.this.readPolicyXml(bais, true);
                    NotificationManagerService.this.savePolicyFile();
                } catch (IOException | NumberFormatException | XmlPullParserException e) {
                    Slog.w(NotificationManagerService.TAG, "applyRestore: error reading payload", e);
                }
            }

            public boolean isNotificationPolicyAccessGranted(String pkg) {
                return checkPolicyAccess(pkg);
            }

            public boolean isNotificationPolicyAccessGrantedForPackage(String pkg) {
                enforceSystemOrSystemUIOrSamePackage(pkg, "request policy access status for another package");
                return checkPolicyAccess(pkg);
            }

            public String[] getPackagesRequestingNotificationPolicyAccess() throws RemoteException {
                enforceSystemOrSystemUI("request policy access packages");
                long identity = Binder.clearCallingIdentity();
                try {
                    return NotificationManagerService.this.mPolicyAccess.getRequestingPackages();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void setNotificationPolicyAccessGranted(String pkg, boolean granted) throws RemoteException {
                enforceSystemOrSystemUI("grant notification policy access");
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        NotificationManagerService.this.mPolicyAccess.put(pkg, granted);
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public NotificationManager.Policy getNotificationPolicy(String pkg) {
                enforcePolicyAccess(pkg, "getNotificationPolicy");
                long identity = Binder.clearCallingIdentity();
                try {
                    return NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void setNotificationPolicy(String pkg, NotificationManager.Policy policy) {
                enforcePolicyAccess(pkg, "setNotificationPolicy");
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setNotificationPolicy(policy);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void applyAdjustmentFromRankerService(INotificationListener token, Adjustment adjustment) throws RemoteException {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        NotificationManagerService.this.mRankerServices.checkServiceTokenLocked(token);
                        NotificationManagerService.this.applyAdjustmentLocked(adjustment);
                    }
                    NotificationManagerService.this.maybeAddAutobundleSummary(adjustment);
                    NotificationManagerService.this.mRankingHandler.requestSort();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void applyAdjustmentsFromRankerService(INotificationListener token, List<Adjustment> adjustments) throws RemoteException {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationList) {
                        NotificationManagerService.this.mRankerServices.checkServiceTokenLocked(token);
                        for (Adjustment adjustment : adjustments) {
                            NotificationManagerService.this.applyAdjustmentLocked(adjustment);
                        }
                    }
                    for (Adjustment adjustment2 : adjustments) {
                        NotificationManagerService.this.maybeAddAutobundleSummary(adjustment2);
                    }
                    NotificationManagerService.this.mRankingHandler.requestSort();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        };
        this.mInternalService = new NotificationManagerInternal() {
            @Override
            public void enqueueNotification(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int[] idReceived, int userId) {
                NotificationManagerService.this.enqueueNotificationInternal(pkg, opPkg, callingUid, callingPid, tag, id, notification, idReceived, userId);
            }

            @Override
            public void removeForegroundServiceFlagFromNotification(String pkg, int notificationId, int userId) {
                NotificationManagerService.checkCallerIsSystem();
                synchronized (NotificationManagerService.this.mNotificationList) {
                    int i = NotificationManagerService.this.indexOfNotificationLocked(pkg, null, notificationId, userId);
                    if (i < 0) {
                        Log.d(NotificationManagerService.TAG, "stripForegroundServiceFlag: Could not find notification with pkg=" + pkg + " / id=" + notificationId + " / userId=" + userId);
                        return;
                    }
                    NotificationRecord r = NotificationManagerService.this.mNotificationList.get(i);
                    StatusBarNotification sbn = r.sbn;
                    sbn.getNotification().flags = r.mOriginalFlags & (-65);
                    NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                    NotificationManagerService.this.mListeners.notifyPostedLocked(sbn, sbn);
                }
            }
        };
    }

    void setAudioManager(AudioManager audioMananger) {
        this.mAudioManager = audioMananger;
    }

    void setVibrator(Vibrator vibrator) {
        this.mVibrator = vibrator;
    }

    void setSystemReady(boolean systemReady) {
        this.mSystemReady = systemReady;
    }

    void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    @Override
    public void onStart() {
        String[] extractorNames;
        Resources resources = getContext().getResources();
        this.mMaxPackageEnqueueRate = Settings.Global.getFloat(getContext().getContentResolver(), "max_notification_enqueue_rate", DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE);
        this.mAm = ActivityManagerNative.getDefault();
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        this.mAppUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mRankerServicePackageName = getContext().getPackageManager().getServicesSystemSharedLibraryPackageName();
        this.mHandler = new WorkerHandler(this, null);
        this.mRankingThread.start();
        try {
            extractorNames = resources.getStringArray(R.array.config_convert_to_emergency_number_map);
        } catch (Resources.NotFoundException e) {
            extractorNames = new String[0];
        }
        this.mUsageStats = new NotificationUsageStats(getContext());
        this.mRankingHandler = new RankingHandlerWorker(this.mRankingThread.getLooper());
        this.mRankingHelper = new RankingHelper(getContext(), this.mRankingHandler, this.mUsageStats, extractorNames);
        this.mConditionProviders = new ConditionProviders(getContext(), this.mHandler, this.mUserProfiles);
        this.mZenModeHelper = new ZenModeHelper(getContext(), this.mHandler.getLooper(), this.mConditionProviders);
        this.mZenModeHelper.addCallback(new ZenModeHelper.Callback() {
            @Override
            public void onConfigChanged() {
                NotificationManagerService.this.savePolicyFile();
            }

            @Override
            void onZenModeChanged() {
                NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.INTERRUPTION_FILTER_CHANGED");
                NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL").addFlags(67108864), UserHandle.ALL, "android.permission.MANAGE_NOTIFICATIONS");
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
            }

            @Override
            void onPolicyChanged() {
                NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.NOTIFICATION_POLICY_CHANGED");
            }
        });
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mPolicyFile = new AtomicFile(new File(systemDir, "notification_policy.xml"));
        syncBlockDb();
        this.mListeners = new NotificationListeners();
        this.mRankerServices = new NotificationRankers();
        this.mRankerServices.registerRanker();
        this.mStatusBar = (StatusBarManagerInternal) getLocalService(StatusBarManagerInternal.class);
        if (this.mStatusBar != null) {
            this.mStatusBar.setNotificationDelegate(this.mNotificationDelegate);
        }
        LightsManager lights = (LightsManager) getLocalService(LightsManager.class);
        this.mNotificationLight = lights.getLight(4);
        this.mAttentionLight = lights.getLight(5);
        this.mDefaultNotificationColor = resources.getColor(R.color.accessibility_feature_background);
        this.mDefaultNotificationLedOn = resources.getInteger(R.integer.config_defaultAlarmVibrationIntensity);
        this.mDefaultNotificationLedOff = resources.getInteger(R.integer.config_defaultAnalogClockSecondsHandFps);
        this.mDefaultVibrationPattern = getLongArray(resources, R.array.config_companionDevicePackages, 17, DEFAULT_VIBRATE_PATTERN);
        this.mFallbackVibrationPattern = getLongArray(resources, R.array.config_companionPermSyncEnabledCerts, 17, DEFAULT_VIBRATE_PATTERN);
        this.mUseAttentionLight = resources.getBoolean(R.^attr-private.colorAccentPrimary);
        if (Settings.Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mDisableNotificationEffects = true;
        }
        this.mZenModeHelper.initZenMode();
        this.mInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        this.mUserProfiles.updateCache(getContext());
        listenForCallState();
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder != null) {
                DmAgent dm = DmAgent.Stub.asInterface(binder);
                this.mDmLock = dm.isLockFlagSet();
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "failed to get DM status!");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        filter.addAction(OMADM_LAWMO_LOCK);
        filter.addAction(OMADM_LAWMO_UNLOCK);
        filter.addAction(PPL_LOCK);
        filter.addAction(PPL_UNLOCK);
        getContext().registerReceiver(this.mIntentReceiver, filter);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        pkgFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        pkgFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, pkgFilter, null, null);
        IntentFilter suspendedPkgFilter = new IntentFilter();
        suspendedPkgFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, suspendedPkgFilter, null, null);
        IntentFilter sdFilter = new IntentFilter("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, sdFilter, null, null);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mArchive = new Archive(resources.getInteger(R.integer.config_defaultMaxDurationBetweenUndimsMillis));
        publishBinderService("notification", this.mService);
        publishLocalService(NotificationManagerInternal.class, this.mInternalService);
    }

    private void sendRegisteredOnlyBroadcast(String action) {
        getContext().sendBroadcastAsUser(new Intent(action).addFlags(1073741824), UserHandle.ALL, null);
    }

    private void syncBlockDb() {
        loadPolicyFile();
        Map<Integer, String> packageBans = this.mRankingHelper.getPackageBans();
        for (Map.Entry<Integer, String> ban : packageBans.entrySet()) {
            setNotificationsEnabledForPackageImpl(ban.getValue(), ban.getKey().intValue(), false);
        }
        packageBans.clear();
        for (UserInfo user : UserManager.get(getContext()).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            PackageManager packageManager = getContext().getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackagesAsUser(0, userId);
            int packageCount = packages.size();
            for (int p = 0; p < packageCount; p++) {
                String packageName = packages.get(p).packageName;
                try {
                    int uid = packageManager.getPackageUidAsUser(packageName, userId);
                    if (!checkNotificationOp(packageName, uid)) {
                        packageBans.put(Integer.valueOf(uid), packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        for (Map.Entry<Integer, String> ban2 : packageBans.entrySet()) {
            this.mRankingHelper.setImportance(ban2.getValue(), ban2.getKey().intValue(), 0);
        }
        savePolicyFile();
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mSystemReady = true;
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            this.mAudioManagerInternal = (AudioManagerInternal) getLocalService(AudioManagerInternal.class);
            this.mVrManagerInternal = (VrManagerInternal) getLocalService(VrManagerInternal.class);
            this.mZenModeHelper.onSystemReady();
            return;
        }
        if (phase != 600) {
            return;
        }
        this.mSettingsObserver.observe();
        this.mListeners.onBootPhaseAppsCanStart();
        this.mRankerServices.onBootPhaseAppsCanStart();
        this.mConditionProviders.onBootPhaseAppsCanStart();
    }

    void setNotificationsEnabledForPackageImpl(String pkg, int uid, boolean enabled) {
        Slog.v(TAG, (enabled ? "en" : "dis") + "abling notifications for " + pkg);
        this.mAppOps.setMode(11, uid, pkg, enabled ? 0 : 1);
        if (enabled) {
            return;
        }
        cancelAllNotificationsInt(MY_UID, MY_PID, pkg, 0, 0, true, UserHandle.getUserId(uid), 7, null);
    }

    private void updateListenerHintsLocked() {
        int hints = calculateHints();
        if (hints == this.mListenerHints) {
            return;
        }
        ZenLog.traceListenerHintsChanged(this.mListenerHints, hints, this.mEffectsSuppressors.size());
        this.mListenerHints = hints;
        scheduleListenerHintsChanged(hints);
    }

    private void updateEffectsSuppressorLocked() {
        long updatedSuppressedEffects = calculateSuppressedEffects();
        if (updatedSuppressedEffects == this.mZenModeHelper.getSuppressedEffects()) {
            return;
        }
        List<ComponentName> suppressors = getSuppressors();
        ZenLog.traceEffectsSuppressorChanged(this.mEffectsSuppressors, suppressors, updatedSuppressedEffects);
        this.mEffectsSuppressors = suppressors;
        this.mZenModeHelper.setSuppressedEffects(updatedSuppressedEffects);
        sendRegisteredOnlyBroadcast("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
    }

    private ArrayList<ComponentName> getSuppressors() {
        ArrayList<ComponentName> names = new ArrayList<>();
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            ArraySet<ManagedServices.ManagedServiceInfo> serviceInfoList = this.mListenersDisablingEffects.valueAt(i);
            for (ManagedServices.ManagedServiceInfo info : serviceInfoList) {
                names.add(info.component);
            }
        }
        return names;
    }

    private boolean removeDisabledHints(ManagedServices.ManagedServiceInfo info) {
        return removeDisabledHints(info, 0);
    }

    private boolean removeDisabledHints(ManagedServices.ManagedServiceInfo info, int hints) {
        boolean removed = false;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            ArraySet<ManagedServices.ManagedServiceInfo> listeners = this.mListenersDisablingEffects.valueAt(i);
            if (hints == 0 || (hint & hints) == hint) {
                removed = !removed ? listeners.remove(info) : true;
            }
        }
        return removed;
    }

    private void addDisabledHints(ManagedServices.ManagedServiceInfo info, int hints) {
        if ((hints & 1) != 0) {
            addDisabledHint(info, 1);
        }
        if ((hints & 2) != 0) {
            addDisabledHint(info, 2);
        }
        if ((hints & 4) == 0) {
            return;
        }
        addDisabledHint(info, 4);
    }

    private void addDisabledHint(ManagedServices.ManagedServiceInfo info, int hint) {
        if (this.mListenersDisablingEffects.indexOfKey(hint) < 0) {
            this.mListenersDisablingEffects.put(hint, new ArraySet<>());
        }
        ArraySet<ManagedServices.ManagedServiceInfo> hintListeners = this.mListenersDisablingEffects.get(hint);
        hintListeners.add(info);
    }

    private int calculateHints() {
        int hints = 0;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            ArraySet<ManagedServices.ManagedServiceInfo> serviceInfoList = this.mListenersDisablingEffects.valueAt(i);
            if (!serviceInfoList.isEmpty()) {
                hints |= hint;
            }
        }
        return hints;
    }

    private long calculateSuppressedEffects() {
        int hints = calculateHints();
        long suppressedEffects = 0;
        if ((hints & 1) != 0) {
            suppressedEffects = 3;
        }
        if ((hints & 2) != 0) {
            suppressedEffects |= 1;
        }
        if ((hints & 4) != 0) {
            return suppressedEffects | 2;
        }
        return suppressedEffects;
    }

    private void updateInterruptionFilterLocked() {
        int interruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        if (interruptionFilter == this.mInterruptionFilter) {
            return;
        }
        this.mInterruptionFilter = interruptionFilter;
        scheduleInterruptionFilterChanged(interruptionFilter);
    }

    private void applyAdjustmentLocked(Adjustment adjustment) {
        maybeClearAutobundleSummaryLocked(adjustment);
        NotificationRecord n = this.mNotificationsByKey.get(adjustment.getKey());
        if (n == null) {
            return;
        }
        if (adjustment.getImportance() != 0) {
            n.setImportance(adjustment.getImportance(), adjustment.getExplanation());
        }
        if (adjustment.getSignals() == null) {
            return;
        }
        Bundle.setDefusable(adjustment.getSignals(), true);
        String autoGroupKey = adjustment.getSignals().getString("group_key_override", null);
        if (autoGroupKey == null) {
            EventLogTags.writeNotificationUnautogrouped(adjustment.getKey());
        } else {
            EventLogTags.writeNotificationAutogrouped(adjustment.getKey());
        }
        n.sbn.setOverrideGroupKey(autoGroupKey);
    }

    private void maybeClearAutobundleSummaryLocked(Adjustment adjustment) {
        ArrayMap<String, String> summaries;
        NotificationRecord removed;
        if (adjustment.getSignals() == null) {
            return;
        }
        Bundle.setDefusable(adjustment.getSignals(), true);
        if (!adjustment.getSignals().containsKey("autogroup_needed") || adjustment.getSignals().getBoolean("autogroup_needed", false) || (summaries = this.mAutobundledSummaries.get(Integer.valueOf(adjustment.getUser()))) == null || !summaries.containsKey(adjustment.getPackage()) || (removed = this.mNotificationsByKey.get(summaries.remove(adjustment.getPackage()))) == null) {
            return;
        }
        this.mNotificationList.remove(removed);
        cancelNotificationLocked(removed, false, 16);
    }

    private void maybeAddAutobundleSummary(Adjustment adjustment) throws Throwable {
        if (adjustment.getSignals() != null) {
            Bundle.setDefusable(adjustment.getSignals(), true);
            if (adjustment.getSignals().getBoolean("autogroup_needed", false)) {
                String newAutoBundleKey = adjustment.getSignals().getString("group_key_override", null);
                NotificationRecord notificationRecord = null;
                synchronized (this.mNotificationList) {
                    try {
                        NotificationRecord notificationRecord2 = this.mNotificationsByKey.get(adjustment.getKey());
                        if (notificationRecord2 == null) {
                            return;
                        }
                        StatusBarNotification adjustedSbn = notificationRecord2.sbn;
                        int userId = adjustedSbn.getUser().getIdentifier();
                        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(userId));
                        if (summaries == null) {
                            summaries = new ArrayMap<>();
                        }
                        this.mAutobundledSummaries.put(Integer.valueOf(userId), summaries);
                        if (!summaries.containsKey(adjustment.getPackage()) && newAutoBundleKey != null) {
                            ApplicationInfo appInfo = (ApplicationInfo) adjustedSbn.getNotification().extras.getParcelable("android.appInfo");
                            Bundle extras = new Bundle();
                            extras.putParcelable("android.appInfo", appInfo);
                            Notification summaryNotification = new Notification.Builder(getContext()).setSmallIcon(adjustedSbn.getNotification().getSmallIcon()).setGroupSummary(true).setGroup(newAutoBundleKey).setFlag(1024, true).setFlag(512, true).setColor(adjustedSbn.getNotification().color).build();
                            summaryNotification.extras.putAll(extras);
                            Intent appIntent = getContext().getPackageManager().getLaunchIntentForPackage(adjustment.getPackage());
                            if (appIntent != null) {
                                summaryNotification.contentIntent = PendingIntent.getActivityAsUser(getContext(), 0, appIntent, 0, null, UserHandle.of(userId));
                            }
                            StatusBarNotification summarySbn = new StatusBarNotification(adjustedSbn.getPackageName(), adjustedSbn.getOpPkg(), Integer.MAX_VALUE, "group_key_override", adjustedSbn.getUid(), adjustedSbn.getInitialPid(), summaryNotification, adjustedSbn.getUser(), newAutoBundleKey, System.currentTimeMillis());
                            NotificationRecord summaryRecord = new NotificationRecord(getContext(), summarySbn);
                            try {
                                summaries.put(adjustment.getPackage(), summarySbn.getKey());
                                notificationRecord = summaryRecord;
                            } catch (Throwable th) {
                                th = th;
                            }
                        }
                        if (notificationRecord != null) {
                            this.mHandler.post(new EnqueueNotificationRunnable(userId, notificationRecord));
                            return;
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                    throw th;
                }
            }
        }
    }

    private String disableNotificationEffects(NotificationRecord record) {
        if (this.mDisableNotificationEffects) {
            return "booleanState";
        }
        if ((this.mListenerHints & 1) != 0) {
            return "listenerHints";
        }
        if (this.mCallState != 0 && !this.mZenModeHelper.isCall(record)) {
            return "callState";
        }
        return null;
    }

    private void dumpJson(PrintWriter pw, DumpFilter filter) {
        JSONObject dump = new JSONObject();
        try {
            dump.put("service", "Notification Manager");
            dump.put("bans", this.mRankingHelper.dumpBansJson(filter));
            dump.put("ranking", this.mRankingHelper.dumpJson(filter));
            dump.put("stats", this.mUsageStats.dumpJson(filter));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        pw.println(dump);
    }

    void dumpImpl(PrintWriter pw, DumpFilter filter) {
        pw.print("Current Notification Manager state");
        if (filter.filtered) {
            pw.print(" (filtered to ");
            pw.print(filter);
            pw.print(")");
        }
        pw.println(':');
        boolean zenOnly = filter.filtered ? filter.zen : false;
        if (!zenOnly) {
            synchronized (this.mToastQueue) {
                int N = this.mToastQueue.size();
                if (N > 0) {
                    pw.println("  Toast Queue:");
                    for (int i = 0; i < N; i++) {
                        this.mToastQueue.get(i).dump(pw, "    ", filter);
                    }
                    pw.println("  ");
                }
            }
        }
        synchronized (this.mNotificationList) {
            if (!zenOnly) {
                int N2 = this.mNotificationList.size();
                if (N2 > 0) {
                    pw.println("  Notification List:");
                    for (int i2 = 0; i2 < N2; i2++) {
                        NotificationRecord nr = this.mNotificationList.get(i2);
                        if (!filter.filtered || filter.matches(nr.sbn)) {
                            nr.dump(pw, "    ", getContext(), filter.redact);
                        }
                    }
                    pw.println("  ");
                }
                if (!filter.filtered) {
                    int N3 = this.mLights.size();
                    if (N3 > 0) {
                        pw.println("  Lights List:");
                        for (int i3 = 0; i3 < N3; i3++) {
                            if (i3 == N3 - 1) {
                                pw.print("  > ");
                            } else {
                                pw.print("    ");
                            }
                            pw.println(this.mLights.get(i3));
                        }
                        pw.println("  ");
                    }
                    pw.println("  mUseAttentionLight=" + this.mUseAttentionLight);
                    pw.println("  mNotificationPulseEnabled=" + this.mNotificationPulseEnabled);
                    pw.println("  mSoundNotificationKey=" + this.mSoundNotificationKey);
                    pw.println("  mVibrateNotificationKey=" + this.mVibrateNotificationKey);
                    pw.println("  mDisableNotificationEffects=" + this.mDisableNotificationEffects);
                    pw.println("  mCallState=" + callStateToString(this.mCallState));
                    pw.println("  mSystemReady=" + this.mSystemReady);
                    pw.println("  mMaxPackageEnqueueRate=" + this.mMaxPackageEnqueueRate);
                }
                pw.println("  mArchive=" + this.mArchive.toString());
                Iterator<StatusBarNotification> iter = this.mArchive.descendingIterator();
                int i4 = 0;
                while (true) {
                    if (!iter.hasNext()) {
                        break;
                    }
                    StatusBarNotification sbn = iter.next();
                    if (filter == null || filter.matches(sbn)) {
                        pw.println("    " + sbn);
                        i4++;
                        if (i4 >= 5) {
                            if (iter.hasNext()) {
                                pw.println("    ...");
                            }
                        }
                    }
                }
                if (!zenOnly) {
                    pw.println("\n  Usage Stats:");
                    this.mUsageStats.dump(pw, "    ", filter);
                }
                if (filter.filtered || zenOnly) {
                    pw.println("\n  Zen Mode:");
                    pw.print("    mInterruptionFilter=");
                    pw.println(this.mInterruptionFilter);
                    this.mZenModeHelper.dump(pw, "    ");
                    pw.println("\n  Zen Log:");
                    ZenLog.dump(pw, "    ");
                }
                if (!zenOnly) {
                    pw.println("\n  Ranking Config:");
                    this.mRankingHelper.dump(pw, "    ", filter);
                    pw.println("\n  Notification listeners:");
                    this.mListeners.dump(pw, filter);
                    pw.print("    mListenerHints: ");
                    pw.println(this.mListenerHints);
                    pw.print("    mListenersDisablingEffects: (");
                    int N4 = this.mListenersDisablingEffects.size();
                    for (int i5 = 0; i5 < N4; i5++) {
                        int hint = this.mListenersDisablingEffects.keyAt(i5);
                        if (i5 > 0) {
                            pw.print(';');
                        }
                        pw.print("hint[" + hint + "]:");
                        ArraySet<ManagedServices.ManagedServiceInfo> listeners = this.mListenersDisablingEffects.valueAt(i5);
                        int listenerSize = listeners.size();
                        for (int j = 0; j < listenerSize; j++) {
                            if (i5 > 0) {
                                pw.print(',');
                            }
                            ManagedServices.ManagedServiceInfo listener = listeners.valueAt(i5);
                            pw.print(listener.component);
                        }
                    }
                    pw.println(')');
                    pw.println("\n  mRankerServicePackageName: " + this.mRankerServicePackageName);
                    pw.println("\n  Notification ranker services:");
                    this.mRankerServices.dump(pw, filter);
                }
                pw.println("\n  Policy access:");
                pw.print("    mPolicyAccess: ");
                pw.println(this.mPolicyAccess);
                pw.println("\n  Condition providers:");
                this.mConditionProviders.dump(pw, filter);
                pw.println("\n  Group summaries:");
                for (Map.Entry<String, NotificationRecord> entry : this.mSummaryByGroupKey.entrySet()) {
                    NotificationRecord r = entry.getValue();
                    pw.println("    " + entry.getKey() + " -> " + r.getKey());
                    if (this.mNotificationsByKey.get(r.getKey()) != r) {
                        pw.println("!!!!!!LEAK: Record not found in mNotificationsByKey.");
                        r.dump(pw, "      ", getContext(), filter.redact);
                    }
                }
            } else {
                if (!zenOnly) {
                }
                if (filter.filtered) {
                    pw.println("\n  Zen Mode:");
                    pw.print("    mInterruptionFilter=");
                    pw.println(this.mInterruptionFilter);
                    this.mZenModeHelper.dump(pw, "    ");
                    pw.println("\n  Zen Log:");
                    ZenLog.dump(pw, "    ");
                    if (!zenOnly) {
                    }
                    pw.println("\n  Policy access:");
                    pw.print("    mPolicyAccess: ");
                    pw.println(this.mPolicyAccess);
                    pw.println("\n  Condition providers:");
                    this.mConditionProviders.dump(pw, filter);
                    pw.println("\n  Group summaries:");
                    while (entry$iterator.hasNext()) {
                    }
                }
            }
        }
    }

    void enqueueNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int[] idOut, int incomingUserId) {
        int intentCount;
        Slog.v(TAG, "enqueueNotificationInternal: pkg=" + pkg + " id=" + id + " notification=" + notification);
        boolean foundTarget = false;
        if (pkg != null && pkg.contains(".stub") && notification != null) {
            String contentTitle = notification.extras != null ? notification.extras.getString("android.title") : " ";
            if (contentTitle != null && contentTitle.startsWith("notify#")) {
                foundTarget = true;
                Slog.d(TAG, "enqueueNotification, found notification, callingUid: " + callingUid + ", callingPid: " + callingPid + ", pkg: " + pkg + ", id: " + id + ", tag: " + tag);
            }
        }
        checkCallerIsSystemOrSameApp(pkg);
        boolean zEquals = !isUidSystem(callingUid) ? "android".equals(pkg) : true;
        boolean isNotificationFromListener = this.mListeners.isListenerPackage(pkg);
        int userId = ActivityManager.handleIncomingUser(callingPid, callingUid, incomingUserId, true, false, "enqueueNotification", pkg);
        UserHandle user = new UserHandle(userId);
        try {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfoAsUser(pkg, 268435456, userId == -1 ? 0 : userId);
            Notification.addFieldsFromContext(ai, userId, notification);
            this.mUsageStats.registerEnqueuedByApp(pkg);
            if (!zEquals && !isNotificationFromListener) {
                synchronized (this.mNotificationList) {
                    float appEnqueueRate = this.mUsageStats.getAppEnqueueRate(pkg);
                    if (appEnqueueRate > this.mMaxPackageEnqueueRate) {
                        this.mUsageStats.registerOverRateQuota(pkg);
                        long now = SystemClock.elapsedRealtime();
                        if (now - this.mLastOverRateLogTime > 5000) {
                            Slog.e(TAG, "Package enqueue rate is " + appEnqueueRate + ". Shedding events. package=" + pkg);
                            this.mLastOverRateLogTime = now;
                        }
                        return;
                    }
                    int count = 0;
                    int N = this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = this.mNotificationList.get(i);
                        if (r.sbn.getPackageName().equals(pkg) && r.sbn.getUserId() == userId) {
                            if (r.sbn.getId() == id && TextUtils.equals(r.sbn.getTag(), tag)) {
                                break;
                            }
                            count++;
                            if (count >= 50) {
                                this.mUsageStats.registerOverCountQuota(pkg);
                                Slog.e(TAG, "Package has already posted " + count + " notifications.  Not showing more.  package=" + pkg);
                                return;
                            }
                        }
                    }
                }
            }
            if (pkg == null || notification == null) {
                throw new IllegalArgumentException("null not allowed: pkg=" + pkg + " id=" + id + " notification=" + notification);
            }
            if (notification.allPendingIntents != null && (intentCount = notification.allPendingIntents.size()) > 0) {
                ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                long duration = ((DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class)).getNotificationWhitelistDuration();
                for (int i2 = 0; i2 < intentCount; i2++) {
                    PendingIntent pendingIntent = (PendingIntent) notification.allPendingIntents.valueAt(i2);
                    if (pendingIntent != null) {
                        am.setPendingIntentWhitelistDuration(pendingIntent.getTarget(), duration);
                    }
                }
            }
            notification.priority = clamp(notification.priority, -2, 2);
            StatusBarNotification n = new StatusBarNotification(pkg, opPkg, id, tag, callingUid, callingPid, 0, notification, user);
            this.mHandler.post(new EnqueueNotificationRunnable(userId, new NotificationRecord(getContext(), n)));
            idOut[0] = id;
            if (foundTarget) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Slog.e(TAG, "Cannot create a context for sending app", e2);
        }
    }

    private class EnqueueNotificationRunnable implements Runnable {
        private final NotificationRecord r;
        private final int userId;

        EnqueueNotificationRunnable(int userId, NotificationRecord r) {
            this.userId = userId;
            this.r = r;
        }

        @Override
        public void run() {
            boolean zEquals;
            synchronized (NotificationManagerService.this.mNotificationList) {
                StatusBarNotification n = this.r.sbn;
                Slog.d(NotificationManagerService.TAG, "EnqueueNotificationRunnable.run for: " + n.getKey());
                NotificationRecord old = NotificationManagerService.this.mNotificationsByKey.get(n.getKey());
                if (old != null) {
                    this.r.copyRankingInformation(old);
                }
                int callingUid = n.getUid();
                int callingPid = n.getInitialPid();
                Notification notification = n.getNotification();
                String pkg = n.getPackageName();
                int id = n.getId();
                String tag = n.getTag();
                if (NotificationManagerService.isUidSystem(callingUid)) {
                    zEquals = true;
                } else {
                    zEquals = "android".equals(pkg);
                }
                NotificationManagerService.this.handleGroupedNotificationLocked(this.r, old, callingUid, callingPid);
                if (!pkg.equals("com.android.providers.downloads") || Log.isLoggable("DownloadManager", 2)) {
                    int enqueueStatus = 0;
                    if (old != null) {
                        enqueueStatus = 1;
                    }
                    EventLogTags.writeNotificationEnqueue(callingUid, callingPid, pkg, id, tag, this.userId, notification.toString(), enqueueStatus);
                }
                NotificationManagerService.this.mRankingHelper.extractSignals(this.r);
                boolean isPackageSuspended = NotificationManagerService.this.isPackageSuspendedForUser(pkg, callingUid);
                if ((this.r.getImportance() == 0 || !NotificationManagerService.this.noteNotificationOp(pkg, callingUid) || isPackageSuspended) && !zEquals) {
                    if (isPackageSuspended) {
                        Slog.e(NotificationManagerService.TAG, "Suppressing notification from package due to package suspended by administrator.");
                        NotificationManagerService.this.mUsageStats.registerSuspendedByAdmin(this.r);
                    } else {
                        Slog.e(NotificationManagerService.TAG, "Suppressing notification from package by user request.");
                        NotificationManagerService.this.mUsageStats.registerBlocked(this.r);
                    }
                    return;
                }
                if (NotificationManagerService.this.mRankerServices.isEnabled()) {
                    NotificationManagerService.this.mRankerServices.onNotificationEnqueued(this.r);
                }
                int index = NotificationManagerService.this.indexOfNotificationLocked(n.getKey());
                if (index < 0) {
                    NotificationManagerService.this.mNotificationList.add(this.r);
                    NotificationManagerService.this.mUsageStats.registerPostedByApp(this.r);
                } else {
                    old = NotificationManagerService.this.mNotificationList.get(index);
                    NotificationManagerService.this.mNotificationList.set(index, this.r);
                    NotificationManagerService.this.mUsageStats.registerUpdatedByApp(this.r, old);
                    notification.flags |= old.getNotification().flags & 64;
                    this.r.isUpdate = true;
                }
                NotificationManagerService.this.mNotificationsByKey.put(n.getKey(), this.r);
                if ((notification.flags & 64) != 0) {
                    notification.flags |= 34;
                }
                NotificationManagerService.this.applyZenModeLocked(this.r);
                NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                if (notification.getSmallIcon() != null && (notification.flags & 268435456) == 0) {
                    NotificationManagerService.this.mListeners.notifyPostedLocked(n, old != null ? old.sbn : null);
                } else {
                    Slog.e(NotificationManagerService.TAG, "Not posting notification without small icon: " + notification);
                    if (old != null && !old.isCanceled) {
                        NotificationManagerService.this.mListeners.notifyRemovedLocked(n);
                    }
                    Slog.e(NotificationManagerService.TAG, "WARNING: In a future release this will crash the app: " + n.getPackageName());
                }
                NotificationManagerService.this.buzzBeepBlinkLocked(this.r);
            }
        }
    }

    private void handleGroupedNotificationLocked(NotificationRecord r, NotificationRecord old, int callingUid, int callingPid) {
        NotificationRecord removedSummary;
        StatusBarNotification sbn = r.sbn;
        Notification n = sbn.getNotification();
        if (n.isGroupSummary() && !sbn.isAppGroup()) {
            n.flags &= -513;
        }
        String group = sbn.getGroupKey();
        boolean isSummary = n.isGroupSummary();
        Notification notification = old != null ? old.sbn.getNotification() : null;
        String groupKey = old != null ? old.sbn.getGroupKey() : null;
        boolean oldIsSummary = old != null ? notification.isGroupSummary() : false;
        if (oldIsSummary && (removedSummary = this.mSummaryByGroupKey.remove(groupKey)) != old) {
            String removedKey = removedSummary != null ? removedSummary.getKey() : "<null>";
            Slog.w(TAG, "Removed summary didn't match old notification: old=" + old.getKey() + ", removed=" + removedKey);
        }
        if (isSummary) {
            this.mSummaryByGroupKey.put(group, r);
        }
        if (oldIsSummary) {
            if (isSummary && groupKey.equals(group)) {
                return;
            }
            cancelGroupChildrenLocked(old, callingUid, callingPid, null, 12, false);
        }
    }

    void buzzBeepBlinkLocked(NotificationRecord record) {
        boolean enableAlerts;
        boolean buzz = false;
        boolean beep = false;
        boolean blink = false;
        Notification notification = record.sbn.getNotification();
        String key = record.getKey();
        boolean aboveThreshold = record.getImportance() >= 3;
        boolean canInterrupt = aboveThreshold && !record.isIntercepted();
        Slog.v(TAG, "pkg=" + record.sbn.getPackageName() + " canInterrupt=" + canInterrupt + " intercept=" + record.isIntercepted());
        long identity = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            Binder.restoreCallingIdentity(identity);
            String disableEffects = disableNotificationEffects(record);
            if (disableEffects != null) {
                ZenLog.traceDisableEffects(record, disableEffects);
            }
            boolean zEquals = key != null ? key.equals(this.mSoundNotificationKey) : false;
            boolean zEquals2 = key != null ? key.equals(this.mVibrateNotificationKey) : false;
            boolean hasValidVibrate = false;
            boolean hasValidSound = false;
            if (((this.mDisabledNotifications & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) == 0 || record.sbn.getPackageName().equals("com.android.mms")) && !this.mDmLock) {
                enableAlerts = !this.mPplLock;
            } else {
                enableAlerts = false;
            }
            if (enableAlerts && disableEffects == null && ((record.getUserId() == -1 || record.getUserId() == currentUser || this.mUserProfiles.isCurrentProfile(record.getUserId())) && canInterrupt && this.mSystemReady && this.mAudioManager != null)) {
                Slog.v(TAG, "Interrupting!");
                boolean useDefaultSound = (notification.defaults & 1) == 0 ? Settings.System.DEFAULT_NOTIFICATION_URI.equals(notification.sound) : true;
                Uri soundUri = null;
                if (useDefaultSound) {
                    soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                    ContentResolver resolver = getContext().getContentResolver();
                    hasValidSound = Settings.System.getString(resolver, "notification_sound") != null;
                } else if (notification.sound != null) {
                    soundUri = notification.sound;
                    hasValidSound = soundUri != null;
                }
                boolean hasCustomVibrate = notification.vibrate != null;
                boolean convertSoundToVibration = !hasCustomVibrate && hasValidSound && this.mAudioManager.getRingerModeInternal() == 1;
                boolean useDefaultVibrate = (notification.defaults & 2) != 0;
                hasValidVibrate = (useDefaultVibrate || convertSoundToVibration) ? true : hasCustomVibrate;
                boolean z = record.isUpdate && (notification.flags & 8) != 0;
                if (!z) {
                    sendAccessibilityEvent(notification, record.sbn.getPackageName());
                    if (hasValidSound) {
                        boolean looping = (notification.flags & 4) != 0;
                        AudioAttributes audioAttributes = audioAttributesForNotification(notification);
                        this.mSoundNotificationKey = key;
                        if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(audioAttributes)) != 0 && !this.mAudioManager.isAudioFocusExclusive()) {
                            identity = Binder.clearCallingIdentity();
                            try {
                                IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                                if (player != null) {
                                    Slog.v(TAG, "Playing sound " + soundUri + " with attributes " + audioAttributes);
                                    player.playAsync(soundUri, record.sbn.getUser(), looping, audioAttributes);
                                    beep = true;
                                }
                            } catch (RemoteException e) {
                            } finally {
                            }
                        }
                    }
                    if (hasValidVibrate && this.mAudioManager.getRingerModeInternal() != 0) {
                        this.mVibrateNotificationKey = key;
                        if (useDefaultVibrate || convertSoundToVibration) {
                            identity = Binder.clearCallingIdentity();
                            try {
                                this.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), useDefaultVibrate ? this.mDefaultVibrationPattern : this.mFallbackVibrationPattern, (notification.flags & 4) != 0 ? 0 : -1, audioAttributesForNotification(notification));
                                buzz = true;
                            } finally {
                            }
                        } else if (notification.vibrate.length > 1) {
                            this.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), notification.vibrate, (notification.flags & 4) != 0 ? 0 : -1, audioAttributesForNotification(notification));
                            buzz = true;
                        }
                    }
                }
            }
            if (zEquals && !hasValidSound) {
                clearSoundLocked();
            }
            if (zEquals2 && !hasValidVibrate) {
                clearVibrateLocked();
            }
            boolean wasShowLights = this.mLights.remove(key);
            if ((notification.flags & 1) != 0 && aboveThreshold && (record.getSuppressedVisualEffects() & 1) == 0) {
                this.mLights.add(key);
                updateLightsLocked();
                if (this.mUseAttentionLight) {
                    this.mAttentionLight.pulse();
                }
                blink = true;
            } else if (wasShowLights) {
                updateLightsLocked();
            }
            if (buzz || beep || blink) {
                if ((record.getSuppressedVisualEffects() & 1) != 0) {
                    Slog.v(TAG, "Suppressed SystemUI from triggering screen on");
                } else {
                    EventLogTags.writeNotificationAlert(key, buzz ? 1 : 0, beep ? 1 : 0, blink ? 1 : 0);
                    this.mHandler.post(this.mBuzzBeepBlinked);
                }
            }
        } finally {
        }
    }

    private static AudioAttributes audioAttributesForNotification(Notification n) {
        if (n.audioAttributes != null && !Notification.AUDIO_ATTRIBUTES_DEFAULT.equals(n.audioAttributes)) {
            return n.audioAttributes;
        }
        if (n.audioStreamType >= 0 && n.audioStreamType < AudioSystem.getNumStreamTypes()) {
            return new AudioAttributes.Builder().setInternalLegacyStreamType(n.audioStreamType).build();
        }
        if (n.audioStreamType == -1) {
            return Notification.AUDIO_ATTRIBUTES_DEFAULT;
        }
        Log.w(TAG, String.format("Invalid stream type: %d", Integer.valueOf(n.audioStreamType)));
        return Notification.AUDIO_ATTRIBUTES_DEFAULT;
    }

    void showNextToastLocked() {
        ToastRecord record = this.mToastQueue.get(0);
        while (record != null) {
            Slog.d(TAG, "Show pkg=" + record.pkg + " callback=" + record.callback);
            try {
                record.callback.show();
                scheduleTimeoutLocked(record);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Object died trying to show notification " + record.callback + " in package " + record.pkg);
                int index = this.mToastQueue.indexOf(record);
                if (index >= 0) {
                    this.mToastQueue.remove(index);
                }
                keepProcessAliveLocked(record.pid);
                if (this.mToastQueue.size() > 0) {
                    record = this.mToastQueue.get(0);
                } else {
                    record = null;
                }
            }
        }
    }

    void cancelToastLocked(int index) {
        ToastRecord record = this.mToastQueue.get(index);
        try {
            record.callback.hide();
        } catch (RemoteException e) {
            Slog.w(TAG, "Object died trying to hide notification " + record.callback + " in package " + record.pkg);
        }
        this.mToastQueue.remove(index);
        keepProcessAliveLocked(record.pid);
        if (this.mToastQueue.size() <= 0) {
            return;
        }
        showNextToastLocked();
    }

    private void scheduleTimeoutLocked(ToastRecord r) {
        this.mHandler.removeCallbacksAndMessages(r);
        Message m = Message.obtain(this.mHandler, 2, r);
        long delay = r.duration == 1 ? LONG_DELAY : SHORT_DELAY;
        this.mHandler.sendMessageDelayed(m, delay);
    }

    private void handleTimeout(ToastRecord record) {
        Slog.d(TAG, "Timeout pkg=" + record.pkg + " callback=" + record.callback);
        synchronized (this.mToastQueue) {
            int index = indexOfToastLocked(record.pkg, record.callback);
            if (index >= 0) {
                cancelToastLocked(index);
            }
        }
    }

    int indexOfToastLocked(String pkg, ITransientNotification callback) {
        IBinder cbak = callback.asBinder();
        ArrayList<ToastRecord> list = this.mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            ToastRecord r = list.get(i);
            if (r.pkg.equals(pkg) && r.callback.asBinder() == cbak) {
                return i;
            }
        }
        return -1;
    }

    void keepProcessAliveLocked(int pid) {
        int toastCount = 0;
        ArrayList<ToastRecord> list = this.mToastQueue;
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ToastRecord r = list.get(i);
            if (r.pid == pid) {
                toastCount++;
            }
        }
        try {
            this.mAm.setProcessForeground(this.mForegroundToken, pid, toastCount > 0);
        } catch (RemoteException e) {
        }
    }

    private void handleRankingReconsideration(Message message) {
        if (message.obj instanceof RankingReconsideration) {
            RankingReconsideration recon = (RankingReconsideration) message.obj;
            recon.run();
            synchronized (this.mNotificationList) {
                NotificationRecord record = this.mNotificationsByKey.get(recon.getKey());
                if (record == null) {
                    return;
                }
                int indexBefore = findNotificationRecordIndexLocked(record);
                boolean interceptBefore = record.isIntercepted();
                int visibilityBefore = record.getPackageVisibilityOverride();
                recon.applyChangesLocked(record);
                applyZenModeLocked(record);
                this.mRankingHelper.sort(this.mNotificationList);
                int indexAfter = findNotificationRecordIndexLocked(record);
                boolean interceptAfter = record.isIntercepted();
                int visibilityAfter = record.getPackageVisibilityOverride();
                boolean changed = (indexBefore == indexAfter && interceptBefore == interceptAfter && visibilityBefore == visibilityAfter) ? false : true;
                if (interceptBefore && !interceptAfter) {
                    buzzBeepBlinkLocked(record);
                }
                if (!changed) {
                    return;
                }
                scheduleSendRankingUpdate();
            }
        }
    }

    private void handleRankingSort() {
        synchronized (this.mNotificationList) {
            int N = this.mNotificationList.size();
            ArrayList<String> orderBefore = new ArrayList<>(N);
            ArrayList<String> groupOverrideBefore = new ArrayList<>(N);
            int[] visibilities = new int[N];
            int[] importances = new int[N];
            for (int i = 0; i < N; i++) {
                NotificationRecord r = this.mNotificationList.get(i);
                orderBefore.add(r.getKey());
                groupOverrideBefore.add(r.sbn.getGroupKey());
                visibilities[i] = r.getPackageVisibilityOverride();
                importances[i] = r.getImportance();
                this.mRankingHelper.extractSignals(r);
            }
            this.mRankingHelper.sort(this.mNotificationList);
            for (int i2 = 0; i2 < N; i2++) {
                NotificationRecord r2 = this.mNotificationList.get(i2);
                if (!orderBefore.get(i2).equals(r2.getKey()) || visibilities[i2] != r2.getPackageVisibilityOverride() || importances[i2] != r2.getImportance() || !groupOverrideBefore.get(i2).equals(r2.sbn.getGroupKey())) {
                    scheduleSendRankingUpdate();
                    return;
                }
            }
        }
    }

    private void applyZenModeLocked(NotificationRecord record) {
        record.setIntercepted(this.mZenModeHelper.shouldIntercept(record));
        if (!record.isIntercepted()) {
            return;
        }
        int suppressed = (this.mZenModeHelper.shouldSuppressWhenScreenOff() ? 1 : 0) | (this.mZenModeHelper.shouldSuppressWhenScreenOn() ? 2 : 0);
        record.setSuppressedVisualEffects(suppressed);
    }

    private int findNotificationRecordIndexLocked(NotificationRecord target) {
        return this.mRankingHelper.indexOf(this.mNotificationList, target);
    }

    private void scheduleSendRankingUpdate() {
        if (this.mHandler.hasMessages(4)) {
            return;
        }
        Message m = Message.obtain(this.mHandler, 4);
        this.mHandler.sendMessage(m);
    }

    private void handleSendRankingUpdate() {
        synchronized (this.mNotificationList) {
            this.mListeners.notifyRankingUpdateLocked();
        }
    }

    private void scheduleListenerHintsChanged(int state) {
        this.mHandler.removeMessages(5);
        this.mHandler.obtainMessage(5, state, 0).sendToTarget();
    }

    private void scheduleInterruptionFilterChanged(int listenerInterruptionFilter) {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, listenerInterruptionFilter, 0).sendToTarget();
    }

    private void handleListenerHintsChanged(int hints) {
        synchronized (this.mNotificationList) {
            this.mListeners.notifyListenerHintsChangedLocked(hints);
        }
    }

    private void handleListenerInterruptionFilterChanged(int interruptionFilter) {
        synchronized (this.mNotificationList) {
            this.mListeners.notifyInterruptionFilterChanged(interruptionFilter);
        }
    }

    private final class WorkerHandler extends Handler {
        WorkerHandler(NotificationManagerService this$0, WorkerHandler workerHandler) {
            this();
        }

        private WorkerHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    NotificationManagerService.this.handleTimeout((ToastRecord) msg.obj);
                    break;
                case 3:
                    NotificationManagerService.this.handleSavePolicyFile();
                    break;
                case 4:
                    NotificationManagerService.this.handleSendRankingUpdate();
                    break;
                case 5:
                    NotificationManagerService.this.handleListenerHintsChanged(msg.arg1);
                    break;
                case 6:
                    NotificationManagerService.this.handleListenerInterruptionFilterChanged(msg.arg1);
                    break;
            }
        }
    }

    private final class RankingHandlerWorker extends Handler implements RankingHandler {
        public RankingHandlerWorker(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    NotificationManagerService.this.handleRankingReconsideration(msg);
                    break;
                case 1001:
                    NotificationManagerService.this.handleRankingSort();
                    break;
            }
        }

        @Override
        public void requestSort() {
            removeMessages(1001);
            sendEmptyMessage(1001);
        }

        @Override
        public void requestReconsideration(RankingReconsideration recon) {
            Message m = Message.obtain(this, 1000, recon);
            long delay = recon.getDelay(TimeUnit.MILLISECONDS);
            sendMessageDelayed(m, delay);
        }
    }

    static int clamp(int x, int low, int high) {
        return x < low ? low : x > high ? high : x;
    }

    void sendAccessibilityEvent(Notification notification, CharSequence packageName) {
        AccessibilityManager manager = AccessibilityManager.getInstance(getContext());
        if (!manager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(64);
        event.setPackageName(packageName);
        event.setClassName(Notification.class.getName());
        event.setParcelableData(notification);
        CharSequence tickerText = notification.tickerText;
        if (!TextUtils.isEmpty(tickerText)) {
            event.getText().add(tickerText);
        }
        manager.sendAccessibilityEvent(event);
    }

    private void cancelNotificationLocked(NotificationRecord r, boolean sendDelete, int reason) {
        long identity;
        if (sendDelete && r.getNotification().deleteIntent != null) {
            try {
                r.getNotification().deleteIntent.send();
            } catch (PendingIntent.CanceledException ex) {
                Slog.w(TAG, "canceled PendingIntent for " + r.sbn.getPackageName(), ex);
            }
        }
        if (r.getNotification().getSmallIcon() != null) {
            r.isCanceled = true;
            this.mListeners.notifyRemovedLocked(r.sbn);
        }
        String canceledKey = r.getKey();
        if (canceledKey.equals(this.mSoundNotificationKey)) {
            this.mSoundNotificationKey = null;
            identity = Binder.clearCallingIdentity();
            try {
                IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                if (player != null) {
                    player.stopAsync();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (RemoteException e) {
            } finally {
            }
        }
        if (canceledKey.equals(this.mVibrateNotificationKey)) {
            this.mVibrateNotificationKey = null;
            identity = Binder.clearCallingIdentity();
            try {
                this.mVibrator.cancel();
            } finally {
            }
        }
        this.mLights.remove(canceledKey);
        switch (reason) {
            case 2:
            case 3:
            case 10:
            case 11:
                this.mUsageStats.registerDismissedByUser(r);
                break;
            case 8:
            case 9:
                this.mUsageStats.registerRemovedByApp(r);
                break;
        }
        this.mNotificationsByKey.remove(r.sbn.getKey());
        String groupKey = r.getGroupKey();
        NotificationRecord groupSummary = this.mSummaryByGroupKey.get(groupKey);
        if (groupSummary != null && groupSummary.getKey().equals(r.getKey())) {
            this.mSummaryByGroupKey.remove(groupKey);
        }
        ArrayMap<String, String> summaries = this.mAutobundledSummaries.get(Integer.valueOf(r.sbn.getUserId()));
        if (summaries != null && r.sbn.getKey().equals(summaries.get(r.sbn.getPackageName()))) {
            summaries.remove(r.sbn.getPackageName());
        }
        this.mArchive.record(r.sbn);
        long now = System.currentTimeMillis();
        EventLogTags.writeNotificationCanceled(canceledKey, reason, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now));
    }

    void cancelNotification(final int callingUid, final int callingPid, final String pkg, final String tag, final int id, final int mustHaveFlags, final int mustNotHaveFlags, final boolean sendDelete, final int userId, final int reason, final ManagedServices.ManagedServiceInfo listener) {
        boolean foundTarget = false;
        if (id == 9 && tag == null && pkg != null && pkg.contains("stub") && reason == 8) {
            Slog.d(TAG, "cancelNotification, pkg: " + pkg + ", id: " + id + ", tag: " + tag);
            synchronized (this.mNotificationList) {
                int index = indexOfNotificationLocked(pkg, tag, id, userId);
                if (index >= 0 && this.mNotificationList.get(index).getNotification() != null) {
                    Notification target = this.mNotificationList.get(index).getNotification();
                    String contentTitle = target.extras != null ? target.extras.getString("android.title") : "";
                    Slog.d(TAG, "contentTitle: " + contentTitle);
                    if ("notify#9".equalsIgnoreCase(contentTitle)) {
                        foundTarget = true;
                        Slog.d(TAG, "Found notification, callingUid: " + callingUid + ", callingPid: " + callingPid + ", pkg: " + pkg + ", id: " + id + ", tag: " + tag);
                    }
                }
            }
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                String shortString = listener == null ? null : listener.component.toShortString();
                EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, id, tag, userId, mustHaveFlags, mustNotHaveFlags, reason, shortString);
                synchronized (NotificationManagerService.this.mNotificationList) {
                    int index2 = NotificationManagerService.this.indexOfNotificationLocked(pkg, tag, id, userId);
                    if (index2 >= 0) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationList.get(index2);
                        if (reason == 1) {
                            NotificationManagerService.this.mUsageStats.registerClickedByUser(r);
                        }
                        if ((r.getNotification().flags & mustHaveFlags) != mustHaveFlags) {
                            return;
                        }
                        if ((r.getNotification().flags & mustNotHaveFlags) != 0) {
                            return;
                        }
                        NotificationManagerService.this.mNotificationList.remove(index2);
                        NotificationManagerService.this.cancelNotificationLocked(r, sendDelete, reason);
                        NotificationManagerService.this.cancelGroupChildrenLocked(r, callingUid, callingPid, shortString, 12, sendDelete);
                        NotificationManagerService.this.updateLightsLocked();
                    }
                }
            }
        });
        if (foundTarget) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }
        }
    }

    private boolean notificationMatchesUserId(NotificationRecord r, int userId) {
        return userId == -1 || r.getUserId() == -1 || r.getUserId() == userId;
    }

    private boolean notificationMatchesCurrentProfiles(NotificationRecord r, int userId) {
        if (notificationMatchesUserId(r, userId)) {
            return true;
        }
        return this.mUserProfiles.isCurrentProfile(r.getUserId());
    }

    boolean cancelAllNotificationsInt(int callingUid, int callingPid, String pkg, int mustHaveFlags, int mustNotHaveFlags, boolean doit, int userId, int reason, ManagedServices.ManagedServiceInfo listener) {
        String listenerName = listener == null ? null : listener.component.toShortString();
        EventLogTags.writeNotificationCancelAll(callingUid, callingPid, pkg, userId, mustHaveFlags, mustNotHaveFlags, reason, listenerName);
        synchronized (this.mNotificationList) {
            int N = this.mNotificationList.size();
            ArrayList<NotificationRecord> canceledNotifications = null;
            for (int i = N - 1; i >= 0; i--) {
                NotificationRecord r = this.mNotificationList.get(i);
                if (notificationMatchesUserId(r, userId) && ((r.getUserId() != -1 || pkg != null) && (r.getFlags() & mustHaveFlags) == mustHaveFlags && (r.getFlags() & mustNotHaveFlags) == 0 && (pkg == null || r.sbn.getPackageName().equals(pkg)))) {
                    if (canceledNotifications == null) {
                        canceledNotifications = new ArrayList<>();
                    }
                    canceledNotifications.add(r);
                    if (!doit) {
                        return true;
                    }
                    this.mNotificationList.remove(i);
                    cancelNotificationLocked(r, false, reason);
                }
            }
            if (doit && canceledNotifications != null) {
                int M = canceledNotifications.size();
                for (int i2 = 0; i2 < M; i2++) {
                    cancelGroupChildrenLocked(canceledNotifications.get(i2), callingUid, callingPid, listenerName, 12, false);
                }
            }
            if (canceledNotifications != null) {
                updateLightsLocked();
            }
            return canceledNotifications != null;
        }
    }

    void cancelAllLocked(int callingUid, int callingPid, int userId, int reason, ManagedServices.ManagedServiceInfo listener, boolean includeCurrentProfiles) {
        String listenerName = listener == null ? null : listener.component.toShortString();
        EventLogTags.writeNotificationCancelAll(callingUid, callingPid, null, userId, 0, 0, reason, listenerName);
        ArrayList<NotificationRecord> canceledNotifications = null;
        int N = this.mNotificationList.size();
        for (int i = N - 1; i >= 0; i--) {
            NotificationRecord r = this.mNotificationList.get(i);
            if (includeCurrentProfiles) {
                if (notificationMatchesCurrentProfiles(r, userId)) {
                    if ((r.getFlags() & 34) == 0) {
                        this.mNotificationList.remove(i);
                        cancelNotificationLocked(r, true, reason);
                        if (canceledNotifications == null) {
                            canceledNotifications = new ArrayList<>();
                        }
                        canceledNotifications.add(r);
                    }
                }
            } else if (notificationMatchesUserId(r, userId)) {
            }
        }
        int M = canceledNotifications != null ? canceledNotifications.size() : 0;
        for (int i2 = 0; i2 < M; i2++) {
            cancelGroupChildrenLocked(canceledNotifications.get(i2), callingUid, callingPid, listenerName, 12, false);
        }
        updateLightsLocked();
    }

    private void cancelGroupChildrenLocked(NotificationRecord r, int callingUid, int callingPid, String listenerName, int reason, boolean sendDelete) {
        Notification n = r.getNotification();
        if (!n.isGroupSummary()) {
            return;
        }
        String pkg = r.sbn.getPackageName();
        int userId = r.getUserId();
        if (pkg == null) {
            Log.e(TAG, "No package for group summary: " + r.getKey());
            return;
        }
        int N = this.mNotificationList.size();
        for (int i = N - 1; i >= 0; i--) {
            NotificationRecord childR = this.mNotificationList.get(i);
            StatusBarNotification childSbn = childR.sbn;
            if (childSbn.isGroup() && !childSbn.getNotification().isGroupSummary() && childR.getGroupKey().equals(r.getGroupKey())) {
                EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, childSbn.getId(), childSbn.getTag(), userId, 0, 0, reason, listenerName);
                this.mNotificationList.remove(i);
                cancelNotificationLocked(childR, sendDelete, reason);
            }
        }
    }

    void updateLightsLocked() {
        NotificationRecord ledNotification = null;
        while (ledNotification == null && !this.mLights.isEmpty()) {
            String owner = this.mLights.get(this.mLights.size() - 1);
            ledNotification = this.mNotificationsByKey.get(owner);
            if (ledNotification == null) {
                Slog.wtfStack(TAG, "LED Notification does not exist: " + owner);
                this.mLights.remove(owner);
            }
        }
        if (ledNotification == null || this.mInCall || this.mScreenOn || this.mDmLock || this.mPplLock) {
            this.mNotificationLight.turnOff();
            if (this.mStatusBar == null) {
                return;
            }
            this.mStatusBar.notificationLightOff();
            return;
        }
        Notification ledno = ledNotification.sbn.getNotification();
        int ledARGB = ledno.ledARGB;
        int ledOnMS = ledno.ledOnMS;
        int ledOffMS = ledno.ledOffMS;
        if ((ledno.defaults & 4) != 0) {
            ledARGB = this.mDefaultNotificationColor;
            ledOnMS = this.mDefaultNotificationLedOn;
            ledOffMS = this.mDefaultNotificationLedOff;
        }
        if (this.mNotificationPulseEnabled) {
            this.mNotificationLight.setFlashing(ledARGB, 1, ledOnMS, ledOffMS);
        }
        if (this.mStatusBar == null) {
            return;
        }
        this.mStatusBar.notificationLightPulse(ledARGB, ledOnMS, ledOffMS);
    }

    int indexOfNotificationLocked(String pkg, String tag, int id, int userId) {
        ArrayList<NotificationRecord> list = this.mNotificationList;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = list.get(i);
            if (notificationMatchesUserId(r, userId) && r.sbn.getId() == id && TextUtils.equals(r.sbn.getTag(), tag) && r.sbn.getPackageName().equals(pkg)) {
                return i;
            }
        }
        return -1;
    }

    int indexOfNotificationLocked(String key) {
        int N = this.mNotificationList.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(this.mNotificationList.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    private void updateNotificationPulse() {
        synchronized (this.mNotificationList) {
            updateLightsLocked();
        }
    }

    private static boolean isUidSystem(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || appid == 1001 || uid == 0;
    }

    private static boolean isCallerSystem() {
        return isUidSystem(Binder.getCallingUid());
    }

    private static void checkCallerIsSystem() {
        if (isCallerSystem()) {
        } else {
            throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
        }
    }

    private static void checkCallerIsSystemOrSameApp(String pkg) {
        if (isCallerSystem()) {
            return;
        }
        checkCallerIsSameApp(pkg);
    }

    private static void checkCallerIsSameApp(String pkg) {
        int uid = Binder.getCallingUid();
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
            if (ai == null) {
                throw new SecurityException("Unknown package " + pkg);
            }
            if (UserHandle.isSameApp(ai.uid, uid)) {
            } else {
                throw new SecurityException("Calling uid " + uid + " gave package" + pkg + " which is owned by uid " + ai.uid);
            }
        } catch (RemoteException re) {
            throw new SecurityException("Unknown package " + pkg + "\n" + re);
        }
    }

    private static String callStateToString(int state) {
        switch (state) {
            case 0:
                return "CALL_STATE_IDLE";
            case 1:
                return "CALL_STATE_RINGING";
            case 2:
                return "CALL_STATE_OFFHOOK";
            default:
                return "CALL_STATE_UNKNOWN_" + state;
        }
    }

    private void listenForCallState() {
        TelephonyManager.from(getContext()).listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (NotificationManagerService.this.mCallState == state) {
                    return;
                }
                Slog.d(NotificationManagerService.TAG, "Call state changed: " + NotificationManagerService.callStateToString(state));
                NotificationManagerService.this.mCallState = state;
            }
        }, 32);
    }

    private NotificationRankingUpdate makeRankingUpdateLocked(ManagedServices.ManagedServiceInfo info) {
        int N = this.mNotificationList.size();
        ArrayList<String> keys = new ArrayList<>(N);
        ArrayList<String> interceptedKeys = new ArrayList<>(N);
        ArrayList<Integer> importance = new ArrayList<>(N);
        Bundle overrideGroupKeys = new Bundle();
        Bundle visibilityOverrides = new Bundle();
        Bundle suppressedVisualEffects = new Bundle();
        Bundle explanation = new Bundle();
        for (int i = 0; i < N; i++) {
            NotificationRecord record = this.mNotificationList.get(i);
            if (isVisibleToListener(record.sbn, info)) {
                String key = record.sbn.getKey();
                keys.add(key);
                importance.add(Integer.valueOf(record.getImportance()));
                if (record.getImportanceExplanation() != null) {
                    explanation.putCharSequence(key, record.getImportanceExplanation());
                }
                if (record.isIntercepted()) {
                    interceptedKeys.add(key);
                }
                suppressedVisualEffects.putInt(key, record.getSuppressedVisualEffects());
                if (record.getPackageVisibilityOverride() != -1000) {
                    visibilityOverrides.putInt(key, record.getPackageVisibilityOverride());
                }
                overrideGroupKeys.putString(key, record.sbn.getOverrideGroupKey());
            }
        }
        int M = keys.size();
        String[] keysAr = (String[]) keys.toArray(new String[M]);
        String[] interceptedKeysAr = (String[]) interceptedKeys.toArray(new String[interceptedKeys.size()]);
        int[] importanceAr = new int[M];
        for (int i2 = 0; i2 < M; i2++) {
            importanceAr[i2] = importance.get(i2).intValue();
        }
        return new NotificationRankingUpdate(keysAr, interceptedKeysAr, visibilityOverrides, suppressedVisualEffects, importanceAr, explanation, overrideGroupKeys);
    }

    private boolean isVisibleToListener(StatusBarNotification sbn, ManagedServices.ManagedServiceInfo listener) {
        if (!listener.enabledAndUserMatches(sbn.getUserId())) {
            return false;
        }
        return true;
    }

    private boolean isPackageSuspendedForUser(String pkg, int uid) {
        int userId = UserHandle.getUserId(uid);
        try {
            return AppGlobals.getPackageManager().isPackageSuspendedForUser(pkg, userId);
        } catch (RemoteException e) {
            throw new SecurityException("Could not talk to package manager service");
        } catch (IllegalArgumentException e2) {
            return false;
        }
    }

    private class TrimCache {
        StatusBarNotification heavy;
        StatusBarNotification sbnClone;
        StatusBarNotification sbnCloneLight;

        TrimCache(StatusBarNotification sbn) {
            this.heavy = sbn;
        }

        StatusBarNotification ForListener(ManagedServices.ManagedServiceInfo info) {
            if (NotificationManagerService.this.mListeners.getOnNotificationPostedTrim(info) == 1) {
                if (this.sbnCloneLight == null) {
                    this.sbnCloneLight = this.heavy.cloneLight();
                }
                return this.sbnCloneLight;
            }
            if (this.sbnClone == null) {
                this.sbnClone = this.heavy.clone();
            }
            return this.sbnClone;
        }
    }

    public class NotificationRankers extends ManagedServices {
        public NotificationRankers() {
            super(NotificationManagerService.this.getContext(), NotificationManagerService.this.mHandler, NotificationManagerService.this.mNotificationList, NotificationManagerService.this.mUserProfiles);
        }

        @Override
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config c = new ManagedServices.Config();
            c.caption = "notification ranker service";
            c.serviceInterface = "android.service.notification.NotificationRankerService";
            c.secureSettingName = null;
            c.bindPermission = "android.permission.BIND_NOTIFICATION_RANKER_SERVICE";
            c.settingsAction = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";
            if (BenesseExtension.getDchaState() != 0) {
                c.settingsAction = null;
            }
            c.clientLabel = R.string.fp_power_button_bp_negative_button;
            return c;
        }

        @Override
        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        @Override
        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        @Override
        protected void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
            NotificationManagerService.this.mListeners.registerGuestService(info);
        }

        @Override
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
            NotificationManagerService.this.mListeners.unregisterService(removed.service, removed.userid);
        }

        public void onNotificationEnqueued(NotificationRecord r) {
            StatusBarNotification sbn = r.sbn;
            TrimCache trimCache = NotificationManagerService.this.new TrimCache(sbn);
            for (final ManagedServices.ManagedServiceInfo info : this.mServices) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, info);
                if (sbnVisible) {
                    final int importance = r.getImportance();
                    final boolean fromUser = r.isImportanceFromUser();
                    final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationRankers.this.notifyEnqueued(info, sbnToPost, importance, fromUser);
                        }
                    });
                }
            }
        }

        private void notifyEnqueued(ManagedServices.ManagedServiceInfo info, StatusBarNotification sbn, int importance, boolean fromUser) {
            INotificationListener ranker = info.service;
            StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbn);
            try {
                ranker.onNotificationEnqueued(sbnHolder, importance, fromUser);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify ranker (enqueued): " + ranker, ex);
            }
        }

        public boolean isEnabled() {
            return !this.mServices.isEmpty();
        }

        @Override
        public void onUserSwitched(int user) {
            synchronized (NotificationManagerService.this.mNotificationList) {
                int i = this.mServices.size() - 1;
                while (true) {
                    int i2 = i - 1;
                    if (i > 0) {
                        ManagedServices.ManagedServiceInfo info = this.mServices.get(i2);
                        unregisterService(info.service, info.userid);
                        i = i2;
                    }
                }
            }
            registerRanker();
        }

        @Override
        public void onPackagesChanged(boolean queryReplace, String[] pkgList) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "onPackagesChanged queryReplace=" + queryReplace + " pkgList=" + (pkgList != null ? Arrays.asList(pkgList) : null));
            }
            if (NotificationManagerService.this.mRankerServicePackageName == null || pkgList == null || pkgList.length <= 0) {
                return;
            }
            for (String pkgName : pkgList) {
                if (NotificationManagerService.this.mRankerServicePackageName.equals(pkgName)) {
                    registerRanker();
                }
            }
        }

        protected void registerRanker() {
            if (NotificationManagerService.this.mRankerServicePackageName == null) {
                Slog.w(this.TAG, "could not start ranker service: no package specified!");
                return;
            }
            Set<ComponentName> rankerComponents = queryPackageForServices(NotificationManagerService.this.mRankerServicePackageName, 0);
            Iterator<ComponentName> iterator = rankerComponents.iterator();
            if (iterator.hasNext()) {
                ComponentName rankerComponent = iterator.next();
                if (iterator.hasNext()) {
                    Slog.e(this.TAG, "found multiple ranker services:" + rankerComponents);
                    return;
                } else {
                    registerSystemService(rankerComponent, 0);
                    return;
                }
            }
            Slog.w(this.TAG, "could not start ranker service: none found");
        }
    }

    public class NotificationListeners extends ManagedServices {
        private final ArraySet<ManagedServices.ManagedServiceInfo> mLightTrimListeners;

        public NotificationListeners() {
            super(NotificationManagerService.this.getContext(), NotificationManagerService.this.mHandler, NotificationManagerService.this.mNotificationList, NotificationManagerService.this.mUserProfiles);
            this.mLightTrimListeners = new ArraySet<>();
        }

        @Override
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config c = new ManagedServices.Config();
            c.caption = "notification listener";
            c.serviceInterface = "android.service.notification.NotificationListenerService";
            c.secureSettingName = "enabled_notification_listeners";
            c.bindPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
            c.settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
            if (BenesseExtension.getDchaState() != 0) {
                c.settingsAction = null;
            }
            c.clientLabel = R.string.forward_intent_to_owner;
            return c;
        }

        @Override
        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        @Override
        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        @Override
        public void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
            NotificationRankingUpdate update;
            INotificationListener listener = info.service;
            synchronized (NotificationManagerService.this.mNotificationList) {
                update = NotificationManagerService.this.makeRankingUpdateLocked(info);
            }
            try {
                listener.onListenerConnected(update);
            } catch (RemoteException e) {
            }
        }

        @Override
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
            if (NotificationManagerService.this.removeDisabledHints(removed)) {
                NotificationManagerService.this.updateListenerHintsLocked();
                NotificationManagerService.this.updateEffectsSuppressorLocked();
            }
            this.mLightTrimListeners.remove(removed);
        }

        public void setOnNotificationPostedTrimLocked(ManagedServices.ManagedServiceInfo info, int trim) {
            if (trim == 1) {
                this.mLightTrimListeners.add(info);
            } else {
                this.mLightTrimListeners.remove(info);
            }
        }

        public int getOnNotificationPostedTrim(ManagedServices.ManagedServiceInfo info) {
            return this.mLightTrimListeners.contains(info) ? 1 : 0;
        }

        public void notifyPostedLocked(StatusBarNotification sbn, StatusBarNotification oldSbn) {
            TrimCache trimCache = NotificationManagerService.this.new TrimCache(sbn);
            for (final ManagedServices.ManagedServiceInfo info : this.mServices) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, info);
                boolean oldSbnVisible = oldSbn != null ? NotificationManagerService.this.isVisibleToListener(oldSbn, info) : false;
                if (oldSbnVisible || sbnVisible) {
                    final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                    if (oldSbnVisible && !sbnVisible) {
                        final StatusBarNotification oldSbnLightClone = oldSbn.cloneLight();
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyRemoved(info, oldSbnLightClone, update);
                            }
                        });
                    } else {
                        final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyPosted(info, sbnToPost, update);
                            }
                        });
                    }
                }
            }
        }

        public void notifyRemovedLocked(StatusBarNotification sbn) {
            final StatusBarNotification sbnLight = sbn.cloneLight();
            for (final ManagedServices.ManagedServiceInfo info : this.mServices) {
                if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                    final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyRemoved(info, sbnLight, update);
                        }
                    });
                }
            }
        }

        public void notifyRankingUpdateLocked() {
            for (final ManagedServices.ManagedServiceInfo serviceInfo : this.mServices) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(serviceInfo);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyRankingUpdate(serviceInfo, update);
                        }
                    });
                }
            }
        }

        public void notifyListenerHintsChangedLocked(final int hints) {
            for (final ManagedServices.ManagedServiceInfo serviceInfo : this.mServices) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyListenerHintsChanged(serviceInfo, hints);
                        }
                    });
                }
            }
        }

        public void notifyInterruptionFilterChanged(final int interruptionFilter) {
            for (final ManagedServices.ManagedServiceInfo serviceInfo : this.mServices) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyInterruptionFilterChanged(serviceInfo, interruptionFilter);
                        }
                    });
                }
            }
        }

        private void notifyPosted(ManagedServices.ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbn);
            try {
                listener.onNotificationPosted(sbnHolder, rankingUpdate);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify listener (posted): " + listener, ex);
            }
        }

        private void notifyRemoved(ManagedServices.ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate) {
            if (!info.enabledAndUserMatches(sbn.getUserId())) {
                return;
            }
            INotificationListener listener = info.service;
            StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbn);
            try {
                listener.onNotificationRemoved(sbnHolder, rankingUpdate);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify listener (removed): " + listener, ex);
            }
        }

        private void notifyRankingUpdate(ManagedServices.ManagedServiceInfo info, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationRankingUpdate(rankingUpdate);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify listener (ranking update): " + listener, ex);
            }
        }

        private void notifyListenerHintsChanged(ManagedServices.ManagedServiceInfo info, int hints) {
            INotificationListener listener = info.service;
            try {
                listener.onListenerHintsChanged(hints);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify listener (listener hints): " + listener, ex);
            }
        }

        private void notifyInterruptionFilterChanged(ManagedServices.ManagedServiceInfo info, int interruptionFilter) {
            INotificationListener listener = info.service;
            try {
                listener.onInterruptionFilterChanged(interruptionFilter);
            } catch (RemoteException ex) {
                Log.e(this.TAG, "unable to notify listener (interruption filter): " + listener, ex);
            }
        }

        private boolean isListenerPackage(String packageName) {
            if (packageName == null) {
                return false;
            }
            synchronized (NotificationManagerService.this.mNotificationList) {
                for (ManagedServices.ManagedServiceInfo serviceInfo : this.mServices) {
                    if (packageName.equals(serviceInfo.component.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public static final class DumpFilter {
        public String pkgFilter;
        public long since;
        public boolean stats;
        public boolean zen;
        public boolean filtered = false;
        public boolean redact = true;

        public static DumpFilter parseFromArguments(String[] args) {
            DumpFilter filter = new DumpFilter();
            int ai = 0;
            while (ai < args.length) {
                String a = args[ai];
                if ("--noredact".equals(a) || "--reveal".equals(a)) {
                    filter.redact = false;
                } else if ("p".equals(a) || "pkg".equals(a) || "--package".equals(a)) {
                    if (ai < args.length - 1) {
                        ai++;
                        filter.pkgFilter = args[ai].trim().toLowerCase();
                        if (filter.pkgFilter.isEmpty()) {
                            filter.pkgFilter = null;
                        } else {
                            filter.filtered = true;
                        }
                    }
                } else if ("--zen".equals(a) || "zen".equals(a)) {
                    filter.filtered = true;
                    filter.zen = true;
                } else if ("--stats".equals(a)) {
                    filter.stats = true;
                    if (ai < args.length - 1) {
                        ai++;
                        filter.since = Long.valueOf(args[ai]).longValue();
                    } else {
                        filter.since = 0L;
                    }
                }
                ai++;
            }
            return filter;
        }

        public boolean matches(StatusBarNotification sbn) {
            if (!this.filtered || this.zen) {
                return true;
            }
            if (sbn == null) {
                return false;
            }
            if (matches(sbn.getPackageName())) {
                return true;
            }
            return matches(sbn.getOpPkg());
        }

        public boolean matches(ComponentName component) {
            if (!this.filtered || this.zen) {
                return true;
            }
            if (component != null) {
                return matches(component.getPackageName());
            }
            return false;
        }

        public boolean matches(String pkg) {
            if (!this.filtered || this.zen) {
                return true;
            }
            if (pkg != null) {
                return pkg.toLowerCase().contains(this.pkgFilter);
            }
            return false;
        }

        public String toString() {
            return this.stats ? "stats" : this.zen ? "zen" : '\'' + this.pkgFilter + '\'';
        }
    }

    private static final class StatusBarNotificationHolder extends IStatusBarNotificationHolder.Stub {
        private StatusBarNotification mValue;

        public StatusBarNotificationHolder(StatusBarNotification value) {
            this.mValue = value;
        }

        public StatusBarNotification get() {
            StatusBarNotification value = this.mValue;
            this.mValue = null;
            return value;
        }
    }

    private final class PolicyAccess {
        private static final String SEPARATOR = ":";
        private final String[] PERM;

        PolicyAccess(NotificationManagerService this$0, PolicyAccess policyAccess) {
            this();
        }

        private PolicyAccess() {
            this.PERM = new String[]{"android.permission.ACCESS_NOTIFICATION_POLICY"};
        }

        public boolean isPackageGranted(String pkg) {
            if (pkg != null) {
                return getGrantedPackages().contains(pkg);
            }
            return false;
        }

        public void put(String pkg, boolean granted) {
            boolean changed;
            if (pkg == null) {
                return;
            }
            ArraySet<String> pkgs = getGrantedPackages();
            if (granted) {
                changed = pkgs.add(pkg);
            } else {
                changed = pkgs.remove(pkg);
            }
            if (changed) {
                String setting = TextUtils.join(SEPARATOR, pkgs);
                int currentUser = ActivityManager.getCurrentUser();
                Settings.Secure.putStringForUser(NotificationManagerService.this.getContext().getContentResolver(), "enabled_notification_policy_access_packages", setting, currentUser);
                NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(pkg).addFlags(1073741824), new UserHandle(currentUser), null);
            }
        }

        public ArraySet<String> getGrantedPackages() {
            ArraySet<String> pkgs = new ArraySet<>();
            long identity = Binder.clearCallingIdentity();
            try {
                String setting = Settings.Secure.getStringForUser(NotificationManagerService.this.getContext().getContentResolver(), "enabled_notification_policy_access_packages", ActivityManager.getCurrentUser());
                if (setting != null) {
                    String[] tokens = setting.split(SEPARATOR);
                    for (int i = 0; i < tokens.length; i++) {
                        String token = tokens[i];
                        if (token != null) {
                            token = token.trim();
                        }
                        if (!TextUtils.isEmpty(token)) {
                            pkgs.add(token);
                        }
                    }
                }
                return pkgs;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public String[] getRequestingPackages() throws RemoteException {
            ParceledListSlice list = AppGlobals.getPackageManager().getPackagesHoldingPermissions(this.PERM, 0, ActivityManager.getCurrentUser());
            List<PackageInfo> pkgs = list.getList();
            if (pkgs == null || pkgs.isEmpty()) {
                return new String[0];
            }
            int N = pkgs.size();
            String[] rt = new String[N];
            for (int i = 0; i < N; i++) {
                rt[i] = pkgs.get(i).packageName;
            }
            return rt;
        }
    }
}
