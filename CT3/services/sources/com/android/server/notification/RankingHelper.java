package com.android.server.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RankingHelper implements RankingConfig {
    private static final String ATT_IMPORTANCE = "importance";
    private static final String ATT_NAME = "name";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_TOPIC_ID = "id";
    private static final String ATT_TOPIC_LABEL = "label";
    private static final String ATT_UID = "uid";
    private static final String ATT_VERSION = "version";
    private static final String ATT_VISIBILITY = "visibility";
    private static final int DEFAULT_IMPORTANCE = -1000;
    private static final int DEFAULT_PRIORITY = 0;
    private static final int DEFAULT_VISIBILITY = -1000;
    private static final String TAG = "RankingHelper";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_RANKING = "ranking";
    private static final int XML_VERSION = 1;
    private final Context mContext;
    private final RankingHandler mRankingHandler;
    private final NotificationSignalExtractor[] mSignalExtractors;
    private final NotificationComparator mPreliminaryComparator = new NotificationComparator();
    private final GlobalSortKeyComparator mFinalComparator = new GlobalSortKeyComparator();
    private final ArrayMap<String, Record> mRecords = new ArrayMap<>();
    private final ArrayMap<String, NotificationRecord> mProxyByGroupTmp = new ArrayMap<>();
    private final ArrayMap<String, Record> mRestoredWithoutUids = new ArrayMap<>();

    public RankingHelper(Context context, RankingHandler rankingHandler, NotificationUsageStats usageStats, String[] extractorNames) {
        this.mContext = context;
        this.mRankingHandler = rankingHandler;
        int N = extractorNames.length;
        this.mSignalExtractors = new NotificationSignalExtractor[N];
        for (int i = 0; i < N; i++) {
            try {
                Class<?> extractorClass = this.mContext.getClassLoader().loadClass(extractorNames[i]);
                NotificationSignalExtractor extractor = (NotificationSignalExtractor) extractorClass.newInstance();
                extractor.initialize(this.mContext, usageStats);
                extractor.setConfig(this);
                this.mSignalExtractors[i] = extractor;
            } catch (ClassNotFoundException e) {
                Slog.w(TAG, "Couldn't find extractor " + extractorNames[i] + ".", e);
            } catch (IllegalAccessException e2) {
                Slog.w(TAG, "Problem accessing extractor " + extractorNames[i] + ".", e2);
            } catch (InstantiationException e3) {
                Slog.w(TAG, "Couldn't instantiate extractor " + extractorNames[i] + ".", e3);
            }
        }
    }

    public <T extends NotificationSignalExtractor> T findExtractor(Class<T> cls) {
        int length = this.mSignalExtractors.length;
        for (int i = 0; i < length; i++) {
            T t = (T) this.mSignalExtractors[i];
            if (cls.equals(t.getClass())) {
                return t;
            }
        }
        return null;
    }

    public void extractSignals(NotificationRecord r) {
        int N = this.mSignalExtractors.length;
        for (int i = 0; i < N; i++) {
            NotificationSignalExtractor extractor = this.mSignalExtractors[i];
            try {
                RankingReconsideration recon = extractor.process(r);
                if (recon != null) {
                    this.mRankingHandler.requestReconsideration(recon);
                }
            } catch (Throwable t) {
                Slog.w(TAG, "NotificationSignalExtractor failed.", t);
            }
        }
    }

    public void readXml(XmlPullParser parser, boolean forRestore) throws XmlPullParserException, IOException {
        Record r;
        Record record = null;
        PackageManager pm = this.mContext.getPackageManager();
        if (parser.getEventType() != 2 || !TAG_RANKING.equals(parser.getName())) {
            return;
        }
        this.mRecords.clear();
        this.mRestoredWithoutUids.clear();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                String tag = parser.getName();
                if (type == 3 && TAG_RANKING.equals(tag)) {
                    return;
                }
                if (type == 2 && TAG_PACKAGE.equals(tag)) {
                    int uid = safeInt(parser, "uid", Record.UNKNOWN_UID);
                    String name = parser.getAttributeValue(null, ATT_NAME);
                    if (!TextUtils.isEmpty(name)) {
                        if (forRestore) {
                            try {
                                uid = pm.getPackageUidAsUser(name, 0);
                            } catch (PackageManager.NameNotFoundException e) {
                            }
                        }
                        if (uid == Record.UNKNOWN_UID) {
                            Record r2 = this.mRestoredWithoutUids.get(name);
                            r = r2;
                            if (r == null) {
                                r = new Record(record);
                                this.mRestoredWithoutUids.put(name, r);
                            }
                        } else {
                            r = getOrCreateRecord(name, uid);
                        }
                        r.importance = safeInt(parser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        r.priority = safeInt(parser, "priority", 0);
                        r.visibility = safeInt(parser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    }
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    private static String recordKey(String pkg, int uid) {
        return pkg + "|" + uid;
    }

    private Record getOrCreateRecord(String pkg, int uid) {
        Record record = null;
        String key = recordKey(pkg, uid);
        Record r = this.mRecords.get(key);
        if (r == null) {
            Record r2 = new Record(record);
            r2.pkg = pkg;
            r2.uid = uid;
            this.mRecords.put(key, r2);
            return r2;
        }
        return r;
    }

    public void writeXml(XmlSerializer out, boolean forBackup) throws IOException {
        out.startTag(null, TAG_RANKING);
        out.attribute(null, ATT_VERSION, Integer.toString(1));
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            Record r = this.mRecords.valueAt(i);
            if (r == null) {
                Slog.d(TAG, "Catch it, this r is null, size: " + N + ", i: " + i);
                int currentSize = this.mRecords.size();
                Slog.d(TAG, "current real size: " + currentSize);
                for (int j = 0; j < currentSize; j++) {
                    Slog.d(TAG, "j: " + j + ",record: " + this.mRecords.keyAt(j));
                    Slog.d(TAG, "record: " + this.mRecords.valueAt(j));
                }
            }
            if (r != null && (!forBackup || UserHandle.getUserId(r.uid) == 0)) {
                boolean hasNonDefaultSettings = (r.importance == -1000 && r.priority == 0 && r.visibility == -1000) ? false : true;
                if (hasNonDefaultSettings) {
                    out.startTag(null, TAG_PACKAGE);
                    out.attribute(null, ATT_NAME, r.pkg);
                    if (r.importance != -1000) {
                        out.attribute(null, ATT_IMPORTANCE, Integer.toString(r.importance));
                    }
                    if (r.priority != 0) {
                        out.attribute(null, "priority", Integer.toString(r.priority));
                    }
                    if (r.visibility != -1000) {
                        out.attribute(null, ATT_VISIBILITY, Integer.toString(r.visibility));
                    }
                    if (!forBackup) {
                        out.attribute(null, "uid", Integer.toString(r.uid));
                    }
                    out.endTag(null, TAG_PACKAGE);
                }
            }
        }
        out.endTag(null, TAG_RANKING);
    }

    private void updateConfig() {
        int N = this.mSignalExtractors.length;
        for (int i = 0; i < N; i++) {
            this.mSignalExtractors[i].setConfig(this);
        }
        this.mRankingHandler.requestSort();
    }

    public void sort(ArrayList<NotificationRecord> notificationList) {
        String groupSortKeyPortion;
        int N = notificationList.size();
        for (int i = N - 1; i >= 0; i--) {
            notificationList.get(i).setGlobalSortKey(null);
        }
        Collections.sort(notificationList, this.mPreliminaryComparator);
        synchronized (this.mProxyByGroupTmp) {
            for (int i2 = N - 1; i2 >= 0; i2--) {
                NotificationRecord record = notificationList.get(i2);
                record.setAuthoritativeRank(i2);
                String groupKey = record.getGroupKey();
                boolean isGroupSummary = record.getNotification().isGroupSummary();
                if (isGroupSummary || !this.mProxyByGroupTmp.containsKey(groupKey)) {
                    this.mProxyByGroupTmp.put(groupKey, record);
                }
            }
            for (int i3 = 0; i3 < N; i3++) {
                NotificationRecord record2 = notificationList.get(i3);
                NotificationRecord groupProxy = this.mProxyByGroupTmp.get(record2.getGroupKey());
                String groupSortKey = record2.getNotification().getSortKey();
                if (groupSortKey == null) {
                    groupSortKeyPortion = "nsk";
                } else if (groupSortKey.equals("")) {
                    groupSortKeyPortion = "esk";
                } else {
                    groupSortKeyPortion = "gsk=" + groupSortKey;
                }
                boolean isGroupSummary2 = record2.getNotification().isGroupSummary();
                Object[] objArr = new Object[5];
                objArr[0] = Character.valueOf(record2.isRecentlyIntrusive() ? '0' : '1');
                objArr[1] = Integer.valueOf(groupProxy.getAuthoritativeRank());
                objArr[2] = Character.valueOf(isGroupSummary2 ? '0' : '1');
                objArr[3] = groupSortKeyPortion;
                objArr[4] = Integer.valueOf(record2.getAuthoritativeRank());
                record2.setGlobalSortKey(String.format("intrsv=%c:grnk=0x%04x:gsmry=%c:%s:rnk=0x%04x", objArr));
            }
            this.mProxyByGroupTmp.clear();
        }
        Collections.sort(notificationList, this.mFinalComparator);
    }

    public int indexOf(ArrayList<NotificationRecord> notificationList, NotificationRecord target) {
        return Collections.binarySearch(notificationList, target, this.mFinalComparator);
    }

    private static int safeInt(XmlPullParser parser, String att, int defValue) {
        String val = parser.getAttributeValue(null, att);
        return tryParseInt(val, defValue);
    }

    private static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static boolean tryParseBool(String value, boolean defValue) {
        return TextUtils.isEmpty(value) ? defValue : Boolean.valueOf(value).booleanValue();
    }

    @Override
    public int getPriority(String packageName, int uid) {
        return getOrCreateRecord(packageName, uid).priority;
    }

    @Override
    public void setPriority(String packageName, int uid, int priority) {
        getOrCreateRecord(packageName, uid).priority = priority;
        updateConfig();
    }

    @Override
    public int getVisibilityOverride(String packageName, int uid) {
        return getOrCreateRecord(packageName, uid).visibility;
    }

    @Override
    public void setVisibilityOverride(String pkgName, int uid, int visibility) {
        getOrCreateRecord(pkgName, uid).visibility = visibility;
        updateConfig();
    }

    @Override
    public int getImportance(String packageName, int uid) {
        return getOrCreateRecord(packageName, uid).importance;
    }

    @Override
    public void setImportance(String pkgName, int uid, int importance) {
        getOrCreateRecord(pkgName, uid).importance = importance;
        updateConfig();
    }

    public void setEnabled(String packageName, int uid, boolean enabled) {
        boolean wasEnabled = getImportance(packageName, uid) != 0;
        if (wasEnabled == enabled) {
            return;
        }
        setImportance(packageName, uid, enabled ? JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE : 0);
    }

    public void dump(PrintWriter pw, String prefix, NotificationManagerService.DumpFilter filter) {
        if (filter == null) {
            int N = this.mSignalExtractors.length;
            pw.print(prefix);
            pw.print("mSignalExtractors.length = ");
            pw.println(N);
            for (int i = 0; i < N; i++) {
                pw.print(prefix);
                pw.print("  ");
                pw.println(this.mSignalExtractors[i]);
            }
        }
        if (filter == null) {
            pw.print(prefix);
            pw.println("per-package config:");
        }
        pw.println("Records:");
        dumpRecords(pw, prefix, filter, this.mRecords);
        pw.println("Restored without uid:");
        dumpRecords(pw, prefix, filter, this.mRestoredWithoutUids);
    }

    private static void dumpRecords(PrintWriter pw, String prefix, NotificationManagerService.DumpFilter filter, ArrayMap<String, Record> records) {
        int N = records.size();
        for (int i = 0; i < N; i++) {
            Record r = records.valueAt(i);
            if (filter == null || filter.matches(r.pkg)) {
                pw.print(prefix);
                pw.print("  ");
                pw.print(r.pkg);
                pw.print(" (");
                pw.print(r.uid == Record.UNKNOWN_UID ? "UNKNOWN_UID" : Integer.toString(r.uid));
                pw.print(')');
                if (r.importance != -1000) {
                    pw.print(" importance=");
                    pw.print(NotificationListenerService.Ranking.importanceToString(r.importance));
                }
                if (r.priority != 0) {
                    pw.print(" priority=");
                    pw.print(Notification.priorityToString(r.priority));
                }
                if (r.visibility != -1000) {
                    pw.print(" visibility=");
                    pw.print(Notification.visibilityToString(r.visibility));
                }
                pw.println();
            }
        }
    }

    public JSONObject dumpJson(NotificationManagerService.DumpFilter filter) {
        JSONObject jSONObject = new JSONObject();
        JSONArray records = new JSONArray();
        try {
            jSONObject.put("noUid", this.mRestoredWithoutUids.size());
        } catch (JSONException e) {
        }
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            Record r = this.mRecords.valueAt(i);
            if (filter == null || filter.matches(r.pkg)) {
                JSONObject record = new JSONObject();
                try {
                    record.put("userId", UserHandle.getUserId(r.uid));
                    record.put("packageName", r.pkg);
                    if (r.importance != -1000) {
                        record.put(ATT_IMPORTANCE, NotificationListenerService.Ranking.importanceToString(r.importance));
                    }
                    if (r.priority != 0) {
                        record.put("priority", Notification.priorityToString(r.priority));
                    }
                    if (r.visibility != -1000) {
                        record.put(ATT_VISIBILITY, Notification.visibilityToString(r.visibility));
                    }
                } catch (JSONException e2) {
                }
                records.put(record);
            }
        }
        try {
            jSONObject.put("records", records);
        } catch (JSONException e3) {
        }
        return jSONObject;
    }

    public JSONArray dumpBansJson(NotificationManagerService.DumpFilter filter) {
        JSONArray bans = new JSONArray();
        Map<Integer, String> packageBans = getPackageBans();
        for (Map.Entry<Integer, String> ban : packageBans.entrySet()) {
            int userId = UserHandle.getUserId(ban.getKey().intValue());
            String packageName = ban.getValue();
            if (filter == null || filter.matches(packageName)) {
                JSONObject banJson = new JSONObject();
                try {
                    banJson.put("userId", userId);
                    banJson.put("packageName", packageName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bans.put(banJson);
            }
        }
        return bans;
    }

    public Map<Integer, String> getPackageBans() {
        int N = this.mRecords.size();
        ArrayMap<Integer, String> packageBans = new ArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            Record r = this.mRecords.valueAt(i);
            if (r.importance == 0) {
                packageBans.put(Integer.valueOf(r.uid), r.pkg);
            }
        }
        return packageBans;
    }

    public void onPackagesChanged(boolean queryReplace, String[] pkgList) {
        if (queryReplace || pkgList == null || pkgList.length == 0 || this.mRestoredWithoutUids.isEmpty()) {
            return;
        }
        PackageManager pm = this.mContext.getPackageManager();
        boolean updated = false;
        for (String pkg : pkgList) {
            Record r = this.mRestoredWithoutUids.get(pkg);
            if (r != null) {
                try {
                    r.uid = pm.getPackageUidAsUser(r.pkg, 0);
                    this.mRestoredWithoutUids.remove(pkg);
                    this.mRecords.put(recordKey(r.pkg, r.uid), r);
                    updated = true;
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        if (!updated) {
            return;
        }
        updateConfig();
    }

    private static class Record {
        static int UNKNOWN_UID = -10000;
        int importance;
        String pkg;
        int priority;
        int uid;
        int visibility;

        Record(Record record) {
            this();
        }

        private Record() {
            this.uid = UNKNOWN_UID;
            this.importance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.priority = 0;
            this.visibility = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }

        public String toString() {
            return "pkg: " + this.pkg + ", uid: " + this.uid + ", importance: " + this.importance;
        }
    }
}
