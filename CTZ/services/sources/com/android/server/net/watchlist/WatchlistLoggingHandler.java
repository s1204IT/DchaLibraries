package com.android.server.net.watchlist;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.HexDump;
import com.android.server.net.watchlist.WatchlistReportDbHelper;
import com.android.server.pm.DumpState;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class WatchlistLoggingHandler extends Handler {
    private static final boolean DEBUG = false;
    private static final String DROPBOX_TAG = "network_watchlist_report";

    @VisibleForTesting
    static final int FORCE_REPORT_RECORDS_NOW_FOR_TEST_MSG = 3;

    @VisibleForTesting
    static final int LOG_WATCHLIST_EVENT_MSG = 1;

    @VisibleForTesting
    static final int REPORT_RECORDS_IF_NECESSARY_MSG = 2;
    private final ConcurrentHashMap<Integer, byte[]> mCachedUidDigestMap;
    private final WatchlistConfig mConfig;
    private final Context mContext;
    private final WatchlistReportDbHelper mDbHelper;
    private final DropBoxManager mDropBoxManager;
    private final PackageManager mPm;
    private int mPrimaryUserId;
    private final ContentResolver mResolver;
    private final WatchlistSettings mSettings;
    private static final String TAG = WatchlistLoggingHandler.class.getSimpleName();
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);

    private interface WatchlistEventKeys {
        public static final String HOST = "host";
        public static final String IP_ADDRESSES = "ipAddresses";
        public static final String TIMESTAMP = "timestamp";
        public static final String UID = "uid";
    }

    WatchlistLoggingHandler(Context context, Looper looper) {
        super(looper);
        this.mPrimaryUserId = -1;
        this.mCachedUidDigestMap = new ConcurrentHashMap<>();
        this.mContext = context;
        this.mPm = this.mContext.getPackageManager();
        this.mResolver = this.mContext.getContentResolver();
        this.mDbHelper = WatchlistReportDbHelper.getInstance(context);
        this.mConfig = WatchlistConfig.getInstance();
        this.mSettings = WatchlistSettings.getInstance();
        this.mDropBoxManager = (DropBoxManager) this.mContext.getSystemService(DropBoxManager.class);
        this.mPrimaryUserId = getPrimaryUserId();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                Bundle data = message.getData();
                handleNetworkEvent(data.getString(WatchlistEventKeys.HOST), data.getStringArray(WatchlistEventKeys.IP_ADDRESSES), data.getInt(WatchlistEventKeys.UID), data.getLong(WatchlistEventKeys.TIMESTAMP));
                break;
            case 2:
                tryAggregateRecords(getLastMidnightTime());
                break;
            case 3:
                if (message.obj instanceof Long) {
                    tryAggregateRecords(((Long) message.obj).longValue());
                } else {
                    Slog.e(TAG, "Msg.obj needs to be a Long object.");
                }
                break;
            default:
                Slog.d(TAG, "WatchlistLoggingHandler received an unknown of message.");
                break;
        }
    }

    private int getPrimaryUserId() {
        UserInfo primaryUser = ((UserManager) this.mContext.getSystemService("user")).getPrimaryUser();
        if (primaryUser != null) {
            return primaryUser.id;
        }
        return -1;
    }

    private boolean isPackageTestOnly(int i) {
        try {
            String[] packagesForUid = this.mPm.getPackagesForUid(i);
            if (packagesForUid != null && packagesForUid.length != 0) {
                return (this.mPm.getApplicationInfo(packagesForUid[0], 0).flags & 256) != 0;
            }
            Slog.e(TAG, "Couldn't find package: " + packagesForUid);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void reportWatchlistIfNecessary() {
        sendMessage(obtainMessage(2));
    }

    public void forceReportWatchlistForTest(long j) {
        Message messageObtainMessage = obtainMessage(3);
        messageObtainMessage.obj = Long.valueOf(j);
        sendMessage(messageObtainMessage);
    }

    public void asyncNetworkEvent(String str, String[] strArr, int i) {
        Message messageObtainMessage = obtainMessage(1);
        Bundle bundle = new Bundle();
        bundle.putString(WatchlistEventKeys.HOST, str);
        bundle.putStringArray(WatchlistEventKeys.IP_ADDRESSES, strArr);
        bundle.putInt(WatchlistEventKeys.UID, i);
        bundle.putLong(WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis());
        messageObtainMessage.setData(bundle);
        sendMessage(messageObtainMessage);
    }

    private void handleNetworkEvent(String str, String[] strArr, int i, long j) {
        if (this.mPrimaryUserId == -1) {
            this.mPrimaryUserId = getPrimaryUserId();
        }
        if (UserHandle.getUserId(i) != this.mPrimaryUserId) {
            return;
        }
        String strSearchAllSubDomainsInWatchlist = searchAllSubDomainsInWatchlist(str);
        if (strSearchAllSubDomainsInWatchlist != null) {
            insertRecord(i, strSearchAllSubDomainsInWatchlist, j);
            return;
        }
        String strSearchIpInWatchlist = searchIpInWatchlist(strArr);
        if (strSearchIpInWatchlist != null) {
            insertRecord(i, strSearchIpInWatchlist, j);
        }
    }

    private boolean insertRecord(int i, String str, long j) {
        if (!this.mConfig.isConfigSecure() && !isPackageTestOnly(i)) {
            return true;
        }
        byte[] digestFromUid = getDigestFromUid(i);
        if (digestFromUid == null) {
            Slog.e(TAG, "Cannot get digest from uid: " + i);
            return false;
        }
        return this.mDbHelper.insertNewRecord(digestFromUid, str, j);
    }

    private boolean shouldReportNetworkWatchlist(long j) {
        long j2 = Settings.Global.getLong(this.mResolver, "network_watchlist_last_report_time", 0L);
        if (j >= j2) {
            return j >= j2 + ONE_DAY_MS;
        }
        Slog.i(TAG, "Last report time is larger than current time, reset report");
        this.mDbHelper.cleanup(j2);
        return false;
    }

    private void tryAggregateRecords(long j) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            if (!shouldReportNetworkWatchlist(j)) {
                Slog.i(TAG, "No need to aggregate record yet.");
                return;
            }
            Slog.i(TAG, "Start aggregating watchlist records.");
            if (this.mDropBoxManager == null || !this.mDropBoxManager.isTagEnabled(DROPBOX_TAG)) {
                Slog.w(TAG, "Network Watchlist dropbox tag is not enabled");
            } else {
                Settings.Global.putLong(this.mResolver, "network_watchlist_last_report_time", j);
                WatchlistReportDbHelper.AggregatedResult aggregatedRecords = this.mDbHelper.getAggregatedRecords(j);
                if (aggregatedRecords == null) {
                    Slog.i(TAG, "Cannot get result from database");
                    return;
                }
                byte[] bArrEncodeWatchlistReport = ReportEncoder.encodeWatchlistReport(this.mConfig, this.mSettings.getPrivacySecretKey(), getAllDigestsForReport(aggregatedRecords), aggregatedRecords);
                if (bArrEncodeWatchlistReport != null) {
                    addEncodedReportToDropBox(bArrEncodeWatchlistReport);
                }
            }
            this.mDbHelper.cleanup(j);
        } finally {
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            Slog.i(TAG, "Milliseconds spent on tryAggregateRecords(): " + (jCurrentTimeMillis2 - jCurrentTimeMillis));
        }
    }

    @VisibleForTesting
    List<String> getAllDigestsForReport(WatchlistReportDbHelper.AggregatedResult aggregatedResult) {
        List<ApplicationInfo> installedApplications = this.mContext.getPackageManager().getInstalledApplications(DumpState.DUMP_INTENT_FILTER_VERIFIERS);
        HashSet hashSet = new HashSet(installedApplications.size() + aggregatedResult.appDigestCNCList.size());
        int size = installedApplications.size();
        for (int i = 0; i < size; i++) {
            byte[] digestFromUid = getDigestFromUid(installedApplications.get(i).uid);
            if (digestFromUid != null) {
                hashSet.add(HexDump.toHexString(digestFromUid));
            } else {
                Slog.e(TAG, "Cannot get digest from uid: " + installedApplications.get(i).uid + ",pkg: " + installedApplications.get(i).packageName);
            }
        }
        hashSet.addAll(aggregatedResult.appDigestCNCList.keySet());
        return new ArrayList(hashSet);
    }

    private void addEncodedReportToDropBox(byte[] bArr) {
        this.mDropBoxManager.addData(DROPBOX_TAG, bArr, 0);
    }

    private byte[] getDigestFromUid(final int i) {
        return this.mCachedUidDigestMap.computeIfAbsent(Integer.valueOf(i), new Function() {
            @Override
            public final Object apply(Object obj) {
                return WatchlistLoggingHandler.lambda$getDigestFromUid$0(this.f$0, i, (Integer) obj);
            }
        });
    }

    public static byte[] lambda$getDigestFromUid$0(WatchlistLoggingHandler watchlistLoggingHandler, int i, Integer num) {
        String[] packagesForUid = watchlistLoggingHandler.mPm.getPackagesForUid(num.intValue());
        int userId = UserHandle.getUserId(i);
        if (!ArrayUtils.isEmpty(packagesForUid)) {
            for (String str : packagesForUid) {
                try {
                    String str2 = watchlistLoggingHandler.mPm.getPackageInfoAsUser(str, 786432, userId).applicationInfo.publicSourceDir;
                    if (TextUtils.isEmpty(str2)) {
                        Slog.w(TAG, "Cannot find apkPath for " + str);
                    } else {
                        return DigestUtils.getSha256Hash(new File(str2));
                    }
                } catch (PackageManager.NameNotFoundException | IOException | NoSuchAlgorithmException e) {
                    Slog.e(TAG, "Should not happen", e);
                    return null;
                }
            }
        }
        return null;
    }

    private String searchIpInWatchlist(String[] strArr) {
        for (String str : strArr) {
            if (isIpInWatchlist(str)) {
                return str;
            }
        }
        return null;
    }

    private boolean isIpInWatchlist(String str) {
        if (str == null) {
            return false;
        }
        return this.mConfig.containsIp(str);
    }

    private boolean isHostInWatchlist(String str) {
        if (str == null) {
            return false;
        }
        return this.mConfig.containsDomain(str);
    }

    private String searchAllSubDomainsInWatchlist(String str) {
        if (str == null) {
            return null;
        }
        for (String str2 : getAllSubDomains(str)) {
            if (isHostInWatchlist(str2)) {
                return str2;
            }
        }
        return null;
    }

    @VisibleForTesting
    static String[] getAllSubDomains(String str) {
        if (str == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(str);
        int iIndexOf = str.indexOf(".");
        while (iIndexOf != -1) {
            str = str.substring(iIndexOf + 1);
            if (!TextUtils.isEmpty(str)) {
                arrayList.add(str);
            }
            iIndexOf = str.indexOf(".");
        }
        return (String[]) arrayList.toArray(new String[0]);
    }

    static long getLastMidnightTime() {
        return getMidnightTimestamp(0);
    }

    static long getMidnightTimestamp(int i) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(11, 0);
        gregorianCalendar.set(12, 0);
        gregorianCalendar.set(13, 0);
        gregorianCalendar.set(14, 0);
        gregorianCalendar.add(5, -i);
        return gregorianCalendar.getTimeInMillis();
    }
}
