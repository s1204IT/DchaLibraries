package com.android.server.notification;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.Notification;
import android.app.PendingIntent;
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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IRingtonePlayer;
import android.net.Uri;
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
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
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
import android.util.Xml;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.ZenModeHelper;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NotificationManagerService extends SystemService {
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int DB_VERSION = 1;
    static final int DEFAULT_STREAM_TYPE = 5;
    static final boolean ENABLE_BLOCKED_NOTIFICATIONS = true;
    static final boolean ENABLE_BLOCKED_TOASTS = true;
    private static final int EVENTLOG_ENQUEUE_STATUS_IGNORED = 2;
    private static final int EVENTLOG_ENQUEUE_STATUS_NEW = 0;
    private static final int EVENTLOG_ENQUEUE_STATUS_UPDATE = 1;
    static final int JUNK_SCORE = -1000;
    static final int LONG_DELAY = 3500;
    static final int MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS = 3000;
    static final float MATCHES_CALL_FILTER_TIMEOUT_AFFINITY = 1.0f;
    static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    static final int MESSAGE_LISTENER_HINTS_CHANGED = 7;
    static final int MESSAGE_LISTENER_NOTIFICATION_FILTER_CHANGED = 8;
    static final int MESSAGE_RANKING_CONFIG_CHANGE = 5;
    static final int MESSAGE_RECONSIDER_RANKING = 4;
    static final int MESSAGE_SAVE_POLICY_FILE = 3;
    static final int MESSAGE_SEND_RANKING_UPDATE = 6;
    static final int MESSAGE_TIMEOUT = 2;
    static final int NOTIFICATION_PRIORITY_MULTIPLIER = 10;
    private static final int REASON_DELEGATE_CANCEL = 2;
    private static final int REASON_DELEGATE_CANCEL_ALL = 3;
    private static final int REASON_DELEGATE_CLICK = 1;
    private static final int REASON_DELEGATE_ERROR = 4;
    private static final int REASON_GROUP_OPTIMIZATION = 13;
    private static final int REASON_GROUP_SUMMARY_CANCELED = 12;
    private static final int REASON_LISTENER_CANCEL = 10;
    private static final int REASON_LISTENER_CANCEL_ALL = 11;
    private static final int REASON_NOMAN_CANCEL = 8;
    private static final int REASON_NOMAN_CANCEL_ALL = 9;
    private static final int REASON_PACKAGE_BANNED = 7;
    private static final int REASON_PACKAGE_CHANGED = 5;
    private static final int REASON_USER_STOPPED = 6;
    static final int SCORE_DISPLAY_THRESHOLD = -20;
    static final int SCORE_INTERRUPTION_THRESHOLD = -10;
    static final boolean SCORE_ONGOING_HIGHER = false;
    static final int SHORT_DELAY = 2000;
    private static final String TAG_BLOCKED_PKGS = "blocked-packages";
    private static final String TAG_BODY = "notification-policy";
    private static final String TAG_PACKAGE = "package";
    static final int VIBRATE_PATTERN_MAXLEN = 17;
    private IActivityManager mAm;
    private AppOpsManager mAppOps;
    private Archive mArchive;
    Light mAttentionLight;
    AudioManager mAudioManager;
    private HashSet<String> mBlockedPackages;
    private final Runnable mBuzzBeepBlinked;
    private int mCallState;
    private ConditionProviders mConditionProviders;
    private int mDefaultNotificationColor;
    private int mDefaultNotificationLedOff;
    private int mDefaultNotificationLedOn;
    private long[] mDefaultVibrationPattern;
    private boolean mDisableNotificationEffects;
    private ComponentName mEffectsSuppressor;
    private long[] mFallbackVibrationPattern;
    final IBinder mForegroundToken;
    private WorkerHandler mHandler;
    private boolean mInCall;
    private final BroadcastReceiver mIntentReceiver;
    private final NotificationManagerInternal mInternalService;
    private int mInterruptionFilter;
    ArrayList<String> mLights;
    private int mListenerHints;
    private NotificationListeners mListeners;
    private final ArraySet<ManagedServices.ManagedServiceInfo> mListenersDisablingEffects;
    private final NotificationDelegate mNotificationDelegate;
    private Light mNotificationLight;
    final ArrayList<NotificationRecord> mNotificationList;
    private boolean mNotificationPulseEnabled;
    final ArrayMap<String, NotificationRecord> mNotificationsByKey;
    private final BroadcastReceiver mPackageIntentReceiver;
    private AtomicFile mPolicyFile;
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
    private ZenModeHelper mZenModeHelper;
    static final String TAG = "NotificationService";
    static final boolean DBG = Log.isLoggable(TAG, 3);
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

        public void clear() {
            this.mBuffer.clear();
        }

        public Iterator<StatusBarNotification> descendingIterator() {
            return this.mBuffer.descendingIterator();
        }

        public Iterator<StatusBarNotification> ascendingIterator() {
            return this.mBuffer.iterator();
        }

        public Iterator<StatusBarNotification> filter(final Iterator<StatusBarNotification> iter, final String pkg, final int userId) {
            return new Iterator<StatusBarNotification>() {
                StatusBarNotification mNext = findNext();

                private StatusBarNotification findNext() {
                    while (iter.hasNext()) {
                        StatusBarNotification nr = (StatusBarNotification) iter.next();
                        if (pkg == null || nr.getPackageName() == pkg) {
                            if (userId == -1 || nr.getUserId() == userId) {
                                return nr;
                            }
                        }
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    if (this.mNext == null) {
                        return true;
                    }
                    return NotificationManagerService.SCORE_ONGOING_HIGHER;
                }

                @Override
                public StatusBarNotification next() {
                    StatusBarNotification next = this.mNext;
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    this.mNext = findNext();
                    return next;
                }

                @Override
                public void remove() {
                    iter.remove();
                }
            };
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

        public StatusBarNotification[] getArray(int count, String pkg, int userId) {
            if (count == 0) {
                count = this.mBufferSize;
            }
            StatusBarNotification[] a = new StatusBarNotification[Math.min(count, this.mBuffer.size())];
            Iterator<StatusBarNotification> iter = filter(descendingIterator(), pkg, userId);
            for (int i = 0; iter.hasNext() && i < count; i++) {
                a[i] = iter.next();
            }
            return a;
        }
    }

    private void loadPolicyFile() {
        synchronized (this.mPolicyFile) {
            this.mBlockedPackages.clear();
            FileInputStream infile = null;
            try {
                try {
                    try {
                        try {
                            infile = this.mPolicyFile.openRead();
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(infile, null);
                            while (true) {
                                int type = parser.next();
                                if (type == 1) {
                                    break;
                                }
                                String tag = parser.getName();
                                if (type == 2) {
                                    if (TAG_BODY.equals(tag)) {
                                        Integer.parseInt(parser.getAttributeValue(null, ATTR_VERSION));
                                    } else if (TAG_BLOCKED_PKGS.equals(tag)) {
                                        while (true) {
                                            int type2 = parser.next();
                                            if (type2 != 1) {
                                                String tag2 = parser.getName();
                                                if (TAG_PACKAGE.equals(tag2)) {
                                                    this.mBlockedPackages.add(parser.getAttributeValue(null, ATTR_NAME));
                                                } else if (!TAG_BLOCKED_PKGS.equals(tag2) || type2 != 3) {
                                                }
                                            }
                                        }
                                    }
                                }
                                this.mZenModeHelper.readXml(parser);
                                this.mRankingHelper.readXml(parser);
                            }
                            IoUtils.closeQuietly(infile);
                        } catch (IOException e) {
                            Log.wtf(TAG, "Unable to read notification policy", e);
                            IoUtils.closeQuietly(infile);
                        }
                    } catch (NumberFormatException e2) {
                        Log.wtf(TAG, "Unable to parse notification policy", e2);
                        IoUtils.closeQuietly(infile);
                    }
                } catch (FileNotFoundException e3) {
                    IoUtils.closeQuietly(infile);
                } catch (XmlPullParserException e4) {
                    Log.wtf(TAG, "Unable to parse notification policy", e4);
                    IoUtils.closeQuietly(infile);
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(infile);
                throw th;
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
                    XmlSerializer out = new FastXmlSerializer();
                    out.setOutput(stream, "utf-8");
                    out.startDocument(null, true);
                    out.startTag(null, TAG_BODY);
                    out.attribute(null, ATTR_VERSION, Integer.toString(1));
                    this.mZenModeHelper.writeXml(out);
                    this.mRankingHelper.writeXml(out);
                    out.endTag(null, TAG_BODY);
                    out.endDocument();
                    this.mPolicyFile.finishWrite(stream);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to save policy file, restoring backup", e);
                    this.mPolicyFile.failWrite(stream);
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to save policy file", e2);
            }
        }
    }

    private boolean noteNotificationOp(String pkg, int uid) {
        if (this.mAppOps.noteOpNoThrow(11, uid, pkg) == 0) {
            return true;
        }
        Slog.v(TAG, "notifications are disabled by AppOps for " + pkg);
        return SCORE_ONGOING_HIGHER;
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
            if (filter == null || filter.matches(this.pkg)) {
                pw.println(prefix + this);
            }
        }

        public final String toString() {
            return "ToastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " pkg=" + this.pkg + " callback=" + this.callback + " duration=" + this.duration;
        }
    }

    class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
        }

        void observe() {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, NotificationManagerService.SCORE_ONGOING_HIGHER, this, -1);
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            boolean pulseEnabled = NotificationManagerService.SCORE_ONGOING_HIGHER;
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                if (Settings.System.getInt(resolver, "notification_light_pulse", 0) != 0) {
                    pulseEnabled = true;
                }
                if (NotificationManagerService.this.mNotificationPulseEnabled != pulseEnabled) {
                    NotificationManagerService.this.mNotificationPulseEnabled = pulseEnabled;
                    NotificationManagerService.this.updateNotificationPulse();
                }
            }
        }
    }

    static long[] getLongArray(Resources r, int resid, int maxlen, long[] def) {
        int[] ar = r.getIntArray(resid);
        if (ar != null) {
            int len = ar.length > maxlen ? maxlen : ar.length;
            long[] out = new long[len];
            for (int i = 0; i < len; i++) {
                out[i] = ar[i];
            }
            return out;
        }
        return def;
    }

    public NotificationManagerService(Context context) {
        super(context);
        this.mForegroundToken = new Binder();
        this.mRankingThread = new HandlerThread("ranker", 10);
        this.mListenersDisablingEffects = new ArraySet<>();
        this.mScreenOn = true;
        this.mInCall = SCORE_ONGOING_HIGHER;
        this.mNotificationList = new ArrayList<>();
        this.mNotificationsByKey = new ArrayMap<>();
        this.mToastQueue = new ArrayList<>();
        this.mSummaryByGroupKey = new ArrayMap<>();
        this.mLights = new ArrayList<>();
        this.mBlockedPackages = new HashSet<>();
        this.mUserProfiles = new ManagedServices.UserProfiles();
        this.mNotificationDelegate = new NotificationDelegate() {
            @Override
            public void onSetDisabled(int status) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationManagerService.this.mDisableNotificationEffects = (262144 & status) != 0 ? true : NotificationManagerService.SCORE_ONGOING_HIGHER;
                    if (NotificationManagerService.this.disableNotificationEffects(null) != null) {
                        long identity = Binder.clearCallingIdentity();
                        try {
                            try {
                                IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                                if (player != null) {
                                    player.stopAsync();
                                }
                            } catch (RemoteException e) {
                                Binder.restoreCallingIdentity(identity);
                            }
                            identity = Binder.clearCallingIdentity();
                            try {
                                NotificationManagerService.this.mVibrator.cancel();
                            } finally {
                            }
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
                    EventLogTags.writeNotificationClicked(key);
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r == null) {
                        Log.w(NotificationManagerService.TAG, "No notification with key: " + key);
                    } else {
                        StatusBarNotification sbn = r.sbn;
                        NotificationManagerService.this.cancelNotification(callingUid, callingPid, sbn.getPackageName(), sbn.getTag(), sbn.getId(), 16, 64, NotificationManagerService.SCORE_ONGOING_HIGHER, r.getUserId(), 1, null);
                    }
                }
            }

            @Override
            public void onNotificationActionClick(int callingUid, int callingPid, String key, int actionIndex) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    EventLogTags.writeNotificationActionClicked(key, actionIndex);
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r == null) {
                        Log.w(NotificationManagerService.TAG, "No notification with key: " + key);
                    }
                }
            }

            @Override
            public void onNotificationClear(int callingUid, int callingPid, String pkg, String tag, int id, int userId) {
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 66, true, userId, 2, null);
            }

            @Override
            public void onPanelRevealed(boolean clearEffects) {
                EventLogTags.writeNotificationPanelRevealed();
                if (clearEffects) {
                    clearEffects();
                }
            }

            @Override
            public void onPanelHidden() {
                EventLogTags.writeNotificationPanelHidden();
            }

            @Override
            public void clearEffects() {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    if (NotificationManagerService.DBG) {
                        Slog.d(NotificationManagerService.TAG, "clearEffects");
                    }
                    NotificationManagerService.this.mSoundNotificationKey = null;
                    long identity = Binder.clearCallingIdentity();
                    try {
                        try {
                            IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                            if (player != null) {
                                player.stopAsync();
                            }
                        } finally {
                        }
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identity);
                    }
                    NotificationManagerService.this.mVibrateNotificationKey = null;
                    identity = Binder.clearCallingIdentity();
                    try {
                        NotificationManagerService.this.mVibrator.cancel();
                        Binder.restoreCallingIdentity(identity);
                        NotificationManagerService.this.mLights.clear();
                        NotificationManagerService.this.updateLightsLocked();
                    } finally {
                    }
                }
            }

            @Override
            public void onNotificationError(int callingUid, int callingPid, String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
                Slog.d(NotificationManagerService.TAG, "onNotification error pkg=" + pkg + " tag=" + tag + " id=" + id + "; will crashApplication(uid=" + uid + ", pid=" + initialPid + ")");
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 0, NotificationManagerService.SCORE_ONGOING_HIGHER, userId, 4, null);
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityManagerNative.getDefault().crashApplication(uid, initialPid, pkg, "Bad notification posted from package " + pkg + ": " + message);
                } catch (RemoteException e) {
                }
                Binder.restoreCallingIdentity(ident);
            }

            @Override
            public void onNotificationVisibilityChanged(String[] newlyVisibleKeys, String[] noLongerVisibleKeys) {
                EventLogTags.writeNotificationVisibilityChanged(TextUtils.join(";", newlyVisibleKeys), TextUtils.join(";", noLongerVisibleKeys));
                synchronized (NotificationManagerService.this.mNotificationList) {
                    for (String key : newlyVisibleKeys) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                        if (r != null) {
                            r.stats.onVisibilityChanged(true);
                        }
                    }
                    for (String key2 : noLongerVisibleKeys) {
                        NotificationRecord r2 = NotificationManagerService.this.mNotificationsByKey.get(key2);
                        if (r2 != null) {
                            r2.stats.onVisibilityChanged(NotificationManagerService.SCORE_ONGOING_HIGHER);
                        }
                    }
                }
            }

            @Override
            public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) {
                EventLogTags.writeNotificationExpansion(key, userAction ? 1 : 0, expanded ? 1 : 0);
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationRecord r = NotificationManagerService.this.mNotificationsByKey.get(key);
                    if (r != null) {
                        r.stats.onExpansionChanged(userAction, expanded);
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
                if (action != null) {
                    boolean queryRestart = NotificationManagerService.SCORE_ONGOING_HIGHER;
                    boolean queryRemove = NotificationManagerService.SCORE_ONGOING_HIGHER;
                    boolean packageChanged = NotificationManagerService.SCORE_ONGOING_HIGHER;
                    boolean cancelNotifications = true;
                    if (action.equals("android.intent.action.PACKAGE_ADDED") || (queryRemove = action.equals("android.intent.action.PACKAGE_REMOVED")) || action.equals("android.intent.action.PACKAGE_RESTARTED") || (packageChanged = action.equals("android.intent.action.PACKAGE_CHANGED")) || (queryRestart = action.equals("android.intent.action.QUERY_PACKAGE_RESTART")) || action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                        int changeUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        boolean queryReplace = (queryRemove && intent.getBooleanExtra("android.intent.extra.REPLACING", NotificationManagerService.SCORE_ONGOING_HIGHER)) ? true : NotificationManagerService.SCORE_ONGOING_HIGHER;
                        if (NotificationManagerService.DBG) {
                            Slog.i(NotificationManagerService.TAG, "action=" + action + " queryReplace=" + queryReplace);
                        }
                        if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        } else if (queryRestart) {
                            pkgList = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                        } else {
                            Uri uri = intent.getData();
                            if (uri != null && (pkgName = uri.getSchemeSpecificPart()) != null) {
                                if (packageChanged) {
                                    try {
                                        IPackageManager pm = AppGlobals.getPackageManager();
                                        int enabled = pm.getApplicationEnabledSetting(pkgName, changeUserId != -1 ? changeUserId : 0);
                                        if (enabled == 1 || enabled == 0) {
                                            cancelNotifications = NotificationManagerService.SCORE_ONGOING_HIGHER;
                                        }
                                    } catch (RemoteException e) {
                                    } catch (IllegalArgumentException e2) {
                                        if (NotificationManagerService.DBG) {
                                            Slog.i(NotificationManagerService.TAG, "Exception trying to look up app enabled setting", e2);
                                        }
                                    }
                                }
                                pkgList = new String[]{pkgName};
                            } else {
                                return;
                            }
                        }
                        if (pkgList != null && pkgList.length > 0) {
                            String[] arr$ = pkgList;
                            for (String pkgName2 : arr$) {
                                if (cancelNotifications) {
                                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkgName2, 0, 0, !queryRestart ? true : NotificationManagerService.SCORE_ONGOING_HIGHER, changeUserId, 5, null);
                                }
                            }
                        }
                        NotificationManagerService.this.mListeners.onPackagesChanged(queryReplace, pkgList);
                        NotificationManagerService.this.mConditionProviders.onPackagesChanged(queryReplace, pkgList);
                    }
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
                    NotificationManagerService.this.mScreenOn = NotificationManagerService.SCORE_ONGOING_HIGHER;
                    NotificationManagerService.this.updateNotificationPulse();
                    return;
                }
                if (action.equals("android.intent.action.PHONE_STATE")) {
                    NotificationManagerService.this.mInCall = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra("state"));
                    NotificationManagerService.this.updateNotificationPulse();
                    return;
                }
                if (action.equals("android.intent.action.USER_STOPPED")) {
                    int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle >= 0) {
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, 0, 0, true, userHandle, 6, null);
                        return;
                    }
                    return;
                }
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                    NotificationManagerService.this.mStatusBar.notificationLightOff();
                } else {
                    if (action.equals("android.intent.action.USER_SWITCHED")) {
                        NotificationManagerService.this.mSettingsObserver.update(null);
                        NotificationManagerService.this.mUserProfiles.updateCache(context2);
                        NotificationManagerService.this.mConditionProviders.onUserSwitched();
                        NotificationManagerService.this.mListeners.onUserSwitched();
                        return;
                    }
                    if (action.equals("android.intent.action.USER_ADDED")) {
                        NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    }
                }
            }
        };
        this.mBuzzBeepBlinked = new Runnable() {
            @Override
            public void run() {
                NotificationManagerService.this.mStatusBar.buzzBeepBlinked();
            }
        };
        this.mService = new INotificationManager.Stub() {
            public void enqueueToast(String pkg, ITransientNotification callback, int duration) {
                if (NotificationManagerService.DBG) {
                    Slog.i(NotificationManagerService.TAG, "enqueueToast pkg=" + pkg + " callback=" + callback + " duration=" + duration);
                }
                if (pkg == null || callback == null) {
                    Slog.e(NotificationManagerService.TAG, "Not doing toast. pkg=" + pkg + " callback=" + callback);
                    return;
                }
                boolean isSystemToast = (NotificationManagerService.isCallerSystem() || "android".equals(pkg)) ? true : NotificationManagerService.SCORE_ONGOING_HIGHER;
                if (!NotificationManagerService.this.noteNotificationOp(pkg, Binder.getCallingUid()) && !isSystemToast) {
                    Slog.e(NotificationManagerService.TAG, "Suppressing toast from package " + pkg + " by user request.");
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
                            if (!isSystemToast) {
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
                NotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), pkg, tag, id, 0, Binder.getCallingUid() == 1000 ? 0 : 64, NotificationManagerService.SCORE_ONGOING_HIGHER, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, NotificationManagerService.SCORE_ONGOING_HIGHER, "cancelNotificationWithTag", pkg), 8, null);
            }

            public void cancelAllNotifications(String pkg, int userId) {
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                NotificationManagerService.this.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), pkg, 0, 64, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, NotificationManagerService.SCORE_ONGOING_HIGHER, "cancelAllNotifications", pkg), 9, null);
            }

            public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.setNotificationsEnabledForPackageImpl(pkg, uid, enabled);
            }

            public boolean areNotificationsEnabledForPackage(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystem();
                if (NotificationManagerService.this.mAppOps.checkOpNoThrow(11, uid, pkg) == 0) {
                    return true;
                }
                return NotificationManagerService.SCORE_ONGOING_HIGHER;
            }

            public void setPackagePriority(String pkg, int uid, int priority) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.mRankingHelper.setPackagePriority(pkg, uid, priority);
                NotificationManagerService.this.savePolicyFile();
            }

            public int getPackagePriority(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getPackagePriority(pkg, uid);
            }

            public void setPackageVisibilityOverride(String pkg, int uid, int visibility) {
                NotificationManagerService.checkCallerIsSystem();
                NotificationManagerService.this.mRankingHelper.setPackageVisibilityOverride(pkg, uid, visibility);
                NotificationManagerService.this.savePolicyFile();
            }

            public int getPackageVisibilityOverride(String pkg, int uid) {
                NotificationManagerService.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getPackageVisibilityOverride(pkg, uid);
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

            public void unregisterListener(INotificationListener listener, int userid) {
                NotificationManagerService.this.mListeners.unregisterService((IInterface) listener, userid);
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
                synchronized (NotificationManagerService.this.mNotificationList) {
                    ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    boolean getKeys = keys != null ? true : NotificationManagerService.SCORE_ONGOING_HIGHER;
                    int N = getKeys ? keys.length : NotificationManagerService.this.mNotificationList.size();
                    ArrayList<StatusBarNotification> list = new ArrayList<>(N);
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = getKeys ? NotificationManagerService.this.mNotificationsByKey.get(keys[i]) : NotificationManagerService.this.mNotificationList.get(i);
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
                        boolean disableEffects = (hints & 1) != 0 ? true : NotificationManagerService.SCORE_ONGOING_HIGHER;
                        if (disableEffects) {
                            NotificationManagerService.this.mListenersDisablingEffects.add(info);
                        } else {
                            NotificationManagerService.this.mListenersDisablingEffects.remove(info);
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
                    if (info != null) {
                        NotificationManagerService.this.mListeners.setOnNotificationPostedTrimLocked(info, trim);
                    }
                }
            }

            public ZenModeConfig getZenModeConfig() {
                enforceSystemOrSystemUI("INotificationManager.getZenModeConfig");
                return NotificationManagerService.this.mZenModeHelper.getConfig();
            }

            public boolean setZenModeConfig(ZenModeConfig config) {
                NotificationManagerService.checkCallerIsSystem();
                return NotificationManagerService.this.mZenModeHelper.setConfig(config);
            }

            public void notifyConditions(String pkg, IConditionProvider provider, Condition[] conditions) {
                ManagedServices.ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
                NotificationManagerService.checkCallerIsSystemOrSameApp(pkg);
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mConditionProviders.notifyConditions(pkg, info, conditions);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void requestZenModeConditions(IConditionListener callback, int relevance) {
                enforceSystemOrSystemUI("INotificationManager.requestZenModeConditions");
                NotificationManagerService.this.mConditionProviders.requestZenModeConditions(callback, relevance);
            }

            public void setZenModeCondition(Condition condition) {
                enforceSystemOrSystemUI("INotificationManager.setZenModeCondition");
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mConditionProviders.setZenModeCondition(condition, "binderCall");
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            public void setAutomaticZenModeConditions(Uri[] conditionIds) {
                enforceSystemOrSystemUI("INotificationManager.setAutomaticZenModeConditions");
                NotificationManagerService.this.mConditionProviders.setAutomaticZenModeConditions(conditionIds);
            }

            public Condition[] getAutomaticZenModeConditions() {
                enforceSystemOrSystemUI("INotificationManager.getAutomaticZenModeConditions");
                return NotificationManagerService.this.mConditionProviders.getAutomaticZenModeConditions();
            }

            private void enforceSystemOrSystemUI(String message) {
                if (!NotificationManagerService.isCallerSystem()) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
                }
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (NotificationManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump NotificationManager from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                } else {
                    NotificationManagerService.this.dumpImpl(pw, DumpFilter.parseFromArguments(args));
                }
            }

            public ComponentName getEffectsSuppressor() {
                enforceSystemOrSystemUI("INotificationManager.getEffectsSuppressor");
                return NotificationManagerService.this.mEffectsSuppressor;
            }

            public boolean matchesCallFilter(Bundle extras) {
                enforceSystemOrSystemUI("INotificationManager.matchesCallFilter");
                return NotificationManagerService.this.mZenModeHelper.matchesCallFilter(UserHandle.getCallingUserHandle(), extras, (ValidateNotificationPeople) NotificationManagerService.this.mRankingHelper.findExtractor(ValidateNotificationPeople.class), NotificationManagerService.MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS, NotificationManagerService.MATCHES_CALL_FILTER_TIMEOUT_AFFINITY);
            }

            public boolean isSystemConditionProviderEnabled(String path) {
                enforceSystemOrSystemUI("INotificationManager.isSystemConditionProviderEnabled");
                return NotificationManagerService.this.mConditionProviders.isSystemConditionProviderEnabled(path);
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

    @Override
    public void onStart() {
        String[] extractorNames;
        Resources resources = getContext().getResources();
        this.mAm = ActivityManagerNative.getDefault();
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        this.mHandler = new WorkerHandler();
        this.mRankingThread.start();
        try {
            extractorNames = resources.getStringArray(R.array.config_cell_retries_per_error_code);
        } catch (Resources.NotFoundException e) {
            extractorNames = new String[0];
        }
        this.mRankingHelper = new RankingHelper(getContext(), new RankingWorkerHandler(this.mRankingThread.getLooper()), extractorNames);
        this.mZenModeHelper = new ZenModeHelper(getContext(), this.mHandler.getLooper());
        this.mZenModeHelper.addCallback(new ZenModeHelper.Callback() {
            @Override
            public void onConfigChanged() {
                NotificationManagerService.this.savePolicyFile();
            }

            @Override
            void onZenModeChanged() {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
            }
        });
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mPolicyFile = new AtomicFile(new File(systemDir, "notification_policy.xml"));
        this.mUsageStats = new NotificationUsageStats(getContext());
        importOldBlockDb();
        this.mListeners = new NotificationListeners();
        this.mConditionProviders = new ConditionProviders(getContext(), this.mHandler, this.mUserProfiles, this.mZenModeHelper);
        this.mStatusBar = (StatusBarManagerInternal) getLocalService(StatusBarManagerInternal.class);
        this.mStatusBar.setNotificationDelegate(this.mNotificationDelegate);
        LightsManager lights = (LightsManager) getLocalService(LightsManager.class);
        this.mNotificationLight = lights.getLight(4);
        this.mAttentionLight = lights.getLight(5);
        this.mDefaultNotificationColor = resources.getColor(R.color.bright_foreground_dark);
        this.mDefaultNotificationLedOn = resources.getInteger(R.integer.config_criticalBatteryWarningLevel);
        this.mDefaultNotificationLedOff = resources.getInteger(R.integer.config_cursorWindowSize);
        this.mDefaultVibrationPattern = getLongArray(resources, R.array.config_cdma_dun_supported_types, 17, DEFAULT_VIBRATE_PATTERN);
        this.mFallbackVibrationPattern = getLongArray(resources, R.array.config_cdma_home_system, 17, DEFAULT_VIBRATE_PATTERN);
        this.mUseAttentionLight = resources.getBoolean(R.^attr-private.calendarViewMode);
        this.mZenModeHelper.readZenModeFromSetting();
        this.mInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        this.mUserProfiles.updateCache(getContext());
        listenForCallState();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        getContext().registerReceiver(this.mIntentReceiver, filter);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        pkgFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        pkgFilter.addDataScheme(TAG_PACKAGE);
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, pkgFilter, null, null);
        IntentFilter sdFilter = new IntentFilter("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, sdFilter, null, null);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mArchive = new Archive(resources.getInteger(R.integer.config_datause_threshold_bytes));
        publishBinderService("notification", this.mService);
        publishLocalService(NotificationManagerInternal.class, this.mInternalService);
    }

    private void importOldBlockDb() {
        loadPolicyFile();
        PackageManager pm = getContext().getPackageManager();
        for (String pkg : this.mBlockedPackages) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0);
                setNotificationsEnabledForPackageImpl(pkg, info.applicationInfo.uid, SCORE_ONGOING_HIGHER);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        this.mBlockedPackages.clear();
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mSystemReady = true;
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            this.mZenModeHelper.onSystemReady();
        } else if (phase == 600) {
            this.mSettingsObserver.observe();
            this.mListeners.onBootPhaseAppsCanStart();
            this.mConditionProviders.onBootPhaseAppsCanStart();
        }
    }

    void setNotificationsEnabledForPackageImpl(String pkg, int uid, boolean enabled) {
        Slog.v(TAG, (enabled ? "en" : "dis") + "abling notifications for " + pkg);
        this.mAppOps.setMode(11, uid, pkg, enabled ? 0 : 1);
        if (!enabled) {
            cancelAllNotificationsInt(MY_UID, MY_PID, pkg, 0, 0, true, UserHandle.getUserId(uid), 7, null);
        }
    }

    private void updateListenerHintsLocked() {
        int hints = this.mListenersDisablingEffects.isEmpty() ? 0 : 1;
        if (hints != this.mListenerHints) {
            this.mListenerHints = hints;
            scheduleListenerHintsChanged(hints);
        }
    }

    private void updateEffectsSuppressorLocked() {
        ComponentName suppressor = !this.mListenersDisablingEffects.isEmpty() ? this.mListenersDisablingEffects.valueAt(0).component : null;
        if (!Objects.equals(suppressor, this.mEffectsSuppressor)) {
            this.mEffectsSuppressor = suppressor;
            this.mZenModeHelper.setEffectsSuppressed(suppressor != null);
            getContext().sendBroadcast(new Intent("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED").addFlags(1073741824));
        }
    }

    private void updateInterruptionFilterLocked() {
        int interruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        if (interruptionFilter != this.mInterruptionFilter) {
            this.mInterruptionFilter = interruptionFilter;
            scheduleInterruptionFilterChanged(interruptionFilter);
        }
    }

    private String[] getActiveNotificationKeys(INotificationListener token) {
        ManagedServices.ManagedServiceInfo info = this.mListeners.checkServiceTokenLocked(token);
        ArrayList<String> keys = new ArrayList<>();
        if (info.isEnabledForCurrentProfiles()) {
            synchronized (this.mNotificationList) {
                int N = this.mNotificationList.size();
                for (int i = 0; i < N; i++) {
                    StatusBarNotification sbn = this.mNotificationList.get(i).sbn;
                    if (info.enabledAndUserMatches(sbn.getUserId())) {
                        keys.add(sbn.getKey());
                    }
                }
            }
        }
        return (String[]) keys.toArray(new String[keys.size()]);
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

    void dumpImpl(PrintWriter pw, DumpFilter filter) {
        pw.print("Current Notification Manager state");
        if (filter != null) {
            pw.print(" (filtered to ");
            pw.print(filter);
            pw.print(")");
        }
        pw.println(':');
        boolean zenOnly = (filter == null || !filter.zen) ? SCORE_ONGOING_HIGHER : true;
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
                        if (filter == null || filter.matches(nr.sbn)) {
                            nr.dump(pw, "    ", getContext());
                        }
                    }
                    pw.println("  ");
                }
                if (filter == null) {
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
                if (filter != null || zenOnly) {
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
                        ManagedServices.ManagedServiceInfo listener = this.mListenersDisablingEffects.valueAt(i5);
                        if (i5 > 0) {
                            pw.print(',');
                        }
                        pw.print(listener.component);
                    }
                    pw.println(')');
                }
                pw.println("\n  Condition providers:");
                this.mConditionProviders.dump(pw, filter);
                pw.println("\n  Group summaries:");
                for (Map.Entry<String, NotificationRecord> entry : this.mSummaryByGroupKey.entrySet()) {
                    NotificationRecord r = entry.getValue();
                    pw.println("    " + entry.getKey() + " -> " + r.getKey());
                    if (this.mNotificationsByKey.get(r.getKey()) != r) {
                        pw.println("!!!!!!LEAK: Record not found in mNotificationsByKey.");
                        r.dump(pw, "      ", getContext());
                    }
                }
            } else {
                if (!zenOnly) {
                }
                if (filter != null) {
                    pw.println("\n  Zen Mode:");
                    pw.print("    mInterruptionFilter=");
                    pw.println(this.mInterruptionFilter);
                    this.mZenModeHelper.dump(pw, "    ");
                    pw.println("\n  Zen Log:");
                    ZenLog.dump(pw, "    ");
                    if (!zenOnly) {
                    }
                    pw.println("\n  Condition providers:");
                    this.mConditionProviders.dump(pw, filter);
                    pw.println("\n  Group summaries:");
                    while (r3.hasNext()) {
                    }
                }
            }
        }
    }

    void enqueueNotificationInternal(final String pkg, final String opPkg, final int callingUid, final int callingPid, final String tag, final int id, final Notification notification, int[] idOut, int incomingUserId) {
        if (DBG) {
            Slog.v(TAG, "enqueueNotificationInternal: pkg=" + pkg + " id=" + id + " notification=" + notification);
        }
        checkCallerIsSystemOrSameApp(pkg);
        final boolean isSystemNotification = (isUidSystem(callingUid) || "android".equals(pkg)) ? true : SCORE_ONGOING_HIGHER;
        boolean isNotificationFromListener = this.mListeners.isListenerPackage(pkg);
        final int userId = ActivityManager.handleIncomingUser(callingPid, callingUid, incomingUserId, true, SCORE_ONGOING_HIGHER, "enqueueNotification", pkg);
        final UserHandle user = new UserHandle(userId);
        if (!isSystemNotification && !isNotificationFromListener) {
            synchronized (this.mNotificationList) {
                int index = indexOfNotificationLocked(pkg, tag, id, userId);
                if (index < 0) {
                    int count = 0;
                    int N = this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord r = this.mNotificationList.get(i);
                        if (r.sbn.getPackageName().equals(pkg) && r.sbn.getUserId() == userId) {
                            count++;
                            if (count >= 50) {
                                Slog.e(TAG, "Package has already posted " + count + " notifications.  Not showing more.  package=" + pkg);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (pkg == null || notification == null) {
            throw new IllegalArgumentException("null not allowed: pkg=" + pkg + " id=" + id + " notification=" + notification);
        }
        if (notification.icon != 0 && !notification.isValid()) {
            throw new IllegalArgumentException("Invalid notification (): pkg=" + pkg + " id=" + id + " notification=" + notification);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    notification.priority = NotificationManagerService.clamp(notification.priority, -2, 2);
                    if ((notification.flags & 128) != 0 && notification.priority < 2) {
                        notification.priority = 2;
                    }
                    int score = notification.priority * 10;
                    StatusBarNotification n = new StatusBarNotification(pkg, opPkg, id, tag, callingUid, callingPid, score, notification, user);
                    NotificationRecord r2 = new NotificationRecord(n, score);
                    NotificationRecord old = NotificationManagerService.this.mNotificationsByKey.get(n.getKey());
                    if (old != null) {
                        r2.copyRankingInformation(old);
                    }
                    NotificationManagerService.this.handleGroupedNotificationLocked(r2, old, callingUid, callingPid);
                    boolean ignoreNotification = NotificationManagerService.this.removeUnusedGroupedNotificationLocked(r2, old, callingUid, callingPid);
                    if (!pkg.equals("com.android.providers.downloads") || Log.isLoggable("DownloadManager", 2)) {
                        int enqueueStatus = 0;
                        if (ignoreNotification) {
                            enqueueStatus = 2;
                        } else if (old != null) {
                            enqueueStatus = 1;
                        }
                        EventLogTags.writeNotificationEnqueue(callingUid, callingPid, pkg, id, tag, userId, notification.toString(), enqueueStatus);
                    }
                    if (!ignoreNotification) {
                        NotificationManagerService.this.mRankingHelper.extractSignals(r2);
                        if (!NotificationManagerService.this.noteNotificationOp(pkg, callingUid) && !isSystemNotification) {
                            r2.score = NotificationManagerService.JUNK_SCORE;
                            Slog.e(NotificationManagerService.TAG, "Suppressing notification from package " + pkg + " by user request.");
                        }
                        if (r2.score >= NotificationManagerService.SCORE_DISPLAY_THRESHOLD) {
                            int index2 = NotificationManagerService.this.indexOfNotificationLocked(n.getKey());
                            if (index2 < 0) {
                                NotificationManagerService.this.mNotificationList.add(r2);
                                NotificationManagerService.this.mUsageStats.registerPostedByApp(r2);
                            } else {
                                old = NotificationManagerService.this.mNotificationList.get(index2);
                                NotificationManagerService.this.mNotificationList.set(index2, r2);
                                NotificationManagerService.this.mUsageStats.registerUpdatedByApp(r2, old);
                                notification.flags |= old.getNotification().flags & 64;
                                r2.isUpdate = true;
                            }
                            NotificationManagerService.this.mNotificationsByKey.put(n.getKey(), r2);
                            if ((notification.flags & 64) != 0) {
                                notification.flags |= 34;
                            }
                            NotificationManagerService.this.applyZenModeLocked(r2);
                            NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                            if (notification.icon != 0) {
                                StatusBarNotification oldSbn = old != null ? old.sbn : null;
                                NotificationManagerService.this.mListeners.notifyPostedLocked(n, oldSbn);
                            } else {
                                Slog.e(NotificationManagerService.TAG, "Not posting notification with icon==0: " + notification);
                                if (old != null && !old.isCanceled) {
                                    NotificationManagerService.this.mListeners.notifyRemovedLocked(n);
                                }
                                Slog.e(NotificationManagerService.TAG, "WARNING: In a future release this will crash the app: " + n.getPackageName());
                            }
                            NotificationManagerService.this.buzzBeepBlinkLocked(r2);
                        }
                    }
                }
            }
        });
        idOut[0] = id;
    }

    private void handleGroupedNotificationLocked(NotificationRecord r, NotificationRecord old, int callingUid, int callingPid) {
        NotificationRecord removedSummary;
        StatusBarNotification sbn = r.sbn;
        Notification n = sbn.getNotification();
        String group = sbn.getGroupKey();
        boolean isSummary = n.isGroupSummary();
        Notification oldN = old != null ? old.sbn.getNotification() : null;
        String oldGroup = old != null ? old.sbn.getGroupKey() : null;
        boolean oldIsSummary = (old == null || !oldN.isGroupSummary()) ? SCORE_ONGOING_HIGHER : true;
        if (oldIsSummary && (removedSummary = this.mSummaryByGroupKey.remove(oldGroup)) != old) {
            String removedKey = removedSummary != null ? removedSummary.getKey() : "<null>";
            Slog.w(TAG, "Removed summary didn't match old notification: old=" + old.getKey() + ", removed=" + removedKey);
        }
        if (isSummary) {
            this.mSummaryByGroupKey.put(group, r);
        }
        if (oldIsSummary) {
            if (!isSummary || !oldGroup.equals(group)) {
                cancelGroupChildrenLocked(old, callingUid, callingPid, null, 12);
            }
        }
    }

    private boolean removeUnusedGroupedNotificationLocked(NotificationRecord r, NotificationRecord old, int callingUid, int callingPid) {
        if (this.mListeners.notificationGroupsDesired()) {
            return SCORE_ONGOING_HIGHER;
        }
        StatusBarNotification sbn = r.sbn;
        String group = sbn.getGroupKey();
        boolean isSummary = sbn.getNotification().isGroupSummary();
        boolean isChild = sbn.getNotification().isGroupChild();
        NotificationRecord summary = this.mSummaryByGroupKey.get(group);
        if (isChild && summary != null) {
            if (DBG) {
                Slog.d(TAG, "Ignoring group child " + sbn.getKey() + " due to existing summary " + summary.getKey());
            }
            if (old != null) {
                if (DBG) {
                    Slog.d(TAG, "Canceling old version of ignored group child " + sbn.getKey());
                }
                cancelNotificationLocked(old, SCORE_ONGOING_HIGHER, 13);
            }
            return true;
        }
        if (isSummary) {
            cancelGroupChildrenLocked(r, callingUid, callingPid, null, 13);
        }
        return SCORE_ONGOING_HIGHER;
    }

    private void buzzBeepBlinkLocked(NotificationRecord record) {
        boolean buzzBeepBlinked = SCORE_ONGOING_HIGHER;
        Notification notification = record.sbn.getNotification();
        boolean aboveThreshold = record.score >= SCORE_INTERRUPTION_THRESHOLD ? true : SCORE_ONGOING_HIGHER;
        boolean canInterrupt = (!aboveThreshold || record.isIntercepted()) ? SCORE_ONGOING_HIGHER : true;
        if (DBG || record.isIntercepted()) {
            Slog.v(TAG, "pkg=" + record.sbn.getPackageName() + " canInterrupt=" + canInterrupt + " intercept=" + record.isIntercepted());
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            Binder.restoreCallingIdentity(identity);
            String disableEffects = disableNotificationEffects(record);
            if (disableEffects != null) {
                ZenLog.traceDisableEffects(record, disableEffects);
            }
            if (disableEffects == null && ((!record.isUpdate || (notification.flags & 8) == 0) && ((record.getUserId() == -1 || record.getUserId() == currentUser || this.mUserProfiles.isCurrentProfile(record.getUserId())) && canInterrupt && this.mSystemReady && this.mAudioManager != null))) {
                if (DBG) {
                    Slog.v(TAG, "Interrupting!");
                }
                sendAccessibilityEvent(notification, record.sbn.getPackageName());
                boolean useDefaultSound = ((notification.defaults & 1) != 0 || Settings.System.DEFAULT_NOTIFICATION_URI.equals(notification.sound)) ? true : SCORE_ONGOING_HIGHER;
                Uri soundUri = null;
                boolean hasValidSound = SCORE_ONGOING_HIGHER;
                if (useDefaultSound) {
                    soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                    ContentResolver resolver = getContext().getContentResolver();
                    hasValidSound = Settings.System.getString(resolver, "notification_sound") != null ? true : SCORE_ONGOING_HIGHER;
                } else if (notification.sound != null) {
                    soundUri = notification.sound;
                    hasValidSound = soundUri != null ? true : SCORE_ONGOING_HIGHER;
                }
                if (hasValidSound) {
                    boolean looping = (notification.flags & 4) != 0 ? true : SCORE_ONGOING_HIGHER;
                    AudioAttributes audioAttributes = audioAttributesForNotification(notification);
                    this.mSoundNotificationKey = record.getKey();
                    if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(audioAttributes)) != 0 && !this.mAudioManager.isAudioFocusExclusive()) {
                        identity = Binder.clearCallingIdentity();
                        try {
                            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                            if (player != null) {
                                if (DBG) {
                                    Slog.v(TAG, "Playing sound " + soundUri + " with attributes " + audioAttributes);
                                }
                                player.playAsync(soundUri, record.sbn.getUser(), looping, audioAttributes);
                                buzzBeepBlinked = true;
                            }
                        } catch (RemoteException e) {
                        } finally {
                        }
                    }
                }
                boolean hasCustomVibrate = notification.vibrate != null ? true : SCORE_ONGOING_HIGHER;
                boolean convertSoundToVibration = (!hasCustomVibrate && hasValidSound && this.mAudioManager.getRingerModeInternal() == 1) ? true : SCORE_ONGOING_HIGHER;
                boolean useDefaultVibrate = (notification.defaults & 2) != 0 ? true : SCORE_ONGOING_HIGHER;
                if ((useDefaultVibrate || convertSoundToVibration || hasCustomVibrate) && this.mAudioManager.getRingerModeInternal() != 0) {
                    this.mVibrateNotificationKey = record.getKey();
                    if (useDefaultVibrate || convertSoundToVibration) {
                        identity = Binder.clearCallingIdentity();
                        try {
                            this.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), useDefaultVibrate ? this.mDefaultVibrationPattern : this.mFallbackVibrationPattern, (notification.flags & 4) != 0 ? 0 : -1, audioAttributesForNotification(notification));
                            buzzBeepBlinked = true;
                        } finally {
                        }
                    } else if (notification.vibrate.length > 1) {
                        this.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), notification.vibrate, (notification.flags & 4) != 0 ? 0 : -1, audioAttributesForNotification(notification));
                        buzzBeepBlinked = true;
                    }
                }
            }
            boolean wasShowLights = this.mLights.remove(record.getKey());
            if ((notification.flags & 1) != 0 && aboveThreshold) {
                this.mLights.add(record.getKey());
                updateLightsLocked();
                if (this.mUseAttentionLight) {
                    this.mAttentionLight.pulse();
                }
                buzzBeepBlinked = true;
            } else if (wasShowLights) {
                updateLightsLocked();
            }
            if (buzzBeepBlinked) {
                this.mHandler.post(this.mBuzzBeepBlinked);
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
            if (DBG) {
                Slog.d(TAG, "Show pkg=" + record.pkg + " callback=" + record.callback);
            }
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
                    ToastRecord record2 = this.mToastQueue.get(0);
                    record = record2;
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
        if (this.mToastQueue.size() > 0) {
            showNextToastLocked();
        }
    }

    private void scheduleTimeoutLocked(ToastRecord r) {
        this.mHandler.removeCallbacksAndMessages(r);
        Message m = Message.obtain(this.mHandler, 2, r);
        long delay = r.duration == 1 ? 3500L : 2000L;
        this.mHandler.sendMessageDelayed(m, delay);
    }

    private void handleTimeout(ToastRecord record) {
        if (DBG) {
            Slog.d(TAG, "Timeout pkg=" + record.pkg + " callback=" + record.callback);
        }
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
            this.mAm.setProcessForeground(this.mForegroundToken, pid, toastCount > 0 ? true : SCORE_ONGOING_HIGHER);
        } catch (RemoteException e) {
        }
    }

    private void handleRankingReconsideration(Message message) {
        if (message.obj instanceof RankingReconsideration) {
            RankingReconsideration recon = (RankingReconsideration) message.obj;
            recon.run();
            synchronized (this.mNotificationList) {
                NotificationRecord record = this.mNotificationsByKey.get(recon.getKey());
                if (record != null) {
                    int indexBefore = findNotificationRecordIndexLocked(record);
                    boolean interceptBefore = record.isIntercepted();
                    int visibilityBefore = record.getPackageVisibilityOverride();
                    recon.applyChangesLocked(record);
                    applyZenModeLocked(record);
                    this.mRankingHelper.sort(this.mNotificationList);
                    int indexAfter = findNotificationRecordIndexLocked(record);
                    boolean interceptAfter = record.isIntercepted();
                    int visibilityAfter = record.getPackageVisibilityOverride();
                    boolean changed = (indexBefore == indexAfter && interceptBefore == interceptAfter && visibilityBefore == visibilityAfter) ? SCORE_ONGOING_HIGHER : true;
                    if (interceptBefore && !interceptAfter) {
                        buzzBeepBlinkLocked(record);
                    }
                    if (changed) {
                        scheduleSendRankingUpdate();
                    }
                }
            }
        }
    }

    private void handleRankingConfigChange() {
        synchronized (this.mNotificationList) {
            int N = this.mNotificationList.size();
            ArrayList<String> orderBefore = new ArrayList<>(N);
            int[] visibilities = new int[N];
            for (int i = 0; i < N; i++) {
                NotificationRecord r = this.mNotificationList.get(i);
                orderBefore.add(r.getKey());
                visibilities[i] = r.getPackageVisibilityOverride();
                this.mRankingHelper.extractSignals(r);
            }
            for (int i2 = 0; i2 < N; i2++) {
                this.mRankingHelper.sort(this.mNotificationList);
                NotificationRecord r2 = this.mNotificationList.get(i2);
                if (!orderBefore.get(i2).equals(r2.getKey()) || visibilities[i2] != r2.getPackageVisibilityOverride()) {
                    scheduleSendRankingUpdate();
                    return;
                }
            }
        }
    }

    private void applyZenModeLocked(NotificationRecord record) {
        record.setIntercepted(this.mZenModeHelper.shouldIntercept(record));
    }

    private int findNotificationRecordIndexLocked(NotificationRecord target) {
        return this.mRankingHelper.indexOf(this.mNotificationList, target);
    }

    private void scheduleSendRankingUpdate() {
        this.mHandler.removeMessages(6);
        Message m = Message.obtain(this.mHandler, 6);
        this.mHandler.sendMessage(m);
    }

    private void handleSendRankingUpdate() {
        synchronized (this.mNotificationList) {
            this.mListeners.notifyRankingUpdateLocked();
        }
    }

    private void scheduleListenerHintsChanged(int state) {
        this.mHandler.removeMessages(7);
        this.mHandler.obtainMessage(7, state, 0).sendToTarget();
    }

    private void scheduleInterruptionFilterChanged(int listenerInterruptionFilter) {
        this.mHandler.removeMessages(8);
        this.mHandler.obtainMessage(8, listenerInterruptionFilter, 0).sendToTarget();
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
                case 6:
                    NotificationManagerService.this.handleSendRankingUpdate();
                    break;
                case 7:
                    NotificationManagerService.this.handleListenerHintsChanged(msg.arg1);
                    break;
                case 8:
                    NotificationManagerService.this.handleListenerInterruptionFilterChanged(msg.arg1);
                    break;
            }
        }
    }

    private final class RankingWorkerHandler extends Handler {
        public RankingWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    NotificationManagerService.this.handleRankingReconsideration(msg);
                    break;
                case 5:
                    NotificationManagerService.this.handleRankingConfigChange();
                    break;
            }
        }
    }

    static int clamp(int x, int low, int high) {
        return x < low ? low : x > high ? high : x;
    }

    void sendAccessibilityEvent(Notification notification, CharSequence packageName) {
        AccessibilityManager manager = AccessibilityManager.getInstance(getContext());
        if (manager.isEnabled()) {
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
        if (r.getNotification().icon != 0) {
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
            case 1:
                this.mUsageStats.registerCancelDueToClick(r);
                break;
            case 2:
            case 3:
            case 10:
            case 11:
                this.mUsageStats.registerDismissedByUser(r);
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            default:
                this.mUsageStats.registerCancelUnknown(r);
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
        this.mArchive.record(r.sbn);
        EventLogTags.writeNotificationCanceled(canceledKey, reason);
    }

    void cancelNotification(final int callingUid, final int callingPid, final String pkg, final String tag, final int id, final int mustHaveFlags, final int mustNotHaveFlags, final boolean sendDelete, final int userId, final int reason, final ManagedServices.ManagedServiceInfo listener) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                String listenerName = listener == null ? null : listener.component.toShortString();
                EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, id, tag, userId, mustHaveFlags, mustNotHaveFlags, reason, listenerName);
                synchronized (NotificationManagerService.this.mNotificationList) {
                    int index = NotificationManagerService.this.indexOfNotificationLocked(pkg, tag, id, userId);
                    if (index >= 0) {
                        NotificationRecord r = NotificationManagerService.this.mNotificationList.get(index);
                        if (reason == 1) {
                            NotificationManagerService.this.mUsageStats.registerClickedByUser(r);
                        }
                        if ((r.getNotification().flags & mustHaveFlags) == mustHaveFlags) {
                            if ((r.getNotification().flags & mustNotHaveFlags) == 0) {
                                NotificationManagerService.this.mNotificationList.remove(index);
                                NotificationManagerService.this.cancelNotificationLocked(r, sendDelete, reason);
                                NotificationManagerService.this.cancelGroupChildrenLocked(r, callingUid, callingPid, listenerName, 12);
                                NotificationManagerService.this.updateLightsLocked();
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean notificationMatchesUserId(NotificationRecord r, int userId) {
        if (userId == -1 || r.getUserId() == -1 || r.getUserId() == userId) {
            return true;
        }
        return SCORE_ONGOING_HIGHER;
    }

    private boolean notificationMatchesCurrentProfiles(NotificationRecord r, int userId) {
        if (notificationMatchesUserId(r, userId) || this.mUserProfiles.isCurrentProfile(r.getUserId())) {
            return true;
        }
        return SCORE_ONGOING_HIGHER;
    }

    boolean cancelAllNotificationsInt(int callingUid, int callingPid, String pkg, int mustHaveFlags, int mustNotHaveFlags, boolean doit, int userId, int reason, ManagedServices.ManagedServiceInfo listener) {
        boolean z;
        String listenerName = listener == null ? null : listener.component.toShortString();
        EventLogTags.writeNotificationCancelAll(callingUid, callingPid, pkg, userId, mustHaveFlags, mustNotHaveFlags, reason, listenerName);
        synchronized (this.mNotificationList) {
            int N = this.mNotificationList.size();
            ArrayList<NotificationRecord> canceledNotifications = null;
            int i = N - 1;
            while (true) {
                if (i >= 0) {
                    NotificationRecord r = this.mNotificationList.get(i);
                    if (notificationMatchesUserId(r, userId) && ((r.getUserId() != -1 || pkg != null) && (r.getFlags() & mustHaveFlags) == mustHaveFlags && (r.getFlags() & mustNotHaveFlags) == 0 && (pkg == null || r.sbn.getPackageName().equals(pkg)))) {
                        if (canceledNotifications == null) {
                            canceledNotifications = new ArrayList<>();
                        }
                        canceledNotifications.add(r);
                        if (!doit) {
                            z = true;
                            break;
                        }
                        this.mNotificationList.remove(i);
                        cancelNotificationLocked(r, SCORE_ONGOING_HIGHER, reason);
                    }
                    i--;
                } else {
                    if (doit && canceledNotifications != null) {
                        int M = canceledNotifications.size();
                        for (int i2 = 0; i2 < M; i2++) {
                            cancelGroupChildrenLocked(canceledNotifications.get(i2), callingUid, callingPid, listenerName, 12);
                        }
                    }
                    if (canceledNotifications != null) {
                        updateLightsLocked();
                    }
                    z = canceledNotifications != null ? true : SCORE_ONGOING_HIGHER;
                }
            }
        }
        return z;
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
            cancelGroupChildrenLocked(canceledNotifications.get(i2), callingUid, callingPid, listenerName, 12);
        }
        updateLightsLocked();
    }

    private void cancelGroupChildrenLocked(NotificationRecord r, int callingUid, int callingPid, String listenerName, int reason) {
        Notification n = r.getNotification();
        if (n.isGroupSummary()) {
            String pkg = r.sbn.getPackageName();
            int userId = r.getUserId();
            if (pkg == null) {
                if (DBG) {
                    Log.e(TAG, "No package for group summary: " + r.getKey());
                    return;
                }
                return;
            }
            int N = this.mNotificationList.size();
            for (int i = N - 1; i >= 0; i--) {
                NotificationRecord childR = this.mNotificationList.get(i);
                StatusBarNotification childSbn = childR.sbn;
                if (childR.getNotification().isGroupChild() && childR.getGroupKey().equals(r.getGroupKey())) {
                    EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, childSbn.getId(), childSbn.getTag(), userId, 0, 0, reason, listenerName);
                    this.mNotificationList.remove(i);
                    cancelNotificationLocked(childR, SCORE_ONGOING_HIGHER, reason);
                }
            }
        }
    }

    void updateLightsLocked() {
        NotificationRecord ledNotification = null;
        while (ledNotification == null && !this.mLights.isEmpty()) {
            String owner = this.mLights.get(this.mLights.size() - 1);
            NotificationRecord ledNotification2 = this.mNotificationsByKey.get(owner);
            ledNotification = ledNotification2;
            if (ledNotification == null) {
                Slog.wtfStack(TAG, "LED Notification does not exist: " + owner);
                this.mLights.remove(owner);
            }
        }
        if (ledNotification == null || this.mInCall || this.mScreenOn) {
            this.mNotificationLight.turnOff();
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
        this.mStatusBar.notificationLightPulse(ledARGB, ledOnMS, ledOffMS);
    }

    int indexOfNotificationLocked(String pkg, String tag, int id, int userId) {
        ArrayList<NotificationRecord> list = this.mNotificationList;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = list.get(i);
            if (notificationMatchesUserId(r, userId) && r.sbn.getId() == id) {
                if (tag == null) {
                    if (r.sbn.getTag() != null) {
                        continue;
                    } else if (r.sbn.getPackageName().equals(pkg)) {
                        return i;
                    }
                } else if (!tag.equals(r.sbn.getTag())) {
                    continue;
                }
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
        if (appid == 1000 || appid == 1001 || uid == 0) {
            return true;
        }
        return SCORE_ONGOING_HIGHER;
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
        if (!isCallerSystem()) {
            int uid = Binder.getCallingUid();
            try {
                ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
                if (ai == null) {
                    throw new SecurityException("Unknown package " + pkg);
                }
                if (!UserHandle.isSameApp(ai.uid, uid)) {
                    throw new SecurityException("Calling uid " + uid + " gave package" + pkg + " which is owned by uid " + ai.uid);
                }
            } catch (RemoteException re) {
                throw new SecurityException("Unknown package " + pkg + "\n" + re);
            }
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
                if (NotificationManagerService.this.mCallState != state) {
                    if (NotificationManagerService.DBG) {
                        Slog.d(NotificationManagerService.TAG, "Call state changed: " + NotificationManagerService.callStateToString(state));
                    }
                    NotificationManagerService.this.mCallState = state;
                }
            }
        }, 32);
    }

    private NotificationRankingUpdate makeRankingUpdateLocked(ManagedServices.ManagedServiceInfo info) {
        int speedBumpIndex = -1;
        int N = this.mNotificationList.size();
        ArrayList<String> keys = new ArrayList<>(N);
        ArrayList<String> interceptedKeys = new ArrayList<>(N);
        Bundle visibilityOverrides = new Bundle();
        for (int i = 0; i < N; i++) {
            NotificationRecord record = this.mNotificationList.get(i);
            if (isVisibleToListener(record.sbn, info)) {
                keys.add(record.sbn.getKey());
                if (record.isIntercepted()) {
                    interceptedKeys.add(record.sbn.getKey());
                }
                if (record.getPackageVisibilityOverride() != JUNK_SCORE) {
                    visibilityOverrides.putInt(record.sbn.getKey(), record.getPackageVisibilityOverride());
                }
                if (speedBumpIndex == -1 && !record.isRecentlyIntrusive() && record.getPackagePriority() <= 0 && record.sbn.getNotification().priority == -2) {
                    speedBumpIndex = keys.size() - 1;
                }
            }
        }
        String[] keysAr = (String[]) keys.toArray(new String[keys.size()]);
        String[] interceptedKeysAr = (String[]) interceptedKeys.toArray(new String[interceptedKeys.size()]);
        return new NotificationRankingUpdate(keysAr, interceptedKeysAr, visibilityOverrides, speedBumpIndex);
    }

    private boolean isVisibleToListener(StatusBarNotification sbn, ManagedServices.ManagedServiceInfo listener) {
        if (listener.enabledAndUserMatches(sbn.getUserId())) {
            return true;
        }
        return SCORE_ONGOING_HIGHER;
    }

    public class NotificationListeners extends ManagedServices {
        private final ArraySet<ManagedServices.ManagedServiceInfo> mLightTrimListeners;
        private boolean mNotificationGroupsDesired;

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
            c.clientLabel = R.string.keyguard_accessibility_widget_changed;
            return c;
        }

        @Override
        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        @Override
        public void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
            NotificationRankingUpdate update;
            INotificationListener listener = info.service;
            synchronized (NotificationManagerService.this.mNotificationList) {
                updateNotificationGroupsDesiredLocked();
                update = NotificationManagerService.this.makeRankingUpdateLocked(info);
            }
            try {
                listener.onListenerConnected(update);
            } catch (RemoteException e) {
            }
        }

        @Override
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
            if (NotificationManagerService.this.mListenersDisablingEffects.remove(removed)) {
                NotificationManagerService.this.updateListenerHintsLocked();
                NotificationManagerService.this.updateEffectsSuppressorLocked();
            }
            this.mLightTrimListeners.remove(removed);
            updateNotificationGroupsDesiredLocked();
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
            boolean oldSbnVisible;
            StatusBarNotification sbnClone = null;
            StatusBarNotification sbnCloneLight = null;
            for (final ManagedServices.ManagedServiceInfo info : this.mServices) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, info);
                if (oldSbn != null) {
                    oldSbnVisible = NotificationManagerService.this.isVisibleToListener(oldSbn, info);
                } else {
                    oldSbnVisible = NotificationManagerService.SCORE_ONGOING_HIGHER;
                }
                if (oldSbnVisible || sbnVisible) {
                    final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                    if (!oldSbnVisible || sbnVisible) {
                        int trim = NotificationManagerService.this.mListeners.getOnNotificationPostedTrim(info);
                        if (trim == 1 && sbnCloneLight == null) {
                            sbnCloneLight = sbn.cloneLight();
                        } else if (trim == 0 && sbnClone == null) {
                            sbnClone = sbn.clone();
                        }
                        final StatusBarNotification sbnToPost = trim == 0 ? sbnClone : sbnCloneLight;
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyPosted(info, sbnToPost, update);
                            }
                        });
                    } else {
                        final StatusBarNotification oldSbnLightClone = oldSbn.cloneLight();
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyRemoved(info, oldSbnLightClone, update);
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
            if (info.enabledAndUserMatches(sbn.getUserId())) {
                INotificationListener listener = info.service;
                StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder(sbn);
                try {
                    listener.onNotificationRemoved(sbnHolder, rankingUpdate);
                } catch (RemoteException ex) {
                    Log.e(this.TAG, "unable to notify listener (removed): " + listener, ex);
                }
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
            boolean z = NotificationManagerService.SCORE_ONGOING_HIGHER;
            if (packageName != null) {
                synchronized (NotificationManagerService.this.mNotificationList) {
                    Iterator<ManagedServices.ManagedServiceInfo> it = this.mServices.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        ManagedServices.ManagedServiceInfo serviceInfo = it.next();
                        if (packageName.equals(serviceInfo.component.getPackageName())) {
                            z = true;
                            break;
                        }
                    }
                }
            }
            return z;
        }

        public boolean notificationGroupsDesired() {
            return this.mNotificationGroupsDesired;
        }

        private void updateNotificationGroupsDesiredLocked() {
            this.mNotificationGroupsDesired = true;
            if (this.mServices.isEmpty()) {
                this.mNotificationGroupsDesired = NotificationManagerService.SCORE_ONGOING_HIGHER;
            } else if (this.mServices.size() == 1 && this.mServices.get(0).component.getPackageName().equals("com.android.systemui")) {
                this.mNotificationGroupsDesired = NotificationManagerService.SCORE_ONGOING_HIGHER;
            }
        }
    }

    public static final class DumpFilter {
        public String pkgFilter;
        public boolean zen;

        public static DumpFilter parseFromArguments(String[] args) {
            if (args != null && args.length == 2 && "p".equals(args[0]) && args[1] != null && !args[1].trim().isEmpty()) {
                DumpFilter filter = new DumpFilter();
                filter.pkgFilter = args[1].trim().toLowerCase();
                return filter;
            }
            if (args != null && args.length == 1 && "zen".equals(args[0])) {
                DumpFilter filter2 = new DumpFilter();
                filter2.zen = true;
                return filter2;
            }
            return null;
        }

        public boolean matches(StatusBarNotification sbn) {
            if (this.zen) {
                return true;
            }
            if (sbn == null || !(matches(sbn.getPackageName()) || matches(sbn.getOpPkg()))) {
                return NotificationManagerService.SCORE_ONGOING_HIGHER;
            }
            return true;
        }

        public boolean matches(ComponentName component) {
            if (this.zen) {
                return true;
            }
            if (component == null || !matches(component.getPackageName())) {
                return NotificationManagerService.SCORE_ONGOING_HIGHER;
            }
            return true;
        }

        public boolean matches(String pkg) {
            if (this.zen) {
                return true;
            }
            if (pkg == null || !pkg.toLowerCase().contains(this.pkgFilter)) {
                return NotificationManagerService.SCORE_ONGOING_HIGHER;
            }
            return true;
        }

        public String toString() {
            return this.zen ? "zen" : '\'' + this.pkgFilter + '\'';
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
}
