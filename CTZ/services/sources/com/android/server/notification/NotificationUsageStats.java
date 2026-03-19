package com.android.server.notification;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.server.BatteryService;
import com.android.server.am.AssistDataRequester;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationUsageStats {
    private static final boolean DEBUG = false;
    private static final String DEVICE_GLOBAL_STATS = "__global";
    private static final long EMIT_PERIOD = 14400000;
    private static final AggregatedStats[] EMPTY_AGGREGATED_STATS = new AggregatedStats[0];
    private static final boolean ENABLE_AGGREGATED_IN_MEMORY_STATS = true;
    private static final boolean ENABLE_SQLITE_LOG = true;
    public static final int FOUR_HOURS = 14400000;
    private static final int MSG_EMIT = 1;
    private static final String TAG = "NotificationUsageStats";
    public static final int TEN_SECONDS = 10000;
    private final Context mContext;
    private final Handler mHandler;
    private final SQLiteLog mSQLiteLog;
    private final Map<String, AggregatedStats> mStats = new HashMap();
    private final ArrayDeque<AggregatedStats[]> mStatsArrays = new ArrayDeque<>();
    private ArraySet<String> mStatExpiredkeys = new ArraySet<>();
    private long mLastEmitTime = SystemClock.elapsedRealtime();

    public NotificationUsageStats(Context context) {
        this.mContext = context;
        this.mSQLiteLog = new SQLiteLog(context);
        this.mHandler = new Handler(this.mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    NotificationUsageStats.this.emit();
                    return;
                }
                Log.wtf(NotificationUsageStats.TAG, "Unknown message type: " + message.what);
            }
        };
        this.mHandler.sendEmptyMessageDelayed(1, 14400000L);
    }

    public synchronized float getAppEnqueueRate(String str) {
        AggregatedStats orCreateAggregatedStatsLocked = getOrCreateAggregatedStatsLocked(str);
        if (orCreateAggregatedStatsLocked != null) {
            return orCreateAggregatedStatsLocked.getEnqueueRate(SystemClock.elapsedRealtime());
        }
        return 0.0f;
    }

    public synchronized boolean isAlertRateLimited(String str) {
        AggregatedStats orCreateAggregatedStatsLocked = getOrCreateAggregatedStatsLocked(str);
        if (orCreateAggregatedStatsLocked != null) {
            return orCreateAggregatedStatsLocked.isAlertRateLimited();
        }
        return false;
    }

    public synchronized void registerEnqueuedByApp(String str) {
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(str);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numEnqueuedByApp++;
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
    }

    public synchronized void registerPostedByApp(NotificationRecord notificationRecord) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        notificationRecord.stats.posttimeElapsedMs = jElapsedRealtime;
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numPostedByApp++;
            aggregatedStats.updateInterarrivalEstimate(jElapsedRealtime);
            aggregatedStats.countApiUse(notificationRecord);
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
        this.mSQLiteLog.logPosted(notificationRecord);
    }

    public synchronized void registerUpdatedByApp(NotificationRecord notificationRecord, NotificationRecord notificationRecord2) {
        notificationRecord.stats.updateFrom(notificationRecord2.stats);
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numUpdatedByApp++;
            aggregatedStats.updateInterarrivalEstimate(SystemClock.elapsedRealtime());
            aggregatedStats.countApiUse(notificationRecord);
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
        this.mSQLiteLog.logPosted(notificationRecord);
    }

    public synchronized void registerRemovedByApp(NotificationRecord notificationRecord) {
        notificationRecord.stats.onRemoved();
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numRemovedByApp++;
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
        this.mSQLiteLog.logRemoved(notificationRecord);
    }

    public synchronized void registerDismissedByUser(NotificationRecord notificationRecord) {
        MetricsLogger.histogram(this.mContext, "note_dismiss_longevity", ((int) (System.currentTimeMillis() - notificationRecord.getRankingTimeMs())) / 60000);
        notificationRecord.stats.onDismiss();
        this.mSQLiteLog.logDismissed(notificationRecord);
    }

    public synchronized void registerClickedByUser(NotificationRecord notificationRecord) {
        MetricsLogger.histogram(this.mContext, "note_click_longevity", ((int) (System.currentTimeMillis() - notificationRecord.getRankingTimeMs())) / 60000);
        notificationRecord.stats.onClick();
        this.mSQLiteLog.logClicked(notificationRecord);
    }

    public synchronized void registerPeopleAffinity(NotificationRecord notificationRecord, boolean z, boolean z2, boolean z3) {
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            if (z) {
                aggregatedStats.numWithValidPeople++;
            }
            if (z2) {
                aggregatedStats.numWithStaredPeople++;
            }
            if (z3) {
                aggregatedStats.numPeopleCacheHit++;
            } else {
                aggregatedStats.numPeopleCacheMiss++;
            }
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
    }

    public synchronized void registerBlocked(NotificationRecord notificationRecord) {
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numBlocked++;
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
    }

    public synchronized void registerSuspendedByAdmin(NotificationRecord notificationRecord) {
        AggregatedStats[] aggregatedStatsLocked = getAggregatedStatsLocked(notificationRecord);
        for (AggregatedStats aggregatedStats : aggregatedStatsLocked) {
            aggregatedStats.numSuspendedByAdmin++;
        }
        releaseAggregatedStatsLocked(aggregatedStatsLocked);
    }

    public synchronized void registerOverRateQuota(String str) {
        for (AggregatedStats aggregatedStats : getAggregatedStatsLocked(str)) {
            aggregatedStats.numRateViolations++;
        }
    }

    public synchronized void registerOverCountQuota(String str) {
        for (AggregatedStats aggregatedStats : getAggregatedStatsLocked(str)) {
            aggregatedStats.numQuotaViolations++;
        }
    }

    private AggregatedStats[] getAggregatedStatsLocked(NotificationRecord notificationRecord) {
        return getAggregatedStatsLocked(notificationRecord.sbn.getPackageName());
    }

    private AggregatedStats[] getAggregatedStatsLocked(String str) {
        AggregatedStats[] aggregatedStatsArrPoll = this.mStatsArrays.poll();
        if (aggregatedStatsArrPoll == null) {
            aggregatedStatsArrPoll = new AggregatedStats[2];
        }
        aggregatedStatsArrPoll[0] = getOrCreateAggregatedStatsLocked(DEVICE_GLOBAL_STATS);
        aggregatedStatsArrPoll[1] = getOrCreateAggregatedStatsLocked(str);
        return aggregatedStatsArrPoll;
    }

    private void releaseAggregatedStatsLocked(AggregatedStats[] aggregatedStatsArr) {
        for (int i = 0; i < aggregatedStatsArr.length; i++) {
            aggregatedStatsArr[i] = null;
        }
        this.mStatsArrays.offer(aggregatedStatsArr);
    }

    private AggregatedStats getOrCreateAggregatedStatsLocked(String str) {
        AggregatedStats aggregatedStats = this.mStats.get(str);
        if (aggregatedStats == null) {
            aggregatedStats = new AggregatedStats(this.mContext, str);
            this.mStats.put(str, aggregatedStats);
        }
        aggregatedStats.mLastAccessTime = SystemClock.elapsedRealtime();
        return aggregatedStats;
    }

    public synchronized JSONObject dumpJson(NotificationManagerService.DumpFilter dumpFilter) {
        JSONObject jSONObject;
        jSONObject = new JSONObject();
        try {
            JSONArray jSONArray = new JSONArray();
            for (AggregatedStats aggregatedStats : this.mStats.values()) {
                if (dumpFilter == null || dumpFilter.matches(aggregatedStats.key)) {
                    jSONArray.put(aggregatedStats.dumpJson());
                }
            }
            jSONObject.put("current", jSONArray);
        } catch (JSONException e) {
        }
        try {
            jSONObject.put("historical", this.mSQLiteLog.dumpJson(dumpFilter));
        } catch (JSONException e2) {
        }
        return jSONObject;
    }

    public synchronized void dump(PrintWriter printWriter, String str, NotificationManagerService.DumpFilter dumpFilter) {
        for (AggregatedStats aggregatedStats : this.mStats.values()) {
            if (dumpFilter == null || dumpFilter.matches(aggregatedStats.key)) {
                aggregatedStats.dump(printWriter, str);
            }
        }
        printWriter.println(str + "mStatsArrays.size(): " + this.mStatsArrays.size());
        printWriter.println(str + "mStats.size(): " + this.mStats.size());
        this.mSQLiteLog.dump(printWriter, str, dumpFilter);
    }

    public synchronized void emit() {
        getOrCreateAggregatedStatsLocked(DEVICE_GLOBAL_STATS).emit();
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 14400000L);
        for (String str : this.mStats.keySet()) {
            if (this.mStats.get(str).mLastAccessTime < this.mLastEmitTime) {
                this.mStatExpiredkeys.add(str);
            }
        }
        Iterator<String> it = this.mStatExpiredkeys.iterator();
        while (it.hasNext()) {
            this.mStats.remove(it.next());
        }
        this.mStatExpiredkeys.clear();
        this.mLastEmitTime = SystemClock.elapsedRealtime();
    }

    private static class AggregatedStats {
        public ImportanceHistogram finalImportance;
        public final String key;
        private final Context mContext;
        public long mLastAccessTime;
        private AggregatedStats mPrevious;
        public ImportanceHistogram noisyImportance;
        public int numAlertViolations;
        public int numAutoCancel;
        public int numBlocked;
        public int numEnqueuedByApp;
        public int numForegroundService;
        public int numInterrupt;
        public int numOngoing;
        public int numPeopleCacheHit;
        public int numPeopleCacheMiss;
        public int numPostedByApp;
        public int numPrivate;
        public int numQuotaViolations;
        public int numRateViolations;
        public int numRemovedByApp;
        public int numSecret;
        public int numSuspendedByAdmin;
        public int numUpdatedByApp;
        public int numWithActions;
        public int numWithBigPicture;
        public int numWithBigText;
        public int numWithInbox;
        public int numWithInfoText;
        public int numWithLargeIcon;
        public int numWithMediaSession;
        public int numWithStaredPeople;
        public int numWithSubText;
        public int numWithText;
        public int numWithTitle;
        public int numWithValidPeople;
        public ImportanceHistogram quietImportance;
        private final long mCreated = SystemClock.elapsedRealtime();
        public RateEstimator enqueueRate = new RateEstimator();
        public AlertRateLimiter alertRate = new AlertRateLimiter();

        public AggregatedStats(Context context, String str) {
            this.key = str;
            this.mContext = context;
            this.noisyImportance = new ImportanceHistogram(context, "note_imp_noisy_");
            this.quietImportance = new ImportanceHistogram(context, "note_imp_quiet_");
            this.finalImportance = new ImportanceHistogram(context, "note_importance_");
        }

        public AggregatedStats getPrevious() {
            if (this.mPrevious == null) {
                this.mPrevious = new AggregatedStats(this.mContext, this.key);
            }
            return this.mPrevious;
        }

        public void countApiUse(NotificationRecord notificationRecord) {
            Notification notification = notificationRecord.getNotification();
            if (notification.actions != null) {
                this.numWithActions++;
            }
            if ((notification.flags & 64) != 0) {
                this.numForegroundService++;
            }
            if ((notification.flags & 2) != 0) {
                this.numOngoing++;
            }
            if ((notification.flags & 16) != 0) {
                this.numAutoCancel++;
            }
            if ((notification.defaults & 1) != 0 || (notification.defaults & 2) != 0 || notification.sound != null || notification.vibrate != null) {
                this.numInterrupt++;
            }
            switch (notification.visibility) {
                case -1:
                    this.numSecret++;
                    break;
                case 0:
                    this.numPrivate++;
                    break;
            }
            if (notificationRecord.stats.isNoisy) {
                this.noisyImportance.increment(notificationRecord.stats.requestedImportance);
            } else {
                this.quietImportance.increment(notificationRecord.stats.requestedImportance);
            }
            this.finalImportance.increment(notificationRecord.getImportance());
            Set<String> setKeySet = notification.extras.keySet();
            if (setKeySet.contains("android.bigText")) {
                this.numWithBigText++;
            }
            if (setKeySet.contains("android.picture")) {
                this.numWithBigPicture++;
            }
            if (setKeySet.contains("android.largeIcon")) {
                this.numWithLargeIcon++;
            }
            if (setKeySet.contains("android.textLines")) {
                this.numWithInbox++;
            }
            if (setKeySet.contains("android.mediaSession")) {
                this.numWithMediaSession++;
            }
            if (setKeySet.contains("android.title") && !TextUtils.isEmpty(notification.extras.getCharSequence("android.title"))) {
                this.numWithTitle++;
            }
            if (setKeySet.contains("android.text") && !TextUtils.isEmpty(notification.extras.getCharSequence("android.text"))) {
                this.numWithText++;
            }
            if (setKeySet.contains("android.subText") && !TextUtils.isEmpty(notification.extras.getCharSequence("android.subText"))) {
                this.numWithSubText++;
            }
            if (setKeySet.contains("android.infoText") && !TextUtils.isEmpty(notification.extras.getCharSequence("android.infoText"))) {
                this.numWithInfoText++;
            }
        }

        public void emit() {
            AggregatedStats previous = getPrevious();
            maybeCount("note_enqueued", this.numEnqueuedByApp - previous.numEnqueuedByApp);
            maybeCount("note_post", this.numPostedByApp - previous.numPostedByApp);
            maybeCount("note_update", this.numUpdatedByApp - previous.numUpdatedByApp);
            maybeCount("note_remove", this.numRemovedByApp - previous.numRemovedByApp);
            maybeCount("note_with_people", this.numWithValidPeople - previous.numWithValidPeople);
            maybeCount("note_with_stars", this.numWithStaredPeople - previous.numWithStaredPeople);
            maybeCount("people_cache_hit", this.numPeopleCacheHit - previous.numPeopleCacheHit);
            maybeCount("people_cache_miss", this.numPeopleCacheMiss - previous.numPeopleCacheMiss);
            maybeCount("note_blocked", this.numBlocked - previous.numBlocked);
            maybeCount("note_suspended", this.numSuspendedByAdmin - previous.numSuspendedByAdmin);
            maybeCount("note_with_actions", this.numWithActions - previous.numWithActions);
            maybeCount("note_private", this.numPrivate - previous.numPrivate);
            maybeCount("note_secret", this.numSecret - previous.numSecret);
            maybeCount("note_interupt", this.numInterrupt - previous.numInterrupt);
            maybeCount("note_big_text", this.numWithBigText - previous.numWithBigText);
            maybeCount("note_big_pic", this.numWithBigPicture - previous.numWithBigPicture);
            maybeCount("note_fg", this.numForegroundService - previous.numForegroundService);
            maybeCount("note_ongoing", this.numOngoing - previous.numOngoing);
            maybeCount("note_auto", this.numAutoCancel - previous.numAutoCancel);
            maybeCount("note_large_icon", this.numWithLargeIcon - previous.numWithLargeIcon);
            maybeCount("note_inbox", this.numWithInbox - previous.numWithInbox);
            maybeCount("note_media", this.numWithMediaSession - previous.numWithMediaSession);
            maybeCount("note_title", this.numWithTitle - previous.numWithTitle);
            maybeCount("note_text", this.numWithText - previous.numWithText);
            maybeCount("note_sub_text", this.numWithSubText - previous.numWithSubText);
            maybeCount("note_info_text", this.numWithInfoText - previous.numWithInfoText);
            maybeCount("note_over_rate", this.numRateViolations - previous.numRateViolations);
            maybeCount("note_over_alert_rate", this.numAlertViolations - previous.numAlertViolations);
            maybeCount("note_over_quota", this.numQuotaViolations - previous.numQuotaViolations);
            this.noisyImportance.maybeCount(previous.noisyImportance);
            this.quietImportance.maybeCount(previous.quietImportance);
            this.finalImportance.maybeCount(previous.finalImportance);
            previous.numEnqueuedByApp = this.numEnqueuedByApp;
            previous.numPostedByApp = this.numPostedByApp;
            previous.numUpdatedByApp = this.numUpdatedByApp;
            previous.numRemovedByApp = this.numRemovedByApp;
            previous.numPeopleCacheHit = this.numPeopleCacheHit;
            previous.numPeopleCacheMiss = this.numPeopleCacheMiss;
            previous.numWithStaredPeople = this.numWithStaredPeople;
            previous.numWithValidPeople = this.numWithValidPeople;
            previous.numBlocked = this.numBlocked;
            previous.numSuspendedByAdmin = this.numSuspendedByAdmin;
            previous.numWithActions = this.numWithActions;
            previous.numPrivate = this.numPrivate;
            previous.numSecret = this.numSecret;
            previous.numInterrupt = this.numInterrupt;
            previous.numWithBigText = this.numWithBigText;
            previous.numWithBigPicture = this.numWithBigPicture;
            previous.numForegroundService = this.numForegroundService;
            previous.numOngoing = this.numOngoing;
            previous.numAutoCancel = this.numAutoCancel;
            previous.numWithLargeIcon = this.numWithLargeIcon;
            previous.numWithInbox = this.numWithInbox;
            previous.numWithMediaSession = this.numWithMediaSession;
            previous.numWithTitle = this.numWithTitle;
            previous.numWithText = this.numWithText;
            previous.numWithSubText = this.numWithSubText;
            previous.numWithInfoText = this.numWithInfoText;
            previous.numRateViolations = this.numRateViolations;
            previous.numAlertViolations = this.numAlertViolations;
            previous.numQuotaViolations = this.numQuotaViolations;
            this.noisyImportance.update(previous.noisyImportance);
            this.quietImportance.update(previous.quietImportance);
            this.finalImportance.update(previous.finalImportance);
        }

        void maybeCount(String str, int i) {
            if (i > 0) {
                MetricsLogger.count(this.mContext, str, i);
            }
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(toStringWithIndent(str));
        }

        public String toString() {
            return toStringWithIndent(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }

        public float getEnqueueRate() {
            return getEnqueueRate(SystemClock.elapsedRealtime());
        }

        public float getEnqueueRate(long j) {
            return this.enqueueRate.getRate(j);
        }

        public void updateInterarrivalEstimate(long j) {
            this.enqueueRate.update(j);
        }

        public boolean isAlertRateLimited() {
            boolean zShouldRateLimitAlert = this.alertRate.shouldRateLimitAlert(SystemClock.elapsedRealtime());
            if (zShouldRateLimitAlert) {
                this.numAlertViolations++;
            }
            return zShouldRateLimitAlert;
        }

        private String toStringWithIndent(String str) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("AggregatedStats{\n");
            String str2 = str + "  ";
            sb.append(str2);
            sb.append("key='");
            sb.append(this.key);
            sb.append("',\n");
            sb.append(str2);
            sb.append("numEnqueuedByApp=");
            sb.append(this.numEnqueuedByApp);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numPostedByApp=");
            sb.append(this.numPostedByApp);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numUpdatedByApp=");
            sb.append(this.numUpdatedByApp);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numRemovedByApp=");
            sb.append(this.numRemovedByApp);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numPeopleCacheHit=");
            sb.append(this.numPeopleCacheHit);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numWithStaredPeople=");
            sb.append(this.numWithStaredPeople);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numWithValidPeople=");
            sb.append(this.numWithValidPeople);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numPeopleCacheMiss=");
            sb.append(this.numPeopleCacheMiss);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numBlocked=");
            sb.append(this.numBlocked);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numSuspendedByAdmin=");
            sb.append(this.numSuspendedByAdmin);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numWithActions=");
            sb.append(this.numWithActions);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numPrivate=");
            sb.append(this.numPrivate);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numSecret=");
            sb.append(this.numSecret);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numInterrupt=");
            sb.append(this.numInterrupt);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numWithBigText=");
            sb.append(this.numWithBigText);
            sb.append(",\n");
            sb.append(str2);
            sb.append("numWithBigPicture=");
            sb.append(this.numWithBigPicture);
            sb.append("\n");
            sb.append(str2);
            sb.append("numForegroundService=");
            sb.append(this.numForegroundService);
            sb.append("\n");
            sb.append(str2);
            sb.append("numOngoing=");
            sb.append(this.numOngoing);
            sb.append("\n");
            sb.append(str2);
            sb.append("numAutoCancel=");
            sb.append(this.numAutoCancel);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithLargeIcon=");
            sb.append(this.numWithLargeIcon);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithInbox=");
            sb.append(this.numWithInbox);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithMediaSession=");
            sb.append(this.numWithMediaSession);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithTitle=");
            sb.append(this.numWithTitle);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithText=");
            sb.append(this.numWithText);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithSubText=");
            sb.append(this.numWithSubText);
            sb.append("\n");
            sb.append(str2);
            sb.append("numWithInfoText=");
            sb.append(this.numWithInfoText);
            sb.append("\n");
            sb.append(str2);
            sb.append("numRateViolations=");
            sb.append(this.numRateViolations);
            sb.append("\n");
            sb.append(str2);
            sb.append("numAlertViolations=");
            sb.append(this.numAlertViolations);
            sb.append("\n");
            sb.append(str2);
            sb.append("numQuotaViolations=");
            sb.append(this.numQuotaViolations);
            sb.append("\n");
            sb.append(str2);
            sb.append(this.noisyImportance.toString());
            sb.append("\n");
            sb.append(str2);
            sb.append(this.quietImportance.toString());
            sb.append("\n");
            sb.append(str2);
            sb.append(this.finalImportance.toString());
            sb.append("\n");
            sb.append(str);
            sb.append("}");
            return sb.toString();
        }

        public JSONObject dumpJson() throws JSONException {
            AggregatedStats previous = getPrevious();
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("key", this.key);
            jSONObject.put("duration", SystemClock.elapsedRealtime() - this.mCreated);
            maybePut(jSONObject, "numEnqueuedByApp", this.numEnqueuedByApp);
            maybePut(jSONObject, "numPostedByApp", this.numPostedByApp);
            maybePut(jSONObject, "numUpdatedByApp", this.numUpdatedByApp);
            maybePut(jSONObject, "numRemovedByApp", this.numRemovedByApp);
            maybePut(jSONObject, "numPeopleCacheHit", this.numPeopleCacheHit);
            maybePut(jSONObject, "numPeopleCacheMiss", this.numPeopleCacheMiss);
            maybePut(jSONObject, "numWithStaredPeople", this.numWithStaredPeople);
            maybePut(jSONObject, "numWithValidPeople", this.numWithValidPeople);
            maybePut(jSONObject, "numBlocked", this.numBlocked);
            maybePut(jSONObject, "numSuspendedByAdmin", this.numSuspendedByAdmin);
            maybePut(jSONObject, "numWithActions", this.numWithActions);
            maybePut(jSONObject, "numPrivate", this.numPrivate);
            maybePut(jSONObject, "numSecret", this.numSecret);
            maybePut(jSONObject, "numInterrupt", this.numInterrupt);
            maybePut(jSONObject, "numWithBigText", this.numWithBigText);
            maybePut(jSONObject, "numWithBigPicture", this.numWithBigPicture);
            maybePut(jSONObject, "numForegroundService", this.numForegroundService);
            maybePut(jSONObject, "numOngoing", this.numOngoing);
            maybePut(jSONObject, "numAutoCancel", this.numAutoCancel);
            maybePut(jSONObject, "numWithLargeIcon", this.numWithLargeIcon);
            maybePut(jSONObject, "numWithInbox", this.numWithInbox);
            maybePut(jSONObject, "numWithMediaSession", this.numWithMediaSession);
            maybePut(jSONObject, "numWithTitle", this.numWithTitle);
            maybePut(jSONObject, "numWithText", this.numWithText);
            maybePut(jSONObject, "numWithSubText", this.numWithSubText);
            maybePut(jSONObject, "numWithInfoText", this.numWithInfoText);
            maybePut(jSONObject, "numRateViolations", this.numRateViolations);
            maybePut(jSONObject, "numQuotaLViolations", this.numQuotaViolations);
            maybePut(jSONObject, "notificationEnqueueRate", getEnqueueRate());
            maybePut(jSONObject, "numAlertViolations", this.numAlertViolations);
            this.noisyImportance.maybePut(jSONObject, previous.noisyImportance);
            this.quietImportance.maybePut(jSONObject, previous.quietImportance);
            this.finalImportance.maybePut(jSONObject, previous.finalImportance);
            return jSONObject;
        }

        private void maybePut(JSONObject jSONObject, String str, int i) throws JSONException {
            if (i > 0) {
                jSONObject.put(str, i);
            }
        }

        private void maybePut(JSONObject jSONObject, String str, float f) throws JSONException {
            double d = f;
            if (d > 0.0d) {
                jSONObject.put(str, d);
            }
        }
    }

    private static class ImportanceHistogram {
        private static final String[] IMPORTANCE_NAMES = {"none", "min", "low", BatteryService.HealthServiceWrapper.INSTANCE_VENDOR, "high", "max"};
        private static final int NUM_IMPORTANCES = 6;
        private final Context mContext;
        private int[] mCount = new int[6];
        private final String[] mCounterNames = new String[6];
        private final String mPrefix;

        ImportanceHistogram(Context context, String str) {
            this.mContext = context;
            this.mPrefix = str;
            for (int i = 0; i < 6; i++) {
                this.mCounterNames[i] = this.mPrefix + IMPORTANCE_NAMES[i];
            }
        }

        void increment(int i) {
            int iMax = Math.max(0, Math.min(i, this.mCount.length - 1));
            int[] iArr = this.mCount;
            iArr[iMax] = iArr[iMax] + 1;
        }

        void maybeCount(ImportanceHistogram importanceHistogram) {
            for (int i = 0; i < 6; i++) {
                int i2 = this.mCount[i] - importanceHistogram.mCount[i];
                if (i2 > 0) {
                    MetricsLogger.count(this.mContext, this.mCounterNames[i], i2);
                }
            }
        }

        void update(ImportanceHistogram importanceHistogram) {
            for (int i = 0; i < 6; i++) {
                this.mCount[i] = importanceHistogram.mCount[i];
            }
        }

        public void maybePut(JSONObject jSONObject, ImportanceHistogram importanceHistogram) throws JSONException {
            jSONObject.put(this.mPrefix, new JSONArray(this.mCount));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mPrefix);
            sb.append(": [");
            for (int i = 0; i < 6; i++) {
                sb.append(this.mCount[i]);
                if (i < 5) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static class SingleNotificationStats {
        public boolean isNoisy;
        public int naturalImportance;
        public int requestedImportance;
        private boolean isVisible = false;
        private boolean isExpanded = false;
        public long posttimeElapsedMs = -1;
        public long posttimeToFirstClickMs = -1;
        public long posttimeToDismissMs = -1;
        public long airtimeCount = 0;
        public long posttimeToFirstAirtimeMs = -1;
        public long currentAirtimeStartElapsedMs = -1;
        public long airtimeMs = 0;
        public long posttimeToFirstVisibleExpansionMs = -1;
        public long currentAirtimeExpandedStartElapsedMs = -1;
        public long airtimeExpandedMs = 0;
        public long userExpansionCount = 0;

        public long getCurrentPosttimeMs() {
            if (this.posttimeElapsedMs < 0) {
                return 0L;
            }
            return SystemClock.elapsedRealtime() - this.posttimeElapsedMs;
        }

        public long getCurrentAirtimeMs() {
            long j = this.airtimeMs;
            if (this.currentAirtimeStartElapsedMs >= 0) {
                return j + (SystemClock.elapsedRealtime() - this.currentAirtimeStartElapsedMs);
            }
            return j;
        }

        public long getCurrentAirtimeExpandedMs() {
            long j = this.airtimeExpandedMs;
            if (this.currentAirtimeExpandedStartElapsedMs >= 0) {
                return j + (SystemClock.elapsedRealtime() - this.currentAirtimeExpandedStartElapsedMs);
            }
            return j;
        }

        public void onClick() {
            if (this.posttimeToFirstClickMs < 0) {
                this.posttimeToFirstClickMs = SystemClock.elapsedRealtime() - this.posttimeElapsedMs;
            }
        }

        public void onDismiss() {
            if (this.posttimeToDismissMs < 0) {
                this.posttimeToDismissMs = SystemClock.elapsedRealtime() - this.posttimeElapsedMs;
            }
            finish();
        }

        public void onCancel() {
            finish();
        }

        public void onRemoved() {
            finish();
        }

        public void onVisibilityChanged(boolean z) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            boolean z2 = this.isVisible;
            this.isVisible = z;
            if (z) {
                if (this.currentAirtimeStartElapsedMs < 0) {
                    this.airtimeCount++;
                    this.currentAirtimeStartElapsedMs = jElapsedRealtime;
                }
                if (this.posttimeToFirstAirtimeMs < 0) {
                    this.posttimeToFirstAirtimeMs = jElapsedRealtime - this.posttimeElapsedMs;
                }
            } else if (this.currentAirtimeStartElapsedMs >= 0) {
                this.airtimeMs += jElapsedRealtime - this.currentAirtimeStartElapsedMs;
                this.currentAirtimeStartElapsedMs = -1L;
            }
            if (z2 != this.isVisible) {
                updateVisiblyExpandedStats();
            }
        }

        public void onExpansionChanged(boolean z, boolean z2) {
            this.isExpanded = z2;
            if (this.isExpanded && z) {
                this.userExpansionCount++;
            }
            updateVisiblyExpandedStats();
        }

        private void updateVisiblyExpandedStats() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (!this.isExpanded || !this.isVisible) {
                if (this.currentAirtimeExpandedStartElapsedMs >= 0) {
                    this.airtimeExpandedMs += jElapsedRealtime - this.currentAirtimeExpandedStartElapsedMs;
                    this.currentAirtimeExpandedStartElapsedMs = -1L;
                    return;
                }
                return;
            }
            if (this.currentAirtimeExpandedStartElapsedMs < 0) {
                this.currentAirtimeExpandedStartElapsedMs = jElapsedRealtime;
            }
            if (this.posttimeToFirstVisibleExpansionMs < 0) {
                this.posttimeToFirstVisibleExpansionMs = jElapsedRealtime - this.posttimeElapsedMs;
            }
        }

        public void finish() {
            onVisibilityChanged(false);
        }

        public String toString() {
            return "SingleNotificationStats{posttimeElapsedMs=" + this.posttimeElapsedMs + ", posttimeToFirstClickMs=" + this.posttimeToFirstClickMs + ", posttimeToDismissMs=" + this.posttimeToDismissMs + ", airtimeCount=" + this.airtimeCount + ", airtimeMs=" + this.airtimeMs + ", currentAirtimeStartElapsedMs=" + this.currentAirtimeStartElapsedMs + ", airtimeExpandedMs=" + this.airtimeExpandedMs + ", posttimeToFirstVisibleExpansionMs=" + this.posttimeToFirstVisibleExpansionMs + ", currentAirtimeExpandedStartElapsedMs=" + this.currentAirtimeExpandedStartElapsedMs + ", requestedImportance=" + this.requestedImportance + ", naturalImportance=" + this.naturalImportance + ", isNoisy=" + this.isNoisy + '}';
        }

        public void updateFrom(SingleNotificationStats singleNotificationStats) {
            this.posttimeElapsedMs = singleNotificationStats.posttimeElapsedMs;
            this.posttimeToFirstClickMs = singleNotificationStats.posttimeToFirstClickMs;
            this.airtimeCount = singleNotificationStats.airtimeCount;
            this.posttimeToFirstAirtimeMs = singleNotificationStats.posttimeToFirstAirtimeMs;
            this.currentAirtimeStartElapsedMs = singleNotificationStats.currentAirtimeStartElapsedMs;
            this.airtimeMs = singleNotificationStats.airtimeMs;
            this.posttimeToFirstVisibleExpansionMs = singleNotificationStats.posttimeToFirstVisibleExpansionMs;
            this.currentAirtimeExpandedStartElapsedMs = singleNotificationStats.currentAirtimeExpandedStartElapsedMs;
            this.airtimeExpandedMs = singleNotificationStats.airtimeExpandedMs;
            this.userExpansionCount = singleNotificationStats.userExpansionCount;
        }
    }

    public static class Aggregate {
        double avg;
        long numSamples;
        double sum2;
        double var;

        public void addSample(long j) {
            this.numSamples++;
            double d = this.numSamples;
            double d2 = j - this.avg;
            this.avg += (1.0d / d) * d2;
            double d3 = d - 1.0d;
            this.sum2 += (d3 / d) * d2 * d2;
            this.var = this.sum2 / (this.numSamples != 1 ? d3 : 1.0d);
        }

        public String toString() {
            return "Aggregate{numSamples=" + this.numSamples + ", avg=" + this.avg + ", var=" + this.var + '}';
        }
    }

    private static class SQLiteLog {
        private static final String COL_ACTION_COUNT = "action_count";
        private static final String COL_AIRTIME_EXPANDED_MS = "expansion_airtime_ms";
        private static final String COL_AIRTIME_MS = "airtime_ms";
        private static final String COL_CATEGORY = "category";
        private static final String COL_DEFAULTS = "defaults";
        private static final String COL_DEMOTED = "demoted";
        private static final String COL_EVENT_TIME = "event_time_ms";
        private static final String COL_EVENT_TYPE = "event_type";
        private static final String COL_EVENT_USER_ID = "event_user_id";
        private static final String COL_EXPAND_COUNT = "expansion_count";
        private static final String COL_FIRST_EXPANSIONTIME_MS = "first_expansion_time_ms";
        private static final String COL_FLAGS = "flags";
        private static final String COL_IMPORTANCE_FINAL = "importance_final";
        private static final String COL_IMPORTANCE_REQ = "importance_request";
        private static final String COL_KEY = "key";
        private static final String COL_MUTED = "muted";
        private static final String COL_NOISY = "noisy";
        private static final String COL_NOTIFICATION_ID = "nid";
        private static final String COL_PKG = "pkg";
        private static final String COL_POSTTIME_MS = "posttime_ms";
        private static final String COL_TAG = "tag";
        private static final String COL_WHEN_MS = "when_ms";
        private static final long DAY_MS = 86400000;
        private static final String DB_NAME = "notification_log.db";
        private static final int DB_VERSION = 5;
        private static final int EVENT_TYPE_CLICK = 2;
        private static final int EVENT_TYPE_DISMISS = 4;
        private static final int EVENT_TYPE_POST = 1;
        private static final int EVENT_TYPE_REMOVE = 3;
        private static final long HORIZON_MS = 604800000;
        private static final int IDLE_CONNECTION_TIMEOUT_MS = 30000;
        private static final int MSG_CLICK = 2;
        private static final int MSG_DISMISS = 4;
        private static final int MSG_POST = 1;
        private static final int MSG_REMOVE = 3;
        private static final long PRUNE_MIN_DELAY_MS = 21600000;
        private static final long PRUNE_MIN_WRITES = 1024;
        private static final String STATS_QUERY = "SELECT event_user_id, pkg, CAST(((%d - event_time_ms) / 86400000) AS int) AS day, COUNT(*) AS cnt, SUM(muted) as muted, SUM(noisy) as noisy, SUM(demoted) as demoted FROM log WHERE event_type=1 AND event_time_ms > %d  GROUP BY event_user_id, day, pkg";
        private static final String TAB_LOG = "log";
        private static final String TAG = "NotificationSQLiteLog";
        private static long sLastPruneMs;
        private static long sNumWrites;
        private final SQLiteOpenHelper mHelper;
        private final Handler mWriteHandler;

        public SQLiteLog(Context context) {
            HandlerThread handlerThread = new HandlerThread("notification-sqlite-log", 10);
            handlerThread.start();
            this.mWriteHandler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message message) {
                    NotificationRecord notificationRecord = (NotificationRecord) message.obj;
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    switch (message.what) {
                        case 1:
                            SQLiteLog.this.writeEvent(notificationRecord.sbn.getPostTime(), 1, notificationRecord);
                            break;
                        case 2:
                            SQLiteLog.this.writeEvent(jCurrentTimeMillis, 2, notificationRecord);
                            break;
                        case 3:
                            SQLiteLog.this.writeEvent(jCurrentTimeMillis, 3, notificationRecord);
                            break;
                        case 4:
                            SQLiteLog.this.writeEvent(jCurrentTimeMillis, 4, notificationRecord);
                            break;
                        default:
                            Log.wtf(SQLiteLog.TAG, "Unknown message type: " + message.what);
                            break;
                    }
                }
            };
            this.mHelper = new SQLiteOpenHelper(context, DB_NAME, null, 5) {
                @Override
                public void onCreate(SQLiteDatabase sQLiteDatabase) {
                    sQLiteDatabase.execSQL("CREATE TABLE log (_id INTEGER PRIMARY KEY AUTOINCREMENT,event_user_id INT,event_type INT,event_time_ms INT,key TEXT,pkg TEXT,nid INT,tag TEXT,when_ms INT,defaults INT,flags INT,importance_request INT,importance_final INT,noisy INT,muted INT,demoted INT,category TEXT,action_count INT,posttime_ms INT,airtime_ms INT,first_expansion_time_ms INT,expansion_airtime_ms INT,expansion_count INT)");
                }

                @Override
                public void onConfigure(SQLiteDatabase sQLiteDatabase) {
                    setIdleConnectionTimeout(30000L);
                }

                @Override
                public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                    if (i != i2) {
                        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS log");
                        onCreate(sQLiteDatabase);
                    }
                }
            };
        }

        public void logPosted(NotificationRecord notificationRecord) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(1, notificationRecord));
        }

        public void logClicked(NotificationRecord notificationRecord) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(2, notificationRecord));
        }

        public void logRemoved(NotificationRecord notificationRecord) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(3, notificationRecord));
        }

        public void logDismissed(NotificationRecord notificationRecord) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(4, notificationRecord));
        }

        private JSONArray jsonPostFrequencies(NotificationManagerService.DumpFilter dumpFilter) throws JSONException {
            JSONArray jSONArray = new JSONArray();
            Cursor cursorRawQuery = this.mHelper.getReadableDatabase().rawQuery(String.format(STATS_QUERY, Long.valueOf(getMidnightMs()), Long.valueOf(dumpFilter.since)), null);
            try {
                cursorRawQuery.moveToFirst();
                while (!cursorRawQuery.isAfterLast()) {
                    int i = cursorRawQuery.getInt(0);
                    String string = cursorRawQuery.getString(1);
                    if (dumpFilter == null || dumpFilter.matches(string)) {
                        int i2 = cursorRawQuery.getInt(2);
                        int i3 = cursorRawQuery.getInt(3);
                        int i4 = cursorRawQuery.getInt(4);
                        int i5 = cursorRawQuery.getInt(5);
                        int i6 = cursorRawQuery.getInt(6);
                        JSONObject jSONObject = new JSONObject();
                        jSONObject.put("user_id", i);
                        jSONObject.put(Settings.ATTR_PACKAGE, string);
                        jSONObject.put("day", i2);
                        jSONObject.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, i3);
                        jSONObject.put(COL_NOISY, i5);
                        jSONObject.put(COL_MUTED, i4);
                        jSONObject.put(COL_DEMOTED, i6);
                        jSONArray.put(jSONObject);
                    }
                    cursorRawQuery.moveToNext();
                }
                return jSONArray;
            } finally {
                cursorRawQuery.close();
            }
        }

        public void printPostFrequencies(PrintWriter printWriter, String str, NotificationManagerService.DumpFilter dumpFilter) {
            Cursor cursorRawQuery = this.mHelper.getReadableDatabase().rawQuery(String.format(STATS_QUERY, Long.valueOf(getMidnightMs()), Long.valueOf(dumpFilter.since)), null);
            try {
                cursorRawQuery.moveToFirst();
                while (!cursorRawQuery.isAfterLast()) {
                    int i = cursorRawQuery.getInt(0);
                    String string = cursorRawQuery.getString(1);
                    if (dumpFilter == null || dumpFilter.matches(string)) {
                        printWriter.println(str + "post_frequency{user_id=" + i + ",pkg=" + string + ",day=" + cursorRawQuery.getInt(2) + ",count=" + cursorRawQuery.getInt(3) + ",muted=" + cursorRawQuery.getInt(4) + SliceClientPermissions.SliceAuthority.DELIMITER + cursorRawQuery.getInt(5) + ",demoted=" + cursorRawQuery.getInt(6) + "}");
                    }
                    cursorRawQuery.moveToNext();
                }
            } finally {
                cursorRawQuery.close();
            }
        }

        private long getMidnightMs() {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.set(gregorianCalendar.get(1), gregorianCalendar.get(2), gregorianCalendar.get(5), 23, 59, 59);
            return gregorianCalendar.getTimeInMillis();
        }

        private void writeEvent(long j, int i, NotificationRecord notificationRecord) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_EVENT_USER_ID, Integer.valueOf(notificationRecord.sbn.getUser().getIdentifier()));
            contentValues.put(COL_EVENT_TIME, Long.valueOf(j));
            contentValues.put(COL_EVENT_TYPE, Integer.valueOf(i));
            putNotificationIdentifiers(notificationRecord, contentValues);
            if (i == 1) {
                putNotificationDetails(notificationRecord, contentValues);
            } else {
                putPosttimeVisibility(notificationRecord, contentValues);
            }
            SQLiteDatabase writableDatabase = this.mHelper.getWritableDatabase();
            if (writableDatabase.insert(TAB_LOG, null, contentValues) < 0) {
                Log.wtf(TAG, "Error while trying to insert values: " + contentValues);
            }
            sNumWrites++;
            pruneIfNecessary(writableDatabase);
        }

        private void pruneIfNecessary(SQLiteDatabase sQLiteDatabase) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (sNumWrites > PRUNE_MIN_WRITES || jCurrentTimeMillis - sLastPruneMs > PRUNE_MIN_DELAY_MS) {
                sNumWrites = 0L;
                sLastPruneMs = jCurrentTimeMillis;
                Log.d(TAG, "Pruned event entries: " + sQLiteDatabase.delete(TAB_LOG, "event_time_ms < ?", new String[]{String.valueOf(jCurrentTimeMillis - 604800000)}));
            }
        }

        private static void putNotificationIdentifiers(NotificationRecord notificationRecord, ContentValues contentValues) {
            contentValues.put(COL_KEY, notificationRecord.sbn.getKey());
            contentValues.put(COL_PKG, notificationRecord.sbn.getPackageName());
        }

        private static void putNotificationDetails(NotificationRecord notificationRecord, ContentValues contentValues) {
            contentValues.put(COL_NOTIFICATION_ID, Integer.valueOf(notificationRecord.sbn.getId()));
            if (notificationRecord.sbn.getTag() != null) {
                contentValues.put(COL_TAG, notificationRecord.sbn.getTag());
            }
            contentValues.put(COL_WHEN_MS, Long.valueOf(notificationRecord.sbn.getPostTime()));
            contentValues.put(COL_FLAGS, Integer.valueOf(notificationRecord.getNotification().flags));
            int i = notificationRecord.stats.requestedImportance;
            int importance = notificationRecord.getImportance();
            boolean z = notificationRecord.stats.isNoisy;
            contentValues.put(COL_IMPORTANCE_REQ, Integer.valueOf(i));
            contentValues.put(COL_IMPORTANCE_FINAL, Integer.valueOf(importance));
            contentValues.put(COL_DEMOTED, Integer.valueOf(importance < i ? 1 : 0));
            contentValues.put(COL_NOISY, Boolean.valueOf(z));
            if (z && importance < 4) {
                contentValues.put(COL_MUTED, (Integer) 1);
            } else {
                contentValues.put(COL_MUTED, (Integer) 0);
            }
            if (notificationRecord.getNotification().category != null) {
                contentValues.put(COL_CATEGORY, notificationRecord.getNotification().category);
            }
            contentValues.put(COL_ACTION_COUNT, Integer.valueOf(notificationRecord.getNotification().actions != null ? notificationRecord.getNotification().actions.length : 0));
        }

        private static void putPosttimeVisibility(NotificationRecord notificationRecord, ContentValues contentValues) {
            contentValues.put(COL_POSTTIME_MS, Long.valueOf(notificationRecord.stats.getCurrentPosttimeMs()));
            contentValues.put(COL_AIRTIME_MS, Long.valueOf(notificationRecord.stats.getCurrentAirtimeMs()));
            contentValues.put(COL_EXPAND_COUNT, Long.valueOf(notificationRecord.stats.userExpansionCount));
            contentValues.put(COL_AIRTIME_EXPANDED_MS, Long.valueOf(notificationRecord.stats.getCurrentAirtimeExpandedMs()));
            contentValues.put(COL_FIRST_EXPANSIONTIME_MS, Long.valueOf(notificationRecord.stats.posttimeToFirstVisibleExpansionMs));
        }

        public void dump(PrintWriter printWriter, String str, NotificationManagerService.DumpFilter dumpFilter) {
            printPostFrequencies(printWriter, str, dumpFilter);
        }

        public JSONObject dumpJson(NotificationManagerService.DumpFilter dumpFilter) {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("post_frequency", jsonPostFrequencies(dumpFilter));
                jSONObject.put("since", dumpFilter.since);
                jSONObject.put("now", System.currentTimeMillis());
            } catch (JSONException e) {
            }
            return jSONObject;
        }
    }
}
