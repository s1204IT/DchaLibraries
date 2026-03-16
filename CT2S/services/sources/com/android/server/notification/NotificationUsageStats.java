package com.android.server.notification;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class NotificationUsageStats {
    private static final AggregatedStats[] EMPTY_AGGREGATED_STATS = new AggregatedStats[0];
    private static final boolean ENABLE_AGGREGATED_IN_MEMORY_STATS = false;
    private static final boolean ENABLE_SQLITE_LOG = false;
    private final Map<String, AggregatedStats> mStats = new HashMap();
    private final SQLiteLog mSQLiteLog = null;

    public NotificationUsageStats(Context context) {
    }

    public synchronized void registerPostedByApp(NotificationRecord notification) {
        notification.stats = new SingleNotificationStats();
        notification.stats.posttimeElapsedMs = SystemClock.elapsedRealtime();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.numPostedByApp++;
        }
    }

    public void registerUpdatedByApp(NotificationRecord notification, NotificationRecord old) {
        notification.stats = old.stats;
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.numUpdatedByApp++;
        }
    }

    public synchronized void registerRemovedByApp(NotificationRecord notification) {
        notification.stats.onRemoved();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.numRemovedByApp++;
            stats.collect(notification.stats);
        }
    }

    public synchronized void registerDismissedByUser(NotificationRecord notification) {
        notification.stats.onDismiss();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.numDismissedByUser++;
            stats.collect(notification.stats);
        }
    }

    public synchronized void registerClickedByUser(NotificationRecord notification) {
        notification.stats.onClick();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.numClickedByUser++;
        }
    }

    public synchronized void registerCancelDueToClick(NotificationRecord notification) {
        notification.stats.onCancel();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.collect(notification.stats);
        }
    }

    public synchronized void registerCancelUnknown(NotificationRecord notification) {
        notification.stats.onCancel();
        AggregatedStats[] arr$ = getAggregatedStatsLocked(notification);
        for (AggregatedStats stats : arr$) {
            stats.collect(notification.stats);
        }
    }

    private AggregatedStats[] getAggregatedStatsLocked(NotificationRecord record) {
        return EMPTY_AGGREGATED_STATS;
    }

    private AggregatedStats getOrCreateAggregatedStatsLocked(String key) {
        AggregatedStats result = this.mStats.get(key);
        if (result == null) {
            AggregatedStats result2 = new AggregatedStats(key);
            this.mStats.put(key, result2);
            return result2;
        }
        return result;
    }

    public synchronized void dump(PrintWriter pw, String indent, NotificationManagerService.DumpFilter filter) {
    }

    private static class AggregatedStats {
        public final String key;
        public int numClickedByUser;
        public int numDismissedByUser;
        public int numPostedByApp;
        public int numRemovedByApp;
        public int numUpdatedByApp;
        public final Aggregate posttimeMs = new Aggregate();
        public final Aggregate posttimeToDismissMs = new Aggregate();
        public final Aggregate posttimeToFirstClickMs = new Aggregate();
        public final Aggregate airtimeCount = new Aggregate();
        public final Aggregate airtimeMs = new Aggregate();
        public final Aggregate posttimeToFirstAirtimeMs = new Aggregate();
        public final Aggregate userExpansionCount = new Aggregate();
        public final Aggregate airtimeExpandedMs = new Aggregate();
        public final Aggregate posttimeToFirstVisibleExpansionMs = new Aggregate();

        public AggregatedStats(String key) {
            this.key = key;
        }

        public void collect(SingleNotificationStats singleNotificationStats) {
            this.posttimeMs.addSample(SystemClock.elapsedRealtime() - singleNotificationStats.posttimeElapsedMs);
            if (singleNotificationStats.posttimeToDismissMs >= 0) {
                this.posttimeToDismissMs.addSample(singleNotificationStats.posttimeToDismissMs);
            }
            if (singleNotificationStats.posttimeToFirstClickMs >= 0) {
                this.posttimeToFirstClickMs.addSample(singleNotificationStats.posttimeToFirstClickMs);
            }
            this.airtimeCount.addSample(singleNotificationStats.airtimeCount);
            if (singleNotificationStats.airtimeMs >= 0) {
                this.airtimeMs.addSample(singleNotificationStats.airtimeMs);
            }
            if (singleNotificationStats.posttimeToFirstAirtimeMs >= 0) {
                this.posttimeToFirstAirtimeMs.addSample(singleNotificationStats.posttimeToFirstAirtimeMs);
            }
            if (singleNotificationStats.posttimeToFirstVisibleExpansionMs >= 0) {
                this.posttimeToFirstVisibleExpansionMs.addSample(singleNotificationStats.posttimeToFirstVisibleExpansionMs);
            }
            this.userExpansionCount.addSample(singleNotificationStats.userExpansionCount);
            if (singleNotificationStats.airtimeExpandedMs >= 0) {
                this.airtimeExpandedMs.addSample(singleNotificationStats.airtimeExpandedMs);
            }
        }

        public void dump(PrintWriter pw, String indent) {
            pw.println(toStringWithIndent(indent));
        }

        public String toString() {
            return toStringWithIndent("");
        }

        private String toStringWithIndent(String indent) {
            return indent + "AggregatedStats{\n" + indent + "  key='" + this.key + "',\n" + indent + "  numPostedByApp=" + this.numPostedByApp + ",\n" + indent + "  numUpdatedByApp=" + this.numUpdatedByApp + ",\n" + indent + "  numRemovedByApp=" + this.numRemovedByApp + ",\n" + indent + "  numClickedByUser=" + this.numClickedByUser + ",\n" + indent + "  numDismissedByUser=" + this.numDismissedByUser + ",\n" + indent + "  posttimeMs=" + this.posttimeMs + ",\n" + indent + "  posttimeToDismissMs=" + this.posttimeToDismissMs + ",\n" + indent + "  posttimeToFirstClickMs=" + this.posttimeToFirstClickMs + ",\n" + indent + "  airtimeCount=" + this.airtimeCount + ",\n" + indent + "  airtimeMs=" + this.airtimeMs + ",\n" + indent + "  posttimeToFirstAirtimeMs=" + this.posttimeToFirstAirtimeMs + ",\n" + indent + "  userExpansionCount=" + this.userExpansionCount + ",\n" + indent + "  airtimeExpandedMs=" + this.airtimeExpandedMs + ",\n" + indent + "  posttimeToFVEMs=" + this.posttimeToFirstVisibleExpansionMs + ",\n" + indent + "}";
        }
    }

    public static class SingleNotificationStats {
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
            long result = this.airtimeMs;
            if (this.currentAirtimeStartElapsedMs >= 0) {
                return result + (SystemClock.elapsedRealtime() - this.currentAirtimeStartElapsedMs);
            }
            return result;
        }

        public long getCurrentAirtimeExpandedMs() {
            long result = this.airtimeExpandedMs;
            if (this.currentAirtimeExpandedStartElapsedMs >= 0) {
                return result + (SystemClock.elapsedRealtime() - this.currentAirtimeExpandedStartElapsedMs);
            }
            return result;
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

        public void onVisibilityChanged(boolean visible) {
            long elapsedNowMs = SystemClock.elapsedRealtime();
            boolean wasVisible = this.isVisible;
            this.isVisible = visible;
            if (visible) {
                if (this.currentAirtimeStartElapsedMs < 0) {
                    this.airtimeCount++;
                    this.currentAirtimeStartElapsedMs = elapsedNowMs;
                }
                if (this.posttimeToFirstAirtimeMs < 0) {
                    this.posttimeToFirstAirtimeMs = elapsedNowMs - this.posttimeElapsedMs;
                }
            } else if (this.currentAirtimeStartElapsedMs >= 0) {
                this.airtimeMs += elapsedNowMs - this.currentAirtimeStartElapsedMs;
                this.currentAirtimeStartElapsedMs = -1L;
            }
            if (wasVisible != this.isVisible) {
                updateVisiblyExpandedStats();
            }
        }

        public void onExpansionChanged(boolean userAction, boolean expanded) {
            this.isExpanded = expanded;
            if (this.isExpanded && userAction) {
                this.userExpansionCount++;
            }
            updateVisiblyExpandedStats();
        }

        private void updateVisiblyExpandedStats() {
            long elapsedNowMs = SystemClock.elapsedRealtime();
            if (!this.isExpanded || !this.isVisible) {
                if (this.currentAirtimeExpandedStartElapsedMs >= 0) {
                    this.airtimeExpandedMs += elapsedNowMs - this.currentAirtimeExpandedStartElapsedMs;
                    this.currentAirtimeExpandedStartElapsedMs = -1L;
                    return;
                }
                return;
            }
            if (this.currentAirtimeExpandedStartElapsedMs < 0) {
                this.currentAirtimeExpandedStartElapsedMs = elapsedNowMs;
            }
            if (this.posttimeToFirstVisibleExpansionMs < 0) {
                this.posttimeToFirstVisibleExpansionMs = elapsedNowMs - this.posttimeElapsedMs;
            }
        }

        public void finish() {
            onVisibilityChanged(false);
        }

        public String toString() {
            return "SingleNotificationStats{posttimeElapsedMs=" + this.posttimeElapsedMs + ", posttimeToFirstClickMs=" + this.posttimeToFirstClickMs + ", posttimeToDismissMs=" + this.posttimeToDismissMs + ", airtimeCount=" + this.airtimeCount + ", airtimeMs=" + this.airtimeMs + ", currentAirtimeStartElapsedMs=" + this.currentAirtimeStartElapsedMs + ", airtimeExpandedMs=" + this.airtimeExpandedMs + ", posttimeToFirstVisibleExpansionMs=" + this.posttimeToFirstVisibleExpansionMs + ", currentAirtimeExpandedSEMs=" + this.currentAirtimeExpandedStartElapsedMs + '}';
        }
    }

    public static class Aggregate {
        double avg;
        long numSamples;
        double sum2;
        double var;

        public void addSample(long sample) {
            this.numSamples++;
            double n = this.numSamples;
            double delta = sample - this.avg;
            this.avg += (1.0d / n) * delta;
            this.sum2 += ((n - 1.0d) / n) * delta * delta;
            double divisor = this.numSamples != 1 ? n - 1.0d : 1.0d;
            this.var = this.sum2 / divisor;
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
        private static final String COL_EVENT_TIME = "event_time_ms";
        private static final String COL_EVENT_TYPE = "event_type";
        private static final String COL_EVENT_USER_ID = "event_user_id";
        private static final String COL_EXPAND_COUNT = "expansion_count";
        private static final String COL_FIRST_EXPANSIONTIME_MS = "first_expansion_time_ms";
        private static final String COL_FLAGS = "flags";
        private static final String COL_KEY = "key";
        private static final String COL_NOTIFICATION_ID = "nid";
        private static final String COL_PKG = "pkg";
        private static final String COL_POSTTIME_MS = "posttime_ms";
        private static final String COL_PRIORITY = "priority";
        private static final String COL_TAG = "tag";
        private static final String COL_WHEN_MS = "when_ms";
        private static final long DAY_MS = 86400000;
        private static final String DB_NAME = "notification_log.db";
        private static final int DB_VERSION = 4;
        private static final int EVENT_TYPE_CLICK = 2;
        private static final int EVENT_TYPE_DISMISS = 4;
        private static final int EVENT_TYPE_POST = 1;
        private static final int EVENT_TYPE_REMOVE = 3;
        private static final long HORIZON_MS = 604800000;
        private static final int MSG_CLICK = 2;
        private static final int MSG_DISMISS = 4;
        private static final int MSG_POST = 1;
        private static final int MSG_REMOVE = 3;
        private static final long PRUNE_MIN_DELAY_MS = 21600000;
        private static final long PRUNE_MIN_WRITES = 1024;
        private static final String TAB_LOG = "log";
        private static final String TAG = "NotificationSQLiteLog";
        private static long sLastPruneMs;
        private static long sNumWrites;
        private final SQLiteOpenHelper mHelper;
        private final Handler mWriteHandler;

        public SQLiteLog(Context context) {
            HandlerThread backgroundThread = new HandlerThread("notification-sqlite-log", 10);
            backgroundThread.start();
            this.mWriteHandler = new Handler(backgroundThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    NotificationRecord r = (NotificationRecord) msg.obj;
                    long nowMs = System.currentTimeMillis();
                    switch (msg.what) {
                        case 1:
                            SQLiteLog.this.writeEvent(r.sbn.getPostTime(), 1, r);
                            break;
                        case 2:
                            SQLiteLog.this.writeEvent(nowMs, 2, r);
                            break;
                        case 3:
                            SQLiteLog.this.writeEvent(nowMs, 3, r);
                            break;
                        case 4:
                            SQLiteLog.this.writeEvent(nowMs, 4, r);
                            break;
                        default:
                            Log.wtf(SQLiteLog.TAG, "Unknown message type: " + msg.what);
                            break;
                    }
                }
            };
            this.mHelper = new SQLiteOpenHelper(context, DB_NAME, null, 4) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL("CREATE TABLE log (_id INTEGER PRIMARY KEY AUTOINCREMENT,event_user_id INT,event_type INT,event_time_ms INT,key TEXT,pkg TEXT,nid INT,tag TEXT,when_ms INT,defaults INT,flags INT,priority INT,category TEXT,action_count INT,posttime_ms INT,airtime_ms INT,first_expansion_time_ms INT,expansion_airtime_ms INT,expansion_count INT)");
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    if (oldVersion <= 3) {
                        db.execSQL("DROP TABLE IF EXISTS log");
                        onCreate(db);
                    }
                }
            };
        }

        public void logPosted(NotificationRecord notification) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(1, notification));
        }

        public void logClicked(NotificationRecord notification) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(2, notification));
        }

        public void logRemoved(NotificationRecord notification) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(3, notification));
        }

        public void logDismissed(NotificationRecord notification) {
            this.mWriteHandler.sendMessage(this.mWriteHandler.obtainMessage(4, notification));
        }

        public void printPostFrequencies(PrintWriter pw, String indent, NotificationManagerService.DumpFilter filter) {
            SQLiteDatabase db = this.mHelper.getReadableDatabase();
            long nowMs = System.currentTimeMillis();
            String q = "SELECT event_user_id, pkg, CAST(((" + nowMs + " - " + COL_EVENT_TIME + ") / " + DAY_MS + ") AS int) AS day, COUNT(*) AS cnt FROM " + TAB_LOG + " WHERE " + COL_EVENT_TYPE + "=1 GROUP BY " + COL_EVENT_USER_ID + ", day, " + COL_PKG;
            Cursor cursor = db.rawQuery(q, null);
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int userId = cursor.getInt(0);
                    String pkg = cursor.getString(1);
                    if (filter == null || filter.matches(pkg)) {
                        int day = cursor.getInt(2);
                        int count = cursor.getInt(3);
                        pw.println(indent + "post_frequency{user_id=" + userId + ",pkg=" + pkg + ",day=" + day + ",count=" + count + "}");
                    }
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        }

        private void writeEvent(long eventTimeMs, int eventType, NotificationRecord r) {
            ContentValues cv = new ContentValues();
            cv.put(COL_EVENT_USER_ID, Integer.valueOf(r.sbn.getUser().getIdentifier()));
            cv.put(COL_EVENT_TIME, Long.valueOf(eventTimeMs));
            cv.put(COL_EVENT_TYPE, Integer.valueOf(eventType));
            putNotificationIdentifiers(r, cv);
            if (eventType == 1) {
                putNotificationDetails(r, cv);
            } else {
                putPosttimeVisibility(r, cv);
            }
            SQLiteDatabase db = this.mHelper.getWritableDatabase();
            if (db.insert(TAB_LOG, null, cv) < 0) {
                Log.wtf(TAG, "Error while trying to insert values: " + cv);
            }
            sNumWrites++;
            pruneIfNecessary(db);
        }

        private void pruneIfNecessary(SQLiteDatabase db) {
            long nowMs = System.currentTimeMillis();
            if (sNumWrites > PRUNE_MIN_WRITES || nowMs - sLastPruneMs > PRUNE_MIN_DELAY_MS) {
                sNumWrites = 0L;
                sLastPruneMs = nowMs;
                long horizonStartMs = nowMs - HORIZON_MS;
                int deletedRows = db.delete(TAB_LOG, "event_time_ms < ?", new String[]{String.valueOf(horizonStartMs)});
                Log.d(TAG, "Pruned event entries: " + deletedRows);
            }
        }

        private static void putNotificationIdentifiers(NotificationRecord r, ContentValues outCv) {
            outCv.put(COL_KEY, r.sbn.getKey());
            outCv.put(COL_PKG, r.sbn.getPackageName());
        }

        private static void putNotificationDetails(NotificationRecord r, ContentValues outCv) {
            outCv.put(COL_NOTIFICATION_ID, Integer.valueOf(r.sbn.getId()));
            if (r.sbn.getTag() != null) {
                outCv.put(COL_TAG, r.sbn.getTag());
            }
            outCv.put(COL_WHEN_MS, Long.valueOf(r.sbn.getPostTime()));
            outCv.put(COL_FLAGS, Integer.valueOf(r.getNotification().flags));
            outCv.put(COL_PRIORITY, Integer.valueOf(r.getNotification().priority));
            if (r.getNotification().category != null) {
                outCv.put(COL_CATEGORY, r.getNotification().category);
            }
            outCv.put(COL_ACTION_COUNT, Integer.valueOf(r.getNotification().actions != null ? r.getNotification().actions.length : 0));
        }

        private static void putPosttimeVisibility(NotificationRecord r, ContentValues outCv) {
            outCv.put(COL_POSTTIME_MS, Long.valueOf(r.stats.getCurrentPosttimeMs()));
            outCv.put(COL_AIRTIME_MS, Long.valueOf(r.stats.getCurrentAirtimeMs()));
            outCv.put(COL_EXPAND_COUNT, Long.valueOf(r.stats.userExpansionCount));
            outCv.put(COL_AIRTIME_EXPANDED_MS, Long.valueOf(r.stats.getCurrentAirtimeExpandedMs()));
            outCv.put(COL_FIRST_EXPANSIONTIME_MS, Long.valueOf(r.stats.posttimeToFirstVisibleExpansionMs));
        }

        public void dump(PrintWriter pw, String indent, NotificationManagerService.DumpFilter filter) {
            printPostFrequencies(pw, indent, filter);
        }
    }
}
