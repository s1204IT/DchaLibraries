package com.android.server.notification;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.notification.NotificationManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RankingHelper implements RankingConfig {
    private static final String ATT_NAME = "name";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_UID = "uid";
    private static final String ATT_VERSION = "version";
    private static final String ATT_VISIBILITY = "visibility";
    private static final boolean DEBUG = false;
    private static final String TAG = "RankingHelper";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_RANKING = "ranking";
    private static final int XML_VERSION = 1;
    private final Context mContext;
    private final ArrayMap<String, NotificationRecord> mProxyByGroupTmp;
    private final Handler mRankingHandler;
    private final NotificationSignalExtractor[] mSignalExtractors;
    private final NotificationComparator mPreliminaryComparator = new NotificationComparator();
    private final GlobalSortKeyComparator mFinalComparator = new GlobalSortKeyComparator();
    private final ArrayMap<String, SparseIntArray> mPackagePriorities = new ArrayMap<>();
    private final ArrayMap<String, SparseIntArray> mPackageVisibilities = new ArrayMap<>();

    public RankingHelper(Context context, Handler rankingHandler, String[] extractorNames) {
        this.mContext = context;
        this.mRankingHandler = rankingHandler;
        int N = extractorNames.length;
        this.mSignalExtractors = new NotificationSignalExtractor[N];
        for (int i = 0; i < N; i++) {
            try {
                Class<?> extractorClass = this.mContext.getClassLoader().loadClass(extractorNames[i]);
                NotificationSignalExtractor extractor = (NotificationSignalExtractor) extractorClass.newInstance();
                extractor.initialize(this.mContext);
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
        this.mProxyByGroupTmp = new ArrayMap<>();
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
                    Message m = Message.obtain(this.mRankingHandler, 4, recon);
                    long delay = recon.getDelay(TimeUnit.MILLISECONDS);
                    this.mRankingHandler.sendMessageDelayed(m, delay);
                }
            } catch (Throwable t) {
                Slog.w(TAG, "NotificationSignalExtractor failed.", t);
            }
        }
    }

    public void readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() == 2 && TAG_RANKING.equals(parser.getName())) {
            this.mPackagePriorities.clear();
            safeInt(parser, ATT_VERSION, 1);
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    String tag = parser.getName();
                    if (type == 3 && TAG_RANKING.equals(tag)) {
                        return;
                    }
                    if (type == 2 && TAG_PACKAGE.equals(tag)) {
                        int uid = safeInt(parser, ATT_UID, -1);
                        int priority = safeInt(parser, ATT_PRIORITY, 0);
                        int vis = safeInt(parser, ATT_VISIBILITY, -1000);
                        String name = parser.getAttributeValue(null, ATT_NAME);
                        if (!TextUtils.isEmpty(name)) {
                            if (priority != 0) {
                                SparseIntArray priorityByUid = this.mPackagePriorities.get(name);
                                if (priorityByUid == null) {
                                    priorityByUid = new SparseIntArray();
                                    this.mPackagePriorities.put(name, priorityByUid);
                                }
                                priorityByUid.put(uid, priority);
                            }
                            if (vis != -1000) {
                                SparseIntArray visibilityByUid = this.mPackageVisibilities.get(name);
                                if (visibilityByUid == null) {
                                    visibilityByUid = new SparseIntArray();
                                    this.mPackageVisibilities.put(name, visibilityByUid);
                                }
                                visibilityByUid.put(uid, vis);
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException("Failed to reach END_DOCUMENT");
                }
            }
        }
    }

    public void writeXml(XmlSerializer out) throws IOException {
        int visibility;
        int priority;
        out.startTag(null, TAG_RANKING);
        out.attribute(null, ATT_VERSION, Integer.toString(1));
        Set<String> packageNames = new ArraySet<>(this.mPackagePriorities.size() + this.mPackageVisibilities.size());
        packageNames.addAll(this.mPackagePriorities.keySet());
        packageNames.addAll(this.mPackageVisibilities.keySet());
        Set<Integer> packageUids = new ArraySet<>();
        for (String packageName : packageNames) {
            packageUids.clear();
            SparseIntArray priorityByUid = this.mPackagePriorities.get(packageName);
            SparseIntArray visibilityByUid = this.mPackageVisibilities.get(packageName);
            if (priorityByUid != null) {
                int M = priorityByUid.size();
                for (int j = 0; j < M; j++) {
                    packageUids.add(Integer.valueOf(priorityByUid.keyAt(j)));
                }
            }
            if (visibilityByUid != null) {
                int M2 = visibilityByUid.size();
                for (int j2 = 0; j2 < M2; j2++) {
                    packageUids.add(Integer.valueOf(visibilityByUid.keyAt(j2)));
                }
            }
            for (Integer uid : packageUids) {
                out.startTag(null, TAG_PACKAGE);
                out.attribute(null, ATT_NAME, packageName);
                if (priorityByUid != null && (priority = priorityByUid.get(uid.intValue())) != 0) {
                    out.attribute(null, ATT_PRIORITY, Integer.toString(priority));
                }
                if (visibilityByUid != null && (visibility = visibilityByUid.get(uid.intValue())) != -1000) {
                    out.attribute(null, ATT_VISIBILITY, Integer.toString(visibility));
                }
                out.attribute(null, ATT_UID, Integer.toString(uid.intValue()));
                out.endTag(null, TAG_PACKAGE);
            }
        }
        out.endTag(null, TAG_RANKING);
    }

    private void updateConfig() {
        int N = this.mSignalExtractors.length;
        for (int i = 0; i < N; i++) {
            this.mSignalExtractors[i].setConfig(this);
        }
        this.mRankingHandler.sendEmptyMessage(5);
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
        if (!TextUtils.isEmpty(value)) {
            try {
                return Integer.valueOf(value).intValue();
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        return defValue;
    }

    @Override
    public int getPackagePriority(String packageName, int uid) {
        SparseIntArray priorityByUid = this.mPackagePriorities.get(packageName);
        if (priorityByUid == null) {
            return 0;
        }
        int priority = priorityByUid.get(uid, 0);
        return priority;
    }

    @Override
    public void setPackagePriority(String packageName, int uid, int priority) {
        if (priority != getPackagePriority(packageName, uid)) {
            SparseIntArray priorityByUid = this.mPackagePriorities.get(packageName);
            if (priorityByUid == null) {
                priorityByUid = new SparseIntArray();
                this.mPackagePriorities.put(packageName, priorityByUid);
            }
            priorityByUid.put(uid, priority);
            updateConfig();
        }
    }

    @Override
    public int getPackageVisibilityOverride(String packageName, int uid) {
        SparseIntArray visibilityByUid = this.mPackageVisibilities.get(packageName);
        if (visibilityByUid == null) {
            return -1000;
        }
        int visibility = visibilityByUid.get(uid, -1000);
        return visibility;
    }

    @Override
    public void setPackageVisibilityOverride(String packageName, int uid, int visibility) {
        if (visibility != getPackageVisibilityOverride(packageName, uid)) {
            SparseIntArray visibilityByUid = this.mPackageVisibilities.get(packageName);
            if (visibilityByUid == null) {
                visibilityByUid = new SparseIntArray();
                this.mPackageVisibilities.put(packageName, visibilityByUid);
            }
            visibilityByUid.put(uid, visibility);
            updateConfig();
        }
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
        int N2 = this.mPackagePriorities.size();
        if (filter == null) {
            pw.print(prefix);
            pw.println("package priorities:");
        }
        for (int i2 = 0; i2 < N2; i2++) {
            String name = this.mPackagePriorities.keyAt(i2);
            if (filter == null || filter.matches(name)) {
                SparseIntArray priorityByUid = this.mPackagePriorities.get(name);
                int M = priorityByUid.size();
                for (int j = 0; j < M; j++) {
                    int uid = priorityByUid.keyAt(j);
                    int priority = priorityByUid.get(uid);
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print(name);
                    pw.print(" (");
                    pw.print(uid);
                    pw.print(") has priority: ");
                    pw.println(priority);
                }
            }
        }
    }
}
