package com.android.server.notification;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.metrics.LogMaker;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RankingHelper implements RankingConfig {
    private static final String ATT_APP_USER_LOCKED_FIELDS = "app_user_locked_fields";
    private static final String ATT_ID = "id";
    private static final String ATT_IMPORTANCE = "importance";
    private static final String ATT_NAME = "name";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_SHOW_BADGE = "show_badge";
    private static final String ATT_UID = "uid";
    private static final String ATT_VERSION = "version";
    private static final String ATT_VISIBILITY = "visibility";
    private static final int DEFAULT_IMPORTANCE = -1000;
    private static final int DEFAULT_LOCKED_APP_FIELDS = 0;
    private static final int DEFAULT_PRIORITY = 0;
    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static final int DEFAULT_VISIBILITY = -1000;
    private static final String TAG = "RankingHelper";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_GROUP = "channelGroup";
    private static final String TAG_PACKAGE = "package";
    static final String TAG_RANKING = "ranking";
    private static final int XML_VERSION = 1;
    private boolean mAreChannelsBypassingDnd;
    private SparseBooleanArray mBadgingEnabled;
    private final Context mContext;
    private final PackageManager mPm;
    private final NotificationComparator mPreliminaryComparator;
    private final RankingHandler mRankingHandler;
    private final NotificationSignalExtractor[] mSignalExtractors;
    private ZenModeHelper mZenModeHelper;
    private final GlobalSortKeyComparator mFinalComparator = new GlobalSortKeyComparator();
    private final ArrayMap<String, Record> mRecords = new ArrayMap<>();
    private final ArrayMap<String, NotificationRecord> mProxyByGroupTmp = new ArrayMap<>();
    private final ArrayMap<String, Record> mRestoredWithoutUids = new ArrayMap<>();

    public @interface LockableAppFields {
        public static final int USER_LOCKED_IMPORTANCE = 1;
    }

    public RankingHelper(Context context, PackageManager packageManager, RankingHandler rankingHandler, ZenModeHelper zenModeHelper, NotificationUsageStats notificationUsageStats, String[] strArr) {
        this.mContext = context;
        this.mRankingHandler = rankingHandler;
        this.mPm = packageManager;
        this.mZenModeHelper = zenModeHelper;
        this.mPreliminaryComparator = new NotificationComparator(this.mContext);
        updateBadgingEnabled();
        int length = strArr.length;
        this.mSignalExtractors = new NotificationSignalExtractor[length];
        for (int i = 0; i < length; i++) {
            try {
                NotificationSignalExtractor notificationSignalExtractor = (NotificationSignalExtractor) this.mContext.getClassLoader().loadClass(strArr[i]).newInstance();
                notificationSignalExtractor.initialize(this.mContext, notificationUsageStats);
                notificationSignalExtractor.setConfig(this);
                notificationSignalExtractor.setZenHelper(zenModeHelper);
                this.mSignalExtractors[i] = notificationSignalExtractor;
            } catch (ClassNotFoundException e) {
                Slog.w(TAG, "Couldn't find extractor " + strArr[i] + ".", e);
            } catch (IllegalAccessException e2) {
                Slog.w(TAG, "Problem accessing extractor " + strArr[i] + ".", e2);
            } catch (InstantiationException e3) {
                Slog.w(TAG, "Couldn't instantiate extractor " + strArr[i] + ".", e3);
            }
        }
        this.mAreChannelsBypassingDnd = (this.mZenModeHelper.getNotificationPolicy().state & 1) == 1;
        updateChannelsBypassingDnd();
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

    public void extractSignals(NotificationRecord notificationRecord) {
        int length = this.mSignalExtractors.length;
        for (int i = 0; i < length; i++) {
            try {
                RankingReconsideration rankingReconsiderationProcess = this.mSignalExtractors[i].process(notificationRecord);
                if (rankingReconsiderationProcess != null) {
                    this.mRankingHandler.requestReconsideration(rankingReconsiderationProcess);
                }
            } catch (Throwable th) {
                Slog.w(TAG, "NotificationSignalExtractor failed.", th);
            }
        }
    }

    public void readXml(XmlPullParser xmlPullParser, boolean z) throws XmlPullParserException, IOException {
        int packageUidAsUser;
        Record orCreateRecord;
        int next;
        int i = 2;
        if (xmlPullParser.getEventType() != 2 || !TAG_RANKING.equals(xmlPullParser.getName())) {
            return;
        }
        this.mRestoredWithoutUids.clear();
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 != 1) {
                String name = xmlPullParser.getName();
                if (next2 == 3 && TAG_RANKING.equals(name)) {
                    return;
                }
                if (next2 == i && "package".equals(name)) {
                    int intAttribute = XmlUtils.readIntAttribute(xmlPullParser, "uid", Record.UNKNOWN_UID);
                    String attributeValue = xmlPullParser.getAttributeValue(null, "name");
                    if (!TextUtils.isEmpty(attributeValue)) {
                        if (z) {
                            try {
                                packageUidAsUser = this.mPm.getPackageUidAsUser(attributeValue, 0);
                            } catch (PackageManager.NameNotFoundException e) {
                                packageUidAsUser = intAttribute;
                            }
                            orCreateRecord = getOrCreateRecord(attributeValue, packageUidAsUser, XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE), XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0), XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE), XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true));
                            orCreateRecord.importance = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                            orCreateRecord.priority = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0);
                            orCreateRecord.visibility = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                            orCreateRecord.showBadge = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                            orCreateRecord.lockedAppFields = XmlUtils.readIntAttribute(xmlPullParser, ATT_APP_USER_LOCKED_FIELDS, 0);
                            int depth = xmlPullParser.getDepth();
                            while (true) {
                                next = xmlPullParser.next();
                                if (next == 1 && (next != 3 || xmlPullParser.getDepth() > depth)) {
                                    if (next != 3 && next != 4) {
                                        String name2 = xmlPullParser.getName();
                                        if (TAG_GROUP.equals(name2)) {
                                            String attributeValue2 = xmlPullParser.getAttributeValue(null, ATT_ID);
                                            String attributeValue3 = xmlPullParser.getAttributeValue(null, "name");
                                            if (!TextUtils.isEmpty(attributeValue2)) {
                                                NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(attributeValue2, attributeValue3);
                                                notificationChannelGroup.populateFromXml(xmlPullParser);
                                                orCreateRecord.groups.put(attributeValue2, notificationChannelGroup);
                                            }
                                        }
                                        if (TAG_CHANNEL.equals(name2)) {
                                            String attributeValue4 = xmlPullParser.getAttributeValue(null, ATT_ID);
                                            String attributeValue5 = xmlPullParser.getAttributeValue(null, "name");
                                            int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                            if (!TextUtils.isEmpty(attributeValue4) && !TextUtils.isEmpty(attributeValue5)) {
                                                NotificationChannel notificationChannel = new NotificationChannel(attributeValue4, attributeValue5, intAttribute2);
                                                if (z) {
                                                    notificationChannel.populateFromXmlForRestore(xmlPullParser, this.mContext);
                                                } else {
                                                    notificationChannel.populateFromXml(xmlPullParser);
                                                }
                                                orCreateRecord.channels.put(attributeValue4, notificationChannel);
                                            }
                                        }
                                    }
                                } else {
                                    try {
                                        deleteDefaultChannelIfNeeded(orCreateRecord);
                                        break;
                                    } catch (PackageManager.NameNotFoundException e2) {
                                        Slog.e(TAG, "deleteDefaultChannelIfNeeded - Exception: " + e2);
                                    }
                                }
                            }
                        } else {
                            packageUidAsUser = intAttribute;
                            orCreateRecord = getOrCreateRecord(attributeValue, packageUidAsUser, XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE), XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0), XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE), XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true));
                            orCreateRecord.importance = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                            orCreateRecord.priority = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0);
                            orCreateRecord.visibility = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                            orCreateRecord.showBadge = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                            orCreateRecord.lockedAppFields = XmlUtils.readIntAttribute(xmlPullParser, ATT_APP_USER_LOCKED_FIELDS, 0);
                            int depth2 = xmlPullParser.getDepth();
                            while (true) {
                                next = xmlPullParser.next();
                                if (next == 1) {
                                }
                                deleteDefaultChannelIfNeeded(orCreateRecord);
                            }
                        }
                    }
                    i = 2;
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    private static String recordKey(String str, int i) {
        return str + "|" + i;
    }

    private Record getRecord(String str, int i) {
        Record record;
        String strRecordKey = recordKey(str, i);
        synchronized (this.mRecords) {
            record = this.mRecords.get(strRecordKey);
        }
        return record;
    }

    private Record getOrCreateRecord(String str, int i) {
        return getOrCreateRecord(str, i, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, 0, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, true);
    }

    private Record getOrCreateRecord(String str, int i, int i2, int i3, int i4, boolean z) {
        Record record;
        String strRecordKey = recordKey(str, i);
        synchronized (this.mRecords) {
            record = i == Record.UNKNOWN_UID ? this.mRestoredWithoutUids.get(str) : this.mRecords.get(strRecordKey);
            if (record == null) {
                record = new Record();
                record.pkg = str;
                record.uid = i;
                record.importance = i2;
                record.priority = i3;
                record.visibility = i4;
                record.showBadge = z;
                try {
                    createDefaultChannelIfNeeded(record);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "createDefaultChannelIfNeeded - Exception: " + e);
                }
                if (record.uid == Record.UNKNOWN_UID) {
                    this.mRestoredWithoutUids.put(str, record);
                } else {
                    this.mRecords.put(strRecordKey, record);
                }
            }
        }
        return record;
    }

    private boolean shouldHaveDefaultChannel(Record record) throws PackageManager.NameNotFoundException {
        return this.mPm.getApplicationInfoAsUser(record.pkg, 0, UserHandle.getUserId(record.uid)).targetSdkVersion < 26;
    }

    private void deleteDefaultChannelIfNeeded(Record record) throws PackageManager.NameNotFoundException {
        if (!record.channels.containsKey("miscellaneous") || shouldHaveDefaultChannel(record)) {
            return;
        }
        record.channels.remove("miscellaneous");
    }

    private void createDefaultChannelIfNeeded(Record record) throws PackageManager.NameNotFoundException {
        if (record.channels.containsKey("miscellaneous")) {
            record.channels.get("miscellaneous").setName(this.mContext.getString(R.string.bg_user_sound_notification_message));
            return;
        }
        if (!shouldHaveDefaultChannel(record)) {
            return;
        }
        NotificationChannel notificationChannel = new NotificationChannel("miscellaneous", this.mContext.getString(R.string.bg_user_sound_notification_message), record.importance);
        notificationChannel.setBypassDnd(record.priority == 2);
        notificationChannel.setLockscreenVisibility(record.visibility);
        if (record.importance != -1000) {
            notificationChannel.lockFields(4);
        }
        if (record.priority != 0) {
            notificationChannel.lockFields(1);
        }
        if (record.visibility != -1000) {
            notificationChannel.lockFields(2);
        }
        record.channels.put(notificationChannel.getId(), notificationChannel);
    }

    public void writeXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
        xmlSerializer.startTag(null, TAG_RANKING);
        xmlSerializer.attribute(null, ATT_VERSION, Integer.toString(1));
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                Record recordValueAt = this.mRecords.valueAt(i);
                if (!z || UserHandle.getUserId(recordValueAt.uid) == 0) {
                    if ((recordValueAt.importance == -1000 && recordValueAt.priority == 0 && recordValueAt.visibility == -1000 && recordValueAt.showBadge && recordValueAt.lockedAppFields == 0 && recordValueAt.channels.size() <= 0 && recordValueAt.groups.size() <= 0) ? false : true) {
                        xmlSerializer.startTag(null, "package");
                        xmlSerializer.attribute(null, "name", recordValueAt.pkg);
                        if (recordValueAt.importance != -1000) {
                            xmlSerializer.attribute(null, ATT_IMPORTANCE, Integer.toString(recordValueAt.importance));
                        }
                        if (recordValueAt.priority != 0) {
                            xmlSerializer.attribute(null, ATT_PRIORITY, Integer.toString(recordValueAt.priority));
                        }
                        if (recordValueAt.visibility != -1000) {
                            xmlSerializer.attribute(null, ATT_VISIBILITY, Integer.toString(recordValueAt.visibility));
                        }
                        xmlSerializer.attribute(null, ATT_SHOW_BADGE, Boolean.toString(recordValueAt.showBadge));
                        xmlSerializer.attribute(null, ATT_APP_USER_LOCKED_FIELDS, Integer.toString(recordValueAt.lockedAppFields));
                        if (!z) {
                            xmlSerializer.attribute(null, "uid", Integer.toString(recordValueAt.uid));
                        }
                        Iterator<NotificationChannelGroup> it = recordValueAt.groups.values().iterator();
                        while (it.hasNext()) {
                            it.next().writeXml(xmlSerializer);
                        }
                        for (NotificationChannel notificationChannel : recordValueAt.channels.values()) {
                            if (z) {
                                if (!notificationChannel.isDeleted()) {
                                    notificationChannel.writeXmlForBackup(xmlSerializer, this.mContext);
                                }
                            } else {
                                notificationChannel.writeXml(xmlSerializer);
                            }
                        }
                        xmlSerializer.endTag(null, "package");
                    }
                }
            }
        }
        xmlSerializer.endTag(null, TAG_RANKING);
    }

    private void updateConfig() {
        int length = this.mSignalExtractors.length;
        for (int i = 0; i < length; i++) {
            this.mSignalExtractors[i].setConfig(this);
        }
        this.mRankingHandler.requestSort();
    }

    public void sort(ArrayList<NotificationRecord> arrayList) {
        String str;
        int size = arrayList.size();
        int i = size - 1;
        for (int i2 = i; i2 >= 0; i2--) {
            arrayList.get(i2).setGlobalSortKey(null);
        }
        Collections.sort(arrayList, this.mPreliminaryComparator);
        synchronized (this.mProxyByGroupTmp) {
            while (i >= 0) {
                try {
                    NotificationRecord notificationRecord = arrayList.get(i);
                    notificationRecord.setAuthoritativeRank(i);
                    String groupKey = notificationRecord.getGroupKey();
                    if (this.mProxyByGroupTmp.get(groupKey) == null) {
                        this.mProxyByGroupTmp.put(groupKey, notificationRecord);
                    }
                    i--;
                } catch (Throwable th) {
                    throw th;
                }
            }
            for (int i3 = 0; i3 < size; i3++) {
                NotificationRecord notificationRecord2 = arrayList.get(i3);
                NotificationRecord notificationRecord3 = this.mProxyByGroupTmp.get(notificationRecord2.getGroupKey());
                String sortKey = notificationRecord2.getNotification().getSortKey();
                if (sortKey == null) {
                    str = "nsk";
                } else if (sortKey.equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
                    str = "esk";
                } else {
                    str = "gsk=" + sortKey;
                }
                boolean zIsGroupSummary = notificationRecord2.getNotification().isGroupSummary();
                Object[] objArr = new Object[5];
                char c = '0';
                objArr[0] = Character.valueOf((!notificationRecord2.isRecentlyIntrusive() || notificationRecord2.getImportance() <= 1) ? '1' : '0');
                objArr[1] = Integer.valueOf(notificationRecord3.getAuthoritativeRank());
                if (!zIsGroupSummary) {
                    c = '1';
                }
                objArr[2] = Character.valueOf(c);
                objArr[3] = str;
                objArr[4] = Integer.valueOf(notificationRecord2.getAuthoritativeRank());
                notificationRecord2.setGlobalSortKey(String.format("intrsv=%c:grnk=0x%04x:gsmry=%c:%s:rnk=0x%04x", objArr));
            }
            this.mProxyByGroupTmp.clear();
        }
        Collections.sort(arrayList, this.mFinalComparator);
    }

    public int indexOf(ArrayList<NotificationRecord> arrayList, NotificationRecord notificationRecord) {
        return Collections.binarySearch(arrayList, notificationRecord, this.mFinalComparator);
    }

    @Override
    public int getImportance(String str, int i) {
        return getOrCreateRecord(str, i).importance;
    }

    public boolean getIsAppImportanceLocked(String str, int i) {
        return (getOrCreateRecord(str, i).lockedAppFields & 1) != 0;
    }

    @Override
    public boolean canShowBadge(String str, int i) {
        return getOrCreateRecord(str, i).showBadge;
    }

    @Override
    public void setShowBadge(String str, int i, boolean z) {
        getOrCreateRecord(str, i).showBadge = z;
        updateConfig();
    }

    @Override
    public boolean isGroupBlocked(String str, int i, String str2) {
        NotificationChannelGroup notificationChannelGroup;
        if (str2 == null || (notificationChannelGroup = getOrCreateRecord(str, i).groups.get(str2)) == null) {
            return false;
        }
        return notificationChannelGroup.isBlocked();
    }

    int getPackagePriority(String str, int i) {
        return getOrCreateRecord(str, i).priority;
    }

    int getPackageVisibility(String str, int i) {
        return getOrCreateRecord(str, i).visibility;
    }

    @Override
    public void createNotificationChannelGroup(String str, int i, NotificationChannelGroup notificationChannelGroup, boolean z) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(notificationChannelGroup);
        Preconditions.checkNotNull(notificationChannelGroup.getId());
        Preconditions.checkNotNull(Boolean.valueOf(!TextUtils.isEmpty(notificationChannelGroup.getName())));
        Record orCreateRecord = getOrCreateRecord(str, i);
        if (orCreateRecord == null) {
            throw new IllegalArgumentException("Invalid package");
        }
        NotificationChannelGroup notificationChannelGroup2 = orCreateRecord.groups.get(notificationChannelGroup.getId());
        if (!notificationChannelGroup.equals(notificationChannelGroup2)) {
            MetricsLogger.action(getChannelGroupLog(notificationChannelGroup.getId(), str));
        }
        if (notificationChannelGroup2 != null) {
            notificationChannelGroup.setChannels(notificationChannelGroup2.getChannels());
            if (z) {
                notificationChannelGroup.setBlocked(notificationChannelGroup2.isBlocked());
            }
        }
        orCreateRecord.groups.put(notificationChannelGroup.getId(), notificationChannelGroup);
    }

    @Override
    public void createNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z, boolean z2) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(notificationChannel);
        Preconditions.checkNotNull(notificationChannel.getId());
        Preconditions.checkArgument(!TextUtils.isEmpty(notificationChannel.getName()));
        Record orCreateRecord = getOrCreateRecord(str, i);
        if (orCreateRecord == null) {
            throw new IllegalArgumentException("Invalid package");
        }
        if (notificationChannel.getGroup() != null && !orCreateRecord.groups.containsKey(notificationChannel.getGroup())) {
            throw new IllegalArgumentException("NotificationChannelGroup doesn't exist");
        }
        if ("miscellaneous".equals(notificationChannel.getId())) {
            throw new IllegalArgumentException("Reserved id");
        }
        NotificationChannel notificationChannel2 = orCreateRecord.channels.get(notificationChannel.getId());
        if (notificationChannel2 != null && z) {
            if (notificationChannel2.isDeleted()) {
                notificationChannel2.setDeleted(false);
                MetricsLogger.action(getChannelLog(notificationChannel, str).setType(1));
            }
            notificationChannel2.setName(notificationChannel.getName().toString());
            notificationChannel2.setDescription(notificationChannel.getDescription());
            notificationChannel2.setBlockableSystem(notificationChannel.isBlockableSystem());
            if (notificationChannel2.getGroup() == null) {
                notificationChannel2.setGroup(notificationChannel.getGroup());
            }
            if (notificationChannel2.getUserLockedFields() == 0 && notificationChannel.getImportance() < notificationChannel2.getImportance()) {
                notificationChannel2.setImportance(notificationChannel.getImportance());
            }
            if (notificationChannel2.getUserLockedFields() == 0 && z2) {
                boolean zCanBypassDnd = notificationChannel.canBypassDnd();
                notificationChannel2.setBypassDnd(zCanBypassDnd);
                if (zCanBypassDnd != this.mAreChannelsBypassingDnd) {
                    updateChannelsBypassingDnd();
                }
            }
            updateConfig();
            return;
        }
        if (notificationChannel.getImportance() < 0 || notificationChannel.getImportance() > 5) {
            throw new IllegalArgumentException("Invalid importance level");
        }
        if (z && !z2) {
            notificationChannel.setBypassDnd(orCreateRecord.priority == 2);
        }
        if (z) {
            notificationChannel.setLockscreenVisibility(orCreateRecord.visibility);
        }
        clearLockedFields(notificationChannel);
        if (notificationChannel.getLockscreenVisibility() == 1) {
            notificationChannel.setLockscreenVisibility(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
        }
        if (!orCreateRecord.showBadge) {
            notificationChannel.setShowBadge(false);
        }
        orCreateRecord.channels.put(notificationChannel.getId(), notificationChannel);
        if (notificationChannel.canBypassDnd() != this.mAreChannelsBypassingDnd) {
            updateChannelsBypassingDnd();
        }
        MetricsLogger.action(getChannelLog(notificationChannel, str).setType(1));
    }

    void clearLockedFields(NotificationChannel notificationChannel) {
        notificationChannel.unlockFields(notificationChannel.getUserLockedFields());
    }

    @Override
    public void updateNotificationChannel(String str, int i, NotificationChannel notificationChannel, boolean z) {
        Preconditions.checkNotNull(notificationChannel);
        Preconditions.checkNotNull(notificationChannel.getId());
        Record orCreateRecord = getOrCreateRecord(str, i);
        if (orCreateRecord == null) {
            throw new IllegalArgumentException("Invalid package");
        }
        NotificationChannel notificationChannel2 = orCreateRecord.channels.get(notificationChannel.getId());
        if (notificationChannel2 == null || notificationChannel2.isDeleted()) {
            throw new IllegalArgumentException("Channel does not exist");
        }
        if (notificationChannel.getLockscreenVisibility() == 1) {
            notificationChannel.setLockscreenVisibility(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
        }
        if (!z) {
            notificationChannel.unlockFields(notificationChannel.getUserLockedFields());
        }
        if (z) {
            notificationChannel.lockFields(notificationChannel2.getUserLockedFields());
            lockFieldsForUpdate(notificationChannel2, notificationChannel);
        }
        orCreateRecord.channels.put(notificationChannel.getId(), notificationChannel);
        if ("miscellaneous".equals(notificationChannel.getId())) {
            orCreateRecord.importance = notificationChannel.getImportance();
            orCreateRecord.priority = notificationChannel.canBypassDnd() ? 2 : 0;
            orCreateRecord.visibility = notificationChannel.getLockscreenVisibility();
            orCreateRecord.showBadge = notificationChannel.canShowBadge();
        }
        if (!notificationChannel2.equals(notificationChannel)) {
            MetricsLogger.action(getChannelLog(notificationChannel, str));
        }
        if (notificationChannel.canBypassDnd() != this.mAreChannelsBypassingDnd) {
            updateChannelsBypassingDnd();
        }
        updateConfig();
    }

    @Override
    public NotificationChannel getNotificationChannel(String str, int i, String str2, boolean z) {
        Preconditions.checkNotNull(str);
        Record orCreateRecord = getOrCreateRecord(str, i);
        if (orCreateRecord == null) {
            return null;
        }
        if (str2 == null) {
            str2 = "miscellaneous";
        }
        NotificationChannel notificationChannel = orCreateRecord.channels.get(str2);
        if (notificationChannel == null || (!z && notificationChannel.isDeleted())) {
            return null;
        }
        return notificationChannel;
    }

    @Override
    public void deleteNotificationChannel(String str, int i, String str2) {
        NotificationChannel notificationChannel;
        Record record = getRecord(str, i);
        if (record != null && (notificationChannel = record.channels.get(str2)) != null) {
            notificationChannel.setDeleted(true);
            LogMaker channelLog = getChannelLog(notificationChannel, str);
            channelLog.setType(2);
            MetricsLogger.action(channelLog);
            if (this.mAreChannelsBypassingDnd && notificationChannel.canBypassDnd()) {
                updateChannelsBypassingDnd();
            }
        }
    }

    @Override
    @VisibleForTesting
    public void permanentlyDeleteNotificationChannel(String str, int i, String str2) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(str2);
        Record record = getRecord(str, i);
        if (record == null) {
            return;
        }
        record.channels.remove(str2);
    }

    @Override
    public void permanentlyDeleteNotificationChannels(String str, int i) {
        Preconditions.checkNotNull(str);
        Record record = getRecord(str, i);
        if (record == null) {
            return;
        }
        for (int size = record.channels.size() - 1; size >= 0; size--) {
            String strKeyAt = record.channels.keyAt(size);
            if (!"miscellaneous".equals(strKeyAt)) {
                record.channels.remove(strKeyAt);
            }
        }
    }

    public NotificationChannelGroup getNotificationChannelGroupWithChannels(String str, int i, String str2, boolean z) {
        Preconditions.checkNotNull(str);
        Record record = getRecord(str, i);
        if (record == null || str2 == null || !record.groups.containsKey(str2)) {
            return null;
        }
        NotificationChannelGroup notificationChannelGroupClone = record.groups.get(str2).clone();
        notificationChannelGroupClone.setChannels(new ArrayList());
        int size = record.channels.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotificationChannel notificationChannelValueAt = record.channels.valueAt(i2);
            if ((z || !notificationChannelValueAt.isDeleted()) && str2.equals(notificationChannelValueAt.getGroup())) {
                notificationChannelGroupClone.addChannel(notificationChannelValueAt);
            }
        }
        return notificationChannelGroupClone;
    }

    public NotificationChannelGroup getNotificationChannelGroup(String str, String str2, int i) {
        Preconditions.checkNotNull(str2);
        Record record = getRecord(str2, i);
        if (record == null) {
            return null;
        }
        return record.groups.get(str);
    }

    @Override
    public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String str, int i, boolean z, boolean z2, boolean z3) {
        Preconditions.checkNotNull(str);
        ArrayMap arrayMap = new ArrayMap();
        Record record = getRecord(str, i);
        if (record == null) {
            return ParceledListSlice.emptyList();
        }
        NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(null, null);
        int size = record.channels.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotificationChannel notificationChannelValueAt = record.channels.valueAt(i2);
            if (z || !notificationChannelValueAt.isDeleted()) {
                if (notificationChannelValueAt.getGroup() != null) {
                    if (record.groups.get(notificationChannelValueAt.getGroup()) != null) {
                        NotificationChannelGroup notificationChannelGroupClone = (NotificationChannelGroup) arrayMap.get(notificationChannelValueAt.getGroup());
                        if (notificationChannelGroupClone == null) {
                            notificationChannelGroupClone = record.groups.get(notificationChannelValueAt.getGroup()).clone();
                            notificationChannelGroupClone.setChannels(new ArrayList());
                            arrayMap.put(notificationChannelValueAt.getGroup(), notificationChannelGroupClone);
                        }
                        notificationChannelGroupClone.addChannel(notificationChannelValueAt);
                    }
                } else {
                    notificationChannelGroup.addChannel(notificationChannelValueAt);
                }
            }
        }
        if (z2 && notificationChannelGroup.getChannels().size() > 0) {
            arrayMap.put(null, notificationChannelGroup);
        }
        if (z3) {
            for (NotificationChannelGroup notificationChannelGroup2 : record.groups.values()) {
                if (!arrayMap.containsKey(notificationChannelGroup2.getId())) {
                    arrayMap.put(notificationChannelGroup2.getId(), notificationChannelGroup2);
                }
            }
        }
        return new ParceledListSlice<>(new ArrayList(arrayMap.values()));
    }

    public List<NotificationChannel> deleteNotificationChannelGroup(String str, int i, String str2) {
        ArrayList arrayList = new ArrayList();
        Record record = getRecord(str, i);
        if (record == null || TextUtils.isEmpty(str2)) {
            return arrayList;
        }
        record.groups.remove(str2);
        int size = record.channels.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotificationChannel notificationChannelValueAt = record.channels.valueAt(i2);
            if (str2.equals(notificationChannelValueAt.getGroup())) {
                notificationChannelValueAt.setDeleted(true);
                arrayList.add(notificationChannelValueAt);
            }
        }
        return arrayList;
    }

    @Override
    public Collection<NotificationChannelGroup> getNotificationChannelGroups(String str, int i) {
        Record record = getRecord(str, i);
        if (record == null) {
            return new ArrayList();
        }
        return record.groups.values();
    }

    @Override
    public ParceledListSlice<NotificationChannel> getNotificationChannels(String str, int i, boolean z) {
        Preconditions.checkNotNull(str);
        ArrayList arrayList = new ArrayList();
        Record record = getRecord(str, i);
        if (record == null) {
            return ParceledListSlice.emptyList();
        }
        int size = record.channels.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotificationChannel notificationChannelValueAt = record.channels.valueAt(i2);
            if (z || !notificationChannelValueAt.isDeleted()) {
                arrayList.add(notificationChannelValueAt);
            }
        }
        return new ParceledListSlice<>(arrayList);
    }

    public boolean onlyHasDefaultChannel(String str, int i) {
        Record orCreateRecord = getOrCreateRecord(str, i);
        return orCreateRecord.channels.size() == 1 && orCreateRecord.channels.containsKey("miscellaneous");
    }

    public int getDeletedChannelCount(String str, int i) {
        Preconditions.checkNotNull(str);
        Record record = getRecord(str, i);
        if (record == null) {
            return 0;
        }
        int size = record.channels.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            if (record.channels.valueAt(i3).isDeleted()) {
                i2++;
            }
        }
        return i2;
    }

    public int getBlockedChannelCount(String str, int i) {
        Preconditions.checkNotNull(str);
        Record record = getRecord(str, i);
        if (record == null) {
            return 0;
        }
        int size = record.channels.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            NotificationChannel notificationChannelValueAt = record.channels.valueAt(i3);
            if (!notificationChannelValueAt.isDeleted() && notificationChannelValueAt.getImportance() == 0) {
                i2++;
            }
        }
        return i2;
    }

    public int getBlockedAppCount(int i) {
        int i2;
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                Record recordValueAt = this.mRecords.valueAt(i3);
                if (i == UserHandle.getUserId(recordValueAt.uid) && recordValueAt.importance == 0) {
                    i2++;
                }
            }
        }
        return i2;
    }

    public void updateChannelsBypassingDnd() {
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                Record recordValueAt = this.mRecords.valueAt(i);
                int size2 = recordValueAt.channels.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    NotificationChannel notificationChannelValueAt = recordValueAt.channels.valueAt(i2);
                    if (!notificationChannelValueAt.isDeleted() && notificationChannelValueAt.canBypassDnd()) {
                        if (!this.mAreChannelsBypassingDnd) {
                            this.mAreChannelsBypassingDnd = true;
                            updateZenPolicy(true);
                        }
                        return;
                    }
                }
            }
            if (this.mAreChannelsBypassingDnd) {
                this.mAreChannelsBypassingDnd = false;
                updateZenPolicy(false);
            }
        }
    }

    public void updateZenPolicy(boolean z) {
        NotificationManager.Policy notificationPolicy = this.mZenModeHelper.getNotificationPolicy();
        this.mZenModeHelper.setNotificationPolicy(new NotificationManager.Policy(notificationPolicy.priorityCategories, notificationPolicy.priorityCallSenders, notificationPolicy.priorityMessageSenders, notificationPolicy.suppressedVisualEffects, z ? 1 : 0));
    }

    public boolean areChannelsBypassingDnd() {
        return this.mAreChannelsBypassingDnd;
    }

    @Override
    public void setImportance(String str, int i, int i2) {
        getOrCreateRecord(str, i).importance = i2;
        updateConfig();
    }

    public void setEnabled(String str, int i, boolean z) {
        if ((getImportance(str, i) != 0) == z) {
            return;
        }
        setImportance(str, i, z ? JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE : 0);
    }

    public void setAppImportanceLocked(String str, int i) {
        Record orCreateRecord = getOrCreateRecord(str, i);
        if ((orCreateRecord.lockedAppFields & 1) != 0) {
            return;
        }
        orCreateRecord.lockedAppFields |= 1;
        updateConfig();
    }

    @VisibleForTesting
    void lockFieldsForUpdate(NotificationChannel notificationChannel, NotificationChannel notificationChannel2) {
        if (notificationChannel.canBypassDnd() != notificationChannel2.canBypassDnd()) {
            notificationChannel2.lockFields(1);
        }
        if (notificationChannel.getLockscreenVisibility() != notificationChannel2.getLockscreenVisibility()) {
            notificationChannel2.lockFields(2);
        }
        if (notificationChannel.getImportance() != notificationChannel2.getImportance()) {
            notificationChannel2.lockFields(4);
        }
        if (notificationChannel.shouldShowLights() != notificationChannel2.shouldShowLights() || notificationChannel.getLightColor() != notificationChannel2.getLightColor()) {
            notificationChannel2.lockFields(8);
        }
        if (!Objects.equals(notificationChannel.getSound(), notificationChannel2.getSound())) {
            notificationChannel2.lockFields(32);
        }
        if (!Arrays.equals(notificationChannel.getVibrationPattern(), notificationChannel2.getVibrationPattern()) || notificationChannel.shouldVibrate() != notificationChannel2.shouldVibrate()) {
            notificationChannel2.lockFields(16);
        }
        if (notificationChannel.canShowBadge() != notificationChannel2.canShowBadge()) {
            notificationChannel2.lockFields(128);
        }
    }

    public void dump(PrintWriter printWriter, String str, NotificationManagerService.DumpFilter dumpFilter) {
        int length = this.mSignalExtractors.length;
        printWriter.print(str);
        printWriter.print("mSignalExtractors.length = ");
        printWriter.println(length);
        for (int i = 0; i < length; i++) {
            printWriter.print(str);
            printWriter.print("  ");
            printWriter.println(this.mSignalExtractors[i].getClass().getSimpleName());
        }
        printWriter.print(str);
        printWriter.println("per-package config:");
        printWriter.println("Records:");
        synchronized (this.mRecords) {
            dumpRecords(printWriter, str, dumpFilter, this.mRecords);
        }
        printWriter.println("Restored without uid:");
        dumpRecords(printWriter, str, dumpFilter, this.mRestoredWithoutUids);
    }

    public void dump(ProtoOutputStream protoOutputStream, NotificationManagerService.DumpFilter dumpFilter) {
        int length = this.mSignalExtractors.length;
        for (int i = 0; i < length; i++) {
            protoOutputStream.write(2237677961217L, this.mSignalExtractors[i].getClass().getSimpleName());
        }
        synchronized (this.mRecords) {
            dumpRecords(protoOutputStream, 2246267895810L, dumpFilter, this.mRecords);
        }
        dumpRecords(protoOutputStream, 2246267895811L, dumpFilter, this.mRestoredWithoutUids);
    }

    private static void dumpRecords(ProtoOutputStream protoOutputStream, long j, NotificationManagerService.DumpFilter dumpFilter, ArrayMap<String, Record> arrayMap) {
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            Record recordValueAt = arrayMap.valueAt(i);
            if (dumpFilter.matches(recordValueAt.pkg)) {
                long jStart = protoOutputStream.start(j);
                protoOutputStream.write(1138166333441L, recordValueAt.pkg);
                protoOutputStream.write(1120986464258L, recordValueAt.uid);
                protoOutputStream.write(1172526071811L, recordValueAt.importance);
                protoOutputStream.write(1120986464260L, recordValueAt.priority);
                protoOutputStream.write(1172526071813L, recordValueAt.visibility);
                protoOutputStream.write(1133871366150L, recordValueAt.showBadge);
                Iterator<NotificationChannel> it = recordValueAt.channels.values().iterator();
                while (it.hasNext()) {
                    it.next().writeToProto(protoOutputStream, 2246267895815L);
                }
                Iterator<NotificationChannelGroup> it2 = recordValueAt.groups.values().iterator();
                while (it2.hasNext()) {
                    it2.next().writeToProto(protoOutputStream, 2246267895816L);
                }
                protoOutputStream.end(jStart);
            }
        }
    }

    private static void dumpRecords(PrintWriter printWriter, String str, NotificationManagerService.DumpFilter dumpFilter, ArrayMap<String, Record> arrayMap) {
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            Record recordValueAt = arrayMap.valueAt(i);
            if (dumpFilter.matches(recordValueAt.pkg)) {
                printWriter.print(str);
                printWriter.print("  AppSettings: ");
                printWriter.print(recordValueAt.pkg);
                printWriter.print(" (");
                printWriter.print(recordValueAt.uid == Record.UNKNOWN_UID ? "UNKNOWN_UID" : Integer.toString(recordValueAt.uid));
                printWriter.print(')');
                if (recordValueAt.importance != -1000) {
                    printWriter.print(" importance=");
                    printWriter.print(NotificationListenerService.Ranking.importanceToString(recordValueAt.importance));
                }
                if (recordValueAt.priority != 0) {
                    printWriter.print(" priority=");
                    printWriter.print(Notification.priorityToString(recordValueAt.priority));
                }
                if (recordValueAt.visibility != -1000) {
                    printWriter.print(" visibility=");
                    printWriter.print(Notification.visibilityToString(recordValueAt.visibility));
                }
                printWriter.print(" showBadge=");
                printWriter.print(Boolean.toString(recordValueAt.showBadge));
                printWriter.println();
                for (NotificationChannel notificationChannel : recordValueAt.channels.values()) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.print("  ");
                    printWriter.println(notificationChannel);
                }
                for (NotificationChannelGroup notificationChannelGroup : recordValueAt.groups.values()) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.print("  ");
                    printWriter.println(notificationChannelGroup);
                }
            }
        }
    }

    public JSONObject dumpJson(NotificationManagerService.DumpFilter dumpFilter) {
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        try {
            jSONObject.put("noUid", this.mRestoredWithoutUids.size());
        } catch (JSONException e) {
        }
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                Record recordValueAt = this.mRecords.valueAt(i);
                if (dumpFilter == null || dumpFilter.matches(recordValueAt.pkg)) {
                    JSONObject jSONObject2 = new JSONObject();
                    try {
                        jSONObject2.put("userId", UserHandle.getUserId(recordValueAt.uid));
                        jSONObject2.put(BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA, recordValueAt.pkg);
                        if (recordValueAt.importance != -1000) {
                            jSONObject2.put(ATT_IMPORTANCE, NotificationListenerService.Ranking.importanceToString(recordValueAt.importance));
                        }
                        if (recordValueAt.priority != 0) {
                            jSONObject2.put(ATT_PRIORITY, Notification.priorityToString(recordValueAt.priority));
                        }
                        if (recordValueAt.visibility != -1000) {
                            jSONObject2.put(ATT_VISIBILITY, Notification.visibilityToString(recordValueAt.visibility));
                        }
                        if (!recordValueAt.showBadge) {
                            jSONObject2.put("showBadge", Boolean.valueOf(recordValueAt.showBadge));
                        }
                        JSONArray jSONArray2 = new JSONArray();
                        Iterator<NotificationChannel> it = recordValueAt.channels.values().iterator();
                        while (it.hasNext()) {
                            jSONArray2.put(it.next().toJson());
                        }
                        jSONObject2.put("channels", jSONArray2);
                        JSONArray jSONArray3 = new JSONArray();
                        Iterator<NotificationChannelGroup> it2 = recordValueAt.groups.values().iterator();
                        while (it2.hasNext()) {
                            jSONArray3.put(it2.next().toJson());
                        }
                        jSONObject2.put("groups", jSONArray3);
                    } catch (JSONException e2) {
                    }
                    jSONArray.put(jSONObject2);
                }
            }
        }
        try {
            jSONObject.put("records", jSONArray);
        } catch (JSONException e3) {
        }
        return jSONObject;
    }

    public JSONArray dumpBansJson(NotificationManagerService.DumpFilter dumpFilter) {
        JSONArray jSONArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : getPackageBans().entrySet()) {
            int userId = UserHandle.getUserId(entry.getKey().intValue());
            String value = entry.getValue();
            if (dumpFilter == null || dumpFilter.matches(value)) {
                JSONObject jSONObject = new JSONObject();
                try {
                    jSONObject.put("userId", userId);
                    jSONObject.put(BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA, value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jSONArray.put(jSONObject);
            }
        }
        return jSONArray;
    }

    public Map<Integer, String> getPackageBans() {
        ArrayMap arrayMap;
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            arrayMap = new ArrayMap(size);
            for (int i = 0; i < size; i++) {
                Record recordValueAt = this.mRecords.valueAt(i);
                if (recordValueAt.importance == 0) {
                    arrayMap.put(Integer.valueOf(recordValueAt.uid), recordValueAt.pkg);
                }
            }
        }
        return arrayMap;
    }

    public JSONArray dumpChannelsJson(NotificationManagerService.DumpFilter dumpFilter) {
        JSONArray jSONArray = new JSONArray();
        for (Map.Entry<String, Integer> entry : getPackageChannels().entrySet()) {
            String key = entry.getKey();
            if (dumpFilter == null || dumpFilter.matches(key)) {
                JSONObject jSONObject = new JSONObject();
                try {
                    jSONObject.put(BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA, key);
                    jSONObject.put("channelCount", entry.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jSONArray.put(jSONObject);
            }
        }
        return jSONArray;
    }

    private Map<String, Integer> getPackageChannels() {
        ArrayMap arrayMap = new ArrayMap();
        synchronized (this.mRecords) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                Record recordValueAt = this.mRecords.valueAt(i);
                int i2 = 0;
                for (int i3 = 0; i3 < recordValueAt.channels.size(); i3++) {
                    if (!recordValueAt.channels.valueAt(i3).isDeleted()) {
                        i2++;
                    }
                }
                arrayMap.put(recordValueAt.pkg, Integer.valueOf(i2));
            }
        }
        return arrayMap;
    }

    public void onUserRemoved(int i) {
        synchronized (this.mRecords) {
            for (int size = this.mRecords.size() - 1; size >= 0; size--) {
                if (UserHandle.getUserId(this.mRecords.valueAt(size).uid) == i) {
                    this.mRecords.removeAt(size);
                }
            }
        }
    }

    protected void onLocaleChanged(Context context, int i) {
        synchronized (this.mRecords) {
            int size = this.mRecords.size();
            for (int i2 = 0; i2 < size; i2++) {
                Record recordValueAt = this.mRecords.valueAt(i2);
                if (UserHandle.getUserId(recordValueAt.uid) == i && recordValueAt.channels.containsKey("miscellaneous")) {
                    recordValueAt.channels.get("miscellaneous").setName(context.getResources().getString(R.string.bg_user_sound_notification_message));
                }
            }
        }
    }

    public void onPackagesChanged(boolean z, int i, String[] strArr, int[] iArr) {
        boolean z2;
        if (strArr == null || strArr.length == 0) {
            return;
        }
        int i2 = 0;
        if (z) {
            int iMin = Math.min(strArr.length, iArr.length);
            z2 = false;
            while (i2 < iMin) {
                String str = strArr[i2];
                int i3 = iArr[i2];
                synchronized (this.mRecords) {
                    this.mRecords.remove(recordKey(str, i3));
                }
                this.mRestoredWithoutUids.remove(str);
                i2++;
                z2 = true;
            }
        } else {
            int length = strArr.length;
            boolean z3 = false;
            while (i2 < length) {
                String str2 = strArr[i2];
                Record record = this.mRestoredWithoutUids.get(str2);
                if (record != null) {
                    try {
                        record.uid = this.mPm.getPackageUidAsUser(record.pkg, i);
                        this.mRestoredWithoutUids.remove(str2);
                        synchronized (this.mRecords) {
                            this.mRecords.put(recordKey(record.pkg, record.uid), record);
                        }
                        z3 = true;
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                try {
                    Record record2 = getRecord(str2, this.mPm.getPackageUidAsUser(str2, i));
                    if (record2 != null) {
                        createDefaultChannelIfNeeded(record2);
                        deleteDefaultChannelIfNeeded(record2);
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                }
                i2++;
            }
            z2 = z3;
        }
        if (z2) {
            updateConfig();
        }
    }

    private LogMaker getChannelLog(NotificationChannel notificationChannel, String str) {
        return new LogMaker(856).setType(6).setPackageName(str).addTaggedData(857, notificationChannel.getId()).addTaggedData(858, Integer.valueOf(notificationChannel.getImportance()));
    }

    private LogMaker getChannelGroupLog(String str, String str2) {
        return new LogMaker(859).setType(6).addTaggedData(860, str).setPackageName(str2);
    }

    public void updateBadgingEnabled() {
        if (this.mBadgingEnabled == null) {
            this.mBadgingEnabled = new SparseBooleanArray();
        }
        boolean z = false;
        for (int i = 0; i < this.mBadgingEnabled.size(); i++) {
            int iKeyAt = this.mBadgingEnabled.keyAt(i);
            boolean z2 = this.mBadgingEnabled.get(iKeyAt);
            boolean z3 = true;
            boolean z4 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, iKeyAt) != 0;
            this.mBadgingEnabled.put(iKeyAt, z4);
            if (z2 == z4) {
                z3 = false;
            }
            z |= z3;
        }
        if (z) {
            updateConfig();
        }
    }

    @Override
    public boolean badgingEnabled(UserHandle userHandle) {
        int identifier = userHandle.getIdentifier();
        boolean z = false;
        if (identifier == -1) {
            return false;
        }
        if (this.mBadgingEnabled.indexOfKey(identifier) < 0) {
            SparseBooleanArray sparseBooleanArray = this.mBadgingEnabled;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, identifier) != 0) {
                z = true;
            }
            sparseBooleanArray.put(identifier, z);
        }
        return this.mBadgingEnabled.get(identifier, true);
    }

    private static class Record {
        static int UNKNOWN_UID = -10000;
        ArrayMap<String, NotificationChannel> channels;
        Map<String, NotificationChannelGroup> groups;
        int importance;
        int lockedAppFields;
        String pkg;
        int priority;
        boolean showBadge;
        int uid;
        int visibility;

        private Record() {
            this.uid = UNKNOWN_UID;
            this.importance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.priority = 0;
            this.visibility = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.showBadge = true;
            this.lockedAppFields = 0;
            this.channels = new ArrayMap<>();
            this.groups = new ConcurrentHashMap();
        }
    }
}
