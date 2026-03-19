package com.android.server.notification;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.backup.BackupManager;
import android.app.usage.UsageStatsManagerInternal;
import android.companion.ICompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.IRingtonePlayer;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.NotificationRankingUpdate;
import android.service.notification.NotificationStats;
import android.service.notification.NotifyingApp;
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
import android.util.proto.ProtoOutputStream;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.DeviceIdleController;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.GroupHelper;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.NotificationRecord;
import com.android.server.notification.SnoozeHelper;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.ppl.MtkPplManager;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NotificationManagerService extends SystemService {
    private static final String ATTR_VERSION = "version";
    private static final int DB_VERSION = 1;
    static final float DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE = 5.0f;
    static final int DEFAULT_STREAM_TYPE = 5;
    private static final long DELAY_FOR_ASSISTANT_TIME = 100;
    static final boolean ENABLE_BLOCKED_TOASTS = true;
    private static final int EVENTLOG_ENQUEUE_STATUS_IGNORED = 2;
    private static final int EVENTLOG_ENQUEUE_STATUS_NEW = 0;
    private static final int EVENTLOG_ENQUEUE_STATUS_UPDATE = 1;
    private static final String EXTRA_KEY = "key";
    static final int FINISH_TOKEN_TIMEOUT = 11000;
    static final int LONG_DELAY = 3500;
    static final int MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS = 3000;
    static final float MATCHES_CALL_FILTER_TIMEOUT_AFFINITY = 1.0f;
    static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    static final int MESSAGE_DURATION_REACHED = 2;
    static final int MESSAGE_FINISH_TOKEN_TIMEOUT = 7;
    static final int MESSAGE_LISTENER_HINTS_CHANGED = 5;
    static final int MESSAGE_LISTENER_NOTIFICATION_FILTER_CHANGED = 6;
    private static final int MESSAGE_RANKING_SORT = 1001;
    private static final int MESSAGE_RECONSIDER_RANKING = 1000;
    static final int MESSAGE_SAVE_POLICY_FILE = 3;
    static final int MESSAGE_SEND_RANKING_UPDATE = 4;
    private static final long MIN_PACKAGE_OVERRATE_LOG_INTERVAL = 5000;
    private static final int REQUEST_CODE_TIMEOUT = 1;
    private static final String SCHEME_TIMEOUT = "timeout";
    static final int SHORT_DELAY = 2000;
    static final long SNOOZE_UNTIL_UNSPECIFIED = -1;
    private static final String TAG_NOTIFICATION_POLICY = "notification-policy";
    static final int VIBRATE_PATTERN_MAXLEN = 17;
    private AccessibilityManager mAccessibilityManager;
    private ActivityManager mActivityManager;
    private AlarmManager mAlarmManager;
    private Predicate<String> mAllowedManagedServicePackages;
    private IActivityManager mAm;
    private ActivityManagerInternal mAmi;
    private AppOpsManager mAppOps;
    private UsageStatsManagerInternal mAppUsageStats;
    private Archive mArchive;
    private NotificationAssistants mAssistants;
    Light mAttentionLight;
    AudioManager mAudioManager;
    AudioManagerInternal mAudioManagerInternal;

    @GuardedBy("mNotificationLock")
    final ArrayMap<Integer, ArrayMap<String, String>> mAutobundledSummaries;
    private int mCallState;
    private ICompanionDeviceManager mCompanionManager;
    private ConditionProviders mConditionProviders;
    private IDeviceIdleController mDeviceIdleController;
    private boolean mDisableNotificationEffects;
    private DevicePolicyManagerInternal mDpm;
    private List<ComponentName> mEffectsSuppressors;

    @GuardedBy("mNotificationLock")
    final ArrayList<NotificationRecord> mEnqueuedNotifications;
    private long[] mFallbackVibrationPattern;
    final IBinder mForegroundToken;
    private GroupHelper mGroupHelper;
    private WorkerHandler mHandler;
    protected boolean mInCall;
    private AudioAttributes mInCallNotificationAudioAttributes;
    private Uri mInCallNotificationUri;
    private float mInCallNotificationVolume;
    private final BroadcastReceiver mIntentReceiver;
    private final NotificationManagerInternal mInternalService;
    private int mInterruptionFilter;
    private boolean mIsTelevision;
    private KeyguardManager mKeyguardManager;
    private long mLastOverRateLogTime;
    ArrayList<String> mLights;
    private int mListenerHints;
    private NotificationListeners mListeners;
    private final SparseArray<ArraySet<ManagedServices.ManagedServiceInfo>> mListenersDisablingEffects;
    protected final BroadcastReceiver mLocaleChangeReceiver;
    private float mMaxPackageEnqueueRate;
    private MetricsLogger mMetricsLogger;
    public MtkPplManager mMtkPplManager;

    @VisibleForTesting
    final NotificationDelegate mNotificationDelegate;
    private Light mNotificationLight;

    @GuardedBy("mNotificationLock")
    final ArrayList<NotificationRecord> mNotificationList;
    final Object mNotificationLock;
    private boolean mNotificationPulseEnabled;
    private final BroadcastReceiver mNotificationTimeoutReceiver;

    @GuardedBy("mNotificationLock")
    final ArrayMap<String, NotificationRecord> mNotificationsByKey;
    private final BroadcastReceiver mPackageIntentReceiver;
    private IPackageManager mPackageManager;
    private PackageManager mPackageManagerClient;
    private AtomicFile mPolicyFile;
    private RankingHandler mRankingHandler;
    private RankingHelper mRankingHelper;
    private final HandlerThread mRankingThread;
    final ArrayMap<Integer, ArrayList<NotifyingApp>> mRecentApps;
    private final BroadcastReceiver mRestoreReceiver;
    private boolean mScreenOn;
    private final IBinder mService;
    private SettingsObserver mSettingsObserver;
    private SnoozeHelper mSnoozeHelper;
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
    private WindowManagerInternal mWindowManagerInternal;
    protected ZenModeHelper mZenModeHelper;
    static final String TAG = "NotificationService";
    static final boolean DBG = Log.isLoggable(TAG, 3);
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    static final boolean DEBUG_INTERRUPTIVENESS = SystemProperties.getBoolean("debug.notification.interruptiveness", false);
    static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final String ACTION_NOTIFICATION_TIMEOUT = NotificationManagerService.class.getSimpleName() + ".TIMEOUT";
    private static final int MY_UID = Process.myUid();
    private static final int MY_PID = Process.myPid();
    private static final IBinder WHITELIST_TOKEN = new Binder();

    private interface FlagChecker {
        boolean apply(int i);
    }

    private static class Archive {
        final ArrayDeque<StatusBarNotification> mBuffer;
        final int mBufferSize;

        public Archive(int i) {
            this.mBufferSize = i;
            this.mBuffer = new ArrayDeque<>(this.mBufferSize);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int size = this.mBuffer.size();
            sb.append("Archive (");
            sb.append(size);
            sb.append(" notification");
            sb.append(size == 1 ? ")" : "s)");
            return sb.toString();
        }

        public void record(StatusBarNotification statusBarNotification) {
            if (this.mBuffer.size() == this.mBufferSize) {
                this.mBuffer.removeFirst();
            }
            this.mBuffer.addLast(statusBarNotification.cloneLight());
        }

        public Iterator<StatusBarNotification> descendingIterator() {
            return this.mBuffer.descendingIterator();
        }

        public StatusBarNotification[] getArray(int i) {
            if (i == 0) {
                i = this.mBufferSize;
            }
            StatusBarNotification[] statusBarNotificationArr = new StatusBarNotification[Math.min(i, this.mBuffer.size())];
            Iterator<StatusBarNotification> itDescendingIterator = descendingIterator();
            for (int i2 = 0; itDescendingIterator.hasNext() && i2 < i; i2++) {
                statusBarNotificationArr[i2] = itDescendingIterator.next();
            }
            return statusBarNotificationArr;
        }
    }

    protected void readDefaultApprovedServices(int i) {
        String string = getContext().getResources().getString(R.string.action_bar_up_description);
        if (string != null) {
            for (String str : string.split(":")) {
                Iterator<ComponentName> it = this.mListeners.queryPackageForServices(str, 786432, i).iterator();
                while (it.hasNext()) {
                    try {
                        getBinderService().setNotificationListenerAccessGrantedForUser(it.next(), i, true);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String string2 = getContext().getResources().getString(R.string.action_bar_home_subtitle_description_format);
        if (string != null) {
            for (String str2 : string2.split(":")) {
                try {
                    getBinderService().setNotificationPolicyAccessGranted(str2, true);
                } catch (RemoteException e2) {
                    e2.printStackTrace();
                }
            }
        }
        readDefaultAssistant(i);
    }

    protected void readDefaultAssistant(int i) {
        String string = getContext().getResources().getString(R.string.accessibility_system_action_screenshot_label);
        if (string != null) {
            Iterator<ComponentName> it = this.mAssistants.queryPackageForServices(string, 786432, i).iterator();
            while (it.hasNext()) {
                try {
                    getBinderService().setNotificationAssistantAccessGrantedForUser(it.next(), i, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void readPolicyXml(InputStream inputStream, boolean z) throws XmlPullParserException, IOException, NumberFormatException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        XmlUtils.beginDocument(xmlPullParserNewPullParser, TAG_NOTIFICATION_POLICY);
        int depth = xmlPullParserNewPullParser.getDepth();
        boolean z2 = false;
        while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth)) {
            if ("zen".equals(xmlPullParserNewPullParser.getName())) {
                this.mZenModeHelper.readXml(xmlPullParserNewPullParser, z);
            } else if ("ranking".equals(xmlPullParserNewPullParser.getName())) {
                this.mRankingHelper.readXml(xmlPullParserNewPullParser, z);
            }
            if (this.mListeners.getConfig().xmlTag.equals(xmlPullParserNewPullParser.getName())) {
                this.mListeners.readXml(xmlPullParserNewPullParser, this.mAllowedManagedServicePackages);
            } else if (this.mAssistants.getConfig().xmlTag.equals(xmlPullParserNewPullParser.getName())) {
                this.mAssistants.readXml(xmlPullParserNewPullParser, this.mAllowedManagedServicePackages);
            } else if (this.mConditionProviders.getConfig().xmlTag.equals(xmlPullParserNewPullParser.getName())) {
                this.mConditionProviders.readXml(xmlPullParserNewPullParser, this.mAllowedManagedServicePackages);
            }
            z2 = true;
        }
        if (!z2) {
            this.mListeners.migrateToXml();
            this.mAssistants.migrateToXml();
            this.mConditionProviders.migrateToXml();
            savePolicyFile();
        }
        this.mAssistants.ensureAssistant();
    }

    private void loadPolicyFile() {
        FileInputStream fileInputStreamOpenRead;
        if (DBG) {
            Slog.d(TAG, "loadPolicyFile");
        }
        synchronized (this.mPolicyFile) {
            FileInputStream fileInputStream = null;
            try {
                try {
                    fileInputStreamOpenRead = this.mPolicyFile.openRead();
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
                e = e2;
            } catch (NumberFormatException e3) {
                e = e3;
            } catch (XmlPullParserException e4) {
                e = e4;
            }
            try {
                readPolicyXml(fileInputStreamOpenRead, false);
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            } catch (FileNotFoundException e5) {
                fileInputStream = fileInputStreamOpenRead;
                readDefaultApprovedServices(0);
                IoUtils.closeQuietly(fileInputStream);
            } catch (IOException e6) {
                e = e6;
                fileInputStream = fileInputStreamOpenRead;
                Log.wtf(TAG, "Unable to read notification policy", e);
                IoUtils.closeQuietly(fileInputStream);
            } catch (NumberFormatException e7) {
                e = e7;
                fileInputStream = fileInputStreamOpenRead;
                Log.wtf(TAG, "Unable to parse notification policy", e);
                IoUtils.closeQuietly(fileInputStream);
            } catch (XmlPullParserException e8) {
                e = e8;
                fileInputStream = fileInputStreamOpenRead;
                Log.wtf(TAG, "Unable to parse notification policy", e);
                IoUtils.closeQuietly(fileInputStream);
            } catch (Throwable th2) {
                th = th2;
                fileInputStream = fileInputStreamOpenRead;
                IoUtils.closeQuietly(fileInputStream);
                throw th;
            }
        }
    }

    public void savePolicyFile() {
        this.mHandler.removeMessages(3);
        this.mHandler.sendEmptyMessage(3);
    }

    private void handleSavePolicyFile() {
        if (DBG) {
            Slog.d(TAG, "handleSavePolicyFile");
        }
        synchronized (this.mPolicyFile) {
            try {
                FileOutputStream fileOutputStreamStartWrite = this.mPolicyFile.startWrite();
                try {
                    writePolicyXml(fileOutputStreamStartWrite, false);
                    this.mPolicyFile.finishWrite(fileOutputStreamStartWrite);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to save policy file, restoring backup", e);
                    this.mPolicyFile.failWrite(fileOutputStreamStartWrite);
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to save policy file", e2);
                return;
            }
        }
        BackupManager.dataChanged(getContext().getPackageName());
    }

    private void writePolicyXml(OutputStream outputStream, boolean z) throws IOException {
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        fastXmlSerializer.startDocument(null, true);
        fastXmlSerializer.startTag(null, TAG_NOTIFICATION_POLICY);
        fastXmlSerializer.attribute(null, ATTR_VERSION, Integer.toString(1));
        this.mZenModeHelper.writeXml(fastXmlSerializer, z, null);
        this.mRankingHelper.writeXml(fastXmlSerializer, z);
        this.mListeners.writeXml(fastXmlSerializer, z);
        this.mAssistants.writeXml(fastXmlSerializer, z);
        this.mConditionProviders.writeXml(fastXmlSerializer, z);
        fastXmlSerializer.endTag(null, TAG_NOTIFICATION_POLICY);
        fastXmlSerializer.endDocument();
    }

    private static final class ToastRecord {
        ITransientNotification callback;
        int duration;
        final int pid;
        final String pkg;
        Binder token;

        ToastRecord(int i, String str, ITransientNotification iTransientNotification, int i2, Binder binder) {
            this.pid = i;
            this.pkg = str;
            this.callback = iTransientNotification;
            this.duration = i2;
            this.token = binder;
        }

        void update(int i) {
            this.duration = i;
        }

        void update(ITransientNotification iTransientNotification) {
            this.callback = iTransientNotification;
        }

        void dump(PrintWriter printWriter, String str, DumpFilter dumpFilter) {
            if (dumpFilter == null || dumpFilter.matches(this.pkg)) {
                printWriter.println(str + this);
            }
        }

        public final String toString() {
            return "ToastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " pkg=" + this.pkg + " callback=" + this.callback + " duration=" + this.duration;
        }
    }

    class AnonymousClass1 implements NotificationDelegate {
        AnonymousClass1() {
        }

        @Override
        public void onSetDisabled(int i) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mDisableNotificationEffects = (i & DumpState.DUMP_DOMAIN_PREFERRED) != 0;
                if (NotificationManagerService.this.disableNotificationEffects(null) != null) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        IRingtonePlayer ringtonePlayer = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                        if (ringtonePlayer != null) {
                            ringtonePlayer.stopAsync();
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (RemoteException e) {
                    } finally {
                    }
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        NotificationManagerService.this.mVibrator.cancel();
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } finally {
                    }
                }
            }
        }

        @Override
        public void onClearAll(int i, int i2, int i3) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.cancelAllLocked(i, i2, i3, 3, null, true);
            }
        }

        @Override
        public void onNotificationClick(int i, int i2, String str, NotificationVisibility notificationVisibility) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord == null) {
                    Log.w(NotificationManagerService.TAG, "No notification with key: " + str);
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                MetricsLogger.action(notificationRecord.getLogMaker(jCurrentTimeMillis).setCategory(128).setType(4).addTaggedData(798, Integer.valueOf(notificationVisibility.rank)).addTaggedData(1395, Integer.valueOf(notificationVisibility.count)));
                EventLogTags.writeNotificationClicked(str, notificationRecord.getLifespanMs(jCurrentTimeMillis), notificationRecord.getFreshnessMs(jCurrentTimeMillis), notificationRecord.getExposureMs(jCurrentTimeMillis), notificationVisibility.rank, notificationVisibility.count);
                StatusBarNotification statusBarNotification = notificationRecord.sbn;
                NotificationManagerService.this.cancelNotification(i, i2, statusBarNotification.getPackageName(), statusBarNotification.getTag(), statusBarNotification.getId(), 16, 64, false, notificationRecord.getUserId(), 1, notificationVisibility.rank, notificationVisibility.count, null);
                notificationVisibility.recycle();
                NotificationManagerService.this.reportUserInteraction(notificationRecord);
            }
        }

        @Override
        public void onNotificationActionClick(int i, int i2, String str, int i3, NotificationVisibility notificationVisibility) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord == null) {
                    Log.w(NotificationManagerService.TAG, "No notification with key: " + str);
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                MetricsLogger.action(notificationRecord.getLogMaker(jCurrentTimeMillis).setCategory(NetworkConstants.ICMPV6_ECHO_REPLY_TYPE).setType(4).setSubtype(i3).addTaggedData(798, Integer.valueOf(notificationVisibility.rank)).addTaggedData(1395, Integer.valueOf(notificationVisibility.count)));
                EventLogTags.writeNotificationActionClicked(str, i3, notificationRecord.getLifespanMs(jCurrentTimeMillis), notificationRecord.getFreshnessMs(jCurrentTimeMillis), notificationRecord.getExposureMs(jCurrentTimeMillis), notificationVisibility.rank, notificationVisibility.count);
                notificationVisibility.recycle();
                NotificationManagerService.this.reportUserInteraction(notificationRecord);
            }
        }

        @Override
        public void onNotificationClear(int i, int i2, String str, String str2, int i3, int i4, String str3, int i5, NotificationVisibility notificationVisibility) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str3);
                if (notificationRecord != null) {
                    notificationRecord.recordDismissalSurface(i5);
                }
            }
            NotificationManagerService.this.cancelNotification(i, i2, str, str2, i3, 0, 66, true, i4, 2, notificationVisibility.rank, notificationVisibility.count, null);
            notificationVisibility.recycle();
        }

        @Override
        public void onPanelRevealed(boolean z, int i) {
            MetricsLogger.visible(NotificationManagerService.this.getContext(), 127);
            MetricsLogger.histogram(NotificationManagerService.this.getContext(), "note_load", i);
            EventLogTags.writeNotificationPanelRevealed(i);
            if (z) {
                clearEffects();
            }
        }

        @Override
        public void onPanelHidden() {
            MetricsLogger.hidden(NotificationManagerService.this.getContext(), 127);
            EventLogTags.writeNotificationPanelHidden();
        }

        @Override
        public void clearEffects() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "clearEffects");
                }
                NotificationManagerService.this.clearSoundLocked();
                NotificationManagerService.this.clearVibrateLocked();
                NotificationManagerService.this.clearLightsLocked();
            }
        }

        @Override
        public void onNotificationError(int i, int i2, final String str, final String str2, final int i3, final int i4, final int i5, final String str3, int i6) {
            boolean z;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecordFindNotificationLocked = NotificationManagerService.this.findNotificationLocked(str, str2, i3, i6);
                z = (notificationRecordFindNotificationLocked == null || (notificationRecordFindNotificationLocked.getNotification().flags & 64) == 0) ? false : true;
            }
            NotificationManagerService.this.cancelNotification(i, i2, str, str2, i3, 0, 0, false, i6, 4, null);
            if (z) {
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() {
                        NotificationManagerService.AnonymousClass1 anonymousClass1 = this.f$0;
                        int i7 = i4;
                        int i8 = i5;
                        String str4 = str;
                        NotificationManagerService.this.mAm.crashApplication(i7, i8, str4, -1, "Bad notification(tag=" + str2 + ", id=" + i3 + ") posted from package " + str4 + ", crashing app(uid=" + i7 + ", pid=" + i8 + "): " + str3, true);
                    }
                });
            }
        }

        @Override
        public void onNotificationVisibilityChanged(NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                for (NotificationVisibility notificationVisibility : notificationVisibilityArr) {
                    NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(notificationVisibility.key);
                    if (notificationRecord != null) {
                        if (!notificationRecord.isSeen()) {
                            if (NotificationManagerService.DBG) {
                                Slog.d(NotificationManagerService.TAG, "Marking notification as visible " + notificationVisibility.key);
                            }
                            NotificationManagerService.this.reportSeen(notificationRecord);
                            if (notificationRecord.getNumSmartRepliesAdded() > 0 && !notificationRecord.hasSeenSmartReplies()) {
                                notificationRecord.setSeenSmartReplies(true);
                                NotificationManagerService.this.mMetricsLogger.write(notificationRecord.getLogMaker().setCategory(1382).addTaggedData(1384, Integer.valueOf(notificationRecord.getNumSmartRepliesAdded())));
                            }
                        }
                        notificationRecord.setVisibility(true, notificationVisibility.rank, notificationVisibility.count);
                        NotificationManagerService.this.maybeRecordInterruptionLocked(notificationRecord);
                        notificationVisibility.recycle();
                    }
                }
                for (NotificationVisibility notificationVisibility2 : notificationVisibilityArr2) {
                    NotificationRecord notificationRecord2 = NotificationManagerService.this.mNotificationsByKey.get(notificationVisibility2.key);
                    if (notificationRecord2 != null) {
                        notificationRecord2.setVisibility(false, notificationVisibility2.rank, notificationVisibility2.count);
                        notificationVisibility2.recycle();
                    }
                }
            }
        }

        @Override
        public void onNotificationExpansionChanged(String str, boolean z, boolean z2) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord != null) {
                    notificationRecord.stats.onExpansionChanged(z, z2);
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    if (z) {
                        MetricsLogger.action(notificationRecord.getLogMaker(jCurrentTimeMillis).setCategory(128).setType(z2 ? 3 : 14));
                    }
                    if (z2 && z) {
                        notificationRecord.recordExpanded();
                    }
                    EventLogTags.writeNotificationExpansion(str, z ? 1 : 0, z2 ? 1 : 0, notificationRecord.getLifespanMs(jCurrentTimeMillis), notificationRecord.getFreshnessMs(jCurrentTimeMillis), notificationRecord.getExposureMs(jCurrentTimeMillis));
                }
            }
        }

        @Override
        public void onNotificationDirectReplied(String str) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord != null) {
                    notificationRecord.recordDirectReplied();
                    NotificationManagerService.this.reportUserInteraction(notificationRecord);
                }
            }
        }

        @Override
        public void onNotificationSmartRepliesAdded(String str, int i) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord != null) {
                    notificationRecord.setNumSmartRepliesAdded(i);
                }
            }
        }

        @Override
        public void onNotificationSmartReplySent(String str, int i) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord != null) {
                    NotificationManagerService.this.mMetricsLogger.write(notificationRecord.getLogMaker().setCategory(1383).setSubtype(i));
                    NotificationManagerService.this.reportUserInteraction(notificationRecord);
                }
            }
        }

        @Override
        public void onNotificationSettingsViewed(String str) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(str);
                if (notificationRecord != null) {
                    notificationRecord.recordViewedSettings();
                }
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private void clearSoundLocked() {
        this.mSoundNotificationKey = null;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer ringtonePlayer = this.mAudioManager.getRingtonePlayer();
            if (ringtonePlayer != null) {
                ringtonePlayer.stopAsync();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    @GuardedBy("mNotificationLock")
    private void clearVibrateLocked() {
        this.mVibrateNotificationKey = null;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mVibrator.cancel();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mNotificationLock")
    private void clearLightsLocked() {
        this.mLights.clear();
        updateLightsLocked();
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_BADGING_URI;
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;
        private final Uri NOTIFICATION_RATE_LIMIT_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_BADGING_URI = Settings.Secure.getUriFor("notification_badging");
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.NOTIFICATION_RATE_LIMIT_URI = Settings.Global.getUriFor("max_notification_enqueue_rate");
        }

        void observe() {
            ContentResolver contentResolver = NotificationManagerService.this.getContext().getContentResolver();
            contentResolver.registerContentObserver(this.NOTIFICATION_BADGING_URI, false, this, -1);
            contentResolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this, -1);
            contentResolver.registerContentObserver(this.NOTIFICATION_RATE_LIMIT_URI, false, this, -1);
            update(null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver contentResolver = NotificationManagerService.this.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                boolean z = Settings.System.getIntForUser(contentResolver, "notification_light_pulse", 0, -2) != 0;
                if (NotificationManagerService.this.mNotificationPulseEnabled != z) {
                    NotificationManagerService.this.mNotificationPulseEnabled = z;
                    NotificationManagerService.this.updateNotificationPulse();
                }
            }
            if (uri == null || this.NOTIFICATION_RATE_LIMIT_URI.equals(uri)) {
                NotificationManagerService.this.mMaxPackageEnqueueRate = Settings.Global.getFloat(contentResolver, "max_notification_enqueue_rate", NotificationManagerService.this.mMaxPackageEnqueueRate);
            }
            if (uri == null || this.NOTIFICATION_BADGING_URI.equals(uri)) {
                NotificationManagerService.this.mRankingHelper.updateBadgingEnabled();
            }
        }
    }

    static long[] getLongArray(Resources resources, int i, int i2, long[] jArr) {
        int[] intArray = resources.getIntArray(i);
        if (intArray == null) {
            return jArr;
        }
        if (intArray.length <= i2) {
            i2 = intArray.length;
        }
        long[] jArr2 = new long[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            jArr2[i3] = intArray[i3];
        }
        return jArr2;
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
        this.mNotificationLock = new Object();
        this.mNotificationList = new ArrayList<>();
        this.mNotificationsByKey = new ArrayMap<>();
        this.mEnqueuedNotifications = new ArrayList<>();
        this.mAutobundledSummaries = new ArrayMap<>();
        this.mToastQueue = new ArrayList<>();
        this.mSummaryByGroupKey = new ArrayMap<>();
        this.mRecentApps = new ArrayMap<>();
        this.mLights = new ArrayList<>();
        this.mUserProfiles = new ManagedServices.UserProfiles();
        this.mMaxPackageEnqueueRate = 5.0f;
        this.mMtkPplManager = MtkSystemServiceFactory.getInstance().makeMtkPplManager();
        this.mNotificationDelegate = new AnonymousClass1();
        this.mLocaleChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    SystemNotificationChannels.createAll(context2);
                    NotificationManagerService.this.mZenModeHelper.updateDefaultZenRules();
                    NotificationManagerService.this.mRankingHelper.onLocaleChanged(context2, ActivityManager.getCurrentUser());
                }
            }
        };
        this.mRestoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.os.action.SETTING_RESTORED".equals(intent.getAction())) {
                    try {
                        String stringExtra = intent.getStringExtra("setting_name");
                        String stringExtra2 = intent.getStringExtra("new_value");
                        int intExtra = intent.getIntExtra("restored_from_sdk_int", 0);
                        NotificationManagerService.this.mListeners.onSettingRestored(stringExtra, stringExtra2, intExtra, getSendingUserId());
                        NotificationManagerService.this.mConditionProviders.onSettingRestored(stringExtra, stringExtra2, intExtra, getSendingUserId());
                    } catch (Exception e) {
                        Slog.wtf(NotificationManagerService.TAG, "Cannot restore managed services from settings", e);
                    }
                }
            }
        };
        this.mNotificationTimeoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NotificationRecord notificationRecordFindNotificationByKeyLocked;
                String action = intent.getAction();
                if (action != null && NotificationManagerService.ACTION_NOTIFICATION_TIMEOUT.equals(action)) {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        notificationRecordFindNotificationByKeyLocked = NotificationManagerService.this.findNotificationByKeyLocked(intent.getStringExtra(NotificationManagerService.EXTRA_KEY));
                    }
                    if (notificationRecordFindNotificationByKeyLocked != null) {
                        NotificationManagerService.this.cancelNotification(notificationRecordFindNotificationByKeyLocked.sbn.getUid(), notificationRecordFindNotificationByKeyLocked.sbn.getInitialPid(), notificationRecordFindNotificationByKeyLocked.sbn.getPackageName(), notificationRecordFindNotificationByKeyLocked.sbn.getTag(), notificationRecordFindNotificationByKeyLocked.sbn.getId(), 0, 64, true, notificationRecordFindNotificationByKeyLocked.getUserId(), 19, null);
                    }
                }
            }
        };
        this.mPackageIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                boolean zEquals;
                boolean zEquals2;
                boolean z;
                String schemeSpecificPart;
                boolean z2;
                boolean z3;
                String[] stringArrayExtra;
                int[] iArr;
                int[] intArrayExtra;
                boolean z4;
                boolean z5;
                int length;
                int i;
                int i2;
                int i3;
                int[] iArr2;
                boolean z6;
                boolean z7;
                int i4;
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                int i5 = 5;
                boolean z8 = false;
                if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                    zEquals = false;
                    zEquals2 = false;
                } else {
                    zEquals = action.equals("android.intent.action.PACKAGE_REMOVED");
                    if (!zEquals && !action.equals("android.intent.action.PACKAGE_RESTARTED")) {
                        zEquals2 = action.equals("android.intent.action.PACKAGE_CHANGED");
                        if (zEquals2) {
                            z = false;
                        } else {
                            boolean zEquals3 = action.equals("android.intent.action.QUERY_PACKAGE_RESTART");
                            if (!zEquals3 && !action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE") && !action.equals("android.intent.action.PACKAGES_SUSPENDED") && !action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                                return;
                            } else {
                                z = zEquals3;
                            }
                        }
                        int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        boolean z9 = true;
                        boolean z10 = (zEquals || intent.getBooleanExtra("android.intent.extra.REPLACING", false)) ? false : true;
                        if (NotificationManagerService.DBG) {
                            Slog.i(NotificationManagerService.TAG, "action=" + action + " removing=" + z10);
                        }
                        if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                            if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                                iArr = null;
                                z5 = true;
                                z3 = false;
                                z4 = false;
                            } else if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                                iArr = null;
                                z4 = true;
                                z3 = false;
                                z5 = false;
                            } else if (z) {
                                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                                intArrayExtra = new int[]{intent.getIntExtra("android.intent.extra.UID", -1)};
                            } else {
                                Uri data = intent.getData();
                                if (data == null || (schemeSpecificPart = data.getSchemeSpecificPart()) == null) {
                                    return;
                                }
                                if (zEquals2) {
                                    try {
                                        int applicationEnabledSetting = NotificationManagerService.this.mPackageManager.getApplicationEnabledSetting(schemeSpecificPart, intExtra != -1 ? intExtra : 0);
                                        z2 = (applicationEnabledSetting == 1 || applicationEnabledSetting == 0) ? false : true;
                                    } catch (RemoteException e) {
                                        z2 = true;
                                    } catch (IllegalArgumentException e2) {
                                        if (NotificationManagerService.DBG) {
                                            Slog.i(NotificationManagerService.TAG, "Exception trying to look up app enabled setting", e2);
                                        }
                                        z2 = true;
                                    }
                                    z3 = z2;
                                    stringArrayExtra = new String[]{schemeSpecificPart};
                                    iArr = new int[]{intent.getIntExtra("android.intent.extra.UID", -1)};
                                    z5 = false;
                                    z4 = false;
                                } else {
                                    z2 = true;
                                    z3 = z2;
                                    stringArrayExtra = new String[]{schemeSpecificPart};
                                    iArr = new int[]{intent.getIntExtra("android.intent.extra.UID", -1)};
                                    z5 = false;
                                    z4 = false;
                                }
                            }
                            if (stringArrayExtra != null && stringArrayExtra.length > 0) {
                                length = stringArrayExtra.length;
                                i = 0;
                                while (i < length) {
                                    String str = stringArrayExtra[i];
                                    if (z3) {
                                        NotificationManagerService notificationManagerService = NotificationManagerService.this;
                                        int i6 = NotificationManagerService.MY_UID;
                                        int i7 = NotificationManagerService.MY_PID;
                                        boolean z11 = !z ? z9 : z8;
                                        i2 = i;
                                        i3 = length;
                                        iArr2 = iArr;
                                        z6 = z10;
                                        z7 = z9;
                                        int i8 = i5;
                                        i4 = intExtra;
                                        notificationManagerService.cancelAllNotificationsInt(i6, i7, str, null, 0, 0, z11, intExtra, i8, null);
                                    } else {
                                        i2 = i;
                                        i3 = length;
                                        iArr2 = iArr;
                                        z6 = z10;
                                        z7 = z9;
                                        i4 = intExtra;
                                        if (z5) {
                                            NotificationManagerService.this.hideNotificationsForPackages(stringArrayExtra);
                                        } else if (z4) {
                                            NotificationManagerService.this.unhideNotificationsForPackages(stringArrayExtra);
                                        }
                                    }
                                    i = i2 + 1;
                                    intExtra = i4;
                                    iArr = iArr2;
                                    length = i3;
                                    z9 = z7;
                                    z10 = z6;
                                    i5 = 5;
                                    z8 = false;
                                }
                            }
                            int[] iArr3 = iArr;
                            boolean z12 = z10;
                            NotificationManagerService.this.mListeners.onPackagesChanged(z12, stringArrayExtra, iArr3);
                            NotificationManagerService.this.mAssistants.onPackagesChanged(z12, stringArrayExtra, iArr3);
                            NotificationManagerService.this.mConditionProviders.onPackagesChanged(z12, stringArrayExtra, iArr3);
                            NotificationManagerService.this.mRankingHelper.onPackagesChanged(z12, intExtra, stringArrayExtra, iArr3);
                            NotificationManagerService.this.savePolicyFile();
                        }
                        stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        intArrayExtra = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                        iArr = intArrayExtra;
                        z3 = true;
                        z5 = false;
                        z4 = false;
                        if (stringArrayExtra != null) {
                            length = stringArrayExtra.length;
                            i = 0;
                            while (i < length) {
                            }
                        }
                        int[] iArr32 = iArr;
                        boolean z122 = z10;
                        NotificationManagerService.this.mListeners.onPackagesChanged(z122, stringArrayExtra, iArr32);
                        NotificationManagerService.this.mAssistants.onPackagesChanged(z122, stringArrayExtra, iArr32);
                        NotificationManagerService.this.mConditionProviders.onPackagesChanged(z122, stringArrayExtra, iArr32);
                        NotificationManagerService.this.mRankingHelper.onPackagesChanged(z122, intExtra, stringArrayExtra, iArr32);
                        NotificationManagerService.this.savePolicyFile();
                    }
                    zEquals2 = false;
                }
                z = zEquals2;
                int intExtra2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                boolean z92 = true;
                if (zEquals) {
                }
                if (NotificationManagerService.DBG) {
                }
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                }
                iArr = intArrayExtra;
                z3 = true;
                z5 = false;
                z4 = false;
                if (stringArrayExtra != null) {
                }
                int[] iArr322 = iArr;
                boolean z1222 = z10;
                NotificationManagerService.this.mListeners.onPackagesChanged(z1222, stringArrayExtra, iArr322);
                NotificationManagerService.this.mAssistants.onPackagesChanged(z1222, stringArrayExtra, iArr322);
                NotificationManagerService.this.mConditionProviders.onPackagesChanged(z1222, stringArrayExtra, iArr322);
                NotificationManagerService.this.mRankingHelper.onPackagesChanged(z1222, intExtra2, stringArrayExtra, iArr322);
                NotificationManagerService.this.savePolicyFile();
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    NotificationManagerService.this.mScreenOn = true;
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    NotificationManagerService.this.mScreenOn = false;
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.PHONE_STATE")) {
                    NotificationManagerService.this.mInCall = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra(AudioService.CONNECT_INTENT_KEY_STATE));
                    NotificationManagerService.this.updateNotificationPulse();
                } else if (action.equals("android.intent.action.USER_STOPPED")) {
                    int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (intExtra >= 0) {
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, intExtra, 6, null);
                    }
                } else if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                    int intExtra2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (intExtra2 >= 0) {
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, intExtra2, 15, null);
                    }
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                    int intExtra3 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mSettingsObserver.update(null);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    NotificationManagerService.this.mConditionProviders.onUserSwitched(intExtra3);
                    NotificationManagerService.this.mListeners.onUserSwitched(intExtra3);
                    NotificationManagerService.this.mAssistants.onUserSwitched(intExtra3);
                    NotificationManagerService.this.mZenModeHelper.onUserSwitched(intExtra3);
                } else if (action.equals("android.intent.action.USER_ADDED")) {
                    int intExtra4 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (intExtra4 != -10000) {
                        NotificationManagerService.this.mUserProfiles.updateCache(context2);
                        if (!NotificationManagerService.this.mUserProfiles.isManagedProfile(intExtra4)) {
                            NotificationManagerService.this.readDefaultApprovedServices(intExtra4);
                        }
                    }
                } else if (action.equals("android.intent.action.USER_REMOVED")) {
                    int intExtra5 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    NotificationManagerService.this.mZenModeHelper.onUserRemoved(intExtra5);
                    NotificationManagerService.this.mRankingHelper.onUserRemoved(intExtra5);
                    NotificationManagerService.this.mListeners.onUserRemoved(intExtra5);
                    NotificationManagerService.this.mConditionProviders.onUserRemoved(intExtra5);
                    NotificationManagerService.this.mAssistants.onUserRemoved(intExtra5);
                    NotificationManagerService.this.savePolicyFile();
                } else if (action.equals("android.intent.action.USER_UNLOCKED")) {
                    int intExtra6 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    NotificationManagerService.this.mConditionProviders.onUserUnlocked(intExtra6);
                    NotificationManagerService.this.mListeners.onUserUnlocked(intExtra6);
                    NotificationManagerService.this.mAssistants.onUserUnlocked(intExtra6);
                    NotificationManagerService.this.mZenModeHelper.onUserUnlocked(intExtra6);
                }
                if (NotificationManagerService.this.mMtkPplManager.filterPplAction(action)) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                }
            }
        };
        this.mService = new INotificationManager.Stub() {
            public void enqueueToast(String str, ITransientNotification iTransientNotification, int i) {
                int iIndexOfToastLocked;
                String str2;
                if (NotificationManagerService.DBG) {
                    Slog.i(NotificationManagerService.TAG, "enqueueToast pkg=" + str + " callback=" + iTransientNotification + " duration=" + i);
                }
                if (str == null || iTransientNotification == null) {
                    Slog.e(NotificationManagerService.TAG, "Not doing toast. pkg=" + str + " callback=" + iTransientNotification);
                    return;
                }
                boolean z = NotificationManagerService.this.isCallerSystemOrPhone() || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str);
                boolean zIsPackageSuspendedForUser = NotificationManagerService.this.isPackageSuspendedForUser(str, Binder.getCallingUid());
                if (!z && (!areNotificationsEnabledForPackage(str, Binder.getCallingUid()) || zIsPackageSuspendedForUser)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Suppressing toast from package ");
                    sb.append(str);
                    if (zIsPackageSuspendedForUser) {
                        str2 = " due to package suspended by administrator.";
                    } else {
                        str2 = " by user request.";
                    }
                    sb.append(str2);
                    Slog.e(NotificationManagerService.TAG, sb.toString());
                    return;
                }
                synchronized (NotificationManagerService.this.mToastQueue) {
                    int callingPid = Binder.getCallingPid();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (!z) {
                            iIndexOfToastLocked = NotificationManagerService.this.indexOfToastPackageLocked(str);
                        } else {
                            iIndexOfToastLocked = NotificationManagerService.this.indexOfToastLocked(str, iTransientNotification);
                        }
                        if (iIndexOfToastLocked >= 0) {
                            ToastRecord toastRecord = NotificationManagerService.this.mToastQueue.get(iIndexOfToastLocked);
                            toastRecord.update(i);
                            try {
                                toastRecord.callback.hide();
                            } catch (RemoteException e) {
                            }
                            toastRecord.update(iTransientNotification);
                        } else {
                            Binder binder = new Binder();
                            NotificationManagerService.this.mWindowManagerInternal.addWindowToken(binder, 2005, 0);
                            NotificationManagerService.this.mToastQueue.add(new ToastRecord(callingPid, str, iTransientNotification, i, binder));
                            iIndexOfToastLocked = NotificationManagerService.this.mToastQueue.size() - 1;
                        }
                        NotificationManagerService.this.keepProcessAliveIfNeededLocked(callingPid);
                        if (iIndexOfToastLocked == 0) {
                            NotificationManagerService.this.showNextToastLocked();
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }

            public void cancelToast(String str, ITransientNotification iTransientNotification) {
                Slog.i(NotificationManagerService.TAG, "cancelToast pkg=" + str + " callback=" + iTransientNotification);
                if (str == null || iTransientNotification == null) {
                    Slog.e(NotificationManagerService.TAG, "Not cancelling notification. pkg=" + str + " callback=" + iTransientNotification);
                    return;
                }
                synchronized (NotificationManagerService.this.mToastQueue) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        int iIndexOfToastLocked = NotificationManagerService.this.indexOfToastLocked(str, iTransientNotification);
                        if (iIndexOfToastLocked >= 0) {
                            NotificationManagerService.this.cancelToastLocked(iIndexOfToastLocked);
                        } else {
                            Slog.w(NotificationManagerService.TAG, "Toast already cancelled. pkg=" + str + " callback=" + iTransientNotification);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }

            public void finishToken(String str, ITransientNotification iTransientNotification) {
                synchronized (NotificationManagerService.this.mToastQueue) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        int iIndexOfToastLocked = NotificationManagerService.this.indexOfToastLocked(str, iTransientNotification);
                        if (iIndexOfToastLocked >= 0) {
                            NotificationManagerService.this.finishTokenLocked(NotificationManagerService.this.mToastQueue.get(iIndexOfToastLocked).token);
                        } else {
                            Slog.w(NotificationManagerService.TAG, "Toast already killed. pkg=" + str + " callback=" + iTransientNotification);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }

            public void enqueueNotificationWithTag(String str, String str2, String str3, int i, Notification notification, int i2) throws RemoteException {
                NotificationManagerService.this.enqueueNotificationInternal(str, str2, Binder.getCallingUid(), Binder.getCallingPid(), str3, i, notification, i2);
            }

            public void cancelNotificationWithTag(String str, String str2, int i, int i2) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                NotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), str, str2, i, 0, NotificationManagerService.this.isCallingUidSystem() ? 0 : 1088, false, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, true, false, "cancelNotificationWithTag", str), 8, null);
            }

            public void cancelAllNotifications(String str, int i) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                NotificationManagerService.this.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), str, null, 0, 64, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, false, "cancelAllNotifications", str), 9, null);
            }

            public void setNotificationsEnabledForPackage(String str, int i, boolean z) {
                enforceSystemOrSystemUI("setNotificationsEnabledForPackage");
                NotificationManagerService.this.mRankingHelper.setEnabled(str, i, z);
                if (!z) {
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, str, null, 0, 0, true, UserHandle.getUserId(i), 7, null);
                }
                try {
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.APP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.BLOCKED_STATE", !z).addFlags(268435456).setPackage(str), UserHandle.of(UserHandle.getUserId(i)), null);
                } catch (SecurityException e) {
                    Slog.w(NotificationManagerService.TAG, "Can't notify app about app block change", e);
                }
                NotificationManagerService.this.savePolicyFile();
            }

            public void setNotificationsEnabledWithImportanceLockForPackage(String str, int i, boolean z) {
                setNotificationsEnabledForPackage(str, i, z);
                NotificationManagerService.this.mRankingHelper.setAppImportanceLocked(str, i);
            }

            public boolean areNotificationsEnabled(String str) {
                return areNotificationsEnabledForPackage(str, Binder.getCallingUid());
            }

            public boolean areNotificationsEnabledForPackage(String str, int i) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                if (UserHandle.getCallingUserId() != UserHandle.getUserId(i)) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "canNotifyAsPackage for uid " + i);
                }
                return NotificationManagerService.this.mRankingHelper.getImportance(str, i) != 0;
            }

            public int getPackageImportance(String str) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                return NotificationManagerService.this.mRankingHelper.getImportance(str, Binder.getCallingUid());
            }

            public boolean canShowBadge(String str, int i) {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.canShowBadge(str, i);
            }

            public void setShowBadge(String str, int i, boolean z) {
                NotificationManagerService.this.checkCallerIsSystem();
                NotificationManagerService.this.mRankingHelper.setShowBadge(str, i, z);
                NotificationManagerService.this.savePolicyFile();
            }

            public void updateNotificationChannelGroupForPackage(String str, int i, NotificationChannelGroup notificationChannelGroup) throws RemoteException {
                enforceSystemOrSystemUI("Caller not system or systemui");
                NotificationManagerService.this.createNotificationChannelGroup(str, i, notificationChannelGroup, false, false);
                NotificationManagerService.this.savePolicyFile();
            }

            public void createNotificationChannelGroups(String str, ParceledListSlice parceledListSlice) throws RemoteException {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                List list = parceledListSlice.getList();
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    NotificationManagerService.this.createNotificationChannelGroup(str, Binder.getCallingUid(), (NotificationChannelGroup) list.get(i), true, false);
                }
                NotificationManagerService.this.savePolicyFile();
            }

            private void createNotificationChannelsImpl(String str, int i, ParceledListSlice parceledListSlice) {
                List list = parceledListSlice.getList();
                int size = list.size();
                for (int i2 = 0; i2 < size; i2++) {
                    NotificationChannel notificationChannel = (NotificationChannel) list.get(i2);
                    Preconditions.checkNotNull(notificationChannel, "channel in list is null");
                    NotificationManagerService.this.mRankingHelper.createNotificationChannel(str, i, notificationChannel, true, NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(str, UserHandle.getUserId(i)));
                    NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(i), NotificationManagerService.this.mRankingHelper.getNotificationChannel(str, i, notificationChannel.getId(), false), 1);
                }
                NotificationManagerService.this.savePolicyFile();
            }

            public void createNotificationChannels(String str, ParceledListSlice parceledListSlice) throws RemoteException {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                createNotificationChannelsImpl(str, Binder.getCallingUid(), parceledListSlice);
            }

            public void createNotificationChannelsForPackage(String str, int i, ParceledListSlice parceledListSlice) throws RemoteException {
                NotificationManagerService.this.checkCallerIsSystem();
                createNotificationChannelsImpl(str, i, parceledListSlice);
            }

            public NotificationChannel getNotificationChannel(String str, String str2) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                return NotificationManagerService.this.mRankingHelper.getNotificationChannel(str, Binder.getCallingUid(), str2, false);
            }

            public NotificationChannel getNotificationChannelForPackage(String str, int i, String str2, boolean z) {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getNotificationChannel(str, i, str2, z);
            }

            private void enforceDeletingChannelHasNoFgService(String str, int i, String str2) {
                if (NotificationManagerService.this.mAmi.hasForegroundServiceNotification(str, i, str2)) {
                    Slog.w(NotificationManagerService.TAG, "Package u" + i + SliceClientPermissions.SliceAuthority.DELIMITER + str + " may not delete notification channel '" + str2 + "' with fg service");
                    throw new SecurityException("Not allowed to delete channel " + str2 + " with a foreground service");
                }
            }

            public void deleteNotificationChannel(String str, String str2) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                int callingUid = Binder.getCallingUid();
                int userId = UserHandle.getUserId(callingUid);
                if ("miscellaneous".equals(str2)) {
                    throw new IllegalArgumentException("Cannot delete default channel");
                }
                enforceDeletingChannelHasNoFgService(str, userId, str2);
                NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, str, str2, 0, 0, true, userId, 17, null);
                NotificationManagerService.this.mRankingHelper.deleteNotificationChannel(str, callingUid, str2);
                NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(callingUid), NotificationManagerService.this.mRankingHelper.getNotificationChannel(str, callingUid, str2, true), 3);
                NotificationManagerService.this.savePolicyFile();
            }

            public NotificationChannelGroup getNotificationChannelGroup(String str, String str2) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroupWithChannels(str, Binder.getCallingUid(), str2, false);
            }

            public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String str) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(str, Binder.getCallingUid(), false, false, true);
            }

            public void deleteNotificationChannelGroup(String str, String str2) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                int callingUid = Binder.getCallingUid();
                NotificationChannelGroup notificationChannelGroup = NotificationManagerService.this.mRankingHelper.getNotificationChannelGroup(str2, str, callingUid);
                if (notificationChannelGroup != null) {
                    int userId = UserHandle.getUserId(callingUid);
                    List<NotificationChannel> channels = notificationChannelGroup.getChannels();
                    for (int i = 0; i < channels.size(); i++) {
                        enforceDeletingChannelHasNoFgService(str, userId, channels.get(i).getId());
                    }
                    int i2 = 0;
                    for (List<NotificationChannel> listDeleteNotificationChannelGroup = NotificationManagerService.this.mRankingHelper.deleteNotificationChannelGroup(str, callingUid, str2); i2 < listDeleteNotificationChannelGroup.size(); listDeleteNotificationChannelGroup = listDeleteNotificationChannelGroup) {
                        NotificationChannel notificationChannel = listDeleteNotificationChannelGroup.get(i2);
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, str, notificationChannel.getId(), 0, 0, true, userId, 17, null);
                        NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(callingUid), notificationChannel, 3);
                        i2++;
                    }
                    NotificationManagerService.this.mListeners.notifyNotificationChannelGroupChanged(str, UserHandle.getUserHandleForUid(callingUid), notificationChannelGroup, 3);
                    NotificationManagerService.this.savePolicyFile();
                }
            }

            public void updateNotificationChannelForPackage(String str, int i, NotificationChannel notificationChannel) {
                enforceSystemOrSystemUI("Caller not system or systemui");
                Preconditions.checkNotNull(notificationChannel);
                NotificationManagerService.this.updateNotificationChannelInt(str, i, notificationChannel, false);
            }

            public ParceledListSlice<NotificationChannel> getNotificationChannelsForPackage(String str, int i, boolean z) {
                enforceSystemOrSystemUI("getNotificationChannelsForPackage");
                return NotificationManagerService.this.mRankingHelper.getNotificationChannels(str, i, z);
            }

            public int getNumNotificationChannelsForPackage(String str, int i, boolean z) {
                enforceSystemOrSystemUI("getNumNotificationChannelsForPackage");
                return NotificationManagerService.this.mRankingHelper.getNotificationChannels(str, i, z).getList().size();
            }

            public boolean onlyHasDefaultChannel(String str, int i) {
                enforceSystemOrSystemUI("onlyHasDefaultChannel");
                return NotificationManagerService.this.mRankingHelper.onlyHasDefaultChannel(str, i);
            }

            public int getDeletedChannelCount(String str, int i) {
                enforceSystemOrSystemUI("getDeletedChannelCount");
                return NotificationManagerService.this.mRankingHelper.getDeletedChannelCount(str, i);
            }

            public int getBlockedChannelCount(String str, int i) {
                enforceSystemOrSystemUI("getBlockedChannelCount");
                return NotificationManagerService.this.mRankingHelper.getBlockedChannelCount(str, i);
            }

            public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsForPackage(String str, int i, boolean z) {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(str, i, z, true, false);
            }

            public NotificationChannelGroup getPopulatedNotificationChannelGroupForPackage(String str, int i, String str2, boolean z) {
                enforceSystemOrSystemUI("getPopulatedNotificationChannelGroupForPackage");
                return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroupWithChannels(str, i, str2, z);
            }

            public NotificationChannelGroup getNotificationChannelGroupForPackage(String str, String str2, int i) {
                enforceSystemOrSystemUI("getNotificationChannelGroupForPackage");
                return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroup(str, str2, i);
            }

            public ParceledListSlice<NotificationChannel> getNotificationChannels(String str) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                return NotificationManagerService.this.mRankingHelper.getNotificationChannels(str, Binder.getCallingUid(), false);
            }

            public ParceledListSlice<NotifyingApp> getRecentNotifyingAppsForUser(int i) {
                ParceledListSlice<NotifyingApp> parceledListSlice;
                NotificationManagerService.this.checkCallerIsSystem();
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    parceledListSlice = new ParceledListSlice<>(new ArrayList(NotificationManagerService.this.mRecentApps.getOrDefault(Integer.valueOf(i), new ArrayList<>())));
                }
                return parceledListSlice;
            }

            public int getBlockedAppCount(int i) {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mRankingHelper.getBlockedAppCount(i);
            }

            public boolean areChannelsBypassingDnd() {
                return NotificationManagerService.this.mRankingHelper.areChannelsBypassingDnd();
            }

            public void clearData(String str, int i, boolean z) throws RemoteException {
                NotificationManagerService.this.checkCallerIsSystem();
                NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, str, null, 0, 0, true, UserHandle.getUserId(Binder.getCallingUid()), 17, null);
                String[] strArr = {str};
                int[] iArr = {i};
                NotificationManagerService.this.mListeners.onPackagesChanged(true, strArr, iArr);
                NotificationManagerService.this.mAssistants.onPackagesChanged(true, strArr, iArr);
                NotificationManagerService.this.mConditionProviders.onPackagesChanged(true, strArr, iArr);
                if (!z) {
                    NotificationManagerService.this.mRankingHelper.onPackagesChanged(true, UserHandle.getCallingUserId(), strArr, iArr);
                }
                NotificationManagerService.this.savePolicyFile();
            }

            public StatusBarNotification[] getActiveNotifications(String str) {
                StatusBarNotification[] statusBarNotificationArr;
                NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getActiveNotifications");
                if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, Binder.getCallingUid(), str) == 0) {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        statusBarNotificationArr = new StatusBarNotification[NotificationManagerService.this.mNotificationList.size()];
                        int size = NotificationManagerService.this.mNotificationList.size();
                        for (int i = 0; i < size; i++) {
                            statusBarNotificationArr[i] = NotificationManagerService.this.mNotificationList.get(i).sbn;
                        }
                    }
                    return statusBarNotificationArr;
                }
                return null;
            }

            public ParceledListSlice<StatusBarNotification> getAppActiveNotifications(String str, int i) {
                ParceledListSlice<StatusBarNotification> parceledListSlice;
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, false, "getAppActiveNotifications", str);
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ArrayMap arrayMap = new ArrayMap(NotificationManagerService.this.mNotificationList.size() + NotificationManagerService.this.mEnqueuedNotifications.size());
                    int size = NotificationManagerService.this.mNotificationList.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        StatusBarNotification statusBarNotificationSanitizeSbn = sanitizeSbn(str, iHandleIncomingUser, NotificationManagerService.this.mNotificationList.get(i2).sbn);
                        if (statusBarNotificationSanitizeSbn != null) {
                            arrayMap.put(statusBarNotificationSanitizeSbn.getKey(), statusBarNotificationSanitizeSbn);
                        }
                    }
                    Iterator<NotificationRecord> it = NotificationManagerService.this.mSnoozeHelper.getSnoozed(iHandleIncomingUser, str).iterator();
                    while (it.hasNext()) {
                        StatusBarNotification statusBarNotificationSanitizeSbn2 = sanitizeSbn(str, iHandleIncomingUser, it.next().sbn);
                        if (statusBarNotificationSanitizeSbn2 != null) {
                            arrayMap.put(statusBarNotificationSanitizeSbn2.getKey(), statusBarNotificationSanitizeSbn2);
                        }
                    }
                    int size2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                    for (int i3 = 0; i3 < size2; i3++) {
                        StatusBarNotification statusBarNotificationSanitizeSbn3 = sanitizeSbn(str, iHandleIncomingUser, NotificationManagerService.this.mEnqueuedNotifications.get(i3).sbn);
                        if (statusBarNotificationSanitizeSbn3 != null) {
                            arrayMap.put(statusBarNotificationSanitizeSbn3.getKey(), statusBarNotificationSanitizeSbn3);
                        }
                    }
                    ArrayList arrayList = new ArrayList(arrayMap.size());
                    arrayList.addAll(arrayMap.values());
                    parceledListSlice = new ParceledListSlice<>(arrayList);
                }
                return parceledListSlice;
            }

            private StatusBarNotification sanitizeSbn(String str, int i, StatusBarNotification statusBarNotification) {
                if (statusBarNotification.getPackageName().equals(str) && statusBarNotification.getUserId() == i) {
                    return new StatusBarNotification(statusBarNotification.getPackageName(), statusBarNotification.getOpPkg(), statusBarNotification.getId(), statusBarNotification.getTag(), statusBarNotification.getUid(), statusBarNotification.getInitialPid(), statusBarNotification.getNotification().clone(), statusBarNotification.getUser(), statusBarNotification.getOverrideGroupKey(), statusBarNotification.getPostTime());
                }
                return null;
            }

            public StatusBarNotification[] getHistoricalNotifications(String str, int i) {
                StatusBarNotification[] array;
                NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getHistoricalNotifications");
                if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, Binder.getCallingUid(), str) == 0) {
                    synchronized (NotificationManagerService.this.mArchive) {
                        array = NotificationManagerService.this.mArchive.getArray(i);
                    }
                    return array;
                }
                return null;
            }

            public void registerListener(INotificationListener iNotificationListener, ComponentName componentName, int i) {
                enforceSystemOrSystemUI("INotificationManager.registerListener");
                NotificationManagerService.this.mListeners.registerService(iNotificationListener, componentName, i);
            }

            public void unregisterListener(INotificationListener iNotificationListener, int i) {
                NotificationManagerService.this.mListeners.unregisterService((IInterface) iNotificationListener, i);
            }

            public void cancelNotificationsFromListener(INotificationListener iNotificationListener, String[] strArr) {
                int i;
                int i2;
                int callingUid = Binder.getCallingUid();
                int callingPid = Binder.getCallingPid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                        if (strArr != null) {
                            int length = strArr.length;
                            int i3 = 0;
                            while (i3 < length) {
                                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(strArr[i3]);
                                if (notificationRecord == null) {
                                    i = i3;
                                    i2 = length;
                                } else {
                                    int userId = notificationRecord.sbn.getUserId();
                                    if (userId != managedServiceInfoCheckServiceTokenLocked.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                        throw new SecurityException("Disallowed call from listener: " + managedServiceInfoCheckServiceTokenLocked.service);
                                    }
                                    i = i3;
                                    i2 = length;
                                    cancelNotificationFromListenerLocked(managedServiceInfoCheckServiceTokenLocked, callingUid, callingPid, notificationRecord.sbn.getPackageName(), notificationRecord.sbn.getTag(), notificationRecord.sbn.getId(), userId);
                                }
                                i3 = i + 1;
                                length = i2;
                            }
                        } else {
                            NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, managedServiceInfoCheckServiceTokenLocked.userid, 11, managedServiceInfoCheckServiceTokenLocked, managedServiceInfoCheckServiceTokenLocked.supportsProfiles());
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void requestBindListener(ComponentName componentName) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(componentName.getPackageName());
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    (NotificationManagerService.this.mAssistants.isComponentEnabledForCurrentProfiles(componentName) ? NotificationManagerService.this.mAssistants : NotificationManagerService.this.mListeners).setComponentState(componentName, true);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void requestUnbindListener(INotificationListener iNotificationListener) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                        managedServiceInfoCheckServiceTokenLocked.getOwner().setComponentState(managedServiceInfoCheckServiceTokenLocked.component, false);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void setNotificationsShownFromListener(INotificationListener iNotificationListener, String[] strArr) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                        if (strArr != null) {
                            int length = strArr.length;
                            for (int i = 0; i < length; i++) {
                                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(strArr[i]);
                                if (notificationRecord != null) {
                                    int userId = notificationRecord.sbn.getUserId();
                                    if (userId != managedServiceInfoCheckServiceTokenLocked.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                        throw new SecurityException("Disallowed call from listener: " + managedServiceInfoCheckServiceTokenLocked.service);
                                    }
                                    if (!notificationRecord.isSeen()) {
                                        if (NotificationManagerService.DBG) {
                                            Slog.d(NotificationManagerService.TAG, "Marking notification as seen " + strArr[i]);
                                        }
                                        NotificationManagerService.this.reportSeen(notificationRecord);
                                        notificationRecord.setSeen();
                                        NotificationManagerService.this.maybeRecordInterruptionLocked(notificationRecord);
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            @GuardedBy("mNotificationLock")
            private void cancelNotificationFromListenerLocked(ManagedServices.ManagedServiceInfo managedServiceInfo, int i, int i2, String str, String str2, int i3, int i4) {
                NotificationManagerService.this.cancelNotification(i, i2, str, str2, i3, 0, 66, true, i4, 10, managedServiceInfo);
            }

            public void snoozeNotificationUntilContextFromListener(INotificationListener iNotificationListener, String str, String str2) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.snoozeNotificationInt(str, -1L, str2, NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener));
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void snoozeNotificationUntilFromListener(INotificationListener iNotificationListener, String str, long j) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.snoozeNotificationInt(str, j, null, NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener));
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void unsnoozeNotificationFromAssistant(INotificationListener iNotificationListener, String str) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.unsnoozeNotificationInt(str, NotificationManagerService.this.mAssistants.checkServiceTokenLocked(iNotificationListener));
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void cancelNotificationFromListener(INotificationListener iNotificationListener, String str, String str2, int i) {
                int callingUid = Binder.getCallingUid();
                int callingPid = Binder.getCallingPid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                        if (managedServiceInfoCheckServiceTokenLocked.supportsProfiles()) {
                            Log.e(NotificationManagerService.TAG, "Ignoring deprecated cancelNotification(pkg, tag, id) from " + managedServiceInfoCheckServiceTokenLocked.component + " use cancelNotification(key) instead.");
                        } else {
                            cancelNotificationFromListenerLocked(managedServiceInfoCheckServiceTokenLocked, callingUid, callingPid, str, str2, i, managedServiceInfoCheckServiceTokenLocked.userid);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public ParceledListSlice<StatusBarNotification> getActiveNotificationsFromListener(INotificationListener iNotificationListener, String[] strArr, int i) {
                ParceledListSlice<StatusBarNotification> parceledListSlice;
                NotificationRecord notificationRecord;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                    boolean z = strArr != null;
                    int length = z ? strArr.length : NotificationManagerService.this.mNotificationList.size();
                    ArrayList arrayList = new ArrayList(length);
                    for (int i2 = 0; i2 < length; i2++) {
                        if (z) {
                            notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(strArr[i2]);
                        } else {
                            notificationRecord = NotificationManagerService.this.mNotificationList.get(i2);
                        }
                        if (notificationRecord != null) {
                            StatusBarNotification statusBarNotificationCloneLight = notificationRecord.sbn;
                            if (NotificationManagerService.this.isVisibleToListener(statusBarNotificationCloneLight, managedServiceInfoCheckServiceTokenLocked)) {
                                if (i != 0) {
                                    statusBarNotificationCloneLight = statusBarNotificationCloneLight.cloneLight();
                                }
                                arrayList.add(statusBarNotificationCloneLight);
                            }
                        }
                    }
                    parceledListSlice = new ParceledListSlice<>(arrayList);
                }
                return parceledListSlice;
            }

            public ParceledListSlice<StatusBarNotification> getSnoozedNotificationsFromListener(INotificationListener iNotificationListener, int i) {
                ParceledListSlice<StatusBarNotification> parceledListSlice;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                    List<NotificationRecord> snoozed = NotificationManagerService.this.mSnoozeHelper.getSnoozed();
                    int size = snoozed.size();
                    ArrayList arrayList = new ArrayList(size);
                    for (int i2 = 0; i2 < size; i2++) {
                        NotificationRecord notificationRecord = snoozed.get(i2);
                        if (notificationRecord != null) {
                            StatusBarNotification statusBarNotificationCloneLight = notificationRecord.sbn;
                            if (NotificationManagerService.this.isVisibleToListener(statusBarNotificationCloneLight, managedServiceInfoCheckServiceTokenLocked)) {
                                if (i != 0) {
                                    statusBarNotificationCloneLight = statusBarNotificationCloneLight.cloneLight();
                                }
                                arrayList.add(statusBarNotificationCloneLight);
                            }
                        }
                    }
                    parceledListSlice = new ParceledListSlice<>(arrayList);
                }
                return parceledListSlice;
            }

            public void requestHintsFromListener(INotificationListener iNotificationListener, int i) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                        if ((i & 7) != 0) {
                            NotificationManagerService.this.addDisabledHints(managedServiceInfoCheckServiceTokenLocked, i);
                        } else {
                            NotificationManagerService.this.removeDisabledHints(managedServiceInfoCheckServiceTokenLocked, i);
                        }
                        NotificationManagerService.this.updateListenerHintsLocked();
                        NotificationManagerService.this.updateEffectsSuppressorLocked();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public int getHintsFromListener(INotificationListener iNotificationListener) {
                int i;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    i = NotificationManagerService.this.mListenerHints;
                }
                return i;
            }

            public void requestInterruptionFilterFromListener(INotificationListener iNotificationListener, int i) throws RemoteException {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.mZenModeHelper.requestFromListener(NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener).component, i);
                        NotificationManagerService.this.updateInterruptionFilterLocked();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public int getInterruptionFilterFromListener(INotificationListener iNotificationListener) throws RemoteException {
                int i;
                synchronized (NotificationManagerService.this.mNotificationLight) {
                    i = NotificationManagerService.this.mInterruptionFilter;
                }
                return i;
            }

            public void setOnNotificationPostedTrimFromListener(INotificationListener iNotificationListener, int i) throws RemoteException {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                    if (managedServiceInfoCheckServiceTokenLocked == null) {
                        return;
                    }
                    NotificationManagerService.this.mListeners.setOnNotificationPostedTrimLocked(managedServiceInfoCheckServiceTokenLocked, i);
                }
            }

            public int getZenMode() {
                return NotificationManagerService.this.mZenModeHelper.getZenMode();
            }

            public ZenModeConfig getZenModeConfig() {
                enforceSystemOrSystemUI("INotificationManager.getZenModeConfig");
                return NotificationManagerService.this.mZenModeHelper.getConfig();
            }

            public void setZenMode(int i, Uri uri, String str) throws RemoteException {
                enforceSystemOrSystemUI("INotificationManager.setZenMode");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setManualZenMode(i, uri, null, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public List<ZenModeConfig.ZenRule> getZenRules() throws RemoteException {
                enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRules");
                return NotificationManagerService.this.mZenModeHelper.getZenRules();
            }

            public AutomaticZenRule getAutomaticZenRule(String str) throws RemoteException {
                Preconditions.checkNotNull(str, "Id is null");
                enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.getAutomaticZenRule(str);
            }

            public String addAutomaticZenRule(AutomaticZenRule automaticZenRule) throws RemoteException {
                Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
                Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
                Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
                Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
                enforcePolicyAccess(Binder.getCallingUid(), "addAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.addAutomaticZenRule(automaticZenRule, "addAutomaticZenRule");
            }

            public boolean updateAutomaticZenRule(String str, AutomaticZenRule automaticZenRule) throws RemoteException {
                Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
                Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
                Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
                Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
                enforcePolicyAccess(Binder.getCallingUid(), "updateAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.updateAutomaticZenRule(str, automaticZenRule, "updateAutomaticZenRule");
            }

            public boolean removeAutomaticZenRule(String str) throws RemoteException {
                Preconditions.checkNotNull(str, "Id is null");
                enforcePolicyAccess(Binder.getCallingUid(), "removeAutomaticZenRule");
                return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRule(str, "removeAutomaticZenRule");
            }

            public boolean removeAutomaticZenRules(String str) throws RemoteException {
                Preconditions.checkNotNull(str, "Package name is null");
                enforceSystemOrSystemUI("removeAutomaticZenRules");
                return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRules(str, "removeAutomaticZenRules");
            }

            public int getRuleInstanceCount(ComponentName componentName) throws RemoteException {
                Preconditions.checkNotNull(componentName, "Owner is null");
                enforceSystemOrSystemUI("getRuleInstanceCount");
                return NotificationManagerService.this.mZenModeHelper.getCurrentInstanceCount(componentName);
            }

            public void setInterruptionFilter(String str, int i) throws RemoteException {
                enforcePolicyAccess(str, "setInterruptionFilter");
                int iZenModeFromInterruptionFilter = NotificationManager.zenModeFromInterruptionFilter(i, -1);
                if (iZenModeFromInterruptionFilter == -1) {
                    throw new IllegalArgumentException("Invalid filter: " + i);
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setManualZenMode(iZenModeFromInterruptionFilter, null, str, "setInterruptionFilter");
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void notifyConditions(final String str, IConditionProvider iConditionProvider, final Condition[] conditionArr) {
                final ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceToken = NotificationManagerService.this.mConditionProviders.checkServiceToken(iConditionProvider);
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                NotificationManagerService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationManagerService.this.mConditionProviders.notifyConditions(str, managedServiceInfoCheckServiceToken, conditionArr);
                    }
                });
            }

            public void requestUnbindProvider(IConditionProvider iConditionProvider) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceToken = NotificationManagerService.this.mConditionProviders.checkServiceToken(iConditionProvider);
                    managedServiceInfoCheckServiceToken.getOwner().setComponentState(managedServiceInfoCheckServiceToken.component, false);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void requestBindProvider(ComponentName componentName) {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(componentName.getPackageName());
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mConditionProviders.setComponentState(componentName, true);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            private void enforceSystemOrSystemUI(String str) {
                if (NotificationManagerService.this.isCallerSystemOrPhone()) {
                    return;
                }
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", str);
            }

            private void enforceSystemOrSystemUIOrSamePackage(String str, String str2) {
                try {
                    NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
                } catch (SecurityException e) {
                    NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", str2);
                }
            }

            private void enforcePolicyAccess(int i, String str) {
                if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
                    return;
                }
                boolean z = false;
                for (String str2 : NotificationManagerService.this.getContext().getPackageManager().getPackagesForUid(i)) {
                    if (NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(str2, UserHandle.getUserId(i))) {
                        z = true;
                    }
                }
                if (!z) {
                    Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + str);
                    throw new SecurityException("Notification policy access denied");
                }
            }

            private void enforcePolicyAccess(String str, String str2) {
                if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") != 0) {
                    NotificationManagerService.this.checkCallerIsSameApp(str);
                    if (!checkPolicyAccess(str)) {
                        Slog.w(NotificationManagerService.TAG, "Notification policy access denied calling " + str2);
                        throw new SecurityException("Notification policy access denied");
                    }
                }
            }

            private boolean checkPackagePolicyAccess(String str) {
                return NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(str, getCallingUserHandle().getIdentifier());
            }

            private boolean checkPolicyAccess(String str) {
                try {
                    if (ActivityManager.checkComponentPermission("android.permission.MANAGE_NOTIFICATIONS", NotificationManagerService.this.getContext().getPackageManager().getPackageUidAsUser(str, UserHandle.getCallingUserId()), -1, true) == 0) {
                        return true;
                    }
                    return checkPackagePolicyAccess(str) || NotificationManagerService.this.mListeners.isComponentEnabledForPackage(str) || (NotificationManagerService.this.mDpm != null && NotificationManagerService.this.mDpm.isActiveAdminWithPolicy(Binder.getCallingUid(), -1));
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }

            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                if (DumpUtils.checkDumpAndUsageStatsPermission(NotificationManagerService.this.getContext(), NotificationManagerService.TAG, printWriter)) {
                    DumpFilter fromArguments = DumpFilter.parseFromArguments(strArr);
                    if (fromArguments.stats) {
                        NotificationManagerService.this.dumpJson(printWriter, fromArguments);
                        return;
                    }
                    if (fromArguments.proto) {
                        NotificationManagerService.this.dumpProto(fileDescriptor, fromArguments);
                    } else if (fromArguments.criticalPriority) {
                        NotificationManagerService.this.dumpNotificationRecords(printWriter, fromArguments);
                    } else {
                        NotificationManagerService.this.dumpImpl(printWriter, fromArguments);
                    }
                }
            }

            public ComponentName getEffectsSuppressor() {
                if (NotificationManagerService.this.mEffectsSuppressors.isEmpty()) {
                    return null;
                }
                return (ComponentName) NotificationManagerService.this.mEffectsSuppressors.get(0);
            }

            public boolean matchesCallFilter(Bundle bundle) {
                enforceSystemOrSystemUI("INotificationManager.matchesCallFilter");
                return NotificationManagerService.this.mZenModeHelper.matchesCallFilter(Binder.getCallingUserHandle(), bundle, (ValidateNotificationPeople) NotificationManagerService.this.mRankingHelper.findExtractor(ValidateNotificationPeople.class), NotificationManagerService.MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS, 1.0f);
            }

            public boolean isSystemConditionProviderEnabled(String str) {
                enforceSystemOrSystemUI("INotificationManager.isSystemConditionProviderEnabled");
                return NotificationManagerService.this.mConditionProviders.isSystemProviderEnabled(str);
            }

            public byte[] getBackupPayload(int i) {
                byte[] byteArray;
                NotificationManagerService.this.checkCallerIsSystem();
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "getBackupPayload u=" + i);
                }
                if (i == 0) {
                    synchronized (NotificationManagerService.this.mPolicyFile) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            NotificationManagerService.this.writePolicyXml(byteArrayOutputStream, true);
                            byteArray = byteArrayOutputStream.toByteArray();
                        } catch (IOException e) {
                            Slog.w(NotificationManagerService.TAG, "getBackupPayload: error writing payload for user " + i, e);
                            return null;
                        }
                    }
                    return byteArray;
                }
                Slog.w(NotificationManagerService.TAG, "getBackupPayload: cannot backup policy for user " + i);
                return null;
            }

            public void applyRestore(byte[] bArr, int i) {
                NotificationManagerService.this.checkCallerIsSystem();
                if (NotificationManagerService.DBG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("applyRestore u=");
                    sb.append(i);
                    sb.append(" payload=");
                    sb.append(bArr != null ? new String(bArr, StandardCharsets.UTF_8) : null);
                    Slog.d(NotificationManagerService.TAG, sb.toString());
                }
                if (bArr == null) {
                    Slog.w(NotificationManagerService.TAG, "applyRestore: no payload to restore for user " + i);
                    return;
                }
                if (i == 0) {
                    synchronized (NotificationManagerService.this.mPolicyFile) {
                        try {
                            NotificationManagerService.this.readPolicyXml(new ByteArrayInputStream(bArr), true);
                            NotificationManagerService.this.savePolicyFile();
                        } catch (IOException | NumberFormatException | XmlPullParserException e) {
                            Slog.w(NotificationManagerService.TAG, "applyRestore: error reading payload", e);
                        }
                    }
                    return;
                }
                Slog.w(NotificationManagerService.TAG, "applyRestore: cannot restore policy for user " + i);
            }

            public boolean isNotificationPolicyAccessGranted(String str) {
                return checkPolicyAccess(str);
            }

            public boolean isNotificationPolicyAccessGrantedForPackage(String str) {
                enforceSystemOrSystemUIOrSamePackage(str, "request policy access status for another package");
                return checkPolicyAccess(str);
            }

            public void setNotificationPolicyAccessGranted(String str, boolean z) throws RemoteException {
                setNotificationPolicyAccessGrantedForUser(str, getCallingUserHandle().getIdentifier(), z);
            }

            public void setNotificationPolicyAccessGrantedForUser(String str, int i, boolean z) {
                NotificationManagerService.this.checkCallerIsSystemOrShell();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (NotificationManagerService.this.mAllowedManagedServicePackages.test(str)) {
                        NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(str, i, true, z);
                        NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(str).addFlags(1073741824), UserHandle.of(i), null);
                        NotificationManagerService.this.savePolicyFile();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public NotificationManager.Policy getNotificationPolicy(String str) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void setNotificationPolicy(String str, NotificationManager.Policy policy) {
                enforcePolicyAccess(str, "setNotificationPolicy");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    ApplicationInfo applicationInfo = NotificationManagerService.this.mPackageManager.getApplicationInfo(str, 0, UserHandle.getUserId(NotificationManagerService.MY_UID));
                    NotificationManager.Policy notificationPolicy = NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                    if (applicationInfo.targetSdkVersion < 28) {
                        policy = new NotificationManager.Policy((policy.priorityCategories & (-33) & (-65) & (-129)) | (notificationPolicy.priorityCategories & 32) | (notificationPolicy.priorityCategories & 64) | (notificationPolicy.priorityCategories & 128), policy.priorityCallSenders, policy.priorityMessageSenders, policy.suppressedVisualEffects);
                    }
                    NotificationManager.Policy policy2 = new NotificationManager.Policy(policy.priorityCategories, policy.priorityCallSenders, policy.priorityMessageSenders, NotificationManagerService.this.calculateSuppressedVisualEffects(policy, notificationPolicy, applicationInfo.targetSdkVersion));
                    ZenLog.traceSetNotificationPolicy(str, applicationInfo.targetSdkVersion, policy2);
                    NotificationManagerService.this.mZenModeHelper.setNotificationPolicy(policy2);
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }

            public List<String> getEnabledNotificationListenerPackages() {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mListeners.getAllowedPackages(getCallingUserHandle().getIdentifier());
            }

            public List<ComponentName> getEnabledNotificationListeners(int i) {
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mListeners.getAllowedComponents(i);
            }

            public boolean isNotificationListenerAccessGranted(ComponentName componentName) {
                Preconditions.checkNotNull(componentName);
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(componentName.getPackageName());
                return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(componentName.flattenToString(), getCallingUserHandle().getIdentifier());
            }

            public boolean isNotificationListenerAccessGrantedForUser(ComponentName componentName, int i) {
                Preconditions.checkNotNull(componentName);
                NotificationManagerService.this.checkCallerIsSystem();
                return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(componentName.flattenToString(), i);
            }

            public boolean isNotificationAssistantAccessGranted(ComponentName componentName) {
                Preconditions.checkNotNull(componentName);
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(componentName.getPackageName());
                return NotificationManagerService.this.mAssistants.isPackageOrComponentAllowed(componentName.flattenToString(), getCallingUserHandle().getIdentifier());
            }

            public void setNotificationListenerAccessGranted(ComponentName componentName, boolean z) throws RemoteException {
                setNotificationListenerAccessGrantedForUser(componentName, getCallingUserHandle().getIdentifier(), z);
            }

            public void setNotificationAssistantAccessGranted(ComponentName componentName, boolean z) throws RemoteException {
                setNotificationAssistantAccessGrantedForUser(componentName, getCallingUserHandle().getIdentifier(), z);
            }

            public void setNotificationListenerAccessGrantedForUser(ComponentName componentName, int i, boolean z) throws RemoteException {
                Preconditions.checkNotNull(componentName);
                NotificationManagerService.this.checkCallerIsSystemOrShell();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (NotificationManagerService.this.mAllowedManagedServicePackages.test(componentName.getPackageName())) {
                        NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(componentName.flattenToString(), i, false, z);
                        NotificationManagerService.this.mListeners.setPackageOrComponentEnabled(componentName.flattenToString(), i, true, z);
                        NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(componentName.getPackageName()).addFlags(1073741824), UserHandle.of(i), null);
                        NotificationManagerService.this.savePolicyFile();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void setNotificationAssistantAccessGrantedForUser(ComponentName componentName, int i, boolean z) throws RemoteException {
                Preconditions.checkNotNull(componentName);
                NotificationManagerService.this.checkCallerIsSystemOrShell();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (NotificationManagerService.this.mAllowedManagedServicePackages.test(componentName.getPackageName())) {
                        NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(componentName.flattenToString(), i, false, z);
                        NotificationManagerService.this.mAssistants.setPackageOrComponentEnabled(componentName.flattenToString(), i, true, z);
                        NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(componentName.getPackageName()).addFlags(1073741824), UserHandle.of(i), null);
                        NotificationManagerService.this.savePolicyFile();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void applyEnqueuedAdjustmentFromAssistant(INotificationListener iNotificationListener, Adjustment adjustment) throws RemoteException {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.mAssistants.checkServiceTokenLocked(iNotificationListener);
                        int size = NotificationManagerService.this.mEnqueuedNotifications.size();
                        int i = 0;
                        while (true) {
                            if (i >= size) {
                                break;
                            }
                            NotificationRecord notificationRecord = NotificationManagerService.this.mEnqueuedNotifications.get(i);
                            if (Objects.equals(adjustment.getKey(), notificationRecord.getKey()) && Objects.equals(Integer.valueOf(adjustment.getUser()), Integer.valueOf(notificationRecord.getUserId()))) {
                                NotificationManagerService.this.applyAdjustment(notificationRecord, adjustment);
                                break;
                            }
                            i++;
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void applyAdjustmentFromAssistant(INotificationListener iNotificationListener, Adjustment adjustment) throws RemoteException {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.mAssistants.checkServiceTokenLocked(iNotificationListener);
                        NotificationManagerService.this.applyAdjustment(NotificationManagerService.this.mNotificationsByKey.get(adjustment.getKey()), adjustment);
                    }
                    NotificationManagerService.this.mRankingHandler.requestSort();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void applyAdjustmentsFromAssistant(INotificationListener iNotificationListener, List<Adjustment> list) throws RemoteException {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        NotificationManagerService.this.mAssistants.checkServiceTokenLocked(iNotificationListener);
                        for (Adjustment adjustment : list) {
                            NotificationManagerService.this.applyAdjustment(NotificationManagerService.this.mNotificationsByKey.get(adjustment.getKey()), adjustment);
                        }
                    }
                    NotificationManagerService.this.mRankingHandler.requestSort();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void updateNotificationChannelGroupFromPrivilegedListener(INotificationListener iNotificationListener, String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup) throws RemoteException {
                Preconditions.checkNotNull(userHandle);
                verifyPrivilegedListener(iNotificationListener, userHandle);
                NotificationManagerService.this.createNotificationChannelGroup(str, getUidForPackageAndUser(str, userHandle), notificationChannelGroup, false, true);
                NotificationManagerService.this.savePolicyFile();
            }

            public void updateNotificationChannelFromPrivilegedListener(INotificationListener iNotificationListener, String str, UserHandle userHandle, NotificationChannel notificationChannel) throws RemoteException {
                Preconditions.checkNotNull(notificationChannel);
                Preconditions.checkNotNull(str);
                Preconditions.checkNotNull(userHandle);
                verifyPrivilegedListener(iNotificationListener, userHandle);
                NotificationManagerService.this.updateNotificationChannelInt(str, getUidForPackageAndUser(str, userHandle), notificationChannel, true);
            }

            public ParceledListSlice<NotificationChannel> getNotificationChannelsFromPrivilegedListener(INotificationListener iNotificationListener, String str, UserHandle userHandle) throws RemoteException {
                Preconditions.checkNotNull(str);
                Preconditions.checkNotNull(userHandle);
                verifyPrivilegedListener(iNotificationListener, userHandle);
                return NotificationManagerService.this.mRankingHelper.getNotificationChannels(str, getUidForPackageAndUser(str, userHandle), false);
            }

            public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsFromPrivilegedListener(INotificationListener iNotificationListener, String str, UserHandle userHandle) throws RemoteException {
                Preconditions.checkNotNull(str);
                Preconditions.checkNotNull(userHandle);
                verifyPrivilegedListener(iNotificationListener, userHandle);
                ArrayList arrayList = new ArrayList();
                arrayList.addAll(NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(str, getUidForPackageAndUser(str, userHandle)));
                return new ParceledListSlice<>(arrayList);
            }

            private void verifyPrivilegedListener(INotificationListener iNotificationListener, UserHandle userHandle) {
                ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked;
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    managedServiceInfoCheckServiceTokenLocked = NotificationManagerService.this.mListeners.checkServiceTokenLocked(iNotificationListener);
                }
                if (!NotificationManagerService.this.hasCompanionDevice(managedServiceInfoCheckServiceTokenLocked)) {
                    throw new SecurityException(managedServiceInfoCheckServiceTokenLocked + " does not have access");
                }
                if (!managedServiceInfoCheckServiceTokenLocked.enabledAndUserMatches(userHandle.getIdentifier())) {
                    throw new SecurityException(managedServiceInfoCheckServiceTokenLocked + " does not have access");
                }
            }

            private int getUidForPackageAndUser(String str, UserHandle userHandle) throws RemoteException {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return NotificationManagerService.this.mPackageManager.getPackageUid(str, 0, userHandle.getIdentifier());
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
                new ShellCmd(NotificationManagerService.this, null).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            }
        };
        this.mInternalService = new NotificationManagerInternal() {
            @Override
            public NotificationChannel getNotificationChannel(String str, int i, String str2) {
                return NotificationManagerService.this.mRankingHelper.getNotificationChannel(str, i, str2, false);
            }

            @Override
            public void enqueueNotification(String str, String str2, int i, int i2, String str3, int i3, Notification notification, int i4) {
                NotificationManagerService.this.enqueueNotificationInternal(str, str2, i, i2, str3, i3, notification, i4);
            }

            @Override
            public void removeForegroundServiceFlagFromNotification(final String str, final int i, final int i2) {
                NotificationManagerService.this.checkCallerIsSystem();
                NotificationManagerService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (NotificationManagerService.this.mNotificationLock) {
                            removeForegroundServiceFlagByListLocked(NotificationManagerService.this.mEnqueuedNotifications, str, i, i2);
                            removeForegroundServiceFlagByListLocked(NotificationManagerService.this.mNotificationList, str, i, i2);
                        }
                    }
                });
            }

            @GuardedBy("mNotificationLock")
            private void removeForegroundServiceFlagByListLocked(ArrayList<NotificationRecord> arrayList, String str, int i, int i2) {
                NotificationRecord notificationRecordFindNotificationByListLocked = NotificationManagerService.this.findNotificationByListLocked(arrayList, str, null, i, i2);
                if (notificationRecordFindNotificationByListLocked == null) {
                    return;
                }
                notificationRecordFindNotificationByListLocked.sbn.getNotification().flags = notificationRecordFindNotificationByListLocked.mOriginalFlags & (-65);
                NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                NotificationManagerService.this.mListeners.notifyPostedLocked(notificationRecordFindNotificationByListLocked, notificationRecordFindNotificationByListLocked);
            }
        };
        Notification.processWhitelistToken = WHITELIST_TOKEN;
    }

    @VisibleForTesting
    void setAudioManager(AudioManager audioManager) {
        this.mAudioManager = audioManager;
    }

    @VisibleForTesting
    void setKeyguardManager(KeyguardManager keyguardManager) {
        this.mKeyguardManager = keyguardManager;
    }

    void setVibrator(Vibrator vibrator) {
        this.mVibrator = vibrator;
    }

    @VisibleForTesting
    void setLights(Light light) {
        this.mNotificationLight = light;
        this.mAttentionLight = light;
        this.mNotificationPulseEnabled = true;
    }

    @VisibleForTesting
    void setScreenOn(boolean z) {
        this.mScreenOn = z;
    }

    @VisibleForTesting
    int getNotificationRecordCount() {
        int size;
        synchronized (this.mNotificationLock) {
            size = this.mNotificationList.size() + this.mNotificationsByKey.size() + this.mSummaryByGroupKey.size() + this.mEnqueuedNotifications.size();
            for (NotificationRecord notificationRecord : this.mNotificationList) {
                if (this.mNotificationsByKey.containsKey(notificationRecord.getKey())) {
                    size--;
                }
                if (notificationRecord.sbn.isGroup() && notificationRecord.getNotification().isGroupSummary()) {
                    size--;
                }
            }
        }
        return size;
    }

    @VisibleForTesting
    void clearNotifications() {
        this.mEnqueuedNotifications.clear();
        this.mNotificationList.clear();
        this.mNotificationsByKey.clear();
        this.mSummaryByGroupKey.clear();
    }

    @VisibleForTesting
    void addNotification(NotificationRecord notificationRecord) {
        this.mNotificationList.add(notificationRecord);
        this.mNotificationsByKey.put(notificationRecord.sbn.getKey(), notificationRecord);
        if (notificationRecord.sbn.isGroup()) {
            this.mSummaryByGroupKey.put(notificationRecord.getGroupKey(), notificationRecord);
        }
    }

    @VisibleForTesting
    void addEnqueuedNotification(NotificationRecord notificationRecord) {
        this.mEnqueuedNotifications.add(notificationRecord);
    }

    @VisibleForTesting
    NotificationRecord getNotificationRecord(String str) {
        return this.mNotificationsByKey.get(str);
    }

    @VisibleForTesting
    void setSystemReady(boolean z) {
        this.mSystemReady = z;
    }

    @VisibleForTesting
    void setHandler(WorkerHandler workerHandler) {
        this.mHandler = workerHandler;
    }

    @VisibleForTesting
    void setFallbackVibrationPattern(long[] jArr) {
        this.mFallbackVibrationPattern = jArr;
    }

    @VisibleForTesting
    void setPackageManager(IPackageManager iPackageManager) {
        this.mPackageManager = iPackageManager;
    }

    @VisibleForTesting
    void setRankingHelper(RankingHelper rankingHelper) {
        this.mRankingHelper = rankingHelper;
    }

    @VisibleForTesting
    void setRankingHandler(RankingHandler rankingHandler) {
        this.mRankingHandler = rankingHandler;
    }

    @VisibleForTesting
    void setIsTelevision(boolean z) {
        this.mIsTelevision = z;
    }

    @VisibleForTesting
    void setUsageStats(NotificationUsageStats notificationUsageStats) {
        this.mUsageStats = notificationUsageStats;
    }

    @VisibleForTesting
    void setAccessibilityManager(AccessibilityManager accessibilityManager) {
        this.mAccessibilityManager = accessibilityManager;
    }

    @VisibleForTesting
    void init(Looper looper, IPackageManager iPackageManager, PackageManager packageManager, LightsManager lightsManager, NotificationListeners notificationListeners, NotificationAssistants notificationAssistants, ConditionProviders conditionProviders, ICompanionDeviceManager iCompanionDeviceManager, SnoozeHelper snoozeHelper, NotificationUsageStats notificationUsageStats, AtomicFile atomicFile, ActivityManager activityManager, GroupHelper groupHelper, IActivityManager iActivityManager, UsageStatsManagerInternal usageStatsManagerInternal, DevicePolicyManagerInternal devicePolicyManagerInternal, ActivityManagerInternal activityManagerInternal) {
        String[] stringArray;
        Resources resources = getContext().getResources();
        this.mMaxPackageEnqueueRate = Settings.Global.getFloat(getContext().getContentResolver(), "max_notification_enqueue_rate", 5.0f);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        this.mAm = iActivityManager;
        this.mPackageManager = iPackageManager;
        this.mPackageManagerClient = packageManager;
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        this.mAppUsageStats = usageStatsManagerInternal;
        this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
        this.mCompanionManager = iCompanionDeviceManager;
        this.mActivityManager = activityManager;
        this.mAmi = activityManagerInternal;
        this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mDpm = devicePolicyManagerInternal;
        this.mHandler = new WorkerHandler(looper);
        this.mRankingThread.start();
        try {
            stringArray = resources.getStringArray(R.array.config_convert_to_emergency_number_map);
        } catch (Resources.NotFoundException e) {
            stringArray = new String[0];
        }
        String[] strArr = stringArray;
        this.mUsageStats = notificationUsageStats;
        this.mMetricsLogger = new MetricsLogger();
        this.mRankingHandler = new RankingHandlerWorker(this.mRankingThread.getLooper());
        this.mConditionProviders = conditionProviders;
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
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
                NotificationManagerService.this.mRankingHandler.requestSort();
            }

            @Override
            void onPolicyChanged() {
                NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.NOTIFICATION_POLICY_CHANGED");
                NotificationManagerService.this.mRankingHandler.requestSort();
            }
        });
        this.mRankingHelper = new RankingHelper(getContext(), this.mPackageManagerClient, this.mRankingHandler, this.mZenModeHelper, this.mUsageStats, strArr);
        this.mSnoozeHelper = snoozeHelper;
        this.mGroupHelper = groupHelper;
        this.mListeners = notificationListeners;
        this.mAssistants = notificationAssistants;
        this.mAllowedManagedServicePackages = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return this.f$0.canUseManagedServices((String) obj);
            }
        };
        this.mPolicyFile = atomicFile;
        loadPolicyFile();
        this.mStatusBar = (StatusBarManagerInternal) getLocalService(StatusBarManagerInternal.class);
        if (this.mStatusBar != null) {
            this.mStatusBar.setNotificationDelegate(this.mNotificationDelegate);
        }
        this.mNotificationLight = lightsManager.getLight(4);
        this.mAttentionLight = lightsManager.getLight(5);
        this.mFallbackVibrationPattern = getLongArray(resources, R.array.config_concurrentDisplayDeviceStates, 17, DEFAULT_VIBRATE_PATTERN);
        this.mInCallNotificationUri = Uri.parse("file://" + resources.getString(R.string.alternate_eri_file));
        this.mInCallNotificationAudioAttributes = new AudioAttributes.Builder().setContentType(4).setUsage(2).build();
        this.mInCallNotificationVolume = resources.getFloat(R.dimen.alertDialog_material_bottom_margin);
        this.mUseAttentionLight = resources.getBoolean(R.^attr-private.pointerIconTopLeftDiagonalDoubleArrow);
        boolean z = true;
        if (Settings.Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mDisableNotificationEffects = true;
        }
        this.mZenModeHelper.initZenMode();
        this.mInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        this.mUserProfiles.updateCache(getContext());
        listenForCallState();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mArchive = new Archive(resources.getInteger(R.integer.config_deviceStateConcurrentRearDisplay));
        if (!this.mPackageManagerClient.hasSystemFeature("android.software.leanback") && !this.mPackageManagerClient.hasSystemFeature("android.hardware.type.television")) {
            z = false;
        }
        this.mIsTelevision = z;
    }

    @Override
    public void onStart() {
        init(Looper.myLooper(), AppGlobals.getPackageManager(), getContext().getPackageManager(), (LightsManager) getLocalService(LightsManager.class), new NotificationListeners(AppGlobals.getPackageManager()), new NotificationAssistants(getContext(), this.mNotificationLock, this.mUserProfiles, AppGlobals.getPackageManager()), new ConditionProviders(getContext(), this.mUserProfiles, AppGlobals.getPackageManager()), null, new SnoozeHelper(getContext(), new SnoozeHelper.Callback() {
            @Override
            public void repost(int i, NotificationRecord notificationRecord) {
                try {
                    if (NotificationManagerService.DBG) {
                        Slog.d(NotificationManagerService.TAG, "Reposting " + notificationRecord.getKey());
                    }
                    NotificationManagerService.this.enqueueNotificationInternal(notificationRecord.sbn.getPackageName(), notificationRecord.sbn.getOpPkg(), notificationRecord.sbn.getUid(), notificationRecord.sbn.getInitialPid(), notificationRecord.sbn.getTag(), notificationRecord.sbn.getId(), notificationRecord.sbn.getNotification(), i);
                } catch (Exception e) {
                    Slog.e(NotificationManagerService.TAG, "Cannot un-snooze notification", e);
                }
            }
        }, this.mUserProfiles), new NotificationUsageStats(getContext()), new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "notification_policy.xml"), TAG_NOTIFICATION_POLICY), (ActivityManager) getContext().getSystemService("activity"), getGroupHelper(), ActivityManager.getService(), (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class), (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class), (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        MtkPplManager mtkPplManager = this.mMtkPplManager;
        intentFilter.addAction(MtkPplManager.PPL_LOCK);
        MtkPplManager mtkPplManager2 = this.mMtkPplManager;
        intentFilter.addAction(MtkPplManager.PPL_UNLOCK);
        getContext().registerReceiver(this.mIntentReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addAction("android.intent.action.PACKAGE_RESTARTED");
        intentFilter2.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        intentFilter2.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, intentFilter2, null, null);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.PACKAGES_SUSPENDED");
        intentFilter3.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, intentFilter3, null, null);
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE"), null, null);
        IntentFilter intentFilter4 = new IntentFilter(ACTION_NOTIFICATION_TIMEOUT);
        intentFilter4.addDataScheme(SCHEME_TIMEOUT);
        getContext().registerReceiver(this.mNotificationTimeoutReceiver, intentFilter4);
        getContext().registerReceiver(this.mRestoreReceiver, new IntentFilter("android.os.action.SETTING_RESTORED"));
        getContext().registerReceiver(this.mLocaleChangeReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        publishBinderService("notification", this.mService, false, 5);
        publishLocalService(NotificationManagerInternal.class, this.mInternalService);
    }

    private GroupHelper getGroupHelper() {
        return new GroupHelper(new GroupHelper.Callback() {
            @Override
            public void addAutoGroup(String str) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.addAutogroupKeyLocked(str);
                }
            }

            @Override
            public void removeAutoGroup(String str) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.removeAutogroupKeyLocked(str);
                }
            }

            @Override
            public void addAutoGroupSummary(int i, String str, String str2) {
                NotificationManagerService.this.createAutoGroupSummary(i, str, str2);
            }

            @Override
            public void removeAutoGroupSummary(int i, String str) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.clearAutogroupSummaryLocked(i, str);
                }
            }
        });
    }

    private void sendRegisteredOnlyBroadcast(String str) {
        getContext().sendBroadcastAsUser(new Intent(str).addFlags(1073741824), UserHandle.ALL, null);
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            this.mSystemReady = true;
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            this.mAudioManagerInternal = (AudioManagerInternal) getLocalService(AudioManagerInternal.class);
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mKeyguardManager = (KeyguardManager) getContext().getSystemService(KeyguardManager.class);
            this.mZenModeHelper.onSystemReady();
            return;
        }
        if (i == 600) {
            this.mSettingsObserver.observe();
            this.mListeners.onBootPhaseAppsCanStart();
            this.mAssistants.onBootPhaseAppsCanStart();
            this.mConditionProviders.onBootPhaseAppsCanStart();
        }
    }

    @GuardedBy("mNotificationLock")
    private void updateListenerHintsLocked() {
        int iCalculateHints = calculateHints();
        if (iCalculateHints == this.mListenerHints) {
            return;
        }
        ZenLog.traceListenerHintsChanged(this.mListenerHints, iCalculateHints, this.mEffectsSuppressors.size());
        this.mListenerHints = iCalculateHints;
        scheduleListenerHintsChanged(iCalculateHints);
    }

    @GuardedBy("mNotificationLock")
    private void updateEffectsSuppressorLocked() {
        long jCalculateSuppressedEffects = calculateSuppressedEffects();
        if (jCalculateSuppressedEffects == this.mZenModeHelper.getSuppressedEffects()) {
            return;
        }
        ArrayList<ComponentName> suppressors = getSuppressors();
        ZenLog.traceEffectsSuppressorChanged(this.mEffectsSuppressors, suppressors, jCalculateSuppressedEffects);
        this.mEffectsSuppressors = suppressors;
        this.mZenModeHelper.setSuppressedEffects(jCalculateSuppressedEffects);
        sendRegisteredOnlyBroadcast("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
    }

    private void exitIdle() {
        try {
            if (this.mDeviceIdleController != null) {
                this.mDeviceIdleController.exitIdle("notification interaction");
            }
        } catch (RemoteException e) {
        }
    }

    private void updateNotificationChannelInt(String str, int i, NotificationChannel notificationChannel, boolean z) {
        if (notificationChannel.getImportance() == 0) {
            cancelAllNotificationsInt(MY_UID, MY_PID, str, notificationChannel.getId(), 0, 0, true, UserHandle.getUserId(i), 17, null);
            if (isUidSystemOrPhone(i)) {
                int[] currentProfileIds = this.mUserProfiles.getCurrentProfileIds();
                int length = currentProfileIds.length;
                int i2 = 0;
                while (i2 < length) {
                    cancelAllNotificationsInt(MY_UID, MY_PID, str, notificationChannel.getId(), 0, 0, true, currentProfileIds[i2], 17, null);
                    i2++;
                    length = length;
                    currentProfileIds = currentProfileIds;
                }
            }
        }
        NotificationChannel notificationChannel2 = this.mRankingHelper.getNotificationChannel(str, i, notificationChannel.getId(), true);
        this.mRankingHelper.updateNotificationChannel(str, i, notificationChannel, true);
        maybeNotifyChannelOwner(str, i, notificationChannel2, notificationChannel);
        if (!z) {
            this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(i), this.mRankingHelper.getNotificationChannel(str, i, notificationChannel.getId(), false), 2);
        }
        savePolicyFile();
    }

    private void maybeNotifyChannelOwner(String str, int i, NotificationChannel notificationChannel, NotificationChannel notificationChannel2) {
        try {
            if ((notificationChannel.getImportance() == 0 && notificationChannel2.getImportance() != 0) || (notificationChannel.getImportance() != 0 && notificationChannel2.getImportance() == 0)) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_ID", notificationChannel2.getId()).putExtra("android.app.extra.BLOCKED_STATE", notificationChannel2.getImportance() == 0).addFlags(268435456).setPackage(str), UserHandle.of(UserHandle.getUserId(i)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about channel change", e);
        }
    }

    private void createNotificationChannelGroup(String str, int i, NotificationChannelGroup notificationChannelGroup, boolean z, boolean z2) {
        Preconditions.checkNotNull(notificationChannelGroup);
        Preconditions.checkNotNull(str);
        NotificationChannelGroup notificationChannelGroup2 = this.mRankingHelper.getNotificationChannelGroup(notificationChannelGroup.getId(), str, i);
        this.mRankingHelper.createNotificationChannelGroup(str, i, notificationChannelGroup, z);
        if (!z) {
            maybeNotifyChannelGroupOwner(str, i, notificationChannelGroup2, notificationChannelGroup);
        }
        if (!z2) {
            this.mListeners.notifyNotificationChannelGroupChanged(str, UserHandle.of(UserHandle.getCallingUserId()), notificationChannelGroup, 1);
        }
    }

    private void maybeNotifyChannelGroupOwner(String str, int i, NotificationChannelGroup notificationChannelGroup, NotificationChannelGroup notificationChannelGroup2) {
        try {
            if (notificationChannelGroup.isBlocked() != notificationChannelGroup2.isBlocked()) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID", notificationChannelGroup2.getId()).putExtra("android.app.extra.BLOCKED_STATE", notificationChannelGroup2.isBlocked()).addFlags(268435456).setPackage(str), UserHandle.of(UserHandle.getUserId(i)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about group change", e);
        }
    }

    private ArrayList<ComponentName> getSuppressors() {
        ArrayList<ComponentName> arrayList = new ArrayList<>();
        for (int size = this.mListenersDisablingEffects.size() - 1; size >= 0; size--) {
            Iterator<ManagedServices.ManagedServiceInfo> it = this.mListenersDisablingEffects.valueAt(size).iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().component);
            }
        }
        return arrayList;
    }

    private boolean removeDisabledHints(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        return removeDisabledHints(managedServiceInfo, 0);
    }

    private boolean removeDisabledHints(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
        boolean z = false;
        for (int size = this.mListenersDisablingEffects.size() - 1; size >= 0; size--) {
            int iKeyAt = this.mListenersDisablingEffects.keyAt(size);
            ArraySet<ManagedServices.ManagedServiceInfo> arraySetValueAt = this.mListenersDisablingEffects.valueAt(size);
            if (i == 0 || (iKeyAt & i) == iKeyAt) {
                z = z || arraySetValueAt.remove(managedServiceInfo);
            }
        }
        return z;
    }

    private void addDisabledHints(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
        if ((i & 1) != 0) {
            addDisabledHint(managedServiceInfo, 1);
        }
        if ((i & 2) != 0) {
            addDisabledHint(managedServiceInfo, 2);
        }
        if ((i & 4) != 0) {
            addDisabledHint(managedServiceInfo, 4);
        }
    }

    private void addDisabledHint(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
        if (this.mListenersDisablingEffects.indexOfKey(i) < 0) {
            this.mListenersDisablingEffects.put(i, new ArraySet<>());
        }
        this.mListenersDisablingEffects.get(i).add(managedServiceInfo);
    }

    private int calculateHints() {
        int i = 0;
        for (int size = this.mListenersDisablingEffects.size() - 1; size >= 0; size--) {
            int iKeyAt = this.mListenersDisablingEffects.keyAt(size);
            if (!this.mListenersDisablingEffects.valueAt(size).isEmpty()) {
                i |= iKeyAt;
            }
        }
        return i;
    }

    private long calculateSuppressedEffects() {
        long j;
        int iCalculateHints = calculateHints();
        if ((iCalculateHints & 1) != 0) {
            j = 3;
        } else {
            j = 0;
        }
        if ((iCalculateHints & 2) != 0) {
            j |= 1;
        }
        if ((iCalculateHints & 4) != 0) {
            return j | 2;
        }
        return j;
    }

    @GuardedBy("mNotificationLock")
    private void updateInterruptionFilterLocked() {
        int zenModeListenerInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        if (zenModeListenerInterruptionFilter == this.mInterruptionFilter) {
            return;
        }
        this.mInterruptionFilter = zenModeListenerInterruptionFilter;
        scheduleInterruptionFilterChanged(zenModeListenerInterruptionFilter);
    }

    @VisibleForTesting
    INotificationManager getBinderService() {
        return INotificationManager.Stub.asInterface(this.mService);
    }

    @GuardedBy("mNotificationLock")
    protected void reportSeen(NotificationRecord notificationRecord) {
        this.mAppUsageStats.reportEvent(notificationRecord.sbn.getPackageName(), getRealUserId(notificationRecord.sbn.getUserId()), 10);
    }

    protected int calculateSuppressedVisualEffects(NotificationManager.Policy policy, NotificationManager.Policy policy2, int i) {
        if (policy.suppressedVisualEffects == -1) {
            return policy.suppressedVisualEffects;
        }
        int[] iArr = {4, 8, 16, 32, 64, 128, 256};
        int i2 = policy.suppressedVisualEffects;
        if (i < 28) {
            for (int i3 = 0; i3 < iArr.length; i3++) {
                i2 = (i2 & (~iArr[i3])) | (policy2.suppressedVisualEffects & iArr[i3]);
            }
            if ((i2 & 1) != 0) {
                i2 = i2 | 8 | 4;
            }
            if ((i2 & 2) != 0) {
                return i2 | 16;
            }
            return i2;
        }
        if ((i2 + (-2)) - 1 > 0) {
            int i4 = i2 & (-4);
            if ((i4 & 16) != 0) {
                i4 |= 2;
            }
            if ((i4 & 8) != 0 && (i4 & 4) != 0 && (i4 & 128) != 0) {
                return i4 | 1;
            }
            return i4;
        }
        if ((i2 & 1) != 0) {
            i2 = i2 | 8 | 4 | 128;
        }
        if ((i2 & 2) != 0) {
            return i2 | 16;
        }
        return i2;
    }

    @GuardedBy("mNotificationLock")
    protected void maybeRecordInterruptionLocked(NotificationRecord notificationRecord) {
        if (notificationRecord.isInterruptive() && !notificationRecord.hasRecordedInterruption()) {
            this.mAppUsageStats.reportInterruptiveNotification(notificationRecord.sbn.getPackageName(), notificationRecord.getChannel().getId(), getRealUserId(notificationRecord.sbn.getUserId()));
            logRecentLocked(notificationRecord);
            notificationRecord.setRecordedInterruption(true);
        }
    }

    protected void reportUserInteraction(NotificationRecord notificationRecord) {
        this.mAppUsageStats.reportEvent(notificationRecord.sbn.getPackageName(), getRealUserId(notificationRecord.sbn.getUserId()), 7);
    }

    private int getRealUserId(int i) {
        if (i == -1) {
            return 0;
        }
        return i;
    }

    @VisibleForTesting
    NotificationManagerInternal getInternalService() {
        return this.mInternalService;
    }

    private void applyAdjustment(NotificationRecord notificationRecord, Adjustment adjustment) {
        if (notificationRecord != null && adjustment.getSignals() != null) {
            Bundle.setDefusable(adjustment.getSignals(), true);
            notificationRecord.addAdjustment(adjustment);
        }
    }

    @GuardedBy("mNotificationLock")
    void addAutogroupKeyLocked(String str) {
        NotificationRecord notificationRecord = this.mNotificationsByKey.get(str);
        if (notificationRecord != null && notificationRecord.sbn.getOverrideGroupKey() == null) {
            addAutoGroupAdjustment(notificationRecord, "ranker_group");
            EventLogTags.writeNotificationAutogrouped(str);
            this.mRankingHandler.requestSort();
        }
    }

    @GuardedBy("mNotificationLock")
    void removeAutogroupKeyLocked(String str) {
        NotificationRecord notificationRecord = this.mNotificationsByKey.get(str);
        if (notificationRecord != null && notificationRecord.sbn.getOverrideGroupKey() != null) {
            addAutoGroupAdjustment(notificationRecord, null);
            EventLogTags.writeNotificationUnautogrouped(str);
            this.mRankingHandler.requestSort();
        }
    }

    private void addAutoGroupAdjustment(NotificationRecord notificationRecord, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("key_group_key", str);
        notificationRecord.addAdjustment(new Adjustment(notificationRecord.sbn.getPackageName(), notificationRecord.getKey(), bundle, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, notificationRecord.sbn.getUserId()));
    }

    @GuardedBy("mNotificationLock")
    private void clearAutogroupSummaryLocked(int i, String str) {
        NotificationRecord notificationRecordFindNotificationByKeyLocked;
        ArrayMap<String, String> arrayMap = this.mAutobundledSummaries.get(Integer.valueOf(i));
        if (arrayMap != null && arrayMap.containsKey(str) && (notificationRecordFindNotificationByKeyLocked = findNotificationByKeyLocked(arrayMap.remove(str))) != null) {
            cancelNotificationLocked(notificationRecordFindNotificationByKeyLocked, false, 16, removeFromNotificationListsLocked(notificationRecordFindNotificationByKeyLocked), null);
        }
    }

    @GuardedBy("mNotificationLock")
    private boolean hasAutoGroupSummaryLocked(StatusBarNotification statusBarNotification) {
        ArrayMap<String, String> arrayMap = this.mAutobundledSummaries.get(Integer.valueOf(statusBarNotification.getUserId()));
        return arrayMap != null && arrayMap.containsKey(statusBarNotification.getPackageName());
    }

    private void createAutoGroupSummary(int i, String str, String str2) {
        NotificationRecord notificationRecord;
        synchronized (this.mNotificationLock) {
            NotificationRecord notificationRecord2 = this.mNotificationsByKey.get(str2);
            if (notificationRecord2 == null) {
                return;
            }
            StatusBarNotification statusBarNotification = notificationRecord2.sbn;
            int identifier = statusBarNotification.getUser().getIdentifier();
            ArrayMap<String, String> arrayMap = this.mAutobundledSummaries.get(Integer.valueOf(identifier));
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
            }
            this.mAutobundledSummaries.put(Integer.valueOf(identifier), arrayMap);
            if (arrayMap.containsKey(str)) {
                notificationRecord = null;
            } else {
                ApplicationInfo applicationInfo = (ApplicationInfo) statusBarNotification.getNotification().extras.getParcelable("android.appInfo");
                Bundle bundle = new Bundle();
                bundle.putParcelable("android.appInfo", applicationInfo);
                Notification notificationBuild = new Notification.Builder(getContext(), notificationRecord2.getChannel().getId()).setSmallIcon(statusBarNotification.getNotification().getSmallIcon()).setGroupSummary(true).setGroupAlertBehavior(2).setGroup("ranker_group").setFlag(1024, true).setFlag(512, true).setColor(statusBarNotification.getNotification().color).setLocalOnly(true).build();
                notificationBuild.extras.putAll(bundle);
                Intent launchIntentForPackage = getContext().getPackageManager().getLaunchIntentForPackage(str);
                if (launchIntentForPackage != null) {
                    notificationBuild.contentIntent = PendingIntent.getActivityAsUser(getContext(), 0, launchIntentForPackage, 0, null, UserHandle.of(identifier));
                }
                StatusBarNotification statusBarNotification2 = new StatusBarNotification(statusBarNotification.getPackageName(), statusBarNotification.getOpPkg(), Integer.MAX_VALUE, "ranker_group", statusBarNotification.getUid(), statusBarNotification.getInitialPid(), notificationBuild, statusBarNotification.getUser(), "ranker_group", System.currentTimeMillis());
                NotificationRecord notificationRecord3 = new NotificationRecord(getContext(), statusBarNotification2, notificationRecord2.getChannel());
                notificationRecord3.setIsAppImportanceLocked(notificationRecord2.getIsAppImportanceLocked());
                arrayMap.put(str, statusBarNotification2.getKey());
                notificationRecord = notificationRecord3;
            }
            if (notificationRecord != null && checkDisqualifyingFeatures(identifier, MY_UID, notificationRecord.sbn.getId(), notificationRecord.sbn.getTag(), notificationRecord, true)) {
                this.mHandler.post(new EnqueueNotificationRunnable(identifier, notificationRecord));
            }
        }
    }

    private String disableNotificationEffects(NotificationRecord notificationRecord) {
        if (this.mDisableNotificationEffects) {
            return "booleanState";
        }
        if ((this.mListenerHints & 1) != 0) {
            return "listenerHints";
        }
        if (this.mCallState != 0 && !this.mZenModeHelper.isCall(notificationRecord)) {
            return "callState";
        }
        return null;
    }

    private void dumpJson(PrintWriter printWriter, DumpFilter dumpFilter) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("service", "Notification Manager");
            jSONObject.put("bans", this.mRankingHelper.dumpBansJson(dumpFilter));
            jSONObject.put("ranking", this.mRankingHelper.dumpJson(dumpFilter));
            jSONObject.put("stats", this.mUsageStats.dumpJson(dumpFilter));
            jSONObject.put("channels", this.mRankingHelper.dumpChannelsJson(dumpFilter));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        printWriter.println(jSONObject);
    }

    private void dumpProto(FileDescriptor fileDescriptor, DumpFilter dumpFilter) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mNotificationLock) {
            int size = this.mNotificationList.size();
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = this.mNotificationList.get(i);
                if (!dumpFilter.filtered || dumpFilter.matches(notificationRecord.sbn)) {
                    notificationRecord.dump(protoOutputStream, 2246267895809L, dumpFilter.redact, 1);
                }
            }
            int size2 = this.mEnqueuedNotifications.size();
            for (int i2 = 0; i2 < size2; i2++) {
                NotificationRecord notificationRecord2 = this.mEnqueuedNotifications.get(i2);
                if (!dumpFilter.filtered || dumpFilter.matches(notificationRecord2.sbn)) {
                    notificationRecord2.dump(protoOutputStream, 2246267895809L, dumpFilter.redact, 0);
                }
            }
            List<NotificationRecord> snoozed = this.mSnoozeHelper.getSnoozed();
            int size3 = snoozed.size();
            for (int i3 = 0; i3 < size3; i3++) {
                NotificationRecord notificationRecord3 = snoozed.get(i3);
                if (!dumpFilter.filtered || dumpFilter.matches(notificationRecord3.sbn)) {
                    notificationRecord3.dump(protoOutputStream, 2246267895809L, dumpFilter.redact, 2);
                }
            }
            long jStart = protoOutputStream.start(1146756268034L);
            this.mZenModeHelper.dump(protoOutputStream);
            Iterator<ComponentName> it = this.mEffectsSuppressors.iterator();
            while (it.hasNext()) {
                it.next().writeToProto(protoOutputStream, 2246267895812L);
            }
            protoOutputStream.end(jStart);
            long jStart2 = protoOutputStream.start(1146756268035L);
            this.mListeners.dump(protoOutputStream, dumpFilter);
            protoOutputStream.end(jStart2);
            protoOutputStream.write(1120986464260L, this.mListenerHints);
            for (int i4 = 0; i4 < this.mListenersDisablingEffects.size(); i4++) {
                long jStart3 = protoOutputStream.start(2246267895813L);
                protoOutputStream.write(1120986464257L, this.mListenersDisablingEffects.keyAt(i4));
                ArraySet<ManagedServices.ManagedServiceInfo> arraySetValueAt = this.mListenersDisablingEffects.valueAt(i4);
                for (int i5 = 0; i5 < arraySetValueAt.size(); i5++) {
                    arraySetValueAt.valueAt(i4).writeToProto(protoOutputStream, 2246267895810L, null);
                }
                protoOutputStream.end(jStart3);
            }
            long jStart4 = protoOutputStream.start(1146756268038L);
            this.mAssistants.dump(protoOutputStream, dumpFilter);
            protoOutputStream.end(jStart4);
            long jStart5 = protoOutputStream.start(1146756268039L);
            this.mConditionProviders.dump(protoOutputStream, dumpFilter);
            protoOutputStream.end(jStart5);
            long jStart6 = protoOutputStream.start(1146756268040L);
            this.mRankingHelper.dump(protoOutputStream, dumpFilter);
            protoOutputStream.end(jStart6);
        }
        protoOutputStream.flush();
    }

    private void dumpNotificationRecords(PrintWriter printWriter, DumpFilter dumpFilter) {
        synchronized (this.mNotificationLock) {
            int size = this.mNotificationList.size();
            if (size > 0) {
                printWriter.println("  Notification List:");
                for (int i = 0; i < size; i++) {
                    NotificationRecord notificationRecord = this.mNotificationList.get(i);
                    if (!dumpFilter.filtered || dumpFilter.matches(notificationRecord.sbn)) {
                        notificationRecord.dump(printWriter, "    ", getContext(), dumpFilter.redact);
                    }
                }
                printWriter.println("  ");
            }
        }
    }

    void dumpImpl(PrintWriter printWriter, DumpFilter dumpFilter) {
        printWriter.print("Current Notification Manager state");
        if (dumpFilter.filtered) {
            printWriter.print(" (filtered to ");
            printWriter.print(dumpFilter);
            printWriter.print(")");
        }
        printWriter.println(':');
        boolean z = dumpFilter.filtered && dumpFilter.zen;
        if (!z) {
            synchronized (this.mToastQueue) {
                int size = this.mToastQueue.size();
                if (size > 0) {
                    printWriter.println("  Toast Queue:");
                    for (int i = 0; i < size; i++) {
                        this.mToastQueue.get(i).dump(printWriter, "    ", dumpFilter);
                    }
                    printWriter.println("  ");
                }
            }
        }
        synchronized (this.mNotificationLock) {
            if (!z) {
                try {
                    if (!dumpFilter.normalPriority) {
                        dumpNotificationRecords(printWriter, dumpFilter);
                    }
                    if (!dumpFilter.filtered) {
                        int size2 = this.mLights.size();
                        if (size2 > 0) {
                            printWriter.println("  Lights List:");
                            for (int i2 = 0; i2 < size2; i2++) {
                                if (i2 == size2 - 1) {
                                    printWriter.print("  > ");
                                } else {
                                    printWriter.print("    ");
                                }
                                printWriter.println(this.mLights.get(i2));
                            }
                            printWriter.println("  ");
                        }
                        printWriter.println("  mUseAttentionLight=" + this.mUseAttentionLight);
                        printWriter.println("  mNotificationPulseEnabled=" + this.mNotificationPulseEnabled);
                        printWriter.println("  mSoundNotificationKey=" + this.mSoundNotificationKey);
                        printWriter.println("  mVibrateNotificationKey=" + this.mVibrateNotificationKey);
                        printWriter.println("  mDisableNotificationEffects=" + this.mDisableNotificationEffects);
                        printWriter.println("  mCallState=" + callStateToString(this.mCallState));
                        printWriter.println("  mSystemReady=" + this.mSystemReady);
                        printWriter.println("  mMaxPackageEnqueueRate=" + this.mMaxPackageEnqueueRate);
                    }
                    printWriter.println("  mArchive=" + this.mArchive.toString());
                    Iterator<StatusBarNotification> itDescendingIterator = this.mArchive.descendingIterator();
                    int i3 = 0;
                    while (true) {
                        if (!itDescendingIterator.hasNext()) {
                            break;
                        }
                        StatusBarNotification next = itDescendingIterator.next();
                        if (dumpFilter == null || dumpFilter.matches(next)) {
                            printWriter.println("    " + next);
                            i3++;
                            if (i3 >= 5) {
                                if (itDescendingIterator.hasNext()) {
                                    printWriter.println("    ...");
                                }
                            }
                        }
                    }
                    if (!z) {
                        int size3 = this.mEnqueuedNotifications.size();
                        if (size3 > 0) {
                            printWriter.println("  Enqueued Notification List:");
                            for (int i4 = 0; i4 < size3; i4++) {
                                NotificationRecord notificationRecord = this.mEnqueuedNotifications.get(i4);
                                if (!dumpFilter.filtered || dumpFilter.matches(notificationRecord.sbn)) {
                                    notificationRecord.dump(printWriter, "    ", getContext(), dumpFilter.redact);
                                }
                            }
                            printWriter.println("  ");
                        }
                        this.mSnoozeHelper.dump(printWriter, dumpFilter);
                    }
                } finally {
                }
            }
            if (!z) {
                printWriter.println("\n  Ranking Config:");
                this.mRankingHelper.dump(printWriter, "    ", dumpFilter);
                printWriter.println("\n  Notification listeners:");
                this.mListeners.dump(printWriter, dumpFilter);
                printWriter.print("    mListenerHints: ");
                printWriter.println(this.mListenerHints);
                printWriter.print("    mListenersDisablingEffects: (");
                int size4 = this.mListenersDisablingEffects.size();
                for (int i5 = 0; i5 < size4; i5++) {
                    int iKeyAt = this.mListenersDisablingEffects.keyAt(i5);
                    if (i5 > 0) {
                        printWriter.print(';');
                    }
                    printWriter.print("hint[" + iKeyAt + "]:");
                    ArraySet<ManagedServices.ManagedServiceInfo> arraySetValueAt = this.mListenersDisablingEffects.valueAt(i5);
                    int size5 = arraySetValueAt.size();
                    for (int i6 = 0; i6 < size5; i6++) {
                        if (i5 > 0) {
                            printWriter.print(',');
                        }
                        ManagedServices.ManagedServiceInfo managedServiceInfoValueAt = arraySetValueAt.valueAt(i5);
                        if (managedServiceInfoValueAt != null) {
                            printWriter.print(managedServiceInfoValueAt.component);
                        }
                    }
                }
                printWriter.println(')');
                printWriter.println("\n  Notification assistant services:");
                this.mAssistants.dump(printWriter, dumpFilter);
            }
            if (!dumpFilter.filtered || z) {
                printWriter.println("\n  Zen Mode:");
                printWriter.print("    mInterruptionFilter=");
                printWriter.println(this.mInterruptionFilter);
                this.mZenModeHelper.dump(printWriter, "    ");
                printWriter.println("\n  Zen Log:");
                ZenLog.dump(printWriter, "    ");
            }
            printWriter.println("\n  Condition providers:");
            this.mConditionProviders.dump(printWriter, dumpFilter);
            printWriter.println("\n  Group summaries:");
            for (Map.Entry<String, NotificationRecord> entry : this.mSummaryByGroupKey.entrySet()) {
                NotificationRecord value = entry.getValue();
                printWriter.println("    " + entry.getKey() + " -> " + value.getKey());
                if (this.mNotificationsByKey.get(value.getKey()) != value) {
                    printWriter.println("!!!!!!LEAK: Record not found in mNotificationsByKey.");
                    value.dump(printWriter, "      ", getContext(), dumpFilter.redact);
                }
            }
            if (!z) {
                printWriter.println("\n  Usage Stats:");
                this.mUsageStats.dump(printWriter, "    ", dumpFilter);
            }
        }
    }

    void enqueueNotificationInternal(String str, String str2, int i, int i2, String str3, int i3, Notification notification, int i4) {
        int i5;
        boolean z;
        int i6;
        int size;
        if (DBG) {
            Slog.v(TAG, "enqueueNotificationInternal: pkg=" + str + " id=" + i3 + " notification=" + notification);
        }
        checkCallerIsSystemOrSameApp(str);
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(i2, i, i4, true, false, "enqueueNotification", str);
        UserHandle userHandle = new UserHandle(iHandleIncomingUser);
        if (str == null || notification == null) {
            throw new IllegalArgumentException("null not allowed: pkg=" + str + " id=" + i3 + " notification=" + notification);
        }
        int iResolveNotificationUid = resolveNotificationUid(str2, i, iHandleIncomingUser);
        try {
            Notification.addFieldsFromContext(this.mPackageManagerClient.getApplicationInfoAsUser(str, 268435456, iHandleIncomingUser == -1 ? 0 : iHandleIncomingUser), notification);
            if (this.mPackageManagerClient.checkPermission("android.permission.USE_COLORIZED_NOTIFICATIONS", str) == 0) {
                notification.flags |= 2048;
            } else {
                notification.flags &= -2049;
            }
            this.mUsageStats.registerEnqueuedByApp(str);
            String channelId = notification.getChannelId();
            if (this.mIsTelevision && new Notification.TvExtender(notification).getChannelId() != null) {
                channelId = new Notification.TvExtender(notification).getChannelId();
            }
            String str4 = channelId;
            NotificationChannel notificationChannel = this.mRankingHelper.getNotificationChannel(str, iResolveNotificationUid, str4, false);
            if (notificationChannel == null) {
                Log.e(TAG, "No Channel found for pkg=" + str + ", channelId=" + str4 + ", id=" + i3 + ", tag=" + str3 + ", opPkg=" + str2 + ", callingUid=" + i + ", userId=" + iHandleIncomingUser + ", incomingUserId=" + i4 + ", notificationUid=" + iResolveNotificationUid + ", notification=" + notification);
                if (this.mRankingHelper.getImportance(str, iResolveNotificationUid) == 0) {
                    return;
                }
                doChannelWarningToast("Developer warning for package \"" + str + "\"\nFailed to post notification on channel \"" + str4 + "\"\nSee log for more details");
                return;
            }
            NotificationRecord notificationRecord = new NotificationRecord(getContext(), new StatusBarNotification(str, str2, i3, str3, iResolveNotificationUid, i2, notification, userHandle, (String) null, System.currentTimeMillis()), notificationChannel);
            notificationRecord.setIsAppImportanceLocked(this.mRankingHelper.getIsAppImportanceLocked(str, i));
            if ((notification.flags & 64) != 0) {
                boolean zIsFgServiceShown = notificationChannel.isFgServiceShown();
                if ((notificationChannel.getUserLockedFields() & 4) == 0 || !zIsFgServiceShown) {
                    z = true;
                    if (notificationRecord.getImportance() == 1 || notificationRecord.getImportance() == 0) {
                        if (TextUtils.isEmpty(str4) || "miscellaneous".equals(str4)) {
                            i5 = iResolveNotificationUid;
                            i6 = 0;
                            notificationRecord.setImportance(2, "Bumped for foreground service");
                        } else {
                            notificationChannel.setImportance(2);
                            if (!zIsFgServiceShown) {
                                notificationChannel.unlockFields(4);
                                notificationChannel.setFgServiceShown(true);
                            }
                            i5 = iResolveNotificationUid;
                            i6 = 0;
                            this.mRankingHelper.updateNotificationChannel(str, i5, notificationChannel, false);
                            notificationRecord.updateNotificationChannel(notificationChannel);
                        }
                        if (checkDisqualifyingFeatures(iHandleIncomingUser, i5, i3, str3, notificationRecord, notificationRecord.sbn.getOverrideGroupKey() == null ? z : i6)) {
                            return;
                        }
                        if (notification.allPendingIntents != null && (size = notification.allPendingIntents.size()) > 0) {
                            ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                            long notificationWhitelistDuration = ((DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class)).getNotificationWhitelistDuration();
                            while (i6 < size) {
                                PendingIntent pendingIntent = (PendingIntent) notification.allPendingIntents.valueAt(i6);
                                if (pendingIntent != null) {
                                    activityManagerInternal.setPendingIntentWhitelistDuration(pendingIntent.getTarget(), WHITELIST_TOKEN, notificationWhitelistDuration);
                                }
                                i6++;
                            }
                        }
                        this.mHandler.post(new EnqueueNotificationRunnable(iHandleIncomingUser, notificationRecord));
                        return;
                    }
                } else {
                    z = true;
                }
                if (!zIsFgServiceShown && !TextUtils.isEmpty(str4) && !"miscellaneous".equals(str4)) {
                    notificationChannel.setFgServiceShown(z);
                    notificationRecord.updateNotificationChannel(notificationChannel);
                }
                i5 = iResolveNotificationUid;
            } else {
                i5 = iResolveNotificationUid;
                z = true;
            }
            i6 = 0;
            if (checkDisqualifyingFeatures(iHandleIncomingUser, i5, i3, str3, notificationRecord, notificationRecord.sbn.getOverrideGroupKey() == null ? z : i6)) {
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Cannot create a context for sending app", e);
        }
    }

    private void doChannelWarningToast(CharSequence charSequence) {
        if (Settings.Global.getInt(getContext().getContentResolver(), "show_notification_channel_warnings", Build.IS_DEBUGGABLE ? 1 : 0) != 0) {
            Toast.makeText(getContext(), this.mHandler.getLooper(), charSequence, 0).show();
        }
    }

    private int resolveNotificationUid(String str, int i, int i2) {
        if (isCallerSystemOrPhone() && str != null && !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str)) {
            try {
                return getContext().getPackageManager().getPackageUidAsUser(str, i2);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return i;
    }

    private boolean checkDisqualifyingFeatures(int i, int i2, int i3, String str, NotificationRecord notificationRecord, boolean z) {
        String packageName = notificationRecord.sbn.getPackageName();
        boolean z2 = isUidSystemOrPhone(i2) || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName);
        boolean zIsListenerPackage = this.mListeners.isListenerPackage(packageName);
        if (!z2 && !zIsListenerPackage) {
            synchronized (this.mNotificationLock) {
                if (this.mNotificationsByKey.get(notificationRecord.sbn.getKey()) == null && isCallerInstantApp(packageName)) {
                    throw new SecurityException("Instant app " + packageName + " cannot create notifications");
                }
                if (this.mNotificationsByKey.get(notificationRecord.sbn.getKey()) != null && !notificationRecord.getNotification().hasCompletedProgress() && !z) {
                    float appEnqueueRate = this.mUsageStats.getAppEnqueueRate(packageName);
                    if (appEnqueueRate > this.mMaxPackageEnqueueRate) {
                        this.mUsageStats.registerOverRateQuota(packageName);
                        long jElapsedRealtime = SystemClock.elapsedRealtime();
                        if (jElapsedRealtime - this.mLastOverRateLogTime > MIN_PACKAGE_OVERRATE_LOG_INTERVAL) {
                            Slog.e(TAG, "Package enqueue rate is " + appEnqueueRate + ". Shedding " + notificationRecord.sbn.getKey() + ". package=" + packageName);
                            this.mLastOverRateLogTime = jElapsedRealtime;
                        }
                        return false;
                    }
                }
                int notificationCountLocked = getNotificationCountLocked(packageName, i, i3, str);
                if (notificationCountLocked >= 50) {
                    this.mUsageStats.registerOverCountQuota(packageName);
                    Slog.e(TAG, "Package has already posted or enqueued " + notificationCountLocked + " notifications.  Not showing more.  package=" + packageName);
                    return false;
                }
            }
        }
        if (!this.mSnoozeHelper.isSnoozed(i, packageName, notificationRecord.getKey())) {
            return !isBlocked(notificationRecord, this.mUsageStats);
        }
        MetricsLogger.action(notificationRecord.getLogMaker().setType(6).setCategory(831));
        if (DBG) {
            Slog.d(TAG, "Ignored enqueue for snoozed notification " + notificationRecord.getKey());
        }
        this.mSnoozeHelper.update(i, notificationRecord);
        savePolicyFile();
        return false;
    }

    @GuardedBy("mNotificationLock")
    protected int getNotificationCountLocked(String str, int i, int i2, String str2) {
        int size = this.mNotificationList.size();
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            NotificationRecord notificationRecord = this.mNotificationList.get(i4);
            if (notificationRecord.sbn.getPackageName().equals(str) && notificationRecord.sbn.getUserId() == i && (notificationRecord.sbn.getId() != i2 || !TextUtils.equals(notificationRecord.sbn.getTag(), str2))) {
                i3++;
            }
        }
        int size2 = this.mEnqueuedNotifications.size();
        for (int i5 = 0; i5 < size2; i5++) {
            NotificationRecord notificationRecord2 = this.mEnqueuedNotifications.get(i5);
            if (notificationRecord2.sbn.getPackageName().equals(str) && notificationRecord2.sbn.getUserId() == i) {
                i3++;
            }
        }
        return i3;
    }

    protected boolean isBlocked(NotificationRecord notificationRecord, NotificationUsageStats notificationUsageStats) {
        String packageName = notificationRecord.sbn.getPackageName();
        int uid = notificationRecord.sbn.getUid();
        boolean zIsPackageSuspendedForUser = isPackageSuspendedForUser(packageName, uid);
        if (zIsPackageSuspendedForUser) {
            Slog.e(TAG, "Suppressing notification from package due to package suspended by administrator.");
            notificationUsageStats.registerSuspendedByAdmin(notificationRecord);
            return zIsPackageSuspendedForUser;
        }
        boolean z = this.mRankingHelper.isGroupBlocked(packageName, uid, notificationRecord.getChannel().getGroup()) || this.mRankingHelper.getImportance(packageName, uid) == 0 || notificationRecord.getChannel().getImportance() == 0;
        if (z) {
            Slog.e(TAG, "Suppressing notification from package by user request.");
            notificationUsageStats.registerBlocked(notificationRecord);
        }
        return z;
    }

    protected class SnoozeNotificationRunnable implements Runnable {
        private final long mDuration;
        private final String mKey;
        private final String mSnoozeCriterionId;

        SnoozeNotificationRunnable(String str, long j, String str2) {
            this.mKey = str;
            this.mDuration = j;
            this.mSnoozeCriterionId = str2;
        }

        @Override
        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord notificationRecordFindNotificationByKeyLocked = NotificationManagerService.this.findNotificationByKeyLocked(this.mKey);
                if (notificationRecordFindNotificationByKeyLocked != null) {
                    snoozeLocked(notificationRecordFindNotificationByKeyLocked);
                }
            }
        }

        @GuardedBy("mNotificationLock")
        void snoozeLocked(NotificationRecord notificationRecord) {
            if (notificationRecord.sbn.isGroup()) {
                List<NotificationRecord> listFindGroupNotificationsLocked = NotificationManagerService.this.findGroupNotificationsLocked(notificationRecord.sbn.getPackageName(), notificationRecord.sbn.getGroupKey(), notificationRecord.sbn.getUserId());
                int i = 0;
                if (notificationRecord.getNotification().isGroupSummary()) {
                    while (i < listFindGroupNotificationsLocked.size()) {
                        snoozeNotificationLocked(listFindGroupNotificationsLocked.get(i));
                        i++;
                    }
                    return;
                } else {
                    if (NotificationManagerService.this.mSummaryByGroupKey.containsKey(notificationRecord.sbn.getGroupKey())) {
                        if (listFindGroupNotificationsLocked.size() != 2) {
                            snoozeNotificationLocked(notificationRecord);
                            return;
                        }
                        while (i < listFindGroupNotificationsLocked.size()) {
                            snoozeNotificationLocked(listFindGroupNotificationsLocked.get(i));
                            i++;
                        }
                        return;
                    }
                    snoozeNotificationLocked(notificationRecord);
                    return;
                }
            }
            snoozeNotificationLocked(notificationRecord);
        }

        @GuardedBy("mNotificationLock")
        void snoozeNotificationLocked(NotificationRecord notificationRecord) {
            MetricsLogger.action(notificationRecord.getLogMaker().setCategory(831).setType(2).addTaggedData(1139, Long.valueOf(this.mDuration)).addTaggedData(832, Integer.valueOf(this.mSnoozeCriterionId == null ? 0 : 1)));
            NotificationManagerService.this.cancelNotificationLocked(notificationRecord, false, 18, NotificationManagerService.this.removeFromNotificationListsLocked(notificationRecord), null);
            NotificationManagerService.this.updateLightsLocked();
            if (this.mSnoozeCriterionId != null) {
                NotificationManagerService.this.mAssistants.notifyAssistantSnoozedLocked(notificationRecord.sbn, this.mSnoozeCriterionId);
                NotificationManagerService.this.mSnoozeHelper.snooze(notificationRecord);
            } else {
                NotificationManagerService.this.mSnoozeHelper.snooze(notificationRecord, this.mDuration);
            }
            notificationRecord.recordSnoozed();
            NotificationManagerService.this.savePolicyFile();
        }
    }

    protected class EnqueueNotificationRunnable implements Runnable {
        private final NotificationRecord r;
        private final int userId;

        EnqueueNotificationRunnable(int i, NotificationRecord notificationRecord) {
            this.userId = i;
            this.r = notificationRecord;
        }

        @Override
        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mEnqueuedNotifications.add(this.r);
                NotificationManagerService.this.scheduleTimeoutLocked(this.r);
                StatusBarNotification statusBarNotification = this.r.sbn;
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "EnqueueNotificationRunnable.run for: " + statusBarNotification.getKey());
                }
                NotificationRecord notificationRecord = NotificationManagerService.this.mNotificationsByKey.get(statusBarNotification.getKey());
                if (notificationRecord != null) {
                    this.r.copyRankingInformation(notificationRecord);
                }
                int uid = statusBarNotification.getUid();
                int initialPid = statusBarNotification.getInitialPid();
                Notification notification = statusBarNotification.getNotification();
                String packageName = statusBarNotification.getPackageName();
                int id = statusBarNotification.getId();
                String tag = statusBarNotification.getTag();
                NotificationManagerService.this.handleGroupedNotificationLocked(this.r, notificationRecord, uid, initialPid);
                if (statusBarNotification.isGroup() && notification.isGroupChild()) {
                    NotificationManagerService.this.mSnoozeHelper.repostGroupSummary(packageName, this.r.getUserId(), statusBarNotification.getGroupKey());
                }
                if (!packageName.equals("com.android.providers.downloads") || Log.isLoggable("DownloadManager", 2)) {
                    int i = 0;
                    if (notificationRecord != null) {
                        i = 1;
                    }
                    EventLogTags.writeNotificationEnqueue(uid, initialPid, packageName, id, tag, this.userId, notification.toString(), i);
                }
                NotificationManagerService.this.mRankingHelper.extractSignals(this.r);
                if (NotificationManagerService.this.mAssistants.isEnabled()) {
                    NotificationManagerService.this.mAssistants.onNotificationEnqueued(this.r);
                    NotificationManagerService.this.mHandler.postDelayed(NotificationManagerService.this.new PostNotificationRunnable(this.r.getKey()), NotificationManagerService.DELAY_FOR_ASSISTANT_TIME);
                } else {
                    NotificationManagerService.this.mHandler.post(NotificationManagerService.this.new PostNotificationRunnable(this.r.getKey()));
                }
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private boolean isPackageSuspendedLocked(NotificationRecord notificationRecord) {
        return isPackageSuspendedForUser(notificationRecord.sbn.getPackageName(), notificationRecord.sbn.getUid());
    }

    protected class PostNotificationRunnable implements Runnable {
        private final String key;

        PostNotificationRunnable(String str) {
            this.key = str;
        }

        @Override
        public void run() {
            NotificationRecord notificationRecord;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                int i = 0;
                try {
                    int size = NotificationManagerService.this.mEnqueuedNotifications.size();
                    int i2 = 0;
                    while (true) {
                        if (i2 >= size) {
                            notificationRecord = null;
                            break;
                        }
                        notificationRecord = NotificationManagerService.this.mEnqueuedNotifications.get(i2);
                        if (Objects.equals(this.key, notificationRecord.getKey())) {
                            break;
                        } else {
                            i2++;
                        }
                    }
                    if (notificationRecord == null) {
                        Slog.i(NotificationManagerService.TAG, "Cannot find enqueued record for key: " + this.key);
                        int size2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                        while (true) {
                            if (i >= size2) {
                                break;
                            }
                            if (Objects.equals(this.key, NotificationManagerService.this.mEnqueuedNotifications.get(i).getKey())) {
                                NotificationManagerService.this.mEnqueuedNotifications.remove(i);
                                break;
                            }
                            i++;
                        }
                        return;
                    }
                    notificationRecord.setHidden(NotificationManagerService.this.isPackageSuspendedLocked(notificationRecord));
                    NotificationRecord notificationRecord2 = NotificationManagerService.this.mNotificationsByKey.get(this.key);
                    final StatusBarNotification statusBarNotification = notificationRecord.sbn;
                    Notification notification = statusBarNotification.getNotification();
                    int iIndexOfNotificationLocked = NotificationManagerService.this.indexOfNotificationLocked(statusBarNotification.getKey());
                    if (iIndexOfNotificationLocked < 0) {
                        NotificationManagerService.this.mNotificationList.add(notificationRecord);
                        NotificationManagerService.this.mUsageStats.registerPostedByApp(notificationRecord);
                        notificationRecord.setInterruptive(NotificationManagerService.this.isVisuallyInterruptive(null, notificationRecord));
                    } else {
                        notificationRecord2 = NotificationManagerService.this.mNotificationList.get(iIndexOfNotificationLocked);
                        NotificationManagerService.this.mNotificationList.set(iIndexOfNotificationLocked, notificationRecord);
                        NotificationManagerService.this.mUsageStats.registerUpdatedByApp(notificationRecord, notificationRecord2);
                        notification.flags |= notificationRecord2.getNotification().flags & 64;
                        notificationRecord.isUpdate = true;
                        notificationRecord.setTextChanged(NotificationManagerService.this.isVisuallyInterruptive(notificationRecord2, notificationRecord));
                    }
                    NotificationManagerService.this.mNotificationsByKey.put(statusBarNotification.getKey(), notificationRecord);
                    if ((notification.flags & 64) != 0) {
                        notification.flags |= 34;
                    }
                    NotificationManagerService.this.applyZenModeLocked(notificationRecord);
                    NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                    if (notification.getSmallIcon() != null) {
                        StatusBarNotification statusBarNotification2 = notificationRecord2 != null ? notificationRecord2.sbn : null;
                        NotificationManagerService.this.mListeners.notifyPostedLocked(notificationRecord, notificationRecord2);
                        if (statusBarNotification2 == null || !Objects.equals(statusBarNotification2.getGroup(), statusBarNotification.getGroup())) {
                            NotificationManagerService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationManagerService.this.mGroupHelper.onNotificationPosted(statusBarNotification, NotificationManagerService.this.hasAutoGroupSummaryLocked(statusBarNotification));
                                }
                            });
                        }
                    } else {
                        Slog.e(NotificationManagerService.TAG, "Not posting notification without small icon: " + notification);
                        if (notificationRecord2 != null && !notificationRecord2.isCanceled) {
                            NotificationManagerService.this.mListeners.notifyRemovedLocked(notificationRecord, 4, null);
                            NotificationManagerService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationManagerService.this.mGroupHelper.onNotificationRemoved(statusBarNotification);
                                }
                            });
                        }
                        Slog.e(NotificationManagerService.TAG, "WARNING: In a future release this will crash the app: " + statusBarNotification.getPackageName());
                    }
                    if (!notificationRecord.isHidden()) {
                        NotificationManagerService.this.buzzBeepBlinkLocked(notificationRecord);
                    }
                    NotificationManagerService.this.maybeRecordInterruptionLocked(notificationRecord);
                    int size3 = NotificationManagerService.this.mEnqueuedNotifications.size();
                    while (true) {
                        if (i >= size3) {
                            break;
                        }
                        if (Objects.equals(this.key, NotificationManagerService.this.mEnqueuedNotifications.get(i).getKey())) {
                            NotificationManagerService.this.mEnqueuedNotifications.remove(i);
                            break;
                        }
                        i++;
                    }
                } catch (Throwable th) {
                    int size4 = NotificationManagerService.this.mEnqueuedNotifications.size();
                    while (true) {
                        if (i >= size4) {
                            break;
                        }
                        if (Objects.equals(this.key, NotificationManagerService.this.mEnqueuedNotifications.get(i).getKey())) {
                            NotificationManagerService.this.mEnqueuedNotifications.remove(i);
                            break;
                        }
                        i++;
                    }
                    throw th;
                }
            }
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    protected boolean isVisuallyInterruptive(NotificationRecord notificationRecord, NotificationRecord notificationRecord2) {
        Notification.Builder builderRecoverBuilder;
        Notification.Builder builderRecoverBuilder2;
        if (notificationRecord == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: new notification");
            }
            return true;
        }
        if (notificationRecord2 == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is not interruptive: null");
            }
            return false;
        }
        Notification notification = notificationRecord.sbn.getNotification();
        Notification notification2 = notificationRecord2.sbn.getNotification();
        if (notification.extras == null || notification2.extras == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is not interruptive: no extras");
            }
            return false;
        }
        if ((notificationRecord2.sbn.getNotification().flags & 64) != 0) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is not interruptive: foreground service");
            }
            return false;
        }
        if (notificationRecord2.sbn.isGroup() && notificationRecord2.sbn.getNotification().isGroupSummary()) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is not interruptive: summary");
            }
            return false;
        }
        String strValueOf = String.valueOf(notification.extras.get("android.title"));
        String strValueOf2 = String.valueOf(notification2.extras.get("android.title"));
        if (!Objects.equals(strValueOf, strValueOf2)) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: changed title");
                StringBuilder sb = new StringBuilder();
                sb.append("INTERRUPTIVENESS: ");
                sb.append(String.format("   old title: %s (%s@0x%08x)", strValueOf, strValueOf.getClass(), Integer.valueOf(strValueOf.hashCode())));
                Log.v(TAG, sb.toString());
                Log.v(TAG, "INTERRUPTIVENESS: " + String.format("   new title: %s (%s@0x%08x)", strValueOf2, strValueOf2.getClass(), Integer.valueOf(strValueOf2.hashCode())));
            }
            return true;
        }
        String strValueOf3 = String.valueOf(notification.extras.get("android.text"));
        String strValueOf4 = String.valueOf(notification2.extras.get("android.text"));
        if (!Objects.equals(strValueOf3, strValueOf4)) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: changed text");
                StringBuilder sb2 = new StringBuilder();
                sb2.append("INTERRUPTIVENESS: ");
                sb2.append(String.format("   old text: %s (%s@0x%08x)", strValueOf3, strValueOf3.getClass(), Integer.valueOf(strValueOf3.hashCode())));
                Log.v(TAG, sb2.toString());
                Log.v(TAG, "INTERRUPTIVENESS: " + String.format("   new text: %s (%s@0x%08x)", strValueOf4, strValueOf4.getClass(), Integer.valueOf(strValueOf4.hashCode())));
            }
            return true;
        }
        if (notification.hasCompletedProgress() != notification2.hasCompletedProgress()) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: completed progress");
            }
            return true;
        }
        if (Notification.areActionsVisiblyDifferent(notification, notification2)) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: changed actions");
            }
            return true;
        }
        try {
            builderRecoverBuilder = Notification.Builder.recoverBuilder(getContext(), notification);
            builderRecoverBuilder2 = Notification.Builder.recoverBuilder(getContext(), notification2);
        } catch (Exception e) {
            Slog.w(TAG, "error recovering builder", e);
        }
        if (Notification.areStyledNotificationsVisiblyDifferent(builderRecoverBuilder, builderRecoverBuilder2)) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: styles differ");
            }
            return true;
        }
        if (Notification.areRemoteViewsChanged(builderRecoverBuilder, builderRecoverBuilder2)) {
            if (DEBUG_INTERRUPTIVENESS) {
                Log.v(TAG, "INTERRUPTIVENESS: " + notificationRecord2.getKey() + " is interruptive: remoteviews differ");
            }
            return true;
        }
        return false;
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    protected void logRecentLocked(NotificationRecord notificationRecord) {
        if (notificationRecord.isUpdate) {
            return;
        }
        ArrayList<NotifyingApp> orDefault = this.mRecentApps.getOrDefault(Integer.valueOf(notificationRecord.getUser().getIdentifier()), new ArrayList<>(6));
        NotifyingApp lastNotified = new NotifyingApp().setPackage(notificationRecord.sbn.getPackageName()).setUid(notificationRecord.sbn.getUid()).setLastNotified(notificationRecord.sbn.getPostTime());
        int size = orDefault.size() - 1;
        while (true) {
            if (size < 0) {
                break;
            }
            NotifyingApp notifyingApp = orDefault.get(size);
            if (!lastNotified.getPackage().equals(notifyingApp.getPackage()) || lastNotified.getUid() != notifyingApp.getUid()) {
                size--;
            } else {
                orDefault.remove(size);
                break;
            }
        }
        orDefault.add(0, lastNotified);
        if (orDefault.size() > 5) {
            orDefault.remove(orDefault.size() - 1);
        }
        this.mRecentApps.put(Integer.valueOf(notificationRecord.getUser().getIdentifier()), orDefault);
    }

    @GuardedBy("mNotificationLock")
    private void handleGroupedNotificationLocked(NotificationRecord notificationRecord, NotificationRecord notificationRecord2, int i, int i2) {
        NotificationRecord notificationRecordRemove;
        StatusBarNotification statusBarNotification = notificationRecord.sbn;
        Notification notification = statusBarNotification.getNotification();
        if (notification.isGroupSummary() && !statusBarNotification.isAppGroup()) {
            notification.flags &= -513;
        }
        String groupKey = statusBarNotification.getGroupKey();
        boolean zIsGroupSummary = notification.isGroupSummary();
        Notification notification2 = notificationRecord2 != null ? notificationRecord2.sbn.getNotification() : null;
        String groupKey2 = notificationRecord2 != null ? notificationRecord2.sbn.getGroupKey() : null;
        boolean z = notificationRecord2 != null && notification2.isGroupSummary();
        if (z && (notificationRecordRemove = this.mSummaryByGroupKey.remove(groupKey2)) != notificationRecord2) {
            Slog.w(TAG, "Removed summary didn't match old notification: old=" + notificationRecord2.getKey() + ", removed=" + (notificationRecordRemove != null ? notificationRecordRemove.getKey() : "<null>"));
        }
        if (zIsGroupSummary) {
            this.mSummaryByGroupKey.put(groupKey, notificationRecord);
        }
        if (z) {
            if (!zIsGroupSummary || !groupKey2.equals(groupKey)) {
                cancelGroupChildrenLocked(notificationRecord2, i, i2, null, false, null);
            }
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    void scheduleTimeoutLocked(NotificationRecord notificationRecord) {
        if (notificationRecord.getNotification().getTimeoutAfter() > 0) {
            this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + notificationRecord.getNotification().getTimeoutAfter(), PendingIntent.getBroadcast(getContext(), 1, new Intent(ACTION_NOTIFICATION_TIMEOUT).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).setData(new Uri.Builder().scheme(SCHEME_TIMEOUT).appendPath(notificationRecord.getKey()).build()).addFlags(268435456).putExtra(EXTRA_KEY, notificationRecord.getKey()), 134217728));
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    void buzzBeepBlinkLocked(NotificationRecord notificationRecord) {
        boolean z;
        boolean zPlaySound;
        boolean zPlayVibration;
        boolean z2;
        boolean z3;
        int i;
        String key = notificationRecord.getKey();
        boolean z4 = notificationRecord.getImportance() >= 3;
        boolean z5 = key != null && key.equals(this.mSoundNotificationKey);
        boolean z6 = key != null && key.equals(this.mVibrateNotificationKey);
        if (notificationRecord.isUpdate || notificationRecord.getImportance() <= 1) {
            z = false;
        } else {
            sendAccessibilityEvent(notificationRecord);
            z = true;
        }
        if (z4 && isNotificationForCurrentUser(notificationRecord) && this.mSystemReady && this.mAudioManager != null) {
            Uri sound = notificationRecord.getSound();
            z2 = (sound == null || Uri.EMPTY.equals(sound)) ? false : true;
            long[] vibration = notificationRecord.getVibration();
            if (vibration == null && z2 && this.mAudioManager.getRingerModeInternal() == 1 && this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(notificationRecord.getAudioAttributes())) == 0) {
                vibration = this.mFallbackVibrationPattern;
            }
            z3 = vibration != null;
            if (!(z2 || z3) || shouldMuteNotificationLocked(notificationRecord)) {
                zPlaySound = false;
                zPlayVibration = false;
            } else {
                if (!z) {
                    sendAccessibilityEvent(notificationRecord);
                }
                if (DBG) {
                    Slog.v(TAG, "Interrupting!");
                }
                if (z2) {
                    this.mSoundNotificationKey = key;
                    if (this.mInCall) {
                        playInCallNotification();
                        zPlaySound = true;
                    } else {
                        zPlaySound = playSound(notificationRecord, sound);
                    }
                } else {
                    zPlaySound = false;
                }
                boolean z7 = this.mAudioManager.getRingerModeInternal() == 0;
                if (!this.mInCall && z3 && !z7) {
                    this.mVibrateNotificationKey = key;
                    zPlayVibration = playVibration(notificationRecord, vibration, z2);
                } else {
                    zPlayVibration = false;
                }
            }
        } else {
            zPlaySound = false;
            zPlayVibration = false;
            z2 = false;
            z3 = false;
        }
        if (z5 && !z2) {
            clearSoundLocked();
        }
        if (z6 && !z3) {
            clearVibrateLocked();
        }
        boolean zRemove = this.mLights.remove(key);
        if (notificationRecord.getLight() != null && z4 && (notificationRecord.getSuppressedVisualEffects() & 8) == 0) {
            this.mLights.add(key);
            updateLightsLocked();
            if (this.mUseAttentionLight) {
                this.mAttentionLight.pulse();
            }
            i = 1;
        } else {
            if (zRemove) {
                updateLightsLocked();
            }
            i = 0;
        }
        if (zPlayVibration || zPlaySound || i != 0) {
            notificationRecord.setInterruptive(true);
            MetricsLogger.action(notificationRecord.getLogMaker().setCategory(199).setType(1).setSubtype((zPlayVibration ? 1 : 0) | (zPlaySound ? 2 : 0) | (i != 0 ? 4 : 0)));
            EventLogTags.writeNotificationAlert(key, zPlayVibration ? 1 : 0, zPlaySound ? 1 : 0, i);
        }
    }

    @GuardedBy("mNotificationLock")
    boolean shouldMuteNotificationLocked(NotificationRecord notificationRecord) {
        Notification notification = notificationRecord.getNotification();
        if (notificationRecord.isUpdate && (notification.flags & 8) != 0) {
            return true;
        }
        String strDisableNotificationEffects = disableNotificationEffects(notificationRecord);
        if (strDisableNotificationEffects != null) {
            ZenLog.traceDisableEffects(notificationRecord, strDisableNotificationEffects);
            return true;
        }
        if (notificationRecord.isIntercepted()) {
            return true;
        }
        if (notificationRecord.sbn.isGroup() && notification.suppressAlertingDueToGrouping()) {
            return true;
        }
        if (this.mUsageStats.isAlertRateLimited(notificationRecord.sbn.getPackageName())) {
            Slog.e(TAG, "Muting recently noisy " + notificationRecord.getKey());
            return true;
        }
        return false;
    }

    private boolean playSound(NotificationRecord notificationRecord, Uri uri) {
        boolean z = (notificationRecord.getNotification().flags & 4) != 0;
        if (!this.mAudioManager.isAudioFocusExclusive() && this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(notificationRecord.getAudioAttributes())) != 0) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                IRingtonePlayer ringtonePlayer = this.mAudioManager.getRingtonePlayer();
                if (ringtonePlayer != null) {
                    if (DBG) {
                        Slog.v(TAG, "Playing sound " + uri + " with attributes " + notificationRecord.getAudioAttributes());
                    }
                    ringtonePlayer.playAsync(uri, notificationRecord.sbn.getUser(), z, notificationRecord.getAudioAttributes());
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return true;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
        return false;
    }

    private boolean playVibration(final NotificationRecord notificationRecord, long[] jArr, boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            final VibrationEffect vibrationEffectCreateWaveform = VibrationEffect.createWaveform(jArr, (notificationRecord.getNotification().flags & 4) != 0 ? 0 : -1);
            if (z) {
                new Thread(new Runnable() {
                    @Override
                    public final void run() {
                        NotificationManagerService.lambda$playVibration$0(this.f$0, notificationRecord, vibrationEffectCreateWaveform);
                    }
                }).start();
            } else {
                this.mVibrator.vibrate(notificationRecord.sbn.getUid(), notificationRecord.sbn.getOpPkg(), vibrationEffectCreateWaveform, notificationRecord.getAudioAttributes());
            }
            return true;
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Error creating vibration waveform with pattern: " + Arrays.toString(jArr));
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static void lambda$playVibration$0(NotificationManagerService notificationManagerService, NotificationRecord notificationRecord, VibrationEffect vibrationEffect) {
        int focusRampTimeMs = notificationManagerService.mAudioManager.getFocusRampTimeMs(3, notificationRecord.getAudioAttributes());
        if (DBG) {
            Slog.v(TAG, "Delaying vibration by " + focusRampTimeMs + "ms");
        }
        try {
            Thread.sleep(focusRampTimeMs);
        } catch (InterruptedException e) {
        }
        notificationManagerService.mVibrator.vibrate(notificationRecord.sbn.getUid(), notificationRecord.sbn.getOpPkg(), vibrationEffect, notificationRecord.getAudioAttributes());
    }

    private boolean isNotificationForCurrentUser(NotificationRecord notificationRecord) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return notificationRecord.getUserId() == -1 || notificationRecord.getUserId() == currentUser || this.mUserProfiles.isCurrentProfile(notificationRecord.getUserId());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    protected void playInCallNotification() {
        new Thread() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    IRingtonePlayer ringtonePlayer = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                    if (ringtonePlayer != null) {
                        ringtonePlayer.play(new Binder(), NotificationManagerService.this.mInCallNotificationUri, NotificationManagerService.this.mInCallNotificationAudioAttributes, NotificationManagerService.this.mInCallNotificationVolume, false);
                    }
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }.start();
    }

    @GuardedBy("mToastQueue")
    void showNextToastLocked() {
        ToastRecord toastRecord = this.mToastQueue.get(0);
        while (toastRecord != null) {
            if (DBG) {
                Slog.d(TAG, "Show pkg=" + toastRecord.pkg + " callback=" + toastRecord.callback);
            }
            try {
                toastRecord.callback.show(toastRecord.token);
                scheduleDurationReachedLocked(toastRecord);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Object died trying to show notification " + toastRecord.callback + " in package " + toastRecord.pkg);
                int iIndexOf = this.mToastQueue.indexOf(toastRecord);
                if (iIndexOf >= 0) {
                    this.mToastQueue.remove(iIndexOf);
                }
                keepProcessAliveIfNeededLocked(toastRecord.pid);
                if (this.mToastQueue.size() > 0) {
                    toastRecord = this.mToastQueue.get(0);
                } else {
                    toastRecord = null;
                }
            }
        }
    }

    @GuardedBy("mToastQueue")
    void cancelToastLocked(int i) {
        ToastRecord toastRecord = this.mToastQueue.get(i);
        try {
            toastRecord.callback.hide();
        } catch (RemoteException e) {
            Slog.w(TAG, "Object died trying to hide notification " + toastRecord.callback + " in package " + toastRecord.pkg);
        }
        ToastRecord toastRecordRemove = this.mToastQueue.remove(i);
        this.mWindowManagerInternal.removeWindowToken(toastRecordRemove.token, false, 0);
        scheduleKillTokenTimeout(toastRecordRemove.token);
        keepProcessAliveIfNeededLocked(toastRecord.pid);
        if (this.mToastQueue.size() > 0) {
            showNextToastLocked();
        }
    }

    void finishTokenLocked(IBinder iBinder) {
        this.mHandler.removeCallbacksAndMessages(iBinder);
        this.mWindowManagerInternal.removeWindowToken(iBinder, true, 0);
    }

    @GuardedBy("mToastQueue")
    private void scheduleDurationReachedLocked(ToastRecord toastRecord) {
        this.mHandler.removeCallbacksAndMessages(toastRecord);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 2, toastRecord), toastRecord.duration == 1 ? 3500L : 2000L);
    }

    private void handleDurationReached(ToastRecord toastRecord) {
        if (DBG) {
            Slog.d(TAG, "Timeout pkg=" + toastRecord.pkg + " callback=" + toastRecord.callback);
        }
        synchronized (this.mToastQueue) {
            int iIndexOfToastLocked = indexOfToastLocked(toastRecord.pkg, toastRecord.callback);
            if (iIndexOfToastLocked >= 0) {
                cancelToastLocked(iIndexOfToastLocked);
            }
        }
    }

    @GuardedBy("mToastQueue")
    private void scheduleKillTokenTimeout(IBinder iBinder) {
        this.mHandler.removeCallbacksAndMessages(iBinder);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 7, iBinder), 11000L);
    }

    private void handleKillTokenTimeout(IBinder iBinder) {
        if (DBG) {
            Slog.d(TAG, "Kill Token Timeout token=" + iBinder);
        }
        synchronized (this.mToastQueue) {
            finishTokenLocked(iBinder);
        }
    }

    @GuardedBy("mToastQueue")
    int indexOfToastLocked(String str, ITransientNotification iTransientNotification) {
        IBinder iBinderAsBinder = iTransientNotification.asBinder();
        ArrayList<ToastRecord> arrayList = this.mToastQueue;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ToastRecord toastRecord = arrayList.get(i);
            if (toastRecord.pkg.equals(str) && toastRecord.callback.asBinder().equals(iBinderAsBinder)) {
                return i;
            }
        }
        return -1;
    }

    @GuardedBy("mToastQueue")
    int indexOfToastPackageLocked(String str) {
        ArrayList<ToastRecord> arrayList = this.mToastQueue;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (arrayList.get(i).pkg.equals(str)) {
                return i;
            }
        }
        return -1;
    }

    @GuardedBy("mToastQueue")
    void keepProcessAliveIfNeededLocked(int i) {
        ArrayList<ToastRecord> arrayList = this.mToastQueue;
        int size = arrayList.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            if (arrayList.get(i3).pid == i) {
                i2++;
            }
        }
        try {
            this.mAm.setProcessImportant(this.mForegroundToken, i, i2 > 0, "toast");
        } catch (RemoteException e) {
        }
    }

    private void handleRankingReconsideration(Message message) {
        if (message.obj instanceof RankingReconsideration) {
            RankingReconsideration rankingReconsideration = (RankingReconsideration) message.obj;
            rankingReconsideration.run();
            synchronized (this.mNotificationLock) {
                NotificationRecord notificationRecord = this.mNotificationsByKey.get(rankingReconsideration.getKey());
                if (notificationRecord == null) {
                    return;
                }
                int iFindNotificationRecordIndexLocked = findNotificationRecordIndexLocked(notificationRecord);
                boolean zIsIntercepted = notificationRecord.isIntercepted();
                float contactAffinity = notificationRecord.getContactAffinity();
                int packageVisibilityOverride = notificationRecord.getPackageVisibilityOverride();
                rankingReconsideration.applyChangesLocked(notificationRecord);
                applyZenModeLocked(notificationRecord);
                this.mRankingHelper.sort(this.mNotificationList);
                int iFindNotificationRecordIndexLocked2 = findNotificationRecordIndexLocked(notificationRecord);
                boolean zIsIntercepted2 = notificationRecord.isIntercepted();
                float contactAffinity2 = notificationRecord.getContactAffinity();
                boolean z = (iFindNotificationRecordIndexLocked == iFindNotificationRecordIndexLocked2 && zIsIntercepted == zIsIntercepted2 && packageVisibilityOverride == notificationRecord.getPackageVisibilityOverride()) ? false : true;
                if (zIsIntercepted && !zIsIntercepted2 && Float.compare(contactAffinity, contactAffinity2) != 0) {
                    buzzBeepBlinkLocked(notificationRecord);
                }
                if (z) {
                    this.mHandler.scheduleSendRankingUpdate();
                }
            }
        }
    }

    void handleRankingSort() {
        if (this.mRankingHelper == null) {
            return;
        }
        synchronized (this.mNotificationLock) {
            int size = this.mNotificationList.size();
            ArrayList arrayList = new ArrayList(size);
            int[] iArr = new int[size];
            boolean[] zArr = new boolean[size];
            ArrayList arrayList2 = new ArrayList(size);
            ArrayList arrayList3 = new ArrayList(size);
            ArrayList arrayList4 = new ArrayList(size);
            ArrayList arrayList5 = new ArrayList(size);
            ArrayList arrayList6 = new ArrayList(size);
            ArrayList arrayList7 = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = this.mNotificationList.get(i);
                arrayList.add(notificationRecord.getKey());
                iArr[i] = notificationRecord.getPackageVisibilityOverride();
                zArr[i] = notificationRecord.canShowBadge();
                arrayList2.add(notificationRecord.getChannel());
                arrayList3.add(notificationRecord.getGroupKey());
                arrayList4.add(notificationRecord.getPeopleOverride());
                arrayList5.add(notificationRecord.getSnoozeCriteria());
                arrayList6.add(Integer.valueOf(notificationRecord.getUserSentiment()));
                arrayList7.add(Integer.valueOf(notificationRecord.getSuppressedVisualEffects()));
                this.mRankingHelper.extractSignals(notificationRecord);
            }
            this.mRankingHelper.sort(this.mNotificationList);
            for (int i2 = 0; i2 < size; i2++) {
                NotificationRecord notificationRecord2 = this.mNotificationList.get(i2);
                if (((String) arrayList.get(i2)).equals(notificationRecord2.getKey()) && iArr[i2] == notificationRecord2.getPackageVisibilityOverride() && zArr[i2] == notificationRecord2.canShowBadge() && Objects.equals(arrayList2.get(i2), notificationRecord2.getChannel()) && Objects.equals(arrayList3.get(i2), notificationRecord2.getGroupKey()) && Objects.equals(arrayList4.get(i2), notificationRecord2.getPeopleOverride()) && Objects.equals(arrayList5.get(i2), notificationRecord2.getSnoozeCriteria()) && Objects.equals(arrayList6.get(i2), Integer.valueOf(notificationRecord2.getUserSentiment())) && Objects.equals(arrayList7.get(i2), Integer.valueOf(notificationRecord2.getSuppressedVisualEffects()))) {
                }
                this.mHandler.scheduleSendRankingUpdate();
                return;
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private void recordCallerLocked(NotificationRecord notificationRecord) {
        if (this.mZenModeHelper.isCall(notificationRecord)) {
            this.mZenModeHelper.recordCaller(notificationRecord);
        }
    }

    @GuardedBy("mNotificationLock")
    private void applyZenModeLocked(NotificationRecord notificationRecord) {
        notificationRecord.setIntercepted(this.mZenModeHelper.shouldIntercept(notificationRecord));
        if (notificationRecord.isIntercepted()) {
            notificationRecord.setSuppressedVisualEffects(this.mZenModeHelper.getNotificationPolicy().suppressedVisualEffects);
        } else {
            notificationRecord.setSuppressedVisualEffects(0);
        }
    }

    @GuardedBy("mNotificationLock")
    private int findNotificationRecordIndexLocked(NotificationRecord notificationRecord) {
        return this.mRankingHelper.indexOf(this.mNotificationList, notificationRecord);
    }

    private void handleSendRankingUpdate() {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyRankingUpdateLocked(null);
        }
    }

    private void scheduleListenerHintsChanged(int i) {
        this.mHandler.removeMessages(5);
        this.mHandler.obtainMessage(5, i, 0).sendToTarget();
    }

    private void scheduleInterruptionFilterChanged(int i) {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, i, 0).sendToTarget();
    }

    private void handleListenerHintsChanged(int i) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyListenerHintsChangedLocked(i);
        }
    }

    private void handleListenerInterruptionFilterChanged(int i) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyInterruptionFilterChanged(i);
        }
    }

    protected class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 2:
                    NotificationManagerService.this.handleDurationReached((ToastRecord) message.obj);
                    break;
                case 3:
                    NotificationManagerService.this.handleSavePolicyFile();
                    break;
                case 4:
                    NotificationManagerService.this.handleSendRankingUpdate();
                    break;
                case 5:
                    NotificationManagerService.this.handleListenerHintsChanged(message.arg1);
                    break;
                case 6:
                    NotificationManagerService.this.handleListenerInterruptionFilterChanged(message.arg1);
                    break;
                case 7:
                    NotificationManagerService.this.handleKillTokenTimeout((IBinder) message.obj);
                    break;
            }
        }

        protected void scheduleSendRankingUpdate() {
            if (!hasMessages(4)) {
                sendMessage(Message.obtain(this, 4));
            }
        }
    }

    private final class RankingHandlerWorker extends Handler implements RankingHandler {
        public RankingHandlerWorker(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1000:
                    NotificationManagerService.this.handleRankingReconsideration(message);
                    break;
                case 1001:
                    NotificationManagerService.this.handleRankingSort();
                    break;
            }
        }

        @Override
        public void requestSort() {
            removeMessages(1001);
            Message messageObtain = Message.obtain();
            messageObtain.what = 1001;
            sendMessage(messageObtain);
        }

        @Override
        public void requestReconsideration(RankingReconsideration rankingReconsideration) {
            sendMessageDelayed(Message.obtain(this, 1000, rankingReconsideration), rankingReconsideration.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    static int clamp(int i, int i2, int i3) {
        return i < i2 ? i2 : i > i3 ? i3 : i;
    }

    void sendAccessibilityEvent(NotificationRecord notificationRecord) {
        boolean z;
        if (!this.mAccessibilityManager.isEnabled()) {
            return;
        }
        Notification notification = notificationRecord.getNotification();
        String packageName = notificationRecord.sbn.getPackageName();
        AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(64);
        accessibilityEventObtain.setPackageName(packageName);
        accessibilityEventObtain.setClassName(Notification.class.getName());
        int packageVisibilityOverride = notificationRecord.getPackageVisibilityOverride();
        if (packageVisibilityOverride == -1000) {
            packageVisibilityOverride = notification.visibility;
        }
        int identifier = notificationRecord.getUser().getIdentifier();
        if (identifier < 0 || !this.mKeyguardManager.isDeviceLocked(identifier)) {
            z = false;
        } else {
            z = true;
        }
        if (z && packageVisibilityOverride != 1) {
            accessibilityEventObtain.setParcelableData(notification.publicVersion);
        } else {
            accessibilityEventObtain.setParcelableData(notification);
        }
        CharSequence charSequence = notification.tickerText;
        if (!TextUtils.isEmpty(charSequence)) {
            accessibilityEventObtain.getText().add(charSequence);
        }
        this.mAccessibilityManager.sendAccessibilityEvent(accessibilityEventObtain);
    }

    @GuardedBy("mNotificationLock")
    private boolean removeFromNotificationListsLocked(NotificationRecord notificationRecord) {
        boolean z;
        NotificationRecord notificationRecordFindNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, notificationRecord.getKey());
        if (notificationRecordFindNotificationByListLocked != null) {
            this.mNotificationList.remove(notificationRecordFindNotificationByListLocked);
            this.mNotificationsByKey.remove(notificationRecordFindNotificationByListLocked.sbn.getKey());
            z = true;
        } else {
            z = false;
        }
        while (true) {
            NotificationRecord notificationRecordFindNotificationByListLocked2 = findNotificationByListLocked(this.mEnqueuedNotifications, notificationRecord.getKey());
            if (notificationRecordFindNotificationByListLocked2 != null) {
                this.mEnqueuedNotifications.remove(notificationRecordFindNotificationByListLocked2);
            } else {
                return z;
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private void cancelNotificationLocked(NotificationRecord notificationRecord, boolean z, int i, boolean z2, String str) {
        cancelNotificationLocked(notificationRecord, z, i, -1, -1, z2, str);
    }

    @GuardedBy("mNotificationLock")
    private void cancelNotificationLocked(final NotificationRecord notificationRecord, boolean z, int i, int i2, int i3, boolean z2, String str) {
        long jClearCallingIdentity;
        String key = notificationRecord.getKey();
        recordCallerLocked(notificationRecord);
        if (notificationRecord.getStats().getDismissalSurface() == -1) {
            notificationRecord.recordDismissalSurface(0);
        }
        if (z && notificationRecord.getNotification().deleteIntent != null) {
            try {
                notificationRecord.getNotification().deleteIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Slog.w(TAG, "canceled PendingIntent for " + notificationRecord.sbn.getPackageName(), e);
            }
        }
        if (z2) {
            if (notificationRecord.getNotification().getSmallIcon() != null) {
                if (i != 18) {
                    notificationRecord.isCanceled = true;
                }
                this.mListeners.notifyRemovedLocked(notificationRecord, i, notificationRecord.getStats());
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationManagerService.this.mGroupHelper.onNotificationRemoved(notificationRecord.sbn);
                    }
                });
            }
            if (key.equals(this.mSoundNotificationKey)) {
                this.mSoundNotificationKey = null;
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    IRingtonePlayer ringtonePlayer = this.mAudioManager.getRingtonePlayer();
                    if (ringtonePlayer != null) {
                        ringtonePlayer.stopAsync();
                    }
                } catch (RemoteException e2) {
                } catch (Throwable th) {
                    throw th;
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
            if (key.equals(this.mVibrateNotificationKey)) {
                this.mVibrateNotificationKey = null;
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mVibrator.cancel();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            this.mLights.remove(key);
        }
        switch (i) {
            default:
                switch (i) {
                    case 8:
                    case 9:
                        this.mUsageStats.registerRemovedByApp(notificationRecord);
                        break;
                }
            case 2:
            case 3:
                this.mUsageStats.registerDismissedByUser(notificationRecord);
                break;
        }
        String groupKey = notificationRecord.getGroupKey();
        NotificationRecord notificationRecord2 = this.mSummaryByGroupKey.get(groupKey);
        if (notificationRecord2 != null && notificationRecord2.getKey().equals(key)) {
            this.mSummaryByGroupKey.remove(groupKey);
        }
        ArrayMap<String, String> arrayMap = this.mAutobundledSummaries.get(Integer.valueOf(notificationRecord.sbn.getUserId()));
        if (arrayMap != null && notificationRecord.sbn.getKey().equals(arrayMap.get(notificationRecord.sbn.getPackageName()))) {
            arrayMap.remove(notificationRecord.sbn.getPackageName());
        }
        this.mArchive.record(notificationRecord.sbn);
        long jCurrentTimeMillis = System.currentTimeMillis();
        LogMaker subtype = notificationRecord.getLogMaker(jCurrentTimeMillis).setCategory(128).setType(5).setSubtype(i);
        if (i2 != -1 && i3 != -1) {
            subtype.addTaggedData(798, Integer.valueOf(i2)).addTaggedData(1395, Integer.valueOf(i3));
        }
        MetricsLogger.action(subtype);
        EventLogTags.writeNotificationCanceled(key, i, notificationRecord.getLifespanMs(jCurrentTimeMillis), notificationRecord.getFreshnessMs(jCurrentTimeMillis), notificationRecord.getExposureMs(jCurrentTimeMillis), i2, i3, str);
    }

    @VisibleForTesting
    void updateUriPermissions(NotificationRecord notificationRecord, NotificationRecord notificationRecord2, String str, int i) {
        IBinder iBinder;
        IBinder iBinder2;
        String key = notificationRecord != null ? notificationRecord.getKey() : notificationRecord2.getKey();
        if (DBG) {
            Slog.d(TAG, key + ": updating permissions");
        }
        ArraySet<Uri> grantableUris = notificationRecord != null ? notificationRecord.getGrantableUris() : null;
        ArraySet<Uri> grantableUris2 = notificationRecord2 != null ? notificationRecord2.getGrantableUris() : null;
        if (grantableUris == null && grantableUris2 == null) {
            return;
        }
        if (notificationRecord != null) {
            iBinder = notificationRecord.permissionOwner;
        } else {
            iBinder = null;
        }
        if (notificationRecord2 != null && iBinder == null) {
            iBinder = notificationRecord2.permissionOwner;
        }
        IBinder iBinderNewUriPermissionOwner = iBinder;
        if (grantableUris != null && iBinderNewUriPermissionOwner == null) {
            try {
                if (DBG) {
                    Slog.d(TAG, key + ": creating owner");
                }
                iBinderNewUriPermissionOwner = this.mAm.newUriPermissionOwner("NOTIF:" + key);
            } catch (RemoteException e) {
            }
        }
        if (grantableUris == null && iBinderNewUriPermissionOwner != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (DBG) {
                    Slog.d(TAG, key + ": destroying owner");
                }
                this.mAm.revokeUriPermissionFromOwner(iBinderNewUriPermissionOwner, (Uri) null, -1, UserHandle.getUserId(notificationRecord2.getUid()));
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                iBinder2 = null;
            } catch (RemoteException e2) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                iBinder2 = iBinderNewUriPermissionOwner;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } else {
            iBinder2 = iBinderNewUriPermissionOwner;
        }
        if (grantableUris != null && iBinder2 != null) {
            for (int i2 = 0; i2 < grantableUris.size(); i2++) {
                Uri uriValueAt = grantableUris.valueAt(i2);
                if (grantableUris2 == null || !grantableUris2.contains(uriValueAt)) {
                    if (DBG) {
                        Slog.d(TAG, key + ": granting " + uriValueAt);
                    }
                    grantUriPermission(iBinder2, uriValueAt, notificationRecord.getUid(), str, i);
                }
            }
        }
        if (grantableUris2 != null && iBinder2 != null) {
            for (int i3 = 0; i3 < grantableUris2.size(); i3++) {
                Uri uriValueAt2 = grantableUris2.valueAt(i3);
                if (grantableUris == null || !grantableUris.contains(uriValueAt2)) {
                    if (DBG) {
                        Slog.d(TAG, key + ": revoking " + uriValueAt2);
                    }
                    revokeUriPermission(iBinder2, uriValueAt2, notificationRecord2.getUid());
                }
            }
        }
        if (notificationRecord != null) {
            notificationRecord.permissionOwner = iBinder2;
        }
    }

    private void grantUriPermission(IBinder iBinder, Uri uri, int i, String str, int i2) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAm.grantUriPermissionFromOwner(iBinder, i, str, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i)), i2);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private void revokeUriPermission(IBinder iBinder, Uri uri, int i) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAm.revokeUriPermissionFromOwner(iBinder, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i)));
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    void cancelNotification(int i, int i2, String str, String str2, int i3, int i4, int i5, boolean z, int i6, int i7, ManagedServices.ManagedServiceInfo managedServiceInfo) {
        cancelNotification(i, i2, str, str2, i3, i4, i5, z, i6, i7, -1, -1, managedServiceInfo);
    }

    void cancelNotification(final int i, final int i2, final String str, final String str2, final int i3, final int i4, final int i5, final boolean z, final int i6, final int i7, final int i8, final int i9, final ManagedServices.ManagedServiceInfo managedServiceInfo) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                String shortString = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
                if (NotificationManagerService.DBG) {
                    EventLogTags.writeNotificationCancel(i, i2, str, i3, str2, i6, i4, i5, i7, shortString);
                }
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    if (NotificationManagerService.this.findNotificationByListLocked(NotificationManagerService.this.mEnqueuedNotifications, str, str2, i3, i6) != null) {
                        NotificationManagerService.this.mHandler.post(this);
                    }
                    NotificationRecord notificationRecordFindNotificationByListLocked = NotificationManagerService.this.findNotificationByListLocked(NotificationManagerService.this.mNotificationList, str, str2, i3, i6);
                    if (notificationRecordFindNotificationByListLocked != null) {
                        if (i7 == 1) {
                            NotificationManagerService.this.mUsageStats.registerClickedByUser(notificationRecordFindNotificationByListLocked);
                        }
                        if ((notificationRecordFindNotificationByListLocked.getNotification().flags & i4) != i4) {
                            return;
                        }
                        if ((notificationRecordFindNotificationByListLocked.getNotification().flags & i5) != 0) {
                            return;
                        }
                        NotificationManagerService.this.cancelNotificationLocked(notificationRecordFindNotificationByListLocked, z, i7, i8, i9, NotificationManagerService.this.removeFromNotificationListsLocked(notificationRecordFindNotificationByListLocked), shortString);
                        NotificationManagerService.this.cancelGroupChildrenLocked(notificationRecordFindNotificationByListLocked, i, i2, shortString, z, null);
                        NotificationManagerService.this.updateLightsLocked();
                    } else if (i7 != 18 && NotificationManagerService.this.mSnoozeHelper.cancel(i6, str, str2, i3)) {
                        NotificationManagerService.this.savePolicyFile();
                    }
                }
            }
        });
    }

    private boolean notificationMatchesUserId(NotificationRecord notificationRecord, int i) {
        return i == -1 || notificationRecord.getUserId() == -1 || notificationRecord.getUserId() == i;
    }

    private boolean notificationMatchesCurrentProfiles(NotificationRecord notificationRecord, int i) {
        return notificationMatchesUserId(notificationRecord, i) || this.mUserProfiles.isCurrentProfile(notificationRecord.getUserId());
    }

    class AnonymousClass15 implements Runnable {
        final int val$callingPid;
        final int val$callingUid;
        final String val$channelId;
        final boolean val$doit;
        final ManagedServices.ManagedServiceInfo val$listener;
        final int val$mustHaveFlags;
        final int val$mustNotHaveFlags;
        final String val$pkg;
        final int val$reason;
        final int val$userId;

        AnonymousClass15(ManagedServices.ManagedServiceInfo managedServiceInfo, int i, int i2, String str, int i3, int i4, int i5, int i6, boolean z, String str2) {
            this.val$listener = managedServiceInfo;
            this.val$callingUid = i;
            this.val$callingPid = i2;
            this.val$pkg = str;
            this.val$userId = i3;
            this.val$mustHaveFlags = i4;
            this.val$mustNotHaveFlags = i5;
            this.val$reason = i6;
            this.val$doit = z;
            this.val$channelId = str2;
        }

        @Override
        public void run() throws Throwable {
            String shortString = this.val$listener == null ? null : this.val$listener.component.toShortString();
            EventLogTags.writeNotificationCancelAll(this.val$callingUid, this.val$callingPid, this.val$pkg, this.val$userId, this.val$mustHaveFlags, this.val$mustNotHaveFlags, this.val$reason, shortString);
            if (!this.val$doit) {
                return;
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                try {
                    try {
                        final int i = this.val$mustHaveFlags;
                        final int i2 = this.val$mustNotHaveFlags;
                        FlagChecker flagChecker = new FlagChecker() {
                            @Override
                            public final boolean apply(int i3) {
                                return NotificationManagerService.AnonymousClass15.lambda$run$0(i, i2, i3);
                            }
                        };
                        NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mNotificationList, this.val$callingUid, this.val$callingPid, this.val$pkg, true, this.val$channelId, flagChecker, false, this.val$userId, false, this.val$reason, shortString, true);
                        NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mEnqueuedNotifications, this.val$callingUid, this.val$callingPid, this.val$pkg, true, this.val$channelId, flagChecker, false, this.val$userId, false, this.val$reason, shortString, false);
                        NotificationManagerService.this.mSnoozeHelper.cancel(this.val$userId, this.val$pkg);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        static boolean lambda$run$0(int i, int i2, int i3) {
            return (i3 & i) == i && (i3 & i2) == 0;
        }
    }

    void cancelAllNotificationsInt(int i, int i2, String str, String str2, int i3, int i4, boolean z, int i5, int i6, ManagedServices.ManagedServiceInfo managedServiceInfo) {
        this.mHandler.post(new AnonymousClass15(managedServiceInfo, i, i2, str, i5, i3, i4, i6, z, str2));
    }

    @GuardedBy("mNotificationLock")
    private void cancelAllNotificationsByListLocked(ArrayList<NotificationRecord> arrayList, int i, int i2, String str, boolean z, String str2, FlagChecker flagChecker, boolean z2, int i3, boolean z3, int i4, String str3, boolean z4) {
        ArrayList arrayList2 = null;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            NotificationRecord notificationRecord = arrayList.get(size);
            if (!z2 ? notificationMatchesUserId(notificationRecord, i3) : notificationMatchesCurrentProfiles(notificationRecord, i3)) {
                if (!z || str != null || notificationRecord.getUserId() != -1) {
                    if (flagChecker.apply(notificationRecord.getFlags()) && ((str == null || notificationRecord.sbn.getPackageName().equals(str)) && (str2 == null || str2.equals(notificationRecord.getChannel().getId())))) {
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList();
                        }
                        arrayList.remove(size);
                        this.mNotificationsByKey.remove(notificationRecord.getKey());
                        arrayList2.add(notificationRecord);
                        cancelNotificationLocked(notificationRecord, z3, i4, z4, str3);
                    }
                }
            }
        }
        if (arrayList2 != null) {
            int size2 = arrayList2.size();
            for (int i5 = 0; i5 < size2; i5++) {
                cancelGroupChildrenLocked((NotificationRecord) arrayList2.get(i5), i, i2, str3, false, flagChecker);
            }
            updateLightsLocked();
        }
    }

    void snoozeNotificationInt(String str, long j, String str2, ManagedServices.ManagedServiceInfo managedServiceInfo) {
        String shortString = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
        if ((j <= 0 && str2 == null) || str == null) {
            return;
        }
        if (DBG) {
            Slog.d(TAG, String.format("snooze event(%s, %d, %s, %s)", str, Long.valueOf(j), str2, shortString));
        }
        this.mHandler.post(new SnoozeNotificationRunnable(str, j, str2));
    }

    void unsnoozeNotificationInt(String str, ManagedServices.ManagedServiceInfo managedServiceInfo) {
        String shortString = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
        if (DBG) {
            Slog.d(TAG, String.format("unsnooze event(%s, %s)", str, shortString));
        }
        this.mSnoozeHelper.repost(str);
        savePolicyFile();
    }

    class AnonymousClass16 implements Runnable {
        final int val$callingPid;
        final int val$callingUid;
        final boolean val$includeCurrentProfiles;
        final ManagedServices.ManagedServiceInfo val$listener;
        final int val$reason;
        final int val$userId;

        AnonymousClass16(ManagedServices.ManagedServiceInfo managedServiceInfo, int i, int i2, int i3, int i4, boolean z) {
            this.val$listener = managedServiceInfo;
            this.val$callingUid = i;
            this.val$callingPid = i2;
            this.val$userId = i3;
            this.val$reason = i4;
            this.val$includeCurrentProfiles = z;
        }

        @Override
        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                String shortString = this.val$listener == null ? null : this.val$listener.component.toShortString();
                EventLogTags.writeNotificationCancelAll(this.val$callingUid, this.val$callingPid, null, this.val$userId, 0, 0, this.val$reason, shortString);
                $$Lambda$NotificationManagerService$16$he13RdFo2qbqR04oK0hiGU5GDUg __lambda_notificationmanagerservice_16_he13rdfo2qbqr04ok0higu5gdug = new FlagChecker() {
                    @Override
                    public final boolean apply(int i) {
                        return NotificationManagerService.AnonymousClass16.lambda$run$0(i);
                    }
                };
                NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mNotificationList, this.val$callingUid, this.val$callingPid, null, false, null, __lambda_notificationmanagerservice_16_he13rdfo2qbqr04ok0higu5gdug, this.val$includeCurrentProfiles, this.val$userId, true, this.val$reason, shortString, true);
                NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mEnqueuedNotifications, this.val$callingUid, this.val$callingPid, null, false, null, __lambda_notificationmanagerservice_16_he13rdfo2qbqr04ok0higu5gdug, this.val$includeCurrentProfiles, this.val$userId, true, this.val$reason, shortString, false);
                NotificationManagerService.this.mSnoozeHelper.cancel(this.val$userId, this.val$includeCurrentProfiles);
            }
        }

        static boolean lambda$run$0(int i) {
            if ((i & 34) != 0) {
                return false;
            }
            return true;
        }
    }

    @GuardedBy("mNotificationLock")
    void cancelAllLocked(int i, int i2, int i3, int i4, ManagedServices.ManagedServiceInfo managedServiceInfo, boolean z) {
        this.mHandler.post(new AnonymousClass16(managedServiceInfo, i, i2, i3, i4, z));
    }

    @GuardedBy("mNotificationLock")
    private void cancelGroupChildrenLocked(NotificationRecord notificationRecord, int i, int i2, String str, boolean z, FlagChecker flagChecker) {
        if (!notificationRecord.getNotification().isGroupSummary()) {
            return;
        }
        if (notificationRecord.sbn.getPackageName() != null) {
            cancelGroupChildrenByListLocked(this.mNotificationList, notificationRecord, i, i2, str, z, true, flagChecker);
            cancelGroupChildrenByListLocked(this.mEnqueuedNotifications, notificationRecord, i, i2, str, z, false, flagChecker);
        } else if (DBG) {
            Log.e(TAG, "No package for group summary: " + notificationRecord.getKey());
        }
    }

    @GuardedBy("mNotificationLock")
    private void cancelGroupChildrenByListLocked(ArrayList<NotificationRecord> arrayList, NotificationRecord notificationRecord, int i, int i2, String str, boolean z, boolean z2, FlagChecker flagChecker) {
        FlagChecker flagChecker2 = flagChecker;
        String packageName = notificationRecord.sbn.getPackageName();
        int userId = notificationRecord.getUserId();
        int size = arrayList.size() - 1;
        while (size >= 0) {
            NotificationRecord notificationRecord2 = arrayList.get(size);
            StatusBarNotification statusBarNotification = notificationRecord2.sbn;
            if (statusBarNotification.isGroup() && !statusBarNotification.getNotification().isGroupSummary() && notificationRecord2.getGroupKey().equals(notificationRecord.getGroupKey()) && (notificationRecord2.getFlags() & 64) == 0 && (flagChecker2 == null || flagChecker2.apply(notificationRecord2.getFlags()))) {
                EventLogTags.writeNotificationCancel(i, i2, packageName, statusBarNotification.getId(), statusBarNotification.getTag(), userId, 0, 0, 12, str);
                arrayList.remove(size);
                this.mNotificationsByKey.remove(notificationRecord2.getKey());
                cancelNotificationLocked(notificationRecord2, z, 12, z2, str);
            }
            size--;
            flagChecker2 = flagChecker;
        }
    }

    @GuardedBy("mNotificationLock")
    void updateLightsLocked() {
        NotificationRecord notificationRecord = null;
        while (notificationRecord == null && !this.mLights.isEmpty()) {
            String str = this.mLights.get(this.mLights.size() - 1);
            NotificationRecord notificationRecord2 = this.mNotificationsByKey.get(str);
            if (notificationRecord2 == null) {
                Slog.wtfStack(TAG, "LED Notification does not exist: " + str);
                this.mLights.remove(str);
            }
            notificationRecord = notificationRecord2;
        }
        if (notificationRecord == null || this.mInCall || this.mScreenOn || this.mMtkPplManager.getPplLockStatus()) {
            this.mNotificationLight.turnOff();
            return;
        }
        NotificationRecord.Light light = notificationRecord.getLight();
        if (light != null && this.mNotificationPulseEnabled) {
            this.mNotificationLight.setFlashing(light.color, 1, light.onMs, light.offMs);
        }
    }

    @GuardedBy("mNotificationLock")
    List<NotificationRecord> findGroupNotificationsLocked(String str, String str2, int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(findGroupNotificationByListLocked(this.mNotificationList, str, str2, i));
        arrayList.addAll(findGroupNotificationByListLocked(this.mEnqueuedNotifications, str, str2, i));
        return arrayList;
    }

    @GuardedBy("mNotificationLock")
    private List<NotificationRecord> findGroupNotificationByListLocked(ArrayList<NotificationRecord> arrayList, String str, String str2, int i) {
        ArrayList arrayList2 = new ArrayList();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotificationRecord notificationRecord = arrayList.get(i2);
            if (notificationMatchesUserId(notificationRecord, i) && notificationRecord.getGroupKey().equals(str2) && notificationRecord.sbn.getPackageName().equals(str)) {
                arrayList2.add(notificationRecord);
            }
        }
        return arrayList2;
    }

    @GuardedBy("mNotificationLock")
    private NotificationRecord findNotificationByKeyLocked(String str) {
        NotificationRecord notificationRecordFindNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, str);
        if (notificationRecordFindNotificationByListLocked != null) {
            return notificationRecordFindNotificationByListLocked;
        }
        NotificationRecord notificationRecordFindNotificationByListLocked2 = findNotificationByListLocked(this.mEnqueuedNotifications, str);
        if (notificationRecordFindNotificationByListLocked2 != null) {
            return notificationRecordFindNotificationByListLocked2;
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    NotificationRecord findNotificationLocked(String str, String str2, int i, int i2) {
        NotificationRecord notificationRecordFindNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, str, str2, i, i2);
        if (notificationRecordFindNotificationByListLocked != null) {
            return notificationRecordFindNotificationByListLocked;
        }
        NotificationRecord notificationRecordFindNotificationByListLocked2 = findNotificationByListLocked(this.mEnqueuedNotifications, str, str2, i, i2);
        if (notificationRecordFindNotificationByListLocked2 != null) {
            return notificationRecordFindNotificationByListLocked2;
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    private NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> arrayList, String str, String str2, int i, int i2) {
        int size = arrayList.size();
        for (int i3 = 0; i3 < size; i3++) {
            NotificationRecord notificationRecord = arrayList.get(i3);
            if (notificationMatchesUserId(notificationRecord, i2) && notificationRecord.sbn.getId() == i && TextUtils.equals(notificationRecord.sbn.getTag(), str2) && notificationRecord.sbn.getPackageName().equals(str)) {
                return notificationRecord;
            }
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    private NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> arrayList, String str) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (str.equals(arrayList.get(i).getKey())) {
                return arrayList.get(i);
            }
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    int indexOfNotificationLocked(String str) {
        int size = this.mNotificationList.size();
        for (int i = 0; i < size; i++) {
            if (str.equals(this.mNotificationList.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    @VisibleForTesting
    protected void hideNotificationsForPackages(String[] strArr) {
        synchronized (this.mNotificationLock) {
            List listAsList = Arrays.asList(strArr);
            ArrayList arrayList = new ArrayList();
            int size = this.mNotificationList.size();
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = this.mNotificationList.get(i);
                if (listAsList.contains(notificationRecord.sbn.getPackageName())) {
                    notificationRecord.setHidden(true);
                    arrayList.add(notificationRecord);
                }
            }
            this.mListeners.notifyHiddenLocked(arrayList);
        }
    }

    @VisibleForTesting
    protected void unhideNotificationsForPackages(String[] strArr) {
        synchronized (this.mNotificationLock) {
            List listAsList = Arrays.asList(strArr);
            ArrayList arrayList = new ArrayList();
            int size = this.mNotificationList.size();
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = this.mNotificationList.get(i);
                if (listAsList.contains(notificationRecord.sbn.getPackageName())) {
                    notificationRecord.setHidden(false);
                    arrayList.add(notificationRecord);
                }
            }
            this.mListeners.notifyUnhiddenLocked(arrayList);
        }
    }

    private void updateNotificationPulse() {
        synchronized (this.mNotificationLock) {
            updateLightsLocked();
        }
    }

    protected boolean isCallingUidSystem() {
        return Binder.getCallingUid() == 1000;
    }

    protected boolean isUidSystemOrPhone(int i) {
        int appId = UserHandle.getAppId(i);
        return appId == 1000 || appId == 1001 || i == 0;
    }

    protected boolean isCallerSystemOrPhone() {
        return isUidSystemOrPhone(Binder.getCallingUid());
    }

    private void checkCallerIsSystemOrShell() {
        if (Binder.getCallingUid() == 2000) {
            return;
        }
        checkCallerIsSystem();
    }

    private void checkCallerIsSystem() {
        if (isCallerSystemOrPhone()) {
            return;
        }
        throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
    }

    private void checkCallerIsSystemOrSameApp(String str) {
        if (isCallerSystemOrPhone()) {
            return;
        }
        checkCallerIsSameApp(str);
    }

    private boolean isCallerInstantApp(String str) {
        if (isCallerSystemOrPhone()) {
            return false;
        }
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 0, UserHandle.getCallingUserId());
            if (applicationInfo == null) {
                throw new SecurityException("Unknown package " + str);
            }
            return applicationInfo.isInstantApp();
        } catch (RemoteException e) {
            throw new SecurityException("Unknown package " + str, e);
        }
    }

    private void checkCallerIsSameApp(String str) {
        int callingUid = Binder.getCallingUid();
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 0, UserHandle.getCallingUserId());
            if (applicationInfo == null) {
                throw new SecurityException("Unknown package " + str);
            }
            if (!UserHandle.isSameApp(applicationInfo.uid, callingUid)) {
                throw new SecurityException("Calling uid " + callingUid + " gave package " + str + " which is owned by uid " + applicationInfo.uid);
            }
        } catch (RemoteException e) {
            throw new SecurityException("Unknown package " + str + "\n" + e);
        }
    }

    private static String callStateToString(int i) {
        switch (i) {
            case 0:
                return "CALL_STATE_IDLE";
            case 1:
                return "CALL_STATE_RINGING";
            case 2:
                return "CALL_STATE_OFFHOOK";
            default:
                return "CALL_STATE_UNKNOWN_" + i;
        }
    }

    private void listenForCallState() {
        TelephonyManager.from(getContext()).listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int i, String str) {
                if (NotificationManagerService.this.mCallState == i) {
                    return;
                }
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "Call state changed: " + NotificationManagerService.callStateToString(i));
                }
                NotificationManagerService.this.mCallState = i;
            }
        }, 32);
    }

    @GuardedBy("mNotificationLock")
    private NotificationRankingUpdate makeRankingUpdateLocked(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        Bundle bundle;
        NotificationManagerService notificationManagerService = this;
        int size = notificationManagerService.mNotificationList.size();
        ArrayList arrayList = new ArrayList(size);
        ArrayList arrayList2 = new ArrayList(size);
        ArrayList arrayList3 = new ArrayList(size);
        Bundle bundle2 = new Bundle();
        Bundle bundle3 = new Bundle();
        Bundle bundle4 = new Bundle();
        Bundle bundle5 = new Bundle();
        Bundle bundle6 = new Bundle();
        Bundle bundle7 = new Bundle();
        Bundle bundle8 = new Bundle();
        Bundle bundle9 = new Bundle();
        Bundle bundle10 = new Bundle();
        Bundle bundle11 = new Bundle();
        int i = 0;
        while (i < size) {
            int i2 = size;
            NotificationRecord notificationRecord = notificationManagerService.mNotificationList.get(i);
            int i3 = i;
            Bundle bundle12 = bundle11;
            if (!notificationManagerService.isVisibleToListener(notificationRecord.sbn, managedServiceInfo)) {
                bundle = bundle12;
            } else {
                String key = notificationRecord.sbn.getKey();
                arrayList.add(key);
                arrayList3.add(Integer.valueOf(notificationRecord.getImportance()));
                if (notificationRecord.getImportanceExplanation() != null) {
                    bundle5.putCharSequence(key, notificationRecord.getImportanceExplanation());
                }
                if (notificationRecord.isIntercepted()) {
                    arrayList2.add(key);
                }
                bundle4.putInt(key, notificationRecord.getSuppressedVisualEffects());
                if (notificationRecord.getPackageVisibilityOverride() != -1000) {
                    bundle3.putInt(key, notificationRecord.getPackageVisibilityOverride());
                }
                bundle2.putString(key, notificationRecord.sbn.getOverrideGroupKey());
                bundle6.putParcelable(key, notificationRecord.getChannel());
                bundle7.putStringArrayList(key, notificationRecord.getPeopleOverride());
                bundle8.putParcelableArrayList(key, notificationRecord.getSnoozeCriteria());
                bundle9.putBoolean(key, notificationRecord.canShowBadge());
                bundle10.putInt(key, notificationRecord.getUserSentiment());
                boolean zIsHidden = notificationRecord.isHidden();
                bundle = bundle12;
                bundle.putBoolean(key, zIsHidden);
            }
            i = i3 + 1;
            bundle11 = bundle;
            size = i2;
            notificationManagerService = this;
        }
        Bundle bundle13 = bundle11;
        int size2 = arrayList.size();
        String[] strArr = (String[]) arrayList.toArray(new String[size2]);
        String[] strArr2 = (String[]) arrayList2.toArray(new String[arrayList2.size()]);
        int[] iArr = new int[size2];
        int i4 = 0;
        while (i4 < size2) {
            iArr[i4] = ((Integer) arrayList3.get(i4)).intValue();
            i4++;
            size2 = size2;
        }
        return new NotificationRankingUpdate(strArr, strArr2, bundle3, bundle4, iArr, bundle5, bundle2, bundle6, bundle7, bundle8, bundle9, bundle10, bundle13);
    }

    boolean hasCompanionDevice(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        if (this.mCompanionManager == null) {
            this.mCompanionManager = getCompanionManager();
        }
        if (this.mCompanionManager == null) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    if (!ArrayUtils.isEmpty(this.mCompanionManager.getAssociations(managedServiceInfo.component.getPackageName(), managedServiceInfo.userid))) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Cannot reach companion device service", e);
                }
            } catch (SecurityException e2) {
            } catch (Exception e3) {
                Slog.e(TAG, "Cannot verify listener " + managedServiceInfo, e3);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected ICompanionDeviceManager getCompanionManager() {
        return ICompanionDeviceManager.Stub.asInterface(ServiceManager.getService("companiondevice"));
    }

    private boolean isVisibleToListener(StatusBarNotification statusBarNotification, ManagedServices.ManagedServiceInfo managedServiceInfo) {
        if (!managedServiceInfo.enabledAndUserMatches(statusBarNotification.getUserId())) {
            return false;
        }
        return true;
    }

    private boolean isPackageSuspendedForUser(String str, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                boolean zIsPackageSuspendedForUser = this.mPackageManager.isPackageSuspendedForUser(str, UserHandle.getUserId(i));
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return zIsPackageSuspendedForUser;
            } catch (RemoteException e) {
                throw new SecurityException("Could not talk to package manager service");
            } catch (IllegalArgumentException e2) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    @VisibleForTesting
    boolean canUseManagedServices(String str) {
        boolean z = !this.mActivityManager.isLowRamDevice() || this.mPackageManagerClient.hasSystemFeature("android.hardware.type.watch");
        for (String str2 : getContext().getResources().getStringArray(R.array.common_nicknames)) {
            if (str2.equals(str)) {
                z = true;
            }
        }
        return z;
    }

    private class TrimCache {
        StatusBarNotification heavy;
        StatusBarNotification sbnClone;
        StatusBarNotification sbnCloneLight;

        TrimCache(StatusBarNotification statusBarNotification) {
            this.heavy = statusBarNotification;
        }

        StatusBarNotification ForListener(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            if (NotificationManagerService.this.mListeners.getOnNotificationPostedTrim(managedServiceInfo) == 1) {
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

    public class NotificationAssistants extends ManagedServices {
        static final String TAG_ENABLED_NOTIFICATION_ASSISTANTS = "enabled_assistants";

        public NotificationAssistants(Context context, Object obj, ManagedServices.UserProfiles userProfiles, IPackageManager iPackageManager) {
            super(context, obj, userProfiles, iPackageManager);
        }

        @Override
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config config = new ManagedServices.Config();
            config.caption = "notification assistant";
            config.serviceInterface = "android.service.notification.NotificationAssistantService";
            config.xmlTag = TAG_ENABLED_NOTIFICATION_ASSISTANTS;
            config.secureSettingName = "enabled_notification_assistant";
            config.bindPermission = "android.permission.BIND_NOTIFICATION_ASSISTANT_SERVICE";
            config.settingsAction = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";
            config.clientLabel = R.string.face_error_lockout_permanent;
            return config;
        }

        @Override
        protected IInterface asInterface(IBinder iBinder) {
            return INotificationListener.Stub.asInterface(iBinder);
        }

        @Override
        protected boolean checkType(IInterface iInterface) {
            return iInterface instanceof INotificationListener;
        }

        @Override
        protected void onServiceAdded(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            NotificationManagerService.this.mListeners.registerGuestService(managedServiceInfo);
        }

        @Override
        @GuardedBy("mNotificationLock")
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            NotificationManagerService.this.mListeners.unregisterService(managedServiceInfo.service, managedServiceInfo.userid);
        }

        @Override
        public void onUserUnlocked(int i) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "onUserUnlocked u=" + i);
            }
            rebindServices(true);
        }

        public void onNotificationEnqueued(NotificationRecord notificationRecord) {
            StatusBarNotification statusBarNotification = notificationRecord.sbn;
            TrimCache trimCache = NotificationManagerService.this.new TrimCache(statusBarNotification);
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(statusBarNotification, managedServiceInfo)) {
                    final StatusBarNotification statusBarNotificationForListener = trimCache.ForListener(managedServiceInfo);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationAssistants.this.notifyEnqueued(managedServiceInfo, statusBarNotificationForListener);
                        }
                    });
                }
            }
        }

        private void notifyEnqueued(ManagedServices.ManagedServiceInfo managedServiceInfo, StatusBarNotification statusBarNotification) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationEnqueued(new StatusBarNotificationHolder(statusBarNotification));
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify assistant (enqueued): " + iNotificationListener, e);
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyAssistantSnoozedLocked(StatusBarNotification statusBarNotification, final String str) {
            TrimCache trimCache = NotificationManagerService.this.new TrimCache(statusBarNotification);
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(statusBarNotification, managedServiceInfo)) {
                    final StatusBarNotification statusBarNotificationForListener = trimCache.ForListener(managedServiceInfo);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            INotificationListener iNotificationListener = managedServiceInfo.service;
                            try {
                                iNotificationListener.onNotificationSnoozedUntilContext(new StatusBarNotificationHolder(statusBarNotificationForListener), str);
                            } catch (RemoteException e) {
                                Log.e(NotificationAssistants.this.TAG, "unable to notify assistant (snoozed): " + iNotificationListener, e);
                            }
                        }
                    });
                }
            }
        }

        public boolean isEnabled() {
            return !getServices().isEmpty();
        }

        protected void ensureAssistant() {
            Iterator it = this.mUm.getUsers(true).iterator();
            while (it.hasNext()) {
                int identifier = ((UserInfo) it.next()).getUserHandle().getIdentifier();
                if (getAllowedPackages(identifier).isEmpty()) {
                    Slog.d(this.TAG, "Approving default notification assistant for user " + identifier);
                    NotificationManagerService.this.readDefaultAssistant(identifier);
                }
            }
        }
    }

    public class NotificationListeners extends ManagedServices {
        static final String TAG_ENABLED_NOTIFICATION_LISTENERS = "enabled_listeners";
        private final ArraySet<ManagedServices.ManagedServiceInfo> mLightTrimListeners;

        public NotificationListeners(IPackageManager iPackageManager) {
            super(NotificationManagerService.this.getContext(), NotificationManagerService.this.mNotificationLock, NotificationManagerService.this.mUserProfiles, iPackageManager);
            this.mLightTrimListeners = new ArraySet<>();
        }

        @Override
        protected ManagedServices.Config getConfig() {
            ManagedServices.Config config = new ManagedServices.Config();
            config.caption = "notification listener";
            config.serviceInterface = "android.service.notification.NotificationListenerService";
            config.xmlTag = TAG_ENABLED_NOTIFICATION_LISTENERS;
            config.secureSettingName = "enabled_notification_listeners";
            config.bindPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
            config.settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
            config.clientLabel = R.string.face_error_hw_not_present;
            return config;
        }

        @Override
        protected IInterface asInterface(IBinder iBinder) {
            return INotificationListener.Stub.asInterface(iBinder);
        }

        @Override
        protected boolean checkType(IInterface iInterface) {
            return iInterface instanceof INotificationListener;
        }

        @Override
        public void onServiceAdded(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            NotificationRankingUpdate notificationRankingUpdateMakeRankingUpdateLocked;
            INotificationListener iNotificationListener = managedServiceInfo.service;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                notificationRankingUpdateMakeRankingUpdateLocked = NotificationManagerService.this.makeRankingUpdateLocked(managedServiceInfo);
            }
            try {
                iNotificationListener.onListenerConnected(notificationRankingUpdateMakeRankingUpdateLocked);
            } catch (RemoteException e) {
            }
        }

        @Override
        @GuardedBy("mNotificationLock")
        protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            if (NotificationManagerService.this.removeDisabledHints(managedServiceInfo)) {
                NotificationManagerService.this.updateListenerHintsLocked();
                NotificationManagerService.this.updateEffectsSuppressorLocked();
            }
            this.mLightTrimListeners.remove(managedServiceInfo);
        }

        @GuardedBy("mNotificationLock")
        public void setOnNotificationPostedTrimLocked(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
            if (i == 1) {
                this.mLightTrimListeners.add(managedServiceInfo);
            } else {
                this.mLightTrimListeners.remove(managedServiceInfo);
            }
        }

        public int getOnNotificationPostedTrim(ManagedServices.ManagedServiceInfo managedServiceInfo) {
            return this.mLightTrimListeners.contains(managedServiceInfo) ? 1 : 0;
        }

        @GuardedBy("mNotificationLock")
        public void notifyPostedLocked(NotificationRecord notificationRecord, NotificationRecord notificationRecord2) {
            notifyPostedLocked(notificationRecord, notificationRecord2, true);
        }

        @GuardedBy("mNotificationLock")
        private void notifyPostedLocked(NotificationRecord notificationRecord, NotificationRecord notificationRecord2, boolean z) {
            boolean zIsVisibleToListener;
            StatusBarNotification statusBarNotification = notificationRecord.sbn;
            StatusBarNotification statusBarNotification2 = notificationRecord2 != null ? notificationRecord2.sbn : null;
            TrimCache trimCache = NotificationManagerService.this.new TrimCache(statusBarNotification);
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                boolean zIsVisibleToListener2 = NotificationManagerService.this.isVisibleToListener(statusBarNotification, managedServiceInfo);
                if (statusBarNotification2 != null) {
                    zIsVisibleToListener = NotificationManagerService.this.isVisibleToListener(statusBarNotification2, managedServiceInfo);
                } else {
                    zIsVisibleToListener = false;
                }
                if (zIsVisibleToListener || zIsVisibleToListener2) {
                    if (!notificationRecord.isHidden() || managedServiceInfo.targetSdkVersion >= 28) {
                        if (z || managedServiceInfo.targetSdkVersion < 28) {
                            final NotificationRankingUpdate notificationRankingUpdateMakeRankingUpdateLocked = NotificationManagerService.this.makeRankingUpdateLocked(managedServiceInfo);
                            if (zIsVisibleToListener && !zIsVisibleToListener2) {
                                final StatusBarNotification statusBarNotificationCloneLight = statusBarNotification2.cloneLight();
                                NotificationManagerService.this.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationListeners.this.notifyRemoved(managedServiceInfo, statusBarNotificationCloneLight, notificationRankingUpdateMakeRankingUpdateLocked, null, 6);
                                    }
                                });
                            } else {
                                NotificationManagerService.this.updateUriPermissions(notificationRecord, notificationRecord2, managedServiceInfo.component.getPackageName(), managedServiceInfo.userid != -1 ? managedServiceInfo.userid : 0);
                                final StatusBarNotification statusBarNotificationForListener = trimCache.ForListener(managedServiceInfo);
                                NotificationManagerService.this.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationListeners.this.notifyPosted(managedServiceInfo, statusBarNotificationForListener, notificationRankingUpdateMakeRankingUpdateLocked);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyRemovedLocked(final NotificationRecord notificationRecord, final int i, NotificationStats notificationStats) {
            StatusBarNotification statusBarNotification = notificationRecord.sbn;
            final StatusBarNotification statusBarNotificationCloneLight = statusBarNotification.cloneLight();
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(statusBarNotification, managedServiceInfo) && (!notificationRecord.isHidden() || i == 14 || managedServiceInfo.targetSdkVersion >= 28)) {
                    if (i != 14 || managedServiceInfo.targetSdkVersion < 28) {
                        final NotificationStats notificationStats2 = NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(managedServiceInfo.service) ? notificationStats : null;
                        final NotificationRankingUpdate notificationRankingUpdateMakeRankingUpdateLocked = NotificationManagerService.this.makeRankingUpdateLocked(managedServiceInfo);
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyRemoved(managedServiceInfo, statusBarNotificationCloneLight, notificationRankingUpdateMakeRankingUpdateLocked, notificationStats2, i);
                            }
                        });
                    }
                }
            }
            NotificationManagerService.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    NotificationManagerService.this.updateUriPermissions(null, notificationRecord, null, 0);
                }
            });
        }

        @GuardedBy("mNotificationLock")
        public void notifyRankingUpdateLocked(List<NotificationRecord> list) {
            boolean z;
            boolean z2 = list != null && list.size() > 0;
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (managedServiceInfo.isEnabledForCurrentProfiles()) {
                    if (z2 && managedServiceInfo.targetSdkVersion >= 28) {
                        Iterator<NotificationRecord> it = list.iterator();
                        while (it.hasNext()) {
                            if (NotificationManagerService.this.isVisibleToListener(it.next().sbn, managedServiceInfo)) {
                                z = true;
                                break;
                            }
                        }
                        z = false;
                        if (!z) {
                        }
                        final NotificationRankingUpdate notificationRankingUpdateMakeRankingUpdateLocked = NotificationManagerService.this.makeRankingUpdateLocked(managedServiceInfo);
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationListeners.this.notifyRankingUpdate(managedServiceInfo, notificationRankingUpdateMakeRankingUpdateLocked);
                            }
                        });
                    } else {
                        z = false;
                        if (!z || !z2) {
                            final NotificationRankingUpdate notificationRankingUpdateMakeRankingUpdateLocked2 = NotificationManagerService.this.makeRankingUpdateLocked(managedServiceInfo);
                            NotificationManagerService.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationListeners.this.notifyRankingUpdate(managedServiceInfo, notificationRankingUpdateMakeRankingUpdateLocked2);
                                }
                            });
                        }
                    }
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyListenerHintsChangedLocked(final int i) {
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (managedServiceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyListenerHintsChanged(managedServiceInfo, i);
                        }
                    });
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyHiddenLocked(List<NotificationRecord> list) {
            if (list == null || list.size() == 0) {
                return;
            }
            notifyRankingUpdateLocked(list);
            int size = list.size();
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = list.get(i);
                NotificationManagerService.this.mListeners.notifyRemovedLocked(notificationRecord, 14, notificationRecord.getStats());
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyUnhiddenLocked(List<NotificationRecord> list) {
            if (list == null || list.size() == 0) {
                return;
            }
            notifyRankingUpdateLocked(list);
            int size = list.size();
            for (int i = 0; i < size; i++) {
                NotificationRecord notificationRecord = list.get(i);
                NotificationManagerService.this.mListeners.notifyPostedLocked(notificationRecord, notificationRecord, false);
            }
        }

        public void notifyInterruptionFilterChanged(final int i) {
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (managedServiceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationListeners.this.notifyInterruptionFilterChanged(managedServiceInfo, i);
                        }
                    });
                }
            }
        }

        protected void notifyNotificationChannelChanged(final String str, final UserHandle userHandle, final NotificationChannel notificationChannel, final int i) {
            if (notificationChannel == null) {
                return;
            }
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (managedServiceInfo.enabledAndUserMatches(UserHandle.getCallingUserId())) {
                    BackgroundThread.getHandler().post(new Runnable() {
                        @Override
                        public final void run() {
                            NotificationManagerService.NotificationListeners.lambda$notifyNotificationChannelChanged$1(this.f$0, managedServiceInfo, str, userHandle, notificationChannel, i);
                        }
                    });
                }
            }
        }

        public static void lambda$notifyNotificationChannelChanged$1(NotificationListeners notificationListeners, ManagedServices.ManagedServiceInfo managedServiceInfo, String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) {
            if (NotificationManagerService.this.hasCompanionDevice(managedServiceInfo)) {
                notificationListeners.notifyNotificationChannelChanged(managedServiceInfo, str, userHandle, notificationChannel, i);
            }
        }

        protected void notifyNotificationChannelGroupChanged(final String str, final UserHandle userHandle, final NotificationChannelGroup notificationChannelGroup, final int i) {
            if (notificationChannelGroup == null) {
                return;
            }
            for (final ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
                if (managedServiceInfo.enabledAndUserMatches(UserHandle.getCallingUserId())) {
                    BackgroundThread.getHandler().post(new Runnable() {
                        @Override
                        public final void run() {
                            NotificationManagerService.NotificationListeners.lambda$notifyNotificationChannelGroupChanged$2(this.f$0, managedServiceInfo, str, userHandle, notificationChannelGroup, i);
                        }
                    });
                }
            }
        }

        public static void lambda$notifyNotificationChannelGroupChanged$2(NotificationListeners notificationListeners, ManagedServices.ManagedServiceInfo managedServiceInfo, String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) {
            if (NotificationManagerService.this.hasCompanionDevice(managedServiceInfo)) {
                notificationListeners.notifyNotificationChannelGroupChanged(managedServiceInfo, str, userHandle, notificationChannelGroup, i);
            }
        }

        private void notifyPosted(ManagedServices.ManagedServiceInfo managedServiceInfo, StatusBarNotification statusBarNotification, NotificationRankingUpdate notificationRankingUpdate) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationPosted(new StatusBarNotificationHolder(statusBarNotification), notificationRankingUpdate);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (posted): " + iNotificationListener, e);
            }
        }

        private void notifyRemoved(ManagedServices.ManagedServiceInfo managedServiceInfo, StatusBarNotification statusBarNotification, NotificationRankingUpdate notificationRankingUpdate, NotificationStats notificationStats, int i) {
            if (!managedServiceInfo.enabledAndUserMatches(statusBarNotification.getUserId())) {
                return;
            }
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationRemoved(new StatusBarNotificationHolder(statusBarNotification), notificationRankingUpdate, notificationStats, i);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (removed): " + iNotificationListener, e);
            }
        }

        private void notifyRankingUpdate(ManagedServices.ManagedServiceInfo managedServiceInfo, NotificationRankingUpdate notificationRankingUpdate) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationRankingUpdate(notificationRankingUpdate);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (ranking update): " + iNotificationListener, e);
            }
        }

        private void notifyListenerHintsChanged(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onListenerHintsChanged(i);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (listener hints): " + iNotificationListener, e);
            }
        }

        private void notifyInterruptionFilterChanged(ManagedServices.ManagedServiceInfo managedServiceInfo, int i) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onInterruptionFilterChanged(i);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (interruption filter): " + iNotificationListener, e);
            }
        }

        void notifyNotificationChannelChanged(ManagedServices.ManagedServiceInfo managedServiceInfo, String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationChannelModification(str, userHandle, notificationChannel, i);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (channel changed): " + iNotificationListener, e);
            }
        }

        private void notifyNotificationChannelGroupChanged(ManagedServices.ManagedServiceInfo managedServiceInfo, String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) {
            INotificationListener iNotificationListener = managedServiceInfo.service;
            try {
                iNotificationListener.onNotificationChannelGroupModification(str, userHandle, notificationChannelGroup, i);
            } catch (RemoteException e) {
                Log.e(this.TAG, "unable to notify listener (channel group changed): " + iNotificationListener, e);
            }
        }

        public boolean isListenerPackage(String str) {
            if (str == null) {
                return false;
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                Iterator<ManagedServices.ManagedServiceInfo> it = getServices().iterator();
                while (it.hasNext()) {
                    if (str.equals(it.next().component.getPackageName())) {
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
        public boolean proto = false;
        public boolean criticalPriority = false;
        public boolean normalPriority = false;

        public static DumpFilter parseFromArguments(String[] strArr) {
            DumpFilter dumpFilter = new DumpFilter();
            int i = 0;
            while (i < strArr.length) {
                String str = strArr[i];
                if (PriorityDump.PROTO_ARG.equals(str)) {
                    dumpFilter.proto = true;
                } else if ("--noredact".equals(str) || "--reveal".equals(str)) {
                    dumpFilter.redact = false;
                } else if ("p".equals(str) || "pkg".equals(str) || "--package".equals(str)) {
                    if (i < strArr.length - 1) {
                        i++;
                        dumpFilter.pkgFilter = strArr[i].trim().toLowerCase();
                        if (dumpFilter.pkgFilter.isEmpty()) {
                            dumpFilter.pkgFilter = null;
                        } else {
                            dumpFilter.filtered = true;
                        }
                    }
                } else if ("--zen".equals(str) || "zen".equals(str)) {
                    dumpFilter.filtered = true;
                    dumpFilter.zen = true;
                } else if ("--stats".equals(str)) {
                    dumpFilter.stats = true;
                    if (i < strArr.length - 1) {
                        i++;
                        dumpFilter.since = Long.parseLong(strArr[i]);
                    } else {
                        dumpFilter.since = 0L;
                    }
                } else if (PriorityDump.PRIORITY_ARG.equals(str) && i < strArr.length - 1) {
                    i++;
                    String str2 = strArr[i];
                    byte b = -1;
                    int iHashCode = str2.hashCode();
                    if (iHashCode != -1986416409) {
                        if (iHashCode == -1560189025 && str2.equals(PriorityDump.PRIORITY_ARG_CRITICAL)) {
                            b = 0;
                        }
                    } else if (str2.equals(PriorityDump.PRIORITY_ARG_NORMAL)) {
                        b = 1;
                    }
                    switch (b) {
                        case 0:
                            dumpFilter.criticalPriority = true;
                            break;
                        case 1:
                            dumpFilter.normalPriority = true;
                            break;
                    }
                }
                i++;
            }
            return dumpFilter;
        }

        public boolean matches(StatusBarNotification statusBarNotification) {
            if (this.filtered && !this.zen) {
                return statusBarNotification != null && (matches(statusBarNotification.getPackageName()) || matches(statusBarNotification.getOpPkg()));
            }
            return true;
        }

        public boolean matches(ComponentName componentName) {
            if (this.filtered && !this.zen) {
                return componentName != null && matches(componentName.getPackageName());
            }
            return true;
        }

        public boolean matches(String str) {
            if (this.filtered && !this.zen) {
                return str != null && str.toLowerCase().contains(this.pkgFilter);
            }
            return true;
        }

        public String toString() {
            if (this.stats) {
                return "stats";
            }
            if (this.zen) {
                return "zen";
            }
            return '\'' + this.pkgFilter + '\'';
        }
    }

    @VisibleForTesting
    protected void simulatePackageSuspendBroadcast(boolean z, String str) {
        checkCallerIsSystemOrShell();
        Bundle bundle = new Bundle();
        bundle.putStringArray("android.intent.extra.changed_package_list", new String[]{str});
        Intent intent = new Intent(z ? "android.intent.action.PACKAGES_SUSPENDED" : "android.intent.action.PACKAGES_UNSUSPENDED");
        intent.putExtras(bundle);
        this.mPackageIntentReceiver.onReceive(getContext(), intent);
    }

    private static final class StatusBarNotificationHolder extends IStatusBarNotificationHolder.Stub {
        private StatusBarNotification mValue;

        public StatusBarNotificationHolder(StatusBarNotification statusBarNotification) {
            this.mValue = statusBarNotification;
        }

        public StatusBarNotification get() {
            StatusBarNotification statusBarNotification = this.mValue;
            this.mValue = null;
            return statusBarNotification;
        }
    }

    private class ShellCmd extends ShellCommand {
        public static final String USAGE = "help\nallow_listener COMPONENT [user_id]\ndisallow_listener COMPONENT [user_id]\nallow_assistant COMPONENT\nremove_assistant COMPONENT\nallow_dnd PACKAGE\ndisallow_dnd PACKAGE\nsuspend_package PACKAGE\nunsuspend_package PACKAGE";

        private ShellCmd() {
        }

        ShellCmd(NotificationManagerService notificationManagerService, AnonymousClass1 anonymousClass1) {
            this();
        }

        public int onCommand(String str) {
            byte b;
            if (str == null) {
                return handleDefaultCommands(str);
            }
            PrintWriter outPrintWriter = getOutPrintWriter();
            try {
                switch (str.hashCode()) {
                    case -1325770982:
                        b = !str.equals("disallow_assistant") ? (byte) -1 : (byte) 5;
                        break;
                    case -506770550:
                        if (str.equals("unsuspend_package")) {
                            b = 7;
                            break;
                        }
                        break;
                    case -432999190:
                        if (str.equals("allow_listener")) {
                            b = 2;
                            break;
                        }
                        break;
                    case -429832618:
                        if (str.equals("disallow_dnd")) {
                            b = 1;
                            break;
                        }
                        break;
                    case 372345636:
                        if (str.equals("allow_dnd")) {
                            b = 0;
                            break;
                        }
                        break;
                    case 393969475:
                        if (str.equals("suspend_package")) {
                            b = 6;
                            break;
                        }
                        break;
                    case 1257269496:
                        if (str.equals("disallow_listener")) {
                            b = 3;
                            break;
                        }
                        break;
                    case 2110474600:
                        if (str.equals("allow_assistant")) {
                            b = 4;
                            break;
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                outPrintWriter.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e(NotificationManagerService.TAG, "Error running shell command", e);
            }
            switch (b) {
                case 0:
                    NotificationManagerService.this.getBinderService().setNotificationPolicyAccessGranted(getNextArgRequired(), true);
                    return 0;
                case 1:
                    NotificationManagerService.this.getBinderService().setNotificationPolicyAccessGranted(getNextArgRequired(), false);
                    return 0;
                case 2:
                    ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(getNextArgRequired());
                    if (componentNameUnflattenFromString == null) {
                        outPrintWriter.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                    String nextArg = getNextArg();
                    if (nextArg == null) {
                        NotificationManagerService.this.getBinderService().setNotificationListenerAccessGranted(componentNameUnflattenFromString, true);
                    } else {
                        NotificationManagerService.this.getBinderService().setNotificationListenerAccessGrantedForUser(componentNameUnflattenFromString, Integer.parseInt(nextArg), true);
                    }
                    return 0;
                case 3:
                    ComponentName componentNameUnflattenFromString2 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (componentNameUnflattenFromString2 == null) {
                        outPrintWriter.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                    String nextArg2 = getNextArg();
                    if (nextArg2 == null) {
                        NotificationManagerService.this.getBinderService().setNotificationListenerAccessGranted(componentNameUnflattenFromString2, false);
                    } else {
                        NotificationManagerService.this.getBinderService().setNotificationListenerAccessGrantedForUser(componentNameUnflattenFromString2, Integer.parseInt(nextArg2), false);
                    }
                    return 0;
                case 4:
                    ComponentName componentNameUnflattenFromString3 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (componentNameUnflattenFromString3 == null) {
                        outPrintWriter.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    }
                    NotificationManagerService.this.getBinderService().setNotificationAssistantAccessGranted(componentNameUnflattenFromString3, true);
                    return 0;
                case 5:
                    ComponentName componentNameUnflattenFromString4 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (componentNameUnflattenFromString4 != null) {
                        NotificationManagerService.this.getBinderService().setNotificationAssistantAccessGranted(componentNameUnflattenFromString4, false);
                        return 0;
                    }
                    outPrintWriter.println("Invalid assistant - must be a ComponentName");
                    return -1;
                case 6:
                    NotificationManagerService.this.simulatePackageSuspendBroadcast(true, getNextArgRequired());
                    return 0;
                case 7:
                    NotificationManagerService.this.simulatePackageSuspendBroadcast(false, getNextArgRequired());
                    return 0;
                default:
                    return handleDefaultCommands(str);
            }
        }

        public void onHelp() {
            getOutPrintWriter().println(USAGE);
        }
    }
}
