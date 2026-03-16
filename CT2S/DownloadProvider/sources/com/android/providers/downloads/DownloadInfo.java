package com.android.providers.downloads;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DownloadInfo {
    public boolean mAllowMetered;
    public boolean mAllowRoaming;
    public int mAllowedNetworkTypes;
    public int mBypassRecommendedSizeLimit;
    public String mClass;
    private final Context mContext;
    public int mControl;
    public String mCookies;
    public long mCurrentBytes;
    public boolean mDeleted;
    public String mDescription;
    public int mDestination;
    public String mETag;
    public String mExtras;
    public String mFileName;
    public int mFuzz;
    public String mHint;
    public long mId;
    public boolean mIsPublicApi;
    public long mLastMod;
    public String mMediaProviderUri;
    public int mMediaScanned;
    public String mMimeType;

    @Deprecated
    public boolean mNoIntegrity;
    private final DownloadNotifier mNotifier;
    public int mNumFailed;
    public String mPackage;
    public String mReferer;
    private List<Pair<String, String>> mRequestHeaders;
    public int mRetryAfter;
    public int mStatus;

    @GuardedBy("this")
    private Future<?> mSubmittedTask;
    private final SystemFacade mSystemFacade;

    @GuardedBy("this")
    private DownloadThread mTask;
    public String mTitle;
    public long mTotalBytes;
    public int mUid;
    public String mUri;
    public String mUserAgent;
    public int mVisibility;

    public enum NetworkState {
        OK,
        NO_CONNECTION,
        UNUSABLE_DUE_TO_SIZE,
        RECOMMENDED_UNUSABLE_DUE_TO_SIZE,
        CANNOT_USE_ROAMING,
        TYPE_DISALLOWED_BY_REQUESTOR,
        BLOCKED
    }

    public static class Reader {
        private Cursor mCursor;
        private ContentResolver mResolver;

        public Reader(ContentResolver resolver, Cursor cursor) {
            this.mResolver = resolver;
            this.mCursor = cursor;
        }

        public DownloadInfo newDownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier) {
            DownloadInfo info = new DownloadInfo(context, systemFacade, notifier);
            updateFromDatabase(info);
            readRequestHeaders(info);
            return info;
        }

        public void updateFromDatabase(DownloadInfo info) {
            info.mId = getLong("_id").longValue();
            info.mUri = getString("uri");
            info.mNoIntegrity = getInt("no_integrity").intValue() == 1;
            info.mHint = getString("hint");
            info.mFileName = getString("_data");
            info.mMimeType = Intent.normalizeMimeType(getString("mimetype"));
            info.mDestination = getInt("destination").intValue();
            info.mVisibility = getInt("visibility").intValue();
            info.mStatus = getInt("status").intValue();
            info.mNumFailed = getInt("numfailed").intValue();
            int retryRedirect = getInt("method").intValue();
            info.mRetryAfter = 268435455 & retryRedirect;
            info.mLastMod = getLong("lastmod").longValue();
            info.mPackage = getString("notificationpackage");
            info.mClass = getString("notificationclass");
            info.mExtras = getString("notificationextras");
            info.mCookies = getString("cookiedata");
            info.mUserAgent = getString("useragent");
            info.mReferer = getString("referer");
            info.mTotalBytes = getLong("total_bytes").longValue();
            info.mCurrentBytes = getLong("current_bytes").longValue();
            info.mETag = getString("etag");
            info.mUid = getInt("uid").intValue();
            info.mMediaScanned = getInt("scanned").intValue();
            info.mDeleted = getInt("deleted").intValue() == 1;
            info.mMediaProviderUri = getString("mediaprovider_uri");
            info.mIsPublicApi = getInt("is_public_api").intValue() != 0;
            info.mAllowedNetworkTypes = getInt("allowed_network_types").intValue();
            info.mAllowRoaming = getInt("allow_roaming").intValue() != 0;
            info.mAllowMetered = getInt("allow_metered").intValue() != 0;
            info.mTitle = getString("title");
            info.mDescription = getString("description");
            info.mBypassRecommendedSizeLimit = getInt("bypass_recommended_size_limit").intValue();
            synchronized (this) {
                info.mControl = getInt("control").intValue();
            }
        }

        private void readRequestHeaders(DownloadInfo info) {
            info.mRequestHeaders.clear();
            Uri headerUri = Uri.withAppendedPath(info.getAllDownloadsUri(), "headers");
            Cursor cursor = this.mResolver.query(headerUri, null, null, null, null);
            try {
                int headerIndex = cursor.getColumnIndexOrThrow("header");
                int valueIndex = cursor.getColumnIndexOrThrow("value");
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    addHeader(info, cursor.getString(headerIndex), cursor.getString(valueIndex));
                    cursor.moveToNext();
                }
                cursor.close();
                if (info.mCookies != null) {
                    addHeader(info, "Cookie", info.mCookies);
                }
                if (info.mReferer != null) {
                    addHeader(info, "Referer", info.mReferer);
                }
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }

        private void addHeader(DownloadInfo info, String header, String value) {
            info.mRequestHeaders.add(Pair.create(header, value));
        }

        private String getString(String column) {
            int index = this.mCursor.getColumnIndexOrThrow(column);
            String s = this.mCursor.getString(index);
            if (TextUtils.isEmpty(s)) {
                return null;
            }
            return s;
        }

        private Integer getInt(String column) {
            return Integer.valueOf(this.mCursor.getInt(this.mCursor.getColumnIndexOrThrow(column)));
        }

        private Long getLong(String column) {
            return Long.valueOf(this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow(column)));
        }
    }

    private DownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier) {
        this.mRequestHeaders = new ArrayList();
        this.mContext = context;
        this.mSystemFacade = systemFacade;
        this.mNotifier = notifier;
        this.mFuzz = Helpers.sRandom.nextInt(1001);
    }

    public Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(this.mRequestHeaders);
    }

    public String getUserAgent() {
        return this.mUserAgent != null ? this.mUserAgent : Constants.DEFAULT_USER_AGENT;
    }

    public void sendIntentIfRequested() {
        Intent intent;
        if (this.mPackage != null) {
            if (this.mIsPublicApi) {
                intent = new Intent("android.intent.action.DOWNLOAD_COMPLETE");
                intent.setPackage(this.mPackage);
                intent.putExtra("extra_download_id", this.mId);
            } else if (this.mClass != null) {
                intent = new Intent("android.intent.action.DOWNLOAD_COMPLETED");
                intent.setClassName(this.mPackage, this.mClass);
                if (this.mExtras != null) {
                    intent.putExtra("notificationextras", this.mExtras);
                }
                intent.setData(getMyDownloadsUri());
            } else {
                return;
            }
            this.mSystemFacade.sendBroadcast(intent);
        }
    }

    public long restartTime(long now) {
        if (this.mNumFailed != 0) {
            if (this.mRetryAfter > 0) {
                long now2 = this.mLastMod + ((long) this.mRetryAfter);
                return now2;
            }
            long now3 = this.mLastMod + ((long) ((this.mFuzz + 1000) * 30 * (1 << (this.mNumFailed - 1))));
            return now3;
        }
        return now;
    }

    private boolean isReadyToDownload() {
        if (this.mControl == 1) {
            return false;
        }
        switch (this.mStatus) {
            case 0:
            case 190:
            case 192:
                break;
            case 194:
                long now = this.mSystemFacade.currentTimeMillis();
                break;
            case 195:
            case 196:
                break;
            case 199:
                break;
        }
        return false;
    }

    public NetworkState checkCanUseNetwork(long totalBytes) {
        NetworkInfo info = this.mSystemFacade.getActiveNetworkInfo(this.mUid);
        if (info == null || !info.isConnected()) {
            return NetworkState.NO_CONNECTION;
        }
        if (NetworkInfo.DetailedState.BLOCKED.equals(info.getDetailedState())) {
            return NetworkState.BLOCKED;
        }
        if (this.mSystemFacade.isNetworkRoaming() && !isRoamingAllowed()) {
            return NetworkState.CANNOT_USE_ROAMING;
        }
        if (this.mSystemFacade.isActiveNetworkMetered() && !this.mAllowMetered) {
            return NetworkState.TYPE_DISALLOWED_BY_REQUESTOR;
        }
        return checkIsNetworkTypeAllowed(info.getType(), totalBytes);
    }

    private boolean isRoamingAllowed() {
        if (this.mIsPublicApi) {
            return this.mAllowRoaming;
        }
        return this.mDestination != 3;
    }

    private NetworkState checkIsNetworkTypeAllowed(int networkType, long totalBytes) {
        if (this.mIsPublicApi) {
            int flag = translateNetworkTypeToApiFlag(networkType);
            boolean allowAllNetworkTypes = this.mAllowedNetworkTypes == -1;
            if (!allowAllNetworkTypes && (this.mAllowedNetworkTypes & flag) == 0) {
                return NetworkState.TYPE_DISALLOWED_BY_REQUESTOR;
            }
        }
        return checkSizeAllowedForNetwork(networkType, totalBytes);
    }

    private int translateNetworkTypeToApiFlag(int networkType) {
        switch (networkType) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 7:
                return 4;
            default:
                return 0;
        }
    }

    private NetworkState checkSizeAllowedForNetwork(int networkType, long totalBytes) {
        Long recommendedMaxBytesOverMobile;
        if (totalBytes <= 0) {
            return NetworkState.OK;
        }
        if (ConnectivityManager.isNetworkTypeMobile(networkType)) {
            Long maxBytesOverMobile = this.mSystemFacade.getMaxBytesOverMobile();
            if (maxBytesOverMobile != null && totalBytes > maxBytesOverMobile.longValue()) {
                return NetworkState.UNUSABLE_DUE_TO_SIZE;
            }
            if (this.mBypassRecommendedSizeLimit == 0 && (recommendedMaxBytesOverMobile = this.mSystemFacade.getRecommendedMaxBytesOverMobile()) != null && totalBytes > recommendedMaxBytesOverMobile.longValue()) {
                return NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE;
            }
        }
        return NetworkState.OK;
    }

    public boolean startDownloadIfReady(ExecutorService executor) {
        boolean isReady;
        synchronized (this) {
            isReady = isReadyToDownload();
            boolean isActive = (this.mSubmittedTask == null || this.mSubmittedTask.isDone()) ? false : true;
            if (isReady && !isActive) {
                if (this.mStatus != 192) {
                    this.mStatus = 192;
                    ContentValues values = new ContentValues();
                    values.put("status", Integer.valueOf(this.mStatus));
                    this.mContext.getContentResolver().update(getAllDownloadsUri(), values, null, null);
                }
                this.mTask = new DownloadThread(this.mContext, this.mSystemFacade, this.mNotifier, this);
                this.mSubmittedTask = executor.submit(this.mTask);
            }
        }
        return isReady;
    }

    public boolean startScanIfReady(DownloadScanner scanner) {
        boolean isReady;
        synchronized (this) {
            isReady = shouldScanFile();
            if (isReady) {
                scanner.requestScan(this);
            }
        }
        return isReady;
    }

    public Uri getMyDownloadsUri() {
        return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, this.mId);
    }

    public Uri getAllDownloadsUri() {
        return ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, this.mId);
    }

    public String toString() {
        CharArrayWriter writer = new CharArrayWriter();
        dump(new IndentingPrintWriter(writer, "  "));
        return writer.toString();
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("DownloadInfo:");
        pw.increaseIndent();
        pw.printPair("mId", Long.valueOf(this.mId));
        pw.printPair("mLastMod", Long.valueOf(this.mLastMod));
        pw.printPair("mPackage", this.mPackage);
        pw.printPair("mUid", Integer.valueOf(this.mUid));
        pw.println();
        pw.printPair("mUri", this.mUri);
        pw.println();
        pw.printPair("mMimeType", this.mMimeType);
        pw.printPair("mCookies", this.mCookies != null ? "yes" : "no");
        pw.printPair("mReferer", this.mReferer != null ? "yes" : "no");
        pw.printPair("mUserAgent", this.mUserAgent);
        pw.println();
        pw.printPair("mFileName", this.mFileName);
        pw.printPair("mDestination", Integer.valueOf(this.mDestination));
        pw.println();
        pw.printPair("mStatus", Downloads.Impl.statusToString(this.mStatus));
        pw.printPair("mCurrentBytes", Long.valueOf(this.mCurrentBytes));
        pw.printPair("mTotalBytes", Long.valueOf(this.mTotalBytes));
        pw.println();
        pw.printPair("mNumFailed", Integer.valueOf(this.mNumFailed));
        pw.printPair("mRetryAfter", Integer.valueOf(this.mRetryAfter));
        pw.printPair("mETag", this.mETag);
        pw.printPair("mIsPublicApi", Boolean.valueOf(this.mIsPublicApi));
        pw.println();
        pw.printPair("mAllowedNetworkTypes", Integer.valueOf(this.mAllowedNetworkTypes));
        pw.printPair("mAllowRoaming", Boolean.valueOf(this.mAllowRoaming));
        pw.printPair("mAllowMetered", Boolean.valueOf(this.mAllowMetered));
        pw.println();
        pw.decreaseIndent();
    }

    public long nextActionMillis(long now) {
        if (Downloads.Impl.isStatusCompleted(this.mStatus)) {
            return Long.MAX_VALUE;
        }
        if (this.mStatus != 194) {
            return 0L;
        }
        long when = restartTime(now);
        if (when > now) {
            return when - now;
        }
        return 0L;
    }

    public boolean shouldScanFile() {
        return this.mMediaScanned == 0 && (this.mDestination == 0 || this.mDestination == 4 || this.mDestination == 6) && Downloads.Impl.isStatusSuccess(this.mStatus);
    }

    void notifyPauseDueToSize(boolean isWifiRequired) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(getAllDownloadsUri());
        intent.setClassName(SizeLimitActivity.class.getPackage().getName(), SizeLimitActivity.class.getName());
        intent.setFlags(268435456);
        intent.putExtra("isWifiRequired", isWifiRequired);
        this.mContext.startActivity(intent);
    }

    public static int queryDownloadStatus(ContentResolver resolver, long id) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), new String[]{"status"}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 190;
        } finally {
            cursor.close();
        }
    }
}
